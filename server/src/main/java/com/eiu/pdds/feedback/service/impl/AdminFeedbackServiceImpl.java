package com.eiu.pdds.feedback.service.impl;

import com.eiu.pdds.audit.event.AdminAuditEvent;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.dto.FeedbackQueueFilter;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackVerifyRequest;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.feedback.repository.FeedbackSpecifications;
import com.eiu.pdds.feedback.service.ActiveLearningDatasetAssembler;
import com.eiu.pdds.feedback.service.ActiveLearningExporter;
import com.eiu.pdds.feedback.service.AdminFeedbackService;
import com.eiu.pdds.feedback.service.FeedbackDiseaseResolver;
import com.eiu.pdds.feedback.service.FeedbackMapper;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private final DiagnosisFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackDiseaseResolver diseaseResolver;
    private final FeedbackMapper feedbackMapper;
    private final ActiveLearningDatasetAssembler datasetAssembler;
    private final Map<ActiveLearningExportFormat, ActiveLearningExporter> exporters;
    private final ApplicationEventPublisher eventPublisher;

    public AdminFeedbackServiceImpl(
            DiagnosisFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            FeedbackDiseaseResolver diseaseResolver,
            FeedbackMapper feedbackMapper,
            ActiveLearningDatasetAssembler datasetAssembler,
            List<ActiveLearningExporter> exporterList,
            ApplicationEventPublisher eventPublisher
    ) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.diseaseResolver = diseaseResolver;
        this.feedbackMapper = feedbackMapper;
        this.datasetAssembler = datasetAssembler;
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(ActiveLearningExporter::supportedFormat, Function.identity()));
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponseDto> getFeedbackQueue(FeedbackQueueFilter filter, Pageable pageable) {
        validateDateRange(filter);
        Page<FeedbackResponseDto> page = feedbackRepository
                .findAll(FeedbackSpecifications.withFilter(filter), pageable)
                .map(feedbackMapper::toDto);
        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public FeedbackResponseDto verifyFeedback(
            UUID feedbackId,
            UUID verifierUserId,
            String verifierUsername,
            FeedbackVerifyRequest request
    ) {
        DiagnosisFeedback feedback = feedbackRepository.findByIdAndDeletedAtIsNull(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (feedback.getDiagnosis() == null || feedback.getDiagnosis().getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        }

        User verifier = userRepository.findById(verifierUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FeedbackVerifyRequest safeRequest = request != null
                ? request
                : new FeedbackVerifyRequest(null, null, null, null);

        Map<String, Object> beforeState = captureAuditState(feedback);
        Boolean effectiveIsAccurate = safeRequest.isAccurate() != null
                ? safeRequest.isAccurate()
                : feedback.getIsAccurate();
        Disease resolvedDisease = resolveVerifiedSuggestedDisease(feedback, effectiveIsAccurate, safeRequest);

        feedback.setIsAccurate(effectiveIsAccurate);
        feedback.setSuggestedDisease(resolvedDisease);
        feedback.setExpertVerified(true);
        feedback.setVerifiedByUser(verifier);
        feedback.setVerifiedAt(Instant.now());
        feedback.setExpertNotes(normalizeOptionalText(safeRequest.expertNotes()));

        DiagnosisFeedback saved = feedbackRepository.saveAndFlush(feedback);
        Map<String, Object> afterState = captureAuditState(saved);

        eventPublisher.publishEvent(new AdminAuditEvent(
                verifierUserId,
                verifierUsername,
                "VERIFY_FEEDBACK",
                "DIAGNOSIS_FEEDBACK",
                saved.getId().toString(),
                beforeState,
                afterState
        ));

        return feedbackMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportVerifiedDataset(String format, FeedbackQueueFilter filter) {
        ActiveLearningExportFormat exportFormat = ActiveLearningExportFormat.fromString(format);
        validateDateRange(filter);

        FeedbackQueueFilter verifiedOnlyFilter = new FeedbackQueueFilter(
                Boolean.TRUE,
                filter != null ? filter.isAccurate() : null,
                filter != null ? filter.plantCode() : null,
                filter != null ? filter.fromDate() : null,
                filter != null ? filter.toDate() : null
        );

        List<DiagnosisFeedback> verifiedFeedbacks = feedbackRepository.findAll(
                FeedbackSpecifications.withFilter(verifiedOnlyFilter),
                Sort.by(Sort.Direction.DESC, "verifiedAt")
        );

        List<ActiveLearningExportRow> rows = datasetAssembler.assemble(verifiedFeedbacks);
        ActiveLearningExporter exporter = exporters.get(exportFormat);
        if (exporter == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Unsupported export format: " + format);
        }
        return exporter.export(rows);
    }

    private Disease resolveVerifiedSuggestedDisease(
            DiagnosisFeedback feedback,
            Boolean effectiveIsAccurate,
            FeedbackVerifyRequest request
    ) {
        if (Boolean.TRUE.equals(effectiveIsAccurate)) {
            diseaseResolver.resolve(Boolean.TRUE, request.suggestedDiseaseId(), request.suggestedDiseaseCode());
            return null;
        }

        boolean requestSpecifiesDisease = request.suggestedDiseaseId() != null
                || StringUtils.hasText(request.suggestedDiseaseCode());
        if (requestSpecifiesDisease) {
            return diseaseResolver.resolve(Boolean.FALSE, request.suggestedDiseaseId(), request.suggestedDiseaseCode());
        }
        return feedback.getSuggestedDisease();
    }

    private Map<String, Object> captureAuditState(DiagnosisFeedback feedback) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("isAccurate", feedback.getIsAccurate());
        state.put("suggestedDiseaseCode",
                feedback.getSuggestedDisease() != null ? feedback.getSuggestedDisease().getDiseaseCode() : null);
        state.put("expertVerified", feedback.getExpertVerified());
        state.put("expertNotes", feedback.getExpertNotes());
        return Collections.unmodifiableMap(state);
    }

    private void validateDateRange(FeedbackQueueFilter filter) {
        if (filter != null
                && filter.fromDate() != null
                && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "fromDate must not be after toDate");
        }
    }

    private String normalizeOptionalText(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }
}
