import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { extractErrorMessage } from '../core/http-error.util';
import { filenameFromContentDisposition, triggerBrowserDownload } from '../core/file-download.util';
import { NotificationService } from '../core/notification.service';
import { Semester } from '../grades/grade.model';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';
import { DOCUMENT_TYPE_LABELS, DocumentResponse, DocumentType } from './document.model';
import { DocumentsService } from './documents.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.css'
})
export class DocumentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly documentsService = inject(DocumentsService);
  private readonly studentsService = inject(StudentsService);
  private readonly notifications = inject(NotificationService);

  readonly documents = this.documentsService.documents;
  readonly typeLabels = DOCUMENT_TYPE_LABELS;

  loading = false;
  generating = false;
  documentStudentQuery = '';
  documentPendingDeletion: DocumentResponse | null = null;

  documentForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    type: ['ATTESTATION_SCOLARITE' as DocumentType],
    academicYear: [this.defaultAcademicYear(), [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]],
    semester: ['' as '' | Semester]
  });

  ngOnInit(): void {
    this.studentsService.load().subscribe();
    this.loadDocuments();
  }

  get documentStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.documentStudentQuery);
  }

  get isReleveNotes(): boolean {
    return this.documentForm.getRawValue().type === 'RELEVE_NOTES';
  }

  get enrollmentCount(): number {
    return this.documents().filter((document) => document.type !== 'RELEVE_NOTES').length;
  }

  get transcriptCount(): number {
    return this.documents().filter((document) => document.type === 'RELEVE_NOTES').length;
  }

  loadDocuments(): void {
    this.loading = true;
    this.documentsService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load documents.'),
      complete: () => this.loading = false
    });
  }

  selectDocumentStudent(student: StudentResponse): void {
    this.documentForm.patchValue({ studentId: student.id });
    this.documentStudentQuery = `${student.firstName} ${student.lastName} · ${student.studentIdentifier}`;
  }

  generateDocument(): void {
    if (this.documentForm.invalid) {
      this.notifications.show('Select a student and a valid academic year.', 'error');
      return;
    }
    const value = this.documentForm.getRawValue();
    this.generating = true;
    this.documentsService.generate({
      ...value,
      semester: value.type === 'RELEVE_NOTES' ? (value.semester || null) : null
    }).subscribe({
      next: (document) => {
        this.notifications.show(`Generated ${this.typeLabels[document.type]} for ${document.studentName}.`, 'success');
        this.documentForm.reset({ studentId: 0, type: 'ATTESTATION_SCOLARITE', academicYear: this.defaultAcademicYear(), semester: '' });
        this.documentStudentQuery = '';
      },
      error: (error) => this.handleError(error, 'Could not generate document.'),
      complete: () => this.generating = false
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

  requestDocumentDelete(document: DocumentResponse): void {
    this.documentPendingDeletion = document;
  }

  cancelDocumentDelete(): void {
    this.documentPendingDeletion = null;
  }

  deleteDocument(): void {
    const document = this.documentPendingDeletion;
    if (!document) return;
    this.loading = true;
    this.documentsService.delete(document.id).subscribe({
      next: () => {
        this.documentPendingDeletion = null;
        this.notifications.show(`Deleted ${this.typeLabels[document.type]} for ${document.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete document.'),
      complete: () => this.loading = false
    });
  }

  private defaultAcademicYear(): string {
    const now = new Date();
    const startYear = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1;
    return `${startYear}-${startYear + 1}`;
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    this.generating = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
