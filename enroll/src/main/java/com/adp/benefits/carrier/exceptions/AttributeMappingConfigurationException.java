package com.adp.benefits.carrier.exceptions;

public class AttributeMappingConfigurationException extends RuntimeException {

    public AttributeMappingConfigurationException(String message) {
        super(message);
    }

    public AttributeMappingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
