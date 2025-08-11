package com.adp.benefits.carrier.exceptions;

public class DatabaseServiceOperationException extends RuntimeException {

    public DatabaseServiceOperationException(String message) {
        super(message);
    }

    public DatabaseServiceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
