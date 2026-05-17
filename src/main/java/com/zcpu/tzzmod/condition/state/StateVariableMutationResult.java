package com.zcpu.tzzmod.condition.state;

import java.util.List;

public record StateVariableMutationResult(
        boolean success,
        boolean changed,
        String code,
        String message,
        StateVariableMutationOperation operation,
        StateVariableScope scope,
        String targetId,
        String key,
        StateVariableRecord oldRecord,
        StateVariableRecord newRecord,
        List<String> validationErrors,
        long durationNanos
) {
    public StateVariableMutationResult {
        code = code == null ? "" : code;
        message = message == null ? "" : message;
        targetId = targetId == null ? "" : targetId;
        key = key == null ? "" : key;
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
        durationNanos = Math.max(0L, durationNanos);
    }

    public String oldValue() {
        return oldRecord == null ? "" : oldRecord.value();
    }

    public String newValue() {
        return newRecord == null ? "" : newRecord.value();
    }

    public static StateVariableMutationResult success(
            String code,
            String message,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord oldRecord,
            StateVariableRecord newRecord,
            boolean changed,
            long durationNanos
    ) {
        return new StateVariableMutationResult(
                true,
                changed,
                code,
                message,
                request == null ? null : request.operation(),
                request == null ? null : request.scope(),
                targetId,
                key,
                oldRecord,
                newRecord,
                List.of(),
                durationNanos
        );
    }

    public static StateVariableMutationResult failed(
            String code,
            String message,
            StateVariableMutationRequest request,
            String targetId,
            String key,
            StateVariableRecord oldRecord,
            List<String> validationErrors,
            long durationNanos
    ) {
        return new StateVariableMutationResult(
                false,
                false,
                code,
                message,
                request == null ? null : request.operation(),
                request == null ? null : request.scope(),
                targetId,
                key,
                oldRecord,
                oldRecord,
                validationErrors,
                durationNanos
        );
    }
}
