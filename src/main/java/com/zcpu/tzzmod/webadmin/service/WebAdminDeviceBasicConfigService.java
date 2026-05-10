package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminDeviceBasicConfigService {
    public static final int MAX_CHANNEL_LENGTH = 128;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminDeviceBasicConfigService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public WebAdminDtos.DeviceBasicConfigDto configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String deviceId
    ) {
        SignalDeviceData device = findDevice(server, deviceId);
        return device == null ? null : configFor(server, user, session, device);
    }

    public WebAdminDtos.DeviceBasicConfigDto configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            SignalDeviceData rawDevice
    ) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return null;
        }
        Support support = support(server, device);
        return new WebAdminDtos.DeviceBasicConfigDto(
                device.id(),
                WebAdminReadonlySupport.deviceType(device),
                device.enabled(),
                device.channel(),
                support.supported(),
                support.supported(),
                support.supported(),
                support.reason(),
                fingerprintFor(device),
                editLockService == null ? null : editLockService.status(
                        WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG,
                        device.id(),
                        user,
                        session
                )
        );
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminDeviceBasicConfigUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String deviceId = request == null ? "" : safe(request.deviceId);
        SignalDeviceData device = findDevice(server, deviceId);
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "DEVICE_BASIC_CONFIG",
                deviceId,
                device == null ? deviceId : WebAdminReadonlySupport.deviceDisplayName(device)
        );
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG,
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

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, Map.of(), Map.of("attempt", "permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "origin_failed"));
            return result;
        }

        Support support = support(server, device);
        if (!support.supported()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported",
                    support.reason(),
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "unsupported"));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG,
                    device.id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(device), Map.of("attempt", "edit_lock_failed"));
                return result;
            }
        }

        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(device, request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, device, request.expectedFingerprint);
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }

        List<WebAdminValidationError> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }

        boolean enabled = (Boolean) request.enabled;
        String channel = SignalChannel.normalize(request.channel);
        if (device.enabled() == enabled && device.channel().equals(channel)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的设备基础配置变化。");
            audit(writeContext, result, currentSummary(device), currentSummary(device));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        SignalDeviceData updated = SignalDeviceStore.updateBasicConfig(server, device.id(), enabled, channel);
        if (updated == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：方块设备可能未加载，WebAdmin 不会强制加载区块。",
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }

        List<String> changedFields = changedFields(device, updated);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("basicConfig", configFor(server, user, session, updated));
        data.put("changedFields", changedFields);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "设备基础配置已保存，修改会立即影响当前世界中的设备行为。",
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
        WebAdminAuditEvent auditEvent = audit(writeContext, result, currentSummary(device), currentSummary(updated));
        publishRealtime(device, updated, auditEvent, changedFields, user);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public static boolean fingerprintMatches(SignalDeviceData device, String expectedFingerprint) {
        return !isBlank(expectedFingerprint) && fingerprintFor(device).equals(expectedFingerprint);
    }

    public static String fingerprintFor(SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        String input = "device_basic_config|"
                + device.id() + "|"
                + device.type() + "|"
                + device.enabled() + "|"
                + SignalChannel.normalize(device.channel());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateRequest(WebAdminDeviceBasicConfigUpdateRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (request == null || !(request.enabled instanceof Boolean)) {
            errors.add(new WebAdminValidationError("enabled", "invalid_boolean", "启用状态必须是 boolean。", request == null ? "" : String.valueOf(request.enabled)));
        }
        String rawChannel = request == null ? "" : safe(request.channel);
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            errors.add(new WebAdminValidationError("channel", "empty_not_supported", "7.2 暂不支持清空主频道，请后续使用专门的频道管理功能。", rawChannel));
        } else if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "主频道长度不能超过 128 个字符。", rawChannel));
        } else if (containsControl(channel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "主频道不能包含控制字符。", rawChannel));
        } else if (!SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "主频道只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        return List.copyOf(errors);
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private static Support support(MinecraftServer server, SignalDeviceData device) {
        if (device == null) {
            return new Support(false, "设备不存在。");
        }
        return switch (device.type()) {
            case SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE -> new Support(true, "");
            case SignalDeviceData.TYPE_SIGNAL_EMITTER,
                    SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                    SignalDeviceData.TYPE_ACTION_RELAY -> new Support(true, "");
            default -> new Support(false, "该设备类型暂不支持 WebAdmin 基础配置编辑。");
        };
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
            SignalDeviceData before,
            SignalDeviceData device,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String deviceId = device.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("设备基础配置已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "device_basic_config")
                .payload("deviceType", device.type())
                .payload("changedFields", changedFields)
                .payload("previousChannel", before == null ? "" : before.channel())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("设备基础配置已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("targetType", "device_basic_config")
                .payload("deviceType", device.type())
                .payload("changedFields", changedFields)
                .payload("enabled", device.enabled())
                .payload("channel", device.channel())
                .payload("previousChannel", before == null ? "" : before.channel())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "device_basic_config")
                .payload("deviceType", device.type())
                .payload("previousChannel", before == null ? "" : before.channel())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id()));
    }

    private void releaseLockAfterWrite(
            WebAdminDeviceBasicConfigUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || request.lockId == null || request.lockId.isBlank()) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG,
                request.deviceId,
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(
            WebAdminWriteTarget target,
            SignalDeviceData current,
            String expectedFingerprint
    ) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentBasicConfig", currentSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "设备基础配置已被其他操作修改，请刷新后再编辑。",
                target.targetType(),
                target.targetId(),
                false,
                List.of(),
                "",
                "",
                false,
                conflict,
                Map.of()
        );
    }

    private static List<String> changedFields(SignalDeviceData before, SignalDeviceData after) {
        List<String> changed = new ArrayList<>();
        if (before.enabled() != after.enabled()) {
            changed.add("enabled");
        }
        if (!before.channel().equals(after.channel())) {
            changed.add("channel");
        }
        return List.copyOf(changed);
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("enabled", device.enabled());
        summary.put("channel", device.channel());
        summary.put("expectedFingerprint", fingerprintFor(device));
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminDeviceBasicConfigUpdateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("enabled", request.enabled);
        summary.put("channel", SignalChannel.normalize(request.channel));
        summary.put("expectedFingerprint", request.expectedFingerprint);
        return summary;
    }

    private static boolean containsControl(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private record Support(boolean supported, String reason) {
    }
}
