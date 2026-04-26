package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirtualBlockDeviceDispatcher {
    private VirtualBlockDeviceDispatcher() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        for (SignalDeviceData device : SignalDeviceStore.getVirtualBlockDevicesSnapshot(server)) {
            tickDevice(server, device);
        }
    }

    private static void tickDevice(MinecraftServer server, SignalDeviceData device) {
        if (device == null || !device.enabled()) {
            return;
        }

        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return;
        }

        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return;
        }

        VirtualBlockPowerState powerState = VirtualBlockDeviceSupport.powerState(world, pos);
        if (powerState.air() || !powerState.blockId().equals(device.blockId())) {
            return;
        }

        if (powerState.currentPowered() == device.lastPowered()) {
            return;
        }

        boolean rising = !device.lastPowered() && powerState.currentPowered();
        boolean falling = device.lastPowered() && !powerState.currentPowered();
        VirtualBlockDeviceMode mode = VirtualBlockDeviceMode.fromId(device.mode());
        String channel = null;
        if (rising && mode.triggersRising()) {
            channel = device.channel();
        } else if (falling && mode.triggersFalling()) {
            channel = device.offChannel().isBlank() ? device.channel() : device.offChannel();
        }

        if (channel == null || channel.isBlank()) {
            SignalDeviceStore.recordVirtualPowerState(world, device, powerState);
            return;
        }

        ActionExecutionResult result;
        if (SignalChannel.isValid(channel)) {
            result = SignalBridgeServer.emit(new SignalEvent(
                    channel,
                    null,
                    world,
                    Vec3d.ofCenter(pos),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    world.getTime()
            ));
        } else {
            result = ActionExecutionResult.failure(SignalChannel.validationError(channel));
        }
        SignalDeviceStore.recordVirtualBlockTrigger(world, device, powerState, result);
    }
}
