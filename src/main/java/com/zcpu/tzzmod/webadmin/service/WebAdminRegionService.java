package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminRegionService {
    public List<WebAdminDtos.RegionListEntryDto> listRegions(MinecraftServer server, int requestedLimit) {
        int limit = WebAdminReadonlySupport.limit(requestedLimit, WebAdminReadonlySupport.MAX_LIST_LIMIT);
        List<WebAdminDtos.RegionListEntryDto> result = new ArrayList<>();
        for (RegionControllerData raw : RegionControllerStore.getSnapshot(server)) {
            if (result.size() >= limit) {
                break;
            }
            RegionControllerData region = raw.normalized();
            MapDataStore.PlannerRegionData planner = MapDataStore.getPlannerRegion(server, region.regionId());
            List<String> boundChannels = boundChannels(region);
            result.add(new WebAdminDtos.RegionListEntryDto(
                    region.id(),
                    displayName(region),
                    planner == null ? "" : planner.dimensionId(),
                    bounds(planner),
                    targetFilter(region),
                    actionCount(region.enterActions()),
                    actionCount(region.exitActions()),
                    actionCount(region.stayActions()),
                    boundChannels.isEmpty() ? "" : boundChannels.get(0),
                    0,
                    "",
                    region.enabled(),
                    region.enabled() ? "OK" : "WARNING"
            ));
        }
        return List.copyOf(result);
    }

    public RegionControllerData findRegion(MinecraftServer server, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (RegionControllerData raw : RegionControllerStore.getSnapshot(server)) {
            RegionControllerData region = raw.normalized();
            if (region.id().equals(id) || region.regionId().equals(id)) {
                return region;
            }
        }
        return null;
    }

    public WebAdminDtos.RegionDetailDto detail(MinecraftServer server, RegionControllerData rawRegion) {
        RegionControllerData region = rawRegion.normalized();
        MapDataStore.PlannerRegionData planner = MapDataStore.getPlannerRegion(server, region.regionId());
        Map<String, List<WebAdminDtos.RegionActionSummaryDto>> actions = new LinkedHashMap<>();
        actions.put("enter", actionSummaries(region.enterActions(), "enter"));
        actions.put("exit", actionSummaries(region.exitActions(), "exit"));
        actions.put("stay", actionSummaries(region.stayActions(), "stay"));
        return new WebAdminDtos.RegionDetailDto(
                region.id(),
                displayName(region),
                planner == null ? "" : planner.dimensionId(),
                bounds(planner),
                targetFilter(region),
                actions,
                boundChannels(region),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static String displayName(RegionControllerData region) {
        return region.name() == null || region.name().isBlank() ? region.id() : region.name();
    }

    private static String targetFilter(RegionControllerData region) {
        if (region.targetFilter() == null || region.targetFilter().type() == null) {
            return "ALL";
        }
        return region.targetFilter().type().name();
    }

    private static WebAdminDtos.RegionBoundsDto bounds(MapDataStore.PlannerRegionData planner) {
        if (planner == null) {
            return null;
        }
        RegionGeometry.Bounds bounds = planner.bounds();
        return new WebAdminDtos.RegionBoundsDto(bounds.minX(), 0, bounds.minZ(), bounds.maxX(), 0, bounds.maxZ());
    }

    private static int actionCount(List<ActionConfig> actions) {
        return actions == null ? 0 : actions.size();
    }

    private static List<String> boundChannels(RegionControllerData region) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        addActionChannels(channels, region.enterActions());
        addActionChannels(channels, region.exitActions());
        addActionChannels(channels, region.stayActions());
        return List.copyOf(channels);
    }

    private static void addActionChannels(LinkedHashSet<String> channels, List<ActionConfig> actions) {
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action != null && action.type() == ActionType.SIGNAL) {
                String channel = SignalChannel.normalize(action.value());
                if (!channel.isBlank()) {
                    channels.add(channel);
                }
            }
        }
    }

    private static List<WebAdminDtos.RegionActionSummaryDto> actionSummaries(List<ActionConfig> actions, String prefix) {
        List<WebAdminDtos.RegionActionSummaryDto> result = new ArrayList<>();
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            result.add(new WebAdminDtos.RegionActionSummaryDto(
                    prefix + ":" + i,
                    WebAdminReadonlySupport.actionType(action),
                    WebAdminReadonlySupport.actionSummary(action),
                    action != null && action.enabled()
            ));
        }
        return List.copyOf(result);
    }
}
