package com.zcpu.tzzmod.condition;

import com.zcpu.tzzmod.condition.regionlogic.ConditionLogicChainEdgeSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionLogicChainNodeSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionLogicChainSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionRegionSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionSignalChannelSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionSignalEventSnapshot;
import com.zcpu.tzzmod.condition.regionlogic.ConditionSignalHistorySnapshot;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConditionRegionSignalLogicChainTest {
    private ConditionRegionSignalLogicChainTest() {
    }

    public static void run() {
        testRegionSnapshots();
        testRegionConditions();
        testSignalSnapshots();
        testSignalConditions();
        testLogicChainSnapshots();
        testLogicChainConditions();
        testInvalidConfigValidation();
        testChineseMetadataAndFailureReasons();
        testGroupIntegrationAndNoSideEffects();
    }

    private static void testRegionSnapshots() {
        ConditionRegionSnapshot region = new ConditionRegionSnapshot(
                "mission.lobby",
                "大厅区域",
                true,
                "minecraft:overworld",
                List.of("player-1", "player-2", "player-2", ""),
                "0,0,0 -> 10,10,10",
                Map.of("purpose", "test")
        );
        requireEquals("mission.lobby", region.regionId(), "region snapshot exists");
        requireTrue(region.enabled(), "enabled true snapshot");
        requireTrue(region.containsPlayer("player-1"), "player list non-empty contains player");
        requireFalse(region.containsPlayer("missing-player"), "player not inside region");
        requireEquals(2, region.playerCount(), "player count distinct");
        requireEquals("minecraft:overworld", region.world(), "world field");
        requireEquals("大厅区域", region.displayName(), "displayName field");
        requireThrows(() -> region.playerIdsInside().add("player-3"), "region snapshot immutable player list");
        requireThrows(() -> region.metadata().put("x", "y"), "region snapshot immutable metadata");
    }

    private static void testRegionConditions() {
        ConditionEvaluationContext context = regionSignalLogicContext();
        requireTrue(evaluate(leaf(ConditionNodeType.REGION_EXISTS, config("regionKey", "region")), context).matched(), "region exists true");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_EXISTS, config("regionKey", "missing")), context), "上下文缺少区域快照", "region exists missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_EXISTS, config("regionKey", "signal")), context), "快照类型不匹配", "region exists wrong snapshot type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.REGION_EXISTS))), "condition_config_missing_regionKey", "region exists missing regionKey validation");

        requireTrue(evaluate(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "region")), context).matched(), "region enabled true");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "disabled_region")), context), "区域启用状态不匹配", "region enabled false");
        requireTrue(evaluate(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "disabled_region", "expected", "false")), context).matched(), "region enabled expected false");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "missing")), context), "上下文缺少区域快照", "region enabled missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "signal")), context), "快照类型不匹配", "region enabled wrong snapshot type");

        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "explicit_player", "playerId", "player-1")), context).matched(), "explicit playerId in region");
        assertFailure(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "explicit_player", "playerId", "player-9")), context), "玩家不在区域内", "explicit playerId not in region");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "context_player")), context).matched(), "context_player in region");
        assertFailure(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "disabled_region", "playerMode", "context_player")), context), "玩家不在区域内", "context_player not in region");
        assertFailure(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "context_player")), noPlayerContext()), "上下文缺少触发玩家", "context_player missing player");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "explicit_player")))), "condition_config_missing_playerId", "missing playerId validation");
        assertFailure(evaluate(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "empty_region", "playerMode", "explicit_player", "playerId", "player-1")), context), "玩家不在区域内", "empty region");

        // coverage markers: region player count eq; region player count ne; region player count gt; region player count gte; region player count lt; region player count lte
        for (String[] pair : List.of(
                new String[]{"eq", "2"},
                new String[]{"ne", "3"},
                new String[]{"gt", "1"},
                new String[]{"gte", "2"},
                new String[]{"lt", "3"},
                new String[]{"lte", "2"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, countConfig("region", "regionKey", pair[0], pair[1])), context).matched(), "region player count " + pair[0]);
        }
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, countConfig("empty_region", "regionKey", "gte", "1")), context), "区域玩家数量不满足", "empty region count");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, countConfig("missing", "regionKey", "gte", "1")), context), "上下文缺少区域快照", "region count missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, countConfig("signal", "regionKey", "gte", "1")), context), "快照类型不匹配", "region count wrong snapshot type");
    }

    private static void testSignalSnapshots() {
        ConditionSignalChannelSnapshot channel = signalChannel();
        requireEquals("mission.start", channel.channel(), "signal channel snapshot exists");
        requireEquals(3, channel.consumerCount(), "consumer count");
        requireEquals(2, channel.enabledConsumerCount(), "enabled consumer count");
        requireEquals(1, channel.disabledConsumerCount(), "disabled consumer count");
        requireEquals(4, channel.actionCount(), "action count");
        requireEquals(2, channel.eventCount(), "event count");
        requireThrows(() -> channel.recentEvents().add(ConditionSignalEventSnapshot.of("x", "y", "z", 1L)), "signal snapshot immutable events");
    }

    private static void testSignalConditions() {
        ConditionEvaluationContext context = regionSignalLogicContext();
        requireTrue(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_EXISTS, config("signalChannelKey", "signal")), context).matched(), "signal channel exists true");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_EXISTS, config("signalChannelKey", "missing")), context), "上下文缺少信号频道快照", "signal channel missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_EXISTS, config("signalChannelKey", "region")), context), "快照类型不匹配", "signal channel wrong snapshot type");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.SIGNAL_CHANNEL_EXISTS))), "condition_config_missing_signalChannelKey", "signal channel missing key validation");

        // coverage markers: signal consumer count eq; signal consumer count ne; signal consumer count gt; signal consumer count gte; signal consumer count lt; signal consumer count lte
        for (String[] pair : List.of(
                new String[]{"eq", "3"},
                new String[]{"ne", "4"},
                new String[]{"gt", "2"},
                new String[]{"gte", "3"},
                new String[]{"lt", "4"},
                new String[]{"lte", "3"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("signal", "signalChannelKey", pair[0], pair[1])), context).matched(), "signal consumer count " + pair[0]);
        }
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("zero_signal", "signalChannelKey", "gte", "1")), context), "信号消费者数量不满足", "zero consumers");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("missing", "signalChannelKey", "gte", "1")), context), "上下文缺少信号频道快照", "signal consumer count missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("region", "signalChannelKey", "gte", "1")), context), "快照类型不匹配", "signal consumer count wrong type");

        // coverage markers: signal event count eq; signal event count ne; signal event count gt; signal event count gte; signal event count lt; signal event count lte
        for (String[] pair : List.of(
                new String[]{"eq", "3"},
                new String[]{"ne", "2"},
                new String[]{"gt", "2"},
                new String[]{"gte", "3"},
                new String[]{"lt", "4"},
                new String[]{"lte", "3"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("signal_history", "signalHistoryKey", pair[0], pair[1])), context).matched(), "signal event count " + pair[0]);
        }
        requireTrue(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, config("signalHistoryKey", "signal_history", "channel", "MISSION.START", "operator", "eq", "count", "2")), context).matched(), "signal event optional channel filter");
        requireTrue(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, config("signalHistoryKey", "signal_history", "sourceType", "listener", "sourceId", "listener.a", "operator", "eq", "count", "1")), context).matched(), "signal event source filters");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("empty_history", "signalHistoryKey", "gte", "1")), context), "信号事件数量不满足", "zero events");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("missing", "signalHistoryKey", "gte", "1")), context), "上下文缺少信号历史快照", "signal event missing history");
        assertFailure(evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("region", "signalHistoryKey", "gte", "1")), context), "快照类型不匹配", "signal event wrong snapshot type");
    }

    private static void testLogicChainSnapshots() {
        ConditionLogicChainSnapshot empty = new ConditionLogicChainSnapshot("", "", List.of(), List.of(), java.util.Set.of(), false, 0);
        ConditionLogicChainSnapshot chain = logicChain(true);
        ConditionLogicChainSnapshot noCycle = logicChain(false);
        requireEquals(0, empty.nodeCount(), "empty chain node count");
        requireTrue(chain.containsNode("channel:mission.start"), "simple chain contains channel node");
        requireTrue(chain.containsNode("action:notify"), "multi-node chain contains action");
        requireTrue(chain.containsChannel("mission.success"), "downstream channel exists");
        requireTrue(chain.hasCycle(), "cycle flag true");
        requireFalse(noCycle.hasCycle(), "cycle flag false");
        requireEquals(4, chain.nodeCount(), "node count");
        requireEquals(2, chain.channelCount(), "channel count");
        requireThrows(() -> chain.nodes().add(ConditionLogicChainNodeSnapshot.of("x", "channel", "x")), "logic chain immutable nodes");
        requireThrows(() -> chain.channels().add("other"), "logic chain immutable channels");
    }

    private static void testLogicChainConditions() {
        ConditionEvaluationContext context = regionSignalLogicContext();
        requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain", "nodeId", "action:notify")), context).matched(), "logic chain node exists");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain", "nodeId", "missing")), context), "逻辑链不包含节点", "logic chain node missing");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain")))), "condition_config_missing_nodeId", "logic chain missing nodeId validation");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "missing", "nodeId", "x")), context), "上下文缺少逻辑链快照", "logic chain node missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "region", "nodeId", "x")), context), "快照类型不匹配", "logic chain node wrong snapshot type");

        requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain", "channel", "mission.start")), context).matched(), "logic chain channel exists");
        requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain", "channel", "mission.success")), context).matched(), "logic chain downstream channel exists");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain", "channel", "mission.fail")), context), "逻辑链不包含频道", "logic chain channel missing");
        requireIssue(new ConditionEvaluator().validate(def(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain")))), "condition_config_missing_channel", "logic chain missing channel validation");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "missing", "channel", "mission.start")), context), "上下文缺少逻辑链快照", "logic chain channel missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "signal", "channel", "mission.start")), context), "快照类型不匹配", "logic chain channel wrong snapshot type");

        requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "chain")), context).matched(), "logic chain hasCycle true");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "no_cycle_chain")), context), "逻辑链循环状态不匹配", "logic chain hasCycle false");
        requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "no_cycle_chain", "expected", "false")), context).matched(), "logic chain expected false");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "missing")), context), "上下文缺少逻辑链快照", "logic chain cycle missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "region")), context), "快照类型不匹配", "logic chain cycle wrong type");

        // coverage markers: logic chain node count eq; logic chain node count ne; logic chain node count gt; logic chain node count gte; logic chain node count lt; logic chain node count lte
        for (String[] pair : List.of(
                new String[]{"eq", "4"},
                new String[]{"ne", "3"},
                new String[]{"gt", "3"},
                new String[]{"gte", "4"},
                new String[]{"lt", "5"},
                new String[]{"lte", "4"}
        )) {
            requireTrue(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, countConfig("chain", "logicChainKey", pair[0], pair[1])), context).matched(), "logic chain node count " + pair[0]);
        }
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, countConfig("empty_chain", "logicChainKey", "gte", "1")), context), "逻辑链节点数量不满足", "empty chain count");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, countConfig("missing", "logicChainKey", "gte", "1")), context), "上下文缺少逻辑链快照", "logic chain count missing snapshot");
        assertFailure(evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, countConfig("region", "logicChainKey", "gte", "1")), context), "快照类型不匹配", "logic chain count wrong type");
    }

    private static void testInvalidConfigValidation() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, config("regionKey", "region", "operator", "between", "count", "1")))), "condition_config_invalid_operator", "region invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, config("regionKey", "region", "operator", "gte", "count", "-1")))), "condition_config_invalid_count", "region invalid negative count");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, config("regionKey", "region", "operator", "gte", "count", "many")))), "condition_config_invalid_count", "region invalid count value");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "region", "expected", "yes")))), "condition_config_invalid_expected", "region expected boolean validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "region", "playerMode", "other")))), "condition_config_invalid_player_mode", "player mode validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, config("signalChannelKey", "signal", "operator", "between", "count", "1")))), "condition_config_invalid_operator", "signal invalid operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, config("signalHistoryKey", "history", "channel", "bad channel", "operator", "gte", "count", "1")))), "condition_config_invalid_channel", "signal event invalid channel");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, config("operator", "gte", "count", "1")))), "condition_config_missing_signalHistoryKey", "signal event missing history key validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain", "channel", "bad channel")))), "condition_config_invalid_channel", "logic chain invalid channel");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "chain", "expected", "maybe")))), "condition_config_invalid_expected", "logic chain expected boolean validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, config("logicChainKey", "chain", "operator", "gte", "count", "-1")))), "condition_config_invalid_count", "logic chain invalid count");
    }

    private static void testChineseMetadataAndFailureReasons() {
        ConditionRegistry registry = ConditionRegistry.defaultRegistry();
        for (String type : regionSignalLogicTypes()) {
            ConditionTypeMetadata metadata = registry.metadata(type).orElseThrow();
            requireTrue(containsChinese(metadata.displayName()), type + " Chinese display name");
            requireTrue(containsChinese(metadata.description()), type + " Chinese description");
            requireTrue(containsChinese(metadata.category()), type + " Chinese category");
            requireFalse(metadata.displayName().equals(type), type + " display not raw type id");
            requireTrue(metadata.fields().stream().allMatch((field) -> containsChinese(field.displayName())), type + " Chinese field display");
        }
        ConditionEvaluationResult result = evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain", "nodeId", "missing")), regionSignalLogicContext());
        requireContains(result.failureReason(), "逻辑链不包含节点", "Chinese failure reason");
        requireTrue(containsChinese(result.failureReason()), "failure reason contains Chinese");
    }

    private static void testGroupIntegrationAndNoSideEffects() {
        ConditionEvaluationContext context = regionSignalLogicContext();
        Map<String, ConditionRegionSnapshot> regionBefore = context.regionSnapshots();
        Map<String, ConditionSignalChannelSnapshot> signalBefore = context.signalChannelSnapshots();
        Map<String, ConditionSignalHistorySnapshot> historyBefore = context.signalHistorySnapshots();
        Map<String, ConditionLogicChainSnapshot> chainBefore = context.logicChainSnapshots();

        ConditionNode disabledRegion = new ConditionNode("disabled_region_condition", ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, "", "", false, ConditionGroupMode.AND, countConfig("empty_region", "regionKey", "gte", "1"), List.of());
        ConditionNode disabledSignal = new ConditionNode("disabled_signal_condition", ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, "", "", false, ConditionGroupMode.AND, countConfig("zero_signal", "signalChannelKey", "gte", "1"), List.of());
        ConditionNode disabledChain = new ConditionNode("disabled_chain_condition", ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, "", "", false, ConditionGroupMode.AND, countConfig("empty_chain", "logicChainKey", "gte", "1"), List.of());
        ConditionNode root = ConditionNode.group("region_signal_logic_group", ConditionGroupMode.AND, List.of(
                leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "region")),
                leaf(ConditionNodeType.SIGNAL_CHANNEL_EXISTS, config("signalChannelKey", "signal")),
                leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, config("logicChainKey", "chain", "channel", "mission.success")),
                ConditionNode.group("or_region_signal_logic", ConditionGroupMode.OR, List.of(
                        leaf(ConditionNodeType.REGION_PLAYER_COUNT_COMPARE, countConfig("empty_region", "regionKey", "gte", "1")),
                        leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("signal_history", "signalHistoryKey", "eq", "3")),
                        leaf(ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE, countConfig("chain", "logicChainKey", "eq", "4"))
                )),
                ConditionNode.not("not_region", leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "empty_region", "playerMode", "explicit_player", "playerId", "player-1"))),
                ConditionNode.not("not_signal", leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("zero_signal", "signalChannelKey", "gte", "1"))),
                ConditionNode.not("not_chain", leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain", "nodeId", "missing"))),
                ConditionNode.group("nested_region_signal_logic", ConditionGroupMode.AND, List.of(
                        leaf(ConditionNodeType.REGION_EXISTS, config("regionKey", "region")),
                        leaf(ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE, countConfig("signal", "signalChannelKey", "eq", "3")),
                        leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "chain"))
                )),
                disabledRegion,
                disabledSignal,
                disabledChain
        ));
        ConditionEvaluationResult first = new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("region_signal_logic_group", root), context);
        ConditionEvaluationResult second = new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("region_signal_logic_group", root), context);
        requireTrue(first.matched(), "group integration with region/signal/logic chain");
        requireEquals(first.matched(), second.matched(), "repeated evaluation result stable");
        requireEquals(3L, first.childResults().stream().filter(ConditionEvaluationResult::skipped).count(), "disabled region/signal/logic node skip count");
        requireEquals(regionBefore, context.regionSnapshots(), "evaluation does not modify region snapshot");
        requireEquals(signalBefore, context.signalChannelSnapshots(), "evaluation does not modify signal snapshot");
        requireEquals(historyBefore, context.signalHistorySnapshots(), "evaluation does not modify signal history snapshot");
        requireEquals(chainBefore, context.logicChainSnapshots(), "evaluation does not modify logic chain snapshot");

        evaluate(leaf(ConditionNodeType.REGION_EXISTS, config("regionKey", "missing")), context);
        evaluate(leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, countConfig("empty_history", "signalHistoryKey", "gte", "1")), context);
        evaluate(leaf(ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, config("logicChainKey", "chain", "nodeId", "missing")), context);
        requireEquals(regionBefore, context.regionSnapshots(), "failed condition does not create/update/delete region snapshots");
        requireEquals(signalBefore, context.signalChannelSnapshots(), "failed condition does not create/update/delete signal snapshots");
        requireEquals(historyBefore, context.signalHistorySnapshots(), "failed condition does not write signal history snapshot");
        requireEquals(chainBefore, context.logicChainSnapshots(), "failed condition does not write logic chain snapshot");
    }

    private static ConditionEvaluationContext regionSignalLogicContext() {
        return ConditionEvaluationContext.builder()
                .player("player-1", "RunnerOne")
                .regionSnapshot("region", new ConditionRegionSnapshot("mission.lobby", "大厅区域", true, "minecraft:overworld", List.of("player-1", "player-2"), "0,0,0 -> 10,10,10", Map.of("purpose", "test")))
                .regionSnapshot("disabled_region", new ConditionRegionSnapshot("mission.disabled", "禁用区域", false, "minecraft:overworld", List.of(), "", Map.of()))
                .regionSnapshot("empty_region", new ConditionRegionSnapshot("mission.empty", "空区域", true, "minecraft:overworld", List.of(), "", Map.of()))
                .signalChannelSnapshot("signal", signalChannel())
                .signalChannelSnapshot("zero_signal", ConditionSignalChannelSnapshot.of("mission.empty", 0, 0, 0, 0))
                .signalHistorySnapshot("signal_history", signalHistory())
                .signalHistorySnapshot("empty_history", ConditionSignalHistorySnapshot.empty())
                .logicChainSnapshot("chain", logicChain(true))
                .logicChainSnapshot("no_cycle_chain", logicChain(false))
                .logicChainSnapshot("empty_chain", new ConditionLogicChainSnapshot("", "", List.of(), List.of(), java.util.Set.of(), false, 0))
                .build();
    }

    private static ConditionEvaluationContext noPlayerContext() {
        return ConditionEvaluationContext.builder()
                .regionSnapshot("region", new ConditionRegionSnapshot("mission.lobby", "大厅区域", true, "minecraft:overworld", List.of("player-1"), "", Map.of()))
                .build();
    }

    private static ConditionSignalChannelSnapshot signalChannel() {
        return new ConditionSignalChannelSnapshot(
                "MISSION.START",
                3,
                2,
                1,
                4,
                List.of(
                        ConditionSignalEventSnapshot.of("mission.start", "listener", "listener.a", 100L),
                        ConditionSignalEventSnapshot.of("mission.start", "relay", "relay.b", 120L)
                )
        );
    }

    private static ConditionSignalHistorySnapshot signalHistory() {
        return new ConditionSignalHistorySnapshot(List.of(
                ConditionSignalEventSnapshot.of("mission.start", "listener", "listener.a", 100L),
                ConditionSignalEventSnapshot.of("mission.start", "relay", "relay.b", 120L),
                ConditionSignalEventSnapshot.of("mission.stop", "region", "region.c", 180L)
        ));
    }

    private static ConditionLogicChainSnapshot logicChain(boolean cycle) {
        List<ConditionLogicChainNodeSnapshot> nodes = List.of(
                ConditionLogicChainNodeSnapshot.of("channel:mission.start", "channel", "mission.start"),
                ConditionLogicChainNodeSnapshot.of("listener:mission.start", "consumer", "mission.start"),
                ConditionLogicChainNodeSnapshot.of("action:notify", "action", ""),
                ConditionLogicChainNodeSnapshot.of("channel:mission.success", "channel", "mission.success")
        );
        List<ConditionLogicChainEdgeSnapshot> edges = cycle
                ? List.of(
                ConditionLogicChainEdgeSnapshot.of("channel:mission.start", "listener:mission.start", "consumes"),
                ConditionLogicChainEdgeSnapshot.of("listener:mission.start", "action:notify", "executes"),
                ConditionLogicChainEdgeSnapshot.of("action:notify", "channel:mission.success", "emits_downstream"),
                ConditionLogicChainEdgeSnapshot.of("channel:mission.success", "channel:mission.start", "reference")
        )
                : List.of(
                ConditionLogicChainEdgeSnapshot.of("channel:mission.start", "listener:mission.start", "consumes"),
                ConditionLogicChainEdgeSnapshot.of("listener:mission.start", "action:notify", "executes"),
                ConditionLogicChainEdgeSnapshot.of("action:notify", "channel:mission.success", "emits_downstream")
        );
        return ConditionLogicChainSnapshot.of("mission.start", "channel:mission.start", nodes, edges, List.of("mission.start", "mission.success"), false, 3);
    }

    private static List<String> regionSignalLogicTypes() {
        return List.of(
                ConditionNodeType.REGION_EXISTS,
                ConditionNodeType.REGION_ENABLED,
                ConditionNodeType.PLAYER_IN_REGION,
                ConditionNodeType.REGION_PLAYER_COUNT_COMPARE,
                ConditionNodeType.SIGNAL_CHANNEL_EXISTS,
                ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE,
                ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE,
                ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL,
                ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE,
                ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE
        );
    }

    private static ConditionNode leaf(String type) {
        return ConditionNode.leaf(type, type);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf(type, type, config);
    }

    private static ConditionNodeConfig countConfig(String key, String keyName, String operator, String count) {
        return config(keyName, key, "operator", operator, "count", count);
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

    private static void requireThrows(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError(message + " expected UnsupportedOperationException");
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
