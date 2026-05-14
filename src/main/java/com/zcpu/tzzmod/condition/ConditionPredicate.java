package com.zcpu.tzzmod.condition;

@FunctionalInterface
public interface ConditionPredicate {
    ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context);
}
