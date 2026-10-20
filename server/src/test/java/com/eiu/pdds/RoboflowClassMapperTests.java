package com.eiu.pdds;

import com.eiu.pdds.integration.roboflow.RoboflowClassMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoboflowClassMapperTests {

    private RoboflowClassMapper mapper;

    // The official 30 classes from Roboflow Universe model (college-whx3b/plant-leaf-diseases/10)
    private static final List<String> OFFICIAL_30_CLASSES = List.of(
            "Apple Scab Leaf",
            "Apple leaf",
            "Apple rust leaf",
            "Bell_pepper leaf",
            "Bell_pepper leaf spot",
            "Blueberry leaf",
            "Cherry leaf",
            "Corn Gray leaf spot",
            "Corn leaf blight",
            "Corn rust leaf",
            "Peach leaf",
            "Potato leaf",
            "Potato leaf early blight",
            "Potato leaf late blight",
            "Raspberry leaf",
            "Soyabean leaf",
            "Soybean leaf",
            "Squash Powdery mildew leaf",
            "Strawberry leaf",
            "Tomato Early blight leaf",
            "Tomato Septoria leaf spot",
            "Tomato leaf",
            "Tomato leaf bacterial spot",
            "Tomato leaf late blight",
            "Tomato leaf mosaic virus",
            "Tomato leaf yellow virus",
            "Tomato mold leaf",
            "Tomato two spotted spider mites leaf",
            "grape leaf",
            "grape leaf black rot"
    );

    @BeforeEach
    void setUp() {
        mapper = new RoboflowClassMapper();
    }

    @Test
    @DisplayName("Should resolve all 30 official Roboflow classes without missing mappings")
    void testAllOfficialClassesMapped() {
        for (String rawLabel : OFFICIAL_30_CLASSES) {
            Optional<RoboflowClassMapper.ClassMetadata> meta = mapper.resolve(rawLabel);
            assertThat(meta)
                    .as("Expected mapping to exist for label: %s", rawLabel)
                    .isPresent();

            RoboflowClassMapper.ClassMetadata mapping = meta.get();
            assertThat(mapping.plantCode()).isNotBlank();
            assertThat(mapping.diseaseCode()).isNotBlank();
            assertThat(mapping.englishName()).isNotBlank();

            // Assert English names do not contain Vietnamese keywords
            assertThat(mapping.englishName())
                    .as("Name should be in English: %s", mapping.englishName())
                    .doesNotContain("bệnh", "lá", "cà chua", "khoai tây", "ngô", "lúa");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "Potato leaf late blight, POTATO, POTATO_LATE_BLIGHT, false",
            "Potato leaf, POTATO, POTATO_HEALTHY, true",
            "Tomato Early blight leaf, TOMATO, TOMATO_EARLY_BLIGHT, false",
            "Tomato leaf, TOMATO, TOMATO_HEALTHY, true",
            "Corn Gray leaf spot, CORN, CORN_GRAY_LEAF_SPOT, false",
            "Corn rust leaf, CORN, CORN_COMMON_RUST, false",
            "Apple Scab Leaf, APPLE, APPLE_SCAB, false",
            "Bell_pepper leaf spot, BELL_PEPPER, BELL_PEPPER_LEAF_SPOT, false",
            "grape leaf black rot, GRAPE, GRAPE_BLACK_ROT, false",
            "Squash Powdery mildew leaf, SQUASH, SQUASH_POWDERY_MILDEW, false"
    })
    @DisplayName("Should map specific raw labels to expected codes and healthy flags")
    void testSpecificLabelMappings(String rawLabel, String expectedPlant, String expectedDisease, boolean expectedHealthy) {
        RoboflowClassMapper.ClassMetadata meta = mapper.resolve(rawLabel).orElseThrow();
        assertThat(meta.plantCode()).isEqualTo(expectedPlant);
        assertThat(meta.diseaseCode()).isEqualTo(expectedDisease);
        assertThat(meta.isHealthy()).isEqualTo(expectedHealthy);
    }

    @Test
    @DisplayName("Should handle case-insensitive matching and whitespace trimming")
    void testCaseInsensitivityAndTrimming() {
        Optional<RoboflowClassMapper.ClassMetadata> meta = mapper.resolve("   tOmAtO lEaF lAtE bLiGhT   ");
        assertThat(meta).isPresent();
        assertThat(meta.get().diseaseCode()).isEqualTo("TOMATO_LATE_BLIGHT");
        assertThat(meta.get().plantCode()).isEqualTo("TOMATO");
    }

    @Test
    @DisplayName("Should fallback gracefully when raw label is unknown or blank")
    void testFallbackForUnknownOrBlank() {
        assertThat(mapper.resolve("Unknown Alien Fungus")).isEmpty();
        assertThat(mapper.resolveDiseaseCode("Unknown Alien Fungus")).isEqualTo("UNKNOWN_ALIEN_FUNGUS");
        assertThat(mapper.resolvePlantCode("Unknown Alien Fungus")).isNull();
        assertThat(mapper.resolveEnglishName("Unknown Alien Fungus")).isEqualTo("Unknown Alien Fungus");

        assertThat(mapper.resolve("")).isEmpty();
        assertThat(mapper.resolve(null)).isEmpty();
    }
}
