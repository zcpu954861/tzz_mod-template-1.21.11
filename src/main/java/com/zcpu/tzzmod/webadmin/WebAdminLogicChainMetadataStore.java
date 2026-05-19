package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminLogicChainMetadataStore {
    // WebAdmin-only metadata: display preferences for read-only logic-chain views, never runtime wiring.
    private WebAdminLogicChainMetadataStore() {
    }

    public static Path path(MinecraftServer server) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        paths.ensureDirectory();
        return paths.directory().resolve("web_admin_logic_chain_metadata.json");
    }

    public static synchronized MetadataFile load(MinecraftServer server) {
        MetadataFile file = JsonStoreSupport.readOrDefault(
                path(server),
                MetadataFile.class,
                MetadataFile::new,
                "web admin logic chain metadata"
        );
        return file.normalized();
    }

    public static synchronized boolean save(MinecraftServer server, MetadataFile file) {
        MetadataFile safeFile = file == null ? new MetadataFile() : file.normalized();
        return JsonStoreSupport.write(path(server), safeFile, "web admin logic chain metadata");
    }

    public static final class MetadataFile {
        public Map<String, MetadataEntry> chains = new LinkedHashMap<>();

        public MetadataFile normalized() {
            MetadataFile copy = new MetadataFile();
            if (chains != null) {
                for (Map.Entry<String, MetadataEntry> entry : chains.entrySet()) {
                    MetadataEntry normalized = MetadataEntry.normalized(entry.getKey(), entry.getValue());
                    if (!normalized.id.isBlank() && !normalized.rootRef.isBlank()) {
                        copy.chains.put(normalized.id, normalized);
                    }
                }
            }
            return copy;
        }
    }

    public static final class MetadataEntry {
        public String id = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public List<String> tags = new ArrayList<>();
        public String group = "";
        public String rootType = "channel";
        public String rootRef = "";
        public boolean includeDisabled = true;
        public int maxDepth = 3;
        public String layoutPreference = "auto";
        public String updatedAt = "";
        public String updatedBy = "";
        public long version = 0L;

        public static MetadataEntry normalized(String fallbackId, MetadataEntry raw) {
            MetadataEntry entry = raw == null ? new MetadataEntry() : raw;
            MetadataEntry copy = new MetadataEntry();
            copy.id = normalizeId(entry.id.isBlank() ? fallbackId : entry.id);
            copy.displayName = safe(entry.displayName).trim();
            copy.note = safe(entry.note).trim();
            copy.iconKey = safe(entry.iconKey).isBlank() ? "auto" : safe(entry.iconKey).trim();
            copy.tags = normalizeTags(entry.tags);
            copy.group = safe(entry.group).trim();
            copy.rootType = normalizeRootType(entry.rootType);
            copy.rootRef = safe(entry.rootRef).trim();
            copy.includeDisabled = entry.includeDisabled;
            copy.maxDepth = Math.max(1, Math.min(8, entry.maxDepth <= 0 ? 3 : entry.maxDepth));
            copy.layoutPreference = normalizeLayout(entry.layoutPreference);
            copy.updatedAt = safe(entry.updatedAt);
            copy.updatedBy = safe(entry.updatedBy);
            copy.version = Math.max(0L, entry.version);
            return copy;
        }
    }

    public static String normalizeId(String value) {
        String raw = safe(value).trim().toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length() && builder.length() < 96; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        return builder.toString();
    }

    private static List<String> normalizeTags(List<String> tags) {
        List<String> result = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                String value = safe(tag).trim();
                if (!value.isBlank() && result.size() < 12 && !result.contains(value)) {
                    result.add(value.length() > 32 ? value.substring(0, 32) : value);
                }
            }
        }
        return result;
    }

    private static String normalizeRootType(String rootType) {
        String value = safe(rootType).trim().toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join", "timer" -> value;
            default -> "channel";
        };
    }

    private static String normalizeLayout(String layoutPreference) {
        String value = safe(layoutPreference).trim().toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "compact", "vertical" -> value;
            default -> "auto";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
