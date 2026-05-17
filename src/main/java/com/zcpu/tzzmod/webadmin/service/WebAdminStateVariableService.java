package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.state.StateVariableKey;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableService;
import com.zcpu.tzzmod.condition.state.StateVariableSnapshot;
import com.zcpu.tzzmod.condition.state.StateVariableStore;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminStateVariableService {
    private final WebAdminPermissionService permissionService;
    private final Path testStorePath;

    public WebAdminStateVariableService(WebAdminPermissionService permissionService) {
        this(permissionService, null);
    }

    public WebAdminStateVariableService(Path testStorePath) {
        this(new WebAdminPermissionService(), testStorePath);
    }

    private WebAdminStateVariableService(WebAdminPermissionService permissionService, Path testStorePath) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.testStorePath = testStorePath;
    }

    public WebAdminDtos.StateVariableListDto list(MinecraftServer server, WebAdminUser user, Map<String, String> query) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed()) {
            return emptyList(false, decision.message());
        }
        StateVariableStore.StateVariableLoadResult loaded = load(server);
        List<WebAdminDtos.StateVariableListEntryDto> variables = loaded.snapshot().records().stream()
                .filter(this::supportedScope)
                .filter(record -> matches(record, query))
                .sorted(Comparator.comparing(StateVariableRecord::updatedAt).reversed()
                        .thenComparing(StateVariableRecord::id))
                .limit(limit(query))
                .map(this::entry)
                .toList();
        return new WebAdminDtos.StateVariableListDto(
                variables,
                variables.size(),
                summary(loaded.snapshot()),
                true,
                StateVariableStore.FILE_NAME,
                loaded.filePresent(),
                loaded.degraded(),
                loaded.message(),
                true,
                List.of(StateVariableScope.GLOBAL.name(), StateVariableScope.PLAYER.name()),
                List.of(StateVariableType.BOOLEAN.name(), StateVariableType.INTEGER.name(), StateVariableType.STRING.name())
        );
    }

    public WebAdminDtos.StateVariableDetailDto detail(MinecraftServer server, WebAdminUser user, String id) {
        WebAdminPermissionDecision decision = permissionService.decide(user, WebAdminOperationType.READ);
        if (!decision.allowed() || id == null || id.isBlank()) {
            return null;
        }
        return load(server).snapshot().records().stream()
                .filter(this::supportedScope)
                .filter(record -> record.id().equals(id))
                .findFirst()
                .map(this::detail)
                .orElse(null);
    }

    private StateVariableStore.StateVariableLoadResult load(MinecraftServer server) {
        if (testStorePath != null) {
            return new StateVariableService(testStorePath).snapshotWithStatus();
        }
        return StateVariableStore.getSnapshotWithStatus(server);
    }

    private WebAdminDtos.StateVariableListDto emptyList(boolean storePresent, String message) {
        return new WebAdminDtos.StateVariableListDto(
                List.of(),
                0,
                new WebAdminDtos.StateVariableSummaryDto(0, 0, 0, 0, 0, 0),
                true,
                StateVariableStore.FILE_NAME,
                storePresent,
                false,
                safe(message),
                true,
                List.of(StateVariableScope.GLOBAL.name(), StateVariableScope.PLAYER.name()),
                List.of(StateVariableType.BOOLEAN.name(), StateVariableType.INTEGER.name(), StateVariableType.STRING.name())
        );
    }

    private WebAdminDtos.StateVariableSummaryDto summary(StateVariableSnapshot snapshot) {
        List<StateVariableRecord> records = snapshot == null ? List.of() : snapshot.records().stream()
                .filter(this::supportedScope)
                .toList();
        return new WebAdminDtos.StateVariableSummaryDto(
                records.size(),
                count(records, record -> record.scope() == StateVariableScope.GLOBAL),
                count(records, record -> record.scope() == StateVariableScope.PLAYER),
                count(records, record -> record.type() == StateVariableType.BOOLEAN),
                count(records, record -> record.type() == StateVariableType.INTEGER),
                count(records, record -> record.type() == StateVariableType.STRING)
        );
    }

    private WebAdminDtos.StateVariableListEntryDto entry(StateVariableRecord record) {
        return new WebAdminDtos.StateVariableListEntryDto(
                record.id(),
                record.scope().name(),
                record.scope().displayName(),
                record.targetId(),
                targetLabel(record),
                record.key(),
                record.type().name(),
                record.type().displayName(),
                typedValue(record),
                record.value(),
                valuePreview(record.value()),
                record.value().length(),
                record.version(),
                record.fingerprint(),
                shortFingerprint(record.fingerprint()),
                WebAdminReadonlySupport.isoTime(record.updatedAt()),
                record.updatedBy(),
                new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath(),
                "#/state-variables/" + URLEncoder.encode(record.id(), StandardCharsets.UTF_8)
        );
    }

    private WebAdminDtos.StateVariableDetailDto detail(StateVariableRecord record) {
        WebAdminDtos.StateVariableListEntryDto entry = entry(record);
        Map<String, String> copyTargets = new LinkedHashMap<>();
        copyTargets.put("key", record.key());
        copyTargets.put("targetId", record.targetId());
        copyTargets.put("displayPath", entry.displayPath());
        return new WebAdminDtos.StateVariableDetailDto(
                entry.id(),
                entry.scope(),
                entry.scopeLabel(),
                entry.targetId(),
                entry.targetLabel(),
                entry.key(),
                entry.type(),
                entry.typeLabel(),
                entry.value(),
                entry.valueText(),
                entry.valuePreview(),
                entry.valueLength(),
                entry.version(),
                entry.fingerprint(),
                entry.fingerprintShort(),
                entry.updatedAt(),
                entry.updatedBy(),
                "",
                entry.displayPath(),
                "world/tzz/webadmin/" + StateVariableStore.FILE_NAME,
                true,
                copyTargets,
                conditionSuggestion(record)
        );
    }

    private Map<String, Object> conditionSuggestion(StateVariableRecord record) {
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("scope", record.scope().name());
        suggestion.put("targetMode", record.scope() == StateVariableScope.GLOBAL ? "global" : "explicit_target");
        suggestion.put("targetId", record.targetId());
        suggestion.put("key", record.key());
        suggestion.put("type", switch (record.type()) {
            case BOOLEAN -> "state_variable_bool_equals";
            case INTEGER -> "state_variable_int_compare";
            case STRING -> "state_variable_string_equals";
        });
        return suggestion;
    }

    private boolean matches(StateVariableRecord record, Map<String, String> query) {
        String scope = queryValue(query, "scope").toUpperCase(Locale.ROOT);
        if (!scope.isBlank() && !"ALL".equals(scope) && !record.scope().name().equals(scope)) {
            return false;
        }
        String type = queryValue(query, "type").toUpperCase(Locale.ROOT);
        if (!type.isBlank() && !"ALL".equals(type) && !record.type().name().equals(type)) {
            return false;
        }
        String target = firstNonBlank(queryValue(query, "targetId"), queryValue(query, "target")).toLowerCase(Locale.ROOT);
        if (!target.isBlank() && !record.targetId().toLowerCase(Locale.ROOT).contains(target)) {
            return false;
        }
        String q = firstNonBlank(queryValue(query, "q"), queryValue(query, "search")).toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return true;
        }
        String haystack = String.join(" ",
                record.id(),
                record.scope().name(),
                record.targetId(),
                record.key(),
                record.type().name(),
                record.value(),
                new StateVariableKey(record.scope(), record.targetId(), record.key()).displayPath()
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(q);
    }

    private int limit(Map<String, String> query) {
        String raw = queryValue(query, "limit");
        if (raw.isBlank()) {
            return WebAdminReadonlySupport.MAX_LIST_LIMIT;
        }
        try {
            return WebAdminReadonlySupport.limit(Integer.parseInt(raw.trim()), WebAdminReadonlySupport.MAX_LIST_LIMIT);
        } catch (NumberFormatException ignored) {
            return WebAdminReadonlySupport.MAX_LIST_LIMIT;
        }
    }

    private boolean supportedScope(StateVariableRecord record) {
        return record != null && (record.scope() == StateVariableScope.GLOBAL || record.scope() == StateVariableScope.PLAYER);
    }

    private Object typedValue(StateVariableRecord record) {
        return switch (record.type()) {
            case BOOLEAN -> Boolean.parseBoolean(record.value());
            case INTEGER -> {
                try {
                    yield Long.parseLong(record.value());
                } catch (NumberFormatException ignored) {
                    yield record.value();
                }
            }
            case STRING -> record.value();
        };
    }

    private static String valuePreview(String value) {
        String safe = safe(value).replace("\r", " ").replace("\n", " ");
        return safe.length() <= 96 ? safe : safe.substring(0, 93) + "...";
    }

    private static String targetLabel(StateVariableRecord record) {
        if (record.scope() == StateVariableScope.GLOBAL) {
            return "全局";
        }
        return record.targetId().isBlank() ? "未指定目标" : "目标 ID: " + record.targetId();
    }

    private static String shortFingerprint(String fingerprint) {
        String safe = safe(fingerprint);
        return safe.length() <= 12 ? safe : safe.substring(0, 12);
    }

    private static String queryValue(Map<String, String> query, String key) {
        return query == null ? "" : safe(query.get(key));
    }

    private static String firstNonBlank(String first, String second) {
        return !safe(first).isBlank() ? safe(first) : safe(second);
    }

    private static int count(List<StateVariableRecord> records, java.util.function.Predicate<StateVariableRecord> predicate) {
        return (int) records.stream().filter(predicate).count();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
