package com.quickbooks.sales.controller;

import com.quickbooks.sales.dto.CategorySalesResponse;
import com.quickbooks.sales.dto.DailyCategorySalesResponse;
import com.quickbooks.sales.dto.ProductSalesRanking;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.service.SalesService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/by-category")
    public List<CategorySalesResponse> getSalesByCategory(
            @RequestParam(defaultValue = "MONTH") Period period) {
        return salesService.getSalesByCategory(period);
    }

    @GetMapping("/by-category/date-range")
    public List<CategorySalesResponse> getSalesByCategoryByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String category) {

        return salesService.getSalesByCategory(startDate, endDate.plusDays(1), category);
    }

    @GetMapping("/by-category/daily")
    public List<DailyCategorySalesResponse> getDailySalesByCategory(
            @RequestParam String category,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return salesService.getDailySalesByCategory(category, startDate, endDate.plusDays(1));
    }



    @GetMapping("/top-products")
    public List<ProductSalesRanking> getTopProducts(
            @Parameter(
                    description = "Start date, format yyyy-MM-dd",
                    example = "2026-08-21",
                    schema = @Schema(type = "string", format = "date")
            )
            @RequestParam LocalDate startDate,

            @Parameter(
                    description = "End date, format yyyy-MM-dd",
                    example = "2026-09-21",
                    schema = @Schema(type = "string", format = "date")
            )
            @RequestParam LocalDate endDate, @RequestParam(required = false) String category) {

        return salesService.getProductSalesRanking(startDate, endDate, category);
    }
}
