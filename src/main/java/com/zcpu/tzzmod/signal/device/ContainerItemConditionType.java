package com.zcpu.tzzmod.signal.device;

public enum ContainerItemConditionType {
    SLOT_EMPTY("slot_empty"),
    SLOT_ITEM("slot_item"),
    TOTAL_ITEM("total_item");

    private final String id;

    ContainerItemConditionType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ContainerItemConditionType fromId(String raw) {
        String normalized = normalize(raw);
        for (ContainerItemConditionType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return SLOT_EMPTY;
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        for (ContainerItemConditionType type : values()) {
            if (type.id.equals(value)) {
                return type.id;
            }
        }
        return SLOT_EMPTY.id;
    }
}
