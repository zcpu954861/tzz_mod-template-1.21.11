package com.zcpu.tzzmod.webadmin.testbridge;

import com.zcpu.tzzmod.network.WebAdminTestBridgeGuiC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WebAdminTestBridgeGuiServer {
    private WebAdminTestBridgeGuiServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(WebAdminTestBridgeGuiC2SPayload.ID, (payload, context) ->
                WebAdminTestBridgeClientGuiBridge.handleClientResponse(context.player(), payload)
        );
    }
}
