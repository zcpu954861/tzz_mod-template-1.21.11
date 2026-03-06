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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhoneChatServer {
    // track last call timestamp per player to enforce cooldown
    private static final Map<UUID, Long> lastCallAt = new ConcurrentHashMap<>();

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
        body.addProperty("maxMessageLength", config.maxMessageLength);
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
            case "remove_member" -> handleRemoveMember(server, player, body);
            case "request_group_members" -> handleRequestGroupMembers(server, player, body);
            case "delete_group" -> handleDeleteGroup(server, player, body);
            case "call_admin" -> handleCallAdmin(server, player);
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

        JsonObject created = PhoneChatService.createGroup(player, name, members);
        String groupName = getString(created, "name");
        if (created.has("members") && created.get("members").isJsonArray()) {
            for (JsonElement element : created.getAsJsonArray("members")) {
                String memberUuid = element.getAsString();
                if (!memberUuid.equals(player.getUuidAsString())) {
                    sendNotice(server, memberUuid, "你已被加入群组: " + groupName);
                }
            }
        }
        refreshAllBootstraps(server);
    }

    private static void handleAddMember(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String groupId = getString(body, "groupId");
        String memberUuid = getString(body, "memberUuid");

        // allow group owner or server OP to manage members
        String ownerUuid = PhoneChatService.getGroupOwner(groupId);
        boolean isOwner = ownerUuid != null && !ownerUuid.isBlank() && ownerUuid.equals(player.getUuidAsString());
        if (!isOwner && !player.isCreativeLevelTwoOp()) {
            PhoneChatService.sendError(player, "Only group owner or OP can add members into groups.");
            return;
        }

        boolean success = PhoneChatService.addMember(groupId, memberUuid);
        if (!success) {
            PhoneChatService.sendError(player, "Failed to add the member to group.");
            return;
        }

        String groupName = PhoneChatService.getGroupName(groupId);
        sendNotice(server, memberUuid, "你已被加入群组: " + (groupName.isBlank() ? groupId : groupName));
        refreshAllBootstraps(server);
    }

    private static void handleRemoveMember(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String groupId = getString(body, "groupId");
        String memberUuid = getString(body, "memberUuid");

        String ownerUuid = PhoneChatService.getGroupOwner(groupId);
        boolean isOwner = ownerUuid != null && !ownerUuid.isBlank() && ownerUuid.equals(player.getUuidAsString());
        if (!isOwner && !player.isCreativeLevelTwoOp()) {
            PhoneChatService.sendError(player, "Only group owner or OP can remove members from groups.");
            return;
        }

        // Prevent removing the owner via this path
        if (ownerUuid != null && ownerUuid.equals(memberUuid)) {
            PhoneChatService.sendError(player, "Cannot remove the group owner.");
            return;
        }

        boolean success = PhoneChatService.removeMember(groupId, memberUuid);
        if (!success) {
            PhoneChatService.sendError(player, "Failed to remove the member from group.");
            return;
        }

        String groupName = PhoneChatService.getGroupName(groupId);
        sendGroupRemoved(server, memberUuid, groupId, "你已被移出群组: " + (groupName.isBlank() ? groupId : groupName));
        refreshAllBootstraps(server);
    }

    // New handler for the call admin action
    private static void handleCallAdmin(MinecraftServer server, ServerPlayerEntity player) {
        // enforce 5-second cooldown per player
        UUID id = player.getUuid();
        long now = System.currentTimeMillis();
        Long previous = lastCallAt.get(id);
        if (previous != null && now - previous < 5000L) {
            PhoneChatService.sendError(player, "请等待冷却再呼叫管理员。");
            return;
        }
        lastCallAt.put(id, now);

        // Build the chat message for OPs: "<playerId>(黄色)在(浅灰色)<x,y,z>(青色#00ffff)呼叫管理员!(黄色)"
        String playerName = player.getName().getString();
        int x = (int)Math.floor(player.getX());
        int y = (int)Math.floor(player.getY());
        int z = (int)Math.floor(player.getZ());
        Text partName = Text.literal(playerName).setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW)));
        Text partAt = Text.literal(" 在 ").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)));
        Text partCoords = Text.literal("<" + x + "," + y + "," + z + ">").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FFFF)));
        Text partEnd = Text.literal(" 呼叫管理员!").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW)));

        Text finalMessage = Text.empty().append(partName).append(partAt).append(partCoords).append(partEnd);

        // Play bell sound and send the message to all online OP players
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (online.isCreativeLevelTwoOp()) {
                online.sendMessage(finalMessage, false);
                online.playSound(SoundEvents.BLOCK_BELL_USE, 1.0F, 1.0F);
            }
        }

        // Feedback to caller: green "已成功呼叫" and experience pick up sound
        player.sendMessage(Text.literal("已成功呼叫").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GREEN))), false);
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private static void handleRequestGroupMembers(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String groupId = getString(body, "groupId");
        if (groupId.isBlank()) {
            PhoneChatService.sendError(player, "Missing groupId");
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("groupId", groupId);
        JsonArray entries = new JsonArray();

        // Build list from online players; mark isMember if group's member set contains uuid
        Set<String> members = PhoneChatService.getGroupMembers(groupId);
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            JsonObject e = new JsonObject();
            e.addProperty("uuid", online.getUuidAsString());
            e.addProperty("name", online.getName().getString());
            e.addProperty("isMember", members.contains(online.getUuidAsString()));
            entries.add(e);
        }

        resp.add("entries", entries);
        PhoneChatService.sendResponse(player, "group_members", resp);
    }

    private static void handleDeleteGroup(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String groupId = getString(body, "groupId");
        if (groupId.isBlank()) {
            PhoneChatService.sendError(player, "Missing groupId");
            return;
        }

        String ownerUuid = PhoneChatService.getGroupOwner(groupId);
        boolean isOwner = ownerUuid != null && !ownerUuid.isBlank() && ownerUuid.equals(player.getUuidAsString());
        if (!isOwner && !player.isCreativeLevelTwoOp()) {
            PhoneChatService.sendError(player, "Only group owner or OP can delete groups.");
            return;
        }

        // capture members before deletion
        String groupName = PhoneChatService.getGroupName(groupId);

        // perform deletion
        Set<String> deletedMembers = PhoneChatService.deleteGroup(groupId);

        String msg = "群组已被删除: " + (groupName.isBlank() ? groupId : groupName);
        for (String memberUuid : deletedMembers) {
            sendGroupRemoved(server, memberUuid, groupId, msg);
        }

        refreshAllBootstraps(server);
    }

    private static void refreshAllBootstraps(MinecraftServer server) {
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            try {
                handleBootstrap(server, online);
            } catch (Exception ignored) {
            }
        }
    }

    private static void sendNotice(MinecraftServer server, String targetUuid, String message) {
        if (targetUuid == null || targetUuid.isBlank() || message == null || message.isBlank()) {
            return;
        }
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(targetUuid));
        if (target == null) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        PhoneChatService.sendResponse(target, "notice", body);
    }

    private static void sendGroupRemoved(MinecraftServer server, String targetUuid, String groupId, String message) {
        if (targetUuid == null || targetUuid.isBlank() || groupId == null || groupId.isBlank()) {
            return;
        }
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(targetUuid));
        if (target == null) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        body.addProperty("message", message);
        PhoneChatService.sendResponse(target, "group_removed", body);
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
