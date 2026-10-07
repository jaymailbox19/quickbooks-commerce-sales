package com.quickbooks.sales.dto;

import java.math.BigDecimal;

public record ProductForecast(
        Long productId,
        String productName,
        BigDecimal historicalPercentage,
        BigDecimal forecastAmount
) {}