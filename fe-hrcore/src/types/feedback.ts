export const FEEDBACK_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
} as const;

export type FeedbackStatus = typeof FEEDBACK_STATUS[keyof typeof FEEDBACK_STATUS];

export interface NamedUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface Feedback {
  id: number;
  fromUser: NamedUser;
  toUser: NamedUser;
  content: string;
  polishedContent?: string;
  status: FeedbackStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeedbackRequest {
  fromUserId: string;
  toUserId: string;
  content: string;
}

export interface FeedbackFilters {
  fromUserId?: string;
  toUserId?: string;
  status?: FeedbackStatus;
  createdAfter?: string;
  createdBefore?: string;
  contentContains?: string;
  hasPolishedContent?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

