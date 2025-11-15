package com.example.hrcore.repository;

import com.example.hrcore.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByToUserIdOrderByCreatedAtDesc(Long toUserId);
    List<Feedback> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId);
    List<Feedback> findByStatusOrderByCreatedAtDesc(String status);
}

