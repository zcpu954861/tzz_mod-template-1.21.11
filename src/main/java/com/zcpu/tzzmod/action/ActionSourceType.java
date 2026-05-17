package com.zcpu.tzzmod.action;

public enum ActionSourceType {
    BLOCKING_CARD("blocking_card"),
    PASSWORD_MACHINE("password_machine"),
    SILENT_SENSOR_PLATE("silent_sensor_plate"),
    REGION_CONTROLLER("region_controller"),
    SIGNAL_BRIDGE("signal_bridge"),
    SIGNAL_JOIN("signal_join"),
    SCHEDULER_TIMER("scheduler_timer"),
    SIGNAL_DEVICE("signal_device"),
    ACTION_RELAY("action_relay"),
    VIRTUAL_BLOCK_DEVICE("virtual_block_device"),
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
