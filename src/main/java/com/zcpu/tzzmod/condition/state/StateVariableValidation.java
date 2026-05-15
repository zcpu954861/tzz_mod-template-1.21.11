package com.zcpu.tzzmod.condition.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StateVariableValidation {
    public static final int MAX_KEY_LENGTH = 96;
    public static final int MAX_VALUE_LENGTH = 512;
    public static final String GLOBAL_TARGET = "global";

    private StateVariableValidation() {
    }

    public record Issue(String code, String message) {
    }

    public static List<Issue> validateUpdate(StateVariableUpdateRequest request) {
        List<Issue> issues = new ArrayList<>();
        if (request == null) {
            issues.add(new Issue("missing_request", "状态变量写入请求不能为空。"));
            return issues;
        }
        validateScope(request.scope(), issues);
        StateVariableScope scope = request.scope() == null ? StateVariableScope.GLOBAL : request.scope();
        validateTarget(scope, request.targetId(), issues);
        validateKey(request.key(), issues);
        validateType(request.type(), issues);
        if (request.type() != null) {
            validateValue(request.type(), request.value(), issues);
        }
        return issues;
    }

    public static List<Issue> validateKeyOnly(StateVariableScope scope, String targetId, String key) {
        List<Issue> issues = new ArrayList<>();
        validateScope(scope, issues);
        validateTarget(scope == null ? StateVariableScope.GLOBAL : scope, targetId, issues);
        validateKey(key, issues);
        return issues;
    }

    public static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeTargetId(StateVariableScope scope, String raw) {
        if (scope == StateVariableScope.GLOBAL) {
            return GLOBAL_TARGET;
        }
        return raw == null ? "" : raw.trim();
    }

    public static String normalizeValue(StateVariableType type, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (type == StateVariableType.BOOLEAN) {
            return Boolean.toString(Boolean.parseBoolean(value));
        }
        if (type == StateVariableType.INTEGER) {
            return Long.toString(Long.parseLong(value));
        }
        return value;
    }

    public static void validateScope(StateVariableScope scope, List<Issue> issues) {
        if (scope == null) {
            issues.add(new Issue("invalid_scope", "状态变量作用域不合法。"));
        }
    }

    public static void validateType(StateVariableType type, List<Issue> issues) {
        if (type == null) {
            issues.add(new Issue("invalid_type", "状态变量类型不合法。"));
        }
    }

    public static void validateTarget(StateVariableScope scope, String targetId, List<Issue> issues) {
        if (scope == StateVariableScope.PLAYER && (targetId == null || targetId.isBlank())) {
            issues.add(new Issue("missing_player_target", "玩家作用域状态变量必须提供目标玩家 ID。"));
        }
        if (targetId != null && hasControlCharacter(targetId)) {
            issues.add(new Issue("invalid_target", "状态变量目标 ID 不能包含控制字符。"));
        }
    }

    public static void validateKey(String key, List<Issue> issues) {
        String normalized = normalizeKey(key);
        if (normalized.isBlank()) {
            issues.add(new Issue("missing_key", "状态变量键不能为空。"));
            return;
        }
        if (normalized.length() > MAX_KEY_LENGTH) {
            issues.add(new Issue("key_too_long", "状态变量键过长，最多 " + MAX_KEY_LENGTH + " 个字符。"));
        }
        if (hasControlCharacter(normalized)) {
            issues.add(new Issue("invalid_key", "状态变量键不能包含控制字符。"));
        }
        if (!normalized.matches("[a-z0-9_.:-]+")) {
            issues.add(new Issue("invalid_key", "状态变量键只能包含小写字母、数字、点、下划线、冒号或短横线。"));
        }
    }

    public static void validateValue(StateVariableType type, String raw, List<Issue> issues) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > MAX_VALUE_LENGTH) {
            issues.add(new Issue("value_too_long", "状态变量值过长，最多 " + MAX_VALUE_LENGTH + " 个字符。"));
            return;
        }
        if (type == StateVariableType.BOOLEAN) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (!normalized.equals("true") && !normalized.equals("false")) {
                issues.add(new Issue("invalid_boolean", "布尔状态变量只能写入 true 或 false。"));
            }
        } else if (type == StateVariableType.INTEGER) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException ex) {
                issues.add(new Issue("invalid_integer", "整数状态变量必须写入整数。"));
            }
        } else if (type == StateVariableType.STRING && hasControlCharacter(value)) {
            issues.add(new Issue("invalid_string", "文本状态变量不能包含控制字符。"));
        }
    }

    private static boolean hasControlCharacter(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
