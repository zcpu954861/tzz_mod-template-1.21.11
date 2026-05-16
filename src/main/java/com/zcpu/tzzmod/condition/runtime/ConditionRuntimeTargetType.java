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
    REGION_STAY("REGION_STAY", "RegionController stay 动作列表");

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
