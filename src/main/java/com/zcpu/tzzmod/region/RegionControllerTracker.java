package com.zcpu.tzzmod.region;

import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.map.MapDataStore;
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

    private RegionControllerTracker() {
    }

    public static void tick(MinecraftServer server, long currentTick) {
        List<RegionControllerData> controllers = RegionControllerStore.getEnabledControllers(server);
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            onlinePlayers.add(playerId);

            PlayerRegionState state = PLAYER_STATES.computeIfAbsent(playerId, ignored -> new PlayerRegionState());
            String dimensionId = player.getEntityWorld().getRegistryKey().getValue().toString();

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

    private static void executeActions(ServerPlayerEntity player, RegionControllerData controller, RegionTriggerType triggerType) {
        if (controller.actionsFor(triggerType).isEmpty()) {
            return;
        }

        ActionContext context = new ActionContext(
                player,
                player.getEntityWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.REGION_CONTROLLER,
                controller.id(),
                ItemStack.EMPTY
        );
        ActionEngine.executeAll(context, controller.actionsFor(triggerType));
    }

    private static final class PlayerRegionState {
        private boolean initialized;
        private String lastDimensionId = "";
        private final Set<String> insideControllerIds = new HashSet<>();
        private final Map<String, Long> lastStayTriggerTicks = new HashMap<>();
    }
}
