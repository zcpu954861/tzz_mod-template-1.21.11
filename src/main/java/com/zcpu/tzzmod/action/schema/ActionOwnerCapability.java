package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import java.util.Objects;
import java.util.Set;

public record ActionOwnerCapability(
        ActionOwnerType ownerType,
        String ownerKind,
        String bucketId,
        String listFieldName,
        int maxActions,
        Set<ActionType> supportedActionTypes,
        ConditionRuntimeTargetType ownerConditionTargetType,
        ConditionRuntimeTargetType actionConditionTargetType,
        boolean supportsAppend,
        boolean supportsSameIndexEdit,
        boolean supportsDelete,
        boolean supportsClear,
        boolean supportsSameBucketReorder
) {
    public ActionOwnerCapability {
        // Phase 2 capability matrix 只描述 owner/bucket 的保存边界和 condition target。
        // 它不持有 edit lock、fingerprint、store writer 或 audit/realtime adapter，避免把 owner 写流程集中成新巨型服务。
        ownerType = Objects.requireNonNull(ownerType, "ownerType");
        ownerKind = safe(ownerKind);
        bucketId = safe(bucketId);
        listFieldName = safe(listFieldName);
        supportedActionTypes = supportedActionTypes == null ? Set.of() : Set.copyOf(supportedActionTypes);
        ownerConditionTargetType = Objects.requireNonNull(ownerConditionTargetType, "ownerConditionTargetType");
        actionConditionTargetType = Objects.requireNonNull(actionConditionTargetType, "actionConditionTargetType");
        if (maxActions < 0) {
            throw new IllegalArgumentException("maxActions must not be negative");
        }
    }

    public boolean supports(ActionType actionType) {
        return actionType != null && supportedActionTypes.contains(actionType);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
