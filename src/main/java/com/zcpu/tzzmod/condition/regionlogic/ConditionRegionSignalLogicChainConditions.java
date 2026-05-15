package com.zcpu.tzzmod.condition.regionlogic;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationResult;
import com.zcpu.tzzmod.condition.ConditionFieldSchema;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.ConditionRegistry;
import com.zcpu.tzzmod.condition.ConditionTypeHandler;
import com.zcpu.tzzmod.condition.ConditionTypeMetadata;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConditionRegionSignalLogicChainConditions {
    private static final String CATEGORY_REGION = "区域条件";
    private static final String CATEGORY_SIGNAL = "信号条件";
    private static final String CATEGORY_LOGIC_CHAIN = "逻辑链条件";

    private ConditionRegionSignalLogicChainConditions() {
    }

    public static void register(ConditionRegistry registry) {
        if (registry == null) {
            return;
        }
        registry.register(new RegionExistsHandler());
        registry.register(new RegionEnabledHandler());
        registry.register(new PlayerInRegionHandler());
        registry.register(new RegionPlayerCountCompareHandler());
        registry.register(new SignalChannelExistsHandler());
        registry.register(new SignalChannelConsumerCountCompareHandler());
        registry.register(new SignalEventCountCompareHandler());
        registry.register(new LogicChainContainsNodeHandler());
        registry.register(new LogicChainContainsChannelHandler());
        registry.register(new LogicChainHasCycleHandler());
        registry.register(new LogicChainNodeCountCompareHandler());
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

    private static ConditionFieldSchema regionKeyField() {
        return field("regionKey", "区域快照键", "string", true, "从 EvaluationContext 读取的区域快照 key");
    }

    private static ConditionFieldSchema signalChannelKeyField() {
        return field("signalChannelKey", "信号频道快照键", "string", true, "从 EvaluationContext 读取的信号频道快照 key");
    }

    private static ConditionFieldSchema signalHistoryKeyField() {
        return field("signalHistoryKey", "信号历史快照键", "string", true, "从 EvaluationContext 读取的信号历史快照 key");
    }

    private static ConditionFieldSchema logicChainKeyField() {
        return field("logicChainKey", "逻辑链快照键", "string", true, "从 EvaluationContext 读取的逻辑链快照 key");
    }

    private static ConditionFieldSchema operatorField() {
        return field("operator", "比较方式", "enum:eq,ne,gt,gte,lt,lte", true, "eq/ne/gt/gte/lt/lte");
    }

    private static ConditionFieldSchema countField() {
        return field("count", "目标数量", "integer", true, "用于比较的目标数量，必须大于等于 0");
    }

    private static String config(ConditionNode node, String key) {
        return node == null || node.config() == null ? "" : node.config().get(key);
    }

    private static boolean hasConfigKey(ConditionNode node, String key) {
        return node != null && node.config() != null && node.config().values().containsKey(key);
    }

    private static boolean configBoolean(ConditionNode node, String key, boolean defaultValue) {
        if (!hasConfigKey(node, key)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config(node, key));
    }

    private static ConditionValidationResult requireNonBlank(ConditionNode node, String key, String displayName) {
        if (node == null || !node.config().has(key)) {
            return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_" + key, "条件缺少必填字段：" + displayName);
        }
        return ConditionValidationResult.ok();
    }

    private static ConditionValidationResult requireBooleanIfPresent(ConditionNode node, String key, String displayName) {
        if (!hasConfigKey(node, key)) {
            return ConditionValidationResult.ok();
        }
        String value = config(node, key).toLowerCase(Locale.ROOT);
        if ("true".equals(value) || "false".equals(value)) {
            return ConditionValidationResult.ok();
        }
        return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_" + key, displayName + " 必须是 true 或 false");
    }

    private static ConditionValidationResult validateCountCompare(ConditionNode node) {
        ConditionValidationResult result = requireNonBlank(node, "operator", "比较方式")
                .merge(requireNonBlank(node, "count", "目标数量"));
        String operator = config(node, "operator");
        if (!operator.isBlank() && CountOperator.parse(operator).isEmpty()) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_operator", "比较方式必须是 eq/ne/gt/gte/lt/lte"));
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

    private static ConditionValidationResult validateChannelIfPresent(ConditionNode node, String key, String displayName) {
        String raw = config(node, key);
        if (raw.isBlank()) {
            return ConditionValidationResult.ok();
        }
        String normalized = SignalChannel.normalize(raw);
        if (!SignalChannel.isValid(normalized)) {
            return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_" + key, displayName + " 只能包含小写字母、数字、下划线、点、冒号和连字符");
        }
        return ConditionValidationResult.ok();
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

    private static String countMessage(String subject, int actual, CountOperator operator, int expected, boolean matched) {
        String status = matched ? "满足" : "不满足";
        return subject + status + "：当前 " + actual + "，要求 " + operator.symbol() + " " + expected + "。";
    }

    private static ConditionEvaluationResult countResult(
            ConditionNode node,
            ConditionEvaluationContext context,
            String displayName,
            String subject,
            int actual
    ) {
        CountOperator operator = CountOperator.parse(config(node, "operator")).orElse(CountOperator.EQ);
        int expected = parseInt(config(node, "count"), 0);
        boolean matched = operator.test(actual, expected);
        return leaf(
                node,
                context,
                matched,
                matched ? "condition_count_compare" : "condition_count_compare_failed",
                countMessage(subject, actual, operator, expected, matched),
                displayName
        );
    }

    private record RegionExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.REGION_EXISTS,
                    "区域快照存在",
                    "检查指定区域快照是否存在。",
                    CATEGORY_REGION,
                    regionKeyField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "regionKey", "区域快照键");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "regionKey");
            Optional<ConditionRegionSnapshot> snapshot = context == null ? Optional.empty() : context.regionSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "区域快照", "区域快照存在");
            }
            return leaf(node, context, true, "region_exists", "区域快照存在：" + snapshot.get().label() + "。", "区域快照存在");
        }
    }

    private record RegionEnabledHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.REGION_ENABLED,
                    "区域已启用",
                    "检查区域快照的 enabled 状态是否符合预期。",
                    CATEGORY_REGION,
                    regionKeyField(),
                    field("expected", "期望启用状态", "boolean", false, "默认 true")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "regionKey", "区域快照键")
                    .merge(requireBooleanIfPresent(node, "expected", "期望启用状态"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "regionKey");
            Optional<ConditionRegionSnapshot> snapshot = context == null ? Optional.empty() : context.regionSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "区域快照", "区域已启用");
            }
            boolean expected = configBoolean(node, "expected", true);
            boolean actual = snapshot.get().enabled();
            boolean matched = actual == expected;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "region_enabled" : "region_enabled_mismatch",
                    matched
                            ? "区域启用状态匹配：" + snapshot.get().label() + " = " + actual + "。"
                            : "区域启用状态不匹配：" + snapshot.get().label() + "，期望 " + expected + "，实际 " + actual + "。",
                    "区域已启用"
            );
        }
    }

    private record PlayerInRegionHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.PLAYER_IN_REGION,
                    "玩家在区域内",
                    "检查指定玩家或触发玩家是否在区域快照记录的玩家列表中。",
                    CATEGORY_REGION,
                    regionKeyField(),
                    field("playerMode", "玩家来源", "enum:context_player,explicit_player", true, "context_player=触发玩家，explicit_player=显式玩家 ID"),
                    field("playerId", "显式玩家 ID", "string", false, "playerMode=explicit_player 时必填")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = requireNonBlank(node, "regionKey", "区域快照键")
                    .merge(requireNonBlank(node, "playerMode", "玩家来源"));
            String mode = config(node, "playerMode");
            if (!mode.isBlank() && !List.of("context_player", "explicit_player").contains(mode)) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_player_mode", "玩家来源必须是 context_player 或 explicit_player"));
            }
            if ("explicit_player".equals(mode)) {
                result = result.merge(requireNonBlank(node, "playerId", "显式玩家 ID"));
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "regionKey");
            Optional<ConditionRegionSnapshot> snapshot = context == null ? Optional.empty() : context.regionSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "区域快照", "玩家在区域内");
            }
            ResolvedPlayer player = resolvePlayer(node, context);
            if (!player.valid()) {
                return leaf(node, context, false, "region_player_missing", player.failureReason(), "玩家在区域内");
            }
            boolean matched = snapshot.get().containsPlayer(player.playerId());
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_in_region" : "player_not_in_region",
                    matched
                            ? "玩家在区域内：" + player.playerId() + " 位于 " + snapshot.get().label() + "。"
                            : "玩家不在区域内：" + player.playerId() + " 不在 " + snapshot.get().label() + "。",
                    "玩家在区域内"
            );
        }
    }

    private record RegionPlayerCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.REGION_PLAYER_COUNT_COMPARE,
                    "区域玩家数量比较",
                    "比较区域快照中的玩家数量。",
                    CATEGORY_REGION,
                    regionKeyField(),
                    operatorField(),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "regionKey", "区域快照键").merge(validateCountCompare(node));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "regionKey");
            Optional<ConditionRegionSnapshot> snapshot = context == null ? Optional.empty() : context.regionSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "区域快照", "区域玩家数量比较");
            }
            return countResult(node, context, "区域玩家数量比较", "区域玩家数量", snapshot.get().playerCount());
        }
    }

    private record SignalChannelExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.SIGNAL_CHANNEL_EXISTS,
                    "信号频道快照存在",
                    "检查指定信号频道快照是否存在。",
                    CATEGORY_SIGNAL,
                    signalChannelKeyField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "signalChannelKey", "信号频道快照键");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "signalChannelKey");
            Optional<ConditionSignalChannelSnapshot> snapshot = context == null ? Optional.empty() : context.signalChannelSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "信号频道快照", "信号频道快照存在");
            }
            return leaf(node, context, true, "signal_channel_exists", "信号频道快照存在：" + snapshot.get().channel() + "。", "信号频道快照存在");
        }
    }

    private record SignalChannelConsumerCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE,
                    "信号消费者数量比较",
                    "比较信号频道快照中的消费者数量。",
                    CATEGORY_SIGNAL,
                    signalChannelKeyField(),
                    operatorField(),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "signalChannelKey", "信号频道快照键").merge(validateCountCompare(node));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "signalChannelKey");
            Optional<ConditionSignalChannelSnapshot> snapshot = context == null ? Optional.empty() : context.signalChannelSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "信号频道快照", "信号消费者数量比较");
            }
            return countResult(node, context, "信号消费者数量比较", "信号消费者数量", snapshot.get().consumerCount());
        }
    }

    private record SignalEventCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                    "信号事件数量比较",
                    "比较信号历史快照中的事件数量，可按频道和来源过滤。",
                    CATEGORY_SIGNAL,
                    signalHistoryKeyField(),
                    field("channel", "信号频道", "channel", false, "可选，只统计指定频道"),
                    field("sourceType", "来源类型", "string", false, "可选，只统计指定来源类型"),
                    field("sourceId", "来源 ID", "string", false, "可选，只统计指定来源 ID"),
                    operatorField(),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "signalHistoryKey", "信号历史快照键")
                    .merge(validateCountCompare(node))
                    .merge(validateChannelIfPresent(node, "channel", "信号频道"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "signalHistoryKey");
            Optional<ConditionSignalHistorySnapshot> snapshot = context == null ? Optional.empty() : context.signalHistorySnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "信号历史快照", "信号事件数量比较");
            }
            int actual = snapshot.get().count(config(node, "channel"), config(node, "sourceType"), config(node, "sourceId"));
            return countResult(node, context, "信号事件数量比较", "信号事件数量", actual);
        }
    }

    private record LogicChainContainsNodeHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE,
                    "逻辑链包含节点",
                    "检查逻辑链快照中是否包含指定节点 ID。",
                    CATEGORY_LOGIC_CHAIN,
                    logicChainKeyField(),
                    field("nodeId", "节点 ID", "string", true, "逻辑链快照中的节点 ID")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "logicChainKey", "逻辑链快照键")
                    .merge(requireNonBlank(node, "nodeId", "节点 ID"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "logicChainKey");
            Optional<ConditionLogicChainSnapshot> snapshot = context == null ? Optional.empty() : context.logicChainSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "逻辑链快照", "逻辑链包含节点");
            }
            String nodeId = config(node, "nodeId");
            boolean matched = snapshot.get().containsNode(nodeId);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "logic_chain_contains_node" : "logic_chain_node_missing",
                    matched ? "逻辑链包含节点：" + nodeId + "。" : "逻辑链不包含节点：" + nodeId + "。",
                    "逻辑链包含节点"
            );
        }
    }

    private record LogicChainContainsChannelHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL,
                    "逻辑链包含频道",
                    "检查逻辑链快照中是否包含指定频道，包括下游频道。",
                    CATEGORY_LOGIC_CHAIN,
                    logicChainKeyField(),
                    field("channel", "信号频道", "channel", true, "逻辑链中的频道")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "logicChainKey", "逻辑链快照键")
                    .merge(requireNonBlank(node, "channel", "信号频道"))
                    .merge(validateChannelIfPresent(node, "channel", "信号频道"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "logicChainKey");
            Optional<ConditionLogicChainSnapshot> snapshot = context == null ? Optional.empty() : context.logicChainSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "逻辑链快照", "逻辑链包含频道");
            }
            String channel = SignalChannel.normalize(config(node, "channel"));
            boolean matched = snapshot.get().containsChannel(channel);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "logic_chain_contains_channel" : "logic_chain_channel_missing",
                    matched ? "逻辑链包含频道：" + channel + "。" : "逻辑链不包含频道：" + channel + "。",
                    "逻辑链包含频道"
            );
        }
    }

    private record LogicChainHasCycleHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE,
                    "逻辑链存在循环",
                    "检查逻辑链快照是否标记或推导出循环引用。",
                    CATEGORY_LOGIC_CHAIN,
                    logicChainKeyField(),
                    field("expected", "期望循环状态", "boolean", false, "默认 true")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "logicChainKey", "逻辑链快照键")
                    .merge(requireBooleanIfPresent(node, "expected", "期望循环状态"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "logicChainKey");
            Optional<ConditionLogicChainSnapshot> snapshot = context == null ? Optional.empty() : context.logicChainSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "逻辑链快照", "逻辑链存在循环");
            }
            boolean expected = configBoolean(node, "expected", true);
            boolean actual = snapshot.get().hasCycle();
            boolean matched = actual == expected;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "logic_chain_cycle_state" : "logic_chain_cycle_mismatch",
                    matched
                            ? "逻辑链循环状态匹配：当前 " + actual + "。"
                            : "逻辑链循环状态不匹配：期望 " + expected + "，实际 " + actual + "。",
                    "逻辑链存在循环"
            );
        }
    }

    private record LogicChainNodeCountCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegionSignalLogicChainConditions.metadata(
                    ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE,
                    "逻辑链节点数量比较",
                    "比较逻辑链快照中的节点数量。",
                    CATEGORY_LOGIC_CHAIN,
                    logicChainKeyField(),
                    operatorField(),
                    countField()
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "logicChainKey", "逻辑链快照键").merge(validateCountCompare(node));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "logicChainKey");
            Optional<ConditionLogicChainSnapshot> snapshot = context == null ? Optional.empty() : context.logicChainSnapshot(key);
            if (snapshot.isEmpty()) {
                return missingSnapshot(node, context, key, "逻辑链快照", "逻辑链节点数量比较");
            }
            return countResult(node, context, "逻辑链节点数量比较", "逻辑链节点数量", snapshot.get().nodeCount());
        }
    }

    private static ResolvedPlayer resolvePlayer(ConditionNode node, ConditionEvaluationContext context) {
        String mode = config(node, "playerMode");
        if ("explicit_player".equals(mode)) {
            String playerId = config(node, "playerId");
            if (playerId.isBlank()) {
                return ResolvedPlayer.invalid("显式玩家 ID 不能为空。");
            }
            return ResolvedPlayer.valid(playerId);
        }
        if (context == null || !context.hasPlayerIdentity()) {
            return ResolvedPlayer.invalid("上下文缺少触发玩家，无法判断玩家是否在区域内。");
        }
        String playerId = !context.playerId().isBlank() ? context.playerId() : context.playerName();
        return ResolvedPlayer.valid(playerId);
    }

    private record ResolvedPlayer(boolean valid, String playerId, String failureReason) {
        private static ResolvedPlayer valid(String playerId) {
            return new ResolvedPlayer(true, playerId, "");
        }

        private static ResolvedPlayer invalid(String failureReason) {
            return new ResolvedPlayer(false, "", failureReason);
        }
    }

    private enum CountOperator {
        EQ("=", "eq") {
            @Override
            boolean test(int actual, int expected) {
                return actual == expected;
            }
        },
        NE("!=", "ne") {
            @Override
            boolean test(int actual, int expected) {
                return actual != expected;
            }
        },
        GT(">", "gt") {
            @Override
            boolean test(int actual, int expected) {
                return actual > expected;
            }
        },
        GTE(">=", "gte") {
            @Override
            boolean test(int actual, int expected) {
                return actual >= expected;
            }
        },
        LT("<", "lt") {
            @Override
            boolean test(int actual, int expected) {
                return actual < expected;
            }
        },
        LTE("<=", "lte") {
            @Override
            boolean test(int actual, int expected) {
                return actual <= expected;
            }
        };

        private final String symbol;
        private final String id;

        CountOperator(String symbol, String id) {
            this.symbol = symbol;
            this.id = id;
        }

        abstract boolean test(int actual, int expected);

        String symbol() {
            return symbol;
        }

        static Optional<CountOperator> parse(String raw) {
            String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            for (CountOperator operator : values()) {
                if (operator.id.equals(value)) {
                    return Optional.of(operator);
                }
            }
            return Optional.empty();
        }
    }
}
