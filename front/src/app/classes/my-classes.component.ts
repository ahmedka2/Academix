import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { MyClassAssignmentResponse } from './class.model';
import { ClassesService } from './classes.service';

@Component({
  selector: 'app-my-classes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-classes.component.html',
  styleUrl: './my-classes.component.css'
})
export class MyClassesComponent implements OnInit {
  private readonly classesService = inject(ClassesService);
  private readonly notifications = inject(NotificationService);

  readonly assignments = this.classesService.myClasses;

  loading = false;
  rosterTarget: MyClassAssignmentResponse | null = null;
  roster: StudentResponse[] = [];
  loadingRoster = false;

  ngOnInit(): void {
    this.loadAssignments();
  }

  loadAssignments(): void {
    this.loading = true;
    this.classesService.loadMine().subscribe({
      error: (error) => this.handleError(error, 'Could not load your classes.'),
      complete: () => this.loading = false
    });
  }

  openRoster(assignment: MyClassAssignmentResponse): void {
    this.rosterTarget = assignment;
    this.loadingRoster = true;
    this.classesService.getRoster(assignment.classId).subscribe({
      next: (students) => this.roster = students,
      error: (error) => this.handleError(error, 'Could not load roster.'),
      complete: () => this.loadingRoster = false
    });
  }

  closeRoster(): void {
    this.rosterTarget = null;
    this.roster = [];
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
