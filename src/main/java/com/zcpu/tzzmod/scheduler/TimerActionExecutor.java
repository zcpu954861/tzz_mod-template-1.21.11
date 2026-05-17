package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

final class TimerActionExecutor {
    private final ConditionActionGateService actionGateService = new ConditionActionGateService();

    ActionExecutionResult execute(
            MinecraftServer server,
            TimerRuntimeInstance instance,
            String bucket,
            List<ActionConfig> actions,
            long now
    ) {
        List<ActionConfig> safeActions = actions == null ? List.of() : actions;
        if (safeActions.isEmpty()) {
            return ActionExecutionResult.success(Text.literal("Timer action list 为空，已跳过。"));
        }
        if (server == null || instance == null) {
            return ActionExecutionResult.failure(Text.literal("Timer action 执行上下文为空。"));
        }
        ServerWorld world = resolveWorld(server, instance.worldId);
        if (world == null) {
            return ActionExecutionResult.failure(Text.literal("Timer action 缺少可用世界，无法执行。"));
        }
        ServerPlayerEntity player = resolvePlayer(server, instance.playerId);
        if (instance.definition.scopeMode == TimerScopeMode.PLAYER && !instance.playerId.isBlank() && player == null) {
            return ActionExecutionResult.failure(Text.literal("PLAYER Timer 的目标玩家不在线，已跳过 action list，避免按全局上下文执行。"));
        }
        Vec3d position = new Vec3d(instance.x, instance.y, instance.z);
        ActionContext actionContext = new ActionContext(
                player,
                world,
                position,
                ActionSourceType.SCHEDULER_TIMER,
                instance.definition.id,
                ItemStack.EMPTY
        );
        ConditionRuntimeTargetType actionTargetType = actionTargetType(bucket);
        ConditionRuntimeTargetType parentTargetType = parentTargetType(bucket);
        ActionExecutionResult last = ActionExecutionResult.success(Text.literal("Timer action list 未执行动作。"));
        for (int index = 0; index < safeActions.size(); index++) {
            ActionConfig action = safeActions.get(index);
            if (action == null || !action.isUsable()) {
                continue;
            }
            String actionTargetId = ConditionActionGateService.actionTargetId("timer_" + bucket, instance.definition.id, index);
            ConditionGateResult gate = actionGateService.evaluate(
                    server,
                    action,
                    actionTargetType,
                    actionTargetId,
                    parentTargetType,
                    instance.definition.id,
                    bucket,
                    index,
                    () -> conditionContext(server, world, player, instance, bucket, now)
            );
            if (!gate.allowed()) {
                last = ActionExecutionResult.success(Text.literal("Timer action gate 未通过，已跳过当前动作。"));
                continue;
            }
            last = ActionEngine.execute(actionContext, action);
            if (!last.success()) {
                return last;
            }
        }
        return last;
    }

    private static ConditionEvaluationContext conditionContext(
            MinecraftServer server,
            ServerWorld world,
            ServerPlayerEntity player,
            TimerRuntimeInstance instance,
            String bucket,
            long now
    ) {
        TimerDefinition timer = instance.definition;
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .worldId(world == null ? instance.worldId : world.getRegistryKey().getValue().toString())
                .source(ActionSourceType.SCHEDULER_TIMER.id(), timer.id)
                .triggerType(parentTargetType(bucket).id())
                .detail(bucket)
                .gameTime(now)
                .blockPos(blockPosSummary(instance))
                .stateVariables(server == null ? null : StateVariableStore.getSnapshot(server))
                .eventMetadata("trigger", "timer_" + safe(bucket))
                .eventMetadata("detail", bucket)
                .eventMetadata("timerId", timer.id)
                .eventMetadata("timerMode", timer.mode.name())
                .eventMetadata("timerScopeMode", timer.scopeMode.name())
                .eventMetadata("timerScopeKey", instance.scopeKey)
                .eventMetadata("timerRunCount", Integer.toString(instance.runCount))
                .eventMetadata("timerRemainingTicks", Long.toString(instance.remainingTicks(now)))
                .variable("timerId", timer.id)
                .variable("timerMode", timer.mode.name())
                .variable("timerScopeMode", timer.scopeMode.name())
                .variable("timerScopeKey", instance.scopeKey)
                .variable("timerRunCount", Integer.toString(instance.runCount))
                .variable("timerRemainingTicks", Long.toString(instance.remainingTicks(now)));
        if (player != null) {
            builder.player(player.getUuidAsString(), player.getName().getString())
                    .playerOnline(true)
                    .playerAlive(player.isAlive())
                    .playerTags(player.getCommandTags())
                    .playerGameMode(String.valueOf(player.interactionManager.getGameMode()).toLowerCase(Locale.ROOT));
            try {
                builder.playerOp(server.getPlayerManager().isOperator(player.getPlayerConfigEntry()));
            } catch (RuntimeException ignored) {
                // Optional condition context field.
            }
            if (player.getScoreboardTeam() != null) {
                builder.playerTeam(player.getScoreboardTeam().getName());
            }
        } else if (!instance.playerId.isBlank() || !instance.playerName.isBlank()) {
            builder.player(instance.playerId, instance.playerName).playerOnline(false);
        }
        return builder.build();
    }

    static ServerPlayerEntity resolvePlayer(MinecraftServer server, String playerIdOrName) {
        if (server == null || playerIdOrName == null || playerIdOrName.isBlank()) {
            return null;
        }
        try {
            ServerPlayerEntity byUuid = server.getPlayerManager().getPlayer(UUID.fromString(playerIdOrName.trim()));
            if (byUuid != null) {
                return byUuid;
            }
        } catch (IllegalArgumentException ignored) {
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player != null && player.getName().getString().equalsIgnoreCase(playerIdOrName.trim())) {
                return player;
            }
        }
        return null;
    }

    static ServerWorld resolveWorld(MinecraftServer server, String worldId) {
        if (server == null) {
            return null;
        }
        String safeWorldId = safe(worldId);
        if (!safeWorldId.isBlank()) {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey().getValue().toString().equals(safeWorldId)) {
                    return world;
                }
            }
        }
        return server.getOverworld();
    }

    private static ConditionRuntimeTargetType actionTargetType(String bucket) {
        return switch (safe(bucket)) {
            case "start" -> ConditionRuntimeTargetType.TIMER_ON_START_ACTION;
            case "tick" -> ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION;
            case "cancel" -> ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION;
            default -> ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION;
        };
    }

    private static ConditionRuntimeTargetType parentTargetType(String bucket) {
        return switch (safe(bucket)) {
            case "start" -> ConditionRuntimeTargetType.TIMER_ON_START;
            case "tick" -> ConditionRuntimeTargetType.TIMER_ON_TICK;
            case "cancel" -> ConditionRuntimeTargetType.TIMER_ON_CANCEL;
            default -> ConditionRuntimeTargetType.TIMER_ON_COMPLETE;
        };
    }

    private static String blockPosSummary(TimerRuntimeInstance instance) {
        return ((int) Math.floor(instance.x)) + "," + ((int) Math.floor(instance.y)) + "," + ((int) Math.floor(instance.z));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
