package com.zcpu.tzzmod.condition.regionlogic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record ConditionRegionSnapshot(
        String regionId,
        String displayName,
        boolean enabled,
        String world,
        List<String> playerIdsInside,
        String boundsSummary,
        Map<String, String> metadata
) {
    public ConditionRegionSnapshot {
        regionId = safe(regionId);
        displayName = safe(displayName);
        world = safe(world);
        playerIdsInside = copyList(playerIdsInside);
        boundsSummary = safe(boundsSummary);
        metadata = copyMap(metadata);
    }

    public static ConditionRegionSnapshot of(String regionId, boolean enabled, Collection<String> playerIdsInside) {
        return new ConditionRegionSnapshot(regionId, "", enabled, "", playerIdsInside == null ? List.of() : List.copyOf(playerIdsInside), "", Map.of());
    }

    public boolean containsPlayer(String playerId) {
        String target = safe(playerId);
        return !target.isBlank() && playerIdsInside.contains(target);
    }

    public int playerCount() {
        return playerIdsInside.size();
    }

    public String label() {
        return displayName.isBlank() ? regionId : displayName;
    }

    private static List<String> copyList(Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(ConditionRegionSnapshot::safe)
                .filter((value) -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static Map<String, String> copyMap(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, String> copy = new TreeMap<>();
        raw.forEach((key, value) -> {
            String safeKey = safe(key);
            if (!safeKey.isBlank()) {
                copy.put(safeKey, safe(value));
            }
        });
        return Map.copyOf(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
