/**
 * Keycloak Integration Types
 */

export interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

export interface KeycloakProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  username: string;
  emailVerified: boolean;
  realmAccess?: {
    roles: string[];
  };
}

export interface KeycloakTokenParsed {
  exp: number;
  iat: number;
  auth_time: number;
  jti: string;
  iss: string;
  aud: string;
  sub: string;
  email: string;
  given_name: string;
  family_name: string;
  preferred_username: string;
  realm_access?: {
    roles: string[];
  };
  resource_access?: Record<string, { roles: string[] }>;
}

export interface KeycloakEventCallbacks {
  onReady?(): void;
  onAuthSuccess?(): void;
  onAuthError?(): void;
  onAuthRefreshSuccess?(): void;
  onAuthRefreshError?(): void;
  onTokenExpired?(): void;
}
