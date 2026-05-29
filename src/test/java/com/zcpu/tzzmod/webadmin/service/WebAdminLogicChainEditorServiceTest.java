package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest;
import com.zcpu.tzzmod.webadmin.draft.WebAdminProtectedDraftRegistry;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionPurpose;
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
        testMalformedDraftListsRemainValidationFailures();
        testDraftOperationPlannerPreservesTypedWriteOrderBoundaries();
        testSaveRejectsModeSpecificJoinThresholdErrors();
        testSaveRejectsInvalidPlacementAndUnplacedDraft();
        testActionAppendRejectsInvalidShapeAndAllowsMixedDraft();
        testActionAppendValidationCoversSupportedOwnersBucketsAndConditionGroup();
        testActionEditValidationCoversActionRelayAndRegionBuckets();
        testSaveRejectsWrongLockAndSameOriginFailure();
        testSaveSignalJoinDraftWritesUnderlyingConfig();
        testSaveTimerDraftWritesUnderlyingConfig();
        testSaveSignalListenerDraftWritesUnderlyingConfig();
        testTimerDraftCanCreateAndSelectDownstreamChannelEndpoint();
        testSaveDraftNormalizesTypedLockTargetsBeforeSaving();
        testSaveTimerDraftAllowsOnCompleteOnlyOutput();
        testTimerActionAppendThroughLogicChainEditor();
        testExistingChannelMetadataEditValidatesTypedPayload();
        testExistingChannelMetadataEditSavesUnderlyingMetadata();
        testExistingTimerNodeEditWritesUnderlyingConfig();
        testExistingSignalListenerBasicEditWritesUnderlyingConfig();
        testExistingSignalJoinEditRejectsInputOutputOverlap();
        testExistingNodeEditRejectsTargetOutsideCurrentGraph();
        testExistingNodeEditAllowsReferencedChannelMetadataDrafts();
        testExistingTypedEditsRejectDraftEdgesAndAllowMultipleTargets();
        testMultiEditValidationFieldsUseActualIndexes();
        testMultiDraftSessionSavesNewNodeExistingEditAndMetadata();
        testMultiActionEditsSaveAcrossOwners();
        testTimerExistingNodeEditCannotMutateActionList();
        testTimerSameIndexActionEditReplacesWithoutReorder();
        testSignalListenerSameIndexActionEditReplacesWithoutReorder();
        testExistingActionEditStructuredPayloadConversion();
        testExistingActionEditStructuredSaveRoundtrip();
        testActionDisableCoercedServerSide();
        testActionEditRejectsDeleteAndReorderOperations();
        testMultipleDraftOperationsValidateConflictsAndAllowNonConflictingDeletesReorders();
        testTargetLockPreflightReportsMissingTargetLockBeforeTypedSave();
        testSelectionStartRequiresLogicChainEditorLock();
        testLogicChainClientAssistedSelectionPurposesAreDistinct();
        testSignalListenerCreateRequiresLockAndCreateFingerprint();
        testProtectedDraftRegistryRequiresActorForMutation();
        testProtectedDraftRegistryTerminalStateAndDuplicateStartFailClosed();
        testVbdDraftAllowsCapturePayloadValidationAndMixedWrites();
        testWorldDeviceDraftRejectsRequestBodyDeviceTypeAuthority();
        testWorldDeviceConsumerValidationSplitsInputOutputChannels();
        testExistingVbdNativeTriggerDraftRequiresNativeTriggerLock();
        testVbdProducerProjectionNodeDeleteValidatesTypedOwned();
        testSignalListenerNodeDeleteRequiresTypedIdentityAndDeletesWithLock();
        testSignalListenerActionDeleteAndReorderDraftsSaveWithLock();
    }

    private static void testLogicChainClientAssistedSelectionPurposesAreDistinct() {
        requireEquals(
                WebAdminSelectionPurpose.LOGIC_CHAIN_VBD_SELECT,
                WebAdminSelectionPurpose.parse("logic_chain_vbd_select"),
                "v8 VBD client-assisted flow keeps its own session purpose"
        );
        requireEquals(
                WebAdminSelectionPurpose.LOGIC_CHAIN_WORLD_DEVICE_PLACE,
                WebAdminSelectionPurpose.parse("logic_chain_world_device_place"),
                "v8 world device client-assisted flow uses the hotbar placement purpose"
        );
        requireEquals(
                WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT,
                WebAdminSelectionPurpose.parse("logic_chain_region_controller_select"),
                "v8 RegionController client-assisted flow uses a distinct region-controller purpose"
        );
        requireEquals(
                WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT,
                WebAdminSelectionPurpose.parse("logic_chain_region_select"),
                "legacy region select purpose remains a compatibility alias only"
        );
        requireFalse(
                WebAdminSelectionPurpose.LOGIC_CHAIN_REGION_CONTROLLER_SELECT == WebAdminSelectionPurpose.LOGIC_CHAIN_VBD_SELECT,
                "v8 RegionController selection must not fall through to the VBD single-block handler"
        );
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
        requireFalse(badResult.success(), "world entity draft creation requires protected draft ownership");
        requireValidationCode(badResult, "logic_chain_protected_draft_required");
        requireStructuredError(badResult, "logic_chain_protected_draft_required", node.id, "");

        node.id = "draft:signal_listener:bad";
        node.type = "signal_listener";
        WebAdminWriteResult listenerResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, bad, fixture.csrf, true);
        requireFalse(listenerResult.success(), "virtual SignalListener is supported but still requires name");
        requireValidationCode(listenerResult, "required");

        node.signalListener.name = "listener.bad";
        node.signalListener.displayName = "Listener Bad";
        node.signalListener.channel = "editor.validation.input";
        WebAdminWriteResult listenerSaveResult = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", bad, fixture.csrf, true);
        requireFalse(listenerSaveResult.success(), "virtual SignalListener save requires one consumes channel");
        requireValidationCode(listenerSaveResult, "logic_chain_listener_consumes_edge_required");

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
        assignActionAppendLock(appendSignalFixture, appendSignal);
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

    private static void testMalformedDraftListsRemainValidationFailures() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.malformed.nulls");

        WebAdminLogicChainEditorRequest nullNode = rootRequest("editor.malformed.nulls");
        nullNode.lockId = lockId(entered);
        nullNode.baseGraphFingerprint = fingerprint(entered);
        List<WebAdminLogicChainEditorRequest.DraftNode> nodes = new java.util.ArrayList<>();
        nodes.add(null);
        nullNode.nodes = nodes;
        WebAdminWriteResult nullNodeResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, nullNode, fixture.csrf, true);
        requireFalse(nullNodeResult.success(), "null draft node remains a structured validation failure");
        requireValidationCode(nullNodeResult, "logic_chain_draft_node_id_invalid");
        requireValidationCode(nullNodeResult, "logic_chain_node_type_deferred");

        WebAdminLogicChainEditorRequest nullMetadata = timerDraftRequest("editor.malformed.nulls", lockId(entered), fingerprint(entered), "editor.malformed.nulls.timer");
        List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> metadataDrafts = new java.util.ArrayList<>();
        metadataDrafts.add(null);
        nullMetadata.channelMetadataDrafts = metadataDrafts;
        WebAdminWriteResult nullMetadataResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, nullMetadata, fixture.csrf, true);
        requireFalse(nullMetadataResult.success(), "null channel metadata draft remains a structured validation failure");
        requireValidationField(nullMetadataResult, "required", "channelMetadataDrafts[0].channel");
    }

    private static void testDraftOperationPlannerPreservesTypedWriteOrderBoundaries() {
        WebAdminLogicChainEditorRequest request = timerDraftRequest("editor.planner", "editor-lock", "base-fingerprint", "editor.planner.timer");
        request.actionAppend = actionAppendDraftRequest(
                "editor.planner",
                "editor-lock",
                "base-fingerprint",
                "listener",
                "listener.planner",
                "",
                messageAction("append")
        ).actionAppend;
        WebAdminSignalListenerBasicConfigUpdateRequest listenerEdit = new WebAdminSignalListenerBasicConfigUpdateRequest();
        listenerEdit.listenerRef = "listener.planner";
        listenerEdit.expectedFingerprint = "listener-fingerprint";
        listenerEdit.lockId = "listener-lock";
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingEdit = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        existingEdit.nodeType = "signal_listener";
        existingEdit.targetId = "listener.planner";
        existingEdit.signalListenerBasic = listenerEdit;
        request.existingNodeEdits = List.of(existingEdit);
        request.actionEdits = actionEditDraftRequest(
                "editor.planner",
                "editor-lock",
                "base-fingerprint",
                "listener",
                "listener.planner",
                "",
                0,
                messageAction("replace")
        ).actionEdits;
        request.actionDeletes = List.of(actionDeleteDraft("timer", "timer.planner", "complete", 0, "timer-fingerprint", "timer-lock"));
        request.actionReorders = List.of(actionReorderDraft("listener", "listener.planner", "", 0, 1, "listener-actions-fingerprint", "listener-actions-lock"));
        request.nodeDeletes = List.of(nodeDeleteDraft("signal_listener", "listener.delete.planner", "delete-fingerprint", "delete-lock"));
        request.channelMetadataDrafts = List.of(channelMetadataDraft("editor.planner.channel", "Planner channel", ""));

        LogicChainDraftOperationPlanner.OperationPlan plan = LogicChainDraftOperationPlanner.plan(request);
        requireEquals(1, plan.draftNodes().size(), "planner keeps new draft nodes as the first typed phase");
        requireTrue(plan.actionAppend() != null, "planner keeps action append as the second typed phase");
        requireEquals(1, plan.existingNodeEdits().size(), "planner keeps existing node edits after append");
        requireEquals(1, plan.actionEdits().size(), "planner keeps action edits after existing node edits");
        requireEquals(1, plan.actionDeletes().size(), "planner keeps action deletes before reorders");
        requireEquals(1, plan.actionReorders().size(), "planner keeps action reorders before node deletes");
        requireEquals(1, plan.nodeDeletes().size(), "planner keeps node deletes as the last typed phase");
        requireEquals(1, plan.channelMetadataDrafts().size(), "planner keeps channel metadata as the tail boundary");
        requireTrue(plan.hasTypedStoreDrafts(), "planner reports typed writes");
        requireTrue(plan.hasNonNodeDeleteTypedStoreDrafts(), "planner reports non-node-delete typed writes");
        requireTrue(plan.hasNodeDelete(), "planner reports node delete boundary");
        requireTrue(plan.hasChannelMetadataDrafts(), "planner reports channel metadata boundary");
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

    private static void testActionAppendRejectsInvalidShapeAndAllowsMixedDraft() throws Exception {
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
        requireFalse(mixed.success(), "invalid action append remains rejected even when mixed with a new node draft");
        requireValidationCode(mixed, "logic_chain_action_append_owner_id_required");
    }

    private static void testActionAppendValidationCoversSupportedOwnersBucketsAndConditionGroup() throws Exception {
        for (String[] owner : List.of(
                new String[]{"listener", ""},
                new String[]{"action_relay", ""},
                new String[]{"region_controller", "enter"},
                new String[]{"region_controller", "exit"},
                new String[]{"region_controller", "stay"},
                new String[]{"timer", "start"},
                new String[]{"timer", "tick"},
                new String[]{"timer", "complete"},
                new String[]{"timer", "cancel"}
        )) {
            Fixture fixture = fixture();
            String bucketSuffix = owner[1].isBlank() ? "actions" : owner[1];
            String rootRef = "editor.append.validation." + owner[0] + "." + bucketSuffix;
            WebAdminWriteResult entered = enter(fixture, rootRef);
            WebAdminLogicChainEditorRequest append = actionAppendDraftRequest(rootRef, lockId(entered), fingerprint(entered), owner[0], "owner." + rootRef, owner[1], messageAction("append " + rootRef));
            assignActionAppendLock(fixture, append);
            WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, append, fixture.csrf, true);
            requireTrue(result.success(), "valid append-only action shape is accepted for " + owner[0] + "/" + owner[1]);
        }

        WebAdminActionRelayActionsUpdateRequest.ActionEntry gated = messageAction("condition gated append");
        gated.conditionGroupId = "editor_action_gate";
        requireEquals("editor_action_gate", WebAdminActionRelayActionsService.actionFromEntry(gated).conditionGroupId(), "new action conditionGroupId roundtrips through action conversion");
    }

    private static void testActionEditValidationCoversActionRelayAndRegionBuckets() throws Exception {
        for (String[] owner : List.of(
                new String[]{"action_relay", ""},
                new String[]{"region_controller", "enter"},
                new String[]{"region_controller", "exit"},
                new String[]{"region_controller", "stay"}
        )) {
            Fixture fixture = fixture();
            String bucketSuffix = owner[1].isBlank() ? "actions" : owner[1];
            String rootRef = "editor.action.edit.validation." + owner[0] + "." + bucketSuffix;
            WebAdminWriteResult entered = enter(fixture, rootRef);
            WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                    rootRef,
                    lockId(entered),
                    fingerprint(entered),
                    owner[0],
                    "owner." + rootRef,
                    owner[1],
                    0,
                    messageAction("replace " + rootRef)
            );
            assignActionEditLock(fixture, edit.actionEdits.getFirst());
            WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);
            requireTrue(result.success(), "valid same-index action edit shape is accepted for " + owner[0] + "/" + owner[1] + " errors=" + result.validationErrors());
        }
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

    private static void testSaveSignalListenerDraftWritesUnderlyingConfig() throws Exception {
        Fixture fixture = fixture();
        saveAlwaysConditionGroup(fixture);
        WebAdminWriteResult entered = enter(fixture, "editor.listener.root");
        WebAdminLogicChainEditorRequest request = signalListenerDraftRequest("editor.listener.root", lockId(entered), fingerprint(entered), "editor.listener.saved");

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);

        requireTrue(result.success(), "virtual SignalListener draft saves through lifecycle service errors=" + result.validationErrors());
        SignalListenerData listener = requireSignalListenerByName(fixture, "editor.listener.saved");
        requireEquals("editor.listener.saved", listener.name(), "SignalListener name persists");
        requireEquals("editor.listener.in", listener.channel(), "SignalListener channel is derived from consumes edge");
        requireEquals(30, listener.cooldownTicks(), "SignalListener cooldown persists");
        requireEquals("always", listener.conditionGroupId(), "SignalListener conditionGroupId persists");
        requireEquals(0, listener.actions().size(), "new SignalListener starts without actions");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor lock released after virtual SignalListener save");
    }

    private static void testTimerDraftCanCreateAndSelectDownstreamChannelEndpoint() throws Exception {
        Fixture createFixture = fixture();
        WebAdminWriteResult createEntered = enter(createFixture, "editor.timer.endpoint.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.timer.endpoint.create", lockId(createEntered), fingerprint(createEntered), "editor.timer.endpoint.create.saved");
        String draftId = create.nodes.getFirst().id;
        create.edges = List.of(edge("draft-edge-timer-new-out", draftId, "channel:editor.timer.endpoint.new", "timer_outputs_channel"));
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

        WebAdminWriteResult startResult = appendTimerActionThroughLogicChain(fixture, "editor.timer.append.start", "editor.timer.append.saved", "start", "timer start append");
        requireTrue(startResult.success(), "Logic Chain editor appends one Timer start Action");
        WebAdminWriteResult cancelResult = appendTimerActionThroughLogicChain(fixture, "editor.timer.append.cancel", "editor.timer.append.saved", "cancel", "timer cancel append");
        requireTrue(cancelResult.success(), "Logic Chain editor appends one Timer cancel Action");
        Map<?, ?> afterFourBuckets = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.append.saved");
        requireEquals(1, ((List<?>) afterFourBuckets.get("onStartActions")).size(), "Timer append supports onStart bucket");
        requireEquals("timer start append", string(((Map<?, ?>) ((List<?>) afterFourBuckets.get("onStartActions")).getFirst()).get("value")), "appended Timer start action is stored");
        requireEquals(1, ((List<?>) afterFourBuckets.get("onCancelActions")).size(), "Timer append supports onCancel bucket");
        requireEquals("timer cancel append", string(((Map<?, ?>) ((List<?>) afterFourBuckets.get("onCancelActions")).getFirst()).get("value")), "appended Timer cancel action is stored");

        WebAdminWriteResult tickRejected = appendTimerActionThroughLogicChain(fixture, "editor.timer.append.delay-tick", "editor.timer.append.saved", "tick", "timer tick append");
        requireFalse(tickRejected.success(), "Logic Chain editor rejects Timer tick append for DELAY mode");
        requireValidationCode(tickRejected, "logic_chain_timer_delay_tick_action_not_supported");
    }

    private static WebAdminWriteResult appendTimerActionThroughLogicChain(
            Fixture fixture,
            String rootRef,
            String timerId,
            String bucket,
            String message
    ) throws Exception {
        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, timerId);
        String timerLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId);
        WebAdminWriteResult entered = enter(fixture, rootRef);
        WebAdminLogicChainEditorRequest append = rootRequest(rootRef);
        append.lockId = lockId(entered);
        append.baseGraphFingerprint = fingerprint(entered);
        append.nodes = List.of();
        append.edges = List.of();
        append.actionAppend = new WebAdminLogicChainEditorRequest.ActionAppendDraft();
        append.actionAppend.ownerType = "timer";
        append.actionAppend.ownerId = timerId;
        append.actionAppend.bucket = bucket;
        append.actionAppend.action = messageAction(message);
        append.actionAppend.expectedFingerprint = string(before.get("expectedFingerprint"));
        append.actionAppend.lockId = timerLockId;
        return fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", append, fixture.csrf, true);
    }

    private static void testExistingTimerNodeEditWritesUnderlyingConfig() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.existing.timer.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.existing.timer.create", lockId(createdEditor), fingerprint(createdEditor), "editor.existing.timer.saved");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("timer unchanged action"));
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture Timer exists before existing-node edit");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.existing.timer.saved");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.existing.timer.saved");
        WebAdminWriteResult entered = enter(fixture, "timer", "editor.existing.timer.saved");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.existing.timer.saved");
        edit.rootType = "timer";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "timer";
        draft.targetId = "editor.existing.timer.saved";
        WebAdminTimerRequest timer = new WebAdminTimerRequest();
        timer.id = "editor.existing.timer.saved";
        timer.displayName = "Edited Existing Timer";
        timer.note = "8.16 existing node edit";
        timer.enabled = true;
        timer.mode = "DELAY";
        timer.scopeMode = "GLOBAL";
        timer.durationTicks = 80L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = "RESTART";
        timer.outputChannel = "editor.existing.timer.out";
        timer.onCompleteActions = List.of(messageAction("malicious node edit replacement"), messageAction("malicious node edit extra"));
        timer.expectedFingerprint = string(before.get("expectedFingerprint"));
        timer.lockId = typedLockId;
        draft.timer = timer;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult validation = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);
        requireTrue(validation.success(), "existing Timer node edit validates with typed lock and fingerprint");
        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);
        requireTrue(saved.success(), "existing Timer node edit writes through Timer service");
        Map<?, ?> after = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.existing.timer.saved");
        requireEquals("Edited Existing Timer", string(after.get("displayName")), "Timer displayName updated by existing-node edit");
        requireEquals("editor.existing.timer.out", string(after.get("outputChannel")), "Timer outputChannel local reconnect is saved as typed field");
        List<?> actions = (List<?>) after.get("onCompleteActions");
        requireEquals(1, actions.size(), "existing Timer edit preserves action list length");
        requireEquals("timer unchanged action", string(((Map<?, ?>) actions.getFirst()).get("value")), "existing Timer edit does not reorder or delete action");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and timer typed locks released after existing Timer edit");
    }

    private static void testExistingChannelMetadataEditValidatesTypedPayload() throws Exception {
        Fixture fixture = fixture();
        String channel = "editor.existing.channel";
        WebAdminDtos.ChannelMetadataDto before = fixture.channelMetadataService.metadataFor(null, fixture.editor, fixture.session, channel, "signal");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_CHANNEL_METADATA, channel);
        WebAdminWriteResult entered = enter(fixture, "channel", channel);
        WebAdminLogicChainEditorRequest edit = rootRequest(channel);
        edit.rootType = "channel";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();

        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "channel_metadata";
        draft.targetId = channel;
        WebAdminChannelMetadataUpdateRequest metadata = new WebAdminChannelMetadataUpdateRequest();
        metadata.channel = channel;
        metadata.displayName = "Edited Channel";
        metadata.note = "8.16 channel metadata edit";
        metadata.iconKey = "auto";
        metadata.expectedFingerprint = before.expectedFingerprint();
        metadata.lockId = typedLockId;
        draft.channelMetadata = metadata;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult validation = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);

        requireTrue(validation.success(), "existing Channel metadata edit validates as a typed payload on the current graph errors=" + validation.validationErrors());
    }

    private static void testExistingChannelMetadataEditSavesUnderlyingMetadata() throws Exception {
        Fixture fixture = fixture();
        String channel = "editor.existing.channel.save";
        WebAdminDtos.ChannelMetadataDto before = fixture.channelMetadataService.metadataFor(null, fixture.editor, fixture.session, channel, "signal");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_CHANNEL_METADATA, channel);
        WebAdminWriteResult entered = enter(fixture, "channel", channel);
        WebAdminLogicChainEditorRequest edit = rootRequest(channel);
        edit.rootType = "channel";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();

        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "channel_metadata";
        draft.targetId = channel;
        WebAdminChannelMetadataUpdateRequest metadata = new WebAdminChannelMetadataUpdateRequest();
        metadata.channel = channel;
        metadata.displayName = "Edited Channel Save";
        metadata.note = "8.16 channel metadata save roundtrip";
        metadata.iconKey = "timer";
        metadata.expectedFingerprint = before.expectedFingerprint();
        metadata.lockId = typedLockId;
        draft.channelMetadata = metadata;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(saved.success(), "existing Channel metadata edit saves through metadata service errors=" + saved.validationErrors());
        WebAdminDtos.ChannelMetadataDto after = fixture.channelMetadataService.metadataFor(null, fixture.editor, fixture.session, channel, "signal");
        requireEquals("Edited Channel Save", after.displayName(), "Channel metadata displayName persists");
        requireEquals("8.16 channel metadata save roundtrip", after.note(), "Channel metadata note persists");
        requireEquals("timer", after.iconKey(), "Channel metadata icon persists");
        requireEquals(1L, after.version(), "Channel metadata version increments on save");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and channel metadata typed locks released after save");
    }

    private static void testExistingSignalListenerBasicEditWritesUnderlyingConfig() throws Exception {
        Fixture fixture = fixture();
        saveAlwaysConditionGroup(fixture);
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                "listener.existing.basic",
                "listener.existing.in",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("listener action unchanged")))
        );
        WebAdminDtos.SignalListenerBasicConfigDto before = fixture.signalListenerBasicConfigService.configFor(null, fixture.editor, fixture.session, listener.id());
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());
        WebAdminLogicChainEditorRequest edit = rootRequest(listener.id());
        edit.rootType = "listener";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();

        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "signal_listener";
        draft.targetId = listener.id();
        WebAdminSignalListenerBasicConfigUpdateRequest basic = new WebAdminSignalListenerBasicConfigUpdateRequest();
        basic.listenerRef = listener.id();
        basic.enabled = Boolean.FALSE;
        basic.channel = "listener.existing.edited";
        basic.cooldownTicks = 35;
        basic.conditionGroupId = "always";
        basic.expectedFingerprint = before.expectedFingerprint();
        basic.lockId = typedLockId;
        draft.signalListenerBasic = basic;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(saved.success(), "existing SignalListener basic edit saves through listener service errors=" + saved.validationErrors());
        SignalListenerData after = requireSignalListener(fixture, listener.id());
        requireEquals(Boolean.FALSE, after.enabled(), "SignalListener enabled field persists");
        requireEquals("listener.existing.edited", after.channel(), "SignalListener channel local reconnect persists");
        requireEquals(35, after.cooldownTicks(), "SignalListener cooldown persists");
        requireEquals("always", after.conditionGroupId(), "SignalListener conditionGroupId persists");
        requireEquals(1, after.actions().size(), "SignalListener basic edit preserves action list");
        requireEquals("listener action unchanged", after.actions().getFirst().value(), "SignalListener basic edit does not replace action content");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and listener basic typed locks released after save");
    }

    private static void testExistingSignalJoinEditRejectsInputOutputOverlap() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "signal_join", "editor.existing.join.validation");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.existing.join.validation");
        edit.rootType = "signal_join";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "signal_join";
        draft.targetId = "editor.existing.join.validation";
        WebAdminSignalJoinRequest join = new WebAdminSignalJoinRequest();
        join.id = "editor.existing.join.validation";
        join.displayName = "Existing Join";
        join.mode = "ALL";
        join.threshold = 2;
        join.inputChannels = List.of(
                new SignalJoinInputDefinition("editor.existing.join.shared", "", "", 1),
                new SignalJoinInputDefinition("editor.existing.join.second", "", "", 1)
        );
        join.outputChannel = "editor.existing.join.shared";
        join.expectedFingerprint = "typed-fingerprint";
        join.lockId = "typed-lock";
        draft.signalJoin = join;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);
        requireFalse(result.success(), "existing Signal Join edit rejects input/output overlap");
        requireValidationCode(result, "logic_chain_join_input_output_channel_conflict");
    }

    private static void testExistingNodeEditRejectsTargetOutsideCurrentGraph() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.existing.graph.scope");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.existing.graph.scope");
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "timer";
        draft.targetId = "editor.existing.graph.outside";
        WebAdminTimerRequest timer = new WebAdminTimerRequest();
        timer.id = "editor.existing.graph.outside";
        timer.displayName = "Outside Timer";
        timer.mode = "DELAY";
        timer.scopeMode = "GLOBAL";
        timer.durationTicks = 40L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = "RESTART";
        timer.outputChannel = "editor.existing.graph.out";
        timer.expectedFingerprint = "typed-fingerprint";
        timer.lockId = "typed-lock";
        draft.timer = timer;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);

        requireFalse(result.success(), "existing node edit rejects target outside current graph");
        requireValidationCode(result, "logic_chain_existing_node_not_in_graph");
    }

    private static void testExistingNodeEditAllowsReferencedChannelMetadataDrafts() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.existing.metadata.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.existing.metadata.create", lockId(createdEditor), fingerprint(createdEditor), "editor.existing.metadata.timer");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("metadata timer action"));
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture Timer exists before metadata draft mixed typed edit");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.existing.metadata.timer");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.existing.metadata.timer");
        WebAdminWriteResult entered = enter(fixture, "timer", "editor.existing.metadata.timer");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.existing.metadata.timer");
        edit.rootType = "timer";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        edit.channelMetadataDrafts = List.of(channelMetadataDraft("editor.existing.metadata.out", "Existing Timer Output", "由已有 Timer 重连引用"));
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "timer";
        draft.targetId = "editor.existing.metadata.timer";
        WebAdminTimerRequest timer = new WebAdminTimerRequest();
        timer.id = "editor.existing.metadata.timer";
        timer.displayName = "Metadata Piggyback Timer";
        timer.mode = "DELAY";
        timer.scopeMode = "GLOBAL";
        timer.durationTicks = 40L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = "RESTART";
        timer.outputChannel = "editor.existing.metadata.out";
        timer.expectedFingerprint = string(before.get("expectedFingerprint"));
        timer.lockId = typedLockId;
        draft.timer = timer;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult validation = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);
        requireTrue(validation.success(), "existing typed edit may validate referenced channel metadata before save-time transaction boundary check");
        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireFalse(result.success(), "existing typed edit and channel metadata mixed save fails closed until cross-store transaction exists");
        requireValidationCode(result, "logic_chain_mixed_metadata_typed_write_fail_closed");
    }

    private static void testExistingTypedEditsRejectDraftEdgesAndAllowMultipleTargets() throws Exception {
        Fixture edgeFixture = fixture();
        WebAdminWriteResult edgeEntered = enter(edgeFixture, "timer", "editor.existing.edge.timer");
        WebAdminLogicChainEditorRequest edgeEdit = rootRequest("editor.existing.edge.timer");
        edgeEdit.rootType = "timer";
        edgeEdit.lockId = lockId(edgeEntered);
        edgeEdit.baseGraphFingerprint = fingerprint(edgeEntered);
        edgeEdit.nodes = List.of();
        edgeEdit.edges = List.of(edge("forged-edge", "timer:editor.existing.edge.timer", "channel:forged", "timer_outputs_channel"));
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existing = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        existing.nodeType = "timer";
        existing.targetId = "editor.existing.edge.timer";
        WebAdminTimerRequest timer = new WebAdminTimerRequest();
        timer.id = "editor.existing.edge.timer";
        timer.displayName = "Forged Edge Timer";
        timer.mode = "DELAY";
        timer.scopeMode = "GLOBAL";
        timer.durationTicks = 40L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = "RESTART";
        timer.outputChannel = "editor.existing.edge.out";
        timer.expectedFingerprint = "typed-fingerprint";
        timer.lockId = "typed-lock";
        existing.timer = timer;
        edgeEdit.existingNodeEdits = List.of(existing);

        WebAdminWriteResult edgeResult = edgeFixture.service.validateDraft(null, edgeFixture.editor, edgeFixture.session, edgeEdit, edgeFixture.csrf, true);
        requireFalse(edgeResult.success(), "existing typed edit rejects draft edges");
        requireValidationCode(edgeResult, "logic_chain_existing_edit_edges_not_allowed");

        Fixture multipleFixture = fixture();
        WebAdminWriteResult multipleEntered = enter(multipleFixture, "editor.existing.multiple");
        WebAdminLogicChainEditorRequest multipleExisting = rootRequest("editor.existing.multiple");
        multipleExisting.lockId = lockId(multipleEntered);
        multipleExisting.baseGraphFingerprint = fingerprint(multipleEntered);
        multipleExisting.nodes = List.of();
        multipleExisting.edges = List.of();
        multipleExisting.existingNodeEdits = List.of(existing, existing);
        WebAdminWriteResult multipleExistingResult = multipleFixture.service.validateDraft(null, multipleFixture.editor, multipleFixture.session, multipleExisting, multipleFixture.csrf, true);
        requireFalse(multipleExistingResult.success(), "duplicate existing-node edits for the same target are rejected");
        requireValidationCode(multipleExistingResult, "logic_chain_existing_node_duplicate_edit");

        Fixture multipleActionFixture = fixture();
        WebAdminWriteResult actionEntered = enter(multipleActionFixture, "editor.action.multiple");
        WebAdminLogicChainEditorRequest actionEdit = actionEditDraftRequest(
                "editor.action.multiple",
                lockId(actionEntered),
                fingerprint(actionEntered),
                "listener",
                "listener.action.multiple",
                "",
                0,
                messageAction("blocked")
        );
        actionEdit.actionEdits = List.of(actionEdit.actionEdits.getFirst(), actionEdit.actionEdits.getFirst());
        WebAdminWriteResult multipleActionResult = multipleActionFixture.service.validateDraft(null, multipleActionFixture.editor, multipleActionFixture.session, actionEdit, multipleActionFixture.csrf, true);
        requireFalse(multipleActionResult.success(), "duplicate action edits for the same target/index are rejected");
        requireValidationCode(multipleActionResult, "logic_chain_action_duplicate_edit");

        Fixture multiSaveFixture = fixture();
        WebAdminWriteResult createEntered = enter(multiSaveFixture, "editor.join.in.a");
        WebAdminLogicChainEditorRequest create = signalJoinDraftRequest("editor.join.in.a", lockId(createEntered), fingerprint(createEntered), "editor.multiple.existing.join");
        WebAdminWriteResult created = multiSaveFixture.service.saveDraft(null, multiSaveFixture.editor, multiSaveFixture.session, "127.0.0.1", create, multiSaveFixture.csrf, true);
        requireTrue(created.success(), "fixture Join exists before multiple existing-node edits");

        WebAdminWriteResult multiEntered = enter(multiSaveFixture, "editor.join.in.a");
        WebAdminLogicChainEditorRequest multiEdit = rootRequest("editor.join.in.a");
        multiEdit.lockId = lockId(multiEntered);
        multiEdit.baseGraphFingerprint = fingerprint(multiEntered);
        multiEdit.nodes = List.of();
        multiEdit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft inChannel = channelMetadataExistingEdit(
                multiSaveFixture,
                "editor.join.in.a",
                "Multiple Input Channel"
        );
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft outChannel = channelMetadataExistingEdit(
                multiSaveFixture,
                "editor.join.out",
                "Multiple Output Channel"
        );
        multiEdit.existingNodeEdits = List.of(inChannel, outChannel);
        WebAdminWriteResult multiSaved = multiSaveFixture.service.saveDraft(null, multiSaveFixture.editor, multiSaveFixture.session, "127.0.0.1", multiEdit, multiSaveFixture.csrf, true);
        requireTrue(multiSaved.success(), "multiple existing-node edits for different targets save together");
        requireEquals("Multiple Input Channel", multiSaveFixture.channelMetadataService.metadataFor(null, multiSaveFixture.editor, multiSaveFixture.session, "editor.join.in.a", "signal").displayName(), "first existing channel metadata edit saved");
        requireEquals("Multiple Output Channel", multiSaveFixture.channelMetadataService.metadataFor(null, multiSaveFixture.editor, multiSaveFixture.session, "editor.join.out", "signal").displayName(), "second existing channel metadata edit saved");
    }

    private static void testMultiEditValidationFieldsUseActualIndexes() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.multi.index");

        WebAdminLogicChainEditorRequest nodeEdit = rootRequest("editor.multi.index");
        nodeEdit.lockId = lockId(entered);
        nodeEdit.baseGraphFingerprint = fingerprint(entered);
        nodeEdit.nodes = List.of();
        nodeEdit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft firstNode = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        firstNode.nodeType = "unknown_existing_node";
        firstNode.targetId = "unknown.one";
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft secondNode = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        secondNode.nodeType = "reference_only_node";
        secondNode.targetId = "reference.two";
        nodeEdit.existingNodeEdits = List.of(firstNode, secondNode);

        WebAdminWriteResult nodeResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, nodeEdit, fixture.csrf, true);
        requireFalse(nodeResult.success(), "unsupported multi existing-node edits fail validation");
        requireValidationField(nodeResult, "logic_chain_existing_node_type_deferred", "existingNodeEdits[0].nodeType");
        requireValidationField(nodeResult, "logic_chain_existing_node_type_deferred", "existingNodeEdits[1].nodeType");

        WebAdminLogicChainEditorRequest actionEdit = rootRequest("editor.multi.index");
        actionEdit.lockId = lockId(entered);
        actionEdit.baseGraphFingerprint = fingerprint(entered);
        actionEdit.nodes = List.of();
        actionEdit.edges = List.of();
        WebAdminLogicChainEditorRequest.ActionEditDraft firstAction = new WebAdminLogicChainEditorRequest.ActionEditDraft();
        firstAction.ownerType = "receiver";
        firstAction.ownerId = "relay.one";
        firstAction.actionIndex = 0;
        firstAction.operation = "replace";
        firstAction.action = messageAction("first");
        firstAction.expectedFingerprint = "typed-owner-fingerprint";
        firstAction.lockId = "typed-owner-lock";
        WebAdminLogicChainEditorRequest.ActionEditDraft secondAction = new WebAdminLogicChainEditorRequest.ActionEditDraft();
        secondAction.ownerType = "unknown_action_owner";
        secondAction.ownerId = "region.two";
        secondAction.actionIndex = 0;
        secondAction.operation = "replace";
        secondAction.action = messageAction("second");
        secondAction.expectedFingerprint = "typed-owner-fingerprint";
        secondAction.lockId = "typed-owner-lock";
        actionEdit.actionEdits = List.of(firstAction, secondAction);

        WebAdminWriteResult actionResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, actionEdit, fixture.csrf, true);
        requireFalse(actionResult.success(), "unsupported multi action edits fail validation");
        requireValidationField(actionResult, "logic_chain_action_edit_owner_type_deferred", "actionEdits[0].ownerType");
        requireValidationField(actionResult, "logic_chain_action_edit_owner_type_deferred", "actionEdits[1].ownerType");
    }

    private static void testMultiDraftSessionSavesNewNodeExistingEditAndMetadata() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.join.in.a");
        WebAdminLogicChainEditorRequest request = signalJoinDraftRequest("editor.join.in.a", lockId(entered), fingerprint(entered), "editor.multi.session.join");
        request.channelMetadataDrafts = List.of(channelMetadataDraft("editor.join.out", "Multi Session Output", "由新增 Join 输出引用"));
        request.existingNodeEdits = List.of(channelMetadataExistingEdit(fixture, "editor.join.in.a", "Multi Session Input"));

        WebAdminWriteResult validation = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(validation.success(), "multi draft session validates referenced metadata before save-time transaction boundary check");
        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);

        requireFalse(saved.success(), "multi draft session with typed stores and channel metadata fails closed until cross-store transaction exists");
        requireValidationCode(saved, "logic_chain_mixed_metadata_typed_write_fail_closed");
    }

    private static void testMultiActionEditsSaveAcrossOwners() throws Exception {
        Fixture fixture = fixture();
        SignalListenerData first = saveSignalListenerFixture(
                fixture,
                "listener.multi.action.first",
                "editor.multi.action.channel",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("first old action")))
        );
        SignalListenerData second = saveSignalListenerFixture(
                fixture,
                "listener.multi.action.second",
                "editor.multi.action.channel",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("second old action")))
        );
        Map<String, Object> firstBefore = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, first.id());
        Map<String, Object> secondBefore = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, second.id());
        String firstLock = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, first.id());
        String secondLock = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, second.id());
        WebAdminWriteResult entered = enter(fixture, "editor.multi.action.channel");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.multi.action.channel");
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ActionEditDraft firstEdit = actionEditDraftRequest("editor.multi.action.channel", lockId(entered), fingerprint(entered), "listener", first.id(), "", 0, messageAction("first replaced action")).actionEdits.getFirst();
        firstEdit.expectedFingerprint = string(firstBefore.get("expectedFingerprint"));
        firstEdit.lockId = firstLock;
        WebAdminLogicChainEditorRequest.ActionEditDraft secondEdit = actionEditDraftRequest("editor.multi.action.channel", lockId(entered), fingerprint(entered), "listener", second.id(), "", 0, messageAction("second replaced action")).actionEdits.getFirst();
        secondEdit.expectedFingerprint = string(secondBefore.get("expectedFingerprint"));
        secondEdit.lockId = secondLock;
        edit.actionEdits = List.of(firstEdit, secondEdit);

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(saved.success(), "multiple action edits for different owners save together");
        requireEquals("first replaced action", requireSignalListener(fixture, first.id()).actions().getFirst().value(), "first owner action edit saved");
        requireEquals("second replaced action", requireSignalListener(fixture, second.id()).actions().getFirst().value(), "second owner action edit saved");
    }

    private static void testTimerExistingNodeEditCannotMutateActionList() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.existing.timer.action-guard.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.existing.timer.action-guard.create", lockId(createdEditor), fingerprint(createdEditor), "editor.existing.timer.action-guard.saved");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("protected old action"));
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture Timer exists before action-list guard test");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.existing.timer.action-guard.saved");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.existing.timer.action-guard.saved");
        WebAdminWriteResult entered = enter(fixture, "timer", "editor.existing.timer.action-guard.saved");
        WebAdminLogicChainEditorRequest edit = rootRequest("editor.existing.timer.action-guard.saved");
        edit.rootType = "timer";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "timer";
        draft.targetId = "editor.existing.timer.action-guard.saved";
        WebAdminTimerRequest timer = new WebAdminTimerRequest();
        timer.id = "editor.existing.timer.action-guard.saved";
        timer.displayName = "Timer Action Guard Edited";
        timer.mode = "DELAY";
        timer.scopeMode = "GLOBAL";
        timer.durationTicks = 100L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = "RESTART";
        timer.outputChannel = "editor.existing.timer.action-guard.out";
        timer.onCompleteActions = List.of(messageAction("forged replacement"), messageAction("forged extra"));
        timer.expectedFingerprint = string(before.get("expectedFingerprint"));
        timer.lockId = typedLockId;
        draft.timer = timer;
        edit.existingNodeEdits = List.of(draft);

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(saved.success(), "existing Timer node edit saves while preserving old action list");
        Map<?, ?> after = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.existing.timer.action-guard.saved");
        List<?> actions = (List<?>) after.get("onCompleteActions");
        requireEquals(1, actions.size(), "existing Timer node edit cannot add or delete old actions");
        requireEquals("protected old action", string(((Map<?, ?>) actions.getFirst()).get("value")), "existing Timer node edit cannot replace old action content");
    }

    private static void testTimerSameIndexActionEditReplacesWithoutReorder() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.timer.action.edit.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.timer.action.edit.create", lockId(createdEditor), fingerprint(createdEditor), "editor.timer.action.edit.saved");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("first action"), messageAction("second action"));
        create.edges = List.of();
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture Timer exists before action edit");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.action.edit.saved");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.timer.action.edit.saved");
        WebAdminWriteResult entered = enter(fixture, "timer", "editor.timer.action.edit.saved");
        WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                "editor.timer.action.edit.saved",
                lockId(entered),
                fingerprint(entered),
                "timer",
                "editor.timer.action.edit.saved",
                "complete",
                0,
                messageAction("replaced first action")
        );
        edit.rootType = "timer";
        edit.actionEdits.getFirst().expectedFingerprint = string(before.get("expectedFingerprint"));
        edit.actionEdits.getFirst().lockId = typedLockId;

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);
        requireTrue(result.success(), "Timer same-index action edit saves through Timer action bucket");
        Map<?, ?> after = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.action.edit.saved");
        List<?> actions = (List<?>) after.get("onCompleteActions");
        requireEquals(2, actions.size(), "same-index action edit preserves Timer action count");
        requireEquals("replaced first action", string(((Map<?, ?>) actions.get(0)).get("value")), "same-index action edit replaces target action");
        requireEquals("second action", string(((Map<?, ?>) actions.get(1)).get("value")), "same-index action edit preserves following action order");
    }

    private static void testSignalListenerSameIndexActionEditReplacesWithoutReorder() throws Exception {
        Fixture fixture = fixture();
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                "listener.action.edit.saved",
                "listener.action.edit.in",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("listener first action")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("listener second action"))
                )
        );
        Map<String, Object> before = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, listener.id());
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());
        WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                listener.id(),
                lockId(entered),
                fingerprint(entered),
                "listener",
                listener.id(),
                "",
                0,
                messageAction("listener replaced first action")
        );
        edit.rootType = "listener";
        edit.actionEdits.getFirst().expectedFingerprint = string(before.get("expectedFingerprint"));
        edit.actionEdits.getFirst().lockId = typedLockId;

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(result.success(), "SignalListener same-index action edit saves through listener action service errors=" + result.validationErrors());
        SignalListenerData after = requireSignalListener(fixture, listener.id());
        requireEquals(2, after.actions().size(), "same-index listener action edit preserves action count");
        requireEquals("listener replaced first action", after.actions().get(0).value(), "same-index listener action edit replaces target action");
        requireEquals("listener second action", after.actions().get(1).value(), "same-index listener action edit preserves following action order");
        requireEquals(0, fixture.editLockService.activeLockCount(), "editor and listener action typed locks released after save");
    }

    private static void testExistingActionEditStructuredPayloadConversion() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry state = stateVariableAction();
        var convertedState = WebAdminActionRelayActionsService.actionFromEntry(state);
        requireEquals("state_variable", convertedState.type().id(), "existing action edit payload supports state_variable type");
        requireEquals("mission.count", convertedState.stateKey(), "existing action edit payload preserves state key");
        requireEquals("set_variable", convertedState.stateOperation(), "existing action edit payload preserves state operation");

        WebAdminActionRelayActionsUpdateRequest.ActionEntry timer = timerStartAction("editor.structured.timer");
        var convertedTimer = WebAdminActionRelayActionsService.actionFromEntry(timer);
        requireEquals("timer_start", convertedTimer.type().id(), "existing action edit payload supports timer_start type");
        requireEquals("editor.structured.timer", convertedTimer.timerId(), "existing action edit payload preserves timerId");
        requireEquals("RESTART", convertedTimer.timerStartPolicyOverride(), "existing action edit payload normalizes Timer start policy");
    }

    private static void testExistingActionEditStructuredSaveRoundtrip() throws Exception {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry state = stateVariableAction();
        state.conditionGroupId = "always";
        ActionConfig savedState = saveSingleListenerActionEditRoundtrip("listener.structured.state", state);
        requireEquals("state_variable", savedState.type().id(), "Logic Chain actionEdits save roundtrip preserves state_variable type");
        requireEquals("mission.count", savedState.stateKey(), "Logic Chain actionEdits save roundtrip preserves state key");
        requireEquals("set_variable", savedState.stateOperation(), "Logic Chain actionEdits save roundtrip preserves state operation");
        requireEquals("always", savedState.conditionGroupId(), "Logic Chain actionEdits save roundtrip preserves action conditionGroupId");

        WebAdminActionRelayActionsUpdateRequest.ActionEntry timerStart = timerStartAction("editor.structured.timer.start");
        timerStart.conditionGroupId = "always";
        ActionConfig savedStart = saveSingleListenerActionEditRoundtrip("listener.structured.timer.start", timerStart);
        requireEquals("timer_start", savedStart.type().id(), "Logic Chain actionEdits save roundtrip preserves timer_start type");
        requireEquals("editor.structured.timer.start", savedStart.timerId(), "Logic Chain actionEdits save roundtrip preserves timer_start timerId");
        requireEquals("RESTART", savedStart.timerStartPolicyOverride(), "Logic Chain actionEdits save roundtrip preserves timer start policy");
        requireEquals("always", savedStart.conditionGroupId(), "Logic Chain actionEdits timer_start preserves conditionGroupId");

        WebAdminActionRelayActionsUpdateRequest.ActionEntry timerCancel = timerCancelAction("editor.structured.timer.cancel");
        timerCancel.conditionGroupId = "always";
        ActionConfig savedCancel = saveSingleListenerActionEditRoundtrip("listener.structured.timer.cancel", timerCancel);
        requireEquals("timer_cancel", savedCancel.type().id(), "Logic Chain actionEdits save roundtrip preserves timer_cancel type");
        requireEquals("editor.structured.timer.cancel", savedCancel.timerId(), "Logic Chain actionEdits save roundtrip preserves timer_cancel timerId");
        requireEquals("fail", savedCancel.timerMissingBehavior(), "Logic Chain actionEdits save roundtrip preserves timer_cancel missing behavior");
        requireEquals("always", savedCancel.conditionGroupId(), "Logic Chain actionEdits timer_cancel preserves conditionGroupId");
    }

    private static void testActionDisableCoercedServerSide() throws Exception {
        Fixture fixture = fixture();
        WebAdminWriteResult createdEditor = enter(fixture, "editor.timer.action.disable.create");
        WebAdminLogicChainEditorRequest create = timerDraftRequest("editor.timer.action.disable.create", lockId(createdEditor), fingerprint(createdEditor), "editor.timer.action.disable.saved");
        create.nodes.getFirst().timer.onCompleteActions = List.of(messageAction("disable me"));
        create.edges = List.of();
        WebAdminWriteResult created = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture Timer exists before disable action edit");

        Map<?, ?> before = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.action.disable.saved");
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_TIMER_CONFIG, "editor.timer.action.disable.saved");
        WebAdminWriteResult entered = enter(fixture, "timer", "editor.timer.action.disable.saved");
        WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                "editor.timer.action.disable.saved",
                lockId(entered),
                fingerprint(entered),
                "timer",
                "editor.timer.action.disable.saved",
                "complete",
                0,
                messageAction("disable me")
        );
        edit.rootType = "timer";
        edit.actionEdits.getFirst().operation = "disable";
        edit.actionEdits.getFirst().action.enabled = Boolean.TRUE;
        edit.actionEdits.getFirst().expectedFingerprint = string(before.get("expectedFingerprint"));
        edit.actionEdits.getFirst().lockId = typedLockId;

        WebAdminWriteResult result = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);

        requireTrue(result.success(), "Action disable edit saves through backend coercion");
        Map<?, ?> after = fixture.timerService.detail(null, fixture.editor, fixture.session, "editor.timer.action.disable.saved");
        List<?> actions = (List<?>) after.get("onCompleteActions");
        requireEquals(Boolean.FALSE, ((Map<?, ?>) actions.getFirst()).get("enabled"), "operation=disable forces enabled=false even if payload sent true");
    }

    private static void testActionEditRejectsDeleteAndReorderOperations() throws Exception {
        for (String operation : List.of("delete", "reorder")) {
            Fixture fixture = fixture();
            WebAdminWriteResult entered = enter(fixture, "editor.action.operation." + operation);
            WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                    "editor.action.operation." + operation,
                    lockId(entered),
                    fingerprint(entered),
                    "listener",
                    "listener.action.operation." + operation,
                    "",
                    0,
                    messageAction("blocked")
            );
            edit.actionEdits.getFirst().operation = operation;
            edit.actionEdits.getFirst().expectedFingerprint = "typed-fingerprint";
            edit.actionEdits.getFirst().lockId = "typed-lock";

            WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);
            requireFalse(result.success(), "old action " + operation + " operation is rejected");
            requireValidationCode(result, "logic_chain_action_edit_operation_invalid");
        }
    }

    private static void testMultipleDraftOperationsValidateConflictsAndAllowNonConflictingDeletesReorders() throws Exception {
        Fixture fixture = fixture();
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                "listener.multi.boundary",
                "editor.multi.boundary",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("old action")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("second action"))
                )
        );
        Map<String, Object> before = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, listener.id());
        String actionLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());

        WebAdminLogicChainEditorRequest conflict = rootRequest(listener.id());
        conflict.rootType = "listener";
        conflict.lockId = lockId(entered);
        conflict.baseGraphFingerprint = fingerprint(entered);
        conflict.nodes = List.of();
        conflict.edges = List.of();
        WebAdminLogicChainEditorRequest.ActionDeleteDraft delete = actionDeleteDraft("listener", listener.id(), "", 0, string(before.get("expectedFingerprint")), actionLockId);
        conflict.actionDeletes = List.of(delete);
        WebAdminLogicChainEditorRequest.ActionEditDraft edit = actionEditDraftRequest(listener.id(), lockId(entered), fingerprint(entered), "listener", listener.id(), "", 0, messageAction("replacement")).actionEdits.getFirst();
        edit.expectedFingerprint = string(before.get("expectedFingerprint"));
        edit.lockId = actionLockId;
        conflict.actionEdits = List.of(edit);

        WebAdminWriteResult conflictResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, conflict, fixture.csrf, true);
        requireFalse(conflictResult.success(), "same action index cannot mix edit and delete/reorder");
        requireValidationCode(conflictResult, "logic_chain_action_edit_delete_reorder_conflict");
        requireValidationCode(conflictResult, "logic_chain_action_target_multi_write_conflict");

        WebAdminLogicChainEditorRequest appendConflict = rootRequest(listener.id());
        appendConflict.rootType = "listener";
        appendConflict.lockId = lockId(entered);
        appendConflict.baseGraphFingerprint = fingerprint(entered);
        appendConflict.nodes = List.of();
        appendConflict.edges = List.of();
        appendConflict.actionAppend = new WebAdminLogicChainEditorRequest.ActionAppendDraft();
        appendConflict.actionAppend.ownerType = "listener";
        appendConflict.actionAppend.ownerId = listener.id();
        appendConflict.actionAppend.bucket = "";
        appendConflict.actionAppend.action = messageAction("append then conflict");
        appendConflict.actionAppend.expectedFingerprint = string(before.get("expectedFingerprint"));
        appendConflict.actionAppend.lockId = actionLockId;
        appendConflict.actionDeletes = List.of(actionDeleteDraft("listener", listener.id(), "", 1, string(before.get("expectedFingerprint")), actionLockId));

        WebAdminWriteResult appendConflictResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, appendConflict, fixture.csrf, true);
        requireFalse(appendConflictResult.success(), "same action list cannot mix append with delete/reorder/edit");
        requireValidationCode(appendConflictResult, "logic_chain_action_target_multi_write_conflict");

        Fixture saveFixture = fixture();
        SignalListenerData deleteOwner = saveSignalListenerFixture(
                saveFixture,
                "listener.multi.delete",
                "editor.multi.boundary.save",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("delete me")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("keep me"))
                )
        );
        SignalListenerData reorderOwner = saveSignalListenerFixture(
                saveFixture,
                "listener.multi.reorder",
                "editor.multi.boundary.save",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("move me")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("front"))
                )
        );
        Map<String, Object> deleteBefore = saveFixture.signalListenerActionsService.actionsFor(null, saveFixture.editor, saveFixture.session, deleteOwner.id());
        Map<String, Object> reorderBefore = saveFixture.signalListenerActionsService.actionsFor(null, saveFixture.editor, saveFixture.session, reorderOwner.id());
        String deleteLock = acquireLock(saveFixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, deleteOwner.id());
        String reorderLock = acquireLock(saveFixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, reorderOwner.id());
        WebAdminWriteResult saveEntered = enter(saveFixture, "editor.multi.boundary.save");
        WebAdminLogicChainEditorRequest multiple = rootRequest("editor.multi.boundary.save");
        multiple.lockId = lockId(saveEntered);
        multiple.baseGraphFingerprint = fingerprint(saveEntered);
        multiple.nodes = List.of();
        multiple.edges = List.of();
        multiple.actionDeletes = List.of(actionDeleteDraft("listener", deleteOwner.id(), "", 0, string(deleteBefore.get("expectedFingerprint")), deleteLock));
        multiple.actionReorders = List.of(actionReorderDraft("listener", reorderOwner.id(), "", 0, 1, string(reorderBefore.get("expectedFingerprint")), reorderLock));

        WebAdminWriteResult multipleResult = saveFixture.service.saveDraft(null, saveFixture.editor, saveFixture.session, "127.0.0.1", multiple, saveFixture.csrf, true);
        requireTrue(multipleResult.success(), "non-conflicting action delete/reorder drafts for different action lists save together errors=" + multipleResult.validationErrors());
        requireEquals(1, requireSignalListener(saveFixture, deleteOwner.id()).actions().size(), "delete owner has one action after draft save");
        requireEquals("front", requireSignalListener(saveFixture, reorderOwner.id()).actions().getFirst().value(), "reorder owner action moved to front");
    }

    private static void testTargetLockPreflightReportsMissingTargetLockBeforeTypedSave() throws Exception {
        Fixture fixture = fixture();
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                "listener.preflight.missing-lock",
                "editor.preflight.missing-lock",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("old action")))
        );
        Map<String, Object> before = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());
        WebAdminLogicChainEditorRequest edit = rootRequest(listener.id());
        edit.rootType = "listener";
        edit.lockId = lockId(entered);
        edit.baseGraphFingerprint = fingerprint(entered);
        edit.nodes = List.of();
        edit.edges = List.of();
        WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit = actionEditDraftRequest(listener.id(), lockId(entered), fingerprint(entered), "listener", listener.id(), "", 0, messageAction("replacement")).actionEdits.getFirst();
        actionEdit.expectedFingerprint = string(before.get("expectedFingerprint"));
        actionEdit.lockId = "missing-target-lock";
        edit.actionEdits = List.of(actionEdit);

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, edit, fixture.csrf, true);

        requireFalse(result.success(), "target lock preflight blocks missing typed lock before save");
        requireValidationField(result, "logic_chain_target_lock_preflight_validation", "actionEdits[0].lockId");
    }

    private static void testSelectionStartRequiresLogicChainEditorLock() throws Exception {
        Fixture fixture = fixture();
        WebAdminSelectionService selectionService = new WebAdminSelectionService(new WebAdminPermissionService(), fixture.security, fixture.editLockService);
        WebAdminSelectionStartRequest request = new WebAdminSelectionStartRequest();
        request.purpose = "logic_chain_world_device_place";
        request.targetPlayerName = "PlayerOne";
        request.draftSessionId = "selection-lock-draft";
        request.editLockId = "not-a-real-lock";
        request.logicChainRootType = "channel";
        request.logicChainRootRef = "selection.lock.root";
        request.logicChainDraftNodeId = "world-device-draft";

        WebAdminSelectionCancelRequest unconfirmedCancel = new WebAdminSelectionCancelRequest();
        unconfirmedCancel.selectionId = "selection-lock-draft";
        WebAdminWriteResult cancelWithoutConfirmation = selectionService.cancel(null, fixture.editor, fixture.session, "127.0.0.1", unconfirmedCancel, fixture.csrf, true);
        requireFalse(cancelWithoutConfirmation.success(), "selection cancel requires explicit WebUI confirmation");
        requireValidationCode(cancelWithoutConfirmation, "webadmin_selection_cancel_confirmation_required");

        WebAdminWriteResult forgedLock = selectionService.start(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(forgedLock.success(), "Logic Chain selection start rejects forged edit lock before creating protected draft");
        requireEquals("edit_lock_expired", forgedLock.code(), "forged Logic Chain selection lock is rejected");

        WebAdminWriteResult entered = enter(fixture, "channel", "selection.lock.root");
        request.editLockId = lockId(entered);
        WebAdminWriteResult lockAccepted = selectionService.start(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(lockAccepted.success(), "null server still cannot start a player selection");
        requireEquals("target_not_found", lockAccepted.code(), "valid Logic Chain lock passes before player lookup");
    }

    private static void testSignalListenerCreateRequiresLockAndCreateFingerprint() throws Exception {
        Fixture fixture = fixture();
        WebAdminSignalListenerCreateRequest request = new WebAdminSignalListenerCreateRequest();
        request.name = "listener.create.locked";
        request.displayName = "Locked Listener";
        request.channel = "listener.create.locked";
        request.enabled = Boolean.TRUE;
        request.cooldownTicks = 0;

        WebAdminWriteResult missingLock = fixture.signalListenerLifecycleService.create(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(missingLock.success(), "SignalListener create requires an edit lock");
        requireEquals("edit_lock_required", missingLock.code(), "SignalListener create without lock returns edit_lock_required");

        request.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, WebAdminSignalListenerLifecycleService.CREATE_LOCK_TARGET_ID);
        WebAdminWriteResult missingFingerprint = fixture.signalListenerLifecycleService.create(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(missingFingerprint.success(), "SignalListener create requires create fingerprint");
        requireValidationCode(missingFingerprint, "signal_listener_create_fingerprint_required");

        request.expectedFingerprint = WebAdminSignalListenerLifecycleService.CREATE_EXPECTED_FINGERPRINT;
        WebAdminWriteResult created = fixture.signalListenerLifecycleService.create(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(created.success(), "SignalListener create succeeds with lock and create fingerprint");
        requireEquals(0, fixture.editLockService.activeLockCount(), "SignalListener create releases the create lock after success");
    }

    private static void testProtectedDraftRegistryRequiresActorForMutation() {
        WebAdminProtectedDraftRegistry.clearForTests();
        WebAdminUser owner = user(WebAdminRole.EDITOR, "draft-owner");
        WebAdminUser other = user(WebAdminRole.EDITOR, "draft-other");
        WebAdminSession ownerSession = session(owner);
        String draftSessionId = "draft-protected-actor";
        String editLockId = "lock-protected-actor";
        WebAdminProtectedDraftRegistry.start(
                draftSessionId,
                editLockId,
                owner,
                ownerSession,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-protected-actor",
                Set.of("select", "configure", "commit", "cancel")
        );
        WebAdminProtectedDraftRegistry.markSelectedBlock(
                draftSessionId,
                editLockId,
                owner.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "minecraft:overworld",
                1,
                64,
                2,
                "minecraft:stone",
                "",
                Map.of("test", "actor")
        );

        requireTrue(WebAdminProtectedDraftRegistry.canMutateProtectedObject(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-protected-actor",
                editLockId,
                "",
                owner
        ), "original actor with matching edit lock can mutate protected draft object");
        requireFalse(WebAdminProtectedDraftRegistry.canMutateProtectedObject(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-protected-actor",
                editLockId,
                "",
                other
        ), "wrong actor cannot mutate protected draft object even with matching lock id");
        requireFalse(WebAdminProtectedDraftRegistry.canMutateProtectedObject(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-protected-actor",
                "",
                draftSessionId,
                other
        ), "wrong actor cannot mutate protected draft object even with matching draft session id");
        List<String> saveViolations = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                draftSessionId,
                editLockId,
                other,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE
        );
        requireFalse(saveViolations.isEmpty(), "wrong actor cannot save protected draft object");
        List<String> missingActorViolations = WebAdminProtectedDraftRegistry.validateForLogicChainSave(
                draftSessionId,
                editLockId,
                null,
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE
        );
        requireFalse(missingActorViolations.isEmpty(), "missing actor cannot save protected draft object");
        WebAdminProtectedDraftRegistry.cancel(draftSessionId);
        requireTrue(WebAdminProtectedDraftRegistry.canMutateProtectedObject(
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-protected-actor",
                "",
                "",
                other
        ), "terminal protected draft no longer blocks unrelated writes");
        WebAdminProtectedDraftRegistry.clearForTests();
    }

    private static void testProtectedDraftRegistryTerminalStateAndDuplicateStartFailClosed() {
        WebAdminProtectedDraftRegistry.clearForTests();
        WebAdminUser owner = user(WebAdminRole.EDITOR, "draft-terminal-owner");
        WebAdminSession ownerSession = session(owner);
        String draftSessionId = "draft-terminal";
        String editLockId = "lock-terminal";
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry first = WebAdminProtectedDraftRegistry.start(
                draftSessionId,
                editLockId,
                owner,
                ownerSession,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-terminal",
                Set.of("select", "configure", "commit", "cancel")
        );
        requireTrue(first != null, "first protected draft start succeeds");
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry duplicate = WebAdminProtectedDraftRegistry.start(
                draftSessionId,
                editLockId,
                owner,
                ownerSession,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-terminal",
                Set.of("select", "configure", "commit", "cancel")
        );
        requireEquals(null, duplicate, "duplicate protected draft start fails closed");

        WebAdminProtectedDraftRegistry.cancel(draftSessionId);
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry revived = WebAdminProtectedDraftRegistry.markSelectedBlock(
                draftSessionId,
                editLockId,
                owner.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "minecraft:overworld",
                1,
                64,
                2,
                "minecraft:stone",
                "",
                Map.of("test", "terminal")
        );
        requireEquals(null, revived, "cancelled protected draft cannot be selected later");
        requireEquals(WebAdminProtectedDraftRegistry.STATE_CANCELLED,
                string(WebAdminProtectedDraftRegistry.summary(draftSessionId).get("state")),
                "cancelled protected draft remains terminal");
        requireEquals(null, WebAdminProtectedDraftRegistry.markSaving(draftSessionId, editLockId, owner, WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE),
                "cancelled protected draft cannot move to saving");
        requireEquals(null, WebAdminProtectedDraftRegistry.markCommitFailed(draftSessionId, "after_cancel"),
                "cancelled protected draft cannot be returned to configuring");

        String committedId = "draft-committed-terminal";
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry committedStart = WebAdminProtectedDraftRegistry.start(
                committedId,
                editLockId,
                owner,
                ownerSession,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-committed",
                Set.of("select", "configure", "commit", "cancel")
        );
        requireTrue(committedStart != null, "committed draft start succeeds");
        WebAdminProtectedDraftRegistry.markSelectedBlock(
                committedId,
                editLockId,
                owner.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "minecraft:overworld",
                3,
                64,
                4,
                "minecraft:stone",
                "",
                Map.of("test", "commit")
        );
        requireTrue(WebAdminProtectedDraftRegistry.markSaving(committedId, editLockId, owner, WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE) != null,
                "selected protected draft can move to saving");
        WebAdminProtectedDraftRegistry.cancel(committedId);
        requireEquals(WebAdminProtectedDraftRegistry.STATE_SAVING,
                string(WebAdminProtectedDraftRegistry.summary(committedId).get("state")),
                "saving protected draft cannot be cancelled during commit");
        requireTrue(WebAdminProtectedDraftRegistry.markCommitted(committedId) != null, "saving protected draft can commit");
        requireEquals(null, WebAdminProtectedDraftRegistry.markCommitFailed(committedId, "after_commit"),
                "committed protected draft cannot be revived by commit failure");
        requireEquals(WebAdminProtectedDraftRegistry.STATE_COMMITTED,
                string(WebAdminProtectedDraftRegistry.summary(committedId).get("state")),
                "committed protected draft remains terminal");

        String expiredId = "draft-expired-terminal";
        WebAdminProtectedDraftRegistry.registerForTest(new WebAdminProtectedDraftRegistry.ProtectedDraftEntry(
                expiredId,
                editLockId,
                owner.username,
                owner.username,
                ownerSession.sessionIdHash,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "vbd-expired",
                expiredId,
                "minecraft:overworld",
                "minecraft:overworld",
                "5,64,6",
                5,
                64,
                6,
                "",
                "",
                "minecraft:stone",
                "1970-01-01T00:00:00Z",
                "1970-01-01T00:00:01Z",
                WebAdminProtectedDraftRegistry.STATE_SELECTED,
                Set.of("select", "configure", "commit", "cancel"),
                Map.of()
        ));
        requireEquals(WebAdminProtectedDraftRegistry.STATE_EXPIRED,
                string(WebAdminProtectedDraftRegistry.summary(expiredId).get("state")),
                "stale protected draft expires before save");
        requireEquals(null, WebAdminProtectedDraftRegistry.markSelectedBlock(
                expiredId,
                editLockId,
                owner.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "minecraft:overworld",
                5,
                64,
                6,
                "minecraft:stone",
                "",
                Map.of("test", "expired")
        ), "expired protected draft cannot be selected again");
        requireEquals(null, WebAdminProtectedDraftRegistry.markSaving(expiredId, editLockId, owner, WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE),
                "expired protected draft cannot move to saving");
        requireEquals(null, WebAdminProtectedDraftRegistry.markCommitFailed(expiredId, "after_expired"),
                "expired protected draft cannot be returned to configuring");
        WebAdminProtectedDraftRegistry.clearForTests();
    }

    private static void testVbdDraftAllowsCapturePayloadValidationAndMixedWrites() throws Exception {
        WebAdminProtectedDraftRegistry.clearForTests();
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.vbd.capture");
        String protectedDraftId = "draft-vbd-capture";
        registerSelectedVbdDraft(fixture, protectedDraftId, lockId(entered));
        WebAdminLogicChainEditorRequest request = vbdDraftRequest("editor.vbd.capture", lockId(entered), fingerprint(entered), protectedDraftId);
        WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft requirement = new WebAdminLogicChainEditorRequest.ItemSubmitRequirementDraft();
        requirement.requirementId = "need-stone";
        requirement.displayName = "需要石头";
        requirement.count = 5;
        requirement.consumeCount = 1;
        requirement.consumeCountFollowsCount = Boolean.TRUE;
        requirement.captureDraftId = "item-capture-draft";
        request.nodes.getFirst().virtualBlockDevice.itemSubmitEnabled = true;
        request.nodes.getFirst().virtualBlockDevice.itemSubmitRequirements = List.of(requirement);

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);

        requireFalse(result.success(), "VBD itemSubmit count/consumeCount mismatch still fails validation");
        requireValidationCode(result, "logic_chain_item_submit_consume_count_follow_mismatch");
        requireNoValidationCode(result, "logic_chain_vbd_item_submit_container_commit_not_wired");

        requirement.consumeCount = 5;
        WebAdminWriteResult fixed = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(fixed.success(), "VBD itemSubmit/container draft payload validates once count contract is fixed errors=" + fixed.validationErrors());

        Fixture mixedFixture = fixture();
        WebAdminWriteResult mixedEntered = enter(mixedFixture, "editor.vbd.mixed");
        String mixedProtectedDraftId = "draft-vbd-mixed";
        registerSelectedVbdDraft(mixedFixture, mixedProtectedDraftId, lockId(mixedEntered));
        WebAdminLogicChainEditorRequest mixed = vbdDraftRequest("editor.vbd.mixed", lockId(mixedEntered), fingerprint(mixedEntered), mixedProtectedDraftId);
        SignalListenerData listener = saveSignalListenerFixture(
                mixedFixture,
                "listener.vbd.mixed",
                "editor.vbd.mixed",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("old action")))
        );
        Map<String, Object> before = mixedFixture.signalListenerActionsService.actionsFor(null, mixedFixture.editor, mixedFixture.session, listener.id());
        String typedLockId = acquireLock(mixedFixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listener.id());
        WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit = actionEditDraftRequest("editor.vbd.mixed", lockId(mixedEntered), fingerprint(mixedEntered), "listener", listener.id(), "", 0, messageAction("replacement")).actionEdits.getFirst();
        actionEdit.expectedFingerprint = string(before.get("expectedFingerprint"));
        actionEdit.lockId = typedLockId;
        mixed.actionEdits = List.of(actionEdit);

        WebAdminWriteResult mixedResult = mixedFixture.service.validateDraft(null, mixedFixture.editor, mixedFixture.session, mixed, mixedFixture.csrf, true);

        requireTrue(mixedResult.success(), "VBD draft can coexist with another non-conflicting typed draft after target lock preflight errors=" + mixedResult.validationErrors());
        requireNoValidationCode(mixedResult, "logic_chain_world_backed_single_write_fail_closed");
        WebAdminProtectedDraftRegistry.clearForTests();
    }

    private static void testWorldDeviceDraftRejectsRequestBodyDeviceTypeAuthority() throws Exception {
        WebAdminProtectedDraftRegistry.clearForTests();
        Fixture fixture = fixture();
        WebAdminWriteResult entered = enter(fixture, "editor.world-device.type-authority");
        String protectedDraftId = "draft-world-device-no-type";
        registerSelectedWorldDeviceDraft(fixture, protectedDraftId, lockId(entered), Map.of());
        WebAdminLogicChainEditorRequest request = worldDeviceDraftRequest(
                "editor.world-device.type-authority",
                lockId(entered),
                fingerprint(entered),
                protectedDraftId,
                "signal_receiver"
        );

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);

        requireFalse(result.success(), "world device request body type must not decide producer/consumer edge semantics");
        requireValidationCode(result, "logic_chain_world_device_type_mismatch");

        WebAdminProtectedDraftRegistry.clearForTests();
    }

    private static void testWorldDeviceConsumerValidationSplitsInputOutputChannels() throws Exception {
        WebAdminProtectedDraftRegistry.clearForTests();
        Fixture receiverFixture = fixture();
        WebAdminWriteResult receiverEntered = enter(receiverFixture, "editor.receiver.consumer");
        String receiverDraftId = "draft-world-device-receiver";
        registerSelectedWorldDeviceDraft(receiverFixture, receiverDraftId, lockId(receiverEntered), Map.of("deviceType", "signal_receiver", "deviceId", "signal_device:minecraft:overworld@12,64,12"));
        WebAdminLogicChainEditorRequest receiver = worldDeviceDraftRequest(
                "editor.receiver.consumer",
                lockId(receiverEntered),
                fingerprint(receiverEntered),
                receiverDraftId,
                "signal_receiver"
        );

        WebAdminWriteResult receiverValid = receiverFixture.service.validateDraft(null, receiverFixture.editor, receiverFixture.session, receiver, receiverFixture.csrf, true);

        requireTrue(receiverValid.success(), "SignalReceiver consumer validates with channel -> receiver consumes edge: " + receiverValid.validationErrors());
        requireNoValidationCode(receiverValid, "logic_chain_world_device_output_channel_required");

        Fixture missingReceiverFixture = fixture();
        WebAdminWriteResult missingReceiverEntered = enter(missingReceiverFixture, "editor.receiver.missing-input");
        String missingReceiverDraftId = "draft-world-device-receiver-missing";
        registerSelectedWorldDeviceDraft(missingReceiverFixture, missingReceiverDraftId, lockId(missingReceiverEntered), Map.of("deviceType", "signal_receiver", "deviceId", "signal_device:minecraft:overworld@12,64,12"));
        WebAdminLogicChainEditorRequest missingReceiver = worldDeviceDraftRequest(
                "editor.receiver.missing-input",
                lockId(missingReceiverEntered),
                fingerprint(missingReceiverEntered),
                missingReceiverDraftId,
                "signal_receiver"
        );
        missingReceiver.edges = List.of();

        WebAdminWriteResult missingReceiverResult = missingReceiverFixture.service.saveDraft(null, missingReceiverFixture.editor, missingReceiverFixture.session, "127.0.0.1", missingReceiver, missingReceiverFixture.csrf, true);

        requireFalse(missingReceiverResult.success(), "SignalReceiver without upstream channel is rejected");
        requireValidationCode(missingReceiverResult, "logic_chain_world_device_input_channel_required");
        requireNoValidationCode(missingReceiverResult, "logic_chain_world_device_output_channel_required");

        Fixture relayFixture = fixture();
        WebAdminWriteResult relayEntered = enter(relayFixture, "editor.relay.consumer");
        String relayDraftId = "draft-world-device-relay";
        registerSelectedWorldDeviceDraft(relayFixture, relayDraftId, lockId(relayEntered), Map.of("deviceType", "action_relay", "deviceId", "signal_device:minecraft:overworld@12,64,12"));
        WebAdminLogicChainEditorRequest relay = worldDeviceDraftRequest(
                "editor.relay.consumer",
                lockId(relayEntered),
                fingerprint(relayEntered),
                relayDraftId,
                "action_relay"
        );

        WebAdminWriteResult relayValid = relayFixture.service.validateDraft(null, relayFixture.editor, relayFixture.session, relay, relayFixture.csrf, true);

        requireTrue(relayValid.success(), "ActionRelay consumer validates with channel -> relay consumes edge: " + relayValid.validationErrors());
        requireNoValidationCode(relayValid, "logic_chain_world_device_output_channel_required");

        Fixture emitterFixture = fixture();
        WebAdminWriteResult emitterEntered = enter(emitterFixture, "editor.emitter.producer");
        String emitterDraftId = "draft-world-device-emitter";
        registerSelectedWorldDeviceDraft(emitterFixture, emitterDraftId, lockId(emitterEntered), Map.of("deviceType", "signal_emitter", "deviceId", "signal_device:minecraft:overworld@12,64,12"));
        WebAdminLogicChainEditorRequest emitter = worldDeviceDraftRequest(
                "editor.emitter.producer",
                lockId(emitterEntered),
                fingerprint(emitterEntered),
                emitterDraftId,
                "signal_emitter"
        );
        String emitterNodeId = emitter.nodes.getFirst().id;
        emitter.edges = List.of(edge("draft-edge-world-device-out", emitterNodeId, "channel:editor.world-device.out", "world_device_outputs_channel"));

        WebAdminWriteResult emitterValid = emitterFixture.service.validateDraft(null, emitterFixture.editor, emitterFixture.session, emitter, emitterFixture.csrf, true);

        requireTrue(emitterValid.success(), "SignalEmitter producer validates with emitter -> channel output edge: " + emitterValid.validationErrors());

        Fixture missingEmitterFixture = fixture();
        WebAdminWriteResult missingEmitterEntered = enter(missingEmitterFixture, "editor.emitter.missing-output");
        String missingEmitterDraftId = "draft-world-device-emitter-missing";
        registerSelectedWorldDeviceDraft(missingEmitterFixture, missingEmitterDraftId, lockId(missingEmitterEntered), Map.of("deviceType", "signal_emitter", "deviceId", "signal_device:minecraft:overworld@12,64,12"));
        WebAdminLogicChainEditorRequest missingEmitter = worldDeviceDraftRequest(
                "editor.emitter.missing-output",
                lockId(missingEmitterEntered),
                fingerprint(missingEmitterEntered),
                missingEmitterDraftId,
                "signal_emitter"
        );
        missingEmitter.edges = List.of();

        WebAdminWriteResult missingEmitterResult = missingEmitterFixture.service.saveDraft(null, missingEmitterFixture.editor, missingEmitterFixture.session, "127.0.0.1", missingEmitter, missingEmitterFixture.csrf, true);

        requireFalse(missingEmitterResult.success(), "SignalEmitter without downstream channel is rejected");
        requireValidationCode(missingEmitterResult, "logic_chain_world_device_output_channel_required");
        WebAdminProtectedDraftRegistry.clearForTests();
    }

    private static void testExistingVbdNativeTriggerDraftRequiresNativeTriggerLock() throws Exception {
        Fixture fixture = fixture();
        String deviceId = "virtual_block_device:minecraft:overworld@10,64,10";
        WebAdminWriteResult entered = enter(fixture, "device", deviceId);
        WebAdminLogicChainEditorRequest request = nativeTriggerEditRequest(deviceId, lockId(entered), fingerprint(entered));

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);

        requireFalse(result.success(), "VBD native trigger draft requires its own typed edit lock");
        requireValidationField(result, "edit_lock_required", "existingNodeEdits[0].virtualBlockDevice.nativeTriggers.lockId");

        Fixture wrongTypeFixture = fixture();
        WebAdminWriteResult wrongTypeEntered = enter(wrongTypeFixture, "device", deviceId);
        WebAdminLogicChainEditorRequest wrongType = nativeTriggerEditRequest(deviceId, lockId(wrongTypeEntered), fingerprint(wrongTypeEntered));
        wrongType.existingNodeEdits.getFirst().virtualBlockDevice.nativeTriggers.lockId = acquireLock(wrongTypeFixture, WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, deviceId);

        WebAdminWriteResult wrongTypeResult = wrongTypeFixture.service.validateDraft(null, wrongTypeFixture.editor, wrongTypeFixture.session, wrongType, wrongTypeFixture.csrf, true);

        requireValidationField(wrongTypeResult, "logic_chain_target_lock_preflight_validation", "existingNodeEdits[0].virtualBlockDevice.nativeTriggers.lockId");

        Fixture wrongTargetFixture = fixture();
        WebAdminWriteResult wrongTargetEntered = enter(wrongTargetFixture, "device", deviceId);
        WebAdminLogicChainEditorRequest wrongTarget = nativeTriggerEditRequest(deviceId, lockId(wrongTargetEntered), fingerprint(wrongTargetEntered));
        wrongTarget.existingNodeEdits.getFirst().virtualBlockDevice.nativeTriggers.lockId = acquireLock(wrongTargetFixture, WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS, "virtual_block_device:minecraft:overworld@11,64,11");

        WebAdminWriteResult wrongTargetResult = wrongTargetFixture.service.validateDraft(null, wrongTargetFixture.editor, wrongTargetFixture.session, wrongTarget, wrongTargetFixture.csrf, true);

        requireValidationField(wrongTargetResult, "logic_chain_target_lock_preflight_validation", "existingNodeEdits[0].virtualBlockDevice.nativeTriggers.lockId");

        Fixture validFixture = fixture();
        WebAdminWriteResult validEntered = enter(validFixture, "device", deviceId);
        WebAdminLogicChainEditorRequest valid = nativeTriggerEditRequest(deviceId, lockId(validEntered), fingerprint(validEntered));
        valid.existingNodeEdits.getFirst().virtualBlockDevice.nativeTriggers.lockId = acquireLock(validFixture, WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS, deviceId);

        WebAdminWriteResult validResult = validFixture.service.validateDraft(null, validFixture.editor, validFixture.session, valid, validFixture.csrf, true);

        requireNoValidationCode(validResult, "logic_chain_target_lock_preflight_validation");
        requireNoValidationCode(validResult, "edit_lock_required");
    }

    private static void testVbdProducerProjectionNodeDeleteValidatesTypedOwned() throws Exception {
        Fixture fixture = fixture();
        String deviceId = "virtual_block_device:minecraft:overworld@10,64,10";
        WebAdminWriteResult entered = enter(fixture, "device", deviceId);
        WebAdminLogicChainEditorRequest request = rootRequest(deviceId);
        request.rootType = "device";
        request.lockId = lockId(entered);
        request.baseGraphFingerprint = fingerprint(entered);
        request.nodes = List.of();
        request.edges = List.of();
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, deviceId);
        request.nodeDeletes = List.of(nodeDeleteDraft("virtual_block_device", deviceId, "vbd-fingerprint", typedLockId));

        WebAdminWriteResult result = fixture.service.validateDraft(null, fixture.editor, fixture.session, request, fixture.csrf, true);

        requireTrue(result.success(), "VBD producer/device graph projection is a typed-owned VBD delete target: " + result.validationErrors());
    }

    private static void testSignalListenerNodeDeleteRequiresTypedIdentityAndDeletesWithLock() throws Exception {
        Fixture fixture = fixture();
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                "listener.node.delete",
                "editor.listener.node.delete",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("removed with owner")))
        );
        WebAdminDtos.SignalListenerBasicConfigDto before = fixture.signalListenerBasicConfigService.configFor(null, fixture.editor, fixture.session, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());

        WebAdminLogicChainEditorRequest missingIdentity = rootRequest(listener.id());
        missingIdentity.rootType = "listener";
        missingIdentity.lockId = lockId(entered);
        missingIdentity.baseGraphFingerprint = fingerprint(entered);
        missingIdentity.nodes = List.of();
        missingIdentity.edges = List.of();
        WebAdminLogicChainEditorRequest.NodeDeleteDraft missing = nodeDeleteDraft("signal_listener", listener.id(), "", "");
        missingIdentity.nodeDeletes = List.of(missing);

        WebAdminWriteResult invalid = fixture.service.validateDraft(null, fixture.editor, fixture.session, missingIdentity, fixture.csrf, true);
        requireFalse(invalid.success(), "SignalListener node delete requires lock and expectedFingerprint");
        requireValidationCode(invalid, "required");

        WebAdminLogicChainEditorRequest wrongPhraseRequest = rootRequest(listener.id());
        wrongPhraseRequest.rootType = "listener";
        wrongPhraseRequest.lockId = lockId(entered);
        wrongPhraseRequest.baseGraphFingerprint = fingerprint(entered);
        wrongPhraseRequest.nodes = List.of();
        wrongPhraseRequest.edges = List.of();
        WebAdminLogicChainEditorRequest.NodeDeleteDraft wrongPhrase = nodeDeleteDraft("signal_listener", listener.id(), before.expectedFingerprint(), "typed-lock");
        wrongPhrase.confirmationText = "删除";
        wrongPhraseRequest.nodeDeletes = List.of(wrongPhrase);

        WebAdminWriteResult wrongPhraseResult = fixture.service.validateDraft(null, fixture.editor, fixture.session, wrongPhraseRequest, fixture.csrf, true);
        requireFalse(wrongPhraseResult.success(), "node delete requires exact fixed phrase");
        requireValidationCode(wrongPhraseResult, "logic_chain_node_delete_confirm_phrase_required");

        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, listener.id());
        WebAdminLogicChainEditorRequest deleteRequest = rootRequest(listener.id());
        deleteRequest.rootType = "listener";
        deleteRequest.lockId = lockId(entered);
        deleteRequest.baseGraphFingerprint = fingerprint(entered);
        deleteRequest.nodes = List.of();
        deleteRequest.edges = List.of();
        deleteRequest.nodeDeletes = List.of(nodeDeleteDraft("signal_listener", listener.id(), before.expectedFingerprint(), typedLockId));

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", deleteRequest, fixture.csrf, true);

        requireTrue(saved.success(), "SignalListener typed-owned node delete saves through lifecycle service errors=" + saved.validationErrors());
        requireFalse(signalListenerExists(fixture, listener.id()), "SignalListener node delete removes the typed-owned listener");
        requireEquals(0, fixture.editLockService.activeLockCount(), "node delete releases editor and typed locks");
    }

    private static void testSignalListenerActionDeleteAndReorderDraftsSaveWithLock() throws Exception {
        Fixture deleteFixture = fixture();
        SignalListenerData deleteListener = saveSignalListenerFixture(
                deleteFixture,
                "listener.action.delete",
                "editor.listener.action.delete",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("delete me")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("keep me"))
                )
        );
        Map<String, Object> deleteBefore = deleteFixture.signalListenerActionsService.actionsFor(null, deleteFixture.editor, deleteFixture.session, deleteListener.id());
        String deleteLockId = acquireLock(deleteFixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, deleteListener.id());
        WebAdminWriteResult deleteEntered = enter(deleteFixture, "listener", deleteListener.id());
        WebAdminLogicChainEditorRequest deleteRequest = rootRequest(deleteListener.id());
        deleteRequest.rootType = "listener";
        deleteRequest.lockId = lockId(deleteEntered);
        deleteRequest.baseGraphFingerprint = fingerprint(deleteEntered);
        deleteRequest.nodes = List.of();
        deleteRequest.edges = List.of();
        deleteRequest.actionDeletes = List.of(actionDeleteDraft("listener", deleteListener.id(), "", 0, string(deleteBefore.get("expectedFingerprint")), deleteLockId));

        WebAdminWriteResult deleteSaved = deleteFixture.service.saveDraft(null, deleteFixture.editor, deleteFixture.session, "127.0.0.1", deleteRequest, deleteFixture.csrf, true);
        requireTrue(deleteSaved.success(), "SignalListener action delete saves through draft operation errors=" + deleteSaved.validationErrors());
        SignalListenerData deleteAfter = requireSignalListener(deleteFixture, deleteListener.id());
        requireEquals(1, deleteAfter.actions().size(), "action delete removes exactly one action");
        requireEquals("keep me", deleteAfter.actions().getFirst().value(), "action delete preserves following action");

        Fixture reorderFixture = fixture();
        SignalListenerData reorderListener = saveSignalListenerFixture(
                reorderFixture,
                "listener.action.reorder",
                "editor.listener.action.reorder",
                List.of(
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("first")),
                        WebAdminActionRelayActionsService.actionFromEntry(messageAction("second"))
                )
        );
        Map<String, Object> reorderBefore = reorderFixture.signalListenerActionsService.actionsFor(null, reorderFixture.editor, reorderFixture.session, reorderListener.id());
        String reorderLockId = acquireLock(reorderFixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, reorderListener.id());
        WebAdminWriteResult reorderEntered = enter(reorderFixture, "listener", reorderListener.id());
        WebAdminLogicChainEditorRequest reorderRequest = rootRequest(reorderListener.id());
        reorderRequest.rootType = "listener";
        reorderRequest.lockId = lockId(reorderEntered);
        reorderRequest.baseGraphFingerprint = fingerprint(reorderEntered);
        reorderRequest.nodes = List.of();
        reorderRequest.edges = List.of();
        reorderRequest.actionReorders = List.of(actionReorderDraft("listener", reorderListener.id(), "", 0, 1, string(reorderBefore.get("expectedFingerprint")), reorderLockId));

        WebAdminWriteResult reorderSaved = reorderFixture.service.saveDraft(null, reorderFixture.editor, reorderFixture.session, "127.0.0.1", reorderRequest, reorderFixture.csrf, true);
        requireTrue(reorderSaved.success(), "SignalListener action reorder saves through draft operation errors=" + reorderSaved.validationErrors());
        SignalListenerData reorderAfter = requireSignalListener(reorderFixture, reorderListener.id());
        requireEquals("second", reorderAfter.actions().get(0).value(), "action reorder moves target action");
        requireEquals("first", reorderAfter.actions().get(1).value(), "action reorder preserves same bucket content");
    }

    private static WebAdminWriteResult enter(Fixture fixture, String rootRef) {
        WebAdminWriteResult result = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", rootRequest(rootRef), fixture.csrf, true);
        requireTrue(result.success(), "enter edit mode for " + rootRef);
        return result;
    }

    private static WebAdminWriteResult enter(Fixture fixture, String rootType, String rootRef) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.rootType = rootType;
        request.rootRef = rootRef;
        WebAdminWriteResult result = fixture.service.enter(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "enter edit mode for " + rootType + ":" + rootRef);
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

    private static WebAdminLogicChainEditorRequest vbdDraftRequest(String rootRef, String lockId, String fingerprint, String protectedDraftId) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:virtual_block_device:" + protectedDraftId;
        node.type = "virtual_block_device";
        node.column = "C2";
        node.slot = 0;
        node.placed = true;
        node.protectedDraftId = protectedDraftId;
        node.virtualBlockDevice.protectedDraftId = protectedDraftId;
        node.virtualBlockDevice.displayName = "Logic Chain VBD";
        node.virtualBlockDevice.note = "draft only";
        node.virtualBlockDevice.enabled = Boolean.TRUE;
        request.nodes = List.of(node);
        request.edges = List.of(edge("draft-edge-vbd-out", node.id, "channel:editor.vbd.out", "vbd_outputs_channel"));
        return request;
    }

    private static WebAdminLogicChainEditorRequest worldDeviceDraftRequest(String rootRef, String lockId, String fingerprint, String protectedDraftId, String deviceType) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:world_device:" + protectedDraftId;
        node.type = "world_device";
        node.column = "C3";
        node.slot = 0;
        node.placed = true;
        node.protectedDraftId = protectedDraftId;
        node.worldDevice.protectedDraftId = protectedDraftId;
        node.worldDevice.deviceType = deviceType;
        node.worldDevice.deviceId = "signal_device:minecraft:overworld@12,64,12";
        node.worldDevice.displayName = "Logic Chain World Device";
        node.worldDevice.enabled = Boolean.TRUE;
        request.nodes = List.of(node);
        request.edges = List.of(edge("draft-edge-world-device-in", "channel:editor.world-device.in", node.id, "world_device_consumes_channel"));
        return request;
    }

    private static void registerSelectedVbdDraft(Fixture fixture, String protectedDraftId, String editLockId) {
        WebAdminProtectedDraftRegistry.start(
                protectedDraftId,
                editLockId,
                fixture.editor,
                fixture.session,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "draft:virtual_block_device:" + protectedDraftId,
                Set.of("select", "configure", "commit", "cancel")
        );
        WebAdminProtectedDraftRegistry.markSelectedBlock(
                protectedDraftId,
                editLockId,
                fixture.editor.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "minecraft:overworld",
                10,
                64,
                10,
                "minecraft:lever",
                "",
                Map.of("test", "logic_chain_vbd")
        );
    }

    private static void registerSelectedWorldDeviceDraft(Fixture fixture, String protectedDraftId, String editLockId, Map<String, ?> metadata) {
        WebAdminProtectedDraftRegistry.start(
                protectedDraftId,
                editLockId,
                fixture.editor,
                fixture.session,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE,
                "draft:world_device:" + protectedDraftId,
                Set.of("select", "configure", "commit", "cancel")
        );
        WebAdminProtectedDraftRegistry.markSelectedBlock(
                protectedDraftId,
                editLockId,
                fixture.editor.username,
                "player-uuid",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_WORLD_DEVICE,
                "minecraft:overworld",
                12,
                64,
                12,
                "minecraft:stone",
                "",
                metadata
        );
    }

    private static WebAdminLogicChainEditorRequest signalListenerDraftRequest(String rootRef, String lockId, String fingerprint, String listenerName) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        WebAdminLogicChainEditorRequest.DraftNode node = new WebAdminLogicChainEditorRequest.DraftNode();
        node.id = "draft:signal_listener:" + listenerName;
        node.type = "signal_listener";
        node.column = "C2";
        node.slot = 0;
        node.placed = true;
        node.signalListener.name = listenerName;
        node.signalListener.displayName = "Editor Listener";
        node.signalListener.note = "created from Logic Chain";
        node.signalListener.channel = "";
        node.signalListener.cooldownTicks = 30;
        node.signalListener.conditionGroupId = "always";
        request.nodes = List.of(node);
        request.edges = List.of(edge("draft-edge-listener-in", "channel:editor.listener.in", node.id, "consumes"));
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

    private static WebAdminLogicChainEditorRequest nativeTriggerEditRequest(String deviceId, String editorLockId, String baseGraphFingerprint) {
        WebAdminLogicChainEditorRequest request = rootRequest(deviceId);
        request.rootType = "device";
        request.lockId = editorLockId;
        request.baseGraphFingerprint = baseGraphFingerprint;
        request.nodes = List.of();
        request.edges = List.of();
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        edit.nodeType = "virtual_block_device";
        edit.targetId = deviceId;
        edit.virtualBlockDevice = new WebAdminLogicChainEditorRequest.VirtualBlockDeviceDraft();
        edit.virtualBlockDevice.nativeTriggers = new WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest();
        edit.virtualBlockDevice.nativeTriggers.deviceId = deviceId;
        edit.virtualBlockDevice.nativeTriggers.expectedFingerprint = "vbd-native-fingerprint";
        request.existingNodeEdits = List.of(edit);
        return request;
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

    private static WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDeleteDraft(
            String nodeType,
            String targetId,
            String expectedFingerprint,
            String lockId
    ) {
        WebAdminLogicChainEditorRequest.NodeDeleteDraft draft = new WebAdminLogicChainEditorRequest.NodeDeleteDraft();
        draft.nodeType = nodeType;
        draft.targetId = targetId;
        draft.ownerType = nodeType;
        draft.ownerId = targetId;
        draft.confirmed = true;
        draft.impactAccepted = true;
        draft.confirmationText = "我确认删除该节点";
        draft.expectedFingerprint = expectedFingerprint;
        draft.lockId = lockId;
        return draft;
    }

    private static WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDeleteDraft(
            String ownerType,
            String ownerId,
            String bucket,
            int actionIndex,
            String expectedFingerprint,
            String lockId
    ) {
        WebAdminLogicChainEditorRequest.ActionDeleteDraft draft = new WebAdminLogicChainEditorRequest.ActionDeleteDraft();
        draft.ownerType = ownerType;
        draft.ownerId = ownerId;
        draft.bucket = bucket;
        draft.actionIndex = actionIndex;
        draft.confirmed = true;
        draft.expectedFingerprint = expectedFingerprint;
        draft.lockId = lockId;
        return draft;
    }

    private static WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorderDraft(
            String ownerType,
            String ownerId,
            String bucket,
            int fromIndex,
            int toIndex,
            String expectedFingerprint,
            String lockId
    ) {
        WebAdminLogicChainEditorRequest.ActionReorderDraft draft = new WebAdminLogicChainEditorRequest.ActionReorderDraft();
        draft.ownerType = ownerType;
        draft.ownerId = ownerId;
        draft.bucket = bucket;
        draft.fromIndex = fromIndex;
        draft.toIndex = toIndex;
        draft.confirmed = true;
        draft.expectedFingerprint = expectedFingerprint;
        draft.lockId = lockId;
        return draft;
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

    private static void assignActionAppendLock(Fixture fixture, WebAdminLogicChainEditorRequest request) {
        WebAdminLogicChainEditorRequest.ActionAppendDraft draft = request == null ? null : request.actionAppend;
        if (draft == null) {
            return;
        }
        String ownerType = switch (String.valueOf(draft.ownerType).toLowerCase(Locale.ROOT)) {
            case "listener" -> WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS;
            case "action_relay" -> WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS;
            case "region_controller" -> WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG;
            case "timer" -> WebAdminEditLockService.TARGET_TIMER_CONFIG;
            default -> "";
        };
        if (!ownerType.isBlank()) {
            draft.lockId = acquireLock(fixture, ownerType, draft.ownerId);
        }
    }

    private static void assignActionEditLock(Fixture fixture, WebAdminLogicChainEditorRequest.ActionEditDraft draft) {
        if (draft == null) {
            return;
        }
        String ownerType = switch (String.valueOf(draft.ownerType).toLowerCase(Locale.ROOT)) {
            case "listener" -> WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS;
            case "action_relay" -> WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS;
            case "region_controller" -> WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG;
            case "timer" -> WebAdminEditLockService.TARGET_TIMER_CONFIG;
            default -> "";
        };
        if (!ownerType.isBlank()) {
            draft.lockId = acquireLock(fixture, ownerType, draft.ownerId);
        }
    }

    private static WebAdminLogicChainEditorRequest actionEditDraftRequest(
            String rootRef,
            String lockId,
            String fingerprint,
            String ownerType,
            String ownerId,
            String bucket,
            int actionIndex,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry action
    ) {
        WebAdminLogicChainEditorRequest request = rootRequest(rootRef);
        request.lockId = lockId;
        request.baseGraphFingerprint = fingerprint;
        request.nodes = List.of();
        request.edges = List.of();
        WebAdminLogicChainEditorRequest.ActionEditDraft edit = new WebAdminLogicChainEditorRequest.ActionEditDraft();
        edit.ownerType = ownerType;
        edit.ownerId = ownerId;
        edit.bucket = bucket;
        edit.actionIndex = actionIndex;
        edit.operation = "replace";
        edit.action = action;
        edit.expectedFingerprint = "typed-owner-fingerprint";
        edit.lockId = "typed-owner-lock";
        request.actionEdits = List.of(edit);
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

    private static WebAdminLogicChainEditorRequest.ExistingNodeEditDraft channelMetadataExistingEdit(
            Fixture fixture,
            String channel,
            String displayName
    ) {
        WebAdminDtos.ChannelMetadataDto before = fixture.channelMetadataService.metadataFor(null, fixture.editor, fixture.session, channel, "signal");
        String lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_CHANNEL_METADATA, channel);
        WebAdminChannelMetadataUpdateRequest metadata = new WebAdminChannelMetadataUpdateRequest();
        metadata.channel = channel;
        metadata.displayName = displayName;
        metadata.note = "multi draft session";
        metadata.iconKey = "auto";
        metadata.expectedFingerprint = before.expectedFingerprint();
        metadata.lockId = lockId;
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft = new WebAdminLogicChainEditorRequest.ExistingNodeEditDraft();
        draft.nodeType = "channel_metadata";
        draft.targetId = channel;
        draft.channelMetadata = metadata;
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

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry stateVariableAction() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = messageAction("");
        entry.type = "state_variable";
        entry.stateOperation = "set_variable";
        entry.stateScope = "GLOBAL";
        entry.stateTargetMode = "global";
        entry.stateKey = "mission.count";
        entry.stateValueType = "INTEGER";
        entry.stateValue = "5";
        entry.stateCreateIfMissing = Boolean.TRUE;
        entry.stateInitialValue = "0";
        return entry;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry timerStartAction(String timerId) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = messageAction("");
        entry.type = "timer_start";
        entry.timerId = timerId;
        entry.timerTargetMode = "global";
        entry.timerStartPolicyOverride = "RESTART";
        entry.timerDurationOverrideTicks = 0;
        return entry;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry timerCancelAction(String timerId) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = messageAction("");
        entry.type = "timer_cancel";
        entry.timerId = timerId;
        entry.timerTargetMode = "global";
        entry.timerMissingBehavior = "fail";
        entry.timerDurationOverrideTicks = 0;
        return entry;
    }

    private static ActionConfig saveSingleListenerActionEditRoundtrip(
            String listenerId,
            WebAdminActionRelayActionsUpdateRequest.ActionEntry replacement
    ) throws Exception {
        Fixture fixture = fixture();
        saveAlwaysConditionGroup(fixture);
        SignalListenerData listener = saveSignalListenerFixture(
                fixture,
                listenerId,
                listenerId + ".in",
                List.of(WebAdminActionRelayActionsService.actionFromEntry(messageAction("structured old action")))
        );
        Map<String, Object> before = fixture.signalListenerActionsService.actionsFor(null, fixture.editor, fixture.session, listener.id());
        String typedLockId = acquireLock(fixture, WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listener.id());
        WebAdminWriteResult entered = enter(fixture, "listener", listener.id());
        WebAdminLogicChainEditorRequest edit = actionEditDraftRequest(
                listener.id(),
                lockId(entered),
                fingerprint(entered),
                "listener",
                listener.id(),
                "",
                0,
                replacement
        );
        edit.rootType = "listener";
        edit.actionEdits.getFirst().expectedFingerprint = string(before.get("expectedFingerprint"));
        edit.actionEdits.getFirst().lockId = typedLockId;

        WebAdminWriteResult saved = fixture.service.saveDraft(null, fixture.editor, fixture.session, "127.0.0.1", edit, fixture.csrf, true);
        requireTrue(saved.success(), "structured Logic Chain actionEdits save roundtrip succeeds for " + replacement.type + " errors=" + saved.validationErrors());
        SignalListenerData after = requireSignalListener(fixture, listener.id());
        requireEquals(1, after.actions().size(), "structured action edit keeps same action count for " + replacement.type);
        requireEquals(0, fixture.editLockService.activeLockCount(), "structured action edit releases locks for " + replacement.type);
        return after.actions().getFirst();
    }

    private static void saveAlwaysConditionGroup(Fixture fixture) {
        WebAdminConditionGroupStore.ConditionGroupFile file = WebAdminConditionGroupStore.load(fixture.directory.resolve(WebAdminConditionGroupStore.FILE_NAME));
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = "always";
        entry.displayName = "始终通过";
        entry.enabled = true;
        entry.groupDefinition = WebAdminConditionGroupStore.defaultDefinition("always", "始终通过");
        file.groups.put("always", entry);
        requireTrue(WebAdminConditionGroupStore.save(fixture.directory.resolve(WebAdminConditionGroupStore.FILE_NAME), file), "save always condition group fixture");
    }

    private static SignalListenerData saveSignalListenerFixture(
            Fixture fixture,
            String id,
            String channel,
            List<ActionConfig> actions
    ) {
        SignalListenerStore.DataFile file = SignalListenerStore.loadWithStatus(fixture.directory.resolve(SignalListenerStore.FILE_NAME)).file();
        SignalListenerData listener = new SignalListenerData(
                id,
                "Listener " + id,
                channel,
                true,
                0,
                "",
                actions == null ? List.of() : actions
        ).normalized();
        file.listeners.removeIf(existing -> existing.id().equals(listener.id()));
        file.listeners.add(listener);
        requireTrue(SignalListenerStore.save(fixture.directory.resolve(SignalListenerStore.FILE_NAME), file), "save SignalListener fixture " + id);
        return listener;
    }

    private static SignalListenerData requireSignalListener(Fixture fixture, String id) {
        for (SignalListenerData listener : SignalListenerStore.loadWithStatus(fixture.directory.resolve(SignalListenerStore.FILE_NAME)).file().listeners) {
            if (listener.id().equals(id)) {
                return listener.normalized();
            }
        }
        throw new AssertionError("missing SignalListener fixture " + id);
    }

    private static boolean signalListenerExists(Fixture fixture, String id) {
        for (SignalListenerData listener : SignalListenerStore.loadWithStatus(fixture.directory.resolve(SignalListenerStore.FILE_NAME)).file().listeners) {
            if (listener.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static SignalListenerData requireSignalListenerByName(Fixture fixture, String name) {
        for (SignalListenerData listener : SignalListenerStore.loadWithStatus(fixture.directory.resolve(SignalListenerStore.FILE_NAME)).file().listeners) {
            if (listener.name().equals(name)) {
                return listener.normalized();
            }
        }
        throw new AssertionError("missing SignalListener fixture name " + name);
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
        WebAdminChannelMetadataService channelMetadataService = new WebAdminChannelMetadataService(permission, security, editLockService, directory.resolve("web_admin_channel_metadata.json"));
        WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService = new WebAdminSignalListenerBasicConfigService(permission, security, editLockService, directory.resolve(SignalListenerStore.FILE_NAME));
        WebAdminSignalListenerActionsService signalListenerActionsService = new WebAdminSignalListenerActionsService(permission, security, editLockService, directory.resolve(SignalListenerStore.FILE_NAME));
        WebAdminSignalListenerLifecycleService signalListenerLifecycleService = new WebAdminSignalListenerLifecycleService(permission, security, editLockService, directory.resolve(SignalListenerStore.FILE_NAME));
        WebAdminActionRelayActionsService actionRelayActionsService = new WebAdminActionRelayActionsService(permission, security, editLockService);
        WebAdminRegionControllerService regionControllerService = new WebAdminRegionControllerService(permission, security, editLockService);
        WebAdminLogicChainEditorService service = new WebAdminLogicChainEditorService(
                permission,
                security,
                editLockService,
                logicChainService,
                signalJoinService,
                timerService,
                channelMetadataService,
                signalListenerBasicConfigService,
                signalListenerActionsService,
                signalListenerLifecycleService,
                actionRelayActionsService,
                regionControllerService
        );
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        return new Fixture(directory, service, signalJoinService, timerService, channelMetadataService, signalListenerBasicConfigService, signalListenerActionsService, signalListenerLifecycleService, security, editLockService, editor, session, security.csrfTokenFor(session));
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

    private static void requireNoValidationCode(WebAdminWriteResult result, String code) {
        for (var error : result.validationErrors()) {
            if (code.equals(error.code())) {
                throw new AssertionError("unexpected validation code " + code + " errors=" + result.validationErrors());
            }
        }
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

    private static void requireValidationField(WebAdminWriteResult result, String code, String field) {
        for (var error : result.validationErrors()) {
            if (code.equals(error.code()) && field.equals(error.field())) {
                requireTrue(containsChinese(error.message()), "validation field " + field + " has Chinese message");
                return;
            }
        }
        throw new AssertionError("missing validation field " + field + " for code " + code + " errors=" + result.validationErrors());
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
            Path directory,
            WebAdminLogicChainEditorService service,
            WebAdminSignalJoinService signalJoinService,
            WebAdminTimerService timerService,
            WebAdminChannelMetadataService channelMetadataService,
            WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService,
            WebAdminSignalListenerActionsService signalListenerActionsService,
            WebAdminSignalListenerLifecycleService signalListenerLifecycleService,
            WebAdminWriteSecurityService security,
            WebAdminEditLockService editLockService,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf
    ) {
    }
}
