package com.zcpu.tzzmod.note;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NoteDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, NoteState> CACHE = new WeakHashMap<>();
    private static final int MAX_TITLE_LENGTH = 64;
    private static final int MAX_CONTENT_LENGTH = 25600;

    private NoteDataStore() {
    }

    public static synchronized List<NoteRecord> getVisibleNotes(MinecraftServer server, String viewerUuid, boolean op) {
        List<NoteRecord> result = new ArrayList<>();
        for (List<NoteRecord> ownerNotes : getState(server).notesByOwner.values()) {
            for (NoteRecord note : ownerNotes) {
                if (canView(note, viewerUuid, op)) {
                    result.add(note.copy());
                }
            }
        }
        result.sort(Comparator.comparingLong((NoteRecord note) -> note.updatedAt).reversed());
        return result;
    }

    public static synchronized NoteRecord getVisibleNote(MinecraftServer server, String noteId, String viewerUuid, boolean op) {
        NoteRecord note = getState(server).find(noteId);
        if (note == null || !canView(note, viewerUuid, op)) {
            return null;
        }
        return note.copy();
    }

    public static synchronized NoteRecord getManageableNote(MinecraftServer server, String noteId, String actorUuid, boolean op) {
        NoteRecord note = getState(server).find(noteId);
        if (note == null || !canManage(note, actorUuid, op)) {
            return null;
        }
        return note.copy();
    }

    public static synchronized NoteRecord createNote(MinecraftServer server, String ownerUuid, String ownerName, String title, String content) {
        NoteState state = getState(server);
        NoteRecord note = new NoteRecord();
        note.noteId = UUID.randomUUID().toString();
        note.ownerUuid = ownerUuid;
        note.ownerName = sanitizeName(ownerName, ownerUuid);
        note.title = sanitizeTitle(title);
        note.content = sanitizeContent(content);
        long now = System.currentTimeMillis();
        note.createdAt = now;
        note.updatedAt = now;
        note.version = 1L;
        state.notesByOwner.computeIfAbsent(ownerUuid, ignored -> new ArrayList<>()).add(note);
        state.dirtyOwners.add(ownerUuid);
        return note.copy();
    }

    public static synchronized UpdateResult updateNote(MinecraftServer server, String noteId, String actorUuid, boolean op,
                                                       String title, String content, long expectedVersion) {
        NoteState state = getState(server);
        NoteRecord note = state.find(noteId);
        if (note == null) {
            return UpdateResult.notFound();
        }
        if (!canManage(note, actorUuid, op)) {
            return UpdateResult.denied();
        }
        if (expectedVersion > 0L && note.version != expectedVersion) {
            return UpdateResult.conflict(note.copy());
        }
        note.title = sanitizeTitle(title);
        note.content = sanitizeContent(content);
        note.updatedAt = System.currentTimeMillis();
        note.version++;
        state.dirtyOwners.add(note.ownerUuid);
        return UpdateResult.success(note.copy());
    }

    public static synchronized DeleteResult deleteNote(MinecraftServer server, String noteId, String actorUuid, boolean op) {
        NoteState state = getState(server);
        for (Map.Entry<String, List<NoteRecord>> entry : state.notesByOwner.entrySet()) {
            List<NoteRecord> notes = entry.getValue();
            for (int i = 0; i < notes.size(); i++) {
                NoteRecord note = notes.get(i);
                if (!note.noteId.equals(noteId)) {
                    continue;
                }
                if (!canManage(note, actorUuid, op)) {
                    return DeleteResult.denied();
                }
                NoteRecord deleted = note.copy();
                notes.remove(i);
                state.dirtyOwners.add(entry.getKey());
                return DeleteResult.success(deleted);
            }
        }
        return DeleteResult.notFound();
    }

    public static synchronized ShareResult setSharedUsers(MinecraftServer server, String noteId, String actorUuid, boolean op,
                                                          Set<String> sharedWith, Map<String, String> names) {
        NoteState state = getState(server);
        NoteRecord note = state.find(noteId);
        if (note == null) {
            return ShareResult.notFound();
        }
        if (!canManage(note, actorUuid, op)) {
            return ShareResult.denied();
        }

        Set<String> safeSharedWith = sharedWith == null ? Set.of() : sharedWith;
        Map<String, String> safeNames = names == null ? Map.of() : names;

        NoteRecord before = note.copy();
        LinkedHashSet<String> nextShared = new LinkedHashSet<>();
        LinkedHashMap<String, String> nextNames = new LinkedHashMap<>();
        for (String uuid : safeSharedWith) {
            if (!isValidUuid(uuid) || uuid.equals(note.ownerUuid)) {
                continue;
            }
            nextShared.add(uuid);
            String name = safeNames.get(uuid);
            if (name == null || name.isBlank()) {
                name = note.sharedNames.getOrDefault(uuid, shortUuid(uuid));
            }
            nextNames.put(uuid, sanitizeName(name, uuid));
        }
        note.sharedWith.clear();
        note.sharedWith.addAll(nextShared);
        note.sharedNames.clear();
        note.sharedNames.putAll(nextNames);
        note.updatedAt = System.currentTimeMillis();
        note.version++;
        state.dirtyOwners.add(note.ownerUuid);
        return ShareResult.success(before, note.copy());
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        NoteState state = CACHE.get(server);
        if (state != null) {
            state.flushDirty();
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static synchronized int removeAll(MinecraftServer server) {
        NoteState state = getState(server);
        int count = 0;
        for (List<NoteRecord> notes : state.notesByOwner.values()) {
            count += notes.size();
        }
        state.notesByOwner.clear();
        state.dirtyOwners.clear();
        state.deleteAllFiles();
        return count;
    }

    private static NoteState getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, NoteDataStore::load);
    }

    private static NoteState load(MinecraftServer server) {
        Path dir = server.getRunDirectory().resolve("config").resolve("tzz_mod").resolve("notes");
        NoteState state = new NoteState(dir);
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    String ownerUuid = fileName.substring(0, fileName.length() - ".json".length());
                    if (!isValidUuid(ownerUuid)) {
                        continue;
                    }
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        PersistedOwner persisted = JsonNullability.fromJsonNullable(GSON, reader, PersistedOwner.class);
                        if (persisted == null || persisted.notes == null) {
                            continue;
                        }
                        List<NoteRecord> notes = new ArrayList<>();
                        for (PersistedNote saved : persisted.notes) {
                            NoteRecord note = fromPersisted(ownerUuid, persisted.ownerName, saved);
                            if (note != null) {
                                notes.add(note);
                            }
                        }
                        if (!notes.isEmpty()) {
                            state.notesByOwner.put(ownerUuid, notes);
                        }
                    } catch (Exception exception) {
                        Tzz_mod.LOGGER.warn("Failed to load note file {}: {}", fileName, exception.getMessage());
                    }
                }
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to initialize notes store: {}", exception.getMessage());
        }
        return state;
    }

    private static NoteRecord fromPersisted(String ownerUuid, String ownerName, PersistedNote saved) {
        if (saved == null) {
            return null;
        }
        String noteId = saved.noteId == null || saved.noteId.isBlank() ? UUID.randomUUID().toString() : saved.noteId;
        NoteRecord note = new NoteRecord();
        note.noteId = noteId;
        note.ownerUuid = ownerUuid;
        note.ownerName = sanitizeName(saved.ownerName == null || saved.ownerName.isBlank() ? ownerName : saved.ownerName, ownerUuid);
        note.title = sanitizeTitle(saved.title);
        note.content = sanitizeContent(saved.content);
        note.createdAt = Math.max(0L, saved.createdAt);
        note.updatedAt = Math.max(note.createdAt, saved.updatedAt);
        note.version = Math.max(1L, saved.version);
        if (saved.sharedWith != null) {
            for (String uuid : saved.sharedWith) {
                if (isValidUuid(uuid) && !uuid.equals(ownerUuid)) {
                    note.sharedWith.add(uuid);
                }
            }
        }
        if (saved.sharedNames != null) {
            for (Map.Entry<String, String> entry : saved.sharedNames.entrySet()) {
                if (note.sharedWith.contains(entry.getKey())) {
                    note.sharedNames.put(entry.getKey(), sanitizeName(entry.getValue(), entry.getKey()));
                }
            }
        }
        for (String uuid : note.sharedWith) {
            note.sharedNames.putIfAbsent(uuid, shortUuid(uuid));
        }
        return note;
    }

    public static boolean canView(NoteRecord note, String viewerUuid, boolean op) {
        return note != null && (op || note.ownerUuid.equals(viewerUuid) || note.sharedWith.contains(viewerUuid));
    }

    public static boolean canManage(NoteRecord note, String actorUuid, boolean op) {
        return note != null && (op || note.ownerUuid.equals(actorUuid));
    }

    public static String relation(NoteRecord note, String viewerUuid, boolean op) {
        if (note.ownerUuid.equals(viewerUuid)) {
            return "owned";
        }
        if (op) {
            return "admin";
        }
        return "shared";
    }

    private static String sanitizeTitle(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            value = "New Note";
        }
        return truncate(value, MAX_TITLE_LENGTH);
    }

    private static String sanitizeContent(String raw) {
        return truncate(raw == null ? "" : raw, MAX_CONTENT_LENGTH);
    }

    private static String sanitizeName(String raw, String fallbackUuid) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            value = shortUuid(fallbackUuid);
        }
        return truncate(value, 32);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public static boolean isValidUuid(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String shortUuid(String uuid) {
        if (uuid == null || uuid.length() < 8) {
            return "Player";
        }
        return uuid.substring(0, 8);
    }

    private static final class NoteState {
        private final Path dir;
        private final LinkedHashMap<String, List<NoteRecord>> notesByOwner = new LinkedHashMap<>();
        private final Set<String> dirtyOwners = new LinkedHashSet<>();

        private NoteState(Path dir) {
            this.dir = dir;
        }

        private NoteRecord find(String noteId) {
            if (noteId == null || noteId.isBlank()) {
                return null;
            }
            for (List<NoteRecord> notes : notesByOwner.values()) {
                for (NoteRecord note : notes) {
                    if (note.noteId.equals(noteId)) {
                        return note;
                    }
                }
            }
            return null;
        }

        private void flushDirty() {
            if (dirtyOwners.isEmpty()) {
                return;
            }
            List<String> owners = new ArrayList<>(dirtyOwners);
            for (String ownerUuid : owners) {
                if (writeOwner(ownerUuid)) {
                    dirtyOwners.remove(ownerUuid);
                }
            }
        }

        private boolean writeOwner(String ownerUuid) {
            try {
                Files.createDirectories(dir);
                List<NoteRecord> notes = notesByOwner.getOrDefault(ownerUuid, List.of());
                Path file = dir.resolve(ownerUuid + ".json");
                if (notes.isEmpty()) {
                    Files.deleteIfExists(file);
                    return true;
                }
                PersistedOwner owner = new PersistedOwner();
                owner.ownerUuid = ownerUuid;
                owner.ownerName = notes.get(0).ownerName;
                owner.notes = new ArrayList<>();
                for (NoteRecord note : notes) {
                    PersistedNote saved = new PersistedNote();
                    saved.noteId = note.noteId;
                    saved.ownerName = note.ownerName;
                    saved.title = note.title;
                    saved.content = note.content;
                    saved.sharedWith = new ArrayList<>(note.sharedWith);
                    saved.sharedNames = new LinkedHashMap<>(note.sharedNames);
                    saved.createdAt = note.createdAt;
                    saved.updatedAt = note.updatedAt;
                    saved.version = note.version;
                    owner.notes.add(saved);
                }
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(owner, writer);
                }
                return true;
            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to write notes for {}: {}", ownerUuid, exception.getMessage());
                return false;
            }
        }

        private void deleteAllFiles() {
            try {
                Files.createDirectories(dir);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                    for (Path path : stream) {
                        Files.deleteIfExists(path);
                    }
                }
            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to clear notes store: {}", exception.getMessage());
            }
        }
    }

    public static final class NoteRecord {
        public String noteId = "";
        public String ownerUuid = "";
        public String ownerName = "";
        public String title = "";
        public String content = "";
        public final LinkedHashSet<String> sharedWith = new LinkedHashSet<>();
        public final LinkedHashMap<String, String> sharedNames = new LinkedHashMap<>();
        public long createdAt;
        public long updatedAt;
        public long version;

        public NoteRecord copy() {
            NoteRecord copy = new NoteRecord();
            copy.noteId = noteId;
            copy.ownerUuid = ownerUuid;
            copy.ownerName = ownerName;
            copy.title = title;
            copy.content = content;
            copy.sharedWith.addAll(sharedWith);
            copy.sharedNames.putAll(sharedNames);
            copy.createdAt = createdAt;
            copy.updatedAt = updatedAt;
            copy.version = version;
            return copy;
        }
    }

    public record UpdateResult(String status, NoteRecord note) {
        static UpdateResult success(NoteRecord note) { return new UpdateResult("success", note); }
        static UpdateResult conflict(NoteRecord note) { return new UpdateResult("conflict", note); }
        static UpdateResult denied() { return new UpdateResult("denied", null); }
        static UpdateResult notFound() { return new UpdateResult("not_found", null); }
    }

    public record DeleteResult(String status, NoteRecord deletedNote) {
        static DeleteResult success(NoteRecord note) { return new DeleteResult("success", note); }
        static DeleteResult denied() { return new DeleteResult("denied", null); }
        static DeleteResult notFound() { return new DeleteResult("not_found", null); }
    }

    public record ShareResult(String status, NoteRecord before, NoteRecord after) {
        static ShareResult success(NoteRecord before, NoteRecord after) { return new ShareResult("success", before, after); }
        static ShareResult denied() { return new ShareResult("denied", null, null); }
        static ShareResult notFound() { return new ShareResult("not_found", null, null); }
    }

    private static final class PersistedOwner {
        private String ownerUuid = "";
        private String ownerName = "";
        private List<PersistedNote> notes = new ArrayList<>();
    }

    private static final class PersistedNote {
        private String noteId = "";
        private String ownerName = "";
        private String title = "";
        private String content = "";
        private List<String> sharedWith = new ArrayList<>();
        private Map<String, String> sharedNames = new LinkedHashMap<>();
        private long createdAt;
        private long updatedAt;
        private long version;
    }
}
