import { Component, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { SalesService, CategorySales, ForecastResult, BacktestComparisonResult, DailyCategorySales, ProductSalesRanking } from '../services/sales';
import {
  LucideUserRound,
  LucideLogOut,
  LucideLayoutDashboard,
  LucideShoppingCart,
  LucidePackage,
  LucideTrendingUp,
  LucideBarChart3
} from '@lucide/angular';
import {
  BACKTEST_COLUMN_DEFS,
  DAILY_SALES_COLUMN_DEFS,
  DEFAULT_COL_DEF
} from './dashboard-grid.config';
import { getDefaultDateRange } from './dashboard-date.utils';
import {
  calculateMae,
  calculateMaeDifference,
  findTopSellingCategory,
  getCategoryOptions
} from './dashboard-metrics.utils';
import { DailySalesPanel } from './daily-sales-panel/daily-sales-panel';
import { ForecastPanel } from './forecast-panel/forecast-panel';
import { TopProductsPanel } from './top-products-panel/top-products-panel';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ModuleRegistry, themeAlpine } from 'ag-grid-community';

ModuleRegistry.registerModules([AllCommunityModule]);

type DashboardSection = 'overview' | 'sales' | 'products' | 'forecast' | 'analytics';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    AgGridAngular,
    LucideUserRound,
    LucideLogOut,
    LucideLayoutDashboard,
    LucideShoppingCart,
    LucidePackage,
    LucideTrendingUp,
    LucideBarChart3,
    DailySalesPanel,
    ForecastPanel,
    TopProductsPanel
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  activeSection = signal<DashboardSection>('overview');

  columnDefs = BACKTEST_COLUMN_DEFS;
  dailySalesColumnDefs = DAILY_SALES_COLUMN_DEFS;
  defaultColDef = DEFAULT_COL_DEF;

  overviewSales = signal<CategorySales[]>([]);
  overviewLoading = signal(true);
  overviewError = signal('');

  salesResults = signal<CategorySales[]>([]);
  loading = signal(true);
  error = signal('');

  forecast = signal<ForecastResult | null>(null);
  forecastLoading = signal(false);
  forecastError = signal('');

  comparison = signal<BacktestComparisonResult[]>([]);
  comparisonLoading = signal(false);
  comparisonError = signal('');

  selectedCategory = 'Electronics';
  selectedPeriod = 'WEEK';

  // Each workspace owns its own date range so filters never change another page.
  overviewStartDate = '';
  overviewEndDate = '';
  salesStartDate = '';
  salesEndDate = '';
  productsStartDate = '';
  productsEndDate = '';

  dailySales = signal<DailyCategorySales[]>([]);
  dailySalesLoading = signal(false);
  dailySalesError = signal('');
  selectedDailyCategory = signal('');
  showDailySales = signal(false);

  overviewTopProducts = signal<ProductSalesRanking[]>([]);
  overviewTopProductsLoading = signal(false);
  overviewTopProductsError = signal('');

  topProducts = signal<ProductSalesRanking[]>([]);
  topProductsLoading = signal(false);
  topProductsError = signal('');

  salesSearchCategory = signal('ALL');
  productsSearchCategory = signal('ALL');
  categoryOptions: string[] = [];

  filteredSales = computed(() => {
    const rows = this.salesResults();
    const category = this.salesSearchCategory();
    return category === 'ALL' ? rows : rows.filter(item => item.category === category);
  });

  totalSales = computed(() =>
    this.overviewSales().reduce((total, item) => total + Number(item.totalSales || 0), 0)
  );

  topSellingCategory = computed(() => findTopSellingCategory(this.overviewSales()));

  overviewProducts = computed(() => this.overviewTopProducts().slice(0, 5));

  movingAverageMae = computed(() =>
    calculateMae(this.comparison(), 'movingAverageError')
  );

  xgboostMae = computed(() =>
    calculateMae(this.comparison(), 'xgboostError')
  );

  maeDifference = computed(() =>
    calculateMaeDifference(this.movingAverageMae(), this.xgboostMae())
  );

  themeAlpine = themeAlpine;
  paginationPageSize = 10;
  paginationPageSizeSelector: number[] | boolean = [10, 20, 50, 100];
  loggedInUsername: string | undefined;

  readonly navItems: { id: DashboardSection; label: string }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'forecast', label: 'Forecast' },
    { id: 'sales', label: 'Sales' },
    { id: 'products', label: 'Products' },
    { id: 'analytics', label: 'Analytics' }
  ];

  constructor(
    private salesService: SalesService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loggedInUsername = this.authService.getUsername() ?? '';
    const dateRange = getDefaultDateRange();

    this.overviewStartDate = dateRange.startDate;
    this.overviewEndDate = dateRange.endDate;
    this.salesStartDate = dateRange.startDate;
    this.salesEndDate = dateRange.endDate;
    this.productsStartDate = dateRange.startDate;
    this.productsEndDate = dateRange.endDate;

    this.loadOverviewData();
    this.loadForecast();
  }

  selectSection(section: DashboardSection): void {
    this.activeSection.set(section);

    if (section === 'sales') {
      this.loadSalesByDateRange();
    } else if (section === 'products') {
      this.loadTopProducts();
    } else if (section === 'overview') {
      this.loadOverviewData();
    }
  }

  sectionTitle(): string {
    return this.navItems.find(item => item.id === this.activeSection())?.label ?? 'Overview';
  }

  sectionSubtitle(): string {
    switch (this.activeSection()) {
      case 'overview':
        return 'Commerce performance at a glance';
      case 'sales':
        return 'Analyze sales performance by category and date range';
      case 'products':
        return 'Explore products ranked by historical sales amount';
      case 'forecast':
        return 'Forecast category sales and allocate the result across products';
      case 'analytics':
        return 'Evaluate forecasting behavior with historical backtesting';
    }
  }

  loadForecast(): void {
    this.forecastLoading.set(true);
    this.forecastError.set('');
    this.forecast.set(null);

    this.salesService.getForecast(this.selectedCategory, this.selectedPeriod).subscribe({
      next: result => {
        this.forecast.set(result);
        this.forecastLoading.set(false);
      },
      error: err => {
        console.error('Forecast API error:', err);
        this.forecastError.set('Unable to load forecast.');
        this.forecastLoading.set(false);
      }
    });
  }

  loadComparison(): void {
    this.comparisonLoading.set(true);
    this.comparisonError.set('');

    this.salesService.getBacktestComparison().subscribe({
      next: data => {
        this.comparison.set(data);
        this.comparisonLoading.set(false);
      },
      error: err => {
        console.error('Backtest comparison API error:', err);
        this.comparisonError.set('Unable to load model comparison.');
        this.comparisonLoading.set(false);
      }
    });
  }

  loadOverviewData(): void {
    if (!this.isValidDateRange(this.overviewStartDate, this.overviewEndDate)) {
      return;
    }

    this.overviewLoading.set(true);
    this.overviewError.set('');

    this.salesService
      .getSalesByCategoryDateRange(this.overviewStartDate, this.overviewEndDate, 'ALL')
      .subscribe({
        next: data => {
          this.overviewSales.set(data);
          this.updateCategoryOptions(data);
          this.overviewLoading.set(false);
        },
        error: err => {
          console.error('Overview sales API error:', err);
          this.overviewError.set('Unable to load overview sales.');
          this.overviewLoading.set(false);
        }
      });

    this.loadOverviewProducts();
  }

  loadSalesByDateRange(): void {
    if (!this.isValidDateRange(this.salesStartDate, this.salesEndDate)) {
      this.error.set('Please select a valid Sales date range.');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.salesService
      .getSalesByCategoryDateRange(
        this.salesStartDate,
        this.salesEndDate,
        this.salesSearchCategory()
      )
      .subscribe({
        next: data => {
          this.salesResults.set(data);
          this.loading.set(false);
        },
        error: err => {
          console.error('Date-range sales API error:', err);
          this.error.set('Unable to load Sales for the selected dates.');
          this.loading.set(false);
        }
      });
  }

  openDailySales(category: string, startDate: string, endDate: string): void {
    if (!this.isValidDateRange(startDate, endDate)) {
      this.dailySalesError.set('Select a valid date range first.');
      return;
    }

    this.selectedDailyCategory.set(category);
    this.showDailySales.set(true);
    this.dailySalesLoading.set(true);
    this.dailySalesError.set('');
    this.dailySales.set([]);

    this.salesService
      .getDailySalesByCategory(category, startDate, endDate)
      .subscribe({
        next: data => {
          this.dailySales.set(data);
          this.dailySalesLoading.set(false);
        },
        error: err => {
          console.error('Daily sales API error:', err);
          this.dailySalesError.set('Unable to load daily sales.');
          this.dailySalesLoading.set(false);
        }
      });
  }

  private loadOverviewProducts(): void {
    this.overviewTopProductsLoading.set(true);
    this.overviewTopProductsError.set('');
    this.overviewTopProducts.set([]);

    this.salesService
      .getTopProducts(this.overviewStartDate, this.overviewEndDate, undefined)
      .subscribe({
        next: data => {
          this.overviewTopProducts.set(data);
          this.overviewTopProductsLoading.set(false);
        },
        error: err => {
          console.error('Overview top products API error:', err);
          this.overviewTopProductsError.set('Unable to load top-selling products.');
          this.overviewTopProductsLoading.set(false);
        }
      });
  }

  loadTopProducts(): void {
    if (!this.isValidDateRange(this.productsStartDate, this.productsEndDate)) {
      this.topProducts.set([]);
      this.topProductsLoading.set(false);
      this.topProductsError.set('Please select both start and end dates.');
      return;
    }

    const selectedCategory = this.productsSearchCategory();
    const category = selectedCategory === 'ALL' ? undefined : selectedCategory;

    this.topProductsLoading.set(true);
    this.topProductsError.set('');
    this.topProducts.set([]);

    this.salesService.getTopProducts(
      this.productsStartDate,
      this.productsEndDate,
      category
    ).subscribe({
      next: data => {
        this.topProducts.set(data);
        this.topProductsLoading.set(false);
      },
      error: err => {
        console.error('Top products API error:', err);
        this.topProductsError.set('Unable to load top-selling products.');
        this.topProductsLoading.set(false);
      }
    });
  }

  closeDailySales(): void {
    this.showDailySales.set(false);
  }

  clearForecast(): void {
    this.forecast.set(null);
    this.forecastError.set('');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private isValidDateRange(startDate: string, endDate: string): boolean {
    return Boolean(startDate && endDate && startDate <= endDate);
  }

  private updateCategoryOptions(data: CategorySales[]): void {
    this.categoryOptions = getCategoryOptions(data);
  }
}
