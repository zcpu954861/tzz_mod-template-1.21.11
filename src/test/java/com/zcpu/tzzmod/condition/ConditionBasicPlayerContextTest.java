package com.zcpu.tzzmod.condition;

import java.util.List;
import java.util.Map;

public final class ConditionBasicPlayerContextTest {
    private ConditionBasicPlayerContextTest() {
    }

    public static void run() {
        testPlayerExistsAndOnline();
        testPlayerOpTagsTeamGamemodeAndAliveState();
        testContextIdConditions();
        testGameTimeCompare();
        testEventMetadataConditions();
        testMissingContextSafeFailures();
        testInvalidConfigValidation();
        testChineseMetadataAndFailureReasons();
        testGroupsWithNewConditions();
    }

    private static void testPlayerExistsAndOnline() {
        ConditionEvaluationContext context = richContext();
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_EXISTS), context).matched(), "player_exists true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_EXISTS), ConditionEvaluationContext.builder().build()).matched(), "player_exists false");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_ONLINE), context).matched(), "player_online true");

        ConditionEvaluationContext offline = ConditionEvaluationContext.builder()
                .player("uuid-1", "Runner")
                .playerOnline(false)
                .build();
        ConditionEvaluationResult offlineResult = evaluate(leaf(ConditionNodeType.PLAYER_ONLINE), offline);
        requireFalse(offlineResult.matched(), "player_online false");
        requireContains(offlineResult.failureReason(), "玩家不在线", "player_online Chinese failure");
    }

    private static void testPlayerOpTagsTeamGamemodeAndAliveState() {
        ConditionEvaluationContext context = richContext();
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_IS_OP), context).matched(), "player_is_op true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_IS_OP, config("expected", "false")), context).matched(), "player_is_op expected false mismatch");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")), context).matched(), "player_has_tag true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "catcher")), context).matched(), "player_has_tag false");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_LACKS_TAG, config("tag", "catcher")), context).matched(), "player_lacks_tag true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_LACKS_TAG, config("tag", "runner")), context).matched(), "player_lacks_tag false");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_TEAM_EQUALS, config("team", "tzz_team_runner")), context).matched(), "player_team_equals true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_TEAM_EQUALS, config("team", "tzz_team_catcher")), context).matched(), "player_team_equals false");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_GAMEMODE_EQUALS, config("gamemode", "survival")), context).matched(), "player_gamemode_equals true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_GAMEMODE_EQUALS, config("gamemode", "spectator")), context).matched(), "player_gamemode_equals false");
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_ALIVE), context).matched(), "player_alive true");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_DEAD), context).matched(), "player_dead false");

        ConditionEvaluationContext dead = ConditionEvaluationContext.builder()
                .player("uuid-2", "DeadRunner")
                .playerAlive(false)
                .build();
        requireTrue(evaluate(leaf(ConditionNodeType.PLAYER_DEAD), dead).matched(), "player_dead true");
    }

    private static void testContextIdConditions() {
        ConditionEvaluationContext context = richContext();
        requireTrue(evaluate(leaf(ConditionNodeType.CHANNEL_EQUALS, config("channel", "MISSION.START")), context).matched(), "channel_equals normalizes true");
        ConditionEvaluationResult channelMismatch = evaluate(leaf(ConditionNodeType.CHANNEL_EQUALS, config("channel", "mission.stop")), context);
        requireFalse(channelMismatch.matched(), "channel_equals mismatch");
        requireContains(channelMismatch.failureReason(), "信号频道不匹配", "channel mismatch Chinese failure");
        requireTrue(evaluate(leaf(ConditionNodeType.SOURCE_TYPE_EQUALS, config("sourceType", "signal_device")), context).matched(), "source_type_equals true");
        requireFalse(evaluate(leaf(ConditionNodeType.SOURCE_TYPE_EQUALS, config("sourceType", "command")), context).matched(), "source_type_equals false");
        requireTrue(evaluate(leaf(ConditionNodeType.SOURCE_ID_EQUALS, config("sourceId", "device-1")), context).matched(), "source_id_equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.WORLD_EQUALS, config("world", "minecraft:overworld")), context).matched(), "world_equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.DEVICE_ID_EQUALS, config("deviceId", "device-1")), context).matched(), "device_id_equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.LISTENER_ID_EQUALS, config("listenerId", "listener-1")), context).matched(), "listener_id_equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.REGION_ID_EQUALS, config("regionId", "region-1")), context).matched(), "region_id_equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.ACTION_ID_EQUALS, config("actionId", "action-1")), context).matched(), "action_id_equals true");
        requireFalse(evaluate(leaf(ConditionNodeType.ACTION_ID_EQUALS, config("actionId", "action-2")), context).matched(), "action_id_equals false");
    }

    private static void testGameTimeCompare() {
        ConditionEvaluationContext context = richContext();
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "eq", "value", "120")), context).matched(), "game_time eq");
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "ne", "value", "121")), context).matched(), "game_time ne");
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "gt", "value", "119")), context).matched(), "game_time gt");
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "gte", "value", "120")), context).matched(), "game_time gte");
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "lt", "value", "121")), context).matched(), "game_time lt");
        requireTrue(evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "lte", "value", "120")), context).matched(), "game_time lte");
        ConditionEvaluationResult failed = evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "gte", "value", "2400")), context);
        requireFalse(failed.matched(), "game_time failed");
        requireContains(failed.failureReason(), "游戏时间不满足", "game_time Chinese failure");
    }

    private static void testEventMetadataConditions() {
        ConditionEvaluationContext context = richContext();
        requireTrue(evaluate(leaf(ConditionNodeType.EVENT_METADATA_EXISTS, config("key", "missionPhase")), context).matched(), "metadata exists true");
        requireFalse(evaluate(leaf(ConditionNodeType.EVENT_METADATA_EXISTS, config("key", "missing")), context).matched(), "metadata exists false");
        requireTrue(evaluate(leaf(ConditionNodeType.EVENT_METADATA_EQUALS, config("key", "missionPhase", "value", "start")), context).matched(), "metadata equals true");
        ConditionEvaluationResult mismatch = evaluate(leaf(ConditionNodeType.EVENT_METADATA_EQUALS, config("key", "missionPhase", "value", "end")), context);
        requireFalse(mismatch.matched(), "metadata equals false");
        requireContains(mismatch.failureReason(), "事件元数据不匹配", "metadata Chinese failure");
    }

    private static void testMissingContextSafeFailures() {
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")), null).matched(), "missing context player tag fails");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_LACKS_TAG, config("tag", "runner")), null).matched(), "missing context lacks tag still fails");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_DEAD), null).matched(), "missing context player_dead fails");
        requireFalse(evaluate(leaf(ConditionNodeType.PLAYER_DEAD), ConditionEvaluationContext.builder().build()).matched(), "missing player player_dead fails");
        requireFalse(evaluate(leaf(ConditionNodeType.CHANNEL_EQUALS, config("channel", "mission.start")), null).matched(), "missing context channel fails");
        requireFalse(evaluate(leaf(ConditionNodeType.SOURCE_TYPE_EQUALS, config("sourceType", "signal_device")), ConditionEvaluationContext.builder().build()).matched(), "missing sourceType fails");
        requireFalse(evaluate(leaf(ConditionNodeType.DEVICE_ID_EQUALS, config("deviceId", "device-1")), ConditionEvaluationContext.builder().build()).matched(), "missing deviceId fails");
        requireFalse(evaluate(leaf(ConditionNodeType.LISTENER_ID_EQUALS, config("listenerId", "listener-1")), ConditionEvaluationContext.builder().build()).matched(), "missing listenerId fails");
        requireFalse(evaluate(leaf(ConditionNodeType.REGION_ID_EQUALS, config("regionId", "region-1")), ConditionEvaluationContext.builder().build()).matched(), "missing regionId fails");
        requireFalse(evaluate(leaf(ConditionNodeType.ACTION_ID_EQUALS, config("actionId", "action-1")), ConditionEvaluationContext.builder().build()).matched(), "missing actionId fails");
        ConditionEvaluationResult world = evaluate(leaf(ConditionNodeType.WORLD_EQUALS, config("world", "minecraft:overworld")), ConditionEvaluationContext.builder().build());
        requireFalse(world.matched(), "missing world fails");
        requireContains(world.failureReason(), "上下文缺少世界", "missing world Chinese failure");
        ConditionEvaluationResult gameTime = evaluate(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "eq", "value", "0")), null);
        requireFalse(gameTime.matched(), "missing context game_time fails");
        requireContains(gameTime.failureReason(), "上下文不存在，无法读取游戏时间", "missing game time Chinese failure");
        requireFalse(evaluate(leaf(ConditionNodeType.EVENT_METADATA_EQUALS, config("key", "phase", "value", "start")), null).matched(), "missing context metadata equals fails");
    }

    private static void testInvalidConfigValidation() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.PLAYER_HAS_TAG))), "condition_config_missing_tag", "missing tag validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.PLAYER_IS_OP, config("expected", "yes")))), "condition_config_invalid_expected", "invalid expected validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.PLAYER_GAMEMODE_EQUALS, config("gamemode", "flying")))), "condition_config_invalid_gamemode", "invalid gamemode validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.CHANNEL_EQUALS, config("channel", "Mission Start!")))), "condition_config_invalid_channel", "invalid channel validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "between", "value", "120")))), "condition_config_invalid_operator", "invalid operator validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.GAME_TIME_COMPARE, config("operator", "gte", "value", "abc")))), "condition_config_invalid_game_time", "invalid game time validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.EVENT_METADATA_EXISTS))), "condition_config_missing_key", "missing metadata key validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.EVENT_METADATA_EQUALS, config("key", "phase")))), "condition_config_missing_value", "missing metadata value validation");

        ConditionEvaluationContext nonOp = ConditionEvaluationContext.builder()
                .player("uuid", "NonOp")
                .playerOp(false)
                .build();
        ConditionEvaluationResult invalidExpected = evaluate(leaf(ConditionNodeType.PLAYER_IS_OP, config("expected", "yes")), nonOp);
        requireTrue(invalidExpected.error(), "invalid expected evaluation is error");
        requireFalse(invalidExpected.matched(), "invalid expected evaluation fails");
        ConditionEvaluationResult missingTag = evaluate(leaf(ConditionNodeType.PLAYER_LACKS_TAG), richContext());
        requireTrue(missingTag.error(), "missing tag evaluation is error");
        requireFalse(missingTag.matched(), "missing tag evaluation fails");
    }

    private static void testChineseMetadataAndFailureReasons() {
        ConditionRegistry registry = ConditionRegistry.defaultRegistry();
        requireEquals("玩家拥有标签", registry.metadata(ConditionNodeType.PLAYER_HAS_TAG).orElseThrow().displayName(), "player_has_tag Chinese display");
        requireEquals("标签", registry.metadata(ConditionNodeType.PLAYER_HAS_TAG).orElseThrow().fields().getFirst().displayName(), "player_has_tag Chinese field");
        requireEquals("信号频道匹配", registry.metadata(ConditionNodeType.CHANNEL_EQUALS).orElseThrow().displayName(), "channel Chinese display");
        requireEquals("永远通过", registry.metadata(ConditionNodeType.ALWAYS_TRUE).orElseThrow().displayName(), "always_true Chinese display");
        requireEquals("上下文存在", registry.metadata(ConditionNodeType.CONTEXT_EXISTS).orElseThrow().displayName(), "context_exists Chinese display");

        ConditionEvaluationResult result = evaluate(leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")), ConditionEvaluationContext.builder().player("uuid", "NoTag").build());
        requireContains(result.failureReason(), "玩家缺少标签", "player tag Chinese failure");
        requireEquals("玩家拥有标签", result.label(), "result label uses Chinese display");
    }

    private static void testGroupsWithNewConditions() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                leaf(ConditionNodeType.PLAYER_EXISTS),
                leaf(ConditionNodeType.CHANNEL_EQUALS, config("channel", "mission.start")),
                ConditionNode.not("not_catcher", leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "catcher"))),
                ConditionNode.group("or", ConditionGroupMode.OR, List.of(
                        leaf(ConditionNodeType.SOURCE_ID_EQUALS, config("sourceId", "missing")),
                        leaf(ConditionNodeType.REGION_ID_EQUALS, config("regionId", "region-1"))
                ))
        ));
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("group81", root), richContext());
        requireTrue(result.matched(), "groups with 8.1 conditions match");
        requireTrue(result.evaluatedNodeCount() >= 7, "groups count evaluated nodes");
    }

    private static ConditionEvaluationContext richContext() {
        return ConditionEvaluationContext.builder()
                .player("uuid-1", "Runner")
                .playerOnline(true)
                .playerOp(true)
                .playerTag("runner")
                .playerTeam("tzz_team_runner")
                .playerGameMode("survival")
                .playerAlive(true)
                .worldId("minecraft:overworld")
                .source("signal_device", "device-1")
                .channel("mission.start")
                .deviceId("device-1")
                .listenerId("listener-1")
                .regionId("region-1")
                .actionId("action-1")
                .gameTime(120L)
                .eventMetadata("missionPhase", "start")
                .build();
    }

    private static ConditionNode leaf(String type) {
        return ConditionNode.leaf(type, type);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf(type, type, config);
    }

    private static ConditionNodeConfig config(String key, String value) {
        return ConditionNodeConfig.of(key, value);
    }

    private static ConditionNodeConfig config(String key1, String value1, String key2, String value2) {
        return new ConditionNodeConfig(Map.of(key1, value1, key2, value2));
    }

    private static ConditionGroupDefinition def(ConditionNode node) {
        return ConditionGroupDefinition.of("test", node);
    }

    private static ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
        return new ConditionEvaluator().evaluate(def(node), context);
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
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message);
    }

    private static void requireIssue(ConditionValidationResult result, String code, String message) {
        requireTrue(result.issues().stream().anyMatch((issue) -> code.equals(issue.code()) && containsChinese(issue.message())), message);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
