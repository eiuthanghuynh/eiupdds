package com.eiu.pdds.diagnosis.repository;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoundingBoxRepository extends JpaRepository<BoundingBox, UUID> {

    List<BoundingBox> findByDiagnosisId(UUID diagnosisId);

    List<BoundingBox> findByDiseaseId(UUID diseaseId);

    @Query("SELECT b.diagnosis.id, COUNT(b) FROM BoundingBox b WHERE b.diagnosis.id IN :diagnosisIds GROUP BY b.diagnosis.id")
    List<Object[]> countByDiagnosisIds(@Param("diagnosisIds") Collection<UUID> diagnosisIds);
}
