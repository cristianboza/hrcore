package com.example.hrcore.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Getter
public enum UserRole {
    SUPER_ADMIN("Super Administrator", 3, Set.of()),
    MANAGER("Manager", 2, Set.of("EMPLOYEE")),
    EMPLOYEE("Employee", 1, Set.of());

    private final String displayName;
    private final int hierarchyLevel;
    private final Set<String> canManageRoles;

    public boolean canManageRole(UserRole targetRole) {
        if (this == SUPER_ADMIN) {
            return true;
        }
        return canManageRoles.contains(targetRole.name());
    }

    public boolean canEdit(UserRole targetUserRole, boolean isSameUser) {
        if (this == SUPER_ADMIN) {
            return true;
        }
        if (this == MANAGER) {
            return true;
        }
        return isSameUser;
    }

    public boolean canDelete() {
        return this == SUPER_ADMIN || this == MANAGER;
    }

    public boolean canViewAll() {
        return this == SUPER_ADMIN || this == MANAGER;
    }

    public boolean isManager() {
        return this == MANAGER;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    public boolean isEmployee() {
        return this == EMPLOYEE;
    }

    public boolean isManagerOrAbove() {
        return hierarchyLevel >= MANAGER.hierarchyLevel;
    }

    public boolean canBeAssignedAsManager() {
        return this == MANAGER;
    }

    public boolean hasHigherPrivilegesThan(UserRole other) {
        return this.hierarchyLevel > other.hierarchyLevel;
    }

    public static Optional<UserRole> fromString(String role) {
        return Optional.ofNullable(role)
                .filter(r -> !r.isBlank())
                .flatMap(r -> Arrays.stream(values())
                        .filter(userRole -> userRole.name().equalsIgnoreCase(r))
                        .findFirst());
    }

    public static UserRole fromStringOrDefault(String role, UserRole defaultRole) {
        return fromString(role).orElse(defaultRole);
    }
}
