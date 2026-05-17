package com.zcpu.tzzmod.action;

import com.google.gson.Gson;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationResult;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableMutationResult;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableService;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ControlledStateActionServiceTest {
    private static final Gson GSON = new Gson();

    private ControlledStateActionServiceTest() {
    }

    public static void run() throws Exception {
        testSetVariableMatrixAndFailures();
        testIntegerMutations();
        testToggleAndClear();
        testTargetModesAndMissingPlayer();
        testReadAfterWriteConditions();
        testActionExecutionResultDetails();
        testActionConfigJsonCompatibility();
        testNoOutOfScopeScopes();
    }

    private static void testSetVariableMatrixAndFailures() throws Exception {
        StateVariableService service = tempService();
        StateVariableMutationResult globalBool = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                ""
        ), "test");
        requireSuccess(globalBool, "set missing GLOBAL boolean with createIfMissing");
        requireEquals("true", globalBool.newValue(), "set global bool value");

        StateVariableMutationResult globalString = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.phase",
                StateVariableType.STRING,
                "waiting",
                0,
                true,
                ""
        ), "test");
        requireSuccess(globalString, "set missing GLOBAL string");

        StateVariableMutationResult playerBool = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.PLAYER,
                StateVariableTargetMode.EXPLICIT_TARGET,
                "player-a",
                "",
                "player.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                ""
        ), "test");
        requireSuccess(playerBool, "set missing PLAYER explicit boolean");

        StateVariableMutationResult missing = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.missing",
                StateVariableType.STRING,
                "x",
                0,
                false,
                ""
        ), "test");
        requireFailure(missing, "missing_variable", "状态变量不存在", "set missing createIfMissing=false fails Chinese");

        StateVariableMutationResult mismatch = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.ready",
                StateVariableType.INTEGER,
                "1",
                0,
                true,
                ""
        ), "test");
        requireFailure(mismatch, "type_mismatch", "类型不匹配", "set type mismatch fails Chinese");

        StateVariableMutationResult invalidKey = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "Bad Key!",
                StateVariableType.STRING,
                "x",
                0,
                true,
                ""
        ), "test");
        requireFailure(invalidKey, "validation_error", "校验失败", "invalid key fails Chinese");
    }

    private static void testIntegerMutations() throws Exception {
        StateVariableService service = tempService();
        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", StateVariableType.INTEGER, "2", 0, true, ""), "test"), "seed integer");

        StateVariableMutationResult increment = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", StateVariableType.INTEGER, "", 5, false, ""), "test");
        requireSuccess(increment, "increment existing integer");
        requireEquals("7", increment.newValue(), "increment value");

        StateVariableMutationResult decrement = service.mutate(request(StateVariableMutationOperation.DECREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", StateVariableType.INTEGER, "", 10, false, ""), "test");
        requireSuccess(decrement, "decrement existing integer allows negative");
        requireEquals("-3", decrement.newValue(), "negative result documented by test");

        StateVariableMutationResult createIncrement = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.EXPLICIT_TARGET, "player-a", "", "player.score", StateVariableType.INTEGER, "", 4, true, "10"), "test");
        requireSuccess(createIncrement, "increment missing player integer with initialValue");
        requireEquals("14", createIncrement.newValue(), "increment missing initial base");

        StateVariableMutationResult invalidDelta = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", StateVariableType.INTEGER, "", 0, false, ""), "test");
        requireFailure(invalidDelta, "validation_error", "delta 必须大于 0", "delta <= 0 invalid Chinese");

        StateVariableMutationResult missingValueType = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", null, "", 1, false, ""), "test");
        requireFailure(missingValueType, "validation_error", "必须选择 INTEGER", "increment missing valueType invalid Chinese");

        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.label", StateVariableType.STRING, "x", 0, true, ""), "test"), "seed string");
        StateVariableMutationResult wrongType = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.label", StateVariableType.INTEGER, "", 1, false, ""), "test");
        requireFailure(wrongType, "type_mismatch", "只能用于 INTEGER", "increment non integer fails Chinese");

        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.big", StateVariableType.INTEGER, Long.toString(Long.MAX_VALUE), 0, true, ""), "test"), "seed max long");
        StateVariableMutationResult overflow = service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.big", StateVariableType.INTEGER, "", 1, false, ""), "test");
        requireFailure(overflow, "integer_overflow", "溢出", "integer overflow fails Chinese");
    }

    private static void testToggleAndClear() throws Exception {
        StateVariableService service = tempService();
        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.flag", StateVariableType.BOOLEAN, "true", 0, true, ""), "test"), "seed bool");

        StateVariableMutationResult toggleExisting = service.mutate(request(StateVariableMutationOperation.TOGGLE_BOOLEAN, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.flag", StateVariableType.BOOLEAN, "", 0, false, ""), "test");
        requireSuccess(toggleExisting, "toggle existing true to false");
        requireEquals("false", toggleExisting.newValue(), "toggle true -> false");

        StateVariableMutationResult toggleMissing = service.mutate(request(StateVariableMutationOperation.TOGGLE_BOOLEAN, StateVariableScope.PLAYER, StateVariableTargetMode.EXPLICIT_TARGET, "player-a", "", "player.ready", StateVariableType.BOOLEAN, "", 0, true, "false"), "test");
        requireSuccess(toggleMissing, "toggle missing createIfMissing");
        requireEquals("true", toggleMissing.newValue(), "toggle false base -> true");

        StateVariableMutationResult toggleMissingValueType = service.mutate(request(StateVariableMutationOperation.TOGGLE_BOOLEAN, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.flag", null, "", 0, false, ""), "test");
        requireFailure(toggleMissingValueType, "validation_error", "必须选择 BOOLEAN", "toggle missing valueType invalid Chinese");

        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.text", StateVariableType.STRING, "abc", 0, true, ""), "test"), "seed string");
        StateVariableMutationResult toggleWrongType = service.mutate(request(StateVariableMutationOperation.TOGGLE_BOOLEAN, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.text", StateVariableType.BOOLEAN, "", 0, false, ""), "test");
        requireFailure(toggleWrongType, "type_mismatch", "只能用于 BOOLEAN", "toggle non boolean fails Chinese");

        StateVariableMutationResult clearExisting = service.mutate(request(StateVariableMutationOperation.CLEAR_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.flag", null, "", 0, false, ""), "test");
        requireSuccess(clearExisting, "clear existing variable");
        requireTrue(clearExisting.changed(), "clear existing changed");
        requireFalse(service.snapshot().get(StateVariableScope.GLOBAL, "", "game.flag").isPresent(), "clear removes variable");

        StateVariableMutationResult clearMissing = service.mutate(request(StateVariableMutationOperation.CLEAR_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.missing", null, "", 0, false, ""), "test");
        requireSuccess(clearMissing, "clear missing no-op success");
        requireFalse(clearMissing.changed(), "clear missing changed=false");
    }

    private static void testTargetModesAndMissingPlayer() throws Exception {
        StateVariableService service = tempService();
        StateVariableMutationResult contextPlayer = service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.CONTEXT_PLAYER, "", "player-context", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, ""), "test");
        requireSuccess(contextPlayer, "PLAYER context_player writes context id");
        requireTrue(service.snapshot().get(StateVariableScope.PLAYER, "player-context", "player.ready").isPresent(), "context player target stored");

        StateVariableMutationResult missingPlayer = service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.CONTEXT_PLAYER, "", "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, ""), "test");
        requireFailure(missingPlayer, "missing_context_player", "需要触发玩家", "PLAYER context_player without player fails Chinese");

        StateVariableMutationResult missingExplicitTarget = service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.EXPLICIT_TARGET, "", "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, ""), "test");
        requireFailure(missingExplicitTarget, "validation_error", "显式目标", "PLAYER explicit_target missing target fails Chinese");
    }

    private static void testReadAfterWriteConditions() throws Exception {
        StateVariableService service = tempService();
        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.ready", StateVariableType.BOOLEAN, "true", 0, true, ""), "test"), "write ready");
        requireSuccess(service.mutate(request(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "mission.count", StateVariableType.INTEGER, "", 3, true, "2"), "test"), "write count");
        requireSuccess(service.mutate(request(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.EXPLICIT_TARGET, "player-a", "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, ""), "test"), "write player");

        ConditionEvaluationContext context = ConditionEvaluationContext.builder()
                .player("player-a", "Runner")
                .stateVariables(service.snapshot())
                .build();
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.ready", "targetMode", "global", "expected", "true")), context).matched(), "set boolean read-after-write condition");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", "eq", "value", "5")), context).matched(), "increment read-after-write condition");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.ready", "targetMode", "context_player", "expected", "true")), context).matched(), "player read-after-write condition");
        requireSuccess(service.mutate(request(StateVariableMutationOperation.CLEAR_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", "game.ready", null, "", 0, false, ""), "test"), "clear ready");
        ConditionEvaluationContext afterClear = ConditionEvaluationContext.builder()
                .player("player-a", "Runner")
                .stateVariables(service.snapshot())
                .build();
        requireFalse(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "key", "game.ready", "targetMode", "global")), afterClear).matched(), "clear read-after-write absence");
    }

    private static void testActionConfigJsonCompatibility() {
        ActionConfig legacy = GSON.fromJson("""
                {"type":"COMMAND","value":"say legacy","enabled":true,"requiresOp":false,"cooldownTicks":0,"notifyOps":false}
                """, ActionConfig.class);
        requireEquals(ActionType.COMMAND, legacy.type(), "legacy action type");
        requireEquals("say legacy", legacy.value(), "legacy value");
        requireEquals("", legacy.stateOperation(), "legacy has blank state operation");

        ActionConfig stateAction = ActionConfig.stateVariable(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "game.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                "",
                "allow"
        );
        requireTrue(stateAction.isUsable(), "state action is usable with blank legacy value");
        requireEquals("", stateAction.value(), "state action does not use raw JSON value");
        String json = GSON.toJson(stateAction);
        requireContains(json, "stateOperation", "state action typed field serializes");
        requireFalse(json.contains("rawJson"), "state action config does not expose raw JSON");
        ActionConfig roundTrip = GSON.fromJson(json, ActionConfig.class);
        requireEquals(ActionType.STATE_VARIABLE, roundTrip.type(), "state action type round trips");
        requireEquals("set_variable", roundTrip.stateOperation(), "state operation round trips");
        requireEquals("GLOBAL", roundTrip.stateScope(), "state scope round trips");
        requireEquals("global", roundTrip.stateTargetMode(), "state target mode round trips");
        requireEquals("game.ready", roundTrip.stateKey(), "state key round trips");
        requireEquals("BOOLEAN", roundTrip.stateValueType(), "state value type round trips");

        ActionConfig publicType = GSON.fromJson("""
                {"type":"state_variable","value":"","enabled":true,"requiresOp":false,"cooldownTicks":0,"notifyOps":false,"stateOperation":"increment_variable","stateScope":"GLOBAL","stateTargetMode":"global","stateKey":"mission.count","stateValueType":"INTEGER","stateDelta":1,"stateCreateIfMissing":true,"stateInitialValue":"0"}
                """, ActionConfig.class);
        requireEquals(ActionType.STATE_VARIABLE, publicType.type(), "public state_variable type id deserializes");
        requireEquals("increment_variable", publicType.stateOperation(), "public type keeps operation");
        requireContains(GSON.toJson(publicType), "state_variable", "public type serializes with lowercase id");
    }

    private static void testActionExecutionResultDetails() throws Exception {
        StateVariableService service = tempService();
        StateVariableMutationResult mutation = service.mutate(request(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                ""
        ), "test");
        ActionExecutionResult success = ActionExecutionResult.stateMutation(mutation);
        requireTrue(success.success(), "state mutation execution result succeeds");
        requireEquals("state_variable", success.details().get("actionType"), "execution result actionType detail");
        requireEquals("set_variable", success.details().get("operation"), "execution result operation detail");
        requireEquals("GLOBAL", success.details().get("scope"), "execution result scope detail");
        requireEquals("game.ready", success.details().get("key"), "execution result key detail");
        requireEquals("true", success.details().get("newValue"), "execution result new value detail");
        requireTrue(success.durationNanos() >= 0L, "execution result duration present");

        ActionConfig invalid = ActionConfig.stateVariable(
                StateVariableMutationOperation.INCREMENT_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "mission.count",
                null,
                "",
                1,
                false,
                "",
                ""
        );
        ActionExecutionResult validation = ActionEngine.execute(
                new ActionContext(null, null, null, ActionSourceType.SIGNAL_BRIDGE, "test", null),
                invalid
        );
        requireFalse(validation.success(), "ActionEngine.execute returns failure for invalid state action");
        requireEquals("validation_error", validation.code(), "ActionEngine state validation failure keeps state code");
        requireEquals("state_variable", validation.details().get("actionType"), "ActionEngine validation failure keeps state details");
        requireContains(String.valueOf(validation.details().get("validationErrors")), "INTEGER", "ActionEngine validation failure carries validation errors");
    }

    private static void testNoOutOfScopeScopes() {
        requireEquals(List.of(StateVariableScope.GLOBAL, StateVariableScope.PLAYER), List.of(StateVariableScope.values()), "8.11 does not add unsupported write scopes");
    }

    private static StateVariableMutationRequest request(
            StateVariableMutationOperation operation,
            StateVariableScope scope,
            StateVariableTargetMode targetMode,
            String targetId,
            String contextPlayerId,
            String key,
            StateVariableType valueType,
            String value,
            long delta,
            boolean createIfMissing,
            String initialValue
    ) {
        return new StateVariableMutationRequest(operation, scope, targetMode, targetId, contextPlayerId, key, valueType, value, delta, createIfMissing, initialValue);
    }

    private static ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
        return new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("controlled_state_action_test", node), context);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf(type, type, config);
    }

    private static ConditionNodeConfig config(String... entries) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static StateVariableService tempService() throws Exception {
        Path path = Files.createTempDirectory("tzz-controlled-state-action").resolve(StateVariableStore.FILE_NAME);
        return new StateVariableService(path);
    }

    private static void requireSuccess(StateVariableMutationResult result, String message) {
        requireTrue(result.success(), message + " result=" + result);
        requireTrue(containsChinese(result.message()), message + " message Chinese");
    }

    private static void requireFailure(StateVariableMutationResult result, String code, String reasonPart, String message) {
        requireFalse(result.success(), message + " should fail");
        requireEquals(code, result.code(), message + " code");
        requireContains(result.message() + " " + String.join(" ", result.validationErrors()), reasonPart, message + " Chinese reason");
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
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
