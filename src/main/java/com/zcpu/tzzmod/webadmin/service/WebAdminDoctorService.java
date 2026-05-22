package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalDoctor;
import com.zcpu.tzzmod.signal.SignalDoctorIssue;
import com.zcpu.tzzmod.signal.SignalDoctorReport;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.webadmin.WebAdminTemplateStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminDoctorService {
    private final WebAdminConditionRuntimeDoctorService conditionRuntimeDoctorService = new WebAdminConditionRuntimeDoctorService();
    private final WebAdminTimerDoctorService timerDoctorService = new WebAdminTimerDoctorService();

    public WebAdminDtos.DoctorReportDto report(MinecraftServer server) {
        SignalDoctorReport report = SignalDoctor.inspect(server);
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        int errorCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        Set<String> affectedChannels = new HashSet<>();

        for (int index = 0; index < report.issues().size(); index++) {
            SignalDoctorIssue issue = report.issues().get(index);
            if (issue == null) {
                continue;
            }
            switch (issue.severity()) {
                case ERROR -> errorCount++;
                case WARNING -> warningCount++;
                case INFO -> infoCount++;
            }
            WebAdminDtos.DoctorIssueDto dto = fromDoctorIssue(issue, index, server);
            if (!dto.channel().isBlank()) {
                affectedChannels.add(dto.channel());
            }
            issues.add(dto);
        }

        for (WebAdminDtos.DoctorIssueDto issue : conditionRuntimeDoctorService.inspect(server)) {
            if (issue == null) {
                continue;
            }
            switch (issue.severity()) {
                case "ERROR" -> errorCount++;
                case "WARNING" -> warningCount++;
                default -> infoCount++;
            }
            issues.add(issue);
        }

        for (WebAdminDtos.DoctorIssueDto issue : timerDoctorService.inspect(server)) {
            if (issue == null) {
                continue;
            }
            switch (issue.severity()) {
                case "ERROR" -> errorCount++;
                case "WARNING" -> warningCount++;
                default -> infoCount++;
            }
            issues.add(issue);
        }

        for (WebAdminDtos.DoctorIssueDto issue : snapshotDiagnostics(server)) {
            switch (issue.severity()) {
                case "ERROR" -> errorCount++;
                case "WARNING" -> warningCount++;
                default -> infoCount++;
            }
            issues.add(issue);
        }

        for (WebAdminDtos.DoctorIssueDto issue : templateDiagnostics(server)) {
            switch (issue.severity()) {
                case "ERROR" -> errorCount++;
                case "WARNING" -> warningCount++;
                default -> infoCount++;
            }
            issues.add(issue);
        }

        WebAdminDtos.DoctorSummaryDto summary = new WebAdminDtos.DoctorSummaryDto(
                errorCount,
                warningCount,
                infoCount,
                0,
                affectedChannels.size()
        );
        return new WebAdminDtos.DoctorReportDto(summary, List.copyOf(issues));
    }

    public static WebAdminDtos.DoctorIssueDto fromDiagnosticIssue(DiagnosticIssue issue, int index) {
        if (issue == null) {
            return emptyIssue("diagnostic:" + index);
        }
        String severity = switch (issue.severity()) {
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
            case INFO -> "INFO";
        };
        String relatedType = issue.deviceId().isBlank()
                ? (issue.channel().isBlank() ? "SYSTEM" : "CHANNEL")
                : "DEVICE";
        String relatedId = issue.deviceId().isBlank() ? issue.channel() : issue.deviceId();
        String navigation = switch (relatedType) {
            case "DEVICE" -> "device:" + relatedId;
            case "CHANNEL" -> "channel:" + issue.channel();
            default -> "";
        };
        return new WebAdminDtos.DoctorIssueDto(
                issue.code().isBlank() ? "diagnostic:" + index : issue.code() + ":" + index,
                severity,
                issue.title(),
                issue.message(),
                relatedType,
                relatedId,
                issue.deviceName(),
                issue.channel(),
                issue.message(),
                issue.suggestion(),
                Instant.now().toString(),
                navigation
        );
    }

    private static WebAdminDtos.DoctorIssueDto fromDoctorIssue(SignalDoctorIssue issue, int index, MinecraftServer server) {
        String severity = switch (issue.severity()) {
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
            case INFO -> "INFO";
        };
        String title = issue.title() == null ? "" : issue.title();
        String detail = issue.detail() == null ? "" : issue.detail();
        String text = title + " " + detail;
        String joinId = extractSignalJoinId(text, server);
        if (!joinId.isBlank()) {
            return new WebAdminDtos.DoctorIssueDto(
                    "doctor:" + index,
                    severity,
                    title,
                    detail,
                    "SIGNAL_JOIN",
                    joinId,
                    joinId,
                    "",
                    detail,
                    "打开信号汇合配置检查输入、输出、scope、timeout 或循环风险。",
                    Instant.now().toString(),
                    "#/signal-joins/" + joinId
            );
        }
        String channel = extractChannel(text);
        return new WebAdminDtos.DoctorIssueDto(
                "doctor:" + index,
                severity,
                title,
                detail,
                channel.isBlank() ? "SYSTEM" : "CHANNEL",
                channel,
                channel,
                channel,
                detail,
                "",
                Instant.now().toString(),
                channel.isBlank() ? "" : "channel:" + channel
        );
    }

    private static List<WebAdminDtos.DoctorIssueDto> snapshotDiagnostics(MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        WebAdminSnapshotStore.ManifestLoadResult manifest = WebAdminSnapshotStore.loadManifest(server);
        if (!manifest.degraded()) {
            return List.of();
        }
        return List.of(new WebAdminDtos.DoctorIssueDto(
                "snapshot-degraded:manifest",
                "ERROR",
                "配置时间轴读取异常",
                manifest.message().isBlank() ? "快照 manifest 当前不可读取，手动保存点和回滚会被阻断以避免覆盖损坏文件。" : manifest.message(),
                "SNAPSHOT",
                "timeline",
                "配置时间轴",
                "",
                "快照 manifest degraded；详细解析错误只写入服务端日志。",
                "检查 tzz/webadmin/snapshots/manifest.json，修复前不要继续创建保存点或执行回滚。",
                Instant.now().toString(),
                "#/snapshots"
        ));
    }

    private static List<WebAdminDtos.DoctorIssueDto> templateDiagnostics(MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        WebAdminTemplateStore.TemplateLoadResult loaded = WebAdminTemplateStore.loadWithStatus(server);
        if (!loaded.degraded()) {
            return List.of();
        }
        return List.of(new WebAdminDtos.DoctorIssueDto(
                "template-degraded:store",
                "ERROR",
                "用户模板库读取异常",
                loaded.message().isBlank() ? "templates.json 当前不可读取，用户模板导入、详情和应用会保持 fail closed。" : loaded.message(),
                "TEMPLATE",
                "templates.json",
                "用户模板库",
                "",
                "Template store degraded；内置模板仍可只读展示，用户模板写入会被阻断。",
                "检查 world-scoped templates.json 是否为有效 JSON，再重新打开模板中心。",
                Instant.now().toString(),
                "#/templates"
        ));
    }

    private static WebAdminDtos.DoctorIssueDto emptyIssue(String id) {
        return new WebAdminDtos.DoctorIssueDto(
                id,
                "INFO",
                "",
                "",
                "SYSTEM",
                "",
                "",
                "",
                "",
                "",
                Instant.now().toString(),
                ""
        );
    }

    private static String extractChannel(String text) {
        if (text == null) {
            return "";
        }
        int index = text.indexOf("channel:");
        if (index < 0) {
            return "";
        }
        int start = index + "channel:".length();
        int end = start;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        return text.substring(start, end).trim();
    }

    private static String extractSignalJoinId(String text, MinecraftServer server) {
        if (text == null || server == null) {
            return "";
        }
        String lower = text.toLowerCase();
        if (!lower.contains("signal join") && !lower.contains("join") && !text.contains("信号汇合")) {
            return "";
        }
        for (SignalJoinDefinition raw : SignalJoinStore.getSnapshot(server)) {
            SignalJoinDefinition join = raw == null ? null : raw.normalized();
            if (join == null || join.id.isBlank()) {
                continue;
            }
            if (text.contains(join.id) || (!join.displayName.isBlank() && text.contains(join.displayName))) {
                return join.id;
            }
        }
        return "";
    }
}
