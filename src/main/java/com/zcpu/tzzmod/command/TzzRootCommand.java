package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
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
                .then(SendMsgCommand.build()));
    }
}
