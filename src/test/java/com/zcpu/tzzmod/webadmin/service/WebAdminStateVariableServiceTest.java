package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableService;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableUpdateRequest;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminStateVariableWriteRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class WebAdminStateVariableServiceTest {
    private WebAdminStateVariableServiceTest() {
    }

    public static void run() throws Exception {
        testMissingStoreReadDoesNotCreateFile();
        testListAndDetailFilters();
        testReadAfterWriteVisibility();
        testBadFileFallbackDoesNotWrite();
        testCreateAndUpdateDefinitionsUseWriteSafety();
        testDefinitionWriteValidationAndConflicts();
    }

    private static void testMissingStoreReadDoesNotCreateFile() throws Exception {
        Path path = Files.createTempDirectory("tzz-state-variable-webadmin-missing").resolve(StateVariableStore.FILE_NAME);
        WebAdminStateVariableService service = new WebAdminStateVariableService(path);
        WebAdminDtos.StateVariableListDto list = service.list(null, viewer(), Map.of());
        requireEquals(0, list.count(), "missing store returns empty list");
        requireFalse(list.storePresent(), "missing store reports absent file");
        requireFalse(Files.exists(path), "read-only state variable API must not create missing store file");
    }

    private static void testListAndDetailFilters() throws Exception {
        Path path = tempPath();
        StateVariableService state = new StateVariableService(path);
        requireTrue(state.set(update(StateVariableScope.GLOBAL, "", "game.ready", StateVariableType.BOOLEAN, "true"), "tester").success(), "global bool write");
        requireTrue(state.set(update(StateVariableScope.PLAYER, "player-a", "mission.score", StateVariableType.INTEGER, "7"), "tester").success(), "player int write");
        requireTrue(state.set(update(StateVariableScope.PLAYER, "player-b", "mission.score", StateVariableType.INTEGER, "3"), "tester").success(), "second player same key write");

        WebAdminStateVariableService service = new WebAdminStateVariableService(path);
        WebAdminDtos.StateVariableListDto all = service.list(null, viewer(), Map.of());
        requireEquals(3, all.count(), "list GLOBAL and PLAYER variables");
        requireEquals(1, all.summary().globalCount(), "summary counts GLOBAL");
        requireEquals(2, all.summary().playerCount(), "summary counts PLAYER");

        WebAdminDtos.StateVariableListDto playerOnly = service.list(null, viewer(), Map.of("scope", "PLAYER"));
        requireEquals(2, playerOnly.count(), "filter by PLAYER scope");
        WebAdminDtos.StateVariableListDto integerOnly = service.list(null, viewer(), Map.of("type", "INTEGER"));
        requireEquals(2, integerOnly.count(), "filter by INTEGER type");
        WebAdminDtos.StateVariableListDto keySearch = service.list(null, viewer(), Map.of("q", "ready"));
        requireEquals(1, keySearch.count(), "search key");
        WebAdminDtos.StateVariableListDto targetSearch = service.list(null, viewer(), Map.of("targetId", "player-b"));
        requireEquals(1, targetSearch.count(), "search targetId");

        WebAdminDtos.StateVariableDetailDto globalDetail = service.detail(null, viewer(), keySearch.variables().get(0).id());
        requireNotNull(globalDetail, "detail GLOBAL variable");
        requireEquals("BOOLEAN", globalDetail.type(), "detail keeps type");
        requireEquals(Boolean.TRUE, globalDetail.value(), "BOOLEAN detail value remains typed");
        requireContains(globalDetail.displayPath(), "global.game.ready", "detail display path");
        requireEquals(null, service.detail(null, viewer(), "missing"), "not found returns null for route Chinese 404");
    }

    private static void testReadAfterWriteVisibility() throws Exception {
        Path path = tempPath();
        StateVariableService state = new StateVariableService(path);
        WebAdminStateVariableService service = new WebAdminStateVariableService(path);

        requireTrue(state.mutate(new StateVariableMutationRequest(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.count",
                StateVariableType.INTEGER,
                "1",
                0,
                true,
                ""
        ), "action").success(), "state action set GLOBAL visible");
        requireEquals(1L, service.list(null, viewer(), Map.of("q", "game.count")).variables().get(0).value(), "GLOBAL set visible in list");

        requireTrue(state.mutate(new StateVariableMutationRequest(
                StateVariableMutationOperation.INCREMENT_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.count",
                StateVariableType.INTEGER,
                "",
                4,
                false,
                ""
        ), "action").success(), "state action increment GLOBAL visible");
        requireEquals(5L, service.list(null, viewer(), Map.of("q", "game.count")).variables().get(0).value(), "GLOBAL increment visible in list");

        requireTrue(state.mutate(new StateVariableMutationRequest(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.PLAYER,
                StateVariableTargetMode.EXPLICIT_TARGET,
                "player-explicit",
                "",
                "player.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                ""
        ), "action").success(), "PLAYER explicit_target write visible");
        requireEquals("player-explicit", service.list(null, viewer(), Map.of("targetId", "player-explicit")).variables().get(0).targetId(), "explicit target grouped by targetId");

        requireTrue(state.mutate(new StateVariableMutationRequest(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.PLAYER,
                StateVariableTargetMode.CONTEXT_PLAYER,
                "",
                "player-context",
                "player.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                ""
        ), "action").success(), "PLAYER context_player write visible");
        requireEquals(2, service.list(null, viewer(), Map.of("q", "player.ready")).count(), "same PLAYER key for two targets shows as two records");

        requireTrue(state.mutate(new StateVariableMutationRequest(
                StateVariableMutationOperation.CLEAR_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "",
                "game.count",
                StateVariableType.INTEGER,
                "",
                0,
                false,
                ""
        ), "action").success(), "clear variable succeeds");
        requireEquals(0, service.list(null, viewer(), Map.of("q", "game.count")).count(), "clear variable removal visible in list");
    }

    private static void testBadFileFallbackDoesNotWrite() throws Exception {
        Path path = tempPath();
        Files.writeString(path, "{broken-json");
        long sizeBefore = Files.size(path);
        WebAdminDtos.StateVariableListDto list = new WebAdminStateVariableService(path).list(null, viewer(), Map.of());
        requireTrue(list.storeDegraded(), "bad file reports degraded");
        requireContains(list.storeMessage(), "状态变量配置文件读取失败", "bad file Chinese fallback");
        requireEquals(sizeBefore, Files.size(path), "bad file fallback does not write or overwrite store");
    }

    private static void testCreateAndUpdateDefinitionsUseWriteSafety() throws Exception {
        Fixture fixture = fixture();
        WebAdminStateVariableWriteRequest request = writeRequest("GLOBAL", "", "game.phase", "STRING", "lobby");
        request.displayName = "游戏阶段";
        request.note = "9.1 create";
        request.expectedFingerprint = WebAdminStateVariableService.CREATE_EXPECTED_FINGERPRINT;

        WebAdminWriteResult viewer = fixture.service.create(null, viewer(), session(viewer()), "127.0.0.1", request, fixture.csrf, true);
        requireFalse(viewer.success(), "VIEWER cannot create state variable definition");

        WebAdminWriteResult badCsrf = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", request, "bad-token", true);
        requireFalse(badCsrf.success(), "create requires valid CSRF");
        requireEquals("csrf_invalid", badCsrf.code(), "invalid CSRF code");

        request.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_STATE_VARIABLE, WebAdminStateVariableService.CREATE_LOCK_TARGET_ID);
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(created.success(), "EDITOR creates GLOBAL state variable definition");
        requireEquals(0, fixture.editLockService.activeLockCount(), "create releases state_variable:new lock after success");

        WebAdminDtos.StateVariableListEntryDto entry = fixture.service.list(null, fixture.editor, Map.of("q", "game.phase")).variables().getFirst();
        requireEquals("游戏阶段", entry.displayName(), "create persists displayName");
        requireEquals("9.1 create", entry.note(), "create persists note");

        WebAdminDtos.StateVariableDetailDto before = fixture.service.detail(null, fixture.editor, entry.id());
        WebAdminStateVariableWriteRequest update = writeRequest("GLOBAL", "", "game.phase", "STRING", "running");
        update.displayName = "游戏阶段";
        update.note = "9.1 update";
        update.expectedFingerprint = before.fingerprint();
        update.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_STATE_VARIABLE, before.id());

        WebAdminWriteResult saved = fixture.service.update(null, fixture.editor, fixture.session, "127.0.0.1", before.id(), update, fixture.csrf, true);
        requireTrue(saved.success(), "EDITOR updates existing state variable definition");
        requireEquals(0, fixture.editLockService.activeLockCount(), "update releases existing state_variable lock after success");

        WebAdminDtos.StateVariableDetailDto after = fixture.service.detail(null, fixture.editor, before.id());
        requireEquals("running", after.valueText(), "update persists new value");
        requireEquals("9.1 update", after.note(), "update persists note");
    }

    private static void testDefinitionWriteValidationAndConflicts() throws Exception {
        Fixture fixture = fixture();
        WebAdminStateVariableWriteRequest create = writeRequest("PLAYER", "player-a", "score", "INTEGER", "1");
        create.expectedFingerprint = WebAdminStateVariableService.CREATE_EXPECTED_FINGERPRINT;
        create.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_STATE_VARIABLE, WebAdminStateVariableService.CREATE_LOCK_TARGET_ID);
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "fixture player state variable created");
        String id = fixture.service.list(null, fixture.editor, Map.of("targetId", "player-a")).variables().getFirst().id();

        WebAdminStateVariableWriteRequest duplicate = writeRequest("PLAYER", "player-a", "score", "INTEGER", "2");
        duplicate.expectedFingerprint = WebAdminStateVariableService.CREATE_EXPECTED_FINGERPRINT;
        duplicate.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_STATE_VARIABLE, WebAdminStateVariableService.CREATE_LOCK_TARGET_ID);
        WebAdminWriteResult duplicateResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", duplicate, fixture.csrf, true);
        requireFalse(duplicateResult.success(), "duplicate state variable create is rejected");
        requireValidationCode(duplicateResult, "state_variable_duplicate");

        WebAdminDtos.StateVariableDetailDto before = fixture.service.detail(null, fixture.editor, id);
        WebAdminStateVariableWriteRequest renamed = writeRequest("PLAYER", "player-a", "score_renamed", "INTEGER", "3");
        renamed.expectedFingerprint = before.fingerprint();
        renamed.lockId = acquireLock(fixture, WebAdminEditLockService.TARGET_STATE_VARIABLE, id);
        WebAdminWriteResult renamedResult = fixture.service.update(null, fixture.editor, fixture.session, "127.0.0.1", id, renamed, fixture.csrf, true);
        requireFalse(renamedResult.success(), "existing state variable identity is immutable");
        requireValidationCode(renamedResult, "state_variable_identity_immutable");
    }

    private static StateVariableUpdateRequest update(StateVariableScope scope, String targetId, String key, StateVariableType type, String value) {
        return new StateVariableUpdateRequest(scope, targetId, key, type, value, "", "", "");
    }

    private static Path tempPath() throws Exception {
        return Files.createTempDirectory("tzz-state-variable-webadmin").resolve(StateVariableStore.FILE_NAME);
    }

    private static Fixture fixture() throws Exception {
        Path path = tempPath();
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminPermissionService permission = new WebAdminPermissionService();
        WebAdminEditLockService editLockService = new WebAdminEditLockService(permission, security, 60_000L);
        WebAdminStateVariableService service = new WebAdminStateVariableService(permission, security, editLockService, path);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        return new Fixture(path, service, security, editLockService, editor, session, security.csrfTokenFor(session));
    }

    private static WebAdminStateVariableWriteRequest writeRequest(String scope, String targetId, String key, String type, String value) {
        WebAdminStateVariableWriteRequest request = new WebAdminStateVariableWriteRequest();
        request.scope = scope;
        request.targetId = targetId;
        request.key = key;
        request.type = type;
        request.value = value;
        return request;
    }

    private static String acquireLock(Fixture fixture, String targetType, String targetId) {
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = targetType;
        request.targetId = targetId;
        WebAdminWriteResult result = fixture.editLockService.acquire(fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "acquire lock " + targetType + ":" + targetId);
        return lockId(result);
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

    private static WebAdminUser viewer() {
        return user(WebAdminRole.VIEWER);
    }

    private static WebAdminUser user(WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = role.id().toLowerCase(Locale.ROOT);
        user.displayName = user.username;
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
    }

    private static void requireValidationCode(WebAdminWriteResult result, String code) {
        for (var error : result.validationErrors()) {
            if (code.equals(error.code())) {
                return;
            }
        }
        throw new AssertionError("missing validation code " + code + " errors=" + result.validationErrors());
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireNotNull(Object value, String message) {
        requireTrue(value != null, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }

    private record Fixture(
            Path path,
            WebAdminStateVariableService service,
            WebAdminWriteSecurityService security,
            WebAdminEditLockService editLockService,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf
    ) {
    }
}
