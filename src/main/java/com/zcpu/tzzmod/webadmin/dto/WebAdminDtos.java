package com.zcpu.tzzmod.webadmin.dto;

import java.util.List;
import java.util.Map;

public final class WebAdminDtos {
    private WebAdminDtos() {
    }

    public record PositionDto(String world, int x, int y, int z) {
    }

    public record DeviceListEntryDto(
            String id,
            String displayName,
            String type,
            String world,
            PositionDto pos,
            boolean enabled,
            String channel,
            String lastTriggeredAt,
            String doctorStatus,
            boolean debugAvailable,
            DeviceMetadataDto metadata
    ) {
    }

    public record DeviceDetailDto(
            String id,
            String displayName,
            String type,
            String world,
            PositionDto pos,
            boolean enabled,
            String channel,
            Map<String, Object> configSummary,
            List<SignalHistoryEntryDto> recentHistory,
            List<DoctorIssueDto> doctorIssues,
            DeviceMetadataDto metadata,
            Map<String, Object> debugSummary,
            Map<String, String> navigation
    ) {
    }

    public record DeviceMetadataDto(
            String deviceId,
            String displayName,
            String note,
            String iconKey,
            String effectiveDisplayName,
            String effectiveIconKey,
            String updatedAt,
            String updatedBy,
            long version
    ) {
    }

    public record DeviceBasicConfigDto(
            String deviceId,
            String deviceType,
            boolean enabled,
            String channel,
            boolean enabledEditable,
            boolean channelEditable,
            boolean supported,
            String unsupportedReason,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus
    ) {
    }

    public record DeviceExtendedConfigDto(
            String deviceId,
            String deviceType,
            Map<String, Object> values,
            List<String> supportedFields,
            List<String> editableFields,
            Map<String, String> fieldLabels,
            Map<String, Boolean> clearableFields,
            Map<String, String> fieldDisabledReasons,
            boolean supported,
            String unsupportedReason,
            String runtimeState,
            boolean worldAvailable,
            boolean chunkLoaded,
            boolean blockEntityLoaded,
            String blockEntityType,
            String blockId,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus
    ) {
    }

    public record DebugCheckDto(String name, String status, String message) {
    }

    public record DeviceDebugDto(
            String deviceId,
            String deviceType,
            boolean enabled,
            String channel,
            String lastTriggeredAt,
            String lastDebugAt,
            List<DebugCheckDto> checks
    ) {
    }

    public record SignalChannelListEntryDto(
            String channel,
            String displayName,
            String note,
            String iconKey,
            String type,
            String lastTriggeredAt,
            int triggerCountToday,
            int sourceCount,
            int listenerCount,
            int receiverCount,
            int actionRelayCount,
            int signalJoinCount,
            int downstreamSignalCount,
            String doctorStatus
    ) {
    }

    public record ChannelMetadataDto(
            String channel,
            String displayName,
            String note,
            String iconKey,
            String effectiveDisplayName,
            String effectiveIconKey,
            String updatedAt,
            String updatedBy,
            long version,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus
    ) {
    }

    public record SignalChannelStatsDto(
            String lastTriggeredAt,
            int triggerCountToday,
            int sourceCount,
            int listenerCount,
            int receiverCount,
            int actionRelayCount,
            int signalJoinCount,
            int downstreamSignalCount
    ) {
    }

    public record SignalChannelEndpointDto(
            String id,
            String name,
            String type,
            String subType,
            String world,
            PositionDto pos,
            boolean enabled,
            String channel,
            int cooldownTicks,
            int actionCount,
            String navigationTarget
    ) {
    }

    public record SignalListenerBasicConfigDto(
            String listenerRef,
            String listenerId,
            String displayName,
            boolean enabled,
            String channel,
            int cooldownTicks,
            String conditionGroupId,
            String conditionGateTargetType,
            String conditionGateTargetId,
            int actionCount,
            List<String> actionSummaries,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus,
            Map<String, Object> recentConditionGate
    ) {
    }

