package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiC2SPayload;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiS2CPayload;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Util;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class WebAdminTestBridgeGuiClient {
    private static final AtomicReference<PendingScreenshot> PENDING_SCREENSHOT = new AtomicReference<>();

    private WebAdminTestBridgeGuiClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WebAdminTestBridgeGuiS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> capturePendingScreenshot(MinecraftClient.getInstance(), false));
        WorldRenderEvents.END_MAIN.register(context -> capturePendingScreenshot(MinecraftClient.getInstance(), false));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register((currentScreen, drawContext, mouseX, mouseY, tickDelta) ->
                        capturePendingScreenshot(client, true))
        );
    }

    private static void handlePayload(MinecraftClient client, WebAdminTestBridgeGuiS2CPayload payload) {
        if ("client_screenshot".equals(payload.operation())) {
            handleClientScreenshot(client, payload);
            return;
        }
        JsonObject envelope;
        try {
            JsonObject body = parse(payload.bodyJson());
            JsonObject data = dispatch(client, payload.operation(), body);
            envelope = ok(data);
        } catch (GuiOperationException exception) {
            envelope = failed(exception.code, exception.getMessage(), exception.data);
        } catch (IllegalArgumentException exception) {
            envelope = failed("VALIDATION_FAILED", exception.getMessage(), new JsonObject());
        } catch (Exception exception) {
            envelope = failed("SCREEN_OPERATION_FAILED", exception.getMessage(), new JsonObject());
        }
        ClientPlayNetworking.send(new WebAdminTestBridgeGuiC2SPayload(
                payload.requestId(),
                payload.nonce(),
                payload.operation(),
                envelope.toString()
        ));
    }

    private static void handleClientScreenshot(MinecraftClient client, WebAdminTestBridgeGuiS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        JsonObject base = currentBase(screenType(client == null ? null : client.currentScreen), client == null ? null : client.currentScreen);
        if (client == null || client.getFramebuffer() == null) {
            send(payload, failed("CLIENT_NOT_READY", "Minecraft client 尚未准备好截图。", base));
            return;
        }
        String outputPath = getString(body, "outputPath");
        String gameDirectory = getString(body, "gameDirectory");
        String fileName = getString(body, "fileName");
        if (outputPath.isBlank() || !outputPath.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            send(payload, failed("VALIDATION_FAILED", "截图输出路径无效。", base));
            return;
        }
        if (gameDirectory.isBlank() || fileName.isBlank() || !fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            send(payload, failed("VALIDATION_FAILED", "截图输出参数无效。", base));
            return;
        }
        Path path;
        File gameDirectoryFile;
        try {
            path = Path.of(outputPath).toAbsolutePath().normalize();
            gameDirectoryFile = Path.of(gameDirectory).toAbsolutePath().normalize().toFile();
            Path parent = path.getParent();
            if (parent == null) {
                send(payload, failed("VALIDATION_FAILED", "截图输出目录无效。", base));
                return;
            }
            Files.createDirectories(parent);
            Files.createDirectories(gameDirectoryFile.toPath());
        } catch (Exception exception) {
            send(payload, failed("SCREENSHOT_FAILED", "无法准备截图输出目录：" + exception.getMessage(), base));
            return;
        }

        int timeoutMs = getInt(body, "timeoutMs", 60000);
        long expiresAtMillis = System.currentTimeMillis() + Math.max(1000, Math.min(timeoutMs, 120000));
        PendingScreenshot pending = new PendingScreenshot(payload, path, expiresAtMillis);
        if (!PENDING_SCREENSHOT.compareAndSet(null, pending)) {
            send(payload, failed("SCREENSHOT_BUSY", "已有 Minecraft 客户端截图请求正在执行。", base));
            return;
        }
        JsonObject queued = base.deepCopy();
        queued.addProperty("path", path.toString());
        queued.addProperty("fileName", path.getFileName().toString());
        queued.addProperty("screenshotQueued", true);
        queued.addProperty("usesClientScreenshotPayload", true);
        queued.addProperty("usesMinecraftClientFramebuffer", true);
        queued.addProperty("usesOsScreenshot", false);
        queued.addProperty("usesCoordinateClicking", false);
        send(payload, ok(queued));
    }

    private static void capturePendingScreenshot(MinecraftClient client, boolean screenRenderPass) {
        PendingScreenshot pending = PENDING_SCREENSHOT.get();
        if (pending == null || client == null || client.getFramebuffer() == null) {
            return;
        }
        if (!PENDING_SCREENSHOT.compareAndSet(pending, null)) {
            return;
        }
        if (System.currentTimeMillis() > pending.expiresAtMillis) {
            JsonObject base = currentBase(screenType(client.currentScreen), client.currentScreen);
            send(pending.payload, failed("TIMEOUT", "Minecraft 客户端截图请求已超时。", base));
            return;
        }
        JsonObject base = currentBase(screenType(client.currentScreen), client.currentScreen);
        try {
            Tzz_mod.LOGGER.info("TestBridge client screenshot capture start screenType={}", screenType(client.currentScreen));
            NativeImage image = readFramebuffer(client);
            Tzz_mod.LOGGER.info("TestBridge client screenshot framebuffer read complete");
            Util.getIoWorkerExecutor().execute(() -> {
                try (image) {
                    image.writeTo(pending.path);
                    Tzz_mod.LOGGER.info("TestBridge client screenshot saved {}", pending.path);
                    client.execute(() -> Tzz_mod.LOGGER.info("TestBridge client screenshot write confirmed {}", pending.path));
                } catch (Exception exception) {
                    client.execute(() -> Tzz_mod.LOGGER.warn("TestBridge client screenshot write failed", exception));
                }
            });
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("TestBridge client screenshot capture failed", exception);
        }
    }

    private static NativeImage readFramebuffer(MinecraftClient client) {
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("Minecraft 客户端窗口 framebuffer 尺寸无效。");
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException("OpenGL framebuffer read failed: " + error);
        }
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceIndex = (x + y * width) * 4;
                int color = pixels.getInt(sourceIndex) | 0xFF000000;
                image.setColor(x, height - y - 1, color);
            }
        }
        return image;
    }

    private static JsonObject dispatch(MinecraftClient client, String operation, JsonObject body) {
        Screen screen = client == null ? null : client.currentScreen;
        if (screen instanceof WebAdminContainerTemplatePreviewScreen containerScreen) {
            return dispatchContainer(containerScreen, operation, body);
        }
        if (screen instanceof WebAdminSingleItemSubmitTemplateScreen singleScreen) {
            return dispatchSingle(singleScreen, operation, body);
        }
        if ("current".equals(operation)) {
            JsonObject current = currentBase(screen == null ? "none" : "unsupported", screen);
            current.addProperty("supported", false);
            current.addProperty("open", screen != null);
            current.add("slots", new com.google.gson.JsonArray());
            return current;
        }
        throw new GuiOperationException(screen == null ? "GUI_NOT_OPEN" : "UNSUPPORTED_GUI",
                screen == null ? "目标玩家当前没有打开 GUI。" : "当前 GUI 不受 TestBridge 抽象操作支持。",
                currentBase(screen == null ? "none" : "unsupported", screen));
    }

    private static JsonObject dispatchContainer(WebAdminContainerTemplatePreviewScreen screen, String operation, JsonObject body) {
        requireTarget(body, "container_template", screen.testBridgeSnapshot(false, false));
        return switch (operation) {
            case "current" -> screen.testBridgeSnapshot(false, false);
            case "slots" -> screen.testBridgeSnapshot(true, false);
            case "put_item" -> screen.testBridgePutItem(getInt(body, "slot", getInt(body, "slotIndex", 0)), getString(body, "itemId"), getInt(body, "count", 1));
            case "clear_slot" -> screen.testBridgeClearSlot(getInt(body, "slot", getInt(body, "slotIndex", 0)));
            case "set_count" -> screen.testBridgeSetCount(getInt(body, "slot", getInt(body, "slotIndex", 0)), getInt(body, "count", 1));
            case "save" -> screen.testBridgeSave();
            case "cancel" -> screen.testBridgeCancel(getString(body, "reason"));
            default -> throw new GuiOperationException("VALIDATION_FAILED", "不支持的 GUI 操作：" + operation, screen.testBridgeSnapshot(false, false));
        };
    }

    private static JsonObject dispatchSingle(WebAdminSingleItemSubmitTemplateScreen screen, String operation, JsonObject body) {
        requireTarget(body, "single_item_submit", screen.testBridgeSnapshot(false, false));
        return switch (operation) {
            case "current" -> screen.testBridgeSnapshot(false, false);
            case "slots" -> screen.testBridgeSnapshot(true, false);
            case "put_item" -> screen.testBridgePutItem(getString(body, "itemId"), getInt(body, "count", 1));
            case "clear_slot" -> screen.testBridgeClearSlot();
            case "set_count" -> screen.testBridgeSetCount(getInt(body, "count", 1));
            case "save" -> screen.testBridgeSave();
            case "cancel" -> screen.testBridgeCancel(getString(body, "reason"));
            default -> throw new GuiOperationException("VALIDATION_FAILED", "不支持的 GUI 操作：" + operation, screen.testBridgeSnapshot(false, false));
        };
    }

    private static void requireTarget(JsonObject body, String actualType, JsonObject data) {
        String target = getString(body, "target");
        if (!target.isBlank() && !target.equals(actualType)) {
            throw new GuiOperationException("SCREEN_MISMATCH", "当前 GUI 类型为 " + actualType + "，与请求 target 不一致。", data);
        }
    }

    private static JsonObject currentBase(String type, Screen screen) {
        JsonObject data = new JsonObject();
        data.addProperty("open", screen != null);
        data.addProperty("type", type);
        data.addProperty("title", screen == null || screen.getTitle() == null ? "" : screen.getTitle().getString());
        return data;
    }

    private static String screenType(Screen screen) {
        if (screen == null) {
            return "none";
        }
        if (screen instanceof WebAdminContainerTemplatePreviewScreen) {
            return "container_template";
        }
        if (screen instanceof WebAdminSingleItemSubmitTemplateScreen) {
            return "single_item_submit";
        }
        return "unsupported";
    }

    private static JsonObject ok(JsonObject data) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("ok", true);
        envelope.addProperty("code", "OK");
        envelope.addProperty("message", "OK");
        envelope.add("data", data == null ? new JsonObject() : data);
        return envelope;
    }

    private static JsonObject failed(String code, String message, JsonObject data) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("ok", false);
        envelope.addProperty("code", code == null || code.isBlank() ? "SCREEN_OPERATION_FAILED" : code);
        envelope.addProperty("message", message == null || message.isBlank() ? "GUI 操作失败。" : message);
        envelope.add("data", data == null ? new JsonObject() : data);
        return envelope;
    }

    private static void send(WebAdminTestBridgeGuiS2CPayload payload, JsonObject envelope) {
        ClientPlayNetworking.send(new WebAdminTestBridgeGuiC2SPayload(
                payload.requestId(),
                payload.nonce(),
                payload.operation(),
                envelope.toString()
        ));
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

    private static final class GuiOperationException extends RuntimeException {
        private final String code;
        private final JsonObject data;

        private GuiOperationException(String code, String message, JsonObject data) {
            super(message);
            this.code = code;
            this.data = data == null ? new JsonObject() : data;
        }
    }

    private record PendingScreenshot(WebAdminTestBridgeGuiS2CPayload payload, Path path, long expiresAtMillis) {
    }
}
