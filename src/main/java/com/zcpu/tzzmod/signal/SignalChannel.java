package com.zcpu.tzzmod.signal;

import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.text.Text;

public final class SignalChannel {
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^[a-z0-9_.:-]{1,128}$");

    private SignalChannel() {
    }

    public static String normalize(String channel) {
        return channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String channel) {
        return CHANNEL_PATTERN.matcher(normalize(channel)).matches();
    }

    public static Text validationError(String channel) {
        return Text.literal("频道名称无效：" + (channel == null ? "" : channel)
                + "。允许字符：小写字母、数字、_、-、.、:；长度限制：1-128。");
    }
}
