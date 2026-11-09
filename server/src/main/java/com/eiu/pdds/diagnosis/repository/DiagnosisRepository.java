package com.eiu.pdds.diagnosis.repository;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID>, JpaSpecificationExecutor<Diagnosis> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"plant", "dominantDisease"})
    Page<Diagnosis> findAll(Specification<Diagnosis> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"plant", "dominantDisease", "boundingBoxes", "boundingBoxes.disease"})
    Optional<Diagnosis> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"plant", "dominantDisease", "boundingBoxes", "boundingBoxes.disease"})
    Optional<Diagnosis> findByIdAndDeletedAtIsNull(UUID id);

    Page<Diagnosis> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Diagnosis> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    List<Diagnosis> findByStatusAndDeletedAtIsNull(DiagnosisStatus status);

    long countByDominantDiseaseIdAndDeletedAtIsNull(UUID diseaseId);

    List<Diagnosis> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
