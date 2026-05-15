package com.zcpu.tzzmod.condition.state;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class StateVariableStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "state_variables.json";
    private static final String LABEL = "state variables";

    private StateVariableStore() {
    }

    public static Path path(MinecraftServer server) {
        return WebAdminStoragePaths.resolve(server).directory().resolve(FILE_NAME);
    }

    public static StateVariableSnapshot getSnapshot(MinecraftServer server) {
        return loadSnapshot(path(server));
    }

    public static StateVariableWriteResult set(MinecraftServer server, StateVariableUpdateRequest request, String actor) {
        return new StateVariableService(path(server)).set(request, actor);
    }

    public static StateVariableWriteResult remove(MinecraftServer server, StateVariableScope scope, String targetId, String key, String expectedFingerprint) {
        return new StateVariableService(path(server)).remove(scope, targetId, key, expectedFingerprint);
    }

    public static void flushDirty(MinecraftServer server) {
        // 8.2 writes are flushed synchronously by StateVariableService.
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

    public static boolean saveSnapshot(Path path, StateVariableSnapshot snapshot) {
        DataFile dataFile = new DataFile();
        dataFile.version = DATA_VERSION;
        dataFile.variables = new ArrayList<>(snapshot == null ? List.of() : snapshot.records());
        return JsonStoreSupport.write(path, dataFile, LABEL);
    }

    public static final class DataFile {
        public int version = DATA_VERSION;
        public List<StateVariableRecord> variables = new ArrayList<>();
    }
}
