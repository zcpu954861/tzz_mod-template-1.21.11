package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionDraft;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionPurpose;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSession;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSessions;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminSelectionService {
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final int MAX_TARGET_PLAYER_LENGTH = 64;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;

    public WebAdminSelectionService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
    }

    public WebAdminWriteResult start(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSelectionStartRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = selectionTarget("");
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.START_OBJECT_SELECTION,
                target
        );
        WebAdminWriteResult gate = writeGate(user, session, csrfToken, sameOrigin, target, context);
        if (!gate.success()) {
            return gate;
        }

        List<WebAdminValidationError> errors = validateStartRequest(request);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(request));
            return result;
        }

        ServerPlayerEntity targetPlayer = findOnlinePlayer(server, request.targetPlayerName);
        if (targetPlayer == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标玩家不在线，请确认玩家名后重试。");
            audit(context, result, Map.of(), requestSummary(request));
            return result;
        }
        WebAdminSelectionSession existing = WebAdminSelectionSessions.activeFor(targetPlayer.getUuid());
        if (existing != null) {
            Map<String, Object> conflict = WebAdminSelectionSessions.status(existing.selectionId);
            WebAdminWriteResult result = new WebAdminWriteResult(
                    false,
                    WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                    "该玩家已有进行中的 WebAdmin 选择，请先取消后再开始新的选择。",
                    "OBJECT_SELECTION",
                    existing.selectionId,
                    false,
                    List.of(),
                    "",
                    "",
                    false,
                    conflict,
                    Map.of("selection", conflict)
            );
            audit(context, result, Map.of(), conflict);
            return result;
        }
        WebAdminSelectionDraft draft = normalizedDraft(request);
        return WebAdminSelectionSessions.startSession(server, targetPlayer, context, draft);
    }

    public WebAdminWriteResult cancel(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSelectionCancelRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String selectionId = request == null ? "" : safe(request.selectionId);
        WebAdminWriteTarget target = selectionTarget(selectionId);
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.START_OBJECT_SELECTION,
                target
        );
        WebAdminWriteResult gate = writeGate(user, session, csrfToken, sameOrigin, target, context);
        if (!gate.success()) {
            return gate;
        }
        if (selectionId.isBlank()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "selectionId",
                    "required",
                    "取消选择需要 selectionId。",
                    ""
            )));
            audit(context, result, Map.of(), Map.of("attempt", "missing_selection_id"));
            return result;
        }
        return WebAdminSelectionSessions.cancelFromWebAdmin(selectionId, context, request == null ? "" : request.reason);
    }

    public Map<String, Object> status(String selectionId) {
        return WebAdminSelectionSessions.status(selectionId);
    }

    public static List<WebAdminValidationError> validateStartRequest(WebAdminSelectionStartRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        WebAdminSelectionPurpose purpose = WebAdminSelectionPurpose.parse(request == null ? "" : request.purpose);
        if (purpose != WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE) {
            errors.add(new WebAdminValidationError(
                    "purpose",
                    "unsupported",
                    "本阶段只支持 create_virtual_block_device 选择用途。",
                    request == null ? "" : safe(request.purpose)
            ));
        }
        String playerName = request == null ? "" : safe(request.targetPlayerName);
        if (playerName.isBlank()) {
            errors.add(new WebAdminValidationError("targetPlayerName", "required", "目标玩家名不能为空。", ""));
        } else if (playerName.length() > MAX_TARGET_PLAYER_LENGTH || containsControl(playerName)) {
            errors.add(new WebAdminValidationError("targetPlayerName", "invalid", "目标玩家名格式无效。", playerName));
        }
        String rawChannel = request == null ? "" : safe(request.channel);
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            errors.add(new WebAdminValidationError("channel", "required", "虚拟方块设备 channel 不能为空。", rawChannel));
        } else if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "channel 长度不能超过 128 个字符。", rawChannel));
        } else if (containsControl(channel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "channel 不能包含控制字符。", rawChannel));
        } else if (!SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "channel 只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        Object enabled = request == null ? Boolean.TRUE : request.enabled;
        if (!(enabled instanceof Boolean)) {
            errors.add(new WebAdminValidationError("enabled", "invalid_boolean", "启用状态必须是 boolean。", String.valueOf(enabled)));
        }
        if (!WebAdminDeviceMetadataService.validateRequest(metadataRequest(request)).isEmpty()) {
            errors.addAll(WebAdminDeviceMetadataService.validateRequest(metadataRequest(request)));
        }
        return List.copyOf(errors);
    }

    private WebAdminWriteResult writeGate(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            WebAdminWriteContext context
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.START_OBJECT_SELECTION);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(context, result, Map.of(), Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(context, result, Map.of(), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
            audit(context, result, Map.of(), Map.of("attempt", "origin_failed"));
            return result;
        }
        return WebAdminWriteResult.ok(target, false, "写入安全检查通过。");
    }

    private WebAdminSelectionDraft normalizedDraft(WebAdminSelectionStartRequest request) {
        boolean enabled = request == null || !(request.enabled instanceof Boolean bool) || bool;
        return new WebAdminSelectionDraft(
                SignalChannel.normalize(request == null ? "" : request.channel),
                request == null ? "" : safe(request.displayName),
                request == null ? "" : safe(request.note),
                request == null || safe(request.iconKey).isBlank() ? "auto" : safe(request.iconKey).toLowerCase(),
                enabled
        );
    }

    private ServerPlayerEntity findOnlinePlayer(MinecraftServer server, String name) {
        if (server == null || isBlank(name)) {
            return null;
        }
        String expected = safe(name);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getName().getString().equalsIgnoreCase(expected)) {
                return player;
            }
        }
        return null;
    }

    private WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> before,
            Map<String, ?> after
    ) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(auditEvent);
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity(result != null && result.success() ? "INFO" : "WARNING")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget("#/history")
                .payload("auditId", auditEvent.auditId())
                .payload("operation", auditEvent.operationType())
                .payload("targetType", auditEvent.targetType())
                .payload("targetId", auditEvent.targetId()));
        return auditEvent;
    }

    private static com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest metadataRequest(WebAdminSelectionStartRequest request) {
        com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest metadata = new com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest();
        metadata.displayName = request == null ? "" : safe(request.displayName);
        metadata.note = request == null ? "" : safe(request.note);
        metadata.iconKey = request == null || safe(request.iconKey).isBlank() ? "auto" : safe(request.iconKey).toLowerCase();
        return metadata;
    }

    private static Map<String, Object> requestSummary(WebAdminSelectionStartRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("purpose", safe(request.purpose));
        summary.put("targetPlayerName", safe(request.targetPlayerName));
        summary.put("channel", SignalChannel.normalize(request.channel));
        summary.put("displayName", safe(request.displayName));
        summary.put("noteLength", safe(request.note).length());
        summary.put("iconKey", safe(request.iconKey));
        summary.put("enabled", request.enabled);
        return summary;
    }

    private static WebAdminWriteTarget selectionTarget(String selectionId) {
        return new WebAdminWriteTarget("OBJECT_SELECTION", safe(selectionId), "新建虚拟方块设备选择");
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static boolean containsControl(String value) {
        String safe = safe(value);
        for (int i = 0; i < safe.length(); i++) {
            if (Character.isISOControl(safe.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
