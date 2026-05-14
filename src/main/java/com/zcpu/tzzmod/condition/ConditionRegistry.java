package com.zcpu.tzzmod.condition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConditionRegistry {
    private final Map<String, ConditionTypeHandler> handlers = new LinkedHashMap<>();
    private final Map<String, ConditionTypeMetadata> groupMetadata = new LinkedHashMap<>();

    public ConditionRegistry register(ConditionTypeHandler handler) {
        if (handler == null || handler.metadata() == null || handler.metadata().type().isBlank()) {
            return this;
        }
        handlers.put(handler.metadata().type(), handler);
        return this;
    }

    public ConditionRegistry registerGroupMetadata(ConditionTypeMetadata metadata) {
        if (metadata != null && !metadata.type().isBlank()) {
            groupMetadata.put(metadata.type(), metadata);
        }
        return this;
    }

    public Optional<ConditionTypeHandler> handler(String type) {
        return Optional.ofNullable(handlers.get(normalize(type)));
    }

    public Optional<ConditionTypeMetadata> metadata(String type) {
        String key = normalize(type);
        ConditionTypeHandler handler = handlers.get(key);
        if (handler != null) {
            return Optional.of(handler.metadata());
        }
        return Optional.ofNullable(groupMetadata.get(key));
    }

    public List<ConditionTypeMetadata> metadata() {
        Map<String, ConditionTypeMetadata> all = new LinkedHashMap<>(groupMetadata);
        handlers.values().forEach((handler) -> all.put(handler.metadata().type(), handler.metadata()));
        return List.copyOf(all.values());
    }

    public ConditionValidationResult validate(ConditionNode node) {
        if (node == null) {
            return ConditionValidationResult.error("", "", "condition_node_null", "条件节点为空");
        }
        if (node.isGroup()) {
            return ConditionValidationResult.ok();
        }
        ConditionTypeHandler handler = handlers.get(node.type());
        if (handler == null) {
            return ConditionValidationResult.error(node.id(), "", "condition_type_unknown", "未知条件类型：" + node.type());
        }
        return handler.validate(node);
    }

    public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
        ConditionTypeHandler handler = handlers.get(node == null ? "" : node.type());
        if (handler == null) {
            return ConditionEvaluationResult.error(node, context, "condition_type_unknown", "未知条件类型：" + (node == null ? "" : node.type()));
        }
        try {
            return handler.evaluate(node, context);
        } catch (Exception exception) {
            return ConditionEvaluationResult.error(node, context, "condition_evaluation_exception", "条件判断异常：" + exception.getMessage());
        }
    }

    public static ConditionRegistry defaultRegistry() {
        ConditionRegistry registry = new ConditionRegistry();
        registry.registerGroupMetadata(new ConditionTypeMetadata(
                ConditionNodeType.GROUP,
                "条件组",
                "AND / OR / NOT 条件组，用于组合子条件。",
                "core",
                List.of()
        ));
        registry.register(new AlwaysTrueHandler());
        registry.register(new AlwaysFalseHandler());
        registry.register(new ContextExistsHandler(ConditionNodeType.CONTEXT_EXISTS));
        registry.register(new ContextExistsHandler(ConditionNodeType.CONTEXT_FIELD_EXISTS));
        registry.register(new ContextEqualsHandler());
        return registry;
    }

    private static String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record AlwaysTrueHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return new ConditionTypeMetadata(
                    ConditionNodeType.ALWAYS_TRUE,
                    "始终通过",
                    "测试与占位条件，总是返回 true。",
                    "core",
                    List.of()
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            return ConditionEvaluationResult.leaf(node, context, true, "always_true", "条件始终通过");
        }
    }

    private record AlwaysFalseHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return new ConditionTypeMetadata(
                    ConditionNodeType.ALWAYS_FALSE,
                    "始终失败",
                    "测试与占位条件，总是返回 false。",
                    "core",
                    List.of()
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            return ConditionEvaluationResult.leaf(node, context, false, "always_false", "条件始终失败");
        }
    }

    private record ContextExistsHandler(String type) implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return new ConditionTypeMetadata(
                    type,
                    "上下文字段存在",
                    "检查 EvaluationContext 中指定字段是否存在且非空。",
                    "context",
                    List.of(new ConditionFieldSchema("field", "字段", "string", true, "例如 channel、sourceType、variables.flag"))
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            if (node == null || !node.config().has("field")) {
                return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_field", "上下文字段存在条件缺少 field");
            }
            return ConditionValidationResult.ok();
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String field = node.config().get("field");
            String actual = context == null ? "" : context.fieldValue(field);
            boolean matched = !actual.isBlank();
            return ConditionEvaluationResult.leaf(
                    node,
                    context,
                    matched,
                    matched ? "context_field_exists" : "context_field_missing",
                    matched ? "上下文字段存在：" + field : "上下文字段不存在或为空：" + field
            );
        }
    }

    private record ContextEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return new ConditionTypeMetadata(
                    ConditionNodeType.CONTEXT_EQUALS,
                    "上下文字段等于",
                    "检查 EvaluationContext 中指定字段是否等于固定值。",
                    "context",
                    List.of(
                            new ConditionFieldSchema("field", "字段", "string", true, "例如 channel、sourceType、variables.flag"),
                            new ConditionFieldSchema("expected", "期望值", "string", true, "字段需要匹配的文本")
                    )
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = ConditionValidationResult.ok();
            if (node == null || !node.config().has("field")) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_field", "上下文等于条件缺少 field"));
            }
            if (node == null || !node.config().values().containsKey("expected")) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_expected", "上下文等于条件缺少 expected"));
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String field = node.config().get("field");
            String expected = node.config().get("expected");
            String actual = context == null ? "" : context.fieldValue(field);
            boolean matched = actual.equals(expected);
            return ConditionEvaluationResult.leaf(
                    node,
                    context,
                    matched,
                    matched ? "context_equals" : "context_not_equal",
                    matched
                            ? "上下文字段匹配：" + field
                            : "上下文字段不匹配：" + field + "，期望 `" + expected + "`，实际 `" + actual + "`"
            );
        }
    }
}
