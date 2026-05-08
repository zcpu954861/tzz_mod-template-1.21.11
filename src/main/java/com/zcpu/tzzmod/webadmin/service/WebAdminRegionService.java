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
        List<RegionControllerData> controllers = normalizedControllers(server);
        Map<String, List<RegionControllerData>> controllersByRegionId = controllersByRegionId(controllers);
        LinkedHashSet<String> includedControllerIds = new LinkedHashSet<>();

        for (MapDataStore.PlannerRegionData planner : MapDataStore.getPlannerRegionsSnapshot(server)) {
            if (result.size() >= limit) {
                break;
            }
            List<RegionControllerData> regionControllers = controllersByRegionId.getOrDefault(planner.id(), List.of());
            regionControllers.forEach(controller -> includedControllerIds.add(controller.id()));
            result.add(listEntry(planner, regionControllers));
        }

        for (RegionControllerData controller : controllers) {
            if (result.size() >= limit) {
                break;
            }
            if (includedControllerIds.contains(controller.id())) {
                continue;
            }
            result.add(listEntry(MapDataStore.getPlannerRegion(server, controller.regionId()), List.of(controller)));
        }
        return List.copyOf(result);
    }

    public WebAdminDtos.RegionDetailDto detail(MinecraftServer server, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        List<RegionControllerData> controllers = normalizedControllers(server);
        MapDataStore.PlannerRegionData planner = MapDataStore.getPlannerRegion(server, id);
        List<RegionControllerData> regionControllers = planner == null ? List.of() : controllersByRegionId(controllers).getOrDefault(planner.id(), List.of());
        if (planner == null) {
            for (RegionControllerData controller : controllers) {
                if (controller.id().equals(id) || controller.regionId().equals(id)) {
                    planner = MapDataStore.getPlannerRegion(server, controller.regionId());
                    regionControllers = List.of(controller);
                    break;
                }
            }
        }
        Map<String, List<WebAdminDtos.RegionActionSummaryDto>> actions = new LinkedHashMap<>();
        actions.put("enter", actionSummaries(regionControllers, RegionActionGroup.ENTER));
        actions.put("exit", actionSummaries(regionControllers, RegionActionGroup.EXIT));
        actions.put("stay", actionSummaries(regionControllers, RegionActionGroup.STAY));
        if (planner == null && regionControllers.isEmpty()) {
            return null;
        }
        return new WebAdminDtos.RegionDetailDto(
                planner == null ? id : planner.id(),
                planner == null ? displayName(regionControllers.get(0)) : planner.name(),
                planner == null ? "" : planner.dimensionId(),
                bounds(planner),
                targetFilter(regionControllers),
                actions,
                boundChannels(regionControllers),
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

    private static String targetFilter(List<RegionControllerData> controllers) {
        if (controllers == null || controllers.isEmpty()) {
            return "ALL";
        }
        LinkedHashSet<String> filters = new LinkedHashSet<>();
        for (RegionControllerData controller : controllers) {
            filters.add(targetFilter(controller));
        }
        return filters.size() == 1 ? filters.iterator().next() : "MULTIPLE";
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

    private static int actionCount(List<RegionControllerData> controllers, RegionActionGroup group) {
        int total = 0;
        for (RegionControllerData controller : controllers == null ? List.<RegionControllerData>of() : controllers) {
            total += actionCount(actions(controller, group));
        }
        return total;
    }

    private static List<String> boundChannels(List<RegionControllerData> controllers) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (RegionControllerData region : controllers == null ? List.<RegionControllerData>of() : controllers) {
            addActionChannels(channels, region.enterActions());
            addActionChannels(channels, region.exitActions());
            addActionChannels(channels, region.stayActions());
        }
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

    private static List<WebAdminDtos.RegionActionSummaryDto> actionSummaries(List<RegionControllerData> controllers, RegionActionGroup group) {
        List<WebAdminDtos.RegionActionSummaryDto> result = new ArrayList<>();
        for (RegionControllerData controller : controllers == null ? List.<RegionControllerData>of() : controllers) {
            List<ActionConfig> source = actions(controller, group);
            for (int i = 0; i < source.size(); i++) {
                ActionConfig action = source.get(i);
                result.add(new WebAdminDtos.RegionActionSummaryDto(
                        group.ownerType + ":" + controller.id() + ":" + i,
                        WebAdminReadonlySupport.actionType(action),
                        WebAdminReadonlySupport.actionSummary(action),
                        action != null && action.enabled()
                ));
            }
        }
        return List.copyOf(result);
    }

    private static WebAdminDtos.RegionListEntryDto listEntry(MapDataStore.PlannerRegionData planner, List<RegionControllerData> controllers) {
        List<RegionControllerData> normalized = controllers == null ? List.of() : controllers.stream()
                .map(RegionControllerData::normalized)
                .toList();
        List<String> channels = boundChannels(normalized);
        String controllerId = normalized.isEmpty() ? "" : normalized.get(0).id();
        boolean enabled = normalized.isEmpty() || normalized.stream().anyMatch(RegionControllerData::enabled);
        String doctorStatus = planner == null ? "WARNING" : enabled ? "OK" : "WARNING";
        String description = normalized.isEmpty()
                ? "规划区域，尚未关联 RegionController"
                : "关联 " + normalized.size() + " 个 RegionController";
        return new WebAdminDtos.RegionListEntryDto(
                planner == null ? controllerId : planner.id(),
                planner == null ? displayName(normalized.get(0)) : planner.name(),
                planner == null ? "" : planner.dimensionId(),
                bounds(planner),
                targetFilter(normalized),
                actionCount(normalized, RegionActionGroup.ENTER),
                actionCount(normalized, RegionActionGroup.EXIT),
                actionCount(normalized, RegionActionGroup.STAY),
                channels.isEmpty() ? "" : channels.get(0),
                0,
                "",
                enabled,
                doctorStatus,
                planner == null ? "controller" : "polygon",
                description,
                controllerId,
                normalized.size()
        );
    }

    private static List<RegionControllerData> normalizedControllers(MinecraftServer server) {
        List<RegionControllerData> result = new ArrayList<>();
        for (RegionControllerData raw : RegionControllerStore.getSnapshot(server)) {
            result.add(raw.normalized());
        }
        return List.copyOf(result);
    }

    private static Map<String, List<RegionControllerData>> controllersByRegionId(List<RegionControllerData> controllers) {
        Map<String, List<RegionControllerData>> grouped = new LinkedHashMap<>();
        for (RegionControllerData controller : controllers == null ? List.<RegionControllerData>of() : controllers) {
            if (controller.regionId() == null || controller.regionId().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(controller.regionId(), ignored -> new ArrayList<>()).add(controller);
        }
        return grouped;
    }

    private static List<ActionConfig> actions(RegionControllerData controller, RegionActionGroup group) {
        if (controller == null || group == null) {
            return List.of();
        }
        return switch (group) {
            case ENTER -> controller.enterActions();
            case EXIT -> controller.exitActions();
            case STAY -> controller.stayActions();
        };
    }

    private enum RegionActionGroup {
        ENTER("REGION_ENTER"),
        EXIT("REGION_EXIT"),
        STAY("REGION_STAY");

        private final String ownerType;

        RegionActionGroup(String ownerType) {
            this.ownerType = ownerType;
        }
    }
}
