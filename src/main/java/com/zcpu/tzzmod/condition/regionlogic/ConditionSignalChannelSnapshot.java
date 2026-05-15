package com.zcpu.tzzmod.condition.regionlogic;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.List;

public record ConditionSignalChannelSnapshot(
        String channel,
        int consumerCount,
        int enabledConsumerCount,
        int disabledConsumerCount,
        int actionCount,
        List<ConditionSignalEventSnapshot> recentEvents
) {
    public ConditionSignalChannelSnapshot {
        channel = SignalChannel.normalize(channel);
        consumerCount = Math.max(0, consumerCount);
        enabledConsumerCount = Math.max(0, enabledConsumerCount);
        disabledConsumerCount = Math.max(0, disabledConsumerCount);
        actionCount = Math.max(0, actionCount);
        recentEvents = recentEvents == null ? List.of() : List.copyOf(recentEvents);
    }

    public static ConditionSignalChannelSnapshot of(String channel, int consumerCount, int enabledConsumerCount, int disabledConsumerCount, int actionCount) {
        return new ConditionSignalChannelSnapshot(channel, consumerCount, enabledConsumerCount, disabledConsumerCount, actionCount, List.of());
    }

    public int eventCount() {
        return recentEvents.size();
    }
}
