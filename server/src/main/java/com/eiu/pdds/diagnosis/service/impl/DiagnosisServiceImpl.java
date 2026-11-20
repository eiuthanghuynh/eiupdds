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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Facade Orchestrator implementing DiagnosisService (SRP, DIP & OCP).
 * Coordinates parallel I/O over a Spring-managed Virtual Thread executor with deterministic
 * Saga compensating S3 cleanup, and delegates domain operations to specialized services.
 */
@Slf4j
@Service
public class DiagnosisServiceImpl implements DiagnosisService {

    private final AiInferenceProvider aiInferenceProvider;
    private final StorageService storageService;
    private final CoordinateTransformer coordinateTransformer;
    private final PathologyAnalysisService pathologyAnalysisService;
    private final TreatmentRecommendationService treatmentRecommendationService;
    private final DiagnosisPersistenceService persistenceService;
    private final DiagnosisIdempotencyService idempotencyService;
    private final DiagnosisRepository diagnosisRepository;
    private final ExecutorService diagnosisIoExecutor;

    public DiagnosisServiceImpl(
            AiInferenceProvider aiInferenceProvider,
            StorageService storageService,
            CoordinateTransformer coordinateTransformer,
            PathologyAnalysisService pathologyAnalysisService,
            TreatmentRecommendationService treatmentRecommendationService,
            DiagnosisPersistenceService persistenceService,
            DiagnosisIdempotencyService idempotencyService,
            DiagnosisRepository diagnosisRepository,
            @Qualifier("diagnosisIoExecutor") ExecutorService diagnosisIoExecutor
    ) {
        this.aiInferenceProvider = aiInferenceProvider;
        this.storageService = storageService;
        this.coordinateTransformer = coordinateTransformer;
        this.pathologyAnalysisService = pathologyAnalysisService;
        this.treatmentRecommendationService = treatmentRecommendationService;
        this.persistenceService = persistenceService;
        this.idempotencyService = idempotencyService;
        this.diagnosisRepository = diagnosisRepository;
        this.diagnosisIoExecutor = diagnosisIoExecutor;
    }

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

        // 2. Idempotency & Quota Guard (authenticated users only; guests are never cached)
        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis(userId, imageHash);
        if (cached.isPresent()) {
            log.info("Idempotent diagnosis hit for user='{}', imageHash='{}', replaying cached diagnosisId='{}' (Quota saved!)",
                    userId, imageHash, cached.get().diagnosisId());
            return cached.get();
        }

        log.info("Starting diagnosis pipeline for file='{}' ({} bytes, hash='{}'), cropHint='{}', device='{}', user='{}'",
                filename, imageBytes.length, imageHash, plantCodeHint, deviceInfo, userId);

        // 3. Parallel I/O via managed Java 21 Virtual Threads with deterministic Saga S3 cleanup
        ParallelIoOutcome ioOutcome = executeParallelIo(imageBytes, filename);
        String s3ImageUrl = ioOutcome.s3ImageUrl();
        InferenceResult inferenceResult = ioOutcome.inferenceResult();
        int inferenceTimeMs = ioOutcome.inferenceTimeMs();

