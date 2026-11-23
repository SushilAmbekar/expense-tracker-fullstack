import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import { Category, Expense, PAYMENT_METHODS, PaymentMethod } from '../../core/models';
import { parseIsoDate, toIsoDate } from '../../core/months';

@Component({
  selector: 'app-expense-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ expense ? 'Edit expense' : 'Add expense' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content class="fields">
        @if (error(); as message) {
          <div class="form-error" role="alert">{{ message }}</div>
        }
        <div class="row">
          <mat-form-field>
            <mat-label>Amount</mat-label>
            <span matTextPrefix>₹&nbsp;</span>
            <input matInput type="number" inputmode="decimal" min="0.01" step="0.01" formControlName="amount" />
            @if (form.controls.amount.invalid) {
              <mat-error>Enter an amount above zero</mat-error>
            }
          </mat-form-field>
          <mat-form-field>
            <mat-label>Date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="date" [max]="today" />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
            @if (form.controls.date.invalid) {
              <mat-error>Choose a date</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-form-field>
          <mat-label>Category</mat-label>
          <mat-select formControlName="categoryId">
            @for (c of categories(); track c.id) {
              <mat-option [value]="c.id">
                <span class="dot" [style.background]="c.color"></span>&nbsp; {{ c.name }}
              </mat-option>
            }
          </mat-select>
          @if (form.controls.categoryId.invalid) {
            <mat-error>Choose a category</mat-error>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Description</mat-label>
          <input matInput formControlName="description" maxlength="255" placeholder="e.g. Groceries at the market" />
        </mat-form-field>
        <div class="label">Paid with</div>
        <mat-button-toggle-group formControlName="paymentMethod" aria-label="Payment method" class="methods">
          @for (m of methods; track m.value) {
            <mat-button-toggle [value]="m.value">{{ m.label }}</mat-button-toggle>
          }
        </mat-button-toggle-group>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="busy()">{{ expense ? 'Save changes' : 'Add expense' }}</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .fields {
      display: grid;
      gap: 14px;
      padding-top: 8px !important;
    }
    .row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .label {
      color: var(--muted);
      font-size: 13px;
      margin-bottom: -6px;
    }
    .methods {
      flex-wrap: wrap;
    }
    @media (max-width: 479px) {
      .row {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ExpenseDialogComponent {
  private readonly api = inject(ApiService);
  private readonly ref = inject(MatDialogRef<ExpenseDialogComponent, Expense>);

  protected readonly expense = inject<Expense | null>(MAT_DIALOG_DATA);
  protected readonly methods = PAYMENT_METHODS;
  protected readonly today = new Date();
  protected readonly categories = signal<Category[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).group({
    amount: [this.expense?.amount ?? null, [Validators.required, Validators.min(0.01)]],
    date: [this.expense ? parseIsoDate(this.expense.date) : new Date(), Validators.required],
    categoryId: [this.expense?.category.id ?? null, Validators.required],
    description: [this.expense?.description ?? ''],
    paymentMethod: [this.expense?.paymentMethod ?? ('UPI' as PaymentMethod), Validators.required],
  });

  constructor() {
    this.api.categories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error) => this.error.set(errorMessage(error)),
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = {
      amount: Number(value.amount),
      date: toIsoDate(value.date!),
      categoryId: value.categoryId!,
      description: value.description?.trim() || null,
      paymentMethod: value.paymentMethod!,
    };
    this.busy.set(true);
    const call = this.expense
      ? this.api.updateExpense(this.expense.id, request)
      : this.api.createExpense(request);
    call.subscribe({
      next: (saved) => this.ref.close(saved),
      error: (error) => {
        this.busy.set(false);
        this.error.set(errorMessage(error));
      },
    });
  }
}

/** Opens the add/edit dialog; emits the saved expense, or undefined when cancelled. */
export function openExpenseDialog(dialog: MatDialog, expense: Expense | null): Observable<Expense | undefined> {
  return dialog
    .open(ExpenseDialogComponent, { data: expense, width: '520px', maxWidth: '95vw', autoFocus: 'first-tabbable' })
    .afterClosed();
}
