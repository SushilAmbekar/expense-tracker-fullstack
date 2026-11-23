import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild,
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);
Chart.defaults.font.family = 'Inter, Roboto, "Helvetica Neue", sans-serif';
Chart.defaults.color = '#64748b';

/** Draws a Chart.js chart and redraws it whenever its configuration changes. */
@Component({
  selector: 'app-chart',
  template: `<canvas #canvas [attr.aria-label]="label()" role="img"></canvas>`,
  styles: `
    :host {
      display: block;
      position: relative;
      height: 100%;
      min-height: 220px;
    }
  `,
})
export class ChartComponent implements AfterViewInit, OnDestroy {
  readonly config = input.required<ChartConfiguration>();
  readonly label = input('Chart');

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const config = this.config();
      if (this.chart) {
        this.chart.data = config.data;
        this.chart.options = config.options ?? {};
        this.chart.update();
      }
    });
  }

  ngAfterViewInit(): void {
    const config = this.config();
    this.chart = new Chart(this.canvas().nativeElement, {
      ...config,
      options: { responsive: true, maintainAspectRatio: false, ...config.options },
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
