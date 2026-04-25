package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class SignalDoctor {
    private static final int HIGH_COOLDOWN_TICKS = 6_000;

    private SignalDoctor() {
    }

    public static SignalDoctorReport inspect(MinecraftServer server) {
        if (server == null) {
            return new SignalDoctorReport(0, 0, 0, 0, 0, List.of());
        }

        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(server);
        List<SignalChannelSummary> summaries = SignalChannelInspector.getSummaries(server);
        List<SignalEventRecord> history = SignalEventHistory.snapshot();
        List<SignalDoctorIssue> issues = new ArrayList<>();

        inspectListeners(listeners, issues);
        inspectHistoryWithoutListeners(history, listeners, issues);
        inspectDisabledChannels(summaries, issues);
        inspectRawListenerData(server, issues);

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

            String value = action.value() == null ? "" : action.value().trim();
            if (value.isEmpty()) {
                issues.add(error("监听器" + listenerName + "的第 " + actionNumber + " 个动作内容为空。"));
                continue;
            }

            if (action.type() == ActionType.SIGNAL) {
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

    private static void inspectHistoryWithoutListeners(
            List<SignalEventRecord> history,
            List<SignalListenerData> listeners,
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
