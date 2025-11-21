package com.example.hrcore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionsDto {
    private boolean canViewAll;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canGiveFeedback;
    private boolean canRequestAbsence;
}
