import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {absenceService} from '../services/absenceService';

export const useAbsenceRequests = (userId: number) => {
    return useQuery({
        queryKey: ['absenceRequests', userId],
        queryFn: () => absenceService.getUserRequests(userId),
        staleTime: 1000 * 60 * 5,
    });
};

export const usePendingAbsenceRequests = () => {
    return useQuery({
        queryKey: ['absenceRequests', 'pending'],
        queryFn: () => absenceService.getPendingRequests(),
        staleTime: 1000 * 60 * 5,
    });
};

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
            userId: number;
            startDate: string;
            endDate: string;
            type: string;
            reason?: string;
        }) => absenceService.submitRequest(userId, startDate, endDate, type, reason),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['absenceRequests']});
        },
    });
};

export const useApproveAbsenceRequest = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({requestId, approverId}: { requestId: number; approverId: number }) =>
            absenceService.approveRequest(requestId, approverId),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['absenceRequests']});
        },
    });
};

export const useRejectAbsenceRequest = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({requestId, approverId, reason}: { requestId: number; approverId: number; reason: string }) =>
            absenceService.rejectRequest(requestId, approverId, reason),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['absenceRequests']});
        },
    });
};

export const useManagerUpdateAbsenceRequest = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ requestId, managerId, status, managerComment }: { requestId: number; managerId: number; status?: string; managerComment?: string }) =>
            absenceService.managerUpdateRequest(requestId, managerId, { status, managerComment }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['absenceRequests'] });
        },
    });
};
