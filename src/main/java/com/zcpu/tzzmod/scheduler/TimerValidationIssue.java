package com.zcpu.tzzmod.scheduler;

public record TimerValidationIssue(String field, String code, String message, String rejectedValue) {
    public TimerValidationIssue {
        field = field == null ? "" : field.trim();
        code = code == null ? "" : code.trim();
        message = message == null ? "" : message.trim();
        rejectedValue = rejectedValue == null ? "" : rejectedValue.trim();
    }
}
