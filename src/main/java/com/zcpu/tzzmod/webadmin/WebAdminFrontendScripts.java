package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendScripts {
    private WebAdminFrontendScripts() {
    }

    public static String appJs() {
        return WebAdminFrontendIconScripts.appJs()
                + WebAdminFrontendCoreScripts.appJs()
                + WebAdminFrontendCoreEventScripts.appJs()
                + WebAdminFrontendPageScripts.appJs()
                + WebAdminLogicChainViewerScripts.appJs()
                + WebAdminLogicChainEditorScripts.appJs()
                + WebAdminLogicChainVbdScripts.appJs()
                + WebAdminFrontendBootstrapScripts.appJs();
    }
}
