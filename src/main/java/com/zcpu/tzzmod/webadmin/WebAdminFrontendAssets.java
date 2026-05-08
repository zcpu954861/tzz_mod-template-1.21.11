package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendAssets {
    private WebAdminFrontendAssets() {
    }

    public static String loginHtml() {
        return WebAdminFrontendShell.loginHtml();
    }

    public static String appHtml() {
        return WebAdminFrontendShell.appHtml();
    }

    public static String appCss() {
        return WebAdminFrontendStyles.appCss();
    }

    public static String appJs() {
        return WebAdminFrontendScripts.appJs();
    }
}
