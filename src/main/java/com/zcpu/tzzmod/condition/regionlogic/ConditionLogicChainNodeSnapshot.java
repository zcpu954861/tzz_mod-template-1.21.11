package com.zcpu.tzzmod.condition.regionlogic;

import com.zcpu.tzzmod.signal.SignalChannel;

public record ConditionLogicChainNodeSnapshot(
        String nodeId,
        String nodeType,
        String channel,
        boolean enabled
) {
    public ConditionLogicChainNodeSnapshot {
        nodeId = safe(nodeId);
        nodeType = safe(nodeType);
        channel = SignalChannel.normalize(channel);
    }

    public static ConditionLogicChainNodeSnapshot of(String nodeId, String nodeType, String channel) {
        return new ConditionLogicChainNodeSnapshot(nodeId, nodeType, channel, true);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
