package com.zcpu.tzzmod.client.phone.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.ar.ui.app.ARChatConfirmDeleteGroupScreen;
import com.zcpu.tzzmod.client.ar.ui.app.ARChatConversationScreen;
import com.zcpu.tzzmod.client.ar.ui.app.ARChatManageMembersScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatConfirmDeleteGroupScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatConversationScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatManageMembersScreen;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.network.PhoneChatC2SPayload;
import com.zcpu.tzzmod.network.PhoneChatS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public final class PhoneChatClient {
    private static final Set<Runnable> LISTENERS = new CopyOnWriteArraySet<>();
    private static final List<ContactData> CONTACTS = new ArrayList<>();
    private static final List<GroupData> GROUPS = new ArrayList<>();
    private static final Map<String, List<ChatMessageData>> HISTORIES = new HashMap<>();
    private static final Map<String, String> TITLES = new HashMap<>();
    private static final Map<String, Integer> UNREAD_COUNTS = new HashMap<>();
    private static final Map<String, List<GroupMemberData>> GROUP_MEMBERS_MAP = new HashMap<>();

    private static boolean enabled = true;
    private static boolean isOp;
    private static String selfUuid = "";
    private static String notificationSound = "minecraft:entity.experience_orb.pickup";
    private static String activeConversationKey = "";
    private static long unreadNotificationExpireAtMs;
    private static int maxMessageLength = 25600;

    private PhoneChatClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PhoneChatS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetState());
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isOp() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean isSingleplayer = client != null && (client.isIntegratedServerRunning() || client.getServer() != null);
            return isOp || isSingleplayer;
        } catch (Throwable ignored) {
            return isOp;
        }
    }

    public static int getMaxMessageLength() {
        return maxMessageLength;
    }

    public static List<ContactData> getContacts() {
        return List.copyOf(CONTACTS);
    }

    public static List<GroupData> getGroups() {
        return List.copyOf(GROUPS);
    }

    public static List<ChatMessageData> getMessages(String type, String targetId) {
        return List.copyOf(HISTORIES.getOrDefault(historyKey(type, targetId), List.of()));
    }

    public static String getTitle(String type, String targetId) {
        return TITLES.getOrDefault(historyKey(type, targetId), targetId);
    }

    public static int getUnreadCount(String type, String targetId) {
        return UNREAD_COUNTS.getOrDefault(historyKey(type, targetId), 0);
    }

    public static List<UnreadEntry> getUnreadEntries() {
        List<UnreadEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : UNREAD_COUNTS.entrySet()) {
            int count = entry.getValue();
            if (count <= 0) {
                continue;
            }
            String[] parts = entry.getKey().split(":", 2);
            String type = parts.length > 0 ? parts[0] : "";
            String targetId = parts.length > 1 ? parts[1] : "";
            entries.add(new UnreadEntry(type, targetId, resolveConversationTitle(type, targetId), count));
        }
        entries.sort(Comparator.comparing(UnreadEntry::title, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    public static long getUnreadNotificationExpireAtMs() {
        return unreadNotificationExpireAtMs;
    }

    public static int getTotalUnreadCount() {
        int total = 0;
        for (Integer value : UNREAD_COUNTS.values()) {
            if (value != null && value > 0) {
                total += value;
            }
        }
        return total;
    }

    public static void setActiveConversation(String type, String targetId) {
        activeConversationKey = historyKey(type, targetId);
        if (UNREAD_COUNTS.remove(activeConversationKey) != null) {
            notifyListeners();
        }
    }

    public static void clearActiveConversation(String type, String targetId) {
        String key = historyKey(type, targetId);
        if (key.equals(activeConversationKey)) {
            activeConversationKey = "";
        }
    }

    public static String resolveUuidByNameOrUuid(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String trimmed = token.trim();
        for (ContactData contact : CONTACTS) {
            if (contact.name.equalsIgnoreCase(trimmed)) {
                return contact.uuid;
            }
        }
        return trimmed;
    }

    public static void requestBootstrap() {
        send("bootstrap", new JsonObject());
    }

    public static void requestWhoAmI() {
        send("whoami", new JsonObject());
    }

    public static void requestHistory(String type, String targetId) {
        JsonObject body = new JsonObject();
        body.addProperty("type", type);
        body.addProperty("targetId", targetId);
        send("open", body);
    }

    public static void sendDirect(String targetUuid, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("targetUuid", targetUuid);
        body.addProperty("content", content);
        send("send_direct", body);
    }

    public static void sendGroup(String groupId, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        body.addProperty("content", content);
        send("send_group", body);
    }

    public static void sendDirectImage(String targetUuid, Path photoPath,
                                       Consumer<Float> progressCallback,
                                       Consumer<Boolean> completeCallback) {
        ChatImageClient.uploadImageMessage("direct", targetUuid, photoPath, progressCallback, completeCallback);
    }

    public static void sendGroupImage(String groupId, Path photoPath,
                                      Consumer<Float> progressCallback,
                                      Consumer<Boolean> completeCallback) {
        ChatImageClient.uploadImageMessage("group", groupId, photoPath, progressCallback, completeCallback);
    }

    public static void createGroup(String name, List<String> members) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        JsonArray array = new JsonArray();
        for (String member : members) {
            if (!member.isBlank()) {
                array.add(member.trim());
            }
        }
        body.add("members", array);
        send("create_group", body);
    }

    public static void addGroupMember(String groupId, String memberUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        body.addProperty("memberUuid", memberUuid);
        send("add_member", body);
    }

    public static void requestGroupMembers(String groupId) {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        send("request_group_members", body);
    }

    public static void removeGroupMember(String groupId, String memberUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        body.addProperty("memberUuid", memberUuid);
        send("remove_member", body);
    }

    public static void deleteGroup(String groupId) {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId);
        send("delete_group", body);
    }

    public static List<GroupMemberData> getGroupMembersList(String groupId) {
        return List.copyOf(GROUP_MEMBERS_MAP.getOrDefault(groupId, List.of()));
    }

    public static String getSelfUuid() {
        return selfUuid;
    }

    private static void send(String action, JsonObject body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new PhoneChatC2SPayload(action, body.toString()));
    }

    private static void handlePayload(MinecraftClient client, PhoneChatS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "app_state" -> applyAppState(body);
            case "bootstrap" -> applyBootstrap(body);
            case "history" -> applyHistory(body);
            case "message" -> applyIncomingMessage(client, body);
            case "group_removed" -> applyGroupRemoved(client, body);
            case "notice" -> showNotice(client, body);
            case "error" -> showError(client, body);
            case "group_members" -> applyGroupMembers(body);
            case "whoami" -> isOp = getBoolean(body, "isOp", isOp);
            case "image_upload_ack", "image_upload_complete", "image_download_data", "image_download_complete" ->
                    ChatImageClient.handlePayload(payload.action(), body);
            default -> {
            }
        }

        notifyListeners();
    }

    private static void applyGroupMembers(JsonObject body) {
        String groupId = getString(body, "groupId");
        List<GroupMemberData> list = new ArrayList<>();
        if (body.has("entries") && body.get("entries").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("entries")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                list.add(new GroupMemberData(
                        getString(object, "uuid"),
                        getString(object, "name"),
                        getBoolean(object, "isMember", false)
                ));
            }
        }
        GROUP_MEMBERS_MAP.put(groupId, list);
    }

    private static void applyAppState(JsonObject body) {
        enabled = getBoolean(body, "enabled", true);
        String sound = getString(body, "notificationSound");
        if (!sound.isBlank()) {
            notificationSound = sound;
        }
        try {
            int serverMax = body.has("maxMessageLength") ? body.get("maxMessageLength").getAsInt() : maxMessageLength;
            if (serverMax >= 16 && serverMax <= 25600) {
                maxMessageLength = serverMax;
            }
        } catch (Exception ignored) {
        }
    }

    private static void applyBootstrap(JsonObject body) {
        CONTACTS.clear();
        GROUPS.clear();

        if (body.has("contacts") && body.get("contacts").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("contacts")) {
                JsonObject object = element.getAsJsonObject();
                CONTACTS.add(new ContactData(getString(object, "uuid"), getString(object, "name")));
            }
        }

        if (body.has("groups") && body.get("groups").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("groups")) {
                JsonObject object = element.getAsJsonObject();
                GROUPS.add(new GroupData(getString(object, "id"), getString(object, "name"), getString(object, "ownerUuid")));
            }
        }

        CONTACTS.sort(Comparator.comparing(ContactData::name, String.CASE_INSENSITIVE_ORDER));
        GROUPS.sort(Comparator.comparing(GroupData::name, String.CASE_INSENSITIVE_ORDER));
        pruneStaleGroupConversationState();

        selfUuid = getString(body, "selfUuid");
        isOp = getBoolean(body, "isOp", false);
        try {
            if (body.has("apps") && body.get("apps").isJsonObject()) {
                com.zcpu.tzzmod.client.phone.PhoneAppsClient.apply(body.getAsJsonObject("apps"));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void pruneStaleGroupConversationState() {
        Set<String> visibleGroupIds = new HashSet<>();
        for (GroupData group : GROUPS) {
            visibleGroupIds.add(group.id());
        }

        pruneGroupStateMap(HISTORIES.keySet(), visibleGroupIds, HISTORIES::remove);
        pruneGroupStateMap(TITLES.keySet(), visibleGroupIds, TITLES::remove);
        pruneGroupStateMap(UNREAD_COUNTS.keySet(), visibleGroupIds, UNREAD_COUNTS::remove);
        GROUP_MEMBERS_MAP.keySet().removeIf(groupId -> !visibleGroupIds.contains(groupId));

        if (activeConversationKey.startsWith("group:")) {
            String activeGroupId = activeConversationKey.substring("group:".length());
            if (!visibleGroupIds.contains(activeGroupId)) {
                activeConversationKey = "";
            }
        }
    }

    private static void pruneGroupStateMap(Set<String> keys, Set<String> visibleGroupIds, java.util.function.Consumer<String> remover) {
        List<String> staleKeys = new ArrayList<>();
        for (String key : keys) {
            if (!key.startsWith("group:")) {
                continue;
            }
            String groupId = key.substring("group:".length());
            if (!visibleGroupIds.contains(groupId)) {
                staleKeys.add(key);
            }
        }
        for (String staleKey : staleKeys) {
            remover.accept(staleKey);
        }
    }

    private static void removeGroupConversationState(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return;
        }
        String key = historyKey("group", groupId);
        HISTORIES.remove(key);
        TITLES.remove(key);
        UNREAD_COUNTS.remove(key);
        GROUP_MEMBERS_MAP.remove(groupId);
        GROUPS.removeIf(group -> group.id().equals(groupId));
        if (key.equals(activeConversationKey)) {
            activeConversationKey = "";
        }
    }

    private static void applyGroupRemoved(MinecraftClient client, JsonObject body) {
        String groupId = getString(body, "groupId");
        removeGroupConversationState(groupId);
        navigateAwayFromRemovedGroup(client, groupId);
        showNotice(client, body);
    }

    private static void applyHistory(JsonObject body) {
        String type = getString(body, "type");
        String targetId = getString(body, "targetId");
        String key = historyKey(type, targetId);

        List<ChatMessageData> messages = new ArrayList<>();
        if (body.has("messages") && body.get("messages").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("messages")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                ChatMessageData message = parseMessage(element.getAsJsonObject());
                messages.add(message);
                if (message.isImage()) {
                    ChatImageClient.requestThumbnailIfNeeded(message);
                }
            }
        }

        HISTORIES.put(key, messages);
        TITLES.put(key, getString(body, "title"));
        if (key.equals(activeConversationKey)) {
            UNREAD_COUNTS.remove(key);
        }
    }

    private static void applyIncomingMessage(MinecraftClient client, JsonObject body) {
        String type = getString(body, "type");
        String targetId = getString(body, "targetId");
        JsonObject message = body.has("message") && body.get("message").isJsonObject()
                ? body.getAsJsonObject("message")
                : new JsonObject();

        ChatMessageData chatMessage = parseMessage(message);

        if ("direct".equals(type)
                && !chatMessage.senderUuid.equals(selfUuid)
                && targetId.equals(selfUuid)) {
            targetId = chatMessage.senderUuid;
        }

        String key = historyKey(type, targetId);
        HISTORIES.computeIfAbsent(key, ignored -> new ArrayList<>()).add(chatMessage);

        String title = getString(body, "title");
        if ("direct".equals(type) && !chatMessage.senderUuid.equals(selfUuid)) {
            title = chatMessage.senderName;
        }
        TITLES.put(key, title);

        if (chatMessage.isImage()) {
            ChatImageClient.requestThumbnailIfNeeded(chatMessage);
        }

        if (chatMessage.senderUuid.equals(selfUuid)) {
            return;
        }
        if (key.equals(activeConversationKey)) {
            UNREAD_COUNTS.remove(key);
            return;
        }

        UNREAD_COUNTS.merge(
                key,
                1,
                (left, right) -> Integer.valueOf(
                        NullSafety.requireNonNull(left).intValue() + NullSafety.requireNonNull(right).intValue()
                )
        );
        unreadNotificationExpireAtMs = System.currentTimeMillis() + 15_000L;
        if (PhoneSettingsClient.isAlertModeEnabled()) {
            AlertSubtitleOverlay.enqueue(Text.translatable("phone.tzz_mod.alert.chat_subtitle"));
        } else {
            playNotification(client);
        }
    }

    private static void playNotification(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        SoundEvent event = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        Identifier id = Identifier.tryParse(notificationSound);
        if (id != null && Registries.SOUND_EVENT.containsId(id)) {
            event = Registries.SOUND_EVENT.get(id);
        }
        var player = client.player;
        if (player != null) {
            player.playSound(event, 0.8F, 1.2F);
        }
    }

    private static void showError(MinecraftClient client, JsonObject body) {
        ChatImageClient.handleError(body);
        String message = NullSafety.requireNonNull(getString(body, "message"));
        if (message != null && message.contains("Unknown chat action: whoami")) {
            return;
        }
        if (message != null && message.contains("Unknown chat action: image_upload_start")) {
            ChatImageClient.abortUpload();
            message = "当前服务器尚未更新聊天图片协议，请更新并重启服务端模组后再发送群聊图片。";
        }
        var player = client.player;
        if (player != null && !message.isBlank()) {
            player.sendMessage(Text.literal("[Phone Chat] " + message), false);
        }
    }

    private static void navigateAwayFromRemovedGroup(MinecraftClient client, String groupId) {
        if (client == null || groupId == null || groupId.isBlank()) {
            return;
        }

        Screen currentScreen = client.currentScreen;
        Screen replacement = null;
        if (currentScreen instanceof PhoneChatConversationScreen conversationScreen && conversationScreen.referencesGroup(groupId)) {
            replacement = conversationScreen.getChatHomeScreen();
        } else if (currentScreen instanceof PhoneChatManageMembersScreen manageMembersScreen && manageMembersScreen.referencesGroup(groupId)) {
            replacement = manageMembersScreen.getChatHomeScreen();
        } else if (currentScreen instanceof PhoneChatConfirmDeleteGroupScreen confirmDeleteGroupScreen && confirmDeleteGroupScreen.referencesGroup(groupId)) {
            replacement = confirmDeleteGroupScreen.getChatHomeScreen();
        } else if (currentScreen instanceof ARChatConversationScreen conversationScreen && conversationScreen.referencesGroup(groupId)) {
            replacement = conversationScreen.getChatHomeScreen();
        } else if (currentScreen instanceof ARChatManageMembersScreen manageMembersScreen && manageMembersScreen.referencesGroup(groupId)) {
            replacement = manageMembersScreen.getChatHomeScreen();
        } else if (currentScreen instanceof ARChatConfirmDeleteGroupScreen confirmDeleteGroupScreen && confirmDeleteGroupScreen.referencesGroup(groupId)) {
            replacement = confirmDeleteGroupScreen.getChatHomeScreen();
        }

        if (replacement != null && replacement != currentScreen) {
            client.setScreen(replacement);
        }
    }

    private static void showNotice(MinecraftClient client, JsonObject body) {
        String message = NullSafety.requireNonNull(getString(body, "message"));
        var player = client.player;
        if (player != null && !message.isBlank()) {
            player.sendMessage(Text.literal("[Phone Chat] " + message), false);
        }
    }

    private static String resolveConversationTitle(String type, String targetId) {
        String key = historyKey(type, targetId);
        String title = TITLES.get(key);
        if (title != null && !title.isBlank()) {
            return title;
        }
        if ("group".equals(type)) {
            for (GroupData group : GROUPS) {
                if (group.id.equals(targetId)) {
                    return group.name;
                }
            }
            return targetId;
        }
        for (ContactData contact : CONTACTS) {
            if (contact.uuid.equals(targetId)) {
                return contact.name;
            }
        }
        return targetId;
    }

    private static ChatMessageData parseMessage(JsonObject message) {
        return new ChatMessageData(
                getString(message, "senderUuid"),
                getString(message, "senderName"),
                getLong(message, "timestamp"),
                getString(message, "content"),
                getString(message, "messageType"),
                getString(message, "imageId"),
                getInt(message, "imageWidth", 0),
                getInt(message, "imageHeight", 0)
        );
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    private static void resetState() {
        CONTACTS.clear();
        GROUPS.clear();
        HISTORIES.clear();
        TITLES.clear();
        UNREAD_COUNTS.clear();
        GROUP_MEMBERS_MAP.clear();
        enabled = true;
        isOp = false;
        selfUuid = "";
        notificationSound = "minecraft:entity.experience_orb.pickup";
        activeConversationKey = "";
        unreadNotificationExpireAtMs = 0L;
        maxMessageLength = 25600;
        ChatImageClient.reset();
        notifyListeners();
    }

    private static JsonObject parse(String body) {
        try {
            if (body == null || body.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(body).getAsJsonObject();
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

    private static long getLong(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return 0L;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception ignored) {
            return 0L;
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

    private static String historyKey(String type, String targetId) {
        return type + ":" + targetId;
    }

    public record ContactData(String uuid, String name) {
    }

    public record GroupData(String id, String name, String ownerUuid) {
    }

    public record GroupMemberData(String uuid, String name, boolean isMember) {
    }

    public record ChatMessageData(
            String senderUuid,
            String senderName,
            long timestamp,
            String content,
            String messageType,
            String imageId,
            int imageWidth,
            int imageHeight
    ) {
        public boolean isImage() {
            return "image".equals(messageType) && imageId != null && !imageId.isBlank();
        }
    }

    public record UnreadEntry(String type, String targetId, String title, int count) {
    }
}
