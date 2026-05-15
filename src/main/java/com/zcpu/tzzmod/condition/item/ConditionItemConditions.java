package com.zcpu.tzzmod.condition.item;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationResult;
import com.zcpu.tzzmod.condition.ConditionFieldSchema;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.ConditionRegistry;
import com.zcpu.tzzmod.condition.ConditionTypeHandler;
import com.zcpu.tzzmod.condition.ConditionTypeMetadata;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConditionItemConditions {
    private static final String CATEGORY_ITEM = "物品条件";
    private static final String CATEGORY_INVENTORY = "背包条件";
    private static final String CATEGORY_CONTAINER = "容器条件";

    private ConditionItemConditions() {
    }

    public static void register(ConditionRegistry registry) {
        if (registry == null) {
            return;
        }
        registry.register(new ItemStackExistsHandler());
        registry.register(new ItemStackMatchesHandler());
        registry.register(new InventoryContainsItemHandler());
        registry.register(new InventoryItemCountCompareHandler());
        registry.register(new ContainerSlotEmptyHandler());
        registry.register(new ContainerSlotItemMatchesHandler());
        registry.register(new ContainerItemCountCompareHandler());
    }

    private static ConditionTypeMetadata metadata(
            String type,
            String displayName,
            String description,
            String category,
            ConditionFieldSchema... fields
    ) {
        return new ConditionTypeMetadata(type, displayName, description, category, List.of(fields));
    }

    private static ConditionFieldSchema field(String name, String displayName, String kind, boolean required, String description) {
        return new ConditionFieldSchema(name, displayName, kind, required, description);
    }

    private static ConditionFieldSchema itemKeyField() {
        return field("itemKey", "物品快照键", "string", true, "从 EvaluationContext 读取的物品快照 key");
    }

    private static ConditionFieldSchema inventoryKeyField() {
        return field("inventoryKey", "背包快照键", "string", true, "从 EvaluationContext 读取的背包快照 key");
    }

    private static ConditionFieldSchema containerKeyField() {
        return field("containerKey", "容器快照键", "string", true, "从 EvaluationContext 读取的容器快照 key");
    }

    private static ConditionFieldSchema itemIdField() {
        return field("itemId", "物品 ID", "item-id", true, "例如 minecraft:diamond");
    }

    private static ConditionFieldSchema countOperatorField(String name) {
        return field(name, "数量比较方式", "enum:eq,ne,gt,gte,lt,lte", true, "eq/ne/gt/gte/lt/lte");
    }

    private static ConditionFieldSchema countField() {
        return field("count", "目标数量", "integer", true, "用于比较的目标数量，必须大于等于 0");
    }

    private static ConditionFieldSchema slotField() {
        return field("slot", "槽位", "integer", true, "0-based 槽位索引");
    }

    private static String config(ConditionNode node, String key) {
        return node == null || node.config() == null ? "" : node.config().get(key);
    }

    private static boolean hasConfigKey(ConditionNode node, String key) {
        return node != null && node.config() != null && node.config().values().containsKey(key);
    }

    private static ConditionValidationResult requireNonBlank(ConditionNode node, String key, String displayName) {
        if (node == null || !node.config().has(key)) {
            return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_" + key, "条件缺少必填字段：" + displayName);
        }
        return ConditionValidationResult.ok();
    }

    private static ConditionValidationResult validateSnapshotKey(ConditionNode node, String key, String displayName) {
        return requireNonBlank(node, key, displayName);
    }

    private static ConditionValidationResult validateSlot(ConditionNode node) {
        ConditionValidationResult result = requireNonBlank(node, "slot", "槽位");
        String raw = config(node, "slot");
        if (!raw.isBlank()) {
            try {
                int slot = Integer.parseInt(raw);
                if (slot < 0) {
                    result = result.merge(ConditionValidationResult.error(node.id(), "", "condition_config_invalid_slot", "槽位必须是 0-based 非负整数"));
                }
            } catch (NumberFormatException exception) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_slot", "槽位必须是 0-based 非负整数"));
            }
        }
        return result;
    }

    private static ConditionValidationResult validateMatcher(ConditionNode node, String operatorKey) {
        ConditionValidationResult result = ConditionValidationResult.ok();
        boolean hasAnyMatcherField = hasConfigKey(node, "itemId") || hasConfigKey(node, operatorKey) || hasConfigKey(node, "count");
        if (!hasAnyMatcherField) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_empty_matcher", "物品匹配器为空：至少需要物品 ID、数量比较方式和目标数量"));
            return result;
        }
        result = result.merge(requireNonBlank(node, "itemId", "物品 ID"))
                .merge(requireNonBlank(node, operatorKey, "数量比较方式"))
                .merge(requireNonBlank(node, "count", "目标数量"));
        String itemId = config(node, "itemId");
        if (!itemId.isBlank() && !ConditionItemMatcher.validItemId(itemId)) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_item_id", "物品 ID 必须是命名空间格式，且不能是 minecraft:air"));
        }
        String operator = config(node, operatorKey);
        if (!operator.isBlank() && ConditionItemCompareOperator.parse(operator).isEmpty()) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_operator", "数量比较方式必须是 eq/ne/gt/gte/lt/lte"));
        }
        String count = config(node, "count");
        if (!count.isBlank()) {
            try {
                int parsed = Integer.parseInt(count);
                if (parsed < 0) {
                    result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_count", "目标数量必须是大于等于 0 的整数"));
                }
            } catch (NumberFormatException exception) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_count", "目标数量必须是大于等于 0 的整数"));
            }
        }
        return result;
    }

    private static ConditionItemMatchConfig matcher(ConditionNode node, String operatorKey) {
        return new ConditionItemMatchConfig(
                config(node, "itemId"),
                ConditionItemCompareOperator.parse(config(node, operatorKey)).orElse(ConditionItemCompareOperator.GTE),
                parseInt(config(node, "count"), 0)
        );
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String label(ConditionNode node, String displayName) {
        return node != null && !node.name().isBlank() ? node.name() : displayName;
    }

    private static ConditionEvaluationResult leaf(
            ConditionNode node,
            ConditionEvaluationContext context,
            boolean matched,
            String code,
            String message,
            String displayName
    ) {
        return ConditionEvaluationResult.leaf(node, context, matched, code, message, label(node, displayName));
    }

    private static ConditionEvaluationResult missingSnapshot(
            ConditionNode node,
            ConditionEvaluationContext context,
            String key,
            String expectedType,
            String displayName
    ) {
        if (context == null) {
            return leaf(node, context, false, "condition_context_missing", "上下文不存在，无法读取" + expectedType + "：" + key + "。", displayName);
        }
        String actualType = context.snapshotType(key);
        if (!actualType.isBlank()) {
            return leaf(node, context, false, "condition_snapshot_type_mismatch", "快照类型不匹配：" + key + " 期望 " + expectedType + "，实际 " + actualType + "。", displayName);
        }
        return leaf(node, context, false, "condition_snapshot_missing", "上下文缺少" + expectedType + "：" + key + "。", displayName);
    }

    private static String emptyItemReason(String key, ConditionItemStackSnapshot snapshot) {
        if (snapshot == null || snapshot.itemId().isBlank()) {
            return "物品快照为空：" + key + "。";
        }
        if ("minecraft:air".equals(snapshot.itemId())) {
            return "物品快照为空物品：" + key + " 为 minecraft:air。";
        }
        if (snapshot.count() <= 0) {
            return "物品快照为空：" + key + " 数量为 " + snapshot.count() + "。";
        }
        return "物品快照为空：" + key + "。";
    }

    private record ItemStackExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.ITEM_STACK_EXISTS,
                    "物品快照存在",
                    "检查指定物品快照存在且不是空物品。",
                    CATEGORY_ITEM,
                    itemKeyField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "itemKey", "物品快照键");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "itemKey");
            Optional<ConditionItemStackSnapshot> snapshot = context == null ? Optional.empty() : context.itemSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "物品快照", "物品快照存在");
            }
            boolean matched = !snapshot.get().isEmpty();
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "item_stack_exists" : "item_stack_empty",
                    matched ? "物品快照存在：" + key + " = " + snapshot.get().summary() + "。" : emptyItemReason(key, snapshot.get()),
                    "物品快照存在"
            );
        }
    }

    private record ItemStackMatchesHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.ITEM_STACK_MATCHES,
                    "物品快照匹配",
                    "检查指定物品快照是否匹配物品 ID 和数量条件。",
                    CATEGORY_ITEM,
                    itemKeyField(),
                    itemIdField(),
                    countOperatorField("countOperator"),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "itemKey", "物品快照键")
                    .merge(validateMatcher(node, "countOperator"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "itemKey");
            Optional<ConditionItemStackSnapshot> snapshot = context == null ? Optional.empty() : context.itemSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "物品快照", "物品快照匹配");
            }
            if (snapshot.get().isEmpty()) {
                return leaf(node, context, false, "item_stack_empty", emptyItemReason(key, snapshot.get()), "物品快照匹配");
            }
            ConditionItemMatchResult result = ConditionItemMatcher.matchStack(snapshot.get(), matcher(node, "countOperator"));
            return leaf(node, context, result.matched(), result.reasonCode(), result.message(), "物品快照匹配");
        }
    }

    private record InventoryContainsItemHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.INVENTORY_CONTAINS_ITEM,
                    "背包包含物品",
                    "跨背包多个槽位统计目标物品数量，并检查是否满足数量条件。",
                    CATEGORY_INVENTORY,
                    inventoryKeyField(),
                    itemIdField(),
                    countOperatorField("countOperator"),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "inventoryKey", "背包快照键")
                    .merge(validateMatcher(node, "countOperator"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "inventoryKey");
            Optional<ConditionInventorySnapshot> snapshot = context == null ? Optional.empty() : context.inventorySnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "背包快照", "背包包含物品");
            }
            ConditionItemMatchConfig matcher = matcher(node, "countOperator");
            int actual = snapshot.get().matchingCount(matcher);
            ConditionItemMatchResult result = ConditionItemMatcher.matchAggregateCount(actual, matcher, "背包物品数量");
            return leaf(node, context, result.matched(), result.reasonCode(), result.message(), "背包包含物品");
        }
    }

    private record InventoryItemCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE,
                    "背包物品数量比较",
                    "跨背包多个槽位统计目标物品总数，并按比较方式判断。",
                    CATEGORY_INVENTORY,
                    inventoryKeyField(),
                    itemIdField(),
                    countOperatorField("operator"),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "inventoryKey", "背包快照键")
                    .merge(validateMatcher(node, "operator"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "inventoryKey");
            Optional<ConditionInventorySnapshot> snapshot = context == null ? Optional.empty() : context.inventorySnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "背包快照", "背包物品数量比较");
            }
            ConditionItemMatchConfig matcher = matcher(node, "operator");
            int actual = snapshot.get().matchingCount(matcher);
            ConditionItemMatchResult result = ConditionItemMatcher.matchAggregateCount(actual, matcher, "背包物品数量");
            return leaf(node, context, result.matched(), result.reasonCode(), result.message(), "背包物品数量比较");
        }
    }

    private record ContainerSlotEmptyHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.CONTAINER_SLOT_EMPTY,
                    "容器槽位为空",
                    "检查容器快照中的 0-based 指定槽位是否为空。",
                    CATEGORY_CONTAINER,
                    containerKeyField(),
                    slotField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "containerKey", "容器快照键").merge(validateSlot(node));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "containerKey");
            Optional<ConditionContainerSnapshot> snapshot = context == null ? Optional.empty() : context.containerSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "容器快照", "容器槽位为空");
            }
            int slot = parseInt(config(node, "slot"), -1);
            if (slot < 0 || slot >= snapshot.get().size()) {
                return leaf(node, context, false, "container_slot_out_of_range", "容器槽位越界：slot=" + slot + "，槽位数=" + snapshot.get().size() + "。", "容器槽位为空");
            }
            ConditionItemStackSnapshot item = snapshot.get().slot(slot).orElse(ConditionItemStackSnapshot.empty());
            boolean matched = item.isEmpty();
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "container_slot_empty" : "container_slot_not_empty",
                    matched ? "容器槽位为空：slot=" + slot + "。" : "容器槽位不为空：slot=" + slot + "，当前 " + item.summary() + "。",
                    "容器槽位为空"
            );
        }
    }

    private record ContainerSlotItemMatchesHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                    "容器槽位物品匹配",
                    "检查容器快照中 0-based 指定槽位的物品是否匹配。",
                    CATEGORY_CONTAINER,
                    containerKeyField(),
                    slotField(),
                    itemIdField(),
                    countOperatorField("countOperator"),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "containerKey", "容器快照键")
                    .merge(validateSlot(node))
                    .merge(validateMatcher(node, "countOperator"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "containerKey");
            Optional<ConditionContainerSnapshot> snapshot = context == null ? Optional.empty() : context.containerSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "容器快照", "容器槽位物品匹配");
            }
            int slot = parseInt(config(node, "slot"), -1);
            if (slot < 0 || slot >= snapshot.get().size()) {
                return leaf(node, context, false, "container_slot_out_of_range", "容器槽位越界：slot=" + slot + "，槽位数=" + snapshot.get().size() + "。", "容器槽位物品匹配");
            }
            ConditionItemStackSnapshot item = snapshot.get().slot(slot).orElse(ConditionItemStackSnapshot.empty());
            ConditionItemMatchResult result = ConditionItemMatcher.matchStack(item, matcher(node, "countOperator"));
            return leaf(node, context, result.matched(), result.reasonCode(), result.message(), "容器槽位物品匹配");
        }
    }

    private record ContainerItemCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionItemConditions.metadata(
                    ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE,
                    "容器物品数量比较",
                    "跨容器多个槽位统计目标物品总数，并按比较方式判断。",
                    CATEGORY_CONTAINER,
                    containerKeyField(),
                    itemIdField(),
                    countOperatorField("operator"),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateSnapshotKey(node, "containerKey", "容器快照键")
                    .merge(validateMatcher(node, "operator"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "containerKey");
            Optional<ConditionContainerSnapshot> snapshot = context == null ? Optional.empty() : context.containerSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "容器快照", "容器物品数量比较");
            }
            ConditionItemMatchConfig matcher = matcher(node, "operator");
            int actual = snapshot.get().matchingCount(matcher);
            ConditionItemMatchResult result = ConditionItemMatcher.matchAggregateCount(actual, matcher, "容器物品数量");
            return leaf(node, context, result.matched(), result.reasonCode(), result.message(), "容器物品数量比较");
        }
    }
}
