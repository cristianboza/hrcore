package com.example.hrcore.controller;

import com.example.hrcore.entity.User;
import com.example.hrcore.entity.ValidToken;
import com.example.hrcore.repository.ValidTokenRepository;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logged-users")
@RequiredArgsConstructor
public class LoggedUsersController {

    private final TokenService tokenService;
    private final ValidTokenRepository validTokenRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllLoggedInUsers() {
        List<ValidToken> activeTokens = validTokenRepository.findByExpiresAtAfter(LocalDateTime.now());
        
        List<Map<String, Object>> loggedUsers = activeTokens.stream()
                .map(token -> {
                    User user = userRepository.findById(token.getUserId()).orElse(null);
                    Map<String, Object> map = new HashMap<>();
                    map.put("tokenId", token.getId());
                    map.put("userId", token.getUserId());
                    map.put("email", user != null ? user.getEmail() : "N/A");
                    map.put("firstName", user != null ? user.getFirstName() : "N/A");
                    map.put("lastName", user != null ? user.getLastName() : "N/A");
                    map.put("issuedAt", token.getIssuedAt());
                    map.put("expiresAt", token.getExpiresAt());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(loggedUsers);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ValidToken>> getUserSessions(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tokenService.getUserActiveTokens(userId));
    }

    @PostMapping("/logout/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> forceLogoutUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        tokenService.forceLogoutUser(userId);
        return ResponseEntity.ok(Map.of("message", "User successfully logged out"));
    }

    @PostMapping("/logout-session/{tokenId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> forceLogoutSession(@PathVariable Long tokenId) {
        ValidToken token = validTokenRepository.findById(tokenId).orElse(null);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        tokenService.invalidateToken(token.getTokenJti());
        return ResponseEntity.ok(Map.of("message", "Session successfully terminated"));
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> cleanupExpiredTokens() {
        tokenService.cleanupExpiredTokens();
        return ResponseEntity.ok(Map.of("message", "Expired tokens cleaned up"));
    }
}


