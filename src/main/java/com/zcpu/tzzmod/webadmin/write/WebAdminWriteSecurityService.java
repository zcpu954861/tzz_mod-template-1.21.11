package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminPasswordHasher;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WebAdminWriteSecurityService {
    private final Map<String, String> csrfTokensBySessionHash = new ConcurrentHashMap<>();

    public String csrfTokenFor(WebAdminSession session) {
        if (session == null || session.sessionIdHash == null || session.sessionIdHash.isBlank()) {
            return "";
        }
        return csrfTokensBySessionHash.computeIfAbsent(session.sessionIdHash, ignored -> WebAdminPasswordHasher.randomSessionId());
    }

    public WebAdminWriteResult requireValidCsrf(WebAdminSession session, String token) {
        if (session == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.UNAUTHENTICATED, WebAdminWriteTarget.none(), "");
        }
        if (token == null || token.isBlank()) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_REQUIRED, WebAdminWriteTarget.none(), "");
        }
        if (!isValidCsrf(session, token)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, WebAdminWriteTarget.none(), "");
        }
        return WebAdminWriteResult.ok(WebAdminWriteTarget.none(), false, "CSRF 校验通过。");
    }

    public boolean isValidCsrf(WebAdminSession session, String token) {
        if (session == null || token == null || token.isBlank()) {
            return false;
        }
        String expected = csrfTokensBySessionHash.get(session.sessionIdHash);
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public boolean isSameOrigin(String origin, String expectedHost, int expectedPort) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        String expectedHttp = "http://" + expectedHost + ":" + expectedPort;
        String expectedHttps = "https://" + expectedHost + ":" + expectedPort;
        return origin.equalsIgnoreCase(expectedHttp) || origin.equalsIgnoreCase(expectedHttps);
    }

    public boolean isSameOriginOrReferer(String origin, String referer, String expectedHost, int expectedPort) {
        if (isSameOrigin(origin, expectedHost, expectedPort)) {
            return true;
        }
        if (referer == null || referer.isBlank()) {
            return false;
        }
        String expectedHttp = "http://" + expectedHost + ":" + expectedPort;
        String expectedHttps = "https://" + expectedHost + ":" + expectedPort;
        return matchesRefererOrigin(referer, expectedHttp) || matchesRefererOrigin(referer, expectedHttps);
    }

    private static boolean matchesRefererOrigin(String referer, String expectedOrigin) {
        return referer.equalsIgnoreCase(expectedOrigin)
                || referer.regionMatches(true, 0, expectedOrigin + "/", 0, expectedOrigin.length() + 1)
                || referer.regionMatches(true, 0, expectedOrigin + "?", 0, expectedOrigin.length() + 1);
    }

    public void clear() {
        csrfTokensBySessionHash.clear();
    }
}
