package com.zcpu.tzzmod.webadmin.container;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WebAdminContainerTemplateSession {
    public final String sessionId;
    public final String nonce;
    public final String deviceId;
    public final String deviceDisplayName;
    public final String dimension;
    public final int x;
    public final int y;
    public final int z;
    public final String blockId;
    public final UUID targetPlayerUuid;
    public final String targetPlayerName;
    public final String lockId;
    public final String expectedFingerprint;
    public final long createdAtMillis;
    public final long expiresAtMillis;
    public final WebAdminUser actorUser;
    public final WebAdminSession actorSession;
    public final WebAdminWriteContext context;
    public final List<Map<String, Object>> itemConditions;
    public final boolean logicChainDraftOnly;
    public final String logicChainCaptureDraftId;
    public final String logicChainEditLockId;
    public final String logicChainRootType;
    public final String logicChainRootRef;
    public final String logicChainDraftNodeId;
    public final String logicChainTriggerKey;
    public final int logicChainRequirementIndex;
    public boolean opened;
    public boolean terminal;

    public WebAdminContainerTemplateSession(
            String sessionId,
            String nonce,
            String deviceId,
            String deviceDisplayName,
            String dimension,
            int x,
            int y,
            int z,
            String blockId,
            UUID targetPlayerUuid,
            String targetPlayerName,
            String lockId,
            String expectedFingerprint,
            long createdAtMillis,
            long expiresAtMillis,
            WebAdminUser actorUser,
            WebAdminSession actorSession,
            WebAdminWriteContext context,
            List<Map<String, Object>> itemConditions,
            boolean logicChainDraftOnly,
            String logicChainCaptureDraftId,
            String logicChainEditLockId,
            String logicChainRootType,
            String logicChainRootRef,
            String logicChainDraftNodeId,
            String logicChainTriggerKey,
            int logicChainRequirementIndex
    ) {
        this.sessionId = sessionId == null ? "" : sessionId;
        this.nonce = nonce == null ? "" : nonce;
        this.deviceId = deviceId == null ? "" : deviceId;
        this.deviceDisplayName = deviceDisplayName == null ? "" : deviceDisplayName;
        this.dimension = dimension == null ? "" : dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId == null ? "" : blockId;
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = targetPlayerName == null ? "" : targetPlayerName;
        this.lockId = lockId == null ? "" : lockId;
        this.expectedFingerprint = expectedFingerprint == null ? "" : expectedFingerprint;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.actorUser = actorUser;
        this.actorSession = actorSession;
        this.context = context;
        this.itemConditions = itemConditions == null ? List.of() : List.copyOf(itemConditions);
        this.logicChainDraftOnly = logicChainDraftOnly;
        this.logicChainCaptureDraftId = logicChainCaptureDraftId == null ? "" : logicChainCaptureDraftId;
        this.logicChainEditLockId = logicChainEditLockId == null ? "" : logicChainEditLockId;
        this.logicChainRootType = logicChainRootType == null ? "" : logicChainRootType;
        this.logicChainRootRef = logicChainRootRef == null ? "" : logicChainRootRef;
        this.logicChainDraftNodeId = logicChainDraftNodeId == null ? "" : logicChainDraftNodeId;
        this.logicChainTriggerKey = logicChainTriggerKey == null ? "" : logicChainTriggerKey;
        this.logicChainRequirementIndex = logicChainRequirementIndex;
    }
}
