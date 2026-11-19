package com.quickbooks.sales.controller;

import com.quickbooks.sales.dto.*;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.security.TenantContext;
import com.quickbooks.sales.service.PredictionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/top-categories")
    public List<TopCategoryPredictionResponse> predictTopCategories(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "MONTH") Period period) {
        return predictionService.predictTopCategories(
                TenantContext.requireTenantId(jwt), period);
    }

    @GetMapping("/forecast")
    public CategoryPredictionResponse forecastCategory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String category,
            @RequestParam(defaultValue = "WEEK") Period period) {
        return predictionService.forecastCategory(
                TenantContext.requireTenantId(jwt), category, period);
    }

    @GetMapping("/daily-sales")
    public List<CategoryDailySales> getDailySales(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "WEEK") Period period) {
        return predictionService.getDailySales(
                TenantContext.requireTenantId(jwt), period);
    }

    @GetMapping("/weekly-sales")
    public Map<String, Map<LocalDate, BigDecimal>> getWeeklySales(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "WEEK") Period period) {
        return predictionService.getWeeklySalesForPeriod(
                TenantContext.requireTenantId(jwt), period);
    }

    @GetMapping("/backtest")
    public List<BacktestResult> runBacktest(
            @AuthenticationPrincipal Jwt jwt) {
        return predictionService.runBacktest(
                TenantContext.requireTenantId(jwt));
    }

    @GetMapping("/backtest/xgboost")
    public List<BacktestResult> runXgboostBacktest(
            @AuthenticationPrincipal Jwt jwt) {
        return predictionService.runXgboostBacktest(
                TenantContext.requireTenantId(jwt));
    }

    @GetMapping("/backtest/compare")
    public List<BacktestComparisonResult> compareBacktests(
            @AuthenticationPrincipal Jwt jwt) {
        return predictionService.compareBacktests(
                TenantContext.requireTenantId(jwt));
    }

    @GetMapping("/backtest/mae")
    public BigDecimal getBacktestMAE(
            @AuthenticationPrincipal Jwt jwt) {
        List<BacktestResult> results = predictionService.runBacktest(
                TenantContext.requireTenantId(jwt));
        return predictionService.calculateMAE(results);
    }
}
