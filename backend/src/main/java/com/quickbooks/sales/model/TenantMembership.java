package com.quickbooks.sales.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_memberships",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_membership_user_tenant",
        columnNames = {"user_id", "tenant_id"}
    )
)
public class TenantMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantRole role;

    protected TenantMembership() {
    }

    public TenantMembership(AppUser user, Tenant tenant, TenantRole role) {
        this.user = user;
        this.tenant = tenant;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TenantRole getRole() {
        return role;
    }
}
