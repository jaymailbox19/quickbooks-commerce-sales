package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ForecastRequest(Long tenantId,String category, List<WeeklySales> historicalSales, int horizon) {

    public record WeeklySales(LocalDate week, BigDecimal sales) {

    }
}