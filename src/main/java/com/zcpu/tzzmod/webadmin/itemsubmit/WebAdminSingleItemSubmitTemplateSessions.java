package com.zcpu.tzzmod.webadmin.itemsubmit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.WebAdminSingleItemSubmitTemplateS2CPayload;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.util.NullSafety;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class WebAdminSingleItemSubmitTemplateSessions {
    public static final long SESSION_TTL_MILLIS = 10L * 60L * 1000L;
    private static final Map<String, WebAdminSingleItemSubmitTemplateSession> SESSIONS_BY_ID = new LinkedHashMap<>();
    private static final Map<UUID, String> ACTIVE_BY_PLAYER = new LinkedHashMap<>();
    private static final Map<String, String> ACTIVE_BY_DEVICE = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> TERMINAL_STATUS = new LinkedHashMap<>();
    private static final Deque<String> TERMINAL_ORDER = new ArrayDeque<>();
    private static final int MAX_TERMINAL_STATUS = 128;
    private static MinecraftServer currentServer;
    private static WebAdminEditLockService lockService;

    private WebAdminSingleItemSubmitTemplateSessions() {
    }

    public static synchronized WebAdminWriteResult startSession(
            MinecraftServer server,
            ServerPlayerEntity targetPlayer,
            WebAdminEditLockService editLockService,
            WebAdminSingleItemSubmitTemplateSession session
    ) {
        expireOld();
        if (server == null || targetPlayer == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target(session == null ? "" : session.deviceId), "目标玩家不在线。");
        }
        currentServer = server;
        lockService = editLockService;
        WebAdminSingleItemSubmitTemplateSession playerSession = activeForPlayer(targetPlayer.getUuid());
        if (playerSession != null) {
            return conflict(session.context, playerSession, "该玩家已有进行中的单物品提交模板会话，请先取消后再开始。");
        }
        WebAdminSingleItemSubmitTemplateSession deviceSession = activeForDevice(session.deviceId);
        if (deviceSession != null) {
            return conflict(session.context, deviceSession, "该 VBD 已有进行中的单物品提交模板会话，请先取消后再开始。");
        }
        SESSIONS_BY_ID.put(session.sessionId, session);
        ACTIVE_BY_PLAYER.put(session.targetPlayerUuid, session.sessionId);
        ACTIVE_BY_DEVICE.put(session.deviceId, session.sessionId);
        try {
            sendOpen(targetPlayer, session);
        } catch (Exception exception) {
            removeActive(session);
            releaseLock(session, "单物品提交模板 GUI 打开失败，编辑锁已释放。");
            return failedResult(session, "gui_send_failed", "无法向目标玩家打开单物品提交模板 GUI：" + exception.getMessage());
        }
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_STARTED, session, "单物品提交模板会话已开始。", Map.of("status", "started"));
        Map<String, Object> data = baseStatus(session, "started");
        data.put("realtimeEventId", event == null ? "" : event.id());
        WebAdminWriteResult result = result(session, true, true, "已通知目标玩家打开单物品提交模板 GUI。", data, event);
        audit(session.context, result, Map.of(), data);
        return result;
    }

    public static synchronized Map<String, Object> status(String sessionId) {
        expireOld();
        WebAdminSingleItemSubmitTemplateSession session = SESSIONS_BY_ID.get(safe(sessionId));
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
        return cancelFromWebAdmin(sessionId, context, reason, "");
    }

    public static synchronized WebAdminWriteResult cancelFromWebAdmin(String sessionId, WebAdminWriteContext context, String reason, String expectedDeviceId) {
        expireOld();
        WebAdminSingleItemSubmitTemplateSession session = SESSIONS_BY_ID.get(safe(sessionId));
        if (session == null) {
            Map<String, Object> terminal = TERMINAL_STATUS.get(safe(sessionId));
            if (terminal != null) {
                Map<String, Object> idempotent = new LinkedHashMap<>(terminal);
                idempotent.put("alreadyTerminal", true);
                idempotent.put("idempotentNoOp", true);
                return new WebAdminWriteResult(true, WebAdminWriteResultCode.OK.id(), "单物品提交模板会话已结束。", "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", safe(sessionId), false, List.of(), "", "", false, Map.of(), Map.of("singleItemSubmitTemplateSession", idempotent));
            }
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, new WebAdminWriteTarget("SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", safe(sessionId), "单物品提交模板会话"), "单物品提交模板会话不存在或已经结束。");
        }
        if (context != null
                && context.actorRole() != WebAdminRole.OWNER
                && !session.context.actorUsername().equalsIgnoreCase(context.actorUsername())) {
            WebAdminWriteResult denied = WebAdminWriteResult.failed(WebAdminWriteResultCode.PERMISSION_DENIED, target(session.deviceId), "只能由发起者或 OWNER 取消该单物品提交模板会话。");
            audit(context, denied, baseStatus(session, "active"), Map.of("attempt", "cancel_non_owner"));
            return denied;
        }
        if (!safe(expectedDeviceId).isBlank() && !safe(session.deviceId).equals(safe(expectedDeviceId))) {
            WebAdminWriteResult mismatch = WebAdminWriteResult.failed(WebAdminWriteResultCode.VALIDATION_FAILED, target(safe(expectedDeviceId)), "单物品提交模板会话与当前设备不匹配。");
            audit(context, mismatch, baseStatus(session, "active"), Map.of("attempt", "cancel_device_mismatch", "sessionDeviceId", session.deviceId));
            return mismatch;
        }
        return cancelSession(session, "webui", safe(reason).isBlank() ? "WebAdmin 已取消单物品提交模板会话。" : reason, true, true);
    }

    public static synchronized void openedFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        WebAdminSingleItemSubmitTemplateSession session = validateClientSession(player, parse(bodyJson));
        if (session == null) {
            return;
        }
        session.opened = true;
        Map<String, Object> data = baseStatus(session, "opened");
        publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_OPENED, session, "目标玩家已打开单物品提交模板 GUI。", data);
    }

    public static synchronized void cancelFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        JsonObject body = parse(bodyJson);
        WebAdminSingleItemSubmitTemplateSession session = validateClientSession(player, body);
        if (session == null) {
            return;
        }
        cancelSession(session, getString(body, "reason").isBlank() ? "client_close" : getString(body, "reason"), "游戏内 GUI 已关闭，单物品提交模板会话已取消。", true, true);
    }

    public static synchronized void saveFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        if (server != null) {
            currentServer = server;
        }
        JsonObject body = parse(bodyJson);
        WebAdminSingleItemSubmitTemplateSession session = validateClientSession(player, body);
        if (session == null) {
            return;
        }
        saveSession(server, player, session, body);
    }

    public static synchronized void cancelForDisconnect(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        WebAdminSingleItemSubmitTemplateSession session = activeForPlayer(player.getUuid());
        if (session != null) {
            failSession(session, "player_disconnect", "目标玩家已断开连接，单物品提交模板会话已取消。", false);
        }
    }

    public static synchronized void clearAll(MinecraftServer server, String reason) {
        List<WebAdminSingleItemSubmitTemplateSession> sessions = List.copyOf(SESSIONS_BY_ID.values());
        for (WebAdminSingleItemSubmitTemplateSession session : sessions) {
            failSession(session, safe(reason).isBlank() ? "server_cleanup" : safe(reason), "服务器正在停止，单物品提交模板会话已取消。", true);
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
        for (WebAdminSingleItemSubmitTemplateSession session : SESSIONS_BY_ID.values().stream().filter(session -> session.expiresAtMillis <= now).toList()) {
            expireSession(session);
        }
    }

    private static void expireSession(WebAdminSingleItemSubmitTemplateSession session) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        notifyPlayer(player, "单物品提交模板会话已过期。", Formatting.YELLOW);
        sendEnd(player, "expired", session, "单物品提交模板会话已过期。", Map.of("source", "timeout"));
        releaseLock(session, "单物品提交模板会话过期，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "expired");
        after.put("source", "timeout");
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_EXPIRED, session, "单物品提交模板会话已过期。", after);
        rememberTerminal(session, "expired", after);
        WebAdminWriteResult result = result(session, true, true, "单物品提交模板会话已过期。", after, event);
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION), result, Map.of("status", "active"), after);
    }

    private static WebAdminWriteResult cancelSession(WebAdminSingleItemSubmitTemplateSession session, String source, String message, boolean notifyPlayer, boolean publish) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        if (notifyPlayer && player != null) {
            String terminalMessage = safe(message).isBlank() ? "单物品提交模板会话已取消。" : message;
            notifyPlayer(player, terminalMessage, Formatting.YELLOW);
            sendEnd(player, "cancelled", session, terminalMessage, Map.of("source", source));
        }
        releaseLock(session, "单物品提交模板会话取消，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "cancelled");
        after.put("source", source);
        after.put("message", message);
        WebAdminRealtimeEvent event = publish ? publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_CANCELLED, session, safe(message).isBlank() ? "单物品提交模板会话已取消。" : message, after) : null;
        rememberTerminal(session, "cancelled", after);
        WebAdminWriteResult result = result(session, true, true, "单物品提交模板会话已取消。", after, event);
        audit(contextFor(session, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION), result, Map.of("status", "active"), after);
        return result;
    }

    private static WebAdminWriteResult saveSession(MinecraftServer server, ServerPlayerEntity player, WebAdminSingleItemSubmitTemplateSession session, JsonObject body) {
        MinecraftServer activeServer = server == null ? currentServer : server;
        SignalDeviceData before = currentDevice(activeServer, session);
        if (before == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(before.type())) {
            return failSession(session, "device_missing", "目标 virtual_block_device 不存在，单物品提交模板保存失败。", true);
        }
        if (!before.interactionEnabled()) {
            return failSession(session, "interaction_disabled", "右键交互未启用，单物品 itemSubmit 当前不能保存。", true);
        }
        if (before.itemSubmitRequirements().size() > 1) {
            return failSession(session, "multi_requirement_readonly", "当前为多物品提交配置，7.10 单物品编辑器不会覆盖。", true);
        }
        if (lockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = lockService.validateLock(
                    WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT,
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
        String currentFingerprint = WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.fingerprintFor(before);
        if (!session.expectedFingerprint.equals(currentFingerprint)
                || (!requestFingerprint.isBlank() && !session.expectedFingerprint.equals(requestFingerprint))) {
            return failSession(session, "fingerprint_conflict", "单物品 itemSubmit 配置已被其他操作修改，保存已取消。", true);
        }
        ItemSubmitSaveDraft draft;
        try {
            draft = parseTemplate(body);
        } catch (IllegalArgumentException exception) {
            return failSession(session, "invalid_item_submit_template", exception.getMessage(), true);
        }
        List<ItemSubmitRequirementData> nextRequirements = draft.hasTemplate()
                ? List.of(requirementFromDraft(before, draft))
                : List.of();
        boolean nextItemSubmitEnabled = draft.itemSubmitEnabled() && draft.hasTemplate();
        SignalDeviceData after = SignalDeviceStore.updateVirtualItemSubmitForWebAdmin(
                activeServer,
                session.deviceId,
                nextItemSubmitEnabled,
                draft.consumeEnabled(),
                draft.consumeOrder(),
                nextRequirements,
                nextItemSubmitEnabled,
                draft.vanillaPolicy()
        );
        if (after == null) {
            return failSession(session, "save_failed", "单物品提交模板保存失败，设备状态未更新。", true);
        }
        removeActive(session);
        String message = draft.hasTemplate() ? "单物品提交模板已保存。" : "单物品提交模板已清空。";
        if (player != null) {
            notifyPlayer(player, message, Formatting.GREEN);
            sendEnd(player, "saved", session, message, Map.of("source", "client_save"));
        }
        releaseLock(session, "单物品提交模板保存完成，编辑锁已释放。");
        Map<String, Object> beforeSummary = templateSummary(before, session.expectedFingerprint);
        Map<String, Object> afterSummary = templateSummary(after, WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.fingerprintFor(after));
        WebAdminRealtimeEvent sessionEvent = publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_SAVED, session, message, afterSummary);
        Map<String, Object> status = baseStatus(session, "saved");
        status.put("message", message);
        status.put("expectedFingerprint", WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.fingerprintFor(after));
        rememberTerminal(session, "saved", status);
        WebAdminWriteResult result = result(session, true, true, message, status, sessionEvent);
        WebAdminAuditEvent auditEvent = audit(contextFor(session, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT), result, beforeSummary, afterSummary);
        publishConfigChangedAfterSave(session, after, auditEvent);
        return result;
    }

    private record ItemSubmitSaveDraft(
            boolean hasTemplate,
            boolean itemSubmitEnabled,
            boolean requirementEnabled,
            String itemId,
            int templateCount,
            String countMode,
            int requiredCount,
            boolean matchDamage,
            boolean matchCustomName,
            boolean matchLore,
            boolean matchCustomData,
            boolean matchComponents,
            int templateDamage,
            String templateCustomName,
            List<String> templateLore,
            String templateCustomData,
            String templateComponents,
            String templateDisplayStack,
            boolean consumeEnabled,
            String consumeOrder,
            int consumeCount,
            String vanillaPolicy,
            String summary
    ) {
    }

    private static ItemSubmitSaveDraft parseTemplate(JsonObject body) {
        JsonObject template = body != null && body.has("template") && body.get("template").isJsonObject()
                ? body.getAsJsonObject("template")
                : new JsonObject();
        String itemId = getString(template, "templateItemId").trim().toLowerCase();
        if (itemId.isBlank()) {
            itemId = getString(template, "itemId").trim().toLowerCase();
        }
        if (itemId.isBlank()) {
            return new ItemSubmitSaveDraft(false, false, false, "", 0, ContainerItemCountMode.AT_LEAST.id(), 0, false, false, false, false, false, 0, "", List.of(), "", "", "", getBoolean(template, "itemSubmitConsumeEnabled", false), InventoryConsumeOrder.normalize(getString(template, "itemSubmitConsumeOrder")), 1, InteractionItemVanillaPolicy.normalize(getString(template, "interactionItemVanillaPolicy")), "");
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || Registries.ITEM.get(id) == null) {
            throw new IllegalArgumentException("提交模板物品 ID 无效：" + itemId);
        }
        String mode = ContainerItemCountMode.normalize(getString(template, "countMode").isBlank() ? getString(template, "templateCountMode") : getString(template, "countMode"));
        int rawCount = getInt(template, "count", getInt(template, "requiredCount", getInt(template, "templateCount", 1)));
        int required = ContainerItemCountMode.IGNORE.id().equals(mode) ? 0 : clampOperationalCount(rawCount);
        int templateCount = clampStackCount(itemId, getInt(template, "templateCount", rawCount <= 0 ? 1 : rawCount));
        int consumeCount = clampOperationalCount(getInt(template, "consumeCount", required <= 0 ? 1 : required));
        return new ItemSubmitSaveDraft(
                true,
                getBoolean(template, "itemSubmitEnabled", true),
                getBoolean(template, "requirementEnabled", true),
                itemId,
                templateCount,
                mode,
                required,
                getBoolean(template, "matchDamage", false),
                getBoolean(template, "matchCustomName", false),
                getBoolean(template, "matchLore", false),
                getBoolean(template, "matchCustomData", false),
                getBoolean(template, "matchComponents", false),
                Math.max(0, getInt(template, "templateDamage", 0)),
                safe(getString(template, "templateCustomName")),
                getStringList(template, "templateLore"),
                safe(getString(template, "templateCustomData")),
                safe(getString(template, "templateComponents")),
                safe(getString(template, "templateDisplayStack")),
                getBoolean(template, "itemSubmitConsumeEnabled", false),
                InventoryConsumeOrder.normalize(getString(template, "itemSubmitConsumeOrder")),
                consumeCount,
                InteractionItemVanillaPolicy.normalize(getString(template, "interactionItemVanillaPolicy")),
                safe(getString(template, "summary"))
        );
    }

    private static ItemSubmitRequirementData requirementFromDraft(SignalDeviceData before, ItemSubmitSaveDraft draft) {
        ItemSubmitRequirementData previous = before.itemSubmitRequirements().isEmpty() ? null : before.itemSubmitRequirements().get(0).normalized();
        long now = System.currentTimeMillis();
        String name = previous == null || previous.name().isBlank() ? "single_item_submit" : previous.name();
        String id = previous == null || previous.id().isBlank() ? UUID.randomUUID().toString() : previous.id();
        ItemStackMatcherData previousMatcher = previous == null ? ItemStackMatcherData.empty().normalized() : previous.matcher().normalized();
        ItemStackMatcherData matcher = new ItemStackMatcherData(
                true,
                draft.itemId(),
                draft.templateCount(),
                draft.countMode(),
                draft.requiredCount(),
                true,
                draft.matchDamage(),
                draft.matchCustomName(),
                draft.matchLore(),
                draft.matchCustomData(),
                draft.matchComponents(),
                draft.templateDamage(),
                draft.templateCustomName(),
                draft.templateLore(),
                draft.templateCustomData(),
                draft.templateComponents(),
                draft.templateDisplayStack(),
                draft.summary().isBlank() ? draft.itemId() + " x" + (draft.requiredCount() <= 0 ? draft.templateCount() : draft.requiredCount()) : draft.summary(),
                previousMatcher.successChannel(),
                previousMatcher.failChannel(),
                previousMatcher.successMessage(),
                previousMatcher.failMessage(),
                previousMatcher.successSoundId(),
                previousMatcher.successSoundVolume(),
                previousMatcher.successSoundPitch(),
                previousMatcher.failSoundId(),
                previousMatcher.failSoundVolume(),
                previousMatcher.failSoundPitch(),
                previousMatcher.consumeEnabled(),
                previousMatcher.consumeCount(),
                previousMatcher.interactionItemConsumeSource(),
                previousMatcher.interactionItemInventoryConsumeOrder(),
                previousMatcher.interactionItemSource(),
                draft.vanillaPolicy(),
                previousMatcher.lastInteractionItemSource(),
                previousMatcher.lastInteractionItemMatchedSlot(),
                previousMatcher.lastInteractionItemMatchedCount(),
                previousMatcher.lastInteractionItemSourceResult(),
                previousMatcher.lastInteractionItemConsumeSource(),
                previousMatcher.lastInteractionItemConsumedSlots(),
                previousMatcher.lastInteractionItemConsumeResult(),
                previous == null ? now : previousMatcher.createdWallTimeMillis(),
                now
        ).normalized();
        return new ItemSubmitRequirementData(
                id,
                name,
                draft.requirementEnabled(),
                matcher,
                draft.consumeCount(),
                previous != null && previous.lastMatched(),
                previous == null ? 0 : previous.lastMatchedCount(),
                previous == null ? 0L : previous.lastCheckGameTime(),
                previous == null ? "" : previous.lastResult()
        ).normalized();
    }

    private static boolean hasUnsupportedAdvancedItemSubmitMatcher(SignalDeviceData device) {
        if (device == null || device.itemSubmitRequirements().size() != 1) {
            return false;
        }
        ItemStackMatcherData matcher = device.itemSubmitRequirements().get(0).normalized().matcher().normalized();
        return matcher.matchDamage()
                || matcher.matchCustomName()
                || matcher.matchLore()
                || matcher.matchCustomData()
                || matcher.matchComponents();
    }

    private static WebAdminWriteResult failSession(WebAdminSingleItemSubmitTemplateSession session, String code, String message, boolean notifyPlayer) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        if (notifyPlayer && player != null) {
            notifyPlayer(player, message, Formatting.YELLOW);
            sendEnd(player, "failed", session, message, Map.of("code", code));
        }
        releaseLock(session, "单物品提交模板会话失败，编辑锁已释放。");
        Map<String, Object> after = baseStatus(session, "failed");
        after.put("code", code);
        after.put("message", message);
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_FAILED, session, message, after);
        rememberTerminal(session, "failed", after);
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.VALIDATION_FAILED.id(), message, "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", session.sessionId, false, List.of(new WebAdminValidationError("singleItemSubmitTemplateSession", code, message, "")), "", event == null ? "" : event.id(), false, Map.of(), Map.of("singleItemSubmitTemplateSession", after));
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION), result, Map.of("status", "active"), after);
        return result;
    }

    private static WebAdminWriteResult failedResult(WebAdminSingleItemSubmitTemplateSession session, String code, String message) {
        Map<String, Object> data = baseStatus(session, "failed");
        data.put("code", code);
        data.put("message", message);
        WebAdminRealtimeEvent event = publishEvent(WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_FAILED, session, message, data);
        rememberTerminal(session, "failed", data);
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.VALIDATION_FAILED.id(), message, "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", session.sessionId, false, List.of(new WebAdminValidationError("singleItemSubmitTemplateSession", code, message, "")), "", event == null ? "" : event.id(), false, Map.of(), Map.of("singleItemSubmitTemplateSession", data));
        audit(contextFor(session, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION), result, Map.of(), data);
        return result;
    }

    private static WebAdminSingleItemSubmitTemplateSession activeForPlayer(UUID playerUuid) {
        String id = playerUuid == null ? "" : ACTIVE_BY_PLAYER.get(playerUuid);
        return id == null || id.isBlank() ? null : SESSIONS_BY_ID.get(id);
    }

    private static WebAdminSingleItemSubmitTemplateSession activeForDevice(String deviceId) {
        String id = ACTIVE_BY_DEVICE.get(safe(deviceId));
        return id == null || id.isBlank() ? null : SESSIONS_BY_ID.get(id);
    }

    private static WebAdminSingleItemSubmitTemplateSession validateClientSession(ServerPlayerEntity player, JsonObject body) {
        String sessionId = getString(body, "sessionId");
        String nonce = getString(body, "nonce");
        WebAdminSingleItemSubmitTemplateSession session = SESSIONS_BY_ID.get(sessionId);
        if (session == null || player == null || !session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            return null;
        }
        return session;
    }

    private static void removeActive(WebAdminSingleItemSubmitTemplateSession session) {
        if (session == null) {
            return;
        }
        session.terminal = true;
        SESSIONS_BY_ID.remove(session.sessionId);
        ACTIVE_BY_PLAYER.remove(session.targetPlayerUuid);
        ACTIVE_BY_DEVICE.remove(session.deviceId);
    }

    private static void releaseLock(WebAdminSingleItemSubmitTemplateSession session, String message) {
        if (lockService == null || session == null || safe(session.lockId).isBlank()) {
            return;
        }
        lockService.releaseForSessionCleanup(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT,
                session.deviceId,
                session.lockId,
                session.actorUser,
                session.actorSession,
                session.context == null ? "" : session.context.remoteAddress(),
                message
        );
    }

    private static void sendOpen(ServerPlayerEntity player, WebAdminSingleItemSubmitTemplateSession session) {
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
        body.add("template", WebAdminJsonResponse.GSON.toJsonTree(session.template));
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminSingleItemSubmitTemplateS2CPayload("open", body.toString()));
    }

    private static void sendEnd(ServerPlayerEntity player, String action, WebAdminSingleItemSubmitTemplateSession session, String message, Map<String, Object> data) {
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
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminSingleItemSubmitTemplateS2CPayload(action, body.toString()));
    }

    private static void notifyPlayer(ServerPlayerEntity player, String message, Formatting formatting) {
        if (player != null && !safe(message).isBlank()) {
            player.sendMessage(Text.literal(message).formatted(formatting), false);
        }
    }

    private static SignalDeviceData currentDevice(MinecraftServer server, WebAdminSingleItemSubmitTemplateSession session) {
        if (server == null || session == null || session.deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, session.deviceId);
        return resolved.foundUnique() && resolved.device() != null ? resolved.device().normalized() : null;
    }

    private static ServerPlayerEntity findOnlinePlayer(WebAdminSingleItemSubmitTemplateSession session) {
        if (session == null || session.targetPlayerUuid == null || currentServer == null) {
            return null;
        }
        return currentServer.getPlayerManager().getPlayer(session.targetPlayerUuid);
    }

    private static Map<String, Object> baseStatus(WebAdminSingleItemSubmitTemplateSession session, String status) {
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
        data.put("lockTarget", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT + ":" + session.deviceId);
        data.put("lockId", session.lockId);
        data.put("expectedFingerprint", session.expectedFingerprint);
        data.put("createdAtMillis", session.createdAtMillis);
        data.put("expiresAtMillis", session.expiresAtMillis);
        data.put("opened", session.opened);
        data.put("singleItemSubmitTemplate", true);
        return data;
    }

    private static Map<String, Object> templateSummary(SignalDeviceData device, String fingerprint) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("displayName", SignalDeviceStore.displayName(device));
        data.put("expectedFingerprint", safe(fingerprint));
        data.put("itemSubmitEnabled", device.itemSubmitEnabled());
        data.put("requirementCount", device.itemSubmitRequirements().size());
        data.put("singleItemSubmit", WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.singleRequirementDto(device));
        return data;
    }

    private static void publishConfigChangedAfterSave(WebAdminSingleItemSubmitTemplateSession session, SignalDeviceData after, WebAdminAuditEvent auditEvent) {
        String routeTarget = "#/devices/" + encode(after.id());
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(after.id())
                .channel(after.interactChannel().isBlank() ? after.channel() : after.interactChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 单物品 itemSubmit 模板已保存。")
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT)
                .payload("deviceType", after.type())
                .payload("singleItemSubmit", true)
                .payload("requirementCount", after.itemSubmitRequirements().size())
                .payload("expectedFingerprint", WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.fingerprintFor(after))
                .payload("actor", session.context == null ? "" : session.context.actorUsername()));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(after.id())
                .channel(after.interactChannel().isBlank() ? after.channel() : after.interactChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 单物品 itemSubmit 模板已保存：" + SignalDeviceStore.displayName(after))
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT)
                .payload("deviceType", after.type())
                .payload("requirementCount", after.itemSubmitRequirements().size()));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(after.id())
                .channel(after.interactChannel().isBlank() ? after.channel() : after.interactChannel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT)
                .payload("deviceType", after.type())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id()));
    }

    private static void rememberTerminal(WebAdminSingleItemSubmitTemplateSession session, String status, Map<String, Object> data) {
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

    private static WebAdminRealtimeEvent publishEvent(WebAdminRealtimeEventType type, WebAdminSingleItemSubmitTemplateSession session, String summary, Map<String, Object> payload) {
        WebAdminRealtimeEvent.Builder builder = WebAdminRealtimeEvent.builder(type)
                .deviceId(session.deviceId)
                .sourceType("virtual_block_device")
                .severity(type == WebAdminRealtimeEventType.SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_FAILED ? "WARNING" : "INFO")
                .summary(summary)
                .routeTarget("#/devices/" + encode(session.deviceId))
                .payload("sessionId", session.sessionId)
                .payload("targetPlayerName", session.targetPlayerName)
                .payload("actor", session.context == null ? "" : session.context.actorUsername())
                .payload("targetType", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT)
                .payload("status", type.id());
        if (payload != null) {
            payload.forEach(builder::payload);
        }
        return WebAdminRealtimeEventBus.publish(builder);
    }

    private static WebAdminWriteResult result(WebAdminSingleItemSubmitTemplateSession session, boolean success, boolean changed, String message, Map<String, Object> data, WebAdminRealtimeEvent event) {
        return new WebAdminWriteResult(success, success ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.VALIDATION_FAILED.id(), message, "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", session.sessionId, changed, List.of(), "", event == null ? "" : event.id(), false, Map.of(), Map.of("singleItemSubmitTemplateSession", data));
    }

    private static WebAdminWriteResult conflict(WebAdminWriteContext context, WebAdminSingleItemSubmitTemplateSession previous, String message) {
        Map<String, Object> data = baseStatus(previous, previous.opened ? "opened" : "started");
        WebAdminWriteResult result = new WebAdminWriteResult(false, WebAdminWriteResultCode.CONFLICT_DETECTED.id(), message, "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION", previous.sessionId, false, List.of(), "", "", false, data, Map.of("singleItemSubmitTemplateSession", data));
        audit(context, result, Map.of(), data);
        return result;
    }

    private static WebAdminWriteContext contextFor(WebAdminSingleItemSubmitTemplateSession session, WebAdminOperationType operationType) {
        return new WebAdminWriteContext(session.context == null ? "" : session.context.actorUsername(), session.context == null ? WebAdminRole.VIEWER : session.context.actorRole(), session.context == null ? "" : session.context.sessionHashSummary(), session.context == null ? "" : session.context.remoteAddress(), operationType, target(session.deviceId));
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
        return new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT", safe(deviceId), "VBD 单物品提交模板会话");
    }

    private static JsonObject parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to parse WebAdmin single itemSubmit template payload: {}", exception.getMessage());
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
        List<String> values = new ArrayList<>();
        object.getAsJsonArray(key).forEach(element -> {
            try {
                values.add(element.getAsString());
            } catch (Exception ignored) {
            }
        });
        return List.copyOf(values);
    }

    private static int clampStackCount(String itemId, int count) {
        Identifier id = Identifier.tryParse(safe(itemId));
        if (id == null) {
            return Math.max(1, Math.min(64, count));
        }
        Item item = Registries.ITEM.get(id);
        if (item == null) {
            return Math.max(1, Math.min(64, count));
        }
        return Math.max(1, Math.min(Math.max(1, new ItemStack(item).getMaxCount()), count));
    }

    private static int clampOperationalCount(int count) {
        return Math.max(1, Math.min(64_000, count));
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
