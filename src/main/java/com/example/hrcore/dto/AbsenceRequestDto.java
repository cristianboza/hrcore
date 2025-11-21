package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceRequestDto {
    private Long id;
    private NamedUserDto user;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private AbsenceRequestType type;
    private AbsenceRequestStatus status;
    private NamedUserDto approver;
    private String rejectionReason;
    private NamedUserDto createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean canApprove; // Whether current user can approve/reject this request
}


