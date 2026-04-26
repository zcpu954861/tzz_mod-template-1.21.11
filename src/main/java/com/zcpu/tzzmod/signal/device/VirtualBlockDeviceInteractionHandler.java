package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
                    || !device.interactionEnabled()) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockState state = serverWorld.getBlockState(pos);
            if (state.isAir() || !VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (device.interactionItemMatcherEnabled()) {
                handleItemMatchedInteraction(serverWorld, serverPlayer, hand, hitResult.getSide().asString(), pos, device);
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (device.interactChannel().isBlank() || !SignalChannel.isValid(device.interactChannel())) {
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
            swingInteractionHand(serverPlayer, hand);
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

    private static void handleItemMatchedInteraction(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            BlockPos pos,
            SignalDeviceData device
    ) {
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        ItemStack stack = player.getMainHandStack();
        if (!ItemStackMatcher.matches(stack, matcher)) {
            runFailureFeedback(world, player, hand, sideName, device, "物品不匹配");
            return;
        }
        if (matcher.consumeEnabled() && stack.getCount() < matcher.consumeCount()) {
            runFailureFeedback(world, player, hand, sideName, device, "not_enough_items_to_consume");
            return;
        }

        String channel = matcher.successChannel().isBlank() ? device.interactChannel() : matcher.successChannel();
        ActionExecutionResult result = null;
        if (!channel.isBlank() && SignalChannel.isValid(channel)) {
            result = SignalBridgeServer.emit(new SignalEvent(
                    channel,
                    player,
                    world,
                    Vec3d.ofCenter(pos),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    world.getTime()
            ));
        }

        if (!matcher.successMessage().isBlank()) {
            player.sendMessage(Text.literal(matcher.successMessage()).formatted(net.minecraft.util.Formatting.GREEN), false);
        }
        playConfiguredSound(player, matcher.successSoundId(), matcher.successSoundVolume(), matcher.successSoundPitch());

        int consumed = 0;
        if (matcher.consumeEnabled()) {
            consumed = matcher.consumeCount();
            stack.decrement(consumed);
            if (stack.isEmpty()) {
                player.setStackInHand(hand, ItemStack.EMPTY);
            }
        }

        swingInteractionHand(player, hand);
        SignalDeviceStore.recordVirtualInteractionItemResult(
                world,
                device,
                player,
                hand.name(),
                sideName,
                true,
                channel.isBlank() ? "匹配成功，但未配置成功频道" : "匹配成功",
                consumed,
                result
        );
    }

    private static void runFailureFeedback(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            SignalDeviceData device,
            String failureReason
    ) {
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        ActionExecutionResult result = null;
        if (!matcher.failChannel().isBlank() && SignalChannel.isValid(matcher.failChannel())) {
            result = SignalBridgeServer.emit(new SignalEvent(
                    matcher.failChannel(),
                    player,
                    world,
                    Vec3d.ofCenter(new BlockPos(device.x(), device.y(), device.z())),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    world.getTime()
            ));
        }
        if (!matcher.failMessage().isBlank()) {
            player.sendMessage(Text.literal(matcher.failMessage()).formatted(net.minecraft.util.Formatting.RED), false);
        }
        playConfiguredSound(player, matcher.failSoundId(), matcher.failSoundVolume(), matcher.failSoundPitch());

        swingInteractionHand(player, hand);
        boolean didSomething = result != null || !matcher.failMessage().isBlank() || !matcher.failSoundId().isBlank();
        if (didSomething) {
            SignalDeviceStore.recordVirtualInteractionItemResult(
                    world,
                    device,
                    player,
                    hand.name(),
                    sideName,
                    false,
                    "匹配失败：" + failureReason,
                    0,
                    result
            );
        }
    }

    private static void swingInteractionHand(ServerPlayerEntity player, Hand hand) {
        if (player != null && hand == Hand.MAIN_HAND) {
            player.swingHand(hand, true);
        }
    }

    private static void playConfiguredSound(ServerPlayerEntity player, String soundId, float volume, float pitch) {
        if (player == null || soundId == null || soundId.isBlank()) {
            return;
        }
        Identifier id = Identifier.tryParse(soundId);
        if (id == null || !Registries.SOUND_EVENT.containsId(id)) {
            return;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(id);
        if (sound == null) {
            return;
        }
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                RegistryEntry.of(sound),
                SoundCategory.PLAYERS,
                player.getX(),
                player.getY(),
                player.getZ(),
                volume,
                pitch,
                player.getRandom().nextLong()
        ));
    }
}
