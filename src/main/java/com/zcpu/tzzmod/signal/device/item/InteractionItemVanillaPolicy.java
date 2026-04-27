package com.zcpu.tzzmod.signal.device.item;

import java.util.Locale;

public final class InteractionItemVanillaPolicy {
    public static final String ALLOW = "allow";
    public static final String REQUIRE_ITEM_MATCH = "require_item_match";

    private InteractionItemVanillaPolicy() {
    }

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return REQUIRE_ITEM_MATCH.equals(value) ? REQUIRE_ITEM_MATCH : ALLOW;
    }

    public static boolean blocksVanillaOnFailure(String raw) {
        return REQUIRE_ITEM_MATCH.equals(normalize(raw));
    }

    public static String displayName(String raw) {
        return switch (normalize(raw)) {
            case REQUIRE_ITEM_MATCH -> "需要物品匹配才允许原版交互（require_item_match）";
            default -> "允许原版交互（allow）";
        };
    }
}
