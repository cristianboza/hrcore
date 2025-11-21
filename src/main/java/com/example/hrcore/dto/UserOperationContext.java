package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Context information for user operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOperationContext {
    private UUID currentUserId;
    private UserRole currentUserRole;
    private UUID targetUserId;
}
