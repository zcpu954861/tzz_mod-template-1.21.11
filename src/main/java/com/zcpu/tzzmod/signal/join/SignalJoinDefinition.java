package com.zcpu.tzzmod.signal.join;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SignalJoinDefinition {
    public static final int DATA_VERSION = 1;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_.:-]{1,96}$");

    public String id = "";
    public String displayName = "";
    public String note = "";
    public boolean enabled = true;
    public List<SignalJoinInputDefinition> inputChannels = new ArrayList<>();
    public String outputChannel = "";
    public SignalJoinMode mode = SignalJoinMode.ALL;
    public int threshold = 2;
    public SignalJoinScopeMode scopeMode = SignalJoinScopeMode.GLOBAL;
    public SignalJoinResetPolicy resetPolicy = SignalJoinResetPolicy.RESET_AFTER_EMIT;
    public long timeoutTicks = 0L;
    public long cooldownTicks = 0L;
    public String createdAt = "";
    public String updatedAt = "";
    public String updatedBy = "";
    public long version = 0L;

    public SignalJoinDefinition normalized() {
        SignalJoinDefinition copy = new SignalJoinDefinition();
        copy.id = normalizeId(id);
        copy.displayName = clean(displayName, 64);
        copy.note = clean(note, 512);
        copy.enabled = enabled;
        copy.inputChannels = normalizeInputs(inputChannels);
        copy.outputChannel = SignalChannel.normalize(outputChannel);
        copy.mode = mode == null ? SignalJoinMode.ALL : mode;
        copy.scopeMode = scopeMode == null ? SignalJoinScopeMode.GLOBAL : scopeMode;
        copy.resetPolicy = resetPolicy == null ? SignalJoinResetPolicy.RESET_AFTER_EMIT : resetPolicy;
        copy.threshold = normalizeThreshold(copy.mode, threshold, copy.inputChannels.size());
        copy.timeoutTicks = Math.max(0L, timeoutTicks);
        copy.cooldownTicks = Math.max(0L, cooldownTicks);
        copy.createdAt = safe(createdAt);
        copy.updatedAt = safe(updatedAt);
        copy.updatedBy = safe(updatedBy);
        copy.version = Math.max(0L, version);
        return copy;
    }

    public SignalJoinDefinition withWriteMetadata(String actor, long nextVersion, boolean created) {
        SignalJoinDefinition copy = normalized();
        String now = java.time.Instant.now().toString();
        copy.createdAt = created || copy.createdAt.isBlank() ? now : copy.createdAt;
        copy.updatedAt = now;
        copy.updatedBy = safe(actor);
        copy.version = Math.max(1L, nextVersion);
        return copy;
    }

    public boolean referencesInput(String channel) {
        String normalized = SignalChannel.normalize(channel);
        for (SignalJoinInputDefinition input : normalized().inputChannels) {
            if (input.channel.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public List<String> inputChannelNames() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SignalJoinInputDefinition input : normalized().inputChannels) {
            if (!input.channel.isBlank()) {
                result.add(input.channel);
            }
        }
        return List.copyOf(result);
    }

    public static String normalizeId(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        String normalized = builder.toString();
        if (normalized.length() > 96) {
            normalized = normalized.substring(0, 96);
        }
        return ID_PATTERN.matcher(normalized).matches() ? normalized : "";
    }

    static int normalizeThreshold(SignalJoinMode mode, int rawThreshold, int inputCount) {
        SignalJoinMode safeMode = mode == null ? SignalJoinMode.ALL : mode;
        if (safeMode == SignalJoinMode.ALL) {
            return Math.max(1, inputCount);
        }
        return Math.max(1, rawThreshold);
    }

    private static List<SignalJoinInputDefinition> normalizeInputs(List<SignalJoinInputDefinition> inputs) {
        List<SignalJoinInputDefinition> result = new ArrayList<>();
        if (inputs != null) {
            for (SignalJoinInputDefinition input : inputs) {
                if (input == null) {
                    continue;
                }
                result.add(input.normalized());
            }
        }
        return List.copyOf(result);
    }

    private static String clean(String raw, int maxLength) {
        String value = safe(raw).trim();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
