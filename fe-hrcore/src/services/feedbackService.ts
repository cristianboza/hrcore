import apiClient from './apiClient';
import { Feedback } from '../types/feedback';

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
 * Filters for searching feedback
 */
export interface FeedbackSearchRequest {
  userId?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  fromUserId?: string;
  createdAfter?: string;
  createdBefore?: string;
  contentContains?: string;
  hasPolishedContent?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const feedbackService = {
  /**
   * Search feedback with advanced filtering and pagination
   */
  searchFeedback: async (searchRequest: FeedbackSearchRequest = {}): Promise<PageResponse<Feedback>> => {
    const response = await apiClient.post('/feedback/search', {
      userId: searchRequest.userId,
      status: searchRequest.status,
      fromUserId: searchRequest.fromUserId,
      createdAfter: searchRequest.createdAfter,
      createdBefore: searchRequest.createdBefore,
      contentContains: searchRequest.contentContains,
      hasPolishedContent: searchRequest.hasPolishedContent,
      page: searchRequest.page || 0,
      size: searchRequest.size || 10,
      sortBy: searchRequest.sortBy || 'createdAt',
      sortDirection: searchRequest.sortDirection || 'DESC',
    });
    return response.data;
  },

  /**
   * Submit feedback from one user to another
   */
  submitFeedback: async (
    fromUserId: string,
    toUserId: string,
    content: string
  ): Promise<Feedback> => {
    const params = new URLSearchParams();
    params.append('fromUserId', fromUserId);
    params.append('toUserId', toUserId);
    const response = await apiClient.post(`/feedback?${params.toString()}`, content);
    return response.data;
  },

  /**
   * Approve feedback (Manager+ only)
   */
  approveFeedback: async (feedbackId: number): Promise<Feedback> => {
    const response = await apiClient.put(`/feedback/${feedbackId}/approve`);
    return response.data;
  },

  /**
   * Reject feedback (Manager+ only)
   */
  rejectFeedback: async (feedbackId: number): Promise<Feedback> => {
    const response = await apiClient.put(`/feedback/${feedbackId}/reject`);
    return response.data;
  },

  /**
   * Polish feedback with AI (Manager+ only)
   */
  polishFeedback: async (feedbackId: number): Promise<Feedback> => {
    const response = await apiClient.post(`/feedback/${feedbackId}/polish`);
    return response.data;
  },

  /**
   * Get feedback for a specific user on their profile
   * Visibility rules applied on backend:
   * - Own user: sees APPROVED feedback received
   * - Direct manager/Admin: sees ALL feedback
   * - Other users: see only feedback they gave
   */
  getUserFeedback: async (userId: string, searchRequest: Partial<FeedbackSearchRequest> = {}): Promise<PageResponse<Feedback>> => {
    const response = await apiClient.post(`/profiles/${userId}/feedback/search`, {
      status: searchRequest.status,
      page: searchRequest.page || 0,
      size: searchRequest.size || 10,
      sortBy: searchRequest.sortBy || 'createdAt',
      sortDirection: searchRequest.sortDirection || 'DESC',
    });
    return response.data;
  },
};
