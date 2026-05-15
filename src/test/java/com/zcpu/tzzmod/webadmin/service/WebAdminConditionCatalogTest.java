package com.zcpu.tzzmod.webadmin.service;

import java.util.List;
import java.util.Map;

public final class WebAdminConditionCatalogTest {
    private WebAdminConditionCatalogTest() {
    }

    public static void run() {
        WebAdminConditionCatalogService service = new WebAdminConditionCatalogService();
        Map<String, Object> catalog = service.catalog();
        requireTrue(Boolean.TRUE.equals(catalog.get("readOnly")), "condition catalog is read-only");
        List<Map<String, Object>> types = castList(catalog.get("types"));

        // catalog includes 8.0 core condition types
        requireType(types, "always_true", "永远通过", "core");
        // catalog includes 8.1 player/context condition types
        requireType(types, "player_has_tag", "玩家拥有标签", "player-context");
        // catalog includes 8.2 state variable condition types
        requireType(types, "state_variable_bool_equals", "布尔状态匹配", "state-variable");
        // catalog includes 8.3 item/inventory/container condition types
        requireType(types, "inventory_contains_item", "背包包含物品", "item-inventory-container");
        // catalog includes 8.4 region/signal/logic chain condition types
        requireType(types, "region_exists", "区域快照存在", "region-signal-logic-chain");
        requireType(types, "logic_chain_contains_node", "逻辑链包含节点", "region-signal-logic-chain");
        requireField(types, "context_equals", "field", "上下文字段");
        requireField(types, "context_equals", "expected", "期望值");
        requireField(types, "state_variable_bool_equals", "expected", "期望值");
        requireField(types, "inventory_contains_item", "inventoryKey", "背包快照键");
        requireField(types, "container_slot_item_matches", "slot", "槽位");
        requireField(types, "region_enabled", "regionKey", "区域快照键");
        requireField(types, "signal_event_count_compare", "signalHistoryKey", "信号历史快照键");
        requireField(types, "logic_chain_has_cycle", "logicChainKey", "逻辑链快照键");

        for (Map<String, Object> type : types) {
            String displayName = string(type.get("displayName"));
            String description = string(type.get("description"));
            requireTrue(containsChinese(displayName), "condition type has Chinese display name: " + type.get("type"));
            requireTrue(containsChinese(description), "condition type has Chinese description: " + type.get("type"));
            for (Map<String, Object> field : castList(type.get("fields"))) {
                requireTrue(containsChinese(string(field.get("displayName"))), "condition field has Chinese display name: " + type.get("type") + "." + field.get("key"));
                String kind = string(field.get("kind"));
                if (kind.contains("eq,ne,gt,gte,lt,lte")) {
                    List<String> options = castStringList(field.get("options"));
                    requireTrue(options.containsAll(List.of("eq", "ne", "gt", "gte", "lt", "lte")), "operator field exposes compare options");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> castStringList(Object value) {
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }

    private static void requireType(List<Map<String, Object>> types, String typeId, String chineseName, String suite) {
        Map<String, Object> type = types.stream()
                .filter(entry -> typeId.equals(entry.get("type")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing condition type: " + typeId));
        requireEquals(chineseName, type.get("displayName"), "condition type Chinese name: " + typeId);
        requireEquals(suite, type.get("suite"), "condition type suite: " + typeId);
    }

    private static void requireField(List<Map<String, Object>> types, String typeId, String fieldKey, String chineseName) {
        Map<String, Object> type = types.stream()
                .filter(entry -> typeId.equals(entry.get("type")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing condition type for field check: " + typeId));
        Map<String, Object> field = castList(type.get("fields")).stream()
                .filter(entry -> fieldKey.equals(entry.get("key")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing condition field: " + typeId + "." + fieldKey));
        requireEquals(chineseName, field.get("displayName"), "condition field Chinese name: " + typeId + "." + fieldKey);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
