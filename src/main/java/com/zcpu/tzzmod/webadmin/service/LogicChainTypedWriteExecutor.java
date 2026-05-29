package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.util.List;
import net.minecraft.server.MinecraftServer;

// 该 executor 只负责“按旧顺序调用 typed 写入口”。它不做去重、不重排、不尝试跨 store
// 回滚；失败恢复仍由 save coordinator 按旧逻辑保留 Logic Chain editor lock / draft。
final class LogicChainTypedWriteExecutor {
    private final Adapter adapter;

    LogicChainTypedWriteExecutor(Adapter adapter) {
        this.adapter = adapter;
    }

    ExecutionResult execute(
            ExecutionContext context,
            WebAdminLogicChainEditorRequest request,
            WebAdminWriteTarget target,
            LogicChainDraftOperationPlanner.OperationPlan plan
    ) {
        WebAdminWriteResult result = WebAdminLogicChainEditorService.ok(target, "逻辑链草稿已保存。", WebAdminLogicChainEditorService.multiDraftSaveData(request));
        WebAdminLogicChainEditorRequest.DraftNode draftNode = null;
        WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit = null;
        WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit = null;
        WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete = null;
        WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete = null;
        WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder = null;

        int nodeIndex = 0;
        for (WebAdminLogicChainEditorRequest.DraftNode node : plan.draftNodes()) {
            draftNode = node;
            result = adapter.saveDraftNode(context, request, target, node, request == null ? List.of() : request.edges, nodeIndex);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
            nodeIndex++;
        }
        if (plan.actionAppend() != null) {
            result = adapter.saveActionAppend(context, plan.actionAppend());
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : plan.existingNodeEdits()) {
            existingNodeEdit = edit;
            result = adapter.saveExistingNodeEdit(context, edit);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : plan.actionEdits()) {
            actionEdit = edit;
            result = adapter.saveActionEdit(context, edit);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : plan.actionDeletes()) {
            actionDelete = delete;
            result = adapter.saveActionDelete(context, delete);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : plan.actionReorders()) {
            actionReorder = reorder;
            result = adapter.saveActionReorder(context, reorder);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : plan.nodeDeletes()) {
            nodeDelete = delete;
            result = adapter.saveNodeDelete(context, delete);
            if (!result.success()) {
                return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
            }
        }
        return new ExecutionResult(result, draftNode, plan.actionAppend(), existingNodeEdit, actionEdit, nodeDelete, actionDelete, actionReorder);
    }

    interface Adapter {
        WebAdminWriteResult saveDraftNode(
                ExecutionContext context,
                WebAdminLogicChainEditorRequest request,
                WebAdminWriteTarget target,
                WebAdminLogicChainEditorRequest.DraftNode node,
                List<WebAdminLogicChainEditorRequest.DraftEdge> edges,
                int fieldIndex
        );

        WebAdminWriteResult saveActionAppend(ExecutionContext context, WebAdminLogicChainEditorRequest.ActionAppendDraft draft);

        WebAdminWriteResult saveExistingNodeEdit(ExecutionContext context, WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft);

        WebAdminWriteResult saveActionEdit(ExecutionContext context, WebAdminLogicChainEditorRequest.ActionEditDraft draft);

        WebAdminWriteResult saveActionDelete(ExecutionContext context, WebAdminLogicChainEditorRequest.ActionDeleteDraft draft);

        WebAdminWriteResult saveActionReorder(ExecutionContext context, WebAdminLogicChainEditorRequest.ActionReorderDraft draft);

        WebAdminWriteResult saveNodeDelete(ExecutionContext context, WebAdminLogicChainEditorRequest.NodeDeleteDraft draft);
    }

    record ExecutionContext(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String csrfToken,
            boolean sameOrigin
    ) {
    }

    record ExecutionResult(
            WebAdminWriteResult result,
            WebAdminLogicChainEditorRequest.DraftNode draftNode,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            WebAdminLogicChainEditorRequest.ExistingNodeEditDraft existingNodeEdit,
            WebAdminLogicChainEditorRequest.ActionEditDraft actionEdit,
            WebAdminLogicChainEditorRequest.NodeDeleteDraft nodeDelete,
            WebAdminLogicChainEditorRequest.ActionDeleteDraft actionDelete,
            WebAdminLogicChainEditorRequest.ActionReorderDraft actionReorder
    ) {
    }
}
