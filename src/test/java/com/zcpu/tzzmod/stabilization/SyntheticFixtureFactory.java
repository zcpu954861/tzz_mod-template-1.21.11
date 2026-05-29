package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionTargetFilter;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinMode;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

final class SyntheticFixtureFactory {
    static final long SEED = 912012L;

    private SyntheticFixtureFactory() {
    }

    static List<FixtureTier> tiers() {
        return List.of(FixtureTier.SMALL, FixtureTier.MEDIUM, FixtureTier.LARGE, FixtureTier.STRESS);
    }

    static GraphFixture graph(FixtureTier tier) {
        Random random = new Random(SEED + tier.graphNodes);
        List<String> requiredTypes = List.of(
                "channel",
                "join",
                "timer",
                "signal_listener",
                "action_relay",
                "signal_receiver",
                "signal_emitter",
                "virtual_block_device",
                "region_controller",
                "state_variable",
                "condition_group"
        );
        List<GraphNode> nodes = new ArrayList<>();
        for (int index = 0; index < tier.graphNodes; index++) {
            String type = requiredTypes.get(index % requiredTypes.size());
            boolean draft = index % 17 == 0;
            boolean pendingDelete = index % 29 == 0;
            nodes.add(new GraphNode(
                    type + ":" + tier.id + ":" + index,
                    type,
                    "节点 " + index,
                    index % 9,
                    index / 9,
                    "channel." + (index % Math.max(4, tier.graphNodes / 25)),
                    draft,
                    pendingDelete
            ));
        }
        List<GraphEdge> edges = new ArrayList<>();
        for (int index = 0; index < tier.graphEdges; index++) {
            GraphNode from = nodes.get(index % nodes.size());
            GraphNode to = nodes.get((index * 7 + 3 + random.nextInt(Math.max(1, nodes.size()))) % nodes.size());
            edges.add(new GraphEdge(
                    from.id,
                    to.id,
                    index % 5 == 0 ? "vbd_trigger" : "channel_link",
                    "edge-" + tier.id + "-" + index
            ));
        }
        List<String> segments = new ArrayList<>();
        int segmentCount = tier == FixtureTier.SMALL ? 6 : Math.max(30, tier.graphNodes / 20);
        for (int index = 0; index < segmentCount; index++) {
            segments.add("segment." + tier.id + "." + index);
        }
        Map<String, Boolean> coverage = new LinkedHashMap<>();
        for (String type : requiredTypes) {
            coverage.put(type, nodes.stream().anyMatch(node -> node.type.equals(type)));
        }
        coverage.put("draft_nodes", nodes.stream().anyMatch(node -> node.draft));
        coverage.put("pending_delete", nodes.stream().anyMatch(node -> node.pendingDelete));
        coverage.put("vbd_trigger_overlay", edges.stream().anyMatch(edge -> edge.type.equals("vbd_trigger")));
        coverage.put("unsaved_diff_expanded", tier.graphNodes >= FixtureTier.SMALL.graphNodes);
        coverage.put("minimap_cap_candidate", segments.size() > 24 || tier == FixtureTier.SMALL);
        return new GraphFixture(tier, nodes, edges, segments, coverage);
    }

    static List<SignalListenerData> listeners(FixtureTier tier) {
        List<SignalListenerData> listeners = new ArrayList<>(tier.listeners);
        for (int index = 0; index < tier.listeners; index++) {
            listeners.add(new SignalListenerData(
                    "listener-" + tier.id + "-" + index,
                    "监听器 " + index,
                    channel(index),
                    index % 13 != 0,
                    index % 5,
                    index % 19 == 0 ? "condition." + index : "",
                    List.of(messageAction("listener action " + index))
            ).normalized());
        }
        return List.copyOf(listeners);
    }

    static List<SignalDeviceData> signalDevices(FixtureTier tier) {
        List<SignalDeviceData> devices = new ArrayList<>(tier.signalDevices);
        for (int index = 0; index < tier.signalDevices; index++) {
            String type = switch (index % 4) {
                case 0 -> SignalDeviceData.TYPE_SIGNAL_EMITTER;
                case 1 -> SignalDeviceData.TYPE_SIGNAL_RECEIVER;
                case 2 -> SignalDeviceData.TYPE_ACTION_RELAY;
                default -> SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE;
            };
            devices.add(new SignalDeviceData(
                    type + ":synthetic-" + index,
                    type,
                    "设备 " + index,
                    "minecraft:overworld",
                    index % 512,
                    64,
                    index / 512,
                    channel(index),
                    index % 11 != 0,
                    5,
                    0,
                    index % 40,
                    index % 7,
                    fixedMillis(index),
                    fixedMillis(index + 1),
                    0L,
                    0L,
                    "",
                    "minecraft:stone",
                    channel(index + 1),
                    "redstone_rising",
                    false,
                    0
            ).normalized());
        }
        return List.copyOf(devices);
    }

