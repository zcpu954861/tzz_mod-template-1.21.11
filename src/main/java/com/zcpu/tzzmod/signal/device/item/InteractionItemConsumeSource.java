package com.zcpu.tzzmod.signal.device.item;

public final class InteractionItemConsumeSource {
    public static final String MATCHED_SOURCE = "matched_source";
    public static final String MAIN_HAND = "main_hand";
    public static final String OFF_HAND = "off_hand";
    public static final String INVENTORY = "inventory";

    private InteractionItemConsumeSource() {
    }

    public static String normalize(String source) {
        if (source == null || source.isBlank()) {
            return MATCHED_SOURCE;
        }
        return switch (source.trim().toLowerCase()) {
            case MAIN_HAND -> MAIN_HAND;
            case OFF_HAND -> OFF_HAND;
            case INVENTORY -> INVENTORY;
            default -> MATCHED_SOURCE;
        };
    }

    public static String displayName(String source) {
        return switch (normalize(source)) {
            case MAIN_HAND -> "主手";
            case OFF_HAND -> "副手";
            case INVENTORY -> "背包/热键栏";
            default -> "匹配来源";
        };
    }
}
