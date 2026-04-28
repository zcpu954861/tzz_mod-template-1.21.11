package com.zcpu.tzzmod.webadmin.write;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class WebAdminWriteSanitizer {
    private WebAdminWriteSanitizer() {
    }

    public static String summarize(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (isSensitiveKey(text)) {
            return "已隐藏";
        }
        if (text.length() > 120) {
            return text.substring(0, 117) + "...";
        }
        return text;
    }

    public static Map<String, Object> redactMap(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        int hiddenCount = 0;
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            if (isSensitiveKey(key)) {
                hiddenCount++;
                result.put("hiddenField" + hiddenCount, "已隐藏");
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                result.put(key, redactNestedMap(mapValue));
            } else {
                result.put(key, sanitizeValue(value));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> redactNestedMap(Map<?, ?> source) {
        Map<String, Object> nested = new LinkedHashMap<>();
        int hiddenCount = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            if (isSensitiveKey(key)) {
                hiddenCount++;
                nested.put("hiddenField" + hiddenCount, "已隐藏");
            } else {
                nested.put(key, sanitizeValue(entry.getValue()));
            }
        }
        return Map.copyOf(nested);
    }

    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return summarize(text);
        }
        return value;
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password")
                || lower.contains("hash")
                || lower.contains("salt")
                || lower.contains("sessionid")
                || lower.contains("session_id")
                || lower.contains("token")
                || lower.contains("cookie")
                || lower.contains("secret");
    }
}
