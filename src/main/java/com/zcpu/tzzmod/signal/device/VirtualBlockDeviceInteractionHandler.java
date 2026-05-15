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
import com.zcpu.tzzmod.signal.device.item.ConsumePlan;
import com.zcpu.tzzmod.signal.device.item.ConsumePlanner;
import com.zcpu.tzzmod.signal.device.item.InteractionDecision;
import com.zcpu.tzzmod.signal.device.item.InteractionDecisionEvaluator;
import com.zcpu.tzzmod.signal.device.item.InteractionItemConsumeSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluationResult;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitInventoryAdapter;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluator;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.util.NullSafety;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
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
    private static final String MAIN_HAND_KEY = "main_hand";
    private static final String OFF_HAND_KEY = "off_hand";
    private static final ConditionGateService CONDITION_GATE_SERVICE = new ConditionGateService();
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
            if (device.interactionItemMatcherEnabled() || device.itemSubmitEnabled()) {
                boolean inCooldown = SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L;
                return NullSafety.requireNonNull(handleItemMatchedInteraction(
                        serverWorld,
                        serverPlayer,
                        hand,
                        hitResult.getSide().asString(),
                        devicePos,
                        device,
                        inCooldown
                ));
            }

            if (SignalDeviceStore.getRemainingInteractionCooldownTicks(device, serverWorld.getTime()) > 0L) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (device.interactChannel().isBlank() || !SignalChannel.isValid(device.interactChannel())) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            ConditionGateResult gate = evaluateInteractionGate(
                    serverWorld,
                    serverPlayer,
                    hand,
                    hitResult.getSide().asString(),
                    devicePos,
                    device,
                    ConditionRuntimeTargetType.VBD_INTERACTION,
                    device.interactChannel(),
                    "right_click"
            );
            if (!gate.allowed()) {
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
        boolean itemSubmitMode = device.itemSubmitEnabled();
        boolean hasItemMatcher = !itemSubmitMode && device.interactionItemMatcherEnabled() && matcher.enabled();
        if (!itemSubmitMode && !hasItemMatcher) {
            return ActionResult.PASS;
        }
        String successChannel = matcher.successChannel().isBlank() ? device.interactChannel() : matcher.successChannel();

        ConditionGateResult interactionGate = evaluateInteractionGate(
                world,
                player,
                hand,
                sideName,
                pos,
                device,
                ConditionRuntimeTargetType.VBD_INTERACTION,
                successChannel,
                itemSubmitMode ? "right_click_item_submit" : "right_click_item_matcher"
        );
        if (!interactionGate.allowed()) {
            return ActionResult.PASS;
        }

        InteractionItemMatch match = hasItemMatcher ? evaluateInteractionItemMatch(player, matcher) : InteractionItemMatch.itemSubmit();
        if (!itemSubmitMode && hasItemMatcher && !match.matched()) {
            return fail(world, player, hand, sideName, device, matcher, inCooldown, match.source(), match.matchedSlot(), match.matchedCount(), "item_not_matched");
        }

        ConsumePlan consumePlan = new ConsumePlan();
        if (itemSubmitMode) {
            ConditionGateResult itemSubmitGate = evaluateInteractionGate(
                    world,
                    player,
                    hand,
                    sideName,
                    pos,
                    device,
                    ConditionRuntimeTargetType.ITEM_SUBMIT,
                    successChannel,
                    "item_submit"
            );
            if (!itemSubmitGate.allowed()) {
                return ActionResult.PASS;
            }
            ItemSubmitEvaluationResult submitEvaluation = evaluateItemSubmit(player, device, !inCooldown, consumePlan);
            if (!submitEvaluation.finalSuccess()) {
                return fail(world, player, hand, sideName, device, matcher, inCooldown, "item_submit", -1, 0, submitEvaluation.failureReason());
            }
            if (device.itemSubmitConsumeEnabled()) {
                consumePlan.replaceWith(submitEvaluation.stagedConsumePlan());
            }
        }

        if (!itemSubmitMode && hasItemMatcher && matcher.consumeEnabled()) {
            String failure = stageInteractionConsumePlan(player, matcher, match, consumePlan);
            if (!failure.isBlank()) {
                return fail(world, player, hand, sideName, device, matcher, inCooldown, match.source(), match.matchedSlot(), match.matchedCount(), failure);
            }
        }

        String consumedSlots = "";
        if (!consumePlan.isEmpty()) {
            consumedSlots = consumePlan.summary();
            consumePlan.apply();
        }

        InteractionDecision decision = InteractionDecisionEvaluator.evaluate(
                matcher.interactionItemVanillaPolicy(),
                true,
                inCooldown,
                !consumePlan.isEmpty(),
                true,
                ""
        );
        if (!decision.executeSignal()) {
            return ActionResult.PASS;
        }

        String channel = successChannel;
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

        swingInteractionHand(player, hand);
        if (itemSubmitMode) {
            SignalDeviceStore.recordVirtualItemSubmitResult(
                    world,
                    device,
                    true,
                    "",
                    consumedSlots,
                    "itemSubmit success"
            );
        }
        SignalDeviceStore.recordVirtualInteractionItemResult(
                world,
                device,
                player,
                hand.name(),
                sideName,
                true,
                channel.isBlank() ? "match_success_no_channel" : "match_success",
                consumePlan.totalCount(),
                match.source(),
                match.matchedSlot(),
                match.matchedCount(),
                consumePlan.primarySource(),
                consumedSlots,
                consumePlan.isEmpty() ? "" : "consume_success",
                result
        );
        return ActionResult.PASS;
    }

    private static ConditionGateResult evaluateInteractionGate(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
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
                        () -> {
                            if (targetType == ConditionRuntimeTargetType.ITEM_SUBMIT) {
                                return ConditionRuntimeContextBuilder.itemSubmit(world, pos, device, player, hand, sideName, channel, detail);
                            }
                            return ConditionRuntimeContextBuilder.interaction(world, pos, device, player, hand, sideName, channel, detail);
                        }
                )
        );
    }

    private static String stageInteractionConsumePlan(
            ServerPlayerEntity player,
            ItemStackMatcherData matcher,
            InteractionItemMatch match,
            ConsumePlan plan
    ) {
        ConsumePlan staged = plan.copy();
        String failure = addInteractionConsumePlan(player, matcher, match, staged);
        if (!failure.isBlank()) {
            return failure;
        }
        plan.replaceWith(staged);
        return "";
    }

    private static ActionResult fail(
            ServerWorld world,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            SignalDeviceData device,
            ItemStackMatcherData matcher,
            boolean inCooldown,
            String sourceName,
            int matchedSlot,
            int matchedCount,
            String failureReason
    ) {
        InteractionDecision decision = InteractionDecisionEvaluator.evaluate(
                matcher.interactionItemVanillaPolicy(),
                false,
                inCooldown,
                false,
                true,
                failureReason
        );
        if (!decision.executeSignal()) {
            return decision.allowVanillaInteraction() ? ActionResult.PASS : ActionResult.FAIL;
        }
        if (device.itemSubmitEnabled()) {
            SignalDeviceStore.recordVirtualItemSubmitResult(world, device, false, failureReason, "", "itemSubmit failed");
        }
        return runFailureFeedback(world, player, hand, sideName, device, sourceName, matchedSlot, matchedCount, failureReason);
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
                    "match_failed:" + failureReason,
                    0,
                    sourceName,
                    matchedSlot,
                    matchedCount,
                    "",
                    "",
                    "",
                    result
            );
        }
        return vanillaFailureResult(matcher);
    }

    private static ActionResult vanillaFailureResult(ItemStackMatcherData matcher) {
        return InteractionItemVanillaPolicy.blocksVanillaOnFailure(matcher.interactionItemVanillaPolicy())
                ? ActionResult.FAIL
                : ActionResult.PASS;
    }

    private static String addInteractionConsumePlan(
            ServerPlayerEntity player,
            ItemStackMatcherData matcher,
            InteractionItemMatch match,
            ConsumePlan plan
    ) {
        String consumeSource = resolveConsumeSource(matcher, match);
        if (consumeSource.isBlank()) {
            return "consume_source_unsupported";
        }
        int consumeCount = Math.max(1, matcher.consumeCount());
        return switch (consumeSource) {
            case InteractionItemConsumeSource.MAIN_HAND -> addHandConsumePlan(player, matcher, Hand.MAIN_HAND, consumeCount, plan);
            case InteractionItemConsumeSource.OFF_HAND -> addHandConsumePlan(player, matcher, Hand.OFF_HAND, consumeCount, plan);
            case InteractionItemConsumeSource.INVENTORY -> addInventoryConsumePlan(player, matcher, consumeCount, matcher.interactionItemInventoryConsumeOrder(), plan, "interactionItem");
            default -> "consume_source_unsupported";
        };
    }

    private static String resolveConsumeSource(ItemStackMatcherData matcher, InteractionItemMatch match) {
        String configured = InteractionItemConsumeSource.normalize(matcher.interactionItemConsumeSource());
        if (!InteractionItemConsumeSource.MATCHED_SOURCE.equals(configured)) {
            return configured;
        }
        return switch (InteractionItemSource.normalize(match.source())) {
            case InteractionItemSource.MAIN_HAND -> InteractionItemConsumeSource.MAIN_HAND;
            case InteractionItemSource.OFF_HAND -> InteractionItemConsumeSource.OFF_HAND;
            case InteractionItemSource.INVENTORY_CONTAINS -> InteractionItemConsumeSource.INVENTORY;
            default -> "";
        };
    }

    private static String addHandConsumePlan(
            ServerPlayerEntity player,
            ItemStackMatcherData matcher,
            Hand consumeHand,
            int count,
            ConsumePlan plan
    ) {
        ItemStack stack = consumeHand == Hand.OFF_HAND ? player.getOffHandStack() : player.getMainHandStack();
        String key = consumeHand == Hand.OFF_HAND ? OFF_HAND_KEY : MAIN_HAND_KEY;
        if (!ItemStackMatcher.matchesIgnoringCount(stack, matcher)) {
            return "consume_source_not_matched";
        }
        return ConsumePlanner.stageSingle(
                plan,
                key,
                stack.getCount(),
                count,
                key,
                amount -> {
                    stack.decrement(amount);
                    if (stack.isEmpty()) {
                        player.setStackInHand(consumeHand, ItemStack.EMPTY);
                    }
                }
        );
    }

    private static String addInventoryConsumePlan(
            ServerPlayerEntity player,
            ItemStackMatcherData matcher,
            int count,
            String order,
            ConsumePlan plan,
            String label
    ) {
        List<ItemStack> stacks = player.getInventory().getMainStacks();
        List<ConsumePlanner.ConsumableStack> matchingStacks = new ArrayList<>();
        for (int slot : inventorySlotOrder(stacks.size(), order)) {
            ItemStack stack = stacks.get(slot);
            if (!ItemStackMatcher.matchesIgnoringCount(stack, matcher)) {
                continue;
            }
            String key = inventoryKey(slot);
            matchingStacks.add(new ConsumePlanner.ConsumableStack(
                    key,
                    stack.getCount(),
                    label + ":slot" + slot,
                    stack::decrement
            ));
        }
        return ConsumePlanner.stageAcrossStacks(plan, matchingStacks, count);
    }

    private static ItemSubmitEvaluationResult evaluateItemSubmit(
            ServerPlayerEntity player,
            SignalDeviceData device,
            boolean recordResults,
            ConsumePlan existingPlan
    ) {
        if (!device.itemSubmitEnabled()) {
            return new ItemSubmitEvaluationResult(true, true, true, "", List.of(), new ConsumePlan(), "");
        }

        ItemSubmitInventoryAdapter.View view = ItemSubmitInventoryAdapter.fromMainStacks(
                player.getInventory().getMainStacks(),
                device.itemSubmitConsumeOrder()
        );
        ItemSubmitEvaluationResult evaluation = ItemSubmitEvaluator.evaluate(
                device.itemSubmitRequirements(),
                view.sourceStacks(),
                device.itemSubmitConsumeEnabled(),
                existingPlan,
                view.matcher()
        );

        long gameTime = player.getCommandSource().getWorld().getTime();
        List<ItemSubmitRequirementData> updated = new ArrayList<>();
        Map<String, ItemSubmitEvaluationResult.RequirementResult> resultsByName = new HashMap<>();
        for (ItemSubmitEvaluationResult.RequirementResult result : evaluation.requirementResults()) {
            resultsByName.put(result.name(), result);
        }
        for (ItemSubmitRequirementData rawRequirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData requirement = rawRequirement.normalized();
            if (!requirement.enabled()) {
                updated.add(requirement);
                continue;
            }
            ItemSubmitEvaluationResult.RequirementResult result = resultsByName.get(requirement.name());
            if (result == null) {
                updated.add(requirement);
            } else {
                updated.add(requirement.withResult(
                        result.matched(),
                        result.matchedCount(),
                        gameTime,
                        result.matched() ? "matched" : "not_matched"
                ));
            }
        }
        if (recordResults) {
            SignalDeviceStore.updateVirtualItemSubmit(
                    player.getCommandSource().getWorld(),
                    new BlockPos(device.x(), device.y(), device.z()),
                    device.itemSubmitEnabled(),
                    device.itemSubmitConsumeEnabled(),
                    device.itemSubmitConsumeOrder(),
                    updated,
                    evaluation.finalSuccess() ? "itemSubmit matched" : "itemSubmit requirement failed: " + evaluation.failureReason()
            );
        }
        return evaluation;
    }

    private static InteractionItemMatch evaluateInteractionItemMatch(ServerPlayerEntity player, ItemStackMatcherData matcher) {
        String source = InteractionItemSource.normalize(matcher.interactionItemSource());
        if (InteractionItemSource.OFF_HAND.equals(source)) {
            ItemStack stack = player.getOffHandStack();
            boolean matched = ItemStackMatcher.matches(stack, matcher);
            return new InteractionItemMatch(matched, source, -1, matched && !stack.isEmpty() ? stack.getCount() : 0);
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
            return new InteractionItemMatch(matchesInventoryCount(totalCount, matcher), source, firstSlot, totalCount);
        }
        if (InteractionItemSource.ARMOR_HEAD.equals(source)) {
            return evaluateEquippedStack(player, matcher, EquipmentSlot.HEAD, InteractionItemSource.ARMOR_HEAD);
        }
        if (InteractionItemSource.ARMOR_CHEST.equals(source)) {
            return evaluateEquippedStack(player, matcher, EquipmentSlot.CHEST, InteractionItemSource.ARMOR_CHEST);
        }
        if (InteractionItemSource.ARMOR_LEGS.equals(source)) {
            return evaluateEquippedStack(player, matcher, EquipmentSlot.LEGS, InteractionItemSource.ARMOR_LEGS);
        }
        if (InteractionItemSource.ARMOR_FEET.equals(source)) {
            return evaluateEquippedStack(player, matcher, EquipmentSlot.FEET, InteractionItemSource.ARMOR_FEET);
        }
        if (InteractionItemSource.ARMOR_ANY.equals(source)) {
            for (ArmorSource armorSource : ARMOR_SOURCES) {
                InteractionItemMatch match = evaluateEquippedStack(player, matcher, armorSource.slot(), armorSource.source());
                if (match.matched()) {
                    return match;
                }
            }
            return new InteractionItemMatch(false, InteractionItemSource.ARMOR_ANY, -1, 0);
        }

        ItemStack stack = player.getMainHandStack();
        boolean matched = ItemStackMatcher.matches(stack, matcher);
        return new InteractionItemMatch(
                matched,
                InteractionItemSource.MAIN_HAND,
                player.getInventory().getSelectedSlot(),
                matched && !stack.isEmpty() ? stack.getCount() : 0
        );
    }

    private static InteractionItemMatch evaluateEquippedStack(
            ServerPlayerEntity player,
            ItemStackMatcherData matcher,
            EquipmentSlot slot,
            String source
    ) {
        ItemStack stack = player.getEquippedStack(slot);
        boolean matched = ItemStackMatcher.matches(stack, matcher);
        return new InteractionItemMatch(matched, source, -1, matched && !stack.isEmpty() ? stack.getCount() : 0);
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

    private static List<Integer> inventorySlotOrder(int size, String rawOrder) {
        return ConsumePlanner.inventorySlotOrder(size, rawOrder);
    }

    private static String inventoryKey(int slot) {
        return "inv:" + slot;
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

    private static final List<ArmorSource> ARMOR_SOURCES = List.of(
            new ArmorSource(EquipmentSlot.HEAD, InteractionItemSource.ARMOR_HEAD),
            new ArmorSource(EquipmentSlot.CHEST, InteractionItemSource.ARMOR_CHEST),
            new ArmorSource(EquipmentSlot.LEGS, InteractionItemSource.ARMOR_LEGS),
            new ArmorSource(EquipmentSlot.FEET, InteractionItemSource.ARMOR_FEET)
    );

    private record ArmorSource(EquipmentSlot slot, String source) {
    }

    private record InteractionItemMatch(boolean matched, String source, int matchedSlot, int matchedCount) {
        private static InteractionItemMatch empty() {
            return new InteractionItemMatch(true, "", -1, 0);
        }

        private static InteractionItemMatch itemSubmit() {
            return new InteractionItemMatch(true, "item_submit", -1, 0);
        }
    }

    private record InteractionTarget(SignalDeviceData device, BlockPos devicePos, BlockState clickedState) {
    }

}
