package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filter criteria for searching absence requests.
 * All fields are optional - null values will be ignored in the query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceRequestFilterDto {
    
    /**
     * Free text search in reason field
     */
    private String search;
    
    /**
     * Filter by user ID
     */
    private UUID userId;
    
    /**
     * Filter by request status
     */
    private AbsenceRequestStatus status;
    
    /**
     * Filter by request type
     */
    private AbsenceRequestType type;
    
    /**
     * Filter requests starting from this date (inclusive)
     */
    private LocalDate startDateFrom;
    
    /**
     * Filter requests starting until this date (inclusive)
     */
    private LocalDate startDateTo;
    
    /**
     * Filter requests ending from this date (inclusive)
     */
    private LocalDate endDateFrom;
    
    /**
     * Filter requests ending until this date (inclusive)
     */
    private LocalDate endDateTo;
    
    /**
     * Filter by approver ID
     */
    private UUID approverId;
    
    /**
     * Filter requests that have a rejection reason
     */
    private Boolean hasRejectionReason;
    
    /**
     * Filter by manager ID (requests from employees of this manager)
     */
    private UUID managerId;
    
    /**
     * Filter by creator ID (who created the request)
     */
    private UUID createdById;
}
