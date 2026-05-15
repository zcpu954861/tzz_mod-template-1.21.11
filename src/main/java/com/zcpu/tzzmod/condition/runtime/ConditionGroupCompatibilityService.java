package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ConditionGroupCompatibilityService {
    private static final Set<String> PLAYER_TYPES = Set.of(
            ConditionNodeType.PLAYER_EXISTS,
            ConditionNodeType.PLAYER_ONLINE,
            ConditionNodeType.PLAYER_IS_OP,
            ConditionNodeType.PLAYER_HAS_TAG,
            ConditionNodeType.PLAYER_LACKS_TAG,
            ConditionNodeType.PLAYER_TEAM_EQUALS,
            ConditionNodeType.PLAYER_GAMEMODE_EQUALS,
            ConditionNodeType.PLAYER_ALIVE,
            ConditionNodeType.PLAYER_DEAD,
            ConditionNodeType.PLAYER_IN_REGION
    );

    private static final Set<String> STATE_VARIABLE_TYPES = Set.of(
            ConditionNodeType.STATE_VARIABLE_EXISTS,
            ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
            ConditionNodeType.STATE_VARIABLE_INT_COMPARE,
            ConditionNodeType.STATE_VARIABLE_STRING_EQUALS,
            ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS
    );

    public ConditionGroupCompatibilityProfile profile(ConditionRuntimeTargetType targetType) {
        return ConditionGroupCompatibilityProfile.forTarget(targetType);
    }

    public ConditionGroupCompatibilityProfile profile(
            ConditionRuntimeTargetType targetType,
            boolean containerSnapshotForOpenClose
    ) {
        return ConditionGroupCompatibilityProfile.forTarget(targetType, containerSnapshotForOpenClose);
    }

    public ConditionGroupCompatibilityResult analyze(ConditionGroupDefinition definition, ConditionRuntimeTargetType targetType) {
        return analyze(definition, profile(targetType));
    }

    public ConditionGroupCompatibilityResult analyze(ConditionGroupDefinition definition, ConditionGroupCompatibilityProfile profile) {
        ConditionGroupCompatibilityProfile safeProfile = profile == null
                ? ConditionGroupCompatibilityProfile.forTarget(ConditionRuntimeTargetType.VBD_INTERACTION)
                : profile;
        if (definition == null) {
            return ConditionGroupCompatibilityResult.incompatible(safeProfile.targetType(), "", List.of("条件组定义为空，不能绑定到运行时触发。"));
        }
        List<String> reasons = new ArrayList<>();
        analyzeNode(definition.root(), "root", safeProfile, reasons);
        if (reasons.isEmpty()) {
            return ConditionGroupCompatibilityResult.compatible(safeProfile.targetType(), definition.id());
        }
        return ConditionGroupCompatibilityResult.incompatible(safeProfile.targetType(), definition.id(), reasons);
    }

    private void analyzeNode(ConditionNode node, String path, ConditionGroupCompatibilityProfile profile, List<String> reasons) {
        if (node == null) {
            reasons.add(path + " 条件节点为空。");
            return;
        }
        if (!node.enabled()) {
            return;
        }
        if (ConditionNodeType.GROUP.equals(node.type())) {
            List<ConditionNode> children = node.children();
            for (int index = 0; index < children.size(); index++) {
                analyzeNode(children.get(index), path + ".children[" + index + "]", profile, reasons);
            }
            return;
        }
        String type = node.type();
        if (type == null || type.isBlank()) {
            reasons.add(path + " 条件类型为空。");
            return;
        }
        if (ConditionNodeType.ALWAYS_TRUE.equals(type)
                || ConditionNodeType.ALWAYS_FALSE.equals(type)
                || ConditionNodeType.CONTEXT_EXISTS.equals(type)
                || ConditionNodeType.GAME_TIME_COMPARE.equals(type)) {
            return;
        }
        if (PLAYER_TYPES.contains(type) && !profile.playerContext()) {
            reasons.add(nodeLabel(path, node) + " 需要触发玩家上下文，但 " + profile.displayName() + " 不提供玩家。");
            return;
        }
        switch (type) {
            case ConditionNodeType.CONTEXT_FIELD_EXISTS, ConditionNodeType.CONTEXT_EQUALS -> requireContextField(node, path, profile, reasons);
            case ConditionNodeType.SOURCE_TYPE_EQUALS -> requireContextField("sourceType", node, path, profile, reasons);
            case ConditionNodeType.SOURCE_ID_EQUALS -> requireContextField("sourceId", node, path, profile, reasons);
            case ConditionNodeType.CHANNEL_EQUALS -> requireContextField("channel", node, path, profile, reasons);
            case ConditionNodeType.WORLD_EQUALS -> requireContextField("world", node, path, profile, reasons);
            case ConditionNodeType.DEVICE_ID_EQUALS -> requireContextField("deviceId", node, path, profile, reasons);
            case ConditionNodeType.LISTENER_ID_EQUALS -> requireContextField("listenerId", node, path, profile, reasons);
            case ConditionNodeType.REGION_ID_EQUALS -> requireContextField("regionId", node, path, profile, reasons);
            case ConditionNodeType.ACTION_ID_EQUALS -> requireContextField("actionId", node, path, profile, reasons);
            case ConditionNodeType.EVENT_METADATA_EXISTS, ConditionNodeType.EVENT_METADATA_EQUALS -> requireMetadataKey(node, path, profile, reasons);
            case ConditionNodeType.ITEM_STACK_EXISTS, ConditionNodeType.ITEM_STACK_MATCHES -> requireKey(node, path, profile.itemKeys(), "itemKey", "物品快照", reasons);
            case ConditionNodeType.INVENTORY_CONTAINS_ITEM, ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE -> requireKey(node, path, profile.inventoryKeys(), "inventoryKey", "背包快照", reasons);
            case ConditionNodeType.CONTAINER_SLOT_EMPTY, ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE -> requireKey(node, path, profile.containerKeys(), "containerKey", "容器快照", reasons);
            case ConditionNodeType.REGION_EXISTS, ConditionNodeType.REGION_ENABLED, ConditionNodeType.PLAYER_IN_REGION, ConditionNodeType.REGION_PLAYER_COUNT_COMPARE -> requireKey(node, path, profile.regionKeys(), "regionKey", "区域快照", reasons);
            case ConditionNodeType.SIGNAL_CHANNEL_EXISTS, ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE -> requireKey(node, path, profile.signalChannelKeys(), "signalChannelKey", "信号频道快照", reasons);
            case ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE -> requireKey(node, path, profile.signalHistoryKeys(), "signalHistoryKey", "信号历史快照", reasons);
            case ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE, ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL, ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE -> requireKey(node, path, profile.logicChainKeys(), "logicChainKey", "逻辑链快照", reasons);
            default -> {
                if (STATE_VARIABLE_TYPES.contains(type)) {
                    requireStateVariable(node, path, profile, reasons);
                }
            }
        }
    }

    private static void requireContextField(ConditionNode node, String path, ConditionGroupCompatibilityProfile profile, List<String> reasons) {
        requireContextField(config(node, "field"), node, path, profile, reasons);
    }

    private static void requireContextField(String field, ConditionNode node, String path, ConditionGroupCompatibilityProfile profile, List<String> reasons) {
        if (field == null || field.isBlank()) {
            return;
        }
        if (!profile.hasContextField(field)) {
            reasons.add(nodeLabel(path, node) + " 需要上下文字段 " + field + "，但 " + profile.displayName() + " 不提供该字段。");
        }
    }

    private static void requireMetadataKey(ConditionNode node, String path, ConditionGroupCompatibilityProfile profile, List<String> reasons) {
        String key = config(node, "key");
        if (key.isBlank()) {
            return;
        }
        if (!profile.eventMetadataKeys().contains(key)) {
            reasons.add(nodeLabel(path, node) + " 需要事件元数据 " + key + "，但 " + profile.displayName() + " 不提供该元数据。");
        }
    }

    private static void requireKey(
            ConditionNode node,
            String path,
            Set<String> availableKeys,
            String configKey,
            String displayName,
            List<String> reasons
    ) {
        String key = config(node, configKey);
        if (key.isBlank()) {
            return;
        }
        if (!availableKeys.contains(key)) {
            reasons.add(nodeLabel(path, node) + " 需要" + displayName + " key=" + key + "，但当前触发方式不提供。");
        }
    }

    private static void requireStateVariable(ConditionNode node, String path, ConditionGroupCompatibilityProfile profile, List<String> reasons) {
        String scope = config(node, "scope").toUpperCase(java.util.Locale.ROOT);
        String targetMode = config(node, "targetMode").toLowerCase(java.util.Locale.ROOT);
        if ("GLOBAL".equals(scope)) {
            if (!profile.globalStateVariables()) {
                reasons.add(nodeLabel(path, node) + " 需要全局状态变量，但当前触发方式不提供状态变量快照。");
            }
            return;
        }
        if (!"PLAYER".equals(scope)) {
            return;
        }
        if (!profile.playerStateVariables()) {
            reasons.add(nodeLabel(path, node) + " 需要玩家状态变量，但当前触发方式不提供玩家状态变量快照。");
            return;
        }
        if ("context_player".equals(targetMode) && !profile.playerContext()) {
            reasons.add(nodeLabel(path, node) + " 使用 context_player 玩家状态变量，但 " + profile.displayName() + " 不提供触发玩家。");
        }
    }

    private static String nodeLabel(String path, ConditionNode node) {
        String name = node == null ? "" : node.name();
        String id = node == null ? "" : node.id();
        if (name != null && !name.isBlank()) {
            return name + "（" + path + "）";
        }
        if (id != null && !id.isBlank()) {
            return id + "（" + path + "）";
        }
        return path;
    }

    private static String config(ConditionNode node, String key) {
        return node == null || node.config() == null ? "" : node.config().get(key);
    }
}
