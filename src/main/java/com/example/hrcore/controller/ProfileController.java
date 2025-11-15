package com.example.hrcore.controller;

import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.repository.ValidTokenRepository;
import com.example.hrcore.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ValidTokenRepository validTokenRepository;
    private final UserRepository userRepository;

    private User extractUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("No authentication or principal found");
            return null;
        }
        
        String jti = (String) authentication.getPrincipal();
        log.debug("Extracting user from authentication JTI: {}", jti);
        
        var validToken = validTokenRepository.findByTokenJti(jti);
        if (validToken.isEmpty()) {
            log.warn("No valid token found for JTI: {}", jti);
            return null;
        }
        
        Long userId = validToken.get().getUserId();
        var user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("No user found for ID: {}", userId);
            return null;
        }
        
        log.info("Successfully extracted user {} from JTI {}", userId, jti);
        return user.get();
    }

    /**
     * Get all profiles (filtered by current user's role)
     */
    @PreAuthorize("hasAnyRole('MANAGER','EMPLOYEE','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllProfiles(Authentication authentication) {
        log.info("GET /api/profiles - Request to get all profiles");
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("GET /api/profiles - Failed to extract user from authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("GET /api/profiles - Fetching profiles for user: {} (role: {}) with ID: {}", 
                currentUser.getEmail(), currentUser.getRole(), currentUser.getId());
        List<UserDto> profiles = profileService.getAllProfilesFiltered(currentUser.getId(), currentUser.getRole());
        log.info("GET /api/profiles - Found {} profiles", profiles.size());
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get a specific profile (with role-based filtering)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE', 'SUPER_ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getProfile(
            @PathVariable Long userId,
            Authentication authentication) {

        log.info("GET /api/profiles/{} - Request to get profile", userId);
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("GET /api/profiles/{} - Failed to extract user from authentication", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("GET /api/profiles/{} - Fetching for current user: {} (role: {})", userId, currentUser.getId(), currentUser.getRole());
        UserDto profile = profileService.getProfileWithRoleFiltering(userId, currentUser.getId(), currentUser.getRole());

        if (profile == null) {
            log.warn("GET /api/profiles/{} - Profile not found or access denied", userId);
            return ResponseEntity.notFound().build();
        }

        log.info("GET /api/profiles/{} - Profile found and returned", userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update a profile (only owner or manager)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE', 'SUPER_ADMIN')")
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable Long userId,
            @RequestBody UserDto userDto,
            Authentication authentication) {

        log.info("PUT /api/profiles/{} - Request to update profile", userId);
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("PUT /api/profiles/{} - Failed to extract user from authentication", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("PUT /api/profiles/{} - Updating for current user: {} (role: {})", userId, currentUser.getId(), currentUser.getRole());
            UserDto updatedProfile = profileService.updateProfile(userId, currentUser.getId(), currentUser.getRole(), userDto);
            log.info("PUT /api/profiles/{} - Profile updated successfully", userId);
            return ResponseEntity.ok(updatedProfile);
        } catch (SecurityException e) {
            log.warn("PUT /api/profiles/{} - Security exception: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /api/profiles/{} - Argument exception: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Delete a profile (only managers)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteProfile(
            @PathVariable Long userId,
            Authentication authentication) {

        log.info("DELETE /api/profiles/{} - Request to delete profile", userId);
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("DELETE /api/profiles/{} - Failed to extract user from authentication", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            log.info("DELETE /api/profiles/{} - Deleting for current user: {} (role: {})", userId, currentUser.getId(), currentUser.getRole());
            profileService.deleteProfile(userId, currentUser.getId(), currentUser.getRole());
            log.info("DELETE /api/profiles/{} - Profile deleted successfully", userId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            log.warn("DELETE /api/profiles/{} - Security exception: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied: " + e.getMessage());
        }
    }

    /**
     * Check permissions for a profile
     */
    @GetMapping("/{userId}/permissions")
    public ResponseEntity<?> getProfilePermissions(
            @PathVariable Long userId,
            Authentication authentication) {

        log.info("GET /api/profiles/{}/permissions - Request to check permissions", userId);
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("GET /api/profiles/{}/permissions - Failed to extract user from authentication", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("GET /api/profiles/{}/permissions - Checking for current user: {} (role: {})", userId, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.ok(new PermissionsDto(
                profileService.canViewAllData(userId, currentUser.getId(), currentUser.getRole()),
                profileService.canEditProfile(userId, currentUser.getId(), currentUser.getRole()),
                profileService.canDeleteProfile(userId, currentUser.getId(), currentUser.getRole())
        ));
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        log.info("GET /api/profiles/me - Request to get current user");
        User currentUser = extractUserFromAuth(authentication);
        if (currentUser == null) {
            log.error("GET /api/profiles/me - Failed to extract user from authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("GET /api/profiles/me - Returning user: {} ({})", currentUser.getId(), currentUser.getEmail());
        return ResponseEntity.ok(UserDto.from(currentUser));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PermissionsDto {
        private boolean canViewAll;
        private boolean canEdit;
        private boolean canDelete;
    }
}
