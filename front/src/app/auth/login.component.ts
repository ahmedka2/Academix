import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../core/auth.service';
import { NotificationBannerComponent } from '../core/notification-banner.component';
import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NotificationBannerComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  loading = false;

  loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  login(): void {
    if (this.loginForm.invalid) {
      this.notifications.show('Enter your email and password.', 'error');
      return;
    }

    this.loading = true;
    const { email, password } = this.loginForm.getRawValue();
    this.authService.login(email, password).subscribe({
      next: (response) => {
        this.notifications.show(`Welcome ${response.user.fullName}.`, 'success');
        this.router.navigateByUrl(this.authService.homeRoute());
      },
      error: (error) => {
        this.loading = false;
        this.notifications.show(extractErrorMessage(error) || 'Login failed. Check the backend and credentials.', 'error');
      },
      complete: () => this.loading = false
    });
  }
}
