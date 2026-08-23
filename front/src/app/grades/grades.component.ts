import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { MyClassAssignmentResponse } from '../classes/class.model';
import { ClassesService } from '../classes/classes.service';
import { AuthService } from '../core/auth.service';
import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { GradeResponse, GradeStatus, Semester } from './grade.model';
import { GradesService } from './grades.service';

@Component({
  selector: 'app-grades',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './grades.component.html',
  styleUrl: './grades.component.css'
})
export class GradesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly gradesService = inject(GradesService);
  private readonly classesService = inject(ClassesService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly grades = this.gradesService.grades;
  readonly myClasses = this.classesService.myClasses;
  readonly isTeacher = computed(() => this.authService.user()?.role === 'TEACHER');
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMINISTRATION');

  loading = false;
  submitting = false;
  semesterFilter: 'ALL' | Semester = 'ALL';
  statusFilter: 'ALL' | GradeStatus = 'ALL';
  editingGrade: GradeResponse | null = null;
  gradePendingDeletion: GradeResponse | null = null;
  rejectTarget: GradeResponse | null = null;
  rejectComment = '';

  selectedAssignment: MyClassAssignmentResponse | null = null;
  roster: StudentResponse[] = [];
  loadingRoster = false;
  scoreDrafts: Record<number, string> = {};

  rollCallForm = this.fb.nonNullable.group({
    coefficient: [1, [Validators.min(0.1), Validators.max(10)]],
    semester: ['S1' as Semester],
    academicYear: [this.defaultAcademicYear(), [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]]
  });

  editGradeForm = this.fb.nonNullable.group({
    subject: ['', [Validators.required]],
    score: [0, [Validators.min(0), Validators.max(20)]],
    coefficient: [1, [Validators.min(0.1), Validators.max(10)]],
    semester: ['S1' as Semester],
    academicYear: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]]
  });

  ngOnInit(): void {
    if (this.isTeacher()) {
      this.classesService.loadMine().subscribe();
    }
    this.loadGrades();
  }

  get filteredGrades(): GradeResponse[] {
    return this.grades().filter((grade) =>
      (this.semesterFilter === 'ALL' || grade.semester === this.semesterFilter) &&
      (this.statusFilter === 'ALL' || grade.status === this.statusFilter)
    );
  }

  get totalGrades(): number {
    return this.grades().length;
  }

  get pendingCount(): number {
    return this.grades().filter((grade) => grade.status === 'PENDING').length;
  }

  get overallAverage(): number {
    const grades = this.grades().filter((grade) => grade.status === 'APPROVED');
    const totalCoefficient = grades.reduce((sum, grade) => sum + grade.coefficient, 0);
    if (totalCoefficient === 0) return 0;
    const weightedSum = grades.reduce((sum, grade) => sum + grade.score * grade.coefficient, 0);
    return weightedSum / totalCoefficient;
  }

  loadGrades(): void {
    this.loading = true;
    this.gradesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load grades.'),
      complete: () => this.loading = false
    });
  }

  selectAssignment(assignment: MyClassAssignmentResponse): void {
    this.selectedAssignment = assignment;
    this.scoreDrafts = {};
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
    this.scoreDrafts = {};
  }

  submitRollCall(): void {
    const assignment = this.selectedAssignment;
    if (!assignment || this.rollCallForm.invalid) {
      this.notifications.show('Enter a valid coefficient and academic year.', 'error');
      return;
    }
    const entries = this.roster
      .map((student) => ({ studentId: student.id, score: this.scoreDrafts[student.id] ? Number(this.scoreDrafts[student.id]) : null }))
      .filter((entry) => entry.score !== null);
    if (!entries.length) {
      this.notifications.show('Enter at least one student score.', 'error');
      return;
    }
    this.submitting = true;
    const value = this.rollCallForm.getRawValue();
    this.gradesService.createBulk({
      classId: assignment.classId,
      subject: assignment.subject,
      coefficient: value.coefficient,
      semester: value.semester,
      academicYear: value.academicYear,
      entries
    }).subscribe({
      next: (created) => {
        this.notifications.show(`Submitted ${created.length} grade(s) for review.`, 'success');
        this.scoreDrafts = {};
      },
      error: (error) => this.handleError(error, 'Could not submit grades.'),
      complete: () => this.submitting = false
    });
  }

  startEditingGrade(grade: GradeResponse): void {
    this.editingGrade = grade;
    this.editGradeForm.reset({
      subject: grade.subject,
      score: grade.score,
      coefficient: grade.coefficient,
      semester: grade.semester,
      academicYear: grade.academicYear
    });
  }

  cancelGradeEditing(): void {
    this.editingGrade = null;
  }

  updateGrade(): void {
    if (!this.editingGrade || this.editGradeForm.invalid) {
      this.notifications.show('Complete the required grade details.', 'error');
      return;
    }
    this.loading = true;
    this.gradesService.update(this.editingGrade.id, {
      studentId: this.editingGrade.studentId,
      ...this.editGradeForm.getRawValue()
    }).subscribe({
      next: (grade) => {
        this.editingGrade = null;
        this.notifications.show(`Updated ${grade.subject} grade for ${grade.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not update grade.'),
      complete: () => this.loading = false
    });
  }

  requestGradeDelete(grade: GradeResponse): void {
    this.gradePendingDeletion = grade;
  }

  cancelGradeDelete(): void {
    this.gradePendingDeletion = null;
  }

  deleteGrade(): void {
    const grade = this.gradePendingDeletion;
    if (!grade) return;
    this.loading = true;
    this.gradesService.delete(grade.id).subscribe({
      next: () => {
        this.gradePendingDeletion = null;
        this.notifications.show(`Deleted ${grade.subject} grade for ${grade.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete grade.'),
      complete: () => this.loading = false
    });
  }

  approveGrade(grade: GradeResponse): void {
    this.gradesService.validate(grade.id, true, '').subscribe({
      next: () => this.notifications.show(`Approved ${grade.subject} grade for ${grade.studentName}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not approve grade.')
    });
  }

  startRejectGrade(grade: GradeResponse): void {
    this.rejectTarget = grade;
    this.rejectComment = '';
  }

  cancelRejectGrade(): void {
    this.rejectTarget = null;
    this.rejectComment = '';
  }

  confirmRejectGrade(): void {
    const grade = this.rejectTarget;
    if (!grade) return;
    this.gradesService.validate(grade.id, false, this.rejectComment.trim()).subscribe({
      next: () => {
        this.notifications.show(`Rejected ${grade.subject} grade for ${grade.studentName}.`, 'success');
        this.cancelRejectGrade();
      },
      error: (error) => this.handleError(error, 'Could not reject grade.')
    });
  }

  private defaultAcademicYear(): string {
    const now = new Date();
    const startYear = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1;
    return `${startYear}-${startYear + 1}`;
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    this.submitting = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
