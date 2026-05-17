package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConditionGroupCompatibilityServiceTest {
    private ConditionGroupCompatibilityServiceTest() {
    }

    public static void run() {
        testAlwaysTrueCompatibleWithAllProfiles();
        testPlayerContextFiltering();
        testStateVariableTargetModeFiltering();
        testContextFieldAndEventMetadataFiltering();
        testItemInventoryAndContainerKeys();
        testUnavailableRegionSignalLogicSnapshots();
        testNestedGroupsAndDisabledNodes();
        testChineseIncompatibilityReasons();
        testReceiverSideRuntimeGateProfiles();
        testSingleActionRuntimeGateProfiles();
        testTimerRuntimeGateProfiles();
    }

    private static void testAlwaysTrueCompatibleWithAllProfiles() {
        ConditionGroupDefinition group = definition("always", leaf(ConditionNodeType.ALWAYS_TRUE));
        for (ConditionRuntimeTargetType targetType : ConditionRuntimeTargetType.values()) {
            requireCompatible(group, targetType, "always_true compatible with " + targetType.id());
        }
    }

    private static void testPlayerContextFiltering() {
        ConditionGroupDefinition playerGroup = definition("player", leaf(ConditionNodeType.PLAYER_EXISTS));
        requireIncompatible(playerGroup, ConditionRuntimeTargetType.VBD_REDSTONE, "触发玩家", "player not compatible with redstone");
        requireIncompatible(playerGroup, ConditionRuntimeTargetType.VBD_BLOCKSTATE, "触发玩家", "player not compatible with blockstate");
        requireIncompatible(playerGroup, ConditionRuntimeTargetType.CONTAINER_CHANGE, "触发玩家", "player not compatible with container change without actor");
        requireCompatible(playerGroup, ConditionRuntimeTargetType.VBD_INTERACTION, "player compatible with interaction");
        requireCompatible(playerGroup, ConditionRuntimeTargetType.ITEM_SUBMIT, "player compatible with itemSubmit");
        requireCompatible(playerGroup, ConditionRuntimeTargetType.CONTAINER_OPEN, "player compatible with container open");
        requireCompatible(playerGroup, ConditionRuntimeTargetType.CONTAINER_CLOSE, "player compatible with container close");
    }

    private static void testStateVariableTargetModeFiltering() {
        ConditionGroupDefinition global = definition("global_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "true")));
        ConditionGroupDefinition contextPlayer = definition("player_state_context", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "player.ready", "targetMode", "context_player", "expected", "true")));
        ConditionGroupDefinition explicitPlayer = definition("player_state_explicit", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "player.ready", "targetMode", "explicit_target", "targetId", "player-1", "expected", "true")));

        for (ConditionRuntimeTargetType targetType : ConditionRuntimeTargetType.values()) {
            requireCompatible(global, targetType, "GLOBAL state variable compatible with " + targetType.id());
        }
        requireIncompatible(contextPlayer, ConditionRuntimeTargetType.VBD_REDSTONE, "context_player", "context player state rejected on redstone");
        requireIncompatible(contextPlayer, ConditionRuntimeTargetType.CONTAINER_CHANGE, "context_player", "context player state rejected on container change");
        requireCompatible(contextPlayer, ConditionRuntimeTargetType.VBD_INTERACTION, "context player state accepted on interaction");
        requireCompatible(contextPlayer, ConditionRuntimeTargetType.ITEM_SUBMIT, "context player state accepted on itemSubmit");
        requireCompatible(explicitPlayer, ConditionRuntimeTargetType.VBD_REDSTONE, "explicit player state accepted on redstone profile");
    }

    private static void testContextFieldAndEventMetadataFiltering() {
        ConditionGroupDefinition trigger = definition("event_trigger", leaf(ConditionNodeType.EVENT_METADATA_EXISTS, config("key", "trigger")));
        ConditionGroupDefinition detail = definition("event_detail", leaf(ConditionNodeType.EVENT_METADATA_EXISTS, config("key", "detail")));
        ConditionGroupDefinition redstoneEdge = definition("event_edge", leaf(ConditionNodeType.EVENT_METADATA_EXISTS, config("key", "edge")));
        ConditionGroupDefinition variables = definition("variables_context", leaf(ConditionNodeType.CONTEXT_EQUALS,
                config("field", "variables.foo", "expected", "bar")));

        requireCompatible(trigger, ConditionRuntimeTargetType.VBD_REDSTONE, "event.trigger metadata is provided by runtime builder");
        requireCompatible(detail, ConditionRuntimeTargetType.CONTAINER_CHANGE, "event.detail metadata is provided by runtime builder");
        requireIncompatible(redstoneEdge, ConditionRuntimeTargetType.VBD_REDSTONE, "事件元数据 edge", "redstone edge metadata is not advertised without runtime snapshot");
        requireIncompatible(variables, ConditionRuntimeTargetType.VBD_INTERACTION, "上下文字段 variables.foo", "variables.* context fields are not advertised unless populated");
    }

    private static void testItemInventoryAndContainerKeys() {
        ConditionGroupDefinition heldItem = definition("held_item", leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "held_item")));
        ConditionGroupDefinition submittedItem = definition("submitted_item", leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "submitted_item")));
        ConditionGroupDefinition offHandItem = definition("off_hand", leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "off_hand")));
        ConditionGroupDefinition inventory = definition("inventory", leaf(ConditionNodeType.INVENTORY_CONTAINS_ITEM,
                config("inventoryKey", "player_inventory", "itemId", "minecraft:diamond", "countOperator", "gte", "count", "1")));
        ConditionGroupDefinition container = definition("container", leaf(ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE,
                config("containerKey", "container", "itemId", "minecraft:diamond", "operator", "gte", "count", "1")));

        requireCompatible(heldItem, ConditionRuntimeTargetType.VBD_INTERACTION, "held item compatible with interaction");
        requireCompatible(submittedItem, ConditionRuntimeTargetType.ITEM_SUBMIT, "submitted item compatible with itemSubmit");
        requireCompatible(offHandItem, ConditionRuntimeTargetType.ITEM_SUBMIT, "off hand snapshot compatible with itemSubmit");
        requireIncompatible(heldItem, ConditionRuntimeTargetType.VBD_REDSTONE, "物品快照", "item snapshot not compatible with redstone");
        requireIncompatible(submittedItem, ConditionRuntimeTargetType.CONTAINER_CHANGE, "物品快照", "submitted item not compatible with container change");

        requireCompatible(inventory, ConditionRuntimeTargetType.VBD_INTERACTION, "inventory compatible with interaction");
        requireCompatible(inventory, ConditionRuntimeTargetType.ITEM_SUBMIT, "inventory compatible with itemSubmit");
        requireIncompatible(inventory, ConditionRuntimeTargetType.VBD_REDSTONE, "背包快照", "inventory not compatible with redstone");
        requireIncompatible(inventory, ConditionRuntimeTargetType.CONTAINER_CHANGE, "背包快照", "inventory not compatible with container change");

        requireIncompatible(container, ConditionRuntimeTargetType.CONTAINER_OPEN, "容器快照", "container snapshot not advertised for open without target Inventory capability");
        requireIncompatible(container, ConditionRuntimeTargetType.CONTAINER_CLOSE, "容器快照", "container snapshot not advertised for close without target Inventory capability");
        requireCompatible(container, ConditionRuntimeTargetType.CONTAINER_OPEN, true, "Inventory container open supports container snapshot group");
        requireCompatible(container, ConditionRuntimeTargetType.CONTAINER_CLOSE, true, "Inventory container close supports container snapshot group");
        requireCompatible(container, ConditionRuntimeTargetType.CONTAINER_CHANGE, "container compatible with change");
        requireIncompatible(container, ConditionRuntimeTargetType.VBD_INTERACTION, "容器快照", "container not compatible with interaction");
    }

    private static void testUnavailableRegionSignalLogicSnapshots() {
        ConditionGroupDefinition region = definition("region", leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "spawn")));
        ConditionGroupDefinition playerRegion = definition("player_region", leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "spawn", "playerMode", "context_player")));
        ConditionGroupDefinition signal = definition("signal", leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                config("signalHistoryKey", "history", "channel", "mission.start", "operator", "gte", "count", "1")));
        ConditionGroupDefinition logic = definition("logic", leaf(ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, config("logicChainKey", "chain")));
        requireIncompatible(region, ConditionRuntimeTargetType.VBD_INTERACTION, "区域快照", "region snapshot unavailable in 8.6 profiles");
        requireIncompatible(playerRegion, ConditionRuntimeTargetType.VBD_INTERACTION, "区域快照", "player_in_region also requires region snapshot");
        requireIncompatible(signal, ConditionRuntimeTargetType.ITEM_SUBMIT, "信号历史快照", "signal history unavailable in 8.6 profiles");
        requireIncompatible(logic, ConditionRuntimeTargetType.CONTAINER_OPEN, "逻辑链快照", "logic chain unavailable in 8.6 profiles");
    }

    private static void testNestedGroupsAndDisabledNodes() {
        ConditionNode disabledPlayer = new ConditionNode(
                "disabled_player",
                ConditionNodeType.PLAYER_EXISTS,
                "",
                "",
                false,
                ConditionGroupMode.AND,
                ConditionNodeConfig.EMPTY,
                List.of()
        );
        ConditionGroupDefinition disabledOnly = definition("disabled", ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                ConditionNode.group("nested", ConditionGroupMode.OR, List.of(disabledPlayer, leaf(ConditionNodeType.ALWAYS_TRUE)))
        )));
        requireCompatible(disabledOnly, ConditionRuntimeTargetType.VBD_REDSTONE, "disabled incompatible player node ignored for compatibility");

        ConditionGroupDefinition nestedContainer = definition("nested_container", ConditionNode.group("root", ConditionGroupMode.AND, List.of(
                ConditionNode.not("not", leaf(ConditionNodeType.ALWAYS_FALSE)),
                ConditionNode.group("nested", ConditionGroupMode.OR, List.of(
                        leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY, config("containerKey", "container", "slot", "0"))
                ))
        )));
        requireIncompatible(nestedContainer, ConditionRuntimeTargetType.VBD_INTERACTION, "容器快照", "nested incompatible node is detected");
    }

    private static void testChineseIncompatibilityReasons() {
        ConditionGroupDefinition playerGroup = definition("player_reason", leaf(ConditionNodeType.PLAYER_EXISTS));
        ConditionGroupCompatibilityResult result = new ConditionGroupCompatibilityService().analyze(playerGroup, ConditionRuntimeTargetType.VBD_REDSTONE);
        requireFalse(result.compatible(), "Chinese reason incompatible result");
        requireTrue(result.reasons().stream().anyMatch(ConditionGroupCompatibilityServiceTest::containsChinese), "incompatibility reason is Chinese");
        requireContains(result.message(), "触发玩家", "incompatibility message Chinese");
    }

    private static void testReceiverSideRuntimeGateProfiles() {
        ConditionGroupDefinition playerTag = definition("player_tag", leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")));
        ConditionGroupDefinition contextPlayerState = definition("context_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "context_player", "expected", "true")));
        ConditionGroupDefinition explicitPlayerState = definition("explicit_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "explicit_target", "targetId", "player-1", "expected", "true")));
        ConditionGroupDefinition region = definition("region_snapshot", leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "region")));
        ConditionGroupDefinition currentRegion = definition("current_region_snapshot", leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "current_region", "playerMode", "context_player")));
        ConditionGroupDefinition heldItem = definition("held_item_receiver", leaf(ConditionNodeType.ITEM_STACK_EXISTS, config("itemKey", "held_item")));
        ConditionGroupDefinition signalHistory = definition("signal_history_receiver", leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                config("signalHistoryKey", "history", "channel", "mission.start", "operator", "gte", "count", "1")));
        ConditionGroupDefinition listenerContext = definition("listener_context", leaf(ConditionNodeType.LISTENER_ID_EQUALS, config("listenerId", "listener-1")));
        ConditionGroupDefinition relayContext = definition("relay_context", leaf(ConditionNodeType.DEVICE_ID_EQUALS, config("deviceId", "relay-1")));

        requireIncompatible(playerTag, ConditionRuntimeTargetType.SIGNAL_LISTENER, "触发玩家", "generic SignalListener profile rejects player-dependent condition");
        requireIncompatible(playerTag, ConditionRuntimeTargetType.ACTION_RELAY, "触发玩家", "generic ActionRelay profile rejects player-dependent condition");
        requireIncompatible(contextPlayerState, ConditionRuntimeTargetType.SIGNAL_LISTENER, "context_player", "SignalListener rejects context_player state variable");
        requireIncompatible(contextPlayerState, ConditionRuntimeTargetType.ACTION_RELAY, "context_player", "ActionRelay rejects context_player state variable");
        requireCompatible(explicitPlayerState, ConditionRuntimeTargetType.SIGNAL_LISTENER, "SignalListener accepts explicit PLAYER state variable");
        requireCompatible(explicitPlayerState, ConditionRuntimeTargetType.ACTION_RELAY, "ActionRelay accepts explicit PLAYER state variable");
        requireCompatible(listenerContext, ConditionRuntimeTargetType.SIGNAL_LISTENER, "SignalListener profile exposes listenerId");
        requireCompatible(relayContext, ConditionRuntimeTargetType.ACTION_RELAY, "ActionRelay profile exposes deviceId");
        requireIncompatible(heldItem, ConditionRuntimeTargetType.REGION_ENTER, "物品快照", "Region profiles do not advertise item snapshots");
        requireIncompatible(signalHistory, ConditionRuntimeTargetType.REGION_STAY, "信号历史快照", "Region profiles do not advertise signal history snapshots");
        requireCompatible(playerTag, ConditionRuntimeTargetType.REGION_ENTER, "Region enter accepts player-dependent condition");
        requireCompatible(contextPlayerState, ConditionRuntimeTargetType.REGION_EXIT, "Region exit accepts context_player state variable");
        requireCompatible(region, ConditionRuntimeTargetType.REGION_ENTER, "Region enter supports region snapshot key");
        requireCompatible(currentRegion, ConditionRuntimeTargetType.REGION_STAY, "Region stay supports current_region snapshot key");
    }

    private static void testSingleActionRuntimeGateProfiles() {
        ConditionGroupDefinition playerTag = definition("action_player_tag", leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")));
        ConditionGroupDefinition contextPlayerState = definition("action_context_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "context_player", "expected", "true")));
        ConditionGroupDefinition explicitPlayerState = definition("action_explicit_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "explicit_target", "targetId", "player-1", "expected", "true")));
        ConditionGroupDefinition actionType = definition("action_type_context", leaf(ConditionNodeType.CONTEXT_EQUALS,
                config("field", "actionType", "expected", "signal")));
        ConditionGroupDefinition actionEventType = definition("action_type_event", leaf(ConditionNodeType.EVENT_METADATA_EXISTS,
                config("key", "actionType")));
        ConditionGroupDefinition parentTarget = definition("parent_target_context", leaf(ConditionNodeType.CONTEXT_EQUALS,
                config("field", "parentTargetId", "expected", "listener-1")));
        ConditionGroupDefinition region = definition("action_region_snapshot", leaf(ConditionNodeType.REGION_ENABLED, config("regionKey", "region")));
        ConditionGroupDefinition currentRegion = definition("action_current_region_snapshot", leaf(ConditionNodeType.PLAYER_IN_REGION, config("regionKey", "current_region", "playerMode", "context_player")));
        ConditionGroupDefinition container = definition("action_container_snapshot", leaf(ConditionNodeType.CONTAINER_SLOT_EMPTY,
                config("containerKey", "container", "slot", "0")));
        ConditionGroupDefinition signalHistory = definition("action_signal_history", leaf(ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                config("signalHistoryKey", "history", "channel", "mission.start", "operator", "gte", "count", "1")));

        for (ConditionRuntimeTargetType targetType : List.of(
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                ConditionRuntimeTargetType.REGION_ENTER_ACTION,
                ConditionRuntimeTargetType.REGION_EXIT_ACTION,
                ConditionRuntimeTargetType.REGION_STAY_ACTION
        )) {
            requireCompatible(actionType, targetType, targetType.id() + " exposes actionType context");
            requireCompatible(actionEventType, targetType, targetType.id() + " exposes actionType event metadata");
            requireCompatible(parentTarget, targetType, targetType.id() + " exposes parentTargetId context");
            requireCompatible(explicitPlayerState, targetType, targetType.id() + " accepts explicit PLAYER state variable");
            requireIncompatible(container, targetType, "容器快照", targetType.id() + " excludes container snapshot");
            requireIncompatible(signalHistory, targetType, "信号历史快照", targetType.id() + " excludes signal history snapshot");
        }

        for (ConditionRuntimeTargetType noPlayerTarget : List.of(
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION
        )) {
            requireIncompatible(playerTag, noPlayerTarget, "触发玩家", noPlayerTarget.id() + " rejects player context");
            requireIncompatible(contextPlayerState, noPlayerTarget, "context_player", noPlayerTarget.id() + " rejects context_player state");
            requireIncompatible(region, noPlayerTarget, "区域快照", noPlayerTarget.id() + " excludes region snapshot");
        }

        for (ConditionRuntimeTargetType regionActionTarget : List.of(
                ConditionRuntimeTargetType.REGION_ENTER_ACTION,
                ConditionRuntimeTargetType.REGION_EXIT_ACTION,
                ConditionRuntimeTargetType.REGION_STAY_ACTION
        )) {
            requireCompatible(playerTag, regionActionTarget, regionActionTarget.id() + " accepts player-dependent condition");
            requireCompatible(contextPlayerState, regionActionTarget, regionActionTarget.id() + " accepts context_player state");
            requireCompatible(region, regionActionTarget, regionActionTarget.id() + " exposes region snapshot");
            requireCompatible(currentRegion, regionActionTarget, regionActionTarget.id() + " exposes current_region snapshot");
        }
    }

    private static void testTimerRuntimeGateProfiles() {
        ConditionGroupDefinition playerTag = definition("timer_player_tag", leaf(ConditionNodeType.PLAYER_HAS_TAG, config("tag", "runner")));
        ConditionGroupDefinition contextPlayerState = definition("timer_context_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "context_player", "expected", "true")));
        ConditionGroupDefinition explicitPlayerState = definition("timer_explicit_player_state", leaf(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                config("scope", "PLAYER", "key", "ready", "targetMode", "explicit_target", "targetId", "player-1", "expected", "true")));
        ConditionGroupDefinition timerId = definition("timer_id_context", leaf(ConditionNodeType.CONTEXT_EQUALS,
                config("field", "timerId", "expected", "timer.demo")));
        ConditionGroupDefinition timerEventDetail = definition("timer_detail_event", leaf(ConditionNodeType.EVENT_METADATA_EXISTS,
                config("key", "detail")));
        ConditionGroupDefinition actionType = definition("timer_action_type", leaf(ConditionNodeType.CONTEXT_EQUALS,
                config("field", "actionType", "expected", "signal")));

        for (ConditionRuntimeTargetType targetType : List.of(
                ConditionRuntimeTargetType.TIMER_ON_START,
                ConditionRuntimeTargetType.TIMER_ON_TICK,
                ConditionRuntimeTargetType.TIMER_ON_COMPLETE,
                ConditionRuntimeTargetType.TIMER_ON_CANCEL
        )) {
            requireCompatible(timerId, targetType, targetType.id() + " exposes timerId context");
            requireCompatible(timerEventDetail, targetType, targetType.id() + " exposes timer detail metadata");
            requireCompatible(explicitPlayerState, targetType, targetType.id() + " accepts explicit PLAYER state variable");
            requireIncompatible(playerTag, targetType, "触发玩家", targetType.id() + " conservatively rejects player context");
            requireIncompatible(contextPlayerState, targetType, "context_player", targetType.id() + " conservatively rejects context_player state");
        }

        for (ConditionRuntimeTargetType targetType : List.of(
                ConditionRuntimeTargetType.TIMER_ON_START_ACTION,
                ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION,
                ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION,
                ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION
        )) {
            requireCompatible(timerId, targetType, targetType.id() + " exposes timerId context");
            requireCompatible(timerEventDetail, targetType, targetType.id() + " exposes timer detail metadata");
            requireCompatible(actionType, targetType, targetType.id() + " exposes action metadata");
            requireCompatible(explicitPlayerState, targetType, targetType.id() + " accepts explicit PLAYER state variable");
            requireIncompatible(playerTag, targetType, "触发玩家", targetType.id() + " conservatively rejects player context");
            requireIncompatible(contextPlayerState, targetType, "context_player", targetType.id() + " conservatively rejects context_player state");
        }
    }

    private static void requireCompatible(ConditionGroupDefinition definition, ConditionRuntimeTargetType targetType, String message) {
        ConditionGroupCompatibilityResult result = new ConditionGroupCompatibilityService().analyze(definition, targetType);
        requireTrue(result.compatible(), message + " reasons=" + result.reasons());
    }

    private static void requireCompatible(
            ConditionGroupDefinition definition,
            ConditionRuntimeTargetType targetType,
            boolean containerSnapshotForOpenClose,
            String message
    ) {
        ConditionGroupCompatibilityService service = new ConditionGroupCompatibilityService();
        ConditionGroupCompatibilityResult result = service.analyze(definition, service.profile(targetType, containerSnapshotForOpenClose));
        requireTrue(result.compatible(), message + " reasons=" + result.reasons());
    }

    private static void requireIncompatible(ConditionGroupDefinition definition, ConditionRuntimeTargetType targetType, String expectedReason, String message) {
        ConditionGroupCompatibilityResult result = new ConditionGroupCompatibilityService().analyze(definition, targetType);
        requireFalse(result.compatible(), message);
        requireContains(result.message(), expectedReason, message + " Chinese reason");
    }

    private static ConditionGroupDefinition definition(String id, ConditionNode root) {
        return ConditionGroupDefinition.of(id, ConditionNode.group("root", ConditionGroupMode.AND, List.of(root)));
    }

    private static ConditionNode leaf(String type) {
        return ConditionNode.leaf("node-" + type, type);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf("node-" + type, type, config);
    }

    private static ConditionNodeConfig config(String... entries) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(entries[index], entries[index + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }

    private static boolean containsChinese(String value) {
        if (value == null) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(index));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
