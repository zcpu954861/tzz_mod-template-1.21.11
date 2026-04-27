package com.zcpu.tzzmod.webadmin;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WebAdminSessionService {
    public static final String COOKIE_NAME = "TZZ_WEBADMIN_SESSION";
    private final Map<String, WebAdminSession> sessionsByHash = new LinkedHashMap<>();

    public synchronized CreatedSession create(WebAdminUser user, int ttlSeconds, String sourceIp, String userAgent) {
        cleanupExpired();
        String token = WebAdminPasswordHasher.randomSessionId();
        String hash = WebAdminPasswordHasher.sha256Base64Url(token);
        long now = System.currentTimeMillis();
        WebAdminSession session = new WebAdminSession(
                hash,
                user.username,
                user.role,
                now,
                now + Math.max(1, ttlSeconds) * 1_000L,
                sourceIp,
                userAgent
        );
        sessionsByHash.put(hash, session);
        return new CreatedSession(token, session);
    }

    public synchronized Optional<WebAdminSession> get(String token) {
        cleanupExpired();
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        WebAdminSession session = sessionsByHash.get(WebAdminPasswordHasher.sha256Base64Url(token));
        if (session == null || session.expiresAt <= System.currentTimeMillis()) {
            return Optional.empty();
        }
        session.lastSeenAt = System.currentTimeMillis();
        return Optional.of(session);
    }

    public synchronized void invalidate(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessionsByHash.remove(WebAdminPasswordHasher.sha256Base64Url(token));
    }

    public synchronized int sessionCount() {
        cleanupExpired();
        return sessionsByHash.size();
    }

    public synchronized void clear() {
        sessionsByHash.clear();
    }

    public static String formatInstant(long millis) {
        return Instant.ofEpochMilli(millis).toString();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, WebAdminSession>> iterator = sessionsByHash.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt <= now) {
                iterator.remove();
            }
        }
    }

    public record CreatedSession(String token, WebAdminSession session) {
    }
}
