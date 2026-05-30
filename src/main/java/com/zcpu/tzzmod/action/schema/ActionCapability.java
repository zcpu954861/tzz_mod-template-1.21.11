package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import java.util.Objects;
import java.util.Set;

public record ActionCapability(
        ActionOwnerType ownerType,
        Set<ActionType> actionTypes,
        String displayName,
        String boundaryNote
) {
    public ActionCapability {
        // Capability 在 Phase 1 只是 owner -> existing ActionType 的静态索引。
        // 它不持有锁、fingerprint、写入 adapter 或 condition compatibility 规则；
        // 这些会在 Phase 2 后端 validation 中接入，避免 metadata 阶段改变旧保存语义。
        ownerType = Objects.requireNonNull(ownerType, "ownerType");
        actionTypes = actionTypes == null ? Set.of() : Set.copyOf(actionTypes);
        displayName = displayName == null ? "" : displayName.trim();
        boundaryNote = boundaryNote == null ? "" : boundaryNote.trim();
    }
}
