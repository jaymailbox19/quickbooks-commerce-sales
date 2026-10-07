package com.quickbooks.sales.service;

import com.quickbooks.sales.dto.*;

import com.quickbooks.sales.exception.MlServiceUnavailableException;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.repository.SaleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.quickbooks.sales.client.MlServiceClient;

import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Slf4j
@Service
public class PredictionService {

private final SaleRepository saleRepository;
private final MlServiceClient mlServiceClient;
private static final int BACKTEST_HISTORY_YEARS = 3;

    private static final Logger log =
            LoggerFactory.getLogger(PredictionService.class);

public PredictionService(
        SaleRepository saleRepository,
        MlServiceClient mlServiceClient) {
    this.saleRepository = saleRepository;
    this.mlServiceClient = mlServiceClient;
}

public List<TopCategoryPredictionResponse> predictTopCategories(Period period) {
    List<CategoryDailySales> dailySales = getDailySales(period);

    Map<String, Map<LocalDate, BigDecimal>> weeklySales =
        getWeeklySales(dailySales);

    List<LocalDate> completedWeeks = getLastCompletedWeeks(4);

    weeklySales = fillMissingWeeks(weeklySales, completedWeeks);

    Map<String, BigDecimal> movingAverages =
        calculateMovingAverage(weeklySales, completedWeeks);

    return movingAverages.entrySet().stream()
        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
        .map(entryItem -> new TopCategoryPredictionResponse(
            entryItem.getKey(),
            entryItem.getValue()
        ))
        .toList();
}

public Map<String, Map<LocalDate, BigDecimal>> getWeeklySalesForPeriod(
        Period period) {

    List<CategoryDailySales> dailySales = getDailySales(period);

    return getWeeklySales(dailySales);
}

private Map<String, Map<LocalDate, BigDecimal>> getWeeklySales(
        List<CategoryDailySales> dailySales) {

    return dailySales.stream()
        .collect(Collectors.groupingBy(
            CategoryDailySales::category,
            Collectors.groupingBy(
                sale -> sale.date()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    CategoryDailySales::sales,
                    BigDecimal::add
                )
            )
        ));
}

