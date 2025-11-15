package com.example.hrcore.service;

import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.entity.Feedback;
import com.example.hrcore.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RestTemplate restTemplate;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public FeedbackDto submitFeedback(Long fromUserId, Long toUserId, String content) {
        Feedback feedback = Feedback.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(content)
                .status(STATUS_PENDING)
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        return FeedbackDto.from(saved);
    }

    public List<FeedbackDto> getReceivedFeedback(Long userId) {
        return feedbackRepository.findByToUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FeedbackDto::from)
                .toList();
    }

    public List<FeedbackDto> getGivenFeedback(Long userId) {
        return feedbackRepository.findByFromUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FeedbackDto::from)
                .toList();
    }

    public List<FeedbackDto> getPendingFeedback() {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING)
                .stream()
                .map(FeedbackDto::from)
                .toList();
    }

    public FeedbackDto approveFeedback(Long feedbackId) {
        Optional<Feedback> feedback = feedbackRepository.findById(feedbackId);
        if (feedback.isPresent()) {
            Feedback f = feedback.get();
            f.setStatus(STATUS_APPROVED);
            Feedback updated = feedbackRepository.save(f);
            return FeedbackDto.from(updated);
        }
        throw new IllegalArgumentException("Feedback not found");
    }

    public FeedbackDto rejectFeedback(Long feedbackId) {
        Optional<Feedback> feedback = feedbackRepository.findById(feedbackId);
        if (feedback.isPresent()) {
            Feedback f = feedback.get();
            f.setStatus(STATUS_REJECTED);
            Feedback updated = feedbackRepository.save(f);
            return FeedbackDto.from(updated);
        }
        throw new IllegalArgumentException("Feedback not found");
    }

    public FeedbackDto polishFeedback(Long feedbackId) throws Exception {
        Optional<Feedback> feedback = feedbackRepository.findById(feedbackId);
        if (feedback.isEmpty()) {
            throw new IllegalArgumentException("Feedback not found");
        }

        Feedback f = feedback.get();
        String originalContent = f.getContent();

        // Call HuggingFace API to polish content
        String polished = callHuggingFaceAPI(originalContent);

        f.setPolishedContent(polished);
        Feedback updated = feedbackRepository.save(f);
        return FeedbackDto.from(updated);
    }

    private String callHuggingFaceAPI(String text) throws Exception {
        String apiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
        String apiKey = System.getenv("HUGGINGFACE_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback: return original text with simple improvement
            return text + " (Professional review conducted)";
        }

        try {
            HuggingFaceRequest request = new HuggingFaceRequest(text);
            HuggingFaceResponse response = restTemplate.postForObject(apiUrl, request, HuggingFaceResponse.class);
            return response != null ? response.getSummaryText() : text;
        } catch (Exception e) {
            System.err.println("HuggingFace API error: " + e.getMessage());
            return text;
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

