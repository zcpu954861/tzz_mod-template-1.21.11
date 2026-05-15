package com.zcpu.tzzmod.condition.state;

import java.util.Locale;
import java.util.Optional;

public enum StateVariableCompareOperator {
    EQ("eq", "="),
    NE("ne", "!="),
    GT("gt", ">"),
    GTE("gte", ">="),
    LT("lt", "<"),
    LTE("lte", "<=");

    private final String id;
    private final String symbol;

    StateVariableCompareOperator(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public String id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }

    public boolean test(long actual, long expected) {
        return switch (this) {
            case EQ -> actual == expected;
            case NE -> actual != expected;
            case GT -> actual > expected;
            case GTE -> actual >= expected;
            case LT -> actual < expected;
            case LTE -> actual <= expected;
        };
    }

    public static Optional<StateVariableCompareOperator> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (StateVariableCompareOperator operator : values()) {
            if (operator.id.equals(normalized) || operator.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(operator);
            }
        }
        return Optional.empty();
    }
}
