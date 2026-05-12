package com.zcpu.tzzmod.webadmin.testbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiC2SPayload;
import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiS2CPayload;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminTestBridgeClientGuiBridge {
    private static final ConcurrentHashMap<String, PendingRequest> PENDING = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long DEFAULT_TIMEOUT_MILLIS = Duration.ofSeconds(8L).toMillis();

    private WebAdminTestBridgeClientGuiBridge() {
    }

    public static Result request(MinecraftServer server, String playerName, String operation, JsonObject body) {
        return request(server, playerName, operation, body, DEFAULT_TIMEOUT_MILLIS);
    }

    public static Result request(MinecraftServer server, String playerName, String operation, JsonObject body, long timeoutMillis) {
        if (server == null) {
            return Result.failed("TESTBRIDGE_NOT_READY", "Minecraft server/world 尚未就绪。");
        }
        String cleanOperation = safe(operation);
        if (!isAllowedOperation(cleanOperation)) {
            return Result.failed("VALIDATION_FAILED", "不支持的 GUI TestBridge 操作：" + cleanOperation);
        }
        CompletableFuture<Result> future = new CompletableFuture<>();
        server.execute(() -> {
            ServerPlayerEntity player = resolvePlayer(server, playerName);
            if (player == null) {
                future.complete(Result.failed("NOT_FOUND", "在线玩家不存在：" + safe(playerName)));
                return;
            }
            String requestId = UUID.randomUUID().toString();
            String nonce = nonce();
            PendingRequest pending = new PendingRequest(
                    player.getUuidAsString(),
                    cleanOperation,
                    nonce,
                    System.currentTimeMillis() + timeoutMillis,
                    future
            );
            PENDING.put(requestId, pending);
            try {
                ServerPlayNetworking.send(player, new WebAdminTestBridgeGuiS2CPayload(
                        requestId,
                        nonce,
                        cleanOperation,
                        body == null ? "{}" : body.toString()
                ));
            } catch (Exception exception) {
                PENDING.remove(requestId);
                future.complete(Result.failed("CLIENT_TESTBRIDGE_UNAVAILABLE", "目标客户端无法接收 GUI TestBridge payload。"));
            }
        });
        try {
            return future.get(Math.max(1000L, timeoutMillis + 500L), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.complete(Result.failed("CLIENT_TIMEOUT", "等待目标客户端 GUI TestBridge 响应超时。"));
            cleanupExpired();
            return Result.failed("CLIENT_TIMEOUT", "等待目标客户端 GUI TestBridge 响应超时。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failed("COMMAND_FAILED", "等待 GUI TestBridge 响应被中断。");
        } catch (Exception exception) {
            return Result.failed("COMMAND_FAILED", exception.getMessage());
        }
    }

    public static void handleClientResponse(ServerPlayerEntity player, WebAdminTestBridgeGuiC2SPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        PendingRequest pending = PENDING.remove(payload.requestId());
        if (pending == null) {
            return;
        }
        if (pending.expiresAtMillis < System.currentTimeMillis()) {
            pending.future.complete(Result.failed("SESSION_EXPIRED", "GUI TestBridge 请求已过期。"));
            return;
        }
        if (!pending.playerUuid.equals(player.getUuidAsString())
                || !pending.nonce.equals(payload.nonce())
                || !pending.operation.equals(payload.operation())) {
            pending.future.complete(Result.failed("SESSION_DENIED", "GUI TestBridge 响应校验失败。"));
            return;
        }
        pending.future.complete(parseResult(payload.bodyJson()));
    }

    private static Result parseResult(String bodyJson) {
        try {
            JsonObject body = JsonParser.parseString(bodyJson == null || bodyJson.isBlank() ? "{}" : bodyJson).getAsJsonObject();
            boolean ok = body.has("ok") && body.get("ok").getAsBoolean();
            String code = body.has("code") ? body.get("code").getAsString() : (ok ? "OK" : "COMMAND_FAILED");
            String message = body.has("message") ? body.get("message").getAsString() : "";
            JsonObject data = body.has("data") && body.get("data").isJsonObject() ? body.getAsJsonObject("data") : new JsonObject();
            return ok ? Result.ok(data) : Result.failed(code, message.isBlank() ? code : message, data);
        } catch (Exception exception) {
            return Result.failed("COMMAND_FAILED", "GUI TestBridge 响应 JSON 无效。");
        }
    }

    private static ServerPlayerEntity resolvePlayer(MinecraftServer server, String rawPlayer) {
        String value = safe(rawPlayer);
        if (value.isBlank() || server.getPlayerManager() == null) {
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

    private static boolean isAllowedOperation(String operation) {
        return switch (operation) {
            case "current", "slots", "put_item", "clear_slot", "set_count", "save", "cancel" -> true;
            default -> false;
        };
    }

    private static String nonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value & 0xFF));
        }
        return builder.toString();
    }

    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis < now);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record PendingRequest(
            String playerUuid,
            String operation,
            String nonce,
            long expiresAtMillis,
            CompletableFuture<Result> future
    ) {
    }

    public record Result(boolean ok, String code, String message, JsonObject data) {
        public static Result ok(JsonObject data) {
            return new Result(true, "OK", "OK", data == null ? new JsonObject() : data);
        }

        public static Result failed(String code, String message) {
            return failed(code, message, new JsonObject());
        }

        public static Result failed(String code, String message, JsonObject data) {
            return new Result(false, safe(code).isBlank() ? "COMMAND_FAILED" : safe(code), safe(message), data == null ? new JsonObject() : data);
        }
    }
}
