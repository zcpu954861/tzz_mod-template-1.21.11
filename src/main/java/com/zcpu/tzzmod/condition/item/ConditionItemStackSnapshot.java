package com.zcpu.tzzmod.condition.item;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public record ConditionItemStackSnapshot(
        String itemId,
        int count,
        String displayName,
        List<String> lore,
        Map<String, String> customData,
        Map<String, String> components
) {
    public ConditionItemStackSnapshot {
        itemId = normalizeItemId(itemId);
        count = Math.max(0, count);
        displayName = safe(displayName);
        lore = lore == null ? List.of() : lore.stream().map(ConditionItemStackSnapshot::safe).toList();
        customData = copyMap(customData);
        components = copyMap(components);
    }

    public static ConditionItemStackSnapshot empty() {
        return new ConditionItemStackSnapshot("", 0, "", List.of(), Map.of(), Map.of());
    }

    public static ConditionItemStackSnapshot of(String itemId, int count) {
        return new ConditionItemStackSnapshot(itemId, count, "", List.of(), Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return itemId.isBlank() || "minecraft:air".equals(itemId) || count <= 0;
    }

    public String summary() {
        if (isEmpty()) {
            return "空物品";
        }
        return itemId + " x" + count;
    }

    static String normalizeItemId(String raw) {
        return safe(raw).toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> copyMap(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, String> copy = new TreeMap<>();
        raw.forEach((key, value) -> {
            String safeKey = safe(key);
            if (!safeKey.isBlank()) {
                copy.put(safeKey, safe(value));
            }
        });
        return Map.copyOf(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
