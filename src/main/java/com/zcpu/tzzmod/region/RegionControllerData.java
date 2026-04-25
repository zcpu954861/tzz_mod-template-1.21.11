package com.zcpu.tzzmod.region;

import com.zcpu.tzzmod.action.ActionConfig;
import java.util.List;

public record RegionControllerData(
        String id,
        String name,
        String regionId,
        boolean enabled,
        RegionTargetFilter targetFilter,
        int stayIntervalTicks,
        List<ActionConfig> enterActions,
        List<ActionConfig> exitActions,
        List<ActionConfig> stayActions
) {
    public static final int DEFAULT_STAY_INTERVAL_TICKS = 100;
    public static final int MIN_STAY_INTERVAL_TICKS = 20;

    public RegionControllerData normalized() {
        String cleanId = id == null ? "" : id.trim();
        String cleanName = name == null ? "" : name.trim();
        String cleanRegionId = regionId == null ? "" : regionId.trim();
        int interval = stayIntervalTicks <= 0 ? DEFAULT_STAY_INTERVAL_TICKS : stayIntervalTicks;
        interval = Math.max(MIN_STAY_INTERVAL_TICKS, interval);

        return new RegionControllerData(
                cleanId,
                cleanName,
                cleanRegionId,
                enabled,
                targetFilter == null ? RegionTargetFilter.all() : targetFilter.normalized(),
                interval,
                enterActions == null ? List.of() : List.copyOf(enterActions),
                exitActions == null ? List.of() : List.copyOf(exitActions),
                stayActions == null ? List.of() : List.copyOf(stayActions)
        );
    }

    public List<ActionConfig> actionsFor(RegionTriggerType triggerType) {
        return switch (triggerType) {
            case ENTER -> enterActions == null ? List.of() : enterActions;
            case EXIT -> exitActions == null ? List.of() : exitActions;
            case STAY -> stayActions == null ? List.of() : stayActions;
        };
    }
}
