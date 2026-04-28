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
        data.put("readonlyStage", true);
        data.put("writeApiEnabled", false);
        data.put("message", "当前版本仅提供写入前置能力，尚未开放 Web UI 配置写入。");
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
            entry.put("futureOnly", operation != WebAdminOperationType.READ);
            operations.add(entry);
        }
        data.put("operations", operations);
        return data;
    }
}
