import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly message = signal('');
  readonly messageType = signal<NotificationType>('success');

  show(message: string, type: NotificationType): void {
    this.message.set(message);
    this.messageType.set(type);
  }
}
