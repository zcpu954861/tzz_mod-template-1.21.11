package com.zcpu.tzzmod.webadmin.selection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.network.WebAdminSelectionS2CPayload;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.util.NullSafety;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.draft.WebAdminProtectedDraftRegistry;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotService;
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
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class WebAdminSelectionSessions {
    private static final Map<String, WebAdminSelectionSession> SESSIONS_BY_ID = new LinkedHashMap<>();
    private static final Map<UUID, String> ACTIVE_BY_PLAYER = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> TERMINAL_STATUS = new LinkedHashMap<>();
    private static final Map<String, HotbarSnapshot> WORLD_DEVICE_HOTBAR_SNAPSHOTS = new LinkedHashMap<>();
    private static final Map<String, List<RegionGeometry.Point>> REGION_SELECTION_POINTS = new LinkedHashMap<>();
    private static final Map<String, String> REGION_SELECTION_DIMENSIONS = new LinkedHashMap<>();
    private static final Deque<String> TERMINAL_ORDER = new ArrayDeque<>();
    private static final int MAX_TERMINAL_STATUS = 128;
    private static final double MAX_SELECTION_DISTANCE_SQUARED = 64.0D;
    private static final long SELECTION_TTL_MILLIS = 15L * 60L * 1000L;
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

    public static synchronized void expireOld(MinecraftServer server) {
        currentServer = server == null ? currentServer : server;
        long now = System.currentTimeMillis();
        for (WebAdminSelectionSession session : List.copyOf(SESSIONS_BY_ID.values())) {
            if (now - session.createdAtMillis <= SELECTION_TTL_MILLIS) {
                continue;
            }
            ServerPlayerEntity player = findOnlinePlayer(session);
            failAndClose(
                    session,
                    player,
                    "selection_expired",
                    "WebAdmin 选择会话已超时，草稿选择已取消。",
                    Map.of("expired", true, "ttlMillis", SELECTION_TTL_MILLIS)
            );
        }
        cleanupExpiredWorldDeviceProtectedDrafts(server, now);
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
            WebAdminSelectionPurpose purpose,
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
                purpose == null ? WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE : purpose,
                draft
        );
        SESSIONS_BY_ID.put(selectionId, session);
        ACTIVE_BY_PLAYER.put(session.targetPlayerUuid, selectionId);
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            applyWorldDeviceHotbarMode(targetPlayer, session);
        }
        sendStart(targetPlayer, session);
        WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_STARTED, session, selectionStartSummary(session), Map.of());
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

    public static synchronized WebAdminWriteResult cancelProtectedDraftFromWebAdmin(
            MinecraftServer server,
            WebAdminWriteContext context,
            String draftSessionId,
            String reason
    ) {
        String draftId = safe(draftSessionId);
        WebAdminWriteTarget target = new WebAdminWriteTarget("PROTECTED_DRAFT", draftId, "Logic Chain 受保护草稿");
        if (draftId.isBlank()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "draftSessionId",
                    "required",
                    "取消 protected draft 需要 draftSessionId。",
                    ""
            )));
            audit(context, result, Map.of(), Map.of("attempt", "missing_protected_draft_id"));
            return result;
        }
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.get(draftId);
        if (entry == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "protected draft 不存在或已过期。");
            audit(context, result, Map.of("draftSessionId", draftId), Map.of("attempt", "missing_protected_draft"));
            return result;
        }
        if (entry.isTerminal()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.VALIDATION_FAILED, target, "protected draft 已结束，不能再次取消。");
            audit(context, result, entry.toMap(), Map.of("attempt", "terminal_protected_draft", "state", entry.state()));
            return result;
        }
        if (context != null
                && context.actorRole() != WebAdminRole.OWNER
                && !entry.actor().isBlank()
                && !entry.actor().equalsIgnoreCase(context.actorUsername())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.PERMISSION_DENIED, target, "只能由发起者或 OWNER 取消该 protected draft。");
            audit(context, result, entry.toMap(), Map.of("attempt", "cancel_protected_draft_non_owner"));
            return result;
        }
        Map<String, Object> before = entry.toMap();
        Map<String, Object> cleanup = cleanupPlacedProtectedDraft(server, entry);
        if (Boolean.FALSE.equals(cleanup.get("success"))) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.VALIDATION_FAILED, target, String.valueOf(cleanup.get("message")));
            audit(context, result, before, cleanup);
            return result;
        }
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry cancelled = WebAdminProtectedDraftRegistry.cancel(draftId);
        Map<String, Object> after = new LinkedHashMap<>(cleanup);
        after.put("status", "cancelled");
        after.put("message", isBlank(reason) ? "protected draft 已取消。" : reason);
        after.put("protectedDraft", cancelled == null ? WebAdminProtectedDraftRegistry.summary(draftId) : cancelled.toMap());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "protected draft 已取消；未创建 graph card。",
                "PROTECTED_DRAFT",
                draftId,
                Boolean.TRUE.equals(cleanup.get("changed")),
                List.of(),
                "",
                "",
                false,
                Map.of(),
                Map.of("selection", after, "protectedDraft", after.get("protectedDraft"))
        );
        audit(context, result, before, after);
        return result;
    }

    public static synchronized int cancelByEditLock(String editLockId, String reason) {
        String lock = safe(editLockId);
        if (lock.isBlank()) {
            return 0;
        }
        int cancelled = 0;
        for (WebAdminSelectionSession session : List.copyOf(SESSIONS_BY_ID.values())) {
            if (session == null || session.draft == null || !lock.equals(session.draft.editLockId())) {
                continue;
            }
            cancelSession(session, "logic_chain_cancel", isBlank(reason) ? "Logic Chain 编辑已退出，选择已取消。" : reason, true, true);
            cancelled++;
        }
        for (WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry : WebAdminProtectedDraftRegistry.activeByEditLock(lock)) {
            if (cleanupAndCancelProtectedDraft(currentServer, entry, isBlank(reason) ? "Logic Chain 编辑已退出，protected draft 已取消。" : reason)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    public static synchronized void cancelFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        currentServer = server == null ? currentServer : server;
        JsonObject body = parse(bodyJson);
        String selectionId = getString(body, "selectionId");
        String nonce = getString(body, "nonce");
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(selectionId);
        if (session == null || player == null || !session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            return;
        }
        if (!getBoolean(body, "confirmed", false)) {
            player.sendMessage(Text.literal("再次按 ESC 确认取消；当前选择进度会丢弃，已放置的草稿设备会被清理。").formatted(Formatting.YELLOW), false);
            return;
        }
        cancelSession(session, "client_esc", "已取消选择。", true, true);
    }

    public static synchronized void updateWorldDeviceSelectedSlotFromClient(MinecraftServer server, ServerPlayerEntity player, String bodyJson) {
        currentServer = server == null ? currentServer : server;
        JsonObject body = parse(bodyJson);
        String selectionId = getString(body, "selectionId");
        String nonce = getString(body, "nonce");
        WebAdminSelectionSession session = SESSIONS_BY_ID.get(selectionId);
        if (session == null || player == null || !session.targetPlayerUuid.equals(player.getUuid()) || !session.nonce.equals(nonce)) {
            return;
        }
        if (session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            return;
        }
        int slot = normalizeWorldDeviceSelectedSlot(getInt(body, "slot"));
        session.worldDeviceSelectedSlot = slot;
        player.getInventory().setSelectedSlot(slot);
        syncInventory(player);
    }

    public static synchronized void cancelForDisconnect(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        WebAdminSelectionSession session = activeFor(player.getUuid());
        if (session != null) {
            cancelSession(session, "disconnect", "玩家已断开连接，选择已取消。", false, true, player);
        }
    }

    public static synchronized void restorePendingWorldDeviceHotbarMode(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        for (Map.Entry<String, HotbarSnapshot> entry : List.copyOf(WORLD_DEVICE_HOTBAR_SNAPSHOTS.entrySet())) {
            HotbarSnapshot snapshot = entry.getValue();
            if (snapshot != null && player.getUuid().equals(snapshot.playerUuid())) {
                restoreWorldDeviceHotbarSnapshot(entry.getKey(), snapshot, player);
            }
        }
    }

    public static synchronized void clearAll(MinecraftServer server, String reason) {
        currentServer = server == null ? currentServer : server;
        List<WebAdminSelectionSession> sessions = List.copyOf(SESSIONS_BY_ID.values());
        for (WebAdminSelectionSession session : sessions) {
            cancelSession(session, safe(reason).isBlank() ? "server_cleanup" : safe(reason), "服务器正在停止，选择已取消。", true, false);
        }
        cleanupAllActiveWorldDeviceProtectedDrafts(server, safe(reason).isBlank() ? "server_cleanup" : safe(reason));
        SESSIONS_BY_ID.clear();
        ACTIVE_BY_PLAYER.clear();
        WORLD_DEVICE_HOTBAR_SNAPSHOTS.clear();
        REGION_SELECTION_POINTS.clear();
        REGION_SELECTION_DIMENSIONS.clear();
        currentServer = null;
    }

    public static synchronized boolean shouldBlockBreak(ServerPlayerEntity player) {
        WebAdminSelectionSession session = player == null ? null : activeFor(player.getUuid());
        return session != null && (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE
                || session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT);
    }

    public static synchronized boolean shouldBlockProtectedDraftUse(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return shouldBlockProtectedDraftMutation(player, world, pos, "使用");
    }

    public static synchronized boolean shouldBlockProtectedDraftBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        return shouldBlockProtectedDraftMutation(player, world, pos, "破坏");
    }

    public static synchronized boolean shouldBlockProtectedDraftCommandMutation(ServerCommandSource source, BlockPos pos, String verb) {
        return shouldBlockProtectedDraftCommandMutation(source, source == null ? null : source.getWorld(), pos, verb);
    }

    public static synchronized boolean shouldBlockProtectedDraftCommandMutation(ServerCommandSource source, ServerWorld world, BlockPos pos, String verb) {
        if (source == null || pos == null) {
            return false;
        }
        if (world == null) {
            return false;
        }
        currentServer = source.getServer();
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = activeWorldDeviceProtectedDraftAt(world, pos);
        if (entry == null) {
            return false;
        }
        source.sendError(Text.literal("该方块是 Logic Chain protected draft，不能通过普通命令" + safe(verb) + "；请回 WebAdmin 保存或取消草稿。")
                .formatted(Formatting.YELLOW));
        return true;
    }

    public static synchronized boolean handleUseBlock(MinecraftServer server, ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        currentServer = server == null ? currentServer : server;
        WebAdminSelectionSession session = player == null ? null : activeFor(player.getUuid());
        if (session == null || hitResult == null || session.completing) {
            return false;
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            completeWorldDevicePlacement(server, player, session, hand, hitResult);
            return true;
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT) {
            handleRegionControllerCorner(server, player, session, hitResult);
            return true;
        }
        return false;
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
        if (session.purpose != WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE
                && session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_VBD_SELECT
                && session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE
                && session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT
                && session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_ITEM_SUBMIT_CAPTURE
                && session.purpose != WebAdminSelectionPurpose.LOGIC_CHAIN_CONTAINER_CAPTURE) {
            failAndClose(session, player, "server_validation", "选择用途不受支持。", Map.of("reason", "unsupported_purpose"));
            return;
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            failAndClose(session, player, "server_validation", "世界设备引用必须使用三格 hotbar 放置模式完成，不能进入 VBD 单方块选择。", Map.of("reason", "wrong_client_handler"));
            return;
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT) {
            failAndClose(session, player, "server_validation", "区域控制器必须使用 RegionPlanner-like 角点选择完成，不能进入 VBD 单方块选择。", Map.of("reason", "wrong_client_handler"));
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
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_VBD_SELECT
                || session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_ITEM_SUBMIT_CAPTURE
                || session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_CONTAINER_CAPTURE) {
            String worldId = world.getRegistryKey().getValue().toString();
            String objectType = protectedDraftObjectType(session.purpose);
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry draftEntry = WebAdminProtectedDraftRegistry.markSelectedBlock(
                    session.draft.draftSessionId(),
                    session.draft.editLockId(),
                    session.actorUsername,
                    player.getUuidAsString(),
                    objectType,
                    worldId,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    VirtualBlockDeviceSupport.blockId(state),
                    "",
                    Map.of(
                            "selectionId", session.selectionId,
                            "purpose", session.purpose.id(),
                            "side", side,
                            "logicChainRootType", session.draft.logicChainRootType(),
                            "logicChainRootRef", session.draft.logicChainRootRef(),
                            "logicChainDraftNodeId", session.draft.logicChainDraftNodeId()
                    )
            );
            if (draftEntry == null) {
                failAndClose(session, player, "protected_draft_inactive", "Logic Chain protected draft 已取消或已过期，请重新从 WebAdmin 发起选择。", Map.of(
                        "draftSessionId", session.draft.draftSessionId(),
                        "editLockId", session.draft.editLockId()
                ));
                return;
            }
            removeActive(session, player);
            String successMessage = "选择成功：已记录为 Logic Chain 受保护草稿 " + worldId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            player.sendMessage(Text.literal(successMessage).formatted(Formatting.GREEN), false);
            sendEnd(player, "complete_ack", session, "选择成功，已写入 Logic Chain protected draft。", Map.of("protectedDraftId", draftEntry.draftSessionId()));
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("status", "selected");
            after.put("protectedDraftId", draftEntry.draftSessionId());
            after.put("draftSessionId", draftEntry.draftSessionId());
            after.put("objectType", draftEntry.objectType());
            after.put("world", worldId);
            after.put("x", pos.getX());
            after.put("y", pos.getY());
            after.put("z", pos.getZ());
            after.put("blockId", VirtualBlockDeviceSupport.blockId(state));
            after.put("side", side);
            after.put("targetPlayer", session.targetPlayerName);
            after.put("selectionId", session.selectionId);
            after.put("draftOnly", true);
            WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_COMPLETED, session, "Logic Chain 受保护草稿选择完成。", after);
            rememberTerminal(session, "selected", after);
            WebAdminWriteResult result = new WebAdminWriteResult(
                    true,
                    WebAdminWriteResultCode.OK.id(),
                    "已记录 Logic Chain 受保护草稿选择，尚未写入正式配置。",
                    "PROTECTED_DRAFT",
                    draftEntry.draftSessionId(),
                    false,
                    List.of(),
                    "",
                    event == null ? "" : event.id(),
                    false,
                    Map.of(),
                    Map.of("selection", after, "protectedDraft", draftEntry.toMap())
            );
            audit(contextFor(session, new WebAdminWriteTarget("PROTECTED_DRAFT", draftEntry.draftSessionId(), "Logic Chain 受保护草稿")), result, Map.of(), after);
            return;
        }

        String channel = SignalChannel.normalize(session.draft.channel());
        if (!SignalChannel.isValid(channel)) {
            failAndClose(session, player, "server_validation", SignalChannel.validationError(channel).getString(), Map.of("channel", channel));
            return;
        }

        WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = WebAdminSnapshotService.createAutoBeforeTrustedWrite(
                server,
                snapshotActor(session),
                WebAdminOperationType.START_OBJECT_SELECTION,
                "Virtual Block Device",
                "virtual_block_device",
                world.getRegistryKey().getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ(),
                "创建虚拟方块设备前自动保存"
        );
        if (!autoSnapshot.created() && !autoSnapshot.skipped()) {
            failAndClose(session, player, "auto_snapshot_failed", "写入前自动保存点创建失败，已停止创建虚拟方块设备。请检查快照存储或损坏配置文件。", Map.of("pos", posSummary(world, pos)));
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

    private static void completeWorldDevicePlacement(
            MinecraftServer server,
            ServerPlayerEntity player,
            WebAdminSelectionSession session,
            Hand hand,
            BlockHitResult hitResult
    ) {
        if (server == null || player == null || session == null || hitResult == null) {
            return;
        }
        session.completing = true;
        ServerWorld world = player.getCommandSource().getWorld();
        BlockPos basePos = hitResult.getBlockPos();
        BlockPos placePos = basePos.offset(hitResult.getSide());
        if (player.squaredDistanceTo(basePos.toCenterPos()) > MAX_SELECTION_DISTANCE_SQUARED) {
            failAndClose(session, player, "server_validation", "目标位置超出可交互距离，请靠近后重新放置。", Map.of("pos", posSummary(world, basePos)));
            return;
        }
        if (!world.isInBuildLimit(placePos)) {
            failAndClose(session, player, "server_validation", "目标放置位置不在世界高度范围内。", Map.of("pos", posSummary(world, placePos)));
            return;
        }
        if (!world.isChunkLoaded(placePos)) {
            failAndClose(session, player, "server_validation", "目标区块未加载，无法放置世界设备。", Map.of("pos", posSummary(world, placePos)));
            return;
        }
        BlockState previousState = world.getBlockState(placePos);
        if (!previousState.isAir()) {
            failAndClose(session, player, "server_validation", "世界设备放置位置必须为空气方块。", Map.of("pos", posSummary(world, placePos)));
            return;
        }
        int selectedSlot = normalizeWorldDeviceSelectedSlot(session.worldDeviceSelectedSlot);
        session.worldDeviceSelectedSlot = selectedSlot;
        player.getInventory().setSelectedSlot(selectedSlot);
        syncInventory(player);
        SelectedWorldDevice selected = selectedWorldDevice(player, selectedSlot);
        if (selected == null) {
            failAndClose(session, player, "server_validation", "世界设备模式只能使用前三格的 SignalEmitter / SignalReceiver / ActionRelay。", Map.of("selectedSlot", selectedSlot));
            return;
        }
        BlockState placedState = selected.block().getDefaultState();
        if (!world.setBlockState(placePos, placedState, Block.NOTIFY_ALL)) {
            failAndClose(session, player, "server_validation", "世界设备放置失败，请换一个位置重试。", Map.of("pos", posSummary(world, placePos)));
            return;
        }
        SignalDeviceData device = upsertPlacedWorldDevice(world, placePos, selected.deviceType());
        if (device == null) {
            world.setBlockState(placePos, previousState, Block.NOTIFY_ALL);
            failAndClose(session, player, "server_validation", "世界设备方块实体未就绪，已撤销本次放置。", Map.of("pos", posSummary(world, placePos)));
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("selectionId", session.selectionId);
        metadata.put("purpose", session.purpose.id());
        metadata.put("side", hitResult.getSide().asString());
        metadata.put("logicChainRootType", session.draft.logicChainRootType());
        metadata.put("logicChainRootRef", session.draft.logicChainRootRef());
        metadata.put("logicChainDraftNodeId", session.draft.logicChainDraftNodeId());
        metadata.put("deviceType", selected.deviceType());
        metadata.put("deviceId", device.id());
        metadata.put("blockId", selected.blockId());
        boolean protectedDraftRecorded = completeProtectedDraft(
                session,
                player,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE,
                world,
                placePos,
                VirtualBlockDeviceSupport.blockId(previousState),
                metadata,
                "placed",
                "世界设备放置成功，已记录为 Logic Chain protected draft。",
                "Logic Chain 世界设备放置完成。"
        );
        if (!protectedDraftRecorded) {
            SignalDeviceStore.remove(server, world, placePos);
            world.setBlockState(placePos, previousState, Block.NOTIFY_ALL);
        }
    }

    private static void handleRegionControllerCorner(
            MinecraftServer server,
            ServerPlayerEntity player,
            WebAdminSelectionSession session,
            BlockHitResult hitResult
    ) {
        if (server == null || player == null || session == null || hitResult == null) {
            return;
        }
        ServerWorld world = player.getCommandSource().getWorld();
        BlockPos pos = hitResult.getBlockPos();
        if (player.squaredDistanceTo(pos.toCenterPos()) > MAX_SELECTION_DISTANCE_SQUARED) {
            failAndClose(session, player, "server_validation", "区域角点超出可交互距离，请靠近后重新选择。", Map.of("pos", posSummary(world, pos)));
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        String existingDimension = REGION_SELECTION_DIMENSIONS.get(session.selectionId);
        if (existingDimension != null && !existingDimension.isBlank() && !existingDimension.equals(worldId)) {
            failAndClose(session, player, "server_validation", "区域角点必须在同一维度内选择。", Map.of("dimensionId", worldId));
            return;
        }
        REGION_SELECTION_DIMENSIONS.put(session.selectionId, worldId);
        List<RegionGeometry.Point> points = new java.util.ArrayList<>(REGION_SELECTION_POINTS.getOrDefault(session.selectionId, List.of()));
        RegionGeometry.Point clicked = new RegionGeometry.Point(pos.getX(), pos.getZ());
        if (points.size() >= 3 && samePoint(points.getFirst(), clicked)) {
            completeRegionControllerSelection(session, player, world, points);
            return;
        }
        if (!points.isEmpty() && samePoint(points.getLast(), clicked)) {
            player.sendMessage(Text.literal("该区域角点已标记，无需重复添加。").formatted(Formatting.YELLOW), false);
            return;
        }
        points.add(clicked);
        REGION_SELECTION_POINTS.put(session.selectionId, List.copyOf(points));
        String message = points.size() == 1
                ? "已标记第一个区域角点；继续右键添加角点。"
                : "已添加区域角点 " + points.size() + "；至少 3 点后回到首点完成游戏内确认。";
        player.sendMessage(Text.literal(message).formatted(Formatting.AQUA), false);
        sendEnd(player, "region_points", session, message, Map.of(
                "regionPointCount", points.size(),
                "regionPoints", regionPointsSummary(points),
                "regionLinePreview", true
        ));
    }

    private static void completeRegionControllerSelection(
            WebAdminSelectionSession session,
            ServerPlayerEntity player,
            ServerWorld world,
            List<RegionGeometry.Point> points
    ) {
        if (session == null || player == null || world == null || points == null || points.size() < 3) {
            return;
        }
        if (!RegionGeometry.isSimplePolygon(points)) {
            failAndClose(session, player, "invalid_region_shape", "区域角点必须形成不自交且有面积的多边形，请重新选择。", Map.of(
                    "regionPointCount", points.size(),
                    "regionPoints", regionPointsSummary(points)
            ));
            return;
        }
        session.completing = true;
        RegionGeometry.Point first = points.getFirst();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("selectionId", session.selectionId);
        metadata.put("purpose", session.purpose.id());
        metadata.put("logicChainRootType", session.draft.logicChainRootType());
        metadata.put("logicChainRootRef", session.draft.logicChainRootRef());
        metadata.put("logicChainDraftNodeId", session.draft.logicChainDraftNodeId());
        metadata.put("regionPointCount", points.size());
        metadata.put("regionPoints", regionPointsSummary(points));
        metadata.put("regionPointsStructured", structuredRegionPoints(points));
        metadata.put("requiresWebUiConfirm", true);
        completeProtectedDraft(
                session,
                player,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_REGION_CONTROLLER,
                world,
                new BlockPos(first.x(), player.getBlockY(), first.z()),
                "",
                metadata,
                "selected",
                "区域角点选择完成，等待 WebAdmin 确认创建 Region + RegionController 草稿。",
                "Logic Chain 区域控制器角点选择完成。"
        );
    }

    private static boolean completeProtectedDraft(
            WebAdminSelectionSession session,
            ServerPlayerEntity player,
            String objectType,
            ServerWorld world,
            BlockPos pos,
            String previousBlockState,
            Map<String, Object> metadata,
            String status,
            String playerMessage,
            String eventSummary
    ) {
        String worldId = world.getRegistryKey().getValue().toString();
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry draftEntry = WebAdminProtectedDraftRegistry.markSelectedBlock(
                session.draft.draftSessionId(),
                session.draft.editLockId(),
                session.actorUsername,
                player.getUuidAsString(),
                objectType,
                worldId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                previousBlockState,
                "",
                metadata
        );
        if (draftEntry == null) {
            failAndClose(session, player, "protected_draft_inactive", "Logic Chain protected draft 已取消或已过期，请重新从 WebAdmin 发起选择。", Map.of(
                    "draftSessionId", session.draft.draftSessionId(),
                    "editLockId", session.draft.editLockId()
            ));
            return false;
        }
        removeActive(session, player);
        player.sendMessage(Text.literal(playerMessage).formatted(Formatting.GREEN), false);
        sendEnd(player, "complete_ack", session, playerMessage, Map.of("protectedDraftId", draftEntry.draftSessionId()));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", status);
        after.put("protectedDraftId", draftEntry.draftSessionId());
        after.put("draftSessionId", draftEntry.draftSessionId());
        after.put("objectType", draftEntry.objectType());
        after.put("world", worldId);
        after.put("x", pos.getX());
        after.put("y", pos.getY());
        after.put("z", pos.getZ());
        after.put("targetPlayer", session.targetPlayerName);
        after.put("selectionId", session.selectionId);
        after.put("draftOnly", true);
        if (metadata != null) {
            after.putAll(metadata);
        }
        WebAdminRealtimeEvent event = publishSelectionEvent(WebAdminRealtimeEventType.SELECTION_COMPLETED, session, eventSummary, after);
        rememberTerminal(session, status, after);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "已记录 Logic Chain 受保护草稿选择，尚未写入正式配置。",
                "PROTECTED_DRAFT",
                draftEntry.draftSessionId(),
                false,
                List.of(),
                "",
                event == null ? "" : event.id(),
                false,
                Map.of(),
                Map.of("selection", after, "protectedDraft", draftEntry.toMap())
        );
        audit(contextFor(session, new WebAdminWriteTarget("PROTECTED_DRAFT", draftEntry.draftSessionId(), "Logic Chain 受保护草稿")), result, Map.of(), after);
        return true;
    }

    private static boolean playerRaycastMatches(ServerPlayerEntity player, BlockPos pos) {
        HitResult hitResult = player.raycast(Math.sqrt(MAX_SELECTION_DISTANCE_SQUARED) + 0.25D, 0.0F, false);
        return hitResult instanceof BlockHitResult blockHitResult
                && hitResult.getType() == HitResult.Type.BLOCK
                && blockHitResult.getBlockPos().equals(pos);
    }

    private static void applyWorldDeviceHotbarMode(ServerPlayerEntity player, WebAdminSelectionSession session) {
        if (player == null || session == null || WORLD_DEVICE_HOTBAR_SNAPSHOTS.containsKey(session.selectionId)) {
            return;
        }
        java.util.List<ItemStack> mainStacks = new java.util.ArrayList<>();
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            mainStacks.add(stack.copy());
        }
        ItemStack offHand = player.getOffHandStack().copy();
        ItemStack cursor = player.currentScreenHandler == null ? ItemStack.EMPTY : player.currentScreenHandler.getCursorStack().copy();
        int selectedSlot = player.getInventory().getSelectedSlot();
        WORLD_DEVICE_HOTBAR_SNAPSHOTS.put(session.selectionId, new HotbarSnapshot(mainStacks, offHand, cursor, selectedSlot, player.getUuid()));
        int hotbarLimit = Math.min(9, player.getInventory().getMainStacks().size());
        for (int index = 0; index < hotbarLimit; index++) {
            player.getInventory().getMainStacks().set(index, ItemStack.EMPTY);
        }
        player.getInventory().getMainStacks().set(0, new ItemStack(ModBlocks.SIGNAL_EMITTER));
        player.getInventory().getMainStacks().set(1, new ItemStack(ModBlocks.SIGNAL_RECEIVER));
        player.getInventory().getMainStacks().set(2, new ItemStack(ModBlocks.ACTION_RELAY));
        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        if (player.currentScreenHandler != null) {
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        }
        session.worldDeviceSelectedSlot = 0;
        player.getInventory().setSelectedSlot(0);
        syncInventory(player);
    }

    private static void restoreWorldDeviceHotbarMode(WebAdminSelectionSession session) {
        restoreWorldDeviceHotbarMode(session, null);
    }

    private static void restoreWorldDeviceHotbarMode(WebAdminSelectionSession session, ServerPlayerEntity fallbackPlayer) {
        HotbarSnapshot snapshot = session == null ? null : WORLD_DEVICE_HOTBAR_SNAPSHOTS.get(session.selectionId);
        if (snapshot == null) {
            return;
        }
        ServerPlayerEntity player = fallbackPlayer != null && fallbackPlayer.getUuid().equals(snapshot.playerUuid())
                ? fallbackPlayer
                : findOnlinePlayer(session);
        if (player == null) {
            return;
        }
        restoreWorldDeviceHotbarSnapshot(session.selectionId, snapshot, player);
    }

    private static void restoreWorldDeviceHotbarSnapshot(String selectionId, HotbarSnapshot snapshot, ServerPlayerEntity player) {
        if (snapshot == null || player == null || !player.getUuid().equals(snapshot.playerUuid())) {
            return;
        }
        java.util.List<ItemStack> stacks = player.getInventory().getMainStacks();
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack restored = index < snapshot.mainStacks().size() ? snapshot.mainStacks().get(index).copy() : ItemStack.EMPTY;
            stacks.set(index, restored);
        }
        player.setStackInHand(Hand.OFF_HAND, snapshot.offHand().copy());
        if (player.currentScreenHandler != null) {
            player.currentScreenHandler.setCursorStack(snapshot.cursor().copy());
        }
        player.getInventory().setSelectedSlot(snapshot.selectedSlot());
        WORLD_DEVICE_HOTBAR_SNAPSHOTS.remove(selectionId);
        syncInventory(player);
    }

    private static void syncInventory(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
    }

    private static int normalizeWorldDeviceSelectedSlot(int slot) {
        return slot < 0 || slot > 2 ? 0 : slot;
    }

    private static SelectedWorldDevice selectedWorldDevice(ServerPlayerEntity player, int selectedSlot) {
        if (player == null) {
            return null;
        }
        int slot = normalizeWorldDeviceSelectedSlot(selectedSlot);
        if (slot < 0 || slot > 2) {
            return null;
        }
        ItemStack stack = player.getInventory().getStack(slot);
        if (stack.isOf(ModBlocks.SIGNAL_EMITTER.asItem())) {
            return new SelectedWorldDevice(ModBlocks.SIGNAL_EMITTER, SignalDeviceData.TYPE_SIGNAL_EMITTER, "tzz_mod:signal_emitter");
        }
        if (stack.isOf(ModBlocks.SIGNAL_RECEIVER.asItem())) {
            return new SelectedWorldDevice(ModBlocks.SIGNAL_RECEIVER, SignalDeviceData.TYPE_SIGNAL_RECEIVER, "tzz_mod:signal_receiver");
        }
        if (stack.isOf(ModBlocks.ACTION_RELAY.asItem())) {
            return new SelectedWorldDevice(ModBlocks.ACTION_RELAY, SignalDeviceData.TYPE_ACTION_RELAY, "tzz_mod:action_relay");
        }
        return null;
    }

    private static SignalDeviceData upsertPlacedWorldDevice(ServerWorld world, BlockPos pos, String deviceType) {
        if (world == null || pos == null) {
            return null;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (SignalDeviceData.TYPE_SIGNAL_EMITTER.equals(deviceType) && blockEntity instanceof SignalEmitterBlockEntity emitter) {
            return SignalDeviceStore.upsertEmitter(world, pos, emitter);
        }
        if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(deviceType) && blockEntity instanceof SignalReceiverBlockEntity receiver) {
            return SignalDeviceStore.upsertReceiver(world, pos, receiver);
        }
        if (SignalDeviceData.TYPE_ACTION_RELAY.equals(deviceType) && blockEntity instanceof ActionRelayBlockEntity relay) {
            return SignalDeviceStore.upsertActionRelay(world, pos, relay);
        }
        return null;
    }

    private static Map<String, Object> cleanupPlacedProtectedDraft(MinecraftServer server, WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry) {
        if (entry == null || !WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE.equals(entry.objectType())) {
            return Map.of("success", true, "changed", false, "cleanup", "not_required");
        }
        if (server == null) {
            return Map.of("success", false, "changed", false, "message", "服务器不可用，无法回滚世界设备 protected draft。");
        }
        ServerWorld world = resolveWorld(server, entry.worldId());
        if (world == null) {
            return Map.of("success", false, "changed", false, "message", "protected draft 所在维度不可用，无法回滚世界设备。", "world", entry.worldId());
        }
        BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
        if (!world.isInBuildLimit(pos) || !world.isChunkLoaded(pos)) {
            return Map.of("success", false, "changed", false, "message", "protected draft 所在区块未加载，无法安全回滚世界设备。", "world", entry.worldId(), "pos", pos.toShortString());
        }
        BlockState currentState = world.getBlockState(pos);
        String expectedBlockId = metadataString(entry, "blockId");
        String currentBlockId = VirtualBlockDeviceSupport.blockId(currentState);
        if (!currentState.isAir() && (!VirtualBlockDeviceSupport.isDedicatedSignalDevice(currentState)
                || (!expectedBlockId.isBlank() && !expectedBlockId.equals(currentBlockId)))) {
            return Map.of(
                    "success", false,
                    "changed", false,
                    "message", "protected draft 方块已被外部修改，无法安全回滚；已 fail closed，请人工检查该位置。",
                    "world", entry.worldId(),
                    "pos", pos.toShortString(),
                    "expectedBlockId", expectedBlockId,
                    "actualBlockId", currentBlockId
            );
        }
        BlockState restoreState = restoreState(entry.previousBlockState());
        boolean blockRestored = world.setBlockState(pos, restoreState, Block.NOTIFY_ALL);
        if (!blockRestored) {
            return Map.of("success", false, "changed", false, "message", "protected draft 方块回滚失败，已 fail closed；SignalDeviceStore 未移除，请人工检查该位置。", "world", entry.worldId(), "pos", pos.toShortString());
        }
        boolean storeRemoved = SignalDeviceStore.remove(server, world, pos);
        return Map.of(
                "success", true,
                "changed", storeRemoved || !currentState.isOf(restoreState.getBlock()),
                "cleanup", "world_device_rollback",
                "storeRemoved", storeRemoved,
                "blockRestored", blockRestored,
                "worldDevicePreviousBlockStateRestore", true,
                "world", entry.worldId(),
                "x", entry.x(),
                "y", entry.y(),
                "z", entry.z()
        );
    }

    private static ServerWorld resolveWorld(MinecraftServer server, String dimensionId) {
        if (server == null) {
            return null;
        }
        Identifier id = Identifier.tryParse(safe(dimensionId));
        if (id == null) {
            return null;
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        return server.getWorld(key);
    }

    private static boolean shouldBlockProtectedDraftMutation(ServerPlayerEntity player, ServerWorld world, BlockPos pos, String verb) {
        if (player == null || world == null || pos == null) {
            return false;
        }
        currentServer = world.getServer();
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = activeWorldDeviceProtectedDraftAt(world, pos);
        if (entry == null) {
            return false;
        }
        player.sendMessage(Text.literal("该方块是 Logic Chain protected draft，不能用普通方式" + verb + "；请回 WebAdmin 保存或取消草稿。").formatted(Formatting.YELLOW), false);
        return true;
    }

    private static WebAdminProtectedDraftRegistry.ProtectedDraftEntry activeWorldDeviceProtectedDraftAt(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        return WebAdminProtectedDraftRegistry.findActiveByWorldPos(
                Set.of(WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE),
                worldId,
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    private static BlockState restoreState(String blockId) {
        Identifier id = Identifier.tryParse(safe(blockId));
        if (id == null) {
            return Blocks.AIR.getDefaultState();
        }
        Block block = Registries.BLOCK.get(id);
        return block == null ? Blocks.AIR.getDefaultState() : block.getDefaultState();
    }

    private static String metadataString(WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry, String key) {
        Object value = entry == null || entry.metadata() == null ? null : entry.metadata().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean samePoint(RegionGeometry.Point left, RegionGeometry.Point right) {
        return left != null && right != null && left.x() == right.x() && left.z() == right.z();
    }

    private static String regionPointsSummary(List<RegionGeometry.Point> points) {
        StringBuilder builder = new StringBuilder();
        for (RegionGeometry.Point point : points == null ? List.<RegionGeometry.Point>of() : points) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(point.x()).append(',').append(point.z());
        }
        return builder.toString();
    }

    private static List<Map<String, Integer>> structuredRegionPoints(List<RegionGeometry.Point> points) {
        java.util.ArrayList<Map<String, Integer>> structured = new java.util.ArrayList<>();
        for (RegionGeometry.Point point : points == null ? List.<RegionGeometry.Point>of() : points) {
            structured.add(Map.of("x", point.x(), "z", point.z()));
        }
        return List.copyOf(structured);
    }

    private static String selectionStartSummary(WebAdminSelectionSession session) {
        if (session == null) {
            return "等待玩家在游戏内选择方块。";
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE) {
            return "等待玩家使用三格 hotbar 放置世界设备。";
        }
        if (session.purpose == WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT) {
            return "等待玩家右键标记 RegionController 区域角点。";
        }
        return "等待玩家在游戏内选择方块。";
    }

    private static WebAdminWriteResult cancelSession(
            WebAdminSelectionSession session,
            String source,
            String message,
            boolean notifyPlayer,
            boolean publish
    ) {
        return cancelSession(session, source, message, notifyPlayer, publish, null);
    }

    private static WebAdminWriteResult cancelSession(
            WebAdminSelectionSession session,
            String source,
            String message,
            boolean notifyPlayer,
            boolean publish,
            ServerPlayerEntity knownPlayer
    ) {
        ServerPlayerEntity player = knownPlayer == null ? findOnlinePlayer(session) : knownPlayer;
        cancelProtectedDraftForSession(session);
        removeActive(session, player);
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
        cancelProtectedDraftForSession(session);
        removeActive(session, player);
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

    private static void cancelProtectedDraftForSession(WebAdminSelectionSession session) {
        if (session == null || session.purpose == WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE || session.draft == null) {
            return;
        }
        String draftSessionId = session.draft.draftSessionId();
        if (isBlank(draftSessionId)) {
            return;
        }
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.get(draftSessionId);
        if (entry == null) {
            WebAdminProtectedDraftRegistry.cancel(draftSessionId);
            return;
        }
        if (entry.isTerminal()) {
            return;
        }
        if (!cleanupAndCancelProtectedDraft(currentServer, entry, "selection_terminal")) {
            Tzz_mod.LOGGER.warn("Logic Chain protected draft {} was left active because cleanup failed during selection terminalization.", draftSessionId);
        }
    }

    private static void cleanupExpiredWorldDeviceProtectedDrafts(MinecraftServer server, long now) {
        for (WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry : WebAdminProtectedDraftRegistry.activeExpiredByObjectType(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE,
                now
        )) {
            if (cleanupPlacedWorldDeviceDraft(server, entry, "selection_expired")) {
                WebAdminProtectedDraftRegistry.markExpired(entry.draftSessionId(), "selection_expired");
            }
        }
    }

    private static void cleanupAllActiveWorldDeviceProtectedDrafts(MinecraftServer server, String reason) {
        for (WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry : WebAdminProtectedDraftRegistry.activeByObjectType(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE
        )) {
            cleanupAndCancelProtectedDraft(server, entry, reason);
        }
    }

    private static boolean cleanupAndCancelProtectedDraft(
            MinecraftServer server,
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry,
            String reason
    ) {
        if (entry == null || entry.isTerminal()) {
            return false;
        }
        if (!cleanupPlacedWorldDeviceDraft(server, entry, reason)) {
            return false;
        }
        WebAdminProtectedDraftRegistry.cancel(entry.draftSessionId());
        return true;
    }

    private static boolean cleanupPlacedWorldDeviceDraft(
            MinecraftServer server,
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry,
            String reason
    ) {
        if (entry == null || !WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE.equals(entry.objectType())) {
            return true;
        }
        if (entry.worldId().isBlank() || entry.blockPos().isBlank()) {
            return true;
        }
        Map<String, Object> cleanup = cleanupPlacedProtectedDraft(server, entry);
        if (Boolean.FALSE.equals(cleanup.get("success"))) {
            Tzz_mod.LOGGER.warn("Failed to cleanup Logic Chain world-device protected draft {} on {}: {}", entry.draftSessionId(), reason, cleanup.get("message"));
            return false;
        }
        return true;
    }

    private static void removeActive(WebAdminSelectionSession session) {
        removeActive(session, null);
    }

    private static void removeActive(WebAdminSelectionSession session, ServerPlayerEntity knownPlayer) {
        if (session == null) {
            return;
        }
        restoreWorldDeviceHotbarMode(session, knownPlayer);
        REGION_SELECTION_POINTS.remove(session.selectionId);
        REGION_SELECTION_DIMENSIONS.remove(session.selectionId);
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
        data.put("draftSessionId", session.draft.draftSessionId());
        data.put("editLockId", session.draft.editLockId());
        data.put("logicChainRootType", session.draft.logicChainRootType());
        data.put("logicChainRootRef", session.draft.logicChainRootRef());
        data.put("logicChainDraftNodeId", session.draft.logicChainDraftNodeId());
        data.put("createdAtMillis", session.createdAtMillis);
        return data;
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

    private record HotbarSnapshot(List<ItemStack> mainStacks, ItemStack offHand, ItemStack cursor, int selectedSlot, UUID playerUuid) {
    }

    private record SelectedWorldDevice(Block block, String deviceType, String blockId) {
    }

    private static WebAdminUser snapshotActor(WebAdminSelectionSession session) {
        WebAdminUser user = new WebAdminUser();
        user.username = session == null ? "" : session.actorUsername;
        user.displayName = user.username;
        user.role = session == null || session.actorRole == null ? WebAdminRole.VIEWER.id() : session.actorRole.id();
        return user.normalized();
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
