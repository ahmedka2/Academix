export type AbsenceStatus = 'UNJUSTIFIED' | 'PENDING_JUSTIFICATION' | 'JUSTIFIED' | 'REJECTED';

export interface AbsenceResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentIdentifier: string;
  subject: string;
  date: string;
  comment: string | null;
  justification: string | null;
  status: AbsenceStatus;
  createdAt: string;
  updatedAt: string;
}
