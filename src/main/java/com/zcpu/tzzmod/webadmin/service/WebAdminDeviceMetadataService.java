package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteAuditContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminDeviceMetadataService {
    public static final int MAX_DISPLAY_NAME_LENGTH = 64;
    public static final int MAX_NOTE_LENGTH = 500;
    public static final Set<String> ICON_KEYS = Set.of(
            "auto",
            "signal_emitter",
            "signal_receiver",
            "action_relay",
            "virtual_block_device",
            "region",
            "action",
            "warning",
            "key",
            "chest",
            "door",
            "signal",
            "custom_1"
    );

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;

    public WebAdminDeviceMetadataService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
    }

    public WebAdminDtos.DeviceMetadataDto metadataFor(MinecraftServer server, SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return emptyDto("");
        }
        WebAdminDeviceMetadataStore.MetadataFile file = WebAdminDeviceMetadataStore.load(server);
        WebAdminDeviceMetadataStore.MetadataEntry entry = file.devices.get(device.id());
        return dto(device, entry);
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminDeviceMetadataUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String deviceId = request == null ? "" : safe(request.deviceId);
        SignalDeviceData device = findDevice(server, deviceId);
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "DEVICE_METADATA",
                deviceId,
                device == null ? deviceId : WebAdminReadonlySupport.deviceDisplayName(device)
        );
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_DEVICE_METADATA,
                target
        );

        if (device == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of());
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_DEVICE_METADATA);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, Map.of(), Map.of("attempt", "permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    resultCode(csrf.code()),
                    target,
                    csrf.message()
            );
            audit(writeContext, result, Map.of(), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, Map.of(), Map.of("attempt", "origin_failed"));
            return result;
        }

        NormalizedRequest normalized = normalize(request);
        List<WebAdminValidationError> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(writeContext, result, Map.of(), normalized.summary());
            return result;
        }

        WebAdminDeviceMetadataStore.MetadataFile file = WebAdminDeviceMetadataStore.load(server);
        WebAdminDeviceMetadataStore.MetadataEntry before = WebAdminDeviceMetadataStore.MetadataEntry.normalized(
                device.id(),
                file.devices.get(device.id())
        );
        if (metadataEquals(before, normalized)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的变化。");
            audit(writeContext, result, beforeSummary(before), beforeSummary(before));
            return result;
        }

        WebAdminDeviceMetadataStore.MetadataEntry after = new WebAdminDeviceMetadataStore.MetadataEntry();
        after.deviceId = device.id();
        after.displayName = normalized.displayName();
        after.note = normalized.note();
        after.iconKey = normalized.iconKey();
        after.updatedAt = Instant.now().toString();
        after.updatedBy = user == null ? "" : user.username;
        after.version = before.version + 1L;
        file.devices.put(device.id(), after);
        boolean saved = WebAdminDeviceMetadataStore.save(server, file);
        if (!saved) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.INTERNAL_ERROR,
                    target,
                    "WebAdmin 显示信息保存失败，请查看服务端日志。"
            );
            audit(writeContext, result, beforeSummary(before), normalized.summary());
            return result;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metadata", dto(device, after));
        data.put("changedFields", changedFields(before, after));
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "WebAdmin 设备显示信息已保存。",
                target.targetType(),
                target.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data
        );
        WebAdminAuditEvent auditEvent = audit(writeContext, result, beforeSummary(before), beforeSummary(after));
        publishRealtime(device, after, auditEvent, changedFields(before, after), user);
        return result;
    }

    public static List<WebAdminValidationError> validateRequest(WebAdminDeviceMetadataUpdateRequest request) {
        NormalizedRequest normalized = normalize(request);
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (normalized.displayName().length() > MAX_DISPLAY_NAME_LENGTH) {
            errors.add(new WebAdminValidationError("displayName", "too_long", "显示名称不能超过 64 个字符。", normalized.displayName()));
        }
        if (normalized.note().length() > MAX_NOTE_LENGTH) {
            errors.add(new WebAdminValidationError("note", "too_long", "备注不能超过 500 个字符。", normalized.note()));
        }
        if (containsControl(normalized.displayName())) {
            errors.add(new WebAdminValidationError("displayName", "control_character", "显示名称不能包含控制字符。", normalized.displayName()));
        }
        if (containsControl(normalized.note())) {
            errors.add(new WebAdminValidationError("note", "control_character", "备注不能包含控制字符。", normalized.note()));
        }
        if (!ICON_KEYS.contains(normalized.iconKey())) {
            errors.add(new WebAdminValidationError("iconKey", "invalid_icon", "图标必须来自 WebAdmin 预设列表。", normalized.iconKey()));
        }
        return List.copyOf(errors);
    }

    public static boolean isAllowedIconKey(String iconKey) {
        return ICON_KEYS.contains(normalizeIcon(iconKey));
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(context),
                result,
                beforeSummary,
                afterSummary
        );
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private void publishRealtime(
            SignalDeviceData device,
            WebAdminDeviceMetadataStore.MetadataEntry metadata,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String deviceId = device.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .severity("INFO")
                .summary("WebAdmin 设备显示信息已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "device_metadata")
                .payload("changedFields", changedFields)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .severity("INFO")
                .summary("设备显示信息已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("changedFields", changedFields)
                .payload("displayName", metadata.displayName)
                .payload("iconKey", metadata.iconKey)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id()));
    }

    private static WebAdminDtos.DeviceMetadataDto dto(
            SignalDeviceData device,
            WebAdminDeviceMetadataStore.MetadataEntry rawEntry
    ) {
        WebAdminDeviceMetadataStore.MetadataEntry entry = WebAdminDeviceMetadataStore.MetadataEntry.normalized(device.id(), rawEntry);
        String effectiveName = entry.displayName.isBlank()
                ? WebAdminReadonlySupport.deviceDisplayName(device)
                : entry.displayName;
        String effectiveIcon = "auto".equals(entry.iconKey) || entry.iconKey.isBlank()
                ? defaultIconKey(device)
                : entry.iconKey;
        return new WebAdminDtos.DeviceMetadataDto(
                device.id(),
                entry.displayName,
                entry.note,
                entry.iconKey,
                effectiveName,
                effectiveIcon,
                entry.updatedAt,
                entry.updatedBy,
                entry.version
        );
    }

    private static WebAdminDtos.DeviceMetadataDto emptyDto(String deviceId) {
        return new WebAdminDtos.DeviceMetadataDto(safe(deviceId), "", "", "auto", "", "device", "", "", 0L);
    }

    private static String defaultIconKey(SignalDeviceData device) {
        String type = WebAdminReadonlySupport.deviceType(device).toLowerCase();
        return switch (type) {
            case "signal_emitter", "signal_receiver", "action_relay", "virtual_block_device" -> type;
            default -> "device";
        };
    }

    private static NormalizedRequest normalize(WebAdminDeviceMetadataUpdateRequest request) {
        return new NormalizedRequest(
                safe(request == null ? "" : request.displayName).trim(),
                safe(request == null ? "" : request.note).trim(),
                normalizeIcon(request == null ? "" : request.iconKey)
        );
    }

    private static String normalizeIcon(String iconKey) {
        String icon = safe(iconKey).trim().toLowerCase();
        return icon.isBlank() ? "auto" : icon;
    }

    private static boolean metadataEquals(WebAdminDeviceMetadataStore.MetadataEntry before, NormalizedRequest request) {
        return safe(before.displayName).equals(request.displayName())
                && safe(before.note).equals(request.note())
                && normalizeIcon(before.iconKey).equals(request.iconKey());
    }

    private static List<String> changedFields(
            WebAdminDeviceMetadataStore.MetadataEntry before,
            WebAdminDeviceMetadataStore.MetadataEntry after
    ) {
        List<String> fields = new ArrayList<>();
        if (!safe(before.displayName).equals(safe(after.displayName))) {
            fields.add("displayName");
        }
        if (!safe(before.note).equals(safe(after.note))) {
            fields.add("note");
        }
        if (!normalizeIcon(before.iconKey).equals(normalizeIcon(after.iconKey))) {
            fields.add("iconKey");
        }
        return List.copyOf(fields);
    }

    private static Map<String, Object> beforeSummary(WebAdminDeviceMetadataStore.MetadataEntry entry) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("displayName", safe(entry.displayName));
        summary.put("noteLength", safe(entry.note).length());
        summary.put("iconKey", normalizeIcon(entry.iconKey));
        summary.put("version", entry.version);
        return summary;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private record NormalizedRequest(String displayName, String note, String iconKey) {
        Map<String, Object> summary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("displayName", displayName);
            summary.put("noteLength", note.length());
            summary.put("iconKey", iconKey);
            return summary;
        }
    }
}
