package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionOwnerType;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.region.RegionTargetFilter;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminRegionControllerRequests;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminRegionControllerService {
    public static final int MAX_ACTIONS_PER_TRIGGER = WebAdminActionRelayActionsService.MAX_ACTIONS;
    public static final String CREATE_LOCK_TARGET_ID = "new";
    public static final String CREATE_EXPECTED_FINGERPRINT = "region_controller_create_v1";
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_REGION_ID_LENGTH = 128;
    private static final int MAX_TAG_LENGTH = 64;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator gateBindingValidator = new WebAdminConditionGateBindingValidator();

    public WebAdminRegionControllerService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public List<Map<String, Object>> listControllers(MinecraftServer server, WebAdminUser user, WebAdminSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RegionControllerData controller : RegionControllerStore.getSnapshot(server)) {
            result.add(controllerData(server, controller.normalized(), user, session, false));
        }
        return List.copyOf(result);
    }

    public Map<String, Object> controllerFor(MinecraftServer server, WebAdminUser user, WebAdminSession session, String controllerId) {
        RegionControllerData controller = RegionControllerStore.getController(server, safe(controllerId));
        if (controller == null) {
            return null;
        }
        return controllerData(server, controller.normalized(), user, session, true);
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminRegionControllerRequests.CreateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = new WebAdminWriteTarget("REGION_CONTROLLER", "new", "RegionController");
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "create"));
            return preflight;
        }
        WebAdminWriteResult lockResult = validateLock(target, CREATE_LOCK_TARGET_ID, user, session, request == null ? "" : request.lockId);
        if (lockResult != null) {
            audit(context, lockResult, Map.of(), Map.of("attempt", "create_edit_lock_failed"));
            return lockResult;
        }
        if (request == null || !CREATE_EXPECTED_FINGERPRINT.equals(safe(request.expectedFingerprint))) {
            WebAdminWriteResult result = validation(target, "expectedFingerprint", "required", "创建区域控制器需要 create fingerprint，用于保持 WebAdmin 写入边界一致。", safe(request == null ? "" : request.expectedFingerprint));
            audit(context, result, Map.of(), Map.of("attempt", "create_fingerprint_missing"));
            return result;
        }
        String regionId = safe(request == null ? "" : request.regionId).trim();
        String name = safe(request == null ? "" : request.name).trim();
        List<WebAdminValidationError> errors = new ArrayList<>();
        validateName(name, errors);
        validateRegionId(regionId, errors);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), Map.of("attempt", "create_validation_failed"));
            return result;
        }
        RegionControllerData created = RegionControllerStore.createController(server, regionId, name);
        RegionControllerStore.flushDirty(server);
        target = target(created);
        context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, created, user, session, true),
                "changedFields", List.of("created")
        );
        WebAdminWriteResult result = writeOk(target, true, "区域控制器已创建。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), auditSummary(created));
        publishRealtime(created, "created", auditEvent, user);
        releaseLockAfterWrite(CREATE_LOCK_TARGET_ID, request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            WebAdminRegionControllerRequests.UpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "update_missing"));
            return result;
        }
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            audit(context, preflight, auditSummary(before), Map.of("attempt", "update_preflight"));
            return preflight;
        }
        WebAdminWriteResult lockResult = validateLock(target, safe(controllerId), user, session, request == null ? "" : request.lockId);
        if (lockResult != null) {
            audit(context, lockResult, auditSummary(before), Map.of("attempt", "edit_lock_failed"));
            return lockResult;
        }
        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = validation(target, "expectedFingerprint", "required", "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。", "");
            audit(context, result, auditSummary(before), Map.of("attempt", "fingerprint_missing"));
            return result;
        }
        if (!fingerprintFor(before).equals(request.expectedFingerprint)) {
            WebAdminWriteResult result = conflict(target, before, request.expectedFingerprint);
            audit(context, result, auditSummary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        Validation parsed = validateUpdate(server, request, before);
        if (!parsed.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, parsed.errors());
            audit(context, result, auditSummary(before), Map.of("attempt", "validation_failed"));
            return result;
        }
        RegionControllerData after = new RegionControllerData(
                before.id(),
                parsed.name(),
                parsed.regionId(),
                parsed.enabled(),
                parsed.targetFilter(),
                parsed.stayIntervalTicks(),
                parsed.enterConditionGroupId(),
                parsed.exitConditionGroupId(),
                parsed.stayConditionGroupId(),
                before.enterActions(),
                before.exitActions(),
                before.stayActions()
        ).normalized();
        if (before.normalized().equals(after)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的区域控制器配置变化。");
            audit(context, result, auditSummary(before), auditSummary(after));
            releaseLockAfterWrite(safe(controllerId), request.lockId, user, session, remoteAddress);
            return result;
        }
        RegionControllerStore.updateController(server, before.id(), after);
        RegionControllerStore.flushDirty(server);
        RegionControllerData saved = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, saved == null ? after : saved, user, session, true),
                "changedFields", changedFields(before, after)
        );
        WebAdminWriteResult result = writeOk(target, true, "区域控制器配置已保存。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(saved == null ? after : saved));
        publishRealtime(saved == null ? after : saved, "updated", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult addAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            RegionTriggerType triggerType,
            WebAdminRegionControllerRequests.ActionAddRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "action_add_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_add_denied"));
            return common;
        }
        ActionValidation actionValidation = validateActionEntry(
                server,
                request == null ? null : request.action,
                actionTargetType(triggerType),
                "action.conditionGroupId"
        );
        if (!actionValidation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, actionValidation.errors());
            audit(context, result, auditSummary(before), Map.of("attempt", "action_validation_failed"));
            return result;
        }
        List<ActionConfig> actions = new ArrayList<>(actionsFor(before, triggerType));
        if (actions.size() >= MAX_ACTIONS_PER_TRIGGER) {
            WebAdminWriteResult result = validation(target, "actions", "too_many", "每个触发类型最多支持 " + MAX_ACTIONS_PER_TRIGGER + " 条 Action。", String.valueOf(actions.size()));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_too_many"));
            return result;
        }
        actions.add(actionValidation.action());
        RegionControllerStore.replaceActions(server, before.id(), triggerType, actions);
        RegionControllerStore.flushDirty(server);
        RegionControllerData after = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, after == null ? before : after, user, session, true),
                "changedFields", List.of(triggerKey(triggerType) + "Actions")
        );
        WebAdminWriteResult result = writeOk(target, true, labelTrigger(triggerType) + " Action 已添加。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after == null ? before : after));
        publishRealtime(after == null ? before : after, "action_added", auditEvent, user);
        releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult clearActions(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            RegionTriggerType triggerType,
            WebAdminRegionControllerRequests.ActionClearRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "action_clear_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_clear_denied"));
            return common;
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "清空 " + labelTrigger(triggerType) + " actions 需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "clear_requires_confirmation", "trigger", triggerKey(triggerType)));
            return result;
        }
        if (actionsFor(before, triggerType).isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, labelTrigger(triggerType) + " Action 列表已经为空。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
            return result;
        }
        RegionControllerStore.clearActions(server, before.id(), triggerType);
        RegionControllerStore.flushDirty(server);
        RegionControllerData after = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, after == null ? before : after, user, session, true),
                "changedFields", List.of(triggerKey(triggerType) + "Actions")
        );
        WebAdminWriteResult result = writeOk(target, true, labelTrigger(triggerType) + " Actions 已清空。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after == null ? before : after));
        publishRealtime(after == null ? before : after, "actions_cleared", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult deleteAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            RegionTriggerType triggerType,
            WebAdminRegionControllerRequests.ActionDeleteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "action_delete_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_delete_denied"));
            return common;
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "删除单条 " + labelTrigger(triggerType) + " action 需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_delete_requires_confirmation", "trigger", triggerKey(triggerType)));
            return result;
        }
        List<ActionConfig> actions = new ArrayList<>(actionsFor(before, triggerType));
        int index = parseInteger(request.actionIndex, -1);
        if (index < 0 || index >= actions.size()) {
            WebAdminWriteResult result = validation(target, "actionIndex", "out_of_range", "要删除的 action 已不存在，请刷新后重试。", String.valueOf(index));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_delete_out_of_range", "trigger", triggerKey(triggerType), "index", index));
            return result;
        }
        actions.remove(index);
        RegionControllerStore.replaceActions(server, before.id(), triggerType, actions);
        RegionControllerStore.flushDirty(server);
        RegionControllerData after = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, after == null ? before : after, user, session, true),
                "changedFields", List.of(triggerKey(triggerType) + "Actions")
        );
        WebAdminWriteResult result = writeOk(target, true, labelTrigger(triggerType) + " Action 已删除。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after == null ? before : after));
        publishRealtime(after == null ? before : after, "action_deleted", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult reorderAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            RegionTriggerType triggerType,
            Object fromIndexValue,
            Object toIndexValue,
            Boolean confirmed,
            String expectedFingerprint,
            String lockId,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "action_reorder_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), safe(lockId), safe(expectedFingerprint), before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_reorder_denied"));
            return common;
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "重排 " + labelTrigger(triggerType) + " action 需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_reorder_requires_confirmation", "trigger", triggerKey(triggerType)));
            return result;
        }
        List<ActionConfig> actions = new ArrayList<>(actionsFor(before, triggerType));
        int fromIndex = parseInteger(fromIndexValue, -1);
        int toIndex = parseInteger(toIndexValue, -1);
        if (fromIndex < 0 || fromIndex >= actions.size() || toIndex < 0 || toIndex >= actions.size()) {
            WebAdminWriteResult result = validation(target, "fromIndex", "out_of_range", "要重排的 action 已不存在，请刷新后重试。", fromIndex + " -> " + toIndex);
            audit(context, result, auditSummary(before), Map.of("attempt", "action_reorder_out_of_range", "trigger", triggerKey(triggerType), "fromIndex", fromIndex, "toIndex", toIndex));
            return result;
        }
        if (fromIndex == toIndex) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, labelTrigger(triggerType) + " Action 顺序没有变化。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), lockId, user, session, remoteAddress);
            return result;
        }
        ActionConfig moving = actions.remove(fromIndex);
        actions.add(toIndex, moving);
        RegionControllerStore.replaceActions(server, before.id(), triggerType, actions);
        RegionControllerStore.flushDirty(server);
        RegionControllerData after = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, after == null ? before : after, user, session, true),
                "changedFields", List.of(triggerKey(triggerType) + "Actions"),
                "fromIndex", fromIndex,
                "toIndex", toIndex
        );
        WebAdminWriteResult result = writeOk(target, true, labelTrigger(triggerType) + " Action 已重排。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after == null ? before : after));
        publishRealtime(after == null ? before : after, "action_reordered", auditEvent, user);
        releaseLockAfterWrite(before.id(), lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult updateAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            RegionTriggerType triggerType,
            WebAdminRegionControllerRequests.ActionUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "action_update_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_update_denied"));
            return common;
        }
        List<ActionConfig> actions = new ArrayList<>(actionsFor(before, triggerType));
        int index = parseInteger(request == null ? null : request.actionIndex, -1);
        if (index < 0 || index >= actions.size()) {
            WebAdminWriteResult result = validation(target, "actionIndex", "out_of_range", "要编辑的 action 已不存在，请刷新后重试。", String.valueOf(index));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_update_out_of_range", "trigger", triggerKey(triggerType), "index", index));
            return result;
        }
        ActionValidation actionValidation = validateActionEntry(
                server,
                request == null ? null : request.action,
                actionTargetType(triggerType),
                "action.conditionGroupId"
        );
        if (!actionValidation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, actionValidation.errors());
            audit(context, result, auditSummary(before), Map.of("attempt", "action_update_validation_failed", "trigger", triggerKey(triggerType), "index", index));
            return result;
        }
        ActionConfig beforeAction = actions.get(index);
        actions.set(index, actionValidation.action());
        if (beforeAction != null && beforeAction.equals(actionValidation.action())) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 " + labelTrigger(triggerType) + " Action 变化。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
            return result;
        }
        RegionControllerStore.replaceActions(server, before.id(), triggerType, actions);
        RegionControllerStore.flushDirty(server);
        RegionControllerData after = RegionControllerStore.getController(server, before.id());
        Map<String, Object> data = Map.of(
                "controller", controllerData(server, after == null ? before : after, user, session, true),
                "changedFields", List.of(triggerKey(triggerType) + "Actions"),
                "actionIndex", index
        );
        WebAdminWriteResult result = writeOk(target, true, labelTrigger(triggerType) + " Action 已更新。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after == null ? before : after));
        publishRealtime(after == null ? before : after, "action_updated", auditEvent, user);
        releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String controllerId,
            WebAdminRegionControllerRequests.DeleteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        RegionControllerData before = RegionControllerStore.getController(server, safe(controllerId));
        WebAdminWriteTarget target = before == null ? target(safe(controllerId), safe(controllerId)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_REGION, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器不存在或已被删除。");
            audit(context, result, Map.of(), Map.of("attempt", "delete_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, safe(controllerId), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "delete_denied"));
            return common;
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed) || !confirmationMatches(request.confirmationText, before)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "删除区域控制器需要输入控制器名称或 ID 二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "delete_requires_confirmation"));
            return result;
        }
        boolean removed = RegionControllerStore.deleteController(server, before.id());
        RegionControllerStore.flushDirty(server);
        if (!removed) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "区域控制器已不存在。");
            audit(context, result, auditSummary(before), Map.of("attempt", "delete_missing_after_confirm"));
            return result;
        }
        Map<String, Object> data = Map.of("controllerId", before.id(), "changedFields", List.of("deleted"));
        WebAdminWriteResult result = writeOk(target, true, "区域控制器已删除。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), Map.of("deleted", true, "controllerId", before.id()));
        publishRealtime(before, "deleted", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
    }

    public static String fingerprintFor(RegionControllerData raw) {
        RegionControllerData controller = raw == null ? null : raw.normalized();
        if (controller == null) {
            return "";
        }
        String input = "region_controller|"
                + controller.id()
                + "|name=" + safe(controller.name())
                + "|regionId=" + safe(controller.regionId())
                + "|enabled=" + controller.enabled()
                + "|target=" + targetFilterFingerprint(controller.targetFilter())
                + "|stay=" + controller.stayIntervalTicks()
                + "|enterConditionGroupId=" + controller.enterConditionGroupId()
                + "|exitConditionGroupId=" + controller.exitConditionGroupId()
                + "|stayConditionGroupId=" + controller.stayConditionGroupId()
                + "|enter=" + actionFingerprintList(controller.enterActions())
                + "|exit=" + actionFingerprintList(controller.exitActions())
                + "|stayActions=" + actionFingerprintList(controller.stayActions());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private Map<String, Object> controllerData(MinecraftServer server, RegionControllerData raw, WebAdminUser user, WebAdminSession session, boolean detailed) {
        RegionControllerData controller = raw.normalized();
        MapDataStore.PlannerRegionData planner = MapDataStore.getPlannerRegion(server, controller.regionId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", controller.id());
        data.put("name", controller.name());
        data.put("regionId", controller.regionId());
        data.put("regionName", planner == null ? "" : planner.name());
        data.put("world", planner == null ? "" : planner.dimensionId());
        data.put("enabled", controller.enabled());
        data.put("targetFilter", targetFilterDto(controller.targetFilter()));
        data.put("targetFilterLabel", targetFilterLabel(controller.targetFilter()));
        data.put("stayIntervalTicks", controller.stayIntervalTicks());
        data.put("enterConditionGroupId", controller.enterConditionGroupId());
        data.put("exitConditionGroupId", controller.exitConditionGroupId());
        data.put("stayConditionGroupId", controller.stayConditionGroupId());
        data.put("conditionGateTargetTypes", Map.of(
                "enter", ConditionRuntimeTargetType.REGION_ENTER.id(),
                "exit", ConditionRuntimeTargetType.REGION_EXIT.id(),
                "stay", ConditionRuntimeTargetType.REGION_STAY.id()
        ));
        data.put("actionConditionGateTargetTypes", Map.of(
                "enter", ConditionRuntimeTargetType.REGION_ENTER_ACTION.id(),
                "exit", ConditionRuntimeTargetType.REGION_EXIT_ACTION.id(),
                "stay", ConditionRuntimeTargetType.REGION_STAY_ACTION.id()
        ));
        data.put("conditionGateTargetId", controller.id());
        data.put("recentConditionGates", Map.of(
                "enter", WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.REGION_ENTER, controller.id()),
                "exit", WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.REGION_EXIT, controller.id()),
                "stay", WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.REGION_STAY, controller.id())
        ));
        data.put("enterActionCount", controller.enterActions().size());
        data.put("exitActionCount", controller.exitActions().size());
        data.put("stayActionCount", controller.stayActions().size());
        data.put("actionCount", controller.enterActions().size() + controller.exitActions().size() + controller.stayActions().size());
        data.put("boundChannels", boundChannels(controller));
        data.put("doctorStatus", planner == null ? "WARNING" : controller.enabled() ? "OK" : "WARNING");
        data.put("orphanRegion", planner == null);
        data.put("expectedFingerprint", fingerprintFor(controller));
        data.put("lockStatus", editLockService == null ? null : editLockService.status(WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controller.id(), user, session));
        data.put("allowedActionTypes", List.of("command", "signal", "message", "sound", "state_variable", "timer_start", "timer_cancel"));
        data.put("minStayIntervalTicks", RegionControllerData.MIN_STAY_INTERVAL_TICKS);
        data.put("defaultStayIntervalTicks", RegionControllerData.DEFAULT_STAY_INTERVAL_TICKS);
        data.put("noRawJson", true);
        data.put("noConditionEngine", false);
        data.put("conditionRuntimeGates", true);
        data.put("singleActionConditionGates", true);
        if (detailed) {
            data.put("actions", Map.of(
                    "enter", actionDtos(controller.enterActions(), controller.id(), "enter"),
                    "exit", actionDtos(controller.exitActions(), controller.id(), "exit"),
                    "stay", actionDtos(controller.stayActions(), controller.id(), "stay")
            ));
            data.put("notes", List.of(
                    "RegionController 只编辑已有 enter / exit / stay action 列表和三个外层条件组 gate，不新增 ConditionEngine、路径可视化或 raw JSON。",
                    "未配置条件组 = 保持旧区域控制器逻辑，不拦截；配置后仅阻断对应 enter / exit / stay Action 列表。",
                    "单条 Action 条件组为空 = 此 action 不单独判断，保持旧执行逻辑；配置后仅跳过当前 action 并继续后续 action。",
                    "状态变量动作使用结构化字段写入 GLOBAL / PLAYER StateVariable，不提供 raw JSON、脚本、表达式或 NBT path。",
                    "command action 会阻断 stop/op/ban/kick/whitelist 等危险服务器管理命令。",
                    "stayIntervalTicks 小于 " + RegionControllerData.MIN_STAY_INTERVAL_TICKS + " 会被拒绝。"
            ));
        }
        return data;
    }

    private WebAdminWriteResult validateWriteCommon(WebAdminUser user, WebAdminSession session, String csrfToken, boolean sameOrigin, WebAdminWriteTarget target, String controllerId, String lockId, String expectedFingerprint, RegionControllerData before) {
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            return preflight;
        }
        WebAdminWriteResult lockResult = validateLock(target, controllerId, user, session, lockId);
        if (lockResult != null) {
            return lockResult;
        }
        if (isBlank(expectedFingerprint)) {
            return validation(target, "expectedFingerprint", "required", "写入需要 expectedFingerprint，用于防止覆盖其他操作的修改。", "");
        }
        if (!fingerprintFor(before).equals(expectedFingerprint)) {
            return conflict(target, before, expectedFingerprint);
        }
        return null;
    }

    private WebAdminWriteResult writePreflight(WebAdminUser user, WebAdminSession session, String csrfToken, boolean sameOrigin, WebAdminWriteTarget target) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_REGION);
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
        return WebAdminWriteResult.ok(target, false, "写请求安全校验通过。");
    }

    private WebAdminWriteResult validateLock(WebAdminWriteTarget target, String controllerId, WebAdminUser user, WebAdminSession session, String lockId) {
        if (editLockService == null) {
            return null;
        }
        WebAdminEditLockService.LockValidation validation = editLockService.validateLock(WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, lockId, user, session);
        return validation.success() ? null : validation.result();
    }

    private Validation validateUpdate(MinecraftServer server, WebAdminRegionControllerRequests.UpdateRequest request, RegionControllerData before) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        String name = safe(request.name).trim();
        String regionId = safe(request.regionId).trim();
        boolean enabled = parseBoolean(request.enabled, before.enabled());
        int stayIntervalTicks = parseInteger(request.stayIntervalTicks, before.stayIntervalTicks());
        validateName(name, errors);
        validateRegionId(regionId, errors);
        if (stayIntervalTicks < RegionControllerData.MIN_STAY_INTERVAL_TICKS) {
            errors.add(new WebAdminValidationError("stayIntervalTicks", "too_small", "stayIntervalTicks 不能小于 " + RegionControllerData.MIN_STAY_INTERVAL_TICKS + " ticks。", String.valueOf(stayIntervalTicks)));
        }
        gateBindingValidator.validate(server, errors, "enterConditionGroupId", request.enterConditionGroupId, ConditionRuntimeTargetType.REGION_ENTER);
        gateBindingValidator.validate(server, errors, "exitConditionGroupId", request.exitConditionGroupId, ConditionRuntimeTargetType.REGION_EXIT);
        gateBindingValidator.validate(server, errors, "stayConditionGroupId", request.stayConditionGroupId, ConditionRuntimeTargetType.REGION_STAY);
        RegionTargetFilter filter = parseTargetFilter(request.targetFilterType, request.targetFilterValue, errors);
        return new Validation(
                errors,
                enabled,
                name,
                regionId,
                filter,
                stayIntervalTicks,
                WebAdminConditionGroupStore.normalizeId(request.enterConditionGroupId),
                WebAdminConditionGroupStore.normalizeId(request.exitConditionGroupId),
                WebAdminConditionGroupStore.normalizeId(request.stayConditionGroupId)
        );
    }

    private static void validateName(String value, List<WebAdminValidationError> errors) {
        if (isBlank(value)) {
            errors.add(new WebAdminValidationError("name", "required", "控制器名称不能为空。", ""));
        } else if (value.length() > MAX_NAME_LENGTH) {
            errors.add(new WebAdminValidationError("name", "too_long", "控制器名称最多 " + MAX_NAME_LENGTH + " 个字符。", value));
        } else if (containsControl(value)) {
            errors.add(new WebAdminValidationError("name", "invalid_control", "控制器名称不能包含控制字符。", value));
        }
    }

    private static void validateRegionId(String value, List<WebAdminValidationError> errors) {
        if (isBlank(value)) {
            errors.add(new WebAdminValidationError("regionId", "required", "绑定区域 ID 不能为空。", ""));
        } else if (value.length() > MAX_REGION_ID_LENGTH) {
            errors.add(new WebAdminValidationError("regionId", "too_long", "绑定区域 ID 最多 " + MAX_REGION_ID_LENGTH + " 个字符。", value));
        } else if (containsControl(value)) {
            errors.add(new WebAdminValidationError("regionId", "invalid_control", "绑定区域 ID 不能包含控制字符。", value));
        }
    }

    private static RegionTargetFilter parseTargetFilter(String rawType, String rawValue, List<WebAdminValidationError> errors) {
        String type = safe(rawType).trim().toUpperCase(Locale.ROOT);
        if (type.isBlank()) {
            type = "ALL";
        }
        if ("ALL".equals(type)) {
            return RegionTargetFilter.all();
        }
        if ("OP".equals(type)) {
            return new RegionTargetFilter(RegionTargetFilter.Type.OP, "");
        }
        if ("TAG".equals(type)) {
            String value = safe(rawValue).trim();
            if (value.isBlank()) {
                errors.add(new WebAdminValidationError("targetFilterValue", "required", "TAG 目标过滤必须填写 tag。", ""));
            } else if (value.length() > MAX_TAG_LENGTH) {
                errors.add(new WebAdminValidationError("targetFilterValue", "too_long", "tag 最多 " + MAX_TAG_LENGTH + " 个字符。", value));
            } else if (containsControl(value)) {
                errors.add(new WebAdminValidationError("targetFilterValue", "invalid_control", "tag 不能包含控制字符。", value));
            }
            return new RegionTargetFilter(RegionTargetFilter.Type.TAG, value);
        }
        errors.add(new WebAdminValidationError("targetFilterType", "invalid_type", "targetFilter 只支持 ALL / OP / TAG。", rawType));
        return RegionTargetFilter.all();
    }

    private ActionValidation validateActionEntry(
            MinecraftServer server,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry,
            ConditionRuntimeTargetType actionTargetType,
            String conditionField
    ) {
        WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
        request.actions = entry == null ? List.of() : List.of(entry);
        List<WebAdminValidationError> errors = WebAdminActionRelayActionsService.validateActionEntries(request.actions, actionOwnerType(actionTargetType));
        if (!errors.isEmpty()) {
            return new ActionValidation(errors, null);
        }
        if (entry == null) {
            return new ActionValidation(List.of(new WebAdminValidationError("action", "required", "Action 配置不能为空。", "")), null);
        }
        gateBindingValidator.validate(
                server,
                errors,
                isBlank(conditionField) ? "action.conditionGroupId" : conditionField,
                entry.conditionGroupId,
                actionTargetType
        );
        if (!errors.isEmpty()) {
            return new ActionValidation(errors, null);
        }
        ActionConfig action = WebAdminActionRelayActionsService.actionFromEntry(entry);
        return new ActionValidation(List.of(), action);
    }

    private static ActionType parseActionType(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        for (ActionType type : ActionType.values()) {
            if (type.id().equals(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return ActionType.COMMAND;
    }

    private static String normalizeActionValue(ActionType type, String raw) {
        String value = safe(raw).trim();
        if (type == ActionType.COMMAND) {
            return ActionConfig.normalizeCommand(value);
        }
        if (type == ActionType.SIGNAL) {
            return SignalChannel.normalize(value);
        }
        return value;
    }

    private static Map<String, Object> targetFilterDto(RegionTargetFilter raw) {
        RegionTargetFilter filter = raw == null ? RegionTargetFilter.all() : raw.normalized();
        return Map.of(
                "type", filter.type().name(),
                "value", filter.value()
        );
    }

    private static String targetFilterLabel(RegionTargetFilter raw) {
        RegionTargetFilter filter = raw == null ? RegionTargetFilter.all() : raw.normalized();
        if (filter.type() == RegionTargetFilter.Type.TAG) {
            return "TAG:" + filter.value();
        }
        return filter.type().name();
    }

    private static List<Map<String, Object>> actionDtos(List<ActionConfig> actions, String controllerId, String bucket) {
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action == null) {
                continue;
            }
            ConditionRuntimeTargetType actionTargetType = ConditionActionGateService.regionActionTargetType(bucket);
            String actionTargetId = ConditionActionGateService.regionActionTargetId(controllerId, bucket, index);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", index);
            entry.put("displayIndex", index + 1);
            entry.put("type", action.type().id());
            entry.put("value", action.value());
            entry.put("enabled", action.enabled());
            entry.put("requiresOp", action.requiresOp());
            entry.put("cooldownTicks", action.cooldownTicks());
            entry.put("notifyOps", action.notifyOps());
            entry.put("conditionGroupId", action.conditionGroupId());
            WebAdminActionRelayActionsService.putStateActionFields(entry, action);
            WebAdminActionRelayActionsService.putTimerActionFields(entry, action);
            entry.put("actionConditionGateTargetType", actionTargetType.id());
            entry.put("actionConditionGateTargetId", actionTargetId);
            entry.put("recentActionConditionGate", WebAdminConditionGateHistoryService.recentStatus(actionTargetType, actionTargetId));
            entry.put("summary", actionSummary(action));
            result.add(entry);
            index++;
        }
        return List.copyOf(result);
    }

    private static List<ActionConfig> actionsFor(RegionControllerData controller, RegionTriggerType triggerType) {
        if (controller == null || triggerType == null) {
            return List.of();
        }
        return switch (triggerType) {
            case ENTER -> controller.enterActions();
            case EXIT -> controller.exitActions();
            case STAY -> controller.stayActions();
        };
    }

    private static List<String> boundChannels(RegionControllerData controller) {
        List<String> channels = new ArrayList<>();
        collectSignalChannels(channels, controller.enterActions());
        collectSignalChannels(channels, controller.exitActions());
        collectSignalChannels(channels, controller.stayActions());
        return List.copyOf(channels.stream().distinct().toList());
    }

    private static void collectSignalChannels(List<String> channels, List<ActionConfig> actions) {
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action != null && action.type() == ActionType.SIGNAL && !isBlank(action.value())) {
                channels.add(SignalChannel.normalize(action.value()));
            }
        }
    }

    private static List<String> changedFields(RegionControllerData before, RegionControllerData after) {
        List<String> fields = new ArrayList<>();
        if (!safe(before.name()).equals(safe(after.name()))) fields.add("name");
        if (!safe(before.regionId()).equals(safe(after.regionId()))) fields.add("regionId");
        if (before.enabled() != after.enabled()) fields.add("enabled");
        if (!targetFilterFingerprint(before.targetFilter()).equals(targetFilterFingerprint(after.targetFilter()))) fields.add("targetFilter");
        if (before.stayIntervalTicks() != after.stayIntervalTicks()) fields.add("stayIntervalTicks");
        if (!before.enterConditionGroupId().equals(after.enterConditionGroupId())) fields.add("enterConditionGroupId");
        if (!before.exitConditionGroupId().equals(after.exitConditionGroupId())) fields.add("exitConditionGroupId");
        if (!before.stayConditionGroupId().equals(after.stayConditionGroupId())) fields.add("stayConditionGroupId");
        return List.copyOf(fields);
    }

    private static String actionSummary(ActionConfig action) {
        return WebAdminActionSummaryService.displaySummary(action);
    }

    private static String auditActionSummary(ActionConfig action) {
        return WebAdminActionSummaryService.auditSummary(action);
    }

    private static Map<String, Object> auditSummary(RegionControllerData controller) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (controller == null) {
            return summary;
        }
        RegionControllerData normalized = controller.normalized();
        summary.put("id", normalized.id());
        summary.put("name", normalized.name());
        summary.put("regionId", normalized.regionId());
        summary.put("enabled", normalized.enabled());
        summary.put("targetFilter", targetFilterLabel(normalized.targetFilter()));
        summary.put("stayIntervalTicks", normalized.stayIntervalTicks());
        summary.put("enterConditionGroupId", normalized.enterConditionGroupId());
        summary.put("exitConditionGroupId", normalized.exitConditionGroupId());
        summary.put("stayConditionGroupId", normalized.stayConditionGroupId());
        summary.put("enterActionCount", normalized.enterActions().size());
        summary.put("exitActionCount", normalized.exitActions().size());
        summary.put("stayActionCount", normalized.stayActions().size());
        summary.put("enterActions", WebAdminActionSummaryService.auditSummaryList(normalized.enterActions()));
        summary.put("exitActions", WebAdminActionSummaryService.auditSummaryList(normalized.exitActions()));
        summary.put("stayActions", WebAdminActionSummaryService.auditSummaryList(normalized.stayActions()));
        summary.put("expectedFingerprint", fingerprintFor(normalized));
        return summary;
    }

    private static String targetFilterFingerprint(RegionTargetFilter raw) {
        RegionTargetFilter filter = raw == null ? RegionTargetFilter.all() : raw.normalized();
        return filter.type().name() + ":" + safe(filter.value());
    }

    private static List<String> actionFingerprintList(List<ActionConfig> actions) {
        return (actions == null ? List.<ActionConfig>of() : actions).stream()
                .filter(action -> action != null)
                .map(action -> action.type().id()
                        + "|value=" + safe(action.value())
                        + "|enabled=" + action.enabled()
                        + "|requiresOp=" + action.requiresOp()
                        + "|cooldownTicks=" + action.cooldownTicks()
                        + "|notifyOps=" + action.notifyOps()
                        + "|conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId())
                        + stateFingerprintSuffix(action))
                .toList();
    }

    private static String stateFingerprintSuffix(ActionConfig action) {
        return action == null || action.type() != ActionType.STATE_VARIABLE ? "" : "|" + action.stateFingerprint();
    }

    private WebAdminWriteResult writeOk(WebAdminWriteTarget target, boolean changed, String message, Map<String, Object> data) {
        return new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                message,
                target.targetType(),
                target.targetId(),
                changed,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data == null ? Map.of() : data
        );
    }

    private static WebAdminWriteResult validation(WebAdminWriteTarget target, String field, String code, String message, String rejectedValue) {
        return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(field, code, message, rejectedValue)));
    }

    private static WebAdminWriteResult conflict(WebAdminWriteTarget target, RegionControllerData current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", safe(expectedFingerprint));
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentController", auditSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "区域控制器已被其他操作修改，请刷新后再编辑。",
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

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private static void publishRealtime(RegionControllerData controller, String action, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        if (controller == null) {
            return;
        }
        String routeTarget = "#/region-controllers/" + encode(controller.id());
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.REGION_CONTROLLER_CHANGED)
                .regionId(controller.regionId())
                .severity("INFO")
                .summary("RegionController 已更新：" + displayName(controller))
                .routeTarget(routeTarget)
                .payload("controllerId", controller.id())
                .payload("action", action)
                .payload("enterConditionGroupId", controller.enterConditionGroupId())
                .payload("exitConditionGroupId", controller.exitConditionGroupId())
                .payload("stayConditionGroupId", controller.stayConditionGroupId())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .regionId(controller.regionId())
                .severity("INFO")
                .summary("RegionController 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("controllerId", controller.id())
                .payload("targetType", "region_controller_config")
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id()));
    }

    private void releaseLockAfterWrite(String controllerId, String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress) {
        if (editLockService == null || isBlank(lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, lockId, user, session, remoteAddress);
    }

    private static boolean confirmationMatches(String text, RegionControllerData controller) {
        String value = safe(text).trim();
        return !value.isBlank() && (value.equals("我确认删除该节点") || value.equals(controller.id()) || value.equals(controller.name()));
    }

    private static WebAdminWriteTarget target(RegionControllerData controller) {
        return target(controller.id(), displayName(controller));
    }

    private static WebAdminWriteTarget target(String id, String displayName) {
        return new WebAdminWriteTarget("REGION_CONTROLLER", safe(id), isBlank(displayName) ? safe(id) : displayName);
    }

    private static String displayName(RegionControllerData controller) {
        return controller == null || isBlank(controller.name()) ? safe(controller == null ? "" : controller.id()) : controller.name();
    }

    private static String labelTrigger(RegionTriggerType triggerType) {
        if (triggerType == RegionTriggerType.ENTER) return "enter";
        if (triggerType == RegionTriggerType.EXIT) return "exit";
        return "stay";
    }

    private static String triggerKey(RegionTriggerType triggerType) {
        return labelTrigger(triggerType);
    }

    private static ConditionRuntimeTargetType actionTargetType(RegionTriggerType triggerType) {
        return switch (triggerType == null ? RegionTriggerType.STAY : triggerType) {
            case ENTER -> ConditionRuntimeTargetType.REGION_ENTER_ACTION;
            case EXIT -> ConditionRuntimeTargetType.REGION_EXIT_ACTION;
            case STAY -> ConditionRuntimeTargetType.REGION_STAY_ACTION;
        };
    }

    private static ActionOwnerType actionOwnerType(ConditionRuntimeTargetType actionTargetType) {
        return switch (actionTargetType == null ? ConditionRuntimeTargetType.REGION_STAY_ACTION : actionTargetType) {
            case REGION_ENTER_ACTION -> ActionOwnerType.REGION_ENTER;
            case REGION_EXIT_ACTION -> ActionOwnerType.REGION_EXIT;
            default -> ActionOwnerType.REGION_STAY;
        };
    }

    private static boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) return true;
            if ("false".equalsIgnoreCase(string.trim())) return false;
        }
        return fallback;
    }

    private static int parseInteger(Object value, int fallback) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return (int) doubleValue;
            }
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean containsControl(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Validation(
            List<WebAdminValidationError> errors,
            boolean enabled,
            String name,
            String regionId,
            RegionTargetFilter targetFilter,
            int stayIntervalTicks,
            String enterConditionGroupId,
            String exitConditionGroupId,
            String stayConditionGroupId
    ) {
    }

    private record ActionValidation(List<WebAdminValidationError> errors, ActionConfig action) {
    }
}
