package com.zcpu.tzzmod.signal.device.item;

import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class ItemStackMatcher {
    private ItemStackMatcher() {
    }

    public static boolean matches(ItemStack stack, ItemStackMatcherData matcher) {
        return matches(stack, matcher, false);
    }

    public static boolean matchesIgnoringCount(ItemStack stack, ItemStackMatcherData matcher) {
        return matches(stack, matcher, true);
    }

    private static boolean matches(ItemStack stack, ItemStackMatcherData rawMatcher, boolean ignoreCount) {
        if (stack == null || stack.isEmpty() || rawMatcher == null) {
            return false;
        }
        ItemStackMatcherData matcher = rawMatcher.normalized();
        if (!matcher.enabled()) {
            return false;
        }
        if (matcher.matchItemId()) {
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (!itemId.equals(matcher.templateItemId())) {
                return false;
            }
        }
        if (!ignoreCount && !ContainerItemCountMode.IGNORE.id().equals(matcher.countMode())) {
            if (!ContainerItemCountMode.fromId(matcher.countMode()).matches(stack.getCount(), matcher.requiredCount())) {
                return false;
            }
        }
        if (matcher.matchDamage() && stack.getDamage() != matcher.templateDamage()) {
            return false;
        }
        if (matcher.matchCustomName()
                && !ItemStackMatcherSupport.customNameSnapshot(stack).equals(matcher.templateCustomName())) {
            return false;
        }
        if (matcher.matchLore()
                && !ItemStackMatcherSupport.loreSnapshot(stack).equals(matcher.templateLore())) {
            return false;
        }
        if (matcher.matchCustomData()
                && !ItemStackMatcherSupport.customDataSnapshot(stack).equals(matcher.templateCustomData())) {
            return false;
        }
        return !matcher.matchComponents()
                || ItemStackMatcherSupport.componentsSnapshot(stack).equals(matcher.templateComponents());
    }
}
