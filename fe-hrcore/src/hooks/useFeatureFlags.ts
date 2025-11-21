import { useMemo } from 'react';
import { featureFlagsService } from '../services/featureFlags';
import { FEATURE_FLAGS, FeatureFlagKey } from '../config/featureFlags';

/**
 * Hook to check if a feature is enabled.
 * Uses environment variables - no async loading needed.
 */
export function useFeature(featureKey: FeatureFlagKey): boolean {
  return useMemo(() => featureFlagsService.isFeatureEnabled(featureKey), [featureKey]);
}

/**
 * Hook to get all feature flags.
 */
export function useFeatureFlags() {
  return useMemo(() => featureFlagsService.getAllFeatureFlags(), []);
}

// Export the feature flag constants for easy access
export { FEATURE_FLAGS };
