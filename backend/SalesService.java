package com.quickbooks.sales.service;

import com.quickbooks.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesService {

    private final SaleRepository saleRepository;

    public SalesService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public List<Object[]> getSalesByCategory() {
        return saleRepository.findSalesByCategory();
    }
}
