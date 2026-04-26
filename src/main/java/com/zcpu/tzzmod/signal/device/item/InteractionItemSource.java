package com.zcpu.tzzmod.signal.device.item;

public final class InteractionItemSource {
    public static final String MAIN_HAND = "main_hand";
    public static final String OFF_HAND = "off_hand";
    public static final String INVENTORY_CONTAINS = "inventory_contains";

    private InteractionItemSource() {
    }

    public static String normalize(String source) {
        if (source == null || source.isBlank()) {
            return MAIN_HAND;
        }
        return switch (source.trim().toLowerCase()) {
            case OFF_HAND -> OFF_HAND;
            case INVENTORY_CONTAINS -> INVENTORY_CONTAINS;
            default -> MAIN_HAND;
        };
    }

    public static boolean supportsConsume(String source) {
        return MAIN_HAND.equals(normalize(source));
    }

    public static String displayName(String source) {
        return switch (normalize(source)) {
            case OFF_HAND -> "off_hand（副手）";
            case INVENTORY_CONTAINS -> "inventory_contains（主背包 / 热键栏）";
            default -> "main_hand（主手）";
        };
    }
}
