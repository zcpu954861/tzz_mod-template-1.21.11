package com.zcpu.tzzmod.signal;

public record SignalDoctorIssue(
        Severity severity,
        String title,
        String detail
) {
    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}
