package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestResult(String category, LocalDate predictionWeek, BigDecimal predictedSales,
                             BigDecimal actualSales, BigDecimal absoluteError) {

}
