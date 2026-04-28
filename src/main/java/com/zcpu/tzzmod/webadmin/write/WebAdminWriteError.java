package com.zcpu.tzzmod.webadmin.write;

import java.util.List;

public record WebAdminWriteError(
        String code,
        String message,
        List<WebAdminValidationError> validationErrors
) {
    public WebAdminWriteError {
        code = safe(code);
        message = safe(message);
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public static WebAdminWriteError of(WebAdminWriteResultCode code) {
        WebAdminWriteResultCode resultCode = code == null ? WebAdminWriteResultCode.INTERNAL_ERROR : code;
        return new WebAdminWriteError(resultCode.id(), resultCode.defaultMessage(), List.of());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
