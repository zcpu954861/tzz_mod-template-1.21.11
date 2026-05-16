package com.zcpu.tzzmod.condition;

public final class ConditionGroupIds {
    private ConditionGroupIds() {
    }

    public static String normalize(String value) {
        String raw = safe(value).trim().toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean lastDash = false;
        for (int i = 0; i < raw.length() && builder.length() < 96; i++) {
            char c = raw.charAt(i);
            boolean accepted = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':';
            if (accepted) {
                builder.append(c);
                lastDash = c == '-';
            } else if (Character.isWhitespace(c) && !lastDash && builder.length() > 0) {
                builder.append('-');
                lastDash = true;
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
