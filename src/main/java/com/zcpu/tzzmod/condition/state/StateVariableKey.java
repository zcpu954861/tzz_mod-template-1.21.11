package com.zcpu.tzzmod.condition.state;

public record StateVariableKey(
        StateVariableScope scope,
        String targetId,
        String key
) {
    public StateVariableKey {
        scope = scope == null ? StateVariableScope.GLOBAL : scope;
        targetId = StateVariableValidation.normalizeTargetId(scope, targetId);
        key = StateVariableValidation.normalizeKey(key);
    }

    public String stableId() {
        return scope.name().toLowerCase() + "|" + lengthPrefixed(targetId) + "|" + lengthPrefixed(key);
    }

    public String displayPath() {
        if (scope == StateVariableScope.GLOBAL) {
            return "global." + key;
        }
        return "player[" + targetId + "]." + key;
    }

    private static String lengthPrefixed(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue.length() + ":" + safeValue;
    }
}
