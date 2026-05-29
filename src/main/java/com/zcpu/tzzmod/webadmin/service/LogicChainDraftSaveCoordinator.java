package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

// saveDraft 的主流程固定为 preflight -> editor lock -> fingerprint -> validate ->
// plan -> execute -> channel metadata -> release。Coordinator 只编排保存流程边界：
// request 先通过 WebAdmin 写前置和 graph fingerprint，再由 planner 描述 typed/channel metadata
// 边界，executor 调旧 typed 写入口，最后才处理 channel metadata tail 和成功释放锁。
// 这里不把多个 typed store 包装成完整原子事务；底层失败仍按旧逻辑保留 editor lock / draft。
// 后续扩展应新增 planner/executor adapter 与 guard，不要把新 store mutation 直接塞进 coordinator。
final class LogicChainDraftSaveCoordinator {
    private final WebAdminLogicChainEditorService service;
    private final LogicChainTypedWriteExecutor executor;

    LogicChainDraftSaveCoordinator(WebAdminLogicChainEditorService service) {
        this.service = service;
        this.executor = new LogicChainTypedWriteExecutor(new ServiceAdapter(service));
    }

    WebAdminWriteResult saveDraft(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminLogicChainEditorRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        WebAdminLogicChainEditorRequest safeRequest = WebAdminLogicChainEditorService.safeRequest(request);
        WebAdminWriteTarget target = WebAdminLogicChainEditorService.target(safeRequest);
        WebAdminWriteContext context = WebAdminLogicChainEditorService.writeContext(user, session, remoteAddress, target);
        WebAdminWriteResult preflight = service.writePreflight(user, session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            service.audit(context, preflight, Map.of(), Map.of("attempt", "preflight_failed"));
            return preflight;
        }
        WebAdminWriteResult lock = service.validateEditorLock(user, session, safeRequest);
        if (!lock.success()) {
            service.audit(context, lock, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "edit_lock_failed"));
            return lock;
        }
        WebAdminDtos.LogicChainGraphDto graph = service.currentGraph(server, user, session, safeRequest);
        String actualFingerprint = WebAdminLogicChainEditorService.graphFingerprintFor(graph);
        if (WebAdminLogicChainEditorService.safe(safeRequest.baseGraphFingerprint).isBlank()
                || !actualFingerprint.equals(WebAdminLogicChainEditorService.safe(safeRequest.baseGraphFingerprint))) {
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("expectedFingerprint", WebAdminLogicChainEditorService.safe(safeRequest.baseGraphFingerprint));
            conflict.put("actualFingerprint", actualFingerprint);
            conflict.put("rootType", WebAdminLogicChainEditorService.normalizeRootType(safeRequest.rootType));
            conflict.put("rootRef", WebAdminLogicChainEditorService.safe(safeRequest.rootRef));
            WebAdminWriteResult result = new WebAdminWriteResult(
                    false,
                    WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                    "逻辑链运行图已变化，请刷新后重新进入编辑模式。",
                    target.targetType(),
                    target.targetId(),
                    false,
                    List.of(),
                    "",
                    "",
                    false,
                    conflict,
                    Map.of("baseGraphFingerprint", actualFingerprint)
            );
            service.audit(context, result, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "fingerprint_conflict", "actualFingerprint", actualFingerprint));
            return result;
        }
        List<WebAdminValidationError> errors = service.validateDraftRequest(safeRequest, graph, true, user, session);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            service.audit(context, result, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "validation_failed", "errorCount", errors.size()));
            return result;
        }
        // mixed-write fail-closed 是保存边界 guard，不改变旧 validation 语义：typed node/action/delete
        // 与 channel metadata tail 仍分批保存，避免任一 store 失败后产生半应用假象。
        LogicChainDraftOperationPlanner.OperationPlan plan = LogicChainDraftOperationPlanner.plan(safeRequest);
        if (plan.hasNodeDelete() && plan.hasNonNodeDeleteTypedStoreDrafts()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(WebAdminLogicChainEditorService.error(
                    "nodeDeletes",
                    "logic_chain_node_delete_mixed_write_fail_closed",
                    "节点删除草稿不能和其它 typed 写入混合保存，避免删除失败时出现半应用。",
                    String.valueOf(safeRequest.nodeDeletes == null ? 0 : safeRequest.nodeDeletes.size()),
                    "",
                    "",
                    "",
                    "先单独保存节点删除草稿，再重新进入编辑模式处理其它字段或 action 草稿。"
            )));
            service.audit(context, result, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "node_delete_mixed_write_fail_closed"));
            return result;
        }
        if (plan.hasTypedStoreDrafts() && plan.hasChannelMetadataDrafts()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(WebAdminLogicChainEditorService.error(
                    "channelMetadataDrafts",
                    "logic_chain_mixed_metadata_typed_write_fail_closed",
                    "频道 metadata 草稿不能和其它 typed store 写入混合保存，避免保存失败时出现半应用。",
                    String.valueOf(safeRequest.channelMetadataDrafts == null ? 0 : safeRequest.channelMetadataDrafts.size()),
                    "",
                    "",
                    "",
                    "先保存纯 typed 草稿，再单独保存频道 metadata 草稿；或移除本次 metadata 草稿。"
            )));
            service.audit(context, result, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "mixed_metadata_typed_write_fail_closed"));
            return result;
        }

        LogicChainTypedWriteExecutor.ExecutionContext executionContext =
                new LogicChainTypedWriteExecutor.ExecutionContext(server, user, session, remoteAddress, csrfToken, sameOrigin);
        LogicChainTypedWriteExecutor.ExecutionResult executed = executor.execute(executionContext, safeRequest, target, plan);
        WebAdminWriteResult result = executed.result();
        if (!result.success()) {
            result = service.logicChainSaveFailurePreservingEditorLock(
                    safeRequest,
                    result,
                    executed.draftNode(),
                    executed.actionAppend(),
                    executed.existingNodeEdit(),
                    executed.actionEdit(),
                    executed.nodeDelete(),
                    executed.actionDelete(),
                    executed.actionReorder()
            );
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("attempt", "typed_write_failed");
            after.put("mode", WebAdminLogicChainEditorService.logicChainFailedWriteMode(
                    executed.draftNode(),
                    executed.actionAppend(),
                    executed.existingNodeEdit(),
                    executed.actionEdit(),
                    executed.nodeDelete(),
                    executed.actionDelete(),
                    executed.actionReorder()
            ));
            after.put("code", WebAdminLogicChainEditorService.safe(result.code()));
            after.put("targetType", WebAdminLogicChainEditorService.safe(result.targetType()));
            after.put("targetId", WebAdminLogicChainEditorService.safe(result.targetId()));
            service.audit(context, result, WebAdminLogicChainEditorService.requestSummary(safeRequest), after);
        }
        if (result.success()) {
            WebAdminWriteResult metadataResult = service.saveChannelMetadataDrafts(server, user, safeRequest.channelMetadataDrafts);
            if (!metadataResult.success()) {
                service.audit(context, metadataResult, WebAdminLogicChainEditorService.requestSummary(safeRequest), Map.of("attempt", "channel_metadata_draft_failed_after_typed_write"));
                result = service.logicChainSaveFailurePreservingEditorLock(safeRequest, metadataResult, null, null, null, null, null, null, null);
            } else {
                result = WebAdminLogicChainEditorService.ok(target, "逻辑链草稿已保存。", WebAdminLogicChainEditorService.multiDraftSaveData(safeRequest));
            }
        }
        if (result.success()) {
            service.releaseEditorLockAfterSuccessfulSave(user, session, remoteAddress, safeRequest);
        }
        return result;
    }

    // ServiceAdapter 是兼容旧 typed 写入口的桥：它只把 executor 的顺序调用转发回原 service
    // adapter 方法，不隐藏新副作用、不释放锁、不补做跨 store rollback，也不改变 WebAdmin API 形状。
    private static final class ServiceAdapter implements LogicChainTypedWriteExecutor.Adapter {
        private final WebAdminLogicChainEditorService service;

        private ServiceAdapter(WebAdminLogicChainEditorService service) {
            this.service = service;
        }

        @Override
        public WebAdminWriteResult saveDraftNode(
                LogicChainTypedWriteExecutor.ExecutionContext context,
                WebAdminLogicChainEditorRequest request,
                WebAdminWriteTarget target,
                WebAdminLogicChainEditorRequest.DraftNode node,
                List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
                int fieldIndex
        ) {
            return service.saveDraftNode(context.server(), context.user(), context.session(), context.remoteAddress(), request, target, node, edges, context.csrfToken(), context.sameOrigin(), fieldIndex);
        }

        @Override
        public WebAdminWriteResult saveActionAppend(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.ActionAppendDraft draft) {
            return service.saveActionAppend(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }

        @Override
        public WebAdminWriteResult saveExistingNodeEdit(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft) {
            return service.saveExistingNodeEdit(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }

        @Override
        public WebAdminWriteResult saveActionEdit(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.ActionEditDraft draft) {
            return service.saveActionEdit(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }

        @Override
        public WebAdminWriteResult saveActionDelete(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.ActionDeleteDraft draft) {
            return service.saveActionDelete(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }

        @Override
        public WebAdminWriteResult saveActionReorder(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.ActionReorderDraft draft) {
            return service.saveActionReorder(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }

        @Override
        public WebAdminWriteResult saveNodeDelete(LogicChainTypedWriteExecutor.ExecutionContext context, WebAdminLogicChainEditorRequest.NodeDeleteDraft draft) {
            return service.saveNodeDelete(context.server(), context.user(), context.session(), context.remoteAddress(), draft, context.csrfToken(), context.sameOrigin());
        }
    }
}
