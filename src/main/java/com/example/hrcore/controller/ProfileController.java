package com.example.hrcore.controller;

import com.example.hrcore.dto.CreateUserRequest;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.dto.PermissionsDto;
import com.example.hrcore.dto.ProfileSearchRequest;
import com.example.hrcore.dto.UserCreationData;
import com.example.hrcore.dto.UserDto;
import com.example.hrcore.dto.UserFilterDto;
import com.example.hrcore.dto.UserOperationContext;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.mapper.UserMapper;
import com.example.hrcore.security.annotation.RequireAuthenticated;
import com.example.hrcore.security.annotation.RequireManagerOrAbove;
import com.example.hrcore.service.AuthenticationService;
import com.example.hrcore.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "User profile management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;

    @RequireAuthenticated
    @PostMapping("/search")
    @Operation(
        summary = "Search profiles",
        description = "Search and filter user profiles with pagination. Advanced search by name, email, role, manager, and department."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved profiles"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<PageResponse<UserDto>> searchProfiles(
            @Parameter(description = "Search filters and pagination") @Valid @RequestBody ProfileSearchRequest searchRequest,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        
        UserFilterDto filters = UserFilterDto.builder()
                .search(searchRequest.getSearch())
                .role(UserRole.fromString(searchRequest.getRole()).orElse(null))
                .managerId(searchRequest.getManagerId())
                .department(searchRequest.getDepartment())
                .build();
        
        PageResponse<UserDto> profiles = profileService.searchProfilesPaginated(
            filters, currentUser.getId(), currentUser.getRole(), 
            searchRequest.getPage(), searchRequest.getSize()
        );
        
        log.debug("User {} retrieved page {} with {} profiles", 
            currentUser.getEmail(), searchRequest.getPage(), profiles.getContent().size());
        return ResponseEntity.ok(profiles);
    }
    
    @RequireManagerOrAbove
    @PostMapping
    @Operation(
        summary = "Create new profile",
        description = "Create a new user profile. Requires MANAGER role or above."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profile created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires MANAGER role", content = @Content)
    })
    public ResponseEntity<UserDto> createProfile(
            @Parameter(description = "User creation data") @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        
        UserRole role = UserRole.fromString(request.getRole()).orElse(UserRole.EMPLOYEE);
        UUID managerId = null;
        
        // Only parse managerId if it's not null or empty
        if (request.getManagerId() != null && !request.getManagerId().trim().isEmpty()) {
            try {
                managerId = UUID.fromString(request.getManagerId());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid managerId format: {}", request.getManagerId());
            }
        }
        
        // Build user creation data
        UserCreationData userData = UserCreationData.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(request.getPassword())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .role(role)
                .managerId(managerId)
                .build();
        
        // Build operation context
        UserOperationContext context = UserOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        UserDto createdUser = profileService.createUser(userData, context);
        
        log.info("Profile created: {} by {}", createdUser.getEmail(), currentUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @RequireAuthenticated
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get profile by ID",
        description = "Retrieve a specific user profile by ID. Access depends on user role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<UserDto> getProfile(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        
        return profileService.getProfileWithRoleFiltering(userId, currentUser.getId(), currentUser.getRole())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequireAuthenticated
    @PutMapping("/{userId}")
    @Operation(
        summary = "Update profile",
        description = "Update a user profile. Users can update their own profile, managers can update their team members."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<UserDto> updateProfile(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Updated profile data") @Valid @RequestBody UserDto userDto,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);
        UserDto updatedProfile = profileService.updateProfile(
            userId, currentUser.getId(), currentUser.getRole(), userDto
        );
        
        log.info("Profile updated: {} by {}", userId, currentUser.getEmail());
        return ResponseEntity.ok(updatedProfile);
    }

    @RequireManagerOrAbove
    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete profile",
        description = "Delete a user profile. Requires MANAGER role or above."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires MANAGER role", content = @Content)
    })
    public ResponseEntity<Void> deleteProfile(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        profileService.deleteProfile(userId, currentUser.getId(), currentUser.getRole());
        
        log.warn("Profile deleted: {} by {}", userId, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @RequireAuthenticated
    @GetMapping("/{userId}/permissions")
    @Operation(
        summary = "Get profile permissions",
        description = "Get what actions the current user can perform on the specified profile"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<PermissionsDto> getProfilePermissions(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);
        
        PermissionsDto permissions = PermissionsDto.builder()
                .canViewAll(profileService.canViewAllData(userId, currentUser.getId(), currentUser.getRole()))
                .canEdit(profileService.canEditProfile(userId, currentUser.getId(), currentUser.getRole()))
                .canDelete(profileService.canDeleteProfile(userId, currentUser.getId(), currentUser.getRole()))
                .canGiveFeedback(true) // Anyone can give feedback to anyone
                .canRequestAbsence(true) // Anyone can request absence for anyone (managers for others, employees for self)
                .build();
        
        return ResponseEntity.ok(permissions);
    }

    @RequireAuthenticated
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the profile of the currently authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        return ResponseEntity.ok(userMapper.toDto(currentUser));
    }

    @RequireAuthenticated
    @GetMapping("/{userId}/direct-reports")
    @Operation(
        summary = "Get direct reports",
        description = "Get list of users who directly report to the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Direct reports retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<UserDto>> getDirectReports(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Authentication authentication) {
        
        List<UserDto> directReports = profileService.getDirectReports(userId);
        log.debug("Retrieved {} direct reports for user {}", directReports.size(), userId);
        return ResponseEntity.ok(directReports);
    }

    @RequireManagerOrAbove
    @GetMapping("/available-managers")
    public ResponseEntity<List<UserDto>> getAvailableManagers() {
        return ResponseEntity.ok(profileService.getAvailableManagers());
    }

    @RequireAuthenticated
    @GetMapping("/{userId}/manager")
    public ResponseEntity<UserDto> getManager(@PathVariable UUID userId) {
        return profileService.getManagerOf(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequireManagerOrAbove
    @PutMapping("/{userId}/manager/{managerId}")
    public ResponseEntity<Void> assignManager(
            @PathVariable UUID userId,
            @PathVariable UUID managerId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        profileService.assignManager(userId, managerId, currentUser.getId(), currentUser.getRole());
        
        log.info("Manager assigned: {} → {} by {}", managerId, userId, currentUser.getEmail());
        return ResponseEntity.ok().build();
    }

    @RequireAuthenticated
    @PostMapping("/{userId}/feedback/search")
    @Operation(
        summary = "Get feedback for a specific user",
        description = "Get feedback for a user on their profile. Visibility rules: " +
                     "- Own user sees all APPROVED feedback received " +
                     "- Direct manager/Admin sees ALL feedback (pending, approved, rejected) " +
                     "- Other users see only feedback they gave to this user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feedback retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<PageResponse<com.example.hrcore.dto.FeedbackDto>> getUserFeedback(
            @Parameter(description = "User ID to get feedback for") @PathVariable UUID userId,
            @Parameter(description = "Search filters") @RequestBody com.example.hrcore.dto.FeedbackSearchRequest searchRequest,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        
        // Delegate to service with proper context
        com.example.hrcore.dto.FeedbackOperationContext context = com.example.hrcore.dto.FeedbackOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .sortBy(searchRequest.getSortBy())
                .sortDirection(searchRequest.getSortDirection())
                .build();
        
        PageResponse<com.example.hrcore.dto.FeedbackDto> feedback = 
            profileService.getUserFeedback(userId, searchRequest.getStatus(), context);
        
        log.debug("User {} retrieved {} feedback items for user {}", 
            currentUser.getEmail(), feedback.getContent().size(), userId);
        return ResponseEntity.ok(feedback);
    }

}
