package com.eiu.pdds.diagnosis.repository;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoundingBoxRepository extends JpaRepository<BoundingBox, UUID> {

    List<BoundingBox> findByDiagnosisId(UUID diagnosisId);

    List<BoundingBox> findByDiseaseId(UUID diseaseId);
}
