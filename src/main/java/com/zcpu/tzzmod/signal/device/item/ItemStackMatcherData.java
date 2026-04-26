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
        long createdWallTimeMillis,
        long updatedWallTimeMillis
) {
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
                Math.max(0L, createdWallTimeMillis),
                Math.max(0L, updatedWallTimeMillis)
        );
    }

    public boolean configured() {
        return normalized().enabled();
    }
}
