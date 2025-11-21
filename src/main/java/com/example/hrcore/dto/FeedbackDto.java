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
public class FeedbackDto {
    private Long id;
    private NamedUserDto fromUser;
    private NamedUserDto toUser;
    private String content;
    private String polishedContent;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

