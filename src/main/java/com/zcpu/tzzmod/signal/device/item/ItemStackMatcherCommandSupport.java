package com.zcpu.tzzmod.signal.device.item;

import java.util.Set;

public final class ItemStackMatcherCommandSupport {
    private static final Set<String> OPTIONS = Set.of(
            "matchDamage",
            "matchCustomName",
            "matchLore",
            "matchCustomData",
            "matchComponents"
    );

    private ItemStackMatcherCommandSupport() {
    }

    public static boolean isOption(String option) {
        return option != null && OPTIONS.contains(option);
    }

    public static ItemStackMatcherData withOption(ItemStackMatcherData matcher, String option, boolean enabled) {
        if (!isOption(option)) {
            return matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        }
        return ItemStackMatcherSupport.withOption(matcher, option, enabled);
    }
}
