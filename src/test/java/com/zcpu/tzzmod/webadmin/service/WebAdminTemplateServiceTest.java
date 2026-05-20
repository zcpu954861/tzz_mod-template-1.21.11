package com.zcpu.tzzmod.webadmin.service;

import com.google.gson.Gson;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinStore;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminLogicChainMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminTemplateStore;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTemplateRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.template.WebAdminBuiltInTemplates;
import com.zcpu.tzzmod.webadmin.template.WebAdminTemplatePackage;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WebAdminTemplateServiceTest {
    private static final Gson GSON = new Gson();

    private WebAdminTemplateServiceTest() {
    }

    public static void run() throws Exception {
        testBuiltInListDetailAndJsonExport();
        testTemplateDetailLookupSourcesAndErrors();
        testImportPreviewSaveAndDoesNotApply();
        testInvalidUnknownJsonAndBadStoreFallback();
        testDryRunAndApplyJoinTemplateWritesRealStores();
        testTimerAndListenerTemplatesWriteRealStores();
        testPlaceholderAndDeferredResourcesBlockApply();
        testSemanticResourceValidationBlocksApply();
        testExternalReferencesAndConditionGroupsBlockApply();
        testExistingRootChannelAndLogicMetadataConflict();
        testStaleFingerprintAndImportSecurity();
        testSecurityLockConfirmationAndRealtime();
    }

    private static void testBuiltInListDetailAndJsonExport() throws Exception {
        Fixture fixture = fixture(false);
        Map<?, ?> list = fixture.service.list(null, fixture.editor, fixture.session);
        requireEquals(3, ((Number) list.get("builtInCount")).intValue(), "8.15 exposes three built-in templates");
        requireContainsTemplate((List<?>) list.get("templates"), "join_all_two_inputs", "built_in");
        requireContainsTemplate((List<?>) list.get("templates"), "timer_delay_with_start_listener", "built_in");
        requireContainsTemplate((List<?>) list.get("templates"), "listener_message_action", "built_in");
        requireEquals(WebAdminTemplateStore.FILE_NAME, string(list.get("userTemplateStore")), "8.15 user template store file name");
        requireEquals(Boolean.TRUE, list.get("worldScoped"), "8.15 user template store is world scoped");

        for (String builtInId : List.of("join_all_two_inputs", "timer_delay_with_start_listener", "listener_message_action")) {
            Map<?, ?> detail = fixture.service.detail(null, fixture.editor, fixture.session, "built_in", builtInId);
            requireFalse(Boolean.TRUE.equals(detail.get("notFound")), "built-in detail opens by explicit source: " + builtInId);
            requireEquals("tzz_template_v1", string(detail.get("schema")), "detail exposes schema");
            requireEquals("built_in", string(detail.get("source")), "built-in detail preserves source");
            requireTrue(string(detail.get("json")).contains(builtInId), "built-in detail exposes JSON preview");
            requireTrue(detail.get("resources") instanceof Map<?, ?>, "built-in detail exposes resources");
            requireTrue(detail.containsKey("parameters"), "built-in detail exposes parameters");
            requireEquals(Boolean.FALSE, detail.get("componentExportSupported"), "component export is explicitly deferred");
            requireTrue(string(detail.get("componentExportDeferredReason")).contains("deferred") || string(detail.get("componentExportDeferredReason")).contains("导出"), "component export has deferred reason");
        }
        String json = string(fixture.service.exportTemplate(null, fixture.editor, fixture.session, "built_in", "join_all_two_inputs").get("json"));
        WebAdminTemplatePackage roundTrip = GSON.fromJson(json, WebAdminTemplatePackage.class).normalized();
        requireEquals(WebAdminTemplatePackage.SCHEMA, roundTrip.schema, "export JSON roundtrips schema");
        requireEquals("join_all_two_inputs", roundTrip.templateId, "export JSON roundtrips template id");
        requireEquals("built_in", roundTrip.metadata.source, "export JSON preserves built-in source");
    }

    private static void testTemplateDetailLookupSourcesAndErrors() throws Exception {
        Fixture fixture = fixture(true);
        Map<?, ?> builtInByBlankSource = fixture.service.detail(null, fixture.editor, fixture.session, "", "join_all_two_inputs");
        requireEquals("built_in", string(builtInByBlankSource.get("source")), "blank source resolves built-in before user store");
        Map<?, ?> builtInByImportedAlias = fixture.service.detail(null, fixture.editor, fixture.session, "imported", "join_all_two_inputs");
        requireEquals(Boolean.TRUE, builtInByImportedAlias.get("notFound"), "imported alias searches user templates, not built-in");
        requireEquals("template_not_found", string(builtInByImportedAlias.get("code")), "missing imported alias returns template_not_found");

        String json = string(fixture.service.exportTemplate(null, fixture.editor, fixture.session, "built_in", "listener_message_action").get("json"));
        WebAdminTemplateRequest importRequest = new WebAdminTemplateRequest();
        importRequest.packageJson = json;
        importRequest.importedTemplateId = "custom.detail.lookup";
        importRequest.importedDisplayName = "用户模板详情";
        importRequest.expectedFingerprint = string(fixture.service.list(null, fixture.editor, fixture.session).get("expectedFingerprint"));
        importRequest.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForImport(), "user-template-store");
        requireTrue(fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, true).success(), "user template fixture imports");
        Map<?, ?> userDetail = fixture.service.detail(null, fixture.editor, fixture.session, "user", "custom.detail.lookup");
        requireEquals("user", string(userDetail.get("source")), "user template detail still works");
        Map<?, ?> unifiedUserDetail = fixture.service.detail(null, fixture.editor, fixture.session, "", "custom.detail.lookup");
        requireEquals("user", string(unifiedUserDetail.get("source")), "blank source resolves user template when no built-in id exists");

        Map<?, ?> missing = fixture.service.detail(null, fixture.editor, fixture.session, "built_in", "missing.template");
        requireEquals(Boolean.TRUE, missing.get("notFound"), "missing template is marked notFound");
        requireEquals("template_not_found", string(missing.get("code")), "missing template returns template_not_found");
        Map<?, ?> invalidSource = fixture.service.detail(null, fixture.editor, fixture.session, "bad_source", "join_all_two_inputs");
        requireEquals(Boolean.FALSE, invalidSource.get("notFound"), "invalid source is not collapsed into not found");
        requireEquals("template_source_invalid", string(invalidSource.get("code")), "invalid source returns template_source_invalid");
        Map<?, ?> denied = fixture.service.detail(null, null, null, "built_in", "join_all_two_inputs");
        requireEquals(Boolean.TRUE, denied.get("permissionDenied"), "permission failure is distinct from not found");
        requireEquals("template_permission_denied", string(denied.get("code")), "permission failure returns template_permission_denied");
    }

    private static void testImportPreviewSaveAndDoesNotApply() throws Exception {
        Fixture fixture = fixture(true);
        String json = string(fixture.service.exportTemplate(null, fixture.editor, fixture.session, "built_in", "listener_message_action").get("json"));
        WebAdminTemplateRequest previewRequest = new WebAdminTemplateRequest();
        previewRequest.packageJson = json;
        previewRequest.importedTemplateId = "custom.listener.message";
        previewRequest.importedDisplayName = "自定义消息监听器";

        WebAdminWriteResult preview = fixture.service.previewImport(null, fixture.editor, fixture.session, previewRequest);
        requireTrue(preview.success(), "valid template import preview succeeds");
        requireFalse(preview.changed(), "import preview is read-only");
        requireEquals(Boolean.TRUE, preview.data().get("importDoesNotApply"), "import preview documents that it does not apply");

        WebAdminTemplateRequest saveRequest = previewRequest;
        saveRequest.expectedFingerprint = string(fixture.service.list(null, fixture.editor, fixture.session).get("expectedFingerprint"));
        saveRequest.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForImport(), "user-template-store");
        WebAdminWriteResult saved = fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", saveRequest, fixture.csrf, true);
        requireTrue(saved.success(), "valid template import saves user template");
        requireTrue(saved.changed(), "import save changes user template store");
        requireEquals(Boolean.TRUE, saved.data().get("importDoesNotApply"), "import save still does not apply config");
        requireContainsTemplate((List<?>) fixture.service.list(null, fixture.editor, fixture.session).get("templates"), "custom.listener.message", "user");
        requireFalse(Files.exists(fixture.dir.resolve(SignalListenerStore.FILE_NAME)), "import JSON does not create SignalListener config");
        requireFalse(Files.exists(fixture.dir.resolve(SignalJoinStore.FILE_NAME)), "import JSON does not create SignalJoin config");
        requireFalse(Files.exists(fixture.dir.resolve(TimerStore.FILE_NAME)), "import JSON does not create Timer config");

        WebAdminTemplateRequest duplicate = previewRequest;
        duplicate.expectedFingerprint = string(fixture.service.list(null, fixture.editor, fixture.session).get("expectedFingerprint"));
        duplicate.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForImport(), "user-template-store");
        WebAdminWriteResult duplicateResult = fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", duplicate, fixture.csrf, true);
        requireFalse(duplicateResult.success(), "duplicate imported template id is rejected");
        requireValidationCode(duplicateResult, "template_id_duplicate");
    }

    private static void testInvalidUnknownJsonAndBadStoreFallback() throws Exception {
        Fixture fixture = fixture(false);
        WebAdminTemplateRequest invalid = new WebAdminTemplateRequest();
        invalid.packageJson = "{bad json";
        WebAdminWriteResult invalidResult = fixture.service.previewImport(null, fixture.editor, fixture.session, invalid);
        requireFalse(invalidResult.success(), "invalid JSON import preview fails");
        requireValidationCode(invalidResult, "template_json_invalid");

        WebAdminTemplatePackage unknown = WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized();
        unknown.schema = "tzz_template_v999";
        WebAdminTemplateRequest unknownRequest = new WebAdminTemplateRequest();
        unknownRequest.packageJson = GSON.toJson(unknown);
        WebAdminWriteResult unknownResult = fixture.service.previewImport(null, fixture.editor, fixture.session, unknownRequest);
        requireFalse(unknownResult.success(), "unknown template schema fails");
        requireTrue(validationMessage(unknownResult).contains("不受支持"), "unknown schema reports Chinese unsupported message");

        WebAdminTemplateRequest missingSchema = new WebAdminTemplateRequest();
        missingSchema.packageJson = GSON.toJson(WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized()).replace("\"schema\":\"tzz_template_v1\",", "");
        WebAdminWriteResult missingSchemaResult = fixture.service.previewImport(null, fixture.editor, fixture.session, missingSchema);
        requireFalse(missingSchemaResult.success(), "missing template schema fails");
        requireValidationCode(missingSchemaResult, "template_schema_required");

        String missingStoreSchemaTemplate = GSON.toJson(WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized()).replace("\"schema\":\"tzz_template_v1\",", "");
        Files.writeString(
                fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME),
                "{\"version\":1,\"templates\":{\"missing.store.schema\":" + missingStoreSchemaTemplate + "}}",
                StandardCharsets.UTF_8
        );
        Map<?, ?> missingStoreSchemaList = fixture.service.list(null, fixture.editor, fixture.session);
        requireEquals(Boolean.TRUE, missingStoreSchemaList.get("storeDegraded"), "templates.json record missing schema degrades list");
        WebAdminTemplateRequest blockedByMissingStoreSchema = new WebAdminTemplateRequest();
        blockedByMissingStoreSchema.packageJson = GSON.toJson(WebAdminBuiltInTemplates.find("join_all_two_inputs"));
        blockedByMissingStoreSchema.expectedFingerprint = string(missingStoreSchemaList.get("expectedFingerprint"));
        WebAdminWriteResult missingStoreSchemaWrite = fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", blockedByMissingStoreSchema, fixture.csrf, true);
        requireFalse(missingStoreSchemaWrite.success(), "templates.json missing schema record blocks import write");
        requireTrue(missingStoreSchemaWrite.message().contains("模板记录无效") || missingStoreSchemaWrite.message().contains("schema"), "missing schema store write failure is Chinese");

        Files.writeString(fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME), "{broken", StandardCharsets.UTF_8);
        Map<?, ?> degradedList = fixture.service.list(null, fixture.editor, fixture.session);
        requireEquals(Boolean.TRUE, degradedList.get("storeDegraded"), "bad templates.json degrades list");
        WebAdminTemplateRequest importRequest = new WebAdminTemplateRequest();
        importRequest.packageJson = GSON.toJson(WebAdminBuiltInTemplates.find("join_all_two_inputs"));
        importRequest.expectedFingerprint = string(degradedList.get("expectedFingerprint"));
        WebAdminWriteResult blocked = fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, true);
        requireFalse(blocked.success(), "bad templates.json blocks import write");
        requireTrue(blocked.message().contains("读取失败") || blocked.message().contains("停止写入"), "bad store write failure is Chinese");
    }

    private static void testDryRunAndApplyJoinTemplateWritesRealStores() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminTemplateRequest request = applyRequest("join_all_two_inputs", "alpha");
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(dryRun.success(), "Join template dry-run succeeds");
        requireEquals(Boolean.TRUE, dryRun.data().get("ok"), "Join dry-run is applyable");
        requireEquals(3, ((List<?>) dryRun.data().get("createChannels")).size(), "Join dry-run creates channel metadata");
        requireEquals(1, ((List<?>) dryRun.data().get("createSignalJoins")).size(), "Join dry-run creates one SignalJoin");
        request.expectedFingerprint = string(dryRun.data().get("expectedFingerprint"));

        request.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForApply(), WebAdminTemplateService.applyLockTargetId(request));
        WebAdminWriteResult unconfirmed = fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(unconfirmed.success(), "apply requires explicit confirmation");
        requireValidationCode(unconfirmed, "template_apply_confirmation_required");

        request.confirmed = true;
        WebAdminWriteResult applied = fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(applied.success(), "Join template apply succeeds");
        requireTrue(applied.changed(), "Join apply reports changed");
        requireTrue(string(applied.data().get("routeTarget")).contains("rootRef=alpha.input_a"), "Join apply returns Logic Chain route target");
        requireTrue(SignalJoinStore.loadWithStatus(fixture.dir.resolve(SignalJoinStore.FILE_NAME)).file().joins.containsKey("alpha.join.main"), "Join apply writes real SignalJoin store");
        WebAdminChannelMetadataStore.MetadataFile channels = readJson(fixture.dir.resolve("web_admin_channel_metadata.json"), WebAdminChannelMetadataStore.MetadataFile.class).normalized();
        requireTrue(channels.channels.containsKey("alpha.input_a"), "Join apply writes channel metadata input_a");
        requireTrue(channels.channels.containsKey("alpha.output_c"), "Join apply writes channel metadata output_c");
        WebAdminLogicChainMetadataStore.MetadataFile logic = readJson(fixture.dir.resolve("web_admin_logic_chain_metadata.json"), WebAdminLogicChainMetadataStore.MetadataFile.class).normalized();
        requireTrue(logic.chains.containsKey("alpha.template"), "Join apply writes Logic Chain metadata");

        WebAdminWriteResult conflictDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, applyRequest("join_all_two_inputs", "alpha"), fixture.csrf, true);
        requireTrue(conflictDryRun.success(), "repeat same prefix dry-run returns structured preview");
        requireEquals(Boolean.FALSE, conflictDryRun.data().get("ok"), "repeat same prefix detects conflicts");
        requireFalse(((List<?>) conflictDryRun.data().get("conflicts")).isEmpty(), "repeat same prefix has conflict list");

        WebAdminTemplateRequest beta = applyRequest("join_all_two_inputs", "beta");
        WebAdminWriteResult betaDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, beta, fixture.csrf, true);
        beta.expectedFingerprint = string(betaDryRun.data().get("expectedFingerprint"));
        beta.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForApply(), WebAdminTemplateService.applyLockTargetId(beta));
        beta.confirmed = true;
        requireTrue(fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", beta, fixture.csrf, true).success(), "repeat with different prefix succeeds");
    }

    private static void testTimerAndListenerTemplatesWriteRealStores() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminTemplateRequest timerRequest = applyRequest("timer_delay_with_start_listener", "delaycase");
        applyWithLock(fixture, timerRequest);
        TimerDefinition timer = TimerStore.loadWithStatus(fixture.dir.resolve(TimerStore.FILE_NAME)).file().timers.get("delaycase.timer.delay");
        requireTrue(timer != null, "Timer template writes real Timer config");
        requireEquals("delaycase.timer_done", timer.outputChannel, "Timer template remaps output channel");
        SignalListenerData startListener = findListener(fixture, "delaycase.listener.start_timer");
        requireTrue(startListener != null, "Timer template writes real SignalListener config");
        requireEquals("delaycase.start", startListener.channel(), "Timer start listener remaps start channel");
        requireEquals("delaycase.timer.delay", startListener.actions().getFirst().timerId(), "Timer start action remaps timer id");

        WebAdminTemplateRequest listenerRequest = applyRequest("listener_message_action", "messagecase");
        applyWithLock(fixture, listenerRequest);
        SignalListenerData messageListener = findListener(fixture, "messagecase.listener.message");
        requireTrue(messageListener != null, "Listener template writes real SignalListener config");
        requireEquals("messagecase.input", messageListener.channel(), "Listener template remaps input channel");
        requireEquals(ActionType.MESSAGE, messageListener.actions().getFirst().type(), "Listener template preserves message action");
    }

    private static void testPlaceholderAndDeferredResourcesBlockApply() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminTemplatePackage template = WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized();
        template.templateId = "deferred.placeholder";
        WebAdminTemplatePackage.Placeholder placeholder = new WebAdminTemplatePackage.Placeholder();
        placeholder.id = "existing_region";
        placeholder.type = "region";
        placeholder.displayName = "已有区域";
        placeholder.required = true;
        template.resources.placeholders = List.of(placeholder);
        template.resources.stateVariables = List.of(Map.of("id", "state.demo"));
        template.resources.conditionGroups = List.of(Map.of("id", "condition.demo"));
        WebAdminTemplatePackage.ActionResource action = new WebAdminTemplatePackage.ActionResource();
        action.id = "orphan.action";
        action.ownerType = "listener";
        action.ownerId = "listener.missing";
        template.resources.actions = List.of(action);
        WebAdminTemplateStore.TemplateFile file = new WebAdminTemplateStore.TemplateFile();
        file.templates.put(template.templateId, template);
        requireTrue(WebAdminTemplateStore.save(fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME), file), "save deferred user template fixture");

        WebAdminTemplateRequest request = applyRequest("deferred.placeholder", "deferred");
        request.source = "user";
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(dryRun.success(), "deferred template dry-run returns structured result");
        requireEquals(Boolean.FALSE, dryRun.data().get("ok"), "unmapped placeholder/deferred resources block apply");
        requireFalse(((List<?>) dryRun.data().get("missingPlaceholders")).isEmpty(), "missing placeholder is listed");
        requireEquals(4, ((List<?>) dryRun.data().get("deferredResources")).size(), "placeholder binding, top-level action, state variable and condition group apply are deferred");

        request.expectedFingerprint = string(dryRun.data().get("expectedFingerprint"));
        request.confirmed = true;
        request.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForApply(), WebAdminTemplateService.applyLockTargetId(request));
        WebAdminWriteResult applied = fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireFalse(applied.success(), "apply rejects missing placeholders/deferred resources");
        requireValidationCode(applied, "template_placeholder_missing");
        requireValidationCode(applied, "template_resource_deferred");
    }

    private static void testSemanticResourceValidationBlocksApply() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminTemplatePackage template = WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized();
        template.templateId = "bad.join.semantic";
        template.resources.signalJoins.getFirst().definition.inputChannels = List.of(new SignalJoinInputDefinition("input_a", "输入 A", "", 1));
        WebAdminTemplateStore.TemplateFile file = new WebAdminTemplateStore.TemplateFile();
        file.templates.put(template.templateId, template);
        requireTrue(WebAdminTemplateStore.save(fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME), file), "save semantic invalid user template fixture");

        WebAdminTemplateRequest request = applyRequest("bad.join.semantic", "badsemantic");
        request.source = "user";
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(dryRun.success(), "semantic invalid template dry-run returns structured result");
        requireEquals(Boolean.FALSE, dryRun.data().get("ok"), "semantic invalid SignalJoin blocks apply");
        requireDataValidationCode(dryRun, "template_signal_join_all_requires_two_inputs");
    }

    private static void testExternalReferencesAndConditionGroupsBlockApply() throws Exception {
        Fixture fixture = fixture(true);

        WebAdminTemplatePackage externalChannel = WebAdminBuiltInTemplates.find("join_all_two_inputs").normalized();
        externalChannel.templateId = "bad.external.channel";
        externalChannel.resources.signalJoins.getFirst().definition.outputChannel = "server.existing.channel";
        saveUserTemplate(fixture, externalChannel);
        WebAdminTemplateRequest externalChannelRequest = applyRequest("bad.external.channel", "externalchannel");
        externalChannelRequest.source = "user";
        WebAdminWriteResult externalChannelDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, externalChannelRequest, fixture.csrf, true);
        requireTrue(externalChannelDryRun.success(), "external channel dry-run returns structured result");
        requireEquals(Boolean.FALSE, externalChannelDryRun.data().get("ok"), "external channel reference blocks apply");
        requireDataValidationCode(externalChannelDryRun, "template_channel_reference_external");

        WebAdminTemplatePackage externalTimer = WebAdminBuiltInTemplates.find("timer_delay_with_start_listener").normalized();
        externalTimer.templateId = "bad.external.timer";
        externalTimer.resources.signalListeners.getFirst().listener = new SignalListenerData(
                "listener.start_timer",
                "启动外部 Timer",
                "start",
                true,
                0,
                "",
                List.of(ActionConfig.timerStart("server.timer.external", TimerTargetMode.GLOBAL, "", TimerStartPolicy.RESTART, ""))
        ).normalized();
        saveUserTemplate(fixture, externalTimer);
        WebAdminTemplateRequest externalTimerRequest = applyRequest("bad.external.timer", "externaltimer");
        externalTimerRequest.source = "user";
        WebAdminWriteResult externalTimerDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, externalTimerRequest, fixture.csrf, true);
        requireTrue(externalTimerDryRun.success(), "external timer dry-run returns structured result");
        requireEquals(Boolean.FALSE, externalTimerDryRun.data().get("ok"), "external timer reference blocks apply");
        requireDataValidationCode(externalTimerDryRun, "template_timer_reference_external");

        WebAdminTemplatePackage conditionRef = WebAdminBuiltInTemplates.find("listener_message_action").normalized();
        conditionRef.templateId = "bad.condition.reference";
        conditionRef.resources.signalListeners.getFirst().listener = new SignalListenerData(
                "listener.message",
                "带 ConditionGroup 的监听器",
                "input",
                true,
                0,
                "condition.demo",
                List.of(new ActionConfig(ActionType.MESSAGE, "condition", true, false, 0, false, "condition.demo"))
        ).normalized();
        saveUserTemplate(fixture, conditionRef);
        WebAdminTemplateRequest conditionRequest = applyRequest("bad.condition.reference", "conditionref");
        conditionRequest.source = "user";
        WebAdminWriteResult conditionDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, conditionRequest, fixture.csrf, true);
        requireTrue(conditionDryRun.success(), "condition reference dry-run returns structured result");
        requireEquals(Boolean.FALSE, conditionDryRun.data().get("ok"), "embedded ConditionGroup reference blocks apply");
        requireDataValidationCode(conditionDryRun, "template_condition_group_reference_deferred");

        WebAdminTemplatePackage stateAction = WebAdminBuiltInTemplates.find("listener_message_action").normalized();
        stateAction.templateId = "bad.state.action";
        stateAction.resources.signalListeners.getFirst().listener = new SignalListenerData(
                "listener.message",
                "带状态变量动作的监听器",
                "input",
                true,
                0,
                "",
                List.of(new ActionConfig(ActionType.STATE_VARIABLE, "", true, false, 0, false, ""))
        ).normalized();
        saveUserTemplate(fixture, stateAction);
        WebAdminTemplateRequest stateActionRequest = applyRequest("bad.state.action", "stateaction");
        stateActionRequest.source = "user";
        WebAdminWriteResult stateActionDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, stateActionRequest, fixture.csrf, true);
        requireTrue(stateActionDryRun.success(), "state action dry-run returns structured result");
        requireEquals(Boolean.FALSE, stateActionDryRun.data().get("ok"), "StateVariable action binding blocks apply");
        requireDataValidationCode(stateActionDryRun, "template_state_action_deferred");

        WebAdminTemplatePackage commandAction = WebAdminBuiltInTemplates.find("listener_message_action").normalized();
        commandAction.templateId = "bad.command.action";
        commandAction.resources.signalListeners.getFirst().listener = new SignalListenerData(
                "listener.message",
                "带命令动作的监听器",
                "input",
                true,
                0,
                "",
                List.of(new ActionConfig(ActionType.COMMAND, "say template", true, false, 0, false, ""))
        ).normalized();
        saveUserTemplate(fixture, commandAction);
        WebAdminTemplateRequest commandActionRequest = applyRequest("bad.command.action", "commandaction");
        commandActionRequest.source = "user";
        WebAdminWriteResult commandActionDryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, commandActionRequest, fixture.csrf, true);
        requireTrue(commandActionDryRun.success(), "command action dry-run returns structured result");
        requireEquals(Boolean.FALSE, commandActionDryRun.data().get("ok"), "command action blocks apply");
        requireDataValidationCode(commandActionDryRun, "template_command_action_deferred");
    }

    private static void testExistingRootChannelAndLogicMetadataConflict() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminChannelMetadataStore.MetadataFile channelFile = new WebAdminChannelMetadataStore.MetadataFile();
        WebAdminChannelMetadataStore.MetadataEntry root = new WebAdminChannelMetadataStore.MetadataEntry();
        root.channel = "existing.root";
        root.displayName = "已有 Root";
        channelFile.channels.put(root.channel, root);
        Files.writeString(fixture.dir.resolve("web_admin_channel_metadata.json"), GSON.toJson(channelFile.normalized()), StandardCharsets.UTF_8);

        WebAdminTemplateRequest rootRequest = applyRequest("join_all_two_inputs", "rootcase");
        rootRequest.rootChannel = "existing.root";
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, rootRequest, fixture.csrf, true);
        requireTrue(dryRun.success(), "existing root channel dry-run succeeds");
        requireEquals(Boolean.TRUE, dryRun.data().get("ok"), "existing root metadata is reused without conflict");
        requireEquals(2, ((List<?>) dryRun.data().get("createChannels")).size(), "root mapped channel metadata is not recreated");
        applyWithLock(fixture, rootRequest);
        SignalJoinDefinition join = SignalJoinStore.loadWithStatus(fixture.dir.resolve(SignalJoinStore.FILE_NAME)).file().joins.get("rootcase.join.main");
        requireTrue(join.inputChannelNames().contains("existing.root"), "root channel remaps join input");

        WebAdminLogicChainMetadataStore.MetadataFile logicFile = new WebAdminLogicChainMetadataStore.MetadataFile();
        WebAdminLogicChainMetadataStore.MetadataEntry existing = new WebAdminLogicChainMetadataStore.MetadataEntry();
        existing.id = "logicconflict.template";
        existing.rootRef = "logicconflict.input_a";
        logicFile.chains.put(existing.id, existing);
        Files.writeString(fixture.dir.resolve("web_admin_logic_chain_metadata.json"), GSON.toJson(logicFile.normalized()), StandardCharsets.UTF_8);
        WebAdminWriteResult logicConflict = fixture.service.dryRunApply(null, fixture.editor, fixture.session, applyRequest("join_all_two_inputs", "logicconflict"), fixture.csrf, true);
        requireTrue(logicConflict.success(), "logic metadata conflict dry-run returns structured result");
        requireEquals(Boolean.FALSE, logicConflict.data().get("ok"), "logic metadata conflict blocks apply");
        requireTrue(String.valueOf(logicConflict.data().get("conflicts")).contains("logicChainMetadata"), "logic metadata conflict is listed");
    }

    private static void testStaleFingerprintAndImportSecurity() throws Exception {
        Fixture fixture = fixture(true);
        String json = string(fixture.service.exportTemplate(null, fixture.editor, fixture.session, "built_in", "join_all_two_inputs").get("json"));

        WebAdminTemplateRequest importRequest = new WebAdminTemplateRequest();
        importRequest.packageJson = json;
        importRequest.importedTemplateId = "security.import";
        importRequest.expectedFingerprint = string(fixture.service.list(null, fixture.editor, fixture.session).get("expectedFingerprint"));
        requireEquals("csrf_invalid", fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, "bad-token", true).code(), "import requires CSRF");
        requireEquals("csrf_invalid", fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, false).code(), "import requires same-origin");
        requireEquals("edit_lock_required", fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, true).code(), "import requires edit lock");

        WebAdminUser viewer = user(WebAdminRole.VIEWER);
        WebAdminSession viewerSession = session(viewer);
        requireEquals("permission_denied", fixture.service.importUserTemplate(null, viewer, viewerSession, "127.0.0.1", importRequest, fixture.security.csrfTokenFor(viewerSession), true).code(), "VIEWER cannot import template");

        importRequest.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForImport(), "user-template-store");
        importRequest.expectedFingerprint = "stale";
        requireEquals("conflict_detected", fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, true).code(), "stale import fingerprint is rejected");

        WebAdminTemplateRequest applyRequest = applyRequest("join_all_two_inputs", "stalecase");
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, applyRequest, fixture.csrf, true);
        requireTrue(dryRun.success(), "stale apply fixture dry-run succeeds");
        applyRequest.expectedFingerprint = "stale";
        applyRequest.confirmed = true;
        applyRequest.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForApply(), WebAdminTemplateService.applyLockTargetId(applyRequest));
        requireEquals("conflict_detected", fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", applyRequest, fixture.csrf, true).code(), "stale apply fingerprint is rejected");
        requireFalse(Files.exists(fixture.dir.resolve(SignalJoinStore.FILE_NAME)), "stale apply does not write SignalJoin store");
    }

    private static void testSecurityLockConfirmationAndRealtime() throws Exception {
        Fixture fixture = fixture(true);
        WebAdminTemplateRequest request = applyRequest("listener_message_action", "securecase");
        requireEquals("csrf_invalid", fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, "bad-token", true).code(), "dry-run requires CSRF");
        requireEquals("csrf_invalid", fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, fixture.csrf, false).code(), "dry-run requires same-origin");
        requireEquals("edit_lock_required", fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true).code(), "apply requires edit lock");

        WebAdminUser viewer = user(WebAdminRole.VIEWER);
        WebAdminSession viewerSession = session(viewer);
        String viewerCsrf = fixture.security.csrfTokenFor(viewerSession);
        requireEquals("permission_denied", fixture.service.apply(null, viewer, viewerSession, "127.0.0.1", request, viewerCsrf, true).code(), "VIEWER cannot apply template");

        long baselineSeq = WebAdminRealtimeEventBus.currentSeq();
        WebAdminTemplateRequest importRequest = new WebAdminTemplateRequest();
        importRequest.packageJson = string(fixture.service.exportTemplate(null, fixture.editor, fixture.session, "built_in", "join_all_two_inputs").get("json"));
        importRequest.importedTemplateId = "secure.imported.join";
        importRequest.expectedFingerprint = string(fixture.service.list(null, fixture.editor, fixture.session).get("expectedFingerprint"));
        importRequest.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForImport(), "user-template-store");
        WebAdminWriteResult imported = fixture.service.importUserTemplate(null, fixture.editor, fixture.session, "127.0.0.1", importRequest, fixture.csrf, true);
        requireTrue(imported.success(), "import emits realtime event");
        requireRecentEventSince(baselineSeq, "template_store_changed", "template import realtime");

        WebAdminTemplateRequest applyRequest = applyRequest("listener_message_action", "securecase");
        applyWithLock(fixture, applyRequest);
        requireRecentEventSince(baselineSeq, "template_applied", "template apply realtime");
        requireRecentEventSince(baselineSeq, "config_changed", "template apply config realtime");
    }

    private static WebAdminWriteResult applyWithLock(Fixture fixture, WebAdminTemplateRequest request) {
        WebAdminWriteResult dryRun = fixture.service.dryRunApply(null, fixture.editor, fixture.session, request, fixture.csrf, true);
        requireTrue(dryRun.success(), "template dry-run succeeds before apply for " + request.templateId);
        requireEquals(Boolean.TRUE, dryRun.data().get("ok"), "template dry-run is ok for " + request.templateId);
        request.expectedFingerprint = string(dryRun.data().get("expectedFingerprint"));
        request.lockId = acquireLock(fixture, WebAdminTemplateService.lockTargetTypeForApply(), WebAdminTemplateService.applyLockTargetId(request));
        request.confirmed = true;
        WebAdminWriteResult applied = fixture.service.apply(null, fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(applied.success(), "template apply succeeds for " + request.templateId);
        return applied;
    }

    private static WebAdminTemplateRequest applyRequest(String templateId, String prefix) {
        WebAdminTemplateRequest request = new WebAdminTemplateRequest();
        request.source = "built_in";
        request.templateId = templateId;
        request.prefix = prefix;
        request.displayNamePrefix = prefix + " ";
        return request;
    }

    private static String acquireLock(Fixture fixture, String targetType, String targetId) {
        WebAdminEditLockRequest request = new WebAdminEditLockRequest();
        request.targetType = targetType;
        request.targetId = targetId;
        WebAdminWriteResult result = fixture.editLockService.acquire(fixture.editor, fixture.session, "127.0.0.1", request, fixture.csrf, true);
        requireTrue(result.success(), "acquire template edit lock " + targetType + "/" + targetId);
        Object lock = result.data().get("lock");
        requireTrue(lock instanceof WebAdminEditLockStatusDto, "edit lock result exposes lock status");
        return ((WebAdminEditLockStatusDto) lock).lockId();
    }

    private static SignalListenerData findListener(Fixture fixture, String id) {
        for (SignalListenerData listener : SignalListenerStore.loadWithStatus(fixture.dir.resolve(SignalListenerStore.FILE_NAME)).file().listeners) {
            if (listener.id().equals(id)) {
                return listener;
            }
        }
        return null;
    }

    private static <T> T readJson(Path path, Class<T> type) throws Exception {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    private static void saveUserTemplate(Fixture fixture, WebAdminTemplatePackage template) {
        WebAdminTemplateStore.TemplateFile file = WebAdminTemplateStore.loadWithStatus(fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME)).file();
        file.templates.put(template.templateId, template.normalized());
        requireTrue(WebAdminTemplateStore.save(fixture.dir.resolve(WebAdminTemplateStore.FILE_NAME), file), "save user template fixture " + template.templateId);
    }

    private static Fixture fixture(boolean withLock) throws Exception {
        Path dir = Files.createTempDirectory("tzz-template-service-test");
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminPermissionService permission = new WebAdminPermissionService();
        WebAdminEditLockService editLockService = withLock ? new WebAdminEditLockService(permission, security, 60_000L) : null;
        WebAdminTemplateService service = new WebAdminTemplateService(
                permission,
                security,
                editLockService,
                dir.resolve(WebAdminTemplateStore.FILE_NAME),
                dir.resolve(SignalJoinStore.FILE_NAME),
                dir.resolve(TimerStore.FILE_NAME),
                dir.resolve(SignalListenerStore.FILE_NAME),
                dir.resolve("web_admin_channel_metadata.json"),
                dir.resolve("web_admin_logic_chain_metadata.json")
        );
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        return new Fixture(dir, service, security, editLockService, editor, session, security.csrfTokenFor(session));
    }

    private static WebAdminUser user(WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = role.id().toLowerCase(Locale.ROOT);
        user.displayName = role.displayName();
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
    }

    private static void requireContainsTemplate(List<?> templates, String templateId, String source) {
        for (Object item : templates) {
            if (item instanceof Map<?, ?> map
                    && templateId.equals(string(map.get("templateId")))
                    && source.equals(string(map.get("source")))) {
                return;
            }
        }
        throw new AssertionError("missing template " + source + ":" + templateId);
    }

    private static void requireRecentEventSince(long baselineSeq, String type, String message) {
        for (WebAdminRealtimeEvent event : WebAdminRealtimeEventBus.recentEvents()) {
            if (event.seq() > baselineSeq && type.equals(event.type())) {
                return;
            }
        }
        throw new AssertionError(message + " missing event type=" + type);
    }

    private static void requireValidationCode(WebAdminWriteResult result, String code) {
        for (var error : result.validationErrors()) {
            if (code.equals(error.code())) {
                requireTrue(containsChinese(error.message()), "validation code " + code + " has Chinese message");
                return;
            }
        }
        throw new AssertionError("missing validation code " + code + " errors=" + result.validationErrors() + " message=" + result.message());
    }

    private static void requireDataValidationCode(WebAdminWriteResult result, String code) {
        Object errors = result.data().get("validationErrors");
        if (!String.valueOf(errors).contains(code)) {
            throw new AssertionError("missing data validation code " + code + " errors=" + errors);
        }
    }

    private static String validationMessage(WebAdminWriteResult result) {
        return result.validationErrors().isEmpty() ? result.message() : result.validationErrors().getFirst().message();
    }

    private static boolean containsChinese(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private record Fixture(
            Path dir,
            WebAdminTemplateService service,
            WebAdminWriteSecurityService security,
            WebAdminEditLockService editLockService,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf
    ) {
    }
}
