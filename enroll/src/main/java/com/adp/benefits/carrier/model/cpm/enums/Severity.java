package com.adp.benefits.carrier.model.cpm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
    INFO("INFO"), // Informational message only
    WARNING("WARNING"), // Potential issue that doesn't affect functionality
    ERROR("ERROR"), // Issue that affects specific functionality
    CRITICAL("CRITICAL"); // Severe issue affecting core functionality

    private final String value;

    Severity(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Severity fromValue(String value) {
        for (Severity severity : Severity.values()) {
            if (severity.value.equalsIgnoreCase(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown severity: " + value);
    }
}
