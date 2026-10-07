package com.quickbooks.sales.repository;

import com.quickbooks.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            SELECT c.name, SUM(s.amount)
            FROM Sale s
            JOIN s.product p
            JOIN p.category c
            WHERE s.saleDate >= :startDate
            GROUP BY c.name
            ORDER BY SUM(s.amount) DESC
            """)
    List<Object[]> findSalesByCategory(
            @Param("startDate") LocalDate startDate);

    @Query("""
            SELECT c.name, s.saleDate, SUM(s.amount)
            FROM Sale s
            JOIN s.product p
            JOIN p.category c
            WHERE s.saleDate >= :startDate
            GROUP BY c.name, s.saleDate
            ORDER BY c.name, s.saleDate
            """)
    List<Object[]> findSalesByCategoryAndDate(
            @Param("startDate") LocalDate startDate);

    @Query("""
            SELECT c.name, s.saleDate, SUM(s.amount)
            FROM Sale s
            JOIN s.product p
            JOIN p.category c
            WHERE s.saleDate >= :startDate
              AND s.saleDate < :endDate
            GROUP BY c.name, s.saleDate
            ORDER BY c.name, s.saleDate
            """)
    List<Object[]> findSalesByCategoryAndDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
    SELECT p.id, p.name, c.name, SUM(s.amount), SUM(s.quantity)
    FROM Sale s
    JOIN s.product p
    JOIN p.category c
    WHERE s.saleDate >= :startDate
      AND s.saleDate < :endDate
      AND (:category IS NULL OR c.name = :category)
    GROUP BY p.id, p.name, c.name
    ORDER BY SUM(s.amount) DESC
    """)
    List<Object[]> findProductSalesRanking(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );
}

