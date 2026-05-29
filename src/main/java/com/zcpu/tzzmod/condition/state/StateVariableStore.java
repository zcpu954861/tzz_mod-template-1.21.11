package com.zcpu.tzzmod.condition.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.core.storage.JsonLoadCacheSupport;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.util.JsonNullability;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class StateVariableStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "state_variables.json";
    private static final String LABEL = "state variables";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LOAD_CACHE_MAX_ENTRIES = 32;
    private static final Map<Path, CachedSnapshot> SNAPSHOT_CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, CachedSnapshot> eldest) {
            return size() > LOAD_CACHE_MAX_ENTRIES;
        }
    };
    private static final Map<Path, CachedLoadResult> STATUS_CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, CachedLoadResult> eldest) {
            return size() > LOAD_CACHE_MAX_ENTRIES;
        }
    };

    private StateVariableStore() {
    }

    public static Path path(MinecraftServer server) {
        return WebAdminStoragePaths.resolve(server).directory().resolve(FILE_NAME);
    }

    public static StateVariableSnapshot getSnapshot(MinecraftServer server) {
        return new StateVariableService(path(server)).snapshot();
    }

    public static StateVariableLoadResult getSnapshotWithStatus(MinecraftServer server) {
        return new StateVariableService(path(server)).snapshotWithStatus();
    }

    public static StateVariableWriteResult set(MinecraftServer server, StateVariableUpdateRequest request, String actor) {
        return new StateVariableService(path(server)).set(request, actor);
    }

    public static StateVariableWriteResult remove(MinecraftServer server, StateVariableScope scope, String targetId, String key, String expectedFingerprint) {
        return new StateVariableService(path(server)).remove(scope, targetId, key, expectedFingerprint);
    }

    public static StateVariableMutationResult mutate(MinecraftServer server, StateVariableMutationRequest request, String actor) {
        return new StateVariableService(path(server)).mutate(request, actor);
    }

    public static void flushDirty(MinecraftServer server) {
        // 8.2 writes are flushed synchronously by StateVariableService.
    }

    public static synchronized StateVariableSnapshot loadSnapshotCached(Path path) {
        Path key = JsonLoadCacheSupport.cacheKey(path);
        JsonLoadCacheSupport.FileFingerprint fingerprint = JsonLoadCacheSupport.fingerprint(path);
        CachedSnapshot cached = SNAPSHOT_CACHE.get(key);
        if (cached != null && cached.fingerprint.equals(fingerprint)) {
            return cached.snapshot;
        }
        StateVariableSnapshot snapshot = loadSnapshot(path);
        SNAPSHOT_CACHE.put(key, new CachedSnapshot(JsonLoadCacheSupport.fingerprint(path), snapshot));
        return snapshot;
    }

    public static StateVariableSnapshot loadSnapshot(Path path) {
        DataFile dataFile = JsonStoreSupport.readOrDefault(path, DataFile.class, DataFile::new, LABEL);
        if (dataFile == null || dataFile.variables == null) {
            return StateVariableSnapshot.empty();
        }
        try {
            return new StateVariableSnapshot(dataFile.variables);
        } catch (RuntimeException ex) {
            return StateVariableSnapshot.empty();
        }
    }

    public static StateVariableLoadResult loadSnapshotWithStatus(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                DataFile dataFile = JsonNullability.fromJsonNullable(GSON, reader, DataFile.class);
                if (dataFile == null || dataFile.variables == null) {
                    return new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", true);
                }
                int rawCount = dataFile.variables.size();
                StateVariableSnapshot snapshot = new StateVariableSnapshot(dataFile.variables);
                boolean degraded = snapshot.size() != rawCount;
                String message = degraded ? "状态变量配置中存在无法读取的记录，已跳过损坏项。" : "";
                return new StateVariableLoadResult(snapshot, degraded, message, true);
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load state variables without creating file: {}", exception.getMessage());
            return new StateVariableLoadResult(
                    StateVariableSnapshot.empty(),
                    true,
                    "状态变量配置文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage(),
                    true
            );
        }
    }

    public static synchronized StateVariableLoadResult loadSnapshotWithStatusCached(Path path) {
        Path key = JsonLoadCacheSupport.cacheKey(path);
        JsonLoadCacheSupport.FileFingerprint fingerprint = JsonLoadCacheSupport.fingerprint(path);
        CachedLoadResult cached = STATUS_CACHE.get(key);
        if (cached != null && cached.fingerprint.equals(fingerprint)) {
            return cached.result;
        }
        StateVariableLoadResult loaded = loadSnapshotWithStatus(path);
        STATUS_CACHE.put(key, new CachedLoadResult(JsonLoadCacheSupport.fingerprint(path), loaded));
        return loaded;
    }

    public static boolean saveSnapshot(Path path, StateVariableSnapshot snapshot) {
        DataFile dataFile = new DataFile();
        dataFile.version = DATA_VERSION;
        dataFile.variables = new ArrayList<>(snapshot == null ? List.of() : snapshot.records());
        boolean saved = JsonStoreSupport.write(path, dataFile, LABEL);
        if (saved) {
            clearCachedLoad(path);
        }
        return saved;
    }

    public static synchronized void clearCachedLoad(MinecraftServer server) {
        clearCachedLoad(path(server));
    }

    public static synchronized void clearCachedLoad(Path path) {
        Path key = JsonLoadCacheSupport.cacheKey(path);
        SNAPSHOT_CACHE.remove(key);
        STATUS_CACHE.remove(key);
    }

    public static final class DataFile {
        public int version = DATA_VERSION;
        public List<StateVariableRecord> variables = new ArrayList<>();
    }

    public record StateVariableLoadResult(
            StateVariableSnapshot snapshot,
            boolean degraded,
            String message,
            boolean filePresent
    ) {
        public StateVariableLoadResult {
            snapshot = snapshot == null ? StateVariableSnapshot.empty() : snapshot;
            message = message == null ? "" : message;
        }
    }

    private record CachedSnapshot(JsonLoadCacheSupport.FileFingerprint fingerprint, StateVariableSnapshot snapshot) {
    }

    private record CachedLoadResult(JsonLoadCacheSupport.FileFingerprint fingerprint, StateVariableLoadResult result) {
    }
}
