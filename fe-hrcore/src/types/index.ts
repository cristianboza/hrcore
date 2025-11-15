/**
 * Main Types Export File
 * Centralized type definitions for the entire application
 */

// API Types
export type { ApiResponse, ApiError, PaginatedResponse, ApiErrorResponse, ApiResult } from './api';

// Authentication Types
export type { User, AuthRequest, AuthResponse, AuthToken, KeycloakToken, CurrentUser, AuthState } from './auth';
export type { UserRole } from './auth';

// Admin Types
export type {
  LoggedUserSession,
  ValidToken,
  ForceLogoutRequest,
  ForceLogoutResponse,
  SessionStats,
} from './admin';

// Keycloak Types
export type {
  KeycloakConfig,
  KeycloakProfile,
  KeycloakTokenParsed,
  KeycloakEventCallbacks,
} from './keycloak';

// Component Types
export type {
  ButtonProps,
  FormInputProps,
  FormSelectProps,
  CardProps,
  ModalProps,
  BaseComponentProps,
  AuthContextType,
  ToastMessage,
  ConfirmDialogProps,
  EventHandler,
  ClickEventHandler,
  ChangeEventHandler,
} from './components';

// Error Types
export type {
  AppError,
  ValidationError,
  AuthenticationError,
  AuthorizationError,
  NotFoundError,
  ConflictError,
  NetworkError,
  ErrorResponse,
  ApplicationError,
} from './errors';

// Absence Types
export type { AbsenceRequest, AbsenceStatus, AbsenceType } from './absence';
export { ABSENCE_STATUS, ABSENCE_TYPE } from './absence';

// Feedback Types
export type { Feedback, FeedbackStatus, CreateFeedbackRequest } from './feedback';
export { FEEDBACK_STATUS } from './feedback';
