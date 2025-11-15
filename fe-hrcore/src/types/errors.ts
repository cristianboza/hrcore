/**
 * Error Types
 */

export interface AppError {
  code: string;
  message: string;
  status?: number;
  details?: Record<string, unknown>;
  name: 'AppError';
}

export interface ValidationError {
  code: 'VALIDATION_ERROR';
  message: string;
  status: 400;
  fields?: Record<string, string[]>;
  name: 'ValidationError';
}

export interface AuthenticationError {
  code: 'AUTH_ERROR';
  message: string;
  status: 401;
  name: 'AuthenticationError';
}

export interface AuthorizationError {
  code: 'AUTHZ_ERROR';
  message: string;
  status: 403;
  name: 'AuthorizationError';
}

export interface NotFoundError {
  code: 'NOT_FOUND';
  message: string;
  status: 404;
  name: 'NotFoundError';
}

export interface ConflictError {
  code: 'CONFLICT';
  message: string;
  status: 409;
  name: 'ConflictError';
}

export interface NetworkError {
  code: 'NETWORK_ERROR';
  message: string;
  name: 'NetworkError';
}

export interface ErrorResponse {
  code: string;
  message: string;
  status: number;
  details?: Record<string, unknown>;
}

export type ApplicationError =
  | AppError
  | ValidationError
  | AuthenticationError
  | AuthorizationError
  | NotFoundError
  | ConflictError
  | NetworkError;
