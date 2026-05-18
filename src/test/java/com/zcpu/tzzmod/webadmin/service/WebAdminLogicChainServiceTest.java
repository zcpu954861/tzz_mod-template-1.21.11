package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore.StateVariableLoadResult;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.util.List;
import java.util.Map;

public final class WebAdminLogicChainServiceTest {
    private WebAdminLogicChainServiceTest() {
    }

    public static void run() {
        WebAdminDtos.LogicChainGraphDto inputGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "in.a",
                List.of(),
                List.of(listener()),
                List.of(),
                List.of(join()),
                List.of(timer()),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        requireNodeType(inputGraph, "condition_gate", "listener list gate visible");
        requireNodeType(inputGraph, "action_gate", "single action gate visible");
        requireNodeType(inputGraph, "state_action", "state action visible");
        WebAdminDtos.LogicChainNodeDto dynamicStateVariable = requireNodeType(inputGraph, "state_variable", "state variable target visible");
        requireEquals("reference", dynamicStateVariable.metadata().get("nodeKind"), "dynamic state variable target is a reference card");
        requireNodeType(inputGraph, "timer_action", "timer action visible");
        requireNodeType(inputGraph, "timer", "timer reference visible");
        requireEquals(0, inputGraph.stats().get("stateVariableCount"), "dynamic state variable reference is not counted as a real state variable");
        WebAdminDtos.LogicChainNodeDto inputJoinNode = requireNodeType(inputGraph, "signal_join", "join input visible");
        requireJoinCurrentInput(inputJoinNode, "in.a");
        requireJoinV2Metadata(inputJoinNode, "in.a", 1);
        requireEdgeType(inputGraph, "gate_guards", "gate guard edge visible");
        requireEdgeType(inputGraph, "state_writes", "state write edge visible");
        requireEdgeType(inputGraph, "action_starts_timer", "timer action edge visible");
        requireEdgeType(inputGraph, "join_input", "join input edge visible");
        requireJoinInputEdge(inputGraph, "channel:in.a", "signal_join:join.alpha", "primary", "solid", "join-primary", false);
        requireJoinInputEdge(inputGraph, "channel:in.b", "signal_join:join.alpha", "related", "dashed", "join-related-dashed", false);
        requireEdge(inputGraph, "signal_join:join.alpha", "channel:out.c", "join_output", "join output converges to output primary");
        requireNodeId(inputGraph, "channel:timer.done", "timer output channel belongs to the same component as timer_start");
        requireComponentStats(inputGraph, "in.a");
        requireNoJoinOutputReference(inputGraph);

        WebAdminDtos.LogicChainGraphDto outputGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "out.c",
                List.of(),
                List.of(listener()),
                List.of(),
                List.of(join()),
                List.of(timer()),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        WebAdminDtos.LogicChainNodeDto joinNode = requireNodeType(outputGraph, "signal_join", "join output visible");
        Object inputSummary = joinNode.metadata().get("inputSummary");
        requireTrue(inputSummary instanceof List<?> list && list.size() == 2, "join output carries all upstream input channels");
        requireJoinInputPortMetadata(joinNode);
        requireJoinV2Metadata(joinNode, "", 2);
        requireEquals(Boolean.TRUE, joinNode.metadata().get("noCrossChannelLongLineMixing"), "join metadata avoids long-line mixing");
        requireEdgeType(outputGraph, "join_output", "join output edge visible");
        requireJoinInputEdge(outputGraph, "channel:in.a", "signal_join:join.alpha", "related", "dashed", "join-related-dashed", false);
        requireJoinInputEdge(outputGraph, "channel:in.b", "signal_join:join.alpha", "related", "dashed", "join-related-dashed", false);
        requireComponentStats(outputGraph, "out.c");

        WebAdminDtos.LogicChainGraphDto timerGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "timer.done",
                List.of(),
                List.of(listener()),
                List.of(),
                List.of(join()),
                List.of(timer()),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        WebAdminDtos.LogicChainNodeDto timerNode = requireNodeType(timerGraph, "timer", "timer output source visible");
        requireEquals("timer", timerNode.metadata().get("graphRole"), "timer node metadata is shared and role-neutral");
        requireEdgeType(timerGraph, "timer_outputs_channel", "timer output edge visible");
        requireTimerGateSummary(timerNode);

        WebAdminDtos.LogicChainGraphDto missingTimerGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "missing.timer",
                List.of(),
                List.of(missingTimerListener()),
                List.of(),
                List.of(),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        WebAdminDtos.LogicChainNodeDto missingTimerNode = requireNodeType(missingTimerGraph, "timer", "missing timer reference visible");
        requireEquals("reference", missingTimerNode.metadata().get("nodeKind"), "missing timer target is a reference card");
        requireEquals(0, missingTimerGraph.stats().get("timerCount"), "missing timer reference is not counted as a real timer");
        requireEquals(0, missingTimerGraph.stats().get("disabledNodeCount"), "missing timer reference is not counted as a disabled real node");
        requireDisplayNameResolverUsesWebAdminMetadata();
        requireComponentAwareJoinTraversalFromAnyRoot();
        requireLargeComponentTruncation();
        requireGraphModelV2Dedupe();
    }

