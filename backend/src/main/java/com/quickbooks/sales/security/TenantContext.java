package com.quickbooks.sales.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class TenantContext {
    private TenantContext() {
    }

    public static Long requireTenantId(Jwt jwt) {
        if (jwt == null || !"workspace".equals(jwt.getClaimAsString("token_type"))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An active business workspace is required"
            );
        }

        Object claim = jwt.getClaim("tenant_id");
        if (!(claim instanceof Number tenantId) || tenantId.longValue() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The active workspace claim is missing or invalid"
            );
        }
        return tenantId.longValue();
    }
}