    static List<StateVariableRecord> stateVariables(FixtureTier tier) {
        List<StateVariableRecord> records = new ArrayList<>(tier.stateVariables);
        for (int index = 0; index < tier.stateVariables; index++) {
            StateVariableScope scope = index % 5 == 0 ? StateVariableScope.PLAYER : StateVariableScope.GLOBAL;
            records.add(StateVariableRecord.create(
                    scope,
                    scope == StateVariableScope.PLAYER ? "player-" + (index % 100) : "",
                    "key_" + index,
                    index % 3 == 0 ? StateVariableType.INTEGER : StateVariableType.STRING,
                    index % 3 == 0 ? Integer.toString(index) : "value-" + index,
                    "状态变量 " + index,
                    "synthetic",
                    fixedMillis(index),
                    "benchmark",
                    index + 1L
            ));
        }
        return List.copyOf(records);
    }

    static List<SignalJoinDefinition> signalJoins(FixtureTier tier) {
        List<SignalJoinDefinition> joins = new ArrayList<>(tier.conditionGroups);
        for (int index = 0; index < tier.conditionGroups; index++) {
            SignalJoinDefinition join = new SignalJoinDefinition();
            join.id = "join." + tier.id + "." + index;
            join.displayName = "Signal Join " + index;
            join.enabled = index % 17 != 0;
            join.mode = index % 3 == 0 ? SignalJoinMode.ANY_N : SignalJoinMode.ALL;
            join.threshold = 2;
            join.inputChannels = List.of(
                    new SignalJoinInputDefinition(channel(index), "输入 A", "", 1),
                    new SignalJoinInputDefinition(channel(index + 1), "输入 B", "", 1)
            );
            join.outputChannel = channel(index + 2);
            joins.add(join.normalized());
        }
        return List.copyOf(joins);
    }

    static List<TimerDefinition> timers(FixtureTier tier) {
        List<TimerDefinition> timers = new ArrayList<>(tier.timers);
        for (int index = 0; index < tier.timers; index++) {
            TimerDefinition timer = new TimerDefinition();
            timer.id = "timer." + tier.id + "." + index;
            timer.displayName = "Timer " + index;
            timer.mode = switch (index % 3) {
                case 0 -> TimerMode.DELAY;
                case 1 -> TimerMode.COUNTDOWN;
                default -> TimerMode.REPEAT;
            };
            timer.durationTicks = 40L + (index % 80);
            timer.intervalTicks = 5L + (index % 20);
            timer.maxRuns = timer.mode == TimerMode.REPEAT ? 2 + (index % 4) : 1;
            timer.outputChannel = index % 23 == 0 ? channel(index) : "";
            timers.add(timer.normalized());
        }
        return List.copyOf(timers);
    }

    static List<RegionControllerData> regionControllers(FixtureTier tier) {
        List<RegionControllerData> controllers = new ArrayList<>(tier.regionControllers);
        for (int index = 0; index < tier.regionControllers; index++) {
            controllers.add(new RegionControllerData(
                    "region-controller-" + tier.id + "-" + index,
                    "区域控制器 " + index,
                    "planner-region-" + (index % Math.max(1, tier.regionControllers / 4)),
                    index % 13 != 0,
                    RegionTargetFilter.all(),
                    20 + (index % 200),
                    List.of(messageAction("enter " + index)),
                    List.of(messageAction("exit " + index)),
                    List.of(messageAction("stay " + index))
            ).normalized());
        }
        return List.copyOf(controllers);
    }