    private static void requireDisplayNameResolverUsesWebAdminMetadata() {
        SignalDeviceData unnamedDevice = new SignalDeviceData(
                "device.alpha",
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                "",
                "minecraft:overworld",
                12,
                64,
                -7,
                "display.channel",
                true,
                5,
                0,
                0,
                0,
                0L,
                0L,
                0L,
                0L,
                "",
                "",
                "",
                "",
                false,
                0
        ).normalized();
        WebAdminDeviceMetadataStore.MetadataFile deviceMetadata = new WebAdminDeviceMetadataStore.MetadataFile();
        WebAdminDeviceMetadataStore.MetadataEntry deviceEntry = new WebAdminDeviceMetadataStore.MetadataEntry();
        deviceEntry.displayName = "玩家踩踏板 A";
        deviceMetadata.devices.put(WebAdminDeviceMetadataStore.metadataKey(unnamedDevice.id(), unnamedDevice.type()), deviceEntry);
        WebAdminChannelMetadataStore.MetadataFile channelMetadata = new WebAdminChannelMetadataStore.MetadataFile();
        WebAdminChannelMetadataStore.MetadataEntry channelEntry = new WebAdminChannelMetadataStore.MetadataEntry();
        channelEntry.displayName = "入口触发频道";
        channelMetadata.channels.put("display.channel", channelEntry);
        WebAdminDtos.LogicChainGraphDto graph = WebAdminLogicChainService.graphForSnapshotForTest(
                "display.channel",
                List.of(unnamedDevice),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3,
                deviceMetadata,
                channelMetadata
        );
        WebAdminDtos.LogicChainNodeDto producer = requireNodeId(graph, "producer:device:device.alpha:channel", "device producer visible");
        requireEquals("玩家踩踏板 A", producer.label(), "device metadata displayName wins over unnamed fallback");
        requireTrue(producer.subtitle().contains("display.channel") && producer.subtitle().contains("device.alpha"), "producer subtitle keeps channel and technical id");
        requireEquals("device_metadata.displayName", producer.metadata().get("displayNameSource"), "producer metadata records display name source");
        WebAdminDtos.LogicChainNodeDto channel = requireNodeId(graph, "channel:display.channel", "channel node visible");
        requireEquals("入口触发频道", channel.label(), "channel metadata displayName is used for channel node");
        requireTrue(channel.subtitle().contains("display.channel"), "channel subtitle keeps raw channel technical id");
        requireEquals(Boolean.TRUE, graph.stats().get("displayNameResolver"), "graph stats expose display name resolver marker");

        WebAdminDtos.LogicChainGraphDto fallbackGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "display.channel",
                List.of(unnamedDevice),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        WebAdminDtos.LogicChainNodeDto fallbackProducer = requireNodeId(fallbackGraph, "producer:device:device.alpha:channel", "fallback device producer visible");
        requireEquals("display.channel", fallbackProducer.label(), "raw channel is used before position or short id fallback");
        requireEquals("channel", fallbackProducer.metadata().get("displayNameSource"), "fallback metadata records raw channel source");
    }

