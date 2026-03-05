package com.zcpu.tzzmod.phone;

public final class PhoneClientAccess {
    private static Runnable opener = () -> {
    };

    private PhoneClientAccess() {
    }

    public static void setOpener(Runnable screenOpener) {
        opener = screenOpener;
    }

    public static void openPhoneScreen() {
        opener.run();
    }
}

