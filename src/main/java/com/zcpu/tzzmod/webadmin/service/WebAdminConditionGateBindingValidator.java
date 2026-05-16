package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityProfile;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.server.MinecraftServer;

final class WebAdminConditionGateBindingValidator {
    private final Path testStorePath;
    private final ConditionEvaluator conditionEvaluator;
    private final ConditionGroupCompatibilityService compatibilityService;

    WebAdminConditionGateBindingValidator() {
        this(null);
    }

    WebAdminConditionGateBindingValidator(Path testStorePath) {
        this(testStorePath, new ConditionEvaluator(), new ConditionGroupCompatibilityService());
    }

    WebAdminConditionGateBindingValidator(
            Path testStorePath,
            ConditionEvaluator conditionEvaluator,
            ConditionGroupCompatibilityService compatibilityService
    ) {
        this.testStorePath = testStorePath;
        this.conditionEvaluator = conditionEvaluator == null ? new ConditionEvaluator() : conditionEvaluator;
        this.compatibilityService = compatibilityService == null ? new ConditionGroupCompatibilityService() : compatibilityService;
    }

    void validate(
            MinecraftServer server,
            List<WebAdminValidationError> errors,
            String field,
            String groupId,
            ConditionRuntimeTargetType targetType
    ) {
        validate(server, errors, field, groupId, targetType, compatibilityService.profile(targetType));
    }

    void validate(
            MinecraftServer server,
            List<WebAdminValidationError> errors,
            String field,
            String groupId,
            ConditionRuntimeTargetType targetType,
            ConditionGroupCompatibilityProfile profile
    ) {
        String normalizedGroupId = WebAdminConditionGroupStore.normalizeId(groupId);
        if (normalizedGroupId.isBlank()) {
            return;
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = loadConditionGroups(server);
        if (loaded.degraded()) {
            errors.add(new WebAdminValidationError(field, "condition_group_store_degraded", loaded.message(), normalizedGroupId));
            return;
        }
        WebAdminConditionGroupStore.ConditionGroupEntry entry = loaded.file().groups.get(normalizedGroupId);
        if (entry == null) {
            errors.add(new WebAdminValidationError(field, "condition_group_missing", "条件组不存在或已删除：" + normalizedGroupId, normalizedGroupId));
            return;
        }
        WebAdminConditionGroupStore.ConditionGroupEntry normalized = WebAdminConditionGroupStore.ConditionGroupEntry.normalized(entry.id, entry);
        if (!normalized.enabled) {
            errors.add(new WebAdminValidationError(field, "condition_group_disabled", "条件组已停用，不能绑定到运行时触发：" + normalizedGroupId, normalizedGroupId));
            return;
        }
        if (normalized.groupDefinition == null) {
            errors.add(new WebAdminValidationError(field, "condition_group_definition_missing", "条件组定义缺失，不能绑定到运行时触发：" + normalizedGroupId, normalizedGroupId));
            return;
        }
        ConditionValidationResult validation = conditionEvaluator.validate(normalized.groupDefinition);
        if (!validation.valid()) {
            String firstIssue = validation.issues().stream()
                    .map(issue -> issue.message())
                    .filter(message -> message != null && !message.isBlank())
                    .findFirst()
                    .orElse("存在无效条件节点");
            errors.add(new WebAdminValidationError(field, "condition_group_validation_failed", "条件组校验失败，不能绑定到运行时触发：" + firstIssue, normalizedGroupId));
            return;
        }
        ConditionGroupCompatibilityResult compatibility = compatibilityService.analyze(normalized.groupDefinition, profile);
        if (!compatibility.compatible()) {
            errors.add(new WebAdminValidationError(field, "condition_group_incompatible", "条件组与当前触发方式不兼容：" + compatibility.message(), normalizedGroupId));
        }
    }

    private WebAdminConditionGroupStore.ConditionGroupLoadResult loadConditionGroups(MinecraftServer server) {
        return testStorePath == null
                ? WebAdminConditionGroupStore.loadWithStatus(server)
                : WebAdminConditionGroupStore.loadWithStatus(testStorePath);
    }
}
