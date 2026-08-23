import { Semester } from '../grades/grade.model';

export type DocumentType = 'ATTESTATION_SCOLARITE' | 'CERTIFICAT_INSCRIPTION' | 'RELEVE_NOTES';

export interface DocumentResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentIdentifier: string;
  type: DocumentType;
  academicYear: string;
  semester: Semester | null;
  generatedAt: string;
  generatedBy: string;
}

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  ATTESTATION_SCOLARITE: 'Attestation de scolarité',
  CERTIFICAT_INSCRIPTION: "Certificat d'inscription",
  RELEVE_NOTES: 'Relevé de notes / Bulletin'
};
