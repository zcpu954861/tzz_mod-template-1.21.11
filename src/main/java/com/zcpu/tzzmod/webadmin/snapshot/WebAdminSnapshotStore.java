package com.zcpu.tzzmod.webadmin.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotManifest;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotPackage;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotRecord;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotResource;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.StoreSpec;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class WebAdminSnapshotStore {
    public static final String SNAPSHOT_DIR = "snapshots";
    public static final String SNAPSHOT_DATA_DIR = "data";
    public static final String MANIFEST_FILE = "manifest.json";
    public static final int AUTO_RETENTION_LIMIT = 200;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Gson COMPACT_GSON = new GsonBuilder().disableHtmlEscaping().create();

    private WebAdminSnapshotStore() {
    }

    public static Path snapshotRoot(MinecraftServer server) {
        return WebAdminStoragePaths.resolve(server).directory().resolve(SNAPSHOT_DIR);
    }

    public static Path manifestPath(Path snapshotRoot) {
        return snapshotRoot.resolve(MANIFEST_FILE);
    }

    public static Path dataDirectory(Path snapshotRoot) {
        return snapshotRoot.resolve(SNAPSHOT_DATA_DIR);
    }

    public static Path packagePath(Path snapshotRoot, String snapshotId) {
        return dataDirectory(snapshotRoot).resolve(safeSnapshotId(snapshotId) + ".json");
    }

    public static ManifestLoadResult loadManifest(MinecraftServer server) {
        return loadManifest(snapshotRoot(server));
    }

    public static synchronized ManifestLoadResult loadManifest(Path snapshotRoot) {
        Path path = manifestPath(snapshotRoot);
        try {
            if (!Files.exists(path)) {
                return new ManifestLoadResult(new SnapshotManifest(), false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SnapshotManifest raw = GSON.fromJson(reader, SnapshotManifest.class);
                int rawSchemaVersion = raw == null ? WebAdminSnapshotModels.SCHEMA_VERSION : raw.schemaVersion;
                SnapshotManifest manifest = (raw == null ? new SnapshotManifest() : raw).normalized();
                boolean degraded = rawSchemaVersion != WebAdminSnapshotModels.SCHEMA_VERSION;
                String warning = degraded ? "Snapshot manifest schemaVersion 未知，已按 8.18 schema 尽量读取。" : "";
                manifest.manifestFingerprint = fingerprintManifest(manifest);
                return new ManifestLoadResult(manifest, degraded, warning);
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load WebAdmin snapshot manifest: {}", exception.getMessage());
            SnapshotManifest fallback = new SnapshotManifest();
            fallback.warnings.add("快照 manifest 读取失败，已返回空列表以避免覆盖损坏文件。详细错误请查看服务端日志。");
            return new ManifestLoadResult(fallback, true, fallback.warnings.get(0));
        }
    }

    public static synchronized boolean saveManifest(Path snapshotRoot, SnapshotManifest manifest) {
        try {
            Files.createDirectories(snapshotRoot);
            SnapshotManifest safeManifest = manifest == null ? new SnapshotManifest() : manifest.normalized();
            safeManifest.manifestFingerprint = fingerprintManifest(safeManifest);
            try (Writer writer = Files.newBufferedWriter(manifestPath(snapshotRoot), StandardCharsets.UTF_8)) {
                GSON.toJson(safeManifest, writer);
            }
            return true;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to save WebAdmin snapshot manifest: {}", exception.getMessage());
            return false;
        }
    }

    public static PackageLoadResult loadPackage(MinecraftServer server, String snapshotId) {
        return loadPackage(snapshotRoot(server), snapshotId);
    }

    public static synchronized PackageLoadResult loadPackage(Path snapshotRoot, String snapshotId) {
        String safeId = safeSnapshotId(snapshotId);
        if (safeId.isBlank()) {
            return new PackageLoadResult(null, true, "快照 ID 不能为空。");
        }
        Path path = packagePath(snapshotRoot, safeId);
        try {
            if (!Files.exists(path)) {
                return new PackageLoadResult(null, true, "快照数据文件不存在：" + safeId);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SnapshotPackage raw = GSON.fromJson(reader, SnapshotPackage.class);
                int rawSchemaVersion = raw == null ? WebAdminSnapshotModels.SCHEMA_VERSION : raw.schemaVersion;
                SnapshotPackage pack = raw == null ? null : raw.normalized();
                if (pack == null || pack.snapshotId.isBlank()) {
                    return new PackageLoadResult(null, true, "快照数据文件为空或缺少 snapshotId。");
                }
                if (!safeId.equals(safeSnapshotId(pack.snapshotId))) {
                    return new PackageLoadResult(null, true, "快照数据文件 snapshotId 与文件名不一致：" + safeId);
                }
                PackageValidationResult validated = validatePackageResources(pack);
                if (validated.degraded()) {
                    return new PackageLoadResult(null, true, validated.message());
                }
                pack = validated.pack();
                boolean degraded = rawSchemaVersion != WebAdminSnapshotModels.SCHEMA_VERSION;
                String warning = degraded ? "Snapshot package schemaVersion 未知，已按 8.18 schema 尽量读取。" : "";
                pack.packageFingerprint = fingerprintPackage(pack);
                return new PackageLoadResult(pack, degraded, warning);
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load WebAdmin snapshot package {}: {}", safeId, exception.getMessage());
            return new PackageLoadResult(null, true, "快照数据文件读取失败。详细错误请查看服务端日志。");
        }
    }

    public static PackageLoadResult loadPackage(MinecraftServer server, SnapshotRecord record) {
        return loadPackage(snapshotRoot(server), record);
    }

    public static synchronized PackageLoadResult loadPackage(Path snapshotRoot, SnapshotRecord record) {
        if (record == null || safeSnapshotId(record.snapshotId).isBlank()) {
            return new PackageLoadResult(null, true, "快照 manifest 记录缺少 snapshotId。");
        }
        SnapshotRecord safeRecord = record.normalized();
        PackageLoadResult loaded = loadPackage(snapshotRoot, safeRecord.snapshotId);
        if (loaded.degraded() || loaded.pack() == null) {
            return loaded;
        }
        String expected = safe(safeRecord.packageFingerprint);
        String actual = safe(loaded.pack().packageFingerprint);
        if (!expected.isBlank() && !expected.equals(actual)) {
            return new PackageLoadResult(null, true, "快照数据指纹与 manifest 不一致，已阻断读取：" + safeRecord.snapshotId);
        }
        return loaded;
    }

    public static synchronized boolean savePackage(Path snapshotRoot, SnapshotPackage pack) {
        try {
            Files.createDirectories(dataDirectory(snapshotRoot));
            SnapshotPackage safePackage = pack == null ? new SnapshotPackage() : pack.normalized();
            safePackage.packageFingerprint = fingerprintPackage(safePackage);
            try (Writer writer = Files.newBufferedWriter(packagePath(snapshotRoot, safePackage.snapshotId), StandardCharsets.UTF_8)) {
                GSON.toJson(safePackage, writer);
            }
            return true;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to save WebAdmin snapshot package: {}", exception.getMessage());
            return false;
        }
    }

    public static synchronized void applyAutoRetention(Path snapshotRoot, SnapshotManifest manifest) {
        if (manifest == null || manifest.records == null) {
            return;
        }
        List<SnapshotRecord> autos = manifest.records.stream()
                .filter(record -> "auto".equals(record.kind))
                .sorted(Comparator.comparingLong(record -> record.sequence))
                .toList();
        int removable = autos.size() - AUTO_RETENTION_LIMIT;
        if (removable <= 0) {
            return;
        }
        List<String> removeIds = autos.stream().limit(removable).map(record -> record.snapshotId).toList();
        manifest.records.removeIf(record -> removeIds.contains(record.snapshotId));
        for (String snapshotId : removeIds) {
            try {
                Files.deleteIfExists(packagePath(snapshotRoot, snapshotId));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to delete retained WebAdmin auto snapshot {}: {}", snapshotId, exception.getMessage());
            }
        }
    }

    public static List<StoreSpec> storeSpecs(MinecraftServer server) {
        Path webAdminDir = WebAdminStoragePaths.resolve(server).directory();
        Path tzzModDir = server.getSavePath(WorldSavePath.ROOT).resolve("tzz_mod");
        return storeSpecs(webAdminDir, tzzModDir);
    }

    public static List<StoreSpec> storeSpecs(Path webAdminDir, Path tzzModDir) {
        return List.of(
                new StoreSpec("channel_metadata", "channel_metadata", "频道显示信息", webAdminDir.resolve("web_admin_channel_metadata.json").toString(), "SignalBridge", false),
                new StoreSpec("logic_chain_metadata", "logic_chain_metadata", "逻辑链显示信息", webAdminDir.resolve("web_admin_logic_chain_metadata.json").toString(), "Logic Chain", false),
                new StoreSpec("device_metadata", "device_metadata", "设备显示信息", webAdminDir.resolve("web_admin_device_metadata.json").toString(), "Device", false),
                new StoreSpec("templates", "template", "用户模板", webAdminDir.resolve("templates.json").toString(), "Template", false),
                new StoreSpec("condition_groups", "condition_group", "条件组", webAdminDir.resolve("condition_groups.json").toString(), "Condition", false),
                new StoreSpec("condition_runtime_gates", "condition_runtime_gate", "运行时 gate 绑定配置", webAdminDir.resolve("condition_runtime_gates.json").toString(), "Condition", false),
                new StoreSpec("signal_devices", "signal_device", "Signal Device / VBD 配置", tzzModDir.resolve("signal_devices.json").toString(), "Device", true),
                new StoreSpec("signal_joins", "signal_join", "Signal Join 配置", webAdminDir.resolve("signal_joins.json").toString(), "Signal Join", false),
                new StoreSpec("timers", "timer", "Timer 配置", webAdminDir.resolve("timers.json").toString(), "Timer", false),
                new StoreSpec("state_variables", "state_variable", "状态变量", webAdminDir.resolve("state_variables.json").toString(), "State", false),
                new StoreSpec("signal_listeners", "signal_listener", "Signal Listener 配置", tzzModDir.resolve("signal_listeners.json").toString(), "Signal Listener", true),
                new StoreSpec("region_controllers", "region_controller", "RegionController 配置", tzzModDir.resolve("region_controllers.json").toString(), "Region", true)
        );
    }

    public static SnapshotCollectionResult collect(MinecraftServer server) {
        return collect(storeSpecs(server));
    }

    public static SnapshotCollectionResult collect(List<StoreSpec> specs) {
        List<SnapshotResource> resources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (StoreSpec spec : specs == null ? List.<StoreSpec>of() : specs) {
            Path path = Path.of(spec.relativePath());
            if (!Files.exists(path)) {
                continue;
            }
            try {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                JsonElement parsed = JsonParser.parseString(raw);
                String canonical = canonicalJson(parsed);
                SnapshotResource fileResource = resource(
                        "store_file",
                        spec.pathKey(),
                        spec.displayName(),
                        spec.pathKey(),
                        spec.pathKey(),
                        canonical,
                        true,
                        Map.of("module", spec.module(), "path", spec.pathKey())
                );
                resources.add(fileResource);
                addLogicalResources(resources, spec, parsed);
            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to collect WebAdmin snapshot store {}: {}", spec.displayName(), exception.getMessage());
                warnings.add(spec.displayName() + " 读取失败，已阻断快照以避免保存半可信数据。详细错误请查看服务端日志。");
            }
        }
        return new SnapshotCollectionResult(resources, !warnings.isEmpty(), warnings);
    }

    public static Map<String, SnapshotResource> restoreResourcesByPath(SnapshotPackage pack) {
        Map<String, SnapshotResource> result = new LinkedHashMap<>();
        if (pack == null || pack.resources == null) {
            return result;
        }
        for (SnapshotResource resource : pack.resources) {
            SnapshotResource normalized = resource == null ? null : resource.normalized();
            if (normalized != null && normalized.restoreResource && "store_file".equals(normalized.resourceType)) {
                result.put(normalized.pathKey, normalized);
            }
        }
        return result;
    }

    public static Map<String, SnapshotResource> diffResourcesByKey(SnapshotPackage pack) {
        Map<String, SnapshotResource> result = new LinkedHashMap<>();
        if (pack == null || pack.resources == null) {
            return result;
        }
        for (SnapshotResource resource : pack.resources) {
            SnapshotResource normalized = resource == null ? null : resource.normalized();
            if (normalized != null && !normalized.restoreResource) {
                result.put(normalized.resourceKey(), normalized);
            }
        }
        return result;
    }

    public static String canonicalJson(JsonElement element) {
        return COMPACT_GSON.toJson(canonicalElement(element));
    }

    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(safe(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    public static String fingerprintManifest(SnapshotManifest manifest) {
        SnapshotManifest copy = manifest == null ? new SnapshotManifest() : manifest.normalized();
        copy.manifestFingerprint = "";
        for (SnapshotRecord record : copy.records) {
            record.storagePath = "";
        }
        return hash(COMPACT_GSON.toJson(copy));
    }

    private static PackageValidationResult validatePackageResources(SnapshotPackage pack) {
        SnapshotPackage result = pack == null ? new SnapshotPackage() : pack.normalized();
        List<SnapshotResource> resources = new ArrayList<>();
        for (SnapshotResource resource : result.resources == null ? List.<SnapshotResource>of() : result.resources) {
            SnapshotResource normalized = resource == null ? null : resource.normalized();
            if (normalized == null) {
                continue;
            }
            try {
                JsonElement parsed = JsonParser.parseString(normalized.canonicalJson);
                normalized.canonicalJson = canonicalJson(parsed);
                normalized.fingerprint = hash(normalized.canonicalJson);
                resources.add(normalized);
            } catch (Exception exception) {
                return new PackageValidationResult(null, true, "快照资源 JSON 校验失败：" + normalized.resourceType + "/" + normalized.resourceId);
            }
        }
        result.resources = resources;
        result.packageFingerprint = fingerprintPackage(result);
        return new PackageValidationResult(result.normalized(), false, "");
    }

    public static String fingerprintPackage(SnapshotPackage pack) {
        SnapshotPackage copy = pack == null ? new SnapshotPackage() : pack.normalized();
        copy.packageFingerprint = "";
        return hash(COMPACT_GSON.toJson(copy));
    }

    public static String safeSnapshotId(String raw) {
        String value = raw == null ? "" : raw.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length() && builder.length() < 96; i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static void addLogicalResources(List<SnapshotResource> resources, StoreSpec spec, JsonElement parsed) {
        if (!parsed.isJsonObject()) {
            return;
        }
        JsonObject root = parsed.getAsJsonObject();
        switch (spec.pathKey()) {
            case "channel_metadata" -> addObjectMapResources(resources, spec, root, "channels", "channel", "channel");
            case "logic_chain_metadata" -> addObjectMapResources(resources, spec, root, "chains", "id", "id");
            case "device_metadata" -> addObjectMapResources(resources, spec, root, "devices", "deviceId", "deviceId");
            case "templates" -> addObjectMapResources(resources, spec, root, "templates", "templateId", "templateId");
            case "condition_groups" -> addObjectMapResources(resources, spec, root, "groups", "id", "id");
            case "condition_runtime_gates" -> addObjectMapResources(resources, spec, root, "virtualBlockDevices", "deviceId", "conditionGroupId");
            case "signal_devices" -> addArrayResources(resources, spec, root, "devices", "id", "name");
            case "signal_joins" -> addObjectMapResources(resources, spec, root, "joins", "id", "id");
            case "timers" -> addObjectMapResources(resources, spec, root, "timers", "id", "id");
            case "state_variables" -> addArrayResources(resources, spec, root, "variables", "id", "key");
            case "signal_listeners" -> addArrayResources(resources, spec, root, "listeners", "id", "name");
            case "region_controllers" -> addArrayResources(resources, spec, root, "controllers", "id", "name");
            default -> {
            }
        }
    }

    private static void addObjectMapResources(List<SnapshotResource> resources, StoreSpec spec, JsonObject root, String memberName, String idField, String displayField) {
        JsonElement member = root.get(memberName);
        if (member == null || !member.isJsonObject()) {
            return;
        }
        JsonObject map = member.getAsJsonObject();
        for (String key : new TreeSet<>(map.keySet())) {
            JsonElement value = map.get(key);
            String id = idFromObject(key, value, idField);
            if (id.isBlank()) {
                continue;
            }
            String display = displayFromObject(id, value, displayField);
            resources.add(resource(spec.resourceType(), id, display, spec.pathKey(), spec.pathKey(), canonicalJson(value), false, Map.of("module", spec.module())));
        }
    }

    private static void addArrayResources(List<SnapshotResource> resources, StoreSpec spec, JsonObject root, String memberName, String idField, String displayField) {
        JsonElement member = root.get(memberName);
        if (member == null || !member.isJsonArray()) {
            return;
        }
        JsonArray array = member.getAsJsonArray();
        int index = 0;
        for (JsonElement value : array) {
            String id = idFromObject(spec.pathKey() + ":" + index, value, idField);
            if (id.isBlank()) {
                id = spec.pathKey() + ":" + index;
            }
            String display = displayFromObject(id, value, displayField);
            resources.add(resource(spec.resourceType(), id, display, spec.pathKey(), spec.pathKey(), canonicalJson(value), false, Map.of("module", spec.module())));
            index++;
        }
    }

    private static SnapshotResource resource(
            String resourceType,
            String resourceId,
            String displayName,
            String sourceStore,
            String pathKey,
            String canonicalJson,
            boolean restoreResource,
            Map<String, String> metadata
    ) {
        SnapshotResource resource = new SnapshotResource();
        resource.resourceType = safe(resourceType);
        resource.resourceId = safe(resourceId);
        resource.displayName = safe(displayName).isBlank() ? safe(resourceId) : safe(displayName);
        resource.sourceStore = safe(sourceStore);
        resource.pathKey = safe(pathKey);
        resource.canonicalJson = safe(canonicalJson);
        resource.fingerprint = hash(resource.canonicalJson);
        resource.restoreResource = restoreResource;
        resource.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        return resource;
    }

    private static String idFromObject(String fallback, JsonElement value, String field) {
        if (value != null && value.isJsonObject()) {
            JsonElement member = value.getAsJsonObject().get(field);
            if (member != null && member.isJsonPrimitive()) {
                String id = member.getAsString();
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return safe(fallback);
    }

    private static String displayFromObject(String fallback, JsonElement value, String field) {
        if (value != null && value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (String candidate : List.of("displayName", "name", field)) {
                JsonElement member = object.get(candidate);
                if (member != null && member.isJsonPrimitive()) {
                    String display = member.getAsString();
                    if (display != null && !display.isBlank()) {
                        return display;
                    }
                }
            }
        }
        return safe(fallback);
    }

    private static JsonElement canonicalElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return new JsonPrimitive(primitive.getAsString());
            }
            if (primitive.isBoolean()) {
                return new JsonPrimitive(primitive.getAsBoolean());
            }
            if (primitive.isNumber()) {
                return new JsonPrimitive(primitive.getAsNumber());
            }
            return primitive;
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                result.add(canonicalElement(child));
            }
            return result;
        }
        JsonObject result = new JsonObject();
        JsonObject object = element.getAsJsonObject();
        for (String key : new TreeSet<>(object.keySet())) {
            result.add(key, canonicalElement(object.get(key)));
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record ManifestLoadResult(SnapshotManifest manifest, boolean degraded, String message) {
        public ManifestLoadResult {
            manifest = manifest == null ? new SnapshotManifest() : manifest.normalized();
            message = message == null ? "" : message;
        }
    }

    public record PackageLoadResult(SnapshotPackage pack, boolean degraded, String message) {
        public PackageLoadResult {
            message = message == null ? "" : message;
        }
    }

    private record PackageValidationResult(SnapshotPackage pack, boolean degraded, String message) {
        private PackageValidationResult {
            message = message == null ? "" : message;
        }
    }

    public record SnapshotCollectionResult(List<SnapshotResource> resources, boolean degraded, List<String> warnings) {
        public SnapshotCollectionResult {
            resources = resources == null ? List.of() : List.copyOf(resources);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }
}
