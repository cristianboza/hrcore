package com.example.hrcore.service;

import com.example.hrcore.entity.ValidToken;
import java.util.UUID;
import com.example.hrcore.entity.enums.UserRole;
import java.util.UUID;
import com.example.hrcore.repository.ValidTokenRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
import org.springframework.stereotype.Service;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final ValidTokenRepository validTokenRepository;

    @Transactional
    public void registerToken(Jwt jwt, UUID userId, UserRole userRole, String idToken) {
        String jti = jwt.getClaimAsString("jti");
        String keycloakSubject = jwt.getClaimAsString("sub");
        
        log.debug("Registering token - JTI: {}, User ID: {}, Role: {}, Subject: {}, Has ID Token: {}", 
            jti, userId, userRole, keycloakSubject, idToken != null);
        
        if (keycloakSubject == null || keycloakSubject.isEmpty()) {
            log.warn("Token has no subject claim, attempting to use user ID as fallback");
            keycloakSubject = String.valueOf(userId);
        }
        
        LocalDateTime issuedAt = jwt.getIssuedAt() != null 
            ? LocalDateTime.from(jwt.getIssuedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
            : LocalDateTime.now();
        LocalDateTime expiresAt = jwt.getExpiresAt() != null 
            ? LocalDateTime.from(jwt.getExpiresAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
            : LocalDateTime.now().plusHours(24);

        ValidToken validToken = ValidToken.builder()
                .tokenJti(jti)
                .userId(userId)
                .userRole(userRole)
                .keycloakSubject(keycloakSubject)
                .idToken(idToken)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        validTokenRepository.save(validToken);
        log.info("Token registered - JTI: {}, User ID: {}, Role: {}, Subject: {}, Expires: {}", 
            jti, userId, userRole, keycloakSubject, expiresAt);
    }

    public boolean isTokenValid(String jti) {
        log.info("Validating token with JTI: {}", jti);
        if (jti == null || jti.isEmpty()) {
            log.warn("JTI is null or empty");
            return false;
        }
        
        Optional<ValidToken> token = validTokenRepository.findByTokenJti(jti);
        if (token.isEmpty()) {
            log.warn("Token not found in database - JTI: {}", jti);
            return false;
        }
        
        ValidToken validToken = token.get();
        boolean isExpired = validToken.isExpired();
        boolean valid = !isExpired;
        log.info("Token validity check - JTI: {}, IsExpired: {}, Valid: {}, ExpiresAt: {}", jti, isExpired, valid, validToken.getExpiresAt());
        return valid;
    }

    @Transactional
    public void invalidateToken(String jti) {
        validTokenRepository.deleteByTokenJti(jti);
        log.info("Token invalidated - JTI: {}", jti);
    }

    @Transactional
    public void invalidateAllUserTokens(UUID userId) {
        validTokenRepository.deleteByUserId(userId);
        log.info("All tokens invalidated for user - User ID: {}", userId);
    }

    public List<ValidToken> getUserActiveTokens(UUID userId) {
        List<ValidToken> tokens = validTokenRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now());
        log.debug("Retrieved active tokens for user - User ID: {}, Count: {}", userId, tokens.size());
        return tokens;
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        long deletedCount = validTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleanup task executed - Deleted expired tokens: {}", deletedCount);
    }

    public Optional<ValidToken> findByJti(String jti) {
        return validTokenRepository.findByTokenJti(jti);
    }

    @Transactional
    public void forceLogoutUser(UUID userId) {
        // Get all active tokens for the user to revoke them in Keycloak
        List<ValidToken> activeTokens = getUserActiveTokens(userId);
        
        // Invalidate all tokens in database
        invalidateAllUserTokens(userId);
        
        log.info("Force logout executed for user - User ID: {}, Tokens invalidated: {}", userId, activeTokens.size());
    }
    
    public List<String> getIdTokensForUser(UUID userId) {
        return validTokenRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now())
                .stream()
                .map(ValidToken::getIdToken)
                .filter(idToken -> idToken != null && !idToken.isEmpty())
                .toList();
    }
}
