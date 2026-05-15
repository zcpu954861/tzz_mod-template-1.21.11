package com.zcpu.tzzmod.condition.runtime;

public record ConditionGateResult(
        boolean allowed,
        boolean skipped,
        String conditionGroupId,
        String failureReason,
        String debugSummary,
        int evaluatedCount,
        long durationNanos,
        String code
) {
    public ConditionGateResult {
        conditionGroupId = safe(conditionGroupId);
        failureReason = safe(failureReason);
        debugSummary = safe(debugSummary);
        code = safe(code);
        evaluatedCount = Math.max(0, evaluatedCount);
        durationNanos = Math.max(0L, durationNanos);
    }

    public static ConditionGateResult skippedResult() {
        return new ConditionGateResult(true, true, "", "", "未配置条件组，跳过条件 gate。", 0, 0L, "condition_gate_skipped");
    }

    public static ConditionGateResult allowed(String conditionGroupId, String debugSummary, int evaluatedCount, long durationNanos) {
        return new ConditionGateResult(true, false, conditionGroupId, "", debugSummary, evaluatedCount, durationNanos, "condition_gate_allowed");
    }

    public static ConditionGateResult blocked(String conditionGroupId, String code, String reason, String debugSummary, int evaluatedCount, long durationNanos) {
        return new ConditionGateResult(false, false, conditionGroupId, reason, debugSummary, evaluatedCount, durationNanos, code);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
