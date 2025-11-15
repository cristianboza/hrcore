import apiClient from './apiClient';
import {Feedback} from '../types/feedback';

export const feedbackService = {
    submitFeedback: async (
        fromUserId: number,
        toUserId: number,
        content: string
    ): Promise<Feedback> => {
        const response = await apiClient.post(
            `/feedback?fromUserId=${fromUserId}&toUserId=${toUserId}`,
            content
        );
        return response.data;
    },

    getReceivedFeedback: async (userId: number): Promise<Feedback[]> => {
        const response = await apiClient.get(`/feedback/received/${userId}`);
        return response.data;
    },

    getGivenFeedback: async (userId: number): Promise<Feedback[]> => {
        const response = await apiClient.get(`/feedback/given/${userId}`);
        return response.data;
    },

    getPendingFeedback: async (): Promise<Feedback[]> => {
        const response = await apiClient.get('/feedback/pending');
        return response.data;
    },

    approveFeedback: async (feedbackId: number): Promise<Feedback> => {
        const response = await apiClient.put(`/feedback/${feedbackId}/approve`);
        return response.data;
    },

    rejectFeedback: async (feedbackId: number): Promise<Feedback> => {
        const response = await apiClient.put(`/feedback/${feedbackId}/reject`);
        return response.data;
    },

    polishFeedback: async (feedbackId: number): Promise<Feedback> => {
        const response = await apiClient.post(`/feedback/${feedbackId}/polish`);
        return response.data;
    },
};

