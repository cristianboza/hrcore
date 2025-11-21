package com.example.hrcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Feedback search request with filters and pagination")
public class FeedbackSearchRequest {
    
    @Schema(description = "Filter by recipient user ID")
    private UUID userId;
    
    @Schema(description = "Filter by feedback status (PENDING, APPROVED, REJECTED)")
    private String status;
    
    @Schema(description = "Filter by sender ID")
    private UUID fromUserId;
    
    @Schema(description = "Filter by created after date")
    private LocalDateTime createdAfter;
    
    @Schema(description = "Filter by created before date")
    private LocalDateTime createdBefore;
    
    @Schema(description = "Search in content")
    private String contentContains;
    
    @Schema(description = "Filter by polished content presence")
    private Boolean hasPolishedContent;
    
    @Schema(description = "Page number (0-indexed)", example = "0")
    @Builder.Default
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    @Builder.Default
    private int size = 10;
    
    @Schema(description = "Sort field", example = "createdAt")
    @Builder.Default
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC")
    @Builder.Default
    private String sortDirection = "DESC";
}
