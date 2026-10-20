package com.eiu.pdds.plant.repository;

import com.eiu.pdds.plant.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantRepository extends JpaRepository<Plant, UUID> {

    Optional<Plant> findByPlantCode(String plantCode);

    boolean existsByPlantCode(String plantCode);

    List<Plant> findByDeletedAtIsNull();
}
