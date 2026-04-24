package com.zcpu.tzzmod.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.JsonNullability;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class JsonStoreSupport {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStoreSupport() {
    }

    public static <T> T readOrDefault(Path path, Class<T> type, Supplier<T> fallback, String label) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(path)) {
                T value = fallback.get();
                write(path, value, label);
                return value;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                T value = JsonNullability.fromJsonNullable(GSON, reader, type);
                return value != null ? value : fallback.get();
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load {}: {}", label, exception.getMessage());
            return fallback.get();
        }
    }

    public static boolean write(Path path, Object value, String label) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }

            return true;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to write {}: {}", label, exception.getMessage());
            return false;
        }
    }
}
