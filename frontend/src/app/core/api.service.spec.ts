import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService, errorMessage } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sends only the filters that are set', () => {
    api.expenses({ q: 'tea', categoryId: null, from: '', page: 0, size: 10, sortBy: 'AMOUNT' }).subscribe();
    const request = backend.expectOne((r) => r.url === '/api/expenses');
    expect(request.request.params.keys().sort()).toEqual(['page', 'q', 'size', 'sortBy']);
    expect(request.request.params.get('sortBy')).toBe('AMOUNT');
  });

  it('exports with the filters but without paging', () => {
    api.exportExpenses({ categoryId: 3, page: 2, size: 10, sortBy: 'DATE', direction: 'DESC' }).subscribe();
    const request = backend.expectOne((r) => r.url === '/api/expenses/export');
    expect(request.request.params.keys()).toEqual(['categoryId']);
    expect(request.request.responseType).toBe('blob');
  });
});

describe('errorMessage', () => {
  it('shows the first field error of a validation problem', () => {
    const error = new HttpErrorResponse({ status: 400, error: { title: 'Validation failed', errors: { amount: 'must be positive' } } });
    expect(errorMessage(error)).toBe('amount must be positive');
  });

  it('shows the detail of other problems', () => {
    const error = new HttpErrorResponse({ status: 409, error: { detail: 'Category already exists' } });
    expect(errorMessage(error)).toBe('Category already exists');
  });

  it('explains when the server cannot be reached', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });
});
