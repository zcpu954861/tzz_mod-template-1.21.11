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
                + WebAdminLogicChainCanvasScripts.appJs()
                + WebAdminLogicChainNodePanelScripts.appJs()
                + WebAdminLogicChainCanvasScripts.stageAppJs()
                + WebAdminLogicChainLayoutScripts.appJs()
                + WebAdminLogicChainDraftOverlayScripts.appJs()
                + WebAdminLogicChainCanvasScripts.renderAppJs()
                + WebAdminLogicChainEditorScripts.appJs()
                + WebAdminLogicChainVbdScripts.appJs()
                + WebAdminLogicChainVbdOverlayScripts.appJs()
                + WebAdminFrontendBootstrapScripts.appJs();
    }
}
