package com.zcpu.tzzmod.condition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        put(summary, "blockPos", blockPos);
        put(summary, "triggerType", triggerType);
        put(summary, "gameTime", Long.toString(gameTime));
        return Map.copyOf(summary);
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
                    eventMetadata,
                    variables
            );
        }
    }
}
