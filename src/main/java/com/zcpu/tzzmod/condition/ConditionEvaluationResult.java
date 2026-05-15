package com.zcpu.tzzmod.condition;

import java.util.List;

public record ConditionEvaluationResult(
        boolean matched,
        String conditionId,
        String nodeId,
        String type,
        String label,
        String reasonCode,
        String failureReason,
        String message,
        String debugSummary,
        List<ConditionEvaluationResult> childResults,
        boolean skipped,
        boolean error,
        int evaluatedNodeCount,
        long durationNanos,
        String contextSummary
) {
    public ConditionEvaluationResult {
        conditionId = safe(conditionId);
        nodeId = safe(nodeId);
        type = safe(type);
        label = safe(label);
        reasonCode = safe(reasonCode);
        failureReason = safe(failureReason);
        message = safe(message);
        debugSummary = safe(debugSummary);
        childResults = childResults == null ? List.of() : List.copyOf(childResults);
        evaluatedNodeCount = Math.max(0, evaluatedNodeCount);
        durationNanos = Math.max(0L, durationNanos);
        contextSummary = safe(contextSummary);
    }

    public ConditionEvaluationStatus status() {
        if (skipped) {
            return ConditionEvaluationStatus.SKIPPED;
        }
        if (error) {
            return ConditionEvaluationStatus.ERROR;
        }
        return matched ? ConditionEvaluationStatus.PASSED : ConditionEvaluationStatus.FAILED;
    }

    public static ConditionEvaluationResult leaf(
            ConditionNode node,
            ConditionEvaluationContext context,
            boolean matched,
            String reasonCode,
            String message
    ) {
        return leaf(node, context, matched, reasonCode, message, label(node));
    }

    public static ConditionEvaluationResult leaf(
            ConditionNode node,
            ConditionEvaluationContext context,
            boolean matched,
            String reasonCode,
            String message,
            String label
    ) {
        return new ConditionEvaluationResult(
                matched,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label,
                reasonCode,
                matched ? "" : message,
                message,
                message,
                List.of(),
                false,
                false,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public static ConditionEvaluationResult skipped(ConditionNode node, ConditionEvaluationContext context) {
        return new ConditionEvaluationResult(
                true,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label(node),
                "condition_node_disabled",
                "",
                "条件节点已禁用，跳过判断",
                "skipped disabled node",
                List.of(),
                true,
                false,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public static ConditionEvaluationResult error(ConditionNode node, ConditionEvaluationContext context, String code, String message) {
        return new ConditionEvaluationResult(
                false,
                "",
                node == null ? "" : node.id(),
                node == null ? "" : node.type(),
                label(node),
                code,
                message,
                message,
                message,
                List.of(),
                false,
                true,
                1,
                0L,
                context == null ? "" : context.compactSummary()
        );
    }

    public ConditionEvaluationResult withConditionId(String conditionId) {
        return new ConditionEvaluationResult(
                matched,
                conditionId,
                nodeId,
                type,
                label,
                reasonCode,
                failureReason,
                message,
                debugSummary,
                childResults,
                skipped,
                error,
                evaluatedNodeCount,
                durationNanos,
                contextSummary
        );
    }

    public ConditionEvaluationResult withDuration(long durationNanos) {
        return new ConditionEvaluationResult(
                matched,
                conditionId,
                nodeId,
                type,
                label,
                reasonCode,
                failureReason,
                message,
                debugSummary,
                childResults,
                skipped,
                error,
                evaluatedNodeCount,
                durationNanos,
                contextSummary
        );
    }

    static String label(ConditionNode node) {
        if (node == null) {
            return "";
        }
        if (!node.name().isBlank()) {
            return node.name();
        }
        if (node.isGroup()) {
            return switch (node.groupMode()) {
                case AND -> "全部满足条件组";
                case OR -> "任意满足条件组";
                case NOT -> "条件取反组";
            };
        }
        return switch (node.type()) {
            case ConditionNodeType.ALWAYS_TRUE -> "永远通过";
            case ConditionNodeType.ALWAYS_FALSE -> "永远失败";
            case ConditionNodeType.CONTEXT_EXISTS -> "上下文存在";
            case ConditionNodeType.CONTEXT_FIELD_EXISTS -> "上下文字段存在";
            case ConditionNodeType.CONTEXT_EQUALS -> "上下文字段匹配";
            case ConditionNodeType.PLAYER_EXISTS -> "触发玩家存在";
            case ConditionNodeType.PLAYER_ONLINE -> "玩家在线";
            case ConditionNodeType.PLAYER_IS_OP -> "玩家是管理员";
            case ConditionNodeType.PLAYER_HAS_TAG -> "玩家拥有标签";
            case ConditionNodeType.PLAYER_LACKS_TAG -> "玩家没有标签";
            case ConditionNodeType.PLAYER_TEAM_EQUALS -> "玩家队伍匹配";
            case ConditionNodeType.PLAYER_GAMEMODE_EQUALS -> "玩家游戏模式匹配";
            case ConditionNodeType.PLAYER_ALIVE -> "玩家存活";
            case ConditionNodeType.PLAYER_DEAD -> "玩家死亡";
            case ConditionNodeType.SOURCE_TYPE_EQUALS -> "来源类型匹配";
            case ConditionNodeType.SOURCE_ID_EQUALS -> "来源 ID 匹配";
            case ConditionNodeType.CHANNEL_EQUALS -> "信号频道匹配";
            case ConditionNodeType.WORLD_EQUALS -> "世界匹配";
            case ConditionNodeType.DEVICE_ID_EQUALS -> "设备 ID 匹配";
            case ConditionNodeType.LISTENER_ID_EQUALS -> "监听器 ID 匹配";
            case ConditionNodeType.REGION_ID_EQUALS -> "区域 ID 匹配";
            case ConditionNodeType.ACTION_ID_EQUALS -> "动作 ID 匹配";
            case ConditionNodeType.GAME_TIME_COMPARE -> "游戏时间比较";
            case ConditionNodeType.EVENT_METADATA_EXISTS -> "事件元数据存在";
            case ConditionNodeType.EVENT_METADATA_EQUALS -> "事件元数据匹配";
            case ConditionNodeType.STATE_VARIABLE_EXISTS -> "状态变量存在";
            case ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS -> "布尔状态匹配";
            case ConditionNodeType.STATE_VARIABLE_INT_COMPARE -> "整数状态比较";
            case ConditionNodeType.STATE_VARIABLE_STRING_EQUALS -> "文本状态匹配";
            case ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS -> "文本状态包含";
            case ConditionNodeType.ITEM_STACK_EXISTS -> "物品快照存在";
            case ConditionNodeType.ITEM_STACK_MATCHES -> "物品快照匹配";
            case ConditionNodeType.INVENTORY_CONTAINS_ITEM -> "背包包含物品";
            case ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE -> "背包物品数量比较";
            case ConditionNodeType.CONTAINER_SLOT_EMPTY -> "容器槽位为空";
            case ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES -> "容器槽位物品匹配";
            case ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE -> "容器物品数量比较";
            case ConditionNodeType.REGION_EXISTS -> "区域快照存在";
            case ConditionNodeType.REGION_ENABLED -> "区域已启用";
            case ConditionNodeType.PLAYER_IN_REGION -> "玩家在区域内";
            case ConditionNodeType.REGION_PLAYER_COUNT_COMPARE -> "区域玩家数量比较";
            case ConditionNodeType.SIGNAL_CHANNEL_EXISTS -> "信号频道快照存在";
            case ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE -> "信号消费者数量比较";
            case ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE -> "信号事件数量比较";
            case ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE -> "逻辑链包含节点";
            case ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL -> "逻辑链包含频道";
            case ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE -> "逻辑链存在循环";
            case ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE -> "逻辑链节点数量比较";
            default -> node.id().isBlank() ? node.type() : node.id();
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
