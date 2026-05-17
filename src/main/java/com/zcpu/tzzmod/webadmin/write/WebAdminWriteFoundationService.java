package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminWriteFoundationService {
    private final WebAdminPermissionService permissionService = new WebAdminPermissionService();
    private final WebAdminWriteSecurityService securityService;

    public WebAdminWriteFoundationService(WebAdminWriteSecurityService securityService) {
        this.securityService = securityService;
    }

    public Map<String, Object> capabilities(WebAdminUser user, WebAdminSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("readonlyStage", false);
        data.put("writeApiEnabled", true);
        data.put("metadataWriteEnabled", true);
        data.put("deviceBasicConfigWriteEnabled", true);
        data.put("deviceExtendedConfigWriteEnabled", true);
        data.put("actionRelayActionListWriteEnabled", true);
        data.put("vbdNativeTriggerWriteEnabled", true);
        data.put("interactionItemMatcherWriteEnabled", true);
        data.put("singleItemSubmitTemplateWriteEnabled", true);
        data.put("channelMetadataWriteEnabled", true);
        data.put("conditionGroupWriteEnabled", true);
        data.put("signalListenerBasicConfigWriteEnabled", true);
        data.put("signalListenerActionListWriteEnabled", true);
        data.put("signalJoinWriteEnabled", true);
        data.put("regionControllerWriteEnabled", true);
        data.put("objectSelectionEnabled", true);
        data.put("virtualBlockDeviceLifecycleEnabled", true);
        data.put("signalListenerLifecycleWriteEnabled", true);
        data.put("message", "当前版本开放 WebAdmin 设备显示信息、设备基础/扩展配置、Action Relay 动作列表、VBD 原生触发配置、VBD 交互物品匹配、VBD 统一 itemSubmit requirement 编辑器、频道显示信息、条件组编辑、Signal Listener 基础配置与动作列表、Signal Join 汇合配置、RegionController 配置、对象选择创建、虚拟方块设备删除/解绑和 Signal Listener 创建/删除。");
        data.put("permissions", permissionService.capabilitySummary(user == null ? null : user.roleEnum()));
        Map<String, Object> csrf = new LinkedHashMap<>();
        csrf.put("requiredForFutureWrites", true);
        csrf.put("headerName", "X-TZZ-WebAdmin-CSRF");
        csrf.put("token", securityService.csrfTokenFor(session));
        data.put("csrf", csrf);
        List<Map<String, Object>> operations = new ArrayList<>();
        for (WebAdminOperationType operation : WebAdminOperationType.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("operation", operation.id());
            entry.put("displayName", operation.displayName());
            entry.put("allowed", permissionService.decide(user, operation).allowed());
            entry.put("futureOnly", operation != WebAdminOperationType.READ
                    && operation != WebAdminOperationType.EDIT_DEVICE_METADATA
                    && operation != WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG
                    && operation != WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG
                    && operation != WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS
                    && operation != WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS
                    && operation != WebAdminOperationType.EDIT_ITEM_MATCHER
                    && operation != WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION
                    && operation != WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT
                    && operation != WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION
                    && operation != WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION
                    && operation != WebAdminOperationType.EDIT_CHANNEL_METADATA
                    && operation != WebAdminOperationType.EDIT_CONDITION_GROUP
                    && operation != WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG
                    && operation != WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS
                    && operation != WebAdminOperationType.EDIT_SIGNAL_JOIN
                    && operation != WebAdminOperationType.START_OBJECT_SELECTION
                    && operation != WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE
                    && operation != WebAdminOperationType.CREATE_SIGNAL_LISTENER
                    && operation != WebAdminOperationType.DELETE_SIGNAL_LISTENER
                    && operation != WebAdminOperationType.EDIT_REGION);
            operations.add(entry);
        }
        data.put("operations", operations);
        return data;
    }
}
