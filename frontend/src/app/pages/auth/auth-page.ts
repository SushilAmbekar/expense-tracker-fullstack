import { Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { errorMessage } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AuthResponse } from '../../core/models';

export const DEMO_EMAIL = 'demo@expense-tracker.dev';
export const DEMO_PASSWORD = 'demo12345';

@Component({
  selector: 'app-auth-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.scss',
})
export class AuthPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Set from the route data. */
  readonly mode = input<'login' | 'register'>('login');
  /** Set when the app signed the user out because the session ended. */
  readonly expired = input<string>();

  protected readonly isRegister = computed(() => this.mode() === 'register');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly hidePassword = signal(true);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected useDemoAccount(): void {
    this.form.patchValue({ email: DEMO_EMAIL, password: DEMO_PASSWORD });
    this.submit(true);
  }

  protected submit(forceLogin = false): void {
    const register = this.isRegister() && !forceLogin;
    const { name, email, password } = this.form.getRawValue();
    if (register && !name.trim()) {
      this.form.controls.name.setErrors({ required: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.error.set(null);
    const request: Observable<AuthResponse> = register
      ? this.auth.register(name.trim(), email, password)
      : this.auth.login(email, password);
    request.subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (error) => {
        this.busy.set(false);
        this.error.set(errorMessage(error));
      },
    });
  }
}
