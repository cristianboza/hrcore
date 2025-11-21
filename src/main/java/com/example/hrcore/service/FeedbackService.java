package com.example.hrcore.service;

import com.example.hrcore.config.FeatureFlags;
import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.dto.FeedbackFilterDto;
import com.example.hrcore.dto.FeedbackOperationContext;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.Feedback;
import com.example.hrcore.entity.enums.FeedbackStatus;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.mapper.FeedbackMapper;
import com.example.hrcore.repository.FeedbackRepository;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.specification.FeedbackSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;
    private final RestTemplate restTemplate;
    private final FeatureFlags featureFlags;

    @Transactional
    public FeedbackDto submitFeedback(UUID fromUserId, UUID toUserId, String content, FeedbackOperationContext context) {
        // Authorization: Can only submit feedback as yourself unless manager+
        if (!Objects.equals(fromUserId, context.getCurrentUserId()) && !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("submit feedback", "another user");
        }
        
        // Validate users exist
        if (!userRepository.existsById(fromUserId)) {
            throw new UserNotFoundException(fromUserId);
        }
        if (!userRepository.existsById(toUserId)) {
            throw new UserNotFoundException(toUserId);
        }
        
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidOperationException("submit feedback", "Content cannot be empty");
        }

        Feedback feedback = Feedback.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(content)
                .status(FeedbackStatus.PENDING)
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback created - From: {}, To: {}", fromUserId, toUserId);
        return feedbackMapper.toDto(saved);
    }

    public PageResponse<FeedbackDto> getReceivedFeedback(UUID userId, FeedbackOperationContext context) {
        // Authorization: Can only view own received feedback unless manager+
        if (!Objects.equals(userId, context.getCurrentUserId()) && !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("view received feedback", "another user");
        }
        
        Pageable pageable = createPageable(context);
        // Only show APPROVED feedback to the receiver - pending feedback needs manager approval first
        Page<Feedback> page = feedbackRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(userId, FeedbackStatus.APPROVED, pageable);
        return PageResponse.from(page, feedbackMapper::toDto);
    }

    public PageResponse<FeedbackDto> getGivenFeedback(UUID userId, FeedbackOperationContext context) {
        // Authorization: Can only view own given feedback unless manager+
        if (!Objects.equals(userId, context.getCurrentUserId()) && !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("view given feedback", "another user");
        }
        
        Pageable pageable = createPageable(context);
        Page<Feedback> page = feedbackRepository.findByFromUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, feedbackMapper::toDto);
    }

    public PageResponse<FeedbackDto> getPendingFeedback(FeedbackOperationContext context) {
        if (!context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("view pending feedback", "this user");
        }
        
        Pageable pageable = createPageable(context);
        Page<Feedback> page = feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING, pageable);
        return PageResponse.from(page, feedbackMapper::toDto);
    }

    public PageResponse<FeedbackDto> searchFeedback(FeedbackFilterDto filters, FeedbackOperationContext context) {
        // Role-based filtering is applied at query level in specification
        Specification<Feedback> spec = FeedbackSpecification.buildSpecification(
            filters, 
            context.getCurrentUserId(), 
            context.getCurrentUserRole()
        );
        Pageable pageable = createPageable(context);
        Page<Feedback> page = feedbackRepository.findAll(spec, pageable);
        return PageResponse.from(page, feedbackMapper::toDto);
    }

    @Transactional
    public FeedbackDto approveFeedback(Long feedbackId, FeedbackOperationContext context) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new InvalidOperationException("approve feedback", 
                    "Feedback not found with ID: " + feedbackId));

        if (feedback.getStatus() != FeedbackStatus.PENDING) {
            throw new InvalidOperationException("approve feedback", 
                "Feedback has already been " + feedback.getStatus());
        }

        // Authorization: Only direct manager or super admin can approve
        if (!canApproveOrReject(feedback.getToUserId(), context)) {
            throw new UnauthorizedException("approve feedback", "only direct manager or super admin");
        }

        feedback.setStatus(FeedbackStatus.APPROVED);
        Feedback updated = feedbackRepository.save(feedback);
        
        log.info("Feedback approved - ID: {}, Approver: {}", feedbackId, context.getCurrentUserId());
        return feedbackMapper.toDto(updated);
    }

    @Transactional
    public FeedbackDto rejectFeedback(Long feedbackId, FeedbackOperationContext context) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new InvalidOperationException("reject feedback", 
                    "Feedback not found with ID: " + feedbackId));

        if (feedback.getStatus() != FeedbackStatus.PENDING) {
            throw new InvalidOperationException("reject feedback", 
                "Feedback has already been " + feedback.getStatus());
        }

        // Authorization: Only direct manager or super admin can reject
        if (!canApproveOrReject(feedback.getToUserId(), context)) {
            throw new UnauthorizedException("reject feedback", "only direct manager or super admin");
        }

        feedback.setStatus(FeedbackStatus.REJECTED);
        Feedback updated = feedbackRepository.save(feedback);
        
        log.info("Feedback rejected - ID: {}, Approver: {}", feedbackId, context.getCurrentUserId());
        return feedbackMapper.toDto(updated);
    }

    /**
     * Check if the current user can approve/reject for the given employee
     * Only direct manager or super admin can approve/reject
     */
    private boolean canApproveOrReject(UUID employeeId, FeedbackOperationContext context) {
        // Super admin can do anything
        if (context.getCurrentUserRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Must be at least a manager
        if (!context.getCurrentUserRole().isManagerOrAbove()) {
            return false;
        }
        
        // Check if current user is the direct manager of the employee
        return userRepository.findById(employeeId)
                .map(employee -> employee.getManager() != null && 
                               employee.getManager().getId().equals(context.getCurrentUserId()))
                .orElse(false);
    }

    @Transactional
    public FeedbackDto polishFeedback(Long feedbackId, FeedbackOperationContext context) {
        if (!context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("polish feedback", "this user");
        }
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new InvalidOperationException("polish feedback", 
                    "Feedback not found with ID: " + feedbackId));

        String originalContent = feedback.getContent();

        // Call HuggingFace API to polish content
        try {
            String polished = callHuggingFaceAPI(originalContent);
            feedback.setPolishedContent(polished);
        } catch (Exception e) {
            log.warn("Failed to polish feedback with AI, using original: {}", e.getMessage());
            feedback.setPolishedContent(originalContent + " [AI polishing unavailable]");
        }

        Feedback updated = feedbackRepository.save(feedback);
        log.info("Feedback polished - ID: {}, Polisher: {}", feedbackId, context.getCurrentUserId());
        return feedbackMapper.toDto(updated);
    }

    private Pageable createPageable(FeedbackOperationContext context) {
        int page = context.getPage() != null ? context.getPage() : 0;
        int size = context.getSize() != null ? context.getSize() : 10;
        String sortBy = context.getSortBy() != null ? context.getSortBy() : "createdAt";
        String direction = context.getSortDirection() != null ? context.getSortDirection() : "DESC";
        
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    private String callHuggingFaceAPI(String text) throws Exception {
        String apiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
        String apiKey = featureFlags.getHuggingFaceApiKey();

        try {
            HuggingFaceRequest request = new HuggingFaceRequest(text);
            HuggingFaceResponse response = restTemplate.postForObject(apiUrl, request, HuggingFaceResponse.class);
            return response != null ? response.getSummaryText() : text;
        } catch (Exception e) {
            log.error("HuggingFace API error: {}", e.getMessage());
            throw e;
        }
    }

    static class HuggingFaceRequest {
        public String inputs;

        public HuggingFaceRequest(String inputs) {
            this.inputs = inputs;
        }
    }

    static class HuggingFaceResponse {
        private String summary_text;

        public String getSummaryText() {
            return summary_text;
        }

        public void setSummaryText(String summary_text) {
            this.summary_text = summary_text;
        }
    }
}
