package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for user creation data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationData {
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String phone;
    private String department;
    private UserRole role;
    private UUID managerId;
}
