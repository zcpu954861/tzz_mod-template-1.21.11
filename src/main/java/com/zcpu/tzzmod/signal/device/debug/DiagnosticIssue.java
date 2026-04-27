package com.zcpu.tzzmod.signal.device.debug;

public record DiagnosticIssue(
        DiagnosticSeverity severity,
        String code,
        String title,
        String message,
        String suggestion,
        String relatedCommand,
        String deviceId,
        String deviceName,
        String pos,
        String channel
) {
    public DiagnosticIssue {
        severity = severity == null ? DiagnosticSeverity.INFO : severity;
        code = clean(code);
        title = clean(title);
        message = clean(message);
        suggestion = clean(suggestion);
        relatedCommand = clean(relatedCommand);
        deviceId = clean(deviceId);
        deviceName = clean(deviceName);
        pos = clean(pos);
        channel = clean(channel);
    }

    public static DiagnosticIssue of(
            DiagnosticSeverity severity,
            String code,
            String title,
            String message,
            String suggestion
    ) {
        return new DiagnosticIssue(severity, code, title, message, suggestion, "", "", "", "", "");
    }

    public DiagnosticIssue withDevice(String id, String name, String position) {
        return new DiagnosticIssue(severity, code, title, message, suggestion, relatedCommand, id, name, position, channel);
    }

    public DiagnosticIssue withChannel(String channel) {
        return new DiagnosticIssue(severity, code, title, message, suggestion, relatedCommand, deviceId, deviceName, pos, channel);
    }

    public DiagnosticIssue withCommand(String command) {
        return new DiagnosticIssue(severity, code, title, message, suggestion, command, deviceId, deviceName, pos, channel);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
