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
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class WebAdminStateVariableServiceTest {
    private WebAdminStateVariableServiceTest() {
    }

    public static void run() throws Exception {
        testMissingStoreReadDoesNotCreateFile();
        testListAndDetailFilters();
        testReadAfterWriteVisibility();
        testBadFileFallbackDoesNotWrite();
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

    private static StateVariableUpdateRequest update(StateVariableScope scope, String targetId, String key, StateVariableType type, String value) {
        return new StateVariableUpdateRequest(scope, targetId, key, type, value, "", "", "");
    }

    private static Path tempPath() throws Exception {
        return Files.createTempDirectory("tzz-state-variable-webadmin").resolve(StateVariableStore.FILE_NAME);
    }

    private static WebAdminUser viewer() {
        WebAdminUser user = new WebAdminUser();
        user.username = "viewer";
        user.role = WebAdminRole.VIEWER.id();
        return user;
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
}
