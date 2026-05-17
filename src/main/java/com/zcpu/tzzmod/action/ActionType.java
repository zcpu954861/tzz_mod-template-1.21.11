package com.zcpu.tzzmod.action;

import com.google.gson.annotations.SerializedName;

public enum ActionType {
    COMMAND("command"),
    MESSAGE("message"),
    SOUND("sound"),
    SIGNAL("signal"),
    @SerializedName(value = "state_variable", alternate = {"STATE_VARIABLE"})
    STATE_VARIABLE("state_variable");

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
            if (value.id.equalsIgnoreCase(raw.trim()) || value.name().equalsIgnoreCase(raw.trim())) {
                return value;
            }
        }

        return COMMAND;
    }
}
