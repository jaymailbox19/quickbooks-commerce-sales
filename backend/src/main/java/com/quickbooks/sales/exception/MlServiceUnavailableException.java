package com.quickbooks.sales.exception;

public class MlServiceUnavailableException extends RuntimeException {

    public MlServiceUnavailableException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}