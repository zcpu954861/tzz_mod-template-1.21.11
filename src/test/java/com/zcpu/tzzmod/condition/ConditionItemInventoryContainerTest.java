package com.zcpu.tzzmod.condition;

import com.zcpu.tzzmod.condition.item.ConditionContainerSnapshot;
import com.zcpu.tzzmod.condition.item.ConditionInventorySnapshot;
import com.zcpu.tzzmod.condition.item.ConditionItemCompareOperator;
import com.zcpu.tzzmod.condition.item.ConditionItemMatchConfig;
import com.zcpu.tzzmod.condition.item.ConditionItemMatchResult;
import com.zcpu.tzzmod.condition.item.ConditionItemMatcher;
import com.zcpu.tzzmod.condition.item.ConditionItemStackSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConditionItemInventoryContainerTest {
    private ConditionItemInventoryContainerTest() {
    }

    public static void run() {
        testItemSnapshotAndMatcherMatrix();
        testItemStackExists();
        testItemStackMatches();
        testInventoryContainsItem();
        testInventoryItemCountCompare();
        testContainerSlotEmpty();
        testContainerSlotItemMatches();
        testContainerItemCountCompare();
        testInvalidConfigValidation();
        testChineseMetadataAndFailureReasons();
        testGroupIntegrationAndNoSideEffects();
    }

    private static void testItemSnapshotAndMatcherMatrix() {
        ConditionItemStackSnapshot empty = ConditionItemStackSnapshot.empty();
        ConditionItemStackSnapshot air = ConditionItemStackSnapshot.of("minecraft:air", 1);
        ConditionItemStackSnapshot zero = ConditionItemStackSnapshot.of("minecraft:diamond", 0);
        ConditionItemStackSnapshot diamond = ConditionItemStackSnapshot.of("minecraft:diamond", 3);
        requireTrue(empty.isEmpty(), "empty item snapshot");
        requireFalse(diamond.isEmpty(), "non-empty item snapshot");
        requireTrue(air.isEmpty(), "minecraft:air is empty");
        requireTrue(zero.isEmpty(), "count <= 0 is empty");
        requireFalse(ConditionItemMatcher.validItemId("minecraft:air"), "minecraft:air invalid matcher item id");
        requireTrue(ConditionItemMatcher.validItemId("minecraft:diamond"), "valid item id");
        requireFalse(ConditionItemMatcher.validItemId("Diamond"), "invalid item id without namespace");

        requireTrue(match(diamond, "eq", 3).matched(), "count eq");
        requireTrue(match(diamond, "ne", 2).matched(), "count ne");
        requireTrue(match(diamond, "gt", 2).matched(), "count gt");
        requireTrue(match(diamond, "gte", 3).matched(), "count gte");
        requireTrue(match(diamond, "lt", 4).matched(), "count lt");
        requireTrue(match(diamond, "lte", 3).matched(), "count lte");
        requireFalse(match(diamond, "gte", 4).matched(), "count mismatch");
        requireContains(match(diamond, "gte", 4).message(), "物品数量不满足", "count mismatch Chinese failure");
        requireFalse(ConditionItemMatcher.matchStack(diamond, new ConditionItemMatchConfig("minecraft:emerald", ConditionItemCompareOperator.GTE, 1)).matched(), "itemId mismatch");
        requireContains(ConditionItemMatcher.matchStack(diamond, new ConditionItemMatchConfig("minecraft:emerald", ConditionItemCompareOperator.GTE, 1)).message(), "物品 ID 不匹配", "itemId mismatch Chinese failure");
        requireFalse(ConditionItemCompareOperator.parse("between").isPresent(), "invalid operator");
    }

    private static void testItemStackExists() {
        ConditionEvaluationContext context = itemContext();
        requireTrue(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "hand")), context).matched(), "item exists true");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "empty")), context), "物品快照为空", "item exists empty");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "zero")), context), "数量为 0", "item exists count <= 0");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "air")), context), "minecraft:air", "item exists air");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "missing")), context), "上下文缺少物品快照", "item missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "inventory")), context), "快照类型不匹配", "item wrong snapshot type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.ITEM_STACK_EXISTS))), "condition_config_missing_itemKey", "item exists missing itemKey validation");
    }

    private static void testItemStackMatches() {
        ConditionEvaluationContext context = itemContext();
        requireTrue(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "gte", "3")), context).matched(), "item stack matches true");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:emerald", "gte", "1")), context), "物品 ID 不匹配", "item stack itemId mismatch");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "gt", "3")), context), "物品数量不满足", "item stack count mismatch");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("empty", "minecraft:diamond", "gte", "1")), context), "物品快照为空", "item stack empty");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("air", "minecraft:diamond", "gte", "1")), context), "物品快照为空", "item stack air");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("missing", "minecraft:diamond", "gte", "1")), context), "上下文缺少物品快照", "item stack missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("inventory", "minecraft:diamond", "gte", "1")), context), "快照类型不匹配", "item stack wrong snapshot type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, config("itemKey", "hand")))), "condition_config_empty_matcher", "item stack empty matcher validation");
    }

    private static void testInventoryContainsItem() {
        ConditionEvaluationContext context = itemContext();
        requireTrue(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:diamond", "gte", "5")), context).matched(), "inventory contains aggregate true");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:diamond", "gte", "6")), context), "背包物品数量不满足", "inventory contains aggregate false");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:emerald", "gte", "1")), context), "背包物品数量不满足", "inventory does not contain item");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("empty_inventory", "minecraft:diamond", "gte", "1")), context), "背包物品数量不满足", "empty inventory");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("missing", "minecraft:diamond", "gte", "1")), context), "上下文缺少背包快照", "missing inventory snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("hand", "minecraft:diamond", "gte", "1")), context), "快照类型不匹配", "inventory wrong snapshot type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, config("inventoryKey", "inventory")))), "condition_config_empty_matcher", "inventory empty matcher validation");
    }

    private static void testInventoryItemCountCompare() {
        ConditionEvaluationContext context = itemContext();
        // coverage markers: inventory count eq; inventory count ne; inventory count gt; inventory count gte; inventory count lt; inventory count lte
        for (String[] pair : List.of(
                new String[]{"eq", "5"},
                new String[]{"ne", "4"},
                new String[]{"gt", "4"},
                new String[]{"gte", "5"},
                new String[]{"lt", "6"},
                new String[]{"lte", "5"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "minecraft:diamond", pair[0], pair[1])), context).matched(), "inventory count " + pair[0]);
        }
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("empty_inventory", "minecraft:diamond", "gte", "1")), context), "背包物品数量不满足", "inventory count empty inventory");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("missing", "minecraft:diamond", "gte", "1")), context), "上下文缺少背包快照", "inventory count missing");
        assertFailure(evaluate(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("hand", "minecraft:diamond", "gte", "1")), context), "快照类型不匹配", "inventory count wrong type");
    }

    private static void testContainerSlotEmpty() {
        ConditionEvaluationContext context = itemContext();
        requireTrue(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "2")), context).matched(), "container slot empty true");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "0")), context), "容器槽位不为空", "container slot non-empty");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "-1")))), "condition_config_invalid_slot", "negative slot validation");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "9")), context), "容器槽位越界", "slot out of range");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "missing", "slot", "0")), context), "上下文缺少容器快照", "missing container");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "hand", "slot", "0")), context), "快照类型不匹配", "container wrong type");
    }

    private static void testContainerSlotItemMatches() {
        ConditionEvaluationContext context = itemContext();
        requireTrue(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "minecraft:diamond", "gte", "3")), context).matched(), "container slot item matches");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "minecraft:emerald", "gte", "1")), context), "物品 ID 不匹配", "container slot item mismatch");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "2", "minecraft:diamond", "gte", "1")), context), "物品快照为空", "container slot empty");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "3", "minecraft:diamond", "gte", "1")), context), "物品快照为空", "container slot air");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "-1", "minecraft:diamond", "gte", "1")))), "condition_config_invalid_slot", "container slot negative validation");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "9", "minecraft:diamond", "gte", "1")), context), "容器槽位越界", "container slot out of range");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("missing", "0", "minecraft:diamond", "gte", "1")), context), "上下文缺少容器快照", "container slot missing container");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("hand", "0", "minecraft:diamond", "gte", "1")), context), "快照类型不匹配", "container slot wrong type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, config("containerKey", "container", "slot", "0")))), "condition_config_empty_matcher", "container slot empty matcher validation");
    }

    private static void testContainerItemCountCompare() {
        ConditionEvaluationContext context = itemContext();
        // coverage markers: container count eq; container count ne; container count gt; container count gte; container count lt; container count lte
        for (String[] pair : List.of(
                new String[]{"eq", "4"},
                new String[]{"ne", "5"},
                new String[]{"gt", "3"},
                new String[]{"gte", "4"},
                new String[]{"lt", "5"},
                new String[]{"lte", "4"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:diamond", pair[0], pair[1])), context).matched(), "container count " + pair[0]);
        }
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("empty_container", "minecraft:diamond", "gte", "1")), context), "容器物品数量不满足", "container empty");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("missing", "minecraft:diamond", "gte", "1")), context), "上下文缺少容器快照", "container missing");
        assertFailure(evaluate(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("inventory", "minecraft:diamond", "gte", "1")), context), "快照类型不匹配", "container wrong type");
    }

    private static void testInvalidConfigValidation() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, config("itemId", "minecraft:diamond", "countOperator", "gte", "count", "1")))), "condition_config_missing_itemKey", "item stack missing itemKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "Diamond", "gte", "1")))), "condition_config_invalid_item_id", "invalid itemId validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:air", "gte", "1")))), "condition_config_invalid_item_id", "minecraft:air matcher validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "between", "1")))), "condition_config_invalid_operator", "invalid count operator validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "gte", "-1")))), "condition_config_invalid_count", "negative count validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "gte", "many")))), "condition_config_invalid_count", "invalid count validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, config("itemId", "minecraft:diamond", "countOperator", "gte", "count", "1")))), "condition_config_missing_inventoryKey", "inventory contains missing inventoryKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "Diamond", "gte", "1")))), "condition_config_invalid_item_id", "inventory contains invalid itemId");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:diamond", "between", "1")))), "condition_config_invalid_operator", "inventory contains invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:diamond", "gte", "many")))), "condition_config_invalid_count", "inventory contains invalid count");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, config("itemId", "minecraft:diamond", "operator", "gte", "count", "1")))), "condition_config_missing_inventoryKey", "inventory count missing inventoryKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, config("inventoryKey", "inventory", "operator", "gte", "count", "1")))), "condition_config_missing_itemId", "inventory count missing itemId validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "Diamond", "gte", "1")))), "condition_config_invalid_item_id", "inventory count invalid itemId validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "minecraft:diamond", "between", "1")))), "condition_config_invalid_operator", "inventory invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "minecraft:diamond", "gte", "many")))), "condition_config_invalid_count", "inventory invalid count");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("slot", "0")))), "condition_config_missing_containerKey", "container slot empty missing containerKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container")))), "condition_config_missing_slot", "container slot empty missing slot validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "one")))), "condition_config_invalid_slot", "container slot empty non-numeric slot validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, config("slot", "0", "itemId", "minecraft:diamond", "countOperator", "gte", "count", "1")))), "condition_config_missing_containerKey", "container slot item missing containerKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, config("containerKey", "container", "itemId", "minecraft:diamond", "countOperator", "gte", "count", "1")))), "condition_config_missing_slot", "container slot item missing slot validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "one", "minecraft:diamond", "gte", "1")))), "condition_config_invalid_slot", "container slot item non-numeric slot validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "Diamond", "gte", "1")))), "condition_config_invalid_item_id", "container slot item invalid itemId");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "minecraft:diamond", "between", "1")))), "condition_config_invalid_operator", "container slot item invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "minecraft:diamond", "gte", "many")))), "condition_config_invalid_count", "container slot item invalid count");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, config("itemId", "minecraft:diamond", "operator", "gte", "count", "1")))), "condition_config_missing_containerKey", "container count missing containerKey validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, config("containerKey", "container", "operator", "gte", "count", "1")))), "condition_config_missing_itemId", "container count missing itemId validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "Diamond", "gte", "1")))), "condition_config_invalid_item_id", "container count invalid itemId");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:diamond", "between", "1")))), "condition_config_invalid_operator", "container invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:diamond", "gte", "many")))), "condition_config_invalid_count", "container invalid count");
    }

    private static void testChineseMetadataAndFailureReasons() {
        ConditionRegistry registry = ConditionRegistry.defaultRegistry();
        for (String type : List.of(
                ConditionNodeType.ITEM_STACK_EXISTS,
                ConditionNodeType.ITEM_STACK_MATCHES,
                ConditionNodeType.INVENTORY_CONTAINS_ITEM,
                ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE,
                ConditionNodeType.CONTAINER_SLOT_EMPTY,
                ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE
        )) {
            ConditionTypeMetadata metadata = registry.metadata(type).orElseThrow();
            requireTrue(containsChinese(metadata.displayName()), type + " Chinese display name");
            requireTrue(containsChinese(metadata.description()), type + " Chinese description");
            requireTrue(containsChinese(metadata.category()), type + " Chinese category");
            requireFalse(metadata.displayName().equals(type), type + " display not raw type id");
            requireTrue(metadata.fields().stream().allMatch((field) -> containsChinese(field.displayName())), type + " Chinese field display");
        }
        ConditionEvaluationResult result = evaluate(leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "0")), itemContext());
        requireContains(result.failureReason(), "容器槽位不为空", "Chinese failure reason");
        requireTrue(containsChinese(result.failureReason()), "failure reason contains Chinese");
    }

    private static void testGroupIntegrationAndNoSideEffects() {
        ConditionEvaluationContext context = itemContext();
        Map<String, ConditionItemStackSnapshot> itemBefore = context.itemSnapshots();
        Map<String, ConditionInventorySnapshot> inventoryBefore = context.inventorySnapshots();
        Map<String, ConditionContainerSnapshot> containerBefore = context.containerSnapshots();

        ConditionNode disabled = new ConditionNode(
                "disabled_item",
                ConditionNodeType.ITEM_STACK_MATCHES,
                "",
                "",
                false,
                ConditionGroupMode.AND,
                itemMatcherConfig("hand", "minecraft:emerald", "gte", "1"),
                List.of()
        );
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:diamond", "gte", "3")),
                leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:diamond", "gte", "5")),
                leaf(ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, containerSlotMatcherConfig("container", "0", "minecraft:diamond", "gte", "3")),
                ConditionNode.group("or", ConditionGroupMode.OR, List.of(
                        leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:emerald", "gte", "1")),
                        leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "minecraft:diamond", "eq", "5")),
                        leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:diamond", "eq", "4"))
                )),
                ConditionNode.not("not_empty_container_slot", leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "0"))),
                ConditionNode.group("nested", ConditionGroupMode.AND, List.of(
                        leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "hand")),
                        leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "2"))
                )),
                disabled
        ));
        ConditionEvaluationResult first = new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("item_group", root), context);
        ConditionEvaluationResult second = new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("item_group", root), context);
        requireTrue(first.matched(), "group integration with item/inventory/container");
        requireEquals(first.matched(), second.matched(), "repeated evaluation stable");
        requireTrue(first.childResults().stream().anyMatch(ConditionEvaluationResult::skipped), "disabled item node skipped");
        requireTrue(new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("not_item", ConditionNode.not(
                "not_item",
                leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("hand", "minecraft:emerald", "gte", "1"))
        )), context).matched(), "NOT + item condition");
        requireTrue(new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("not_inventory", ConditionNode.not(
                "not_inventory",
                leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:emerald", "gte", "1"))
        )), context).matched(), "NOT + inventory condition");
        requireTrue(new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("not_container", ConditionNode.not(
                "not_container",
                leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:emerald", "gte", "1"))
        )), context).matched(), "NOT + container condition");
        requireTrue(new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("nested_inventory", ConditionNode.group(
                "nested_inventory",
                ConditionGroupMode.AND,
                List.of(leaf(ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE, inventoryCompareConfig("inventory", "minecraft:diamond", "eq", "5")))
        )), context).matched(), "nested group + inventory condition");
        ConditionNode disabledInventory = new ConditionNode(
                "disabled_inventory",
                ConditionNodeType.INVENTORY_CONTAINS_ITEM,
                "",
                "",
                false,
                ConditionGroupMode.AND,
                inventoryMatcherConfig("inventory", "minecraft:emerald", "gte", "1"),
                List.of()
        );
        ConditionNode disabledContainer = new ConditionNode(
                "disabled_container",
                ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE,
                "",
                "",
                false,
                ConditionGroupMode.AND,
                containerCompareConfig("container", "minecraft:emerald", "gte", "1"),
                List.of()
        );
        ConditionEvaluationResult disabledGroup = new ConditionEvaluator().evaluate(
                ConditionGroupDefinition.of(
                        "disabled_more",
                        ConditionNode.group("disabled_more", ConditionGroupMode.AND, List.of(
                                leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "hand")),
                                disabledInventory,
                                disabledContainer
                        ))
                ),
                context
        );
        requireTrue(disabledGroup.matched(), "disabled inventory/container condition skipped");
        requireEquals(2L, disabledGroup.childResults().stream().filter(ConditionEvaluationResult::skipped).count(), "disabled inventory/container skip count");
        requireEquals(itemBefore, context.itemSnapshots(), "evaluation does not modify item snapshot");
        requireEquals(inventoryBefore, context.inventorySnapshots(), "evaluation does not modify inventory snapshot");
        requireEquals(containerBefore, context.containerSnapshots(), "evaluation does not modify container snapshot");

        evaluate(leaf(ConditionNodeType.ITEM_STACK_MATCHES, itemMatcherConfig("missing", "minecraft:diamond", "gte", "1")), context);
        evaluate(leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM, inventoryMatcherConfig("inventory", "minecraft:emerald", "gte", "1")), context);
        evaluate(leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE, containerCompareConfig("container", "minecraft:emerald", "gte", "1")), context);
        requireEquals(itemBefore, context.itemSnapshots(), "failed condition does not create/update/delete item snapshots");
        requireEquals(inventoryBefore, context.inventorySnapshots(), "failed condition does not create/update/delete inventory snapshots");
        requireEquals(containerBefore, context.containerSnapshots(), "failed condition does not create/update/delete container snapshots");
    }

    private static ConditionItemMatchResult match(ConditionItemStackSnapshot snapshot, String operator, int count) {
        return ConditionItemMatcher.matchStack(
                snapshot,
                new ConditionItemMatchConfig("minecraft:diamond", ConditionItemCompareOperator.parse(operator).orElse(null), count)
        );
    }

    private static ConditionEvaluationContext itemContext() {
        return ConditionEvaluationContext.builder()
                .itemSnapshot("hand", ConditionItemStackSnapshot.of("minecraft:diamond", 3))
                .itemSnapshot("empty", ConditionItemStackSnapshot.empty())
                .itemSnapshot("zero", ConditionItemStackSnapshot.of("minecraft:diamond", 0))
                .itemSnapshot("air", ConditionItemStackSnapshot.of("minecraft:air", 1))
                .inventorySnapshot("inventory", new ConditionInventorySnapshot(List.of(
                        ConditionItemStackSnapshot.of("minecraft:diamond", 2),
                        ConditionItemStackSnapshot.of("minecraft:stone", 64),
                        ConditionItemStackSnapshot.of("minecraft:diamond", 3),
                        ConditionItemStackSnapshot.empty()
                )))
                .inventorySnapshot("empty_inventory", ConditionInventorySnapshot.empty())
                .containerSnapshot("container", new ConditionContainerSnapshot(List.of(
                        ConditionItemStackSnapshot.of("minecraft:diamond", 3),
                        ConditionItemStackSnapshot.of("minecraft:diamond", 1),
                        ConditionItemStackSnapshot.empty(),
                        ConditionItemStackSnapshot.of("minecraft:air", 1)
                )))
                .containerSnapshot("empty_container", ConditionContainerSnapshot.empty())
                .build();
    }

    private static ConditionNode leaf(String type) {
        return ConditionNode.leaf(type, type);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf(type, type, config);
    }

    private static ConditionNodeConfig itemMatcherConfig(String key, String itemId, String operator, String count) {
        return config("itemKey", key, "itemId", itemId, "countOperator", operator, "count", count);
    }

    private static ConditionNodeConfig inventoryMatcherConfig(String key, String itemId, String operator, String count) {
        return config("inventoryKey", key, "itemId", itemId, "countOperator", operator, "count", count);
    }

    private static ConditionNodeConfig inventoryCompareConfig(String key, String itemId, String operator, String count) {
        return config("inventoryKey", key, "itemId", itemId, "operator", operator, "count", count);
    }

    private static ConditionNodeConfig containerSlotMatcherConfig(String key, String slot, String itemId, String operator, String count) {
        return config("containerKey", key, "slot", slot, "itemId", itemId, "countOperator", operator, "count", count);
    }

    private static ConditionNodeConfig containerCompareConfig(String key, String itemId, String operator, String count) {
        return config("containerKey", key, "itemId", itemId, "operator", operator, "count", count);
    }

    private static ConditionNodeConfig config(String... entries) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static ConditionGroupDefinition def(ConditionNode node) {
        return ConditionGroupDefinition.of("test", node);
    }

    private static ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
        return new ConditionEvaluator().evaluate(def(node), context);
    }

    private static void assertFailure(ConditionEvaluationResult result, String expectedReasonPart, String message) {
        requireFalse(result.matched(), message);
        requireContains(result.failureReason(), expectedReasonPart, message + " failure reason");
        requireTrue(containsChinese(result.failureReason()), message + " Chinese failure reason");
    }

    private static void requireIssue(ConditionValidationResult result, String code, String message) {
        requireTrue(result.issues().stream().anyMatch((issue) -> code.equals(issue.code()) && containsChinese(issue.message())), message);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
