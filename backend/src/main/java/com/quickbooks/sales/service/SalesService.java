package com.quickbooks.sales.service;

import com.quickbooks.sales.dto.CategorySalesResponse;
import com.quickbooks.sales.dto.DailyCategorySalesResponse;
import com.quickbooks.sales.dto.ProductSalesRanking;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesService {

    private final SaleRepository saleRepository;

    public SalesService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public List<CategorySalesResponse> getSalesByCategory(Period period) {

        LocalDate startDate = switch (period) {
            case WEEK -> LocalDate.now().minusDays(7);
            case MONTH -> LocalDate.now().minusDays(30);
            case YEAR -> LocalDate.now().minusDays(365);
        };

        return saleRepository.findSalesByCategory(startDate)
                .stream()
                .map(row -> new CategorySalesResponse(
                        (String) row[0],
                        (java.math.BigDecimal) row[1]))
                .toList();
    }

    public List<DailyCategorySalesResponse> getDailySalesByCategory(
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        return saleRepository
                .findSalesByCategoryAndDateBetween(startDate, endDate)
                .stream()
                .filter(row -> category.equals(row[0]))
                .map(row -> new DailyCategorySalesResponse(
                        (String) row[0],
                        (LocalDate) row[1],
                        (java.math.BigDecimal) row[2]))
                .toList();
    }
    public List<CategorySalesResponse> getSalesByCategory(
            LocalDate startDate,
            LocalDate endDate) {

        return getSalesByCategory(startDate, endDate, null);
    }

    public List<CategorySalesResponse> getSalesByCategory(
            LocalDate startDate,
            LocalDate endDate,
            String category) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        return saleRepository
                .findSalesByCategoryAndDateBetween(startDate, endDate)
                .stream()
                .filter(row ->
                        category == null
                                || category.isBlank()
                                || "ALL".equalsIgnoreCase(category)
                                || category.equals(row[0]))
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> (String) row[0],
                        java.util.stream.Collectors.reducing(
                                java.math.BigDecimal.ZERO,
                                row -> (java.math.BigDecimal) row[2],
                                java.math.BigDecimal::add
                        )
                ))
                .entrySet()
                .stream()
                .sorted(java.util.Map.Entry
                        .<String, java.math.BigDecimal>comparingByValue()
                        .reversed())
                .map(entry -> new CategorySalesResponse(
                        entry.getKey(),
                        entry.getValue()))
                .toList();
    }

    public List<ProductSalesRanking> getProductSalesRanking(
            LocalDate startDate,
            LocalDate endDate, String category) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        List<Object[]> rows =
                saleRepository.findProductSalesRanking(
                        startDate,
                        endDate.plusDays(1), category);

        List<ProductSalesRanking> rankings = new ArrayList<>();

        long rank = 1;

        for (Object[] row : rows) {
            rankings.add(new ProductSalesRanking(
                    (Long) row[0],
                    (String) row[1],
                    (String) row[2],
                    (BigDecimal) row[3],
                    ((Number) row[4]).longValue(),
                    rank
            ));

            rank++;
        }

        return rankings;
    }
}
