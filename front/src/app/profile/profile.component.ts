import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AbsenceResponse } from '../absences/absence.model';
import { AbsencesService } from '../absences/absences.service';
import { extractErrorMessage } from '../core/http-error.util';
import { filenameFromContentDisposition, triggerBrowserDownload } from '../core/file-download.util';
import { NotificationService } from '../core/notification.service';
import { DOCUMENT_TYPE_LABELS, DocumentResponse } from '../documents/document.model';
import { DocumentsService } from '../documents/documents.service';
import { GradeResponse, Semester } from '../grades/grade.model';
import { GradesService } from '../grades/grades.service';
import { InvoicesService } from '../invoices/invoices.service';
import { STAGE_DOCUMENT_TYPES, STAGE_DOCUMENT_TYPE_LABELS, StageDocumentResponse, StageDocumentType } from '../stage-documents/stage-document.model';
import { StageDocumentsService } from '../stage-documents/stage-documents.service';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';

const UNJUSTIFIED_THRESHOLD = 3;

export type ProfileTab = 'profile' | 'grades' | 'absences' | 'invoices' | 'documents' | 'stage';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly studentsService = inject(StudentsService);
  private readonly gradesService = inject(GradesService);
  private readonly absencesService = inject(AbsencesService);
  private readonly invoicesService = inject(InvoicesService);
  private readonly documentsService = inject(DocumentsService);
  private readonly stageDocumentsService = inject(StageDocumentsService);
  private readonly notifications = inject(NotificationService);

  readonly grades = this.gradesService.grades;
  readonly absences = this.absencesService.absences;
  readonly invoices = this.invoicesService.invoices;
  readonly documents = this.documentsService.documents;
  readonly documentTypeLabels = DOCUMENT_TYPE_LABELS;
  readonly stageDocuments = this.stageDocumentsService.documents;
  readonly stageDocumentTypes = STAGE_DOCUMENT_TYPES;
  readonly stageDocumentTypeLabels = STAGE_DOCUMENT_TYPE_LABELS;

  readonly activeTab = signal<ProfileTab>('profile');

  profile: StudentResponse | null = null;
  loadingProfile = false;
  loadingGrades = false;
  loadingAbsences = false;
  loadingInvoices = false;
  loadingDocuments = false;
  loadingStageDocuments = false;
  uploadingStageType: StageDocumentType | null = null;
  semesterFilter: 'ALL' | Semester = 'ALL';
  justifyTarget: AbsenceResponse | null = null;
  justificationText = '';
  attemptedJustify = false;
  editingPhone = false;
  phoneDraft = '';
  savingPhone = false;
  uploadingPhoto = false;

  ngOnInit(): void {
    this.loadProfile();
    this.loadGrades();
    this.loadAbsences();
    this.loadInvoices();
    this.loadDocuments();
    this.loadStageDocuments();
  }

  get filteredGrades(): GradeResponse[] {
    return this.semesterFilter === 'ALL'
      ? this.grades()
      : this.grades().filter((grade) => grade.semester === this.semesterFilter);
  }

  get overallAverage(): number {
    const grades = this.grades();
    const totalCoefficient = grades.reduce((sum, grade) => sum + grade.coefficient, 0);
    if (totalCoefficient === 0) return 0;
    const weightedSum = grades.reduce((sum, grade) => sum + grade.score * grade.coefficient, 0);
    return weightedSum / totalCoefficient;
  }

  /** A decorative "library card" bar pattern derived from the student's own
   * identifier — not a real scannable barcode, but drawn from real data
   * rather than pure decoration, so it's the same card every time. */
  get idBarcode(): number[] {
    const id = this.profile?.studentIdentifier ?? '';
    const widths: number[] = [];
    for (let i = 0; i < id.length; i++) {
      widths.push((id.charCodeAt(i) % 3) + 2);
    }
    return widths;
  }

  loadProfile(): void {
    this.loadingProfile = true;
    this.studentsService.getMe().subscribe({
      next: (profile) => this.profile = profile,
      error: (error) => this.handleError(error, 'Could not load your profile.'),
      complete: () => this.loadingProfile = false
    });
  }

  loadGrades(): void {
    this.loadingGrades = true;
    this.gradesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your grades.'),
      complete: () => this.loadingGrades = false
    });
  }

  startEditingPhone(): void {
    this.editingPhone = true;
    this.phoneDraft = this.profile?.phone ?? '';
  }

  cancelEditingPhone(): void {
    this.editingPhone = false;
  }

  savePhone(): void {
    this.savingPhone = true;
    this.studentsService.updateMyPhone(this.phoneDraft.trim()).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.editingPhone = false;
        this.notifications.show('Phone number updated.', 'success');
      },
      error: (error) => this.handleError(error, 'Could not update phone number.'),
      complete: () => this.savingPhone = false
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.notifications.show('Photo must be a JPEG, PNG, or WEBP image.', 'error');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.notifications.show('Photo must be smaller than 2MB.', 'error');
      input.value = '';
      return;
    }

    this.uploadingPhoto = true;
    this.studentsService.uploadMyPhoto(file).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.notifications.show('Profile picture updated.', 'success');
      },
      error: (error) => this.handleError(error, 'Could not upload photo.'),
      complete: () => {
        this.uploadingPhoto = false;
        input.value = '';
      }
    });
  }

  loadInvoices(): void {
    this.loadingInvoices = true;
    this.invoicesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your invoices.'),
      complete: () => this.loadingInvoices = false
    });
  }

  loadDocuments(): void {
    this.loadingDocuments = true;
    this.documentsService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your documents.'),
      complete: () => this.loadingDocuments = false
    });
  }

  downloadDocument(document: DocumentResponse): void {
    this.documentsService.download(document.id).subscribe({
      next: (response) => {
        const fallback = `${document.type.toLowerCase()}-${document.studentIdentifier}.pdf`;
        const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition'), fallback);
        triggerBrowserDownload(response.body as Blob, filename);
      },
      error: (error) => this.handleError(error, 'Could not download document.')
    });
  }

  loadStageDocuments(): void {
    this.loadingStageDocuments = true;
    this.stageDocumentsService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your stage documents.'),
      complete: () => this.loadingStageDocuments = false
    });
  }

  stageDocumentFor(type: StageDocumentType): StageDocumentResponse | undefined {
    return this.stageDocuments().find((document) => document.type === type);
  }

  onStageDocumentSelected(type: StageDocumentType, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)) {
      this.notifications.show('File must be a PDF, JPEG, or PNG.', 'error');
      input.value = '';
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.notifications.show('File must be smaller than 10MB.', 'error');
      input.value = '';
      return;
    }

    this.uploadingStageType = type;
    this.stageDocumentsService.upload(type, file).subscribe({
      next: () => this.notifications.show(`${this.stageDocumentTypeLabels[type]} uploaded for review.`, 'success'),
      error: (error) => this.handleError(error, 'Could not upload document.'),
      complete: () => {
        this.uploadingStageType = null;
        input.value = '';
      }
    });
  }

  downloadStageDocument(document: StageDocumentResponse): void {
    this.stageDocumentsService.download(document.id).subscribe({
      next: (response) => {
        const fallback = `${document.type.toLowerCase()}-${document.studentIdentifier}`;
        const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition'), fallback);
        triggerBrowserDownload(response.body as Blob, filename);
      },
      error: (error) => this.handleError(error, 'Could not download document.')
    });
  }

  get outstandingAmount(): number {
    return this.invoices()
      .filter((invoice) => invoice.status === 'UNPAID')
      .reduce((sum, invoice) => sum + invoice.amount, 0);
  }

  get unjustifiedCount(): number {
    return this.absences().filter((absence) => absence.status === 'UNJUSTIFIED').length;
  }

  get overThreshold(): boolean {
    return this.unjustifiedCount >= UNJUSTIFIED_THRESHOLD;
  }

  canJustify(absence: AbsenceResponse): boolean {
    return absence.status === 'UNJUSTIFIED' || absence.status === 'REJECTED';
  }

  loadAbsences(): void {
    this.loadingAbsences = true;
    this.absencesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your absences.'),
      complete: () => this.loadingAbsences = false
    });
  }

  startJustifying(absence: AbsenceResponse): void {
    this.justifyTarget = absence;
    this.justificationText = absence.justification ?? '';
    this.attemptedJustify = false;
  }

  cancelJustifying(): void {
    this.justifyTarget = null;
    this.justificationText = '';
    this.attemptedJustify = false;
  }

  submitJustification(): void {
    this.attemptedJustify = true;
    const target = this.justifyTarget;
    if (!target || !this.justificationText.trim()) {
      this.notifications.show('Enter a justification before submitting.', 'error');
      return;
    }
    this.absencesService.justify(target.id, this.justificationText.trim()).subscribe({
      next: () => {
        this.notifications.show('Justification submitted for review.', 'success');
        this.cancelJustifying();
      },
      error: (error) => this.handleError(error, 'Could not submit justification.')
    });
  }

  private handleError(error: unknown, fallback: string): void {
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
