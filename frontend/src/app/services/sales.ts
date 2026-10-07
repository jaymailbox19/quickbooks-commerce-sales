
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CategorySales {
  category: string;
  totalSales: number;
}

export interface WeeklyPrediction {
  week: string;
  predictedSales: number;
}
export interface WeeklyActual {
  week: string;
  actualSales: number;
}

export interface ProductForecast {
  productId: number;
  productName: string;
  historicalPercentage: number;
  forecastAmount: number;
}

export interface ForecastResult {
  category: string;
  predictedSales: number;
  historicalThroughDate: string;
  weeklyPredictions: WeeklyPrediction[];
  historicalActuals: WeeklyActual[];
  productForecasts:ProductForecast[]
}

export interface BacktestComparisonResult {
  category: string;
  predictionWeek: string;
  actualSales: number;
  movingAveragePrediction: number;
  movingAverageError: number;
  xgboostPrediction: number;
  xgboostError: number;
}

export interface DailyCategorySales {
  category: string;
  date: string;
  amount: number;
}

export interface ProductSalesRanking {
  productId: number;
  productName: string;
  categoryName: string;
  totalSales: number;
  totalQuantity: number;
  rank: number;
}

@Injectable({
  providedIn: 'root'
})
export class SalesService {
  private apiUrl = 'http://localhost:8080/api/sales/by-category';

  constructor(private http: HttpClient) {}

  getSalesByCategory(): Observable<CategorySales[]> {
    return this.http.get<CategorySales[]>(this.apiUrl);
  }

  getForecast(category: string, period: string): Observable<ForecastResult> {
    const params = new HttpParams()
      .set('tenantId',1)
      .set('category', category)
      .set('period', period);

    return this.http.get<ForecastResult>(
      'http://localhost:8080/api/predictions/forecast',
      { params }
    );
  }

  getBacktestComparison(): Observable<BacktestComparisonResult[]> {
    return this.http.get<BacktestComparisonResult[]>(
      'http://localhost:8080/api/predictions/backtest/compare'
    );
  }

 getSalesByCategoryDateRange(
  startDate: string,
  endDate: string,
  category?: string
) {
  let params = new HttpParams()
    .set('startDate', startDate)
    .set('endDate', endDate);

  if (category && category !== 'ALL') {
    params = params.set('category', category);
  }

  return this.http.get<CategorySales[]>(
    `${this.apiUrl}/date-range`,
    { params }
  );
}

getDailySalesByCategory(
  category: string,
  startDate: string,
  endDate: string
): Observable<DailyCategorySales[]> {
  const params = new HttpParams()
    .set('category', category)
    .set('startDate', startDate)
    .set('endDate', endDate);

  return this.http.get<DailyCategorySales[]>(
    `${this.apiUrl}/daily`,
    { params }
  );
}

getTopProducts(
  startDate: string,
  endDate: string,
  category?: string
): Observable<ProductSalesRanking[]> {
  let params = new HttpParams()
    .set('startDate', startDate)
    .set('endDate', endDate);

  if (category) {
    params = params.set('category', category);
  }

  return this.http.get<ProductSalesRanking[]>(
    'http://localhost:8080/api/sales/top-products',
    { params }
  );
}

}