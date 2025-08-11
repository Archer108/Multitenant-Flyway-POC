package com.adp.benefits.carrier.exceptions;

public class ShortEncryptedDataException extends Exception {
    public ShortEncryptedDataException(String message) {
        super(message);
    }

    public ShortEncryptedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
