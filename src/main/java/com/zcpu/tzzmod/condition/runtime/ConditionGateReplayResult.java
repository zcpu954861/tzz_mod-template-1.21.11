package com.zcpu.tzzmod.condition.runtime;

import java.util.List;
import java.util.Map;

public record ConditionGateReplayResult(
        boolean success,
        boolean readOnly,
        boolean noSideEffects,
        boolean noLiveWorldRead,
        String recordId,
        String conditionGroupId,
        String originalResult,
        String replayResult,
        boolean originalMatched,
        boolean replayMatched,
        boolean resultConsistent,
        String code,
        String failureReason,
        String debugSummary,
        int evaluatedCount,
        long durationNanos,
        String definitionFingerprint,
        String currentDefinitionFingerprint,
        boolean fingerprintChanged,
        List<String> warnings,
        Map<String, String> contextSummary,
        ConditionGateDebugNode debugTree
) {
    public ConditionGateReplayResult {
        recordId = safe(recordId);
        conditionGroupId = safe(conditionGroupId);
        originalResult = safe(originalResult);
        replayResult = safe(replayResult);
        code = safe(code);
        failureReason = safe(failureReason);
        debugSummary = safe(debugSummary);
        definitionFingerprint = safe(definitionFingerprint);
        currentDefinitionFingerprint = safe(currentDefinitionFingerprint);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        contextSummary = contextSummary == null ? Map.of() : Map.copyOf(contextSummary);
        evaluatedCount = Math.max(0, evaluatedCount);
        durationNanos = Math.max(0L, durationNanos);
    }

    public static ConditionGateReplayResult failed(
            String recordId,
            String conditionGroupId,
            String code,
            String message,
            List<String> warnings
    ) {
        return new ConditionGateReplayResult(
                false,
                true,
                true,
                true,
                recordId,
                conditionGroupId,
                "",
                "ERROR",
                false,
                false,
                false,
                code,
                message,
                message,
                0,
                0L,
                "",
                "",
                false,
                warnings,
                Map.of(),
                null
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
