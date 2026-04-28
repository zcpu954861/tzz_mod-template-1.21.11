package com.zcpu.tzzmod.webadmin.write;

public record WebAdminWriteTarget(
        String targetType,
        String targetId,
        String displayName
) {
    public WebAdminWriteTarget {
        targetType = safe(targetType);
        targetId = safe(targetId);
        displayName = safe(displayName);
    }

    public static WebAdminWriteTarget none() {
        return new WebAdminWriteTarget("", "", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
