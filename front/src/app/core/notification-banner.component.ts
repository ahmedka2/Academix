import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { NotificationService } from './notification.service';

@Component({
  selector: 'app-notification-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <p class="notice" *ngIf="message()" [class.error]="messageType() === 'error'">
      <span class="notice-icon">
        <svg *ngIf="messageType() === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
        <svg *ngIf="messageType() === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4"/><path d="M12 17h.01"/><circle cx="12" cy="12" r="9"/></svg>
      </span>
      {{ message() }}
    </p>
  `,
  styleUrl: './notification-banner.component.css'
})
export class NotificationBannerComponent {
  private readonly notifications = inject(NotificationService);
  readonly message = this.notifications.message;
  readonly messageType = this.notifications.messageType;
}
