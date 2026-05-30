package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import java.util.EnumSet;
import java.util.Set;

public final class ActionCapabilityMatrixTest {
    private static final Set<ActionOwnerType> EXPECTED_OWNERS = EnumSet.of(
            ActionOwnerType.SIGNAL_LISTENER,
            ActionOwnerType.ACTION_RELAY,
            ActionOwnerType.REGION_ENTER,
            ActionOwnerType.REGION_EXIT,
            ActionOwnerType.REGION_STAY,
            ActionOwnerType.TIMER_START,
            ActionOwnerType.TIMER_TICK,
            ActionOwnerType.TIMER_COMPLETE,
            ActionOwnerType.TIMER_CANCEL
    );

    private ActionCapabilityMatrixTest() {
    }

    public static void run() {
        requireOwnerSetAndNoNonOwners();
        requireEveryCurrentOwnerSupportsCurrentActionTypes();
        requireOwnerTargetsAndListFields();
        requireMatrixIsImmutable();
    }

    private static void requireOwnerSetAndNoNonOwners() {
        require(ActionCapabilityMatrix.ownerTypes().equals(EXPECTED_OWNERS), "Phase 2 matrix owner set changed");
        require(ActionCapabilityMatrix.find(null).isEmpty(), "null owner must not resolve");
        require(ActionCapabilityMatrix.findByOwnerId("").isEmpty(), "blank owner id must not resolve");
        for (ActionOwnerType ownerType : EXPECTED_OWNERS) {
            require(ActionCapabilityMatrix.findByOwnerId(ownerType.id()).orElseThrow().ownerType() == ownerType,
                    "owner id lookup must resolve exact documented id for " + ownerType);
            require(ActionCapabilityMatrix.findByOwnerId(ownerType.name()).isEmpty(),
                    "owner id lookup must not accept enum name for " + ownerType);
        }
        require(ActionCapabilityMatrix.findByOwnerId("vbd_trigger").isEmpty(), "VBD trigger must not be an ActionConfig owner");
        require(ActionCapabilityMatrix.findByOwnerId("item_submit").isEmpty(), "itemSubmit must not be an ActionConfig owner");
        require(ActionCapabilityMatrix.findByOwnerId("container_change").isEmpty(), "container must not be an ActionConfig owner");
        require(ActionCapabilityMatrix.findByOwnerId("branch").isEmpty(), "Program branch must not be an ActionConfig owner");
    }

    private static void requireEveryCurrentOwnerSupportsCurrentActionTypes() {
        Set<ActionType> expectedTypes = EnumSet.allOf(ActionType.class);
        for (ActionOwnerType ownerType : EXPECTED_OWNERS) {
            ActionOwnerCapability capability = ActionCapabilityMatrix.require(ownerType);
            require(capability.maxActions() == 64, ownerType + " max action count must remain 64");
            require(capability.supportedActionTypes().equals(expectedTypes), ownerType + " must support the current ActionType set");
            for (ActionType actionType : expectedTypes) {
                require(ActionCapabilityMatrix.supports(ownerType, actionType), ownerType + " must support " + actionType);
            }
            require(capability.supportsAppend(), ownerType + " must support append");
            require(capability.supportsSameIndexEdit(), ownerType + " must support same-index edit");
            require(capability.supportsDelete(), ownerType + " must support delete");
            require(capability.supportsClear(), ownerType + " must support clear");
        }
    }

    private static void requireOwnerTargetsAndListFields() {
        requireCapability(ActionOwnerType.SIGNAL_LISTENER, "signal_listener", "actions", "actions",
                ConditionRuntimeTargetType.SIGNAL_LISTENER, ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION, true);
        requireCapability(ActionOwnerType.ACTION_RELAY, "action_relay", "actions", "actions",
                ConditionRuntimeTargetType.ACTION_RELAY, ConditionRuntimeTargetType.ACTION_RELAY_ACTION, true);
        requireCapability(ActionOwnerType.REGION_ENTER, "region_controller", "enter", "enterActions",
                ConditionRuntimeTargetType.REGION_ENTER, ConditionRuntimeTargetType.REGION_ENTER_ACTION, true);
        requireCapability(ActionOwnerType.REGION_EXIT, "region_controller", "exit", "exitActions",
                ConditionRuntimeTargetType.REGION_EXIT, ConditionRuntimeTargetType.REGION_EXIT_ACTION, true);
        requireCapability(ActionOwnerType.REGION_STAY, "region_controller", "stay", "stayActions",
                ConditionRuntimeTargetType.REGION_STAY, ConditionRuntimeTargetType.REGION_STAY_ACTION, true);
        requireCapability(ActionOwnerType.TIMER_START, "timer", "start", "onStartActions",
                ConditionRuntimeTargetType.TIMER_ON_START, ConditionRuntimeTargetType.TIMER_ON_START_ACTION, true);
        requireCapability(ActionOwnerType.TIMER_TICK, "timer", "tick", "onTickActions",
                ConditionRuntimeTargetType.TIMER_ON_TICK, ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION, true);
        requireCapability(ActionOwnerType.TIMER_COMPLETE, "timer", "complete", "onCompleteActions",
                ConditionRuntimeTargetType.TIMER_ON_COMPLETE, ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION, true);
        requireCapability(ActionOwnerType.TIMER_CANCEL, "timer", "cancel", "onCancelActions",
                ConditionRuntimeTargetType.TIMER_ON_CANCEL, ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION, true);
    }

    private static void requireCapability(
            ActionOwnerType ownerType,
            String ownerKind,
            String bucketId,
            String listFieldName,
            ConditionRuntimeTargetType ownerTarget,
            ConditionRuntimeTargetType actionTarget,
            boolean reorder
    ) {
        ActionOwnerCapability capability = ActionCapabilityMatrix.require(ownerType);
        require(ownerKind.equals(capability.ownerKind()), ownerType + " ownerKind mismatch");
        require(bucketId.equals(capability.bucketId()), ownerType + " bucket mismatch");
        require(listFieldName.equals(capability.listFieldName()), ownerType + " list field mismatch");
        require(ownerTarget == capability.ownerConditionTargetType(), ownerType + " owner condition target mismatch");
        require(actionTarget == capability.actionConditionTargetType(), ownerType + " action condition target mismatch");
        require(capability.supportsSameBucketReorder() == reorder, ownerType + " reorder capability mismatch");
    }

    private static void requireMatrixIsImmutable() {
        ActionOwnerCapability first = ActionCapabilityMatrix.capabilities().getFirst();
        expectUnsupported(() -> ActionCapabilityMatrix.capabilities().add(first), "capability list must be immutable");
        expectUnsupported(() -> ActionCapabilityMatrix.ownerTypes().add(ActionOwnerType.ACTION_RELAY), "owner set must be immutable");
        expectUnsupported(() -> first.supportedActionTypes().add(ActionType.COMMAND), "supported type set must be immutable");
        expectUnsupported(() -> ActionCapabilityMatrix.actionTypesForOwner(ActionOwnerType.ACTION_RELAY).add(ActionType.COMMAND),
                "owner action type lookup must be immutable");
    }

    private static void expectUnsupported(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
