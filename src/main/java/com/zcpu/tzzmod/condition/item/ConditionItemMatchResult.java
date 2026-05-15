package com.zcpu.tzzmod.condition.item;

public record ConditionItemMatchResult(
        boolean matched,
        String reasonCode,
        String message
) {
    public static ConditionItemMatchResult matched(String message) {
        return new ConditionItemMatchResult(true, "item_match", message);
    }

    public static ConditionItemMatchResult failed(String reasonCode, String message) {
        return new ConditionItemMatchResult(false, reasonCode, message);
    }
}
