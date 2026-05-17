package com.zcpu.tzzmod.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class TimerStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "timers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TimerStore() {
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

    public static synchronized TimerLoadResult loadWithStatus(MinecraftServer server) {
        return loadWithStatus(path(server, false));
    }

    public static synchronized TimerLoadResult loadWithStatus(Path path) {
        try {
            if (!Files.exists(path)) {
                return new TimerLoadResult(new TimerFile(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                TimerFile raw = GSON.fromJson(reader, TimerFile.class);
                TimerFile normalized = (raw == null ? new TimerFile() : raw).normalized();
                return new TimerLoadResult(normalized, !normalized.warnings().isEmpty(), normalized.warnings());
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load timers: {}", exception.getMessage());
            return new TimerLoadResult(
                    new TimerFile(),
                    true,
                    "Timer 配置文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage()
            );
        }
    }

    public static synchronized TimerFile load(MinecraftServer server) {
        return loadWithStatus(server).file();
    }

    public static synchronized TimerFile load(Path path) {
        return loadWithStatus(path).file();
    }

    public static synchronized List<TimerDefinition> getSnapshot(MinecraftServer server) {
        return List.copyOf(load(server).timers.values());
    }

    public static synchronized boolean save(MinecraftServer server, TimerFile file) {
        return save(path(server, true), file);
    }

    public static synchronized boolean save(Path path, TimerFile file) {
        TimerFile safeFile = file == null ? new TimerFile() : file.normalized();
        return JsonStoreSupport.write(path, safeFile, "timers");
    }

    public static String normalizeId(String raw) {
        return TimerDefinition.normalizeId(raw);
    }

    public static String fingerprintFor(TimerDefinition raw) {
        return fingerprintFor(raw, true);
    }

    public static String editableFingerprintFor(TimerDefinition raw) {
        return fingerprintFor(raw, false);
    }

    private static String fingerprintFor(TimerDefinition raw, boolean includeVersion) {
        TimerDefinition timer = raw == null ? new TimerDefinition() : raw.normalized();
        StringBuilder builder = new StringBuilder();
        builder.append(timer.id).append('\n');
        builder.append(timer.displayName).append('\n');
        builder.append(timer.note).append('\n');
        builder.append(timer.enabled).append('\n');
        builder.append(timer.mode.name()).append('\n');
        builder.append(timer.scopeMode.name()).append('\n');
        builder.append(timer.durationTicks).append('\n');
        builder.append(timer.intervalTicks).append('\n');
        builder.append(timer.maxRuns).append('\n');
        builder.append(timer.startPolicy.name()).append('\n');
        builder.append(timer.outputChannel).append('\n');
        appendActions(builder, "onStartActions", timer.onStartActions);
        appendActions(builder, "onTickActions", timer.onTickActions);
        appendActions(builder, "onCompleteActions", timer.onCompleteActions);
        appendActions(builder, "onCancelActions", timer.onCancelActions);
        if (includeVersion) {
            builder.append(timer.version);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(builder.toString().hashCode());
        }
    }

    public static Map<String, Object> summary(TimerDefinition raw) {
        TimerDefinition timer = raw == null ? new TimerDefinition() : raw.normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", timer.id);
        data.put("displayName", timer.displayName);
        data.put("note", timer.note);
        data.put("enabled", timer.enabled);
        data.put("mode", timer.mode.name());
        data.put("modeDisplayName", timer.mode.displayName());
        data.put("scopeMode", timer.scopeMode.name());
        data.put("scopeDisplayName", timer.scopeMode.displayName());
        data.put("durationTicks", timer.durationTicks);
        data.put("intervalTicks", timer.intervalTicks);
        data.put("maxRuns", timer.maxRuns);
        data.put("startPolicy", timer.startPolicy.name());
        data.put("startPolicyDisplayName", timer.startPolicy.displayName());
        data.put("onStartActions", timer.onStartActions);
        data.put("onTickActions", timer.onTickActions);
        data.put("onCompleteActions", timer.onCompleteActions);
        data.put("onCancelActions", timer.onCancelActions);
        data.put("onStartActionCount", timer.onStartActions.size());
        data.put("onTickActionCount", timer.onTickActions.size());
        data.put("onCompleteActionCount", timer.onCompleteActions.size());
        data.put("onCancelActionCount", timer.onCancelActions.size());
        data.put("outputChannel", timer.outputChannel);
        data.put("hasOutputOrAction", timer.hasAnyOutputOrAction());
        data.put("hasTickOrCompleteOutput", timer.hasTickOrCompleteOutput());
        data.put("version", timer.version);
        data.put("fingerprint", fingerprintFor(timer));
        data.put("expectedFingerprint", fingerprintFor(timer));
        data.put("createdAt", timer.createdAt);
        data.put("updatedAt", timer.updatedAt);
        data.put("updatedBy", timer.updatedBy);
        return data;
    }

    private static void appendActions(StringBuilder builder, String label, List<ActionConfig> actions) {
        builder.append(label).append('\n');
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            ActionConfig normalized = action == null ? null : action.normalized();
            if (normalized == null) {
                continue;
            }
            builder.append(normalized.type().id())
                    .append("|value=").append(normalized.value())
                    .append("|enabled=").append(normalized.enabled())
                    .append("|requiresOp=").append(normalized.requiresOp())
                    .append("|cooldownTicks=").append(normalized.cooldownTicks())
                    .append("|notifyOps=").append(normalized.notifyOps())
                    .append("|conditionGroupId=").append(normalized.conditionGroupId())
                    .append('|').append(normalized.stateFingerprint())
                    .append('|').append(normalized.timerFingerprint())
                    .append('\n');
        }
    }

    public static final class TimerFile {
        public int version = DATA_VERSION;
        public Map<String, TimerDefinition> timers = new LinkedHashMap<>();
        private transient List<String> warnings = new ArrayList<>();

        public TimerFile normalized() {
            TimerFile copy = new TimerFile();
            copy.version = DATA_VERSION;
            if (timers != null) {
                for (Map.Entry<String, TimerDefinition> entry : timers.entrySet()) {
                    TimerDefinition raw = entry.getValue();
                    if (raw == null) {
                        copy.warnings.add("Timer 记录为空，已跳过：" + normalizeId(entry.getKey()));
                        continue;
                    }
                    if (raw.id == null || raw.id.isBlank()) {
                        raw.id = entry.getKey();
                    }
                    TimerDefinition normalized = raw.normalized();
                    if (normalized.id.isBlank()) {
                        copy.warnings.add("Timer ID 无效，已跳过：" + entry.getKey());
                        continue;
                    }
                    copy.timers.put(normalized.id, normalized);
                }
            }
            return copy;
        }

        public List<String> warnings() {
            return List.copyOf(warnings == null ? List.of() : warnings);
        }
    }

    public record TimerLoadResult(TimerFile file, boolean degraded, String message) {
        public TimerLoadResult {
            file = file == null ? new TimerFile() : file;
            message = message == null ? "" : message;
        }

        public TimerLoadResult(TimerFile file, boolean degraded, List<String> warnings) {
            this(file, degraded, warnings == null || warnings.isEmpty() ? "" : String.join("；", warnings));
        }
    }
}
