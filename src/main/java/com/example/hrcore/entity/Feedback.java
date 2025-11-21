package com.example.hrcore.entity;

import com.example.hrcore.entity.enums.FeedbackStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_from_user", columnList = "fromUserId"),
    @Index(name = "idx_feedback_to_user", columnList = "toUserId"),
    @Index(name = "idx_feedback_status", columnList = "status"),
    @Index(name = "idx_feedback_created_at", columnList = "created_at"),
    @Index(name = "idx_feedback_to_user_status", columnList = "toUserId,status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Feedback extends Auditable {

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
}

