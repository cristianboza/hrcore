package com.example.hrcore.controller;

import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<FeedbackDto> submitFeedback(
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId,
            @RequestBody String content) {
        log.info("POST /api/feedback - Submit feedback from user {} to user {}", fromUserId, toUserId);
        try {
            FeedbackDto feedback = feedbackService.submitFeedback(fromUserId, toUserId, content);
            log.info("POST /api/feedback - Feedback created successfully with ID: {}", feedback.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
        } catch (Exception e) {
            log.error("POST /api/feedback - Error submitting feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<FeedbackDto>> getReceivedFeedback(@PathVariable Long userId) {
        log.info("GET /api/feedback/received/{} - Fetching received feedback for user", userId);
        try {
            List<FeedbackDto> feedback = feedbackService.getReceivedFeedback(userId);
            log.info("GET /api/feedback/received/{} - Found {} feedback items", userId, feedback.size());
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("GET /api/feedback/received/{} - Error fetching received feedback: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @GetMapping("/given/{userId}")
    public ResponseEntity<List<FeedbackDto>> getGivenFeedback(@PathVariable Long userId) {
        log.info("GET /api/feedback/given/{} - Fetching given feedback for user", userId);
        try {
            List<FeedbackDto> feedback = feedbackService.getGivenFeedback(userId);
            log.info("GET /api/feedback/given/{} - Found {} feedback items", userId, feedback.size());
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("GET /api/feedback/given/{} - Error fetching given feedback: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<FeedbackDto>> getPendingFeedback() {
        log.info("GET /api/feedback/pending - Fetching pending feedback");
        try {
            List<FeedbackDto> feedback = feedbackService.getPendingFeedback();
            log.info("GET /api/feedback/pending - Found {} pending feedback items", feedback.size());
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("GET /api/feedback/pending - Error fetching pending feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PutMapping("/{feedbackId}/approve")
    public ResponseEntity<FeedbackDto> approveFeedback(@PathVariable Long feedbackId) {
        log.info("PUT /api/feedback/{}/approve - Approving feedback", feedbackId);
        try {
            FeedbackDto feedback = feedbackService.approveFeedback(feedbackId);
            log.info("PUT /api/feedback/{}/approve - Feedback approved successfully", feedbackId);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("PUT /api/feedback/{}/approve - Error approving feedback: {}", feedbackId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PutMapping("/{feedbackId}/reject")
    public ResponseEntity<FeedbackDto> rejectFeedback(@PathVariable Long feedbackId) {
        log.info("PUT /api/feedback/{}/reject - Rejecting feedback", feedbackId);
        try {
            FeedbackDto feedback = feedbackService.rejectFeedback(feedbackId);
            log.info("PUT /api/feedback/{}/reject - Feedback rejected successfully", feedbackId);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("PUT /api/feedback/{}/reject - Error rejecting feedback: {}", feedbackId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PostMapping("/{feedbackId}/polish")
    public ResponseEntity<?> polishFeedback(@PathVariable Long feedbackId) {
        log.info("POST /api/feedback/{}/polish - Polishing feedback", feedbackId);
        try {
            FeedbackDto feedback = feedbackService.polishFeedback(feedbackId);
            log.info("POST /api/feedback/{}/polish - Feedback polished successfully", feedbackId);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("POST /api/feedback/{}/polish - Error polishing feedback: {}", feedbackId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error polishing feedback: " + e.getMessage());
        }
    }
}
