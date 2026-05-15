package com.zcpu.tzzmod.condition;

import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableService;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableUpdateRequest;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.condition.state.StateVariableWriteResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ConditionStateVariableTest {
    private ConditionStateVariableTest() {
    }

    public static void run() throws Exception {
        testCompleteScopeTypeLifecycleMatrix();
        testStoreAndService();
        testStoreCorruptionFallback();
        testExistsAndBooleanConditions();
        testIntegerCompareConditions();
        testStringConditions();
        testConditionFailureCoverageMatrix();
        testMissingAndTypeMismatchSafeFailures();
        testInvalidConfigValidation();
        testChineseMetadataAndFailureReasons();
        testGroupIntegrationAndNoSideEffects();
    }

    private static void testCompleteScopeTypeLifecycleMatrix() throws Exception {
        StateVariableService service = tempService();
        for (VariableCase variableCase : List.of(
                new VariableCase(StateVariableScope.GLOBAL, "", "matrix.global_bool", StateVariableType.BOOLEAN, "true", "false", "false"),
                new VariableCase(StateVariableScope.GLOBAL, "", "matrix.global_int", StateVariableType.INTEGER, "1", "2", "2"),
                new VariableCase(StateVariableScope.GLOBAL, "", "matrix.global_string", StateVariableType.STRING, "alpha", "beta", "beta"),
                new VariableCase(StateVariableScope.PLAYER, "player-matrix", "matrix.player_bool", StateVariableType.BOOLEAN, "true", "false", "false"),
                new VariableCase(StateVariableScope.PLAYER, "player-matrix", "matrix.player_int", StateVariableType.INTEGER, "7", "8", "8"),
                new VariableCase(StateVariableScope.PLAYER, "player-matrix", "matrix.player_string", StateVariableType.STRING, "runner", "catcher", "catcher")
        )) {
            StateVariableWriteResult created = service.set(update(variableCase.scope(), variableCase.targetId(), variableCase.key(), variableCase.type(), variableCase.initialValue()), "tester");
            requireTrue(created.success(), variableCase.label() + " create");
            requireTrue(created.changed(), variableCase.label() + " create changed");

            StateVariableRecord read = service.snapshot().get(variableCase.scope(), variableCase.targetId(), variableCase.key()).orElseThrow();
            requireEquals(StateVariableValidation.normalizeTargetId(variableCase.scope(), variableCase.targetId()), read.targetId(), variableCase.label() + " normalized target");
            requireEquals(variableCase.type(), read.type(), variableCase.label() + " read type");
            requireEquals(StateVariableValidation.normalizeValue(variableCase.type(), variableCase.initialValue()), read.value(), variableCase.label() + " read value");

            StateVariableWriteResult updated = service.set(new StateVariableUpdateRequest(
                    variableCase.scope(),
                    variableCase.targetId(),
                    variableCase.key(),
                    variableCase.type(),
                    variableCase.updatedValue(),
                    "",
                    "",
                    read.fingerprint()
            ), "tester");
            requireTrue(updated.success(), variableCase.label() + " update");
            requireEquals(read.version() + 1L, updated.record().version(), variableCase.label() + " update increments version");
            requireFalse(read.fingerprint().equals(updated.record().fingerprint()), variableCase.label() + " update changes fingerprint");
            StateVariableRecord updatedRead = service.snapshot().get(variableCase.scope(), variableCase.targetId(), variableCase.key()).orElseThrow();
            requireEquals(variableCase.expectedUpdatedValue(), updatedRead.value(), variableCase.label() + " updated value");

            StateVariableWriteResult deleted = service.remove(variableCase.scope(), variableCase.targetId(), variableCase.key(), updatedRead.fingerprint());
            requireTrue(deleted.success(), variableCase.label() + " delete");
            requireFalse(service.snapshot().get(variableCase.scope(), variableCase.targetId(), variableCase.key()).isPresent(), variableCase.label() + " delete absent");
        }
        requireEquals(0, service.snapshot().size(), "complete lifecycle matrix deletes all variables");
    }

    private static void testStoreAndService() throws Exception {
        StateVariableService service = tempService();

        StateVariableWriteResult globalBool = service.set(update(StateVariableScope.GLOBAL, "", "game.active", StateVariableType.BOOLEAN, "true"), "tester");
        requireTrue(globalBool.success(), "create global bool");
        requireTrue(globalBool.changed(), "global bool changed");
        requireContains(globalBool.record().id(), "game.active", "global bool id includes key");

        StateVariableWriteResult globalInt = service.set(update(StateVariableScope.GLOBAL, "", "mission.count", StateVariableType.INTEGER, "2"), "tester");
        StateVariableWriteResult globalString = service.set(update(StateVariableScope.GLOBAL, "", "mission.phase", StateVariableType.STRING, "waiting"), "tester");
        StateVariableWriteResult playerBool = service.set(update(StateVariableScope.PLAYER, "player-1", "player.certified", StateVariableType.BOOLEAN, "false"), "tester");
        StateVariableWriteResult playerInt = service.set(update(StateVariableScope.PLAYER, "player-1", "player.score", StateVariableType.INTEGER, "9"), "tester");
        StateVariableWriteResult playerString = service.set(update(StateVariableScope.PLAYER, "player-1", "player.role", StateVariableType.STRING, "runner"), "tester");

        requireTrue(globalInt.success() && globalString.success() && playerBool.success() && playerInt.success() && playerString.success(), "create all state variable types");
        requireEquals(6, service.snapshot().size(), "snapshot includes six records");

        StateVariableRecord beforeUpdate = globalInt.record();
        StateVariableWriteResult updated = service.set(new StateVariableUpdateRequest(
                StateVariableScope.GLOBAL,
                "",
                "mission.count",
                StateVariableType.INTEGER,
                "3",
                "任务计数",
                "测试更新",
                beforeUpdate.fingerprint()
        ), "tester");
        requireTrue(updated.success(), "update variable");
        requireEquals(2L, updated.record().version(), "update increments version");
        requireFalse(beforeUpdate.fingerprint().equals(updated.record().fingerprint()), "update changes fingerprint");

        StateVariableWriteResult conflict = service.set(new StateVariableUpdateRequest(
                StateVariableScope.GLOBAL,
                "",
                "mission.count",
                StateVariableType.INTEGER,
                "4",
                "",
                "",
                beforeUpdate.fingerprint()
        ), "tester");
        requireFalse(conflict.success(), "stale fingerprint rejected");
        requireEquals("fingerprint_mismatch", conflict.code(), "fingerprint conflict code");

        StateVariableWriteResult staleDelete = service.remove(StateVariableScope.GLOBAL, "", "mission.count", beforeUpdate.fingerprint());
        requireFalse(staleDelete.success(), "stale delete fingerprint rejected");
        requireEquals("fingerprint_mismatch", staleDelete.code(), "delete fingerprint conflict code");

        StateVariableWriteResult deleted = service.remove(StateVariableScope.PLAYER, "player-1", "player.role", playerString.record().fingerprint());
        requireTrue(deleted.success(), "delete variable");
        requireEquals(5, service.snapshot().size(), "snapshot after delete");

        requireFalse(service.set(update(StateVariableScope.GLOBAL, "", "Bad Key!", StateVariableType.STRING, "x"), "tester").success(), "invalid key rejected");
        requireFalse(service.set(update(StateVariableScope.GLOBAL, "", "game.invalid", StateVariableType.INTEGER, "abc"), "tester").success(), "invalid integer rejected");
        requireFalse(service.set(new StateVariableUpdateRequest(StateVariableScope.GLOBAL, "", "game.invalid", null, "x", "", "", ""), "tester").success(), "invalid type rejected");
        requireChineseValidation(service.set(update(StateVariableScope.PLAYER, "", "player.missing", StateVariableType.BOOLEAN, "true"), "tester"), "missing target Chinese validation");
        requireChineseValidation(service.set(new StateVariableUpdateRequest(null, "", "game.invalid", StateVariableType.BOOLEAN, "true", "", "", ""), "tester"), "invalid scope Chinese validation");
        requireChineseValidation(service.set(update(StateVariableScope.GLOBAL, "player-ignored", "game.bad_bool", StateVariableType.BOOLEAN, "yes"), "tester"), "invalid bool Chinese validation");
        requireChineseValidation(service.set(update(StateVariableScope.GLOBAL, "", "game.bad_string", StateVariableType.STRING, "bad\nvalue"), "tester"), "invalid string Chinese validation");
        StateVariableWriteResult globalTargetNormalized = service.set(update(StateVariableScope.GLOBAL, "player-ignored", "game.normalized", StateVariableType.STRING, "ok"), "tester");
        requireTrue(globalTargetNormalized.success(), "global player target normalized");
        requireEquals(StateVariableValidation.GLOBAL_TARGET, globalTargetNormalized.record().targetId(), "global target normalized to global");

        StateVariableRecord collisionLeft = record(StateVariableScope.PLAYER, "a:b", "c", StateVariableType.STRING, "left");
        StateVariableRecord collisionRight = record(StateVariableScope.PLAYER, "a", "b:c", StateVariableType.STRING, "right");
        requireFalse(collisionLeft.id().equals(collisionRight.id()), "length-prefixed stable id avoids target/key delimiter collisions");

        Path explicitPath = Files.createTempDirectory("tzz-state-var-store").resolve(StateVariableStore.FILE_NAME);
        StateVariableStore.saveSnapshot(explicitPath, service.snapshot());
        requireEquals(service.snapshot().size(), StateVariableStore.loadSnapshot(explicitPath).size(), "store load/save round trip");
    }

    private static void testStoreCorruptionFallback() throws Exception {
        Path invalidJson = Files.createTempDirectory("tzz-state-var-corrupt-json").resolve(StateVariableStore.FILE_NAME);
        Files.writeString(invalidJson, "{not-json");
        requireEquals(0, StateVariableStore.loadSnapshot(invalidJson).size(), "corrupt json falls back to empty snapshot");

        Path invalidRecord = Files.createTempDirectory("tzz-state-var-corrupt-record").resolve(StateVariableStore.FILE_NAME);
        Files.writeString(invalidRecord, """
                {
                  "version": 1,
                  "variables": [
                    {
                      "id": "broken",
                      "scope": "GLOBAL",
                      "targetId": "global",
                      "key": "mission.count",
                      "type": "INTEGER",
                      "value": "not-number",
                      "version": 1
                    },
                    {
                      "scope": "GLOBAL",
                      "targetId": "global",
                      "key": "game.active",
                      "type": "BOOLEAN",
                      "value": "true",
                      "version": 1
                    }
                  ]
                }
                """);
        StateVariableSnapshot snapshot = StateVariableStore.loadSnapshot(invalidRecord);
        requireEquals(0, snapshot.size(), "corrupt persisted record falls back to empty snapshot");
    }

    private static void testExistsAndBooleanConditions() {
        ConditionEvaluationContext context = contextWithState();
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, globalConfig("game.active")), context).matched(), "state exists true");
        ConditionEvaluationResult missing = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, globalConfig("game.missing")), context);
        requireFalse(missing.matched(), "state exists false");
        requireContains(missing.failureReason(), "状态变量不存在", "missing variable Chinese failure");

        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "true")), context).matched(), "bool equals true");
        ConditionEvaluationResult boolFalse = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "false")), context);
        requireFalse(boolFalse.matched(), "bool equals false");
        requireContains(boolFalse.failureReason(), "布尔状态不匹配", "bool Chinese failure");

        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.certified", "targetMode", "context_player", "expected", "true")), context).matched(), "player bool context target");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.ready", "targetMode", "explicit_target", "targetId", "player-2", "expected", "false")), context).matched(), "player bool explicit target");
    }

    private static void testIntegerCompareConditions() {
        ConditionEvaluationContext context = contextWithState();
        requireTrue(intCompare("eq", "2", context).matched(), "int eq");
        requireTrue(intCompare("ne", "3", context).matched(), "int ne");
        requireTrue(intCompare("gt", "1", context).matched(), "int gt");
        requireTrue(intCompare("gte", "2", context).matched(), "int gte");
        requireTrue(intCompare("lt", "3", context).matched(), "int lt");
        requireTrue(intCompare("lte", "2", context).matched(), "int lte");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "PLAYER", "key", "player.score", "targetMode", "context_player", "operator", "gte", "value", "9")), context).matched(), "player int context target");

        ConditionEvaluationResult failed = intCompare("gte", "3", context);
        requireFalse(failed.matched(), "int compare false");
        requireContains(failed.failureReason(), "整数状态不满足", "int Chinese failure");
    }

    private static void testStringConditions() {
        ConditionEvaluationContext context = contextWithState();
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "waiting")), context).matched(), "string equals true");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "WAITING", "ignoreCase", "true")), context).matched(), "string equals ignore case");

        ConditionEvaluationResult mismatch = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "running")), context);
        requireFalse(mismatch.matched(), "string equals false");
        requireContains(mismatch.failureReason(), "文本状态不匹配", "string equals Chinese failure");

        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "ait")), context).matched(), "string contains true");
        requireTrue(evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "PLAYER", "key", "player.role", "targetMode", "context_player", "value", "RUN", "ignoreCase", "true")), context).matched(), "player string contains ignore case");
        ConditionEvaluationResult notContains = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "done")), context);
        requireFalse(notContains.matched(), "string contains false");
        requireContains(notContains.failureReason(), "文本状态不包含", "string contains Chinese failure");
    }

    private static void testConditionFailureCoverageMatrix() {
        ConditionEvaluationContext context = contextWithState();
        ConditionEvaluationContext noPlayerContext = ConditionEvaluationContext.builder().stateVariables(context.stateVariables()).build();

        ConditionEvaluationResult existsWrongTypeNotApplicable = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, globalConfig("mission.count")), context);
        requireTrue(existsWrongTypeNotApplicable.matched(), "state exists is type agnostic");
        requireContains(existsWrongTypeNotApplicable.debugSummary(), "状态变量存在", "exists Chinese success summary");
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "PLAYER", "key", "player.certified", "targetMode", "context_player")), noPlayerContext),
                "上下文缺少触发玩家",
                "exists missing player"
        );

        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.missing_bool", "targetMode", "global", "expected", "true")), context),
                "状态变量不存在",
                "bool missing variable"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.certified", "targetMode", "context_player", "expected", "true")), noPlayerContext),
                "上下文缺少触发玩家",
                "bool missing player"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "expected", "true")), context),
                "状态变量类型不匹配",
                "bool wrong type"
        );

        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "game.missing_int", "targetMode", "global", "operator", "gte", "value", "1")), context),
                "状态变量不存在",
                "int missing variable"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "PLAYER", "key", "player.score", "targetMode", "context_player", "operator", "gte", "value", "1")), noPlayerContext),
                "上下文缺少触发玩家",
                "int missing player"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "operator", "gte", "value", "1")), context),
                "状态变量类型不匹配",
                "int wrong type"
        );

        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "game.missing_string", "targetMode", "global", "value", "x")), context),
                "状态变量不存在",
                "string equals missing variable"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "PLAYER", "key", "player.role", "targetMode", "context_player", "value", "runner")), noPlayerContext),
                "上下文缺少触发玩家",
                "string equals missing player"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "value", "true")), context),
                "状态变量类型不匹配",
                "string equals wrong type"
        );

        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "game.missing_contains", "targetMode", "global", "value", "x")), context),
                "状态变量不存在",
                "string contains missing variable"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "PLAYER", "key", "player.role", "targetMode", "context_player", "value", "run")), noPlayerContext),
                "上下文缺少触发玩家",
                "string contains missing player"
        );
        assertStateFailure(
                evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "value", "2")), context),
                "状态变量类型不匹配",
                "string contains wrong type"
        );
    }

    private static void testMissingAndTypeMismatchSafeFailures() {
        ConditionEvaluationContext context = contextWithState();
        ConditionEvaluationResult missingPlayer = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.certified", "targetMode", "context_player", "expected", "true")), ConditionEvaluationContext.builder().stateVariables(context.stateVariables()).build());
        requireFalse(missingPlayer.matched(), "missing player fails safely");
        requireContains(missingPlayer.failureReason(), "上下文缺少触发玩家", "missing player Chinese failure");

        ConditionEvaluationResult wrongType = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "expected", "true")), context);
        requireFalse(wrongType.matched(), "wrong type fails safely");
        requireContains(wrongType.failureReason(), "状态变量类型不匹配", "type mismatch Chinese failure");

        ConditionEvaluationResult intWrongType = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "operator", "gte", "value", "1")), context);
        requireFalse(intWrongType.matched(), "int compare wrong type fails safely");
        requireContains(intWrongType.failureReason(), "状态变量类型不匹配", "int compare type mismatch Chinese failure");

        ConditionEvaluationResult stringEqualsWrongType = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "value", "2")), context);
        requireFalse(stringEqualsWrongType.matched(), "string equals wrong type fails safely");
        requireContains(stringEqualsWrongType.failureReason(), "状态变量类型不匹配", "string equals type mismatch Chinese failure");

        ConditionEvaluationResult stringContainsWrongType = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "value", "tru")), context);
        requireFalse(stringContainsWrongType.matched(), "string contains wrong type fails safely");
        requireContains(stringContainsWrongType.failureReason(), "状态变量类型不匹配", "string contains type mismatch Chinese failure");

        ConditionEvaluationResult nullContext = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, globalConfig("game.active")), null);
        requireFalse(nullContext.matched(), "null context fails safely");
        requireContains(nullContext.failureReason(), "状态变量不存在", "null context state Chinese failure");
    }

    private static void testInvalidConfigValidation() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS))), "condition_config_missing_scope", "missing scope validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "targetMode", "global")))), "condition_config_missing_key", "missing key validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "key", "game.active")))), "condition_config_missing_targetMode", "missing target mode validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "TEAM", "key", "game.active", "targetMode", "global")))), "condition_config_invalid_scope", "invalid scope validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "current_player")))), "condition_config_invalid_target_mode", "invalid target mode validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "context_player")))), "condition_config_invalid_target_mode", "global invalid target mode");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "PLAYER", "key", "player.ready", "targetMode", "global")))), "condition_config_invalid_target_mode", "player invalid global target mode");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "PLAYER", "key", "player.ready", "targetMode", "explicit_target")))), "condition_config_missing_target_id", "missing explicit target validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, config("scope", "GLOBAL", "key", "Bad Key!", "targetMode", "global")))), "condition_config_invalid_key", "invalid key validation");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, globalConfig("game.active")))), "condition_config_missing_expected", "missing bool expected");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "yes")))), "condition_config_invalid_expected", "invalid bool expected");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", "between", "value", "2")))), "condition_config_invalid_operator", "invalid int operator");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", "gte", "value", "two")))), "condition_config_invalid_state_integer", "invalid int value");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, globalConfig("mission.phase")))), "condition_config_missing_value", "missing string value");
        requireIssue(evaluator.validate(def(leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "wait", "ignoreCase", "maybe")))), "condition_config_invalid_ignoreCase", "invalid ignoreCase validation");
    }

    private static void testChineseMetadataAndFailureReasons() {
        ConditionRegistry registry = ConditionRegistry.defaultRegistry();
        requireEquals("状态变量存在", registry.metadata(ConditionNodeType.STATE_VARIABLE_EXISTS).orElseThrow().displayName(), "state exists Chinese display");
        requireEquals("作用域", registry.metadata(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS).orElseThrow().fields().get(0).displayName(), "state scope Chinese field");
        requireEquals("状态变量条件", registry.metadata(ConditionNodeType.STATE_VARIABLE_INT_COMPARE).orElseThrow().category(), "state Chinese category");

        ConditionEvaluationResult result = evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", "gte", "value", "3")), contextWithState());
        requireContains(result.failureReason(), "整数状态不满足", "state int Chinese failure");
        requireEquals("整数状态比较", result.label(), "state result label Chinese");
    }

    private static void testGroupIntegrationAndNoSideEffects() {
        ConditionEvaluationContext context = contextWithState();
        StateVariableSnapshot before = context.stateVariables();
        ConditionNode disabledFailingStateCondition = new ConditionNode(
                "disabled_state",
                ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                "",
                "",
                false,
                ConditionGroupMode.AND,
                config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "false"),
                List.of()
        );
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "true")),
                leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", "gte", "value", "2")),
                ConditionNode.not("not_finished", leaf(ConditionNodeType.STATE_VARIABLE_STRING_EQUALS, config("scope", "GLOBAL", "key", "mission.phase", "targetMode", "global", "value", "finished"))),
                ConditionNode.group("or", ConditionGroupMode.OR, List.of(
                        leaf(ConditionNodeType.STATE_VARIABLE_EXISTS, globalConfig("missing.flag")),
                        leaf(ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS, config("scope", "PLAYER", "key", "player.role", "targetMode", "context_player", "value", "run"))
                )),
                disabledFailingStateCondition
        ));
        ConditionEvaluationResult result = new ConditionEvaluator().evaluate(ConditionGroupDefinition.of("state_group", root), context);
        requireTrue(result.matched(), "group with state variables matches");
        requireTrue(result.childResults().stream().anyMatch(ConditionEvaluationResult::skipped), "disabled state condition is skipped");
        requireEquals(before.records(), context.stateVariables().records(), "condition evaluation has no side effects");

        evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "expected", "true")), context);
        requireEquals(before.records(), context.stateVariables().records(), "type mismatch evaluation has no side effects");
        evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "PLAYER", "key", "player.certified", "targetMode", "context_player", "expected", "true")), ConditionEvaluationContext.builder().stateVariables(before).build());
        requireEquals(before.records(), context.stateVariables().records(), "missing player evaluation has no side effects");

        try {
            Path path = Files.createTempDirectory("tzz-state-var-no-side-effect").resolve(StateVariableStore.FILE_NAME);
            StateVariableService service = new StateVariableService(path);
            service.set(update(StateVariableScope.GLOBAL, "", "game.active", StateVariableType.BOOLEAN, "true"), "tester");
            StateVariableRecord fileRecordBefore = service.snapshot().get(StateVariableScope.GLOBAL, "", "game.active").orElseThrow();
            String beforeFile = Files.readString(path);
            evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "false")), ConditionEvaluationContext.builder().stateVariables(service.snapshot()).build());
            evaluate(leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.missing", "targetMode", "global", "expected", "true")), ConditionEvaluationContext.builder().stateVariables(service.snapshot()).build());
            String afterFile = Files.readString(path);
            requireEquals(beforeFile, afterFile, "condition evaluation does not write state variable store file");
            StateVariableRecord fileRecordAfter = service.snapshot().get(StateVariableScope.GLOBAL, "", "game.active").orElseThrow();
            requireEquals(fileRecordBefore.version(), fileRecordAfter.version(), "condition evaluation does not change state variable version");
            requireEquals(fileRecordBefore.fingerprint(), fileRecordAfter.fingerprint(), "condition evaluation does not change state variable fingerprint");
            requireEquals(1, service.snapshot().size(), "failed condition evaluation does not create/delete variables");
        } catch (Exception exception) {
            throw new AssertionError("store file no side effect check failed", exception);
        }
    }

    private record VariableCase(
            StateVariableScope scope,
            String targetId,
            String key,
            StateVariableType type,
            String initialValue,
            String updatedValue,
            String expectedUpdatedValue
    ) {
        String label() {
            return scope + " " + type + " " + key;
        }
    }

    private static ConditionEvaluationResult intCompare(String operator, String value, ConditionEvaluationContext context) {
        return evaluate(leaf(ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config("scope", "GLOBAL", "key", "mission.count", "targetMode", "global", "operator", operator, "value", value)), context);
    }

    private static ConditionEvaluationContext contextWithState() {
        StateVariableSnapshot snapshot = StateVariableSnapshot.empty()
                .with(record(StateVariableScope.GLOBAL, "", "game.active", StateVariableType.BOOLEAN, "true"))
                .with(record(StateVariableScope.GLOBAL, "", "mission.count", StateVariableType.INTEGER, "2"))
                .with(record(StateVariableScope.GLOBAL, "", "mission.phase", StateVariableType.STRING, "waiting"))
                .with(record(StateVariableScope.PLAYER, "player-1", "player.certified", StateVariableType.BOOLEAN, "true"))
                .with(record(StateVariableScope.PLAYER, "player-1", "player.score", StateVariableType.INTEGER, "9"))
                .with(record(StateVariableScope.PLAYER, "player-1", "player.role", StateVariableType.STRING, "runner"))
                .with(record(StateVariableScope.PLAYER, "player-2", "player.ready", StateVariableType.BOOLEAN, "false"));
        return ConditionEvaluationContext.builder()
                .player("player-1", "Runner")
                .stateVariables(snapshot)
                .build();
    }

    private static StateVariableRecord record(StateVariableScope scope, String targetId, String key, StateVariableType type, String value) {
        return StateVariableRecord.create(scope, targetId, key, type, value, "", "", 1L, "test", 1L);
    }

    private static StateVariableService tempService() throws Exception {
        Path path = Files.createTempDirectory("tzz-state-var-service").resolve(StateVariableStore.FILE_NAME);
        return new StateVariableService(path);
    }

    private static StateVariableUpdateRequest update(StateVariableScope scope, String targetId, String key, StateVariableType type, String value) {
        return new StateVariableUpdateRequest(scope, targetId, key, type, value, "", "", "");
    }

    private static ConditionNode leaf(String type) {
        return ConditionNode.leaf(type, type);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf(type, type, config);
    }

    private static ConditionNodeConfig globalConfig(String key) {
        return config("scope", "GLOBAL", "key", key, "targetMode", "global");
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

    private static void requireChineseValidation(StateVariableWriteResult result, String message) {
        requireFalse(result.success(), message);
        requireTrue(result.validationErrors().stream().anyMatch(ConditionStateVariableTest::containsChinese), message + " validation error Chinese");
    }

    private static void assertStateFailure(ConditionEvaluationResult result, String expectedReasonPart, String message) {
        requireFalse(result.matched(), message);
        requireContains(result.failureReason(), expectedReasonPart, message + " failure reason");
        requireTrue(containsChinese(result.failureReason()), message + " Chinese failure reason");
    }

    private static void requireIssue(ConditionValidationResult result, String code, String message) {
        requireTrue(result.issues().stream().anyMatch((issue) -> code.equals(issue.code()) && containsChinese(issue.message())), message);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
