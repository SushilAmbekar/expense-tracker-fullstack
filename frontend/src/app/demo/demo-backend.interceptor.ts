import { HttpErrorResponse, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, delay, dematerialize, materialize, of, throwError } from 'rxjs';
import {
  Budget,
  Category,
  CategorySpend,
  Expense,
  ExpenseRequest,
  MonthlySummary,
  MonthlyTotal,
  Page,
  PaymentMethod,
  User,
} from '../core/models';
import { addMonths, currentMonth, daysInMonth, toIsoDate } from '../core/months';

/**
 * The GitHub Pages demo has no server, so this interceptor answers the API calls in the browser.
 * It follows the same rules as the Spring Boot API (validation, ownership, error messages) and keeps
 * the data in localStorage. Only the demo build registers it.
 */

const STORAGE_KEY = 'expense-tracker.demo-db.v1';
const DEMO_EMAIL = 'demo@expense-tracker.dev';

interface StoredUser extends User {
  password: string;
}

interface StoredExpense {
  id: number;
  userId: number;
  amount: number;
  date: string;
  categoryId: number;
  description: string | null;
  paymentMethod: PaymentMethod;
  createdAt: number;
}

interface StoredBudget {
  id: number;
  userId: number;
  categoryId: number;
  month: string;
  amount: number;
}

interface Db {
  nextId: number;
  users: StoredUser[];
  categories: (Category & { userId: number })[];
  expenses: StoredExpense[];
  budgets: StoredBudget[];
}

const DEFAULT_CATEGORIES: [string, string][] = [
  ['Food & Dining', '#f97316'],
  ['Transport', '#0ea5e9'],
  ['Shopping', '#ec4899'],
  ['Bills & Utilities', '#6366f1'],
  ['Health', '#10b981'],
  ['Entertainment', '#a855f7'],
  ['Travel', '#14b8a6'],
  ['Education', '#eab308'],
  ['Other', '#64748b'],
];

class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string,
    readonly errors?: Record<string, string>,
  ) {
    super(detail);
  }
}

let db: Db = loadDb();

export const demoBackendInterceptor: HttpInterceptorFn = (request) => {
  let response: unknown;
  let status = 200;
  try {
    const result = route(request);
    response = result.body;
    status = result.status;
  } catch (e) {
    const error = e instanceof ApiError ? e : new ApiError(500, 'Unexpected error');
    const titles: Record<number, string> = {
      400: error.errors ? 'Validation failed' : 'Bad Request',
      401: 'Unauthorized',
      404: 'Not Found',
      409: 'Conflict',
    };
    return throwError(
      () =>
        new HttpErrorResponse({
          status: error.status,
          url: request.url,
          error: { title: titles[error.status] ?? 'Error', status: error.status, detail: error.detail, errors: error.errors },
        }),
    ).pipe(materialize(), delay(200), dematerialize());
  }
  saveDb();
  return of(new HttpResponse({ status, body: response, url: request.url })).pipe(delay(200));
};

