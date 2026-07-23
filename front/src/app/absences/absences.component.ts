import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../core/auth.service';
import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';
import { AbsenceResponse, AbsenceStatus } from './absence.model';
import { AbsencesService } from './absences.service';

const UNJUSTIFIED_THRESHOLD = 3;

@Component({
  selector: 'app-absences',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './absences.component.html',
  styleUrl: './absences.component.css'
})
export class AbsencesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly absencesService = inject(AbsencesService);
  private readonly studentsService = inject(StudentsService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly absences = this.absencesService.absences;
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMINISTRATION');

  loading = false;
  statusFilter: 'ALL' | AbsenceStatus = 'ALL';
  editingAbsence: AbsenceResponse | null = null;
  absencePendingDeletion: AbsenceResponse | null = null;
  absenceStudentQuery = '';
  editAbsenceStudentQuery = '';

  absenceForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    subject: ['', [Validators.required]],
    date: [this.today(), [Validators.required]],
    comment: ['']
  });

  editAbsenceForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    subject: ['', [Validators.required]],
    date: ['', [Validators.required]],
    comment: ['']
  });

  ngOnInit(): void {
    this.studentsService.load().subscribe();
    this.loadAbsences();
  }

  get filteredAbsences(): AbsenceResponse[] {
    return this.statusFilter === 'ALL'
      ? this.absences()
      : this.absences().filter((absence) => absence.status === this.statusFilter);
  }

  get totalAbsences(): number {
    return this.absences().length;
  }

  get unjustifiedCount(): number {
    return this.absences().filter((absence) => absence.status === 'UNJUSTIFIED').length;
  }

  get pendingCount(): number {
    return this.absences().filter((absence) => absence.status === 'PENDING_JUSTIFICATION').length;
  }

  unjustifiedCountFor(studentId: number): number {
    return this.absences().filter((absence) => absence.studentId === studentId && absence.status === 'UNJUSTIFIED').length;
  }

  overThreshold(studentId: number): boolean {
    return this.unjustifiedCountFor(studentId) >= UNJUSTIFIED_THRESHOLD;
  }

  get absenceStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.absenceStudentQuery);
  }

  get editAbsenceStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.editAbsenceStudentQuery);
  }

  createAbsence(): void {
    if (this.absenceForm.invalid) {
      this.notifications.show('Select a student and enter a valid absence.', 'error');
      return;
    }
    this.loading = true;
    this.absencesService.create(this.absenceForm.getRawValue()).subscribe({
      next: (absence) => {
        this.notifications.show(`Declared absence for ${absence.studentName}.`, 'success');
        this.absenceForm.reset({ studentId: 0, subject: '', date: this.today(), comment: '' });
        this.absenceStudentQuery = '';
      },
      error: (error) => this.handleError(error, 'Could not declare absence.'),
      complete: () => this.loading = false
    });
  }

  loadAbsences(): void {
    this.absencesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load absences.')
    });
  }

  selectAbsenceStudent(student: StudentResponse, editing = false): void {
    const label = `${student.firstName} ${student.lastName} · ${student.studentIdentifier}`;
    if (editing) {
      this.editAbsenceForm.patchValue({ studentId: student.id });
      this.editAbsenceStudentQuery = label;
      return;
    }
    this.absenceForm.patchValue({ studentId: student.id });
    this.absenceStudentQuery = label;
  }

  startEditing(absence: AbsenceResponse): void {
    this.editingAbsence = absence;
    this.editAbsenceForm.reset({
      studentId: absence.studentId,
      subject: absence.subject,
      date: absence.date,
      comment: absence.comment ?? ''
    });
    this.editAbsenceStudentQuery = `${absence.studentName} · ${absence.studentIdentifier}`;
  }

  cancelEditing(): void {
    this.editingAbsence = null;
  }

  updateAbsence(): void {
    if (!this.editingAbsence || this.editAbsenceForm.invalid) {
      this.notifications.show('Complete the required absence details.', 'error');
      return;
    }
    this.loading = true;
    this.absencesService.update(this.editingAbsence.id, this.editAbsenceForm.getRawValue()).subscribe({
      next: (absence) => {
        this.editingAbsence = null;
        this.notifications.show(`Updated absence for ${absence.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not update absence.'),
      complete: () => this.loading = false
    });
  }

  requestDelete(absence: AbsenceResponse): void {
    this.absencePendingDeletion = absence;
  }

  cancelDelete(): void {
    this.absencePendingDeletion = null;
  }

  deleteAbsence(): void {
    const absence = this.absencePendingDeletion;
    if (!absence) return;
    this.loading = true;
    this.absencesService.delete(absence.id).subscribe({
      next: () => {
        this.absencePendingDeletion = null;
        this.notifications.show(`Deleted absence for ${absence.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete absence.'),
      complete: () => this.loading = false
    });
  }

  approve(absence: AbsenceResponse): void {
    this.absencesService.validate(absence.id, true).subscribe({
      next: () => this.notifications.show(`Justification approved for ${absence.studentName}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not approve justification.')
    });
  }

  reject(absence: AbsenceResponse): void {
    this.absencesService.validate(absence.id, false).subscribe({
      next: () => this.notifications.show(`Justification rejected for ${absence.studentName}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not reject justification.')
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
