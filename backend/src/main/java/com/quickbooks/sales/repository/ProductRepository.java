package com.quickbooks.sales.repository;

import com.quickbooks.sales.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
