package com.zcpu.tzzmod.webadmin;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class WebAdminPasswordHasher {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int DEFAULT_ITERATIONS = 160_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public record PasswordHash(String salt, String hash, String algorithm, int iterations) {
    }

    private WebAdminPasswordHasher() {
    }

    public static PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return hash(password, Base64.getEncoder().encodeToString(salt), DEFAULT_ITERATIONS);
    }

    public static PasswordHash hash(String password, String saltBase64, int iterations) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password == null ? new char[0] : password.toCharArray(), salt, iterations, HASH_BITS);
            byte[] hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return new PasswordHash(saltBase64, Base64.getEncoder().encodeToString(hash), ALGORITHM, iterations);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash WebAdmin password", exception);
        }
    }

    public static boolean verify(String password, WebAdminUser user) {
        if (user == null || user.passwordSalt == null || user.passwordHash == null || user.passwordHash.isBlank()) {
            return false;
        }
        PasswordHash candidate = hash(password, user.passwordSalt, Math.max(1, user.passwordIterations));
        return constantTimeEquals(candidate.hash(), user.passwordHash);
    }

    public static String generateInitialPassword() {
        StringBuilder builder = new StringBuilder();
        for (int group = 0; group < 3; group++) {
            if (group > 0) {
                builder.append('-');
            }
            for (int i = 0; i < 4; i++) {
                builder.append(PASSWORD_CHARS[RANDOM.nextInt(PASSWORD_CHARS.length)]);
            }
        }
        return builder.toString();
    }

    public static String randomSessionId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Base64Url(String value) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash WebAdmin session id", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] a = left == null ? new byte[0] : left.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = right == null ? new byte[0] : right.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
