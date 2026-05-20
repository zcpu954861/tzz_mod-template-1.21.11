package com.zcpu.tzzmod.webadmin.dto;

import java.util.ArrayList;
import java.util.List;

public final class WebAdminLogicChainEditorRequest {
    public String rootType = "channel";
    public String rootRef = "";
    public boolean includeDisabled = true;
    public int maxDepth = 3;
    public String baseGraphFingerprint = "";
    public String lockId = "";
    public String lockTargetType = "";
    public String lockTargetId = "";
    public List<DraftNode> nodes = new ArrayList<>();
    public List<DraftEdge> edges = new ArrayList<>();
    public List<ChannelMetadataDraft> channelMetadataDrafts = new ArrayList<>();
    public ActionAppendDraft actionAppend = null;
    public List<ExistingNodeEditDraft> existingNodeEdits = new ArrayList<>();
    public List<ActionEditDraft> actionEdits = new ArrayList<>();

    public static final class DraftNode {
        public String id = "";
        public String type = "";
        public String column = "";
        public int slot = 0;
        public boolean placed = false;
        public WebAdminSignalJoinRequest signalJoin = new WebAdminSignalJoinRequest();
        public WebAdminTimerRequest timer = new WebAdminTimerRequest();
    }

    public static final class DraftEdge {
        public String id = "";
        public String from = "";
        public String to = "";
        public String type = "";
    }

    public static final class ChannelMetadataDraft {
        public String channel = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public boolean newChannel = false;
    }

    public static final class ActionAppendDraft {
        public String ownerType = "";
        public String ownerId = "";
        public String bucket = "";
        public WebAdminActionRelayActionsUpdateRequest.ActionEntry action = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ExistingNodeEditDraft {
        public String nodeType = "";
        public String targetId = "";
        public WebAdminChannelMetadataUpdateRequest channelMetadata = null;
        public WebAdminSignalJoinRequest signalJoin = null;
        public WebAdminTimerRequest timer = null;
        public WebAdminSignalListenerBasicConfigUpdateRequest signalListenerBasic = null;
    }

    public static final class ActionEditDraft {
        public String ownerType = "";
        public String ownerId = "";
        public String bucket = "";
        public Object actionIndex = 0;
        public String operation = "replace";
        public WebAdminActionRelayActionsUpdateRequest.ActionEntry action = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        public String expectedFingerprint = "";
        public String lockId = "";
    }
}
