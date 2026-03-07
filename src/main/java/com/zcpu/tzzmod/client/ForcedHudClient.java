package com.zcpu.tzzmod.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ForcedHudClient {
    // tri-state: null = no server enforcement, true = force show, false = force hide
    private static volatile Boolean serverEnforcedHud = null;

    private ForcedHudClient() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverEnforcedHud = null);
    }

    public static void setServerEnforcedHud(Boolean enforced) {
        serverEnforcedHud = enforced;
    }

    public static Boolean getServerEnforcedHud() {
        return serverEnforcedHud;
    }

    public static boolean isForceShowHead() {
        return Boolean.TRUE.equals(serverEnforcedHud);
    }

    public static boolean isForceHideHead() {
        return Boolean.FALSE.equals(serverEnforcedHud);
    }
}
