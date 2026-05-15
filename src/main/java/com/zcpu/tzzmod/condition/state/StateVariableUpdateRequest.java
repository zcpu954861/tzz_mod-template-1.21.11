package com.zcpu.tzzmod.condition.state;

public record StateVariableUpdateRequest(
        StateVariableScope scope,
        String targetId,
        String key,
        StateVariableType type,
        String value,
        String displayName,
        String note,
        String expectedFingerprint
) {
}
