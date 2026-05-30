package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionOwnerType;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerActionRequests;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminSignalListenerActionsService {
    public static final int MAX_ACTIONS = WebAdminActionRelayActionsService.MAX_ACTIONS;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator gateBindingValidator;
    private final Path testStorePath;

    public WebAdminSignalListenerActionsService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null);
    }

    WebAdminSignalListenerActionsService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path testStorePath
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.testStorePath = testStorePath;
        this.gateBindingValidator = new WebAdminConditionGateBindingValidator(conditionGroupTestPath(testStorePath));
    }

    private static Path conditionGroupTestPath(Path signalListenerTestStorePath) {
        return signalListenerTestStorePath == null || signalListenerTestStorePath.getParent() == null
                ? null
                : signalListenerTestStorePath.getParent().resolve(WebAdminConditionGroupStore.FILE_NAME);
    }

    public Map<String, Object> actionsFor(MinecraftServer server, WebAdminUser user, WebAdminSession session, String listenerRef) {
        SignalListenerData listener = findListener(server, listenerRef);
        return listener == null ? null : actionData(listener, user, session);
    }

    public WebAdminWriteResult addAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String listenerRef,
            WebAdminSignalListenerActionRequests.ActionAddRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        synchronized (SignalListenerStore.class) {
        SignalListenerData before = findListener(server, listenerRef);
        WebAdminWriteTarget target = before == null ? target(safe(listenerRef), safe(listenerRef)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "action_add_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, before.id(), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_add_denied"));
            return common;
        }
        ActionValidation validation = validateActionEntry(server, request == null ? null : request.action);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(context, result, auditSummary(before), Map.of("attempt", "action_validation_failed"));
            return result;
        }
        if (before.actions().size() >= MAX_ACTIONS) {
            WebAdminWriteResult result = validation(target, "actions", "too_many", "虚拟监听器最多支持 " + MAX_ACTIONS + " 条 Action。", String.valueOf(before.actions().size()));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_too_many"));
            return result;
        }
        boolean added = addAction(server, before.id(), validation.action());
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        SignalListenerData after = findListener(server, before.id());
        if (!added || after == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_add_removed"));
            return result;
        }
        Map<String, Object> data = Map.of(
                "actionList", actionData(after, user, session),
                "changedFields", List.of("actions")
        );
        WebAdminWriteResult result = writeOk(target, true, "虚拟监听器动作已添加。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after));
        publishRealtime(after, "action_added", auditEvent, user);
        releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
        return result;
        }
    }

    public WebAdminWriteResult clearActions(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String listenerRef,
            WebAdminSignalListenerActionRequests.ActionClearRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        synchronized (SignalListenerStore.class) {
        SignalListenerData before = findListener(server, listenerRef);
        WebAdminWriteTarget target = before == null ? target(safe(listenerRef), safe(listenerRef)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "action_clear_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, before.id(), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_clear_denied"));
            return common;
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "清空虚拟监听器动作需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "clear_requires_confirmation"));
            return result;
        }
        if (before.actions().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "虚拟监听器动作列表已经为空。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
            return result;
        }
        boolean cleared = clearActions(server, before.id());
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        SignalListenerData after = findListener(server, before.id());
        if (!cleared || after == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_clear_removed"));
            return result;
        }
        Map<String, Object> data = Map.of(
                "actionList", actionData(after, user, session),
                "changedFields", List.of("actions")
        );
        WebAdminWriteResult result = writeOk(target, true, "虚拟监听器动作已清空。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after));
        publishRealtime(after, "actions_cleared", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
        }
    }

    public WebAdminWriteResult deleteAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String listenerRef,
            WebAdminSignalListenerActionRequests.ActionDeleteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        synchronized (SignalListenerStore.class) {
        SignalListenerData before = findListener(server, listenerRef);
        WebAdminWriteTarget target = before == null ? target(safe(listenerRef), safe(listenerRef)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "action_delete_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, before.id(), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_delete_denied"));
            return common;
        }
        if (request == null || !Boolean.TRUE.equals(request.confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "删除单条虚拟监听器动作需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_delete_requires_confirmation"));
            return result;
        }
        int index = parseInteger(request.actionIndex, -1);
        List<ActionConfig> actions = new ArrayList<>(before.actions());
        if (index < 0 || index >= actions.size()) {
            WebAdminWriteResult result = validation(target, "actionIndex", "out_of_range", "要删除的 action 已不存在，请刷新后重试。", String.valueOf(index));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_delete_out_of_range", "index", index));
            return result;
        }
        actions.remove(index);
        SignalListenerData after = replaceActions(server, before.id(), actions);
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        if (after == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_delete_removed"));
            return result;
        }
        Map<String, Object> data = Map.of(
                "actionList", actionData(after, user, session),
                "changedFields", List.of("actions")
        );
        WebAdminWriteResult result = writeOk(target, true, "虚拟监听器动作已删除。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after));
        publishRealtime(after, "action_deleted", auditEvent, user);
        releaseLockAfterWrite(before.id(), request.lockId, user, session, remoteAddress);
        return result;
        }
    }

    public WebAdminWriteResult reorderAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String listenerRef,
            Object fromIndexValue,
            Object toIndexValue,
            Boolean confirmed,
            String expectedFingerprint,
            String lockId,
            String csrfToken,
            boolean sameOrigin
    ) {
        synchronized (SignalListenerStore.class) {
        SignalListenerData before = findListener(server, listenerRef);
        WebAdminWriteTarget target = before == null ? target(safe(listenerRef), safe(listenerRef)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "action_reorder_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, before.id(), safe(lockId), safe(expectedFingerprint), before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_reorder_denied"));
            return common;
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION, target, "重排虚拟监听器动作需要二次确认。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_reorder_requires_confirmation"));
            return result;
        }
        int fromIndex = parseInteger(fromIndexValue, -1);
        int toIndex = parseInteger(toIndexValue, -1);
        List<ActionConfig> actions = new ArrayList<>(before.actions());
        if (fromIndex < 0 || fromIndex >= actions.size() || toIndex < 0 || toIndex >= actions.size()) {
            WebAdminWriteResult result = validation(target, "fromIndex", "out_of_range", "要重排的 action 已不存在，请刷新后重试。", fromIndex + " -> " + toIndex);
            audit(context, result, auditSummary(before), Map.of("attempt", "action_reorder_out_of_range", "fromIndex", fromIndex, "toIndex", toIndex));
            return result;
        }
        if (fromIndex == toIndex) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "虚拟监听器动作顺序没有变化。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), lockId, user, session, remoteAddress);
            return result;
        }
        ActionConfig moving = actions.remove(fromIndex);
        actions.add(toIndex, moving);
        SignalListenerData after = replaceActions(server, before.id(), actions);
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        if (after == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_reorder_removed"));
            return result;
        }
        Map<String, Object> data = Map.of(
                "actionList", actionData(after, user, session),
                "changedFields", List.of("actions"),
                "fromIndex", fromIndex,
                "toIndex", toIndex
        );
        WebAdminWriteResult result = writeOk(target, true, "虚拟监听器动作已重排。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after));
        publishRealtime(after, "action_reordered", auditEvent, user);
        releaseLockAfterWrite(before.id(), lockId, user, session, remoteAddress);
        return result;
        }
    }

    public WebAdminWriteResult updateAction(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String listenerRef,
            WebAdminSignalListenerActionRequests.ActionUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        synchronized (SignalListenerStore.class) {
        SignalListenerData before = findListener(server, listenerRef);
        WebAdminWriteTarget target = before == null ? target(safe(listenerRef), safe(listenerRef)) : target(before);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, target);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of("attempt", "action_update_missing"));
            return result;
        }
        WebAdminWriteResult common = validateWriteCommon(user, session, csrfToken, sameOrigin, target, before.id(), request == null ? "" : request.lockId, request == null ? "" : request.expectedFingerprint, before);
        if (common != null) {
            audit(context, common, auditSummary(before), Map.of("attempt", "action_update_denied"));
            return common;
        }
        int index = parseInteger(request == null ? null : request.actionIndex, -1);
        List<ActionConfig> actions = new ArrayList<>(before.actions());
        if (index < 0 || index >= actions.size()) {
            WebAdminWriteResult result = validation(target, "actionIndex", "out_of_range", "要编辑的 action 已不存在，请刷新后重试。", String.valueOf(index));
            audit(context, result, auditSummary(before), Map.of("attempt", "action_update_out_of_range", "index", index));
            return result;
        }
        ActionValidation validation = validateActionEntry(server, request == null ? null : request.action);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(context, result, auditSummary(before), Map.of("attempt", "action_update_validation_failed", "index", index));
            return result;
        }
        ActionConfig beforeAction = actions.get(index);
        actions.set(index, validation.action());
        if (normalizeAction(beforeAction).equals(validation.action())) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的动作变化。");
            audit(context, result, auditSummary(before), auditSummary(before));
            releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
            return result;
        }
        SignalListenerData after = replaceActions(server, before.id(), actions);
        if (testStorePath == null) {
            SignalListenerStore.flushDirty(server);
        }
        if (after == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Listener 已被删除。");
            audit(context, result, auditSummary(before), Map.of("attempt", "action_update_removed"));
            return result;
        }
        Map<String, Object> data = Map.of(
                "actionList", actionData(after, user, session),
                "changedFields", List.of("actions"),
                "actionIndex", index
        );
        WebAdminWriteResult result = writeOk(target, true, "虚拟监听器动作已更新。", data);
        WebAdminAuditEvent auditEvent = audit(context, result, auditSummary(before), auditSummary(after));
        publishRealtime(after, "action_updated", auditEvent, user);
        releaseLockAfterWrite(before.id(), request == null ? "" : request.lockId, user, session, remoteAddress);
        return result;
        }
    }

    private Map<String, Object> actionData(SignalListenerData rawListener, WebAdminUser user, WebAdminSession session) {
        SignalListenerData listener = rawListener == null ? null : rawListener.normalized();
        if (listener == null) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listenerId", listener.id());
        data.put("displayName", displayName(listener));
        data.put("channel", listener.channel());
        data.put("enabled", listener.enabled());
        data.put("cooldownTicks", listener.cooldownTicks());
        data.put("actionCount", listener.actions().size());
        data.put("actions", actionDtos(listener.actions(), listener.id()));
        data.put("allowedActionTypes", List.of("command", "signal", "message", "sound", "state_variable", "timer_start", "timer_cancel"));
        data.put("expectedFingerprint", WebAdminSignalListenerBasicConfigService.fingerprintFor(listener));
        WebAdminEditLockStatusDto lockStatus = editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS,
                listener.id(),
                user,
                session
        );
        data.put("lockStatus", lockStatus);
        data.put("noRawJson", true);
        data.put("noConditionEngine", false);
        data.put("conditionRuntimeGates", true);
        data.put("singleActionConditionGates", true);
        data.put("noPathVisualization", true);
        data.put("notes", List.of(
                "虚拟监听器动作列表按当前 ActionEngine 顺序执行，不改变 SignalBridge 运行时语义。",
                "单条 Action 条件组为空 = 此 action 不单独判断，保持旧执行逻辑；配置后仅跳过当前 action 并继续后续 action。",
                "状态变量动作使用结构化字段写入 GLOBAL / PLAYER StateVariable，不提供 raw JSON、脚本、表达式或 NBT path。",
                "command action 会阻断 stop/op/ban/kick/whitelist 等危险服务器管理命令，包括 execute ... run 嵌套形式。",
                "本编辑器不提供 raw JSON、ConditionEngine 或路径可视化。"
        ));
        return data;
    }

    private WebAdminWriteResult validateWriteCommon(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            String listenerId,
            String lockId,
            String expectedFingerprint,
            SignalListenerData before
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS);
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
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS,
                    listenerId,
                    lockId,
                    user,
                    session
            );
            if (!validation.success()) {
                return validation.result();
            }
        }
        if (isBlank(expectedFingerprint)) {
            return validation(target, "expectedFingerprint", "required", "写入需要 expectedFingerprint，用于防止覆盖其他操作的修改。", "");
        }
        if (!WebAdminSignalListenerBasicConfigService.fingerprintFor(before).equals(expectedFingerprint)) {
            return conflict(target, before, expectedFingerprint);
        }
        return null;
    }

    private ActionValidation validateActionEntry(MinecraftServer server, WebAdminActionRelayActionsUpdateRequest.ActionEntry entry) {
        WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
        request.actions = entry == null ? List.of() : List.of(entry);
        List<WebAdminValidationError> errors = WebAdminActionRelayActionsService.validateActionEntries(request.actions, ActionOwnerType.SIGNAL_LISTENER);
        if (!errors.isEmpty()) {
            return new ActionValidation(errors, null);
        }
        if (entry == null) {
            return new ActionValidation(List.of(new WebAdminValidationError("action", "required", "Action 配置不能为空。", "")), null);
        }
        gateBindingValidator.validate(
                server,
                errors,
                "action.conditionGroupId",
                entry.conditionGroupId,
                ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION
        );
        if (!errors.isEmpty()) {
            return new ActionValidation(errors, null);
        }
        return new ActionValidation(List.of(), WebAdminActionRelayActionsService.actionFromEntry(entry));
    }

    private static List<Map<String, Object>> actionDtos(List<ActionConfig> actions, String listenerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action == null) {
                continue;
            }
            ActionConfig normalized = normalizeAction(action);
            String actionTargetId = ConditionActionGateService.actionTargetId("listener", listenerId, index);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", index);
            entry.put("displayIndex", index + 1);
            entry.put("type", normalized.type().id());
            entry.put("value", normalized.value());
            entry.put("enabled", normalized.enabled());
            entry.put("requiresOp", normalized.requiresOp());
            entry.put("cooldownTicks", normalized.cooldownTicks());
            entry.put("notifyOps", normalized.notifyOps());
            entry.put("conditionGroupId", normalized.conditionGroupId());
            WebAdminActionRelayActionsService.putStateActionFields(entry, normalized);
            WebAdminActionRelayActionsService.putTimerActionFields(entry, normalized);
            entry.put("actionConditionGateTargetType", ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION.id());
            entry.put("actionConditionGateTargetId", actionTargetId);
            entry.put("recentActionConditionGate", WebAdminConditionGateHistoryService.recentStatus(
                    ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                    actionTargetId
            ));
            entry.put("summary", actionSummary(normalized));
            result.add(entry);
            index++;
        }
        return List.copyOf(result);
    }

    private static ActionConfig normalizeAction(ActionConfig action) {
        ActionType type = action == null || action.type() == null ? ActionType.COMMAND : action.type();
        return action == null ? new ActionConfig(type, "", false, false, 0, false) : action.normalized();
    }

    private static String actionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        if (action.type() == ActionType.STATE_VARIABLE) {
            return (action.enabled() ? "" : "[disabled] ") + action.type().id() + ": " + action.stateActionSummary();
        }
        if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            return (action.enabled() ? "" : "[disabled] ") + action.type().id() + ": " + action.timerActionSummary();
        }
        return (action.enabled() ? "" : "[disabled] ") + action.type().id() + ": " + safe(action.value());
    }

    private static String auditActionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String value = safe(action.value());
        if (action.type() == ActionType.STATE_VARIABLE) {
            value = action.stateActionSummary() + " " + action.stateAuditFingerprint();
        } else if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            value = action.timerActionSummary() + " " + action.timerAuditFingerprint();
        } else
        if (action.type() == ActionType.COMMAND) {
            value = "<command redacted length=" + value.length() + ">";
        } else if (value.length() > 96) {
            value = value.substring(0, 96) + "...";
        }
        return action.type().id()
                + ":" + value
                + " enabled=" + action.enabled()
                + " conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
    }

    private static Map<String, Object> auditSummary(SignalListenerData rawListener) {
        Map<String, Object> summary = new LinkedHashMap<>();
        SignalListenerData listener = rawListener == null ? null : rawListener.normalized();
        if (listener == null) {
            return summary;
        }
        summary.put("listenerId", listener.id());
        summary.put("name", listener.name());
        summary.put("channel", listener.channel());
        summary.put("enabled", listener.enabled());
        summary.put("cooldownTicks", listener.cooldownTicks());
        summary.put("actionCount", listener.actions().size());
        summary.put("actions", listener.actions().stream().map(WebAdminSignalListenerActionsService::auditActionSummary).toList());
        summary.put("expectedFingerprint", WebAdminSignalListenerBasicConfigService.fingerprintFor(listener));
        return summary;
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private static void publishRealtime(SignalListenerData listener, String action, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        if (listener == null) {
            return;
        }
        String routeTarget = "#/listeners/" + encode(listener.id());
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("虚拟监听器动作列表已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "signal_listener_actions")
                .payload("listenerId", listener.id())
                .payload("action", action)
                .payload("actionCount", listener.actions().size())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent actionEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_LISTENER_ACTION_CHANGED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("虚拟监听器动作已变化：" + displayName(listener))
                .routeTarget(routeTarget)
                .payload("targetType", "signal_listener_actions")
                .payload("listenerId", listener.id())
                .payload("action", action)
                .payload("actionCount", listener.actions().size())
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .channel(listener.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "signal_listener_actions")
                .payload("listenerId", listener.id())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("actionEventId", actionEvent == null ? "" : actionEvent.id()));
    }

    private void releaseLockAfterWrite(String listenerId, String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress) {
        if (editLockService == null || isBlank(lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listenerId, lockId, user, session, remoteAddress);
    }

    private SignalListenerData findListener(MinecraftServer server, String listenerRef) {
        if ((server == null && testStorePath == null) || isBlank(listenerRef)) {
            return null;
        }
        SignalListenerStore.ResolveResult resolved = testStorePath == null
                ? SignalListenerStore.resolveListener(server, listenerRef)
                : SignalListenerStore.resolveListener(testStorePath, listenerRef);
        return resolved.foundUnique() ? resolved.listener().normalized() : null;
    }

    private boolean addAction(MinecraftServer server, String listenerId, ActionConfig action) {
        return testStorePath == null
                ? SignalListenerStore.addAction(server, listenerId, action)
                : SignalListenerStore.addAction(testStorePath, listenerId, action);
    }

    private boolean clearActions(MinecraftServer server, String listenerId) {
        return testStorePath == null
                ? SignalListenerStore.clearActions(server, listenerId)
                : SignalListenerStore.clearActions(testStorePath, listenerId);
    }

    private SignalListenerData replaceActions(MinecraftServer server, String listenerId, List<ActionConfig> actions) {
        return testStorePath == null
                ? SignalListenerStore.replaceActionsForWebAdmin(server, listenerId, actions)
                : SignalListenerStore.replaceActionsForWebAdmin(testStorePath, listenerId, actions);
    }

    private static WebAdminWriteResult writeOk(WebAdminWriteTarget target, boolean changed, String message, Map<String, Object> data) {
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

    private static WebAdminWriteResult conflict(WebAdminWriteTarget target, SignalListenerData current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", safe(expectedFingerprint));
        conflict.put("currentFingerprint", WebAdminSignalListenerBasicConfigService.fingerprintFor(current));
        conflict.put("currentListener", auditSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "虚拟监听器动作列表已被其他操作修改，请刷新后再编辑。",
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

    private static WebAdminWriteTarget target(SignalListenerData listener) {
        return target(listener.id(), displayName(listener));
    }

    private static WebAdminWriteTarget target(String id, String displayName) {
        return new WebAdminWriteTarget("SIGNAL_LISTENER_ACTIONS", safe(id), isBlank(displayName) ? safe(id) : displayName);
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

    private static boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) {
                return true;
            }
            if ("false".equalsIgnoreCase(string.trim())) {
                return false;
            }
        }
        return fallback;
    }

    private static int parseInteger(Object value, int fallback) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            return Double.isFinite(d) && Math.rint(d) == d ? number.intValue() : fallback;
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

    private static String displayName(SignalListenerData listener) {
        return listener == null || isBlank(listener.name()) ? "Listener " + SignalListenerStore.shortId(listener == null ? "" : listener.id()) : listener.name();
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ActionValidation(List<WebAdminValidationError> errors, ActionConfig action) {
    }
}
