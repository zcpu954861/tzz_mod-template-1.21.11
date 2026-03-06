package com.zcpu.tzzmod.task;

public final class TaskConfiguratorClientAccess {
    private static Runnable opener = () -> {
    };

    private TaskConfiguratorClientAccess() {
    }

    public static void setOpener(Runnable screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen() {
        opener.run();
    }
}

