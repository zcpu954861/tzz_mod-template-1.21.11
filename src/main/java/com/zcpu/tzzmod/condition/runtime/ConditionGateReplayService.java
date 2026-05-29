package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationTrace;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;

public final class ConditionGateReplayService {
    private final Function<String, WebAdminConditionGroupStore.ConditionGroupEntry> groupLoader;
    private final ConditionEvaluator evaluator;

    public ConditionGateReplayService() {
        this(null, new ConditionEvaluator());
    }

    public ConditionGateReplayService(
            Function<String, WebAdminConditionGroupStore.ConditionGroupEntry> groupLoader,
            ConditionEvaluator evaluator
    ) {
        this.groupLoader = groupLoader;
        this.evaluator = evaluator == null ? new ConditionEvaluator() : evaluator;
    }

    public ConditionGateReplayResult replay(MinecraftServer server, String recordId) {
        ConditionGateHistoryRecord record = ConditionGateHistory.find(recordId).orElse(null);
        if (record == null) {
            return ConditionGateReplayResult.failed(recordId, "", "condition_gate_history_missing", "条件 gate 历史记录不存在或已被内存环形缓冲淘汰。", List.of());
        }
        if (record.replayContext() == null) {
            return ConditionGateReplayResult.failed(record.id(), record.conditionGroupId(), "condition_gate_replay_context_missing", "该历史记录没有可复现上下文快照，无法只读 replay。", List.of("未读取 live world / player / inventory / region / SignalBridge。"));
        }
        if (record.definitionSnapshot() == null) {
            return ConditionGateReplayResult.failed(record.id(), record.conditionGroupId(), "condition_gate_replay_definition_missing", "该历史记录没有条件组定义快照，无法只读 replay。", List.of("未读取 live world / player / inventory / region / SignalBridge。"));
        }

        List<String> warnings = new ArrayList<>();
        WebAdminConditionGroupStore.ConditionGroupEntry current = loadGroup(server, record.conditionGroupId());
        if (current == null || current.groupDefinition == null) {
            return ConditionGateReplayResult.failed(
                    record.id(),
                    record.conditionGroupId(),
                    "condition_gate_replay_group_deleted",
                    "原条件组已删除或当前不可读取，replay 已安全停止。",
                    List.of("replay 没有执行 action、没有 emit signal、没有写入 store。")
            );
        }
        String currentFingerprint = current.groupDefinition.stableFingerprint();
        boolean changed = !record.definitionFingerprint().isBlank() && !record.definitionFingerprint().equals(currentFingerprint);
        if (changed) {
            warnings.add("条件组当前定义已变化，本次使用历史快照只读评估，结果可能不同于当前配置。");
        }
        warnings.add("Replay 只使用历史 ConditionEvaluationContext 快照，不读取 live world / player / inventory / region / SignalBridge。");
        warnings.add("Replay 不写 store、不 emit signal、不执行 action、不消费或移动物品。");

        ConditionEvaluationTrace trace = evaluator.evaluateTrace(record.definitionSnapshot(), record.replayContext());
        boolean matched = trace.rootResult().matched() && !trace.rootResult().error();
        String replayResult = trace.rootResult().error() ? "ERROR" : (matched ? "ALLOWED" : "BLOCKED");
        String failureReason = matched ? "" : trace.rootResult().message();
        boolean originalMatched = "ALLOWED".equals(record.result());
        return new ConditionGateReplayResult(
                true,
                true,
                true,
                true,
                record.id(),
                record.conditionGroupId(),
                record.result(),
                replayResult,
                originalMatched,
                matched,
                record.result().equals(replayResult),
                trace.rootResult().reasonCode(),
                failureReason,
                trace.rootResult().debugSummary(),
                trace.evaluatedNodeCount(),
                trace.durationNanos(),
                record.definitionFingerprint(),
                currentFingerprint,
                changed,
                warnings,
                record.contextSummary(),
                ConditionGateDebugNode.from(trace.rootResult())
        );
    }

    private WebAdminConditionGroupStore.ConditionGroupEntry loadGroup(MinecraftServer server, String groupId) {
        if (groupLoader != null) {
            return groupLoader.apply(groupId);
        }
        WebAdminConditionGroupStore.ConditionGroupLoadResult loaded = WebAdminConditionGroupStore.loadWithStatusCached(server);
        if (loaded.degraded()) {
            return null;
        }
        return loaded.file().groups.get(WebAdminConditionGroupStore.normalizeId(groupId));
    }
}
