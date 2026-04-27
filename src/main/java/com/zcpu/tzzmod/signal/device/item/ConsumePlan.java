package com.zcpu.tzzmod.signal.device.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConsumePlan {
    @FunctionalInterface
    public interface ConsumeAction {
        void consume(int count);
    }

    public record Entry(String key, int count, String label, ConsumeAction action) {
        public Entry {
            key = key == null ? "" : key;
            count = Math.max(0, count);
            label = label == null || label.isBlank() ? key : label;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Integer> reserved = new HashMap<>();

    public ConsumePlan copy() {
        ConsumePlan copy = new ConsumePlan();
        copy.entries.addAll(entries);
        copy.reserved.putAll(reserved);
        return copy;
    }

    public void replaceWith(ConsumePlan other) {
        entries.clear();
        reserved.clear();
        if (other != null) {
            entries.addAll(other.entries);
            reserved.putAll(other.reserved);
        }
    }

    public void add(String key, int count, String label, ConsumeAction action) {
        if (key == null || key.isBlank() || count <= 0) {
            return;
        }
        Entry entry = new Entry(key, count, label, action);
        entries.add(entry);
        reserved.put(entry.key(), reserved(entry.key()) + entry.count());
    }

    public int reserved(String key) {
        return reserved.getOrDefault(key == null ? "" : key, 0);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int totalCount() {
        int total = 0;
        for (Entry entry : entries) {
            total += entry.count();
        }
        return total;
    }

    public String primarySource() {
        return entries.isEmpty() ? "" : entries.get(0).key();
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public String summary() {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.label()).append(" x").append(entry.count());
        }
        return builder.toString();
    }

    public void apply() {
        for (Entry entry : entries) {
            if (entry.action() != null && entry.count() > 0) {
                entry.action().consume(entry.count());
            }
        }
    }
}
