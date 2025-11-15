import apiClient from './apiClient';
import { AbsenceRequest } from '../types/absence';

export const absenceService = {
  submitRequest: async (
    userId: number,
    startDate: string,
    endDate: string,
    type: string,
    reason?: string
  ): Promise<AbsenceRequest> => {
    const response = await apiClient.post(
      `/absence-requests?userId=${userId}&startDate=${startDate}&endDate=${endDate}&type=${type}&reason=${reason || ''}`
    );
    return response.data;
  },

  getUserRequests: async (userId: number): Promise<AbsenceRequest[]> => {
    const response = await apiClient.get(`/absence-requests?userId=${userId}`);
    return response.data;
  },

  getPendingRequests: async (): Promise<AbsenceRequest[]> => {
    const response = await apiClient.get('/absence-requests/pending');
    return response.data;
  },

  approveRequest: async (requestId: number, approverId: number): Promise<AbsenceRequest> => {
    const response = await apiClient.put(`/absence-requests/${requestId}/approve?approverId=${approverId}`);
    return response.data;
  },

  rejectRequest: async (requestId: number, approverId: number, reason: string): Promise<AbsenceRequest> => {
    const response = await apiClient.put(
      `/absence-requests/${requestId}/reject?approverId=${approverId}&reason=${encodeURIComponent(reason)}`
    );
    return response.data;
  },

  checkConflicts: async (userId: number, startDate: string, endDate: string): Promise<AbsenceRequest[]> => {
    const response = await apiClient.get(`/absence-requests/conflicts?userId=${userId}&startDate=${startDate}&endDate=${endDate}`);
    return response.data;
  },

  managerUpdateRequest: async (
    requestId: number,
    managerId: number,
    update: { status?: string; managerComment?: string }
  ): Promise<AbsenceRequest> => {
    const response = await apiClient.patch(
      `/absence-requests/${requestId}/manager-update?managerId=${managerId}`,
      update
    );
    return response.data;
  },
};
