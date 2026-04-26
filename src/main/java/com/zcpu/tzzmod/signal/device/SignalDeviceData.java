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
        int pulseTicks,
        int remainingPulseTicks,
        int cooldownTicks,
        int actionCount,
        long createdWallTimeMillis,
        long updatedWallTimeMillis,
        long lastTriggerGameTime,
        long lastTriggerWallTimeMillis,
        String lastResult,
        String blockId,
        String offChannel,
        String mode,
        boolean lastPowered,
        int lastPowerLevel
) {
    public static final String TYPE_SIGNAL_EMITTER = "signal_emitter";
    public static final String TYPE_SIGNAL_RECEIVER = "signal_receiver";
    public static final String TYPE_ACTION_RELAY = "action_relay";
    public static final String TYPE_VIRTUAL_BLOCK_DEVICE = "virtual_block_device";
    public static final int DEFAULT_RECEIVER_PULSE_TICKS = 5;

    public SignalDeviceData normalized() {
        String cleanId = id == null ? "" : id.trim();
        String cleanType = type == null || type.isBlank() ? TYPE_SIGNAL_EMITTER : type.trim();
        String cleanName = name == null ? "" : name.trim();
        String cleanDimension = dimension == null ? "" : dimension.trim();
        String cleanLastResult = lastResult == null ? "" : lastResult.trim();
        String cleanBlockId = blockId == null ? "" : blockId.trim();
        String cleanOffChannel = SignalChannel.normalize(offChannel);
        String cleanMode = VirtualBlockDeviceMode.normalize(mode);
        int cleanPulseTicks = cleanType.equals(TYPE_SIGNAL_RECEIVER)
                ? Math.max(1, pulseTicks <= 0 ? DEFAULT_RECEIVER_PULSE_TICKS : pulseTicks)
                : Math.max(0, pulseTicks);
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
                cleanPulseTicks,
                Math.max(0, remainingPulseTicks),
                Math.max(0, cooldownTicks),
                Math.max(0, actionCount),
                Math.max(0L, createdWallTimeMillis),
                Math.max(0L, updatedWallTimeMillis),
                Math.max(0L, lastTriggerGameTime),
                Math.max(0L, lastTriggerWallTimeMillis),
                cleanLastResult,
                cleanBlockId,
                cleanOffChannel,
                cleanMode,
                lastPowered,
                Math.max(0, Math.min(15, lastPowerLevel))
        );
    }
}