    private static SignalListenerData listener() {
        ActionConfig state = ActionConfig.stateVariable(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.PLAYER,
                StateVariableTargetMode.CONTEXT_PLAYER,
                "",
                "round.active",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                "false",
                "group.state"
        );
        ActionConfig timerStart = ActionConfig.timerStart(
                "timer.alpha",
                TimerTargetMode.GLOBAL,
                "",
                TimerStartPolicy.RESTART,
                "group.timer.start"
        );
        return new SignalListenerData("listener.alpha", "监听器 Alpha", "in.a", true, 0, "group.list", List.of(state, timerStart)).normalized();
    }

    private static SignalJoinDefinition join() {
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = "join.alpha";
        join.displayName = "汇合 Alpha";
        join.inputChannels = List.of(
                new SignalJoinInputDefinition("in.a", "入口 A", "第一路输入", 1),
                new SignalJoinInputDefinition("in.b", "入口 B", "第二路输入", 2)
        );
        join.outputChannel = "out.c";
        return join.normalized();
    }

    private static TimerDefinition timer() {
        TimerDefinition timer = new TimerDefinition();
        timer.id = "timer.alpha";
        timer.displayName = "计时器 Alpha";
        timer.outputChannel = "timer.done";
        timer.durationTicks = 40;
        timer.onCompleteActions = List.of(new ActionConfig(ActionType.SIGNAL, "timer.after", true, false, 0, false, "group.timer.complete"));
        return timer.normalized();
    }

