package com.example.hrcore.service;

import com.example.hrcore.entity.ValidToken;
import com.example.hrcore.repository.ValidTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final ValidTokenRepository validTokenRepository;

    @Transactional
    public void registerToken(Jwt jwt, Long userId) {
        String jti = jwt.getClaimAsString("jti");
        String keycloakSubject = jwt.getClaimAsString("sub");
        
        log.debug("Registering token - JTI: {}, User ID: {}, Subject: {}", jti, userId, keycloakSubject);
        
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
                .keycloakSubject(keycloakSubject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        validTokenRepository.save(validToken);
        log.info("Token registered - JTI: {}, User ID: {}, Subject: {}, Expires: {}", jti, userId, keycloakSubject, expiresAt);
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
    public void invalidateAllUserTokens(Long userId) {
        validTokenRepository.deleteByUserId(userId);
        log.info("All tokens invalidated for user - User ID: {}", userId);
    }

    public List<ValidToken> getUserActiveTokens(Long userId) {
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

    @Transactional
    public void forceLogoutUser(Long userId) {
        invalidateAllUserTokens(userId);
    }
}