public List<CategoryDailySales> getDailySales(Period period) {

    LocalDate startDate = getHistoricalStartDate(period);

    return saleRepository.findSalesByCategoryAndDate(startDate)
            .stream()
            .map(row -> new CategoryDailySales(
                    (String) row[0],
                    (LocalDate) row[1],
                    (BigDecimal) row[2]))
            .toList();
}

    public CategoryPredictionResponse forecastCategory(
            Long tenantId,
            String category,
            Period period) {

        List<ForecastRequest.WeeklySales> historicalSales =
                getHistoricalSales(category, period);

        int horizon = getHorizon(period);

        LocalDate historicalThroughDate =
                historicalSales.get(historicalSales.size() - 1).week();

        List<WeeklyActual> historicalActuals =
                createHistoricalActuals(historicalSales);

        ForecastRequest request =
                new ForecastRequest(
                        tenantId,
                        category,
                        historicalSales,
                        horizon
                );

        CategoryPredictionResponse prediction;

        try {
            prediction = mlServiceClient.forecast(request);

        } catch (MlServiceUnavailableException ex) {

            log.warn(
                    "ML service unavailable. tenant={}, category={}, period={}. "
                            + "Using moving average fallback.",
                    tenantId,
                    category,
                    period,
                    ex
            );

            return forecastUsingMovingAverage(
                    category,
                    period,
                    historicalSales,
                    historicalThroughDate,
                    historicalActuals
            );
        }

        List<ProductForecast> productForecasts =
                calculateProductForecasts(
                        category,
                        period,
                        historicalThroughDate,
                        prediction.predictedSales()
                );

        return new CategoryPredictionResponse(
                prediction.category(),
                prediction.predictedSales(),
                historicalThroughDate.toString(),
                prediction.weeklyPredictions(),
                historicalActuals,
                productForecasts
        );
    }

    private CategoryPredictionResponse forecastUsingMovingAverage(
            String category,
            Period period,
            List<ForecastRequest.WeeklySales> historicalSales,
            LocalDate historicalThroughDate,
            List<WeeklyActual> historicalActuals) {

        int horizon = getHorizon(period);

        List<ForecastRequest.WeeklySales> workingHistory =
                new java.util.ArrayList<>(historicalSales);

        List<WeeklyPrediction> weeklyPredictions =
                new java.util.ArrayList<>();

        BigDecimal totalForecast = BigDecimal.ZERO;

        for (int i = 1; i <= horizon; i++) {

            BigDecimal predictedSales =
                    calculateNextWeekMovingAverage(workingHistory);

            LocalDate predictionWeek =
                    historicalThroughDate.plusWeeks(i);

            weeklyPredictions.add(
                    new WeeklyPrediction(
                            predictionWeek.toString(),
                            predictedSales
                    )
            );

            totalForecast =
                    totalForecast.add(predictedSales);

            // Add the prediction to the working history so that
            // the next forecast uses the previous forecast as input.
            workingHistory.add(
                    new ForecastRequest.WeeklySales(
                            predictionWeek,
                            predictedSales
                    )
            );
        }

        List<ProductForecast> productForecasts =
                calculateProductForecasts(
                        category,
                        period,
                        historicalThroughDate,
                        totalForecast
                );

        return new CategoryPredictionResponse(
                category,
                totalForecast,
                historicalThroughDate.toString(),
                weeklyPredictions,
                historicalActuals,
                productForecasts
        );
    }
    private BigDecimal calculateNextWeekMovingAverage(
            List<ForecastRequest.WeeklySales> historicalSales) {

        int size = historicalSales.size();

        if (size < 4) {
            throw new IllegalArgumentException(
                    "At least 4 weeks of sales history are required "
                            + "for moving average forecast"
            );
        }

        return historicalSales.subList(size - 4, size)
                .stream()
                .map(ForecastRequest.WeeklySales::sales)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                )
                .divide(
                        BigDecimal.valueOf(4),
                        2,
                        RoundingMode.HALF_UP
                );
    }
private List<ForecastRequest.WeeklySales> getHistoricalSales(
        String category,
        Period period) {

    Map<String, Map<LocalDate, BigDecimal>> weeklySales =
            getWeeklySales(getDailySales(period));

    Map<LocalDate, BigDecimal> categorySales =
            weeklySales.get(category);

    if (categorySales == null) {
        throw new IllegalArgumentException(
                "No sales found for category: " + category
        );
    }

    List<ForecastRequest.WeeklySales> historicalSales =
            categorySales.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry ->
                            new ForecastRequest.WeeklySales(
                                    entry.getKey(),
                                    entry.getValue()
                            )
                    )
                    .toList();

    if (historicalSales.size() < 5) {
        throw new IllegalArgumentException(
                "At least 5 weeks of sales history are required"
        );
    }

    return historicalSales;
}

private List<WeeklyActual> createHistoricalActuals(
        List<ForecastRequest.WeeklySales> historicalSales) {

    return historicalSales.stream()
            .map(entry ->
                    new WeeklyActual(
                            entry.week().toString(),
                            entry.sales()
                    )
            )
            .toList();
}

private List<ProductForecast> calculateProductForecasts(
        String category,
        Period period,
        LocalDate historicalThroughDate,
        BigDecimal categoryForecast) {

    List<ProductSalesRanking> productRankings =
            getProductSalesRankings(
                    category,
                    period,
                    historicalThroughDate
            );

    BigDecimal historicalProductTotal =
            productRankings.stream()
                    .map(ProductSalesRanking::totalSales)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

    return productRankings.stream()
            .map(product ->
                    createProductForecast(
                            product,
                            historicalProductTotal,
                            categoryForecast
                    )
            )
            .toList();
}

