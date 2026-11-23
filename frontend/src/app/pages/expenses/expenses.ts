import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import {
  Category,
  Expense,
  ExpenseQuery,
  PAYMENT_METHODS,
  PaymentMethod,
  SortDirection,
  SortField,
  paymentMethodLabel,
} from '../../core/models';
import { toIsoDate } from '../../core/months';
import { confirm } from '../../shared/confirm-dialog';
import { InrPipe } from '../../shared/inr.pipe';
import { openExpenseDialog } from './expense-dialog';

const SORT_FIELDS: Record<string, SortField> = {
  date: 'DATE',
  amount: 'AMOUNT',
  description: 'DESCRIPTION',
};

@Component({
  selector: 'app-expenses',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    InrPipe,
  ],
  templateUrl: './expenses.html',
  styleUrl: './expenses.scss',
})
export class ExpensesComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['date', 'description', 'category', 'paymentMethod', 'amount', 'actions'];
  protected readonly methods = PAYMENT_METHODS;
  protected readonly paymentMethodLabel = paymentMethodLabel;

  protected readonly categories = signal<Category[]>([]);
  protected readonly rows = signal<Expense[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<{ field: SortField; direction: SortDirection }>({ field: 'DATE', direction: 'DESC' });
  protected readonly pageTotal = computed(() => this.rows().reduce((sum, e) => sum + e.amount, 0));

  protected readonly filters = inject(FormBuilder).group({
    q: [''],
    categoryId: [null as number | null],
    paymentMethod: [null as PaymentMethod | null],
    from: [null as Date | null],
    to: [null as Date | null],
  });

  protected readonly hasFilters = signal(false);

  constructor() {
    this.api.categories().subscribe((categories) => this.categories.set(categories));
    this.filters.valueChanges.pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  protected onSort(sort: Sort): void {
    this.sort.set({
      field: sort.direction ? SORT_FIELDS[sort.active] : 'DATE',
      direction: sort.direction ? (sort.direction.toUpperCase() as SortDirection) : 'DESC',
    });
    this.page.set(0);
    this.load();
  }

  protected onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.load();
  }

  protected clearFilters(): void {
    this.filters.reset();
  }

  protected add(): void {
    openExpenseDialog(this.dialog, null).subscribe((saved) => {
      if (saved) {
        this.snackBar.open('Expense added', undefined, { duration: 2500 });
        this.load();
      }
    });
  }

  protected edit(expense: Expense): void {
    openExpenseDialog(this.dialog, expense).subscribe((saved) => {
      if (saved) {
        this.snackBar.open('Expense updated', undefined, { duration: 2500 });
        this.load();
      }
    });
  }

  protected remove(expense: Expense): void {
    confirm(this.dialog, {
      title: 'Delete expense?',
      message: `"${expense.description || expense.category.name}" will be deleted. This cannot be undone.`,
    }).subscribe((yes) => {
      if (!yes) {
        return;
      }
      this.api.deleteExpense(expense.id).subscribe({
        next: () => {
          this.snackBar.open('Expense deleted', undefined, { duration: 2500 });
          if (this.rows().length === 1 && this.page() > 0) {
            this.page.update((p) => p - 1);
          }
          this.load();
        },
        error: (error) => this.snackBar.open(errorMessage(error), 'Close'),
      });
    });
  }

  protected exportCsv(): void {
    this.api.exportExpenses(this.query()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `expenses-${toIsoDate(new Date())}.csv`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (error) => this.snackBar.open(errorMessage(error), 'Close'),
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    const query = this.query();
    this.hasFilters.set(Boolean(query.q || query.categoryId || query.paymentMethod || query.from || query.to));
    this.api.expenses(query).subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private query(): ExpenseQuery {
    const f = this.filters.getRawValue();
    return {
      q: f.q?.trim() || null,
      categoryId: f.categoryId,
      paymentMethod: f.paymentMethod,
      from: f.from ? toIsoDate(f.from) : null,
      to: f.to ? toIsoDate(f.to) : null,
      page: this.page(),
      size: this.size(),
      sortBy: this.sort().field,
      direction: this.sort().direction,
    };
  }
}
