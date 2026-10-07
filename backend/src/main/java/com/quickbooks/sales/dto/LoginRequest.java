package com.quickbooks.sales.dto;

public record LoginRequest(
        String username,
        String password
) {
}
