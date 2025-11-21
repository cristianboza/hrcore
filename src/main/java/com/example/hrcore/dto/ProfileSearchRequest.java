package com.example.hrcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Request object for searching and filtering user profiles")
public class ProfileSearchRequest {
    
    @Schema(description = "Search term for name or email", example = "john")
    private String search;
    
    @Schema(description = "Filter by user role", example = "EMPLOYEE")
    private String role;
    
    @Schema(description = "Filter by manager ID")
    private UUID managerId;
    
    @Schema(description = "Filter by department", example = "Engineering")
    private String department;
    
    @Schema(description = "Page number (0-indexed)", example = "0")
    @Min(0)
    @Builder.Default
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 10;
    
    @Schema(description = "Sort by field", example = "lastName")
    @Builder.Default
    private String sortBy = "lastName";
    
    @Schema(description = "Sort direction (ASC or DESC)", example = "ASC")
    @Builder.Default
    private String sortDirection = "ASC";
}
