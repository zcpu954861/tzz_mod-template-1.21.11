package com.zcpu.tzzmod.webadmin.service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class WebAdminHelpCatalogServiceTest {
    private WebAdminHelpCatalogServiceTest() {
    }

    public static void run() {
        Map<String, Object> catalog = new WebAdminHelpCatalogService().catalog();
        requireEquals("8.20-pre9-stabilization", string(catalog.get("version")), "8.20 stabilized catalog version");
        requireEquals(Boolean.TRUE, catalog.get("readOnly"), "8.17 help catalog is read-only");
        requireEquals(Boolean.TRUE, catalog.get("noWriteApi"), "8.17 help catalog exposes no write API");
        requireEquals(Boolean.TRUE, catalog.get("copyOnly"), "8.17 help examples are copy-only documentation");
        requireEquals(Boolean.FALSE, catalog.get("worldScoped"), "8.17 builtin help catalog is world independent");

        List<?> categories = list(catalog.get("categories"));
        List<?> topics = list(catalog.get("topics"));
        List<?> examples = list(catalog.get("examples"));
        List<?> troubleshooting = list(catalog.get("troubleshooting"));
        List<?> glossary = list(catalog.get("glossary"));
        Set<String> categoryIds = idSet(categories);
        Set<String> topicIds = idSet(topics);
        Set<String> exampleIds = idSet(examples);
        Set<String> troubleshootingIds = idSet(troubleshooting);
        Set<String> glossaryIds = idSet(glossary);

        requireTrue(categories.size() >= 10, "8.17 category count");
        requireTrue(examples.size() >= 6, "8.17 example count");
        requireTrue(troubleshooting.size() >= 12, "8.20 troubleshooting count includes Snapshot/Rollback");
        requireTrue(glossary.size() >= 24, "8.20 glossary count includes Snapshot/Rollback");

        for (String id : List.of(
                "getting-started.overview",
                "signalbridge.channel-basics",
                "signalbridge.listener-flow",
                "action.config-basics",
                "condition.group-basics",
                "state-variable.basics",
                "signal-join.basics",
                "timer.delay",
                "logic-chain.viewer",
                "logic-chain.editor-draft",
                "templates.prefab",
                "snapshot.rollback",
                "debugger.doctor-replay",
                "device-trigger.references",
                "region.controller"
        )) {
            Map<?, ?> topic = find(topics, id);
            requireTrue(topic != null, "required help topic exists: " + id);
            requireNotBlank(string(topic.get("basicTitle")), "topic basic title: " + id);
            requireNotBlank(string(topic.get("professionalTitle")), "topic professional title: " + id);
            requireTrue(!list(topic.get("basicSections")).isEmpty(), "topic basic sections: " + id);
            requireTrue(!list(topic.get("professionalSections")).isEmpty(), "topic professional sections: " + id);
            requireTrue(!list(topic.get("pageLinks")).isEmpty(), "topic page links: " + id);
        }

        for (String id : List.of(
                "example.join-two-inputs",
                "example.timer-delay-channel",
                "example.listener-message",
                "example.listener-state-variable",
                "example.condition-controls-action",
                "example.template-join-timer-listener",
                "example.signal-no-consumer",
                "example.template-import-vs-apply",
                "example.editor-draft-join-timer",
                "example.snapshot-dry-run-rollback"
        )) {
            Map<?, ?> example = find(examples, id);
            requireTrue(example != null, "required example exists: " + id);
            requireTrue(!list(example.get("steps")).isEmpty(), "example has steps: " + id);
            requireEquals(Boolean.TRUE, example.get("readOnlyExample"), "example remains documentation-only: " + id);
        }

        for (String id : List.of(
                "trouble.condition-not-selectable",
                "trouble.join-no-output",
                "trouble.timer-not-triggered",
                "trouble.listener-action-not-executed",
                "trouble.template-apply-conflict",
                "trouble.logic-chain-one-entry-many-channels",
                "trouble.editor-save-failed",
                "trouble.node-hidden-missing",
                "trouble.readonly-nodes",
                "trouble.import-json-no-effect",
                "trouble.blank-gate-no-history",
                "trouble.state-variable-action-failed",
                "trouble.signal-no-consumer",
                "trouble.snapshot-degraded",
                "trouble.rollback-operation-diff",
                "trouble.snapshot-retention"
        )) {
            Map<?, ?> item = find(troubleshooting, id);
            requireTrue(item != null, "required troubleshooting exists: " + id);
            requireTrue(!list(item.get("likelyCauses")).isEmpty(), "troubleshooting has likely causes: " + id);
            requireTrue(!list(item.get("checks")).isEmpty(), "troubleshooting has checks: " + id);
            requireTrue(!list(item.get("fixHints")).isEmpty(), "troubleshooting has fix hints: " + id);
        }

        for (String id : List.of(
                "channel",
                "logic-chain",
                "focus-channel",
                "associated-component",
                "signalbridge",
                "signal-listener",
                "action",
                "action-config",
                "condition-group",
                "state-variable",
                "state-action",
                "join",
                "barrier",
                "aggregator",
                "timer",
                "scheduler",
                "template",
                "prefab",
                "snapshot",
                "rollback",
                "pre-rollback",
                "operation-diff",
                "edit-lock",
                "fingerprint",
                "dry-run",
                "placeholder",
                "runtime-gate",
                "action-gate",
                "doctor",
                "debugger",
                "replay",
                "vbd",
                "action-relay",
                "signal-receiver",
                "region"
        )) {
            requireTrue(find(glossary, id) != null, "required glossary term exists: " + id);
        }

        for (Object item : list(catalog.get("featuredTopicIds"))) {
            requireTrue(topicIds.contains(string(item)), "featured topic id resolves: " + item);
        }
        for (Object raw : topics) {
            Map<?, ?> topic = requireMap(raw, "topic entry");
            String topicId = string(topic.get("id"));
            requireTrue(categoryIds.contains(string(topic.get("category"))), "topic category resolves: " + topicId);
            requireReferences(exampleIds, list(topic.get("examples")), "topic examples resolve: " + topicId);
            requireReferences(troubleshootingIds, list(topic.get("troubleshootingLinks")), "topic troubleshooting resolves: " + topicId);
            requireReferences(glossaryIds, list(topic.get("glossaryTerms")), "topic glossary terms resolve: " + topicId);
            requireReferences(topicIds, list(topic.get("relatedTopics")), "topic related topics resolve: " + topicId);
            requireInternalRoutes(list(topic.get("pageLinks")), "topic page links stay internal: " + topicId);
        }
        for (Object raw : examples) {
            Map<?, ?> example = requireMap(raw, "example entry");
            String exampleId = string(example.get("id"));
            requireReferences(topicIds, list(example.get("relatedTopicIds")), "example related topics resolve: " + exampleId);
            requireInternalRoutes(list(example.get("relatedRoutes")), "example related routes stay internal: " + exampleId);
        }
        for (Object raw : troubleshooting) {
            Map<?, ?> item = requireMap(raw, "troubleshooting entry");
            requireInternalRoutes(list(item.get("relatedRoutes")), "troubleshooting routes stay internal: " + item.get("id"));
        }

        String all = catalog.toString();
        for (String marker : List.of(
                "ConditionEngine 只判断，不写状态，不发信号，不执行动作。",
                "SignalBridge 是事件总线，不是状态数据库。",
                "StateVariable 保存状态。",
                "GameController / MissionSystem / PhaseController deferred",
                "full Logic Chain Editor deferred",
                "Scratch editor deferred",
                "if / else runtime deferred",
                "world entity in-editor draft create and binding deferred",
                "old node move / delete / reorder deferred",
                "old action delete / reorder deferred",
                "new write API for notes / favorites deferred",
                "placeholder binding apply deferred",
                "component export deferred",
                "ConditionGroup apply deferred",
                "StateVariable definition apply deferred",
                "external reference fail closed",
                "Git-like branch / merge / rebase deferred；Snapshot 配置回滚已实现且仅限 allowlist 配置。",
                "Snapshot / Rollback 是 WebAdmin 配置恢复能力，不是 Git 分支系统或世界备份。",
                "bad manifest / bad package",
                "pre_rollback",
                "本次操作变化"
        )) {
            requireTrue(all.contains(marker), "8.17 content accuracy marker: " + marker);
        }
    }

    private static Map<?, ?> find(List<?> items, String id) {
        for (Object item : items) {
            if (item instanceof Map<?, ?> map && id.equals(string(map.get("id")))) {
                return map;
            }
        }
        return null;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Set<String> idSet(List<?> items) {
        Set<String> ids = new LinkedHashSet<>();
        for (Object item : items) {
            Map<?, ?> map = requireMap(item, "id set entry");
            String id = string(map.get("id"));
            requireNotBlank(id, "entry id");
            requireTrue(ids.add(id), "duplicate catalog id: " + id);
        }
        return ids;
    }

    private static void requireReferences(Set<String> knownIds, List<?> refs, String message) {
        for (Object ref : refs) {
            requireTrue(knownIds.contains(string(ref)), message + " -> " + ref);
        }
    }

    private static void requireInternalRoutes(List<?> links, String message) {
        for (Object raw : links) {
            Map<?, ?> link = requireMap(raw, message);
            String route = string(link.get("route"));
            requireTrue(route.startsWith("#/"), message + " -> " + route);
        }
    }

    private static Map<?, ?> requireMap(Object value, String message) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new AssertionError(message + ": expected map");
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void requireTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AssertionError(message + ": expected non-blank value");
        }
    }
}
