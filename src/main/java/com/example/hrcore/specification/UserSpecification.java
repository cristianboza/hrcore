package com.example.hrcore.specification;

import com.example.hrcore.dto.UserFilterDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.User_;
import com.example.hrcore.entity.enums.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

    public static Specification<User> withFilters(UserFilterDto filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filters.hasSearch()) {
                String searchPattern = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.firstName)), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.lastName)), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.email)), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.department)), searchPattern)
                ));
            }

            if (filters.hasRole()) {
                predicates.add(criteriaBuilder.equal(root.get(User_.role), filters.getRole()));
            }

            if (filters.hasManagerId()) {
                predicates.add(criteriaBuilder.equal(root.get(User_.manager).get(User_.id), filters.getManagerId()));
            }

            if (filters.hasDepartment()) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get(User_.department)), 
                    filters.getDepartment().toLowerCase()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<User> hasRole(UserRole role) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get(User_.role), role);
    }

    public static Specification<User> hasManager(UUID managerId) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get(User_.manager).get(User_.id), managerId);
    }

    public static Specification<User> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.firstName)), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.lastName)), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.email)), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get(User_.department)), searchPattern)
            );
        };
    }

    public static Specification<User> excludeSuperAdminsForNonSuperAdmin(UserRole currentRole) {
        return (root, query, criteriaBuilder) -> {
            if (currentRole == UserRole.SUPER_ADMIN) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.notEqual(root.get(User_.role), UserRole.SUPER_ADMIN);
        };
    }
}
