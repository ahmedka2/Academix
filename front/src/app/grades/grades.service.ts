import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { GradeResponse } from './grade.model';

@Injectable({ providedIn: 'root' })
export class GradesService {
  private readonly http = inject(HttpClient);

  private readonly gradesSignal = signal<GradeResponse[]>([]);
  readonly grades = this.gradesSignal.asReadonly();

  load(): Observable<GradeResponse[]> {
    return this.http.get<GradeResponse[]>('/api/grades').pipe(
      tap((grades) => this.gradesSignal.set(grades))
    );
  }

  loadMine(): Observable<GradeResponse[]> {
    return this.http.get<GradeResponse[]>('/api/grades/me').pipe(
      tap((grades) => this.gradesSignal.set(grades))
    );
  }

  createBulk(payload: Record<string, unknown>): Observable<GradeResponse[]> {
    return this.http.post<GradeResponse[]>('/api/grades/bulk', payload).pipe(
      tap((created) => this.gradesSignal.update((grades) => [...created, ...grades]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<GradeResponse> {
    return this.http.put<GradeResponse>(`/api/grades/${id}`, payload).pipe(
      tap((updated) => this.gradesSignal.update((grades) =>
        grades.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/grades/${id}`).pipe(
      tap(() => this.gradesSignal.update((grades) => grades.filter((item) => item.id !== id)))
    );
  }

  validate(id: number, approved: boolean, comment: string): Observable<GradeResponse> {
    return this.http.put<GradeResponse>(`/api/grades/${id}/validate`, { approved, comment: comment || null }).pipe(
      tap((updated) => this.gradesSignal.update((grades) =>
        grades.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }
}
