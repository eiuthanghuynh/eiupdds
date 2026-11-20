package com.eiu.pdds.feedback.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackDiseaseResolverTest {

    @Mock
    private DiseaseRepository diseaseRepository;

    private FeedbackDiseaseResolver resolver;

    private Disease lateBlight;

    @BeforeEach
    void setUp() {
        resolver = new FeedbackDiseaseResolver(diseaseRepository);
        lateBlight = Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato late blight")
                .severity(Severity.CRITICAL)
                .build();
        lateBlight.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Returns null when neither suggestedDiseaseId nor suggestedDiseaseCode is provided")
    void resolve_WhenNeitherProvided_ReturnsNull() {
        Disease resolved = resolver.resolve(false, null, "   ");

        assertThat(resolved).isNull();
        verifyNoInteractions(diseaseRepository);
    }

    @Test
    @DisplayName("Rejects suggestedDiseaseId when isAccurate is true with 400 REQ_001")
    void resolve_WhenIsAccurateTrueAndIdProvided_ThrowsInvalidRequest() {
        UUID diseaseId = lateBlight.getId();

        assertThatThrownBy(() -> resolver.resolve(true, diseaseId, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));

        verifyNoInteractions(diseaseRepository);
    }

    @Test
    @DisplayName("Rejects suggestedDiseaseCode when isAccurate is true with 400 REQ_001")
    void resolve_WhenIsAccurateTrueAndCodeProvided_ThrowsInvalidRequest() {
        assertThatThrownBy(() -> resolver.resolve(true, null, "POTATO_LATE_BLIGHT"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));

        verifyNoInteractions(diseaseRepository);
    }

    @Test
    @DisplayName("Resolves active disease by ID when isAccurate is false")
    void resolve_ById_Success() {
        when(diseaseRepository.findByIdAndDeletedAtIsNull(lateBlight.getId()))
                .thenReturn(Optional.of(lateBlight));

        Disease resolved = resolver.resolve(false, lateBlight.getId(), null);

        assertThat(resolved).isSameAs(lateBlight);
    }

    @Test
    @DisplayName("Resolves active disease by normalized code when isAccurate is false")
    void resolve_ByNormalizedCode_Success() {
        when(diseaseRepository.findByDiseaseCodeAndDeletedAtIsNull("POTATO_LATE_BLIGHT"))
                .thenReturn(Optional.of(lateBlight));

        Disease resolved = resolver.resolve(false, null, "  potato_late_blight ");

        assertThat(resolved).isSameAs(lateBlight);
    }

    @Test
    @DisplayName("Resolves active disease when both matching ID and code are provided case-insensitively")
    void resolve_WhenBothMatchCaseInsensitively_ReturnsDisease() {
        when(diseaseRepository.findByIdAndDeletedAtIsNull(lateBlight.getId()))
                .thenReturn(Optional.of(lateBlight));

        Disease resolved = resolver.resolve(false, lateBlight.getId(), "potato_late_blight");

        assertThat(resolved).isSameAs(lateBlight);
    }

    @Test
    @DisplayName("Throws 400 REQ_001 when both ID and code are provided but refer to different diseases")
    void resolve_WhenIdAndCodeMismatch_ThrowsInvalidRequest() {
        when(diseaseRepository.findByIdAndDeletedAtIsNull(lateBlight.getId()))
                .thenReturn(Optional.of(lateBlight));

        assertThatThrownBy(() -> resolver.resolve(false, lateBlight.getId(), "TOMATO_EARLY_BLIGHT"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
    }

    @Test
    @DisplayName("Throws 404 DISEASE_001 when disease ID is missing or soft-deleted")
    void resolve_WhenIdNotFound_ThrowsDiseaseNotFound() {
        UUID missingId = UUID.randomUUID();
        when(diseaseRepository.findByIdAndDeletedAtIsNull(missingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(false, missingId, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DISEASE_NOT_FOUND));
    }

    @Test
    @DisplayName("Throws 404 DISEASE_001 when disease code is missing or soft-deleted")
    void resolve_WhenCodeNotFound_ThrowsDiseaseNotFound() {
        when(diseaseRepository.findByDiseaseCodeAndDeletedAtIsNull("UNKNOWN_DISEASE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(false, null, "unknown_disease"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DISEASE_NOT_FOUND));
    }
}
