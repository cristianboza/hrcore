package com.example.hrcore.util;

import com.example.hrcore.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-change-this-in-production-should-be-at-least-32-characters-long}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long expiration;

    public String generateToken(User user) {
        return generateToken(user.getId(), user.getEmail(), user.getRole().name(), user.getFirstName(), user.getLastName());
    }

    public String generateToken(Long userId, String email, String role, String firstName, String lastName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        String jti = UUID.randomUUID().toString();

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("firstName", firstName)
                .claim("lastName", lastName)
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public Long extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    public String extractJti(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("jti", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
}
