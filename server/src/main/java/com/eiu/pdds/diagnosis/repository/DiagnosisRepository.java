package com.eiu.pdds.diagnosis.repository;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    Page<Diagnosis> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Diagnosis> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    List<Diagnosis> findByStatusAndDeletedAtIsNull(DiagnosisStatus status);

    long countByDominantDiseaseIdAndDeletedAtIsNull(UUID diseaseId);

    List<Diagnosis> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
