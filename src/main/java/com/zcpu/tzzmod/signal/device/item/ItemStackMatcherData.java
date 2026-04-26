package com.zcpu.tzzmod.signal.device.item;

import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import java.util.ArrayList;
import java.util.List;

public record ItemStackMatcherData(
        boolean enabled,
        String templateItemId,
        int templateCount,
        String countMode,
        int requiredCount,
        boolean matchItemId,
        boolean matchDamage,
        boolean matchCustomName,
        boolean matchLore,
        boolean matchCustomData,
        boolean matchComponents,
        int templateDamage,
        String templateCustomName,
        List<String> templateLore,
        String templateCustomData,
        String templateComponents,
        String templateSummary,
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
        int consumeCount,
        String interactionItemSource,
        String interactionItemVanillaPolicy,
        String lastInteractionItemSource,
        int lastInteractionItemMatchedSlot,
        int lastInteractionItemMatchedCount,
        String lastInteractionItemSourceResult,
        long createdWallTimeMillis,
        long updatedWallTimeMillis
) {
    public ItemStackMatcherData(
            boolean enabled,
            String templateItemId,
            int templateCount,
            String countMode,
            int requiredCount,
            boolean matchItemId,
            boolean matchDamage,
            boolean matchCustomName,
            boolean matchLore,
            boolean matchCustomData,
            boolean matchComponents,
            int templateDamage,
            String templateCustomName,
            List<String> templateLore,
            String templateCustomData,
            String templateComponents,
            String templateSummary,
            long createdWallTimeMillis,
            long updatedWallTimeMillis
    ) {
        this(
                enabled,
                templateItemId,
                templateCount,
                countMode,
                requiredCount,
                matchItemId,
                matchDamage,
                matchCustomName,
                matchLore,
                matchCustomData,
                matchComponents,
                templateDamage,
                templateCustomName,
                templateLore,
                templateCustomData,
                templateComponents,
                templateSummary,
                "",
                "",
                "",
                "",
                "",
                1.0F,
                1.0F,
                "",
                1.0F,
                1.0F,
                false,
                1,
                InteractionItemSource.MAIN_HAND,
                InteractionItemVanillaPolicy.ALLOW,
                "",
                -1,
                0,
                "",
                createdWallTimeMillis,
                updatedWallTimeMillis
        );
    }

    public static ItemStackMatcherData empty() {
        return new ItemStackMatcherData(
                false,
                "",
                0,
                ContainerItemCountMode.IGNORE.id(),
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                1.0F,
                1.0F,
                "",
                1.0F,
                1.0F,
                false,
                1,
                InteractionItemSource.MAIN_HAND,
                InteractionItemVanillaPolicy.ALLOW,
                "",
                -1,
                0,
                "",
                0L,
                0L
        );
    }

    public ItemStackMatcherData normalized() {
        String cleanItemId = templateItemId == null ? "" : templateItemId.trim().toLowerCase();
        String cleanCountMode = ContainerItemCountMode.normalize(countMode);
        int cleanRequiredCount = ContainerItemCountMode.IGNORE.id().equals(cleanCountMode)
                ? 0
                : Math.max(1, requiredCount);
        String cleanCustomName = templateCustomName == null ? "" : templateCustomName.trim();
        String cleanCustomData = templateCustomData == null ? "" : templateCustomData.trim();
        String cleanComponents = templateComponents == null ? "" : templateComponents.trim();
        String cleanSummary = templateSummary == null ? "" : templateSummary.trim();
        String cleanSuccessChannel = successChannel == null ? "" : successChannel.trim().toLowerCase();
        String cleanFailChannel = failChannel == null ? "" : failChannel.trim().toLowerCase();
        String cleanSuccessMessage = successMessage == null ? "" : successMessage.trim();
        String cleanFailMessage = failMessage == null ? "" : failMessage.trim();
        String cleanSuccessSoundId = successSoundId == null ? "" : successSoundId.trim().toLowerCase();
        String cleanFailSoundId = failSoundId == null ? "" : failSoundId.trim().toLowerCase();
        String cleanInteractionItemSource = InteractionItemSource.normalize(interactionItemSource);
        String cleanInteractionItemVanillaPolicy = InteractionItemVanillaPolicy.normalize(interactionItemVanillaPolicy);
        String cleanLastInteractionItemSource = lastInteractionItemSource == null ? "" : lastInteractionItemSource.trim().toLowerCase();
        String cleanLastInteractionItemSourceResult = lastInteractionItemSourceResult == null ? "" : lastInteractionItemSourceResult.trim();
        List<String> cleanLore = new ArrayList<>();
        if (templateLore != null) {
            for (String line : templateLore) {
                if (line != null) {
                    cleanLore.add(line);
                }
            }
        }
        boolean configured = enabled && !cleanItemId.isBlank();
        return new ItemStackMatcherData(
                configured,
                cleanItemId,
                Math.max(0, templateCount),
                cleanCountMode,
                cleanRequiredCount,
                matchItemId,
                matchDamage,
                matchCustomName,
                matchLore,
                matchCustomData,
                matchComponents,
                Math.max(0, templateDamage),
                cleanCustomName,
                List.copyOf(cleanLore),
                cleanCustomData,
                cleanComponents,
                cleanSummary,
                cleanSuccessChannel,
                cleanFailChannel,
                cleanSuccessMessage,
                cleanFailMessage,
                cleanSuccessSoundId,
                clamp(successSoundVolume, 0.0F, 10.0F, 1.0F),
                clamp(successSoundPitch, 0.0F, 2.0F, 1.0F),
                cleanFailSoundId,
                clamp(failSoundVolume, 0.0F, 10.0F, 1.0F),
                clamp(failSoundPitch, 0.0F, 2.0F, 1.0F),
                consumeEnabled,
                Math.max(1, consumeCount),
                cleanInteractionItemSource,
                cleanInteractionItemVanillaPolicy,
                cleanLastInteractionItemSource,
                Math.max(-1, lastInteractionItemMatchedSlot),
                Math.max(0, lastInteractionItemMatchedCount),
                cleanLastInteractionItemSourceResult,
                Math.max(0L, createdWallTimeMillis),
                Math.max(0L, updatedWallTimeMillis)
        );
    }

    public boolean configured() {
        return normalized().enabled();
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
