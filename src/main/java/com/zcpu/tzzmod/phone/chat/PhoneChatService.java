package com.zcpu.tzzmod.phone.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zcpu.tzzmod.network.PhoneChatS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhoneChatService {
    private static final Map<String, List<ChatMessage>> DIRECT_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<String, ChatGroup> GROUPS = new ConcurrentHashMap<>();

    private PhoneChatService() {
    }

    public static JsonObject buildBootstrap(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject result = new JsonObject();
        JsonArray contacts = new JsonArray();
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (online.getUuid().equals(player.getUuid())) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", online.getUuidAsString());
            entry.addProperty("name", online.getName().getString());
            contacts.add(entry);
        }
        result.add("contacts", contacts);

        JsonArray groups = new JsonArray();
        for (ChatGroup group : GROUPS.values()) {
            if (!group.members.contains(player.getUuidAsString())) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", group.id);
            entry.addProperty("name", group.name);
            entry.addProperty("ownerUuid", group.ownerUuid);
            groups.add(entry);
        }
        result.add("groups", groups);

        result.addProperty("selfUuid", player.getUuidAsString());
        result.addProperty("isOp", player.isCreativeLevelTwoOp());
        return result;
    }

    public static JsonObject buildHistory(MinecraftServer server, ServerPlayerEntity player, String type, String targetId) {
        JsonObject result = new JsonObject();
        result.addProperty("type", type);
        result.addProperty("targetId", targetId);
        JsonArray messages = new JsonArray();

        if ("direct".equals(type)) {
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(targetId));
            String title = target == null ? targetId : target.getName().getString();
            result.addProperty("title", title);
            List<ChatMessage> history = DIRECT_MESSAGES.getOrDefault(directKey(player.getUuidAsString(), targetId), List.of());
            for (ChatMessage message : history) {
                messages.add(message.toJson());
            }
        } else {
            ChatGroup group = GROUPS.get(targetId);
            if (group == null || !group.members.contains(player.getUuidAsString())) {
                return null;
            }
            result.addProperty("title", group.name);
            for (ChatMessage message : group.messages) {
                messages.add(message.toJson());
            }
        }

        result.add("messages", messages);
        return result;
    }

    public static JsonObject createGroup(ServerPlayerEntity owner, String name, List<String> members) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            cleanName = "Group";
        }

        ChatGroup group = new ChatGroup();
        group.id = UUID.randomUUID().toString();
        group.name = cleanName;
        group.ownerUuid = owner.getUuidAsString();
        group.members.add(owner.getUuidAsString());

        for (String member : members) {
            if (isValidUuid(member)) {
                group.members.add(member);
            }
        }

        GROUPS.put(group.id, group);

        JsonObject result = new JsonObject();
        result.addProperty("id", group.id);
        result.addProperty("name", group.name);
        result.addProperty("ownerUuid", group.ownerUuid);
        JsonArray memberArray = new JsonArray();
        for (String member : group.members) {
            memberArray.add(member);
        }
        result.add("members", memberArray);
        return result;
    }

    public static boolean addMember(String groupId, String memberUuid) {
        ChatGroup group = GROUPS.get(groupId);
        if (group == null || !isValidUuid(memberUuid)) {
            return false;
        }
        group.members.add(memberUuid);
        return true;
    }

    public static JsonObject sendDirect(
            MinecraftServer server,
            ServerPlayerEntity sender,
            String targetUuid,
            String message,
            PhoneChatConfig config
    ) {
        if (!isValidUuid(targetUuid)) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage(sender.getUuidAsString(), sender.getName().getString(), System.currentTimeMillis(), message);
        String key = directKey(sender.getUuidAsString(), targetUuid);
        List<ChatMessage> history = DIRECT_MESSAGES.computeIfAbsent(key, ignored -> new ArrayList<>());
        history.add(chatMessage);
        trimHistory(history, config.maxHistoryPerConversation);

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(targetUuid));

        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", "direct");
        envelope.addProperty("targetId", targetUuid);
        envelope.addProperty("title", target == null ? targetUuid : target.getName().getString());
        envelope.add("message", chatMessage.toJson());

        return envelope;
    }

    public static JsonObject sendGroup(
            ServerPlayerEntity sender,
            String groupId,
            String message,
            PhoneChatConfig config
    ) {
        ChatGroup group = GROUPS.get(groupId);
        if (group == null || !group.members.contains(sender.getUuidAsString())) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage(sender.getUuidAsString(), sender.getName().getString(), System.currentTimeMillis(), message);
        group.messages.add(chatMessage);
        trimHistory(group.messages, config.maxHistoryPerConversation);

        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", "group");
        envelope.addProperty("targetId", group.id);
        envelope.addProperty("title", group.name);
        envelope.add("message", chatMessage.toJson());
        return envelope;
    }

    public static void deliverToParticipants(MinecraftServer server, JsonObject envelope, Collection<String> targets) {
        for (String uuidText : targets) {
            if (!isValidUuid(uuidText)) {
                continue;
            }
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(uuidText));
            if (target != null) {
                ServerPlayNetworking.send(target, new PhoneChatS2CPayload("message", envelope.toString()));
            }
        }
    }

    public static Set<String> getGroupMembers(String groupId) {
        ChatGroup group = GROUPS.get(groupId);
        if (group == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(group.members);
    }

    public static void sendResponse(ServerPlayerEntity player, String action, JsonObject body) {
        ServerPlayNetworking.send(player, new PhoneChatS2CPayload(action, body.toString()));
    }

    public static void sendError(ServerPlayerEntity player, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        sendResponse(player, "error", error);
    }

    private static void trimHistory(List<ChatMessage> history, int max) {
        if (history.size() <= max) {
            return;
        }
        int removeCount = history.size() - max;
        if (removeCount > 0) {
            history.subList(0, removeCount).clear();
        }
    }

    private static String directKey(String first, String second) {
        if (first.compareTo(second) <= 0) {
            return first + "|" + second;
        }
        return second + "|" + first;
    }

    private static boolean isValidUuid(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final class ChatGroup {
        private String id;
        private String name;
        private String ownerUuid;
        private final Set<String> members = new LinkedHashSet<>();
        private final List<ChatMessage> messages = new ArrayList<>();
    }

    public record ChatMessage(String senderUuid, String senderName, long timestamp, String content) {
        private JsonObject toJson() {
            JsonObject result = new JsonObject();
            result.addProperty("senderUuid", senderUuid);
            result.addProperty("senderName", senderName);
            result.addProperty("timestamp", timestamp);
            result.addProperty("content", content);
            return result;
        }
    }
}
