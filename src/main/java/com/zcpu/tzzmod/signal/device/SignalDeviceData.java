package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.signal.SignalChannel;

public record SignalDeviceData(
        String id,
        String type,
        String name,
        String dimension,
        int x,
        int y,
        int z,
        String channel,
        boolean enabled,
        long createdWallTimeMillis,
        long updatedWallTimeMillis,
        long lastTriggerGameTime,
        long lastTriggerWallTimeMillis,
        String lastResult
) {
    public static final String TYPE_SIGNAL_EMITTER = "signal_emitter";

    public SignalDeviceData normalized() {
        String cleanId = id == null ? "" : id.trim();
        String cleanType = type == null || type.isBlank() ? TYPE_SIGNAL_EMITTER : type.trim();
        String cleanName = name == null ? "" : name.trim();
        String cleanDimension = dimension == null ? "" : dimension.trim();
        String cleanLastResult = lastResult == null ? "" : lastResult.trim();
        return new SignalDeviceData(
                cleanId,
                cleanType,
                cleanName,
                cleanDimension,
                x,
                y,
                z,
                SignalChannel.normalize(channel),
                enabled,
                Math.max(0L, createdWallTimeMillis),
                Math.max(0L, updatedWallTimeMillis),
                Math.max(0L, lastTriggerGameTime),
                Math.max(0L, lastTriggerWallTimeMillis),
                cleanLastResult
        );
    }
}
