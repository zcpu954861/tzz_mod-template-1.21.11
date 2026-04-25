package com.zcpu.tzzmod.action;

public enum ActionType {
    COMMAND("command"),
    MESSAGE("message"),
    SOUND("sound"),
    SIGNAL("signal");

    private final String id;

    ActionType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ActionType fromId(String raw) {
        if (raw == null) {
            return COMMAND;
        }

        for (ActionType value : values()) {
            if (value.id.equalsIgnoreCase(raw.trim())) {
                return value;
            }
        }

        return COMMAND;
    }
}
