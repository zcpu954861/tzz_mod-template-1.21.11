package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.state.StateVariableKey;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableService;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableUpdateRequest;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.condition.state.StateVariableWriteResult;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminStateVariableWriteRequest;
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
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminStateVariableService {
    public static final String TARGET_TYPE = "STATE_VARIABLE";
    public static final String CREATE_LOCK_TARGET_ID = "new";
    public static final String CREATE_EXPECTED_FINGERPRINT = "state_variable_create_v1";

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final Path testStorePath;

    public WebAdminStateVariableService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null);
    }

    public WebAdminStateVariableService(WebAdminPermissionService permissionService) {
        this(permissionService, new WebAdminWriteSecurityService(), null, null);
    }

    public WebAdminStateVariableService(Path testStorePath) {
        this(new WebAdminPermissionService(), new WebAdminWriteSecurityService(), null, testStorePath);
    }

    WebAdminStateVariableService(
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

    public WebAdminDtos.StateVariableListDto list(MinecraftServer server, WebAdminUser user, Map<String, String> query) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed()) {
            return emptyList(false, decision.message());
        }
        StateVariableStore.StateVariableLoadResult loaded = load(server);
        List<WebAdminDtos.StateVariableListEntryDto> variables = loaded.snapshot().records().stream()
                .filter(this::supportedScope)
                .filter(record -> matches(record, query))
                .sorted(Comparator.comparing(StateVariableRecord::updatedAt).reversed()
                        .thenComparing(StateVariableRecord::id))
                .limit(limit(query))
                .map(this::entry)
                .toList();
        return new WebAdminDtos.StateVariableListDto(
                variables,
                variables.size(),
                summary(loaded.snapshot()),
                true,
                StateVariableStore.FILE_NAME,
                loaded.filePresent(),
                loaded.degraded(),
                loaded.message(),
                false,
                List.of(StateVariableScope.GLOBAL.name(), StateVariableScope.PLAYER.name()),
                List.of(StateVariableType.BOOLEAN.name(), StateVariableType.INTEGER.name(), StateVariableType.STRING.name())
        );
    }

    public WebAdminDtos.StateVariableDetailDto detail(MinecraftServer server, WebAdminUser user, String id) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed() || id == null || id.isBlank()) {
            return null;
        }
        return load(server).snapshot().records().stream()
                .filter(this::supportedScope)
                .filter(record -> record.id().equals(id))
                .findFirst()
                .map(this::detail)
                .orElse(null);
    }

    public WebAdminWriteResult create(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminStateVariableWriteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminStateVariableWriteRequest safeRequest = request == null ? new WebAdminStateVariableWriteRequest() : request;
        ParsedRequest parsed = parse(safeRequest);
        WebAdminWriteTarget target = parsed.id().isBlank() ? target(CREATE_LOCK_TARGET_ID, "新状态变量") : target(parsed.id(), parsed.displayName());
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_STATE_VARIABLE, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, CREATE_LOCK_TARGET_ID);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "create_preflight_failed"));
            return preflight;
        }
        StateVariableStore.StateVariableLoadResult loaded = load(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), requestSummary(parsed));
            return result;
        }
        List<WebAdminValidationError> errors = new ArrayList<>(parsed.errors());
        if (!CREATE_EXPECTED_FINGERPRINT.equals(safe(safeRequest.expectedFingerprint))) {
            errors.add(error("expectedFingerprint", "state_variable_create_fingerprint_required", "创建状态变量需要 create fingerprint，用于保持 WebAdmin 写入边界一致。", safe(safeRequest.expectedFingerprint)));
        }
        if (!parsed.id().isBlank() && findById(loaded.snapshot(), parsed.id()) != null) {
            errors.add(error("key", "state_variable_duplicate", "状态变量已存在：" + displayPath(parsed), parsed.key()));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, Map.of(), requestSummary(parsed));
            return result;
        }

        StateVariableWriteResult write = service(server).set(parsed.updateRequest(""), username(user));
        WebAdminWriteResult result = toWriteResult(target, write, user);
        audit(context, result, Map.of(), summary(write.record()));
        if (result.success() && result.changed()) {
            publishRealtime(write.record(), user);
            releaseLockAfterWrite(CREATE_LOCK_TARGET_ID, safeRequest.lockId, user, session, remoteAddress);
        }
        return result;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String id,
            WebAdminStateVariableWriteRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeId = safe(id);
        WebAdminStateVariableWriteRequest safeRequest = request == null ? new WebAdminStateVariableWriteRequest() : request;
        ParsedRequest parsed = parse(safeRequest);
        WebAdminWriteTarget target = target(safeId, parsed.displayName());
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_STATE_VARIABLE, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, target, safeRequest.lockId, safeId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "update_preflight_failed"));
            return preflight;
        }
        StateVariableStore.StateVariableLoadResult loaded = load(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), requestSummary(parsed));
            return result;
        }
        StateVariableRecord before = findById(loaded.snapshot(), safeId);
        List<WebAdminValidationError> errors = new ArrayList<>(parsed.errors());
        if (before == null) {
            errors.add(error("id", "state_variable_not_found", "状态变量不存在或已删除。", safeId));
        }
        if (safe(safeRequest.expectedFingerprint).isBlank()) {
            errors.add(error("expectedFingerprint", "required", "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。", ""));
        }
        if (!parsed.id().isBlank() && !safeId.equals(parsed.id())) {
            errors.add(error("key", "state_variable_identity_immutable", "状态变量 scope / targetId / key 组成稳定 ID，已有变量暂不支持重命名。请新建变量后调整引用。", displayPath(parsed)));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, summary(before), requestSummary(parsed));
            return result;
        }

        StateVariableWriteResult write = service(server).set(parsed.updateRequest(safeRequest.expectedFingerprint), username(user));
        WebAdminWriteResult result = toWriteResult(target, write, user);
        audit(context, result, summary(before), summary(write.record()));
        if (result.success()) {
            if (result.changed()) {
                publishRealtime(write.record(), user);
            }
            releaseLockAfterWrite(safeId, safeRequest.lockId, user, session, remoteAddress);
        }
        return result;
    }

    private WebAdminWriteResult toWriteResult(WebAdminWriteTarget target, StateVariableWriteResult write, WebAdminUser user) {
        if (write == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "状态变量写入失败。");
        }
        if (!write.success()) {
            if ("fingerprint_mismatch".equals(write.code())) {
                Map<String, Object> conflict = new LinkedHashMap<>();
                conflict.put("currentFingerprint", write.currentFingerprint());
                conflict.put("current", summary(write.record()));
                return new WebAdminWriteResult(
                        false,
                        WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                        write.message(),
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
            if (!write.validationErrors().isEmpty()) {
                return WebAdminWriteResult.validationFailed(
                        target,
                        write.validationErrors().stream()
                                .map(message -> error("stateVariable", "state_variable_validation", message, ""))
                                .toList()
                );
            }
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, write.message());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("variable", write.record() == null ? Map.of() : detail(write.record()));
        data.put("routeTarget", write.record() == null ? "" : routeTarget(write.record().id()));
        data.put("actor", username(user));
        if (!write.changed()) {
            return new WebAdminWriteResult(
                    true,
                    WebAdminWriteResultCode.NO_CHANGE.id(),
                    write.message(),
                    target.targetType(),
                    target.targetId(),
                    false,
                    List.of(),
                    "",
                    "",
                    false,
                    Map.of(),
                    data
            );
        }
        return new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                write.message(),
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

    private ParsedRequest parse(WebAdminStateVariableWriteRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        StateVariableScope scope = StateVariableScope.parse(request == null ? "" : request.scope).orElse(null);
        StateVariableType type = StateVariableType.parse(request == null ? "" : request.type).orElse(null);
        if (scope == null) {
            errors.add(error("scope", "invalid_scope", "状态变量 scope 只支持 GLOBAL / PLAYER。", safe(request == null ? "" : request.scope)));
            scope = StateVariableScope.GLOBAL;
        }
        if (type == null) {
            errors.add(error("type", "invalid_type", "状态变量 type 只支持 BOOLEAN / INTEGER / STRING。", safe(request == null ? "" : request.type)));
            type = StateVariableType.STRING;
        }
        StateVariableUpdateRequest update = new StateVariableUpdateRequest(
                scope,
                safe(request == null ? "" : request.targetId),
                safe(request == null ? "" : request.key),
                type,
                safe(request == null ? "" : request.value),
                safe(request == null ? "" : request.displayName),
                safe(request == null ? "" : request.note),
                ""
        );
        for (StateVariableValidation.Issue issue : StateVariableValidation.validateUpdate(update)) {
            errors.add(error(fieldFor(issue.code()), issue.code(), issue.message(), ""));
        }
        String id = new StateVariableKey(scope, update.targetId(), update.key()).stableId();
        return new ParsedRequest(update, id, errors);
    }

    private StateVariableStore.StateVariableLoadResult load(MinecraftServer server) {
        if (testStorePath != null) {
            return new StateVariableService(testStorePath).snapshotWithStatus();
        }
        return StateVariableStore.getSnapshotWithStatus(server);
    }

    private StateVariableRecord findById(StateVariableSnapshot snapshot, String id) {
        if (snapshot == null || safe(id).isBlank()) {
            return null;
        }
        return snapshot.records().stream()
                .filter(record -> record != null && safe(id).equals(record.id()))
                .findFirst()
                .orElse(null);
    }

    private StateVariableService service(MinecraftServer server) {
        return new StateVariableService(testStorePath == null ? StateVariableStore.path(server) : testStorePath);
    }

    private WebAdminDtos.StateVariableListDto emptyList(boolean storePresent, String message) {
        return new WebAdminDtos.StateVariableListDto(
                List.of(),
                0,
                new WebAdminDtos.StateVariableSummaryDto(0, 0, 0, 0, 0, 0),
                true,
                StateVariableStore.FILE_NAME,
                storePresent,
                false,
                safe(message),
                false,
                List.of(StateVariableScope.GLOBAL.name(), StateVariableScope.PLAYER.name()),
                List.of(StateVariableType.BOOLEAN.name(), StateVariableType.INTEGER.name(), StateVariableType.STRING.name())
        );
    }

    private WebAdminDtos.StateVariableSummaryDto summary(StateVariableSnapshot snapshot) {
        List<StateVariableRecord> records = snapshot == null ? List.of() : snapshot.records().stream()
                .filter(this::supportedScope)
                .toList();
        return new WebAdminDtos.StateVariableSummaryDto(
                records.size(),
                count(records, record -> record.scope() == StateVariableScope.GLOBAL),
                count(records, record -> record.scope() == StateVariableScope.PLAYER),
                count(records, record -> record.type() == StateVariableType.BOOLEAN),
                count(records, record -> record.type() == StateVariableType.INTEGER),
                count(records, record -> record.type() == StateVariableType.STRING)
        );
    }

    private WebAdminDtos.StateVariableListEntryDto entry(StateVariableRecord record) {
        return new WebAdminDtos.StateVariableListEntryDto(
                record.id(),
                record.scope().name(),
                record.scope().displayName(),
                record.targetId(),
                targetLabel(record),
                record.key(),
                record.type().name(),
                record.type().displayName(),
                typedValue(record),
                record.value(),
                valuePreview(record.value()),
                record.value().length(),
                record.displayName(),
                record.note(),
                record.version(),
                record.fingerprint(),
                shortFingerprint(record.fingerprint()),
                WebAdminReadonlySupport.isoTime(record.updatedAt()),
                record.updatedBy(),
                new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath(),
                routeTarget(record.id())
        );
    }

    private WebAdminDtos.StateVariableDetailDto detail(StateVariableRecord record) {
        WebAdminDtos.StateVariableListEntryDto entry = entry(record);
        Map<String, String> copyTargets = new LinkedHashMap<>();
        copyTargets.put("key", record.key());
        copyTargets.put("targetId", record.targetId());
        copyTargets.put("displayPath", entry.displayPath());
        return new WebAdminDtos.StateVariableDetailDto(
                entry.id(),
                entry.scope(),
                entry.scopeLabel(),
                entry.targetId(),
                entry.targetLabel(),
                entry.key(),
                entry.type(),
                entry.typeLabel(),
                entry.value(),
                entry.valueText(),
                entry.valuePreview(),
                entry.valueLength(),
                entry.displayName(),
                entry.note(),
                entry.version(),
                entry.fingerprint(),
                entry.fingerprintShort(),
                entry.updatedAt(),
                entry.updatedBy(),
                "",
                entry.displayPath(),
                "world/tzz/webadmin/" + StateVariableStore.FILE_NAME,
                false,
                copyTargets,
                conditionSuggestion(record)
        );
    }

    private Map<String, Object> conditionSuggestion(StateVariableRecord record) {
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("scope", record.scope().name());
        suggestion.put("targetMode", record.scope() == StateVariableScope.GLOBAL ? "global" : "explicit_target");
        suggestion.put("targetId", record.targetId());
        suggestion.put("key", record.key());
        suggestion.put("type", switch (record.type()) {
            case BOOLEAN -> "state_variable_bool_equals";
            case INTEGER -> "state_variable_int_compare";
            case STRING -> "state_variable_string_equals";
        });
        return suggestion;
    }

    private boolean matches(StateVariableRecord record, Map<String, String> query) {
        String scope = queryValue(query, "scope").toUpperCase(Locale.ROOT);
        if (!scope.isBlank() && !"ALL".equals(scope) && !record.scope().name().equals(scope)) {
            return false;
        }
        String type = queryValue(query, "type").toUpperCase(Locale.ROOT);
        if (!type.isBlank() && !"ALL".equals(type) && !record.type().name().equals(type)) {
            return false;
        }
        String target = firstNonBlank(queryValue(query, "targetId"), queryValue(query, "target")).toLowerCase(Locale.ROOT);
        if (!target.isBlank() && !record.targetId().toLowerCase(Locale.ROOT).contains(target)) {
            return false;
        }
        String q = firstNonBlank(queryValue(query, "q"), queryValue(query, "search")).toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return true;
        }
        String haystack = String.join(" ",
                record.id(),
                record.scope().name(),
                record.targetId(),
                record.key(),
                record.displayName(),
                record.note(),
                record.type().name(),
                record.value(),
                new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath()
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(q);
    }

    private int limit(Map<String, String> query) {
        String raw = queryValue(query, "limit");
        if (raw.isBlank()) {
            return WebAdminReadonlySupport.MAX_LIST_LIMIT;
        }
        try {
            return WebAdminReadonlySupport.limit(Integer.parseInt(raw.trim()), WebAdminReadonlySupport.MAX_LIST_LIMIT);
        } catch (NumberFormatException ignored) {
            return WebAdminReadonlySupport.MAX_LIST_LIMIT;
        }
    }

    private boolean supportedScope(StateVariableRecord record) {
        return record != null && (record.scope() == StateVariableScope.GLOBAL || record.scope() == StateVariableScope.PLAYER);
    }

    private Object typedValue(StateVariableRecord record) {
        return switch (record.type()) {
            case BOOLEAN -> Boolean.parseBoolean(record.value());
            case INTEGER -> {
                try {
                    yield Long.parseLong(record.value());
                } catch (NumberFormatException ignored) {
                    yield record.value();
                }
            }
            case STRING -> record.value();
        };
    }

    private WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target,
            String lockId,
            String lockTargetId
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_STATE_VARIABLE);
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
                    WebAdminEditLockService.TARGET_STATE_VARIABLE,
                    lockTargetId,
                    lockId,
                    user,
                    session
            );
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "写入前置检查通过。");
    }

    private void releaseLockAfterWrite(String targetId, String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress) {
        if (editLockService != null && !safe(lockId).isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_STATE_VARIABLE, targetId, lockId, user, session, remoteAddress);
        }
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(StateVariableRecord record, WebAdminUser user) {
        if (record == null) {
            return;
        }
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .sourceType("state_variable")
                .severity("INFO")
                .summary("状态变量定义已变化：" + displayName(record))
                .routeTarget(routeTarget(record.id()))
                .payload("targetType", "state_variable_definition")
                .payload("stateVariableId", record.id())
                .payload("scope", record.scope().name())
                .payload("key", record.key())
                .payload("actor", username(user)));
        WebAdminRealtimeEvent stateEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.STATE_VARIABLE_CHANGED)
                .sourceType("state_variable")
                .severity("INFO")
                .summary("状态变量定义已变化：" + displayName(record))
                .routeTarget(routeTarget(record.id()))
                .payload("stateVariableId", record.id())
                .payload("displayPath", new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath())
                .payload("type", record.type().name())
                .payload("actor", username(user)));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .sourceType("state_variable")
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget(record.id()))
                .payload("targetType", "state_variable_definition")
                .payload("stateVariableId", record.id())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("stateEventId", stateEvent == null ? "" : stateEvent.id()));
    }

    private Map<String, Object> summary(StateVariableRecord record) {
        if (record == null) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.id());
        data.put("scope", record.scope().name());
        data.put("targetId", record.targetId());
        data.put("key", record.key());
        data.put("type", record.type().name());
        data.put("value", record.value());
        data.put("displayName", record.displayName());
        data.put("note", record.note());
        data.put("version", record.version());
        data.put("fingerprint", record.fingerprint());
        return data;
    }

    private Map<String, Object> requestSummary(ParsedRequest parsed) {
        if (parsed == null) {
            return Map.of();
        }
        return Map.of(
                "id", parsed.id(),
                "scope", parsed.request().scope().name(),
                "targetId", parsed.request().targetId(),
                "key", parsed.request().key(),
                "type", parsed.request().type().name(),
                "displayName", parsed.request().displayName()
        );
    }

    private static String displayPath(ParsedRequest parsed) {
        return parsed == null ? "" : new StateVariableKey(parsed.request().scope(), parsed.request().targetId(), parsed.request().key()).displayPath();
    }

    private static String displayName(StateVariableRecord record) {
        if (record == null) {
            return "";
        }
        String name = safe(record.displayName());
        return name.isBlank() ? new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath() : name;
    }

    private static String valuePreview(String value) {
        String safe = safe(value).replace("\r", " ").replace("\n", " ");
        return safe.length() <= 96 ? safe : safe.substring(0, 93) + "...";
    }

    private static String targetLabel(StateVariableRecord record) {
        if (record.scope() == StateVariableScope.GLOBAL) {
            return "全局";
        }
        return record.targetId().isBlank() ? "未指定目标" : "目标 ID: " + record.targetId();
    }

    private static String shortFingerprint(String fingerprint) {
        String safe = safe(fingerprint);
        return safe.length() <= 12 ? safe : safe.substring(0, 12);
    }

    private static String routeTarget(String id) {
        return "#/state-variables/" + encode(id);
    }

    private static WebAdminWriteTarget target(String id, String displayName) {
        return new WebAdminWriteTarget(TARGET_TYPE, safe(id), safe(displayName).isBlank() ? "状态变量定义" : safe(displayName));
    }

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static String fieldFor(String code) {
        String safeCode = safe(code);
        if (safeCode.contains("scope")) return "scope";
        if (safeCode.contains("target")) return "targetId";
        if (safeCode.contains("key")) return "key";
        if (safeCode.contains("type")) return "type";
        if (safeCode.contains("value") || safeCode.contains("boolean") || safeCode.contains("integer") || safeCode.contains("string")) return "value";
        return "stateVariable";
    }

    private static String queryValue(Map<String, String> query, String key) {
        return query == null ? "" : safe(query.get(key));
    }

    private static String firstNonBlank(String first, String second) {
        return !safe(first).isBlank() ? safe(first) : safe(second);
    }

    private static int count(List<StateVariableRecord> records, java.util.function.Predicate<StateVariableRecord> predicate) {
        return (int) records.stream().filter(predicate).count();
    }

    private static String username(WebAdminUser user) {
        return user == null ? "" : safe(user.username);
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
        return value == null ? "" : value.trim();
    }

    private record ParsedRequest(StateVariableUpdateRequest request, String id, List<WebAdminValidationError> errors) {
        private ParsedRequest {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        private String displayName() {
            return request == null ? "" : request.displayName();
        }

        private String key() {
            return request == null ? "" : request.key();
        }

        private StateVariableUpdateRequest updateRequest(String expectedFingerprint) {
            return new StateVariableUpdateRequest(
                    request.scope(),
                    request.targetId(),
                    request.key(),
                    request.type(),
                    request.value(),
                    request.displayName(),
                    request.note(),
                    expectedFingerprint
            );
        }
    }
}
