export interface TeacherAssignmentResponse {
  id: number;
  teacherId: number;
  teacherName: string;
  subject: string;
}

export interface ClassResponse {
  id: number;
  name: string;
  levelId: number;
  levelName: string;
  studentCount: number;
  capacity: number;
  teacherAssignments: TeacherAssignmentResponse[];
}

export interface MyClassAssignmentResponse {
  assignmentId: number;
  classId: number;
  className: string;
  levelName: string;
  subject: string;
  studentCount: number;
}
