package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirtualBlockDeviceInteractionHandler {
    private static boolean registered;

    private VirtualBlockDeviceInteractionHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockPos pos = hitResult.getBlockPos();
            SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(serverWorld.getServer(), serverWorld, pos);
            if (device == null
                    || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())
                    || !device.enabled()
                    || !device.interactionEnabled()
                    || device.interactChannel().isBlank()
                    || !SignalChannel.isValid(device.interactChannel())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockState state = serverWorld.getBlockState(pos);
            if (state.isAir() || !VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (device.interactionItemMatcherEnabled()
                    && !ItemStackMatcher.matches(serverPlayer.getMainHandStack(), device.interactionItemMatcher())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                    device.interactChannel(),
                    serverPlayer,
                    serverWorld,
                    Vec3d.ofCenter(pos),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    serverWorld.getTime()
            ));
            serverPlayer.swingHand(hand, true);
            SignalDeviceStore.recordVirtualInteractionTrigger(
                    serverWorld,
                    device,
                    serverPlayer,
                    hand.name(),
                    hitResult.getSide().asString(),
                    result
            );
            return NullSafety.requireNonNull(ActionResult.PASS);
        });
    }
}