    private static void requireGraphModelV2Dedupe() {
        WebAdminDtos.LogicChainGraphDto graph = WebAdminLogicChainService.graphForSnapshotForTest(
                "out.c",
                List.of(),
                List.of(downstreamListener()),
                List.of(),
                List.of(join("join.alpha", "in.a", "in.b"), join("join.beta", "in.x", "in.y"), join("join.gamma", "in.m", "in.n")),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        requireEquals(1, countNodes(graph, "channel", "channel:out.c", "primary"), "same output channel appears once as primary");
        requireEquals(1, countNodes(graph, "consumer", "consumer:listener:listener.out", "primary"), "same listener downstream appears once");
        requireEquals(3, countNodesByType(graph, "signal_join"), "each join appears once as primary");
        requireEquals(0, countNodeIdsContaining(graph, ":input:"), "join input aliases are not duplicated primary nodes");
        requireEquals(0, countNodeIdsContaining(graph, ":output"), "join output aliases are not duplicated primary nodes");
        requireEdge(graph, "signal_join:join.alpha", "channel:out.c", "join_output", "join alpha converges to output channel");
        requireEdge(graph, "signal_join:join.beta", "channel:out.c", "join_output", "join beta converges to output channel");
        requireEdge(graph, "signal_join:join.gamma", "channel:out.c", "join_output", "join gamma converges to output channel");
        requireReferenceCardsHavePrimaryNodeId(graph);
        requireEdgesHaveVisualModel(graph);
        requireEquals("v2-join-layout", graph.stats().get("graphModelVersion"), "graph stats expose V2 Join layout model");
        requireEquals(Boolean.TRUE, graph.stats().get("edgeDedupeEnabled"), "graph stats expose edge dedupe");
        requireGraphModelV2DedupeFromSharedInput();
    }

    private static void requireGraphModelV2DedupeFromSharedInput() {
        WebAdminDtos.LogicChainGraphDto graph = WebAdminLogicChainService.graphForSnapshotForTest(
                "shared.in",
                List.of(),
                List.of(downstreamListener()),
                List.of(),
                List.of(join("join.alpha", "shared.in", "in.b"), join("join.beta", "shared.in", "in.y"), join("join.gamma", "shared.in", "in.n")),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        requireEquals(1, countNodes(graph, "channel", "channel:out.c", "primary"), "shared input root still emits one output primary channel");
        requireEquals(1, countNodes(graph, "consumer", "consumer:listener:listener.out", "primary"), "shared input root still expands downstream listener once");
        requireEquals(3, countNodesByType(graph, "signal_join"), "shared input root keeps all joined upstream paths visible");
        requireEdge(graph, "signal_join:join.alpha", "channel:out.c", "join_output", "join alpha converges from shared input");
        requireEdge(graph, "signal_join:join.beta", "channel:out.c", "join_output", "join beta converges from shared input");
        requireEdge(graph, "signal_join:join.gamma", "channel:out.c", "join_output", "join gamma converges from shared input");
        requireComponentStats(graph, "shared.in");
    }

    private static void requireComponentAwareJoinTraversalFromAnyRoot() {
        List<SignalListenerData> listeners = List.of(
                upstreamSignalProducer("producer-a", "seed.a", "in.a"),
                upstreamSignalProducer("producer-b", "seed.b", "in.b"),
                downstreamListener()
        );
        for (String root : List.of("in.a", "in.b", "out.c")) {
            WebAdminDtos.LogicChainGraphDto graph = WebAdminLogicChainService.graphForSnapshotForTest(
                    root,
                    List.of(),
                    listeners,
                    List.of(),
                    List.of(join()),
                    List.of(),
                    new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                    true,
                    3
            );
            requireComponentStats(graph, root);
            requireNodeId(graph, "channel:in.a", "component contains Join input A from root " + root);
            requireNodeId(graph, "channel:in.b", "component contains Join input B from root " + root);
            requireNodeId(graph, "channel:out.c", "component contains Join output C from root " + root);
            requireNodeId(graph, "signal_join:join.alpha", "component contains Join node from root " + root);
            requireNodeId(graph, "producer:listener:producer-a:0", "component contains input A upstream signal producer alias from root " + root);
            requireNodeId(graph, "producer:listener:producer-b:0", "component contains input B upstream signal producer alias from root " + root);
            requireNodeId(graph, "consumer:listener:producer-a", "component expands input A producer owner channel from root " + root);
            requireNodeId(graph, "consumer:listener:producer-b", "component expands input B producer owner channel from root " + root);
            requireNodeId(graph, "consumer:listener:listener.out", "component expands shared output downstream once from root " + root);
            requireEquals(1, countNodes(graph, "channel", "channel:out.c", "primary"), "component keeps output channel primary deduped from root " + root);
            requireEquals(1, countNodes(graph, "consumer", "consumer:listener:listener.out", "primary"), "component keeps downstream listener primary deduped from root " + root);
        }
    }

    private static void requireLargeComponentTruncation() {
        SignalJoinDefinition large = new SignalJoinDefinition();
        large.id = "join.large";
        large.displayName = "大型汇合";
        java.util.ArrayList<SignalJoinInputDefinition> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < 110; i++) {
            inputs.add(new SignalJoinInputDefinition("large.in." + i, "", "", 1));
        }
        large.inputChannels = List.copyOf(inputs);
        large.outputChannel = "large.out";
        WebAdminDtos.LogicChainGraphDto graph = WebAdminLogicChainService.graphForSnapshotForTest(
                "large.out",
                List.of(),
                List.of(),
                List.of(),
                List.of(large.normalized()),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        requireEquals(Boolean.TRUE, graph.stats().get("componentTruncated"), "large component is marked truncated");
        Object reason = graph.stats().get("componentTruncationReason");
        requireTrue(reason instanceof String text && text.contains("输入数量超过") && text.contains("展示"), "large component truncation reason is Chinese and specific");
        WebAdminDtos.LogicChainNodeDto joinNode = requireNodeId(graph, "signal_join:join.large", "large join node remains visible");
        Object ports = joinNode.metadata().get("inputPorts");
        requireTrue(ports instanceof List<?> list && list.size() == 64, "large join input ports are capped");
        requireEquals(Boolean.TRUE, joinNode.metadata().get("inputPortsTruncated"), "large join metadata exposes truncated input ports");
        requireEquals(Boolean.FALSE, joinNode.metadata().get("allUpstreamInputChannelsVisible"), "large join does not claim all inputs are visible after cap");
        requireEquals(46, joinNode.metadata().get("omittedInputPortCount"), "large join metadata exposes omitted input count");
        requireTrue(graph.nodes().size() <= 70, "large join graph node materialization stays bounded");

        WebAdminDtos.LogicChainGraphDto focusedInputGraph = WebAdminLogicChainService.graphForSnapshotForTest(
                "large.in.109",
                List.of(),
                List.of(),
                List.of(),
                List.of(large.normalized()),
                List.of(),
                new StateVariableLoadResult(StateVariableSnapshot.empty(), false, "", false),
                true,
                3
        );
        WebAdminDtos.LogicChainNodeDto focusedJoin = requireNodeId(focusedInputGraph, "signal_join:join.large", "large join focused input node remains visible");
        Object focusedPorts = focusedJoin.metadata().get("inputPorts");
        requireTrue(focusedPorts instanceof List<?> list && list.size() == 65, "large join keeps capped inputs plus current focus input");
        requireEquals(110, focusedJoin.metadata().get("totalInputChannelCount"), "large join still reports total input count without showing every row");
        requireEquals(45, focusedJoin.metadata().get("omittedInputPortCount"), "large join omitted count accounts for preserved focus input");
        requireEquals(Boolean.TRUE, focusedJoin.metadata().get("currentRootIsInput"), "large join recognizes focus input beyond cap");
        requireJoinInputEdge(focusedInputGraph, "channel:large.in.109", "signal_join:join.large", "primary", "solid", "join-primary", false);
        requireTrue(focusedInputGraph.nodes().size() <= 70, "large join focused input graph node materialization stays bounded");
    }

    private static SignalListenerData downstreamListener() {
        ActionConfig signal = new ActionConfig(ActionType.SIGNAL, "after.c", true, false, 0, false, "");
        return new SignalListenerData("listener.out", "输出监听器", "out.c", true, 0, "", List.of(signal)).normalized();
    }

    private static SignalListenerData upstreamSignalProducer(String id, String ownerChannel, String outputChannel) {
        ActionConfig signal = new ActionConfig(ActionType.SIGNAL, outputChannel, true, false, 0, false, "");
        return new SignalListenerData(id, "上游 " + id, ownerChannel, true, 0, "", List.of(signal)).normalized();
    }

    private static SignalListenerData missingTimerListener() {
        ActionConfig timerStart = ActionConfig.timerStart(
                "timer.missing",
                TimerTargetMode.GLOBAL,
                "",
                TimerStartPolicy.RESTART,
                ""
        );
        return new SignalListenerData("listener.missing-timer", "缺失计时器监听器", "missing.timer", true, 0, "", List.of(timerStart)).normalized();
    }

    private static SignalJoinDefinition join(String id, String firstInput, String secondInput) {
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = id;
        join.displayName = "汇合 " + id;
        join.inputChannels = List.of(
                new SignalJoinInputDefinition(firstInput, "", "", 1),
                new SignalJoinInputDefinition(secondInput, "", "", 1)
        );
        join.outputChannel = "out.c";
        return join.normalized();
    }

    private static WebAdminDtos.LogicChainNodeDto requireNodeType(WebAdminDtos.LogicChainGraphDto graph, String type, String message) {
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if (type.equals(node.type())) {
                return node;
            }
        }
        throw new AssertionError(message + " missing type=" + type);
    }

    private static void requireEdgeType(WebAdminDtos.LogicChainGraphDto graph, String type, String message) {
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            if (type.equals(edge.type())) {
                return;
            }
        }
        throw new AssertionError(message + " missing edge=" + type);
    }

