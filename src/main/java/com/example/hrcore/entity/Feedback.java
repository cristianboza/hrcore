package com.example.hrcore.entity;

import com.example.hrcore.entity.enums.FeedbackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_from_user", columnList = "fromUserId"),
    @Index(name = "idx_feedback_to_user", columnList = "toUserId"),
    @Index(name = "idx_feedback_status", columnList = "status"),
    @Index(name = "idx_feedback_created_at", columnList = "createdAt"),
    @Index(name = "idx_feedback_to_user_status", columnList = "toUserId,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID fromUserId;

    @Column(nullable = false)
    private UUID toUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toUserId", referencedColumnName = "id", insertable = false, updatable = false)
    private User toUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String polishedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

