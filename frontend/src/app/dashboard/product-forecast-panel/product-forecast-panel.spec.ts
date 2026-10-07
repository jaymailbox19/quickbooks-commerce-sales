import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductForecastPanel } from './product-forecast-panel';

describe('ProductForecastPanel', () => {
  let component: ProductForecastPanel;
  let fixture: ComponentFixture<ProductForecastPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductForecastPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductForecastPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
