package com.example.hrcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Absence request search with filters and pagination")
public class AbsenceRequestSearchRequest {
    
    @Schema(description = "Filter by user ID")
    private UUID userId;
    
    @Schema(description = "Filter by status (PENDING, APPROVED, REJECTED)")
    private String status;
    
    @Schema(description = "Filter by type (VACATION, SICK_LEAVE, PERSONAL, etc)")
    private String type;
    
    @Schema(description = "Search term in reason")
    private String search;
    
    @Schema(description = "Start date from")
    private LocalDate startDateFrom;
    
    @Schema(description = "Start date to")
    private LocalDate startDateTo;
    
    @Schema(description = "End date from")
    private LocalDate endDateFrom;
    
    @Schema(description = "End date to")
    private LocalDate endDateTo;
    
    @Schema(description = "Filter by approver ID")
    private UUID approverId;
    
    @Schema(description = "Filter by manager ID")
    private UUID managerId;
    
    @Schema(description = "Has rejection reason")
    private Boolean hasRejectionReason;
    
    @Schema(description = "Page number (0-indexed)", example = "0")
    @Builder.Default
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    @Builder.Default
    private int size = 10;
    
    @Schema(description = "Sort field", example = "createdAt")
    @Builder.Default
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC")
    @Builder.Default
    private String sortDirection = "DESC";
}
