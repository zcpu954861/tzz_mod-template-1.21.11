package com.zcpu.tzzmod.webadmin.itemsubmit;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import java.util.Map;
import java.util.UUID;

public final class WebAdminSingleItemSubmitTemplateSession {
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
    public final Map<String, Object> template;
    public boolean opened;
    public boolean terminal;

    public WebAdminSingleItemSubmitTemplateSession(
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
            Map<String, Object> template
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
        this.template = template == null ? Map.of() : Map.copyOf(template);
    }
}
