package com.zcpu.tzzmod.client.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.network.TaskC2SPayload;
import com.zcpu.tzzmod.network.TaskS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public final class TaskClient {
    private static final Set<Runnable> LISTENERS = new CopyOnWriteArraySet<>();
    private static final List<TaskLineData> LINES = new ArrayList<>();
    private static CurrentTaskData currentTask;

    // --- notification tracking (modeled after PhoneChatClient) ---
    private static final Map<String, Integer> UNREAD_COUNTS = new HashMap<>();
    private static long unreadNotificationExpireAtMs;
    private static String notificationSound = "minecraft:entity.experience_orb.pickup";
    // track last-seen triggered ids across payloads for reliable diffing
    private static final Set<String> KNOWN_TRIGGERED = new HashSet<>();

    private TaskClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TaskS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
    }

    public static void requestBootstrap() {
        send("bootstrap", new JsonObject());
    }

    public static void upsertTask(String lineName, int taskIndex, String titleJson, String contentJson) {
        JsonObject body = new JsonObject();
        body.addProperty("lineName", lineName);
        body.addProperty("taskIndex", taskIndex);
        body.addProperty("titleJson", titleJson);
        body.addProperty("contentJson", contentJson);
        send("upsert_task", body);
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static List<TaskLineData> getLines() {
        return List.copyOf(LINES);
    }

    public static CurrentTaskData getCurrentTask() {
        return currentTask;
    }

    public static long getUnreadNotificationExpireAtMs() {
        return unreadNotificationExpireAtMs;
    }

    // New: return total unread tasks across all lines (sum of UNREAD_COUNTS)
    public static int getTotalUnreadCount() {
        int total = 0;
        for (Integer value : UNREAD_COUNTS.values()) {
            if (value != null && value > 0) {
                total += value;
            }
        }
        return total;
    }

    public static List<UnreadEntry> getUnreadEntries() {
        List<UnreadEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : UNREAD_COUNTS.entrySet()) {
            int count = entry.getValue();
            if (count <= 0) continue;
            String lineName = entry.getKey();
            String title = resolveLineTitle(lineName);
            entries.add(new UnreadEntry(lineName, title, count));
        }
        entries.sort(Comparator.comparing(UnreadEntry::title, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    public static void clearAllUnread() {
        if (!UNREAD_COUNTS.isEmpty()) {
            UNREAD_COUNTS.clear();
            notifyListeners();
        }
    }

    private static String resolveLineTitle(String lineName) {
        for (TaskLineData line : LINES) {
            if (line.name().equals(lineName)) return lineName; // TODO: could parse friendly title from resources if present
        }
        return lineName;
    }

    private static void send(String action, JsonObject body) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new TaskC2SPayload(action, body.toString()));
    }

    private static void handlePayload(MinecraftClient client, TaskS2CPayload payload) {
        // capture whether we had a previous snapshot (used to suppress initial-join noise)
        boolean hadPreviousSnapshot = !LINES.isEmpty();

        JsonObject body = parse(payload.bodyJson());
        String action = payload.action();

        // Debug logging to help trace notification flows
        try {
            Tzz_mod.LOGGER.debug("TaskClient.handlePayload action={} bodyLinesPresent={}", action, body.has("lines"));
        } catch (Exception ignored) {}

        // Server may send an explicit 'triggered' notification with a single lineName/taskIndex
        // when a task is triggered. Handle it quickly: if it's new for us, mark unread and play alert.
        if ("triggered".equals(action)) {
            String lineName = getString(body, "lineName");
            int idx = getInt(body, "taskIndex");
            if (!lineName.isEmpty() && idx > 0) {
                String id = lineName + ":" + idx;
                if (!KNOWN_TRIGGERED.contains(id)) {
                    UNREAD_COUNTS.merge(lineName, 1, Integer::sum);
                    unreadNotificationExpireAtMs = System.currentTimeMillis() + 15_000L;
                    // Always play a short sound so user receives audible feedback; if alert mode
                    // is enabled also enqueue subtitle-style notification.
                    playNotification(client);
                    if (PhoneSettingsClient.isAlertModeEnabled()) {
                        AlertSubtitleOverlay.enqueue(Text.translatable("phone.tzz_mod.alert.task_subtitle"));
                    }
                }
                KNOWN_TRIGGERED.add(id);
                // Also refresh full lines from server in case bootstrap wasn't sent
                notifyListeners();
            }
            return;
        }

        // Server may also send 'untriggered' when a task is cancelled. Ensure we clear known state
        // so future re-triggers will be detected as new, and update unread counts/UI.
        if ("untriggered".equals(action)) {
            String lineName = getString(body, "lineName");
            int idx = getInt(body, "taskIndex");
            if (!lineName.isEmpty() && idx > 0) {
                String id = lineName + ":" + idx;
                KNOWN_TRIGGERED.remove(id);
                // decrement unread count for that line if present (safely)
                UNREAD_COUNTS.computeIfPresent(lineName, (k, v) -> v > 1 ? v - 1 : null);
                notifyListeners();
            }
            return;
        }

        // Build currentTriggeredFromBody from payload if available
        Set<String> currentTriggeredFromBody = new HashSet<>();
        if (body.has("lines") && body.get("lines").isJsonArray()) {
            for (JsonElement lineEl : body.getAsJsonArray("lines")) {
                try {
                    JsonObject lineObj = lineEl.getAsJsonObject();
                    String lineName = getString(lineObj, "name");
                    if (lineObj.has("tasks") && lineObj.get("tasks").isJsonArray()) {
                        for (JsonElement taskEl : lineObj.getAsJsonArray("tasks")) {
                            try {
                                JsonObject taskObj = taskEl.getAsJsonObject();
                                int idx = getInt(taskObj, "index");
                                boolean trg = getBoolean(taskObj, "triggered", false);
                                if (trg) currentTriggeredFromBody.add(lineName + ":" + idx);
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        switch (action) {
            case "bootstrap" -> applyBootstrap(body);
            case "error" -> showError(client, body);
            default -> {}
        }

        // Decide whether this is the very first bootstrap (client had no prior snapshot and
        // we haven't observed any triggers yet). In that case, skip notifying to avoid noisy
        // initial join behaviour. For subsequent bootstraps or other actions, detect newly
        // triggered IDs by comparing the server-sent set against KNOWN_TRIGGERED.
        boolean initialBootstrap = "bootstrap".equals(action) && !hadPreviousSnapshot && KNOWN_TRIGGERED.isEmpty();
        if (!initialBootstrap) {
            Set<String> newlyTriggered = new HashSet<>();
            for (String id : currentTriggeredFromBody) {
                if (!KNOWN_TRIGGERED.contains(id)) {
                    newlyTriggered.add(id);
                    String lineName = id.split(":", 2)[0];
                    UNREAD_COUNTS.merge(lineName, 1, Integer::sum);
                }
            }

            if (!newlyTriggered.isEmpty()) {
                unreadNotificationExpireAtMs = System.currentTimeMillis() + 15_000L;
                // Always play a sound; subtitle if alert mode enabled
                playNotification(client);
                if (PhoneSettingsClient.isAlertModeEnabled()) {
                    AlertSubtitleOverlay.enqueue(Text.translatable("phone.tzz_mod.alert.task_subtitle"));
                }
            }
        }

        // Update KNOWN_TRIGGERED to the current server state so future diffs are correct.
        KNOWN_TRIGGERED.clear();
        KNOWN_TRIGGERED.addAll(currentTriggeredFromBody);

        notifyListeners();
    }

    private static void applyBootstrap(JsonObject body) {
        LINES.clear();

        if (body.has("lines") && body.get("lines").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("lines")) {
                JsonObject lineObject = element.getAsJsonObject();
                String lineName = getString(lineObject, "name");
                List<TaskNodeData> tasks = new ArrayList<>();

                if (lineObject.has("tasks") && lineObject.get("tasks").isJsonArray()) {
                    for (JsonElement taskElement : lineObject.getAsJsonArray("tasks")) {
                        JsonObject taskObject = taskElement.getAsJsonObject();
                        tasks.add(new TaskNodeData(
                                getInt(taskObject, "index"),
                                getString(taskObject, "titleJson"),
                                getString(taskObject, "contentJson"),
                                getBoolean(taskObject, "triggered", false)
                        ));
                    }
                }

                tasks.sort(Comparator.comparingInt(TaskNodeData::index));
                LINES.add(new TaskLineData(lineName, List.copyOf(tasks)));
            }
        }

        LINES.sort(Comparator.comparing(TaskLineData::name, String.CASE_INSENSITIVE_ORDER));

        if (body.has("current") && body.get("current").isJsonObject()) {
            JsonObject currentObject = body.getAsJsonObject("current");
            currentTask = new CurrentTaskData(
                    getString(currentObject, "lineName"),
                    getInt(currentObject, "taskIndex"),
                    getString(currentObject, "titleJson"),
                    getString(currentObject, "contentJson")
            );
        } else {
            currentTask = null;
        }
    }

    private static void playNotification(MinecraftClient client) {
        if (client.player == null) return;

        SoundEvent event = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        Identifier id = Identifier.tryParse(notificationSound);
        if (id != null && Registries.SOUND_EVENT.containsId(id)) {
            event = Registries.SOUND_EVENT.get(id);
        }
        client.player.playSound(event, 0.8F, 1.2F);
    }

    private static void showError(MinecraftClient client, JsonObject body) {
        String message = getString(body, "message");
        if (client.player != null && !message.isBlank()) {
            client.player.sendMessage(Text.literal("[Task] " + message), false);
        }
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
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

    private static int getInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
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

    public record TaskLineData(String name, List<TaskNodeData> tasks) {
    }

    public record TaskNodeData(int index, String titleJson, String contentJson, boolean triggered) {
    }

    public record CurrentTaskData(String lineName, int taskIndex, String titleJson, String contentJson) {
    }

    public record UnreadEntry(String lineName, String title, int count) { }
}
