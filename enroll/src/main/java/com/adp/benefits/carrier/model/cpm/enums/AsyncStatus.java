package com.adp.benefits.carrier.model.cpm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AsyncStatus {
    INITIATED("INITIATED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    TIMEOUT("TIMEOUT");

    private final String value;

    AsyncStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AsyncStatus fromValue(String value) {
        for (AsyncStatus status : AsyncStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown async status: " + value);
    }
}
