package com.zcpu.tzzmod.webadmin;

// data-webadmin-frontend-bundle-entry-only
// WebAdmin 前端仍是 Java 字符串生成的单一 /assets/app.js。此 facade 只负责稳定顺序拼接
// 各模块：不要在这里新增业务逻辑、事件 handler、route 表、临时 patch 或任意 JS text block。
// 新功能必须进入拥有该职责的模块，并同步 guard/docs 说明输出顺序和状态边界。
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
