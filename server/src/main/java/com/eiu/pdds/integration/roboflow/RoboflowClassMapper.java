package com.eiu.pdds.integration.roboflow;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * High-performance, immutable class mapper for Roboflow model 'plant-leaf-diseases/10'.
 * Maps raw model detection labels into standardized plant codes, disease codes,
 * and pure English botanical/pathological names without Vietnamese translations.
 */
@Component
public class RoboflowClassMapper {

    public record ClassMetadata(
            String rawLabel,
            String plantCode,
            String diseaseCode,
            String englishName,
            boolean isHealthy
    ) {}

    private static final Map<String, ClassMetadata> LABEL_MAP;

    static {
        Map<String, ClassMetadata> map = new HashMap<>();

        // 1. Apple (Malus domestica)
        register(map, "Apple leaf", "APPLE", "APPLE_HEALTHY", "Healthy apple leaf", true);
        register(map, "Apple Scab Leaf", "APPLE", "APPLE_SCAB", "Apple scab", false);
        register(map, "Apple rust leaf", "APPLE", "APPLE_RUST", "Cedar apple rust", false);

        // 2. Bell Pepper (Capsicum annuum)
        register(map, "Bell_pepper leaf", "BELL_PEPPER", "BELL_PEPPER_HEALTHY", "Healthy bell pepper leaf", true);
        register(map, "Bell_pepper leaf spot", "BELL_PEPPER", "BELL_PEPPER_LEAF_SPOT", "Bell pepper bacterial leaf spot", false);

        // 3. Blueberry (Vaccinium corymbosum)
        register(map, "Blueberry leaf", "BLUEBERRY", "BLUEBERRY_HEALTHY", "Healthy blueberry leaf", true);

        // 4. Cherry (Prunus avium)
        register(map, "Cherry leaf", "CHERRY", "CHERRY_HEALTHY", "Healthy cherry leaf", true);

        // 5. Corn (Zea mays)
        register(map, "Corn Gray leaf spot", "CORN", "CORN_GRAY_LEAF_SPOT", "Corn gray leaf spot", false);
        register(map, "Corn leaf blight", "CORN", "CORN_LEAF_BLIGHT", "Corn northern leaf blight", false);
        register(map, "Corn rust leaf", "CORN", "CORN_COMMON_RUST", "Corn common rust", false);

        // 6. Grape (Vitis vinifera)
        register(map, "grape leaf", "GRAPE", "GRAPE_HEALTHY", "Healthy grape leaf", true);
        register(map, "grape leaf black rot", "GRAPE", "GRAPE_BLACK_ROT", "Grape black rot", false);

        // 7. Peach (Prunus persica)
        register(map, "Peach leaf", "PEACH", "PEACH_HEALTHY", "Healthy peach leaf", true);

        // 8. Potato (Solanum tuberosum)
        register(map, "Potato leaf", "POTATO", "POTATO_HEALTHY", "Healthy potato leaf", true);
        register(map, "Potato leaf early blight", "POTATO", "POTATO_EARLY_BLIGHT", "Potato early blight", false);
        register(map, "Potato leaf late blight", "POTATO", "POTATO_LATE_BLIGHT", "Potato late blight", false);

        // 9. Raspberry (Rubus idaeus)
        register(map, "Raspberry leaf", "RASPBERRY", "RASPBERRY_HEALTHY", "Healthy raspberry leaf", true);

        // 10. Soybean (Glycine max) - supports both alternate label spellings in Roboflow dataset
        register(map, "Soybean leaf", "SOYBEAN", "SOYBEAN_HEALTHY", "Healthy soybean leaf", true);
        register(map, "Soyabean leaf", "SOYBEAN", "SOYBEAN_HEALTHY", "Healthy soybean leaf", true);

        // 11. Squash (Cucurbita pepo)
        register(map, "Squash Powdery mildew leaf", "SQUASH", "SQUASH_POWDERY_MILDEW", "Squash powdery mildew", false);

        // 12. Strawberry (Fragaria ananassa)
        register(map, "Strawberry leaf", "STRAWBERRY", "STRAWBERRY_HEALTHY", "Healthy strawberry leaf", true);

        // 13. Tomato (Solanum lycopersicum)
        register(map, "Tomato leaf", "TOMATO", "TOMATO_HEALTHY", "Healthy tomato leaf", true);
        register(map, "Tomato Early blight leaf", "TOMATO", "TOMATO_EARLY_BLIGHT", "Tomato early blight", false);
        register(map, "Tomato leaf late blight", "TOMATO", "TOMATO_LATE_BLIGHT", "Tomato late blight", false);
        register(map, "Tomato leaf bacterial spot", "TOMATO", "TOMATO_BACTERIAL_SPOT", "Tomato bacterial spot", false);
        register(map, "Tomato leaf mosaic virus", "TOMATO", "TOMATO_MOSAIC_VIRUS", "Tomato mosaic virus", false);
        register(map, "Tomato leaf yellow virus", "TOMATO", "TOMATO_YELLOW_VIRUS", "Tomato yellow leaf curl virus", false);
        register(map, "Tomato mold leaf", "TOMATO", "TOMATO_LEAF_MOLD", "Tomato leaf mold", false);
        register(map, "Tomato Septoria leaf spot", "TOMATO", "TOMATO_SEPTORIA_LEAF_SPOT", "Tomato Septoria leaf spot", false);
        register(map, "Tomato two spotted spider mites leaf", "TOMATO", "TOMATO_SPIDER_MITES", "Tomato two-spotted spider mites", false);

        LABEL_MAP = Collections.unmodifiableMap(map);
    }

    private static void register(Map<String, ClassMetadata> map, String label, String plantCode, String diseaseCode, String englishName, boolean isHealthy) {
        ClassMetadata meta = new ClassMetadata(label, plantCode, diseaseCode, englishName, isHealthy);
        map.put(label.toLowerCase().trim(), meta);
    }

    /**
     * Resolves metadata for a raw Roboflow prediction label (case-insensitive & trimmed).
     */
    public Optional<ClassMetadata> resolve(String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(rawLabel.toLowerCase().trim()));
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
}
