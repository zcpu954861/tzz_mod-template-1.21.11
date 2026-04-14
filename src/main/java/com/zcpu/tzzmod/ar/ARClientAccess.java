package com.zcpu.tzzmod.ar;

public final class ARClientAccess {
    private static Runnable opener = () -> {
    };

    private ARClientAccess() {
    }

    public static void setOpener(Runnable screenOpener) {
        opener = screenOpener;
    }

    public static void openARScreen() {
        opener.run();
    }
}
