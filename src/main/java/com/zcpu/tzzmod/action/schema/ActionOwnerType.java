package com.zcpu.tzzmod.action.schema;

public enum ActionOwnerType {
    SIGNAL_LISTENER("signal_listener", "SignalListener 动作列表"),
    ACTION_RELAY("action_relay", "ActionRelay 动作列表"),
    REGION_ENTER("region_enter", "区域进入动作"),
    REGION_EXIT("region_exit", "区域离开动作"),
    REGION_STAY("region_stay", "区域停留动作"),
    TIMER_START("timer_on_start", "Timer 启动动作"),
    TIMER_TICK("timer_on_tick", "Timer Tick 动作"),
    TIMER_COMPLETE("timer_on_complete", "Timer 完成动作"),
    TIMER_CANCEL("timer_on_cancel", "Timer 取消动作");

    private final String id;
    private final String displayName;

    ActionOwnerType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
