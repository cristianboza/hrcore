package com.example.hrcore.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.hrcore.config.FeatureFlagConstants.*;

@Component
@Getter
public class FeatureFlags {
    
    @Value("${features." + FEEDBACK_AI_POLISH + ".enabled:false}")
    private boolean feedbackAiPolishEnabled;
    
    @Value("${features." + FEEDBACK_AI_POLISH + ".huggingface-api-key:}")
    private String huggingFaceApiKey;
    
    public boolean isFeatureEnabled(String featureKey) {
        return switch (featureKey) {
            case FEEDBACK_AI_POLISH -> isFeedbackAiPolishAvailable();
            default -> false;
        };
    }
    
    public boolean isFeedbackAiPolishAvailable() {
        return feedbackAiPolishEnabled && huggingFaceApiKey != null && !huggingFaceApiKey.trim().isEmpty();
    }
}
