package com.eiu.pdds.disease.repository;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, UUID>, JpaSpecificationExecutor<Disease> {

    Optional<Disease> findByDiseaseCode(String diseaseCode);

    List<Disease> findByDiseaseCodeIn(java.util.Collection<String> diseaseCodes);

    Optional<Disease> findByDiseaseCodeAndDeletedAtIsNull(String diseaseCode);

    Optional<Disease> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByDiseaseCode(String diseaseCode);

    boolean existsByDiseaseCodeAndDeletedAtIsNull(String diseaseCode);

    boolean existsByDiseaseCodeAndIdNotAndDeletedAtIsNull(String diseaseCode, UUID id);

    List<Disease> findBySeverityAndDeletedAtIsNull(Severity severity);

    List<Disease> findByDeletedAtIsNull();
}
