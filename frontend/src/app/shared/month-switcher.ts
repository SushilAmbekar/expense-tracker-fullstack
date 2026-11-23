import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { addMonths, currentMonth, monthLabel } from '../core/months';

/** Steps through months: ‹ November 2026 ›. */
@Component({
  selector: 'app-month-switcher',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <button mat-icon-button (click)="monthChange.emit(previous())" matTooltip="Previous month"
            aria-label="Previous month">
      <mat-icon>chevron_left</mat-icon>
    </button>
    <span class="label">{{ label() }}</span>
    <button mat-icon-button (click)="monthChange.emit(next())" [disabled]="isCurrent()" matTooltip="Next month"
            aria-label="Next month">
      <mat-icon>chevron_right</mat-icon>
    </button>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 999px;
      padding: 2px;
    }
    .label {
      min-width: 132px;
      text-align: center;
      font-weight: 600;
    }
  `,
})
export class MonthSwitcherComponent {
  readonly month = input.required<string>();
  readonly monthChange = output<string>();

  protected readonly label = computed(() => monthLabel(this.month()));
  protected readonly previous = computed(() => addMonths(this.month(), -1));
  protected readonly next = computed(() => addMonths(this.month(), 1));
  protected readonly isCurrent = computed(() => this.month() >= currentMonth());
}
