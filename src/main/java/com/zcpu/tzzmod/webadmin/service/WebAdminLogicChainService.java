package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.condition.state.StateVariableKey;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableStore.StateVariableLoadResult;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerRuntimeService;
import com.zcpu.tzzmod.scheduler.TimerStatusSnapshot;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinRuntimeService;
import com.zcpu.tzzmod.signal.join.SignalJoinStatusSnapshot;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
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
    private static final int MAX_CHAIN_INDEX_CHANNELS = 512;
    private static final int MAX_GRAPH_NODES = 320;
    private static final int MAX_GRAPH_EDGES = 640;
    private static final int MAX_COMPONENT_CHANNELS = 96;
    private static final int MAX_JOIN_INPUT_PORTS = 64;
    private static final int MAX_COMPONENT_METADATA_ROWS = 64;
    private static final String TRUNCATED_NODE_ID = "external:graph_truncated";

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
            buildComponent(build, rootChannel);
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
        Map<String, Object> stats = buildStats(build, rootChannel, safeDepth);
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

    static WebAdminDtos.LogicChainGraphDto graphForSnapshotForTest(
            String rootChannel,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins,
            List<TimerDefinition> timers,
            StateVariableLoadResult stateVariables,
            boolean includeDisabled,
            int maxDepth
    ) {
        Snapshot snapshot = new Snapshot(
                null,
                devices == null ? List.of() : List.copyOf(devices),
                listeners == null ? List.of() : List.copyOf(listeners),
                regions == null ? List.of() : List.copyOf(regions),
                joins == null ? List.of() : List.copyOf(joins),
                timers == null ? List.of() : List.copyOf(timers),
                stateVariables == null ? new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false) : stateVariables,
                new WebAdminDeviceMetadataStore.MetadataFile(),
                new WebAdminChannelMetadataStore.MetadataFile()
        );
        return graphForSnapshotForTest(snapshot, rootChannel, includeDisabled, maxDepth);
    }

    static WebAdminDtos.LogicChainGraphDto graphForSnapshotForTest(
            String rootChannel,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins,
            List<TimerDefinition> timers,
            StateVariableLoadResult stateVariables,
            boolean includeDisabled,
            int maxDepth,
            WebAdminDeviceMetadataStore.MetadataFile deviceMetadata,
            WebAdminChannelMetadataStore.MetadataFile channelMetadata
    ) {
        Snapshot snapshot = new Snapshot(
                null,
                devices == null ? List.of() : List.copyOf(devices),
                listeners == null ? List.of() : List.copyOf(listeners),
                regions == null ? List.of() : List.copyOf(regions),
                joins == null ? List.of() : List.copyOf(joins),
                timers == null ? List.of() : List.copyOf(timers),
                stateVariables == null ? new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false) : stateVariables,
                deviceMetadata == null ? new WebAdminDeviceMetadataStore.MetadataFile() : deviceMetadata.normalized(),
                channelMetadata == null ? new WebAdminChannelMetadataStore.MetadataFile() : channelMetadata.normalized()
        );
        return graphForSnapshotForTest(snapshot, rootChannel, includeDisabled, maxDepth);
    }

    private static WebAdminDtos.LogicChainGraphDto graphForSnapshotForTest(
            Snapshot snapshot,
            String rootChannel,
            boolean includeDisabled,
            int maxDepth
    ) {
        WebAdminLogicChainService service = new WebAdminLogicChainService(null, null, null);
        int safeDepth = Math.max(1, Math.min(HARD_MAX_DEPTH, maxDepth <= 0 ? DEFAULT_MAX_DEPTH : maxDepth));
        String channel = SignalChannel.normalize(rootChannel);
        GraphBuild build = new GraphBuild(snapshot, includeDisabled, safeDepth);
        if (channel.isBlank()) {
            build.warnings.add("当前 root 无法解析到 Signal 频道，图谱为空。");
        } else {
            service.buildComponent(build, channel);
        }
        Map<String, Object> stats = buildStats(build, channel, safeDepth);
        WebAdminDtos.LogicChainNodeDto root = build.nodes.getOrDefault("channel:" + channel, new WebAdminDtos.LogicChainNodeDto(
                "root:" + safeNodeId(channel),
                "root",
                "channel",
                channel,
                channel.isBlank() ? "未解析 root" : channel,
                channel.isBlank() ? "没有可解析频道" : channel,
                channel,
                true,
                channel.isBlank() ? "WARNING" : "OK",
                channel.isBlank() ? "WARNING" : "OK",
                "",
                channel.isBlank() ? "" : "#/signals/" + encode(channel),
                Map.of("readOnly", true)
        ));
        WebAdminDtos.LogicChainMetadataDto metadata = new WebAdminDtos.LogicChainMetadataDto(
                "test:" + channel,
                "测试逻辑链",
                "",
                "logic-chain",
                "测试逻辑链",
                "logic-chain",
                List.of(),
                "",
                "channel",
                channel,
                channel,
                includeDisabled,
                safeDepth,
                "tree",
                "",
                "",
                0L,
                "",
                null
        );
        return new WebAdminDtos.LogicChainGraphDto(
                metadata,
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

    private static Map<String, Object> buildStats(GraphBuild build, String focusChannel, int safeDepth) {
        Map<String, Object> stats = new LinkedHashMap<>();
        String safeFocus = SignalChannel.normalize(focusChannel);
        boolean componentTruncated = build.componentChannelsTruncated || build.nodesTruncated || build.edgesTruncated;
        Map<String, Object> componentSummary = new LinkedHashMap<>();
        componentSummary.put("focusChannel", safeFocus);
        componentSummary.put("focusComponentId", build.focusComponentId());
        componentSummary.put("channelCount", build.channelCount());
        componentSummary.put("producerCount", build.producerCount());
        componentSummary.put("consumerCount", build.consumerCount());
        componentSummary.put("actionCount", build.actionCount());
        componentSummary.put("signalJoinCount", build.countByType("signal_join"));
        componentSummary.put("timerCount", build.countByType("timer"));
        componentSummary.put("stateActionCount", build.countByType("state_action"));
        componentSummary.put("stateVariableCount", build.countByType("state_variable"));
        componentSummary.put("conditionGateCount", build.countByType("condition_gate"));
        componentSummary.put("actionGateCount", build.countByType("action_gate"));
        componentSummary.put("strongEdgeCount", build.associationEdgeCount("strong"));
        componentSummary.put("weakEdgeCount", build.associationEdgeCount("weak"));
        componentSummary.put("truncated", componentTruncated);
        componentSummary.put("truncationReason", build.componentTruncationReason);

        stats.put("channelCount", build.channelCount());
        stats.put("producerCount", build.producerCount());
        stats.put("consumerCount", build.consumerCount());
        stats.put("actionCount", build.actionCount());
        stats.put("downstreamChannelCount", build.countEdges("emits_downstream"));
        stats.put("signalJoinCount", build.countByType("signal_join"));
        stats.put("timerCount", build.countByType("timer"));
        stats.put("stateActionCount", build.countByType("state_action"));
        stats.put("stateVariableCount", build.countByType("state_variable"));
        stats.put("conditionGateCount", build.countByType("condition_gate"));
        stats.put("actionGateCount", build.countByType("action_gate"));
        stats.put("primaryNodeCount", build.countByNodeKind("primary"));
        stats.put("referenceNodeCount", build.countByNodeKind("reference"));
        stats.put("edgeMergeCount", build.edgeMergeCount);
        stats.put("disabledNodeCount", build.disabledCount());
        stats.put("maxDepth", safeDepth);
        stats.put("maxGraphNodes", MAX_GRAPH_NODES);
        stats.put("maxGraphEdges", MAX_GRAPH_EDGES);
        stats.put("maxComponentChannels", MAX_COMPONENT_CHANNELS);
        stats.put("maxJoinInputPorts", MAX_JOIN_INPUT_PORTS);
        stats.put("maxComponentMetadataRows", MAX_COMPONENT_METADATA_ROWS);
        stats.put("componentLimit", MAX_COMPONENT_CHANNELS);
        stats.put("nodesTruncated", build.nodesTruncated);
        stats.put("edgesTruncated", build.edgesTruncated);
        stats.put("componentTruncated", componentTruncated);
        stats.put("componentTruncationReason", build.componentTruncationReason);
        stats.put("readOnly", true);
        stats.put("segmentModel", "component-aware");
        stats.put("logicChainModel", "component-aware-connected-subgraph");
        stats.put("rootChannelRole", "focus");
        stats.put("focusChannel", safeFocus);
        stats.put("focusComponentId", build.focusComponentId());
        stats.put("componentView", true);
        stats.put("componentCount", safeFocus.isBlank() ? 0 : 1);
        stats.put("componentChannelCount", build.channelCount());
        stats.put("componentStrongEdgeCount", build.associationEdgeCount("strong"));
        stats.put("componentWeakEdgeCount", build.associationEdgeCount("weak"));
        stats.put("componentSummary", componentSummary);
        stats.put("displayNameResolver", true);
        stats.put("displayNamePriority", "webadmin_metadata,runtime_name,channel_metadata,channel,position_or_short_id,fallback");
        stats.put("graphModelVersion", "v2-join-layout");
        stats.put("edgeDedupeEnabled", true);
        stats.put("pathColorGroups", List.of("signal", "join", "gate", "timer", "state", "reference"));
        stats.put("strongEdgeRules", List.of("channel_consume_produce", "signal_action_output", "join_input_output", "timer_output", "timer_action_reference", "state_action_write", "condition_gate_guard"));
        stats.put("weakEdgeRules", List.of("shared_state_variable_readers", "shared_condition_group_bindings", "high_fan_in_timer_references", "large_unrelated_consumer_fanout"));
        stats.put("enhancementStage", "8.13");
        stats.put("noCrossChannelLongLineMixing", true);
        return stats;
    }

    private void buildComponent(GraphBuild build, String rawRootChannel) {
        String rootChannel = SignalChannel.normalize(rawRootChannel);
        if (rootChannel.isBlank()) {
            return;
        }
        build.focusChannel = rootChannel;
        Deque<ComponentQueueItem> queue = new ArrayDeque<>();
        Set<String> queued = new LinkedHashSet<>();
        queue.add(new ComponentQueueItem(rootChannel, 0, "focus"));
        queued.add(rootChannel);
        while (!queue.isEmpty()) {
            if (build.isTruncated()) {
                build.markComponentTruncated("图规模达到安全上限，关联组件已局部展示。");
                break;
            }
            ComponentQueueItem item = queue.removeFirst();
            String channel = SignalChannel.normalize(item.channel());
            if (channel.isBlank()) {
                continue;
            }
            if (item.depth() > build.maxDepth) {
                build.markComponentTruncated("关联组件超过最大展开深度 " + build.maxDepth + "，部分关联频道已折叠。");
                continue;
            }
            buildSegment(build, channel, item.depth(), new LinkedHashSet<>());
            if (build.isTruncated()) {
                build.markComponentTruncated("图规模达到安全上限，关联组件已局部展示。");
                break;
            }
            for (String related : componentRelatedChannels(build, channel)) {
                if (related.isBlank() || queued.contains(related)) {
                    continue;
                }
                if (queued.size() >= MAX_COMPONENT_CHANNELS) {
                    build.markComponentTruncated("关联组件频道数量超过 " + MAX_COMPONENT_CHANNELS + "，后续关联已折叠。");
                    break;
                }
                int nextDepth = item.depth() + 1;
                if (nextDepth > build.maxDepth) {
                    build.markComponentTruncated("关联组件超过最大展开深度 " + build.maxDepth + "，频道 " + related + " 已折叠。");
                    continue;
                }
                queued.add(related);
                queue.addLast(new ComponentQueueItem(related, nextDepth, "strong"));
            }
        }
    }

    private LinkedHashSet<String> componentRelatedChannels(GraphBuild build, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        LinkedHashSet<String> related = new LinkedHashSet<>();
        if (channel.isBlank()) {
            return related;
        }
        for (SignalJoinDefinition raw : build.snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            String output = SignalChannel.normalize(join.outputChannel);
            JoinInputSelection inputSelection = selectJoinInputChannels(join.inputChannels, channel, MAX_JOIN_INPUT_PORTS);
            boolean touches = output.equals(channel) || inputSelection.containsFocus();
            if (!touches || !includeNode(build, join.enabled)) {
                continue;
            }
            if (!output.isBlank()) {
                related.add(output);
            }
            if (inputSelection.truncated()) {
                build.markComponentTruncated("Signal Join " + join.id + " 输入数量超过 " + MAX_JOIN_INPUT_PORTS + "，其余输入关联已折叠。");
            }
            for (String input : inputSelection.visibleChannels()) {
                related.add(input);
            }
        }
        for (SignalListenerData raw : build.snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            if (!includeNode(build, listener.enabled())) {
                continue;
            }
            collectActionRelatedChannels(build, related, channel, listener.channel(), listener.actions());
        }
        for (RegionControllerData raw : build.snapshot.regions) {
            RegionControllerData region = raw.normalized();
            if (!includeNode(build, region.enabled())) {
                continue;
            }
            collectActionRelatedChannels(build, related, channel, "", region.enterActions());
            collectActionRelatedChannels(build, related, channel, "", region.exitActions());
            collectActionRelatedChannels(build, related, channel, "", region.stayActions());
        }
        related.remove(channel);
        return related;
    }

    private static JoinInputSelection selectJoinInputChannels(List<SignalJoinInputDefinition> inputChannels, String focusChannel, int limit) {
        String focus = SignalChannel.normalize(focusChannel);
        int safeLimit = Math.max(0, limit);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        LinkedHashSet<String> visible = new LinkedHashSet<>();
        boolean containsFocus = false;
        for (SignalJoinInputDefinition raw : inputChannels == null ? List.<SignalJoinInputDefinition>of() : inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            String channel = SignalChannel.normalize(input.channel);
            if (channel.isBlank() || !seen.add(channel)) {
                continue;
            }
            boolean focusInput = !focus.isBlank() && channel.equals(focus);
            if (focusInput) {
                containsFocus = true;
            }
            if (visible.size() < safeLimit || focusInput) {
                visible.add(channel);
            }
        }
        return new JoinInputSelection(List.copyOf(visible), seen.size(), containsFocus, seen.size() > visible.size());
    }

    private static boolean joinReferencesInput(SignalJoinDefinition join, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        if (join == null || channel.isBlank()) {
            return false;
        }
        for (SignalJoinInputDefinition raw : join.inputChannels == null ? List.<SignalJoinInputDefinition>of() : join.inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            if (channel.equals(SignalChannel.normalize(input.channel))) {
                return true;
            }
        }
        return false;
    }

    private void collectActionRelatedChannels(GraphBuild build, LinkedHashSet<String> related, String focusChannel, String rawOwnerChannel, List<ActionConfig> actions) {
        String ownerChannel = SignalChannel.normalize(rawOwnerChannel);
        for (ActionConfig raw : actions == null ? List.<ActionConfig>of() : actions) {
            ActionConfig action = raw == null ? null : raw.normalized();
            if (action == null || !includeNode(build, action.enabled() && action.isUsable())) {
                continue;
            }
            if (action.type() == ActionType.SIGNAL) {
                String output = SignalChannel.normalize(action.value());
                if (ownerChannel.equals(focusChannel) && !output.isBlank()) {
                    related.add(output);
                }
                if (output.equals(focusChannel) && !ownerChannel.isBlank()) {
                    related.add(ownerChannel);
                }
            }
            if (action.isTimerAction()) {
                TimerDefinition timer = findTimer(build.snapshot, TimerStore.normalizeId(action.timerId()));
                String output = timer == null ? "" : SignalChannel.normalize(timer.outputChannel);
                if (ownerChannel.equals(focusChannel) && !output.isBlank()) {
                    related.add(output);
                }
                if (output.equals(focusChannel) && !ownerChannel.isBlank()) {
                    related.add(ownerChannel);
                }
            }
        }
    }

    private void buildSegment(GraphBuild build, String rawChannel, int depth, LinkedHashSet<String> visiting) {
        String channel = SignalChannel.normalize(rawChannel);
        if (channel.isBlank()) {
            return;
        }
        if (build.isTruncated()) {
            return;
        }
        if (!build.visitedChannels.contains(channel) && build.visitedChannels.size() >= MAX_COMPONENT_CHANNELS) {
            build.markComponentTruncated("关联组件频道数量超过 " + MAX_COMPONENT_CHANNELS + "，频道 " + channel + " 已折叠。");
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto("segment:" + safeNodeId(channel) + ":component-limit", channel, depth, false, "component_truncated", List.of(), List.of(), List.of(), List.of(), List.of("关联组件频道数量达到安全上限，已停止继续展开。")));
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
        if (TRUNCATED_NODE_ID.equals(channelNode) || build.nodesTruncated) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                    segmentId,
                    channel,
                    depth,
                    false,
                    "truncated",
                    List.of(channelNode),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("图谱节点数量达到安全上限，已停止继续展开该频道。")
            ));
            visiting.remove(channel);
            return;
        }
        List<String> producers = producersFor(build, channel);
        if (build.nodesTruncated) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                    segmentId,
                    channel,
                    depth,
                    false,
                    "truncated",
                    List.copyOf(producers),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("图谱节点数量达到安全上限，已停止继续展开该频道。")
            ));
            visiting.remove(channel);
            return;
        }
        List<String> consumers = consumersFor(build, channel);
        if (build.nodesTruncated) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                    segmentId,
                    channel,
                    depth,
                    false,
                    "truncated",
                    List.copyOf(producers),
                    List.copyOf(consumers),
                    List.of(),
                    List.of(),
                    List.of("图谱节点数量达到安全上限，已停止继续展开该频道。")
            ));
            visiting.remove(channel);
            return;
        }
        List<String> actions = actionsFor(build, channel, consumers);
        if (build.isTruncated()) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                    segmentId,
                    channel,
                    depth,
                    false,
                    "truncated",
                    List.copyOf(producers),
                    List.copyOf(consumers),
                    List.copyOf(actions),
                    List.of(),
                    List.of("图谱达到安全上限，已停止继续展开该频道动作。")
            ));
            visiting.remove(channel);
            return;
        }
        LinkedHashSet<String> downstream = new LinkedHashSet<>();
        for (String actionNode : actions) {
            if (build.isTruncated()) {
                break;
            }
            WebAdminDtos.LogicChainNodeDto action = build.nodes.get(actionNode);
            Object raw = action == null ? "" : action.metadata().get("downstreamChannel");
            String downstreamChannel = SignalChannel.normalize(raw instanceof String text ? text : "");
            if (!downstreamChannel.isBlank()) {
                String downstreamNode = addDownstreamChannelNode(build, downstreamChannel, depth + 1);
                downstream.add(downstreamNode);
                addEdge(build, actionNode, downstreamNode, "emits_downstream", "下游频道", "dashed");
            }
        }
        for (String consumerNode : consumers) {
            if (build.isTruncated()) {
                break;
            }
            WebAdminDtos.LogicChainNodeDto node = build.nodes.get(consumerNode);
            if (node != null && ("signal_join".equals(node.type()) || "signal_join".equals(node.refType()))) {
                String downstreamChannel = SignalChannel.normalize(String.valueOf(node.metadata().getOrDefault("outputChannel", "")));
                if (!downstreamChannel.isBlank()) {
                    String downstreamNode = addChannelNode(build, downstreamChannel);
                    downstream.add(downstreamNode);
                    addEdge(build, consumerNode, downstreamNode, "join_output", "汇合输出", "solid", Map.of(
                            "joinOutputRole", "primary",
                            "downstreamPrimaryNode", downstreamNode,
                            "nonTraversalReference", false
                    ));
                }
                continue;
            }
            Object raw = node == null ? "" : node.metadata().get("downstreamChannel");
            String downstreamChannel = SignalChannel.normalize(raw instanceof String text ? text : "");
            if (!downstreamChannel.isBlank()) {
                String downstreamNode = addDownstreamChannelNode(build, downstreamChannel, depth + 1);
                downstream.add(downstreamNode);
                addEdge(build, consumerNode, downstreamNode, "join_output", "汇合输出", "dashed");
            }
        }
        if (build.isTruncated()) {
            build.segments.add(new WebAdminDtos.LogicChainSegmentDto(
                    segmentId,
                    channel,
                    depth,
                    false,
                    "truncated",
                    List.copyOf(producers),
                    List.copyOf(consumers),
                    List.copyOf(actions),
                    List.copyOf(downstream),
                    List.of("图谱达到安全上限，后续下游已折叠。")
            ));
            visiting.remove(channel);
            return;
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
                    LogicNodeDisplayName display = deviceDisplayName(build, device, channel);
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("field", ref.field());
                    metadata.put("sourceType", device.type());
                    metadata.put("displayNameSource", display.source());
                    metadata.put("fallbackReason", display.fallbackReason());
                    metadata.put("technicalSubtitle", display.subtitle());
                    result.add(addNode(build, id, "producer", "device", device.id(), display.title(), ref.label() + " · " + display.subtitle(), channel, enabled, "#/devices/" + encode(device.id()), metadata));
                    addEdge(build, id, "channel:" + channel, "emits", ref.label(), "solid");
                }
            }
        }
        for (SignalListenerData raw : build.snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            addSignalActionProducers(build, result, channel, listener.actions(), "listener", listener.id(), listener.name(), listener.enabled(), listener.conditionGroupId(), "#/listeners/" + encode(listener.id()), listener.channel());
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
            boolean enabled = join.enabled;
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addSignalJoinNode(build, join, "", "producer"));
        }
        for (TimerDefinition raw : build.snapshot.timers) {
            TimerDefinition timer = raw.normalized();
            if (!SignalChannel.normalize(timer.outputChannel).equals(channel)) {
                continue;
            }
            boolean enabled = timer.enabled;
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addTimerNode(build, timer, channel, "producer"));
            addEdge(build, "timer:" + safeNodeId(timer.id), "channel:" + channel, "timer_outputs_channel", "Timer outputChannel", "solid");
        }
        return List.copyOf(result);
    }

    private String addSignalJoinNode(GraphBuild build, SignalJoinDefinition rawJoin, String inputChannel, String graphRole) {
        SignalJoinDefinition join = rawJoin == null ? new SignalJoinDefinition().normalized() : rawJoin.normalized();
        String input = SignalChannel.normalize(inputChannel);
        String id = signalJoinNodeId(join.id);
        boolean enabled = join.enabled;
        String subtitle = "producer".equals(graphRole)
                ? "Signal Join 输出 · " + join.mode.displayName()
                : join.mode.displayName() + " → " + join.outputChannel;
        String nodeId = addNode(
                build,
                id,
                "signal_join",
                "signal_join",
                join.id,
                joinName(join),
                subtitle,
                input.isBlank() ? SignalChannel.normalize(join.outputChannel) : input,
                enabled,
                "#/signal-joins/" + encode(join.id),
                joinMetadata(build, join, input, graphRole)
        );
        addSignalJoinInputEdges(build, join, input);
        addSignalJoinOutputEdge(build, join);
        return nodeId;
    }

    private void addSignalJoinInputEdges(GraphBuild build, SignalJoinDefinition join, String inputChannel) {
        String joinNode = signalJoinNodeId(join.id);
        String primaryInput = SignalChannel.normalize(inputChannel);
        JoinInputSelection inputSelection = selectJoinInputChannels(join.inputChannels, primaryInput, MAX_JOIN_INPUT_PORTS);
        Set<String> visibleInputs = new LinkedHashSet<>(inputSelection.visibleChannels());
        if (inputSelection.truncated()) {
            build.markComponentTruncated("Signal Join " + join.id + " 输入数量超过 " + MAX_JOIN_INPUT_PORTS + "，其余输入已折叠。");
        }
        int index = 0;
        int visibleIndex = 0;
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (SignalJoinInputDefinition raw : join.inputChannels == null ? List.<SignalJoinInputDefinition>of() : join.inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            String channel = SignalChannel.normalize(input.channel);
            if (channel.isBlank() || !seen.add(channel)) {
                index++;
                continue;
            }
            if (!visibleInputs.contains(channel)) {
                index++;
                continue;
            }
            boolean primary = !primaryInput.isBlank() && channel.equals(primaryInput);
            String inputNode = addChannelNode(build, channel);
            if (hasEdge(build, inputNode, joinNode, "join_input")) {
                index++;
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("joinId", join.id);
            metadata.put("joinInputRole", primary ? "primary" : "related");
            metadata.put("primaryInput", primary);
            metadata.put("relatedInput", !primary);
            metadata.put("inputChannel", channel);
            metadata.put("inputNodeId", inputNode);
            metadata.put("joinNodeId", joinNode);
            metadata.put("portIndex", visibleIndex);
            metadata.put("rawPortIndex", index);
            metadata.put("visualStyle", primary ? "join-primary" : "join-related-dashed");
            metadata.put("dataLogicChainJoinPrimaryInputEdge", primary);
            metadata.put("dataLogicChainJoinRelatedInputEdge", !primary);
            addEdge(
                    build,
                    inputNode,
                    joinNode,
                    "join_input",
                    primary ? "当前 Join 输入" : "其他 Join 输入",
                    primary ? "solid" : "dashed",
                    metadata
            );
            visibleIndex++;
            index++;
        }
    }

    private static boolean hasEdge(GraphBuild build, String from, String to, String type) {
        if (build == null) {
            return false;
        }
        for (WebAdminDtos.LogicChainEdgeDto edge : build.edges) {
            if (safe(from).equals(edge.from()) && safe(to).equals(edge.to()) && safe(type).equals(edge.type())) {
                return true;
            }
        }
        return false;
    }

    private void addSignalJoinOutputEdge(GraphBuild build, SignalJoinDefinition join) {
        String outputChannel = SignalChannel.normalize(join.outputChannel);
        if (outputChannel.isBlank()) {
            return;
        }
        String outputNode = addChannelNode(build, outputChannel);
        addEdge(build, signalJoinNodeId(join.id), outputNode, "join_output", "汇合输出", "solid", Map.of(
                "joinId", join.id,
                "joinOutputRole", "primary",
                "outputChannel", outputChannel,
                "outputChannelNodeId", outputNode,
                "downstreamPrimaryNode", outputNode,
                "visualStyle", "join-output"
        ));
    }

    private static int metadataPortIndex(SignalJoinDefinition join, String channel) {
        int index = 0;
        for (SignalJoinInputDefinition raw : join.inputChannels == null ? List.<SignalJoinInputDefinition>of() : join.inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            if (SignalChannel.normalize(input.channel).equals(SignalChannel.normalize(channel))) {
                return index;
            }
            index++;
        }
        return -1;
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
            String ownerConditionGroupId,
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
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("actionIndex", i);
            metadata.put("ownerChannel", safe(ownerChannel));
            metadata.put("ownerType", refType);
            metadata.put("ownerId", refId);
            metadata.put("actionType", "signal");
            metadata.put("downstreamChannel", channel);
            metadata.put("nodeKind", "reference");
            metadata.put("primaryNodeId", "action:" + refType + ":" + safeNodeId(refId) + ":" + i);
            metadata.put("canonicalNodeId", "action:" + refType + ":" + safeNodeId(refId) + ":" + i);
            metadata.put("aliasKind", "signal_action_producer");
            metadata.put("referenceReason", "signal_action_producer_alias");
            String visibleNode = addNode(build, id, "producer", refType, refId, safe(refName).isBlank() ? refId : refName, "signal action #" + (i + 1) + " · " + safe(ownerChannel), channel, enabled, detailRoute, metadata);
            boolean gatePathLinked = false;
            String listGate = WebAdminConditionGroupStore.normalizeId(ownerConditionGroupId);
            if (!listGate.isBlank()) {
                ConditionRuntimeTargetType parentType = parentTargetTypeForOwner(refType);
                String gateNode = addGateNode(
                        build,
                        listGateNodeId(refType, refId, ""),
                        "condition_gate",
                        "列表条件 gate",
                        listGate,
                        parentType,
                        refId,
                        "LIST",
                        null,
                        "",
                        -1,
                        "",
                        channel,
                        detailRoute
                );
                visibleNode = gateNode;
            }
            String actionGate = WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
            if (!actionGate.isBlank()) {
                ConditionRuntimeTargetType actionTargetType = actionTargetTypeForOwner(refType);
                ConditionRuntimeTargetType parentType = parentTargetTypeForOwner(refType);
                String targetId = actionTargetIdForOwner(refType, refId, i);
                String gateNode = addGateNode(
                        build,
                        actionGateNodeId(refType, refId, "", i),
                        "action_gate",
                        "单条 Action gate",
                        actionGate,
                        actionTargetType,
                        targetId,
                        "ACTION",
                        parentType,
                        refId,
                        i,
                        action.type().id(),
                        channel,
                        detailRoute
                );
                if (!visibleNode.equals(id)) {
                    addEdge(build, visibleNode, gateNode, "gate_guards", "通过后进入单条 gate", "solid");
                }
                addEdge(build, gateNode, id, "gate_guards", "通过后发出频道", "solid");
                gatePathLinked = true;
                visibleNode = gateNode;
            }
            if (!visibleNode.equals(id) && !gatePathLinked) {
                addEdge(build, visibleNode, id, "gate_guards", "通过后执行 signal action", "solid");
            }
            result.add(visibleNode);
            addEdge(build, id, "channel:" + channel, "emits", "signal action", "solid");
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
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("triggerType", trigger.name());
            metadata.put("regionId", region.regionId());
            metadata.put("actionIndex", i);
            metadata.put("actionType", "signal");
            metadata.put("downstreamChannel", channel);
            metadata.put("nodeKind", "reference");
            metadata.put("primaryNodeId", "action:region_controller:" + safeNodeId(region.id()) + ":" + trigger.name().toLowerCase(Locale.ROOT) + ":" + i);
            metadata.put("canonicalNodeId", "action:region_controller:" + safeNodeId(region.id()) + ":" + trigger.name().toLowerCase(Locale.ROOT) + ":" + i);
            metadata.put("aliasKind", "signal_action_producer");
            metadata.put("referenceReason", "region_signal_action_producer_alias");
            String detailRoute = "#/region-controllers/" + encode(region.id());
            String visibleNode = addNode(build, id, "producer", "region_controller", region.id(), region.name().isBlank() ? region.id() : region.name(), triggerLabel(trigger) + " signal action #" + (i + 1), channel, enabled, detailRoute, metadata);
            boolean gatePathLinked = false;
            String bucket = trigger.name().toLowerCase(Locale.ROOT);
            String listGate = WebAdminConditionGroupStore.normalizeId(regionConditionGroupId(region, trigger));
            if (!listGate.isBlank()) {
                String gateNode = addGateNode(
                        build,
                        listGateNodeId("region", region.id(), bucket),
                        "condition_gate",
                        triggerLabel(trigger) + "列表 gate",
                        listGate,
                        regionParentTargetType(trigger),
                        region.id(),
                        "LIST",
                        null,
                        "",
                        -1,
                        "",
                        channel,
                        detailRoute
                );
                visibleNode = gateNode;
            }
            String actionGate = WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
            if (!actionGate.isBlank()) {
                String targetId = ConditionActionGateService.regionActionTargetId(region.id(), bucket, i);
                String gateNode = addGateNode(
                        build,
                        actionGateNodeId("region", region.id(), bucket, i),
                        "action_gate",
                        triggerLabel(trigger) + "单条 Action gate",
                        actionGate,
                        ConditionActionGateService.regionActionTargetType(bucket),
                        targetId,
                        "ACTION",
                        regionParentTargetType(trigger),
                        region.id(),
                        i,
                        action.type().id(),
                        channel,
                        detailRoute
                );
                if (!visibleNode.equals(id)) {
                    addEdge(build, visibleNode, gateNode, "gate_guards", "通过后进入单条 gate", "solid");
                }
                addEdge(build, gateNode, id, "gate_guards", "通过后发出频道", "solid");
                gatePathLinked = true;
                visibleNode = gateNode;
            }
            if (!visibleNode.equals(id) && !gatePathLinked) {
                addEdge(build, visibleNode, id, "gate_guards", "通过后执行区域 signal action", "solid");
            }
            result.add(visibleNode);
            addEdge(build, id, "channel:" + channel, "emits", triggerLabel(trigger), "solid");
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
            String detailRoute = "#/listeners/" + encode(listener.id());
            addNode(build, id, "consumer", "listener", listener.id(), listener.name().isBlank() ? listener.id() : listener.name(), listener.actions().size() + " 个动作", channel, listener.enabled(), detailRoute, Map.of("actionCount", listener.actions().size(), "cooldownTicks", listener.cooldownTicks()));
            String conditionGroupId = WebAdminConditionGroupStore.normalizeId(listener.conditionGroupId());
            if (!conditionGroupId.isBlank()) {
                String gateNode = addGateNode(build, listGateNodeId("listener", listener.id(), ""), "condition_gate", "监听器列表 gate", conditionGroupId, ConditionRuntimeTargetType.SIGNAL_LISTENER, listener.id(), "LIST", null, "", -1, "", channel, detailRoute);
                result.add(gateNode);
                addEdge(build, "channel:" + channel, gateNode, "consumes", "列表条件 gate", "solid");
                addEdge(build, gateNode, id, "gate_guards", "通过后进入监听器", "solid");
            } else {
                result.add(id);
                addEdge(build, "channel:" + channel, id, "consumes", "并列消费者", "solid");
            }
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
                LogicNodeDisplayName display = deviceDisplayName(build, device, channel);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("pulseTicks", device.pulseTicks());
                metadata.put("pos", posMap(device));
                metadata.put("displayNameSource", display.source());
                metadata.put("fallbackReason", display.fallbackReason());
                result.add(addNode(build, id, "consumer", "signal_receiver", device.id(), display.title(), "红石脉冲接收器 · " + display.subtitle(), channel, device.enabled(), "#/devices/" + encode(device.id()), metadata));
                addEdge(build, "channel:" + channel, id, "consumes", "接收器", "solid");
            } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                int actionCount = device.actionCount();
                String id = "consumer:action_relay:" + safeNodeId(device.id());
                if (!includeNode(build, device.enabled())) {
                    continue;
                }
                String detailRoute = "#/devices/" + encode(device.id());
                LogicNodeDisplayName display = deviceDisplayName(build, device, channel);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("actionCount", actionCount);
                metadata.put("loaded", false);
                metadata.put("actionRelayActionsSummaryOnly", true);
                metadata.put("gateVisibility", "snapshot_summary_only");
                metadata.put("readOnlySnapshotOnly", true);
                metadata.put("pos", posMap(device));
                metadata.put("displayNameSource", display.source());
                metadata.put("fallbackReason", display.fallbackReason());
                addNode(build, id, "consumer", "action_relay", device.id(), display.title(), actionCount + " 个动作 · " + display.subtitle(), channel, device.enabled(), detailRoute, metadata);
                result.add(id);
                addEdge(build, "channel:" + channel, id, "consumes", "动作继电器", "solid");
            }
        }
        for (SignalJoinDefinition raw : build.snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            if (!joinReferencesInput(join, channel)) {
                continue;
            }
            boolean enabled = join.enabled;
            if (!includeNode(build, enabled)) {
                continue;
            }
            result.add(addSignalJoinNode(build, join, channel, "consumer"));
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
            if (device.actionCount() > 0) {
                build.warnings.add("动作继电器 " + deviceDisplayName(build, device, channel).title() + " 未加载，无法展开其动作列表；动作详情存放在方块实体中，8.13 只读图谱不直接读取 live world，已保留摘要节点。");
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
            metadata.put("actionDisplayIndex", i + 1);
            metadata.put("ownerType", ownerType);
            metadata.put("ownerId", ownerId);
            metadata.put("ownerName", safe(ownerName));
            metadata.put("ownerChannel", ownerChannel);
            metadata.put("actionType", WebAdminReadonlySupport.actionType(action));
            metadata.put("summary", WebAdminReadonlySupport.actionSummary(action));
            metadata.put("downstreamChannel", downstream);
            metadata.put("readOnly", true);
            if (action != null && action.isStateVariableAction()) {
                metadata.putAll(stateActionMetadata(build, action));
            }
            if (action != null && action.isTimerAction()) {
                metadata.putAll(timerActionMetadata(build, action));
            }
            boolean enabled = ownerEnabled && action != null && action.enabled() && action.isUsable();
            if (!includeNode(build, enabled)) {
                continue;
            }
            String conditionGroupId = WebAdminConditionGroupStore.normalizeId(action == null ? "" : action.conditionGroupId());
            String gateId = conditionGroupId.isBlank() ? "" : actionGateNodeId(ownerType, ownerId, "", i);
            metadata.put("conditionGroupId", conditionGroupId);
            metadata.put("conditionGateNodeId", gateId);
            String nodeType = actionNodeType(action);
            String refType = actionRefType(action);
            result.add(addNode(build, id, nodeType, refType, ownerType + ":" + ownerId + ":" + i, "#" + (i + 1) + " " + labelAction(action), WebAdminReadonlySupport.actionSummary(action), ownerChannel, enabled, detailRoute, metadata));
            if (!conditionGroupId.isBlank()) {
                ConditionRuntimeTargetType actionTargetType = actionTargetTypeForOwner(ownerType);
                ConditionRuntimeTargetType parentTargetType = parentTargetTypeForOwner(ownerType);
                String actionTargetId = actionTargetIdForOwner(ownerType, ownerId, i);
                String gateNode = addGateNode(
                        build,
                        gateId,
                        "action_gate",
                        "单条 Action gate",
                        conditionGroupId,
                        actionTargetType,
                        actionTargetId,
                        "ACTION",
                        parentTargetType,
                        ownerId,
                        i,
                        action == null || action.type() == null ? "" : action.type().id(),
                        ownerChannel,
                        detailRoute
                );
                addEdge(build, consumerNode, gateNode, "gate_guards", "动作 #" + (i + 1) + " 条件", "solid");
                addEdge(build, gateNode, id, "executes", "通过后执行动作 #" + (i + 1), "solid");
            } else {
                addEdge(build, consumerNode, id, "executes", "动作 #" + (i + 1), "solid");
            }
            if (action != null && action.isStateVariableAction()) {
                addStateVariableReference(build, id, action);
            }
            if (action != null && action.isTimerAction()) {
                addTimerActionReference(build, id, action);
            }
        }
    }

    private static boolean includeNode(GraphBuild build, boolean enabled) {
        return build.includeDisabled || enabled;
    }

    private static LogicNodeDisplayName deviceDisplayName(GraphBuild build, SignalDeviceData device, String channel) {
        SignalDeviceData safeDevice = device == null
                ? new SignalDeviceData("", SignalDeviceData.TYPE_SIGNAL_EMITTER, "", "", 0, 0, 0, "", true, 0, 0, 0, 0, 0L, 0L, 0L, 0L, "", "", "", "", false, 0)
                : device.normalized();
        WebAdminDeviceMetadataStore.MetadataEntry metadata = deviceMetadataEntry(build, safeDevice);
        if (metadata != null && !safe(metadata.displayName).isBlank()) {
            return new LogicNodeDisplayName(metadata.displayName, deviceTechnicalSubtitle(safeDevice, channel), "device_metadata.displayName", "");
        }
        if (!safe(safeDevice.name()).isBlank()) {
            return new LogicNodeDisplayName(safeDevice.name(), deviceTechnicalSubtitle(safeDevice, channel), "device.name", "");
        }
        LogicNodeDisplayName channelName = channelDisplayName(build, channel, "");
        if (!SignalChannel.normalize(channel).isBlank() && !"channel".equals(channelName.source())) {
            return new LogicNodeDisplayName(channelName.title(), deviceTechnicalSubtitle(safeDevice, channel), "channel_metadata.displayName", "device_name_blank");
        }
        String safeChannel = SignalChannel.normalize(channel);
        if (!safeChannel.isBlank()) {
            return new LogicNodeDisplayName(safeChannel, deviceTechnicalSubtitle(safeDevice, channel), "channel", "device_name_blank");
        }
        String pos = devicePositionLabel(safeDevice);
        if (!pos.isBlank()) {
            return new LogicNodeDisplayName(pos, deviceTechnicalSubtitle(safeDevice, channel), "device.position", "device_name_blank");
        }
        String shortId = SignalDeviceStore.shortId(safeDevice.id());
        if (!shortId.isBlank()) {
            return new LogicNodeDisplayName(shortId, deviceTechnicalSubtitle(safeDevice, channel), "device.shortId", "device_name_blank");
        }
        return new LogicNodeDisplayName(WebAdminReadonlySupport.deviceDisplayName(safeDevice), deviceTechnicalSubtitle(safeDevice, channel), "fallback", "no_display_name_or_identity");
    }

    private static LogicNodeDisplayName channelDisplayName(GraphBuild build, String channel, String fallbackSubtitle) {
        String safeChannel = SignalChannel.normalize(channel);
        WebAdminChannelMetadataStore.MetadataEntry metadata = channelMetadataEntry(build, safeChannel);
        if (metadata != null && !safe(metadata.displayName).isBlank()) {
            return new LogicNodeDisplayName(metadata.displayName, "频道 " + safeChannel, "channel_metadata.displayName", "");
        }
        return new LogicNodeDisplayName(safeChannel.isBlank() ? "未解析频道" : safeChannel, safe(fallbackSubtitle).isBlank() ? "频道 " + safeChannel : safe(fallbackSubtitle) + " · " + safeChannel, "channel", safeChannel.isBlank() ? "channel_blank" : "");
    }

    private static WebAdminDeviceMetadataStore.MetadataEntry deviceMetadataEntry(GraphBuild build, SignalDeviceData device) {
        if (build == null || build.snapshot == null || build.snapshot.deviceMetadata == null || device == null) {
            return null;
        }
        for (String key : WebAdminDeviceMetadataStore.metadataKeys(device.id(), device.type())) {
            WebAdminDeviceMetadataStore.MetadataEntry entry = build.snapshot.deviceMetadata.devices.get(key);
            if (entry != null) {
                return WebAdminDeviceMetadataStore.MetadataEntry.normalized(key, entry);
            }
        }
        return null;
    }

    private static WebAdminChannelMetadataStore.MetadataEntry channelMetadataEntry(GraphBuild build, String channel) {
        if (build == null || build.snapshot == null || build.snapshot.channelMetadata == null) {
            return null;
        }
        String safeChannel = SignalChannel.normalize(channel);
        WebAdminChannelMetadataStore.MetadataEntry entry = build.snapshot.channelMetadata.channels.get(safeChannel);
        return entry == null ? null : WebAdminChannelMetadataStore.MetadataEntry.normalized(safeChannel, entry);
    }

    private static String deviceTechnicalSubtitle(SignalDeviceData device, String channel) {
        String id = SignalDeviceStore.shortId(device.id());
        String pos = devicePositionLabel(device);
        String safeChannel = SignalChannel.normalize(channel);
        List<String> parts = new ArrayList<>();
        if (!safeChannel.isBlank()) {
            parts.add("频道 " + safeChannel);
        }
        if (!id.isBlank()) {
            parts.add("ID " + id);
        }
        if (!pos.isBlank()) {
            parts.add(pos);
        }
        return parts.isEmpty() ? "设备来源" : String.join(" · ", parts);
    }

    private static String devicePositionLabel(SignalDeviceData device) {
        if (device == null || safe(device.dimension()).isBlank()) {
            return "";
        }
        return safe(device.dimension()) + " " + device.x() + " " + device.y() + " " + device.z();
    }

    private String addChannelNode(GraphBuild build, String channel) {
        String id = "channel:" + channel;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("parallelConsumers", true);
        metadata.put("readOnly", true);
        metadata.put("nodeKind", "primary");
        metadata.put("primaryNodeId", id);
        metadata.put("canonicalNodeId", id);
        metadata.put("graphModelVersion", "v2-join-layout");
        LogicNodeDisplayName display = channelDisplayName(build, channel, "当前频道段");
        metadata.put("displayNameSource", display.source());
        metadata.put("technicalSubtitle", display.subtitle());
        return addNode(build, id, "channel", "channel", channel, display.title(), display.subtitle(), channel, true, "#/signals/" + encode(channel), metadata);
    }

    private String addDownstreamChannelNode(GraphBuild build, String channel, int depth) {
        String primaryId = "channel:" + SignalChannel.normalize(channel);
        String id = "reference:channel:" + safeNodeId(channel);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("expandsAsSegment", true);
        metadata.put("depth", depth);
        metadata.put("nodeKind", "reference");
        metadata.put("primaryNodeId", primaryId);
        metadata.put("canonicalNodeId", primaryId);
        metadata.put("referenceReason", "downstream_merge");
        metadata.put("isReferenceCard", true);
        metadata.put("graphModelVersion", "v2-join-layout");
        metadata.put("visualOnly", true);
        metadata.put("nonTraversal", true);
        metadata.put("visualLane", "reference");
        LogicNodeDisplayName display = channelDisplayName(build, channel, depth > build.maxDepth ? "频道引用：还有下游，超出当前深度" : "频道引用：点击可展开下一频道段");
        metadata.put("displayNameSource", display.source());
        metadata.put("technicalSubtitle", display.subtitle());
        return addNode(build, id, "downstream_channel", "channel", channel, display.title(), display.subtitle(), channel, true, "#/logic-chains/resolve?rootType=channel&rootRef=" + encode(channel), metadata);
    }

    private String addTimerNode(GraphBuild build, TimerDefinition timer, String channel, String graphRole) {
        TimerDefinition safeTimer = timer == null ? new TimerDefinition() : timer.normalized();
        String id = "timer:" + safeNodeId(safeTimer.id);
        String safeChannel = SignalChannel.normalize(channel).isBlank() ? SignalChannel.normalize(safeTimer.outputChannel) : SignalChannel.normalize(channel);
        String subtitle = "producer".equals(graphRole)
                ? "Timer 完成输出 · " + safeTimer.mode.displayName()
                : "Timer 引用 · " + safeTimer.mode.displayName();
        return addNode(
                build,
                id,
                "timer",
                "timer",
                safeTimer.id,
                timerName(safeTimer),
                subtitle,
                safeChannel,
                safeTimer.enabled,
                "#/timers/" + encode(safeTimer.id),
                timerMetadata(build, safeTimer, graphRole)
        );
    }

    private String addMissingTimerNode(GraphBuild build, String timerId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "timer");
        metadata.put("nodeType", "TIMER");
        metadata.put("graphRole", "missing_reference");
        metadata.put("timerId", TimerStore.normalizeId(timerId));
        metadata.put("targetExists", false);
        metadata.put("runtimeStatus", "MISSING");
        metadata.put("readOnly", true);
        metadata.put("nodeKind", "reference");
        metadata.put("primaryNodeId", "timer:" + safeNodeId(timerId));
        metadata.put("canonicalNodeId", "timer:" + safeNodeId(timerId));
        metadata.put("referenceReason", "missing_timer_target");
        metadata.put("isReferenceCard", true);
        return addNode(
                build,
                "reference:timer:" + safeNodeId(timerId),
                "timer",
                "timer",
                timerId,
                safe(timerId).isBlank() ? "缺失 Timer" : timerId,
                "Timer 引用不存在 / 已删除",
                "",
                false,
                "",
                metadata
        );
    }

    private String addGateNode(
            GraphBuild build,
            String id,
            String nodeType,
            String label,
            String conditionGroupId,
            ConditionRuntimeTargetType targetType,
            String targetId,
            String gateLevel,
            ConditionRuntimeTargetType parentTargetType,
            String parentTargetId,
            int actionIndex,
            String actionType,
            String channel,
            String ownerDetailRoute
    ) {
        String groupId = WebAdminConditionGroupStore.normalizeId(conditionGroupId);
        Map<String, Object> metadata = gateMetadata(
                nodeType,
                groupId,
                targetType,
                targetId,
                gateLevel,
                parentTargetType,
                parentTargetId,
                actionIndex,
                actionType,
                channel,
                ownerDetailRoute
        );
        String subtitle = ("ACTION".equals(safe(gateLevel)) ? "单条 Action gate" : "整组列表 gate") + " · " + groupId;
        return addNode(
                build,
                id,
                nodeType,
                nodeType,
                groupId,
                label,
                subtitle,
                channel,
                true,
                groupId.isBlank() ? "" : "#/condition-groups/" + encode(groupId),
                metadata
        );
    }

    private void addStateVariableReference(GraphBuild build, String actionNodeId, ActionConfig action) {
        StateVariableReference reference = resolveStateVariableCurrent(build == null ? null : build.snapshot, stateVariableReference(action));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "state_variable");
        metadata.put("nodeType", "STATE_VARIABLE");
        metadata.put("graphRole", "write_target");
        metadata.put("readOnly", true);
        metadata.put("resolvable", reference.resolvable());
        metadata.put("unresolvedReason", reference.unresolvedReason());
        metadata.put("stateVariableId", reference.stableId());
        metadata.put("displayPath", reference.displayPath());
        metadata.put("stateScope", reference.scope() == null ? "" : reference.scope().name());
        metadata.put("stateScopeLabel", reference.scope() == null ? "" : reference.scope().displayName());
        metadata.put("stateTargetId", reference.targetId());
        metadata.put("stateKey", reference.key());
        metadata.put("detailRoute", reference.detailRoute());
        reference.current().ifPresent(record -> {
            metadata.put("currentValueVisible", true);
            metadata.put("currentValue", record.value());
            metadata.put("currentType", record.type().name());
            metadata.put("currentTypeLabel", record.type().displayName());
            metadata.put("currentVersion", record.version());
            metadata.put("currentUpdatedAt", WebAdminReadonlySupport.isoTime(record.updatedAt()));
        });
        if (build.snapshot.stateVariables.degraded()) {
            metadata.put("snapshotDegraded", true);
            metadata.put("snapshotMessage", build.snapshot.stateVariables.message());
        }
        if (!reference.resolvable()) {
            String primaryId = reference.stableId().isBlank()
                    ? "state_variable:dynamic:" + safeNodeId(actionNodeId)
                    : "state_variable:" + safeNodeId(reference.stableId());
            metadata.put("nodeKind", "reference");
            metadata.put("primaryNodeId", primaryId);
            metadata.put("canonicalNodeId", primaryId);
            metadata.put("referenceReason", "dynamic_state_variable_target");
            metadata.put("isReferenceCard", true);
        }
        String id = reference.resolvable()
                ? "state_variable:" + safeNodeId(reference.stableId())
                : "reference:state_variable:unresolved:" + safeNodeId(actionNodeId);
        String label = reference.resolvable() ? reference.displayPath() : "动态状态变量目标";
        String subtitle = reference.resolvable()
                ? (reference.current().isPresent() ? "当前值可见 · " + reference.current().get().type().displayName() : "当前未创建 / 无可见记录")
                : reference.unresolvedReason();
        addNode(build, id, "state_variable", "state_variable", reference.stableId(), label, subtitle, "", true, reference.detailRoute(), metadata);
        addEdge(build, actionNodeId, id, "state_writes", "写入状态变量", reference.resolvable() ? "solid" : "dashed");
    }

    private void addTimerActionReference(GraphBuild build, String actionNodeId, ActionConfig action) {
        String timerId = TimerStore.normalizeId(action == null ? "" : action.timerId());
        TimerDefinition timer = findTimer(build.snapshot, timerId);
        String timerNode = timer == null ? addMissingTimerNode(build, timerId) : addTimerNode(build, timer, timer.outputChannel, "referenced_by_action");
        boolean start = action != null && action.type() == ActionType.TIMER_START;
        addEdge(build, actionNodeId, timerNode, start ? "action_starts_timer" : "action_cancels_timer", start ? "启动 Timer" : "取消 Timer", "dashed");
    }

    private void addEdge(GraphBuild build, String from, String to, String type, String label, String style) {
        addEdge(build, from, to, type, label, style, Map.of());
    }

    private void addEdge(GraphBuild build, String from, String to, String type, String label, String style, Map<String, Object> extraMetadata) {
        if (build == null || safe(from).isBlank() || safe(to).isBlank()) {
            return;
        }
        String edgeKey = safe(from) + "\u001f" + safe(to) + "\u001f" + safe(type) + "\u001f" + safe(label);
        if (!build.edgeKeys.add(edgeKey)) {
            build.edgeMergeCount++;
            return;
        }
        if (build.edges.size() >= MAX_GRAPH_EDGES) {
            if (!build.edgesTruncated) {
                build.edgesTruncated = true;
                build.warnings.add("逻辑链图边数量超过 " + MAX_GRAPH_EDGES + "，后续边已截断。");
            }
            return;
        }
        String pathGroupId = pathGroupForEdge(type);
        String visualStyle = visualStyleForEdge(type, style);
        if (extraMetadata != null) {
            Object requestedPathGroup = extraMetadata.get("pathGroupId");
            Object requestedVisualStyle = extraMetadata.get("visualStyle");
            if (requestedPathGroup instanceof String text && !text.isBlank()) {
                pathGroupId = text;
            }
            if (requestedVisualStyle instanceof String text && !text.isBlank()) {
                visualStyle = text;
            }
        }
        boolean referenceEdge = isReferenceEdge(build, from, to, type);
        if (referenceEdge) {
            pathGroupId = "reference";
            visualStyle = "reference";
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dedupeKey", edgeKey);
        metadata.put("mergedDuplicateCount", 0);
        metadata.put("readOnly", true);
        metadata.put("graphModelVersion", "v2-join-layout");
        metadata.put("componentId", build.focusComponentId());
        metadata.put("associationStrength", "strong");
        metadata.put("componentTraversal", true);
        metadata.put("rootChannelRole", "focus");
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }
        if (referenceEdge) {
            metadata.putIfAbsent("visualOnly", true);
            metadata.putIfAbsent("nonTraversal", true);
            metadata.put("associationStrength", "weak");
            metadata.put("componentTraversal", false);
        }
        build.edges.add(new WebAdminDtos.LogicChainEdgeDto(from, to, type, label, style, pathGroupId, visualStyle, referenceEdge, Map.copyOf(metadata)));
    }

    private static String pathGroupForEdge(String type) {
        return switch (safe(type)) {
            case "join_input", "join_output" -> "join";
            case "gate_guards" -> "gate";
            case "timer_outputs_channel", "action_starts_timer", "action_cancels_timer" -> "timer";
            case "state_writes" -> "state";
            case "executes" -> "execution";
            case "emits_downstream" -> "downstream";
            case "consumes" -> "consumer";
            default -> "signal";
        };
    }

    private static String visualStyleForEdge(String type, String style) {
        String group = pathGroupForEdge(type);
        if ("dashed".equals(safe(style))) {
            return group + "-dashed";
        }
        return group;
    }

    private static boolean isReferenceEdge(GraphBuild build, String from, String to, String type) {
        if ("reference_to_primary".equals(safe(type))) {
            return true;
        }
        WebAdminDtos.LogicChainNodeDto fromNode = build.nodes.get(from);
        WebAdminDtos.LogicChainNodeDto toNode = build.nodes.get(to);
        return isReferenceNode(fromNode) || isReferenceNode(toNode);
    }

    private static boolean isReferenceNode(WebAdminDtos.LogicChainNodeDto node) {
        if (node == null || node.metadata() == null) {
            return false;
        }
        return "reference".equals(String.valueOf(node.metadata().get("nodeKind")))
                || Boolean.TRUE.equals(node.metadata().get("isReferenceCard"));
    }

    private static String actionNodeType(ActionConfig action) {
        if (action == null) {
            return "action";
        }
        if (action.isStateVariableAction()) {
            return "state_action";
        }
        if (action.isTimerAction()) {
            return "timer_action";
        }
        return "action";
    }

    private static String actionRefType(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "action";
        }
        if (action.type() == ActionType.STATE_VARIABLE) {
            return "state_variable_action";
        }
        if (action.type() == ActionType.TIMER_START) {
            return "timer_start";
        }
        if (action.type() == ActionType.TIMER_CANCEL) {
            return "timer_cancel";
        }
        return "action";
    }

    private static Map<String, Object> stateActionMetadata(GraphBuild build, ActionConfig action) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (action == null) {
            return metadata;
        }
        StateVariableReference reference = resolveStateVariableCurrent(build.snapshot, stateVariableReference(action));
        metadata.put("kind", "state_action");
        metadata.put("nodeType", "STATE_ACTION");
        metadata.put("stateOperation", safe(action.stateOperation()));
        metadata.put("stateOperationLabel", StateVariableMutationLabel.operation(action.stateOperation()));
        metadata.put("stateScope", safe(action.stateScope()));
        metadata.put("stateScopeLabel", StateVariableMutationLabel.scope(action.stateScope()));
        metadata.put("stateTargetMode", safe(action.stateTargetMode()));
        metadata.put("stateTargetModeLabel", StateVariableMutationLabel.targetMode(action.stateTargetMode()));
        metadata.put("stateTargetId", safe(action.stateTargetId()));
        metadata.put("stateKey", safe(action.stateKey()));
        metadata.put("stateValueType", safe(action.stateValueType()));
        metadata.put("stateValueTypeLabel", StateVariableMutationLabel.type(action.stateValueType()));
        metadata.put("stateDelta", action.stateDelta());
        metadata.put("stateCreateIfMissing", action.stateCreateIfMissing());
        metadata.put("stateSummary", action.stateActionSummary());
        metadata.put("stateVariableResolvable", reference.resolvable());
        metadata.put("stateVariableId", reference.stableId());
        metadata.put("stateVariableDisplayPath", reference.displayPath());
        metadata.put("stateVariableRoute", reference.detailRoute());
        metadata.put("stateVariableUnresolvedReason", reference.unresolvedReason());
        if (build != null && build.snapshot.stateVariables.degraded()) {
            metadata.put("stateVariableSnapshotDegraded", true);
            metadata.put("stateVariableSnapshotMessage", build.snapshot.stateVariables.message());
        }
        reference.current().ifPresent(record -> {
            metadata.put("stateVariableCurrentValue", record.value());
            metadata.put("stateVariableCurrentType", record.type().name());
            metadata.put("stateVariableCurrentTypeLabel", record.type().displayName());
            metadata.put("stateVariableCurrentVersion", record.version());
        });
        return metadata;
    }

    private static Map<String, Object> timerActionMetadata(GraphBuild build, ActionConfig action) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (action == null) {
            return metadata;
        }
        String timerId = TimerStore.normalizeId(action.timerId());
        TimerDefinition timer = build == null ? null : findTimer(build.snapshot, timerId);
        metadata.put("kind", "timer_action");
        metadata.put("nodeType", "TIMER_ACTION");
        metadata.put("timerOperation", action.type() == ActionType.TIMER_CANCEL ? "timer_cancel" : "timer_start");
        metadata.put("timerId", timerId);
        metadata.put("timerTargetMode", safe(action.timerTargetMode()));
        metadata.put("timerTargetId", safe(action.timerTargetId()));
        metadata.put("timerSummary", action.timerActionSummary());
        metadata.put("timerRoute", timerId.isBlank() ? "" : "#/timers/" + encode(timerId));
        metadata.put("targetExists", timer != null);
        if (timer != null) {
            TimerDefinition safeTimer = timer.normalized();
            metadata.put("targetEnabled", safeTimer.enabled);
            metadata.put("targetMode", safeTimer.mode.name());
            metadata.put("targetModeLabel", safeTimer.mode.displayName());
            metadata.put("targetScopeMode", safeTimer.scopeMode.name());
            metadata.put("targetScopeLabel", safeTimer.scopeMode.displayName());
            metadata.put("targetOutputChannel", safeTimer.outputChannel);
        }
        return metadata;
    }

    private static Map<String, Object> gateMetadata(
            String nodeType,
            String conditionGroupId,
            ConditionRuntimeTargetType targetType,
            String targetId,
            String gateLevel,
            ConditionRuntimeTargetType parentTargetType,
            String parentTargetId,
            int actionIndex,
            String actionType,
            String channel,
            String ownerDetailRoute
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String groupId = WebAdminConditionGroupStore.normalizeId(conditionGroupId);
        String safeTargetId = safe(targetId);
        Map<String, Object> recent = targetType == null || safeTargetId.isBlank()
                ? WebAdminConditionGateHistoryService.recentStatus((ConditionRuntimeTargetType) null, "")
                : WebAdminConditionGateHistoryService.recentStatus(targetType, safeTargetId);
        metadata.put("kind", nodeType);
        metadata.put("nodeType", "action_gate".equals(nodeType) ? "ACTION_GATE" : "CONDITION_GATE");
        metadata.put("conditionGroupId", groupId);
        metadata.put("conditionGroupRoute", groupId.isBlank() ? "" : "#/condition-groups/" + encode(groupId));
        metadata.put("targetType", targetType == null ? "" : targetType.id());
        metadata.put("targetTypeLabel", targetType == null ? "" : targetType.displayName());
        metadata.put("targetId", safeTargetId);
        metadata.put("gateLevel", safe(gateLevel));
        metadata.put("parentTargetType", parentTargetType == null ? "" : parentTargetType.id());
        metadata.put("parentTargetLabel", parentTargetType == null ? "" : parentTargetType.displayName());
        metadata.put("parentTargetId", safe(parentTargetId));
        metadata.put("actionIndex", actionIndex);
        metadata.put("actionDisplayIndex", actionIndex < 0 ? 0 : actionIndex + 1);
        metadata.put("actionType", safe(actionType));
        metadata.put("channel", SignalChannel.normalize(channel));
        metadata.put("ownerDetailRoute", safe(ownerDetailRoute));
        metadata.put("recentConditionGate", recent);
        metadata.put("runtimeStatus", recent.getOrDefault("status", "UNCONFIGURED_OR_NO_HISTORY"));
        metadata.put("debuggerRoute", recent.getOrDefault("debuggerRoute", "#/condition-debugger"));
        metadata.put("readOnly", true);
        return metadata;
    }

    private static StateVariableReference stateVariableReference(ActionConfig action) {
        StateVariableScope scope = StateVariableScope.parse(action == null ? "" : action.stateScope()).orElse(null);
        StateVariableTargetMode targetMode = StateVariableTargetMode.parse(action == null ? "" : action.stateTargetMode()).orElse(null);
        String key = action == null ? "" : safe(action.stateKey());
        if (scope == null || key.isBlank()) {
            return StateVariableReference.unresolved("状态变量作用域或 key 未配置。");
        }
        String targetId = "";
        if (scope == StateVariableScope.PLAYER) {
            if (targetMode != StateVariableTargetMode.EXPLICIT_TARGET || safe(action.stateTargetId()).isBlank()) {
                return StateVariableReference.unresolved("PLAYER + context_player 需要运行时玩家，Viewer 无法静态解析具体变量。");
            }
            targetId = action.stateTargetId();
        }
        StateVariableKey variableKey = new StateVariableKey(scope, targetId, key);
        String route = "#/state-variables/" + encode(variableKey.stableId());
        return StateVariableReference.resolved(variableKey.stableId(), variableKey.displayPath(), route, scope, targetId, key);
    }

    private static StateVariableReference resolveStateVariableCurrent(Snapshot snapshot, StateVariableReference reference) {
        if (snapshot == null || reference == null || !reference.resolvable()) {
            return reference;
        }
        return reference.withCurrent(snapshot.stateVariables.snapshot().get(reference.scope(), reference.targetId(), reference.key()));
    }

    private static TimerDefinition findTimer(Snapshot snapshot, String timerId) {
        String id = TimerStore.normalizeId(timerId);
        if (snapshot == null || id.isBlank()) {
            return null;
        }
        for (TimerDefinition raw : snapshot.timers) {
            TimerDefinition timer = raw.normalized();
            if (timer.id.equals(id)) {
                return timer;
            }
        }
        return null;
    }

    private static ConditionRuntimeTargetType parentTargetTypeForOwner(String ownerType) {
        return switch (safe(ownerType).toLowerCase(Locale.ROOT)) {
            case "action_relay" -> ConditionRuntimeTargetType.ACTION_RELAY;
            case "region_enter" -> ConditionRuntimeTargetType.REGION_ENTER;
            case "region_exit" -> ConditionRuntimeTargetType.REGION_EXIT;
            case "region_stay" -> ConditionRuntimeTargetType.REGION_STAY;
            default -> ConditionRuntimeTargetType.SIGNAL_LISTENER;
        };
    }

    private static ConditionRuntimeTargetType actionTargetTypeForOwner(String ownerType) {
        return switch (safe(ownerType).toLowerCase(Locale.ROOT)) {
            case "action_relay" -> ConditionRuntimeTargetType.ACTION_RELAY_ACTION;
            case "region_enter" -> ConditionRuntimeTargetType.REGION_ENTER_ACTION;
            case "region_exit" -> ConditionRuntimeTargetType.REGION_EXIT_ACTION;
            case "region_stay" -> ConditionRuntimeTargetType.REGION_STAY_ACTION;
            default -> ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION;
        };
    }

    private static String actionTargetIdForOwner(String ownerType, String ownerId, int actionIndex) {
        if ("action_relay".equals(safe(ownerType).toLowerCase(Locale.ROOT))) {
            return ConditionActionGateService.actionTargetId("relay", ownerId, actionIndex);
        }
        return ConditionActionGateService.actionTargetId("listener", ownerId, actionIndex);
    }

    private static String regionConditionGroupId(RegionControllerData region, RegionTriggerType trigger) {
        if (region == null || trigger == null) {
            return "";
        }
        return switch (trigger) {
            case ENTER -> region.enterConditionGroupId();
            case EXIT -> region.exitConditionGroupId();
            case STAY -> region.stayConditionGroupId();
        };
    }

    private static ConditionRuntimeTargetType regionParentTargetType(RegionTriggerType trigger) {
        return switch (trigger == null ? RegionTriggerType.STAY : trigger) {
            case ENTER -> ConditionRuntimeTargetType.REGION_ENTER;
            case EXIT -> ConditionRuntimeTargetType.REGION_EXIT;
            case STAY -> ConditionRuntimeTargetType.REGION_STAY;
        };
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
        if (!build.nodes.containsKey(id) && build.nodes.size() >= MAX_GRAPH_NODES) {
            if (!build.nodesTruncated) {
                build.nodesTruncated = true;
                build.warnings.add("逻辑链图节点数量超过 " + MAX_GRAPH_NODES + "，后续节点已用截断摘要表示。");
            }
            id = TRUNCATED_NODE_ID;
            type = "external";
            refType = "graph_truncated";
            refId = "graph_truncated";
            label = "图谱已截断";
            subtitle = "节点数量超过安全限制，请缩小 root 或降低展开深度。";
            channel = "";
            enabled = true;
            detailRoute = "";
            metadata = Map.of("kind", "graph_truncated", "nodeLimit", MAX_GRAPH_NODES, "readOnly", true);
        }
        Map<String, Object> enrichedMetadata = graphNodeMetadata(id, type, metadata);
        enrichedMetadata.putIfAbsent("componentId", build.focusComponentId());
        enrichedMetadata.putIfAbsent("focusComponentId", build.focusComponentId());
        enrichedMetadata.putIfAbsent("componentView", true);
        enrichedMetadata.putIfAbsent("rootChannelRole", "focus");
        if (!build.focusChannel.isBlank()) {
            enrichedMetadata.putIfAbsent("focusChannel", build.focusChannel);
            if (SignalChannel.normalize(channel).equals(build.focusChannel)) {
                enrichedMetadata.put("isFocusChannel", true);
                enrichedMetadata.put("componentRole", "focus_channel");
            }
        }
        WebAdminDtos.LogicChainNodeDto existing = build.nodes.get(id);
        if (existing == null) {
            build.nodes.put(id, new WebAdminDtos.LogicChainNodeDto(
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
                    Map.copyOf(enrichedMetadata)
            ));
        } else {
            Map<String, Object> merged = new LinkedHashMap<>(enrichedMetadata);
            merged.putAll(existing.metadata());
            merged.put("duplicateGraphNodeMerged", true);
            Object oldRole = existing.metadata().get("graphRole");
            Object newRole = enrichedMetadata.get("graphRole");
            if (oldRole != null && newRole != null && !String.valueOf(oldRole).equals(String.valueOf(newRole))) {
                merged.put("graphRoleAliases", List.of(String.valueOf(oldRole), String.valueOf(newRole)));
            }
            build.nodes.put(id, new WebAdminDtos.LogicChainNodeDto(
                    existing.id(),
                    existing.type(),
                    existing.refType(),
                    existing.refId(),
                    existing.label(),
                    existing.subtitle(),
                    existing.channel(),
                    existing.enabled(),
                    existing.status(),
                    existing.doctorStatus(),
                    existing.lastEvent(),
                    existing.detailRoute(),
                    Map.copyOf(merged)
            ));
        }
        return id;
    }

    private static Map<String, Object> graphNodeMetadata(String id, String type, Map<String, Object> metadata) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (metadata != null) {
            result.putAll(metadata);
        }
        result.putIfAbsent("nodeKind", "primary");
        result.putIfAbsent("primaryNodeId", id);
        result.putIfAbsent("canonicalNodeId", id);
        result.putIfAbsent("graphModelVersion", "v2-join-layout");
        result.putIfAbsent("visualLane", pathGroupForNodeType(type));
        if ("reference".equals(String.valueOf(result.get("nodeKind")))) {
            result.put("isReferenceCard", true);
            result.putIfAbsent("visualOnly", true);
            result.putIfAbsent("nonTraversal", true);
        } else {
            result.put("isPrimaryNode", true);
        }
        return result;
    }

    private static String pathGroupForNodeType(String type) {
        return switch (safe(type)) {
            case "signal_join" -> "join";
            case "timer", "timer_action" -> "timer";
            case "state_action", "state_variable" -> "state";
            case "condition_gate", "action_gate" -> "gate";
            case "downstream_channel" -> "reference";
            case "action" -> "execution";
            case "consumer" -> "consumer";
            default -> "signal";
        };
    }

    private GraphStats summarize(Snapshot snapshot, String rootChannel, boolean includeDisabled, int maxDepth) {
        if (rootChannel == null || rootChannel.isBlank()) {
            return new GraphStats(0, 0, 0, 0, 0, 0, "WARNING", "");
        }
        GraphBuild build = new GraphBuild(snapshot, includeDisabled, Math.max(1, Math.min(HARD_MAX_DEPTH, maxDepth <= 0 ? DEFAULT_MAX_DEPTH : maxDepth)));
        buildComponent(build, rootChannel);
        return new GraphStats(
                build.channelCount(),
                build.producerCount(),
                build.consumerCount(),
                build.actionCount(),
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
                SignalJoinStore.getSnapshot(server),
                TimerStore.getSnapshot(server),
                stateVariablesWithStatus(server),
                server == null ? new WebAdminDeviceMetadataStore.MetadataFile() : WebAdminDeviceMetadataStore.load(server),
                server == null ? new WebAdminChannelMetadataStore.MetadataFile() : WebAdminChannelMetadataStore.load(server)
        );
    }

    private static StateVariableLoadResult stateVariablesWithStatus(MinecraftServer server) {
        if (server == null) {
            return new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "服务器上下文不可用，无法读取状态变量快照。", false);
        }
        return StateVariableStore.getSnapshotWithStatus(server);
    }

    private LinkedHashSet<String> knownChannels(Snapshot snapshot) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (SignalDeviceData raw : snapshot.devices) {
            SignalDeviceData device = raw.normalized();
            for (ChannelRef ref : deviceChannelRefs(device)) {
                if (!addKnownChannel(channels, ref.channel())) {
                    return channels;
                }
            }
        }
        for (SignalListenerData raw : snapshot.listeners) {
            SignalListenerData listener = raw.normalized();
            if (!addKnownChannel(channels, listener.channel()) || !addKnownActionChannels(channels, listener.actions())) {
                return channels;
            }
        }
        for (RegionControllerData raw : snapshot.regions) {
            RegionControllerData region = raw.normalized();
            if (!addKnownActionChannels(channels, region.enterActions())
                    || !addKnownActionChannels(channels, region.exitActions())
                    || !addKnownActionChannels(channels, region.stayActions())) {
                return channels;
            }
        }
        for (SignalJoinDefinition raw : snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            if (!addKnownChannel(channels, join.outputChannel)) {
                return channels;
            }
            int remaining = Math.max(0, MAX_CHAIN_INDEX_CHANNELS - channels.size());
            for (String input : boundedJoinInputChannels(join.inputChannels, remaining)) {
                if (!addKnownChannel(channels, input)) {
                    return channels;
                }
            }
        }
        for (TimerDefinition raw : snapshot.timers) {
            if (!addKnownChannel(channels, raw.normalized().outputChannel)) {
                return channels;
            }
        }
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            if (!addKnownChannel(channels, record.channel())) {
                return channels;
            }
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
            if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type()) && device.actionCount() > 0) {
                children.putIfAbsent(SignalChannel.normalize(device.channel()), new LinkedHashSet<>());
            }
        }
        for (SignalJoinDefinition raw : snapshot.joins) {
            SignalJoinDefinition join = raw.normalized();
            for (String input : boundedJoinInputChannels(join.inputChannels, MAX_CHAIN_INDEX_CHANNELS)) {
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
        String upstreamNodeId = signalJoinNodeId(join.id);
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
        if ("timer".equals(type)) {
            for (TimerDefinition raw : snapshot.timers) {
                TimerDefinition timer = raw.normalized();
                if (timer.id.equals(ref) || timer.displayName.equals(ref)) {
                    return SignalChannel.normalize(timer.outputChannel);
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

    private static boolean addKnownActionChannels(Set<String> channels, List<ActionConfig> actions) {
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action != null && action.type() == ActionType.SIGNAL && !addKnownChannel(channels, action.value())) {
                return false;
            }
        }
        return true;
    }

    private static boolean addKnownChannel(Set<String> channels, String channel) {
        String normalized = SignalChannel.normalize(channel);
        if (normalized.isBlank() || channels.contains(normalized)) {
            return true;
        }
        if (channels.size() >= MAX_CHAIN_INDEX_CHANNELS) {
            return false;
        }
        channels.add(normalized);
        return true;
    }

    private static List<String> boundedJoinInputChannels(List<SignalJoinInputDefinition> inputChannels, int limit) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit == 0) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SignalJoinInputDefinition raw : inputChannels == null ? List.<SignalJoinInputDefinition>of() : inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            String channel = SignalChannel.normalize(input.channel);
            if (!channel.isBlank()) {
                result.add(channel);
            }
            if (result.size() >= safeLimit) {
                break;
            }
        }
        return List.copyOf(result);
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
        if ("timer".equals(type)) {
            for (TimerDefinition raw : snapshot.timers) {
                TimerDefinition timer = raw.normalized();
                if (timer.id.equals(ref) || timer.displayName.equals(ref)) {
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
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join", "timer" -> value;
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
            case STATE_VARIABLE -> "状态变量";
            case TIMER_START -> "启动 Timer";
            case TIMER_CANCEL -> "取消 Timer";
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

    private static String timerName(TimerDefinition timer) {
        if (timer == null) {
            return "Timer";
        }
        return timer.displayName.isBlank() ? timer.id : timer.displayName;
    }

    private static Map<String, Object> joinMetadata(GraphBuild build, SignalJoinDefinition join, String inputChannel, String graphRole) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "signal_join");
        metadata.put("nodeType", "SIGNAL_JOIN");
        metadata.put("graphRole", safe(graphRole));
        metadata.put("nodeKind", "primary");
        metadata.put("primaryNodeId", signalJoinNodeId(join.id));
        metadata.put("canonicalNodeId", signalJoinNodeId(join.id));
        metadata.put("graphModelVersion", "v2-join-layout");
        metadata.put("visualLane", "join");
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
        SignalJoinStatusSnapshot status = joinStatus(build, join);
        String primaryInput = SignalChannel.normalize(inputChannel);
        JoinInputSummary inputSummary = joinInputSummary(join.inputChannels, primaryInput, status);
        List<Map<String, Object>> inputPorts = inputSummary.rows();
        boolean inputPortsTruncated = inputSummary.truncated();
        if (inputPortsTruncated && build != null) {
            build.markComponentTruncated("Signal Join " + join.id + " 输入数量超过 " + MAX_JOIN_INPUT_PORTS + "，详情仅展示前 " + MAX_JOIN_INPUT_PORTS + " 个输入和当前焦点输入。");
        }
        List<String> relatedInputs = inputPorts.stream()
                .map(row -> SignalChannel.normalize(String.valueOf(row.get("channel"))))
                .filter(channel -> !channel.isBlank())
                .filter(channel -> primaryInput.isBlank() || !channel.equals(primaryInput))
                .toList();
        String outputChannel = SignalChannel.normalize(join.outputChannel);
        String outputNodeId = outputChannel.isBlank() ? "" : "channel:" + outputChannel;
        metadata.put("inputChannels", inputSummary.visibleChannels());
        metadata.put("totalInputChannelCount", inputSummary.totalDistinctCount());
        metadata.put("inputPortsTruncated", inputPortsTruncated);
        metadata.put("omittedInputPortCount", Math.max(0, inputSummary.totalDistinctCount() - inputPorts.size()));
        metadata.put("inputPorts", inputPorts);
        metadata.put("joinInputPorts", inputPorts);
        metadata.put("inputPortCount", inputPorts.size());
        metadata.put("primaryInput", primaryInput);
        metadata.put("relatedInputs", relatedInputs);
        metadata.put("inputChannel", primaryInput);
        metadata.put("outputChannel", outputChannel);
        metadata.put("outputPort", outputChannel);
        metadata.put("outputChannelNodeId", outputNodeId);
        metadata.put("downstreamPrimaryNode", outputNodeId);
        metadata.put("upstreamGroup", "join-inputs:" + safeNodeId(join.id));
        metadata.put("joinTraversalPolicy", "no_recursive_downstream_copy");
        metadata.put("canAppearAsProducer", true);
        metadata.put("canAppearAsConsumer", true);
        metadata.put("joinOutputPrimaryNodeId", outputNodeId);
        metadata.put("currentRootIsInput", inputSummary.containsCurrentInput());
        metadata.put("allUpstreamInputChannelsVisible", !inputPortsTruncated);
        metadata.put("joinLayoutV2", true);
        metadata.put("upstreamSummaryModel", false);
        metadata.put("noCrossChannelLongLineMixing", true);
        metadata.put("joinDetailRoute", "#/signal-joins/" + encode(join.id));
        metadata.put("outputChannelRoute", SignalChannel.normalize(join.outputChannel).isBlank() ? "" : "#/signals/" + encode(join.outputChannel));
        metadata.put("pendingScopeCount", status.pendingScopeCount());
        metadata.put("lastResult", status.lastResult());
        metadata.put("lastFailureReason", status.lastFailureReason());
        List<Map<String, Object>> scopes = limitedMetadataRows(status.scopes());
        metadata.put("scopes", scopes);
        metadata.put("scopesTruncated", status.scopes() != null && status.scopes().size() > scopes.size());
        metadata.put("omittedScopeCount", status.scopes() == null ? 0 : Math.max(0, status.scopes().size() - scopes.size()));
        metadata.put("runtimeStatus", status.lastResult().isBlank() ? (status.lastFailureReason().isBlank() ? "NO_HISTORY" : "ERROR") : status.lastResult());
        metadata.put("inputSummary", inputPorts);
        return metadata;
    }

    private static Map<String, Object> timerMetadata(GraphBuild build, TimerDefinition timer, String graphRole) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", "timer");
        metadata.put("nodeType", "TIMER");
        metadata.put("graphRole", "timer");
        metadata.put("graphRoleHint", safe(graphRole));
        metadata.put("canAppearAsProducer", !SignalChannel.normalize(timer.outputChannel).isBlank());
        metadata.put("canBeReferencedByAction", true);
        metadata.put("legacyProducerNodeId", "producer:timer:" + safeNodeId(timer.id));
        metadata.put("timerId", timer.id);
        metadata.put("mode", timer.mode.name());
        metadata.put("modeLabel", timer.mode.displayName());
        metadata.put("scopeMode", timer.scopeMode.name());
        metadata.put("scopeLabel", timer.scopeMode.displayName());
        metadata.put("durationTicks", timer.durationTicks);
        metadata.put("intervalTicks", timer.intervalTicks);
        metadata.put("maxRuns", timer.maxRuns);
        metadata.put("startPolicy", timer.startPolicy.name());
        metadata.put("startPolicyLabel", timer.startPolicy.displayName());
        metadata.put("outputChannel", timer.outputChannel);
        metadata.put("timerDetailRoute", "#/timers/" + encode(timer.id));
        metadata.put("outputChannelRoute", SignalChannel.normalize(timer.outputChannel).isBlank() ? "" : "#/signals/" + encode(timer.outputChannel));
        metadata.put("onStartActionCount", timer.onStartActions.size());
        metadata.put("onTickActionCount", timer.onTickActions.size());
        metadata.put("onCompleteActionCount", timer.onCompleteActions.size());
        metadata.put("onCancelActionCount", timer.onCancelActions.size());
        metadata.put("actionBuckets", timerActionBucketSummaries(timer));
        TimerStatusSnapshot status = timerStatus(build, timer);
        metadata.put("activeInstanceCount", status.activeInstanceCount());
        metadata.put("lastResult", status.lastResult());
        metadata.put("lastFailureReason", status.lastFailureReason());
        List<Map<String, Object>> instances = limitedMetadataRows(status.instances());
        metadata.put("instances", instances);
        metadata.put("instancesTruncated", status.instances() != null && status.instances().size() > instances.size());
        metadata.put("omittedInstanceCount", status.instances() == null ? 0 : Math.max(0, status.instances().size() - instances.size()));
        metadata.put("runtimeStatePersistent", status.runtimeStatePersistent());
        metadata.put("runtimeStatus", status.activeInstanceCount() > 0 ? "ACTIVE" : (status.lastResult().isBlank() && status.lastFailureReason().isBlank() ? "IDLE" : status.lastResult()));
        return metadata;
    }

    private static SignalJoinStatusSnapshot joinStatus(GraphBuild build, SignalJoinDefinition join) {
        SignalJoinDefinition safeJoin = join == null ? new SignalJoinDefinition().normalized() : join.normalized();
        if (build == null) {
            return SignalJoinRuntimeService.statusReadOnly(null, safeJoin, 0L);
        }
        return build.joinStatusCache.computeIfAbsent(safeJoin.id, ignored ->
                SignalJoinRuntimeService.statusReadOnly(build.snapshot.server, safeJoin, currentGameTime(build.snapshot.server)));
    }

    private static TimerStatusSnapshot timerStatus(GraphBuild build, TimerDefinition timer) {
        TimerDefinition safeTimer = timer == null ? new TimerDefinition().normalized() : timer.normalized();
        if (build == null) {
            return TimerRuntimeService.status(null, safeTimer, 0L);
        }
        return build.timerStatusCache.computeIfAbsent(safeTimer.id, ignored ->
                TimerRuntimeService.status(build.snapshot.server, safeTimer, currentGameTime(build.snapshot.server)));
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
    }

    private static JoinInputSummary joinInputSummary(List<SignalJoinInputDefinition> inputChannels, String currentInputChannel, SignalJoinStatusSnapshot status) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> visibleChannels = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int index = 0;
        int visibleCount = 0;
        boolean containsCurrentInput = false;
        String currentInput = SignalChannel.normalize(currentInputChannel);
        for (SignalJoinInputDefinition raw : inputChannels == null ? List.<SignalJoinInputDefinition>of() : inputChannels) {
            SignalJoinInputDefinition input = raw == null ? new SignalJoinInputDefinition() : raw.normalized();
            String channel = SignalChannel.normalize(input.channel);
            boolean current = channel.equals(currentInput);
            if (channel.isBlank()) {
                index++;
                continue;
            }
            if (!seen.add(channel)) {
                if (current) {
                    containsCurrentInput = true;
                }
                index++;
                continue;
            }
            if (current) {
                containsCurrentInput = true;
            }
            if (visibleCount >= MAX_JOIN_INPUT_PORTS && !current) {
                index++;
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("portIndex", index);
            row.put("displayIndex", index + 1);
            row.put("channel", channel);
            row.put("nodeId", "channel:" + channel);
            row.put("inputNodeId", "channel:" + channel);
            row.put("displayName", safe(input.displayName));
            row.put("note", safe(input.note));
            row.put("requiredCount", input.requiredCount);
            row.put("currentRootInput", current);
            row.put("joinInputRole", current ? "primary" : "related");
            row.put("edgeStyle", current ? "solid" : "dashed");
            row.put("visualStyle", current ? "join-primary" : "join-related-dashed");
            row.put("matchedInAnyPendingScope", joinInputMatched(status, channel));
            row.put("signalRoute", "#/signals/" + encode(channel));
            row.put("logicChainRoute", "#/logic-chains/resolve?rootType=channel&rootRef=" + encode(channel));
            row.put("expandableUpstream", true);
            row.put("dataLogicChainJoinInputSummary", true);
            result.add(Map.copyOf(row));
            visibleChannels.add(channel);
            index++;
            visibleCount++;
        }
        return new JoinInputSummary(
                List.copyOf(result),
                List.copyOf(visibleChannels),
                seen.size(),
                containsCurrentInput,
                seen.size() > result.size()
        );
    }

    private static boolean joinInputMatched(SignalJoinStatusSnapshot status, String channel) {
        String safeChannel = SignalChannel.normalize(channel);
        if (status == null || safeChannel.isBlank()) {
            return false;
        }
        for (Map<String, Object> scope : limitedMetadataRows(status.scopes())) {
            Object raw = scope.get("matchedChannels");
            if (raw instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (safeChannel.equals(SignalChannel.normalize(String.valueOf(item)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Map<String, Object>> limitedMetadataRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rows.subList(0, Math.min(MAX_COMPONENT_METADATA_ROWS, rows.size())));
    }

    private static List<Map<String, Object>> timerActionBucketSummaries(TimerDefinition timer) {
        if (timer == null) {
            return List.of();
        }
        TimerDefinition safeTimer = timer.normalized();
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(timerActionBucketSummary(safeTimer, "start", "启动动作", safeTimer.onStartActions));
        result.add(timerActionBucketSummary(safeTimer, "tick", "Tick 动作", safeTimer.onTickActions));
        result.add(timerActionBucketSummary(safeTimer, "complete", "完成动作", safeTimer.onCompleteActions));
        result.add(timerActionBucketSummary(safeTimer, "cancel", "取消动作", safeTimer.onCancelActions));
        return List.copyOf(result);
    }

    private static Map<String, Object> timerActionBucketSummary(TimerDefinition timer, String bucket, String label, List<ActionConfig> actions) {
        List<ActionConfig> safeActions = actions == null ? List.of() : actions;
        int stateCount = 0;
        int signalCount = 0;
        int timerActionCount = 0;
        int gatedCount = 0;
        List<Map<String, Object>> gateSummaries = new ArrayList<>();
        for (int index = 0; index < safeActions.size(); index++) {
            ActionConfig action = safeActions.get(index);
            if (action == null) {
                continue;
            }
            if (action.isStateVariableAction()) {
                stateCount++;
            }
            if (action.type() == ActionType.SIGNAL) {
                signalCount++;
            }
            if (action.isTimerAction()) {
                timerActionCount++;
            }
            String groupId = WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
            if (!groupId.isBlank()) {
                gatedCount++;
                if (gateSummaries.size() >= MAX_COMPONENT_METADATA_ROWS) {
                    continue;
                }
                String targetId = ConditionActionGateService.actionTargetId("timer_" + safe(bucket), timer.id, index);
                Map<String, Object> gate = new LinkedHashMap<>();
                gate.put("conditionGroupId", groupId);
                gate.put("targetType", timerActionTargetType(bucket).id());
                gate.put("targetId", targetId);
                gate.put("actionIndex", index);
                gate.put("actionDisplayIndex", index + 1);
                gate.put("actionType", action.type() == null ? "" : action.type().id());
                gate.put("recentConditionGate", WebAdminConditionGateHistoryService.recentStatus(timerActionTargetType(bucket), targetId));
                gateSummaries.add(Map.copyOf(gate));
            }
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("bucket", bucket);
        row.put("label", label);
        row.put("parentTargetType", timerParentTargetType(bucket).id());
        row.put("actionTargetType", timerActionTargetType(bucket).id());
        row.put("actionCount", safeActions.size());
        row.put("stateActionCount", stateCount);
        row.put("signalActionCount", signalCount);
        row.put("timerActionCount", timerActionCount);
        row.put("gatedActionCount", gatedCount);
        row.put("gateSummaries", gateSummaries);
        row.put("gateSummariesTruncated", gatedCount > gateSummaries.size());
        row.put("omittedGateSummaryCount", Math.max(0, gatedCount - gateSummaries.size()));
        return Map.copyOf(row);
    }

    private static ConditionRuntimeTargetType timerActionTargetType(String bucket) {
        return switch (safe(bucket)) {
            case "start" -> ConditionRuntimeTargetType.TIMER_ON_START_ACTION;
            case "tick" -> ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION;
            case "cancel" -> ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION;
            default -> ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION;
        };
    }

    private static ConditionRuntimeTargetType timerParentTargetType(String bucket) {
        return switch (safe(bucket)) {
            case "start" -> ConditionRuntimeTargetType.TIMER_ON_START;
            case "tick" -> ConditionRuntimeTargetType.TIMER_ON_TICK;
            case "cancel" -> ConditionRuntimeTargetType.TIMER_ON_CANCEL;
            default -> ConditionRuntimeTargetType.TIMER_ON_COMPLETE;
        };
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
        String raw = safe(value);
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_.:-]", "_");
        if (raw.equals(sanitized)) {
            return sanitized;
        }
        return sanitized + "_" + shortNodeHash(raw);
    }

    private static String shortNodeHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(safe(value).getBytes(StandardCharsets.UTF_8)));
            return encoded.substring(0, Math.min(10, encoded.length()));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    private static String signalJoinNodeId(String joinId) {
        return "signal_join:" + safeNodeId(joinId);
    }

    private static String listGateNodeId(String ownerType, String ownerId, String bucket) {
        String safeBucket = safe(bucket);
        return "condition_gate:" + safeNodeId(ownerType) + ":" + safeNodeId(ownerId) + (safeBucket.isBlank() ? "" : ":" + safeNodeId(safeBucket));
    }

    private static String actionGateNodeId(String ownerType, String ownerId, String bucket, int actionIndex) {
        String safeBucket = safe(bucket);
        return "action_gate:" + safeNodeId(ownerType) + ":" + safeNodeId(ownerId) + (safeBucket.isBlank() ? "" : ":" + safeNodeId(safeBucket)) + ":" + actionIndex;
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

    private static final class StateVariableMutationLabel {
        private StateVariableMutationLabel() {
        }

        private static String operation(String value) {
            return StateVariableMutationOperation.parse(value)
                    .map(StateVariableMutationOperation::displayName)
                    .orElse(safe(value));
        }

        private static String scope(String value) {
            return StateVariableScope.parse(value)
                    .map(StateVariableScope::displayName)
                    .orElse(safe(value));
        }

        private static String targetMode(String value) {
            return StateVariableTargetMode.parse(value)
                    .map(StateVariableTargetMode::displayName)
                    .orElse(safe(value));
        }

        private static String type(String value) {
            return StateVariableType.parse(value)
                    .map(StateVariableType::displayName)
                    .orElse(safe(value));
        }
    }

    private record StateVariableReference(
            boolean resolvable,
            String stableId,
            String displayPath,
            String detailRoute,
            StateVariableScope scope,
            String targetId,
            String key,
            String unresolvedReason,
            java.util.Optional<StateVariableRecord> current
    ) {
        private StateVariableReference {
            stableId = safe(stableId);
            displayPath = safe(displayPath);
            detailRoute = safe(detailRoute);
            targetId = safe(targetId);
            key = safe(key);
            unresolvedReason = safe(unresolvedReason);
            current = current == null ? java.util.Optional.empty() : current;
        }

        private static StateVariableReference resolved(String stableId, String displayPath, String detailRoute, StateVariableScope scope, String targetId, String key) {
            return new StateVariableReference(true, stableId, displayPath, detailRoute, scope, targetId, key, "", java.util.Optional.empty());
        }

        private static StateVariableReference unresolved(String reason) {
            return new StateVariableReference(false, "", "", "", null, "", "", reason, java.util.Optional.empty());
        }

        private StateVariableReference withCurrent(java.util.Optional<StateVariableRecord> record) {
            return new StateVariableReference(resolvable, stableId, displayPath, detailRoute, scope, targetId, key, unresolvedReason, record);
        }
    }

    private record Snapshot(
            MinecraftServer server,
            List<SignalDeviceData> devices,
            List<SignalListenerData> listeners,
            List<RegionControllerData> regions,
            List<SignalJoinDefinition> joins,
            List<TimerDefinition> timers,
            StateVariableLoadResult stateVariables,
            WebAdminDeviceMetadataStore.MetadataFile deviceMetadata,
            WebAdminChannelMetadataStore.MetadataFile channelMetadata
    ) {
    }

    private record ChannelRef(String channel, String field, String label, boolean enabled) {
    }

    private record LogicNodeDisplayName(String title, String subtitle, String source, String fallbackReason) {
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

    private record ComponentQueueItem(String channel, int depth, String reason) {
    }

    private record JoinInputSelection(
            List<String> visibleChannels,
            int totalDistinctCount,
            boolean containsFocus,
            boolean truncated
    ) {
    }

    private record JoinInputSummary(
            List<Map<String, Object>> rows,
            List<String> visibleChannels,
            int totalDistinctCount,
            boolean containsCurrentInput,
            boolean truncated
    ) {
    }

    private static final class GraphBuild {
        private final Snapshot snapshot;
        private final boolean includeDisabled;
        private final int maxDepth;
        private final Map<String, WebAdminDtos.LogicChainNodeDto> nodes = new LinkedHashMap<>();
        private final List<WebAdminDtos.LogicChainEdgeDto> edges = new ArrayList<>();
        private final Set<String> edgeKeys = new LinkedHashSet<>();
        private final List<WebAdminDtos.LogicChainSegmentDto> segments = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final Set<String> visitedChannels = new LinkedHashSet<>();
        private final Map<String, SignalJoinStatusSnapshot> joinStatusCache = new LinkedHashMap<>();
        private final Map<String, TimerStatusSnapshot> timerStatusCache = new LinkedHashMap<>();
        private String focusChannel = "";
        private boolean componentChannelsTruncated;
        private String componentTruncationReason = "";
        private int edgeMergeCount;
        private boolean nodesTruncated;
        private boolean edgesTruncated;

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
                if (!isReferenceNode(node) && type.equals(node.type())) {
                    count++;
                }
            }
            return count;
        }

        private int countByNodeKind(String kind) {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (kind.equals(String.valueOf(node.metadata().get("nodeKind")))) {
                    count++;
                }
            }
            return count;
        }

        private int producerCount() {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (!isReferenceNode(node) && ("producer".equals(node.type()) || "producer".equals(String.valueOf(node.metadata().get("graphRole"))))) {
                    count++;
                }
            }
            return count;
        }

        private int consumerCount() {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (!isReferenceNode(node) && ("consumer".equals(node.type()) || "consumer".equals(String.valueOf(node.metadata().get("graphRole"))))) {
                    count++;
                }
            }
            return count;
        }

        private int actionCount() {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (!isReferenceNode(node) && ("action".equals(node.type()) || "state_action".equals(node.type()) || "timer_action".equals(node.type()))) {
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

        private int associationEdgeCount(String strength) {
            int count = 0;
            for (WebAdminDtos.LogicChainEdgeDto edge : edges) {
                if (strength.equals(String.valueOf(edge.metadata().getOrDefault("associationStrength", "strong")))) {
                    count++;
                }
            }
            return count;
        }

        private int disabledCount() {
            int count = 0;
            for (WebAdminDtos.LogicChainNodeDto node : nodes.values()) {
                if (!isReferenceNode(node) && !node.enabled()) {
                    count++;
                }
            }
            return count;
        }

        private boolean isTruncated() {
            return componentChannelsTruncated || nodesTruncated || edgesTruncated;
        }

        private String focusComponentId() {
            return focusChannel.isBlank() ? "" : "component:" + safeNodeId(focusChannel);
        }

        private void markComponentTruncated(String reason) {
            if (!componentChannelsTruncated) {
                componentChannelsTruncated = true;
                componentTruncationReason = safe(reason).isBlank() ? "关联组件达到安全限制，已局部展示。" : reason;
                warnings.add(componentTruncationReason);
            }
        }

        private static boolean isReferenceNode(WebAdminDtos.LogicChainNodeDto node) {
            return node != null && "reference".equals(String.valueOf(node.metadata().get("nodeKind")));
        }
    }
}
