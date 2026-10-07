import {
  Component,
  EventEmitter,
  Input,
  Output,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import type { ForecastResult } from '../../services/sales';
import { ProductForecastPanel } from '../product-forecast-panel/product-forecast-panel';

import { BaseChartDirective } from 'ng2-charts';
import {
  ChartConfiguration,
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
} from 'chart.js';

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
);

@Component({
  selector: 'app-forecast-panel',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    BaseChartDirective,
    ProductForecastPanel
  ],
  templateUrl: './forecast-panel.html',
  styleUrl: './forecast-panel.css'
})
export class ForecastPanel implements OnChanges {
  @Input() categoryOptions: string[] = [];
  @Input() selectedCategory = '';
  @Input() selectedPeriod = 'WEEK';
  @Input() loading = false;
  @Input() error = '';
  @Input() forecast: ForecastResult | null = null;

  @Output() categoryChange = new EventEmitter<string>();
  @Output() periodChange = new EventEmitter<string>();
  @Output() forecastRequested = new EventEmitter<void>();

  public forecastChartType: 'bar' = 'bar';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forecast']) {
      const actuals = this.forecast?.historicalActuals ?? [];
      const predictions = this.forecast?.weeklyPredictions ?? [];

      // Keep the API order. Each bar is placed at the same index as its
      // corresponding week label, so the value and date cannot drift apart.
      const weeks = [
        ...actuals.map(item => item.week),
        ...predictions.map(item => item.week)
      ];

      this.forecastChartData = {
        labels: weeks,
        datasets: [
          {
            label: 'Actual Sales',
            data: [
              ...actuals.map(item => item.actualSales),
              ...predictions.map(() => null)
            ],
            backgroundColor: '#16a34a',
            borderRadius: 4,
            maxBarThickness: 48
          },
          {
            label: 'Forecast Sales',
            data: [
              ...actuals.map(() => null),
              ...predictions.map(item => item.predictedSales)
            ],
            backgroundColor: '#3b82f6',
            borderRadius: 4,
            maxBarThickness: 48
          }
        ]
      };
    }
  }

  public forecastChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        label: 'Actual Sales',
        data: [],
        backgroundColor: '#16a34a',
        borderRadius: 4,
        maxBarThickness: 48
      },
      {
        label: 'Forecast Sales',
        data: [],
        backgroundColor: '#3b82f6',
        borderRadius: 4,
        maxBarThickness: 48
      }
    ]
  };

  public forecastChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,

    interaction: {
      mode: 'index',
      intersect: false
    },

    plugins: {
      legend: {
        display: true,
        position: 'top',
        align: 'end',
        labels: {
          usePointStyle: true,
          boxWidth: 8,
          padding: 16
        }
      },

      tooltip: {
        callbacks: {
          label: context => {
            const value = context.parsed.y;

            if (value == null) {
              return `${context.dataset.label}: —`;
            }

            return `${context.dataset.label}: $${value.toLocaleString('en-US', {
              maximumFractionDigits: 0
            })}`;
          }
        }
      }
    },

    scales: {
      x: {
        type: 'category',

        grid: {
          display: false
        },

        ticks: {
          color: '#64748b',
          autoSkip: false,

          font: {
            size: 10
          },

          padding: 8,

          // The chart is now full-width, so keep all weekly dates horizontal.
          maxRotation: 0,
          minRotation: 0,

          callback: (value: string | number): string => {
            const index = Number(value);
            const labels = this.forecastChartData.labels ?? [];
            const rawLabel = labels[index];

            if (typeof rawLabel !== 'string') {
              return rawLabel == null ? '' : String(rawLabel);
            }

            const match = rawLabel.match(/^(\d{4})-(\d{2})-(\d{2})/);

            if (!match) {
              return rawLabel;
            }

            const [, year, month, day] = match;

            return new Intl.DateTimeFormat('en-US', {
              month: 'short',
              day: 'numeric'
            }).format(
              new Date(
                Number(year),
                Number(month) - 1,
                Number(day)
              )
            );
          }
        }
      },

      y: {
        beginAtZero: true,

        grid: {
          color: '#e8edf3'
        },

        ticks: {
          color: '#64748b',

          callback: value =>
            `$${Number(value).toLocaleString('en-US', {
              notation: 'compact',
              maximumFractionDigits: 1
            })}`
        }
      }
    }
  };
}
