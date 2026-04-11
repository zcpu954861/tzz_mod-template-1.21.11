package com.zcpu.tzzmod.map;

public final class MapColors {
    public static final int[] MARKER_PALETTE = {
            0xFFF55D5D,
            0xFFFFA94D,
            0xFFFFE066,
            0xFF6BDC8A,
            0xFF4DABF7,
            0xFF9775FA,
            0xFFFF75C3,
            0xFFE9ECEF
    };

    private MapColors() {
    }

    public static int paletteColor(int index) {
        if (MARKER_PALETTE.length == 0) {
            return 0xFFE9ECEF;
        }
        int safeIndex = Math.floorMod(index, MARKER_PALETTE.length);
        return MARKER_PALETTE[safeIndex];
    }
}