import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {feedbackService} from '../services/feedbackService';

export const useFeedback = (userId: number, type: 'received' | 'given' = 'received') => {
    return useQuery({
        queryKey: ['feedback', userId, type],
        queryFn: () => type === 'received' ? feedbackService.getReceivedFeedback(userId) : feedbackService.getGivenFeedback(userId),
        staleTime: 1000 * 60 * 5,
    });
};

export const usePendingFeedback = () => {
    return useQuery({
        queryKey: ['feedback', 'pending'],
        queryFn: () => feedbackService.getPendingFeedback(),
        staleTime: 1000 * 60 * 5,
    });
};

export const useSubmitFeedback = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({fromUserId, toUserId, content}: { fromUserId: number; toUserId: number; content: string }) =>
            feedbackService.submitFeedback(fromUserId, toUserId, content),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['feedback']});
        },
    });
};

export const usePolishFeedback = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (feedbackId: number) => feedbackService.polishFeedback(feedbackId),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['feedback']});
        },
    });
};

export const useApproveFeedback = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (feedbackId: number) => feedbackService.approveFeedback(feedbackId),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['feedback']});
        },
    });
};

export const useRejectFeedback = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (feedbackId: number) => feedbackService.rejectFeedback(feedbackId),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['feedback']});
        },
    });
};

