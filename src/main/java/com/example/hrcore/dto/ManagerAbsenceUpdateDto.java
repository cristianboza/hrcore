package com.example.hrcore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerAbsenceUpdateDto {
    private String status;
    private String managerComment;
}