private List<ProductSalesRanking> getProductSalesRankings(
        String category,
        Period period,
        LocalDate historicalThroughDate) {

    return saleRepository.findProductSalesRanking(
                    getHistoricalStartDate(period),
                    historicalThroughDate.plusDays(7),
                    category
            )
            .stream()
            .map(row ->
                    new ProductSalesRanking(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            (String) row[2],
                            (BigDecimal) row[3],
                            ((Number) row[4]).longValue(),
                            null
                    )
            )
            .toList();
}

private ProductForecast createProductForecast(
        ProductSalesRanking product,
        BigDecimal historicalProductTotal,
        BigDecimal categoryForecast) {

    BigDecimal historicalPercentage =
            historicalProductTotal.signum() == 0
                    ? BigDecimal.ZERO
                    : product.totalSales()
                    .divide(
                            historicalProductTotal,
                            6,
                            RoundingMode.HALF_UP
                    );

    BigDecimal forecastAmount =
            categoryForecast
                    .multiply(historicalPercentage)
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

    return new ProductForecast(
            product.productId(),
            product.productName(),
            historicalPercentage,
            forecastAmount
    );
}

public List<BacktestResult> runBacktest() {

    List<CategoryDailySales> dailySales = getDailySales(Period.WEEK);

    Map<String, Map<LocalDate, BigDecimal>> weeklySales =
            getWeeklySales(dailySales);

    List<LocalDate> predictionWeeks = List.of(
          //  LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 9, 14)
    );

    List<BacktestResult> results = new java.util.ArrayList<>();

    for (Map.Entry<String, Map<LocalDate, BigDecimal>> entry
            : weeklySales.entrySet()) {

        String category = entry.getKey();
        Map<LocalDate, BigDecimal> categorySales = entry.getValue();

        for (LocalDate predictionWeek : predictionWeeks) {

            BigDecimal predictedSales =
                    predictNextWeek(categorySales, predictionWeek);

            BigDecimal actualSales =
                    categorySales.getOrDefault(
                            predictionWeek,
                            BigDecimal.ZERO);

            BigDecimal absoluteError =
                    predictedSales
                            .subtract(actualSales)
                            .abs();

            results.add(new BacktestResult(
                    category,
                    predictionWeek,
                    predictedSales,
                    actualSales,
                    absoluteError
            ));
        }
    }

    return results;
}

