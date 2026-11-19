package com.eiu.pdds.user.repository;

import com.eiu.pdds.user.dto.UserFilter;
import com.eiu.pdds.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilter(UserFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted users
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter != null) {
                if (filter.keyword() != null && !filter.keyword().trim().isEmpty()) {
                    String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
                    Predicate keywordPredicate = cb.or(
                            cb.like(cb.lower(root.get("username")), pattern),
                            cb.like(cb.lower(root.get("email")), pattern),
                            cb.like(cb.lower(root.get("fullName")), pattern)
                    );
                    predicates.add(keywordPredicate);
                }

                if (filter.role() != null) {
                    predicates.add(cb.equal(root.get("role"), filter.role()));
                }

                if (filter.isActive() != null) {
                    predicates.add(cb.equal(root.get("isActive"), filter.isActive()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
