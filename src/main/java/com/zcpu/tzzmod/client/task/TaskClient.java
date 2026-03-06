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
    private static final Set<String> UNREAD_TRIGGERED = new HashSet<>();
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
        return UNREAD_TRIGGERED.size();
    }

    public static List<UnreadEntry> getUnreadEntries() {
        Map<String, Integer> counts = buildUnreadCountsByLine();
        List<UnreadEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
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
        if (!UNREAD_TRIGGERED.isEmpty()) {
            UNREAD_TRIGGERED.clear();
            notifyListeners();
        }
    }

    private static Map<String, Integer> buildUnreadCountsByLine() {
        Map<String, Integer> counts = new HashMap<>();
        for (String id : UNREAD_TRIGGERED) {
            String[] parts = id.split(":", 2);
            if (parts.length < 2 || parts[0].isBlank()) {
                continue;
            }
            counts.merge(parts[0], 1, Integer::sum);
        }
        return counts;
    }

    private static String resolveLineTitle(String lineName) {
        for (TaskLineData line : LINES) {
            if (line.name().equals(lineName)) {
                return lineName;
            }
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
        JsonObject body = parse(payload.bodyJson());
        String action = payload.action();

        try {
            Tzz_mod.LOGGER.debug("TaskClient.handlePayload action={} bodyLinesPresent={}", action, body.has("lines"));
        } catch (Exception ignored) {}

        if ("triggered".equals(action)) {
            String lineName = getString(body, "lineName");
            int idx = getInt(body, "taskIndex");
            if (!lineName.isEmpty() && idx > 0) {
                String id = lineName + ":" + idx;
                if (UNREAD_TRIGGERED.add(id)) {
                    unreadNotificationExpireAtMs = System.currentTimeMillis() + 15_000L;
                    playNotification(client);
                    if (PhoneSettingsClient.isAlertModeEnabled()) {
                        AlertSubtitleOverlay.enqueue(Text.translatable("phone.tzz_mod.alert.task_subtitle"));
                    }
                }
                KNOWN_TRIGGERED.add(id);
                notifyListeners();
            }
            return;
        }

        if ("untriggered".equals(action)) {
            String lineName = getString(body, "lineName");
            int idx = getInt(body, "taskIndex");
            if (!lineName.isEmpty() && idx > 0) {
                String id = lineName + ":" + idx;
                KNOWN_TRIGGERED.remove(id);
                UNREAD_TRIGGERED.remove(id);
                notifyListeners();
            }
            return;
        }

        Set<String> currentTriggeredFromBody = collectTriggeredIds(body);

        switch (action) {
            case "bootstrap" -> {
                applyBootstrap(body);
                reconcileTriggeredState(currentTriggeredFromBody);
            }
            case "error" -> showError(client, body);
            default -> {
            }
        }

        notifyListeners();
    }

    private static Set<String> collectTriggeredIds(JsonObject body) {
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
                                if (trg && !lineName.isEmpty() && idx > 0) {
                                    currentTriggeredFromBody.add(lineName + ":" + idx);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return currentTriggeredFromBody;
    }

    private static void reconcileTriggeredState(Set<String> currentTriggeredFromBody) {
        UNREAD_TRIGGERED.removeIf(id -> !currentTriggeredFromBody.contains(id));
        KNOWN_TRIGGERED.clear();
        KNOWN_TRIGGERED.addAll(currentTriggeredFromBody);
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
