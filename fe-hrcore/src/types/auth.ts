/**
 * Authentication and User Types
 */

export type UserRole = 'SUPER_ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  department?: string;
  role: UserRole;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthRequest {
  email?: string;
  keycloakToken?: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
  department?: string;
}

export interface AuthToken {
  token: string;
  expiresIn: number;
  tokenType: 'Bearer';
}

export interface KeycloakToken {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
  token_type: 'Bearer';
  scope?: string;
}

export interface CurrentUser extends User {}

export interface AuthState {
  currentUser: CurrentUser | null;
  token: string | null;
  isAuthenticated: () => boolean;
}
