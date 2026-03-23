package com.zcpu.tzzmod.password;

import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public final class PasswordMachineClientAccess {
    private static Consumer<BlockPos> opener = pos -> {
    };

    private PasswordMachineClientAccess() {
    }

    public static void setOpener(Consumer<BlockPos> screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen(BlockPos pos) {
        opener.accept(pos.toImmutable());
    }
}

