package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
