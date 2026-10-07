package com.quickbooks.sales;

import com.quickbooks.sales.client.MlServiceClient;
import com.quickbooks.sales.dto.CategoryPredictionResponse;
import com.quickbooks.sales.dto.ForecastRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MlServiceClientIntegrationTest {
    @Autowired
    private MlServiceClient mlServiceClient;

    @Test
    void testMlForecast() {

        ForecastRequest request = new ForecastRequest(1L,
                "Electronics",
                List.of(
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 6, 15),
                                new BigDecimal("9000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 6, 22),
                                new BigDecimal("11000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 6, 29),
                                new BigDecimal("10000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 7, 6),
                                new BigDecimal("13000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 7, 13),
                                new BigDecimal("12000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 7, 20),
                                new BigDecimal("15000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 7, 27),
                                new BigDecimal("14000")),
                        new ForecastRequest.WeeklySales(
                                LocalDate.of(2026, 8, 3),
                                new BigDecimal("16000"))
                ),
                1
        );

        CategoryPredictionResponse response =
                mlServiceClient.forecast(request);
        assertNotNull(response);
        assertEquals("Electronics", response.category());
        assertNotNull(response.predictedSales());
        assertTrue(response.predictedSales().compareTo(BigDecimal.ZERO) >= 0);

        System.out.println(
                "ML response: "
                        + response.category()
                        + " = "
                        + response.predictedSales()
        );
    }
}
