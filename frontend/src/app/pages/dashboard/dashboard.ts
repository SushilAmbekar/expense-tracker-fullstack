import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { forkJoin } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import { Expense, MonthlySummary, MonthlyTotal, paymentMethodLabel } from '../../core/models';
import { currentMonth, daysInMonth, firstDay, lastDay, monthLabel } from '../../core/months';
import { ChartComponent } from '../../shared/chart';
import { InrPipe, formatInr } from '../../shared/inr.pipe';
import { MonthSwitcherComponent } from '../../shared/month-switcher';
import { openExpenseDialog } from '../expenses/expense-dialog';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    ChartComponent,
    InrPipe,
    MonthSwitcherComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);

  protected readonly month = signal(currentMonth());
  protected readonly summary = signal<MonthlySummary | null>(null);
  protected readonly trend = signal<MonthlyTotal[]>([]);
  protected readonly recent = signal<Expense[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly paymentMethodLabel = paymentMethodLabel;

  protected readonly monthName = computed(() => monthLabel(this.month()));
  protected readonly budgets = computed(() =>
    (this.summary()?.byCategory ?? [])
      .filter((c) => c.budget !== null)
      .map((c) => ({ ...c, percent: Math.round((c.total / (c.budget || 1)) * 100) })),
  );
  protected readonly budgetUsed = computed(() => {
    const s = this.summary();
    return s && s.budgetTotal > 0 ? Math.round((s.total / s.budgetTotal) * 100) : null;
  });

  protected readonly categoryChart = computed<ChartConfiguration>(() => {
    const spending = (this.summary()?.byCategory ?? []).filter((c) => c.total > 0);
    return {
      type: 'doughnut',
      data: {
        labels: spending.map((c) => c.name),
        datasets: [
          {
            data: spending.map((c) => c.total),
            backgroundColor: spending.map((c) => c.color),
            borderWidth: 2,
            borderColor: '#ffffff',
            hoverOffset: 6,
          },
        ],
      },
      options: {
        cutout: '68%',
        plugins: {
          legend: { position: 'right', labels: { usePointStyle: true, boxWidth: 8, padding: 14 } },
          tooltip: { callbacks: { label: (item) => ` ${item.label}: ${formatInr(item.parsed as number)}` } },
        },
      },
    };
  });

  protected readonly trendChart = computed<ChartConfiguration>(() => {
    const trend = this.trend();
    const selected = this.month();
    return {
      type: 'bar',
      data: {
        labels: trend.map((t) => monthLabel(t.month, 'short')),
        datasets: [
          {
            label: 'Spent',
            data: trend.map((t) => t.total),
            backgroundColor: trend.map((t) => (t.month === selected ? '#4f46e5' : '#c7d2fe')),
            borderRadius: 8,
            maxBarThickness: 44,
          },
        ],
      },
      options: {
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (item) => ` ${formatInr(item.parsed.y as number)}` } },
        },
        scales: {
          x: { grid: { display: false } },
          y: {
            beginAtZero: true,
            border: { display: false },
            ticks: { callback: (value) => compactInr(Number(value)) },
          },
        },
      },
    };
  });

  protected readonly dailyChart = computed<ChartConfiguration>(() => {
    const s = this.summary();
    const days = daysInMonth(this.month());
    const byDay = new Map((s?.daily ?? []).map((d) => [Number(d.date.slice(8)), d.total]));
    const labels = Array.from({ length: days }, (_, i) => String(i + 1));
    let running = 0;
    const today = new Date();
    const isCurrent = this.month() === currentMonth(today);
    const cumulative = labels.map((_, i) => {
      if (isCurrent && i + 1 > today.getDate()) {
        return null;
      }
      running += byDay.get(i + 1) ?? 0;
      return running;
    });
    const datasets: ChartConfiguration<'line'>['data']['datasets'] = [
      {
        label: 'Spent so far',
        data: cumulative,
        borderColor: '#4f46e5',
        backgroundColor: 'rgba(79, 70, 229, 0.12)',
        fill: true,
        tension: 0.3,
        pointRadius: 0,
        borderWidth: 2.5,
      },
    ];
    if (s && s.budgetTotal > 0) {
      datasets.push({
        label: 'Budget',
        data: labels.map(() => s.budgetTotal),
        borderColor: '#f97316',
        borderDash: [6, 6],
        pointRadius: 0,
        borderWidth: 2,
        fill: false,
      });
    }
    return {
      type: 'line',
      data: { labels, datasets },
      options: {
        interaction: { intersect: false, mode: 'index' },
        plugins: {
          legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } },
          tooltip: {
            callbacks: {
              title: (items) => `Day ${items[0].label}`,
              label: (item) => ` ${item.dataset.label}: ${formatInr(item.parsed.y as number)}`,
            },
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { maxTicksLimit: 10 } },
          y: { beginAtZero: true, border: { display: false }, ticks: { callback: (v) => compactInr(Number(v)) } },
        },
      },
    } as ChartConfiguration;
  });

  constructor() {
    this.load();
  }

  protected changeMonth(month: string): void {
    this.month.set(month);
    this.load();
  }

  protected addExpense(): void {
    openExpenseDialog(this.dialog, null).subscribe((saved) => saved && this.load());
  }

  protected load(): void {
    const month = this.month();
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      summary: this.api.summary(month),
      trend: this.api.trend(month, 6),
      recent: this.api.expenses({ from: firstDay(month), to: lastDay(month), size: 6, sortBy: 'DATE' }),
    }).subscribe({
      next: ({ summary, trend, recent }) => {
        this.summary.set(summary);
        this.trend.set(trend);
        this.recent.set(recent.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}

function compactInr(value: number): string {
  if (value >= 100000) {
    return `₹${(value / 100000).toFixed(value % 100000 === 0 ? 0 : 1)}L`;
  }
  if (value >= 1000) {
    return `₹${(value / 1000).toFixed(value % 1000 === 0 ? 0 : 1)}k`;
  }
  return `₹${value}`;
}
