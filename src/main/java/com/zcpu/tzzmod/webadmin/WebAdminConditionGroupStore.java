package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminConditionGroupStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "condition_groups.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WebAdminConditionGroupStore() {
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

    public static synchronized ConditionGroupFile load(MinecraftServer server) {
        return load(path(server));
    }

    public static synchronized ConditionGroupFile load(Path path) {
        return loadWithStatus(path).file();
    }

    public static synchronized ConditionGroupLoadResult loadWithStatus(MinecraftServer server) {
        return loadWithStatus(path(server, false));
    }

    public static synchronized ConditionGroupLoadResult loadWithStatus(Path path) {
        try {
            if (!Files.exists(path)) {
                return new ConditionGroupLoadResult(new ConditionGroupFile(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ConditionGroupFile raw = GSON.fromJson(reader, ConditionGroupFile.class);
                ConditionGroupFile normalized = (raw == null ? new ConditionGroupFile() : raw).normalized();
                return new ConditionGroupLoadResult(normalized, !normalized.warnings().isEmpty(), normalized.warnings());
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load web admin condition groups: {}", exception.getMessage());
            return new ConditionGroupLoadResult(
                    new ConditionGroupFile(),
                    true,
                    "条件组配置文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage()
            );
        }
    }

    public static synchronized boolean save(MinecraftServer server, ConditionGroupFile file) {
        return save(path(server, true), file);
    }

    public static synchronized boolean save(Path path, ConditionGroupFile file) {
        ConditionGroupFile safeFile = file == null ? new ConditionGroupFile() : file.normalized();
        return JsonStoreSupport.write(path, safeFile, "web admin condition groups");
    }

    public static String normalizeId(String value) {
        String raw = safe(value).trim().toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean lastDash = false;
        for (int i = 0; i < raw.length() && builder.length() < 96; i++) {
            char c = raw.charAt(i);
            boolean accepted = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':';
            if (accepted) {
                builder.append(c);
                lastDash = c == '-';
            } else if (Character.isWhitespace(c) && !lastDash && builder.length() > 0) {
                builder.append('-');
                lastDash = true;
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String fingerprintFor(ConditionGroupEntry entry) {
        ConditionGroupEntry normalized = ConditionGroupEntry.normalized(entry == null ? "" : entry.id, entry);
        StringBuilder builder = new StringBuilder();
        builder.append(normalized.id).append('\n');
        builder.append(normalized.displayName).append('\n');
        builder.append(normalized.note).append('\n');
        builder.append(normalized.iconKey).append('\n');
        builder.append(normalized.enabled).append('\n');
        builder.append(String.join(",", normalized.tags)).append('\n');
        builder.append(normalized.groupDefinition == null ? "" : normalized.groupDefinition.stableFingerprint()).append('\n');
        builder.append(normalized.version);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(builder.toString().hashCode());
        }
    }

    public static ConditionGroupDefinition defaultDefinition(String id, String displayName) {
        ConditionNode root = ConditionNode.group(
                "root",
                ConditionGroupMode.AND,
                List.of(ConditionNode.leaf("node-always-true", ConditionNodeType.ALWAYS_TRUE, ConditionNodeConfig.EMPTY))
        );
        return new ConditionGroupDefinition(
                normalizeId(id).isBlank() ? "condition.group" : normalizeId(id),
                1,
                safe(displayName).isBlank() ? "新条件组" : safe(displayName),
                "",
                List.of(),
                root
        );
    }

    public static final class ConditionGroupFile {
        public int version = DATA_VERSION;
        public Map<String, ConditionGroupEntry> groups = new LinkedHashMap<>();
        private transient List<String> warnings = new ArrayList<>();

        public ConditionGroupFile normalized() {
            ConditionGroupFile copy = new ConditionGroupFile();
            copy.version = DATA_VERSION;
            if (groups != null) {
                for (Map.Entry<String, ConditionGroupEntry> entry : groups.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().groupDefinition == null) {
                        copy.warnings.add("条件组记录缺少条件组定义（groupDefinition），已跳过：" + normalizeId(entry.getKey()));
                        continue;
                    }
                    ConditionGroupEntry normalized = ConditionGroupEntry.normalized(entry.getKey(), entry.getValue());
                    if (!normalized.id.isBlank()) {
                        copy.groups.put(normalized.id, normalized);
                    }
                }
            }
            return copy;
        }

        public List<String> warnings() {
            return List.copyOf(warnings == null ? List.of() : warnings);
        }
    }

    public static final class ConditionGroupEntry {
        public String id = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "doctor-overview";
        public boolean enabled = true;
        public List<String> tags = new ArrayList<>();
        public ConditionGroupDefinition groupDefinition;
        public String createdAt = "";
        public String updatedAt = "";
        public String updatedBy = "";
        public long version = 0L;

        public static ConditionGroupEntry normalized(String fallbackId, ConditionGroupEntry raw) {
            ConditionGroupEntry source = raw == null ? new ConditionGroupEntry() : raw;
            ConditionGroupEntry copy = new ConditionGroupEntry();
            copy.id = normalizeId(source.id.isBlank() ? fallbackId : source.id);
            copy.displayName = safe(source.displayName).trim();
            copy.note = safe(source.note).trim();
            copy.iconKey = safe(source.iconKey).isBlank() ? "doctor-overview" : safe(source.iconKey).trim();
            copy.enabled = source.enabled;
            copy.tags = normalizeTags(source.tags);
            copy.groupDefinition = source.groupDefinition == null
                    ? null
                    : normalizeDefinition(source.groupDefinition, copy.id, copy.displayName);
            copy.createdAt = safe(source.createdAt);
            copy.updatedAt = safe(source.updatedAt);
            copy.updatedBy = safe(source.updatedBy);
            copy.version = Math.max(0L, source.version);
            return copy;
        }

        public Map<String, Object> summary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("displayName", displayName);
            summary.put("note", note);
            summary.put("iconKey", iconKey);
            summary.put("enabled", enabled);
            summary.put("tags", tags);
            summary.put("nodeCount", countNodes(groupDefinition == null ? null : groupDefinition.root()));
            summary.put("version", version);
            summary.put("fingerprint", fingerprintFor(this));
            summary.put("createdAt", createdAt);
            summary.put("updatedAt", updatedAt);
            summary.put("updatedBy", updatedBy);
            return summary;
        }

        public ConditionGroupEntry withWriteMetadata(String actor, long nextVersion, boolean created) {
            ConditionGroupEntry copy = normalized(id, this);
            String now = Instant.now().toString();
            copy.createdAt = created || copy.createdAt.isBlank() ? now : copy.createdAt;
            copy.updatedAt = now;
            copy.updatedBy = safe(actor);
            copy.version = Math.max(1L, nextVersion);
            return copy;
        }
    }

    public static int countNodes(ConditionNode node) {
        if (node == null) {
            return 0;
        }
        int count = 1;
        for (ConditionNode child : node.children()) {
            count += countNodes(child);
        }
        return count;
    }

    private static ConditionGroupDefinition normalizeDefinition(ConditionGroupDefinition definition, String id, String displayName) {
        if (definition == null) {
            return null;
        }
        String definitionId = normalizeId(definition.id()).isBlank() ? id : normalizeId(definition.id());
        return new ConditionGroupDefinition(
                definitionId.isBlank() ? id : definitionId,
                Math.max(1, definition.version()),
                safe(definition.displayName()).isBlank() ? displayName : definition.displayName(),
                safe(definition.note()),
                definition.tags(),
                definition.root()
        );
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
        return List.copyOf(result);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record ConditionGroupLoadResult(ConditionGroupFile file, boolean degraded, String message) {
        public ConditionGroupLoadResult {
            file = file == null ? new ConditionGroupFile() : file;
            message = safe(message);
        }

        public ConditionGroupLoadResult(ConditionGroupFile file, boolean degraded, List<String> warnings) {
            this(file, degraded, warnings == null || warnings.isEmpty() ? "" : String.join("；", warnings));
        }
    }
}
