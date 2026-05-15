package com.zcpu.tzzmod.condition.regionlogic;

public record ConditionLogicChainEdgeSnapshot(
        String fromNodeId,
        String toNodeId,
        String type
) {
    public ConditionLogicChainEdgeSnapshot {
        fromNodeId = safe(fromNodeId);
        toNodeId = safe(toNodeId);
        type = safe(type);
    }

    public static ConditionLogicChainEdgeSnapshot of(String fromNodeId, String toNodeId, String type) {
        return new ConditionLogicChainEdgeSnapshot(fromNodeId, toNodeId, type);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
