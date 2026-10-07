package com.quickbooks.sales.exception;

public class MlServiceBadRequestException extends RuntimeException {

    public MlServiceBadRequestException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}