package com.zcpu.tzzmod.webadmin.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerValidationIssue;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.signal.join.SignalJoinValidationIssue;
import com.zcpu.tzzmod.signal.join.SignalJoinValidator;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminLogicChainMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import com.zcpu.tzzmod.webadmin.WebAdminTemplateStore;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTemplateRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.template.WebAdminBuiltInTemplates;
import com.zcpu.tzzmod.webadmin.template.WebAdminTemplatePackage;
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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class WebAdminTemplateService {
    public static final String TARGET_TYPE = "TEMPLATE_PACKAGE";
    public static final String SOURCE_BUILT_IN = "built_in";
    public static final String SOURCE_USER = "user";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final Path templateStorePath;
    private final Path signalJoinStorePath;
    private final Path timerStorePath;
    private final Path signalListenerStorePath;
    private final Path channelMetadataStorePath;
    private final Path logicChainMetadataStorePath;

    public WebAdminTemplateService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this(permissionService, securityService, editLockService, null, null, null, null, null, null);
    }

    public WebAdminTemplateService(Path directory) {
        this(
                new WebAdminPermissionService(),
                new WebAdminWriteSecurityService(),
                null,
                directory.resolve(WebAdminTemplateStore.FILE_NAME),
                directory.resolve(SignalJoinStore.FILE_NAME),
                directory.resolve(TimerStore.FILE_NAME),
                directory.resolve(SignalListenerStore.FILE_NAME),
                directory.resolve("web_admin_channel_metadata.json"),
                directory.resolve("web_admin_logic_chain_metadata.json")
        );
    }

    public WebAdminTemplateService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService,
            Path templateStorePath,
            Path signalJoinStorePath,
            Path timerStorePath,
            Path signalListenerStorePath,
            Path channelMetadataStorePath,
            Path logicChainMetadataStorePath
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
        this.templateStorePath = templateStorePath;
        this.signalJoinStorePath = signalJoinStorePath;
        this.timerStorePath = timerStorePath;
        this.signalListenerStorePath = signalListenerStorePath;
        this.channelMetadataStorePath = channelMetadataStorePath;
        this.logicChainMetadataStorePath = logicChainMetadataStorePath;
    }

    public Map<String, Object> list(MinecraftServer server, WebAdminUser user, WebAdminSession session) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of("templates", List.of(), "permissionDenied", true, "message", permission.message());
        }
        WebAdminTemplateStore.TemplateLoadResult loaded = loadTemplates(server);
        List<Map<String, Object>> templates = new ArrayList<>();
        for (WebAdminTemplatePackage template : WebAdminBuiltInTemplates.list()) {
            templates.add(summary(template, SOURCE_BUILT_IN));
        }
        if (!loaded.degraded()) {
            for (WebAdminTemplatePackage template : loaded.file().templates.values()) {
                templates.add(summary(template, SOURCE_USER));
            }
        }
        templates.sort(Comparator.comparing(entry -> String.valueOf(entry.getOrDefault("displayName", ""))));
        return Map.of(
                "templates", templates,
                "count", templates.size(),
                "builtInCount", WebAdminBuiltInTemplates.list().size(),
                "userCount", loaded.degraded() ? 0 : loaded.file().templates.size(),
                "userTemplateStore", WebAdminTemplateStore.FILE_NAME,
                "worldScoped", true,
                "storeDegraded", loaded.degraded(),
                "storeMessage", loaded.message(),
                "storeFingerprint", WebAdminTemplateStore.fingerprintFor(loaded.file()),
                "expectedFingerprint", WebAdminTemplateStore.fingerprintFor(loaded.file())
        );
    }

    public Map<String, Object> detail(MinecraftServer server, WebAdminUser user, WebAdminSession session, String source, String templateId) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return Map.of(
                    "permissionDenied", true,
                    "code", "template_permission_denied",
                    "permissionCode", permission.code(),
                    "message", "没有权限查看该模板。"
            );
        }
        ResolvedTemplate resolved = resolve(server, source, templateId);
        if (!resolved.success()) {
            return Map.of(
                    "notFound", "template_not_found".equals(resolved.code()),
                    "code", resolved.code(),
                    "source", safe(source),
                    "templateId", safe(templateId),
                    "message", resolved.message()
            );
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(summary(resolved.template(), resolved.source()));
        data.put("template", resolved.template());
        data.put("json", exportJson(resolved.template(), resolved.source()));
        data.put("resources", resourceSummary(resolved.template()));
        data.put("parameters", resolved.template().parameters);
        data.put("placeholders", resolved.template().resources.placeholders);
        data.put("componentExportSupported", false);
        data.put("componentExportDeferredReason", "从现有 Logic Chain component 反向导出需要跨 store 依赖追踪，本阶段先支持模板 JSON 导出。");
        return data;
    }

    public Map<String, Object> exportTemplate(MinecraftServer server, WebAdminUser user, WebAdminSession session, String source, String templateId) {
        Map<String, Object> detail = detail(server, user, session, source, templateId);
        if (Boolean.TRUE.equals(detail.get("notFound")) || Boolean.TRUE.equals(detail.get("permissionDenied"))) {
            return detail;
        }
        return Map.of(
                "source", detail.get("source"),
                "templateId", detail.get("templateId"),
                "displayName", detail.get("displayName"),
                "json", detail.get("json"),
                "fingerprint", detail.get("fingerprint"),
                "downloadFileName", detail.get("templateId") + ".json"
        );
    }

    public WebAdminWriteResult previewImport(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminTemplateRequest request
    ) {
        WebAdminWriteTarget target = target("import-preview");
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        ParseResult parsed = parsePackage(request == null ? "" : request.packageJson);
        if (!parsed.success()) {
            return WebAdminWriteResult.validationFailed(target, parsed.errors());
        }
        WebAdminTemplatePackage template = withImportOverrides(parsed.template(), request).normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("template", summary(template, SOURCE_USER));
        data.put("resources", resourceSummary(template));
        data.put("json", exportJson(template, SOURCE_USER));
        data.put("importDoesNotApply", true);
        data.put("message", "导入预览只校验并准备保存为用户模板，不会应用配置。");
        return result(target, true, false, "模板 JSON 预览通过，尚未写入也不会自动应用。", data, "", "");
    }

    public WebAdminWriteResult importUserTemplate(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminTemplateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = target("user-template-store");
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.IMPORT_TEMPLATE, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.IMPORT_TEMPLATE, target, lockTargetTypeForImport(), "user-template-store", request == null ? "" : request.lockId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminTemplateStore.TemplateLoadResult loaded = loadTemplates(server);
        if (loaded.degraded()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, loaded.message());
            audit(context, result, Map.of(), Map.of("attempt", "store_degraded"));
            return result;
        }
        String expected = safe(request == null ? "" : request.expectedFingerprint);
        String actual = WebAdminTemplateStore.fingerprintFor(loaded.file());
        if (expected.isBlank() || !actual.equals(expected)) {
            WebAdminWriteResult result = conflict(target, "模板库已被其他操作修改，请刷新后重试。", expected, actual);
            audit(context, result, Map.of("storeFingerprint", actual), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        ParseResult parsed = parsePackage(request == null ? "" : request.packageJson);
        if (!parsed.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, parsed.errors());
            audit(context, result, Map.of(), Map.of("attempt", "import_validation_failed"));
            return result;
        }
        WebAdminTemplatePackage template = withImportOverrides(parsed.template(), request).normalized();
        if (loaded.file().templates.containsKey(template.templateId) || WebAdminBuiltInTemplates.find(template.templateId) != null) {
            WebAdminValidationError error = error("templateId", "template_id_duplicate", "模板 ID 已存在，请换一个用户模板 ID。", template.templateId);
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(error));
            audit(context, result, Map.of(), summary(template, SOURCE_USER));
            return result;
        }
        loaded.file().templates.put(template.templateId, template);
        if (!saveTemplates(server, loaded.file())) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "模板库保存失败，请查看服务端日志。");
            audit(context, result, Map.of(), summary(template, SOURCE_USER));
            return result;
        }
        WebAdminTemplateStore.TemplateFile afterFile = loadTemplates(server).file();
        WebAdminWriteResult result = result(target, true, true, "模板已导入为用户模板，尚未应用配置。", Map.of(
                "template", summary(template, SOURCE_USER),
                "storeFingerprint", WebAdminTemplateStore.fingerprintFor(afterFile),
                "importDoesNotApply", true
        ), "", "");
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of("storeFingerprint", actual), summary(template, SOURCE_USER));
        WebAdminRealtimeEvent event = publishTemplateStoreChanged(template, auditEvent, user);
        releaseLockAfterWrite(lockTargetTypeForImport(), "user-template-store", request == null ? "" : request.lockId, user, session, remoteAddress);
        return withAuditAndRealtime(result, auditEvent, event);
    }

    public WebAdminWriteResult dryRunApply(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminTemplateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = target(safe(request == null ? "" : request.templateId));
        WebAdminWriteResult readGate = readWritePreviewPreflight(user, session, csrfToken, sameOrigin, target);
        if (!readGate.success()) {
            return readGate;
        }
        ApplyPlan plan = buildApplyPlan(server, request);
        if (!plan.resolved.success()) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, plan.resolved.message());
        }
        Map<String, Object> data = plan.toMap();
        data.put("dryRunReadOnly", true);
        data.put("expectedFingerprint", plan.planFingerprint);
        return result(target, true, false, plan.ok() ? "模板应用预览通过，尚未写入配置。" : "模板应用预览发现阻断项，修正后才能应用。", data, "", "");
    }

    public WebAdminWriteResult apply(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminTemplateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = target(safe(request == null ? "" : request.templateId));
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.APPLY_TEMPLATE, target);
        String lockTargetId = applyLockTargetId(request);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.APPLY_TEMPLATE, target, lockTargetTypeForApply(), lockTargetId, request == null ? "" : request.lockId);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        if (request == null || !request.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(
                    error("confirmed", "template_apply_confirmation_required", "应用模板前必须先确认 dry-run 预览结果。", "")
            ));
            audit(context, result, Map.of(), Map.of("attempt", "confirmation_required"));
            return result;
        }
        ApplyPlan plan = buildApplyPlan(server, request);
        if (!plan.resolved.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, plan.resolved.message());
            audit(context, result, Map.of(), Map.of("attempt", "template_not_found"));
            return result;
        }
        String expected = safe(request == null ? "" : request.expectedFingerprint);
        if (expected.isBlank() || !expected.equals(plan.planFingerprint)) {
            WebAdminWriteResult result = conflict(target, "预览指纹已过期，请重新执行 dry-run preview。", expected, plan.planFingerprint);
            audit(context, result, Map.of(), Map.of("attempt", "plan_fingerprint_conflict"));
            return result;
        }
        if (!plan.ok()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, plan.validationErrors());
            audit(context, result, Map.of(), plan.toMap());
            return result;
        }
        WebAdminWriteResult writeResult = writePlan(server, plan, user, session, target);
        if (!writeResult.success()) {
            audit(context, writeResult, Map.of(), plan.toMap());
            return writeResult;
        }
        WebAdminAuditEvent auditEvent = audit(context, writeResult, Map.of(), plan.toMap());
        WebAdminRealtimeEvent event = publishTemplateApplied(plan, auditEvent, user);
        releaseLockAfterWrite(lockTargetTypeForApply(), lockTargetId, request == null ? "" : request.lockId, user, session, remoteAddress);
        return withAuditAndRealtime(writeResult, auditEvent, event);
    }

    private WebAdminWriteResult writePlan(MinecraftServer server, ApplyPlan plan, WebAdminUser user, WebAdminSession session, WebAdminWriteTarget target) {
        WebAdminChannelMetadataStore.MetadataFile channelFile = loadChannelMetadata(server).file();
        WebAdminLogicChainMetadataStore.MetadataFile logicFile = loadLogicChainMetadata(server).file();
        SignalJoinStore.SignalJoinFile joinFile = loadJoins(server).file();
        TimerStore.TimerFile timerFile = loadTimers(server).file();
        SignalListenerStore.DataFile listenerFile = loadListeners(server).file();
        String actor = username(user);
        String now = Instant.now().toString();

        for (PlannedChannel channel : plan.channels) {
            WebAdminChannelMetadataStore.MetadataEntry entry = new WebAdminChannelMetadataStore.MetadataEntry();
            entry.channel = channel.id;
            entry.displayName = channel.displayName;
            entry.note = channel.note;
            entry.iconKey = channel.iconKey;
            entry.updatedAt = now;
            entry.updatedBy = actor;
            entry.version = 1L;
            channelFile.channels.put(channel.id, entry);
        }
        for (PlannedLogicChain planned : plan.logicChains) {
            WebAdminLogicChainMetadataStore.MetadataEntry chain = new WebAdminLogicChainMetadataStore.MetadataEntry();
            chain.id = planned.id;
            chain.displayName = planned.displayName;
            chain.note = planned.note;
            chain.iconKey = "template-package";
            chain.rootType = "channel";
            chain.rootRef = planned.rootChannel;
            chain.maxDepth = 4;
            chain.includeDisabled = true;
            chain.updatedAt = now;
            chain.updatedBy = actor;
            chain.version = 1L;
            logicFile.chains.put(chain.id, chain);
        }
        for (SignalJoinDefinition join : plan.signalJoins) {
            joinFile.joins.put(join.id, join.withWriteMetadata(actor, 1L, true));
        }
        for (TimerDefinition timer : plan.timers) {
            timerFile.timers.put(timer.id, timer.withWriteMetadata(actor, 1L, true));
        }
        for (SignalListenerData listener : plan.signalListeners) {
            listenerFile.listeners.add(listener.normalized());
        }

        if (!saveChannelMetadata(server, channelFile)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "频道 metadata 保存失败，请查看服务端日志。");
        }
        if (!saveLogicChainMetadata(server, logicFile)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "逻辑链 metadata 保存失败，请查看服务端日志。");
        }
        if (!saveJoins(server, joinFile)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Signal Join 保存失败，请查看服务端日志。");
        }
        if (!saveTimers(server, timerFile)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Timer 保存失败，请查看服务端日志。");
        }
        if (!saveListeners(server, listenerFile, plan.signalListeners)) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "Signal Listener 保存失败，请查看服务端日志。");
        }
        Map<String, Object> data = plan.toMap();
        data.put("routeTarget", plan.rootChannel().isBlank() ? "#/logic-chains" : "#/logic-chains/resolve?rootType=channel&rootRef=" + plan.rootChannel());
        return result(target, true, true, "模板已应用，配置已写入真实 store。", data, "", "");
    }

    private ApplyPlan buildApplyPlan(MinecraftServer server, WebAdminTemplateRequest request) {
        ResolvedTemplate resolved = resolve(server, request == null ? "" : request.source, request == null ? "" : request.templateId);
        ApplyPlan plan = new ApplyPlan(resolved);
        if (!resolved.success()) {
            return plan;
        }
        WebAdminTemplatePackage template = resolved.template();
        plan.prefix = normalizePrefix(request == null ? "" : request.prefix);
        plan.displayNamePrefix = cleanDisplayNamePrefix(request == null ? "" : request.displayNamePrefix);
        plan.placeholderMappings = request == null || request.placeholderMappings == null ? Map.of() : new LinkedHashMap<>(request.placeholderMappings);
        if (plan.prefix.isBlank()) {
            plan.errors.add(error("prefix", "template_prefix_required", "应用模板需要填写命名空间前缀，避免覆盖现有配置。", ""));
        }
        for (WebAdminTemplatePackage.Placeholder placeholder : template.resources.placeholders) {
            String mapped = safe(plan.placeholderMappings.get(placeholder.id));
            if (placeholder.required && mapped.isBlank()) {
                plan.missingPlaceholders.add(Map.of(
                        "id", placeholder.id,
                        "type", placeholder.type,
                        "displayName", placeholder.displayName,
                        "message", "占位引用未映射：" + placeholder.displayName
                ));
            }
        }
        if (!template.resources.placeholders.isEmpty()) {
            plan.deferredResources.add(Map.of(
                    "type", "placeholders",
                    "count", template.resources.placeholders.size(),
                    "message", "Placeholder binding apply 本阶段 deferred；外部世界实体引用需要后续字段级绑定 schema，不能仅凭字符串映射写入真实配置。"
            ));
        }
        if (!template.resources.actions.isEmpty()) {
            plan.deferredResources.add(Map.of("type", "actions", "count", template.resources.actions.size(), "message", "Top-level ActionResource apply 本阶段 deferred；ActionConfig 仅支持作为 Listener / Timer owned actions 落地。"));
        }
        if (!template.resources.stateVariables.isEmpty()) {
            plan.deferredResources.add(Map.of("type", "stateVariables", "count", template.resources.stateVariables.size(), "message", "StateVariable definition apply 本阶段 deferred。"));
        }
        if (!template.resources.conditionGroups.isEmpty()) {
            plan.deferredResources.add(Map.of("type", "conditionGroups", "count", template.resources.conditionGroups.size(), "message", "ConditionGroup apply 本阶段 deferred。"));
        }

        Map<String, String> channelMap = buildChannelMap(template, request, plan.prefix);
        plan.idMap.put("channels", channelMap);
        plan.externalMappedChannels.addAll(rootMappedChannels(template, request, channelMap));
        plan.rootChannel = firstChannelTarget(template, channelMap);
        Map<String, String> joinMap = new LinkedHashMap<>();
        Map<String, String> timerMap = new LinkedHashMap<>();
        Map<String, String> listenerMap = new LinkedHashMap<>();
        for (WebAdminTemplatePackage.SignalJoinResource resource : template.resources.signalJoins) {
            joinMap.put(resource.id, SignalJoinDefinition.normalizeId(plan.prefix + "." + resource.id));
        }
        for (WebAdminTemplatePackage.TimerResource resource : template.resources.timers) {
            timerMap.put(resource.id, TimerDefinition.normalizeId(plan.prefix + "." + resource.id));
        }
        for (WebAdminTemplatePackage.SignalListenerResource resource : template.resources.signalListeners) {
            listenerMap.put(resource.id, normalizeConfigId(plan.prefix + "." + resource.id));
        }
        plan.idMap.put("signalJoins", joinMap);
        plan.idMap.put("timers", timerMap);
        plan.idMap.put("signalListeners", listenerMap);

        StoreSnapshot stores = loadStoreSnapshot(server);
        if (stores.degraded()) {
            plan.errors.add(error("stores", "template_apply_store_degraded", stores.message(), ""));
        }
        for (WebAdminTemplatePackage.ChannelResource channel : template.resources.channels) {
            String targetId = channelMap.getOrDefault(channel.id, "");
            if (targetId.isBlank()) {
                plan.errors.add(error("channels", "template_channel_id_invalid", "频道 ID 映射后为空：" + channel.id, channel.id));
                continue;
            }
            if (stores.channels.channels.containsKey(targetId)) {
                if (!plan.externalMappedChannels.contains(targetId)) {
                    plan.conflicts.add(conflictMap("channel", targetId, "频道 metadata 已存在：" + targetId));
                }
            } else {
                plan.channels.add(new PlannedChannel(
                        targetId,
                        displayName(plan.displayNamePrefix, channel.displayName, targetId),
                        channel.note,
                        channel.iconKey
                ));
            }
        }
        for (WebAdminTemplatePackage.SignalJoinResource resource : template.resources.signalJoins) {
            String targetId = joinMap.getOrDefault(resource.id, "");
            int referenceErrorCount = plan.errors.size();
            addJoinReferenceErrors(plan, resource.definition, channelMap, resource.id);
            if (plan.errors.size() > referenceErrorCount) {
                continue;
            }
            SignalJoinDefinition join = remapJoin(resource.definition, targetId, channelMap, plan.displayNamePrefix);
            if (targetId.isBlank() || join.id.isBlank()) {
                plan.errors.add(error("signalJoins", "template_signal_join_id_invalid", "Signal Join ID 映射后为空：" + resource.id, resource.id));
                continue;
            }
            int errorCount = plan.errors.size();
            addJoinValidationErrors(plan, join, resource.id);
            if (plan.errors.size() > errorCount) {
                continue;
            }
            if (stores.joins.joins.containsKey(targetId)) {
                plan.conflicts.add(conflictMap("signalJoin", targetId, "Signal Join 已存在：" + targetId));
            } else {
                plan.signalJoins.add(join);
            }
        }
        for (WebAdminTemplatePackage.TimerResource resource : template.resources.timers) {
            String targetId = timerMap.getOrDefault(resource.id, "");
            int referenceErrorCount = plan.errors.size();
            addTimerReferenceErrors(plan, resource.definition, channelMap, timerMap, resource.id);
            if (plan.errors.size() > referenceErrorCount) {
                continue;
            }
            TimerDefinition timer = remapTimer(resource.definition, targetId, channelMap, timerMap, plan.displayNamePrefix);
            if (targetId.isBlank() || timer.id.isBlank()) {
                plan.errors.add(error("timers", "template_timer_id_invalid", "Timer ID 映射后为空：" + resource.id, resource.id));
                continue;
            }
            int errorCount = plan.errors.size();
            addTimerValidationErrors(plan, timer, resource.id);
            if (plan.errors.size() > errorCount) {
                continue;
            }
            if (stores.timers.timers.containsKey(targetId)) {
                plan.conflicts.add(conflictMap("timer", targetId, "Timer 已存在：" + targetId));
            } else {
                plan.timers.add(timer);
            }
        }
        for (WebAdminTemplatePackage.SignalListenerResource resource : template.resources.signalListeners) {
            String targetId = listenerMap.getOrDefault(resource.id, "");
            int referenceErrorCount = plan.errors.size();
            addListenerReferenceErrors(plan, resource.listener, channelMap, timerMap, resource.id);
            if (plan.errors.size() > referenceErrorCount) {
                continue;
            }
            SignalListenerData listener = remapListener(resource.listener, targetId, channelMap, timerMap, plan.displayNamePrefix);
            if (targetId.isBlank() || listener.id().isBlank() || listener.channel().isBlank()) {
                plan.errors.add(error("signalListeners", "template_signal_listener_invalid", "Signal Listener ID 或频道映射后为空：" + resource.id, resource.id));
                continue;
            }
            int errorCount = plan.errors.size();
            addListenerValidationErrors(plan, listener, resource.id);
            if (plan.errors.size() > errorCount) {
                continue;
            }
            boolean duplicateId = stores.listeners.listeners.stream().anyMatch(existing -> existing.id().equals(listener.id()));
            boolean duplicateName = stores.listeners.listeners.stream().anyMatch(existing -> !listener.name().isBlank() && existing.name().equalsIgnoreCase(listener.name()));
            if (duplicateId || duplicateName) {
                plan.conflicts.add(conflictMap("signalListener", listener.id(), duplicateId ? "Signal Listener ID 已存在：" + listener.id() : "Signal Listener 名称已存在：" + listener.name()));
            } else {
                plan.signalListeners.add(listener);
            }
        }
        if (!plan.rootChannel.isBlank()) {
            String logicId = WebAdminLogicChainMetadataStore.normalizeId(plan.prefix + ".template");
            if (stores.logicChainMetadata.chains.containsKey(logicId)) {
                plan.conflicts.add(conflictMap("logicChainMetadata", logicId, "逻辑链 metadata 已存在：" + logicId));
            } else {
                plan.logicChains.add(new PlannedLogicChain(
                        logicId,
                        plan.displayNamePrefix.isBlank() ? plan.resolved.template().displayName : plan.displayNamePrefix + plan.resolved.template().displayName,
                        "由模板中心应用：" + plan.resolved.template().displayName,
                        plan.rootChannel
                ));
            }
        }
        plan.planFingerprint = fingerprint(GSON.toJson(plan.fingerprintInput()));
        return plan;
    }

    private Map<String, String> buildChannelMap(WebAdminTemplatePackage template, WebAdminTemplateRequest request, String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        String rootChannel = SignalChannel.normalize(request == null ? "" : request.rootChannel);
        for (WebAdminTemplatePackage.ChannelResource channel : template.resources.channels) {
            String mapped = "";
            if (!rootChannel.isBlank() && isRootLikeChannel(channel.id)) {
                mapped = rootChannel;
            }
            if (mapped.isBlank()) {
                mapped = SignalChannel.normalize(prefix + "." + channel.id);
            }
            result.put(channel.id, mapped);
        }
        return result;
    }

    private Set<String> rootMappedChannels(WebAdminTemplatePackage template, WebAdminTemplateRequest request, Map<String, String> channelMap) {
        String rootChannel = SignalChannel.normalize(request == null ? "" : request.rootChannel);
        if (rootChannel.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (WebAdminTemplatePackage.ChannelResource channel : template.resources.channels) {
            if (isRootLikeChannel(channel.id)) {
                String mapped = channelMap.getOrDefault(channel.id, "");
                if (!mapped.isBlank()) {
                    result.add(mapped);
                }
            }
        }
        return result;
    }

    private String firstChannelTarget(WebAdminTemplatePackage template, Map<String, String> channelMap) {
        for (WebAdminTemplatePackage.ChannelResource channel : template.resources.channels) {
            String mapped = channelMap.getOrDefault(channel.id, "");
            if (!mapped.isBlank()) {
                return mapped;
            }
        }
        return "";
    }

    private static boolean isRootLikeChannel(String id) {
        String value = safe(id).toLowerCase(Locale.ROOT);
        return value.equals("root") || value.equals("input") || value.equals("input_a") || value.equals("start");
    }

    private SignalJoinDefinition remapJoin(SignalJoinDefinition raw, String targetId, Map<String, String> channelMap, String displayPrefix) {
        SignalJoinDefinition source = raw == null ? new SignalJoinDefinition() : raw.normalized();
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = SignalJoinDefinition.normalizeId(targetId);
        join.displayName = displayName(displayPrefix, source.displayName, join.id);
        join.note = source.note;
        join.enabled = source.enabled;
        join.inputChannels = source.inputChannels.stream()
                .map(input -> new SignalJoinInputDefinition(remapChannel(input.channel, channelMap), input.displayName, input.note, input.requiredCount))
                .toList();
        join.outputChannel = remapChannel(source.outputChannel, channelMap);
        join.mode = source.mode;
        join.threshold = source.threshold;
        join.scopeMode = source.scopeMode;
        join.resetPolicy = source.resetPolicy;
        join.timeoutTicks = source.timeoutTicks;
        join.cooldownTicks = source.cooldownTicks;
        return join.normalized();
    }

    private TimerDefinition remapTimer(TimerDefinition raw, String targetId, Map<String, String> channelMap, Map<String, String> timerMap, String displayPrefix) {
        TimerDefinition source = raw == null ? new TimerDefinition() : raw.normalized();
        TimerDefinition timer = new TimerDefinition();
        timer.id = TimerDefinition.normalizeId(targetId);
        timer.displayName = displayName(displayPrefix, source.displayName, timer.id);
        timer.note = source.note;
        timer.enabled = source.enabled;
        timer.mode = source.mode;
        timer.scopeMode = source.scopeMode;
        timer.durationTicks = source.durationTicks;
        timer.intervalTicks = source.intervalTicks;
        timer.maxRuns = source.maxRuns;
        timer.startPolicy = source.startPolicy;
        timer.outputChannel = remapChannel(source.outputChannel, channelMap);
        timer.onStartActions = remapActions(source.onStartActions, channelMap, timerMap);
        timer.onTickActions = remapActions(source.onTickActions, channelMap, timerMap);
        timer.onCompleteActions = remapActions(source.onCompleteActions, channelMap, timerMap);
        timer.onCancelActions = remapActions(source.onCancelActions, channelMap, timerMap);
        return timer.normalized();
    }

    private SignalListenerData remapListener(SignalListenerData raw, String targetId, Map<String, String> channelMap, Map<String, String> timerMap, String displayPrefix) {
        SignalListenerData listener = raw == null ? new SignalListenerData("", "", "", true, 0, "", List.of()) : raw.normalized();
        return new SignalListenerData(
                normalizeConfigId(targetId),
                displayName(displayPrefix, listener.name(), targetId),
                remapChannel(listener.channel(), channelMap),
                listener.enabled(),
                listener.cooldownTicks(),
                listener.conditionGroupId(),
                remapActions(listener.actions(), channelMap, timerMap)
        ).normalized();
    }

    private List<ActionConfig> remapActions(List<ActionConfig> actions, Map<String, String> channelMap, Map<String, String> timerMap) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<ActionConfig> result = new ArrayList<>();
        for (ActionConfig action : actions) {
            if (action == null) {
                continue;
            }
            ActionConfig normalized = action.normalized();
            String value = normalized.value();
            String timerId = normalized.timerId();
            if (normalized.type() == com.zcpu.tzzmod.action.ActionType.SIGNAL) {
                value = remapChannel(value, channelMap);
            }
            if (normalized.isTimerAction()) {
                timerId = timerMap.getOrDefault(timerId, timerId);
            }
            result.add(new ActionConfig(
                    normalized.type(),
                    value,
                    normalized.enabled(),
                    normalized.requiresOp(),
                    normalized.cooldownTicks(),
                    normalized.notifyOps(),
                    normalized.conditionGroupId(),
                    normalized.stateOperation(),
                    normalized.stateScope(),
                    normalized.stateTargetMode(),
                    normalized.stateTargetId(),
                    normalized.stateKey(),
                    normalized.stateValueType(),
                    normalized.stateValue(),
                    normalized.stateDelta(),
                    normalized.stateCreateIfMissing(),
                    normalized.stateInitialValue(),
                    timerId,
                    normalized.timerTargetMode(),
                    normalized.timerTargetId(),
                    normalized.timerStartPolicyOverride(),
                    normalized.timerDurationOverrideTicks(),
                    normalized.timerMissingBehavior()
            ).normalized());
        }
        return List.copyOf(result);
    }

    private void addJoinValidationErrors(ApplyPlan plan, SignalJoinDefinition join, String sourceId) {
        for (SignalJoinValidationIssue issue : SignalJoinValidator.validate(join, true)) {
            plan.errors.add(error(
                    "signalJoins." + issue.field(),
                    "template_" + issue.code(),
                    "模板 Signal Join 无效（" + sourceId + "）：" + issue.message(),
                    issue.rejectedValue()
            ));
        }
    }

    private void addTimerValidationErrors(ApplyPlan plan, TimerDefinition timer, String sourceId) {
        for (TimerValidationIssue issue : TimerValidator.validate(timer, true)) {
            plan.errors.add(error(
                    "timers." + issue.field(),
                    "template_" + issue.code(),
                    "模板 Timer 无效（" + sourceId + "）：" + issue.message(),
                    issue.rejectedValue()
            ));
        }
    }

    private void addJoinReferenceErrors(ApplyPlan plan, SignalJoinDefinition raw, Map<String, String> channelMap, String sourceId) {
        SignalJoinDefinition join = raw == null ? new SignalJoinDefinition() : raw.normalized();
        int index = 0;
        for (SignalJoinInputDefinition input : join.inputChannels) {
            addChannelReferenceError(plan, "signalJoins." + sourceId + ".inputChannels[" + index + "]", sourceId, input.channel, channelMap, true);
            index++;
        }
        addChannelReferenceError(plan, "signalJoins." + sourceId + ".outputChannel", sourceId, join.outputChannel, channelMap, true);
    }

    private void addTimerReferenceErrors(ApplyPlan plan, TimerDefinition raw, Map<String, String> channelMap, Map<String, String> timerMap, String sourceId) {
        TimerDefinition timer = raw == null ? new TimerDefinition() : raw.normalized();
        addChannelReferenceError(plan, "timers." + sourceId + ".outputChannel", sourceId, timer.outputChannel, channelMap, false);
        addActionReferenceErrors(plan, "timers." + sourceId + ".onStartActions", sourceId, timer.onStartActions, channelMap, timerMap);
        addActionReferenceErrors(plan, "timers." + sourceId + ".onTickActions", sourceId, timer.onTickActions, channelMap, timerMap);
        addActionReferenceErrors(plan, "timers." + sourceId + ".onCompleteActions", sourceId, timer.onCompleteActions, channelMap, timerMap);
        addActionReferenceErrors(plan, "timers." + sourceId + ".onCancelActions", sourceId, timer.onCancelActions, channelMap, timerMap);
    }

    private void addListenerReferenceErrors(ApplyPlan plan, SignalListenerData raw, Map<String, String> channelMap, Map<String, String> timerMap, String sourceId) {
        SignalListenerData listener = raw == null ? new SignalListenerData("", "", "", true, 0, "", List.of()) : raw.normalized();
        addChannelReferenceError(plan, "signalListeners." + sourceId + ".channel", sourceId, listener.channel(), channelMap, true);
        if (!safe(listener.conditionGroupId()).isBlank()) {
            plan.errors.add(error(
                    "signalListeners." + sourceId + ".conditionGroupId",
                    "template_condition_group_reference_deferred",
                    "模板 Signal Listener 引用了 ConditionGroup，本阶段 ConditionGroup apply deferred，不能原样绑定到目标世界：" + listener.conditionGroupId(),
                    listener.conditionGroupId()
            ));
        }
        addActionReferenceErrors(plan, "signalListeners." + sourceId + ".actions", sourceId, listener.actions(), channelMap, timerMap);
    }

    private void addActionReferenceErrors(
            ApplyPlan plan,
            String field,
            String sourceId,
            List<ActionConfig> actions,
            Map<String, String> channelMap,
            Map<String, String> timerMap
    ) {
        List<ActionConfig> safeActions = actions == null ? List.of() : actions;
        for (int index = 0; index < safeActions.size(); index++) {
            ActionConfig action = safeActions.get(index);
            if (action == null) {
                continue;
            }
            ActionConfig normalized = action.normalized();
            String actionField = field + "[" + index + "]";
            if (!safe(normalized.conditionGroupId()).isBlank()) {
                plan.errors.add(error(
                        actionField + ".conditionGroupId",
                        "template_condition_group_reference_deferred",
                        "模板 Action 引用了 ConditionGroup，本阶段 ConditionGroup apply deferred，不能原样绑定到目标世界：" + normalized.conditionGroupId(),
                        normalized.conditionGroupId()
                ));
            }
            if (normalized.type() == ActionType.STATE_VARIABLE) {
                plan.errors.add(error(
                        actionField + ".type",
                        "template_state_action_deferred",
                        "模板 Action 会写入 StateVariable；本阶段 StateVariable action binding apply deferred，不能原样写入目标世界。",
                        normalized.stateKey()
                ));
            }
            if (normalized.type() == ActionType.COMMAND) {
                plan.errors.add(error(
                        actionField + ".type",
                        "template_command_action_deferred",
                        "模板 Action 包含命令文本；本阶段无法验证其中的世界实体或坐标引用，不能原样写入目标世界。",
                        normalized.value()
                ));
            }
            if (normalized.type() == ActionType.SIGNAL) {
                addChannelReferenceError(plan, actionField + ".value", sourceId, normalized.value(), channelMap, true);
            }
            if (normalized.isTimerAction()) {
                addTimerReferenceError(plan, actionField + ".timerId", sourceId, normalized.timerId(), timerMap);
            }
        }
    }

    private void addChannelReferenceError(ApplyPlan plan, String field, String sourceId, String channel, Map<String, String> channelMap, boolean required) {
        String normalized = SignalChannel.normalize(channel);
        if (normalized.isBlank()) {
            if (required) {
                plan.errors.add(error(field, "template_channel_reference_required", "模板资源引用的频道不能为空：" + sourceId, safe(channel)));
            }
            return;
        }
        if (!channelMap.containsKey(normalized)) {
            plan.errors.add(error(
                    field,
                    "template_channel_reference_external",
                    "模板引用了未在 resources.channels 声明的频道，必须先作为模板资源或后续 placeholder binding 支持：" + normalized,
                    normalized
            ));
        }
    }

    private void addTimerReferenceError(ApplyPlan plan, String field, String sourceId, String timerId, Map<String, String> timerMap) {
        String normalized = TimerDefinition.normalizeId(timerId);
        if (normalized.isBlank()) {
            plan.errors.add(error(field, "template_timer_reference_required", "模板 Timer action 引用的 timerId 不能为空：" + sourceId, safe(timerId)));
            return;
        }
        if (!timerMap.containsKey(normalized)) {
            plan.errors.add(error(
                    field,
                    "template_timer_reference_external",
                    "模板引用了未在 resources.timers 声明的 Timer，不能原样绑定到目标世界：" + normalized,
                    normalized
            ));
        }
    }

    private void addListenerValidationErrors(ApplyPlan plan, SignalListenerData listener, String sourceId) {
        if (listener.id().isBlank()) {
            plan.errors.add(error("signalListeners.id", "template_signal_listener_id_required", "模板 Signal Listener ID 映射后不能为空：" + sourceId, sourceId));
        }
        if (listener.name().isBlank()) {
            plan.errors.add(error("signalListeners.name", "template_signal_listener_name_required", "模板 Signal Listener 名称不能为空：" + sourceId, sourceId));
        }
        if (!safe(listener.conditionGroupId()).isBlank()) {
            plan.errors.add(error("signalListeners.conditionGroupId", "template_condition_group_reference_deferred", "模板 Signal Listener 的 ConditionGroup 引用本阶段 deferred。", listener.conditionGroupId()));
        }
        if (!SignalChannel.isValid(listener.channel())) {
            plan.errors.add(error("signalListeners.channel", "template_signal_listener_channel_invalid", "模板 Signal Listener 频道无效：" + sourceId, listener.channel()));
        }
        List<ActionConfig> actions = listener.actions() == null ? List.of() : listener.actions();
        if (actions.size() > 64) {
            plan.errors.add(error("signalListeners.actions", "template_signal_listener_too_many_actions", "每个模板 Signal Listener 最多支持 64 条动作。", String.valueOf(actions.size())));
        }
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index);
            if (action == null) {
                plan.errors.add(error("signalListeners.actions[" + index + "]", "template_signal_listener_action_required", "模板 Signal Listener action 不能为空。", sourceId));
                continue;
            }
            ActionConfig normalized = action.normalized();
            if (normalized.enabled() && !normalized.isUsable()) {
                plan.errors.add(error("signalListeners.actions[" + index + "]", "template_signal_listener_action_unusable", "模板 Signal Listener action 缺少必要字段。", normalized.type().id()));
            }
            if (!safe(normalized.conditionGroupId()).isBlank()) {
                plan.errors.add(error("signalListeners.actions[" + index + "].conditionGroupId", "template_condition_group_reference_deferred", "模板 Signal Listener action 的 ConditionGroup 引用本阶段 deferred。", normalized.conditionGroupId()));
            }
        }
    }

    private static String remapChannel(String channel, Map<String, String> channelMap) {
        String normalized = SignalChannel.normalize(channel);
        return channelMap.getOrDefault(normalized, normalized);
    }

    private ResolvedTemplate resolve(MinecraftServer server, String source, String templateId) {
        // 8.15 built-in detail fix marker: source may be explicit built_in/user, or blank for unified lookup.
        String safeSource = normalizeSourceForLookup(source);
        String safeId = WebAdminTemplatePackage.normalizeId(templateId);
        if (safeId.isBlank()) {
            return ResolvedTemplate.failed("template_not_found", "模板不存在或已移除。");
        }
        if ("invalid".equals(safeSource)) {
            return ResolvedTemplate.failed("template_source_invalid", "模板来源无效。");
        }
        if (safeSource.isBlank()) {
            WebAdminTemplatePackage builtIn = WebAdminBuiltInTemplates.find(safeId);
            if (builtIn != null) {
                return ResolvedTemplate.ok(SOURCE_BUILT_IN, builtIn.normalized());
            }
            WebAdminTemplateStore.TemplateLoadResult loaded = loadTemplates(server);
            if (loaded.degraded()) {
                return ResolvedTemplate.failed("template_schema_invalid", loaded.message());
            }
            WebAdminTemplatePackage userTemplate = loaded.file().templates.get(safeId);
            return userTemplate == null
                    ? ResolvedTemplate.failed("template_not_found", "模板不存在或已移除。")
                    : ResolvedTemplate.ok(SOURCE_USER, userTemplate.normalized());
        }
        if (SOURCE_BUILT_IN.equals(safeSource)) {
            WebAdminTemplatePackage template = WebAdminBuiltInTemplates.find(safeId);
            return template == null
                    ? ResolvedTemplate.failed("template_not_found", "内置模板不存在或已移除。")
                    : ResolvedTemplate.ok(SOURCE_BUILT_IN, template.normalized());
        }
        WebAdminTemplateStore.TemplateLoadResult loaded = loadTemplates(server);
        if (loaded.degraded()) {
            return ResolvedTemplate.failed("template_schema_invalid", loaded.message());
        }
        WebAdminTemplatePackage template = loaded.file().templates.get(safeId);
        return template == null
                ? ResolvedTemplate.failed("template_not_found", "用户模板不存在或已删除。")
                : ResolvedTemplate.ok(SOURCE_USER, template.normalized());
    }

    private ParseResult parsePackage(String packageJson) {
        if (safe(packageJson).isBlank()) {
            return ParseResult.failed(List.of(error("packageJson", "template_json_required", "请粘贴模板 JSON。", "")));
        }
        try {
            JsonElement element = JsonParser.parseString(packageJson);
            if (!element.isJsonObject()) {
                return ParseResult.failed(List.of(error("packageJson", "template_json_object_required", "模板 JSON 必须是对象。", "")));
            }
            JsonObject object = element.getAsJsonObject();
            String schemaValue = "";
            if (object.has("schema") && !object.get("schema").isJsonNull()) {
                try {
                    schemaValue = object.get("schema").getAsString();
                } catch (Exception exception) {
                    return ParseResult.failed(List.of(error("packageJson", "template_schema_invalid", "模板 JSON schema 必须是字符串。", "")));
                }
            }
            if (schemaValue.isBlank()) {
                return ParseResult.failed(List.of(error("packageJson", "template_schema_required", "模板 JSON 缺少 schema，必须显式声明 tzz_template_v1。", "")));
            }
            WebAdminTemplatePackage parsed = GSON.fromJson(element, WebAdminTemplatePackage.class);
            if (parsed == null) {
                return ParseResult.failed(List.of(error("packageJson", "template_json_empty", "模板 JSON 为空。", "")));
            }
            WebAdminTemplatePackage normalized = parsed.normalized();
            List<String> errors = normalized.validationErrors();
            if (!errors.isEmpty()) {
                List<WebAdminValidationError> validationErrors = new ArrayList<>();
                for (String message : errors) {
                    validationErrors.add(error("packageJson", "template_schema_invalid", message, normalized.templateId));
                }
                return ParseResult.failed(validationErrors);
            }
            return ParseResult.ok(normalized);
        } catch (JsonSyntaxException exception) {
            return ParseResult.failed(List.of(error("packageJson", "template_json_invalid", "模板 JSON 无法解析：" + exception.getMessage(), "")));
        }
    }

    private WebAdminTemplatePackage withImportOverrides(WebAdminTemplatePackage raw, WebAdminTemplateRequest request) {
        WebAdminTemplatePackage template = raw == null ? new WebAdminTemplatePackage() : raw.normalized();
        String overrideId = WebAdminTemplatePackage.normalizeId(request == null ? "" : request.importedTemplateId);
        if (!overrideId.isBlank()) {
            template.templateId = overrideId;
        }
        String overrideName = safe(request == null ? "" : request.importedDisplayName).trim();
        if (!overrideName.isBlank()) {
            template.displayName = overrideName;
        }
        template.metadata.source = "user";
        if (template.createdAt <= 0L) {
            template.createdAt = System.currentTimeMillis();
        }
        template.updatedAt = System.currentTimeMillis();
        return template.normalized();
    }

    private Map<String, Object> summary(WebAdminTemplatePackage template, String source) {
        Map<String, Object> data = WebAdminTemplateStore.summary(template, source);
        data.put("routeKey", source + ":" + template.templateId);
        data.put("detailRoute", "#/templates/" + urlEncode(source + ":" + template.templateId));
        data.put("data-template-export-json-action", true);
        return data;
    }

    private Map<String, Object> resourceSummary(WebAdminTemplatePackage template) {
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("channels", template.resources.channels);
        resources.put("signalJoins", template.resources.signalJoins);
        resources.put("timers", template.resources.timers);
        resources.put("signalListeners", template.resources.signalListeners);
        resources.put("actions", template.resources.actions);
        resources.put("stateVariables", template.resources.stateVariables);
        resources.put("conditionGroups", template.resources.conditionGroups);
        resources.put("placeholders", template.resources.placeholders);
        return resources;
    }

    private String exportJson(WebAdminTemplatePackage template, String source) {
        WebAdminTemplatePackage copy = template.normalized();
        copy.metadata.source = normalizeSource(source);
        return GSON.toJson(copy);
    }

    private WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminOperationType operation,
            WebAdminWriteTarget target,
            String lockTargetType,
            String lockTargetId,
            String lockId
    ) {
        WebAdminPermissionDecision permission = permissionService.decide(user, operation);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            return WebAdminWriteResult.failed(code(csrf.code()), target, csrf.message());
        }
        if (!sameOrigin) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(lockTargetType, lockTargetId, lockId, user, session);
            if (!validation.success()) {
                return validation.result();
            }
        }
        return WebAdminWriteResult.ok(target, false, "写请求安全校验通过。");
    }

    private WebAdminWriteResult readWritePreviewPreflight(WebAdminUser user, WebAdminSession session, String csrfToken, boolean sameOrigin, WebAdminWriteTarget target) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.READ);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            return WebAdminWriteResult.failed(code(csrf.code()), target, csrf.message());
        }
        if (!sameOrigin) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
        }
        return WebAdminWriteResult.ok(target, false, "模板预览安全校验通过。");
    }

    private void releaseLockAfterWrite(String targetType, String targetId, String lockId, WebAdminUser user, WebAdminSession session, String remoteAddress) {
        if (editLockService != null && !safe(lockId).isBlank()) {
            editLockService.releaseAfterWrite(targetType, targetId, lockId, user, session, remoteAddress);
        }
    }

    private WebAdminTemplateStore.TemplateLoadResult loadTemplates(MinecraftServer server) {
        return templateStorePath == null ? WebAdminTemplateStore.loadWithStatus(server) : WebAdminTemplateStore.loadWithStatus(templateStorePath);
    }

    private boolean saveTemplates(MinecraftServer server, WebAdminTemplateStore.TemplateFile file) {
        return templateStorePath == null ? WebAdminTemplateStore.save(server, file) : WebAdminTemplateStore.save(templateStorePath, file);
    }

    private LoadResult<WebAdminChannelMetadataStore.MetadataFile> loadChannelMetadata(MinecraftServer server) {
        return loadJson(metadataPath(server, "channel"), WebAdminChannelMetadataStore.MetadataFile.class, new WebAdminChannelMetadataStore.MetadataFile(), "频道 metadata");
    }

    private boolean saveChannelMetadata(MinecraftServer server, WebAdminChannelMetadataStore.MetadataFile file) {
        if (channelMetadataStorePath == null) {
            return WebAdminChannelMetadataStore.save(server, file);
        }
        return JsonStoreSupport.write(channelMetadataStorePath, file == null ? new WebAdminChannelMetadataStore.MetadataFile() : file.normalized(), "web admin channel metadata");
    }

    private LoadResult<WebAdminLogicChainMetadataStore.MetadataFile> loadLogicChainMetadata(MinecraftServer server) {
        return loadJson(metadataPath(server, "logic"), WebAdminLogicChainMetadataStore.MetadataFile.class, new WebAdminLogicChainMetadataStore.MetadataFile(), "逻辑链 metadata");
    }

    private boolean saveLogicChainMetadata(MinecraftServer server, WebAdminLogicChainMetadataStore.MetadataFile file) {
        if (logicChainMetadataStorePath == null) {
            return WebAdminLogicChainMetadataStore.save(server, file);
        }
        return JsonStoreSupport.write(logicChainMetadataStorePath, file == null ? new WebAdminLogicChainMetadataStore.MetadataFile() : file.normalized(), "web admin logic chain metadata");
    }

    private SignalJoinStore.SignalJoinLoadResult loadJoins(MinecraftServer server) {
        return signalJoinStorePath == null ? SignalJoinStore.loadWithStatus(server) : SignalJoinStore.loadWithStatus(signalJoinStorePath);
    }

    private boolean saveJoins(MinecraftServer server, SignalJoinStore.SignalJoinFile file) {
        return signalJoinStorePath == null ? SignalJoinStore.save(server, file) : SignalJoinStore.save(signalJoinStorePath, file);
    }

    private TimerStore.TimerLoadResult loadTimers(MinecraftServer server) {
        return timerStorePath == null ? TimerStore.loadWithStatus(server) : TimerStore.loadWithStatus(timerStorePath);
    }

    private boolean saveTimers(MinecraftServer server, TimerStore.TimerFile file) {
        return timerStorePath == null ? TimerStore.save(server, file) : TimerStore.save(timerStorePath, file);
    }

    private SignalListenerStore.SignalListenerLoadResult loadListeners(MinecraftServer server) {
        return signalListenerStorePath == null
                ? SignalListenerStore.loadWithStatus(SignalListenerStore.path(server, false))
                : SignalListenerStore.loadWithStatus(signalListenerStorePath);
    }

    private boolean saveListeners(MinecraftServer server, SignalListenerStore.DataFile file, List<SignalListenerData> newlyCreated) {
        if (signalListenerStorePath != null) {
            return SignalListenerStore.save(signalListenerStorePath, file);
        }
        boolean ok = true;
        for (SignalListenerData listener : newlyCreated) {
            ok &= SignalListenerStore.addListenerExactForWebAdmin(server, listener);
        }
        SignalListenerStore.flushDirty(server);
        return ok;
    }

    private Path metadataPath(MinecraftServer server, String type) {
        if ("channel".equals(type) && channelMetadataStorePath != null) {
            return channelMetadataStorePath;
        }
        if ("logic".equals(type) && logicChainMetadataStorePath != null) {
            return logicChainMetadataStorePath;
        }
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        return "channel".equals(type)
                ? paths.channelMetadataPath()
                : paths.directory().resolve("web_admin_logic_chain_metadata.json");
    }

    private <T> LoadResult<T> loadJson(Path path, Class<T> type, T fallback, String label) {
        try {
            if (path == null || !Files.exists(path)) {
                return new LoadResult<>(fallback, false, "");
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                T parsed = GSON.fromJson(reader, type);
                return new LoadResult<>(parsed == null ? fallback : parsed, false, "");
            }
        } catch (Exception exception) {
            return new LoadResult<>(fallback, true, label + " 文件读取失败，已停止写入以避免覆盖损坏文件：" + exception.getMessage());
        }
    }

    private StoreSnapshot loadStoreSnapshot(MinecraftServer server) {
        LoadResult<WebAdminChannelMetadataStore.MetadataFile> channels = loadChannelMetadata(server);
        SignalJoinStore.SignalJoinLoadResult joins = loadJoins(server);
        TimerStore.TimerLoadResult timers = loadTimers(server);
        SignalListenerStore.SignalListenerLoadResult listeners = loadListeners(server);
        LoadResult<WebAdminLogicChainMetadataStore.MetadataFile> logic = loadLogicChainMetadata(server);
        List<String> messages = new ArrayList<>();
        if (channels.degraded) messages.add(channels.message);
        if (joins.degraded()) messages.add(joins.message());
        if (timers.degraded()) messages.add(timers.message());
        if (listeners.degraded()) messages.add(listeners.message());
        if (logic.degraded) messages.add(logic.message);
        return new StoreSnapshot(
                channels.file.normalized(),
                joins.file(),
                timers.file(),
                listeners.file(),
                logic.file.normalized(),
                !messages.isEmpty(),
                String.join("；", messages)
        );
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private WebAdminRealtimeEvent publishTemplateStoreChanged(WebAdminTemplatePackage template, WebAdminAuditEvent auditEvent, WebAdminUser actor) {
        return WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.TEMPLATE_STORE_CHANGED)
                .sourceType("template_package")
                .severity("INFO")
                .summary("模板库已更新：" + template.displayName)
                .routeTarget("#/templates")
                .payload("templateId", template.templateId)
                .payload("actor", username(actor))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
    }

    private WebAdminRealtimeEvent publishTemplateApplied(ApplyPlan plan, WebAdminAuditEvent auditEvent, WebAdminUser actor) {
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .sourceType("template_package")
                .severity("INFO")
                .summary("模板应用已写入配置：" + plan.resolved.template().displayName)
                .routeTarget("#/logic-chains")
                .payload("templateId", plan.resolved.template().templateId)
                .payload("prefix", plan.prefix)
                .payload("actor", username(actor)));
        return WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.TEMPLATE_APPLIED)
                .sourceType("template_package")
                .severity("INFO")
                .summary("模板已应用：" + plan.resolved.template().displayName)
                .routeTarget("#/templates")
                .payload("templateId", plan.resolved.template().templateId)
                .payload("source", plan.resolved.source())
                .payload("prefix", plan.prefix)
                .payload("createdResources", plan.createdResourceCount())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
    }

    private WebAdminWriteResult withAuditAndRealtime(WebAdminWriteResult result, WebAdminAuditEvent auditEvent, WebAdminRealtimeEvent event) {
        return new WebAdminWriteResult(
                result.success(),
                result.code(),
                result.message(),
                result.targetType(),
                result.targetId(),
                result.changed(),
                result.validationErrors(),
                auditEvent == null ? result.auditId() : auditEvent.auditId(),
                event == null ? result.realtimeEventId() : event.id(),
                result.requiresConfirmation(),
                result.conflict(),
                result.data()
        );
    }

    private WebAdminWriteResult result(WebAdminWriteTarget target, boolean success, boolean changed, String message, Map<String, Object> data, String auditId, String realtimeEventId) {
        return new WebAdminWriteResult(
                success,
                success ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.VALIDATION_FAILED.id(),
                message,
                target.targetType(),
                target.targetId(),
                changed,
                List.of(),
                auditId,
                realtimeEventId,
                false,
                Map.of(),
                data == null ? Map.of() : data
        );
    }

    private WebAdminWriteResult conflict(WebAdminWriteTarget target, String message, String expected, String actual) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expected);
        conflict.put("actualFingerprint", actual);
        return new WebAdminWriteResult(false, WebAdminWriteResultCode.CONFLICT_DETECTED.id(), message, target.targetType(), target.targetId(), false, List.of(), "", "", false, conflict, Map.of());
    }

    private static WebAdminValidationError error(String field, String code, String message, String rejectedValue) {
        return new WebAdminValidationError(field, code, message, rejectedValue);
    }

    private static WebAdminWriteTarget target(String id) {
        return new WebAdminWriteTarget(TARGET_TYPE, safe(id), safe(id));
    }

    public static String lockTargetTypeForImport() {
        return WebAdminEditLockService.TARGET_TEMPLATE_STORE;
    }

    public static String lockTargetTypeForApply() {
        return WebAdminEditLockService.TARGET_TEMPLATE_APPLY;
    }

    public static String applyLockTargetId(WebAdminTemplateRequest request) {
        if (request == null) {
            return "unknown";
        }
        return normalizeSource(request.source) + ":" + WebAdminTemplatePackage.normalizeId(request.templateId) + ":" + normalizePrefix(request.prefix);
    }

    private static String normalizeSource(String source) {
        return SOURCE_BUILT_IN.equals(safe(source).trim().toLowerCase(Locale.ROOT)) ? SOURCE_BUILT_IN : SOURCE_USER;
    }

    private static String normalizeSourceForLookup(String source) {
        String value = safe(source).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "";
        }
        if (SOURCE_BUILT_IN.equals(value)) {
            return SOURCE_BUILT_IN;
        }
        if (SOURCE_USER.equals(value) || "imported".equals(value)) {
            return SOURCE_USER;
        }
        return "invalid";
    }

    private static String normalizePrefix(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length() && builder.length() < 48; index++) {
            char c = value.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        String result = builder.toString();
        while (result.endsWith(".") || result.endsWith(":") || result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String normalizeConfigId(String raw) {
        return WebAdminTemplatePackage.normalizeId(raw);
    }

    private static String displayName(String prefix, String displayName, String fallback) {
        String base = safe(displayName).isBlank() ? safe(fallback) : safe(displayName).trim();
        return (safe(prefix).isBlank() ? "" : safe(prefix).trim()) + base;
    }

    private static String cleanDisplayNamePrefix(String raw) {
        String value = safe(raw).trim();
        return value.length() > 32 ? value.substring(0, 32) : value;
    }

    private static Map<String, Object> conflictMap(String type, String id, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("id", id);
        data.put("message", message);
        return data;
    }

    private static String username(WebAdminUser user) {
        return user == null ? "" : safe(user.username);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static WebAdminWriteResultCode code(String id) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(id)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record LoadResult<T>(T file, boolean degraded, String message) {
        private LoadResult {
            message = message == null ? "" : message;
        }
    }

    private record StoreSnapshot(
            WebAdminChannelMetadataStore.MetadataFile channels,
            SignalJoinStore.SignalJoinFile joins,
            TimerStore.TimerFile timers,
            SignalListenerStore.DataFile listeners,
            WebAdminLogicChainMetadataStore.MetadataFile logicChainMetadata,
            boolean degraded,
            String message
    ) {
    }

    private record ResolvedTemplate(boolean success, String source, WebAdminTemplatePackage template, String code, String message) {
        static ResolvedTemplate ok(String source, WebAdminTemplatePackage template) {
            return new ResolvedTemplate(true, source, template, "ok", "");
        }

        static ResolvedTemplate failed(String code, String message) {
            return new ResolvedTemplate(false, "", null, code, message);
        }
    }

    private record ParseResult(boolean success, WebAdminTemplatePackage template, List<WebAdminValidationError> errors) {
        static ParseResult ok(WebAdminTemplatePackage template) {
            return new ParseResult(true, template, List.of());
        }

        static ParseResult failed(List<WebAdminValidationError> errors) {
            return new ParseResult(false, null, errors == null ? List.of() : List.copyOf(errors));
        }
    }

    private record PlannedChannel(String id, String displayName, String note, String iconKey) {
    }

    private record PlannedLogicChain(String id, String displayName, String note, String rootChannel) {
    }

    private static final class ApplyPlan {
        final ResolvedTemplate resolved;
        String prefix = "";
        String displayNamePrefix = "";
        String rootChannel = "";
        String planFingerprint = "";
        Map<String, String> placeholderMappings = Map.of();
        final Map<String, Map<String, String>> idMap = new LinkedHashMap<>();
        final Set<String> externalMappedChannels = new LinkedHashSet<>();
        final List<PlannedChannel> channels = new ArrayList<>();
        final List<PlannedLogicChain> logicChains = new ArrayList<>();
        final List<SignalJoinDefinition> signalJoins = new ArrayList<>();
        final List<TimerDefinition> timers = new ArrayList<>();
        final List<SignalListenerData> signalListeners = new ArrayList<>();
        final List<Map<String, Object>> conflicts = new ArrayList<>();
        final List<Map<String, Object>> missingPlaceholders = new ArrayList<>();
        final List<Map<String, Object>> deferredResources = new ArrayList<>();
        final List<WebAdminValidationError> errors = new ArrayList<>();

        ApplyPlan(ResolvedTemplate resolved) {
            this.resolved = resolved;
        }

        boolean ok() {
            return resolved.success()
                    && errors.isEmpty()
                    && conflicts.isEmpty()
                    && missingPlaceholders.isEmpty()
                    && deferredResources.isEmpty();
        }

        String rootChannel() {
            return rootChannel;
        }

        int createdResourceCount() {
            return channels.size() + logicChains.size() + signalJoins.size() + timers.size() + signalListeners.size();
        }

        List<WebAdminValidationError> validationErrors() {
            List<WebAdminValidationError> result = new ArrayList<>(errors);
            for (Map<String, Object> conflict : conflicts) {
                result.add(error("conflicts", "template_apply_conflict", String.valueOf(conflict.get("message")), String.valueOf(conflict.get("id"))));
            }
            for (Map<String, Object> missing : missingPlaceholders) {
                result.add(error("placeholders", "template_placeholder_missing", String.valueOf(missing.get("message")), String.valueOf(missing.get("id"))));
            }
            for (Map<String, Object> deferred : deferredResources) {
                result.add(error("resources", "template_resource_deferred", String.valueOf(deferred.get("message")), String.valueOf(deferred.get("type"))));
            }
            return result;
        }

        Map<String, Object> fingerprintInput() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("source", resolved.source());
            data.put("templateFingerprint", WebAdminTemplateStore.fingerprintFor(resolved.template()));
            data.put("prefix", prefix);
            data.put("displayNamePrefix", displayNamePrefix);
            data.put("placeholderMappings", placeholderMappings);
            data.put("idMap", idMap);
            data.put("createChannels", channels.stream().map(PlannedChannel::id).toList());
            data.put("createLogicChains", logicChains.stream().map(PlannedLogicChain::id).toList());
            data.put("createSignalJoins", signalJoins.stream().map(join -> join.id).toList());
            data.put("createTimers", timers.stream().map(timer -> timer.id).toList());
            data.put("createSignalListeners", signalListeners.stream().map(SignalListenerData::id).toList());
            data.put("conflicts", conflicts);
            data.put("missingPlaceholders", missingPlaceholders);
            data.put("deferredResources", deferredResources);
            data.put("errors", errors.stream().map(WebAdminValidationError::code).toList());
            return data;
        }

        Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ok", ok());
            data.put("source", resolved.source());
            data.put("template", WebAdminTemplateStore.summary(resolved.template(), resolved.source()));
            data.put("prefix", prefix);
            data.put("displayNamePrefix", displayNamePrefix);
            data.put("planFingerprint", planFingerprint);
            data.put("expectedFingerprint", planFingerprint);
            data.put("idMap", idMap);
            data.put("createdResourceCount", createdResourceCount());
            data.put("createChannels", channels);
            data.put("createLogicChains", logicChains);
            data.put("createSignalJoins", signalJoins);
            data.put("createTimers", timers);
            data.put("createSignalListeners", signalListeners);
            data.put("createActions", actionCount(signalListeners, timers));
            data.put("conflicts", conflicts);
            data.put("missingPlaceholders", missingPlaceholders);
            data.put("deferredResources", deferredResources);
            data.put("warnings", warnings());
            data.put("validationErrors", validationErrors());
            return data;
        }

        private List<String> warnings() {
            List<String> warnings = new ArrayList<>();
            if (!missingPlaceholders.isEmpty()) {
                warnings.add("存在未映射 placeholder，不能应用。");
            }
            if (!deferredResources.isEmpty()) {
                warnings.add("存在本阶段 deferred 的资源类型。");
            }
            warnings.add("dry-run 不写入任何 store；apply 会重新计算并校验 expectedFingerprint。");
            warnings.add("世界实体引用不会自动复制或放置方块。");
            warnings.add("apply 已先完成全量校验；多 store 顺序保存失败时会返回失败信息，完整事务回滚留待后续阶段。");
            return List.copyOf(warnings);
        }

        private static int actionCount(List<SignalListenerData> listeners, List<TimerDefinition> timers) {
            int count = 0;
            for (SignalListenerData listener : listeners) {
                count += listener.actions().size();
            }
            for (TimerDefinition timer : timers) {
                count += timer.onStartActions.size() + timer.onTickActions.size() + timer.onCompleteActions.size() + timer.onCancelActions.size();
            }
            return count;
        }
    }
}
