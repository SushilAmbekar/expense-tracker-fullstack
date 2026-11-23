export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface Category {
  id: number;
  name: string;
  color: string;
}

export type PaymentMethod = 'CASH' | 'UPI' | 'CARD' | 'NET_BANKING' | 'OTHER';

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'UPI', label: 'UPI' },
  { value: 'CARD', label: 'Card' },
  { value: 'CASH', label: 'Cash' },
  { value: 'NET_BANKING', label: 'Net banking' },
  { value: 'OTHER', label: 'Other' },
];

export function paymentMethodLabel(value: PaymentMethod): string {
  return PAYMENT_METHODS.find((m) => m.value === value)?.label ?? value;
}

export interface Expense {
  id: number;
  amount: number;
  /** yyyy-MM-dd */
  date: string;
  description: string | null;
  paymentMethod: PaymentMethod;
  category: Category;
}

export interface ExpenseRequest {
  amount: number;
  date: string;
  categoryId: number;
  description: string | null;
  paymentMethod: PaymentMethod;
}

export type SortField = 'DATE' | 'AMOUNT' | 'DESCRIPTION' | 'CREATED';
export type SortDirection = 'ASC' | 'DESC';

export interface ExpenseQuery {
  from?: string | null;
  to?: string | null;
  categoryId?: number | null;
  paymentMethod?: PaymentMethod | null;
  q?: string | null;
  page?: number;
  size?: number;
  sortBy?: SortField;
  direction?: SortDirection;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Budget {
  id: number;
  categoryId: number;
  categoryName: string;
  color: string;
  /** yyyy-MM */
  month: string;
  amount: number;
  spent: number;
  remaining: number;
  percentUsed: number;
}

export interface CategorySpend {
  categoryId: number;
  name: string;
  color: string;
  total: number;
  count: number;
  budget: number | null;
  percentOfTotal: number;
}

export interface MonthlySummary {
  month: string;
  total: number;
  previousMonthTotal: number;
  changePercent: number | null;
  transactionCount: number;
  averagePerDay: number;
  budgetTotal: number;
  byCategory: CategorySpend[];
  daily: { date: string; total: number }[];
}

export interface MonthlyTotal {
  month: string;
  total: number;
}

/** The RFC 7807 problem details the API returns for errors. */
export interface Problem {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
