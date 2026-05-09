package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.network.WebAdminSelectionC2SPayload;
import com.zcpu.tzzmod.network.WebAdminSelectionS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class WebAdminSelectionClient {
    private static boolean active;
    private static boolean completing;
    private static String selectionId = "";
    private static String nonce = "";
    private static String purpose = "";
    private static String title = "选择虚拟方块设备目标";
    private static String confirmHint = "右键方块确认";
    private static String cancelHint = "ESC 取消";
    private static String channel = "";

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
        clearPressedInputs(client);
    }

    public static boolean handleKey(MinecraftClient client, int action, KeyInput input) {
        if (!active || client == null) {
            return false;
        }
        if (action == 1 && input.isEscape()) {
            cancelFromClient("esc");
            return true;
        }
        if (action == 1 && matchesAny(input, client.options.useKey)) {
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
            completeFromCrosshair(client);
            return true;
        }
        return client.options.attackKey.matchesMouse(click);
    }

    public static boolean shouldConsumeMouseScroll() {
        return active;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client == null || client.player == null || client.world == null) {
            return;
        }
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int panelW = Math.min(360, Math.max(32, sw - 16));
        int panelH = channel.isBlank() ? 60 : 76;
        int x = Math.max(4, Math.min(sw - panelW - 4, sw / 2 - panelW / 2));
        int y = Math.max(4, sh - panelH - 12);
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
        drawTrimmed(context, client, title, textX, textY, maxTextWidth, 0xFFBFFBFF);
        drawTrimmed(context, client, confirmHint + " · " + cancelHint, textX, textY + 16, maxTextWidth, 0xFF7CE7F0);
        String target = targetText(client);
        drawTrimmed(context, client, target, textX, textY + 34, maxTextWidth, 0xFFE7F7FF);
        if (!channel.isBlank()) {
            drawTrimmed(context, client, "channel: " + channel, textX, textY + 52, maxTextWidth, 0xFF93A8B8);
        }
    }

    private static void handlePayload(MinecraftClient client, WebAdminSelectionS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "begin" -> activate(client, body);
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
        title = fallback(getString(body, "title"), "选择虚拟方块设备目标");
        confirmHint = fallback(getString(body, "confirmHint"), "右键方块确认");
        cancelHint = fallback(getString(body, "cancelHint"), "ESC 取消");
        channel = getString(body, "channel");
        completing = false;
        active = true;
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
        if (!active || completing || client == null || client.player == null || client.world == null) {
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

    private static void cancelFromClient(String reason) {
        if (!active) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("selectionId", selectionId);
        body.addProperty("nonce", nonce);
        body.addProperty("reason", reason);
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
        channel = "";
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
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            hotbarKey.setPressed(false);
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
                return true;
            }
        }
        return false;
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

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
