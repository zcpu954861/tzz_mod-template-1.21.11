package com.zcpu.tzzmod.webadmin.template;

import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WebAdminTemplatePackage {
    public static final String SCHEMA = "tzz_template_v1";

    public String schema = "";
    public String templateId = "";
    public String displayName = "";
    public String description = "";
    public String category = "";
    public int version = 1;
    public String author = "local";
    public long createdAt = 0L;
    public long updatedAt = 0L;
    public String iconKey = "template-package";
    public List<Parameter> parameters = new ArrayList<>();
    public Resources resources = new Resources();
    public Metadata metadata = new Metadata();

    public WebAdminTemplatePackage normalized() {
        WebAdminTemplatePackage copy = new WebAdminTemplatePackage();
        copy.schema = safe(schema).isBlank() ? SCHEMA : safe(schema).trim();
        copy.templateId = normalizeId(templateId);
        copy.displayName = clean(displayName, 80);
        copy.description = clean(description, 512);
        copy.category = clean(category, 64);
        copy.version = Math.max(1, version);
        copy.author = clean(author, 64).isBlank() ? "local" : clean(author, 64);
        copy.createdAt = Math.max(0L, createdAt);
        copy.updatedAt = Math.max(0L, updatedAt);
        copy.iconKey = clean(iconKey, 64).isBlank() ? "template-package" : clean(iconKey, 64);
        copy.parameters = normalizeParameters(parameters);
        copy.resources = resources == null ? new Resources() : resources.normalized();
        copy.metadata = metadata == null ? new Metadata() : metadata.normalized();
        return copy;
    }

    public List<String> validationErrors() {
        WebAdminTemplatePackage normalized = normalized();
        List<String> errors = new ArrayList<>();
        String rawSchema = safe(schema).trim();
        if (rawSchema.isBlank()) {
            errors.add("模板 schema 不能为空。");
        } else if (!SCHEMA.equals(rawSchema)) {
            errors.add("模板版本不受支持：" + rawSchema);
        }
        if (normalized.templateId.isBlank()) {
            errors.add("模板 ID 不能为空，且只能包含小写字母、数字、点、下划线、短横线或冒号。");
        }
        if (normalized.displayName.isBlank()) {
            errors.add("模板显示名称不能为空。");
        }
        if (normalized.category.isBlank()) {
            errors.add("模板分类不能为空。");
        }
        if (normalized.resources.resourceCount() == 0) {
            errors.add("模板至少需要包含一个可识别资源。");
        }
        return errors;
    }

    public int resourceCount() {
        return normalized().resources.resourceCount();
    }

    public boolean hasPlaceholders() {
        return !normalized().resources.placeholders.isEmpty();
    }

    public static String normalizeId(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length() && builder.length() < 96; index++) {
            char c = value.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == ':') {
                builder.append(c);
            } else if (Character.isWhitespace(c)) {
                builder.append('-');
            }
        }
        return builder.toString();
    }

    private static List<Parameter> normalizeParameters(List<Parameter> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Parameter> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Parameter parameter : raw) {
            Parameter normalized = (parameter == null ? new Parameter() : parameter).normalized();
            if (!normalized.key.isBlank() && seen.add(normalized.key)) {
                result.add(normalized);
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

    public static final class Parameter {
        public String key = "";
        public String displayName = "";
        public String type = "text";
        public String defaultValue = "";
        public boolean required = false;
        public String description = "";

        public Parameter normalized() {
            Parameter copy = new Parameter();
            copy.key = normalizeId(key);
            copy.displayName = clean(displayName, 64);
            copy.type = clean(type, 32).isBlank() ? "text" : clean(type, 32).toLowerCase(Locale.ROOT);
            copy.defaultValue = clean(defaultValue, 160);
            copy.required = required;
            copy.description = clean(description, 256);
            return copy;
        }
    }

    public static final class Resources {
        public List<ChannelResource> channels = new ArrayList<>();
        public List<SignalJoinResource> signalJoins = new ArrayList<>();
        public List<TimerResource> timers = new ArrayList<>();
        public List<SignalListenerResource> signalListeners = new ArrayList<>();
        public List<ActionResource> actions = new ArrayList<>();
        public List<Object> stateVariables = new ArrayList<>();
        public List<Object> conditionGroups = new ArrayList<>();
        public List<Placeholder> placeholders = new ArrayList<>();

        public Resources normalized() {
            Resources copy = new Resources();
            copy.channels = normalizeList(channels, ChannelResource::normalized);
            copy.signalJoins = normalizeList(signalJoins, SignalJoinResource::normalized);
            copy.timers = normalizeList(timers, TimerResource::normalized);
            copy.signalListeners = normalizeList(signalListeners, SignalListenerResource::normalized);
            copy.actions = normalizeList(actions, ActionResource::normalized);
            copy.stateVariables = stateVariables == null ? List.of() : List.copyOf(stateVariables);
            copy.conditionGroups = conditionGroups == null ? List.of() : List.copyOf(conditionGroups);
            copy.placeholders = normalizeList(placeholders, Placeholder::normalized);
            return copy;
        }

        public int resourceCount() {
            return safeSize(channels)
                    + safeSize(signalJoins)
                    + safeSize(timers)
                    + safeSize(signalListeners)
                    + safeSize(actions)
                    + safeSize(stateVariables)
                    + safeSize(conditionGroups);
        }

        private static int safeSize(List<?> list) {
            return list == null ? 0 : list.size();
        }
    }

    private interface Normalizer<T> {
        T normalize(T value);
    }

    private static <T> List<T> normalizeList(List<T> raw, Normalizer<T> normalizer) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (T item : raw) {
            if (item != null) {
                T normalized = normalizer.normalize(item);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        }
        return List.copyOf(result);
    }

    public static final class ChannelResource {
        public String id = "";
        public String displayName = "";
        public String note = "";
        public String iconKey = "auto";

        public ChannelResource normalized() {
            ChannelResource copy = new ChannelResource();
            copy.id = normalizeId(id);
            copy.displayName = clean(displayName, 64);
            copy.note = clean(note, 256);
            copy.iconKey = clean(iconKey, 64).isBlank() ? "auto" : clean(iconKey, 64);
            return copy.id.isBlank() ? null : copy;
        }
    }

    public static final class SignalJoinResource {
        public String id = "";
        public SignalJoinDefinition definition = new SignalJoinDefinition();

        public SignalJoinResource normalized() {
            SignalJoinResource copy = new SignalJoinResource();
            copy.id = normalizeId(id);
            copy.definition = definition == null ? new SignalJoinDefinition() : definition.normalized();
            if (copy.definition.id.isBlank()) {
                copy.definition.id = copy.id;
            }
            return copy.id.isBlank() ? null : copy;
        }
    }

    public static final class TimerResource {
        public String id = "";
        public TimerDefinition definition = new TimerDefinition();

        public TimerResource normalized() {
            TimerResource copy = new TimerResource();
            copy.id = normalizeId(id);
            copy.definition = definition == null ? new TimerDefinition() : definition.normalized();
            if (copy.definition.id.isBlank()) {
                copy.definition.id = copy.id;
            }
            return copy.id.isBlank() ? null : copy;
        }
    }

    public static final class SignalListenerResource {
        public String id = "";
        public SignalListenerData listener = new SignalListenerData("", "", "", true, SignalListenerData.DEFAULT_COOLDOWN_TICKS, "", List.of());

        public SignalListenerResource normalized() {
            SignalListenerResource copy = new SignalListenerResource();
            copy.id = normalizeId(id);
            SignalListenerData raw = listener == null
                    ? new SignalListenerData("", "", "", true, SignalListenerData.DEFAULT_COOLDOWN_TICKS, "", List.of())
                    : listener;
            copy.listener = new SignalListenerData(
                    raw.id().isBlank() ? copy.id : raw.id(),
                    clean(raw.name(), 64),
                    SignalChannel.normalize(raw.channel()),
                    raw.enabled(),
                    raw.cooldownTicks(),
                    raw.conditionGroupId(),
                    raw.actions()
            ).normalized();
            return copy.id.isBlank() ? null : copy;
        }
    }

    public static final class ActionResource {
        public String id = "";
        public String ownerType = "";
        public String ownerId = "";
        public String bucket = "";

        public ActionResource normalized() {
            ActionResource copy = new ActionResource();
            copy.id = normalizeId(id);
            copy.ownerType = clean(ownerType, 48).toLowerCase(Locale.ROOT);
            copy.ownerId = normalizeId(ownerId);
            copy.bucket = clean(bucket, 48).toLowerCase(Locale.ROOT);
            return copy;
        }
    }

    public static final class Placeholder {
        public String id = "";
        public String type = "";
        public String displayName = "";
        public String description = "";
        public boolean required = true;

        public Placeholder normalized() {
            Placeholder copy = new Placeholder();
            copy.id = normalizeId(id);
            copy.type = clean(type, 48).toLowerCase(Locale.ROOT);
            copy.displayName = clean(displayName, 64);
            copy.description = clean(description, 256);
            copy.required = required;
            return copy.id.isBlank() ? null : copy;
        }
    }

    public static final class Metadata {
        public String source = "user";
        public List<String> notes = new ArrayList<>();
        public List<String> compatibility = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public Metadata normalized() {
            Metadata copy = new Metadata();
            String normalizedSource = clean(source, 32).toLowerCase(Locale.ROOT);
            copy.source = switch (normalizedSource) {
                case "built_in", "imported", "exported_component" -> normalizedSource;
                default -> "user";
            };
            copy.notes = cleanStringList(notes, 20, 160);
            copy.compatibility = cleanStringList(compatibility, 20, 160);
            copy.warnings = cleanStringList(warnings, 20, 160);
            return copy;
        }

        private static List<String> cleanStringList(List<String> raw, int maxItems, int maxLength) {
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (String item : raw) {
                String value = clean(item, maxLength);
                if (!value.isBlank() && result.size() < maxItems) {
                    result.add(value);
                }
            }
            return List.copyOf(result);
        }
    }
}
