package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackOperationContext {
    private UUID currentUserId;
    private UserRole currentUserRole;
    private FeedbackFilterDto filters;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
