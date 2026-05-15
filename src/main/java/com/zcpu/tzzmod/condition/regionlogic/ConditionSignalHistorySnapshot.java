package com.zcpu.tzzmod.condition.regionlogic;

import java.util.List;

public record ConditionSignalHistorySnapshot(List<ConditionSignalEventSnapshot> events) {
    public ConditionSignalHistorySnapshot {
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static ConditionSignalHistorySnapshot empty() {
        return new ConditionSignalHistorySnapshot(List.of());
    }

    public int count(String channelFilter, String sourceTypeFilter, String sourceIdFilter) {
        int count = 0;
        for (ConditionSignalEventSnapshot event : events) {
            if (event != null && event.matchesFilter(channelFilter, sourceTypeFilter, sourceIdFilter)) {
                count++;
            }
        }
        return count;
    }
}
