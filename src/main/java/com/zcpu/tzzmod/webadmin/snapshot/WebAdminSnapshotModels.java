package com.zcpu.tzzmod.webadmin.snapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminSnapshotModels {
    public static final int SCHEMA_VERSION = 1;

    private WebAdminSnapshotModels() {
    }

    public enum SnapshotKind {
        MANUAL("manual", "手动保存点"),
        AUTO("auto", "自动快照"),
        PRE_ROLLBACK("pre_rollback", "回滚前保护点");

        private final String id;
        private final String displayName;

        SnapshotKind(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public static SnapshotKind parse(String value) {
            String safe = safe(value);
            for (SnapshotKind kind : values()) {
                if (kind.id.equalsIgnoreCase(safe) || kind.name().equalsIgnoreCase(safe)) {
                    return kind;
                }
            }
            return AUTO;
        }
    }

    public static final class SnapshotManifest {
        public int schemaVersion = SCHEMA_VERSION;
        public long nextSequence = 1L;
        public List<SnapshotRecord> records = new ArrayList<>();
        public String manifestFingerprint = "";
        public List<String> warnings = new ArrayList<>();

        public SnapshotManifest normalized() {
            SnapshotManifest copy = new SnapshotManifest();
            copy.schemaVersion = SCHEMA_VERSION;
            copy.nextSequence = Math.max(1L, nextSequence);
            if (records != null) {
                for (SnapshotRecord record : records) {
                    if (record == null || safe(record.snapshotId).isBlank()) {
                        continue;
                    }
                    SnapshotRecord normalized = record.normalized();
                    copy.records.add(normalized);
                    copy.nextSequence = Math.max(copy.nextSequence, normalized.sequence + 1L);
                }
                copy.records.sort((left, right) -> Long.compare(left.sequence, right.sequence));
            }
            copy.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
            copy.manifestFingerprint = safe(manifestFingerprint);
            return copy;
        }
    }

    public static final class SnapshotRecord {
        public String snapshotId = "";
        public long sequence = 0L;
        public String createdAt = "";
        public String createdBy = "";
        public String kind = SnapshotKind.AUTO.id();
        public String title = "";
        public String note = "";
        public List<String> tags = new ArrayList<>();
        public SnapshotTrigger trigger = new SnapshotTrigger();
        public String previousSnapshotId = "";
        public Map<String, Integer> resourceCounts = new LinkedHashMap<>();
        public SnapshotDiffSummary diffSummary = new SnapshotDiffSummary();
        public SnapshotDiff operationDiff = new SnapshotDiff();
        public String packageFingerprint = "";
        public String storagePath = "";
        public List<String> warnings = new ArrayList<>();

        public SnapshotRecord normalized() {
            SnapshotRecord copy = new SnapshotRecord();
            copy.snapshotId = safe(snapshotId);
            copy.sequence = Math.max(0L, sequence);
            copy.createdAt = safe(createdAt);
            copy.createdBy = safe(createdBy);
            copy.kind = SnapshotKind.parse(kind).id();
            copy.title = safe(title);
            copy.note = safe(note);
            copy.tags = normalizeTags(tags);
            copy.trigger = trigger == null ? new SnapshotTrigger() : trigger.normalized();
            copy.previousSnapshotId = safe(previousSnapshotId);
            copy.resourceCounts = normalizeIntegerMap(resourceCounts);
            copy.diffSummary = diffSummary == null ? new SnapshotDiffSummary() : diffSummary.normalized();
            copy.operationDiff = operationDiff == null ? new SnapshotDiff() : operationDiff.normalized();
            copy.packageFingerprint = safe(packageFingerprint);
            copy.storagePath = safe(storagePath);
            copy.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
            return copy;
        }
    }

    public static final class SnapshotTrigger {
        public String operation = "";
        public String module = "";
        public String targetType = "";
        public String targetId = "";
        public String reason = "";
        public String routeTarget = "";

        public SnapshotTrigger normalized() {
            SnapshotTrigger copy = new SnapshotTrigger();
            copy.operation = safe(operation);
            copy.module = safe(module);
            copy.targetType = safe(targetType);
            copy.targetId = safe(targetId);
            copy.reason = safe(reason);
            copy.routeTarget = safe(routeTarget);
            return copy;
        }
    }

    public static final class SnapshotPackage {
        public int schemaVersion = SCHEMA_VERSION;
        public String snapshotId = "";
        public long sequence = 0L;
        public String createdAt = "";
        public String createdBy = "";
        public String kind = SnapshotKind.AUTO.id();
        public SnapshotTrigger trigger = new SnapshotTrigger();
        public List<SnapshotResource> resources = new ArrayList<>();
        public String packageFingerprint = "";
        public List<String> warnings = new ArrayList<>();

        public SnapshotPackage normalized() {
            SnapshotPackage copy = new SnapshotPackage();
            copy.schemaVersion = SCHEMA_VERSION;
            copy.snapshotId = safe(snapshotId);
            copy.sequence = Math.max(0L, sequence);
            copy.createdAt = safe(createdAt);
            copy.createdBy = safe(createdBy);
            copy.kind = SnapshotKind.parse(kind).id();
            copy.trigger = trigger == null ? new SnapshotTrigger() : trigger.normalized();
            if (resources != null) {
                for (SnapshotResource resource : resources) {
                    if (resource == null || safe(resource.resourceType).isBlank() || safe(resource.resourceId).isBlank()) {
                        continue;
                    }
                    copy.resources.add(resource.normalized());
                }
            }
            copy.resources.sort((left, right) -> (left.resourceType + "\n" + left.resourceId).compareTo(right.resourceType + "\n" + right.resourceId));
            copy.packageFingerprint = safe(packageFingerprint);
            copy.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
            return copy;
        }
    }

    public static final class SnapshotResource {
        public String resourceType = "";
        public String resourceId = "";
        public String displayName = "";
        public String sourceStore = "";
        public String pathKey = "";
        public String canonicalJson = "";
        public String fingerprint = "";
        public boolean restoreResource = false;
        public Map<String, String> metadata = new LinkedHashMap<>();

        public SnapshotResource normalized() {
            SnapshotResource copy = new SnapshotResource();
            copy.resourceType = safe(resourceType);
            copy.resourceId = safe(resourceId);
            copy.displayName = safe(displayName);
            copy.sourceStore = safe(sourceStore);
            copy.pathKey = safe(pathKey);
            copy.canonicalJson = safe(canonicalJson);
            copy.fingerprint = WebAdminSnapshotStore.hash(copy.canonicalJson);
            copy.restoreResource = restoreResource;
            copy.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
            return copy;
        }

        public String resourceKey() {
            return safe(resourceType) + "\n" + safe(resourceId);
        }
    }

    public static final class SnapshotDiffSummary {
        public int created = 0;
        public int updated = 0;
        public int deleted = 0;
        public int unchanged = 0;
        public Map<String, Integer> byType = new LinkedHashMap<>();
        public List<String> warnings = new ArrayList<>();

        public SnapshotDiffSummary normalized() {
            SnapshotDiffSummary copy = new SnapshotDiffSummary();
            copy.created = Math.max(0, created);
            copy.updated = Math.max(0, updated);
            copy.deleted = Math.max(0, deleted);
            copy.unchanged = Math.max(0, unchanged);
            copy.byType = normalizeIntegerMap(byType);
            copy.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
            return copy;
        }

        public int changed() {
            return created + updated + deleted;
        }
    }

    public static final class SnapshotDiff {
        public SnapshotDiffSummary summary = new SnapshotDiffSummary();
        public List<SnapshotDiffEntry> entries = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public SnapshotDiff normalized() {
            SnapshotDiff copy = new SnapshotDiff();
            copy.summary = summary == null ? new SnapshotDiffSummary() : summary.normalized();
            if (entries != null) {
                for (SnapshotDiffEntry entry : entries) {
                    if (entry != null) {
                        copy.entries.add(entry.normalized());
                    }
                }
            }
            copy.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
            return copy;
        }
    }

    public static final class SnapshotDiffEntry {
        public String changeType = "";
        public String resourceType = "";
        public String resourceId = "";
        public String displayName = "";
        public String sourceStore = "";
        public String beforeFingerprint = "";
        public String afterFingerprint = "";
        public String beforeSummary = "";
        public String afterSummary = "";
        public String beforeJsonPreview = "";
        public String afterJsonPreview = "";
        public List<SnapshotFieldDiff> fieldDiffs = new ArrayList<>();
        public int omittedFieldDiffs = 0;

        public SnapshotDiffEntry normalized() {
            SnapshotDiffEntry copy = new SnapshotDiffEntry();
            copy.changeType = safe(changeType);
            copy.resourceType = safe(resourceType);
            copy.resourceId = safe(resourceId);
            copy.displayName = safe(displayName);
            copy.sourceStore = safe(sourceStore);
            copy.beforeFingerprint = safe(beforeFingerprint);
            copy.afterFingerprint = safe(afterFingerprint);
            copy.beforeSummary = safe(beforeSummary);
            copy.afterSummary = safe(afterSummary);
            copy.beforeJsonPreview = safe(beforeJsonPreview);
            copy.afterJsonPreview = safe(afterJsonPreview);
            if (fieldDiffs != null) {
                for (SnapshotFieldDiff fieldDiff : fieldDiffs) {
                    if (fieldDiff != null) {
                        copy.fieldDiffs.add(fieldDiff.normalized());
                    }
                }
            }
            copy.omittedFieldDiffs = Math.max(0, omittedFieldDiffs);
            return copy;
        }
    }

    public static final class SnapshotFieldDiff {
        public String field = "";
        public String changeType = "";
        public String beforeValue = "";
        public String afterValue = "";

        public SnapshotFieldDiff normalized() {
            SnapshotFieldDiff copy = new SnapshotFieldDiff();
            copy.field = safe(field);
            copy.changeType = safe(changeType);
            copy.beforeValue = safe(beforeValue);
            copy.afterValue = safe(afterValue);
            return copy;
        }
    }

    public static final class RollbackPlan {
        public String snapshotId = "";
        public long targetSequence = 0L;
        public String currentFingerprint = "";
        public String targetFingerprint = "";
        public String manifestFingerprint = "";
        public String dryRunFingerprint = "";
        public List<RollbackOperation> operations = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> blockers = new ArrayList<>();
        public SnapshotDiffSummary summary = new SnapshotDiffSummary();
    }

    public static final class RollbackOperation {
        public String operation = "";
        public String pathKey = "";
        public String resourceType = "store_file";
        public String resourceId = "";
        public String displayName = "";
        public String beforeFingerprint = "";
        public String afterFingerprint = "";
        public boolean destructive = false;
    }

    public record StoreSpec(String pathKey, String resourceType, String displayName, String relativePath, String module, boolean worldTzzMod) {
    }

    private static List<String> normalizeTags(List<String> raw) {
        List<String> result = new ArrayList<>();
        if (raw != null) {
            for (String item : raw) {
                String value = safe(item).trim();
                if (!value.isBlank() && result.size() < 12 && !result.contains(value)) {
                    result.add(value.length() > 32 ? value.substring(0, 32) : value);
                }
            }
        }
        return result;
    }

    private static Map<String, Integer> normalizeIntegerMap(Map<String, Integer> raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (raw != null) {
            raw.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> result.put(safe(entry.getKey()), Math.max(0, entry.getValue() == null ? 0 : entry.getValue())));
        }
        return result;
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }
}
