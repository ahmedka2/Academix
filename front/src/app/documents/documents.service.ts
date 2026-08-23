import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { DocumentResponse } from './document.model';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);

  private readonly documentsSignal = signal<DocumentResponse[]>([]);
  readonly documents = this.documentsSignal.asReadonly();

  load(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>('/api/documents').pipe(
      tap((documents) => this.documentsSignal.set(documents))
    );
  }

  loadMine(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>('/api/documents/me').pipe(
      tap((documents) => this.documentsSignal.set(documents))
    );
  }

  generate(payload: Record<string, unknown>): Observable<DocumentResponse> {
    return this.http.post<DocumentResponse>('/api/documents/generate', payload).pipe(
      tap((document) => this.documentsSignal.update((documents) => [document, ...documents]))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`).pipe(
      tap(() => this.documentsSignal.update((documents) => documents.filter((item) => item.id !== id)))
    );
  }

  download(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/documents/${id}/download`, { observe: 'response', responseType: 'blob' });
  }
}
