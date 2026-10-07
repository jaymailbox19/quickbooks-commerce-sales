import { Component, Input } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, themeAlpine } from 'ag-grid-community';
import { ProductForecast } from '../../services/sales';

@Component({
  selector: 'app-product-forecast-panel',
  standalone: true,
  imports: [CurrencyPipe, PercentPipe, AgGridAngular],
  templateUrl: './product-forecast-panel.html',
  styleUrl: './product-forecast-panel.css'
})
export class ProductForecastPanel {

  @Input() products: ProductForecast[] = [];
  @Input() category = '';
  @Input() predictedSales = 0;

  themeAlpine = themeAlpine;

  defaultColDef: ColDef = {
    sortable: true,
    resizable: true,
    flex: 1,
    minWidth: 120
  };

  columnDefs: ColDef<ProductForecast>[] = [
    {
      headerName: 'Product',
      field: 'productName',
      minWidth: 160
    },
    {
      headerName: 'Historical Share',
      field: 'historicalPercentage',
      valueFormatter: params =>
        params.value == null
          ? ''
          : `${(params.value * 100).toFixed(1)}%`
    },
    {
      headerName: 'Forecast Amount',
      field: 'forecastAmount',
      minWidth: 150,
      valueFormatter: params =>
        params.value == null
          ? ''
          : new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD',
              maximumFractionDigits: 0
            }).format(params.value)
    }
  ];
}