    private static void requireTimerGateSummary(WebAdminDtos.LogicChainNodeDto timerNode) {
        Object rawBuckets = timerNode.metadata().get("actionBuckets");
        requireTrue(rawBuckets instanceof List<?>, "timer metadata exposes action bucket summaries");
        List<?> buckets = (List<?>) rawBuckets;
        for (Object bucket : buckets) {
            if (bucket instanceof Map<?, ?> row && "complete".equals(String.valueOf(row.get("bucket")))) {
                Object rawGates = row.get("gateSummaries");
                requireTrue(rawGates instanceof List<?> gates && gates.size() == 1, "timer complete bucket exposes action gate summary");
                return;
            }
        }
        throw new AssertionError("timer complete bucket missing");
    }

    private static void requireJoinCurrentInput(WebAdminDtos.LogicChainNodeDto joinNode, String channel) {
        Object rawRows = joinNode.metadata().get("inputSummary");
        requireTrue(rawRows instanceof List<?>, "join input summary exists");
        for (Object raw : (List<?>) rawRows) {
            if (raw instanceof Map<?, ?> row && channel.equals(String.valueOf(row.get("channel")))) {
                requireEquals(Boolean.TRUE, row.get("currentRootInput"), "join current input row is highlighted");
                return;
            }
        }
        throw new AssertionError("join input summary missing current input " + channel);
    }

