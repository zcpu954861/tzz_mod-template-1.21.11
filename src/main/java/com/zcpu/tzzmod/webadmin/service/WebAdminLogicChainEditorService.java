package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.region.RegionTargetFilter;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.draft.WebAdminProtectedDraftRegistry;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminRegionControllerRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerActionRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest;
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
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSessions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminLogicChainEditorService {
    private static final Set<String> SUPPORTED_NODE_TYPES = Set.of("signal_join", "timer", "signal_listener", "world_device", "virtual_block_device", "region_controller");
    private static final int MAX_DRAFT_NODES_PER_SAVE = 8;
    private static final int MAX_DRAFT_EDGES_PER_SAVE = 16;
    private static final int MAX_JOIN_CYCLE_GUARD_NODES = 256;
    private static final int MAX_JOIN_CYCLE_GUARD_EDGES = 512;
    private static final String NODE_DELETE_CONFIRMATION_TEXT = "我确认删除该节点";

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminLogicChainService logicChainService;
    private final WebAdminSignalJoinService signalJoinService;
    private final WebAdminTimerService timerService;
    private final WebAdminChannelMetadataService channelMetadataService;
    private final WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService;
    private final WebAdminSignalListenerActionsService signalListenerActionsService;
    private final WebAdminSignalListenerLifecycleService signalListenerLifecycleService;
    private final WebAdminActionRelayActionsService actionRelayActionsService;
    private final WebAdminRegionControllerService regionControllerService;
    private final WebAdminVirtualBlockDeviceLifecycleService virtualBlockDeviceLifecycleService;
    private final WebAdminVirtualBlockDeviceNativeTriggerService virtualBlockDeviceNativeTriggerService;
    private final WebAdminDeviceBasicConfigService deviceBasicConfigService;
    private final WebAdminDeviceMetadataService deviceMetadataService;
    private final LogicChainDraftSaveCoordinator saveCoordinator;

    public WebAdminLogicChainEditorService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            WebAdminLogicChainService logicChainService,
            WebAdminSignalJoinService signalJoinService,
            WebAdminTimerService timerService,
            WebAdminChannelMetadataService channelMetadataService,
            WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService,
            WebAdminSignalListenerActionsService signalListenerActionsService,
            WebAdminSignalListenerLifecycleService signalListenerLifecycleService,
            WebAdminActionRelayActionsService actionRelayActionsService,
            WebAdminRegionControllerService regionControllerService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.logicChainService = logicChainService == null ? new WebAdminLogicChainService(this.permissionService, this.securityService, editLockService) : logicChainService;
        this.signalJoinService = signalJoinService == null ? new WebAdminSignalJoinService(this.permissionService, this.securityService, editLockService) : signalJoinService;
        this.timerService = timerService == null ? new WebAdminTimerService(this.permissionService, this.securityService, editLockService) : timerService;
        this.channelMetadataService = channelMetadataService == null ? new WebAdminChannelMetadataService(this.permissionService, this.securityService, editLockService) : channelMetadataService;
        this.signalListenerBasicConfigService = signalListenerBasicConfigService == null ? new WebAdminSignalListenerBasicConfigService(this.permissionService, this.securityService, editLockService) : signalListenerBasicConfigService;
        this.signalListenerActionsService = signalListenerActionsService == null ? new WebAdminSignalListenerActionsService(this.permissionService, this.securityService, editLockService) : signalListenerActionsService;
        this.signalListenerLifecycleService = signalListenerLifecycleService == null ? new WebAdminSignalListenerLifecycleService(this.permissionService, this.securityService, editLockService) : signalListenerLifecycleService;
        this.actionRelayActionsService = actionRelayActionsService == null ? new WebAdminActionRelayActionsService(this.permissionService, this.securityService, editLockService) : actionRelayActionsService;
        this.regionControllerService = regionControllerService == null ? new WebAdminRegionControllerService(this.permissionService, this.securityService, editLockService) : regionControllerService;
        this.virtualBlockDeviceLifecycleService = new WebAdminVirtualBlockDeviceLifecycleService(this.permissionService, this.securityService, editLockService);
        this.virtualBlockDeviceNativeTriggerService = new WebAdminVirtualBlockDeviceNativeTriggerService(this.permissionService, this.securityService, editLockService);
        this.deviceBasicConfigService = new WebAdminDeviceBasicConfigService(this.permissionService, this.securityService, editLockService);
        this.deviceMetadataService = new WebAdminDeviceMetadataService(this.permissionService, this.securityService, editLockService);
        this.saveCoordinator = new LogicChainDraftSaveCoordinator(this);
    }

    public Map<String, Object> capabilities(WebAdminUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", "9.1 Logic Chain / Global Editor capability completion");
        data.put("editMode", true);
        data.put("newNodeOnly", false);
        data.put("existingNodeEditing", true);
        data.put("sameIndexActionEditing", true);
        data.put("localReconnect", true);
        data.put("maxDraftNodesPerSave", MAX_DRAFT_NODES_PER_SAVE);
        data.put("supportedNodeTypes", List.of(
                capability("signal_join", "Signal Join", "创建 SignalJoinDefinition；输入/输出频道写入现有 Signal Join 配置。", List.of("dynamic_downstream_channel_column"), true),
                capability("timer", "Timer", "创建 TimerDefinition；输出频道写入现有 Scheduler Timer 配置；C5 Timer 引用 / 目标位需要 action-list 映射，已 deferred。", List.of("C0"), true),
                capability("signal_listener", "Signal Listener", "从频道下游创建虚拟监听器；频道由画布 consumes 连线推导。", List.of("dynamic_downstream_channel_column"), true),
                capability("world_device", "世界设备引用", "覆盖 SignalEmitter / SignalReceiver / ActionRelay；必须来自客户端辅助放置产生的 protected draft，保存会提交真实世界设备配置并在失败时回滚草稿方块。", List.of("client_assisted_draft", "protected_draft_registry", "typed_commit_rollback_adapter"), false),
                capability("virtual_block_device", "VBD 虚拟方块设备", "复用游戏内 VBD 选择模式，不预填 channel；最终 channel 由画布 vbd_outputs_channel 连线推导，itemSubmit/container 捕获保持 draft payload。", List.of("client_selection", "protected_draft_registry", "draft_only_itemsubmit_container"), false),
                capability("region_controller", "区域控制器", "通过区域选择 protected draft 创建真实 Region + RegionController；输出必须在保存后通过 enter / exit / stay action bucket 管理。", List.of("region_selection", "protected_draft_registry", "typed_commit_rollback_adapter", "action_bucket_output_only"), false),
                capability("channel_metadata", "Channel metadata", "编辑已有频道 displayName / note / iconKey；不重命名 channel id。", List.of(), true),
                capability("signal_listener_edit", "Signal Listener edit", "编辑已有虚拟监听器 enabled / channel / cooldownTicks / conditionGroupId。", List.of(), true),
                capability("action_append", "Action append", "在已有 SignalListener / ActionRelay / Region / Timer action list 后追加 1 条 ActionConfig；不移动、不删除、不重排旧 action。", List.of(), true),
                capability("action_edit", "Action edit", "替换、禁用、删除或同 bucket 重排已有 SignalListener / Timer / ActionRelay / Region action；保存前均为 draft。", List.of("same_bucket_reorder", "draft_delete"), true)
        ));
        data.put("deferredNodeTypes", List.of(
                "GameController / MissionSystem / PhaseController",
                "if/else runtime / branch / fallback",
                "new ActionType / ConditionNodeType / StateVariable scope",
                "Condition gate remains editable reference, not branch",
                "StateVariable direct graph document save is not implemented; definition edit uses typed StateVariable endpoints",
                "freeform graph document save"
        ));
        data.put("virtualSignalListenerRequirement", "虚拟监听器是纯配置对象，不需要世界实体；9.1 可从合法频道下游列创建，保存仍走 SignalListener lifecycle 写入口。");
        data.put("worldEntityRequirement", "世界设备、VBD 和 RegionController 必须通过 protected draft registry / 客户端辅助会话进入 Logic Chain；后端仍拒绝手写 world/pos fake node，并对 world-backed draft 执行真实 typed commit / rollback adapter。");
        data.put("standaloneActionRelayAddNodeRemoved", true);
        data.put("protectedDraftRegistry", WebAdminProtectedDraftRegistry.class.getSimpleName());
        data.put("protectedDraftRecoverableFailureField", "lastCommitFailure");
        data.put("draftTransactionBoundary", "所有新增、编辑、删除、重排、连接和客户端辅助捕获在点击保存前都保留在 Logic Chain draft / protected draft registry；取消会释放 edit lock 并撤销 registry 草稿。");
        data.put("canEdit", permissionService.decide(user, WebAdminOperationType.EDIT_LOGIC_CHAIN).allowed());
        return data;
    }

    public WebAdminWriteResult enter(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest safeRequest = safeRequest(request);
        if (editLockService == null) {
            WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target(safeRequest));
            if (!preflight.success()) {
                return preflight;
            }
            return ok(target(safeRequest), "已进入逻辑链编辑模式。", editorData(server, user, session, safeRequest, Map.of()));
        }
        WebAdminEditLockRequest lockRequest = new WebAdminEditLockRequest();
        lockRequest.targetType = WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR;
        lockRequest.targetId = targetId(safeRequest);
        WebAdminWriteResult lockResult = editLockService.acquire(user, session, remoteAddress, lockRequest, csrfToken, sameOrigin);
        if (!lockResult.success()) {
            return lockResult;
        }
        return ok(target(safeRequest), "已进入逻辑链编辑模式。", editorData(server, user, session, safeRequest, lockResult.data()));
    }

    public WebAdminWriteResult validateDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminLogicChainEditorRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest safeRequest = safeRequest(request);
        WebAdminWriteTarget target = target(safeRequest);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            return preflight;
        }
        WebAdminWriteResult lock = validateEditorLock(user, session, safeRequest);
        if (!lock.success()) {
            return lock;
        }
        WebAdminDtos.LogicChainGraphDto graph = currentGraph(server, user, session, safeRequest);
        List<WebAdminValidationError> errors = validateDraftRequest(safeRequest, graph, false, user, session);
        if (!errors.isEmpty()) {
            return WebAdminWriteResult.validationFailed(target, errors);
        }
        return ok(target, "草稿校验通过。", Map.of(
                "baseGraphFingerprint", graphFingerprintFor(graph),
                "validation", Map.of("ok", true, "errors", List.of()),
                "supportedNodeTypes", List.copyOf(SUPPORTED_NODE_TYPES)
        ));
    }

    public WebAdminWriteResult saveDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        return saveCoordinator.saveDraft(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
    }

    public WebAdminWriteResult cancel(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest safeRequest = safeRequest(request);
        if (editLockService == null) {
            WebAdminSelectionSessions.cancelByEditLock(safeRequest.lockId, "Logic Chain 编辑已退出，选择已取消。");
            return ok(target(safeRequest), "已退出逻辑链编辑模式。", Map.of("discarded", true));
        }
        WebAdminEditLockRequest lockRequest = new WebAdminEditLockRequest();
        lockRequest.targetType = WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR;
        lockRequest.targetId = targetId(safeRequest);
        lockRequest.lockId = safeRequest.lockId;
        WebAdminWriteResult result = editLockService.release(user, session, remoteAddress, lockRequest, csrfToken, sameOrigin);
        if (result.success()) {
            WebAdminSelectionSessions.cancelByEditLock(safeRequest.lockId, "Logic Chain 编辑已退出，选择已取消。");
        }
        return result;
    }

    void releaseEditorLockAfterSuccessfulSave(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request
    ) {
        if (editLockService != null && !safe(request == null ? "" : request.lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR, targetId(request), request.lockId, user, session, remoteAddress);
        }
    }

    WebAdminWriteResult saveDraftNode(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            WebAdminWriteTarget target,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String csrfToken,
            boolean sameOrigin,
            int fieldIndex
    ) {
        return switch (normalizeNodeType(draftNode == null ? "" : draftNode.type)) {
            case "signal_join" -> saveSignalJoin(server, user, session, remoteAddress, draftNode, edges, csrfToken, sameOrigin);
            case "timer" -> saveTimer(server, user, session, remoteAddress, draftNode, edges, csrfToken, sameOrigin);
            case "signal_listener" -> saveSignalListener(server, user, session, remoteAddress, draftNode, edges, csrfToken, sameOrigin);
            case "virtual_block_device" -> saveVirtualBlockDeviceDraft(server, user, session, remoteAddress, request, draftNode, edges);
            case "world_device", "region_controller" -> saveProtectedWorldBackedDraft(server, user, session, remoteAddress, request, draftNode, edges);
            default -> WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[" + Math.max(0, fieldIndex) + "].type",
                    "unsupported_node_type",
                    "当前阶段暂不支持该节点类型。",
                    draftNode == null ? "" : draftNode.type,
                    safe(draftNode == null ? "" : draftNode.id),
                    "",
                    "",
                    "请选择当前阶段支持的 Signal Join、Timer、SignalListener、VBD、世界设备引用或区域控制器草稿节点。"
            )));
        };
    }

    private WebAdminWriteResult saveSignalJoin(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalJoinRequest request = deriveSignalJoinRequestFromEdges(node, edges);
        String joinId = SignalJoinStore.normalizeId(request.id);
        request.id = joinId;
        WebAdminWriteResult lockResult = acquireTypedLock(WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, user, session, remoteAddress, csrfToken, sameOrigin);
        if (!lockResult.success()) {
            return lockResult;
        }
        request.lockId = lockId(lockResult);
        WebAdminWriteResult result = signalJoinService.create(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
        releaseTypedLockOnFailure(WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, request.lockId, result, user, session, remoteAddress);
        return result;
    }

    private WebAdminWriteResult saveTimer(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminTimerRequest request = deriveTimerRequestFromEdges(node, edges);
        String timerId = TimerStore.normalizeId(request.id);
        request.id = timerId;
        WebAdminWriteResult lockResult = acquireTypedLock(WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, user, session, remoteAddress, csrfToken, sameOrigin);
        if (!lockResult.success()) {
            return lockResult;
        }
        request.lockId = lockId(lockResult);
        WebAdminWriteResult result = timerService.create(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
        releaseTypedLockOnFailure(WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, request.lockId, result, user, session, remoteAddress);
        return result;
    }

    private WebAdminWriteResult saveSignalListener(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalListenerCreateRequest request = deriveSignalListenerRequestFromEdges(node, edges);
        request.expectedFingerprint = WebAdminSignalListenerLifecycleService.CREATE_EXPECTED_FINGERPRINT;
        WebAdminWriteResult lockResult = acquireTypedLock(
                WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG,
                WebAdminSignalListenerLifecycleService.CREATE_LOCK_TARGET_ID,
                user,
                session,
                remoteAddress,
                csrfToken,
                sameOrigin
        );
        if (!lockResult.success()) {
            return lockResult;
        }
        request.lockId = lockId(lockResult);
        WebAdminWriteResult result = signalListenerLifecycleService.create(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
        releaseTypedLockOnFailure(WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, WebAdminSignalListenerLifecycleService.CREATE_LOCK_TARGET_ID, request.lockId, result, user, session, remoteAddress);
        return result;
    }

    private WebAdminWriteResult saveVirtualBlockDeviceDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges
    ) {
        String protectedDraftId = virtualBlockProtectedDraftId(node);
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.get(protectedDraftId);
        WebAdminWriteTarget target = target(request);
        if (entry == null) {
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_protected_draft_required",
                    "VBD 草稿必须来自游戏内选择产生的 protected draft。",
                    protectedDraftId,
                    safe(node == null ? "" : node.id),
                    "",
                    "",
                    "从 Logic Chain 的 VBD 节点入口发起游戏内选择，不要手写 world/pos。"
            )));
        }
        String channel = channelRefsFromEdges(edges, safe(node == null ? "" : node.id), "", "vbd_outputs_channel", false)
                .stream()
                .findFirst()
                .map(WebAdminLogicChainEditorService::channelName)
                .orElse("");
        if (channel.isBlank() || !SignalChannel.isValid(channel)) {
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "edges",
                    "logic_chain_vbd_output_channel_required",
                    "VBD 草稿需要通过画布连线决定下游 channel。",
                    channel,
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    "点击 VBD 右侧绿色加号，连接 1 个 Channel Endpoint。"
            )));
        }
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry savingEntry = WebAdminProtectedDraftRegistry.markSaving(
                protectedDraftId,
                request == null ? "" : request.lockId,
                user,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE
        );
        if (savingEntry == null) {
            List<String> violations = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                    protectedDraftId,
                    request == null ? "" : request.lockId,
                    user,
                    WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE
            );
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_protected_draft_inactive",
                    "VBD protected draft 已取消、过期或被其它编辑锁占用，保存已停止。",
                    protectedDraftId,
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    violations.isEmpty() ? "重新从 Logic Chain 发起游戏内选择。" : violations.getFirst()
            )));
        }
        if (server == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "server_unavailable");
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_vbd_commit_requires_server",
                    "VBD 草稿必须写入真实 VBD store；当前没有 Minecraft Server，已 fail closed，不能假提交，protected draft 已保留可重试。",
                    protectedDraftId,
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    "在真实服务器环境重新提交，或保留草稿等待服务器可用。"
            )));
        }
        ServerWorld world = serverWorld(server, entry.worldId());
        if (world == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_unavailable");
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_protected_draft_world_unloaded",
                    "VBD protected draft 所在世界不可用，已 fail closed，protected draft 已保留可重试。",
                    entry.worldId(),
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    "重新进入游戏内选择流程，确保目标世界已加载。"
            )));
        }
        BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
        SignalDeviceData existing = SignalDeviceStore.findVirtualBlockDevice(server, world, pos);
        if (existing != null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "vbd_position_conflict");
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_vbd_position_conflict",
                    "VBD protected draft 位置已经被其它虚拟方块设备占用，保存已停止，避免覆盖后来的配置。",
                    existing.id(),
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    "刷新后检查该 VBD；如需使用该位置，请重新发起 Logic Chain VBD 选择。"
            )));
        }
        SignalDeviceData created = SignalDeviceStore.createVirtualBlockIfAbsent(world, pos, channel);
        if (created == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "vbd_position_conflict");
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].virtualBlockDevice.protectedDraftId",
                    "logic_chain_vbd_position_conflict",
                    "VBD protected draft 位置已经被其它虚拟方块设备占用，保存已停止，避免覆盖后来的配置。",
                    protectedDraftId,
                    safe(node == null ? "" : node.id),
                    "",
                    channel,
                    "刷新后检查该 VBD；如需使用该位置，请重新发起 Logic Chain VBD 选择。"
            )));
        }
        boolean enabled = boolValue(node == null || node.virtualBlockDevice == null ? Boolean.TRUE : node.virtualBlockDevice.enabled, true);
        if (!enabled) {
            SignalDeviceData updated = SignalDeviceStore.updateBasicConfig(server, created.id(), false, channel);
            if (updated != null) {
                created = updated;
            } else {
                SignalDeviceStore.removeVirtualBlock(server, world, pos);
                WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "vbd_disable_update_failed");
                return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "VBD protected draft 提交失败，启用状态写入未完成；已回滚 VBD 绑定并保留 protected draft 可重试。");
            }
        }
        VbdDraftRequirementApplyResult requirementApply = applyVirtualBlockDeviceDraftRequirements(
                server,
                target,
                created,
                node == null ? null : node.virtualBlockDevice
        );
        if (requirementApply.failure() != null) {
            SignalDeviceStore.removeVirtualBlock(server, world, pos);
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "vbd_requirement_commit_failed");
            return requirementApply.failure();
        }
        if (requirementApply.device() != null) {
            created = requirementApply.device();
        }
        applyDeviceDraftMetadata(server, created, node == null ? null : node.virtualBlockDevice, user);
        if (WebAdminProtectedDraftRegistry.markCommitted(protectedDraftId) == null) {
            SignalDeviceStore.removeVirtualBlock(server, world, pos);
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "vbd_commit_state_lost");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CONFLICT_DETECTED, target, "VBD protected draft 提交状态已变化，已回滚本次 VBD 绑定，请刷新后重试。");
        }
        WebAdminWriteResult result = ok(target, "VBD 草稿已保存为正式虚拟方块设备。", Map.of(
                "deviceId", created.id(),
                "outputChannel", created.channel(),
                "itemSubmitRequirementCount", created.itemSubmitRequirements().size(),
                "containerRequirementCount", created.itemConditions().size(),
                "protectedDraft", WebAdminProtectedDraftRegistry.summary(protectedDraftId),
                "dataLogicChainVbdOutputDerivedFromEdges", true,
                "dataLogicChainVbdItemSubmitContainerCommitWired", true,
                "dataLogicChainVbdDeleteBindingNotBlock", true
        ));
        WebAdminAuditEvent auditEvent = audit(
                WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_LOGIC_CHAIN, new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE", created.id(), "Logic Chain VBD protected draft")),
                result,
                Map.of("protectedDraftId", protectedDraftId, "world", entry.worldId(), "x", entry.x(), "y", entry.y(), "z", entry.z()),
                Map.of("deviceId", created.id(), "outputChannel", created.channel(), "protectedDraftState", "committed")
        );
        publishVbdProtectedDraftWriteAudit(created, auditEvent, user);
        return result;
    }

    private VbdDraftRequirementApplyResult applyVirtualBlockDeviceDraftRequirements(
            MinecraftServer server,
            WebAdminWriteTarget target,
            SignalDeviceData created,
            WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft draft
    ) {
        SignalDeviceData updated = created;
        List<ItemSubmitRequirementData> itemSubmitRequirements = itemSubmitRequirementsFromLogicChainDraft(draft);
        boolean itemSubmitRequested = draft != null && (draft.itemSubmitEnabled || !itemSubmitRequirements.isEmpty());
        if (itemSubmitRequested) {
            boolean consumeEnabled = draft.itemSubmitConsumeEnabled || itemSubmitRequirements.stream().anyMatch(requirement -> requirement.consumeCount() > 0);
            String consumeOrder = InventoryConsumeOrder.normalize(draft.itemSubmitConsumeOrder);
            SignalDeviceData afterItemSubmit = SignalDeviceStore.updateVirtualItemSubmitForWebAdmin(
                    server,
                    updated == null ? "" : updated.id(),
                    true,
                    consumeEnabled,
                    consumeOrder.isBlank() ? InventoryConsumeOrder.HOTBAR_FIRST : consumeOrder,
                    itemSubmitRequirements,
                    false,
                    ""
            );
            if (afterItemSubmit == null) {
                return new VbdDraftRequirementApplyResult(WebAdminWriteResult.failed(
                        WebAdminWriteResultCode.INTERNAL_ERROR,
                        target,
                        "VBD itemSubmit 草稿提交失败；已回滚 VBD 绑定并保留 protected draft 可重试。"
                ), updated);
            }
            updated = afterItemSubmit;
        }

        List<ContainerItemConditionData> containerRequirements = containerRequirementsFromLogicChainDraft(draft);
        if (!containerRequirements.isEmpty()) {
            SignalDeviceData afterContainer = SignalDeviceStore.updateVirtualItemConditionsForWebAdmin(
                    server,
                    updated == null ? "" : updated.id(),
                    containerRequirements
            );
            if (afterContainer == null) {
                return new VbdDraftRequirementApplyResult(WebAdminWriteResult.failed(
                        WebAdminWriteResultCode.INTERNAL_ERROR,
                        target,
                        "VBD container 草稿提交失败；已回滚 VBD 绑定并保留 protected draft 可重试。"
                ), updated);
            }
            updated = afterContainer;
        }
        return new VbdDraftRequirementApplyResult(null, updated);
    }

    private static boolean virtualBlockDeviceDraftHasRequirements(WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft draft) {
        return draft != null && (Boolean.TRUE.equals(draft.itemSubmitEnabled)
                || (draft.itemSubmitRequirements != null && !draft.itemSubmitRequirements.isEmpty())
                || (draft.containerRequirements != null && !draft.containerRequirements.isEmpty()));
    }

    private static boolean virtualBlockDeviceDraftHasNativeTriggers(WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft draft) {
        return draft != null && draft.nativeTriggers != null;
    }

    private static List<ItemSubmitRequirementData> itemSubmitRequirementsFromLogicChainDraft(WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft draft) {
        if (draft == null || draft.itemSubmitRequirements == null || draft.itemSubmitRequirements.isEmpty()) {
            return List.of();
        }
        List<ItemSubmitRequirementData> requirements = new ArrayList<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft requirement : draft.itemSubmitRequirements) {
            if (requirement != null) {
                requirements.add(itemSubmitRequirementFromLogicChainDraft(requirement, index));
            }
            index++;
        }
        return List.copyOf(requirements);
    }

    private static ItemSubmitRequirementData itemSubmitRequirementFromLogicChainDraft(WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft draft, int index) {
        String itemId = firstNonBlank(draft.templateItemId, draft.itemId).trim().toLowerCase(Locale.ROOT);
        String countMode = ContainerItemCountMode.normalize(firstNonBlank(draft.countMode, ContainerItemCountMode.AT_LEAST.id()));
        int templateCount = Math.max(1, parseActionIndex(draft.templateCount));
        int requiredCount = parseActionIndex(draft.requiredCount);
        int count = Math.max(1, requiredCount > 0 ? requiredCount : parseActionIndex(draft.count));
        int consumeCount = Math.max(1, parseActionIndex(draft.consumeCount));
        long now = System.currentTimeMillis();
        String id = safe(draft.requirementId).isBlank() ? "logic-chain-item-" + (index + 1) : safe(draft.requirementId);
        String name = safe(draft.displayName).isBlank() ? "Logic Chain itemSubmit " + (index + 1) : safe(draft.displayName);
        String summary = safe(draft.templateSummary).isBlank() ? (itemId.isBlank() ? "Logic Chain VBD itemSubmit 草稿" : itemId + " x" + count) : safe(draft.templateSummary);
        ItemStackMatcherData matcher = new ItemStackMatcherData(
                true,
                itemId,
                templateCount,
                countMode,
                ContainerItemCountMode.IGNORE.id().equals(countMode) ? 0 : count,
                draft.matchItemId == null || draft.matchItemId,
                truthy(draft.matchDamage),
                truthy(draft.matchCustomName),
                truthy(draft.matchLore),
                truthy(draft.matchCustomData),
                truthy(draft.matchComponents),
                Math.max(0, parseActionIndex(draft.templateDamage)),
                safe(draft.templateCustomName),
                draft.templateLore == null ? List.of() : draft.templateLore,
                safe(draft.templateCustomData),
                safe(draft.templateComponents),
                summary,
                now,
                now
        ).normalized();
        return new ItemSubmitRequirementData(
                id,
                name,
                draft.requirementEnabled == null || draft.requirementEnabled,
                matcher,
                consumeCount,
                false,
                0,
                0L,
                "Logic Chain VBD draft"
        ).normalized();
    }

    private static List<ContainerItemConditionData> containerRequirementsFromLogicChainDraft(WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft draft) {
        if (draft == null || draft.containerRequirements == null || draft.containerRequirements.isEmpty()) {
            return List.of();
        }
        List<ContainerItemConditionData> conditions = new ArrayList<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ContainerRequirementDraft requirement : draft.containerRequirements) {
            if (requirement != null) {
                conditions.add(containerRequirementFromLogicChainDraft(requirement, index));
            }
            index++;
        }
        return List.copyOf(conditions);
    }

    private static ContainerItemConditionData containerRequirementFromLogicChainDraft(WebAdminLogicChainEditorRequest.ContainerRequirementDraft draft, int index) {
        int slot = Math.max(0, parseActionIndex(draft.slot));
        int count = Math.max(0, parseActionIndex(draft.count));
        String id = safe(draft.requirementId).isBlank() ? "logic-chain-container-" + (index + 1) : safe(draft.requirementId);
        String name = safe(draft.displayName).isBlank() ? "Logic Chain container " + (index + 1) : safe(draft.displayName);
        String type = safe(draft.type).isBlank() ? ContainerItemConditionType.SLOT_MATCHER.id() : safe(draft.type);
        String countMode = ContainerItemCountMode.normalize(firstNonBlank(draft.countMode, ContainerItemCountMode.AT_LEAST.id()));
        String summary = safe(draft.templateSummary).isBlank() ? (safe(draft.itemId).isBlank() ? "Logic Chain VBD container 草稿" : safe(draft.itemId) + " x" + count) : safe(draft.templateSummary);
        ItemStackMatcherData matcher = new ItemStackMatcherData(
                !safe(draft.matcherTemplateItemId).isBlank(),
                safe(draft.matcherTemplateItemId),
                Math.max(1, parseActionIndex(draft.matcherTemplateCount)),
                ContainerItemCountMode.normalize(firstNonBlank(draft.matcherCountMode, countMode)),
                Math.max(0, parseActionIndex(draft.matcherRequiredCount)),
                draft.matcherMatchItemId == null || draft.matcherMatchItemId,
                truthy(draft.matcherMatchDamage),
                truthy(draft.matcherMatchCustomName),
                truthy(draft.matcherMatchLore),
                truthy(draft.matcherMatchCustomData),
                truthy(draft.matcherMatchComponents),
                Math.max(0, parseActionIndex(draft.matcherTemplateDamage)),
                safe(draft.matcherTemplateCustomName),
                draft.matcherTemplateLore == null ? List.of() : draft.matcherTemplateLore,
                safe(draft.matcherTemplateCustomData),
                safe(draft.matcherTemplateComponents),
                firstNonBlank(draft.matcherSummary, summary),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        ).normalized();
        return new ContainerItemConditionData(
                id,
                name,
                draft.enabled == null || draft.enabled,
                type,
                slot,
                safe(draft.itemId),
                countMode,
                count,
                safe(draft.channel),
                safe(draft.offChannel),
                safe(draft.mode),
                false,
                0L,
                0L,
                0L,
                summary,
                matcher
        ).normalized();
    }

    private record VbdDraftRequirementApplyResult(WebAdminWriteResult failure, SignalDeviceData device) {
    }

    private record ActionListConversion(List<WebAdminValidationError> errors, List<ActionConfig> actions) {
    }

    private WebAdminWriteResult saveProtectedWorldBackedDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges
    ) {
        String nodeType = normalizeNodeType(node == null ? "" : node.type);
        String protectedDraftId = protectedDraftIdFor(node);
        WebAdminWriteTarget target = target(request);
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.markSaving(
                protectedDraftId,
                request == null ? "" : request.lockId,
                user,
                protectedDraftObjectType(nodeType)
        );
        if (entry == null) {
            List<String> violations = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                    protectedDraftId,
                    request == null ? "" : request.lockId,
                    user,
                    protectedDraftObjectType(nodeType)
            );
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].protectedDraftId",
                    "logic_chain_protected_draft_required",
                    "世界对象草稿必须来自持锁客户端辅助会话：" + violations.getFirst(),
                    protectedDraftId,
                    safe(node == null ? "" : node.id),
                    "",
                    "",
                    "重新从 Logic Chain 发起世界设备/区域控制器客户端辅助流程。"
            )));
        }
        if (server == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "server_unavailable");
            return WebAdminWriteResult.validationFailed(target, List.of(error("nodes[0].protectedDraftId", "logic_chain_world_backed_commit_requires_server", "世界对象草稿必须写入真实 typed store；当前没有 Minecraft Server，已 fail closed。", protectedDraftId, safe(node == null ? "" : node.id), "", "", "在真实服务器环境重新提交。")));
        }
        return "world_device".equals(nodeType)
                ? saveWorldDeviceProtectedDraft(server, user, session, remoteAddress, request, node, edges, entry, target)
                : saveRegionControllerProtectedDraft(server, user, session, remoteAddress, request, node, entry, target);
    }

    private WebAdminWriteResult saveWorldDeviceProtectedDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry,
            WebAdminWriteTarget target
    ) {
        String protectedDraftId = entry.draftSessionId();
        String nodeId = safe(node == null ? "" : node.id);
        String requestDeviceType = normalizeWorldDeviceDraftType(node == null || node.worldDevice == null ? "" : node.worldDevice.deviceType);
        String protectedDeviceType = normalizeWorldDeviceDraftType(metadataString(entry, "deviceType"));
        if (!requestDeviceType.isBlank() && !protectedDeviceType.isBlank() && !requestDeviceType.equals(protectedDeviceType)) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_type_mismatch");
            return WebAdminWriteResult.validationFailed(target, List.of(worldDeviceTypeMismatchError("nodes[0].worldDevice.deviceType", requestDeviceType, protectedDeviceType, "", nodeId)));
        }
        ServerWorld world = serverWorld(server, entry.worldId());
        if (world == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_unavailable");
            return WebAdminWriteResult.validationFailed(target, List.of(error("nodes[0].protectedDraftId", "logic_chain_protected_draft_world_unloaded", "世界设备 protected draft 所在世界不可用，已 fail closed。", entry.worldId(), nodeId, "", "", "重新发起游戏内放置，确保目标世界已加载。")));
        }
        BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
        String deviceId = firstNonBlank(metadataString(entry, "deviceId"), SignalDeviceStore.sourceId(world, pos));
        Map<String, Object> integrity = validateWorldDeviceProtectedDraftIntegrity(world, pos, entry, deviceId);
        if (Boolean.FALSE.equals(integrity.get("success"))) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_integrity_check_failed");
            return WebAdminWriteResult.validationFailed(target, List.of(error(
                    "nodes[0].protectedDraftId",
                    "logic_chain_world_device_integrity_check_failed",
                    String.valueOf(integrity.get("message")),
                    String.valueOf(integrity.getOrDefault("actualBlockId", "")),
                    nodeId,
                    "",
                    "",
                    "不要手动破坏或替换 protected draft 方块；请取消后重新发起世界设备放置。"
            )));
        }
        SignalDeviceData beforeDevice = SignalDeviceStore.getSnapshot(server).stream()
                .filter(device -> device.id().equals(deviceId))
                .findFirst()
                .orElse(null);
        String storeDeviceType = normalizeWorldDeviceDraftType(beforeDevice == null ? "" : beforeDevice.type());
        if (!storeDeviceType.isBlank()
                && ((!protectedDeviceType.isBlank() && !storeDeviceType.equals(protectedDeviceType))
                || (!requestDeviceType.isBlank() && !storeDeviceType.equals(requestDeviceType)))) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_type_mismatch");
            return WebAdminWriteResult.validationFailed(target, List.of(worldDeviceTypeMismatchError("nodes[0].worldDevice.deviceType", requestDeviceType, protectedDeviceType, storeDeviceType, nodeId)));
        }
        String deviceType = firstNonBlank(protectedDeviceType, storeDeviceType);
        if (deviceType.isBlank()) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_type_mismatch");
            return WebAdminWriteResult.validationFailed(target, List.of(worldDeviceTypeMismatchError("nodes[0].worldDevice.deviceType", requestDeviceType, protectedDeviceType, storeDeviceType, nodeId)));
        }
        boolean consumerDevice = SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(deviceType) || SignalDeviceData.TYPE_ACTION_RELAY.equals(deviceType);
        String edgeType = consumerDevice ? "world_device_consumes_channel" : "world_device_outputs_channel";
        String channel = (consumerDevice
                ? channelRefsFromEdges(edges, "", nodeId, edgeType, true)
                : channelRefsFromEdges(edges, nodeId, "", edgeType, false))
                .stream()
                .findFirst()
                .map(WebAdminLogicChainEditorService::channelName)
                .orElse("");
        if (channel.isBlank() || !SignalChannel.isValid(channel)) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, consumerDevice ? "world_device_input_channel_required" : "world_device_output_channel_required");
            return WebAdminWriteResult.validationFailed(target, List.of(error("edges", consumerDevice ? "logic_chain_world_device_input_channel_required" : "logic_chain_world_device_output_channel_required", consumerDevice ? "接收器 / 动作继电器草稿需要通过左侧 consumes 连线决定接收频道。" : "世界设备草稿需要通过画布连线决定下游 channel。", channel, nodeId, "", channel, consumerDevice ? "点击世界设备引用左侧绿色加号，连接 1 个 Channel Endpoint。" : "点击世界设备引用右侧绿色加号，连接 1 个 Channel Endpoint。")));
        }
        WebAdminLogicChainEditorRequest.WorldDeviceDraft worldDraft = node == null ? null : node.worldDevice;
        ActionListConversion actionConversion = draftActionsFromEntries(worldDraft == null ? List.of() : worldDraft.actions);
        if (!actionConversion.errors().isEmpty()) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_draft_actions_invalid");
            return WebAdminWriteResult.validationFailed(target, actionConversion.errors());
        }
        ActionRelayBlockEntity draftActionRelay = null;
        if (!actionConversion.actions().isEmpty()) {
            if (beforeDevice == null || !SignalDeviceData.TYPE_ACTION_RELAY.equals(beforeDevice.type())) {
                WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_draft_actions_not_action_relay");
                return WebAdminWriteResult.validationFailed(target, List.of(error(
                        "nodes[0].worldDevice.actions",
                        "logic_chain_world_device_actions_require_action_relay",
                        "只有 ActionRelay 世界设备引用能携带草稿 Action 列表。",
                        beforeDevice == null ? "" : beforeDevice.type(),
                        nodeId,
                        "",
                        channel,
                        "请在三格 hotbar 中选择 ActionRelay，或清空该世界设备草稿的 Action 列表。"
                )));
            }
            draftActionRelay = SignalDeviceStore.getLoadedActionRelay(server, beforeDevice);
            if (draftActionRelay == null) {
                WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_action_relay_unloaded");
                return WebAdminWriteResult.validationFailed(target, List.of(error(
                        "nodes[0].worldDevice.actions",
                        "logic_chain_world_device_action_relay_unloaded",
                        "ActionRelay 草稿 Action 需要目标方块实体已加载，不能写入 store-only fallback。",
                        beforeDevice.id(),
                        nodeId,
                        "",
                        channel,
                        "请保持该世界设备所在区块加载后重试，或先清空草稿 Action 列表。"
                )));
            }
        }
        boolean enabled = boolValue(node == null || node.worldDevice == null ? Boolean.TRUE : node.worldDevice.enabled, true);
        SignalDeviceData updated = SignalDeviceStore.updateBasicConfig(server, deviceId, enabled, channel);
        if (updated == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_commit_failed");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "世界设备 protected draft 提交失败；未写入 graph card，草稿保留在可重试状态。");
        }
        if (!actionConversion.actions().isEmpty()) {
            ActionRelayBlockEntity relay = draftActionRelay == null ? SignalDeviceStore.getLoadedActionRelay(server, updated) : draftActionRelay;
            relay.replaceActions(actionConversion.actions());
            SignalDeviceStore.updateActions(world, pos, relay);
        }
        applyDeviceDraftMetadata(server, updated, worldDraft, user);
        SignalDeviceStore.flushDirty(server);
        if (WebAdminProtectedDraftRegistry.markCommitted(protectedDraftId) == null) {
            WebAdminSelectionSessions.cancelProtectedDraftFromWebAdmin(server, writeContext(user, session, remoteAddress, target), protectedDraftId, "世界设备草稿提交状态丢失，已回滚。");
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "world_device_commit_state_lost");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CONFLICT_DETECTED, target, "世界设备 protected draft 提交状态已变化，已回滚本次写入，请刷新后重试。");
        }
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("deviceId", updated.id());
        resultData.put(consumerDevice ? "inputChannel" : "outputChannel", updated.channel());
        resultData.put(consumerDevice ? "consumesChannel" : "emitsChannel", updated.channel());
        resultData.put("protectedDraft", WebAdminProtectedDraftRegistry.summary(protectedDraftId));
        resultData.put("dataLogicChainWorldBackedCommitRollbackAdapter", true);
        WebAdminWriteResult result = ok(target, "世界设备草稿已保存为正式 SignalDevice 配置。", resultData);
        Map<String, Object> auditAfter = new LinkedHashMap<>();
        auditAfter.put("deviceId", updated.id());
        auditAfter.put(consumerDevice ? "inputChannel" : "outputChannel", updated.channel());
        auditAfter.put(consumerDevice ? "consumesChannel" : "emitsChannel", updated.channel());
        auditAfter.put("protectedDraftState", "committed");
        WebAdminAuditEvent auditEvent = audit(writeContext(user, session, remoteAddress, new WebAdminWriteTarget("DEVICE", updated.id(), "Logic Chain 世界设备 protected draft")), result, entry.toMap(), auditAfter);
        publishWorldBackedProtectedDraftWriteAudit(updated.id(), updated.channel(), updated.type(), auditEvent);
        return result;
    }

    private Map<String, Object> validateWorldDeviceProtectedDraftIntegrity(
            ServerWorld world,
            BlockPos pos,
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry,
            String deviceId
    ) {
        if (world == null || pos == null || entry == null) {
            return Map.of("success", false, "message", "世界设备 protected draft 缺少提交上下文。");
        }
        if (!world.isInBuildLimit(pos) || !world.isChunkLoaded(pos)) {
            return Map.of("success", false, "message", "世界设备 protected draft 所在区块未加载，不能提交 store-only fallback。", "world", entry.worldId(), "pos", pos.toShortString());
        }
        BlockState currentState = world.getBlockState(pos);
        String expectedBlockId = metadataString(entry, "blockId");
        String actualBlockId = VirtualBlockDeviceSupport.blockId(currentState);
        if (currentState.isAir()
                || !VirtualBlockDeviceSupport.isDedicatedSignalDevice(currentState)
                || (!expectedBlockId.isBlank() && !expectedBlockId.equals(actualBlockId))) {
            return Map.of(
                    "success", false,
                    "message", "世界设备 protected draft 方块已缺失或被外部替换，已 fail closed。",
                    "expectedBlockId", expectedBlockId,
                    "actualBlockId", actualBlockId
            );
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return Map.of("success", false, "message", "世界设备 protected draft 方块实体未加载，不能提交 store-only fallback。", "actualBlockId", actualBlockId);
        }
        String sourceId = SignalDeviceStore.sourceId(world, pos);
        if (!sourceId.equals(deviceId)) {
            return Map.of("success", false, "message", "世界设备 protected draft 设备 ID 与位置不匹配，不能提交。", "expectedDeviceId", sourceId, "actualDeviceId", safe(deviceId));
        }
        return Map.of("success", true, "actualBlockId", actualBlockId, "deviceId", deviceId);
    }

    private WebAdminWriteResult saveRegionControllerProtectedDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            WebAdminLogicChainEditorRequest.DraftNode node,
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry,
            WebAdminWriteTarget target
    ) {
        String protectedDraftId = entry.draftSessionId();
        String nodeId = safe(node == null ? "" : node.id);
        List<RegionGeometry.Point> points = regionPointsFromProtectedDraft(entry);
        if (points.size() < 3 || !RegionGeometry.isSimplePolygon(points)) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_points_invalid");
            return WebAdminWriteResult.validationFailed(target, List.of(error("nodes[0].regionController.protectedDraftId", "logic_chain_region_points_invalid", "区域控制器 protected draft 缺少有效角点，无法创建真实 Region。", metadataString(entry, "regionPoints"), nodeId, "", "", "重新发起区域控制器游戏内角点选择。")));
        }
        WebAdminLogicChainEditorRequest.RegionControllerDraft draft = node == null ? null : node.regionController;
        ActionListConversion enterActions = draftActionsFromEntries(draft == null ? List.of() : draft.enterActions);
        ActionListConversion exitActions = draftActionsFromEntries(draft == null ? List.of() : draft.exitActions);
        ActionListConversion stayActions = draftActionsFromEntries(draft == null ? List.of() : draft.stayActions);
        List<WebAdminValidationError> actionErrors = new ArrayList<>();
        actionErrors.addAll(enterActions.errors());
        actionErrors.addAll(exitActions.errors());
        actionErrors.addAll(stayActions.errors());
        if (!actionErrors.isEmpty()) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_controller_draft_actions_invalid");
            return WebAdminWriteResult.validationFailed(target, actionErrors);
        }
        MapDataStore.PlannerRegionResult regionResult = MapDataStore.addPlannerRegion(server, entry.worldId(), points, 0x22D3EE);
        if (regionResult.status() != MapDataStore.PlannerRegionStatus.OK || regionResult.region() == null) {
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_create_failed_" + regionResult.status());
            return WebAdminWriteResult.validationFailed(target, List.of(error("nodes[0].regionController.regionId", "logic_chain_region_commit_failed", "Region 创建失败：" + regionResult.status(), regionResult.status().name(), nodeId, "", "", "检查区域是否重叠、点数和形状后重新选择。")));
        }
        MapDataStore.PlannerRegionData region = regionResult.region();
        String regionName = firstNonBlank(draft == null ? "" : draft.regionDisplayName, draft == null ? "" : draft.controllerDisplayName, "Logic Chain 区域");
        MapDataStore.PlannerRegionResult renamed = MapDataStore.renamePlannerRegion(server, region.id(), regionName);
        if (renamed.status() == MapDataStore.PlannerRegionStatus.OK && renamed.region() != null) {
            region = renamed.region();
        }
        String controllerName = firstNonBlank(draft == null ? "" : draft.controllerDisplayName, region.name(), "Logic Chain 区域控制器");
        RegionControllerData controller = RegionControllerStore.createController(server, region.id(), controllerName);
        if (controller == null) {
            MapDataStore.deletePlannerRegion(server, region.id());
            MapDataStore.flushDirty(server);
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_controller_create_failed");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "RegionController 创建失败；已回滚新建 Region。");
        }
        RegionControllerData configured = new RegionControllerData(
                controller.id(),
                controllerName,
                region.id(),
                boolValue(draft == null ? Boolean.TRUE : draft.enabled, true),
                regionTargetFilter(draft),
                intValue(draft == null ? 100 : draft.stayIntervalTicks, 100),
                controller.enterConditionGroupId(),
                controller.exitConditionGroupId(),
                controller.stayConditionGroupId(),
                enterActions.actions(),
                exitActions.actions(),
                stayActions.actions()
        ).normalized();
        if (!RegionControllerStore.updateController(server, controller.id(), configured)) {
            RegionControllerStore.deleteController(server, controller.id());
            MapDataStore.deletePlannerRegion(server, region.id());
            RegionControllerStore.flushDirty(server);
            MapDataStore.flushDirty(server);
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_controller_update_failed");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "RegionController 配置写入失败；已回滚新建 Controller 和 Region。");
        }
        MapDataStore.flushDirty(server);
        RegionControllerStore.flushDirty(server);
        if (WebAdminProtectedDraftRegistry.markCommitted(protectedDraftId) == null) {
            RegionControllerStore.deleteController(server, controller.id());
            MapDataStore.deletePlannerRegion(server, region.id());
            RegionControllerStore.flushDirty(server);
            MapDataStore.flushDirty(server);
            WebAdminProtectedDraftRegistry.markCommitFailed(protectedDraftId, "region_controller_commit_state_lost");
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CONFLICT_DETECTED, target, "RegionController protected draft 提交状态已变化，已回滚本次写入，请刷新后重试。");
        }
        WebAdminWriteResult result = ok(target, "区域控制器草稿已保存为真实 Region + RegionController。", Map.of(
                "regionId", region.id(),
                "controllerId", controller.id(),
                "protectedDraft", WebAdminProtectedDraftRegistry.summary(protectedDraftId),
                "regionControllerOutputRequiresActionBucket", true,
                "dataLogicChainWorldBackedCommitRollbackAdapter", true
        ));
        WebAdminAuditEvent auditEvent = audit(writeContext(user, session, remoteAddress, new WebAdminWriteTarget("REGION_CONTROLLER", controller.id(), "Logic Chain RegionController protected draft")), result, entry.toMap(), Map.of("regionId", region.id(), "controllerId", controller.id(), "protectedDraftState", "committed"));
        publishWorldBackedProtectedDraftWriteAudit(controller.id(), "", "region_controller", auditEvent);
        return result;
    }

    WebAdminWriteResult saveActionAppend(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.ActionAppendDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.ActionAppendDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionAppendDraft() : draft;
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = safe(safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        if ("listener".equals(ownerType)) {
            WebAdminSignalListenerActionRequests.ActionAddRequest request = new WebAdminSignalListenerActionRequests.ActionAddRequest();
            request.listenerId = ownerId;
            request.action = safeDraft.action == null ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry() : safeDraft.action;
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return signalListenerActionsService.addAction(server, user, session, remoteAddress, ownerId, request, csrfToken, sameOrigin);
        }
        if ("action_relay".equals(ownerType)) {
            WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
            request.deviceId = ownerId;
            request.actions = List.of(safeDraft.action == null ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry() : safeDraft.action);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return actionRelayActionsService.addAction(server, user, session, remoteAddress, ownerId, request, csrfToken, sameOrigin);
        }
        if ("region_controller".equals(ownerType)) {
            RegionTriggerType triggerType = parseRegionTrigger(bucket);
            if (triggerType == null) {
                return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                        "actionAppend.bucket",
                        "logic_chain_region_action_bucket_invalid",
                        "Region action bucket 只支持 enter / exit / stay。",
                        bucket,
                        "region_controller:" + ownerId,
                        "",
                        "",
                        "选择 Region 的 enter、exit 或 stay 动作桶后再追加 Action。"
                )));
            }
            WebAdminRegionControllerRequests.ActionAddRequest request = new WebAdminRegionControllerRequests.ActionAddRequest();
            request.controllerId = ownerId;
            request.triggerType = triggerType.name();
            request.action = safeDraft.action == null ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry() : safeDraft.action;
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return regionControllerService.addAction(server, user, session, remoteAddress, ownerId, triggerType, request, csrfToken, sameOrigin);
        }
        if ("timer".equals(ownerType)) {
            return timerService.addActionToBucket(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    bucket,
                    safeDraft.action,
                    safeDraft.expectedFingerprint,
                    safeDraft.lockId,
                    csrfToken,
                    sameOrigin
            );
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "actionAppend.ownerType",
                "logic_chain_action_append_owner_type_invalid",
                "当前只支持给 SignalListener、ActionRelay、RegionController 或 Timer 追加 Action。",
                ownerType,
                "",
                "",
                "",
                "从已有 SignalListener、ActionRelay、RegionController 或 Timer 的 action 列表入口追加。"
        )));
    }

    WebAdminWriteResult saveExistingNodeEdit(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft() : draft;
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        String targetId = safe(safeDraft.targetId);
        if ("channel_metadata".equals(nodeType)) {
            WebAdminChannelMetadataUpdateRequest request = safeDraft.channelMetadata == null ? new WebAdminChannelMetadataUpdateRequest() : safeDraft.channelMetadata;
            if (safe(request.channel).isBlank()) {
                request.channel = targetId;
            }
            return channelMetadataService.update(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
        }
        if ("signal_join".equals(nodeType)) {
            WebAdminSignalJoinRequest request = safeDraft.signalJoin == null ? new WebAdminSignalJoinRequest() : safeDraft.signalJoin;
            if (safe(request.id).isBlank()) {
                request.id = targetId;
            }
            String joinId = SignalJoinStore.normalizeId(request.id);
            return signalJoinService.update(server, user, session, remoteAddress, joinId, request, csrfToken, sameOrigin);
        }
        if ("timer".equals(nodeType)) {
            WebAdminTimerRequest request = safeDraft.timer == null ? new WebAdminTimerRequest() : safeDraft.timer;
            if (safe(request.id).isBlank()) {
                request.id = targetId;
            }
            String timerId = TimerStore.normalizeId(request.id);
            return timerService.updateBasicConfigPreservingActions(server, user, session, remoteAddress, timerId, request, csrfToken, sameOrigin);
        }
        if ("signal_listener".equals(nodeType)) {
            WebAdminSignalListenerBasicConfigUpdateRequest request = safeDraft.signalListenerBasic == null ? new WebAdminSignalListenerBasicConfigUpdateRequest() : safeDraft.signalListenerBasic;
            if (safe(request.listenerRef).isBlank()) {
                request.listenerRef = targetId;
            }
            return signalListenerBasicConfigService.update(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
        }
        if (isDeviceExistingNodeType(nodeType)) {
            WebAdminWriteResult result = ok(target(new WebAdminLogicChainEditorRequest()), "设备节点编辑草稿已处理。", Map.of("nodeType", nodeType, "targetId", targetId));
            if (isPhysicalDeviceNodeType(nodeType) && (safeDraft.deviceBasic != null || safeDraft.deviceMetadata != null)) {
                WebAdminWriteResult preflight = requirePhysicalDevicePresentForExistingEdit(server, nodeType, targetId);
                if (!preflight.success()) {
                    return preflight;
                }
            }
            if (safeDraft.deviceBasic != null) {
                WebAdminDeviceBasicConfigUpdateRequest request = safeDraft.deviceBasic;
                if (safe(request.deviceId).isBlank()) {
                    request.deviceId = targetId;
                }
                if (isPhysicalDeviceNodeType(nodeType)) {
                    request.strictPhysicalPresence = Boolean.TRUE;
                }
                result = deviceBasicConfigService.update(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
                if (!result.success()) {
                    return result;
                }
            }
            if (safeDraft.deviceMetadata != null) {
                WebAdminDeviceMetadataUpdateRequest request = safeDraft.deviceMetadata;
                if (safe(request.deviceId).isBlank()) {
                    request.deviceId = targetId;
                }
                result = deviceMetadataService.update(server, user, session, remoteAddress, request, csrfToken, sameOrigin);
                if (!result.success()) {
                    return result;
                }
            }
            if ("virtual_block_device".equals(nodeType) && safeDraft.virtualBlockDevice != null && virtualBlockDeviceDraftHasNativeTriggers(safeDraft.virtualBlockDevice)) {
                WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request = safeDraft.virtualBlockDevice.nativeTriggers;
                if (request != null && safe(request.deviceId).isBlank()) {
                    request.deviceId = targetId;
                }
                result = virtualBlockDeviceNativeTriggerService.update(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
                if (!result.success()) {
                    return result;
                }
            }
            if ("virtual_block_device".equals(nodeType) && safeDraft.virtualBlockDevice != null && virtualBlockDeviceDraftHasRequirements(safeDraft.virtualBlockDevice)) {
                SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, targetId);
                SignalDeviceData device = resolved.foundUnique() ? resolved.device().normalized() : null;
                WebAdminWriteTarget writeTarget = new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE", targetId, targetId);
                if (device == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
                    return WebAdminWriteResult.validationFailed(writeTarget, List.of(error(
                            "existingNodeEdits[].virtualBlockDevice",
                            "logic_chain_vbd_existing_draft_target_missing",
                            "VBD 节点草稿目标不存在或类型不匹配，保存已停止。",
                            targetId,
                            existingNodeIdForError(nodeType, targetId),
                            "",
                            "",
                            "刷新 Logic Chain 后重新从 VBD 主节点打开编辑。"
                    )));
                }
                VbdDraftRequirementApplyResult requirementApply = applyVirtualBlockDeviceDraftRequirements(server, writeTarget, device, safeDraft.virtualBlockDevice);
                if (requirementApply.failure() != null) {
                    return requirementApply.failure();
                }
                result = ok(writeTarget, "VBD 节点内 itemSubmit / container 草稿已保存。", Map.of(
                        "deviceId", device.id(),
                        "itemSubmitRequirementCount", itemSubmitRequirementsFromLogicChainDraft(safeDraft.virtualBlockDevice).size(),
                        "containerRequirementCount", containerRequirementsFromLogicChainDraft(safeDraft.virtualBlockDevice).size(),
                        "dataLogicChainExistingVbdItemSubmitContainerDraftOnly", true
                ));
            }
            return result;
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "existingNodeEdits[].nodeType",
                "logic_chain_existing_node_type_deferred",
                "此节点当前只能查看，编辑能力后续支持。",
                safeDraft.nodeType,
                targetId,
                "",
                "",
                "本阶段只支持 Channel metadata、Signal Join、Timer、SignalListener、世界设备和 VBD 的受控安全字段。"
        )));
    }

    private WebAdminWriteResult requirePhysicalDevicePresentForExistingEdit(
            MinecraftServer server,
            String nodeType,
            String targetId
    ) {
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, targetId);
        SignalDeviceData device = resolved.foundUnique() ? resolved.device().normalized() : null;
        WebAdminWriteTarget writeTarget = new WebAdminWriteTarget("DEVICE_BASIC_CONFIG", device == null ? targetId : device.id(), device == null ? targetId : WebAdminReadonlySupport.deviceDisplayName(device));
        if (device == null) {
            return WebAdminWriteResult.validationFailed(writeTarget, List.of(error(
                    "existingNodeEdits[].targetId",
                    "logic_chain_physical_device_missing_or_broken",
                    "实体设备不存在或已被删除，不能继续保存为正常节点。",
                    targetId,
                    existingNodeIdForError(nodeType, targetId),
                    "",
                    "",
                    "刷新 Logic Chain，确认 missing/broken 状态后再处理该设备。"
            )));
        }
        if (!normalizeExistingNodeEditType(nodeType).equals(device.type()) || !loadedPhysicalDevicePresent(server, device)) {
            return WebAdminWriteResult.validationFailed(writeTarget, List.of(error(
                    "existingNodeEdits[].targetId",
                    "logic_chain_physical_device_missing_or_broken",
                    "实体设备方块不存在、区块未加载或方块实体类型不匹配，保存已 fail closed。",
                    device.id(),
                    existingNodeIdForError(nodeType, targetId),
                    "",
                    device.channel(),
                    "刷新 Logic Chain；如果方块已被外部破坏，不能只保存 metadata 把它伪装成正常节点。"
            )));
        }
        return ok(writeTarget, "实体设备 presence 预检通过。", Map.of("deviceId", device.id()));
    }

    WebAdminWriteResult saveActionEdit(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.ActionEditDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.ActionEditDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionEditDraft() : draft;
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = safe(safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        if ("listener".equals(ownerType)) {
            WebAdminSignalListenerActionRequests.ActionUpdateRequest request = new WebAdminSignalListenerActionRequests.ActionUpdateRequest();
            request.listenerId = ownerId;
            request.actionIndex = safeDraft.actionIndex;
            request.action = actionEntryForActionEditOperation(safeDraft);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return signalListenerActionsService.updateAction(server, user, session, remoteAddress, ownerId, request, csrfToken, sameOrigin);
        }
        if ("timer".equals(ownerType)) {
            return timerService.updateActionInBucket(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    bucket,
                    safeDraft.actionIndex,
                    actionEntryForActionEditOperation(safeDraft),
                    safeDraft.expectedFingerprint,
                    safeDraft.lockId,
                    csrfToken,
                    sameOrigin
            );
        }
        if ("action_relay".equals(ownerType)) {
            WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
            request.deviceId = ownerId;
            request.actions = List.of(actionEntryForActionEditOperation(safeDraft));
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return actionRelayActionsService.updateAction(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    parseActionIndex(safeDraft.actionIndex),
                    request,
                    csrfToken,
                    sameOrigin
            );
        }
        if ("region_controller".equals(ownerType)) {
            RegionTriggerType triggerType = parseRegionTrigger(bucket);
            if (triggerType == null) {
                return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                        "actionEdits[].bucket",
                        "logic_chain_region_action_bucket_invalid",
                        "Region action bucket 只支持 enter / exit / stay。",
                        bucket,
                        actionAppendNodeId(ownerType, ownerId),
                        "",
                        "",
                        "选择 Region 的 enter、exit 或 stay 动作桶。"
                )));
            }
            WebAdminRegionControllerRequests.ActionUpdateRequest request = new WebAdminRegionControllerRequests.ActionUpdateRequest();
            request.controllerId = ownerId;
            request.triggerType = triggerType.name();
            request.actionIndex = safeDraft.actionIndex;
            request.action = actionEntryForActionEditOperation(safeDraft);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return regionControllerService.updateAction(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    triggerType,
                    request,
                    csrfToken,
                    sameOrigin
            );
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "actionEdits[].ownerType",
                "logic_chain_action_edit_owner_type_deferred",
                "当前只支持编辑 SignalListener、Timer、ActionRelay 或 Region 上的已有 Action。",
                safeDraft.ownerType,
                actionAppendNodeId(ownerType, ownerId),
                "",
                "",
                "从可编辑 Action 卡片进入同 index 编辑；不支持删除或重排旧 Action。"
        )));
    }

    WebAdminWriteResult saveActionDelete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.ActionDeleteDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionDeleteDraft() : draft;
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = normalizedActionOwnerId(ownerType, safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        if ("listener".equals(ownerType)) {
            WebAdminSignalListenerActionRequests.ActionDeleteRequest request = new WebAdminSignalListenerActionRequests.ActionDeleteRequest();
            request.listenerId = ownerId;
            request.actionIndex = safeDraft.actionIndex;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return signalListenerActionsService.deleteAction(server, user, session, remoteAddress, ownerId, request, csrfToken, sameOrigin);
        }
        if ("timer".equals(ownerType)) {
            return timerService.deleteActionInBucket(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    bucket,
                    safeDraft.actionIndex,
                    Boolean.TRUE.equals(safeDraft.confirmed),
                    safeDraft.expectedFingerprint,
                    safeDraft.lockId,
                    csrfToken,
                    sameOrigin
            );
        }
        if ("action_relay".equals(ownerType)) {
            return actionRelayActionsService.deleteAction(
                    server,
                    user,
                    session,
                    remoteAddress,
                    ownerId,
                    parseActionIndex(safeDraft.actionIndex),
                    Boolean.TRUE.equals(safeDraft.confirmed),
                    safeDraft.expectedFingerprint,
                    safeDraft.lockId,
                    csrfToken,
                    sameOrigin
            );
        }
        if ("region_controller".equals(ownerType)) {
            RegionTriggerType triggerType = parseRegionTrigger(bucket);
            if (triggerType == null) {
                return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                        "actionDeletes[].bucket",
                        "logic_chain_region_action_bucket_invalid",
                        "Region action bucket 只支持 enter / exit / stay。",
                        bucket
                )));
            }
            WebAdminRegionControllerRequests.ActionDeleteRequest request = new WebAdminRegionControllerRequests.ActionDeleteRequest();
            request.controllerId = ownerId;
            request.triggerType = triggerType.name();
            request.actionIndex = safeDraft.actionIndex;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return regionControllerService.deleteAction(server, user, session, remoteAddress, ownerId, triggerType, request, csrfToken, sameOrigin);
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "actionDeletes[].ownerType",
                "logic_chain_action_delete_owner_type_deferred",
                "当前提交层只支持 SignalListener、Timer、ActionRelay 和 RegionController action 删除。",
                ownerType,
                actionAppendNodeId(ownerType, ownerId),
                "",
                "",
                "从可编辑 Action 卡片进入删除，保留 owner、bucket、index、lock 和 fingerprint。"
        )));
    }

    WebAdminWriteResult saveActionReorder(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.ActionReorderDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.ActionReorderDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionReorderDraft() : draft;
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = normalizedActionOwnerId(ownerType, safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        int fromIndex = parseActionIndex(safeDraft.fromIndex);
        int toIndex = parseActionIndex(safeDraft.toIndex);
        boolean confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
        if ("listener".equals(ownerType)) {
            return signalListenerActionsService.reorderAction(server, user, session, remoteAddress, ownerId, safeDraft.fromIndex, safeDraft.toIndex, confirmed, safeDraft.expectedFingerprint, safeDraft.lockId, csrfToken, sameOrigin);
        }
        if ("timer".equals(ownerType)) {
            return timerService.reorderActionInBucket(server, user, session, remoteAddress, ownerId, bucket, safeDraft.fromIndex, safeDraft.toIndex, confirmed, safeDraft.expectedFingerprint, safeDraft.lockId, csrfToken, sameOrigin);
        }
        if ("action_relay".equals(ownerType)) {
            return actionRelayActionsService.reorderAction(server, user, session, remoteAddress, ownerId, fromIndex, toIndex, confirmed, safeDraft.expectedFingerprint, safeDraft.lockId, csrfToken, sameOrigin);
        }
        if ("region_controller".equals(ownerType)) {
            RegionTriggerType triggerType = parseRegionTrigger(bucket);
            if (triggerType == null) {
                return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                        "actionReorders[].bucket",
                        "logic_chain_region_action_bucket_invalid",
                        "Region action bucket 只支持 enter / exit / stay。",
                        bucket
                )));
            }
            return regionControllerService.reorderAction(server, user, session, remoteAddress, ownerId, triggerType, safeDraft.fromIndex, safeDraft.toIndex, confirmed, safeDraft.expectedFingerprint, safeDraft.lockId, csrfToken, sameOrigin);
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "actionReorders[].ownerType",
                "logic_chain_action_reorder_owner_type_invalid",
                "Action 重排只支持 SignalListener、Timer、ActionRelay 或 RegionController。",
                ownerType,
                actionAppendNodeId(ownerType, ownerId),
                "",
                "",
                "从同一个 action 容器内进入重排。"
        )));
    }

    WebAdminWriteResult saveNodeDelete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft draft,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest.NodeDeleteDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.NodeDeleteDraft() : draft;
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        String targetId = safe(safeDraft.targetId);
        if ("signal_join".equals(nodeType)) {
            WebAdminSignalJoinRequest request = new WebAdminSignalJoinRequest();
            request.id = targetId;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return signalJoinService.delete(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
        }
        if ("timer".equals(nodeType)) {
            WebAdminTimerRequest request = new WebAdminTimerRequest();
            request.id = targetId;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return timerService.delete(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
        }
        if ("signal_listener".equals(nodeType)) {
            WebAdminSignalListenerDeleteRequest request = new WebAdminSignalListenerDeleteRequest();
            request.listenerId = targetId;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            request.reason = "Logic Chain typed-owned node delete";
            return signalListenerLifecycleService.delete(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
        }
        if ("virtual_block_device".equals(nodeType)) {
            WebAdminVirtualBlockDeviceDeleteRequest request = new WebAdminVirtualBlockDeviceDeleteRequest();
            request.deviceId = targetId;
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.confirmationText = NODE_DELETE_CONFIRMATION_TEXT;
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.reason = "Logic Chain VBD node delete: unbind only, keep world block";
            request.lockId = safeDraft.lockId;
            request.draftSessionId = "";
            return virtualBlockDeviceLifecycleService.delete(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
        }
        if (isPhysicalDeviceNodeType(nodeType)) {
            return savePhysicalDeviceNodeDelete(server, user, session, remoteAddress, nodeType, targetId, safeDraft);
        }
        if ("region_controller".equals(nodeType)) {
            WebAdminRegionControllerRequests.DeleteRequest request = new WebAdminRegionControllerRequests.DeleteRequest();
            request.confirmed = Boolean.TRUE.equals(safeDraft.confirmed);
            request.confirmationText = NODE_DELETE_CONFIRMATION_TEXT;
            request.expectedFingerprint = safeDraft.expectedFingerprint;
            request.lockId = safeDraft.lockId;
            return regionControllerService.delete(server, user, session, remoteAddress, targetId, request, csrfToken, sameOrigin);
        }
        return WebAdminWriteResult.validationFailed(target(new WebAdminLogicChainEditorRequest()), List.of(error(
                "nodeDeletes[].nodeType",
                "logic_chain_node_delete_commit_fail_closed",
                "该节点不能从 Logic Chain 直接删除：只有能证明 owner/store 的 typed-owned resource 才允许提交删除。",
                safeDraft.nodeType,
                existingNodeIdForError(nodeType, targetId),
                "",
                channelForExistingNode(nodeType, targetId),
                "打开对应管理页面删除 owner typed resource；引用节点和 projection 节点不能直接删。"
        )));
    }

    private WebAdminWriteResult savePhysicalDeviceNodeDelete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String nodeType,
            String targetId,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft draft
    ) {
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, targetId);
        SignalDeviceData device = resolved.foundUnique() ? resolved.device().normalized() : null;
        WebAdminWriteTarget writeTarget = new WebAdminWriteTarget("DEVICE_BASIC_CONFIG", device == null ? targetId : device.id(), device == null ? targetId : WebAdminReadonlySupport.deviceDisplayName(device));
        if (device == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, writeTarget, "实体设备不存在或已被删除。");
        }
        if (!normalizeExistingNodeEditType(nodeType).equals(device.type())) {
            return WebAdminWriteResult.validationFailed(writeTarget, List.of(error(
                    "nodeDeletes[].nodeType",
                    "logic_chain_physical_device_type_mismatch",
                    "删除目标设备类型与画布节点类型不一致，已 fail closed。",
                    nodeType,
                    existingNodeIdForError(nodeType, targetId),
                    "",
                    device.channel(),
                    "刷新 Logic Chain 后重新从实体设备主节点进入删除。"
            )));
        }
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG);
        if (!permission.allowed()) {
            return permission.asWriteResult(writeTarget);
        }
        if (draft == null || safe(draft.expectedFingerprint).isBlank()) {
            return WebAdminWriteResult.validationFailed(writeTarget, List.of(error("nodeDeletes[].expectedFingerprint", "required", "删除实体设备需要 expectedFingerprint。", "", existingNodeIdForError(nodeType, targetId), "", device.channel(), "重新打开删除确认，获取最新设备指纹。")));
        }
        if (!WebAdminDeviceBasicConfigService.fingerprintMatches(device, draft.expectedFingerprint)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CONFLICT_DETECTED, writeTarget, "实体设备已被其它操作修改，请刷新后重新确认删除。");
        }
        if (!Boolean.TRUE.equals(draft.confirmed) || !NODE_DELETE_CONFIRMATION_TEXT.equals(safe(draft.confirmationText).trim())) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, writeTarget, "删除实体设备需要输入固定文本“我确认删除该节点”。");
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, device.id(), draft.lockId, user, session);
            if (!lockValidation.success()) {
                return lockValidation.result();
            }
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (world == null || !loadedPhysicalDevicePresent(server, device)) {
            return WebAdminWriteResult.validationFailed(writeTarget, List.of(error(
                    "nodeDeletes[].targetId",
                    "logic_chain_physical_device_missing_or_broken",
                    "实体设备方块不存在、区块未加载或方块实体类型不匹配，删除已 fail closed。",
                    device.id(),
                    existingNodeIdForError(nodeType, targetId),
                    "",
                    device.channel(),
                    "刷新 Logic Chain；如果方块已被外部破坏，先确认 missing/broken 状态后再处理注册信息。"
            )));
        }
        WebAdminPhysicalDeviceDeletionSupport.DeleteResult deletion = WebAdminPhysicalDeviceDeletionSupport.deletePhysicalDevice(server, device);
        if (!deletion.worldBlockRemoved()) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, writeTarget, "实体设备方块删除失败，注册信息未移除。");
        }
        if (editLockService != null) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, device.id(), draft.lockId, user, session, remoteAddress);
        }
        return ok(writeTarget, deletion.registryRemoved() ? "实体设备方块和注册信息已删除。" : "实体设备方块已移除，注册信息已不存在。", Map.of(
                "deviceId", device.id(),
                "deviceType", device.type(),
                "worldBlockRemoved", true,
                "registryRemoved", deletion.registryRemoved(),
                "metadataRemoved", deletion.metadataRemoved()
        ));
    }

    WebAdminWriteResult saveChannelMetadataDrafts(
            MinecraftServer server,
            WebAdminUser user,
            List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> drafts
    ) {
        if (channelMetadataService != null) {
            return channelMetadataService.saveDraftsFromLogicChainEditor(server, user, drafts);
        }
        List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> safeDrafts = drafts == null ? List.of() : drafts;
        if (safeDrafts.isEmpty() || server == null) {
            return WebAdminWriteResult.ok(new WebAdminWriteTarget("CHANNEL_METADATA", "drafts", "频道端点 metadata"), false, "无频道 metadata 草稿。");
        }
        WebAdminChannelMetadataStore.MetadataFile file = WebAdminChannelMetadataStore.load(server);
        boolean changed = false;
        String actor = user == null ? "" : safe(user.username);
        for (WebAdminLogicChainEditorRequest.ChannelMetadataDraft draft : safeDrafts) {
            String channel = SignalChannel.normalize(draft == null ? "" : draft.channel);
            if (channel.isBlank()) {
                continue;
            }
            WebAdminChannelMetadataStore.MetadataEntry before = WebAdminChannelMetadataStore.MetadataEntry.normalized(channel, file.channels.get(channel));
            WebAdminChannelMetadataStore.MetadataEntry after = new WebAdminChannelMetadataStore.MetadataEntry();
            after.channel = channel;
            after.displayName = cleanMetadataText(draft.displayName, WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH);
            after.note = cleanMetadataText(draft.note, WebAdminChannelMetadataService.MAX_NOTE_LENGTH);
            after.iconKey = normalizeIcon(draft.iconKey);
            after.updatedAt = Instant.now().toString();
            after.updatedBy = actor;
            after.version = before.version + 1L;
            if (safe(before.displayName).equals(after.displayName)
                    && safe(before.note).equals(after.note)
                    && safe(before.iconKey).equals(after.iconKey)
                    && file.channels.containsKey(channel)) {
                continue;
            }
            file.channels.put(channel, after);
            changed = true;
        }
        if (!changed) {
            return WebAdminWriteResult.noChange(new WebAdminWriteTarget("CHANNEL_METADATA", "drafts", "频道端点 metadata"), "频道端点 metadata 无变化。");
        }
        if (!WebAdminChannelMetadataStore.save(server, file)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, new WebAdminWriteTarget("CHANNEL_METADATA", "drafts", "频道端点 metadata"), "频道端点 metadata 保存失败。");
        }
        return WebAdminWriteResult.ok(new WebAdminWriteTarget("CHANNEL_METADATA", "drafts", "频道端点 metadata"), true, "频道端点 metadata 已保存。");
    }

    private WebAdminSignalJoinRequest deriveSignalJoinRequestFromEdges(
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges
    ) {
        WebAdminSignalJoinRequest source = node == null || node.signalJoin == null ? new WebAdminSignalJoinRequest() : node.signalJoin;
        WebAdminSignalJoinRequest derived = new WebAdminSignalJoinRequest();
        derived.id = source.id;
        derived.displayName = source.displayName;
        derived.note = source.note;
        derived.enabled = source.enabled;
        derived.mode = source.mode;
        derived.threshold = source.threshold;
        derived.scopeMode = source.scopeMode;
        derived.resetPolicy = source.resetPolicy;
        derived.timeoutTicks = source.timeoutTicks;
        derived.cooldownTicks = source.cooldownTicks;
        derived.expectedFingerprint = source.expectedFingerprint;
        derived.confirmed = source.confirmed;
        derived.reason = source.reason;
        derived.scopeKey = source.scopeKey;
        List<SignalJoinInputDefinition> inputs = new ArrayList<>();
        int index = 1;
        for (String channelRef : channelRefsFromEdges(edges, "", node == null ? "" : node.id, "join_input", true)) {
            String channel = channelName(channelRef);
            if (!channel.isBlank()) {
                inputs.add(new SignalJoinInputDefinition(channel, "", "", index++));
            }
        }
        derived.inputChannels = inputs;
        derived.outputChannel = channelRefsFromEdges(edges, node == null ? "" : node.id, "", "join_output", false)
                .stream()
                .findFirst()
                .map(WebAdminLogicChainEditorService::channelName)
                .orElse("");
        return derived;
    }

    private WebAdminTimerRequest deriveTimerRequestFromEdges(
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges
    ) {
        WebAdminTimerRequest source = node == null || node.timer == null ? new WebAdminTimerRequest() : node.timer;
        WebAdminTimerRequest derived = new WebAdminTimerRequest();
        derived.id = source.id;
        derived.displayName = source.displayName;
        derived.note = source.note;
        derived.enabled = source.enabled;
        derived.mode = source.mode;
        derived.scopeMode = source.scopeMode;
        derived.durationTicks = source.durationTicks;
        derived.intervalTicks = source.intervalTicks;
        derived.maxRuns = source.maxRuns;
        derived.startPolicy = source.startPolicy;
        derived.onStartActions = source.onStartActions == null ? new ArrayList<>() : new ArrayList<>(source.onStartActions);
        derived.onTickActions = source.onTickActions == null ? new ArrayList<>() : new ArrayList<>(source.onTickActions);
        derived.onCompleteActions = source.onCompleteActions == null ? new ArrayList<>() : new ArrayList<>(source.onCompleteActions);
        derived.onCancelActions = source.onCancelActions == null ? new ArrayList<>() : new ArrayList<>(source.onCancelActions);
        derived.outputChannel = channelRefsFromEdges(edges, node == null ? "" : node.id, "", "timer_outputs_channel", false)
                .stream()
                .findFirst()
                .map(WebAdminLogicChainEditorService::channelName)
                .orElse("");
        derived.expectedFingerprint = source.expectedFingerprint;
        derived.confirmed = source.confirmed;
        derived.reason = source.reason;
        derived.scopeKey = source.scopeKey;
        derived.targetMode = source.targetMode;
        derived.targetId = source.targetId;
        derived.startPolicyOverride = source.startPolicyOverride;
        return derived;
    }

    private WebAdminSignalListenerCreateRequest deriveSignalListenerRequestFromEdges(
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges
    ) {
        WebAdminSignalListenerCreateRequest source = node == null || node.signalListener == null ? new WebAdminSignalListenerCreateRequest() : node.signalListener;
        WebAdminSignalListenerCreateRequest derived = new WebAdminSignalListenerCreateRequest();
        derived.name = source.name;
        derived.displayName = source.displayName;
        derived.enabled = source.enabled;
        derived.cooldownTicks = source.cooldownTicks;
        derived.conditionGroupId = source.conditionGroupId;
        derived.actions = source.actions == null ? List.of() : List.copyOf(source.actions);
        derived.note = source.note;
        derived.channel = channelRefsFromEdges(edges, "", node == null ? "" : node.id, "consumes", true)
                .stream()
                .findFirst()
                .map(WebAdminLogicChainEditorService::channelName)
                .orElse(source.channel);
        return derived;
    }

    private WebAdminWriteResult acquireTypedLock(
            String targetType,
            String targetId,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String csrfToken,
            boolean sameOrigin
    ) {
        if (editLockService == null) {
            return ok(new WebAdminWriteTarget("EDIT_LOCK", targetType + ":" + targetId, "WebAdmin 编辑锁"), "无需编辑锁。", Map.of("lock", Map.of("lockId", "")));
        }
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = targetType;
        request.targetId = targetId;
        return editLockService.acquire(user, session, remoteAddress, request, csrfToken, sameOrigin);
    }

    private void releaseTypedLockOnFailure(
            String targetType,
            String targetId,
            String lockId,
            WebAdminWriteResult result,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService != null && result != null && !result.success() && !safe(lockId).isBlank()) {
            editLockService.releaseForSessionCleanup(targetType, targetId, lockId, user, session, remoteAddress, "保存未完成，已释放临时配置编辑锁。");
        }
    }

    WebAdminWriteResult logicChainSaveFailurePreservingEditorLock(
            WebAdminLogicChainEditorRequest request,
            WebAdminWriteResult result,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit,
            WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete,
            WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder
    ) {
        if (result == null || result.success()) {
            return result;
        }
        Map<String, Object> data = new LinkedHashMap<>(result.data());
        data.put("editorLockLost", false);
        data.put("draftPreserved", true);
        data.put("editorLockTargetType", WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR);
        data.put("editorLockTargetId", targetId(request));
        data.put("typedFailure", Map.of(
                "code", safe(result.code()),
                "targetType", safe(result.targetType()),
                "targetId", safe(result.targetId())
        ));
        List<WebAdminValidationError> errors = logicChainEnrichTypedFailureErrors(result, draftNode, actionAppend, existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
        return new WebAdminWriteResult(
                false,
                result.code(),
                result.message(),
                result.targetType(),
                result.targetId(),
                false,
                errors,
                result.auditId(),
                result.realtimeEventId(),
                result.requiresConfirmation(),
                result.conflict(),
                data
        );
    }

    private static List<WebAdminValidationError> logicChainEnrichTypedFailureErrors(
            WebAdminWriteResult result,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit,
            WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete,
            WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder
    ) {
        String nodeId = logicChainDraftContextNodeId(draftNode, actionAppend, existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
        String fallbackHint = logicChainTypedFailureFixHint(result == null ? "" : result.code());
        List<WebAdminValidationError> source = result == null ? List.of() : result.validationErrors();
        if (source.isEmpty()) {
            return List.of(new WebAdminValidationError(
                    "typedWrite",
                    safe(result == null ? "" : result.code()),
                    safe(result == null ? "" : result.message()).isBlank() ? "底层配置保存失败，Logic Chain 编辑锁和草稿已保留。" : result.message(),
                    safe(result == null ? "" : result.targetId()),
                    nodeId,
                    "",
                    "",
                    "error",
                    fallbackHint
            ));
        }
        List<WebAdminValidationError> enriched = new ArrayList<>();
        for (WebAdminValidationError error : source) {
            enriched.add(new WebAdminValidationError(
                    error.field(),
                    error.code(),
                    error.message(),
                    error.rejectedValueSummary(),
                    safe(error.nodeId()).isBlank() ? nodeId : error.nodeId(),
                    error.edgeId(),
                    error.channelId(),
                    error.severity(),
                    safe(error.fixHint()).isBlank() ? fallbackHint : error.fixHint()
            ));
        }
        return List.copyOf(enriched);
    }

    private static String logicChainDraftContextNodeId(
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit,
            WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete,
            WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder
    ) {
        if (draftNode != null && !safe(draftNode.id).isBlank()) {
            return safe(draftNode.id);
        }
        if (actionAppend != null) {
            String ownerType = normalizeOwnerType(actionAppend.ownerType);
            String ownerId = safe(actionAppend.ownerId);
            return ownerId.isBlank() ? ownerType : ownerType + ":" + ownerId;
        }
        if (existingNodeEdit != null) {
            String type = normalizeExistingNodeEditType(existingNodeEdit.nodeType);
            String id = safe(existingNodeEdit.targetId);
            return id.isBlank() ? type : type + ":" + id;
        }
        if (actionEdit != null) {
            String ownerType = normalizeOwnerType(actionEdit.ownerType);
            String ownerId = safe(actionEdit.ownerId);
            return ownerId.isBlank() ? ownerType : ownerType + ":" + ownerId + ":action:" + parseActionIndex(actionEdit.actionIndex);
        }
        if (nodeDelete != null) {
            String type = normalizeExistingNodeEditType(nodeDelete.nodeType);
            String id = safe(nodeDelete.targetId);
            return id.isBlank() ? type : type + ":" + id;
        }
        if (actionDelete != null) {
            String ownerType = normalizeOwnerType(actionDelete.ownerType);
            String ownerId = safe(actionDelete.ownerId);
            return ownerId.isBlank() ? ownerType : ownerType + ":" + ownerId + ":action:" + parseActionIndex(actionDelete.actionIndex);
        }
        if (actionReorder != null) {
            String ownerType = normalizeOwnerType(actionReorder.ownerType);
            String ownerId = safe(actionReorder.ownerId);
            return ownerId.isBlank() ? ownerType : ownerType + ":" + ownerId + ":action:" + parseActionIndex(actionReorder.fromIndex);
        }
        return "";
    }

    private static String logicChainTypedFailureFixHint(String code) {
        if (WebAdminWriteResultCode.EDIT_LOCK_REQUIRED.id().equals(code)
                || WebAdminWriteResultCode.EDIT_LOCK_EXPIRED.id().equals(code)
                || WebAdminWriteResultCode.EDIT_LOCK_CONFLICT.id().equals(code)) {
            return "底层配置编辑锁不可用；Logic Chain 编辑锁和草稿已保留，请稍后重试或释放目标配置锁后再保存。";
        }
        return "Logic Chain 编辑锁和草稿已保留；按此错误修正当前草稿后可继续保存。";
    }

    WebAdminWriteResult validateEditorLock(WebAdminUser user, WebAdminSession session, WebAdminLogicChainEditorRequest request) {
        if (editLockService == null) {
            return WebAdminWriteResult.ok(target(request), false, "编辑锁校验已跳过。");
        }
        String expectedTargetType = WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR;
        String expectedTargetId = targetId(request);
        String submittedTargetType = safe(request == null ? "" : request.lockTargetType);
        String submittedTargetId = safe(request == null ? "" : request.lockTargetId);
        if (!submittedTargetType.isBlank() && !expectedTargetType.equals(submittedTargetType)) {
            return WebAdminWriteResult.validationFailed(target(request), List.of(error(
                    "lockTargetType",
                    "logic_chain_editor_lock_target_mismatch",
                    "逻辑链编辑锁目标不一致，请刷新后重新进入编辑模式。",
                    submittedTargetType,
                    "",
                    "",
                    "",
                    "重新进入逻辑链编辑模式，使用后端返回的 canonical lock target 后再保存。"
            )));
        }
        if (!submittedTargetId.isBlank() && !expectedTargetId.equals(submittedTargetId)) {
            return WebAdminWriteResult.validationFailed(target(request), List.of(error(
                    "lockTargetId",
                    "logic_chain_editor_lock_target_mismatch",
                    "逻辑链编辑锁目标不一致，请刷新后重新进入编辑模式。",
                    submittedTargetId,
                    "",
                    "",
                    "",
                    "重新进入逻辑链编辑模式，避免使用刷新前的 lock target。"
            )));
        }
        WebAdminEditLockService.LockValidation validation = editLockService.validateLock(
                expectedTargetType,
                expectedTargetId,
                request.lockId,
                user,
                session
        );
        return validation.success()
                ? WebAdminWriteResult.ok(target(request), false, "逻辑链编辑锁有效。")
                : logicChainEditorLockFailure(request, validation.result());
    }

    private WebAdminWriteResult logicChainEditorLockFailure(WebAdminLogicChainEditorRequest request, WebAdminWriteResult result) {
        WebAdminWriteResult safeResult = result == null
                ? WebAdminWriteResult.failed(WebAdminWriteResultCode.EDIT_LOCK_EXPIRED, target(request), "编辑锁不存在或已过期，请重新进入编辑。")
                : result;
        String code = safe(safeResult.code()).isBlank() ? "edit_lock_expired" : safeResult.code();
        String message = safe(safeResult.message()).isBlank() ? "编辑锁不存在或已过期，请重新进入编辑模式。" : safeResult.message();
        String fixHint = switch (code) {
            case "edit_lock_required" -> "重新进入逻辑链编辑模式获取编辑锁后再保存，当前草稿不会被服务端当作成功保存。";
            case "edit_lock_conflict" -> "当前对象可能已被其它会话占用；请刷新确认锁状态，必要时重新进入编辑模式。";
            default -> "编辑锁已失效；请重新进入逻辑链编辑模式，确认草稿后再保存。";
        };
        List<WebAdminValidationError> errors = safeResult.validationErrors().isEmpty()
                ? List.of(error("lockId", code, message, safe(request == null ? "" : request.lockId), "", "", "", fixHint))
                : safeResult.validationErrors();
        Map<String, Object> data = new LinkedHashMap<>(safeResult.data());
        data.put("editorLockLost", true);
        data.put("draftPreserved", true);
        data.put("editorLockTargetType", WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR);
        data.put("editorLockTargetId", targetId(request));
        data.put("submittedLockTargetType", safe(request == null ? "" : request.lockTargetType));
        data.put("submittedLockTargetId", safe(request == null ? "" : request.lockTargetId));
        return new WebAdminWriteResult(
                false,
                code,
                message,
                safeResult.targetType(),
                safeResult.targetId(),
                false,
                errors,
                safeResult.auditId(),
                safeResult.realtimeEventId(),
                safeResult.requiresConfirmation(),
                safeResult.conflict(),
                data
        );
    }

    WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_LOGIC_CHAIN);
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
        return WebAdminWriteResult.ok(target, false, "Logic Chain Editor 写入前置检查通过。");
    }

    private Map<String, Object> editorData(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminLogicChainEditorRequest request,
            Map<String, Object> lockData
    ) {
        WebAdminDtos.LogicChainGraphDto graph = currentGraph(server, user, session, request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("capabilities", capabilities(user));
        data.put("graph", graph);
        data.put("baseGraphFingerprint", graphFingerprintFor(graph));
        data.put("rootType", normalizeRootType(request.rootType));
        data.put("rootRef", safe(request.rootRef));
        data.put("includeDisabled", request.includeDisabled);
        data.put("maxDepth", request.maxDepth <= 0 ? 3 : request.maxDepth);
        data.put("targetType", WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR);
        data.put("targetId", targetId(request));
        data.put("lock", lockData == null ? Map.of() : lockData.getOrDefault("lock", Map.of()));
        data.put("draftPreserved", true);
        return data;
    }

    List<WebAdminValidationError> validateDraftRequest(
            WebAdminLogicChainEditorRequest request,
            WebAdminDtos.LogicChainGraphDto graph,
            boolean requireComplete,
            WebAdminUser user,
            WebAdminSession session
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        List<WebAdminLogicChainEditorRequest.DraftNode> nodes = request.nodes == null ? List.of() : request.nodes;
        List<WebAdminLogicChainEditorRequest.DraftEdge> edges = request.edges == null ? List.of() : request.edges;
        boolean actionAppend = hasActionAppend(request);
        boolean existingNodeEdit = hasExistingNodeEdit(request);
        boolean actionEdit = hasActionEdit(request);
        boolean nodeDelete = hasNodeDelete(request);
        boolean actionDelete = hasActionDelete(request);
        boolean actionReorder = hasActionReorder(request);
        validateChannelMetadataDrafts(request, errors);
        boolean hasAnyWrite = !nodes.isEmpty() || actionAppend || existingNodeEdit || actionEdit || nodeDelete || actionDelete || actionReorder;
        if (!hasAnyWrite) {
            errors.add(error("nodes", "logic_chain_draft_node_required", "请先新增一个草稿节点、编辑/删除已有节点，或从已有 action 容器追加、编辑、删除、重排 1 条 Action。", "", "", "", "", "点击“新增节点”、点击可编辑旧节点，或从已有 action 容器选择维护入口。"));
        }
        if (actionAppend && nodes.isEmpty() && !edges.isEmpty()) {
            errors.add(error("edges", "logic_chain_action_append_edges_not_allowed", "Action 追加草稿不能携带新增节点连线。", String.valueOf(edges.size()), "", "", "", "Action 追加只修改目标 action list；新增节点连线请使用新增节点草稿。"));
        } else if ((existingNodeEdit || actionEdit || nodeDelete || actionDelete || actionReorder) && nodes.isEmpty() && !edges.isEmpty()) {
            errors.add(error("edges", "logic_chain_existing_edit_edges_not_allowed", "已有节点 / Action 维护草稿不能携带新增节点连线。", String.valueOf(edges.size()), "", "", "", "局部重连会保存为具体配置字段，不通过 draft edges 修改旧图。"));
        }
        validateDuplicateDraftNodes(request, errors);
        validateDuplicateExistingNodeEdits(request, errors);
        validateDuplicateActionEdits(request, errors);
        validateDuplicateNodeDeletes(request, errors);
        validateSingleNodeDeletePerSave(request, errors);
        validateDuplicateActionDeletes(request, errors);
        validateDuplicateActionReorders(request, errors);
        validateCrossDraftTargetConflicts(request, errors);
        validateSameActionTargetMultiWriteConflicts(request, errors);
        if (nodes.size() > MAX_DRAFT_NODES_PER_SAVE) {
            errors.add(error("nodes", "logic_chain_draft_nodes_too_many", "一次保存最多支持 " + MAX_DRAFT_NODES_PER_SAVE + " 个新增草稿节点。", String.valueOf(nodes.size()), "", "", "", "减少本次新增节点数量，或分批保存。"));
        }
        if (edges.size() > MAX_DRAFT_EDGES_PER_SAVE) {
            errors.add(error("edges", "logic_chain_draft_too_many_edges", "草稿连线过多；8.14 MVP 只允许必要上下游连线。", String.valueOf(edges.size()), "", "", "", "移除多余草稿连线，只保留当前新增节点所需的输入和输出频道。"));
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        if (actionAppend) {
            validateActionAppendDraft(request.actionAppend, errors);
        }
        if (existingNodeEdit) {
            int editIndex = 0;
            for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request.existingNodeEdits == null ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of() : request.existingNodeEdits) {
                if (isExistingNodeEditDraftPresent(edit)) {
                    validateExistingNodeEditDraft(graph, edit, editIndex, errors);
                }
                editIndex++;
            }
        }
        if (actionEdit) {
            int editIndex = 0;
            for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
                if (isActionEditDraftPresent(edit)) {
                    validateActionEditDraft(graph, edit, editIndex, errors);
                }
                editIndex++;
            }
        }
        if (nodeDelete) {
            int deleteIndex = 0;
            for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request.nodeDeletes == null ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of() : request.nodeDeletes) {
                if (isNodeDeleteDraftPresent(delete)) {
                    validateNodeDeleteDraft(graph, delete, deleteIndex, errors);
                }
                deleteIndex++;
            }
        }
        if (actionDelete) {
            int deleteIndex = 0;
            for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
                if (isActionDeleteDraftPresent(delete)) {
                    validateActionDeleteDraft(graph, delete, deleteIndex, errors);
                }
                deleteIndex++;
            }
        }
        if (actionReorder) {
            int reorderIndex = 0;
            for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request.actionReorders == null ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of() : request.actionReorders) {
                if (isActionReorderDraftPresent(reorder)) {
                    validateActionReorderDraft(graph, reorder, reorderIndex, errors);
                }
                reorderIndex++;
            }
        }
        if (nodes.isEmpty()) {
            validateTargetLockPreflight(request, user, session, errors);
            return errors;
        }

        validateDraftEdgesAttachedToKnownDrafts(nodes, edges, errors);
        int nodeIndex = 0;
        for (WebAdminLogicChainEditorRequest.DraftNode node : nodes) {
            validateNewDraftNode(request, graph, user, node, edgesForDraftNode(edges, safe(node == null ? "" : node.id)), requireComplete, nodeIndex, errors);
            nodeIndex++;
        }
        validateTargetLockPreflight(request, user, session, errors);
        return errors;
    }

    private void validateNewDraftNode(
            WebAdminLogicChainEditorRequest request,
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminUser user,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> nodeEdges,
            boolean requireComplete,
            int nodeIndex,
            List<WebAdminValidationError> errors
    ) {
        String prefix = "nodes[" + Math.max(0, nodeIndex) + "]";
        WebAdminLogicChainEditorRequest.DraftNode safeNode = node == null ? new WebAdminLogicChainEditorRequest.DraftNode() : node;
        String nodeType = normalizeNodeType(safeNode.type);
        String nodeId = safe(safeNode.id);
        if (!nodeId.startsWith("draft:")) {
            errors.add(error(prefix + ".id", "logic_chain_draft_node_id_invalid", "新增节点 ID 必须是 draft: 前缀，不能指向旧节点。", nodeId, nodeId, "", "", "请重新新增节点，不要复用旧图节点 ID。"));
        }
        if (!SUPPORTED_NODE_TYPES.contains(nodeType)) {
            errors.add(error(prefix + ".type", "logic_chain_node_type_deferred", "当前不支持该节点类型。", safeNode.type, nodeId, "", "", "请选择 Signal Join、Timer、SignalListener、世界设备引用、VBD 或区域控制器；不要手写 fake draft。"));
        }
        if (!safeNode.placed) {
            errors.add(error(prefix + ".placed", "logic_chain_draft_node_not_placed", "请先把新节点放入合法槽位。", "false", nodeId, "", "", "把草稿卡片拖到白色合法 slot 后再保存。"));
        }
        validateSlot(safeNode, nodeType, prefix, errors);
        validateEdges(graph, safeNode, nodeEdges, errors);

        if ("signal_join".equals(nodeType)) {
            validateSignalJoinDraft(graph, safeNode, nodeEdges, requireComplete, errors);
        } else if ("timer".equals(nodeType)) {
            validateTimerDraft(safeNode, nodeEdges, requireComplete, errors);
        } else if ("signal_listener".equals(nodeType)) {
            validateSignalListenerDraft(safeNode, nodeEdges, requireComplete, prefix, errors);
        } else if ("virtual_block_device".equals(nodeType)) {
            validateVirtualBlockDeviceDraft(request, user, safeNode, nodeEdges, requireComplete, prefix, errors);
        } else if ("world_device".equals(nodeType) || "region_controller".equals(nodeType)) {
            validateProtectedWorldBackedDraft(request, user, safeNode, nodeEdges, nodeType, requireComplete, prefix, errors);
        }
    }

    private static List<WebAdminLogicChainEditorRequest.DraftEdge> edgesForDraftNode(
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String draftNodeId
    ) {
        if (safe(draftNodeId).isBlank()) {
            return List.of();
        }
        List<WebAdminLogicChainEditorRequest.DraftEdge> filtered = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : edges) {
            if (draftNodeId.equals(safe(edge == null ? "" : edge.from)) || draftNodeId.equals(safe(edge == null ? "" : edge.to))) {
                filtered.add(edge);
            }
        }
        return filtered;
    }

    private static void validateDraftEdgesAttachedToKnownDrafts(
            List<WebAdminLogicChainEditorRequest.DraftNode> nodes,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            List<WebAdminValidationError> errors
    ) {
        Set<String> draftNodeIds = new LinkedHashSet<>();
        for (WebAdminLogicChainEditorRequest.DraftNode node : nodes == null ? List.<WebAdminLogicChainEditorRequest.DraftNode>of() : nodes) {
            String nodeId = safe(node == null ? "" : node.id);
            if (!nodeId.isBlank()) {
                draftNodeIds.add(nodeId);
            }
        }
        int index = 0;
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : edges) {
            String from = safe(edge == null ? "" : edge.from);
            String to = safe(edge == null ? "" : edge.to);
            if (from.isBlank() || to.isBlank()) {
                errors.add(error("edges[" + index + "]", "logic_chain_edge_incomplete", "草稿连线必须包含起点、终点和类型。", from + ">" + to, "", safe(edge == null ? "" : edge.id), edgeChannelId(edge), "重新点击绿色加号选择频道端点，生成完整连线。"));
            } else if (!draftNodeIds.contains(from) && !draftNodeIds.contains(to)) {
                errors.add(error("edges[" + index + "]", "logic_chain_edge_not_incident_to_draft", "草稿连线必须连接到本次新增草稿节点，不能伪造旧节点之间的连线。", from + ">" + to, "", safe(edge == null ? "" : edge.id), edgeChannelId(edge), "删除该连线；旧节点局部维护必须通过对应 typed 草稿字段提交。"));
            }
            index++;
        }
    }

    private void validateTargetLockPreflight(
            WebAdminLogicChainEditorRequest request,
            WebAdminUser user,
            WebAdminSession session,
            List<WebAdminValidationError> errors
    ) {
        // logic_chain_target_lock_preflight_validation
        if (editLockService == null) {
            return;
        }
        Set<String> checked = new LinkedHashSet<>();
        for (TargetLockPreflight lock : targetLockPreflightRequirements(request)) {
            if (lock.targetType().isBlank() || lock.targetId().isBlank()) {
                continue;
            }
            String key = lock.field() + "\n" + lock.targetType() + "\n" + lock.targetId() + "\n" + lock.lockId();
            if (!checked.add(key)) {
                continue;
            }
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(lock.targetType(), lock.targetId(), lock.lockId(), user, session);
            if (!validation.success()) {
                WebAdminWriteResult result = validation.result();
                errors.add(error(
                        lock.field(),
                        "logic_chain_target_lock_preflight_validation",
                        "目标配置编辑锁校验失败：" + safe(result == null ? "" : result.message()),
                        lock.targetType() + ":" + lock.targetId(),
                        lock.label(),
                        "",
                        "",
                        "重新打开该目标的编辑/维护面板，获取有效目标编辑锁后再保存；Logic Chain 编辑锁不等于目标配置锁。"
                ));
            }
        }
    }

    private static List<TargetLockPreflight> targetLockPreflightRequirements(WebAdminLogicChainEditorRequest request) {
        if (request == null) {
            return List.of();
        }
        List<TargetLockPreflight> locks = new ArrayList<>();
        if (hasActionAppend(request)) {
            String ownerType = normalizeOwnerType(request.actionAppend.ownerType);
            String ownerId = normalizedActionOwnerId(ownerType, request.actionAppend.ownerId);
            locks.add(new TargetLockPreflight("actionAppend.lockId", actionOwnerLockTargetType(ownerType), ownerId, request.actionAppend.lockId, actionAppendNodeId(ownerType, ownerId)));
        }
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request.existingNodeEdits == null ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of() : request.existingNodeEdits) {
            if (isExistingNodeEditDraftPresent(edit)) {
                locks.add(existingNodeEditTargetLock(edit, index));
                TargetLockPreflight metadataLock = existingNodeEditMetadataTargetLock(edit, index);
                if (!metadataLock.targetType().isBlank()) {
                    locks.add(metadataLock);
                }
                TargetLockPreflight nativeTriggerLock = existingNodeEditNativeTriggerTargetLock(edit, index);
                if (!nativeTriggerLock.targetType().isBlank()) {
                    locks.add(nativeTriggerLock);
                }
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
            if (isActionEditDraftPresent(edit)) {
                String ownerType = normalizeOwnerType(edit.ownerType);
                String ownerId = normalizedActionOwnerId(ownerType, edit.ownerId);
                locks.add(new TargetLockPreflight("actionEdits[" + index + "].lockId", actionOwnerLockTargetType(ownerType), ownerId, edit.lockId, actionAppendNodeId(ownerType, ownerId)));
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request.nodeDeletes == null ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of() : request.nodeDeletes) {
            if (isNodeDeleteDraftPresent(delete)) {
                String nodeType = normalizeExistingNodeEditType(delete.nodeType);
                String targetId = nodeDeleteLockTargetId(nodeType, delete.targetId);
                locks.add(new TargetLockPreflight("nodeDeletes[" + index + "].lockId", nodeDeleteLockTargetType(nodeType), targetId, delete.lockId, existingNodeIdForError(nodeType, targetId)));
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
            if (isActionDeleteDraftPresent(delete)) {
                String ownerType = normalizeOwnerType(delete.ownerType);
                String ownerId = normalizedActionOwnerId(ownerType, delete.ownerId);
                locks.add(new TargetLockPreflight("actionDeletes[" + index + "].lockId", actionOwnerLockTargetType(ownerType), ownerId, delete.lockId, actionAppendNodeId(ownerType, ownerId)));
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request.actionReorders == null ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of() : request.actionReorders) {
            if (isActionReorderDraftPresent(reorder)) {
                String ownerType = normalizeOwnerType(reorder.ownerType);
                String ownerId = normalizedActionOwnerId(ownerType, reorder.ownerId);
                locks.add(new TargetLockPreflight("actionReorders[" + index + "].lockId", actionOwnerLockTargetType(ownerType), ownerId, reorder.lockId, actionAppendNodeId(ownerType, ownerId)));
            }
            index++;
        }
        return locks;
    }

    private static TargetLockPreflight existingNodeEditTargetLock(
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit,
            int index
    ) {
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft safeDraft = edit == null ? new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft() : edit;
        String prefix = "existingNodeEdits[" + Math.max(0, index) + "]";
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        String targetId = safe(safeDraft.targetId);
        return switch (nodeType) {
            case "channel_metadata" -> {
                WebAdminChannelMetadataUpdateRequest request = safeDraft.channelMetadata == null ? new WebAdminChannelMetadataUpdateRequest() : safeDraft.channelMetadata;
                String channel = SignalChannel.normalize(safe(request.channel).isBlank() ? targetId : request.channel);
                yield new TargetLockPreflight(prefix + ".channelMetadata.lockId", WebAdminEditLockService.TARGET_CHANNEL_METADATA, channel, request.lockId, "channel:" + channel);
            }
            case "signal_join" -> {
                WebAdminSignalJoinRequest request = safeDraft.signalJoin == null ? new WebAdminSignalJoinRequest() : safeDraft.signalJoin;
                String joinId = SignalJoinStore.normalizeId(safe(request.id).isBlank() ? targetId : request.id);
                yield new TargetLockPreflight(prefix + ".signalJoin.lockId", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, request.lockId, "signal_join:" + joinId);
            }
            case "timer" -> {
                WebAdminTimerRequest request = safeDraft.timer == null ? new WebAdminTimerRequest() : safeDraft.timer;
                String timerId = TimerStore.normalizeId(safe(request.id).isBlank() ? targetId : request.id);
                yield new TargetLockPreflight(prefix + ".timer.lockId", WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, request.lockId, "timer:" + timerId);
            }
            case "signal_listener" -> {
                WebAdminSignalListenerBasicConfigUpdateRequest request = safeDraft.signalListenerBasic == null ? new WebAdminSignalListenerBasicConfigUpdateRequest() : safeDraft.signalListenerBasic;
                String listenerRef = safe(request.listenerRef).isBlank() ? targetId : safe(request.listenerRef);
                yield new TargetLockPreflight(prefix + ".signalListenerBasic.lockId", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, listenerRef, request.lockId, "listener:" + listenerRef);
            }
            case "signal_emitter", "signal_receiver", "action_relay", "virtual_block_device" -> {
                if (safeDraft.deviceBasic == null) {
                    yield new TargetLockPreflight("", "", "", "", "");
                }
                WebAdminDeviceBasicConfigUpdateRequest request = safeDraft.deviceBasic == null ? new WebAdminDeviceBasicConfigUpdateRequest() : safeDraft.deviceBasic;
                String deviceId = safe(request.deviceId).isBlank() ? targetId : safe(request.deviceId);
                yield new TargetLockPreflight(prefix + ".deviceBasic.lockId", WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, deviceId, request.lockId, existingNodeIdForError(nodeType, deviceId));
            }
            default -> new TargetLockPreflight(prefix + ".lockId", "", "", "", nodeType + ":" + targetId);
        };
    }

    private static TargetLockPreflight existingNodeEditMetadataTargetLock(
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit,
            int index
    ) {
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft safeDraft = edit == null ? new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft() : edit;
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        if (!isDeviceExistingNodeType(nodeType) || safeDraft.deviceMetadata == null) {
            return new TargetLockPreflight("", "", "", "", "");
        }
        String prefix = "existingNodeEdits[" + Math.max(0, index) + "]";
        String targetId = safe(safeDraft.targetId);
        WebAdminDeviceMetadataUpdateRequest request = safeDraft.deviceMetadata;
        String deviceId = safe(request.deviceId).isBlank() ? targetId : safe(request.deviceId);
        return new TargetLockPreflight(prefix + ".deviceMetadata.lockId", WebAdminEditLockService.TARGET_DEVICE_METADATA, deviceId, request.lockId, existingNodeIdForError(nodeType, deviceId));
    }

    private static TargetLockPreflight existingNodeEditNativeTriggerTargetLock(
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit,
            int index
    ) {
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft safeDraft = edit == null ? new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft() : edit;
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        if (!"virtual_block_device".equals(nodeType) || safeDraft.virtualBlockDevice == null || safeDraft.virtualBlockDevice.nativeTriggers == null) {
            return new TargetLockPreflight("", "", "", "", "");
        }
        String prefix = "existingNodeEdits[" + Math.max(0, index) + "]";
        WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request = safeDraft.virtualBlockDevice.nativeTriggers;
        String targetId = safe(safeDraft.targetId);
        String deviceId = safe(request.deviceId).isBlank() ? targetId : safe(request.deviceId);
        return new TargetLockPreflight(prefix + ".virtualBlockDevice.nativeTriggers.lockId", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS, deviceId, request.lockId, existingNodeIdForError(nodeType, deviceId));
    }

    private static String actionOwnerLockTargetType(String ownerType) {
        return switch (normalizeOwnerType(ownerType)) {
            case "listener" -> WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS;
            case "action_relay" -> WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS;
            case "region_controller" -> WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG;
            case "timer" -> WebAdminEditLockService.TARGET_TIMER_CONFIG;
            default -> "";
        };
    }

    private static String nodeDeleteLockTargetType(String nodeType) {
        return switch (normalizeExistingNodeEditType(nodeType)) {
            case "signal_join" -> WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG;
            case "timer" -> WebAdminEditLockService.TARGET_TIMER_CONFIG;
            case "signal_listener" -> WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG;
            case "virtual_block_device" -> WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG;
            case "signal_emitter", "signal_receiver", "action_relay" -> WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG;
            case "region_controller" -> WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG;
            default -> "";
        };
    }

    private static String nodeDeleteLockTargetId(String nodeType, String targetId) {
        return switch (normalizeExistingNodeEditType(nodeType)) {
            case "signal_join" -> SignalJoinStore.normalizeId(targetId);
            case "timer" -> TimerStore.normalizeId(targetId);
            default -> safe(targetId);
        };
    }

    private record TargetLockPreflight(String field, String targetType, String targetId, String lockId, String label) {
        private TargetLockPreflight {
            field = safe(field);
            targetType = safe(targetType);
            targetId = safe(targetId);
            lockId = safe(lockId);
            label = safe(label);
        }
    }

    private static void validateDuplicateDraftNodes(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.DraftNode node : request == null || request.nodes == null ? List.<WebAdminLogicChainEditorRequest.DraftNode>of() : request.nodes) {
            String nodeId = safe(node == null ? "" : node.id);
            if (!nodeId.isBlank() && !seen.add(nodeId)) {
                errors.add(error("nodes[" + index + "].id", "logic_chain_draft_node_duplicate_id", "同一保存会话不能重复提交相同的新增草稿节点 ID。", nodeId, nodeId, "", "", "删除重复草稿，或重新从新增节点入口创建不同草稿。"));
            }
            index++;
        }
    }

    private static void validateDuplicateExistingNodeEdits(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request == null || request.existingNodeEdits == null ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of() : request.existingNodeEdits) {
            if (!isExistingNodeEditDraftPresent(edit)) {
                index++;
                continue;
            }
            String key = normalizeExistingNodeEditType(edit.nodeType) + ":" + safe(edit.targetId);
            if (!key.endsWith(":") && !seen.add(key)) {
                errors.add(error("existingNodeEdits[" + index + "]", "logic_chain_existing_node_duplicate_edit", "同一保存会话不能重复提交同一个已有节点。", key, key, "", "", "保留该节点的一份草稿；同一目标的字段会在前端合并。"));
            }
            index++;
        }
    }

    private static void validateDuplicateActionEdits(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request == null || request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
            if (!isActionEditDraftPresent(edit)) {
                index++;
                continue;
            }
            String ownerType = normalizeOwnerType(edit.ownerType);
            String bucket = normalizeBucket(edit.bucket);
            String key = ownerType + ":" + safe(edit.ownerId) + ":" + bucket + ":" + parseActionIndex(edit.actionIndex);
            if (!seen.add(key)) {
                errors.add(error("actionEdits[" + index + "]", "logic_chain_action_duplicate_edit", "同一保存会话不能重复提交同一个 Action index。", key, actionAppendNodeId(ownerType, safe(edit.ownerId)), "", "", "保留该 Action 的一份草稿；同一 index 的字段会在前端合并。"));
            }
            index++;
        }
    }

    private void validateSlot(WebAdminLogicChainEditorRequest.DraftNode node, String nodeType, String prefix, List<WebAdminValidationError> errors) {
        String column = safe(node.column);
        String nodeId = safe(node == null ? "" : node.id);
        if ("signal_join".equals(nodeType) && !isSignalJoinPlacementColumn(column)) {
            errors.add(error(prefix + ".column", "logic_chain_join_column_invalid", "Signal Join 只能放在上游频道卡的下游合法列。", column, nodeId, "", "", "把 Signal Join 草稿拖到频道卡右侧出现的白色合法 slot 后再保存；C0 根来源列仍不可用。"));
        }
        if ("timer".equals(nodeType) && !"C0".equalsIgnoreCase(column)) {
            errors.add(error(prefix + ".column", "logic_chain_timer_column_deferred", "Timer 只能放在 C0 来源列；C5 Timer 引用 / 目标位需要 action-list 映射，已 deferred。", column, nodeId, "", "", "把 Timer 草稿拖到 C0 来源列；C5 Timer 引用留到后续阶段。"));
        }
        if ("signal_listener".equals(nodeType) && !isSignalJoinPlacementColumn(column)) {
            errors.add(error(prefix + ".column", "logic_chain_listener_column_invalid", "SignalListener 只能放在上游频道卡的下游合法列。", column, nodeId, "", "", "把 SignalListener 草稿拖到频道卡右侧出现的白色合法 slot 后再保存；C0 根来源列仍不可用。"));
        }
        if (Set.of("world_device", "virtual_block_device", "region_controller").contains(nodeType)
                && (column.isBlank() || !column.toUpperCase(Locale.ROOT).startsWith("C"))) {
            errors.add(error(prefix + ".column", "logic_chain_world_backed_column_invalid", "世界对象草稿必须放在规范画布列。", column, nodeId, "", "", "把草稿卡片拖到画布上出现的合法 slot 后再保存。"));
        }
        if (node.slot < 0 || node.slot > 200) {
            errors.add(error(prefix + ".slot", "logic_chain_slot_invalid", "槽位必须是 0 到 200 之间的规范 slot。", String.valueOf(node.slot), nodeId, "", "", "把草稿卡片重新拖到白色合法 slot，使用自动吸附后的规范槽位。"));
        }
    }

    private static boolean isSignalJoinPlacementColumn(String column) {
        if (column == null || column.length() < 2 || Character.toUpperCase(column.charAt(0)) != 'C') {
            return false;
        }
        try {
            return Integer.parseInt(column.substring(1)) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void validateEdges(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            List<WebAdminValidationError> errors
    ) {
        String draftId = safe(draftNode == null ? "" : draftNode.id);
        String nodeType = normalizeNodeType(draftNode == null ? "" : draftNode.type);
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : edges) {
            String from = safe(edge.from);
            String to = safe(edge.to);
            String type = normalizeEdgeType(edge.type);
            String key = from + ">" + to + ">" + type;
            String edgeId = safe(edge.id);
            String channelId = edgeChannelId(edge);
            if (!seen.add(key)) {
                errors.add(error("edges[" + index + "]", "logic_chain_duplicate_edge", "草稿连线不能重复。", key, draftId, edgeId, channelId, "删除重复连线，只保留一条相同方向和类型的连线。"));
            }
            if (from.isBlank() || to.isBlank() || type.isBlank()) {
                errors.add(error("edges[" + index + "]", "logic_chain_edge_incomplete", "草稿连线必须包含起点、终点和类型。", key, draftId, edgeId, channelId, "重新点击绿色加号选择频道端点，生成完整连线。"));
            }
            if (!Set.of("join_input", "join_output", "timer_outputs_channel", "consumes", "vbd_outputs_channel", "world_device_outputs_channel", "world_device_consumes_channel").contains(type)) {
                errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_invalid", "当前阶段只支持 Join 输入/输出、Timer 输出、SignalListener consumes、VBD 输出和世界设备输出连线；RegionController 输出必须通过 enter / exit / stay action bucket。", type, draftId, edgeId, channelId, "删除该连线后，通过草稿节点绿色加号重新连接，或保存 RegionController 后在 action bucket 内创建 signal action。"));
                index++;
                continue;
            }
            boolean fromDraft = draftId.equals(from);
            boolean toDraft = draftId.equals(to);
            if (!fromDraft && !toDraft) {
                errors.add(error("edges[" + index + "]", "logic_chain_edge_not_incident_to_draft", "8.14 只允许新增草稿节点与频道端点之间的连线。", key, draftId, edgeId, channelId, "本阶段不能修改旧节点之间的连线，请只连接当前新增草稿节点。"));
            }
            if ("signal_join".equals(nodeType)) {
                if ("join_input".equals(type)) {
                    if (!toDraft || !isChannelNodeRef(from)) {
                        errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "Signal Join 输入连线必须从频道端点指向新增 Join。", key, draftId, edgeId, channelId, "请选择左侧输入频道端点；不要直接连接 producer / consumer / 旧节点。"));
                    }
                } else if ("join_output".equals(type)) {
                    if (!fromDraft || !isChannelNodeRef(to)) {
                        errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "Signal Join 输出连线必须从新增 Join 指向频道端点。", key, draftId, edgeId, channelId, "请选择右侧输出频道端点；视觉引用卡可以使用，但保存端点必须是真实 channel。"));
                    }
                } else {
                    errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_not_allowed_for_node", "Signal Join 草稿不允许该连线类型。", type, draftId, edgeId, channelId, "Signal Join 只能使用输入频道和输出频道连线。"));
                }
            } else if ("timer".equals(nodeType)) {
                if (!"timer_outputs_channel".equals(type)) {
                    errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_not_allowed_for_node", "Timer 草稿只允许输出到频道的连线。", type, draftId, edgeId, channelId, "Timer 只能从右侧连接 1 个完成输出频道，或改用 onCompleteActions。"));
                } else if (!fromDraft || !isChannelNodeRef(to)) {
                    errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "Timer 输出连线必须从新增 Timer 指向频道端点。", key, draftId, edgeId, channelId, "请选择真实频道端点；不要把 Timer 直接连到旧节点或 consumer。"));
                }
            } else if ("signal_listener".equals(nodeType)) {
                if (!"consumes".equals(type)) {
                    errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_not_allowed_for_node", "SignalListener 草稿只允许从频道端点输入 consumes 连线。", type, draftId, edgeId, channelId, "SignalListener 只能从左侧选择 1 个监听频道。"));
                } else if (!toDraft || !isChannelNodeRef(from)) {
                    errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "SignalListener consumes 连线必须从频道端点指向新增监听器。", key, draftId, edgeId, channelId, "请选择左侧监听频道端点；不要直接连接 producer / action / 旧节点。"));
                }
            } else if ("virtual_block_device".equals(nodeType)) {
                if (!"vbd_outputs_channel".equals(type)) {
                    errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_not_allowed_for_node", "VBD 草稿只允许输出到频道的连线。", type, draftId, edgeId, channelId, "VBD 的 channel 由右侧 vbd_outputs_channel 连线决定。"));
                } else if (!fromDraft || !isChannelNodeRef(to)) {
                    errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "VBD 输出连线必须从新增 VBD 指向频道端点。", key, draftId, edgeId, channelId, "请选择真实频道端点；不要把 VBD 直接连到旧节点或 consumer。"));
                }
            } else if ("world_device".equals(nodeType)) {
                String deviceType = authoritativeWorldDeviceDraftType(draftNode);
                if (deviceType.isBlank()) {
                    errors.add(error("nodes[].worldDevice.deviceType", "logic_chain_world_device_type_mismatch", "世界设备 protected draft 缺少可信设备类型，不能由请求体决定 producer / consumer 语义。", draftNode == null || draftNode.worldDevice == null ? "" : safe(draftNode.worldDevice.deviceType), draftId, edgeId, channelId, "重新发起客户端辅助世界设备放置，确保 protected draft metadata 完整。"));
                    index++;
                    continue;
                }
                boolean consumerDevice = SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(deviceType) || SignalDeviceData.TYPE_ACTION_RELAY.equals(deviceType);
                String expectedType = consumerDevice ? "world_device_consumes_channel" : "world_device_outputs_channel";
                if (!expectedType.equals(type)) {
                    errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_not_allowed_for_node", consumerDevice ? "SignalReceiver / ActionRelay 草稿只允许从频道端点输入 consumes 连线。" : "SignalEmitter 草稿只允许使用专属输出连线。", type, draftId, edgeId, channelId, consumerDevice ? "通过世界设备引用节点左侧绿色加号连接频道端点。" : "通过世界设备引用节点右侧绿色加号连接频道端点。"));
                } else if (consumerDevice && (!toDraft || !isChannelNodeRef(from))) {
                    errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "SignalReceiver / ActionRelay 输入连线必须从频道端点指向新增设备引用。", key, draftId, edgeId, channelId, "请选择真实频道端点。"));
                } else if (!consumerDevice && (!fromDraft || !isChannelNodeRef(to))) {
                    errors.add(error("edges[" + index + "]", "logic_chain_edge_endpoint_not_channel", "SignalEmitter 输出连线必须从新增设备引用指向频道端点。", key, draftId, edgeId, channelId, "请选择真实频道端点。"));
                }
            } else if ("region_controller".equals(nodeType)) {
                errors.add(error("edges[" + index + "].type", "logic_chain_region_controller_requires_action_bucket", "区域控制器不能直接连接下游频道；输出必须先选择 enter / exit / stay bucket 并创建 signal action。", type, draftId, edgeId, channelId, "保存 RegionController 后，在节点维护面板的 enter / exit / stay action list 中新增 signal action。"));
            }
            index++;
        }
    }

    private void validateSignalJoinDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            boolean requireComplete,
            List<WebAdminValidationError> errors
    ) {
        WebAdminSignalJoinRequest join = node.signalJoin == null ? new WebAdminSignalJoinRequest() : node.signalJoin;
        String nodeId = safe(node.id);
        String normalizedJoinId = SignalJoinStore.normalizeId(join.id);
        if (normalizedJoinId.isBlank()) {
            errors.add(error("signalJoin.id", "signal_join_id_required", "Signal Join ID 不能为空。", safe(join.id), nodeId, "", "", "在 Signal Join 配置中填写唯一 ID 后再保存。"));
        }
        Set<String> inputEdges = channelRefsFromEdges(edges, "", node.id, "join_input", true);
        Set<String> outputEdges = channelRefsFromEdges(edges, node.id, "", "join_output", false);
        for (String output : outputEdges) {
            if (inputEdges.contains(output)) {
                String edgeId = edgeIdForChannel(edges, node.id, output, "join_output");
                errors.add(error("edges", "logic_chain_join_input_output_channel_conflict", "同一个信号汇合中，输入频道不能同时作为输出频道：" + output, output, safe(node.id), edgeId, output, "移除该频道的输入连线，或选择另一个输出频道。"));
            }
        }
        validateDraftJoinCycleGuard(graph, inputEdges, outputEdges, safe(node.id), errors);
        String mode = safe(join.mode).trim().toUpperCase(Locale.ROOT);
        if ("ANY_N".equals(mode)) {
            if (join.threshold < 1) {
                errors.add(error("signalJoin.threshold", "logic_chain_join_any_n_threshold_required", "ANY_N 模式阈值必须大于等于 1。", String.valueOf(join.threshold), nodeId, "", "", "把 ANY_N 阈值改为 1 或更大的整数。"));
            } else if (!inputEdges.isEmpty() && join.threshold > inputEdges.size()) {
                errors.add(error("signalJoin.threshold", "logic_chain_join_any_n_threshold_exceeds_inputs", "ANY_N 模式阈值不能大于已连接输入频道数量。", String.valueOf(join.threshold), nodeId, "", "", "降低 ANY_N 阈值，或先连接更多输入频道。"));
            }
        }
        if ("COUNT".equals(mode) && join.threshold < 1) {
            errors.add(error("signalJoin.threshold", "logic_chain_join_count_threshold_required", "COUNT 模式阈值必须大于等于 1。", String.valueOf(join.threshold), nodeId, "", "", "把 COUNT 阈值改为 1 或更大的整数。"));
        }
        if (requireComplete && inputEdges.size() < 2) {
            errors.add(error("edges", "logic_chain_join_input_edge_required", "请至少连接 2 个不同上游频道到 Signal Join。", String.valueOf(inputEdges.size()), safe(node.id), "", "", "点击 Join 左侧绿色加号，选择至少 2 个不同输入频道。"));
        }
        if (requireComplete && outputEdges.size() != 1) {
            errors.add(error("edges", "logic_chain_join_output_edge_required", "请连接 Signal Join 到 1 个输出频道。", String.valueOf(outputEdges.size()), safe(node.id), "", "", "点击 Join 右侧绿色加号，选择 1 个输出频道。"));
        }
    }

    static List<WebAdminValidationError> draftJoinCycleGuardDiagnostics(
            WebAdminDtos.LogicChainGraphDto graph,
            Set<String> inputChannels,
            Set<String> outputChannels
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        validateDraftJoinCycleGuard(graph, inputChannels, outputChannels, "", errors);
        return errors;
    }

    private static void validateDraftJoinCycleGuard(
            WebAdminDtos.LogicChainGraphDto graph,
            Set<String> inputChannels,
            Set<String> outputChannels,
            String draftNodeId,
            List<WebAdminValidationError> errors
    ) {
        if (graph == null || graph.edges() == null || graph.edges().isEmpty() || inputChannels == null || inputChannels.isEmpty() || outputChannels == null || outputChannels.isEmpty()) {
            return;
        }
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            if (edge == null || logicChainCycleGuardSkips(edge)) {
                continue;
            }
            String from = safe(edge.from());
            String to = safe(edge.to());
            if (!from.isBlank() && !to.isBlank()) {
                adjacency.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
            }
        }
        if (adjacency.isEmpty()) {
            return;
        }
        Map<String, Set<String>> channelNodeIds = logicChainChannelNodeIds(graph);
        Set<String> targetIds = new LinkedHashSet<>();
        for (String input : inputChannels) {
            targetIds.addAll(logicChainNodeIdsForChannel(channelNodeIds, input));
        }
        if (targetIds.isEmpty()) {
            return;
        }
        for (String output : outputChannels) {
            String outputChannel = channelRef(output);
            if (outputChannel.isBlank() || inputChannels.contains(outputChannel)) {
                continue;
            }
            Set<String> starts = logicChainNodeIdsForChannel(channelNodeIds, outputChannel);
            CycleGuardResult result = logicChainReachabilityGuard(starts, targetIds, adjacency);
            if (result.cycle()) {
                String cyclePath = logicChainCyclePathSummary(result.path(), outputChannel);
                errors.add(error("edges", "logic_chain_join_cycle_risk", "输出频道会形成真实循环路径：" + cyclePath + "，已拒绝保存。", outputChannel, draftNodeId, "", outputChannel, "请选择不会回到当前输入频道的输出频道，或先拆分现有逻辑链。"));
                return;
            }
            if (result.truncated()) {
                errors.add(error("edges", "logic_chain_join_cycle_guard_truncated", "循环检查达到安全上限，请缩小图谱范围或拆分配置后再保存。", outputChannel, draftNodeId, "", outputChannel, "缩小当前 Logic Chain 范围，或拆分配置后重新保存。"));
                return;
            }
        }
    }

    private static CycleGuardResult logicChainReachabilityGuard(
            Set<String> starts,
            Set<String> targets,
            Map<String, List<String>> adjacency
    ) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        Map<String, String> previous = new LinkedHashMap<>();
        for (String start : starts == null ? Set.<String>of() : starts) {
            if (!safe(start).isBlank()) {
                queue.add(start);
                visited.add(start);
                previous.put(start, "");
            }
        }
        int traversedEdges = 0;
        while (!queue.isEmpty()) {
            if (visited.size() > MAX_JOIN_CYCLE_GUARD_NODES || traversedEdges > MAX_JOIN_CYCLE_GUARD_EDGES) {
                return new CycleGuardResult(false, true, List.of());
            }
            String current = queue.removeFirst();
            for (String next : adjacency.getOrDefault(current, List.of())) {
                traversedEdges++;
                if (targets.contains(next)) {
                    return new CycleGuardResult(true, false, logicChainCyclePath(previous, current, next));
                }
                if (visited.add(next)) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
                if (visited.size() > MAX_JOIN_CYCLE_GUARD_NODES || traversedEdges > MAX_JOIN_CYCLE_GUARD_EDGES) {
                    return new CycleGuardResult(false, true, List.of());
                }
            }
        }
        return new CycleGuardResult(false, false, List.of());
    }

    private static List<String> logicChainCyclePath(Map<String, String> previous, String current, String target) {
        Deque<String> path = new ArrayDeque<>();
        path.addFirst(safe(target));
        String cursor = safe(current);
        while (!cursor.isBlank()) {
            path.addFirst(cursor);
            cursor = safe(previous.get(cursor));
        }
        return List.copyOf(path);
    }

    private static String logicChainCyclePathSummary(List<String> path, String outputChannel) {
        List<String> safePath = path == null ? List.of() : path.stream()
                .filter(value -> !safe(value).isBlank())
                .toList();
        if (safePath.isEmpty()) {
            return outputChannel + " -> ... -> 当前 Join 输入";
        }
        if (safePath.size() <= 8) {
            return String.join(" -> ", safePath);
        }
        List<String> compact = new ArrayList<>();
        compact.addAll(safePath.subList(0, 4));
        compact.add("...");
        compact.addAll(safePath.subList(safePath.size() - 3, safePath.size()));
        return String.join(" -> ", compact);
    }

    private static Map<String, Set<String>> logicChainChannelNodeIds(WebAdminDtos.LogicChainGraphDto graph) {
        Map<String, Set<String>> ids = new LinkedHashMap<>();
        for (WebAdminDtos.LogicChainNodeDto node : graph == null || graph.nodes() == null ? List.<WebAdminDtos.LogicChainNodeDto>of() : graph.nodes()) {
            if (node == null) {
                continue;
            }
            String channel = channelRef(!safe(node.channel()).isBlank() ? node.channel() : node.refId());
            if (!channel.isBlank()) {
                ids.computeIfAbsent(channel, ignored -> new LinkedHashSet<>()).add(safe(node.id()));
            }
        }
        return ids;
    }

    private static Set<String> logicChainNodeIdsForChannel(Map<String, Set<String>> channelNodeIds, String channel) {
        String normalized = channelRef(channel);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>(channelNodeIds.getOrDefault(normalized, Set.of()));
        ids.add("channel:" + normalized);
        return ids;
    }

    private static boolean logicChainCycleGuardSkips(WebAdminDtos.LogicChainEdgeDto edge) {
        Map<String, Object> metadata = edge.metadata() == null ? Map.of() : edge.metadata();
        return edge.referenceEdge()
                || truthy(metadata.get("nonTraversal"))
                || truthy(metadata.get("visualOnly"));
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private record CycleGuardResult(boolean cycle, boolean truncated, List<String> path) {
    }

    private void validateTimerDraft(
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            boolean requireComplete,
            List<WebAdminValidationError> errors
    ) {
        WebAdminTimerRequest timer = node.timer == null ? new WebAdminTimerRequest() : node.timer;
        String nodeId = safe(node.id);
        String normalizedTimerId = TimerStore.normalizeId(timer.id);
        if (normalizedTimerId.isBlank()) {
            errors.add(error("timer.id", "timer_id_required", "Timer ID 不能为空。", safe(timer.id), nodeId, "", "", "在 Timer 配置中填写唯一 ID 后再保存。"));
        }
        Set<String> outputEdges = channelRefsFromEdges(edges, node.id, "", "timer_outputs_channel", false);
        boolean hasCompleteActions = timer.onCompleteActions != null && !timer.onCompleteActions.isEmpty();
        if (requireComplete && outputEdges.size() > 1) {
            errors.add(error("edges", "logic_chain_timer_output_edge_single_required", "Timer 只能连接 1 个完成输出频道。", String.valueOf(outputEdges.size()), safe(node.id), "", "", "保留 1 个 Timer 输出频道，删除其它输出连线。"));
        }
        if (requireComplete && outputEdges.isEmpty() && !hasCompleteActions) {
            errors.add(error("edges", "logic_chain_timer_output_edge_required", "请连接 Timer 到输出频道；如果没有输出连线，必须配置 onCompleteActions。", "", safe(node.id), "", "", "点击 Timer 右侧绿色加号选择输出频道，或配置完成动作。"));
        }
    }

    private static void validateCrossDraftTargetConflicts(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> nodeDeletes = new LinkedHashSet<>();
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request == null || request.nodeDeletes == null ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of() : request.nodeDeletes) {
            if (isNodeDeleteDraftPresent(delete)) {
                nodeDeletes.add(normalizeExistingNodeEditType(delete.nodeType) + ":" + safe(delete.targetId));
            }
        }
        int editIndex = 0;
        for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request == null || request.existingNodeEdits == null ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of() : request.existingNodeEdits) {
            if (isExistingNodeEditDraftPresent(edit)) {
                String key = normalizeExistingNodeEditType(edit.nodeType) + ":" + safe(edit.targetId);
                if (nodeDeletes.contains(key)) {
                    errors.add(error("existingNodeEdits[" + editIndex + "]", "logic_chain_node_edit_delete_conflict", "同一保存会话不能同时编辑并删除同一个节点。", key, key, "", "", "保留编辑草稿或删除草稿其中一个。"));
                }
            }
            editIndex++;
        }

        Set<String> actionDeletes = new LinkedHashSet<>();
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request == null || request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
            if (isActionDeleteDraftPresent(delete)) {
                actionDeletes.add(actionTargetKey(normalizeOwnerType(delete.ownerType), safe(delete.ownerId), normalizeBucket(delete.bucket), parseActionIndex(delete.actionIndex)));
            }
        }
        Set<String> actionReorders = new LinkedHashSet<>();
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request == null || request.actionReorders == null ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of() : request.actionReorders) {
            if (isActionReorderDraftPresent(reorder)) {
                actionReorders.add(actionTargetKey(normalizeOwnerType(reorder.ownerType), safe(reorder.ownerId), normalizeBucket(reorder.bucket), parseActionIndex(reorder.fromIndex)));
            }
        }
        int actionEditIndex = 0;
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request == null || request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
            if (isActionEditDraftPresent(edit)) {
                String ownerType = normalizeOwnerType(edit.ownerType);
                String key = actionTargetKey(ownerType, safe(edit.ownerId), normalizeBucket(edit.bucket), parseActionIndex(edit.actionIndex));
                if (actionDeletes.contains(key) || actionReorders.contains(key)) {
                    errors.add(error("actionEdits[" + actionEditIndex + "]", "logic_chain_action_edit_delete_reorder_conflict", "同一 Action index 不能同时提交编辑、删除或重排草稿。", key, actionAppendNodeId(ownerType, safe(edit.ownerId)), "", "", "保留一种维护草稿；删除/重排会清理同 index 的字段编辑草稿。"));
                }
            }
            actionEditIndex++;
        }
        int actionDeleteIndex = 0;
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request == null || request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
            if (isActionDeleteDraftPresent(delete)) {
                String ownerType = normalizeOwnerType(delete.ownerType);
                String key = actionTargetKey(ownerType, safe(delete.ownerId), normalizeBucket(delete.bucket), parseActionIndex(delete.actionIndex));
                if (actionReorders.contains(key)) {
                    errors.add(error("actionDeletes[" + actionDeleteIndex + "]", "logic_chain_action_delete_reorder_conflict", "同一 Action index 不能同时删除并重排。", key, actionAppendNodeId(ownerType, safe(delete.ownerId)), "", "", "删除草稿和重排草稿只保留一个。"));
                }
            }
            actionDeleteIndex++;
        }
    }

    private static void validateSameActionTargetMultiWriteConflicts(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Map<String, String> firstFieldByTarget = new LinkedHashMap<>();
        Set<String> conflicted = new LinkedHashSet<>();
        if (hasActionAppend(request)) {
            WebAdminLogicChainEditorRequest.ActionAppendDraft append = request.actionAppend;
            String key = actionListTargetKey(append.ownerType, append.ownerId, append.bucket);
            registerActionTargetWrite(firstFieldByTarget, conflicted, key, "actionAppend");
        }
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request == null || request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
            if (isActionEditDraftPresent(edit)) {
                String key = actionListTargetKey(edit.ownerType, edit.ownerId, edit.bucket);
                registerActionTargetWrite(firstFieldByTarget, conflicted, key, "actionEdits[" + index + "]");
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request == null || request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
            if (isActionDeleteDraftPresent(delete)) {
                String key = actionListTargetKey(delete.ownerType, delete.ownerId, delete.bucket);
                registerActionTargetWrite(firstFieldByTarget, conflicted, key, "actionDeletes[" + index + "]");
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request == null || request.actionReorders == null ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of() : request.actionReorders) {
            if (isActionReorderDraftPresent(reorder)) {
                String key = actionListTargetKey(reorder.ownerType, reorder.ownerId, reorder.bucket);
                registerActionTargetWrite(firstFieldByTarget, conflicted, key, "actionReorders[" + index + "]");
            }
            index++;
        }
        for (String key : conflicted) {
            String field = firstFieldByTarget.getOrDefault(key, "actionEdits");
            errors.add(error(
                    field,
                    "logic_chain_action_target_multi_write_conflict",
                    "同一个 Action list 不能在一次 Logic Chain 保存中提交多个独立维护草稿，避免底层 typed 写入顺序导致半应用。",
                    key,
                    key,
                    "",
                    "",
                    "同一 Action list 请先保留一个维护草稿，或在列表层生成单一合并草稿；不同节点/不同 action list 的多个草稿可以一次保存。"
            ));
        }
    }

    private static void registerActionTargetWrite(
            Map<String, String> firstFieldByTarget,
            Set<String> conflicted,
            String key,
            String field
    ) {
        if (safe(key).isBlank()) {
            return;
        }
        String previous = firstFieldByTarget.putIfAbsent(key, field);
        if (previous != null) {
            conflicted.add(key);
        }
    }

    private static String actionListTargetKey(String ownerType, String ownerId, String bucket) {
        String normalizedOwnerType = normalizeOwnerType(ownerType);
        String normalizedOwnerId = normalizedActionOwnerId(normalizedOwnerType, ownerId);
        String normalizedBucket = "timer".equals(normalizedOwnerType)
                ? normalizeTimerActionBucketAlias(bucket)
                : normalizeBucket(bucket);
        return normalizedOwnerType + ":" + normalizedOwnerId + ":" + normalizedBucket;
    }

    private static void validateDuplicateNodeDeletes(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request == null || request.nodeDeletes == null ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of() : request.nodeDeletes) {
            if (!isNodeDeleteDraftPresent(delete)) {
                index++;
                continue;
            }
            String key = normalizeExistingNodeEditType(delete.nodeType) + ":" + safe(delete.targetId);
            if (!seen.add(key)) {
                errors.add(error("nodeDeletes[" + index + "]", "logic_chain_node_duplicate_delete", "同一保存会话不能重复删除同一个节点。", key, existingNodeIdForError(normalizeExistingNodeEditType(delete.nodeType), safe(delete.targetId)), "", channelForExistingNode(normalizeExistingNodeEditType(delete.nodeType), safe(delete.targetId)), "保留一条删除草稿即可。"));
            }
            index++;
        }
    }

    private static void validateSingleNodeDeletePerSave(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        int count = 0;
        int index = 0;
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request == null || request.nodeDeletes == null ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of() : request.nodeDeletes) {
            if (!isNodeDeleteDraftPresent(delete)) {
                index++;
                continue;
            }
            count++;
            if (count > 1) {
                String nodeType = normalizeExistingNodeEditType(delete.nodeType);
                String targetId = safe(delete.targetId);
                errors.add(error(
                        "nodeDeletes[" + index + "]",
                        "logic_chain_node_delete_single_write_fail_closed",
                        "一次保存只允许删除一个节点，避免多个 typed delete 顺序执行时出现半应用。",
                        String.valueOf(count),
                        existingNodeIdForError(nodeType, targetId),
                        "",
                        channelForExistingNode(nodeType, targetId),
                        "先保存一个删除草稿，刷新后再删除其它节点。"
                ));
            }
            index++;
        }
    }

    private static void validateDuplicateActionDeletes(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request == null || request.actionDeletes == null ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of() : request.actionDeletes) {
            if (!isActionDeleteDraftPresent(delete)) {
                index++;
                continue;
            }
            String ownerType = normalizeOwnerType(delete.ownerType);
            String bucket = normalizeBucket(delete.bucket);
            String key = ownerType + ":" + safe(delete.ownerId) + ":" + bucket + ":" + parseActionIndex(delete.actionIndex);
            if (!seen.add(key)) {
                errors.add(error("actionDeletes[" + index + "]", "logic_chain_action_duplicate_delete", "同一保存会话不能重复删除同一个 Action。", key, actionAppendNodeId(ownerType, safe(delete.ownerId)), "", "", "保留一条删除草稿即可。"));
            }
            index++;
        }
    }

    private static void validateDuplicateActionReorders(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request == null || request.actionReorders == null ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of() : request.actionReorders) {
            if (!isActionReorderDraftPresent(reorder)) {
                index++;
                continue;
            }
            String ownerType = normalizeOwnerType(reorder.ownerType);
            String bucket = normalizeBucket(reorder.bucket);
            String key = ownerType + ":" + safe(reorder.ownerId) + ":" + bucket + ":" + parseActionIndex(reorder.fromIndex);
            if (!seen.add(key)) {
                errors.add(error("actionReorders[" + index + "]", "logic_chain_action_duplicate_reorder", "同一保存会话不能重复重排同一个 Action。", key, actionAppendNodeId(ownerType, safe(reorder.ownerId)), "", "", "保留一条重排草稿即可。"));
            }
            index++;
        }
    }

    private void validateSignalListenerDraft(
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            boolean requireComplete,
            String prefix,
            List<WebAdminValidationError> errors
    ) {
        WebAdminSignalListenerCreateRequest request = deriveSignalListenerRequestFromEdges(node, edges);
        String nodeId = safe(node == null ? "" : node.id);
        Set<String> inputEdges = channelRefsFromEdges(edges, "", node == null ? "" : node.id, "consumes", true);
        if (requireComplete && inputEdges.size() != 1) {
            errors.add(error("edges", "logic_chain_listener_consumes_edge_required", "请连接 SignalListener 到 1 个上游监听频道。", String.valueOf(inputEdges.size()), nodeId, "", "", "点击 SignalListener 左侧绿色加号，选择 1 个频道端点作为监听频道。"));
        }
        if (inputEdges.size() > 1) {
            errors.add(error("edges", "logic_chain_listener_single_channel_required", "SignalListener 只能监听 1 个频道。", String.valueOf(inputEdges.size()), nodeId, "", "", "保留 1 条 consumes 连线，删除其它监听频道连线。"));
        }
        for (WebAdminValidationError error : WebAdminSignalListenerLifecycleService.validateCreateRequest(request)) {
            errors.add(new WebAdminValidationError(
                    "signalListener." + error.field(),
                    error.code(),
                    error.message(),
                    error.rejectedValueSummary(),
                    nodeId,
                    "",
                    "channel".equals(error.field()) ? SignalChannel.normalize(request.channel) : "",
                    error.severity(),
                    safe(error.fixHint()).isBlank() ? "填写虚拟监听器名称并通过左侧绿色加号选择监听频道。" : error.fixHint()
            ));
        }
    }

    private void validateVirtualBlockDeviceDraft(
            WebAdminLogicChainEditorRequest request,
            WebAdminUser user,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            boolean requireComplete,
            String prefix,
            List<WebAdminValidationError> errors
    ) {
        String nodeId = safe(node == null ? "" : node.id);
        String protectedDraftId = virtualBlockProtectedDraftId(node);
        List<String> protectedErrors = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                protectedDraftId,
                request == null ? "" : request.lockId,
                user,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE
        );
        if (!protectedErrors.isEmpty()) {
            errors.add(error(prefix + ".virtualBlockDevice.protectedDraftId", "logic_chain_protected_draft_required", "VBD 必须绑定当前编辑锁持有的 protected draft：" + protectedErrors.getFirst(), protectedDraftId, nodeId, "", "", "从 VBD 节点编辑页发起游戏内选择，选择完成后再保存。"));
        }
        Set<String> outputEdges = channelRefsFromEdges(edges, nodeId, "", "vbd_outputs_channel", false);
        if (requireComplete && outputEdges.size() != 1) {
            errors.add(error("edges", "logic_chain_vbd_output_channel_required", "VBD 的下游 channel 必须由 1 条画布连线决定。", String.valueOf(outputEdges.size()), nodeId, "", "", "点击 VBD 右侧绿色加号连接 1 个 Channel Endpoint。"));
        }
        validateItemSubmitContainerDraft(node, prefix, errors);
    }

    private void validateProtectedWorldBackedDraft(
            WebAdminLogicChainEditorRequest request,
            WebAdminUser user,
            WebAdminLogicChainEditorRequest.DraftNode node,
            List<WebAdminLogicChainEditorRequest.DraftEdge> nodeEdges,
            String nodeType,
            boolean requireComplete,
            String prefix,
            List<WebAdminValidationError> errors
    ) {
        String nodeId = safe(node == null ? "" : node.id);
        String protectedDraftId = protectedDraftIdFor(node);
        List<String> protectedErrors = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                protectedDraftId,
                request == null ? "" : request.lockId,
                user,
                protectedDraftObjectType(nodeType)
        );
        if (!protectedErrors.isEmpty()) {
            errors.add(error(prefix + ".protectedDraftId", "logic_chain_protected_draft_required", "世界对象草稿必须绑定当前编辑锁持有的 protected draft：" + protectedErrors.getFirst(), protectedDraftId, nodeId, "", "", "从 Logic Chain 发起对应的游戏内选择/放置/区域选择流程。"));
        }
        if ("world_device".equals(nodeType)) {
            WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.get(protectedDraftId);
            String requestType = normalizeWorldDeviceDraftType(node == null || node.worldDevice == null ? "" : node.worldDevice.deviceType);
            String protectedType = normalizeWorldDeviceDraftType(metadataString(entry, "deviceType"));
            if (!requestType.isBlank() && !protectedType.isBlank() && !requestType.equals(protectedType)) {
                errors.add(worldDeviceTypeMismatchError(prefix + ".worldDevice.deviceType", requestType, protectedType, "", nodeId));
            }
            String deviceType = firstNonBlank(protectedType, requestType);
            if (requireComplete && !deviceType.isBlank()) {
                boolean consumerDevice = SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(deviceType) || SignalDeviceData.TYPE_ACTION_RELAY.equals(deviceType);
                Set<String> channels = consumerDevice
                        ? channelRefsFromEdges(nodeEdges, "", nodeId, "world_device_consumes_channel", true)
                        : channelRefsFromEdges(nodeEdges, nodeId, "", "world_device_outputs_channel", false);
                if (channels.size() != 1) {
                    errors.add(error("edges", consumerDevice ? "logic_chain_world_device_input_channel_required" : "logic_chain_world_device_output_channel_required", consumerDevice ? "SignalReceiver / ActionRelay 需要 1 条 channel -> device 的 consumes 输入连线。" : "SignalEmitter 需要 1 条 device -> channel 的输出连线。", String.valueOf(channels.size()), nodeId, "", "", consumerDevice ? "点击世界设备引用左侧绿色加号，连接 1 个 Channel Endpoint。" : "点击世界设备引用右侧绿色加号，连接 1 个 Channel Endpoint。"));
                }
            }
        }
    }

    private static void validateItemSubmitContainerDraft(WebAdminLogicChainEditorRequest.DraftNode node, String prefix, List<WebAdminValidationError> errors) {
        if (node == null || node.virtualBlockDevice == null) {
            return;
        }
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft requirement : node.virtualBlockDevice.itemSubmitRequirements == null ? List.<WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft>of() : node.virtualBlockDevice.itemSubmitRequirements) {
            int count = parseActionIndex(requirement == null ? 1 : requirement.count);
            int consumeCount = parseActionIndex(requirement == null ? 1 : requirement.consumeCount);
            if (count < 1) {
                errors.add(error(prefix + ".virtualBlockDevice.itemSubmitRequirements[" + index + "].count", "logic_chain_item_submit_count_invalid", "itemSubmit count 必须大于 0。", String.valueOf(requirement == null ? "" : requirement.count), safe(node.id), "", "", "修正 requirement count；consumeCount 默认跟随 count，手动修改后可解耦。"));
            }
            if (consumeCount < 0) {
                errors.add(error(prefix + ".virtualBlockDevice.itemSubmitRequirements[" + index + "].consumeCount", "logic_chain_item_submit_consume_count_invalid", "itemSubmit consumeCount 不能为负。", String.valueOf(requirement == null ? "" : requirement.consumeCount), safe(node.id), "", "", "consumeCount 可小于 count，但不能小于 0。"));
            }
            if (requirement != null && Boolean.TRUE.equals(requirement.consumeCountFollowsCount) && count >= 1 && consumeCount >= 0 && consumeCount != count) {
                errors.add(error(prefix + ".virtualBlockDevice.itemSubmitRequirements[" + index + "].consumeCount", "logic_chain_item_submit_consume_count_follow_mismatch", "consumeCount 仍设置为跟随 count 时，两者必须一致。", count + " / " + consumeCount, safe(node.id), "", "", "如果要单独修改 consumeCount，请先把 consumeCountFollowsCount 设为 false。"));
            }
            index++;
        }
        index = 0;
        for (WebAdminLogicChainEditorRequest.ContainerRequirementDraft requirement : node.virtualBlockDevice.containerRequirements == null ? List.<WebAdminLogicChainEditorRequest.ContainerRequirementDraft>of() : node.virtualBlockDevice.containerRequirements) {
            int slot = parseActionIndex(requirement == null ? 0 : requirement.slot);
            if (slot < 0) {
                errors.add(error(prefix + ".virtualBlockDevice.containerRequirements[" + index + "].slot", "logic_chain_container_slot_invalid", "container slot 不能为负。", String.valueOf(requirement == null ? "" : requirement.slot), safe(node.id), "", "", "通过容器捕获流程重新选择 slot，不要手写负数。"));
            }
            index++;
        }
    }

    private Set<String> channelRefsFromEdges(
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String from,
            String to,
            String type,
            boolean useSource
    ) {
        String safeFrom = safe(from);
        String safeTo = safe(to);
        String safeType = normalizeEdgeType(type);
        Set<String> channels = new LinkedHashSet<>();
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : edges) {
            if (!safeFrom.isBlank() && !safeFrom.equals(safe(edge.from))) {
                continue;
            }
            if (!safeTo.isBlank() && !safeTo.equals(safe(edge.to))) {
                continue;
            }
            if (safeType.equals(normalizeEdgeType(edge.type))) {
                String channel = channelRef(useSource ? edge.from : edge.to);
                if (!channel.isBlank()) {
                    channels.add(channel);
                }
            }
        }
        return channels;
    }

    private static String edgeChannelId(WebAdminLogicChainEditorRequest.DraftEdge edge) {
        if (edge != null && isChannelNodeRef(edge.from)) {
            return channelRef(edge.from);
        }
        if (edge != null && isChannelNodeRef(edge.to)) {
            return channelRef(edge.to);
        }
        return "";
    }

    private static String edgeIdForChannel(
            List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
            String draftId,
            String channel,
            String type
    ) {
        String normalized = channelRef(channel);
        String safeDraftId = safe(draftId);
        String safeType = normalizeEdgeType(type);
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : edges) {
            if (!safeType.equals(normalizeEdgeType(edge.type))) {
                continue;
            }
            boolean incident = safeDraftId.equals(safe(edge.from)) || safeDraftId.equals(safe(edge.to));
            if (incident && normalized.equals(edgeChannelId(edge))) {
                return safe(edge.id);
            }
        }
        return "";
    }

    WebAdminDtos.LogicChainGraphDto currentGraph(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminLogicChainEditorRequest request
    ) {
        if (server == null) {
            return fallbackGraphForNullServer(request);
        }
        return logicChainService.graphForRoot(
                server,
                user,
                session,
                normalizeRootType(request.rootType),
                safe(request.rootRef),
                request.includeDisabled,
                request.maxDepth <= 0 ? 3 : request.maxDepth,
                null
        );
    }

    private static WebAdminDtos.LogicChainGraphDto fallbackGraphForNullServer(WebAdminLogicChainEditorRequest request) {
        String rootType = normalizeRootType(request == null ? "" : request.rootType);
        String rootRef = safe(request == null ? "" : request.rootRef);
        int depth = request == null || request.maxDepth <= 0 ? 3 : request.maxDepth;
        String rootChannel = "channel".equals(rootType) ? SignalChannel.normalize(rootRef) : "";
        String rootId = ("channel".equals(rootType) ? "channel:" : rootType + ":") + rootRef;
        WebAdminDtos.LogicChainNodeDto root = new WebAdminDtos.LogicChainNodeDto(
                rootId,
                "channel".equals(rootType) ? "channel" : "root",
                rootType,
                rootRef,
                rootRef.isBlank() ? "未解析 root" : rootRef,
                "测试环境 root snapshot",
                rootChannel,
                true,
                rootRef.isBlank() ? "WARNING" : "OK",
                rootRef.isBlank() ? "WARNING" : "OK",
                "",
                "channel".equals(rootType) && !rootChannel.isBlank() ? "#/signals/" + rootChannel : "",
                Map.of("readOnly", true, "testFallback", true)
        );
        WebAdminDtos.LogicChainMetadataDto metadata = new WebAdminDtos.LogicChainMetadataDto(
                "editor:" + rootType + ":" + rootRef,
                "逻辑链编辑测试图",
                "",
                "logic-chain",
                "逻辑链编辑测试图",
                "logic-chain",
                List.of(),
                "",
                rootType,
                rootRef,
                rootChannel,
                request == null || request.includeDisabled,
                depth,
                "tree",
                "",
                "",
                0L,
                "",
                null
        );
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("channelCount", "channel".equals(rootType) && !rootChannel.isBlank() ? 1 : 0);
        stats.put("producerCount", 0);
        stats.put("consumerCount", 0);
        stats.put("actionCount", 0);
        stats.put("downstreamChannelCount", 0);
        stats.put("disabledNodeCount", 0);
        stats.put("maxDepth", depth);
        stats.put("fallback", true);
        List<WebAdminDtos.LogicChainNodeDto> nodes = new ArrayList<>();
        if ("signal_join".equals(rootType) && !rootRef.isBlank()) {
            root = new WebAdminDtos.LogicChainNodeDto(
                    "signal_join:" + rootRef,
                    "signal_join",
                    "signal_join",
                    SignalJoinStore.normalizeId(rootRef),
                    rootRef,
                    "测试环境 Signal Join root",
                    "",
                    true,
                    "OK",
                    "OK",
                    "",
                    "",
                    Map.of("kind", "signal_join", "nodeKind", "primary", "joinId", SignalJoinStore.normalizeId(rootRef))
            );
        } else if ("timer".equals(rootType) && !rootRef.isBlank()) {
            root = new WebAdminDtos.LogicChainNodeDto(
                    "timer:" + rootRef,
                    "timer",
                    "timer",
                    TimerStore.normalizeId(rootRef),
                    rootRef,
                    "测试环境 Timer root",
                    rootChannel,
                    true,
                    "OK",
                    "OK",
                    "",
                    "",
                    Map.of("kind", "timer", "nodeKind", "primary", "timerId", TimerStore.normalizeId(rootRef))
            );
            for (String bucket : List.of("start", "tick", "complete", "cancel")) {
                nodes.add(fallbackActionNode("timer", TimerStore.normalizeId(rootRef), bucket, 0));
            }
        } else if ("listener".equals(rootType) && !rootRef.isBlank()) {
            root = new WebAdminDtos.LogicChainNodeDto(
                    "consumer:listener:" + rootRef,
                    "consumer",
                    "listener",
                    rootRef,
                    rootRef,
                    "测试环境 Listener root",
                    rootChannel,
                    true,
                    "OK",
                    "OK",
                    "",
                    "",
                    Map.of("kind", "signal_listener", "nodeKind", "primary", "listenerId", rootRef)
            );
            nodes.add(fallbackActionNode("listener", rootRef, "", 0));
        } else if ("device".equals(rootType) && rootRef.startsWith(SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE + ":")) {
            root = new WebAdminDtos.LogicChainNodeDto(
                    "producer:device:" + rootRef + ":channel",
                    "producer",
                    "device",
                    rootRef,
                    rootRef,
                    "测试环境 VBD producer projection",
                    rootChannel,
                    true,
                    "OK",
                    "OK",
                    "",
                    "",
                    Map.of(
                            "nodeKind", "primary",
                            "sourceType", SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.toUpperCase(Locale.ROOT),
                            "deviceId", rootRef
                    )
            );
        }
        nodes.addFirst(root);
        return new WebAdminDtos.LogicChainGraphDto(
                metadata,
                root,
                List.of(),
                List.copyOf(nodes),
                List.of(),
                rootRef.isBlank() ? List.of("当前 root 无法解析到 Signal 频道，图谱为空。") : List.of(),
                stats
        );
    }

    private static WebAdminDtos.LogicChainNodeDto fallbackActionNode(String ownerType, String ownerId, String bucket, int actionIndex) {
        String type = "timer".equals(ownerType) ? "timer_action" : "action";
        String id = "timer".equals(ownerType)
                ? "action:timer:" + ownerId + ":" + bucket + ":" + actionIndex
                : "action:listener:" + ownerId + ":" + actionIndex;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nodeKind", "primary");
        metadata.put("ownerType", ownerType);
        metadata.put("ownerId", ownerId);
        metadata.put("actionIndex", actionIndex);
        metadata.put("actionType", "message");
        if ("timer".equals(ownerType)) {
            metadata.put("timerBucket", bucket);
        }
        return new WebAdminDtos.LogicChainNodeDto(
                id,
                type,
                type,
                ownerType + ":" + ownerId + ":" + (bucket.isBlank() ? "" : bucket + ":") + actionIndex,
                "测试环境 Action #" + (actionIndex + 1),
                "测试环境 action root",
                "",
                true,
                "OK",
                "OK",
                "",
                "",
                Map.copyOf(metadata)
        );
    }

    public static String graphFingerprintFor(WebAdminDtos.LogicChainGraphDto graph) {
        StringBuilder input = new StringBuilder("logic_chain_editor_graph_v1|");
        WebAdminDtos.LogicChainMetadataDto metadata = graph == null ? null : graph.metadata();
        WebAdminDtos.LogicChainNodeDto root = graph == null ? null : graph.root();
        input.append(safe(metadata == null ? "" : metadata.rootType())).append('|')
                .append(safe(metadata == null ? "" : metadata.rootRef())).append('|')
                .append(safe(metadata == null ? "" : metadata.rootChannel())).append('|')
                .append(safe(root == null ? "" : root.id())).append('|');
        List<WebAdminDtos.LogicChainNodeDto> nodes = new ArrayList<>(graph == null || graph.nodes() == null ? List.of() : graph.nodes());
        nodes.sort(Comparator.comparing(WebAdminDtos.LogicChainNodeDto::id));
        for (WebAdminDtos.LogicChainNodeDto node : nodes) {
            input.append("n:").append(safe(node.id())).append(':').append(safe(node.type())).append(':')
                    .append(safe(node.refId())).append(':').append(safe(node.channel())).append(':')
                    .append(node.enabled()).append('|');
        }
        List<WebAdminDtos.LogicChainEdgeDto> edges = new ArrayList<>(graph == null || graph.edges() == null ? List.of() : graph.edges());
        edges.sort(Comparator.comparing(edge -> safe(edge.from()) + ">" + safe(edge.to()) + ">" + safe(edge.type())));
        for (WebAdminDtos.LogicChainEdgeDto edge : edges) {
            input.append("e:").append(safe(edge.from())).append('>').append(safe(edge.to())).append(':')
                    .append(safe(edge.type())).append(':').append(edge.referenceEdge()).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.toString().hashCode());
        }
    }

    private static Map<String, Object> capability(String type, String label, String description, List<String> allowedColumns, boolean pureConfig) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("label", label);
        item.put("description", description);
        item.put("pureConfig", pureConfig);
        item.put("allowedColumns", allowedColumns == null ? List.of() : List.copyOf(allowedColumns));
        item.put("worldEntityRequired", !pureConfig);
        return item;
    }

    static WebAdminLogicChainEditorRequest safeRequest(WebAdminLogicChainEditorRequest request) {
        return request == null ? new WebAdminLogicChainEditorRequest() : request;
    }

    private static String lockId(WebAdminWriteResult result) {
        Object lock = result == null || result.data() == null ? null : result.data().get("lock");
        if (lock instanceof WebAdminEditLockStatusDto status) {
            return safe(status.lockId());
        }
        if (lock instanceof Map<?, ?> map) {
            Object id = map.get("lockId");
            return id == null ? "" : String.valueOf(id);
        }
        return "";
    }

    private static boolean isChannelNodeRef(String value) {
        String safe = safe(value);
        return safe.startsWith("channel:") && SignalChannel.isValid(safe.substring("channel:".length()));
    }

    private static String channelRef(String value) {
        String safe = safe(value);
        if (safe.startsWith("channel:")) {
            safe = safe.substring("channel:".length());
        }
        String normalized = SignalChannel.normalize(safe);
        return SignalChannel.isValid(normalized) ? normalized : "";
    }

    private static String channelName(String value) {
        String normalized = channelRef(value);
        return normalized.isBlank() ? "" : normalized;
    }

    private static boolean hasActionAppend(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasActionAppend(request);
    }

    private static boolean hasTypedStoreDrafts(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasTypedStoreDrafts(request);
    }

    private static boolean hasNonNodeDeleteTypedStoreDrafts(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasNonNodeDeleteTypedStoreDrafts(request);
    }

    private static boolean hasChannelMetadataDrafts(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasChannelMetadataDrafts(request);
    }

    private static boolean hasExistingNodeEdit(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasExistingNodeEdit(request);
    }

    private static boolean isExistingNodeEditDraftPresent(WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft) {
        return LogicChainDraftOperationPlanner.isExistingNodeEditDraftPresent(draft);
    }

    private static boolean hasActionEdit(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasActionEdit(request);
    }

    private static boolean isActionEditDraftPresent(WebAdminLogicChainEditorRequest.ActionEditDraft draft) {
        return LogicChainDraftOperationPlanner.isActionEditDraftPresent(draft);
    }

    private static boolean hasNodeDelete(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasNodeDelete(request);
    }

    private static boolean isNodeDeleteDraftPresent(WebAdminLogicChainEditorRequest.NodeDeleteDraft draft) {
        return LogicChainDraftOperationPlanner.isNodeDeleteDraftPresent(draft);
    }

    private static boolean hasActionDelete(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasActionDelete(request);
    }

    private static boolean isActionDeleteDraftPresent(WebAdminLogicChainEditorRequest.ActionDeleteDraft draft) {
        return LogicChainDraftOperationPlanner.isActionDeleteDraftPresent(draft);
    }

    private static boolean hasActionReorder(WebAdminLogicChainEditorRequest request) {
        return LogicChainDraftOperationPlanner.hasActionReorder(request);
    }

    private static boolean isActionReorderDraftPresent(WebAdminLogicChainEditorRequest.ActionReorderDraft draft) {
        return LogicChainDraftOperationPlanner.isActionReorderDraftPresent(draft);
    }

    private static String actionAppendNodeId(String ownerType, String ownerId) {
        String type = normalizeOwnerType(ownerType);
        String id = safe(ownerId);
        if (type.isBlank() && id.isBlank()) {
            return "";
        }
        return type + ":" + id;
    }

    private static void validateActionAppendDraft(
            WebAdminLogicChainEditorRequest.ActionAppendDraft draft,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.ActionAppendDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionAppendDraft() : draft;
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = safe(safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        String ownerNodeId = actionAppendNodeId(ownerType, ownerId);
        if (!Set.of("listener", "action_relay", "region_controller", "timer").contains(ownerType)) {
            errors.add(error("actionAppend.ownerType", "logic_chain_action_append_owner_type_invalid", "当前只支持给 SignalListener、ActionRelay、RegionController 或 Timer 追加 Action。", safeDraft.ownerType, ownerNodeId, "", "", "从已有 SignalListener、ActionRelay、RegionController 或 Timer 的 action 列表入口追加。"));
        }
        if (ownerId.isBlank()) {
            errors.add(error("actionAppend.ownerId", "logic_chain_action_append_owner_id_required", "追加 Action 需要已有 action 容器 ID。", "", ownerNodeId, "", "", "回到已有 action 容器节点，从该节点的追加入口创建草稿。"));
        }
        if ("region_controller".equals(ownerType) && parseRegionTrigger(bucket) == null) {
            errors.add(error("actionAppend.bucket", "logic_chain_region_action_bucket_invalid", "Region action bucket 只支持 enter / exit / stay。", bucket, ownerNodeId, "", "", "选择 Region 的 enter、exit 或 stay 动作桶。"));
        }
        if ("timer".equals(ownerType) && !Set.of("start", "tick", "complete", "cancel").contains(bucket)) {
            errors.add(error("actionAppend.bucket", "logic_chain_timer_action_bucket_invalid", "Timer action bucket 只支持 start / tick / complete / cancel。", bucket, ownerNodeId, "", "", "选择 Timer 的 start、tick、complete 或 cancel 动作桶。"));
        }
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = safeDraft.action == null ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry() : safeDraft.action;
        List<WebAdminValidationError> actionErrors = WebAdminActionRelayActionsService.validateActionEntries(List.of(entry));
        for (WebAdminValidationError actionError : actionErrors) {
            errors.add(error("actionAppend." + actionError.field(), actionError.code(), actionError.message(), actionError.rejectedValueSummary(), ownerNodeId, "", channelRef(actionError.rejectedValueSummary()), "修正待追加 Action 的字段后再保存；当前只会追加 1 条新 Action。"));
        }
        if (safe(safeDraft.expectedFingerprint).isBlank()) {
            errors.add(error("actionAppend.expectedFingerprint", "required", "追加 Action 需要 expectedFingerprint。", "", ownerNodeId, "", "", "重新从 action 容器节点进入追加流程，获取最新 fingerprint。"));
        }
        if (safe(safeDraft.lockId).isBlank()) {
            errors.add(error("actionAppend.lockId", "edit_lock_required", "追加 Action 需要对应 action 容器编辑锁。", "", ownerNodeId, "", "", "重新打开该 action 容器的追加流程，获取对应编辑锁。"));
        }
    }

    private static void validateExistingNodeEditDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft,
            int index,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft() : draft;
        String prefix = "existingNodeEdits[" + Math.max(0, index) + "]";
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        String targetId = safe(safeDraft.targetId);
        if (!Set.of("channel_metadata", "signal_join", "timer", "signal_listener", "signal_emitter", "signal_receiver", "action_relay", "virtual_block_device").contains(nodeType)) {
            errors.add(error(prefix + ".nodeType", "logic_chain_existing_node_type_deferred", "此节点当前只能查看，编辑能力后续支持。", safeDraft.nodeType, targetId, "", "", "本阶段只支持 Channel metadata、Signal Join、Timer 和 SignalListener 基础配置。"));
            return;
        }
        if (targetId.isBlank()) {
            errors.add(error(prefix + ".targetId", "required", "编辑已有节点需要目标 ID。", "", "", "", "", "从画布上的已有节点进入编辑，避免手写 targetId。"));
        }
        if ("channel_metadata".equals(nodeType)) {
            WebAdminChannelMetadataUpdateRequest request = safeDraft.channelMetadata == null ? new WebAdminChannelMetadataUpdateRequest() : safeDraft.channelMetadata;
            String channel = SignalChannel.normalize(safe(request.channel).isBlank() ? targetId : request.channel);
            validateExistingEditGraphMembership(graph, nodeType, channel, prefix + ".targetId", errors);
            validateExistingEditTargetMatches(prefix + ".channelMetadata.channel", nodeType, targetId, channel, errors);
            for (WebAdminValidationError channelError : WebAdminChannelMetadataService.validateChannel(channel, safe(request.channel).isBlank() ? targetId : request.channel)) {
                errors.add(error(prefix + ".channelMetadata." + channelError.field(), channelError.code(), channelError.message(), channelError.rejectedValueSummary(), "channel:" + channel, "", channel, "修正频道显示信息后再保存；channel id 本阶段不可重命名。"));
            }
            for (WebAdminValidationError metadataError : WebAdminChannelMetadataService.validateRequest(request)) {
                errors.add(error(prefix + ".channelMetadata." + metadataError.field(), metadataError.code(), metadataError.message(), metadataError.rejectedValueSummary(), "channel:" + channel, "", channel, "修正频道 displayName / note / iconKey 后再保存。"));
            }
            requireTypedWriteIdentity(prefix + ".channelMetadata", request.expectedFingerprint, request.lockId, "channel:" + channel, errors);
            return;
        }
        if ("signal_join".equals(nodeType)) {
            WebAdminSignalJoinRequest request = safeDraft.signalJoin == null ? new WebAdminSignalJoinRequest() : safeDraft.signalJoin;
            String joinId = SignalJoinStore.normalizeId(safe(request.id).isBlank() ? targetId : request.id);
            validateExistingEditGraphMembership(graph, nodeType, joinId, prefix + ".targetId", errors);
            validateExistingEditTargetMatches(prefix + ".signalJoin.id", nodeType, targetId, joinId, errors);
            if (joinId.isBlank()) {
                errors.add(error(prefix + ".signalJoin.id", "signal_join_id_required", "Signal Join ID 不能为空。", safe(request.id).isBlank() ? targetId : request.id, "signal_join:" + targetId, "", "", "从已有 Signal Join 节点进入编辑，不要手写空 ID。"));
            }
            Set<String> inputChannels = signalJoinRequestInputChannels(request);
            String outputChannel = SignalChannel.normalize(request.outputChannel);
            if (!outputChannel.isBlank() && inputChannels.contains(outputChannel)) {
                errors.add(error(prefix + ".signalJoin.outputChannel", "logic_chain_join_input_output_channel_conflict", "同一个信号汇合中，输入频道不能同时作为输出频道：" + outputChannel, outputChannel, "signal_join:" + joinId, "", outputChannel, "移除该频道的输入，或选择另一个输出频道。"));
            }
            validateDraftJoinCycleGuard(graph, inputChannels, outputChannel.isBlank() ? Set.of() : Set.of(outputChannel), "signal_join:" + joinId, errors);
            requireTypedWriteIdentity(prefix + ".signalJoin", request.expectedFingerprint, request.lockId, "signal_join:" + joinId, errors);
            return;
        }
        if ("timer".equals(nodeType)) {
            WebAdminTimerRequest request = safeDraft.timer == null ? new WebAdminTimerRequest() : safeDraft.timer;
            String timerId = TimerStore.normalizeId(safe(request.id).isBlank() ? targetId : request.id);
            validateExistingEditGraphMembership(graph, nodeType, timerId, prefix + ".targetId", errors);
            validateExistingEditTargetMatches(prefix + ".timer.id", nodeType, targetId, timerId, errors);
            if (timerId.isBlank()) {
                errors.add(error(prefix + ".timer.id", "timer_id_required", "Timer ID 不能为空。", safe(request.id).isBlank() ? targetId : request.id, "timer:" + targetId, "", "", "从已有 Timer 节点进入编辑，不要手写空 ID。"));
            }
            requireTypedWriteIdentity(prefix + ".timer", request.expectedFingerprint, request.lockId, "timer:" + timerId, errors);
            return;
        }
        if ("signal_listener".equals(nodeType)) {
            WebAdminSignalListenerBasicConfigUpdateRequest request = safeDraft.signalListenerBasic == null ? new WebAdminSignalListenerBasicConfigUpdateRequest() : safeDraft.signalListenerBasic;
            String listenerRef = safe(request.listenerRef).isBlank() ? targetId : safe(request.listenerRef);
            validateExistingEditGraphMembership(graph, nodeType, listenerRef, prefix + ".targetId", errors);
            validateExistingEditTargetMatches(prefix + ".signalListenerBasic.listenerRef", nodeType, targetId, listenerRef, errors);
            requireTypedWriteIdentity(prefix + ".signalListenerBasic", request.expectedFingerprint, request.lockId, "listener:" + (safe(request.listenerRef).isBlank() ? targetId : request.listenerRef), errors);
            return;
        }
        if (isDeviceExistingNodeType(nodeType)) {
            validateExistingEditGraphMembership(graph, nodeType, targetId, prefix + ".targetId", errors);
            if (safeDraft.deviceBasic != null) {
                WebAdminDeviceBasicConfigUpdateRequest basic = safeDraft.deviceBasic;
                String deviceId = safe(basic.deviceId).isBlank() ? targetId : basic.deviceId;
                validateExistingEditTargetMatches(prefix + ".deviceBasic.deviceId", nodeType, targetId, deviceId, errors);
                for (WebAdminValidationError basicError : WebAdminDeviceBasicConfigService.validateRequest(basic)) {
                    errors.add(error(prefix + ".deviceBasic." + basicError.field(), basicError.code(), basicError.message(), basicError.rejectedValueSummary(), existingNodeIdForError(nodeType, targetId), "", SignalChannel.normalize(basic.channel), "修正设备基础配置后再保存。"));
                }
                requireTypedWriteIdentity(prefix + ".deviceBasic", basic.expectedFingerprint, basic.lockId, existingNodeIdForError(nodeType, targetId), errors);
            }
            if (safeDraft.deviceMetadata != null) {
                WebAdminDeviceMetadataUpdateRequest metadata = safeDraft.deviceMetadata;
                String deviceId = safe(metadata.deviceId).isBlank() ? targetId : metadata.deviceId;
                validateExistingEditTargetMatches(prefix + ".deviceMetadata.deviceId", nodeType, targetId, deviceId, errors);
                for (WebAdminValidationError metadataError : WebAdminDeviceMetadataService.validateRequest(metadata)) {
                    errors.add(error(prefix + ".deviceMetadata." + metadataError.field(), metadataError.code(), metadataError.message(), metadataError.rejectedValueSummary(), existingNodeIdForError(nodeType, targetId), "", "", "修正设备显示名称 / 备注 / 图标后再保存。"));
                }
                if (metadata.expectedVersion == null) {
                    errors.add(error(prefix + ".deviceMetadata.expectedVersion", "required", "保存设备显示信息需要 expectedVersion。", "", existingNodeIdForError(nodeType, targetId), "", "", "重新从节点编辑入口打开，获取最新 metadata 版本。"));
                }
                if (safe(metadata.lockId).isBlank()) {
                    errors.add(error(prefix + ".deviceMetadata.lockId", "edit_lock_required", "保存设备显示信息需要对应目标编辑锁。", "", existingNodeIdForError(nodeType, targetId), "", "", "重新打开该节点编辑面板，获取设备显示信息编辑锁。"));
                }
            }
            if (safeDraft.virtualBlockDevice != null) {
                if (!"virtual_block_device".equals(nodeType)) {
                    errors.add(error(prefix + ".virtualBlockDevice", "logic_chain_vbd_existing_draft_wrong_node_type", "只有 VBD 节点可以携带 VBD 节点内草稿。", nodeType, existingNodeIdForError(nodeType, targetId), "", "", "重新从 VBD 主节点打开编辑。"));
                } else if (virtualBlockDeviceDraftHasRequirements(safeDraft.virtualBlockDevice) && safeDraft.deviceBasic == null) {
                    errors.add(error(prefix + ".virtualBlockDevice", "logic_chain_vbd_existing_draft_requires_device_lock", "VBD itemSubmit / container 草稿需要同时携带 deviceBasic lock，确保最终保存仍在 Logic Chain typed lock 下执行。", targetId, existingNodeIdForError(nodeType, targetId), "", "", "重新打开 VBD 节点编辑面板后再加入捕获草稿。"));
                } else if ("virtual_block_device".equals(nodeType)) {
                    if (virtualBlockDeviceDraftHasNativeTriggers(safeDraft.virtualBlockDevice)) {
                        WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest nativeTriggers = safeDraft.virtualBlockDevice.nativeTriggers;
                        requireTypedWriteIdentity(prefix + ".virtualBlockDevice.nativeTriggers", nativeTriggers == null ? "" : nativeTriggers.expectedFingerprint, nativeTriggers == null ? "" : nativeTriggers.lockId, existingNodeIdForError(nodeType, targetId), errors);
                        String nativeDeviceId = nativeTriggers == null || safe(nativeTriggers.deviceId).isBlank() ? targetId : safe(nativeTriggers.deviceId);
                        validateExistingEditTargetMatches(prefix + ".virtualBlockDevice.nativeTriggers.deviceId", nodeType, targetId, nativeDeviceId, errors);
                        if (safeDraft.deviceBasic != null || safeDraft.deviceMetadata != null || virtualBlockDeviceDraftHasRequirements(safeDraft.virtualBlockDevice)) {
                            errors.add(error(prefix + ".virtualBlockDevice.nativeTriggers", "logic_chain_vbd_native_trigger_mixed_write_fail_closed", "VBD 原生触发配置暂不能和基础配置、显示信息、itemSubmit 或 container 草稿在同一个已有节点编辑中一起保存，避免非事务批量写入造成半应用。", targetId, existingNodeIdForError(nodeType, targetId), "", "", "先保存 VBD 原生触发配置草稿，再单独保存其它 VBD 节点字段。"));
                        }
                    }
                    WebAdminLogicChainEditorRequest.DraftNode validationNode = new WebAdminLogicChainEditorRequest.DraftNode();
                    validationNode.id = existingNodeIdForError(nodeType, targetId);
                    validationNode.virtualBlockDevice = safeDraft.virtualBlockDevice;
                    validateItemSubmitContainerDraft(validationNode, prefix, errors);
                }
            }
            if (safeDraft.deviceBasic == null && safeDraft.deviceMetadata == null && safeDraft.virtualBlockDevice == null) {
                errors.add(error(prefix, "logic_chain_device_edit_payload_required", "设备节点编辑至少需要 deviceBasic、deviceMetadata 或 VBD 节点内 payload。", nodeType, existingNodeIdForError(nodeType, targetId), "", "", "重新从 Logic Chain 设备节点编辑面板加入草稿。"));
            }
        }
    }

    private static void validateActionEditDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.ActionEditDraft draft,
            int index,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.ActionEditDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionEditDraft() : draft;
        String prefix = "actionEdits[" + Math.max(0, index) + "]";
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = normalizedActionOwnerId(ownerType, safeDraft.ownerId);
        String ownerNodeId = actionAppendNodeId(ownerType, ownerId);
        int actionIndex = parseActionIndex(safeDraft.actionIndex);
        if (!Set.of("listener", "timer", "action_relay", "region_controller").contains(ownerType)) {
            errors.add(error(prefix + ".ownerType", "logic_chain_action_edit_owner_type_deferred", "当前只支持编辑 SignalListener、Timer、ActionRelay 或 Region 上的已有 Action。", safeDraft.ownerType, ownerNodeId, "", "", "从可编辑 Action 卡片进入同 index 编辑；删除或同 bucket 重排请走独立草稿。"));
        }
        if (ownerId.isBlank()) {
            errors.add(error(prefix + ".ownerId", "required", "编辑已有 Action 需要 owner ID。", "", ownerNodeId, "", "", "从已有 Action 卡片进入编辑，保留 owner 信息。"));
        }
        if (actionIndex < 0) {
            errors.add(error(prefix + ".actionIndex", "required", "编辑已有 Action 需要合法的同 index。", String.valueOf(safeDraft.actionIndex), ownerNodeId, "", "", "从已有 Action 卡片进入编辑；删除或同 bucket 重排请走独立草稿。"));
        }
        String operation = safe(safeDraft.operation).toLowerCase(Locale.ROOT);
        if (!Set.of("", "replace", "disable").contains(operation)) {
            errors.add(error(prefix + ".operation", "logic_chain_action_edit_operation_invalid", "Action 维护只支持同 index 替换或禁用。", operation, ownerNodeId, "", "", "选择 replace 或 disable；不支持 delete / reorder。"));
        }
        if ("timer".equals(ownerType) && !Set.of("start", "tick", "complete", "cancel", "onstart", "ontick", "oncomplete", "oncancel", "on_start", "on_tick", "on_complete", "on_cancel", "onStartActions", "onTickActions", "onCompleteActions", "onCancelActions").contains(safe(safeDraft.bucket))) {
            errors.add(error(prefix + ".bucket", "logic_chain_timer_action_bucket_invalid", "Timer action bucket 只支持 start / tick / complete / cancel。", safeDraft.bucket, ownerNodeId, "", "", "选择 Timer 的 onStart、onTick、onComplete 或 onCancel action bucket。"));
        }
        if ("region_controller".equals(ownerType) && parseRegionTrigger(safeDraft.bucket) == null) {
            errors.add(error(prefix + ".bucket", "logic_chain_region_action_bucket_invalid", "Region action bucket 只支持 enter / exit / stay。", safeDraft.bucket, ownerNodeId, "", "", "选择 Region 的 enter、exit 或 stay action bucket。"));
        }
        if (!ownerId.isBlank() && actionIndex >= 0) {
            validateActionEditGraphMembership(graph, ownerType, ownerId, normalizeBucket(safeDraft.bucket), actionIndex, prefix + ".ownerId", errors);
        }
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = actionEntryForActionEditOperation(safeDraft);
        List<WebAdminValidationError> actionErrors = WebAdminActionRelayActionsService.validateActionEntries(List.of(entry));
        for (WebAdminValidationError actionError : actionErrors) {
            errors.add(error(prefix + ".action." + actionError.field(), actionError.code(), actionError.message(), actionError.rejectedValueSummary(), ownerNodeId, "", channelRef(actionError.rejectedValueSummary()), "修正待替换 Action 的字段后再保存；旧 Action 顺序和数量不会变化。"));
        }
        requireTypedWriteIdentity(prefix, safeDraft.expectedFingerprint, safeDraft.lockId, ownerNodeId, errors);
    }

    private static void validateNodeDeleteDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft draft,
            int index,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.NodeDeleteDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.NodeDeleteDraft() : draft;
        String prefix = "nodeDeletes[" + Math.max(0, index) + "]";
        String nodeType = normalizeExistingNodeEditType(safeDraft.nodeType);
        String targetId = safe(safeDraft.targetId);
        if (!Set.of("signal_join", "timer", "signal_listener", "virtual_block_device", "signal_emitter", "signal_receiver", "action_relay", "region_controller").contains(nodeType)) {
            errors.add(error(prefix + ".nodeType", "logic_chain_reference_node_delete_rejected", "这是引用节点或无法证明 owner/store 的节点，不能从 Logic Chain 直接删除。", safeDraft.nodeType, existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "请打开对应管理页面，或删除其 owner typed resource。"));
            return;
        }
        if (targetId.isBlank()) {
            errors.add(error(prefix + ".targetId", "required", "删除已有节点需要目标 ID。", "", "", "", "", "从画布上的 typed-owned 主节点进入删除。"));
        } else {
            validateExistingEditGraphMembership(graph, nodeType, targetId, prefix + ".targetId", errors);
        }
        if (!Boolean.TRUE.equals(safeDraft.confirmed)) {
            errors.add(error(prefix + ".confirmed", "confirmation_required", "节点删除保存前需要二次确认。", String.valueOf(safeDraft.confirmed), existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "在删除 modal 中确认影响范围后再保存。"));
        }
        if (!Boolean.TRUE.equals(safeDraft.impactAccepted)) {
            errors.add(error(prefix + ".impactAccepted", "logic_chain_node_delete_impact_not_accepted", "节点删除保存前必须确认 dry-run 影响范围。", String.valueOf(safeDraft.impactAccepted), existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "阅读删除影响后勾选确认。"));
        }
        if (!NODE_DELETE_CONFIRMATION_TEXT.equals(safe(safeDraft.confirmationText).trim())) {
            errors.add(error(prefix + ".confirmationText", "logic_chain_node_delete_confirm_phrase_required", "节点删除必须输入固定文本“我确认删除该节点”。", safe(safeDraft.confirmationText), existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "完全输入：我确认删除该节点"));
        }
        requireTypedWriteIdentity(prefix, safeDraft.expectedFingerprint, safeDraft.lockId, existingNodeIdForError(nodeType, targetId), errors);
    }

    private static void validateActionDeleteDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft draft,
            int index,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.ActionDeleteDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionDeleteDraft() : draft;
        String prefix = "actionDeletes[" + Math.max(0, index) + "]";
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = normalizedActionOwnerId(ownerType, safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        String ownerNodeId = actionAppendNodeId(ownerType, ownerId);
        int actionIndex = parseActionIndex(safeDraft.actionIndex);
        if (!Set.of("listener", "timer", "action_relay", "region_controller").contains(ownerType)) {
            errors.add(error(prefix + ".ownerType", "logic_chain_action_delete_owner_type_invalid", "Action 删除只支持 SignalListener、Timer、ActionRelay 或 RegionController。", safeDraft.ownerType, ownerNodeId, "", "", "从可编辑 Action 卡片进入删除。"));
        }
        if (ownerId.isBlank()) {
            errors.add(error(prefix + ".ownerId", "required", "删除 Action 需要 owner ID。", "", ownerNodeId, "", "", "从已有 Action 卡片进入删除，保留 owner 信息。"));
        }
        if (actionIndex < 0) {
            errors.add(error(prefix + ".actionIndex", "required", "删除 Action 需要合法 index。", String.valueOf(safeDraft.actionIndex), ownerNodeId, "", "", "从已有 Action 卡片进入删除，不要手写 index。"));
        }
        if ("timer".equals(ownerType) && normalizeTimerActionBucketAlias(bucket).isBlank()) {
            errors.add(error(prefix + ".bucket", "logic_chain_timer_action_bucket_invalid", "Timer action bucket 只支持 start / tick / complete / cancel。", safeDraft.bucket, ownerNodeId, "", "", "选择 Timer 的同一 action bucket。"));
        }
        if ("region_controller".equals(ownerType) && parseRegionTrigger(bucket) == null) {
            errors.add(error(prefix + ".bucket", "logic_chain_region_action_bucket_invalid", "Region action bucket 只支持 enter / exit / stay。", safeDraft.bucket, ownerNodeId, "", "", "选择 Region 的 enter、exit 或 stay action bucket。"));
        }
        if (!ownerId.isBlank() && actionIndex >= 0) {
            validateActionEditGraphMembership(graph, ownerType, ownerId, bucket, actionIndex, prefix + ".ownerId", errors);
        }
        if (!Boolean.TRUE.equals(safeDraft.confirmed)) {
            errors.add(error(prefix + ".confirmed", "confirmation_required", "Action 删除保存前需要二次确认。", String.valueOf(safeDraft.confirmed), ownerNodeId, "", "", "在删除 modal 中确认后再保存；取消编辑不会应用删除。"));
        }
        requireTypedWriteIdentity(prefix, safeDraft.expectedFingerprint, safeDraft.lockId, ownerNodeId, errors);
    }

    private static void validateActionReorderDraft(
            WebAdminDtos.LogicChainGraphDto graph,
            WebAdminLogicChainEditorRequest.ActionReorderDraft draft,
            int index,
            List<WebAdminValidationError> errors
    ) {
        WebAdminLogicChainEditorRequest.ActionReorderDraft safeDraft = draft == null ? new WebAdminLogicChainEditorRequest.ActionReorderDraft() : draft;
        String prefix = "actionReorders[" + Math.max(0, index) + "]";
        String ownerType = normalizeOwnerType(safeDraft.ownerType);
        String ownerId = normalizedActionOwnerId(ownerType, safeDraft.ownerId);
        String bucket = normalizeBucket(safeDraft.bucket);
        String ownerNodeId = actionAppendNodeId(ownerType, ownerId);
        int fromIndex = parseActionIndex(safeDraft.fromIndex);
        int toIndex = parseActionIndex(safeDraft.toIndex);
        if (!Set.of("listener", "timer", "action_relay", "region_controller").contains(ownerType)) {
            errors.add(error(prefix + ".ownerType", "logic_chain_action_reorder_owner_type_invalid", "Action 重排只支持 SignalListener、Timer、ActionRelay 或 RegionController。", safeDraft.ownerType, ownerNodeId, "", "", "从同一个 action 容器内重排。"));
        }
        if (ownerId.isBlank()) {
            errors.add(error(prefix + ".ownerId", "required", "重排 Action 需要 owner ID。", "", ownerNodeId, "", "", "从已有 action 容器进入重排。"));
        }
        if (fromIndex < 0 || toIndex < 0) {
            errors.add(error(prefix + ".fromIndex", "required", "重排 Action 需要合法的 fromIndex / toIndex。", fromIndex + " -> " + toIndex, ownerNodeId, "", "", "只允许同一 bucket 内重排，不允许跨 source/bucket。"));
        }
        if ("timer".equals(ownerType) && normalizeTimerActionBucketAlias(bucket).isBlank()) {
            errors.add(error(prefix + ".bucket", "logic_chain_timer_action_bucket_invalid", "Timer action bucket 只支持 start / tick / complete / cancel。", safeDraft.bucket, ownerNodeId, "", "", "选择 Timer 的同一 action bucket。"));
        }
        if ("region_controller".equals(ownerType) && parseRegionTrigger(bucket) == null) {
            errors.add(error(prefix + ".bucket", "logic_chain_region_action_bucket_invalid", "Region action bucket 只支持 enter / exit / stay。", safeDraft.bucket, ownerNodeId, "", "", "选择 Region 的 enter、exit 或 stay action bucket。"));
        }
        if (!ownerId.isBlank() && fromIndex >= 0) {
            validateActionEditGraphMembership(graph, ownerType, ownerId, bucket, fromIndex, prefix + ".ownerId", errors);
        }
        if (fromIndex == toIndex && fromIndex >= 0) {
            errors.add(error(prefix + ".toIndex", "logic_chain_action_reorder_noop", "Action 重排需要不同的目标位置。", fromIndex + " -> " + toIndex, ownerNodeId, "", "", "选择不同位置，或删除该重排草稿。"));
        }
        if (!Boolean.TRUE.equals(safeDraft.confirmed)) {
            errors.add(error(prefix + ".confirmed", "confirmation_required", "Action 重排保存前需要二次确认。", String.valueOf(safeDraft.confirmed), ownerNodeId, "", "", "在重排 modal 中确认后再保存；取消编辑不会应用重排。"));
        }
        requireTypedWriteIdentity(prefix, safeDraft.expectedFingerprint, safeDraft.lockId, ownerNodeId, errors);
    }

    private static void validateExistingEditGraphMembership(
            WebAdminDtos.LogicChainGraphDto graph,
            String nodeType,
            String targetId,
            String field,
            List<WebAdminValidationError> errors
    ) {
        if (safe(targetId).isBlank()) {
            return;
        }
        if ("channel_metadata".equals(nodeType) && isFallbackGraph(graph)) {
            return;
        }
        if (findExistingEditGraphNode(graph, nodeType, targetId) == null) {
            errors.add(error(field, "logic_chain_existing_node_not_in_graph", "只能编辑当前逻辑链画布中可见的已有主节点。", targetId, existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "从当前画布上的节点进入编辑；引用卡、其它逻辑链或不可见配置不能通过本保存接口修改。"));
        }
    }

    private static boolean isFallbackGraph(WebAdminDtos.LogicChainGraphDto graph) {
        Object fallback = graph == null || graph.stats() == null ? null : graph.stats().get("fallback");
        return Boolean.TRUE.equals(fallback) || "true".equalsIgnoreCase(String.valueOf(fallback));
    }

    private static void validateExistingEditTargetMatches(
            String field,
            String nodeType,
            String targetId,
            String requestTarget,
            List<WebAdminValidationError> errors
    ) {
        String expected = canonicalExistingTarget(nodeType, targetId);
        String actual = canonicalExistingTarget(nodeType, requestTarget);
        if (!expected.isBlank() && !actual.isBlank() && !expected.equals(actual)) {
            errors.add(error(field, "logic_chain_existing_node_target_mismatch", "编辑 payload 的目标 ID 必须与画布节点一致。", requestTarget, existingNodeIdForError(nodeType, targetId), "", channelForExistingNode(nodeType, targetId), "重新从画布节点打开编辑面板，不要手写或替换嵌套配置 ID。"));
        }
    }

    private static WebAdminDtos.LogicChainNodeDto findExistingEditGraphNode(
            WebAdminDtos.LogicChainGraphDto graph,
            String nodeType,
            String targetId
    ) {
        for (WebAdminDtos.LogicChainNodeDto node : graphNodes(graph)) {
            if (existingEditNodeMatches(node, nodeType, targetId)) {
                return node;
            }
        }
        return null;
    }

    private static boolean existingEditNodeMatches(WebAdminDtos.LogicChainNodeDto node, String nodeType, String targetId) {
        if (node == null || isReferenceNode(node)) {
            return false;
        }
        if (isOwnedActionAliasNode(node)) {
            return false;
        }
        String type = safe(node.type()).toLowerCase(Locale.ROOT);
        String refType = safe(node.refType()).toLowerCase(Locale.ROOT);
        Map<String, Object> metadata = node.metadata() == null ? Map.of() : node.metadata();
        String kind = safe(metadata.get("kind")).toLowerCase(Locale.ROOT);
        String target = canonicalExistingTarget(nodeType, targetId);
        if (target.isBlank()) {
            return false;
        }
        if ("channel_metadata".equals(nodeType)) {
            if (!"channel".equals(type)) {
                return false;
            }
            return target.equals(SignalChannel.normalize(node.channel()))
                    || target.equals(SignalChannel.normalize(node.refId()))
                    || (safe(node.id()).startsWith("channel:") && target.equals(SignalChannel.normalize(safe(node.id()).substring("channel:".length()))));
        }
        if ("signal_join".equals(nodeType)) {
            if (!"signal_join".equals(type) && !"signal_join".equals(refType) && !"signal_join".equals(kind)) {
                return false;
            }
            return target.equals(SignalJoinStore.normalizeId(node.refId()))
                    || target.equals(SignalJoinStore.normalizeId(safe(metadata.get("joinId"))));
        }
        if ("timer".equals(nodeType)) {
            if (!"timer".equals(type) && !"timer".equals(refType) && !"timer".equals(kind)) {
                return false;
            }
            return target.equals(TimerStore.normalizeId(node.refId()))
                    || target.equals(TimerStore.normalizeId(safe(metadata.get("timerId"))));
        }
        if ("signal_listener".equals(nodeType)) {
            if (!"listener".equals(refType) && !"signal_listener".equals(type) && !"signal_listener".equals(kind) && !safe(node.id()).startsWith("consumer:listener:")) {
                return false;
            }
            return target.equals(safe(node.refId()))
                    || target.equals(safe(metadata.get("listenerId")))
                    || target.equals(safe(metadata.get("ownerId")));
        }
        if ("virtual_block_device".equals(nodeType)) {
            String sourceType = safe(metadata.get("sourceType")).toLowerCase(Locale.ROOT);
            boolean visibleVbdProducer = "producer".equals(type)
                    && "device".equals(refType)
                    && SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(sourceType);
            if (!"virtual_block_device".equals(refType) && !"virtual_block_device".equals(type) && !"virtual_block_device".equals(kind) && !visibleVbdProducer) {
                return false;
            }
            return target.equals(safe(node.refId()))
                    || target.equals(safe(metadata.get("deviceId")))
                    || target.equals(safe(metadata.get("ownerId")));
        }
        if (isPhysicalDeviceNodeType(nodeType)) {
            String sourceType = safe(metadata.get("sourceType")).toLowerCase(Locale.ROOT);
            boolean typeMatches = nodeType.equals(refType)
                    || nodeType.equals(type)
                    || nodeType.equals(kind)
                    || nodeType.equals(sourceType);
            if (!typeMatches) {
                return false;
            }
            return target.equals(safe(node.refId()))
                    || target.equals(safe(metadata.get("deviceId")))
                    || target.equals(safe(metadata.get("ownerId")));
        }
        if ("region_controller".equals(nodeType)) {
            if (!"region_controller".equals(refType) && !"region_controller".equals(type) && !"region_controller".equals(kind)) {
                return false;
            }
            return target.equals(safe(node.refId()))
                    || target.equals(safe(metadata.get("controllerId")))
                    || target.equals(safe(metadata.get("ownerId")));
        }
        return false;
    }

    private static void validateActionEditGraphMembership(
            WebAdminDtos.LogicChainGraphDto graph,
            String ownerType,
            String ownerId,
            String bucket,
            int actionIndex,
            String field,
            List<WebAdminValidationError> errors
    ) {
        if (isFallbackGraph(graph)) {
            return;
        }
        if (findActionEditGraphNode(graph, ownerType, ownerId, bucket, actionIndex) == null) {
            String ownerNodeId = actionAppendNodeId(ownerType, ownerId);
            errors.add(error(field, "logic_chain_action_edit_target_not_in_graph", "只能编辑当前逻辑链画布中可见的已有 Action。", ownerId, ownerNodeId, "", "", "从当前画布上的 Action 卡片进入编辑；其它 owner 或不可见 action 不能通过本保存接口修改。"));
        }
    }

    private static WebAdminDtos.LogicChainNodeDto findActionEditGraphNode(
            WebAdminDtos.LogicChainGraphDto graph,
            String ownerType,
            String ownerId,
            String bucket,
            int actionIndex
    ) {
        for (WebAdminDtos.LogicChainNodeDto node : graphNodes(graph)) {
            if (actionEditNodeMatches(node, ownerType, ownerId, bucket, actionIndex)) {
                return node;
            }
        }
        return null;
    }

    private static boolean actionEditNodeMatches(
            WebAdminDtos.LogicChainNodeDto node,
            String ownerType,
            String ownerId,
            String bucket,
            int actionIndex
    ) {
        if (node == null) {
            return false;
        }
        if ("action_relay".equals(ownerType) && actionRelaySummaryNodeMatches(node, ownerId, actionIndex)) {
            return true;
        }
        if ("region_controller".equals(ownerType) && regionSignalActionProducerAliasMatches(node, ownerId, bucket, actionIndex)) {
            return true;
        }
        if (isReferenceNode(node)) {
            return false;
        }
        String type = safe(node.type()).toLowerCase(Locale.ROOT);
        if (!Set.of("action", "state_action", "timer_action").contains(type)) {
            return false;
        }
        Map<String, Object> metadata = node.metadata() == null ? Map.of() : node.metadata();
        String metaOwnerType = normalizeOwnerType(safe(metadata.get("ownerType")));
        String metaOwnerId = normalizedActionOwnerId(metaOwnerType, safe(metadata.get("ownerId")));
        int metaIndex = parseActionIndex(metadata.get("actionIndex"));
        if (!ownerType.equals(metaOwnerType) || !ownerId.equals(metaOwnerId) || actionIndex != metaIndex) {
            return false;
        }
        if ("timer".equals(ownerType)) {
            String requestedBucket = normalizeTimerActionBucketAlias(bucket);
            String metaBucket = normalizeTimerActionBucketAlias(safe(metadata.get("timerBucket")));
            return !requestedBucket.isBlank() && requestedBucket.equals(metaBucket);
        }
        if ("region_controller".equals(ownerType)) {
            String requestedBucket = normalizeBucket(bucket);
            String metaBucket = normalizeBucket(firstNonBlank(safe(metadata.get("regionBucket")), safe(metadata.get("triggerType"))));
            return !requestedBucket.isBlank() && requestedBucket.equals(metaBucket);
        }
        return true;
    }

    private static boolean actionRelaySummaryNodeMatches(WebAdminDtos.LogicChainNodeDto node, String ownerId, int actionIndex) {
        if (node == null || actionIndex < 0) {
            return false;
        }
        String type = safe(node.type()).toLowerCase(Locale.ROOT);
        String refType = safe(node.refType()).toLowerCase(Locale.ROOT);
        if (!"consumer".equals(type) || !"action_relay".equals(refType)) {
            return false;
        }
        String refId = safe(node.refId());
        if (!safe(ownerId).equals(refId)) {
            return false;
        }
        Map<String, Object> metadata = node.metadata() == null ? Map.of() : node.metadata();
        int actionCount = parseActionIndex(metadata.get("actionCount"));
        return actionCount > 0 && actionIndex < actionCount;
    }

    private static boolean regionSignalActionProducerAliasMatches(WebAdminDtos.LogicChainNodeDto node, String ownerId, String bucket, int actionIndex) {
        if (node == null || actionIndex < 0) {
            return false;
        }
        Map<String, Object> metadata = node.metadata() == null ? Map.of() : node.metadata();
        if (!"region_signal_action_producer_alias".equals(safe(metadata.get("referenceReason")))) {
            return false;
        }
        if (!"region_controller".equals(safe(node.refType()).toLowerCase(Locale.ROOT)) || !safe(ownerId).equals(safe(node.refId()))) {
            return false;
        }
        if (actionIndex != parseActionIndex(metadata.get("actionIndex"))) {
            return false;
        }
        String requestedBucket = normalizeBucket(bucket);
        String metaBucket = normalizeBucket(safe(metadata.get("triggerType")));
        return !requestedBucket.isBlank() && requestedBucket.equals(metaBucket);
    }

    private static List<WebAdminDtos.LogicChainNodeDto> graphNodes(WebAdminDtos.LogicChainGraphDto graph) {
        return graph == null || graph.nodes() == null ? List.of() : graph.nodes();
    }

    private static boolean isReferenceNode(WebAdminDtos.LogicChainNodeDto node) {
        Map<String, Object> metadata = node == null || node.metadata() == null ? Map.of() : node.metadata();
        return "reference".equals(safe(metadata.get("nodeKind")))
                || Boolean.TRUE.equals(metadata.get("isReferenceCard"));
    }

    private static boolean isOwnedActionAliasNode(WebAdminDtos.LogicChainNodeDto node) {
        Map<String, Object> metadata = node == null || node.metadata() == null ? Map.of() : node.metadata();
        if (Boolean.TRUE.equals(metadata.get("regionActionOwnedAlias"))) {
            return true;
        }
        if ("region_signal_action_producer_alias".equals(safe(metadata.get("referenceReason")))) {
            return true;
        }
        String ownerType = normalizeOwnerType(safe(metadata.get("ownerType")));
        return !ownerType.isBlank() && parseActionIndex(metadata.get("actionIndex")) >= 0;
    }

    private static String canonicalExistingTarget(String nodeType, String targetId) {
        return switch (nodeType) {
            case "channel_metadata" -> SignalChannel.normalize(targetId);
            case "signal_join" -> SignalJoinStore.normalizeId(targetId);
            case "timer" -> TimerStore.normalizeId(targetId);
            case "signal_listener" -> safe(targetId);
            default -> safe(targetId);
        };
    }

    private static String existingNodeIdForError(String nodeType, String targetId) {
        return switch (nodeType) {
            case "channel_metadata" -> "channel:" + SignalChannel.normalize(targetId);
            case "signal_join" -> "signal_join:" + SignalJoinStore.normalizeId(targetId);
            case "timer" -> "timer:" + TimerStore.normalizeId(targetId);
            case "signal_listener" -> "listener:" + safe(targetId);
            case "signal_emitter" -> "signal_emitter:" + safe(targetId);
            case "signal_receiver" -> "signal_receiver:" + safe(targetId);
            case "action_relay" -> "action_relay:" + safe(targetId);
            case "virtual_block_device" -> "virtual_block_device:" + safe(targetId);
            case "region_controller" -> "region_controller:" + safe(targetId);
            default -> safe(targetId);
        };
    }

    private static String channelForExistingNode(String nodeType, String targetId) {
        return "channel_metadata".equals(nodeType) ? SignalChannel.normalize(targetId) : "";
    }

    private static String normalizedActionOwnerId(String ownerType, String ownerId) {
        return "timer".equals(ownerType) ? TimerStore.normalizeId(ownerId) : safe(ownerId);
    }

    private static String normalizeTimerActionBucketAlias(String bucket) {
        return switch (safe(bucket).trim().toLowerCase(Locale.ROOT)) {
            case "start", "onstart", "on_start", "onstartactions" -> "start";
            case "tick", "ontick", "on_tick", "ontickactions" -> "tick";
            case "complete", "oncomplete", "on_complete", "oncompleteactions" -> "complete";
            case "cancel", "oncancel", "on_cancel", "oncancelactions" -> "cancel";
            default -> normalizeBucket(bucket);
        };
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry actionEntryForActionEditOperation(WebAdminLogicChainEditorRequest.ActionEditDraft draft) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = copyActionEntry(draft == null ? null : draft.action);
        if ("disable".equals(safe(draft == null ? "" : draft.operation).toLowerCase(Locale.ROOT))) {
            entry.enabled = Boolean.FALSE;
        }
        return entry;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry copyActionEntry(WebAdminActionRelayActionsUpdateRequest.ActionEntry source) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry src = source == null ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry() : source;
        WebAdminActionRelayActionsUpdateRequest.ActionEntry copy = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        copy.type = src.type;
        copy.value = src.value;
        copy.enabled = src.enabled;
        copy.requiresOp = src.requiresOp;
        copy.cooldownTicks = src.cooldownTicks;
        copy.notifyOps = src.notifyOps;
        copy.conditionGroupId = src.conditionGroupId;
        copy.stateOperation = src.stateOperation;
        copy.stateScope = src.stateScope;
        copy.stateTargetMode = src.stateTargetMode;
        copy.stateTargetId = src.stateTargetId;
        copy.stateKey = src.stateKey;
        copy.stateValueType = src.stateValueType;
        copy.stateValue = src.stateValue;
        copy.stateDelta = src.stateDelta;
        copy.stateCreateIfMissing = src.stateCreateIfMissing;
        copy.stateInitialValue = src.stateInitialValue;
        copy.timerId = src.timerId;
        copy.timerTargetMode = src.timerTargetMode;
        copy.timerTargetId = src.timerTargetId;
        copy.timerStartPolicyOverride = src.timerStartPolicyOverride;
        copy.timerDurationOverrideTicks = src.timerDurationOverrideTicks;
        copy.timerMissingBehavior = src.timerMissingBehavior;
        return copy;
    }

    private static void requireTypedWriteIdentity(String field, String expectedFingerprint, String lockId, String nodeId, List<WebAdminValidationError> errors) {
        if (safe(expectedFingerprint).isBlank()) {
            errors.add(error(field + ".expectedFingerprint", "required", "保存已有配置需要 expectedFingerprint。", "", nodeId, "", "", "重新从节点编辑入口打开，获取最新 fingerprint。"));
        }
        if (safe(lockId).isBlank()) {
            errors.add(error(field + ".lockId", "edit_lock_required", "保存已有配置需要对应目标编辑锁。", "", nodeId, "", "", "重新打开该节点编辑面板，获取对应配置锁。"));
        }
    }

    private static void validateChannelMetadataDrafts(
            WebAdminLogicChainEditorRequest request,
            List<WebAdminValidationError> errors
    ) {
        List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> drafts = request == null ? List.of() : request.channelMetadataDrafts;
        List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> safeDrafts = drafts == null ? List.of() : drafts;
        Set<String> referencedChannels = channelMetadataDraftReferencedChannels(request);
        if (safeDrafts.size() > 32) {
            errors.add(error("channelMetadataDrafts", "logic_chain_channel_metadata_drafts_too_many", "频道端点 metadata 草稿过多，一次最多 32 个。", String.valueOf(safeDrafts.size()), "", "", "", "减少本次新增频道端点 metadata 数量，分多次保存。"));
        }
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (WebAdminLogicChainEditorRequest.ChannelMetadataDraft draft : safeDrafts) {
            String channel = SignalChannel.normalize(draft == null ? "" : draft.channel);
            for (WebAdminValidationError channelError : WebAdminChannelMetadataService.validateChannel(channel, draft == null ? "" : draft.channel)) {
                errors.add(error("channelMetadataDrafts[" + index + "]." + channelError.field(), channelError.code(), channelError.message(), channelError.rejectedValueSummary(), "", "", channel, "修正频道 ID；允许小写字母、数字、下划线、点、冒号和连字符。"));
            }
            if (!channel.isBlank() && !seen.add(channel)) {
                errors.add(error("channelMetadataDrafts[" + index + "].channel", "duplicate_channel", "频道端点 metadata 不能重复提交同一个 channel。", channel, "", "", channel, "删除重复的频道 metadata 草稿，只保留一条。"));
            }
            if (!channel.isBlank() && !referencedChannels.contains(channel)) {
                errors.add(error("channelMetadataDrafts[" + index + "].channel", "logic_chain_channel_metadata_unreferenced", "频道端点 metadata 必须对应本次新增连线或 Signal action 输出频道，不能保存孤立频道。", channel, "", "", channel, "删除未连接的频道 metadata，或先用绿色加号把该频道端点连接到当前草稿。"));
            }
            String displayName = cleanMetadataText(draft == null ? "" : draft.displayName, WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH + 1);
            String note = cleanMetadataText(draft == null ? "" : draft.note, WebAdminChannelMetadataService.MAX_NOTE_LENGTH + 1);
            String iconKey = normalizeIcon(draft == null ? "" : draft.iconKey);
            if (displayName.length() > WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH) {
                errors.add(error("channelMetadataDrafts[" + index + "].displayName", "too_long", "显示名不能超过 64 个字符。", displayName, "", "", channel, "缩短频道显示名到 64 个字符以内。"));
            }
            if (note.length() > WebAdminChannelMetadataService.MAX_NOTE_LENGTH) {
                errors.add(error("channelMetadataDrafts[" + index + "].note", "too_long", "备注不能超过 512 个字符。", note, "", "", channel, "缩短频道备注到 512 个字符以内。"));
            }
            if (containsControl(displayName)) {
                errors.add(error("channelMetadataDrafts[" + index + "].displayName", "control_character", "显示名不能包含控制字符。", displayName, "", "", channel, "删除显示名中的换行、制表符或其它控制字符。"));
            }
            if (containsControl(note)) {
                errors.add(error("channelMetadataDrafts[" + index + "].note", "control_character", "备注不能包含控制字符。", note, "", "", channel, "删除备注中的控制字符。"));
            }
            if (!WebAdminDeviceMetadataService.isAllowedIconKey(iconKey)) {
                errors.add(error("channelMetadataDrafts[" + index + "].iconKey", "invalid_icon", "图标必须来自 WebAdmin 预设列表。", iconKey, "", "", channel, "选择 WebAdmin 已注册的图标 key，或使用 auto。"));
            }
            index++;
        }
    }

    private static Set<String> channelMetadataDraftReferencedChannels(WebAdminLogicChainEditorRequest request) {
        Set<String> channels = new LinkedHashSet<>();
        if (request == null) {
            return channels;
        }
        if (hasActionAppend(request)) {
            addSignalActionReferencedChannel(channels, request.actionAppend == null ? null : request.actionAppend.action);
        }
        if (hasActionEdit(request)) {
            for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request.actionEdits == null ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of() : request.actionEdits) {
                addSignalActionReferencedChannel(channels, edit == null ? null : edit.action);
            }
        }
        if (hasExistingNodeEdit(request)) {
            for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request.existingNodeEdits == null ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of() : request.existingNodeEdits) {
                addExistingNodeEditReferencedChannels(channels, edit);
            }
        }
        if (request.nodes != null && !request.nodes.isEmpty()) {
            for (WebAdminLogicChainEditorRequest.DraftEdge edge : request.edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : request.edges) {
                addResolvedChannel(channels, edgeChannelId(edge));
            }
        }
        return channels;
    }

    private static void addSignalActionReferencedChannel(Set<String> channels, WebAdminActionRelayActionsUpdateRequest.ActionEntry action) {
        if (action != null && "signal".equalsIgnoreCase(safe(action.type))) {
            addChannelRef(channels, action.value);
        }
    }

    private static void addExistingNodeEditReferencedChannels(Set<String> channels, WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit) {
        String nodeType = normalizeExistingNodeEditType(edit == null ? "" : edit.nodeType);
        if ("signal_join".equals(nodeType) && edit != null && edit.signalJoin != null) {
            channels.addAll(signalJoinRequestInputChannels(edit.signalJoin));
            addNormalizedChannel(channels, edit.signalJoin.outputChannel);
        } else if ("timer".equals(nodeType) && edit != null && edit.timer != null) {
            addNormalizedChannel(channels, edit.timer.outputChannel);
        } else if ("signal_listener".equals(nodeType) && edit != null && edit.signalListenerBasic != null) {
            addNormalizedChannel(channels, edit.signalListenerBasic.channel);
        } else if ("channel_metadata".equals(nodeType) && edit != null && edit.channelMetadata != null) {
            addNormalizedChannel(channels, edit.channelMetadata.channel);
        } else if (isDeviceExistingNodeType(nodeType) && edit != null && edit.deviceBasic != null) {
            addNormalizedChannel(channels, edit.deviceBasic.channel);
        }
    }

    private static void addChannelRef(Set<String> channels, String value) {
        addResolvedChannel(channels, channelRef(value));
    }

    private static void addNormalizedChannel(Set<String> channels, String value) {
        addResolvedChannel(channels, SignalChannel.normalize(value));
    }

    private static void addResolvedChannel(Set<String> channels, String channel) {
        if (!channel.isBlank()) {
            channels.add(channel);
        }
    }

    private static RegionTriggerType parseRegionTrigger(String bucket) {
        return switch (normalizeBucket(bucket)) {
            case "enter" -> RegionTriggerType.ENTER;
            case "exit" -> RegionTriggerType.EXIT;
            case "stay" -> RegionTriggerType.STAY;
            default -> null;
        };
    }

    private static String protectedDraftIdFor(WebAdminLogicChainEditorRequest.DraftNode node) {
        if (node == null) {
            return "";
        }
        String direct = safe(node.protectedDraftId);
        if (!direct.isBlank()) {
            return direct;
        }
        String nodeType = normalizeNodeType(node.type);
        if ("virtual_block_device".equals(nodeType) && node.virtualBlockDevice != null) {
            return safe(node.virtualBlockDevice.protectedDraftId);
        }
        if ("world_device".equals(nodeType) && node.worldDevice != null) {
            return safe(node.worldDevice.protectedDraftId);
        }
        if ("region_controller".equals(nodeType) && node.regionController != null) {
            return safe(node.regionController.protectedDraftId);
        }
        return "";
    }

    private static String virtualBlockProtectedDraftId(WebAdminLogicChainEditorRequest.DraftNode node) {
        return protectedDraftIdFor(node);
    }

    private static String protectedDraftObjectType(String nodeType) {
        return switch (normalizeNodeType(nodeType)) {
            case "world_device" -> WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE;
            case "region_controller" -> WebAdminProtectedDraftRegistry.OBJECT_TYPE_REGION_CONTROLLER;
            default -> WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE;
        };
    }

    private static ServerWorld serverWorld(MinecraftServer server, String worldId) {
        if (server == null || safe(worldId).isBlank()) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (world != null && world.getRegistryKey().getValue().toString().equals(safe(worldId))) {
                return world;
            }
        }
        return null;
    }

    private static String metadataString(WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry, String key) {
        Object value = entry == null || entry.metadata() == null ? null : entry.metadata().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static List<RegionGeometry.Point> regionPointsFromProtectedDraft(WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry) {
        java.util.ArrayList<RegionGeometry.Point> points = new java.util.ArrayList<>();
        Object structured = entry == null || entry.metadata() == null ? null : entry.metadata().get("regionPointsStructured");
        if (structured instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    int x = intValue(map.get("x"), Integer.MIN_VALUE);
                    int z = intValue(map.get("z"), Integer.MIN_VALUE);
                    if (x != Integer.MIN_VALUE && z != Integer.MIN_VALUE) {
                        points.add(new RegionGeometry.Point(x, z));
                    }
                }
            }
        }
        if (!points.isEmpty()) {
            return List.copyOf(points);
        }
        String summary = metadataString(entry, "regionPoints");
        for (String pair : summary.split(";")) {
            String[] parts = pair.split(",");
            if (parts.length == 2) {
                int x = intValue(parts[0], Integer.MIN_VALUE);
                int z = intValue(parts[1], Integer.MIN_VALUE);
                if (x != Integer.MIN_VALUE && z != Integer.MIN_VALUE) {
                    points.add(new RegionGeometry.Point(x, z));
                }
            }
        }
        return List.copyOf(points);
    }

    private static RegionTargetFilter regionTargetFilter(WebAdminLogicChainEditorRequest.RegionControllerDraft draft) {
        if (draft == null) {
            return RegionTargetFilter.all();
        }
        String rawType = safe(draft.targetFilterType).trim().toUpperCase(Locale.ROOT);
        try {
            RegionTargetFilter.Type type = rawType.isBlank() ? RegionTargetFilter.Type.ALL : RegionTargetFilter.Type.valueOf(rawType);
            return new RegionTargetFilter(type, safe(draft.targetFilterValue)).normalized();
        } catch (IllegalArgumentException ignored) {
            return RegionTargetFilter.all();
        }
    }

    private static ActionListConversion draftActionsFromEntries(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> safeEntries = entries == null ? List.of() : entries;
        List<WebAdminValidationError> errors = WebAdminActionRelayActionsService.validateActionEntries(safeEntries);
        if (!errors.isEmpty()) {
            return new ActionListConversion(errors, List.of());
        }
        List<ActionConfig> actions = safeEntries.stream()
                .map(WebAdminActionRelayActionsService::actionFromEntry)
                .map(ActionConfig::normalized)
                .toList();
        return new ActionListConversion(List.of(), actions);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value == null ? "" : value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void applyDeviceDraftMetadata(MinecraftServer server, SignalDeviceData device, Object draft, WebAdminUser user) {
        if (server == null || device == null || draft == null) {
            return;
        }
        String displayName = "";
        String note = "";
        String iconKey = "auto";
        if (draft instanceof WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft vbd) {
            displayName = cleanMetadataText(vbd.displayName, WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH);
            note = cleanMetadataText(vbd.note, WebAdminChannelMetadataService.MAX_NOTE_LENGTH);
            iconKey = normalizeIcon(vbd.iconKey);
        } else if (draft instanceof WebAdminLogicChainEditorRequest.WorldDeviceDraft worldDevice) {
            displayName = cleanMetadataText(worldDevice.displayName, WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH);
            note = cleanMetadataText(worldDevice.note, WebAdminChannelMetadataService.MAX_NOTE_LENGTH);
            iconKey = normalizeIcon(worldDevice.iconKey);
        }
        if (!WebAdminDeviceMetadataService.isAllowedIconKey(iconKey)) {
            iconKey = "auto";
        }
        if (displayName.isBlank() && note.isBlank() && ("auto".equals(iconKey) || iconKey.isBlank())) {
            return;
        }
        WebAdminDeviceMetadataStore.MetadataFile file = WebAdminDeviceMetadataStore.load(server);
        String metadataKey = WebAdminDeviceMetadataStore.metadataKey(device.id(), device.type());
        WebAdminDeviceMetadataStore.MetadataEntry before = WebAdminDeviceMetadataStore.MetadataEntry.normalized(metadataKey, file.devices.get(metadataKey));
        WebAdminDeviceMetadataStore.MetadataEntry after = new WebAdminDeviceMetadataStore.MetadataEntry();
        after.deviceId = device.id();
        after.displayName = displayName;
        after.note = note;
        after.iconKey = iconKey;
        after.updatedAt = Instant.now().toString();
        after.updatedBy = user == null ? "" : safe(user.username);
        after.version = before.version + 1L;
        file.devices.put(metadataKey, after);
        WebAdminDeviceMetadataStore.save(server, file);
    }

    private static boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return fallback;
    }

    private static String normalizeExistingNodeEditType(String nodeType) {
        String value = safe(nodeType).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "channel", "channel_metadata", "downstream_channel" -> "channel_metadata";
            case "join", "signal_join" -> "signal_join";
            case "listener", "signal_listener", "virtual_listener" -> "signal_listener";
            case "vbd", "virtual_block", "virtual_block_device" -> "virtual_block_device";
            case "emitter", "signal_emitter" -> "signal_emitter";
            case "receiver", "signal_receiver" -> "signal_receiver";
            case "relay", "action_relay" -> "action_relay";
            case "region", "region_controller" -> "region_controller";
            default -> value;
        };
    }

    private static boolean isDeviceExistingNodeType(String nodeType) {
        return switch (normalizeExistingNodeEditType(nodeType)) {
            case "signal_emitter", "signal_receiver", "action_relay", "virtual_block_device" -> true;
            default -> false;
        };
    }

    private static boolean isPhysicalDeviceNodeType(String nodeType) {
        return switch (normalizeExistingNodeEditType(nodeType)) {
            case "signal_emitter", "signal_receiver", "action_relay" -> true;
            default -> false;
        };
    }

    private static boolean loadedPhysicalDevicePresent(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return false;
        }
        return switch (device.type()) {
            case SignalDeviceData.TYPE_SIGNAL_EMITTER -> SignalDeviceStore.getLoadedEmitter(server, device) != null;
            case SignalDeviceData.TYPE_SIGNAL_RECEIVER -> SignalDeviceStore.getLoadedReceiver(server, device) != null;
            case SignalDeviceData.TYPE_ACTION_RELAY -> SignalDeviceStore.getLoadedActionRelay(server, device) != null;
            default -> false;
        };
    }

    private static Set<String> signalJoinRequestInputChannels(WebAdminSignalJoinRequest request) {
        Set<String> channels = new LinkedHashSet<>();
        List<SignalJoinInputDefinition> inputs = request == null || request.inputChannels == null ? List.of() : request.inputChannels;
        for (SignalJoinInputDefinition input : inputs) {
            String channel = SignalChannel.normalize(input == null ? "" : input.channel);
            if (!channel.isBlank()) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private static String normalizeOwnerType(String ownerType) {
        String value = safe(ownerType).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "signal_listener", "listener" -> "listener";
            case "relay", "action_relay" -> "action_relay";
            case "region", "region_enter", "region_exit", "region_stay", "region_controller" -> "region_controller";
            case "timer" -> "timer";
            default -> value;
        };
    }

    private static String actionTargetKey(String ownerType, String ownerId, String bucket, int actionIndex) {
        return normalizeOwnerType(ownerType) + ":" + safe(ownerId) + ":" + normalizeBucket(bucket) + ":" + actionIndex;
    }

    private static String normalizeBucket(String bucket) {
        return safe(bucket).trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String first, String second) {
        return safe(first).isBlank() ? safe(second) : safe(first);
    }

    private static String firstNonBlank(String first, String second, String third) {
        String value = firstNonBlank(first, second);
        return value.isBlank() ? safe(third) : value;
    }

    private static String normalizeWorldDeviceDraftType(String value) {
        String type = safe(value).trim().toLowerCase(Locale.ROOT);
        return Set.of(
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                SignalDeviceData.TYPE_ACTION_RELAY
        ).contains(type) ? type : "";
    }

    private static String authoritativeWorldDeviceDraftType(WebAdminLogicChainEditorRequest.DraftNode node) {
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry entry = WebAdminProtectedDraftRegistry.get(protectedDraftIdFor(node));
        String protectedType = normalizeWorldDeviceDraftType(metadataString(entry, "deviceType"));
        return protectedType;
    }

    private static WebAdminValidationError worldDeviceTypeMismatchError(String field, String requestType, String protectedType, String storeType, String nodeId) {
        String expected = firstNonBlank(protectedType, storeType);
        String rejected = safe(requestType).isBlank() ? firstNonBlank(protectedType, storeType) : requestType;
        String message = "世界设备引用的设备类型必须以后端 protected draft / store 记录为准，不能由前端请求改写。";
        return error(
                field,
                "logic_chain_world_device_type_mismatch",
                message,
                rejected,
                nodeId,
                "",
                "",
                "重新刷新 Logic Chain，或重新发起世界设备三格 hotbar 放置流程；不要手写 deviceType。后端记录：" + firstNonBlank(expected, "unknown")
        );
    }

    private static int parseActionIndex(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value == null ? "" : value).trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String normalizeIcon(String iconKey) {
        String value = safe(iconKey).trim();
        return value.isBlank() ? "auto" : value;
    }

    private static String cleanMetadataText(String value, int maxLength) {
        String cleaned = safe(value).trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private static boolean containsControl(String value) {
        String safeValue = safe(value);
        for (int i = 0; i < safeValue.length(); i++) {
            if (Character.isISOControl(safeValue.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    static WebAdminWriteContext writeContext(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminWriteTarget target
    ) {
        return WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_LOGIC_CHAIN, target);
    }

    WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(context),
                result,
                beforeSummary,
                afterSummary
        );
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishVbdProtectedDraftWriteAudit(SignalDeviceData device, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        if (device == null || auditEvent == null) {
            return;
        }
        SignalDeviceData normalized = device.normalized();
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(normalized.id())
                .channel(normalized.channel())
                .sourceType("virtual_block_device")
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget("#/history")
                .payload("auditId", auditEvent.auditId())
                .payload("operation", auditEvent.operationType())
                .payload("targetType", auditEvent.targetType())
                .payload("targetId", auditEvent.targetId())
                .payload("deviceId", normalized.id())
                .payload("actor", user == null ? "" : user.username));
    }

    private void publishWorldBackedProtectedDraftWriteAudit(String targetId, String channel, String sourceType, WebAdminAuditEvent auditEvent) {
        if (auditEvent == null) {
            return;
        }
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(safe(targetId))
                .channel(SignalChannel.normalize(channel))
                .sourceType(safe(sourceType))
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget("#/history")
                .payload("auditId", auditEvent.auditId())
                .payload("operation", auditEvent.operationType())
                .payload("targetType", auditEvent.targetType())
                .payload("targetId", auditEvent.targetId())
                .payload("worldBackedCommitRollbackAdapter", true));
    }

    static Map<String, Object> requestSummary(WebAdminLogicChainEditorRequest request) {
        WebAdminLogicChainEditorRequest safeRequest = safeRequest(request);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rootType", normalizeRootType(safeRequest.rootType));
        summary.put("rootRef", safe(safeRequest.rootRef));
        summary.put("baseGraphFingerprint", safe(safeRequest.baseGraphFingerprint));
        summary.put("lockTargetType", safe(safeRequest.lockTargetType));
        summary.put("lockTargetId", safe(safeRequest.lockTargetId));
        summary.put("draftNodeCount", safeRequest.nodes == null ? 0 : safeRequest.nodes.size());
        summary.put("draftEdgeCount", safeRequest.edges == null ? 0 : safeRequest.edges.size());
        summary.put("channelMetadataDraftCount", safeRequest.channelMetadataDrafts == null ? 0 : safeRequest.channelMetadataDrafts.size());
        summary.put("existingNodeEditCount", safeRequest.existingNodeEdits == null ? 0 : safeRequest.existingNodeEdits.size());
        summary.put("actionEditCount", safeRequest.actionEdits == null ? 0 : safeRequest.actionEdits.size());
        summary.put("nodeDeleteCount", safeRequest.nodeDeletes == null ? 0 : safeRequest.nodeDeletes.size());
        summary.put("actionDeleteCount", safeRequest.actionDeletes == null ? 0 : safeRequest.actionDeletes.size());
        summary.put("actionReorderCount", safeRequest.actionReorders == null ? 0 : safeRequest.actionReorders.size());
        if (hasActionAppend(safeRequest)) {
            WebAdminLogicChainEditorRequest.ActionAppendDraft draft = safeRequest.actionAppend;
            summary.put("actionAppendOwnerType", normalizeOwnerType(draft.ownerType));
            summary.put("actionAppendOwnerId", safe(draft.ownerId));
            summary.put("actionAppendBucket", normalizeBucket(draft.bucket));
            summary.put("actionAppendType", draft.action == null ? "" : safe(draft.action.type));
        }
        if (hasExistingNodeEdit(safeRequest)) {
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = safeRequest.existingNodeEdits.getFirst();
            summary.put("existingNodeEditType", normalizeExistingNodeEditType(draft.nodeType));
            summary.put("existingNodeEditTargetId", safe(draft.targetId));
        }
        if (hasActionEdit(safeRequest)) {
            WebAdminLogicChainEditorRequest.ActionEditDraft draft = safeRequest.actionEdits.getFirst();
            summary.put("actionEditOwnerType", normalizeOwnerType(draft.ownerType));
            summary.put("actionEditOwnerId", safe(draft.ownerId));
            summary.put("actionEditBucket", normalizeBucket(draft.bucket));
            summary.put("actionEditIndex", parseActionIndex(draft.actionIndex));
            summary.put("actionEditType", draft.action == null ? "" : safe(draft.action.type));
        }
        if (safeRequest.nodes != null && !safeRequest.nodes.isEmpty()) {
            WebAdminLogicChainEditorRequest.DraftNode node = safeRequest.nodes.getFirst();
            summary.put("nodeType", normalizeNodeType(node.type));
            summary.put("draftNodeId", safe(node.id));
            summary.put("column", safe(node.column));
            summary.put("slot", node.slot);
            summary.put("placed", node.placed);
            if ("signal_join".equals(normalizeNodeType(node.type)) && node.signalJoin != null) {
                summary.put("signalJoinId", safe(node.signalJoin.id));
                summary.put("signalJoinOutputChannel", SignalChannel.normalize(node.signalJoin.outputChannel));
            }
            if ("timer".equals(normalizeNodeType(node.type)) && node.timer != null) {
                summary.put("timerId", safe(node.timer.id));
                summary.put("timerOutputChannel", SignalChannel.normalize(node.timer.outputChannel));
            }
            if ("signal_listener".equals(normalizeNodeType(node.type)) && node.signalListener != null) {
                summary.put("signalListenerName", safe(node.signalListener.name));
                summary.put("signalListenerChannel", SignalChannel.normalize(node.signalListener.channel));
                summary.put("signalListenerConditionGroupId", safe(node.signalListener.conditionGroupId));
            }
            if ("virtual_block_device".equals(normalizeNodeType(node.type))) {
                summary.put("protectedDraftId", protectedDraftIdFor(node));
                summary.put("vbdOutputDerivedFromEdges", true);
            }
            if ("world_device".equals(normalizeNodeType(node.type)) || "region_controller".equals(normalizeNodeType(node.type))) {
                summary.put("protectedDraftId", protectedDraftIdFor(node));
            }
        }
        return summary;
    }

    static Map<String, Object> multiDraftSaveData(WebAdminLogicChainEditorRequest request) {
        Map<String, Object> data = new LinkedHashMap<>(requestSummary(request));
        data.put("multiDraftSession", true);
        data.put("draftOverlaySaved", true);
        return data;
    }

    static String logicChainFailedWriteMode(
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit,
            WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete,
            WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder
    ) {
        if (nodeDelete != null) {
            return "node_delete";
        }
        if (actionDelete != null) {
            return "action_delete";
        }
        if (actionReorder != null) {
            return "action_reorder";
        }
        if (actionEdit != null) {
            return "action_edit";
        }
        if (existingNodeEdit != null) {
            return "existing_node_edit";
        }
        if (actionAppend != null) {
            return "action_append";
        }
        if (draftNode != null) {
            return "new_node";
        }
        return "multi_draft_session";
    }

    static WebAdminWriteTarget target(WebAdminLogicChainEditorRequest request) {
        return new WebAdminWriteTarget("LOGIC_CHAIN_EDITOR", targetId(request), "Logic Chain 编辑草稿");
    }

    static String targetId(WebAdminLogicChainEditorRequest request) {
        String rootType = normalizeRootType(request == null ? "" : request.rootType);
        String rootRef = safe(request == null ? "" : request.rootRef);
        return (rootType + ":" + rootRef).replaceAll("[\\r\\n\\t]", "_");
    }

    static WebAdminWriteResult ok(WebAdminWriteTarget target, String message, Map<String, Object> data) {
        return new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                message,
                target.targetType(),
                target.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data == null ? Map.of() : data
        );
    }

    static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    static WebAdminValidationError error(
            String field,
            String code,
            String message,
            String rejectedValue,
            String nodeId,
            String edgeId,
            String channelId,
            String fixHint
    ) {
        String normalizedChannel = channelRef(channelId);
        if (normalizedChannel.isBlank()) {
            normalizedChannel = channelRef(rejectedValue);
        }
        return new WebAdminValidationError(field, code, message, rejectedValue, nodeId, edgeId, normalizedChannel, "error", fixHint);
    }

    static String normalizeRootType(String rootType) {
        String value = safe(rootType).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join", "timer" -> value;
            default -> "channel";
        };
    }

    private static String normalizeNodeType(String type) {
        return safe(type).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEdgeType(String type) {
        return safe(type).trim().toLowerCase(Locale.ROOT);
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }

    static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }
}
