package com.zcpu.tzzmod.signal.join;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;

public final class SignalJoinRuntimeService {
    private static final Map<MinecraftServer, RuntimeStore> STORES = new WeakHashMap<>();
    private static final ThreadLocal<LinkedHashSet<String>> ACTIVE_JOIN_IDS = ThreadLocal.withInitial(LinkedHashSet::new);

    private SignalJoinRuntimeService() {
    }

    public static void observeAcceptedSignal(SignalEvent event, String rawChannel, int depth) {
        if (event == null || event.world() == null || event.world().getServer() == null) {
            return;
        }
        MinecraftServer server = event.world().getServer();
        SignalJoinStore.SignalJoinLoadResult loaded = SignalJoinStore.loadWithStatus(server);
        if (loaded.degraded()) {
            runtimeStore(server).setDiagnostic("", "store_degraded", loaded.message(), event.gameTime());
            return;
        }
        if (loaded.file().joins.isEmpty()) {
            return;
        }
        String channel = SignalChannel.normalize(rawChannel);
        List<OutputDecision> outputs = observe(runtimeStore(server), loaded.file().joins.values().stream().toList(), EventView.from(event, channel), depth);
        for (OutputDecision output : outputs) {
            emitOutput(event, output, depth);
        }
    }

    public static SignalJoinStatusSnapshot status(MinecraftServer server, SignalJoinDefinition join, long gameTime) {
        SignalJoinDefinition normalized = join == null ? new SignalJoinDefinition() : join.normalized();
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            store.applyLazyTimeout(normalized, gameTime);
            Map<String, SignalJoinRuntimeState> states = store.statesFor(normalized.id);
            List<Map<String, Object>> scopes = new ArrayList<>();
            String lastResult = "";
            String lastFailure = store.diagnostic(normalized.id);
            int pendingScopeCount = 0;
            for (SignalJoinRuntimeState state : states.values()) {
                scopes.add(state.toMap(normalized));
                if (!state.channelHits.isEmpty() || state.latched) {
                    pendingScopeCount++;
                }
                if (!state.lastResult.isBlank()) {
                    lastResult = state.lastResult;
                }
                if (!state.lastFailureReason.isBlank()) {
                    lastFailure = state.lastFailureReason;
                }
            }
            return new SignalJoinStatusSnapshot(
                    normalized.id,
                    normalized.enabled,
                    normalized.mode.name(),
                    normalized.scopeMode.name(),
                    normalized.resetPolicy.name(),
                    pendingScopeCount,
                    gameTime,
                    lastResult,
                    lastFailure,
                    List.copyOf(scopes)
            );
        }
    }

    public static int reset(MinecraftServer server, String joinId, String scopeKey, long gameTime, String reason) {
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            return store.reset(joinId, scopeKey, gameTime, reason);
        }
    }

    public static void clearJoin(MinecraftServer server, String joinId) {
        if (server == null || joinId == null || joinId.isBlank()) {
            return;
        }
        RuntimeStore store = runtimeStore(server);
        synchronized (store) {
            store.reset(joinId, "", 0L, "config_changed");
        }
    }

    public static void clearServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (SignalJoinRuntimeService.class) {
            STORES.remove(server);
        }
    }

    public static TestRuntime testRuntime() {
        return new TestRuntime(new RuntimeStore());
    }

    static boolean isSatisfied(SignalJoinDefinition join, SignalJoinRuntimeState state) {
        if (join == null || state == null) {
            return false;
        }
        return switch (join.mode) {
            case ALL -> state.channelHits.keySet().containsAll(join.inputChannelNames());
            case ANY_N -> state.channelHits.size() >= join.threshold;
            case COUNT -> state.totalCount >= join.threshold;
        };
    }

    private static List<OutputDecision> observe(RuntimeStore store, List<SignalJoinDefinition> definitions, EventView event, int depth) {
        if (event == null || event.channel.isBlank() || definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        List<OutputDecision> outputs = new ArrayList<>();
        synchronized (store) {
            for (SignalJoinDefinition raw : definitions) {
                SignalJoinDefinition join = raw == null ? null : raw.normalized();
                if (join == null || !join.enabled || !join.referencesInput(event.channel)) {
                    continue;
                }
                if (!SignalJoinValidator.validate(join, false).isEmpty()) {
                    store.setDiagnostic(join.id, "invalid_config", "Signal Join 配置无效，已跳过运行时汇合。", event.gameTime);
                    continue;
                }
                if (ACTIVE_JOIN_IDS.get().contains(join.id)) {
                    store.setDiagnostic(join.id, "recursive_join_skipped", "Signal Join 输出链路检测到递归，已跳过本次自触发。", event.gameTime);
                    continue;
                }
                String scopeKey = scopeKey(join, event);
                if (scopeKey.isBlank()) {
                    store.setDiagnostic(join.id, "missing_player_context", "PLAYER scope 需要玩家上下文；本次 signal 没有 player，已忽略。", event.gameTime);
                    continue;
                }
                store.applyLazyTimeout(join, event.gameTime);
                SignalJoinRuntimeState state = store.state(join.id, scopeKey);
                if (state.latched && join.resetPolicy == SignalJoinResetPolicy.LATCH_UNTIL_MANUAL_RESET) {
                    state.lastResult = "LATCHED";
                    state.lastFailureReason = "Signal Join 已锁存，手动重置前不会重复输出。";
                    continue;
                }
                if (join.cooldownTicks > 0 && state.lastOutputAt > 0 && event.gameTime - state.lastOutputAt < join.cooldownTicks) {
                    state.lastResult = "COOLDOWN";
                    state.lastFailureReason = "Signal Join 输出冷却中。";
                    continue;
                }
                state.recordHit(event.channel, event.gameTime);
                if (!isSatisfied(join, state)) {
                    continue;
                }
                OutputDecision output = OutputDecision.from(join, state, event.channel, depth + 1);
                state.lastOutputAt = event.gameTime;
                state.triggerCount++;
                state.lastResult = "OUTPUT";
                state.lastFailureReason = "";
                if (join.resetPolicy == SignalJoinResetPolicy.RESET_AFTER_EMIT) {
                    state.resetPending(event.gameTime, "emit", "输出后已清空 pending state。");
                    state.lastOutputAt = event.gameTime;
                    state.triggerCount = Math.max(1L, state.triggerCount);
                    state.lastResult = "OUTPUT_RESET";
                } else {
                    state.latched = true;
                    state.lastResult = "LATCHED_OUTPUT";
                }
                outputs.add(output);
            }
        }
        return List.copyOf(outputs);
    }

    private static void emitOutput(SignalEvent source, OutputDecision output, int depth) {
        if (source == null || output == null || output.outputChannel.isBlank()) {
            return;
        }
        LinkedHashSet<String> active = ACTIVE_JOIN_IDS.get();
        if (!active.add(output.joinId)) {
            return;
        }
        try {
            ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                    output.outputChannel,
                    source.player(),
                    source.world(),
                    source.position(),
                    ActionSourceType.SIGNAL_JOIN,
                    output.joinId,
                    depth + 1,
                    source.gameTime(),
                    output.detail
            ));
            if (!result.success()) {
                Tzz_mod.LOGGER.warn("[SignalJoin] output signal failed join={} channel={} result={}", output.joinId, output.outputChannel, result.message());
            }
        } finally {
            active.remove(output.joinId);
        }
    }

    private static String scopeKey(SignalJoinDefinition join, EventView event) {
        if (join.scopeMode == SignalJoinScopeMode.GLOBAL) {
            return "global";
        }
        if (join.scopeMode == SignalJoinScopeMode.PLAYER) {
            return event.playerId;
        }
        return "global";
    }

    private static RuntimeStore runtimeStore(MinecraftServer server) {
        if (server == null) {
            return new RuntimeStore();
        }
        synchronized (SignalJoinRuntimeService.class) {
            return STORES.computeIfAbsent(server, ignored -> new RuntimeStore());
        }
    }

    private record EventView(String channel, String playerId, long gameTime) {
        private static EventView from(SignalEvent event, String channel) {
            return new EventView(
                    SignalChannel.normalize(channel),
                    event == null || event.player() == null ? "" : event.player().getUuidAsString(),
                    event == null ? 0L : event.gameTime()
            );
        }
    }

    private record OutputDecision(String joinId, String outputChannel, String detail) {
        private static OutputDecision from(SignalJoinDefinition join, SignalJoinRuntimeState state, String matchedChannel, int outputDepth) {
            String detail = "Signal Join 输出"
                    + "；joinId=" + join.id
                    + "；mode=" + join.mode.name()
                    + "；scope=" + state.scopeKey
                    + "；matched=" + String.join(",", state.channelHits.keySet())
                    + "；lastInput=" + matchedChannel
                    + "；totalCount=" + state.totalCount
                    + "；depth=" + outputDepth;
            return new OutputDecision(join.id, join.outputChannel, detail);
        }
    }

    private static final class RuntimeStore {
        private final Map<String, Map<String, SignalJoinRuntimeState>> states = new LinkedHashMap<>();
        private final Map<String, String> diagnostics = new LinkedHashMap<>();

        private SignalJoinRuntimeState state(String joinId, String scopeKey) {
            return states.computeIfAbsent(joinId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(scopeKey, ignored -> new SignalJoinRuntimeState(joinId, scopeKey));
        }

        private Map<String, SignalJoinRuntimeState> statesFor(String joinId) {
            return states.getOrDefault(joinId, Map.of());
        }

        private void setDiagnostic(String joinId, String code, String message, long gameTime) {
            String safeId = joinId == null || joinId.isBlank() ? "global" : joinId;
            diagnostics.put(safeId, message == null || message.isBlank() ? code : message);
        }

        private String diagnostic(String joinId) {
            return diagnostics.getOrDefault(joinId == null || joinId.isBlank() ? "global" : joinId, "");
        }

        private void applyLazyTimeout(SignalJoinDefinition join, long gameTime) {
            if (join == null || join.timeoutTicks <= 0) {
                return;
            }
            Map<String, SignalJoinRuntimeState> joinStates = states.get(join.id);
            if (joinStates == null) {
                return;
            }
            for (SignalJoinRuntimeState state : joinStates.values()) {
                if (state.latched && join.resetPolicy == SignalJoinResetPolicy.LATCH_UNTIL_MANUAL_RESET) {
                    continue;
                }
                if (state.firstMatchedAt > 0 && gameTime - state.firstMatchedAt >= join.timeoutTicks) {
                    state.resetPending(gameTime, "timeout", "Signal Join pending state 已超时，已在本次事件或查询时清理。");
                }
            }
        }

        private int reset(String rawJoinId, String rawScopeKey, long gameTime, String reason) {
            String joinId = SignalJoinStore.normalizeId(rawJoinId);
            String scopeKey = rawScopeKey == null ? "" : rawScopeKey.trim();
            if (joinId.isBlank()) {
                int count = states.values().stream().mapToInt(Map::size).sum();
                states.clear();
                diagnostics.clear();
                return count;
            }
            Map<String, SignalJoinRuntimeState> joinStates = states.get(joinId);
            if (joinStates == null || joinStates.isEmpty()) {
                diagnostics.remove(joinId);
                return 0;
            }
            if (scopeKey.isBlank()) {
                int count = joinStates.size();
                states.remove(joinId);
                diagnostics.remove(joinId);
                return count;
            }
            SignalJoinRuntimeState removed = joinStates.remove(scopeKey);
            if (joinStates.isEmpty()) {
                states.remove(joinId);
            }
            return removed == null ? 0 : 1;
        }
    }

    public static final class TestRuntime {
        private final RuntimeStore store;

        private TestRuntime(RuntimeStore store) {
            this.store = store;
        }

        public List<String> observe(List<SignalJoinDefinition> definitions, String channel, String playerId, long gameTime) {
            List<OutputDecision> outputs = SignalJoinRuntimeService.observe(store, definitions, new EventView(SignalChannel.normalize(channel), playerId == null ? "" : playerId, gameTime), 0);
            List<String> result = new ArrayList<>();
            for (OutputDecision output : outputs) {
                result.add(output.outputChannel);
            }
            return List.copyOf(result);
        }

        public SignalJoinStatusSnapshot status(SignalJoinDefinition join, long gameTime) {
            synchronized (store) {
                store.applyLazyTimeout(join.normalized(), gameTime);
                Map<String, SignalJoinRuntimeState> states = new LinkedHashMap<>(store.statesFor(join.normalized().id));
                List<Map<String, Object>> scopes = new ArrayList<>();
                int pendingScopeCount = 0;
                for (SignalJoinRuntimeState state : states.values()) {
                    scopes.add(state.toMap(join.normalized()));
                    if (!state.channelHits.isEmpty() || state.latched) {
                        pendingScopeCount++;
                    }
                }
                return new SignalJoinStatusSnapshot(join.normalized().id, join.enabled, join.mode.name(), join.scopeMode.name(), join.resetPolicy.name(), pendingScopeCount, gameTime, "", store.diagnostic(join.normalized().id), List.copyOf(scopes));
            }
        }

        public int reset(String joinId, String scopeKey) {
            synchronized (store) {
                return store.reset(joinId, scopeKey, 0L, "manual");
            }
        }
    }
}
