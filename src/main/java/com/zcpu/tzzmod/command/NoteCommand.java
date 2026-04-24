package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.zcpu.tzzmod.note.NoteDataStore;
import com.zcpu.tzzmod.note.NoteServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class NoteCommand {
    private NoteCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("note")
                .then(CommandManager.literal("removeall")
                .requires(source -> !(source.getEntity() instanceof ServerPlayerEntity player) || player.isCreativeLevelTwoOp())
                        .executes(context -> removeAll(context.getSource()))));
    }

    private static int removeAll(ServerCommandSource source) {
        int count = NoteDataStore.removeAll(source.getServer());
        NoteServer.broadcastAllCleared(source.getServer());
        source.sendFeedback(() -> Text.literal("Deleted all notes: " + count), true);
        return count;
    }
}
