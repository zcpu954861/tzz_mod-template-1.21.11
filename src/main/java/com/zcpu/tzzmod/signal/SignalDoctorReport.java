package com.zcpu.tzzmod.signal;

import java.util.List;

public record SignalDoctorReport(
        int listenerCount,
        int enabledListenerCount,
        int disabledListenerCount,
        int channelCount,
        int historyCount,
        List<SignalDoctorIssue> issues
) {
    public SignalDoctorReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
