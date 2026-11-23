import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  function signIn(): void {
    auth.login('asha@example.com', 'password123').subscribe();
    backend.expectOne('/api/auth/login').flush({
      token: 'jwt-token',
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
      user: { id: 1, name: 'Asha', email: 'asha@example.com' },
    });
  }

  it('sends the token with API calls once signed in', () => {
    signIn();
    expect(auth.user()?.name).toBe('Asha');

    http.get('/api/expenses').subscribe();
    expect(backend.expectOne('/api/expenses').request.headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('sends no token before signing in', () => {
    http.get('/api/categories').subscribe({ error: () => undefined });
    expect(backend.expectOne('/api/categories').request.headers.has('Authorization')).toBeFalse();
  });

  it('signs out and goes to the login page when the server rejects the token', () => {
    signIn();
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    http.get('/api/expenses').subscribe({ error: () => undefined });
    backend.expectOne('/api/expenses').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBeFalse();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: true } });
  });

  it('keeps the user on the page when a login attempt fails', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate');
    auth.login('asha@example.com', 'wrong').subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/login').flush({ detail: 'Invalid email or password' }, { status: 401, statusText: '' });
    expect(navigate).not.toHaveBeenCalled();
  });
});
