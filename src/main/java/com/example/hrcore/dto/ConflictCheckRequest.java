package com.example.hrcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Request object for checking absence request conflicts")
public class ConflictCheckRequest {
    
    @Schema(description = "User ID to check conflicts for", required = true)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Schema(description = "Start date of the period to check", required = true)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @Schema(description = "End date of the period to check", required = true)
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    @Schema(description = "Page number (0-indexed)", example = "0")
    @Min(0)
    @Builder.Default
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 10;
}
