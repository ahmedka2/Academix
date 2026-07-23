import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { TeacherResponse } from '../teachers/teacher.model';
import { TeachersService } from '../teachers/teachers.service';
import { ClassResponse } from './class.model';
import { ClassesService } from './classes.service';
import { LevelResponse } from './level.model';
import { LevelsService } from './levels.service';

@Component({
  selector: 'app-classes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './classes.component.html',
  styleUrl: './classes.component.css'
})
export class ClassesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly levelsService = inject(LevelsService);
  private readonly classesService = inject(ClassesService);
  private readonly teachersService = inject(TeachersService);
  private readonly notifications = inject(NotificationService);

  readonly levels = this.levelsService.levels;
  readonly classes = this.classesService.classes;
  readonly teachers = this.teachersService.teachers;

  loading = false;
  newLevelName = '';
  editingLevel: LevelResponse | null = null;
  editingLevelName = '';

  classForm = this.fb.nonNullable.group({
    levelId: [0, [Validators.min(1)]],
    name: ['', [Validators.required]]
  });

  rosterTarget: ClassResponse | null = null;
  roster: StudentResponse[] = [];
  loadingRoster = false;

  assignTarget: ClassResponse | null = null;
  assignTeacherId = 0;
  assignSubject = '';

  ngOnInit(): void {
    this.loadLevels();
    this.loadClasses();
    this.teachersService.load().subscribe();
  }

  loadLevels(): void {
    this.levelsService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load levels.')
    });
  }

  loadClasses(): void {
    this.classesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load classes.')
    });
  }

  createLevel(): void {
    if (!this.newLevelName.trim()) {
      this.notifications.show('Enter a level name.', 'error');
      return;
    }
    this.levelsService.create(this.newLevelName.trim()).subscribe({
      next: (level) => {
        this.notifications.show(`Created level ${level.name}.`, 'success');
        this.newLevelName = '';
      },
      error: (error) => this.handleError(error, 'Could not create level.')
    });
  }

  startEditingLevel(level: LevelResponse): void {
    this.editingLevel = level;
    this.editingLevelName = level.name;
  }

  cancelEditingLevel(): void {
    this.editingLevel = null;
  }

  saveLevel(): void {
    const level = this.editingLevel;
    if (!level || !this.editingLevelName.trim()) return;
    this.levelsService.update(level.id, this.editingLevelName.trim()).subscribe({
      next: () => {
        this.notifications.show('Level updated.', 'success');
        this.editingLevel = null;
      },
      error: (error) => this.handleError(error, 'Could not update level.')
    });
  }

  deleteLevel(level: LevelResponse): void {
    this.levelsService.delete(level.id).subscribe({
      next: () => this.notifications.show(`Deleted level ${level.name}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not delete level.')
    });
  }

  createClass(): void {
    if (this.classForm.invalid) {
      this.notifications.show('Select a level and enter a class name.', 'error');
      return;
    }
    this.loading = true;
    this.classesService.create(this.classForm.getRawValue()).subscribe({
      next: (schoolClass) => {
        this.notifications.show(`Created class ${schoolClass.name}.`, 'success');
        this.classForm.reset({ levelId: 0, name: '' });
      },
      error: (error) => this.handleError(error, 'Could not create class.'),
      complete: () => this.loading = false
    });
  }

  deleteClass(schoolClass: ClassResponse): void {
    this.classesService.delete(schoolClass.id).subscribe({
      next: () => this.notifications.show(`Deleted class ${schoolClass.name}.`, 'success'),
      error: (error) => this.handleError(error, 'Could not delete class.')
    });
  }

  openRoster(schoolClass: ClassResponse): void {
    this.rosterTarget = schoolClass;
    this.loadingRoster = true;
    this.classesService.getRoster(schoolClass.id).subscribe({
      next: (students) => this.roster = students,
      error: (error) => this.handleError(error, 'Could not load roster.'),
      complete: () => this.loadingRoster = false
    });
  }

  closeRoster(): void {
    this.rosterTarget = null;
    this.roster = [];
  }

  openAssign(schoolClass: ClassResponse): void {
    this.assignTarget = schoolClass;
    this.assignTeacherId = 0;
    this.assignSubject = '';
  }

  cancelAssign(): void {
    this.assignTarget = null;
  }

  submitAssign(): void {
    const target = this.assignTarget;
    if (!target || !this.assignTeacherId || !this.assignSubject.trim()) {
      this.notifications.show('Select a teacher and enter a subject.', 'error');
      return;
    }
    this.classesService.assignTeacher(target.id, this.assignTeacherId, this.assignSubject.trim()).subscribe({
      next: () => {
        this.notifications.show('Teacher assigned.', 'success');
        this.assignTarget = null;
        this.loadClasses();
      },
      error: (error) => this.handleError(error, 'Could not assign teacher.')
    });
  }

  unassignTeacher(schoolClass: ClassResponse, assignmentId: number): void {
    this.classesService.unassignTeacher(schoolClass.id, assignmentId).subscribe({
      next: () => {
        this.notifications.show('Teacher unassigned.', 'success');
        this.loadClasses();
      },
      error: (error) => this.handleError(error, 'Could not unassign teacher.')
    });
  }

  trackTeacher(_: number, teacher: TeacherResponse): number {
    return teacher.id;
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
