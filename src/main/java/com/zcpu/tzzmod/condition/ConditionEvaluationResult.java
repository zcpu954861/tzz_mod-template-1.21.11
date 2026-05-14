package com.zcpu.tzzmod.condition;

import java.util.List;

public record ConditionEvaluationResult(
        boolean matched,
        String conditionId,
        String nodeId,
        String type,
        String label,
        String reasonCode,
        String failureReason,
        String message,
        String debugSummary,
        List<ConditionEvaluationResult> childResults,
        boolean skipped,
        boolean error,
        int evaluatedNodeCount,
        long durationNanos,
        String contextSummary
) {
    public ConditionEvaluationResult {
        conditionId = safe(conditionId);
        nodeId = safe(nodeId);
        type = safe(type);
        label = safe(label);
        reasonCode = safe(reasonCode);
        failureReason = safe(failureReason);
        message = safe(message);
        debugSummary = safe(debugSummary);
        childResults = childResults == null ? List.of() : List.copyOf(childResults);
        evaluatedNodeCount = Math.max(0, evaluatedNodeCount);
        durationNanos = Math.max(0L, durationNanos);
        contextSummary = safe(contextSummary);
    }

    public ConditionEvaluationStatus status() {
        if (skipped) {
            return ConditionEvaluationStatus.SKIPPED;
        }
        if (error) {
            return ConditionEvaluationStatus.ERROR;
        }
        return matched ? ConditionEvaluationStatus.PASSED : ConditionEvaluationStatus.FAILED;
    }

    public static ConditionEvaluationResult leaf(
            ConditionNode node,
            ConditionEvaluationContext context,
            boolean matched,
            String reasonCode,
            String message
    ) {
        return new ConditionEvaluationResult(
                matched,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label(node),
                reasonCode,
                matched ? "" : message,
                message,
                message,
                List.of(),
                false,
                false,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public static ConditionEvaluationResult skipped(ConditionNode node, ConditionEvaluationContext context) {
        return new ConditionEvaluationResult(
                true,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label(node),
                "condition_node_disabled",
                "",
                "条件节点已禁用，跳过判断",
                "skipped disabled node",
                List.of(),
                true,
                false,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public static ConditionEvaluationResult error(ConditionNode node, ConditionEvaluationContext context, String code, String message) {
        return new ConditionEvaluationResult(
                false,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label(node),
                code,
                message,
                message,
                message,
                List.of(),
                false,
                true,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public ConditionEvaluationResult withConditionId(String conditionId) {
        return new ConditionEvaluationResult(
                matched,
                conditionId,
                nodeId,
                type,
                label,
                reasonCode,
                failureReason,
                message,
                debugSummary,
                childResults,
                skipped,
                error,
                evaluatedNodeCount,
                durationNanos,
                contextSummary
        );
    }

    public ConditionEvaluationResult withDuration(long durationNanos) {
        return new ConditionEvaluationResult(
                matched,
                conditionId,
                nodeId,
                type,
                label,
                reasonCode,
                failureReason,
                message,
                debugSummary,
                childResults,
                skipped,
                error,
                evaluatedNodeCount,
                durationNanos,
                contextSummary
        );
    }

    static String label(ConditionNode node) {
        if (node == null) {
            return "";
        }
        if (!node.name().isBlank()) {
            return node.name();
        }
        return node.id().isBlank() ? node.type() : node.id();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
