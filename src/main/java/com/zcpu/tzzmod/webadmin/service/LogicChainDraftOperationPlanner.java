package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Logic Chain 保存计划只描述“这次草稿要按什么顺序写入”，不执行任何 store mutation。
// Planner 只做草稿快照和存在性过滤：不做权限/锁校验、不写 store、不保存自由 graph document。
// 这里保持 9.1 已验收的 typed resource 边界：nodes/action append/existing edit/action
// edit/delete/reorder/node delete 是 typed 写入，channel metadata 仍是尾部独立边界。
final class LogicChainDraftOperationPlanner {
    private LogicChainDraftOperationPlanner() {
    }

    static OperationPlan plan(WebAdminLogicChainEditorRequest request) {
        List<WebAdminLogicChainEditorRequest.DraftNode> draftNodes = request == null || request.nodes == null
                ? List.of()
                : nullableSnapshot(request.nodes);
        List<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft> existingNodeEdits = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : request == null || request.existingNodeEdits == null
                ? List.<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft>of()
                : request.existingNodeEdits) {
            if (isExistingNodeEditDraftPresent(edit)) {
                existingNodeEdits.add(edit);
            }
        }
        List<WebAdminLogicChainEditorRequest.ActionEditDraft> actionEdits = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : request == null || request.actionEdits == null
                ? List.<WebAdminLogicChainEditorRequest.ActionEditDraft>of()
                : request.actionEdits) {
            if (isActionEditDraftPresent(edit)) {
                actionEdits.add(edit);
            }
        }
        List<WebAdminLogicChainEditorRequest.ActionDeleteDraft> actionDeletes = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : request == null || request.actionDeletes == null
                ? List.<WebAdminLogicChainEditorRequest.ActionDeleteDraft>of()
                : request.actionDeletes) {
            if (isActionDeleteDraftPresent(delete)) {
                actionDeletes.add(delete);
            }
        }
        List<WebAdminLogicChainEditorRequest.ActionReorderDraft> actionReorders = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : request == null || request.actionReorders == null
                ? List.<WebAdminLogicChainEditorRequest.ActionReorderDraft>of()
                : request.actionReorders) {
            if (isActionReorderDraftPresent(reorder)) {
                actionReorders.add(reorder);
            }
        }
        List<WebAdminLogicChainEditorRequest.NodeDeleteDraft> nodeDeletes = new ArrayList<>();
        for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : request == null || request.nodeDeletes == null
                ? List.<WebAdminLogicChainEditorRequest.NodeDeleteDraft>of()
                : request.nodeDeletes) {
            if (isNodeDeleteDraftPresent(delete)) {
                nodeDeletes.add(delete);
            }
        }
        List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> channelMetadataDrafts = request == null || request.channelMetadataDrafts == null
                ? List.of()
                : nullableSnapshot(request.channelMetadataDrafts);
        return new OperationPlan(
                draftNodes,
                hasActionAppend(request) ? request.actionAppend : null,
                List.copyOf(existingNodeEdits),
                List.copyOf(actionEdits),
                List.copyOf(actionDeletes),
                List.copyOf(actionReorders),
                List.copyOf(nodeDeletes),
                channelMetadataDrafts
        );
    }

    static boolean hasActionAppend(WebAdminLogicChainEditorRequest request) {
        WebAdminLogicChainEditorRequest.ActionAppendDraft draft = request == null ? null : request.actionAppend;
        return draft != null && (!safe(draft.ownerType).isBlank() || !safe(draft.ownerId).isBlank() || draft.action != null);
    }

    static boolean hasTypedStoreDrafts(WebAdminLogicChainEditorRequest request) {
        OperationPlan plan = plan(request);
        return !plan.draftNodes().isEmpty()
                || plan.actionAppend() != null
                || !plan.existingNodeEdits().isEmpty()
                || !plan.actionEdits().isEmpty()
                || !plan.nodeDeletes().isEmpty()
                || !plan.actionDeletes().isEmpty()
                || !plan.actionReorders().isEmpty();
    }

    static boolean hasNonNodeDeleteTypedStoreDrafts(WebAdminLogicChainEditorRequest request) {
        OperationPlan plan = plan(request);
        return !plan.draftNodes().isEmpty()
                || plan.actionAppend() != null
                || !plan.existingNodeEdits().isEmpty()
                || !plan.actionEdits().isEmpty()
                || !plan.actionDeletes().isEmpty()
                || !plan.actionReorders().isEmpty();
    }

    static boolean hasChannelMetadataDrafts(WebAdminLogicChainEditorRequest request) {
        return request != null && request.channelMetadataDrafts != null && !request.channelMetadataDrafts.isEmpty();
    }

    static boolean hasExistingNodeEdit(WebAdminLogicChainEditorRequest request) {
        return !plan(request).existingNodeEdits().isEmpty();
    }

    static boolean isExistingNodeEditDraftPresent(WebAdminLogicChainEditorRequest.ExistingNodeEditDraft draft) {
        return draft != null && (!safe(draft.nodeType).isBlank()
                || !safe(draft.targetId).isBlank()
                || draft.channelMetadata != null
                || draft.signalJoin != null
                || draft.timer != null
                || draft.signalListenerBasic != null
                || draft.deviceBasic != null
                || draft.deviceMetadata != null
                || draft.virtualBlockDevice != null);
    }

    static boolean hasActionEdit(WebAdminLogicChainEditorRequest request) {
        return !plan(request).actionEdits().isEmpty();
    }

    static boolean isActionEditDraftPresent(WebAdminLogicChainEditorRequest.ActionEditDraft draft) {
        return draft != null && (!safe(draft.ownerType).isBlank()
                || !safe(draft.ownerId).isBlank()
                || draft.action != null
                || !safe(draft.expectedFingerprint).isBlank()
                || !safe(draft.lockId).isBlank());
    }

    static boolean hasNodeDelete(WebAdminLogicChainEditorRequest request) {
        return !plan(request).nodeDeletes().isEmpty();
    }

    static boolean isNodeDeleteDraftPresent(WebAdminLogicChainEditorRequest.NodeDeleteDraft draft) {
        return draft != null && (!safe(draft.nodeType).isBlank()
                || !safe(draft.targetId).isBlank()
                || !safe(draft.ownerType).isBlank()
                || !safe(draft.ownerId).isBlank()
                || !safe(draft.expectedFingerprint).isBlank()
                || !safe(draft.lockId).isBlank()
                || !safe(draft.confirmationText).isBlank()
                || Boolean.TRUE.equals(draft.impactAccepted)
                || Boolean.TRUE.equals(draft.confirmed));
    }

    static boolean hasActionDelete(WebAdminLogicChainEditorRequest request) {
        return !plan(request).actionDeletes().isEmpty();
    }

    static boolean isActionDeleteDraftPresent(WebAdminLogicChainEditorRequest.ActionDeleteDraft draft) {
        return draft != null && (!safe(draft.ownerType).isBlank()
                || !safe(draft.ownerId).isBlank()
                || !safe(draft.expectedFingerprint).isBlank()
                || !safe(draft.lockId).isBlank()
                || Boolean.TRUE.equals(draft.confirmed));
    }

    static boolean hasActionReorder(WebAdminLogicChainEditorRequest request) {
        return !plan(request).actionReorders().isEmpty();
    }

    static boolean isActionReorderDraftPresent(WebAdminLogicChainEditorRequest.ActionReorderDraft draft) {
        return draft != null && (!safe(draft.ownerType).isBlank()
                || !safe(draft.ownerId).isBlank()
                || !safe(draft.expectedFingerprint).isBlank()
                || !safe(draft.lockId).isBlank()
                || Boolean.TRUE.equals(draft.confirmed));
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static <T> List<T> nullableSnapshot(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    // OperationPlan 前半段字段顺序就是 typed 写入顺序；channelMetadataDrafts 是尾部独立边界。
    // 新增 operation kind 必须同步 executor 顺序、mixed-write fail-closed guard、service tests 和 current context。
    record OperationPlan(
            List<WebAdminLogicChainEditorRequest.DraftNode> draftNodes,
            WebAdminLogicChainEditorRequest.ActionAppendDraft actionAppend,
            List<WebAdminLogicChainEditorRequest.ExistingNodeEditDraft> existingNodeEdits,
            List<WebAdminLogicChainEditorRequest.ActionEditDraft> actionEdits,
            List<WebAdminLogicChainEditorRequest.ActionDeleteDraft> actionDeletes,
            List<WebAdminLogicChainEditorRequest.ActionReorderDraft> actionReorders,
            List<WebAdminLogicChainEditorRequest.NodeDeleteDraft> nodeDeletes,
            List<WebAdminLogicChainEditorRequest.ChannelMetadataDraft> channelMetadataDrafts
    ) {
        boolean hasNodeDelete() {
            return !nodeDeletes.isEmpty();
        }

        boolean hasTypedStoreDrafts() {
            return !draftNodes.isEmpty()
                    || actionAppend != null
                    || !existingNodeEdits.isEmpty()
                    || !actionEdits.isEmpty()
                    || !nodeDeletes.isEmpty()
                    || !actionDeletes.isEmpty()
                    || !actionReorders.isEmpty();
        }

        boolean hasNonNodeDeleteTypedStoreDrafts() {
            return !draftNodes.isEmpty()
                    || actionAppend != null
                    || !existingNodeEdits.isEmpty()
                    || !actionEdits.isEmpty()
                    || !actionDeletes.isEmpty()
                    || !actionReorders.isEmpty();
        }

        boolean hasChannelMetadataDrafts() {
            return !channelMetadataDrafts.isEmpty();
        }
    }
}
