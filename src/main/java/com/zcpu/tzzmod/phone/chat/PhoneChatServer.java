package com.zcpu.tzzmod.phone.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.PhoneChatC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class PhoneChatServer {
    private PhoneChatServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendAppState(server, handler.getPlayer()))
        );

        ServerPlayNetworking.registerGlobalReceiver(PhoneChatC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
    }

    private static void sendAppState(MinecraftServer server, ServerPlayerEntity player) {
        PhoneChatConfig config = PhoneChatConfig.get(server);
        JsonObject body = new JsonObject();
        body.addProperty("enabled", config.enabled);
        body.addProperty("notificationSound", config.notificationSound);
        PhoneChatService.sendResponse(player, "app_state", body);
    }

    private static void handlePayload(MinecraftServer server, ServerPlayerEntity player, PhoneChatC2SPayload payload) {
        PhoneChatConfig config = PhoneChatConfig.get(server);
        if (!config.enabled) {
            PhoneChatService.sendError(player, "Chat app is disabled by config.");
            return;
        }

        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "bootstrap" -> handleBootstrap(server, player);
            case "open" -> handleOpenConversation(server, player, body);
            case "send_direct" -> handleSendDirect(server, player, body, config);
            case "send_group" -> handleSendGroup(server, player, body, config);
            case "create_group" -> handleCreateGroup(server, player, body);
            case "add_member" -> handleAddMember(server, player, body);
            default -> PhoneChatService.sendError(player, "Unknown chat action: " + payload.action());
        }
    }

    private static void handleBootstrap(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject data = PhoneChatService.buildBootstrap(server, player);
        PhoneChatService.sendResponse(player, "bootstrap", data);
    }

    private static void handleOpenConversation(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String type = getString(body, "type");
        String targetId = getString(body, "targetId");
        if (type.isEmpty() || targetId.isEmpty()) {
            PhoneChatService.sendError(player, "Missing chat target.");
            return;
        }

        JsonObject history = PhoneChatService.buildHistory(server, player, type, targetId);
        if (history == null) {
            PhoneChatService.sendError(player, "No permission to view this conversation.");
            return;
        }
        PhoneChatService.sendResponse(player, "history", history);
    }

    private static void handleSendDirect(MinecraftServer server, ServerPlayerEntity player, JsonObject body, PhoneChatConfig config) {
        String targetUuid = getString(body, "targetUuid");
        String content = sanitizeMessage(getString(body, "content"), config.maxMessageLength);
        if (targetUuid.isEmpty() || content.isEmpty()) {
            PhoneChatService.sendError(player, "Message is empty or target is missing.");
            return;
        }

        JsonObject envelope = PhoneChatService.sendDirect(server, player, targetUuid, content, config);
        if (envelope == null) {
            PhoneChatService.sendError(player, "Target player is invalid.");
            return;
        }

        // Sender keeps the current target as conversation key.
        PhoneChatService.deliverToParticipants(server, envelope, List.of(player.getUuidAsString()));

        JsonObject receiverEnvelope = envelope.deepCopy();
        receiverEnvelope.addProperty("targetId", player.getUuidAsString());
        receiverEnvelope.addProperty("title", player.getName().getString());

        // Receiver should index this direct chat by sender UUID/name.
        PhoneChatService.deliverToParticipants(server, receiverEnvelope, List.of(targetUuid));
    }

    private static void handleSendGroup(MinecraftServer server, ServerPlayerEntity player, JsonObject body, PhoneChatConfig config) {
        String groupId = getString(body, "groupId");
        String content = sanitizeMessage(getString(body, "content"), config.maxMessageLength);
        if (groupId.isEmpty() || content.isEmpty()) {
            PhoneChatService.sendError(player, "Message is empty or group is missing.");
            return;
        }

        JsonObject envelope = PhoneChatService.sendGroup(player, groupId, content, config);
        if (envelope == null) {
            PhoneChatService.sendError(player, "Cannot send message to this group.");
            return;
        }

        PhoneChatService.deliverToParticipants(server, envelope, PhoneChatService.getGroupMembers(groupId));
    }

    private static void handleCreateGroup(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        if (!player.isCreativeLevelTwoOp()) {
            PhoneChatService.sendError(player, "Only OP can create group chats.");
            return;
        }

        String name = getString(body, "name");
        List<String> members = new ArrayList<>();
        if (body.has("members") && body.get("members").isJsonArray()) {
            JsonArray memberArray = body.getAsJsonArray("members");
            for (JsonElement element : memberArray) {
                members.add(element.getAsString());
            }
        }

        PhoneChatService.createGroup(player, name, members);
        handleBootstrap(server, player);
    }

    private static void handleAddMember(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        if (!player.isCreativeLevelTwoOp()) {
            PhoneChatService.sendError(player, "Only OP can add members into groups.");
            return;
        }

        String groupId = getString(body, "groupId");
        String memberUuid = getString(body, "memberUuid");
        boolean success = PhoneChatService.addMember(groupId, memberUuid);
        if (!success) {
            PhoneChatService.sendError(player, "Failed to add the member to group.");
            return;
        }

        handleBootstrap(server, player);
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

    private static String sanitizeMessage(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }
        if (raw.isBlank()) {
            return "";
        }
        if (raw.length() > maxLength) {
            return "";
        }
        return raw;
    }
}
