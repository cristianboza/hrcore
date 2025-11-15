package com.example.hrcore.controller;

import com.example.hrcore.config.KeycloakTokenProvider;
import com.example.hrcore.dto.AuthResponse;
import com.example.hrcore.dto.KeycloakTokenRequest;
import com.example.hrcore.entity.User;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.service.KeycloakService;
import com.example.hrcore.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final KeycloakTokenProvider keycloakTokenProvider;
    private final TokenService tokenService;
    private final KeycloakService keycloakService;

    @Value("${keycloak.auth-server-url:http://localhost:9080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:hrcore}")
    private String realm;

    @Value("${keycloak.client-id:hrcore-app}")
    private String clientId;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;


    @GetMapping("/login-redirect")
    public ResponseEntity<Map<String, String>> getLoginRedirect() {
        try {
            String redirectUri = URLEncoder.encode(frontendUrl + "/auth/callback", StandardCharsets.UTF_8.toString());
            String keycloakAuthUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/auth?client_id=%s&response_type=code&redirect_uri=%s&scope=openid%%20profile%%20email",
                keycloakUrl, realm, clientId, redirectUri
            );
            log.info("Login redirect URL generated - Keycloak URL: {}, Client ID: {}", keycloakUrl, clientId);
            log.debug("Full redirect URL: {}", keycloakAuthUrl);
            return ResponseEntity.ok(Map.of("redirectUrl", keycloakAuthUrl));
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding redirect URI", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<AuthResponse> handleCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            log.info("Callback received - Code: {}", code != null ? code.substring(0, Math.min(20, code.length())) + "..." : "null");
            log.debug("Full callback request payload: {}", request);
            
            if (code == null || code.isEmpty()) {
                log.error("No authorization code provided in callback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            String redirectUri = frontendUrl + "/auth/callback";
            log.info("Exchanging code for token - Redirect URI: {}", redirectUri);
            
            Map<String, Object> tokenResponse = keycloakService.exchangeCodeForToken(code, redirectUri);
            String accessToken = (String) tokenResponse.get("access_token");

            if (accessToken == null) {
                log.error("No access token received from Keycloak token endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("Access token received, parsing claims");
            Map<String, Object> tokenClaims = keycloakService.parseToken(accessToken);
            String email = (String) tokenClaims.get("email");
            String givenName = (String) tokenClaims.get("given_name");
            String familyName = (String) tokenClaims.get("family_name");
            
            log.info("Token claims parsed - Email: {}, Name: {} {}", email, givenName, familyName);

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        log.warn("User not found for email: {}, creating new user", email);
                        User newUser = User.builder()
                                .email(email)
                                .firstName(givenName != null ? givenName : "")
                                .lastName(familyName != null ? familyName : "")
                                .role(User.UserRole.EMPLOYEE)
                                .build();
                        return userRepository.save(newUser);
                    });

            log.info("User found/created - ID: {}, Email: {}", user.getId(), user.getEmail());
            
            // Register the token as valid
            Jwt jwt = keycloakService.createJwtFromToken(accessToken);
            tokenService.registerToken(jwt, user.getId());
            log.info("Token registered in database for user: {}", user.getId());

            AuthResponse response = AuthResponse.builder()
                    .token(accessToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .phone(user.getPhone())
                    .department(user.getDepartment())
                    .build();

            log.info("Returning successful auth response for user: {}", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in auth callback", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody KeycloakTokenRequest request) {
        try {
            Jwt jwt = (Jwt) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            String email = keycloakTokenProvider.getEmailFromToken(jwt);
            String firstName = keycloakTokenProvider.getGivenNameFromToken(jwt);
            String lastName = keycloakTokenProvider.getFamilyNameFromToken(jwt);
            
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .email(email)
                                .firstName(firstName != null ? firstName : "")
                                .lastName(lastName != null ? lastName : "")
                                .role(User.UserRole.EMPLOYEE)
                                .build();
                        return userRepository.save(newUser);
                    });
            
            tokenService.registerToken(jwt, user.getId());
            
            AuthResponse response = AuthResponse.builder()
                    .token(request.getToken())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .phone(user.getPhone())
                    .department(user.getDepartment())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        String jti = (String) authentication.getPrincipal();
        if (jti != null && !jti.isEmpty()) {
            log.info("Logging out user with JTI: {}", jti);
            tokenService.invalidateToken(jti);
            SecurityContextHolder.getContext().setAuthentication(null);
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PostMapping("/logout-keycloak")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logoutKeycloak(
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        String jti = (String) authentication.getPrincipal();
        
        try {
            String token = request != null ? request.get("token") : null;
            if (token != null && !token.isEmpty()) {
                log.info("Revoking token in Keycloak");
                keycloakService.revokeToken(token);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke token in Keycloak: {}", e.getMessage());
        }
        
        if (jti != null && !jti.isEmpty()) {
            tokenService.invalidateToken(jti);
        }
        SecurityContextHolder.getContext().setAuthentication(null);
        
        return ResponseEntity.ok(Map.of("message", "Logout from Keycloak successful"));
    }

    @GetMapping("/logout-redirect")
    public ResponseEntity<Map<String, String>> getLogoutRedirect() {
        try {
            String redirectUri = frontendUrl;
            String logoutUrl = keycloakService.getLogoutRedirectUrl(redirectUri);
            log.info("Logout redirect URL generated");
            return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
        } catch (Exception e) {
            log.error("Error generating logout redirect URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        try {
            String jti = (String) authentication.getPrincipal();
            log.debug("Getting current user with JTI: {}", jti);
            
            // Get token from database to find user
            return ResponseEntity.ok(AuthResponse.builder().build());
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}


