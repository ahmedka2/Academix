import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { StudentResponse } from '../students/student.model';
import { ClassResponse, MyClassAssignmentResponse, TeacherAssignmentResponse } from './class.model';

@Injectable({ providedIn: 'root' })
export class ClassesService {
  private readonly http = inject(HttpClient);

  private readonly classesSignal = signal<ClassResponse[]>([]);
  readonly classes = this.classesSignal.asReadonly();

  private readonly myClassesSignal = signal<MyClassAssignmentResponse[]>([]);
  readonly myClasses = this.myClassesSignal.asReadonly();

  load(): Observable<ClassResponse[]> {
    return this.http.get<ClassResponse[]>('/api/classes').pipe(
      tap((classes) => this.classesSignal.set(classes))
    );
  }

  loadMine(): Observable<MyClassAssignmentResponse[]> {
    return this.http.get<MyClassAssignmentResponse[]>('/api/classes/mine').pipe(
      tap((assignments) => this.myClassesSignal.set(assignments))
    );
  }

  getRoster(classId: number): Observable<StudentResponse[]> {
    return this.http.get<StudentResponse[]>(`/api/classes/${classId}/students`);
  }

  create(payload: Record<string, unknown>): Observable<ClassResponse> {
    return this.http.post<ClassResponse>('/api/classes', payload).pipe(
      tap((schoolClass) => this.classesSignal.update((classes) => [...classes, schoolClass]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<ClassResponse> {
    return this.http.put<ClassResponse>(`/api/classes/${id}`, payload).pipe(
      tap((updated) => this.classesSignal.update((classes) =>
        classes.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/classes/${id}`).pipe(
      tap(() => this.classesSignal.update((classes) => classes.filter((item) => item.id !== id)))
    );
  }

  assignTeacher(classId: number, teacherId: number, subject: string): Observable<TeacherAssignmentResponse> {
    return this.http.post<TeacherAssignmentResponse>(`/api/classes/${classId}/assignments`, { teacherId, subject });
  }

  unassignTeacher(classId: number, assignmentId: number): Observable<void> {
    return this.http.delete<void>(`/api/classes/${classId}/assignments/${assignmentId}`);
  }
}
