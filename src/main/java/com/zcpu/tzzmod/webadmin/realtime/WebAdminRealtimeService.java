package com.zcpu.tzzmod.webadmin.realtime;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class WebAdminRealtimeService {
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(20);

    public void handleEventStream(HttpExchange exchange, WebAdminUser user) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/event-stream; charset=utf-8");
        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");
        headers.set("X-Accel-Buffering", "no");
        exchange.sendResponseHeaders(200, 0);

        long lastSeenSeq = lastSeenSeq(exchange);
        WebAdminRealtimeClient client = WebAdminRealtimeEventBus.subscribe(user.username, user.role, lastSeenSeq);
        try (OutputStream output = exchange.getResponseBody()) {
            writeRaw(output, "retry: 3000\n\n");
            writeEvent(output, WebAdminRealtimeEventBus.connectedEvent(user.username, user.role, lastSeenSeq));
            while (!client.closed()) {
                WebAdminRealtimeEvent event = client.poll(HEARTBEAT_INTERVAL);
                writeEvent(output, event == null ? WebAdminRealtimeEventBus.heartbeatEvent() : event);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException exception) {
            Tzz_mod.LOGGER.debug("WebAdmin realtime client disconnected: {}", exception.getMessage());
        } finally {
            WebAdminRealtimeEventBus.unsubscribe(client);
            exchange.close();
        }
    }

    public void closeAll() {
        WebAdminRealtimeEventBus.closeAll();
    }

    private static void writeEvent(OutputStream output, WebAdminRealtimeEvent event) throws IOException {
        if (event == null) {
            return;
        }
        writeRaw(output, "id: " + event.id() + "\n");
        writeRaw(output, "event: " + event.type() + "\n");
        writeRaw(output, "data: " + WebAdminJsonResponse.GSON.toJson(event) + "\n\n");
        output.flush();
    }

    private static long lastSeenSeq(HttpExchange exchange) {
        if (exchange == null) {
            return 0L;
        }
        String header = exchange.getRequestHeaders().getFirst("Last-Event-ID");
        long fromHeader = parseSeq(header);
        if (fromHeader > 0L) {
            return fromHeader;
        }
        return parseSeq(queryParam(exchange.getRequestURI() == null ? "" : exchange.getRequestURI().getRawQuery(), "lastEventId"));
    }

    private static long parseSeq(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String queryParam(String query, String name) {
        if (query == null || query.isBlank() || name == null || name.isBlank()) {
            return "";
        }
        for (String pair : query.split("&")) {
            int split = pair.indexOf('=');
            String key = split >= 0 ? pair.substring(0, split) : pair;
            if (!name.equals(decode(key))) {
                continue;
            }
            return decode(split >= 0 ? pair.substring(split + 1) : "");
        }
        return "";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static void writeRaw(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }
}
