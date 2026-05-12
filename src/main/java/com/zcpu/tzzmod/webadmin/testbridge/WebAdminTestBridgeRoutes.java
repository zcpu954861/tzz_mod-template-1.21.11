package com.zcpu.tzzmod.webadmin.testbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDoctorService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class WebAdminTestBridgeRoutes {
    private static final int MIN_TEST_X = -128;
    private static final int MAX_TEST_X = 128;
    private static final int MIN_TEST_Y = -64;
    private static final int MAX_TEST_Y = 320;
    private static final int MIN_TEST_Z = -128;
    private static final int MAX_TEST_Z = 128;
    private static final int MAX_CLEAR_VOLUME = 4096;
    private static final int MAX_GIVE_COUNT = 2304;
    private static final int MAX_SIGNAL_HISTORY_LIMIT = 200;
    private static final PositionRequest DEFAULT_PREPARE_MIN = new PositionRequest(-16, -60, -16);
    private static final PositionRequest DEFAULT_PREPARE_MAX = new PositionRequest(16, -58, 16);
    private static final PositionRequest DEFAULT_PLAYER_POS = new PositionRequest(0, -57, 0);
    private static final List<String> COMMAND_ALLOWLIST = List.of("tzz", "setblock", "give", "clear", "tp", "time", "weather", "say");
    private static final List<String> COMMAND_DENYLIST = List.of("stop", "op", "deop", "ban", "kick", "whitelist", "save-off", "save-on", "pardon", "reload");

    private final WebAdminTestBridgeSecurityService security = new WebAdminTestBridgeSecurityService();
    private final WebAdminDeviceService deviceService = new WebAdminDeviceService();
    private final WebAdminSignalService signalService = new WebAdminSignalService();
    private final WebAdminDoctorService doctorService = new WebAdminDoctorService();

    public void handle(HttpExchange exchange, MinecraftServer server, String path, String method) throws IOException {
        if (path.equals("/api/testbridge/status")) {
            WebAdminTestBridgeSecurityService.AccessResult access = security.requireLoopback(exchange);
            if (!access.allowed()) {
                WebAdminJsonResponse.error(exchange, access.status(), access.code(), access.message());
                return;
            }
            if (security.enabled()) {
                access = security.requireEnabledAndToken(exchange);
                if (!access.allowed()) {
                    WebAdminJsonResponse.error(exchange, access.status(), access.code(), access.message());
                    return;
                }
            }
            requireMethod(exchange, method, "GET");
            WebAdminJsonResponse.ok(exchange, status(server));
            return;
        }

        WebAdminTestBridgeSecurityService.AccessResult access = security.requireEnabledAndToken(exchange);
        if (!access.allowed()) {
            WebAdminJsonResponse.error(exchange, access.status(), access.code(), access.message());
            return;
        }

        try {
            if (path.equals("/api/testbridge/players")) {
                requireMethod(exchange, method, "GET");
                WebAdminJsonResponse.ok(exchange, players(server));
                return;
            }
            if (path.equals("/api/testbridge/gui/current")) {
                requireMethod(exchange, method, "GET");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "current", GuiOperationRequest.fromQuery(queryParams(exchange)), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/slots")) {
                requireMethod(exchange, method, "GET");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "slots", GuiOperationRequest.fromQuery(queryParams(exchange)), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/put-item")) {
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "put_item", readJson(exchange, GuiOperationRequest.class), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/clear-slot")) {
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "clear_slot", readJson(exchange, GuiOperationRequest.class), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/set-count")) {
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "set_count", readJson(exchange, GuiOperationRequest.class), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/save")) {
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "save", readJson(exchange, GuiOperationRequest.class), exchange));
                return;
            }
            if (path.equals("/api/testbridge/gui/cancel")) {
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, guiOperation(server, "cancel", readJson(exchange, GuiOperationRequest.class), exchange));
                return;
            }
            if (path.equals("/api/testbridge/command")) {
                requireMethod(exchange, method, "POST");
                CommandRequest request = readJson(exchange, CommandRequest.class);
                WebAdminJsonResponse.ok(exchange, command(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/world/set-block")) {
                requireMethod(exchange, method, "POST");
                SetBlockRequest request = readJson(exchange, SetBlockRequest.class);
                WebAdminJsonResponse.ok(exchange, setBlock(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/world/clear-area")) {
                requireMethod(exchange, method, "POST");
                ClearAreaRequest request = readJson(exchange, ClearAreaRequest.class);
                WebAdminJsonResponse.ok(exchange, clearArea(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/world/prepare-area")) {
                requireMethod(exchange, method, "POST");
                PrepareAreaRequest request = readJson(exchange, PrepareAreaRequest.class);
                WebAdminJsonResponse.ok(exchange, prepareArea(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/world/prepare-player")) {
                requireMethod(exchange, method, "POST");
                PreparePlayerRequest request = readJson(exchange, PreparePlayerRequest.class);
                WebAdminJsonResponse.ok(exchange, preparePlayer(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/world/prepare")) {
                requireMethod(exchange, method, "POST");
                PrepareWorldRequest request = readJson(exchange, PrepareWorldRequest.class);
                WebAdminJsonResponse.ok(exchange, prepareWorld(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/player/give")) {
                requireMethod(exchange, method, "POST");
                PlayerItemRequest request = readJson(exchange, PlayerItemRequest.class);
                WebAdminJsonResponse.ok(exchange, giveItem(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/player/clear-inventory")) {
                requireMethod(exchange, method, "POST");
                PlayerRequest request = readJson(exchange, PlayerRequest.class);
                WebAdminJsonResponse.ok(exchange, clearInventory(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/player/set-main-hand")) {
                requireMethod(exchange, method, "POST");
                PlayerItemRequest request = readJson(exchange, PlayerItemRequest.class);
                WebAdminJsonResponse.ok(exchange, setMainHand(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/player/use-block")) {
                requireMethod(exchange, method, "POST");
                UseBlockRequest request = readJson(exchange, UseBlockRequest.class);
                WebAdminJsonResponse.ok(exchange, useBlock(server, request, exchange));
                return;
            }
            if (path.equals("/api/testbridge/device/inspect")) {
                if (method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.ok(exchange, inspectDevice(server, InspectDeviceRequest.fromQuery(queryParams(exchange))));
                    return;
                }
                requireMethod(exchange, method, "POST");
                WebAdminJsonResponse.ok(exchange, inspectDevice(server, readJson(exchange, InspectDeviceRequest.class)));
                return;
            }
            if (path.equals("/api/testbridge/signal/history")) {
                requireMethod(exchange, method, "GET");
                WebAdminJsonResponse.ok(exchange, signalHistory(server, queryParams(exchange)));
                return;
            }
            if (path.equals("/api/testbridge/doctor/issues")) {
                requireMethod(exchange, method, "GET");
                WebAdminJsonResponse.ok(exchange, doctorIssues(server));
                return;
            }
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "未知 TestBridge endpoint。");
        } catch (ResponseSentException exception) {
            return;
        } catch (TestBridgeException exception) {
            WebAdminJsonResponse.error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (JsonSyntaxException exception) {
            WebAdminJsonResponse.error(exchange, 400, "VALIDATION_FAILED", "请求 JSON 格式无效。");
        }
    }

    private Map<String, Object> status(MinecraftServer server) {
        List<Map<String, Object>> worlds = new ArrayList<>();
        if (server != null) {
            for (ServerWorld world : server.getWorlds()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("dimension", dimension(world));
                entry.put("time", world.getTime());
                worlds.add(entry);
            }
        }
        List<String> playerNames = server == null
                ? List.of()
                : server.getPlayerManager().getPlayerList().stream().map(player -> player.getName().getString()).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", security.enabled());
        data.put("ready", security.enabled() && security.tokenConfigured() && server != null && !worlds.isEmpty());
        data.put("serverLoaded", server != null);
        data.put("worldLoaded", !worlds.isEmpty());
        data.put("worlds", worlds);
        data.put("onlinePlayers", playerNames);
        data.put("webAdminReady", true);
        data.put("version", "testbridge-foundation-1");
        data.put("safetyMode", "dev-only loopback token test-area-restricted");
        data.put("tokenConfigured", security.tokenConfigured());
        data.put("testArea", testArea());
        return data;
    }

    private Map<String, Object> players(MinecraftServer server) {
        requireServerReady(server);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            result.add(playerDto(player));
        }
        return Map.of("players", result);
    }

    private Map<String, Object> command(MinecraftServer server, CommandRequest request, HttpExchange exchange) {
        requireServerReady(server);
        String normalized = normalizeCommand(request == null ? "" : request.command);
        String root = commandRoot(normalized);
        if (root.isBlank() || COMMAND_DENYLIST.contains(root) || !COMMAND_ALLOWLIST.contains(root)) {
            WebAdminAuditLogger.testBridge("command", "DENIED", security.sourceIp(exchange), "root=" + root);
            throw new TestBridgeException(403, "COMMAND_DENIED", "命令不在 TestBridge allowlist，或属于危险命令。");
        }
        ServerWorld world = defaultWorld(server);
        ServerPlayerEntity player = resolveOptionalPlayer(server, request == null ? "" : request.player);
        ServerCommandSource source = player == null
                ? server.getCommandSource().withPermissions(PermissionPredicate.ALL).withSilent().withWorld(world).withPosition(Vec3d.ZERO)
                : player.getCommandSource().withPermissions(PermissionPredicate.ALL).withSilent();
        try {
            int result = server.getCommandManager().getDispatcher().execute(normalized, source);
            WebAdminAuditLogger.testBridge("command", "OK", security.sourceIp(exchange), "root=" + root);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("command", "/" + normalized);
            data.put("result", result);
            data.put("allowedRoot", root);
            return data;
        } catch (CommandSyntaxException exception) {
            WebAdminAuditLogger.testBridge("command", "FAILED", security.sourceIp(exchange), "root=" + root);
            throw new TestBridgeException(400, "COMMAND_FAILED", exception.getMessage());
        }
    }

    private JsonObject guiOperation(MinecraftServer server, String operation, GuiOperationRequest request, HttpExchange exchange) {
        GuiOperationRequest safeRequest = request == null ? new GuiOperationRequest("", "", null, null, "", 0, "") : request;
        if (safe(safeRequest.player).isBlank()) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "GUI 操作需要 player。");
        }
        JsonObject body = WebAdminJsonResponse.GSON.toJsonTree(safeRequest).getAsJsonObject();
        WebAdminTestBridgeClientGuiBridge.Result result = WebAdminTestBridgeClientGuiBridge.request(server, safeRequest.player, operation, body);
        WebAdminAuditLogger.testBridge("gui_" + operation, result.ok() ? "OK" : "FAILED", security.sourceIp(exchange), "player=" + safe(safeRequest.player) + " code=" + result.code());
        if (!result.ok()) {
            throw new TestBridgeException(statusForGuiCode(result.code()), result.code(), result.message());
        }
        JsonObject data = result.data() == null ? new JsonObject() : result.data();
        data.addProperty("testbridgeGuiOperation", operation);
        data.addProperty("usesClientScreenAbstraction", true);
        data.addProperty("rawSignalDeviceDataWrite", false);
        return data;
    }

    private static int statusForGuiCode(String code) {
        return switch (safe(code)) {
            case "NOT_FOUND" -> 404;
            case "VALIDATION_FAILED" -> 400;
            case "CLIENT_TIMEOUT" -> 504;
            case "GUI_NOT_OPEN", "UNSUPPORTED_GUI", "SCREEN_MISMATCH", "CLIENT_TESTBRIDGE_UNAVAILABLE" -> 409;
            case "SESSION_DENIED", "SESSION_EXPIRED" -> 403;
            default -> 400;
        };
    }

    private Map<String, Object> setBlock(MinecraftServer server, SetBlockRequest request, HttpExchange exchange) {
        requireServerReady(server);
        if (request == null) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "缺少 set_block 请求。");
        }
        ServerWorld world = worldByDimension(server, request.dimension);
        BlockPos pos = pos(request.x, request.y, request.z);
        requireInsideTestArea(pos);
        requireChunkLoaded(world, pos);
        BlockState state = blockState(request.blockId, request.properties);
        boolean changed = world.setBlockState(pos, state, Block.NOTIFY_ALL);
        WebAdminAuditLogger.testBridge("set_block", changed ? "OK" : "NO_CHANGE", security.sourceIp(exchange), dimension(world) + "@" + pos.toShortString());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("changed", changed);
        data.put("dimension", dimension(world));
        data.put("pos", posDto(pos));
        data.put("blockState", blockStateDto(world.getBlockState(pos)));
        return data;
    }

    private Map<String, Object> clearArea(MinecraftServer server, ClearAreaRequest request, HttpExchange exchange) {
        requireServerReady(server);
        if (request == null || request.min == null || request.max == null) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "clear_area 需要 min/max 坐标。");
        }
        ServerWorld world = worldByDimension(server, request.dimension);
        AreaBounds bounds = bounds(request.min, request.max, "clear_area");
        AreaEditResult result = clearAreaBlocks(world, bounds);
        WebAdminAuditLogger.testBridge("clear_area", "OK", security.sourceIp(exchange), dimension(world) + " volume=" + bounds.volume + " changed=" + result.changed);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", dimension(world));
        data.put("min", posDto(bounds.min));
        data.put("max", posDto(bounds.max));
        data.put("volume", bounds.volume);
        data.put("changedBlocks", result.changed);
        data.put("skippedUnloadedBlocks", result.skippedUnloaded);
        return data;
    }

    private Map<String, Object> prepareArea(MinecraftServer server, PrepareAreaRequest request, HttpExchange exchange) {
        requireServerReady(server);
        PrepareAreaRequest safeRequest = request == null ? new PrepareAreaRequest("", null, null, "", null) : request;
        ServerWorld world = worldByDimension(server, safeRequest.dimension);
        PositionRequest minRequest = safeRequest.min == null ? DEFAULT_PREPARE_MIN : safeRequest.min;
        PositionRequest maxRequest = safeRequest.max == null ? DEFAULT_PREPARE_MAX : safeRequest.max;
        AreaBounds bounds = bounds(minRequest, maxRequest, "prepare_test_area");
        AreaEditResult cleared = clearAreaBlocks(world, bounds);
        String floorBlockId = safe(safeRequest.floorBlockId).isBlank() ? "minecraft:stone" : safeRequest.floorBlockId;
        int floorBlocks = 0;
        if (bool(safeRequest.placeFloor, true)) {
            floorBlocks = placeFloor(world, bounds, blockState(floorBlockId, Map.of()));
        }
        WebAdminAuditLogger.testBridge("prepare_test_area", "OK", security.sourceIp(exchange), dimension(world) + " volume=" + bounds.volume + " changed=" + cleared.changed + " floor=" + floorBlocks);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", dimension(world));
        data.put("min", posDto(bounds.min));
        data.put("max", posDto(bounds.max));
        data.put("volume", bounds.volume);
        data.put("changedBlocks", cleared.changed);
        data.put("skippedUnloadedBlocks", cleared.skippedUnloaded);
        data.put("floorBlocks", floorBlocks);
        data.put("floorBlockId", floorBlockId);
        data.put("testArea", testArea());
        return data;
    }

    private Map<String, Object> preparePlayer(MinecraftServer server, PreparePlayerRequest request, HttpExchange exchange) {
        if (request == null) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "prepare_test_player 需要 player。");
        }
        ServerPlayerEntity player = requirePlayer(server, request.player);
        ServerWorld world = worldByDimension(server, safe(request.dimension).isBlank() ? dimension(player.getCommandSource().getWorld()) : request.dimension);
        PositionRequest target = request.position == null ? DEFAULT_PLAYER_POS : request.position;
        BlockPos targetBlock = pos(target.x, target.y, target.z);
        requireInsideTestArea(targetBlock);
        requireChunkLoaded(world, targetBlock);
        Map<String, Object> clearSummary = Map.of("skipped", true);
        if (bool(request.clearInventory, true)) {
            clearSummary = clearPlayerInventory(player);
        }
        boolean offhandCleared = false;
        if (bool(request.clearOffhand, true)) {
            offhandCleared = !player.getOffHandStack().isEmpty();
            player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
        boolean teleported = false;
        if (bool(request.teleport, true)) {
            teleported = player.teleport(world, target.x + 0.5D, target.y, target.z + 0.5D, Set.of(), player.getYaw(), player.getPitch(), false);
        }
        syncInventory(player);
        WebAdminAuditLogger.testBridge("prepare_test_player", "OK", security.sourceIp(exchange), player.getName().getString() + " teleported=" + teleported);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", player.getName().getString());
        data.put("dimension", dimension(player.getCommandSource().getWorld()));
        data.put("target", posDto(targetBlock));
        data.put("teleported", teleported);
        data.put("clearInventory", clearSummary);
        data.put("offhandCleared", offhandCleared);
        data.put("mainHand", itemStackSummary(player.getMainHandStack()));
        data.put("position", Map.of("x", player.getX(), "y", player.getY(), "z", player.getZ()));
        return data;
    }

    private Map<String, Object> prepareWorld(MinecraftServer server, PrepareWorldRequest request, HttpExchange exchange) {
        requireServerReady(server);
        PrepareWorldRequest safeRequest = request == null ? new PrepareWorldRequest("", "", null, null, null, null, null, null, null) : request;
        ServerWorld world = worldByDimension(server, safeRequest.dimension);
        Map<String, Object> area = Map.of("skipped", true);
        if (bool(safeRequest.prepareArea, true)) {
            area = prepareArea(server, safeRequest.area == null ? new PrepareAreaRequest(dimension(world), null, null, "", null) : withDimension(safeRequest.area, dimension(world)), exchange);
        }
        Map<String, Object> player = Map.of("skipped", true);
        if (!safe(safeRequest.player).isBlank() && bool(safeRequest.preparePlayer, true)) {
            PreparePlayerRequest playerRequest = safeRequest.playerSetup == null
                    ? new PreparePlayerRequest(safeRequest.player, dimension(world), DEFAULT_PLAYER_POS, true, true, true)
                    : withPlayerAndDimension(safeRequest.playerSetup, safeRequest.player, dimension(world));
            player = preparePlayer(server, playerRequest, exchange);
        }
        boolean timeSet = false;
        boolean weatherCleared = false;
        if (bool(safeRequest.setDayTime, true)) {
            world.setTimeOfDay(1000L);
            timeSet = true;
        }
        if (bool(safeRequest.clearWeather, true)) {
            world.setWeather(6000, 0, false, false);
            weatherCleared = true;
        }
        WebAdminAuditLogger.testBridge("prepare_test_world", "OK", security.sourceIp(exchange), dimension(world) + " player=" + safe(safeRequest.player));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", dimension(world));
        data.put("worldLoaded", true);
        data.put("area", area);
        data.put("player", player);
        data.put("timeSet", timeSet);
        data.put("weatherCleared", weatherCleared);
        data.put("idempotent", true);
        data.put("testArea", testArea());
        return data;
    }

    private Map<String, Object> giveItem(MinecraftServer server, PlayerItemRequest request, HttpExchange exchange) {
        ServerPlayerEntity player = requirePlayer(server, request == null ? "" : request.player);
        Item item = item(request == null ? "" : request.itemId);
        int requestedCount = boundedCount(request == null ? 0 : request.count, 1, MAX_GIVE_COUNT);
        int remaining = requestedCount;
        int inserted = 0;
        while (remaining > 0) {
            int next = Math.min(new ItemStack(item).getMaxCount(), remaining);
            ItemStack stack = new ItemStack(item, next);
            if (!player.getInventory().insertStack(stack)) {
                break;
            }
            inserted += next - stack.getCount();
            remaining -= next - stack.getCount();
            if (!stack.isEmpty()) {
                break;
            }
        }
        syncInventory(player);
        WebAdminAuditLogger.testBridge("give_item", inserted == requestedCount ? "OK" : "PARTIAL", security.sourceIp(exchange), player.getName().getString() + " item=" + Registries.ITEM.getId(item) + " count=" + inserted);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", player.getName().getString());
        data.put("item", Registries.ITEM.getId(item).toString());
        data.put("requestedCount", requestedCount);
        data.put("insertedCount", inserted);
        data.put("partial", inserted != requestedCount);
        return data;
    }

    private Map<String, Object> clearInventory(MinecraftServer server, PlayerRequest request, HttpExchange exchange) {
        ServerPlayerEntity player = requirePlayer(server, request == null ? "" : request.player);
        Map<String, Object> cleared = clearPlayerInventory(player);
        syncInventory(player);
        WebAdminAuditLogger.testBridge("clear_inventory", "OK", security.sourceIp(exchange), player.getName().getString() + " stacks=" + cleared.get("clearedStacks"));
        return cleared;
    }

    private Map<String, Object> clearPlayerInventory(ServerPlayerEntity player) {
        int clearedStacks = 0;
        int clearedItems = 0;
        var stacks = player.getInventory().getMainStacks();
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (!stack.isEmpty()) {
                clearedStacks++;
                clearedItems += stack.getCount();
                stacks.set(index, ItemStack.EMPTY);
            }
        }
        return Map.of(
                "player", player.getName().getString(),
                "clearedStacks", clearedStacks,
                "clearedItems", clearedItems
        );
    }

    private Map<String, Object> setMainHand(MinecraftServer server, PlayerItemRequest request, HttpExchange exchange) {
        ServerPlayerEntity player = requirePlayer(server, request == null ? "" : request.player);
        Item item = item(request == null ? "" : request.itemId);
        ItemStack template = new ItemStack(item);
        int count = boundedCount(request == null ? 0 : request.count, 1, template.getMaxCount());
        ItemStack stack = new ItemStack(item, count);
        player.setStackInHand(Hand.MAIN_HAND, stack);
        syncInventory(player);
        WebAdminAuditLogger.testBridge("set_main_hand", "OK", security.sourceIp(exchange), player.getName().getString() + " item=" + Registries.ITEM.getId(item));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", player.getName().getString());
        data.put("mainHand", itemStackSummary(player.getMainHandStack()));
        return data;
    }

    private Map<String, Object> useBlock(MinecraftServer server, UseBlockRequest request, HttpExchange exchange) {
        ServerPlayerEntity player = requirePlayer(server, request == null ? "" : request.player);
        ServerWorld world = worldByDimension(server, request == null ? "" : request.dimension);
        BlockPos pos = pos(request == null ? 0 : request.x, request == null ? 0 : request.y, request == null ? 0 : request.z);
        requireInsideTestArea(pos);
        requireChunkLoaded(world, pos);
        Hand hand = parseHand(request == null ? "" : request.hand);
        Direction side = parseDirection(request == null ? "" : request.side);
        int before = SignalEventHistory.size();
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), side, pos, false);
        ActionResult actionResult = UseBlockCallback.EVENT.invoker().interact(player, world, hand, hit);
        List<SignalEventRecord> newEvents = SignalEventHistory.snapshot().stream().skip(before).toList();
        SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(server, world, pos);
        WebAdminAuditLogger.testBridge("use_block", "OK", security.sourceIp(exchange), player.getName().getString() + " " + dimension(world) + "@" + pos.toShortString());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", player.getName().getString());
        data.put("dimension", dimension(world));
        data.put("pos", posDto(pos));
        data.put("hand", hand.name().toLowerCase(Locale.ROOT));
        data.put("side", side.name().toLowerCase(Locale.ROOT));
        data.put("actionResult", String.valueOf(actionResult));
        data.put("signalEvents", newEvents.stream().map(WebAdminTestBridgeRoutes::signalRecordDto).toList());
        data.put("device", device == null ? Map.of() : deviceSummary(device.normalized()));
        return data;
    }

    private Map<String, Object> inspectDevice(MinecraftServer server, InspectDeviceRequest request) {
        requireServerReady(server);
        SignalDeviceData device = resolveDevice(server, request);
        if (device == null) {
            throw new TestBridgeException(404, "NOT_FOUND", "未找到匹配设备。");
        }
        SignalDeviceData normalized = device.normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("device", deviceSummary(normalized));
        data.put("detail", deviceService.detail(server, normalized));
        data.put("debug", deviceService.debug(server, normalized));
        return data;
    }

    private Map<String, Object> signalHistory(MinecraftServer server, Map<String, String> query) {
        requireServerReady(server);
        String channel = query.getOrDefault("channel", "");
        int limit = boundedCount(parseInt(query.get("limit"), 50), 1, MAX_SIGNAL_HISTORY_LIMIT);
        List<WebAdminDtos.SignalHistoryEntryDto> history = signalService.history(server, channel, limit);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("channel", SignalChannel.normalize(channel));
        data.put("limit", limit);
        data.put("events", history);
        data.put("available", true);
        return data;
    }

    private Map<String, Object> doctorIssues(MinecraftServer server) {
        requireServerReady(server);
        WebAdminDtos.DoctorReportDto report = doctorService.report(server);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", report.summary());
        data.put("issues", report.issues());
        return data;
    }

    private SignalDeviceData resolveDevice(MinecraftServer server, InspectDeviceRequest request) {
        if (request == null) {
            return null;
        }
        if (!safe(request.deviceId).isBlank()) {
            SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, request.deviceId);
            return resolved.foundUnique() ? resolved.device().normalized() : null;
        }
        if (!safe(request.dimension).isBlank()) {
            for (SignalDeviceData device : SignalDeviceStore.getSnapshot(server)) {
                SignalDeviceData normalized = device.normalized();
                if (normalized.dimension().equals(request.dimension)
                        && normalized.x() == request.x
                        && normalized.y() == request.y
                        && normalized.z() == request.z) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private static Map<String, Object> playerDto(ServerPlayerEntity player) {
        ServerWorld world = player.getCommandSource().getWorld();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", player.getName().getString());
        data.put("uuid", player.getUuidAsString());
        data.put("dimension", dimension(world));
        data.put("position", Map.of("x", player.getX(), "y", player.getY(), "z", player.getZ()));
        data.put("blockPos", posDto(player.getBlockPos()));
        data.put("mainHand", itemStackSummary(player.getMainHandStack()));
        data.put("screenOpen", player.currentScreenHandler != player.playerScreenHandler);
        data.put("screenHandler", player.currentScreenHandler == null ? "" : player.currentScreenHandler.getClass().getSimpleName());
        data.put("health", player.getHealth());
        data.put("gameMode", String.valueOf(player.interactionManager.getGameMode()));
        return data;
    }

    private static Map<String, Object> deviceSummary(SignalDeviceData device) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", device.id());
        data.put("type", device.type());
        data.put("name", device.name());
        data.put("dimension", device.dimension());
        data.put("pos", Map.of("x", device.x(), "y", device.y(), "z", device.z()));
        data.put("enabled", device.enabled());
        data.put("channel", device.channel());
        data.put("nativeTriggers", Map.of(
                "redstoneMode", device.mode(),
                "offChannel", device.offChannel(),
                "conditionEnabled", device.conditionEnabled(),
                "interactionEnabled", device.interactionEnabled(),
                "containerEnabled", device.containerEnabled(),
                "containerChangeChannel", device.containerChangeChannel()
        ));
        data.put("itemSubmit", Map.of(
                "enabled", device.itemSubmitEnabled(),
                "consumeEnabled", device.itemSubmitConsumeEnabled(),
                "consumeOrder", device.itemSubmitConsumeOrder(),
                "requirementCount", device.itemSubmitRequirements().size(),
                "lastMatched", device.lastItemSubmitMatched(),
                "lastFailureReason", device.lastItemSubmitFailureReason(),
                "lastResult", device.lastItemSubmitResult()
        ));
        data.put("itemConditions", Map.of("count", device.itemConditions().size()));
        data.put("runtime", Map.of(
                "lastResult", device.lastResult(),
                "lastInteractionResult", device.lastInteractionResult(),
                "lastContainerResult", device.lastContainerResult()
        ));
        return data;
    }

    private static Map<String, Object> signalRecordDto(SignalEventRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gameTime", record.gameTime());
        data.put("wallTimeMillis", record.wallTimeMillis());
        data.put("channel", record.channel());
        data.put("playerName", record.playerName());
        data.put("sourceType", record.sourceType());
        data.put("sourceId", record.sourceId());
        data.put("listenerCount", record.listenerCount());
        data.put("executedCount", record.executedCount());
        data.put("failedCount", record.failedCount());
        data.put("resultMessage", record.resultMessage());
        return data;
    }

    private static Map<String, Object> itemStackSummary(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Map.of("empty", true);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("empty", false);
        data.put("itemId", Registries.ITEM.getId(stack.getItem()).toString());
        data.put("count", stack.getCount());
        data.put("maxCount", stack.getMaxCount());
        data.put("name", stack.getName().getString());
        return data;
    }

    private static Map<String, Object> blockStateDto(BlockState state) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            properties.put(property.getName(), propertyValueName(state, property));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blockId", Registries.BLOCK.getId(state.getBlock()).toString());
        data.put("properties", properties);
        return data;
    }

    private static Map<String, Object> posDto(BlockPos pos) {
        return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
    }

    private Map<String, Object> testArea() {
        return Map.of(
                "min", Map.of("x", MIN_TEST_X, "y", MIN_TEST_Y, "z", MIN_TEST_Z),
                "max", Map.of("x", MAX_TEST_X, "y", MAX_TEST_Y, "z", MAX_TEST_Z),
                "maxClearVolume", MAX_CLEAR_VOLUME
        );
    }

    private static AreaBounds bounds(PositionRequest first, PositionRequest second, String operation) {
        BlockPos min = pos(Math.min(first.x, second.x), Math.min(first.y, second.y), Math.min(first.z, second.z));
        BlockPos max = pos(Math.max(first.x, second.x), Math.max(first.y, second.y), Math.max(first.z, second.z));
        requireInsideTestArea(min);
        requireInsideTestArea(max);
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume > MAX_CLEAR_VOLUME) {
            throw new TestBridgeException(400, "BOUNDS_DENIED", operation + " 超出最大体积限制：" + MAX_CLEAR_VOLUME);
        }
        return new AreaBounds(min, max, volume);
    }

    private static AreaEditResult clearAreaBlocks(ServerWorld world, AreaBounds bounds) {
        int changed = 0;
        int skipped = 0;
        BlockState air = Registries.BLOCK.get(Identifier.of("minecraft:air")).getDefaultState();
        for (int x = bounds.min.getX(); x <= bounds.max.getX(); x++) {
            for (int y = bounds.min.getY(); y <= bounds.max.getY(); y++) {
                for (int z = bounds.min.getZ(); z <= bounds.max.getZ(); z++) {
                    BlockPos current = new BlockPos(x, y, z);
                    if (!world.isChunkLoaded(current)) {
                        skipped++;
                        continue;
                    }
                    if (!world.getBlockState(current).isAir() && world.setBlockState(current, air, Block.NOTIFY_ALL)) {
                        changed++;
                    }
                }
            }
        }
        return new AreaEditResult(changed, skipped);
    }

    private static int placeFloor(ServerWorld world, AreaBounds bounds, BlockState floorState) {
        int changed = 0;
        int y = bounds.min.getY();
        for (int x = bounds.min.getX(); x <= bounds.max.getX(); x++) {
            for (int z = bounds.min.getZ(); z <= bounds.max.getZ(); z++) {
                BlockPos current = new BlockPos(x, y, z);
                if (!world.isChunkLoaded(current)) {
                    continue;
                }
                if (!world.getBlockState(current).equals(floorState) && world.setBlockState(current, floorState, Block.NOTIFY_ALL)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private static PrepareAreaRequest withDimension(PrepareAreaRequest request, String dimension) {
        return new PrepareAreaRequest(dimension, request.min, request.max, request.floorBlockId, request.placeFloor);
    }

    private static PreparePlayerRequest withPlayerAndDimension(PreparePlayerRequest request, String player, String dimension) {
        return new PreparePlayerRequest(player, dimension, request.position, request.clearInventory, request.clearOffhand, request.teleport);
    }

    private static BlockState blockState(String blockId, Map<String, String> properties) {
        Identifier id = Identifier.tryParse(safe(blockId));
        if (id == null || !Registries.BLOCK.containsId(id)) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "方块 ID 无效或不存在：" + safe(blockId));
        }
        BlockState state = Registries.BLOCK.get(id).getDefaultState();
        if (properties == null || properties.isEmpty()) {
            return state;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Property<?> property = state.getBlock().getStateManager().getProperty(entry.getKey());
            if (property == null) {
                throw new TestBridgeException(400, "VALIDATION_FAILED", "方块不支持状态：" + entry.getKey());
            }
            state = withProperty(state, property, entry.getValue());
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withProperty(BlockState state, Property<?> property, String rawValue) {
        Property rawProperty = property;
        for (Object value : rawProperty.getValues()) {
            Comparable comparable = (Comparable) value;
            if (rawProperty.name(comparable).equals(rawValue)) {
                return state.with(rawProperty, comparable);
            }
        }
        throw new TestBridgeException(400, "VALIDATION_FAILED", "状态 " + property.getName() + " 不支持值：" + rawValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(BlockState state, Property<?> property) {
        Property rawProperty = property;
        return rawProperty.name((Comparable) state.get(property));
    }

    private static Item item(String itemId) {
        Identifier id = Identifier.tryParse(safe(itemId));
        if (id == null || !Registries.ITEM.containsId(id)) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "物品 ID 无效或不存在：" + safe(itemId));
        }
        return Registries.ITEM.get(id);
    }

    private static ServerWorld worldByDimension(MinecraftServer server, String rawDimension) {
        requireServerReady(server);
        String requested = safe(rawDimension);
        if (requested.isBlank()) {
            return defaultWorld(server);
        }
        for (ServerWorld world : server.getWorlds()) {
            if (dimension(world).equals(requested)) {
                return world;
            }
        }
        throw new TestBridgeException(404, "NOT_FOUND", "未找到维度：" + requested);
    }

    private static ServerWorld defaultWorld(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) {
            throw new TestBridgeException(503, "TESTBRIDGE_NOT_READY", "Minecraft world 尚未加载。");
        }
        return world;
    }

    private static ServerPlayerEntity requirePlayer(MinecraftServer server, String rawPlayer) {
        requireServerReady(server);
        ServerPlayerEntity player = resolveOptionalPlayer(server, rawPlayer);
        if (player == null) {
            throw new TestBridgeException(404, "NOT_FOUND", "在线玩家不存在：" + safe(rawPlayer));
        }
        return player;
    }

    private static ServerPlayerEntity resolveOptionalPlayer(MinecraftServer server, String rawPlayer) {
        String value = safe(rawPlayer);
        if (value.isBlank()) {
            return null;
        }
        try {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(value));
            if (player != null) {
                return player;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to name lookup.
        }
        return server.getPlayerManager().getPlayer(value);
    }

    private static void requireServerReady(MinecraftServer server) {
        if (server == null || server.getOverworld() == null) {
            throw new TestBridgeException(503, "TESTBRIDGE_NOT_READY", "Minecraft server/world 尚未就绪。");
        }
    }

    private static void requireInsideTestArea(BlockPos pos) {
        if (pos.getX() < MIN_TEST_X || pos.getX() > MAX_TEST_X
                || pos.getY() < MIN_TEST_Y || pos.getY() > MAX_TEST_Y
                || pos.getZ() < MIN_TEST_Z || pos.getZ() > MAX_TEST_Z) {
            throw new TestBridgeException(400, "BOUNDS_DENIED", "坐标超出 TestBridge 测试区域限制。");
        }
    }

    private static void requireChunkLoaded(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            throw new TestBridgeException(409, "TESTBRIDGE_NOT_READY", "目标区块未加载，TestBridge 不会强制加载区块。");
        }
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private static int boundedCount(int value, int min, int max) {
        if (value < min || value > max) {
            throw new TestBridgeException(400, "VALIDATION_FAILED", "数量必须在 " + min + ".." + max + " 之间。");
        }
        return value;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Hand parseHand(String raw) {
        return "off_hand".equalsIgnoreCase(safe(raw)) ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    private static Direction parseDirection(String raw) {
        return switch (safe(raw).toLowerCase(Locale.ROOT)) {
            case "down" -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> Direction.UP;
        };
    }

    private static String normalizeCommand(String raw) {
        String value = safe(raw);
        while (value.startsWith("/")) {
            value = value.substring(1).trim();
        }
        return value;
    }

    private static String commandRoot(String command) {
        int space = command.indexOf(' ');
        return (space < 0 ? command : command.substring(0, space)).toLowerCase(Locale.ROOT);
    }

    private static void syncInventory(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
    }

    private static String dimension(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static boolean bool(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private static <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return null;
        }
        return WebAdminJsonResponse.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
    }

    private static void requireMethod(HttpExchange exchange, String actual, String expected) throws IOException {
        if (!actual.equalsIgnoreCase(expected)) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 " + expected + "。");
            throw new ResponseSentException();
        }
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            params.put(java.net.URLDecoder.decode(key, StandardCharsets.UTF_8), java.net.URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private record CommandRequest(String command, String player) {
    }

    private record SetBlockRequest(String dimension, int x, int y, int z, String blockId, Map<String, String> properties) {
    }

    private record ClearAreaRequest(String dimension, PositionRequest min, PositionRequest max) {
    }

    private record PrepareAreaRequest(String dimension, PositionRequest min, PositionRequest max, String floorBlockId, Boolean placeFloor) {
    }

    private record PositionRequest(int x, int y, int z) {
    }

    private record PlayerRequest(String player) {
    }

    private record PreparePlayerRequest(String player, String dimension, PositionRequest position, Boolean clearInventory, Boolean clearOffhand, Boolean teleport) {
    }

    private record PrepareWorldRequest(String dimension, String player, PrepareAreaRequest area, PreparePlayerRequest playerSetup, Boolean prepareArea, Boolean preparePlayer, Boolean setDayTime, Boolean clearWeather, Boolean idempotent) {
    }

    private record PlayerItemRequest(String player, String itemId, int count) {
    }

    private record UseBlockRequest(String player, String dimension, int x, int y, int z, String hand, String side) {
    }

    private record GuiOperationRequest(String player, String target, Integer slot, Integer slotIndex, String itemId, int count, String reason) {
        private static GuiOperationRequest fromQuery(Map<String, String> query) {
            return new GuiOperationRequest(
                    query.getOrDefault("player", ""),
                    query.getOrDefault("target", ""),
                    null,
                    null,
                    "",
                    0,
                    ""
            );
        }
    }

    private record InspectDeviceRequest(String deviceId, String dimension, int x, int y, int z) {
        private static InspectDeviceRequest fromQuery(Map<String, String> query) {
            return new InspectDeviceRequest(
                    query.getOrDefault("deviceId", ""),
                    query.getOrDefault("dimension", ""),
                    parseInt(query.get("x"), 0),
                    parseInt(query.get("y"), 0),
                    parseInt(query.get("z"), 0)
            );
        }
    }

    private record AreaBounds(BlockPos min, BlockPos max, long volume) {
    }

    private record AreaEditResult(int changed, int skippedUnloaded) {
    }

    private static final class TestBridgeException extends RuntimeException {
        private final int status;
        private final String code;

        private TestBridgeException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    private static final class ResponseSentException extends RuntimeException {
    }
}
