package com.example.hrcore.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakTokenProvider {

    public String getSubjectFromToken(Jwt token) {
        return token.getClaimAsString("sub");
    }

    public String getEmailFromToken(Jwt token) {
        return token.getClaimAsString("email");
    }

    public String getPreferredUsernameFromToken(Jwt token) {
        return token.getClaimAsString("preferred_username");
    }

    public String getGivenNameFromToken(Jwt token) {
        return token.getClaimAsString("given_name");
    }

    public String getFamilyNameFromToken(Jwt token) {
        return token.getClaimAsString("family_name");
    }

    public List<String> getRolesFromToken(Jwt token) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) token.getClaims().get("realm_access");
        
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles;
        }
        
        return List.of();
    }

    public boolean hasRole(Jwt token, String role) {
        List<String> roles = getRolesFromToken(token);
        return roles != null && roles.contains(role);
    }
}
