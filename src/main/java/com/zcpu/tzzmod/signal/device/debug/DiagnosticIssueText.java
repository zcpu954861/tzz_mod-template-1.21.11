package com.zcpu.tzzmod.signal.device.debug;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticIssueText {
    private static final int LINE_LIMIT = 30;

    private DiagnosticIssueText() {
    }

    public static String headline(DiagnosticIssue issue) {
        if (issue == null) {
            return "[信息] 诊断信息";
        }
        return "[" + severityLabel(issue.severity()) + "] " + normalizeVisibleText(issue.title());
    }

    public static List<String> detailLines(DiagnosticIssue issue) {
        List<String> lines = new ArrayList<>();
        if (issue == null) {
            return lines;
        }

        if (!issue.deviceName().isBlank()) {
            lines.add("设备：" + normalizeVisibleText(issue.deviceName()));
        }
        if (!issue.pos().isBlank()) {
            lines.add("位置：" + normalizeVisibleText(issue.pos()));
        }
        if (!issue.channel().isBlank() && !hasField(issue.message(), "频道")) {
            lines.add("频道：" + normalizeVisibleText(issue.channel()));
        }
        addMessageLines(lines, issue.message());
        if (!issue.suggestion().isBlank()) {
            addWrappedField(lines, "建议", issue.suggestion());
        }
        if (!issue.relatedCommand().isBlank()) {
            addWrappedField(lines, "命令", issue.relatedCommand());
        }
        if (!issue.code().isBlank()) {
            lines.add("代码：" + issue.code());
        }
        return List.copyOf(lines);
    }

    public static String doctorDetail(DiagnosticIssue issue) {
        return String.join("\n", detailLines(issue));
    }

    public static String severityLabel(DiagnosticSeverity severity) {
        return switch (severity == null ? DiagnosticSeverity.INFO : severity) {
            case ERROR -> "错误";
            case WARNING -> "警告";
            case INFO -> "信息";
        };
    }

    private static void addMessageLines(List<String> lines, String text) {
        String normalized = normalizeVisibleText(text);
        if (normalized.isBlank()) {
            return;
        }
        for (String line : normalized.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.startsWith("当前匹配数量：")) {
                lines.add("当前：" + trimmed.substring("当前匹配数量：".length()).trim());
            } else if (trimmed.startsWith("要求数量：")) {
                lines.add("要求：" + trimmed.substring("要求数量：".length()).trim());
            } else if (trimmed.startsWith("说明：")) {
                addWrappedField(lines, "说明", trimmed.substring("说明：".length()));
            } else if (trimmed.startsWith("建议：")) {
                addWrappedField(lines, "建议", trimmed.substring("建议：".length()));
            } else if (trimmed.contains("：")) {
                lines.add(trimmed);
            } else {
                addWrappedField(lines, "说明", trimmed);
            }
        }
    }

    private static boolean hasField(String text, String label) {
        String normalized = normalizeVisibleText(text);
        return normalized.startsWith(label + "：") || normalized.contains("\n" + label + "：");
    }

    private static void addWrappedField(List<String> lines, String label, String rawValue) {
        List<String> wrapped = wrap(normalizeVisibleText(rawValue), LINE_LIMIT);
        if (wrapped.isEmpty()) {
            return;
        }
        lines.add(label + "：" + wrapped.get(0));
        for (int index = 1; index < wrapped.size(); index++) {
            lines.add("      " + wrapped.get(index));
        }
    }

    private static List<String> wrap(String raw, int limit) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        String remaining = value;
        while (remaining.length() > limit) {
            int splitIndex = findSplitIndex(remaining, limit);
            String segment = trimSplitPunctuation(remaining.substring(0, splitIndex).trim());
            if (!segment.isBlank()) {
                lines.add(segment);
            }
            remaining = remaining.substring(Math.min(splitIndex + 1, remaining.length())).trim();
        }
        if (!remaining.isBlank()) {
            lines.add(remaining);
        }
        return lines;
    }

    private static int findSplitIndex(String value, int limit) {
        int max = Math.min(value.length(), Math.max(1, limit));
        for (int index = max - 1; index >= Math.max(8, max / 2); index--) {
            char c = value.charAt(index);
            if (c == '。' || c == '；' || c == '，' || c == '、') {
                return index;
            }
        }
        return max;
    }

    private static String trimSplitPunctuation(String value) {
        if (value.endsWith("，") || value.endsWith("、") || value.endsWith("；")) {
            return value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private static String normalizeVisibleText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return "";
        }
        return text
                .replace("code=", "诊断代码：")
                .replace("matcher 模板", "物品匹配模板")
                .replace("matcher", "物品匹配模板")
                .replace("countMode", "数量模式")
                .replace("SignalBridge channel", "SignalBridge 频道")
                .replace("SignalDeviceData", "信号设备数据")
                .replace("interactionItem matcher", "单物品交互匹配")
                .replace("interactionItem", "交互物品匹配")
                .replace("itemSubmitEnabled=true", "多物品提交已启用")
                .replace("itemSubmit consume", "多物品提交消耗")
                .replace("itemSubmit", "多物品提交")
                .replace("successChannel", "成功频道")
                .replace("failChannel", "失败频道")
                .replace("interactChannel", "交互频道")
                .replace("consumeEnabled=true", "消耗已启用")
                .replace("consumeCount", "消耗数量")
                .replace("cooldown", "冷却")
                .replace("signal emit", "信号发出")
                .replace("signal 仍可 emit", "信号仍会发出")
                .replace("触发 signal", "触发信号")
                .replace("发出 signal", "发出信号")
                .replace("Signal 历史", "信号历史")
                .replace("listener、receiver 或 action_relay", "监听器、接收器或动作继电器")
                .replace("receiver、action_relay 或 listener", "监听器（listener）、接收器（signal_receiver）或动作继电器（action_relay）")
                .replace("emit", "发出")
                .replace("requirement", "提交条件")
                .replace("enabled", "启用")
                .replace("disabled", "禁用")
                .replace("channel", "频道")
                .replace("; ", "；")
                .replace(";", "；")
                .replace(", ", "，")
                .replace(" .", "。")
                .replace("..", "。");
    }
}
