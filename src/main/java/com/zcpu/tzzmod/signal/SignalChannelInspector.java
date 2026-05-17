package com.zcpu.tzzmod.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;

public final class SignalChannelInspector {
    private SignalChannelInspector() {
    }

    public static List<SignalChannelSummary> getSummaries(MinecraftServer server) {
        LinkedHashSet<String> channels = knownChannels(server);
        List<SignalChannelSummary> summaries = new ArrayList<>();
        for (String channel : channels) {
            summaries.add(getSummary(server, channel));
        }

        summaries.sort((left, right) -> {
            SignalEventRecord leftEvent = left.latestEvent();
            SignalEventRecord rightEvent = right.latestEvent();
            if (leftEvent == null && rightEvent != null) {
                return 1;
            }
            if (leftEvent != null && rightEvent == null) {
                return -1;
            }
            if (leftEvent != null) {
                int timeCompare = Long.compare(rightEvent.wallTimeMillis(), leftEvent.wallTimeMillis());
                if (timeCompare != 0) {
                    return timeCompare;
                }
            }
            return left.channel().compareTo(right.channel());
        });
        return List.copyOf(summaries);
    }

    public static SignalChannelSummary getSummary(MinecraftServer server, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalListenerData> listeners = getListenersForChannel(server, normalizedChannel);
        int enabledCount = 0;
        int actionCount = 0;
        for (SignalListenerData listener : listeners) {
            if (listener.enabled()) {
                enabledCount++;
            }
            actionCount += listener.actions() == null ? 0 : listener.actions().size();
        }

        return new SignalChannelSummary(
                normalizedChannel,
                listeners.size(),
                enabledCount,
                listeners.size() - enabledCount,
                actionCount,
                latestEvent(normalizedChannel)
        );
    }

    public static List<SignalListenerData> getListenersForChannel(MinecraftServer server, String channel) {
        if (server == null) {
            return List.of();
        }

        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalListenerData> result = new ArrayList<>();
        for (SignalListenerData listener : SignalListenerStore.getSnapshot(server)) {
            if (SignalChannel.normalize(listener.channel()).equals(normalizedChannel)) {
                result.add(listener);
            }
        }
        return List.copyOf(result);
    }

    public static List<SignalEventRecord> getRecentEvents(String channel, int limit) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalEventRecord> records = SignalEventHistory.snapshot(normalizedChannel);
        int safeLimit = Math.max(0, limit);
        int startIndex = Math.max(0, records.size() - safeLimit);
        return List.copyOf(records.subList(startIndex, records.size()));
    }

    private static LinkedHashSet<String> knownChannels(MinecraftServer server) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        if (server != null) {
            for (SignalListenerData listener : SignalListenerStore.getSnapshot(server)) {
                String channel = SignalChannel.normalize(listener.channel());
                if (!channel.isBlank()) {
                    channels.add(channel);
                }
            }
            for (SignalJoinDefinition join : SignalJoinStore.getSnapshot(server)) {
                SignalJoinDefinition normalized = join.normalized();
                if (!normalized.outputChannel.isBlank()) {
                    channels.add(normalized.outputChannel);
                }
                for (String input : normalized.inputChannelNames()) {
                    if (!input.isBlank()) {
                        channels.add(input);
                    }
                }
            }
        }

        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            String channel = SignalChannel.normalize(record.channel());
            if (!channel.isBlank()) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private static SignalEventRecord latestEvent(String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        Map<String, SignalEventRecord> latestByChannel = new HashMap<>();
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            String recordChannel = SignalChannel.normalize(record.channel());
            if (recordChannel.isBlank()) {
                continue;
            }
            SignalEventRecord current = latestByChannel.get(recordChannel);
            if (current == null || record.wallTimeMillis() >= current.wallTimeMillis()) {
                latestByChannel.put(recordChannel, record);
            }
        }
        return latestByChannel.get(normalizedChannel);
    }
}
