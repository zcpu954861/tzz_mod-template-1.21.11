package com.zcpu.tzzmod.signal.join;

import com.zcpu.tzzmod.signal.SignalChannel;

public final class SignalJoinInputDefinition {
    public String channel = "";
    public String displayName = "";
    public String note = "";
    public int requiredCount = 1;

    public SignalJoinInputDefinition() {
    }

    public SignalJoinInputDefinition(String channel, String displayName, String note, int requiredCount) {
        this.channel = channel;
        this.displayName = displayName;
        this.note = note;
        this.requiredCount = requiredCount;
    }

    public SignalJoinInputDefinition normalized() {
        SignalJoinInputDefinition copy = new SignalJoinInputDefinition();
        copy.channel = SignalChannel.normalize(channel);
        copy.displayName = clean(displayName, 64);
        copy.note = clean(note, 256);
        copy.requiredCount = Math.max(1, requiredCount);
        return copy;
    }

    private static String clean(String raw, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
