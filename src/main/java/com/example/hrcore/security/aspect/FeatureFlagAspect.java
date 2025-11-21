package com.example.hrcore.security.aspect;

import com.example.hrcore.config.FeatureFlags;
import com.example.hrcore.security.annotation.RequireFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureFlagAspect {

    private final FeatureFlags featureFlags;

    @Before("@annotation(requireFeature)")
    public void checkFeatureFlag(RequireFeature requireFeature) {
        String featureKey = requireFeature.value();
        boolean isEnabled = featureFlags.isFeatureEnabled(featureKey);
        
        if (!isEnabled) {
            log.warn("Feature {} is not enabled or not configured properly", featureKey);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Feature not available"
            );
        }
        
        log.debug("Feature {} is enabled", featureKey);
    }
}