        // 4-7. Domain Resolution & Transactional Persistence (Saga: delete S3 object on any failure)
        List<BoundingBoxDto> boundingBoxDtos;
        PathologyResolution pathology;
        List<TreatmentDto> treatments;
        Diagnosis saved;
        try {
            // 4. Coordinate Transformation (SRP)
            boundingBoxDtos = coordinateTransformer.toBoundingBoxDtos(
                    inferenceResult.detections(),
                    inferenceResult.imageWidth(),
                    inferenceResult.imageHeight()
            );

            // 5. Pathology & Crop Analysis (SRP)
            pathology = pathologyAnalysisService.resolvePathology(
                    inferenceResult.detections(),
                    plantCodeHint
            );

            // 6. Treatment Recommendations (SRP)
            treatments = resolveTreatments(inferenceResult, pathology, plantCodeHint);

            // 7. Transactional Persistence (SRP)
            saved = persistenceService.saveDiagnosis(new DiagnosisSaveCommand(
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
        } catch (RuntimeException ex) {
            cleanupUploadedImageSafely(s3ImageUrl, "downstream diagnosis processing/persistence failed");
            throw ex;
        }

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

    private ParallelIoOutcome executeParallelIo(byte[] imageBytes, String filename) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(
                () -> storageService.uploadImage(imageBytes, filename, "diagnoses"),
                diagnosisIoExecutor
        );
        CompletableFuture<InferenceResult> inferenceFuture = CompletableFuture.supplyAsync(
                () -> aiInferenceProvider.inferPlantDisease(imageBytes, null),
                diagnosisIoExecutor
        );

        // Await BOTH futures deterministically so an early inference error still cleans up an in-flight upload
        // and an upload failure never leaks an orphaned inference virtual thread.
        String s3ImageUrl = null;
        Throwable uploadFailure = null;
        try {
            s3ImageUrl = uploadFuture.join();
        } catch (Throwable t) {
            uploadFailure = unwrapAsyncException(t);
        }

        InferenceResult inferenceResult = null;
        Throwable inferenceFailure = null;
        try {
            inferenceResult = inferenceFuture.join();
        } catch (Throwable t) {
            inferenceFailure = unwrapAsyncException(t);
        }

        if (uploadFailure != null) {
            log.error("Object storage upload failed during parallel diagnosis I/O", uploadFailure);
            if (inferenceFailure != null) {
                log.warn("Suppressing secondary AI inference failure after upload failure: {}", inferenceFailure.getMessage());
            }
            propagateUploadFailure(uploadFailure);
        }

        if (inferenceFailure != null) {
            log.error("AI inference failed during parallel diagnosis I/O", inferenceFailure);
            cleanupUploadedImageSafely(s3ImageUrl, "AI inference failed");
            propagateInferenceFailure(inferenceFailure);
        }

        int inferenceTimeMs = (int) (System.currentTimeMillis() - startTime);
        return new ParallelIoOutcome(s3ImageUrl, inferenceResult, inferenceTimeMs);
    }

    private Throwable unwrapAsyncException(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void propagateUploadFailure(Throwable failure) {
        if (failure instanceof BusinessException be) {
            throw be;
        }
        if (failure instanceof RuntimeException re) {
            throw re;
        }
        if (failure instanceof Error err) {
            throw err;
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Object storage upload failed: " + failure.getMessage());
    }

    private void propagateInferenceFailure(Throwable failure) {
        if (failure instanceof BusinessException be) {
            throw be;
        }
        if (failure instanceof Error err) {
            throw err;
        }
        throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Diagnosis pipeline failed: " + failure.getMessage());
    }

    private void cleanupUploadedImageSafely(String s3Url, String reason) {
        if (s3Url == null || s3Url.isBlank()) {
            return;
        }
        try {
            storageService.deleteImage(s3Url);
            log.info("Saga compensating cleanup deleted orphaned image '{}' (reason={})", s3Url, reason);
        } catch (Exception cleanupEx) {
            log.warn("Saga compensating delete failed for image '{}' (reason={}): {}",
                    s3Url, reason, cleanupEx.getMessage());
        }
    }

    private List<TreatmentDto> resolveTreatments(
            InferenceResult inferenceResult,
            PathologyResolution pathology,
            String plantCodeHint
    ) {
        if (!inferenceResult.hasDetections()) {
            return Collections.emptyList();
        }
        if (pathology.isHealthy()) {
            return treatmentRecommendationService.getHealthyCareTreatments(plantCodeHint);
        }
        if (pathology.dominantDiseaseEntity() != null) {
            return treatmentRecommendationService.getTreatmentsForDisease(pathology.dominantDiseaseEntity().getId());
        }
        return Collections.emptyList();
    }

    private record ParallelIoOutcome(String s3ImageUrl, InferenceResult inferenceResult, int inferenceTimeMs) {}

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
