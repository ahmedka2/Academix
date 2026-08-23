export type StageDocumentType = 'CONVENTION' | 'RAPPORT' | 'ATTESTATION' | 'FICHE_ENCADREMENT';
export type StageDocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface StageDocumentResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentIdentifier: string;
  type: StageDocumentType;
  status: StageDocumentStatus;
  comment: string | null;
  uploadedAt: string;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

export const STAGE_DOCUMENT_TYPES: StageDocumentType[] = ['CONVENTION', 'RAPPORT', 'ATTESTATION', 'FICHE_ENCADREMENT'];

export const STAGE_DOCUMENT_TYPE_LABELS: Record<StageDocumentType, string> = {
  CONVENTION: 'Convention de stage',
  RAPPORT: 'Rapport de stage',
  ATTESTATION: 'Attestation de stage',
  FICHE_ENCADREMENT: "Fiche d'encadrement"
};
