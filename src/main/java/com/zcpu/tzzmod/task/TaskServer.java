package com.zcpu.tzzmod.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.TaskC2SPayload;
import com.zcpu.tzzmod.network.TaskS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TaskServer {
    private TaskServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendBootstrap(handler.getPlayer(), server))
        );

        ServerPlayNetworking.registerGlobalReceiver(TaskC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendBootstrap(player, server);
        }
    }

    private static void handlePayload(MinecraftServer server, ServerPlayerEntity player, TaskC2SPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "bootstrap" -> sendBootstrap(player, server);
            case "upsert_task" -> handleUpsert(server, player, body);
            default -> sendError(player, "Unknown task action: " + payload.action());
        }
    }

    private static void handleUpsert(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        if (!player.isCreativeLevelTwoOp()) {
            sendError(player, "Only OP can edit task lines.");
            return;
        }

        String lineName = getString(body, "lineName");
        int taskIndex = getInt(body, "taskIndex");
        String titleJson = getString(body, "titleJson");
        String contentJson = getString(body, "contentJson");

        boolean success = TaskDataStore.upsertTask(server, lineName, taskIndex, titleJson, contentJson);
        if (!success) {
            sendError(player, "Invalid task line name or task index.");
            return;
        }

        // full sync to clients
        syncAll(server);
        // also send an explicit 'triggered' notification so clients can immediately notify
        JsonObject triggered = new JsonObject();
        triggered.addProperty("lineName", lineName);
        triggered.addProperty("taskIndex", taskIndex);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new TaskS2CPayload("triggered", triggered.toString()));
        }
        if (player != null) {
            player.sendMessage(Text.literal("已触发任务线 '" + lineName + "' 的第 " + taskIndex + " 个任务。"), false);
        }
    }

    private static void sendBootstrap(ServerPlayerEntity player, MinecraftServer server) {
        TaskDataStore.TaskSnapshot snapshot = TaskDataStore.getSnapshot(server);
        JsonObject body = buildBootstrap(snapshot);
        ServerPlayNetworking.send(player, new TaskS2CPayload("bootstrap", body.toString()));
    }

    private static JsonObject buildBootstrap(TaskDataStore.TaskSnapshot snapshot) {
        JsonObject result = new JsonObject();
        JsonArray lines = new JsonArray();

        for (Map.Entry<String, List<TaskDataStore.TaskNode>> entry : snapshot.lines().entrySet()) {
            String lineName = entry.getKey();
            List<TaskDataStore.TaskNode> tasks = entry.getValue();
            Set<Integer> triggeredIndexes = snapshot.triggered().getOrDefault(lineName, Set.of());

            JsonObject lineObject = new JsonObject();
            lineObject.addProperty("name", lineName);

            JsonArray taskArray = new JsonArray();
            for (int i = 0; i < tasks.size(); i++) {
                TaskDataStore.TaskNode node = tasks.get(i);
                int index = i + 1;

                JsonObject nodeObject = new JsonObject();
                nodeObject.addProperty("index", index);
                nodeObject.addProperty("titleJson", node.titleJson());
                nodeObject.addProperty("contentJson", node.contentJson());
                nodeObject.addProperty("triggered", triggeredIndexes.contains(index));
                taskArray.add(nodeObject);
            }

            lineObject.add("tasks", taskArray);
            lines.add(lineObject);
        }

        result.add("lines", lines);

        if (!snapshot.currentLine().isBlank() && snapshot.currentIndex() > 0) {
            List<TaskDataStore.TaskNode> tasks = snapshot.lines().get(snapshot.currentLine());
            if (tasks != null && snapshot.currentIndex() <= tasks.size()) {
                TaskDataStore.TaskNode currentNode = tasks.get(snapshot.currentIndex() - 1);
                JsonObject current = new JsonObject();
                current.addProperty("lineName", snapshot.currentLine());
                current.addProperty("taskIndex", snapshot.currentIndex());
                current.addProperty("titleJson", currentNode.titleJson());
                current.addProperty("contentJson", currentNode.contentJson());
                current.addProperty("triggeredAt", snapshot.currentTriggeredAt());
                result.add("current", current);
            }
        }

        return result;
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

    private static void sendError(ServerPlayerEntity player, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        ServerPlayNetworking.send(player, new TaskS2CPayload("error", body.toString()));
    }
}

