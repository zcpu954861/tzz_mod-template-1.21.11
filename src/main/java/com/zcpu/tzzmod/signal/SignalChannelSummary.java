package com.zcpu.tzzmod.signal;

public record SignalChannelSummary(
        String channel,
        int listenerCount,
        int enabledListenerCount,
        int disabledListenerCount,
        int actionCount,
        SignalEventRecord latestEvent
) {
}
