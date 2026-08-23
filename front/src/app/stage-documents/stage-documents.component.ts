import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { extractErrorMessage } from '../core/http-error.util';
import { filenameFromContentDisposition, triggerBrowserDownload } from '../core/file-download.util';
import { NotificationService } from '../core/notification.service';
import { STAGE_DOCUMENT_TYPE_LABELS, StageDocumentResponse, StageDocumentStatus } from './stage-document.model';
import { StageDocumentsService } from './stage-documents.service';

@Component({
  selector: 'app-stage-documents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stage-documents.component.html',
  styleUrl: './stage-documents.component.css'
})
export class StageDocumentsComponent implements OnInit {
  private readonly stageDocumentsService = inject(StageDocumentsService);
  private readonly notifications = inject(NotificationService);

  readonly documents = this.stageDocumentsService.documents;
  readonly typeLabels = STAGE_DOCUMENT_TYPE_LABELS;

  loading = false;
  statusFilter: 'ALL' | StageDocumentStatus = 'ALL';
  searchQuery = '';
  rejectTarget: StageDocumentResponse | null = null;
  rejectComment = '';
  documentPendingDeletion: StageDocumentResponse | null = null;

  ngOnInit(): void {
    this.loadDocuments();
  }

  get filteredDocuments(): StageDocumentResponse[] {
    const query = this.searchQuery.trim().toLowerCase();
    return this.documents().filter((document) => {
      const matchesStatus = this.statusFilter === 'ALL' || document.status === this.statusFilter;
      const matchesQuery = !query || `${document.studentName} ${document.studentIdentifier}`.toLowerCase().includes(query);
      return matchesStatus && matchesQuery;
    });
  }

  get pendingCount(): number {
    return this.documents().filter((document) => document.status === 'PENDING').length;
  }

  get approvedCount(): number {
    return this.documents().filter((document) => document.status === 'APPROVED').length;
  }

  get rejectedCount(): number {
    return this.documents().filter((document) => document.status === 'REJECTED').length;
  }

  loadDocuments(): void {
    this.loading = true;
    this.stageDocumentsService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load stage documents.'),
      complete: () => this.loading = false
    });
  }

  approve(document: StageDocumentResponse): void {
    this.stageDocumentsService.validate(document.id, true, '').subscribe({
      next: () => this.notifications.show(`Approved ${this.typeLabels[document.type]} for ${document.studentName}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not approve document.')
    });
  }

  startReject(document: StageDocumentResponse): void {
    this.rejectTarget = document;
    this.rejectComment = '';
  }

  cancelReject(): void {
    this.rejectTarget = null;
    this.rejectComment = '';
  }

  confirmReject(): void {
    const document = this.rejectTarget;
    if (!document) return;
    this.stageDocumentsService.validate(document.id, false, this.rejectComment.trim()).subscribe({
      next: () => {
        this.notifications.show(`Rejected ${this.typeLabels[document.type]} for ${document.studentName}.`, 'success');
        this.cancelReject();
      },
      error: (error) => this.handleError(error, 'Could not reject document.')
    });
  }

  downloadDocument(document: StageDocumentResponse): void {
    this.stageDocumentsService.download(document.id).subscribe({
      next: (response) => {
        const fallback = `${document.type.toLowerCase()}-${document.studentIdentifier}`;
        const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition'), fallback);
        triggerBrowserDownload(response.body as Blob, filename);
      },
      error: (error) => this.handleError(error, 'Could not download document.')
    });
  }

  requestDocumentDelete(document: StageDocumentResponse): void {
    this.documentPendingDeletion = document;
  }

  cancelDocumentDelete(): void {
    this.documentPendingDeletion = null;
  }

  deleteDocument(): void {
    const document = this.documentPendingDeletion;
    if (!document) return;
    this.loading = true;
    this.stageDocumentsService.delete(document.id).subscribe({
      next: () => {
        this.documentPendingDeletion = null;
        this.notifications.show(`Deleted ${this.typeLabels[document.type]} for ${document.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete document.'),
      complete: () => this.loading = false
    });
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
