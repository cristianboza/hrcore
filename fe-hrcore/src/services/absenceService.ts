import apiClient from './apiClient';
import { AbsenceRequest } from '../types/absence';

/**
 * Page response wrapper for paginated data
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
  first: boolean;
  last: boolean;
}

/**
 * Request for searching absence requests
 */
export interface AbsenceRequestSearchRequest {
  userId?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  type?: 'VACATION' | 'SICK_LEAVE' | 'PERSONAL' | 'OTHER';
  search?: string;
  startDateFrom?: string;
  startDateTo?: string;
  endDateFrom?: string;
  endDateTo?: string;
  approverId?: string;
  managerId?: string;
  hasRejectionReason?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const absenceService = {
  /**
   * Search absence requests with advanced filtering and pagination
   */
  searchRequests: async (searchRequest: AbsenceRequestSearchRequest = {}): Promise<PageResponse<AbsenceRequest>> => {
    const response = await apiClient.post('/absence-requests/search', {
      userId: searchRequest.userId,
      status: searchRequest.status,
      type: searchRequest.type,
      search: searchRequest.search,
      startDateFrom: searchRequest.startDateFrom,
      startDateTo: searchRequest.startDateTo,
      endDateFrom: searchRequest.endDateFrom,
      endDateTo: searchRequest.endDateTo,
      approverId: searchRequest.approverId,
      managerId: searchRequest.managerId,
      hasRejectionReason: searchRequest.hasRejectionReason,
      page: searchRequest.page || 0,
      size: searchRequest.size || 10,
      sortBy: searchRequest.sortBy || 'createdAt',
      sortDirection: searchRequest.sortDirection || 'DESC',
    });
    return response.data;
  },

  /**
   * Submit a new absence request
   */
  submitRequest: async (
    userId: string,
    startDate: string,
    endDate: string,
    type: string,
    reason?: string
  ): Promise<AbsenceRequest> => {
    const params = new URLSearchParams();
    params.append('userId', userId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    params.append('type', type);
    if (reason) params.append('reason', reason);
    
    const response = await apiClient.post(`/absence-requests?${params.toString()}`);
    return response.data;
  },

  /**
   * Approve an absence request (Manager+ only)
   */
  approveRequest: async (requestId: number): Promise<AbsenceRequest> => {
    const response = await apiClient.put(`/absence-requests/${requestId}/approve`);
    return response.data;
  },

  /**
   * Reject an absence request (Manager+ only)
   */
  rejectRequest: async (requestId: number, reason: string): Promise<AbsenceRequest> => {
    const params = new URLSearchParams();
    params.append('reason', reason);
    const response = await apiClient.put(`/absence-requests/${requestId}/reject?${params.toString()}`);
    return response.data;
  },

  /**
   * Check for conflicting absence requests with pagination
   */
  checkConflicts: async (
    userId: string, 
    startDate: string, 
    endDate: string,
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<AbsenceRequest>> => {
    const response = await apiClient.post('/absence-requests/conflicts', {
      userId,
      startDate,
      endDate,
      page,
      size,
    });
    return response.data;
  },

  /**
   * Manager update of absence request (Manager+ only)
   */
  managerUpdateRequest: async (
    requestId: number,
    update: { status?: string; managerComment?: string }
  ): Promise<AbsenceRequest> => {
    const response = await apiClient.patch(`/absence-requests/${requestId}/manager-update`, update);
    return response.data;
  },
};
