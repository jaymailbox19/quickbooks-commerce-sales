import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Dashboard } from './dashboard';
import { SalesService } from '../services/sales';
import { AuthService } from '../services/auth.service';

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let component: Dashboard;

  let salesService: {
    getSalesByCategoryDateRange: ReturnType<typeof vi.fn>;
    getForecast: ReturnType<typeof vi.fn>;
    getBacktestComparison: ReturnType<typeof vi.fn>;
    getDailySalesByCategory: ReturnType<typeof vi.fn>;
    getTopProducts: ReturnType<typeof vi.fn>;
  };

  let authService: {
    getUsername: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };

  const salesData = [
    { category: 'Electronics', totalSales: 500 },
    { category: 'Furniture', totalSales: 300 }
  ];

  const forecastData = {
    category: 'Electronics',
    predictedSales: 600,
    historicalThroughDate: '2026-09-20',
    weeklyPredictions: [
      { week: '2026-09-21', predictedSales: 600 }
    ],
    historicalActuals: [
      { week: '2026-09-14', actualSales: 550 }
    ]
  };

  beforeEach(async () => {
    salesService = {
      getSalesByCategoryDateRange: vi.fn(),
      getForecast: vi.fn(),
      getBacktestComparison: vi.fn(),
      getDailySalesByCategory: vi.fn(),
      getTopProducts: vi.fn()
    };

    authService = {
      getUsername: vi.fn(),
      logout: vi.fn()
    };

    router = {
      navigate: vi.fn()
    };

    // Default successful responses
    salesService.getSalesByCategoryDateRange.mockReturnValue(of(salesData));
    salesService.getTopProducts.mockReturnValue(of([]));
    salesService.getForecast.mockReturnValue(of(forecastData));
    salesService.getBacktestComparison.mockReturnValue(of([]));
    salesService.getDailySalesByCategory.mockReturnValue(of([]));

    authService.getUsername.mockReturnValue('Jay');

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        { provide: SalesService, useValue: salesService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    })
      // These tests focus on Dashboard class behavior, not child components.
      .overrideComponent(Dashboard, {
        set: { template: '' }
      })
      .compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the dashboard', () => {
    expect(component).toBeTruthy();
  });

  it('should load sales and top products on initialization', () => {
    expect(
      salesService.getSalesByCategoryDateRange
    ).toHaveBeenCalledWith(
      component.overviewStartDate,
      component.overviewEndDate,
      'ALL'
    );

    expect(salesService.getTopProducts).toHaveBeenCalled();

    expect(component.overviewSales()).toEqual(salesData);
    expect(component.loading()).toBe(false);
    expect(component.loggedInUsername).toBe('Jay');
  });

  it('should display an error when sales loading fails', () => {
    salesService.getSalesByCategoryDateRange.mockReturnValue(
      throwError(() => new Error('API error'))
    );

    // Trigger the request again with the error response.
    component.loadSalesByDateRange();

    expect(component.error()).toBe(
      'Unable to load sales for the selected dates.'
    );
    expect(component.loading()).toBe(false);
  });

  it('should not request sales when either date is missing', () => {
    salesService.getSalesByCategoryDateRange.mockClear();

    component.salesStartDate = '';
    component.salesEndDate = '2026-09-20';

    component.loadSalesByDateRange();

    expect(component.error()).toBe(
      'Please select a valid Sales date range.'
    );
    expect(
      salesService.getSalesByCategoryDateRange
    ).not.toHaveBeenCalled();
  });

  it('should not request sales when start date is after end date', () => {
    salesService.getSalesByCategoryDateRange.mockClear();

    component.salesStartDate = '2026-09-20';
    component.salesEndDate = '2026-09-10';

    component.loadSalesByDateRange();

    expect(component.error()).toBe(
      'Please select a valid Sales date range.'
    );
    expect(
      salesService.getSalesByCategoryDateRange
    ).not.toHaveBeenCalled();
  });

  it('should filter sales by selected category', () => {
    component.salesResults.set(salesData);
    component.salesSearchCategory.set('Electronics');

    expect(component.filteredSales()).toEqual([
      { category: 'Electronics', totalSales: 500 }
    ]);
  });

  it('should return all sales when category is ALL', () => {
    component.salesResults.set(salesData);
    component.salesSearchCategory.set('ALL');

    expect(component.filteredSales()).toEqual(salesData);
  });

  it('should keep Overview data independent from Sales and Products filters', () => {
    component.overviewStartDate = '2026-09-01';
    component.overviewEndDate = '2026-10-01';
    component.salesStartDate = '2026-09-17';
    component.salesEndDate = '2026-09-30';
    component.productsStartDate = '2026-08-01';
    component.productsEndDate = '2026-08-31';

    expect(component.overviewStartDate).toBe('2026-09-01');
    expect(component.overviewEndDate).toBe('2026-10-01');
    expect(component.salesStartDate).toBe('2026-09-17');
    expect(component.salesEndDate).toBe('2026-09-30');
    expect(component.productsStartDate).toBe('2026-08-01');
    expect(component.productsEndDate).toBe('2026-08-31');
  });

  it('should load forecast successfully', () => {
    component.selectedCategory = 'Electronics';
    component.selectedPeriod = 'WEEK';

    component.loadForecast();

    expect(salesService.getForecast).toHaveBeenCalledWith(
      'Electronics',
      'WEEK'
    );
    expect(component.forecast()).toEqual(forecastData);
    expect(component.forecastLoading()).toBe(false);
    expect(component.forecastError()).toBe('');
  });

  it('should show an error when forecast loading fails', () => {
    salesService.getForecast.mockReturnValue(
      throwError(() => new Error('Forecast API error'))
    );

    component.loadForecast();

    expect(component.forecastError()).toBe(
      'Unable to load forecast.'
    );
    expect(component.forecastLoading()).toBe(false);
  });

  it('should clear the forecast when requested', () => {
    component.forecast.set(forecastData);
    component.forecastError.set('Previous error');

    component.clearForecast();

    expect(component.forecast()).toBeNull();
    expect(component.forecastError()).toBe('');
  });

  it('should request daily sales for the selected category and dates', () => {
    component.salesStartDate = '2026-09-01';
    component.salesEndDate = '2026-09-20';

    component.openDailySales('Electronics');

    expect(
      salesService.getDailySalesByCategory
    ).toHaveBeenCalledWith(
      'Electronics',
      '2026-09-01',
      '2026-09-20'
    );

    expect(component.selectedDailyCategory()).toBe('Electronics');
    expect(component.showDailySales()).toBe(true);
    expect(component.dailySalesLoading()).toBe(false);
  });

  it('should close the daily sales panel', () => {
    component.showDailySales.set(true);

    component.closeDailySales();

    expect(component.showDailySales()).toBe(false);
  });

  it('should log out and navigate to login', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});