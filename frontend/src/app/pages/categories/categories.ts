import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService, errorMessage } from '../../core/api.service';
import { Category } from '../../core/models';
import { confirm } from '../../shared/confirm-dialog';

export const COLOURS = [
  '#4f46e5', '#0ea5e9', '#10b981', '#84cc16', '#f59e0b', '#f97316',
  '#ef4444', '#ec4899', '#a855f7', '#64748b',
];

@Component({
  selector: 'app-categories',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
  ],
  templateUrl: './categories.html',
  styleUrl: './categories.scss',
})
export class CategoriesComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly colours = COLOURS;
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  /** The category being edited, or null when adding a new one. */
  protected readonly editing = signal<Category | null>(null);
  protected readonly busy = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(50)]],
    color: [COLOURS[0], Validators.required],
  });

  constructor() {
    this.load();
  }

  protected edit(category: Category): void {
    this.editing.set(category);
    this.form.setValue({ name: category.name, color: category.color });
  }

  protected cancel(): void {
    this.editing.set(null);
    this.form.reset({ name: '', color: COLOURS[0] });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, color } = this.form.getRawValue();
    const editing = this.editing();
    this.busy.set(true);
    const call = editing
      ? this.api.updateCategory(editing.id, name.trim(), color)
      : this.api.createCategory(name.trim(), color);
    call.subscribe({
      next: (saved) => {
        this.busy.set(false);
        this.categories.update((list) =>
          [...list.filter((c) => c.id !== saved.id), saved].sort((a, b) => a.name.localeCompare(b.name)),
        );
        this.snackBar.open(editing ? 'Category updated' : 'Category added', undefined, { duration: 2500 });
        this.cancel();
      },
      error: (error) => {
        this.busy.set(false);
        this.snackBar.open(errorMessage(error), 'Close', { duration: 5000 });
      },
    });
  }

  protected remove(category: Category): void {
    confirm(this.dialog, {
      title: 'Delete category?',
      message: `"${category.name}" will be deleted. A category that still has expenses cannot be deleted.`,
    }).subscribe((yes) => {
      if (yes) {
        this.api.deleteCategory(category.id).subscribe({
          next: () => {
            this.categories.update((list) => list.filter((c) => c.id !== category.id));
            if (this.editing()?.id === category.id) {
              this.cancel();
            }
          },
          error: (error) => this.snackBar.open(errorMessage(error), 'Close', { duration: 6000 }),
        });
      }
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.api.categories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
