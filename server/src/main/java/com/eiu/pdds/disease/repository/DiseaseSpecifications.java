package com.eiu.pdds.disease.repository;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.PlantDisease;
import com.eiu.pdds.dto.disease.DiseaseFilter;
import com.eiu.pdds.plant.entity.Plant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA criteria specifications for filtering plant disease catalog entities.
 */
public final class DiseaseSpecifications {

    private DiseaseSpecifications() {
    }

    /**
     * Constructs a composite JPA Specification matching active diseases based on optional filter criteria.
     *
     * @param filter filter criteria containing keyword, plantCode, and severity
     * @return JPA Specification for Disease entity
     */
    public static Specification<Disease> withFilter(DiseaseFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce soft-deletion filter to only return active diseases
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter != null) {
                // Keyword search across diseaseCode, commonName, and scientificName
                if (filter.keyword() != null && !filter.keyword().trim().isEmpty()) {
                    String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
                    Predicate keywordPredicate = cb.or(
                            cb.like(cb.lower(root.get("diseaseCode")), pattern),
                            cb.like(cb.lower(root.get("commonName")), pattern),
                            cb.like(cb.lower(root.get("scientificName")), pattern)
                    );
                    predicates.add(keywordPredicate);
                }

                // Filter by associated crop/plant code
                if (filter.plantCode() != null && !filter.plantCode().trim().isEmpty()) {
                    if (query != null) {
                        query.distinct(true);
                    }
                    Join<Disease, PlantDisease> plantDiseaseJoin = root.join("plantDiseases", JoinType.LEFT);
                    Join<PlantDisease, Plant> plantJoin = plantDiseaseJoin.join("plant", JoinType.LEFT);
                    predicates.add(cb.equal(
                            cb.upper(plantJoin.get("plantCode")),
                            filter.plantCode().trim().toUpperCase()
                    ));
                }

                // Filter by pathological severity level
                if (filter.severity() != null) {
                    predicates.add(cb.equal(root.get("severity"), filter.severity()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
