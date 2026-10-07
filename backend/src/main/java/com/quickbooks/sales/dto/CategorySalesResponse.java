package com.quickbooks.sales.dto;

import java.math.BigDecimal;

public record CategorySalesResponse(String category, BigDecimal totalSales) {

}
