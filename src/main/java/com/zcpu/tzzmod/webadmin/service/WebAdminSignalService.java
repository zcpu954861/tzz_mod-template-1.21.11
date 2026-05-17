package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalChannelInspector;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminSignalService {
    public List<WebAdminDtos.SignalChannelListEntryDto> listChannels(MinecraftServer server, int requestedLimit) {
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(server);
        List<RegionControllerData> regions = RegionControllerStore.getSnapshot(server);
        List<SignalJoinDefinition> joins = SignalJoinStore.getSnapshot(server);
        WebAdminChannelMetadataStore.MetadataFile metadataFile = WebAdminChannelMetadataStore.load(server);
        LinkedHashSet<String> channels = knownChannels(server, devices, listeners, regions, joins);
        int limit = WebAdminReadonlySupport.limit(requestedLimit, WebAdminReadonlySupport.MAX_LIST_LIMIT);
        List<WebAdminDtos.SignalChannelListEntryDto> result = new ArrayList<>();
        for (String channel : channels) {
            if (result.size() >= limit) {
                break;
            }
            ChannelCounts counts = counts(server, channel, devices, listeners, regions, joins);
            SignalEventRecord latest = latest(channel);
            String fallbackIcon = counts.doctorStatus().equals("OK") ? "signal" : "warning";
            WebAdminChannelMetadataStore.MetadataEntry metadata = metadataFile.channels.get(channel);
            WebAdminDtos.ChannelMetadataDto metadataDto = WebAdminChannelMetadataService.dto(
                    metadata == null ? emptyMetadata(channel) : metadata,
                    fallbackIcon,
                    null
            );
            result.add(new WebAdminDtos.SignalChannelListEntryDto(
                    channel,
                    metadataDto.effectiveDisplayName(),
                    metadataDto.note(),
                    metadataDto.effectiveIconKey(),
                    channelType(channel, counts),
                    latest == null ? "" : WebAdminReadonlySupport.isoTime(latest.wallTimeMillis()),
                    historyForChannel(channel).size(),
                    counts.sourceCount(),
                    counts.listenerCount(),
                    counts.receiverCount(),
                    counts.actionRelayCount(),
                    counts.signalJoinCount(),
                    counts.downstreamSignalCount(),
                    counts.doctorStatus()
            ));
        }
        return List.copyOf(result);
    }

    public WebAdminDtos.SignalChannelDetailDto channelDetail(MinecraftServer server, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(server);
        List<RegionControllerData> regions = RegionControllerStore.getSnapshot(server);
        List<SignalJoinDefinition> joins = SignalJoinStore.getSnapshot(server);
        WebAdminChannelMetadataStore.MetadataFile metadataFile = WebAdminChannelMetadataStore.load(server);
        ChannelCounts counts = counts(server, channel, devices, listeners, regions, joins);
        SignalEventRecord latest = latest(channel);
        WebAdminDtos.SignalChannelStatsDto stats = new WebAdminDtos.SignalChannelStatsDto(
                latest == null ? "" : WebAdminReadonlySupport.isoTime(latest.wallTimeMillis()),
                historyForChannel(channel).size(),
                counts.sourceCount(),
                counts.listenerCount(),
                counts.receiverCount(),
                counts.actionRelayCount(),
                counts.signalJoinCount(),
                counts.downstreamSignalCount()
        );
        List<WebAdminDtos.SignalChannelEndpointDto> sources = sourceEndpoints(channel, devices, joins);
        List<WebAdminDtos.SignalChannelEndpointDto> listenerEndpoints = listenerEndpoints(channel, listeners);
        List<WebAdminDtos.SignalChannelEndpointDto> receivers = deviceEndpoints(channel, devices, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        List<WebAdminDtos.SignalChannelEndpointDto> relays = deviceEndpoints(channel, devices, SignalDeviceData.TYPE_ACTION_RELAY);
        List<WebAdminDtos.SignalChannelEndpointDto> joinEndpoints = joinInputEndpoints(channel, joins);
        List<WebAdminDtos.ActionListEntryDto> actions = actionsForChannel(server, channel, devices, listeners, regions);
        List<String> downstream = downstreamSignals(actions, channel, joins);
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        if (counts.listenerCount() == 0 && counts.receiverCount() == 0 && counts.actionRelayCount() == 0 && counts.signalJoinCount() == 0) {
            issues.add(new WebAdminDtos.DoctorIssueDto(
                    "channel_no_consumers",
                    "WARNING",
                    "频道暂无消费者",
                    "该信号仍会发出并写入历史，但当前不会触发监听器、接收器或动作继电器。",
                    "CHANNEL",
                    channel,
                    channel,
                    channel,
            "没有已配置的消费者。",
                    "如该频道需要产生下游效果，请添加监听器 listener、接收器 signal_receiver、动作继电器 action_relay 或 Signal Join。",
                    java.time.Instant.now().toString(),
                    "channel:" + channel
            ));
        }
        String fallbackIcon = counts.doctorStatus().equals("OK") ? "signal" : "warning";
        WebAdminDtos.ChannelMetadataDto metadata = WebAdminChannelMetadataService.dto(
                metadataFile.channels.get(channel) == null ? emptyMetadata(channel) : metadataFile.channels.get(channel),
                fallbackIcon,
                null
        );
        return new WebAdminDtos.SignalChannelDetailDto(
                channel,
                metadata,
                metadata.effectiveIconKey(),
                channelType(channel, counts),
                stats,
                sources,
                listenerEndpoints,
                receivers,
                relays,
                joinEndpoints,
                actions,
                downstream,
                WebAdminReadonlySupport.historyDtos(historyForChannel(channel), devices, 50),
                List.copyOf(issues)
        );
    }

    public boolean channelExists(MinecraftServer server, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            return false;
        }
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(server);
        List<RegionControllerData> regions = RegionControllerStore.getSnapshot(server);
        List<SignalJoinDefinition> joins = SignalJoinStore.getSnapshot(server);
        return knownChannels(server, devices, listeners, regions, joins).contains(channel);
    }

    public List<WebAdminDtos.SignalHistoryEntryDto> history(MinecraftServer server, String channel, int requestedLimit) {
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(server);
        List<SignalEventRecord> records = channel == null || channel.isBlank()
                ? SignalEventHistory.snapshot()
                : SignalEventHistory.snapshot(SignalChannel.normalize(channel));
        return WebAdminReadonlySupport.historyDtos(records, devices, requestedLimit);
    }

    private LinkedHashSet<String> knownChannels(
            MinecraftServer server,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins
    ) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (SignalDeviceData device : devices) {
            addDeviceChannels(channels, device.normalized());
            addActionRelayActionChannels(server, channels, device.normalized());
        }
        for (SignalListenerData listener : listeners) {
            add(channels, listener.channel());
            for (ActionConfig action : listener.actions()) {
                addActionChannel(channels, action);
            }
        }
        for (RegionControllerData region : regions) {
            addActionChannels(channels, region.enterActions());
            addActionChannels(channels, region.exitActions());
            addActionChannels(channels, region.stayActions());
        }
        for (SignalJoinDefinition join : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition normalized = join.normalized();
            add(channels, normalized.outputChannel);
            for (String input : normalized.inputChannelNames()) {
                add(channels, input);
            }
        }
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            add(channels, record.channel());
        }
        return channels;
    }

    private void addDeviceChannels(Set<String> channels, SignalDeviceData device) {
        add(channels, device.channel());
        add(channels, device.offChannel());
        add(channels, device.interactChannel());
        add(channels, device.containerOpenChannel());
        add(channels, device.containerCloseChannel());
        add(channels, device.containerChangeChannel());
        for (ContainerItemConditionData condition : device.itemConditions()) {
            add(channels, condition.channel());
            add(channels, condition.offChannel());
        }
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        add(channels, matcher.successChannel());
        add(channels, matcher.failChannel());
    }

    private ChannelCounts counts(
            MinecraftServer server,
            String channel,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins
    ) {
        int sources = sourceEndpoints(channel, devices, joins).size();
        int listenerCount = 0;
        int receiverCount = 0;
        int relayCount = 0;
        int joinCount = 0;
        int downstreamCount = 0;
        for (SignalListenerData listener : listeners) {
            if (SignalChannel.normalize(listener.channel()).equals(channel)) {
                listenerCount++;
            }
            for (ActionConfig action : listener.actions()) {
                if (isSignalActionTo(action, channel)) {
                    downstreamCount++;
                }
            }
        }
        for (SignalDeviceData device : devices) {
            if (SignalChannel.normalize(device.channel()).equals(channel)) {
                if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type())) {
                    receiverCount++;
                } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                    relayCount++;
                }
            }
            ActionRelayBlockEntity relay = loadedActionRelay(server, device);
            if (relay != null) {
                downstreamCount += countSignalActionsTo(relay.actions(), channel);
            }
        }
        for (RegionControllerData region : regions) {
            downstreamCount += countSignalActionsTo(region.enterActions(), channel);
            downstreamCount += countSignalActionsTo(region.exitActions(), channel);
            downstreamCount += countSignalActionsTo(region.stayActions(), channel);
        }
        for (SignalJoinDefinition raw : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition join = raw.normalized();
            if (join.inputChannelNames().contains(channel)) {
                joinCount++;
                if (!join.outputChannel.isBlank()) {
                    downstreamCount++;
                }
            }
        }
        String status = listenerCount + receiverCount + relayCount + joinCount == 0 ? "WARNING" : "OK";
        return new ChannelCounts(sources, listenerCount, receiverCount, relayCount, joinCount, downstreamCount, status);
    }

    private List<WebAdminDtos.SignalChannelEndpointDto> sourceEndpoints(String channel, List<SignalDeviceData> devices, List<SignalJoinDefinition> joins) {
        List<WebAdminDtos.SignalChannelEndpointDto> endpoints = new ArrayList<>();
        for (SignalDeviceData raw : devices) {
            SignalDeviceData device = raw.normalized();
            if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type()) || SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                continue;
            }
            if (deviceReferencesChannel(device, channel)) {
                endpoints.add(deviceEndpoint(device, "DEVICE", WebAdminReadonlySupport.deviceType(device)));
            }
        }
        for (SignalJoinDefinition raw : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition join = raw.normalized();
            if (SignalChannel.normalize(join.outputChannel).equals(channel)) {
                endpoints.add(joinEndpoint(join, "SIGNAL_JOIN", "signal_join", join.outputChannel));
            }
        }
        return List.copyOf(endpoints);
    }

    private List<WebAdminDtos.SignalChannelEndpointDto> listenerEndpoints(String channel, List<SignalListenerData> listeners) {
        List<WebAdminDtos.SignalChannelEndpointDto> endpoints = new ArrayList<>();
        for (SignalListenerData listener : listeners) {
            if (SignalChannel.normalize(listener.channel()).equals(channel)) {
                endpoints.add(new WebAdminDtos.SignalChannelEndpointDto(
                        listener.id(),
                        listener.name().isBlank() ? "Unnamed listener" : listener.name(),
                        "LISTENER",
                        "signal_listener",
                        "",
                        null,
                        listener.enabled(),
                        listener.channel(),
                        listener.cooldownTicks(),
                        listener.actions().size(),
                        "listener:" + listener.id()
                ));
            }
        }
        return List.copyOf(endpoints);
    }

    private List<WebAdminDtos.SignalChannelEndpointDto> deviceEndpoints(
            String channel,
            List<SignalDeviceData> devices,
            String type
    ) {
        List<WebAdminDtos.SignalChannelEndpointDto> endpoints = new ArrayList<>();
        for (SignalDeviceData raw : devices) {
            SignalDeviceData device = raw.normalized();
            if (type.equals(device.type()) && SignalChannel.normalize(device.channel()).equals(channel)) {
                endpoints.add(deviceEndpoint(device, type.equals(SignalDeviceData.TYPE_SIGNAL_RECEIVER) ? "RECEIVER" : "ACTION_RELAY", WebAdminReadonlySupport.deviceType(device)));
            }
        }
        return List.copyOf(endpoints);
    }

    private List<WebAdminDtos.SignalChannelEndpointDto> joinInputEndpoints(String channel, List<SignalJoinDefinition> joins) {
        List<WebAdminDtos.SignalChannelEndpointDto> endpoints = new ArrayList<>();
        for (SignalJoinDefinition raw : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition join = raw.normalized();
            if (join.inputChannelNames().contains(channel)) {
                endpoints.add(joinEndpoint(join, "SIGNAL_JOIN", join.mode.name(), channel));
            }
        }
        return List.copyOf(endpoints);
    }

    private WebAdminDtos.SignalChannelEndpointDto joinEndpoint(SignalJoinDefinition join, String type, String subType, String channel) {
        return new WebAdminDtos.SignalChannelEndpointDto(
                join.id,
                join.displayName.isBlank() ? join.id : join.displayName,
                type,
                subType,
                "",
                null,
                join.enabled,
                channel,
                0,
                join.inputChannelNames().size(),
                "signal_join:" + join.id
        );
    }

    private WebAdminDtos.SignalChannelEndpointDto deviceEndpoint(SignalDeviceData device, String type, String subType) {
        return new WebAdminDtos.SignalChannelEndpointDto(
                device.id(),
                WebAdminReadonlySupport.deviceDisplayName(device),
                type,
                subType,
                device.dimension(),
                WebAdminReadonlySupport.pos(device),
                device.enabled(),
                device.channel(),
                0,
                0,
                "device:" + device.id()
        );
    }

    private static WebAdminChannelMetadataStore.MetadataEntry emptyMetadata(String channel) {
        return WebAdminChannelMetadataStore.MetadataEntry.normalized(channel, null);
    }

    private List<WebAdminDtos.ActionListEntryDto> actionsForChannel(
            MinecraftServer server,
            String channel,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions
    ) {
        List<WebAdminDtos.ActionListEntryDto> actions = new ArrayList<>();
        for (SignalListenerData listener : listeners) {
            if (!SignalChannel.normalize(listener.channel()).equals(channel)) {
                continue;
            }
            addActions(actions, listener.actions(), "LISTENER", listener.id(), listener.name(), channel);
        }
        for (RegionControllerData region : regions) {
            addActions(actions, region.enterActions(), "REGION_ENTER", region.id(), region.name(), channel);
            addActions(actions, region.exitActions(), "REGION_EXIT", region.id(), region.name(), channel);
            addActions(actions, region.stayActions(), "REGION_STAY", region.id(), region.name(), channel);
        }
        for (SignalDeviceData device : devices) {
            ActionRelayBlockEntity relay = loadedActionRelay(server, device);
            if (relay != null && SignalChannel.normalize(device.channel()).equals(channel)) {
                addActions(actions, relay.actions(), "ACTION_RELAY", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), channel);
            }
        }
        return List.copyOf(actions);
    }

    private void addActions(
            List<WebAdminDtos.ActionListEntryDto> output,
            List<ActionConfig> actions,
            String ownerType,
            String ownerId,
            String ownerName,
            String ownerChannel
    ) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            String id = ownerType + ":" + ownerId + ":" + i;
            output.add(new WebAdminDtos.ActionListEntryDto(
                    id,
                    ownerType + " #" + (i + 1),
                    WebAdminReadonlySupport.actionType(action),
                    WebAdminReadonlySupport.actionSummary(action),
                    ownerType,
                    ownerId,
                    ownerName == null || ownerName.isBlank() ? ownerId : ownerName,
                    ownerChannel,
                    1,
                    0,
                    "UNKNOWN",
                    "",
                    action != null && action.isUsable() ? "OK" : "WARNING"
            ));
        }
    }

    private List<String> downstreamSignals(List<WebAdminDtos.ActionListEntryDto> actions, String sourceChannel, List<SignalJoinDefinition> joins) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (WebAdminDtos.ActionListEntryDto action : actions) {
            if ("SIGNAL".equals(action.type())) {
                String summary = action.summary();
                int index = summary.indexOf(':');
                if (index >= 0) {
                    add(channels, summary.substring(index + 1).trim());
                }
            }
        }
        for (SignalJoinDefinition raw : joins == null ? List.<SignalJoinDefinition>of() : joins) {
            SignalJoinDefinition join = raw.normalized();
            if (join.inputChannelNames().contains(sourceChannel)) {
                add(channels, join.outputChannel);
            }
        }
        return List.copyOf(channels);
    }

    private boolean deviceReferencesChannel(SignalDeviceData device, String channel) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        addDeviceChannels(channels, device);
        return channels.contains(channel);
    }

    private List<SignalEventRecord> historyForChannel(String channel) {
        return SignalEventHistory.snapshot(SignalChannel.normalize(channel));
    }

    private SignalEventRecord latest(String channel) {
        List<SignalEventRecord> records = historyForChannel(channel);
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    private String channelType(String channel, ChannelCounts counts) {
        if (counts.sourceCount() > 0) {
            return "DEVICE";
        }
        if (channel.startsWith("region.") || channel.contains(".region.")) {
            return "REGION";
        }
        if (channel.startsWith("game.") || channel.startsWith("system.")) {
            return "SYSTEM";
        }
        return "UNKNOWN";
    }

    private void addActionChannels(Set<String> channels, List<ActionConfig> actions) {
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            addActionChannel(channels, action);
        }
    }

    private void addActionChannel(Set<String> channels, ActionConfig action) {
        if (action != null && action.type() == ActionType.SIGNAL) {
            add(channels, action.value());
        }
    }

    private void addActionRelayActionChannels(MinecraftServer server, Set<String> channels, SignalDeviceData device) {
        ActionRelayBlockEntity relay = loadedActionRelay(server, device);
        if (relay != null) {
            addActionChannels(channels, relay.actions());
        }
    }

    private ActionRelayBlockEntity loadedActionRelay(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null || !SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
            return null;
        }
        return SignalDeviceStore.getLoadedActionRelay(server, device);
    }

    private int countSignalActionsTo(List<ActionConfig> actions, String channel) {
        int count = 0;
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (isSignalActionTo(action, channel)) {
                count++;
            }
        }
        return count;
    }

    private boolean isSignalActionTo(ActionConfig action, String channel) {
        return action != null
                && action.type() == ActionType.SIGNAL
                && SignalChannel.normalize(action.value()).equals(channel);
    }

    private void add(Set<String> channels, String channel) {
        String normalized = SignalChannel.normalize(channel);
        if (!normalized.isBlank()) {
            channels.add(normalized);
        }
    }

    private record ChannelCounts(
            int sourceCount,
            int listenerCount,
            int receiverCount,
            int actionRelayCount,
            int signalJoinCount,
            int downstreamSignalCount,
            String doctorStatus
    ) {
    }
}