    static List<String> plannerRegionIds(FixtureTier tier) {
        int count = Math.max(16, tier.regionControllers / 2);
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add("planner-region-" + index);
        }
        return List.copyOf(ids);
    }

    static VbdRuntimeFixture vbdRuntimeFixture(FixtureTier tier, boolean allChunksUnloaded) {
        int slots = tier == FixtureTier.STRESS ? 54 : 27;
        int conditions = switch (tier) {
            case SMALL -> 4;
            case MEDIUM -> 12;
            case LARGE -> 24;
            case STRESS -> 48;
        };
        List<VbdRuntimeConfig> configs = new ArrayList<>(tier.vbdConfigs);
        for (int index = 0; index < tier.vbdConfigs; index++) {
            boolean loaded = !allChunksUnloaded && index % 7 != 0;
            boolean intervalEligible = index % 3 != 0;
            boolean cooldownReady = index % 5 != 0;
            configs.add(new VbdRuntimeConfig(
                    "vbd-" + tier.id + "-" + index,
                    channel(index),
                    loaded,
                    intervalEligible,
                    cooldownReady,
                    slots,
                    conditions
            ));
        }
        return new VbdRuntimeFixture(tier, configs, containerConditions(tier), slots, conditions);
    }

    static List<ContainerItemConditionData> containerConditions(FixtureTier tier) {
        int count = switch (tier) {
            case SMALL -> 8;
            case MEDIUM -> 32;
            case LARGE -> 96;
            case STRESS -> 192;
        };
        List<ContainerItemConditionData> conditions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ContainerItemConditionType type = switch (index % 4) {
                case 0 -> ContainerItemConditionType.SLOT_ITEM;
                case 1 -> ContainerItemConditionType.TOTAL_ITEM;
                case 2 -> ContainerItemConditionType.SLOT_MATCHER;
                default -> ContainerItemConditionType.TOTAL_MATCHER;
            };
            ItemStackMatcherData matcher = type == ContainerItemConditionType.SLOT_MATCHER
                    || type == ContainerItemConditionType.TOTAL_MATCHER
                    ? matcher("minecraft:diamond", 1 + (index % 32))
                    : ItemStackMatcherData.empty();
            conditions.add(new ContainerItemConditionData(
                    "container-condition-" + tier.id + "-" + index,
                    "容器条件 " + index,
                    index % 17 != 0,
                    type.id(),
                    index % 54,
                    type == ContainerItemConditionType.SLOT_MATCHER || type == ContainerItemConditionType.TOTAL_MATCHER
                            ? matcher.templateItemId()
                            : "minecraft:diamond",
                    ContainerItemCountMode.AT_LEAST.id(),
                    1 + (index % 32),
                    channel(index),
                    channel(index + 1),
                    "",
                    false,
                    0L,
                    0L,
                    0L,
                    "",
                    matcher
            ).normalized());
        }
        return List.copyOf(conditions);
    }

    static WebAdminConditionGroupStore.ConditionGroupFile conditionGroupFile(FixtureTier tier) {
        WebAdminConditionGroupStore.ConditionGroupFile file = new WebAdminConditionGroupStore.ConditionGroupFile();
        for (int index = 0; index < tier.conditionGroups; index++) {
            WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
            entry.id = "condition." + tier.id + "." + index;
            entry.displayName = "条件组 " + index;
            entry.enabled = index % 19 != 0;
            entry.groupDefinition = conditionDefinition(entry.id, index);
            file.groups.put(entry.id, entry);
        }
        return file.normalized();
    }

    static WebAdminSnapshotModels.SnapshotManifest snapshotManifest(FixtureTier tier) {
        WebAdminSnapshotModels.SnapshotManifest manifest = new WebAdminSnapshotModels.SnapshotManifest();
        int count = switch (tier) {
            case SMALL -> 100;
            case MEDIUM -> 1_000;
            case LARGE -> 5_000;
            case STRESS -> 10_000;
        };
        for (int index = 0; index < count; index++) {
            WebAdminSnapshotModels.SnapshotRecord record = new WebAdminSnapshotModels.SnapshotRecord();
            record.snapshotId = "snapshot-" + tier.id + "-" + index;
            record.sequence = index + 1L;
            record.createdAt = Instant.ofEpochMilli(fixedMillis(index)).toString();
            record.createdBy = "benchmark";
            record.kind = index % 5 == 0 ? "manual" : "auto";
            record.title = "性能基线 " + index;
            record.packageFingerprint = "fp-" + index;
            manifest.records.add(record);
        }
        manifest.manifestFingerprint = WebAdminSnapshotStore.fingerprintManifest(manifest);
        return manifest.normalized();
    }

    static String snapshotPackageJson(FixtureTier tier) {
        int targetBytes = switch (tier) {
            case SMALL -> 1024 * 1024;
            case MEDIUM -> 5 * 1024 * 1024;
            case LARGE -> 20 * 1024 * 1024;
            case STRESS -> 50 * 1024 * 1024;
        };
        StringBuilder builder = new StringBuilder(targetBytes + 1024);
        builder.append("{\"schemaVersion\":1,\"snapshotId\":\"package-").append(tier.id)
                .append("\",\"resources\":[");
        int index = 0;
        while (builder.length() < targetBytes) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"resourceType\":\"store_file\",\"resourceId\":\"resource-")
                    .append(index)
                    .append("\",\"canonicalJson\":\"{\\\"index\\\":")
                    .append(index)
                    .append(",\\\"payload\\\":\\\"")
                    .append("x".repeat(128))
                    .append("\\\"}\"}");
            index++;
        }
        builder.append("]}");
        return builder.toString();
    }

    static BenchmarkRow benchmarkRow(
            String suite,
            String caseName,
            FixtureTier tier,
            long sizePrimary,
            long sizeSecondary,
            double measuredMs,
            Double previousMeasuredMs,
            String complexity,
            String reason,
            boolean hardFail,
            Map<String, Object> extra
    ) {
        double scale = previousMeasuredMs == null || previousMeasuredMs <= 0.0d ? 0.0d : measuredMs / previousMeasuredMs;
        String risk = hardFail ? "FAIL" : riskLevel(measuredMs, scale, complexity);
        return new BenchmarkRow(
                suite,
                caseName,
                tier.id,
                sizePrimary,
                sizeSecondary,
                measuredMs,
                measuredMs * 3.0d,
                measuredMs * 5.0d,
                measuredMs * 10.0d,
                scale,
                complexity,
                risk,
                reason,
                hardFail,
                extra == null ? Map.of() : Map.copyOf(extra)
        );
    }

    static long measureNanos(Runnable runnable) {
        long started = System.nanoTime();
        runnable.run();
        return Math.max(0L, System.nanoTime() - started);
    }

    static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0d;
    }

    static long utf8Bytes(String value) {
        return value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
    }

    static String channel(int index) {
        return "synthetic.channel." + Math.floorMod(index, 97);
    }

    private static String riskLevel(double measuredMs, double scale, String complexity) {
        String key = complexity == null ? "" : complexity.toLowerCase(Locale.ROOT);
        if (key.contains("o(n^2)") || key.contains("o(n*m)") || scale > 4.0d || measuredMs * 10.0d > 250.0d) {
            return "WARN";
        }
        if (measuredMs * 5.0d > 50.0d) {
            return "WARN";
        }
        return "PASS";
    }

    private static ConditionGroupDefinition conditionDefinition(String id, int index) {
        ConditionNode node = ConditionNode.leaf(
                "node-" + index,
                index % 2 == 0 ? ConditionNodeType.ALWAYS_TRUE : ConditionNodeType.STATE_VARIABLE_EXISTS,
                ConditionNodeConfig.EMPTY
        );
        return ConditionGroupDefinition.of(id, node);
    }

    private static ActionConfig messageAction(String value) {
        return new ActionConfig(ActionType.MESSAGE, value, true, false, 0, false);
    }

    private static ItemStackMatcherData matcher(String itemId, int requiredCount) {
        return new ItemStackMatcherData(
                true,
                itemId,
                Math.max(1, requiredCount),
                ContainerItemCountMode.AT_LEAST.id(),
                Math.max(1, requiredCount),
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                "matcher " + itemId,
                0L,
                0L
        ).normalized();
    }

    private static long fixedMillis(int index) {
        return 1_700_000_000_000L + index * 1000L;
    }

    enum FixtureTier {
        SMALL("small", 20, 30, 100, 100, 100, 1000, 100, 100, 100, 10),
        MEDIUM("medium", 100, 200, 1000, 500, 500, 5000, 500, 1000, 500, 100),
        LARGE("large", 500, 1000, 5000, 1000, 1000, 10000, 1000, 1000, 1000, 500),
        STRESS("stress", 2000, 5000, 10000, 2000, 2000, 25000, 2500, 2048, 2000, 1000);

        final String id;
        final int graphNodes;
        final int graphEdges;
        final int signalDevices;
        final int vbdConfigs;
        final int conditionGroups;
        final int stateVariables;
        final int listeners;
        final int timers;
        final int regionControllers;
        final int players;

        FixtureTier(
                String id,
                int graphNodes,
                int graphEdges,
                int signalDevices,
                int vbdConfigs,
                int conditionGroups,
                int stateVariables,
                int listeners,
                int timers,
                int regionControllers,
                int players
        ) {
            this.id = id;
            this.graphNodes = graphNodes;
            this.graphEdges = graphEdges;
            this.signalDevices = signalDevices;
            this.vbdConfigs = vbdConfigs;
            this.conditionGroups = conditionGroups;
            this.stateVariables = stateVariables;
            this.listeners = listeners;
            this.timers = timers;
            this.regionControllers = regionControllers;
            this.players = players;
        }
    }

    record GraphFixture(
            FixtureTier tier,
            List<GraphNode> nodes,
            List<GraphEdge> edges,
            List<String> segments,
            Map<String, Boolean> coverage
    ) {
        boolean complete() {
            return coverage.values().stream().allMatch(Boolean::booleanValue)
                    && nodes.size() == tier.graphNodes
                    && edges.size() == tier.graphEdges;
        }

        Set<String> relatedIds(String nodeId) {
            Set<String> related = new LinkedHashSet<>();
            for (GraphEdge edge : edges) {
                if (edge.from.equals(nodeId)) {
                    related.add(edge.to);
                } else if (edge.to.equals(nodeId)) {
                    related.add(edge.from);
                }
            }
            return related;
        }

        String selectedNodeId() {
            return nodes.isEmpty() ? "" : nodes.get(Math.min(nodes.size() - 1, nodes.size() / 3)).id;
        }
    }

    record GraphNode(
            String id,
            String type,
            String label,
            int column,
            int row,
            String channel,
            boolean draft,
            boolean pendingDelete
    ) {
    }

    record GraphEdge(String from, String to, String type, String label) {
    }

    record VbdRuntimeFixture(
            FixtureTier tier,
            List<VbdRuntimeConfig> configs,
            List<ContainerItemConditionData> containerConditions,
            int slots,
            int conditionsPerDevice
    ) {
        int loadedCount() {
            int count = 0;
            for (VbdRuntimeConfig config : configs) {
                if (config.chunkLoaded) {
                    count++;
                }
            }
            return count;
        }

        int eligibleLoadedCount() {
            int count = 0;
            for (VbdRuntimeConfig config : configs) {
                if (config.chunkLoaded && config.intervalEligible && config.cooldownReady) {
                    count++;
                }
            }
            return count;
        }

        long totalItemConditionCount() {
            return containerConditions.stream()
                    .filter(condition -> ContainerItemConditionType.TOTAL_ITEM.id().equals(condition.type()))
                    .count();
        }

        long totalMatcherConditionCount() {
            return containerConditions.stream()
                    .filter(condition -> ContainerItemConditionType.TOTAL_MATCHER.id().equals(condition.type()))
                    .count();
        }
    }

    record VbdRuntimeConfig(
            String deviceId,
            String channel,
            boolean chunkLoaded,
            boolean intervalEligible,
            boolean cooldownReady,
            int slots,
            int conditions
    ) {
    }

    record BenchmarkRow(
            String suite,
            String caseName,
            String tier,
            long sizePrimary,
            long sizeSecondary,
            double measuredMs,
            double estimatedX3,
            double estimatedX5,
            double estimatedX10,
            double scaleFactor,
            String complexity,
            String riskLevel,
            String reason,
            boolean hardFail,
            Map<String, Object> extra
    ) {
        String metricValue() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("suite", suite);
            fields.put("case", caseName);
            fields.put("tier", tier);
            fields.put("size_primary", sizePrimary);
            if (sizeSecondary >= 0) {
                fields.put("size_secondary", sizeSecondary);
            }
            fields.put("measured_ms", fmt(measuredMs));
            fields.put("estimated_low_end_ms_x3", fmt(estimatedX3));
            fields.put("estimated_low_end_ms_x5", fmt(estimatedX5));
            fields.put("estimated_very_low_end_ms_x10", fmt(estimatedX10));
            fields.put("scale_factor_from_previous_size", scaleFactor <= 0.0d ? "n/a" : fmt(scaleFactor));
            fields.put("complexity_class_estimate", complexity);
            fields.put("risk_level", riskLevel);
            fields.put("reason", reason);
            fields.put("hard_fail", hardFail);
            fields.putAll(extra);
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(';');
                }
                builder.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return builder.toString();
        }

        private static String fmt(double value) {
            return String.format(Locale.ROOT, "%.3f", value);
        }
    }

    static final class Blackhole {
        private static volatile int sink;

        private Blackhole() {
        }

        static void consume(Object value) {
            sink = 31 * sink + (value == null ? 0 : value.hashCode());
        }

        static void consumeInt(int value) {
            sink = 31 * sink + value;
        }

        static int sink() {
            return sink;
        }
    }

    static <T> T timed(Supplier<T> supplier) {
        return supplier.get();
    }
}
