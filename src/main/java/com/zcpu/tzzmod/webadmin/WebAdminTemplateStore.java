package com.zcpu.tzzmod.webadmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.webadmin.template.WebAdminTemplatePackage;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminTemplateStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "templates.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WebAdminTemplateStore() {
    }

    public static Path path(MinecraftServer server) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        paths.ensureDirectory();
        return paths.directory().resolve(FILE_NAME);
    }

    private static Path path(MinecraftServer server, boolean ensureDirectory) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        if (ensureDirectory) {
            paths.ensureDirectory();
        }
        return paths.directory().resolve(FILE_NAME);
    }

    public static synchronized TemplateLoadResult loadWithStatus(MinecraftServer server) {
        return loadWithStatus(path(server, false));
    }

    public static synchronized TemplateLoadResult loadWithStatus(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return new TemplateLoadResult(new TemplateFile(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                TemplateFile raw = GSON.fromJson(reader, TemplateFile.class);
                TemplateFile normalized = (raw == null ? new TemplateFile() : raw).normalized();
                return new TemplateLoadResult(normalized, !normalized.warnings().isEmpty(), normalized.warnings());
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load WebAdmin templates: {}", exception.getMessage());
            return new TemplateLoadResult(
                    new TemplateFile(),
                    true,
                    "模板文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage()
            );
        }
    }

    public static synchronized boolean save(MinecraftServer server, TemplateFile file) {
        return save(path(server, true), file);
    }

    public static synchronized boolean save(Path path, TemplateFile file) {
        TemplateFile safeFile = file == null ? new TemplateFile() : file.normalized();
        return JsonStoreSupport.write(path, safeFile, "web admin templates");
    }

    public static String fingerprintFor(WebAdminTemplatePackage raw) {
        WebAdminTemplatePackage normalized = raw == null ? new WebAdminTemplatePackage() : raw.normalized();
        WebAdminTemplatePackage stable = normalized.normalized();
        stable.createdAt = 0L;
        stable.updatedAt = 0L;
        return hash(GSON.toJson(stable));
    }

    public static String fingerprintFor(TemplateFile raw) {
        TemplateFile normalized = raw == null ? new TemplateFile() : raw.normalized();
        return hash(GSON.toJson(normalized));
    }

    public static Map<String, Object> summary(WebAdminTemplatePackage raw, String source) {
        WebAdminTemplatePackage template = raw == null ? new WebAdminTemplatePackage() : raw.normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schema", template.schema);
        data.put("templateId", template.templateId);
        data.put("source", source == null || source.isBlank() ? template.metadata.source : source);
        data.put("displayName", template.displayName);
        data.put("description", template.description);
        data.put("category", template.category);
        data.put("version", template.version);
        data.put("author", template.author);
        data.put("iconKey", template.iconKey);
        data.put("resourceCount", template.resourceCount());
        data.put("channelCount", template.resources.channels.size());
        data.put("signalJoinCount", template.resources.signalJoins.size());
        data.put("timerCount", template.resources.timers.size());
        data.put("signalListenerCount", template.resources.signalListeners.size());
        data.put("actionCount", template.resources.actions.size());
        data.put("stateVariableCount", template.resources.stateVariables.size());
        data.put("conditionGroupCount", template.resources.conditionGroups.size());
        data.put("placeholderCount", template.resources.placeholders.size());
        data.put("hasPlaceholders", template.hasPlaceholders());
        data.put("createdAt", template.createdAt);
        data.put("updatedAt", template.updatedAt);
        data.put("fingerprint", fingerprintFor(template));
        data.put("expectedFingerprint", fingerprintFor(template));
        data.put("notes", template.metadata.notes);
        data.put("warnings", template.metadata.warnings);
        return data;
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    public static final class TemplateFile {
        public int version = DATA_VERSION;
        public Map<String, WebAdminTemplatePackage> templates = new LinkedHashMap<>();
        private transient List<String> warnings = new ArrayList<>();

        public TemplateFile normalized() {
            TemplateFile copy = new TemplateFile();
            copy.version = DATA_VERSION;
            if (templates != null) {
                templates.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> WebAdminTemplatePackage.normalizeId(entry.getKey())))
                        .forEach(entry -> {
                            WebAdminTemplatePackage raw = entry.getValue();
                            if (raw == null) {
                                copy.warnings.add("模板记录为空，已跳过：" + WebAdminTemplatePackage.normalizeId(entry.getKey()));
                                return;
                            }
                            if (raw.templateId == null || raw.templateId.isBlank()) {
                                raw.templateId = entry.getKey();
                            }
                            List<String> errors = raw.validationErrors();
                            if (!errors.isEmpty()) {
                                copy.warnings.add("模板记录无效，已跳过 " + entry.getKey() + "：" + String.join("；", errors));
                                return;
                            }
                            WebAdminTemplatePackage normalized = raw.normalized();
                            normalized.metadata.source = "user";
                            copy.templates.put(normalized.templateId, normalized);
                        });
            }
            return copy;
        }

        public List<String> warnings() {
            return List.copyOf(warnings == null ? List.of() : warnings);
        }
    }

    public record TemplateLoadResult(TemplateFile file, boolean degraded, String message) {
        public TemplateLoadResult {
            file = file == null ? new TemplateFile() : file;
            message = message == null ? "" : message;
        }

        public TemplateLoadResult(TemplateFile file, boolean degraded, List<String> warnings) {
            this(file, degraded, warnings == null || warnings.isEmpty() ? "" : String.join("；", warnings));
        }
    }
}
