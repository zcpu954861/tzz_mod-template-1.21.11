package com.zcpu.tzzmod.webadmin.realtime;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import java.io.IOException;
import java.io.OutputStream;
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

        WebAdminRealtimeClient client = WebAdminRealtimeEventBus.subscribe(user.username, user.role);
        try (OutputStream output = exchange.getResponseBody()) {
            writeRaw(output, "retry: 3000\n\n");
            writeEvent(output, WebAdminRealtimeEventBus.connectedEvent(user.username, user.role));
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

    private static void writeRaw(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }
}
