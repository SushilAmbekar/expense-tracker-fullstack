import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Sends the token with every API call, and signs out when the server rejects it. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isAuthCall = request.url.startsWith('/api/auth/login') || request.url.startsWith('/api/auth/register');
  const token = auth.token;

  if (token && request.url.startsWith('/api/') && !isAuthCall) {
    request = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthCall) {
        auth.sessionExpired();
      }
      return throwError(() => error);
    }),
  );
};
