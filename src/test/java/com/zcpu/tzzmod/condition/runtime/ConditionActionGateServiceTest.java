package com.zcpu.tzzmod.condition.runtime;

import com.google.gson.Gson;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConditionActionGateServiceTest {
    private static final Gson GSON = new Gson();

    private ConditionActionGateServiceTest() {
    }

    public static void run() {
        testActionConfigJsonCompatibility();
        testBlankActionConditionSkipsRuntimeReadsAndHistory();
        testActionGateRecordsActionMetadata();
        testActionGateFalseBlocksOnlyCurrentActionDecision();
        testRuntimeLoopSemanticsSkipCurrentAndContinue();
        testStateActionGateFalseSkipsStateActionExecutionDecision();
        testManualBypassSkipsActionGateEvaluation();
        testActionGateRejectsIncompatibleProfileBeforeContextBuild();
    }

    private static void testActionConfigJsonCompatibility() {
        ActionConfig legacy = GSON.fromJson("""
                {"type":"COMMAND","value":"say legacy","enabled":true,"requiresOp":false,"cooldownTicks":0,"notifyOps":false}
                """, ActionConfig.class);
        requireEquals("", legacy.conditionGroupId(), "legacy action JSON without conditionGroupId defaults blank");
        requireEquals("say legacy", legacy.value(), "legacy action JSON keeps old value");

        ActionConfig gated = new ActionConfig(ActionType.SIGNAL, "mission.next", true, false, 0, false, " allow.action ");
        requireEquals("allow.action", gated.conditionGroupId(), "new action conditionGroupId normalizes");
        String json = GSON.toJson(gated);
        requireContains(json, "conditionGroupId", "new action conditionGroupId serializes");
        ActionConfig roundTrip = GSON.fromJson(json, ActionConfig.class);
        requireEquals("allow.action", roundTrip.conditionGroupId(), "new action conditionGroupId roundtrips");
        requireEquals("", ActionConfig.command("say old", false).conditionGroupId(), "command factory preserves old blank condition");
        requireEquals("", ActionConfig.signal("mission.old", false).conditionGroupId(), "signal factory preserves old blank condition");
    }

    private static void testBlankActionConditionSkipsRuntimeReadsAndHistory() {
        ConditionGateHistory.clearForTest();
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateService gateService = new ConditionGateService(
                (id) -> {
                    loaderCalled.set(true);
                    return null;
                },
                new ConditionEvaluator(),
                new ConditionGroupCompatibilityService()
        );
        ConditionActionGateService service = new ConditionActionGateService(gateService, new ConditionGroupCompatibilityService());

        ConditionGateResult result = service.evaluate(
                null,
                new ActionConfig(ActionType.COMMAND, "say skip", true, false, 0, false, ""),
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                ConditionActionGateService.actionTargetId("listener", "listener-1", 0),
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                "",
                0,
                () -> {
                    contextCalled.set(true);
                    return baseContext();
                }
        );

        requireTrue(result.allowed(), "blank action condition preserves old execute path");
        requireTrue(result.skipped(), "blank action condition is skipped");
        requireFalse(loaderCalled.get(), "blank action condition does not read group store");
        requireFalse(contextCalled.get(), "blank action condition does not build EvaluationContext");
        requireTrue(ConditionGateHistory.snapshot().isEmpty(), "blank action condition records no history");
    }

    private static void testActionGateRecordsActionMetadata() {
        ConditionGateHistory.clearForTest();
        ConditionActionGateService service = service(Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.CONTEXT_EQUALS,
                        config("field", "actionType", "expected", "signal"))), true)
        ));

        String actionTargetId = ConditionActionGateService.actionTargetId("listener", "listener-1", 2);
        ConditionGateResult result = service.evaluate(
                null,
                new ActionConfig(ActionType.SIGNAL, "mission.downstream", true, false, 0, false, "allow"),
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                actionTargetId,
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                "",
                2,
                ConditionActionGateServiceTest::baseContext
        );

        requireTrue(result.allowed(), "matching action metadata allows single action gate");
        ConditionGateHistoryRecord record = latest(ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION, actionTargetId);
        requireEquals("ACTION", record.gateLevel(), "action gate history marks gate level");
        requireEquals("SIGNAL_LISTENER", record.parentTargetType(), "action gate history parent type");
        requireEquals("listener-1", record.parentTargetId(), "action gate history parent id");
        requireEquals(2, record.actionIndex(), "action gate history action index");
        requireEquals(3, record.actionDisplayIndex(), "action gate history display index");
        requireEquals("signal", record.actionType(), "action gate history action type");
        requireEquals("signal", record.contextSummary().get("actionType"), "action metadata appears in context summary");
        requireEquals("2", record.contextSummary().get("actionIndex"), "action index appears in context summary");
        requireEquals("SIGNAL_LISTENER", record.compactDto().get("parentTargetType"), "compact dto exposes parent target type");
    }

    private static void testActionGateFalseBlocksOnlyCurrentActionDecision() {
        ConditionGateHistory.clearForTest();
        ConditionActionGateService service = service(Map.of(
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        ));
        String actionTargetId = ConditionActionGateService.actionTargetId("relay", "minecraft:overworld@1,2,3", 1);

        ConditionGateResult result = service.evaluate(
                null,
                new ActionConfig(ActionType.SIGNAL, "mission.blocked", true, false, 0, false, "deny"),
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                actionTargetId,
                ConditionRuntimeTargetType.ACTION_RELAY,
                "minecraft:overworld@1,2,3",
                "",
                1,
                ConditionActionGateServiceTest::baseContext
        );

        requireFalse(result.allowed(), "false single action gate returns blocked decision");
        requireFalse(result.skipped(), "configured false action gate is not skipped");
        requireContains(result.failureReason(), "未通过", "false action gate failure reason is Chinese");
        ConditionGateHistoryRecord record = latest(ConditionRuntimeTargetType.ACTION_RELAY_ACTION, actionTargetId);
        requireEquals("BLOCKED", record.result(), "false action gate records blocked history");
        requireEquals("ACTION", record.gateLevel(), "false action gate history remains action level");
        requireEquals("signal", record.actionType(), "false action gate records skipped action type");
    }

    private static void testRuntimeLoopSemanticsSkipCurrentAndContinue() {
        ConditionGateHistory.clearForTest();
        ConditionActionGateService service = service(Map.of(
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        ));
        List<ActionConfig> actions = List.of(
                new ActionConfig(ActionType.SIGNAL, "mission.blocked", true, false, 0, false, "deny"),
                new ActionConfig(ActionType.MESSAGE, "after-block", true, false, 0, false, "")
        );

        List<String> listenerExecuted = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                "listener",
                "listener-1",
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                "",
                false
        );
        requireEquals(List.of("after-block"), listenerExecuted, "SignalListener false action gate skips current action and continues next action");
        requireTrue(ConditionGateHistory.latestFor(ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                        ConditionActionGateService.actionTargetId("listener", "listener-1", 0)).isPresent(),
                "SignalListener action false records history");

        List<String> relayExecuted = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                "relay",
                "minecraft:overworld@1,2,3",
                ConditionRuntimeTargetType.ACTION_RELAY,
                "minecraft:overworld@1,2,3",
                "",
                false
        );
        requireEquals(List.of("after-block"), relayExecuted, "ActionRelay false action gate skips current action and continues next action");
        requireTrue(ConditionGateHistory.latestFor(ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                        ConditionActionGateService.actionTargetId("relay", "minecraft:overworld@1,2,3", 0)).isPresent(),
                "ActionRelay action false records history");

        List<String> regionExecuted = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.REGION_STAY_ACTION,
                "region",
                "controller-1",
                ConditionRuntimeTargetType.REGION_STAY,
                "controller-1",
                "stay",
                false
        );
        requireEquals(List.of("after-block"), regionExecuted, "Region stay false action gate skips current action and continues next action");
        requireTrue(ConditionGateHistory.latestFor(ConditionRuntimeTargetType.REGION_STAY_ACTION,
                        ConditionActionGateService.regionActionTargetId("controller-1", "stay", 0)).isPresent(),
                "Region action false records history");
    }

    private static void testManualBypassSkipsActionGateEvaluation() {
        ConditionGateHistory.clearForTest();
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        ConditionGateService gateService = new ConditionGateService(
                (id) -> {
                    loaderCalled.set(true);
                    return null;
                },
                new ConditionEvaluator(),
                new ConditionGroupCompatibilityService()
        );
        ConditionActionGateService service = new ConditionActionGateService(gateService, new ConditionGroupCompatibilityService());
        List<ActionConfig> actions = List.of(
                new ActionConfig(ActionType.SIGNAL, "manual-signal", true, false, 0, false, "deny.manual")
        );

        List<String> executed = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                "relay",
                "minecraft:overworld@1,2,3",
                ConditionRuntimeTargetType.ACTION_RELAY,
                "minecraft:overworld@1,2,3",
                "",
                true
        );

        requireEquals(List.of("manual-signal"), executed, "ActionRelay manual test bypasses action gate and executes configured action");
        requireFalse(loaderCalled.get(), "ActionRelay manual bypass does not evaluate action gate");
        requireTrue(ConditionGateHistory.snapshot().isEmpty(), "ActionRelay manual bypass records no action gate history");
    }

    private static void testStateActionGateFalseSkipsStateActionExecutionDecision() {
        ConditionGateHistory.clearForTest();
        ConditionActionGateService service = service(Map.of(
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        ));
        ActionConfig blockedStateAction = ActionConfig.stateVariable(
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
                "deny"
        );
        List<ActionConfig> actions = List.of(
                blockedStateAction,
                new ActionConfig(ActionType.MESSAGE, "after-state-block", true, false, 0, false, "")
        );

        List<String> executed = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                "listener",
                "listener-1",
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                "",
                false
        );

        requireEquals(List.of("after-state-block"), executed, "single action gate false skips blocked state_variable action and continues");
        requireTrue(ConditionGateHistory.latestFor(ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                        ConditionActionGateService.actionTargetId("listener", "listener-1", 0)).isPresent(),
                "blocked state action gate records history");

        List<String> manualExecuted = runActionLoop(
                service,
                actions,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                "relay",
                "minecraft:overworld@1,2,3",
                ConditionRuntimeTargetType.ACTION_RELAY,
                "minecraft:overworld@1,2,3",
                "",
                true
        );
        requireTrue(manualExecuted.getFirst().startsWith("state_variable:"), "ActionRelay manual test intentionally bypasses state action gate");
    }

    private static void testActionGateRejectsIncompatibleProfileBeforeContextBuild() {
        ConditionGateHistory.clearForTest();
        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionActionGateService service = service(Map.of(
                "player", entry("player", definition("player", leaf(ConditionNodeType.PLAYER_EXISTS)), true)
        ));
        String actionTargetId = ConditionActionGateService.actionTargetId("listener", "listener-1", 0);

        ConditionGateResult result = service.evaluate(
                null,
                new ActionConfig(ActionType.COMMAND, "say blocked", true, false, 0, false, "player"),
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                actionTargetId,
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                "",
                0,
                () -> {
                    contextCalled.set(true);
                    return baseContext();
                }
        );

        requireFalse(result.allowed(), "incompatible SignalListener action group fails closed");
        requireContains(result.failureReason(), "触发玩家", "incompatible action gate reports Chinese player reason");
        requireFalse(contextCalled.get(), "incompatible action gate rejects before live context builder runs");
        ConditionGateHistoryRecord record = latest(ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION, actionTargetId);
        requireEquals("condition_group_incompatible", record.code(), "incompatible action gate history code");
    }

    private static ConditionActionGateService service(Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups) {
        ConditionGateService gateService = new ConditionGateService(groups::get, new ConditionEvaluator(), new ConditionGroupCompatibilityService());
        return new ConditionActionGateService(gateService, new ConditionGroupCompatibilityService());
    }

    private static ConditionEvaluationContext baseContext() {
        return ConditionEvaluationContext.builder()
                .worldId("minecraft:overworld")
                .source("signal_bridge", "source-1")
                .channel("mission.start")
                .listenerId("listener-1")
                .deviceId("minecraft:overworld@1,2,3")
                .gameTime(123L)
                .build();
    }

    private static List<String> runActionLoop(
            ConditionActionGateService service,
            List<ActionConfig> actions,
            ConditionRuntimeTargetType actionTargetType,
            String ownerKind,
            String ownerId,
            ConditionRuntimeTargetType parentTargetType,
            String parentTargetId,
            String bucket,
            boolean manualBypass
    ) {
        java.util.ArrayList<String> executed = new java.util.ArrayList<>();
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index);
            if (action == null || !action.isUsable()) {
                continue;
            }
            if (!manualBypass) {
                String actionTargetId = actionTargetType == ConditionRuntimeTargetType.REGION_ENTER_ACTION
                        || actionTargetType == ConditionRuntimeTargetType.REGION_EXIT_ACTION
                        || actionTargetType == ConditionRuntimeTargetType.REGION_STAY_ACTION
                        ? ConditionActionGateService.regionActionTargetId(ownerId, bucket, index)
                        : ConditionActionGateService.actionTargetId(ownerKind, ownerId, index);
                ConditionGateResult gate = service.evaluate(
                        null,
                        action,
                        actionTargetType,
                        actionTargetId,
                        parentTargetType,
                        parentTargetId,
                        bucket,
                        index,
                        ConditionActionGateServiceTest::baseContext
                );
                if (!gate.allowed()) {
                    continue;
                }
            }
            executed.add(actionLabel(action));
        }
        return List.copyOf(executed);
    }

    private static String actionLabel(ActionConfig action) {
        if (action != null && action.type() == ActionType.STATE_VARIABLE) {
            return action.type().id() + ":" + action.stateActionSummary();
        }
        return action == null ? "" : action.value();
    }

    private static WebAdminConditionGroupStore.ConditionGroupEntry entry(String id, ConditionGroupDefinition definition, boolean enabled) {
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = id;
        entry.displayName = id;
        entry.enabled = enabled;
        entry.groupDefinition = definition;
        return entry;
    }

    private static ConditionGroupDefinition definition(String id, ConditionNode root) {
        return ConditionGroupDefinition.of(id, ConditionNode.group("root", ConditionGroupMode.AND, List.of(root)));
    }

    private static ConditionNode leaf(String type) {
        return leaf(type, ConditionNodeConfig.EMPTY);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf("node-" + type, type, config);
    }

    private static ConditionNodeConfig config(String... entries) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(entries[index], entries[index + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static ConditionGateHistoryRecord latest(ConditionRuntimeTargetType targetType, String targetId) {
        return ConditionGateHistory.latestFor(targetType, targetId)
                .orElseThrow(() -> new AssertionError("missing latest action gate history for " + targetType + " " + targetId));
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
}
