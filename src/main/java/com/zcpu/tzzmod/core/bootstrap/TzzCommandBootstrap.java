package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.command.MapCommand;
import com.zcpu.tzzmod.command.NoteCommand;
import com.zcpu.tzzmod.command.SendMsgCommand;
import com.zcpu.tzzmod.command.TaskCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class TzzCommandBootstrap {
    private TzzCommandBootstrap() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SendMsgCommand.register(dispatcher);
            MapCommand.register(dispatcher);
            TaskCommand.register(dispatcher);
            NoteCommand.register(dispatcher);
        });
    }
}
