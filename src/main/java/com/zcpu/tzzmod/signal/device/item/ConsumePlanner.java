package com.zcpu.tzzmod.signal.device.item;

import java.util.ArrayList;
import java.util.List;

public final class ConsumePlanner {
    public static final String DEFAULT_FAILURE = "not_enough_items_to_consume";

    private ConsumePlanner() {
    }

    public record ConsumableStack(
            String key,
            int count,
            String label,
            ConsumePlan.ConsumeAction action
    ) {
        public ConsumableStack {
            key = key == null ? "" : key;
            count = Math.max(0, count);
            label = label == null || label.isBlank() ? key : label;
        }
    }

    public static String stageSingle(
            ConsumePlan plan,
            String key,
            int availableCount,
            int count,
            String label,
            ConsumePlan.ConsumeAction action
    ) {
        if (plan == null || key == null || key.isBlank() || count <= 0) {
            return DEFAULT_FAILURE;
        }
        ConsumePlan staged = plan.copy();
        int available = Math.max(0, availableCount) - staged.reserved(key);
        if (available < count) {
            return DEFAULT_FAILURE;
        }
        staged.add(key, count, label, action);
        plan.replaceWith(staged);
        return "";
    }

    public static String stageAcrossStacks(ConsumePlan plan, List<ConsumableStack> stacks, int count) {
        if (plan == null || count <= 0 || stacks == null || stacks.isEmpty()) {
            return DEFAULT_FAILURE;
        }
        ConsumePlan staged = plan.copy();
        int remaining = count;
        for (ConsumableStack stack : stacks) {
            if (stack == null || stack.key().isBlank() || stack.count() <= 0) {
                continue;
            }
            int available = stack.count() - staged.reserved(stack.key());
            if (available <= 0) {
                continue;
            }
            int take = Math.min(available, remaining);
            staged.add(stack.key(), take, stack.label(), stack.action());
            remaining -= take;
            if (remaining <= 0) {
                plan.replaceWith(staged);
                return "";
            }
        }
        return DEFAULT_FAILURE;
    }

    public static List<Integer> inventorySlotOrder(int size, String rawOrder) {
        List<Integer> slots = new ArrayList<>(Math.max(0, size));
        String order = InventoryConsumeOrder.normalize(rawOrder);
        if (InventoryConsumeOrder.MAIN_INVENTORY_FIRST.equals(order)) {
            for (int i = 9; i < size; i++) {
                slots.add(i);
            }
            for (int i = 0; i < Math.min(9, size); i++) {
                slots.add(i);
            }
            return slots;
        }
        for (int i = 0; i < Math.min(9, size); i++) {
            slots.add(i);
        }
        for (int i = 9; i < size; i++) {
            slots.add(i);
        }
        return slots;
    }
}
