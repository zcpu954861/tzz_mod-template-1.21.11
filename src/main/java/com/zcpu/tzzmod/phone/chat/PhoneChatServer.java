package com.zcpu.tzzmod.phone.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.PhoneChatC2SPayload;
import com.zcpu.tzzmod.util.SharedImageTransferBudget;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhoneChatServer {
    private static final Map<UUID, Long> lastCallAt = new ConcurrentHashMap<>();
    private static final Map<UUID, ImageUploadSession> imageUploads = new ConcurrentHashMap<>();

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
        body.addProperty("imageUploadBandwidthMbps", config.imageUploadBandwidthMbps);
        body.addProperty("imageDownloadBandwidthMbps", config.imageDownloadBandwidthMbps);
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
            case "whoami" -> handleWhoAmI(server, player);
            case "open" -> handleOpenConversation(server, player, body);
            case "send_direct" -> handleSendDirect(server, player, body, config);
            case "send_group" -> handleSendGroup(server, player, body, config);
            case "image_upload_start" -> handleImageUploadStart(server, player, body, config);
            case "image_upload_chunk" -> handleImageUploadChunk(player, body, config);
            case "image_upload_finish" -> handleImageUploadFinish(server, player, config);
            case "image_download_start" -> handleImageDownloadStart(server, player, body, config);
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
        PhoneChatService.sendResponse(player, "bootstrap", PhoneChatService.buildBootstrap(server, player));
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

        deliverDirectEnvelope(server, player, targetUuid, envelope);
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

    private static void handleImageUploadStart(MinecraftServer server, ServerPlayerEntity player, JsonObject body, PhoneChatConfig config) {
        String conversationType = getString(body, "conversationType");
        String targetId = getString(body, "targetId");
        String imageId = getString(body, "imageId");
        int totalSize = getInt(body, "totalSize", 0);
        int imageWidth = getInt(body, "imageWidth", 0);
        int imageHeight = getInt(body, "imageHeight", 0);

        if (imageId.isBlank() || totalSize <= 0 || !isValidConversationTarget(player, conversationType, targetId)) {
            sendImageError(player, "Invalid chat image upload parameters.", imageId, false, true);
            return;
        }

        try {
            PhoneChatImageStore.ensureStorageReady(server);
            if (PhoneChatImageStore.imageExists(server, imageId)) {
                PhoneChatImageStore.resolveDownloadFile(server, imageId, true);
                if (!dispatchImageMessage(server, player, conversationType, targetId, imageId, imageWidth, imageHeight, config)) {
                    sendImageError(player, "Cannot send image to this conversation.", imageId, false, true);
                    return;
                }
                JsonObject resp = new JsonObject();
                resp.addProperty("imageId", imageId);
                resp.addProperty("skippedUpload", true);
                PhoneChatService.sendResponse(player, "image_upload_complete", resp);
                return;
            }
        } catch (Exception exception) {
            sendImageError(player, "Server failed to prepare chat image storage.", imageId, false, true);
            return;
        }

        SharedImageTransferBudget.TransferLease lease = SharedImageTransferBudget.acquireUpload();
        ImageUploadSession previous = imageUploads.put(player.getUuid(), new ImageUploadSession(
                conversationType,
                targetId,
                imageId,
                totalSize,
                imageWidth,
                imageHeight,
                lease
        ));
        if (previous != null) {
            previous.close();
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("imageId", imageId);
        resp.addProperty("chunkSize", currentUploadChunkSize(config.imageUploadBandwidthMbps));
        PhoneChatService.sendResponse(player, "image_upload_ack", resp);
    }

    private static void handleImageUploadChunk(ServerPlayerEntity player, JsonObject body, PhoneChatConfig config) {
        ImageUploadSession session = imageUploads.get(player.getUuid());
        if (session == null) {
            sendImageError(player, "No active chat image upload session.", "", false, true);
            return;
        }

        String encoded = getString(body, "data");
        if (encoded.isBlank()) {
            sendImageError(player, "Empty chat image chunk.", session.imageId, false, true);
            return;
        }

        byte[] chunk = Base64.getDecoder().decode(encoded);
        if (session.receivedBytes + chunk.length > session.totalSize) {
            imageUploads.remove(player.getUuid());
            session.close();
            sendImageError(player, "Chat image upload exceeded declared size.", session.imageId, false, true);
            return;
        }

        session.appendData(chunk);
        JsonObject resp = new JsonObject();
        resp.addProperty("imageId", session.imageId);
        resp.addProperty("progress", Math.min(1.0F, (float) session.receivedBytes / session.totalSize));
        resp.addProperty("chunkSize", currentUploadChunkSize(config.imageUploadBandwidthMbps));
        PhoneChatService.sendResponse(player, "image_upload_ack", resp);
    }

    private static void handleImageUploadFinish(MinecraftServer server, ServerPlayerEntity player, PhoneChatConfig config) {
        ImageUploadSession session = imageUploads.remove(player.getUuid());
        if (session == null) {
            sendImageError(player, "No active chat image upload session.", "", false, true);
            return;
        }

        try {
            if (session.receivedBytes != session.totalSize) {
                sendImageError(player, "Chat image upload is incomplete.", session.imageId, false, true);
                return;
            }

            PhoneChatImageStore.StoredChatImage storedImage = PhoneChatImageStore.saveImage(server, session.imageId, session.data.toByteArray());
            if (!dispatchImageMessage(server, player, session.conversationType, session.targetId,
                    storedImage.imageId(), storedImage.width(), storedImage.height(), config)) {
                sendImageError(player, "Cannot send image to this conversation.", session.imageId, false, true);
                return;
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("imageId", storedImage.imageId());
            PhoneChatService.sendResponse(player, "image_upload_complete", resp);
        } catch (Exception exception) {
            sendImageError(player, "Server failed to save chat image.", session.imageId, false, true);
        } finally {
            session.close();
        }
    }

    private static void handleImageDownloadStart(MinecraftServer server, ServerPlayerEntity player, JsonObject body, PhoneChatConfig config) {
        String imageId = getString(body, "imageId");
        boolean thumbnail = getBoolean(body, "thumbnail", false);
        if (imageId.isBlank()) {
            sendImageError(player, "Missing imageId.", imageId, thumbnail, false);
            return;
        }

        Path downloadFile;
        try {
            downloadFile = PhoneChatImageStore.resolveDownloadFile(server, imageId, thumbnail);
        } catch (Exception exception) {
            sendImageError(player, "Server failed to prepare chat image.", imageId, thumbnail, false);
            return;
        }

        if (!Files.exists(downloadFile)) {
            sendImageError(player, "Chat image not found.", imageId, thumbnail, false);
            return;
        }

        try {
            byte[] data = Files.readAllBytes(downloadFile);
            Thread downloadThread = new Thread(
                    () -> streamImageDownload(server, player, imageId, thumbnail, data, config.imageDownloadBandwidthMbps),
                    "TzzMod-ChatImageDownload-" + imageId.substring(0, Math.min(8, imageId.length()))
            );
            downloadThread.setDaemon(true);
            downloadThread.start();
        } catch (Exception exception) {
            sendImageError(player, "Server failed to read chat image.", imageId, thumbnail, false);
        }
    }

    private static boolean dispatchImageMessage(MinecraftServer server, ServerPlayerEntity player,
                                                String conversationType, String targetId,
                                                String imageId, int imageWidth, int imageHeight,
                                                PhoneChatConfig config) {
        if ("group".equals(conversationType)) {
            JsonObject envelope = PhoneChatService.sendGroupImage(player, targetId, imageId, imageWidth, imageHeight, config);
            if (envelope == null) {
                return false;
            }
            PhoneChatService.deliverToParticipants(server, envelope, PhoneChatService.getGroupMembers(targetId));
            return true;
        }

        if ("direct".equals(conversationType)) {
            JsonObject envelope = PhoneChatService.sendDirectImage(server, player, targetId, imageId, imageWidth, imageHeight, config);
            if (envelope == null) {
                return false;
            }
            deliverDirectEnvelope(server, player, targetId, envelope);
            return true;
        }

        return false;
    }

    private static void deliverDirectEnvelope(MinecraftServer server, ServerPlayerEntity sender, String targetUuid, JsonObject envelope) {
        PhoneChatService.deliverToParticipants(server, envelope, List.of(sender.getUuidAsString()));

        JsonObject receiverEnvelope = envelope.deepCopy();
        receiverEnvelope.addProperty("targetId", sender.getUuidAsString());
        receiverEnvelope.addProperty("title", sender.getName().getString());
        PhoneChatService.deliverToParticipants(server, receiverEnvelope, List.of(targetUuid));
    }

    private static boolean isValidConversationTarget(ServerPlayerEntity player, String conversationType, String targetId) {
        if ("group".equals(conversationType)) {
            return !targetId.isBlank() && PhoneChatService.getGroupMembers(targetId).contains(player.getUuidAsString());
        }
        if ("direct".equals(conversationType)) {
            return isValidUuid(targetId);
        }
        return false;
    }

    private static int currentUploadChunkSize(double bandwidthMbps) {
        return SharedImageTransferBudget.recommendUploadChunkSize(bandwidthMbps);
    }

    private static int currentDownloadChunkSize(double bandwidthMbps) {
        return SharedImageTransferBudget.recommendDownloadChunkSize(bandwidthMbps);
    }

    private static void streamImageDownload(MinecraftServer server, ServerPlayerEntity player,
                                            String imageId, boolean thumbnail, byte[] data,
                                            double bandwidthMbps) {
        try (SharedImageTransferBudget.TransferLease ignored = SharedImageTransferBudget.acquireDownload()) {
            int offset = 0;
            while (offset < data.length) {
                int chunkSize = currentDownloadChunkSize(bandwidthMbps);
                int end = Math.min(offset + chunkSize, data.length);
                byte[] chunk = java.util.Arrays.copyOfRange(data, offset, end);
                String encoded = Base64.getEncoder().encodeToString(chunk);

                JsonObject resp = new JsonObject();
                resp.addProperty("imageId", imageId);
                resp.addProperty("thumbnail", thumbnail);
                resp.addProperty("data", encoded);
                resp.addProperty("progress", (float) end / data.length);

                JsonObject payload = resp;
                server.execute(() -> PhoneChatService.sendResponse(player, "image_download_data", payload));
                offset = end;
                if (offset < data.length) {
                    Thread.sleep(SharedImageTransferBudget.getTransferIntervalMs());
                }
            }

            JsonObject complete = new JsonObject();
            complete.addProperty("imageId", imageId);
            complete.addProperty("thumbnail", thumbnail);
            server.execute(() -> PhoneChatService.sendResponse(player, "image_download_complete", complete));
        } catch (Exception exception) {
            server.execute(() -> sendImageError(player, "Server failed to send chat image.", imageId, thumbnail, false));
        }
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

    private static void handleCallAdmin(MinecraftServer server, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        long now = System.currentTimeMillis();
        Long previous = lastCallAt.get(id);
        if (previous != null && now - previous < 5000L) {
            PhoneChatService.sendError(player, "请等待冷却再呼叫管理员。");
            return;
        }
        lastCallAt.put(id, now);

        String playerName = player.getName().getString();
        int x = (int) Math.floor(player.getX());
        int y = (int) Math.floor(player.getY());
        int z = (int) Math.floor(player.getZ());
        Text partName = Text.literal(playerName).setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW)));
        Text partAt = Text.literal(" 在 ").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)));
        Text partCoords = Text.literal("<" + x + "," + y + "," + z + ">").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FFFF)));
        Text partEnd = Text.literal(" 呼叫管理员!").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW)));
        Text finalMessage = Text.empty().append(partName).append(partAt).append(partCoords).append(partEnd);

        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (online.isCreativeLevelTwoOp()) {
                online.sendMessage(finalMessage, false);
                online.playSound(SoundEvents.BLOCK_BELL_USE, 1.0F, 1.0F);
            }
        }

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
        Set<String> members = PhoneChatService.getGroupMembers(groupId);
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", online.getUuidAsString());
            entry.addProperty("name", online.getName().getString());
            entry.addProperty("isMember", members.contains(online.getUuidAsString()));
            entries.add(entry);
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

        String groupName = PhoneChatService.getGroupName(groupId);
        Set<String> deletedMembers = PhoneChatService.deleteGroup(groupId);
        String message = "群组已被删除: " + (groupName.isBlank() ? groupId : groupName);
        for (String memberUuid : deletedMembers) {
            sendGroupRemoved(server, memberUuid, groupId, message);
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

    private static void sendImageError(ServerPlayerEntity player, String message, String imageId, boolean thumbnail, boolean uploadError) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        error.addProperty("chatImage", true);
        error.addProperty("chatImageUpload", uploadError);
        if (!imageId.isBlank()) {
            error.addProperty("imageId", imageId);
        }
        error.addProperty("thumbnail", thumbnail);
        PhoneChatService.sendResponse(player, "error", error);
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

    private static String sanitizeMessage(String raw, int maxLength) {
        if (raw == null || raw.isBlank() || raw.length() > maxLength) {
            return "";
        }
        return raw;
    }

    private static boolean isValidUuid(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void handleWhoAmI(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject body = new JsonObject();
        body.addProperty("isOp", player.isCreativeLevelTwoOp());
        PhoneChatService.sendResponse(player, "whoami", body);
    }

    private static final class ImageUploadSession implements AutoCloseable {
        private final String conversationType;
        private final String targetId;
        private final String imageId;
        private final int totalSize;
        private final int imageWidth;
        private final int imageHeight;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private final SharedImageTransferBudget.TransferLease transferLease;
        private int receivedBytes;

        private ImageUploadSession(String conversationType, String targetId, String imageId,
                                   int totalSize, int imageWidth, int imageHeight,
                                   SharedImageTransferBudget.TransferLease transferLease) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.imageId = imageId;
            this.totalSize = totalSize;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.transferLease = transferLease;
        }

        private void appendData(byte[] chunk) {
            data.write(chunk, 0, chunk.length);
            receivedBytes += chunk.length;
        }

        @Override
        public void close() {
            transferLease.close();
        }
    }
}
