package com.zcpu.tzzmod.condition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConditionEvaluator {
    private final ConditionRegistry registry;
    private final ConditionEngineLimits limits;

    public ConditionEvaluator() {
        this(ConditionRegistry.defaultRegistry(), ConditionEngineLimits.DEFAULT);
    }

    public ConditionEvaluator(ConditionRegistry registry, ConditionEngineLimits limits) {
        this.registry = registry == null ? ConditionRegistry.defaultRegistry() : registry;
        this.limits = limits == null ? ConditionEngineLimits.DEFAULT : limits;
    }

    public ConditionEvaluationResult evaluate(ConditionGroupDefinition definition, ConditionEvaluationContext context) {
        return evaluateTrace(definition, context).rootResult();
    }

    public ConditionEvaluationTrace evaluateTrace(ConditionGroupDefinition definition, ConditionEvaluationContext context) {
        long started = System.nanoTime();
        ConditionGroupDefinition safeDefinition = definition == null
                ? ConditionGroupDefinition.of("condition", ConditionNode.group("root", ConditionGroupMode.AND, List.of()))
                : definition;
        EvaluationState state = new EvaluationState();
        ConditionEvaluationResult result = evaluateNode(safeDefinition.root(), context, 0, state)
                .withConditionId(safeDefinition.id());
        long duration = System.nanoTime() - started;
        result = result.withDuration(duration);
        return new ConditionEvaluationTrace(
                result,
                state.visitedNodes,
                limits.maxDepth(),
                limits.maxNodes(),
                state.truncated,
                duration
        );
    }

    public ConditionValidationResult validate(ConditionGroupDefinition definition) {
        List<ConditionValidationIssue> issues = new ArrayList<>();
        if (definition == null) {
            issues.add(new ConditionValidationIssue("", "root", "condition_definition_null", "条件定义为空"));
            return new ConditionValidationResult(issues);
        }
        validateNode(definition.root(), "root", 0, new HashSet<>(), new ValidationState(), issues);
        return new ConditionValidationResult(issues);
    }

    private ConditionEvaluationResult evaluateNode(
            ConditionNode node,
            ConditionEvaluationContext context,
            int depth,
            EvaluationState state
    ) {
        if (node == null) {
            return ConditionEvaluationResult.error(null, context, "condition_node_null", "条件节点为空");
        }
        state.visitedNodes++;
        if (depth > limits.maxDepth()) {
            state.truncated = true;
            return ConditionEvaluationResult.error(node, context, "condition_max_depth_exceeded", "条件嵌套深度超过限制：" + limits.maxDepth());
        }
        if (state.visitedNodes > limits.maxNodes()) {
            state.truncated = true;
            return ConditionEvaluationResult.error(node, context, "condition_max_nodes_exceeded", "条件节点数量超过限制：" + limits.maxNodes());
        }
        if (!node.enabled()) {
            return ConditionEvaluationResult.skipped(node, context);
        }
        if (ConditionNodeType.GROUP.equals(node.type())) {
            return evaluateGroup(node, context, depth, state);
        }
        return registry.evaluate(node, context);
    }

    private ConditionEvaluationResult evaluateGroup(
            ConditionNode node,
            ConditionEvaluationContext context,
            int depth,
            EvaluationState state
    ) {
        long started = System.nanoTime();
        List<ConditionEvaluationResult> children = new ArrayList<>();
        for (ConditionNode child : node.children()) {
            if (state.truncated) {
                break;
            }
            children.add(evaluateNode(child, context, depth + 1, state));
            if (state.truncated) {
                break;
            }
        }
        List<ConditionEvaluationResult> active = children.stream()
                .filter((child) -> !child.skipped())
                .toList();
        boolean matched;
        boolean error = active.stream().anyMatch(ConditionEvaluationResult::error);
        String code;
        String message;
        if (active.isEmpty()) {
            matched = false;
            code = "condition_group_empty";
            message = "条件组没有启用的子条件";
        } else if (node.groupMode() == ConditionGroupMode.AND) {
            matched = !error && active.stream().allMatch(ConditionEvaluationResult::matched);
            code = matched ? "condition_group_and_passed" : "condition_group_and_failed";
            message = matched ? "AND 条件组全部通过" : "AND 条件组存在未通过子条件";
        } else if (node.groupMode() == ConditionGroupMode.OR) {
            matched = !error && active.stream().anyMatch(ConditionEvaluationResult::matched);
            code = matched ? "condition_group_or_passed" : "condition_group_or_failed";
            message = matched ? "OR 条件组至少一个子条件通过" : "OR 条件组没有子条件通过";
        } else {
            if (active.size() != 1) {
                matched = false;
                error = true;
                code = "condition_group_not_child_count_invalid";
                message = "NOT 条件组必须且只能有一个启用子条件";
            } else if (active.getFirst().error()) {
                matched = false;
                error = true;
                code = "condition_group_not_child_error";
                message = "NOT 子条件判断失败：" + active.getFirst().message();
            } else {
                matched = !active.getFirst().matched();
                code = matched ? "condition_group_not_passed" : "condition_group_not_failed";
                message = matched ? "NOT 子条件未通过，因此条件组通过" : "NOT 子条件通过，因此条件组失败";
            }
        }
        if (!matched || error) {
            message = appendFirstChildFailure(message, active);
        }
        String failureReason = matched && !error ? "" : message;
        int evaluatedCount = 1 + children.stream().mapToInt(ConditionEvaluationResult::evaluatedNodeCount).sum();
        return new ConditionEvaluationResult(
                matched,
                "",
                node.id(),
                node.type(),
                ConditionEvaluationResult.label(node),
                code,
                failureReason,
                message,
                message,
                children,
                false,
                error,
                evaluatedCount,
                System.nanoTime() - started,
                context == null ? "" : context.compactSummary()
        );
    }

    private void validateNode(
            ConditionNode node,
            String path,
            int depth,
            Set<String> ids,
            ValidationState state,
            List<ConditionValidationIssue> issues
    ) {
        if (state.truncated) {
            return;
        }
        if (node == null) {
            issues.add(new ConditionValidationIssue("", path, "condition_node_null", "条件节点为空"));
            return;
        }
        state.nodeCount++;
        if (depth > limits.maxDepth()) {
            state.truncated = true;
            issues.add(new ConditionValidationIssue(node.id(), path, "condition_max_depth_exceeded", "条件嵌套深度超过限制：" + limits.maxDepth()));
            return;
        }
        if (state.nodeCount > limits.maxNodes()) {
            state.truncated = true;
            issues.add(new ConditionValidationIssue(node.id(), path, "condition_max_nodes_exceeded", "条件节点数量超过限制：" + limits.maxNodes()));
            return;
        }
        if (!node.id().isBlank() && !ids.add(node.id())) {
            issues.add(new ConditionValidationIssue(node.id(), path, "condition_duplicate_node_id", "重复的条件节点 ID：" + node.id()));
        }
        if (ConditionNodeType.GROUP.equals(node.type())) {
            long enabledChildren = node.children().stream().filter((child) -> child != null && child.enabled()).count();
            if (enabledChildren == 0) {
                issues.add(new ConditionValidationIssue(node.id(), path, "condition_group_empty", "条件组至少需要一个启用子条件"));
            }
            if (node.groupMode() == ConditionGroupMode.NOT && enabledChildren != 1) {
                issues.add(new ConditionValidationIssue(node.id(), path, "condition_group_not_child_count_invalid", "NOT 条件组必须且只能有一个启用子条件"));
            }
            for (int index = 0; index < node.children().size(); index++) {
                if (state.truncated) {
                    break;
                }
                validateNode(node.children().get(index), path + ".children[" + index + "]", depth + 1, ids, state, issues);
            }
            return;
        }
        ConditionValidationResult result = registry.validate(node);
        for (ConditionValidationIssue issue : result.issues()) {
            issues.add(new ConditionValidationIssue(
                    issue.nodeId().isBlank() ? node.id() : issue.nodeId(),
                    issue.path().isBlank() ? path : issue.path(),
                    issue.code(),
                    issue.message()
            ));
        }
    }

    private static String appendFirstChildFailure(String message, List<ConditionEvaluationResult> active) {
        for (ConditionEvaluationResult child : active) {
            if (child.error() || !child.matched()) {
                String nodeId = child.nodeId() == null || child.nodeId().isBlank() ? child.label() : child.nodeId();
                return message + "；首个失败节点：" + nodeId + "（" + child.reasonCode() + "）" + child.message();
            }
        }
        return message;
    }

    private static final class EvaluationState {
        private int visitedNodes = 0;
        private boolean truncated = false;
    }

    private static final class ValidationState {
        private int nodeCount = 0;
        private boolean truncated = false;
    }
}
