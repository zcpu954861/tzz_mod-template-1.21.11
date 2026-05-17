package com.zcpu.tzzmod.condition.runtime;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ConditionGroupCompatibilityProfile(
        ConditionRuntimeTargetType targetType,
        String displayName,
        boolean playerContext,
        boolean globalStateVariables,
        boolean playerStateVariables,
        Set<String> contextFields,
        Set<String> eventMetadataKeys,
        Set<String> itemKeys,
        Set<String> inventoryKeys,
        Set<String> containerKeys,
        Set<String> regionKeys,
        Set<String> signalChannelKeys,
        Set<String> signalHistoryKeys,
        Set<String> logicChainKeys
) {
    public ConditionGroupCompatibilityProfile {
        displayName = safe(displayName);
        contextFields = copy(contextFields);
        eventMetadataKeys = copy(eventMetadataKeys);
        itemKeys = copy(itemKeys);
        inventoryKeys = copy(inventoryKeys);
        containerKeys = copy(containerKeys);
        regionKeys = copy(regionKeys);
        signalChannelKeys = copy(signalChannelKeys);
        signalHistoryKeys = copy(signalHistoryKeys);
        logicChainKeys = copy(logicChainKeys);
    }

    public static ConditionGroupCompatibilityProfile forTarget(ConditionRuntimeTargetType targetType) {
        return forTarget(targetType, false);
    }

    public static ConditionGroupCompatibilityProfile forTarget(
            ConditionRuntimeTargetType targetType,
            boolean containerSnapshotForOpenClose
    ) {
        ConditionRuntimeTargetType type = targetType == null ? ConditionRuntimeTargetType.VBD_INTERACTION : targetType;
        Set<String> base = set(
                "sourceType",
                "sourceId",
                "deviceId",
                "worldId",
                "world",
                "channel",
                "blockPos",
                "triggerType",
                "detail",
                "gameTime",
                "signalDepth"
        );
        return switch (type) {
            case VBD_REDSTONE -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    base,
                    set("trigger", "detail"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case VBD_BLOCKSTATE -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    base,
                    set("trigger", "detail"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case VBD_INTERACTION -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    true,
                    true,
                    true,
                    withPlayer(base),
                    set("trigger", "detail", "side", "hand"),
                    set("main_hand", "off_hand", "held_item"),
                    set("player_inventory"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case ITEM_SUBMIT -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    true,
                    true,
                    true,
                    withPlayer(base),
                    set("trigger", "detail", "side", "hand", "itemSubmit"),
                    set("submitted_item", "main_hand", "off_hand", "held_item"),
                    set("player_inventory"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case CONTAINER_OPEN, CONTAINER_CLOSE -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    true,
                    true,
                    true,
                    withPlayer(base),
                    set("trigger", "detail", "container"),
                    Set.of(),
                    Set.of(),
                    containerSnapshotForOpenClose ? set("container") : Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case CONTAINER_CHANGE -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    base,
                    set("trigger", "detail", "container"),
                    Set.of(),
                    Set.of(),
                    set("container"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case SIGNAL_LISTENER -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "channel",
                            "listenerId",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "signalDepth"
                    ),
                    set("trigger", "detail"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case ACTION_RELAY -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "channel",
                            "deviceId",
                            "relayId",
                            "blockPos",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "signalDepth"
                    ),
                    set("trigger", "detail"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case REGION_ENTER, REGION_EXIT, REGION_STAY -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    true,
                    true,
                    true,
                    withPlayer(set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "regionId",
                            "triggerType",
                            "detail",
                            "gameTime"
                    )),
                    set("trigger", "detail", "actionBucket"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    set("region", "current_region"),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case SIGNAL_LISTENER_ACTION -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    withAction(set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "channel",
                            "listenerId",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "signalDepth"
                    )),
                    withActionMetadata(set("trigger", "detail")),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case ACTION_RELAY_ACTION -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    withAction(set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "channel",
                            "deviceId",
                            "relayId",
                            "blockPos",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "signalDepth"
                    )),
                    withActionMetadata(set("trigger", "detail")),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case REGION_ENTER_ACTION, REGION_EXIT_ACTION, REGION_STAY_ACTION -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    true,
                    true,
                    true,
                    withAction(withPlayer(set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "regionId",
                            "triggerType",
                            "detail",
                            "gameTime"
                    ))),
                    withActionMetadata(set("trigger", "detail", "actionBucket")),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    set("region", "current_region"),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case TIMER_ON_START, TIMER_ON_TICK, TIMER_ON_COMPLETE, TIMER_ON_CANCEL -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "timerId",
                            "timerMode",
                            "timerScopeMode",
                            "timerScopeKey",
                            "timerRunCount",
                            "timerRemainingTicks"
                    ),
                    set("trigger", "detail", "timerId", "timerMode", "timerScopeMode", "timerScopeKey", "timerRunCount", "timerRemainingTicks"),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
            case TIMER_ON_START_ACTION, TIMER_ON_TICK_ACTION, TIMER_ON_COMPLETE_ACTION, TIMER_ON_CANCEL_ACTION -> new ConditionGroupCompatibilityProfile(
                    type,
                    type.displayName(),
                    false,
                    true,
                    true,
                    withAction(set(
                            "sourceType",
                            "sourceId",
                            "worldId",
                            "world",
                            "triggerType",
                            "detail",
                            "gameTime",
                            "timerId",
                            "timerMode",
                            "timerScopeMode",
                            "timerScopeKey",
                            "timerRunCount",
                            "timerRemainingTicks"
                    )),
                    withActionMetadata(set("trigger", "detail", "timerId", "timerMode", "timerScopeMode", "timerScopeKey", "timerRunCount", "timerRemainingTicks")),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
        };
    }

    public boolean hasContextField(String field) {
        String key = safe(field);
        if (key.isBlank()) {
            return false;
        }
        if (key.startsWith("event.")) {
            return eventMetadataKeys.contains(key.substring("event.".length()));
        }
        if (key.startsWith("state.global.")) {
            return globalStateVariables;
        }
        if (key.startsWith("state.player.")) {
            return playerContext && playerStateVariables;
        }
        return contextFields.contains(key);
    }

    public Map<String, Object> summary() {
        java.util.LinkedHashMap<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("targetType", targetType.id());
        summary.put("displayName", displayName);
        summary.put("playerContext", playerContext);
        summary.put("globalStateVariables", globalStateVariables);
        summary.put("playerStateVariables", playerStateVariables);
        summary.put("contextFields", List.copyOf(contextFields));
        summary.put("eventMetadataKeys", List.copyOf(eventMetadataKeys));
        summary.put("itemKeys", List.copyOf(itemKeys));
        summary.put("inventoryKeys", List.copyOf(inventoryKeys));
        summary.put("containerKeys", List.copyOf(containerKeys));
        summary.put("regionKeys", List.copyOf(regionKeys));
        summary.put("signalChannelKeys", List.copyOf(signalChannelKeys));
        summary.put("signalHistoryKeys", List.copyOf(signalHistoryKeys));
        summary.put("logicChainKeys", List.copyOf(logicChainKeys));
        return Map.copyOf(summary);
    }

    private static Set<String> withPlayer(Set<String> base) {
        LinkedHashSet<String> copy = new LinkedHashSet<>(base);
        copy.addAll(set(
                "playerId",
                "playerName",
                "playerOnline",
                "playerOp",
                "playerIsOp",
                "playerTags",
                "playerTeam",
                "team",
                "playerGameMode",
                "playerGamemode",
                "gamemode",
                "playerAlive",
                "playerDead"
        ));
        return Set.copyOf(copy);
    }

    private static Set<String> withAction(Set<String> base) {
        LinkedHashSet<String> copy = new LinkedHashSet<>(base);
        copy.addAll(set(
                "actionId",
                "actionIndex",
                "actionDisplayIndex",
                "actionType",
                "parentTargetType",
                "parentTargetId",
                "parentActionBucket"
        ));
        return Set.copyOf(copy);
    }

    private static Set<String> withActionMetadata(Set<String> base) {
        LinkedHashSet<String> copy = new LinkedHashSet<>(base);
        copy.addAll(set(
                "actionId",
                "actionIndex",
                "actionDisplayIndex",
                "actionType",
                "parentTargetType",
                "parentTargetId",
                "parentActionBucket"
        ));
        return Set.copyOf(copy);
    }

    private static Set<String> set(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String safeValue = safe(value);
                if (!safeValue.isBlank()) {
                    result.add(safeValue);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> copy(Collection<String> raw) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (raw != null) {
            for (String value : raw) {
                String safeValue = safe(value);
                if (!safeValue.isBlank()) {
                    result.add(safeValue);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static String lower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }
}
