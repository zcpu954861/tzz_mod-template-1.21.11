package com.zcpu.tzzmod.password;

import net.minecraft.util.Hand;

import java.util.function.Consumer;

public final class PasswordCardClientAccess {
    private static Consumer<Hand> opener = hand -> {
    };

    private PasswordCardClientAccess() {
    }

    public static void setOpener(Consumer<Hand> screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen(Hand hand) {
        opener.accept(hand);
    }
}

