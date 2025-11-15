package com.example.hrcore.dto;

import com.example.hrcore.entity.AbsenceRequest;
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
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String type;
    private String status;
    private Long approverId;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AbsenceRequestDto from(AbsenceRequest request) {
        return AbsenceRequestDto.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .type(request.getType())
                .status(request.getStatus())
                .approverId(request.getApproverId())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}

