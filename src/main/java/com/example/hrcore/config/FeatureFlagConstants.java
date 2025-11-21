package com.example.hrcore.config;

/**
 * Central registry of all feature flag keys used in the application.
 * This ensures consistency between backend and frontend feature flag checks.
 */
public final class FeatureFlagConstants {
    
    private FeatureFlagConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Feedback features
    public static final String FEEDBACK_AI_POLISH = "feedback.ai-polish";
    
    // Future feature flags can be added here
    // public static final String ABSENCE_AUTO_APPROVAL = "absence.auto-approval";
    // public static final String PROFILE_ADVANCED_SEARCH = "profile.advanced-search";
}
