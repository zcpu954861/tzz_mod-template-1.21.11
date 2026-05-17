package com.zcpu.tzzmod.scheduler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class TimerServer {
    private TimerServer() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(TimerRuntimeService::refreshDefinitions);
        ServerTickEvents.END_SERVER_TICK.register(TimerRuntimeService::tick);
    }
}
