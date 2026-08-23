import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { StageDocumentResponse, StageDocumentType } from './stage-document.model';

@Injectable({ providedIn: 'root' })
export class StageDocumentsService {
  private readonly http = inject(HttpClient);

  private readonly documentsSignal = signal<StageDocumentResponse[]>([]);
  readonly documents = this.documentsSignal.asReadonly();

  load(studentId?: number, status?: string): Observable<StageDocumentResponse[]> {
    const params: string[] = [];
    if (studentId) params.push(`studentId=${studentId}`);
    if (status) params.push(`status=${status}`);
    const query = params.length ? `?${params.join('&')}` : '';
    return this.http.get<StageDocumentResponse[]>(`/api/stage-documents${query}`).pipe(
      tap((documents) => this.documentsSignal.set(documents))
    );
  }

  loadMine(): Observable<StageDocumentResponse[]> {
    return this.http.get<StageDocumentResponse[]>('/api/stage-documents/me').pipe(
      tap((documents) => this.documentsSignal.set(documents))
    );
  }

  upload(type: StageDocumentType, file: File): Observable<StageDocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<StageDocumentResponse>(`/api/stage-documents/me/${type}`, formData).pipe(
      tap((document) => this.documentsSignal.update((documents) => [
        document,
        ...documents.filter((item) => item.id !== document.id)
      ]))
    );
  }

  validate(id: number, approved: boolean, comment: string): Observable<StageDocumentResponse> {
    return this.http.put<StageDocumentResponse>(`/api/stage-documents/${id}/validate`, { approved, comment: comment || null }).pipe(
      tap((updated) => this.documentsSignal.update((documents) =>
        documents.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/stage-documents/${id}`).pipe(
      tap(() => this.documentsSignal.update((documents) => documents.filter((item) => item.id !== id)))
    );
  }

  download(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/stage-documents/${id}/download`, { observe: 'response', responseType: 'blob' });
  }
}
