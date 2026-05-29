package com.zcpu.tzzmod.webadmin.snapshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeGateStore;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.scheduler.TimerRuntimeService;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.RollbackOperation;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.RollbackPlan;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotFieldDiff;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotDiff;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotDiffEntry;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotDiffSummary;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotKind;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotManifest;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotPackage;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotRecord;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotResource;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotTrigger;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.StoreSpec;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.ManifestLoadResult;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.PackageLoadResult;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.SnapshotCollectionResult;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;

public final class WebAdminSnapshotService {
    private static final ThreadLocal<Integer> SUPPRESS_AUTO_CAPTURE = ThreadLocal.withInitial(() -> 0);
    private static final WebAdminWriteTarget SNAPSHOT_TARGET = new WebAdminWriteTarget("SNAPSHOT", "timeline", "配置快照时间轴");
    private static final int DIFF_JSON_PREVIEW_LIMIT = 2400;
    private static final int DIFF_FIELD_LIMIT = 20;
    private static final int DIFF_VALUE_LIMIT = 420;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminSnapshotService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> list(MinecraftServer server, WebAdminUser user, Map<String, String> query) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.VIEW_SNAPSHOTS);
        if (!permission.allowed()) {
            return Map.of("permissionDenied", true, "message", permission.message());
        }
        ManifestLoadResult loaded = WebAdminSnapshotStore.loadManifest(server);
        SnapshotManifest manifest = loaded.manifest();
        List<SnapshotRecord> viewRecords = manifest.records.stream()
                .map(record -> viewRecordWithEffectiveSummary(server, manifest, record))
                .toList();
        List<SnapshotRecord> records = viewRecords.stream()
                .filter(record -> matches(record, query == null ? Map.of() : query))
                .sorted(Comparator.comparingLong((SnapshotRecord record) -> record.sequence).reversed())
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("manifestFingerprint", manifest.manifestFingerprint);
        data.put("storagePath", "tzz/webadmin/snapshots");
        data.put("degraded", loaded.degraded());
        data.put("message", loaded.message());
        data.put("stats", stats(manifest.records));
        data.put("filters", filterOptions(viewRecords));
        data.put("retention", Map.of(
                "autoRetentionLimit", WebAdminSnapshotStore.AUTO_RETENTION_LIMIT,
                "manualProtected", true,
                "preRollbackProtected", true
        ));
        return data;
    }

    public Map<String, Object> detail(MinecraftServer server, WebAdminUser user, String snapshotId) {
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.VIEW_SNAPSHOTS);
        if (!permission.allowed()) {
            return Map.of("permissionDenied", true, "message", permission.message());
        }
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(server);
        SnapshotRecord record = findRecord(manifestLoad.manifest(), snapshotId);
        if (record == null) {
            return Map.of("notFound", true, "message", "快照不存在或已被清理。");
        }
        PackageLoadResult packLoad = WebAdminSnapshotStore.loadPackage(server, record);
        SnapshotRecord previousRecord = previousRecord(manifestLoad.manifest(), record);
        PackageLoadResult previousLoad = previousRecord == null ? new PackageLoadResult(null, false, "") : WebAdminSnapshotStore.loadPackage(server, previousRecord);
        SnapshotDiff operationDiff = effectiveOperationDiff(manifestLoad.manifest(), record, packLoad.pack(), WebAdminSnapshotStore.snapshotRoot(server));
        SnapshotDiff diff = diffAgainstPreviousRecord(previousRecord, previousLoad.pack(), packLoad.pack());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record", viewRecord(record));
        data.put("previousRecord", previousRecord == null ? Map.of() : viewRecord(previousRecord));
        data.put("resources", packLoad.pack() == null ? List.of() : packLoad.pack().resources.stream().map(WebAdminSnapshotService::viewResource).toList());
        data.put("diff", diff);
        data.put("operationDiff", operationDiff);
        data.put("manifestFingerprint", manifestLoad.manifest().manifestFingerprint);
        data.put("degraded", manifestLoad.degraded() || packLoad.degraded() || previousLoad.degraded());
        data.put("message", joinMessages(manifestLoad.message(), packLoad.message(), previousLoad.message()));
        return data;
    }

    public WebAdminWriteResult createManual(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminSnapshotRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.CREATE_SNAPSHOT, SNAPSHOT_TARGET);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.CREATE_SNAPSHOT, SNAPSHOT_TARGET);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "manual_snapshot_preflight_failed"));
            return preflight;
        }
        WebAdminSnapshotRequest safeRequest = request == null ? new WebAdminSnapshotRequest() : request;
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (safe(safeRequest.title).isBlank()) {
            errors.add(new WebAdminValidationError("title", "required", "手动保存点需要填写名称。", ""));
        }
        if (safe(safeRequest.title).length() > 80) {
            errors.add(new WebAdminValidationError("title", "too_long", "名称不能超过 80 个字符。", safeRequest.title));
        }
        if (safe(safeRequest.note).length() > 1000) {
            errors.add(new WebAdminValidationError("note", "too_long", "备注不能超过 1000 个字符。", ""));
        }
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(SNAPSHOT_TARGET, errors);
            audit(context, result, Map.of(), Map.of("attempt", "manual_snapshot_validation_failed"));
            return result;
        }

        SnapshotTrigger trigger = new SnapshotTrigger();
        trigger.operation = WebAdminOperationType.CREATE_SNAPSHOT.id();
        trigger.module = "Snapshot";
        trigger.reason = "manual";
        SnapshotRecord record = runSuppressed(() -> createSnapshot(
                server,
                SnapshotKind.MANUAL,
                username(user),
                safeRequest.title,
                safeRequest.note,
                safeRequest.tags,
                trigger
        ));
        if (record == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, SNAPSHOT_TARGET, "手动保存点创建失败，请查看服务端日志。");
            audit(context, result, Map.of(), Map.of("attempt", "manual_snapshot_failed"));
            return result;
        }
        WebAdminWriteResult result = okWithData(SNAPSHOT_TARGET, "手动保存点已创建。", Map.of("snapshot", record, "routeTarget", "#/snapshots"));
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), snapshotSummary(record));
        publishSnapshotRealtime(WebAdminRealtimeEventType.SNAPSHOT_CREATED, record, auditEvent, user, "手动保存点已创建。");
        return result;
    }

    public WebAdminSnapshotAutoResult createAutoBeforeWrite(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminOperationType operationType,
            String module,
            String targetType,
            String targetId,
            String reason
    ) {
        if (server == null || suppressed()) {
            return WebAdminSnapshotAutoResult.skipped("auto capture suppressed");
        }
        SnapshotTrigger trigger = new SnapshotTrigger();
        trigger.operation = operationType == null ? "" : operationType.id();
        trigger.module = safe(module);
        trigger.targetType = safe(targetType);
        trigger.targetId = safe(targetId);
        trigger.reason = safe(reason);
        trigger.routeTarget = routeTarget(targetType, targetId);
        try {
            SnapshotRecord record = runSuppressed(() -> createSnapshot(
                    server,
                    SnapshotKind.AUTO,
                    username(user),
                    autoTitle(operationType, module),
                    safe(reason),
                    List.of(moduleTag(module)),
                    trigger
            ));
            if (record == null) {
                WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, SNAPSHOT_TARGET, "自动快照创建失败，请查看服务端日志。");
                audit(autoAuditContext(user, operationType, null), result, Map.of(), autoSnapshotAuditData(operationType, module, targetType, targetId, "auto_snapshot_null_record"));
                return WebAdminSnapshotAutoResult.failed("auto snapshot failed");
            }
            WebAdminWriteResult result = okWithData(snapshotTarget(record.snapshotId), "自动快照已创建。", Map.of("snapshot", record, "triggerOperation", trigger.operation));
            WebAdminAuditEvent auditEvent = audit(autoAuditContext(user, operationType, record), result, Map.of(), autoSnapshotAuditData(operationType, module, targetType, targetId, "auto_snapshot_created"));
            publishSnapshotRealtime(WebAdminRealtimeEventType.SNAPSHOT_CREATED, record, auditEvent, user, "自动快照已创建。");
            publishSnapshotRealtime(WebAdminRealtimeEventType.SNAPSHOT_TIMELINE_CHANGED, record, auditEvent, user, "配置快照时间轴已更新。");
            return WebAdminSnapshotAutoResult.created(record);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to create WebAdmin auto snapshot before {}: {}", operationType, exception.getMessage());
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, SNAPSHOT_TARGET, "自动快照创建失败，请查看服务端日志。");
            audit(autoAuditContext(user, operationType, null), result, Map.of(), autoSnapshotAuditData(operationType, module, targetType, targetId, "auto_snapshot_exception"));
            return WebAdminSnapshotAutoResult.failed("auto snapshot failed");
        }
    }

    public static WebAdminSnapshotAutoResult createAutoBeforeTrustedWrite(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminOperationType operationType,
            String module,
            String targetType,
            String targetId,
            String reason
    ) {
        WebAdminSnapshotService service = new WebAdminSnapshotService(null, null, null);
        return service.createAutoBeforeWrite(server, user, operationType, module, targetType, targetId, reason);
    }

    public WebAdminWriteResult dryRunRollback(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String snapshotId,
            WebAdminSnapshotRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminWriteTarget target = snapshotTarget(snapshotId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.ROLLBACK_SNAPSHOT, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.ROLLBACK_SNAPSHOT, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "rollback_dry_run_preflight_failed"));
            return preflight;
        }
        RollbackPlan plan = buildRollbackPlan(server, snapshotId);
        if (!plan.blockers.isEmpty()) {
            WebAdminWriteResult result = okWithData(target, "回滚 dry-run 已完成，但存在阻断项。", Map.of("plan", plan));
            audit(context, result, Map.of(), Map.of("blockers", plan.blockers.size(), "operationCount", plan.operations.size()));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "回滚 dry-run 已完成。", Map.of("plan", plan));
        audit(context, result, Map.of(), Map.of("operationCount", plan.operations.size(), "dryRunFingerprint", plan.dryRunFingerprint));
        return result;
    }

    public WebAdminWriteResult applyRollback(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String snapshotId,
            WebAdminSnapshotRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminSnapshotRequest safeRequest = request == null ? new WebAdminSnapshotRequest() : request;
        WebAdminWriteTarget target = snapshotTarget(snapshotId);
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.ROLLBACK_SNAPSHOT, target);
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.ROLLBACK_SNAPSHOT, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "rollback_apply_preflight_failed"));
            return preflight;
        }
        if (!safeRequest.confirmed) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("confirmed", "confirmation_required", "回滚写入前需要二次确认。", "false")));
            audit(context, result, Map.of(), Map.of("attempt", "rollback_without_confirmation"));
            return result;
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation validation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_SNAPSHOT_ROLLBACK,
                    "timeline",
                    safeRequest.lockId,
                    user,
                    session
            );
            if (!validation.success()) {
                audit(context, validation.result(), Map.of(), Map.of("attempt", "rollback_edit_lock_failed"));
                return validation.result();
            }
        }
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(server);
        if (safeRequest.expectedFingerprint.isBlank()) {
            WebAdminWriteResult result = conflict(target, "回滚需要提交当前 manifest 指纹，请重新 dry-run 后再回滚。", "", manifestLoad.manifest().manifestFingerprint);
            audit(context, result, Map.of("expectedFingerprint", ""), Map.of("actualFingerprint", manifestLoad.manifest().manifestFingerprint));
            return result;
        }
        if (!safeRequest.expectedFingerprint.equals(manifestLoad.manifest().manifestFingerprint)) {
            WebAdminWriteResult result = conflict(target, "快照时间轴已变化，请重新 dry-run 后再回滚。", safeRequest.expectedFingerprint, manifestLoad.manifest().manifestFingerprint);
            audit(context, result, Map.of("expectedFingerprint", safeRequest.expectedFingerprint), Map.of("actualFingerprint", manifestLoad.manifest().manifestFingerprint));
            return result;
        }
        RollbackPlan plan = buildRollbackPlan(server, snapshotId);
        if (!plan.blockers.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, plan.blockers.stream()
                    .map(blocker -> new WebAdminValidationError("rollback", "rollback_blocked", blocker, ""))
                    .toList());
            audit(context, result, Map.of(), Map.of("blockers", plan.blockers.size()));
            return result;
        }
        if (safeRequest.dryRunFingerprint.isBlank() || !safeRequest.dryRunFingerprint.equals(plan.dryRunFingerprint)) {
            WebAdminWriteResult result = conflict(target, "Dry-run 指纹不一致，请重新预览回滚计划。", safeRequest.dryRunFingerprint, plan.dryRunFingerprint);
            audit(context, result, Map.of("expectedDryRunFingerprint", safeRequest.dryRunFingerprint), Map.of("actualDryRunFingerprint", plan.dryRunFingerprint));
            return result;
        }
        SnapshotTrigger trigger = new SnapshotTrigger();
        trigger.operation = WebAdminOperationType.ROLLBACK_SNAPSHOT.id();
        trigger.module = "Snapshot";
        trigger.targetType = "snapshot";
        trigger.targetId = safe(snapshotId);
        trigger.reason = "pre_rollback";
        SnapshotRecord preRollback = runSuppressed(() -> createSnapshot(
                server,
                SnapshotKind.PRE_ROLLBACK,
                username(user),
                "回滚前保护点：" + safe(snapshotId),
                "执行回滚前自动创建，用于撤回本次回滚。",
                List.of("pre-rollback"),
                trigger
        ));
        if (preRollback == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "回滚前保护点创建失败，已停止回滚。");
            audit(context, result, Map.of(), Map.of("attempt", "pre_rollback_failed"));
            return result;
        }
        SnapshotRecord annotatedPreRollback = updatePreRollbackOperationDiff(WebAdminSnapshotStore.snapshotRoot(server), preRollback.snapshotId, snapshotId);
        if (annotatedPreRollback != null) {
            preRollback = annotatedPreRollback;
        }
        try {
            applyRollbackFiles(server, plan);
            clearRestoredCaches(server);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to apply WebAdmin snapshot rollback {} after pre_rollback {}: {}", snapshotId, preRollback.snapshotId, exception.getMessage());
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "回滚写入失败，已创建回滚前保护点；详细错误请查看服务端日志。");
            audit(context, result, Map.of("preRollbackSnapshotId", preRollback.snapshotId), Map.of("attempt", "rollback_write_failed"));
            return result;
        }
        WebAdminWriteResult result = okWithData(target, "配置已回滚到选中的保存点。", Map.of(
                "plan", plan,
                "preRollbackSnapshotId", preRollback.snapshotId,
                "routeTarget", "#/snapshots"
        ));
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of("preRollbackSnapshotId", preRollback.snapshotId), Map.of("operationCount", plan.operations.size()));
        publishSnapshotRealtime(WebAdminRealtimeEventType.SNAPSHOT_ROLLBACK_APPLIED, preRollback, auditEvent, user, "配置快照回滚已应用。");
        publishSnapshotRealtime(WebAdminRealtimeEventType.SNAPSHOT_TIMELINE_CHANGED, preRollback, auditEvent, user, "配置快照时间轴已更新。");
        if (editLockService != null && !safeRequest.lockId.isBlank()) {
            editLockService.releaseAfterWrite(WebAdminEditLockService.TARGET_SNAPSHOT_ROLLBACK, "timeline", safeRequest.lockId, user, session, remoteAddress);
        }
        return result;
    }

    public RollbackPlan buildRollbackPlan(MinecraftServer server, String snapshotId) {
        return buildRollbackPlan(WebAdminSnapshotStore.snapshotRoot(server), WebAdminSnapshotStore.storeSpecs(server), snapshotId);
    }

    public RollbackPlan buildRollbackPlan(Path snapshotRoot, List<StoreSpec> storeSpecs, String snapshotId) {
        RollbackPlan plan = new RollbackPlan();
        plan.snapshotId = safe(snapshotId);
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        plan.manifestFingerprint = manifestLoad.manifest().manifestFingerprint;
        if (manifestLoad.degraded()) {
            plan.blockers.add(manifestLoad.message());
        }
        SnapshotRecord record = findRecord(manifestLoad.manifest(), snapshotId);
        if (record == null) {
            plan.blockers.add("目标快照不存在或 manifest 已损坏。");
            plan.dryRunFingerprint = fingerprintPlan(plan);
            return plan;
        }
        plan.targetSequence = record.sequence;
        PackageLoadResult targetLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, record);
        if (targetLoad.degraded() || targetLoad.pack() == null) {
            plan.blockers.add(targetLoad.message().isBlank() ? "目标快照数据不可读取。" : targetLoad.message());
            plan.dryRunFingerprint = fingerprintPlan(plan);
            return plan;
        }
        SnapshotCollectionResult currentCollection = WebAdminSnapshotStore.collect(storeSpecs);
        if (currentCollection.degraded()) {
            plan.blockers.addAll(currentCollection.warnings());
            plan.dryRunFingerprint = fingerprintPlan(plan);
            return plan;
        }
        SnapshotPackage currentPack = new SnapshotPackage();
        currentPack.snapshotId = "__current__";
        currentPack.resources = new ArrayList<>(currentCollection.resources());
        currentPack.packageFingerprint = WebAdminSnapshotStore.fingerprintPackage(currentPack);
        plan.currentFingerprint = currentPack.packageFingerprint;
        plan.targetFingerprint = targetLoad.pack().packageFingerprint;
        Map<String, SnapshotResource> currentFiles = WebAdminSnapshotStore.restoreResourcesByPath(currentPack);
        Map<String, SnapshotResource> targetFiles = WebAdminSnapshotStore.restoreResourcesByPath(targetLoad.pack());
        Set<String> pathKeys = new LinkedHashSet<>();
        (storeSpecs == null ? List.<StoreSpec>of() : storeSpecs).forEach(spec -> pathKeys.add(spec.pathKey()));
        pathKeys.addAll(currentFiles.keySet());
        pathKeys.addAll(targetFiles.keySet());
        for (String pathKey : pathKeys) {
            SnapshotResource current = currentFiles.get(pathKey);
            SnapshotResource target = targetFiles.get(pathKey);
            RollbackOperation op = new RollbackOperation();
            op.pathKey = pathKey;
            op.resourceId = pathKey;
            op.displayName = pathKey;
            op.beforeFingerprint = current == null ? "" : current.fingerprint;
            op.afterFingerprint = target == null ? "" : target.fingerprint;
            if (current == null && target != null) {
                op.operation = "create";
            } else if (current != null && target == null) {
                op.operation = "delete";
                op.destructive = true;
            } else if (current != null && !current.fingerprint.equals(target.fingerprint)) {
                op.operation = "update";
            } else {
                op.operation = "unchanged";
            }
            if (!"unchanged".equals(op.operation)) {
                plan.operations.add(op);
            }
        }
        plan.summary = diff(WebAdminSnapshotStore.diffResourcesByKey(currentPack), WebAdminSnapshotStore.diffResourcesByKey(targetLoad.pack())).summary;
        plan.warnings.add("回滚只恢复 8.18 allowlist 中的配置文件，不恢复 runtime history、Timer 运行中实例、Signal Join pending state、玩家实时背包或世界实体。");
        plan.dryRunFingerprint = fingerprintPlan(plan);
        return plan;
    }

    public SnapshotRecord createSnapshot(
            MinecraftServer server,
            SnapshotKind kind,
            String actor,
            String title,
            String note,
            List<String> tags,
            SnapshotTrigger trigger
    ) {
        return createSnapshot(WebAdminSnapshotStore.snapshotRoot(server), WebAdminSnapshotStore.collect(server), kind, actor, title, note, tags, trigger);
    }

    public SnapshotRecord createSnapshot(
            Path snapshotRoot,
            SnapshotCollectionResult collection,
            SnapshotKind kind,
            String actor,
            String title,
            String note,
            List<String> tags,
            SnapshotTrigger trigger
    ) {
        if (collection == null || collection.degraded()) {
            Tzz_mod.LOGGER.warn("Skipped WebAdmin snapshot because collection is degraded: {}", collection == null ? "" : collection.warnings());
            return null;
        }
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        if (manifestLoad.degraded()) {
            Tzz_mod.LOGGER.warn("Skipped WebAdmin snapshot because manifest is degraded: {}", manifestLoad.message());
            return null;
        }
        SnapshotManifest manifest = manifestLoad.manifest();
        long sequence = Math.max(1L, manifest.nextSequence);
        String snapshotId = "snap-" + sequence + "-" + UUID.randomUUID().toString().substring(0, 8);
        SnapshotPackage pack = new SnapshotPackage();
        pack.snapshotId = snapshotId;
        pack.sequence = sequence;
        pack.createdAt = Instant.now().toString();
        pack.createdBy = safe(actor);
        pack.kind = (kind == null ? SnapshotKind.AUTO : kind).id();
        pack.trigger = trigger == null ? new SnapshotTrigger() : trigger.normalized();
        pack.resources = new ArrayList<>(collection.resources());
        pack.warnings = new ArrayList<>(collection.warnings());
        pack.packageFingerprint = WebAdminSnapshotStore.fingerprintPackage(pack);
        if (!WebAdminSnapshotStore.savePackage(snapshotRoot, pack)) {
            return null;
        }
        SnapshotRecord record = new SnapshotRecord();
        record.snapshotId = snapshotId;
        record.sequence = sequence;
        record.createdAt = pack.createdAt;
        record.createdBy = pack.createdBy;
        record.kind = pack.kind;
        record.title = safe(title).isBlank() ? defaultTitle(kind, trigger) : safe(title);
        record.note = safe(note);
        record.tags = tags == null ? List.of() : tags;
        record.trigger = pack.trigger;
        record.previousSnapshotId = manifest.records.isEmpty() ? "" : manifest.records.get(manifest.records.size() - 1).snapshotId;
        record.resourceCounts = resourceCounts(pack);
        SnapshotRecord previousRecord = record.previousSnapshotId.isBlank() ? null : findRecord(manifest, record.previousSnapshotId);
        SnapshotPackage previous = previousRecord == null ? null : WebAdminSnapshotStore.loadPackage(snapshotRoot, previousRecord).pack();
        record.diffSummary = diffAgainstPreviousRecord(previousRecord, previous, pack).summary;
        record.packageFingerprint = pack.packageFingerprint;
        record.storagePath = WebAdminSnapshotStore.SNAPSHOT_DIR + "/" + WebAdminSnapshotStore.SNAPSHOT_DATA_DIR + "/" + snapshotId + ".json";
        record.warnings = new ArrayList<>(collection.warnings());
        manifest.records.add(record.normalized());
        manifest.nextSequence = sequence + 1L;
        WebAdminSnapshotStore.applyAutoRetention(snapshotRoot, manifest);
        if (!WebAdminSnapshotStore.saveManifest(snapshotRoot, manifest)) {
            return null;
        }
        return record.normalized();
    }

    public SnapshotRecord updateAutoSnapshotOperationDiff(MinecraftServer server, SnapshotRecord record) {
        if (server == null || record == null || safe(record.snapshotId).isBlank()) {
            return null;
        }
        return updateAutoSnapshotOperationDiff(WebAdminSnapshotStore.snapshotRoot(server), WebAdminSnapshotStore.collect(server), record.snapshotId);
    }

    public SnapshotRecord updateAutoSnapshotOperationDiff(Path snapshotRoot, SnapshotCollectionResult currentCollection, String snapshotId) {
        if (snapshotRoot == null || currentCollection == null || currentCollection.degraded() || safe(snapshotId).isBlank()) {
            return null;
        }
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        if (manifestLoad.degraded()) {
            return null;
        }
        SnapshotManifest manifest = manifestLoad.manifest();
        SnapshotRecord record = findRecord(manifest, snapshotId);
        if (record == null || !"auto".equals(record.kind)) {
            return null;
        }
        PackageLoadResult beforeLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, record);
        if (beforeLoad.degraded() || beforeLoad.pack() == null) {
            return null;
        }
        SnapshotPackage current = new SnapshotPackage();
        current.snapshotId = "operation-current-" + snapshotId;
        current.sequence = record.sequence;
        current.createdAt = Instant.now().toString();
        current.createdBy = record.createdBy;
        current.kind = SnapshotKind.AUTO.id();
        current.trigger = record.trigger == null ? new SnapshotTrigger() : record.trigger.normalized();
        current.resources = new ArrayList<>(currentCollection.resources());
        current.packageFingerprint = WebAdminSnapshotStore.fingerprintPackage(current);
        SnapshotDiff operationDiff = diff(beforeLoad.pack(), current).normalized();
        record.operationDiff = operationDiff;
        for (int i = 0; i < manifest.records.size(); i++) {
            SnapshotRecord candidate = manifest.records.get(i);
            if (candidate != null && snapshotId.equals(candidate.snapshotId)) {
                manifest.records.set(i, record.normalized());
                break;
            }
        }
        if (!WebAdminSnapshotStore.saveManifest(snapshotRoot, manifest)) {
            return null;
        }
        return record.normalized();
    }

    public SnapshotRecord updatePreRollbackOperationDiff(Path snapshotRoot, String preRollbackSnapshotId, String targetSnapshotId) {
        if (snapshotRoot == null || safe(preRollbackSnapshotId).isBlank() || safe(targetSnapshotId).isBlank()) {
            return null;
        }
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        if (manifestLoad.degraded()) {
            return null;
        }
        SnapshotManifest manifest = manifestLoad.manifest();
        SnapshotRecord preRollback = findRecord(manifest, preRollbackSnapshotId);
        SnapshotRecord target = findRecord(manifest, targetSnapshotId);
        if (preRollback == null || target == null || !"pre_rollback".equals(preRollback.kind)) {
            return null;
        }
        PackageLoadResult beforeLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, preRollback);
        PackageLoadResult afterLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, target);
        if (beforeLoad.degraded() || afterLoad.degraded() || beforeLoad.pack() == null || afterLoad.pack() == null) {
            return null;
        }
        preRollback.operationDiff = diff(beforeLoad.pack(), afterLoad.pack()).normalized();
        for (int i = 0; i < manifest.records.size(); i++) {
            SnapshotRecord candidate = manifest.records.get(i);
            if (candidate != null && preRollbackSnapshotId.equals(candidate.snapshotId)) {
                manifest.records.set(i, preRollback.normalized());
                break;
            }
        }
        if (!WebAdminSnapshotStore.saveManifest(snapshotRoot, manifest)) {
            return null;
        }
        return preRollback.normalized();
    }

    private void applyRollbackFiles(MinecraftServer server, RollbackPlan plan) throws Exception {
        applyRollbackFiles(WebAdminSnapshotStore.snapshotRoot(server), WebAdminSnapshotStore.storeSpecs(server), plan);
    }

    public void applyRollbackFiles(Path snapshotRoot, List<StoreSpec> storeSpecs, RollbackPlan plan) throws Exception {
        ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        SnapshotRecord record = findRecord(manifestLoad.manifest(), plan.snapshotId);
        if (record == null) {
            throw new IllegalStateException("目标快照不存在或 manifest 已损坏。");
        }
        PackageLoadResult targetLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, record);
        if (targetLoad.degraded() || targetLoad.pack() == null) {
            throw new IllegalStateException(targetLoad.message().isBlank() ? "目标快照数据不可读取。" : targetLoad.message());
        }
        if (!safe(plan.targetFingerprint).isBlank() && !safe(plan.targetFingerprint).equals(targetLoad.pack().packageFingerprint)) {
            throw new IllegalStateException("目标快照数据指纹已变化，请重新 dry-run。");
        }
        Map<String, SnapshotResource> targetFiles = WebAdminSnapshotStore.restoreResourcesByPath(targetLoad.pack());
        Map<String, Path> paths = new LinkedHashMap<>();
        (storeSpecs == null ? List.<StoreSpec>of() : storeSpecs).forEach(spec -> paths.put(spec.pathKey(), Path.of(spec.relativePath())));
        Map<Path, String> plannedWrites = new LinkedHashMap<>();
        List<Path> plannedDeletes = new ArrayList<>();
        for (RollbackOperation operation : plan.operations) {
            Path path = paths.get(operation.pathKey);
            if (path == null) {
                continue;
            }
            if ("delete".equals(operation.operation)) {
                plannedDeletes.add(path);
                continue;
            }
            SnapshotResource target = targetFiles.get(operation.pathKey);
            if (target == null) {
                continue;
            }
            plannedWrites.put(path, rollbackWriteContent(path, operation.pathKey, target.canonicalJson));
        }
        Path stagingDir = snapshotRoot.resolve("rollback-staging").resolve(UUID.randomUUID().toString());
        Map<Path, Path> stagedWrites = new LinkedHashMap<>();
        try {
            Files.createDirectories(stagingDir);
            for (Map.Entry<Path, String> entry : plannedWrites.entrySet()) {
                JsonParser.parseString(entry.getValue());
                Path staged = stagingDir.resolve(WebAdminSnapshotStore.safeSnapshotId(entry.getKey().getFileName().toString()) + ".tmp");
                Files.writeString(staged, entry.getValue(), StandardCharsets.UTF_8);
                stagedWrites.put(entry.getKey(), staged);
            }
            for (Map.Entry<Path, Path> entry : stagedWrites.entrySet()) {
                Path target = entry.getKey();
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                moveStagedFile(entry.getValue(), target);
            }
            for (Path path : plannedDeletes) {
                Files.deleteIfExists(path);
            }
        } finally {
            deleteDirectoryIfExists(stagingDir);
        }
    }

    private static String rollbackWriteContent(Path currentPath, String pathKey, String targetCanonicalJson) throws Exception {
        JsonElement parsed = JsonParser.parseString(targetCanonicalJson);
        if ("state_variables".equals(pathKey)) {
            return mergeStateVariableDefinitionsForRollback(currentPath, parsed);
        }
        if ("signal_devices".equals(pathKey)) {
            return mergeSignalDeviceRuntimeForRollback(currentPath, parsed);
        }
        return WebAdminSnapshotStore.canonicalJson(parsed);
    }

    private static String mergeSignalDeviceRuntimeForRollback(Path currentPath, JsonElement targetParsed) throws Exception {
        if (!targetParsed.isJsonObject()) {
            return WebAdminSnapshotStore.canonicalJson(targetParsed);
        }
        JsonObject targetRoot = targetParsed.getAsJsonObject();
        JsonElement targetDevices = targetRoot.get("devices");
        if (targetDevices == null || !targetDevices.isJsonArray() || currentPath == null || !Files.exists(currentPath)) {
            return WebAdminSnapshotStore.canonicalJson(targetRoot);
        }
        JsonObject currentRoot = JsonParser.parseString(Files.readString(currentPath, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonElement currentDevices = currentRoot.get("devices");
        if (currentDevices == null || !currentDevices.isJsonArray()) {
            return WebAdminSnapshotStore.canonicalJson(targetRoot);
        }
        Map<String, JsonObject> currentById = new LinkedHashMap<>();
        for (JsonElement element : currentDevices.getAsJsonArray()) {
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                String id = stringMember(object, "id");
                if (!id.isBlank()) {
                    currentById.put(id, object);
                }
            }
        }
        JsonObject mergedRoot = targetRoot.deepCopy();
        JsonArray mergedDevices = new JsonArray();
        for (JsonElement element : targetDevices.getAsJsonArray()) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject targetDevice = element.getAsJsonObject().deepCopy();
            JsonObject currentDevice = currentById.get(stringMember(targetDevice, "id"));
            if (currentDevice != null) {
                preserveSignalDeviceRuntimeMembers(currentDevice, targetDevice);
            }
            mergedDevices.add(targetDevice);
        }
        mergedRoot.add("devices", mergedDevices);
        return WebAdminSnapshotStore.canonicalJson(mergedRoot);
    }

    private static void preserveSignalDeviceRuntimeMembers(JsonObject currentDevice, JsonObject targetDevice) {
        for (String field : List.of(
                "remainingPulseTicks",
                "lastTriggerGameTime",
                "lastTriggerWallTimeMillis",
                "lastResult",
                "lastPowered",
                "lastPowerLevel",
                "lastConditionMatched",
                "lastConditionCheckGameTime",
                "lastConditionResult",
                "lastInteractionGameTime",
                "lastInteractionWallTimeMillis",
                "lastInteractionPlayerName",
                "lastInteractionPlayerUuid",
                "lastInteractionResult",
                "lastInteractionHand",
                "lastInteractionSide",
                "lastContainerCheckGameTime",
                "lastContainerFingerprint",
                "lastContainerOpenGameTime",
                "lastContainerOpenWallTimeMillis",
                "lastContainerCloseGameTime",
                "lastContainerCloseWallTimeMillis",
                "lastContainerChangeGameTime",
                "lastContainerChangeWallTimeMillis",
                "lastContainerPlayerName",
                "lastContainerPlayerUuid",
                "lastContainerResult",
                "lastContainerEventType",
                "lastInteractionItemMatched",
                "lastInteractionItemResult",
                "lastItemSubmitMatched",
                "lastItemSubmitFailureReason",
                "lastItemSubmitConsumedSummary",
                "lastItemSubmitResult"
        )) {
            copyJsonMember(currentDevice, targetDevice, field);
        }
    }

    private static String mergeStateVariableDefinitionsForRollback(Path currentPath, JsonElement targetParsed) throws Exception {
        if (!targetParsed.isJsonObject()) {
            return WebAdminSnapshotStore.canonicalJson(targetParsed);
        }
        JsonObject targetRoot = targetParsed.getAsJsonObject();
        JsonElement targetVariables = targetRoot.get("variables");
        if (targetVariables == null || !targetVariables.isJsonArray() || currentPath == null || !Files.exists(currentPath)) {
            return WebAdminSnapshotStore.canonicalJson(targetRoot);
        }
        JsonObject currentRoot = JsonParser.parseString(Files.readString(currentPath, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonElement currentVariables = currentRoot.get("variables");
        if (currentVariables == null || !currentVariables.isJsonArray()) {
            return WebAdminSnapshotStore.canonicalJson(targetRoot);
        }
        Map<String, JsonObject> currentByIdentity = new LinkedHashMap<>();
        for (JsonElement element : currentVariables.getAsJsonArray()) {
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                currentByIdentity.put(stateVariableIdentity(object), object);
            }
        }
        JsonObject mergedRoot = targetRoot.deepCopy();
        JsonArray mergedVariables = new JsonArray();
        for (JsonElement element : targetVariables.getAsJsonArray()) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject targetVariable = element.getAsJsonObject().deepCopy();
            JsonObject currentVariable = currentByIdentity.get(stateVariableIdentity(targetVariable));
            if (currentVariable != null) {
                preserveStateVariableRuntimeValue(currentVariable, targetVariable);
            }
            mergedVariables.add(targetVariable);
        }
        mergedRoot.add("variables", mergedVariables);
        return WebAdminSnapshotStore.canonicalJson(mergedRoot);
    }

    private static void preserveStateVariableRuntimeValue(JsonObject currentVariable, JsonObject targetVariable) {
        String currentValue = stringMember(currentVariable, "value");
        StateVariableType targetType = stateVariableType(targetVariable);
        try {
            List<StateVariableValidation.Issue> issues = new ArrayList<>();
            StateVariableValidation.validateValue(targetType, currentValue, issues);
            if (issues.isEmpty()) {
                targetVariable.addProperty("value", StateVariableValidation.normalizeValue(targetType, currentValue));
                copyJsonMember(currentVariable, targetVariable, "updatedAt");
                copyJsonMember(currentVariable, targetVariable, "updatedBy");
                copyJsonMember(currentVariable, targetVariable, "version");
                targetVariable.addProperty("fingerprint", "");
            }
        } catch (RuntimeException ignored) {
            targetVariable.addProperty("fingerprint", "");
        }
    }

    private static StateVariableType stateVariableType(JsonObject variable) {
        String raw = stringMember(variable, "type").trim().toUpperCase(Locale.ROOT);
        for (StateVariableType type : StateVariableType.values()) {
            if (type.name().equals(raw)) {
                return type;
            }
        }
        return StateVariableType.STRING;
    }

    private static String stateVariableIdentity(JsonObject variable) {
        String id = stringMember(variable, "id");
        if (!id.isBlank()) {
            return "id:" + id;
        }
        return String.join("\n",
                stringMember(variable, "scope"),
                stringMember(variable, "targetId"),
                stringMember(variable, "key")
        );
    }

    private static String stringMember(JsonObject object, String name) {
        if (object == null) {
            return "";
        }
        JsonElement member = object.get(name);
        return member != null && member.isJsonPrimitive() ? member.getAsString() : "";
    }

    private static void copyJsonMember(JsonObject source, JsonObject target, String name) {
        JsonElement member = source == null ? null : source.get(name);
        if (member != null) {
            target.add(name, member.deepCopy());
        }
    }

    private static void moveStagedFile(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteDirectoryIfExists(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception exception) {
                    Tzz_mod.LOGGER.warn("Failed to clean WebAdmin snapshot rollback staging file {}: {}", path, exception.getMessage());
                }
            });
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to clean WebAdmin snapshot rollback staging dir {}: {}", directory, exception.getMessage());
        }
    }

    private void clearRestoredCaches(MinecraftServer server) {
        try {
            SignalDeviceStore.clearCache(server);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to clear SignalDevice cache after snapshot rollback: {}", exception.getMessage());
        }
        try {
            SignalListenerStore.clearCache(server);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to clear SignalListener cache after snapshot rollback: {}", exception.getMessage());
        }
        try {
            SignalJoinStore.clearCachedLoad(server);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to clear SignalJoin cache after snapshot rollback: {}", exception.getMessage());
        }
        try {
            RegionControllerStore.clearCache(server);
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to clear RegionController cache after snapshot rollback: {}", exception.getMessage());
        }
        try {
            StateVariableStore.flushDirty(server);
            ConditionRuntimeGateStore.loadWithStatus(server);
            TimerRuntimeService.replaceDefinitionCache(server, TimerStore.loadWithStatus(server));
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to refresh WebAdmin config caches after snapshot rollback: {}", exception.getMessage());
        }
    }

    private WebAdminWriteResult writePreflight(WebAdminUser user, WebAdminSession session, String csrfToken, boolean sameOrigin, WebAdminOperationType operation, WebAdminWriteTarget target) {
        WebAdminPermissionDecision permission = permissionService.decide(user, operation);
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
        return WebAdminWriteResult.ok(target, false, "快照写入安全检查通过。");
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private static WebAdminWriteContext autoAuditContext(WebAdminUser user, WebAdminOperationType operationType, SnapshotRecord record) {
        WebAdminWriteTarget target = record == null ? SNAPSHOT_TARGET : snapshotTarget(record.snapshotId);
        return WebAdminWriteContext.of(user, null, "auto_snapshot", WebAdminOperationType.CREATE_SNAPSHOT, target);
    }

    private static Map<String, Object> autoSnapshotAuditData(WebAdminOperationType operationType, String module, String targetType, String targetId, String attempt) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("attempt", safe(attempt));
        data.put("triggerOperation", operationType == null ? "" : operationType.id());
        data.put("triggerModule", safe(module));
        data.put("triggerTargetType", safe(targetType));
        data.put("triggerTargetId", safe(targetId));
        data.put("autoSnapshotAudit", true);
        return data;
    }

    private void publishSnapshotRealtime(WebAdminRealtimeEventType type, SnapshotRecord record, WebAdminAuditEvent auditEvent, WebAdminUser user, String summary) {
        WebAdminRealtimeEvent event = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(type)
                .severity("INFO")
                .summary(summary)
                .routeTarget("#/snapshots")
                .payload("targetType", "snapshot_timeline")
                .payload("snapshotId", record == null ? "" : record.snapshotId)
                .payload("sequence", Long.toString(record == null ? 0L : record.sequence))
                .payload("kind", record == null ? "" : record.kind)
                .payload("actor", username(user))
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
        if (auditEvent == null) {
            return;
        }
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .severity("INFO")
                .summary("WebAdmin 快照审计已记录。")
                .routeTarget("#/snapshots")
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", event == null ? "" : event.id()));
    }

    public static SnapshotDiff diff(SnapshotPackage previous, SnapshotPackage current) {
        return diff(WebAdminSnapshotStore.diffResourcesByKey(previous), WebAdminSnapshotStore.diffResourcesByKey(current));
    }

    private static SnapshotDiff diffAgainstPreviousRecord(SnapshotRecord previousRecord, SnapshotPackage previous, SnapshotPackage current) {
        SnapshotDiff raw = diff(previous, current);
        if (previousRecord == null
                || previousRecord.operationDiff == null
                || previousRecord.operationDiff.summary == null
                || previousRecord.operationDiff.summary.changed() <= 0) {
            return raw;
        }
        Set<String> carriedOperationKeys = previousRecord.operationDiff.entries == null
                ? Set.of()
                : previousRecord.operationDiff.entries.stream()
                .filter(entry -> entry != null && !"unchanged".equals(entry.changeType))
                .map(WebAdminSnapshotService::diffEntryIdentity)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (carriedOperationKeys.isEmpty()) {
            return raw;
        }
        SnapshotDiff result = new SnapshotDiff();
        for (SnapshotDiffEntry entry : raw.entries) {
            if (entry != null && !"unchanged".equals(entry.changeType) && carriedOperationKeys.contains(diffEntryIdentity(entry))) {
                continue;
            }
            result.entries.add(entry == null ? new SnapshotDiffEntry() : entry.normalized());
        }
        rebuildSummary(result);
        return result.normalized();
    }

    private static String diffEntryIdentity(SnapshotDiffEntry entry) {
        if (entry == null) {
            return "";
        }
        return safe(entry.changeType) + "\n"
                + safe(entry.resourceType) + "\n"
                + safe(entry.resourceId) + "\n"
                + safe(entry.beforeFingerprint) + "\n"
                + safe(entry.afterFingerprint);
    }

    private static void rebuildSummary(SnapshotDiff diff) {
        SnapshotDiffSummary summary = new SnapshotDiffSummary();
        if (diff != null && diff.entries != null) {
            for (SnapshotDiffEntry entry : diff.entries) {
                if (entry == null) {
                    continue;
                }
                String type = safe(entry.changeType);
                if ("created".equals(type)) {
                    summary.created++;
                } else if ("updated".equals(type)) {
                    summary.updated++;
                } else if ("deleted".equals(type)) {
                    summary.deleted++;
                } else {
                    summary.unchanged++;
                }
                summary.byType.merge(safe(entry.resourceType), "unchanged".equals(type) ? 0 : 1, Integer::sum);
            }
        }
        diff.summary = summary;
    }

    private static SnapshotDiff diff(Map<String, SnapshotResource> previous, Map<String, SnapshotResource> current) {
        SnapshotDiff diff = new SnapshotDiff();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(previous.keySet());
        keys.addAll(current.keySet());
        for (String key : keys) {
            SnapshotResource before = previous.get(key);
            SnapshotResource after = current.get(key);
            SnapshotDiffEntry entry = new SnapshotDiffEntry();
            SnapshotResource sample = after == null ? before : after;
            entry.resourceType = sample == null ? "" : sample.resourceType;
            entry.resourceId = sample == null ? "" : sample.resourceId;
            entry.displayName = sample == null ? "" : sample.displayName;
            entry.sourceStore = sample == null ? "" : sample.sourceStore;
            entry.beforeFingerprint = before == null ? "" : before.fingerprint;
            entry.afterFingerprint = after == null ? "" : after.fingerprint;
            entry.beforeSummary = resourceSummary(before);
            entry.afterSummary = resourceSummary(after);
            entry.beforeJsonPreview = jsonPreview(before);
            entry.afterJsonPreview = jsonPreview(after);
            if (before == null && after != null) {
                entry.changeType = "created";
                diff.summary.created++;
            } else if (before != null && after == null) {
                entry.changeType = "deleted";
                diff.summary.deleted++;
            } else if (before != null && !before.fingerprint.equals(after.fingerprint)) {
                entry.changeType = "updated";
                applyFieldDiffs(entry, before, after);
                diff.summary.updated++;
            } else {
                entry.changeType = "unchanged";
                diff.summary.unchanged++;
            }
            diff.summary.byType.merge(entry.resourceType, "unchanged".equals(entry.changeType) ? 0 : 1, Integer::sum);
            diff.entries.add(entry);
        }
        diff.entries.sort(Comparator.comparing((SnapshotDiffEntry entry) -> entry.changeType).thenComparing(entry -> entry.resourceType).thenComparing(entry -> entry.resourceId));
        return diff;
    }

    private static String resourceSummary(SnapshotResource resource) {
        if (resource == null) {
            return "";
        }
        String display = safe(resource.displayName).isBlank() ? safe(resource.resourceId) : safe(resource.displayName);
        String fingerprint = safe(resource.fingerprint);
        String shortFingerprint = fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint;
        return safe(resource.resourceType) + " / " + display + " / " + safe(resource.sourceStore) + " / " + shortFingerprint;
    }

    private static String jsonPreview(SnapshotResource resource) {
        if (resource == null || safe(resource.canonicalJson).isBlank()) {
            return "";
        }
        return truncateForDiff(resource.canonicalJson, DIFF_JSON_PREVIEW_LIMIT);
    }

    private static void applyFieldDiffs(SnapshotDiffEntry entry, SnapshotResource before, SnapshotResource after) {
        List<SnapshotFieldDiff> fields = jsonFieldDiffs(before == null ? "" : before.canonicalJson, after == null ? "" : after.canonicalJson);
        if (fields.size() > DIFF_FIELD_LIMIT) {
            entry.omittedFieldDiffs = fields.size() - DIFF_FIELD_LIMIT;
            entry.fieldDiffs = new ArrayList<>(fields.subList(0, DIFF_FIELD_LIMIT));
        } else {
            entry.fieldDiffs = fields;
        }
    }

    private static List<SnapshotFieldDiff> jsonFieldDiffs(String beforeJson, String afterJson) {
        List<SnapshotFieldDiff> result = new ArrayList<>();
        JsonElement before = parseJsonElement(beforeJson);
        JsonElement after = parseJsonElement(afterJson);
        if (before == null || after == null || !before.isJsonObject() || !after.isJsonObject()) {
            SnapshotFieldDiff field = new SnapshotFieldDiff();
            field.field = "$";
            field.changeType = "updated";
            field.beforeValue = fingerprintSummary(beforeJson);
            field.afterValue = fingerprintSummary(afterJson);
            result.add(field);
            return result;
        }
        JsonObject beforeObject = before.getAsJsonObject();
        JsonObject afterObject = after.getAsJsonObject();
        Set<String> names = new LinkedHashSet<>();
        beforeObject.keySet().stream().sorted().forEach(names::add);
        afterObject.keySet().stream().sorted().forEach(names::add);
        for (String name : names) {
            JsonElement beforeValue = beforeObject.get(name);
            JsonElement afterValue = afterObject.get(name);
            if (jsonEquivalent(beforeValue, afterValue)) {
                continue;
            }
            SnapshotFieldDiff field = new SnapshotFieldDiff();
            field.field = name;
            field.changeType = beforeValue == null ? "created" : afterValue == null ? "deleted" : "updated";
            field.beforeValue = summarizeJsonValue(beforeValue);
            field.afterValue = summarizeJsonValue(afterValue);
            result.add(field);
        }
        if (result.isEmpty()) {
            SnapshotFieldDiff field = new SnapshotFieldDiff();
            field.field = "$";
            field.changeType = "updated";
            field.beforeValue = fingerprintSummary(beforeJson);
            field.afterValue = fingerprintSummary(afterJson);
            result.add(field);
        }
        return result;
    }

    private static JsonElement parseJsonElement(String json) {
        try {
            String safe = safe(json);
            return safe.isBlank() ? null : JsonParser.parseString(safe);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean jsonEquivalent(JsonElement before, JsonElement after) {
        if (before == null && after == null) {
            return true;
        }
        if (before == null || after == null) {
            return false;
        }
        return before.equals(after);
    }

    private static String summarizeJsonValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (value.isJsonPrimitive()) {
            return truncateForDiff(value.getAsJsonPrimitive().toString(), DIFF_VALUE_LIMIT);
        }
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            return "列表，数量 " + array.size() + "，hash " + shortHash(value.toString());
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            return "对象，字段 " + object.size() + "，hash " + shortHash(value.toString());
        }
        return truncateForDiff(value.toString(), DIFF_VALUE_LIMIT);
    }

    private static String fingerprintSummary(String value) {
        String safe = safe(value);
        return safe.isBlank() ? "" : "hash " + shortHash(safe) + "，长度 " + safe.length();
    }

    private static String shortHash(String value) {
        String hash = WebAdminSnapshotStore.hash(safe(value));
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private static String truncateForDiff(String value, int limit) {
        String safe = safe(value);
        if (safe.length() <= limit) {
            return safe;
        }
        return safe.substring(0, Math.max(0, limit)) + "\n... 已截断，原长度 " + safe.length();
    }

    private static SnapshotRecord findRecord(SnapshotManifest manifest, String snapshotId) {
        String safeId = safe(snapshotId);
        if (manifest == null || manifest.records == null) {
            return null;
        }
        return manifest.records.stream()
                .filter(record -> safeId.equals(record.snapshotId))
                .findFirst()
                .orElse(null);
    }

    private static SnapshotRecord previousRecord(SnapshotManifest manifest, SnapshotRecord record) {
        if (manifest == null || record == null || manifest.records == null) {
            return null;
        }
        if (!record.previousSnapshotId.isBlank()) {
            SnapshotRecord previous = findRecord(manifest, record.previousSnapshotId);
            if (previous != null) {
                return previous;
            }
        }
        return manifest.records.stream()
                .filter(candidate -> candidate.sequence < record.sequence)
                .max(Comparator.comparingLong(candidate -> candidate.sequence))
                .orElse(null);
    }

    private static Map<String, Integer> resourceCounts(SnapshotPackage pack) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (pack != null && pack.resources != null) {
            for (SnapshotResource resource : pack.resources) {
                if (resource != null && !resource.restoreResource) {
                    counts.merge(resource.resourceType, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static SnapshotRecord viewRecord(SnapshotRecord raw) {
        SnapshotRecord record = raw == null ? new SnapshotRecord() : raw.normalized();
        record.storagePath = record.storagePath.isBlank()
                ? ""
                : WebAdminSnapshotStore.SNAPSHOT_DIR + "/" + WebAdminSnapshotStore.SNAPSHOT_DATA_DIR + "/" + record.snapshotId + ".json";
        return record;
    }

    private SnapshotRecord viewRecordWithEffectiveSummary(MinecraftServer server, SnapshotManifest manifest, SnapshotRecord raw) {
        SnapshotRecord record = viewRecord(raw);
        if (server == null || manifest == null || safe(record.snapshotId).isBlank()) {
            return record;
        }
        SnapshotRecord previous = previousRecord(manifest, record);
        if (previous == null || previous.operationDiff == null || previous.operationDiff.summary == null || previous.operationDiff.summary.changed() <= 0) {
            return record;
        }
        PackageLoadResult currentLoad = WebAdminSnapshotStore.loadPackage(server, record);
        PackageLoadResult previousLoad = WebAdminSnapshotStore.loadPackage(server, previous);
        if (currentLoad.degraded() || previousLoad.degraded() || currentLoad.pack() == null) {
            return record;
        }
        record.operationDiff = effectiveOperationDiff(manifest, record, currentLoad.pack(), WebAdminSnapshotStore.snapshotRoot(server));
        record.diffSummary = diffAgainstPreviousRecord(previous, previousLoad.pack(), currentLoad.pack()).summary;
        return record;
    }

    private static SnapshotDiff effectiveOperationDiff(SnapshotManifest manifest, SnapshotRecord record, SnapshotPackage recordPackage, Path snapshotRoot) {
        SnapshotDiff existing = record == null || record.operationDiff == null ? new SnapshotDiff() : record.operationDiff.normalized();
        if (existing.summary != null && existing.summary.changed() > 0) {
            return existing;
        }
        if (manifest == null
                || record == null
                || !"pre_rollback".equals(record.kind)
                || record.trigger == null
                || safe(record.trigger.targetId).isBlank()
                || recordPackage == null
                || snapshotRoot == null) {
            return existing;
        }
        SnapshotRecord target = findRecord(manifest, record.trigger.targetId);
        if (target == null) {
            return existing;
        }
        PackageLoadResult targetLoad = WebAdminSnapshotStore.loadPackage(snapshotRoot, target);
        if (targetLoad.degraded() || targetLoad.pack() == null) {
            return existing;
        }
        return diff(recordPackage, targetLoad.pack()).normalized();
    }

    private static SnapshotResource viewResource(SnapshotResource raw) {
        SnapshotResource resource = raw == null ? new SnapshotResource() : raw.normalized();
        resource.canonicalJson = "";
        if (resource.metadata != null && resource.metadata.containsKey("path")) {
            Map<String, String> metadata = new LinkedHashMap<>(resource.metadata);
            metadata.put("path", resource.pathKey);
            resource.metadata = metadata;
        }
        return resource;
    }

    private static Map<String, Object> stats(List<SnapshotRecord> records) {
        Map<String, Object> stats = new LinkedHashMap<>();
        int manual = 0;
        int auto = 0;
        int preRollback = 0;
        for (SnapshotRecord record : records == null ? List.<SnapshotRecord>of() : records) {
            if ("manual".equals(record.kind)) {
                manual++;
            } else if ("pre_rollback".equals(record.kind)) {
                preRollback++;
            } else {
                auto++;
            }
        }
        stats.put("total", records == null ? 0 : records.size());
        stats.put("manual", manual);
        stats.put("auto", auto);
        stats.put("preRollback", preRollback);
        return stats;
    }

    private static Map<String, Object> filterOptions(List<SnapshotRecord> records) {
        Set<String> modules = new LinkedHashSet<>();
        Set<String> users = new LinkedHashSet<>();
        Set<String> resourceTypes = new LinkedHashSet<>();
        for (SnapshotRecord record : records == null ? List.<SnapshotRecord>of() : records) {
            if (record.trigger != null && !safe(record.trigger.module).isBlank()) {
                modules.add(record.trigger.module);
            }
            if (!safe(record.createdBy).isBlank()) {
                users.add(record.createdBy);
            }
            resourceTypes.addAll(changedResourceTypes(record));
        }
        return Map.of("modules", List.copyOf(modules), "users", List.copyOf(users), "resourceTypes", List.copyOf(resourceTypes));
    }

    private static boolean matches(SnapshotRecord record, Map<String, String> query) {
        String kind = safe(query.get("kind"));
        if (!kind.isBlank() && !"ALL".equalsIgnoreCase(kind) && !kind.equalsIgnoreCase(record.kind)) {
            return false;
        }
        String module = safe(query.get("module"));
        if (!module.isBlank() && !"ALL".equalsIgnoreCase(module) && (record.trigger == null || !module.equalsIgnoreCase(record.trigger.module))) {
            return false;
        }
        String user = safe(query.get("user"));
        if (!user.isBlank() && !"ALL".equalsIgnoreCase(user) && !user.equalsIgnoreCase(record.createdBy)) {
            return false;
        }
        String resource = safe(query.get("resource"));
        if (!resource.isBlank() && !"ALL".equalsIgnoreCase(resource) && changedResourceTypes(record).stream().noneMatch(resource::equalsIgnoreCase)) {
            return false;
        }
        String search = safe(query.get("search")).toLowerCase(Locale.ROOT);
        if (!search.isBlank()) {
            String haystack = String.join(" ",
                    record.snapshotId,
                    record.title,
                    record.note,
                    record.createdBy,
                    snapshotKindLabel(record.kind),
                    snapshotModuleLabel(record.trigger == null ? "" : record.trigger.module),
                    snapshotOperationLabel(record.trigger == null ? "" : record.trigger.operation),
                    record.trigger == null ? "" : record.trigger.targetId,
                    record.trigger == null ? "" : record.trigger.targetType
            ).toLowerCase(Locale.ROOT);
            if (!haystack.contains(search)) {
                return false;
            }
        }
        String from = safe(query.get("from"));
        if (!from.isBlank() && compareIso(record.createdAt, from) < 0) {
            return false;
        }
        String to = safe(query.get("to"));
        return to.isBlank() || compareIso(record.createdAt, to) <= 0;
    }

    private static String snapshotKindLabel(String kind) {
        return switch (safe(kind).toLowerCase(Locale.ROOT)) {
            case "manual" -> "手动保存 手动保存点";
            case "pre_rollback" -> "回滚前保护 回滚保护 保护点";
            default -> "自动快照 自动保存";
        };
    }

    private static String snapshotModuleLabel(String module) {
        return switch (safe(module).toLowerCase(Locale.ROOT)) {
            case "snapshot" -> "快照 回滚 保存点";
            case "logic chain" -> "逻辑链";
            case "template" -> "模板";
            case "timer" -> "计时器";
            case "signal join" -> "信号汇合";
            case "signal listener" -> "信号监听器";
            case "condition group" -> "条件组";
            case "state variable" -> "状态变量";
            case "region controller" -> "区域控制器";
            default -> safe(module);
        };
    }

    private static String snapshotOperationLabel(String operation) {
        return switch (safe(operation).toUpperCase(Locale.ROOT)) {
            case "ROLLBACK_SNAPSHOT" -> "回滚快照 回滚保存点";
            case "CREATE_SNAPSHOT" -> "创建保存点 手动保存";
            case "EDIT_LOGIC_CHAIN" -> "编辑逻辑链";
            case "APPLY_TEMPLATE" -> "应用模板";
            case "IMPORT_TEMPLATE" -> "导入模板";
            case "EDIT_TIMER" -> "编辑计时器";
            default -> safe(operation);
        };
    }

    private static Set<String> changedResourceTypes(SnapshotRecord record) {
        Set<String> types = new LinkedHashSet<>();
        SnapshotDiffSummary operationSummary = record == null || record.operationDiff == null ? null : record.operationDiff.summary;
        SnapshotDiffSummary summary = operationSummary != null && operationSummary.changed() > 0 ? operationSummary : record == null ? null : record.diffSummary;
        if (summary != null && summary.byType != null) {
            for (Map.Entry<String, Integer> entry : summary.byType.entrySet()) {
                if (entry != null && entry.getValue() != null && entry.getValue() > 0 && !safe(entry.getKey()).isBlank()) {
                    types.add(entry.getKey());
                }
            }
        }
        return types;
    }

    private static int compareIso(String left, String right) {
        try {
            return Instant.parse(safe(left)).compareTo(Instant.parse(safe(right)));
        } catch (DateTimeParseException exception) {
            return 0;
        }
    }

    private static String defaultTitle(SnapshotKind kind, SnapshotTrigger trigger) {
        SnapshotKind safeKind = kind == null ? SnapshotKind.AUTO : kind;
        if (safeKind == SnapshotKind.PRE_ROLLBACK) {
            return "回滚前保护点";
        }
        if (safeKind == SnapshotKind.MANUAL) {
            return "手动保存点";
        }
        return autoTitle(null, trigger == null ? "" : trigger.module);
    }

    private static String autoTitle(WebAdminOperationType operationType, String module) {
        String operation = operationType == null ? "" : operationType.displayName();
        String safeModule = safe(module);
        if (!operation.isBlank()) {
            return "自动：" + operation + "前";
        }
        return safeModule.isBlank() ? "自动：配置写入前" : "自动：" + safeModule + " 写入前";
    }

    private static String moduleTag(String module) {
        String safeModule = safe(module).trim();
        return safeModule.isBlank() ? "webadmin" : safeModule.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static Map<String, Object> snapshotSummary(SnapshotRecord record) {
        if (record == null) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("snapshotId", record.snapshotId);
        data.put("sequence", record.sequence);
        data.put("kind", record.kind);
        data.put("title", record.title);
        data.put("changedResources", record.diffSummary == null ? 0 : record.diffSummary.changed());
        data.put("operationChangedResources", record.operationDiff == null || record.operationDiff.summary == null ? 0 : record.operationDiff.summary.changed());
        data.put("packageFingerprint", record.packageFingerprint);
        return data;
    }

    private static String fingerprintPlan(RollbackPlan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append(plan.snapshotId).append('\n')
                .append(plan.currentFingerprint).append('\n')
                .append(plan.targetFingerprint).append('\n')
                .append(plan.manifestFingerprint).append('\n');
        for (RollbackOperation operation : plan.operations) {
            builder.append(operation.operation).append('|')
                    .append(operation.pathKey).append('|')
                    .append(operation.beforeFingerprint).append('|')
                    .append(operation.afterFingerprint).append('\n');
        }
        return WebAdminSnapshotStore.hash(builder.toString());
    }

    private static String joinMessages(String... messages) {
        List<String> result = new ArrayList<>();
        for (String message : messages) {
            if (!safe(message).isBlank()) {
                result.add(message);
            }
        }
        return String.join("；", result);
    }

    private static boolean suppressed() {
        return SUPPRESS_AUTO_CAPTURE.get() > 0;
    }

    private static <T> T runSuppressed(SupplierWithException<T> supplier) {
        SUPPRESS_AUTO_CAPTURE.set(SUPPRESS_AUTO_CAPTURE.get() + 1);
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        } finally {
            int next = Math.max(0, SUPPRESS_AUTO_CAPTURE.get() - 1);
            if (next == 0) {
                SUPPRESS_AUTO_CAPTURE.remove();
            } else {
                SUPPRESS_AUTO_CAPTURE.set(next);
            }
        }
    }

    private static String routeTarget(String targetType, String targetId) {
        String type = safe(targetType);
        String id = safe(targetId);
        if ("signal_join_config".equals(type)) {
            return "#/signal-joins/" + id;
        }
        if ("timer_config".equals(type)) {
            return "#/timers/" + id;
        }
        if ("condition_group".equals(type)) {
            return "#/condition-groups/" + id;
        }
        if ("logic_chain_metadata".equals(type)) {
            return "#/logic-chains/" + id;
        }
        if ("template_store".equals(type) || "template_apply".equals(type)) {
            return "#/templates";
        }
        if ("region_controller_config".equals(type)) {
            return "#/region-controllers/" + id;
        }
        if (type.contains("listener")) {
            return "#/listeners/" + id;
        }
        if (type.contains("channel")) {
            return "#/signals/" + id;
        }
        if (type.contains("device") || type.contains("action_relay")) {
            return "#/devices/" + id;
        }
        return "#/snapshots";
    }

    private static WebAdminWriteResult okWithData(WebAdminWriteTarget target, String message, Map<String, Object> data) {
        return new WebAdminWriteResult(true, WebAdminWriteResultCode.OK.id(), message, target.targetType(), target.targetId(), true, List.of(), "", "", false, Map.of(), data);
    }

    private static WebAdminWriteResult conflict(WebAdminWriteTarget target, String message, String expected, String actual) {
        return new WebAdminWriteResult(false, WebAdminWriteResultCode.CONFLICT_DETECTED.id(), message, target.targetType(), target.targetId(), false, List.of(), "", "", false, Map.of("expectedFingerprint", expected, "actualFingerprint", actual), Map.of());
    }

    private static WebAdminWriteTarget snapshotTarget(String snapshotId) {
        return new WebAdminWriteTarget("SNAPSHOT", safe(snapshotId), "配置快照");
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String username(WebAdminUser user) {
        return user == null ? "" : safe(user.username);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    public record WebAdminSnapshotAutoResult(boolean created, boolean skipped, String message, SnapshotRecord record) {
        public static WebAdminSnapshotAutoResult created(SnapshotRecord record) {
            return new WebAdminSnapshotAutoResult(true, false, "", record);
        }

        public static WebAdminSnapshotAutoResult skipped(String message) {
            return new WebAdminSnapshotAutoResult(false, true, message, null);
        }

        public static WebAdminSnapshotAutoResult failed(String message) {
            return new WebAdminSnapshotAutoResult(false, false, message == null ? "" : message, null);
        }
    }

    public static final class WebAdminSnapshotRequest {
        public String title = "";
        public String note = "";
        public List<String> tags = new ArrayList<>();
        public String expectedFingerprint = "";
        public String dryRunFingerprint = "";
        public String lockId = "";
        public boolean confirmed = false;
    }
}
