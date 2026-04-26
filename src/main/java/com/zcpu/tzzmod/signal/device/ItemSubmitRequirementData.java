package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;

public record ItemSubmitRequirementData(
        String id,
        String name,
        boolean enabled,
        ItemStackMatcherData matcher,
        int consumeCount,
        boolean lastMatched,
        int lastMatchedCount,
        long lastCheckGameTime,
        String lastResult
) {
    public ItemSubmitRequirementData normalized() {
        String cleanName = name == null ? "" : name.trim();
        String cleanId = id == null || id.isBlank() ? generatedId(cleanName) : id.trim();
        ItemStackMatcherData cleanMatcher = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        int cleanConsumeCount = Math.max(1, consumeCount);
        String cleanResult = lastResult == null ? "" : lastResult.trim();
        return new ItemSubmitRequirementData(
                cleanId,
                cleanName,
                enabled,
                cleanMatcher,
                cleanConsumeCount,
                lastMatched,
                Math.max(0, lastMatchedCount),
                Math.max(0L, lastCheckGameTime),
                cleanResult
        );
    }

    public ItemSubmitRequirementData withMatcher(ItemStackMatcherData newMatcher, int newConsumeCount) {
        ItemSubmitRequirementData data = normalized();
        return new ItemSubmitRequirementData(
                data.id(),
                data.name(),
                data.enabled(),
                newMatcher == null ? ItemStackMatcherData.empty() : newMatcher.normalized(),
                Math.max(1, newConsumeCount),
                data.lastMatched(),
                data.lastMatchedCount(),
                data.lastCheckGameTime(),
                data.lastResult()
        ).normalized();
    }

    public ItemSubmitRequirementData withEnabled(boolean newEnabled, boolean matched, int matchedCount, long gameTime, String result) {
        ItemSubmitRequirementData data = normalized();
        return new ItemSubmitRequirementData(
                data.id(),
                data.name(),
                newEnabled,
                data.matcher(),
                data.consumeCount(),
                matched,
                Math.max(0, matchedCount),
                Math.max(0L, gameTime),
                result == null ? "" : result.trim()
        ).normalized();
    }

    public ItemSubmitRequirementData withResult(boolean matched, int matchedCount, long gameTime, String result) {
        return withEnabled(enabled, matched, matchedCount, gameTime, result);
    }

    private static String generatedId(String name) {
        String base = name == null || name.isBlank() ? "item_submit" : name.trim();
        return Integer.toHexString(base.hashCode());
    }
}
