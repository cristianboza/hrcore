package com.example.hrcore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static Optional<UUID> getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                if (subject != null) {
                    return Optional.of(UUID.fromString(subject));
                }
            }
        } catch (Exception e) {
            // Ignore, will return empty
        }
        return Optional.empty();
    }
}
