package com.adp.benefits.carrier.model.cpm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum State {
    SUCCESS("SUCCESS"),
    PAUSE("PAUSE"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    VOLUME_ERROR("VOLUME_ERROR"),
    CRITICAL_ERROR("CRITICAL_ERROR"),
    CANCEL("CANCEL"),
    ONE_CG_SEND("ONE_CG_SEND"),
    ONE_CG_ACKNOWLEDGEMENT("ONE_CG_ACKNOWLEDGEMENT"),
    CARRIER_RECEIVED("CARRIER_RECEIVED"),
    CARRIER_ERROR("CARRIER_ERROR"),
    CARRIER_SUCCESS("CARRIER_SUCCESS"),
    ONE_CG_ERROR("ONE_CG_ERROR");

    private final String value;

    State(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static State fromValue(String value) {
        for (State state : State.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state: " + value);
    }
}
