package com.zcpu.tzzmod.condition.item;

import java.util.Locale;
import java.util.Optional;

public enum ConditionItemCompareOperator {
    EQ("eq", "="),
    NE("ne", "!="),
    GT("gt", ">"),
    GTE("gte", ">="),
    LT("lt", "<"),
    LTE("lte", "<=");

    private final String id;
    private final String symbol;

    ConditionItemCompareOperator(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public String id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }

    public boolean test(int actual, int expected) {
        return switch (this) {
            case EQ -> actual == expected;
            case NE -> actual != expected;
            case GT -> actual > expected;
            case GTE -> actual >= expected;
            case LT -> actual < expected;
            case LTE -> actual <= expected;
        };
    }

    public static Optional<ConditionItemCompareOperator> parse(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (ConditionItemCompareOperator operator : values()) {
            if (operator.id.equals(value)) {
                return Optional.of(operator);
            }
        }
        return Optional.empty();
    }
}
