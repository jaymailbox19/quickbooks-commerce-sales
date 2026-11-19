package com.quickbooks.sales.dto;

import jakarta.validation.constraints.NotNull;

public record SelectWorkspaceRequest(@NotNull Long tenantId) {
}
