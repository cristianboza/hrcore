export const ABSENCE_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
} as const;

export const ABSENCE_TYPE = {
  VACATION: 'VACATION',
  SICK: 'SICK',
  OTHER: 'OTHER',
} as const;

export type AbsenceStatus = typeof ABSENCE_STATUS[keyof typeof ABSENCE_STATUS];
export type AbsenceType = typeof ABSENCE_TYPE[keyof typeof ABSENCE_TYPE];

export interface AbsenceRequest {
  id: number;
  userId: number;
  startDate: string;
  endDate: string;
  reason?: string;
  type: AbsenceType;
  status: AbsenceStatus;
  approverId?: number;
  rejectionReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

