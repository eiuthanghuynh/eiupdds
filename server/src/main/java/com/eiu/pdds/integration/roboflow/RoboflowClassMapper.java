package com.eiu.pdds.integration.roboflow;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * High-performance, immutable class mapper for Roboflow model 'college-whx3b/plant-leaf-diseases/10'.
 * Maps raw model detection labels and 0-indexed class IDs (0-29) into standardized plant codes,
 * disease codes, and English botanical/pathological names without Vietnamese translations.
 */
@Component
public class RoboflowClassMapper {

    public record ClassMetadata(
            int classId,
            String rawLabel,
            String plantCode,
            String diseaseCode,
            String englishName,
            boolean isHealthy
    ) {
        public ClassMetadata(String rawLabel, String plantCode, String diseaseCode, String englishName, boolean isHealthy) {
            this(-1, rawLabel, plantCode, diseaseCode, englishName, isHealthy);
        }
    }

    private static final Map<String, ClassMetadata> LABEL_MAP;
    private static final Map<Integer, ClassMetadata> ID_MAP;

    static {
        Map<String, ClassMetadata> labelMap = new HashMap<>();
        Map<Integer, ClassMetadata> idMap = new HashMap<>();

        // Register exact 30 classes with official 0-indexed class_ids (0-29) from Roboflow model metadata
        register(labelMap, idMap, 0, "Apple Scab Leaf", "APPLE", "APPLE_SCAB", "Apple scab", false);
        register(labelMap, idMap, 1, "Apple leaf", "APPLE", "APPLE_HEALTHY", "Healthy apple leaf", true);
        register(labelMap, idMap, 2, "Apple rust leaf", "APPLE", "APPLE_RUST", "Cedar apple rust", false);
        register(labelMap, idMap, 3, "Bell_pepper leaf", "BELL_PEPPER", "BELL_PEPPER_HEALTHY", "Healthy bell pepper leaf", true);
        register(labelMap, idMap, 4, "Bell_pepper leaf spot", "BELL_PEPPER", "BELL_PEPPER_LEAF_SPOT", "Bell pepper bacterial leaf spot", false);
        register(labelMap, idMap, 5, "Blueberry leaf", "BLUEBERRY", "BLUEBERRY_HEALTHY", "Healthy blueberry leaf", true);
        register(labelMap, idMap, 6, "Cherry leaf", "CHERRY", "CHERRY_HEALTHY", "Healthy cherry leaf", true);
        register(labelMap, idMap, 7, "Corn Gray leaf spot", "CORN", "CORN_GRAY_LEAF_SPOT", "Corn gray leaf spot", false);
        register(labelMap, idMap, 8, "Corn leaf blight", "CORN", "CORN_LEAF_BLIGHT", "Corn northern leaf blight", false);
        register(labelMap, idMap, 9, "Corn rust leaf", "CORN", "CORN_COMMON_RUST", "Corn common rust", false);
        register(labelMap, idMap, 10, "Peach leaf", "PEACH", "PEACH_HEALTHY", "Healthy peach leaf", true);
        register(labelMap, idMap, 11, "Potato leaf", "POTATO", "POTATO_HEALTHY", "Healthy potato leaf", true);
        register(labelMap, idMap, 12, "Potato leaf early blight", "POTATO", "POTATO_EARLY_BLIGHT", "Potato early blight", false);
        register(labelMap, idMap, 13, "Potato leaf late blight", "POTATO", "POTATO_LATE_BLIGHT", "Potato late blight", false);
        register(labelMap, idMap, 14, "Raspberry leaf", "RASPBERRY", "RASPBERRY_HEALTHY", "Healthy raspberry leaf", true);
        register(labelMap, idMap, 15, "Soyabean leaf", "SOYBEAN", "SOYBEAN_HEALTHY", "Healthy soybean leaf", true);
        register(labelMap, idMap, 16, "Soybean leaf", "SOYBEAN", "SOYBEAN_HEALTHY", "Healthy soybean leaf", true);
        register(labelMap, idMap, 17, "Squash Powdery mildew leaf", "SQUASH", "SQUASH_POWDERY_MILDEW", "Squash powdery mildew", false);
        register(labelMap, idMap, 18, "Strawberry leaf", "STRAWBERRY", "STRAWBERRY_HEALTHY", "Healthy strawberry leaf", true);
        register(labelMap, idMap, 19, "Tomato Early blight leaf", "TOMATO", "TOMATO_EARLY_BLIGHT", "Tomato early blight", false);
        register(labelMap, idMap, 20, "Tomato Septoria leaf spot", "TOMATO", "TOMATO_SEPTORIA_LEAF_SPOT", "Tomato Septoria leaf spot", false);
        register(labelMap, idMap, 21, "Tomato leaf", "TOMATO", "TOMATO_HEALTHY", "Healthy tomato leaf", true);
        register(labelMap, idMap, 22, "Tomato leaf bacterial spot", "TOMATO", "TOMATO_BACTERIAL_SPOT", "Tomato bacterial spot", false);
        register(labelMap, idMap, 23, "Tomato leaf late blight", "TOMATO", "TOMATO_LATE_BLIGHT", "Tomato late blight", false);
        register(labelMap, idMap, 24, "Tomato leaf mosaic virus", "TOMATO", "TOMATO_MOSAIC_VIRUS", "Tomato mosaic virus", false);
        register(labelMap, idMap, 25, "Tomato leaf yellow virus", "TOMATO", "TOMATO_YELLOW_VIRUS", "Tomato yellow leaf curl virus", false);
        register(labelMap, idMap, 26, "Tomato mold leaf", "TOMATO", "TOMATO_LEAF_MOLD", "Tomato leaf mold", false);
        register(labelMap, idMap, 27, "Tomato two spotted spider mites leaf", "TOMATO", "TOMATO_SPIDER_MITES", "Tomato two-spotted spider mites", false);
        register(labelMap, idMap, 28, "grape leaf", "GRAPE", "GRAPE_HEALTHY", "Healthy grape leaf", true);
        register(labelMap, idMap, 29, "grape leaf black rot", "GRAPE", "GRAPE_BLACK_ROT", "Grape black rot", false);

        LABEL_MAP = Collections.unmodifiableMap(labelMap);
        ID_MAP = Collections.unmodifiableMap(idMap);
    }

