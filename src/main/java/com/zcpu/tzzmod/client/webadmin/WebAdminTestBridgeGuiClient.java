package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiC2SPayload;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class WebAdminTestBridgeGuiClient {
    private WebAdminTestBridgeGuiClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WebAdminTestBridgeGuiS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
    }

    private static void handlePayload(MinecraftClient client, WebAdminTestBridgeGuiS2CPayload payload) {
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
}
