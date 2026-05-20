package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminChannelMetadataStore {
    private WebAdminChannelMetadataStore() {
    }

    public static Path path(MinecraftServer server) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        paths.ensureDirectory();
        return paths.channelMetadataPath();
    }

    public static synchronized MetadataFile load(MinecraftServer server) {
        return load(path(server));
    }

    public static synchronized MetadataFile load(Path path) {
        MetadataFile file = JsonStoreSupport.readOrDefault(
                path,
                MetadataFile.class,
                MetadataFile::new,
                "web admin channel metadata"
        );
        return file.normalized();
    }

    public static synchronized boolean save(MinecraftServer server, MetadataFile file) {
        return save(path(server), file);
    }

    public static synchronized boolean save(Path path, MetadataFile file) {
        MetadataFile safeFile = file == null ? new MetadataFile() : file.normalized();
        return JsonStoreSupport.write(path, safeFile, "web admin channel metadata");
    }

    public static final class MetadataFile {
        public Map<String, MetadataEntry> channels = new LinkedHashMap<>();

        public MetadataFile normalized() {
            MetadataFile copy = new MetadataFile();
            if (channels != null) {
                for (Map.Entry<String, MetadataEntry> entry : channels.entrySet()) {
                    String channel = SignalChannel.normalize(entry.getKey());
                    if (channel.isBlank()) {
                        continue;
                    }
                    copy.channels.put(channel, MetadataEntry.normalized(channel, entry.getValue()));
                }
            }
            return copy;
        }
    }

    public static final class MetadataEntry {
        public String channel = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public String updatedAt = "";
        public String updatedBy = "";
        public long version = 0L;

        public static MetadataEntry normalized(String fallbackChannel, MetadataEntry raw) {
            MetadataEntry entry = raw == null ? new MetadataEntry() : raw;
            MetadataEntry copy = new MetadataEntry();
            String normalizedChannel = SignalChannel.normalize(entry.channel);
            copy.channel = normalizedChannel.isBlank() ? SignalChannel.normalize(fallbackChannel) : normalizedChannel;
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
