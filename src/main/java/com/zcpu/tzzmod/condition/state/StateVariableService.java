package com.zcpu.tzzmod.condition.state;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class StateVariableService {
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private final Path path;

    public StateVariableService(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public StateVariableSnapshot snapshot() {
        synchronized (lock()) {
            return StateVariableStore.loadSnapshotCached(path);
        }
    }

    public StateVariableStore.StateVariableLoadResult snapshotWithStatus() {
        synchronized (lock()) {
            return StateVariableStore.loadSnapshotWithStatusCached(path);
        }
    }

    public StateVariableWriteResult set(StateVariableUpdateRequest request, String actor) {
        synchronized (lock()) {
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
    }

    public StateVariableWriteResult remove(StateVariableScope scope, String targetId, String key, String expectedFingerprint) {
        synchronized (lock()) {
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

    public StateVariableMutationResult mutate(StateVariableMutationRequest request, String actor) {
        long started = System.nanoTime();
        synchronized (lock()) {
            List<StateVariableMutationValidation.Issue> issues = StateVariableMutationValidation.validate(request);
            if (!issues.isEmpty()) {
                return failed(
                        "validation_error",
                        "状态变量动作校验失败。",
                        request,
                        request == null ? "" : StateVariableMutationValidation.resolvedTargetId(request),
                        request == null ? "" : request.key(),
                        null,
                        issues.stream().map(StateVariableMutationValidation.Issue::message).toList(),
                        started
                );
            }

            String targetId = StateVariableMutationValidation.resolvedTargetId(request);
            if (request.scope() == StateVariableScope.PLAYER
                    && request.targetMode() == StateVariableTargetMode.CONTEXT_PLAYER
                    && targetId.isBlank()) {
                return failed(
                        "missing_context_player",
                        "状态变量动作需要触发玩家，但当前动作上下文没有玩家。",
                        request,
                        "",
                        request.key(),
                        null,
                        List.of("PLAYER + context_player 需要运行时提供触发玩家。"),
                        started
                );
            }

            String key = StateVariableValidation.normalizeKey(request.key());
            StateVariableSnapshot snapshot = snapshot();
            StateVariableRecord current = snapshot.get(request.scope(), targetId, key).orElse(null);
            return switch (request.operation()) {
                case SET_VARIABLE -> mutateSet(snapshot, request, targetId, key, current, actor, started);
                case INCREMENT_VARIABLE -> mutateInteger(snapshot, request, targetId, key, current, actor, true, started);
                case DECREMENT_VARIABLE -> mutateInteger(snapshot, request, targetId, key, current, actor, false, started);
                case TOGGLE_BOOLEAN -> mutateToggle(snapshot, request, targetId, key, current, actor, started);
                case CLEAR_VARIABLE -> mutateClear(snapshot, request, targetId, key, current, started);
            };
        }
    }

    private StateVariableMutationResult mutateSet(
            StateVariableSnapshot snapshot,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            String actor,
            long started
    ) {
        if (current == null && !request.createIfMissing()) {
            return failed("missing_variable", "状态变量不存在，且未允许自动创建。", request, targetId, key, null, List.of("请先创建变量，或开启 createIfMissing。"), started);
        }
        if (current != null && current.type() != request.valueType()) {
            return failed("type_mismatch", "状态变量类型不匹配，不能写入不同类型的值。", request, targetId, key, current, List.of("当前类型：" + current.type().displayName() + "，动作类型：" + request.valueType().displayName()), started);
        }
        StateVariableRecord next = nextRecord(request, targetId, key, request.valueType(), request.value(), current, actor);
        if (current != null && current.sameEditableValue(next)) {
            return StateVariableMutationResult.success("unchanged", "状态变量没有变化。", request, targetId, key, current, current, false, elapsed(started));
        }
        return save(snapshot.with(next), request, targetId, key, current, next, current == null ? "created" : "updated", current == null ? "状态变量已创建。" : "状态变量已更新。", started);
    }

    private StateVariableMutationResult mutateInteger(
            StateVariableSnapshot snapshot,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            String actor,
            boolean increment,
            long started
    ) {
        long base;
        if (current == null) {
            if (!request.createIfMissing()) {
                return failed("missing_variable", "状态变量不存在，且未允许自动创建。", request, targetId, key, null, List.of("请先创建 INTEGER 变量，或开启 createIfMissing。"), started);
            }
            base = request.initialValue().isBlank() ? 0L : Long.parseLong(request.initialValue());
        } else if (current.type() != StateVariableType.INTEGER) {
            return failed("type_mismatch", "状态变量类型不匹配，增加 / 减少只能用于 INTEGER。", request, targetId, key, current, List.of("当前类型：" + current.type().displayName()), started);
        } else {
            base = Long.parseLong(current.value());
        }

        long nextValue;
        try {
            nextValue = increment ? Math.addExact(base, request.delta()) : Math.subtractExact(base, request.delta());
        } catch (ArithmeticException exception) {
            return failed("integer_overflow", "整数状态变量计算溢出，已拒绝写入。", request, targetId, key, current, List.of("base=" + base + ", delta=" + request.delta()), started);
        }
        StateVariableRecord next = nextRecord(request, targetId, key, StateVariableType.INTEGER, Long.toString(nextValue), current, actor);
        return save(snapshot.with(next), request, targetId, key, current, next, current == null ? "created" : "updated", current == null ? "状态变量已创建并完成整数计算。" : "状态变量整数已更新。", started);
    }

    private StateVariableMutationResult mutateToggle(
            StateVariableSnapshot snapshot,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            String actor,
            long started
    ) {
        boolean base;
        if (current == null) {
            if (!request.createIfMissing()) {
                return failed("missing_variable", "状态变量不存在，且未允许自动创建。", request, targetId, key, null, List.of("请先创建 BOOLEAN 变量，或开启 createIfMissing。"), started);
            }
            base = !request.initialValue().isBlank() && Boolean.parseBoolean(request.initialValue());
        } else if (current.type() != StateVariableType.BOOLEAN) {
            return failed("type_mismatch", "状态变量类型不匹配，切换只能用于 BOOLEAN。", request, targetId, key, current, List.of("当前类型：" + current.type().displayName()), started);
        } else {
            base = Boolean.parseBoolean(current.value());
        }
        StateVariableRecord next = nextRecord(request, targetId, key, StateVariableType.BOOLEAN, Boolean.toString(!base), current, actor);
        return save(snapshot.with(next), request, targetId, key, current, next, current == null ? "created" : "updated", current == null ? "状态变量已创建并完成布尔切换。" : "状态变量布尔值已切换。", started);
    }

    private StateVariableMutationResult mutateClear(
            StateVariableSnapshot snapshot,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            long started
    ) {
        if (current == null) {
            return StateVariableMutationResult.success("missing_noop", "状态变量不存在，清除动作无变化。", request, targetId, key, null, null, false, elapsed(started));
        }
        boolean saved = StateVariableStore.saveSnapshot(path, snapshot.without(request.scope(), targetId, key));
        if (!saved) {
            return failed("write_failed", "状态变量清除失败。", request, targetId, key, current, List.of("无法写入状态变量存储文件。"), started);
        }
        return StateVariableMutationResult.success("cleared", "状态变量已清除。", request, targetId, key, current, null, true, elapsed(started));
    }

    private StateVariableMutationResult save(
            StateVariableSnapshot snapshot,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            StateVariableRecord next,
            String code,
            String message,
            long started
    ) {
        boolean saved = StateVariableStore.saveSnapshot(path, snapshot);
        if (!saved) {
            return failed("write_failed", "状态变量保存失败。", request, targetId, key, current, List.of("无法写入状态变量存储文件。"), started);
        }
        return StateVariableMutationResult.success(code, message, request, targetId, key, current, next, true, elapsed(started));
    }

    private static StateVariableRecord nextRecord(
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableType type,
            String value,
            StateVariableRecord current,
            String actor
    ) {
        return StateVariableRecord.create(
                request.scope(),
                targetId,
                key,
                type,
                value,
                current == null ? "" : current.displayName(),
                current == null ? "" : current.note(),
                Instant.now().toEpochMilli(),
                actor == null ? "" : actor,
                current == null ? 1L : current.version() + 1L
        );
    }

    private static StateVariableMutationResult failed(
            String code,
            String message,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord current,
            List<String> errors,
            long started
    ) {
        return StateVariableMutationResult.failed(code, message, request, targetId, key, current, errors, elapsed(started));
    }

    private static long elapsed(long started) {
        return Math.max(0L, System.nanoTime() - started);
    }

    private Object lock() {
        return LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new Object());
    }
}
