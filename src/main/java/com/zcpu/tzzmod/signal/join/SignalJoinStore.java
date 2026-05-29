package com.zcpu.tzzmod.signal.join;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.signal.SignalChannel;
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

public final class SignalJoinStore {
    public static final int DATA_VERSION = 1;
    public static final String FILE_NAME = "signal_joins.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LOAD_CACHE_MAX_ENTRIES = 32;
    private static final Map<Path, CachedLoadResult> LOAD_CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, CachedLoadResult> eldest) {
            return size() > LOAD_CACHE_MAX_ENTRIES;
        }
    };

    private SignalJoinStore() {
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

    public static synchronized SignalJoinLoadResult loadWithStatus(MinecraftServer server) {
        return loadWithStatus(path(server, false));
    }

    public static synchronized SignalJoinLoadResult loadWithStatusCached(MinecraftServer server) {
        return loadWithStatusCached(path(server, false));
    }

    public static synchronized SignalJoinLoadResult loadWithStatusCached(Path path) {
        Path key = cacheKey(path);
        FileFingerprint fingerprint = fingerprint(path);
        CachedLoadResult cached = LOAD_CACHE.get(key);
        if (cached != null && cached.fingerprint.equals(fingerprint)) {
            return cached.result;
        }
        SignalJoinLoadResult loaded = loadWithStatus(path);
        LOAD_CACHE.put(key, new CachedLoadResult(fingerprint, loaded));
        return loaded;
    }

    public static synchronized SignalJoinLoadResult loadWithStatus(Path path) {
        try {
            if (!Files.exists(path)) {
                return new SignalJoinLoadResult(new SignalJoinFile(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SignalJoinFile raw = GSON.fromJson(reader, SignalJoinFile.class);
                SignalJoinFile normalized = (raw == null ? new SignalJoinFile() : raw).normalized();
                return new SignalJoinLoadResult(normalized, !normalized.warnings().isEmpty(), normalized.warnings());
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load signal joins: {}", exception.getMessage());
            return new SignalJoinLoadResult(
                    new SignalJoinFile(),
                    true,
                    "Signal Join 配置文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage()
            );
        }
    }

    public static synchronized SignalJoinFile load(MinecraftServer server) {
        return loadWithStatus(server).file();
    }

    public static synchronized SignalJoinFile load(Path path) {
        return loadWithStatus(path).file();
    }

    public static synchronized List<SignalJoinDefinition> getSnapshot(MinecraftServer server) {
        return List.copyOf(load(server).joins.values());
    }

    public static synchronized boolean save(MinecraftServer server, SignalJoinFile file) {
        return save(path(server, true), file);
    }

    public static synchronized boolean save(Path path, SignalJoinFile file) {
        SignalJoinFile safeFile = file == null ? new SignalJoinFile() : file.normalized();
        boolean saved = JsonStoreSupport.write(path, safeFile, "signal joins");
        if (saved) {
            invalidateCachedLoad(path);
        }
        return saved;
    }

    public static synchronized void clearCachedLoad(MinecraftServer server) {
        invalidateCachedLoad(path(server, false));
    }

    private static void invalidateCachedLoad(Path path) {
        LOAD_CACHE.remove(cacheKey(path));
    }

    private static Path cacheKey(Path path) {
        return path == null ? Path.of("") : path.toAbsolutePath().normalize();
    }

    private static FileFingerprint fingerprint(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return new FileFingerprint(false, -1L, -1L, "");
            }
            byte[] bytes = Files.readAllBytes(path);
            return new FileFingerprint(
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    bytes.length,
                    sha256(bytes)
            );
        } catch (Exception ignored) {
            return new FileFingerprint(true, -1L, -1L, "");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(bytes == null ? new byte[0] : bytes));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(java.util.Arrays.hashCode(bytes == null ? new byte[0] : bytes));
        }
    }

    public static String normalizeId(String raw) {
        return SignalJoinDefinition.normalizeId(raw);
    }

    public static String fingerprintFor(SignalJoinDefinition raw) {
        SignalJoinDefinition join = raw == null ? new SignalJoinDefinition() : raw.normalized();
        StringBuilder builder = new StringBuilder();
        builder.append(join.id).append('\n');
        builder.append(join.displayName).append('\n');
        builder.append(join.note).append('\n');
        builder.append(join.enabled).append('\n');
        builder.append(join.outputChannel).append('\n');
        builder.append(join.mode.name()).append('\n');
        builder.append(join.threshold).append('\n');
        builder.append(join.scopeMode.name()).append('\n');
        builder.append(join.resetPolicy.name()).append('\n');
        builder.append(join.timeoutTicks).append('\n');
        builder.append(join.cooldownTicks).append('\n');
        for (SignalJoinInputDefinition input : join.inputChannels) {
            builder.append(input.channel).append('|').append(input.displayName).append('|').append(input.note).append('|').append(input.requiredCount).append('\n');
        }
        builder.append(join.version);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(builder.toString().hashCode());
        }
    }

    public static Map<String, Object> summary(SignalJoinDefinition raw) {
        SignalJoinDefinition join = raw == null ? new SignalJoinDefinition() : raw.normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", join.id);
        data.put("displayName", join.displayName);
        data.put("note", join.note);
        data.put("enabled", join.enabled);
        data.put("inputChannels", join.inputChannels);
        data.put("inputChannelCount", join.inputChannelNames().size());
        data.put("outputChannel", join.outputChannel);
        data.put("mode", join.mode.name());
        data.put("modeDisplayName", join.mode.displayName());
        data.put("threshold", join.threshold);
        data.put("scopeMode", join.scopeMode.name());
        data.put("scopeDisplayName", join.scopeMode.displayName());
        data.put("resetPolicy", join.resetPolicy.name());
        data.put("resetPolicyDisplayName", join.resetPolicy.displayName());
        data.put("timeoutTicks", join.timeoutTicks);
        data.put("cooldownTicks", join.cooldownTicks);
        data.put("version", join.version);
        data.put("fingerprint", fingerprintFor(join));
        data.put("expectedFingerprint", fingerprintFor(join));
        data.put("createdAt", join.createdAt);
        data.put("updatedAt", join.updatedAt);
        data.put("updatedBy", join.updatedBy);
        return data;
    }

    public static final class SignalJoinFile {
        public int version = DATA_VERSION;
        public Map<String, SignalJoinDefinition> joins = new IndexedJoinMap(this);
        private transient List<String> warnings = new ArrayList<>();
        private transient Map<String, List<SignalJoinDefinition>> enabledJoinsByInputChannel;

        public SignalJoinFile normalized() {
            SignalJoinFile copy = new SignalJoinFile();
            copy.version = DATA_VERSION;
            if (joins != null) {
                for (Map.Entry<String, SignalJoinDefinition> entry : joins.entrySet()) {
                    SignalJoinDefinition raw = entry.getValue();
                    if (raw == null) {
                        copy.warnings.add("Signal Join 记录为空，已跳过：" + normalizeId(entry.getKey()));
                        continue;
                    }
                    if (raw.id == null || raw.id.isBlank()) {
                        raw.id = entry.getKey();
                    }
                    SignalJoinDefinition normalized = raw.normalized();
                    if (normalized.id.isBlank()) {
                        copy.warnings.add("Signal Join ID 无效，已跳过：" + entry.getKey());
                        continue;
                    }
                    copy.joins.put(normalized.id, normalized);
                }
            }
            return copy;
        }

        public List<SignalJoinDefinition> enabledJoinsReferencing(String channel) {
            String normalizedChannel = SignalChannel.normalize(channel);
            if (enabledJoinsByInputChannel == null) {
                // accepted signal 热路径只需要当前 channel 的 join；索引按 joins.values() 顺序构建，保持旧输出顺序。
                // joins 是可变配置 map，因此 map 写入会清空该索引，避免 WebAdmin service 构造文件时读到陈旧候选集。
                enabledJoinsByInputChannel = indexEnabledJoinsByInputChannel();
            }
            return List.copyOf(enabledJoinsByInputChannel.getOrDefault(normalizedChannel, List.of()));
        }

        private void invalidateRuntimeIndex() {
            enabledJoinsByInputChannel = null;
        }

        private Map<String, List<SignalJoinDefinition>> indexEnabledJoinsByInputChannel() {
            Map<String, List<SignalJoinDefinition>> mutable = new LinkedHashMap<>();
            if (joins != null) {
                for (SignalJoinDefinition raw : joins.values()) {
                    SignalJoinDefinition join = raw == null ? null : raw.normalized();
                    if (join == null || !join.enabled) {
                        continue;
                    }
                    for (String inputChannel : join.inputChannelNames()) {
                        if (!inputChannel.isBlank()) {
                            mutable.computeIfAbsent(inputChannel, ignored -> new ArrayList<>()).add(join);
                        }
                    }
                }
            }
            Map<String, List<SignalJoinDefinition>> indexed = new LinkedHashMap<>();
            for (Map.Entry<String, List<SignalJoinDefinition>> entry : mutable.entrySet()) {
                indexed.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return indexed;
        }

        public List<String> warnings() {
            return List.copyOf(warnings == null ? List.of() : warnings);
        }
    }

    private static final class IndexedJoinMap extends LinkedHashMap<String, SignalJoinDefinition> {
        private final SignalJoinFile owner;

        private IndexedJoinMap(SignalJoinFile owner) {
            this.owner = owner;
        }

        @Override
        public SignalJoinDefinition put(String key, SignalJoinDefinition value) {
            owner.invalidateRuntimeIndex();
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends SignalJoinDefinition> map) {
            owner.invalidateRuntimeIndex();
            super.putAll(map);
        }

        @Override
        public SignalJoinDefinition remove(Object key) {
            owner.invalidateRuntimeIndex();
            return super.remove(key);
        }

        @Override
        public void clear() {
            owner.invalidateRuntimeIndex();
            super.clear();
        }
    }

    public record SignalJoinLoadResult(SignalJoinFile file, boolean degraded, String message) {
        public SignalJoinLoadResult {
            file = file == null ? new SignalJoinFile() : file;
            message = message == null ? "" : message;
        }

        public SignalJoinLoadResult(SignalJoinFile file, boolean degraded, List<String> warnings) {
            this(file, degraded, warnings == null || warnings.isEmpty() ? "" : String.join("；", warnings));
        }
    }

    private record FileFingerprint(boolean exists, long modifiedMillis, long sizeBytes, String contentHash) {
    }

    private record CachedLoadResult(FileFingerprint fingerprint, SignalJoinLoadResult result) {
    }
}
