package com.quickbooks.sales.controller;

import com.quickbooks.sales.dto.*;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/top-categories")
    public List<TopCategoryPredictionResponse> predictTopCategories(
            @RequestParam(defaultValue = "MONTH") Period period) {

        return predictionService.predictTopCategories(period);
    }

    @GetMapping("/forecast")
    public CategoryPredictionResponse forecastCategory(
            @RequestParam Long tenantId,
            @RequestParam String category,
            @RequestParam(defaultValue = "WEEK") Period period) {

        return predictionService.forecastCategory(tenantId,category, period);
    }

    @GetMapping("/daily-sales")
    public List<CategoryDailySales> getDailySales(
            @RequestParam(defaultValue = "WEEK") Period period) {

        return predictionService.getDailySales(period);
    }

    @GetMapping("/weekly-sales")
    public Map<String, Map<LocalDate, BigDecimal>> getWeeklySales(
        @RequestParam(defaultValue = "WEEK") Period period) {       

         return predictionService.getWeeklySalesForPeriod(period);
    }
    
    @GetMapping("/backtest")
    public List<BacktestResult> runBacktest() {
        return predictionService.runBacktest();
    }

    @GetMapping("/backtest/xgboost")
    public List<BacktestResult> runXgboostBacktest() {
        return predictionService.runXgboostBacktest();
    }

    @GetMapping("/backtest/compare")
    public List<BacktestComparisonResult> compareBacktests() {
        return predictionService.compareBacktests();
    }


@GetMapping("/backtest/mae")
public BigDecimal getBacktestMAE() {

    List<BacktestResult> results =
            predictionService.runBacktest();

    return predictionService.calculateMAE(results);
}


}
