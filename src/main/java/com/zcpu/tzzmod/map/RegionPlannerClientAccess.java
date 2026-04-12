package com.zcpu.tzzmod.map;

public final class RegionPlannerClientAccess {
    private static Runnable opener = () -> {
    };

    private RegionPlannerClientAccess() {
    }

    public static void setOpener(Runnable screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen() {
        opener.run();
    }
}