export const FEEDBACK_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
} as const;

export type FeedbackStatus = typeof FEEDBACK_STATUS[keyof typeof FEEDBACK_STATUS];

export interface Feedback {
  id: number;
  fromUserId: number;
  toUserId: number;
  content: string;
  polishedContent?: string;
  status: FeedbackStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateFeedbackRequest {
  fromUserId: number;
  toUserId: number;
  content: string;
}

