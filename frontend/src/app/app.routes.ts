import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Sign in · Expense Tracker',
    data: { mode: 'login' },
    loadComponent: () => import('./pages/auth/auth-page').then((m) => m.AuthPageComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Create account · Expense Tracker',
    data: { mode: 'register' },
    loadComponent: () => import('./pages/auth/auth-page').then((m) => m.AuthPageComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard · Expense Tracker',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardComponent),
      },
      {
        path: 'expenses',
        title: 'Expenses · Expense Tracker',
        loadComponent: () => import('./pages/expenses/expenses').then((m) => m.ExpensesComponent),
      },
      {
        path: 'budgets',
        title: 'Budgets · Expense Tracker',
        loadComponent: () => import('./pages/budgets/budgets').then((m) => m.BudgetsComponent),
      },
      {
        path: 'categories',
        title: 'Categories · Expense Tracker',
        loadComponent: () => import('./pages/categories/categories').then((m) => m.CategoriesComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
