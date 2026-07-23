import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';
import { GradeResponse, Semester } from './grade.model';
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
  private readonly studentsService = inject(StudentsService);
  private readonly notifications = inject(NotificationService);

  readonly grades = this.gradesService.grades;

  loading = false;
  semesterFilter: 'ALL' | Semester = 'ALL';
  editingGrade: GradeResponse | null = null;
  gradePendingDeletion: GradeResponse | null = null;
  gradeStudentQuery = '';
  editGradeStudentQuery = '';

  gradeForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    subject: ['', [Validators.required]],
    score: [0, [Validators.min(0), Validators.max(20)]],
    coefficient: [1, [Validators.min(0.1), Validators.max(10)]],
    semester: ['S1' as Semester],
    academicYear: [this.defaultAcademicYear(), [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]]
  });

  editGradeForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    subject: ['', [Validators.required]],
    score: [0, [Validators.min(0), Validators.max(20)]],
    coefficient: [1, [Validators.min(0.1), Validators.max(10)]],
    semester: ['S1' as Semester],
    academicYear: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]]
  });

  ngOnInit(): void {
    this.studentsService.load().subscribe();
    this.loadGrades();
  }

  get filteredGrades(): GradeResponse[] {
    return this.semesterFilter === 'ALL'
      ? this.grades()
      : this.grades().filter((grade) => grade.semester === this.semesterFilter);
  }

  get totalGrades(): number {
    return this.grades().length;
  }

  get overallAverage(): number {
    const grades = this.grades();
    const totalCoefficient = grades.reduce((sum, grade) => sum + grade.coefficient, 0);
    if (totalCoefficient === 0) return 0;
    const weightedSum = grades.reduce((sum, grade) => sum + grade.score * grade.coefficient, 0);
    return weightedSum / totalCoefficient;
  }

  get gradeStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.gradeStudentQuery);
  }

  get editGradeStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.editGradeStudentQuery);
  }

  createGrade(): void {
    if (this.gradeForm.invalid) {
      this.notifications.show('Select a student and enter a valid grade.', 'error');
      return;
    }
    this.loading = true;
    this.gradesService.create(this.gradeForm.getRawValue()).subscribe({
      next: (grade) => {
        this.notifications.show(`Recorded ${grade.subject} grade for ${grade.studentName}.`, 'success');
        this.gradeForm.reset({ studentId: 0, subject: '', score: 0, coefficient: 1, semester: 'S1', academicYear: this.defaultAcademicYear() });
        this.gradeStudentQuery = '';
      },
      error: (error) => this.handleError(error, 'Could not create grade.'),
      complete: () => this.loading = false
    });
  }

  loadGrades(): void {
    this.gradesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load grades.')
    });
  }

  startEditingGrade(grade: GradeResponse): void {
    this.editingGrade = grade;
    this.editGradeForm.reset({
      studentId: grade.studentId,
      subject: grade.subject,
      score: grade.score,
      coefficient: grade.coefficient,
      semester: grade.semester,
      academicYear: grade.academicYear
    });
    this.editGradeStudentQuery = `${grade.studentName} · ${grade.studentIdentifier}`;
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
    this.gradesService.update(this.editingGrade.id, this.editGradeForm.getRawValue()).subscribe({
      next: (grade) => {
        this.editingGrade = null;
        this.notifications.show(`Updated ${grade.subject} grade for ${grade.studentName}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not update grade.'),
      complete: () => this.loading = false
    });
  }

  selectGradeStudent(student: StudentResponse, editing = false): void {
    const label = `${student.firstName} ${student.lastName} · ${student.studentIdentifier}`;
    if (editing) {
      this.editGradeForm.patchValue({ studentId: student.id });
      this.editGradeStudentQuery = label;
      return;
    }
    this.gradeForm.patchValue({ studentId: student.id });
    this.gradeStudentQuery = label;
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

  private defaultAcademicYear(): string {
    const now = new Date();
    const startYear = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1;
    return `${startYear}-${startYear + 1}`;
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
