import { FEATURE_FLAGS, isFeatureEnabled as checkFeatureFlag, FeatureFlagKey } from '../config/featureFlags';

export interface FeatureFlags {
  [FEATURE_FLAGS.FEEDBACK_AI_POLISH]: boolean;
}

class FeatureFlagsService {
  /**
   * Check if a feature is enabled.
   * Uses environment variables - no backend call needed.
   */
  isFeatureEnabled(featureKey: FeatureFlagKey): boolean {
    return checkFeatureFlag(featureKey);
  }

  /**
   * Get all feature flags as an object.
   */
  getAllFeatureFlags(): Record<FeatureFlagKey, boolean> {
    return Object.values(FEATURE_FLAGS).reduce((acc, key) => {
      acc[key as FeatureFlagKey] = checkFeatureFlag(key as FeatureFlagKey);
      return acc;
    }, {} as Record<FeatureFlagKey, boolean>);
  }
}

export const featureFlagsService = new FeatureFlagsService();
