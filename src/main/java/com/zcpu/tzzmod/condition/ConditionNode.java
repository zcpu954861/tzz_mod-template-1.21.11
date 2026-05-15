package com.zcpu.tzzmod.condition;

import java.util.List;
import java.util.Locale;

public record ConditionNode(
        String id,
        String type,
        String name,
        String note,
        boolean enabled,
        ConditionGroupMode groupMode,
        ConditionNodeConfig config,
        List<ConditionNode> children
) {
    public ConditionNode {
        id = normalizeId(id);
        type = normalizeType(type);
        name = safe(name);
        note = safe(note);
        groupMode = groupMode == null ? ConditionGroupMode.AND : groupMode;
        config = config == null ? ConditionNodeConfig.EMPTY : config;
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static ConditionNode leaf(String id, String type) {
        return leaf(id, type, ConditionNodeConfig.EMPTY);
    }

    public static ConditionNode leaf(String id, String type, ConditionNodeConfig config) {
        return new ConditionNode(id, type, "", "", true, ConditionGroupMode.AND, config, List.of());
    }

    public static ConditionNode disabledLeaf(String id, String type) {
        return new ConditionNode(id, type, "", "", false, ConditionGroupMode.AND, ConditionNodeConfig.EMPTY, List.of());
    }

    public static ConditionNode group(String id, ConditionGroupMode mode, List<ConditionNode> children) {
        return new ConditionNode(id, ConditionNodeType.GROUP, "", "", true, mode, ConditionNodeConfig.EMPTY, children);
    }

    public static ConditionNode not(String id, ConditionNode child) {
        return group(id, ConditionGroupMode.NOT, child == null ? List.of() : List.of(child));
    }

    public ConditionNode normalized(String fallbackId) {
        String safeId = id.isBlank() ? normalizeId(fallbackId) : id;
        List<ConditionNode> safeChildren = children.stream()
                .map((child) -> child == null ? null : child.normalized(""))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ConditionNode(safeId, type, name, note, enabled, groupMode, config, safeChildren);
    }

    public boolean isGroup() {
        return ConditionNodeType.GROUP.equals(type);
    }

    private static String normalizeType(String value) {
        String type = safe(value).toLowerCase(Locale.ROOT);
        return type;
    }

    private static String normalizeId(String value) {
        String raw = safe(value).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length() && builder.length() < 80; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
