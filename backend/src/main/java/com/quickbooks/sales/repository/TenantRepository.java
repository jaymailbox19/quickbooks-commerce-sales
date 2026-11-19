package com.quickbooks.sales.repository;

import com.quickbooks.sales.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
