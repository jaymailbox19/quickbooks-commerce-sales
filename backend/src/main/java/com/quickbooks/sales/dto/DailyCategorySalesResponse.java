package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCategorySalesResponse(
        String category,
        LocalDate date,
        BigDecimal amount
) {
}