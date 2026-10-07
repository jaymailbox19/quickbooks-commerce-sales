package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestComparisonResult(
        String category,
        LocalDate predictionWeek,
        BigDecimal actualSales,
        BigDecimal movingAveragePrediction,
        BigDecimal movingAverageError,
        BigDecimal xgboostPrediction,
        BigDecimal xgboostError
) {
}