package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.Locale;

public record ContainerItemConditionData(
        String id,
        String name,
        boolean enabled,
        String type,
        int slot,
        String itemId,
        String countMode,
        int count,
        String channel,
        String offChannel,
        String mode,
        boolean lastMatched,
        long lastCheckGameTime,
        long lastTriggerGameTime,
        long lastTriggerWallTimeMillis,
        String lastResult
) {
    public ContainerItemConditionData normalized() {
        String cleanName = name == null ? "" : name.trim();
        String cleanId = id == null || id.isBlank()
                ? generatedId(cleanName)
                : id.trim();
        String cleanType = ContainerItemConditionType.normalize(type);
        String cleanItemId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        String cleanCountMode = ContainerItemCountMode.normalize(countMode);
        String cleanChannel = SignalChannel.normalize(channel);
        String cleanOffChannel = SignalChannel.normalize(offChannel);
        String cleanMode = BlockStateConditionMode.normalize(mode);
        String cleanResult = lastResult == null ? "" : lastResult.trim();
        int cleanSlot = Math.max(0, slot);
        int cleanCount = Math.max(0, count);
        if (ContainerItemConditionType.SLOT_EMPTY.id().equals(cleanType)) {
            cleanItemId = "";
            cleanCountMode = ContainerItemCountMode.AT_LEAST.id();
            cleanCount = 0;
        } else {
            cleanCount = Math.max(1, cleanCount);
        }
        return new ContainerItemConditionData(
                cleanId,
                cleanName,
                enabled,
                cleanType,
                cleanSlot,
                cleanItemId,
                cleanCountMode,
                cleanCount,
                cleanChannel,
                cleanOffChannel,
                cleanMode,
                lastMatched,
                Math.max(0L, lastCheckGameTime),
                Math.max(0L, lastTriggerGameTime),
                Math.max(0L, lastTriggerWallTimeMillis),
                cleanResult
        );
    }

    public ContainerItemConditionData withMatched(boolean matched, long gameTime, String result) {
        ContainerItemConditionData normalized = normalized();
        return new ContainerItemConditionData(
                normalized.id(),
                normalized.name(),
                normalized.enabled(),
                normalized.type(),
                normalized.slot(),
                normalized.itemId(),
                normalized.countMode(),
                normalized.count(),
                normalized.channel(),
                normalized.offChannel(),
                normalized.mode(),
                matched,
                gameTime,
                normalized.lastTriggerGameTime(),
                normalized.lastTriggerWallTimeMillis(),
                result == null ? "" : result.trim()
        ).normalized();
    }

    public ContainerItemConditionData withTriggered(boolean matched, long gameTime, long wallTimeMillis, String result) {
        ContainerItemConditionData normalized = normalized();
        return new ContainerItemConditionData(
                normalized.id(),
                normalized.name(),
                normalized.enabled(),
                normalized.type(),
                normalized.slot(),
                normalized.itemId(),
                normalized.countMode(),
                normalized.count(),
                normalized.channel(),
                normalized.offChannel(),
                normalized.mode(),
                matched,
                gameTime,
                gameTime,
                wallTimeMillis,
                result == null ? "" : result.trim()
        ).normalized();
    }

    private static String generatedId(String name) {
        String base = name == null || name.isBlank() ? "item_condition" : name.trim();
        return Integer.toHexString(base.hashCode());
    }
}
