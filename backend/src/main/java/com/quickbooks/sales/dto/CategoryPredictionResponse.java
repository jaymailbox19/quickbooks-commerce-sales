package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryPredictionResponse(
        String category,
        BigDecimal predictedSales,
        String historicalThroughDate,
        List<WeeklyPrediction> weeklyPredictions,
        List<WeeklyActual> historicalActuals,
        List<ProductForecast> productForecasts
) {}