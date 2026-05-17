package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinMode;
import com.zcpu.tzzmod.signal.join.SignalJoinResetPolicy;
import com.zcpu.tzzmod.signal.join.SignalJoinRuntimeService;
import com.zcpu.tzzmod.signal.join.SignalJoinScopeMode;
import com.zcpu.tzzmod.signal.join.SignalJoinStatusSnapshot;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.join.SignalJoinValidationIssue;
import com.zcpu.tzzmod.signal.join.SignalJoinValidator;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
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

public final class WebAdminSignalJoinService {
    public static final String TARGET_TYPE = "SIGNAL_JOIN";

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final Path testStorePath;

    public WebAdminSignalJoinService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null);
    }

    public WebAdminSignalJoinService(Path testStorePath) {
        this(new WebAdminPermissionService(), new WebAdminWriteSecurityService(), null, testStorePath);
    }

    WebAdminSignalJoinService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path testStorePath
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.testStorePath = testStorePath;
    }

    public Map<String, Object> list(MinecraftServer server, WebAdminUser user, WebAdminSession session) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("joins", List.of(), "permissionDenied", true, "message", permission.message());
        }
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        List<Map<String, Object>> joins = loaded.file().joins.values().stream()
                .map(join -> detailMap(server, join, user, session, false))
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getOrDefault("displayName", ""))))
                .toList();
        return Map.of(
                "joins", joins,
                "count", joins.size(),
                "worldScoped", true,
                "storeFile", SignalJoinStore.FILE_NAME,
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
        String safeId = SignalJoinStore.normalizeId(id);
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return Map.of("notFound", true, "storeDegraded", true, "message", loaded.message(), "id", safeId);
        }
        SignalJoinDefinition join = loaded.file().joins.get(safeId);
        if (join == null) {
            return Map.of("notFound", true, "message", "Signal Join 不存在或已删除。", "id", safeId);
        }
        return detailMap(server, join, user, session, true);
    }

    public Map<String, Object> status(MinecraftServer server, WebAdminUser user, String id, String scopeKey) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("permissionDenied", true, "message", permission.message());
        }
        String safeId = SignalJoinStore.normalizeId(id);
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return Map.of("notFound", true, "storeDegraded", true, "message", loaded.message(), "id", safeId);
        }
        SignalJoinDefinition join = loaded.file().joins.get(safeId);
        if (join == null) {
            return Map.of("notFound", true, "message", "Signal Join 不存在或已删除。", "id", safeId);
        }
        SignalJoinStatusSnapshot snapshot = SignalJoinRuntimeService.status(server, join, currentGameTime(server));
        return statusMap(snapshot);
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSignalJoinRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalJoinRequest safeRequest = request == null ? new WebAdminSignalJoinRequest() : request;
        SignalJoinDefinition after = definitionFromRequest(safeRequest, null);
        String id = after.id;
        WebAdminWriteTarget target = target(id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_JOIN, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        List<WebAdminValidationError> errors = validate(after, safeRequest, true);
        if (loaded.file().joins.containsKey(id)) {
            errors.add(error("id", "signal_join_id_duplicate", "Signal Join ID 已存在：" + id, id));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(after));
            return result;
        }
        SignalJoinDefinition saved = after.withWriteMetadata(username(user), 1L, true);
        loaded.file().joins.put(saved.id, saved);
        if (!save(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Signal Join 保存失败，请查看服务端日志。");
            audit(context, result, Map.of(), requestSummary(after));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "Signal Join 已创建。", Map.of("join", detailMap(server, saved, user, session, true), "routeTarget", routeTarget(saved.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), SignalJoinStore.summary(saved));
        publishRealtime(saved, auditEvent, user, "Signal Join 已创建。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, saved.id);
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminSignalJoinRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalJoinRequest safeRequest = request == null ? new WebAdminSignalJoinRequest() : request;
        safeRequest.id = SignalJoinStore.normalizeId(id);
        SignalJoinDefinition afterDraft = definitionFromRequest(safeRequest, null);
        WebAdminWriteTarget target = target(afterDraft.id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_JOIN, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, afterDraft.id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        SignalJoinDefinition before = loaded.file().joins.get(afterDraft.id);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Join 不存在或已删除。");
            audit(context, result, Map.of(), requestSummary(afterDraft));
            return result;
        }
        List<WebAdminValidationError> errors = validate(afterDraft, safeRequest, false);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, SignalJoinStore.summary(before), requestSummary(afterDraft));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        String actual = SignalJoinStore.fingerprintFor(before);
        if (expected.isBlank() || !actual.equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, SignalJoinStore.summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        SignalJoinDefinition after = afterDraft.withWriteMetadata(username(user), before.version + 1L, false);
        if (editableEquals(before, after)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Signal Join 变化。");
            audit(context, result, SignalJoinStore.summary(before), SignalJoinStore.summary(before));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
            return result;
        }
        loaded.file().joins.put(after.id, after);
        if (!save(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Signal Join 保存失败，请查看服务端日志。");
            audit(context, result, SignalJoinStore.summary(before), SignalJoinStore.summary(after));
            return result;
        }
        SignalJoinRuntimeService.clearJoin(server, after.id);
        WebAdminWriteResult result = okWithData(target, "Signal Join 已保存，pending runtime state 已清理。", Map.of("join", detailMap(server, after, user, session, true), "routeTarget", routeTarget(after.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, SignalJoinStore.summary(before), SignalJoinStore.summary(after));
        publishRealtime(after, auditEvent, user, "Signal Join 已保存。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
        return result;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminSignalJoinRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalJoinRequest safeRequest = request == null ? new WebAdminSignalJoinRequest() : request;
        String safeId = SignalJoinStore.normalizeId(id);
        WebAdminWriteTarget target = target(safeId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_JOIN, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        SignalJoinStore.SignalJoinFile file = loaded.file();
        SignalJoinDefinition before = file.joins.get(safeId);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "Signal Join 不存在或已删除。");
            audit(context, result, Map.of(), Map.of("deleted", false));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
            return result;
        }
        if (!safeRequest.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(error("confirmed", "confirmation_required", "删除前需要确认。", "false")));
            audit(context, result, SignalJoinStore.summary(before), Map.of("attempt", "delete_without_confirmation"));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        if (expected.isBlank() || !SignalJoinStore.fingerprintFor(before).equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, SignalJoinStore.summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        file.joins.remove(safeId);
        if (!save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Signal Join 删除失败，请查看服务端日志。");
            audit(context, result, SignalJoinStore.summary(before), Map.of("deleted", false));
            return result;
        }
        SignalJoinRuntimeService.clearJoin(server, safeId);
        WebAdminWriteResult result = okWithData(target, "Signal Join 已删除。", Map.of("routeTarget", "#/signal-joins"));
        WebAdminAuditEvent auditEvent = audit(context, result, SignalJoinStore.summary(before), Map.of("deleted", true));
        publishRealtime(before, auditEvent, user, "Signal Join 已删除。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
        return result;
    }

    public WebAdminWriteResult reset(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminSignalJoinRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSignalJoinRequest safeRequest = request == null ? new WebAdminSignalJoinRequest() : request;
        String safeId = SignalJoinStore.normalizeId(id);
        WebAdminWriteTarget target = target(safeId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_SIGNAL_JOIN, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        SignalJoinStore.SignalJoinLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        SignalJoinDefinition join = loaded.file().joins.get(safeId);
        if (join == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "Signal Join 不存在或已删除。");
            audit(context, result, Map.of(), Map.of("attempt", "reset_missing"));
            return result;
        }
        if (!safeRequest.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(error("confirmed", "confirmation_required", "重置 runtime state 前需要确认。", "false")));
            audit(context, result, SignalJoinStore.summary(join), Map.of("attempt", "reset_without_confirmation"));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        if (expected.isBlank() || !SignalJoinStore.fingerprintFor(join).equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, join, expected);
            audit(context, result, SignalJoinStore.summary(join), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        int resetCount = SignalJoinRuntimeService.reset(server, safeId, safeRequest.scopeKey, currentGameTime(server), "manual");
        WebAdminWriteResult result = okWithData(target, resetCount == 0 ? "Signal Join 当前没有 pending state。" : "Signal Join runtime state 已重置。", Map.of("resetCount", resetCount, "status", statusMap(SignalJoinRuntimeService.status(server, join, currentGameTime(server)))));
        WebAdminAuditEvent auditEvent = audit(context, result, SignalJoinStore.summary(join), Map.of("resetCount", resetCount, "scopeKey", safe(safeRequest.scopeKey)));
        publishRealtime(join, auditEvent, user, "Signal Join runtime state 已重置。");
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
        return result;
    }

    private Map<String, Object> detailMap(MinecraftServer server, SignalJoinDefinition raw, WebAdminUser user, WebAdminSession session, boolean includeStatus) {
        SignalJoinDefinition join = raw == null ? new SignalJoinDefinition() : raw.normalized();
        Map<String, Object> data = new LinkedHashMap<>(SignalJoinStore.summary(join));
        data.put("validationErrors", validate(join, null, false));
        data.put("lockStatus", editLockService == null
                ? Map.of("targetType", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, "targetId", join.id, "editable", true)
                : editLockService.status(WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, join.id, user, session));
        data.put("routeTarget", routeTarget(join.id));
        data.put("runtimeStatePersistent", false);
        if (includeStatus) {
            data.put("status", statusMap(SignalJoinRuntimeService.status(server, join, currentGameTime(server))));
        }
        return data;
    }

    private static Map<String, Object> statusMap(SignalJoinStatusSnapshot snapshot) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (snapshot == null) {
            return data;
        }
        data.put("joinId", snapshot.joinId());
        data.put("enabled", snapshot.enabled());
        data.put("mode", snapshot.mode());
        data.put("scopeMode", snapshot.scopeMode());
        data.put("resetPolicy", snapshot.resetPolicy());
        data.put("pendingScopeCount", snapshot.pendingScopeCount());
        data.put("lastStatusAt", snapshot.lastStatusAt());
        data.put("lastResult", snapshot.lastResult());
        data.put("lastFailureReason", snapshot.lastFailureReason());
        data.put("scopes", snapshot.scopes());
        data.put("runtimeStatePersistent", false);
        return Map.copyOf(data);
    }

    private SignalJoinDefinition definitionFromRequest(WebAdminSignalJoinRequest request, SignalJoinDefinition before) {
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = SignalJoinStore.normalizeId(request == null ? "" : request.id);
        join.displayName = safe(request == null ? "" : request.displayName);
        join.note = safe(request == null ? "" : request.note);
        join.enabled = request == null || request.enabled;
        join.inputChannels = request == null || request.inputChannels == null ? List.of() : normalizeInputRequests(request.inputChannels);
        join.outputChannel = safe(request == null ? "" : request.outputChannel);
        join.mode = SignalJoinMode.parse(request == null ? "" : request.mode);
        join.threshold = request == null ? 2 : request.threshold;
        join.scopeMode = SignalJoinScopeMode.parse(request == null ? "" : request.scopeMode);
        join.resetPolicy = SignalJoinResetPolicy.parse(request == null ? "" : request.resetPolicy);
        join.timeoutTicks = request == null ? 0L : request.timeoutTicks;
        join.cooldownTicks = request == null ? 0L : request.cooldownTicks;
        join.createdAt = before == null ? "" : before.createdAt;
        join.updatedAt = before == null ? "" : before.updatedAt;
        join.updatedBy = before == null ? "" : before.updatedBy;
        join.version = before == null ? 0L : before.version;
        return join;
    }

    private static List<SignalJoinInputDefinition> normalizeInputRequests(List<SignalJoinInputDefinition> inputs) {
        List<SignalJoinInputDefinition> result = new ArrayList<>();
        for (SignalJoinInputDefinition input : inputs) {
            if (input != null) {
                result.add(input.normalized());
            }
        }
        return List.copyOf(result);
    }

    private List<WebAdminValidationError> validate(SignalJoinDefinition join, WebAdminSignalJoinRequest request, boolean creating) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        validateRawRequest(request, errors);
        for (SignalJoinValidationIssue issue : SignalJoinValidator.validate(join, creating)) {
            errors.add(error(issue.field(), issue.code(), issue.message(), issue.rejectedValue()));
        }
        return List.copyOf(errors);
    }

    private static void validateRawRequest(WebAdminSignalJoinRequest request, List<WebAdminValidationError> errors) {
        if (request == null) {
            return;
        }
        if (!oneOf(request.mode, "ALL", "ANY_N", "COUNT")) {
            errors.add(error("mode", "signal_join_mode_invalid", "模式必须是 ALL、ANY_N 或 COUNT。", safe(request.mode)));
        }
        if (!oneOf(request.scopeMode, "GLOBAL", "PLAYER")) {
            errors.add(error("scopeMode", "signal_join_scope_invalid", "作用域必须选择 GLOBAL 或 PLAYER。", safe(request.scopeMode)));
        }
        if (!oneOf(request.resetPolicy, "RESET_AFTER_EMIT", "LATCH_UNTIL_MANUAL_RESET")) {
            errors.add(error("resetPolicy", "signal_join_reset_policy_invalid", "重置策略必须是 RESET_AFTER_EMIT 或 LATCH_UNTIL_MANUAL_RESET。", safe(request.resetPolicy)));
        }
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

    private SignalJoinStore.SignalJoinFile load(MinecraftServer server) {
        return loadResult(server).file();
    }

    private SignalJoinStore.SignalJoinLoadResult loadResult(MinecraftServer server) {
        return testStorePath == null ? SignalJoinStore.loadWithStatus(server) : SignalJoinStore.loadWithStatus(testStorePath);
    }

    private boolean save(MinecraftServer server, SignalJoinStore.SignalJoinFile file) {
        return testStorePath == null ? SignalJoinStore.save(server, file) : SignalJoinStore.save(testStorePath, file);
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
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_SIGNAL_JOIN);
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
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, id, lockId, user, session);
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "Signal Join 写入前置检查通过。");
    }

    private void releaseLockAfterWrite(String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress, String id) {
        if (editLockService != null && !safe(lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, id, lockId, user, session, remoteAddress);
        }
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(SignalJoinDefinition join, WebAdminAuditEvent auditEvent, WebAdminUser user, String summary) {
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_JOIN_CHANGED)
                .sourceType("signal_join")
                .channel(join.outputChannel)
                .severity("INFO")
                .summary(summary)
                .routeTarget(routeTarget(join.id))
                .payload("targetType", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG)
                .payload("targetId", join.id)
                .payload("signalJoinId", join.id)
                .payload("outputChannel", join.outputChannel)
                .payload("inputChannels", String.join(",", join.inputChannelNames()))
                .payload("actor", username(user))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget(join.id))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id()));
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

    private static WebAdminWriteResult fingerprintConflict(WebAdminWriteTarget target, SignalJoinDefinition before, String expected) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expected);
        conflict.put("actualFingerprint", SignalJoinStore.fingerprintFor(before));
        conflict.put("version", before == null ? 0L : before.version);
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "Signal Join 已被其它操作修改，请刷新后重试。",
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

    private static boolean editableEquals(SignalJoinDefinition before, SignalJoinDefinition after) {
        if (before == null || after == null) {
            return false;
        }
        SignalJoinDefinition left = before.normalized();
        SignalJoinDefinition right = after.normalized();
        return safe(left.displayName).equals(safe(right.displayName))
                && safe(left.note).equals(safe(right.note))
                && left.enabled == right.enabled
                && safe(left.outputChannel).equals(safe(right.outputChannel))
                && left.mode == right.mode
                && left.threshold == right.threshold
                && left.scopeMode == right.scopeMode
                && left.resetPolicy == right.resetPolicy
                && left.timeoutTicks == right.timeoutTicks
                && left.cooldownTicks == right.cooldownTicks
                && inputEquals(left.inputChannels, right.inputChannels);
    }

    private static boolean inputEquals(List<SignalJoinInputDefinition> left, List<SignalJoinInputDefinition> right) {
        List<SignalJoinInputDefinition> leftInputs = left == null ? List.of() : left;
        List<SignalJoinInputDefinition> rightInputs = right == null ? List.of() : right;
        if (leftInputs.size() != rightInputs.size()) {
            return false;
        }
        for (int i = 0; i < leftInputs.size(); i++) {
            SignalJoinInputDefinition a = leftInputs.get(i).normalized();
            SignalJoinInputDefinition b = rightInputs.get(i).normalized();
            if (!safe(a.channel).equals(safe(b.channel))
                    || !safe(a.displayName).equals(safe(b.displayName))
                    || !safe(a.note).equals(safe(b.note))
                    || a.requiredCount != b.requiredCount) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> requestSummary(SignalJoinDefinition join) {
        return join == null ? Map.of() : SignalJoinStore.summary(join);
    }

    private static WebAdminWriteTarget target(String id) {
        return new WebAdminWriteTarget(TARGET_TYPE, id, "Signal Join");
    }

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static String routeTarget(String id) {
        return "#/signal-joins/" + encode(id);
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
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
