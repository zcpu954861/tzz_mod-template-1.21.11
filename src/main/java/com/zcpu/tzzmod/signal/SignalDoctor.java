package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.debug.DeviceDiagnostic;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssueText;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.signal.device.debug.VirtualBlockDeviceDiagnosticService;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinMode;
import com.zcpu.tzzmod.signal.join.SignalJoinScopeMode;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.join.SignalJoinValidationIssue;
import com.zcpu.tzzmod.signal.join.SignalJoinValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class SignalDoctor {
    private static final int HIGH_COOLDOWN_TICKS = 6_000;
    private static final String SIGNAL_JOIN_SELF_OUTPUT_CODE = "signal_join_output_equals_input";

    private SignalDoctor() {
    }

    public static SignalDoctorReport inspect(MinecraftServer server) {
        if (server == null) {
            return new SignalDoctorReport(0, 0, 0, 0, 0, List.of());
        }

        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(server);
        List<SignalJoinDefinition> joins = SignalJoinStore.getSnapshot(server);
        List<SignalChannelSummary> summaries = SignalChannelInspector.getSummaries(server);
        List<SignalEventRecord> history = SignalEventHistory.snapshot();
        List<SignalDoctorIssue> issues = new ArrayList<>();

        inspectListeners(listeners, issues);
        inspectSignalJoins(joins, issues);
        inspectHistoryWithoutListeners(history, listeners, joins, issues);
        inspectDisabledChannels(summaries, issues);
        inspectRawListenerData(server, issues);
        inspectDevices(server, issues);

        int enabledCount = 0;
        for (SignalListenerData listener : listeners) {
            if (listener.enabled()) {
                enabledCount++;
            }
        }

        return new SignalDoctorReport(
                listeners.size(),
                enabledCount,
                listeners.size() - enabledCount,
                summaries.size(),
                history.size(),
                issues
        );
    }

    private static void inspectListeners(List<SignalListenerData> listeners, List<SignalDoctorIssue> issues) {
        for (SignalListenerData listener : listeners) {
            if (listener == null) {
                continue;
            }

            List<ActionConfig> actions = listener.actions() == null ? List.of() : listener.actions();
            if (listener.enabled() && actions.isEmpty()) {
                issues.add(warning("监听器" + quotedName(listener) + "已启用，但没有配置动作。"));
            }

            if (listener.cooldownTicks() >= HIGH_COOLDOWN_TICKS) {
                issues.add(info("监听器" + quotedName(listener) + "的冷却时间为 "
                        + listener.cooldownTicks() + " tick，约 " + formatMinutes(listener.cooldownTicks()) + "。"));
            }

            inspectActions(listener, actions, issues);
        }
    }

    private static void inspectActions(SignalListenerData listener, List<ActionConfig> actions, List<SignalDoctorIssue> issues) {
        String listenerName = quotedName(listener);
        String listenerChannel = SignalChannel.normalize(listener.channel());
        for (int i = 0; i < actions.size(); i++) {
            ActionConfig action = actions.get(i);
            int actionNumber = i + 1;
            if (action == null) {
                issues.add(error("监听器" + listenerName + "的第 " + actionNumber + " 个动作配置为空。"));
                continue;
            }
            if (action.type() == null) {
                issues.add(error("监听器" + listenerName + "的第 " + actionNumber + " 个动作类型为空。"));
            }
            if (!action.enabled()) {
                issues.add(warning("监听器" + listenerName + "的第 " + actionNumber + " 个动作未启用。"));
            }

            String emptyMessage = actionContentIssue(action);
            if (!emptyMessage.isBlank()) {
                issues.add(error("监听器" + listenerName + "的第 " + actionNumber + " 个" + emptyMessage));
                continue;
            }

            if (action.type() == ActionType.SIGNAL) {
                String value = action.value() == null ? "" : action.value().trim();
                String actionChannel = SignalChannel.normalize(value);
                if (!SignalChannel.isValid(actionChannel)) {
                    issues.add(error("监听器" + listenerName + "的第 " + actionNumber
                            + " 个 signal 动作频道无效：" + value));
                }
                if (actionChannel.equals(listenerChannel)) {
                    issues.add(warning("监听器" + listenerName + "存在直接 signal 自递归风险："
                            + listenerChannel + " -> " + actionChannel));
                }
            }
        }
    }

    private static String actionContentIssue(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "";
        }
        if (action.type() == ActionType.TIMER_START) {
            return action.timerId() == null || action.timerId().trim().isEmpty() ? " timer_start 缺少 timerId。" : "";
        }
        if (action.type() == ActionType.TIMER_CANCEL) {
            return action.timerId() == null || action.timerId().trim().isEmpty() ? " timer_cancel 缺少 timerId。" : "";
        }
        if (action.type() == ActionType.STATE_VARIABLE) {
            return action.stateKey() == null || action.stateKey().trim().isEmpty() ? " state action 缺少 key。" : "";
        }
        String value = action.value() == null ? "" : action.value().trim();
        if (!value.isEmpty()) {
            return "";
        }
        if (action.type() == ActionType.COMMAND) {
            return " command action 缺少 command。";
        }
        if (action.type() == ActionType.MESSAGE) {
            return " message action 缺少 message。";
        }
        if (action.type() == ActionType.SOUND) {
            return " sound action 缺少 sound。";
        }
        if (action.type() == ActionType.SIGNAL) {
            return " signal action 缺少 channel。";
        }
        return "动作内容为空。";
    }

    private static void inspectHistoryWithoutListeners(
            List<SignalEventRecord> history,
            List<SignalListenerData> listeners,
            List<SignalJoinDefinition> joins,
            List<SignalDoctorIssue> issues
    ) {
        Set<String> listenerChannels = new HashSet<>();
        for (SignalListenerData listener : listeners) {
            if (listener != null) {
                String channel = SignalChannel.normalize(listener.channel());
                if (!channel.isBlank()) {
                    listenerChannels.add(channel);
                }
            }
        }
        for (SignalJoinDefinition raw : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition join = raw.normalized();
            if (join.enabled) {
                listenerChannels.addAll(join.inputChannelNames());
            }
        }

        Set<String> reportedChannels = new HashSet<>();
        for (SignalEventRecord record : history) {
            if (record == null) {
                continue;
            }
            String channel = SignalChannel.normalize(record.channel());
            if (channel.isBlank() || listenerChannels.contains(channel) || !reportedChannels.add(channel)) {
                continue;
            }
            issues.add(warning("频道 " + channel + " 最近被触发，但没有监听器。"));
        }
    }

    private static void inspectSignalJoins(List<SignalJoinDefinition> joins, List<SignalDoctorIssue> issues) {
        List<SignalJoinDefinition> source = joins == null ? List.of() : joins;
        for (SignalJoinDefinition raw : source) {
            SignalJoinDefinition join = raw == null ? null : raw.normalized();
            if (join == null) {
                continue;
            }
            for (SignalJoinValidationIssue issue : SignalJoinValidator.validate(join, false)) {
                issues.add(error("Signal Join“" + joinName(join) + "”配置无效（" + issue.code() + "）：" + issue.message()));
            }
            if (!join.enabled) {
                issues.add(info("Signal Join“" + joinName(join) + "”当前已禁用，不会累计输入或输出信号。"));
            }
            if (join.scopeMode == SignalJoinScopeMode.PLAYER) {
                issues.add(warning("Signal Join“" + joinName(join) + "”使用 PLAYER scope；没有玩家上下文的 signal 会被忽略。"));
            }
            if (join.mode == SignalJoinMode.COUNT && join.threshold > 1000) {
                issues.add(info("Signal Join“" + joinName(join) + "”的 COUNT threshold 较高：" + join.threshold + "。"));
            }
            if (join.timeoutTicks > 0 && join.timeoutTicks < 20) {
                issues.add(warning("Signal Join“" + joinName(join) + "”的 timeoutTicks 小于 20 tick，可能导致 pending state 过快清理。"));
            }
        }
        inspectJoinCycles(source, issues);
    }

    private static void inspectJoinCycles(List<SignalJoinDefinition> joins, List<SignalDoctorIssue> issues) {
        Set<String> reported = new HashSet<>();
        for (SignalJoinDefinition leftRaw : joins) {
            SignalJoinDefinition left = leftRaw == null ? null : leftRaw.normalized();
            if (left == null || !left.enabled || left.outputChannel.isBlank()) {
                continue;
            }
            for (SignalJoinDefinition rightRaw : joins) {
                SignalJoinDefinition right = rightRaw == null ? null : rightRaw.normalized();
                if (right == null || !right.enabled || left.id.equals(right.id)) {
                    continue;
                }
                if (right.inputChannelNames().contains(left.outputChannel) && left.inputChannelNames().contains(right.outputChannel)) {
                    String key = left.id.compareTo(right.id) < 0 ? left.id + "|" + right.id : right.id + "|" + left.id;
                    if (reported.add(key)) {
                        issues.add(warning("Signal Join“" + joinName(left) + "”与“" + joinName(right) + "”存在互相输出 / 输入的循环风险。"));
                    }
                }
            }
        }
    }

    private static void inspectDisabledChannels(List<SignalChannelSummary> summaries, List<SignalDoctorIssue> issues) {
        for (SignalChannelSummary summary : summaries) {
            if (summary.listenerCount() > 0 && summary.enabledListenerCount() == 0) {
                issues.add(warning("频道 " + summary.channel() + " 有监听器，但全部处于禁用状态。"));
            }
        }
    }

    private static void inspectRawListenerData(MinecraftServer server, List<SignalDoctorIssue> issues) {
        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("tzz_mod")
                .resolve("signal_listeners.json");
        SignalListenerStore.DataFile dataFile = JsonStoreSupport.readOrDefault(
                path,
                SignalListenerStore.DataFile.class,
                SignalListenerStore.DataFile::new,
                "signal listeners doctor"
        );
        if (dataFile.listeners == null) {
            return;
        }

        Set<String> reportedListenerIds = new HashSet<>();
        for (SignalListenerData listener : dataFile.listeners) {
            if (listener == null) {
                continue;
            }
            String channel = SignalChannel.normalize(listener.channel());
            if (!SignalChannel.isValid(channel) && reportedListenerIds.add(listener.id())) {
                issues.add(error("监听器" + quotedName(listener) + "的频道名称无效：" + safeText(listener.channel())));
            }
        }
    }

    private static void inspectDevices(MinecraftServer server, List<SignalDoctorIssue> issues) {
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        int disabledCount = 0;
        for (SignalDeviceData device : devices) {
            if (!device.enabled()) {
                disabledCount++;
            }
            if (device.channel().isBlank()) {
                issues.add(warning("设备“" + SignalDeviceStore.displayName(device) + "”未设置主频道。"));
            }
            if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
                DeviceDiagnostic diagnostic = VirtualBlockDeviceDiagnosticService.diagnose(
                        server,
                        device,
                        server.getOverworld() == null ? 0L : server.getOverworld().getTime()
                );
                for (DiagnosticIssue issue : diagnostic.issues()) {
                    if (issue.severity() == DiagnosticSeverity.INFO) {
                        continue;
                    }
                    issues.add(toDoctorIssue(issue));
                }
            }
        }
        if (disabledCount > 0) {
            issues.add(info("当前登记表中有 " + disabledCount + " 个 disabled signal device。"));
        }
    }

    private static SignalDoctorIssue toDoctorIssue(DiagnosticIssue issue) {
        SignalDoctorIssue.Severity severity = switch (issue.severity()) {
            case ERROR -> SignalDoctorIssue.Severity.ERROR;
            case WARNING -> SignalDoctorIssue.Severity.WARNING;
            case INFO -> SignalDoctorIssue.Severity.INFO;
        };
        return new SignalDoctorIssue(severity, DiagnosticIssueText.headline(issue), DiagnosticIssueText.doctorDetail(issue));
    }

    private static SignalDoctorIssue error(String title) {
        return new SignalDoctorIssue(SignalDoctorIssue.Severity.ERROR, title, "");
    }

    private static SignalDoctorIssue warning(String title) {
        return new SignalDoctorIssue(SignalDoctorIssue.Severity.WARNING, title, "");
    }

    private static SignalDoctorIssue info(String title) {
        return new SignalDoctorIssue(SignalDoctorIssue.Severity.INFO, title, "");
    }

    private static String quotedName(SignalListenerData listener) {
        String name = listener == null ? "" : listener.name();
        return "“" + (name == null || name.isBlank() ? "未命名监听器" : name) + "”";
    }

    private static String joinName(SignalJoinDefinition join) {
        return join == null || join.displayName.isBlank() ? (join == null ? "unknown" : join.id) : join.displayName;
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "空" : value;
    }

    private static String formatMinutes(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes <= 0) {
            return seconds + " 秒";
        }
        if (remainingSeconds == 0) {
            return minutes + " 分钟";
        }
        return minutes + " 分 " + remainingSeconds + " 秒";
    }
}
