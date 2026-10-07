package com.quickbooks.sales;

import com.quickbooks.sales.dto.CategorySalesResponse;
import com.quickbooks.sales.dto.DailyCategorySalesResponse;
import com.quickbooks.sales.dto.ProductSalesRanking;
import com.quickbooks.sales.repository.SaleRepository;
import com.quickbooks.sales.service.SalesService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class SalesServiceApplicationTests {
    private final SaleRepository saleRepository =
            mock(SaleRepository.class);

    private final SalesService salesService =
            new SalesService(saleRepository);

    @Test
    void contextLoads() {
    }

    @Test
    void getSalesByCategory_shouldGroupAndSortSalesDescending() {

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(saleRepository.findSalesByCategoryAndDateBetween(
                startDate, endDate))
                .thenReturn(List.of(
                        new Object[]{"Electronics", LocalDate.of(2026, 1, 5),
                                new BigDecimal("100.00")},
                        new Object[]{"Books", LocalDate.of(2026, 1, 10),
                                new BigDecimal("50.00")},
                        new Object[]{"Electronics", LocalDate.of(2026, 1, 15),
                                new BigDecimal("200.00")}
                ));

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        startDate, endDate, null);

        assertEquals(2, result.size());

        assertEquals("Electronics", result.get(0).category());
        assertEquals(new BigDecimal("300.00"),
                result.get(0).totalSales());

        assertEquals("Books", result.get(1).category());
        assertEquals(new BigDecimal("50.00"),
                result.get(1).totalSales());

        verify(saleRepository).findSalesByCategoryAndDateBetween(
                startDate, endDate);
    }

    @Test
    void getSalesByCategory_shouldRejectStartDateAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions
                .assertThrows(
                        IllegalArgumentException.class,
                        () -> salesService.getSalesByCategory(
                                startDate, endDate, null)
                );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getProductSalesRanking_shouldMapRowsAndAssignRanks() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        List<Object[]> rows = List.of(
                new Object[]{
                        101L,
                        "Laptop",
                        "Electronics",
                        new BigDecimal("2500.00"),
                        5L
                },
                new Object[]{
                        102L,
                        "Monitor",
                        "Electronics",
                        new BigDecimal("1200.00"),
                        4L
                }
        );

        when(saleRepository.findProductSalesRanking(
                startDate,
                endDate.plusDays(1),
                "Electronics"
        )).thenReturn(rows);

        List<ProductSalesRanking> result =
                salesService.getProductSalesRanking(
                        startDate,
                        endDate,
                        "Electronics"
                );

        assertEquals(2, result.size());

        assertEquals(101L, result.get(0).productId());
        assertEquals("Laptop", result.get(0).productName());
        assertEquals("Electronics", result.get(0).categoryName());
        assertEquals(new BigDecimal("2500.00"), result.get(0).totalSales());
        assertEquals(5L, result.get(0).totalQuantity());
        assertEquals(1L, result.get(0).rank());

        assertEquals(102L, result.get(1).productId());
        assertEquals("Monitor", result.get(1).productName());
        assertEquals(2L, result.get(1).rank());

        verify(saleRepository).findProductSalesRanking(
                startDate,
                endDate.plusDays(1),
                "Electronics"
        );
    }

    @Test
    void getProductSalesRanking_shouldRejectStartDateAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getProductSalesRanking(
                        startDate,
                        endDate,
                        "Electronics"
                )
        );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getProductSalesRanking_shouldRejectNullDates() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getProductSalesRanking(
                        null,
                        LocalDate.of(2026, 1, 31),
                        "Electronics"
                )
        );

        assertEquals(
                "Start date and end date are required",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getDailySalesByCategory_shouldFilterCategoryAndMapRows() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 3);

        List<Object[]> rows = List.of(
                new Object[]{
                        "Electronics",
                        LocalDate.of(2026, 1, 1),
                        new BigDecimal("1200.00")
                },
                new Object[]{
                        "Clothing",
                        LocalDate.of(2026, 1, 2),
                        new BigDecimal("500.00")
                },
                new Object[]{
                        "Electronics",
                        LocalDate.of(2026, 1, 3),
                        new BigDecimal("1800.00")
                }
        );

        when(saleRepository.findSalesByCategoryAndDateBetween(
                startDate, endDate
        )).thenReturn(rows);

        List<DailyCategorySalesResponse> result =
                salesService.getDailySalesByCategory(
                        "Electronics", startDate, endDate
                );

        assertEquals(2, result.size());

        assertEquals("Electronics", result.get(0).category());
        assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
        assertEquals(new BigDecimal("1200.00"), result.get(0).amount());

        assertEquals("Electronics", result.get(1).category());
        assertEquals(LocalDate.of(2026, 1, 3), result.get(1).date());
        assertEquals(new BigDecimal("1800.00"), result.get(1).amount());

        verify(saleRepository).findSalesByCategoryAndDateBetween(
                startDate, endDate
        );
    }

    @Test
    void getDailySalesByCategory_shouldRejectBlankCategory() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getDailySalesByCategory(
                        " ",
                        startDate,
                        endDate
                )
        );

        assertEquals("Category is required", exception.getMessage());
        verifyNoInteractions(saleRepository);
    }

    @Test
    void getDailySalesByCategory_shouldRejectNullDates() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getDailySalesByCategory(
                        "Electronics",
                        null,
                        LocalDate.of(2026, 1, 31)
                )
        );

        assertEquals(
                "Start date and end date are required",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }
    @Test
    void getSalesByCategory_shouldReturnSalesForWeekPeriod() {

        when(saleRepository.findSalesByCategory(any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[]{"Electronics", new BigDecimal("1000.00")},
                        new Object[]{"Books", new BigDecimal("500.00")}
                ));

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        com.quickbooks.sales.model.Period.WEEK);

        assertEquals(2, result.size());

        assertEquals("Electronics", result.get(0).category());
        assertEquals(
                new BigDecimal("1000.00"),
                result.get(0).totalSales()
        );

        assertEquals("Books", result.get(1).category());
        assertEquals(
                new BigDecimal("500.00"),
                result.get(1).totalSales()
        );

        verify(saleRepository).findSalesByCategory(
                any(LocalDate.class)
        );
    }

    @Test
    void getSalesByCategory_shouldReturnSalesForMonthPeriod() {
        List<Object[]> rows = Collections.singletonList(
                new Object[]{
                        "Electronics",
                        new BigDecimal("2000.00")
                }
        );
        when(saleRepository.findSalesByCategory(any(LocalDate.class)))
                .thenReturn(rows);

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        com.quickbooks.sales.model.Period.MONTH);

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).category());
        assertEquals(
                new BigDecimal("2000.00"),
                result.get(0).totalSales()
        );

        verify(saleRepository).findSalesByCategory(
                any(LocalDate.class)
        );
    }

    @Test
    void getSalesByCategory_shouldReturnSalesForYearPeriod() {
        List<Object[]> rows = Collections.singletonList(new Object[]{"Electronics",
                new BigDecimal("50000.00")});
        when(saleRepository.findSalesByCategory(any(LocalDate.class)))
                .thenReturn(rows);

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        com.quickbooks.sales.model.Period.YEAR);

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).category());
        assertEquals(
                new BigDecimal("50000.00"),
                result.get(0).totalSales()
        );

        verify(saleRepository).findSalesByCategory(
                any(LocalDate.class)
        );
    }

    @Test
    void getSalesByCategory_shouldSupportDateRangeOverload() {

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(saleRepository.findSalesByCategoryAndDateBetween(
                startDate, endDate))
                .thenReturn(List.of(
                        new Object[]{
                                "Electronics",
                                LocalDate.of(2026, 1, 10),
                                new BigDecimal("1500.00")
                        },
                        new Object[]{
                                "Electronics",
                                LocalDate.of(2026, 1, 20),
                                new BigDecimal("500.00")
                        }
                ));

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        startDate, endDate);

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).category());
        assertEquals(
                new BigDecimal("2000.00"),
                result.get(0).totalSales()
        );

        verify(saleRepository).findSalesByCategoryAndDateBetween(
                startDate, endDate);
    }

    @Test
    void getSalesByCategory_shouldRejectNullStartDate() {

        LocalDate endDate = LocalDate.of(2026, 1, 31);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getSalesByCategory(
                        null, endDate, null)
        );

        assertEquals(
                "Start date and end date are required",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getSalesByCategory_shouldRejectNullEndDate() {

        LocalDate startDate = LocalDate.of(2026, 1, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getSalesByCategory(
                        startDate, null, null)
        );

        assertEquals(
                "Start date and end date are required",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getSalesByCategory_shouldReturnAllCategoriesForAllCategory() {

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(saleRepository.findSalesByCategoryAndDateBetween(
                startDate, endDate))
                .thenReturn(List.of(
                        new Object[]{
                                "Electronics",
                                LocalDate.of(2026, 1, 5),
                                new BigDecimal("3000.00")
                        },
                        new Object[]{
                                "Books",
                                LocalDate.of(2026, 1, 10),
                                new BigDecimal("5000.00")
                        }
                ));

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        startDate, endDate, "ALL");

        assertEquals(2, result.size());

        // Results are sorted descending by total sales
        assertEquals("Books", result.get(0).category());
        assertEquals("Electronics", result.get(1).category());
    }

    @Test
    void getSalesByCategory_shouldReturnAllCategoriesForBlankCategory() {

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(saleRepository.findSalesByCategoryAndDateBetween(
                startDate, endDate))
                .thenReturn(List.of(
                        new Object[]{
                                "Electronics",
                                LocalDate.of(2026, 1, 5),
                                new BigDecimal("3000.00")
                        },
                        new Object[]{
                                "Books",
                                LocalDate.of(2026, 1, 10),
                                new BigDecimal("5000.00")
                        }
                ));

        List<CategorySalesResponse> result =
                salesService.getSalesByCategory(
                        startDate, endDate, " ");

        assertEquals(2, result.size());
    }
    @Test
    void getDailySalesByCategory_shouldRejectStartDateAfterEndDate() {

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getDailySalesByCategory(
                        "Electronics",
                        startDate,
                        endDate)
        );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }

    @Test
    void getDailySalesByCategory_shouldRejectNullEndDate() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getDailySalesByCategory(
                        "Electronics",
                        LocalDate.of(2026, 1, 1),
                        null)
        );

        assertEquals(
                "Start date and end date are required",
                exception.getMessage()
        );

        verifyNoInteractions(saleRepository);
    }
}