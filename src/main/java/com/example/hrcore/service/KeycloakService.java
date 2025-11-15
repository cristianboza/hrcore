package com.example.hrcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url-internal:http://keycloak:8080}")
    private String keycloakUrlInternal;

    @Value("${keycloak.realm:hrcore}")
    private String realm;

    @Value("${keycloak.client-id:hrcore-app}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    public Map<String, Object> exchangeCodeForToken(String code, String redirectUri) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrlInternal, realm);
            log.info("Exchanging authorization code for token - URL: {}", tokenUrl);
            log.debug("Keycloak base URL: {}, Realm: {}", keycloakUrlInternal, realm);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            log.debug("Token exchange request - client_id: {}, redirect_uri: {}, code: {}", clientId, redirectUri, code.substring(0, Math.min(20, code.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            log.debug("Sending request to Keycloak token endpoint");
            String response = restTemplate.postForObject(tokenUrl, request, String.class);

            log.info("Token exchange successful - Response received with {} characters", response.length());
            log.debug("Token response: {}", response);
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.error("Failed to exchange code for token - Keycloak URL: {}, Exception: {}", keycloakUrlInternal, e.getMessage(), e);
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    public Map<String, Object> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Invalid token format - expected 3 parts, got {}", parts.length);
                throw new IllegalArgumentException("Invalid token format");
            }

            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            Map<String, Object> claims = objectMapper.readValue(decodedPayload, Map.class);
            log.debug("Token parsed successfully - Claims: {}", claims.keySet());
            return claims;
        } catch (Exception e) {
            log.error("Failed to parse token", e);
            throw new RuntimeException("Failed to parse token", e);
        }
    }

    public Jwt createJwtFromToken(String tokenString) {
        try {
            Map<String, Object> claims = parseToken(tokenString);
            
            log.debug("All token claims available: {}", claims.keySet());
            
            Long iat = ((Number) claims.get("iat")).longValue();
            Long exp = ((Number) claims.get("exp")).longValue();
            String sub = (String) claims.get("sub");
            String iss = (String) claims.get("iss");
            String jti = (String) claims.get("jti");
            
            if (sub == null || sub.isEmpty()) {
                log.warn("Token has no 'sub' claim, using 'preferred_username' or 'sub' fallback");
                sub = (String) claims.getOrDefault("preferred_username", (String) claims.get("sub"));
            }
            
            log.info("Creating JWT from token - Issued: {}, Expires: {}, Subject: {}, JTI: {}", iat, exp, sub, jti);
            
            Map<String, Object> cleanClaims = new HashMap<>(claims);
            cleanClaims.remove("iat");
            cleanClaims.remove("exp");
            cleanClaims.remove("iss");
            cleanClaims.remove("sub");
            
            return Jwt.withTokenValue(tokenString)
                    .header("alg", "RS256")
                    .header("typ", "JWT")
                    .issuer(iss)
                    .subject(sub)
                    .issuedAt(Instant.ofEpochSecond(iat))
                    .expiresAt(Instant.ofEpochSecond(exp))
                    .claim("jti", jti)
                    .claims(c -> c.putAll(cleanClaims))
                    .build();
        } catch (Exception e) {
            log.error("Failed to create JWT from token", e);
            throw new RuntimeException("Failed to create JWT from token", e);
        }
    }

    public String getTokenIssuer() {
        return String.format("%s/realms/%s", keycloakUrlInternal, realm);
    }

    public void revokeToken(String token) {
        try {
            String revokeUrl = String.format("%s/realms/%s/protocol/openid-connect/revoke", keycloakUrlInternal, realm);
            log.info("Revoking token with Keycloak - URL: {}", revokeUrl);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(revokeUrl, request, String.class);

            log.info("Token successfully revoked in Keycloak");
        } catch (Exception e) {
            log.warn("Failed to revoke token in Keycloak: {}", e.getMessage());
        }
    }

    public String getLogoutRedirectUrl(String redirectUri) {
        try {
            String logoutUrl = String.format(
                    "%s/realms/%s/protocol/openid-connect/logout?redirect_uri=%s",
                    keycloakUrlInternal.replace(":8080", ""), realm, 
                    java.net.URLEncoder.encode(redirectUri, "UTF-8")
            );
            log.info("Generated logout redirect URL");
            return logoutUrl;
        } catch (Exception e) {
            log.error("Failed to generate logout URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate logout URL", e);
        }
    }
}
