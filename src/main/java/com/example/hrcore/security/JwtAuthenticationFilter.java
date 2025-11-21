package com.example.hrcore.security;

import com.example.hrcore.repository.ValidTokenRepository;
import com.example.hrcore.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final JwtDecoder jwtDecoder;
    private final ValidTokenRepository validTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.info("Incoming request: {} {}", method, path);
        
        String authorizationHeader = request.getHeader("Authorization");
        log.info("Authorization header present: {}", authorizationHeader != null);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            log.info("Processing JWT token from Authorization header for {} {}", method, path);

            try {
                String jti = extractJti(token);
                log.info("Extracted JTI from token: {}", jti);
                
                if (jti == null) {
                    log.error("JTI extraction failed - token format invalid");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token format");
                    return;
                }
                
                boolean isValid = tokenService.isTokenValid(jti);
                log.info("Token validation result for JTI {}: {}", jti, isValid);
                
                if (!isValid) {
                    log.warn("Token validation failed - JTI: {} (token may have been revoked or expired)", jti);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid or has been revoked");
                    return;
                }
                
                log.info("Token is valid, extracting authorities from token");
                Collection<GrantedAuthority> authorities = extractAuthoritiesFromToken(jti);
                log.info("Extracted {} authorities: {}", authorities.size(), authorities);
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        jti, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Security context successfully set with {} authorities for {} {}", authorities.size(), method, path);
                
            } catch (Exception e) {
                log.error("Error processing JWT token for {} {}: {}", method, path, e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        } else {
            log.warn("No valid Authorization header for {} {} - will continue without authentication", method, path);
        }

        filterChain.doFilter(request, response);
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromToken(String jti) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        try {
            log.debug("Loading role from ValidToken using JTI: {}", jti);
            
            validTokenRepository.findByTokenJti(jti).ifPresentOrElse(
                validToken -> {
                    String role = validToken.getUserRole().name();
                    String authority = "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(authority));
                    log.info("Loaded role from token - Role: {}, Authority: {}", role, authority);
                },
                () -> log.warn("No valid token found for JTI: {}", jti)
            );
        } catch (Exception e) {
            log.error("Error extracting authorities from token for JTI {}: {}", jti, e.getMessage(), e);
        }
        log.info("Total authorities extracted from token: {}", authorities.size());
        return authorities;
    }

    private String extractJti(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.debug("Invalid token format - expected 3 parts, got {}", parts.length);
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            if (payload.contains("\"jti\"")) {
                int start = payload.indexOf("\"jti\":\"") + 7;
                int end = payload.indexOf("\"", start);
                return payload.substring(start, end);
            }
        } catch (Exception e) {
            log.error("Error extracting JTI from token", e);
        }
        return null;
    }
}
