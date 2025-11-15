package com.example.hrcore.service;

import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    // Define sensitive fields that only MANAGER and the user themselves can see
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "salary",
            "performance_reviews",
            "social_security",
            "address"
    );

    /**
     * Get user profile with role-based filtering
     *
     * @param userId       The user being viewed
     * @param currentUserId The user making the request
     * @param currentRole  The role of the current user
     * @return UserDto with filtered data based on permissions
     */
    public UserDto getProfileWithRoleFiltering(Long userId, Long currentUserId, User.UserRole currentRole) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return null;
        }

        User targetUser = user.get();
        UserDto dto = UserDto.from(targetUser);

        // Determine visibility based on role
        if (canViewAllData(userId, currentUserId, currentRole)) {
            // Can see everything
            return dto;
        } else if (currentRole == User.UserRole.EMPLOYEE && targetUser.getRole() == User.UserRole.EMPLOYEE && !userId.equals(currentUserId)) {
            // EMPLOYEE viewing another EMPLOYEE (COWORKER constraint)
            // Can only see non-sensitive data
            maskSensitiveData(dto);
        } else {
            // Other restrictions
            maskSensitiveData(dto);
        }

        return dto;
    }

    /**
     * Check if current user can edit the profile
     */
    public boolean canEditProfile(Long userId, Long currentUserId, User.UserRole currentRole) {
        if (currentRole == User.UserRole.SUPER_ADMIN || currentRole == User.UserRole.MANAGER) {
            return true; // Super admin and manager can edit any profile
        }
        return userId.equals(currentUserId); // Employees can only edit their own
    }

    /**
     * Check if current user can delete the profile
     */
    public boolean canDeleteProfile(Long userId, Long currentUserId, User.UserRole currentRole) {
        // Super admin and managers can delete profiles
        return currentRole == User.UserRole.SUPER_ADMIN || currentRole == User.UserRole.MANAGER;
    }

    /**
     * Get all profiles filtered by current user's role
     */
    public List<UserDto> getAllProfilesFiltered(Long currentUserId, User.UserRole currentRole) {
        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .map(user -> getProfileWithRoleFiltering(user.getId(), currentUserId, currentRole))
                .collect(Collectors.toList());
    }

    /**
     * Update user profile with permission checks
     */
    public UserDto updateProfile(Long userId, Long currentUserId, User.UserRole currentRole, UserDto updateDto) {
        if (!canEditProfile(userId, currentUserId, currentRole)) {
            throw new SecurityException("You do not have permission to edit this profile");
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOptional.get();
        user.setFirstName(updateDto.getFirstName());
        user.setLastName(updateDto.getLastName());
        user.setPhone(updateDto.getPhone());
        user.setDepartment(updateDto.getDepartment());

        // Only super admin and managers can change roles
        if ((currentRole == User.UserRole.SUPER_ADMIN || currentRole == User.UserRole.MANAGER) && updateDto.getRole() != null) {
            user.setRole(User.UserRole.valueOf(updateDto.getRole()));
        }

        User updatedUser = userRepository.save(user);
        return getProfileWithRoleFiltering(updatedUser.getId(), currentUserId, currentRole);
    }

    /**
     * Delete profile with permission checks
     */
    public void deleteProfile(Long userId, Long currentUserId, User.UserRole currentRole) {
        log.info("deleteProfile - Attempting to delete user {} by user {} (role: {})", userId, currentUserId, currentRole);
        
        if (!canDeleteProfile(userId, currentUserId, currentRole)) {
            log.error("deleteProfile - Permission denied: Only managers and super admins can delete profiles");
            throw new SecurityException("Only managers and super admins can delete profiles");
        }

        try {
            log.info("deleteProfile - Deleting user {} from database", userId);
            userRepository.deleteById(userId);
            log.info("deleteProfile - User {} deleted successfully by user {}", userId, currentUserId);
        } catch (Exception e) {
            log.error("deleteProfile - Error deleting user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user profile", e);
        }
    }

    /**
     * Check if current user can view all data of target user
     */
    public boolean canViewAllData(Long userId, Long currentUserId, User.UserRole currentRole) {
        // Super admin and manager can see all
        if (currentRole == User.UserRole.SUPER_ADMIN || currentRole == User.UserRole.MANAGER) {
            return true;
        }
        // User can see their own full profile
        return userId.equals(currentUserId);
    }

    // ...existing code...

    private void maskSensitiveData(UserDto dto) {
        // Optionally mask sensitive data for non-authorized users
        // This is a basic implementation; you can enhance it as needed
        dto.setUpdatedAt(null); // Don't expose internal timestamps
    }
}

