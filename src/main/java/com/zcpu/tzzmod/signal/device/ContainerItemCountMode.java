package com.zcpu.tzzmod.signal.device;

public enum ContainerItemCountMode {
    AT_LEAST("at_least"),
    EXACTLY("exactly"),
    AT_MOST("at_most"),
    IGNORE("ignore");

    private final String id;

    ContainerItemCountMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean matches(int actual, int expected) {
        return switch (this) {
            case AT_LEAST -> actual >= expected;
            case EXACTLY -> actual == expected;
            case AT_MOST -> actual <= expected;
            case IGNORE -> true;
        };
    }

    public static ContainerItemCountMode fromId(String raw) {
        String normalized = normalize(raw);
        for (ContainerItemCountMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return AT_LEAST;
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        for (ContainerItemCountMode mode : values()) {
            if (mode.id.equals(value)) {
                return mode.id;
            }
        }
        return AT_LEAST.id;
    }
}
