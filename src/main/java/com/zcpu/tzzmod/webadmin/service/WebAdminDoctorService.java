package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalDoctor;
import com.zcpu.tzzmod.signal.SignalDoctorIssue;
import com.zcpu.tzzmod.signal.SignalDoctorReport;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
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
            WebAdminDtos.DoctorIssueDto dto = fromDoctorIssue(issue, index);
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

    private static WebAdminDtos.DoctorIssueDto fromDoctorIssue(SignalDoctorIssue issue, int index) {
        String severity = switch (issue.severity()) {
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
            case INFO -> "INFO";
        };
        String title = issue.title() == null ? "" : issue.title();
        String detail = issue.detail() == null ? "" : issue.detail();
        String channel = extractChannel(title + " " + detail);
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
}
