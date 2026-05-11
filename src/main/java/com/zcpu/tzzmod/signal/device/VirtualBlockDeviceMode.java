package com.zcpu.tzzmod.signal.device;

public enum VirtualBlockDeviceMode {
    REDSTONE_DISABLED("redstone_disabled"),
    REDSTONE_RISING("redstone_rising"),
    REDSTONE_FALLING("redstone_falling"),
    REDSTONE_BOTH("redstone_both");

    private final String id;

    VirtualBlockDeviceMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean triggersRising() {
        return this == REDSTONE_RISING || this == REDSTONE_BOTH;
    }

    public boolean triggersFalling() {
        return this == REDSTONE_FALLING || this == REDSTONE_BOTH;
    }

    public static VirtualBlockDeviceMode fromId(String raw) {
        String value = normalize(raw);
        for (VirtualBlockDeviceMode mode : values()) {
            if (mode.id.equals(value)) {
                return mode;
            }
        }
        return REDSTONE_RISING;
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        for (VirtualBlockDeviceMode mode : values()) {
            if (mode.id.equals(value)) {
                return mode.id;
            }
        }
        return REDSTONE_RISING.id;
    }

    public static String displayName(String raw) {
        return switch (fromId(raw)) {
            case REDSTONE_DISABLED -> "不触发红石边沿（redstone_disabled）";
            case REDSTONE_FALLING -> "断电时触发（redstone_falling）";
            case REDSTONE_BOTH -> "通电和断电都触发（redstone_both）";
            default -> "通电时触发（redstone_rising）";
        };
    }
}
