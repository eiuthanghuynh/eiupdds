package com.eiu.pdds.feedback.repository;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.feedback.dto.FeedbackQueueFilter;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.plant.entity.Plant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic JPA Specifications for querying active diagnosis feedback moderation queues and exports.
 */
public final class FeedbackSpecifications {

    private FeedbackSpecifications() {
    }

    public static Specification<DiagnosisFeedback> withFilter(FeedbackQueueFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));
            Join<DiagnosisFeedback, Diagnosis> diagnosisJoin = root.join("diagnosis", JoinType.INNER);
            predicates.add(cb.isNull(diagnosisJoin.get("deletedAt")));

            if (filter == null) {
                return cb.and(predicates.toArray(Predicate[]::new));
            }

            if (filter.expertVerified() != null) {
                predicates.add(cb.equal(root.get("expertVerified"), filter.expertVerified()));
            }

            if (filter.isAccurate() != null) {
                predicates.add(cb.equal(root.get("isAccurate"), filter.isAccurate()));
            }

            if (StringUtils.hasText(filter.plantCode())) {
                Join<Diagnosis, Plant> plantJoin = diagnosisJoin.join("plant", JoinType.LEFT);
                predicates.add(cb.equal(
                        cb.upper(plantJoin.get("plantCode")),
                        filter.plantCode().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
            }

            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.toDate()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
