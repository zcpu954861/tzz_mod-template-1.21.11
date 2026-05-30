package com.zcpu.tzzmod.action.schema;

public enum ActionFieldType {
    TEXT("text"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    SELECT("select"),
    TEXTAREA("textarea"),
    CHANNEL_PICKER("channel_picker"),
    STATE_VARIABLE_PICKER("state_variable_picker"),
    CONDITION_GROUP_PICKER("condition_group_picker"),
    PLAYER_TARGET_MODE("player_target_mode"),
    READONLY_SUMMARY("readonly_summary");

    private final String id;

    ActionFieldType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
