package com.quickbooks.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CategoryDailySales(String category, LocalDate date, BigDecimal sales) {

}
