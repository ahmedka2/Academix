import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { MyClassAssignmentResponse } from '../classes/class.model';
import { ClassesService } from '../classes/classes.service';
import { AuthService } from '../core/auth.service';
import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
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
  private readonly classesService = inject(ClassesService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly absences = this.absencesService.absences;
  readonly myClasses = this.classesService.myClasses;
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMINISTRATION');
  readonly isTeacher = computed(() => this.authService.user()?.role === 'TEACHER');

  loading = false;
  submitting = false;
  statusFilter: 'ALL' | AbsenceStatus = 'ALL';
  editingAbsence: AbsenceResponse | null = null;
  absencePendingDeletion: AbsenceResponse | null = null;

  selectedAssignment: MyClassAssignmentResponse | null = null;
  roster: StudentResponse[] = [];
  loadingRoster = false;
  absentDrafts: Record<number, boolean> = {};

  rollCallForm = this.fb.nonNullable.group({
    date: [this.today(), [Validators.required]]
  });

  editAbsenceForm = this.fb.nonNullable.group({
    subject: ['', [Validators.required]],
    date: ['', [Validators.required]],
    comment: ['']
  });

  ngOnInit(): void {
    if (this.isTeacher()) {
      this.classesService.loadMine().subscribe();
    }
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

  loadAbsences(): void {
    this.absencesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load absences.')
    });
  }

  selectAssignment(assignment: MyClassAssignmentResponse): void {
    this.selectedAssignment = assignment;
    this.absentDrafts = {};
    this.loadingRoster = true;
    this.classesService.getRoster(assignment.classId).subscribe({
      next: (roster) => this.roster = roster,
      error: (error) => this.handleError(error, 'Could not load the class roster.'),
      complete: () => this.loadingRoster = false
    });
  }

  clearAssignment(): void {
    this.selectedAssignment = null;
    this.roster = [];
    this.absentDrafts = {};
  }

  toggleAbsent(studentId: number, absent: boolean): void {
    this.absentDrafts[studentId] = absent;
  }

  submitRollCall(): void {
    const assignment = this.selectedAssignment;
    if (!assignment || this.rollCallForm.invalid) {
      this.notifications.show('Select a valid date.', 'error');
      return;
    }
    const absentStudentIds = this.roster.filter((student) => this.absentDrafts[student.id]).map((student) => student.id);
    if (!absentStudentIds.length) {
      this.notifications.show('No students marked absent — nothing to record.', 'error');
      return;
    }
    this.submitting = true;
    this.absencesService.createBulk({
      classId: assignment.classId,
      subject: assignment.subject,
      date: this.rollCallForm.getRawValue().date,
      absentStudentIds
    }).subscribe({
      next: (created) => {
        this.notifications.show(`Recorded ${created.length} absence(s).`, 'success');
        this.absentDrafts = {};
      },
      error: (error) => this.handleError(error, 'Could not record absences.'),
      complete: () => this.submitting = false
    });
  }

  startEditing(absence: AbsenceResponse): void {
    this.editingAbsence = absence;
    this.editAbsenceForm.reset({
      subject: absence.subject,
      date: absence.date,
      comment: absence.comment ?? ''
    });
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
    this.absencesService.update(this.editingAbsence.id, {
      studentId: this.editingAbsence.studentId,
      ...this.editAbsenceForm.getRawValue()
    }).subscribe({
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
    this.submitting = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
