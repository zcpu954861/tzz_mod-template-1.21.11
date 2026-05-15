package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.condition.runtime.ConditionGateRequest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeContextBuilder;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeGateStore;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirtualBlockDeviceDispatcher {
    private static final ConditionGateService CONDITION_GATE_SERVICE = new ConditionGateService();

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

        BlockState state = world.getBlockState(pos);
        if (device.conditionEnabled()) {
            tickCondition(world, pos, device, state);
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
            ConditionGateResult gate = evaluateGate(world, pos, device, ConditionRuntimeTargetType.VBD_REDSTONE, channel, rising ? "redstone_rising" : "redstone_falling");
            if (!gate.allowed()) {
                return;
            }
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

    private static void tickCondition(ServerWorld world, BlockPos pos, SignalDeviceData device, BlockState state) {
        if (!device.conditionEnabled()) {
            return;
        }
        if (!VirtualBlockDeviceSupport.blockId(state).equals(device.conditionBlockId())) {
            return;
        }

        boolean currentMatched = BlockStateConditionParser.matches(state, device);
        if (currentMatched == device.lastConditionMatched()) {
            return;
        }

        boolean entering = !device.lastConditionMatched() && currentMatched;
        boolean exiting = device.lastConditionMatched() && !currentMatched;
        BlockStateConditionMode mode = BlockStateConditionMode.fromId(device.conditionMode());
        String channel = null;
        if (entering && mode.triggersEnter()) {
            channel = device.channel();
        } else if (exiting && mode.triggersExit()) {
            channel = device.offChannel().isBlank() ? device.channel() : device.offChannel();
        }

        if (channel == null || channel.isBlank()) {
            SignalDeviceStore.recordVirtualConditionState(
                    world,
                    device,
                    currentMatched,
                    currentMatched ? "当前满足方块状态条件" : "当前不满足方块状态条件"
            );
            return;
        }

        ActionExecutionResult result;
        if (SignalChannel.isValid(channel)) {
            ConditionGateResult gate = evaluateGate(world, pos, device, ConditionRuntimeTargetType.VBD_BLOCKSTATE, channel, entering ? "blockstate_enter" : "blockstate_exit");
            if (!gate.allowed()) {
                return;
            }
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
        SignalDeviceStore.recordVirtualConditionTrigger(world, device, currentMatched, result);
    }

    private static ConditionGateResult evaluateGate(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ConditionRuntimeTargetType targetType,
            String channel,
            String detail
    ) {
        String conditionGroupId = ConditionRuntimeGateStore.conditionGroupId(world.getServer(), device.id(), targetType);
        return CONDITION_GATE_SERVICE.evaluate(
                world.getServer(),
                new ConditionGateRequest(
                        conditionGroupId,
                        targetType,
                        device.id(),
                        () -> ConditionRuntimeContextBuilder.base(world, pos, device, targetType, channel, detail)
                )
        );
    }
}
