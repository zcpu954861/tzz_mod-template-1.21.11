package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SignalEventHistory {
    private static final int MAX_RECORDS = 200;
    private static final Deque<SignalEventRecord> RECORDS = new ArrayDeque<>();

    private SignalEventHistory() {
    }

    public static void record(SignalEventRecord record) {
        if (record == null) {
            return;
        }

        synchronized (SignalEventHistory.class) {
            while (RECORDS.size() >= MAX_RECORDS) {
                RECORDS.removeFirst();
            }
            RECORDS.addLast(record);
        }
        WebAdminRealtimeEventBus.publishSignalHistory(record);
    }

    public static synchronized List<SignalEventRecord> snapshot() {
        return List.copyOf(RECORDS);
    }

    public static synchronized List<SignalEventRecord> snapshot(String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalEventRecord> result = new ArrayList<>();
        for (SignalEventRecord record : RECORDS) {
            if (record != null && SignalChannel.normalize(record.channel()).equals(normalizedChannel)) {
                result.add(record);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized void clear() {
        RECORDS.clear();
    }

    public static synchronized int size() {
        return RECORDS.size();
    }

    public static int maxSize() {
        return MAX_RECORDS;
    }
}
