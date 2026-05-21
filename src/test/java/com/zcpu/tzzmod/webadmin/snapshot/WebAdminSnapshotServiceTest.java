package com.zcpu.tzzmod.webadmin.snapshot;

import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.RollbackPlan;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotKind;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotPackage;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotRecord;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.SnapshotTrigger;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotModels.StoreSpec;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.ManifestLoadResult;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.PackageLoadResult;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotStore.SnapshotCollectionResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WebAdminSnapshotServiceTest {
    private WebAdminSnapshotServiceTest() {
    }

    public static void run() throws Exception {
        testManualSnapshotManifestPackageAndDiff();
        testTimerBeforeWriteAutoSnapshotRecordsOperationDiff();
        testOperationDiffCoversSnapshotResourceTypes();
        testResourceMetadataChangesProduceUpdatedDiffs();
        testBadManifestAndPackageFallback();
        testPackageFingerprintMismatchBlocksRollbackPlan();
        testRollbackPlanAndApplyRestoresStoreFiles();
        testRollbackRestoresSignalDeviceConfigAndPreservesRuntime();
        testRollbackPreservesExistingStateVariableValues();
        testAutoRetentionProtectsManualAndPreRollback();
        testCollectExcludesForbiddenDirectories();
    }

    private static void testManualSnapshotManifestPackageAndDiff() throws Exception {
        Fixture fixture = fixture();
        writeChannelMetadata(fixture.webAdminDir, "Alpha");
        WebAdminSnapshotService service = service();

        SnapshotRecord first = create(service, fixture, SnapshotKind.MANUAL, "手动保存点", "manual");
        requireTrue(first != null && "manual".equals(first.kind), "manual snapshot is created");
        requireTrue(first.diffSummary.created > 0, "first snapshot reports created resources");
        requireTrue(first.diffSummary.created == 0 || first.diffSummary.changed() > 0, "created diff summary is populated");
        ManifestLoadResult manifest = WebAdminSnapshotStore.loadManifest(fixture.snapshotRoot);
        requireEquals(1, manifest.manifest().records.size(), "manifest contains manual snapshot");
        requireTrue(Files.exists(WebAdminSnapshotStore.packagePath(fixture.snapshotRoot, first.snapshotId)), "snapshot package is persisted");

        writeChannelMetadata(fixture.webAdminDir, "Beta");
        SnapshotRecord second = create(service, fixture, SnapshotKind.AUTO, "自动保存点", "auto");
        requireTrue(second.diffSummary.updated > 0, "second snapshot records updated resources");
        SnapshotPackage previous = WebAdminSnapshotStore.loadPackage(fixture.snapshotRoot, first.snapshotId).pack();
        SnapshotPackage current = WebAdminSnapshotStore.loadPackage(fixture.snapshotRoot, second.snapshotId).pack();
        var diff = WebAdminSnapshotService.diff(previous, current);
        requireTrue(diff.summary.updated > 0, "resource diff detects updates");
        requireTrue(diff.entries.stream().anyMatch(entry -> "updated".equals(entry.changeType) && !entry.beforeSummary.isBlank() && !entry.afterSummary.isBlank()), "updated diff carries before/after summary");
        SnapshotPackage empty = new SnapshotPackage();
        var createdDiff = WebAdminSnapshotService.diff(empty, current);
        requireTrue(createdDiff.entries.stream().anyMatch(entry -> "created".equals(entry.changeType) && !entry.afterSummary.isBlank() && !entry.afterJsonPreview.isBlank()), "created diff carries new resource summary");
        var deletedDiff = WebAdminSnapshotService.diff(current, empty);
        requireTrue(deletedDiff.entries.stream().anyMatch(entry -> "deleted".equals(entry.changeType) && !entry.beforeSummary.isBlank() && !entry.beforeJsonPreview.isBlank()), "deleted diff carries old resource summary");
    }

    private static void testTimerBeforeWriteAutoSnapshotRecordsOperationDiff() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeTimer(fixture.webAdminDir, "timer.rename", "旧 Timer 名称", "before note");
        SnapshotRecord baseline = create(service, fixture, SnapshotKind.MANUAL, "Timer 基线", "manual");
        requireTrue(baseline.diffSummary.created > 0, "baseline captures timer resource");

        SnapshotRecord beforeWrite = create(service, fixture, SnapshotKind.AUTO, "编辑 Timer 前自动保存", "auto");
        requireEquals(0, beforeWrite.diffSummary.changed(), "before-write auto snapshot itself can match previous snapshot");

        writeTimer(fixture.webAdminDir, "timer.rename", "新 Timer 名称", "after note");
        SnapshotRecord annotated = service.updateAutoSnapshotOperationDiff(
                fixture.snapshotRoot,
                WebAdminSnapshotStore.collect(fixture.specs),
                beforeWrite.snapshotId
        );
        requireTrue(annotated != null, "auto snapshot operation diff is persisted after Timer write");
        requireTrue(annotated.operationDiff.summary.updated > 0, "Timer rename is reported as operation updated diff");
        requireTrue(annotated.operationDiff.entries.stream().anyMatch(entry ->
                "updated".equals(entry.changeType)
                        && "timer".equals(entry.resourceType)
                        && "timer.rename".equals(entry.resourceId)
                        && "新 Timer 名称".equals(entry.displayName)
                        && !entry.beforeSummary.isBlank()
                        && !entry.afterSummary.isBlank()
                        && !entry.beforeJsonPreview.isBlank()
                        && !entry.afterJsonPreview.isBlank()
                        && entry.fieldDiffs.stream().anyMatch(field -> "displayName".equals(field.field)
                        && field.beforeValue.contains("旧 Timer 名称")
                        && field.afterValue.contains("新 Timer 名称"))
        ), "operation diff includes updated Timer resource marker");

        SnapshotRecord stored = WebAdminSnapshotStore.loadManifest(fixture.snapshotRoot).manifest().records.stream()
                .filter(record -> beforeWrite.snapshotId.equals(record.snapshotId))
                .findFirst()
                .orElseThrow();
        requireTrue(stored.operationDiff.summary.updated > 0, "manifest stores operation diff for before-write auto snapshot detail");

        SnapshotRecord nextBeforeWrite = create(service, fixture, SnapshotKind.AUTO, "下一次写入前自动保存", "auto");
        requireEquals(0, nextBeforeWrite.diffSummary.changed(), "next before-write snapshot does not repeat the previous operation diff as previous-snapshot diff");
    }

    private static void testOperationDiffCoversSnapshotResourceTypes() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeAllDiffResources(fixture, "Before");
        SnapshotRecord baseline = create(service, fixture, SnapshotKind.MANUAL, "资源操作基线", "manual");
        requireTrue(baseline != null, "baseline exists for operation diff coverage");
        SnapshotRecord beforeWrite = create(service, fixture, SnapshotKind.AUTO, "写操作前自动保存", "auto");
        requireEquals(0, beforeWrite.diffSummary.changed(), "before-write snapshot keeps previous diff independent from operation diff");

        writeAllDiffResources(fixture, "After");
        SnapshotRecord annotated = service.updateAutoSnapshotOperationDiff(
                fixture.snapshotRoot,
                WebAdminSnapshotStore.collect(fixture.specs),
                beforeWrite.snapshotId
        );
        requireTrue(annotated != null, "operation diff annotation succeeds for generic resources");
        String changedTypes = annotated.operationDiff.entries.stream()
                .filter(entry -> "updated".equals(entry.changeType))
                .map(entry -> entry.resourceType)
                .reduce("", (left, right) -> left + "\n" + right);
        for (String type : List.of("channel", "logic_chain", "signal_join", "timer", "signal_listener", "condition_group", "state_variable")) {
            requireContains(changedTypes, type, type + " updated write is reported in operation diff");
        }
        requireTrue(annotated.operationDiff.entries.stream().filter(entry -> "updated".equals(entry.changeType)).allMatch(entry ->
                !entry.beforeSummary.isBlank()
                        && !entry.afterSummary.isBlank()
                        && !entry.beforeJsonPreview.isBlank()
                        && !entry.afterJsonPreview.isBlank()
        ), "operation diff entries carry read-only detail summaries");
        requireTrue(annotated.operationDiff.entries.stream().anyMatch(entry ->
                "timer".equals(entry.resourceType)
                        && !entry.fieldDiffs.isEmpty()
        ), "Timer operation diff exposes shallow field diff");
    }

    private static void testResourceMetadataChangesProduceUpdatedDiffs() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeAllDiffResources(fixture, "Before");
        SnapshotRecord before = create(service, fixture, SnapshotKind.MANUAL, "资源变化前", "manual");
        writeAllDiffResources(fixture, "After");
        SnapshotRecord after = create(service, fixture, SnapshotKind.MANUAL, "资源变化后", "manual");
        SnapshotPackage previous = WebAdminSnapshotStore.loadPackage(fixture.snapshotRoot, before.snapshotId).pack();
        SnapshotPackage current = WebAdminSnapshotStore.loadPackage(fixture.snapshotRoot, after.snapshotId).pack();
        String changedTypes = WebAdminSnapshotService.diff(previous, current).entries.stream()
                .filter(entry -> "updated".equals(entry.changeType))
                .map(entry -> entry.resourceType)
                .reduce("", (left, right) -> left + "\n" + right);
        for (String type : List.of("channel", "logic_chain", "signal_join", "timer", "signal_listener", "condition_group", "state_variable", "template", "region_controller", "signal_device")) {
            requireContains(changedTypes, type, type + " metadata/config change is detected as updated");
        }
    }

    private static void testBadManifestAndPackageFallback() throws Exception {
        Fixture fixture = fixture();
        Files.createDirectories(fixture.snapshotRoot);
        Files.writeString(WebAdminSnapshotStore.manifestPath(fixture.snapshotRoot), "{bad json", StandardCharsets.UTF_8);
        ManifestLoadResult manifest = WebAdminSnapshotStore.loadManifest(fixture.snapshotRoot);
        requireTrue(manifest.degraded(), "bad manifest degrades safely");
        requireEquals(0, manifest.manifest().records.size(), "bad manifest returns empty records");
        requireFalse(manifest.message().contains("Expected") || manifest.message().contains("line 1"), "bad manifest message hides parser details from UI");
        writeChannelMetadata(fixture.webAdminDir, "Blocked");
        requireTrue(create(service(), fixture, SnapshotKind.MANUAL, "blocked", "manual") == null, "bad manifest prevents overwriting corrupted timeline");

        Files.createDirectories(WebAdminSnapshotStore.dataDirectory(fixture.snapshotRoot));
        Files.writeString(WebAdminSnapshotStore.packagePath(fixture.snapshotRoot, "broken"), "{bad json", StandardCharsets.UTF_8);
        PackageLoadResult pack = WebAdminSnapshotStore.loadPackage(fixture.snapshotRoot, "broken");
        requireTrue(pack.degraded(), "bad snapshot package degrades safely");
        requireTrue(pack.pack() == null, "bad snapshot package does not return partial data");
        requireFalse(pack.message().contains("Expected") || pack.message().contains("line 1"), "bad snapshot package message hides parser details from UI");

        Fixture corruptStore = fixture();
        Files.writeString(corruptStore.webAdminDir.resolve("web_admin_channel_metadata.json"), "{bad json", StandardCharsets.UTF_8);
        SnapshotCollectionResult collection = WebAdminSnapshotStore.collect(corruptStore.specs);
        requireTrue(collection.degraded(), "bad store collection degrades safely");
        String warnings = String.join("\n", collection.warnings());
        requireFalse(warnings.contains("Expected") || warnings.contains("line 1"), "bad store warning hides parser details from UI");
    }

    private static void testPackageFingerprintMismatchBlocksRollbackPlan() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeChannelMetadata(fixture.webAdminDir, "Trusted");
        SnapshotRecord target = create(service, fixture, SnapshotKind.MANUAL, "可信保存点", "manual");
        Path packagePath = WebAdminSnapshotStore.packagePath(fixture.snapshotRoot, target.snapshotId);
        String tampered = Files.readString(packagePath, StandardCharsets.UTF_8).replace("Trusted", "Tampered");
        Files.writeString(packagePath, tampered, StandardCharsets.UTF_8);

        RollbackPlan plan = service.buildRollbackPlan(fixture.snapshotRoot, fixture.specs, target.snapshotId);
        requireFalse(plan.blockers.isEmpty(), "fingerprint mismatch blocks rollback dry-run");
        requireContains(String.join("\n", plan.blockers), "指纹", "fingerprint mismatch is explained");
    }

    private static void testRollbackPlanAndApplyRestoresStoreFiles() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeChannelMetadata(fixture.webAdminDir, "Before");
        SnapshotRecord target = create(service, fixture, SnapshotKind.MANUAL, "目标保存点", "manual");

        writeChannelMetadata(fixture.webAdminDir, "After");
        SnapshotRecord preRollback = create(service, fixture, SnapshotKind.PRE_ROLLBACK, "回滚前保护点", "pre_rollback");
        requireTrue("pre_rollback".equals(preRollback.kind), "pre_rollback snapshot is persisted before rollback apply");
        SnapshotRecord annotatedPreRollback = service.updatePreRollbackOperationDiff(fixture.snapshotRoot, preRollback.snapshotId, target.snapshotId);
        requireTrue(annotatedPreRollback != null && annotatedPreRollback.operationDiff.summary.updated > 0, "pre_rollback operation diff shows rollback changes");
        requireTrue(annotatedPreRollback.operationDiff.entries.stream().anyMatch(entry ->
                "updated".equals(entry.changeType)
                        && entry.beforeJsonPreview.contains("After")
                        && entry.afterJsonPreview.contains("Before")
        ), "pre_rollback operation diff direction is current before rollback to rollback target");

        RollbackPlan plan = service.buildRollbackPlan(fixture.snapshotRoot, fixture.specs, target.snapshotId);
        requireTrue(plan.blockers.isEmpty(), "rollback dry-run has no blockers");
        requireTrue(plan.operations.stream().anyMatch(op -> "channel_metadata".equals(op.pathKey) && "update".equals(op.operation)), "rollback plan includes channel metadata update");
        service.applyRollbackFiles(fixture.snapshotRoot, fixture.specs, plan);
        String restored = Files.readString(fixture.webAdminDir.resolve("web_admin_channel_metadata.json"), StandardCharsets.UTF_8);
        requireContains(restored, "Before", "rollback apply restores target file content");
        requireFalse(restored.contains("After"), "rollback apply removes newer content");
    }

    private static void testRollbackRestoresSignalDeviceConfigAndPreservesRuntime() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeSignalDevices(fixture.tzzModDir, "Device Before", "old-result");
        SnapshotRecord target = create(service, fixture, SnapshotKind.MANUAL, "设备保存点", "manual");

        writeSignalDevices(fixture.tzzModDir, "Device After", "live-result");
        RollbackPlan plan = service.buildRollbackPlan(fixture.snapshotRoot, fixture.specs, target.snapshotId);
        requireTrue(plan.blockers.isEmpty(), "signal device rollback dry-run has no blockers");
        requireTrue(plan.operations.stream().anyMatch(op -> "signal_devices".equals(op.pathKey) && "update".equals(op.operation)), "rollback plan includes signal device update");
        service.applyRollbackFiles(fixture.snapshotRoot, fixture.specs, plan);

        String restored = Files.readString(fixture.tzzModDir.resolve("signal_devices.json"), StandardCharsets.UTF_8);
        requireContains(restored, "Device Before", "rollback restores signal device configuration");
        requireContains(restored, "live-result", "rollback preserves current signal device runtime fields");
        requireFalse(restored.contains("Device After"), "rollback removes newer signal device config name");
        requireFalse(restored.contains("old-result"), "rollback does not revert signal device runtime result");
    }

    private static void testRollbackPreservesExistingStateVariableValues() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeStateVariables(fixture.webAdminDir, "Snapshot Name", "1", 1L);
        SnapshotRecord target = create(service, fixture, SnapshotKind.MANUAL, "状态定义保存点", "manual");

        writeStateVariables(fixture.webAdminDir, "Current Name", "99", 8L);
        RollbackPlan plan = service.buildRollbackPlan(fixture.snapshotRoot, fixture.specs, target.snapshotId);
        requireTrue(plan.blockers.isEmpty(), "state variable rollback dry-run has no blockers");
        requireTrue(plan.operations.stream().anyMatch(op -> "state_variables".equals(op.pathKey) && "update".equals(op.operation)), "rollback plan includes state variable definition update");
        service.applyRollbackFiles(fixture.snapshotRoot, fixture.specs, plan);

        String restored = Files.readString(fixture.webAdminDir.resolve("state_variables.json"), StandardCharsets.UTF_8);
        requireContains(restored, "Snapshot Name", "rollback restores state variable definition metadata");
        requireContains(restored, "\"value\":\"99\"", "rollback preserves current state variable value for existing definitions");
        requireFalse(restored.contains("\"value\":\"1\""), "rollback does not revert existing state variable value");
    }


    private static void testAutoRetentionProtectsManualAndPreRollback() throws Exception {
        Fixture fixture = fixture();
        WebAdminSnapshotService service = service();
        writeChannelMetadata(fixture.webAdminDir, "Retained");
        SnapshotRecord manual = create(service, fixture, SnapshotKind.MANUAL, "手动保护", "manual");
        SnapshotRecord preRollback = create(service, fixture, SnapshotKind.PRE_ROLLBACK, "回滚保护", "pre_rollback");
        for (int i = 0; i < WebAdminSnapshotStore.AUTO_RETENTION_LIMIT + 5; i++) {
            writeChannelMetadata(fixture.webAdminDir, "Auto-" + i);
            create(service, fixture, SnapshotKind.AUTO, "自动 " + i, "auto");
        }
        ManifestLoadResult loaded = WebAdminSnapshotStore.loadManifest(fixture.snapshotRoot);
        long autoCount = loaded.manifest().records.stream().filter(record -> "auto".equals(record.kind)).count();
        requireTrue(autoCount <= WebAdminSnapshotStore.AUTO_RETENTION_LIMIT, "auto retention caps old auto snapshots");
        requireTrue(loaded.manifest().records.stream().anyMatch(record -> record.snapshotId.equals(manual.snapshotId)), "manual snapshot is retained");
        requireTrue(loaded.manifest().records.stream().anyMatch(record -> record.snapshotId.equals(preRollback.snapshotId)), "pre_rollback snapshot is retained");
    }

    private static void testCollectExcludesForbiddenDirectories() throws Exception {
        Fixture fixture = fixture();
        writeChannelMetadata(fixture.webAdminDir, "Visible");
        Files.createDirectories(fixture.root.resolve("logs"));
        Files.writeString(fixture.root.resolve("logs/runtime.log"), "secret", StandardCharsets.UTF_8);
        Files.createDirectories(fixture.root.resolve(".codex"));
        Files.writeString(fixture.root.resolve(".codex/session.json"), "secret", StandardCharsets.UTF_8);
        Files.createDirectories(fixture.root.resolve("build"));
        Files.writeString(fixture.root.resolve("build/output.json"), "{\"bad\":true}", StandardCharsets.UTF_8);
        Files.createDirectories(fixture.root.resolve("node_modules"));
        Files.writeString(fixture.root.resolve("node_modules/pkg.json"), "{\"bad\":true}", StandardCharsets.UTF_8);

        SnapshotCollectionResult collection = WebAdminSnapshotStore.collect(fixture.specs);
        String joinedPaths = collection.resources().stream()
                .map(resource -> String.valueOf(resource.metadata.getOrDefault("path", "")))
                .reduce("", (left, right) -> left + "\n" + right);
        requireFalse(joinedPaths.contains("logs") || joinedPaths.contains(".codex") || joinedPaths.contains("build") || joinedPaths.contains("node_modules"), "snapshot collect excludes forbidden directories");
    }

    private static SnapshotRecord create(WebAdminSnapshotService service, Fixture fixture, SnapshotKind kind, String title, String reason) {
        SnapshotTrigger trigger = new SnapshotTrigger();
        trigger.operation = kind.id();
        trigger.module = "Test";
        trigger.targetType = "test";
        trigger.targetId = title;
        trigger.reason = reason;
        return service.createSnapshot(
                fixture.snapshotRoot,
                WebAdminSnapshotStore.collect(fixture.specs),
                kind,
                "tester",
                title,
                "note",
                List.of(reason),
                trigger
        );
    }

    private static WebAdminSnapshotService service() {
        return new WebAdminSnapshotService(null, null, null);
    }

    private static Fixture fixture() throws Exception {
        Path root = Files.createTempDirectory("tzz-webadmin-snapshot-test");
        Path webAdminDir = root.resolve("world").resolve("tzz").resolve("webadmin");
        Path tzzModDir = root.resolve("world").resolve("tzz_mod");
        Files.createDirectories(webAdminDir);
        Files.createDirectories(tzzModDir);
        Path snapshotRoot = webAdminDir.resolve(WebAdminSnapshotStore.SNAPSHOT_DIR);
        return new Fixture(root, webAdminDir, tzzModDir, snapshotRoot, WebAdminSnapshotStore.storeSpecs(webAdminDir, tzzModDir));
    }

    private static void writeChannelMetadata(Path webAdminDir, String displayName) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "channels": {
                    "snap.alpha": {
                      "channel": "snap.alpha",
                      "displayName": "%s",
                      "note": "snapshot test"
                    }
                  }
                }
                """.formatted(displayName);
        Files.writeString(webAdminDir.resolve("web_admin_channel_metadata.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeAllDiffResources(Fixture fixture, String suffix) throws Exception {
        writeChannelMetadata(fixture.webAdminDir, "Channel " + suffix);
        writeLogicChainMetadata(fixture.webAdminDir, "Logic Chain " + suffix);
        writeTimer(fixture.webAdminDir, "timer.diff", "Timer " + suffix, "Timer note " + suffix);
        writeSignalJoin(fixture.webAdminDir, "Join " + suffix);
        writeSignalListener(fixture.tzzModDir, "Listener " + suffix, "diff." + suffix.toLowerCase());
        writeConditionGroup(fixture.webAdminDir, "Condition " + suffix);
        writeStateVariables(fixture.webAdminDir, "State " + suffix, "1", 1L);
        writeTemplate(fixture.webAdminDir, "Template " + suffix);
        writeRegionController(fixture.tzzModDir, "Region " + suffix);
        writeSignalDevices(fixture.tzzModDir, "Device " + suffix, "runtime");
    }

    private static void writeTimer(Path webAdminDir, String timerId, String displayName, String note) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "version": 1,
                  "timers": {
                    "%s": {
                      "id": "%s",
                      "displayName": "%s",
                      "note": "%s",
                      "enabled": true,
                      "mode": "DELAY",
                      "durationTicks": 40,
                      "intervalTicks": 20,
                      "maxRuns": 1,
                      "scope": "GLOBAL",
                      "startPolicy": "RESTART",
                      "outputChannel": "timer.done",
                      "onTickActions": [],
                      "onCompleteActions": []
                    }
                  }
                }
                """.formatted(timerId, timerId, displayName, note);
        Files.writeString(webAdminDir.resolve("timers.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeLogicChainMetadata(Path webAdminDir, String displayName) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "chains": {
                    "logic.diff": {
                      "chainId": "logic.diff",
                      "displayName": "%s",
                      "note": "snapshot logic chain metadata",
                      "rootType": "channel",
                      "rootRef": "snap.alpha",
                      "includeDisabled": true,
                      "maxDepth": 5,
                      "layoutPreference": "auto"
                    }
                  }
                }
                """.formatted(displayName);
        Files.writeString(webAdminDir.resolve("web_admin_logic_chain_metadata.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeSignalJoin(Path webAdminDir, String displayName) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "version": 1,
                  "joins": {
                    "join.diff": {
                      "id": "join.diff",
                      "displayName": "%s",
                      "mode": "ALL",
                      "inputChannels": ["a", "b"],
                      "outputChannel": "joined"
                    }
                  }
                }
                """.formatted(displayName);
        Files.writeString(webAdminDir.resolve("signal_joins.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeSignalListener(Path tzzModDir, String name, String channel) throws Exception {
        Files.createDirectories(tzzModDir);
        String json = """
                {
                  "version": 1,
                  "listeners": [
                    {
                      "id": "listener.diff",
                      "name": "%s",
                      "channel": "%s",
                      "enabled": true,
                      "cooldownTicks": 20,
                      "actions": []
                    }
                  ]
                }
                """.formatted(name, channel);
        Files.writeString(tzzModDir.resolve("signal_listeners.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeConditionGroup(Path webAdminDir, String displayName) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "version": 1,
                  "groups": {
                    "condition.diff": {
                      "id": "condition.diff",
                      "displayName": "%s",
                      "logic": "ALL",
                      "nodes": [
                        {"id":"node-1","type":"PLAYER_ONLINE","enabled":true}
                      ]
                    }
                  }
                }
                """.formatted(displayName);
        Files.writeString(webAdminDir.resolve("condition_groups.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeTemplate(Path webAdminDir, String displayName) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "version": 1,
                  "templates": {
                    "template.diff": {
                      "templateId": "template.diff",
                      "displayName": "%s",
                      "note": "template note",
                      "resources": [
                        {"type":"timer","id":"timer.diff"}
                      ]
                    }
                  }
                }
                """.formatted(displayName);
        Files.writeString(webAdminDir.resolve("templates.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeRegionController(Path tzzModDir, String name) throws Exception {
        Files.createDirectories(tzzModDir);
        String json = """
                {
                  "version": 1,
                  "controllers": [
                    {
                      "id": "region.diff",
                      "name": "%s",
                      "enabled": true,
                      "enterActions": []
                    }
                  ]
                }
                """.formatted(name);
        Files.writeString(tzzModDir.resolve("region_controllers.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeStateVariables(Path webAdminDir, String displayName, String value, long version) throws Exception {
        Files.createDirectories(webAdminDir);
        String json = """
                {
                  "version": 1,
                  "variables": [
                    {
                      "id": "state-score",
                      "scope": "GLOBAL",
                      "targetId": "global",
                      "key": "score",
                      "type": "INTEGER",
                      "value": "%s",
                      "displayName": "%s",
                      "note": "snapshot state definition",
                      "updatedAt": 1000,
                      "updatedBy": "tester",
                      "version": %d,
                      "fingerprint": ""
                    }
                  ]
                }
                """.formatted(value, displayName, version);
        Files.writeString(webAdminDir.resolve("state_variables.json"), json, StandardCharsets.UTF_8);
    }

    private static void writeSignalDevices(Path tzzModDir, String name, String lastResult) throws Exception {
        Files.createDirectories(tzzModDir);
        String json = """
                {
                  "version": 1,
                  "devices": [
                    {
                      "id": "device-1",
                      "type": "virtual_block_device",
                      "name": "%s",
                      "dimension": "minecraft:overworld",
                      "x": 1,
                      "y": 64,
                      "z": 2,
                      "channel": "snap.device",
                      "enabled": true,
                      "lastResult": "%s"
                    }
                  ]
                }
                """.formatted(name, lastResult);
        Files.writeString(tzzModDir.resolve("signal_devices.json"), json, StandardCharsets.UTF_8);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String text, String marker, String message) {
        if (text == null || !text.contains(marker)) {
            throw new AssertionError(message + " missing=" + marker);
        }
    }

    private record Fixture(Path root, Path webAdminDir, Path tzzModDir, Path snapshotRoot, List<StoreSpec> specs) {
    }
}
