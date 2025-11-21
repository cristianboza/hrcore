package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Context object containing information about the current user performing an operation.
 * Used to pass authorization context through service layer methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceRequestOperationContext {
    
    /**
     * ID of the user performing the operation
     */
    private UUID currentUserId;
    
    /**
     * Role of the user performing the operation
     */
    private UserRole currentUserRole;
    
    /**
     * Sort field for pagination
     */
    @Builder.Default
    private String sortBy = "createdAt";
    
    /**
     * Sort direction for pagination
     */
    @Builder.Default
    private String sortDirection = "DESC";
}
