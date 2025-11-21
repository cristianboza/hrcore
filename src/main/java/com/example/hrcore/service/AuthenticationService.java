package com.example.hrcore.service;

import com.example.hrcore.entity.User;
import java.util.UUID;
import com.example.hrcore.entity.enums.UserRole;
import java.util.UUID;
import com.example.hrcore.repository.UserRepository;
import java.util.UUID;
import com.example.hrcore.repository.ValidTokenRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final ValidTokenRepository validTokenRepository;
    private final UserRepository userRepository;

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authentication found");
        }

        String jti = (String) authentication.getPrincipal();
        
        return validTokenRepository.findByTokenJti(jti)
                .map(validToken -> validToken.getUserId())
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new IllegalStateException("User not found for current authentication"));
    }

    public UUID getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    public UserRole getCurrentUserRole(Authentication authentication) {
        return getCurrentUser(authentication).getRole();
    }

    public boolean isCurrentUser(Authentication authentication, UUID userId) {
        return getCurrentUserId(authentication).equals(userId);
    }
}
