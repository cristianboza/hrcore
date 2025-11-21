package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserFilterDto {
    
    private String search;
    private UserRole role;
    private UUID managerId;
    private String department;
    
    public boolean hasSearch() {
        return search != null && !search.isBlank();
    }
    
    public boolean hasRole() {
        return role != null;
    }
    
    public boolean hasManagerId() {
        return managerId != null;
    }
    
    public boolean hasDepartment() {
        return department != null && !department.isBlank();
    }
    
    public boolean hasAnyFilter() {
        return hasSearch() || hasRole() || hasManagerId() || hasDepartment();
    }
}
