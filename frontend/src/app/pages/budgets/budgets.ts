import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import { Budget, Category } from '../../core/models';
import { addMonths, currentMonth, monthLabel } from '../../core/months';
import { confirm } from '../../shared/confirm-dialog';
import { InrPipe } from '../../shared/inr.pipe';
import { MonthSwitcherComponent } from '../../shared/month-switcher';

@Component({
  selector: 'app-budgets',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    InrPipe,
    MonthSwitcherComponent,
  ],
  templateUrl: './budgets.html',
  styleUrl: './budgets.scss',
})
export class BudgetsComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly month = signal(currentMonth());
  protected readonly budgets = signal<Budget[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  /** The budget whose amount is being edited. */
  protected readonly editing = signal<number | null>(null);

  protected readonly monthName = computed(() => monthLabel(this.month()));
  protected readonly previousMonthName = computed(() => monthLabel(addMonths(this.month(), -1)));
  protected readonly unbudgeted = computed(() => {
    const used = new Set(this.budgets().map((b) => b.categoryId));
    return this.categories().filter((c) => !used.has(c.id));
  });
  protected readonly totals = computed(() => {
    const amount = this.budgets().reduce((sum, b) => sum + b.amount, 0);
    const spent = this.budgets().reduce((sum, b) => sum + b.spent, 0);
    return { amount, spent, remaining: amount - spent, percent: amount ? Math.round((spent / amount) * 100) : 0 };
  });

  protected readonly form = inject(FormBuilder).group({
    categoryId: [null as number | null, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    this.load();
  }

  protected changeMonth(month: string): void {
    this.month.set(month);
    this.load();
  }

  protected add(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { categoryId, amount } = this.form.getRawValue();
    this.save(categoryId!, Number(amount), () => this.form.reset());
  }

  protected updateAmount(budget: Budget, value: string): void {
    this.editing.set(null);
    const amount = Number(value);
    if (!value || amount === budget.amount) {
      return;
    }
    if (!(amount >= 1)) {
      this.snackBar.open('Enter an amount of at least ₹1', 'Close', { duration: 3000 });
      return;
    }
    this.save(budget.categoryId, amount);
  }

  protected copyFromLastMonth(): void {
    this.api.copyBudgets(addMonths(this.month(), -1), this.month()).subscribe({
      next: (budgets) => {
        const added = budgets.length - this.budgets().length;
        this.budgets.set(budgets);
        this.snackBar.open(
          added > 0 ? `Copied ${added} budget${added === 1 ? '' : 's'} from ${this.previousMonthName()}` :
            `${this.previousMonthName()} has no budgets to copy`,
          undefined,
          { duration: 3000 },
        );
      },
      error: (error) => this.snackBar.open(errorMessage(error), 'Close'),
    });
  }

  protected remove(budget: Budget): void {
    confirm(this.dialog, {
      title: 'Remove budget?',
      message: `The ${budget.categoryName} budget for ${this.monthName()} will be removed. Your expenses are kept.`,
      confirmLabel: 'Remove',
    }).subscribe((yes) => {
      if (yes) {
        this.api.deleteBudget(budget.id).subscribe({
          next: () => this.budgets.update((list) => list.filter((b) => b.id !== budget.id)),
          error: (error) => this.snackBar.open(errorMessage(error), 'Close'),
        });
      }
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ budgets: this.api.budgets(this.month()), categories: this.api.categories() }).subscribe({
      next: ({ budgets, categories }) => {
        this.budgets.set(budgets);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private save(categoryId: number, amount: number, done?: () => void): void {
    this.api.setBudget(categoryId, this.month(), amount).subscribe({
      next: (saved) => {
        this.budgets.update((list) => {
          const others = list.filter((b) => b.id !== saved.id);
          return [...others, saved].sort((a, b) => a.categoryName.localeCompare(b.categoryName));
        });
        done?.();
      },
      error: (error) => this.snackBar.open(errorMessage(error), 'Close'),
    });
  }
}
