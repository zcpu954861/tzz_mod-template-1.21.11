package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.debug.DeviceDiagnostic;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.signal.device.debug.VirtualBlockDeviceDiagnosticService;
import com.zcpu.tzzmod.signal.device.item.InteractionItemConsumeSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminDeviceService {
    public List<WebAdminDtos.DeviceListEntryDto> listDevices(MinecraftServer server, int requestedLimit) {
        int limit = WebAdminReadonlySupport.limit(requestedLimit, WebAdminReadonlySupport.MAX_LIST_LIMIT);
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<WebAdminDtos.DeviceListEntryDto> result = new ArrayList<>();
        for (SignalDeviceData raw : devices) {
            if (result.size() >= limit) {
                break;
            }
            SignalDeviceData device = raw.normalized();
            List<DiagnosticIssue> issues = diagnosticIssues(server, device);
            result.add(new WebAdminDtos.DeviceListEntryDto(
                    device.id(),
                    WebAdminReadonlySupport.deviceDisplayName(device),
                    WebAdminReadonlySupport.deviceType(device),
                    device.dimension(),
                    WebAdminReadonlySupport.pos(device),
                    device.enabled(),
                    device.channel(),
                    WebAdminReadonlySupport.isoTime(device.lastTriggerWallTimeMillis()),
                    WebAdminReadonlySupport.doctorStatus(issues),
                    true
            ));
        }
        return List.copyOf(result);
    }

    public SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        if (resolved.foundUnique()) {
            return resolved.device().normalized();
        }
        return null;
    }

    public WebAdminDtos.DeviceDetailDto detail(MinecraftServer server, SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice.normalized();
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<DiagnosticIssue> issues = diagnosticIssues(server, device);
        Map<String, String> navigation = new LinkedHashMap<>();
        if (!device.channel().isBlank()) {
            navigation.put("channelDetail", "/api/signals/channels/" + device.channel());
        }
        return new WebAdminDtos.DeviceDetailDto(
                device.id(),
                WebAdminReadonlySupport.deviceDisplayName(device),
                WebAdminReadonlySupport.deviceType(device),
                device.dimension(),
                WebAdminReadonlySupport.pos(device),
                device.enabled(),
                device.channel(),
                configSummary(device),
                recentHistoryForDevice(device, devices),
                doctorIssueDtos(issues),
                debugSummary(device, issues),
                navigation
        );
    }

    public WebAdminDtos.DeviceDebugDto debug(MinecraftServer server, SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice.normalized();
        List<DiagnosticIssue> issues = diagnosticIssues(server, device);
        List<WebAdminDtos.DebugCheckDto> checks = new ArrayList<>();
        checks.add(new WebAdminDtos.DebugCheckDto("enabled", device.enabled() ? "OK" : "WARNING",
                device.enabled() ? "Device is enabled." : "Device is disabled."));
        checks.add(new WebAdminDtos.DebugCheckDto("channel", device.channel().isBlank() ? "WARNING" : "OK",
                device.channel().isBlank() ? "Primary channel is empty." : device.channel()));
        for (DiagnosticIssue issue : issues) {
            checks.add(new WebAdminDtos.DebugCheckDto(
                    issue.code(),
                    severity(issue.severity()),
                    issue.title().isBlank() ? issue.message() : issue.title()
            ));
        }
        return new WebAdminDtos.DeviceDebugDto(
                device.id(),
                WebAdminReadonlySupport.deviceType(device),
                device.enabled(),
                device.channel(),
                WebAdminReadonlySupport.isoTime(device.lastTriggerWallTimeMillis()),
                Instant.now().toString(),
                List.copyOf(checks)
        );
    }

    private List<DiagnosticIssue> diagnosticIssues(MinecraftServer server, SignalDeviceData device) {
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            return List.of();
        }
        long time = server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
        DeviceDiagnostic diagnostic = VirtualBlockDeviceDiagnosticService.diagnose(server, device, time);
        return diagnostic.issues();
    }

    private List<WebAdminDtos.DoctorIssueDto> doctorIssueDtos(List<DiagnosticIssue> issues) {
        List<WebAdminDtos.DoctorIssueDto> result = new ArrayList<>();
        for (int i = 0; i < issues.size(); i++) {
            result.add(WebAdminDoctorService.fromDiagnosticIssue(issues.get(i), i));
        }
        return List.copyOf(result);
    }

    private List<WebAdminDtos.SignalHistoryEntryDto> recentHistoryForDevice(
            SignalDeviceData device,
            List<SignalDeviceData> devices
    ) {
        List<com.zcpu.tzzmod.signal.SignalEventRecord> records = new ArrayList<>();
        for (com.zcpu.tzzmod.signal.SignalEventRecord record : SignalEventHistory.snapshot()) {
            if (record != null && device.id().equals(record.sourceId())) {
                records.add(record);
            }
        }
        return WebAdminReadonlySupport.historyDtos(records, devices, 20);
    }

    private Map<String, Object> configSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("shortId", SignalDeviceStore.shortId(device.id()));
        summary.put("cooldownTicks", device.cooldownTicks());
        summary.put("pulseTicks", device.pulseTicks());
        summary.put("actionCount", device.actionCount());
        summary.put("blockId", device.blockId());
        summary.put("mode", device.mode());
        summary.put("conditionEnabled", device.conditionEnabled());
        summary.put("interactionEnabled", device.interactionEnabled());
        summary.put("containerEnabled", device.containerEnabled());
        summary.put("itemConditionCount", device.itemConditions().size());
        summary.put("itemSubmitEnabled", device.itemSubmitEnabled());
        summary.put("itemSubmitRequirementCount", device.itemSubmitRequirements().size());

        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        Map<String, Object> interactionItem = new LinkedHashMap<>();
        interactionItem.put("enabled", device.interactionItemMatcherEnabled());
        interactionItem.put("source", matcher.interactionItemSource());
        interactionItem.put("sourceDisplayName", InteractionItemSource.displayName(matcher.interactionItemSource()));
        interactionItem.put("vanillaPolicy", matcher.interactionItemVanillaPolicy());
        interactionItem.put("vanillaPolicyDisplayName", InteractionItemVanillaPolicy.displayName(matcher.interactionItemVanillaPolicy()));
        interactionItem.put("consumeEnabled", matcher.consumeEnabled());
        interactionItem.put("consumeCount", matcher.consumeCount());
        interactionItem.put("consumeSource", matcher.interactionItemConsumeSource());
        interactionItem.put("consumeSourceDisplayName", InteractionItemConsumeSource.displayName(matcher.interactionItemConsumeSource()));
        interactionItem.put("inventoryConsumeOrder", matcher.interactionItemInventoryConsumeOrder());
        interactionItem.put("inventoryConsumeOrderDisplayName", InventoryConsumeOrder.displayName(matcher.interactionItemInventoryConsumeOrder()));
        interactionItem.put("templateSummary", matcher.templateSummary());
        summary.put("interactionItem", interactionItem);
        return summary;
    }

    private Map<String, Object> debugSummary(SignalDeviceData device, List<DiagnosticIssue> issues) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", WebAdminReadonlySupport.doctorStatus(issues));
        summary.put("issueCount", issues.size());
        summary.put("lastResult", device.lastResult());
        summary.put("lastInteractionResult", device.lastInteractionResult());
        summary.put("lastInteractionItemResult", device.lastInteractionItemResult());
        summary.put("lastItemSubmitResult", device.lastItemSubmitResult());
        return summary;
    }

    private static String severity(DiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
            case INFO -> "INFO";
        };
    }
}