    private static void register(
            Map<String, ClassMetadata> labelMap,
            Map<Integer, ClassMetadata> idMap,
            int classId,
            String label,
            String plantCode,
            String diseaseCode,
            String englishName,
            boolean isHealthy
    ) {
        ClassMetadata meta = new ClassMetadata(classId, label, plantCode, diseaseCode, englishName, isHealthy);
        labelMap.put(label.toLowerCase().trim(), meta);
        idMap.put(classId, meta);
    }

    /**
     * Resolves metadata for a raw Roboflow prediction label (case-insensitive & trimmed).
     */
    public static Optional<ClassMetadata> fromRawClassName(String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(rawLabel.toLowerCase().trim()));
    }

    /**
     * Resolves metadata for a numeric 0-indexed Roboflow class_id (0-29).
     */
    public static Optional<ClassMetadata> fromClassId(Integer classId) {
        if (classId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ID_MAP.get(classId));
    }

    /**
     * Resolves metadata for a raw Roboflow prediction label (case-insensitive & trimmed).
     */
    public Optional<ClassMetadata> resolve(String rawLabel) {
        return fromRawClassName(rawLabel);
    }

    /**
     * Resolves metadata for a numeric Roboflow class_id.
     */
    public Optional<ClassMetadata> resolveByClassId(Integer classId) {
        return fromClassId(classId);
    }

    /**
     * Resolves disease code, falling back to a sanitized UPPER_SNAKE_CASE string if unknown.
     */
    public String resolveDiseaseCode(String rawLabel) {
        return resolve(rawLabel)
                .map(ClassMetadata::diseaseCode)
                .orElseGet(() -> rawLabel.toUpperCase().replaceAll("[^A-Z0-9]+", "_"));
    }

    /**
     * Resolves plant code, falling back to null if unknown.
     */
    public String resolvePlantCode(String rawLabel) {
        return resolve(rawLabel)
                .map(ClassMetadata::plantCode)
                .orElse(null);
    }

    /**
     * Resolves English display name.
     */
    public String resolveEnglishName(String rawLabel) {
        return resolve(rawLabel)
                .map(ClassMetadata::englishName)
                .orElse(rawLabel);
    }

    public Map<String, ClassMetadata> getAllMappings() {
        return LABEL_MAP;
    }

    public Map<Integer, ClassMetadata> getAllIdMappings() {
        return ID_MAP;
    }
}
