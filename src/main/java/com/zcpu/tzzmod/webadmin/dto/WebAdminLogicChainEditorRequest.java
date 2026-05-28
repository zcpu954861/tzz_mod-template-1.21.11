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
    public List<NodeDeleteDraft> nodeDeletes = new ArrayList<>();
    public List<ActionDeleteDraft> actionDeletes = new ArrayList<>();
    public List<ActionReorderDraft> actionReorders = new ArrayList<>();

    public static final class DraftNode {
        public String id = "";
        public String type = "";
        public String column = "";
        public int slot = 0;
        public boolean placed = false;
        public String protectedDraftId = "";
        public WebAdminSignalJoinRequest signalJoin = new WebAdminSignalJoinRequest();
        public WebAdminTimerRequest timer = new WebAdminTimerRequest();
        public WebAdminSignalListenerCreateRequest signalListener = new WebAdminSignalListenerCreateRequest();
        public VirtualBlockDeviceDraft virtualBlockDevice = new VirtualBlockDeviceDraft();
        public WorldDeviceDraft worldDevice = new WorldDeviceDraft();
        public RegionControllerDraft regionController = new RegionControllerDraft();
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
        public WebAdminDeviceBasicConfigUpdateRequest deviceBasic = null;
        public WebAdminDeviceMetadataUpdateRequest deviceMetadata = null;
        public VirtualBlockDeviceDraft virtualBlockDevice = null;
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

    public static final class VirtualBlockDeviceDraft {
        public String protectedDraftId = "";
        public String deviceId = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public Object enabled = Boolean.TRUE;
        public String triggerType = "";
        public boolean itemSubmitEnabled = false;
        public boolean itemSubmitConsumeEnabled = false;
        public String itemSubmitConsumeOrder = "";
        public WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest nativeTriggers = null;
        public List<ItemSubmitRequirementDraft> itemSubmitRequirements = new ArrayList<>();
        public List<ContainerRequirementDraft> containerRequirements = new ArrayList<>();
    }

    public static final class WorldDeviceDraft {
        public String protectedDraftId = "";
        public String deviceType = "";
        public String deviceId = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";
        public Object enabled = Boolean.TRUE;
        public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> actions = new ArrayList<>();
    }

    public static final class RegionControllerDraft {
        public String protectedDraftId = "";
        public String regionId = "";
        public String regionDisplayName = "";
        public String regionNote = "";
        public String controllerId = "";
        public String controllerDisplayName = "";
        public String controllerNote = "";
        public Object enabled = Boolean.TRUE;
        public String targetFilterType = "ALL";
        public String targetFilterValue = "";
        public Object stayIntervalTicks = 100;
        public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> enterActions = new ArrayList<>();
        public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> exitActions = new ArrayList<>();
        public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> stayActions = new ArrayList<>();
    }

    public static final class ItemSubmitRequirementDraft {
        public String requirementId = "";
        public String displayName = "";
        public Object count = 1;
        public Object consumeCount = 1;
        public Boolean consumeCountFollowsCount = Boolean.TRUE;
        public String captureDraftId = "";
        public String templateSummary = "";
        public Boolean requirementEnabled = Boolean.TRUE;
        public String itemId = "";
        public String templateItemId = "";
        public String countMode = "";
        public Object templateCount = 1;
        public Object requiredCount = 1;
        public Boolean matchItemId = Boolean.TRUE;
        public Boolean matchDamage = Boolean.FALSE;
        public Boolean matchCustomName = Boolean.FALSE;
        public Boolean matchLore = Boolean.FALSE;
        public Boolean matchCustomData = Boolean.FALSE;
        public Boolean matchComponents = Boolean.FALSE;
        public Object templateDamage = 0;
        public String templateCustomName = "";
        public List<String> templateLore = new ArrayList<>();
        public String templateCustomData = "";
        public String templateComponents = "";
        public String templateDisplayStack = "";
    }

    public static final class ContainerRequirementDraft {
        public String requirementId = "";
        public String displayName = "";
        public Object slot = 0;
        public Object count = 1;
        public String captureDraftId = "";
        public String templateSummary = "";
        public Boolean enabled = Boolean.TRUE;
        public String type = "";
        public String itemId = "";
        public String countMode = "";
        public String channel = "";
        public String offChannel = "";
        public String mode = "";
        public String matcherTemplateItemId = "";
        public String matcherCountMode = "";
        public Object matcherRequiredCount = 0;
        public Object matcherTemplateCount = 1;
        public Boolean matcherMatchItemId = Boolean.TRUE;
        public Boolean matcherMatchDamage = Boolean.FALSE;
        public Boolean matcherMatchCustomName = Boolean.FALSE;
        public Boolean matcherMatchLore = Boolean.FALSE;
        public Boolean matcherMatchCustomData = Boolean.FALSE;
        public Boolean matcherMatchComponents = Boolean.FALSE;
        public Object matcherTemplateDamage = 0;
        public String matcherTemplateCustomName = "";
        public List<String> matcherTemplateLore = new ArrayList<>();
        public String matcherTemplateCustomData = "";
        public String matcherTemplateComponents = "";
        public String matcherSummary = "";
    }

    public static final class NodeDeleteDraft {
        public String nodeType = "";
        public String targetId = "";
        public String ownerType = "";
        public String ownerId = "";
        public Boolean confirmed = Boolean.FALSE;
        public String confirmationText = "";
        public Boolean impactAccepted = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionDeleteDraft {
        public String ownerType = "";
        public String ownerId = "";
        public String bucket = "";
        public Object actionIndex = 0;
        public Boolean confirmed = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionReorderDraft {
        public String ownerType = "";
        public String ownerId = "";
        public String bucket = "";
        public Object fromIndex = 0;
        public Object toIndex = 0;
        public Boolean confirmed = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }
}
