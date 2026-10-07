package com.quickbooks.sales.service;

import com.quickbooks.sales.client.MlServiceClient;
import com.quickbooks.sales.dto.BacktestResult;
import com.quickbooks.sales.dto.CategoryPredictionResponse;
import com.quickbooks.sales.dto.ForecastRequest;
import com.quickbooks.sales.model.Period;
import com.quickbooks.sales.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class PredictionServiceTest {

    private SaleRepository saleRepository;
    private MlServiceClient mlServiceClient;
    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        mlServiceClient = mock(MlServiceClient.class);
        predictionService =
                new PredictionService(saleRepository, mlServiceClient);
    }

    @Test
    void forecastCategory_shouldRejectCategoryWithNoSales() {
        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> predictionService.forecastCategory(
                        1L,
                        "Electronics",
                        Period.WEEK
                )
        );

        assertEquals(
                "No sales found for category: Electronics",
                exception.getMessage()
        );

        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void forecastCategory_shouldRejectFewerThanFiveWeeksOfHistory() {
        LocalDate monday = LocalDate.of(2026, 8, 3);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{
                                "Electronics",
                                monday,
                                new BigDecimal("9000")
                        },
                        new Object[]{
                                "Electronics",
                                monday.plusDays(7),
                                new BigDecimal("11000")
                        },
                        new Object[]{
                                "Electronics",
                                monday.plusDays(14),
                                new BigDecimal("10000")
                        },
                        new Object[]{
                                "Electronics",
                                monday.plusDays(21),
                                new BigDecimal("13000")
                        }
                ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> predictionService.forecastCategory(
                        1L,
                        "Electronics",
                        Period.WEEK
                )
        );

        assertEquals(
                "At least 5 weeks of sales history are required",
                exception.getMessage()
        );

        verifyNoInteractions(mlServiceClient);
    }

    @Test
    void forecastCategory_shouldReturnPredictionAndHistoricalActuals() {
        LocalDate firstWeek = LocalDate.of(2026, 6, 1);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", firstWeek, new BigDecimal("9000")},
                        new Object[]{"Electronics", firstWeek.plusWeeks(1), new BigDecimal("11000")},
                        new Object[]{"Electronics", firstWeek.plusWeeks(2), new BigDecimal("10000")},
                        new Object[]{"Electronics", firstWeek.plusWeeks(3), new BigDecimal("13000")},
                        new Object[]{"Electronics", firstWeek.plusWeeks(4), new BigDecimal("12000")}
                ));

        CategoryPredictionResponse mlResponse =
                new CategoryPredictionResponse(
                        "Electronics",
                        new BigDecimal("12500.00"),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(mlServiceClient.forecast(any(ForecastRequest.class)))
                .thenReturn(mlResponse);

        CategoryPredictionResponse result =
                predictionService.forecastCategory(
                        1L,
                        "Electronics",
                        Period.WEEK
                );

        assertEquals("Electronics", result.category());
        assertEquals(new BigDecimal("12500.00"), result.predictedSales());
        assertEquals(firstWeek.plusWeeks(4).toString(),
                result.historicalThroughDate());

        assertEquals(5, result.historicalActuals().size());
        assertEquals(firstWeek.toString(),
                result.historicalActuals().get(0).week());
        assertEquals(new BigDecimal("9000"),
                result.historicalActuals().get(0).actualSales());

        verify(mlServiceClient).forecast(any(ForecastRequest.class));
    }
    @Test
    void getDailySales_shouldMapRepositoryRows() {
        LocalDate date = LocalDate.of(2026, 8, 3);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", date, new BigDecimal("1000.00")},
                        new Object[]{"Furniture", date.plusDays(1), new BigDecimal("2000.00")}
                ));

        var result = predictionService.getDailySales(Period.WEEK);

        assertEquals(2, result.size());

        assertEquals("Electronics", result.get(0).category());
        assertEquals(date, result.get(0).date());
        assertEquals(new BigDecimal("1000.00"), result.get(0).sales());

        assertEquals("Furniture", result.get(1).category());
    }

    @Test
    void getWeeklySalesForPeriod_shouldGroupSalesByMonday() {
        LocalDate monday = LocalDate.of(2026, 8, 3);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", monday, new BigDecimal("1000")},
                        new Object[]{"Electronics", monday.plusDays(1), new BigDecimal("2000")},
                        new Object[]{"Electronics", monday.plusDays(3), new BigDecimal("3000")}
                ));

        var result = predictionService.getWeeklySalesForPeriod(Period.MONTH);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Electronics"));

        BigDecimal weeklyTotal =
                result.get("Electronics").get(monday);

        assertEquals(new BigDecimal("6000"), weeklyTotal);
    }

    @Test
    void calculateMAE_shouldReturnAverageAbsoluteError() {
        var results = List.of(
                new BacktestResult(
                        "Electronics",
                        LocalDate.of(2026, 9, 7),
                        new BigDecimal("1000"),
                        new BigDecimal("900"),
                        new BigDecimal("100")
                ),
                new BacktestResult(
                        "Electronics",
                        LocalDate.of(2026, 9, 14),
                        new BigDecimal("1200"),
                        new BigDecimal("1000"),
                        new BigDecimal("200")
                )
        );

        BigDecimal mae = predictionService.calculateMAE(results);

        assertEquals(new BigDecimal("150.00"), mae);
    }

    @Test
    void predictTopCategories_shouldReturnCategoriesSortedByPrediction() {
        LocalDate monday = LocalDate.of(2026, 8, 3);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", monday, new BigDecimal("1000")},
                        new Object[]{"Electronics", monday.plusWeeks(1), new BigDecimal("2000")},
                        new Object[]{"Electronics", monday.plusWeeks(2), new BigDecimal("3000")},
                        new Object[]{"Electronics", monday.plusWeeks(3), new BigDecimal("4000")},

                        new Object[]{"Furniture", monday, new BigDecimal("5000")},
                        new Object[]{"Furniture", monday.plusWeeks(1), new BigDecimal("5000")},
                        new Object[]{"Furniture", monday.plusWeeks(2), new BigDecimal("5000")},
                        new Object[]{"Furniture", monday.plusWeeks(3), new BigDecimal("5000")}
                ));

        var result = predictionService.predictTopCategories(Period.WEEK);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Furniture has the larger moving average
        assertEquals("Electronics", result.get(0).category());
        assertEquals("Furniture", result.get(1).category());
    }

    @Test
    void runBacktest_shouldCalculatePredictionsAndAbsoluteErrors() {
        LocalDate firstMonday = LocalDate.of(2026, 8, 3);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        // Aug 3
                        new Object[]{"Electronics", firstMonday, new BigDecimal("1000")},

                        // Aug 10
                        new Object[]{"Electronics", firstMonday.plusWeeks(1), new BigDecimal("2000")},

                        // Aug 17
                        new Object[]{"Electronics", firstMonday.plusWeeks(2), new BigDecimal("3000")},

                        // Aug 24
                        new Object[]{"Electronics", firstMonday.plusWeeks(3), new BigDecimal("4000")},

                        // Aug 31
                        new Object[]{"Electronics", firstMonday.plusWeeks(4), new BigDecimal("5000")},

                        // Sep 7
                        new Object[]{"Electronics", firstMonday.plusWeeks(5), new BigDecimal("6000")},

                        // Sep 14
                        new Object[]{"Electronics", firstMonday.plusWeeks(6), new BigDecimal("7000")}
                ));

        var result = predictionService.runBacktest();

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Sep 7 prediction = average of previous 4 weeks:
        // Aug 10 + Aug 17 + Aug 24 + Aug 31
        // = (2000 + 3000 + 4000 + 5000) / 4 = 3500
        BacktestResult sep7 = result.stream()
                .filter(r -> r.predictionWeek().equals(LocalDate.of(2026, 9, 7)))
                .findFirst()
                .orElseThrow();

        assertEquals("Electronics", sep7.category());
        assertEquals(new BigDecimal("3500.00"), sep7.predictedSales());
        assertEquals(new BigDecimal("6000"), sep7.actualSales());
        assertEquals(new BigDecimal("2500.00"), sep7.absoluteError());

        // Sep 14 prediction = average of Aug 17, Aug 24, Aug 31 and Sep 7
        // = (3000 + 4000 + 5000 + 6000) / 4 = 4500
        BacktestResult sep14 = result.stream()
                .filter(r -> r.predictionWeek().equals(LocalDate.of(2026, 9, 14)))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("4500.00"), sep14.predictedSales());
        assertEquals(new BigDecimal("7000"), sep14.actualSales());
        assertEquals(new BigDecimal("2500.00"), sep14.absoluteError());
    }

    @Test
    void runXgboostBacktest_shouldGenerateResults() {
        LocalDate firstMonday = LocalDate.of(2023, 9, 4);

        List<Object[]> rows = new java.util.ArrayList<>();

        // Create more than 5 weeks of historical data.
        for (int i = 0; i < 160; i++) {
            rows.add(new Object[]{
                    "Electronics",
                    firstMonday.plusWeeks(i),
                    new BigDecimal(1000 + (i * 100))
            });
        }

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(rows);

        // The XGBoost backtest makes an additional repository call
        // using findSalesByCategoryAndDateBetween().
        when(saleRepository.findSalesByCategoryAndDateBetween(any(), any()))
                .thenReturn(rows);

        CategoryPredictionResponse mlResponse =
                new CategoryPredictionResponse(
                        "Electronics",
                        new BigDecimal("15000"),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(mlServiceClient.forecast(any(ForecastRequest.class)))
                .thenReturn(mlResponse);

        var result = predictionService.runXgboostBacktest();

        assertNotNull(result);

        verify(mlServiceClient, atLeastOnce())
                .forecast(any(ForecastRequest.class));
    }

    @Test
    void compareBacktests_shouldCombineMatchingResults() {
        LocalDate predictionWeek = LocalDate.of(2026, 9, 7);

        when(saleRepository.findSalesByCategoryAndDate(any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", LocalDate.of(2026, 8, 10), new BigDecimal("1000")},
                        new Object[]{"Electronics", LocalDate.of(2026, 8, 17), new BigDecimal("2000")},
                        new Object[]{"Electronics", LocalDate.of(2026, 8, 24), new BigDecimal("3000")},
                        new Object[]{"Electronics", LocalDate.of(2026, 8, 31), new BigDecimal("4000")},
                        new Object[]{"Electronics", predictionWeek, new BigDecimal("5000")},
                        new Object[]{"Electronics", predictionWeek.plusWeeks(1), new BigDecimal("6000")}
                ));

        when(saleRepository.findSalesByCategoryAndDateBetween(any(), any()))
                .thenReturn(List.of(
                        new Object[]{"Electronics", LocalDate.of(2023, 9, 4), new BigDecimal("1000")},
                        new Object[]{"Electronics", LocalDate.of(2023, 9, 11), new BigDecimal("1200")},
                        new Object[]{"Electronics", LocalDate.of(2023, 9, 18), new BigDecimal("1400")},
                        new Object[]{"Electronics", LocalDate.of(2023, 9, 25), new BigDecimal("1600")},
                        new Object[]{"Electronics", LocalDate.of(2023, 10, 2), new BigDecimal("1800")},
                        new Object[]{"Electronics", LocalDate.of(2023, 10, 9), new BigDecimal("2000")}
                ));

        when(mlServiceClient.forecast(any(ForecastRequest.class)))
                .thenReturn(new CategoryPredictionResponse(
                        "Electronics",
                        new BigDecimal("2500"),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                ));

        var result = predictionService.compareBacktests();

        assertNotNull(result);

        assertFalse(result.isEmpty());

        var comparison = result.stream()
                .filter(r -> r.predictionWeek().equals(predictionWeek))
                .findFirst()
                .orElseThrow();

        assertEquals("Electronics", comparison.category());
        assertEquals(predictionWeek, comparison.predictionWeek());

        assertEquals(new BigDecimal("2500"), comparison.xgboostPrediction());

        verify(mlServiceClient, atLeastOnce())
                .forecast(any(ForecastRequest.class));
    }

    @Test
    void forecastCategory_shouldCalculateProductForecasts() {

        LocalDate startDate = LocalDate.now().minusDays(56);

        // 5+ weeks of category history
        List<Object[]> dailySales = List.of(
                new Object[]{"Electronics", startDate, new BigDecimal("1000")},
                new Object[]{"Electronics", startDate.plusDays(7), new BigDecimal("1000")},
                new Object[]{"Electronics", startDate.plusDays(14), new BigDecimal("1000")},
                new Object[]{"Electronics", startDate.plusDays(21), new BigDecimal("1000")},
                new Object[]{"Electronics", startDate.plusDays(28), new BigDecimal("1000")}
        );

        when(saleRepository.findSalesByCategoryAndDate(startDate))
                .thenReturn(dailySales);

        when(saleRepository.findProductSalesRanking(
                any(),
                any(),
                eq("Electronics")))
                .thenReturn(List.of(
                        new Object[]{
                                1L,
                                "Laptop",
                                "Electronics",
                                new BigDecimal("6000"),
                                10L
                        },
                        new Object[]{
                                2L,
                                "Phone",
                                "Electronics",
                                new BigDecimal("4000"),
                                20L
                        }
                ));

        CategoryPredictionResponse mlResponse =
                new CategoryPredictionResponse(
                        "Electronics",
                        new BigDecimal("20000"),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(mlServiceClient.forecast(any()))
                .thenReturn(mlResponse);

        CategoryPredictionResponse result =
                predictionService.forecastCategory(
                        1L,
                        "Electronics",
                        Period.WEEK
                );

        assertEquals(2, result.productForecasts().size());

        assertEquals(
                new BigDecimal("0.600000"),
                result.productForecasts().get(0).historicalPercentage()
        );

        assertEquals(
                new BigDecimal("12000.00"),
                result.productForecasts().get(0).forecastAmount()
        );

        assertEquals(
                new BigDecimal("0.400000"),
                result.productForecasts().get(1).historicalPercentage()
        );

        assertEquals(
                new BigDecimal("8000.00"),
                result.productForecasts().get(1).forecastAmount()
        );
    }

}