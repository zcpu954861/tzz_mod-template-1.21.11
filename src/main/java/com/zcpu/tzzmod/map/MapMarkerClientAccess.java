package com.zcpu.tzzmod.map;

public final class MapMarkerClientAccess {
    private static Runnable opener = () -> {
    };

    private MapMarkerClientAccess() {
    }

    public static void setOpener(Runnable screenOpener) {
        opener = screenOpener;
    }

    public static void openScreen() {
        opener.run();
    }
}