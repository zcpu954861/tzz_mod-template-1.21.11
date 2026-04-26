package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
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
            BlockState clickedState = serverWorld.getBlockState(pos);
            InteractionTarget target = findInteractionTarget(serverWorld, pos, clickedState);
            if (target == null) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            SignalDeviceData device = target.device();
            if (device == null
                    || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())
                    || !device.enabled()
                    || !device.interactionEnabled()) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockState state = target.clickedState();
            if (state.isAir() || !VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockPos devicePos = target.devicePos();
            if (device.interactionItemMatcherEnabled()) {
                boolean inCooldown = SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L;
                return NullSafety.requireNonNull(handleItemMatchedInteraction(serverWorld, serverPlayer, hand, hitResult.getSide().asString(), devicePos, device, inCooldown));
            }

            if (SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (device.interactChannel().isBlank() || !SignalChannel.isValid(device.interactChannel())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                    device.interactChannel(),
                    serverPlayer,
                    serverWorld,
                    Vec3d.ofCenter(devicePos),
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

    private static InteractionTarget findInteractionTarget(ServerWorld world, BlockPos clickedPos, BlockState clickedState) {
        SignalDeviceData direct = SignalDeviceStore.findVirtualBlockDevice(world.getServer(), world, clickedPos);
        if (direct != null) {
            return new InteractionTarget(direct, clickedPos, clickedState);
        }

        BlockPos otherHalfPos = otherDoorHalfPos(clickedState, clickedPos);
        if (otherHalfPos == null) {
            return null;
        }
        SignalDeviceData otherHalf = SignalDeviceStore.findVirtualBlockDevice(world.getServer(), world, otherHalfPos);
        if (otherHalf == null) {
            return null;
        }
        return new InteractionTarget(otherHalf, otherHalfPos, clickedState);
    }

    private static BlockPos otherDoorHalfPos(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof DoorBlock) || !state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            return null;
        }
        DoubleBlockHalf half = state.get(Properties.DOUBLE_BLOCK_HALF);
        return half == DoubleBlockHalf.UPPER ? pos.down() : pos.up();
    }

    private static ActionResult handleItemMatchedInteraction(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            BlockPos pos,
            SignalDeviceData device,
            boolean inCooldown
    ) {
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        InteractionItemMatch match = evaluateInteractionItemMatch(player, matcher);
        if (!match.matched()) {
            if (inCooldown) {
                return vanillaFailureResult(matcher);
            }
            return runFailureFeedback(world, player, hand, sideName, device, match.source(), match.matchedSlot(), match.matchedCount(), "物品不匹配");
        }
        if (matcher.consumeEnabled() && !InteractionItemSource.supportsConsume(match.source())) {
            if (inCooldown) {
                return vanillaFailureResult(matcher);
            }
            return runFailureFeedback(world, player, hand, sideName, device, match.source(), match.matchedSlot(), match.matchedCount(), "consume_source_unsupported");
        }
        if (matcher.consumeEnabled() && match.stackForConsume().getCount() < matcher.consumeCount()) {
            if (inCooldown) {
                return vanillaFailureResult(matcher);
            }
            return runFailureFeedback(world, player, hand, sideName, device, match.source(), match.matchedSlot(), match.matchedCount(), "not_enough_items_to_consume");
        }
        if (inCooldown) {
            return ActionResult.PASS;
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
            match.stackForConsume().decrement(consumed);
            if (match.stackForConsume().isEmpty()) {
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
                match.source(),
                match.matchedSlot(),
                match.matchedCount(),
                result
        );
        return ActionResult.PASS;
    }

    private static ActionResult runFailureFeedback(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            SignalDeviceData device,
            String sourceName,
            int matchedSlot,
            int matchedCount,
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
                    sourceName,
                    matchedSlot,
                    matchedCount,
                    result
            );
        }
        return InteractionItemVanillaPolicy.blocksVanillaOnFailure(matcher.interactionItemVanillaPolicy())
                ? ActionResult.FAIL
                : ActionResult.PASS;
    }

    private static ActionResult vanillaFailureResult(ItemStackMatcherData matcher) {
        return InteractionItemVanillaPolicy.blocksVanillaOnFailure(matcher.interactionItemVanillaPolicy())
                ? ActionResult.FAIL
                : ActionResult.PASS;
    }

    private static InteractionItemMatch evaluateInteractionItemMatch(ServerPlayerEntity player, ItemStackMatcherData matcher) {
        String source = InteractionItemSource.normalize(matcher.interactionItemSource());
        if (InteractionItemSource.OFF_HAND.equals(source)) {
            ItemStack stack = player.getOffHandStack();
            boolean matched = ItemStackMatcher.matches(stack, matcher);
            return new InteractionItemMatch(
                    matched,
                    source,
                    -1,
                    matched && stack != null && !stack.isEmpty() ? stack.getCount() : 0,
                    ItemStack.EMPTY
            );
        }
        if (InteractionItemSource.INVENTORY_CONTAINS.equals(source)) {
            int firstSlot = -1;
            int totalCount = 0;
            var stacks = player.getInventory().getMainStacks();
            for (int index = 0; index < stacks.size(); index++) {
                ItemStack stack = stacks.get(index);
                if (!ItemStackMatcher.matchesIgnoringCount(stack, matcher)) {
                    continue;
                }
                if (firstSlot < 0) {
                    firstSlot = index;
                }
                totalCount += stack.getCount();
            }
            return new InteractionItemMatch(
                    matchesInventoryCount(totalCount, matcher),
                    source,
                    firstSlot,
                    totalCount,
                    ItemStack.EMPTY
            );
        }

        ItemStack stack = player.getMainHandStack();
        boolean matched = ItemStackMatcher.matches(stack, matcher);
        return new InteractionItemMatch(
                matched,
                InteractionItemSource.MAIN_HAND,
                player.getInventory().getSelectedSlot(),
                matched && stack != null && !stack.isEmpty() ? stack.getCount() : 0,
                stack
        );
    }

    private static boolean matchesInventoryCount(int totalCount, ItemStackMatcherData matcher) {
        String mode = ContainerItemCountMode.normalize(matcher.countMode());
        if (ContainerItemCountMode.IGNORE.id().equals(mode)) {
            return totalCount > 0;
        }
        if (ContainerItemCountMode.AT_MOST.id().equals(mode)) {
            return totalCount > 0 && totalCount <= matcher.requiredCount();
        }
        return ContainerItemCountMode.fromId(mode).matches(totalCount, matcher.requiredCount());
    }

    private static void swingInteractionHand(ServerPlayerEntity player, Hand hand) {
        if (player != null && hand == Hand.MAIN_HAND) {
            player.swingHand(hand, true);
        }
    }

    private record InteractionItemMatch(
            boolean matched,
            String source,
            int matchedSlot,
            int matchedCount,
            ItemStack stackForConsume
    ) {
    }

    private record InteractionTarget(
            SignalDeviceData device,
            BlockPos devicePos,
            BlockState clickedState
    ) {
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
