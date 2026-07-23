export interface ClassAverageResponse {
  classId: number;
  className: string;
  levelName: string;
  studentCount: number;
  average: number;
}

export interface CategoryCountResponse {
  label: string;
  count: number;
}

export interface AbsenceStatsResponse {
  total: number;
  unjustified: number;
  pendingJustification: number;
  justified: number;
  rejected: number;
  unjustifiedRate: number;
}

export interface PaymentStatsResponse {
  totalInvoices: number;
  paidCount: number;
  unpaidCount: number;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
}

export interface StatisticsResponse {
  totalStudents: number;
  totalTeachers: number;
  totalClasses: number;
  overallPassRate: number;
  averageByClass: ClassAverageResponse[];
  studentsByFieldOfStudy: CategoryCountResponse[];
  studentsByLevel: CategoryCountResponse[];
  absences: AbsenceStatsResponse;
  payments: PaymentStatsResponse;
}
