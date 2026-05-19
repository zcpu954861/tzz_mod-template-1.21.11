package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WebAdminLogicChainEditorServiceTest {
    private WebAdminLogicChainEditorServiceTest() {
    }

    public static void run() throws Exception {
        testEnterRequiresPermissionCsrfAndEditorLock();
        testLockConflictBlocksEditMode();
        testSaveDraftWithStableLockTargetSucceedsAndReleasesLock();
        testMissingMismatchAndValidationFailurePreserveDraftLockState();
        testValidationFailureRetryUsesSameLockAndStructuredErrors();
        testTypedLockFailurePreservesEditorLockAndDraft();
        testValidateDraftRejectsIncompleteAndOutOfScopeNodes();
        testStaleBaseGraphFingerprintBlocksSave();
        testSaveRejectsSignalJoinMissingRequiredConfigAndEdges();
        testSaveRejectsTimerMissingRequiredConfigAndEdges();
        testSaveRejectsIdsThatNormalizeToBlankBeforeTypedLock();
        testSaveRejectsInvalidDuplicateAndIncompleteEdges();
        testSaveRejectsJoinInputOutputChannelConflict();
        testSaveAllowsVisualUpstreamOutputWhenNoRealCycle();
        testJoinCycleGuardRejectsReachableInputAndTruncates();
        testSaveRejectsDirectNonChannelVisualEndpoints();
        testChannelMetadataDraftValidation();
        testSaveRejectsModeSpecificJoinThresholdErrors();
        testSaveRejectsInvalidPlacementAndUnplacedDraft();
        testActionAppendRejectsInvalidShapeAndMixedDraft();
        testActionAppendValidationCoversSupportedOwnersBucketsAndConditionGroup();
        testSaveRejectsWrongLockAndSameOriginFailure();
        testSaveSignalJoinDraftWritesUnderlyingConfig();
        testSaveTimerDraftWritesUnderlyingConfig();
        testTimerDraftCanCreateAndSelectDownstreamChannelEndpoint();
        testSaveDraftNormalizesTypedLockTargetsBeforeSaving();
        testSaveTimerDraftAllowsOnCompleteOnlyOutput();
        testTimerActionAppendThroughLogicChainEditor();
    }

    private static void testEnterRequiresPermissionCsrfAndEditorLock() throws Exception {
        Fixture fixture = fixture();
        WebAdminLogicChainEditorRequest request = rootRequest("editor.security");
        WebAdminWriteResult viewer = fixture.service.enter(null, user(WebAdminRole.VIEWER), session(user(WebAdminRole.VIEWER)), "127.0.0.1", request, fixture.csrf, true);
        requireFalse(viewer.success(), "VIEWER cannot enter Logic Chain editor");

        WebAdminWriteResult invalidCsrf = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", request, "bad-token", true);
        requireFalse(invalidCsrf.success(), "edit mode rejects invalid CSRF");
        requireEquals("csrf_invalid", invalidCsrf.code(), "invalid CSRF code");

        WebAdminWriteResult entered = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(entered.success(), "editor enters edit mode with lock");
        requireTrue(lockId(entered).length() > 8, "enter edit mode returns logic_chain_editor lock");
        requireTrue(String.valueOf(entered.data().get("baseGraphFingerprint")).length() > 12, "enter edit mode returns base graph fingerprint");
        requireEquals(1, fixture.editLockService.activeLockCount(), "logic_chain_editor lock is active after enter");
    }

    private static void testLockConflictBlocksEditMode() throws Exception {
        Fixture fixture = fixture();
        WebAdminLogicChainEditorRequest request = rootRequest("editor.conflict");
        WebAdminWriteResult first = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(first.success(), "first editor gets lock");

        WebAdminUser secondUser = user(WebAdminRole.EDITOR, "second-editor");
        WebAdminSession secondSession = session(secondUser);
        String secondCsrf = fixture.security.csrfTokenFor(secondSession);
        WebAdminWriteResult second = fixture.service.enter(null, secondUser, secondSession, "127.0.0.1", request, secondCsrf, true);
        requireFalse(second.success(), "second editor cannot enter while lock is held");
        requireEquals("edit_lock_conflict", second.code(), "lock conflict code is exposed");
        requireTrue(second.message().contains("正在编辑"), "lock conflict message is Chinese and visible");
    }

    private static void testSaveDraftWithStableLockTargetSucceedsAndReleasesLock() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.lock.stable");
        WebAdminLogicChainEditorRequest request = timerDraftRequest("editor.lock.stable", lockId(entered), fingerprint(entered), "editor.lock.stable.timer");
        request.lockTargetType = string(entered.data().get("targetType"));
        request.lockTargetId = string(entered.data().get("targetId"));

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);

        requireTrue(result.success(), "save draft with same lock target succeeds");
        requireEquals(0, fixture.editLockService.activeLockCount(), "successful save releases editor lock");
    }

    private static void testMissingMismatchAndValidationFailurePreserveDraftLockState() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.lock.lifecycle");
        String targetType = string(entered.data().get("targetType"));
        String targetId = string(entered.data().get("targetId"));

        WebAdminLogicChainEditorRequest missingLock = timerDraftRequest("editor.lock.lifecycle", "", fingerprint(entered), "editor.lock.missing.timer");
        missingLock.lockTargetType = targetType;
        missingLock.lockTargetId = targetId;
        WebAdminWriteResult missing = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", missingLock, fixture.csrf, true);
        requireFalse(missing.success(), "save draft missing lock fails");
        requireEquals("edit_lock_required", missing.code(), "missing editor lock uses edit_lock_required");
        requireTrue(containsChinese(missing.message()), "missing lock message is Chinese");
        requireEquals(Boolean.TRUE, missing.data().get("editorLockLost"), "missing submitted lock is marked as editor lock lost");
        requireEquals(Boolean.TRUE, missing.data().get("draftPreserved"), "missing submitted lock preserves draft");
        requireStructuredError(missing, "edit_lock_required", "", "");
        requireEquals(1, fixture.editLockService.activeLockCount(), "missing lock save does not release held editor lock");

        Fixture expiredFixture = fixture(1_000L);
        WebAdminWriteResult expiredEntered = enter(expiredFixture, "editor.lock.expired");
        Thread.sleep(1_100L);
        WebAdminLogicChainEditorRequest expiredLock = timerDraftRequest("editor.lock.expired", lockId(expiredEntered), fingerprint(expiredEntered), "editor.lock.expired.timer");
        expiredLock.lockTargetType = string(expiredEntered.data().get("targetType"));
        expiredLock.lockTargetId = string(expiredEntered.data().get("targetId"));
        WebAdminWriteResult expired = expiredFixture.service.saveDraft(null, expiredFixture.editor, expiredFixture.session, "127.0.0.1", expiredLock, expiredFixture.csrf, true);
        requireFalse(expired.success(), "expired editor lock fails save");
        requireEquals("edit_lock_expired", expired.code(), "expired lock keeps edit_lock_expired code");
        requireEquals(Boolean.TRUE, expired.data().get("editorLockLost"), "expired editor lock is explicitly marked lost");
        requireEquals(Boolean.TRUE, expired.data().get("draftPreserved"), "expired editor lock result preserves draft");
        requireStructuredError(expired, "edit_lock_expired", "", "");

        WebAdminLogicChainEditorRequest mismatch = timerDraftRequest("editor.lock.lifecycle", lockId(entered), fingerprint(entered), "editor.lock.mismatch.timer");
        mismatch.lockTargetType = targetType;
        mismatch.lockTargetId = "channel:editor.lock.other";
        WebAdminWriteResult mismatchResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", mismatch, fixture.csrf, true);
        requireFalse(mismatchResult.success(), "lock target mismatch fails");
        requireValidationCode(mismatchResult, "logic_chain_editor_lock_target_mismatch");
        requireEquals(1, fixture.editLockService.activeLockCount(), "lock target mismatch keeps original editor lock active");

        WebAdminLogicChainEditorRequest invalidDraft = signalJoinDraftRequest("editor.lock.lifecycle", lockId(entered), fingerprint(entered), "editor.lock.validation.join");
        invalidDraft.lockTargetType = targetType;
        invalidDraft.lockTargetId = targetId;
        invalidDraft.edges = List.of();
        WebAdminWriteResult invalid = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", invalidDraft, fixture.csrf, true);
        requireFalse(invalid.success(), "validation failure preserves draft for retry");
        requireValidationCode(invalid, "logic_chain_join_input_edge_required");
        requireFalse(Boolean.TRUE.equals(invalid.data().get("editorLockLost")), "draft validation failure is not marked as lost editor lock");
        requireEquals(1, fixture.editLockService.activeLockCount(), "validation failure does not release editor lock");

        WebAdminWriteResult cancel = fixture.service.cancel(fixture.editor, fixture.session, "127.0.0.1", invalidDraft, fixture.csrf, true);
        requireTrue(cancel.success(), "cancel edit releases stable editor lock");
        requireEquals(0, fixture.editLockService.activeLockCount(), "cancel releases editor lock");
    }

    private static void testValidationFailureRetryUsesSameLockAndStructuredErrors() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.lock.retry");
        String targetType = string(entered.data().get("targetType"));
        String targetId = string(entered.data().get("targetId"));

        WebAdminLogicChainEditorRequest invalidDraft = signalJoinDraftRequest("editor.lock.retry", lockId(entered), fingerprint(entered), "editor.lock.retry.join");
        invalidDraft.lockTargetType = targetType;
        invalidDraft.lockTargetId = targetId;
        invalidDraft.edges = List.of(edge("draft-edge-retry-out", invalidDraft.nodes.getFirst().id, "channel:editor.retry.out", "join_output"));
        WebAdminWriteResult invalid = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", invalidDraft, fixture.csrf, true);
        requireFalse(invalid.success(), "validation failure blocks save");
        requireFalse(Boolean.TRUE.equals(invalid.data().get("editorLockLost")), "validation failure is not marked as lost editor lock");
        WebAdminValidationError missingInput = findValidationError(invalid, "logic_chain_join_input_edge_required");
        requireEquals(invalidDraft.nodes.getFirst().id, missingInput.nodeId(), "structured validation includes draft node id");
        requireEquals("error", missingInput.severity(), "structured validation includes severity");
        requireTrue(containsChinese(missingInput.fixHint()), "structured validation includes Chinese fix hint");
        requireEquals(1, fixture.editLockService.activeLockCount(), "validation failure keeps editor lock active");

        WebAdminLogicChainEditorRequest validRetry = timerDraftRequest("editor.lock.retry", lockId(entered), fingerprint(entered), "editor.lock.retry.timer");
        validRetry.lockTargetType = targetType;
        validRetry.lockTargetId = targetId;
        WebAdminWriteResult retry = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", validRetry, fixture.csrf, true);
        requireTrue(retry.success(), "second save after validation failure can reuse the same lock");
        requireEquals(0, fixture.editLockService.activeLockCount(), "successful retry releases editor lock");
    }

    private static void testTypedLockFailurePreservesEditorLockAndDraft() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.lock.typed.failure");
        WebAdminLogicChainEditorRequest request = signalJoinDraftRequest("editor.lock.typed.failure", lockId(entered), fingerprint(entered), "editor.lock.typed.join");
        request.lockTargetType = string(entered.data().get("targetType"));
        request.lockTargetId = string(entered.data().get("targetId"));
        String draftId = request.nodes.getFirst().id;

        WebAdminUser otherEditor = user(WebAdminRole.EDITOR, "other-editor");
        WebAdminSession otherSession = session(otherEditor);
        WebAdminEditLockRequest typedLock = new WebAdminEditLockRequest();
        typedLock.targetType = WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG;
        typedLock.targetId = "editor.lock.typed.join";
        WebAdminWriteResult occupied = fixture.editLockService.acquire(
                otherEditor,
                otherSession,
                "127.0.0.2",
                typedLock,
                fixture.security.csrfTokenFor(otherSession),
                true
        );
        requireTrue(occupied.success(), "other session holds typed Signal Join lock");

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);

        requireFalse(result.success(), "typed lock conflict blocks save");
        requireEquals("edit_lock_conflict", result.code(), "typed lock conflict keeps typed edit lock code");
        requireEquals(Boolean.FALSE, result.data().get("editorLockLost"), "typed failure explicitly keeps editor lock");
        requireEquals(Boolean.TRUE, result.data().get("draftPreserved"), "typed failure explicitly preserves draft");
        requireEquals("logic_chain_editor", result.data().get("editorLockTargetType"), "typed failure reports editor lock target type");
        requireEquals("channel:editor.lock.typed.failure", result.data().get("editorLockTargetId"), "typed failure reports editor lock target id");
        requireStructuredError(result, "edit_lock_conflict", draftId, "");
        requireEquals(2, fixture.editLockService.activeLockCount(), "typed failure keeps editor lock and other typed lock active");
    }

    private static void testValidateDraftRejectsIncompleteAndOutOfScopeNodes() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.validation");
        WebAdminLogicChainEditorRequest empty = rootRequest("editor.validation");
        empty.baseGraphFingerprint = fingerprint(entered);
        empty.lockId = lockId(entered);
        WebAdminWriteResult emptyResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, empty, fixture.csrf, true);
        requireFalse(emptyResult.success(), "empty draft is rejected");
        requireValidationCode(emptyResult, "logic_chain_draft_node_required");

        WebAdminLogicChainEditorRequest bad = rootRequest("editor.validation");
        bad.baseGraphFingerprint = fingerprint(entered);
        bad.lockId = lockId(entered);
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:world_device:bad";
        node.type = "world_device";
        node.column = "C2";
        node.slot = 0;
        node.placed = true;
        bad.nodes = List.of(node);
        WebAdminWriteResult badResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, bad, fixture.csrf, true);
        requireFalse(badResult.success(), "world entity draft creation is deferred");
        requireValidationCode(badResult, "logic_chain_node_type_deferred");
        requireStructuredError(badResult, "logic_chain_node_type_deferred", node.id, "");

        node.id = "draft:signal_listener:bad";
        node.type = "signal_listener";
        WebAdminWriteResult listenerResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, bad, fixture.csrf, true);
        requireFalse(listenerResult.success(), "virtual SignalListener canvas creation is deferred until safe edit lock path exists");
        requireValidationCode(listenerResult, "logic_chain_node_type_deferred");
        requireStructuredError(listenerResult, "logic_chain_node_type_deferred", node.id, "");

        node.id = "draft:signal_join:bad";
        node.type = "signal_join";
        node.column = "C0";
        WebAdminWriteResult slotResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, bad, fixture.csrf, true);
        requireFalse(slotResult.success(), "Signal Join invalid column is rejected");
        requireValidationCode(slotResult, "logic_chain_join_column_invalid");
        requireStructuredError(slotResult, "logic_chain_join_column_invalid", node.id, "");
    }

    private static void testStaleBaseGraphFingerprintBlocksSave() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.stale");
        WebAdminLogicChainEditorRequest request = signalJoinDraftRequest("editor.stale", lockId(entered), "stale-fingerprint", "editor.stale.join");
        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(result.success(), "stale graph fingerprint blocks save");
        requireEquals("conflict_detected", result.code(), "stale graph uses conflict result");
        requireTrue(String.valueOf(result.conflict().get("actualFingerprint")).length() > 12, "conflict includes actual graph fingerprint");
    }

    private static void testSaveRejectsSignalJoinMissingRequiredConfigAndEdges() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.missing");
        WebAdminLogicChainEditorRequest request = rootRequest("editor.join.missing");
        request.lockId = lockId(entered);
        request.baseGraphFingerprint = fingerprint(entered);
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:signal_join:missing";
        node.type = "signal_join";
        node.column = "C2";
        node.slot = 0;
        node.placed = true;
        request.nodes = List.of(node);
        request.edges = List.of();

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(result.success(), "Signal Join missing config and edges is rejected");
        requireValidationCode(result, "signal_join_id_required");
        requireStructuredError(result, "signal_join_id_required", node.id, "");
        requireValidationCode(result, "logic_chain_join_input_edge_required");
        requireValidationCode(result, "logic_chain_join_output_edge_required");
    }

    private static void testSaveRejectsTimerMissingRequiredConfigAndEdges() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.timer.missing");
        WebAdminLogicChainEditorRequest request = rootRequest("editor.timer.missing");
        request.lockId = lockId(entered);
        request.baseGraphFingerprint = fingerprint(entered);
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:timer:missing";
        node.type = "timer";
        node.column = "C0";
        node.slot = 0;
        node.placed = true;
        request.nodes = List.of(node);
        request.edges = List.of();

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(result.success(), "Timer missing config and output edge is rejected");
        requireValidationCode(result, "timer_id_required");
        requireStructuredError(result, "timer_id_required", node.id, "");
        requireValidationCode(result, "logic_chain_timer_output_edge_required");
    }

    private static void testSaveRejectsIdsThatNormalizeToBlankBeforeTypedLock() throws Exception {
        Fixture joinFixture = fixture();
        WebAdminWriteResult joinEntered = enter(joinFixture, "editor.join.invalid.normalized");
        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.join.invalid.normalized", lockId(joinEntered), fingerprint(joinEntered), "!!!");
        WebAdminWriteResult joinResult = joinFixture.service.saveDraft(null, joinFixture.editor, joinFixture.session, "127.0.0.1", join, joinFixture.csrf, true);
        requireFalse(joinResult.success(), "Signal Join id that normalizes blank fails validation before typed lock");
        requireValidationCode(joinResult, "signal_join_id_required");
        requireEquals(1, joinFixture.editLockService.activeLockCount(), "invalid normalized Signal Join id does not acquire a typed lock");

        Fixture timerFixture = fixture();
        WebAdminWriteResult timerEntered = enter(timerFixture, "editor.timer.invalid.normalized");
        WebAdminLogicChainEditorRequest timer = timerDraftRequest("editor.timer.invalid.normalized", lockId(timerEntered), fingerprint(timerEntered), "!!!");
        WebAdminWriteResult timerResult = timerFixture.service.saveDraft(null, timerFixture.editor, timerFixture.session, "127.0.0.1", timer, timerFixture.csrf, true);
        requireFalse(timerResult.success(), "Timer id that normalizes blank fails validation before typed lock");
        requireValidationCode(timerResult, "timer_id_required");
        requireEquals(1, timerFixture.editLockService.activeLockCount(), "invalid normalized Timer id does not acquire a typed lock");
    }

    private static void testSaveRejectsInvalidDuplicateAndIncompleteEdges() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.edge.invalid");
        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.edge.invalid", lockId(entered), fingerprint(entered), "editor.edge.join");
        String draftId = join.nodes.getFirst().id;
        join.edges = List.of(
                edge("draft-edge-join-in-a", "channel:editor.join.in.a", draftId, "join_input"),
                edge("draft-edge-join-in-a-copy", "channel:editor.join.in.a", draftId, "join_input"),
                edge("draft-edge-join-out-wrong", draftId, "channel:editor.join.other", "join_output"),
                edge("draft-edge-detached", "channel:editor.detached.a", "channel:editor.detached.b", "join_input")
        );
        WebAdminWriteResult joinResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);
        requireFalse(joinResult.success(), "duplicate, detached and incomplete Signal Join edges are rejected");
        requireValidationCode(joinResult, "logic_chain_duplicate_edge");
        requireValidationCode(joinResult, "logic_chain_edge_not_incident_to_draft");
        requireValidationCode(joinResult, "logic_chain_join_input_edge_required");

        join.edges = List.of(
                edge("draft-edge-join-in-a", "channel:editor.join.in.a", draftId, "join_input"),
                edge("draft-edge-join-in-b", "channel:editor.join.in.b", draftId, "join_input"),
                edge("draft-edge-join-out-a", draftId, "channel:editor.join.out.a", "join_output"),
                edge("draft-edge-join-out-b", draftId, "channel:editor.join.out.b", "join_output")
        );
        WebAdminWriteResult multiOutput = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);
        requireFalse(multiOutput.success(), "Signal Join rejects more than one downstream output edge");
        requireValidationCode(multiOutput, "logic_chain_join_output_edge_required");

        Fixture timerFixture = fixture();
        WebAdminWriteResult timerEntered = enter(timerFixture, "editor.edge.timer.invalid");
        WebAdminLogicChainEditorRequest timer = timerDraftRequest("editor.edge.timer.invalid", lockId(timerEntered), fingerprint(timerEntered), "editor.edge.timer");
        timer.edges = List.of(edge("timer-wrong-type", "channel:editor.timer.in", timer.nodes.getFirst().id, "join_input"));
        WebAdminWriteResult timerResult = timerFixture.service.saveDraft(null, timerFixture.editor, timerFixture.session, "127.0.0.1", timer, timerFixture.csrf, true);
        requireFalse(timerResult.success(), "Timer rejects non-timer draft edge type");
        requireValidationCode(timerResult, "logic_chain_edge_type_not_allowed_for_node");
        requireValidationCode(timerResult, "logic_chain_timer_output_edge_required");

        timer.edges = List.of(
                edge("timer-output-a", timer.nodes.getFirst().id, "channel:editor.timer.a", "timer_outputs_channel"),
                edge("timer-output-b", timer.nodes.getFirst().id, "channel:editor.timer.b", "timer_outputs_channel")
        );
        WebAdminWriteResult timerMultiOutput = timerFixture.service.saveDraft(null, timerFixture.editor, timerFixture.session, "127.0.0.1", timer, timerFixture.csrf, true);
        requireFalse(timerMultiOutput.success(), "Timer rejects more than one downstream output edge");
        requireValidationCode(timerMultiOutput, "logic_chain_timer_output_edge_single_required");

        timer.edges = List.of();
        WebAdminWriteResult timerNoOutput = timerFixture.service.saveDraft(null, timerFixture.editor, timerFixture.session, "127.0.0.1", timer, timerFixture.csrf, true);
        requireFalse(timerNoOutput.success(), "Timer without output edge requires onCompleteActions");
        requireValidationCode(timerNoOutput, "logic_chain_timer_output_edge_required");
    }

    private static void testSaveRejectsJoinInputOutputChannelConflict() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.conflict");
        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.join.conflict", lockId(entered), fingerprint(entered), "editor.join.conflict.saved");
        String draftId = join.nodes.getFirst().id;
        join.edges = List.of(
                edge("draft-edge-join-in-a", "channel:editor.join.shared", draftId, "join_input"),
                edge("draft-edge-join-in-b", "channel:editor.join.in.b", draftId, "join_input"),
                edge("draft-edge-join-out", draftId, "channel:editor.join.shared", "join_output")
        );

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);
        requireFalse(result.success(), "Signal Join rejects same channel as input and output");
        requireValidationCode(result, "logic_chain_join_input_output_channel_conflict");
        WebAdminValidationError conflict = findValidationError(result, "logic_chain_join_input_output_channel_conflict");
        requireTrue(conflict.message().contains("editor.join.shared"), "conflict message includes the channel id");
        requireEquals(draftId, conflict.nodeId(), "conflict error includes draft Join node id");
        requireEquals("draft-edge-join-out", conflict.edgeId(), "conflict error includes output edge id");
        requireEquals("editor.join.shared", conflict.channelId(), "conflict error includes channel id");
        requireTrue(containsChinese(conflict.fixHint()), "conflict error includes Chinese fix hint");
    }

    private static void testSaveAllowsVisualUpstreamOutputWhenNoRealCycle() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.visual");
        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.join.visual", lockId(entered), fingerprint(entered), "editor.join.visual.saved");
        String draftId = join.nodes.getFirst().id;
        join.edges = List.of(
                edge("draft-edge-join-in-b", "channel:b", draftId, "join_input"),
                edge("draft-edge-join-in-d", "channel:d", draftId, "join_input"),
                edge("draft-edge-join-out-a", draftId, "channel:a", "join_output")
        );

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);

        requireTrue(result.success(), "Join input b,d output a saves when no real a-to-input cycle exists");
        Map<?, ?> detail = fixture.signalJoinService.detail(null, fixture.editor, fixture.session, "editor.join.visual.saved");
        requireEquals("a", string(detail.get("outputChannel")), "visual upstream output saves as canonical output channel");
        List<?> savedInputs = (List<?>) detail.get("inputChannels");
        requireTrue(savedInputs.stream().filter(SignalJoinInputDefinition.class::isInstance).map(SignalJoinInputDefinition.class::cast).anyMatch(input -> "b".equals(input.channel)), "saved Join includes input channel b");
        requireTrue(savedInputs.stream().filter(SignalJoinInputDefinition.class::isInstance).map(SignalJoinInputDefinition.class::cast).anyMatch(input -> "d".equals(input.channel)), "saved Join includes input channel d");
        requireEquals(0, fixture.editLockService.activeLockCount(), "successful visual-upstream save releases editor and typed locks");
    }

    private static void testJoinCycleGuardRejectsReachableInputAndTruncates() {
        WebAdminDtos.LogicChainGraphDto visualOnlyGraph = graph(
                List.of(
                        logicNode("channel:a", "channel", "a"),
                        logicNode("channel:b", "channel", "b"),
                        logicNode("channel:d", "channel", "d")
                ),
                List.of()
        );
        List<WebAdminValidationError> visualAllowed = WebAdminLogicChainEditorService.draftJoinCycleGuardDiagnostics(
                visualOnlyGraph,
                Set.of("b", "d"),
                Set.of("a")
        );
        requireTrue(visualAllowed.isEmpty(), "visual upstream output a is allowed when no real cycle reaches b or d");
        for (WebAdminDtos.LogicChainEdgeDto skippedEdge : List.of(
                logicEdge("channel:a", "channel:b", "emits_downstream", true, Map.of()),
                logicEdge("channel:a", "channel:b", "emits_downstream", false, Map.of("nonTraversal", true)),
                logicEdge("channel:a", "channel:b", "emits_downstream", false, Map.of("visualOnly", true))
        )) {
            WebAdminDtos.LogicChainGraphDto skippedGraph = graph(visualOnlyGraph.nodes(), List.of(skippedEdge));
            List<WebAdminValidationError> skipped = WebAdminLogicChainEditorService.draftJoinCycleGuardDiagnostics(
                    skippedGraph,
                    Set.of("b", "d"),
                    Set.of("a")
            );
            requireTrue(skipped.isEmpty(), "reference / visual-only edges do not create Join cycle risk");
        }

        WebAdminDtos.LogicChainGraphDto directGraph = graph(
                List.of(
                        logicNode("channel:editor.cycle.direct.out", "channel", "editor.cycle.direct.out"),
                        logicNode("channel:editor.cycle.direct.in", "channel", "editor.cycle.direct.in")
                ),
                List.of(logicEdge("channel:editor.cycle.direct.out", "channel:editor.cycle.direct.in", "emits_downstream"))
        );
        List<WebAdminValidationError> direct = WebAdminLogicChainEditorService.draftJoinCycleGuardDiagnostics(
                directGraph,
                Set.of("editor.cycle.direct.in"),
                Set.of("editor.cycle.direct.out")
        );
        requireValidationCode(direct, "logic_chain_join_cycle_risk");

        WebAdminDtos.LogicChainGraphDto cycleGraph = graph(
                List.of(
                        logicNode("channel:editor.cycle.out", "channel", "editor.cycle.out"),
                        logicNode("signal_join:editor.cycle.existing", "signal_join", ""),
                        logicNode("channel:editor.cycle.in", "channel", "editor.cycle.in")
                ),
                List.of(
                        logicEdge("channel:editor.cycle.out", "signal_join:editor.cycle.existing", "join_input"),
                        logicEdge("signal_join:editor.cycle.existing", "channel:editor.cycle.in", "join_output")
                )
        );
        List<WebAdminValidationError> cycle = WebAdminLogicChainEditorService.draftJoinCycleGuardDiagnostics(
                cycleGraph,
                Set.of("editor.cycle.in"),
                Set.of("editor.cycle.out")
        );
        requireValidationCode(cycle, "logic_chain_join_cycle_risk");
        WebAdminValidationError cycleError = findValidationError(cycle, "logic_chain_join_cycle_risk");
        requireEquals("editor.cycle.out", cycleError.channelId(), "cycle diagnostic includes output channel id");
        requireTrue(cycleError.message().contains("editor.cycle.out"), "cycle diagnostic message names output channel");
        requireTrue(cycleError.message().contains("channel:editor.cycle.out"), "cycle diagnostic includes path start node");
        requireTrue(cycleError.message().contains("signal_join:editor.cycle.existing"), "cycle diagnostic includes intermediate node");
        requireTrue(cycleError.message().contains("channel:editor.cycle.in"), "cycle diagnostic includes reached input node");
        requireTrue(containsChinese(cycleError.fixHint()), "cycle diagnostic includes Chinese fix hint");

        List<WebAdminDtos.LogicChainEdgeDto> longEdges = new java.util.ArrayList<>();
        String previous = "channel:editor.cycle.truncated.out";
        for (int i = 0; i < 270; i++) {
            String next = "logic_chain_guard_node:" + i;
            longEdges.add(logicEdge(previous, next, "emits_downstream"));
            previous = next;
        }
        WebAdminDtos.LogicChainGraphDto truncatedGraph = graph(
                List.of(logicNode("channel:editor.cycle.truncated.out", "channel", "editor.cycle.truncated.out")),
                longEdges
        );
        List<WebAdminValidationError> truncated = WebAdminLogicChainEditorService.draftJoinCycleGuardDiagnostics(
                truncatedGraph,
                Set.of("editor.cycle.truncated.in"),
                Set.of("editor.cycle.truncated.out")
        );
        requireValidationCode(truncated, "logic_chain_join_cycle_guard_truncated");
    }

    private static void testSaveRejectsDirectNonChannelVisualEndpoints() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.endpoint");
        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.join.endpoint", lockId(entered), fingerprint(entered), "editor.join.endpoint.saved");
        String draftId = join.nodes.getFirst().id;
        join.edges = List.of(
                edge("draft-edge-producer", "producer:device.alpha:channel", draftId, "join_input"),
                edge("draft-edge-join-in-b", "channel:editor.endpoint.in.b", draftId, "join_input"),
                edge("draft-edge-consumer", draftId, "consumer:listener.alpha", "join_output")
        );

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);
        requireFalse(result.success(), "Join endpoints must resolve to canonical channel refs, not visual producer / consumer ids");
        requireValidationCode(result, "logic_chain_edge_endpoint_not_channel");
    }

    private static void testChannelMetadataDraftValidation() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.channel.metadata.valid");
        WebAdminLogicChainEditorRequest valid = timerDraftRequest("editor.channel.metadata.valid", lockId(entered), fingerprint(entered), "editor.channel.metadata.timer");
        valid.channelMetadataDrafts = List.of(channelMetadataDraft("editor.timer.out", "Timer 输出频道", "由频道端点 picker 创建"));
        WebAdminWriteResult validResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, valid, fixture.csrf, true);
        requireTrue(validResult.success(), "connected channel metadata draft does not block draft validation");

        Fixture appendSignalFixture = fixture();
        WebAdminWriteResult appendSignalEntered = enter(appendSignalFixture, "editor.channel.metadata.append.signal");
        WebAdminLogicChainEditorRequest appendSignal = actionAppendDraftRequest("editor.channel.metadata.append.signal", lockId(appendSignalEntered), fingerprint(appendSignalEntered), "listener", "owner.editor.channel.metadata.append.signal", "", signalAction("editor.append.signal.out"));
        appendSignal.channelMetadataDrafts = List.of(channelMetadataDraft("editor.append.signal.out", "追加 Signal 输出频道", "由 Signal action append 创建"));
        WebAdminWriteResult appendSignalResult = appendSignalFixture.service.validateDraft(null, appendSignalFixture.editor, appendSignalFixture.session, appendSignal, appendSignalFixture.csrf, true);
        requireTrue(appendSignalResult.success(), "Signal action append may reference its own output channel metadata");

        Fixture orphanFixture = fixture();
        WebAdminWriteResult orphanEntered = enter(orphanFixture, "editor.channel.metadata.orphan");
        WebAdminLogicChainEditorRequest orphan = timerDraftRequest("editor.channel.metadata.orphan", lockId(orphanEntered), fingerprint(orphanEntered), "editor.channel.metadata.orphan.timer");
        orphan.channelMetadataDrafts = List.of(channelMetadataDraft("editor.channel.metadata.orphan", "孤立频道", "未被当前连线引用"));
        WebAdminWriteResult orphanResult = orphanFixture.service.validateDraft(null, orphanFixture.editor, orphanFixture.session, orphan, orphanFixture.csrf, true);
        requireFalse(orphanResult.success(), "orphan channel metadata draft is rejected");
        requireValidationCode(orphanResult, "logic_chain_channel_metadata_unreferenced");

        Fixture duplicateFixture = fixture();
        WebAdminWriteResult duplicateEntered = enter(duplicateFixture, "editor.channel.metadata.duplicate");
        WebAdminLogicChainEditorRequest duplicate = timerDraftRequest("editor.channel.metadata.duplicate", lockId(duplicateEntered), fingerprint(duplicateEntered), "editor.channel.metadata.duplicate.timer");
        duplicate.channelMetadataDrafts = List.of(
                channelMetadataDraft("editor.channel.metadata.same", "第一个", ""),
                channelMetadataDraft("editor.channel.metadata.same", "第二个", "")
        );
        WebAdminWriteResult duplicateResult = duplicateFixture.service.validateDraft(null, duplicateFixture.editor, duplicateFixture.session, duplicate, duplicateFixture.csrf, true);
        requireFalse(duplicateResult.success(), "duplicate channel metadata drafts are rejected");
        requireValidationCode(duplicateResult, "duplicate_channel");
        requireStructuredError(duplicateResult, "duplicate_channel", "", "editor.channel.metadata.same");

        Fixture appendFakeEdgeFixture = fixture();
        WebAdminWriteResult appendFakeEdgeEntered = enter(appendFakeEdgeFixture, "editor.channel.metadata.append.fake-edge");
        WebAdminLogicChainEditorRequest appendFakeEdge = actionAppendDraftRequest("editor.channel.metadata.append.fake-edge", lockId(appendFakeEdgeEntered), fingerprint(appendFakeEdgeEntered), "timer", "owner.editor.channel.metadata.append.fake-edge", "complete", messageAction("append"));
        appendFakeEdge.edges = List.of(edge("fake-edge-metadata-ref", "channel:editor.append.fake.edge", "draft:signal_join:fake", "join_input"));
        appendFakeEdge.channelMetadataDrafts = List.of(channelMetadataDraft("editor.append.fake.edge", "伪造连线频道", "不应被 action append edges 引用"));
        WebAdminWriteResult appendFakeEdgeResult = appendFakeEdgeFixture.service.validateDraft(null, appendFakeEdgeFixture.editor, appendFakeEdgeFixture.session, appendFakeEdge, appendFakeEdgeFixture.csrf, true);
        requireFalse(appendFakeEdgeResult.success(), "action append cannot use fake draft edges to save orphan metadata");
        requireValidationCode(appendFakeEdgeResult, "logic_chain_action_append_edges_not_allowed");
        requireValidationCode(appendFakeEdgeResult, "logic_chain_channel_metadata_unreferenced");
    }

    private static void testSaveRejectsModeSpecificJoinThresholdErrors() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.threshold");
        WebAdminLogicChainEditorRequest anyN = signalJoinDraftRequest("editor.join.threshold", lockId(entered), fingerprint(entered), "editor.join.threshold.any");
        anyN.nodes.getFirst().signalJoin.mode = "ANY_N";
        anyN.nodes.getFirst().signalJoin.threshold = 3;
        WebAdminWriteResult anyNResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", anyN, fixture.csrf, true);
        requireFalse(anyNResult.success(), "ANY_N threshold greater than connected input count is rejected");
        requireValidationCode(anyNResult, "logic_chain_join_any_n_threshold_exceeds_inputs");
        requireStructuredError(anyNResult, "logic_chain_join_any_n_threshold_exceeds_inputs", anyN.nodes.getFirst().id, "");

        Fixture countFixture = fixture();
        WebAdminWriteResult countEntered = enter(countFixture, "editor.join.count.threshold");
        WebAdminLogicChainEditorRequest count = signalJoinDraftRequest("editor.join.count.threshold", lockId(countEntered), fingerprint(countEntered), "editor.join.threshold.count");
        count.nodes.getFirst().signalJoin.mode = "COUNT";
        count.nodes.getFirst().signalJoin.threshold = 0;
        WebAdminWriteResult countResult = countFixture.service.saveDraft(null, countFixture.editor, countFixture.session, "127.0.0.1", count, countFixture.csrf, true);
        requireFalse(countResult.success(), "COUNT threshold below one is rejected");
        requireValidationCode(countResult, "logic_chain_join_count_threshold_required");
        requireStructuredError(countResult, "logic_chain_join_count_threshold_required", count.nodes.getFirst().id, "");

        Fixture allFixture = fixture();
        WebAdminWriteResult allEntered = enter(allFixture, "editor.join.all.threshold");
        WebAdminLogicChainEditorRequest all = signalJoinDraftRequest("editor.join.all.threshold", lockId(allEntered), fingerprint(allEntered), "editor.join.threshold.all");
        all.nodes.getFirst().signalJoin.mode = "ALL";
        all.nodes.getFirst().signalJoin.threshold = 0;
        WebAdminWriteResult allResult = allFixture.service.saveDraft(null, allFixture.editor, allFixture.session, "127.0.0.1", all, allFixture.csrf, true);
        requireTrue(allResult.success(), "ALL mode does not require user-entered threshold");
    }

    private static void testSaveRejectsInvalidPlacementAndUnplacedDraft() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.placement");
        WebAdminLogicChainEditorRequest timer = timerDraftRequest("editor.placement", lockId(entered), fingerprint(entered), "editor.placement.timer");
        timer.nodes.getFirst().column = "C5";
        WebAdminWriteResult timerResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", timer, fixture.csrf, true);
        requireFalse(timerResult.success(), "Timer C5 placement is deferred in 8.14");
        requireValidationCode(timerResult, "logic_chain_timer_column_deferred");
        requireStructuredError(timerResult, "logic_chain_timer_column_deferred", timer.nodes.getFirst().id, "");

        WebAdminLogicChainEditorRequest join = signalJoinDraftRequest("editor.placement", lockId(entered), fingerprint(entered), "editor.placement.join");
        join.nodes.getFirst().placed = false;
        WebAdminWriteResult joinResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", join, fixture.csrf, true);
        requireFalse(joinResult.success(), "unplaced draft cannot be saved");
        requireValidationCode(joinResult, "logic_chain_draft_node_not_placed");

        Fixture expandedFixture = fixture();
        WebAdminWriteResult expandedEntered = enter(expandedFixture, "editor.placement.expanded");
        WebAdminLogicChainEditorRequest joinC3 = signalJoinDraftRequest("editor.placement.expanded", lockId(expandedEntered), fingerprint(expandedEntered), "editor.placement.join.c3");
        joinC3.nodes.getFirst().column = "C3";
        joinC3.nodes.getFirst().slot = 3;
        WebAdminWriteResult joinC3Result = expandedFixture.service.saveDraft(null, expandedFixture.editor, expandedFixture.session, "127.0.0.1", joinC3, expandedFixture.csrf, true);
        requireTrue(joinC3Result.success(), "Signal Join can be placed in the expanded C3 processing slot");

        Fixture illegalFixture = fixture();
        WebAdminWriteResult illegalEntered = enter(illegalFixture, "editor.placement.illegal");
        WebAdminLogicChainEditorRequest joinC4 = signalJoinDraftRequest("editor.placement.illegal", lockId(illegalEntered), fingerprint(illegalEntered), "editor.placement.join.c4");
        joinC4.nodes.getFirst().column = "C4";
        WebAdminWriteResult joinC4Result = illegalFixture.service.validateDraft(null, illegalFixture.editor, illegalFixture.session, joinC4, illegalFixture.csrf, true);
        requireTrue(joinC4Result.success(), "Signal Join can be placed in a dynamic downstream channel column even when that visual column is occupied");

        WebAdminLogicChainEditorRequest joinC5 = signalJoinDraftRequest("editor.placement.illegal", lockId(illegalEntered), fingerprint(illegalEntered), "editor.placement.join.c5");
        joinC5.nodes.getFirst().column = "C5";
        WebAdminWriteResult joinC5Result = illegalFixture.service.validateDraft(null, illegalFixture.editor, illegalFixture.session, joinC5, illegalFixture.csrf, true);
        requireTrue(joinC5Result.success(), "Signal Join can be placed in a farther visual downstream channel column");

        WebAdminLogicChainEditorRequest joinC0 = signalJoinDraftRequest("editor.placement.illegal", lockId(illegalEntered), fingerprint(illegalEntered), "editor.placement.join.c0");
        joinC0.nodes.getFirst().column = "C0";
        WebAdminWriteResult joinC0Result = illegalFixture.service.validateDraft(null, illegalFixture.editor, illegalFixture.session, joinC0, illegalFixture.csrf, true);
        requireFalse(joinC0Result.success(), "Signal Join cannot be placed in source columns");
        requireValidationCode(joinC0Result, "logic_chain_join_column_invalid");

    }

    private static void testActionAppendRejectsInvalidShapeAndMixedDraft() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.action.append.invalid");
        WebAdminLogicChainEditorRequest append = rootRequest("editor.action.append.invalid");
        append.lockId = lockId(entered);
        append.baseGraphFingerprint = fingerprint(entered);
        append.actionAppend = new WebAdminLogicChainEditorRequest.ActionAppendDraft();
        append.actionAppend.ownerType = "timer";
        append.actionAppend.ownerId = "";
        append.actionAppend.bucket = "bad";
        append.actionAppend.action = messageAction("append");
        append.actionAppend.expectedFingerprint = "";
        append.actionAppend.lockId = "";

        WebAdminWriteResult invalid = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", append, fixture.csrf, true);
        requireFalse(invalid.success(), "invalid action append shape is rejected");
        requireValidationCode(invalid, "logic_chain_action_append_owner_id_required");
        requireStructuredError(invalid, "logic_chain_action_append_owner_id_required", "timer:", "");
        requireValidationCode(invalid, "logic_chain_timer_action_bucket_invalid");
        requireStructuredError(invalid, "logic_chain_timer_action_bucket_invalid", "timer:", "");

        append.nodes = signalJoinDraftRequest("editor.action.append.invalid", lockId(entered), fingerprint(entered), "editor.action.append.mixed").nodes;
        WebAdminWriteResult mixed = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", append, fixture.csrf, true);
        requireFalse(mixed.success(), "action append cannot be mixed with a new node draft");
        requireValidationCode(mixed, "logic_chain_draft_single_write_only");
    }

    private static void testActionAppendValidationCoversSupportedOwnersBucketsAndConditionGroup() throws Exception {
        for (String[] owner : List.of(
                new String[]{"listener", ""},
                new String[]{"action_relay", ""},
                new String[]{"region_controller", "enter"},
                new String[]{"region_controller", "exit"},
                new String[]{"region_controller", "stay"},
                new String[]{"timer", "tick"},
                new String[]{"timer", "complete"}
        )) {
            Fixture fixture = fixture();
            String bucketSuffix = owner[1].isBlank() ? "actions" : owner[1];
            String rootRef = "editor.append.validation." + owner[0] + "." + bucketSuffix;
            WebAdminWriteResult entered = enter(fixture, rootRef);
            WebAdminLogicChainEditorRequest append = actionAppendDraftRequest(rootRef, lockId(entered), fingerprint(entered), owner[0], "owner." + rootRef, owner[1], messageAction("append " + rootRef));
            WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, append, fixture.csrf, true);
            requireTrue(result.success(), "valid append-only action shape is accepted for " + owner[0] + "/" + owner[1]);
        }

        WebAdminActionRelayActionsUpdateRequest.ActionEntry gated = messageAction("condition gated append");
        gated.conditionGroupId = "editor_action_gate";
        requireEquals("editor_action_gate", WebAdminActionRelayActionsService.actionFromEntry(gated).conditionGroupId(), "new action conditionGroupId roundtrips through action conversion");
    }

    private static void testSaveRejectsWrongLockAndSameOriginFailure() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.security.save");
        WebAdminLogicChainEditorRequest request = timerDraftRequest("editor.security.save", lockId(entered), fingerprint(entered), "editor.security.timer");

        WebAdminWriteResult sameOrigin = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, false);
        requireFalse(sameOrigin.success(), "save requires same-origin write request");
        requireEquals("csrf_invalid", sameOrigin.code(), "same-origin failure uses CSRF invalid code");

        request.lockId = "wrong-lock-id";
        WebAdminWriteResult wrongLock = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(wrongLock.success(), "save rejects wrong editor lock");
        requireEquals("edit_lock_conflict", wrongLock.code(), "wrong lock reports edit lock conflict");
    }

    private static void testSaveSignalJoinDraftWritesUnderlyingConfig() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.root");
        WebAdminLogicChainEditorRequest request = signalJoinDraftRequest("editor.join.root", lockId(entered), fingerprint(entered), "editor.join.saved");
        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "Signal Join draft save succeeds");
        Map<?, ?> detail = fixture.signalJoinService.detail(null, fixture.editor, fixture.session, "editor.join.saved");
        requireEquals("editor.join.out", string(detail.get("outputChannel")), "Signal Join save writes output channel to underlying config");
        requireEquals(2, ((List<?>) detail.get("inputChannels")).size(), "Signal Join save writes input channels to underlying config");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and typed locks are released after Signal Join save");
    }

    private static void testSaveTimerDraftWritesUnderlyingConfig() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.timer.root");
        WebAdminLogicChainEditorRequest request = timerDraftRequest("editor.timer.root", lockId(entered), fingerprint(entered), "editor.timer.saved");
        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "Timer draft save succeeds");
        Map<?, ?> detail = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.saved");
        requireEquals("editor.timer.out", string(detail.get("outputChannel")), "Timer save writes output channel to underlying config");
        requireEquals("DELAY", string(detail.get("mode")), "Timer save writes internal mode to underlying config");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and typed locks are released after Timer save");
    }

    private static void testTimerDraftCanCreateAndSelectDownstreamChannelEndpoint() throws Exception {
        Fixture createFixture = fixture();
        WebAdminWriteResult createEntered = enter(createFixture, "editor.timer.endpoint.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.timer.endpoint.create", lockId(createEntered), fingerprint(createEntered), "editor.timer.endpoint.create.saved");
        String draftId = create.nodes.getFirst().id;
        create.edges = List.of(edge("draft-edge-timer-new-out", draftId, "channel:editor.timer.endpoint.new", "timer_outputs_channel"));
        create.channelMetadataDrafts = List.of(channelMetadataDraft("editor.timer.endpoint.new", "新 Timer 输出频道", "Timer 下游端点创建"));
        WebAdminWriteResult createResult = createFixture.service.saveDraft(null, createFixture.editor, createFixture.session, "127.0.0.1", create, createFixture.csrf, true);
        requireTrue(createResult.success(), "Timer draft can create a downstream channel endpoint");
        Map<?, ?> createdDetail = createFixture.timerService.detail(null, createFixture.editor, createFixture.session, "editor.timer.endpoint.create.saved");
        requireEquals("editor.timer.endpoint.new", string(createdDetail.get("outputChannel")), "new Timer endpoint writes Timer outputChannel");

        Fixture selectFixture = fixture();
        WebAdminWriteResult selectEntered = enter(selectFixture, "editor.timer.endpoint.select");
        WebAdminLogicChainEditorRequest select = timerDraftRequest("editor.timer.endpoint.select", lockId(selectEntered), fingerprint(selectEntered), "editor.timer.endpoint.select.saved");
        select.edges = List.of(edge("draft-edge-timer-existing-out", select.nodes.getFirst().id, "channel:editor.timer.out", "timer_outputs_channel"));
        WebAdminWriteResult selectResult = selectFixture.service.saveDraft(null, selectFixture.editor, selectFixture.session, "127.0.0.1", select, selectFixture.csrf, true);
        requireTrue(selectResult.success(), "Timer draft can select an existing downstream channel endpoint");
        Map<?, ?> selectedDetail = selectFixture.timerService.detail(null, selectFixture.editor, selectFixture.session, "editor.timer.endpoint.select.saved");
        requireEquals("editor.timer.out", string(selectedDetail.get("outputChannel")), "existing Timer endpoint writes Timer outputChannel");
    }

    private static void testSaveDraftNormalizesTypedLockTargetsBeforeSaving() throws Exception {
        Fixture joinFixture = fixture();
        WebAdminWriteResult joinEntered = enter(joinFixture, "editor.join.normalized.root");
        String rawJoinId = "8.14 Join Test";
        String normalizedJoinId = SignalJoinStore.normalizeId(rawJoinId);
        WebAdminLogicChainEditorRequest joinRequest = signalJoinDraftRequest("editor.join.normalized.root", lockId(joinEntered), fingerprint(joinEntered), rawJoinId);
        WebAdminWriteResult joinResult = joinFixture.service.saveDraft(null, joinFixture.editor, joinFixture.session, "127.0.0.1", joinRequest, joinFixture.csrf, true);
        requireTrue(joinResult.success(), "Signal Join draft save normalizes typed lock target before create");
        Map<?, ?> joinDetail = joinFixture.signalJoinService.detail(null, joinFixture.editor, joinFixture.session, normalizedJoinId);
        requireEquals(normalizedJoinId, string(joinDetail.get("id")), "Signal Join save stores normalized id");
        requireEquals("editor.join.out", string(joinDetail.get("outputChannel")), "Signal Join normalized-id save writes output channel");
        requireEquals(0, joinFixture.editLockService.activeLockCount(), "normalized Signal Join save releases editor and typed locks");

        Fixture timerFixture = fixture();
        WebAdminWriteResult timerEntered = enter(timerFixture, "editor.timer.normalized.root");
        String rawTimerId = "8.14 Timer Test";
        String normalizedTimerId = TimerStore.normalizeId(rawTimerId);
        WebAdminLogicChainEditorRequest timerRequest = timerDraftRequest("editor.timer.normalized.root", lockId(timerEntered), fingerprint(timerEntered), rawTimerId);
        WebAdminWriteResult timerResult = timerFixture.service.saveDraft(null, timerFixture.editor, timerFixture.session, "127.0.0.1", timerRequest, timerFixture.csrf, true);
        requireTrue(timerResult.success(), "Timer draft save normalizes typed lock target before create");
        Map<?, ?> timerDetail = timerFixture.timerService.detail(null, timerFixture.editor, timerFixture.session, normalizedTimerId);
        requireEquals(normalizedTimerId, string(timerDetail.get("id")), "Timer save stores normalized id");
        requireEquals("editor.timer.out", string(timerDetail.get("outputChannel")), "Timer normalized-id save writes output channel");
        requireEquals(0, timerFixture.editLockService.activeLockCount(), "normalized Timer save releases editor and typed locks");
    }

    private static void testSaveTimerDraftAllowsOnCompleteOnlyOutput() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.timer.actions");
        WebAdminLogicChainEditorRequest request = timerDraftRequest("editor.timer.actions", lockId(entered), fingerprint(entered), "editor.timer.actions.saved");
        request.edges = List.of();
        request.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("done without output channel"));

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "Timer draft with onCompleteActions can save without downstream output edge");
        Map<?, ?> detail = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.actions.saved");
        requireEquals("", string(detail.get("outputChannel")), "Timer onComplete-only save keeps output channel empty");
        requireEquals(1, ((List<?>) detail.get("onCompleteActions")).size(), "Timer onComplete-only save writes action bucket");
    }

    private static void testTimerActionAppendThroughLogicChainEditor() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.timer.append.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.timer.append.create", lockId(createdEditor), fingerprint(createdEditor), "editor.timer.append.saved");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("timer existing complete"));
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "Timer draft can be created before append test");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.append.saved");
        String expectedFingerprint = string(before.get("expectedFingerprint"));
        String timerLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.timer.append.saved");
        WebAdminWriteResult entered = enter(fixture, "editor.timer.append.root");
        WebAdminLogicChainEditorRequest append = rootRequest("editor.timer.append.root");
        append.lockId = lockId(entered);
        append.baseGraphFingerprint = fingerprint(entered);
        append.nodes = List.of();
        append.edges = List.of();
        append.actionAppend = new WebAdminLogicChainEditorRequest.ActionAppendDraft();
        append.actionAppend.ownerType = "timer";
        append.actionAppend.ownerId = "editor.timer.append.saved";
        append.actionAppend.bucket = "complete";
        append.actionAppend.action = messageAction("timer complete append");
        append.actionAppend.expectedFingerprint = expectedFingerprint;
        append.actionAppend.lockId = timerLockId;

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", append, fixture.csrf, true);
        requireTrue(result.success(), "Logic Chain editor appends one Timer complete Action");
        Map<?, ?> after = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.append.saved");
        List<?> actions = (List<?>) after.get("onCompleteActions");
        requireEquals(2, actions.size(), "Timer append preserves existing order and adds one action");
        requireEquals("timer existing complete", string(((Map<?, ?>) actions.get(0)).get("value")), "existing Timer complete action stays first");
        requireEquals("timer complete append", string(((Map<?, ?>) actions.get(1)).get("value")), "appended Timer complete action is last");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and timer locks are released after append");
    }

    private static WebAdminWriteResult enter(Fixture fixture, String rootRef) {
        WebAdminWriteResult result = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", rootRequest(rootRef), fixture.csrf, true);
        requireTrue(result.success(), "enter edit mode for " + rootRef);
        return result;
    }

    private static WebAdminLogicChainEditorRequest signalJoinDraftRequest(String rootRef, String lockId, String fingerprint, String joinId) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:signal_join:" + joinId;
        node.type = "signal_join";
        node.column = "C2";
        node.slot = 0;
        node.placed = true;
        node.signalJoin.id = joinId;
        node.signalJoin.displayName = "Editor Join";
        node.signalJoin.mode = "ALL";
        node.signalJoin.threshold = 2;
        node.signalJoin.scopeMode = "GLOBAL";
        node.signalJoin.resetPolicy = "RESET_AFTER_EMIT";
        node.signalJoin.inputChannels = List.of();
        node.signalJoin.outputChannel = "";
        request.nodes = List.of(node);
        request.edges = List.of(
                edge("draft-edge-join-in", "channel:editor.join.in.a", node.id, "join_input"),
                edge("draft-edge-join-in-b", "channel:editor.join.in.b", node.id, "join_input"),
                edge("draft-edge-join-out", node.id, "channel:editor.join.out", "join_output")
        );
        return request;
    }

    private static WebAdminLogicChainEditorRequest timerDraftRequest(String rootRef, String lockId, String fingerprint, String timerId) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:timer:" + timerId;
        node.type = "timer";
        node.column = "C0";
        node.slot = 0;
        node.placed = true;
        node.timer.id = timerId;
        node.timer.displayName = "Editor Timer";
        node.timer.mode = "DELAY";
        node.timer.scopeMode = "GLOBAL";
        node.timer.durationTicks = 40L;
        node.timer.intervalTicks = 0L;
        node.timer.maxRuns = 1;
        node.timer.startPolicy = "RESTART";
        node.timer.outputChannel = "";
        request.nodes = List.of(node);
        request.edges = List.of(edge("draft-edge-timer-out", node.id, "channel:editor.timer.out", "timer_outputs_channel"));
        return request;
    }

    private static String acquireLock(Fixture fixture, String targetType, String targetId) {
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = targetType;
        request.targetId = targetId;
        WebAdminWriteResult result = fixture.editLockService.acquire(fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "acquire " + targetType + " lock for " + targetId);
        return lockId(result);
    }

    private static WebAdminLogicChainEditorRequest rootRequest(String rootRef) {
        WebAdminLogicChainEditorRequest request = new WebAdminLogicChainEditorRequest();
        request.rootType = "channel";
        request.rootRef = rootRef;
        request.includeDisabled = true;
        request.maxDepth = 3;
        return request;
    }

    private static WebAdminLogicChainEditorRequest.DraftEdge edge(String id, String from, String to, String type) {
        WebAdminLogicChainEditorRequest.DraftEdge edge = new WebAdminLogicChainEditorRequest.DraftEdge();
        edge.id = id;
        edge.from = from;
        edge.to = to;
        edge.type = type;
        return edge;
    }

    private static WebAdminDtos.LogicChainGraphDto graph(
            List<WebAdminDtos.LogicChainNodeDto> nodes,
            List<WebAdminDtos.LogicChainEdgeDto> edges
    ) {
        return new WebAdminDtos.LogicChainGraphDto(null, null, List.of(), nodes, edges, List.of(), Map.of());
    }

    private static WebAdminDtos.LogicChainNodeDto logicNode(String id, String type, String channel) {
        return new WebAdminDtos.LogicChainNodeDto(
                id,
                type,
                type,
                channel,
                id,
                "",
                channel,
                true,
                "OK",
                "OK",
                "",
                "",
                Map.of()
        );
    }

    private static WebAdminDtos.LogicChainEdgeDto logicEdge(String from, String to, String type) {
        return logicEdge(from, to, type, false, Map.of());
    }

    private static WebAdminDtos.LogicChainEdgeDto logicEdge(String from, String to, String type, boolean referenceEdge, Map<String, Object> metadata) {
        return new WebAdminDtos.LogicChainEdgeDto(
                from,
                to,
                type,
                "",
                "solid",
                "signal",
                "",
                referenceEdge,
                metadata == null ? Map.of() : metadata
        );
    }

    private static WebAdminLogicChainEditorRequest actionAppendDraftRequest(
            String rootRef,
            String lockId,
            String fingerprint,
            String ownerType,
            String ownerId,
            String bucket,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry action
    ) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        request.nodes = List.of();
        request.edges = List.of();
        request.actionAppend = new WebAdminLogicChainEditorRequest.ActionAppendDraft();
        request.actionAppend.ownerType = ownerType;
        request.actionAppend.ownerId = ownerId;
        request.actionAppend.bucket = bucket;
        request.actionAppend.action = action;
        request.actionAppend.expectedFingerprint = "owner-fingerprint";
        request.actionAppend.lockId = "typed-owner-lock";
        return request;
    }

    private static WebAdminLogicChainEditorRequest.ChannelMetadataDraft channelMetadataDraft(String channel, String displayName, String note) {
        WebAdminLogicChainEditorRequest.ChannelMetadataDraft draft = new WebAdminLogicChainEditorRequest.ChannelMetadataDraft();
        draft.channel = channel;
        draft.displayName = displayName;
        draft.note = note;
        draft.iconKey = "auto";
        return draft;
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

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry signalAction(String channel) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = messageAction(channel);
        entry.type = "signal";
        return entry;
    }

    private static Fixture fixture() throws Exception {
        return fixture(60_000L);
    }

    private static Fixture fixture(long lockTtlMillis) throws Exception {
        Path directory = Files.createTempDirectory("tzz-logic-chain-editor-service-test");
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminPermissionService permission = new WebAdminPermissionService();
        WebAdminEditLockService editLockService = new WebAdminEditLockService(permission, security, lockTtlMillis);
        WebAdminSignalJoinService signalJoinService = new WebAdminSignalJoinService(permission, security, editLockService, directory.resolve("signal_joins.json"));
        WebAdminTimerService timerService = new WebAdminTimerService(permission, security, editLockService, directory.resolve("timers.json"));
        WebAdminLogicChainService logicChainService = new WebAdminLogicChainService(permission, security, editLockService);
        WebAdminSignalListenerActionsService signalListenerActionsService = new WebAdminSignalListenerActionsService(permission, security, editLockService);
        WebAdminActionRelayActionsService actionRelayActionsService = new WebAdminActionRelayActionsService(permission, security, editLockService);
        WebAdminRegionControllerService regionControllerService = new WebAdminRegionControllerService(permission, security, editLockService);
        WebAdminLogicChainEditorService service = new WebAdminLogicChainEditorService(
                permission,
                security,
                editLockService,
                logicChainService,
                signalJoinService,
                timerService,
                signalListenerActionsService,
                actionRelayActionsService,
                regionControllerService
        );
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        return new Fixture(service, signalJoinService, timerService, security, editLockService, editor, session, security.csrfTokenFor(session));
    }

    private static String lockId(WebAdminWriteResult result) {
        Object lock = result == null || result.data() == null ? null : result.data().get("lock");
        if (lock instanceof WebAdminEditLockStatusDto status) {
            return status.lockId();
        }
        if (lock instanceof Map<?, ?> map) {
            Object id = map.get("lockId");
            return id == null ? "" : String.valueOf(id);
        }
        return "";
    }

    private static String fingerprint(WebAdminWriteResult result) {
        return string(result == null || result.data() == null ? "" : result.data().get("baseGraphFingerprint"));
    }

    private static WebAdminUser user(WebAdminRole role) {
        return user(role, role.id().toLowerCase(Locale.ROOT));
    }

    private static WebAdminUser user(WebAdminRole role, String username) {
        WebAdminUser user = new WebAdminUser();
        user.username = username;
        user.displayName = username;
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
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

    private static void requireValidationCode(List<WebAdminValidationError> errors, String code) {
        for (var error : errors) {
            if (code.equals(error.code())) {
                requireTrue(containsChinese(error.message()), "validation code " + code + " has Chinese message");
                return;
            }
        }
        throw new AssertionError("missing validation code " + code + " errors=" + errors);
    }

    private static WebAdminValidationError findValidationError(WebAdminWriteResult result, String code) {
        return findValidationError(result.validationErrors(), code);
    }

    private static WebAdminValidationError findValidationError(List<WebAdminValidationError> errors, String code) {
        for (WebAdminValidationError error : errors) {
            if (code.equals(error.code())) {
                return error;
            }
        }
        throw new AssertionError("missing validation code " + code + " errors=" + errors);
    }

    private static void requireStructuredError(WebAdminWriteResult result, String code, String nodeId, String channelId) {
        WebAdminValidationError error = findValidationError(result, code);
        requireEquals("error", error.severity(), "structured validation includes severity for " + code);
        requireTrue(containsChinese(error.fixHint()), "structured validation includes Chinese fix hint for " + code);
        if (!string(nodeId).isBlank()) {
            requireEquals(nodeId, error.nodeId(), "structured validation includes node id for " + code);
        }
        if (!string(channelId).isBlank()) {
            requireEquals(channelId, error.channelId(), "structured validation includes channel id for " + code);
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
            WebAdminLogicChainEditorService service,
            WebAdminSignalJoinService signalJoinService,
            WebAdminTimerService timerService,
            WebAdminWriteSecurityService security,
            WebAdminEditLockService editLockService,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf
    ) {
    }
}
