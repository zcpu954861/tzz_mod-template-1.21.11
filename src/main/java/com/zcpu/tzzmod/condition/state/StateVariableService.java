package com.zcpu.tzzmod.condition.state;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class StateVariableService {
    private final Path path;

    public StateVariableService(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public StateVariableSnapshot snapshot() {
        return StateVariableStore.loadSnapshot(path);
    }

    public StateVariableWriteResult set(StateVariableUpdateRequest request, String actor) {
        List<String> validationErrors = StateVariableValidation.validateUpdate(request).stream()
                .map(StateVariableValidation.Issue::message)
                .toList();
        if (!validationErrors.isEmpty()) {
            return StateVariableWriteResult.failed("validation_error", "状态变量校验失败。", validationErrors, null);
        }

        StateVariableScope scope = request.scope();
        String targetId = StateVariableValidation.normalizeTargetId(scope, request.targetId());
        String key = StateVariableValidation.normalizeKey(request.key());
        StateVariableSnapshot snapshot = snapshot();
        StateVariableRecord current = snapshot.get(scope, targetId, key).orElse(null);
        if (request.expectedFingerprint() != null && !request.expectedFingerprint().isBlank()) {
            if (current == null || !request.expectedFingerprint().trim().equals(current.fingerprint())) {
                return StateVariableWriteResult.failed(
                        "fingerprint_mismatch",
                        "状态变量指纹不匹配，请刷新后重试。",
                        List.of("状态变量已被其他写入更新。"),
                        current
                );
            }
        }

        long version = current == null ? 1L : current.version() + 1L;
        StateVariableRecord next = StateVariableRecord.create(
                scope,
                targetId,
                key,
                request.type(),
                request.value(),
                request.displayName(),
                request.note(),
                Instant.now().toEpochMilli(),
                actor == null ? "" : actor,
                version
        );
        if (current != null && current.sameEditableValue(next)) {
            return StateVariableWriteResult.success("unchanged", "状态变量没有变化。", current, false);
        }

        boolean saved = StateVariableStore.saveSnapshot(path, snapshot.with(next));
        if (!saved) {
            return StateVariableWriteResult.failed("write_failed", "状态变量保存失败。", List.of("无法写入状态变量存储文件。"), current);
        }
        return StateVariableWriteResult.success(current == null ? "created" : "updated", current == null ? "状态变量已创建。" : "状态变量已更新。", next, true);
    }

    public StateVariableWriteResult remove(StateVariableScope scope, String targetId, String key, String expectedFingerprint) {
        List<String> validationErrors = StateVariableValidation.validateKeyOnly(scope, targetId, key).stream()
                .map(StateVariableValidation.Issue::message)
                .toList();
        if (!validationErrors.isEmpty()) {
            return StateVariableWriteResult.failed("validation_error", "状态变量校验失败。", validationErrors, null);
        }

        String normalizedTargetId = StateVariableValidation.normalizeTargetId(scope, targetId);
        String normalizedKey = StateVariableValidation.normalizeKey(key);
        StateVariableSnapshot snapshot = snapshot();
        StateVariableRecord current = snapshot.get(scope, normalizedTargetId, normalizedKey).orElse(null);
        if (current == null) {
            return StateVariableWriteResult.failed("not_found", "状态变量不存在。", List.of("状态变量不存在：" + new StateVariableKey(scope, normalizedTargetId, normalizedKey).displayPath()), null);
        }
        if (expectedFingerprint != null && !expectedFingerprint.isBlank() && !expectedFingerprint.trim().equals(current.fingerprint())) {
            return StateVariableWriteResult.failed(
                    "fingerprint_mismatch",
                    "状态变量指纹不匹配，请刷新后重试。",
                    List.of("状态变量已被其他写入更新。"),
                    current
            );
        }

        boolean saved = StateVariableStore.saveSnapshot(path, snapshot.without(scope, normalizedTargetId, normalizedKey));
        if (!saved) {
            return StateVariableWriteResult.failed("write_failed", "状态变量删除失败。", List.of("无法写入状态变量存储文件。"), current);
        }
        return StateVariableWriteResult.success("deleted", "状态变量已删除。", current, true);
    }
}
