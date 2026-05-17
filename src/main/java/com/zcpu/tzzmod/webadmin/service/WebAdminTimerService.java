package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.scheduler.TimerOperationResult;
import com.zcpu.tzzmod.scheduler.TimerRuntimeService;
import com.zcpu.tzzmod.scheduler.TimerScopeMode;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerStatusSnapshot;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerValidationIssue;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminTimerService {
    public static final String TARGET_TYPE = "TIMER";
    private static final int MAX_ACTIONS_PER_LIST = 64;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator conditionGateBindingValidator;
    private final Path testStorePath;

    public WebAdminTimerService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null);
    }

    public WebAdminTimerService(Path testStorePath) {
        this(new WebAdminPermissionService(), new WebAdminWriteSecurityService(), null, testStorePath);
    }

    WebAdminTimerService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path testStorePath
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.testStorePath = testStorePath;
        this.conditionGateBindingValidator = new WebAdminConditionGateBindingValidator(conditionGroupTestPath(testStorePath));
    }

    private static Path conditionGroupTestPath(Path timerTestStorePath) {
        return timerTestStorePath == null || timerTestStorePath.getParent() == null
                ? null
                : timerTestStorePath.getParent().resolve(WebAdminConditionGroupStore.FILE_NAME);
    }

    public Map<String, Object> list(MinecraftServer server, WebAdminUser user, WebAdminSession session) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("timers", List.of(), "permissionDenied", true, "message", permission.message());
        }
        TimerStore.TimerLoadResult loaded = loadResult(server);
        long gameTime = currentGameTime(server);
        List<Map<String, Object>> timers = loaded.file().timers.values().stream()
                .map(timer -> detailMap(server, timer, user, session, false, gameTime))
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getOrDefault("displayName", ""))))
                .toList();
        return Map.of(
                "timers", timers,
                "count", timers.size(),
                "worldScoped", true,
                "storeFile", TimerStore.FILE_NAME,
                "runtimeStatePersistent", false,
                "storeDegraded", loaded.degraded(),
                "storeMessage", loaded.message()
        );
    }

    public Map<String, Object> detail(MinecraftServer server, WebAdminUser user, WebAdminSession session, String id) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("permissionDenied", true, "message", permission.message());
        }
        String safeId = TimerStore.normalizeId(id);
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return Map.of("notFound", true, "storeDegraded", true, "message", loaded.message(), "id", safeId);
        }
        TimerDefinition timer = loaded.file().timers.get(safeId);
        if (timer == null) {
            return Map.of("notFound", true, "message", "Timer 不存在或已删除。", "id", safeId);
        }
        return detailMap(server, timer, user, session, true, currentGameTime(server));
    }

    public Map<String, Object> status(MinecraftServer server, WebAdminUser user, String id) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("permissionDenied", true, "message", permission.message());
        }
        String safeId = TimerStore.normalizeId(id);
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return Map.of("notFound", true, "storeDegraded", true, "message", loaded.message(), "id", safeId);
        }
        TimerDefinition timer = loaded.file().timers.get(safeId);
        if (timer == null) {
            return Map.of("notFound", true, "message", "Timer 不存在或已删除。", "id", safeId);
        }
        return statusMap(TimerRuntimeService.status(server, timer, currentGameTime(server)));
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminTimerRequest safeRequest = request == null ? new WebAdminTimerRequest() : request;
        TimerDefinition after = definitionFromRequest(safeRequest, null);
        WebAdminWriteTarget target = target(after.id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_TIMER, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, after.id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        List<WebAdminValidationError> errors = validate(server, after, safeRequest, true);
        if (loaded.file().timers.containsKey(after.id)) {
            errors.add(error("id", "timer_id_duplicate", "Timer ID 已存在：" + after.id, after.id));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(after));
            return result;
        }
        TimerDefinition saved = after.withWriteMetadata(username(user), 1L, true);
        loaded.file().timers.put(saved.id, saved);
        if (!save(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Timer 保存失败，请查看服务端日志。");
            audit(context, result, Map.of(), requestSummary(after));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "Timer 已创建。", Map.of("timer", detailMap(server, saved, user, session, true, currentGameTime(server)), "routeTarget", routeTarget(saved.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), TimerStore.summary(saved));
        publishConfigRealtime(saved, auditEvent, user, "Timer 已创建。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, saved.id);
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminTimerRequest safeRequest = request == null ? new WebAdminTimerRequest() : request;
        safeRequest.id = TimerStore.normalizeId(id);
        TimerDefinition afterDraft = definitionFromRequest(safeRequest, null);
        WebAdminWriteTarget target = target(afterDraft.id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_TIMER, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, afterDraft.id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        TimerDefinition before = loaded.file().timers.get(afterDraft.id);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Timer 不存在或已删除。");
            audit(context, result, Map.of(), requestSummary(afterDraft));
            return result;
        }
        List<WebAdminValidationError> errors = validate(server, afterDraft, safeRequest, false);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, TimerStore.summary(before), requestSummary(afterDraft));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        String actual = TimerStore.fingerprintFor(before);
        if (expected.isBlank() || !actual.equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, TimerStore.summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        TimerDefinition after = afterDraft.withWriteMetadata(username(user), before.version + 1L, false);
        if (editableEquals(before, after)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Timer 变化。");
            audit(context, result, TimerStore.summary(before), TimerStore.summary(before));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
            return result;
        }
        loaded.file().timers.put(after.id, after);
        if (!save(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Timer 保存失败，请查看服务端日志。");
            audit(context, result, TimerStore.summary(before), TimerStore.summary(after));
            return result;
        }
        TimerRuntimeService.clearTimer(server, after.id);
        WebAdminWriteResult result = okWithData(target, "Timer 已保存，运行中实例已清理。", Map.of("timer", detailMap(server, after, user, session, true, currentGameTime(server)), "routeTarget", routeTarget(after.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, TimerStore.summary(before), TimerStore.summary(after));
        publishConfigRealtime(after, auditEvent, user, "Timer 已保存。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
        return result;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminTimerRequest safeRequest = request == null ? new WebAdminTimerRequest() : request;
        String safeId = TimerStore.normalizeId(id);
        WebAdminWriteTarget target = target(safeId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_TIMER, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        TimerDefinition before = loaded.file().timers.get(safeId);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "Timer 不存在或已删除。");
            audit(context, result, Map.of(), Map.of("deleted", false));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
            return result;
        }
        if (!safeRequest.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(error("confirmed", "confirmation_required", "删除前需要确认。", "false")));
            audit(context, result, TimerStore.summary(before), Map.of("attempt", "delete_without_confirmation"));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        if (expected.isBlank() || !TimerStore.fingerprintFor(before).equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, TimerStore.summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        loaded.file().timers.remove(safeId);
        if (!save(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Timer 删除失败，请查看服务端日志。");
            audit(context, result, TimerStore.summary(before), Map.of("deleted", false));
            return result;
        }
        TimerRuntimeService.clearTimer(server, safeId);
        WebAdminWriteResult result = okWithData(target, "Timer 已删除。", Map.of("routeTarget", "#/timers"));
        WebAdminAuditEvent auditEvent = audit(context, result, TimerStore.summary(before), Map.of("deleted", true));
        publishConfigRealtime(before, auditEvent, user, "Timer 已删除。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
        return result;
    }

    public WebAdminWriteResult start(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        return runtimeWrite(server, user, session, remoteAddress, id, request, csrfToken, sameOrigin, "start");
    }

    public WebAdminWriteResult cancel(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        return runtimeWrite(server, user, session, remoteAddress, id, request, csrfToken, sameOrigin, "cancel");
    }

    public WebAdminWriteResult reset(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        return runtimeWrite(server, user, session, remoteAddress, id, request, csrfToken, sameOrigin, "reset");
    }

    private WebAdminWriteResult runtimeWrite(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminTimerRequest request,
            String csrfToken,
            boolean sameOrigin,
            String operation
    ) {
        WebAdminTimerRequest safeRequest = request == null ? new WebAdminTimerRequest() : request;
        String safeId = TimerStore.normalizeId(id);
        WebAdminWriteTarget target = target(safeId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_TIMER, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed", "operation", operation));
            return preflight;
        }
        TimerStore.TimerLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded", "operation", operation));
            return result;
        }
        TimerDefinition timer = loaded.file().timers.get(safeId);
        if (timer == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Timer 不存在或已删除。");
            audit(context, result, Map.of(), Map.of("attempt", operation + "_missing"));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        if (expected.isBlank() || !TimerStore.fingerprintFor(timer).equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, timer, expected);
            audit(context, result, TimerStore.summary(timer), Map.of("attempt", "fingerprint_conflict", "operation", operation));
            return result;
        }
        if ("reset".equals(operation) && !safeRequest.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(error("confirmed", "confirmation_required", "重置 runtime state 前需要确认。", "false")));
            audit(context, result, TimerStore.summary(timer), Map.of("attempt", "reset_without_confirmation"));
            return result;
        }

        WebAdminWriteResult result;
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("operation", operation);
        after.put("targetMode", safe(safeRequest.targetMode));
        after.put("targetId", safe(safeRequest.targetId));
        after.put("scopeKey", safe(safeRequest.scopeKey));
        if ("start".equals(operation)) {
            TimerOperationResult started = TimerRuntimeService.startManual(server, safeId, safeRequest.targetMode, safeRequest.targetId, safeRequest.startPolicyOverride);
            after.putAll(started.actionDetails());
            result = started.success()
                    ? okWithData(target, started.message(), Map.of("operationResult", started, "status", statusMap(TimerRuntimeService.status(server, timer, currentGameTime(server)))))
                    : WebAdminWriteResult.failed(WebAdminWriteResultCode.VALIDATION_FAILED, target, started.message());
        } else if ("cancel".equals(operation)) {
            TimerOperationResult cancelled = TimerRuntimeService.cancelManual(server, safeId, safeRequest.targetMode, safeRequest.targetId);
            after.putAll(cancelled.actionDetails());
            result = cancelled.success()
                    ? okWithData(target, cancelled.message(), Map.of("operationResult", cancelled, "status", statusMap(TimerRuntimeService.status(server, timer, currentGameTime(server)))))
                    : WebAdminWriteResult.failed(WebAdminWriteResultCode.VALIDATION_FAILED, target, cancelled.message());
        } else {
            int resetCount = TimerRuntimeService.reset(server, safeId, safeRequest.scopeKey, "manual");
            after.put("resetCount", resetCount);
            result = okWithData(target, resetCount == 0 ? "Timer 当前没有运行中实例。" : "Timer runtime state 已重置。", Map.of("resetCount", resetCount, "status", statusMap(TimerRuntimeService.status(server, timer, currentGameTime(server)))));
        }
        WebAdminAuditEvent auditEvent = audit(context, result, TimerStore.summary(timer), after);
        publishRuntimeRealtime(timer, auditEvent, user, runtimeSummary(operation, result));
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
        return result;
    }

    private Map<String, Object> detailMap(MinecraftServer server, TimerDefinition raw, WebAdminUser user, WebAdminSession session, boolean includeStatus, long gameTime) {
        TimerDefinition timer = raw == null ? new TimerDefinition() : raw.normalized();
        Map<String, Object> data = new LinkedHashMap<>(TimerStore.summary(timer));
        data.put("onStartActions", actionMaps(timer.onStartActions));
        data.put("onTickActions", actionMaps(timer.onTickActions));
        data.put("onCompleteActions", actionMaps(timer.onCompleteActions));
        data.put("onCancelActions", actionMaps(timer.onCancelActions));
        data.put("validationErrors", validate(server, timer, null, false));
        data.put("lockStatus", editLockService == null
                ? Map.of("targetType", WebAdminEditLockService.TARGET_TIMER_CONFIG, "targetId", timer.id, "editable", true)
                : editLockService.status(WebAdminEditLockService.TARGET_TIMER_CONFIG, timer.id, user, session));
        data.put("routeTarget", routeTarget(timer.id));
        data.put("runtimeStatePersistent", false);
        if (includeStatus) {
            data.put("status", statusMap(TimerRuntimeService.status(server, timer, gameTime)));
        } else {
            TimerStatusSnapshot status = TimerRuntimeService.status(server, timer, gameTime);
            data.put("activeInstanceCount", status.activeInstanceCount());
            data.put("lastResult", status.lastResult());
            data.put("lastFailureReason", status.lastFailureReason());
        }
        return data;
    }

    private static Map<String, Object> statusMap(TimerStatusSnapshot snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timerId", snapshot.timerId());
        data.put("enabled", snapshot.enabled());
        data.put("mode", snapshot.mode());
        data.put("scopeMode", snapshot.scopeMode());
        data.put("startPolicy", snapshot.startPolicy());
        data.put("activeInstanceCount", snapshot.activeInstanceCount());
        data.put("lastStatusAt", snapshot.lastStatusAt());
        data.put("lastResult", snapshot.lastResult());
        data.put("lastFailureReason", snapshot.lastFailureReason());
        data.put("instances", snapshot.instances());
        data.put("runtimeStatePersistent", snapshot.runtimeStatePersistent());
        return Map.copyOf(data);
    }

    private TimerDefinition definitionFromRequest(WebAdminTimerRequest request, TimerDefinition before) {
        TimerDefinition timer = new TimerDefinition();
        timer.id = TimerStore.normalizeId(request == null ? "" : request.id);
        timer.displayName = safe(request == null ? "" : request.displayName);
        timer.note = safe(request == null ? "" : request.note);
        timer.enabled = request == null || request.enabled;
        timer.mode = TimerMode.parse(request == null ? "" : request.mode);
        timer.scopeMode = TimerScopeMode.parse(request == null ? "" : request.scopeMode);
        timer.durationTicks = request == null ? 20L : request.durationTicks;
        timer.intervalTicks = request == null ? 20L : request.intervalTicks;
        timer.maxRuns = request == null ? 1 : request.maxRuns;
        timer.startPolicy = TimerStartPolicy.parse(request == null ? "" : request.startPolicy);
        timer.outputChannel = safe(request == null ? "" : request.outputChannel);
        timer.onStartActions = actionsFromEntries(request == null ? null : request.onStartActions);
        timer.onTickActions = actionsFromEntries(request == null ? null : request.onTickActions);
        timer.onCompleteActions = actionsFromEntries(request == null ? null : request.onCompleteActions);
        timer.onCancelActions = actionsFromEntries(request == null ? null : request.onCancelActions);
        applyModeSemantics(timer);
        timer.createdAt = before == null ? "" : before.createdAt;
        timer.updatedAt = before == null ? "" : before.updatedAt;
        timer.updatedBy = before == null ? "" : before.updatedBy;
        timer.version = before == null ? 0L : before.version;
        return timer;
    }

    private static void applyModeSemantics(TimerDefinition timer) {
        if (timer == null || timer.mode == null) {
            return;
        }
        if (timer.mode == TimerMode.DELAY) {
            timer.intervalTicks = 0L;
            timer.maxRuns = 1;
            timer.onTickActions = List.of();
        } else if (timer.mode == TimerMode.COUNTDOWN) {
            timer.maxRuns = 1;
        } else if (timer.mode == TimerMode.REPEAT) {
            timer.durationTicks = 0L;
        }
    }

    private static List<ActionConfig> actionsFromEntries(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<ActionConfig> actions = new ArrayList<>();
        for (WebAdminActionRelayActionsUpdateRequest.ActionEntry entry : entries) {
            if (entry != null) {
                actions.add(WebAdminActionRelayActionsService.actionFromEntry(entry).normalized());
            }
        }
        return List.copyOf(actions);
    }

    private List<WebAdminValidationError> validate(MinecraftServer server, TimerDefinition timer, WebAdminTimerRequest request, boolean creating) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        validateRawRequest(request, errors);
        for (TimerValidationIssue issue : TimerValidator.validate(timer, creating)) {
            errors.add(error(issue.field(), issue.code(), issue.message(), issue.rejectedValue()));
        }
        TimerMode mode = timer == null || timer.mode == null ? TimerMode.DELAY : timer.mode;
        validateActionEntries(server, "onStartActions", request == null ? null : request.onStartActions, ConditionRuntimeTargetType.TIMER_ON_START_ACTION, errors);
        if (mode != TimerMode.DELAY) {
            validateActionEntries(server, "onTickActions", request == null ? null : request.onTickActions, ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION, errors);
        }
        validateActionEntries(server, "onCompleteActions", request == null ? null : request.onCompleteActions, ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION, errors);
        validateActionEntries(server, "onCancelActions", request == null ? null : request.onCancelActions, ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION, errors);
        return List.copyOf(errors);
    }

    private static void validateRawRequest(WebAdminTimerRequest request, List<WebAdminValidationError> errors) {
        if (request == null) {
            return;
        }
        if (!oneOf(request.mode, "DELAY", "COUNTDOWN", "REPEAT")) {
            errors.add(error("mode", "timer_mode_invalid", "模式必须是 DELAY、COUNTDOWN 或 REPEAT。", safe(request.mode)));
        }
        if (!oneOf(request.scopeMode, "GLOBAL", "PLAYER")) {
            errors.add(error("scopeMode", "timer_scope_invalid", "作用域必须选择 GLOBAL 或 PLAYER。", safe(request.scopeMode)));
        }
        if (!oneOf(request.startPolicy, "RESTART", "IGNORE_IF_RUNNING", "FAIL_IF_RUNNING")) {
            errors.add(error("startPolicy", "timer_start_policy_invalid", "启动策略必须是 RESTART、IGNORE_IF_RUNNING 或 FAIL_IF_RUNNING。", safe(request.startPolicy)));
        }
    }

    private void validateActionEntries(
            MinecraftServer server,
            String field,
            List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries,
            ConditionRuntimeTargetType targetType,
            List<WebAdminValidationError> errors
    ) {
        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> safeEntries = entries == null ? List.of() : entries;
        if (safeEntries.size() > MAX_ACTIONS_PER_LIST) {
            errors.add(error(field, "timer_too_many_actions", "每个 Timer action list 最多支持 64 条动作。", String.valueOf(safeEntries.size())));
            return;
        }
        for (int index = 0; index < safeEntries.size(); index++) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = safeEntries.get(index);
            String prefix = field + "[" + index + "]";
            if (entry == null) {
                errors.add(error(prefix, "timer_action_required", "Timer action 不能为空。", ""));
                continue;
            }
            ActionType type = parseActionType(entry.type);
            if (type == null) {
                errors.add(error(prefix + ".type", "timer_action_type_invalid", "Action 类型必须是 command、signal、message、sound、state_variable、timer_start 或 timer_cancel。", safe(entry.type)));
            } else if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
                WebAdminActionRelayActionsService.validateTimerAction(errors, prefix, entry, type);
            }
            conditionGateBindingValidator.validate(server, errors, prefix + ".conditionGroupId", entry.conditionGroupId, targetType);
        }
    }

    private TimerStore.TimerLoadResult loadResult(MinecraftServer server) {
        TimerStore.TimerLoadResult loaded = testStorePath == null ? TimerStore.loadWithStatus(server) : TimerStore.loadWithStatus(testStorePath);
        if (testStorePath == null) {
            TimerRuntimeService.replaceDefinitionCache(server, loaded);
        }
        return loaded;
    }

    private boolean save(MinecraftServer server, TimerStore.TimerFile file) {
        boolean saved = testStorePath == null ? TimerStore.save(server, file) : TimerStore.save(testStorePath, file);
        if (saved && testStorePath == null) {
            TimerRuntimeService.replaceDefinitionCache(server, file);
        }
        return saved;
    }

    private WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            String lockId,
            String id
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_TIMER);
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
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(WebAdminEditLockService.TARGET_TIMER_CONFIG, id, lockId, user, session);
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "Timer 写入前置检查通过。");
    }

    private void releaseLockAfterWrite(String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress, String id) {
        if (editLockService != null && !safe(lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_TIMER_CONFIG, id, lockId, user, session, remoteAddress);
        }
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishConfigRealtime(TimerDefinition timer, WebAdminAuditEvent auditEvent, WebAdminUser user, String summary) {
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.TIMER_CHANGED)
                .sourceType("scheduler_timer")
                .channel(timer.outputChannel)
                .severity("INFO")
                .summary(summary)
                .routeTarget(routeTarget(timer.id))
                .payload("targetType", WebAdminEditLockService.TARGET_TIMER_CONFIG)
                .payload("targetId", timer.id)
                .payload("timerId", timer.id)
                .payload("outputChannel", timer.outputChannel)
                .payload("actor", username(user))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget(timer.id))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id()));
    }

    private void publishRuntimeRealtime(TimerDefinition timer, WebAdminAuditEvent auditEvent, WebAdminUser user, String summary) {
        WebAdminRealtimeEvent runtimeEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.TIMER_RUNTIME_CHANGED)
                .sourceType("scheduler_timer")
                .channel(timer.outputChannel)
                .severity("INFO")
                .summary(summary)
                .routeTarget(routeTarget(timer.id))
                .payload("targetType", "timer_runtime")
                .payload("targetId", timer.id)
                .payload("timerId", timer.id)
                .payload("actor", username(user))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget(timer.id))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("runtimeEventId", runtimeEvent == null ? "" : runtimeEvent.id()));
    }

    private static List<Map<String, Object>> actionMaps(List<ActionConfig> actions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActionConfig action : actions == null ? List.<ActionConfig>of() : actions) {
            if (action != null) {
                result.add(actionMap(action.normalized()));
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> actionMap(ActionConfig action) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", action.type().id());
        data.put("value", action.value());
        data.put("enabled", action.enabled());
        data.put("requiresOp", action.requiresOp());
        data.put("cooldownTicks", action.cooldownTicks());
        data.put("notifyOps", action.notifyOps());
        data.put("conditionGroupId", action.conditionGroupId());
        if (action.type() == ActionType.STATE_VARIABLE) {
            WebAdminActionRelayActionsService.putStateActionFields(data, action);
        }
        if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            WebAdminActionRelayActionsService.putTimerActionFields(data, action);
        }
        data.put("summary", actionSummary(action));
        return Map.copyOf(data);
    }

    private static String actionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String prefix = action.enabled() ? "" : "[disabled] ";
        if (action.type() == ActionType.STATE_VARIABLE) {
            return prefix + action.stateActionSummary();
        }
        if (action.type() == ActionType.TIMER_START || action.type() == ActionType.TIMER_CANCEL) {
            return prefix + action.timerActionSummary();
        }
        return prefix + actionTypeDisplayName(action.type()) + " · " + safe(action.value());
    }

    private static WebAdminWriteResult okWithData(WebAdminWriteTarget target, String message, Map<String, Object> data) {
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
                data
        );
    }

    private static WebAdminWriteResult fingerprintConflict(WebAdminWriteTarget target, TimerDefinition before, String expected) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expected);
        conflict.put("actualFingerprint", TimerStore.fingerprintFor(before));
        conflict.put("version", before == null ? 0L : before.version);
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "Timer 已被其它操作修改，请刷新后重试。",
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

    private static boolean editableEquals(TimerDefinition before, TimerDefinition after) {
        return TimerStore.editableFingerprintFor(before).equals(TimerStore.editableFingerprintFor(after));
    }

    private static Map<String, Object> requestSummary(TimerDefinition timer) {
        return timer == null ? Map.of() : TimerStore.summary(timer);
    }

    private static WebAdminWriteTarget target(String id) {
        return new WebAdminWriteTarget(TARGET_TYPE, id, "Timer");
    }

    private static String runtimeSummary(String operation, WebAdminWriteResult result) {
        if (result == null || !result.success()) {
            return "Timer runtime 操作失败。";
        }
        return switch (operation) {
            case "start" -> "Timer 已手动启动。";
            case "cancel" -> "Timer 已手动取消。";
            default -> "Timer runtime state 已重置。";
        };
    }

    private static String routeTarget(String id) {
        return "#/timers/" + encode(id);
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static boolean oneOf(String value, String... allowed) {
        String safeValue = safe(value);
        for (String item : allowed) {
            if (item.equals(safeValue)) {
                return true;
            }
        }
        return false;
    }

    private static ActionType parseActionType(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return null;
        }
        for (ActionType type : ActionType.values()) {
            if (type.id().equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    private static String actionTypeDisplayName(ActionType type) {
        if (type == null) {
            return "未知动作";
        }
        return switch (type) {
            case COMMAND -> "命令";
            case SIGNAL -> "Signal";
            case MESSAGE -> "消息";
            case SOUND -> "音效";
            case STATE_VARIABLE -> "状态变量";
            case TIMER_START -> "启动 Timer";
            case TIMER_CANCEL -> "取消 Timer";
        };
    }

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static String username(WebAdminUser user) {
        return user == null ? "" : safe(user.username);
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
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
        return value == null ? "" : value.trim();
    }
}
