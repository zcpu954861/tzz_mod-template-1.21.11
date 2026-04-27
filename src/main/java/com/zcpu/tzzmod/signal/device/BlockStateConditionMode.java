package com.zcpu.tzzmod.signal.device;

public enum BlockStateConditionMode {
    CONDITION_ENTER("condition_enter"),
    CONDITION_EXIT("condition_exit"),
    CONDITION_BOTH("condition_both");

    private final String id;

    BlockStateConditionMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean triggersEnter() {
        return this == CONDITION_ENTER || this == CONDITION_BOTH;
    }

    public boolean triggersExit() {
        return this == CONDITION_EXIT || this == CONDITION_BOTH;
    }

    public static BlockStateConditionMode fromId(String raw) {
        String normalized = normalize(raw);
        for (BlockStateConditionMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return CONDITION_ENTER;
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        for (BlockStateConditionMode mode : values()) {
            if (mode.id.equals(value)) {
                return mode.id;
            }
        }
        return CONDITION_ENTER.id;
    }

    public static String displayName(String raw) {
        return switch (fromId(raw)) {
            case CONDITION_EXIT -> "退出条件时触发（condition_exit）";
            case CONDITION_BOTH -> "进入和退出都触发（condition_both）";
            default -> "进入条件时触发（condition_enter）";
        };
    }
}
