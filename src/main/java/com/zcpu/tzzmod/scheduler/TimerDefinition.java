package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TimerDefinition {
    public static final int DATA_VERSION = 1;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_.:-]{1,96}$");

    public String id = "";
    public String displayName = "";
    public String note = "";
    public boolean enabled = true;
    public TimerMode mode = TimerMode.DELAY;
    public TimerScopeMode scopeMode = TimerScopeMode.GLOBAL;
    public long durationTicks = 20L;
    public long intervalTicks = 20L;
    public int maxRuns = 1;
    public TimerStartPolicy startPolicy = TimerStartPolicy.RESTART;
    public List<ActionConfig> onStartActions = new ArrayList<>();
    public List<ActionConfig> onTickActions = new ArrayList<>();
    public List<ActionConfig> onCompleteActions = new ArrayList<>();
    public List<ActionConfig> onCancelActions = new ArrayList<>();
    public String outputChannel = "";
    public String createdAt = "";
    public String updatedAt = "";
    public String updatedBy = "";
    public long version = 0L;

    public TimerDefinition normalized() {
        TimerDefinition copy = new TimerDefinition();
        copy.id = normalizeId(id);
        copy.displayName = clean(displayName, 64);
        copy.note = clean(note, 512);
        copy.enabled = enabled;
        copy.mode = mode == null ? TimerMode.DELAY : mode;
        copy.scopeMode = scopeMode == null ? TimerScopeMode.GLOBAL : scopeMode;
        copy.durationTicks = Math.max(0L, durationTicks);
        copy.intervalTicks = Math.max(0L, intervalTicks);
        copy.maxRuns = Math.max(0, maxRuns);
        copy.startPolicy = startPolicy == null ? TimerStartPolicy.RESTART : startPolicy;
        copy.onStartActions = normalizeActions(onStartActions);
        copy.onTickActions = normalizeActions(onTickActions);
        copy.onCompleteActions = normalizeActions(onCompleteActions);
        copy.onCancelActions = normalizeActions(onCancelActions);
        copy.outputChannel = SignalChannel.normalize(outputChannel);
        copy.createdAt = safe(createdAt);
        copy.updatedAt = safe(updatedAt);
        copy.updatedBy = safe(updatedBy);
        copy.version = Math.max(0L, version);
        return copy;
    }

    public TimerDefinition withWriteMetadata(String actor, long nextVersion, boolean created) {
        TimerDefinition copy = normalized();
        String now = java.time.Instant.now().toString();
        copy.createdAt = created || copy.createdAt.isBlank() ? now : copy.createdAt;
        copy.updatedAt = now;
        copy.updatedBy = safe(actor);
        copy.version = Math.max(1L, nextVersion);
        return copy;
    }

    public boolean hasAnyOutputOrAction() {
        TimerDefinition timer = normalized();
        return !timer.outputChannel.isBlank()
                || !timer.onStartActions.isEmpty()
                || !timer.onTickActions.isEmpty()
                || !timer.onCompleteActions.isEmpty()
                || !timer.onCancelActions.isEmpty();
    }

    public boolean hasTickOrCompleteOutput() {
        TimerDefinition timer = normalized();
        return !timer.outputChannel.isBlank()
                || !timer.onTickActions.isEmpty()
                || !timer.onCompleteActions.isEmpty();
    }

    public static String normalizeId(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
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

    private static List<ActionConfig> normalizeActions(List<ActionConfig> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<ActionConfig> result = new ArrayList<>();
        for (ActionConfig action : actions) {
            if (action != null) {
                result.add(action.normalized());
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
