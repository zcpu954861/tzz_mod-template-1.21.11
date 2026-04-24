package com.zcpu.tzzmod.action;

public enum ActionSourceType {
    BLOCKING_CARD("blocking_card"),
    PASSWORD_MACHINE("password_machine"),
    SILENT_SENSOR_PLATE("silent_sensor_plate"),
    REGION_CONTROLLER("region_controller"),
    COMMAND("command"),
    UNKNOWN("unknown");

    private final String id;

    ActionSourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
