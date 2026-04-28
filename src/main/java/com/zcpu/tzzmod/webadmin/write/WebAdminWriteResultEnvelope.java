package com.zcpu.tzzmod.webadmin.write;

public record WebAdminWriteResultEnvelope(
        boolean ok,
        WebAdminWriteResult data,
        WebAdminWriteError error
) {
    public static WebAdminWriteResultEnvelope of(WebAdminWriteResult result) {
        if (result != null && result.success()) {
            return new WebAdminWriteResultEnvelope(true, result, null);
        }
        WebAdminWriteResult safeResult = result == null
                ? WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, WebAdminWriteTarget.none(), "")
                : result;
        return new WebAdminWriteResultEnvelope(
                false,
                safeResult,
                new WebAdminWriteError(safeResult.code(), safeResult.message(), safeResult.validationErrors())
        );
    }
}
