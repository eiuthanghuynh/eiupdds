package com.eiu.pdds.feedback.repository;

import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosisFeedbackRepository extends JpaRepository<DiagnosisFeedback, UUID>, JpaSpecificationExecutor<DiagnosisFeedback> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"diagnosis", "diagnosis.plant", "diagnosis.dominantDisease", "user", "suggestedDisease", "verifiedByUser"})
    Page<DiagnosisFeedback> findAll(@Nullable Specification<DiagnosisFeedback> spec, @NonNull Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"diagnosis", "diagnosis.plant", "diagnosis.dominantDisease", "user", "suggestedDisease", "verifiedByUser"})
    List<DiagnosisFeedback> findAll(@Nullable Specification<DiagnosisFeedback> spec, @NonNull Sort sort);

    @EntityGraph(attributePaths = {"diagnosis", "diagnosis.plant", "diagnosis.dominantDisease", "user", "suggestedDisease", "verifiedByUser"})
    Optional<DiagnosisFeedback> findByDiagnosisIdAndUserIdAndDeletedAtIsNull(UUID diagnosisId, UUID userId);

    @EntityGraph(attributePaths = {"diagnosis", "diagnosis.plant", "diagnosis.dominantDisease", "user", "suggestedDisease", "verifiedByUser"})
    Optional<DiagnosisFeedback> findByIdAndDeletedAtIsNull(UUID id);

    List<DiagnosisFeedback> findByDiagnosisIdAndDeletedAtIsNull(UUID diagnosisId);

    List<DiagnosisFeedback> findByExpertVerifiedFalseAndDeletedAtIsNull();

    List<DiagnosisFeedback> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<DiagnosisFeedback> findByDeletedAtIsNull();
}
