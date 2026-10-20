package com.eiu.pdds.treatment.repository;

import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

    List<Treatment> findByDiseaseIdAndDeletedAtIsNull(UUID diseaseId);

    List<Treatment> findByDiseaseIdAndPlantIdAndDeletedAtIsNull(UUID diseaseId, UUID plantId);

    List<Treatment> findByTreatmentTypeAndDeletedAtIsNull(TreatmentType treatmentType);

    List<Treatment> findByDeletedAtIsNull();
}
