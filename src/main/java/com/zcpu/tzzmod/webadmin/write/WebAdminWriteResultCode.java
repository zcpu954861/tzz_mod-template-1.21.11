package com.zcpu.tzzmod.webadmin.write;

public enum WebAdminWriteResultCode {
    OK("ok", "操作已完成。"),
    PERMISSION_DENIED("permission_denied", "权限不足，无法执行该操作。"),
    UNAUTHENTICATED("unauthenticated", "请先登录。"),
    CSRF_REQUIRED("csrf_required", "写操作需要 CSRF 校验。"),
    CSRF_INVALID("csrf_invalid", "CSRF 校验失败，请刷新页面后重试。"),
    VALIDATION_FAILED("validation_failed", "提交内容未通过校验。"),
    TARGET_NOT_FOUND("target_not_found", "目标不存在或已被删除。"),
    CONFLICT_DETECTED("conflict_detected", "目标已被其他操作修改，请刷新后重试。"),
    DANGEROUS_OPERATION_REQUIRES_CONFIRMATION("dangerous_operation_requires_confirmation", "该危险操作需要二次确认。"),
    NO_CHANGE("no_change", "没有检测到需要保存的变化。"),
    INTERNAL_ERROR("internal_error", "写操作处理失败。");

    private final String id;
    private final String defaultMessage;

    WebAdminWriteResultCode(String id, String defaultMessage) {
        this.id = id;
        this.defaultMessage = defaultMessage;
    }

    public String id() {
        return id;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
