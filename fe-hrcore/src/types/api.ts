/**
 * API Request and Response Types
 */

export interface ApiResponse<T> {
  data: T;
  status: number;
  message?: string;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: Record<string, string[]>;
  timestamp?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ApiErrorResponse {
  error: ApiError;
}

export type ApiResult<T> = 
  | { success: true; data: T }
  | { success: false; error: ApiError };
