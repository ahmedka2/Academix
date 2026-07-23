import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { ClassesService } from '../classes/classes.service';
import { AuthService } from '../core/auth.service';
import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from './student.model';
import { StudentsService } from './students.service';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './students.component.html',
  styleUrl: './students.component.css'
})
export class StudentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly studentsService = inject(StudentsService);
  private readonly classesService = inject(ClassesService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly students = this.studentsService.students;
  readonly classes = this.classesService.classes;
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMINISTRATION');

  query = '';
  classFilter = 0;
  loading = false;
  loadingStudents = false;
  passwordVisible = false;
  editingStudent: StudentResponse | null = null;
  studentPendingDeletion: StudentResponse | null = null;

  studentForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    cin: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    fieldOfStudy: ['', [Validators.required]],
    classId: [0]
  });

  editForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    cin: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    fieldOfStudy: ['', [Validators.required]],
    classId: [0],
    photoUrl: [''],
    address: ['']
  });

  ngOnInit(): void {
    this.loadStudents();
    this.classesService.load().subscribe();
  }

  get studentCount(): number {
    return this.students().length;
  }

  get filteredStudents(): StudentResponse[] {
    return this.classFilter
      ? this.students().filter((student) => student.classId === this.classFilter)
      : this.students();
  }

  createStudent(): void {
    if (this.studentForm.invalid) {
      this.notifications.show('Fill all required student fields.', 'error');
      return;
    }

    this.loading = true;
    const { classId, ...rest } = this.studentForm.getRawValue();
    this.studentsService.create({ ...rest, classId: classId || null }).subscribe({
      next: (student) => {
        this.notifications.show(`Created student ${student.firstName} ${student.lastName}.`, 'success');
        this.studentForm.reset({ classId: 0 });
      },
      error: (error) => this.handleError(error, 'Could not create student.'),
      complete: () => this.loading = false
    });
  }

  loadStudents(): void {
    this.loadingStudents = true;
    this.studentsService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load students.'),
      complete: () => this.loadingStudents = false
    });
  }

  searchStudents(): void {
    this.loadingStudents = true;
    this.studentsService.search(this.query).subscribe({
      error: (error) => this.handleError(error, 'Search failed.'),
      complete: () => this.loadingStudents = false
    });
  }

  trackStudent(_: number, student: StudentResponse): number {
    return student.id;
  }

  startEditing(student: StudentResponse): void {
    this.editingStudent = student;
    this.editForm.reset({
      firstName: student.firstName,
      lastName: student.lastName,
      cin: student.cin,
      email: student.email,
      phone: student.phone ?? '',
      fieldOfStudy: student.fieldOfStudy,
      classId: student.classId ?? 0,
      photoUrl: student.photoUrl ?? '',
      address: student.address ?? ''
    });
  }

  cancelEditing(): void {
    this.editingStudent = null;
  }

  updateStudent(): void {
    if (!this.editingStudent || this.editForm.invalid) {
      this.notifications.show('Fill all required student fields.', 'error');
      return;
    }

    const student = this.editingStudent;
    const { classId, ...rest } = this.editForm.getRawValue();
    this.loading = true;
    this.studentsService.update(student.id, {
      ...rest,
      classId: classId || null,
      studentIdentifier: student.studentIdentifier
    }).subscribe({
      next: (updatedStudent) => {
        this.notifications.show(`Updated ${updatedStudent.firstName} ${updatedStudent.lastName}.`, 'success');
        this.editingStudent = null;
      },
      error: (error) => this.handleError(error, 'Could not update student.'),
      complete: () => this.loading = false
    });
  }

  requestDelete(student: StudentResponse): void {
    this.studentPendingDeletion = student;
  }

  cancelDelete(): void {
    this.studentPendingDeletion = null;
  }

  deleteStudent(): void {
    const student = this.studentPendingDeletion;
    if (!student) {
      return;
    }

    this.loading = true;
    this.studentsService.delete(student.id).subscribe({
      next: () => {
        this.notifications.show(`Deleted ${student.firstName} ${student.lastName}.`, 'success');
        this.studentPendingDeletion = null;
      },
      error: (error) => this.handleError(error, 'Could not delete student.'),
      complete: () => this.loading = false
    });
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
