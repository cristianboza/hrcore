package com.example.hrcore.repository;

import com.example.hrcore.entity.Feedback;
import com.example.hrcore.entity.enums.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    Page<Feedback> findByToUserIdOrderByCreatedAtDesc(UUID toUserId, Pageable pageable);
    Page<Feedback> findByToUserIdAndStatusOrderByCreatedAtDesc(UUID toUserId, FeedbackStatus status, Pageable pageable);
    Page<Feedback> findByFromUserIdOrderByCreatedAtDesc(UUID fromUserId, Pageable pageable);
    Page<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);
}

