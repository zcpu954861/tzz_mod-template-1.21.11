package com.zcpu.tzzmod.condition;

public record ConditionEvaluationTrace(
        ConditionEvaluationResult rootResult,
        int evaluatedNodeCount,
        int maxDepth,
        int maxNodes,
        boolean truncated,
        long durationNanos
) {
    public ConditionEvaluationTrace {
        evaluatedNodeCount = Math.max(0, evaluatedNodeCount);
        maxDepth = Math.max(0, maxDepth);
        maxNodes = Math.max(0, maxNodes);
        durationNanos = Math.max(0L, durationNanos);
    }
}
