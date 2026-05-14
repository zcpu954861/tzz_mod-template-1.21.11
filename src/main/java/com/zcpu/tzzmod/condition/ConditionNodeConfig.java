package com.zcpu.tzzmod.condition;

import java.util.LinkedHashMap;
import java.util.Map;

public record ConditionNodeConfig(Map<String, String> values) {
    public static final ConditionNodeConfig EMPTY = new ConditionNodeConfig(Map.of());

    public ConditionNodeConfig {
        values = normalize(values);
    }

    public static ConditionNodeConfig of(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return new ConditionNodeConfig(map);
    }

    public static ConditionNodeConfig of(String key1, String value1, String key2, String value2) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return new ConditionNodeConfig(map);
    }

    public String get(String key) {
        if (key == null) {
            return "";
        }
        return values.getOrDefault(key.trim(), "");
    }

    public boolean has(String key) {
        return !get(key).isBlank();
    }

    private static Map<String, String> normalize(Map<String, String> raw) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (raw != null) {
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                String key = safe(entry.getKey());
                if (!key.isBlank()) {
                    copy.put(key, safe(entry.getValue()));
                }
            }
        }
        return Map.copyOf(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
