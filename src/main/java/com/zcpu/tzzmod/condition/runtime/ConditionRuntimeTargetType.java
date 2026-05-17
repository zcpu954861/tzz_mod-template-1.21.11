package com.zcpu.tzzmod.condition.runtime;

import java.util.Locale;
import java.util.Optional;

public enum ConditionRuntimeTargetType {
    VBD_REDSTONE("VBD_REDSTONE", "VBD 红石 / 受电状态"),
    VBD_BLOCKSTATE("VBD_BLOCKSTATE", "VBD BlockState 条件"),
    VBD_INTERACTION("VBD_INTERACTION", "VBD 玩家右键交互"),
    ITEM_SUBMIT("ITEM_SUBMIT", "VBD itemSubmit"),
    CONTAINER_OPEN("CONTAINER_OPEN", "VBD 容器打开"),
    CONTAINER_CLOSE("CONTAINER_CLOSE", "VBD 容器关闭"),
    CONTAINER_CHANGE("CONTAINER_CHANGE", "VBD 容器内容变化"),
    SIGNAL_LISTENER("SIGNAL_LISTENER", "Signal Listener 动作列表"),
    ACTION_RELAY("ACTION_RELAY", "Action Relay 动作列表"),
    REGION_ENTER("REGION_ENTER", "RegionController enter 动作列表"),
    REGION_EXIT("REGION_EXIT", "RegionController exit 动作列表"),
    REGION_STAY("REGION_STAY", "RegionController stay 动作列表"),
    SIGNAL_LISTENER_ACTION("SIGNAL_LISTENER_ACTION", "Signal Listener 单条 Action"),
    ACTION_RELAY_ACTION("ACTION_RELAY_ACTION", "Action Relay 单条 Action"),
    REGION_ENTER_ACTION("REGION_ENTER_ACTION", "RegionController enter 单条 Action"),
    REGION_EXIT_ACTION("REGION_EXIT_ACTION", "RegionController exit 单条 Action"),
    REGION_STAY_ACTION("REGION_STAY_ACTION", "RegionController stay 单条 Action"),
    TIMER_ON_START("TIMER_ON_START", "Timer 启动动作列表"),
    TIMER_ON_START_ACTION("TIMER_ON_START_ACTION", "Timer 启动单条 Action"),
    TIMER_ON_TICK("TIMER_ON_TICK", "Timer 间隔动作列表"),
    TIMER_ON_TICK_ACTION("TIMER_ON_TICK_ACTION", "Timer 间隔单条 Action"),
    TIMER_ON_COMPLETE("TIMER_ON_COMPLETE", "Timer 完成动作列表"),
    TIMER_ON_COMPLETE_ACTION("TIMER_ON_COMPLETE_ACTION", "Timer 完成单条 Action"),
    TIMER_ON_CANCEL("TIMER_ON_CANCEL", "Timer 取消动作列表"),
    TIMER_ON_CANCEL_ACTION("TIMER_ON_CANCEL_ACTION", "Timer 取消单条 Action");

    private final String id;
    private final String displayName;

    ConditionRuntimeTargetType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ConditionRuntimeTargetType> parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (ConditionRuntimeTargetType type : values()) {
            if (type.id.equals(value) || type.name().equals(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
