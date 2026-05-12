package com.zcpu.tzzmod.webadmin.testbridge;

import com.sun.net.httpserver.HttpExchange;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class WebAdminTestBridgeSecurityService {
    public static final String TOKEN_HEADER = "X-TZZ-TestBridge-Token";
    public static final String ENABLED_ENV = "TZZ_TESTBRIDGE_ENABLED";
    public static final String TOKEN_ENV = "TZZ_TESTBRIDGE_TOKEN";

    public boolean enabled() {
        return isTruthy(System.getenv(ENABLED_ENV));
    }

    public boolean tokenConfigured() {
        return !token().isBlank();
    }

    public AccessResult requireLoopback(HttpExchange exchange) {
        if (!isLoopback(exchange)) {
            return AccessResult.deny(403, "TESTBRIDGE_FORBIDDEN", "TestBridge 只允许 localhost / loopback 调用。");
        }
        return AccessResult.allow();
    }

    public AccessResult requireEnabledAndToken(HttpExchange exchange) {
        AccessResult loopback = requireLoopback(exchange);
        if (!loopback.allowed()) {
            return loopback;
        }
        if (!enabled()) {
            return AccessResult.deny(404, "TESTBRIDGE_DISABLED", "TestBridge 默认关闭。设置 TZZ_TESTBRIDGE_ENABLED=true 后才可用。");
        }
        String expected = token();
        if (expected.isBlank()) {
            return AccessResult.deny(403, "TESTBRIDGE_TOKEN_REQUIRED", "TestBridge 需要配置 TZZ_TESTBRIDGE_TOKEN。");
        }
        String supplied = exchange.getRequestHeaders().getFirst(TOKEN_HEADER);
        if (supplied == null || supplied.isBlank()) {
            return AccessResult.deny(403, "TESTBRIDGE_TOKEN_REQUIRED", "请求缺少 X-TZZ-TestBridge-Token。");
        }
        if (!constantTimeEquals(expected, supplied.trim())) {
            return AccessResult.deny(403, "TESTBRIDGE_TOKEN_INVALID", "TestBridge token 无效。");
        }
        return AccessResult.allow();
    }

    public String sourceIp(HttpExchange exchange) {
        return exchange == null || exchange.getRemoteAddress() == null || exchange.getRemoteAddress().getAddress() == null
                ? "unknown"
                : exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private boolean isLoopback(HttpExchange exchange) {
        if (exchange == null || exchange.getRemoteAddress() == null) {
            return false;
        }
        InetAddress address = exchange.getRemoteAddress().getAddress();
        return address != null && (address.isLoopbackAddress() || address.isAnyLocalAddress());
    }

    private String token() {
        String value = System.getenv(TOKEN_ENV);
        return value == null ? "" : value.trim();
    }

    private static boolean isTruthy(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return value.equals("true") || value.equals("1") || value.equals("yes") || value.equals("on");
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] suppliedBytes = supplied.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, suppliedBytes);
    }

    public record AccessResult(boolean allowed, int status, String code, String message) {
        public static AccessResult allow() {
            return new AccessResult(true, 200, "OK", "");
        }

        public static AccessResult deny(int status, String code, String message) {
            return new AccessResult(false, status, code, message);
        }
    }
}
