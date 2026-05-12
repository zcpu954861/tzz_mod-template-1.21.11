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

    public static ItemStackMatcherData withInteractionSettingsFrom(
            ItemStackMatcherData template,
            ItemStackMatcherData previousSettings
    ) {
        ItemStackMatcherData data = template == null ? ItemStackMatcherData.empty() : template.normalized();
        ItemStackMatcherData previous = previousSettings == null ? ItemStackMatcherData.empty() : previousSettings.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                previous.successChannel(),
                previous.failChannel(),
                previous.successMessage(),
                previous.failMessage(),
                previous.successSoundId(),
                previous.successSoundVolume(),
                previous.successSoundPitch(),
                previous.failSoundId(),
                previous.failSoundVolume(),
                previous.failSoundPitch(),
                previous.consumeEnabled(),
                previous.consumeCount(),
                previous.interactionItemConsumeSource(),
                previous.interactionItemInventoryConsumeOrder(),
                previous.interactionItemSource(),
                previous.interactionItemVanillaPolicy(),
                "",
                -1,
                0,
                "",
                "",
                "",
                "",
                data.createdWallTimeMillis(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withSuccessChannel(ItemStackMatcherData matcher, String channel) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, channel, data.failChannel(), data.successMessage(), data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withFailChannel(ItemStackMatcherData matcher, String channel) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), channel, data.successMessage(), data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withSuccessMessage(ItemStackMatcherData matcher, String message) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), message, data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withFailMessage(ItemStackMatcherData matcher, String message) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), data.successMessage(), message,
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withSuccessSound(ItemStackMatcherData matcher, String soundId, float volume, float pitch) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), data.successMessage(), data.failMessage(),
                soundId, volume, pitch, data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withFailSound(ItemStackMatcherData matcher, String soundId, float volume, float pitch) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), data.successMessage(), data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(), soundId, volume, pitch,
                data.consumeEnabled(), data.consumeCount());
    }

    public static ItemStackMatcherData withConsume(ItemStackMatcherData matcher, boolean enabled) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), data.successMessage(), data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                enabled, data.consumeCount());
    }

    public static ItemStackMatcherData withConsumeCount(ItemStackMatcherData matcher, int count) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return copyFeedback(data, data.successChannel(), data.failChannel(), data.successMessage(), data.failMessage(),
                data.successSoundId(), data.successSoundVolume(), data.successSoundPitch(),
                data.failSoundId(), data.failSoundVolume(), data.failSoundPitch(),
                data.consumeEnabled(), Math.max(1, count));
    }

    public static ItemStackMatcherData withConsumeSource(ItemStackMatcherData matcher, String consumeSource) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                InteractionItemConsumeSource.normalize(consumeSource),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withInventoryConsumeOrder(ItemStackMatcherData matcher, String order) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                InventoryConsumeOrder.normalize(order),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withSource(ItemStackMatcherData matcher, String source) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                InteractionItemSource.normalize(source),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withVanillaPolicy(ItemStackMatcherData matcher, String policy) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                InteractionItemVanillaPolicy.normalize(policy),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withSourceResult(
            ItemStackMatcherData matcher,
            String source,
            int matchedSlot,
            int matchedCount,
            String result
    ) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                InteractionItemSource.normalize(source),
                matchedSlot,
                matchedCount,
                result == null ? "" : result,
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    public static ItemStackMatcherData withConsumeResult(
            ItemStackMatcherData matcher,
            String consumeSource,
            String consumedSlots,
            String result
    ) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                InteractionItemConsumeSource.normalize(consumeSource),
                consumedSlots == null ? "" : consumedSlots,
                result == null ? "" : result,
                data.createdWallTimeMillis(),
                now
        ).normalized();
    }

    private static ItemStackMatcherData copyFeedback(
            ItemStackMatcherData data,
            String successChannel,
            String failChannel,
            String successMessage,
            String failMessage,
            String successSoundId,
            float successSoundVolume,
            float successSoundPitch,
            String failSoundId,
            float failSoundVolume,
            float failSoundPitch,
            boolean consumeEnabled,
            int consumeCount
    ) {
        long now = System.currentTimeMillis();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
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
                data.templateDisplayStack(),
                data.templateSummary(),
                successChannel,
                failChannel,
                successMessage,
                failMessage,
                successSoundId,
                successSoundVolume,
                successSoundPitch,
                failSoundId,
                failSoundVolume,
                failSoundPitch,
                consumeEnabled,
                consumeCount,
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
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
        if (ContainerItemCountMode.IGNORE.id().equals(matcher.countMode())) {
            builder.append("（不检查数量）");
        } else {
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

    public static String countRequirementText(ItemStackMatcherData rawMatcher) {
        ItemStackMatcherData matcher = rawMatcher == null ? ItemStackMatcherData.empty() : rawMatcher.normalized();
        if (ContainerItemCountMode.IGNORE.id().equals(matcher.countMode())) {
            return "不检查数量";
        }
        return Integer.toString(matcher.requiredCount());
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