    private static void requireJoinInputPortMetadata(WebAdminDtos.LogicChainNodeDto joinNode) {
        Object rawPorts = joinNode.metadata().get("joinInputPorts");
        requireTrue(rawPorts instanceof List<?> ports && ports.size() == 2, "join input ports expose full port list");
        for (Object raw : (List<?>) rawPorts) {
            if (raw instanceof Map<?, ?> row && "in.b".equals(String.valueOf(row.get("channel")))) {
                requireEquals("入口 B", row.get("displayName"), "join input port displayName is visible");
                requireEquals("第二路输入", row.get("note"), "join input port note is visible");
                requireEquals(2, row.get("requiredCount"), "join input port requiredCount is visible");
                return;
            }
        }
        throw new AssertionError("join input port metadata missing in.b");
    }

    private static void requireJoinV2Metadata(WebAdminDtos.LogicChainNodeDto joinNode, String primaryInput, int relatedCount) {
        requireEquals("v2-join-layout", joinNode.metadata().get("graphModelVersion"), "join uses V2 graph model");
        requireEquals("no_recursive_downstream_copy", joinNode.metadata().get("joinTraversalPolicy"), "join does not use generic downstream traversal");
        requireTrue(!joinNode.metadata().containsKey("downstreamChannel"), "join V2 metadata does not use generic downstreamChannel");
        requireEquals(primaryInput, joinNode.metadata().get("primaryInput"), "join primary input is explicit");
        requireEquals("channel:out.c", joinNode.metadata().get("downstreamPrimaryNode"), "join downstream primary node is explicit");
        Object rawInputPorts = joinNode.metadata().get("inputPorts");
        requireTrue(rawInputPorts instanceof List<?> ports && ports.size() == 2, "join V2 inputPorts expose full list");
        Object rawRelated = joinNode.metadata().get("relatedInputs");
        requireTrue(rawRelated instanceof List<?> related && related.size() == relatedCount, "join V2 relatedInputs count");
    }

