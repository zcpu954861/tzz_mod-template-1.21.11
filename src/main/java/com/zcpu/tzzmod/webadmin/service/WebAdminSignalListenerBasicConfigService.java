package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
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
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminSignalListenerBasicConfigService {
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final int MAX_COOLDOWN_TICKS = 72_000;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator gateBindingValidator;
    private final Path testStorePath;

    public WebAdminSignalListenerBasicConfigService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null);
    }

    WebAdminSignalListenerBasicConfigService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path testStorePath
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.testStorePath = testStorePath;
        this.gateBindingValidator = new WebAdminConditionGateBindingValidator(conditionGroupTestPath(testStorePath));
    }

    private static Path conditionGroupTestPath(Path signalListenerTestStorePath) {
        return signalListenerTestStorePath == null || signalListenerTestStorePath.getParent() == null
                ? null
                : signalListenerTestStorePath.getParent().resolve(WebAdminConditionGroupStore.FILE_NAME);
    }

    public WebAdminDtos.SignalListenerBasicConfigDto configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String listenerRef
    ) {
        SignalListenerData listener = findListener(server, listenerRef);
        return listener == null ? null : dto(listener, user, session);
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSignalListenerBasicConfigUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String listenerRef = request == null ? "" : safe(request.listenerRef);
        SignalListenerData listener = findListener(server, listenerRef);
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "SIGNAL_LISTENER_BASIC_CONFIG",
                listener == null ? listenerRef : listener.id(),
                listener == null ? listenerRef : displayName(listener)
        );
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG,
                target
        );
        if (listener == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of());
            return result;
        }
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(context, result, currentSummary(listener), Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(context, result, currentSummary(listener), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
            audit(context, result, currentSummary(listener), Map.of("attempt", "origin_failed"));
            return result;
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG,
                    listener.id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(context, result, currentSummary(listener), Map.of("attempt", "edit_lock_failed"));
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
            audit(context, result, currentSummary(listener), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintFor(listener).equals(request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, listener, request.expectedFingerprint);
            audit(context, result, currentSummary(listener), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        List<WebAdminValidationError> errors = new ArrayList<>(validateRequest(request));
        gateBindingValidator.validate(
                server,
                errors,
                "conditionGroupId",
                request == null ? "" : request.conditionGroupId,
                ConditionRuntimeTargetType.SIGNAL_LISTENER
        );
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, currentSummary(listener), requestSummary(request));
            return result;
        }
        boolean enabled = (Boolean) request.enabled;
        String channel = SignalChannel.normalize(request.channel);
        int cooldownTicks = toInteger(request.cooldownTicks);
        String conditionGroupId = WebAdminConditionGroupStore.normalizeId(request.conditionGroupId);
        if (listener.enabled() == enabled
                && listener.channel().equals(channel)
                && listener.cooldownTicks() == cooldownTicks
                && listener.conditionGroupId().equals(conditionGroupId)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Listener 基础配置变化。");
            audit(context, result, currentSummary(listener), currentSummary(listener));
            releaseLockAfterWrite(request, listener, user, session, remoteAddress);
            return result;
        }

        SignalListenerData updated = updateBasicConfig(server, listener.id(), enabled, channel, cooldownTicks, conditionGroupId);
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        if (updated == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或已被删除。");
            audit(context, result, currentSummary(listener), requestSummary(request));
            return result;
        }
        List<String> changedFields = changedFields(listener, updated);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listenerConfig", dto(updated, user, session));
        data.put("changedFields", changedFields);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Signal Listener 基础配置已保存。",
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
        WebAdminAuditEvent auditEvent = audit(context, result, currentSummary(listener), currentSummary(updated));
        publishRealtime(listener, updated, auditEvent, changedFields, user);
        releaseLockAfterWrite(request, updated, user, session, remoteAddress);
        return result;
    }

    public WebAdminDtos.SignalListenerBasicConfigDto dto(SignalListenerData rawListener, WebAdminUser user, WebAdminSession session) {
        SignalListenerData listener = rawListener == null ? null : rawListener.normalized();
        if (listener == null) {
            return null;
        }
        Map<String, Object> recentConditionGate = WebAdminConditionGateHistoryService.recentStatus(
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                listener.id()
        );
        return new WebAdminDtos.SignalListenerBasicConfigDto(
                listener.id(),
                listener.id(),
                displayName(listener),
                listener.enabled(),
                listener.channel(),
                listener.cooldownTicks(),
                listener.conditionGroupId(),
                ConditionRuntimeTargetType.SIGNAL_LISTENER.id(),
                listener.id(),
                listener.actions().size(),
                actionSummaries(listener),
                fingerprintFor(listener),
                editLockService == null ? null : editLockService.status(
                        WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG,
                        listener.id(),
                        user,
                        session
                ),
                recentConditionGate
        );
    }

    public static String fingerprintFor(SignalListenerData rawListener) {
        SignalListenerData listener = rawListener == null ? null : rawListener.normalized();
        if (listener == null) {
            return "";
        }
        String input = "signal_listener_basic_config|"
                + listener.id() + "|"
                + listener.name() + "|"
                + listener.enabled() + "|"
                + SignalChannel.normalize(listener.channel()) + "|"
                + listener.cooldownTicks() + "|"
                + WebAdminConditionGroupStore.normalizeId(listener.conditionGroupId()) + "|"
                + WebAdminJsonResponse.GSON.toJson(listener.actions());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateRequest(WebAdminSignalListenerBasicConfigUpdateRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (request == null || !(request.enabled instanceof Boolean)) {
            errors.add(new WebAdminValidationError("enabled", "invalid_boolean", "启用状态必须是 boolean。", request == null ? "" : String.valueOf(request.enabled)));
        }
        String rawChannel = request == null ? "" : safe(request.channel);
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            errors.add(new WebAdminValidationError("channel", "required", "Listener 频道不能为空。", rawChannel));
        } else if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "Listener 频道长度不能超过 128 个字符。", rawChannel));
        } else if (containsControl(channel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "Listener 频道不能包含控制字符。", rawChannel));
        } else if (!SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "Listener 频道只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        if (!isInteger(request == null ? null : request.cooldownTicks)) {
            errors.add(new WebAdminValidationError("cooldownTicks", "invalid_integer", "冷却时间必须是整数 tick。", request == null ? "" : String.valueOf(request.cooldownTicks)));
        } else {
            int ticks = toInteger(request.cooldownTicks);
            if (ticks < 0) {
                errors.add(new WebAdminValidationError("cooldownTicks", "negative", "冷却时间不能为负数。", String.valueOf(ticks)));
            } else if (ticks > MAX_COOLDOWN_TICKS) {
                errors.add(new WebAdminValidationError("cooldownTicks", "too_large", "冷却时间不能超过 72000 tick。", String.valueOf(ticks)));
            }
        }
        return List.copyOf(errors);
    }

    private SignalListenerData findListener(MinecraftServer server, String listenerRef) {
        if ((server == null && testStorePath == null) || isBlank(listenerRef)) {
            return null;
        }
        SignalListenerStore.ResolveResult resolved = testStorePath == null
                ? SignalListenerStore.resolveListener(server, listenerRef)
                : SignalListenerStore.resolveListener(testStorePath, listenerRef);
        return resolved.foundUnique() ? resolved.listener().normalized() : null;
    }

    private SignalListenerData updateBasicConfig(
            MinecraftServer server,
            String listenerId,
            boolean enabled,
            String channel,
            int cooldownTicks,
            String conditionGroupId
    ) {
        return testStorePath == null
                ? SignalListenerStore.updateBasicConfigForWebAdmin(server, listenerId, enabled, channel, cooldownTicks, conditionGroupId)
                : SignalListenerStore.updateBasicConfigForWebAdmin(testStorePath, listenerId, enabled, channel, cooldownTicks, conditionGroupId);
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(
            SignalListenerData before,
            SignalListenerData listener,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String routeTarget = "#/signals/" + encode(listener.channel());
        String previousChannel = before == null ? "" : before.channel();
        List<String> affectedChannels = affectedChannels(previousChannel, listener.channel());
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("Signal Listener 基础配置已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "signal_listener_basic_config")
                .payload("listenerId", listener.id())
                .payload("changedFields", changedFields)
                .payload("previousChannel", previousChannel)
                .payload("affectedChannels", affectedChannels)
                .payload("conditionGroupId", listener.conditionGroupId())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent listenerEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_LISTENER_CONFIG_CHANGED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("Signal Listener 基础配置已更新：" + displayName(listener))
                .routeTarget(routeTarget)
                .payload("listenerId", listener.id())
                .payload("changedFields", changedFields)
                .payload("enabled", listener.enabled())
                .payload("channel", listener.channel())
                .payload("previousChannel", previousChannel)
                .payload("affectedChannels", affectedChannels)
                .payload("cooldownTicks", listener.cooldownTicks())
                .payload("conditionGroupId", listener.conditionGroupId())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("listenerEventId", listenerEvent == null ? "" : listenerEvent.id())
                .payload("previousChannel", previousChannel)
                .payload("affectedChannels", affectedChannels));
    }

    private static List<String> affectedChannels(String beforeChannel, String afterChannel) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        String before = SignalChannel.normalize(beforeChannel);
        String after = SignalChannel.normalize(afterChannel);
        if (!before.isBlank()) {
            channels.add(before);
        }
        if (!after.isBlank()) {
            channels.add(after);
        }
        return List.copyOf(channels);
    }

    private void releaseLockAfterWrite(
            WebAdminSignalListenerBasicConfigUpdateRequest request,
            SignalListenerData listener,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || isBlank(request.lockId) || listener == null) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG,
                listener.id(),
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(WebAdminWriteTarget target, SignalListenerData current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("current", currentSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "Signal Listener 基础配置已被其他操作修改，请刷新后再编辑。",
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

    private static List<String> actionSummaries(SignalListenerData listener) {
        List<String> summaries = new ArrayList<>();
        for (ActionConfig action : listener.actions()) {
            if (action == null) {
                continue;
            }
            summaries.add(action.type().id() + (action.enabled() ? " enabled" : " disabled"));
            if (summaries.size() >= 5) {
                break;
            }
        }
        return List.copyOf(summaries);
    }

    private static List<String> changedFields(SignalListenerData before, SignalListenerData after) {
        List<String> fields = new ArrayList<>();
        if (before.enabled() != after.enabled()) {
            fields.add("enabled");
        }
        if (!before.channel().equals(after.channel())) {
            fields.add("channel");
        }
        if (before.cooldownTicks() != after.cooldownTicks()) {
            fields.add("cooldownTicks");
        }
        if (!before.conditionGroupId().equals(after.conditionGroupId())) {
            fields.add("conditionGroupId");
        }
        return List.copyOf(fields);
    }

    private static Map<String, Object> currentSummary(SignalListenerData listener) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (listener == null) {
            return summary;
        }
        summary.put("listenerId", listener.id());
        summary.put("name", listener.name());
        summary.put("enabled", listener.enabled());
        summary.put("channel", listener.channel());
        summary.put("cooldownTicks", listener.cooldownTicks());
        summary.put("conditionGroupId", listener.conditionGroupId());
        summary.put("actionCount", listener.actions().size());
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminSignalListenerBasicConfigUpdateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("enabled", String.valueOf(request.enabled));
        summary.put("channel", SignalChannel.normalize(request.channel));
        summary.put("cooldownTicks", String.valueOf(request.cooldownTicks));
        summary.put("conditionGroupId", WebAdminConditionGroupStore.normalizeId(request.conditionGroupId));
        return summary;
    }

    private static boolean isInteger(Object value) {
        if (!(value instanceof Number number)) {
            return false;
        }
        double doubleValue = number.doubleValue();
        return Double.isFinite(doubleValue) && Math.rint(doubleValue) == doubleValue
                && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE;
    }

    private static int toInteger(Object value) {
        return ((Number) value).intValue();
    }

    private static boolean containsControl(String value) {
        String safeValue = safe(value);
        for (int i = 0; i < safeValue.length(); i++) {
            if (Character.isISOControl(safeValue.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String displayName(SignalListenerData listener) {
        String name = listener.name();
        return name == null || name.isBlank() ? "Listener " + SignalListenerStore.shortId(listener.id()) : name;
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
