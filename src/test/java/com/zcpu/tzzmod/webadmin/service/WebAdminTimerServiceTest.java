package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminConfig;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WebAdminTimerServiceTest {
    private WebAdminTimerServiceTest() {
    }

    public static void run() throws Exception {
        testCreateDetailAndNoChangeUpdate();
        testActionBucketRoundTripDoesNotMixBuckets();
        testModeSpecificHiddenFieldsAreIgnoredAndSanitized();
        testRejectsUnsafeRawRequestValues();
        testRejectsInvalidTimerActionFields();
        testWriteSecurityFingerprintDeleteStatusAndRuntimeApis();
        testEditLockRequiredAndConflict();
        testAuditAndRealtimeEventsForWrites();
        testRuntimeResetRequiresFingerprintAndConfirmation();
        testViewerCannotWriteTimer();
    }

    private static void testCreateDetailAndNoChangeUpdate() throws Exception {
        Fixture fixture = fixture();
        WebAdminTimerRequest create = validRequest("timer.webadmin");
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "timer create succeeds");

        Map<?, ?> detail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.webadmin");
        requireEquals("DELAY", string(detail.get("mode")), "timer detail exposes internal mode");
        requireEquals("GLOBAL", string(detail.get("scopeMode")), "timer detail exposes internal scope");
        requireEquals("RESTART", string(detail.get("startPolicy")), "timer detail exposes internal startPolicy");
        requireEquals(Boolean.FALSE, detail.get("runtimeStatePersistent"), "timer runtime state is documented as memory-only");
        requireTrue(String.valueOf(detail.get("expectedFingerprint")).length() > 12, "timer detail exposes expectedFingerprint");

        WebAdminTimerRequest same = validRequest("timer.webadmin");
        same.expectedFingerprint = string(detail.get("expectedFingerprint"));
        WebAdminWriteResult noChange = fixture.service.update(null, fixture.editor, fixture.session, "127.0.0.1", "timer.webadmin", same, fixture.csrf, true);
        requireTrue(noChange.success(), "no-change update succeeds");
        requireEquals("no_change", noChange.code(), "no-change update uses no_change result");
        requireFalse(noChange.changed(), "no-change update does not report changed");
    }

    private static void testActionBucketRoundTripDoesNotMixBuckets() throws Exception {
        Fixture fixture = fixture();
        WebAdminTimerRequest create = validRequest("timer.bucket-roundtrip");
        create.mode = "COUNTDOWN";
        create.intervalTicks = 20;
        create.outputChannel = "";
        create.onStartActions = List.of(messageAction("start bucket"));
        create.onTickActions = List.of(messageAction("tick bucket"));
        create.onCompleteActions = List.of(messageAction("delay complete"));
        create.onCancelActions = List.of(messageAction("cancel bucket"));

        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "timer action bucket create succeeds");
        Map<?, ?> detail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.bucket-roundtrip");
        requireActionBucketValue(detail, "onStartActions", "start bucket", "onStartActions roundtrips start action only");
        requireActionBucketValue(detail, "onTickActions", "tick bucket", "onTickActions roundtrips tick action only");
        requireActionBucketValue(detail, "onCompleteActions", "delay complete", "onCompleteActions roundtrips complete action only");
        requireActionBucketValue(detail, "onCancelActions", "cancel bucket", "onCancelActions roundtrips cancel action only");
        requireActionBucketExcludes(detail, "onStartActions", "delay complete", "complete action is not displayed as start action");

        WebAdminTimerRequest update = validRequest("timer.bucket-roundtrip");
        update.mode = "COUNTDOWN";
        update.intervalTicks = 20;
        update.expectedFingerprint = string(detail.get("expectedFingerprint"));
        update.note = "updated without bucket mix";
        update.outputChannel = "";
        update.onStartActions = List.of(messageAction("start bucket"));
        update.onTickActions = List.of(messageAction("tick bucket"));
        update.onCompleteActions = List.of(messageAction("delay complete"));
        update.onCancelActions = List.of(messageAction("cancel bucket"));
        WebAdminWriteResult updated = fixture.service.update(null, fixture.editor, fixture.session, "127.0.0.1", "timer.bucket-roundtrip", update, fixture.csrf, true);
        requireTrue(updated.success(), "timer action bucket update succeeds");
        Map<?, ?> updatedDetail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.bucket-roundtrip");
        requireActionBucketValue(updatedDetail, "onCompleteActions", "delay complete", "update preserves onCompleteActions");
        requireActionBucketExcludes(updatedDetail, "onStartActions", "delay complete", "update does not store complete action in onStartActions");
    }

    private static void testModeSpecificHiddenFieldsAreIgnoredAndSanitized() throws Exception {
        Fixture fixture = fixture();

        WebAdminTimerRequest repeat = validRequest("timer.repeat-hidden-duration");
        repeat.mode = "REPEAT";
        repeat.durationTicks = -1;
        repeat.intervalTicks = 20;
        repeat.maxRuns = 2;
        repeat.outputChannel = "";
        repeat.onTickActions = List.of(messageAction("repeat tick"));
        WebAdminWriteResult repeatResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", repeat, fixture.csrf, true);
        requireTrue(repeatResult.success(), "REPEAT ignores hidden durationTicks and saves");
        Map<?, ?> repeatDetail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.repeat-hidden-duration");
        requireEquals("0", string(repeatDetail.get("durationTicks")), "REPEAT stores sanitized durationTicks");

        WebAdminTimerRequest delay = validRequest("timer.delay-hidden-tick");
        delay.mode = "DELAY";
        delay.intervalTicks = -1;
        delay.maxRuns = -5;
        delay.onTickActions = List.of(timerAction("", "timer_start"));
        WebAdminWriteResult delayResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", delay, fixture.csrf, true);
        requireTrue(delayResult.success(), "DELAY ignores hidden interval/maxRuns/onTick fields");
        Map<?, ?> delayDetail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.delay-hidden-tick");
        requireEquals("0", string(delayDetail.get("intervalTicks")), "DELAY stores sanitized intervalTicks");
        requireEquals("1", string(delayDetail.get("maxRuns")), "DELAY stores sanitized maxRuns");
        requireEquals(0, ((List<?>) delayDetail.get("onTickActions")).size(), "DELAY clears hidden onTick actions");
    }

    private static void testRejectsUnsafeRawRequestValues() throws Exception {
        Fixture fixture = fixture();
        WebAdminTimerRequest mode = validRequest("timer.bad-mode");
        mode.mode = "延迟执行";
        WebAdminWriteResult modeResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", mode, fixture.csrf, true);
        requireFalse(modeResult.success(), "mode display label is rejected");
        requireTrue(validationMessage(modeResult).contains("模式必须是 DELAY、COUNTDOWN 或 REPEAT"), "invalid mode reports Chinese validation");

        WebAdminTimerRequest scope = validRequest("timer.bad-scope");
        scope.scopeMode = "玩家";
        WebAdminWriteResult scopeResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", scope, fixture.csrf, true);
        requireFalse(scopeResult.success(), "scope display label is rejected");
        requireTrue(validationMessage(scopeResult).contains("作用域必须选择 GLOBAL 或 PLAYER"), "invalid scope reports Chinese validation");

        WebAdminTimerRequest repeat = validRequest("timer.bad-interval");
        repeat.mode = "REPEAT";
        repeat.intervalTicks = 0;
        WebAdminWriteResult repeatResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", repeat, fixture.csrf, true);
        requireFalse(repeatResult.success(), "REPEAT interval 0 rejected");
        requireTrue(validationMessage(repeatResult).contains("触发间隔"), "invalid repeat interval reports Chinese validation");
    }

    private static void testRejectsInvalidTimerActionFields() throws Exception {
        Fixture fixture = fixture();
        WebAdminTimerRequest invalidTarget = validRequest("timer.bad-action-target");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry targetAction = timerAction("timer.other", "timer_start");
        targetAction.timerTargetMode = "nearby_player";
        invalidTarget.onCompleteActions = List.of(targetAction);
        WebAdminWriteResult targetResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", invalidTarget, fixture.csrf, true);
        requireFalse(targetResult.success(), "invalid timer action target mode rejected");
        requireValidationCode(targetResult, "timer_target_mode_invalid");

        WebAdminTimerRequest invalidPolicy = validRequest("timer.bad-action-policy");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry policyAction = timerAction("timer.other", "timer_start");
        policyAction.timerStartPolicyOverride = "restart_later";
        invalidPolicy.onCompleteActions = List.of(policyAction);
        WebAdminWriteResult policyResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", invalidPolicy, fixture.csrf, true);
        requireFalse(policyResult.success(), "invalid timer action start policy rejected");
        requireValidationCode(policyResult, "timer_start_policy_invalid");

        WebAdminTimerRequest invalidMissingBehavior = validRequest("timer.bad-action-missing");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry cancelAction = timerAction("timer.other", "timer_cancel");
        cancelAction.timerMissingBehavior = "delete_config";
        invalidMissingBehavior.onCompleteActions = List.of(cancelAction);
        WebAdminWriteResult missingResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", invalidMissingBehavior, fixture.csrf, true);
        requireFalse(missingResult.success(), "invalid timer cancel missing behavior rejected");
        requireValidationCode(missingResult, "timer_missing_behavior_invalid");
    }

    private static void testWriteSecurityFingerprintDeleteStatusAndRuntimeApis() throws Exception {
        Fixture fixture = fixture();

        WebAdminWriteResult invalidCsrf = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.csrf"), "bad-token", true);
        requireFalse(invalidCsrf.success(), "invalid CSRF token rejected");
        requireEquals("csrf_invalid", invalidCsrf.code(), "invalid CSRF code");

        WebAdminWriteResult crossOrigin = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.origin"), fixture.csrf, false);
        requireFalse(crossOrigin.success(), "same-origin failure rejected");
        requireEquals("csrf_invalid", crossOrigin.code(), "same-origin failure uses CSRF invalid code");
        requireTrue(crossOrigin.message().contains("来源校验失败"), "same-origin failure has Chinese message");

        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.api"), fixture.csrf, true);
        requireTrue(created.success(), "timer create succeeds before API coverage");
        Map<?, ?> detail = fixture.service.detail(null, fixture.editor, fixture.session, "timer.api");
        String fingerprint = string(detail.get("expectedFingerprint"));

        WebAdminWriteResult startInvalidCsrf = fixture.service.start(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", runtimeRequest(fingerprint), "bad-token", true);
        requireFalse(startInvalidCsrf.success(), "manual start rejects invalid CSRF before runtime");
        requireEquals("csrf_invalid", startInvalidCsrf.code(), "manual start invalid CSRF code");

        WebAdminWriteResult cancelCrossOrigin = fixture.service.cancel(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", runtimeRequest(fingerprint), fixture.csrf, false);
        requireFalse(cancelCrossOrigin.success(), "manual cancel rejects same-origin failure before runtime");
        requireEquals("csrf_invalid", cancelCrossOrigin.code(), "manual cancel same-origin failure code");

        Map<?, ?> status = fixture.service.status(null, fixture.editor, "timer.api");
        requireEquals(0, status.get("activeInstanceCount"), "status API exposes active instance count");
        requireEquals(Boolean.FALSE, status.get("runtimeStatePersistent"), "status API documents memory-only runtime");

        WebAdminTimerRequest staleUpdate = validRequest("timer.api");
        staleUpdate.expectedFingerprint = "stale-fingerprint";
        staleUpdate.note = "changed";
        WebAdminWriteResult staleUpdateResult = fixture.service.update(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", staleUpdate, fixture.csrf, true);
        requireFalse(staleUpdateResult.success(), "stale update fingerprint rejected");
        requireEquals("conflict_detected", staleUpdateResult.code(), "stale update reports conflict");

        WebAdminTimerRequest runtime = runtimeRequest(fingerprint);
        WebAdminWriteResult start = fixture.service.start(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", runtime, fixture.csrf, true);
        requireFalse(start.success(), "manual start without Minecraft server fails safely in service test");
        requireEquals("validation_failed", start.code(), "manual start runtime failure maps to validation failure");
        requireTrue(start.message().contains("服务器上下文为空"), "manual start failure is Chinese");

        WebAdminWriteResult cancel = fixture.service.cancel(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", runtimeRequest(fingerprint), fixture.csrf, true);
        requireFalse(cancel.success(), "manual cancel without Minecraft server fails safely in service test");
        requireTrue(cancel.message().contains("服务器上下文为空"), "manual cancel failure is Chinese");

        WebAdminTimerRequest deleteMissingConfirmation = runtimeRequest(fingerprint);
        WebAdminWriteResult deleteConfirmation = fixture.service.delete(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", deleteMissingConfirmation, fixture.csrf, true);
        requireFalse(deleteConfirmation.success(), "delete requires confirmation");
        requireValidationCode(deleteConfirmation, "confirmation_required");

        WebAdminTimerRequest staleDelete = runtimeRequest("stale-fingerprint");
        staleDelete.confirmed = true;
        WebAdminWriteResult staleDeleteResult = fixture.service.delete(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", staleDelete, fixture.csrf, true);
        requireFalse(staleDeleteResult.success(), "stale delete fingerprint rejected");
        requireEquals("conflict_detected", staleDeleteResult.code(), "stale delete reports conflict");

        WebAdminTimerRequest delete = runtimeRequest(fingerprint);
        delete.confirmed = true;
        WebAdminWriteResult deleted = fixture.service.delete(null, fixture.editor, fixture.session, "127.0.0.1", "timer.api", delete, fixture.csrf, true);
        requireTrue(deleted.success(), "delete succeeds with confirmation and matching fingerprint");
        requireTrue(Boolean.TRUE.equals(fixture.service.detail(null, fixture.editor, fixture.session, "timer.api").get("notFound")), "detail API reports deleted timer notFound");
        requireTrue(Boolean.TRUE.equals(fixture.service.status(null, fixture.editor, "timer.api").get("notFound")), "status API reports deleted timer notFound");
    }

    private static void testEditLockRequiredAndConflict() throws Exception {
        Fixture fixture = fixtureWithLock();

        WebAdminWriteResult missingLock = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.lock-required"), fixture.csrf, true);
        requireFalse(missingLock.success(), "Timer write requires edit lock when lock service is enabled");
        requireEquals("edit_lock_required", missingLock.code(), "missing edit lock code");

        String lockId = acquireLock(fixture, "timer.locked");
        WebAdminTimerRequest lockedCreate = validRequest("timer.locked");
        lockedCreate.lockId = lockId;
        WebAdminWriteResult lockedCreated = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", lockedCreate, fixture.csrf, true);
        requireTrue(lockedCreated.success(), "Timer create succeeds with matching edit lock");
        requireEquals(0, fixture.editLockService.activeLockCount(), "Timer write releases edit lock after success");
        String fingerprint = string(fixture.service.detail(null, fixture.editor, fixture.session, "timer.locked").get("expectedFingerprint"));
        WebAdminTimerRequest runtime = runtimeRequest(fingerprint);
        runtime.confirmed = true;
        requireEquals("edit_lock_required",
                fixture.service.start(null, fixture.editor, fixture.session, "127.0.0.1", "timer.locked", runtimeRequest(fingerprint), fixture.csrf, true).code(),
                "manual start requires edit lock when lock service is enabled");
        requireEquals("edit_lock_required",
                fixture.service.cancel(null, fixture.editor, fixture.session, "127.0.0.1", "timer.locked", runtimeRequest(fingerprint), fixture.csrf, true).code(),
                "manual cancel requires edit lock when lock service is enabled");
        requireEquals("edit_lock_required",
                fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "timer.locked", runtime, fixture.csrf, true).code(),
                "manual reset requires edit lock when lock service is enabled");

        acquireLock(fixture, "timer.lock-conflict");
        WebAdminTimerRequest conflict = validRequest("timer.lock-conflict");
        conflict.lockId = "wrong-lock-id";
        WebAdminWriteResult conflictResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", conflict, fixture.csrf, true);
        requireFalse(conflictResult.success(), "Timer write rejects conflicting edit lock");
        requireEquals("edit_lock_conflict", conflictResult.code(), "edit lock conflict code");
    }

    private static void testAuditAndRealtimeEventsForWrites() throws Exception {
        Fixture fixture = fixtureWithAudit();
        long baselineSeq = WebAdminRealtimeEventBus.currentSeq();

        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.audit"), fixture.csrf, true);
        requireTrue(created.success(), "create succeeds before audit/realtime assertions");
        requireTrue(Files.readString(fixture.auditLogPath, StandardCharsets.UTF_8).contains("timer.audit"), "Timer create writes audit log");
        requireRecentEventSince(baselineSeq, "timer_changed", "Timer create publishes config realtime");
        requireRecentEventSince(baselineSeq, "write_audit_appended", "Timer create publishes write audit realtime");

        String fingerprint = string(((Map<?, ?>) fixture.service.detail(null, fixture.editor, fixture.session, "timer.audit")).get("expectedFingerprint"));
        WebAdminTimerRequest reset = runtimeRequest(fingerprint);
        reset.confirmed = true;
        WebAdminWriteResult resetResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "timer.audit", reset, fixture.csrf, true);
        requireTrue(resetResult.success(), "reset succeeds before runtime realtime assertion");
        requireRecentEventSince(baselineSeq, "timer_runtime_changed", "Timer reset publishes runtime realtime");

        WebAdminConfig disabledAudit = new WebAdminConfig();
        disabledAudit.auditEnabled = false;
        WebAdminAuditLogger.configure(null, disabledAudit);
    }

    private static void testRuntimeResetRequiresFingerprintAndConfirmation() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.reset"), fixture.csrf, true);
        requireTrue(created.success(), "timer create succeeds before reset checks");
        String fingerprint = string(((Map<?, ?>) fixture.service.detail(null, fixture.editor, fixture.session, "timer.reset")).get("expectedFingerprint"));

        WebAdminTimerRequest missingConfirmation = new WebAdminTimerRequest();
        missingConfirmation.expectedFingerprint = fingerprint;
        WebAdminWriteResult missingConfirmationResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "timer.reset", missingConfirmation, fixture.csrf, true);
        requireFalse(missingConfirmationResult.success(), "reset requires explicit confirmation");

        WebAdminTimerRequest missingFingerprint = new WebAdminTimerRequest();
        missingFingerprint.confirmed = true;
        WebAdminWriteResult missingFingerprintResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "timer.reset", missingFingerprint, fixture.csrf, true);
        requireFalse(missingFingerprintResult.success(), "reset requires expectedFingerprint");

        WebAdminTimerRequest reset = new WebAdminTimerRequest();
        reset.confirmed = true;
        reset.expectedFingerprint = fingerprint;
        WebAdminWriteResult resetResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "timer.reset", reset, fixture.csrf, true);
        requireTrue(resetResult.success(), "reset succeeds with confirmation and matching fingerprint");
    }

    private static void testViewerCannotWriteTimer() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", validRequest("timer.viewer-runtime"), fixture.csrf, true);
        requireTrue(created.success(), "editor creates timer before viewer runtime denial checks");
        String fingerprint = string(fixture.service.detail(null, fixture.editor, fixture.session, "timer.viewer-runtime").get("expectedFingerprint"));
        WebAdminUser viewer = user(WebAdminRole.VIEWER);
        WebAdminSession viewerSession = session(viewer);
        String viewerCsrf = fixture.security.csrfTokenFor(viewerSession);
        WebAdminWriteResult result = fixture.service.create(null, viewer, viewerSession, "127.0.0.1", validRequest("timer.viewer"), viewerCsrf, true);
        requireFalse(result.success(), "VIEWER cannot create timers");
        requireFalse(fixture.service.start(null, viewer, viewerSession, "127.0.0.1", "timer.viewer-runtime", runtimeRequest(fingerprint), viewerCsrf, true).success(), "VIEWER cannot manually start timers");
        requireFalse(fixture.service.cancel(null, viewer, viewerSession, "127.0.0.1", "timer.viewer-runtime", runtimeRequest(fingerprint), viewerCsrf, true).success(), "VIEWER cannot manually cancel timers");
        WebAdminTimerRequest reset = runtimeRequest(fingerprint);
        reset.confirmed = true;
        requireFalse(fixture.service.reset(null, viewer, viewerSession, "127.0.0.1", "timer.viewer-runtime", reset, viewerCsrf, true).success(), "VIEWER cannot manually reset timers");
    }

    private static Fixture fixture() throws Exception {
        return fixture(false, false);
    }

    private static Fixture fixtureWithLock() throws Exception {
        return fixture(true, false);
    }

    private static Fixture fixtureWithAudit() throws Exception {
        return fixture(false, true);
    }

    private static Fixture fixture(boolean withLock, boolean withAudit) throws Exception {
        Path directory = Files.createTempDirectory("tzz-timer-service-test");
        Path path = directory.resolve("timers.json");
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminPermissionService permission = new WebAdminPermissionService();
        WebAdminEditLockService editLockService = withLock ? new WebAdminEditLockService(permission, security, 60_000L) : null;
        WebAdminTimerService service = new WebAdminTimerService(permission, security, editLockService, path);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        Path auditLogPath = directory.resolve("web_admin_audit.log");
        if (withAudit) {
            WebAdminConfig config = new WebAdminConfig();
            config.auditEnabled = true;
            WebAdminAuditLogger.configure(new WebAdminStoragePaths(
                    directory,
                    directory.resolve("web_admin_config.json"),
                    directory.resolve("web_admin_users.json"),
                    directory.resolve("web_admin_device_metadata.json"),
                    directory.resolve("web_admin_channel_metadata.json"),
                    auditLogPath,
                    directory.resolve("legacy"),
                    directory.resolve("legacy").resolve("web_admin_config.json"),
                    directory.resolve("legacy").resolve("web_admin_users.json"),
                    directory.resolve("legacy").resolve("web_admin_audit.log")
            ), config);
        }
        return new Fixture(service, security, editLockService, editor, session, security.csrfTokenFor(session), auditLogPath);
    }

    private static WebAdminTimerRequest validRequest(String id) {
        WebAdminTimerRequest request = new WebAdminTimerRequest();
        request.id = id;
        request.displayName = id;
        request.enabled = true;
        request.mode = "DELAY";
        request.scopeMode = "GLOBAL";
        request.durationTicks = 40;
        request.intervalTicks = 0;
        request.maxRuns = 1;
        request.startPolicy = "RESTART";
        request.outputChannel = "timer.done";
        request.onCompleteActions = List.of(messageAction("done"));
        return request;
    }

    private static WebAdminTimerRequest runtimeRequest(String expectedFingerprint) {
        WebAdminTimerRequest request = new WebAdminTimerRequest();
        request.expectedFingerprint = expectedFingerprint;
        request.targetMode = "global";
        request.targetId = "";
        request.scopeKey = "";
        return request;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry messageAction(String value) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = "message";
        entry.value = value;
        entry.enabled = Boolean.TRUE;
        entry.requiresOp = Boolean.FALSE;
        entry.cooldownTicks = 0;
        entry.notifyOps = Boolean.FALSE;
        return entry;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry timerAction(String timerId, String type) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = type;
        entry.timerId = timerId;
        entry.timerTargetMode = "global";
        entry.timerStartPolicyOverride = "";
        entry.timerDurationOverrideTicks = 0;
        entry.timerMissingBehavior = "noop_success";
        return entry;
    }

    private static WebAdminUser user(WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = role.id().toLowerCase(Locale.ROOT);
        user.displayName = role.displayName();
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
    }

    private static String acquireLock(Fixture fixture, String timerId) {
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = WebAdminEditLockService.TARGET_TIMER_CONFIG;
        request.targetId = TimerStore.normalizeId(timerId);
        WebAdminWriteResult result = fixture.editLockService.acquire(fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "acquire edit lock for " + timerId);
        Object lock = result.data().get("lock");
        requireTrue(lock instanceof WebAdminEditLockStatusDto, "edit lock result exposes lock status");
        return ((WebAdminEditLockStatusDto) lock).lockId();
    }

    private static void requireRecentEventSince(long baselineSeq, String type, String message) {
        for (WebAdminRealtimeEvent event : WebAdminRealtimeEventBus.recentEvents()) {
            if (event.seq() > baselineSeq && type.equals(event.type())) {
                return;
            }
        }
        throw new AssertionError(message + " missing event type=" + type);
    }

    private static String validationMessage(WebAdminWriteResult result) {
        return result.validationErrors().isEmpty() ? result.message() : result.validationErrors().getFirst().message();
    }

    private static void requireValidationCode(WebAdminWriteResult result, String code) {
        for (var error : result.validationErrors()) {
            if (code.equals(error.code())) {
                requireTrue(containsChinese(error.message()), "validation code " + code + " has Chinese message");
                return;
            }
        }
        throw new AssertionError("missing validation code " + code + " errors=" + result.validationErrors());
    }

    private static void requireActionBucketValue(Map<?, ?> detail, String bucket, String expectedValue, String message) {
        Object raw = detail.get(bucket);
        requireTrue(raw instanceof List<?>, message + " exposes list");
        List<?> actions = (List<?>) raw;
        requireEquals(1, actions.size(), message + " action count");
        requireTrue(actions.getFirst() instanceof Map<?, ?>, message + " entry is map");
        Map<?, ?> entry = (Map<?, ?>) actions.getFirst();
        requireEquals(expectedValue, string(entry.get("value")), message);
    }

    private static void requireActionBucketExcludes(Map<?, ?> detail, String bucket, String unexpectedValue, String message) {
        Object raw = detail.get(bucket);
        requireTrue(raw instanceof List<?>, message + " exposes list");
        for (Object action : (List<?>) raw) {
            if (action instanceof Map<?, ?> entry && unexpectedValue.equals(string(entry.get("value")))) {
                throw new AssertionError(message + " unexpectedValue=" + unexpectedValue);
            }
        }
    }

    private static boolean containsChinese(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private record Fixture(
            WebAdminTimerService service,
            WebAdminWriteSecurityService security,
            WebAdminEditLockService editLockService,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf,
            Path auditLogPath
    ) {
    }
}
