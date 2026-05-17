package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinRuntimeService;
import com.zcpu.tzzmod.signal.join.SignalJoinStatusSnapshot;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminLogicChainMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainMetadataRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteAuditContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminLogicChainService {
    public static final int DEFAULT_MAX_DEPTH = 3;
    public static final int HARD_MAX_DEPTH = 8;
    private static final int AUTO_CHAIN_LIMIT = 120;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminLogicChainService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public List<WebAdminDtos.LogicChainSummaryDto> listChains(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            int requestedLimit
    ) {
        Snapshot snapshot = snapshot(server);
        WebAdminLogicChainMetadataStore.MetadataFile file = WebAdminLogicChainMetadataStore.load(server);
        List<WebAdminDtos.LogicChainSummaryDto> result = new ArrayList<>();
        Set<String> seenChannels = new LinkedHashSet<>();
        ChannelHierarchy hierarchy = channelHierarchy(snapshot);
        int limit = WebAdminReadonlySupport.limit(requestedLimit, AUTO_CHAIN_LIMIT);
        for (WebAdminLogicChainMetadataStore.MetadataEntry entry : file.chains.values()) {
            String rootChannel = resolveRootChannel(snapshot, entry.rootType, entry.rootRef);
            WebAdminDtos.LogicChainMetadataDto metadata = metadataDto(entry, rootChannel, user, session);
            GraphStats stats = summarize(snapshot, rootChannel, entry.includeDisabled, entry.maxDepth);
            result.add(summary(entry.id, metadata.effectiveDisplayName(), entry.rootType, entry.rootRef, rootChannel, true, metadata, stats, hierarchy));
            if (!rootChannel.isBlank()) {
                seenChannels.add(rootChannel);
            }
        }
        for (String channel : knownChannels(snapshot)) {
            if (result.size() >= limit) {
                break;
            }
            if (seenChannels.contains(channel)) {
                continue;
            }
            GraphStats stats = summarize(snapshot, channel, true, DEFAULT_MAX_DEPTH);
            String id = autoChainId(channel);
            WebAdminLogicChainMetadataStore.MetadataEntry entry = defaultEntry(id, "channel", channel);
            WebAdminDtos.LogicChainMetadataDto metadata = metadataDto(entry, channel, user, session);
            result.add(summary(id, metadata.effectiveDisplayName(), "channel", channel, channel, false, metadata, stats, hierarchy));
        }
        return List.copyOf(result);
    }

    public WebAdminDtos.LogicChainGraphDto graphForChain(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String chainId
    ) {
        String safeId = normalizeChainId(chainId);
        WebAdminLogicChainMetadataStore.MetadataFile file = WebAdminLogicChainMetadataStore.load(server);
        WebAdminLogicChainMetadataStore.MetadataEntry entry = file.chains.get(safeId);
        if (entry == null && safeId.startsWith("auto:channel:")) {
            String channel = safeId.substring("auto:channel:".length());
            entry = defaultEntry(safeId, "channel", channel);
        }
        if (entry == null) {
            entry = defaultEntry(safeId, "channel", "");
        }
        return graphForRoot(server, user, session, entry.rootType, entry.rootRef, entry.includeDisabled, entry.maxDepth, entry);
    }

    public WebAdminDtos.LogicChainGraphDto graphForRoot(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String rootType,
            String rootRef,
            boolean includeDisabled,
            int maxDepth,
            WebAdminLogicChainMetadataStore.MetadataEntry metadata
    ) {
        Snapshot snapshot = snapshot(server);
        int safeDepth = Math.max(1, Math.min(HARD_MAX_DEPTH, maxDepth <= 0 ? DEFAULT_MAX_DEPTH : maxDepth));
        String type = normalizeRootType(rootType);
        String ref = safe(rootRef).trim();
        String rootChannel = resolveRootChannel(snapshot, type, ref);
        WebAdminLogicChainMetadataStore.MetadataEntry entry = metadata == null
                ? defaultEntry(autoRootChainId(type, ref, rootChannel), type, ref)
                : WebAdminLogicChainMetadataStore.MetadataEntry.normalized(metadata.id, metadata);
        if (metadata == null) {
            entry.includeDisabled = includeDisabled;
            entry.maxDepth = safeDepth;
        }
        WebAdminDtos.LogicChainMetadataDto metadataDto = metadataDto(entry, rootChannel, user, session);
        GraphBuild build = new GraphBuild(snapshot, includeDisabled, safeDepth);
        if (rootChannel.isBlank()) {
            build.warnings.add("当前 root 无法解析到 Signal 频道，图谱为空。");
        } else {
            buildSegment(build, rootChannel, 0, new LinkedHashSet<>());
        }
        build.warnings.addAll(rootWarnings(snapshot, type, ref, rootChannel));
        WebAdminDtos.LogicChainNodeDto root = build.nodes.getOrDefault("channel:" + rootChannel, new WebAdminDtos.LogicChainNodeDto(
                "root:" + safeNodeId(ref),
                "root",
                type,
                ref,
                ref.isBlank() ? "未解析 root" : ref,
                rootChannel.isBlank() ? "没有可解析频道" : rootChannel,
                rootChannel,
                true,
                rootChannel.isBlank() ? "WARNING" : "OK",
                rootChannel.isBlank() ? "WARNING" : doctorStatus(build, rootChannel),
                latestTime(rootChannel),
                rootChannel.isBlank() ? "" : "#/signals/" + encode(rootChannel),
                Map.of("readOnly", true)
        ));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("channelCount", build.channelCount());
        stats.put("producerCount", build.countByType("producer"));
        stats.put("consumerCount", build.countByType("consumer"));
        stats.put("actionCount", build.countByType("action"));
        stats.put("downstreamChannelCount", build.countEdges("emits_downstream"));
        stats.put("disabledNodeCount", build.disabledCount());
        stats.put("maxDepth", safeDepth);
        stats.put("readOnly", true);
        stats.put("segmentModel", "cross-channel");
        return new WebAdminDtos.LogicChainGraphDto(
                metadataDto,
                root,
                List.copyOf(build.segments),
                List.copyOf(build.nodes.values()),
                List.copyOf(build.edges),
                List.copyOf(build.warnings),
                stats
        );
    }

    public WebAdminWriteResult upsertMetadata(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainMetadataRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainMetadataRequest safeRequest = request == null ? new WebAdminLogicChainMetadataRequest() : request;
        String id = safeRequest.chainId.isBlank() ? generatedId(safeRequest) : normalizeChainId(safeRequest.chainId);
        WebAdminWriteTarget target = target(id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }

        Snapshot currentSnapshot = snapshot(server);
        List<WebAdminValidationError> errors = validateMetadataRequest(currentSnapshot, safeRequest, id);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(safeRequest, id));
            return result;
        }
        WebAdminLogicChainMetadataStore.MetadataFile file = WebAdminLogicChainMetadataStore.load(server);
        WebAdminLogicChainMetadataStore.MetadataEntry before = WebAdminLogicChainMetadataStore.MetadataEntry.normalized(id, file.chains.get(id));
        boolean exists = file.chains.containsKey(id);
        if (exists && safeRequest.expectedFingerprint.isBlank()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存已有逻辑链元数据需要 expectedFingerprint。",
                    ""
            )));
            audit(context, result, beforeSummary(before), requestSummary(safeRequest, id));
            return result;
        }
        if (exists && !fingerprintFor(before).equals(safeRequest.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, before, safeRequest.expectedFingerprint);
            audit(context, result, beforeSummary(before), requestSummary(safeRequest, id));
            return result;
        }

        WebAdminLogicChainMetadataStore.MetadataEntry after = normalizedEntry(safeRequest, id, before.version + (exists ? 1L : 0L), user);
        if (exists && metadataEquals(before, after)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的逻辑链显示信息变化。");
            audit(context, result, beforeSummary(before), beforeSummary(before));
            releaseLockAfterWrite(safeRequest, user, session, remoteAddress, id);
            return result;
        }
        file.chains.put(id, after);
        if (!WebAdminLogicChainMetadataStore.save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "逻辑链显示信息保存失败，请查看服务端日志。");
            audit(context, result, beforeSummary(before), beforeSummary(after));
            return result;
        }

        WebAdminDtos.LogicChainMetadataDto dto = metadataDto(after, resolveRootChannel(currentSnapshot, after.rootType, after.rootRef), user, session);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metadata", dto);
        data.put("routeTarget", "#/logic-chains/" + encode(after.id));
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                exists ? "逻辑链显示信息已保存。" : "逻辑链已保存到 WebAdmin 视图。",
                target.targetType(),
                target.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data
        );
        WebAdminAuditEvent auditEvent = audit(context, result, beforeSummary(before), beforeSummary(after));
        publishRealtime(after, auditEvent, user);
        releaseLockAfterWrite(safeRequest, user, session, remoteAddress, id);
        return result;
    }

    public WebAdminWriteResult deleteMetadata(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String chainId,
            WebAdminLogicChainMetadataRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String id = normalizeChainId(chainId);
        WebAdminWriteTarget target = target(id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA, target);
        WebAdminLogicChainMetadataRequest safeRequest = request == null ? new WebAdminLogicChainMetadataRequest() : request;
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminLogicChainMetadataStore.MetadataFile file = WebAdminLogicChainMetadataStore.load(server);
        WebAdminLogicChainMetadataStore.MetadataEntry before = file.chains.get(id);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "逻辑链元数据不存在或已删除。");
            audit(context, result, Map.of(), Map.of("deleted", false));
            releaseLockAfterWrite(safeRequest, user, session, remoteAddress, id);
            return result;
        }
        if (safeRequest.expectedFingerprint.isBlank() || !fingerprintFor(before).equals(safeRequest.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, before, safeRequest.expectedFingerprint);
            audit(context, result, beforeSummary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        file.chains.remove(id);
        if (!WebAdminLogicChainMetadataStore.save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "逻辑链元数据删除失败，请查看服务端日志。");
            audit(context, result, beforeSummary(before), Map.of("deleted", false));
            return result;
        }
        WebAdminWriteResult result = WebAdminWriteResult.ok(target, true, "逻辑链元数据已删除。");
        WebAdminAuditEvent auditEvent = audit(context, result, beforeSummary(before), Map.of("deleted", true));
        publishRealtime(before, auditEvent, user);
        releaseLockAfterWrite(safeRequest, user, session, remoteAddress, id);
        return result;
    }

    private void buildSegment(GraphBuild build, String rawChannel, int depth, LinkedHashSet<String> visiting) {
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            return;
        }
        if (depth > build.maxDepth) {
            build.warnings.add("频道 " + channel + " 超出最大展开深度，已作为下游引用保留。");
            return;
        }
        String segmentId = "segment:" + safeNodeId(channel);
        if (visiting.contains(channel)) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(segmentId + ":cycle", channel, depth, false, "cycle", List.of(), List.of(), List.of(), List.of(), List.of("检测到循环引用，已停止继续展开。")));
            build.warnings.add("频道 " + channel + " 检测到循环引用。");
            return;
        }
        if (build.visitedChannels.contains(channel)) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(segmentId + ":reference", channel, depth, false, "already_expanded", List.of(), List.of(), List.of(), List.of(), List.of("该频道已在其它段展开。")));
            return;
        }
        build.visitedChannels.add(channel);
        visiting.add(channel);

        String channelNode = addChannelNode(build, channel);
        List<String> producers = producersFor(build, channel);
        List<String> consumers = consumersFor(build, channel);
        List<String> actions = actionsFor(build, channel, consumers);
        LinkedHashSet<String> downstream = new LinkedHashSet<>();
        for (String actionNode : actions) {
            Object raw = build.nodes.get(actionNode).metadata().get("downstreamChannel");
            String downstreamChannel = SignalChannel.normalize(raw instanceof String text ? text : "");
            if (!downstreamChannel.isBlank()) {
                String downstreamNode = addDownstreamChannelNode(build, downstreamChannel, depth + 1);
                downstream.add(downstreamNode);
                build.edges.add(new WebAdminDtos.LogicChainEdgeDto(actionNode, downstreamNode, "emits_downstream", "下游频道", "dashed"));
            }
        }
        for (String consumerNode : consumers) {
            WebAdminDtos.LogicChainNodeDto node = build.nodes.get(consumerNode);
            Object raw = node == null ? "" : node.metadata().get("downstreamChannel");
            String downstreamChannel = SignalChannel.normalize(raw instanceof String text ? text : "");
            if (!downstreamChannel.isBlank()) {
                String downstreamNode = addDownstreamChannelNode(build, downstreamChannel, depth + 1);
                downstream.add(downstreamNode);
                build.edges.add(new WebAdminDtos.LogicChainEdgeDto(consumerNode, downstreamNode, "join_output", "汇合输出", "dashed"));
            }
        }
        if (consumers.isEmpty()) {
            build.warnings.add("频道 " + channel + " 当前没有消费者。");
        }
        build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                segmentId,
                channel,
                depth,
                true,
                "expanded",
                List.copyOf(producers),
                List.copyOf(consumers),
                List.copyOf(actions),
                List.copyOf(downstream),
                consumers.isEmpty() ? List.of("当前频道暂无消费者。") : List.of()
        ));
        for (String downstreamNode : downstream) {
            WebAdminDtos.LogicChainNodeDto node = build.nodes.get(downstreamNode);
            if (node != null && depth + 1 <= build.maxDepth) {
                buildSegment(build, node.channel(), depth + 1, visiting);
            } else if (node != null) {
                build.warnings.add("频道 " + node.channel() + " 超出最大展开深度，已作为下游频道卡片保留。");
            }
        }
        visiting.remove(channel);
    }

    private List<String> producersFor(GraphBuild build, String channel) {
        List<String> result = new ArrayList<>();
        for (SignalDeviceData raw : build.snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type()) || SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                continue;
            }
            for (ChannelRef ref : deviceChannelRefs(device)) {
                if (ref.channel().equals(channel)) {
                    String id = "producer:device:" + safeNodeId(device.id()) + ":" + safeNodeId(ref.field());
                    boolean enabled = device.enabled() && ref.enabled();
                    if (!includeNode(build, enabled)) {
                        continue;
                    }
                    result.add(addNode(build, id, "producer", "device", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), ref.label(), channel, enabled, "#/devices/" + encode(device.id()), Map.of("field", ref.field(), "sourceType", device.type())));
                    build.edges.add(new WebAdminDtos.LogicChainEdgeDto(id, "channel:" + channel, "emits", ref.label(), "solid"));
                }
            }
        }
        for (SignalListenerData raw : build.snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            addSignalActionProducers(build, result, channel, listener.actions(), "listener", listener.id(), listener.name(), listener.enabled(), "#/listeners/" + encode(listener.id()), listener.channel());
        }
        for (SignalDeviceData raw : build.snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            ActionRelayBlockEntity relay = loadedActionRelay(build.snapshot.server, device);
            if (relay != null) {
                addSignalActionProducers(build, result, channel, relay.actions(), "action_relay", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), device.enabled(), "#/devices/" + encode(device.id()), device.channel());
            }
        }
        for (RegionControllerData raw : build.snapshot.regions) {
            RegionControllerData region = raw.normalized();
            addRegionActionProducers(build, result, channel, region, RegionTriggerType.ENTER, region.enterActions());
            addRegionActionProducers(build, result, channel, region, RegionTriggerType.EXIT, region.exitActions());
            addRegionActionProducers(build, result, channel, region, RegionTriggerType.STAY, region.stayActions());
        }
        for (SignalJoinDefinition raw : build.snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            if (!SignalChannel.normalize(join.outputChannel).equals(channel)) {
                continue;
            }
            String id = "producer:signal_join:" + safeNodeId(join.id);
            boolean enabled = join.enabled;
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addNode(build, id, "producer", "signal_join", join.id, joinName(join), "Signal Join 输出 · " + join.mode.displayName(), channel, enabled, "#/signal-joins/" + encode(join.id), joinMetadata(build, join, "")));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto(id, "channel:" + channel, "emits", "汇合输出", "solid"));
        }
        return List.copyOf(result);
    }

    private void addSignalActionProducers(
            GraphBuild build,
            List<String> result,
            String channel,
            List<ActionConfig> actions,
            String refType,
            String refId,
            String refName,
            boolean ownerEnabled,
            String detailRoute,
            String ownerChannel
    ) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            if (action == null || action.type() != ActionType.SIGNAL || !SignalChannel.normalize(action.value()).equals(channel)) {
                continue;
            }
            String id = "producer:" + refType + ":" + safeNodeId(refId) + ":" + i;
            boolean enabled = ownerEnabled && action.enabled() && action.isUsable();
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addNode(build, id, "producer", refType, refId, safe(refName).isBlank() ? refId : refName, "signal action #" + (i + 1) + " · " + safe(ownerChannel), channel, enabled, detailRoute, Map.of("actionIndex", i, "ownerChannel", safe(ownerChannel))));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto(id, "channel:" + channel, "emits", "signal action", "solid"));
        }
    }

    private void addRegionActionProducers(GraphBuild build, List<String> result, String channel, RegionControllerData region, RegionTriggerType trigger, List<ActionConfig> actions) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            if (action == null || action.type() != ActionType.SIGNAL || !SignalChannel.normalize(action.value()).equals(channel)) {
                continue;
            }
            String id = "producer:region_controller:" + safeNodeId(region.id()) + ":" + trigger.name().toLowerCase(Locale.ROOT) + ":" + i;
            boolean enabled = region.enabled() && action.enabled() && action.isUsable();
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addNode(build, id, "producer", "region_controller", region.id(), region.name().isBlank() ? region.id() : region.name(), triggerLabel(trigger) + " signal action #" + (i + 1), channel, enabled, "#/region-controllers/" + encode(region.id()), Map.of("triggerType", trigger.name(), "regionId", region.regionId())));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto(id, "channel:" + channel, "emits", triggerLabel(trigger), "solid"));
        }
    }

    private List<String> consumersFor(GraphBuild build, String channel) {
        List<String> result = new ArrayList<>();
        for (SignalListenerData raw : build.snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            if (!SignalChannel.normalize(listener.channel()).equals(channel)) {
                continue;
            }
            String id = "consumer:listener:" + safeNodeId(listener.id());
            if (!includeNode(build, listener.enabled())) {
                continue;
            }
            result.add(addNode(build, id, "consumer", "listener", listener.id(), listener.name().isBlank() ? listener.id() : listener.name(), listener.actions().size() + " 个动作", channel, listener.enabled(), "#/listeners/" + encode(listener.id()), Map.of("actionCount", listener.actions().size(), "cooldownTicks", listener.cooldownTicks())));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto("channel:" + channel, id, "consumes", "并列消费者", "solid"));
        }
        for (SignalDeviceData raw : build.snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            if (!SignalChannel.normalize(device.channel()).equals(channel)) {
                continue;
            }
            if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type())) {
                String id = "consumer:receiver:" + safeNodeId(device.id());
                if (!includeNode(build, device.enabled())) {
                    continue;
                }
                result.add(addNode(build, id, "consumer", "signal_receiver", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), "红石脉冲接收器", channel, device.enabled(), "#/devices/" + encode(device.id()), Map.of("pulseTicks", device.pulseTicks(), "pos", posMap(device))));
                build.edges.add(new WebAdminDtos.LogicChainEdgeDto("channel:" + channel, id, "consumes", "接收器", "solid"));
            } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                ActionRelayBlockEntity relay = loadedActionRelay(build.snapshot.server, device);
                int actionCount = relay == null ? device.actionCount() : relay.actions().size();
                String id = "consumer:action_relay:" + safeNodeId(device.id());
                if (!includeNode(build, device.enabled())) {
                    continue;
                }
                result.add(addNode(build, id, "consumer", "action_relay", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), actionCount + " 个动作" + (relay == null ? " · 未加载" : ""), channel, device.enabled(), "#/devices/" + encode(device.id()), Map.of("actionCount", actionCount, "loaded", relay != null, "pos", posMap(device))));
                build.edges.add(new WebAdminDtos.LogicChainEdgeDto("channel:" + channel, id, "consumes", "动作继电器", "solid"));
            }
        }
        for (SignalJoinDefinition raw : build.snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            if (!join.inputChannelNames().contains(channel)) {
                continue;
            }
            String id = "consumer:signal_join:" + safeNodeId(join.id) + ":input:" + safeNodeId(channel);
            boolean enabled = join.enabled;
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addNode(build, id, "consumer", "signal_join", join.id, joinName(join), join.mode.displayName() + " → " + join.outputChannel, channel, enabled, "#/signal-joins/" + encode(join.id), joinMetadata(build, join, channel)));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto("channel:" + channel, id, "consumes", "信号汇合", "solid"));
        }
        return List.copyOf(result);
    }

    private List<String> actionsFor(GraphBuild build, String channel, List<String> consumers) {
        List<String> result = new ArrayList<>();
        for (SignalListenerData raw : build.snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            if (!SignalChannel.normalize(listener.channel()).equals(channel)) {
                continue;
            }
            addConsumerActions(build, result, "consumer:listener:" + safeNodeId(listener.id()), listener.actions(), "listener", listener.id(), listener.name(), listener.channel(), listener.enabled(), "#/listeners/" + encode(listener.id()));
        }
        for (SignalDeviceData raw : build.snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            if (!SignalChannel.normalize(device.channel()).equals(channel) || !SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                continue;
            }
            ActionRelayBlockEntity relay = loadedActionRelay(build.snapshot.server, device);
            if (relay != null) {
                addConsumerActions(build, result, "consumer:action_relay:" + safeNodeId(device.id()), relay.actions(), "action_relay", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), device.channel(), device.enabled(), "#/devices/" + encode(device.id()));
            } else if (device.actionCount() > 0) {
                build.warnings.add("动作继电器 " + WebAdminReadonlySupport.deviceDisplayName(device) + " 未加载，无法展开其动作列表；已保留消费者节点。");
            }
        }
        return List.copyOf(result);
    }

    private void addConsumerActions(
            GraphBuild build,
            List<String> result,
            String consumerNode,
            List<ActionConfig> actions,
            String ownerType,
            String ownerId,
            String ownerName,
            String ownerChannel,
            boolean ownerEnabled,
            String detailRoute
    ) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            String downstream = action != null && action.type() == ActionType.SIGNAL ? SignalChannel.normalize(action.value()) : "";
            String id = "action:" + ownerType + ":" + safeNodeId(ownerId) + ":" + i;
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("actionIndex", i);
            metadata.put("ownerType", ownerType);
            metadata.put("ownerId", ownerId);
            metadata.put("ownerChannel", ownerChannel);
            metadata.put("actionType", WebAdminReadonlySupport.actionType(action));
            metadata.put("summary", WebAdminReadonlySupport.actionSummary(action));
            metadata.put("downstreamChannel", downstream);
            boolean enabled = ownerEnabled && action != null && action.enabled() && action.isUsable();
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addNode(build, id, "action", "action", ownerType + ":" + ownerId + ":" + i, "#" + (i + 1) + " " + labelAction(action), WebAdminReadonlySupport.actionSummary(action), ownerChannel, enabled, detailRoute, metadata));
            build.edges.add(new WebAdminDtos.LogicChainEdgeDto(consumerNode, id, "executes", "动作 #" + (i + 1), "solid"));
        }
    }

    private static boolean includeNode(GraphBuild build, boolean enabled) {
        return build.includeDisabled || enabled;
    }

    private String addChannelNode(GraphBuild build, String channel) {
        String id = "channel:" + channel;
        return addNode(build, id, "channel", "channel", channel, channel, "当前频道段", channel, true, "#/signals/" + encode(channel), Map.of("parallelConsumers", true, "readOnly", true));
    }

    private String addDownstreamChannelNode(GraphBuild build, String channel, int depth) {
        String id = "downstream_channel:" + safeNodeId(channel) + ":" + depth;
        return addNode(build, id, "downstream_channel", "channel", channel, channel, depth > build.maxDepth ? "还有下游，超出当前深度" : "展开下一频道段", channel, true, "#/logic-chains/resolve?rootType=channel&rootRef=" + encode(channel), Map.of("expandsAsSegment", true, "depth", depth));
    }

    private String addNode(
            GraphBuild build,
            String id,
            String type,
            String refType,
            String refId,
            String label,
            String subtitle,
            String channel,
            boolean enabled,
            String detailRoute,
            Map<String, Object> metadata
    ) {
        build.nodes.putIfAbsent(id, new WebAdminDtos.LogicChainNodeDto(
                id,
                type,
                refType,
                refId,
                safe(label).isBlank() ? refId : label,
                safe(subtitle),
                SignalChannel.normalize(channel),
                enabled,
                enabled ? "OK" : "DISABLED",
                enabled ? "OK" : "WARNING",
                latestTime(channel),
                safe(detailRoute),
                metadata == null ? Map.of() : Map.copyOf(metadata)
        ));
        return id;
    }

    private GraphStats summarize(Snapshot snapshot, String rootChannel, boolean includeDisabled, int maxDepth) {
        if (rootChannel == null || rootChannel.isBlank()) {
            return new GraphStats(0, 0, 0, 0, 0, 0, "WARNING", "");
        }
        GraphBuild build = new GraphBuild(snapshot, includeDisabled, Math.max(1, Math.min(HARD_MAX_DEPTH, maxDepth <= 0 ? DEFAULT_MAX_DEPTH : maxDepth)));
        buildSegment(build, rootChannel, 0, new LinkedHashSet<>());
        return new GraphStats(
                build.channelCount(),
                build.countByType("producer"),
                build.countByType("consumer"),
                build.countByType("action"),
                build.countEdges("emits_downstream") + build.countEdges("join_output"),
                build.disabledCount(),
                build.warnings.isEmpty() ? "OK" : "WARNING",
                latestTime(rootChannel)
        );
    }

    private WebAdminDtos.LogicChainSummaryDto summary(
            String id,
            String displayName,
            String rootType,
            String rootRef,
            String rootChannel,
            boolean saved,
            WebAdminDtos.LogicChainMetadataDto metadata,
            GraphStats stats,
            ChannelHierarchy hierarchy
    ) {
        ChannelHierarchyInfo info = hierarchy.info(rootChannel);
        return new WebAdminDtos.LogicChainSummaryDto(
                id,
                displayName,
                rootType,
                rootRef,
                rootChannel,
                stats.channelCount(),
                stats.producerCount(),
                stats.consumerCount(),
                stats.actionCount(),
                stats.downstreamChannelCount(),
                stats.disabledNodeCount(),
                stats.doctorStatus(),
                stats.lastTriggeredAt(),
                saved,
                info.level(),
                info.parentChannel().isBlank() ? "" : autoChainId(info.parentChannel()),
                info.parentChannel(),
                info.upstreamLabel(),
                info.upstreamNodeId(),
                info.level() > 0,
                !saved && info.level() > 0,
                info.multipleParents(),
                info.cycle(),
                info.selfCycle(),
                hierarchy.childrenCount(rootChannel),
                info.level() == 0,
                metadata
        );
    }

    private Snapshot snapshot(MinecraftServer server) {
        return new Snapshot(
                server,
                SignalDeviceStore.getSnapshot(server),
                SignalListenerStore.getSnapshot(server),
                RegionControllerStore.getSnapshot(server),
                SignalJoinStore.getSnapshot(server)
        );
    }

    private LinkedHashSet<String> knownChannels(Snapshot snapshot) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (SignalDeviceData raw : snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            for (ChannelRef ref : deviceChannelRefs(device)) {
                addChannel(channels, ref.channel());
            }
            ActionRelayBlockEntity relay = loadedActionRelay(snapshot.server, device);
            if (relay != null) {
                addActionChannels(channels, relay.actions());
            }
        }
        for (SignalListenerData raw : snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            addChannel(channels, listener.channel());
            addActionChannels(channels, listener.actions());
        }
        for (RegionControllerData raw : snapshot.regions) {
            RegionControllerData region = raw.normalized();
            addActionChannels(channels, region.enterActions());
            addActionChannels(channels, region.exitActions());
            addActionChannels(channels, region.stayActions());
        }
        for (SignalJoinDefinition raw : snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            addChannel(channels, join.outputChannel);
            for (String input : join.inputChannelNames()) {
                addChannel(channels, input);
            }
        }
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            addChannel(channels, record.channel());
        }
        return channels;
    }

    private ChannelHierarchy channelHierarchy(Snapshot snapshot) {
        LinkedHashSet<String> channels = knownChannels(snapshot);
        Map<String, List<UpstreamRef>> parents = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> children = new LinkedHashMap<>();
        LinkedHashSet<String> selfCycles = new LinkedHashSet<>();
        for (SignalListenerData raw : snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            addActionHierarchy(parents, children, selfCycles, listener.channel(), listener.actions(), "listener", listener.id(), listener.name(), "虚拟监听器");
        }
        for (SignalDeviceData raw : snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            ActionRelayBlockEntity relay = loadedActionRelay(snapshot.server, device);
            if (relay != null) {
                addActionHierarchy(parents, children, selfCycles, device.channel(), relay.actions(), "action_relay", device.id(), WebAdminReadonlySupport.deviceDisplayName(device), "动作继电器");
            }
        }
        for (SignalJoinDefinition raw : snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            for (String input : join.inputChannelNames()) {
                addJoinHierarchy(parents, children, selfCycles, input, join);
            }
        }
        for (String channel : channels) {
            parents.putIfAbsent(channel, List.of());
            children.putIfAbsent(channel, new LinkedHashSet<>());
        }
        Map<String, TempHierarchyInfo> assigned = new LinkedHashMap<>();
        LinkedHashSet<String> cycleChannels = new LinkedHashSet<>(selfCycles);
        for (String channel : channels) {
            if (parents.getOrDefault(channel, List.of()).isEmpty()) {
                assignHierarchyInfo(channel, 0, "", null, parents, children, assigned, cycleChannels, new LinkedHashSet<>());
            }
        }
        for (String channel : channels) {
            if (!assigned.containsKey(channel)) {
                assignHierarchyInfo(channel, 0, "", null, parents, children, assigned, cycleChannels, new LinkedHashSet<>());
            }
        }
        Map<String, ChannelHierarchyInfo> info = new LinkedHashMap<>();
        for (String channel : channels) {
            TempHierarchyInfo temp = assigned.getOrDefault(channel, TempHierarchyInfo.root());
            info.put(channel, new ChannelHierarchyInfo(
                    temp.level(),
                    temp.parentChannel(),
                    temp.upstreamLabel(),
                    temp.upstreamNodeId(),
                    parents.getOrDefault(channel, List.of()).size() > 1,
                    cycleChannels.contains(channel),
                    selfCycles.contains(channel)
            ));
        }
        return new ChannelHierarchy(info, children);
    }

    private void addActionHierarchy(
            Map<String, List<UpstreamRef>> parents,
            Map<String, LinkedHashSet<String>> children,
            LinkedHashSet<String> selfCycles,
            String ownerChannel,
            List<ActionConfig> actions,
            String ownerType,
            String ownerId,
            String ownerName,
            String ownerLabel
    ) {
        String parent = SignalChannel.normalize(ownerChannel);
        if (parent.isBlank()) {
            return;
        }
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            if (action == null || action.type() != ActionType.SIGNAL) {
                continue;
            }
            String child = SignalChannel.normalize(action.value());
            if (child.isBlank()) {
                continue;
            }
            if (child.equals(parent)) {
                selfCycles.add(parent);
                continue;
            }
            String label = safe(ownerName).isBlank() ? ownerId : ownerName;
            String upstreamLabel = ownerLabel + " " + label + " / 动作 #" + (i + 1);
            String upstreamNodeId = "action:" + ownerType + ":" + safeNodeId(ownerId) + ":" + i;
            parents.computeIfAbsent(child, ignored -> new ArrayList<>()).add(new UpstreamRef(parent, upstreamLabel, upstreamNodeId));
            children.computeIfAbsent(parent, ignored -> new LinkedHashSet<>()).add(child);
        }
    }

    private void addJoinHierarchy(
            Map<String, List<UpstreamRef>> parents,
            Map<String, LinkedHashSet<String>> children,
            LinkedHashSet<String> selfCycles,
            String rawInputChannel,
            SignalJoinDefinition join
    ) {
        String parent = SignalChannel.normalize(rawInputChannel);
        String child = SignalChannel.normalize(join == null ? "" : join.outputChannel);
        if (parent.isBlank() || child.isBlank()) {
            return;
        }
        if (child.equals(parent)) {
            selfCycles.add(parent);
            return;
        }
        String upstreamLabel = "信号汇合 " + joinName(join);
        String upstreamNodeId = "consumer:signal_join:" + safeNodeId(join.id) + ":input:" + safeNodeId(parent);
        parents.computeIfAbsent(child, ignored -> new ArrayList<>()).add(new UpstreamRef(parent, upstreamLabel, upstreamNodeId));
        children.computeIfAbsent(parent, ignored -> new LinkedHashSet<>()).add(child);
    }

    private void assignHierarchyInfo(
            String channel,
            int level,
            String parentChannel,
            UpstreamRef upstreamRef,
            Map<String, List<UpstreamRef>> parents,
            Map<String, LinkedHashSet<String>> children,
            Map<String, TempHierarchyInfo> assigned,
            LinkedHashSet<String> cycleChannels,
            LinkedHashSet<String> path
    ) {
        String safeChannel = SignalChannel.normalize(channel);
        if (safeChannel.isBlank()) {
            return;
        }
        if (path.contains(safeChannel)) {
            cycleChannels.add(safeChannel);
            return;
        }
        if (assigned.containsKey(safeChannel)) {
            return;
        }
        assigned.put(safeChannel, new TempHierarchyInfo(
                level,
                SignalChannel.normalize(parentChannel),
                upstreamRef == null ? "" : upstreamRef.label(),
                upstreamRef == null ? "" : upstreamRef.nodeId()
        ));
        path.add(safeChannel);
        for (String rawChild : children.getOrDefault(safeChannel, new LinkedHashSet<>())) {
            String child = SignalChannel.normalize(rawChild);
            if (child.isBlank()) {
                continue;
            }
            if (child.equals(safeChannel)) {
                cycleChannels.add(safeChannel);
                continue;
            }
            if (path.contains(child)) {
                cycleChannels.add(child);
                cycleChannels.add(safeChannel);
                continue;
            }
            assignHierarchyInfo(child, level + 1, safeChannel, upstreamRefFor(parents, child, safeChannel), parents, children, assigned, cycleChannels, path);
        }
        path.remove(safeChannel);
    }

    private UpstreamRef upstreamRefFor(Map<String, List<UpstreamRef>> parents, String childChannel, String parentChannel) {
        String parent = SignalChannel.normalize(parentChannel);
        for (UpstreamRef ref : parents.getOrDefault(SignalChannel.normalize(childChannel), List.of())) {
            if (ref.parentChannel().equals(parent)) {
                return ref;
            }
        }
        List<UpstreamRef> refs = parents.getOrDefault(SignalChannel.normalize(childChannel), List.of());
        return refs.isEmpty() ? null : refs.get(0);
    }

    private String resolveRootChannel(Snapshot snapshot, String rootType, String rootRef) {
        String type = normalizeRootType(rootType);
        String ref = safe(rootRef).trim();
        if ("channel".equals(type)) {
            return SignalChannel.normalize(ref);
        }
        if ("listener".equals(type)) {
            for (SignalListenerData listener : snapshot.listeners) {
                if (listener.id().equals(ref) || listener.name().equals(ref)) {
                    return SignalChannel.normalize(listener.channel());
                }
            }
        }
        if ("receiver".equals(type) || "relay".equals(type) || "device".equals(type)) {
            for (SignalDeviceData raw : snapshot.devices) {
                SignalDeviceData device = raw.normalized();
                if ((device.id().equals(ref) || SignalDeviceStore.shortId(device.id()).equals(ref)) && deviceMatchesRootType(device, type)) {
                    return SignalChannel.normalize(device.channel());
                }
            }
        }
        if ("region".equals(type) || "region_controller".equals(type)) {
            for (RegionControllerData raw : snapshot.regions) {
                RegionControllerData region = raw.normalized();
                if (region.id().equals(ref) || region.regionId().equals(ref)) {
                    for (ActionConfig action : combinedRegionActions(region)) {
                        if (action != null && action.type() == ActionType.SIGNAL) {
                            return SignalChannel.normalize(action.value());
                        }
                    }
                }
            }
        }
        if ("action".equals(type)) {
            return resolveActionRootChannel(snapshot, ref);
        }
        if ("signal_join".equals(type)) {
            for (SignalJoinDefinition raw : snapshot.joins) {
                SignalJoinDefinition join = raw.normalized();
                if (join.id.equals(ref) || join.displayName.equals(ref)) {
                    return SignalChannel.normalize(join.outputChannel);
                }
            }
        }
        return "";
    }

    private List<String> rootWarnings(Snapshot snapshot, String rootType, String rootRef, String rootChannel) {
        String type = normalizeRootType(rootType);
        String ref = safe(rootRef).trim();
        if ("channel".equals(type)) {
            return List.of();
        }
        if (rootChannel.isBlank()) {
            return List.of("Root " + type + " / " + ref + " 当前无法解析到 Signal 频道。");
        }
        if ("region".equals(type) || "region_controller".equals(type)) {
            int count = 0;
            for (RegionControllerData raw : snapshot.regions) {
                RegionControllerData region = raw.normalized();
                if (region.id().equals(ref) || region.regionId().equals(ref)) {
                    for (ActionConfig action : combinedRegionActions(region)) {
                        if (action != null && action.type() == ActionType.SIGNAL && !SignalChannel.normalize(action.value()).isBlank()) {
                            count++;
                        }
                    }
                }
            }
            if (count > 1) {
                return List.of("区域控制器包含多个 signal action，当前视图以第一个可解析频道作为 root；其它 signal action 可从节点或频道入口分别查看。");
            }
        }
        return List.of();
    }

    private static boolean deviceMatchesRootType(SignalDeviceData device, String rootType) {
        if ("device".equals(rootType)) {
            return true;
        }
        if ("receiver".equals(rootType)) {
            return SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type());
        }
        if ("relay".equals(rootType)) {
            return SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type());
        }
        return false;
    }

    private String resolveActionRootChannel(Snapshot snapshot, String rootRef) {
        for (SignalListenerData raw : snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            for (int i = 0; i < listener.actions().size(); i++) {
                if (actionId("LISTENER", listener.id(), i).equals(rootRef)) {
                    return SignalChannel.normalize(listener.channel());
                }
            }
        }
        for (RegionControllerData raw : snapshot.regions) {
            RegionControllerData region = raw.normalized();
            String resolved = resolveRegionActionRootChannel(rootRef, region, "REGION_ENTER", region.enterActions());
            if (!resolved.isBlank()) {
                return resolved;
            }
            resolved = resolveRegionActionRootChannel(rootRef, region, "REGION_EXIT", region.exitActions());
            if (!resolved.isBlank()) {
                return resolved;
            }
            resolved = resolveRegionActionRootChannel(rootRef, region, "REGION_STAY", region.stayActions());
            if (!resolved.isBlank()) {
                return resolved;
            }
        }
        for (SignalDeviceData raw : snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type()) && ("ACTION_RELAY:" + device.id()).equals(rootRef)) {
                return SignalChannel.normalize(device.channel());
            }
        }
        return "";
    }

    private static String resolveRegionActionRootChannel(String rootRef, RegionControllerData region, String ownerType, List<ActionConfig> actions) {
        List<ActionConfig> source = actions == null ? List.of() : actions;
        for (int i = 0; i < source.size(); i++) {
            ActionConfig action = source.get(i);
            if (actionId(ownerType, region.id(), i).equals(rootRef) && action != null && action.type() == ActionType.SIGNAL) {
                return SignalChannel.normalize(action.value());
            }
        }
        return "";
    }

    private static String actionId(String ownerType, String ownerId, int index) {
        return safe(ownerType) + ":" + safe(ownerId) + ":" + index;
    }

    private List<ChannelRef> deviceChannelRefs(SignalDeviceData device) {
        List<ChannelRef> refs = new ArrayList<>();
        addRef(refs, device.channel(), "channel", "主频道", device.enabled());
        addRef(refs, device.offChannel(), "offChannel", "关闭频道", device.enabled());
        addRef(refs, device.interactChannel(), "interactChannel", "右键交互频道", device.enabled() && device.interactionEnabled());
        addRef(refs, device.containerOpenChannel(), "containerOpenChannel", "容器打开频道", device.enabled() && device.containerEnabled());
        addRef(refs, device.containerCloseChannel(), "containerCloseChannel", "容器关闭频道", device.enabled() && device.containerEnabled());
        addRef(refs, device.containerChangeChannel(), "containerChangeChannel", "容器变化频道", device.enabled() && device.containerEnabled());
        for (ContainerItemConditionData condition : device.itemConditions()) {
            ContainerItemConditionData normalized = condition.normalized();
            addRef(refs, normalized.channel(), "itemCondition:" + normalized.name(), "容器物品条件", device.enabled() && device.containerEnabled() && normalized.enabled());
            addRef(refs, normalized.offChannel(), "itemConditionOff:" + normalized.name(), "容器物品条件退出", device.enabled() && device.containerEnabled() && normalized.enabled());
        }
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        addRef(refs, matcher.successChannel(), "matcherSuccessChannel", "交互匹配成功", device.enabled() && device.interactionEnabled() && matcher.enabled());
        addRef(refs, matcher.failChannel(), "matcherFailChannel", "交互匹配失败", device.enabled() && device.interactionEnabled() && matcher.enabled());
        return List.copyOf(refs);
    }

    private static void addRef(List<ChannelRef> refs, String channel, String field, String label, boolean enabled) {
        String normalized = SignalChannel.normalize(channel);
        if (!normalized.isBlank()) {
            refs.add(new ChannelRef(normalized, field, label, enabled));
        }
    }

    private static void addActionChannels(Set<String> channels, List<ActionConfig> actions) {
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action != null && action.type() == ActionType.SIGNAL) {
                addChannel(channels, action.value());
            }
        }
    }

    private static void addChannel(Set<String> channels, String channel) {
        String normalized = SignalChannel.normalize(channel);
        if (!normalized.isBlank()) {
            channels.add(normalized);
        }
    }

    private static List<ActionConfig> combinedRegionActions(RegionControllerData region) {
        List<ActionConfig> actions = new ArrayList<>();
        actions.addAll(region.enterActions() == null ? List.of() : region.enterActions());
        actions.addAll(region.exitActions() == null ? List.of() : region.exitActions());
        actions.addAll(region.stayActions() == null ? List.of() : region.stayActions());
        return List.copyOf(actions);
    }

    private ActionRelayBlockEntity loadedActionRelay(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null || !SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
            return null;
        }
        return SignalDeviceStore.getLoadedActionRelay(server, device);
    }

    public static String fingerprintFor(WebAdminLogicChainMetadataStore.MetadataEntry rawEntry) {
        WebAdminLogicChainMetadataStore.MetadataEntry entry = WebAdminLogicChainMetadataStore.MetadataEntry.normalized(rawEntry == null ? "" : rawEntry.id, rawEntry);
        String input = "logic_chain_metadata|"
                + entry.id + "|"
                + entry.displayName + "|"
                + entry.note + "|"
                + entry.iconKey + "|"
                + String.join(",", entry.tags) + "|"
                + entry.group + "|"
                + entry.rootType + "|"
                + entry.rootRef + "|"
                + entry.includeDisabled + "|"
                + entry.maxDepth + "|"
                + entry.layoutPreference + "|"
                + entry.version;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private WebAdminDtos.LogicChainMetadataDto metadataDto(
            WebAdminLogicChainMetadataStore.MetadataEntry rawEntry,
            String rootChannel,
            WebAdminUser user,
            WebAdminSession session
    ) {
        WebAdminLogicChainMetadataStore.MetadataEntry entry = WebAdminLogicChainMetadataStore.MetadataEntry.normalized(rawEntry == null ? "" : rawEntry.id, rawEntry);
        String effectiveName = entry.displayName.isBlank()
                ? (rootChannel == null || rootChannel.isBlank() ? entry.rootRef : "逻辑链：" + rootChannel)
                : entry.displayName;
        String effectiveIcon = "auto".equals(entry.iconKey) || entry.iconKey.isBlank() ? "action-binding" : entry.iconKey;
        return new WebAdminDtos.LogicChainMetadataDto(
                entry.id,
                entry.displayName,
                entry.note,
                entry.iconKey,
                effectiveName,
                effectiveIcon,
                List.copyOf(entry.tags),
                entry.group,
                entry.rootType,
                entry.rootRef,
                rootChannel == null ? "" : rootChannel,
                entry.includeDisabled,
                entry.maxDepth,
                entry.layoutPreference,
                entry.updatedAt,
                entry.updatedBy,
                entry.version,
                fingerprintFor(entry),
                editLockService == null ? null : editLockService.status(WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, entry.id, user, session)
        );
    }

    private WebAdminLogicChainMetadataStore.MetadataEntry normalizedEntry(WebAdminLogicChainMetadataRequest request, String id, long version, WebAdminUser user) {
        WebAdminLogicChainMetadataStore.MetadataEntry entry = new WebAdminLogicChainMetadataStore.MetadataEntry();
        entry.id = id;
        entry.displayName = safe(request.displayName).trim();
        entry.note = safe(request.note).trim();
        entry.iconKey = safe(request.iconKey).isBlank() ? "auto" : safe(request.iconKey).trim();
        entry.tags = request.tags == null ? new ArrayList<>() : new ArrayList<>(request.tags);
        entry.group = safe(request.group).trim();
        entry.rootType = normalizeRootType(request.rootType);
        entry.rootRef = normalizeRootRef(entry.rootType, request.rootRef);
        entry.includeDisabled = request.includeDisabled;
        entry.maxDepth = Math.max(1, Math.min(HARD_MAX_DEPTH, request.maxDepth <= 0 ? DEFAULT_MAX_DEPTH : request.maxDepth));
        entry.layoutPreference = normalizeLayoutPreference(request.layoutPreference);
        entry.updatedAt = Instant.now().toString();
        entry.updatedBy = user == null ? "" : user.username;
        entry.version = Math.max(0L, version);
        return WebAdminLogicChainMetadataStore.MetadataEntry.normalized(id, entry);
    }

    private List<WebAdminValidationError> validateMetadataRequest(Snapshot snapshot, WebAdminLogicChainMetadataRequest request, String id) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (id.isBlank()) {
            errors.add(new WebAdminValidationError("chainId", "invalid", "逻辑链 ID 不能为空，且只能包含小写字母、数字、下划线、点、冒号和连字符。", safe(request.chainId)));
        }
        if (safe(request.displayName).trim().length() > 64) {
            errors.add(new WebAdminValidationError("displayName", "too_long", "显示名不能超过 64 个字符。", request.displayName));
        }
        if (safe(request.note).trim().length() > 512) {
            errors.add(new WebAdminValidationError("note", "too_long", "备注不能超过 512 个字符。", request.note));
        }
        if (safe(request.group).trim().length() > 64) {
            errors.add(new WebAdminValidationError("group", "too_long", "分组不能超过 64 个字符。", request.group));
        }
        if (!WebAdminDeviceMetadataService.isAllowedIconKey(safe(request.iconKey).isBlank() ? "auto" : request.iconKey)) {
            errors.add(new WebAdminValidationError("iconKey", "invalid_icon", "图标必须来自 WebAdmin 预设列表。", request.iconKey));
        }
        String rootType = normalizeRootType(request.rootType);
        String rootRef = normalizeRootRef(rootType, request.rootRef);
        if (rootRef.isBlank()) {
            errors.add(new WebAdminValidationError("rootRef", "required", "逻辑链 root 不能为空。", request.rootRef));
        }
        if ("channel".equals(rootType) && !SignalChannel.isValid(rootRef)) {
            errors.add(new WebAdminValidationError("rootRef", "invalid_channel", "root channel 只能包含小写字母、数字、下划线、点、冒号和连字符。", request.rootRef));
        }
        if (!"channel".equals(rootType)) {
            if (rootRef.length() > 160 || containsControlCharacter(rootRef)) {
                errors.add(new WebAdminValidationError("rootRef", "invalid_ref", "root 引用不能超过 160 个字符，且不能包含控制字符。", request.rootRef));
            } else if (!rootRefExists(snapshot, rootType, rootRef)) {
                errors.add(new WebAdminValidationError("rootRef", "not_found", "当前 root 引用不存在或类型不匹配。", request.rootRef));
            }
        }
        return List.copyOf(errors);
    }

    private boolean rootRefExists(Snapshot snapshot, String rootType, String rootRef) {
        String type = normalizeRootType(rootType);
        String ref = safe(rootRef).trim();
        if ("listener".equals(type)) {
            for (SignalListenerData listener : snapshot.listeners) {
                if (listener.id().equals(ref) || listener.name().equals(ref)) {
                    return true;
                }
            }
            return false;
        }
        if ("device".equals(type) || "receiver".equals(type) || "relay".equals(type)) {
            for (SignalDeviceData raw : snapshot.devices) {
                SignalDeviceData device = raw.normalized();
                if ((device.id().equals(ref) || SignalDeviceStore.shortId(device.id()).equals(ref)) && deviceMatchesRootType(device, type)) {
                    return true;
                }
            }
            return false;
        }
        if ("region".equals(type) || "region_controller".equals(type)) {
            for (RegionControllerData raw : snapshot.regions) {
                RegionControllerData region = raw.normalized();
                if (region.id().equals(ref) || region.regionId().equals(ref)) {
                    return true;
                }
            }
            return false;
        }
        if ("action".equals(type)) {
            return !resolveActionRootChannel(snapshot, ref).isBlank();
        }
        if ("signal_join".equals(type)) {
            for (SignalJoinDefinition raw : snapshot.joins) {
                SignalJoinDefinition join = raw.normalized();
                if (join.id.equals(ref) || join.displayName.equals(ref)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static boolean containsControlCharacter(String value) {
        String text = safe(value);
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            String lockId,
            String chainId
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            return WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
        }
        if (!sameOrigin) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, chainId, lockId, user, session);
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "逻辑链元数据写入安全校验通过。");
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(WebAdminLogicChainMetadataStore.MetadataEntry metadata, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        String routeTarget = "#/logic-chains/" + encode(metadata.id);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .severity("INFO")
                .summary("逻辑链显示信息已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "logic_chain_metadata")
                .payload("targetId", metadata.id)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.LOGIC_CHAIN_METADATA_CHANGED)
                .severity("INFO")
                .summary("逻辑链显示信息已更新：" + metadata.id)
                .routeTarget(routeTarget)
                .payload("chainId", metadata.id)
                .payload("rootType", metadata.rootType)
                .payload("rootRef", metadata.rootRef)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id()));
    }

    private void releaseLockAfterWrite(WebAdminLogicChainMetadataRequest request, WebAdminUser user, WebAdminSession session, String remoteAddress, String chainId) {
        if (editLockService == null || request == null || safe(request.lockId).isBlank()) {
            return;
        }
        editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, chainId, request.lockId, user, session, remoteAddress);
    }

    private static WebAdminWriteResult conflictDetected(WebAdminWriteTarget target, WebAdminLogicChainMetadataStore.MetadataEntry current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentMetadata", beforeSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "逻辑链显示信息已被其他用户修改，请刷新后再编辑。",
                target.targetType(),
                target.targetId(),
                false,
                List.of(),
                "",
                "",
                false,
                conflict,
                Map.of()
        );
    }

    private static Map<String, Object> beforeSummary(WebAdminLogicChainMetadataStore.MetadataEntry entry) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (entry == null) {
            return summary;
        }
        summary.put("id", entry.id);
        summary.put("displayName", entry.displayName);
        summary.put("note", entry.note);
        summary.put("iconKey", entry.iconKey);
        summary.put("tags", entry.tags);
        summary.put("group", entry.group);
        summary.put("rootType", entry.rootType);
        summary.put("rootRef", entry.rootRef);
        summary.put("includeDisabled", entry.includeDisabled);
        summary.put("maxDepth", entry.maxDepth);
        summary.put("layoutPreference", entry.layoutPreference);
        summary.put("version", entry.version);
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminLogicChainMetadataRequest request, String id) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", id);
        summary.put("displayName", safe(request.displayName));
        summary.put("rootType", normalizeRootType(request.rootType));
        summary.put("rootRef", normalizeRootRef(normalizeRootType(request.rootType), request.rootRef));
        summary.put("maxDepth", request.maxDepth);
        return summary;
    }

    private static boolean metadataEquals(WebAdminLogicChainMetadataStore.MetadataEntry before, WebAdminLogicChainMetadataStore.MetadataEntry after) {
        return safe(before.displayName).equals(safe(after.displayName))
                && safe(before.note).equals(safe(after.note))
                && safe(before.iconKey).equals(safe(after.iconKey))
                && safe(before.group).equals(safe(after.group))
                && safe(before.rootType).equals(safe(after.rootType))
                && safe(before.rootRef).equals(safe(after.rootRef))
                && before.includeDisabled == after.includeDisabled
                && before.maxDepth == after.maxDepth
                && safe(before.layoutPreference).equals(safe(after.layoutPreference))
                && before.tags.equals(after.tags);
    }

    private static WebAdminLogicChainMetadataStore.MetadataEntry defaultEntry(String id, String rootType, String rootRef) {
        WebAdminLogicChainMetadataStore.MetadataEntry entry = new WebAdminLogicChainMetadataStore.MetadataEntry();
        entry.id = normalizeChainId(id);
        entry.rootType = normalizeRootType(rootType);
        entry.rootRef = normalizeRootRef(entry.rootType, rootRef);
        entry.includeDisabled = true;
        entry.maxDepth = DEFAULT_MAX_DEPTH;
        entry.layoutPreference = "auto";
        entry.iconKey = "auto";
        return WebAdminLogicChainMetadataStore.MetadataEntry.normalized(entry.id, entry);
    }

    private static String generatedId(WebAdminLogicChainMetadataRequest request) {
        String rootType = normalizeRootType(request == null ? "" : request.rootType);
        String rootRef = normalizeRootRef(rootType, request == null ? "" : request.rootRef);
        return normalizeChainId(rootType + ":" + rootRef);
    }

    private static String autoChainId(String channel) {
        return "auto:channel:" + SignalChannel.normalize(channel);
    }

    private static String autoRootChainId(String rootType, String rootRef, String rootChannel) {
        String type = normalizeRootType(rootType);
        if ("channel".equals(type)) {
            return autoChainId(rootChannel.isBlank() ? rootRef : rootChannel);
        }
        return normalizeChainId("auto:" + type + ":" + safe(rootRef));
    }

    private static String normalizeChainId(String id) {
        String normalized = WebAdminLogicChainMetadataStore.normalizeId(id);
        if (normalized.isBlank() && safe(id).startsWith("auto:channel:")) {
            return "auto:channel:" + SignalChannel.normalize(id.substring("auto:channel:".length()));
        }
        return normalized;
    }

    private static WebAdminWriteTarget target(String id) {
        return new WebAdminWriteTarget("LOGIC_CHAIN_METADATA", id, id);
    }

    private static String normalizeRootType(String rootType) {
        String value = safe(rootType).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join" -> value;
            default -> "channel";
        };
    }

    private static String normalizeRootRef(String rootType, String rootRef) {
        String value = safe(rootRef).trim();
        return "channel".equals(normalizeRootType(rootType)) ? SignalChannel.normalize(value) : value;
    }

    private static String normalizeLayoutPreference(String value) {
        String safeValue = safe(value).trim().toLowerCase(Locale.ROOT);
        return switch (safeValue) {
            case "compact", "vertical" -> safeValue;
            default -> "auto";
        };
    }

    private static String triggerLabel(RegionTriggerType triggerType) {
        return switch (triggerType) {
            case ENTER -> "进入区域";
            case EXIT -> "离开区域";
            case STAY -> "停留区域";
        };
    }

    private static String labelAction(ActionConfig action) {
        if (action == null) {
            return "缺失动作";
        }
        return switch (action.type()) {
            case COMMAND -> "命令";
            case MESSAGE -> "消息";
            case SOUND -> "音效";
            case SIGNAL -> "发出频道";
        };
    }

    private static String latestTime(String channel) {
        List<SignalEventRecord> records = SignalEventHistory.snapshot(SignalChannel.normalize(channel));
        if (records.isEmpty()) {
            return "";
        }
        return WebAdminReadonlySupport.isoTime(records.get(records.size() - 1).wallTimeMillis());
    }

    private static String doctorStatus(GraphBuild build, String channel) {
        return build.warnings.stream().anyMatch(warning -> warning.contains(channel)) ? "WARNING" : "OK";
    }

    private static String joinName(SignalJoinDefinition join) {
        if (join == null) {
            return "Signal Join";
        }
        return join.displayName.isBlank() ? join.id : join.displayName;
    }

    private static Map<String, Object> joinMetadata(GraphBuild build, SignalJoinDefinition join, String inputChannel) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "signal_join");
        metadata.put("joinId", join.id);
        metadata.put("mode", join.mode.name());
        metadata.put("modeLabel", join.mode.displayName());
        metadata.put("threshold", join.threshold);
        metadata.put("scopeMode", join.scopeMode.name());
        metadata.put("scopeLabel", join.scopeMode.displayName());
        metadata.put("resetPolicy", join.resetPolicy.name());
        metadata.put("resetPolicyLabel", join.resetPolicy.displayName());
        metadata.put("timeoutTicks", join.timeoutTicks);
        metadata.put("cooldownTicks", join.cooldownTicks);
        metadata.put("inputChannels", join.inputChannelNames());
        metadata.put("inputChannel", SignalChannel.normalize(inputChannel));
        metadata.put("outputChannel", join.outputChannel);
        metadata.put("downstreamChannel", join.outputChannel);
        SignalJoinStatusSnapshot status = SignalJoinRuntimeService.status(build.snapshot.server, join, currentGameTime(build.snapshot.server));
        metadata.put("pendingScopeCount", status.pendingScopeCount());
        metadata.put("lastResult", status.lastResult());
        metadata.put("lastFailureReason", status.lastFailureReason());
        metadata.put("scopes", status.scopes());
        return metadata;
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
    }

    private static Map<String, Object> posMap(SignalDeviceData device) {
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("world", device.dimension());
        pos.put("x", device.x());
        pos.put("y", device.y());
        pos.put("z", device.z());
        return pos;
    }

    private static String safeNodeId(String value) {
        return safe(value).replaceAll("[^a-zA-Z0-9_.:-]", "_");
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Snapshot(
            MinecraftServer server,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins
    ) {
    }

    private record ChannelRef(String channel, String field, String label, boolean enabled) {
    }

    private record UpstreamRef(String parentChannel, String label, String nodeId) {
    }

    private record ChannelHierarchyInfo(
            int level,
            String parentChannel,
            String upstreamLabel,
            String upstreamNodeId,
            boolean multipleParents,
            boolean cycle,
            boolean selfCycle
    ) {
        private static ChannelHierarchyInfo root() {
            return new ChannelHierarchyInfo(0, "", "", "", false, false, false);
        }
    }

    private record TempHierarchyInfo(
            int level,
            String parentChannel,
            String upstreamLabel,
            String upstreamNodeId
    ) {
        private static TempHierarchyInfo root() {
            return new TempHierarchyInfo(0, "", "", "");
        }
    }

    private record ChannelHierarchy(
            Map<String, ChannelHierarchyInfo> infoByChannel,
            Map<String, LinkedHashSet<String>> childrenByChannel
    ) {
        private ChannelHierarchyInfo info(String channel) {
            return infoByChannel.getOrDefault(SignalChannel.normalize(channel), ChannelHierarchyInfo.root());
        }

        private int childrenCount(String channel) {
            return childrenByChannel.getOrDefault(SignalChannel.normalize(channel), new LinkedHashSet<>()).size();
        }
    }

    private record GraphStats(
            int channelCount,
            int producerCount,
            int consumerCount,
            int actionCount,
            int downstreamChannelCount,
            int disabledNodeCount,
            String doctorStatus,
            String lastTriggeredAt
    ) {
    }

    private static final class GraphBuild {
        private final Snapshot snapshot;
        private final boolean includeDisabled;
        private final int maxDepth;
        private final Map<String, WebAdminDtos.LogicChainNodeDto> nodes = new LinkedHashMap<>();
        private final List<WebAdminDtos.LogicChainEdgeDto> edges = new ArrayList<>();
        private final List<WebAdminDtos.LogicChainSegmentDto> segments = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final Set<String> visitedChannels = new LinkedHashSet<>();

        private GraphBuild(Snapshot snapshot, boolean includeDisabled, int maxDepth) {
            this.snapshot = snapshot;
            this.includeDisabled = includeDisabled;
            this.maxDepth = maxDepth;
        }

        private int channelCount() {
            return visitedChannels.size();
        }

        private int countByType(String type) {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (type.equals(node.type())) {
                    count++;
                }
            }
            return count;
        }

        private int countEdges(String type) {
            int count = 0;
            for (WebAdminDtos.LogicChainEdgeDto edge : edges) {
                if (type.equals(edge.type())) {
                    count++;
                }
            }
            return count;
        }

        private int disabledCount() {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (!node.enabled()) {
                    count++;
                }
            }
            return count;
        }
    }
}
