import type { ColDef } from 'ag-grid-community';
import type {
  BacktestComparisonResult,
  DailyCategorySales
} from '../services/sales';

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0
});

function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '';
  }

  // Parse date-only values in local time to avoid timezone shifts.
  const [year, month, day] = String(value).split('-').map(Number);

  return new Date(year, month - 1, day).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
}

function formatCurrency(value: number | null | undefined): string {
  return value == null ? '' : currencyFormatter.format(value);
}

export const BACKTEST_COLUMN_DEFS: ColDef<BacktestComparisonResult>[] = [
  {
    field: 'category',
    headerName: 'Category',
    minWidth: 130,
    flex: 1,
    filter: true
  },
  {
    field: 'predictionWeek',
    headerName: 'Prediction Week',
    minWidth: 150,
    flex: 1.2,
    valueFormatter: params => formatDate(params.value)
  },
  {
    field: 'actualSales',
    headerName: 'Actual Sales',
    valueFormatter: params => formatCurrency(params.value)
  },
  {
    field: 'movingAveragePrediction',
    headerName: 'Historical Trend',
    headerTooltip: 'Four-week historical moving-average model',
    valueFormatter: params => formatCurrency(params.value)
  },
  {
    field: 'movingAverageError',
    headerName: 'Historical Trend Error',
    valueFormatter: params => formatCurrency(params.value)
  },
  {
    field: 'xgboostPrediction',
    headerName: 'AI-Powered Forecast',
    headerTooltip: 'XGBoost machine-learning model',
    valueFormatter: params => formatCurrency(params.value)
  },
  {
    field: 'xgboostError',
    headerName: 'AI Forecast Error',
    headerTooltip: 'Absolute prediction error from the XGBoost model',
    valueFormatter: params => formatCurrency(params.value)
  }
];

export const DAILY_SALES_COLUMN_DEFS: ColDef<DailyCategorySales>[] = [
  {
    field: 'date',
    headerName: 'Date',
    minWidth: 150,
    flex: 1,
    valueFormatter: params => formatDate(params.value)
  },
  {
    field: 'category',
    headerName: 'Category',
    minWidth: 130,
    flex: 1
  },
  {
    field: 'amount',
    headerName: 'Sales',
    minWidth: 140,
    type: 'numericColumn',
    valueFormatter: params => formatCurrency(params.value)
  }
];

export const DEFAULT_COL_DEF: ColDef = {
  sortable: true,
  resizable: true
};