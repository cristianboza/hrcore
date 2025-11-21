package com.example.hrcore.service;

import com.example.hrcore.dto.UserCreationData;
import com.example.hrcore.dto.UserOperationContext;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Service for validating business rules and permissions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final UserRepository userRepository;

    /**
     * Validate if current user can create a user with the given role
     */
    public void validateCanCreateUserWithRole(UserRole currentRole, UserRole targetRole) {
        if (currentRole == UserRole.SUPER_ADMIN) {
            return; // Super admin can create any role
        }
        
        if (currentRole == UserRole.MANAGER) {
            if (targetRole != UserRole.EMPLOYEE) {
                log.warn("Manager attempted to create user with role: {}", targetRole);
                throw new UnauthorizedException(
                    "create user with role " + targetRole,
                    "managers can only create employees"
                );
            }
            return;
        }
        
        // Employees cannot create users
        throw new UnauthorizedException("create users", "only managers and super admins can create users");
    }

    /**
     * Validate if current user can assign a specific manager
     */
    public void validateCanAssignManager(UUID managerId, UserOperationContext context) {
        if (context.getCurrentUserRole() == UserRole.SUPER_ADMIN) {
            return; // Super admin can assign any manager
        }
        
        if (context.getCurrentUserRole() == UserRole.MANAGER) {
            if (!Objects.equals(managerId, context.getCurrentUserId())) {
                log.warn("Manager {} attempted to assign different manager: {}", 
                    context.getCurrentUserId(), managerId);
                throw new UnauthorizedException(
                    "assign manager",
                    "managers can only assign employees to themselves"
                );
            }
            return;
        }
        
        throw new UnauthorizedException("assign manager", "insufficient permissions");
    }

    /**
     * Validate if user can be assigned as a manager
     */
    public void validateUserCanBeManager(User user) {
        if (user == null) {
            throw new UserNotFoundException("Manager user not found");
        }
        
        if (!user.getRole().canBeAssignedAsManager()) {
            log.warn("Attempted to assign user {} with role {} as manager", user.getId(), user.getRole());
            throw new InvalidOperationException(
                "assign user as manager",
                "user with role " + user.getRole() + " cannot be a manager"
            );
        }
    }

    /**
     * Validate if current user can edit the target user
     */
    public void validateCanEditUser(UUID targetUserId, UserOperationContext context) {
        if (context.getCurrentUserRole() == UserRole.SUPER_ADMIN) {
            return; // Super admin can edit anyone
        }
        
        if (Objects.equals(targetUserId, context.getCurrentUserId())) {
            return; // Can edit own profile
        }
        
        if (context.getCurrentUserRole() == UserRole.MANAGER) {
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new UserNotFoundException(targetUserId));
            
            // Cannot edit other managers or super admins
            if (targetUser.getRole() != UserRole.EMPLOYEE) {
                log.warn("Manager {} attempted to edit user {} with role {}", 
                    context.getCurrentUserId(), targetUserId, targetUser.getRole());
                throw new UnauthorizedException(
                    "edit this profile",
                    "managers can only edit employees in their hierarchy"
                );
            }
            
            // Check if user is in manager's hierarchy
            if (!isInManagerHierarchy(targetUserId, context.getCurrentUserId())) {
                log.warn("Manager {} attempted to edit user {} outside their hierarchy", 
                    context.getCurrentUserId(), targetUserId);
                throw new UnauthorizedException(
                    "edit this profile",
                    "user is not in your management hierarchy"
                );
            }
            return;
        }
        
        throw new UnauthorizedException("edit this profile", "insufficient permissions");
    }

    /**
     * Validate if current user can delete the target user
     */
    public void validateCanDeleteUser(UUID targetUserId, UserOperationContext context) {
        if (context.getCurrentUserRole() == UserRole.SUPER_ADMIN) {
            return; // Super admin can delete anyone
        }
        
        if (context.getCurrentUserRole() == UserRole.MANAGER) {
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new UserNotFoundException(targetUserId));
            
            // Cannot delete other managers or super admins
            if (targetUser.getRole() != UserRole.EMPLOYEE) {
                log.warn("Manager {} attempted to delete user {} with role {}", 
                    context.getCurrentUserId(), targetUserId, targetUser.getRole());
                throw new UnauthorizedException(
                    "delete this profile",
                    "managers can only delete employees in their hierarchy"
                );
            }
            
            // Check if user is in manager's hierarchy
            if (!isInManagerHierarchy(targetUserId, context.getCurrentUserId())) {
                log.warn("Manager {} attempted to delete user {} outside their hierarchy", 
                    context.getCurrentUserId(), targetUserId);
                throw new UnauthorizedException(
                    "delete this profile",
                    "user is not in your management hierarchy"
                );
            }
            return;
        }
        
        throw new UnauthorizedException("delete this profile", "insufficient permissions");
    }

    /**
     * Validate if email is available
     */
    public void validateEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Attempted to create user with existing email: {}", email);
            throw new InvalidOperationException(
                "create user",
                "A user with this email address already exists"
            );
        }
    }

    /**
     * Validate password strength
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidOperationException(
                "set password",
                "password must be at least 8 characters long"
            );
        }
        
        // Add more password rules if needed
        // - Must contain uppercase
        // - Must contain lowercase
        // - Must contain number
        // - Must contain special character
    }

    /**
     * Validate if assigning a manager would create a circular reference
     */
    public void validateNoCircularReference(User employee, User newManager) {
        if (employee == null || newManager == null) {
            return;
        }
        
        User current = newManager;
        while (current != null) {
            if (Objects.equals(current.getId(), employee.getId())) {
                log.warn("Circular reference detected: employee {} cannot be manager of {}", 
                    employee.getId(), newManager.getId());
                throw new InvalidOperationException(
                    "assign this manager",
                    "this would create a circular hierarchy"
                );
            }
            current = current.getManager();
        }
    }

    /**
     * Check if a user is in a manager's hierarchy
     */
    public boolean isInManagerHierarchy(UUID userId, UUID managerId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        User currentManager = user.getManager();
        while (currentManager != null) {
            if (Objects.equals(currentManager.getId(), managerId)) {
                return true;
            }
            currentManager = currentManager.getManager();
        }
        
        return false;
    }

    /**
     * Validate business rules for user creation
     */
    public void validateUserCreation(UserCreationData userData, UserOperationContext context) {
        // Validate email is available
        validateEmailAvailable(userData.getEmail());
        
        // Validate password strength
        validatePassword(userData.getPassword());
        
        // Validate role permissions
        validateCanCreateUserWithRole(context.getCurrentUserRole(), userData.getRole());
        
        // Validate manager assignment if provided
        if (userData.getManagerId() != null) {
            validateCanAssignManager(userData.getManagerId(), context);
        }
    }

    /**
     * Validate business rules for user update
     */
    public void validateUserUpdate(UserOperationContext context) {
        validateCanEditUser(context.getTargetUserId(), context);
    }

    /**
     * Validate business rules for user deletion
     */
    public void validateUserDeletion(UserOperationContext context) {
        validateCanDeleteUser(context.getTargetUserId(), context);
    }
}
