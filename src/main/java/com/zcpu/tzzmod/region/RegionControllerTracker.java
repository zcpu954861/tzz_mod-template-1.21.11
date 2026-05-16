package com.zcpu.tzzmod.region;

import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.condition.runtime.ConditionGateRequest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeContextBuilder;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class RegionControllerTracker {
    private static final Map<UUID, PlayerRegionState> PLAYER_STATES = new HashMap<>();
    private static final ConditionGateService CONDITION_GATE_SERVICE = new ConditionGateService();

    private RegionControllerTracker() {
    }

    public static void tick(MinecraftServer server, long currentTick) {
        List<RegionControllerData> controllers = RegionControllerStore.getEnabledControllers(server);
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            onlinePlayers.add(playerId);

            PlayerRegionState state = PLAYER_STATES.computeIfAbsent(playerId, ignored -> new PlayerRegionState());
            String dimensionId = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();

            for (RegionControllerData controller : controllers) {
                if (!controller.targetFilter().normalized().matches(player)) {
                    continue;
                }

                MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(server, controller.regionId());
                if (region == null) {
                    continue;
                }

                boolean inside = dimensionId.equals(region.dimensionId())
                        && region.containsBlock(player.getBlockX(), player.getBlockZ());
                boolean oldInside = state.insideControllerIds.contains(controller.id());

                if (!state.initialized) {
                    updateInsideState(state, controller.id(), inside, currentTick);
                    continue;
                }

                if (!oldInside && inside) {
                    updateInsideState(state, controller.id(), true, currentTick);
                    executeActions(player, controller, RegionTriggerType.ENTER);
                    continue;
                }

                if (oldInside && !inside) {
                    updateInsideState(state, controller.id(), false, currentTick);
                    executeActions(player, controller, RegionTriggerType.EXIT);
                    continue;
                }

                if (inside) {
                    maybeExecuteStay(player, controller, state, currentTick);
                }
            }

            state.initialized = true;
            state.lastDimensionId = dimensionId;
        }

        PLAYER_STATES.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
    }

    public static void clearServerState() {
        PLAYER_STATES.clear();
    }

    private static void updateInsideState(PlayerRegionState state, String controllerId, boolean inside, long currentTick) {
        if (inside) {
            state.insideControllerIds.add(controllerId);
            state.lastStayTriggerTicks.put(controllerId, currentTick);
        } else {
            state.insideControllerIds.remove(controllerId);
            state.lastStayTriggerTicks.remove(controllerId);
        }
    }

    private static void maybeExecuteStay(
            ServerPlayerEntity player,
            RegionControllerData controller,
            PlayerRegionState state,
            long currentTick
    ) {
        if (controller.stayActions().isEmpty()) {
            return;
        }

        int interval = Math.max(RegionControllerData.MIN_STAY_INTERVAL_TICKS, controller.stayIntervalTicks());
        Long lastTick = state.lastStayTriggerTicks.get(controller.id());
        if (lastTick == null) {
            state.lastStayTriggerTicks.put(controller.id(), currentTick);
            return;
        }

        if (currentTick - lastTick < interval) {
            return;
        }

        executeActions(player, controller, RegionTriggerType.STAY);
        state.lastStayTriggerTicks.put(controller.id(), currentTick);
    }

    private static ActionExecutionResult executeActions(ServerPlayerEntity player, RegionControllerData controller, RegionTriggerType triggerType) {
        if (controller.actionsFor(triggerType).isEmpty()) {
            return ActionExecutionResult.success(net.minecraft.text.Text.literal("区域控制器没有配置动作"));
        }

        MinecraftServer server = player.getCommandSource().getServer();
        MapDataStore.PlannerRegionData region = server == null ? null : MapDataStore.getPlannerRegion(server, controller.regionId());
        String conditionGroupId = conditionGroupIdFor(controller, triggerType);
        ConditionGateResult gate = CONDITION_GATE_SERVICE.evaluate(server, new ConditionGateRequest(
                conditionGroupId,
                targetTypeFor(triggerType),
                controller.id(),
                () -> ConditionRuntimeContextBuilder.regionController(server, player, controller, triggerType, region)
        ));
        if (!gate.allowed()) {
            return ActionExecutionResult.failure(net.minecraft.text.Text.literal("区域控制器条件阻断：" + gate.failureReason()));
        }
        ActionContext context = new ActionContext(
                player,
                player.getCommandSource().getWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.REGION_CONTROLLER,
                controller.id(),
                ItemStack.EMPTY
        );
        ActionExecutionResult result = ActionEngine.executeAll(context, controller.actionsFor(triggerType));
        WebAdminRealtimeEventBus.publishRegionRuntimeEvent(controller, triggerType, player);
        return result;
    }

    public static ActionExecutionResult executeActionsForTest(ServerPlayerEntity player, RegionControllerData controller, RegionTriggerType triggerType) {
        if (player == null || controller == null || triggerType == null) {
            return ActionExecutionResult.failure(net.minecraft.text.Text.literal("区域控制器测试缺少上下文"));
        }
        return executeActions(player, controller, triggerType);
    }

    private static String conditionGroupIdFor(RegionControllerData controller, RegionTriggerType triggerType) {
        if (controller == null || triggerType == null) {
            return "";
        }
        return switch (triggerType) {
            case ENTER -> controller.enterConditionGroupId();
            case EXIT -> controller.exitConditionGroupId();
            case STAY -> controller.stayConditionGroupId();
        };
    }

    private static ConditionRuntimeTargetType targetTypeFor(RegionTriggerType triggerType) {
        return switch (triggerType == null ? RegionTriggerType.STAY : triggerType) {
            case ENTER -> ConditionRuntimeTargetType.REGION_ENTER;
            case EXIT -> ConditionRuntimeTargetType.REGION_EXIT;
            case STAY -> ConditionRuntimeTargetType.REGION_STAY;
        };
    }

    private static final class PlayerRegionState {
        private boolean initialized;
        private String lastDimensionId = "";
        private final Set<String> insideControllerIds = new HashSet<>();
        private final Map<String, Long> lastStayTriggerTicks = new HashMap<>();
    }
}
