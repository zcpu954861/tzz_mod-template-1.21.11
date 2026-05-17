package com.zcpu.tzzmod.condition.state;

public record StateVariableMutationRequest(
        StateVariableMutationOperation operation,
        StateVariableScope scope,
        StateVariableTargetMode targetMode,
        String targetId,
        String contextPlayerId,
        String key,
        StateVariableType valueType,
        String value,
        long delta,
        boolean createIfMissing,
        String initialValue
) {
    public StateVariableMutationRequest {
        targetId = targetId == null ? "" : targetId.trim();
        contextPlayerId = contextPlayerId == null ? "" : contextPlayerId.trim();
        key = StateVariableValidation.normalizeKey(key);
        value = value == null ? "" : value.trim();
        initialValue = initialValue == null ? "" : initialValue.trim();
    }
}
