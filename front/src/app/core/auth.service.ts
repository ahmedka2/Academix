import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'academix_token';

export type Role = 'ADMINISTRATION' | 'TEACHER' | 'STUDENT';

export interface AuthUser {
  id: number;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: {
    id: number;
    fullName: string;
    email: string;
    role: number;
  };
}

const ROLE_BY_CODE: Record<number, Role> = { 0: 'ADMINISTRATION', 1: 'TEACHER', 2: 'STUDENT' };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenSignal = signal(readValidToken());
  private readonly userSignal = signal<AuthUser | null>(userFromToken(this.tokenSignal()));

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/signin', { email, password }).pipe(
      tap((response) => {
        this.tokenSignal.set(response.token);
        this.userSignal.set({
          id: response.user.id,
          email: response.user.email,
          role: ROLE_BY_CODE[response.user.role] ?? 'STUDENT'
        });
        localStorage.setItem(TOKEN_KEY, response.token);
      })
    );
  }

  logout(): void {
    this.tokenSignal.set('');
    this.userSignal.set(null);
    localStorage.removeItem(TOKEN_KEY);
  }

  homeRoute(): string {
    switch (this.userSignal()?.role) {
      case 'STUDENT': return '/me';
      case 'TEACHER':
      case 'ADMINISTRATION': return '/students';
      default: return '/login';
    }
  }
}

function readValidToken(): string {
  const token = localStorage.getItem(TOKEN_KEY) ?? '';
  if (!token) return '';

  const claims = decodeJwtPayload(token);
  const expiresAt = claims?.['exp'];
  if (!claims || (typeof expiresAt === 'number' && expiresAt * 1000 <= Date.now())) {
    localStorage.removeItem(TOKEN_KEY);
    return '';
  }
  return token;
}

function userFromToken(token: string): AuthUser | null {
  if (!token) return null;

  const claims = decodeJwtPayload(token);
  if (!claims) return null;

  const roleName = claims['role'];
  const role: Role = roleName === 'ADMINISTRATION' || roleName === 'TEACHER' || roleName === 'STUDENT'
    ? roleName
    : 'STUDENT';

  return {
    id: Number(claims['userId']),
    email: String(claims['sub'] ?? ''),
    role
  };
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.');
  if (parts.length !== 3) return null;

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}
