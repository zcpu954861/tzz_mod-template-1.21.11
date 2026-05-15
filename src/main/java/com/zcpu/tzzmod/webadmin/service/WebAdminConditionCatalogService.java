package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionFieldSchema;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.ConditionRegistry;
import com.zcpu.tzzmod.condition.ConditionTypeMetadata;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebAdminConditionCatalogService {
    private static final Set<String> CORE_TYPES = Set.of(
            ConditionNodeType.GROUP,
            ConditionNodeType.ALWAYS_TRUE,
            ConditionNodeType.ALWAYS_FALSE,
            ConditionNodeType.CONTEXT_EXISTS,
            ConditionNodeType.CONTEXT_FIELD_EXISTS,
            ConditionNodeType.CONTEXT_EQUALS
    );
    private static final Set<String> PLAYER_CONTEXT_TYPES = Set.of(
            ConditionNodeType.PLAYER_EXISTS,
            ConditionNodeType.PLAYER_ONLINE,
            ConditionNodeType.PLAYER_IS_OP,
            ConditionNodeType.PLAYER_HAS_TAG,
            ConditionNodeType.PLAYER_LACKS_TAG,
            ConditionNodeType.PLAYER_TEAM_EQUALS,
            ConditionNodeType.PLAYER_GAMEMODE_EQUALS,
            ConditionNodeType.PLAYER_ALIVE,
            ConditionNodeType.PLAYER_DEAD,
            ConditionNodeType.SOURCE_TYPE_EQUALS,
            ConditionNodeType.SOURCE_ID_EQUALS,
            ConditionNodeType.CHANNEL_EQUALS,
            ConditionNodeType.WORLD_EQUALS,
            ConditionNodeType.DEVICE_ID_EQUALS,
            ConditionNodeType.LISTENER_ID_EQUALS,
            ConditionNodeType.REGION_ID_EQUALS,
            ConditionNodeType.ACTION_ID_EQUALS,
            ConditionNodeType.GAME_TIME_COMPARE,
            ConditionNodeType.EVENT_METADATA_EXISTS,
            ConditionNodeType.EVENT_METADATA_EQUALS
    );
    private static final Set<String> STATE_TYPES = Set.of(
            ConditionNodeType.STATE_VARIABLE_EXISTS,
            ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
            ConditionNodeType.STATE_VARIABLE_INT_COMPARE,
            ConditionNodeType.STATE_VARIABLE_STRING_EQUALS,
            ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS
    );
    private static final Set<String> ITEM_TYPES = Set.of(
            ConditionNodeType.ITEM_STACK_EXISTS,
            ConditionNodeType.ITEM_STACK_MATCHES,
            ConditionNodeType.INVENTORY_CONTAINS_ITEM,
            ConditionNodeType.INVENTORY_ITEM_COUNT_COMPARE,
            ConditionNodeType.CONTAINER_SLOT_EMPTY,
            ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
            ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE
    );
    private static final Set<String> REGION_SIGNAL_LOGIC_TYPES = Set.of(
            ConditionNodeType.REGION_EXISTS,
            ConditionNodeType.REGION_ENABLED,
            ConditionNodeType.PLAYER_IN_REGION,
            ConditionNodeType.REGION_PLAYER_COUNT_COMPARE,
            ConditionNodeType.SIGNAL_CHANNEL_EXISTS,
            ConditionNodeType.SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE,
            ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
            ConditionNodeType.LOGIC_CHAIN_CONTAINS_NODE,
            ConditionNodeType.LOGIC_CHAIN_CONTAINS_CHANNEL,
            ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE,
            ConditionNodeType.LOGIC_CHAIN_NODE_COUNT_COMPARE
    );

    private final ConditionRegistry registry;

    public WebAdminConditionCatalogService() {
        this(ConditionRegistry.defaultRegistry());
    }

    public WebAdminConditionCatalogService(ConditionRegistry registry) {
        this.registry = registry == null ? ConditionRegistry.defaultRegistry() : registry;
    }

    public Map<String, Object> catalog() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> types = new ArrayList<>();
        for (ConditionTypeMetadata metadata : registry.metadata()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", metadata.type());
            entry.put("displayName", metadata.displayName());
            entry.put("description", metadata.description());
            entry.put("category", metadata.category());
            entry.put("suite", suite(metadata.type()));
            entry.put("fields", fields(metadata.fields()));
            entry.put("readOnly", true);
            types.add(entry);
        }
        result.put("types", types);
        result.put("count", types.size());
        result.put("readOnly", true);
        result.put("message", "条件类型目录只读；中文名称用于 WebAdmin 主文案，type id 仅作为技术副文本。");
        return result;
    }

    private static List<Map<String, Object>> fields(List<ConditionFieldSchema> raw) {
        List<Map<String, Object>> fields = new ArrayList<>();
        if (raw != null) {
            for (ConditionFieldSchema field : raw) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", field.name());
                entry.put("displayName", field.displayName());
                entry.put("kind", field.kind());
                entry.put("required", field.required());
                entry.put("description", field.description());
                entry.put("options", optionsForKind(field.kind()));
                fields.add(entry);
            }
        }
        return List.copyOf(fields);
    }

    private static List<String> optionsForKind(String kind) {
        String safe = kind == null ? "" : kind.trim();
        if (safe.startsWith("enum:")) {
            return List.of(safe.substring("enum:".length()).split(","));
        }
        if ("operator:eq,ne,gt,gte,lt,lte".equals(safe)) {
            return List.of("eq", "ne", "gt", "gte", "lt", "lte");
        }
        if ("boolean".equals(safe)) {
            return List.of("true", "false");
        }
        return List.of();
    }

    private static String suite(String type) {
        if (CORE_TYPES.contains(type)) {
            return "core";
        }
        if (PLAYER_CONTEXT_TYPES.contains(type)) {
            return "player-context";
        }
        if (STATE_TYPES.contains(type)) {
            return "state-variable";
        }
        if (ITEM_TYPES.contains(type)) {
            return "item-inventory-container";
        }
        if (REGION_SIGNAL_LOGIC_TYPES.contains(type)) {
            return "region-signal-logic-chain";
        }
        return "custom";
    }
}
