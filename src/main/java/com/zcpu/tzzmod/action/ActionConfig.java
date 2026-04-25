package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.signal.SignalChannel;

public record ActionConfig(
        ActionType type,
        String value,
        boolean enabled,
        boolean requiresOp,
        int cooldownTicks,
        boolean notifyOps
) {
    public static ActionConfig command(String command, boolean notifyOps) {
        return new ActionConfig(
                ActionType.COMMAND,
                normalizeCommand(command),
                true,
                false,
                0,
                notifyOps
        );
    }

    public static ActionConfig signal(String channel, boolean notifyOps) {
        return new ActionConfig(
                ActionType.SIGNAL,
                SignalChannel.normalize(channel),
                true,
                false,
                0,
                notifyOps
        );
    }

    public boolean isUsable() {
        return enabled && value != null && !value.trim().isEmpty();
    }

    public static String normalizeCommand(String command) {
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }
}
