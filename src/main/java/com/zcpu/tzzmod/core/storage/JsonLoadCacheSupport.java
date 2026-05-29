package com.zcpu.tzzmod.core.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class JsonLoadCacheSupport {
    private JsonLoadCacheSupport() {
    }

    public static Path cacheKey(Path path) {
        return path == null ? Path.of("") : path.toAbsolutePath().normalize();
    }

    public static FileFingerprint fingerprint(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return new FileFingerprint(false, -1L, -1L, "");
            }
            byte[] bytes = Files.readAllBytes(path);
            return new FileFingerprint(
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    bytes.length,
                    sha256(bytes)
            );
        } catch (Exception ignored) {
            return new FileFingerprint(true, -1L, -1L, "");
        }
    }

    private static String sha256(byte[] bytes) {
        byte[] safeBytes = bytes == null ? new byte[0] : bytes;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(safeBytes));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(java.util.Arrays.hashCode(safeBytes));
        }
    }

    public record FileFingerprint(boolean exists, long modifiedMillis, long sizeBytes, String contentHash) {
    }
}
