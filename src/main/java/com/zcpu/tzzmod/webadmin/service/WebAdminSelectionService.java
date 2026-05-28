package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.draft.WebAdminProtectedDraftRegistry;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionDraft;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionPurpose;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSession;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSessions;
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
    private final WebAdminEditLockService editLockService;

    public WebAdminSelectionService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this(permissionService, securityService, null);
    }

    public WebAdminSelectionService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
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

        WebAdminSelectionPurpose purpose = WebAdminSelectionPurpose.parse(request == null ? "" : request.purpose);
        WebAdminWriteResult lock = validateLogicChainSelectionLock(user, session, request, purpose);
        if (!lock.success()) {
            audit(context, lock, Map.of(), requestSummary(request));
            return lock;
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
        if (purpose != WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE) {
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry protectedDraft = WebAdminProtectedDraftRegistry.start(
                    draft.draftSessionId(),
                    draft.editLockId(),
                    user,
                    session,
                    targetPlayer.getUuidAsString(),
                    protectedDraftObjectType(purpose),
                    draft.logicChainDraftNodeId(),
                    java.util.Set.of("select", "configure", "commit", "cancel")
            );
            if (protectedDraft == null) {
                WebAdminWriteResult result = new WebAdminWriteResult(
                        false,
                        WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                        "Logic Chain protected draft 已存在或已结束，请重新发起新的草稿选择。",
                        "PROTECTED_DRAFT",
                        draft.draftSessionId(),
                        false,
                        List.of(new WebAdminValidationError(
                                "draftSessionId",
                                "protected_draft_session_conflict",
                                "draftSessionId 已被已有 protected draft 占用，不能覆盖其它选择会话。",
                                draft.draftSessionId()
                        )),
                        "",
                        "",
                        false,
                        WebAdminProtectedDraftRegistry.summary(draft.draftSessionId()),
                        Map.of()
                );
                audit(context, result, Map.of(), requestSummary(request));
                return result;
            }
        }
        return WebAdminSelectionSessions.startSession(server, targetPlayer, context, purpose, draft);
    }

    public WebAdminWriteResult cancel(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSelectionCancelRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String selectionId = request == null ? "" : safe(request.selectionId);
        String draftSessionId = firstNonBlank(
                request == null ? "" : request.protectedDraftId,
                request == null ? "" : request.draftSessionId
        );
        boolean cleanupProtectedDraft = truthy(request == null ? Boolean.FALSE : request.cleanupProtectedDraft) && !draftSessionId.isBlank();
        WebAdminWriteTarget target = cleanupProtectedDraft
                ? new WebAdminWriteTarget("PROTECTED_DRAFT", draftSessionId, "Logic Chain 受保护草稿")
                : selectionTarget(selectionId);
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
        if (!truthy(request == null ? Boolean.FALSE : request.confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    cleanupProtectedDraft ? "protectedDraftId" : "selectionId",
                    "webadmin_selection_cancel_confirmation_required",
                    "取消游戏内选择需要 WebUI 二次确认。",
                    cleanupProtectedDraft ? draftSessionId : selectionId
            )));
            audit(context, result, Map.of(), Map.of("attempt", "cancel_without_confirmation"));
            return result;
        }
        if (cleanupProtectedDraft) {
            return WebAdminSelectionSessions.cancelProtectedDraftFromWebAdmin(
                    server,
                    context,
                    draftSessionId,
                    request == null ? "" : request.reason
            );
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
        if (purpose == null) {
            errors.add(new WebAdminValidationError(
                    "purpose",
                    "unsupported",
                    "选择用途不受支持。",
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
        boolean channelRequired = purpose == WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE;
        if (channelRequired && channel.isBlank()) {
            errors.add(new WebAdminValidationError("channel", "required", "虚拟方块设备 channel 不能为空。", rawChannel));
        } else if (!channel.isBlank() && channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "channel 长度不能超过 128 个字符。", rawChannel));
        } else if (!channel.isBlank() && containsControl(channel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "channel 不能包含控制字符。", rawChannel));
        } else if (!channel.isBlank() && !SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "channel 只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        if (purpose != null && purpose != WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE) {
            if (safe(request == null ? "" : request.draftSessionId).isBlank()) {
                errors.add(new WebAdminValidationError("draftSessionId", "required", "Logic Chain 客户端辅助选择需要 draftSessionId。", ""));
            }
            if (safe(request == null ? "" : request.editLockId).isBlank()) {
                errors.add(new WebAdminValidationError("editLockId", "required", "Logic Chain 客户端辅助选择需要 editLockId。", ""));
            }
            if (safe(request == null ? "" : request.logicChainRootType).isBlank()) {
                errors.add(new WebAdminValidationError("logicChainRootType", "required", "Logic Chain 客户端辅助选择需要 rootType，用于校验编辑锁归属。", ""));
            }
            if (safe(request == null ? "" : request.logicChainRootRef).isBlank()) {
                errors.add(new WebAdminValidationError("logicChainRootRef", "required", "Logic Chain 客户端辅助选择需要 rootRef，用于校验编辑锁归属。", ""));
            }
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

    private WebAdminWriteResult validateLogicChainSelectionLock(
            WebAdminUser user,
            WebAdminSession session,
            WebAdminSelectionStartRequest request,
            WebAdminSelectionPurpose purpose
    ) {
        if (purpose == null || purpose == WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE || editLockService == null) {
            return WebAdminWriteResult.ok(selectionTarget(""), false, "选择编辑锁校验通过。");
        }
        String targetId = logicChainTargetId(request);
        WebAdminEditLockService.LockValidation validation = editLockService.validateLock(
                WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR,
                targetId,
                request == null ? "" : request.editLockId,
                user,
                session
        );
        if (!validation.success()) {
            return validation.result();
        }
        return WebAdminWriteResult.ok(selectionTarget(""), false, "Logic Chain 编辑锁校验通过。");
    }

    private WebAdminSelectionDraft normalizedDraft(WebAdminSelectionStartRequest request) {
        boolean enabled = request == null || !(request.enabled instanceof Boolean bool) || bool;
        return new WebAdminSelectionDraft(
                SignalChannel.normalize(request == null ? "" : request.channel),
                request == null ? "" : safe(request.displayName),
                request == null ? "" : safe(request.note),
                request == null || safe(request.iconKey).isBlank() ? "auto" : safe(request.iconKey).toLowerCase(),
                enabled,
                request == null ? "" : safe(request.draftSessionId),
                request == null ? "" : safe(request.editLockId),
                request == null ? "" : safe(request.logicChainRootType),
                request == null ? "" : safe(request.logicChainRootRef),
                request == null ? "" : safe(request.logicChainDraftNodeId)
        );
    }

    private static String protectedDraftObjectType(WebAdminSelectionPurpose purpose) {
        if (purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            return WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE;
        }
        if (purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT) {
            return WebAdminProtectedDraftRegistry.OBJECT_TYPE_REGION_CONTROLLER;
        }
        if (purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_ITEM_SUBMIT_CAPTURE) {
            return WebAdminProtectedDraftRegistry.OBJECT_TYPE_ITEM_SUBMIT_CAPTURE;
        }
        if (purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_CONTAINER_CAPTURE) {
            return WebAdminProtectedDraftRegistry.OBJECT_TYPE_CONTAINER_CAPTURE;
        }
        return WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE;
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
        summary.put("draftSessionId", safe(request.draftSessionId));
        summary.put("editLockId", safe(request.editLockId));
        summary.put("logicChainRootType", safe(request.logicChainRootType));
        summary.put("logicChainRootRef", safe(request.logicChainRootRef));
        summary.put("logicChainDraftNodeId", safe(request.logicChainDraftNodeId));
        return summary;
    }

    private static WebAdminWriteTarget selectionTarget(String selectionId) {
        return new WebAdminWriteTarget("OBJECT_SELECTION", safe(selectionId), "新建虚拟方块设备选择");
    }

    private static String logicChainTargetId(WebAdminSelectionStartRequest request) {
        return (normalizeLogicChainRootType(request == null ? "" : request.logicChainRootType) + ":" + safe(request == null ? "" : request.logicChainRootRef))
                .replaceAll("[\\r\\n\\t]", "_");
    }

    private static String normalizeLogicChainRootType(String rootType) {
        String value = safe(rootType).toLowerCase();
        return switch (value) {
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join", "timer" -> value;
            default -> "channel";
        };
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

    private static String firstNonBlank(String first, String second) {
        String cleanFirst = safe(first);
        return cleanFirst.isBlank() ? safe(second) : cleanFirst;
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
