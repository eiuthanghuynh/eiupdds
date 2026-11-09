package com.eiu.pdds.diagnosis.repository;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.dto.diagnosis.DiagnosisHistoryFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DiagnosisSpecifications {

    private DiagnosisSpecifications() {
    }

    public static Specification<Diagnosis> withFilter(UUID userId, DiagnosisHistoryFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter != null) {
                if (filter.plantCode() != null && !filter.plantCode().isBlank()) {
                    predicates.add(cb.equal(
                            cb.upper(root.get("plant").get("plantCode")),
                            filter.plantCode().trim().toUpperCase()
                    ));
                }

                if (filter.diseaseCode() != null && !filter.diseaseCode().isBlank()) {
                    String normalizedDiseaseCode = filter.diseaseCode().trim().toUpperCase();
                    if (normalizedDiseaseCode.endsWith("_HEALTHY")) {
                        predicates.add(cb.isNull(root.get("dominantDisease")));
                        predicates.add(cb.isNotEmpty(root.get("boundingBoxes")));
                        String plantPrefix = normalizedDiseaseCode.substring(0, normalizedDiseaseCode.length() - "_HEALTHY".length()).trim();
                        if (!plantPrefix.isBlank() && !plantPrefix.equals("CROP")) {
                            predicates.add(cb.equal(
                                    cb.upper(root.get("plant").get("plantCode")),
                                    plantPrefix
                            ));
                        }
                    } else {
                        predicates.add(cb.equal(
                                cb.upper(root.get("dominantDisease").get("diseaseCode")),
                                normalizedDiseaseCode
                        ));
                    }
                }

                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }

                if (filter.fromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
                }

                if (filter.toDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.toDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
