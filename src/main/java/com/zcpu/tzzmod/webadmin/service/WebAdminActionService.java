package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminActionService {
    public List<WebAdminDtos.ActionListEntryDto> listActions(MinecraftServer server, int requestedLimit) {
        int limit = WebAdminReadonlySupport.limit(requestedLimit, WebAdminReadonlySupport.MAX_LIST_LIMIT);
        List<WebAdminDtos.ActionListEntryDto> actions = collectActions(server);
        if (actions.size() <= limit) {
            return actions;
        }
        return List.copyOf(actions.subList(0, limit));
    }

    public WebAdminDtos.ActionDetailDto findAction(MinecraftServer server, String id) {
        for (WebAdminDtos.ActionListEntryDto action : collectActions(server)) {
            if (action.id().equals(id)) {
                WebAdminDtos.ActionOwnerDto owner = new WebAdminDtos.ActionOwnerDto(
                        action.ownerType(),
                        action.ownerId(),
                        action.ownerName(),
                        action.channel()
                );
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("name", action.name());
                summary.put("referencedByCount", action.referencedByCount());
                summary.put("executionCount", action.executionCount());
                summary.put("doctorStatus", action.doctorStatus());
                return new WebAdminDtos.ActionDetailDto(
                        action.id(),
                        action.type(),
                        action.summary(),
                        owner,
                        summary,
                        List.of(),
                        List.of(),
                        "action:" + action.id()
                );
            }
        }
        return null;
    }

    private List<WebAdminDtos.ActionListEntryDto> collectActions(MinecraftServer server) {
        List<WebAdminDtos.ActionListEntryDto> result = new ArrayList<>();
        for (SignalListenerData listener : SignalListenerStore.getSnapshot(server)) {
            addConfiguredActions(result, listener.actions(), "LISTENER", listener.id(), display(listener.name(), listener.id()), listener.channel());
        }
        for (RegionControllerData rawRegion : RegionControllerStore.getSnapshot(server)) {
            RegionControllerData region = rawRegion.normalized();
            addConfiguredActions(result, region.enterActions(), "REGION_ENTER", region.id(), display(region.name(), region.id()), "");
            addConfiguredActions(result, region.exitActions(), "REGION_EXIT", region.id(), display(region.name(), region.id()), "");
            addConfiguredActions(result, region.stayActions(), "REGION_STAY", region.id(), display(region.name(), region.id()), "");
        }
        for (SignalDeviceData rawDevice : SignalDeviceStore.getSnapshot(server)) {
            SignalDeviceData device = rawDevice.normalized();
            if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type()) && device.actionCount() > 0) {
                result.add(new WebAdminDtos.ActionListEntryDto(
                        "ACTION_RELAY:" + device.id(),
                        WebAdminReadonlySupport.deviceDisplayName(device),
                        "UNKNOWN",
                        "Action relay contains " + device.actionCount() + " action(s); detailed configs are stored in the block entity.",
                        "ACTION_RELAY",
                        device.id(),
                        WebAdminReadonlySupport.deviceDisplayName(device),
                        device.channel(),
                        device.actionCount(),
                        0,
                        "UNKNOWN",
                        WebAdminReadonlySupport.isoTime(device.lastTriggerWallTimeMillis()),
                        device.enabled() ? "OK" : "WARNING"
                ));
            }
        }
        return List.copyOf(result);
    }

    private void addConfiguredActions(
            List<WebAdminDtos.ActionListEntryDto> result,
            List<ActionConfig> actions,
            String ownerType,
            String ownerId,
            String ownerName,
            String channel
    ) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            result.add(new WebAdminDtos.ActionListEntryDto(
                    ownerType + ":" + ownerId + ":" + i,
                    ownerName + " #" + (i + 1),
                    WebAdminReadonlySupport.actionType(action),
                    WebAdminReadonlySupport.actionSummary(action),
                    ownerType,
                    ownerId,
                    ownerName,
                    channel,
                    1,
                    0,
                    "UNKNOWN",
                    "",
                    action != null && action.isUsable() ? "OK" : "WARNING"
            ));
        }
    }

    private static String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
