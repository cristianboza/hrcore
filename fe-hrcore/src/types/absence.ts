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

export interface NamedUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface AbsenceRequest {
  id: number;
  user: NamedUser;
  startDate: string;
  endDate: string;
  reason?: string;
  type: AbsenceType;
  status: AbsenceStatus;
  approver?: NamedUser;
  rejectionReason?: string;
  createdBy: NamedUser;
  createdAt: string;
  updatedAt?: string;
  canApprove?: boolean; // Whether current user can approve/reject this request
}


