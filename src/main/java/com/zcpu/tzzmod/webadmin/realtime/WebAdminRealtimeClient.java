package com.zcpu.tzzmod.webadmin.realtime;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public final class WebAdminRealtimeClient {
    public static final int MAX_QUEUE_SIZE = 128;

    private final String id = UUID.randomUUID().toString();
    private final String username;
    private final String role;
    private final long connectedAtMillis = System.currentTimeMillis();
    private final LinkedBlockingDeque<WebAdminRealtimeEvent> queue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
    private volatile boolean closed;

    WebAdminRealtimeClient(String username, String role) {
        this.username = username == null ? "" : username;
        this.role = role == null ? "" : role;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String role() {
        return role;
    }

    public long connectedAtMillis() {
        return connectedAtMillis;
    }

    public boolean closed() {
        return closed;
    }

    public void close() {
        closed = true;
        queue.clear();
    }

    public boolean offer(WebAdminRealtimeEvent event) {
        if (closed || event == null) {
            return false;
        }
        if (queue.offerLast(event)) {
            return true;
        }
        queue.pollFirst();
        return queue.offerLast(event);
    }

    public WebAdminRealtimeEvent poll(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        return queue.pollFirst(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS);
    }
}
