package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.common.util.FileSecurityValidator;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.DiagnosisService;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.integration.roboflow.RoboflowClassMapper;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Production implementation of DiagnosisService.
 * Executes parallel I/O over Java 21 Virtual Threads, transforms dual coordinates,
 * identifies dominant pathology, enforces strict database integrity, and persists
 * transactional diagnosis audit records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private final RoboflowClient roboflowClient;
    private final StorageService storageService;
    private final RoboflowClassMapper classMapper;
    private final DiagnosisRepository diagnosisRepository;
    private final DiseaseRepository diseaseRepository;
    private final PlantRepository plantRepository;
    private final TreatmentRepository treatmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DiagnosisAnalyzeResponse analyzeImage(
            MultipartFile image,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo
    ) {
        // 1. Validate file security (Magic Bytes, size <= 15MB) and extract byte buffer
        byte[] imageBytes = FileSecurityValidator.validateAndExtractBytes(image);
        String originalFilename = image.getOriginalFilename();

        log.info("Starting diagnosis pipeline for file='{}' ({} bytes), plantCodeHint='{}', device='{}'",
                originalFilename, imageBytes.length, plantCodeHint, deviceInfo);

        // 2. Parallel I/O via Java 21 Virtual Threads (MinIO Upload & Roboflow AI Inference)
        long startTime = System.currentTimeMillis();

        CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(
                () -> storageService.uploadImage(imageBytes, originalFilename, "diagnoses")
        );

        CompletableFuture<RoboflowWorkflowResponse.ModelOutput> inferenceFuture = CompletableFuture.supplyAsync(
                () -> roboflowClient.inferPlantDisease(imageBytes, null)
        );

        String s3ImageUrl;
        RoboflowWorkflowResponse.ModelOutput modelOutput;

        try {
            CompletableFuture.allOf(uploadFuture, inferenceFuture).join();
            s3ImageUrl = uploadFuture.get();
            modelOutput = inferenceFuture.get();
        } catch (Exception e) {
            log.error("Parallel I/O failure during diagnosis analysis: ", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Diagnosis pipeline failed: " + cause.getMessage());
        }

        int inferenceTimeMs = (int) (System.currentTimeMillis() - startTime);

        // 3. Extract dimensions & predictions
        int imageWidth = (modelOutput.image() != null && modelOutput.image().width() > 0)
                ? modelOutput.image().width()
                : 399;
        int imageHeight = (modelOutput.image() != null && modelOutput.image().height() > 0)
                ? modelOutput.image().height()
                : 501;

        List<RoboflowWorkflowResponse.Prediction> predictions = modelOutput.predictions();

        // 4. Handle Case: Zero Detections (empty bounding boxes)
        if (predictions == null || predictions.isEmpty()) {
            log.info("Roboflow model returned zero detections for image key={}", s3ImageUrl);
            return handleZeroDetections(s3ImageUrl, imageWidth, imageHeight, inferenceTimeMs,
                    plantCodeHint, latitude, longitude, deviceInfo, modelOutput);
        }

        // 5. Transform all predictions into BoundingBoxDto (Dual Coordinates: Pixel & Normalized)
        List<BoundingBoxDto> boundingBoxDtos = predictions.stream()
                .map(p -> toBoundingBoxDto(p, imageWidth, imageHeight))
                .toList();

        // 6. Dominant Pathology Resolution (Max Confidence detection)
        RoboflowWorkflowResponse.Prediction dominantPrediction = predictions.stream()
                .max(Comparator.comparingDouble(RoboflowWorkflowResponse.Prediction::confidence))
                .orElse(predictions.get(0));

        String dominantLabel = dominantPrediction.className();
        RoboflowClassMapper.ClassMetadata dominantMeta = classMapper.resolve(dominantLabel).orElse(null);
        String dominantDiseaseCode = classMapper.resolveDiseaseCode(dominantLabel);
        double dominantConfidence = round(dominantPrediction.confidence(), 4);

        Disease dominantDiseaseEntity = null;
        DiseaseSummaryDto dominantDiseaseDto;
        List<TreatmentDto> treatments;

        if (dominantMeta != null && dominantMeta.isHealthy()) {
            // Case 4A: Healthy Leaf Detected
            log.info("Dominant detection is Healthy Foliage: code='{}', label='{}'", dominantDiseaseCode, dominantLabel);
            dominantDiseaseDto = new DiseaseSummaryDto(
                    null,
                    dominantDiseaseCode,
                    dominantMeta.englishName(),
                    "Healthy foliage (No pathology detected)",
                    Severity.LOW
            );
            treatments = List.of(createHealthyCareTreatment(plantCodeHint));
        } else {
            // Case 4B: Diseased Leaf - Strict DB Catalog Lookup (Throw 404 if not found)
            dominantDiseaseEntity = diseaseRepository.findByDiseaseCode(dominantDiseaseCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DISEASE_NOT_FOUND,
                            "Pathology not registered in catalog: " + dominantDiseaseCode));

            dominantDiseaseDto = new DiseaseSummaryDto(
                    dominantDiseaseEntity.getId(),
                    dominantDiseaseEntity.getDiseaseCode(),
                    dominantDiseaseEntity.getCommonName(),
                    dominantDiseaseEntity.getScientificName(),
                    dominantDiseaseEntity.getSeverity()
            );

            treatments = treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(dominantDiseaseEntity.getId()).stream()
                    .map(this::toTreatmentDto)
                    .toList();
        }

        // 7. Co-occurring Pathologies (Distinct secondary pathologies)
        List<DiseaseSummaryDto> coOccurringDiseases = predictions.stream()
                .map(p -> classMapper.resolveDiseaseCode(p.className()))
                .filter(code -> !code.equalsIgnoreCase(dominantDiseaseCode))
                .distinct()
                .map(code -> diseaseRepository.findByDiseaseCode(code)
                        .map(d -> new DiseaseSummaryDto(d.getId(), d.getDiseaseCode(), d.getCommonName(), d.getScientificName(), d.getSeverity()))
                        .orElseGet(() -> new DiseaseSummaryDto(null, code, classMapper.resolveEnglishName(code), "Co-occurring pathology", Severity.MODERATE)))
                .toList();

        // 8. Resolve Plant Crop Entity
        Plant plantEntity = resolvePlant(plantCodeHint, dominantLabel);

        // 9. Persist Transactional Diagnosis & BoundingBoxes in PostgreSQL
        String rawAiJson = serializeRawOutputSafely(modelOutput);

        Diagnosis diagnosis = Diagnosis.builder()
                .user(null) // Anonymous farmer in Sprint 2 (Per Grill-me decision #3)
                .plant(plantEntity)
                .originalImageUrl(s3ImageUrl)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .latitude(latitude)
                .longitude(longitude)
                .deviceInfo(deviceInfo)
                .dominantDisease(dominantDiseaseEntity)
                .dominantConfidence(dominantConfidence)
                .inferenceTimeMs(inferenceTimeMs)
                .rawAiResponse(rawAiJson)
                .status(DiagnosisStatus.COMPLETED)
                .build();

        for (BoundingBoxDto boxDto : boundingBoxDtos) {
            Disease boxDisease = (boxDto.diseaseCode() != null && !boxDto.diseaseCode().endsWith("_HEALTHY"))
                    ? diseaseRepository.findByDiseaseCode(boxDto.diseaseCode()).orElse(null)
                    : null;

            BoundingBox box = BoundingBox.builder()
                    .disease(boxDisease)
                    .label(boxDto.label())
                    .confidence(boxDto.confidence())
                    .x(boxDto.x())
                    .y(boxDto.y())
                    .width(boxDto.width())
                    .height(boxDto.height())
                    .build();

            diagnosis.addBoundingBox(box);
        }

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        log.info("Persisted diagnosis record id='{}' with {} bounding boxes",
                savedDiagnosis.getId(), savedDiagnosis.getBoundingBoxes().size());

        // 10. Assemble and Return Response DTO
        return new DiagnosisAnalyzeResponse(
                savedDiagnosis.getId(),
                s3ImageUrl,
                imageWidth,
                imageHeight,
                dominantDiseaseDto,
                dominantConfidence,
                boundingBoxDtos,
                coOccurringDiseases,
                treatments,
                inferenceTimeMs,
                savedDiagnosis.getStatus().name(),
                savedDiagnosis.getCreatedAt() != null ? savedDiagnosis.getCreatedAt() : Instant.now()
        );
    }

    private DiagnosisAnalyzeResponse handleZeroDetections(
            String s3ImageUrl,
            int imageWidth,
            int imageHeight,
            int inferenceTimeMs,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo,
            RoboflowWorkflowResponse.ModelOutput modelOutput
    ) {
        Plant plant = resolvePlant(plantCodeHint, null);
        String rawAiJson = serializeRawOutputSafely(modelOutput);

        Diagnosis diagnosis = Diagnosis.builder()
                .user(null)
                .plant(plant)
                .originalImageUrl(s3ImageUrl)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .latitude(latitude)
                .longitude(longitude)
                .deviceInfo(deviceInfo)
                .dominantDisease(null)
                .dominantConfidence(null)
                .inferenceTimeMs(inferenceTimeMs)
                .rawAiResponse(rawAiJson)
                .status(DiagnosisStatus.COMPLETED)
                .build();

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);

        return new DiagnosisAnalyzeResponse(
                savedDiagnosis.getId(),
                s3ImageUrl,
                imageWidth,
                imageHeight,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                inferenceTimeMs,
                savedDiagnosis.getStatus().name(),
                savedDiagnosis.getCreatedAt() != null ? savedDiagnosis.getCreatedAt() : Instant.now()
        );
    }

    private BoundingBoxDto toBoundingBoxDto(RoboflowWorkflowResponse.Prediction p, int imgW, int imgH) {
        double xMin = p.x() - (p.width() / 2.0);
        double yMin = p.y() - (p.height() / 2.0);

        double xMinNorm = Math.max(0.0, Math.min(1.0, xMin / (double) imgW));
        double yMinNorm = Math.max(0.0, Math.min(1.0, yMin / (double) imgH));
        double wNorm = Math.max(0.0, Math.min(1.0, p.width() / (double) imgW));
        double hNorm = Math.max(0.0, Math.min(1.0, p.height() / (double) imgH));

        String label = p.className();
        String diseaseCode = classMapper.resolveDiseaseCode(label);
        UUID boxId = parseUuidSafely(p.detectionId());

        return new BoundingBoxDto(
                boxId,
                label,
                diseaseCode,
                round(p.confidence(), 4),
                round(p.x(), 1),
                round(p.y(), 1),
                round(p.width(), 1),
                round(p.height(), 1),
                round(xMin, 1),
                round(yMin, 1),
                round(xMinNorm, 4),
                round(yMinNorm, 4),
                round(wNorm, 4),
                round(hNorm, 4)
        );
    }

    private Plant resolvePlant(String plantCodeHint, String label) {
        if (plantCodeHint != null && !plantCodeHint.isBlank()) {
            Optional<Plant> plantOpt = plantRepository.findByPlantCode(plantCodeHint.toUpperCase().trim());
            if (plantOpt.isPresent()) {
                return plantOpt.get();
            }
        }
        if (label != null && !label.isBlank()) {
            String plantCode = classMapper.resolvePlantCode(label);
            if (plantCode != null) {
                return plantRepository.findByPlantCode(plantCode).orElse(null);
            }
        }
        return null;
    }

    private TreatmentDto toTreatmentDto(Treatment t) {
        return new TreatmentDto(
                t.getId(),
                t.getTreatmentType(),
                t.getTitle(),
                t.getInstructions(),
                t.getActiveIngredients(),
                t.getDosageRecommendation(),
                t.getSafetyIntervalDays(),
                t.getWarning()
        );
    }

    private TreatmentDto createHealthyCareTreatment(String plantCode) {
        return new TreatmentDto(
                UUID.fromString("44444444-4444-4444-4444-444444444499"),
                TreatmentType.PREVENTIVE,
                "Routine Crop Care & Foliage Monitoring",
                "Maintain adequate planting distance, provide balanced soil nutrition, avoid overhead sprinkler splash, and routinely inspect foliage for early sign of fungal or bacterial sporulation.",
                "None (Natural Cultural Practice)",
                "Standard canopy aeration and drip irrigation at root zone",
                0,
                "Continue routine preventive monitoring during humid morning hours."
        );
    }

    private UUID parseUuidSafely(String str) {
        if (str == null || str.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private String serializeRawOutputSafely(RoboflowWorkflowResponse.ModelOutput modelOutput) {
        try {
            return objectMapper.writeValueAsString(modelOutput);
        } catch (Exception e) {
            log.warn("Failed to serialize modelOutput to JSON: ", e);
            return "{}";
        }
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
