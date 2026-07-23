import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { StudentResponse } from './student.model';

@Injectable({ providedIn: 'root' })
export class StudentsService {
  private readonly http = inject(HttpClient);

  private readonly studentsSignal = signal<StudentResponse[]>([]);
  readonly students = this.studentsSignal.asReadonly();

  load(): Observable<StudentResponse[]> {
    return this.http.get<StudentResponse[]>('/api/students').pipe(
      tap((students) => this.studentsSignal.set(students))
    );
  }

  getMe(): Observable<StudentResponse> {
    return this.http.get<StudentResponse>('/api/students/me');
  }

  updateMyPhone(phone: string): Observable<StudentResponse> {
    return this.http.put<StudentResponse>('/api/students/me', { phone });
  }

  uploadMyPhoto(file: File): Observable<StudentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<StudentResponse>('/api/students/me/photo', formData);
  }

  search(query: string): Observable<StudentResponse[]> {
    const url = query.trim()
      ? `/api/students/search?query=${encodeURIComponent(query.trim())}`
      : '/api/students';
    return this.http.get<StudentResponse[]>(url).pipe(
      tap((students) => this.studentsSignal.set(students))
    );
  }

  create(payload: Record<string, unknown>): Observable<StudentResponse> {
    return this.http.post<StudentResponse>('/api/students', payload).pipe(
      tap((student) => this.studentsSignal.update((students) => [...students, student]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<StudentResponse> {
    return this.http.put<StudentResponse>(`/api/students/${id}`, payload).pipe(
      tap((updated) => this.studentsSignal.update((students) =>
        students.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/students/${id}`).pipe(
      tap(() => this.studentsSignal.update((students) => students.filter((item) => item.id !== id)))
    );
  }

  suggestionsFor(query: string): StudentResponse[] {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return [];
    return this.studentsSignal().filter((student) =>
      `${student.firstName} ${student.lastName} ${student.studentIdentifier}`.toLowerCase().includes(normalizedQuery)
    ).slice(0, 6);
  }
}
