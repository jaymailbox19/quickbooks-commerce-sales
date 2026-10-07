import { Component, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, themeAlpine } from 'ag-grid-community';
import { ProductSalesRanking } from '../../services/sales';

@Component({
  selector: 'app-top-products-panel',
  standalone: true,
  imports: [DatePipe, AgGridAngular],
  templateUrl: './top-products-panel.html',
  styleUrl: './top-products-panel.css'
})
export class TopProductsPanel {
  @Input() products: ProductSalesRanking[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() startDate = '';
  @Input() endDate = '';

  themeAlpine = themeAlpine;

  defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    resizable: true,
    flex: 1,
    minWidth: 100
  };

  columnDefs: ColDef<ProductSalesRanking>[] = [
    {
      headerName: 'Rank',
      field: 'rank',
      maxWidth: 90
    },
    {
      headerName: 'Product',
      field: 'productName',
      minWidth: 140
    },
    {
      headerName: 'Category',
      field: 'categoryName'
    },
    {
      headerName: 'Quantity',
      field: 'totalQuantity',
      maxWidth: 120
    },
    {
      headerName: 'Total Sales',
      field: 'totalSales',
      minWidth: 140,
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