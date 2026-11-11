package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.common.util.FileSecurityValidator;
import com.eiu.pdds.diagnosis.dto.DiagnosisSaveCommand;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.CoordinateTransformer;
import com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService;
import com.eiu.pdds.diagnosis.service.DiagnosisPersistenceService;
import com.eiu.pdds.diagnosis.service.DiagnosisService;
import com.eiu.pdds.diagnosis.service.PathologyAnalysisService;
import com.eiu.pdds.diagnosis.service.dto.PathologyResolution;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeRequest;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.plant.PlantDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.integration.ai.AiInferenceProvider;
import com.eiu.pdds.integration.ai.dto.InferenceResult;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.treatment.service.TreatmentRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Facade Orchestrator implementing DiagnosisService (SRP, DIP & OCP).
 * Coordinates parallel I/O over Virtual Threads and delegates domain operations
 * to specialized single-responsibility services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private final AiInferenceProvider aiInferenceProvider;
    private final StorageService storageService;
    private final CoordinateTransformer coordinateTransformer;
    private final PathologyAnalysisService pathologyAnalysisService;
    private final TreatmentRecommendationService treatmentRecommendationService;
    private final DiagnosisPersistenceService persistenceService;
    private final DiagnosisIdempotencyService idempotencyService;
    private final DiagnosisRepository diagnosisRepository;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public DiagnosisAnalyzeResponse analyze(DiagnosisAnalyzeRequest request) {
        if (request == null || request.image() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Diagnosis request and image file must not be null");
        }
        return analyzeImage(
                request.image(),
                request.plantCode(),
                request.latitude(),
                request.longitude(),
                request.deviceInfo(),
                request.userId()
        );
    }

    @Override
    public DiagnosisAnalyzeResponse analyzeImage(
            MultipartFile image,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo,
            UUID userId
    ) {
        // 1. Validate file magic bytes & extract bytes
        byte[] imageBytes = FileSecurityValidator.validateAndExtractBytes(image);
        String filename = image.getOriginalFilename();
        String imageHash = FileSecurityValidator.calculateSha256(imageBytes);

        // 2. Idempotency & Quota Guard: Replay recent diagnosis if identical image sent (Saves API Quota)
        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis(userId, imageHash);
        if (cached.isPresent()) {
            log.info("Idempotent diagnosis hit for user='{}', imageHash='{}', replaying cached diagnosisId='{}' (Quota saved!)",
                    userId, imageHash, cached.get().diagnosisId());
            return cached.get();
        }

        log.info("Starting diagnosis pipeline for file='{}' ({} bytes, hash='{}'), cropHint='{}', device='{}', user='{}'",
                filename, imageBytes.length, imageHash, plantCodeHint, deviceInfo, userId);

        // 3. Parallel I/O via Java 21 Virtual Threads (Storage Upload & AI Inference)
        long startTime = System.currentTimeMillis();
        CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(
                () -> storageService.uploadImage(imageBytes, filename, "diagnoses"),
                virtualThreadExecutor
        );
        CompletableFuture<InferenceResult> inferenceFuture = CompletableFuture.supplyAsync(
                () -> aiInferenceProvider.inferPlantDisease(imageBytes, null),
                virtualThreadExecutor
        );

        String s3ImageUrl;
        InferenceResult inferenceResult;
        try {
            CompletableFuture.allOf(uploadFuture, inferenceFuture).join();
            s3ImageUrl = uploadFuture.get();
            inferenceResult = inferenceFuture.get();
        } catch (Exception e) {
            log.error("Parallel I/O failure during diagnosis analysis: ", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Diagnosis pipeline failed: " + cause.getMessage());
        }
        int inferenceTimeMs = (int) (System.currentTimeMillis() - startTime);

        // 4. Coordinate Transformation (SRP)
        List<BoundingBoxDto> boundingBoxDtos = coordinateTransformer.toBoundingBoxDtos(
                inferenceResult.detections(),
                inferenceResult.imageWidth(),
                inferenceResult.imageHeight()
        );

        // 5. Pathology & Crop Analysis (SRP)
        PathologyResolution pathology = pathologyAnalysisService.resolvePathology(
                inferenceResult.detections(),
                plantCodeHint
        );

        // 6. Treatment Recommendations (SRP)
        List<TreatmentDto> treatments = Collections.emptyList();
        if (inferenceResult.hasDetections()) {
            treatments = pathology.isHealthy()
                    ? treatmentRecommendationService.getHealthyCareTreatments(plantCodeHint)
                    : (pathology.dominantDiseaseEntity() != null
                    ? treatmentRecommendationService.getTreatmentsForDisease(pathology.dominantDiseaseEntity().getId())
                    : Collections.emptyList());
        }

        // 7. Transactional Persistence (SRP)
        Diagnosis saved = persistenceService.saveDiagnosis(new DiagnosisSaveCommand(
                userId,
                pathology.plantEntity(),
                s3ImageUrl,
                inferenceResult.imageWidth(),
                inferenceResult.imageHeight(),
                latitude,
                longitude,
                deviceInfo,
                pathology.dominantDiseaseEntity(),
                pathology.dominantConfidence(),
                inferenceTimeMs,
                inferenceResult.rawPayloadJson(),
                boundingBoxDtos
        ));

        // 8. Assemble Response DTO and record in idempotency cache
        DiagnosisAnalyzeResponse response = new DiagnosisAnalyzeResponse(
                saved.getId(),
                s3ImageUrl,
                inferenceResult.imageWidth(),
                inferenceResult.imageHeight(),
                toPlantDto(pathology.plantEntity()),
                pathology.dominantDiseaseDto(),
                pathology.dominantConfidence(),
                boundingBoxDtos,
                pathology.coOccurringDiseases(),
                treatments,
                inferenceTimeMs,
                latitude,
                longitude,
                deviceInfo,
                saved.getStatus().name(),
                saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now()
        );

        idempotencyService.recordDiagnosis(userId, imageHash, response);
        return response;
    }

    private PlantDto toPlantDto(Plant plant) {
        if (plant == null) {
            return null;
        }
        return new PlantDto(
                plant.getId(),
                plant.getPlantCode(),
                plant.getCommonName(),
                plant.getScientificName(),
                plant.getIconUrl(),
                plant.getDescription()
        );
    }

    @Override
    @Transactional
    public void softDeleteUserDiagnosis(UUID userId, UUID diagnosisId) {
        Diagnosis diagnosis = diagnosisRepository.findByIdAndDeletedAtIsNull(diagnosisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND));

        if (userId != null && (diagnosis.getUser() == null || !diagnosis.getUser().getId().equals(userId))) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        }

        diagnosis.setDeletedAt(Instant.now());
        diagnosisRepository.save(diagnosis);
        idempotencyService.evictById(diagnosisId);
        log.info("Soft-deleted diagnosis session id='{}' for user='{}'", diagnosisId, userId);
    }
}
