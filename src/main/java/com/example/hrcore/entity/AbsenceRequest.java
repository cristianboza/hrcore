package com.example.hrcore.entity;

import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "absence_requests", indexes = {
    @Index(name = "idx_absence_user", columnList = "userId"),
    @Index(name = "idx_absence_status", columnList = "status"),
    @Index(name = "idx_absence_start_date", columnList = "startDate"),
    @Index(name = "idx_absence_created_by", columnList = "created_by"),
    @Index(name = "idx_absence_approver", columnList = "approverId"),
    @Index(name = "idx_absence_user_status", columnList = "userId,status"),
    @Index(name = "idx_absence_dates", columnList = "startDate,endDate")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AbsenceRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceRequestStatus status;

    private UUID approverId;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private UUID createdById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdById", referencedColumnName = "id", insertable = false, updatable = false)
    private User requestCreator;
}

