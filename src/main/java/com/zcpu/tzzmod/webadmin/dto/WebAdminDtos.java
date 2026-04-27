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
            boolean debugAvailable
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
            Map<String, Object> debugSummary,
            Map<String, String> navigation
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
            String navigationTarget
    ) {
    }

    public record SignalChannelDetailDto(
            String channel,
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
            String doctorStatus
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
}
