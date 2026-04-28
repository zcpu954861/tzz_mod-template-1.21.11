package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class SignalListenerStore {
    private static final Map<MinecraftServer, State> CACHE = new WeakHashMap<>();

    private SignalListenerStore() {
    }

    public static synchronized List<SignalListenerData> getSnapshot(MinecraftServer server) {
        return List.copyOf(getState(server).listeners);
    }

    public static synchronized SignalListenerData getListener(MinecraftServer server, String listenerRef) {
        ResolveResult result = resolveListener(server, listenerRef);
        return result.listener();
    }

    public static synchronized List<SignalListenerData> getEnabledListenersForChannel(MinecraftServer server, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalListenerData> result = new ArrayList<>();
        for (SignalListenerData listener : getState(server).listeners) {
            if (listener.enabled() && listener.channel().equals(normalizedChannel)) {
                result.add(listener);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized SignalListenerData createListener(MinecraftServer server, String channel, String name) {
        State state = getState(server);
        SignalListenerData listener = new SignalListenerData(
                UUID.randomUUID().toString(),
                cleanUserText(name),
                SignalChannel.normalize(channel),
                true,
                SignalListenerData.DEFAULT_COOLDOWN_TICKS,
                List.of()
        ).normalized();
        state.listeners.add(listener);
        state.markDirty();
        return listener;
    }

    public static synchronized boolean deleteListener(MinecraftServer server, String listenerRef) {
        ResolveResult resolved = resolveListener(server, listenerRef);
        if (!resolved.foundUnique()) {
            return false;
        }
        State state = getState(server);
        boolean removed = state.listeners.removeIf(listener -> listener.id().equals(resolved.listener().id()));
        if (removed) {
            state.markDirty();
        }
        return removed;
    }

    public static synchronized boolean setEnabled(MinecraftServer server, String listenerRef, boolean enabled) {
        return replace(server, listenerRef, listener -> new SignalListenerData(
                listener.id(),
                listener.name(),
                listener.channel(),
                enabled,
                listener.cooldownTicks(),
                listener.actions()
        ).normalized());
    }

    public static synchronized boolean setCooldown(MinecraftServer server, String listenerRef, int ticks) {
        return replace(server, listenerRef, listener -> new SignalListenerData(
                listener.id(),
                listener.name(),
                listener.channel(),
                listener.enabled(),
                ticks,
                listener.actions()
        ).normalized());
    }

    public static synchronized SignalListenerData updateBasicConfigForWebAdmin(
            MinecraftServer server,
            String listenerRef,
            boolean enabled,
            String channel,
            int cooldownTicks
    ) {
        return replaceReturning(server, listenerRef, listener -> withBasicConfigForWebAdmin(listener, enabled, channel, cooldownTicks));
    }

    public static SignalListenerData withBasicConfigForWebAdmin(
            SignalListenerData listener,
            boolean enabled,
            String channel,
            int cooldownTicks
    ) {
        if (listener == null) {
            return null;
        }
        return new SignalListenerData(
                listener.id(),
                listener.name(),
                SignalChannel.normalize(channel),
                enabled,
                cooldownTicks,
                listener.actions()
        ).normalized();
    }

    public static synchronized boolean addAction(MinecraftServer server, String listenerRef, ActionConfig action) {
        if (action == null) {
            return false;
        }
        return replace(server, listenerRef, listener -> new SignalListenerData(
                listener.id(),
                listener.name(),
                listener.channel(),
                listener.enabled(),
                listener.cooldownTicks(),
                appendAction(listener.actions(), action)
        ).normalized());
    }

    public static synchronized boolean clearActions(MinecraftServer server, String listenerRef) {
        return replace(server, listenerRef, listener -> new SignalListenerData(
                listener.id(),
                listener.name(),
                listener.channel(),
                listener.enabled(),
                listener.cooldownTicks(),
                List.of()
        ).normalized());
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        State state = CACHE.get(server);
        if (state != null) {
            state.flushDirty();
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static synchronized ResolveResult resolveListener(MinecraftServer server, String listenerRef) {
        if (listenerRef == null || listenerRef.isBlank()) {
            return ResolveResult.none();
        }

        String query = cleanUserText(listenerRef);
        for (SignalListenerData listener : getState(server).listeners) {
            if (listener.id().equals(query)) {
                return ResolveResult.unique(listener);
            }
        }

        String shortQuery = query.endsWith("...") ? query.substring(0, query.length() - 3) : query;
        List<SignalListenerData> matches = new ArrayList<>();
        for (SignalListenerData listener : getState(server).listeners) {
            if (cleanUserText(listener.name()).equals(query)
                    || shortId(listener.id()).equals(query)
                    || (shortQuery.length() >= 8 && listener.id().startsWith(shortQuery))) {
                matches.add(listener);
            }
        }

        if (matches.isEmpty()) {
            return ResolveResult.none();
        }
        if (matches.size() == 1) {
            return ResolveResult.unique(matches.get(0));
        }
        return ResolveResult.ambiguous(List.copyOf(matches));
    }

    private static List<ActionConfig> appendAction(List<ActionConfig> actions, ActionConfig action) {
        List<ActionConfig> copy = new ArrayList<>(actions == null ? List.of() : actions);
        copy.add(action);
        return List.copyOf(copy);
    }

    private static boolean replace(MinecraftServer server, String listenerRef, Function<SignalListenerData, SignalListenerData> updater) {
        return replaceReturning(server, listenerRef, updater) != null;
    }

    private static SignalListenerData replaceReturning(MinecraftServer server, String listenerRef, Function<SignalListenerData, SignalListenerData> updater) {
        ResolveResult resolved = resolveListener(server, listenerRef);
        if (!resolved.foundUnique()) {
            return null;
        }
        State state = getState(server);
        for (int i = 0; i < state.listeners.size(); i++) {
            SignalListenerData listener = state.listeners.get(i);
            if (!listener.id().equals(resolved.listener().id())) {
                continue;
            }
            SignalListenerData updated = updater.apply(listener).normalized();
            state.listeners.set(i, updated);
            state.markDirty();
            return updated;
        }
        return null;
    }

    private static State getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, SignalListenerStore::load);
    }

    private static State load(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("tzz_mod")
                .resolve("signal_listeners.json");
        State state = new State(path);
        DataFile dataFile = JsonStoreSupport.readOrDefault(path, DataFile.class, DataFile::new, "signal listeners");
        if (dataFile.listeners != null) {
            for (SignalListenerData listener : dataFile.listeners) {
                if (listener == null) {
                    continue;
                }
                SignalListenerData normalized = listener.normalized();
                if (!normalized.id().isBlank() && SignalChannel.isValid(normalized.channel())) {
                    state.listeners.add(normalized);
                }
            }
        }
        return state;
    }

    public static String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "未知";
        }
        return id.length() <= 8 ? id : id.substring(0, 8) + "...";
    }

    private static String cleanUserText(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    public static final class DataFile {
        public int version = 1;
        public List<SignalListenerData> listeners = new ArrayList<>();
    }

    public record ResolveResult(
            SignalListenerData listener,
            List<SignalListenerData> matches,
            boolean ambiguous
    ) {
        public static ResolveResult none() {
            return new ResolveResult(null, List.of(), false);
        }

        public static ResolveResult unique(SignalListenerData listener) {
            return new ResolveResult(listener, List.of(listener), false);
        }

        public static ResolveResult ambiguous(List<SignalListenerData> matches) {
            return new ResolveResult(null, List.copyOf(matches), true);
        }

        public boolean foundUnique() {
            return listener != null && !ambiguous;
        }
    }

    private static final class State {
        private final Path path;
        private final List<SignalListenerData> listeners = new ArrayList<>();
        private boolean dirty;

        private State(Path path) {
            this.path = path;
        }

        private void markDirty() {
            dirty = true;
        }

        private void flushDirty() {
            if (!dirty) {
                return;
            }
            DataFile dataFile = new DataFile();
            dataFile.listeners = new ArrayList<>(listeners.size());
            for (SignalListenerData listener : listeners) {
                dataFile.listeners.add(listener.normalized());
            }
            if (JsonStoreSupport.write(path, dataFile, "signal listeners")) {
                dirty = false;
            }
        }
    }
}
