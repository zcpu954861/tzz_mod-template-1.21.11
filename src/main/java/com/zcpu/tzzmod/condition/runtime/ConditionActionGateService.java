package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public final class ConditionActionGateService {
    private final ConditionGateService gateService;
    private final ConditionGroupCompatibilityService compatibilityService;

    public ConditionActionGateService() {
        this(new ConditionGateService(), new ConditionGroupCompatibilityService());
    }

    ConditionActionGateService(ConditionGateService gateService, ConditionGroupCompatibilityService compatibilityService) {
        this.gateService = gateService == null ? new ConditionGateService() : gateService;
        this.compatibilityService = compatibilityService == null ? new ConditionGroupCompatibilityService() : compatibilityService;
    }

    public ConditionGateResult evaluate(
            MinecraftServer server,
            ActionConfig action,
            ConditionRuntimeTargetType actionTargetType,
            String actionTargetId,
            ConditionRuntimeTargetType parentTargetType,
            String parentTargetId,
            String parentActionBucket,
            int actionIndex,
            Supplier<ConditionEvaluationContext> parentContextSupplier
    ) {
        String conditionGroupId = WebAdminConditionGroupStore.normalizeId(action == null ? "" : action.conditionGroupId());
        if (conditionGroupId.isBlank()) {
            return ConditionGateResult.skippedResult();
        }
        ConditionRuntimeTargetType safeActionTargetType = actionTargetType == null
                ? ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION
                : actionTargetType;
        String safeActionTargetId = safe(actionTargetId);
        String safeParentTargetId = safe(parentTargetId);
        String actionType = action == null || action.type() == null ? "" : action.type().id();
        ConditionGroupCompatibilityProfile profile = compatibilityService.profile(safeActionTargetType);
        return gateService.evaluate(server, new ConditionGateRequest(
                conditionGroupId,
                safeActionTargetType,
                safeActionTargetId,
                () -> ConditionRuntimeContextBuilder.withActionMetadata(
                        parentContextSupplier == null ? null : parentContextSupplier.get(),
                        action,
                        safeActionTargetType,
                        safeActionTargetId,
                        parentTargetType,
                        safeParentTargetId,
                        parentActionBucket,
                        actionIndex
                ),
                profile,
                "ACTION",
                parentTargetType,
                safeParentTargetId,
                parentTargetType == null ? "" : parentTargetType.displayName(),
                actionIndex,
                actionIndex < 0 ? 0 : actionIndex + 1,
                actionType,
                parentActionBucket
        ));
    }

    public static String actionTargetId(String ownerKind, String ownerId, int actionIndex) {
        String safeOwnerKind = safe(ownerKind);
        String safeOwnerId = safe(ownerId);
        return safeOwnerKind + ":" + safeOwnerId + ":action:" + Math.max(0, actionIndex);
    }

    public static String regionActionTargetId(String controllerId, String bucket, int actionIndex) {
        return "region:" + safe(controllerId) + ":" + safe(bucket) + ":action:" + Math.max(0, actionIndex);
    }

    public static ConditionRuntimeTargetType regionActionTargetType(String bucket) {
        return switch (safe(bucket).toLowerCase(java.util.Locale.ROOT)) {
            case "enter" -> ConditionRuntimeTargetType.REGION_ENTER_ACTION;
            case "exit" -> ConditionRuntimeTargetType.REGION_EXIT_ACTION;
            default -> ConditionRuntimeTargetType.REGION_STAY_ACTION;
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
