package com.zcpu.tzzmod.condition;

import java.util.List;

public record ConditionDefinition(ConditionGroupDefinition group) {
    public ConditionDefinition {
        group = group == null
                ? ConditionGroupDefinition.of("condition", ConditionNode.group("root", ConditionGroupMode.AND, List.of()))
                : group;
    }

    public static ConditionDefinition of(String id, ConditionNode root) {
        return new ConditionDefinition(ConditionGroupDefinition.of(id, root));
    }

    public String id() {
        return group.id();
    }

    public ConditionNode root() {
        return group.root();
    }

    public String stableFingerprint() {
        return group.stableFingerprint();
    }
}
