/**
 * Central registry of all feature flag keys used in the application.
 * This ensures consistency between backend and frontend feature flag checks.
 * 
 * IMPORTANT: These keys must match the ones defined in FeatureFlagConstants.java
 */
export const FEATURE_FLAGS = {
  FEEDBACK_AI_POLISH: 'feedback.ai-polish',
  // Future feature flags can be added here
  // ABSENCE_AUTO_APPROVAL: 'absence.auto-approval',
  // PROFILE_ADVANCED_SEARCH: 'profile.advanced-search',
} as const;

export type FeatureFlagKey = typeof FEATURE_FLAGS[keyof typeof FEATURE_FLAGS];

/**
 * Check if a feature is enabled based on environment variables.
 * Feature flags are configured via VITE_FEATURE_* environment variables.
 * 
 * @param featureKey - The feature key from FEATURE_FLAGS
 * @returns true if the feature is enabled, false otherwise
 */
export function isFeatureEnabled(featureKey: FeatureFlagKey): boolean {
  const envKey = `VITE_FEATURE_${featureKey.toUpperCase().replace(/\./g, '_')}_ENABLED`;
  const value = import.meta.env[envKey];
  return value === 'true' || value === '1';
}

/**
 * Get feature configuration value from environment variables.
 * 
 * @param featureKey - The feature key from FEATURE_FLAGS
 * @param configKey - The configuration key (e.g., 'api-key', 'endpoint')
 * @returns the configuration value or undefined
 */
export function getFeatureConfig(featureKey: FeatureFlagKey, configKey: string): string | undefined {
  const envKey = `VITE_FEATURE_${featureKey.toUpperCase().replace(/\./g, '_')}_${configKey.toUpperCase().replace(/-/g, '_')}`;
  return import.meta.env[envKey];
}
