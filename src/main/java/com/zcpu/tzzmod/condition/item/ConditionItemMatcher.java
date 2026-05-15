package com.zcpu.tzzmod.condition.item;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ConditionItemMatcher {
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private ConditionItemMatcher() {
    }

    public static boolean validItemId(String itemId) {
        String normalized = ConditionItemStackSnapshot.normalizeItemId(itemId);
        return !normalized.isBlank()
                && !"minecraft:air".equals(normalized)
                && ITEM_ID_PATTERN.matcher(normalized).matches();
    }

    public static boolean sameItem(ConditionItemStackSnapshot snapshot, String itemId) {
        if (snapshot == null || snapshot.isEmpty()) {
            return false;
        }
        return snapshot.itemId().equals(ConditionItemStackSnapshot.normalizeItemId(itemId));
    }

    public static ConditionItemMatchResult matchStack(ConditionItemStackSnapshot snapshot, ConditionItemMatchConfig matcher) {
        if (matcher == null || matcher.empty()) {
            return ConditionItemMatchResult.failed("item_matcher_empty", "物品匹配器为空。");
        }
        if (snapshot == null || snapshot.isEmpty()) {
            return ConditionItemMatchResult.failed("item_stack_empty", "物品快照为空。");
        }
        if (!sameItem(snapshot, matcher.itemId())) {
            return ConditionItemMatchResult.failed(
                    "item_id_mismatch",
                    "物品 ID 不匹配：期望 " + matcher.itemId() + "，实际 " + snapshot.itemId() + "。"
            );
        }
        boolean countMatched = matcher.countOperator().test(snapshot.count(), matcher.count());
        if (!countMatched) {
            return ConditionItemMatchResult.failed(
                    "item_count_mismatch",
                    "物品数量不满足：当前 " + snapshot.count() + "，要求 " + matcher.countOperator().symbol() + " " + matcher.count() + "。"
            );
        }
        return ConditionItemMatchResult.matched("物品匹配：" + snapshot.summary() + "。");
    }

    public static ConditionItemMatchResult matchAggregateCount(int actualCount, ConditionItemMatchConfig matcher, String subject) {
        if (matcher == null || matcher.empty()) {
            return ConditionItemMatchResult.failed("item_matcher_empty", "物品匹配器为空。");
        }
        boolean countMatched = matcher.countOperator().test(actualCount, matcher.count());
        String safeSubject = subject == null || subject.isBlank() ? "物品总数" : subject.trim();
        if (!countMatched) {
            return ConditionItemMatchResult.failed(
                    "item_count_mismatch",
                    safeSubject + "不满足：当前 " + actualCount + "，要求 " + matcher.countOperator().symbol() + " " + matcher.count() + "。"
            );
        }
        return ConditionItemMatchResult.matched(safeSubject + "满足：当前 " + actualCount + "，要求 " + matcher.countOperator().symbol() + " " + matcher.count() + "。");
    }

    public static String normalizeOperator(String operator) {
        return operator == null ? "" : operator.trim().toLowerCase(Locale.ROOT);
    }
}
