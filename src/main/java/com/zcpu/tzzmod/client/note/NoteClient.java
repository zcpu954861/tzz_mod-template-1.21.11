package com.zcpu.tzzmod.client.note;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneNoteConfirmDeleteScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneNoteConfirmSaveScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneNoteDetailScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneNoteEditScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneNoteShareScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.network.NoteC2SPayload;
import com.zcpu.tzzmod.network.NoteS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class NoteClient {
    private static final Set<Runnable> LISTENERS = new CopyOnWriteArraySet<>();
    private static final List<NoteSummaryData> NOTES = new ArrayList<>();
    private static final Map<String, NoteDetailData> DETAILS = new HashMap<>();
    private static final Map<String, List<ShareTargetData>> SHARE_TARGETS = new HashMap<>();
    private static final Set<String> UNREAD_NOTE_IDS = new LinkedHashSet<>();

    private static String selfUuid = "";
    private static boolean isOp;
    private static String activeNoteId = "";
    private static long unreadNotificationExpireAtMs;

    private NoteClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NoteS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetState());
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static List<NoteSummaryData> getNotes() {
        return List.copyOf(NOTES);
    }

    public static NoteDetailData getDetail(String noteId) {
        return DETAILS.get(noteId);
    }

    public static List<ShareTargetData> getShareTargets(String noteId) {
        return List.copyOf(SHARE_TARGETS.getOrDefault(noteId, List.of()));
    }

    public static int getTotalUnreadCount() {
        return UNREAD_NOTE_IDS.size();
    }

    public static long getUnreadNotificationExpireAtMs() {
        return unreadNotificationExpireAtMs;
    }

    public static boolean isOp() {
        return isOp;
    }

    public static String getSelfUuid() {
        return selfUuid;
    }

    public static void setActiveNote(String noteId) {
        activeNoteId = noteId == null ? "" : noteId;
        if (UNREAD_NOTE_IDS.remove(activeNoteId)) {
            notifyListeners();
        }
    }

    public static void clearActiveNote(String noteId) {
        if (activeNoteId.equals(noteId)) {
            activeNoteId = "";
        }
    }

    public static void requestBootstrap() {
        send("bootstrap", new JsonObject());
    }

    public static void requestOpen(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("noteId", noteId);
        send("open", body);
    }

    public static void createNote(String title, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("title", title);
        body.addProperty("content", content);
        send("create", body);
    }

    public static void updateNote(String noteId, String title, String content, long version) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("noteId", noteId);
        body.addProperty("title", title);
        body.addProperty("content", content);
        body.addProperty("version", version);
        send("update", body);
    }

    public static void deleteNote(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("noteId", noteId);
        send("delete", body);
    }

    public static void requestShareTargets(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("noteId", noteId);
        send("request_share_targets", body);
    }

    public static void setSharedUsers(String noteId, List<String> sharedWith) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("noteId", noteId);
        JsonArray array = new JsonArray();
        for (String uuid : sharedWith == null ? List.<String>of() : sharedWith) {
            if (uuid != null && !uuid.isBlank()) {
                array.add(uuid);
            }
        }
        body.add("sharedWith", array);
        send("set_shared_users", body);
    }

    private static void send(String action, JsonObject body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new NoteC2SPayload(action, body.toString()));
    }

    private static void handlePayload(MinecraftClient client, NoteS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "bootstrap" -> applyBootstrap(body);
            case "note_detail" -> applyNoteDetail(body);
            case "note_updated" -> applyNoteUpdated(client, body);
            case "note_deleted" -> applyNoteDeleted(client, body);
            case "share_targets" -> applyShareTargets(body);
            case "notice" -> showNotice(client, body);
            case "error" -> showError(client, body);
            default -> {
            }
        }
        notifyListeners();
    }

    private static void applyBootstrap(JsonObject body) {
        selfUuid = getString(body, "selfUuid");
        isOp = getBoolean(body, "isOp", false);
        NOTES.clear();
        if (body.has("notes") && body.get("notes").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("notes")) {
                if (element.isJsonObject()) {
                    NOTES.add(parseSummary(element.getAsJsonObject()));
                }
            }
        }
        NOTES.sort(Comparator.comparingLong(NoteSummaryData::updatedAt).reversed());
    }

    private static void applyNoteDetail(JsonObject body) {
        NoteDetailData detail = parseDetail(body);
        if (!detail.noteId().isBlank()) {
            DETAILS.put(detail.noteId(), detail);
            upsertSummary(detail.toSummary());
            UNREAD_NOTE_IDS.remove(detail.noteId());
        }
    }

    private static void applyNoteUpdated(MinecraftClient client, JsonObject body) {
        NoteSummaryData summary = parseSummary(body);
        if (summary.noteId().isBlank()) {
            return;
        }
        upsertSummary(summary);
        if (body.has("content")) {
            NoteDetailData detail = parseDetail(body);
            DETAILS.put(detail.noteId(), detail);
        }
        String actorUuid = getString(body, "actorUuid");
        boolean fromSelf = !actorUuid.isBlank() && actorUuid.equals(selfUuid);
        boolean owned = summary.ownerUuid().equals(selfUuid);
        if (!fromSelf && !owned && !summary.noteId().equals(activeNoteId)) {
            UNREAD_NOTE_IDS.add(summary.noteId());
            unreadNotificationExpireAtMs = System.currentTimeMillis() + 15_000L;
            if (PhoneSettingsClient.isAlertModeEnabled()) {
                AlertSubtitleOverlay.enqueue(Text.translatable("phone.tzz_mod.alert.note_subtitle"));
            } else if (client.player != null) {
                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.25F);
            }
        }
    }

    private static void applyNoteDeleted(MinecraftClient client, JsonObject body) {
        boolean all = getBoolean(body, "all", false);
        String noteId = getString(body, "noteId");
        if (all) {
            NOTES.clear();
            DETAILS.clear();
            SHARE_TARGETS.clear();
            UNREAD_NOTE_IDS.clear();
            activeNoteId = "";
            navigateAwayFromRemovedNote(client, "");
            return;
        }
        if (noteId.isBlank()) {
            return;
        }
        NOTES.removeIf(note -> note.noteId().equals(noteId));
        DETAILS.remove(noteId);
        SHARE_TARGETS.remove(noteId);
        UNREAD_NOTE_IDS.remove(noteId);
        if (noteId.equals(activeNoteId)) {
            activeNoteId = "";
        }
        navigateAwayFromRemovedNote(client, noteId);
    }

    private static void applyShareTargets(JsonObject body) {
        String noteId = getString(body, "noteId");
        List<ShareTargetData> targets = new ArrayList<>();
        if (body.has("entries") && body.get("entries").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("entries")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                targets.add(new ShareTargetData(
                        getString(entry, "uuid"),
                        getString(entry, "name"),
                        getBoolean(entry, "online", false),
                        getBoolean(entry, "selected", false)
                ));
            }
        }
        targets.sort(Comparator.comparing(ShareTargetData::online).reversed()
                .thenComparing(ShareTargetData::name, String.CASE_INSENSITIVE_ORDER));
        SHARE_TARGETS.put(noteId, targets);
    }

    private static void upsertSummary(NoteSummaryData summary) {
        NOTES.removeIf(note -> note.noteId().equals(summary.noteId()));
        NOTES.add(summary);
        NOTES.sort(Comparator.comparingLong(NoteSummaryData::updatedAt).reversed());
    }

    private static NoteSummaryData parseSummary(JsonObject object) {
        return new NoteSummaryData(
                getString(object, "noteId"),
                getString(object, "ownerUuid"),
                getString(object, "ownerName"),
                getString(object, "title"),
                getString(object, "relation"),
                getBoolean(object, "canManage", false),
                getLong(object, "updatedAt", 0L),
                getLong(object, "version", 0L)
        );
    }

    private static NoteDetailData parseDetail(JsonObject object) {
        List<SharedUserData> shared = new ArrayList<>();
        if (object.has("sharedWith") && object.get("sharedWith").isJsonArray()) {
            for (JsonElement element : object.getAsJsonArray("sharedWith")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                shared.add(new SharedUserData(getString(entry, "uuid"), getString(entry, "name")));
            }
        }
        NoteSummaryData summary = parseSummary(object);
        return new NoteDetailData(
                summary.noteId(),
                summary.ownerUuid(),
                summary.ownerName(),
                summary.title(),
                getString(object, "content"),
                summary.relation(),
                summary.canManage(),
                getLong(object, "createdAt", 0L),
                summary.updatedAt(),
                summary.version(),
                shared
        );
    }

    private static void navigateAwayFromRemovedNote(MinecraftClient client, String noteId) {
        if (client == null) {
            return;
        }
        Screen currentScreen = client.currentScreen;
        Screen replacement = null;
        if (currentScreen instanceof PhoneNoteDetailScreen screen && (noteId.isBlank() || screen.referencesNote(noteId))) {
            replacement = screen.getNotesHomeScreen();
        } else if (currentScreen instanceof PhoneNoteEditScreen screen && (noteId.isBlank() || screen.referencesNote(noteId))) {
            replacement = screen.getNotesHomeScreen();
        } else if (currentScreen instanceof PhoneNoteShareScreen screen && (noteId.isBlank() || screen.referencesNote(noteId))) {
            replacement = screen.getNotesHomeScreen();
        } else if (currentScreen instanceof PhoneNoteConfirmDeleteScreen screen && (noteId.isBlank() || screen.referencesNote(noteId))) {
            replacement = screen.getNotesHomeScreen();
        } else if (currentScreen instanceof PhoneNoteConfirmSaveScreen screen && (noteId.isBlank() || screen.referencesNote(noteId))) {
            replacement = screen.getNotesHomeScreen();
        }
        if (replacement != null && replacement != currentScreen) {
            client.setScreen(replacement);
        }
    }

    private static void showNotice(MinecraftClient client, JsonObject body) {
        String message = getString(body, "message");
        var player = client.player;
        if (player != null && !message.isBlank()) {
            player.sendMessage(Text.literal("[Notes] " + message), false);
        }
    }

    private static void showError(MinecraftClient client, JsonObject body) {
        String message = getString(body, "message");
        var player = client.player;
        if (player != null && !message.isBlank()) {
            player.sendMessage(Text.literal("[Notes] " + message), false);
        }
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            try {
                listener.run();
            } catch (Throwable throwable) {
                com.zcpu.tzzmod.Tzz_mod.LOGGER.warn("NoteClient listener failed: {}", throwable.getMessage());
            }
        }
    }

    private static void resetState() {
        NOTES.clear();
        DETAILS.clear();
        SHARE_TARGETS.clear();
        UNREAD_NOTE_IDS.clear();
        selfUuid = "";
        isOp = false;
        activeNoteId = "";
        unreadNotificationExpireAtMs = 0L;
        notifyListeners();
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

    public record NoteSummaryData(String noteId, String ownerUuid, String ownerName, String title,
                                  String relation, boolean canManage, long updatedAt, long version) {
    }

    public record NoteDetailData(String noteId, String ownerUuid, String ownerName, String title, String content,
                                 String relation, boolean canManage, long createdAt, long updatedAt, long version,
                                 List<SharedUserData> sharedWith) {
        public NoteSummaryData toSummary() {
            return new NoteSummaryData(noteId, ownerUuid, ownerName, title, relation, canManage, updatedAt, version);
        }
    }

    public record SharedUserData(String uuid, String name) {
    }

    public record ShareTargetData(String uuid, String name, boolean online, boolean selected) {
    }
}
