package com.zcpu.tzzmod.webadmin.write;

public enum WebAdminAuditEventType {
    WRITE_PREVIEW("write_preview", "写入预览"),
    WRITE_APPLIED("write_applied", "写入已应用"),
    WRITE_DENIED("write_denied", "写入被拒绝"),
    WRITE_VALIDATION_FAILED("write_validation_failed", "写入校验失败"),
    WRITE_CONFLICT("write_conflict", "写入冲突"),
    WRITE_INTERNAL_ERROR("write_internal_error", "写入内部错误");

    private final String id;
    private final String displayName;

    WebAdminAuditEventType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
