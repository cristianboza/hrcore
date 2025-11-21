package com.example.hrcore.controller;

import com.example.hrcore.config.KeycloakTokenProvider;
import com.example.hrcore.dto.AuthResponse;
import com.example.hrcore.dto.KeycloakTokenRequest;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.service.KeycloakService;
import com.example.hrcore.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final UserRepository userRepository;
    private final KeycloakTokenProvider keycloakTokenProvider;
    private final TokenService tokenService;
    private final KeycloakService keycloakService;
    private final JwtDecoder jwtDecoder;

    @Value("${keycloak.auth-server-url:http://localhost:9080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:hrcore}")
    private String realm;

    @Value("${keycloak.client-id:hrcore-app}")
    private String clientId;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;


    @GetMapping("/login-redirect")
    @Operation(
        summary = "Get login redirect URL",
        description = "Get the Keycloak login redirect URL for OAuth2 authentication flow"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Redirect URL retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
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
    @Operation(
        summary = "Handle OAuth2 callback",
        description = "Exchange authorization code for access token and create user session"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "400", description = "Invalid authorization code", content = @Content),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content)
    })
    public ResponseEntity<AuthResponse> handleCallback(
            @Parameter(description = "Callback data containing authorization code") @RequestBody Map<String, String> request) {
        try {
            request.forEach((key, value) -> log.info("Callback request param - {}: {}", key, value));
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
            String idToken = (String) tokenResponse.get("id_token");

            if (accessToken == null) {
                log.error("No access token received from Keycloak token endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            log.info("ID token received: {}", idToken != null ? "Yes" : "No");

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
                                .role(UserRole.EMPLOYEE)
                                .build();
                        return userRepository.save(newUser);
                    });

            log.info("User found/created - ID: {}, Email: {}", user.getId(), user.getEmail());
            
            // Register the token as valid with id_token
            Jwt jwt = keycloakService.createJwtFromToken(accessToken);
            tokenService.registerToken(jwt, user.getId(), user.getRole(), idToken);
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
    @Operation(
        summary = "Login (deprecated)",
        description = "Legacy login endpoint. Use /callback instead.",
        deprecated = true
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content)
    })
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Login credentials") @RequestBody KeycloakTokenRequest request) {
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
                                .role(UserRole.EMPLOYEE)
                                .build();
                        return userRepository.save(newUser);
                    });
            
            tokenService.registerToken(jwt, user.getId(), user.getRole(), null);
            
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
    @Operation(
        summary = "Logout",
        description = "Logout user and invalidate session"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "500", description = "Logout failed", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "Authorization header with Bearer token") @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Processing logout request");
        
        try {
            String token = null;
            String idToken = null;
            
            // Extract token from Authorization header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.info("Token extracted from Authorization header");
            }
            
            // Invalidate token locally if we have a JTI
            if (token != null) {
                try {
                    var jwt = jwtDecoder.decode(token);
                    String jti = jwt.getClaimAsString("jti");
                    if (jti != null && !jti.isEmpty()) {
                        log.info("Invalidating token locally with JTI: {}", jti);
                        
                        // Get id_token from database before invalidating
                        var validToken = tokenService.findByJti(jti);
                        if (validToken.isPresent()) {
                            idToken = validToken.get().getIdToken();
                            log.info("ID token retrieved from database: {}", idToken != null ? "Yes" : "No");
                        }
                        
                        tokenService.invalidateToken(jti);
                    }
                } catch (Exception e) {
                    log.warn("Could not extract JTI from token: {}", e.getMessage());
                }
                
                // Revoke token in Keycloak
                try {
                    log.info("Attempting to revoke token in Keycloak");
                    keycloakService.revokeToken(token);
                } catch (Exception e) {
                    log.warn("Failed to revoke token in Keycloak: {}", e.getMessage());
                }
            }
            
            // Clear security context
            SecurityContextHolder.clearContext();
            
            // Generate Keycloak logout URL with id_token_hint if available
            String logoutUrl = keycloakService.getLogoutRedirectUrl(frontendUrl, idToken);
            
            log.info("Logout successful, redirecting to: {}", logoutUrl);
            return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "logoutUrl", logoutUrl
            ));
            
        } catch (Exception e) {
            log.error("Error during logout", e);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(Map.of(
                "message", "Logout completed with warnings",
                "logoutUrl", frontendUrl
            ));
        }
    }

    @DeleteMapping("/logout-keycloak")
    @Operation(
        summary = "Logout from Keycloak (deprecated)",
        description = "Legacy Keycloak logout endpoint. Use POST /logout instead.",
        deprecated = true
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout successful")
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> logoutKeycloak(
            @Parameter(description = "Authorization header with Bearer token") @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Processing Keycloak logout (deprecated - use POST /logout instead)");
        
        // Just clear local state - actual logout handled by POST /logout
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/logout-redirect")
    @Operation(
        summary = "Get logout redirect URL",
        description = "Get the Keycloak logout redirect URL"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout URL retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Map<String, String>> getLogoutRedirect() {
        try {
            String logoutUrl = keycloakService.getLogoutRedirectUrl(frontendUrl);
            log.info("Logout redirect URL generated: {}", logoutUrl);
            return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
        } catch (Exception e) {
            log.error("Error generating logout redirect URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("logoutUrl", frontendUrl));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get current user",
        description = "Get the currently authenticated user's information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        try {
            String jti = (String) authentication.getPrincipal();
            log.info("Getting current user with JTI: {}", jti);
            
            // Get user from token via JTI
            var token = tokenService.findByJti(jti);
            if (token.isEmpty()) {
                log.warn("Token not found for JTI: {}", jti);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            UUID userId = token.get().getUserId();
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                log.warn("User not found for ID: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            log.info("Current user found: {} {}", user.getFirstName(), user.getLastName());
            
            AuthResponse response = AuthResponse.builder()
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
            log.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}


