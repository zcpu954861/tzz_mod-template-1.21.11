package com.zcpu.tzzmod.util;

import com.google.gson.Gson;
import java.io.Reader;
import org.jspecify.annotations.Nullable;

public final class JsonNullability {
    private JsonNullability() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> @Nullable T fromJsonNullable(Gson gson, Reader reader, Class<T> type) {
        return (@Nullable T) gson.fromJson(reader, (Class) type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> @Nullable T fromJsonNullable(Gson gson, String json, Class<T> type) {
        return (@Nullable T) gson.fromJson(json, (Class) type);
    }
}