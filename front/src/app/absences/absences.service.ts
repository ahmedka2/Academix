import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { AbsenceResponse } from './absence.model';

@Injectable({ providedIn: 'root' })
export class AbsencesService {
  private readonly http = inject(HttpClient);

  private readonly absencesSignal = signal<AbsenceResponse[]>([]);
  readonly absences = this.absencesSignal.asReadonly();

  load(): Observable<AbsenceResponse[]> {
    return this.http.get<AbsenceResponse[]>('/api/absences').pipe(
      tap((absences) => this.absencesSignal.set(absences))
    );
  }

  loadMine(): Observable<AbsenceResponse[]> {
    return this.http.get<AbsenceResponse[]>('/api/absences/me').pipe(
      tap((absences) => this.absencesSignal.set(absences))
    );
  }

  create(payload: Record<string, unknown>): Observable<AbsenceResponse> {
    return this.http.post<AbsenceResponse>('/api/absences', payload).pipe(
      tap((absence) => this.absencesSignal.update((absences) => [absence, ...absences]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<AbsenceResponse> {
    return this.http.put<AbsenceResponse>(`/api/absences/${id}`, payload).pipe(
      tap((updated) => this.absencesSignal.update((absences) =>
        absences.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/absences/${id}`).pipe(
      tap(() => this.absencesSignal.update((absences) => absences.filter((item) => item.id !== id)))
    );
  }

  justify(id: number, justification: string): Observable<AbsenceResponse> {
    return this.http.put<AbsenceResponse>(`/api/absences/${id}/justify`, { justification }).pipe(
      tap((updated) => this.absencesSignal.update((absences) =>
        absences.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  validate(id: number, approved: boolean): Observable<AbsenceResponse> {
    return this.http.put<AbsenceResponse>(`/api/absences/${id}/validate`, { approved }).pipe(
      tap((updated) => this.absencesSignal.update((absences) =>
        absences.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }
}
