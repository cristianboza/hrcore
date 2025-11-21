package com.example.hrcore.service;

import com.example.hrcore.dto.UserCreationData;
import com.example.hrcore.exception.InvalidOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url-internal:http://keycloak:8080}")
    private String keycloakUrlInternal;
    
    @Value("${keycloak.auth-server-url:http://localhost:9080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:hrcore}")
    private String realm;

    @Value("${keycloak.client-id:hrcore-app}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;
    
    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;
    
    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

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
        return getLogoutRedirectUrl(redirectUri, null);
    }

    public String getLogoutRedirectUrl(String redirectUri, String idTokenHint) {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            StringBuilder logoutUrl = new StringBuilder(String.format(
                    "%s/realms/%s/protocol/openid-connect/logout?post_logout_redirect_uri=%s&client_id=%s",
                    keycloakUrl, realm, encodedRedirectUri, clientId
            ));
            
            if (idTokenHint != null && !idTokenHint.isEmpty()) {
                logoutUrl.append("&id_token_hint=").append(URLEncoder.encode(idTokenHint, "UTF-8"));
                log.info("Generated Keycloak logout URL with id_token_hint");
            } else {
                log.info("Generated Keycloak logout URL without id_token_hint");
            }
            
            return logoutUrl.toString();
        } catch (Exception e) {
            log.error("Failed to generate logout URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate logout URL", e);
        }
    }
    
    private String getAdminAccessToken() {
        try {
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrlInternal);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", "admin-cli");
            body.add("username", adminUsername);
            body.add("password", adminPassword);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(tokenUrl, request, String.class);
            
            Map<String, Object> tokenResponse = objectMapper.readValue(response, Map.class);
            return (String) tokenResponse.get("access_token");
        } catch (Exception e) {
            log.error("Failed to get admin access token", e);
            throw new RuntimeException("Failed to authenticate with Keycloak admin", e);
        }
    }
    
    public String createKeycloakUser(UserCreationData userData) {
        try {
            String adminToken = getAdminAccessToken();
            String createUserUrl = String.format("%s/admin/realms/%s/users", keycloakUrlInternal, realm);
            
            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", userData.getEmail()); // Use email as username
            userRepresentation.put("email", userData.getEmail());
            userRepresentation.put("firstName", userData.getFirstName());
            userRepresentation.put("lastName", userData.getLastName());
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", true);
            
            // Set password
            List<Map<String, Object>> credentials = new ArrayList<>();
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", userData.getPassword());
            credential.put("temporary", false);
            credentials.add(credential);
            userRepresentation.put("credentials", credentials);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                createUserUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            // Get user ID from Location header
            String location = response.getHeaders().getFirst("Location");
            if (location != null) {
                String[] parts = location.split("/");
                String keycloakUserId = parts[parts.length - 1];
                log.info("User created in Keycloak - Email: {}, Keycloak ID: {}", 
                    userData.getEmail(), keycloakUserId);
                return keycloakUserId;
            }
            
            log.error("No Location header in response, cannot determine Keycloak user ID");
            throw new InvalidOperationException("create user in Keycloak", "Failed to get user ID from Keycloak");
            
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("User already exists in Keycloak - Email: {}", userData.getEmail());
            throw new InvalidOperationException(
                "create user", 
                "A user with this email address is already registered in the system"
            );
        } catch (HttpClientErrorException e) {
            log.error("Failed to create user in Keycloak - Status: {}, Email: {}", 
                e.getStatusCode(), userData.getEmail(), e);
            throw new InvalidOperationException(
                "create user in Keycloak", 
                "Unable to create user account. Please try again or contact support."
            );
        } catch (Exception e) {
            log.error("Unexpected error creating user in Keycloak - Email: {}", userData.getEmail(), e);
            throw new InvalidOperationException(
                "create user", 
                "An unexpected error occurred. Please try again."
            );
        }
    }
    
    public void deleteKeycloakUser(String keycloakUserId) {
        try {
            String adminToken = getAdminAccessToken();
            String deleteUserUrl = String.format("%s/admin/realms/%s/users/%s", 
                keycloakUrlInternal, realm, keycloakUserId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, request, String.class);
            
            log.info("User deleted from Keycloak - Keycloak ID: {}", keycloakUserId);
        } catch (Exception e) {
            log.warn("Failed to delete user from Keycloak - Keycloak ID: {}", keycloakUserId, e);
        }
    }
}
