package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.zcpu.tzzmod.region.RegionControllerCommand;
import com.zcpu.tzzmod.signal.SignalCommand;
import com.zcpu.tzzmod.webadmin.WebAdminCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class TzzRootCommand {
    private TzzRootCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tzz")
                .then(MapCommand.build())
                .then(TaskCommand.build())
                .then(NoteCommand.build())
                .then(SendMsgCommand.build())
                .then(RegionControllerCommand.build())
                .then(SignalCommand.build())
                .then(WebAdminCommand.build()));
    }
}
