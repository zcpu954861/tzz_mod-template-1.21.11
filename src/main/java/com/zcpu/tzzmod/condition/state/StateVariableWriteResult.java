package com.zcpu.tzzmod.condition.state;

import java.util.List;

public record StateVariableWriteResult(
        boolean success,
        boolean changed,
        String code,
        String message,
        StateVariableRecord record,
        List<String> validationErrors,
        String currentFingerprint
) {
    public StateVariableWriteResult {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public static StateVariableWriteResult success(String code, String message, StateVariableRecord record, boolean changed) {
        return new StateVariableWriteResult(true, changed, code, message, record, List.of(), record == null ? "" : record.fingerprint());
    }

    public static StateVariableWriteResult failed(String code, String message, List<String> errors, StateVariableRecord current) {
        return new StateVariableWriteResult(false, false, code, message, current, errors, current == null ? "" : current.fingerprint());
    }
}
