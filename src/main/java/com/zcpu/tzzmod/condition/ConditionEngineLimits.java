package com.zcpu.tzzmod.condition;

public record ConditionEngineLimits(int maxDepth, int maxNodes) {
    public static final int DEFAULT_MAX_DEPTH = 16;
    public static final int DEFAULT_MAX_NODES = 128;
    public static final ConditionEngineLimits DEFAULT = new ConditionEngineLimits(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);

    public ConditionEngineLimits {
        maxDepth = maxDepth <= 0 ? DEFAULT_MAX_DEPTH : maxDepth;
        maxNodes = maxNodes <= 0 ? DEFAULT_MAX_NODES : maxNodes;
    }
}
