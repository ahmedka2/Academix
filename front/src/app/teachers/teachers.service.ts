import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { TeacherResponse } from './teacher.model';

@Injectable({ providedIn: 'root' })
export class TeachersService {
  private readonly http = inject(HttpClient);

  private readonly teachersSignal = signal<TeacherResponse[]>([]);
  readonly teachers = this.teachersSignal.asReadonly();

  load(): Observable<TeacherResponse[]> {
    return this.http.get<TeacherResponse[]>('/api/teachers').pipe(
      tap((teachers) => this.teachersSignal.set(teachers))
    );
  }

  create(payload: Record<string, unknown>): Observable<TeacherResponse> {
    return this.http.post<TeacherResponse>('/api/teachers', payload).pipe(
      tap((teacher) => this.teachersSignal.update((teachers) => [...teachers, teacher]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<TeacherResponse> {
    return this.http.put<TeacherResponse>(`/api/teachers/${id}`, payload).pipe(
      tap((updated) => this.teachersSignal.update((teachers) =>
        teachers.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/teachers/${id}`).pipe(
      tap(() => this.teachersSignal.update((teachers) => teachers.filter((item) => item.id !== id)))
    );
  }
}
