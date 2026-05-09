package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceDeleteRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminVirtualBlockDeviceLifecycleService {
    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;

    public WebAdminVirtualBlockDeviceLifecycleService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String pathDeviceId,
            WebAdminVirtualBlockDeviceDeleteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String deviceId = firstNonBlank(pathDeviceId, request == null ? "" : request.deviceId);
        WebAdminWriteTarget initialTarget = new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE", safe(deviceId), safe(deviceId));
        WebAdminWriteContext initialContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE,
                initialTarget
        );
        WebAdminWriteResult initialGate = writeGate(user, session, csrfToken, sameOrigin, initialTarget, initialContext, Map.of());
        if (!initialGate.success()) {
            return initialGate;
        }

        SignalDeviceData device = findDevice(server, deviceId);
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "VIRTUAL_BLOCK_DEVICE",
                safe(deviceId),
                device == null ? safe(deviceId) : WebAdminReadonlySupport.deviceDisplayName(device)
        );
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE,
                target
        );

        if (device == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "虚拟方块设备不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "target_not_found"));
            return result;
        }
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "deviceId",
                    "invalid_type",
                    "只能删除 / 解绑 virtual_block_device，不能删除其它 Signal 设备类型。",
                    device.type()
            )));
            audit(context, result, deviceSummary(device), Map.of("attempt", "non_virtual_block_device"));
            return result;
        }
        WebAdminWriteResult confirmation = requireDangerConfirmation(request, target, device);
        if (!confirmation.success()) {
            audit(context, confirmation, deviceSummary(device), Map.of("attempt", "confirmation_required"));
            return confirmation;
        }

        // This is an unbind: remove only WebAdmin/SignalDeviceStore registry config, never the world block.
        boolean removed = SignalDeviceStore.removeById(server, device.id());
        if (!removed) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "虚拟方块设备已被删除。");
            audit(context, result, deviceSummary(device), Map.of("attempt", "already_removed"));
            return result;
        }
        SignalDeviceStore.forceFlushDirty(server);
        boolean metadataRemoved = WebAdminDeviceMetadataStore.removeDevice(server, device.id());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deletedDevice", deviceSummary(device));
        data.put("deviceId", device.id());
        data.put("metadataRemoved", metadataRemoved);
        data.put("routeTarget", "#/virtual-block-devices");
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "虚拟方块设备已删除 / 解绑，世界方块未被破坏。",
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
        WebAdminAuditEvent auditEvent = audit(context, result, deviceSummary(device), Map.of("removed", true, "reason", safe(request == null ? "" : request.reason)));
        publishRealtime(device, auditEvent, user);
        return result;
    }

    private WebAdminWriteResult writeGate(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            WebAdminWriteContext context,
            Map<String, ?> before
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(context, result, before, Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(context, result, before, Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
            audit(context, result, before, Map.of("attempt", "origin_failed"));
            return result;
        }
        return WebAdminWriteResult.ok(target, false, "写入安全检查通过。");
    }

    private WebAdminWriteResult requireDangerConfirmation(WebAdminVirtualBlockDeviceDeleteRequest request, WebAdminWriteTarget target, SignalDeviceData device) {
        String confirmation = safe(request == null ? "" : request.confirmationText).trim();
        boolean confirmed = request != null && Boolean.TRUE.equals(request.confirmed);
        String displayName = WebAdminReadonlySupport.deviceDisplayName(device);
        if (confirmed && (confirmation.equals(device.id()) || (!displayName.isBlank() && confirmation.equals(displayName)))) {
            return WebAdminWriteResult.ok(target, false, "危险操作确认通过。");
        }
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION.id(),
                "删除 / 解绑虚拟方块设备前，需要勾选确认并输入设备 ID 或显示名称。",
                target.targetType(),
                target.targetId(),
                false,
                List.of(new WebAdminValidationError("confirmationText", "required", "请输入设备 ID 或显示名称以确认删除 / 解绑。", confirmation)),
                "",
                "",
                true,
                Map.of("expected", device.id()),
                Map.of()
        );
    }

    private SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private void publishRealtime(SignalDeviceData device, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(device.id())
                .channel(device.channel())
                .sourceType(SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE)
                .severity("INFO")
                .summary("虚拟方块设备已删除 / 解绑。")
                .routeTarget("#/virtual-block-devices")
                .payload("targetType", "virtual_block_device")
                .payload("deviceId", device.id())
                .payload("removed", true)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(device.id())
                .channel(device.channel())
                .sourceType(SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget("#/history")
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id()));
    }

    private static Map<String, Object> deviceSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        summary.put("deviceId", device.id());
        summary.put("type", device.type());
        summary.put("name", device.name());
        summary.put("world", device.dimension());
        summary.put("x", device.x());
        summary.put("y", device.y());
        summary.put("z", device.z());
        summary.put("channel", device.channel());
        summary.put("blockId", device.blockId());
        summary.put("enabled", device.enabled());
        return summary;
    }

    private static String firstNonBlank(String first, String second) {
        return !safe(first).isBlank() ? safe(first) : safe(second);
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }
}
