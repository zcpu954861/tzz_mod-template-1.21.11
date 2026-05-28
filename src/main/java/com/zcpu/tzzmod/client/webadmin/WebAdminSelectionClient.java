package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.map.RegionPlannerPreviewRenderer;
import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.network.WebAdminSelectionC2SPayload;
import com.zcpu.tzzmod.network.WebAdminSelectionS2CPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class WebAdminSelectionClient {
    private static final String SELECTION_CANCEL_CONFIRM_SURVIVES_ESC_RELEASE = "selectionCancelConfirmSurvivesEscRelease";
    private static final String DATA_LOGIC_CHAIN_REGION_PLANNER_PREVIEW_SHARED_RENDERER = "dataLogicChainRegionPlannerPreviewSharedRenderer";
    private static final String DATA_LOGIC_CHAIN_REGION_NO_WEBUI_GEOMETRY_PREVIEW = "dataLogicChainRegionNoWebuiGeometryPreview";
    private static final String DATA_LOGIC_CHAIN_WORLD_DEVICE_HUD_NO_TARGET_TEXT = "dataLogicChainWorldDeviceHudNoTargetText";
    private static final String DATA_LOGIC_CHAIN_WORLD_DEVICE_SELECTED_SLOT_SYNC = "dataLogicChainWorldDeviceSelectedSlotSync";
    private static boolean active;
    private static boolean completing;
    private static String selectionId = "";
    private static String nonce = "";
    private static String purpose = "";
    private static String title = "选择虚拟方块设备目标";
    private static String confirmHint = "右键方块确认";
    private static String cancelHint = "ESC 取消";
    private static String channel = "";
    private static boolean cancelConfirmArmed;
    private static long cancelConfirmUntilMillis;
    private static int regionPointCount;
    private static String regionPointSummary = "";
    private static final List<int[]> regionPointPreview = new ArrayList<>();

    private WebAdminSelectionClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WebAdminSelectionS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> deactivate(false));
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(MinecraftClient client) {
        if (!active || client == null) {
            return;
        }
        if (client.player == null || client.world == null) {
            deactivate(false);
            return;
        }
        ensureGameInputCaptured(client);
        if (isWorldDevicePlacementPurpose()) {
            int selectedSlot = client.player.getInventory().getSelectedSlot();
            if (selectedSlot < 0 || selectedSlot > 2) {
                setWorldDeviceSelectedSlot(client, 0, true);
            } else {
                normalizeWorldDeviceSelectedSlot(client);
            }
        }
        clearPressedInputs(client);
        renderRegionPlannerPreviewInWorld(client);
    }

    public static boolean handleKey(MinecraftClient client, int action, KeyInput input) {
        if (!active || client == null) {
            return false;
        }
        if (input.isEscape()) {
            if (action == 1) {
                requestCancelFromClient(client, "esc");
            }
            return true;
        }
        if (action != 1) {
            return false;
        }
        resetCancelConfirmation();
        if (isWorldDevicePlacementPurpose()) {
            int slot = worldDeviceHotbarKeySlot(client, input);
            if (slot >= 0) {
                setWorldDeviceSelectedSlot(client, slot, true);
                resetCancelConfirmation();
                return true;
            }
        }
        if (isServerUseBlockPurpose() && matchesAny(input, client.options.useKey)) {
            return false;
        }
        if (matchesAny(input, client.options.useKey)) {
            completeFromCrosshair(client);
            return true;
        }
        if (matchesBlockedKey(client, input)) {
            return true;
        }
        return false;
    }

    public static boolean shouldConsumeMouseClick(Click click) {
        if (!active) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        if (client.options.useKey.matchesMouse(click)) {
            if (isServerUseBlockPurpose()) {
                return false;
            }
            completeFromCrosshair(client);
            return true;
        }
        return client.options.attackKey.matchesMouse(click);
    }

    public static boolean shouldConsumeMouseScroll() {
        return active;
    }

    public static boolean isWorldDevicePlacementMode() {
        return active && isWorldDevicePlacementPurpose();
    }

    public static boolean handleMouseScroll(double vertical) {
        if (!active) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return true;
        }
        if (isWorldDevicePlacementPurpose()) {
            int current = normalizeWorldDeviceSelectedSlot(client);
            int direction = vertical > 0 ? -1 : 1;
            int next = Math.floorMod(current + direction, 3);
            setWorldDeviceSelectedSlot(client, next, true);
            resetCancelConfirmation();
        }
        return true;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client == null || client.player == null || client.world == null) {
            return;
        }
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int hotbarTop = sh;
        if (isWorldDevicePlacementPurpose()) {
            renderWorldDeviceHotbar(context, client, sw, sh);
            hotbarTop = worldDeviceHotbarTop(sh);
        }
        int reservedBottomTop = isWorldDevicePlacementPurpose() ? hotbarTop : Math.max(0, sh - 24);
        int panelW = Math.min(360, Math.max(32, sw - 16));
        int desiredPanelH = isRegionControllerPurpose() ? 104 : (renderChannelText() ? 76 : 60);
        int maxPanelH = Math.max(0, reservedBottomTop - 6);
        if (maxPanelH < 24) {
            return;
        }
        int panelH = Math.min(desiredPanelH, maxPanelH);
        int x = Math.max(4, Math.min(sw - panelW - 4, sw / 2 - panelW / 2));
        int y = Math.max(2, Math.min(reservedBottomTop - panelH - 4, 4));
        if (y + panelH > reservedBottomTop - 2) {
            panelH = Math.max(0, reservedBottomTop - y - 2);
        }
        if (panelH < 24) {
            return;
        }
        int bg = 0xCC071827;
        int border = 0xDD22D3EE;
        int maxTextWidth = Math.max(24, panelW - 24);
        context.fill(x, y, x + panelW, y + panelH, bg);
        context.fill(x, y, x + panelW, y + 1, border);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, border);
        context.fill(x, y, x + 1, y + panelH, border);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, border);
        int textX = x + 12;
        int textY = y + 9;
        if (panelH >= 28) {
            drawTrimmed(context, client, title, textX, textY, maxTextWidth, 0xFFBFFBFF);
        }
        if (panelH >= 44) {
            drawTrimmed(context, client, confirmHint + " · " + cancelHint, textX, textY + 16, maxTextWidth, 0xFF7CE7F0);
        }
        if (panelH >= 58) {
            String target = isWorldDevicePlacementPurpose() ? worldDeviceHudSelectionText(client) : targetText(client);
            drawTrimmed(context, client, target, textX, textY + 34, maxTextWidth, 0xFFE7F7FF);
        }
        if (panelH >= 72 && isRegionControllerPurpose()) {
            String regionPreview = regionPointCount <= 0
                    ? "至少 3 个角点后，回到首点右键完成"
                    : "已选 " + regionPointCount + " 点，游戏内粒子点线预览：" + regionPointSummary;
            drawTrimmed(context, client, regionPreview, textX, textY + 52, maxTextWidth, 0xFF93A8B8);
        } else if (panelH >= 72 && renderChannelText()) {
            drawTrimmed(context, client, "channel: " + channel, textX, textY + 52, maxTextWidth, 0xFF93A8B8);
        }
    }

    private static void handlePayload(MinecraftClient client, WebAdminSelectionS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "begin" -> activate(client, body);
            case "region_points" -> updateRegionPoints(body);
            case "cancel", "failed", "complete_ack" -> {
                deactivate(false);
            }
            default -> {
            }
        }
    }

    private static void activate(MinecraftClient client, JsonObject body) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (client.currentScreen != null) {
            client.setScreen(null);
        }
        if (CameraModeClient.isActive()) {
            CameraModeClient.deactivate(client);
        }
        selectionId = getString(body, "selectionId");
        nonce = getString(body, "nonce");
        purpose = getString(body, "purpose");
        title = titleForPurpose(purpose, getString(body, "title"));
        confirmHint = confirmHintForPurpose(purpose, getString(body, "confirmHint"));
        cancelHint = fallback(getString(body, "cancelHint"), "ESC 取消");
        channel = getString(body, "channel");
        cancelConfirmArmed = false;
        cancelConfirmUntilMillis = 0L;
        regionPointCount = 0;
        regionPointSummary = "";
        regionPointPreview.clear();
        completing = false;
        active = true;
        if (isWorldDevicePlacementPurpose()) {
            setWorldDeviceSelectedSlot(client, normalizeWorldDeviceSelectedSlot(client), true);
        }
        ensureGameInputCaptured(client);
        clearPressedInputs(client);
    }

    private static void ensureGameInputCaptured(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (client.currentScreen != null) {
            client.setScreen(null);
        }
        if (client.currentScreen == null && client.mouse != null && !client.mouse.isCursorLocked()) {
            client.mouse.lockCursor();
        }
    }

    private static void completeFromCrosshair(MinecraftClient client) {
        if (!active || completing || isServerUseBlockPurpose() || client == null || client.player == null || client.world == null) {
            return;
        }
        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult hitResult) || target.getType() != HitResult.Type.BLOCK) {
            client.player.sendMessage(Text.literal("请对准方块后右键确认。").formatted(Formatting.YELLOW), false);
            return;
        }
        BlockPos pos = hitResult.getBlockPos();
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", selectionId);
        body.addProperty("nonce", nonce);
        body.addProperty("purpose", purpose);
        body.addProperty("dimensionId", client.world.getRegistryKey().getValue().toString());
        body.addProperty("x", pos.getX());
        body.addProperty("y", pos.getY());
        body.addProperty("z", pos.getZ());
        body.addProperty("side", hitResult.getSide().asString());
        send("complete", body);
        completing = true;
        deactivate(false);
    }

    private static void requestCancelFromClient(MinecraftClient client, String reason) {
        if (!active) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!cancelConfirmArmed || now > cancelConfirmUntilMillis) {
            cancelConfirmArmed = true;
            cancelConfirmUntilMillis = now + 4000L;
            cancelHint = "再次按 ESC 确认取消";
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("再次按 ESC 确认取消；当前进度会丢弃，已放置的草稿设备会由 WebAdmin 清理。").formatted(Formatting.YELLOW), false);
            }
            return;
        }
        cancelFromClient(reason);
    }

    private static void cancelFromClient(String reason) {
        if (!active) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", selectionId);
        body.addProperty("nonce", nonce);
        body.addProperty("reason", reason);
        body.addProperty("confirmed", true);
        send("cancel", body);
        deactivate(false);
    }

    private static void send(String action, JsonObject body) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new WebAdminSelectionC2SPayload(action, body.toString()));
    }

    private static void deactivate(boolean notify) {
        active = false;
        completing = false;
        selectionId = "";
        nonce = "";
        purpose = "";
        title = "选择虚拟方块设备目标";
        confirmHint = "右键方块确认";
        cancelHint = "ESC 取消";
        channel = "";
        cancelConfirmArmed = false;
        cancelConfirmUntilMillis = 0L;
        regionPointCount = 0;
        regionPointSummary = "";
        regionPointPreview.clear();
        if (notify) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("已退出选择模式。"), false);
            }
        }
    }

    private static void clearPressedInputs(MinecraftClient client) {
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.inventoryKey.setPressed(false);
        client.options.pickItemKey.setPressed(false);
        client.options.dropKey.setPressed(false);
        client.options.swapHandsKey.setPressed(false);
        client.options.chatKey.setPressed(false);
        client.options.commandKey.setPressed(false);
        for (int index = 0; index < client.options.hotbarKeys.length; index++) {
            if (isWorldDevicePlacementPurpose() && index < 3) {
                continue;
            }
            client.options.hotbarKeys[index].setPressed(false);
        }
    }

    private static boolean matchesBlockedKey(MinecraftClient client, KeyInput input) {
        if (matchesAny(input,
                client.options.inventoryKey,
                client.options.swapHandsKey,
                client.options.dropKey,
                client.options.pickItemKey,
                client.options.saveToolbarActivatorKey,
                client.options.loadToolbarActivatorKey,
                client.options.advancementsKey,
                client.options.chatKey,
                client.options.commandKey,
                client.options.playerListKey,
                client.options.socialInteractionsKey,
                client.options.attackKey,
                client.options.useKey)) {
            return true;
        }
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(input)) {
                return !isWorldDevicePlacementPurpose() || !matchesAllowedWorldDeviceHotbarKey(client, input);
            }
        }
        return false;
    }

    private static boolean isServerUseBlockPurpose() {
        return isWorldDevicePlacementPurpose() || isRegionControllerPurpose();
    }

    private static boolean isWorldDevicePlacementPurpose() {
        return "logic_chain_world_device_place".equals(purpose);
    }

    private static boolean isRegionControllerPurpose() {
        return "logic_chain_region_controller_select".equals(purpose) || "logic_chain_region_select".equals(purpose);
    }

    private static boolean matchesAllowedWorldDeviceHotbarKey(MinecraftClient client, KeyInput input) {
        return worldDeviceHotbarKeySlot(client, input) >= 0;
    }

    private static int worldDeviceHotbarKeySlot(MinecraftClient client, KeyInput input) {
        if (client == null || client.options == null || client.options.hotbarKeys == null) {
            return -1;
        }
        int limit = Math.min(3, client.options.hotbarKeys.length);
        for (int index = 0; index < limit; index++) {
            if (client.options.hotbarKeys[index].matchesKey(input)) {
                return index;
            }
        }
        return -1;
    }

    private static int normalizeWorldDeviceSelectedSlot(MinecraftClient client) {
        if (client == null || client.player == null) {
            return 0;
        }
        int slot = client.player.getInventory().getSelectedSlot();
        if (slot < 0 || slot > 2) {
            slot = 0;
            client.player.getInventory().setSelectedSlot(slot);
        }
        return slot;
    }

    private static void setWorldDeviceSelectedSlot(MinecraftClient client, int slot, boolean notifyServer) {
        if (client == null || client.player == null) {
            return;
        }
        int normalized = Math.max(0, Math.min(2, slot));
        client.player.getInventory().setSelectedSlot(normalized);
        if (notifyServer) {
            syncWorldDeviceSelectedSlot(normalized);
        }
    }

    private static void syncWorldDeviceSelectedSlot(int slot) {
        if (!active || !isWorldDevicePlacementPurpose()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", selectionId);
        body.addProperty("nonce", nonce);
        body.addProperty("slot", Math.max(0, Math.min(2, slot)));
        body.addProperty("deviceType", worldDeviceType(slot));
        body.addProperty("marker", DATA_LOGIC_CHAIN_WORLD_DEVICE_SELECTED_SLOT_SYNC);
        send("world_device_slot", body);
    }

    private static String worldDeviceHudSelectionText(MinecraftClient client) {
        if (DATA_LOGIC_CHAIN_WORLD_DEVICE_HUD_NO_TARGET_TEXT.isBlank()) {
            return "";
        }
        return "当前选中设备：" + selectedWorldDeviceLabel(client);
    }

    private static void resetCancelConfirmation() {
        cancelConfirmArmed = false;
        cancelConfirmUntilMillis = 0L;
        cancelHint = "ESC 取消";
    }

    private static void renderWorldDeviceHotbar(DrawContext context, MinecraftClient client, int sw, int sh) {
        int slotSize = worldDeviceHotbarSlotSize(sh);
        int gap = 3;
        int total = slotSize * 3 + gap * 2;
        if (sh < slotSize + 2) {
            return;
        }
        int x = Math.max(4, sw / 2 - total / 2);
        int y = worldDeviceHotbarTop(sh);
        int selected = normalizeWorldDeviceSelectedSlot(client);
        int bgTop = Math.max(0, y - 6);
        int bgBottom = Math.min(sh, y + slotSize + 14);
        context.fill(x - 6, bgTop, x + total + 6, bgBottom, 0xD0061421);
        context.fill(x - 6, bgTop, x + total + 6, Math.min(bgTop + 1, bgBottom), 0xDD22D3EE);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * (slotSize + gap);
            int border = i == selected ? 0xFFEAB308 : 0xDD35556D;
            context.fill(sx, y, sx + slotSize, y + slotSize, 0xEE0F2433);
            context.fill(sx, y, sx + slotSize, y + 1, border);
            context.fill(sx, y + slotSize - 1, sx + slotSize, y + slotSize, border);
            context.fill(sx, y, sx + 1, y + slotSize, border);
            context.fill(sx + slotSize - 1, y, sx + slotSize, y + slotSize, border);
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                context.drawItem(stack, sx + Math.max(1, (slotSize - 16) / 2), y + Math.max(1, (slotSize - 16) / 2));
            }
            if (y + slotSize + 11 <= sh) {
                context.drawTextWithShadow(client.textRenderer, Text.literal(String.valueOf(i + 1)), sx + 8, y + slotSize + 2, i == selected ? 0xFFFFF7AD : 0xFF8FB4C7);
            }
        }
    }

    private static void updateRegionPoints(JsonObject body) {
        regionPointCount = getInt(body, "regionPointCount", regionPointCount);
        regionPointSummary = fallback(getString(body, "regionPoints"), regionPointSummary);
        regionPointPreview.clear();
        for (String part : regionPointSummary.split(";")) {
            String[] pieces = part.split(",");
            if (pieces.length != 2) {
                continue;
            }
            try {
                regionPointPreview.add(new int[]{Integer.parseInt(pieces[0].trim()), Integer.parseInt(pieces[1].trim())});
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void renderRegionPlannerPreviewInWorld(MinecraftClient client) {
        if (!isRegionControllerPurpose() || regionPointPreview.isEmpty()) {
            return;
        }
        List<RegionGeometry.Point> points = new ArrayList<>(regionPointPreview.size());
        for (int[] point : regionPointPreview) {
            if (point.length >= 2) {
                points.add(new RegionGeometry.Point(point[0], point[1]));
            }
        }
        RegionPlannerPreviewRenderer.renderSelectionPreview(client, points, 0x22D3EE);
    }

    private static int worldDeviceHotbarSlotSize(int scaledHeight) {
        return scaledHeight < 84 ? 16 : 22;
    }

    private static int worldDeviceHotbarTop(int scaledHeight) {
        int slotSize = worldDeviceHotbarSlotSize(scaledHeight);
        int height = slotSize + 20;
        int maxTop = Math.max(0, scaledHeight - slotSize - 2);
        return Math.max(0, Math.min(Math.max(2, scaledHeight - height - 2), maxTop));
    }

    private static boolean renderChannelText() {
        return !channel.isBlank() && !isWorldDevicePlacementPurpose() && !isRegionControllerPurpose();
    }

    private static String selectedWorldDeviceLabel(MinecraftClient client) {
        int slot = normalizeWorldDeviceSelectedSlot(client);
        return switch (slot) {
            case 1 -> "SignalReceiver 接收端";
            case 2 -> "ActionRelay 执行端";
            default -> "SignalEmitter 发射端";
        };
    }

    private static String worldDeviceType(int slot) {
        return switch (Math.max(0, Math.min(2, slot))) {
            case 1 -> "signal_receiver";
            case 2 -> "action_relay";
            default -> "signal_emitter";
        };
    }

    private static String titleForPurpose(String value, String fallbackTitle) {
        String clean = value == null ? "" : value;
        if ("logic_chain_world_device_place".equals(clean)) {
            return "放置 Logic Chain 世界设备";
        }
        if ("logic_chain_region_controller_select".equals(clean) || "logic_chain_region_select".equals(clean)) {
            return "选择 Logic Chain 区域角点";
        }
        if ("logic_chain_vbd_select".equals(clean)) {
            return "选择 Logic Chain VBD 方块";
        }
        return fallback(fallbackTitle, "选择虚拟方块设备目标");
    }

    private static String confirmHintForPurpose(String value, String fallbackHint) {
        String clean = value == null ? "" : value;
        if ("logic_chain_world_device_place".equals(clean)) {
            return "1-3 选择设备，右键空气侧放置";
        }
        if ("logic_chain_region_controller_select".equals(clean) || "logic_chain_region_select".equals(clean)) {
            return "右键角点，回到首点完成";
        }
        return fallback(fallbackHint, "右键方块确认");
    }

    private static boolean matchesAny(KeyInput input, KeyBinding... bindings) {
        for (KeyBinding binding : bindings) {
            if (binding != null && binding.matchesKey(input)) {
                return true;
            }
        }
        return false;
    }

    private static String targetText(MinecraftClient client) {
        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult hitResult) || target.getType() != HitResult.Type.BLOCK) {
            return "当前未指向方块";
        }
        BlockPos pos = hitResult.getBlockPos();
        String blockId = Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).toString();
        return blockId + "  " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void drawTrimmed(DrawContext context, MinecraftClient client, String text, int x, int y, int width, int color) {
        String shown = client.textRenderer.trimToWidth(text == null ? "" : text, Math.max(12, width));
        context.drawTextWithShadow(client.textRenderer, Text.literal(shown), x, y, color);
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

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