public List<BacktestResult> runXgboostBacktest() {

        List<CategoryDailySales> allDailySales =
                getDailySales(Period.YEAR);

        Map<String, Map<LocalDate, BigDecimal>> weeklySales =
                getWeeklySales(allDailySales);

        List<LocalDate> predictionWeeks = List.of(
               // LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 14)
        );

        List<BacktestResult> results = new java.util.ArrayList<>();

        for (Map.Entry<String, Map<LocalDate, BigDecimal>> entry
                : weeklySales.entrySet()) {

            String category = entry.getKey();
            Map<LocalDate, BigDecimal> categorySales = entry.getValue();

            for (LocalDate predictionWeek : predictionWeeks) {

                List<CategoryDailySales> historicalDailySales =
                        saleRepository
                                .findSalesByCategoryAndDateBetween(
                                        predictionWeek.minusYears(BACKTEST_HISTORY_YEARS),
                                        predictionWeek)
                                .stream()
                                .filter(row -> category.equals(row[0]))
                                .map(row -> new CategoryDailySales(
                                        (String) row[0],
                                        (LocalDate) row[1],
                                        (BigDecimal) row[2]))
                                .toList();

                Map<String, Map<LocalDate, BigDecimal>> historicalWeeklySales =
                        getWeeklySales(historicalDailySales);

                Map<LocalDate, BigDecimal> categoryHistory =
                        historicalWeeklySales.getOrDefault(category, Map.of());

                List<ForecastRequest.WeeklySales> history =
                        categoryHistory.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entryItem -> new ForecastRequest.WeeklySales(
                                        entryItem.getKey(),
                                        entryItem.getValue()))
                                .toList();
                if (history.size() < 5) {
                    continue;
                }

                ForecastRequest request =
                        new ForecastRequest(1L,category, history, 1);

                BigDecimal predictedSales =
                        mlServiceClient.forecast(request).predictedSales();

                BigDecimal actualSales =
                        categorySales.getOrDefault(
                                predictionWeek,
                                BigDecimal.ZERO);

                BigDecimal absoluteError =
                        predictedSales.subtract(actualSales).abs();

                results.add(new BacktestResult(
                        category,
                        predictionWeek,
                        predictedSales,
                        actualSales,
                        absoluteError
                ));
            }
        }

        return results;
    }

    public List<BacktestComparisonResult> compareBacktests() {

        List<BacktestResult> movingAverageResults = runBacktest();
        List<BacktestResult> xgboostResults = runXgboostBacktest();

        Map<String, BacktestResult> xgboostByKey =
                xgboostResults.stream()
                        .collect(Collectors.toMap(
                                result -> result.category() + "|" + result.predictionWeek(),
                                result -> result
                        ));

        return movingAverageResults.stream()
                .map(movingAverage -> {
                    String key = movingAverage.category()
                            + "|" + movingAverage.predictionWeek();

                    BacktestResult xgboost = xgboostByKey.get(key);

                    if (xgboost == null) {
                        return null;
                    }

                    return new BacktestComparisonResult(
                            movingAverage.category(),
                            movingAverage.predictionWeek(),
                            movingAverage.actualSales(),
                            movingAverage.predictedSales(),
                            movingAverage.absoluteError(),
                            xgboost.predictedSales(),
                            xgboost.absoluteError()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
public BigDecimal calculateMAE(List<BacktestResult> results) {

    return results.stream()
            .map(BacktestResult::absoluteError)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(
                    BigDecimal.valueOf(results.size()),
                    2,
                    RoundingMode.HALF_UP
            );
}

private LocalDate getHistoricalStartDate(Period period) {
    return switch (period) {
        case WEEK -> LocalDate.now().minusDays(56);
        case MONTH -> LocalDate.now().minusDays(120);
        case YEAR -> LocalDate.now().minusDays(1095);
    };
}

private List<LocalDate> getLastCompletedWeeks(int numberOfWeeks) {
    LocalDate currentWeekStart = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    return IntStream.rangeClosed(1, numberOfWeeks)
        .mapToObj(weekOffset ->
            currentWeekStart.minusWeeks(weekOffset)
        )
        .toList();
}

private Map<String, Map<LocalDate, BigDecimal>> fillMissingWeeks(
        Map<String, Map<LocalDate, BigDecimal>> weeklySales,
        List<LocalDate> completedWeeks) {

    for (String category : weeklySales.keySet()) {
        Map<LocalDate, BigDecimal> categorySales = weeklySales.get(category);

        for (LocalDate week : completedWeeks) {
            categorySales.putIfAbsent(week, BigDecimal.ZERO);
        }
    }

    return weeklySales;
}


private Map<String, BigDecimal> calculateMovingAverage(
        Map<String, Map<LocalDate, BigDecimal>> weeklySales,
        List<LocalDate> completedWeeks) {

    return weeklySales.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> completedWeeks.stream()
                .map(week -> entry.getValue()
                    .getOrDefault(week, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                    BigDecimal.valueOf(completedWeeks.size()),
                    2,
                    RoundingMode.HALF_UP
                )
        ));
}

private BigDecimal predictNextWeek(
        Map<LocalDate, BigDecimal> weeklySales,
        LocalDate predictionWeek) {

    return IntStream.rangeClosed(1, 4)
        .mapToObj(i -> weeklySales.getOrDefault(
            predictionWeek.minusWeeks(i),
            BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(
            BigDecimal.valueOf(4),
            2,
            RoundingMode.HALF_UP
        );
}

private int getHorizon(Period period) {
        return switch (period) {
            case WEEK -> 1;
            case MONTH -> 4;
            case YEAR -> 52;
        };
    }

}
