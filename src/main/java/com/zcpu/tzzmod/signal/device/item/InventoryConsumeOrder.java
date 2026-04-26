package com.zcpu.tzzmod.signal.device.item;

public final class InventoryConsumeOrder {
    public static final String HOTBAR_FIRST = "hotbar_first";
    public static final String MAIN_INVENTORY_FIRST = "main_inventory_first";

    private InventoryConsumeOrder() {
    }

    public static String normalize(String order) {
        if (order == null || order.isBlank()) {
            return HOTBAR_FIRST;
        }
        return MAIN_INVENTORY_FIRST.equals(order.trim().toLowerCase())
                ? MAIN_INVENTORY_FIRST
                : HOTBAR_FIRST;
    }

    public static String displayName(String order) {
        return MAIN_INVENTORY_FIRST.equals(normalize(order)) ? "优先主背包" : "优先热键栏";
    }
}
