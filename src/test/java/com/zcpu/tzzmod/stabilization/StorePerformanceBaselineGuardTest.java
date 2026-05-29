package com.zcpu.tzzmod.stabilization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateSession;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateSessions;
import com.zcpu.tzzmod.webadmin.draft.WebAdminProtectedDraftRegistry;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateSession;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateSessions;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StorePerformanceBaselineGuardTest {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private StorePerformanceBaselineGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.2 store performance baseline guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        report.metric("performance.912.store.seed", SyntheticFixtureFactory.SEED);
        checkCorruptJsonFallbacks(report);
        checkCachedLoadEquivalence(report);
        checkSessionLifecycleMarkers(report);
        runStateVariableStoreBenchmarks(report);
        runConditionGroupStoreBenchmarks(report);
        runSignalDeviceStoreBenchmarks(report);
        runRegionControllerStoreBenchmarks(report);
        runSnapshotBenchmarks(report);
        runSessionCleanupBenchmarks(report);
    }

    private static void checkCorruptJsonFallbacks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Path root = tempRoot("corrupt");

        Path statePath = root.resolve(StateVariableStore.FILE_NAME);
        Files.writeString(statePath, "{bad json", StandardCharsets.UTF_8);
        StateVariableStore.StateVariableLoadResult stateLoad = StateVariableStore.loadSnapshotWithStatus(statePath);
        report.require(stateLoad.degraded(), "Corrupt state_variables.json must be degraded");
        report.require(Files.readString(statePath).equals("{bad json"), "Corrupt state_variables.json must not be overwritten by fallback load");
        report.require(stateLoad.message().contains("已停止写入"), "State variable corrupt JSON fallback must keep Chinese stop-write message");

        Path conditionPath = root.resolve(WebAdminConditionGroupStore.FILE_NAME);
        Files.writeString(conditionPath, "{bad json", StandardCharsets.UTF_8);
        WebAdminConditionGroupStore.ConditionGroupLoadResult conditionLoad = WebAdminConditionGroupStore.loadWithStatus(conditionPath);
        report.require(conditionLoad.degraded(), "Corrupt condition_groups.json must be degraded");
        report.require(Files.readString(conditionPath).equals("{bad json"), "Corrupt condition_groups.json must not be overwritten by fallback load");
        report.require(conditionLoad.message().contains("条件组配置文件读取失败"), "Condition group corrupt fallback must be Chinese and user-readable");

        Path snapshotRoot = root.resolve("snapshots");
        Files.createDirectories(snapshotRoot);
        Files.writeString(WebAdminSnapshotStore.manifestPath(snapshotRoot), "{bad json", StandardCharsets.UTF_8);
        WebAdminSnapshotStore.ManifestLoadResult manifestLoad = WebAdminSnapshotStore.loadManifest(snapshotRoot);
        report.require(manifestLoad.degraded(), "Corrupt snapshot manifest must be degraded");
        report.require(Files.readString(WebAdminSnapshotStore.manifestPath(snapshotRoot)).equals("{bad json"),
                "Corrupt snapshot manifest must not be overwritten by fallback load");
        report.require(manifestLoad.message().contains("快照 manifest 读取失败"), "Snapshot corrupt fallback must keep Chinese safe message");

        Path missingRoot = tempRoot("missing");
        Path missingState = missingRoot.resolve(StateVariableStore.FILE_NAME);
        StateVariableStore.loadSnapshotWithStatus(missingState);
        report.require(!Files.exists(missingState), "Missing state_variables.json load must not create a file");
        Path missingCondition = missingRoot.resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminConditionGroupStore.loadWithStatus(missingCondition);
        report.require(!Files.exists(missingCondition), "Missing condition_groups.json load must not create a file");
        Path missingSnapshotRoot = missingRoot.resolve("snapshots-missing");
        WebAdminSnapshotStore.loadManifest(missingSnapshotRoot);
        report.require(!Files.exists(WebAdminSnapshotStore.manifestPath(missingSnapshotRoot)),
                "Missing snapshot manifest load must not create a file");
    }

    private static void checkSessionLifecycleMarkers(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String selection = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java");
        String itemSubmit = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/itemsubmit/WebAdminSingleItemSubmitTemplateSessions.java");
        String container = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/container/WebAdminContainerTemplateSessions.java");
        for (String source : List.of(selection, itemSubmit, container)) {
            report.require(source.contains("MAX_TERMINAL_STATUS = 128"), "Session terminal status cap must remain 128");
            report.require(source.contains("while (TERMINAL_ORDER.size() > MAX_TERMINAL_STATUS)"),
                    "Session terminal status cleanup must remain capped");
        }
        report.require(container.contains("nextExpiryMillis") && container.contains("if (now < nextExpiryMillis)"),
                "Container template session cleanup must keep next-expiry short-circuit marker");
        report.require(itemSubmit.contains("nextExpiryMillis") && itemSubmit.contains("if (now < nextExpiryMillis)"),
                "itemSubmit template session cleanup must keep next-expiry short-circuit marker");

        WebAdminProtectedDraftRegistry.clearForTests();
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry terminal = new WebAdminProtectedDraftRegistry.ProtectedDraftEntry(
                "draft-terminal",
                "lock",
                "owner",
                "owner",
                "session",
                "player",
                WebAdminProtectedDraftRegistry.OBJECT_TYPE_VIRTUAL_BLOCK_DEVICE,
                "object",
                "draft",
                "",
                "",
                "",
                0,
                0,
                0,
                "",
                "",
                "",
                Instant.now().toString(),
                Instant.now().plusSeconds(60).toString(),
                WebAdminProtectedDraftRegistry.STATE_CANCELLED,
                Set.of("cancel"),
                Map.of()
        );
        WebAdminProtectedDraftRegistry.registerForTest(terminal);
        WebAdminProtectedDraftRegistry.ProtectedDraftEntry cancelled = WebAdminProtectedDraftRegistry.cancel("draft-terminal");
        report.require(cancelled != null && WebAdminProtectedDraftRegistry.STATE_CANCELLED.equals(cancelled.state()),
                "Terminal protected draft cancel must be idempotent");
        report.require(WebAdminProtectedDraftRegistry.markCommitted("draft-terminal") == null,
                "Terminal protected draft must not be revived by markCommitted");
        WebAdminProtectedDraftRegistry.clearForTests();

        checkActualSessionTerminalReplay(report);
    }

    private static void checkActualSessionTerminalReplay(CodeQualityGuardSupport.GuardReport report) throws Exception {
        clearSessionStaticState(WebAdminContainerTemplateSessions.class);
        clearSessionStaticState(WebAdminSingleItemSubmitTemplateSessions.class);

        Method rememberContainer = WebAdminContainerTemplateSessions.class.getDeclaredMethod(
                "rememberTerminal",
                WebAdminContainerTemplateSession.class,
                String.class,
                Map.class
        );
        rememberContainer.setAccessible(true);
        for (int index = 0; index < 130; index++) {
            WebAdminContainerTemplateSession session = containerSession("container-session-" + index);
            rememberContainer.invoke(null, session, "cancelled", Map.of("index", index));
        }
        Map<String, Object> firstContainer = WebAdminContainerTemplateSessions.status("container-session-0");
        Map<String, Object> lastContainer = WebAdminContainerTemplateSessions.status("container-session-129");
        report.require("not_found".equals(firstContainer.get("status")),
                "Container terminal replay cap must evict oldest status after 128 entries");
        report.require("cancelled".equals(lastContainer.get("status")) && Boolean.FALSE.equals(lastContainer.get("active")),
                "Container terminal replay must return latest terminal inactive status");

        Method rememberItemSubmit = WebAdminSingleItemSubmitTemplateSessions.class.getDeclaredMethod(
                "rememberTerminal",
                WebAdminSingleItemSubmitTemplateSession.class,
                String.class,
                Map.class
        );
        rememberItemSubmit.setAccessible(true);
        for (int index = 0; index < 130; index++) {
            WebAdminSingleItemSubmitTemplateSession session = itemSubmitSession("item-submit-session-" + index);
            rememberItemSubmit.invoke(null, session, "cancelled", Map.of("index", index));
        }
        Map<String, Object> firstItemSubmit = WebAdminSingleItemSubmitTemplateSessions.status("item-submit-session-0");
        Map<String, Object> lastItemSubmit = WebAdminSingleItemSubmitTemplateSessions.status("item-submit-session-129");
        report.require("not_found".equals(firstItemSubmit.get("status")),
                "itemSubmit terminal replay cap must evict oldest status after 128 entries");
        report.require("cancelled".equals(lastItemSubmit.get("status")) && Boolean.FALSE.equals(lastItemSubmit.get("active")),
                "itemSubmit terminal replay must return latest terminal inactive status");

        clearSessionStaticState(WebAdminContainerTemplateSessions.class);
        clearSessionStaticState(WebAdminSingleItemSubmitTemplateSessions.class);
    }

    private static void checkCachedLoadEquivalence(CodeQualityGuardSupport.GuardReport report) throws Exception {
        checkStateVariableCachedLoadEquivalence(report);
        checkConditionGroupCachedLoadEquivalence(report);
        String snapshotService = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java");
        report.requireContains(snapshotService, "WebAdminConditionGroupStore.clearCachedLoad(server)",
                "Snapshot rollback must clear condition group cached load");
        report.requireContains(snapshotService, "StateVariableStore.clearCachedLoad(server)",
                "Snapshot rollback must clear state variable cached load");
    }

    @SuppressWarnings("unchecked")
    private static void checkStateVariableCachedLoadEquivalence(CodeQualityGuardSupport.GuardReport report) throws Exception {
        clearCacheMap(StateVariableStore.class, "SNAPSHOT_CACHE");
        clearCacheMap(StateVariableStore.class, "STATUS_CACHE");
        Path root = tempRoot("state-cache");
        Path path = root.resolve(StateVariableStore.FILE_NAME);

        Path missingStatusPath = root.resolve("missing-status").resolve(StateVariableStore.FILE_NAME);
        StateVariableStore.StateVariableLoadResult missing = StateVariableStore.loadSnapshotWithStatusCached(missingStatusPath);
        report.require(!missing.filePresent() && missing.snapshot().size() == 0,
                "Cached state status load must preserve missing no-create result");
        report.require(!Files.exists(missingStatusPath), "Cached state status missing load must not create file");

        Path missingSnapshotPath = root.resolve("missing-snapshot").resolve(StateVariableStore.FILE_NAME);
        StateVariableSnapshot created = StateVariableStore.loadSnapshotCached(missingSnapshotPath);
        report.require(created.size() == 0 && Files.exists(missingSnapshotPath),
                "Cached raw state snapshot load must preserve legacy missing-file creation");

        StateVariableSnapshot first = new StateVariableSnapshot(SyntheticFixtureFactory.stateVariables(SyntheticFixtureFactory.FixtureTier.SMALL));
        report.require(StateVariableStore.saveSnapshot(path, first), "StateVariable cache guard must save first fixture");
        StateVariableStore.StateVariableLoadResult firstCached = StateVariableStore.loadSnapshotWithStatusCached(path);
        report.require(firstCached.snapshot().size() == first.size(), "Cached state load must match first fixture");

        StateVariableSnapshot second = new StateVariableSnapshot(SyntheticFixtureFactory.stateVariables(SyntheticFixtureFactory.FixtureTier.MEDIUM));
        report.require(StateVariableStore.saveSnapshot(path, second), "StateVariable save must invalidate cached load result");
        StateVariableStore.StateVariableLoadResult secondCached = StateVariableStore.loadSnapshotWithStatusCached(path);
        report.require(secondCached.snapshot().size() == second.size(), "Cached state load must refresh after saveSnapshot");

        java.nio.file.attribute.FileTime cachedStateMtime = Files.getLastModifiedTime(path);
        StateVariableSnapshot external = new StateVariableSnapshot(SyntheticFixtureFactory.stateVariables(SyntheticFixtureFactory.FixtureTier.LARGE));
        Files.writeString(path, stateJson(external), StandardCharsets.UTF_8);
        Files.setLastModifiedTime(path, cachedStateMtime);
        StateVariableStore.StateVariableLoadResult externalCached = StateVariableStore.loadSnapshotWithStatusCached(path);
        report.require(externalCached.snapshot().size() == external.size(),
                "Cached state load must refresh after external file replacement even when mtime is restored");

        Files.writeString(path, "{bad json", StandardCharsets.UTF_8);
        StateVariableStore.StateVariableLoadResult corrupt = StateVariableStore.loadSnapshotWithStatusCached(path);
        report.require(corrupt.degraded() && corrupt.message().contains("已停止写入"),
                "Cached state corrupt load must preserve degraded Chinese message");
        report.require(Files.readString(path).equals("{bad json"), "Cached state corrupt load must not overwrite file");

        Files.writeString(path, stateJson(first), StandardCharsets.UTF_8);
        StateVariableStore.StateVariableLoadResult repaired = StateVariableStore.loadSnapshotWithStatusCached(path);
        report.require(repaired.snapshot().size() == first.size() && !repaired.degraded(),
                "Cached state load must refresh after corrupt file repair");

        Map<Path, ?> statusCache = (Map<Path, ?>) cacheField(StateVariableStore.class, "STATUS_CACHE").get(null);
        statusCache.clear();
        for (int index = 0; index < 40; index++) {
            Path cachePath = root.resolve("bound-" + index).resolve(StateVariableStore.FILE_NAME);
            StateVariableStore.saveSnapshot(cachePath, first);
            StateVariableStore.loadSnapshotWithStatusCached(cachePath);
        }
        report.require(statusCache.size() <= 32, "StateVariable cached load must remain LRU bounded");
    }

    @SuppressWarnings("unchecked")
    private static void checkConditionGroupCachedLoadEquivalence(CodeQualityGuardSupport.GuardReport report) throws Exception {
        clearCacheMap(WebAdminConditionGroupStore.class, "LOAD_CACHE");
        Path root = tempRoot("condition-cache");
        Path path = root.resolve(WebAdminConditionGroupStore.FILE_NAME);

        Path missingPath = root.resolve("missing").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminConditionGroupStore.ConditionGroupLoadResult missing = WebAdminConditionGroupStore.loadWithStatusCached(missingPath);
        report.require(!missing.degraded() && missing.file().groups.isEmpty(), "Cached condition group missing load must return empty file");
        report.require(!Files.exists(missingPath), "Cached condition group missing load must not create file");

        WebAdminConditionGroupStore.ConditionGroupFile first = SyntheticFixtureFactory.conditionGroupFile(SyntheticFixtureFactory.FixtureTier.SMALL);
        report.require(WebAdminConditionGroupStore.save(path, first), "ConditionGroup cache guard must save first fixture");
        WebAdminConditionGroupStore.ConditionGroupLoadResult firstCached = WebAdminConditionGroupStore.loadWithStatusCached(path);
        report.require(firstCached.file().groups.size() == first.groups.size(), "Cached condition group load must match first fixture");

        WebAdminConditionGroupStore.ConditionGroupFile second = SyntheticFixtureFactory.conditionGroupFile(SyntheticFixtureFactory.FixtureTier.MEDIUM);
        report.require(WebAdminConditionGroupStore.save(path, second), "ConditionGroup save must invalidate cached load result");
        WebAdminConditionGroupStore.ConditionGroupLoadResult secondCached = WebAdminConditionGroupStore.loadWithStatusCached(path);
        report.require(secondCached.file().groups.size() == second.groups.size(), "Cached condition group load must refresh after save");

        java.nio.file.attribute.FileTime cachedConditionMtime = Files.getLastModifiedTime(path);
        WebAdminConditionGroupStore.ConditionGroupFile external = SyntheticFixtureFactory.conditionGroupFile(SyntheticFixtureFactory.FixtureTier.LARGE);
        Files.writeString(path, GSON.toJson(external), StandardCharsets.UTF_8);
        Files.setLastModifiedTime(path, cachedConditionMtime);
        WebAdminConditionGroupStore.ConditionGroupLoadResult externalCached = WebAdminConditionGroupStore.loadWithStatusCached(path);
        report.require(externalCached.file().groups.size() == external.groups.size(),
                "Cached condition group load must refresh after external file replacement even when mtime is restored");

        Files.writeString(path, "{bad json", StandardCharsets.UTF_8);
        WebAdminConditionGroupStore.ConditionGroupLoadResult corrupt = WebAdminConditionGroupStore.loadWithStatusCached(path);
        report.require(corrupt.degraded() && corrupt.message().contains("条件组配置文件读取失败"),
                "Cached condition group corrupt load must preserve degraded Chinese message");
        report.require(Files.readString(path).equals("{bad json"), "Cached condition group corrupt load must not overwrite file");

        Files.writeString(path, GSON.toJson(first), StandardCharsets.UTF_8);
        WebAdminConditionGroupStore.ConditionGroupLoadResult repaired = WebAdminConditionGroupStore.loadWithStatusCached(path);
        report.require(repaired.file().groups.size() == first.groups.size() && !repaired.degraded(),
                "Cached condition group load must refresh after corrupt file repair");

        Map<Path, ?> loadCache = (Map<Path, ?>) cacheField(WebAdminConditionGroupStore.class, "LOAD_CACHE").get(null);
        loadCache.clear();
        for (int index = 0; index < 40; index++) {
            Path cachePath = root.resolve("bound-" + index).resolve(WebAdminConditionGroupStore.FILE_NAME);
            WebAdminConditionGroupStore.save(cachePath, first);
            WebAdminConditionGroupStore.loadWithStatusCached(cachePath);
        }
        report.require(loadCache.size() <= 32, "ConditionGroup cached load must remain LRU bounded");
    }

    private static String stateJson(StateVariableSnapshot snapshot) {
        StateVariableStore.DataFile dataFile = new StateVariableStore.DataFile();
        dataFile.version = StateVariableStore.DATA_VERSION;
        dataFile.variables = new java.util.ArrayList<>(snapshot == null ? List.of() : snapshot.records());
        return GSON.toJson(dataFile);
    }

    @SuppressWarnings("unchecked")
    private static void clearCacheMap(Class<?> owner, String fieldName) throws Exception {
        Object value = cacheField(owner, fieldName).get(null);
        if (value instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).clear();
        }
    }

    private static Field cacheField(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static void clearSessionStaticState(Class<?> sessionClass) throws Exception {
        for (String fieldName : List.of("SESSIONS_BY_ID", "ACTIVE_BY_PLAYER", "ACTIVE_BY_DEVICE", "TERMINAL_STATUS", "TERMINAL_ORDER")) {
            try {
                Field field = sessionClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Map<?, ?> map) {
                    ((Map<Object, Object>) map).clear();
                } else if (value instanceof ArrayDeque<?> deque) {
                    ((ArrayDeque<Object>) deque).clear();
                }
            } catch (NoSuchFieldException ignored) {
                // Selection sessions do not share every index; the concrete classes under test do.
            }
        }
        try {
            Field field = sessionClass.getDeclaredField("nextExpiryMillis");
            field.setAccessible(true);
            field.setLong(null, Long.MAX_VALUE);
        } catch (NoSuchFieldException ignored) {
            // Selection sessions do not use the next-expiry short-circuit.
        }
    }

    private static WebAdminContainerTemplateSession containerSession(String sessionId) {
        return new WebAdminContainerTemplateSession(
                sessionId,
                "nonce",
                "vbd-1",
                "VBD 1",
                "minecraft:overworld",
                0,
                64,
                0,
                "minecraft:chest",
                UUID.nameUUIDFromBytes(sessionId.getBytes(StandardCharsets.UTF_8)),
                "Player",
                "lock",
                "fp",
                1L,
                2L,
                null,
                null,
                writeContext("VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE", "vbd-1"),
                List.of(),
                true,
                "capture",
                "edit-lock",
                "CHANNEL",
                "root",
                "draft-node",
                "container_change",
                0
        );
    }

    private static WebAdminSingleItemSubmitTemplateSession itemSubmitSession(String sessionId) {
        return new WebAdminSingleItemSubmitTemplateSession(
                sessionId,
                "nonce",
                "vbd-1",
                "VBD 1",
                "minecraft:overworld",
                0,
                64,
                0,
                "minecraft:chest",
                UUID.nameUUIDFromBytes(sessionId.getBytes(StandardCharsets.UTF_8)),
                "Player",
                "lock",
                "fp",
                1L,
                2L,
                null,
                null,
                writeContext("VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT", "vbd-1"),
                Map.of("template", true),
                true,
                "capture",
                "edit-lock",
                "CHANNEL",
                "root",
                "draft-node",
                "item_submit",
                0
        );
    }

    private static WebAdminWriteContext writeContext(String targetType, String targetId) {
        return new WebAdminWriteContext(
                "benchmark",
                WebAdminRole.OWNER,
                "session",
                "127.0.0.1",
                WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                new WebAdminWriteTarget(targetType, targetId, targetId)
        );
    }

    private static void runStateVariableStoreBenchmarks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Double previous = null;
        Double previousCached = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            Path path = tempRoot("state-" + tier.id).resolve(StateVariableStore.FILE_NAME);
            StateVariableSnapshot snapshot = new StateVariableSnapshot(SyntheticFixtureFactory.stateVariables(tier));
            StateVariableStore.saveSnapshot(path, snapshot);
            long bytes = Files.size(path);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                StateVariableStore.StateVariableLoadResult loaded = StateVariableStore.loadSnapshotWithStatus(path);
                SyntheticFixtureFactory.Blackhole.consumeInt(loaded.snapshot().size());
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "state_variables.cold_load",
                    tier,
                    snapshot.size(),
                    -1,
                    ms,
                    previous,
                    "O(records log records)",
                    "状态变量大 JSON cold load 需要关注低配 VPS IO、Gson 分配和 Snapshot 排序成本。",
                    false,
                    storeExtra(bytes, 1, "per WebAdmin save / action state mutation", "n/a")
            ));
            previous = ms;

            StateVariableStore.loadSnapshotWithStatusCached(path);
            long cachedNanos = SyntheticFixtureFactory.measureNanos(() -> {
                StateVariableStore.StateVariableLoadResult loaded = StateVariableStore.loadSnapshotWithStatusCached(path);
                SyntheticFixtureFactory.Blackhole.consumeInt(loaded.snapshot().size());
            });
            double cachedMs = SyntheticFixtureFactory.nanosToMillis(cachedNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "state_variables.cached_status_load",
                    tier,
                    snapshot.size(),
                    -1,
                    cachedMs,
                    previousCached,
                    "O(bytes hash)",
                    "StateVariable cached load 保留内容指纹刷新，避免重复 Gson parse，但仍读取文件 bytes 以防外部替换。",
                    false,
                    storeExtra(bytes, 0, "per runtime context / WebAdmin status read", "n/a")
            ));
            previousCached = cachedMs;
        }
    }

    private static void runConditionGroupStoreBenchmarks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Double previous = null;
        Double previousCached = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            Path path = tempRoot("condition-" + tier.id).resolve(WebAdminConditionGroupStore.FILE_NAME);
            WebAdminConditionGroupStore.ConditionGroupFile file = SyntheticFixtureFactory.conditionGroupFile(tier);
            WebAdminConditionGroupStore.save(path, file);
            long bytes = Files.size(path);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = WebAdminConditionGroupStore.loadWithStatus(path);
                SyntheticFixtureFactory.Blackhole.consumeInt(loaded.file().groups.size());
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "condition_groups.cold_load",
                    tier,
                    file.groups.size(),
                    -1,
                    ms,
                    previous,
                    "O(groups*nodes)",
                    "ConditionGroup store cold load 是 runtime gate 的外部风险；blank gate 已硬守不读取该 store。",
                    false,
                    storeExtra(bytes, 1, "per WebAdmin save / configured gate load", "n/a")
            ));
            previous = ms;

            WebAdminConditionGroupStore.loadWithStatusCached(path);
            long cachedNanos = SyntheticFixtureFactory.measureNanos(() -> {
                WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = WebAdminConditionGroupStore.loadWithStatusCached(path);
                SyntheticFixtureFactory.Blackhole.consumeInt(loaded.file().groups.size());
            });
            double cachedMs = SyntheticFixtureFactory.nanosToMillis(cachedNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "condition_groups.cached_runtime_load",
                    tier,
                    file.groups.size(),
                    -1,
                    cachedMs,
                    previousCached,
                    "O(bytes hash)",
                    "ConditionGroup cached load 仅用于 runtime gate / replay，保存和 rollback 后显式失效，WebAdmin 写路径仍保留 uncached 校验。",
                    false,
                    storeExtra(bytes, 0, "per configured runtime gate / replay", "n/a")
            ));
            previousCached = cachedMs;
        }
    }

    private static void runSignalDeviceStoreBenchmarks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            SignalDeviceStore.DataFile dataFile = new SignalDeviceStore.DataFile();
            dataFile.devices = SyntheticFixtureFactory.signalDevices(tier);
            Path path = tempRoot("signal-devices-" + tier.id).resolve("signal_devices.json");
            String json = GSON.toJson(dataFile);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            long bytes = Files.size(path);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                try {
                    String raw = Files.readString(path, StandardCharsets.UTF_8);
                    SignalDeviceStore.DataFile loaded = GSON.fromJson(raw, SignalDeviceStore.DataFile.class);
                    int enabled = 0;
                    for (SignalDeviceData device : loaded.devices == null ? List.<SignalDeviceData>of() : loaded.devices) {
                        if (device.normalized().enabled()) {
                            enabled++;
                        }
                    }
                    SyntheticFixtureFactory.Blackhole.consumeInt(enabled);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "signal_devices.file_read_deserialize_normalize",
                    tier,
                    dataFile.devices.size(),
                    -1,
                    ms,
                    previous,
                    "O(devices)",
                    "SignalDeviceStore 仍是全文件 JSON；Phase 1 只量化 deserialize/normalize 和低配写放大风险。",
                    false,
                    storeExtra(bytes, 1, "dirty flush / WebAdmin save", "n/a")
            ));
            previous = ms;
        }
    }

    private static void runRegionControllerStoreBenchmarks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            RegionControllerStore.DataFile dataFile = new RegionControllerStore.DataFile();
            dataFile.controllers = SyntheticFixtureFactory.regionControllers(tier);
            String json = GSON.toJson(dataFile);
            Path path = tempRoot("region-controllers-" + tier.id).resolve("region_controllers.json");
            Files.writeString(path, json, StandardCharsets.UTF_8);
            long bytes = Files.size(path);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                try {
                    String raw = Files.readString(path, StandardCharsets.UTF_8);
                    RegionControllerStore.DataFile loaded = GSON.fromJson(raw, RegionControllerStore.DataFile.class);
                    int actions = 0;
                    for (RegionControllerData controller : loaded.controllers == null ? List.<RegionControllerData>of() : loaded.controllers) {
                        actions += controller.normalized().enterActions().size()
                                + controller.normalized().exitActions().size()
                                + controller.normalized().stayActions().size();
                    }
                    SyntheticFixtureFactory.Blackhole.consumeInt(actions);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "store",
                    "region_controllers.file_read_deserialize_normalize",
                    tier,
                    dataFile.controllers.size(),
                    -1,
                    ms,
                    previous,
                    "O(controllers*actions)",
                    "RegionController store 需要保持 action 顺序；索引/缓存优化必须以后续等价 guard 证明。",
                    false,
                    storeExtra(bytes, 1, "dirty flush / WebAdmin save", "n/a")
            ));
            previous = ms;
        }
    }

    private static void runSnapshotBenchmarks(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Double previousManifest = null;
        Double previousPackage = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            Path snapshotRoot = tempRoot("snapshot-" + tier.id);
            WebAdminSnapshotModels.SnapshotManifest manifest = SyntheticFixtureFactory.snapshotManifest(tier);
            WebAdminSnapshotStore.saveManifest(snapshotRoot, manifest);
            long manifestBytes = Files.size(WebAdminSnapshotStore.manifestPath(snapshotRoot));
            long manifestNanos = SyntheticFixtureFactory.measureNanos(() -> {
                WebAdminSnapshotStore.ManifestLoadResult loaded = WebAdminSnapshotStore.loadManifest(snapshotRoot);
                SyntheticFixtureFactory.Blackhole.consumeInt(loaded.manifest().records.size());
            });
            double manifestMs = SyntheticFixtureFactory.nanosToMillis(manifestNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "snapshot",
                    "snapshot_manifest.load",
                    tier,
                    manifest.records.size(),
                    -1,
                    manifestMs,
                    previousManifest,
                    "O(records)",
                    "Snapshot manifest 是手动 WebAdmin 路径，但大历史会放大低配 IO 和排序成本。",
                    false,
                    storeExtra(manifestBytes, 1, "manual snapshot list/detail", "n/a")
            ));
            previousManifest = manifestMs;

            String packageJson = SyntheticFixtureFactory.snapshotPackageJson(tier);
            Path packagePath = snapshotRoot.resolve("synthetic-package.json");
            Files.writeString(packagePath, packageJson, StandardCharsets.UTF_8);
            long packageBytes = Files.size(packagePath);
            long packageNanos = SyntheticFixtureFactory.measureNanos(() -> {
                try {
                    String raw = Files.readString(packagePath, StandardCharsets.UTF_8);
                    SyntheticFixtureFactory.Blackhole.consumeInt(raw.length());
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });
            double packageMs = SyntheticFixtureFactory.nanosToMillis(packageNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "snapshot",
                    "snapshot_package.raw_read",
                    tier,
                    packageBytes,
                    -1,
                    packageMs,
                    previousPackage,
                    "O(bytes)",
                    "Phase 1 guard 使用受控 package bytes 量化 IO 曲线；50MB 手动目标留在 docs/Phase 6 计划中跟进。",
                    false,
                    storeExtra(packageBytes, 1, "manual snapshot detail/diff", "n/a")
            ));
            previousPackage = packageMs;
        }
    }

    private static void runSessionCleanupBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            int activeCount = switch (tier) {
                case SMALL -> 100;
                case MEDIUM -> 1_000;
                case LARGE -> 5_000;
                case STRESS -> 10_000;
            };
            Map<String, Long> active = new LinkedHashMap<>();
            ArrayDeque<String> terminalOrder = new ArrayDeque<>();
            Map<String, Map<String, Object>> terminal = new LinkedHashMap<>();
            for (int index = 0; index < activeCount; index++) {
                active.put("session-" + index, (long) index);
                terminal.put("terminal-" + index, Map.of("status", "cancelled", "index", index));
                terminalOrder.addLast("terminal-" + index);
            }
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                long expired = 0;
                for (Map.Entry<String, Long> entry : active.entrySet()) {
                    if (entry.getValue() % 3 == 0) {
                        expired++;
                    }
                }
                while (terminalOrder.size() > 128) {
                    terminal.remove(terminalOrder.removeFirst());
                }
                SyntheticFixtureFactory.Blackhole.consumeInt((int) expired + terminal.size());
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "session",
                    "session_registry.cleanup_proxy",
                    tier,
                    activeCount,
                    128,
                    ms,
                    previous,
                    "O(active)+O(terminal_over_cap)",
                    "session/registry cleanup 必须有 terminal cap，避免长期 WebAdmin 会话无界增长。",
                    false,
                    storeExtra(0, 0, "per expireOld / route status", "O(active)+cap(128)")
            ));
            previous = ms;
        }
    }

    private static Map<String, Object> storeExtra(long bytes, int serializationCount, String writeFrequency, String cleanupComplexity) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("bytes", bytes);
        extra.put("serialization_count", serializationCount);
        extra.put("write_frequency", writeFrequency);
        extra.put("cleanup_complexity", cleanupComplexity);
        extra.put("tick_path", false);
        extra.put("main_thread", true);
        extra.put("io_on_hot_path", false);
        extra.put("indexed_by_channel_device_region", "none/file-scan");
        extra.put("bound_object_only", "n/a");
        extra.put("cap_or_cooldown", cleanupComplexity.contains("128") ? "terminal cap 128" : "n/a");
        return extra;
    }

    private static void emit(CodeQualityGuardSupport.GuardReport report, SyntheticFixtureFactory.BenchmarkRow row) {
        report.metric("benchmark." + row.suite() + "." + row.caseName() + "." + row.tier(), row.metricValue());
        if (!"PASS".equals(row.riskLevel())) {
            report.warning("9.1.2 store benchmark risk " + row.riskLevel() + ": " + row.caseName()
                    + " tier=" + row.tier() + " reason=" + row.reason());
        }
    }

    private static Path tempRoot(String suffix) throws Exception {
        Path root = CodeQualityGuardSupport.projectRoot()
                .resolve("build/tmp/phase912-store-guard")
                .resolve(suffix + "-" + System.nanoTime());
        Files.createDirectories(root);
        return root;
    }
}
