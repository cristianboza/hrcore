package com.example.hrcore.specification;

import com.example.hrcore.dto.AbsenceRequestFilterDto;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import com.example.hrcore.entity.enums.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification for filtering AbsenceRequest entities.
 * Provides type-safe, composable query building with role-based filtering.
 */
public class AbsenceRequestSpecification {

    /**
     * Build a composite specification from filter DTO with role-based access control.
     * Filtering is applied at query level for performance.
     *
     * @param filters The filter criteria
     * @param currentUserId Current user's ID
     * @param currentUserRole Current user's role
     * @return Composite specification
     */
    public static Specification<AbsenceRequest> withFilters(AbsenceRequestFilterDto filters, UUID currentUserId, UserRole currentUserRole) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply role-based access control at query level
            if (currentUserRole == UserRole.EMPLOYEE) {
                // Employees can only see their own requests
                predicates.add(criteriaBuilder.equal(root.get("userId"), currentUserId));
            }
            // MANAGER and SUPER_ADMIN can see ALL requests - no additional predicate

            // Apply filter criteria
            if (filters != null) {
                if (filters.getSearch() != null && !filters.getSearch().trim().isEmpty()) {
                    String searchPattern = "%" + filters.getSearch().toLowerCase() + "%";
                    predicates.add(
                        criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("reason")),
                            searchPattern
                        )
                    );
                }

                if (filters.getUserId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("userId"), filters.getUserId()));
                }

                if (filters.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filters.getStatus()));
                }

                if (filters.getType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("type"), filters.getType()));
                }

                if (filters.getStartDateFrom() != null) {
                    predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), filters.getStartDateFrom())
                    );
                }

                if (filters.getStartDateTo() != null) {
                    predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), filters.getStartDateTo())
                    );
                }

                if (filters.getEndDateFrom() != null) {
                    predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), filters.getEndDateFrom())
                    );
                }

                if (filters.getEndDateTo() != null) {
                    predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), filters.getEndDateTo())
                    );
                }

                if (filters.getApproverId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("approverId"), filters.getApproverId()));
                }
                
                // Filter by creator
                if (filters.getCreatedById() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("createdById"), filters.getCreatedById()));
                }

                // Filter by manager - requests from employees of a specific manager
                if (filters.getManagerId() != null) {
                    Join<AbsenceRequest, User> userJoin = root.join("user");
                    Join<User, User> managerJoin = userJoin.join("manager");
                    predicates.add(criteriaBuilder.equal(managerJoin.get("id"), filters.getManagerId()));
                }

                if (filters.getHasRejectionReason() != null) {
                    if (filters.getHasRejectionReason()) {
                        predicates.add(criteriaBuilder.isNotNull(root.get("rejectionReason")));
                    } else {
                        predicates.add(criteriaBuilder.isNull(root.get("rejectionReason")));
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by user ID
     */
    public static Specification<AbsenceRequest> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) ->
            userId == null ? null : criteriaBuilder.equal(root.get("userId"), userId);
    }

    /**
     * Filter by status
     */
    public static Specification<AbsenceRequest> hasStatus(AbsenceRequestStatus status) {
        return (root, query, criteriaBuilder) ->
            status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Filter by type
     */
    public static Specification<AbsenceRequest> hasType(AbsenceRequestType type) {
        return (root, query, criteriaBuilder) ->
            type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    /**
     * Filter by date range - requests that start within the given range
     */
    public static Specification<AbsenceRequest> startDateBetween(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("startDate"), from, to);
            }
            if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), from);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), to);
        };
    }

    /**
     * Filter by date range - requests that end within the given range
     */
    public static Specification<AbsenceRequest> endDateBetween(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("endDate"), from, to);
            }
            if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), from);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), to);
        };
    }

    /**
     * Filter by approver ID
     */
    public static Specification<AbsenceRequest> hasApproverId(UUID approverId) {
        return (root, query, criteriaBuilder) ->
            approverId == null ? null : criteriaBuilder.equal(root.get("approverId"), approverId);
    }

    /**
     * Search in reason field (case-insensitive)
     */
    public static Specification<AbsenceRequest> reasonContains(String searchText) {
        return (root, query, criteriaBuilder) -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + searchText.toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), pattern);
        };
    }

    /**
     * Filter requests that have a rejection reason
     */
    public static Specification<AbsenceRequest> hasRejectionReason(boolean hasReason) {
        return (root, query, criteriaBuilder) ->
            hasReason ?
                criteriaBuilder.isNotNull(root.get("rejectionReason")) :
                criteriaBuilder.isNull(root.get("rejectionReason"));
    }

    /**
     * Filter by pending status
     */
    public static Specification<AbsenceRequest> isPending() {
        return hasStatus(AbsenceRequestStatus.PENDING);
    }

    /**
     * Filter by approved status
     */
    public static Specification<AbsenceRequest> isApproved() {
        return hasStatus(AbsenceRequestStatus.APPROVED);
    }

    /**
     * Filter by rejected status
     */
    public static Specification<AbsenceRequest> isRejected() {
        return hasStatus(AbsenceRequestStatus.REJECTED);
    }
}
