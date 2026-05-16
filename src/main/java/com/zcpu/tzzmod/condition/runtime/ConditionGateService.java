package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationTrace;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;

public final class ConditionGateService {
    private final Function<String, WebAdminConditionGroupStore.ConditionGroupEntry> groupLoader;
    private final ConditionEvaluator evaluator;
    private final ConditionGroupCompatibilityService compatibilityService;

    public ConditionGateService() {
        this(null, new ConditionEvaluator(), new ConditionGroupCompatibilityService());
    }

    ConditionGateService(
            Function<String, WebAdminConditionGroupStore.ConditionGroupEntry> groupLoader,
            ConditionEvaluator evaluator,
            ConditionGroupCompatibilityService compatibilityService
    ) {
        this.groupLoader = groupLoader;
        this.evaluator = evaluator == null ? new ConditionEvaluator() : evaluator;
        this.compatibilityService = compatibilityService == null ? new ConditionGroupCompatibilityService() : compatibilityService;
    }

    public ConditionGateResult evaluate(MinecraftServer server, ConditionGateRequest request) {
        String groupId = normalizeId(request == null ? "" : request.conditionGroupId());
        if (groupId.isBlank()) {
            return ConditionGateResult.skippedResult();
        }
        long started = System.nanoTime();
        if (ConditionRuntimeGateStore.STORE_UNAVAILABLE_GROUP_ID.equals(groupId)) {
            return record(request, null, null, null,
                    blocked(groupId, "condition_runtime_gate_store_unavailable", "条件组运行时 gate 配置读取失败，已安全阻断当前触发。", started, 0));
        }
        WebAdminConditionGroupStore.ConditionGroupEntry entry = loadGroup(server, groupId);
        if (entry == null) {
            return record(request, null, null, null,
                    blocked(groupId, "condition_group_missing", "条件组不存在或已删除：" + groupId + "。", started, 0));
        }
        if (!entry.enabled) {
            return record(request, entry, null, null,
                    blocked(groupId, "condition_group_disabled", "条件组已停用：" + groupId + "。", started, 0));
        }
        ConditionGroupDefinition definition = entry.groupDefinition;
        if (definition == null) {
            return record(request, entry, null, null,
                    blocked(groupId, "condition_group_definition_missing", "条件组定义缺失，已阻断运行时触发：" + groupId + "。", started, 0));
        }
        ConditionValidationResult validation = evaluator.validate(definition);
        if (!validation.valid()) {
            String summary = validation.issues().stream()
                    .map(issue -> issue.message())
                    .filter(message -> message != null && !message.isBlank())
                    .findFirst()
                    .orElse("存在无效条件节点");
            return record(request, entry, null, null,
                    blocked(groupId, "condition_group_validation_failed", "条件组校验失败：" + summary + "。", started, 0));
        }
        ConditionRuntimeTargetType targetType = request == null || request.targetType() == null
                ? ConditionRuntimeTargetType.VBD_INTERACTION
                : request.targetType();
        ConditionGroupCompatibilityProfile profile = request == null || request.compatibilityProfile() == null
                ? compatibilityService.profile(targetType)
                : request.compatibilityProfile();
        ConditionGroupCompatibilityResult compatibility = compatibilityService.analyze(definition, profile);
        if (!compatibility.compatible()) {
            return record(request, entry, null, null,
                    blocked(groupId, "condition_group_incompatible", "条件组与当前触发方式不兼容：" + compatibility.message(), started, 0));
        }
        // 8.6 guard marker: ConditionEvaluationContext context = request supplier remains lazy.
        ConditionEvaluationContext context = null;
        ConditionEvaluationTrace trace = null;
        try {
            context = request == null || request.contextSupplier() == null
                    ? null
                    : request.contextSupplier().get();
            trace = evaluator.evaluateTrace(definition, context);
            if (trace.rootResult().error()) {
                return record(request, entry, context, trace,
                        blocked(groupId, trace.rootResult().reasonCode(), "条件组评估失败：" + trace.rootResult().message(), started, trace.evaluatedNodeCount()));
            }
            if (!trace.rootResult().matched()) {
                return record(request, entry, context, trace,
                        blocked(groupId, "condition_group_not_matched", "条件组未通过：" + trace.rootResult().message(), started, trace.evaluatedNodeCount()));
            }
            return record(request, entry, context, trace, ConditionGateResult.allowed(
                    groupId,
                    "条件组通过：" + definition.displayName() + "；context=" + (context == null ? "empty" : context.compactSummary()),
                    trace.evaluatedNodeCount(),
                    System.nanoTime() - started
            ));
        } catch (RuntimeException exception) {
            return record(request, entry, context, trace,
                    blocked(groupId, "condition_gate_exception", "条件组评估异常，已安全阻断：" + exception.getMessage() + "。", started, 0));
        }
    }

    private WebAdminConditionGroupStore.ConditionGroupEntry loadGroup(MinecraftServer server, String groupId) {
        if (groupLoader != null) {
            return groupLoader.apply(groupId);
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = WebAdminConditionGroupStore.loadWithStatus(server);
        if (loaded.degraded()) {
            return null;
        }
        return loaded.file().groups.get(groupId);
    }

    private static ConditionGateResult blocked(String groupId, String code, String reason, long started, int evaluatedCount) {
        return ConditionGateResult.blocked(groupId, code, reason, reason, evaluatedCount, System.nanoTime() - started);
    }

    private static ConditionGateResult record(
            ConditionGateRequest request,
            WebAdminConditionGroupStore.ConditionGroupEntry entry,
            ConditionEvaluationContext context,
            ConditionEvaluationTrace trace,
            ConditionGateResult result
    ) {
        ConditionGateHistory.record(request, entry, context, trace, result);
        return result;
    }

    private static String normalizeId(String value) {
        return WebAdminConditionGroupStore.normalizeId(value);
    }

    public Map<String, Object> summary(ConditionGateResult result) {
        ConditionGateResult safe = result == null ? ConditionGateResult.skippedResult() : result;
        return Map.of(
                "allowed", safe.allowed(),
                "skipped", safe.skipped(),
                "conditionGroupId", safe.conditionGroupId(),
                "failureReason", safe.failureReason(),
                "debugSummary", safe.debugSummary(),
                "evaluatedCount", safe.evaluatedCount(),
                "durationNanos", safe.durationNanos(),
                "code", safe.code()
        );
    }
}
