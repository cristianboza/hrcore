package com.example.hrcore.dto;

import com.example.hrcore.entity.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDto {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String polishedContent;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedbackDto from(Feedback feedback) {
        return FeedbackDto.builder()
                .id(feedback.getId())
                .fromUserId(feedback.getFromUserId())
                .toUserId(feedback.getToUserId())
                .content(feedback.getContent())
                .polishedContent(feedback.getPolishedContent())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}

