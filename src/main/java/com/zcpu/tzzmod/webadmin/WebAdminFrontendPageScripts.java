package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendPageScripts {
    private WebAdminFrontendPageScripts() {
    }

    static String appJs() {
        return WebAdminFrontendDashboardScripts.appJs()
                + WebAdminFrontendDeviceScripts.appJs()
                + WebAdminFrontendDeviceSessionScripts.appJs()
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
