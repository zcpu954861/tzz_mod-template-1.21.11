package com.zcpu.tzzmod.condition;

import com.zcpu.tzzmod.condition.item.ConditionContainerSnapshot;
import com.zcpu.tzzmod.condition.item.ConditionInventorySnapshot;
import com.zcpu.tzzmod.condition.item.ConditionItemStackSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionLogicChainSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionRegionSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionSignalChannelSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionSignalHistorySnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ConditionEvaluationContext(
        String playerId,
        String playerName,
        Boolean playerOnline,
        Boolean playerOp,
        List<String> playerTags,
        String playerTeam,
        String playerGameMode,
        Boolean playerAlive,
        String worldId,
        String sourceType,
        String sourceId,
        String channel,
        String deviceId,
        String listenerId,
        String regionId,
        String actionId,
        String blockPos,
        String itemStackSummary,
        String triggerType,
        String detail,
        long gameTime,
        int signalDepth,
        StateVariableSnapshot stateVariables,
        Map<String, ConditionItemStackSnapshot> itemSnapshots,
        Map<String, ConditionInventorySnapshot> inventorySnapshots,
        Map<String, ConditionContainerSnapshot> containerSnapshots,
        Map<String, ConditionRegionSnapshot> regionSnapshots,
        Map<String, ConditionSignalChannelSnapshot> signalChannelSnapshots,
        Map<String, ConditionSignalHistorySnapshot> signalHistorySnapshots,
        Map<String, ConditionLogicChainSnapshot> logicChainSnapshots,
        Map<String, String> eventMetadata,
        Map<String, String> variables
) {
    public ConditionEvaluationContext {
        playerId = safe(playerId);
        playerName = safe(playerName);
        playerTags = copyList(playerTags);
        playerTeam = safe(playerTeam);
        playerGameMode = safe(playerGameMode).toLowerCase(java.util.Locale.ROOT);
        worldId = safe(worldId);
        sourceType = safe(sourceType);
        sourceId = safe(sourceId);
        channel = safe(channel);
        deviceId = safe(deviceId);
        listenerId = safe(listenerId);
        regionId = safe(regionId);
        actionId = safe(actionId);
        blockPos = safe(blockPos);
        itemStackSummary = safe(itemStackSummary);
        triggerType = safe(triggerType);
        detail = safe(detail);
        signalDepth = Math.max(0, signalDepth);
        stateVariables = stateVariables == null ? StateVariableSnapshot.empty() : stateVariables;
        itemSnapshots = copySnapshotMap(itemSnapshots);
        inventorySnapshots = copySnapshotMap(inventorySnapshots);
        containerSnapshots = copySnapshotMap(containerSnapshots);
        regionSnapshots = copySnapshotMap(regionSnapshots);
        signalChannelSnapshots = copySnapshotMap(signalChannelSnapshots);
        signalHistorySnapshots = copySnapshotMap(signalHistorySnapshots);
        logicChainSnapshots = copySnapshotMap(logicChainSnapshots);
        eventMetadata = copy(eventMetadata);
        variables = copy(variables);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String fieldValue(String field) {
        String key = safe(field);
        if (key.startsWith("variables.")) {
            return variables.getOrDefault(key.substring("variables.".length()), "");
        }
        if (key.startsWith("event.")) {
            return eventMetadata.getOrDefault(key.substring("event.".length()), "");
        }
        if (key.startsWith("state.global.")) {
            String variableKey = key.substring("state.global.".length());
            return stateVariables.get(StateVariableScope.GLOBAL, "global", variableKey)
                    .map(StateVariableRecord::value)
                    .orElse("");
        }
        if (key.startsWith("state.player.") && hasPlayerIdentity()) {
            String variableKey = key.substring("state.player.".length());
            String targetId = !playerId.isBlank() ? playerId : playerName;
            return stateVariables.get(StateVariableScope.PLAYER, targetId, variableKey)
                    .map(StateVariableRecord::value)
                    .orElse("");
        }
        return switch (key) {
            case "playerId" -> playerId;
            case "playerName" -> playerName;
            case "playerOnline" -> playerOnline == null ? "" : playerOnline.toString();
            case "playerOp", "playerIsOp" -> playerOp == null ? "" : playerOp.toString();
            case "playerTags" -> String.join(",", playerTags);
            case "playerTeam", "team" -> playerTeam;
            case "playerGameMode", "playerGamemode", "gamemode" -> playerGameMode;
            case "playerAlive" -> playerAlive == null ? "" : playerAlive.toString();
            case "playerDead" -> playerAlive == null ? "" : Boolean.toString(!playerAlive);
            case "worldId", "world" -> worldId;
            case "sourceType" -> sourceType;
            case "sourceId" -> sourceId;
            case "channel" -> channel;
            case "deviceId" -> deviceId;
            case "listenerId" -> listenerId;
            case "regionId" -> regionId;
            case "actionId" -> actionId;
            case "blockPos" -> blockPos;
            case "itemStack", "itemStackSummary" -> itemStackSummary;
            case "triggerType" -> triggerType;
            case "detail" -> detail;
            case "gameTime" -> Long.toString(gameTime);
            case "signalDepth" -> Integer.toString(signalDepth);
            default -> variables.getOrDefault(key, "");
        };
    }

    public Map<String, String> summary() {
        Map<String, String> summary = new LinkedHashMap<>();
        put(summary, "player", playerName.isBlank() ? playerId : playerName);
        put(summary, "playerTeam", playerTeam);
        put(summary, "playerGameMode", playerGameMode);
        put(summary, "world", worldId);
        put(summary, "channel", channel);
        put(summary, "sourceType", sourceType);
        put(summary, "sourceId", sourceId);
        put(summary, "deviceId", deviceId);
        put(summary, "listenerId", listenerId);
        put(summary, "regionId", regionId);
        put(summary, "actionId", actionId);
        put(summary, "actionIndex", variables.getOrDefault("actionIndex", ""));
        put(summary, "actionType", variables.getOrDefault("actionType", ""));
        put(summary, "parentTargetType", variables.getOrDefault("parentTargetType", ""));
        put(summary, "parentTargetId", variables.getOrDefault("parentTargetId", ""));
        put(summary, "parentActionBucket", variables.getOrDefault("parentActionBucket", ""));
        put(summary, "blockPos", blockPos);
        put(summary, "triggerType", triggerType);
        put(summary, "gameTime", Long.toString(gameTime));
        if (stateVariables.size() > 0) {
            put(summary, "stateVariables", stateVariables.summary());
        }
        if (!itemSnapshots.isEmpty()) {
            put(summary, "itemSnapshots", Integer.toString(itemSnapshots.size()));
        }
        if (!inventorySnapshots.isEmpty()) {
            put(summary, "inventorySnapshots", Integer.toString(inventorySnapshots.size()));
        }
        if (!containerSnapshots.isEmpty()) {
            put(summary, "containerSnapshots", Integer.toString(containerSnapshots.size()));
        }
        if (!regionSnapshots.isEmpty()) {
            put(summary, "regionSnapshots", Integer.toString(regionSnapshots.size()));
        }
        if (!signalChannelSnapshots.isEmpty()) {
            put(summary, "signalChannelSnapshots", Integer.toString(signalChannelSnapshots.size()));
        }
        if (!signalHistorySnapshots.isEmpty()) {
            put(summary, "signalHistorySnapshots", Integer.toString(signalHistorySnapshots.size()));
        }
        if (!logicChainSnapshots.isEmpty()) {
            put(summary, "logicChainSnapshots", Integer.toString(logicChainSnapshots.size()));
        }
        return Map.copyOf(summary);
    }

    public Optional<ConditionItemStackSnapshot> itemSnapshot(String key) {
        return Optional.ofNullable(itemSnapshots.get(safe(key)));
    }

    public Optional<ConditionInventorySnapshot> inventorySnapshot(String key) {
        return Optional.ofNullable(inventorySnapshots.get(safe(key)));
    }

    public Optional<ConditionContainerSnapshot> containerSnapshot(String key) {
        return Optional.ofNullable(containerSnapshots.get(safe(key)));
    }

    public Optional<ConditionRegionSnapshot> regionSnapshot(String key) {
        return Optional.ofNullable(regionSnapshots.get(safe(key)));
    }

    public Optional<ConditionSignalChannelSnapshot> signalChannelSnapshot(String key) {
        return Optional.ofNullable(signalChannelSnapshots.get(safe(key)));
    }

    public Optional<ConditionSignalHistorySnapshot> signalHistorySnapshot(String key) {
        return Optional.ofNullable(signalHistorySnapshots.get(safe(key)));
    }

    public Optional<ConditionLogicChainSnapshot> logicChainSnapshot(String key) {
        return Optional.ofNullable(logicChainSnapshots.get(safe(key)));
    }

    public String snapshotType(String key) {
        String safeKey = safe(key);
        if (itemSnapshots.containsKey(safeKey)) {
            return "物品快照";
        }
        if (inventorySnapshots.containsKey(safeKey)) {
            return "背包快照";
        }
        if (containerSnapshots.containsKey(safeKey)) {
            return "容器快照";
        }
        if (regionSnapshots.containsKey(safeKey)) {
            return "区域快照";
        }
        if (signalChannelSnapshots.containsKey(safeKey)) {
            return "信号频道快照";
        }
        if (signalHistorySnapshots.containsKey(safeKey)) {
            return "信号历史快照";
        }
        if (logicChainSnapshots.containsKey(safeKey)) {
            return "逻辑链快照";
        }
        return "";
    }

    public boolean hasPlayerIdentity() {
        return !playerId.isBlank() || !playerName.isBlank();
    }

    public String playerLabel() {
        if (!playerName.isBlank()) {
            return playerName;
        }
        return playerId.isBlank() ? "未知玩家" : playerId;
    }

    public boolean hasPlayerTag(String tag) {
        String expected = safe(tag);
        return !expected.isBlank() && playerTags.contains(expected);
    }

    public String compactSummary() {
        Map<String, String> summary = summary();
        if (summary.isEmpty()) {
            return "empty_context";
        }
        StringBuilder builder = new StringBuilder();
        summary.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(key).append('=').append(value);
        });
        return builder.toString();
    }

    private static void put(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static Map<String, String> copy(Map<String, String> raw) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                String safeKey = safe(key);
                if (!safeKey.isBlank()) {
                    copy.put(safeKey, safe(value));
                }
            });
        }
        return Map.copyOf(copy);
    }

    private static <T> Map<String, T> copySnapshotMap(Map<String, T> raw) {
        Map<String, T> copy = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                String safeKey = safe(key);
                if (!safeKey.isBlank() && value != null) {
                    copy.put(safeKey, value);
                }
            });
        }
        return Map.copyOf(copy);
    }

    private static List<String> copyList(Collection<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(ConditionEvaluationContext::safe)
                .filter((value) -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private final Map<String, String> eventMetadata = new LinkedHashMap<>();
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final java.util.ArrayList<String> playerTags = new java.util.ArrayList<>();
        private String playerId = "";
        private String playerName = "";
        private Boolean playerOnline = null;
        private Boolean playerOp = null;
        private String playerTeam = "";
        private String playerGameMode = "";
        private Boolean playerAlive = null;
        private String worldId = "";
        private String sourceType = "";
        private String sourceId = "";
        private String channel = "";
        private String deviceId = "";
        private String listenerId = "";
        private String regionId = "";
        private String actionId = "";
        private String blockPos = "";
        private String itemStackSummary = "";
        private String triggerType = "";
        private String detail = "";
        private long gameTime = 0L;
        private int signalDepth = 0;
        private StateVariableSnapshot stateVariables = StateVariableSnapshot.empty();
        private final Map<String, ConditionItemStackSnapshot> itemSnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionInventorySnapshot> inventorySnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionContainerSnapshot> containerSnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionRegionSnapshot> regionSnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionSignalChannelSnapshot> signalChannelSnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionSignalHistorySnapshot> signalHistorySnapshots = new LinkedHashMap<>();
        private final Map<String, ConditionLogicChainSnapshot> logicChainSnapshots = new LinkedHashMap<>();

        public Builder player(String id, String name) {
            this.playerId = id;
            this.playerName = name;
            return this;
        }

        public Builder playerOnline(boolean playerOnline) {
            this.playerOnline = playerOnline;
            return this;
        }

        public Builder playerOp(boolean playerOp) {
            this.playerOp = playerOp;
            return this;
        }

        public Builder playerTags(Collection<String> tags) {
            this.playerTags.clear();
            this.playerTags.addAll(copyList(tags));
            return this;
        }

        public Builder playerTag(String tag) {
            String safeTag = safe(tag);
            if (!safeTag.isBlank() && !this.playerTags.contains(safeTag)) {
                this.playerTags.add(safeTag);
            }
            return this;
        }

        public Builder playerTeam(String playerTeam) {
            this.playerTeam = playerTeam;
            return this;
        }

        public Builder playerGameMode(String playerGameMode) {
            this.playerGameMode = playerGameMode;
            return this;
        }

        public Builder playerAlive(boolean playerAlive) {
            this.playerAlive = playerAlive;
            return this;
        }

        public Builder worldId(String worldId) {
            this.worldId = worldId;
            return this;
        }

        public Builder source(String sourceType, String sourceId) {
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder listenerId(String listenerId) {
            this.listenerId = listenerId;
            return this;
        }

        public Builder regionId(String regionId) {
            this.regionId = regionId;
            return this;
        }

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder blockPos(String blockPos) {
            this.blockPos = blockPos;
            return this;
        }

        public Builder itemStackSummary(String itemStackSummary) {
            this.itemStackSummary = itemStackSummary;
            return this;
        }

        public Builder triggerType(String triggerType) {
            this.triggerType = triggerType;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder gameTime(long gameTime) {
            this.gameTime = gameTime;
            return this;
        }

        public Builder signalDepth(int signalDepth) {
            this.signalDepth = signalDepth;
            return this;
        }

        public Builder eventMetadata(String key, String value) {
            this.eventMetadata.put(key, value);
            return this;
        }

        public Builder variable(String key, String value) {
            this.variables.put(key, value);
            return this;
        }

        public Builder stateVariables(StateVariableSnapshot stateVariables) {
            this.stateVariables = stateVariables == null ? StateVariableSnapshot.empty() : stateVariables;
            return this;
        }

        public Builder stateVariable(StateVariableRecord record) {
            if (record != null) {
                this.stateVariables = this.stateVariables.with(record);
            }
            return this;
        }

        public Builder itemSnapshot(String key, ConditionItemStackSnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.itemSnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder inventorySnapshot(String key, ConditionInventorySnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.inventorySnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder containerSnapshot(String key, ConditionContainerSnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.containerSnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder regionSnapshot(String key, ConditionRegionSnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.regionSnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder signalChannelSnapshot(String key, ConditionSignalChannelSnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.signalChannelSnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder signalHistorySnapshot(String key, ConditionSignalHistorySnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.signalHistorySnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public Builder logicChainSnapshot(String key, ConditionLogicChainSnapshot snapshot) {
            String safeKey = safe(key);
            if (!safeKey.isBlank() && snapshot != null) {
                this.logicChainSnapshots.put(safeKey, snapshot);
            }
            return this;
        }

        public ConditionEvaluationContext build() {
            return new ConditionEvaluationContext(
                    playerId,
                    playerName,
                    playerOnline,
                    playerOp,
                    playerTags,
                    playerTeam,
                    playerGameMode,
                    playerAlive,
                    worldId,
                    sourceType,
                    sourceId,
                    channel,
                    deviceId,
                    listenerId,
                    regionId,
                    actionId,
                    blockPos,
                    itemStackSummary,
                    triggerType,
                    detail,
                    gameTime,
                    signalDepth,
                    stateVariables,
                    itemSnapshots,
                    inventorySnapshots,
                    containerSnapshots,
                    regionSnapshots,
                    signalChannelSnapshots,
                    signalHistorySnapshots,
                    logicChainSnapshots,
                    eventMetadata,
                    variables
            );
        }
    }
}
