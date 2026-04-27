package com.zcpu.tzzmod.webadmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class WebAdminJsonResponse {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private WebAdminJsonResponse() {
    }

    public static void ok(HttpExchange exchange, Object data) throws IOException {
        send(exchange, 200, Map.of("ok", true, "data", data == null ? Map.of() : data));
    }

    public static void error(HttpExchange exchange, int status, String code, String message) throws IOException {
        send(exchange, status, Map.of(
                "ok", false,
                "error", Map.of("code", code, "message", message)
        ));
    }

    public static void send(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
