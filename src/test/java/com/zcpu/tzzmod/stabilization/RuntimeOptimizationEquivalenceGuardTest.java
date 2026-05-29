package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinMode;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class RuntimeOptimizationEquivalenceGuardTest {
    private RuntimeOptimizationEquivalenceGuardTest() {
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        checkSourceMarkers(report);
        checkStateVariableSnapshotLookupEquivalence(report);
        checkSignalListenerChannelIndexEquivalence(report);
        checkSignalJoinChannelIndexEquivalence(report);
        checkSignalJoinCachedLoadInvalidation(report);
        checkSignalJoinCachedLoadBound(report);
        checkPlannerRegionIndexEquivalence(report);
        checkPlannerRegionContainingPublicPath(report);
    }

    private static void checkSourceMarkers(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String stateSnapshot = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/condition/state/StateVariableSnapshot.java");
        String stateStore = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/condition/state/StateVariableStore.java");
        String conditionStore = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminConditionGroupStore.java");
        String conditionGate = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateService.java");
        String listenerStore = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/SignalListenerStore.java");
        String joinStore = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/join/SignalJoinStore.java");
        String joinRuntime = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/join/SignalJoinRuntimeService.java");
        String mapStore = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/map/MapDataStore.java");
        String snapshotService = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java");
        report.requireContains(stateSnapshot, "while (low <= high)", "StateVariableSnapshot must use sorted lookup marker");
        report.requireContains(stateStore, "loadSnapshotWithStatusCached", "StateVariableStore cached load marker");
        report.requireContains(stateStore, "removeEldestEntry", "StateVariableStore cached load LRU bound marker");
        report.requireContains(conditionStore, "loadWithStatusCached", "ConditionGroupStore cached load marker");
        report.requireContains(conditionStore, "removeEldestEntry", "ConditionGroupStore cached load LRU bound marker");
        report.requireContains(conditionGate, "loadWithStatusCached(server)", "ConditionGateService runtime cached condition group load marker");
        report.requireContains(listenerStore, "indexEnabledListenersByChannel", "SignalListenerStore channel index marker");
        report.requireContains(joinStore, "FileFingerprint", "SignalJoinStore cached load fingerprint marker");
        report.requireContains(joinStore, "removeEldestEntry", "SignalJoinStore cached load LRU bound marker");
        report.requireContains(joinRuntime, "loadWithStatusCached", "SignalJoin runtime cached load marker");
        report.requireContains(joinRuntime, "enabledJoinsReferencing(channel)", "SignalJoin runtime channel index marker");
        report.requireContains(mapStore, "putIfAbsent(region.id(), region)", "MapDataStore planner id index first-match marker");
        report.requireContains(mapStore, "boundsMayContain(region, blockX, blockZ)", "MapDataStore bounds prefilter marker");
        report.requireContains(snapshotService, "SignalJoinStore.clearCachedLoad(server)",
                "Snapshot rollback must clear SignalJoin cached load");
        report.requireContains(snapshotService, "WebAdminConditionGroupStore.clearCachedLoad(server)",
                "Snapshot rollback must clear ConditionGroup cached load");
        report.requireContains(snapshotService, "StateVariableStore.clearCachedLoad(server)",
                "Snapshot rollback must clear StateVariable cached load");
    }

    private static void checkStateVariableSnapshotLookupEquivalence(CodeQualityGuardSupport.GuardReport report) {
        List<StateVariableRecord> records = new ArrayList<>();
        for (int index = 9_999; index >= 0; index--) {
            records.add(stateRecord("key_" + index, "value_" + index, index + 1L));
        }
        records.add(stateRecord("key_42", "override", 20_001L));
        StateVariableSnapshot snapshot = new StateVariableSnapshot(records);
        report.metric("phase2.runtime_equivalence.state_snapshot.records", snapshot.records().size());
        for (int key : List.of(0, 1, 42, 999, 9_999)) {
            String name = "key_" + key;
            Optional<StateVariableRecord> indexed = snapshot.get(StateVariableScope.GLOBAL, "", name);
            Optional<StateVariableRecord> linear = snapshot.records().stream()
                    .filter(record -> record.key().equals(name))
                    .findFirst();
            report.require(Objects.equals(indexed.map(StateVariableRecord::id), linear.map(StateVariableRecord::id)),
                    "StateVariableSnapshot indexed get must match sorted records scan for " + name);
        }
        report.require(snapshot.get(StateVariableScope.GLOBAL, "", "key_42")
                        .map(StateVariableRecord::value)
                        .filter("override"::equals)
                        .isPresent(),
                "StateVariableSnapshot duplicate normalization must keep last record for key_42");
        report.require(snapshot.get(StateVariableScope.GLOBAL, "", "missing").isEmpty(),
                "StateVariableSnapshot indexed get must keep missing lookup empty");
        StateVariableRecord playerRecord = StateVariableRecord.create(
                StateVariableScope.PLAYER,
                "player-a",
                " Score ",
                StateVariableType.INTEGER,
                "12",
                "score",
                "",
                30_001L,
                "phase2-guard",
                1L
        );
        StateVariableSnapshot playerSnapshot = new StateVariableSnapshot(List.of(playerRecord));
        report.require(playerSnapshot.get(StateVariableScope.PLAYER, "player-a", "score")
                        .map(StateVariableRecord::value)
                        .filter("12"::equals)
                        .isPresent(),
                "StateVariableSnapshot indexed get must match normalized player-scope key lookup");
        report.require(playerSnapshot.get(StateVariableScope.PLAYER, "player-b", "score").isEmpty(),
                "StateVariableSnapshot indexed get must keep player target isolation");
    }

    @SuppressWarnings("unchecked")
    private static void checkSignalListenerChannelIndexEquivalence(CodeQualityGuardSupport.GuardReport report) throws Exception {
        List<SignalListenerData> listeners = List.of(
                listener("listener-a", "Alpha", true),
                listener("listener-disabled", "alpha", false),
                listener("listener-b", "beta", true),
                listener("listener-c", "ALPHA", true)
        );
        Method method = SignalListenerStore.class.getDeclaredMethod("indexEnabledListenersByChannel", List.class);
        method.setAccessible(true);
        Map<String, List<SignalListenerData>> indexed = (Map<String, List<SignalListenerData>>) method.invoke(null, listeners);
        requireSameIds(report, linearListeners(listeners, "alpha"), indexed.getOrDefault("alpha", List.of()),
                "SignalListener channel index must preserve alpha listener order");
        requireSameIds(report, linearListeners(listeners, "beta"), indexed.getOrDefault("beta", List.of()),
                "SignalListener channel index must preserve beta listener order");
        report.require(indexed.getOrDefault("missing", List.of()).isEmpty(),
                "SignalListener channel index must keep missing channel empty");
    }

    private static void checkSignalJoinChannelIndexEquivalence(CodeQualityGuardSupport.GuardReport report) {
        SignalJoinStore.SignalJoinFile file = new SignalJoinStore.SignalJoinFile();
        file.joins.put("join-disabled", join("join-disabled", false, SignalJoinMode.ALL, "out.disabled", "alpha"));
        file.joins.put("join-all", join("join-all", true, SignalJoinMode.ALL, "out.all", "alpha", "beta"));
        file.joins.put("join-duplicate-input", join("join-duplicate-input", true, SignalJoinMode.ALL, "out.dup", "alpha", "alpha", "beta"));
        file.joins.put("join-invalid", join("join-invalid", true, SignalJoinMode.ALL, "alpha", "alpha", "gamma"));
        file.joins.put("join-any", join("join-any", true, SignalJoinMode.ANY_N, "out.any", "gamma", "alpha"));
        SignalJoinStore.SignalJoinFile normalized = file.normalized();
        requireSameIds(report, linearJoins(normalized.joins.values(), "alpha"), normalized.enabledJoinsReferencing("alpha"),
                "SignalJoin channel index must preserve enabled first-scan order for alpha");
        requireSameIds(report, linearJoins(normalized.joins.values(), "beta"), normalized.enabledJoinsReferencing("beta"),
                "SignalJoin channel index must preserve enabled first-scan order for beta");
        report.require(normalized.enabledJoinsReferencing("missing").isEmpty(),
                "SignalJoin channel index must keep missing channel empty");
        normalized.enabledJoinsReferencing("alpha");
        normalized.joins.put("join-late", join("join-late", true, SignalJoinMode.ALL, "out.late", "alpha", "delta"));
        requireSameIds(report, linearJoins(normalized.joins.values(), "alpha"), normalized.enabledJoinsReferencing("alpha"),
                "SignalJoin channel index must invalidate when joins map mutates after first lookup");
    }

    private static void checkSignalJoinCachedLoadInvalidation(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Path dir = Files.createTempDirectory("tzz-phase2-signal-join-cache");
        Path path = dir.resolve(SignalJoinStore.FILE_NAME);
        SignalJoinStore.SignalJoinFile first = new SignalJoinStore.SignalJoinFile();
        first.joins.put("join-cache", join("join-cache", true, SignalJoinMode.ALL, "out.one", "alpha", "beta"));
        report.require(SignalJoinStore.save(path, first), "SignalJoin cache guard must save first fixture");
        SignalJoinStore.SignalJoinLoadResult loadedFirst = SignalJoinStore.loadWithStatusCached(path);
        report.require(outputChannel(loadedFirst, "join-cache").equals("out.one"),
                "SignalJoin cached load must read first fixture");

        SignalJoinStore.SignalJoinFile second = new SignalJoinStore.SignalJoinFile();
        second.joins.put("join-cache", join("join-cache", true, SignalJoinMode.ALL, "out.two", "alpha", "beta"));
        report.require(SignalJoinStore.save(path, second), "SignalJoin save must invalidate cached load result");
        SignalJoinStore.SignalJoinLoadResult loadedSecond = SignalJoinStore.loadWithStatusCached(path);
        report.require(outputChannel(loadedSecond, "join-cache").equals("out.two"),
                "SignalJoin cached load must refresh after SignalJoinStore.save");

        FileTime beforeExternalEdit = Files.getLastModifiedTime(path);
        SignalJoinStore.loadWithStatusCached(path);
        String oldJson = Files.readString(path);
        String newJson = oldJson.replace("out.two", "out.red");
        report.require(!oldJson.equals(newJson) && oldJson.length() == newJson.length(),
                "SignalJoin external replacement fixture must be same-size and changed");
        Files.writeString(path, newJson);
        Files.setLastModifiedTime(path, beforeExternalEdit);
        SignalJoinStore.SignalJoinLoadResult externalReplacement = SignalJoinStore.loadWithStatusCached(path);
        report.require(outputChannel(externalReplacement, "join-cache").equals("out.red"),
                "SignalJoin cached load must refresh same-size same-mtime external replacement");

        SignalJoinStore.loadWithStatusCached(path);
        Files.writeString(path, "{bad");
        SignalJoinStore.SignalJoinLoadResult corrupt = SignalJoinStore.loadWithStatusCached(path);
        report.require(corrupt.degraded() && corrupt.message().contains("读取失败"),
                "SignalJoin cached load must keep corrupt external edit fail-closed");
    }

    @SuppressWarnings("unchecked")
    private static void checkSignalJoinCachedLoadBound(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Field cacheField = SignalJoinStore.class.getDeclaredField("LOAD_CACHE");
        cacheField.setAccessible(true);
        Map<Path, ?> cache = (Map<Path, ?>) cacheField.get(null);
        cache.clear();
        for (int index = 0; index < 40; index++) {
            Path dir = Files.createTempDirectory("tzz-phase2-signal-join-cache-bound");
            Path path = dir.resolve(SignalJoinStore.FILE_NAME);
            SignalJoinStore.SignalJoinFile file = new SignalJoinStore.SignalJoinFile();
            file.joins.put("join-" + index, join("join-" + index, true, SignalJoinMode.ALL, "out." + index, "alpha"));
            report.require(SignalJoinStore.save(path, file), "SignalJoin cache bound guard must save fixture " + index);
            SignalJoinStore.loadWithStatusCached(path);
        }
        report.metric("phase2.runtime_equivalence.signal_join.cache_entries", cache.size());
        report.require(cache.size() <= 32, "SignalJoin cached load map must stay bounded to 32 entries");
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    private static void checkPlannerRegionIndexEquivalence(CodeQualityGuardSupport.GuardReport report) throws Exception {
        MapDataStore.PlannerRegionData first = plannerRegion("region-a", "A", "minecraft:overworld", 0, 0, 10, 10);
        MapDataStore.PlannerRegionData second = plannerRegion("region-b", "B", "minecraft:overworld", 20, 20, 30, 30);
        MapDataStore.PlannerRegionData duplicate = plannerRegion("region-a", "A2", "minecraft:overworld", 40, 40, 50, 50);
        List<MapDataStore.PlannerRegionData> regions = List.of(first, second, duplicate);
        Method indexMethod = MapDataStore.class.getDeclaredMethod("indexPlannerRegionsById", List.class);
        indexMethod.setAccessible(true);
        Map<String, MapDataStore.PlannerRegionData> indexed =
                (Map<String, MapDataStore.PlannerRegionData>) indexMethod.invoke(null, regions);
        report.require(indexed.get("region-a") == first,
                "MapDataStore planner id index must preserve old first-match semantics for duplicate ids");
        report.require(indexed.get("region-b") == second,
                "MapDataStore planner id index must resolve existing region id");
        Method boundsMethod = MapDataStore.class.getDeclaredMethod("boundsMayContain", MapDataStore.PlannerRegionData.class, int.class, int.class);
        boundsMethod.setAccessible(true);
        for (int[] point : List.of(new int[]{5, 5}, new int[]{11, 5}, new int[]{-1, 0}, new int[]{10, 10})) {
            boolean boundsMayContain = (boolean) boundsMethod.invoke(null, first, point[0], point[1]);
            boolean oldContains = first.containsBlock(point[0], point[1]);
            report.require((boundsMayContain && oldContains) == oldContains,
                    "MapDataStore bounds prefilter must not reject a point contained by exact polygon");
        }
        MapDataStore.PlannerRegionData concave = new MapDataStore.PlannerRegionData(
                "region-concave",
                "Concave",
                "minecraft:overworld",
                List.of(
                        new RegionGeometry.Point(0, 0),
                        new RegionGeometry.Point(6, 0),
                        new RegionGeometry.Point(6, 6),
                        new RegionGeometry.Point(3, 3),
                        new RegionGeometry.Point(0, 6)
                ),
                0xFFFFFFFF
        );
        for (int[] point : List.of(new int[]{1, 1}, new int[]{5, 1}, new int[]{5, 5}, new int[]{3, 4}, new int[]{7, 1})) {
            boolean boundsMayContain = (boolean) boundsMethod.invoke(null, concave, point[0], point[1]);
            boolean exact = concave.containsBlock(point[0], point[1]);
            report.require(!exact || boundsMayContain,
                    "MapDataStore bounds prefilter must not reject concave exact hit at " + point[0] + "," + point[1]);
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkPlannerRegionContainingPublicPath(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Field cacheField = MapDataStore.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        Map<Object, Object> cache = (Map<Object, Object>) cacheField.get(null);
        Class<?> stateClass = Class.forName("com.zcpu.tzzmod.map.MapDataStore$MapState");
        var constructor = stateClass.getDeclaredConstructor(Path.class);
        constructor.setAccessible(true);
        Object state = constructor.newInstance(Files.createTempFile("tzz-phase2-map-state", ".json"));
        Field plannerRegionsField = stateClass.getDeclaredField("plannerRegions");
        plannerRegionsField.setAccessible(true);
        List<MapDataStore.PlannerRegionData> plannerRegions =
                (List<MapDataStore.PlannerRegionData>) plannerRegionsField.get(state);
        MapDataStore.PlannerRegionData ignoredByBounds = plannerRegion("outside", "Outside", "minecraft:overworld", 100, 100, 110, 110);
        MapDataStore.PlannerRegionData wrongDimension = plannerRegion("wrong-dimension", "Wrong", "minecraft:the_nether", 0, 0, 10, 10);
        MapDataStore.PlannerRegionData exactHit = plannerRegion("exact-hit", "Exact", "minecraft:overworld", 0, 0, 10, 10);
        plannerRegions.add(ignoredByBounds);
        plannerRegions.add(wrongDimension);
        plannerRegions.add(exactHit);
        cache.put(null, state);
        try {
            MapDataStore.PlannerRegionData found =
                    MapDataStore.findPlannerRegionContaining(null, "minecraft:overworld", 5.4D, 5.9D);
            report.require(found == exactHit,
                    "MapDataStore public containing lookup must preserve dimension filter, bounds prefilter and exact hit result");
            report.require(MapDataStore.findPlannerRegionContaining(null, "minecraft:overworld", 50D, 50D) == null,
                    "MapDataStore public containing lookup must keep outside points empty");
        } finally {
            cache.remove(null);
        }
    }

    private static String outputChannel(SignalJoinStore.SignalJoinLoadResult loaded, String joinId) {
        SignalJoinDefinition join = loaded.file().joins.get(joinId);
        return join == null ? "" : join.outputChannel;
    }

    private static StateVariableRecord stateRecord(String key, String value, long version) {
        return StateVariableRecord.create(
                StateVariableScope.GLOBAL,
                "",
                key,
                StateVariableType.STRING,
                value,
                key,
                "",
                version,
                "phase2-guard",
                version
        );
    }

    private static SignalListenerData listener(String id, String channel, boolean enabled) {
        return new SignalListenerData(id, id, channel, enabled, 0, "", List.of()).normalized();
    }

    private static List<SignalListenerData> linearListeners(List<SignalListenerData> listeners, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalListenerData> result = new ArrayList<>();
        for (SignalListenerData listener : listeners) {
            if (listener.enabled() && listener.channel().equals(normalizedChannel)) {
                result.add(listener);
            }
        }
        return List.copyOf(result);
    }

    private static SignalJoinDefinition join(String id, boolean enabled, SignalJoinMode mode, String output, String... inputs) {
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = id;
        join.displayName = id;
        join.enabled = enabled;
        join.mode = mode;
        join.outputChannel = output;
        join.inputChannels = java.util.Arrays.stream(inputs)
                .map(channel -> new SignalJoinInputDefinition(channel, "", "", 1))
                .toList();
        join.threshold = mode == SignalJoinMode.ALL ? inputs.length : 2;
        return join.normalized();
    }

    private static List<SignalJoinDefinition> linearJoins(Collection<SignalJoinDefinition> joins, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        List<SignalJoinDefinition> result = new ArrayList<>();
        for (SignalJoinDefinition raw : joins) {
            SignalJoinDefinition join = raw == null ? null : raw.normalized();
            if (join == null || !join.enabled || !join.referencesInput(normalizedChannel)) {
                continue;
            }
            result.add(join);
        }
        return List.copyOf(result);
    }

    private static MapDataStore.PlannerRegionData plannerRegion(
            String id,
            String name,
            String dimensionId,
            int minX,
            int minZ,
            int maxX,
            int maxZ
    ) {
        return new MapDataStore.PlannerRegionData(
                id,
                name,
                dimensionId,
                List.of(
                        new RegionGeometry.Point(minX, minZ),
                        new RegionGeometry.Point(maxX, minZ),
                        new RegionGeometry.Point(maxX, maxZ),
                        new RegionGeometry.Point(minX, maxZ)
                ),
                0xFFFFFFFF
        );
    }

    private static void requireSameIds(
            CodeQualityGuardSupport.GuardReport report,
            List<?> expected,
            List<?> actual,
            String message
    ) {
        report.require(ids(expected).equals(ids(actual)), message + " expected=" + ids(expected) + " actual=" + ids(actual));
    }

    private static List<String> ids(List<?> values) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof SignalListenerData listener) {
                result.add(listener.id());
            } else if (value instanceof SignalJoinDefinition join) {
                result.add(join.id);
            } else {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }
}
