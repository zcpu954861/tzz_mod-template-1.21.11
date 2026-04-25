package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.command.TzzRootCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class TzzCommandBootstrap {
    private TzzCommandBootstrap() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TzzRootCommand.register(dispatcher);
        });
    }
}
