package com.zcpu.tzzmod.condition;

public interface ConditionTypeHandler {
    ConditionTypeMetadata metadata();

    default ConditionValidationResult validate(ConditionNode node) {
        return ConditionValidationResult.ok();
    }

    ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context);
}
