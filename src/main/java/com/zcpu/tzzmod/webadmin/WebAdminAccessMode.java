package com.zcpu.tzzmod.webadmin;

public enum WebAdminAccessMode {
    LOCAL_ONLY("LOCAL_ONLY", "本机访问"),
    LAN_DEV("LAN_DEV", "局域网协作开发"),
    MULTIPLAYER_DEV("MULTIPLAYER_DEV", "多人服务器协作开发");

    private final String id;
    private final String displayName;

    WebAdminAccessMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName + "（" + id + "）";
    }

    public boolean needsSecurityWarning() {
        return this == LAN_DEV || this == MULTIPLAYER_DEV;
    }

    public static WebAdminAccessMode parse(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL_ONLY;
        }
        String normalized = value.trim().toUpperCase();
        for (WebAdminAccessMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return LOCAL_ONLY;
    }
}
