package com.zcpu.tzzmod.signal.device.debug;

import java.util.List;

public record DeviceDiagnostic(
        String deviceId,
        String deviceName,
        String type,
        String pos,
        List<DiagnosticIssue> issues
) {
    public DeviceDiagnostic {
        deviceId = clean(deviceId);
        deviceName = clean(deviceName);
        type = clean(type);
        pos = clean(pos);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public long count(DiagnosticSeverity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).count();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
