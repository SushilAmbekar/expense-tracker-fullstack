import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Budget,
  Category,
  Expense,
  ExpenseQuery,
  ExpenseRequest,
  MonthlySummary,
  MonthlyTotal,
  Page,
  Problem,
} from './models';

/** Typed client for the Expense Tracker REST API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Expenses

  expenses(query: ExpenseQuery): Observable<Page<Expense>> {
    return this.http.get<Page<Expense>>('/api/expenses', { params: toParams(query) });
  }

  exportExpenses(query: ExpenseQuery): Observable<Blob> {
    const { page, size, sortBy, direction, ...filters } = query;
    return this.http.get('/api/expenses/export', { params: toParams(filters), responseType: 'blob' });
  }

  createExpense(request: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>('/api/expenses', request);
  }

  updateExpense(id: number, request: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`/api/expenses/${id}`, request);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`/api/expenses/${id}`);
  }

  // Categories

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  createCategory(name: string, color: string): Observable<Category> {
    return this.http.post<Category>('/api/categories', { name, color });
  }

  updateCategory(id: number, name: string, color: string): Observable<Category> {
    return this.http.put<Category>(`/api/categories/${id}`, { name, color });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`/api/categories/${id}`);
  }

  // Budgets

  budgets(month: string): Observable<Budget[]> {
    return this.http.get<Budget[]>('/api/budgets', { params: { month } });
  }

  setBudget(categoryId: number, month: string, amount: number): Observable<Budget> {
    return this.http.put<Budget>('/api/budgets', { categoryId, month, amount });
  }

  copyBudgets(from: string, to: string): Observable<Budget[]> {
    return this.http.post<Budget[]>('/api/budgets/copy', null, { params: { from, to } });
  }

  deleteBudget(id: number): Observable<void> {
    return this.http.delete<void>(`/api/budgets/${id}`);
  }

  // Analytics

  summary(month: string): Observable<MonthlySummary> {
    return this.http.get<MonthlySummary>('/api/analytics/summary', { params: { month } });
  }

  trend(end: string, months = 6): Observable<MonthlyTotal[]> {
    return this.http.get<MonthlyTotal[]>('/api/analytics/trend', { params: { end, months } });
  }
}

function toParams(values: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

/** A message to show the user for a failed API call. */
export function errorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Check that the backend is running.';
    }
    const problem = error.error as Problem | null;
    if (problem && typeof problem === 'object') {
      if (problem.errors) {
        const [field, message] = Object.entries(problem.errors)[0] ?? [];
        if (field) {
          return `${field} ${message}`;
        }
      }
      if (problem.detail) {
        return problem.detail;
      }
    }
  }
  return fallback;
}
