export interface StudentResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  cin: string;
  studentIdentifier: string;
  email: string;
  phone: string | null;
  fieldOfStudy: string;
  classId: number | null;
  className: string | null;
  levelName: string | null;
  photoUrl: string | null;
  address: string | null;
}
