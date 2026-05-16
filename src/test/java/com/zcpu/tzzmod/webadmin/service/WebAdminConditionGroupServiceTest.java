package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupPreviewRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminRolePolicy;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class WebAdminConditionGroupServiceTest {
    private WebAdminConditionGroupServiceTest() {
    }

    public static void run() throws Exception {
        testStoreServiceLifecycleAndWorldScopedPath();
        testPreviewAndValidation();
        testWriteFoundationBoundaries();
        testValidationMatrixAndStoreSafety();
        testEditLockEnforcement();
        testAvailableListFiltersCompatibleAndValidGroups();
        testConditionTypeRoundTripAndStrictValidation();
        testJsonCreateUpdateRoundTripsAll85RepresentativeTypes();
        testJsonUnknownBlankMissingConfigFailsWithoutAlwaysTrueFallback();
    }

    private static void testStoreServiceLifecycleAndWorldScopedPath() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-groups").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        WebAdminConditionGroupRequest request = request("demo.start", "示例开始条件", definition("demo.start", ConditionNode.leaf("node-channel", ConditionNodeType.CHANNEL_EQUALS, ConditionNodeConfig.of("channel", "demo.start"))));
        WebAdminWriteResult created = service.create(null, editor, session, "127.0.0.1", request, csrf, true);
        requireTrue(created.success(), "create condition group");
        requireTrue(created.changed(), "create condition group changed");
        requireTrue(Files.isRegularFile(storePath), "condition group store is world-scoped file path");

        Map<String, Object> list = service.list(null, editor, session);
        requireEquals(1, list.get("count"), "list contains one group");
        requireEquals(WebAdminConditionGroupStore.FILE_NAME, list.get("storeFile"), "list reports condition group store file");

        Map<String, Object> detail = service.detail(null, editor, session, "demo.start");
        String firstFingerprint = string(detail.get("fingerprint"));
        requireFalse(firstFingerprint.isBlank(), "detail has fingerprint");
        requireEquals(true, detail.get("enabled"), "detail enabled saved");
        requireEquals("示例开始条件", detail.get("displayName"), "displayName saved");

        WebAdminConditionGroupRequest update = request("demo.start", "示例开始条件 v2", definition("demo.start", ConditionNode.group(
                "root",
                ConditionGroupMode.AND,
                List.of(
                        ConditionNode.leaf("node-channel", ConditionNodeType.CHANNEL_EQUALS, ConditionNodeConfig.of("channel", "demo.start")),
                        ConditionNode.leaf("node-source", ConditionNodeType.SOURCE_TYPE_EQUALS, ConditionNodeConfig.of("sourceType", "signal_emitter"))
                )
        )));
        update.expectedFingerprint = firstFingerprint;
        WebAdminWriteResult updated = service.update(null, editor, session, "127.0.0.1", "demo.start", update, csrf, true);
        requireTrue(updated.success(), "update condition group");
        requireTrue(updated.changed(), "update condition group changed");
        Map<String, Object> updatedDetail = service.detail(null, editor, session, "demo.start");
        requireFalse(firstFingerprint.equals(string(updatedDetail.get("fingerprint"))), "update changes fingerprint");

        update.expectedFingerprint = firstFingerprint;
        WebAdminWriteResult conflict = service.update(null, editor, session, "127.0.0.1", "demo.start", update, csrf, true);
        requireFalse(conflict.success(), "stale expectedFingerprint rejected");
        requireContains(conflict.message(), "刷新", "fingerprint conflict Chinese message");

        WebAdminConditionGroupRequest delete = new WebAdminConditionGroupRequest();
        delete.expectedFingerprint = string(updatedDetail.get("fingerprint"));
        WebAdminWriteResult deleted = service.delete(null, editor, session, "127.0.0.1", "demo.start", delete, csrf, true);
        requireTrue(deleted.success(), "delete condition group");
        requireEquals(0, service.list(null, editor, session).get("count"), "delete removes group");
    }

    private static void testPreviewAndValidation() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-preview").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);

        ConditionGroupDefinition valid = definition("preview", ConditionNode.group(
                "root",
                ConditionGroupMode.AND,
                List.of(
                        ConditionNode.leaf("channel", ConditionNodeType.CHANNEL_EQUALS, ConditionNodeConfig.of("channel", "demo.start")),
                        ConditionNode.leaf("tag", ConditionNodeType.PLAYER_HAS_TAG, ConditionNodeConfig.of("tag", "runner")),
                        ConditionNode.leaf("state", ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config(
                                "scope", "GLOBAL",
                                "key", "game.active",
                                "targetMode", "global",
                                "expected", "true"
                        ))
                )
        ));
        WebAdminConditionGroupPreviewRequest preview = new WebAdminConditionGroupPreviewRequest();
        preview.groupDefinition = valid;
        preview.context = new WebAdminConditionGroupPreviewRequest.PreviewContext();
        preview.context.channel = "demo.start";
        preview.context.playerId = "player-1";
        preview.context.playerName = "Runner";
        preview.context.playerTags = List.of("runner");
        preview.context.playerOnline = true;
        preview.context.playerAlive = true;
        WebAdminConditionGroupPreviewRequest.StateVariableInput state = new WebAdminConditionGroupPreviewRequest.StateVariableInput();
        state.scope = com.zcpu.tzzmod.condition.state.StateVariableScope.GLOBAL;
        state.targetId = "global";
        state.key = "game.active";
        state.type = com.zcpu.tzzmod.condition.state.StateVariableType.BOOLEAN;
        state.value = "true";
        preview.context.stateVariables = List.of(state);

        Map<String, Object> result = service.preview(null, editor, "preview", preview);
        requireEquals(true, result.get("matched"), "preview true result");
        requireEquals(true, result.get("previewOnly"), "preview is read-only");
        requireTrue(((Number) result.get("evaluatedNodeCount")).intValue() >= 3, "preview returns evaluated node count");
        requireEquals(result.get("evaluatedNodeCount"), result.get("evaluatedCount"), "preview returns evaluatedCount alias");
        requireTrue(result.containsKey("debugTree"), "preview returns debug tree");

        preview.context.channel = "demo.stop";
        Map<String, Object> failed = service.preview(null, editor, "preview", preview);
        requireEquals(false, failed.get("matched"), "preview false result");
        requireTrue(containsChinese(string(failed.get("failureReason"))), "preview failure reason Chinese");

        Map<String, Object> invalid = service.validate(null, editor, "preview", definition("invalid", ConditionNode.leaf("bad", "unknown_type")));
        requireEquals(false, invalid.get("valid"), "unknown condition invalid");
        requireContains(string(invalid.get("message")), "校验", "validation response Chinese");

        preview.groupDefinition = definition("invalid-preview", ConditionNode.leaf("bad", "unknown_type"));
        Map<String, Object> invalidPreview = service.preview(null, editor, "preview", preview);
        requireEquals(false, invalidPreview.get("success"), "invalid preview is rejected before evaluation");
        requireEquals(0, ((Number) invalidPreview.get("evaluatedCount")).intValue(), "invalid preview does not evaluate");
        requireContains(string(invalidPreview.get("failureReason")), "校验", "invalid preview failure reason Chinese");

        Path missingStore = Files.createTempDirectory("tzz-condition-preview-missing").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminConditionGroupService missingService = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, missingStore, null);
        Map<String, Object> missingPreview = missingService.preview(null, editor, "missing", new WebAdminConditionGroupPreviewRequest());
        requireEquals(false, missingPreview.get("success"), "missing preview group fails safely");
        requireEquals(0, ((Number) missingPreview.get("evaluatedCount")).intValue(), "missing preview does not evaluate fallback default");
        requireFalse(Files.exists(missingStore), "preview does not create store file");
    }

    private static void testWriteFoundationBoundaries() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-boundary").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        WebAdminConditionGroupRequest create = request("boundary", "边界测试", definition("boundary", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE)));
        requireFalse(service.create(null, user(WebAdminRole.VIEWER), session(user(WebAdminRole.VIEWER)), "127.0.0.1", create, csrf, true).success(), "VIEWER cannot write condition groups");
        requireFalse(service.create(null, editor, session, "127.0.0.1", create, "", true).success(), "CSRF required for condition group writes");
        requireFalse(service.create(null, editor, session, "127.0.0.1", create, csrf, false).success(), "same-origin required for condition group writes");
        requireTrue(service.create(null, editor, session, "127.0.0.1", create, csrf, true).success(), "EDITOR can create condition group");
        requireTrue(WebAdminRolePolicy.allows(WebAdminRole.EDITOR, WebAdminOperationType.EDIT_CONDITION_GROUP), "EDITOR can edit condition groups by policy");
        requireFalse(WebAdminRolePolicy.allows(WebAdminRole.VIEWER, WebAdminOperationType.EDIT_CONDITION_GROUP), "VIEWER cannot edit condition groups by policy");
    }

    private static void testValidationMatrixAndStoreSafety() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-validation").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        requireInvalid(service, editor, "empty", definition("empty", ConditionNode.group("empty", ConditionGroupMode.AND, List.of())), "至少");
        requireInvalid(service, editor, "bad-not", definition("bad-not", ConditionNode.group(
                "bad-not",
                ConditionGroupMode.NOT,
                List.of(ConditionNode.leaf("a", ConditionNodeType.ALWAYS_TRUE), ConditionNode.leaf("b", ConditionNodeType.ALWAYS_FALSE))
        )), "必须");
        requireInvalid(service, editor, "missing-field", definition("missing-field", ConditionNode.leaf("missing", ConditionNodeType.CHANNEL_EQUALS)), "信号频道");
        requireInvalid(service, editor, "invalid-operator", definition("invalid-operator", ConditionNode.leaf("int", ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config(
                "scope", "GLOBAL",
                "key", "game.count",
                "targetMode", "global",
                "operator", "bad",
                "value", "1"
        ))), "比较");
        requireInvalid(service, editor, "invalid-value", definition("invalid-value", ConditionNode.leaf("int", ConditionNodeType.STATE_VARIABLE_INT_COMPARE, config(
                "scope", "GLOBAL",
                "key", "game.count",
                "targetMode", "global",
                "operator", "gte",
                "value", "not-number"
        ))), "整数");
        requireInvalid(service, editor, "deep", definition("deep", deepGroup(20)), "深度");
        requireInvalid(service, editor, "many", definition("many", manyGroup(140)), "数量");

        WebAdminConditionGroupRequest valid = request("duplicate", "重复测试", definition("duplicate", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE)));
        requireTrue(service.create(null, editor, session, "127.0.0.1", valid, csrf, true).success(), "initial create for duplicate test");
        WebAdminWriteResult duplicate = service.create(null, editor, session, "127.0.0.1", valid, csrf, true);
        requireFalse(duplicate.success(), "duplicate id rejected");
        requireContains(firstValidationMessage(duplicate), "已存在", "duplicate id validation Chinese");
        WebAdminWriteResult missingDefinition = service.create(null, editor, session, "127.0.0.1", request("missing-definition", "缺少定义", null), csrf, true);
        requireFalse(missingDefinition.success(), "missing groupDefinition rejected");
        requireContains(firstValidationMessage(missingDefinition), "不能为空", "missing groupDefinition validation Chinese");
        requireFalse(service.delete(null, editor, session, "127.0.0.1", "not-found", new WebAdminConditionGroupRequest(), csrf, true).changed(), "delete not found is no-change");

        Files.writeString(storePath, "{\"version\":1,\"groups\":{\"bad\":{\"id\":\"bad\",\"displayName\":\"坏记录\"}}}");
        Map<String, Object> skipped = service.list(null, editor, session);
        requireEquals(0, skipped.get("count"), "record without groupDefinition is skipped instead of converted to always_true");
        requireEquals(true, skipped.get("storeDegraded"), "record without groupDefinition marks store degraded");
        requireContains(string(skipped.get("storeMessage")), "缺少条件组定义", "missing groupDefinition load warning Chinese");
        WebAdminWriteResult skippedBlocked = service.create(null, editor, session, "127.0.0.1", valid, csrf, true);
        requireFalse(skippedBlocked.success(), "missing groupDefinition store blocks writes");

        Files.writeString(storePath, "{not-json");
        Map<String, Object> degraded = service.list(null, editor, session);
        requireEquals(true, degraded.get("storeDegraded"), "bad file fallback marks store degraded");
        requireContains(string(degraded.get("storeMessage")), "读取失败", "bad file fallback message Chinese");
        WebAdminWriteResult blocked = service.create(null, editor, session, "127.0.0.1", valid, csrf, true);
        requireFalse(blocked.success(), "degraded store blocks writes");
        requireContains(blocked.message(), "读取失败", "degraded write block Chinese");
    }

    private static void testEditLockEnforcement() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-lock").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminPermissionService permission = new WebAdminPermissionService();
        WebAdminEditLockService locks = new WebAdminEditLockService(permission, security);
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(permission, security, locks, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);
        WebAdminConditionGroupRequest request = request("locked", "锁定条件组", definition("locked", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE)));

        WebAdminWriteResult missingLock = service.create(null, editor, session, "127.0.0.1", request, csrf, true);
        requireFalse(missingLock.success(), "condition group write requires edit lock");
        requireContains(missingLock.message(), "编辑锁", "missing edit lock message Chinese");

        WebAdminUser other = user(WebAdminRole.OWNER);
        WebAdminSession otherSession = session(other);
        String otherCsrf = security.csrfTokenFor(otherSession);
        WebAdminWriteResult otherLock = locks.acquire(other, otherSession, "127.0.0.1", lockRequest("locked", ""), otherCsrf, true);
        requireTrue(otherLock.success(), "other user can acquire condition group lock");
        request.lockId = "wrong-lock";
        WebAdminWriteResult conflict = service.create(null, editor, session, "127.0.0.1", request, csrf, true);
        requireFalse(conflict.success(), "condition group lock conflict rejected");
        requireContains(conflict.message(), "占用", "lock conflict message Chinese");
        locks.release(other, otherSession, "127.0.0.1", lockRequest("locked", lockId(otherLock)), otherCsrf, true);

        WebAdminWriteResult ownLock = locks.acquire(editor, session, "127.0.0.1", lockRequest("locked", ""), csrf, true);
        requireTrue(ownLock.success(), "editor can acquire condition group lock");
        request.lockId = lockId(ownLock);
        WebAdminWriteResult created = service.create(null, editor, session, "127.0.0.1", request, csrf, true);
        requireTrue(created.success(), "condition group write succeeds with held lock");
        requireEquals(0, locks.activeLockCount(), "condition group write releases edit lock");
    }

    private static void testAvailableListFiltersCompatibleAndValidGroups() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-available").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminConditionGroupStore.ConditionGroupFile file = new WebAdminConditionGroupStore.ConditionGroupFile();
        file.groups.put("always", entry("always", "始终通过", definition("always", ConditionNode.leaf("always", ConditionNodeType.ALWAYS_TRUE)), true));
        file.groups.put("player", entry("player", "玩家条件", definition("player", ConditionNode.leaf("player", ConditionNodeType.PLAYER_EXISTS)), true));
        file.groups.put("container", entry("container", "容器条件", definition("container", ConditionNode.leaf("container", ConditionNodeType.CONTAINER_ITEM_COUNT_COMPARE,
                config("containerKey", "container", "itemId", "minecraft:stone", "operator", "gte", "count", "1"))), true));
        file.groups.put("relay", entry("relay", "继电器条件", definition("relay", ConditionNode.leaf("relay", ConditionNodeType.CONTEXT_EQUALS,
                config("field", "relayId", "expected", "minecraft:overworld@1,2,3"))), true));
        file.groups.put("region", entry("region", "区域条件", definition("region", ConditionNode.leaf("region", ConditionNodeType.REGION_ENABLED,
                config("regionKey", "region"))), true));
        file.groups.put("invalid", entry("invalid", "无效条件", definition("invalid", ConditionNode.leaf("invalid", "unknown_type")), true));
        file.groups.put("disabled", entry("disabled", "停用条件", definition("disabled", ConditionNode.leaf("disabled", ConditionNodeType.ALWAYS_TRUE)), false));
        requireTrue(WebAdminConditionGroupStore.save(storePath, file), "seed available list condition groups");

        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), new WebAdminWriteSecurityService(), null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);

        Map<String, Object> redstone = service.available(null, editor, session, ConditionRuntimeTargetType.VBD_REDSTONE.id(), "vbd-1");
        requireEquals(1, redstone.get("count"), "redstone available list returns only always_true");
        requireTrue(groupIds(redstone).contains("always"), "redstone available includes always group");
        requireFalse(groupIds(redstone).contains("player"), "redstone available excludes player group");
        requireFalse(groupIds(redstone).contains("container"), "redstone available excludes container group");
        requireFalse(groupIds(redstone).contains("invalid"), "redstone available excludes invalid group");
        requireFalse(groupIds(redstone).contains("disabled"), "redstone available excludes disabled group");
        requireTrue(((Number) redstone.get("incompatibleCount")).intValue() >= 3, "redstone available reports incompatible diagnostics");
        requireContains(string(redstone.get("optionalGateMessage")), "保持旧逻辑", "available list optional gate message Chinese");

        Map<String, Object> containerChange = service.available(null, editor, session, ConditionRuntimeTargetType.CONTAINER_CHANGE.id(), "vbd-1");
        requireTrue(groupIds(containerChange).contains("container"), "container change available includes container snapshot group");
        requireFalse(groupIds(containerChange).contains("player"), "container change available excludes player group");

        Map<String, Object> signalListener = service.available(null, editor, session, ConditionRuntimeTargetType.SIGNAL_LISTENER.id(), "listener-1");
        requireTrue(groupIds(signalListener).contains("always"), "SignalListener available includes always group");
        requireFalse(groupIds(signalListener).contains("player"), "SignalListener available excludes player context group");
        requireFalse(groupIds(signalListener).contains("container"), "SignalListener available excludes container snapshot group");
        requireFalse(groupIds(signalListener).contains("relay"), "SignalListener available excludes relay-only context group");
        requireFalse(groupIds(signalListener).contains("region"), "SignalListener available excludes region snapshot group");

        Map<String, Object> actionRelay = service.available(null, editor, session, ConditionRuntimeTargetType.ACTION_RELAY.id(), "minecraft:overworld@1,2,3");
        requireTrue(groupIds(actionRelay).contains("always"), "ActionRelay available includes always group");
        requireTrue(groupIds(actionRelay).contains("relay"), "ActionRelay available includes relayId context group");
        requireFalse(groupIds(actionRelay).contains("player"), "ActionRelay available excludes player context group");
        requireFalse(groupIds(actionRelay).contains("container"), "ActionRelay available excludes container snapshot group");
        requireFalse(groupIds(actionRelay).contains("region"), "ActionRelay available excludes region snapshot group");

        for (ConditionRuntimeTargetType regionTarget : List.of(
                ConditionRuntimeTargetType.REGION_ENTER,
                ConditionRuntimeTargetType.REGION_EXIT,
                ConditionRuntimeTargetType.REGION_STAY
        )) {
            Map<String, Object> regionAvailable = service.available(null, editor, session, regionTarget.id(), "region-controller-1");
            requireTrue(groupIds(regionAvailable).contains("always"), regionTarget.id() + " available includes always group");
            requireTrue(groupIds(regionAvailable).contains("player"), regionTarget.id() + " available includes player context group");
            requireTrue(groupIds(regionAvailable).contains("region"), regionTarget.id() + " available includes region snapshot group");
            requireFalse(groupIds(regionAvailable).contains("container"), regionTarget.id() + " available excludes container snapshot group");
            requireFalse(groupIds(regionAvailable).contains("relay"), regionTarget.id() + " available excludes relay-only context group");
        }

        Map<String, Object> containerOpenUnresolved = service.available(null, editor, session, ConditionRuntimeTargetType.CONTAINER_OPEN.id(), "missing-vbd");
        requireFalse(groupIds(containerOpenUnresolved).contains("container"), "unresolved container open target excludes container snapshot group");
        requireContains(string(containerOpenUnresolved.get("message")), "无法提供容器内容快照", "unresolved container open diagnostic is Chinese");

        WebAdminConditionGroupService inventoryTargetService = new WebAdminConditionGroupService(
                new WebAdminPermissionService(),
                new WebAdminWriteSecurityService(),
                null,
                storePath,
                null,
                (server, targetId) -> "inventory-vbd".equals(targetId)
        );
        Map<String, Object> containerOpenInventory = inventoryTargetService.available(null, editor, session, ConditionRuntimeTargetType.CONTAINER_OPEN.id(), "inventory-vbd");
        Map<String, Object> containerCloseInventory = inventoryTargetService.available(null, editor, session, ConditionRuntimeTargetType.CONTAINER_CLOSE.id(), "inventory-vbd");
        Map<String, Object> containerCloseNonInventory = inventoryTargetService.available(null, editor, session, ConditionRuntimeTargetType.CONTAINER_CLOSE.id(), "non-inventory-vbd");
        requireTrue(groupIds(containerOpenInventory).contains("container"), "Inventory container open available includes container_slot_item_matches-compatible group");
        requireTrue(groupIds(containerCloseInventory).contains("container"), "Inventory container close available includes container_slot_item_matches-compatible group");
        requireFalse(groupIds(containerCloseNonInventory).contains("container"), "non-Inventory container close available excludes container snapshot group");
        requireContains(string(containerCloseNonInventory.get("message")), "无法提供容器内容快照", "non-Inventory container close diagnostic is Chinese");
    }

    private static void testConditionTypeRoundTripAndStrictValidation() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-roundtrip").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        ConditionGroupDefinition definition = definition("roundtrip", ConditionNode.group(
                "root",
                ConditionGroupMode.AND,
                List.of(
                        ConditionNode.leaf("context", ConditionNodeType.CONTEXT_EQUALS, config("field", "channel", "expected", "mission.start")),
                        ConditionNode.leaf("state", ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, config("scope", "GLOBAL", "key", "game.active", "targetMode", "global", "expected", "true")),
                        ConditionNode.leaf("inventory", ConditionNodeType.INVENTORY_CONTAINS_ITEM, config("inventoryKey", "player", "itemId", "minecraft:diamond", "countOperator", "gte", "count", "1"))
                )
        ));
        WebAdminWriteResult created = service.create(null, editor, session, "127.0.0.1", request("roundtrip", "类型回显", definition), csrf, true);
        requireTrue(created.success(), "condition type roundtrip create succeeds");
        ConditionGroupDefinition saved = (ConditionGroupDefinition) service.detail(null, editor, session, "roundtrip").get("groupDefinition");
        List<ConditionNode> children = saved.root().children();
        requireEquals(ConditionNodeType.CONTEXT_EQUALS, children.get(0).type(), "context_equals type roundtrips");
        requireEquals("channel", children.get(0).config().get("field"), "context_equals field roundtrips");
        requireEquals(ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, children.get(1).type(), "state_variable_bool_equals type roundtrips");
        requireEquals("true", children.get(1).config().get("expected"), "state bool expected roundtrips");
        requireEquals(ConditionNodeType.INVENTORY_CONTAINS_ITEM, children.get(2).type(), "inventory condition type roundtrips");
        requireEquals("minecraft:diamond", children.get(2).config().get("itemId"), "inventory matcher itemId roundtrips");

        requireInvalid(service, editor, "unknown-type", definition("unknown-type", ConditionNode.leaf("bad", "unknown_type")), "未知条件类型");
        requireInvalid(service, editor, "blank-type", definition("blank-type", new ConditionNode("blank", "", "", "", true, ConditionGroupMode.AND, ConditionNodeConfig.EMPTY, List.of())), "未知条件类型");
        requireInvalid(service, editor, "context-missing-field", definition("context-missing-field", ConditionNode.leaf("context", ConditionNodeType.CONTEXT_EQUALS, config("field", "channel"))), "期望值");
    }

    private static void testJsonCreateUpdateRoundTripsAll85RepresentativeTypes() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-json-roundtrip").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        WebAdminConditionGroupRequest create = requestFromJson("""
                {
                  "id":"json.roundtrip",
                  "displayName":"JSON 类型回显",
                  "note":"真实 JSON 请求 roundtrip",
                  "iconKey":"doctor-overview",
                  "enabled":true,
                  "tags":["json"],
                  "groupDefinition":{
                    "id":"json.roundtrip",
                    "version":1,
                    "displayName":"JSON 类型回显",
                    "note":"",
                    "tags":["json"],
                    "root":{
                      "id":"root",
                      "type":"group",
                      "name":"",
                      "note":"",
                      "enabled":true,
                      "groupMode":"AND",
                      "config":{"values":{}},
                      "children":[
                        {"id":"context","type":"context_equals","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"field":"channel","expected":"mission.start"}},"children":[]},
                        {"id":"state","type":"state_variable_bool_equals","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"scope":"GLOBAL","key":"game.active","targetMode":"global","expected":"true"}},"children":[]},
                        {"id":"inventory","type":"inventory_contains_item","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"inventoryKey":"player","itemId":"minecraft:diamond","countOperator":"gte","count":"1"}},"children":[]},
                        {"id":"container","type":"container_slot_item_matches","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"containerKey":"chest","slot":"0","itemId":"minecraft:stone","countOperator":"gte","count":"1"}},"children":[]},
                        {"id":"region","type":"region_enabled","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"regionKey":"spawn","expected":"true"}},"children":[]},
                        {"id":"signal","type":"signal_event_count_compare","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"signalHistoryKey":"history","operator":"gte","count":"2"}},"children":[]},
                        {"id":"logic","type":"logic_chain_has_cycle","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"logicChainKey":"chain","expected":"false"}},"children":[]}
                      ]
                    }
                  }
                }
                """);
        WebAdminWriteResult created = service.create(null, editor, session, "127.0.0.1", create, csrf, true);
        requireTrue(created.success(), "JSON create keeps selected condition types");
        List<ConditionNode> children = detailChildren(service, editor, session, "json.roundtrip");
        requireEquals(7, children.size(), "JSON roundtrip child count");
        requireChild(children, 0, ConditionNodeType.CONTEXT_EQUALS, "field", "channel");
        requireChild(children, 1, ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS, "expected", "true");
        requireChild(children, 2, ConditionNodeType.INVENTORY_CONTAINS_ITEM, "itemId", "minecraft:diamond");
        requireChild(children, 3, ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES, "containerKey", "chest");
        requireChild(children, 4, ConditionNodeType.REGION_ENABLED, "regionKey", "spawn");
        requireChild(children, 5, ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE, "signalHistoryKey", "history");
        requireChild(children, 6, ConditionNodeType.LOGIC_CHAIN_HAS_CYCLE, "logicChainKey", "chain");
        for (ConditionNode child : children) {
            requireFalse(ConditionNodeType.ALWAYS_TRUE.equals(child.type()), "JSON request does not fallback child to always_true: " + child.id());
        }

        WebAdminConditionGroupRequest seed = requestFromJson("""
                {
                  "id":"json.update",
                  "displayName":"JSON 更新回显",
                  "iconKey":"doctor-overview",
                  "enabled":true,
                  "groupDefinition":{"id":"json.update","version":1,"displayName":"JSON 更新回显","note":"","tags":[],"root":{"id":"root","type":"group","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{}},"children":[{"id":"seed","type":"always_true","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{}},"children":[]}]}}
                }
                """);
        requireTrue(service.create(null, editor, session, "127.0.0.1", seed, csrf, true).success(), "JSON seed create");
        String fingerprint = string(service.detail(null, editor, session, "json.update").get("fingerprint"));
        WebAdminConditionGroupRequest update = requestFromJson("""
                {
                  "id":"json.update",
                  "displayName":"JSON 更新回显",
                  "iconKey":"doctor-overview",
                  "enabled":true,
                  "expectedFingerprint":"__FP__",
                  "groupDefinition":{"id":"json.update","version":1,"displayName":"JSON 更新回显","note":"","tags":[],"root":{"id":"root","type":"group","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{}},"children":[{"id":"context","type":"context_equals","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{"field":"channel","expected":"mission.start"}},"children":[]}]}}
                }
                """.replace("__FP__", fingerprint));
        WebAdminWriteResult updated = service.update(null, editor, session, "127.0.0.1", "json.update", update, csrf, true);
        requireTrue(updated.success(), "JSON update from always_true to context_equals succeeds");
        List<ConditionNode> updatedChildren = detailChildren(service, editor, session, "json.update");
        requireChild(updatedChildren, 0, ConditionNodeType.CONTEXT_EQUALS, "expected", "mission.start");
    }

    private static void testJsonUnknownBlankMissingConfigFailsWithoutAlwaysTrueFallback() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-json-invalid").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminConditionGroupService service = new WebAdminConditionGroupService(new WebAdminPermissionService(), security, null, storePath, null);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        String csrf = security.csrfTokenFor(session);

        requireJsonCreateInvalid(service, editor, session, csrf, "json.unknown", "unknown_type", """
                {"values":{"field":"channel","expected":"mission.start"}}
                """, "未知条件类型");
        requireJsonCreateInvalid(service, editor, session, csrf, "json.blank", "", """
                {"values":{"field":"channel","expected":"mission.start"}}
                """, "未知条件类型");
        requireJsonCreateInvalid(service, editor, session, csrf, "json.missing-config", "context_equals", """
                {"values":{"field":"channel"}}
                """, "期望值");

        WebAdminConditionGroupRequest missingDefinition = requestFromJson("""
                {"id":"json.missing-definition","displayName":"缺少定义","enabled":true}
                """);
        WebAdminWriteResult missing = service.create(null, editor, session, "127.0.0.1", missingDefinition, csrf, true);
        requireFalse(missing.success(), "JSON missing groupDefinition rejected");
        requireContains(firstValidationMessage(missing), "不能为空", "JSON missing groupDefinition Chinese validation");
        requireEquals(0, service.list(null, editor, session).get("count"), "invalid JSON creates no condition groups");
        if (Files.exists(storePath)) {
            String persisted = Files.readString(storePath);
            requireFalse(persisted.contains("node-always-true"), "invalid JSON does not persist default always_true fallback");
        }
    }

    private static WebAdminConditionGroupRequest request(String id, String displayName, ConditionGroupDefinition definition) {
        WebAdminConditionGroupRequest request = new WebAdminConditionGroupRequest();
        request.id = id;
        request.displayName = displayName;
        request.note = "测试条件组";
        request.iconKey = "doctor-overview";
        request.enabled = true;
        request.tags = List.of("test");
        request.groupDefinition = definition;
        return request;
    }

    private static WebAdminConditionGroupRequest requestFromJson(String json) {
        return WebAdminJsonResponse.GSON.fromJson(json, WebAdminConditionGroupRequest.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> groupIds(Map<String, Object> available) {
        Object groupsRaw = available.get("groups");
        if (!(groupsRaw instanceof List<?> groups)) {
            return List.of();
        }
        return groups.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(group -> string(((Map<String, Object>) group).get("id")))
                .toList();
    }

    private static WebAdminConditionGroupStore.ConditionGroupEntry entry(
            String id,
            String displayName,
            ConditionGroupDefinition definition,
            boolean enabled
    ) {
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = id;
        entry.displayName = displayName;
        entry.enabled = enabled;
        entry.groupDefinition = definition;
        return entry;
    }

    private static List<ConditionNode> detailChildren(WebAdminConditionGroupService service, WebAdminUser editor, WebAdminSession session, String id) {
        ConditionGroupDefinition saved = (ConditionGroupDefinition) service.detail(null, editor, session, id).get("groupDefinition");
        return saved.root().children();
    }

    private static void requireChild(List<ConditionNode> children, int index, String expectedType, String key, String expectedValue) {
        ConditionNode child = children.get(index);
        requireEquals(expectedType, child.type(), "JSON roundtrip type at index " + index);
        requireEquals(expectedValue, child.config().get(key), "JSON roundtrip config " + key + " at index " + index);
    }

    private static void requireJsonCreateInvalid(WebAdminConditionGroupService service, WebAdminUser editor, WebAdminSession session, String csrf, String id, String type, String configJson, String expectedChineseFragment) {
        String json = """
                {
                  "id":"__ID__",
                  "displayName":"JSON invalid",
                  "iconKey":"doctor-overview",
                  "enabled":true,
                  "groupDefinition":{"id":"__ID__","version":1,"displayName":"JSON invalid","note":"","tags":[],"root":{"id":"root","type":"group","name":"","note":"","enabled":true,"groupMode":"AND","config":{"values":{}},"children":[{"id":"bad","type":"__TYPE__","name":"","note":"","enabled":true,"groupMode":"AND","config":__CONFIG__,"children":[]}]}}
                }
                """.replace("__ID__", id).replace("__TYPE__", type).replace("__CONFIG__", configJson.trim());
        WebAdminWriteResult result = service.create(null, editor, session, "127.0.0.1", requestFromJson(json), csrf, true);
        requireFalse(result.success(), "JSON invalid request rejected: " + id);
        requireContains(firstValidationMessage(result), expectedChineseFragment, "JSON invalid validation Chinese: " + id);
    }

    private static ConditionGroupDefinition definition(String id, ConditionNode root) {
        return new ConditionGroupDefinition(id, 1, id, "", List.of(), root);
    }

    private static ConditionNodeConfig config(String... entries) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static void requireInvalid(WebAdminConditionGroupService service, WebAdminUser editor, String id, ConditionGroupDefinition definition, String expectedChineseFragment) {
        Map<String, Object> invalid = service.validate(null, editor, id, definition);
        requireEquals(false, invalid.get("valid"), "condition group should be invalid: " + id);
        requireContains(string(invalid.get("issues")), expectedChineseFragment, "invalid condition group validation Chinese: " + id);
    }

    private static ConditionNode deepGroup(int depth) {
        ConditionNode node = ConditionNode.leaf("leaf", ConditionNodeType.ALWAYS_TRUE);
        for (int index = 0; index < depth; index++) {
            node = ConditionNode.group("deep-" + index, ConditionGroupMode.AND, List.of(node));
        }
        return node;
    }

    private static ConditionNode manyGroup(int count) {
        java.util.ArrayList<ConditionNode> nodes = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            nodes.add(ConditionNode.leaf("node-" + index, ConditionNodeType.ALWAYS_TRUE));
        }
        return ConditionNode.group("many-root", ConditionGroupMode.AND, nodes);
    }

    private static WebAdminEditLockRequest lockRequest(String targetId, String lockId) {
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = WebAdminEditLockService.TARGET_CONDITION_GROUP;
        request.targetId = targetId;
        request.lockId = lockId;
        return request;
    }

    private static String lockId(WebAdminWriteResult result) {
        Object lock = result.data().get("lock");
        if (lock instanceof WebAdminEditLockStatusDto status) {
            return status.lockId();
        }
        return "";
    }

    private static String firstValidationMessage(WebAdminWriteResult result) {
        return result.validationErrors().isEmpty() ? result.message() : result.validationErrors().getFirst().message();
    }

    private static WebAdminUser user(WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = role.id().toLowerCase(java.util.Locale.ROOT);
        user.displayName = role.displayName();
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
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

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }
}
