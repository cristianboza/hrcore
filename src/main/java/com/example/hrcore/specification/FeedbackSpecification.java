package com.example.hrcore.specification;

import com.example.hrcore.dto.FeedbackFilterDto;
import com.example.hrcore.entity.Feedback;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.FeedbackStatus;
import com.example.hrcore.entity.enums.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedbackSpecification {

    /**
     * Build specification with role-based filtering applied at query level
     */
    public static Specification<Feedback> buildSpecification(FeedbackFilterDto filters, UUID currentUserId, UserRole currentUserRole) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply role-based access control at query level
            if (currentUserRole == UserRole.EMPLOYEE) {
                // Employees can only see:
                // 1. Feedback they gave (fromUserId = currentUserId)
                // 2. APPROVED feedback they received (toUserId = currentUserId AND status = APPROVED)
                Predicate fromPredicate = cb.equal(root.get("fromUserId"), currentUserId);
                Predicate toPredicate = cb.and(
                    cb.equal(root.get("toUserId"), currentUserId),
                    cb.equal(root.get("status"), FeedbackStatus.APPROVED)
                );
                predicates.add(cb.or(fromPredicate, toPredicate));
            }
            // MANAGER and SUPER_ADMIN can see everything - no additional predicate

            // Apply filter criteria
            if (filters != null) {
                if (filters.getFromUserId() != null) {
                    predicates.add(cb.equal(root.get("fromUserId"), filters.getFromUserId()));
                }
                if (filters.getToUserId() != null) {
                    predicates.add(cb.equal(root.get("toUserId"), filters.getToUserId()));
                }
                if (filters.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filters.getStatus()));
                }
                if (filters.getCreatedAfter() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filters.getCreatedAfter()));
                }
                if (filters.getCreatedBefore() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filters.getCreatedBefore()));
                }
                if (filters.getContentContains() != null && !filters.getContentContains().trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("content")), "%" + filters.getContentContains().toLowerCase() + "%"));
                }
                if (filters.getHasPolishedContent() != null) {
                    if (filters.getHasPolishedContent()) {
                        predicates.add(cb.isNotNull(root.get("polishedContent")));
                    } else {
                        predicates.add(cb.isNull(root.get("polishedContent")));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Feedback> byFromUser(UUID fromUserId) {
        return (root, query, cb) -> fromUserId == null ? null :
                cb.equal(root.get("fromUserId"), fromUserId);
    }

    public static Specification<Feedback> byToUser(UUID toUserId) {
        return (root, query, cb) -> toUserId == null ? null :
                cb.equal(root.get("toUserId"), toUserId);
    }

    public static Specification<Feedback> byStatus(FeedbackStatus status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }

    public static Specification<Feedback> createdAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? null :
                cb.greaterThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<Feedback> createdBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? null :
                cb.lessThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<Feedback> contentContains(String content) {
        return (root, query, cb) -> content == null || content.trim().isEmpty() ? null :
                cb.like(cb.lower(root.get("content")), "%" + content.toLowerCase() + "%");
    }

    public static Specification<Feedback> hasPolishedContent(Boolean hasPolished) {
        return (root, query, cb) -> {
            if (hasPolished == null) return null;
            if (hasPolished) {
                return cb.isNotNull(root.get("polishedContent"));
            } else {
                return cb.isNull(root.get("polishedContent"));
            }
        };
    }
}
