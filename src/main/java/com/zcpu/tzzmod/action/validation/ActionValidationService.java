package com.zcpu.tzzmod.action.validation;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionOwnerCapability;
import com.zcpu.tzzmod.action.schema.ActionOwnerType;
import com.zcpu.tzzmod.condition.ConditionGroupIds;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableMutationValidation;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ActionValidationService {
    public static final int MAX_COMMAND_LENGTH = 512;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_SOUND_ID_LENGTH = 128;
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final int MAX_COOLDOWN_TICKS = 72_000;

    private static final String ACTION_TYPE_MESSAGE =
            "Action 类型必须是 command、signal、message、sound、state_variable、timer_start 或 timer_cancel。";

    private ActionValidationService() {
    }

    @FunctionalInterface
    public interface ConditionGroupValidator {
        List<ActionValidationError> validate(String field, String groupId, ConditionRuntimeTargetType targetType);
    }

    public static ActionValidationResult validate(ActionOwnerType ownerType, String prefix, ActionDraft draft) {
        return validate(ownerType, prefix, draft, null);
    }

    public static ActionValidationResult validate(
            ActionOwnerType ownerType,
            String prefix,
            ActionDraft draft,
            ConditionGroupValidator conditionGroupValidator
    ) {
        // 后端 validation 是 Phase 2 的权威入口：先 strict parse actionType，再检查 owner capability，
        // 最后才构造 ActionConfig。这样旧 ActionType.fromId / ActionConfig canonical fallback 只保留读取兼容，
        // 不会让新保存请求把未知 action 静默变成 command。
        String fieldPrefix = safe(prefix).isBlank() ? "action" : safe(prefix);
        List<ActionValidationError> errors = new ArrayList<>();
        if (draft == null) {
            errors.add(error(fieldPrefix, "required", "Action 配置不能为空。", ""));
            return new ActionValidationResult(null, null, errors);
        }

        Optional<ActionOwnerCapability> capability = ActionCapabilityMatrix.find(ownerType);
        if (capability.isEmpty()) {
            errors.add(error(fieldPrefix + ".ownerType", "unsupported_owner", "Action owner 不在 9.2 capability matrix 范围内。", ownerType == null ? "" : ownerType.id()));
            return new ActionValidationResult(null, null, errors);
        }

        ActionType type = parseActionTypeStrict(draft.type()).orElse(null);
        if (type == null) {
            errors.add(error(fieldPrefix + ".type", "invalid_type", ACTION_TYPE_MESSAGE, draft.type()));
            return new ActionValidationResult(null, null, errors);
        }
        if (!capability.get().supports(type)) {
            errors.add(error(fieldPrefix + ".type", "unsupported_action_type", "当前 owner 不支持该 action 类型。", type.id()));
            return new ActionValidationResult(type, null, errors);
        }

        Boolean enabled = parseBoolean(draft.enabled());
        Boolean requiresOp = parseBoolean(draft.requiresOp());
        Boolean notifyOps = parseBoolean(draft.notifyOps());
        Integer cooldownTicks = parseInteger(draft.cooldownTicks());
        if (enabled == null) {
            errors.add(error(fieldPrefix + ".enabled", "invalid_boolean", "启用状态必须是 boolean。", String.valueOf(draft.enabled())));
            enabled = Boolean.TRUE;
        }
        if (requiresOp == null) {
            errors.add(error(fieldPrefix + ".requiresOp", "invalid_boolean", "requiresOp 必须是 boolean。", String.valueOf(draft.requiresOp())));
            requiresOp = Boolean.FALSE;
        }
        if (notifyOps == null) {
            errors.add(error(fieldPrefix + ".notifyOps", "invalid_boolean", "notifyOps 必须是 boolean。", String.valueOf(draft.notifyOps())));
            notifyOps = Boolean.FALSE;
        }
        if (cooldownTicks == null || cooldownTicks < 0 || cooldownTicks > MAX_COOLDOWN_TICKS) {
            errors.add(error(fieldPrefix + ".cooldownTicks", "out_of_range", "Action 冷却字段必须是 0～72000 的整数。", String.valueOf(draft.cooldownTicks())));
            cooldownTicks = 0;
        }

        String value = normalizeValue(type, draft.value());
        validateValue(errors, fieldPrefix, type, value, draft);
        validateActionConditionGroup(errors, fieldPrefix, draft, capability.get(), conditionGroupValidator);

        ActionConfig action = actionFromDraft(draft, type, value, enabled, requiresOp, cooldownTicks, notifyOps);
        return new ActionValidationResult(type, action.normalized(), errors);
    }

    public static Optional<ActionType> parseActionTypeStrict(String raw) {
        String value = safe(raw).trim();
        if (value.isBlank()) {
            return Optional.empty();
        }
        for (ActionType type : ActionType.values()) {
            if (type.id().equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<ActionConfig> actionFromDraft(ActionDraft draft) {
        if (draft == null) {
            return Optional.empty();
        }
        ActionType type = parseActionTypeStrict(draft.type()).orElse(null);
        if (type == null) {
            return Optional.empty();
        }
        Boolean enabled = parseBoolean(draft.enabled());
        Boolean requiresOp = parseBoolean(draft.requiresOp());
        Boolean notifyOps = parseBoolean(draft.notifyOps());
        Integer cooldownTicks = parseInteger(draft.cooldownTicks());
        return Optional.of(actionFromDraft(
                draft,
                type,
                normalizeValue(type, draft.value()),
                enabled == null ? true : enabled,
                requiresOp != null && requiresOp,
                cooldownTicks == null ? 0 : Math.max(0, cooldownTicks),
                notifyOps != null && notifyOps
        ).normalized());
    }

    private static void validateValue(
            List<ActionValidationError> errors,
            String prefix,
            ActionType type,
            String value,
            ActionDraft draft
    ) {
        if (type == ActionType.STATE_VARIABLE) {
            validateStateAction(errors, prefix, draft);
            return;
        }
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            validateTimerAction(errors, prefix, draft, type);
            return;
        }
        String field = prefix + ".value";
        if (value.isBlank()) {
            errors.add(error(field, "empty", "Action 内容不能为空。", value));
            return;
        }
        if (containsControl(value)) {
            errors.add(error(field, "control_character", "Action 内容不能包含控制字符。", value));
            return;
        }
        switch (type) {
            case COMMAND -> {
                if (value.length() > MAX_COMMAND_LENGTH) {
                    errors.add(error(field, "too_long", "命令长度不能超过 " + MAX_COMMAND_LENGTH + " 个字符。", value));
                } else if (isBlockedServerManagementCommand(value)) {
                    errors.add(error(field, "server_management_command_forbidden", "该命令属于服务器管理高风险命令，不允许通过 WebAdmin action_relay 保存。", value));
                }
            }
            case SIGNAL -> {
                if (value.length() > MAX_CHANNEL_LENGTH) {
                    errors.add(error(field, "too_long", "频道长度不能超过 128 个字符。", value));
                } else if (!SignalChannel.isValid(value)) {
                    errors.add(error(field, "invalid_channel", "Signal action 的频道只能包含小写字母、数字、下划线、点、冒号和连字符。", value));
                }
            }
            case MESSAGE -> {
                if (value.length() > MAX_MESSAGE_LENGTH) {
                    errors.add(error(field, "too_long", "消息长度不能超过 " + MAX_MESSAGE_LENGTH + " 个字符。", value));
                }
            }
            case SOUND -> {
                if (value.length() > MAX_SOUND_ID_LENGTH) {
                    errors.add(error(field, "too_long", "音效 ID 长度不能超过 " + MAX_SOUND_ID_LENGTH + " 个字符。", value));
                } else if (!value.matches("[a-z0-9_.:-]+(/[a-z0-9_.:-]+)*")) {
                    errors.add(error(field, "invalid_sound_id", "音效 ID 应使用 minecraft:entity.example 这类小写资源 ID。", value));
                }
            }
            case STATE_VARIABLE, TIMER_START, TIMER_CANCEL -> {
            }
        }
    }

    private static void validateStateAction(List<ActionValidationError> errors, String prefix, ActionDraft draft) {
        Boolean createIfMissing = parseBoolean(draft.stateCreateIfMissing());
        if (createIfMissing == null) {
            errors.add(error(
                    prefix + ".stateCreateIfMissing",
                    "invalid_boolean",
                    "变量不存在时自动创建字段必须是 boolean。",
                    String.valueOf(draft.stateCreateIfMissing())
            ));
        }
        ActionConfig config = actionFromDraft(
                draft,
                ActionType.STATE_VARIABLE,
                "",
                true,
                false,
                0,
                false
        );
        StateVariableMutationRequest request = config.stateMutationRequest("");
        for (StateVariableMutationValidation.Issue issue : StateVariableMutationValidation.validate(request)) {
            errors.add(error(prefix + "." + issue.field(), issue.code(), issue.message(), issue.rejectedValue()));
        }
    }

    private static void validateTimerAction(
            List<ActionValidationError> errors,
            String prefix,
            ActionDraft draft,
            ActionType type
    ) {
        String timerId = TimerStore.normalizeId(draft.timerId());
        if (timerId.isBlank()) {
            errors.add(error(prefix + ".timerId", "timer_id_required", "Timer 动作必须选择 timerId。", draft.timerId()));
        }
        String targetMode = safe(draft.timerTargetMode());
        TimerTargetMode parsedTargetMode = TimerTargetMode.parse(targetMode);
        if (!targetMode.isBlank() && parsedTargetMode == null) {
            errors.add(error(prefix + ".timerTargetMode", "timer_target_mode_invalid", "Timer 目标模式必须是 global、context_player 或 explicit_target。", targetMode));
        }
        if (parsedTargetMode == TimerTargetMode.EXPLICIT_TARGET && safe(draft.timerTargetId()).isBlank()) {
            errors.add(error(prefix + ".timerTargetId", "timer_target_id_required", "Timer 指定玩家目标不能为空。", ""));
        }
        String policy = safe(draft.timerStartPolicyOverride());
        if (type == ActionType.TIMER_START && !policy.isBlank() && TimerStartPolicy.parse(policy) == null) {
            errors.add(error(prefix + ".timerStartPolicyOverride", "timer_start_policy_invalid", "Timer 启动策略覆盖必须是 RESTART、IGNORE_IF_RUNNING 或 FAIL_IF_RUNNING。", policy));
        }
        Long durationOverride = parseLongObject(draft.timerDurationOverrideTicks());
        if (durationOverride == null || durationOverride < 0 || durationOverride > TimerValidator.MAX_DURATION_TICKS) {
            errors.add(error(prefix + ".timerDurationOverrideTicks", "timer_duration_override_invalid", "Timer 时长覆盖必须是 0 到 1728000 的整数。", String.valueOf(draft.timerDurationOverrideTicks())));
        }
        String missingBehavior = safe(draft.timerMissingBehavior()).toLowerCase(Locale.ROOT);
        if (type == ActionType.TIMER_CANCEL
                && !missingBehavior.isBlank()
                && !"noop_success".equals(missingBehavior)
                && !"fail".equals(missingBehavior)
                && !"fail_if_missing".equals(missingBehavior)) {
            errors.add(error(prefix + ".timerMissingBehavior", "timer_missing_behavior_invalid", "Timer 缺失处理策略必须是 noop_success、fail 或旧值 fail_if_missing。", missingBehavior));
        }
    }

    private static void validateActionConditionGroup(
            List<ActionValidationError> errors,
            String prefix,
            ActionDraft draft,
            ActionOwnerCapability capability,
            ConditionGroupValidator conditionGroupValidator
    ) {
        String groupId = ConditionGroupIds.normalize(draft.conditionGroupId());
        if (groupId.isBlank() || conditionGroupValidator == null) {
            return;
        }
        List<ActionValidationError> conditionErrors = conditionGroupValidator.validate(
                prefix + ".conditionGroupId",
                groupId,
                capability.actionConditionTargetType()
        );
        if (conditionErrors != null) {
            errors.addAll(conditionErrors);
        }
    }

    private static ActionConfig actionFromDraft(
            ActionDraft draft,
            ActionType type,
            String value,
            boolean enabled,
            boolean requiresOp,
            int cooldownTicks,
            boolean notifyOps
    ) {
        String conditionGroupId = ConditionGroupIds.normalize(draft.conditionGroupId());
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            return new ActionConfig(
                    type,
                    "",
                    enabled,
                    false,
                    cooldownTicks,
                    false,
                    conditionGroupId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    0L,
                    false,
                    "",
                    draft.timerId(),
                    draft.timerTargetMode(),
                    draft.timerTargetId(),
                    draft.timerStartPolicyOverride(),
                    parseLong(draft.timerDurationOverrideTicks(), 0L),
                    draft.timerMissingBehavior()
            );
        }
        if (type != ActionType.STATE_VARIABLE) {
            return new ActionConfig(type, value, enabled, requiresOp, cooldownTicks, notifyOps, conditionGroupId);
        }
        return new ActionConfig(
                ActionType.STATE_VARIABLE,
                "",
                enabled,
                false,
                cooldownTicks,
                false,
                conditionGroupId,
                draft.stateOperation(),
                draft.stateScope(),
                draft.stateTargetMode(),
                draft.stateTargetId(),
                draft.stateKey(),
                draft.stateValueType(),
                draft.stateValue(),
                parseLong(draft.stateDelta(), 0L),
                Boolean.TRUE.equals(parseBoolean(draft.stateCreateIfMissing())),
                draft.stateInitialValue()
        );
    }

    private static String normalizeValue(ActionType type, String rawValue) {
        String value = safe(rawValue).trim();
        if (type == ActionType.COMMAND) {
            return ActionConfig.normalizeCommand(value);
        }
        if (type == ActionType.SIGNAL) {
            return SignalChannel.normalize(value);
        }
        if (type == ActionType.STATE_VARIABLE || type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            return "";
        }
        return value;
    }

    private static boolean isBlockedServerManagementCommand(String command) {
        List<String> tokens = commandTokens(command);
        if (tokens.isEmpty()) {
            return false;
        }
        if (isServerManagementRoot(commandRoot(tokens.getFirst()))) {
            return true;
        }
        if ("execute".equals(commandRoot(tokens.getFirst()))) {
            for (int index = 0; index < tokens.size() - 1; index++) {
                if ("run".equals(commandRoot(tokens.get(index)))
                        && isServerManagementRoot(commandRoot(tokens.get(index + 1)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isServerManagementRoot(String root) {
        return "ban".equals(root)
                || "ban-ip".equals(root)
                || "kick".equals(root)
                || "op".equals(root)
                || "deop".equals(root)
                || "reload".equals(root)
                || "save-off".equals(root)
                || "save-on".equals(root)
                || "stop".equals(root)
                || "whitelist".equals(root)
                || "pardon".equals(root)
                || "pardon-ip".equals(root);
    }

    private static String commandRoot(String token) {
        String value = safe(token).trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int namespace = value.indexOf(':');
        return namespace >= 0 && namespace + 1 < value.length() ? value.substring(namespace + 1) : value;
    }

    private static List<String> commandTokens(String command) {
        String normalized = ActionConfig.normalizeCommand(command);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < normalized.length(); index++) {
            char c = normalized.charAt(index);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                current.append(c);
                quoted = !quoted;
                continue;
            }
            if (!quoted && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(string.trim())) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return (int) doubleValue;
            }
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static long parseLong(Object value, long fallback) {
        Long parsed = parseLongObject(value);
        return parsed == null ? fallback : parsed;
    }

    private static Long parseLongObject(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE) {
                return number.longValue();
            }
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsControl(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static ActionValidationError error(String field, String code, String message, String rejectedValue) {
        return new ActionValidationError(field, code, message, rejectedValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
