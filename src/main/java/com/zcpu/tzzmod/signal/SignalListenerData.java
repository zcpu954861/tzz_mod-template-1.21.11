package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.action.ActionConfig;
import java.util.List;

public record SignalListenerData(
        String id,
        String name,
        String channel,
        boolean enabled,
        int cooldownTicks,
        List<ActionConfig> actions
) {
    public static final int DEFAULT_COOLDOWN_TICKS = 0;
    public static final int MIN_COOLDOWN_TICKS = 0;

    public SignalListenerData normalized() {
        String cleanId = id == null ? "" : id.trim();
        String cleanName = name == null ? "" : name.trim();
        return new SignalListenerData(
                cleanId,
                cleanName,
                SignalChannel.normalize(channel),
                enabled,
                Math.max(MIN_COOLDOWN_TICKS, cooldownTicks),
                actions == null ? List.of() : List.copyOf(actions)
        );
    }
}
