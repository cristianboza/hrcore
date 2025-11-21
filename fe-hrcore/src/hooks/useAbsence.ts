import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { absenceService, AbsenceRequestSearchRequest, PageResponse } from '../services/absenceService';
import { AbsenceRequest } from '../types/absence';

/**
 * Hook to search absence requests with advanced filters
 */
export const useSearchAbsenceRequests = (searchRequest: AbsenceRequestSearchRequest) => {
  return useQuery<PageResponse<AbsenceRequest>>({
    queryKey: ['absenceRequests', 'search', searchRequest],
    queryFn: () => absenceService.searchRequests(searchRequest),
    staleTime: 1000 * 60 * 5,
  });
};

/**
 * Backward compatibility: Get absence requests for a specific user
 */
export const useAbsenceRequests = (userId: string, page: number = 0, size: number = 10) => {
  return useSearchAbsenceRequests({ userId, page, size });
};

/**
 * Backward compatibility: Get pending absence requests (for managers)
 */
export const usePendingAbsenceRequests = (page: number = 0, size: number = 10) => {
  return useSearchAbsenceRequests({ status: 'PENDING', page, size });
};

/**
 * Hook to submit a new absence request
 */
export const useSubmitAbsenceRequest = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      userId,
      startDate,
      endDate,
      type,
      reason,
    }: {
      userId: string;
      startDate: string;
      endDate: string;
      type: string;
      reason?: string;
    }) => absenceService.submitRequest(userId, startDate, endDate, type, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['absenceRequests'] });
    },
  });
};

/**
 * Hook to approve an absence request (Manager+ only)
 */
export const useApproveAbsenceRequest = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ requestId }: { requestId: number }) =>
      absenceService.approveRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['absenceRequests'] });
    },
  });
};

/**
 * Hook to reject an absence request (Manager+ only)
 */
export const useRejectAbsenceRequest = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ requestId, reason }: { requestId: number; reason: string }) =>
      absenceService.rejectRequest(requestId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['absenceRequests'] });
    },
  });
};

/**
 * Hook for manager to update an absence request (Manager+ only)
 */
export const useManagerUpdateAbsenceRequest = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ 
      requestId, 
      status, 
      managerComment 
    }: { 
      requestId: number; 
      status?: string; 
      managerComment?: string;
    }) =>
      absenceService.managerUpdateRequest(requestId, { status, managerComment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['absenceRequests'] });
    },
  });
};

/**
 * Hook to check for conflicting absence requests
 */
export const useCheckConflicts = (
  userId: string,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 10
) => {
  return useQuery<PageResponse<AbsenceRequest>>({
    queryKey: ['absenceRequests', 'conflicts', userId, startDate, endDate, page, size],
    queryFn: () => absenceService.checkConflicts(userId, startDate, endDate, page, size),
    enabled: !!(userId && startDate && endDate),
    staleTime: 0, // Don't cache conflicts
  });
};
