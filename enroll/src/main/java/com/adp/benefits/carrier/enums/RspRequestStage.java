package com.adp.benefits.carrier.enums;

public enum RspRequestStage {
    CONFIG,
    TRANSFORM,
    VALIDATE,
    SEND,
    SYNCRSP,
    ASYNCRSP;

    public boolean equalsIgnoreCase(String value) {
        return this.name().equalsIgnoreCase(value);
    }

    public static RspRequestStage fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return RspRequestStage.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
