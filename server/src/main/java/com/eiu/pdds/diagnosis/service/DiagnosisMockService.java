package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mock diagnosis service implementing the API contract for leaf image analysis.
 * Transforms Roboflow Computer Vision detections into dual-coordinate bounding boxes
 * (Pixel & Top-Left Normalized [0.0 - 1.0]), identifies dominant pathology by Max Confidence,
 * and attaches curated agricultural treatment protocols.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisMockService {

    private final DiseaseRepository diseaseRepository;
    private final TreatmentRepository treatmentRepository;

    public DiagnosisAnalyzeResponse analyzeImage(MultipartFile image, String plantCode, Double latitude, Double longitude, String deviceInfo) {
        log.info("Received leaf image analysis request: filename={}, size={} bytes, plantCode={}, lat={}, lon={}, device={}",
                image != null ? image.getOriginalFilename() : "sample.jpg",
                image != null ? image.getSize() : 0,
                plantCode, latitude, longitude, deviceInfo);

        int imageWidth = 399;
        int imageHeight = 501;

        // Raw predictions from Roboflow model output sample
        List<RawPrediction> rawDetections = List.of(
                new RawPrediction("31faab02-9bd3-47fe-8258-70e577b27006", "Potato leaf late blight", "POTATO_LATE_BLIGHT", 0.7994398474693298, 322.0, 426.0, 154.0, 150.0),
                new RawPrediction("fefb09d5-fae1-49f9-b2b8-edddfb696a0d", "Potato leaf late blight", "POTATO_LATE_BLIGHT", 0.6795565485954285, 269.0, 128.0, 138.0, 254.0),
                new RawPrediction("b70dd935-bc19-4044-b1cc-42b832671e6a", "Potato leaf late blight", "POTATO_LATE_BLIGHT", 0.5705733895301819, 358.0, 142.5, 82.0, 255.0),
                new RawPrediction("ea11cf2c-127a-427b-8563-0b4b8f369ebc", "Potato leaf early blight", "POTATO_EARLY_BLIGHT", 0.5202842354774475, 99.0, 336.0, 198.0, 204.0),
                new RawPrediction("7066cc8e-d7e4-464e-9cff-91954cccb91d", "Potato leaf late blight", "POTATO_LATE_BLIGHT", 0.4390724301338196, 125.5, 226.5, 243.0, 245.0)
        );

        // Convert to standardized BoundingBoxDto with dual coordinate systems
        List<BoundingBoxDto> boundingBoxes = rawDetections.stream()
                .map(p -> toBoundingBoxDto(p, imageWidth, imageHeight))
                .toList();

        // 1. Dominant Disease: Pick detection with maximum confidence
        RawPrediction maxConfDetection = rawDetections.stream()
                .max((a, b) -> Double.compare(a.confidence(), b.confidence()))
                .orElse(rawDetections.get(0));

        DiseaseSummaryDto dominantDisease = resolveDiseaseSummary(maxConfDetection.diseaseCode(), maxConfDetection.label(), Severity.CRITICAL);
        double dominantConfidence = round(maxConfDetection.confidence(), 4);

        // 2. Co-occurring Diseases (Distinct secondary pathologies present in detections)
        List<DiseaseSummaryDto> coOccurringDiseases = rawDetections.stream()
                .map(RawPrediction::diseaseCode)
                .filter(code -> !code.equals(dominantDisease.diseaseCode()))
                .distinct()
                .map(code -> resolveDiseaseSummary(code, "Potato leaf early blight", Severity.HIGH))
                .toList();

        // 3. Recommended Treatment Protocols for Dominant Disease
        List<TreatmentDto> treatments = fetchTreatments(dominantDisease.diseaseCode());

        return new DiagnosisAnalyzeResponse(
                UUID.fromString("55555555-5555-5555-5555-555555555501"),
                "https://s3.eiu.edu.vn/eiu-pdds-plants/potato-leaf-sample.jpg",
                imageWidth,
                imageHeight,
                dominantDisease,
                dominantConfidence,
                boundingBoxes,
                coOccurringDiseases,
                treatments,
                285,
                "COMPLETED",
                Instant.now()
        );
    }

    private BoundingBoxDto toBoundingBoxDto(RawPrediction p, int imgW, int imgH) {
        double xMin = p.x() - (p.width() / 2.0);
        double yMin = p.y() - (p.height() / 2.0);

        double xMinNorm = Math.max(0.0, Math.min(1.0, xMin / (double) imgW));
        double yMinNorm = Math.max(0.0, Math.min(1.0, yMin / (double) imgH));
        double wNorm = Math.max(0.0, Math.min(1.0, p.width() / (double) imgW));
        double hNorm = Math.max(0.0, Math.min(1.0, p.height() / (double) imgH));

        return new BoundingBoxDto(
                UUID.fromString(p.id()),
                p.label(),
                p.diseaseCode(),
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

    private DiseaseSummaryDto resolveDiseaseSummary(String code, String fallbackName, Severity fallbackSeverity) {
        return diseaseRepository.findByDiseaseCode(code)
                .map(d -> new DiseaseSummaryDto(d.getId(), d.getDiseaseCode(), d.getCommonName(), d.getScientificName(), d.getSeverity()))
                .orElse(new DiseaseSummaryDto(UUID.randomUUID(), code, fallbackName, "Phytophthora infestans", fallbackSeverity));
    }

    private List<TreatmentDto> fetchTreatments(String diseaseCode) {
        return diseaseRepository.findByDiseaseCode(diseaseCode)
                .map(disease -> treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(disease.getId()).stream()
                        .map(this::toTreatmentDto)
                        .toList())
                .filter(list -> !list.isEmpty())
                .orElseGet(this::createFallbackTreatments);
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

    private List<TreatmentDto> createFallbackTreatments() {
        return List.of(
                new TreatmentDto(
                        UUID.fromString("44444444-4444-4444-4444-444444444401"),
                        TreatmentType.CHEMICAL,
                        "Metalaxyl + Mancozeb Systemic Fungicide Spray",
                        "Spray thoroughly on both upper and lower leaf surfaces upon first appearance of symptoms. Reapply after 7-10 days if humid conditions persist.",
                        "Metalaxyl 8% + Mancozeb 64%",
                        "50g per 16L sprayer (2.5 - 3.0 kg/ha)",
                        7,
                        "Do not apply more than 3 consecutive sprays of systemic phenylamides per season to avoid chemical resistance."
                ),
                new TreatmentDto(
                        UUID.fromString("44444444-4444-4444-4444-444444444402"),
                        TreatmentType.BIOLOGICAL,
                        "Bacillus subtilis & Copper Octanoate Biopesticide",
                        "Apply as preventive spray or early stage treatment. Enhances plant defense and forms protective barrier on foliage.",
                        "Bacillus subtilis QST 713 + Copper Octanoate 10%",
                        "30-40ml per 16L water",
                        1,
                        "Safe for beneficial insects; avoid spraying during intense midday sunlight."
                )
        );
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private record RawPrediction(
            String id,
            String label,
            String diseaseCode,
            double confidence,
            double x,
            double y,
            double width,
            double height
    ) {}
}
