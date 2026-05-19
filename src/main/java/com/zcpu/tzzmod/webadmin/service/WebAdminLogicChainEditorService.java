package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminRegionControllerRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerActionRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
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
import net.minecraft.server.MinecraftServer;

public final class WebAdminLogicChainEditorService {
    private static final Set<String> SUPPORTED_NODE_TYPES = Set.of("signal_join", "timer");
    private static final int MAX_DRAFT_NODES_PER_SAVE = 1;
    private static final int MAX_DRAFT_EDGES_PER_SAVE = 16;
    private static final int MAX_JOIN_CYCLE_GUARD_NODES = 256;
    private static final int MAX_JOIN_CYCLE_GUARD_EDGES = 512;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminLogicChainService logicChainService;
    private final WebAdminSignalJoinService signalJoinService;
    private final WebAdminTimerService timerService;
    private final WebAdminSignalListenerActionsService signalListenerActionsService;
    private final WebAdminActionRelayActionsService actionRelayActionsService;
    private final WebAdminRegionControllerService regionControllerService;

    public WebAdminLogicChainEditorService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            WebAdminLogicChainService logicChainService,
            WebAdminSignalJoinService signalJoinService,
            WebAdminTimerService timerService,
            WebAdminSignalListenerActionsService signalListenerActionsService,
            WebAdminActionRelayActionsService actionRelayActionsService,
            WebAdminRegionControllerService regionControllerService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.logicChainService = logicChainService == null ? new WebAdminLogicChainService(this.permissionService, this.securityService, editLockService) : logicChainService;
        this.signalJoinService = signalJoinService == null ? new WebAdminSignalJoinService(this.permissionService, this.securityService, editLockService) : signalJoinService;
        this.timerService = timerService == null ? new WebAdminTimerService(this.permissionService, this.securityService, editLockService) : timerService;
        this.signalListenerActionsService = signalListenerActionsService == null ? new WebAdminSignalListenerActionsService(this.permissionService, this.securityService, editLockService) : signalListenerActionsService;
        this.actionRelayActionsService = actionRelayActionsService == null ? new WebAdminActionRelayActionsService(this.permissionService, this.securityService, editLockService) : actionRelayActionsService;
        this.regionControllerService = regionControllerService == null ? new WebAdminRegionControllerService(this.permissionService, this.securityService, editLockService) : regionControllerService;
    }

    public Map<String, Object> capabilities(WebAdminUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", "8.14 Logic Chain Editor MVP");
        data.put("editMode", true);
        data.put("newNodeOnly", true);
        data.put("maxDraftNodesPerSave", MAX_DRAFT_NODES_PER_SAVE);
        data.put("supportedNodeTypes", List.of(
                capability("signal_join", "Signal Join", "创建 SignalJoinDefinition；输入/输出频道写入现有 Signal Join 配置。", List.of("dynamic_downstream_channel_column"), true),
                capability("timer", "Timer", "创建 TimerDefinition；输出频道写入现有 Scheduler Timer 配置；C5 Timer 引用 / 目标位需要 action-list 映射，已 deferred。", List.of("C0"), true),
                capability("action_append", "Action", "在已有 SignalListener / ActionRelay / Region / Timer action list 后追加 1 条 ActionConfig；不移动、不删除、不重排旧 action。", List.of(), true)
        ));
        data.put("deferredNodeTypes", List.of(
                "Virtual SignalListener create (pure config, disabled until Logic Chain editor write path has its own listener create lock)",
                "Condition gate",
                "StateVariable direct create",
                "world device / receiver / region create",
                "old node move/delete/reorder"
        ));
        data.put("virtualSignalListenerRequirement", "虚拟监听器是纯配置对象，不需要世界实体；当前画布新增路径暂未接入安全 listener create edit lock，因此必须先在虚拟监听器页面创建。");
        data.put("worldEntityRequirement", "设备、接收器、ActionRelay、区域等世界实体必须先在游戏内或对应 WebAdmin 页面创建，本编辑器只引用已有对象。");
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
        List<WebAdminValidationError> errors = validateDraftRequest(safeRequest, graph, false);
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
        WebAdminLogicChainEditorRequest safeRequest = safeRequest(request);
        WebAdminWriteTarget target = target(safeRequest);
        WebAdminWriteContext context = writeContext(user, session, remoteAddress, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminWriteResult lock = validateEditorLock(user, session, safeRequest);
        if (!lock.success()) {
            audit(context, lock, requestSummary(safeRequest), Map.of("attempt", "edit_lock_failed"));
            return lock;
        }
        WebAdminDtos.LogicChainGraphDto graph = currentGraph(server, user, session, safeRequest);
        String actualFingerprint = graphFingerprintFor(graph);
        if (safe(safeRequest.baseGraphFingerprint).isBlank() || !actualFingerprint.equals(safe(safeRequest.baseGraphFingerprint))) {
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("expectedFingerprint", safe(safeRequest.baseGraphFingerprint));
            conflict.put("actualFingerprint", actualFingerprint);
            conflict.put("rootType", normalizeRootType(safeRequest.rootType));
            conflict.put("rootRef", safe(safeRequest.rootRef));
            WebAdminWriteResult result = new WebAdminWriteResult(
                    false,
                    WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                    "逻辑链运行图已变化，请刷新后重新进入编辑模式。",
                    target.targetType(),
                    target.targetId(),
                    false,
                    List.of(),
                    "",
                    "",
                    false,
                    conflict,
                    Map.of("baseGraphFingerprint", actualFingerprint)
            );
            audit(context, result, requestSummary(safeRequest), Map.of("attempt", "fingerprint_conflict", "actualFingerprint", actualFingerprint));
            return result;
        }
        List<WebAdminValidationError> errors = validateDraftRequest(safeRequest, graph, true);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, requestSummary(safeRequest), Map.of("attempt", "validation_failed", "errorCount", errors.size()));
            return result;
        }
        WebAdminWriteResult result;
        WebAdminLogicChainEditorRequest.DraftNode draftNode = null;
        if (hasActionAppend(safeRequest)) {
            result = saveActionAppend(server, user, session, remoteAddress, safeRequest.actionAppend, csrfToken, sameOrigin);
        } else {
            draftNode = safeRequest.nodes.getFirst();
            result = switch (normalizeNodeType(draftNode.type)) {
                case "signal_join" -> saveSignalJoin(server, user, session, remoteAddress, draftNode, safeRequest.edges, csrfToken, sameOrigin);
                case "timer" -> saveTimer(server, user, session, remoteAddress, draftNode, safeRequest.edges, csrfToken, sameOrigin);
                default -> WebAdminWriteResult.validationFailed(target, List.of(error(
                        "nodes[0].type",
                        "unsupported_node_type",
                        "当前阶段暂不支持该节点类型。",
                        draftNode.type,
                        safe(draftNode.id),
                        "",
                        "",
                        "请选择当前阶段支持的 Signal Join 或 Timer 草稿节点。"
                )));
            };
        }
        if (!result.success()) {
            result = logicChainSaveFailurePreservingEditorLock(safeRequest, result, draftNode, safeRequest.actionAppend);
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("attempt", "typed_write_failed");
            after.put("mode", hasActionAppend(safeRequest) ? "action_append" : "new_node");
            after.put("code", safe(result.code()));
            after.put("targetType", safe(result.targetType()));
            after.put("targetId", safe(result.targetId()));
            audit(context, result, requestSummary(safeRequest), after);
        }
        if (result.success()) {
            WebAdminWriteResult metadataResult = saveChannelMetadataDrafts(server, user, safeRequest.channelMetadataDrafts);
            if (!metadataResult.success()) {
                audit(context, metadataResult, requestSummary(safeRequest), Map.of("attempt", "channel_metadata_draft_failed_after_typed_write"));
            }
        }
        if (result.success() && editLockService != null && !safe(safeRequest.lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR, targetId(safeRequest), safeRequest.lockId, user, session, remoteAddress);
        }
        return result;
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
            return ok(target(safeRequest), "已退出逻辑链编辑模式。", Map.of("discarded", true));
        }
        WebAdminEditLockRequest lockRequest = new WebAdminEditLockRequest();
        lockRequest.targetType = WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR;
        lockRequest.targetId = targetId(safeRequest);
        lockRequest.lockId = safeRequest.lockId;
        return editLockService.release(user, session, remoteAddress, lockRequest, csrfToken, sameOrigin);
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

    private WebAdminWriteResult saveActionAppend(
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

    private WebAdminWriteResult saveChannelMetadataDrafts(
            MinecraftServer server,
            WebAdminUser user,
            List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> drafts
    ) {
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

    private WebAdminWriteResult logicChainSaveFailurePreservingEditorLock(
            WebAdminLogicChainEditorRequest request,
            WebAdminWriteResult result,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend
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
        List<WebAdminValidationError> errors = logicChainEnrichTypedFailureErrors(result, draftNode, actionAppend);
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
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend
    ) {
        String nodeId = logicChainDraftContextNodeId(draftNode, actionAppend);
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
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend
    ) {
        if (draftNode != null && !safe(draftNode.id).isBlank()) {
            return safe(draftNode.id);
        }
        if (actionAppend != null) {
            String ownerType = normalizeOwnerType(actionAppend.ownerType);
            String ownerId = safe(actionAppend.ownerId);
            return ownerId.isBlank() ? ownerType : ownerType + ":" + ownerId;
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

    private WebAdminWriteResult validateEditorLock(WebAdminUser user, WebAdminSession session, WebAdminLogicChainEditorRequest request) {
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

    private WebAdminWriteResult writePreflight(
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

    private List<WebAdminValidationError> validateDraftRequest(
            WebAdminLogicChainEditorRequest request,
            WebAdminDtos.LogicChainGraphDto graph,
            boolean requireComplete
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        List<WebAdminLogicChainEditorRequest.DraftNode> nodes = request.nodes == null ? List.of() : request.nodes;
        List<WebAdminLogicChainEditorRequest.DraftEdge> edges = request.edges == null ? List.of() : request.edges;
        boolean actionAppend = hasActionAppend(request);
        validateChannelMetadataDrafts(request, errors);
        if (nodes.isEmpty() && !actionAppend) {
            errors.add(error("nodes", "logic_chain_draft_node_required", "请先新增一个草稿节点，或从已有 action 容器追加 1 条 Action。", "", "", "", "", "点击“新增节点”创建 Signal Join / Timer，或从已有 action 容器选择“追加 Action”。"));
        }
        if (!nodes.isEmpty() && actionAppend) {
            errors.add(error("actionAppend", "logic_chain_draft_single_write_only", "一次保存只能新增一个节点，或追加一条 Action，不能混合提交。", "", "", "", "", "保留一种草稿类型：要么保存新增节点，要么保存追加 Action。"));
        }
        if (actionAppend && !edges.isEmpty()) {
            errors.add(error("edges", "logic_chain_action_append_edges_not_allowed", "追加 Action 草稿不能携带新增节点连线。", String.valueOf(edges.size()), "", "", "", "删除 payload 中的 draft edges；Action append 只允许通过 actionAppend.action 写入。"));
        }
        if (nodes.size() > MAX_DRAFT_NODES_PER_SAVE) {
            errors.add(error("nodes", "logic_chain_draft_single_node_only", "8.14 MVP 每次保存只支持 1 个新增节点。", String.valueOf(nodes.size()), "", "", "", "删除多余草稿节点，本阶段一次只保存一个新增节点。"));
        }
        if (edges.size() > MAX_DRAFT_EDGES_PER_SAVE) {
            errors.add(error("edges", "logic_chain_draft_too_many_edges", "草稿连线过多；8.14 MVP 只允许必要上下游连线。", String.valueOf(edges.size()), "", "", "", "移除多余草稿连线，只保留当前新增节点所需的输入和输出频道。"));
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        if (actionAppend) {
            validateActionAppendDraft(request.actionAppend, errors);
            return errors;
        }

        WebAdminLogicChainEditorRequest.DraftNode node = nodes.getFirst();
        String nodeType = normalizeNodeType(node.type);
        String nodeId = safe(node.id);
        if (!nodeId.startsWith("draft:")) {
            errors.add(error("nodes[0].id", "logic_chain_draft_node_id_invalid", "新增节点 ID 必须是 draft: 前缀，不能指向旧节点。", nodeId, nodeId, "", "", "请重新新增节点，不要复用旧图节点 ID。"));
        }
        if (!SUPPORTED_NODE_TYPES.contains(nodeType)) {
            errors.add(error("nodes[0].type", "logic_chain_node_type_deferred", "当前阶段只支持新增 Signal Join 和 Timer；该类型已 deferred。", node.type, nodeId, "", "", "请选择 Signal Join 或 Timer；其它节点类型留到后续阶段。"));
        }
        if (!node.placed) {
            errors.add(error("nodes[0].placed", "logic_chain_draft_node_not_placed", "请先把新节点放入合法槽位。", "false", nodeId, "", "", "把草稿卡片拖到白色合法 slot 后再保存。"));
        }
        validateSlot(node, nodeType, errors);
        validateEdges(graph, node, edges, errors);

        if ("signal_join".equals(nodeType)) {
            validateSignalJoinDraft(graph, node, edges, requireComplete, errors);
        } else if ("timer".equals(nodeType)) {
            validateTimerDraft(node, edges, requireComplete, errors);
        }
        return errors;
    }

    private void validateSlot(WebAdminLogicChainEditorRequest.DraftNode node, String nodeType, List<WebAdminValidationError> errors) {
        String column = safe(node.column);
        String nodeId = safe(node == null ? "" : node.id);
        if ("signal_join".equals(nodeType) && !isSignalJoinPlacementColumn(column)) {
            errors.add(error("nodes[0].column", "logic_chain_join_column_invalid", "Signal Join 只能放在上游频道卡的下游合法列。", column, nodeId, "", "", "把 Signal Join 草稿拖到频道卡右侧出现的白色合法 slot 后再保存；C0 根来源列仍不可用。"));
        }
        if ("timer".equals(nodeType) && !"C0".equalsIgnoreCase(column)) {
            errors.add(error("nodes[0].column", "logic_chain_timer_column_deferred", "Timer 只能放在 C0 来源列；C5 Timer 引用 / 目标位需要 action-list 映射，已 deferred。", column, nodeId, "", "", "把 Timer 草稿拖到 C0 来源列；C5 Timer 引用留到后续阶段。"));
        }
        if (node.slot < 0 || node.slot > 200) {
            errors.add(error("nodes[0].slot", "logic_chain_slot_invalid", "槽位必须是 0 到 200 之间的规范 slot。", String.valueOf(node.slot), nodeId, "", "", "把草稿卡片重新拖到白色合法 slot，使用自动吸附后的规范槽位。"));
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
            if (!Set.of("join_input", "join_output", "timer_outputs_channel").contains(type)) {
                errors.add(error("edges[" + index + "].type", "logic_chain_edge_type_invalid", "当前阶段只支持 Join 输入/输出和 Timer 输出连线。", type, draftId, edgeId, channelId, "删除该连线后，通过草稿节点绿色加号重新连接。"));
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

    private WebAdminDtos.LogicChainGraphDto currentGraph(
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
        return new WebAdminDtos.LogicChainGraphDto(
                metadata,
                root,
                List.of(),
                List.of(root),
                List.of(),
                rootRef.isBlank() ? List.of("当前 root 无法解析到 Signal 频道，图谱为空。") : List.of(),
                stats
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
        item.put("worldEntityRequired", false);
        return item;
    }

    private static WebAdminLogicChainEditorRequest safeRequest(WebAdminLogicChainEditorRequest request) {
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
        WebAdminLogicChainEditorRequest.ActionAppendDraft draft = request == null ? null : request.actionAppend;
        return draft != null && (!safe(draft.ownerType).isBlank() || !safe(draft.ownerId).isBlank() || draft.action != null);
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
        if ("timer".equals(ownerType) && !Set.of("tick", "complete").contains(bucket)) {
            errors.add(error("actionAppend.bucket", "logic_chain_timer_action_bucket_invalid", "Timer action bucket 只支持 tick / complete。", bucket, ownerNodeId, "", "", "选择 Timer 的 tick 或 complete 动作桶。"));
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
            WebAdminLogicChainEditorRequest.ActionAppendDraft append = request.actionAppend;
            WebAdminActionRelayActionsUpdateRequest.ActionEntry action = append == null ? null : append.action;
            if (action != null && "signal".equalsIgnoreCase(safe(action.type))) {
                String channel = channelRef(action.value);
                if (!channel.isBlank()) {
                    channels.add(channel);
                }
            }
            return channels;
        }
        for (WebAdminLogicChainEditorRequest.DraftEdge edge : request.edges == null ? List.<WebAdminLogicChainEditorRequest.DraftEdge>of() : request.edges) {
            String channel = edgeChannelId(edge);
            if (!channel.isBlank()) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private static RegionTriggerType parseRegionTrigger(String bucket) {
        return switch (normalizeBucket(bucket)) {
            case "enter" -> RegionTriggerType.ENTER;
            case "exit" -> RegionTriggerType.EXIT;
            case "stay" -> RegionTriggerType.STAY;
            default -> null;
        };
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

    private static String normalizeBucket(String bucket) {
        return safe(bucket).trim().toLowerCase(Locale.ROOT);
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

    private static WebAdminWriteContext writeContext(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminWriteTarget target
    ) {
        return WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_LOGIC_CHAIN, target);
    }

    private WebAdminAuditEvent audit(
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

    private static Map<String, Object> requestSummary(WebAdminLogicChainEditorRequest request) {
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
        if (hasActionAppend(safeRequest)) {
            WebAdminLogicChainEditorRequest.ActionAppendDraft draft = safeRequest.actionAppend;
            summary.put("actionAppendOwnerType", normalizeOwnerType(draft.ownerType));
            summary.put("actionAppendOwnerId", safe(draft.ownerId));
            summary.put("actionAppendBucket", normalizeBucket(draft.bucket));
            summary.put("actionAppendType", draft.action == null ? "" : safe(draft.action.type));
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
        }
        return summary;
    }

    private static WebAdminWriteTarget target(WebAdminLogicChainEditorRequest request) {
        return new WebAdminWriteTarget("LOGIC_CHAIN_EDITOR", targetId(request), "Logic Chain 新增节点草稿");
    }

    private static String targetId(WebAdminLogicChainEditorRequest request) {
        String rootType = normalizeRootType(request == null ? "" : request.rootType);
        String rootRef = safe(request == null ? "" : request.rootRef);
        return (rootType + ":" + rootRef).replaceAll("[\\r\\n\\t]", "_");
    }

    private static WebAdminWriteResult ok(WebAdminWriteTarget target, String message, Map<String, Object> data) {
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

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static WebAdminValidationError error(
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

    private static String normalizeRootType(String rootType) {
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

    private static String safe(String value) {
        return value == null ? "" : value;
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
