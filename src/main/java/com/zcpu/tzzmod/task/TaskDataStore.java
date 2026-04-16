package com.zcpu.tzzmod.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.JsonNullability;
import net.minecraft.server.MinecraftServer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class TaskDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, TaskState> CACHE = new WeakHashMap<>();

    private TaskDataStore() {
    }

    public static synchronized TaskSnapshot getSnapshot(MinecraftServer server) {
        return getState(server).toSnapshot();
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        TaskState state = CACHE.get(server);
        if (state != null) {
            state.flushDirty();
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static synchronized boolean upsertTask(MinecraftServer server, String lineName, int indexOneBased, String titleJson, String contentJson) {
        String cleanLineName = sanitizeLineName(lineName);
        if (cleanLineName.isEmpty() || indexOneBased < 1 || indexOneBased > 512) {
            return false;
        }

        TaskState state = getState(server);
        List<TaskNode> tasks = state.lines.computeIfAbsent(cleanLineName, ignored -> new ArrayList<>());
        while (tasks.size() < indexOneBased) {
            tasks.add(new TaskNode("{\"text\":\"\"}", "{\"text\":\"\"}"));
        }
        tasks.set(indexOneBased - 1, new TaskNode(normalizeJsonText(titleJson), normalizeJsonText(contentJson)));
        state.markDirty();
        return true;
    }

    public static synchronized boolean triggerTask(MinecraftServer server, String lineName, int indexOneBased) {
        String cleanLineName = sanitizeLineName(lineName);
        if (cleanLineName.isEmpty() || indexOneBased < 1) {
            return false;
        }

        TaskState state = getState(server);
        List<TaskNode> tasks = state.lines.get(cleanLineName);
        if (tasks == null || indexOneBased > tasks.size()) {
            return false;
        }

        state.triggered.computeIfAbsent(cleanLineName, ignored -> new LinkedHashSet<>()).add(indexOneBased);
        state.currentLine = cleanLineName;
        state.currentIndex = indexOneBased;
        state.currentTriggeredAt = System.currentTimeMillis();
        state.markDirty();
        return true;
    }

    // New: un-trigger a task (remove from triggered set). Returns true if removed.
    public static synchronized boolean untriggerTask(MinecraftServer server, String lineName, int indexOneBased) {
        String cleanLineName = sanitizeLineName(lineName);
        if (cleanLineName.isEmpty() || indexOneBased < 1) {
            return false;
        }

        TaskState state = getState(server);
        List<TaskNode> tasks = state.lines.get(cleanLineName);
        if (tasks == null || indexOneBased > tasks.size()) {
            return false;
        }

        var set = state.triggered.get(cleanLineName);
        if (set == null || !set.remove(indexOneBased)) {
            return false; // nothing to untrigger
        }

        if (set.isEmpty()) {
            state.triggered.remove(cleanLineName);
        } else {
            state.triggered.put(cleanLineName, set);
        }

        // if the current pointer pointed to this triggered task, clear the current pointer
        if (cleanLineName.equals(state.currentLine) && state.currentIndex == indexOneBased) {
            state.currentLine = "";
            state.currentIndex = 0;
            state.currentTriggeredAt = 0L;
        }

        state.markDirty();
        return true;
    }

    // New: delete a single task by index (1-based). Returns true if deleted.
    public static synchronized boolean deleteTask(MinecraftServer server, String lineName, int indexOneBased) {
        String cleanLineName = sanitizeLineName(lineName);
        if (cleanLineName.isEmpty() || indexOneBased < 1) {
            return false;
        }

        TaskState state = getState(server);
        List<TaskNode> tasks = state.lines.get(cleanLineName);
        if (tasks == null || indexOneBased > tasks.size()) {
            return false;
        }

        // remove the specific task
        tasks.remove(indexOneBased - 1);

        // if the line becomes empty, remove the line entirely
        if (tasks.isEmpty()) {
            state.lines.remove(cleanLineName);
            state.triggered.remove(cleanLineName);
        } else {
            // adjust triggered indexes: remove any equal to removed index and shift greater ones down
            var oldSet = state.triggered.get(cleanLineName);
            if (oldSet != null && !oldSet.isEmpty()) {
                var newSet = new LinkedHashSet<Integer>();
                for (Integer idx : oldSet) {
                    if (idx == null) continue;
                    if (idx == indexOneBased) continue; // removed
                    if (idx > indexOneBased) newSet.add(idx - 1);
                    else newSet.add(idx);
                }
                if (newSet.isEmpty()) state.triggered.remove(cleanLineName);
                else state.triggered.put(cleanLineName, newSet);
            }
        }

        // if current pointer pointed to removed task, clear it
        if (cleanLineName.equals(state.currentLine) && state.currentIndex == indexOneBased) {
            state.currentLine = "";
            state.currentIndex = 0;
            state.currentTriggeredAt = 0L;
        } else if (cleanLineName.equals(state.currentLine) && state.currentIndex > indexOneBased) {
            state.currentIndex = Math.max(0, state.currentIndex - 1);
        }

        state.markDirty();
        return true;
    }

    // New: delete an entire task line. Returns true if removed.
    public static synchronized boolean deleteTaskLine(MinecraftServer server, String lineName) {
        String cleanLineName = sanitizeLineName(lineName);
        if (cleanLineName.isEmpty()) return false;

        TaskState state = getState(server);
        if (!state.lines.containsKey(cleanLineName)) return false;

        state.lines.remove(cleanLineName);
        state.triggered.remove(cleanLineName);

        if (cleanLineName.equals(state.currentLine)) {
            state.currentLine = "";
            state.currentIndex = 0;
            state.currentTriggeredAt = 0L;
        }

        state.markDirty();
        return true;
    }

    private static TaskState getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, TaskDataStore::load);
    }

    private static TaskState load(MinecraftServer server) {
        Path dir = server.getRunDirectory().resolve("config").resolve("tzz_mod").resolve("taskline");
        Path legacy = server.getRunDirectory().resolve("config").resolve("tzz_mod-tasks.json");
        TaskState state = new TaskState(dir, legacy);

        try {
            Files.createDirectories(legacy.getParent());

            // If directory exists and has JSON files, prefer loading them
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                    boolean found = false;
                    for (Path p : stream) {
                        String fileName = p.getFileName().toString();
                        if ("_meta.json".equals(fileName)) continue;
                        found = true;
                        try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                            String name = obj.has("name") ? obj.get("name").getAsString() : fileName.replaceFirst("\\\\.json$", "");
                            List<PersistedTaskNode> nodes = new ArrayList<>();
                            if (obj.has("tasks") && obj.get("tasks").isJsonArray()) {
                                for (JsonElement e : obj.getAsJsonArray("tasks")) {
                                    try {
                                        JsonObject t = e.getAsJsonObject();
                                        PersistedTaskNode n = new PersistedTaskNode();
                                        n.titleJson = t.has("titleJson") ? t.get("titleJson").getAsString() : "{\"text\":\"\"}";
                                        n.contentJson = t.has("contentJson") ? t.get("contentJson").getAsString() : "{\"text\":\"\"}";
                                        nodes.add(n);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            state.lines.put(sanitizeLineName(name), new ArrayList<>());
                            List<TaskNode> nodeList = new ArrayList<>();
                            for (PersistedTaskNode n : nodes) {
                                nodeList.add(new TaskNode(normalizeJsonText(n.titleJson), normalizeJsonText(n.contentJson)));
                            }
                            state.lines.put(sanitizeLineName(name), nodeList);

                            // load triggered from file if present
                            if (obj.has("triggered") && obj.get("triggered").isJsonArray()) {
                                LinkedHashSet<Integer> triggeredSet = new LinkedHashSet<>();
                                for (JsonElement e : obj.getAsJsonArray("triggered")) {
                                    try {
                                        int v = e.getAsInt();
                                        if (v > 0) triggeredSet.add(v);
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (!triggeredSet.isEmpty()) {
                                    state.triggered.put(sanitizeLineName(name), triggeredSet);
                                }
                            }
                        }
                    }

                    // load _meta.json for current pointer if present
                    Path meta = dir.resolve("_meta.json");
                    if (Files.exists(meta)) {
                        try (Reader reader = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
                            PersistedState metaState = JsonNullability.fromJsonNullable(GSON, reader, PersistedState.class);
                            if (metaState != null) {
                                state.currentLine = sanitizeLineName(metaState.currentLine);
                                state.currentIndex = Math.max(0, metaState.currentIndex);
                                state.currentTriggeredAt = Math.max(0L, metaState.currentTriggeredAt);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (found) {
                        return state;
                    }
                }
            }

            // fallback to legacy single-file format; if exists, read and migrate
            if (Files.exists(legacy)) {
                try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
                    PersistedState persisted = JsonNullability.fromJsonNullable(GSON, reader, PersistedState.class);
                    state.apply(persisted);
                }

                // attempt migration: write per-line files and meta, and backup legacy
                try {
                    Files.createDirectories(dir);
                    state.writeToDisk();
                    String legacyName = legacy.getFileName().toString();
                    Path bak = legacy.resolveSibling(legacyName + ".bak");
                    Files.copy(legacy, bak);
                } catch (Exception ignored) {
                    // migration best-effort
                }

                return state;
            }

            // nothing existed - persist initial state (writes both formats)
            state.writeToDisk();
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load task config: {}", exception.getMessage());
        }

        return state;
    }

    private static String sanitizeLineName(String lineName) {
        if (lineName == null) {
            return "";
        }
        String clean = lineName.trim();
        if (clean.length() > 64) {
            clean = clean.substring(0, 64);
        }
        return clean;
    }

    private static String normalizeJsonText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{\"text\":\"\"}";
        }

        String trimmed = raw.trim();
        try {
            JsonParser.parseString(trimmed);
            return trimmed;
        } catch (Exception ignored) {
            return "{\"text\":" + GSON.toJson(raw) + "}";
        }
    }

    private static String toSafeLineFileName(String lineName) {
        String safeName = lineName.replaceAll("[^a-zA-Z0-9-_\\. ]", "_");
        if (safeName.isBlank()) {
            safeName = "line_" + Math.abs(lineName.hashCode());
        }
        return safeName + ".json";
    }

    public record TaskNode(String titleJson, String contentJson) {
    }

    public record TaskSnapshot(
            Map<String, List<TaskNode>> lines,
            Map<String, Set<Integer>> triggered,
            String currentLine,
            int currentIndex,
            long currentTriggeredAt
    ) {
    }

    private static final class TaskState {
        private final Path dirPath;
        private final Path legacyPath;
        private final LinkedHashMap<String, List<TaskNode>> lines = new LinkedHashMap<>();
        private final LinkedHashMap<String, LinkedHashSet<Integer>> triggered = new LinkedHashMap<>();
        private String currentLine = "";
        private int currentIndex = 0;
        private long currentTriggeredAt = 0L;
        private boolean dirty;

        private TaskState(Path dirPath, Path legacyPath) {
            this.dirPath = dirPath;
            this.legacyPath = legacyPath;
        }

        private void apply(PersistedState persisted) {
            if (persisted == null) {
                return;
            }

            lines.clear();
            if (persisted.lines != null) {
                for (Map.Entry<String, List<PersistedTaskNode>> entry : persisted.lines.entrySet()) {
                    String lineName = sanitizeLineName(entry.getKey());
                    if (lineName.isEmpty()) {
                        continue;
                    }

                    List<TaskNode> nodeList = new ArrayList<>();
                    List<PersistedTaskNode> savedNodes = entry.getValue() == null ? List.of() : entry.getValue();
                    for (PersistedTaskNode savedNode : savedNodes) {
                        if (savedNode == null) {
                            continue;
                        }
                        nodeList.add(new TaskNode(
                                normalizeJsonText(savedNode.titleJson),
                                normalizeJsonText(savedNode.contentJson)
                        ));
                    }
                    lines.put(lineName, nodeList);
                }
            }

            triggered.clear();
            if (persisted.triggered != null) {
                for (Map.Entry<String, List<Integer>> entry : persisted.triggered.entrySet()) {
                    String lineName = sanitizeLineName(entry.getKey());
                    if (lineName.isEmpty()) {
                        continue;
                    }
                    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
                    if (entry.getValue() != null) {
                        for (Integer value : entry.getValue()) {
                            if (value != null && value > 0) {
                                indexes.add(value);
                            }
                        }
                    }
                    triggered.put(lineName, indexes);
                }
            }

            currentLine = sanitizeLineName(persisted.currentLine);
            currentIndex = Math.max(0, persisted.currentIndex);
            currentTriggeredAt = Math.max(0L, persisted.currentTriggeredAt);
        }

        private TaskSnapshot toSnapshot() {
            LinkedHashMap<String, List<TaskNode>> lineCopy = new LinkedHashMap<>();
            for (Map.Entry<String, List<TaskNode>> entry : lines.entrySet()) {
                lineCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            LinkedHashMap<String, Set<Integer>> triggeredCopy = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashSet<Integer>> entry : triggered.entrySet()) {
                triggeredCopy.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }

            return new TaskSnapshot(
                    Map.copyOf(lineCopy),
                    Map.copyOf(triggeredCopy),
                    currentLine,
                    currentIndex,
                    currentTriggeredAt
            );
        }

        private void markDirty() {
            dirty = true;
        }

        private void flushDirty() {
            if (!dirty) {
                return;
            }
            if (writeToDisk()) {
                dirty = false;
            }
        }

        private boolean writeToDisk() {
            try {
                // ensure parent dir for legacy exists
                Files.createDirectories(legacyPath.getParent());

                // write legacy single-file format (for backward compatibility)
                PersistedState persisted = new PersistedState();
                persisted.currentLine = currentLine;
                persisted.currentIndex = currentIndex;
                persisted.currentTriggeredAt = currentTriggeredAt;

                persisted.lines = new LinkedHashMap<>();
                for (Map.Entry<String, List<TaskNode>> entry : lines.entrySet()) {
                    List<PersistedTaskNode> taskNodes = new ArrayList<>();
                    for (TaskNode node : entry.getValue()) {
                        PersistedTaskNode persistedNode = new PersistedTaskNode();
                        persistedNode.titleJson = node.titleJson();
                        persistedNode.contentJson = node.contentJson();
                        taskNodes.add(persistedNode);
                    }
                    persisted.lines.put(entry.getKey(), taskNodes);
                }

                persisted.triggered = new LinkedHashMap<>();
                for (Map.Entry<String, LinkedHashSet<Integer>> entry : triggered.entrySet()) {
                    persisted.triggered.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }

                // write legacy file
                try (Writer writer = Files.newBufferedWriter(legacyPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(persisted, writer);
                }

                // also write per-line files and meta
                Files.createDirectories(dirPath);

                Set<String> activeLineFiles = new LinkedHashSet<>();
                for (String lineName : lines.keySet()) {
                    activeLineFiles.add(toSafeLineFileName(lineName));
                }

                // Clean up any stale per-line files that no longer correspond to in-memory lines.
                try {
                    if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.json")) {
                            for (Path p : stream) {
                                String fileName = p.getFileName().toString();
                                if ("_meta.json".equals(fileName)) continue; // keep meta (will be rewritten)
                                if (!activeLineFiles.contains(fileName)) {
                                    try {
                                        Files.deleteIfExists(p);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                for (Map.Entry<String, List<TaskNode>> entry : lines.entrySet()) {
                    String name = entry.getKey();
                    Path file = dirPath.resolve(toSafeLineFileName(name));

                    JsonObject obj = new JsonObject();
                    obj.addProperty("name", name);
                    JsonArray tasks = new JsonArray();
                    for (TaskNode node : entry.getValue()) {
                        JsonObject n = new JsonObject();
                        n.addProperty("titleJson", node.titleJson());
                        n.addProperty("contentJson", node.contentJson());
                        tasks.add(n);
                    }
                    obj.add("tasks", tasks);

                    // triggered for this line
                    if (triggered.containsKey(name)) {
                        JsonArray trg = new JsonArray();
                        for (Integer i : triggered.get(name)) trg.add(i);
                        obj.add("triggered", trg);
                    }

                    try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                        GSON.toJson(obj, writer);
                    }
                }

                // write meta file
                Path meta = dirPath.resolve("_meta.json");
                try (Writer writer = Files.newBufferedWriter(meta, StandardCharsets.UTF_8)) {
                    GSON.toJson(persisted, writer);
                }

                return true;

            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to write task config: {}", exception.getMessage());
                return false;
            }
        }
    }

    private static final class PersistedState {
        private Map<String, List<PersistedTaskNode>> lines;
        private Map<String, List<Integer>> triggered;
        private String currentLine = "";
        private int currentIndex = 0;
        private long currentTriggeredAt = 0L;
    }

    private static final class PersistedTaskNode {
        private String titleJson;
        private String contentJson;
    }
}

