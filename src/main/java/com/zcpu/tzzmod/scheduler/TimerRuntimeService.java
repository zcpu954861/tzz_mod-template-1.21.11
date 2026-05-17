package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class TimerRuntimeService {
    public static final int MAX_ACTIVE_TIMERS_PER_SERVER = 2048;
    public static final int MAX_DUE_EXECUTIONS_PER_TICK = 256;
    private static final Map<MinecraftServer, RuntimeStore> STORES = new WeakHashMap<>();
    private static final TimerActionExecutor ACTION_EXECUTOR = new TimerActionExecutor();

    private TimerRuntimeService() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null || server.getOverworld() == null) {
            return;
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            store.tick(server, server.getOverworld().getTime());
        }
    }

    public static void refreshDefinitions(MinecraftServer server) {
        if (server == null) {
            return;
        }
        replaceDefinitionCache(server, TimerStore.loadWithStatus(server));
    }

    public static void replaceDefinitionCache(MinecraftServer server, TimerStore.TimerFile file) {
        replaceDefinitionCache(server, new TimerStore.TimerLoadResult(file == null ? new TimerStore.TimerFile() : file.normalized(), false, ""));
    }

    public static void replaceDefinitionCache(MinecraftServer server, TimerStore.TimerLoadResult loaded) {
        if (server == null) {
            return;
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            store.replaceDefinitions(loaded);
        }
    }

    public static ActionExecutionResult startFromAction(ActionContext context, ActionConfig config) {
        if (context == null || context.world() == null || config == null) {
            return ActionExecutionResult.timerOperation(TimerOperationResult.failure("timer_context_missing", "Timer action 缺少运行上下文。", config == null ? "" : config.timerId(), ""));
        }
        TimerOperationResult result = start(
                context.world().getServer(),
                config.timerId(),
                context,
                config.timerTargetMode(),
                config.timerTargetId(),
                config.timerStartPolicyOverride(),
                config.timerDurationOverrideTicks()
        );
        return ActionExecutionResult.timerOperation(result);
    }

    public static ActionExecutionResult cancelFromAction(ActionContext context, ActionConfig config) {
        if (context == null || context.world() == null || config == null) {
            return ActionExecutionResult.timerOperation(TimerOperationResult.failure("timer_context_missing", "Timer action 缺少运行上下文。", config == null ? "" : config.timerId(), ""));
        }
        TimerOperationResult result = cancel(
                context.world().getServer(),
                config.timerId(),
                context,
                config.timerTargetMode(),
                config.timerTargetId(),
                config.timerMissingBehavior(),
                true
        );
        return ActionExecutionResult.timerOperation(result);
    }

    public static TimerOperationResult startManual(
            MinecraftServer server,
            String timerId,
            String targetMode,
            String targetId,
            String startPolicyOverride
    ) {
        return start(server, timerId, manualContext(server, targetMode, targetId), targetMode, targetId, startPolicyOverride, 0L);
    }

    public static TimerOperationResult cancelManual(
            MinecraftServer server,
            String timerId,
            String targetMode,
            String targetId
    ) {
        return cancel(server, timerId, manualContext(server, targetMode, targetId), targetMode, targetId, "fail", true);
    }

    public static int reset(MinecraftServer server, String timerId, String scopeKey, String reason) {
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            int count = store.reset(timerId, scopeKey, reason);
            if (count > 0) {
                publishRuntimeChanged(timerId, scopeKey, "Timer runtime state 已重置。", "reset", Map.of("resetCount", count));
            }
            return count;
        }
    }

    public static TimerStatusSnapshot status(MinecraftServer server, TimerDefinition definition, long gameTime) {
        TimerDefinition timer = definition == null ? new TimerDefinition().normalized() : definition.normalized();
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            List<Map<String, Object>> instances = new ArrayList<>();
            String lastResult = store.diagnosticResult(timer.id);
            String lastFailure = store.diagnosticFailure(timer.id);
            Map<String, TimerRuntimeInstance> active = store.instances.getOrDefault(timer.id, Map.of());
            for (TimerRuntimeInstance instance : active.values()) {
                instances.add(instance.toMap(gameTime));
                if (!instance.lastResult.isBlank()) {
                    lastResult = instance.lastResult;
                }
                if (!instance.lastFailureReason.isBlank()) {
                    lastFailure = instance.lastFailureReason;
                }
            }
            return new TimerStatusSnapshot(
                    timer.id,
                    timer.enabled,
                    timer.mode.name(),
                    timer.scopeMode.name(),
                    timer.startPolicy.name(),
                    active.size(),
                    gameTime,
                    lastResult,
                    lastFailure,
                    List.copyOf(instances),
                    false
            );
        }
    }

    public static void clearTimer(MinecraftServer server, String timerId) {
        if (server == null || timerId == null || timerId.isBlank()) {
            return;
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            store.reset(timerId, "", "config_changed");
        }
    }

    public static void clearServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (TimerRuntimeService.class) {
            STORES.remove(server);
        }
    }

    public static int activeCount(MinecraftServer server) {
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            return store.instances.values().stream().mapToInt(Map::size).sum();
        }
    }

    public static TestRuntime testRuntime() {
        return new TestRuntime(new RuntimeStore());
    }

    private static TimerOperationResult start(
            MinecraftServer server,
            String rawTimerId,
            ActionContext context,
            String rawTargetMode,
            String rawTargetId,
            String rawStartPolicyOverride,
            long durationOverrideTicks
    ) {
        String timerId = TimerStore.normalizeId(rawTimerId);
        if (server == null) {
            return TimerOperationResult.failure("timer_server_missing", "Timer 启动失败：服务器上下文为空。", timerId, "");
        }
        if (timerId.isBlank()) {
            return TimerOperationResult.failure("timer_id_missing", "timer_start 缺少 timerId。", "", "");
        }
        TimerStore.TimerLoadResult loaded = definitionLoadResult(server);
        if (loaded.degraded()) {
            return TimerOperationResult.failure("timer_store_degraded", loaded.message(), timerId, "");
        }
        TimerDefinition timer = loaded.file().timers.get(timerId);
        if (timer == null) {
            return TimerOperationResult.failure("timer_missing", "Timer 不存在或已删除：" + timerId, timerId, "");
        }
        return start(server, timer, context, rawTargetMode, rawTargetId, rawStartPolicyOverride, durationOverrideTicks);
    }

    private static TimerOperationResult start(
            MinecraftServer server,
            TimerDefinition rawTimer,
            ActionContext context,
            String rawTargetMode,
            String rawTargetId,
            String rawStartPolicyOverride,
            long durationOverrideTicks
    ) {
        TimerDefinition timer = rawTimer == null ? new TimerDefinition().normalized() : rawTimer.normalized();
        if (!timer.enabled) {
            return TimerOperationResult.failure("timer_disabled", "Timer 已停用，不能启动：" + timer.id, timer.id, "");
        }
        List<TimerValidationIssue> issues = TimerValidator.validate(timer, false);
        if (!issues.isEmpty()) {
            return TimerOperationResult.failure("timer_invalid", "Timer 配置无效：" + issues.getFirst().message(), timer.id, "");
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            ScopeResolution scope = resolveScope(server, timer, context, rawTargetMode, rawTargetId);
            if (!scope.success()) {
                store.setDiagnostic(timer.id, "FAILED", scope.message());
                return TimerOperationResult.failure(scope.code(), scope.message(), timer.id, scope.scopeKey());
            }
            TimerStartPolicy policy = TimerStartPolicy.parse(rawStartPolicyOverride);
            if (policy == null) {
                policy = timer.startPolicy;
            }
            Map<String, TimerRuntimeInstance> active = store.instances.computeIfAbsent(timer.id, ignored -> new LinkedHashMap<>());
            TimerRuntimeInstance existing = active.get(scope.scopeKey());
            if (existing != null) {
                if (policy == TimerStartPolicy.IGNORE_IF_RUNNING) {
                    existing.lastResult = "IGNORED_RUNNING";
                    existing.lastFailureReason = "Timer 已在运行中，本次 start 按策略忽略。";
                    return TimerOperationResult.success("timer_ignored_running", existing.lastFailureReason, timer.id, scope.scopeKey(), false);
                }
                if (policy == TimerStartPolicy.FAIL_IF_RUNNING) {
                    existing.lastResult = "FAILED_RUNNING";
                    existing.lastFailureReason = "Timer 已在运行中，启动策略要求失败。";
                    return TimerOperationResult.failure("timer_already_running", existing.lastFailureReason, timer.id, scope.scopeKey());
                }
            }
            if (existing == null && store.activeCount() >= MAX_ACTIVE_TIMERS_PER_SERVER) {
                store.setDiagnostic(timer.id, "ACTIVE_LIMIT", "Timer 运行中实例数量已达到上限。");
                return TimerOperationResult.failure("timer_active_limit", "Timer 运行中实例数量已达到上限。", timer.id, scope.scopeKey());
            }
            long now = currentGameTime(server);
            TimerRuntimeInstance instance = new TimerRuntimeInstance(
                    timer,
                    scope.scopeKey(),
                    scope.playerId(),
                    scope.playerName(),
                    scope.worldId(),
                    scope.position().x,
                    scope.position().y,
                    scope.position().z,
                    now,
                    durationOverrideTicks > 0L ? durationOverrideTicks : -1L
            );
            active.put(scope.scopeKey(), instance);
            ActionExecutionResult startActions = ACTION_EXECUTOR.execute(server, instance, "start", timer.onStartActions, now);
            if (!startActions.success()) {
                instance.lastResult = "START_ACTION_FAILED";
                instance.lastFailureReason = startActions.message() == null ? "Timer onStart action 执行失败。" : startActions.message().getString();
            }
            publishRuntimeChanged(timer.id, scope.scopeKey(), "Timer 已启动。", "start", Map.of("mode", timer.mode.name(), "scopeMode", timer.scopeMode.name()));
            return TimerOperationResult.success("timer_started", "Timer 已启动。", timer.id, scope.scopeKey(), true);
        }
    }

    private static TimerOperationResult cancel(
            MinecraftServer server,
            String rawTimerId,
            ActionContext context,
            String rawTargetMode,
            String rawTargetId,
            String rawMissingBehavior,
            boolean executeCancelActions
    ) {
        String timerId = TimerStore.normalizeId(rawTimerId);
        if (server == null) {
            return TimerOperationResult.failure("timer_server_missing", "Timer 取消失败：服务器上下文为空。", timerId, "");
        }
        if (timerId.isBlank()) {
            return TimerOperationResult.failure("timer_id_missing", "timer_cancel 缺少 timerId。", "", "");
        }
        TimerStore.TimerLoadResult loaded = definitionLoadResult(server);
        if (loaded.degraded()) {
            return TimerOperationResult.failure("timer_store_degraded", loaded.message(), timerId, "");
        }
        TimerDefinition timer = loaded.file().timers.get(timerId);
        if (timer == null) {
            if (missingBehaviorNoop(rawMissingBehavior)) {
                return TimerOperationResult.success("timer_missing_noop", "Timer 不存在或已删除，timer_cancel 按 no-op success 处理：" + timerId, timerId, "", false);
            }
            return TimerOperationResult.failure("timer_missing", "Timer 不存在或已删除：" + timerId, timerId, "");
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            ScopeResolution scope = resolveScope(server, timer.normalized(), context, rawTargetMode, rawTargetId);
            if (!scope.success()) {
                store.setDiagnostic(timerId, "FAILED", scope.message());
                return TimerOperationResult.failure(scope.code(), scope.message(), timerId, scope.scopeKey());
            }
            Map<String, TimerRuntimeInstance> active = store.instances.get(timerId);
            TimerRuntimeInstance removed = active == null ? null : active.remove(scope.scopeKey());
            if (active != null && active.isEmpty()) {
                store.instances.remove(timerId);
            }
            if (removed == null) {
                return TimerOperationResult.success("timer_not_running", "Timer 当前没有运行中实例，取消按 no-op success 处理。", timerId, scope.scopeKey(), false);
            }
            removed.cancelledAtTick = currentGameTime(server);
            removed.lastResult = "CANCELLED";
            removed.lastFailureReason = "";
            if (executeCancelActions && !removed.definition.onCancelActions.isEmpty()) {
                ActionExecutionResult cancelActions = ACTION_EXECUTOR.execute(server, removed, "cancel", removed.definition.onCancelActions, removed.cancelledAtTick);
                if (!cancelActions.success()) {
                    removed.lastResult = "CANCEL_ACTION_FAILED";
                    removed.lastFailureReason = cancelActions.message() == null ? "Timer onCancel action 执行失败。" : cancelActions.message().getString();
                }
            }
            store.setDiagnostic(timerId, removed.lastResult, removed.lastFailureReason);
            publishRuntimeChanged(timerId, scope.scopeKey(), "Timer 已取消。", "cancel", Map.of("scopeMode", timer.scopeMode.name()));
            return TimerOperationResult.success("timer_cancelled", "Timer 已取消，不会执行 onCompleteActions。", timerId, scope.scopeKey(), true);
        }
    }

    private static ScopeResolution resolveScope(
            MinecraftServer server,
            TimerDefinition timer,
            ActionContext context,
            String rawTargetMode,
            String rawTargetId
    ) {
        ServerWorld world = context == null ? null : context.world();
        ServerPlayerEntity contextPlayer = context == null ? null : context.player();
        Vec3d position = context == null ? null : context.position();
        if (world == null) {
            world = server == null ? null : server.getOverworld();
        }
        if (position == null && world != null) {
            position = Vec3d.ZERO;
        }
        if (timer.scopeMode == TimerScopeMode.GLOBAL) {
            return ScopeResolution.ok(
                    "global",
                    "",
                    "",
                    world == null ? "" : world.getRegistryKey().getValue().toString(),
                    position == null ? Vec3d.ZERO : position
            );
        }
        TimerTargetMode targetMode = TimerTargetMode.parse(rawTargetMode);
        if (targetMode == null || targetMode == TimerTargetMode.GLOBAL) {
            targetMode = TimerTargetMode.CONTEXT_PLAYER;
        }
        String targetId = safe(rawTargetId);
        ServerPlayerEntity player = targetMode == TimerTargetMode.EXPLICIT_TARGET
                ? TimerActionExecutor.resolvePlayer(server, targetId)
                : contextPlayer;
        String playerId = "";
        String playerName = "";
        if (player != null) {
            playerId = player.getUuidAsString();
            playerName = player.getName().getString();
            world = player.getCommandSource().getWorld();
            position = new Vec3d(player.getX(), player.getY(), player.getZ());
        } else if (targetMode == TimerTargetMode.EXPLICIT_TARGET && !targetId.isBlank()) {
            playerId = targetId;
        }
        if (playerId.isBlank()) {
            return ScopeResolution.failed("timer_player_context_missing", "PLAYER scope Timer 需要触发玩家或显式 targetId；本次启动/取消没有玩家上下文。", "");
        }
        return ScopeResolution.ok(playerId, playerId, playerName, world == null ? "" : world.getRegistryKey().getValue().toString(), position == null ? Vec3d.ZERO : position);
    }

    private static ActionContext manualContext(MinecraftServer server, String targetMode, String targetId) {
        if (server == null) {
            return null;
        }
        ServerPlayerEntity player = TimerTargetMode.parse(targetMode) == TimerTargetMode.EXPLICIT_TARGET
                ? TimerActionExecutor.resolvePlayer(server, targetId)
                : null;
        ServerWorld world = player == null ? server.getOverworld() : player.getCommandSource().getWorld();
        Vec3d position = player == null
                ? Vec3d.ZERO
                : new Vec3d(player.getX(), player.getY(), player.getZ());
        return new ActionContext(player, world, position, ActionSourceType.SCHEDULER_TIMER, "manual", net.minecraft.item.ItemStack.EMPTY);
    }

    private static RuntimeStore runtimeStore(MinecraftServer server) {
        if (server == null) {
            return new RuntimeStore();
        }
        synchronized (TimerRuntimeService.class) {
            return STORES.computeIfAbsent(server, ignored -> new RuntimeStore());
        }
    }

    private static TimerStore.TimerLoadResult definitionLoadResult(MinecraftServer server) {
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            return store.definitions();
        }
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
    }

    private static boolean missingBehaviorNoop(String rawMissingBehavior) {
        String value = safe(rawMissingBehavior).toLowerCase(java.util.Locale.ROOT);
        return value.isBlank() || "noop_success".equals(value);
    }

    private static void publishRuntimeChanged(String timerId, String scopeKey, String summary, String action, Map<String, ?> extra) {
        WebAdminRealtimeEvent.Builder builder = WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.TIMER_RUNTIME_CHANGED)
                .sourceType("scheduler_timer")
                .severity("INFO")
                .summary(summary)
                .routeTarget("#/timers/" + timerId)
                .payload("targetType", "timer_runtime")
                .payload("timerId", timerId)
                .payload("scopeKey", scopeKey)
                .payload("action", action);
        if (extra != null) {
            for (Map.Entry<String, ?> entry : extra.entrySet()) {
                if (entry != null) {
                    builder.payload(entry.getKey(), entry.getValue());
                }
            }
        }
        WebAdminRealtimeEventBus.publish(builder);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ScopeResolution(boolean success, String code, String message, String scopeKey, String playerId, String playerName, String worldId, Vec3d position) {
        static ScopeResolution ok(String scopeKey, String playerId, String playerName, String worldId, Vec3d position) {
            return new ScopeResolution(true, "ok", "", scopeKey, playerId, playerName, worldId, position);
        }

        static ScopeResolution failed(String code, String message, String scopeKey) {
            return new ScopeResolution(false, code, message, scopeKey, "", "", "", Vec3d.ZERO);
        }
    }

    private static final class RuntimeStore {
        private final Map<String, Map<String, TimerRuntimeInstance>> instances = new LinkedHashMap<>();
        private final Map<String, String> diagnosticResults = new LinkedHashMap<>();
        private final Map<String, String> diagnosticFailures = new LinkedHashMap<>();
        private TimerStore.TimerLoadResult definitions;

        private void replaceDefinitions(TimerStore.TimerLoadResult loaded) {
            TimerStore.TimerLoadResult safeLoaded = loaded == null
                    ? new TimerStore.TimerLoadResult(new TimerStore.TimerFile(), false, "")
                    : loaded;
            TimerStore.TimerFile file = safeLoaded.file() == null ? new TimerStore.TimerFile() : safeLoaded.file().normalized();
            definitions = new TimerStore.TimerLoadResult(file, safeLoaded.degraded(), safeLoaded.message());
        }

        private TimerStore.TimerLoadResult definitions() {
            if (definitions == null) {
                return new TimerStore.TimerLoadResult(
                        new TimerStore.TimerFile(),
                        true,
                        "Timer 定义缓存尚未初始化；请等待服务器启动完成或刷新 WebAdmin Timer 配置。"
                );
            }
            return definitions;
        }

        private int activeCount() {
            return instances.values().stream().mapToInt(Map::size).sum();
        }

        private void tick(MinecraftServer server, long now) {
            if (instances.isEmpty()) {
                return;
            }
            int dueExecutions = 0;
            for (Map.Entry<String, Map<String, TimerRuntimeInstance>> timerEntry : new ArrayList<>(instances.entrySet())) {
                Map<String, TimerRuntimeInstance> scopes = instances.get(timerEntry.getKey());
                if (scopes == null || scopes.isEmpty()) {
                    continue;
                }
                for (Map.Entry<String, TimerRuntimeInstance> scopeEntry : new ArrayList<>(scopes.entrySet())) {
                    TimerRuntimeInstance instance = scopeEntry.getValue();
                    if (scopes.get(scopeEntry.getKey()) != instance) {
                        continue;
                    }
                    if (instance == null || now <= instance.startedAtTick) {
                        continue;
                    }
                    int dueEvents = dueEventCount(instance, now);
                    if (dueEvents <= 0) {
                        continue;
                    }
                    if (dueExecutions + dueEvents > MAX_DUE_EXECUTIONS_PER_TICK) {
                        setDiagnostic(timerEntry.getKey(), "THROTTLED", "本 tick 到期 Timer 过多，剩余实例延后处理。");
                        return;
                    }
                    boolean remove = false;
                    switch (instance.definition.mode) {
                        case DELAY -> {
                            if (now >= instance.deadlineTick) {
                                complete(server, instance, now);
                                dueExecutions++;
                                remove = true;
                            }
                        }
                        case COUNTDOWN -> {
                            if (now < instance.deadlineTick && instance.definition.intervalTicks > 0 && now >= instance.nextTickAt) {
                                executeTick(server, instance, now);
                                dueExecutions++;
                                if (!isLive(timerEntry.getKey(), scopeEntry.getKey(), instance)) {
                                    continue;
                                }
                                instance.nextTickAt = now + Math.max(1L, instance.definition.intervalTicks);
                            }
                            if (now >= instance.deadlineTick) {
                                complete(server, instance, now);
                                dueExecutions++;
                                remove = true;
                            }
                        }
                        case REPEAT -> {
                            if (instance.definition.intervalTicks > 0 && now >= instance.nextTickAt) {
                                executeTick(server, instance, now);
                                dueExecutions++;
                                if (!isLive(timerEntry.getKey(), scopeEntry.getKey(), instance)) {
                                    continue;
                                }
                                instance.runCount++;
                                instance.nextTickAt = now + Math.max(1L, instance.definition.intervalTicks);
                                if (instance.definition.maxRuns > 0 && instance.runCount >= instance.definition.maxRuns) {
                                    complete(server, instance, now);
                                    dueExecutions++;
                                    remove = true;
                                }
                            }
                        }
                    }
                    if (remove) {
                        Map<String, TimerRuntimeInstance> liveScopes = instances.get(timerEntry.getKey());
                        if (liveScopes != null && liveScopes.get(scopeEntry.getKey()) == instance) {
                            liveScopes.remove(scopeEntry.getKey());
                        }
                    }
                }
            }
            instances.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        private boolean isLive(String timerId, String scopeKey, TimerRuntimeInstance instance) {
            Map<String, TimerRuntimeInstance> scopes = instances.get(timerId);
            return scopes != null && scopes.get(scopeKey) == instance;
        }

        private int dueEventCount(TimerRuntimeInstance instance, long now) {
            return switch (instance.definition.mode) {
                case DELAY -> now >= instance.deadlineTick ? 1 : 0;
                case COUNTDOWN -> {
                    if (now >= instance.deadlineTick) {
                        yield 1;
                    }
                    yield instance.definition.intervalTicks > 0 && now >= instance.nextTickAt ? 1 : 0;
                }
                case REPEAT -> {
                    if (instance.definition.intervalTicks <= 0 || now < instance.nextTickAt) {
                        yield 0;
                    }
                    int count = 1;
                    if (instance.definition.maxRuns > 0 && instance.runCount + 1 >= instance.definition.maxRuns) {
                        count++;
                    }
                    yield count;
                }
            };
        }

        private int reset(String rawTimerId, String rawScopeKey, String reason) {
            String timerId = TimerStore.normalizeId(rawTimerId);
            String scopeKey = safe(rawScopeKey);
            if (timerId.isBlank()) {
                int count = activeCount();
                instances.clear();
                diagnosticResults.clear();
                diagnosticFailures.clear();
                return count;
            }
            Map<String, TimerRuntimeInstance> scopes = instances.get(timerId);
            if (scopes == null || scopes.isEmpty()) {
                diagnosticResults.remove(timerId);
                diagnosticFailures.remove(timerId);
                return 0;
            }
            if (scopeKey.isBlank()) {
                int count = scopes.size();
                instances.remove(timerId);
                setDiagnostic(timerId, "RESET", reason);
                return count;
            }
            TimerRuntimeInstance removed = scopes.remove(scopeKey);
            if (scopes.isEmpty()) {
                instances.remove(timerId);
            }
            setDiagnostic(timerId, "RESET", reason);
            return removed == null ? 0 : 1;
        }

        private void executeTick(MinecraftServer server, TimerRuntimeInstance instance, long now) {
            instance.lastTickAt = now;
            ActionExecutionResult result = ACTION_EXECUTOR.execute(server, instance, "tick", instance.definition.onTickActions, now);
            if (result.success()) {
                instance.lastResult = "TICK";
                instance.lastFailureReason = "";
            } else {
                instance.lastResult = "TICK_ACTION_FAILED";
                instance.lastFailureReason = result.message() == null ? "Timer onTick action 执行失败。" : result.message().getString();
            }
            setDiagnostic(instance.definition.id, instance.lastResult, instance.lastFailureReason);
        }

        private void complete(MinecraftServer server, TimerRuntimeInstance instance, long now) {
            instance.completedAtTick = now;
            ActionExecutionResult actionResult = ACTION_EXECUTOR.execute(server, instance, "complete", instance.definition.onCompleteActions, now);
            ActionExecutionResult outputResult = emitOutput(server, instance, now);
            if (!actionResult.success()) {
                instance.lastResult = "COMPLETE_ACTION_FAILED";
                instance.lastFailureReason = actionResult.message() == null ? "Timer onComplete action 执行失败。" : actionResult.message().getString();
            } else if (!outputResult.success()) {
                instance.lastResult = "OUTPUT_FAILED";
                instance.lastFailureReason = outputResult.message() == null ? "Timer outputChannel 输出失败。" : outputResult.message().getString();
            } else {
                instance.lastResult = "COMPLETED";
                instance.lastFailureReason = "";
            }
            setDiagnostic(instance.definition.id, instance.lastResult, instance.lastFailureReason);
            publishRuntimeChanged(instance.definition.id, instance.scopeKey, "Timer 已完成。", "complete", Map.of("runCount", instance.runCount));
        }

        private ActionExecutionResult emitOutput(MinecraftServer server, TimerRuntimeInstance instance, long now) {
            String outputChannel = SignalChannel.normalize(instance.definition.outputChannel);
            if (outputChannel.isBlank()) {
                return ActionExecutionResult.success(Text.literal("Timer outputChannel 未配置，已跳过。"));
            }
            ServerWorld world = TimerActionExecutor.resolveWorld(server, instance.worldId);
            if (world == null) {
                return ActionExecutionResult.failure(Text.literal("Timer outputChannel 缺少可用世界。"));
            }
            ServerPlayerEntity player = TimerActionExecutor.resolvePlayer(server, instance.playerId);
            String detail = "Timer 完成；timerId=" + instance.definition.id
                    + "；scope=" + instance.scopeKey
                    + "；mode=" + instance.definition.mode.name()
                    + "；elapsedTicks=" + Math.max(0L, now - instance.startedAtTick)
                    + "；runCount=" + instance.runCount;
            try {
                return SignalBridgeServer.emit(new SignalEvent(
                        outputChannel,
                        player,
                        world,
                        new Vec3d(instance.x, instance.y, instance.z),
                        ActionSourceType.SCHEDULER_TIMER,
                        instance.definition.id,
                        SignalBridgeServer.currentDepth() + 1,
                        now,
                        detail
                ));
            } catch (RuntimeException exception) {
                Tzz_mod.LOGGER.warn("[Timer] output signal failed timer={} channel={} error={}", instance.definition.id, outputChannel, exception.getMessage());
                return ActionExecutionResult.failure(Text.literal("Timer outputChannel 输出失败：" + exception.getMessage()));
            }
        }

        private void setDiagnostic(String timerId, String result, String failure) {
            String safeId = TimerStore.normalizeId(timerId);
            if (safeId.isBlank()) {
                safeId = "global";
            }
            diagnosticResults.put(safeId, safe(result));
            diagnosticFailures.put(safeId, safe(failure));
        }

        private String diagnosticResult(String timerId) {
            return diagnosticResults.getOrDefault(TimerStore.normalizeId(timerId), "");
        }

        private String diagnosticFailure(String timerId) {
            return diagnosticFailures.getOrDefault(TimerStore.normalizeId(timerId), "");
        }
    }

    public static final class TestRuntime {
        private final RuntimeStore store;
        private int completedCount = 0;
        private int tickCount = 0;
        private int startCount = 0;
        private int cancelCount = 0;

        private TestRuntime(RuntimeStore store) {
            this.store = store;
        }

        public TimerOperationResult start(TimerDefinition definition, String scopeKey, long gameTime) {
            TimerDefinition timer = definition == null ? new TimerDefinition().normalized() : definition.normalized();
            String safeScopeKey = safe(scopeKey);
            if (timer.scopeMode == TimerScopeMode.PLAYER && safeScopeKey.isBlank()) {
                return TimerOperationResult.failure("timer_player_context_missing", "PLAYER scope Timer 需要触发玩家或显式 targetId；本次启动/取消没有玩家上下文。", timer.id, "");
            }
            synchronized (store) {
                Map<String, TimerRuntimeInstance> active = store.instances.computeIfAbsent(timer.id, ignored -> new LinkedHashMap<>());
                if (active.containsKey(safeScopeKey)) {
                    if (timer.startPolicy == TimerStartPolicy.IGNORE_IF_RUNNING) {
                        return TimerOperationResult.success("timer_ignored_running", "Timer 已在运行中，本次 start 按策略忽略。", timer.id, safeScopeKey, false);
                    }
                    if (timer.startPolicy == TimerStartPolicy.FAIL_IF_RUNNING) {
                        return TimerOperationResult.failure("timer_already_running", "Timer 已在运行中，启动策略要求失败。", timer.id, safeScopeKey);
                    }
                }
                if (!active.containsKey(safeScopeKey) && store.activeCount() >= MAX_ACTIVE_TIMERS_PER_SERVER) {
                    store.setDiagnostic(timer.id, "ACTIVE_LIMIT", "Timer 运行中实例数量已达到上限。");
                    return TimerOperationResult.failure("timer_active_limit", "Timer 运行中实例数量已达到上限。", timer.id, safeScopeKey);
                }
                active.put(safeScopeKey, new TimerRuntimeInstance(timer, safeScopeKey, "", "", "", 0, 64, 0, gameTime, -1L));
                if (!timer.onStartActions.isEmpty()) {
                    startCount++;
                }
                return TimerOperationResult.success("timer_started", "Timer 已启动。", timer.id, safeScopeKey, true);
            }
        }

        public void tick(long gameTime) {
            synchronized (store) {
                for (Map<String, TimerRuntimeInstance> scopes : new ArrayList<>(store.instances.values())) {
                    for (TimerRuntimeInstance instance : new ArrayList<>(scopes.values())) {
                        if (gameTime <= instance.startedAtTick) {
                            continue;
                        }
                        if (instance.definition.mode == TimerMode.REPEAT && gameTime >= instance.nextTickAt) {
                            tickCount++;
                            instance.runCount++;
                            instance.nextTickAt = gameTime + Math.max(1L, instance.definition.intervalTicks);
                            if (instance.definition.maxRuns > 0 && instance.runCount >= instance.definition.maxRuns) {
                                completedCount++;
                                scopes.remove(instance.scopeKey);
                            }
                        } else if ((instance.definition.mode == TimerMode.DELAY || instance.definition.mode == TimerMode.COUNTDOWN) && gameTime >= instance.deadlineTick) {
                            completedCount++;
                            scopes.remove(instance.scopeKey);
                        } else if (instance.definition.mode == TimerMode.COUNTDOWN && gameTime >= instance.nextTickAt) {
                            tickCount++;
                            instance.nextTickAt = gameTime + Math.max(1L, instance.definition.intervalTicks);
                        }
                    }
                }
                store.instances.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }

        public int activeCount() {
            synchronized (store) {
                return store.activeCount();
            }
        }

        public int completedCount() {
            return completedCount;
        }

        public int tickCount() {
            return tickCount;
        }

        public int startCount() {
            return startCount;
        }

        public int cancelCount() {
            return cancelCount;
        }

        public int reset(String timerId, String scopeKey) {
            synchronized (store) {
                return store.reset(timerId, scopeKey, "test");
            }
        }

        public TimerOperationResult cancel(String timerId, String scopeKey) {
            String safeTimerId = TimerStore.normalizeId(timerId);
            String safeScopeKey = safe(scopeKey);
            synchronized (store) {
                Map<String, TimerRuntimeInstance> active = store.instances.get(safeTimerId);
                TimerRuntimeInstance removed = active == null ? null : active.remove(safeScopeKey);
                if (active != null && active.isEmpty()) {
                    store.instances.remove(safeTimerId);
                }
                if (removed == null) {
                    return TimerOperationResult.success("timer_not_running", "Timer 当前没有运行中实例，取消按 no-op success 处理。", safeTimerId, safeScopeKey, false);
                }
                if (!removed.definition.onCancelActions.isEmpty()) {
                    cancelCount++;
                }
                return TimerOperationResult.success("timer_cancelled", "Timer 已取消，不会执行 onCompleteActions。", safeTimerId, safeScopeKey, true);
            }
        }

        public void tickActual(long gameTime) {
            synchronized (store) {
                store.tick(null, gameTime);
            }
        }

        public TimerStatusSnapshot status(TimerDefinition definition, long gameTime) {
            TimerDefinition timer = definition == null ? new TimerDefinition().normalized() : definition.normalized();
            synchronized (store) {
                List<Map<String, Object>> instances = new ArrayList<>();
                Map<String, TimerRuntimeInstance> active = store.instances.getOrDefault(timer.id, Map.of());
                String lastResult = store.diagnosticResult(timer.id);
                String lastFailure = store.diagnosticFailure(timer.id);
                for (TimerRuntimeInstance instance : active.values()) {
                    instances.add(instance.toMap(gameTime));
                    if (!instance.lastResult.isBlank()) {
                        lastResult = instance.lastResult;
                    }
                    if (!instance.lastFailureReason.isBlank()) {
                        lastFailure = instance.lastFailureReason;
                    }
                }
                return new TimerStatusSnapshot(
                        timer.id,
                        timer.enabled,
                        timer.mode.name(),
                        timer.scopeMode.name(),
                        timer.startPolicy.name(),
                        active.size(),
                        gameTime,
                        lastResult,
                        lastFailure,
                        List.copyOf(instances),
                        false
                );
            }
        }
    }
}
