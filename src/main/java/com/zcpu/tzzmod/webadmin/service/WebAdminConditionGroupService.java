package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationTrace;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionValidationIssue;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupPreviewRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupRequest;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminConditionGroupService {
    public static final String TARGET_TYPE = "CONDITION_GROUP";
    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final Path testStorePath;
    private final ConditionEvaluator evaluator;

    public WebAdminConditionGroupService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null, new ConditionEvaluator());
    }

    public WebAdminConditionGroupService(Path testStorePath) {
        this(new WebAdminPermissionService(), new WebAdminWriteSecurityService(), null, testStorePath, new ConditionEvaluator());
    }

    WebAdminConditionGroupService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path testStorePath,
            ConditionEvaluator evaluator
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.testStorePath = testStorePath;
        this.evaluator = evaluator == null ? new ConditionEvaluator() : evaluator;
    }

    public Map<String, Object> list(MinecraftServer server, WebAdminUser user, WebAdminSession session) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed()) {
            return Map.of("groups", List.of(), "permissionDenied", true, "message", decision.message());
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        WebAdminConditionGroupStore.ConditionGroupFile file = loaded.file();
        List<Map<String, Object>> groups = file.groups.values().stream()
                .map(entry -> detailMap(entry, user, session, false))
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getOrDefault("displayName", ""))))
                .toList();
        return Map.of(
                "groups", groups,
                "count", groups.size(),
                "worldScoped", true,
                "storeFile", WebAdminConditionGroupStore.FILE_NAME,
                "storeDegraded", loaded.degraded(),
                "storeMessage", loaded.message()
        );
    }

    public Map<String, Object> detail(MinecraftServer server, WebAdminUser user, WebAdminSession session, String id) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed()) {
            return Map.of("permissionDenied", true, "message", decision.message());
        }
        String safeId = WebAdminConditionGroupStore.normalizeId(id);
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return Map.of("notFound", true, "storeDegraded", true, "message", loaded.message(), "id", safeId);
        }
        WebAdminConditionGroupStore.ConditionGroupEntry entry = loaded.file().groups.get(safeId);
        if (entry == null) {
            return Map.of("notFound", true, "message", "条件组不存在或已删除。", "id", safeId);
        }
        return detailMap(entry, user, session, true);
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminConditionGroupRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminConditionGroupRequest safeRequest = request == null ? new WebAdminConditionGroupRequest() : request;
        String id = WebAdminConditionGroupStore.normalizeId(safeRequest.id);
        WebAdminWriteTarget target = target(id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_CONDITION_GROUP, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        WebAdminConditionGroupStore.ConditionGroupFile file = loaded.file();
        List<WebAdminValidationError> errors = validateRequest(safeRequest, true);
        if (file.groups.containsKey(id)) {
            errors.add(error("id", "condition_group_id_duplicate", "条件组 ID 已存在：" + id, id));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(safeRequest, id));
            return result;
        }
        WebAdminConditionGroupStore.ConditionGroupEntry after = entryFromRequest(safeRequest, null).withWriteMetadata(username(user), 1L, true);
        file.groups.put(after.id, after);
        if (!save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "条件组保存失败，请查看服务端日志。");
            audit(context, result, Map.of(), requestSummary(safeRequest, id));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "条件组已创建。", Map.of("group", detailMap(after, user, session, true), "routeTarget", routeTarget(after.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), summary(after));
        publishRealtime(after, auditEvent, user);
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminConditionGroupRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminConditionGroupRequest safeRequest = request == null ? new WebAdminConditionGroupRequest() : request;
        safeRequest.id = WebAdminConditionGroupStore.normalizeId(id);
        WebAdminWriteTarget target = target(safeRequest.id);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_CONDITION_GROUP, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeRequest.id);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        WebAdminConditionGroupStore.ConditionGroupFile file = loaded.file();
        WebAdminConditionGroupStore.ConditionGroupEntry before = file.groups.get(safeRequest.id);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "条件组不存在或已删除。");
            audit(context, result, Map.of(), requestSummary(safeRequest, safeRequest.id));
            return result;
        }
        List<WebAdminValidationError> errors = validateRequest(safeRequest, false);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, summary(before), requestSummary(safeRequest, safeRequest.id));
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        String actual = WebAdminConditionGroupStore.fingerprintFor(before);
        if (expected.isBlank() || !actual.equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        WebAdminConditionGroupStore.ConditionGroupEntry after = entryFromRequest(safeRequest, before).withWriteMetadata(username(user), before.version + 1L, false);
        if (editableEquals(before, after)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的条件组变化。");
            audit(context, result, summary(before), summary(before));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeRequest.id);
            return result;
        }
        file.groups.put(after.id, after);
        if (!save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "条件组保存失败，请查看服务端日志。");
            audit(context, result, summary(before), summary(after));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "条件组已保存。", Map.of("group", detailMap(after, user, session, true), "routeTarget", routeTarget(after.id)));
        WebAdminAuditEvent auditEvent = audit(context, result, summary(before), summary(after));
        publishRealtime(after, auditEvent, user);
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, after.id);
        return result;
    }

    public WebAdminWriteResult delete(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminConditionGroupRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeId = WebAdminConditionGroupStore.normalizeId(id);
        WebAdminConditionGroupRequest safeRequest = request == null ? new WebAdminConditionGroupRequest() : request;
        WebAdminWriteTarget target = target(safeId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_CONDITION_GROUP, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        WebAdminConditionGroupStore.ConditionGroupFile file = loaded.file();
        WebAdminConditionGroupStore.ConditionGroupEntry before = file.groups.get(safeId);
        if (before == null) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "条件组不存在或已删除。");
            audit(context, result, Map.of(), Map.of("deleted", false));
            releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
            return result;
        }
        String expected = safe(safeRequest.expectedFingerprint);
        String actual = WebAdminConditionGroupStore.fingerprintFor(before);
        if (expected.isBlank() || !actual.equals(expected)) {
            WebAdminWriteResult result = fingerprintConflict(target, before, expected);
            audit(context, result, summary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        file.groups.remove(safeId);
        if (!save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "条件组删除失败，请查看服务端日志。");
            audit(context, result, summary(before), Map.of("deleted", false));
            return result;
        }
        WebAdminWriteResult result = WebAdminWriteResult.ok(target, true, "条件组已删除。");
        WebAdminAuditEvent auditEvent = audit(context, result, summary(before), Map.of("deleted", true));
        publishRealtime(before, auditEvent, user);
        releaseLockAfterWrite(safeRequest.lockId, user, session, remoteAddress, safeId);
        return result;
    }

    public Map<String, Object> validate(MinecraftServer server, WebAdminUser user, String id, ConditionGroupDefinition definition) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.TEST);
        if (!decision.allowed()) {
            return Map.of("valid", false, "permissionDenied", true, "message", decision.message());
        }
        ResolvedDefinition resolved = resolveDefinition(server, id, definition);
        if (!resolved.success()) {
            return validationProblemMap(resolved.code(), resolved.message());
        }
        ConditionValidationResult result = evaluator.validate(resolved.definition());
        return validationMap(result);
    }

    public Map<String, Object> preview(MinecraftServer server, WebAdminUser user, String id, WebAdminConditionGroupPreviewRequest request) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.TEST);
        if (!decision.allowed()) {
            return Map.of("success", false, "permissionDenied", true, "message", decision.message());
        }
        ResolvedDefinition resolved = resolveDefinition(server, id, request == null ? null : request.groupDefinition);
        if (!resolved.success()) {
            return previewFailure(resolved.message(), validationProblemMap(resolved.code(), resolved.message()));
        }
        ConditionGroupDefinition definition = resolved.definition();
        ConditionValidationResult validation = evaluator.validate(definition);
        if (!validation.valid()) {
            return previewFailure("条件组未通过校验，无法测试评估。", validationMap(validation));
        }
        ConditionEvaluationContext context = previewContext(request == null ? null : request.context);
        ConditionEvaluationTrace trace = evaluator.evaluateTrace(definition, context);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("result", trace.rootResult());
        data.put("matched", trace.rootResult().matched());
        data.put("failureReason", trace.rootResult().failureReason());
        data.put("evaluatedNodeCount", trace.evaluatedNodeCount());
        data.put("evaluatedCount", trace.evaluatedNodeCount());
        data.put("durationNanos", trace.durationNanos());
        data.put("debugTree", trace.rootResult());
        data.put("contextSummary", context.summary());
        data.put("previewOnly", true);
        data.put("deferredSnapshots", List.of("item/inventory/container", "region/signal/logic-chain"));
        return data;
    }

    private ResolvedDefinition resolveDefinition(MinecraftServer server, String id, ConditionGroupDefinition supplied) {
        if (supplied != null) {
            return ResolvedDefinition.ok(supplied);
        }
        String safeId = WebAdminConditionGroupStore.normalizeId(id);
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadResult(server);
        if (loaded.degraded()) {
            return ResolvedDefinition.failed("condition_group_store_degraded", loaded.message());
        }
        WebAdminConditionGroupStore.ConditionGroupEntry entry = loaded.file().groups.get(safeId);
        if (entry == null) {
            return ResolvedDefinition.failed("condition_group_not_found", "条件组不存在或已删除：" + safeId);
        }
        if (entry.groupDefinition == null) {
            return ResolvedDefinition.failed("condition_group_definition_missing", "条件组缺少定义，无法校验或测试评估：" + safeId);
        }
        return ResolvedDefinition.ok(entry.groupDefinition);
    }

    private ConditionEvaluationContext previewContext(WebAdminConditionGroupPreviewRequest.PreviewContext request) {
        WebAdminConditionGroupPreviewRequest.PreviewContext context = request == null ? new WebAdminConditionGroupPreviewRequest.PreviewContext() : request;
        ConditionEvaluationContext.Builder builder = ConditionEvaluationContext.builder()
                .player(context.playerId, context.playerName)
                .worldId(context.world)
                .source(context.sourceType, context.sourceId)
                .channel(context.channel)
                .deviceId(context.deviceId)
                .listenerId(context.listenerId)
                .regionId(context.regionId)
                .actionId(context.actionId)
                .gameTime(context.gameTime)
                .playerTags(context.playerTags)
                .playerTeam(context.playerTeam)
                .playerGameMode(context.playerGameMode);
        if (context.playerOnline != null) {
            builder.playerOnline(context.playerOnline);
        }
        if (context.playerOp != null) {
            builder.playerOp(context.playerOp);
        }
        if (context.playerAlive != null) {
            builder.playerAlive(context.playerAlive);
        }
        if (context.eventMetadata != null) {
            context.eventMetadata.forEach(builder::eventMetadata);
        }
        if (context.variables != null) {
            context.variables.forEach(builder::variable);
        }
        StateVariableSnapshot stateSnapshot = StateVariableSnapshot.empty();
        if (context.stateVariables != null) {
            for (WebAdminConditionGroupPreviewRequest.StateVariableInput input : context.stateVariables) {
                if (input == null) {
                    continue;
                }
                try {
                    StateVariableScope scope = input.scope == null ? StateVariableScope.GLOBAL : input.scope;
                    StateVariableType type = input.type == null ? StateVariableType.STRING : input.type;
                    StateVariableRecord record = StateVariableRecord.create(
                            scope,
                            input.targetId,
                            input.key,
                            type,
                            input.value,
                            input.displayName,
                            input.note,
                            Instant.now().toEpochMilli(),
                            "preview",
                            1L
                    );
                    stateSnapshot = stateSnapshot.with(record);
                } catch (RuntimeException ignored) {
                    // Invalid preview variables are ignored; condition validation still reports bad condition config.
                }
            }
        }
        builder.stateVariables(stateSnapshot);
        return builder.build();
    }

    private Map<String, Object> validationMap(ConditionValidationResult result) {
        List<Map<String, Object>> issues = new ArrayList<>();
        if (result != null) {
            for (ConditionValidationIssue issue : result.issues()) {
                issues.add(Map.of(
                        "nodeId", issue.nodeId(),
                        "path", issue.path(),
                        "code", issue.code(),
                        "message", issue.message()
                ));
            }
        }
        return Map.of("valid", issues.isEmpty(), "issues", issues, "message", issues.isEmpty() ? "条件组校验通过。" : "条件组存在校验问题。");
    }

    private Map<String, Object> validationProblemMap(String code, String message) {
        String safeCode = safe(code).isBlank() ? "condition_group_validation_unavailable" : safe(code);
        String safeMessage = safe(message).isBlank() ? "条件组无法校验。" : safe(message);
        return Map.of(
                "valid", false,
                "issues", List.of(Map.of("nodeId", "", "path", "root", "code", safeCode, "message", safeMessage)),
                "message", safeMessage
        );
    }

    private Map<String, Object> previewFailure(String failureReason, Map<String, Object> validation) {
        Map<String, Object> invalid = new LinkedHashMap<>(validation == null ? Map.of() : validation);
        invalid.put("success", false);
        invalid.put("matched", false);
        invalid.put("failureReason", safe(failureReason).isBlank() ? "条件组无法测试评估。" : safe(failureReason));
        invalid.put("evaluatedNodeCount", 0);
        invalid.put("evaluatedCount", 0);
        invalid.put("previewOnly", true);
        return invalid;
    }

    private List<WebAdminValidationError> validateRequest(WebAdminConditionGroupRequest request, boolean creating) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        String id = WebAdminConditionGroupStore.normalizeId(request == null ? "" : request.id);
        if (id.isBlank()) {
            errors.add(error("id", "condition_group_id_required", "条件组 ID 不能为空。", ""));
        }
        if (safe(request == null ? "" : request.displayName).isBlank()) {
            errors.add(error("displayName", "condition_group_name_required", "条件组名称不能为空。", ""));
        }
        if (!creating && safe(request == null ? "" : request.expectedFingerprint).isBlank()) {
            errors.add(error("expectedFingerprint", "expected_fingerprint_required", "保存已有条件组需要 expectedFingerprint。", ""));
        }
        ConditionGroupDefinition definition = request == null ? null : request.groupDefinition;
        if (definition == null) {
            errors.add(error("groupDefinition", "condition_group_definition_required", "条件组定义不能为空。", ""));
        } else {
            ConditionValidationResult validation = evaluator.validate(definition);
            for (ConditionValidationIssue issue : validation.issues()) {
                errors.add(error("groupDefinition." + issue.path(), issue.code(), issue.message(), issue.nodeId()));
            }
        }
        return errors;
    }

    private WebAdminConditionGroupStore.ConditionGroupEntry entryFromRequest(WebAdminConditionGroupRequest request, WebAdminConditionGroupStore.ConditionGroupEntry before) {
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = WebAdminConditionGroupStore.normalizeId(request.id);
        entry.displayName = safe(request.displayName);
        entry.note = safe(request.note);
        entry.iconKey = safe(request.iconKey).isBlank() ? "doctor-overview" : safe(request.iconKey);
        entry.enabled = request.enabled;
        entry.tags = request.tags == null ? List.of() : List.copyOf(request.tags);
        entry.groupDefinition = request.groupDefinition;
        entry.createdAt = before == null ? "" : before.createdAt;
        entry.updatedAt = before == null ? "" : before.updatedAt;
        entry.updatedBy = before == null ? "" : before.updatedBy;
        entry.version = before == null ? 0L : before.version;
        return WebAdminConditionGroupStore.ConditionGroupEntry.normalized(entry.id, entry);
    }

    private Map<String, Object> detailMap(WebAdminConditionGroupStore.ConditionGroupEntry entry, WebAdminUser user, WebAdminSession session, boolean includeDefinition) {
        WebAdminConditionGroupStore.ConditionGroupEntry normalized = WebAdminConditionGroupStore.ConditionGroupEntry.normalized(entry == null ? "" : entry.id, entry);
        Map<String, Object> data = new LinkedHashMap<>(normalized.summary());
        data.put("lockStatus", editLockService == null
                ? Map.of("targetType", WebAdminEditLockService.TARGET_CONDITION_GROUP, "targetId", normalized.id, "editable", true)
                : editLockService.status(WebAdminEditLockService.TARGET_CONDITION_GROUP, normalized.id, user, session));
        data.put("routeTarget", routeTarget(normalized.id));
        if (includeDefinition) {
            data.put("groupDefinition", normalized.groupDefinition);
            data.put("validation", validationMap(evaluator.validate(normalized.groupDefinition)));
        }
        return data;
    }

    private WebAdminConditionGroupStore.ConditionGroupFile load(MinecraftServer server) {
        return loadResult(server).file();
    }

    private WebAdminConditionGroupStore.ConditionGroupLoadResult loadResult(MinecraftServer server) {
        return testStorePath == null ? WebAdminConditionGroupStore.loadWithStatus(server) : WebAdminConditionGroupStore.loadWithStatus(testStorePath);
    }

    private boolean save(MinecraftServer server, WebAdminConditionGroupStore.ConditionGroupFile file) {
        return testStorePath == null ? WebAdminConditionGroupStore.save(server, file) : WebAdminConditionGroupStore.save(testStorePath, file);
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
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_CONDITION_GROUP);
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
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(WebAdminEditLockService.TARGET_CONDITION_GROUP, id, lockId, user, session);
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "写入前置检查通过。");
    }

    private void releaseLockAfterWrite(String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress, String id) {
        if (editLockService != null && !safe(lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_CONDITION_GROUP, id, lockId, user, session, remoteAddress);
        }
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(WebAdminConditionGroupStore.ConditionGroupEntry entry, WebAdminAuditEvent auditEvent, WebAdminUser user) {
        WebAdminRealtimeEvent event = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONDITION_GROUP_CHANGED)
                .sourceType("condition_group")
                .severity("INFO")
                .summary("条件组已变化：" + (safe(entry.displayName).isBlank() ? entry.id : entry.displayName))
                .routeTarget(routeTarget(entry.id))
                .payload("conditionGroupId", entry.id)
                .payload("enabled", entry.enabled)
                .payload("actor", username(user))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
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

    private static WebAdminWriteResult fingerprintConflict(WebAdminWriteTarget target, WebAdminConditionGroupStore.ConditionGroupEntry before, String expected) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expected);
        conflict.put("actualFingerprint", WebAdminConditionGroupStore.fingerprintFor(before));
        conflict.put("version", before.version);
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "条件组已被其它操作修改，请刷新后重试。",
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

    private static boolean editableEquals(WebAdminConditionGroupStore.ConditionGroupEntry before, WebAdminConditionGroupStore.ConditionGroupEntry after) {
        return before != null && after != null
                && safe(before.displayName).equals(safe(after.displayName))
                && safe(before.note).equals(safe(after.note))
                && safe(before.iconKey).equals(safe(after.iconKey))
                && before.enabled == after.enabled
                && before.tags.equals(after.tags)
                && before.groupDefinition.stableFingerprint().equals(after.groupDefinition.stableFingerprint());
    }

    private static Map<String, Object> summary(WebAdminConditionGroupStore.ConditionGroupEntry entry) {
        return entry == null ? Map.of() : entry.summary();
    }

    private static Map<String, Object> requestSummary(WebAdminConditionGroupRequest request, String id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        if (request != null) {
            data.put("displayName", safe(request.displayName));
            data.put("enabled", request.enabled);
            data.put("nodeCount", WebAdminConditionGroupStore.countNodes(request.groupDefinition == null ? null : request.groupDefinition.root()));
        }
        return data;
    }

    private static WebAdminWriteTarget target(String id) {
        return new WebAdminWriteTarget(TARGET_TYPE, id, "条件组");
    }

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static String routeTarget(String id) {
        return "#/condition-groups/" + encode(id);
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String username(WebAdminUser user) {
        return user == null ? "" : safe(user.username);
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

    private record ResolvedDefinition(boolean success, ConditionGroupDefinition definition, String code, String message) {
        private static ResolvedDefinition ok(ConditionGroupDefinition definition) {
            return new ResolvedDefinition(true, definition, "", "");
        }

        private static ResolvedDefinition failed(String code, String message) {
            return new ResolvedDefinition(false, null, code, message);
        }
    }
}
