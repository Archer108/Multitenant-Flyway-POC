package com.adp.benefits.carrier.model.enrollment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public enum Timeframe {
    TODAY("TODAY"),
    ONE_WEEK("ONE_WEEK"),
    LAST_30_DAYS("LAST_30_DAYS"),
    LAST_6_MONTHS("LAST_6_MONTHS"),
    CUSTOM_DATE_RANGE("CUSTOM_DATE_RANGE");

    private final String value;

    Timeframe(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Timeframe fromValue(String value) {
        for (Timeframe tf : values()) {
            if (tf.value.equalsIgnoreCase(value)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe: " + value);
    }

    public Date toCutoffDate() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        LocalDate cutoff;

        switch (this) {
            case TODAY -> cutoff = now;
            case ONE_WEEK -> cutoff = now.minusWeeks(1);
            case LAST_30_DAYS -> cutoff = now.minusDays(30);
            case LAST_6_MONTHS -> cutoff = now.minusMonths(6);
            case CUSTOM_DATE_RANGE -> cutoff = null;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        }

        return cutoff == null
                ? null
                : Date.from(cutoff.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
