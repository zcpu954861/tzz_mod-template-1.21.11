package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableMutationValidation;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminActionRelayActionsService {
    public static final int MAX_ACTIONS = 64;
    public static final int MAX_COMMAND_LENGTH = 512;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_SOUND_ID_LENGTH = 128;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator gateBindingValidator = new WebAdminConditionGateBindingValidator();

    public WebAdminActionRelayActionsService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> actionsFor(MinecraftServer server, WebAdminUser user, WebAdminSession session, String deviceId) {
        ActionRelayTarget relayTarget = resolveRelay(server, deviceId);
        if (relayTarget.device() == null) {
            return null;
        }
        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            Map<String, Object> data = baseData(relayTarget.device(), null, user, session);
            data.put("supported", false);
            data.put("typeSupported", false);
            data.put("actionsReadable", false);
            data.put("actionsEditable", false);
            data.put("unsupportedReason", "只有 action_relay 支持 Action 列表。");
            data.put("loadedState", "not_action_relay");
            data.put("worldAvailable", false);
            data.put("chunkLoaded", false);
            data.put("blockEntityLoaded", false);
            data.put("blockEntityType", "");
            data.put("blockId", "");
            data.put("expectedBlockId", "");
            return data;
        }
        boolean typeSupported = SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type());
        boolean actionsReadable = relayTarget.actionsReadable();
        boolean actionsEditable = relayTarget.editable();
        Map<String, Object> data = baseData(relayTarget.device(), relayTarget.relay(), user, session);
        data.put("supported", typeSupported);
        data.put("typeSupported", typeSupported);
        data.put("actionsReadable", actionsReadable);
        data.put("actionsEditable", actionsEditable);
        data.put("unsupportedReason", actionsEditable ? "" : relayTarget.unsupportedReason());
        data.put("loadedState", relayTarget.loadedState());
        data.put("worldAvailable", relayTarget.worldAvailable());
        data.put("chunkLoaded", relayTarget.chunkLoaded());
        data.put("blockEntityLoaded", relayTarget.blockEntityLoaded());
        data.put("blockEntityType", relayTarget.blockEntityType());
        data.put("blockId", relayTarget.blockId());
        data.put("expectedBlockId", relayTarget.expectedBlockId());
        data.put("dimension", relayTarget.device().dimension());
        data.put("position", Map.of("x", relayTarget.device().x(), "y", relayTarget.device().y(), "z", relayTarget.device().z()));
        return data;
    }

    public WebAdminWriteResult addAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            WebAdminActionRelayActionsUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceId = safe(deviceId);
        WebAdminWriteTarget target = target(safeDeviceId, safeDeviceId);
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS,
                target
        );

        ActionRelayTarget relayTarget = resolveRelay(server, safeDeviceId);
        if (relayTarget.device() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of("attempt", "append_action_missing"));
            return result;
        }
        target = target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device()));
        writeContext = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, target);
        if (request != null) {
            request.deviceId = relayTarget.device().id();
        }

        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 action_relay 支持追加 Action。",
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_invalid_type"));
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_origin_failed"));
            return result;
        }

        if (relayTarget.relay() == null || relayTarget.world() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：" + relayTarget.unsupportedReason(),
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_unloaded"));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                    relayTarget.device().id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_edit_lock_failed"));
                return result;
            }
        }

        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> requestedActions = request == null || request.actions == null
                ? List.of()
                : request.actions;
        if (requestedActions.size() != 1) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "action",
                    "logic_chain_action_append_single_action_required",
                    "逻辑链编辑器一次只能追加 1 条 Action。",
                    String.valueOf(requestedActions.size())
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_count_invalid"));
            return result;
        }

        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_fingerprint_conflict"));
            return result;
        }

        if (relayTarget.relay().actions().size() >= MAX_ACTIONS) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "actions",
                    "too_many",
                    "Action 列表最多支持 " + MAX_ACTIONS + " 条。",
                    String.valueOf(relayTarget.relay().actions().size())
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_too_many"));
            return result;
        }

        WebAdminActionRelayActionsUpdateRequest appendRequest = new WebAdminActionRelayActionsUpdateRequest();
        appendRequest.deviceId = relayTarget.device().id();
        appendRequest.conditionGroupId = relayTarget.relay().conditionGroupId();
        appendRequest.actions = requestedActions;
        Validation validation = validateRequest(server, appendRequest, gateBindingValidator);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), requestSummary(validation.actions(), relayTarget.relay().conditionGroupId()));
            return result;
        }
        if (validation.actions().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "action",
                    "required",
                    "Action 配置不能为空。",
                    ""
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "append_action_empty_validation"));
            return result;
        }

        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        String conditionGroupId = relayTarget.relay().conditionGroupId();
        List<ActionConfig> afterActions = new ArrayList<>(beforeActions);
        afterActions.add(validation.actions().getFirst());
        relayTarget.relay().setConditionGroupId(conditionGroupId);
        relayTarget.relay().replaceActions(afterActions);
        SignalDeviceData updated = SignalDeviceStore.updateActions(relayTarget.world(), relayTarget.pos(), relayTarget.relay());
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actionList", baseData(updated == null ? relayTarget.device() : updated, relayTarget.relay(), user, session));
        data.put("changedFields", List.of("actions"));
        data.put("actionCountBefore", beforeActions.size());
        data.put("actionCountAfter", afterActions.size());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Action Relay 动作已追加。",
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
        WebAdminAuditEvent auditEvent = audit(
                writeContext,
                result,
                currentSummary(relayTarget.device(), beforeActions, conditionGroupId),
                currentSummary(updated == null ? relayTarget.device() : updated, afterActions, conditionGroupId)
        );
        publishRealtime(updated == null ? relayTarget.device() : updated, auditEvent, user, beforeActions, afterActions, conditionGroupId);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            WebAdminActionRelayActionsUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceId = safe(deviceId);
        WebAdminWriteTarget target = target(safeDeviceId, safeDeviceId);
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS,
                target
        );

        ActionRelayTarget relayTarget = resolveRelay(server, safeDeviceId);
        if (relayTarget.device() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of());
            return result;
        }
        target = target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device()));
        writeContext = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, target);
        if (request != null) {
            request.deviceId = relayTarget.device().id();
        }

        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 action_relay 支持编辑 Action 列表。",
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "invalid_type"));
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "origin_failed"));
            return result;
        }

        if (relayTarget.relay() == null || relayTarget.world() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：" + relayTarget.unsupportedReason(),
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "unloaded"));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                    relayTarget.device().id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "edit_lock_failed"));
                return result;
            }
        }

        Validation validation = validateRequest(server, request, gateBindingValidator);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), requestSummary(validation.actions(), request == null ? "" : request.conditionGroupId));
            return result;
        }

        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }

        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        List<ActionConfig> afterActions = validation.actions();
        String beforeConditionGroupId = relayTarget.relay().conditionGroupId();
        String afterConditionGroupId = WebAdminConditionGroupStore.normalizeId(request.conditionGroupId);
        if (beforeActions.equals(afterActions) && beforeConditionGroupId.equals(afterConditionGroupId)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Action 列表变化。");
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), currentSummary(relayTarget.device(), relayTarget.relay()));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        relayTarget.relay().setConditionGroupId(afterConditionGroupId);
        relayTarget.relay().replaceActions(afterActions);
        SignalDeviceData updated = SignalDeviceStore.updateActions(relayTarget.world(), relayTarget.pos(), relayTarget.relay());
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actionList", baseData(updated == null ? relayTarget.device() : updated, relayTarget.relay(), user, session));
        data.put("changedFields", changedFields(beforeActions, afterActions, beforeConditionGroupId, afterConditionGroupId));
        data.put("actionCountBefore", beforeActions.size());
        data.put("actionCountAfter", afterActions.size());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Action Relay 动作列表已保存。",
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
        WebAdminAuditEvent auditEvent = audit(
                writeContext,
                result,
                currentSummary(relayTarget.device(), beforeActions, beforeConditionGroupId),
                currentSummary(updated == null ? relayTarget.device() : updated, afterActions, afterConditionGroupId)
        );
        publishRealtime(updated == null ? relayTarget.device() : updated, auditEvent, user, beforeActions, afterActions, afterConditionGroupId);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult updateAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            int actionIndex,
            WebAdminActionRelayActionsUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceId = safe(deviceId);
        WebAdminWriteTarget target = target(safeDeviceId, safeDeviceId);
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS,
                target
        );

        ActionRelayTarget relayTarget = resolveRelay(server, safeDeviceId);
        if (relayTarget.device() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of("attempt", "same_index_update_missing"));
            return result;
        }
        target = target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device()));
        writeContext = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, target);
        if (request != null) {
            request.deviceId = relayTarget.device().id();
        }

        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 action_relay 支持 same-index Action 编辑。",
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_invalid_type"));
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_origin_failed"));
            return result;
        }

        if (relayTarget.relay() == null || relayTarget.world() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：" + relayTarget.unsupportedReason(),
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_unloaded"));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                    relayTarget.device().id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_edit_lock_failed"));
                return result;
            }
        }

        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        if (actionIndex < 0 || actionIndex >= beforeActions.size()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "actionIndex",
                    "out_of_range",
                    "Action index 不存在，same-index 编辑不能新增、删除或重排旧 Action。",
                    String.valueOf(actionIndex)
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_index_invalid"));
            return result;
        }
        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> requestedActions = request == null || request.actions == null
                ? List.of()
                : request.actions;
        if (requestedActions.size() != 1) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "actions",
                    "same_index_single_action_required",
                    "same-index 编辑一次只能提交 1 条替换 Action。",
                    String.valueOf(requestedActions.size())
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_count_invalid"));
            return result;
        }
        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_expected_fingerprint_missing"));
            return result;
        }
        String beforeConditionGroupId = relayTarget.relay().conditionGroupId();
        if (!fingerprintMatches(relayTarget.device(), beforeActions, beforeConditionGroupId, request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, relayTarget.device(), beforeActions, beforeConditionGroupId, request.expectedFingerprint);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "same_index_update_fingerprint_conflict"));
            return result;
        }

        WebAdminActionRelayActionsUpdateRequest validationRequest = new WebAdminActionRelayActionsUpdateRequest();
        validationRequest.deviceId = relayTarget.device().id();
        validationRequest.conditionGroupId = beforeConditionGroupId;
        validationRequest.actions = List.of(requestedActions.getFirst());
        Validation validation = validateRequest(server, validationRequest, gateBindingValidator);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), requestSummary(validation.actions(), beforeConditionGroupId));
            return result;
        }

        List<ActionConfig> afterActions = new ArrayList<>(beforeActions);
        afterActions.set(actionIndex, validation.actions().getFirst());
        if (afterActions.size() != beforeActions.size()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "actions",
                    "same_index_count_changed",
                    "same-index 编辑不得改变 Action 数量。",
                    beforeActions.size() + " -> " + afterActions.size()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), requestSummary(afterActions, beforeConditionGroupId));
            return result;
        }
        if (beforeActions.equals(afterActions)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Action 变化。");
            audit(writeContext, result, currentSummary(relayTarget.device(), beforeActions, beforeConditionGroupId), currentSummary(relayTarget.device(), afterActions, beforeConditionGroupId));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        relayTarget.relay().setConditionGroupId(beforeConditionGroupId);
        relayTarget.relay().replaceActions(afterActions);
        SignalDeviceData updated = SignalDeviceStore.updateActions(relayTarget.world(), relayTarget.pos(), relayTarget.relay());
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actionList", baseData(updated == null ? relayTarget.device() : updated, relayTarget.relay(), user, session));
        data.put("changedFields", List.of("actions[" + actionIndex + "]"));
        data.put("actionIndex", actionIndex);
        data.put("actionCountBefore", beforeActions.size());
        data.put("actionCountAfter", afterActions.size());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Action Relay 动作已按原 index 更新。",
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
        WebAdminAuditEvent auditEvent = audit(
                writeContext,
                result,
                currentSummary(relayTarget.device(), beforeActions, beforeConditionGroupId),
                currentSummary(updated == null ? relayTarget.device() : updated, afterActions, beforeConditionGroupId)
        );
        publishRealtime(updated == null ? relayTarget.device() : updated, auditEvent, user, beforeActions, afterActions, beforeConditionGroupId);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public WebAdminWriteResult deleteAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            int actionIndex,
            boolean confirmed,
            String expectedFingerprint,
            String lockId,
            String csrfToken,
            boolean sameOrigin
    ) {
        if (!confirmed) {
            return WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION,
                    target(safe(deviceId), safe(deviceId)),
                    "删除 ActionRelay 单条 action 需要二次确认。"
            );
        }
        ActionRelayTarget relayTarget = resolveRelay(server, safe(deviceId));
        if (relayTarget.device() == null || relayTarget.relay() == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target(safe(deviceId), safe(deviceId)), "目标 ActionRelay 不存在、未加载或引用不唯一。");
        }
        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        if (actionIndex < 0 || actionIndex >= beforeActions.size()) {
            return WebAdminWriteResult.validationFailed(target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device())), List.of(new WebAdminValidationError(
                    "actionIndex",
                    "out_of_range",
                    "要删除的 ActionRelay action 已不存在，请刷新后重试。",
                    String.valueOf(actionIndex)
            )));
        }
        List<ActionConfig> afterActions = new ArrayList<>(beforeActions);
        afterActions.remove(actionIndex);
        WebAdminActionRelayActionsUpdateRequest request = requestFromActions(relayTarget.device().id(), relayTarget.relay().conditionGroupId(), afterActions, expectedFingerprint, lockId);
        return update(server, user, session, remoteAddress, relayTarget.device().id(), request, csrfToken, sameOrigin);
    }

    public WebAdminWriteResult reorderAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            int fromIndex,
            int toIndex,
            boolean confirmed,
            String expectedFingerprint,
            String lockId,
            String csrfToken,
            boolean sameOrigin
    ) {
        if (!confirmed) {
            return WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION,
                    target(safe(deviceId), safe(deviceId)),
                    "重排 ActionRelay action 需要二次确认。"
            );
        }
        ActionRelayTarget relayTarget = resolveRelay(server, safe(deviceId));
        if (relayTarget.device() == null || relayTarget.relay() == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target(safe(deviceId), safe(deviceId)), "目标 ActionRelay 不存在、未加载或引用不唯一。");
        }
        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        if (fromIndex < 0 || fromIndex >= beforeActions.size() || toIndex < 0 || toIndex >= beforeActions.size()) {
            return WebAdminWriteResult.validationFailed(target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device())), List.of(new WebAdminValidationError(
                    "fromIndex",
                    "out_of_range",
                    "ActionRelay action 重排 index 已不存在，请刷新后重试。",
                    fromIndex + " -> " + toIndex
            )));
        }
        if (fromIndex == toIndex) {
            return WebAdminWriteResult.noChange(target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device())), "ActionRelay action 顺序没有变化。");
        }
        List<ActionConfig> afterActions = new ArrayList<>(beforeActions);
        ActionConfig moving = afterActions.remove(fromIndex);
        afterActions.add(toIndex, moving);
        WebAdminActionRelayActionsUpdateRequest request = requestFromActions(relayTarget.device().id(), relayTarget.relay().conditionGroupId(), afterActions, expectedFingerprint, lockId);
        return update(server, user, session, remoteAddress, relayTarget.device().id(), request, csrfToken, sameOrigin);
    }

    public static boolean fingerprintMatches(SignalDeviceData device, List<ActionConfig> actions, String conditionGroupId, String expectedFingerprint) {
        return !isBlank(expectedFingerprint) && fingerprintFor(device, actions, conditionGroupId).equals(expectedFingerprint);
    }

    public static String fingerprintFor(SignalDeviceData rawDevice, List<ActionConfig> rawActions) {
        return fingerprintFor(rawDevice, rawActions, "");
    }

    public static String fingerprintFor(SignalDeviceData rawDevice, List<ActionConfig> rawActions, String conditionGroupId) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        String input = "action_relay_actions|" + device.id() + "|" + device.type() + "|" + device.channel()
                + "|conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(conditionGroupId)
                + "|" + actionFingerprintList(rawActions);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateActionEntries(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        return validateRequest(null, requestFor(entries), new WebAdminConditionGateBindingValidator()).errors();
    }

    private static WebAdminActionRelayActionsUpdateRequest requestFor(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
        request.actions = entries == null ? List.of() : entries;
        return request;
    }

    private static WebAdminActionRelayActionsUpdateRequest requestFromActions(
            String deviceId,
            String conditionGroupId,
            List<ActionConfig> actions,
            String expectedFingerprint,
            String lockId
    ) {
        WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
        request.deviceId = safe(deviceId);
        request.conditionGroupId = WebAdminConditionGroupStore.normalizeId(conditionGroupId);
        request.actions = normalizeActions(actions).stream().map(WebAdminActionRelayActionsService::entryFromAction).toList();
        request.expectedFingerprint = safe(expectedFingerprint);
        request.lockId = safe(lockId);
        return request;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry entryFromAction(ActionConfig action) {
        ActionConfig safeAction = action == null ? new ActionConfig(ActionType.COMMAND, "", true, false, 0, false) : action.normalized();
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = safeAction.type().id();
        entry.value = safeAction.value();
        entry.enabled = safeAction.enabled();
        entry.requiresOp = safeAction.requiresOp();
        entry.cooldownTicks = safeAction.cooldownTicks();
        entry.notifyOps = safeAction.notifyOps();
        entry.conditionGroupId = safeAction.conditionGroupId();
        entry.stateOperation = safeAction.stateOperation();
        entry.stateScope = safeAction.stateScope();
        entry.stateTargetMode = safeAction.stateTargetMode();
        entry.stateTargetId = safeAction.stateTargetId();
        entry.stateKey = safeAction.stateKey();
        entry.stateValueType = safeAction.stateValueType();
        entry.stateValue = safeAction.stateValue();
        entry.stateDelta = safeAction.stateDelta();
        entry.stateCreateIfMissing = safeAction.stateCreateIfMissing();
        entry.stateInitialValue = safeAction.stateInitialValue();
        entry.timerId = safeAction.timerId();
        entry.timerTargetMode = safeAction.timerTargetMode();
        entry.timerTargetId = safeAction.timerTargetId();
        entry.timerStartPolicyOverride = safeAction.timerStartPolicyOverride();
        entry.timerDurationOverrideTicks = safeAction.timerDurationOverrideTicks();
        entry.timerMissingBehavior = safeAction.timerMissingBehavior();
        return entry;
    }

    private Map<String, Object> baseData(
            SignalDeviceData device,
            ActionRelayBlockEntity relay,
            WebAdminUser user,
            WebAdminSession session
    ) {
        List<ActionConfig> actions = normalizeActions(relay == null ? List.of() : relay.actions());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("deviceType", device.type());
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("channel", device.channel());
        data.put("actionCount", relay == null ? device.actionCount() : actions.size());
        data.put("snapshotActionCount", device.actionCount());
        data.put("actions", actionDtos(actions, device.id()));
        data.put("allowedActionTypes", List.of("command", "signal", "message", "sound", "state_variable", "timer_start", "timer_cancel"));
        data.put("conditionGroupId", relay == null ? "" : relay.conditionGroupId());
        data.put("conditionGateTargetType", ConditionRuntimeTargetType.ACTION_RELAY.id());
        data.put("conditionGateTargetId", device.id());
        data.put("recentConditionGate", WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.ACTION_RELAY, device.id()));
        data.put("expectedFingerprint", fingerprintFor(device, actions, relay == null ? "" : relay.conditionGroupId()));
        WebAdminEditLockStatusDto lockStatus = editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                device.id(),
                user,
                session
        );
        data.put("lockStatus", lockStatus);
        data.put("noRawJson", true);
        data.put("physicalDeviceDeleteAllowed", false);
        data.put("notes", List.of(
                "Action 列表属于 action_relay BlockEntity 配置，不会创建或删除真实方块。",
                "未配置条件组 = 保持旧继电器逻辑，不拦截；配置后仅作为整条 Action 列表外层 gate。",
                "单条 Action 条件组为空 = 此 action 不单独判断，保持旧执行逻辑；配置后仅跳过当前 action 并继续后续 action。",
                "状态变量动作通过结构化字段写入 GLOBAL / PLAYER StateVariable，不提供 raw JSON、脚本、表达式或 NBT path。",
                "command action 是地图玩法控制能力；WebAdmin 只硬阻断 ban/kick/op/stop/whitelist 等服务器管理高风险命令。",
                "sound action 当前底层只存储 sound id；per-action cooldown/requiresOp 字段会保留，但执行语义以现有 ActionEngine 为准。"
        ));
        return data;
    }

    private static List<Map<String, Object>> actionDtos(List<ActionConfig> actions, String deviceId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index);
            String actionTargetId = ConditionActionGateService.actionTargetId("relay", deviceId, index);
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
            putStateActionFields(entry, action);
            putTimerActionFields(entry, action);
            entry.put("actionConditionGateTargetType", ConditionRuntimeTargetType.ACTION_RELAY_ACTION.id());
            entry.put("actionConditionGateTargetId", actionTargetId);
            entry.put("recentActionConditionGate", WebAdminConditionGateHistoryService.recentStatus(
                    ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                    actionTargetId
            ));
            entry.put("summary", actionSummary(action));
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static Validation validateRequest(
            MinecraftServer server,
            WebAdminActionRelayActionsUpdateRequest request,
            WebAdminConditionGateBindingValidator gateBindingValidator
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        WebAdminConditionGateBindingValidator validator = gateBindingValidator == null
                ? new WebAdminConditionGateBindingValidator()
                : gateBindingValidator;
        if (server != null) {
            validator.validate(
                    server,
                    errors,
                    "conditionGroupId",
                    request == null ? "" : request.conditionGroupId,
                    ConditionRuntimeTargetType.ACTION_RELAY
            );
        }
        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries = request == null || request.actions == null
                ? List.of()
                : request.actions;
        if (entries.size() > MAX_ACTIONS) {
            errors.add(new WebAdminValidationError("actions", "too_many", "Action 列表最多支持 " + MAX_ACTIONS + " 条。", String.valueOf(entries.size())));
        }
        List<ActionConfig> actions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = entries.get(index);
            String prefix = "actions[" + index + "]";
            if (entry == null) {
                errors.add(new WebAdminValidationError(prefix, "required", "Action 配置不能为空。", ""));
                continue;
            }
            ActionType type = parseType(entry.type);
            if (type == null) {
                errors.add(new WebAdminValidationError(prefix + ".type", "invalid_type", "Action 类型必须是 command、signal、message、sound、state_variable、timer_start 或 timer_cancel。", safe(entry.type)));
                continue;
            }
            Boolean enabled = parseBoolean(entry.enabled);
            Boolean requiresOp = parseBoolean(entry.requiresOp);
            Boolean notifyOps = parseBoolean(entry.notifyOps);
            Integer cooldownTicks = parseInteger(entry.cooldownTicks);
            if (enabled == null) {
                errors.add(new WebAdminValidationError(prefix + ".enabled", "invalid_boolean", "启用状态必须是 boolean。", String.valueOf(entry.enabled)));
                enabled = Boolean.TRUE;
            }
            if (requiresOp == null) {
                errors.add(new WebAdminValidationError(prefix + ".requiresOp", "invalid_boolean", "requiresOp 必须是 boolean。", String.valueOf(entry.requiresOp)));
                requiresOp = Boolean.FALSE;
            }
            if (notifyOps == null) {
                errors.add(new WebAdminValidationError(prefix + ".notifyOps", "invalid_boolean", "notifyOps 必须是 boolean。", String.valueOf(entry.notifyOps)));
                notifyOps = Boolean.FALSE;
            }
            if (cooldownTicks == null || cooldownTicks < 0 || cooldownTicks > 72000) {
                errors.add(new WebAdminValidationError(prefix + ".cooldownTicks", "out_of_range", "Action 冷却字段必须是 0～72000 的整数。", String.valueOf(entry.cooldownTicks)));
                cooldownTicks = 0;
            }
            String value = normalizeValue(type, entry.value);
            validateValue(server, errors, prefix + ".value", type, value, entry);
            String actionConditionGroupId = WebAdminConditionGroupStore.normalizeId(entry.conditionGroupId);
            if (server != null) {
                validator.validate(
                        server,
                        errors,
                        prefix + ".conditionGroupId",
                        actionConditionGroupId,
                        ConditionRuntimeTargetType.ACTION_RELAY_ACTION
                );
            }
            actions.add(actionFromEntry(entry, type, value, enabled, requiresOp, cooldownTicks, notifyOps, actionConditionGroupId));
        }
        return new Validation(List.copyOf(errors), errors.isEmpty() ? normalizeActions(actions) : List.copyOf(actions));
    }

    private static void validateValue(
            MinecraftServer server,
            List<WebAdminValidationError> errors,
            String field,
            ActionType type,
            String value,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry
    ) {
        if (type == ActionType.STATE_VARIABLE) {
            validateStateAction(errors, field.substring(0, Math.max(0, field.length() - ".value".length())), entry);
            return;
        }
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            validateTimerAction(errors, field.substring(0, Math.max(0, field.length() - ".value".length())), entry, type);
            return;
        }
        if (value.isBlank()) {
            errors.add(new WebAdminValidationError(field, "empty", "Action 内容不能为空。", value));
            return;
        }
        if (containsControl(value)) {
            errors.add(new WebAdminValidationError(field, "control_character", "Action 内容不能包含控制字符。", value));
            return;
        }
        switch (type) {
            case COMMAND -> {
                if (value.length() > MAX_COMMAND_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "命令长度不能超过 " + MAX_COMMAND_LENGTH + " 个字符。", value));
                } else if (isBlockedServerManagementCommand(value)) {
                    errors.add(new WebAdminValidationError(field, "server_management_command_forbidden", "该命令属于服务器管理高风险命令，不允许通过 WebAdmin action_relay 保存。", value));
                }
            }
            case SIGNAL -> {
                if (value.length() > WebAdminDeviceBasicConfigService.MAX_CHANNEL_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "频道长度不能超过 128 个字符。", value));
                } else if (!SignalChannel.isValid(value)) {
                    errors.add(new WebAdminValidationError(field, "invalid_channel", "Signal action 的频道只能包含小写字母、数字、下划线、点、冒号和连字符。", value));
                }
            }
            case MESSAGE -> {
                if (value.length() > MAX_MESSAGE_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "消息长度不能超过 " + MAX_MESSAGE_LENGTH + " 个字符。", value));
                }
            }
            case SOUND -> {
                if (value.length() > MAX_SOUND_ID_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "音效 ID 长度不能超过 " + MAX_SOUND_ID_LENGTH + " 个字符。", value));
                } else if (!value.matches("[a-z0-9_.:-]+(/[a-z0-9_.:-]+)*")) {
                    errors.add(new WebAdminValidationError(field, "invalid_sound_id", "音效 ID 应使用 minecraft:entity.example 这类小写资源 ID。", value));
                }
            }
            case STATE_VARIABLE -> {
            }
            case TIMER_START, TIMER_CANCEL -> {
            }
        }
    }

    private static boolean isBlockedServerManagementCommand(String command) {
        List<String> tokens = commandTokens(command);
        if (tokens.isEmpty()) {
            return false;
        }
        if (isServerManagementRoot(commandRoot(tokens.getFirst()))) {
            return true;
        }
        if ("execute".equals(commandRoot(tokens.getFirst()))) {
            for (int index = 0; index < tokens.size() - 1; index++) {
                if ("run".equals(commandRoot(tokens.get(index)))
                        && isServerManagementRoot(commandRoot(tokens.get(index + 1)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isServerManagementRoot(String root) {
        return "ban".equals(root)
                || "ban-ip".equals(root)
                || "kick".equals(root)
                || "op".equals(root)
                || "deop".equals(root)
                || "reload".equals(root)
                || "save-off".equals(root)
                || "save-on".equals(root)
                || "stop".equals(root)
                || "whitelist".equals(root)
                || "pardon".equals(root)
                || "pardon-ip".equals(root);
    }

    private static String commandRoot(String token) {
        String value = safe(token).trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int namespace = value.indexOf(':');
        return namespace >= 0 && namespace + 1 < value.length() ? value.substring(namespace + 1) : value;
    }

    private static List<String> commandTokens(String command) {
        String normalized = ActionConfig.normalizeCommand(command);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < normalized.length(); index++) {
            char c = normalized.charAt(index);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                current.append(c);
                quoted = !quoted;
                continue;
            }
            if (!quoted && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private static ActionType parseType(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        for (ActionType type : ActionType.values()) {
            if (type.id().equals(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    private static String normalizeValue(ActionType type, String rawValue) {
        String value = safe(rawValue).trim();
        if (type == ActionType.COMMAND) {
            return ActionConfig.normalizeCommand(value);
        }
        if (type == ActionType.SIGNAL) {
            return SignalChannel.normalize(value);
        }
        if (type == ActionType.STATE_VARIABLE) {
            return "";
        }
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            return "";
        }
        return value;
    }

    public static ActionConfig actionFromEntry(WebAdminActionRelayActionsUpdateRequest.ActionEntry entry) {
        ActionType type = parseType(entry == null ? "" : entry.type);
        if (type == null) {
            type = ActionType.COMMAND;
        }
        String value = normalizeValue(type, entry == null ? "" : entry.value);
        return actionFromEntry(
                entry,
                type,
                value,
                parseBoolean(entry == null ? Boolean.TRUE : entry.enabled) != null ? parseBoolean(entry == null ? Boolean.TRUE : entry.enabled) : Boolean.TRUE,
                parseBoolean(entry == null ? Boolean.FALSE : entry.requiresOp) != null ? parseBoolean(entry == null ? Boolean.FALSE : entry.requiresOp) : Boolean.FALSE,
                parseInteger(entry == null ? 0 : entry.cooldownTicks) == null ? 0 : Math.max(0, parseInteger(entry == null ? 0 : entry.cooldownTicks)),
                parseBoolean(entry == null ? Boolean.FALSE : entry.notifyOps) != null ? parseBoolean(entry == null ? Boolean.FALSE : entry.notifyOps) : Boolean.FALSE,
                WebAdminConditionGroupStore.normalizeId(entry == null ? "" : entry.conditionGroupId)
        );
    }

    private static ActionConfig actionFromEntry(
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry,
            ActionType type,
            String value,
            boolean enabled,
            boolean requiresOp,
            int cooldownTicks,
            boolean notifyOps,
            String conditionGroupId
    ) {
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry safeEntry = entry == null
                    ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry()
                    : entry;
            return new ActionConfig(
                    type,
                    "",
                    enabled,
                    false,
                    cooldownTicks,
                    false,
                    conditionGroupId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    0L,
                    false,
                    "",
                    safe(safeEntry.timerId),
                    safe(safeEntry.timerTargetMode),
                    safe(safeEntry.timerTargetId),
                    safe(safeEntry.timerStartPolicyOverride),
                    parseLong(safeEntry.timerDurationOverrideTicks, 0L),
                    safe(safeEntry.timerMissingBehavior)
            );
        }
        if (type != ActionType.STATE_VARIABLE) {
            return new ActionConfig(type, value, enabled, requiresOp, cooldownTicks, notifyOps, conditionGroupId);
        }
        WebAdminActionRelayActionsUpdateRequest.ActionEntry safeEntry = entry == null
                ? new WebAdminActionRelayActionsUpdateRequest.ActionEntry()
                : entry;
        return new ActionConfig(
                ActionType.STATE_VARIABLE,
                "",
                enabled,
                false,
                cooldownTicks,
                false,
                conditionGroupId,
                safe(safeEntry.stateOperation),
                safe(safeEntry.stateScope),
                safe(safeEntry.stateTargetMode),
                safe(safeEntry.stateTargetId),
                safe(safeEntry.stateKey),
                safe(safeEntry.stateValueType),
                safe(safeEntry.stateValue),
                parseLong(safeEntry.stateDelta, 0L),
                Boolean.TRUE.equals(parseBoolean(safeEntry.stateCreateIfMissing)),
                safe(safeEntry.stateInitialValue)
        );
    }

    private static void validateStateAction(
            List<WebAdminValidationError> errors,
            String prefix,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry
    ) {
        Boolean createIfMissing = parseBoolean(entry == null ? Boolean.FALSE : entry.stateCreateIfMissing);
        if (createIfMissing == null) {
            errors.add(new WebAdminValidationError(
                    prefix + ".stateCreateIfMissing",
                    "invalid_boolean",
                    "变量不存在时自动创建字段必须是 boolean。",
                    String.valueOf(entry == null ? "" : entry.stateCreateIfMissing)
            ));
        }
        ActionConfig config = actionFromEntry(entry);
        StateVariableMutationRequest request = config.stateMutationRequest("");
        for (StateVariableMutationValidation.Issue issue : StateVariableMutationValidation.validate(request)) {
            errors.add(new WebAdminValidationError(
                    prefix + "." + issue.field(),
                    issue.code(),
                    issue.message(),
                    issue.rejectedValue()
            ));
        }
    }

    public static void validateTimerAction(
            List<WebAdminValidationError> errors,
            String prefix,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry,
            ActionType type
    ) {
        String timerId = TimerStore.normalizeId(entry == null ? "" : entry.timerId);
        if (timerId.isBlank()) {
            errors.add(new WebAdminValidationError(prefix + ".timerId", "timer_id_required", "Timer 动作必须选择 timerId。", entry == null ? "" : entry.timerId));
        }
        String targetMode = safe(entry == null ? "" : entry.timerTargetMode);
        TimerTargetMode parsedTargetMode = TimerTargetMode.parse(targetMode);
        if (!targetMode.isBlank() && parsedTargetMode == null) {
            errors.add(new WebAdminValidationError(prefix + ".timerTargetMode", "timer_target_mode_invalid", "Timer 目标模式必须是 global、context_player 或 explicit_target。", targetMode));
        }
        if (parsedTargetMode == TimerTargetMode.EXPLICIT_TARGET && safe(entry == null ? "" : entry.timerTargetId).isBlank()) {
            errors.add(new WebAdminValidationError(prefix + ".timerTargetId", "timer_target_id_required", "Timer 指定玩家目标不能为空。", ""));
        }
        String policy = safe(entry == null ? "" : entry.timerStartPolicyOverride);
        if (type == ActionType.TIMER_START && !policy.isBlank() && TimerStartPolicy.parse(policy) == null) {
            errors.add(new WebAdminValidationError(prefix + ".timerStartPolicyOverride", "timer_start_policy_invalid", "Timer 启动策略覆盖必须是 RESTART、IGNORE_IF_RUNNING 或 FAIL_IF_RUNNING。", policy));
        }
        Long durationOverride = parseLongObject(entry == null ? 0 : entry.timerDurationOverrideTicks);
        if (durationOverride == null || durationOverride < 0 || durationOverride > TimerValidator.MAX_DURATION_TICKS) {
            errors.add(new WebAdminValidationError(prefix + ".timerDurationOverrideTicks", "timer_duration_override_invalid", "Timer 时长覆盖必须是 0 到 1728000 的整数。", String.valueOf(entry == null ? "" : entry.timerDurationOverrideTicks)));
        }
        String missingBehavior = safe(entry == null ? "" : entry.timerMissingBehavior).toLowerCase(java.util.Locale.ROOT);
        if (type == ActionType.TIMER_CANCEL
                && !missingBehavior.isBlank()
                && !"noop_success".equals(missingBehavior)
                && !"fail".equals(missingBehavior)
                && !"fail_if_missing".equals(missingBehavior)) {
            errors.add(new WebAdminValidationError(prefix + ".timerMissingBehavior", "timer_missing_behavior_invalid", "Timer 缺失处理策略必须是 noop_success 或 fail。", missingBehavior));
        }
    }

    public static void putStateActionFields(Map<String, Object> entry, ActionConfig action) {
        if (entry == null || action == null) {
            return;
        }
        entry.put("stateOperation", action.stateOperation());
        entry.put("stateScope", action.stateScope());
        entry.put("stateTargetMode", action.stateTargetMode());
        entry.put("stateTargetId", action.stateTargetId());
        entry.put("stateKey", action.stateKey());
        entry.put("stateValueType", action.stateValueType());
        entry.put("stateValue", action.stateValue());
        entry.put("stateDelta", action.stateDelta());
        entry.put("stateCreateIfMissing", action.stateCreateIfMissing());
        entry.put("stateInitialValue", action.stateInitialValue());
        entry.put("stateActionSummary", action.stateActionSummary());
    }

    public static void putTimerActionFields(Map<String, Object> entry, ActionConfig action) {
        if (entry == null || action == null) {
            return;
        }
        entry.put("timerId", action.timerId());
        entry.put("timerTargetMode", action.timerTargetMode());
        entry.put("timerTargetId", action.timerTargetId());
        entry.put("timerStartPolicyOverride", action.timerStartPolicyOverride());
        entry.put("timerDurationOverrideTicks", action.timerDurationOverrideTicks());
        entry.put("timerMissingBehavior", action.timerMissingBehavior());
        entry.put("timerActionSummary", action.timerActionSummary());
    }

    public static String stateActionSummary(ActionConfig action) {
        return action == null || action.type() != ActionType.STATE_VARIABLE ? "" : action.stateActionSummary();
    }

    private static List<ActionConfig> normalizeActions(List<ActionConfig> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<ActionConfig> normalized = new ArrayList<>();
        for (ActionConfig action : actions) {
            if (action == null) {
                continue;
            }
            normalized.add(action.normalized());
        }
        return List.copyOf(normalized);
    }

    private ActionRelayTarget resolveRelay(MinecraftServer server, String deviceId) {
        if (server == null || isBlank(deviceId)) {
            return ActionRelayTarget.missing();
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        if (!resolved.foundUnique()) {
            return ActionRelayTarget.missing();
        }
        SignalDeviceData device = resolved.device().normalized();
        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
            return ActionRelayTarget.unsupportedType(device);
        }
        ServerWorld world = findWorld(server, device.dimension());
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean worldAvailable = world != null;
        boolean chunkLoaded = false;
        BlockEntity blockEntity = null;
        String blockId = "";
        String expectedBlockId = Registries.BLOCK.getId(ModBlocks.ACTION_RELAY).toString();
        if (world != null) {
            chunkLoaded = world.isChunkLoaded(pos);
            if (chunkLoaded) {
                blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
                blockEntity = world.getBlockEntity(pos);
            }
        }
        ActionRelayBlockEntity relay = blockEntity instanceof ActionRelayBlockEntity actionRelay ? actionRelay : null;
        return new ActionRelayTarget(
                device,
                relay,
                world,
                pos,
                worldAvailable,
                chunkLoaded,
                blockEntity != null,
                blockEntity == null ? "" : blockEntity.getClass().getSimpleName(),
                blockId,
                expectedBlockId
        );
    }

    private WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(context),
                result,
                beforeSummary,
                afterSummary
        );
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private void publishRealtime(
            SignalDeviceData device,
            WebAdminAuditEvent auditEvent,
            WebAdminUser user,
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions,
            String conditionGroupId
    ) {
        String deviceId = device.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        List<String> affectedChannels = affectedSignalChannels(device, beforeActions, afterActions);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("Action Relay 动作列表已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent actionConfigEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .actionId("ACTION_RELAY:" + deviceId)
                .severity("INFO")
                .summary("Action Relay 动作列表已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("actionCount", device.actionCount())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent actionEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .actionId("ACTION_RELAY:" + deviceId)
                .severity("INFO")
                .summary("Action Relay 动作已变化。")
                .routeTarget("#/actions")
                .payload("targetType", "action_relay_actions")
                .payload("deviceId", deviceId)
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels));
        WebAdminRealtimeEvent deviceConfigEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("Action Relay 动作列表已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("actionCount", device.actionCount())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("actionConfigEventId", actionConfigEvent == null ? "" : actionConfigEvent.id())
                .payload("actionEventId", actionEvent == null ? "" : actionEvent.id())
                .payload("deviceConfigEventId", deviceConfigEvent == null ? "" : deviceConfigEvent.id()));
    }

    private static List<String> affectedSignalChannels(
            SignalDeviceData device,
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions
    ) {
        Set<String> channels = new LinkedHashSet<>();
        if (device != null && !isBlank(device.channel())) {
            channels.add(SignalChannel.normalize(device.channel()));
        }
        collectSignalActionChannels(channels, beforeActions);
        collectSignalActionChannels(channels, afterActions);
        channels.removeIf(WebAdminActionRelayActionsService::isBlank);
        return List.copyOf(channels);
    }

    private static void collectSignalActionChannels(Set<String> channels, List<ActionConfig> actions) {
        if (channels == null || actions == null) {
            return;
        }
        for (ActionConfig action : actions) {
            if (action != null && action.type() == ActionType.SIGNAL && !isBlank(action.value())) {
                channels.add(SignalChannel.normalize(action.value()));
            }
        }
    }

    private void releaseLockAfterWrite(
            WebAdminActionRelayActionsUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || isBlank(request.lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                request.deviceId,
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(
            WebAdminWriteTarget target,
            SignalDeviceData device,
            List<ActionConfig> actions,
            String conditionGroupId,
            String expectedFingerprint
    ) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(device, actions, conditionGroupId));
        conflict.put("currentActionList", currentSummary(device, actions, conditionGroupId));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "Action Relay 动作列表已被其他操作修改，请刷新后再编辑。",
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

    private static Map<String, Object> currentSummary(SignalDeviceData device, ActionRelayBlockEntity relay) {
        return currentSummary(device, relay == null ? List.of() : relay.actions(), relay == null ? "" : relay.conditionGroupId());
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device, List<ActionConfig> actions) {
        return currentSummary(device, actions, "");
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device, List<ActionConfig> actions, String conditionGroupId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        List<ActionConfig> normalizedActions = normalizeActions(actions);
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("channel", device.channel());
        summary.put("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId));
        summary.put("actionCount", normalizedActions.size());
        summary.put("actions", auditActionSummaryList(normalizedActions));
        summary.put("expectedFingerprint", fingerprintFor(device, normalizedActions, conditionGroupId));
        return summary;
    }

    private static Map<String, Object> requestSummary(List<ActionConfig> actions) {
        return requestSummary(actions, "");
    }

    private static Map<String, Object> requestSummary(List<ActionConfig> actions, String conditionGroupId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId));
        summary.put("actionCount", actions == null ? 0 : actions.size());
        summary.put("actions", auditActionSummaryList(actions));
        return summary;
    }

    private static List<String> changedFields(
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions,
            String beforeConditionGroupId,
            String afterConditionGroupId
    ) {
        List<String> fields = new ArrayList<>();
        if (!normalizeActions(beforeActions).equals(normalizeActions(afterActions))) {
            fields.add("actions");
        }
        if (!WebAdminConditionGroupStore.normalizeId(beforeConditionGroupId).equals(WebAdminConditionGroupStore.normalizeId(afterConditionGroupId))) {
            fields.add("conditionGroupId");
        }
        return List.copyOf(fields);
    }

    private static List<String> actionSummaryList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream().map(WebAdminActionRelayActionsService::actionSummary).toList();
    }

    private static List<String> actionFingerprintList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream()
                .map(action -> action.type().id()
                        + "|value=" + safe(action.value())
                        + "|enabled=" + action.enabled()
                        + "|requiresOp=" + action.requiresOp()
                        + "|cooldownTicks=" + action.cooldownTicks()
                        + "|notifyOps=" + action.notifyOps()
                        + "|conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId())
                        + stateFingerprintSuffix(action)
                        + timerFingerprintSuffix(action))
                .toList();
    }

    private static String stateFingerprintSuffix(ActionConfig action) {
        return action == null || action.type() != ActionType.STATE_VARIABLE ? "" : "|" + action.stateFingerprint();
    }

    private static String timerFingerprintSuffix(ActionConfig action) {
        return action == null || (action.type() != ActionType.TIMER_START && action.type() != ActionType.TIMER_CANCEL) ? "" : "|" + action.timerFingerprint();
    }

    private static List<String> auditActionSummaryList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream().map(WebAdminActionRelayActionsService::auditActionSummary).toList();
    }

    private static String actionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String prefix = action.enabled() ? "" : "[disabled] ";
        if (action.type() == ActionType.STATE_VARIABLE) {
            return prefix + action.type().id() + ": " + action.stateActionSummary();
        }
        if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            return prefix + action.type().id() + ": " + action.timerActionSummary();
        }
        return prefix + action.type().id() + ": " + safe(action.value());
    }

    private static String auditActionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String prefix = action.enabled() ? "" : "[disabled] ";
        if (action.type() == ActionType.STATE_VARIABLE) {
            return prefix + action.type().id()
                    + ": " + action.stateActionSummary()
                    + " " + action.stateAuditFingerprint()
                    + " conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
        }
        if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            return prefix + action.type().id()
                    + ": " + action.timerActionSummary()
                    + " " + action.timerAuditFingerprint()
                    + " conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
        }
        String value = safe(action.value());
        if (action.type() == ActionType.COMMAND) {
            value = "<command redacted length=" + value.length() + ">";
        } else if (action.type() == ActionType.MESSAGE && value.length() > 96) {
            value = value.substring(0, 96) + "...";
        }
        return prefix + action.type().id()
                + ": " + value
                + " requiresOp=" + action.requiresOp()
                + " cooldownTicks=" + action.cooldownTicks()
                + " notifyOps=" + action.notifyOps()
                + " conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(string.trim())) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static Integer parseInteger(Object value) {
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
                return null;
            }
        }
        return null;
    }

    private static long parseLong(Object value, long fallback) {
        Long parsed = parseLongObject(value);
        return parsed == null ? fallback : parsed;
    }

    private static Long parseLongObject(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE) {
                return number.longValue();
            }
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private static ServerWorld findWorld(MinecraftServer server, String dimension) {
        if (server == null || isBlank(dimension)) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimension)) {
                return world;
            }
        }
        return null;
    }

    private static WebAdminWriteTarget target(String deviceId, String displayName) {
        return new WebAdminWriteTarget("ACTION_RELAY_ACTIONS", safe(deviceId), isBlank(displayName) ? safe(deviceId) : displayName);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private record ActionRelayTarget(
            SignalDeviceData device,
            ActionRelayBlockEntity relay,
            ServerWorld world,
            BlockPos pos,
            boolean worldAvailable,
            boolean chunkLoaded,
            boolean blockEntityLoaded,
            String blockEntityType,
            String blockId,
            String expectedBlockId
    ) {
        static ActionRelayTarget missing() {
            return new ActionRelayTarget(null, null, null, null, false, false, false, "", "", "");
        }

        static ActionRelayTarget unsupportedType(SignalDeviceData device) {
            return new ActionRelayTarget(device, null, null, null, false, false, false, "", "", "");
        }

        boolean actionsReadable() {
            return relay != null;
        }

        boolean editable() {
            return relay != null;
        }

        String loadedState() {
            return WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(
                    worldAvailable,
                    chunkLoaded,
                    blockId,
                    expectedBlockId,
                    blockEntityLoaded,
                    relay != null
            );
        }

        String unsupportedReason() {
            return switch (loadedState()) {
                case "world_unavailable" -> "设备维度不可用或世界未加载。";
                case "chunk_unloaded" -> "该 action_relay 所在区块未加载。WebAdmin 不会强制加载区块；请让玩家靠近该方块后重试。";
                case "block_missing" -> "区块已加载，但该位置是空气或没有可用方块。预期方块：" + expectedBlockId + "。";
                case "physical_block_mismatch" -> "区块已加载，但当前位置不是 action_relay 方块。预期方块：" + expectedBlockId + "。";
                case "block_entity_missing" -> "当前方块是 " + (isBlank(blockId) ? expectedBlockId : blockId)
                        + "，但区块内缺少 ActionRelayBlockEntity。可能是旧存档、外部编辑或方块实体数据未随区块正常恢复。";
                case "block_entity_type_mismatch" -> "区块已加载，但当前位置的方块实体不是 action_relay（当前："
                        + (isBlank(blockEntityType) ? "未知" : blockEntityType) + "，预期：ActionRelayBlockEntity）。";
                default -> "";
            };
        }
    }

    private record Validation(List<WebAdminValidationError> errors, List<ActionConfig> actions) {
    }
}
