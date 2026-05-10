package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminDeviceMetadataStore {
    private WebAdminDeviceMetadataStore() {
    }

    public static Path path(MinecraftServer server) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        paths.ensureDirectory();
        return paths.deviceMetadataPath();
    }

    public static synchronized MetadataFile load(MinecraftServer server) {
        MetadataFile file = JsonStoreSupport.readOrDefault(
                path(server),
                MetadataFile.class,
                MetadataFile::new,
                "web admin device metadata"
        );
        return file.normalized();
    }

    public static synchronized boolean save(MinecraftServer server, MetadataFile file) {
        MetadataFile safeFile = file == null ? new MetadataFile() : file.normalized();
        return JsonStoreSupport.write(path(server), safeFile, "web admin device metadata");
    }

    public static synchronized boolean removeDevice(MinecraftServer server, String deviceId) {
        String safeDeviceId = safe(deviceId);
        if (safeDeviceId.isBlank()) {
            return false;
        }
        MetadataFile file = load(server);
        if (!file.devices.containsKey(safeDeviceId)) {
            return false;
        }
        file.devices.remove(safeDeviceId);
        return save(server, file);
    }

    public static synchronized boolean removeDeviceAliases(MinecraftServer server, String deviceId, String deviceType) {
        List<String> keys = metadataKeys(deviceId, deviceType);
        if (keys.isEmpty()) {
            return false;
        }
        MetadataFile file = load(server);
        boolean removed = false;
        for (String key : keys) {
            removed |= file.devices.remove(key) != null;
        }
        return removed && save(server, file);
    }

    public static String metadataKey(String deviceId, String deviceType) {
        String id = stripKnownTypePrefix(safe(deviceId).trim());
        String type = safe(deviceType).trim().toLowerCase();
        if (id.isBlank()) {
            return "";
        }
        return isPhysicalDeviceType(type) ? type + ":" + id : id;
    }

    public static List<String> metadataKeys(String deviceId, String deviceType) {
        String id = safe(deviceId).trim();
        if (id.isBlank()) {
            return List.of();
        }
        String untyped = stripKnownTypePrefix(id);
        Set<String> keys = new LinkedHashSet<>();
        String primary = metadataKey(id, deviceType);
        if (!primary.isBlank()) {
            keys.add(primary);
        }
        keys.add(id);
        keys.add(untyped);
        for (String type : List.of("signal_emitter", "signal_receiver", "action_relay")) {
            keys.add(type + ":" + untyped);
        }
        return List.copyOf(keys);
    }

    private static boolean isPhysicalDeviceType(String type) {
        return "signal_emitter".equals(type) || "signal_receiver".equals(type) || "action_relay".equals(type);
    }

    private static String stripKnownTypePrefix(String value) {
        String clean = safe(value).trim();
        for (String type : List.of("signal_emitter", "signal_receiver", "action_relay", "virtual_block_device")) {
            String prefix = type + ":";
            if (clean.startsWith(prefix)) {
                return clean.substring(prefix.length());
            }
        }
        return clean;
    }

    public static final class MetadataFile {
        public Map<String, MetadataEntry> devices = new LinkedHashMap<>();

        public MetadataFile normalized() {
            MetadataFile copy = new MetadataFile();
            if (devices != null) {
                for (Map.Entry<String, MetadataEntry> entry : devices.entrySet()) {
                    String deviceId = safe(entry.getKey());
                    if (deviceId.isBlank()) {
                        continue;
                    }
                    copy.devices.put(deviceId, MetadataEntry.normalized(deviceId, entry.getValue()));
                }
            }
            return copy;
        }
    }

    public static final class MetadataEntry {
        public String deviceId = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public String updatedAt = "";
        public String updatedBy = "";
        public long version = 0L;

        public static MetadataEntry normalized(String fallbackDeviceId, MetadataEntry raw) {
            MetadataEntry entry = raw == null ? new MetadataEntry() : raw;
            MetadataEntry copy = new MetadataEntry();
            copy.deviceId = safe(entry.deviceId).isBlank() ? safe(fallbackDeviceId) : safe(entry.deviceId);
            copy.displayName = safe(entry.displayName).trim();
            copy.note = safe(entry.note).trim();
            copy.iconKey = safe(entry.iconKey).isBlank() ? "auto" : safe(entry.iconKey).trim();
            copy.updatedAt = safe(entry.updatedAt);
            copy.updatedBy = safe(entry.updatedBy);
            copy.version = Math.max(0L, entry.version);
            return copy;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