    private static void requireJoinInputEdge(
            WebAdminDtos.LogicChainGraphDto graph,
            String from,
            String to,
            String role,
            String style,
            String visualStyle,
            boolean referenceEdge
    ) {
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            if (from.equals(edge.from()) && to.equals(edge.to()) && "join_input".equals(edge.type())) {
                requireEquals(role, edge.metadata().get("joinInputRole"), "join input edge role");
                requireEquals(style, edge.style(), "join input edge style");
                requireEquals("join", edge.pathGroupId(), "join input edge path group");
                requireEquals(visualStyle, edge.visualStyle(), "join input edge visual style");
                requireEquals(referenceEdge, edge.referenceEdge(), "join input edge reference flag");
                Object portIndex = edge.metadata().get("portIndex");
                requireTrue(portIndex instanceof Number number && number.intValue() >= 0, "join input portIndex is a visible 0-based index");
                return;
            }
        }
        throw new AssertionError("missing join input edge " + from + " -> " + to + " role=" + role);
    }

    private static void requireNoJoinOutputReference(WebAdminDtos.LogicChainGraphDto graph) {
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            if ("join_output".equals(edge.type()) && edge.to().startsWith("reference:channel:")) {
                throw new AssertionError("join output must converge to primary channel, not reference card: " + edge.to());
            }
        }
    }

    private static int countNodesByType(WebAdminDtos.LogicChainGraphDto graph, String type) {
        int count = 0;
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if (type.equals(node.type())) {
                count++;
            }
        }
        return count;
    }

    private static int countNodes(WebAdminDtos.LogicChainGraphDto graph, String type, String id, String nodeKind) {
        int count = 0;
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if (type.equals(node.type()) && id.equals(node.id()) && nodeKind.equals(String.valueOf(node.metadata().get("nodeKind")))) {
                count++;
            }
        }
        return count;
    }

    private static int countNodeIdsContaining(WebAdminDtos.LogicChainGraphDto graph, String token) {
        int count = 0;
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if (node.id().contains(token)) {
                count++;
            }
        }
        return count;
    }

    private static void requireEdge(WebAdminDtos.LogicChainGraphDto graph, String from, String to, String type, String message) {
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            if (from.equals(edge.from()) && to.equals(edge.to()) && type.equals(edge.type())) {
                return;
            }
        }
        throw new AssertionError(message + " missing edge " + from + " -> " + to + " type=" + type);
    }

    private static WebAdminDtos.LogicChainNodeDto requireNodeId(WebAdminDtos.LogicChainGraphDto graph, String id, String message) {
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if (id.equals(node.id())) {
                return node;
            }
        }
        throw new AssertionError(message + " missing node id=" + id);
    }

    private static void requireComponentStats(WebAdminDtos.LogicChainGraphDto graph, String focusChannel) {
        requireEquals("component-aware-connected-subgraph", graph.stats().get("logicChainModel"), "graph exposes component-aware model");
        requireEquals("focus", graph.stats().get("rootChannelRole"), "root channel is a focus marker");
        requireEquals(focusChannel, graph.stats().get("focusChannel"), "graph focus channel is explicit");
        requireEquals(Boolean.TRUE, graph.stats().get("componentView"), "component view flag is explicit");
        Object summary = graph.stats().get("componentSummary");
        requireTrue(summary instanceof Map<?, ?> row && focusChannel.equals(String.valueOf(row.get("focusChannel"))), "component summary exposes focus channel");
    }

    private static void requireReferenceCardsHavePrimaryNodeId(WebAdminDtos.LogicChainGraphDto graph) {
        boolean foundReference = false;
        for (WebAdminDtos.LogicChainNodeDto node : graph.nodes()) {
            if ("reference".equals(String.valueOf(node.metadata().get("nodeKind")))) {
                foundReference = true;
                Object primaryNodeId = node.metadata().get("primaryNodeId");
                requireTrue(primaryNodeId instanceof String text && !text.isBlank(), "reference cards have primaryNodeId");
                Object reason = node.metadata().get("referenceReason");
                requireTrue(reason instanceof String text && !text.isBlank(), "reference cards have referenceReason");
                requireEquals(Boolean.TRUE, node.metadata().get("visualOnly"), "reference cards are visual only");
                requireEquals(Boolean.TRUE, node.metadata().get("nonTraversal"), "reference cards do not participate in traversal");
            }
        }
        requireTrue(foundReference, "graph includes at least one reference card");
    }

    private static void requireEdgesHaveVisualModel(WebAdminDtos.LogicChainGraphDto graph) {
        boolean referenceEdge = false;
        for (WebAdminDtos.LogicChainEdgeDto edge : graph.edges()) {
            requireTrue(edge.pathGroupId() != null && !edge.pathGroupId().isBlank(), "edge pathGroupId exists");
            requireTrue(edge.visualStyle() != null && !edge.visualStyle().isBlank(), "edge visualStyle exists");
            if (edge.referenceEdge()) {
                referenceEdge = true;
            }
        }
        requireTrue(referenceEdge, "reference edge style marker exists");
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
