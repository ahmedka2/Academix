export type Semester = 'S1' | 'S2';
export type GradeStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface GradeResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentIdentifier: string;
  subject: string;
  score: number;
  coefficient: number;
  semester: Semester;
  academicYear: string;
  status: GradeStatus;
  submittedBy: string | null;
  reviewComment: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

export interface BulkGradeEntry {
  studentId: number;
  score: number | null;
}
