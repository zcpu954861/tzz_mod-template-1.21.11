package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerDeleteRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminSignalListenerLifecycleService {
    public static final int MAX_LISTENER_NAME_LENGTH = 64;
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final int MAX_COOLDOWN_TICKS = WebAdminSignalListenerBasicConfigService.MAX_COOLDOWN_TICKS;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;

    public WebAdminSignalListenerLifecycleService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSignalListenerCreateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = new WebAdminWriteTarget("SIGNAL_LISTENER", "", "新建 Signal Listener");
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.CREATE_SIGNAL_LISTENER,
                target
        );
        WebAdminWriteResult gate = writeGate(user, session, csrfToken, sameOrigin, target, context, Map.of(), WebAdminOperationType.CREATE_SIGNAL_LISTENER);
        if (!gate.success()) {
            return gate;
        }

        List<WebAdminValidationError> errors = validateCreateRequest(request);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), createRequestSummary(request));
            return result;
        }

        String name = listenerName(request);
        String channel = SignalChannel.normalize(request.channel);
        for (SignalListenerData listener : SignalListenerStore.getSnapshot(server)) {
            if (!name.isBlank() && listener.name().equalsIgnoreCase(name)) {
                WebAdminWriteResult result = new WebAdminWriteResult(
                        false,
                        WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                        "已存在同名 Signal Listener，请换一个名称。",
                        target.targetType(),
                        target.targetId(),
                        false,
                        List.of(),
                        "",
                        "",
                        false,
                        Map.of("name", name),
                        Map.of()
                );
                audit(context, result, Map.of(), createRequestSummary(request));
                return result;
            }
        }

        boolean enabled = request.enabled == null || (request.enabled instanceof Boolean bool && bool);
        int cooldownTicks = request.cooldownTicks == null ? SignalListenerData.DEFAULT_COOLDOWN_TICKS : ((Number) request.cooldownTicks).intValue();
        SignalListenerData listener = SignalListenerStore.createListener(server, channel, name, enabled, cooldownTicks).normalized();
        SignalListenerStore.flushDirty(server);
        WebAdminWriteTarget resultTarget = new WebAdminWriteTarget("SIGNAL_LISTENER", listener.id(), displayName(listener));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listener", listenerSummary(listener));
        data.put("listenerId", listener.id());
        data.put("routeTarget", listenerRoute(listener.id()));
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Signal Listener 已创建。当前监听器没有动作，可在后续动作编辑阶段配置。",
                resultTarget.targetType(),
                resultTarget.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data
        );
        WebAdminAuditEvent auditEvent = audit(
                WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.CREATE_SIGNAL_LISTENER, resultTarget),
                result,
                Map.of(),
                listenerSummary(listener)
        );
        publishRealtime(listener, auditEvent, user, false);
        return result;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String pathListenerId,
            WebAdminSignalListenerDeleteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String listenerId = firstNonBlank(pathListenerId, request == null ? "" : request.listenerId);
        WebAdminWriteTarget initialTarget = new WebAdminWriteTarget("SIGNAL_LISTENER", safe(listenerId), safe(listenerId));
        WebAdminWriteContext initialContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.DELETE_SIGNAL_LISTENER,
                initialTarget
        );
        WebAdminWriteResult initialGate = writeGate(user, session, csrfToken, sameOrigin, initialTarget, initialContext, Map.of(), WebAdminOperationType.DELETE_SIGNAL_LISTENER);
        if (!initialGate.success()) {
            return initialGate;
        }

        SignalListenerData listener = findListener(server, listenerId);
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "SIGNAL_LISTENER",
                safe(listenerId),
                listener == null ? safe(listenerId) : displayName(listener)
        );
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.DELETE_SIGNAL_LISTENER,
                target
        );
        if (listener == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "target_not_found"));
            return result;
        }
        WebAdminWriteResult confirmation = requireDangerConfirmation(request, target, listener);
        if (!confirmation.success()) {
            audit(context, confirmation, listenerSummary(listener), Map.of("attempt", "confirmation_required"));
            return confirmation;
        }

        boolean deleted = SignalListenerStore.deleteListener(server, listener.id());
        if (!deleted) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, listenerSummary(listener), Map.of("attempt", "already_removed"));
            return result;
        }
        SignalListenerStore.flushDirty(server);
        SignalBridgeServer.clearListenerRuntime(listener.id());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deletedListener", listenerSummary(listener));
        data.put("listenerId", listener.id());
        data.put("routeTarget", "#/listeners");
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Signal Listener 已删除。关联动作列表随该 Listener 一起删除，未删除 channel、receiver、device 或历史记录。",
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
        WebAdminAuditEvent auditEvent = audit(context, result, listenerSummary(listener), Map.of("removed", true, "reason", safe(request == null ? "" : request.reason)));
        publishRealtime(listener, auditEvent, user, true);
        return result;
    }

    public static List<WebAdminValidationError> validateCreateRequest(WebAdminSignalListenerCreateRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        String name = listenerName(request);
        if (name.isBlank()) {
            errors.add(new WebAdminValidationError("name", "required", "监听器名称不能为空。", ""));
        } else if (name.length() > MAX_LISTENER_NAME_LENGTH) {
            errors.add(new WebAdminValidationError("name", "too_long", "监听器名称不能超过 64 个字符。", name));
        } else if (containsControl(name)) {
            errors.add(new WebAdminValidationError("name", "control_character", "监听器名称不能包含控制字符。", name));
        }
        String rawChannel = request == null ? "" : safe(request.channel);
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            errors.add(new WebAdminValidationError("channel", "required", "监听频道不能为空。", rawChannel));
        } else if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "监听频道长度不能超过 128 个字符。", rawChannel));
        } else if (containsControl(channel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "监听频道不能包含控制字符。", rawChannel));
        } else if (!SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "监听频道只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        if (request != null && request.enabled != null && !(request.enabled instanceof Boolean)) {
            errors.add(new WebAdminValidationError("enabled", "invalid_boolean", "启用状态必须是 boolean。", String.valueOf(request.enabled)));
        }
        if (request != null && request.cooldownTicks != null) {
            if (!isInteger(request.cooldownTicks)) {
                errors.add(new WebAdminValidationError("cooldownTicks", "invalid_integer", "冷却时间必须是整数 tick。", String.valueOf(request.cooldownTicks)));
            } else {
                int ticks = ((Number) request.cooldownTicks).intValue();
                if (ticks < 0) {
                    errors.add(new WebAdminValidationError("cooldownTicks", "negative", "冷却时间不能为负数。", String.valueOf(ticks)));
                } else if (ticks > MAX_COOLDOWN_TICKS) {
                    errors.add(new WebAdminValidationError("cooldownTicks", "too_large", "冷却时间不能超过 72000 tick。", String.valueOf(ticks)));
                }
            }
        }
        return List.copyOf(errors);
    }

    private WebAdminWriteResult writeGate(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            WebAdminWriteContext context,
            Map<String, ?> before,
            WebAdminOperationType operationType
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, operationType);
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

    private WebAdminWriteResult requireDangerConfirmation(WebAdminSignalListenerDeleteRequest request, WebAdminWriteTarget target, SignalListenerData listener) {
        String confirmation = safe(request == null ? "" : request.confirmationText).trim();
        boolean confirmed = request != null && Boolean.TRUE.equals(request.confirmed);
        if (confirmed && (confirmation.equals(listener.id()) || (!displayName(listener).isBlank() && confirmation.equals(displayName(listener))))) {
            return WebAdminWriteResult.ok(target, false, "危险操作确认通过。");
        }
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION.id(),
                "删除 Signal Listener 前，需要勾选确认并输入 Listener ID 或名称。",
                target.targetType(),
                target.targetId(),
                false,
                List.of(new WebAdminValidationError("confirmationText", "required", "请输入 Listener ID 或名称以确认删除。", confirmation)),
                "",
                "",
                true,
                Map.of("expected", listener.id(), "actionCount", listener.actions().size()),
                Map.of()
        );
    }

    private SignalListenerData findListener(MinecraftServer server, String listenerId) {
        if (server == null || listenerId == null || listenerId.isBlank()) {
            return null;
        }
        SignalListenerStore.ResolveResult resolved = SignalListenerStore.resolveListener(server, listenerId);
        return resolved.foundUnique() ? resolved.listener().normalized() : null;
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private void publishRealtime(SignalListenerData listener, WebAdminAuditEvent auditEvent, WebAdminUser user, boolean removed) {
        String routeTarget = removed ? "#/listeners" : listenerRoute(listener.id());
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary(removed ? "Signal Listener 已删除。" : "Signal Listener 已创建。")
                .routeTarget(routeTarget)
                .payload("targetType", "signal_listener_lifecycle")
                .payload("listenerId", listener.id())
                .payload("removed", removed)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget("#/history")
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("listenerId", listener.id()));
    }

    private static Map<String, Object> createRequestSummary(WebAdminSignalListenerCreateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("name", listenerName(request));
        summary.put("channel", SignalChannel.normalize(request.channel));
        summary.put("enabled", request.enabled == null ? true : request.enabled);
        summary.put("cooldownTicks", request.cooldownTicks == null ? 0 : request.cooldownTicks);
        summary.put("actionsCreated", 0);
        return summary;
    }

    private static Map<String, Object> listenerSummary(SignalListenerData listener) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (listener == null) {
            return summary;
        }
        summary.put("listenerId", listener.id());
        summary.put("name", listener.name());
        summary.put("channel", listener.channel());
        summary.put("enabled", listener.enabled());
        summary.put("cooldownTicks", listener.cooldownTicks());
        summary.put("actionCount", listener.actions().size());
        return summary;
    }

    private static String listenerName(WebAdminSignalListenerCreateRequest request) {
        String name = safe(request == null ? "" : request.name);
        return name.isBlank() ? safe(request == null ? "" : request.displayName) : name;
    }

    private static String displayName(SignalListenerData listener) {
        return listener.name() == null || listener.name().isBlank() ? "Listener " + SignalListenerStore.shortId(listener.id()) : listener.name();
    }

    private static String listenerRoute(String listenerId) {
        return "#/listeners/" + encode(listenerId) + "?returnTo=%23%2Flisteners";
    }

    private static boolean isInteger(Object value) {
        if (!(value instanceof Number number)) {
            return false;
        }
        double doubleValue = number.doubleValue();
        return Double.isFinite(doubleValue) && Math.rint(doubleValue) == doubleValue
                && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE;
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
