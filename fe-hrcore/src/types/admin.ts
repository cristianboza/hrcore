/**
 * Admin and Token Management Types
 */

export interface LoggedUserSession {
  tokenId: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  issuedAt: string;
  expiresAt: string;
  lastUsedAt?: string;
  ipAddress?: string;
  userAgent?: string;
  deviceName?: string;
  isActive: boolean;
}

export interface ValidToken {
  id: number;
  tokenJti: string;
  userId: number;
  tokenHash: string;
  issuedAt: string;
  expiresAt: string;
  lastUsedAt?: string;
  ipAddress?: string;
  userAgent?: string;
  deviceName?: string;
  isActive: boolean;
}

export interface ForceLogoutRequest {
  tokenId: number;
}

export interface ForceLogoutResponse {
  success: boolean;
  message: string;
}

export interface SessionStats {
  totalActiveSessions: number;
  totalUsers: number;
  sessionsPerUser: Record<number, number>;
}
