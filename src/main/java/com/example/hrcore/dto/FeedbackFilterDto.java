package com.example.hrcore.dto;

import com.example.hrcore.entity.enums.FeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackFilterDto {
    private UUID fromUserId;
    private UUID toUserId;
    private FeedbackStatus status;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String contentContains;
    private Boolean hasPolishedContent;
}
