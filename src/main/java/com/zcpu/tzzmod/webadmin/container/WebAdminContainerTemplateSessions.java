package com.zcpu.tzzmod.webadmin.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.WebAdminContainerTemplateS2CPayload;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionSupport;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import com.zcpu.tzzmod.util.NullSafety;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceContainerTemplateSessionService;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotService;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteAuditContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

public final class WebAdminContainerTemplateSessions {
    public static final long SESSION_TTL_MILLIS = 10L * 60L * 1000L;
    private static final Map<String, WebAdminContainerTemplateSession> SESSIONS_BY_ID = new LinkedHashMap<>();
    private static final Map<UUID, String> ACTIVE_BY_PLAYER = new LinkedHashMap<>();
    private static final Map<String, String> ACTIVE_BY_DEVICE = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> TERMINAL_STATUS = new LinkedHashMap<>();
    private static final Deque<String> TERMINAL_ORDER = new ArrayDeque<>();
    private static final int MAX_TERMINAL_STATUS = 128;
    private static MinecraftServer currentServer;
    private static WebAdminEditLockService lockService;

    private WebAdminContainerTemplateSessions() {
    }

    public static synchronized WebAdminWriteResult startSession(
            MinecraftServer server,
            ServerPlayerEntity targetPlayer,
            WebAdminEditLockService editLockService,
            WebAdminContainerTemplateSession session
    ) {
        expireOld();
        if (server == null || targetPlayer == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target(session == null ? "" : session.deviceId), "目标玩家不在线。");
        }
        currentServer = server;
        lockService = editLockService;
        if (activeForPlayer(targetPlayer.getUuid()) != null) {
            WebAdminContainerTemplateSession previous = activeForPlayer(targetPlayer.getUuid());
            return conflict(session.context, previous, "该玩家已有进行中的容器模板会话，请先取消后再开始。");
        }
        if (activeForDevice(session.deviceId) != null) {
            WebAdminContainerTemplateSession previous = activeForDevice(session.deviceId);
            return conflict(session.context, previous, "该 VBD 已有进行中的容器模板会话，请先取消后再开始。");
        }
        SESSIONS_BY_ID.put(session.sessionId, session);
        ACTIVE_BY_PLAYER.put(session.targetPlayerUuid, session.sessionId);
        ACTIVE_BY_DEVICE.put(session.deviceId, session.sessionId);
        try {
            sendOpen(targetPlayer, session);
        } catch (Exception exception) {
            removeActive(session);
            releaseLock(session, "GUI 打开失败，编辑锁已释放。");
            return failedResult(session, "gui_send_failed", "无法向目标玩家打开容器模板 GUI：" + exception.getMessage());
        }
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_STARTED, session, "容器变化模板会话已开始。", Map.of("status", "started"));
        Map<String, Object> data = baseStatus(session, "started");
        data.put("realtimeEventId", event == null ? "" : event.id());
        WebAdminWriteResult result = result(session, true, true, "已通知目标玩家打开容器变化模板 GUI。", data, event);
        audit(session.context, result, Map.of(), data);
        return result;
    }

    public static synchronized Map<String, Object> status(String sessionId) {
        expireOld();
        WebAdminContainerTemplateSession session = SESSIONS_BY_ID.get(safe(sessionId));
        if (session != null) {
            Map<String, Object> status = baseStatus(session, session.opened ? "opened" : "started");
            status.put("active", true);
            return status;
        }
        Map<String, Object> terminal = TERMINAL_STATUS.get(safe(sessionId));
        if (terminal != null) {
            return Map.copyOf(terminal);
        }
        return Map.of("active", false, "status", "not_found", "sessionId", safe(sessionId));
    }

    public static synchronized WebAdminWriteResult cancelFromWebAdmin(String sessionId, WebAdminWriteContext context, String reason) {
        expireOld();
        WebAdminContainerTemplateSession session = SESSIONS_BY_ID.get(safe(sessionId));
        if (session == null) {
            Map<String, Object> terminal = TERMINAL_STATUS.get(safe(sessionId));
            if (terminal != null) {
                Map<String, Object> idempotent = new LinkedHashMap<>(terminal);
                idempotent.put("alreadyTerminal", true);
                idempotent.put("idempotentNoOp", true);
                return new WebAdminWriteResult(true, WebAdminWriteResultCode.OK.id(), "容器模板会话已结束。", "CONTAINER_TEMPLATE_SESSION", safe(sessionId), false, List.of(), "", "", false, Map.of(), Map.of("containerTemplateSession", idempotent));
            }
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, new WebAdminWriteTarget("CONTAINER_TEMPLATE_SESSION", safe(sessionId), "容器变化模板会话"), "容器模板会话不存在或已经结束。");
        }
        if (context != null
                && context.actorRole() != WebAdminRole.OWNER
                && !session.context.actorUsername().equalsIgnoreCase(context.actorUsername())) {
            WebAdminWriteResult denied = WebAdminWriteResult.failed(WebAdminWriteResultCode.PERMISSION_DENIED, target(session.deviceId), "只能由发起者或 OWNER 取消该容器模板会话。");
            audit(context, denied, baseStatus(session, "active"), Map.of("attempt", "cancel_non_owner"));
            return denied;
        }
        return cancelSession(session, "webui", safe(reason).isBlank() ? "WebAdmin 已取消容器模板会话。" : reason, true, true);
    }

    public static synchronized void openedFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        JsonObject body = parse(bodyJson);
        WebAdminContainerTemplateSession session = validateClientSession(player, body);
        if (session == null) {
            return;
        }
        session.opened = true;
        Map<String, Object> data = baseStatus(session, "opened");
        publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_OPENED, session, "目标玩家已打开容器变化模板 GUI。", data);
    }

    public static synchronized void cancelFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        JsonObject body = parse(bodyJson);
        WebAdminContainerTemplateSession session = validateClientSession(player, body);
        if (session == null) {
            return;
        }
        cancelSession(session, getString(body, "reason").isBlank() ? "client_close" : getString(body, "reason"), "游戏内 GUI 已关闭，容器模板会话已取消。", true, true);
    }

    public static synchronized void saveFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        JsonObject body = parse(bodyJson);
        WebAdminContainerTemplateSession session = validateClientSession(player, body);
        if (session == null) {
            return;
        }
        saveSession(server, player, session, body);
    }

    public static synchronized void cancelForDisconnect(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        WebAdminContainerTemplateSession session = activeForPlayer(player.getUuid());
        if (session != null) {
            failSession(session, "player_disconnect", "目标玩家已断开连接，容器模板会话已取消。", false);
        }
    }

    public static synchronized void clearAll(MinecraftServer server, String reason) {
        List<WebAdminContainerTemplateSession> sessions = List.copyOf(SESSIONS_BY_ID.values());
        for (WebAdminContainerTemplateSession session : sessions) {
            failSession(session, safe(reason).isBlank() ? "server_cleanup" : safe(reason), "服务器正在停止，容器模板会话已取消。", true);
        }
        SESSIONS_BY_ID.clear();
        ACTIVE_BY_PLAYER.clear();
        ACTIVE_BY_DEVICE.clear();
        currentServer = null;
    }

    public static synchronized void expireOld(MinecraftServer server) {
        if (server != null) {
            currentServer = server;
        }
        expireOld();
    }

    private static void expireOld() {
        long now = System.currentTimeMillis();
        List<WebAdminContainerTemplateSession> expired = SESSIONS_BY_ID.values().stream()
                .filter(session -> session.expiresAtMillis <= now)
                .toList();
        for (WebAdminContainerTemplateSession session : expired) {
            expireSession(session);
        }
    }

    private static void expireSession(WebAdminContainerTemplateSession session) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        notifyPlayer(player, "容器模板会话已过期。", Formatting.YELLOW);
        sendEnd(player, "expired", session, "容器模板会话已过期。", Map.of("source", "timeout"));
        releaseLock(session, "容器模板会话过期，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "expired");
        after.put("source", "timeout");
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_EXPIRED, session, "容器模板会话已过期。", after);
        rememberTerminal(session, "expired", after);
        WebAdminWriteResult result = result(session, true, true, "容器模板会话已过期。", after, event);
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION), result, Map.of("status", "active"), after);
    }

    private static WebAdminWriteResult cancelSession(WebAdminContainerTemplateSession session, String source, String message, boolean notifyPlayer, boolean publish) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        if (notifyPlayer && player != null) {
            String terminalMessage = safe(message).isBlank() ? "容器模板会话已取消。" : message;
            notifyPlayer(player, terminalMessage, Formatting.YELLOW);
            sendEnd(player, "cancelled", session, terminalMessage, Map.of("source", source));
        }
        releaseLock(session, "容器模板会话取消，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "cancelled");
        after.put("source", source);
        after.put("message", message);
        WebAdminRealtimeEvent event = publish ? publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_CANCELLED, session, safe(message).isBlank() ? "容器模板会话已取消。" : message, after) : null;
        rememberTerminal(session, "cancelled", after);
        WebAdminWriteResult result = result(session, true, true, "容器模板会话已取消。", after, event);
        audit(contextFor(session, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION), result, Map.of("status", "active"), after);
        return result;
    }

    private static WebAdminWriteResult saveSession(
            MinecraftServer server,
            ServerPlayerEntity player,
            WebAdminContainerTemplateSession session,
            JsonObject body
    ) {
        MinecraftServer activeServer = server == null ? currentServer : server;
        SignalDeviceData before = currentDevice(activeServer, session);
        if (before == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(before.type())) {
            return failSession(session, "device_missing", "目标 virtual_block_device 不存在，容器模板保存失败。", true);
        }
        if (lockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = lockService.validateLock(
                    WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                    session.deviceId,
                    session.lockId,
                    session.actorUser,
                    session.actorSession
            );
            if (!lockValidation.success()) {
                return failSession(session, "edit_lock_invalid", lockValidation.result().message(), true);
            }
        }
        String requestFingerprint = getString(body, "expectedFingerprint");
        String currentFingerprint = WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor(before);
        if (!session.expectedFingerprint.equals(currentFingerprint)
                || (!requestFingerprint.isBlank() && !session.expectedFingerprint.equals(requestFingerprint))) {
            return failSession(session, "fingerprint_conflict", "容器模板已被其他操作修改，保存已取消。", true);
        }
        List<ContainerItemConditionData> nextConditions;
        try {
            nextConditions = parseItemConditions(body);
        } catch (IllegalArgumentException exception) {
            return failSession(session, "invalid_item_conditions", exception.getMessage(), true);
        }
        if (session.logicChainDraftOnly) {
            return saveLogicChainDraftOnlySession(player, session, nextConditions);
        }
        WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = WebAdminSnapshotService.createAutoBeforeTrustedWrite(
                activeServer,
                session.actorUser,
                WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                "Virtual Block Device",
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                session.deviceId,
                "保存 VBD 容器变化模板前自动保存"
        );
        if (!autoSnapshot.created() && !autoSnapshot.skipped()) {
            return failSession(session, "auto_snapshot_failed", "写入前自动保存点创建失败，容器模板保存已停止。请检查快照存储或损坏配置文件。", true);
        }
        SignalDeviceData after = SignalDeviceStore.updateVirtualItemConditionsForWebAdmin(activeServer, session.deviceId, nextConditions);
        if (after == null) {
            return failSession(session, "save_failed", "容器模板保存失败，设备状态未更新。", true);
        }
        removeActive(session);
        String message = "容器变化模板已保存。";
        if (player != null) {
            notifyPlayer(player, message, Formatting.GREEN);
            sendEnd(player, "saved", session, message, Map.of("source", "client_save"));
        }
        releaseLock(session, "容器模板保存完成，编辑锁已释放。");
        Map<String, Object> beforeSummary = templateSummary(before, session.expectedFingerprint);
        Map<String, Object> afterSummary = templateSummary(after, WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor(after));
        WebAdminRealtimeEvent sessionEvent = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_SAVED, session, message, afterSummary);
        Map<String, Object> status = baseStatus(session, "saved");
        status.put("message", message);
        status.put("itemConditionCount", after.itemConditions().size());
        status.put("expectedFingerprint", WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor(after));
        rememberTerminal(session, "saved", status);
        WebAdminWriteResult result = result(session, true, true, message, status, sessionEvent);
        WebAdminAuditEvent auditEvent = audit(contextFor(session, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE), result, beforeSummary, afterSummary);
        publishConfigChangedAfterSave(session, after, auditEvent);
        return result;
    }

    private static WebAdminWriteResult saveLogicChainDraftOnlySession(ServerPlayerEntity player, WebAdminContainerTemplateSession session, List<ContainerItemConditionData> conditions) {
        removeActive(session);
        String message = conditions.isEmpty()
                ? "Logic Chain container 捕获未产生 requirement，未修改正式 VBD。"
                : "Logic Chain container 捕获已回填草稿（" + conditions.size() + " 个 requirement）。";
        if (player != null) {
            notifyPlayer(player, message, Formatting.GREEN);
            sendEnd(player, "saved", session, message, Map.of("source", "logic_chain_draft_only"));
        }
        releaseLock(session, "Logic Chain container draft-only 捕获完成，编辑锁已释放。");
        Map<String, Object> status = baseStatus(session, "saved");
        status.put("message", message);
        status.put("logicChainDraftOnly", true);
        status.put("dataLogicChainVbdContainerCaptureSessionPurpose", true);
        status.put("logicChainCaptureDraftId", session.logicChainCaptureDraftId);
        status.put("logicChainEditLockId", session.logicChainEditLockId);
        status.put("logicChainRootType", session.logicChainRootType);
        status.put("logicChainRootRef", session.logicChainRootRef);
        status.put("logicChainDraftNodeId", session.logicChainDraftNodeId);
        status.put("logicChainTriggerKey", session.logicChainTriggerKey);
        status.put("logicChainRequirementIndex", session.logicChainRequirementIndex);
        status.put("logicChainContainerRequirements", logicChainContainerRequirements(conditions, session.logicChainCaptureDraftId));
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_SAVED, session, message, status);
        rememberTerminal(session, "saved", status);
        WebAdminWriteResult result = result(session, true, false, message, status, event);
        audit(contextFor(session, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE), result, Map.of("status", "active"), status);
        return result;
    }

    private static List<Map<String, Object>> logicChainContainerRequirements(List<ContainerItemConditionData> conditions, String captureDraftId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int index = 0;
        for (ContainerItemConditionData raw : conditions) {
            ContainerItemConditionData condition = raw == null ? null : raw.normalized();
            if (condition == null) {
                continue;
            }
            ItemStackMatcherData matcher = condition.matcher().normalized();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("requirementId", condition.id().isBlank() ? "logic-chain-container-" + (index + 1) : condition.id());
            row.put("displayName", condition.name().isBlank() ? "container 捕获 " + (index + 1) : condition.name());
            row.put("slot", condition.slot());
            row.put("count", condition.count());
            row.put("captureDraftId", safe(captureDraftId));
            row.put("templateSummary", ContainerItemConditionSupport.summary(condition));
            row.put("enabled", condition.enabled());
            row.put("type", condition.type());
            row.put("itemId", condition.itemId());
            row.put("countMode", condition.countMode());
            row.put("channel", condition.channel());
            row.put("offChannel", condition.offChannel());
            row.put("mode", condition.mode());
            row.put("matcherTemplateItemId", matcher.templateItemId());
            row.put("matcherCountMode", matcher.countMode());
            row.put("matcherRequiredCount", matcher.requiredCount());
            row.put("matcherTemplateCount", matcher.templateCount());
            row.put("matcherMatchItemId", matcher.matchItemId());
            row.put("matcherMatchDamage", matcher.matchDamage());
            row.put("matcherMatchCustomName", matcher.matchCustomName());
            row.put("matcherMatchLore", matcher.matchLore());
            row.put("matcherMatchCustomData", matcher.matchCustomData());
            row.put("matcherMatchComponents", matcher.matchComponents());
            row.put("matcherTemplateDamage", matcher.templateDamage());
            row.put("matcherTemplateCustomName", matcher.templateCustomName());
            row.put("matcherTemplateLore", matcher.templateLore());
            row.put("matcherTemplateCustomData", matcher.templateCustomData());
            row.put("matcherTemplateComponents", matcher.templateComponents());
            row.put("matcherSummary", matcher.templateSummary().isBlank() ? ItemStackMatcherSupport.summary(matcher) : matcher.templateSummary());
            rows.add(row);
            index++;
        }
        return List.copyOf(rows);
    }

    private static WebAdminWriteResult failSession(WebAdminContainerTemplateSession session, String code, String message, boolean notifyPlayer) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        if (notifyPlayer && player != null) {
            notifyPlayer(player, message, Formatting.YELLOW);
            sendEnd(player, "failed", session, message, Map.of("code", code));
        }
        releaseLock(session, "容器模板会话失败，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "failed");
        after.put("code", code);
        after.put("message", message);
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_FAILED, session, message, after);
        rememberTerminal(session, "failed", after);
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.VALIDATION_FAILED.id(), message, "CONTAINER_TEMPLATE_SESSION", session.sessionId, false, List.of(new WebAdminValidationError("containerTemplateSession", code, message, "")), "", event == null ? "" : event.id(), false, Map.of(), Map.of("containerTemplateSession", after));
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION), result, Map.of("status", "active"), after);
        return result;
    }

    private static WebAdminWriteResult failedResult(WebAdminContainerTemplateSession session, String code, String message) {
        Map<String, Object> data = baseStatus(session, "failed");
        data.put("code", code);
        data.put("message", message);
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_FAILED, session, message, data);
        rememberTerminal(session, "failed", data);
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.VALIDATION_FAILED.id(), message, "CONTAINER_TEMPLATE_SESSION", session.sessionId, false, List.of(new WebAdminValidationError("containerTemplateSession", code, message, "")), "", event == null ? "" : event.id(), false, Map.of(), Map.of("containerTemplateSession", data));
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION), result, Map.of(), data);
        return result;
    }

    private static WebAdminContainerTemplateSession activeForPlayer(UUID playerUuid) {
        String id = playerUuid == null ? "" : ACTIVE_BY_PLAYER.get(playerUuid);
        return id == null || id.isBlank() ? null : SESSIONS_BY_ID.get(id);
    }

    private static WebAdminContainerTemplateSession activeForDevice(String deviceId) {
        String id = ACTIVE_BY_DEVICE.get(safe(deviceId));
        return id == null || id.isBlank() ? null : SESSIONS_BY_ID.get(id);
    }

    private static WebAdminContainerTemplateSession validateClientSession(ServerPlayerEntity player, JsonObject body) {
        String sessionId = getString(body, "sessionId");
        String nonce = getString(body, "nonce");
        WebAdminContainerTemplateSession session = SESSIONS_BY_ID.get(sessionId);
        if (session == null || player == null || !session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            return null;
        }
        return session;
    }

    private static void removeActive(WebAdminContainerTemplateSession session) {
        if (session == null) {
            return;
        }
        session.terminal = true;
        SESSIONS_BY_ID.remove(session.sessionId);
        ACTIVE_BY_PLAYER.remove(session.targetPlayerUuid);
        ACTIVE_BY_DEVICE.remove(session.deviceId);
    }

    private static void releaseLock(WebAdminContainerTemplateSession session, String message) {
        if (lockService == null || session == null || safe(session.lockId).isBlank()) {
            return;
        }
        lockService.releaseForSessionCleanup(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                session.deviceId,
                session.lockId,
                session.actorUser,
                session.actorSession,
                session.context == null ? "" : session.context.remoteAddress(),
                message
        );
    }

    private static void sendOpen(ServerPlayerEntity player, WebAdminContainerTemplateSession session) {
        JsonObject body = new JsonObject();
        body.addProperty("sessionId", session.sessionId);
        body.addProperty("nonce", session.nonce);
        body.addProperty("deviceId", session.deviceId);
        body.addProperty("displayName", session.deviceDisplayName);
        body.addProperty("dimension", session.dimension);
        body.addProperty("x", session.x);
        body.addProperty("y", session.y);
        body.addProperty("z", session.z);
        body.addProperty("blockId", session.blockId);
        body.addProperty("expectedFingerprint", session.expectedFingerprint);
        body.addProperty("expiresAtMillis", session.expiresAtMillis);
        body.add("itemConditions", WebAdminJsonResponse.GSON.toJsonTree(session.itemConditions));
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminContainerTemplateS2CPayload("open", body.toString()));
    }

    private static void sendEnd(ServerPlayerEntity player, String action, WebAdminContainerTemplateSession session, String message, Map<String, Object> data) {
        if (player == null || session == null) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("sessionId", session.sessionId);
        body.addProperty("nonce", session.nonce);
        body.addProperty("deviceId", session.deviceId);
        body.addProperty("message", safe(message));
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number number) {
                    body.addProperty(entry.getKey(), number);
                } else if (value instanceof Boolean bool) {
                    body.addProperty(entry.getKey(), bool);
                } else {
                    body.addProperty(entry.getKey(), value == null ? "" : String.valueOf(value));
                }
            }
        }
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminContainerTemplateS2CPayload(action, body.toString()));
    }

    private static void notifyPlayer(ServerPlayerEntity player, String message, Formatting formatting) {
        if (player != null && !safe(message).isBlank()) {
            player.sendMessage(Text.literal(message).formatted(formatting), false);
        }
    }

    private static SignalDeviceData currentDevice(MinecraftServer server, WebAdminContainerTemplateSession session) {
        if (server == null || session == null || session.deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, session.deviceId);
        return resolved.foundUnique() && resolved.device() != null ? resolved.device().normalized() : null;
    }

    private static ServerPlayerEntity findOnlinePlayer(WebAdminContainerTemplateSession session) {
        if (session == null || session.targetPlayerUuid == null || currentServer == null) {
            return null;
        }
        return currentServer.getPlayerManager().getPlayer(session.targetPlayerUuid);
    }

    private static Map<String, Object> baseStatus(WebAdminContainerTemplateSession session, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", !session.terminal);
        data.put("sessionId", session.sessionId);
        data.put("sessionRef", session.sessionId);
        data.put("id", session.sessionId);
        data.put("status", status);
        data.put("deviceId", session.deviceId);
        data.put("displayName", session.deviceDisplayName);
        data.put("targetPlayerName", session.targetPlayerName);
        data.put("targetPlayerUuid", session.targetPlayerUuid == null ? "" : session.targetPlayerUuid.toString());
        data.put("lockTarget", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE + ":" + session.deviceId);
        data.put("lockId", session.lockId);
        data.put("expectedFingerprint", session.expectedFingerprint);
        data.put("createdAtMillis", session.createdAtMillis);
        data.put("expiresAtMillis", session.expiresAtMillis);
        data.put("opened", session.opened);
        data.put("itemConditionCount", session.itemConditions.size());
        data.put("p3bSavesItemConditions", true);
        data.put("logicChainDraftOnly", session.logicChainDraftOnly);
        data.put("logicChainCaptureDraftId", session.logicChainCaptureDraftId);
        data.put("logicChainTriggerKey", session.logicChainTriggerKey);
        return data;
    }

    private static List<ContainerItemConditionData> parseItemConditions(JsonObject body) {
        JsonArray array = body != null && body.has("itemConditions") && body.get("itemConditions").isJsonArray()
                ? body.getAsJsonArray("itemConditions")
                : new JsonArray();
        List<ContainerItemConditionData> result = new ArrayList<>();
        int index = 0;
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String type = ContainerItemConditionType.normalize(getString(object, "type"));
            String itemId = getString(object, "itemId").trim().toLowerCase();
            String matcherItemId = getString(object, "matcherTemplateItemId").trim().toLowerCase();
            if (itemId.isBlank()) {
                itemId = getString(object, "templateItemId").trim().toLowerCase();
            }
            String countMode = ContainerItemCountMode.normalize(getString(object, "countMode").isBlank()
                    ? getString(object, "templateCountMode")
                    : getString(object, "countMode"));
            int count = Math.max(0, getInt(object, "count", getInt(object, "templateCount", 1)));
            if (type.startsWith("slot_") && !ContainerItemConditionType.SLOT_EMPTY.id().equals(type)) {
                count = clampSlotItemCount(itemId, count);
            }
            ItemStackMatcherData matcher = parseMatcher(object, type, itemId, countMode, count);
            if (ContainerItemConditionType.SLOT_MATCHER.id().equals(type)
                    || ContainerItemConditionType.TOTAL_MATCHER.id().equals(type)) {
                itemId = matcher.templateItemId();
                countMode = matcher.countMode();
                count = matcher.requiredCount();
            }
            if (!ContainerItemConditionType.SLOT_EMPTY.id().equals(type) && itemId.isBlank() && matcherItemId.isBlank()) {
                continue;
            }
            String fallbackId = type + "-" + index;
            String id = getString(object, "id").isBlank() ? fallbackId : getString(object, "id");
            String name = getString(object, "name").isBlank() ? defaultConditionName(type, getInt(object, "slot", 0), itemId, index) : getString(object, "name");
            result.add(new ContainerItemConditionData(
                    id,
                    name,
                    !object.has("enabled") || getBoolean(object, "enabled", true),
                    type,
                    Math.max(0, getInt(object, "slot", 0)),
                    itemId,
                    countMode,
                    count,
                    getString(object, "channel"),
                    getString(object, "offChannel"),
                    getString(object, "mode"),
                    false,
                    0L,
                    0L,
                    0L,
                    "",
                    matcher
            ).normalized());
            index++;
        }
        return List.copyOf(result);
    }

    private static ItemStackMatcherData parseMatcher(JsonObject object, String type, String itemId, String countMode, int count) {
        boolean matcherType = ContainerItemConditionType.SLOT_MATCHER.id().equals(type)
                || ContainerItemConditionType.TOTAL_MATCHER.id().equals(type);
        if (!matcherType) {
            return ItemStackMatcherData.empty();
        }
        String templateItemId = getString(object, "matcherTemplateItemId").isBlank()
                ? itemId
                : getString(object, "matcherTemplateItemId").trim().toLowerCase();
        String matcherCountMode = ContainerItemCountMode.normalize(getString(object, "matcherCountMode").isBlank()
                ? countMode
                : getString(object, "matcherCountMode"));
        int requiredCount = getInt(object, "matcherRequiredCount", count);
        int templateCount = getInt(object, "matcherTemplateCount", Math.max(1, count));
        if (ContainerItemConditionType.SLOT_MATCHER.id().equals(type)) {
            requiredCount = ContainerItemCountMode.IGNORE.id().equals(matcherCountMode) ? 0 : clampSlotItemCount(templateItemId, requiredCount);
            templateCount = clampSlotItemCount(templateItemId, templateCount);
        }
        return new ItemStackMatcherData(
                !templateItemId.isBlank(),
                templateItemId,
                Math.max(1, templateCount),
                matcherCountMode,
                Math.max(0, requiredCount),
                getBoolean(object, "matcherMatchItemId", true),
                getBoolean(object, "matcherMatchDamage", false),
                getBoolean(object, "matcherMatchCustomName", false),
                getBoolean(object, "matcherMatchLore", false),
                getBoolean(object, "matcherMatchCustomData", false),
                getBoolean(object, "matcherMatchComponents", false),
                Math.max(0, getInt(object, "matcherTemplateDamage", 0)),
                getString(object, "matcherTemplateCustomName"),
                getStringList(object, "matcherTemplateLore"),
                getString(object, "matcherTemplateCustomData"),
                getString(object, "matcherTemplateComponents"),
                getString(object, "matcherSummary"),
                0L,
                System.currentTimeMillis()
        ).normalized();
    }

    private static Map<String, Object> templateSummary(SignalDeviceData device, String fingerprint) {
        if (device == null) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("displayName", displayName(device));
        data.put("itemConditionCount", device.itemConditions().size());
        data.put("expectedFingerprint", safe(fingerprint));
        data.put("containerChangeChannel", device.containerChangeChannel());
        return data;
    }

    private static void publishConfigChangedAfterSave(
            WebAdminContainerTemplateSession session,
            SignalDeviceData after,
            WebAdminAuditEvent auditEvent
    ) {
        if (session == null || after == null) {
            return;
        }
        String routeTarget = "#/devices/" + encode(after.id());
        Map<String, Object> payload = Map.of(
                "targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                "deviceType", after.type(),
                "itemConditionCount", after.itemConditions().size(),
                "expectedFingerprint", WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor(after),
                "actor", session.context == null ? "" : session.context.actorUsername()
        );
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(after.id())
                .channel(after.containerChangeChannel().isBlank() ? after.channel() : after.containerChangeChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 容器变化模板已保存。")
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE)
                .payload("deviceType", after.type())
                .payload("itemConditionCount", after.itemConditions().size())
                .payload("expectedFingerprint", WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor(after))
                .payload("actor", session.context == null ? "" : session.context.actorUsername()));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(after.id())
                .channel(after.containerChangeChannel().isBlank() ? after.channel() : after.containerChangeChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 容器变化模板已保存：" + displayName(after))
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE)
                .payload("deviceType", after.type())
                .payload("itemConditionCount", after.itemConditions().size()));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(after.id())
                .channel(after.containerChangeChannel().isBlank() ? after.channel() : after.containerChangeChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE)
                .payload("deviceType", after.type())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id())
                .payload("payload", payload.toString()));
    }


    private static void rememberTerminal(WebAdminContainerTemplateSession session, String status, Map<String, Object> data) {
        Map<String, Object> entry = baseStatus(session, status);
        if (data != null) {
            entry.putAll(data);
        }
        entry.put("active", false);
        TERMINAL_STATUS.put(session.sessionId, Map.copyOf(entry));
        TERMINAL_ORDER.addLast(session.sessionId);
        while (TERMINAL_ORDER.size() > MAX_TERMINAL_STATUS) {
            TERMINAL_STATUS.remove(TERMINAL_ORDER.removeFirst());
        }
    }

    private static WebAdminRealtimeEvent publishEvent(
            WebAdminRealtimeEventType type,
            WebAdminContainerTemplateSession session,
            String summary,
            Map<String, Object> payload
    ) {
        WebAdminRealtimeEvent.Builder builder = WebAdminRealtimeEvent.builder(type)
                .deviceId(session.deviceId)
                .sourceType("virtual_block_device")
                .severity(type == WebAdminRealtimeEventType.CONTAINER_TEMPLATE_SESSION_FAILED ? "WARNING" : "INFO")
                .summary(summary)
                .routeTarget("#/devices/" + encode(session.deviceId))
                .payload("sessionId", session.sessionId)
                .payload("targetPlayerName", session.targetPlayerName)
                .payload("actor", session.context == null ? "" : session.context.actorUsername())
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE)
                .payload("status", type.id());
        if (payload != null) {
            payload.forEach(builder::payload);
        }
        return WebAdminRealtimeEventBus.publish(builder);
    }

    private static WebAdminWriteResult result(
            WebAdminContainerTemplateSession session,
            boolean success,
            boolean changed,
            String message,
            Map<String, Object> data,
            WebAdminRealtimeEvent event
    ) {
        return new WebAdminWriteResult(
                success,
                success ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.VALIDATION_FAILED.id(),
                message,
                "CONTAINER_TEMPLATE_SESSION",
                session.sessionId,
                changed,
                List.of(),
                "",
                event == null ? "" : event.id(),
                false,
                Map.of(),
                Map.of("containerTemplateSession", data)
        );
    }

    private static WebAdminWriteResult conflict(WebAdminWriteContext context, WebAdminContainerTemplateSession previous, String message) {
        Map<String, Object> data = baseStatus(previous, previous.opened ? "opened" : "started");
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.CONFLICT_DETECTED.id(), message, "CONTAINER_TEMPLATE_SESSION", previous.sessionId, false, List.of(), "", "", false, data, Map.of("containerTemplateSession", data));
        audit(context, result, Map.of(), data);
        return result;
    }

    private static WebAdminWriteContext contextFor(WebAdminContainerTemplateSession session, WebAdminOperationType operationType) {
        return new WebAdminWriteContext(
                session.context == null ? "" : session.context.actorUsername(),
                session.context == null ? WebAdminRole.VIEWER : session.context.actorRole(),
                session.context == null ? "" : session.context.sessionHashSummary(),
                session.context == null ? "" : session.context.remoteAddress(),
                operationType,
                target(session.deviceId)
        );
    }

    private static WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
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

    private static WebAdminWriteTarget target(String deviceId) {
        return new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE", safe(deviceId), "VBD 容器变化模板会话");
    }

    private static JsonObject parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to parse WebAdmin container template payload: {}", exception.getMessage());
            return new JsonObject();
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> getStringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element != null && !element.isJsonNull()) {
                try {
                    result.add(element.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        return List.copyOf(result);
    }

    private static String defaultConditionName(String type, int slot, String itemId, int index) {
        String safeType = safe(type);
        if (safeType.startsWith("slot_")) {
            return "Slot " + Math.max(0, slot) + (safe(itemId).isBlank() ? "" : " " + safe(itemId));
        }
        if (safeType.startsWith("total_")) {
            return "Total " + (safe(itemId).isBlank() ? ("condition " + index) : safe(itemId));
        }
        return "Condition " + index;
    }

    private static int clampSlotItemCount(String itemId, int count) {
        int max = maxStackCount(itemId);
        return Math.max(1, Math.min(max, count));
    }

    private static int maxStackCount(String itemId) {
        Identifier id = Identifier.tryParse(safe(itemId));
        if (id == null) {
            return 64;
        }
        Item item = Registries.ITEM.get(id);
        if (item == null) {
            return 64;
        }
        return Math.max(1, new ItemStack(item).getMaxCount());
    }

    private static String displayName(SignalDeviceData device) {
        if (device == null) {
            return "";
        }
        return safe(device.name()).isBlank() ? device.id() : device.name();
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String iso(long epochMillis) {
        return epochMillis <= 0L ? "" : Instant.ofEpochMilli(epochMillis).toString();
    }
}
