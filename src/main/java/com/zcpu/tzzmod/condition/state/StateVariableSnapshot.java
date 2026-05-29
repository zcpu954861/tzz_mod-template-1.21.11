package com.zcpu.tzzmod.condition.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record StateVariableSnapshot(List<StateVariableRecord> records) {
    public StateVariableSnapshot {
        Map<String, StateVariableRecord> byId = new LinkedHashMap<>();
        if (records != null) {
            for (StateVariableRecord record : records) {
                if (record == null) {
                    continue;
                }
                try {
                    StateVariableRecord normalized = StateVariableRecord.create(
                            record.scope(),
                            record.targetId(),
                            record.key(),
                            record.type(),
                            record.value(),
                            record.displayName(),
                            record.note(),
                            record.updatedAt(),
                            record.updatedBy(),
                            record.version()
                    );
                    byId.put(normalized.id(), normalized);
                } catch (RuntimeException ignored) {
                    // Corrupt records are dropped so one bad variable cannot break condition evaluation.
                }
            }
        }
        records = byId.values().stream()
                .sorted(Comparator.comparing(StateVariableRecord::id))
                .toList();
    }

    public static StateVariableSnapshot empty() {
        return new StateVariableSnapshot(List.of());
    }

    public Optional<StateVariableRecord> get(StateVariableScope scope, String targetId, String key) {
        StateVariableKey variableKey = new StateVariableKey(scope, targetId, key);
        String id = variableKey.stableId();
        int low = 0;
        int high = records.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            StateVariableRecord record = records.get(mid);
            int compared = record.id().compareTo(id);
            if (compared == 0) {
                return Optional.of(record);
            }
            if (compared < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return Optional.empty();
    }

    public StateVariableSnapshot with(StateVariableRecord record) {
        Map<String, StateVariableRecord> byId = asMap();
        StateVariableRecord normalized = StateVariableRecord.create(
                record.scope(),
                record.targetId(),
                record.key(),
                record.type(),
                record.value(),
                record.displayName(),
                record.note(),
                record.updatedAt(),
                record.updatedBy(),
                record.version()
        );
        byId.put(normalized.id(), normalized);
        return new StateVariableSnapshot(new ArrayList<>(byId.values()));
    }

    public StateVariableSnapshot without(StateVariableScope scope, String targetId, String key) {
        Map<String, StateVariableRecord> byId = asMap();
        byId.remove(new StateVariableKey(scope, targetId, key).stableId());
        return new StateVariableSnapshot(new ArrayList<>(byId.values()));
    }

    public int size() {
        return records.size();
    }

    public String summary() {
        return records.size() + " 个状态变量";
    }

    private Map<String, StateVariableRecord> asMap() {
        Map<String, StateVariableRecord> byId = new LinkedHashMap<>();
        for (StateVariableRecord record : records) {
            byId.put(record.id(), record);
        }
        return byId;
    }
}
