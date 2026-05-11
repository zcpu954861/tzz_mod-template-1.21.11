package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ContainerItemConditionSupport {
    private ContainerItemConditionSupport() {
    }

    public static Inventory inventory(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Inventory inventory ? inventory : null;
    }

    public static boolean itemExists(String rawItemId) {
        Identifier id = Identifier.tryParse(rawItemId == null ? "" : rawItemId.trim());
        return id != null && Registries.ITEM.containsId(id);
    }

    public static String normalizeItemId(String rawItemId) {
        Identifier id = Identifier.tryParse(rawItemId == null ? "" : rawItemId.trim());
        return id == null ? "" : id.toString();
    }

    public static boolean isSlotInRange(Inventory inventory, int slot) {
        return inventory != null && slot >= 0 && slot < inventory.size();
    }

    public static boolean matches(Inventory inventory, ContainerItemConditionData rawCondition) {
        if (inventory == null || rawCondition == null) {
            return false;
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        ContainerItemConditionType type = ContainerItemConditionType.fromId(condition.type());
        return switch (type) {
            case SLOT_EMPTY -> matchesSlotEmpty(inventory, condition.slot());
            case SLOT_ITEM -> matchesSlotItem(inventory, condition);
            case TOTAL_ITEM -> matchesTotalItem(inventory, condition);
            case SLOT_MATCHER -> matchesSlotMatcher(inventory, condition);
            case TOTAL_MATCHER -> matchesTotalMatcher(inventory, condition);
        };
    }

    public static List<String> validate(Inventory inventory, ContainerItemConditionData rawCondition) {
        return validate(inventory, rawCondition, "");
    }

    public static List<String> validate(Inventory inventory, ContainerItemConditionData rawCondition, String inheritedContainerChangeChannel) {
        List<String> issues = new ArrayList<>();
        if (rawCondition == null) {
            issues.add("物品条件为空。");
            return issues;
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        if (condition.name().isBlank()) {
            issues.add("物品条件名称为空。");
        }
        if (effectiveChannel(condition, inheritedContainerChangeChannel, false).isBlank()) {
            issues.add("物品条件频道为空，且父 VBD 未配置容器内容变化频道。");
        }
        ContainerItemConditionType type = ContainerItemConditionType.fromId(condition.type());
        if ((type == ContainerItemConditionType.SLOT_EMPTY
                || type == ContainerItemConditionType.SLOT_ITEM
                || type == ContainerItemConditionType.SLOT_MATCHER)
                && !isSlotInRange(inventory, condition.slot())) {
            issues.add("槽位 " + condition.slot() + " 超出当前容器范围。");
        }
        if ((type == ContainerItemConditionType.SLOT_ITEM || type == ContainerItemConditionType.TOTAL_ITEM)
                && !itemExists(condition.itemId())) {
            issues.add("物品 ID 无效：" + condition.itemId());
        }
        if ((type == ContainerItemConditionType.SLOT_ITEM || type == ContainerItemConditionType.TOTAL_ITEM)
                && condition.count() < 1) {
            issues.add("物品数量必须大于等于 1。");
        }
        if ((type == ContainerItemConditionType.SLOT_MATCHER || type == ContainerItemConditionType.TOTAL_MATCHER)
                && (condition.matcher() == null || !condition.matcher().normalized().enabled())) {
            issues.add("ItemStack matcher 模板未配置。");
        }
        return issues;
    }

    public static String effectiveChannel(ContainerItemConditionData rawCondition, String inheritedContainerChangeChannel, boolean exiting) {
        String inherited = SignalChannel.normalize(inheritedContainerChangeChannel);
        if (rawCondition == null) {
            return inherited;
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        if (exiting && !condition.offChannel().isBlank()) {
            return condition.offChannel();
        }
        if (!condition.channel().isBlank()) {
            return condition.channel();
        }
        return inherited;
    }

    public static String effectiveChannelSource(ContainerItemConditionData rawCondition, String inheritedContainerChangeChannel, boolean exiting) {
        String inherited = SignalChannel.normalize(inheritedContainerChangeChannel);
        if (rawCondition == null) {
            return inherited.isBlank() ? "missing" : "container_change_channel";
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        if (exiting && !condition.offChannel().isBlank()) {
            return "condition_off_channel";
        }
        if (!condition.channel().isBlank()) {
            return "condition_channel";
        }
        return inherited.isBlank() ? "missing" : "container_change_channel";
    }

    public static Map<String, Integer> totalCounts(Inventory inventory, List<ContainerItemConditionData> conditions) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        if (inventory == null || conditions == null || conditions.isEmpty()) {
            return targets;
        }
        for (ContainerItemConditionData rawCondition : conditions) {
            if (rawCondition == null) {
                continue;
            }
            ContainerItemConditionData condition = rawCondition.normalized();
            if (condition.enabled()
                    && ContainerItemConditionType.TOTAL_ITEM.id().equals(condition.type())
                    && itemExists(condition.itemId())) {
                targets.putIfAbsent(condition.itemId(), 0);
            }
        }
        if (targets.isEmpty()) {
            return targets;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (targets.containsKey(itemId)) {
                targets.put(itemId, targets.get(itemId) + stack.getCount());
            }
        }
        return targets;
    }

    public static boolean matchesWithTotals(
            Inventory inventory,
            ContainerItemConditionData rawCondition,
            Map<String, Integer> totalCounts
    ) {
        if (inventory == null || rawCondition == null) {
            return false;
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        if (!ContainerItemConditionType.TOTAL_ITEM.id().equals(condition.type())) {
            return matches(inventory, condition);
        }
        int actual = totalCounts == null ? totalItemCount(inventory, condition.itemId()) : totalCounts.getOrDefault(condition.itemId(), 0);
        return ContainerItemCountMode.fromId(condition.countMode()).matches(actual, condition.count());
    }

    public static String summary(ContainerItemConditionData rawCondition) {
        if (rawCondition == null) {
            return "无效条件";
        }
        ContainerItemConditionData condition = rawCondition.normalized();
        ContainerItemConditionType type = ContainerItemConditionType.fromId(condition.type());
        return switch (type) {
            case SLOT_EMPTY -> "slot_empty slot=" + condition.slot();
            case SLOT_ITEM -> "slot_item slot=" + condition.slot()
                    + " item=" + condition.itemId()
                    + " " + condition.countMode()
                    + " " + condition.count();
            case TOTAL_ITEM -> "total_item item=" + condition.itemId()
                    + " " + condition.countMode()
                    + " " + condition.count();
            case SLOT_MATCHER -> "slot_matcher slot=" + condition.slot()
                    + " " + ItemStackMatcherSupport.summary(condition.matcher());
            case TOTAL_MATCHER -> "total_matcher "
                    + ItemStackMatcherSupport.summary(condition.matcher());
        };
    }

    private static boolean matchesSlotEmpty(Inventory inventory, int slot) {
        return isSlotInRange(inventory, slot) && inventory.getStack(slot).isEmpty();
    }

    private static boolean matchesSlotItem(Inventory inventory, ContainerItemConditionData condition) {
        if (!isSlotInRange(inventory, condition.slot()) || !itemExists(condition.itemId())) {
            return false;
        }
        ItemStack stack = inventory.getStack(condition.slot());
        if (stack.isEmpty()) {
            return false;
        }
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        return itemId.equals(condition.itemId())
                && ContainerItemCountMode.fromId(condition.countMode()).matches(stack.getCount(), condition.count());
    }

    private static boolean matchesTotalItem(Inventory inventory, ContainerItemConditionData condition) {
        if (!itemExists(condition.itemId())) {
            return false;
        }
        return ContainerItemCountMode.fromId(condition.countMode())
                .matches(totalItemCount(inventory, condition.itemId()), condition.count());
    }

    private static boolean matchesSlotMatcher(Inventory inventory, ContainerItemConditionData condition) {
        return isSlotInRange(inventory, condition.slot())
                && ItemStackMatcher.matches(inventory.getStack(condition.slot()), condition.matcher());
    }

    private static boolean matchesTotalMatcher(Inventory inventory, ContainerItemConditionData condition) {
        int total = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && ItemStackMatcher.matchesIgnoringCount(stack, condition.matcher())) {
                total += stack.getCount();
            }
        }
        return ContainerItemCountMode.fromId(condition.matcher().countMode())
                .matches(total, condition.matcher().requiredCount());
    }

    private static int totalItemCount(Inventory inventory, String itemId) {
        int total = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
