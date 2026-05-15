package com.zcpu.tzzmod.condition.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ConditionInventorySnapshot(List<ConditionItemStackSnapshot> slots) {
    public ConditionInventorySnapshot {
        slots = copySlots(slots);
    }

    public static ConditionInventorySnapshot empty() {
        return new ConditionInventorySnapshot(List.of());
    }

    public int size() {
        return slots.size();
    }

    public Optional<ConditionItemStackSnapshot> slot(int index) {
        if (index < 0 || index >= slots.size()) {
            return Optional.empty();
        }
        return Optional.of(slots.get(index));
    }

    public int matchingCount(ConditionItemMatchConfig matcher) {
        int count = 0;
        for (ConditionItemStackSnapshot slot : slots) {
            if (ConditionItemMatcher.sameItem(slot, matcher.itemId())) {
                count += slot.count();
            }
        }
        return count;
    }

    private static List<ConditionItemStackSnapshot> copySlots(List<ConditionItemStackSnapshot> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        ArrayList<ConditionItemStackSnapshot> copy = new ArrayList<>(raw.size());
        for (ConditionItemStackSnapshot slot : raw) {
            copy.add(slot == null ? ConditionItemStackSnapshot.empty() : slot);
        }
        return List.copyOf(copy);
    }
}
