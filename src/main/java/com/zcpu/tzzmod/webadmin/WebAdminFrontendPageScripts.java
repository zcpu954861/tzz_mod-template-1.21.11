package com.zcpu.tzzmod.webadmin;

// 非 Logic Chain 页面 bundle facade。这里只维护 Dashboard/Device/Signal/Help/Snapshot 等页面模块
// 的拼接顺序，不承载业务逻辑；具体页面状态、写操作和 modal 行为必须留在对应 owner 模块中。
final class WebAdminFrontendPageScripts {
    private WebAdminFrontendPageScripts() {
    }

    static String appJs() {
        return WebAdminFrontendDashboardScripts.appJs()
                + WebAdminFrontendDeviceScripts.appJs()
                + WebAdminFrontendDeviceSessionScripts.appJs()
                + WebAdminActionSchemaScripts.appJs()
                + WebAdminActionFieldRenderScripts.appJs()
                + WebAdminActionSummaryScripts.appJs()
                + WebAdminFrontendDeviceEditorScripts.appJs()
                + WebAdminFrontendSignalScripts.appJs()
                + WebAdminFrontendActionTimerScripts.appJs()
                + WebAdminFrontendHelpScripts.appJs()
                + WebAdminFrontendModalScripts.appJs()
                + WebAdminFrontendSnapshotScripts.appJs()
                + WebAdminFrontendTemplateConfigScripts.appJs()
                + WebAdminFrontendRegionConditionScripts.appJs();
    }
}
