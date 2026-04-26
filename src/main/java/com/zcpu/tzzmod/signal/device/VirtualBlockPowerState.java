package com.zcpu.tzzmod.signal.device;

public record VirtualBlockPowerState(
        String blockId,
        boolean blockStatePowered,
        int receivedPowerLevel,
        boolean currentPowered,
        boolean air
) {
}