function route(request: HttpRequest<unknown>): { status: number; body: unknown } {
  const url = new URL(request.urlWithParams, 'http://demo');
  const path = url.pathname;
  const params = url.searchParams;
  const method = request.method;
  const body = (request.body ?? {}) as Record<string, unknown>;
  const ok = (value: unknown, code = 200) => ({ status: code, body: value });
  const id = Number(path.split('/').pop());

  if (path === '/api/auth/register' && method === 'POST') return ok(register(body), 201);
  if (path === '/api/auth/login' && method === 'POST') return ok(login(body));

  const userId = authenticate(request);

  if (path === '/api/auth/me') return ok(publicUser(db.users.find((u) => u.id === userId)!));

  if (path === '/api/categories') {
    if (method === 'GET') return ok(categoriesOf(userId));
    if (method === 'POST') return ok(saveCategory(userId, null, body), 201);
  }
  if (path.startsWith('/api/categories/')) {
    if (method === 'PUT') return ok(saveCategory(userId, id, body));
    if (method === 'DELETE') return ok(deleteCategory(userId, id), 204);
  }

  if (path === '/api/expenses' && method === 'GET') return ok(listExpenses(userId, params));
  if (path === '/api/expenses' && method === 'POST') return ok(saveExpense(userId, null, body), 201);
  if (path === '/api/expenses/export') {
    return ok(new Blob([exportCsv(userId, params)], { type: 'text/csv;charset=utf-8' }));
  }
  if (path.startsWith('/api/expenses/')) {
    if (method === 'GET') return ok(toExpense(ownedExpense(userId, id)));
    if (method === 'PUT') return ok(saveExpense(userId, id, body));
    if (method === 'DELETE') {
      ownedExpense(userId, id);
      db.expenses = db.expenses.filter((e) => e.id !== id);
      return ok(null, 204);
    }
  }

  if (path === '/api/budgets' && method === 'GET') return ok(budgetsOf(userId, month(params.get('month'))));
  if (path === '/api/budgets' && method === 'PUT') return ok(setBudget(userId, body));
  if (path === '/api/budgets/copy') return ok(copyBudgets(userId, month(params.get('from')), month(params.get('to'))));
  if (path.startsWith('/api/budgets/') && method === 'DELETE') {
    if (!db.budgets.some((b) => b.id === id && b.userId === userId)) throw new ApiError(404, `Budget ${id} not found`);
    db.budgets = db.budgets.filter((b) => b.id !== id);
    return ok(null, 204);
  }

  if (path === '/api/analytics/summary') return ok(summary(userId, month(params.get('month'))));
  if (path === '/api/analytics/trend') {
    return ok(trend(userId, month(params.get('end')), Number(params.get('months') ?? 6)));
  }

  throw new ApiError(404, `No endpoint ${method} ${path}`);
}

// Auth

function register(body: Record<string, unknown>): unknown {
  const name = String(body['name'] ?? '').trim();
  const email = String(body['email'] ?? '').trim().toLowerCase();
  const password = String(body['password'] ?? '');
  const errors: Record<string, string> = {};
  if (!name) errors['name'] = 'must not be blank';
  if (!/^[^@\s]+@[^@\s]+$/.test(email)) errors['email'] = 'must be a well-formed email address';
  if (password.length < 8 || password.length > 72) errors['password'] = 'size must be between 8 and 72';
  if (Object.keys(errors).length) throw new ApiError(400, 'Invalid request content.', errors);
  if (db.users.some((u) => u.email === email)) throw new ApiError(409, 'An account with this email already exists');

  const user: StoredUser = { id: nextId(), name, email, password };
  db.users.push(user);
  createDefaultCategories(user.id);
  return tokenFor(user);
}

function login(body: Record<string, unknown>): unknown {
  const email = String(body['email'] ?? '').trim().toLowerCase();
  const user = db.users.find((u) => u.email === email);
  if (!user || user.password !== body['password']) throw new ApiError(401, 'Invalid email or password');
  if (user.email === DEMO_EMAIL) refreshDemoData(user.id);
  return tokenFor(user);
}

function tokenFor(user: StoredUser) {
  return {
    token: `demo-token-${user.id}`,
    expiresAt: new Date(Date.now() + 12 * 3600 * 1000).toISOString(),
    user: publicUser(user),
  };
}

function authenticate(request: HttpRequest<unknown>): number {
  const match = /^Bearer demo-token-(\d+)$/.exec(request.headers.get('Authorization') ?? '');
  const userId = match ? Number(match[1]) : NaN;
  if (!db.users.some((u) => u.id === userId)) throw new ApiError(401, 'Sign in to continue');
  return userId;
}

function publicUser({ id, name, email }: StoredUser): User {
  return { id, name, email };
}

// Categories

