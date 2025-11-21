package com.example.hrcore.controller;

import com.example.hrcore.config.FeatureFlagConstants;
import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.dto.FeedbackFilterDto;
import com.example.hrcore.dto.FeedbackOperationContext;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.FeedbackStatus;
import com.example.hrcore.security.annotation.RequireAuthenticated;
import com.example.hrcore.security.annotation.RequireFeature;
import com.example.hrcore.security.annotation.RequireManagerOrAbove;
import com.example.hrcore.service.AuthenticationService;
import com.example.hrcore.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthenticationService authenticationService;

    @RequireAuthenticated
    @PostMapping
    @Operation(
        summary = "Submit feedback",
        description = "Submit feedback from one user to another"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Feedback submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<FeedbackDto> submitFeedback(
            @Parameter(description = "Feedback sender ID") @RequestParam UUID fromUserId,
            @Parameter(description = "Feedback recipient ID") @RequestParam UUID toUserId,
            @Parameter(description = "Feedback content") @RequestBody String content,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Submit feedback from user {} to user {} by {}", 
            fromUserId, toUserId, currentUser.getEmail());
        
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        FeedbackDto feedback = feedbackService.submitFeedback(fromUserId, toUserId, content, context);
        
        log.info("Feedback created successfully with ID: {}", feedback.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    @RequireAuthenticated
    @PostMapping("/search")
    @Operation(
        summary = "Search feedback with filters",
        description = "Advanced search and filtering of feedback. Managers/Admins can filter by user, status, dates, content. Employees see only their own."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved feedback"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<PageResponse<FeedbackDto>> searchFeedback(
            @Parameter(description = "Search filters and pagination") @RequestBody com.example.hrcore.dto.FeedbackSearchRequest searchRequest,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Searching feedback by {}, filters: fromUser={}, toUser={}, status={}", 
            currentUser.getEmail(), searchRequest.getFromUserId(), searchRequest.getUserId(), searchRequest.getStatus());
        
        FeedbackFilterDto filters = FeedbackFilterDto.builder()
                .fromUserId(searchRequest.getFromUserId())
                .toUserId(searchRequest.getUserId())
                .status(searchRequest.getStatus() != null ? FeedbackStatus.valueOf(searchRequest.getStatus().toUpperCase()) : null)
                .createdAfter(searchRequest.getCreatedAfter())
                .createdBefore(searchRequest.getCreatedBefore())
                .contentContains(searchRequest.getContentContains())
                .hasPolishedContent(searchRequest.getHasPolishedContent())
                .build();
        
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .sortBy(searchRequest.getSortBy())
                .sortDirection(searchRequest.getSortDirection())
                .build();
        
        PageResponse<FeedbackDto> feedback = feedbackService.searchFeedback(filters, context);
        
        log.debug("Found {} feedback items", feedback.getTotalElements());
        return ResponseEntity.ok(feedback);
    }

    @RequireManagerOrAbove
    @PutMapping("/{feedbackId}/approve")
    public ResponseEntity<FeedbackDto> approveFeedback(
            @PathVariable Long feedbackId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Approving feedback {} by {}", feedbackId, currentUser.getEmail());
        
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        FeedbackDto feedback = feedbackService.approveFeedback(feedbackId, context);
        
        log.info("Feedback {} approved successfully", feedbackId);
        return ResponseEntity.ok(feedback);
    }

    @RequireManagerOrAbove
    @PutMapping("/{feedbackId}/reject")
    public ResponseEntity<FeedbackDto> rejectFeedback(
            @PathVariable Long feedbackId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Rejecting feedback {} by {}", feedbackId, currentUser.getEmail());
        
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        FeedbackDto feedback = feedbackService.rejectFeedback(feedbackId, context);
        
        log.info("Feedback {} rejected successfully", feedbackId);
        return ResponseEntity.ok(feedback);
    }

    @RequireManagerOrAbove
    @RequireFeature(FeatureFlagConstants.FEEDBACK_AI_POLISH)
    @PostMapping("/{feedbackId}/polish")
    public ResponseEntity<FeedbackDto> polishFeedback(
            @PathVariable Long feedbackId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Polishing feedback {} by {}", feedbackId, currentUser.getEmail());
        
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        FeedbackDto feedback = feedbackService.polishFeedback(feedbackId, context);
        
        log.info("Feedback {} polished successfully", feedbackId);
        return ResponseEntity.ok(feedback);
    }
}
