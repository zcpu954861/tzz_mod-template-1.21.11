package com.zcpu.tzzmod.note;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.NoteC2SPayload;
import com.zcpu.tzzmod.network.NoteS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NoteServer {
    private NoteServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendBootstrap(server, handler.getPlayer())));
        ServerPlayNetworking.registerGlobalReceiver(NoteC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload)));
    }

    public static void broadcastAllCleared(MinecraftServer server) {
        JsonObject body = new JsonObject();
        body.addProperty("all", true);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            send(player, "note_deleted", body);
            sendBootstrap(server, player);
        }
    }

    private static void handlePayload(MinecraftServer server, ServerPlayerEntity player, NoteC2SPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "bootstrap" -> sendBootstrap(server, player);
            case "open" -> handleOpen(server, player, body);
            case "create" -> handleCreate(server, player, body);
            case "update" -> handleUpdate(server, player, body);
            case "delete" -> handleDelete(server, player, body);
            case "request_share_targets" -> handleShareTargets(server, player, body);
            case "set_shared_users" -> handleSetSharedUsers(server, player, body);
            default -> sendError(player, "Unknown note action: " + payload.action());
        }
    }

    private static void sendBootstrap(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject body = new JsonObject();
        body.addProperty("selfUuid", player.getUuidAsString());
        body.addProperty("isOp", player.isCreativeLevelTwoOp());
        JsonArray notes = new JsonArray();
        for (NoteDataStore.NoteRecord note : NoteDataStore.getVisibleNotes(server, player.getUuidAsString(), player.isCreativeLevelTwoOp())) {
            notes.add(toSummary(note, player));
        }
        body.add("notes", notes);
        send(player, "bootstrap", body);
    }

    private static void handleOpen(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String noteId = getString(body, "noteId");
        NoteDataStore.NoteRecord note = NoteDataStore.getVisibleNote(server, noteId, player.getUuidAsString(), player.isCreativeLevelTwoOp());
        if (note == null) {
            if (!noteId.isBlank()) {
                JsonObject deleted = new JsonObject();
                deleted.addProperty("noteId", noteId);
                send(player, "note_deleted", deleted);
            }
            sendError(player, "No permission to view this note.");
            return;
        }
        send(player, "note_detail", toDetail(note, player));
    }

    private static void handleCreate(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        NoteDataStore.NoteRecord note = NoteDataStore.createNote(
                server,
                player.getUuidAsString(),
                player.getName().getString(),
                getString(body, "title"),
                getString(body, "content")
        );
        NoteDataStore.flushDirty(server);
        send(player, "note_detail", toDetail(note, player));
        broadcastNoteUpdated(server, note, player.getUuidAsString());
        refreshAllBootstraps(server);
    }

    private static void handleUpdate(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String noteId = getString(body, "noteId");
        long version = getLong(body, "version", 0L);
        NoteDataStore.UpdateResult result = NoteDataStore.updateNote(
                server,
                noteId,
                player.getUuidAsString(),
                player.isCreativeLevelTwoOp(),
                getString(body, "title"),
                getString(body, "content"),
                version
        );
        switch (result.status()) {
            case "success" -> {
                NoteDataStore.flushDirty(server);
                send(player, "note_detail", toDetail(result.note(), player));
                broadcastNoteUpdated(server, result.note(), player.getUuidAsString());
                refreshAllBootstraps(server);
            }
            case "conflict" -> {
                JsonObject resp = toDetail(result.note(), player);
                resp.addProperty("message", "Note was changed elsewhere. Refreshed latest version.");
                send(player, "note_detail", resp);
                sendError(player, "Note was changed elsewhere. Please review the latest content.");
            }
            case "denied" -> sendError(player, "Only note owner or OP can edit this note.");
            default -> sendError(player, "Note no longer exists.");
        }
    }

    private static void handleDelete(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String noteId = getString(body, "noteId");
        NoteDataStore.DeleteResult result = NoteDataStore.deleteNote(server, noteId, player.getUuidAsString(), player.isCreativeLevelTwoOp());
        switch (result.status()) {
            case "success" -> {
                NoteDataStore.flushDirty(server);
                broadcastNoteDeleted(server, result.deletedNote());
                refreshAllBootstraps(server);
            }
            case "denied" -> sendError(player, "Only note owner or OP can delete this note.");
            default -> sendError(player, "Note no longer exists.");
        }
    }

    private static void handleShareTargets(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String noteId = getString(body, "noteId");
        NoteDataStore.NoteRecord note = NoteDataStore.getManageableNote(server, noteId, player.getUuidAsString(), player.isCreativeLevelTwoOp());
        if (note == null) {
            sendError(player, "Only note owner or OP can manage sharing.");
            return;
        }

        LinkedHashMap<String, TargetInfo> targets = new LinkedHashMap<>();
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (online.getUuidAsString().equals(note.ownerUuid)) {
                continue;
            }
            targets.put(online.getUuidAsString(), new TargetInfo(online.getUuidAsString(), online.getName().getString(), true));
        }
        for (String uuid : note.sharedWith) {
            if (uuid.equals(note.ownerUuid)) {
                continue;
            }
            targets.putIfAbsent(uuid, new TargetInfo(uuid, note.sharedNames.getOrDefault(uuid, NoteDataStore.shortUuid(uuid)), false));
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("noteId", note.noteId);
        JsonArray entries = new JsonArray();
        for (TargetInfo target : targets.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", target.uuid());
            entry.addProperty("name", target.name());
            entry.addProperty("online", target.online());
            entry.addProperty("selected", note.sharedWith.contains(target.uuid()));
            entries.add(entry);
        }
        resp.add("entries", entries);
        send(player, "share_targets", resp);
    }

    private static void handleSetSharedUsers(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String noteId = getString(body, "noteId");
        Set<String> sharedWith = new LinkedHashSet<>();
        if (body.has("sharedWith") && body.get("sharedWith").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("sharedWith")) {
                String uuid = element.getAsString();
                if (NoteDataStore.isValidUuid(uuid)) {
                    sharedWith.add(uuid);
                }
            }
        }

        Map<String, String> names = new LinkedHashMap<>();
        for (String uuid : sharedWith) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
            if (online != null) {
                names.put(uuid, online.getName().getString());
            }
        }

        NoteDataStore.ShareResult result = NoteDataStore.setSharedUsers(
                server,
                noteId,
                player.getUuidAsString(),
                player.isCreativeLevelTwoOp(),
                sharedWith,
                names
        );
        switch (result.status()) {
            case "success" -> {
                NoteDataStore.flushDirty(server);
                send(player, "note_detail", toDetail(result.after(), player));
                broadcastShareChanges(server, result.before(), result.after(), player.getUuidAsString());
                refreshAllBootstraps(server);
            }
            case "denied" -> sendError(player, "Only note owner or OP can manage sharing.");
            default -> sendError(player, "Note no longer exists.");
        }
    }

    private static void broadcastNoteUpdated(MinecraftServer server, NoteDataStore.NoteRecord note, String actorUuid) {
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (!NoteDataStore.canView(note, online.getUuidAsString(), online.isCreativeLevelTwoOp())) {
                continue;
            }
            JsonObject body = toDetail(note, online);
            body.addProperty("actorUuid", actorUuid);
            send(online, "note_updated", body);
        }
    }

    private static void broadcastNoteDeleted(MinecraftServer server, NoteDataStore.NoteRecord note) {
        JsonObject body = new JsonObject();
        body.addProperty("noteId", note.noteId);
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (NoteDataStore.canView(note, online.getUuidAsString(), online.isCreativeLevelTwoOp())) {
                send(online, "note_deleted", body);
            }
        }
    }

    private static void broadcastShareChanges(MinecraftServer server, NoteDataStore.NoteRecord before,
                                              NoteDataStore.NoteRecord after, String actorUuid) {
        Set<String> beforeVisible = visibleOnlineUuids(server, before);
        Set<String> afterVisible = visibleOnlineUuids(server, after);
        for (String uuid : beforeVisible) {
            if (!afterVisible.contains(uuid)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
                if (player != null) {
                    JsonObject body = new JsonObject();
                    body.addProperty("noteId", before.noteId);
                    send(player, "note_deleted", body);
                }
            }
        }
        for (String uuid : afterVisible) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
            if (player != null) {
                JsonObject body = toDetail(after, player);
                body.addProperty("actorUuid", actorUuid);
                send(player, "note_updated", body);
            }
        }
    }

    private static Set<String> visibleOnlineUuids(MinecraftServer server, NoteDataStore.NoteRecord note) {
        Set<String> result = new LinkedHashSet<>();
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            if (NoteDataStore.canView(note, online.getUuidAsString(), online.isCreativeLevelTwoOp())) {
                result.add(online.getUuidAsString());
            }
        }
        return result;
    }

    private static void refreshAllBootstraps(MinecraftServer server) {
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            sendBootstrap(server, online);
        }
    }

    private static JsonObject toSummary(NoteDataStore.NoteRecord note, ServerPlayerEntity viewer) {
        JsonObject obj = new JsonObject();
        obj.addProperty("noteId", note.noteId);
        obj.addProperty("ownerUuid", note.ownerUuid);
        obj.addProperty("ownerName", note.ownerName);
        obj.addProperty("title", note.title);
        obj.addProperty("updatedAt", note.updatedAt);
        obj.addProperty("version", note.version);
        obj.addProperty("relation", NoteDataStore.relation(note, viewer.getUuidAsString(), viewer.isCreativeLevelTwoOp()));
        obj.addProperty("canManage", NoteDataStore.canManage(note, viewer.getUuidAsString(), viewer.isCreativeLevelTwoOp()));
        return obj;
    }

    private static JsonObject toDetail(NoteDataStore.NoteRecord note, ServerPlayerEntity viewer) {
        JsonObject obj = toSummary(note, viewer);
        obj.addProperty("content", note.content);
        obj.addProperty("createdAt", note.createdAt);
        JsonArray shared = new JsonArray();
        for (String uuid : note.sharedWith) {
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", uuid);
            entry.addProperty("name", note.sharedNames.getOrDefault(uuid, NoteDataStore.shortUuid(uuid)));
            shared.add(entry);
        }
        obj.add("sharedWith", shared);
        return obj;
    }

    private static void send(ServerPlayerEntity player, String action, JsonObject body) {
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new NoteS2CPayload(action, body.toString()));
    }

    private static void sendError(ServerPlayerEntity player, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        send(player, "error", body);
    }

    private static JsonObject parse(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(raw).getAsJsonObject();
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

    private static long getLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record TargetInfo(String uuid, String name, boolean online) {
    }
}
