import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {feedbackService, FeedbackSearchRequest, PageResponse} from '../services/feedbackService';
import {Feedback} from '../types/feedback';

export const useSearchFeedback = (searchRequest: FeedbackSearchRequest) => {
    return useQuery<PageResponse<Feedback>>({
        queryKey: ['feedback', 'search', searchRequest],
        queryFn: () => feedbackService.searchFeedback(searchRequest),
        staleTime: 1000 * 60 * 5,
    });
};

/**
 * Backward compatibility: Get feedback for a specific user
 * @param userId - User ID to get feedback for
 * @param type - 'received' or 'given'
 */
export const useFeedback = (userId: string, type?: 'received' | 'given', page: number = 0, size: number = 10) => {
    return useSearchFeedback({
        userId: type === 'received' ? userId : undefined,
        fromUserId: type === 'given' ? userId : undefined,
        page,
        size,
    });
};

/**
 * Backward compatibility: Get pending feedback (for managers)
 */
export const usePendingFeedback = (page: number = 0, size: number = 10) => {
    return useSearchFeedback({ status: 'PENDING', page, size });
};

export const useSubmitFeedback = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({fromUserId, toUserId, content}: { fromUserId: string; toUserId: string; content: string }) =>
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

