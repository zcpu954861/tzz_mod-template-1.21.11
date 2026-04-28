package com.zcpu.tzzmod.webadmin.write;

public enum WebAdminAuditResult {
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    DENIED("denied", "权限拒绝"),
    VALIDATION_FAILED("validation_failed", "校验失败");

    private final String id;
    private final String displayName;

    WebAdminAuditResult(String id, String displayName) {
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
