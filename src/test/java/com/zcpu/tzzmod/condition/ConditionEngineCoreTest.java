package com.zcpu.tzzmod.condition;

import java.util.ArrayList;
import java.util.List;

public final class ConditionEngineCoreTest {
    private ConditionEngineCoreTest() {
    }

    public static void run() {
        testBooleanGroups();
        testNestedGroup();
        testDisabledNode();
        testContextConditions();
        testUnknownAndInvalidTypes();
        testValidationIssues();
        testDepthAndNodeLimits();
        testFingerprint();
        testResultDebugTree();
    }

    private static void testBooleanGroups() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        requireTrue(evaluate(evaluator, ConditionGroupMode.AND, trueNode("a"), trueNode("b")).matched(), "AND true + true");
        requireFalse(evaluate(evaluator, ConditionGroupMode.AND, trueNode("a"), falseNode("b")).matched(), "AND true + false");
        requireTrue(evaluate(evaluator, ConditionGroupMode.OR, falseNode("a"), trueNode("b")).matched(), "OR false + true");
        requireFalse(evaluate(evaluator, ConditionGroupMode.OR, falseNode("a"), falseNode("b")).matched(), "OR false + false");
        requireFalse(evaluator.evaluate(ConditionGroupDefinition.of("not_true", ConditionNode.not("not", trueNode("child"))), context()).matched(), "NOT true");
        requireTrue(evaluator.evaluate(ConditionGroupDefinition.of("not_false", ConditionNode.not("not", falseNode("child"))), context()).matched(), "NOT false");
    }

    private static void testNestedGroup() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode nested = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                trueNode("a"),
                ConditionNode.group("or", ConditionGroupMode.OR, List.of(falseNode("b"), trueNode("c")))
        ));
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("nested", nested), context());
        requireTrue(result.matched(), "nested group matches");
        requireEquals(2, result.childResults().size(), "nested child count");
        requireEquals("condition_group_and_passed", result.reasonCode(), "nested reason code");
    }

    private static void testDisabledNode() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                ConditionNode.disabledLeaf("disabled_false", ConditionNodeType.ALWAYS_FALSE),
                trueNode("enabled_true")
        ));
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("disabled", root), context());
        requireTrue(result.matched(), "disabled false node should not block AND group");
        requireTrue(result.childResults().getFirst().skipped(), "disabled node is marked skipped");
    }

    private static void testContextConditions() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionEvaluationContext context = ConditionEvaluationContext.builder()
                .channel("mission.submit")
                .source("virtual_block_device", "device-1")
                .worldId("minecraft:overworld")
                .variable("gameActive", "true")
                .build();
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                ConditionNode.leaf("channel_exists", ConditionNodeType.CONTEXT_FIELD_EXISTS, ConditionNodeConfig.of("field", "channel")),
                ConditionNode.leaf("channel_equals", ConditionNodeType.CONTEXT_EQUALS, ConditionNodeConfig.of("field", "channel", "expected", "mission.submit")),
                ConditionNode.leaf("variable_equals", ConditionNodeType.CONTEXT_EQUALS, ConditionNodeConfig.of("field", "variables.gameActive", "expected", "true"))
        ));
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("context", root), context);
        requireTrue(result.matched(), "context conditions match");

        ConditionNode mismatch = ConditionNode.leaf("source_mismatch", ConditionNodeType.CONTEXT_EQUALS, ConditionNodeConfig.of("field", "sourceType", "expected", "region"));
        ConditionEvaluationResult mismatchResult = evaluator.evaluate(ConditionGroupDefinition.of("mismatch", mismatch), context);
        requireFalse(mismatchResult.matched(), "context mismatch fails");
        requireContains(mismatchResult.failureReason(), "期望 `region`", "context mismatch failure reason");
    }

    private static void testUnknownAndInvalidTypes() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode unknown = ConditionNode.leaf("unknown", "unknown_type");
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("unknown", unknown), context());
        requireFalse(result.matched(), "unknown type fails safely");
        requireTrue(result.error(), "unknown type is error");
        requireEquals("condition_type_unknown", result.reasonCode(), "unknown type reason");
        requireFalse(evaluator.validate(ConditionGroupDefinition.of("unknown", unknown)).valid(), "unknown type validation fails");

        ConditionNode invalid = ConditionNode.leaf("invalid_context", ConditionNodeType.CONTEXT_EQUALS, ConditionNodeConfig.of("field", "channel"));
        requireFalse(evaluator.validate(ConditionGroupDefinition.of("invalid", invalid)).valid(), "invalid context_equals config fails validation");
    }

    private static void testValidationIssues() {
        ConditionEvaluator evaluator = new ConditionEvaluator();

        ConditionValidationResult empty = evaluator.validate(ConditionGroupDefinition.of("empty", ConditionNode.group("empty", ConditionGroupMode.AND, List.of())));
        requireIssue(empty, "condition_group_empty", "empty group validation issue");

        ConditionValidationResult invalidNot = evaluator.validate(ConditionGroupDefinition.of("not", ConditionNode.group("not", ConditionGroupMode.NOT, List.of(trueNode("a"), falseNode("b")))));
        requireIssue(invalidNot, "condition_group_not_child_count_invalid", "NOT child count validation issue");

        ConditionValidationResult duplicate = evaluator.validate(ConditionGroupDefinition.of("duplicate", ConditionNode.group("root", ConditionGroupMode.AND, List.of(trueNode("same"), falseNode("same")))));
        requireIssue(duplicate, "condition_duplicate_node_id", "duplicate node id validation issue");

        ConditionNode disabledUnknown = new ConditionNode("disabled_unknown", "unknown_type", "", "", false, ConditionGroupMode.AND, ConditionNodeConfig.EMPTY, List.of());
        requireIssue(evaluator.validate(ConditionGroupDefinition.of("disabled_unknown", disabledUnknown)), "condition_type_unknown", "disabled nodes remain structurally validated");
    }

    private static void testDepthAndNodeLimits() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode deep = trueNode("leaf");
        for (int i = 0; i < ConditionEngineLimits.DEFAULT_MAX_DEPTH + 2; i++) {
            deep = ConditionNode.group("depth_" + i, ConditionGroupMode.AND, List.of(deep));
        }
        ConditionGroupDefinition deepDefinition = ConditionGroupDefinition.of("deep", deep);
        requireFalse(evaluator.validate(deepDefinition).valid(), "max depth validation fails");
        requireTrue(evaluator.evaluateTrace(deepDefinition, context()).truncated(), "max depth evaluation marks truncated");

        List<ConditionNode> many = new ArrayList<>();
        for (int i = 0; i < ConditionEngineLimits.DEFAULT_MAX_NODES + 1; i++) {
            many.add(trueNode("node_" + i));
        }
        ConditionGroupDefinition manyDefinition = ConditionGroupDefinition.of("many", ConditionNode.group("root", ConditionGroupMode.AND, many));
        requireFalse(evaluator.validate(manyDefinition).valid(), "max node validation fails");
        ConditionEvaluationTrace manyTrace = evaluator.evaluateTrace(manyDefinition, context());
        requireTrue(manyTrace.truncated(), "max node evaluation marks truncated");
        requireTrue(manyTrace.evaluatedNodeCount() <= ConditionEngineLimits.DEFAULT_MAX_NODES + 1, "max node evaluation stops after limit");
    }

    private static void testFingerprint() {
        ConditionGroupDefinition first = ConditionGroupDefinition.of("fingerprint", ConditionNode.group("root", ConditionGroupMode.AND, List.of(trueNode("a"))));
        ConditionGroupDefinition second = ConditionGroupDefinition.of("fingerprint", ConditionNode.group("root", ConditionGroupMode.AND, List.of(trueNode("a"))));
        ConditionGroupDefinition changed = ConditionGroupDefinition.of("fingerprint", ConditionNode.group("root", ConditionGroupMode.AND, List.of(falseNode("a"))));
        requireEquals(first.stableFingerprint(), second.stableFingerprint(), "same definition fingerprint stable");
        requireFalse(first.stableFingerprint().equals(changed.stableFingerprint()), "changed definition fingerprint changes");
    }

    private static void testResultDebugTree() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        ConditionNode root = ConditionNode.group("root", ConditionGroupMode.AND, List.of(trueNode("a"), falseNode("b")));
        ConditionEvaluationResult result = evaluator.evaluate(ConditionGroupDefinition.of("debug", root), context());
        requireFalse(result.matched(), "debug result fails");
        requireEquals("debug", result.conditionId(), "result carries condition id");
        requireEquals(2, result.childResults().size(), "debug tree carries child results");
        requireTrue(result.evaluatedNodeCount() >= 3, "debug tree reports evaluated node count");
        requireContains(result.failureReason(), "AND", "debug tree has readable failure reason");
        requireContains(result.failureReason(), "b", "debug tree failure reason points at failed child");
    }

    private static ConditionEvaluationResult evaluate(ConditionEvaluator evaluator, ConditionGroupMode mode, ConditionNode left, ConditionNode right) {
        return evaluator.evaluate(ConditionGroupDefinition.of("group", ConditionNode.group("root", mode, List.of(left, right))), context());
    }

    private static ConditionNode trueNode(String id) {
        return ConditionNode.leaf(id, ConditionNodeType.ALWAYS_TRUE);
    }

    private static ConditionNode falseNode(String id) {
        return ConditionNode.leaf(id, ConditionNodeType.ALWAYS_FALSE);
    }

    private static ConditionEvaluationContext context() {
        return ConditionEvaluationContext.builder()
                .worldId("minecraft:overworld")
                .source("test", "condition-core-test")
                .gameTime(120L)
                .build();
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
        requireTrue(result.issues().stream().anyMatch((issue) -> code.equals(issue.code())), message);
    }
}
