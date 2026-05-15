package com.zcpu.tzzmod.condition.regionlogic;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ConditionLogicChainSnapshot(
        String rootChannel,
        String rootNodeId,
        List<ConditionLogicChainNodeSnapshot> nodes,
        List<ConditionLogicChainEdgeSnapshot> edges,
        Set<String> channels,
        boolean hasCycle,
        int maxDepth
) {
    public ConditionLogicChainSnapshot {
        rootChannel = SignalChannel.normalize(rootChannel);
        rootNodeId = safe(rootNodeId);
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        channels = copyChannels(channels);
        hasCycle = hasCycle || detectsCycle(nodes, edges);
        maxDepth = Math.max(0, maxDepth);
    }

    public static ConditionLogicChainSnapshot of(
            String rootChannel,
            String rootNodeId,
            Collection<ConditionLogicChainNodeSnapshot> nodes,
            Collection<ConditionLogicChainEdgeSnapshot> edges,
            Collection<String> channels,
            boolean hasCycle,
            int maxDepth
    ) {
        return new ConditionLogicChainSnapshot(
                rootChannel,
                rootNodeId,
                nodes == null ? List.of() : List.copyOf(nodes),
                edges == null ? List.of() : List.copyOf(edges),
                channels == null ? Set.of() : copyChannels(channels),
                hasCycle,
                maxDepth
        );
    }

    public boolean containsNode(String nodeId) {
        String expected = safe(nodeId);
        return !expected.isBlank() && nodes.stream().anyMatch((node) -> node.nodeId().equals(expected));
    }

    public boolean containsChannel(String channel) {
        String expected = SignalChannel.normalize(channel);
        if (expected.isBlank()) {
            return false;
        }
        if (rootChannel.equals(expected) || channels.contains(expected)) {
            return true;
        }
        return nodes.stream().anyMatch((node) -> node.channel().equals(expected));
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int channelCount() {
        return channels.size();
    }

    private static Set<String> copyChannels(Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String channel : raw) {
            String normalized = SignalChannel.normalize(channel);
            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }
        return Set.copyOf(copy);
    }

    private static boolean detectsCycle(List<ConditionLogicChainNodeSnapshot> nodes, List<ConditionLogicChainEdgeSnapshot> edges) {
        if (nodes == null || edges == null || edges.isEmpty()) {
            return false;
        }
        Set<String> nodeIds = new LinkedHashSet<>();
        for (ConditionLogicChainNodeSnapshot node : nodes) {
            if (node != null && !node.nodeId().isBlank()) {
                nodeIds.add(node.nodeId());
            }
        }
        java.util.Map<String, java.util.List<String>> graph = new java.util.LinkedHashMap<>();
        for (ConditionLogicChainEdgeSnapshot edge : edges) {
            if (edge == null || edge.fromNodeId().isBlank() || edge.toNodeId().isBlank()) {
                continue;
            }
            if (nodeIds.contains(edge.fromNodeId()) && nodeIds.contains(edge.toNodeId())) {
                graph.computeIfAbsent(edge.fromNodeId(), ignored -> new java.util.ArrayList<>()).add(edge.toNodeId());
            }
        }
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (hasCycle(nodeId, graph, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCycle(
            String nodeId,
            java.util.Map<String, java.util.List<String>> graph,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (!visiting.add(nodeId)) {
            return true;
        }
        for (String next : graph.getOrDefault(nodeId, List.of())) {
            if (hasCycle(next, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
