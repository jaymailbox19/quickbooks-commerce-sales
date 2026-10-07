package com.quickbooks.sales.dto;

import java.math.BigDecimal;

public record WeeklyActual(
        String week,
        BigDecimal actualSales
) {}
