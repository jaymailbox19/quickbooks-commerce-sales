package com.quickbooks.sales.repository;

import com.quickbooks.sales.model.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMembershipRepository
        extends JpaRepository<TenantMembership, Long> {

    List<TenantMembership> findAllByUser_IdAndUser_EnabledTrue(Long userId);

    Optional<TenantMembership> findByUser_IdAndTenant_IdAndUser_EnabledTrue(
            Long userId,
            Long tenantId
    );
}
