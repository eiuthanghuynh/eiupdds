package com.eiu.pdds.disease.repository;

import com.eiu.pdds.disease.entity.PlantDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantDiseaseRepository extends JpaRepository<PlantDisease, UUID> {

    Optional<PlantDisease> findByPlantIdAndDiseaseId(UUID plantId, UUID diseaseId);

    List<PlantDisease> findByPlantId(UUID plantId);

    List<PlantDisease> findByDiseaseId(UUID diseaseId);
}
