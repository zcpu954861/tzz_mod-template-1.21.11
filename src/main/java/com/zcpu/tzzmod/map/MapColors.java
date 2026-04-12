package com.zcpu.tzzmod.map;

public final class MapColors {
    public static final int[] MARKER_PALETTE = {
        0xFFF55D5D,
        0xFFFF8A5B,
        0xFFFFC857,
        0xFFEAF27C,
        0xFF9BE564,
        0xFF57CC99,
        0xFF38A3A5,
        0xFF4DABF7,
        0xFF5C7CFA,
        0xFF9775FA,
        0xFFC77DFF,
        0xFFFF75C3,
        0xFFF783AC,
        0xFFD4A373,
        0xFFADB5BD,
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