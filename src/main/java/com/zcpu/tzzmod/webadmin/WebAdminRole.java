package com.zcpu.tzzmod.webadmin;

public enum WebAdminRole {
    VIEWER("VIEWER", "查看者"),
    TESTER("TESTER", "测试员"),
    EDITOR("EDITOR", "编辑者"),
    OWNER("OWNER", "所有者");

    private final String id;
    private final String displayName;

    WebAdminRole(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName + "（" + id + "）";
    }

    public static WebAdminRole parse(String value) {
        if (value == null || value.isBlank()) {
            return VIEWER;
        }
        String normalized = value.trim().toUpperCase();
        for (WebAdminRole role : values()) {
            if (role.id.equals(normalized)) {
                return role;
            }
        }
        return null;
    }
}
