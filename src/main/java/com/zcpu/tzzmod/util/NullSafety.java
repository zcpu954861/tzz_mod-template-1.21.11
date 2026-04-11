package com.zcpu.tzzmod.util;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class NullSafety {
    private NullSafety() {
    }

    public static <T> @NonNull T requireNonNull(@Nullable T value) {
        return Objects.requireNonNull(value);
    }
}