    public record SignalChannelDetailDto(
            String channel,
            ChannelMetadataDto metadata,
            String iconKey,
            String type,
            SignalChannelStatsDto stats,
            List<SignalChannelEndpointDto> sources,
            List<SignalChannelEndpointDto> listeners,
            List<SignalChannelEndpointDto> receivers,
            List<SignalChannelEndpointDto> actionRelays,
            List<SignalChannelEndpointDto> signalJoins,
            List<ActionListEntryDto> actions,
            List<String> downstreamSignals,
            List<SignalHistoryEntryDto> recentHistory,
            List<DoctorIssueDto> doctorIssues
    ) {
    }

    public record SignalHistoryEntryDto(
            String id,
            String time,
            String channel,
            String sourceType,
            String sourceId,
            String sourceName,
            String world,
            PositionDto pos,
            String playerName,
            String result,
            String description
    ) {
    }

    public record DoctorSummaryDto(
            int errorCount,
            int warningCount,
            int infoCount,
            int affectedDeviceCount,
            int affectedChannelCount
    ) {
    }

    public record DoctorIssueDto(
            String id,
            String severity,
            String title,
            String message,
            String relatedObjectType,
            String relatedObjectId,
            String relatedObjectName,
            String channel,
            String impact,
            String suggestion,
            String detectedAt,
            String navigationTarget
    ) {
    }

    public record DoctorReportDto(DoctorSummaryDto summary, List<DoctorIssueDto> issues) {
    }

