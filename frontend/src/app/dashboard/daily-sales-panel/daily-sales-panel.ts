import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import type { ColDef } from 'ag-grid-community';
import type { DailyCategorySales } from '../../services/sales';

@Component({
  selector: 'app-daily-sales-panel',
  standalone: true,
  imports: [AgGridAngular],
  styleUrl: './daily-sales-panel.css',
  templateUrl: './daily-sales-panel.html'
})
export class DailySalesPanel {
  @Input() category = '';
  @Input() loading = false;
  @Input() error = '';
  @Input() sales: DailyCategorySales[] = [];
  @Input() theme: any;
  @Input() columnDefs: ColDef<DailyCategorySales>[] = [];
  @Input() defaultColDef: ColDef = {};

  @Output() close = new EventEmitter<void>();
}