function categoriesOf(userId: number): Category[] {
  return db.categories
    .filter((c) => c.userId === userId)
    .map(({ id, name, color }) => ({ id, name, color }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function ownedCategory(userId: number, id: number) {
  const category = db.categories.find((c) => c.id === id && c.userId === userId);
  if (!category) throw new ApiError(404, `Category ${id} not found`);
  return category;
}

function saveCategory(userId: number, id: number | null, body: Record<string, unknown>): Category {
  const name = String(body['name'] ?? '').trim();
  const color = String(body['color'] ?? '').toLowerCase();
  const errors: Record<string, string> = {};
  if (!name) errors['name'] = 'must not be blank';
  if (!/^#[0-9a-f]{6}$/.test(color)) errors['color'] = 'must be a hex colour such as #4f46e5';
  if (Object.keys(errors).length) throw new ApiError(400, 'Invalid request content.', errors);
  if (db.categories.some((c) => c.userId === userId && c.id !== id && c.name.toLowerCase() === name.toLowerCase())) {
    throw new ApiError(409, `A category named '${name}' already exists`);
  }
  if (id === null) {
    const category = { id: nextId(), userId, name, color };
    db.categories.push(category);
    return { id: category.id, name, color };
  }
  const category = ownedCategory(userId, id);
  Object.assign(category, { name, color });
  return { id, name, color };
}

function deleteCategory(userId: number, id: number): null {
  const category = ownedCategory(userId, id);
  const count = db.expenses.filter((e) => e.categoryId === id).length;
  if (count > 0) {
    throw new ApiError(
      409,
      `Category '${category.name}' has ${count} expense(s). Move or delete them before deleting the category.`,
    );
  }
  db.categories = db.categories.filter((c) => c.id !== id);
  db.budgets = db.budgets.filter((b) => b.categoryId !== id);
  return null;
}

function createDefaultCategories(userId: number): void {
  for (const [name, color] of DEFAULT_CATEGORIES) {
    db.categories.push({ id: nextId(), userId, name, color });
  }
}

// Expenses

function ownedExpense(userId: number, id: number): StoredExpense {
  const expense = db.expenses.find((e) => e.id === id && e.userId === userId);
  if (!expense) throw new ApiError(404, `Expense ${id} not found`);
  return expense;
}

function toExpense(e: StoredExpense): Expense {
  const { id, name, color } = db.categories.find((c) => c.id === e.categoryId)!;
  return {
    id: e.id,
    amount: e.amount,
    date: e.date,
    description: e.description,
    paymentMethod: e.paymentMethod,
    category: { id, name, color },
  };
}

function saveExpense(userId: number, id: number | null, body: Record<string, unknown>): Expense {
  const request = body as unknown as ExpenseRequest;
  const amount = Number(request.amount);
  const errors: Record<string, string> = {};
  if (!(amount >= 0.01)) errors['amount'] = 'must be greater than or equal to 0.01';
  if (!request.date) errors['date'] = 'must not be null';
  if (!request.categoryId) errors['categoryId'] = 'must not be null';
  if ((request.description ?? '').length > 255) errors['description'] = 'size must be between 0 and 255';
  if (Object.keys(errors).length) throw new ApiError(400, 'Invalid request content.', errors);
  if (!db.categories.some((c) => c.id === request.categoryId && c.userId === userId)) {
    throw new ApiError(400, `Category ${request.categoryId} not found`);
  }
  const values = {
    amount: Math.round(amount * 100) / 100,
    date: request.date,
    categoryId: request.categoryId,
    description: request.description?.trim() || null,
    paymentMethod: request.paymentMethod,
  };
  if (id === null) {
    const expense: StoredExpense = { id: nextId(), userId, createdAt: Date.now(), ...values };
    db.expenses.push(expense);
    return toExpense(expense);
  }
  const expense = ownedExpense(userId, id);
  Object.assign(expense, values);
  return toExpense(expense);
}

function filtered(userId: number, params: URLSearchParams): StoredExpense[] {
  const from = params.get('from');
  const to = params.get('to');
  if (from && to && from > to) throw new ApiError(400, "'from' must be on or before 'to'");
  const categoryId = params.get('categoryId');
  const paymentMethod = params.get('paymentMethod');
  const q = params.get('q')?.trim().toLowerCase();
  return db.expenses.filter(
    (e) =>
      e.userId === userId &&
      (!from || e.date >= from) &&
      (!to || e.date <= to) &&
      (!categoryId || e.categoryId === Number(categoryId)) &&
      (!paymentMethod || e.paymentMethod === paymentMethod) &&
      (!q || (e.description ?? '').toLowerCase().includes(q)),
  );
}

function listExpenses(userId: number, params: URLSearchParams): Page<Expense> {
  const page = Number(params.get('page') ?? 0);
  const size = Number(params.get('size') ?? 20);
  if (size < 1 || size > 100) throw new ApiError(400, 'size must be between 1 and 100');
  const sortBy = params.get('sortBy') ?? 'DATE';
  const sign = params.get('direction') === 'ASC' ? 1 : -1;
  const key: Record<string, (e: StoredExpense) => string | number> = {
    DATE: (e) => e.date,
    AMOUNT: (e) => e.amount,
    DESCRIPTION: (e) => (e.description ?? '').toLowerCase(),
    CREATED: (e) => e.createdAt,
  };
  const pick = key[sortBy];
  if (!pick) throw new ApiError(400, `Invalid sortBy '${sortBy}'`);
  const rows = filtered(userId, params).sort((a, b) => {
    const x = pick(a);
    const y = pick(b);
    return x < y ? -sign : x > y ? sign : b.id - a.id;
  });
  return {
    content: rows.slice(page * size, page * size + size).map(toExpense),
    page,
    size,
    totalElements: rows.length,
    totalPages: Math.ceil(rows.length / size),
  };
}

function exportCsv(userId: number, params: URLSearchParams): string {
  const field = (value: string) => {
    const safe = /^[=+\-@]/.test(value) ? `'${value}` : value;
    return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
  };
  const rows = filtered(userId, params)
    .sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id)
    .map((e) => {
      const x = toExpense(e);
      return [x.date, field(x.category.name), field(x.description ?? ''), x.paymentMethod, x.amount.toFixed(2)].join(',');
    });
  return ['Date,Category,Description,Payment method,Amount', ...rows].join('\r\n') + '\r\n';
}

// Budgets and analytics

function month(value: string | null): string {
  if (value === null) return currentMonth();
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(value)) throw new ApiError(400, `'${value}' is not a month such as 2026-11`);
  return value;
}

function spentByCategory(userId: number, m: string): Map<number, { total: number; count: number }> {
  const totals = new Map<number, { total: number; count: number }>();
  for (const e of db.expenses) {
    if (e.userId === userId && e.date.startsWith(m)) {
      const t = totals.get(e.categoryId) ?? { total: 0, count: 0 };
      t.total = round(t.total + e.amount);
      t.count++;
      totals.set(e.categoryId, t);
    }
  }
  return totals;
}

function budgetsOf(userId: number, m: string): Budget[] {
  const spent = spentByCategory(userId, m);
  return db.budgets
    .filter((b) => b.userId === userId && b.month === m)
    .map((b) => {
      const category = db.categories.find((c) => c.id === b.categoryId)!;
      const used = spent.get(b.categoryId)?.total ?? 0;
      return {
        id: b.id,
        categoryId: b.categoryId,
        categoryName: category.name,
        color: category.color,
        month: b.month,
        amount: b.amount,
        spent: used,
        remaining: round(b.amount - used),
        percentUsed: Math.round((used / b.amount) * 100),
      };
    })
    .sort((a, b) => a.categoryName.localeCompare(b.categoryName));
}

function setBudget(userId: number, body: Record<string, unknown>): Budget {
  const categoryId = Number(body['categoryId']);
  const m = String(body['month'] ?? '');
  const amount = Number(body['amount']);
  const errors: Record<string, string> = {};
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(m)) errors['month'] = 'must be a month such as 2026-11';
  if (!(amount >= 0.01)) errors['amount'] = 'must be greater than or equal to 0.01';
  if (Object.keys(errors).length) throw new ApiError(400, 'Invalid request content.', errors);
  if (!db.categories.some((c) => c.id === categoryId && c.userId === userId)) {
    throw new ApiError(400, `Category ${categoryId} not found`);
  }
  let budget = db.budgets.find((b) => b.userId === userId && b.categoryId === categoryId && b.month === m);
  if (budget) {
    budget.amount = amount;
  } else {
    budget = { id: nextId(), userId, categoryId, month: m, amount };
    db.budgets.push(budget);
  }
  return budgetsOf(userId, m).find((b) => b.id === budget!.id)!;
}

function copyBudgets(userId: number, from: string, to: string): Budget[] {
  for (const b of db.budgets.filter((x) => x.userId === userId && x.month === from)) {
    if (!db.budgets.some((x) => x.userId === userId && x.month === to && x.categoryId === b.categoryId)) {
      db.budgets.push({ ...b, id: nextId(), month: to });
    }
  }
  return budgetsOf(userId, to);
}

function summary(userId: number, m: string): MonthlySummary {
  const spent = spentByCategory(userId, m);
  const total = round([...spent.values()].reduce((s, t) => s + t.total, 0));
  const previous = round(
    [...spentByCategory(userId, addMonths(m, -1)).values()].reduce((s, t) => s + t.total, 0),
  );
  const budgets = db.budgets.filter((b) => b.userId === userId && b.month === m);
  const budgetFor = new Map(budgets.map((b) => [b.categoryId, b.amount]));
  const ids = new Set([...spent.keys(), ...budgetFor.keys()]);
  const byCategory: CategorySpend[] = [...ids]
    .map((id) => {
      const c = db.categories.find((x) => x.id === id)!;
      const t = spent.get(id) ?? { total: 0, count: 0 };
      return {
        categoryId: id,
        name: c.name,
        color: c.color,
        total: t.total,
        count: t.count,
        budget: budgetFor.get(id) ?? null,
        percentOfTotal: total ? Math.round((t.total / total) * 1000) / 10 : 0,
      };
    })
    .sort((a, b) => b.total - a.total || a.name.localeCompare(b.name));

  const daily = new Map<string, number>();
  for (const e of db.expenses) {
    if (e.userId === userId && e.date.startsWith(m)) daily.set(e.date, round((daily.get(e.date) ?? 0) + e.amount));
  }
  const now = currentMonth();
  const days = m === now ? new Date().getDate() : m < now ? daysInMonth(m) : 0;

  return {
    month: m,
    total,
    previousMonthTotal: previous,
    changePercent: previous ? Math.round(((total - previous) / previous) * 1000) / 10 : null,
    transactionCount: [...spent.values()].reduce((s, t) => s + t.count, 0),
    averagePerDay: days ? round(total / days) : 0,
    budgetTotal: round(budgets.reduce((s, b) => s + b.amount, 0)),
    byCategory,
    daily: [...daily.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([date, t]) => ({ date, total: t })),
  };
}

function trend(userId: number, end: string, months: number): MonthlyTotal[] {
  if (!(months >= 1 && months <= 24)) throw new ApiError(400, 'months must be between 1 and 24');
  return Array.from({ length: months }, (_, i) => {
    const m = addMonths(end, i - months + 1);
    const total = db.expenses
      .filter((e) => e.userId === userId && e.date.startsWith(m))
      .reduce((s, e) => s + e.amount, 0);
    return { month: m, total: round(total) };
  });
}

// Storage and sample data

function round(value: number): number {
  return Math.round(value * 100) / 100;
}

function nextId(): number {
  return db.nextId++;
}

function loadDb(): Db {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as Db;
  } catch {
    // Start fresh
  }
  db = { nextId: 1, users: [], categories: [], expenses: [], budgets: [] };
  const demo: StoredUser = { id: nextId(), name: 'Demo User', email: DEMO_EMAIL, password: 'demo12345' };
  db.users.push(demo);
  createDefaultCategories(demo.id);
  refreshDemoData(demo.id);
  return db;
}

