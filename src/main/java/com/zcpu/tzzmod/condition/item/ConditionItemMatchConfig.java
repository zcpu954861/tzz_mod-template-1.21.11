package com.zcpu.tzzmod.condition.item;

public record ConditionItemMatchConfig(
        String itemId,
        ConditionItemCompareOperator countOperator,
        int count
) {
    public ConditionItemMatchConfig {
        itemId = ConditionItemStackSnapshot.normalizeItemId(itemId);
        countOperator = countOperator == null ? ConditionItemCompareOperator.GTE : countOperator;
        count = Math.max(0, count);
    }

    public boolean empty() {
        return itemId.isBlank();
    }
}
