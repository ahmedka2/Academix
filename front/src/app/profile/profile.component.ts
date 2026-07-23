import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AbsenceResponse } from '../absences/absence.model';
import { AbsencesService } from '../absences/absences.service';
import { extractErrorMessage } from '../core/http-error.util';
import { NotificationService } from '../core/notification.service';
import { GradeResponse, Semester } from '../grades/grade.model';
import { GradesService } from '../grades/grades.service';
import { InvoicesService } from '../invoices/invoices.service';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';

const UNJUSTIFIED_THRESHOLD = 3;

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly studentsService = inject(StudentsService);
  private readonly gradesService = inject(GradesService);
  private readonly absencesService = inject(AbsencesService);
  private readonly invoicesService = inject(InvoicesService);
  private readonly notifications = inject(NotificationService);

  readonly grades = this.gradesService.grades;
  readonly absences = this.absencesService.absences;
  readonly invoices = this.invoicesService.invoices;

  profile: StudentResponse | null = null;
  loadingProfile = false;
  loadingGrades = false;
  loadingAbsences = false;
  loadingInvoices = false;
  semesterFilter: 'ALL' | Semester = 'ALL';
  justifyTarget: AbsenceResponse | null = null;
  justificationText = '';
  editingPhone = false;
  phoneDraft = '';
  savingPhone = false;
  uploadingPhoto = false;

  ngOnInit(): void {
    this.loadProfile();
    this.loadGrades();
    this.loadAbsences();
    this.loadInvoices();
  }

  get filteredGrades(): GradeResponse[] {
    return this.semesterFilter === 'ALL'
      ? this.grades()
      : this.grades().filter((grade) => grade.semester === this.semesterFilter);
  }

  get overallAverage(): number {
    const grades = this.grades();
    const totalCoefficient = grades.reduce((sum, grade) => sum + grade.coefficient, 0);
    if (totalCoefficient === 0) return 0;
    const weightedSum = grades.reduce((sum, grade) => sum + grade.score * grade.coefficient, 0);
    return weightedSum / totalCoefficient;
  }

  loadProfile(): void {
    this.loadingProfile = true;
    this.studentsService.getMe().subscribe({
      next: (profile) => this.profile = profile,
      error: (error) => this.handleError(error, 'Could not load your profile.'),
      complete: () => this.loadingProfile = false
    });
  }

  loadGrades(): void {
    this.loadingGrades = true;
    this.gradesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your grades.'),
      complete: () => this.loadingGrades = false
    });
  }

  startEditingPhone(): void {
    this.editingPhone = true;
    this.phoneDraft = this.profile?.phone ?? '';
  }

  cancelEditingPhone(): void {
    this.editingPhone = false;
  }

  savePhone(): void {
    this.savingPhone = true;
    this.studentsService.updateMyPhone(this.phoneDraft.trim()).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.editingPhone = false;
        this.notifications.show('Phone number updated.', 'success');
      },
      error: (error) => this.handleError(error, 'Could not update phone number.'),
      complete: () => this.savingPhone = false
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.notifications.show('Photo must be a JPEG, PNG, or WEBP image.', 'error');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.notifications.show('Photo must be smaller than 2MB.', 'error');
      input.value = '';
      return;
    }

    this.uploadingPhoto = true;
    this.studentsService.uploadMyPhoto(file).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.notifications.show('Profile picture updated.', 'success');
      },
      error: (error) => this.handleError(error, 'Could not upload photo.'),
      complete: () => {
        this.uploadingPhoto = false;
        input.value = '';
      }
    });
  }

  loadInvoices(): void {
    this.loadingInvoices = true;
    this.invoicesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your invoices.'),
      complete: () => this.loadingInvoices = false
    });
  }

  get outstandingAmount(): number {
    return this.invoices()
      .filter((invoice) => invoice.status === 'UNPAID')
      .reduce((sum, invoice) => sum + invoice.amount, 0);
  }

  get unjustifiedCount(): number {
    return this.absences().filter((absence) => absence.status === 'UNJUSTIFIED').length;
  }

  get overThreshold(): boolean {
    return this.unjustifiedCount >= UNJUSTIFIED_THRESHOLD;
  }

  canJustify(absence: AbsenceResponse): boolean {
    return absence.status === 'UNJUSTIFIED' || absence.status === 'REJECTED';
  }

  loadAbsences(): void {
    this.loadingAbsences = true;
    this.absencesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your absences.'),
      complete: () => this.loadingAbsences = false
    });
  }

  startJustifying(absence: AbsenceResponse): void {
    this.justifyTarget = absence;
    this.justificationText = absence.justification ?? '';
  }

  cancelJustifying(): void {
    this.justifyTarget = null;
    this.justificationText = '';
  }

  submitJustification(): void {
    const target = this.justifyTarget;
    if (!target || !this.justificationText.trim()) {
      this.notifications.show('Enter a justification before submitting.', 'error');
      return;
    }
    this.absencesService.justify(target.id, this.justificationText.trim()).subscribe({
      next: () => {
        this.notifications.show('Justification submitted for review.', 'success');
        this.cancelJustifying();
      },
      error: (error) => this.handleError(error, 'Could not submit justification.')
    });
  }

  private handleError(error: unknown, fallback: string): void {
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
