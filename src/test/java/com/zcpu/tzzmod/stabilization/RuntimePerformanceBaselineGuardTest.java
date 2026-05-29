package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.runtime.ConditionGateRequest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionGateHistory;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.scheduler.TimerRuntimeService;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.item.ConsumePlan;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluationResult;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluator;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinRuntimeService;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class RuntimePerformanceBaselineGuardTest {
    private RuntimePerformanceBaselineGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.2 runtime performance baseline guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        report.metric("performance.912.runtime.seed", SyntheticFixtureFactory.SEED);
        checkSourceOrderMarkers(report);
        checkBlankConditionGateDoesNotBuildContext(report);
        checkConfiguredConditionGateMatrix(report);
        checkItemSubmitAllOrNothing(report);
        runListenerFilterBenchmarks(report);
        runSignalDeviceFanOutBenchmarks(report);
        runActionEngineChainBenchmarks(report);
        runSignalJoinBenchmarks(report);
        runStateVariableBenchmarks(report);
        runTimerBenchmarks(report);
        runRegionBenchmarks(report);
        runVbdAndItemSubmitBenchmarks(report);
    }

    private static void checkSourceOrderMarkers(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String signalBridge = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/SignalBridgeServer.java");
        requireOrdered(report, signalBridge, "SignalBridge accepted signal order",
                "SignalReceiverDispatcher.dispatch(event, channel)",
                "ActionRelayDispatcher.dispatch(event, channel)",
                "SignalListenerStore.getEnabledListenersForChannel",
                "recordAcceptedHistory(");
        int listenerLoop = signalBridge.indexOf("for (SignalListenerData listener : listeners)");
        int listenerEnd = signalBridge.indexOf("} finally {", listenerLoop);
        String listenerPath = listenerLoop >= 0 && listenerEnd > listenerLoop
                ? signalBridge.substring(listenerLoop, listenerEnd)
                : "";
        requireOrdered(report, listenerPath, "SignalBridge listener path records history after listener actions",
                "lastResult = executeListenerActions(context, event, listener);",
                "LAST_TRIGGER_TICKS.put(listener.id(), event.gameTime());",
                "recordAcceptedHistory(");
        requireOrdered(report, signalBridge, "SignalJoin observer follows accepted history record",
                "recordHistory(event, channel, listenerCount, executedCount, skippedCooldownCount, skippedEmptyCount, failedCount, depth, resultMessage);",
                "SignalJoinRuntimeService.observeAcceptedSignal(event, channel, depth);");

        String actionEngine = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/action/ActionEngine.java");
        requireOrdered(report, actionEngine, "ActionEngine executeAll order and first failure",
                "for (ActionConfig config : configs)",
                "lastResult = execute(context, config);",
                "if (!lastResult.success())",
                "return lastResult;");

        String vbdDispatcher = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceDispatcher.java");
        requireOrdered(report, vbdDispatcher, "VBD dispatcher unloaded chunk guard",
                "if (!world.isChunkLoaded(pos))",
                "VirtualBlockDeviceSupport.powerState(world, pos)",
                "world.getBlockState(pos)");

        String container = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceContainerHandler.java");
        requireOrdered(report, container, "VBD content scan unloaded chunk guard",
                "if (!world.isChunkLoaded(pos))",
                "long gameTime = world.getTime();",
                "ContainerDeviceSupport.fingerprint(world, pos)");
        report.require(container.contains("private static Supplier<Inventory> containerInventorySupplier")
                        && container.contains("if (!loaded[0])"),
                "Container inventory supplier must remain lazy and cached within one evaluation");

        String timer = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/scheduler/TimerRuntimeService.java");
        report.requireContains(timer, "MAX_DUE_EXECUTIONS_PER_TICK", "Timer due budget marker");
        report.requireContains(timer, "private final Map<String, Map<String, TimerRuntimeInstance>> instances = new LinkedHashMap<>();",
                "Timer runtime outer insertion order must remain LinkedHashMap");
        report.requireContains(timer, "store.instances.computeIfAbsent(timer.id, ignored -> new LinkedHashMap<>())",
                "Timer runtime scope insertion order must remain LinkedHashMap");
        report.requireContains(timer, "new ArrayList<>(instances.entrySet())", "Timer tick preserves snapshot iteration marker");
        report.requireContains(timer, "new ArrayList<>(scopes.entrySet())", "Timer tick preserves scope snapshot iteration marker");
        report.requireContains(timer, "public void tickActual(long gameTime)", "Timer TestRuntime.tickActual benchmark seam");
    }

    private static void checkBlankConditionGateDoesNotBuildContext(CodeQualityGuardSupport.GuardReport report) {
        AtomicInteger contextBuilds = new AtomicInteger();
        ConditionGateResult result = new ConditionGateService().evaluate(null, new ConditionGateRequest(
                "",
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener.synthetic",
                () -> {
                    contextBuilds.incrementAndGet();
                    return null;
                }
        ));
        report.require(result.allowed() && result.skipped(), "Blank condition gate must be allowed and skipped");
        report.require(contextBuilds.get() == 0, "Blank condition gate must not build EvaluationContext; calls=" + contextBuilds.get());
        emit(report, SyntheticFixtureFactory.benchmarkRow(
                "runtime",
                "condition_gate.blank_skip",
                SyntheticFixtureFactory.FixtureTier.SMALL,
                1,
                -1,
                result.durationNanos() / 1_000_000.0d,
                null,
                "O(1)",
                "未配置 conditionGroupId 时直接跳过，不加载 store，不构造 context。",
                false,
                runtimeExtra(false, true, false, "blank-skip", true, "none")
        ));
    }

    private static void checkConfiguredConditionGateMatrix(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = new LinkedHashMap<>();
        groups.put("valid", conditionEntry("valid", true,
                ConditionGroupDefinition.of("valid", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE))));
        groups.put("disabled", conditionEntry("disabled", false,
                ConditionGroupDefinition.of("disabled", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE))));
        groups.put("invalid", conditionEntry("invalid", true,
                ConditionGroupDefinition.of("invalid", ConditionNode.leaf("bad", "unknown_912_condition"))));
        groups.put("incompatible", conditionEntry("incompatible", true,
                ConditionGroupDefinition.of("incompatible", ConditionNode.leaf(
                        "region",
                        ConditionNodeType.REGION_EXISTS,
                        ConditionNodeConfig.of("regionKey", "region")
                ))));
        groups.put("false", conditionEntry("false", true,
                ConditionGroupDefinition.of("false", ConditionNode.leaf("always_false", ConditionNodeType.ALWAYS_FALSE))));
        groups.put("exception", conditionEntry("exception", true,
                ConditionGroupDefinition.of("exception", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE))));

        ConditionGateService service = conditionGateService(groups);
        ConditionGateHistory.clearForTest();
        runConditionGateCase(report, service, "valid", true, "allowed", false);
        runConditionGateCase(report, service, "missing", false, "condition_group_missing", false);
        runConditionGateCase(report, service, "disabled", false, "condition_group_disabled", false);
        runConditionGateCase(report, service, "invalid", false, "condition_group_validation_failed", false);
        runConditionGateCase(report, service, "incompatible", false, "condition_group_incompatible", false);
        runConditionGateCase(report, service, "false", false, "condition_group_not_matched", false);
        runConditionGateCase(report, service, "exception", false, "condition_gate_exception", true);
        report.require(!ConditionGateHistory.snapshot().isEmpty(),
                "Configured condition gate must continue to append history records for non-skipped gate evaluations");
        report.metric("benchmark.runtime_marker.condition_gate.history_records", ConditionGateHistory.snapshot().size());
        ConditionGateHistory.clearForTest();
    }

    private static void runConditionGateCase(
            CodeQualityGuardSupport.GuardReport report,
            ConditionGateService service,
            String groupId,
            boolean expectedAllowed,
            String expectedCode,
            boolean throwContext
    ) {
        AtomicInteger contextBuilds = new AtomicInteger();
        long nanos = SyntheticFixtureFactory.measureNanos(() -> {
            ConditionGateResult result = service.evaluate(null, new ConditionGateRequest(
                    groupId,
                    ConditionRuntimeTargetType.VBD_INTERACTION,
                    "vbd.synthetic",
                    () -> {
                        contextBuilds.incrementAndGet();
                        if (throwContext) {
                            throw new IllegalStateException("synthetic context failure");
                        }
                        return ConditionEvaluationContext.builder()
                                .source("virtual_block_device", "vbd.synthetic")
                                .deviceId("vbd.synthetic")
                                .channel(SyntheticFixtureFactory.channel(1))
                                .gameTime(912012L)
                                .build();
                    }
            ));
            report.require(result.allowed() == expectedAllowed,
                    "Condition gate case " + groupId + " allowed changed: " + result.allowed());
            if (!"allowed".equals(expectedCode)) {
                report.require(expectedCode.equals(result.code()),
                        "Condition gate case " + groupId + " code changed: expected=" + expectedCode + " actual=" + result.code());
            }
            boolean shouldBuildContext = expectedAllowed || "condition_group_not_matched".equals(expectedCode) || "condition_gate_exception".equals(expectedCode);
            report.require((contextBuilds.get() > 0) == shouldBuildContext,
                    "Condition gate case " + groupId + " context build boundary changed; builds=" + contextBuilds.get());
        });
        emit(report, SyntheticFixtureFactory.benchmarkRow(
                "runtime",
                "condition_gate.configured_" + groupId,
                SyntheticFixtureFactory.FixtureTier.SMALL,
                1,
                contextBuilds.get(),
                SyntheticFixtureFactory.nanosToMillis(nanos),
                null,
                throwContext ? "O(1)+exception" : "O(group_nodes)",
                "配置 condition gate 覆盖 valid/missing/disabled/invalid/incompatible/fail-closed/history；timing 只报告，分支语义硬守。",
                false,
                runtimeExtra(false, true, false, "condition-group-id", true, "fail-closed")
        ));
    }

    private static WebAdminConditionGroupStore.ConditionGroupEntry conditionEntry(
            String id,
            boolean enabled,
            ConditionGroupDefinition definition
    ) {
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = id;
        entry.displayName = "条件组 " + id;
        entry.enabled = enabled;
        entry.groupDefinition = definition;
        return entry;
    }

    private static ConditionGateService conditionGateService(
            Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups
    ) throws Exception {
        Constructor<ConditionGateService> constructor = ConditionGateService.class.getDeclaredConstructor(
                java.util.function.Function.class,
                ConditionEvaluator.class,
                ConditionGroupCompatibilityService.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
                (java.util.function.Function<String, WebAdminConditionGroupStore.ConditionGroupEntry>) groups::get,
                new ConditionEvaluator(),
                new ConditionGroupCompatibilityService()
        );
    }

    private static void checkItemSubmitAllOrNothing(CodeQualityGuardSupport.GuardReport report) {
        AtomicInteger consumed = new AtomicInteger();
        List<ItemSubmitEvaluator.SourceStack> stacks = List.of(
                new ItemSubmitEvaluator.SourceStack("slot-0", "minecraft:diamond", 3, "钻石", consumed::addAndGet),
                new ItemSubmitEvaluator.SourceStack("slot-1", "minecraft:emerald", 1, "绿宝石", consumed::addAndGet)
        );
        List<ItemSubmitRequirementData> successRequirements = List.of(
                requirement("diamond", "minecraft:diamond", 2, 2),
                requirement("emerald", "minecraft:emerald", 1, 1)
        );
        ItemSubmitEvaluationResult success = ItemSubmitEvaluator.evaluate(successRequirements, stacks, true, new ConsumePlan(), null);
        report.require(success.finalSuccess(), "itemSubmit success fixture must produce a staged consume plan");
        report.require(success.stagedConsumePlan().totalCount() == 3, "itemSubmit staged plan count changed");
        report.require(consumed.get() == 0, "itemSubmit staged consume must not mutate before plan.apply()");
        success.stagedConsumePlan().apply();
        report.require(consumed.get() == 3, "itemSubmit consume plan apply must consume staged count exactly");

        List<ItemSubmitRequirementData> lateFailureRequirements = List.of(
                requirement("diamond", "minecraft:diamond", 2, 2),
                requirement("emerald-late-fail", "minecraft:emerald", 99, 1)
        );
        ItemSubmitEvaluationResult lateFailure = ItemSubmitEvaluator.evaluate(lateFailureRequirements, stacks, true, new ConsumePlan(), null);
        report.require(!lateFailure.finalSuccess(), "itemSubmit late requirement failure must fail final result");
        report.require(lateFailure.stagedConsumePlan().isEmpty(), "itemSubmit late requirement failure must return empty staged plan");

        ItemSubmitEvaluationResult consumeFailure = ItemSubmitEvaluator.evaluate(
                List.of(requirement("consume-fail", "minecraft:diamond", 1, 99)),
                stacks,
                true,
                new ConsumePlan(),
                null
        );
        report.require(!consumeFailure.finalSuccess() && consumeFailure.failureReason().contains("item_submit_consume_plan_failed"),
                "itemSubmit consume-plan failure must fail closed with explicit reason");
        report.require(consumeFailure.stagedConsumePlan().isEmpty(), "itemSubmit consume-plan failure must not leak staged entries");
    }

    private static void runListenerFilterBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<SignalListenerData> listeners = SyntheticFixtureFactory.listeners(tier);
            String target = SyntheticFixtureFactory.channel(7);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                int matches = 0;
                for (SignalListenerData listener : listeners) {
                    if (listener.enabled() && listener.channel().equals(target)) {
                        matches++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(matches);
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "signal_listener.channel_filter",
                    tier,
                    listeners.size(),
                    97,
                    ms,
                    previous,
                    "O(listeners)",
                    "Signal Listener 当前按 channel 线性筛选；低配评估先报告增长曲线，Phase 2 再决定是否建索引。",
                    false,
                    runtimeExtra(false, true, false, "none/list-scan", false, "listener cooldown")
            ));
            previous = ms;
        }
    }

    private static void runSignalDeviceFanOutBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previousReceivers = null;
        Double previousRelays = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<SignalDeviceData> devices = SyntheticFixtureFactory.signalDevices(tier);
            String target = SyntheticFixtureFactory.channel(7);
            long receiverNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int matches = 0;
                for (SignalDeviceData device : devices) {
                    SignalDeviceData normalized = device.normalized();
                    if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(normalized.type())
                            && normalized.enabled()
                            && target.equals(normalized.channel())) {
                        matches++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(matches);
            });
            double receiverMs = SyntheticFixtureFactory.nanosToMillis(receiverNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "signal_device.receiver_channel_filter",
                    tier,
                    devices.size(),
                    97,
                    receiverMs,
                    previousReceivers,
                    "O(devices)",
                    "SignalReceiver fan-out 当前以设备快照筛选 channel/type/enabled；Phase 2 索引必须保持顺序和过滤语义。",
                    false,
                    runtimeExtra(false, true, false, "none/device-list-scan", false, "receiver cooldown")
            ));
            previousReceivers = receiverMs;

            long relayNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int matches = 0;
                for (SignalDeviceData device : devices) {
                    SignalDeviceData normalized = device.normalized();
                    if (SignalDeviceData.TYPE_ACTION_RELAY.equals(normalized.type())
                            && normalized.enabled()
                            && target.equals(normalized.channel())) {
                        matches++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(matches);
            });
            double relayMs = SyntheticFixtureFactory.nanosToMillis(relayNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "signal_device.action_relay_channel_filter",
                    tier,
                    devices.size(),
                    97,
                    relayMs,
                    previousRelays,
                    "O(devices)",
                    "ActionRelay fan-out 必须保持 action relay order 和 enabled/channel filter；当前行量化低配 list-scan 风险。",
                    false,
                    runtimeExtra(false, true, false, "none/device-list-scan", false, "relay cooldown")
            ));
            previousRelays = relayMs;
        }
    }

    private static void runActionEngineChainBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            int chainLength = tier.listeners;
            List<ActionConfig> configs = new ArrayList<>(chainLength);
            for (int index = 0; index < chainLength; index++) {
                configs.add(new ActionConfig(ActionType.MESSAGE, "chain action " + index, true, false, 0, false));
            }
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                int usable = 0;
                for (ActionConfig config : configs) {
                    if (config != null && config.isUsable()) {
                        usable++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(usable);
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "action_engine.execute_all_chain_scan",
                    tier,
                    configs.size(),
                    -1,
                    ms,
                    previous,
                    "O(actions)",
                    "ActionEngine.executeAll 的真实执行依赖 Minecraft context；Phase 1 量化链长度扫描并用源序 guard 硬守 stop-on-first-failure。",
                    false,
                    runtimeExtra(false, true, false, "ordered-list", true, "stop-on-first-failure")
            ));
            previous = ms;
        }
    }

    private static void runSignalJoinBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        Double previousStore = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<SignalJoinDefinition> joins = SyntheticFixtureFactory.signalJoins(tier);
            SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                List<String> outputs = runtime.observe(joins, SyntheticFixtureFactory.channel(1), "player-a", 100L);
                SyntheticFixtureFactory.Blackhole.consume(outputs);
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "signal_join.accepted_signal_scan",
                    tier,
                    joins.size(),
                    2,
                    ms,
                    previous,
                    "O(joins)",
                    "SignalJoin accepted-signal 路径会扫描 join 定义；真实 emit 仍保持 report-only，不伪造 Minecraft world。",
                    false,
                    runtimeExtra(false, true, false, "none/list-scan", false, "MAX_SIGNAL_DEPTH")
            ));
            previous = ms;

            try {
                Path path = CodeQualityGuardSupport.projectRoot()
                        .resolve("build/tmp/phase912-runtime-guard/signal-joins-" + tier.id + "-" + System.nanoTime())
                        .resolve(SignalJoinStore.FILE_NAME);
                SignalJoinStore.SignalJoinFile file = new SignalJoinStore.SignalJoinFile();
                for (SignalJoinDefinition join : joins) {
                    file.joins.put(join.id, join);
                }
                SignalJoinStore.save(path, file);
                long storeNanos = SyntheticFixtureFactory.measureNanos(() -> {
                    SignalJoinStore.SignalJoinLoadResult loaded = SignalJoinStore.loadWithStatus(path);
                    List<String> outputs = runtime.observe(
                            List.copyOf(loaded.file().joins.values()),
                            SyntheticFixtureFactory.channel(1),
                            "player-a",
                            100L
                    );
                    SyntheticFixtureFactory.Blackhole.consumeInt(loaded.file().joins.size() + outputs.size());
                });
                double storeMs = SyntheticFixtureFactory.nanosToMillis(storeNanos);
                emit(report, SyntheticFixtureFactory.benchmarkRow(
                        "runtime",
                        "signal_join.store_load_and_scan",
                        tier,
                        joins.size(),
                        -1,
                        storeMs,
                        previousStore,
                        "O(file_bytes+joins)",
                        "SignalJoin accepted-signal 真实 store load/scan 路径含 IO；该行显式标记 hot path IO 风险，后续优化不能隐藏顺序语义。",
                        false,
                        runtimeExtra(false, true, true, "none/file-load+list-scan", false, "MAX_SIGNAL_DEPTH")
                ));
                previousStore = storeMs;
            } catch (Exception exception) {
                report.fail("SignalJoin temp-file load benchmark failed: " + exception.getMessage());
            }
        }
    }

    private static void runStateVariableBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<StateVariableRecord> records = SyntheticFixtureFactory.stateVariables(tier);
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                StateVariableSnapshot snapshot = new StateVariableSnapshot(records);
                int hits = 0;
                int sample = Math.min(256, records.size());
                for (int index = 0; index < sample; index++) {
                    if (snapshot.get(StateVariableScope.GLOBAL, "", "key_" + index).isPresent()) {
                        hits++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(snapshot.size() + hits);
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "state_variable.snapshot_sort_and_linear_get",
                    tier,
                    records.size(),
                    Math.min(256, records.size()),
                    ms,
                    previous,
                    "O(n log n)+O(sample*n)",
                    "StateVariableSnapshot 构造会排序，get 当前为线性查找；低配下大规模状态变量先标记为优化候选。",
                    false,
                    runtimeExtra(false, true, false, "none/list-scan", false, "manual mutation boundary")
            ));
            previous = ms;
        }
    }

    private static void runTimerBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previousNotDue = null;
        Double previousDue = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<TimerDefinition> timers = SyntheticFixtureFactory.timers(tier);
            TimerRuntimeService.TestRuntime notDue = TimerRuntimeService.testRuntime();
            for (TimerDefinition timer : timers) {
                timer.durationTicks = 10_000L;
                timer.mode = TimerMode.DELAY;
                notDue.start(timer, "scope-" + timer.id, 0L);
            }
            long notDueNanos = SyntheticFixtureFactory.measureNanos(() -> {
                notDue.tickActual(1L);
                SyntheticFixtureFactory.Blackhole.consumeInt(notDue.activeCount());
            });
            double notDueMs = SyntheticFixtureFactory.nanosToMillis(notDueNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "timer.tick_not_due",
                    tier,
                    timers.size(),
                    0,
                    notDueMs,
                    previousNotDue,
                    "O(active_timers)",
                    "Timer tickActual 当前遍历 active timers；未到期路径不能在低配 VPS 上放大成 tick 抖动。",
                    false,
                    runtimeExtra(true, true, false, "none/active-map-scan", false, "MAX_ACTIVE_TIMERS_PER_SERVER=2048")
            ));
            previousNotDue = notDueMs;

            int dueCount = Math.min(300, timers.size());
            TimerRuntimeService.TestRuntime due = TimerRuntimeService.testRuntime();
            for (int index = 0; index < dueCount; index++) {
                TimerDefinition timer = new TimerDefinition();
                timer.id = "timer.due." + tier.id + "." + index;
                timer.mode = TimerMode.REPEAT;
                timer.intervalTicks = 5L;
                timer.maxRuns = 1;
                due.start(timer, "scope-" + index, 0L);
            }
            long dueNanos = SyntheticFixtureFactory.measureNanos(() -> {
                due.tickActual(5L);
                SyntheticFixtureFactory.Blackhole.consumeInt(due.activeCount());
            });
            double dueMs = SyntheticFixtureFactory.nanosToMillis(dueNanos);
            if (dueCount > TimerRuntimeService.MAX_DUE_EXECUTIONS_PER_TICK) {
                report.require(due.activeCount() > 0, "Timer due budget must defer excess due executions");
            }
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "timer.tick_due_budget",
                    tier,
                    dueCount,
                    TimerRuntimeService.MAX_DUE_EXECUTIONS_PER_TICK,
                    dueMs,
                    previousDue,
                    "O(due_until_budget)",
                    "Timer 到期执行受每 tick 预算限制；超预算必须延后而不是同 tick 全部执行。",
                    false,
                    runtimeExtra(true, true, false, "none/active-map-scan", false, "MAX_DUE_EXECUTIONS_PER_TICK=256")
            ));
            previousDue = dueMs;
        }
    }

    private static void runRegionBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previous = null;
        Double previousPlannerLookup = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            List<RegionControllerData> controllers = SyntheticFixtureFactory.regionControllers(tier);
            List<String> plannerRegionIds = SyntheticFixtureFactory.plannerRegionIds(tier);
            int players = tier.players;
            long nanos = SyntheticFixtureFactory.measureNanos(() -> {
                int transitions = 0;
                for (int player = 0; player < players; player++) {
                    int x = player % 128;
                    int z = player / 128;
                    for (RegionControllerData controller : controllers) {
                        if (!controller.enabled()) {
                            continue;
                        }
                        int bucket = Math.abs(controller.regionId().hashCode() % 128);
                        if (Math.abs(bucket - x) <= 2 && z % 3 == 0) {
                            transitions++;
                        }
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(transitions);
            });
            double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "region_controller.player_controller_scan",
                    tier,
                    controllers.size(),
                    players,
                    ms,
                    previous,
                    "O(players*controllers)",
                    "RegionController tick 风险来自玩家 x 控制器扫描；Phase 1 只量化，不改变 enter/exit/stay 顺序。",
                    false,
                    runtimeExtra(true, true, false, "none/list-scan", false, "10-tick cadence")
            ));
            previous = ms;

            long lookupNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int found = 0;
                int boundsMiss = 0;
                for (int player = 0; player < players; player++) {
                    boolean outsideBounds = player % 4 != 0;
                    for (RegionControllerData controller : controllers) {
                        if (outsideBounds) {
                            boundsMiss++;
                            continue;
                        }
                        String targetRegionId = player % 11 == 0
                                ? "missing-region-" + player
                                : controller.regionId();
                        for (String plannerRegionId : plannerRegionIds) {
                            if (plannerRegionId.equals(targetRegionId)) {
                                found++;
                                break;
                            }
                        }
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(found + boundsMiss);
            });
            double lookupMs = SyntheticFixtureFactory.nanosToMillis(lookupNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "region_controller.player_controller_planner_lookup_scan",
                    tier,
                    controllers.size(),
                    (long) players * plannerRegionIds.size(),
                    lookupMs,
                    previousPlannerLookup,
                    "O(players*controllers*plannerRegions)",
                    "RegionController 真实风险包含 controller regionId 到 planner region 的同步线性查找、missing id 和 bounds miss 分布；Phase 1 只量化。",
                    false,
                    runtimeExtra(true, true, false, "none/region-id-list-scan", false, "10-tick cadence")
            ));
            previousPlannerLookup = lookupMs;
        }
    }

    private static void runVbdAndItemSubmitBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        checkAllUnloadedVbdFixture(report);
        runContainerMatcherBenchmarks(report);
        Double previousVbd = null;
        Double previousDispatcher = null;
        Double previousOpenClose = null;
        Double previousItemSubmit = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            SyntheticFixtureFactory.VbdRuntimeFixture fixture = SyntheticFixtureFactory.vbdRuntimeFixture(tier, false);
            List<SignalDeviceData> devices = SyntheticFixtureFactory.signalDevices(tier);
            long dispatcherNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int vbdEnabled = 0;
                for (SignalDeviceData device : devices) {
                    SignalDeviceData normalized = device.normalized();
                    if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(normalized.type()) && normalized.enabled()) {
                        vbdEnabled++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(vbdEnabled);
            });
            double dispatcherMs = SyntheticFixtureFactory.nanosToMillis(dispatcherNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "vbd_dispatcher.tick_snapshot_filter",
                    tier,
                    devices.size(),
                    fixture.configs().size(),
                    dispatcherMs,
                    previousDispatcher,
                    "O(devices)",
                    "VirtualBlockDeviceDispatcher.tick 入口依赖 VBD snapshot/filter；低配重点看设备总数增长和后续是否可按 trigger/index 收窄。",
                    false,
                    runtimeExtra(true, true, false, "none/device-list-scan", true, "redstone/block-state cooldown")
            ));
            previousDispatcher = dispatcherMs;

            long vbdNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int checked = 0;
                for (SyntheticFixtureFactory.VbdRuntimeConfig config : fixture.configs()) {
                    if (!config.chunkLoaded() || !config.intervalEligible() || !config.cooldownReady()) {
                        continue;
                    }
                    for (int slot = 0; slot < config.slots(); slot++) {
                        for (int condition = 0; condition < config.conditions(); condition++) {
                            checked += (slot + condition + config.deviceId().hashCode()) & 1;
                        }
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(checked);
            });
            double vbdMs = SyntheticFixtureFactory.nanosToMillis(vbdNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "vbd_container_tick_content_proxy",
                    tier,
                    fixture.configs().size(),
                    fixture.slots(),
                    vbdMs,
                    previousVbd,
                    "O(vbd*slots*conditions)",
                    "VBD 内容变化路径必须先过 chunk/interval/cooldown，再检查绑定容器；该行用于低配增长预警。",
                    false,
                    runtimeExtra(true, true, false, "device/list-scan", true, "container interval/cooldown")
            ));
            previousVbd = vbdMs;

            long openCloseNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int sessionsToClose = 0;
                int pendingToOpen = 0;
                for (int index = 0; index < tier.vbdConfigs; index++) {
                    if (index % 5 == 0) {
                        sessionsToClose++;
                    }
                    if (index % 7 == 0) {
                        pendingToOpen++;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(sessionsToClose + pendingToOpen);
            });
            double openCloseMs = SyntheticFixtureFactory.nanosToMillis(openCloseNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "vbd_container_tick_open_close_sessions",
                    tier,
                    tier.vbdConfigs,
                    -1,
                    openCloseMs,
                    previousOpenClose,
                    "O(open_sessions+pending_opens)",
                    "Container open/close session tick 是每 tick 扫描型路径；terminal/cooldown 语义不在 Phase 1 改动。",
                    false,
                    runtimeExtra(true, true, false, "session-id maps", true, "open/close TTL")
            ));
            previousOpenClose = openCloseMs;

            int requirements = switch (tier) {
                case SMALL -> 4;
                case MEDIUM -> 16;
                case LARGE -> 48;
                case STRESS -> 96;
            };
            List<ItemSubmitRequirementData> reqs = new ArrayList<>();
            for (int index = 0; index < requirements; index++) {
                reqs.add(requirement("req-" + index, "minecraft:diamond", 1, 1));
            }
            List<ItemSubmitEvaluator.SourceStack> stacks = new ArrayList<>();
            for (int slot = 0; slot < 36; slot++) {
                stacks.add(new ItemSubmitEvaluator.SourceStack("slot-" + slot, "minecraft:diamond", 64, "slot " + slot, null));
            }
            long itemNanos = SyntheticFixtureFactory.measureNanos(() -> {
                ItemSubmitEvaluationResult result = ItemSubmitEvaluator.evaluate(reqs, stacks, true, new ConsumePlan(), null);
                SyntheticFixtureFactory.Blackhole.consume(result.consumedSummary());
            });
            double itemMs = SyntheticFixtureFactory.nanosToMillis(itemNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "item_submit.requirements_x_inventory",
                    tier,
                    requirements,
                    stacks.size(),
                    itemMs,
                    previousItemSubmit,
                    "O(requirements*slots)",
                    "itemSubmit 必须保持 all-or-nothing staged consume；多 requirement 在低配下关注 requirements x slots 增长。",
                    false,
                    runtimeExtra(false, true, false, "bound inventory only", true, "consume plan all-or-nothing")
            ));
            previousItemSubmit = itemMs;
        }
    }

    private static void checkAllUnloadedVbdFixture(CodeQualityGuardSupport.GuardReport report) {
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            SyntheticFixtureFactory.VbdRuntimeFixture unloaded = SyntheticFixtureFactory.vbdRuntimeFixture(tier, true);
            AtomicInteger blockReads = new AtomicInteger();
            AtomicInteger inventoryReads = new AtomicInteger();
            AtomicInteger fingerprintReads = new AtomicInteger();
            for (SyntheticFixtureFactory.VbdRuntimeConfig config : unloaded.configs()) {
                if (!config.chunkLoaded()) {
                    continue;
                }
                blockReads.incrementAndGet();
                inventoryReads.incrementAndGet();
                fingerprintReads.incrementAndGet();
            }
            report.require(blockReads.get() == 0 && inventoryReads.get() == 0 && fingerprintReads.get() == 0,
                    "All-unloaded VBD fixture must prove zero block/inventory/fingerprint reads for tier=" + tier.id);
            report.metric("benchmark.runtime_marker.vbd_all_unloaded_chunk_guard." + tier.id,
                    "devices=" + unloaded.configs().size()
                            + ";block_reads=" + blockReads.get()
                            + ";inventory_reads=" + inventoryReads.get()
                            + ";fingerprint_reads=" + fingerprintReads.get()
                            + ";hard_guard=true");
        }
    }

    private static void runContainerMatcherBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Double previousTotalItem = null;
        Double previousTotalMatcher = null;
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            SyntheticFixtureFactory.VbdRuntimeFixture fixture = SyntheticFixtureFactory.vbdRuntimeFixture(tier, false);
            List<ContainerItemConditionData> conditions = fixture.containerConditions();
            long totalItemNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int matched = 0;
                for (int slot = 0; slot < fixture.slots(); slot++) {
                    for (ContainerItemConditionData condition : conditions) {
                        if (ContainerItemConditionType.TOTAL_ITEM.id().equals(condition.type())
                                && "minecraft:diamond".equals(condition.itemId())) {
                            matched += slot + condition.count();
                        }
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(matched);
            });
            double totalItemMs = SyntheticFixtureFactory.nanosToMillis(totalItemNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "container_matcher.total_item_proxy",
                    tier,
                    conditions.size(),
                    fixture.slots(),
                    totalItemMs,
                    previousTotalItem,
                    "O(slots+total_item_conditions)",
                    "TOTAL_ITEM 应优先复用每次容器快照的 totalCounts；Phase 1 用 fixture-backed 代理量化 slot/condition 增长。",
                    false,
                    runtimeExtra(true, true, false, "bound-container-snapshot", true, "container interval/cooldown")
            ));
            previousTotalItem = totalItemMs;

            long totalMatcherNanos = SyntheticFixtureFactory.measureNanos(() -> {
                int matched = 0;
                for (ContainerItemConditionData condition : conditions) {
                    if (!ContainerItemConditionType.TOTAL_MATCHER.id().equals(condition.type())) {
                        continue;
                    }
                    for (int slot = 0; slot < fixture.slots(); slot++) {
                        matched += (slot + condition.matcher().requiredCount()) & 1;
                    }
                }
                SyntheticFixtureFactory.Blackhole.consumeInt(matched);
            });
            double totalMatcherMs = SyntheticFixtureFactory.nanosToMillis(totalMatcherNanos);
            emit(report, SyntheticFixtureFactory.benchmarkRow(
                    "runtime",
                    "container_matcher.total_matcher_proxy",
                    tier,
                    conditions.size(),
                    fixture.slots(),
                    totalMatcherMs,
                    previousTotalMatcher,
                    "O(total_matcher_conditions*slots)",
                    "TOTAL_MATCHER 是低配高风险乘法路径；Phase 1 只量化，不改变 ItemStack matcher 语义。",
                    false,
                    runtimeExtra(true, true, false, "bound-container-snapshot", true, "container interval/cooldown")
            ));
            previousTotalMatcher = totalMatcherMs;
        }
    }

    private static ItemSubmitRequirementData requirement(String name, String itemId, int requiredCount, int consumeCount) {
        return new ItemSubmitRequirementData(
                "req-" + name,
                name,
                true,
                matcher(itemId, requiredCount),
                consumeCount,
                false,
                0,
                0L,
                ""
        ).normalized();
    }

    private static ItemStackMatcherData matcher(String itemId, int requiredCount) {
        return new ItemStackMatcherData(
                true,
                itemId,
                requiredCount,
                ContainerItemCountMode.AT_LEAST.id(),
                requiredCount,
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

    private static Map<String, Object> runtimeExtra(
            boolean tickPath,
            boolean mainThread,
            boolean ioOnHotPath,
            String indexState,
            boolean boundObjectOnly,
            String capOrCooldown
    ) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("tick_path", tickPath);
        extra.put("main_thread", mainThread);
        extra.put("io_on_hot_path", ioOnHotPath);
        extra.put("indexed_by_channel_device_region", indexState);
        extra.put("bound_object_only", boundObjectOnly);
        extra.put("cap_or_cooldown", capOrCooldown);
        return extra;
    }

    private static void emit(CodeQualityGuardSupport.GuardReport report, SyntheticFixtureFactory.BenchmarkRow row) {
        report.metric("benchmark." + row.suite() + "." + row.caseName() + "." + row.tier(), row.metricValue());
        if (!"PASS".equals(row.riskLevel())) {
            report.warning("9.1.2 runtime benchmark risk " + row.riskLevel() + ": " + row.caseName()
                    + " tier=" + row.tier() + " reason=" + row.reason());
        }
    }

    private static void requireOrdered(
            CodeQualityGuardSupport.GuardReport report,
            String text,
            String label,
            String... needles
    ) {
        int previous = -1;
        for (String needle : needles) {
            int current = text.indexOf(needle);
            report.require(current >= 0, label + " missing marker `" + needle + "`");
            if (current >= 0 && previous >= 0 && current <= previous) {
                report.fail(label + " changed order near `" + needle + "`");
            }
            previous = current;
        }
    }
}
