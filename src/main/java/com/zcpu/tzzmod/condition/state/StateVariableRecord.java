package com.zcpu.tzzmod.condition.state;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public record StateVariableRecord(
        String id,
        StateVariableScope scope,
        String targetId,
        String key,
        StateVariableType type,
        String value,
        String displayName,
        String note,
        long updatedAt,
        String updatedBy,
        long version,
        String fingerprint
) {
    public StateVariableRecord {
        scope = scope == null ? StateVariableScope.GLOBAL : scope;
        targetId = StateVariableValidation.normalizeTargetId(scope, targetId);
        key = StateVariableValidation.normalizeKey(key);
        type = type == null ? StateVariableType.STRING : type;
        value = StateVariableValidation.normalizeValue(type, value);
        displayName = displayName == null ? "" : displayName.trim();
        note = note == null ? "" : note.trim();
        updatedAt = updatedAt <= 0L ? Instant.now().toEpochMilli() : updatedAt;
        updatedBy = updatedBy == null ? "" : updatedBy.trim();
        version = Math.max(1L, version);
        String stableId = new StateVariableKey(scope, targetId, key).stableId();
        id = (id == null || id.isBlank()) ? stableId : id.trim();
        fingerprint = (fingerprint == null || fingerprint.isBlank())
                ? fingerprintFor(id, scope, targetId, key, type, value, displayName, note, version)
                : fingerprint.trim();
    }

    public StateVariableRecord withVersion(long nextVersion, long nextUpdatedAt, String actor) {
        return create(scope, targetId, key, type, value, displayName, note, nextUpdatedAt, actor, nextVersion);
    }

    public static StateVariableRecord create(
            StateVariableScope scope,
            String targetId,
            String key,
            StateVariableType type,
            String value,
            String displayName,
            String note,
            long updatedAt,
            String updatedBy,
            long version
    ) {
        StateVariableScope normalizedScope = scope == null ? StateVariableScope.GLOBAL : scope;
        String normalizedTargetId = StateVariableValidation.normalizeTargetId(normalizedScope, targetId);
        String normalizedKey = StateVariableValidation.normalizeKey(key);
        StateVariableType normalizedType = type == null ? StateVariableType.STRING : type;
        String normalizedValue = StateVariableValidation.normalizeValue(normalizedType, value);
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        String normalizedNote = note == null ? "" : note.trim();
        long normalizedVersion = Math.max(1L, version);
        String stableId = new StateVariableKey(normalizedScope, normalizedTargetId, normalizedKey).stableId();
        String fp = fingerprintFor(
                stableId,
                normalizedScope,
                normalizedTargetId,
                normalizedKey,
                normalizedType,
                normalizedValue,
                normalizedDisplayName,
                normalizedNote,
                normalizedVersion
        );
        return new StateVariableRecord(
                stableId,
                normalizedScope,
                normalizedTargetId,
                normalizedKey,
                normalizedType,
                normalizedValue,
                normalizedDisplayName,
                normalizedNote,
                updatedAt,
                updatedBy,
                normalizedVersion,
                fp
        );
    }

    public boolean sameEditableValue(StateVariableRecord other) {
        return other != null
                && scope == other.scope
                && Objects.equals(targetId, other.targetId)
                && Objects.equals(key, other.key)
                && type == other.type
                && Objects.equals(value, other.value)
                && Objects.equals(displayName, other.displayName)
                && Objects.equals(note, other.note);
    }

    private static String fingerprintFor(
            String id,
            StateVariableScope scope,
            String targetId,
            String key,
            StateVariableType type,
            String value,
            String displayName,
            String note,
            long version
    ) {
        String canonical = String.join("\n",
                nullToEmpty(id),
                scope == null ? "" : scope.name(),
                nullToEmpty(targetId),
                nullToEmpty(key),
                type == null ? "" : type.name(),
                nullToEmpty(value),
                nullToEmpty(displayName),
                nullToEmpty(note),
                Long.toString(version)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(canonical.hashCode());
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
