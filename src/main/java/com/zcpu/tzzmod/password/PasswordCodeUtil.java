package com.zcpu.tzzmod.password;

public final class PasswordCodeUtil {
    public static final String DEFAULT_CODE = "0000";
    public static final int CODE_LENGTH = 4;

    private PasswordCodeUtil() {
    }

    public static boolean isValid(String code) {
        return code != null && code.matches("\\d{" + CODE_LENGTH + "}");
    }

    public static String normalize(String code) {
        if (isValid(code)) {
            return code;
        }
        if (code == null) {
            return DEFAULT_CODE;
        }

        String digitsOnly = code.replaceAll("\\D", "");
        if (digitsOnly.length() >= CODE_LENGTH) {
            return digitsOnly.substring(0, CODE_LENGTH);
        }
        return String.format("%1$" + CODE_LENGTH + "s", digitsOnly).replace(' ', '0');
    }
}

