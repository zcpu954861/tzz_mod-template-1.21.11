package com.zcpu.tzzmod.signal.device.item;

public final class InteractionItemSource {
    public static final String MAIN_HAND = "main_hand";
    public static final String OFF_HAND = "off_hand";
    public static final String INVENTORY_CONTAINS = "inventory_contains";
    public static final String ARMOR_HEAD = "armor_head";
    public static final String ARMOR_CHEST = "armor_chest";
    public static final String ARMOR_LEGS = "armor_legs";
    public static final String ARMOR_FEET = "armor_feet";
    public static final String ARMOR_ANY = "armor_any";

    private InteractionItemSource() {
    }

    public static String normalize(String source) {
        if (source == null || source.isBlank()) {
            return MAIN_HAND;
        }
        return switch (source.trim().toLowerCase()) {
            case OFF_HAND -> OFF_HAND;
            case INVENTORY_CONTAINS -> INVENTORY_CONTAINS;
            case ARMOR_HEAD -> ARMOR_HEAD;
            case ARMOR_CHEST -> ARMOR_CHEST;
            case ARMOR_LEGS -> ARMOR_LEGS;
            case ARMOR_FEET -> ARMOR_FEET;
            case ARMOR_ANY -> ARMOR_ANY;
            default -> MAIN_HAND;
        };
    }

    public static boolean supportsConsume(String source) {
        return MAIN_HAND.equals(normalize(source));
    }

    public static boolean isArmorSource(String source) {
        return switch (normalize(source)) {
            case ARMOR_HEAD, ARMOR_CHEST, ARMOR_LEGS, ARMOR_FEET, ARMOR_ANY -> true;
            default -> false;
        };
    }

    public static String displayName(String source) {
        return switch (normalize(source)) {
            case OFF_HAND -> "off_hand（副手）";
            case INVENTORY_CONTAINS -> "inventory_contains（主背包 / 热键栏）";
            case ARMOR_HEAD -> "armor_head（头盔槽）";
            case ARMOR_CHEST -> "armor_chest（胸甲槽）";
            case ARMOR_LEGS -> "armor_legs（护腿槽）";
            case ARMOR_FEET -> "armor_feet（靴子槽）";
            case ARMOR_ANY -> "armor_any（任意盔甲槽）";
            default -> "main_hand（主手）";
        };
    }
}
