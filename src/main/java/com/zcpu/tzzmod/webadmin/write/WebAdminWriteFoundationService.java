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
        data.put("channelMetadataWriteEnabled", true);
        data.put("signalListenerBasicConfigWriteEnabled", true);
        data.put("message", "当前版本开放 WebAdmin 设备显示信息、设备基础/扩展配置、频道显示信息以及 Signal Listener 基础配置写入。");
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
                    && operation != WebAdminOperationType.EDIT_CHANNEL_METADATA
                    && operation != WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG);
            operations.add(entry);
        }
        data.put("operations", operations);
        return data;
    }
}