    public record RegionBoundsDto(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public record RegionActionSummaryDto(String id, String type, String summary, boolean enabled) {
    }

    public record RegionListEntryDto(
            String id,
            String name,
            String world,
            RegionBoundsDto bounds,
            String targetFilter,
            int enterActionCount,
            int exitActionCount,
            int stayActionCount,
            String boundChannel,
            int playersInside,
            String lastEventAt,
            boolean enabled,
            String doctorStatus,
            String type,
            String description,
            String controllerId,
            int controllerCount
    ) {
    }

    public record RegionDetailDto(
            String id,
            String name,
            String world,
            RegionBoundsDto bounds,
            String targetFilter,
            Map<String, List<RegionActionSummaryDto>> actions,
            List<String> boundChannels,
            List<String> playersInside,
            List<Object> recentEvents,
            List<DoctorIssueDto> doctorIssues
    ) {
    }

    public record ActionOwnerDto(String ownerType, String ownerId, String ownerName, String channel) {
    }

    public record ActionListEntryDto(
            String id,
            String name,
            String type,
            String summary,
            String ownerType,
            String ownerId,
            String ownerName,
            String channel,
            int referencedByCount,
            int executionCount,
            String lastResult,
            String lastExecutedAt,
            String doctorStatus
    ) {
    }

    public record ActionDetailDto(
            String id,
            String type,
            String summary,
            ActionOwnerDto owner,
            Map<String, Object> configSummary,
            List<Object> recentExecutions,
            List<DoctorIssueDto> doctorIssues,
            String navigationTarget
    ) {
    }

    public record StateVariableSummaryDto(
            int totalCount,
            int globalCount,
            int playerCount,
            int booleanCount,
            int integerCount,
            int stringCount
    ) {
    }

    public record StateVariableListEntryDto(
            String id,
            String scope,
            String scopeLabel,
            String targetId,
            String targetLabel,
            String key,
            String type,
            String typeLabel,
            Object value,
            String valueText,
            String valuePreview,
            int valueLength,
            long version,
            String fingerprint,
            String fingerprintShort,
            String updatedAt,
            String updatedBy,
            String displayPath,
            String navigationTarget
    ) {
    }

    public record StateVariableListDto(
            List<StateVariableListEntryDto> variables,
            int count,
            StateVariableSummaryDto summary,
            boolean worldScoped,
            String storeFile,
            boolean storePresent,
            boolean storeDegraded,
            String storeMessage,
            boolean readOnly,
            List<String> allowedScopes,
            List<String> allowedTypes
    ) {
    }

    public record StateVariableDetailDto(
            String id,
            String scope,
            String scopeLabel,
            String targetId,
            String targetLabel,
            String key,
            String type,
            String typeLabel,
            Object value,
            String valueText,
            String valuePreview,
            int valueLength,
            long version,
            String fingerprint,
            String fingerprintShort,
            String updatedAt,
            String updatedBy,
            String createdAt,
            String displayPath,
            String storagePathSummary,
            boolean readOnly,
            Map<String, String> copyTargets,
            Map<String, Object> conditionSuggestion
    ) {
    }

    public record WebAdminUserSummaryDto(
            int totalCount,
            int onlineCount,
            int ownerCount,
            int editorCount,
            int testerCount,
            int viewerCount,
            int disabledCount
    ) {
    }

    public record WebAdminRoleSummaryDto(String role, String displayName, int count) {
    }

    public record WebAdminUserListEntryDto(
            String username,
            String displayName,
            String role,
            String roleDisplayName,
            boolean enabled,
            boolean online,
            int sessionCount,
            String createdAt,
            String createdBy,
            String lastLoginAt,
            boolean forcePasswordChange
    ) {
    }

    public record WebAdminUsersDto(
            WebAdminUserSummaryDto summary,
            List<WebAdminUserListEntryDto> users,
            List<WebAdminRoleSummaryDto> roles
    ) {
    }

    public record WebAdminSettingsDto(
            Map<String, Object> service,
            Map<String, Object> storage,
            Map<String, Object> security,
            Map<String, Object> audit,
            Map<String, Object> system,
            Map<String, Object> visibility
    ) {
    }

    public record LogicChainMetadataDto(
            String id,
            String displayName,
            String note,
            String iconKey,
            String effectiveDisplayName,
            String effectiveIconKey,
            List<String> tags,
            String group,
            String rootType,
            String rootRef,
            String rootChannel,
            boolean includeDisabled,
            int maxDepth,
            String layoutPreference,
            String updatedAt,
            String updatedBy,
            long version,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus
    ) {
    }

    public record LogicChainSummaryDto(
            String id,
            String componentId,
            String displayName,
            String rootType,
            String rootRef,
            String rootChannel,
            String defaultFocusChannel,
            List<String> includedChannels,
            int channelCount,
            int producerCount,
            int consumerCount,
            int listenerCount,
            int actionCount,
            int downstreamChannelCount,
            int signalJoinCount,
            int timerCount,
            int disabledNodeCount,
            String doctorStatus,
            String lastTriggeredAt,
            String source,
            boolean saved,
            int hierarchyLevel,
            String parentChainId,
            String parentRootRef,
            String upstreamSourceLabel,
            String upstreamNodeId,
            boolean isSubChain,
            boolean isReference,
            boolean hasMultipleParents,
            boolean hasCycle,
            boolean selfCycle,
            int childrenCount,
            boolean visibleInTopLevel,
            LogicChainMetadataDto metadata
    ) {
    }

    public record LogicChainGraphDto(
            LogicChainMetadataDto metadata,
            LogicChainNodeDto root,
            List<LogicChainSegmentDto> segments,
            List<LogicChainNodeDto> nodes,
            List<LogicChainEdgeDto> edges,
            List<String> warnings,
            Map<String, Object> stats
    ) {
    }

    public record LogicChainSegmentDto(
            String id,
            String channel,
            int depth,
            boolean expanded,
            String visitedState,
            List<String> producers,
            List<String> consumers,
            List<String> actions,
            List<String> downstreamChannels,
            List<String> warnings
    ) {
    }

    public record LogicChainNodeDto(
            String id,
            String type,
            String refType,
            String refId,
            String label,
            String subtitle,
            String channel,
            boolean enabled,
            String status,
            String doctorStatus,
            String lastEvent,
            String detailRoute,
            Map<String, Object> metadata
    ) {
    }

    public record LogicChainEdgeDto(
            String from,
            String to,
            String type,
            String label,
            String style,
            String pathGroupId,
            String visualStyle,
            boolean referenceEdge,
            Map<String, Object> metadata
    ) {
    }
}
