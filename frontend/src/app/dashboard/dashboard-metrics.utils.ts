import type {
  BacktestComparisonResult,
  CategorySales
} from '../services/sales';

export function calculateMae(
  rows: BacktestComparisonResult[],
  errorField: 'movingAverageError' | 'xgboostError'
): number | null {
  if (rows.length === 0) {
    return null;
  }

  const totalError = rows.reduce(
    (sum, row) => sum + Math.abs(row[errorField]),
    0
  );

  return totalError / rows.length;
}

export function calculateMaeDifference(
  movingAverageMae: number | null,
  xgboostMae: number | null
): number | null {
  if (movingAverageMae === null || xgboostMae === null) {
    return null;
  }

  return Math.abs(movingAverageMae - xgboostMae);
}

export function findTopSellingCategory(
  rows: CategorySales[]
): CategorySales | null {
  if (rows.length === 0) {
    return null;
  }

  return rows.reduce((top, item) =>
    item.totalSales > top.totalSales ? item : top
  );
}

export function getCategoryOptions(
  rows: CategorySales[]
): string[] {
  return [...new Set(rows.map(item => item.category))];
}