function saveDb(): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
  } catch {
    // The demo still works for this tab
  }
}

/** Gives the demo account three months of realistic expenses ending today, once per day. */
function refreshDemoData(userId: number): void {
  const today = toIsoDate(new Date());
  const latest = db.expenses.filter((e) => e.userId === userId).reduce((max, e) => (e.date > max ? e.date : max), '');
  if (latest && latest.slice(0, 7) === today.slice(0, 7)) return;

  db.expenses = db.expenses.filter((e) => e.userId !== userId);
  db.budgets = db.budgets.filter((b) => b.userId !== userId);
  const category = (name: string) => db.categories.find((c) => c.userId === userId && c.name === name)!.id;

  const templates: [string, string, number, number, PaymentMethod, number][] = [
    // category, description, min, max, payment method, times per month
    ['Food & Dining', 'Groceries', 600, 2200, 'UPI', 5],
    ['Food & Dining', 'Swiggy order', 250, 700, 'UPI', 6],
    ['Food & Dining', 'Lunch with team', 300, 900, 'CARD', 3],
    ['Food & Dining', 'Coffee', 120, 300, 'UPI', 6],
    ['Transport', 'Uber ride', 150, 550, 'UPI', 6],
    ['Transport', 'Metro card recharge', 500, 500, 'UPI', 1],
    ['Transport', 'Petrol', 1200, 2500, 'CARD', 2],
    ['Shopping', 'Amazon order', 450, 3500, 'CARD', 2],
    ['Shopping', 'Clothes', 1200, 3200, 'CARD', 1],
    ['Bills & Utilities', 'Electricity bill', 1400, 2300, 'NET_BANKING', 1],
    ['Bills & Utilities', 'Mobile recharge', 299, 299, 'UPI', 1],
    ['Bills & Utilities', 'Broadband', 799, 799, 'NET_BANKING', 1],
    ['Health', 'Pharmacy', 180, 900, 'UPI', 1],
    ['Health', 'Gym membership', 1500, 1500, 'CARD', 1],
    ['Entertainment', 'Movie tickets', 400, 900, 'UPI', 1],
    ['Entertainment', 'Netflix', 649, 649, 'CARD', 1],
    ['Education', 'Udemy course', 449, 799, 'CARD', 1],
    ['Other', 'Gift', 500, 1500, 'UPI', 1],
  ];

  let seed = 42;
  const random = () => {
    seed = (seed * 1103515245 + 12345) % 2147483648;
    return seed / 2147483648;
  };
  const now = new Date();
  const thisMonth = currentMonth(now);

  for (let back = 2; back >= 0; back--) {
    const m = addMonths(thisMonth, -back);
    const lastDay = back === 0 ? now.getDate() : daysInMonth(m);
    // The current month only has the days so far, so it gets a share of each month's expenses.
    const share = lastDay / daysInMonth(m);
    for (const [name, description, min, max, paymentMethod, times] of templates) {
      const count = back === 0 ? Math.floor(times * share + random()) : times;
      for (let i = 0; i < count; i++) {
        const day = 1 + Math.floor(random() * lastDay);
        db.expenses.push({
          id: nextId(),
          userId,
          amount: Math.round(min + random() * (max - min)),
          date: `${m}-${String(day).padStart(2, '0')}`,
          categoryId: category(name),
          description,
          paymentMethod,
          createdAt: Date.now(),
        });
      }
    }
  }

  const budgets: [string, number][] = [
    ['Food & Dining', 12000],
    ['Transport', 6000],
    ['Shopping', 6000],
    ['Bills & Utilities', 4000],
    ['Entertainment', 1500],
  ];
  for (const [name, amount] of budgets) {
    db.budgets.push({ id: nextId(), userId, categoryId: category(name), month: thisMonth, amount });
  }
}
