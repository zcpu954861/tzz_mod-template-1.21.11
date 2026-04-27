package com.zcpu.tzzmod.signal.device.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class ItemSubmitInventoryAdapter {
    private ItemSubmitInventoryAdapter() {
    }

    public record View(
            List<ItemSubmitEvaluator.SourceStack> sourceStacks,
            ItemSubmitEvaluator.Matcher matcher
    ) {
        public View {
            sourceStacks = sourceStacks == null ? List.of() : List.copyOf(sourceStacks);
        }
    }

    public static View fromMainStacks(List<ItemStack> stacks, String order) {
        if (stacks == null || stacks.isEmpty()) {
            return new View(List.of(), (source, matcher) -> false);
        }

        Map<String, ItemStack> stacksByKey = new HashMap<>();
        List<ItemSubmitEvaluator.SourceStack> sources = new ArrayList<>();
        for (int slot : ConsumePlanner.inventorySlotOrder(stacks.size(), order)) {
            ItemStack stack = stacks.get(slot);
            String key = "inv:" + slot;
            stacksByKey.put(key, stack);
            sources.add(new ItemSubmitEvaluator.SourceStack(
                    key,
                    itemId(stack),
                    stack == null || stack.isEmpty() ? 0 : stack.getCount(),
                    "submit:slot" + slot,
                    amount -> {
                        if (stack != null && !stack.isEmpty()) {
                            stack.decrement(amount);
                        }
                    }
            ));
        }

        return new View(
                sources,
                (source, matcher) -> ItemStackMatcher.matchesIgnoringCount(stacksByKey.get(source.key()), matcher)
        );
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
