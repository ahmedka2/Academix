import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { TeacherResponse } from './teacher.model';
import { TeachersService } from './teachers.service';

@Component({
  selector: 'app-teachers',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './teachers.component.html',
  styleUrl: './teachers.component.css'
})
export class TeachersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly teachersService = inject(TeachersService);
  private readonly notifications = inject(NotificationService);

  readonly teachers = this.teachersService.teachers;

  query = '';
  loading = false;
  loadingTeachers = false;
  passwordVisible = false;
  editingTeacher: TeacherResponse | null = null;
  teacherPendingDeletion: TeacherResponse | null = null;

  teacherForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  editForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnInit(): void {
    this.loadTeachers();
  }

  get teacherCount(): number {
    return this.teachers().length;
  }

  get filteredTeachers(): TeacherResponse[] {
    const normalizedQuery = this.query.trim().toLowerCase();
    if (!normalizedQuery) return this.teachers();
    return this.teachers().filter((teacher) =>
      `${teacher.fullName} ${teacher.email}`.toLowerCase().includes(normalizedQuery)
    );
  }

  createTeacher(): void {
    if (this.teacherForm.invalid) {
      this.notifications.show('Fill all required teacher fields.', 'error');
      return;
    }

    this.loading = true;
    this.teachersService.create(this.teacherForm.getRawValue()).subscribe({
      next: (teacher) => {
        this.notifications.show(`Created teacher ${teacher.fullName}.`, 'success');
        this.teacherForm.reset();
      },
      error: (error) => this.handleError(error, 'Could not create teacher.'),
      complete: () => this.loading = false
    });
  }

  loadTeachers(): void {
    this.loadingTeachers = true;
    this.teachersService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load teachers.'),
      complete: () => this.loadingTeachers = false
    });
  }

  trackTeacher(_: number, teacher: TeacherResponse): number {
    return teacher.id;
  }

  startEditing(teacher: TeacherResponse): void {
    this.editingTeacher = teacher;
    this.editForm.reset({
      fullName: teacher.fullName,
      email: teacher.email
    });
  }

  cancelEditing(): void {
    this.editingTeacher = null;
  }

  updateTeacher(): void {
    if (!this.editingTeacher || this.editForm.invalid) {
      this.notifications.show('Fill all required teacher fields.', 'error');
      return;
    }

    const teacher = this.editingTeacher;
    this.loading = true;
    this.teachersService.update(teacher.id, this.editForm.getRawValue()).subscribe({
      next: (updatedTeacher) => {
        this.notifications.show(`Updated ${updatedTeacher.fullName}.`, 'success');
        this.editingTeacher = null;
      },
      error: (error) => this.handleError(error, 'Could not update teacher.'),
      complete: () => this.loading = false
    });
  }

  requestDelete(teacher: TeacherResponse): void {
    this.teacherPendingDeletion = teacher;
  }

  cancelDelete(): void {
    this.teacherPendingDeletion = null;
  }

  deleteTeacher(): void {
    const teacher = this.teacherPendingDeletion;
    if (!teacher) {
      return;
    }

    this.loading = true;
    this.teachersService.delete(teacher.id).subscribe({
      next: () => {
        this.notifications.show(`Deleted ${teacher.fullName}.`, 'success');
        this.teacherPendingDeletion = null;
      },
      error: (error) => this.handleError(error, 'Could not delete teacher.'),
      complete: () => this.loading = false
    });
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
