import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { NotificationBannerComponent } from '../core/notification-banner.component';
import { NotificationService } from '../core/notification.service';

const COLLAPSED_KEY = 'academix_sidebar_collapsed';

const PAGE_TITLES: Record<string, { eyebrow: string; title: string }> = {
  '/students': { eyebrow: 'Student directory', title: 'Good to see you again.' },
  '/teachers': { eyebrow: 'Teacher directory', title: 'Manage your teaching staff.' },
  '/classes': { eyebrow: 'School structure', title: 'Levels and classes.' },
  '/my-classes': { eyebrow: 'My classes', title: 'The classes you teach.' },
  '/invoices': { eyebrow: 'Billing', title: 'Invoices at a glance.' },
  '/grades': { eyebrow: 'Academics', title: 'Grades at a glance.' },
  '/absences': { eyebrow: 'Attendance', title: 'Absences at a glance.' },
  '/statistics': { eyebrow: 'Reporting', title: 'School-wide statistics.' },
  '/documents': { eyebrow: 'Registrar', title: 'Administrative documents.' },
  '/stage-documents': { eyebrow: 'Internships', title: 'Stage document review.' },
  '/me': { eyebrow: 'My space', title: 'Your academic record.' }
};

const ROLE_LABELS: Record<string, string> = {
  ADMINISTRATION: 'Administrator',
  TEACHER: 'Teacher',
  STUDENT: 'Student'
};

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, NotificationBannerComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  readonly user = this.authService.user;
  readonly collapsed = signal(localStorage.getItem(COLLAPSED_KEY) === '1');

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url)
    ),
    { initialValue: this.router.url }
  );

  readonly pageTitle = computed(() => PAGE_TITLES[this.currentUrl()] ?? PAGE_TITLES['/students']);

  readonly initials = computed(() => {
    const email = this.authService.user()?.email;
    return email ? email.slice(0, 2).toUpperCase() : 'AD';
  });

  readonly roleLabel = computed(() => ROLE_LABELS[this.authService.user()?.role ?? ''] ?? 'Account');
  readonly homeRoute = computed(() => this.authService.homeRoute());
  readonly canSeeStudents = computed(() => this.hasAnyRole('ADMINISTRATION', 'TEACHER'));
  readonly canSeeTeachers = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeClasses = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeMyClasses = computed(() => this.hasAnyRole('TEACHER'));
  readonly canSeeInvoices = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeGrades = computed(() => this.hasAnyRole('ADMINISTRATION', 'TEACHER'));
  readonly canSeeAbsences = computed(() => this.hasAnyRole('ADMINISTRATION', 'TEACHER'));
  readonly canSeeStatistics = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeDocuments = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeStageDocuments = computed(() => this.hasAnyRole('ADMINISTRATION'));
  readonly canSeeProfile = computed(() => this.hasAnyRole('STUDENT'));

  private hasAnyRole(...roles: string[]): boolean {
    const role = this.authService.user()?.role;
    return !!role && roles.includes(role);
  }

  toggleCollapsed(): void {
    this.collapsed.update((value) => {
      const next = !value;
      localStorage.setItem(COLLAPSED_KEY, next ? '1' : '0');
      return next;
    });
  }

  logout(): void {
    this.authService.logout();
    this.notifications.show('Signed out.', 'success');
    this.router.navigateByUrl('/login');
  }
}
