package com.zcpu.tzzmod.blocking;

import net.minecraft.util.Hand;

import java.util.function.Consumer;

public final class BlockingCardConfiguratorClientAccess {
    private static Consumer<Hand> opener = hand -> {
    };

    private BlockingCardConfiguratorClientAccess() {
    }

    public static void setOpener(Consumer<Hand> screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen(Hand hand) {
        opener.accept(hand);
    }
}