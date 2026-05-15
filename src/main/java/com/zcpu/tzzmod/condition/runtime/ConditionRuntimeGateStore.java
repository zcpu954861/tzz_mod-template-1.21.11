package com.zcpu.tzzmod.condition.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class ConditionRuntimeGateStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "condition_runtime_gates.json";
    public static final String STORE_UNAVAILABLE_GROUP_ID = "__condition_runtime_gate_store_unavailable__";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConditionRuntimeGateStore() {
    }

    public static Path path(MinecraftServer server) {
        return path(server, true);
    }

    private static Path path(MinecraftServer server, boolean ensureDirectory) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        if (ensureDirectory) {
            paths.ensureDirectory();
        }
        return paths.directory().resolve(FILE_NAME);
    }

    public static synchronized ConditionRuntimeGateFile load(MinecraftServer server) {
        return load(path(server, false));
    }

    public static synchronized ConditionRuntimeGateFile load(Path path) {
        return loadWithStatus(path).file();
    }

    public static synchronized ConditionRuntimeGateLoadResult loadWithStatus(MinecraftServer server) {
        return loadWithStatus(path(server, false));
    }

    public static synchronized ConditionRuntimeGateLoadResult loadWithStatus(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return new ConditionRuntimeGateLoadResult(new ConditionRuntimeGateFile(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ConditionRuntimeGateFile raw = GSON.fromJson(reader, ConditionRuntimeGateFile.class);
                return new ConditionRuntimeGateLoadResult((raw == null ? new ConditionRuntimeGateFile() : raw).normalized(), false, "");
            }
        } catch (Exception exception) {
            return new ConditionRuntimeGateLoadResult(
                    new ConditionRuntimeGateFile(),
                    true,
                    "条件组运行时 gate 配置读取失败，已安全阻断相关触发：" + exception.getMessage()
            );
        }
    }

    public static synchronized boolean save(MinecraftServer server, ConditionRuntimeGateFile file) {
        return save(path(server, true), file);
    }

    public static synchronized boolean save(Path path, ConditionRuntimeGateFile file) {
        return JsonStoreSupport.write(path, file == null ? new ConditionRuntimeGateFile() : file.normalized(), "condition runtime gates");
    }

    public static synchronized VirtualBlockDeviceGateConfig virtualBlockDevice(MinecraftServer server, String deviceId) {
        return load(server).virtualBlockDevices.getOrDefault(safe(deviceId), VirtualBlockDeviceGateConfig.empty()).normalized();
    }

    public static synchronized boolean updateVirtualBlockDevice(
            MinecraftServer server,
            String deviceId,
            VirtualBlockDeviceGateConfig config
    ) {
        String safeDeviceId = safe(deviceId);
        ConditionRuntimeGateLoadResult loaded = loadWithStatus(server);
        if (loaded.degraded()) {
            return false;
        }
        ConditionRuntimeGateFile file = loaded.file();
        VirtualBlockDeviceGateConfig normalized = (config == null ? VirtualBlockDeviceGateConfig.empty() : config).normalized();
        if (safeDeviceId.isBlank()) {
            return false;
        }
        if (normalized.isEmpty()) {
            file.virtualBlockDevices.remove(safeDeviceId);
        } else {
            file.virtualBlockDevices.put(safeDeviceId, normalized);
        }
        return save(server, file);
    }

    public static synchronized String conditionGroupId(MinecraftServer server, String deviceId, ConditionRuntimeTargetType targetType) {
        ConditionRuntimeGateLoadResult loaded = loadWithStatus(server);
        if (loaded.degraded()) {
            return STORE_UNAVAILABLE_GROUP_ID;
        }
        return loaded.file().virtualBlockDevices.getOrDefault(safe(deviceId), VirtualBlockDeviceGateConfig.empty()).normalized().conditionGroupId(targetType);
    }

    public record ConditionRuntimeGateLoadResult(
            ConditionRuntimeGateFile file,
            boolean degraded,
            String message
    ) {
        public ConditionRuntimeGateLoadResult {
            file = file == null ? new ConditionRuntimeGateFile() : file.normalized();
            message = safe(message);
        }
    }

    public static final class ConditionRuntimeGateFile {
        public int version = DATA_VERSION;
        public Map<String, VirtualBlockDeviceGateConfig> virtualBlockDevices = new LinkedHashMap<>();

        public ConditionRuntimeGateFile normalized() {
            ConditionRuntimeGateFile copy = new ConditionRuntimeGateFile();
            copy.version = DATA_VERSION;
            if (virtualBlockDevices != null) {
                for (Map.Entry<String, VirtualBlockDeviceGateConfig> entry : virtualBlockDevices.entrySet()) {
                    String deviceId = safe(entry.getKey());
                    VirtualBlockDeviceGateConfig config = entry.getValue() == null ? VirtualBlockDeviceGateConfig.empty() : entry.getValue().normalized();
                    if (!deviceId.isBlank() && !config.isEmpty()) {
                        copy.virtualBlockDevices.put(deviceId, config);
                    }
                }
            }
            return copy;
        }
    }

    public record VirtualBlockDeviceGateConfig(
            String redstoneConditionGroupId,
            String blockStateConditionGroupId,
            String interactionConditionGroupId,
            String itemSubmitConditionGroupId,
            String containerOpenConditionGroupId,
            String containerCloseConditionGroupId,
            String containerChangeConditionGroupId
    ) {
        public VirtualBlockDeviceGateConfig {
            redstoneConditionGroupId = normalizeGroupId(redstoneConditionGroupId);
            blockStateConditionGroupId = normalizeGroupId(blockStateConditionGroupId);
            interactionConditionGroupId = normalizeGroupId(interactionConditionGroupId);
            itemSubmitConditionGroupId = normalizeGroupId(itemSubmitConditionGroupId);
            containerOpenConditionGroupId = normalizeGroupId(containerOpenConditionGroupId);
            containerCloseConditionGroupId = normalizeGroupId(containerCloseConditionGroupId);
            containerChangeConditionGroupId = normalizeGroupId(containerChangeConditionGroupId);
        }

        public static VirtualBlockDeviceGateConfig empty() {
            return new VirtualBlockDeviceGateConfig("", "", "", "", "", "", "");
        }

        public VirtualBlockDeviceGateConfig normalized() {
            return new VirtualBlockDeviceGateConfig(
                    redstoneConditionGroupId,
                    blockStateConditionGroupId,
                    interactionConditionGroupId,
                    itemSubmitConditionGroupId,
                    containerOpenConditionGroupId,
                    containerCloseConditionGroupId,
                    containerChangeConditionGroupId
            );
        }

        public boolean isEmpty() {
            return redstoneConditionGroupId.isBlank()
                    && blockStateConditionGroupId.isBlank()
                    && interactionConditionGroupId.isBlank()
                    && itemSubmitConditionGroupId.isBlank()
                    && containerOpenConditionGroupId.isBlank()
                    && containerCloseConditionGroupId.isBlank()
                    && containerChangeConditionGroupId.isBlank();
        }

        public String conditionGroupId(ConditionRuntimeTargetType targetType) {
            if (targetType == null) {
                return "";
            }
            return switch (targetType) {
                case VBD_REDSTONE -> redstoneConditionGroupId;
                case VBD_BLOCKSTATE -> blockStateConditionGroupId;
                case VBD_INTERACTION -> interactionConditionGroupId;
                case ITEM_SUBMIT -> itemSubmitConditionGroupId;
                case CONTAINER_OPEN -> containerOpenConditionGroupId;
                case CONTAINER_CLOSE -> containerCloseConditionGroupId;
                case CONTAINER_CHANGE -> containerChangeConditionGroupId;
            };
        }

        public Map<String, Object> summary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("redstoneConditionGroupId", redstoneConditionGroupId);
            summary.put("blockStateConditionGroupId", blockStateConditionGroupId);
            summary.put("interactionConditionGroupId", interactionConditionGroupId);
            summary.put("itemSubmitConditionGroupId", itemSubmitConditionGroupId);
            summary.put("containerOpenConditionGroupId", containerOpenConditionGroupId);
            summary.put("containerCloseConditionGroupId", containerCloseConditionGroupId);
            summary.put("containerChangeConditionGroupId", containerChangeConditionGroupId);
            return Map.copyOf(summary);
        }
    }

    private static String normalizeGroupId(String value) {
        return WebAdminConditionGroupStore.normalizeId(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
