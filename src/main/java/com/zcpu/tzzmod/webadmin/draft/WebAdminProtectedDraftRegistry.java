package com.zcpu.tzzmod.webadmin.draft;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WebAdminProtectedDraftRegistry {
    public static final String OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE = "virtual_block_device";
    public static final String OBJECT_TYPE_WORLD_DEVICE = "world_device";
    public static final String OBJECT_TYPE_REGION_CONTROLLER = "region_controller";
    public static final String OBJECT_TYPE_ITEM_SUBMIT_CAPTURE = "item_submit_capture";
    public static final String OBJECT_TYPE_CONTAINER_CAPTURE = "container_capture";

    public static final String STATE_SELECTING = "selecting";
    public static final String STATE_PLACED = "placed";
    public static final String STATE_SELECTED = "selected";
    public static final String STATE_DRAFT_CONFIGURING = "draft_configuring";
    public static final String STATE_SAVING = "saving";
    public static final String STATE_CANCELLED = "cancelled";
    public static final String STATE_EXPIRED = "expired";
    public static final String STATE_COMMITTED = "committed";

    private static final long DEFAULT_TTL_MILLIS = 15L * 60L * 1000L;
    private static final Map<String, ProtectedDraftEntry> ENTRIES = new LinkedHashMap<>();

    private WebAdminProtectedDraftRegistry() {
    }

    public static synchronized ProtectedDraftEntry start(
            String draftSessionId,
            String editLockId,
            WebAdminUser actor,
            WebAdminSession session,
            String boundPlayerUuid,
            String objectType,
            String objectId,
            Set<String> allowedOperations
    ) {
        String draftId = safe(draftSessionId).isBlank() ? UUID.randomUUID().toString() : safe(draftSessionId);
        expireStale();
        if (ENTRIES.containsKey(draftId)) {
            return null;
        }
        long now = System.currentTimeMillis();
        ProtectedDraftEntry entry = new ProtectedDraftEntry(
                draftId,
                safe(editLockId),
                actor == null ? "" : safe(actor.username),
                actor == null ? "" : safe(actor.username),
                session == null ? "" : safe(session.sessionIdHash),
                safe(boundPlayerUuid),
                normalizeObjectType(objectType),
                safe(objectId).isBlank() ? draftId : safe(objectId),
                draftId,
                "",
                "",
                "",
                0,
                0,
                0,
                "",
                "",
                "",
                Instant.ofEpochMilli(now).toString(),
                Instant.ofEpochMilli(now + DEFAULT_TTL_MILLIS).toString(),
                STATE_SELECTING,
                allowedOperations == null || allowedOperations.isEmpty()
                        ? Set.of("select", "cancel")
                        : Set.copyOf(allowedOperations),
                Map.of()
        );
        ENTRIES.put(entry.draftSessionId(), entry);
        return entry;
    }

    public static synchronized ProtectedDraftEntry registerForTest(ProtectedDraftEntry entry) {
        ProtectedDraftEntry safeEntry = entry == null ? null : entry.normalized();
        if (safeEntry != null && !safeEntry.draftSessionId().isBlank()) {
            ENTRIES.put(safeEntry.draftSessionId(), safeEntry);
        }
        return safeEntry;
    }

    public static synchronized ProtectedDraftEntry markSelectedBlock(
            String draftSessionId,
            String editLockId,
            String actor,
            String boundPlayerUuid,
            String objectType,
            String worldId,
            int x,
            int y,
            int z,
            String previousBlockState,
            String previousBlockEntitySnapshot,
            Map<String, ?> metadata
    ) {
        expireStale();
        ProtectedDraftEntry before = ENTRIES.get(safe(draftSessionId));
        if (before == null
                || before.isTerminal()
                || (!STATE_SELECTING.equals(before.state()) && !STATE_PLACED.equals(before.state()))) {
            return null;
        }
        if (!sameNonBlank(before.editLockId(), editLockId)
                || !sameNonBlank(before.actor(), actor)
                || !sameNonBlank(before.boundPlayerUuid(), boundPlayerUuid)
                || !before.objectType().equals(normalizeObjectType(objectType))) {
            return null;
        }
        long now = System.currentTimeMillis();
        ProtectedDraftEntry entry = new ProtectedDraftEntry(
                safe(draftSessionId).isBlank() ? UUID.randomUUID().toString() : safe(draftSessionId),
                firstNonBlank(editLockId, before == null ? "" : before.editLockId()),
                firstNonBlank(actor, before == null ? "" : before.actor()),
                firstNonBlank(actor, before == null ? "" : before.webAdminUserId()),
                before == null ? "" : before.webAdminSessionId(),
                firstNonBlank(boundPlayerUuid, before == null ? "" : before.boundPlayerUuid()),
                normalizeObjectType(objectType),
                before == null || before.objectId().isBlank() ? safe(draftSessionId) : before.objectId(),
                safe(draftSessionId),
                safe(worldId),
                safe(worldId),
                x + "," + y + "," + z,
                x,
                y,
                z,
                "",
                "",
                safe(previousBlockState),
                before == null ? Instant.ofEpochMilli(now).toString() : before.createdAt(),
                before == null ? Instant.ofEpochMilli(now + DEFAULT_TTL_MILLIS).toString() : before.expiresAt(),
                STATE_SELECTED,
                before == null ? Set.of("configure", "commit", "cancel") : mergeAllowed(before.allowedOperations(), Set.of("configure", "commit", "cancel")),
                metadata == null ? Map.of() : Map.copyOf(metadata)
        ).withPreviousBlockEntitySnapshot(previousBlockEntitySnapshot);
        ENTRIES.put(entry.draftSessionId(), entry);
        return entry;
    }

    public static synchronized ProtectedDraftEntry get(String draftSessionId) {
        expireStale();
        return ENTRIES.get(safe(draftSessionId));
    }

    public static synchronized List<String> validateForLogicChainSave(
            String draftSessionId,
            String editLockId,
            WebAdminUser actor,
            String requiredObjectType
    ) {
        expireStale();
        ProtectedDraftEntry entry = ENTRIES.get(safe(draftSessionId));
        if (entry == null) {
            return List.of("protected draft 不存在或已过期。");
        }
        if (!entry.state().equals(STATE_SELECTED)
                && !entry.state().equals(STATE_PLACED)
                && !entry.state().equals(STATE_DRAFT_CONFIGURING)) {
            return List.of("protected draft 当前状态不能保存：" + entry.state());
        }
        if (!normalizeObjectType(requiredObjectType).equals(entry.objectType())) {
            return List.of("protected draft 类型不匹配：" + entry.objectType());
        }
        if (!entry.editLockId().isBlank() && !safe(editLockId).equals(entry.editLockId())) {
            return List.of("protected draft 绑定的 edit lock 不匹配。");
        }
        String actorName = actor == null ? "" : safe(actor.username);
        if (!entry.actor().isBlank() && actorName.isBlank()) {
            return List.of("protected draft 只能由原 WebAdmin 用户提交。");
        }
        if (!entry.actor().isBlank() && !entry.actor().equalsIgnoreCase(actorName)) {
            return List.of("protected draft 只能由原 WebAdmin 用户提交。");
        }
        if (!entry.allowedOperations().contains("commit")) {
            return List.of("protected draft 当前不允许 commit。");
        }
        return List.of();
    }

    public static synchronized ProtectedDraftEntry markSaving(
            String draftSessionId,
            String editLockId,
            WebAdminUser actor,
            String requiredObjectType
    ) {
        List<String> violations = validateForLogicChainSave(draftSessionId, editLockId, actor, requiredObjectType);
        if (!violations.isEmpty()) {
            return null;
        }
        return updateActiveState(draftSessionId, STATE_SAVING);
    }

    public static synchronized ProtectedDraftEntry markCommitted(String draftSessionId) {
        ProtectedDraftEntry entry = ENTRIES.get(safe(draftSessionId));
        if (entry == null || entry.isTerminal() || !STATE_SAVING.equals(entry.state())) {
            return null;
        }
        ProtectedDraftEntry updated = entry.withState(STATE_COMMITTED);
        ENTRIES.put(updated.draftSessionId(), updated);
        return updated;
    }

    public static synchronized ProtectedDraftEntry markCommitFailed(String draftSessionId, String reason) {
        ProtectedDraftEntry entry = ENTRIES.get(safe(draftSessionId));
        if (entry == null || entry.isTerminal()) {
            return null;
        }
        ProtectedDraftEntry updated = entry.withState(STATE_DRAFT_CONFIGURING)
                .withMetadata("lastCommitFailure", safe(reason));
        ENTRIES.put(updated.draftSessionId(), updated);
        return updated;
    }

    public static synchronized ProtectedDraftEntry markExpired(String draftSessionId, String reason) {
        ProtectedDraftEntry entry = ENTRIES.get(safe(draftSessionId));
        if (entry == null || entry.isTerminal() || STATE_SAVING.equals(entry.state())) {
            return null;
        }
        ProtectedDraftEntry updated = entry.withState(STATE_EXPIRED)
                .withMetadata("expireReason", safe(reason));
        ENTRIES.put(updated.draftSessionId(), updated);
        return updated;
    }

    public static synchronized ProtectedDraftEntry cancel(String draftSessionId) {
        return updateActiveState(draftSessionId, STATE_CANCELLED);
    }

    public static synchronized void cancelByEditLock(String editLockId) {
        String lock = safe(editLockId);
        if (lock.isBlank()) {
            return;
        }
        for (ProtectedDraftEntry entry : List.copyOf(ENTRIES.values())) {
            if (lock.equals(entry.editLockId())) {
                updateActiveState(entry.draftSessionId(), STATE_CANCELLED);
            }
        }
    }

    public static synchronized boolean isProtected(String objectType, String worldId, int x, int y, int z) {
        expireStale();
        String normalizedType = normalizeObjectType(objectType);
        String normalizedWorld = safe(worldId);
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (!entry.isActive()) {
                continue;
            }
            if (entry.objectType().equals(normalizedType)
                    && entry.worldId().equals(normalizedWorld)
                    && entry.x() == x
                    && entry.y() == y
                    && entry.z() == z) {
                return true;
            }
        }
        return false;
    }

    public static synchronized ProtectedDraftEntry findActiveByWorldPos(Set<String> objectTypes, String worldId, int x, int y, int z) {
        expireStale();
        Set<String> normalizedTypes = new LinkedHashSet<>();
        for (String objectType : objectTypes == null ? Set.<String>of() : objectTypes) {
            normalizedTypes.add(normalizeObjectType(objectType));
        }
        String normalizedWorld = safe(worldId);
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (!entry.isActive()) {
                continue;
            }
            if (!normalizedTypes.isEmpty() && !normalizedTypes.contains(entry.objectType())) {
                continue;
            }
            if (entry.worldId().equals(normalizedWorld)
                    && entry.x() == x
                    && entry.y() == y
                    && entry.z() == z) {
                return entry;
            }
        }
        return null;
    }

    public static synchronized List<ProtectedDraftEntry> activeByEditLock(String editLockId) {
        expireStale();
        String lock = safe(editLockId);
        if (lock.isBlank()) {
            return List.of();
        }
        java.util.ArrayList<ProtectedDraftEntry> result = new java.util.ArrayList<>();
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (entry.isActive() && lock.equals(entry.editLockId())) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized List<ProtectedDraftEntry> activeByObjectType(String objectType) {
        expireStale();
        String normalizedType = normalizeObjectType(objectType);
        java.util.ArrayList<ProtectedDraftEntry> result = new java.util.ArrayList<>();
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (entry.isActive() && entry.objectType().equals(normalizedType)) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized List<ProtectedDraftEntry> activeExpiredByObjectType(String objectType, long nowMillis) {
        String normalizedType = normalizeObjectType(objectType);
        java.util.ArrayList<ProtectedDraftEntry> result = new java.util.ArrayList<>();
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (entry.isActive()
                    && !STATE_SAVING.equals(entry.state())
                    && entry.objectType().equals(normalizedType)
                    && entry.expiresAtMillis() < nowMillis) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized boolean canMutateProtectedObject(
            String objectType,
            String objectId,
            String editLockId,
            String draftSessionId,
            WebAdminUser actor
    ) {
        expireStale();
        String normalizedType = normalizeObjectType(objectType);
        String normalizedId = safe(objectId);
        String lock = safe(editLockId);
        String sessionId = safe(draftSessionId);
        String actorName = actor == null ? "" : safe(actor.username);
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (!entry.isActive() || !entry.objectType().equals(normalizedType)) {
                continue;
            }
            if (!normalizedId.isBlank() && !normalizedId.equals(entry.objectId()) && !normalizedId.equals(entry.draftId())) {
                continue;
            }
            if (!entry.actor().isBlank() && (actorName.isBlank() || !entry.actor().equalsIgnoreCase(actorName))) {
                return false;
            }
            return (!lock.isBlank() && lock.equals(entry.editLockId()))
                    || (!sessionId.isBlank() && sessionId.equals(entry.draftSessionId()));
        }
        return true;
    }

    public static synchronized Map<String, Object> summary(String draftSessionId) {
        ProtectedDraftEntry entry = get(draftSessionId);
        return entry == null ? Map.of() : entry.toMap();
    }

    public static synchronized int expireOld() {
        int before = expiredCount();
        expireStale();
        return Math.max(0, expiredCount() - before);
    }

    public static synchronized void cancelAll(String reason) {
        for (ProtectedDraftEntry entry : List.copyOf(ENTRIES.values())) {
            if (!entry.isTerminal() && !STATE_SAVING.equals(entry.state())) {
                if (requiresServerCleanupBeforeTerminal(entry)) {
                    continue;
                }
                ENTRIES.put(entry.draftSessionId(), entry.withState(STATE_CANCELLED).withMetadata("cancelReason", safe(reason)));
            }
        }
    }

    public static synchronized void clearForTests() {
        ENTRIES.clear();
    }

    private static ProtectedDraftEntry updateActiveState(String draftSessionId, String state) {
        ProtectedDraftEntry entry = ENTRIES.get(safe(draftSessionId));
        if (entry == null) {
            return null;
        }
        if (entry.isTerminal()) {
            return entry;
        }
        if (STATE_CANCELLED.equals(state) && STATE_SAVING.equals(entry.state())) {
            return entry;
        }
        ProtectedDraftEntry updated = entry.withState(state);
        ENTRIES.put(updated.draftSessionId(), updated);
        return updated;
    }

    private static void expireStale() {
        long now = System.currentTimeMillis();
        for (ProtectedDraftEntry entry : List.copyOf(ENTRIES.values())) {
            if (!entry.isTerminal() && !STATE_SAVING.equals(entry.state()) && entry.expiresAtMillis() < now) {
                if (requiresServerCleanupBeforeTerminal(entry)) {
                    continue;
                }
                ENTRIES.put(entry.draftSessionId(), entry.withState(STATE_EXPIRED));
            }
        }
    }

    private static boolean requiresServerCleanupBeforeTerminal(ProtectedDraftEntry entry) {
        return entry != null
                && OBJECT_TYPE_WORLD_DEVICE.equals(entry.objectType())
                && !safe(entry.worldId()).isBlank()
                && !safe(entry.blockPos()).isBlank()
                && !entry.previousBlockState().isBlank();
    }

    private static int expiredCount() {
        int count = 0;
        for (ProtectedDraftEntry entry : ENTRIES.values()) {
            if (STATE_EXPIRED.equals(entry.state())) {
                count++;
            }
        }
        return count;
    }

    private static Set<String> mergeAllowed(Set<String> left, Set<String> right) {
        Set<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return Set.copyOf(merged);
    }

    private static String normalizeObjectType(String value) {
        String safe = safe(value).toLowerCase();
        return switch (safe) {
            case "vbd", "virtual_block", "virtual_block_device" -> OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE;
            case "world", "world_device", "signal_device" -> OBJECT_TYPE_WORLD_DEVICE;
            case "region", "region_controller" -> OBJECT_TYPE_REGION_CONTROLLER;
            case "itemsubmit", "item_submit", "item_submit_capture" -> OBJECT_TYPE_ITEM_SUBMIT_CAPTURE;
            case "container", "container_capture" -> OBJECT_TYPE_CONTAINER_CAPTURE;
            default -> safe;
        };
    }

    private static String firstNonBlank(String first, String second) {
        return safe(first).isBlank() ? safe(second) : safe(first);
    }

    private static boolean sameNonBlank(String expected, String actual) {
        String left = safe(expected);
        String right = safe(actual);
        return left.isBlank() || right.isBlank() || left.equalsIgnoreCase(right);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record ProtectedDraftEntry(
            String draftSessionId,
            String editLockId,
            String webAdminUserId,
            String actor,
            String webAdminSessionId,
            String boundPlayerUuid,
            String objectType,
            String objectId,
            String draftId,
            String worldId,
            String dimension,
            String blockPos,
            int x,
            int y,
            int z,
            String regionId,
            String deviceId,
            String previousBlockState,
            String createdAt,
            String expiresAt,
            String state,
            Set<String> allowedOperations,
            Map<String, ?> metadata
    ) {
        public ProtectedDraftEntry {
            draftSessionId = safe(draftSessionId);
            editLockId = safe(editLockId);
            webAdminUserId = safe(webAdminUserId);
            actor = safe(actor);
            webAdminSessionId = safe(webAdminSessionId);
            boundPlayerUuid = safe(boundPlayerUuid);
            objectType = normalizeObjectType(objectType);
            objectId = safe(objectId);
            draftId = safe(draftId).isBlank() ? draftSessionId : safe(draftId);
            worldId = safe(worldId);
            dimension = safe(dimension).isBlank() ? worldId : safe(dimension);
            blockPos = safe(blockPos);
            regionId = safe(regionId);
            deviceId = safe(deviceId);
            previousBlockState = safe(previousBlockState);
            createdAt = safe(createdAt).isBlank() ? Instant.now().toString() : safe(createdAt);
            expiresAt = safe(expiresAt).isBlank() ? Instant.ofEpochMilli(System.currentTimeMillis() + DEFAULT_TTL_MILLIS).toString() : safe(expiresAt);
            state = safe(state).isBlank() ? STATE_SELECTING : safe(state);
            allowedOperations = allowedOperations == null ? Set.of() : Set.copyOf(allowedOperations);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public ProtectedDraftEntry normalized() {
            return new ProtectedDraftEntry(
                    draftSessionId,
                    editLockId,
                    webAdminUserId,
                    actor,
                    webAdminSessionId,
                    boundPlayerUuid,
                    objectType,
                    objectId,
                    draftId,
                    worldId,
                    dimension,
                    blockPos,
                    x,
                    y,
                    z,
                    regionId,
                    deviceId,
                    previousBlockState,
                    createdAt,
                    expiresAt,
                    state,
                    allowedOperations,
                    metadata
            );
        }

        public ProtectedDraftEntry withState(String nextState) {
            return new ProtectedDraftEntry(
                    draftSessionId, editLockId, webAdminUserId, actor, webAdminSessionId, boundPlayerUuid,
                    objectType, objectId, draftId, worldId, dimension, blockPos, x, y, z, regionId, deviceId,
                    previousBlockState, createdAt, expiresAt, nextState, allowedOperations, metadata
            );
        }

        public ProtectedDraftEntry withPreviousBlockEntitySnapshot(String snapshot) {
            Map<String, Object> nextMetadata = new LinkedHashMap<>(metadata);
            nextMetadata.put("previousBlockEntitySnapshot", safe(snapshot));
            return new ProtectedDraftEntry(
                    draftSessionId, editLockId, webAdminUserId, actor, webAdminSessionId, boundPlayerUuid,
                    objectType, objectId, draftId, worldId, dimension, blockPos, x, y, z, regionId, deviceId,
                    previousBlockState, createdAt, expiresAt, state, allowedOperations, nextMetadata
            );
        }

        public ProtectedDraftEntry withMetadata(String key, String value) {
            Map<String, Object> nextMetadata = new LinkedHashMap<>(metadata);
            nextMetadata.put(safe(key), safe(value));
            return new ProtectedDraftEntry(
                    draftSessionId, editLockId, webAdminUserId, actor, webAdminSessionId, boundPlayerUuid,
                    objectType, objectId, draftId, worldId, dimension, blockPos, x, y, z, regionId, deviceId,
                    previousBlockState, createdAt, expiresAt, state, allowedOperations, nextMetadata
            );
        }

        public boolean isActive() {
            return !isTerminal();
        }

        public boolean isTerminal() {
            return Set.of(STATE_CANCELLED, STATE_EXPIRED, STATE_COMMITTED).contains(state);
        }

        public long expiresAtMillis() {
            try {
                return Instant.parse(expiresAt).toEpochMilli();
            } catch (Exception ignored) {
                return 0L;
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("draftSessionId", draftSessionId);
            data.put("editLockId", editLockId);
            data.put("webAdminUserId", webAdminUserId);
            data.put("actor", actor);
            data.put("webAdminSessionId", webAdminSessionId);
            data.put("boundPlayerUuid", boundPlayerUuid);
            data.put("objectType", objectType);
            data.put("objectId", objectId);
            data.put("draftId", draftId);
            data.put("worldId", worldId);
            data.put("dimension", dimension);
            data.put("blockPos", blockPos);
            data.put("x", x);
            data.put("y", y);
            data.put("z", z);
            data.put("regionId", regionId);
            data.put("deviceId", deviceId);
            data.put("previousBlockState", previousBlockState);
            Object previousBlockEntitySnapshot = metadata.get("previousBlockEntitySnapshot");
            data.put("previousBlockEntitySnapshot", previousBlockEntitySnapshot == null ? "" : previousBlockEntitySnapshot);
            data.put("createdAt", createdAt);
            data.put("expiresAt", expiresAt);
            data.put("state", state);
            data.put("allowedOperations", List.copyOf(allowedOperations));
            data.put("metadata", metadata);
            return data;
        }
    }
}
