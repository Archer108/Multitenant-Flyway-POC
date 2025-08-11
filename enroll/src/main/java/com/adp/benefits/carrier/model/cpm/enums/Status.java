package com.adp.benefits.carrier.model.cpm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    CONTINUE("CONTINUE"),
    ASYNC("ASYNC"),
    STOP("STOP"),
    WARNING("WARNING"),
    SKIPPED("SKIPPED"),
    IN_PROGRESS("IN_PROGRESS"),
    TIMEOUT("TIMEOUT"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Status fromValue(String value) {
        for (Status status : Status.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
