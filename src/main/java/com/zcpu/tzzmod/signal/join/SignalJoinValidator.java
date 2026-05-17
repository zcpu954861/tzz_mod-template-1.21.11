package com.zcpu.tzzmod.signal.join;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class SignalJoinValidator {
    private static final long MAX_TIMEOUT_TICKS = 20L * 60L * 60L * 24L;
    private static final long MAX_COOLDOWN_TICKS = 20L * 60L * 60L * 24L;
    private static final int MAX_COUNT_THRESHOLD = 100_000;

    private SignalJoinValidator() {
    }

    public static List<SignalJoinValidationIssue> validate(SignalJoinDefinition raw, boolean creating) {
        SignalJoinDefinition source = raw == null ? new SignalJoinDefinition() : raw;
        SignalJoinDefinition join = source.normalized();
        List<SignalJoinValidationIssue> issues = new ArrayList<>();
        if (join.id.isBlank()) {
            issues.add(issue("id", "signal_join_id_required", "Signal Join ID 不能为空，且只能包含小写字母、数字、_、-、.、:。", raw == null ? "" : raw.id));
        }
        if (join.displayName.isBlank()) {
            issues.add(issue("displayName", "signal_join_name_required", "Signal Join 名称不能为空。", raw == null ? "" : raw.displayName));
        }
        if (source.mode == null) {
            issues.add(issue("mode", "signal_join_mode_invalid", "模式必须是 ALL、ANY_N 或 COUNT。", ""));
        }
        if (source.scopeMode == null) {
            issues.add(issue("scopeMode", "signal_join_scope_invalid", "作用域必须选择 GLOBAL 或 PLAYER。", ""));
        }
        if (source.resetPolicy == null) {
            issues.add(issue("resetPolicy", "signal_join_reset_policy_invalid", "重置策略必须是 RESET_AFTER_EMIT 或 LATCH_UNTIL_MANUAL_RESET。", ""));
        }
        validateInputs(join, issues);
        if (!SignalChannel.isValid(join.outputChannel)) {
            issues.add(issue("outputChannel", "signal_join_output_channel_invalid", "输出频道不能为空，且只能包含小写字母、数字、_、-、.、:。", raw == null ? "" : raw.outputChannel));
        }
        if (join.inputChannelNames().contains(join.outputChannel)) {
            issues.add(issue("outputChannel", "signal_join_output_equals_input", "输出频道不能与任一输入频道相同，避免自循环。", join.outputChannel));
        }
        int inputCount = join.inputChannelNames().size();
        SignalJoinMode mode = source.mode == null ? join.mode : source.mode;
        int rawThreshold = source.threshold;
        if (mode == SignalJoinMode.ALL && inputCount < 2) {
            issues.add(issue("inputChannels", "signal_join_all_requires_two_inputs", "ALL 模式至少需要 2 个不同输入频道。", String.valueOf(inputCount)));
        }
        if (mode == SignalJoinMode.ANY_N) {
            if (inputCount < 2) {
                issues.add(issue("inputChannels", "signal_join_any_n_requires_two_inputs", "ANY_N 模式至少需要 2 个不同输入频道。", String.valueOf(inputCount)));
            }
            if (rawThreshold <= 0 || rawThreshold > inputCount) {
                issues.add(issue("threshold", "signal_join_any_n_threshold_invalid", "ANY_N 阈值必须在 1 到输入频道数量之间。", String.valueOf(rawThreshold)));
            }
        }
        if (mode == SignalJoinMode.COUNT && (rawThreshold <= 0 || rawThreshold > MAX_COUNT_THRESHOLD)) {
            issues.add(issue("threshold", "signal_join_count_threshold_invalid", "COUNT 阈值必须在 1 到 100000 之间。", String.valueOf(rawThreshold)));
        }
        if (source.timeoutTicks < 0 || source.timeoutTicks > MAX_TIMEOUT_TICKS) {
            issues.add(issue("timeoutTicks", "signal_join_timeout_invalid", "超时 tick 必须在 0 到 1728000 之间，0 表示不启用超时。", String.valueOf(source.timeoutTicks)));
        }
        if (source.cooldownTicks < 0 || source.cooldownTicks > MAX_COOLDOWN_TICKS) {
            issues.add(issue("cooldownTicks", "signal_join_cooldown_invalid", "输出冷却 tick 必须在 0 到 1728000 之间，0 表示不启用冷却。", String.valueOf(source.cooldownTicks)));
        }
        return List.copyOf(issues);
    }

    private static void validateInputs(SignalJoinDefinition join, List<SignalJoinValidationIssue> issues) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (SignalJoinInputDefinition input : join.inputChannels) {
            String field = "inputChannels[" + index + "].channel";
            if (!SignalChannel.isValid(input.channel)) {
                issues.add(issue(field, "signal_join_input_channel_invalid", "输入频道不能为空，且只能包含小写字母、数字、_、-、.、:。", input.channel));
            } else if (!seen.add(input.channel)) {
                issues.add(issue(field, "signal_join_input_channel_duplicate", "输入频道不能重复：" + input.channel, input.channel));
            }
            index++;
        }
        if (seen.isEmpty()) {
            issues.add(issue("inputChannels", "signal_join_inputs_required", "至少需要配置 1 个输入频道。", ""));
        }
    }

    private static SignalJoinValidationIssue issue(String field, String code, String message, String rejectedValue) {
        return new SignalJoinValidationIssue(field, code, message, rejectedValue);
    }
}
