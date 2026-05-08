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
            Map<String, String> fieldLabels,
            Map<String, Boolean> clearableFields,
            boolean supported,
            String unsupportedReason,
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
            int actionCount,
            List<String> actionSummaries,
            String expectedFingerprint,
            WebAdminEditLockStatusDto lockStatus
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
}
