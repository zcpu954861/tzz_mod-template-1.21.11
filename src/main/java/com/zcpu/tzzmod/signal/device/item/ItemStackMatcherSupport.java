package com.zcpu.tzzmod.signal.device.item;

import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public final class ItemStackMatcherSupport {
    private ItemStackMatcherSupport() {
    }

    public static ItemStackMatcherData captureTemplate(
            ItemStack stack,
            ContainerItemCountMode countMode,
            int requiredCount
    ) {
        if (stack == null || stack.isEmpty()) {
            return ItemStackMatcherData.empty();
        }
        long now = System.currentTimeMillis();
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String customName = customNameSnapshot(stack);
        List<String> lore = loreSnapshot(stack);
        String customData = customDataSnapshot(stack);
        String components = componentsSnapshot(stack);
        String mode = countMode == null ? ContainerItemCountMode.AT_LEAST.id() : countMode.id();
        int count = ContainerItemCountMode.IGNORE.id().equals(mode) ? 0 : Math.max(1, requiredCount);
        return new ItemStackMatcherData(
                true,
                itemId,
                stack.getCount(),
                mode,
                count,
                true,
                false,
                false,
                false,
                false,
                false,
                stack.getDamage(),
                customName,
                lore,
                customData,
                components,
                summary(itemId, stack.getCount(), stack.getDamage(), customName, lore, customData, components),
                now,
                now
        ).normalized();
    }

    public static ItemStackMatcherData withOption(ItemStackMatcherData matcher, String option, boolean enabled) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
                data.matchItemId(),
                "matchDamage".equals(option) ? enabled : data.matchDamage(),
                "matchCustomName".equals(option) ? enabled : data.matchCustomName(),
                "matchLore".equals(option) ? enabled : data.matchLore(),
                "matchCustomData".equals(option) ? enabled : data.matchCustomData(),
                "matchComponents".equals(option) ? enabled : data.matchComponents(),
                data.templateDamage(),
                data.templateCustomName(),
                data.templateLore(),
                data.templateCustomData(),
                data.templateComponents(),
                data.templateSummary(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withCount(ItemStackMatcherData matcher, ContainerItemCountMode mode, int count) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        String cleanMode = mode == null ? ContainerItemCountMode.AT_LEAST.id() : mode.id();
        int cleanCount = ContainerItemCountMode.IGNORE.id().equals(cleanMode) ? 0 : Math.max(1, count);
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                cleanMode,
                cleanCount,
                data.matchItemId(),
                data.matchDamage(),
                data.matchCustomName(),
                data.matchLore(),
                data.matchCustomData(),
                data.matchComponents(),
                data.templateDamage(),
                data.templateCustomName(),
                data.templateLore(),
                data.templateCustomData(),
                data.templateComponents(),
                data.templateSummary(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static String customNameSnapshot(ItemStack stack) {
        Text name = stack == null ? null : stack.get(DataComponentTypes.CUSTOM_NAME);
        return name == null ? "" : name.getString();
    }

    public static List<String> loreSnapshot(ItemStack stack) {
        LoreComponent lore = stack == null ? null : stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return List.of();
        }
        return lore.lines().stream().map(Text::getString).toList();
    }

    public static String customDataSnapshot(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return customData.isEmpty() ? "" : customData.copyNbt().toString();
    }

    public static String componentsSnapshot(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        return stack.getComponentChanges()
                .withRemovedIf(type -> type == DataComponentTypes.DAMAGE)
                .toString();
    }

    public static String summary(ItemStackMatcherData rawMatcher) {
        if (rawMatcher == null || !rawMatcher.normalized().enabled()) {
            return "未配置模板";
        }
        ItemStackMatcherData matcher = rawMatcher.normalized();
        StringBuilder builder = new StringBuilder(matcher.templateItemId());
        builder.append(" count ").append(matcher.countMode());
        if (!ContainerItemCountMode.IGNORE.id().equals(matcher.countMode())) {
            builder.append(" ").append(matcher.requiredCount());
        }
        if (matcher.matchDamage()) {
            builder.append(", damage=").append(matcher.templateDamage());
        }
        if (matcher.matchCustomName()) {
            builder.append(", customName");
        }
        if (matcher.matchLore()) {
            builder.append(", lore");
        }
        if (matcher.matchCustomData()) {
            builder.append(", customData");
        }
        if (matcher.matchComponents()) {
            builder.append(", components");
        }
        return builder.toString();
    }

    private static String summary(
            String itemId,
            int count,
            int damage,
            String customName,
            List<String> lore,
            String customData,
            String components
    ) {
        StringBuilder builder = new StringBuilder(itemId).append(" x").append(count);
        if (damage > 0) {
            builder.append(", damage=").append(damage);
        }
        if (!customName.isBlank()) {
            builder.append(", customName");
        }
        if (!lore.isEmpty()) {
            builder.append(", lore=").append(lore.size());
        }
        if (!customData.isBlank()) {
            builder.append(", customData");
        }
        if (!components.isBlank() && !"{}".equals(components)) {
            builder.append(", components");
        }
        return builder.toString();
    }
}
