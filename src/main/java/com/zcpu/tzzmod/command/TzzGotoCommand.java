package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public final class TzzGotoCommand {
    private TzzGotoCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Intentionally left blank to disable the /tzz_goto command registration.
    }
}
