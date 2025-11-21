package com.example.hrcore.service;

import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.dto.UserCreationData;
import com.example.hrcore.dto.UserDto;
import com.example.hrcore.dto.UserFilterDto;
import com.example.hrcore.dto.UserOperationContext;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.mapper.UserMapper;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final KeycloakService keycloakService;
    private final ValidationService validationService;
    private final FeedbackService feedbackService;
    private final AbsenceRequestService absenceRequestService;

    public ProfileService(UserRepository userRepository, UserMapper userMapper, 
                         TokenService tokenService, KeycloakService keycloakService, 
                         ValidationService validationService,
                         FeedbackService feedbackService,
                         AbsenceRequestService absenceRequestService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.keycloakService = keycloakService;
        this.validationService = validationService;
        this.feedbackService = feedbackService;
        this.absenceRequestService = absenceRequestService;
    }

    public List<UserDto> searchProfiles(UserFilterDto filters, UUID currentUserId, UserRole currentRole) {
        Specification<User> spec = UserSpecification.withFilters(filters);
        
        return userRepository.findAll(spec).stream()
                .map(user -> getProfileWithRoleFiltering(user.getId(), currentUserId, currentRole))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public PageResponse<UserDto> searchProfilesPaginated(
            UserFilterDto filters, 
            UUID currentUserId, 
            UserRole currentRole,
            int page,
            int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
        
        // Build specification with role filtering in query
        Specification<User> spec = UserSpecification.withFilters(filters)
                .and(UserSpecification.excludeSuperAdminsForNonSuperAdmin(currentRole));
        
        Page<User> userPage = userRepository.findAll(spec, pageable);
        
        List<UserDto> content = userPage.getContent().stream()
                .map(user -> getProfileWithRoleFiltering(user.getId(), currentUserId, currentRole))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        
        return PageResponse.<UserDto>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public Optional<UserDto> getProfileWithRoleFiltering(UUID userId, UUID currentUserId, UserRole currentRole) {
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return Optional.empty();
        }

        UserDto dto = userMapper.toDto(targetUser);
        
        if (!canViewAllData(userId, currentUserId, currentRole)) {
            maskSensitiveData(dto);
        }
        
        return Optional.of(dto);
    }

    @Cacheable(value = "permissions", key = "#userId + '-edit-' + #currentUserId")
    public boolean canEditProfile(UUID userId, UUID currentUserId, UserRole currentRole) {
        try {
            UserOperationContext context = UserOperationContext.builder()
                    .currentUserId(currentUserId)
                    .currentUserRole(currentRole)
                    .targetUserId(userId)
                    .build();
            validationService.validateCanEditUser(userId, context);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }
    
    @Cacheable(value = "permissions", key = "#userId + '-delete-' + #currentUserId")
    public boolean canDeleteProfile(UUID userId, UUID currentUserId, UserRole currentRole) {
        try {
            UserOperationContext context = UserOperationContext.builder()
                    .currentUserId(currentUserId)
                    .currentUserRole(currentRole)
                    .targetUserId(userId)
                    .build();
            validationService.validateCanDeleteUser(userId, context);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    public List<UserDto> getAllProfilesFiltered(UUID currentUserId, UserRole currentRole) {
        return userRepository.findAll().stream()
                .map(user -> getProfileWithRoleFiltering(user.getId(), currentUserId, currentRole))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "profiles", allEntries = true),
        @CacheEvict(value = "permissions", allEntries = true)
    })
    public UserDto updateProfile(UUID userId, UUID currentUserId, UserRole currentRole, UserDto updateDto) {
        // Validate permissions using validation service
        UserOperationContext context = UserOperationContext.builder()
                .currentUserId(currentUserId)
                .currentUserRole(currentRole)
                .targetUserId(userId)
                .build();
        validationService.validateUserUpdate(context);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Optional.ofNullable(updateDto.getFirstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(updateDto.getLastName()).ifPresent(user::setLastName);
        Optional.ofNullable(updateDto.getPhone()).ifPresent(user::setPhone);
        Optional.ofNullable(updateDto.getDepartment()).ifPresent(user::setDepartment);

        updateRoleIfAuthorized(user, currentRole, updateDto.getRole());
        updateManagerIfProvided(user, currentRole, updateDto.getManagerId());

        User updatedUser = userRepository.save(user);
        log.debug("Profile updated: {}", userId);

        return getProfileWithRoleFiltering(updatedUser.getId(), currentUserId, currentRole)
                .orElseThrow(() -> new UserNotFoundException(updatedUser.getId()));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "profiles", allEntries = true),
        @CacheEvict(value = "permissions", allEntries = true),
        @CacheEvict(value = "managerReports", allEntries = true)
    })
    public void deleteProfile(UUID userId, UUID currentUserId, UserRole currentRole) {
        // Validate permissions using validation service
        UserOperationContext context = UserOperationContext.builder()
                .currentUserId(currentUserId)
                .currentUserRole(currentRole)
                .targetUserId(userId)
                .build();
        validationService.validateUserDeletion(context);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Get all id_tokens for this user to logout from Keycloak
        List<String> idTokens = tokenService.getIdTokensForUser(userId);
        
        // Invalidate all tokens in database
        tokenService.invalidateAllUserTokens(userId);
        
        // Logout from Keycloak for each active session
        for (String idToken : idTokens) {
            try {
                log.info("Logging out user {} from Keycloak with id_token", userId);
                String logoutUrl = keycloakService.getLogoutRedirectUrl("", idToken);
                log.info("Generated Keycloak logout URL for user {}", userId);
            } catch (Exception e) {
                log.warn("Failed to generate logout URL for user {}: {}", userId, e.getMessage());
            }
        }
        
        userRepository.deleteById(userId);
        log.info("Profile deleted: {}, Sessions invalidated: {}", userId, idTokens.size());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "profiles", allEntries = true),
        @CacheEvict(value = "permissions", allEntries = true),
        @CacheEvict(value = "managerReports", allEntries = true)
    })
    public UserDto createUser(UserCreationData userData, UserOperationContext context) {
        
        // Validate all business rules using validation service
        validationService.validateUserCreation(userData, context);
        
        // Create user in Keycloak first
        String keycloakUserId;
        try {
            keycloakUserId = keycloakService.createKeycloakUser(userData);
        } catch (InvalidOperationException e) {
            // Re-throw validation errors from Keycloak (e.g., duplicate username)
            throw e;
        }
        
        try {
            // Create user in database
            User.UserBuilder userBuilder = User.builder()
                    .email(userData.getEmail())
                    .firstName(userData.getFirstName())
                    .lastName(userData.getLastName())
                    .phone(userData.getPhone())
                    .department(userData.getDepartment())
                    .role(userData.getRole() != null ? userData.getRole() : UserRole.EMPLOYEE);
            
            // Set manager if provided
            if (userData.getManagerId() != null) {
                User manager = userRepository.findById(userData.getManagerId())
                        .orElseThrow(() -> new UserNotFoundException(userData.getManagerId()));
                
                // Validate manager can be assigned
                validationService.validateUserCanBeManager(manager);
                
                userBuilder.manager(manager);
            } else if (context.getCurrentUserRole() == UserRole.MANAGER) {
                // If manager creates user without specifying manager, assign to themselves
                User currentUserEntity = userRepository.findById(context.getCurrentUserId())
                        .orElseThrow(() -> new UserNotFoundException(context.getCurrentUserId()));
                userBuilder.manager(currentUserEntity);
            }
            
            User user = userBuilder.build();
            
            // Validate no circular reference
            if (user.getManager() != null) {
                validationService.validateNoCircularReference(user, user.getManager());
            }
            
            User savedUser = userRepository.save(user);
            
            log.info("User created - Email: {}, ID: {}, Keycloak ID: {}, Created by: {}", 
                userData.getEmail(), savedUser.getId(), 
                keycloakUserId, context.getCurrentUserId());
            return userMapper.toDto(savedUser);
            
        } catch (Exception e) {
            // Rollback: delete from Keycloak if database creation fails
            log.error("Failed to create user in database, rolling back Keycloak user", e);
            try {
                keycloakService.deleteKeycloakUser(keycloakUserId);
            } catch (Exception deleteEx) {
                log.error("Failed to delete Keycloak user during rollback", deleteEx);
            }
            throw e;
        }
    }

    public boolean canViewAllData(UUID userId, UUID currentUserId, UserRole currentRole) {
        return currentRole.canViewAll() || Objects.equals(userId, currentUserId);
    }

    public List<UserDto> getDirectReports(UUID managerId) {
        return userMapper.toDtoList(userRepository.findByManagerId(managerId));
    }

    public List<UserDto> getAvailableManagers() {
        return userMapper.toDtoList(userRepository.findByRole(UserRole.MANAGER));
    }

    public Optional<UserDto> getManagerOf(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getManager() == null) {
            return Optional.empty();
        }
        return Optional.of(userMapper.toDto(user.getManager()));
    }

    @Transactional
    public void assignManager(UUID userId, UUID managerId, UUID currentUserId, UserRole currentRole) {
        if (!canEditProfile(userId, currentUserId, currentRole)) {
            throw new UnauthorizedException("assign manager to", "this user");
        }

        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new UserNotFoundException(managerId));

        if (!manager.getRole().canBeAssignedAsManager()) {
            throw new InvalidOperationException(
                "assign user as manager", 
                "user with role " + manager.getRole() + " cannot be a manager"
            );
        }

        if (wouldCreateCycle(employee, manager)) {
            throw new InvalidOperationException(
                "assign this manager", 
                "it would create a circular hierarchy"
            );
        }

        employee.setManager(manager);
        userRepository.save(employee);
        log.debug("Manager assigned: {} → {}", managerId, userId);
    }

    private void updateRoleIfAuthorized(User user, UserRole currentRole, String newRole) {
        Optional.ofNullable(newRole)
                .filter(r -> currentRole.isManagerOrAbove())
                .flatMap(UserRole::fromString)
                .ifPresent(user::setRole);
    }

    private void updateManagerIfProvided(User user, UserRole currentRole, UUID managerId) {
        if (!currentRole.isManagerOrAbove() || managerId == null) {
            return;
        }

        // Check for special UUID to indicate removal of manager
        if (managerId.toString().equals("00000000-0000-0000-0000-000000000000")) {
            user.setManager(null);
            return;
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new UserNotFoundException(managerId));
        
        if (!manager.getRole().canBeAssignedAsManager()) {
            throw new InvalidOperationException(
                "assign user as manager",
                "user with role " + manager.getRole() + " cannot be a manager"
            );
        }
        
        user.setManager(manager);
    }

    private boolean wouldCreateCycle(User employee, User newManager) {
        User current = newManager;
        while (current != null) {
            if (Objects.equals(current.getId(), employee.getId())) {
                return true;
            }
            current = current.getManager();
        }
        return false;
    }

    private void maskSensitiveData(UserDto dto) {
        dto.setEmail(null);
        dto.setPhone(null);
        dto.setUpdatedAt(null);
    }

    public PageResponse<com.example.hrcore.dto.FeedbackDto> getEmployeeFeedback(
            UUID userId, String type, int page, int size, String sortBy, String sortDirection,
            UUID currentUserId, UserRole currentRole) {
        
        log.info("Getting feedback for employee {} (type: {}) by user {} with role {}", 
            userId, type, currentUserId, currentRole);
        
        if (!currentRole.isManagerOrAbove()) {
            throw new UnauthorizedException("view employee feedback", "requires manager role or above");
        }
        
        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Employee not found: " + userId));
        
        com.example.hrcore.dto.FeedbackOperationContext context = 
            com.example.hrcore.dto.FeedbackOperationContext.builder()
                .currentUserId(currentUserId)
                .currentUserRole(currentRole)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        if ("received".equalsIgnoreCase(type)) {
            return feedbackService.getReceivedFeedback(userId, context);
        } else if ("given".equalsIgnoreCase(type)) {
            return feedbackService.getGivenFeedback(userId, context);
        } else {
            com.example.hrcore.dto.FeedbackFilterDto filters = 
                com.example.hrcore.dto.FeedbackFilterDto.builder()
                    .toUserId(userId)
                    .fromUserId(userId)
                    .build();
            
            com.example.hrcore.dto.FeedbackOperationContext newContext = 
                com.example.hrcore.dto.FeedbackOperationContext.builder()
                    .currentUserId(currentUserId)
                    .currentUserRole(currentRole)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
            
            return feedbackService.searchFeedback(filters, newContext);
        }
    }
    
    public PageResponse<com.example.hrcore.dto.AbsenceRequestDto> getEmployeeAbsenceRequests(
            UUID userId, int page, int size, UUID currentUserId, UserRole currentRole) {
        
        log.info("Getting absence requests for employee {} by user {} with role {}", 
            userId, currentUserId, currentRole);
        
        if (!currentRole.isManagerOrAbove()) {
            throw new UnauthorizedException("view employee absence requests", "requires manager role or above");
        }
        
        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Employee not found: " + userId));
        
        com.example.hrcore.dto.AbsenceRequestOperationContext context = 
            com.example.hrcore.dto.AbsenceRequestOperationContext.builder()
                .currentUserId(currentUserId)
                .currentUserRole(currentRole)
                .build();
        
        return absenceRequestService.getUserRequests(userId, page, size, context);
    }
    
    public PageResponse<com.example.hrcore.dto.FeedbackDto> getEmployeeFeedback(
            UUID userId, int page, int size, String sortBy, String sortDirection,
            UUID currentUserId, UserRole currentRole) {
        
        log.info("Getting feedback for employee {} by user {} with role {}", 
            userId, currentUserId, currentRole);
        
        if (!currentRole.isManagerOrAbove()) {
            throw new UnauthorizedException("view employee feedback", "requires manager role or above");
        }
        
        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Employee not found: " + userId));
        
        com.example.hrcore.dto.FeedbackOperationContext context = 
            com.example.hrcore.dto.FeedbackOperationContext.builder()
                .currentUserId(currentUserId)
                .currentUserRole(currentRole)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        return feedbackService.getReceivedFeedback(userId, context);
    }

    /**
     * Get feedback for a user on their profile with visibility rules:
     * - Own user: sees all APPROVED feedback received
     * - Direct manager/Admin: sees ALL feedback (all statuses)
     * - Other users: see only feedback they personally gave to this user
     */
    public PageResponse<com.example.hrcore.dto.FeedbackDto> getUserFeedback(
            UUID userId, 
            String statusFilter,
            com.example.hrcore.dto.FeedbackOperationContext context) {
        
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        boolean isOwnProfile = Objects.equals(userId, context.getCurrentUserId());
        boolean isManager = context.getCurrentUserRole() == UserRole.MANAGER;
        boolean isAdmin = context.getCurrentUserRole() == UserRole.SUPER_ADMIN;
        
        com.example.hrcore.dto.FeedbackFilterDto.FeedbackFilterDtoBuilder filterBuilder = 
            com.example.hrcore.dto.FeedbackFilterDto.builder()
                .toUserId(userId);
        
        // Apply visibility rules
        if (isManager || isAdmin) {
            // Managers and admins: all feedback for this user, optionally filtered by status
            if (statusFilter != null && !statusFilter.equals("all")) {
                filterBuilder.status(com.example.hrcore.entity.enums.FeedbackStatus.valueOf(statusFilter.toUpperCase()));
            }
        } else if (isOwnProfile) {
            // Own user (employee): only APPROVED feedback
            filterBuilder.status(com.example.hrcore.entity.enums.FeedbackStatus.APPROVED);
        } else {
            // Other users: only feedback they gave to this user
            filterBuilder.fromUserId(context.getCurrentUserId());
            // Show only approved feedback others gave
            filterBuilder.status(com.example.hrcore.entity.enums.FeedbackStatus.APPROVED);
        }
        
        return feedbackService.searchFeedback(filterBuilder.build(), context);
    }
}

