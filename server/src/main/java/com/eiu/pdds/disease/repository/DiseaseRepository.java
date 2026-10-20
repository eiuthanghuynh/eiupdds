package com.eiu.pdds.disease.repository;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, UUID> {

    Optional<Disease> findByDiseaseCode(String diseaseCode);

    boolean existsByDiseaseCode(String diseaseCode);

    List<Disease> findBySeverityAndDeletedAtIsNull(Severity severity);

    List<Disease> findByDeletedAtIsNull();
}
