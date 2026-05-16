package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.condition.item.ConditionContainerSnapshot;
import com.zcpu.tzzmod.condition.item.ConditionInventorySnapshot;
import com.zcpu.tzzmod.condition.item.ConditionItemStackSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionRegionSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.item.ConsumePlan;
import com.zcpu.tzzmod.signal.device.item.ConsumePlanner;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluationResult;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluator;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class ConditionRuntimeContextBuilder {
    private ConditionRuntimeContextBuilder() {
    }

    public static ConditionEvaluationContext base(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ConditionRuntimeTargetType targetType,
            String channel,
            String detail
    ) {
        return baseBuilder(world, pos, device, targetType, channel, detail).build();
    }

    public static ConditionEvaluationContext interaction(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            String channel,
            String detail
    ) {
        ConditionEvaluationContext.Builder builder = baseBuilder(world, pos, device, ConditionRuntimeTargetType.VBD_INTERACTION, channel, detail)
                .eventMetadata("hand", hand == null ? "" : hand.name().toLowerCase(Locale.ROOT))
                .eventMetadata("side", sideName);
        addPlayer(builder, player);
        addHands(builder, player);
        addPlayerInventory(builder, player);
        return builder.build();
    }

    public static ConditionEvaluationContext itemSubmit(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ServerPlayerEntity player,
            Hand hand,
            String sideName,
            String channel,
            String detail
    ) {
        ConditionEvaluationContext.Builder builder = baseBuilder(world, pos, device, ConditionRuntimeTargetType.ITEM_SUBMIT, channel, detail)
                .eventMetadata("hand", hand == null ? "" : hand.name().toLowerCase(Locale.ROOT))
                .eventMetadata("side", sideName)
                .eventMetadata("itemSubmit", "true");
        addPlayer(builder, player);
        addHands(builder, player);
        addSubmittedItem(builder, device, player);
        addPlayerInventory(builder, player);
        return builder.build();
    }

    public static ConditionEvaluationContext container(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ServerPlayerEntity player,
            Inventory inventory,
            ConditionRuntimeTargetType targetType,
            String channel,
            String detail
    ) {
        ConditionEvaluationContext.Builder builder = baseBuilder(world, pos, device, targetType, channel, detail)
                .eventMetadata("container", "true");
        addPlayer(builder, player);
        if (inventory != null) {
            builder.containerSnapshot("container", containerSnapshot(inventory));
        }
        return builder.build();
    }

    public static ConditionEvaluationContext signalListener(SignalEvent event, SignalListenerData listener) {
        ServerWorld world = event == null ? null : event.world();
        String channel = SignalChannel.normalize(event == null ? "" : event.channel());
        ConditionEvaluationContext.Builder builder = signalEventBuilder(
                world,
                event,
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                channel,
                event == null ? "" : event.detail()
        );
        if (listener != null) {
            builder.listenerId(listener.id());
        }
        return builder.build();
    }

    public static ConditionEvaluationContext actionRelay(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ActionRelayBlockEntity relay,
            SignalEvent event
    ) {
        String deviceId = device == null ? "" : device.id();
        String relayId = !deviceId.isBlank()
                ? deviceId
                : world == null || pos == null ? "" : ActionRelayBlockEntity.sourceId(world, pos);
        String contextDeviceId = !deviceId.isBlank() ? deviceId : relayId;
        ConditionEvaluationContext.Builder builder = signalEventBuilder(
                world,
                event,
                ConditionRuntimeTargetType.ACTION_RELAY,
                SignalChannel.normalize(event == null ? (relay == null ? "" : relay.channel()) : event.channel()),
                event == null ? "" : event.detail()
        )
                .deviceId(contextDeviceId)
                .blockPos(pos == null ? "" : pos.toShortString())
                .variable("relayId", relayId);
        return builder.build();
    }

    public static ConditionEvaluationContext regionController(
            MinecraftServer server,
            ServerPlayerEntity player,
            RegionControllerData controller,
            RegionTriggerType triggerType,
            MapDataStore.PlannerRegionData region
    ) {
        ServerWorld world = player == null ? null : player.getCommandSource().getWorld();
        ConditionRuntimeTargetType targetType = switch (triggerType == null ? RegionTriggerType.STAY : triggerType) {
            case ENTER -> ConditionRuntimeTargetType.REGION_ENTER;
            case EXIT -> ConditionRuntimeTargetType.REGION_EXIT;
            case STAY -> ConditionRuntimeTargetType.REGION_STAY;
        };
        String bucket = switch (triggerType == null ? RegionTriggerType.STAY : triggerType) {
            case ENTER -> "enter";
            case EXIT -> "exit";
            case STAY -> "stay";
        };
        String regionId = controller == null ? "" : controller.regionId();
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .worldId(world == null ? "" : world.getRegistryKey().getValue().toString())
                .source("region_controller", controller == null ? "" : controller.id())
                .regionId(regionId)
                .triggerType(targetType.id())
                .detail(bucket)
                .eventMetadata("trigger", targetType.id())
                .eventMetadata("detail", bucket)
                .eventMetadata("actionBucket", bucket)
                .gameTime(world == null ? 0L : world.getTime())
                .regionSnapshot("region", regionSnapshot(server, region, controller))
                .regionSnapshot("current_region", regionSnapshot(server, region, controller));
        addPlayer(builder, player);
        if (world != null) {
            builder.stateVariables(StateVariableStore.getSnapshot(world.getServer()));
        } else if (server != null) {
            builder.stateVariables(StateVariableStore.getSnapshot(server));
        }
        return builder.build();
    }

    public static ConditionEvaluationContext withActionMetadata(
            ConditionEvaluationContext parent,
            ActionConfig action,
            ConditionRuntimeTargetType actionTargetType,
            String actionTargetId,
            ConditionRuntimeTargetType parentTargetType,
            String parentTargetId,
            String parentActionBucket,
            int actionIndex
    ) {
        ConditionEvaluationContext safeParent = parent == null ? ConditionEvaluationContext.builder().build() : parent;
        String actionId = safe(actionTargetId);
        String actionType = action == null || action.type() == null ? "" : action.type().id();
        String displayIndex = actionIndex < 0 ? "" : Integer.toString(actionIndex + 1);
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .player(safeParent.playerId(), safeParent.playerName())
                .playerTags(safeParent.playerTags())
                .playerTeam(safeParent.playerTeam())
                .playerGameMode(safeParent.playerGameMode())
                .worldId(safeParent.worldId())
                .source(safeParent.sourceType(), safeParent.sourceId())
                .channel(safeParent.channel())
                .deviceId(safeParent.deviceId())
                .listenerId(safeParent.listenerId())
                .regionId(safeParent.regionId())
                .actionId(actionId)
                .blockPos(safeParent.blockPos())
                .itemStackSummary(safeParent.itemStackSummary())
                .triggerType(actionTargetType == null ? safeParent.triggerType() : actionTargetType.id())
                .detail(safe(parentActionBucket).isBlank() ? safeParent.detail() : safe(parentActionBucket))
                .gameTime(safeParent.gameTime())
                .signalDepth(safeParent.signalDepth())
                .stateVariables(safeParent.stateVariables());
        if (safeParent.playerOnline() != null) {
            builder.playerOnline(safeParent.playerOnline());
        }
        if (safeParent.playerOp() != null) {
            builder.playerOp(safeParent.playerOp());
        }
        if (safeParent.playerAlive() != null) {
            builder.playerAlive(safeParent.playerAlive());
        }
        safeParent.itemSnapshots().forEach(builder::itemSnapshot);
        safeParent.inventorySnapshots().forEach(builder::inventorySnapshot);
        safeParent.containerSnapshots().forEach(builder::containerSnapshot);
        safeParent.regionSnapshots().forEach(builder::regionSnapshot);
        safeParent.signalChannelSnapshots().forEach(builder::signalChannelSnapshot);
        safeParent.signalHistorySnapshots().forEach(builder::signalHistorySnapshot);
        safeParent.logicChainSnapshots().forEach(builder::logicChainSnapshot);
        safeParent.eventMetadata().forEach(builder::eventMetadata);
        safeParent.variables().forEach(builder::variable);
        builder.eventMetadata("trigger", actionTargetType == null ? "" : actionTargetType.id())
                .eventMetadata("actionId", actionId)
                .eventMetadata("actionIndex", actionIndex < 0 ? "" : Integer.toString(actionIndex))
                .eventMetadata("actionDisplayIndex", displayIndex)
                .eventMetadata("actionType", actionType)
                .eventMetadata("parentTargetType", parentTargetType == null ? "" : parentTargetType.id())
                .eventMetadata("parentTargetId", safe(parentTargetId))
                .eventMetadata("parentActionBucket", safe(parentActionBucket))
                .variable("actionId", actionId)
                .variable("actionIndex", actionIndex < 0 ? "" : Integer.toString(actionIndex))
                .variable("actionDisplayIndex", displayIndex)
                .variable("actionType", actionType)
                .variable("parentTargetType", parentTargetType == null ? "" : parentTargetType.id())
                .variable("parentTargetId", safe(parentTargetId))
                .variable("parentActionBucket", safe(parentActionBucket));
        return builder.build();
    }

    private static ConditionEvaluationContext.Builder baseBuilder(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            ConditionRuntimeTargetType targetType,
            String channel,
            String detail
    ) {
        String worldId = world == null ? "" : world.getRegistryKey().getValue().toString();
        String deviceId = device == null ? "" : device.id();
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .worldId(worldId)
                .source("virtual_block_device", deviceId)
                .deviceId(deviceId)
                .channel(channel)
                .triggerType(targetType == null ? "" : targetType.id())
                .detail(detail)
                .eventMetadata("trigger", targetType == null ? "" : targetType.id())
                .eventMetadata("detail", detail)
                .blockPos(pos == null ? "" : pos.toShortString())
                .gameTime(world == null ? 0L : world.getTime())
                .signalDepth(com.zcpu.tzzmod.signal.SignalBridgeServer.currentDepth());
        if (world != null) {
            builder.stateVariables(StateVariableStore.getSnapshot(world.getServer()));
        }
        return builder;
    }

    private static ConditionEvaluationContext.Builder signalEventBuilder(
            ServerWorld world,
            SignalEvent event,
            ConditionRuntimeTargetType targetType,
            String channel,
            String detail
    ) {
        String worldId = world == null ? "" : world.getRegistryKey().getValue().toString();
        String sourceType = event == null || event.sourceType() == null ? "" : event.sourceType().id();
        String sourceId = event == null ? "" : event.sourceId();
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .worldId(worldId)
                .source(sourceType, sourceId)
                .channel(channel)
                .triggerType(targetType == null ? "" : targetType.id())
                .detail(detail)
                .eventMetadata("trigger", targetType == null ? "" : targetType.id())
                .eventMetadata("detail", detail)
                .gameTime(event == null ? (world == null ? 0L : world.getTime()) : event.gameTime())
                .signalDepth(com.zcpu.tzzmod.signal.SignalBridgeServer.currentDepth());
        if (world != null) {
            builder.stateVariables(StateVariableStore.getSnapshot(world.getServer()));
        }
        return builder;
    }

    private static ConditionRegionSnapshot regionSnapshot(
            MinecraftServer server,
            MapDataStore.PlannerRegionData region,
            RegionControllerData controller
    ) {
        String regionId = region == null ? (controller == null ? "" : controller.regionId()) : region.id();
        String worldId = region == null ? "" : region.dimensionId();
        List<String> playerIdsInside = new ArrayList<>();
        if (server != null && region != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player == null) {
                    continue;
                }
                String playerWorld = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
                if (worldId.equals(playerWorld) && region.containsBlock(player.getBlockX(), player.getBlockZ())) {
                    playerIdsInside.add(player.getUuidAsString());
                }
            }
        }
        return new ConditionRegionSnapshot(
                regionId,
                region == null ? "" : region.name(),
                controller == null || controller.enabled(),
                worldId,
                playerIdsInside,
                regionBoundsSummary(region),
                Map.of("controllerId", controller == null ? "" : controller.id())
        );
    }

    private static String regionBoundsSummary(MapDataStore.PlannerRegionData region) {
        if (region == null || region.points() == null || region.points().isEmpty()) {
            return "";
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (com.zcpu.tzzmod.map.RegionGeometry.Point point : region.points()) {
            if (point == null) {
                continue;
            }
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minZ = Math.min(minZ, point.z());
            maxZ = Math.max(maxZ, point.z());
        }
        if (minX == Integer.MAX_VALUE) {
            return "";
        }
        return minX + "," + minZ + " -> " + maxX + "," + maxZ;
    }

    private static void addPlayer(ConditionEvaluationContext.Builder builder, ServerPlayerEntity player) {
        if (builder == null || player == null) {
            return;
        }
        builder.player(player.getUuidAsString(), player.getName().getString())
                .playerOnline(true)
                .playerAlive(player.isAlive())
                .playerTags(player.getCommandTags())
                .playerGameMode(String.valueOf(player.interactionManager.getGameMode()).toLowerCase(Locale.ROOT));
        try {
            builder.playerOp(player.getCommandSource().getServer().getPlayerManager().isOperator(player.getPlayerConfigEntry()));
        } catch (RuntimeException ignored) {
            // Permission level is optional snapshot data; missing op state should not crash gate building.
        }
        if (player.getScoreboardTeam() != null) {
            builder.playerTeam(player.getScoreboardTeam().getName());
        }
    }

    private static void addHands(ConditionEvaluationContext.Builder builder, ServerPlayerEntity player) {
        if (builder == null || player == null) {
            return;
        }
        ConditionItemStackSnapshot main = itemSnapshot(player.getMainHandStack());
        ConditionItemStackSnapshot off = itemSnapshot(player.getOffHandStack());
        builder.itemSnapshot("main_hand", main)
                .itemSnapshot("held_item", main)
                .itemSnapshot("off_hand", off)
                .itemStackSummary(main.summary());
    }

    private static void addSubmittedItem(ConditionEvaluationContext.Builder builder, SignalDeviceData device, ServerPlayerEntity player) {
        if (builder == null || player == null) {
            return;
        }
        builder.itemSnapshot("submitted_item", submittedItemSnapshot(device, player));
    }

    private static ConditionItemStackSnapshot submittedItemSnapshot(SignalDeviceData device, ServerPlayerEntity player) {
        if (device == null || player == null) {
            return ConditionItemStackSnapshot.empty();
        }
        List<ItemStack> stacks = player.getInventory().getMainStacks();
        if (stacks == null || stacks.isEmpty() || device.itemSubmitRequirements() == null || device.itemSubmitRequirements().isEmpty()) {
            return ConditionItemStackSnapshot.empty();
        }

        Map<String, ItemStack> stacksByKey = new LinkedHashMap<>();
        List<ItemSubmitEvaluator.SourceStack> sources = new ArrayList<>();
        for (int slot : ConsumePlanner.inventorySlotOrder(stacks.size(), device.itemSubmitConsumeOrder())) {
            ItemStack stack = stacks.get(slot);
            String key = "inv:" + slot;
            stacksByKey.put(key, stack);
            sources.add(new ItemSubmitEvaluator.SourceStack(
                    key,
                    itemId(stack),
                    stack == null || stack.isEmpty() ? 0 : stack.getCount(),
                    "submit:slot" + slot,
                    ignored -> {
                    }
            ));
        }

        ItemSubmitEvaluationResult evaluation = ItemSubmitEvaluator.evaluate(
                device.itemSubmitRequirements(),
                sources,
                device.itemSubmitConsumeEnabled(),
                new ConsumePlan(),
                (source, matcher) -> ItemStackMatcher.matchesIgnoringCount(stacksByKey.get(source.key()), matcher)
        );
        if (!evaluation.finalSuccess()) {
            return ConditionItemStackSnapshot.empty();
        }
        if (device.itemSubmitConsumeEnabled() && !evaluation.stagedConsumePlan().entries().isEmpty()) {
            ConsumePlan.Entry entry = evaluation.stagedConsumePlan().entries().get(0);
            return itemSnapshotWithCount(stacksByKey.get(entry.key()), entry.count());
        }

        if (device != null && device.itemSubmitRequirements() != null) {
            for (ItemSubmitRequirementData raw : device.itemSubmitRequirements()) {
                if (raw == null || !raw.normalized().enabled()) {
                    continue;
                }
                ItemSubmitRequirementData requirement = raw.normalized();
                for (ItemSubmitEvaluator.SourceStack source : sources) {
                    ItemStack stack = stacksByKey.get(source.key());
                    if (ItemStackMatcher.matchesIgnoringCount(stack, requirement.matcher())) {
                        return itemSnapshot(stack);
                    }
                }
            }
        }
        return ConditionItemStackSnapshot.empty();
    }

    private static void addPlayerInventory(ConditionEvaluationContext.Builder builder, ServerPlayerEntity player) {
        if (builder == null || player == null) {
            return;
        }
        builder.inventorySnapshot("player_inventory", inventorySnapshot(player.getInventory().getMainStacks()));
    }

    private static ConditionInventorySnapshot inventorySnapshot(List<ItemStack> stacks) {
        List<ConditionItemStackSnapshot> slots = new ArrayList<>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                slots.add(itemSnapshot(stack));
            }
        }
        return new ConditionInventorySnapshot(slots);
    }

    private static ConditionContainerSnapshot containerSnapshot(Inventory inventory) {
        if (inventory == null) {
            return ConditionContainerSnapshot.empty();
        }
        List<ConditionItemStackSnapshot> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            slots.add(itemSnapshot(inventory.getStack(slot)));
        }
        return new ConditionContainerSnapshot(slots);
    }

    public static ConditionItemStackSnapshot itemSnapshot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ConditionItemStackSnapshot.empty();
        }
        return new ConditionItemStackSnapshot(
                Registries.ITEM.getId(stack.getItem()).toString(),
                stack.getCount(),
                ItemStackMatcherSupport.customNameSnapshot(stack),
                ItemStackMatcherSupport.loreSnapshot(stack),
                Map.of("customData", ItemStackMatcherSupport.customDataSnapshot(stack)),
                Map.of("components", ItemStackMatcherSupport.componentsSnapshot(stack))
        );
    }

    private static ConditionItemStackSnapshot itemSnapshotWithCount(ItemStack stack, int count) {
        ConditionItemStackSnapshot snapshot = itemSnapshot(stack);
        if (snapshot.isEmpty()) {
            return ConditionItemStackSnapshot.empty();
        }
        return new ConditionItemStackSnapshot(
                snapshot.itemId(),
                count,
                snapshot.displayName(),
                snapshot.lore(),
                snapshot.customData(),
                snapshot.components()
        );
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
