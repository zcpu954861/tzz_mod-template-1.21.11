package com.zcpu.tzzmod.condition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record ConditionGroupDefinition(
        String id,
        int version,
        String displayName,
        String note,
        List<String> tags,
        ConditionNode root
) {
    public static final int CURRENT_VERSION = 1;

    public ConditionGroupDefinition {
        id = normalizeId(id);
        version = version <= 0 ? CURRENT_VERSION : version;
        displayName = safe(displayName);
        note = safe(note);
        tags = normalizeTags(tags);
        root = root == null ? ConditionNode.group("root", ConditionGroupMode.AND, List.of()) : root.normalized("root");
    }

    public static ConditionGroupDefinition of(String id, ConditionNode root) {
        return new ConditionGroupDefinition(id, CURRENT_VERSION, "", "", List.of(), root);
    }

    public String stableFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(canonical().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public String canonical() {
        StringBuilder builder = new StringBuilder();
        appendDefinition(builder);
        return builder.toString();
    }

    private void appendDefinition(StringBuilder builder) {
        builder.append("id=").append(id)
                .append(";version=").append(version)
                .append(";displayName=").append(displayName)
                .append(";note=").append(note)
                .append(";tags=").append(String.join(",", tags))
                .append(";root=");
        appendNode(builder, root);
    }

    private static void appendNode(StringBuilder builder, ConditionNode node) {
        builder.append('{')
                .append("id=").append(node.id())
                .append(";type=").append(node.type())
                .append(";enabled=").append(node.enabled())
                .append(";mode=").append(node.groupMode().id())
                .append(";name=").append(node.name())
                .append(";note=").append(node.note())
                .append(";config=");
        node.config().values().entrySet().stream()
                .sorted(MapEntryComparator.INSTANCE)
                .forEach((entry) -> builder.append(entry.getKey()).append('=').append(entry.getValue()).append(','));
        builder.append(";children=[");
        for (ConditionNode child : node.children()) {
            appendNode(builder, child);
        }
        builder.append("]}");
    }

    private static List<String> normalizeTags(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(ConditionGroupDefinition::safe)
                .filter((tag) -> !tag.isBlank())
                .distinct()
                .limit(16)
                .toList();
    }

    private static String normalizeId(String value) {
        String raw = safe(value).toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length() && builder.length() < 96; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        return builder.isEmpty() ? "condition-group" : builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private enum MapEntryComparator implements java.util.Comparator<java.util.Map.Entry<String, String>> {
        INSTANCE;

        @Override
        public int compare(java.util.Map.Entry<String, String> left, java.util.Map.Entry<String, String> right) {
            return left.getKey().compareTo(right.getKey());
        }
    }
}
