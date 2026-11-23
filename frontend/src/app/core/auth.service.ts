import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from './models';

const STORAGE_KEY = 'expense-tracker.session';

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

/** Keeps the signed-in user and their token, in memory and in localStorage. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(readStoredSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);

  get token(): string | null {
    const session = this.session();
    if (session && new Date(session.expiresAt).getTime() <= Date.now()) {
      this.clear();
      return null;
    }
    return session?.token ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.store(response)));
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { name, email, password })
      .pipe(tap((response) => this.store(response)));
  }

  logout(): void {
    this.clear();
    this.router.navigate(['/login']);
  }

  /** Signs out because the server no longer accepts the token. */
  sessionExpired(): void {
    if (this.session()) {
      this.clear();
      this.router.navigate(['/login'], { queryParams: { expired: true } });
    }
  }

  private store(response: AuthResponse): void {
    const session: Session = {
      token: response.token,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Storage can be unavailable (private mode); the session then lasts until the tab closes.
    }
  }

  private clear(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nothing to remove
    }
  }
}

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
  } catch {
    return null;
  }
}
