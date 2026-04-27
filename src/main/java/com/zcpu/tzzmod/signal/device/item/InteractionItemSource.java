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
        return switch (normalize(source)) {
            case MAIN_HAND, OFF_HAND, INVENTORY_CONTAINS -> true;
            default -> false;
        };
    }

    public static boolean isArmorSource(String source) {
        return switch (normalize(source)) {
            case ARMOR_HEAD, ARMOR_CHEST, ARMOR_LEGS, ARMOR_FEET, ARMOR_ANY -> true;
            default -> false;
        };
    }

    public static String displayName(String source) {
        return switch (normalize(source)) {
            case OFF_HAND -> "副手（off_hand）";
            case INVENTORY_CONTAINS -> "背包/热键栏（inventory_contains）";
            case ARMOR_HEAD -> "头盔槽（armor_head）";
            case ARMOR_CHEST -> "胸甲槽（armor_chest）";
            case ARMOR_LEGS -> "护腿槽（armor_legs）";
            case ARMOR_FEET -> "靴子槽（armor_feet）";
            case ARMOR_ANY -> "任意盔甲槽（armor_any）";
            default -> "主手（main_hand）";
        };
    }
}
