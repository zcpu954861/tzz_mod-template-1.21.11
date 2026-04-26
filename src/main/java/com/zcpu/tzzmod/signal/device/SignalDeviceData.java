package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SignalDeviceData(
        String id,
        String type,
        String name,
        String dimension,
        int x,
        int y,
        int z,
        String channel,
        boolean enabled,
        int pulseTicks,
        int remainingPulseTicks,
        int cooldownTicks,
        int actionCount,
        long createdWallTimeMillis,
        long updatedWallTimeMillis,
        long lastTriggerGameTime,
        long lastTriggerWallTimeMillis,
        String lastResult,
        String blockId,
        String offChannel,
        String mode,
        boolean lastPowered,
        int lastPowerLevel,
        boolean conditionEnabled,
        String conditionBlockId,
        Map<String, String> conditionProperties,
        String conditionRaw,
        String conditionMode,
        boolean lastConditionMatched,
        long lastConditionCheckGameTime,
        String lastConditionResult,
        boolean interactionEnabled,
        String interactChannel,
        int interactionCooldownTicks,
        long lastInteractionGameTime,
        long lastInteractionWallTimeMillis,
        String lastInteractionPlayerName,
        String lastInteractionPlayerUuid,
        String lastInteractionResult,
        String lastInteractionHand,
        String lastInteractionSide,
        boolean containerEnabled,
        String containerOpenChannel,
        String containerCloseChannel,
        String containerChangeChannel,
        int containerCooldownTicks,
        int containerChangeCheckIntervalTicks,
        long lastContainerCheckGameTime,
        String lastContainerFingerprint,
        long lastContainerOpenGameTime,
        long lastContainerOpenWallTimeMillis,
        long lastContainerCloseGameTime,
        long lastContainerCloseWallTimeMillis,
        long lastContainerChangeGameTime,
        long lastContainerChangeWallTimeMillis,
        String lastContainerPlayerName,
        String lastContainerPlayerUuid,
        String lastContainerResult,
        String lastContainerEventType,
        List<ContainerItemConditionData> itemConditions
) {
    public static final String TYPE_SIGNAL_EMITTER = "signal_emitter";
    public static final String TYPE_SIGNAL_RECEIVER = "signal_receiver";
    public static final String TYPE_ACTION_RELAY = "action_relay";
    public static final String TYPE_VIRTUAL_BLOCK_DEVICE = "virtual_block_device";
    public static final int DEFAULT_RECEIVER_PULSE_TICKS = 5;

    public SignalDeviceData(
            String id,
            String type,
            String name,
            String dimension,
            int x,
            int y,
            int z,
            String channel,
            boolean enabled,
            int pulseTicks,
            int remainingPulseTicks,
            int cooldownTicks,
            int actionCount,
            long createdWallTimeMillis,
            long updatedWallTimeMillis,
            long lastTriggerGameTime,
            long lastTriggerWallTimeMillis,
            String lastResult,
            String blockId,
            String offChannel,
            String mode,
            boolean lastPowered,
            int lastPowerLevel
    ) {
        this(
                id,
                type,
                name,
                dimension,
                x,
                y,
                z,
                channel,
                enabled,
                pulseTicks,
                remainingPulseTicks,
                cooldownTicks,
                actionCount,
                createdWallTimeMillis,
                updatedWallTimeMillis,
                lastTriggerGameTime,
                lastTriggerWallTimeMillis,
                lastResult,
                blockId,
                offChannel,
                mode,
                lastPowered,
                lastPowerLevel,
                false,
                "",
                Map.of(),
                "",
                BlockStateConditionMode.CONDITION_ENTER.id(),
                false,
                0L,
                "",
                false,
                "",
                0,
                0L,
                0L,
                "",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                0,
                10,
                0L,
                "",
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                "",
                "",
                "",
                "",
                List.of()
        );
    }

    public SignalDeviceData(
            String id,
            String type,
            String name,
            String dimension,
            int x,
            int y,
            int z,
            String channel,
            boolean enabled,
            int pulseTicks,
            int remainingPulseTicks,
            int cooldownTicks,
            int actionCount,
            long createdWallTimeMillis,
            long updatedWallTimeMillis,
            long lastTriggerGameTime,
            long lastTriggerWallTimeMillis,
            String lastResult,
            String blockId,
            String offChannel,
            String mode,
            boolean lastPowered,
            int lastPowerLevel,
            boolean conditionEnabled,
            String conditionBlockId,
            Map<String, String> conditionProperties,
            String conditionRaw,
            String conditionMode,
            boolean lastConditionMatched,
            long lastConditionCheckGameTime,
            String lastConditionResult,
            boolean interactionEnabled,
            String interactChannel,
            int interactionCooldownTicks,
            long lastInteractionGameTime,
            long lastInteractionWallTimeMillis,
            String lastInteractionPlayerName,
            String lastInteractionPlayerUuid,
            String lastInteractionResult,
            String lastInteractionHand,
            String lastInteractionSide
    ) {
        this(
                id,
                type,
                name,
                dimension,
                x,
                y,
                z,
                channel,
                enabled,
                pulseTicks,
                remainingPulseTicks,
                cooldownTicks,
                actionCount,
                createdWallTimeMillis,
                updatedWallTimeMillis,
                lastTriggerGameTime,
                lastTriggerWallTimeMillis,
                lastResult,
                blockId,
                offChannel,
                mode,
                lastPowered,
                lastPowerLevel,
                conditionEnabled,
                conditionBlockId,
                conditionProperties,
                conditionRaw,
                conditionMode,
                lastConditionMatched,
                lastConditionCheckGameTime,
                lastConditionResult,
                interactionEnabled,
                interactChannel,
                interactionCooldownTicks,
                lastInteractionGameTime,
                lastInteractionWallTimeMillis,
                lastInteractionPlayerName,
                lastInteractionPlayerUuid,
                lastInteractionResult,
                lastInteractionHand,
                lastInteractionSide,
                false,
                "",
                "",
                "",
                0,
                10,
                0L,
                "",
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                "",
                "",
                "",
                "",
                List.of()
        );
    }

    public SignalDeviceData normalized() {
        String cleanId = id == null ? "" : id.trim();
        String cleanType = type == null || type.isBlank() ? TYPE_SIGNAL_EMITTER : type.trim();
        String cleanName = name == null ? "" : name.trim();
        String cleanDimension = dimension == null ? "" : dimension.trim();
        String cleanLastResult = lastResult == null ? "" : lastResult.trim();
        String cleanBlockId = blockId == null ? "" : blockId.trim();
        String cleanOffChannel = SignalChannel.normalize(offChannel);
        String cleanMode = VirtualBlockDeviceMode.normalize(mode);
        String cleanConditionBlockId = conditionBlockId == null ? "" : conditionBlockId.trim();
        String cleanConditionRaw = conditionRaw == null ? "" : conditionRaw.trim();
        String cleanConditionMode = BlockStateConditionMode.normalize(conditionMode);
        String cleanConditionResult = lastConditionResult == null ? "" : lastConditionResult.trim();
        String cleanInteractChannel = SignalChannel.normalize(interactChannel);
        String cleanInteractionPlayerName = lastInteractionPlayerName == null ? "" : lastInteractionPlayerName.trim();
        String cleanInteractionPlayerUuid = lastInteractionPlayerUuid == null ? "" : lastInteractionPlayerUuid.trim();
        String cleanInteractionResult = lastInteractionResult == null ? "" : lastInteractionResult.trim();
        String cleanInteractionHand = lastInteractionHand == null ? "" : lastInteractionHand.trim();
        String cleanInteractionSide = lastInteractionSide == null ? "" : lastInteractionSide.trim();
        String cleanContainerOpenChannel = SignalChannel.normalize(containerOpenChannel);
        String cleanContainerCloseChannel = SignalChannel.normalize(containerCloseChannel);
        String cleanContainerChangeChannel = SignalChannel.normalize(containerChangeChannel);
        String cleanContainerFingerprint = lastContainerFingerprint == null ? "" : lastContainerFingerprint.trim();
        String cleanContainerPlayerName = lastContainerPlayerName == null ? "" : lastContainerPlayerName.trim();
        String cleanContainerPlayerUuid = lastContainerPlayerUuid == null ? "" : lastContainerPlayerUuid.trim();
        String cleanContainerResult = lastContainerResult == null ? "" : lastContainerResult.trim();
        String cleanContainerEventType = lastContainerEventType == null ? "" : lastContainerEventType.trim();
        List<ContainerItemConditionData> cleanItemConditions = new ArrayList<>();
        if (itemConditions != null) {
            for (ContainerItemConditionData condition : itemConditions) {
                if (condition == null) {
                    continue;
                }
                ContainerItemConditionData normalized = condition.normalized();
                if (!normalized.name().isBlank()) {
                    cleanItemConditions.add(normalized);
                }
            }
        }
        Map<String, String> cleanConditionProperties = new LinkedHashMap<>();
        if (conditionProperties != null) {
            for (Map.Entry<String, String> entry : conditionProperties.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    continue;
                }
                cleanConditionProperties.put(entry.getKey().trim(), entry.getValue().trim());
            }
        }
        boolean cleanConditionEnabled = conditionEnabled
                && !cleanConditionBlockId.isBlank()
                && !cleanConditionProperties.isEmpty();
        boolean cleanContainerEnabled = containerEnabled
                && (!cleanContainerOpenChannel.isBlank()
                || !cleanContainerCloseChannel.isBlank()
                || !cleanContainerChangeChannel.isBlank());
        int cleanPulseTicks = cleanType.equals(TYPE_SIGNAL_RECEIVER)
                ? Math.max(1, pulseTicks <= 0 ? DEFAULT_RECEIVER_PULSE_TICKS : pulseTicks)
                : Math.max(0, pulseTicks);
        return new SignalDeviceData(
                cleanId,
                cleanType,
                cleanName,
                cleanDimension,
                x,
                y,
                z,
                SignalChannel.normalize(channel),
                enabled,
                cleanPulseTicks,
                Math.max(0, remainingPulseTicks),
                Math.max(0, cooldownTicks),
                Math.max(0, actionCount),
                Math.max(0L, createdWallTimeMillis),
                Math.max(0L, updatedWallTimeMillis),
                Math.max(0L, lastTriggerGameTime),
                Math.max(0L, lastTriggerWallTimeMillis),
                cleanLastResult,
                cleanBlockId,
                cleanOffChannel,
                cleanMode,
                lastPowered,
                Math.max(0, Math.min(15, lastPowerLevel)),
                cleanConditionEnabled,
                cleanConditionBlockId,
                Map.copyOf(cleanConditionProperties),
                cleanConditionRaw,
                cleanConditionMode,
                lastConditionMatched,
                Math.max(0L, lastConditionCheckGameTime),
                cleanConditionResult,
                interactionEnabled && !cleanInteractChannel.isBlank(),
                cleanInteractChannel,
                Math.max(0, interactionCooldownTicks),
                Math.max(0L, lastInteractionGameTime),
                Math.max(0L, lastInteractionWallTimeMillis),
                cleanInteractionPlayerName,
                cleanInteractionPlayerUuid,
                cleanInteractionResult,
                cleanInteractionHand,
                cleanInteractionSide,
                cleanContainerEnabled,
                cleanContainerOpenChannel,
                cleanContainerCloseChannel,
                cleanContainerChangeChannel,
                Math.max(0, containerCooldownTicks),
                Math.max(1, containerChangeCheckIntervalTicks <= 0 ? 10 : containerChangeCheckIntervalTicks),
                Math.max(0L, lastContainerCheckGameTime),
                cleanContainerFingerprint,
                Math.max(0L, lastContainerOpenGameTime),
                Math.max(0L, lastContainerOpenWallTimeMillis),
                Math.max(0L, lastContainerCloseGameTime),
                Math.max(0L, lastContainerCloseWallTimeMillis),
                Math.max(0L, lastContainerChangeGameTime),
                Math.max(0L, lastContainerChangeWallTimeMillis),
                cleanContainerPlayerName,
                cleanContainerPlayerUuid,
                cleanContainerResult,
                cleanContainerEventType,
                List.copyOf(cleanItemConditions)
        );
    }
}
