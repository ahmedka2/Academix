export type Semester = 'S1' | 'S2';

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
}
