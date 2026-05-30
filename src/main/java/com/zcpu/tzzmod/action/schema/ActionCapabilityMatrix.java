package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ActionCapabilityMatrix {
    private static final int DEFAULT_MAX_ACTIONS = 64;
    private static final Set<ActionType> CURRENT_ACTION_TYPES = Collections.unmodifiableSet(EnumSet.allOf(ActionType.class));
    private static final Map<ActionOwnerType, ActionOwnerCapability> CAPABILITIES_BY_OWNER = buildCapabilitiesByOwner();
    private static final List<ActionOwnerCapability> CAPABILITIES = List.copyOf(CAPABILITIES_BY_OWNER.values());

    private ActionCapabilityMatrix() {
    }

    public static Optional<ActionOwnerCapability> find(ActionOwnerType ownerType) {
        return ownerType == null ? Optional.empty() : Optional.ofNullable(CAPABILITIES_BY_OWNER.get(ownerType));
    }

    public static Optional<ActionOwnerCapability> findByOwnerId(String ownerId) {
        String id = safe(ownerId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        for (ActionOwnerCapability capability : CAPABILITIES) {
            if (capability.ownerType().id().equals(id)) {
                return Optional.of(capability);
            }
        }
        return Optional.empty();
    }

    public static ActionOwnerCapability require(ActionOwnerType ownerType) {
        return find(ownerType).orElseThrow(() -> new IllegalArgumentException("Unknown action owner: " + ownerType));
    }

    public static boolean supports(ActionOwnerType ownerType, ActionType actionType) {
        return find(ownerType).map(capability -> capability.supports(actionType)).orElse(false);
    }

    public static List<ActionOwnerCapability> capabilities() {
        return CAPABILITIES;
    }

    public static Set<ActionOwnerType> ownerTypes() {
        return CAPABILITIES_BY_OWNER.keySet();
    }

    public static Set<ActionType> actionTypesForOwner(ActionOwnerType ownerType) {
        return find(ownerType).map(ActionOwnerCapability::supportedActionTypes).orElse(Set.of());
    }

    private static Map<ActionOwnerType, ActionOwnerCapability> buildCapabilitiesByOwner() {
        EnumMap<ActionOwnerType, ActionOwnerCapability> capabilities = new EnumMap<>(ActionOwnerType.class);
        register(capabilities, capability(
                ActionOwnerType.SIGNAL_LISTENER,
                "signal_listener",
                "actions",
                "actions",
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.ACTION_RELAY,
                "action_relay",
                "actions",
                "actions",
                ConditionRuntimeTargetType.ACTION_RELAY,
                ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.REGION_ENTER,
                "region_controller",
                "enter",
                "enterActions",
                ConditionRuntimeTargetType.REGION_ENTER,
                ConditionRuntimeTargetType.REGION_ENTER_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.REGION_EXIT,
                "region_controller",
                "exit",
                "exitActions",
                ConditionRuntimeTargetType.REGION_EXIT,
                ConditionRuntimeTargetType.REGION_EXIT_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.REGION_STAY,
                "region_controller",
                "stay",
                "stayActions",
                ConditionRuntimeTargetType.REGION_STAY,
                ConditionRuntimeTargetType.REGION_STAY_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.TIMER_START,
                "timer",
                "start",
                "onStartActions",
                ConditionRuntimeTargetType.TIMER_ON_START,
                ConditionRuntimeTargetType.TIMER_ON_START_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.TIMER_TICK,
                "timer",
                "tick",
                "onTickActions",
                ConditionRuntimeTargetType.TIMER_ON_TICK,
                ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.TIMER_COMPLETE,
                "timer",
                "complete",
                "onCompleteActions",
                ConditionRuntimeTargetType.TIMER_ON_COMPLETE,
                ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION,
                true
        ));
        register(capabilities, capability(
                ActionOwnerType.TIMER_CANCEL,
                "timer",
                "cancel",
                "onCancelActions",
                ConditionRuntimeTargetType.TIMER_ON_CANCEL,
                ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION,
                true
        ));
        return Collections.unmodifiableMap(capabilities);
    }

    private static ActionOwnerCapability capability(
            ActionOwnerType ownerType,
            String ownerKind,
            String bucketId,
            String listFieldName,
            ConditionRuntimeTargetType ownerTarget,
            ConditionRuntimeTargetType actionTarget,
            boolean reorder
    ) {
        return new ActionOwnerCapability(
                ownerType,
                ownerKind,
                bucketId,
                listFieldName,
                DEFAULT_MAX_ACTIONS,
                CURRENT_ACTION_TYPES,
                ownerTarget,
                actionTarget,
                true,
                true,
                true,
                true,
                reorder
        );
    }

    private static void register(EnumMap<ActionOwnerType, ActionOwnerCapability> capabilities, ActionOwnerCapability capability) {
        ActionOwnerCapability previous = capabilities.put(capability.ownerType(), capability);
        if (previous != null) {
            throw new IllegalStateException("Duplicate action owner capability: " + capability.ownerType());
        }
    }

    static List<ActionOwnerType> declaredOwnerTypesForTest() {
        return new ArrayList<>(CAPABILITIES_BY_OWNER.keySet());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
