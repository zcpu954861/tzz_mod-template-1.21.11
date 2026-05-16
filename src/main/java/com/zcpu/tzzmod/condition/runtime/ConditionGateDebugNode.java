package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationResult;
import java.util.List;

public record ConditionGateDebugNode(
        boolean matched,
        boolean skipped,
        boolean error,
        String conditionId,
        String nodeId,
        String type,
        String label,
        String reasonCode,
        String failureReason,
        String message,
        String debugSummary,
        int evaluatedNodeCount,
        long durationNanos,
        String contextSummary,
        List<ConditionGateDebugNode> childResults
) {
    public ConditionGateDebugNode {
        conditionId = safe(conditionId);
        nodeId = safe(nodeId);
        type = safe(type);
        label = safe(label);
        reasonCode = safe(reasonCode);
        failureReason = safe(failureReason);
        message = safe(message);
        debugSummary = safe(debugSummary);
        contextSummary = safe(contextSummary);
        evaluatedNodeCount = Math.max(0, evaluatedNodeCount);
        durationNanos = Math.max(0L, durationNanos);
        childResults = childResults == null ? List.of() : List.copyOf(childResults);
    }

    public static ConditionGateDebugNode from(ConditionEvaluationResult result) {
        if (result == null) {
            return null;
        }
        return new ConditionGateDebugNode(
                result.matched(),
                result.skipped(),
                result.error(),
                result.conditionId(),
                result.nodeId(),
                result.type(),
                result.label(),
                result.reasonCode(),
                result.failureReason(),
                result.message(),
                result.debugSummary(),
                result.evaluatedNodeCount(),
                result.durationNanos(),
                result.contextSummary(),
                result.childResults().stream()
                        .map(ConditionGateDebugNode::from)
                        .toList()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
