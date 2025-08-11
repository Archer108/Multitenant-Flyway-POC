package com.adp.benefits.carrier.exceptions;

import lombok.Getter;

/** Thrown when the evaluation service encounters an unrecoverable error. */
@Getter
public class ServiceProcessingException extends RuntimeException {

    private final String correlationId;

    public ServiceProcessingException(String correlationId, String message, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
    }
}
