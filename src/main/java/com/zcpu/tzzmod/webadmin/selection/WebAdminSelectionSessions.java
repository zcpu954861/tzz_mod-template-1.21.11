package com.zcpu.tzzmod.webadmin.selection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.WebAdminSelectionS2CPayload;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.util.NullSafety;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
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
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class WebAdminSelectionSessions {
    private static final Map<String, WebAdminSelectionSession> SESSIONS_BY_ID = new LinkedHashMap<>();
    private static final Map<UUID, String> ACTIVE_BY_PLAYER = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> TERMINAL_STATUS = new LinkedHashMap<>();
    private static final Deque<String> TERMINAL_ORDER = new ArrayDeque<>();
    private static final int MAX_TERMINAL_STATUS = 128;
    private static final double MAX_SELECTION_DISTANCE_SQUARED = 64.0D;
    private static MinecraftServer currentServer;

    private WebAdminSelectionSessions() {
    }

    public static synchronized boolean hasActive(UUID playerUuid) {
        return playerUuid != null && ACTIVE_BY_PLAYER.containsKey(playerUuid);
    }

    public static synchronized WebAdminSelectionSession activeFor(UUID playerUuid) {
        String id = playerUuid == null ? "" : ACTIVE_BY_PLAYER.get(playerUuid);
        return id == null || id.isBlank() ? null : SESSIONS_BY_ID.get(id);
    }

    public static synchronized Map<String, Object> status(String selectionId) {
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(safe(selectionId));
        if (session != null) {
            Map<String, Object> status = baseStatus(session, "active");
            status.put("active", true);
            return status;
        }
        Map<String, Object> terminal = TERMINAL_STATUS.get(safe(selectionId));
        if (terminal != null) {
            return Map.copyOf(terminal);
        }
        return Map.of(
                "active", false,
                "status", "not_found",
                "selectionId", safe(selectionId)
        );
    }

    public static synchronized WebAdminWriteResult startSession(
            MinecraftServer server,
            ServerPlayerEntity targetPlayer,
            WebAdminWriteContext context,
            WebAdminSelectionDraft draft
    ) {
        if (targetPlayer == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, selectionTarget(""), "目标玩家不在线。");
        }
        currentServer = server;
        WebAdminSelectionSession previous = activeFor(targetPlayer.getUuid());
        if (previous != null) {
            return activeConflict(context, previous);
        }
        String selectionId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        WebAdminWriteContext safeContext = context == null
                ? new WebAdminWriteContext("", null, "", "", WebAdminOperationType.START_OBJECT_SELECTION, selectionTarget(selectionId))
                : context;
        WebAdminSelectionSession session = new WebAdminSelectionSession(
                selectionId,
                nonce,
                safeContext.actorUsername(),
                safeContext.actorRole(),
                safeContext.sessionHashSummary(),
                safeContext.remoteAddress(),
                targetPlayer.getUuid(),
                targetPlayer.getName().getString(),
                WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE,
                draft
        );
        SESSIONS_BY_ID.put(selectionId, session);
        ACTIVE_BY_PLAYER.put(session.targetPlayerUuid, selectionId);
        sendStart(targetPlayer, session);
        WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_STARTED, session, "等待玩家在游戏内选择方块。", Map.of());
        Map<String, Object> data = baseStatus(session, "started");
        data.put("realtimeEventId", event == null ? "" : event.id());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "已通知目标玩家进入选择模式。",
                "OBJECT_SELECTION",
                selectionId,
                true,
                List.of(),
                "",
                event == null ? "" : event.id(),
                false,
                Map.of(),
                Map.of("selection", data)
        );
        audit(contextFor(session, selectionTarget(selectionId)), result, Map.of(), data);
        return result;
    }

    public static synchronized WebAdminWriteResult cancelFromWebAdmin(String selectionId, WebAdminWriteContext requestContext, String reason) {
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(safe(selectionId));
        if (session == null) {
            Map<String, Object> terminal = TERMINAL_STATUS.get(safe(selectionId));
            if (terminal != null) {
                return new WebAdminWriteResult(
                        true,
                        WebAdminWriteResultCode.OK.id(),
                        "选择 session 已结束。",
                        "OBJECT_SELECTION",
                        safe(selectionId),
                        false,
                        List.of(),
                        "",
                        "",
                        false,
                        Map.of(),
                        Map.of("selection", terminal)
                );
            }
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, selectionTarget(selectionId), "选择 session 不存在或已经结束。");
            audit(requestContext, result, Map.of("selectionId", safe(selectionId)), Map.of("attempt", "cancel_missing"));
            return result;
        }
        if (requestContext != null
                && requestContext.actorRole() != com.zcpu.tzzmod.webadmin.WebAdminRole.OWNER
                && !session.actorUsername.equalsIgnoreCase(requestContext.actorUsername())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.PERMISSION_DENIED, selectionTarget(selectionId), "只能由发起者或 OWNER 取消该选择。");
            audit(requestContext, result, Map.of("status", "active"), Map.of("attempt", "cancel_non_owner", "selectionId", session.selectionId));
            return result;
        }
        return cancelSession(session, "webui", isBlank(reason) ? "WebAdmin 已取消选择。" : reason, true, true);
    }

    public static synchronized void cancelFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        JsonObject body = parse(bodyJson);
        String selectionId = getString(body, "selectionId");
        String nonce = getString(body, "nonce");
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(selectionId);
        if (session == null || player == null || !session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            return;
        }
        cancelSession(session, "client_esc", "已取消选择。", true, true);
    }

    public static synchronized void cancelForDisconnect(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        WebAdminSelectionSession session = activeFor(player.getUuid());
        if (session != null) {
            cancelSession(session, "disconnect", "玩家已断开连接，选择已取消。", false, true);
        }
    }

    public static synchronized void clearAll(MinecraftServer server, String reason) {
        List<WebAdminSelectionSession> sessions = List.copyOf(SESSIONS_BY_ID.values());
        for (WebAdminSelectionSession session : sessions) {
            cancelSession(session, safe(reason).isBlank() ? "server_cleanup" : safe(reason), "服务器正在停止，选择已取消。", true, false);
        }
        SESSIONS_BY_ID.clear();
        ACTIVE_BY_PLAYER.clear();
        currentServer = null;
    }

    public static synchronized void completeFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        JsonObject body = parse(bodyJson);
        String selectionId = getString(body, "selectionId");
        String nonce = getString(body, "nonce");
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(selectionId);
        if (session == null || player == null || session.completing) {
            return;
        }
        if (!session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            failAndClose(session, player, "server_validation", "选择 session 校验失败，请重新发起选择。", Map.of("reason", "session_mismatch"));
            return;
        }
        session.completing = true;
        if (session.purpose != WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE) {
            failAndClose(session, player, "server_validation", "选择用途不受支持。", Map.of("reason", "unsupported_purpose"));
            return;
        }

        ServerWorld world = player.getCommandSource().getWorld();
        String dimensionId = getString(body, "dimensionId");
        BlockPos pos = new BlockPos(getInt(body, "x"), getInt(body, "y"), getInt(body, "z"));
        String side = getString(body, "side");
        if (!world.getRegistryKey().getValue().toString().equals(dimensionId)) {
            failAndClose(session, player, "server_validation", "选择维度与玩家当前维度不一致，请重新选择。", Map.of("dimensionId", dimensionId));
            return;
        }
        if (!world.isInBuildLimit(pos)) {
            failAndClose(session, player, "server_validation", "目标方块不在世界高度范围内。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        if (player.squaredDistanceTo(pos.toCenterPos()) > MAX_SELECTION_DISTANCE_SQUARED) {
            failAndClose(session, player, "server_validation", "目标方块超出可交互距离，请靠近后重新选择。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        if (!playerRaycastMatches(player, pos)) {
            failAndClose(session, player, "server_validation", "目标方块不是玩家当前准星指向的方块，请重新选择。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        if (!world.isChunkLoaded(pos)) {
            failAndClose(session, player, "server_validation", "目标区块未加载，无法创建虚拟方块设备。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            failAndClose(session, player, "server_validation", "不能选择空气方块。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        if (VirtualBlockDeviceSupport.isDedicatedSignalDevice(state)) {
            failAndClose(session, player, "server_validation", "该位置是 TZZ 专用信号设备，请选择普通方块。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        SignalDeviceData existing = SignalDeviceStore.findVirtualBlockDevice(server, world, pos);
        if (existing != null) {
            failAndClose(session, player, "conflict", "该方块已存在虚拟设备。", Map.of(
                    "existingDeviceId", existing.id(),
                    "routeTarget", "#/devices/" + encode(existing.id()),
                    "pos", posSummary(world, pos)
            ));
            return;
        }
        String channel = SignalChannel.normalize(session.draft.channel());
        if (!SignalChannel.isValid(channel)) {
            failAndClose(session, player, "server_validation", SignalChannel.validationError(channel).getString(), Map.of("channel", channel));
            return;
        }

        SignalDeviceData created = SignalDeviceStore.upsertVirtualBlock(world, pos, channel);
        if (!session.draft.enabled()) {
            SignalDeviceData updated = SignalDeviceStore.updateBasicConfig(server, created.id(), false, channel);
            if (updated != null) {
                created = updated;
            }
        }
        try {
            applyMetadata(server, session, created);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to apply WebAdmin selection metadata for {}: {}", created.id(), exception.getMessage());
        }
        removeActive(session);
        String worldId = world.getRegistryKey().getValue().toString();
        String successMessage = "选择成功：已选择方块 " + worldId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        player.sendMessage(Text.literal(successMessage).formatted(Formatting.GREEN), false);
        sendEnd(player, "complete_ack", session, "选择成功。", Map.of("deviceId", created.id()));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "completed");
        after.put("deviceId", created.id());
        after.put("world", worldId);
        after.put("x", pos.getX());
        after.put("y", pos.getY());
        after.put("z", pos.getZ());
        after.put("blockId", VirtualBlockDeviceSupport.blockId(state));
        after.put("side", side);
        after.put("channel", created.channel());
        after.put("targetPlayer", session.targetPlayerName);
        after.put("selectionId", session.selectionId);
        WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_COMPLETED, session, "虚拟方块设备已创建。", after);
        rememberTerminal(session, "completed", after);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "已创建虚拟方块设备。",
                "DEVICE",
                created.id(),
                true,
                List.of(),
                "",
                event == null ? "" : event.id(),
                false,
                Map.of(),
                Map.of("selection", after, "deviceId", created.id())
        );
        audit(contextFor(session, new WebAdminWriteTarget("DEVICE", created.id(), "虚拟方块设备")), result, Map.of(), after);
    }

    private static boolean playerRaycastMatches(ServerPlayerEntity player, BlockPos pos) {
        HitResult hitResult = player.raycast(Math.sqrt(MAX_SELECTION_DISTANCE_SQUARED) + 0.25D, 0.0F, false);
        return hitResult instanceof BlockHitResult blockHitResult
                && hitResult.getType() == HitResult.Type.BLOCK
                && blockHitResult.getBlockPos().equals(pos);
    }

    private static WebAdminWriteResult cancelSession(
            WebAdminSelectionSession session,
            String source,
            String message,
            boolean notifyPlayer,
            boolean publish
    ) {
        removeActive(session);
        ServerPlayerEntity player = findOnlinePlayer(session);
        if (notifyPlayer && player != null) {
            player.sendMessage(Text.literal(isBlank(message) ? "已取消选择。" : message).formatted(Formatting.YELLOW), false);
            sendEnd(player, "cancel", session, isBlank(message) ? "已取消选择。" : message, Map.of("source", source));
        }
        Map<String, Object> after = baseStatus(session, "cancelled");
        after.put("source", source);
        after.put("message", message);
        WebAdminRealtimeEvent event = publish ? publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_CANCELLED, session, isBlank(message) ? "选择已取消。" : message, after) : null;
        rememberTerminal(session, "cancelled", after);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "选择已取消。",
                "OBJECT_SELECTION",
                session.selectionId,
                true,
                List.of(),
                "",
                event == null ? "" : event.id(),
                false,
                Map.of(),
                Map.of("selection", after)
        );
        audit(contextFor(session, selectionTarget(session.selectionId)), result, Map.of("status", "active"), after);
        return result;
    }

    private static void failAndClose(
            WebAdminSelectionSession session,
            ServerPlayerEntity player,
            String code,
            String message,
            Map<String, Object> extra
    ) {
        removeActive(session);
        Map<String, Object> after = baseStatus(session, "failed");
        after.put("code", code);
        after.put("message", message);
        after.putAll(extra == null ? Map.of() : extra);
        if (player != null) {
            player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), false);
            sendEnd(player, "failed", session, message, after);
        }
        WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_FAILED, session, message, after);
        rememberTerminal(session, "failed", after);
        WebAdminWriteResult result = new WebAdminWriteResult(
                false,
                "conflict".equals(code) ? WebAdminWriteResultCode.CONFLICT_DETECTED.id() : WebAdminWriteResultCode.VALIDATION_FAILED.id(),
                message,
                "OBJECT_SELECTION",
                session.selectionId,
                false,
                "conflict".equals(code) ? List.of() : List.of(new WebAdminValidationError("selection", code, message, "")),
                "",
                event == null ? "" : event.id(),
                false,
                "conflict".equals(code) ? extra : Map.of(),
                Map.of("selection", after)
        );
        audit(contextFor(session, selectionTarget(session.selectionId)), result, Map.of("status", "active"), after);
    }

    private static void removeActive(WebAdminSelectionSession session) {
        if (session == null) {
            return;
        }
        SESSIONS_BY_ID.remove(session.selectionId);
        ACTIVE_BY_PLAYER.remove(session.targetPlayerUuid);
    }

    private static void sendStart(ServerPlayerEntity player, WebAdminSelectionSession session) {
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", session.selectionId);
        body.addProperty("nonce", session.nonce);
        body.addProperty("purpose", session.purpose.id());
        body.addProperty("title", "选择虚拟方块设备目标");
        body.addProperty("confirmHint", "右键方块确认");
        body.addProperty("cancelHint", "ESC 取消");
        body.addProperty("channel", session.draft.channel());
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminSelectionS2CPayload("begin", body.toString()));
    }

    private static void sendEnd(ServerPlayerEntity player, String action, WebAdminSelectionSession session, String message, Map<String, Object> data) {
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", session.selectionId);
        body.addProperty("message", message == null ? "" : message);
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
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new WebAdminSelectionS2CPayload(action, body.toString()));
    }

    private static ServerPlayerEntity findOnlinePlayer(WebAdminSelectionSession session) {
        if (session == null || session.targetPlayerUuid == null) {
            return null;
        }
        return currentServer == null ? null : currentServer.getPlayerManager().getPlayer(session.targetPlayerUuid);
    }

    private static void applyMetadata(MinecraftServer server, WebAdminSelectionSession session, SignalDeviceData device) {
        if (server == null || device == null || session == null) {
            return;
        }
        WebAdminSelectionDraft draft = session.draft;
        String displayName = draft.displayName();
        String note = draft.note();
        String iconKey = draft.iconKey();
        if (displayName.isBlank() && note.isBlank() && ("auto".equals(iconKey) || iconKey.isBlank())) {
            return;
        }
        if (!WebAdminDeviceMetadataService.isAllowedIconKey(iconKey)) {
            iconKey = "auto";
        }
        WebAdminDeviceMetadataStore.MetadataFile file = WebAdminDeviceMetadataStore.load(server);
        WebAdminDeviceMetadataStore.MetadataEntry before = WebAdminDeviceMetadataStore.MetadataEntry.normalized(device.id(), file.devices.get(device.id()));
        WebAdminDeviceMetadataStore.MetadataEntry after = new WebAdminDeviceMetadataStore.MetadataEntry();
        after.deviceId = device.id();
        after.displayName = displayName;
        after.note = note;
        after.iconKey = iconKey;
        after.updatedAt = Instant.now().toString();
        after.updatedBy = session.actorUsername;
        after.version = before.version + 1L;
        file.devices.put(device.id(), after);
        WebAdminDeviceMetadataStore.save(server, file);
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED)
                .deviceId(device.id())
                .channel(device.channel())
                .sourceType(device.type())
                .summary("设备显示信息已更新：" + device.id())
                .routeTarget("#/devices/" + encode(device.id()))
                .payload("displayName", displayName)
                .payload("iconKey", iconKey)
                .payload("actor", session.actorUsername));
    }

    private static WebAdminRealtimeEvent publishSelectionEvent(
            WebAdminRealtimeEventType type,
            WebAdminSelectionSession session,
            String summary,
            Map<String, Object> payload
    ) {
        WebAdminRealtimeEvent.Builder builder = WebAdminRealtimeEvent.builder(type)
                .sourceType("webadmin_selection")
                .severity(type == WebAdminRealtimeEventType.SELECTION_FAILED ? "WARNING" : "INFO")
                .summary(summary)
                .routeTarget(payload != null && payload.containsKey("deviceId")
                        ? "#/devices/" + encode(String.valueOf(payload.get("deviceId")))
                        : "#/virtual-block-devices")
                .payload("selectionId", session.selectionId)
                .payload("purpose", session.purpose.id())
                .payload("targetPlayerName", session.targetPlayerName)
                .payload("actor", session.actorUsername)
                .payload("status", type.id());
        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                builder.payload(entry.getKey(), entry.getValue());
            }
        }
        if (payload != null && payload.containsKey("deviceId")) {
            builder.deviceId(String.valueOf(payload.get("deviceId")));
        }
        if (payload != null && payload.containsKey("channel")) {
            builder.channel(String.valueOf(payload.get("channel")));
        }
        return WebAdminRealtimeEventBus.publish(builder);
    }

    private static void rememberTerminal(WebAdminSelectionSession session, String status, Map<String, Object> data) {
        Map<String, Object> entry = baseStatus(session, status);
        if (data != null) {
            entry.putAll(data);
        }
        TERMINAL_STATUS.put(session.selectionId, Map.copyOf(entry));
        TERMINAL_ORDER.addLast(session.selectionId);
        while (TERMINAL_ORDER.size() > MAX_TERMINAL_STATUS) {
            String id = TERMINAL_ORDER.removeFirst();
            TERMINAL_STATUS.remove(id);
        }
    }

    private static Map<String, Object> baseStatus(WebAdminSelectionSession session, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("selectionId", session.selectionId);
        data.put("status", status);
        data.put("purpose", session.purpose.id());
        data.put("targetPlayerName", session.targetPlayerName);
        data.put("channel", session.draft.channel());
        data.put("displayName", session.draft.displayName());
        data.put("enabled", session.draft.enabled());
        data.put("createdAtMillis", session.createdAtMillis);
        return data;
    }

    private static Map<String, Object> posSummary(ServerWorld world, BlockPos pos) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("world", world == null ? "" : world.getRegistryKey().getValue().toString());
        summary.put("x", pos == null ? 0 : pos.getX());
        summary.put("y", pos == null ? 0 : pos.getY());
        summary.put("z", pos == null ? 0 : pos.getZ());
        return summary;
    }

    private static WebAdminWriteContext contextFor(WebAdminSelectionSession session, WebAdminWriteTarget target) {
        return new WebAdminWriteContext(
                session.actorUsername,
                session.actorRole,
                session.sessionHashSummary,
                session.remoteAddress,
                WebAdminOperationType.START_OBJECT_SELECTION,
                target
        );
    }

    private static WebAdminAuditEvent audit(
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

    private static WebAdminWriteTarget selectionTarget(String selectionId) {
        return new WebAdminWriteTarget("OBJECT_SELECTION", safe(selectionId), "新建虚拟方块设备选择");
    }

    private static WebAdminWriteResult activeConflict(WebAdminWriteContext context, WebAdminSelectionSession previous) {
        Map<String, Object> conflict = baseStatus(previous, "active");
        WebAdminWriteResult result = new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "该玩家已有进行中的 WebAdmin 选择，请先取消后再开始新的选择。",
                "OBJECT_SELECTION",
                previous.selectionId,
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

    private static JsonObject parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception ignored) {
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

    private static int getInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
