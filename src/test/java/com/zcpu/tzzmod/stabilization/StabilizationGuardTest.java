package com.zcpu.tzzmod.stabilization;

import com.google.gson.Gson;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.ConditionBasicPlayerContextTest;
import com.zcpu.tzzmod.condition.ConditionEngineCoreTest;
import com.zcpu.tzzmod.condition.ConditionItemInventoryContainerTest;
import com.zcpu.tzzmod.condition.ConditionRegionSignalLogicChainTest;
import com.zcpu.tzzmod.condition.ConditionStateVariableTest;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateServiceTest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateServiceTest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateHistoryServiceTest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateReplayServiceTest;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityServiceTest;
import com.zcpu.tzzmod.resources.ResourceIntegrityTest;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.device.BlockStateConditionMode;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceInteractionHandler;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceMode;
import com.zcpu.tzzmod.signal.device.debug.DeviceDiagnostic;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssueText;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.signal.device.debug.InteractionItemDiagnostic;
import com.zcpu.tzzmod.signal.device.debug.ItemSubmitDiagnostic;
import com.zcpu.tzzmod.signal.device.debug.VirtualBlockDeviceDiagnosticService;
import com.zcpu.tzzmod.signal.device.item.ConsumePlan;
import com.zcpu.tzzmod.signal.device.item.ConsumePlanner;
import com.zcpu.tzzmod.signal.device.item.InteractionDecision;
import com.zcpu.tzzmod.signal.device.item.InteractionDecisionEvaluator;
import com.zcpu.tzzmod.signal.device.item.InteractionItemConsumeSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluationResult;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitEvaluator;
import com.zcpu.tzzmod.signal.device.item.ItemSubmitInventoryAdapter;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendShell;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendScripts;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendStyles;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.dto.WebAdminInteractionItemMatcherUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeClient;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionRelayActionsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionGateConfigTest;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionCatalogTest;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionGroupServiceTest;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionRuntimeDoctorServiceTest;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminInteractionItemMatcherService;
import com.zcpu.tzzmod.webadmin.service.WebAdminChannelMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSelectionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerLifecycleService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceNativeTriggerService;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionPurpose;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteAuditContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteFoundationService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.network.ServerPlayerEntity;

public final class StabilizationGuardTest {
    private StabilizationGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        testLegacyConstructorsNormalizeToSafeDefaults();
        testOldMatcherDefaults();
        testInteractionItemCopyPreservesUnrelatedFields();
        testItemSubmitCopyPreservesUnrelatedFields();
        testMatcherTemplateRefreshPreservesInteractionSettings();
        testCountModeIgnoreSemantics();
        testConsumePlanner();
        testItemSubmitEvaluator();
        testItemSubmitInventoryAdapterSeam();
        testProductionItemSubmitPathUsesEvaluationResult();
        testInteractionDecisionEvaluator();
        testLegacyJsonSamples();
        testDisplayNames();
        testDiagnosticIssueModel();
        testDiagnosticIssueRendering();
        testVirtualDeviceDiagnostics();
        testWebAdminReadonlyFrontendAssets();
        testWebAdminRealtimeFoundation();
        testWebAdminWriteFoundation();
        testWebAdminSelectionFoundation();
        testWebAdminLifecycleFoundation();
        testWebAdminPhysicalDeviceActionRelayFoundation();
        testWebAdminInteractionItemMatcherEditing();
        testWebAdminVbdNativeTriggerOverview();
        testWebAdminRegionControllerEditing();
        testWebAdminSignalListenerEditing();
        testWebAdminEditingStabilization714();
        testWebAdminLogicChainViewer715();
        ConditionEngineCoreTest.run();
        ConditionBasicPlayerContextTest.run();
        ConditionStateVariableTest.run();
        ConditionItemInventoryContainerTest.run();
        ConditionRegionSignalLogicChainTest.run();
        ConditionGroupCompatibilityServiceTest.run();
        ConditionGateServiceTest.run();
        ConditionActionGateServiceTest.run();
        ConditionGateHistoryServiceTest.run();
        ConditionGateReplayServiceTest.run();
        WebAdminConditionGateConfigTest.run();
        WebAdminConditionCatalogTest.run();
        WebAdminConditionGroupServiceTest.run();
        WebAdminConditionRuntimeDoctorServiceTest.run();
        testConditionEngineCore80();
        testConditionBasicPlayerContext81();
        testConditionStateVariables82();
        testConditionItemInventoryContainer83();
        testConditionRegionSignalLogicChain84();
        testWebAdminConditionEditor85();
        testConditionRuntimeGates86();
        testConditionRuntimeReceiverGates87();
        testConditionRuntimeDebugger88();
        testConditionRuntimeSingleActionGates89();
        ResourceIntegrityTest.run();
        System.out.println("Stabilization guard checks passed.");
    }

    private static void testLegacyConstructorsNormalizeToSafeDefaults() {
        SignalDeviceData legacy = new SignalDeviceData(
                "virtual_block_device:minecraft:overworld@1,2,3",
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE,
                "legacy",
                "minecraft:overworld",
                1,
                2,
                3,
                "legacy.channel",
                true,
                0,
                0,
                0,
                0,
                10L,
                20L,
                0L,
                0L,
                "",
                "minecraft:stone",
                "",
                VirtualBlockDeviceMode.REDSTONE_RISING.id(),
                false,
                0
        ).normalized();

        requireFalse(legacy.conditionEnabled(), "legacy condition defaults disabled");
        requireFalse(legacy.interactionEnabled(), "legacy interaction defaults disabled");
        requireFalse(legacy.containerEnabled(), "legacy container defaults disabled");
        requireTrue(legacy.itemConditions().isEmpty(), "legacy itemConditions default empty");
        requireFalse(legacy.interactionItemMatcherEnabled(), "legacy interaction matcher default disabled");
        requireFalse(legacy.interactionItemMatcher().consumeEnabled(), "legacy consume default disabled");
        requireEquals(1, legacy.interactionItemMatcher().consumeCount(), "legacy consumeCount default");
        requireEquals(InteractionItemSource.MAIN_HAND, legacy.interactionItemMatcher().interactionItemSource(), "legacy source default");
        requireEquals(InteractionItemVanillaPolicy.ALLOW, legacy.interactionItemMatcher().interactionItemVanillaPolicy(), "legacy vanilla policy default");
        requireEquals(InteractionItemConsumeSource.MATCHED_SOURCE, legacy.interactionItemMatcher().interactionItemConsumeSource(), "legacy consumeSource default");
        requireEquals(InventoryConsumeOrder.HOTBAR_FIRST, legacy.interactionItemMatcher().interactionItemInventoryConsumeOrder(), "legacy inventory consume order default");
        requireFalse(legacy.itemSubmitEnabled(), "legacy itemSubmit default disabled");
        requireFalse(legacy.itemSubmitConsumeEnabled(), "legacy itemSubmit consume default disabled");
        requireTrue(legacy.itemSubmitRequirements().isEmpty(), "legacy itemSubmit requirements default empty");
    }

    private static void testOldMatcherDefaults() {
        ItemStackMatcherData oldMatcher = new ItemStackMatcherData(
                true,
                "minecraft:diamond",
                3,
                ContainerItemCountMode.AT_LEAST.id(),
                1,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                "minecraft:diamond x3",
                100L,
                200L
        ).normalized();

        requireEquals("", oldMatcher.successChannel(), "old matcher success channel default");
        requireEquals("", oldMatcher.failChannel(), "old matcher fail channel default");
        requireFalse(oldMatcher.consumeEnabled(), "old matcher consume default disabled");
        requireEquals(1, oldMatcher.consumeCount(), "old matcher consume count default");
        requireEquals(InteractionItemSource.MAIN_HAND, oldMatcher.interactionItemSource(), "old matcher source default");
        requireEquals(InteractionItemVanillaPolicy.ALLOW, oldMatcher.interactionItemVanillaPolicy(), "old matcher vanilla policy default");
        requireEquals(InteractionItemConsumeSource.MATCHED_SOURCE, oldMatcher.interactionItemConsumeSource(), "old matcher consume source default");
        requireEquals(InventoryConsumeOrder.HOTBAR_FIRST, oldMatcher.interactionItemInventoryConsumeOrder(), "old matcher consume order default");
    }

    private static void testInteractionItemCopyPreservesUnrelatedFields() throws Exception {
        SignalDeviceData device = fullDevice();
        ItemStackMatcherData replacementMatcher = ItemStackMatcherSupport.withCount(
                device.interactionItemMatcher(),
                ContainerItemCountMode.EXACTLY,
                4
        );

        SignalDeviceData updated = invokeSignalDeviceCopy(
                "withInteractionItemMatcher",
                new Class<?>[]{
                        SignalDeviceData.class,
                        boolean.class,
                        ItemStackMatcherData.class,
                        boolean.class,
                        String.class
                },
                device,
                true,
                replacementMatcher,
                false,
                "matcher updated"
        );

        assertSubmitPreserved(device, updated);
        assertContainerPreserved(device, updated);
        assertItemConditionsPreserved(device, updated);
        assertRedstoneAndConditionPreserved(device, updated);
        requireEquals("success.channel", updated.interactionItemMatcher().successChannel(), "interaction success channel preserved");
        requireEquals("fail.channel", updated.interactionItemMatcher().failChannel(), "interaction fail channel preserved");
        requireEquals(2, updated.interactionItemMatcher().consumeCount(), "interaction consumeCount preserved");
        requireEquals(InteractionItemConsumeSource.INVENTORY, updated.interactionItemMatcher().interactionItemConsumeSource(), "interaction consumeSource preserved");
        requireEquals(InventoryConsumeOrder.MAIN_INVENTORY_FIRST, updated.interactionItemMatcher().interactionItemInventoryConsumeOrder(), "interaction consume order preserved");
    }

    private static void testItemSubmitCopyPreservesUnrelatedFields() throws Exception {
        SignalDeviceData device = fullDevice();
        ItemSubmitRequirementData replacementRequirement = fullRequirement("need_emerald_2", "minecraft:emerald", 2);

        SignalDeviceData updated = invokeSignalDeviceCopy(
                "withItemSubmit",
                new Class<?>[]{
                        SignalDeviceData.class,
                        boolean.class,
                        boolean.class,
                        String.class,
                        List.class,
                        boolean.class,
                        String.class,
                        String.class,
                        String.class
                },
                device,
                true,
                true,
                InventoryConsumeOrder.HOTBAR_FIRST,
                List.of(replacementRequirement),
                false,
                "submit failed",
                "none",
                "submit updated"
        );

        assertInteractionMatcherPreserved(device, updated);
        assertContainerPreserved(device, updated);
        assertItemConditionsPreserved(device, updated);
        assertRedstoneAndConditionPreserved(device, updated);
        requireEquals(1, updated.itemSubmitRequirements().size(), "updated submit requirement count");
        requireEquals("need_emerald_2", updated.itemSubmitRequirements().get(0).name(), "updated submit requirement name");
    }

    private static void testMatcherTemplateRefreshPreservesInteractionSettings() {
        ItemStackMatcherData previous = fullMatcher();
        ItemStackMatcherData newTemplate = new ItemStackMatcherData(
                true,
                "minecraft:emerald",
                8,
                ContainerItemCountMode.EXACTLY.id(),
                8,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                "minecraft:emerald x8",
                300L,
                400L
        ).normalized();

        ItemStackMatcherData refreshed = ItemStackMatcherSupport.withInteractionSettingsFrom(newTemplate, previous);

        requireEquals("minecraft:emerald", refreshed.templateItemId(), "template item refreshed");
        requireEquals(ContainerItemCountMode.EXACTLY.id(), refreshed.countMode(), "template count mode refreshed");
        requireEquals(8, refreshed.requiredCount(), "template required count refreshed");
        requireEquals("success.channel", refreshed.successChannel(), "successChannel preserved on setFromHand path");
        requireEquals("fail.channel", refreshed.failChannel(), "failChannel preserved on setFromHand path");
        requireEquals("success message", refreshed.successMessage(), "successMessage preserved on setFromHand path");
        requireEquals("fail message", refreshed.failMessage(), "failMessage preserved on setFromHand path");
        requireTrue(refreshed.consumeEnabled(), "consumeEnabled preserved on setFromHand path");
        requireEquals(2, refreshed.consumeCount(), "consumeCount preserved on setFromHand path");
        requireEquals(InteractionItemConsumeSource.INVENTORY, refreshed.interactionItemConsumeSource(), "consumeSource preserved on setFromHand path");
        requireEquals(InventoryConsumeOrder.MAIN_INVENTORY_FIRST, refreshed.interactionItemInventoryConsumeOrder(), "inventory order preserved on setFromHand path");
        requireEquals(InteractionItemSource.INVENTORY_CONTAINS, refreshed.interactionItemSource(), "source preserved on setFromHand path");
        requireEquals(InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH, refreshed.interactionItemVanillaPolicy(), "vanilla policy preserved on setFromHand path");
    }

    private static void testCountModeIgnoreSemantics() {
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withCount(fullMatcher(), ContainerItemCountMode.IGNORE, 99);
        requireEquals(ContainerItemCountMode.IGNORE.id(), matcher.countMode(), "ignore count mode stored");
        requireEquals(0, matcher.requiredCount(), "ignore required count is normalized to zero");
        String requirementText = ItemStackMatcherSupport.countRequirementText(matcher);
        requireTrue(requirementText != null && !requirementText.isBlank(), "ignore display is present");
        requireFalse("0".equals(requirementText), "ignore display avoids misleading zero");
        requireTrue(ContainerItemCountMode.IGNORE.matches(0, 99), "raw ignore enum ignores count");
    }

    private static void testConsumePlanner() {
        FakeStack mainHand = new FakeStack("minecraft:diamond", 3);
        ConsumePlan mainPlan = new ConsumePlan();
        requireBlank(ConsumePlanner.stageSingle(mainPlan, "main_hand", mainHand.count(), 2, "main_hand", mainHand::consume), "main hand plan succeeds");
        requireEquals(2, mainPlan.totalCount(), "main hand plan total");
        requireTrue(mainPlan.summary().contains("x2"), "main hand summary shows count");
        mainPlan.apply();
        requireEquals(1, mainHand.count(), "main hand apply consumes configured count");

        FakeStack insufficientMainHand = new FakeStack("minecraft:diamond", 1);
        ConsumePlan insufficientPlan = new ConsumePlan();
        requireNotBlank(ConsumePlanner.stageSingle(insufficientPlan, "main_hand", insufficientMainHand.count(), 2, "main_hand", insufficientMainHand::consume), "main hand insufficient fails");
        insufficientPlan.apply();
        requireEquals(1, insufficientMainHand.count(), "failed main hand plan does not mutate");

        FakeStack offHand = new FakeStack("minecraft:diamond", 3);
        ConsumePlan offPlan = new ConsumePlan();
        requireBlank(ConsumePlanner.stageSingle(offPlan, "off_hand", offHand.count(), 2, "off_hand", offHand::consume), "off hand plan succeeds");
        offPlan.apply();
        requireEquals(1, offHand.count(), "off hand apply consumes configured count");

        FakeStack stackA = new FakeStack("minecraft:diamond", 2);
        FakeStack stackB = new FakeStack("minecraft:diamond", 3);
        ConsumePlan inventoryPlan = new ConsumePlan();
        requireBlank(ConsumePlanner.stageAcrossStacks(inventoryPlan, List.of(
                consumable("inv:0", stackA),
                consumable("inv:1", stackB)
        ), 5), "inventory cross stack plan succeeds");
        inventoryPlan.apply();
        requireEquals(0, stackA.count(), "inventory first stack consumed");
        requireEquals(0, stackB.count(), "inventory second stack consumed");

        FakeStack shortA = new FakeStack("minecraft:diamond", 2);
        FakeStack shortB = new FakeStack("minecraft:diamond", 2);
        ConsumePlan shortPlan = new ConsumePlan();
        requireNotBlank(ConsumePlanner.stageAcrossStacks(shortPlan, List.of(
                consumable("inv:0", shortA),
                consumable("inv:1", shortB)
        ), 5), "inventory insufficient fails");
        shortPlan.apply();
        requireEquals(2, shortA.count(), "failed inventory plan leaves first stack unchanged");
        requireEquals(2, shortB.count(), "failed inventory plan leaves second stack unchanged");

        FakeStack shared = new FakeStack("minecraft:diamond", 4);
        ConsumePlan transaction = new ConsumePlan();
        ConsumePlan staged = transaction.copy();
        requireBlank(ConsumePlanner.stageAcrossStacks(staged, List.of(consumable("inv:0", shared)), 2), "first staged reservation succeeds");
        requireNotBlank(ConsumePlanner.stageAcrossStacks(staged, List.of(consumable("inv:0", shared)), 3), "second staged reservation cannot reuse same stack");
        transaction.apply();
        requireEquals(4, shared.count(), "abandoned duplicate reservation does not mutate");
    }

    private static void testItemSubmitEvaluator() {
        ItemSubmitRequirementData needDiamond3 = submitRequirement("need_diamond_3", "minecraft:diamond", ContainerItemCountMode.AT_LEAST, 3, 3, true);
        ItemSubmitRequirementData needEmerald2 = submitRequirement("need_emerald_2", "minecraft:emerald", ContainerItemCountMode.AT_LEAST, 2, 2, true);

        FakeStack diamonds = new FakeStack("minecraft:diamond", 3);
        FakeStack emeralds = new FakeStack("minecraft:emerald", 2);
        ItemSubmitEvaluationResult noConsume = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3, needEmerald2),
                sourceStacks(diamonds, emeralds),
                false,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireTrue(noConsume.finalSuccess(), "submit succeeds without consume when all requirements match");
        requireTrue(noConsume.stagedConsumePlan().isEmpty(), "submit without consume has empty plan");

        FakeStack oneEmerald = new FakeStack("minecraft:emerald", 1);
        ItemSubmitEvaluationResult notMatched = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3, needEmerald2),
                sourceStacks(new FakeStack("minecraft:diamond", 3), oneEmerald),
                false,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireFalse(notMatched.finalSuccess(), "submit fails when a requirement is not matched");
        requireTrue(notMatched.failureReason().contains("need_emerald_2"), "submit failure points to missing requirement");

        FakeStack consumeDiamonds = new FakeStack("minecraft:diamond", 3);
        FakeStack consumeEmeralds = new FakeStack("minecraft:emerald", 2);
        ItemSubmitEvaluationResult consumeOk = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3, needEmerald2),
                sourceStacks(consumeDiamonds, consumeEmeralds),
                true,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireTrue(consumeOk.finalSuccess(), "submit consume plan succeeds");
        consumeOk.stagedConsumePlan().apply();
        requireEquals(0, consumeDiamonds.count(), "submit consumes diamond requirement count");
        requireEquals(0, consumeEmeralds.count(), "submit consumes emerald requirement count");

        FakeStack failDiamonds = new FakeStack("minecraft:diamond", 3);
        FakeStack failEmeralds = new FakeStack("minecraft:emerald", 1);
        ItemSubmitEvaluationResult consumeFail = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3, needEmerald2),
                sourceStacks(failDiamonds, failEmeralds),
                true,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireFalse(consumeFail.finalSuccess(), "submit consume fails atomically");
        consumeFail.stagedConsumePlan().apply();
        requireEquals(3, failDiamonds.count(), "failed submit does not consume diamonds");
        requireEquals(1, failEmeralds.count(), "failed submit does not consume emeralds");

        ItemSubmitRequirementData disabled = submitRequirement("disabled_emerald", "minecraft:emerald", ContainerItemCountMode.AT_LEAST, 64, 64, false);
        ItemSubmitEvaluationResult disabledIgnored = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3, disabled),
                sourceStacks(new FakeStack("minecraft:diamond", 3)),
                false,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireTrue(disabledIgnored.finalSuccess(), "disabled requirement is ignored");

        ItemSubmitRequirementData ignoreDiamond = submitRequirement("any_diamond", "minecraft:diamond", ContainerItemCountMode.IGNORE, 0, 1, true);
        ItemSubmitEvaluationResult ignorePresent = ItemSubmitEvaluator.evaluate(
                List.of(ignoreDiamond),
                sourceStacks(new FakeStack("minecraft:diamond", 1)),
                false,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireTrue(ignorePresent.finalSuccess(), "ignore succeeds when a matching stack exists");
        ItemSubmitEvaluationResult ignoreMissing = ItemSubmitEvaluator.evaluate(
                List.of(ignoreDiamond),
                sourceStacks(new FakeStack("minecraft:emerald", 1)),
                false,
                new ConsumePlan(),
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireFalse(ignoreMissing.finalSuccess(), "ignore fails when no matching stack exists");

        FakeStack reservedShort = new FakeStack("minecraft:diamond", 4);
        ConsumePlan preReservedShort = new ConsumePlan();
        requireBlank(ConsumePlanner.stageAcrossStacks(preReservedShort, List.of(consumable("inv:0", reservedShort)), 2), "pre-reserve succeeds");
        ItemSubmitEvaluationResult reservedFail = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3),
                sourceStacksWithKeys(reservedShort),
                true,
                preReservedShort,
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireFalse(reservedFail.finalSuccess(), "submit cannot reuse pre-reserved stack when total is short");

        FakeStack reservedEnough = new FakeStack("minecraft:diamond", 5);
        ConsumePlan preReservedEnough = new ConsumePlan();
        requireBlank(ConsumePlanner.stageAcrossStacks(preReservedEnough, List.of(consumable("inv:0", reservedEnough)), 2), "pre-reserve enough succeeds");
        ItemSubmitEvaluationResult reservedOk = ItemSubmitEvaluator.evaluate(
                List.of(needDiamond3),
                sourceStacksWithKeys(reservedEnough),
                true,
                preReservedEnough,
                StabilizationGuardTest::sourceStackMatchesItemId
        );
        requireTrue(reservedOk.finalSuccess(), "submit succeeds when pre-reserved and submit counts fit");
        reservedOk.stagedConsumePlan().apply();
        requireEquals(0, reservedEnough.count(), "combined plan consumes existing and submit reservations");
    }

    private static void testItemSubmitInventoryAdapterSeam() {
        ItemSubmitInventoryAdapter.View emptyView = ItemSubmitInventoryAdapter.fromMainStacks(List.of(), InventoryConsumeOrder.HOTBAR_FIRST);
        requireTrue(emptyView.sourceStacks().isEmpty(), "empty adapter view exposes no source stacks");
        ItemSubmitEvaluator.SourceStack synthetic = new ItemSubmitEvaluator.SourceStack(
                "inv:0",
                "minecraft:diamond",
                1,
                "submit:slot0",
                amount -> {
                }
        );
        requireFalse(emptyView.matcher().matches(synthetic, fullMatcher()), "empty adapter matcher does not invent matches");
    }

    private static void testProductionItemSubmitPathUsesEvaluationResult() throws Exception {
        Method method = VirtualBlockDeviceInteractionHandler.class.getDeclaredMethod(
                "evaluateItemSubmit",
                ServerPlayerEntity.class,
                SignalDeviceData.class,
                boolean.class,
                ConsumePlan.class
        );
        requireEquals(ItemSubmitEvaluationResult.class, method.getReturnType(), "production itemSubmit path returns evaluator result");
    }

    private static void testInteractionDecisionEvaluator() {
        InteractionDecision allowFailure = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.ALLOW,
                false,
                false,
                false,
                true,
                "failed"
        );
        requireTrue(allowFailure.allowVanillaInteraction(), "allow policy never locks vanilla on failure");

        InteractionDecision requireFailure = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                false,
                false,
                false,
                true,
                "failed"
        );
        requireFalse(requireFailure.allowVanillaInteraction(), "require_item_match locks vanilla on failure");

        InteractionDecision requireFailureCooldown = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                false,
                true,
                false,
                true,
                "failed"
        );
        requireFalse(requireFailureCooldown.allowVanillaInteraction(), "cooldown does not unlock failure");
        requireFalse(requireFailureCooldown.executeSignal(), "cooldown suppresses signal");
        requireFalse(requireFailureCooldown.executeMessage(), "cooldown suppresses message");
        requireFalse(requireFailureCooldown.executeSound(), "cooldown suppresses sound");

        InteractionDecision successCooldownConsume = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                true,
                true,
                true,
                true,
                ""
        );
        requireTrue(successCooldownConsume.allowVanillaInteraction(), "success allows vanilla in cooldown");
        requireTrue(successCooldownConsume.executeConsume(), "cooldown does not suppress consume");
        requireFalse(successCooldownConsume.executeSignal(), "cooldown suppresses success signal");

        InteractionDecision successNoCooldown = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                true,
                false,
                true,
                true,
                ""
        );
        requireTrue(successNoCooldown.allowVanillaInteraction(), "success allows vanilla");
        requireTrue(successNoCooldown.executeConsume(), "success executes consume");
        requireTrue(successNoCooldown.executeSignal(), "no cooldown executes signal");

        InteractionDecision consumeFailed = InteractionDecisionEvaluator.evaluate(
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                true,
                false,
                true,
                false,
                ""
        );
        requireFalse(consumeFailed.allowVanillaInteraction(), "consume plan failure locks vanilla");
        requireFalse(consumeFailed.executeConsume(), "consume plan failure does not consume");
    }

    private static void testLegacyJsonSamples() {
        Gson gson = new Gson();
        for (String sample : List.of(
                "signal_device_legacy_5_4.json",
                "signal_device_legacy_5_5.json",
                "signal_device_legacy_5_8.json",
                "signal_device_legacy_5_10.json",
                "signal_device_legacy_5_12.json",
                "signal_device_legacy_5_14.json"
        )) {
            SignalDeviceData data = readSample(gson, sample).normalized();
            requireEquals(InteractionItemSource.MAIN_HAND, data.interactionItemMatcher().interactionItemSource(), sample + " source default");
            requireEquals(InteractionItemVanillaPolicy.ALLOW, data.interactionItemMatcher().interactionItemVanillaPolicy(), sample + " vanilla policy default");
            requireFalse(data.interactionItemMatcher().consumeEnabled(), sample + " consume default disabled");
            requireEquals(1, data.interactionItemMatcher().consumeCount(), sample + " consume count default");
            requireEquals(InteractionItemConsumeSource.MATCHED_SOURCE, data.interactionItemMatcher().interactionItemConsumeSource(), sample + " consume source default");
            requireEquals(InventoryConsumeOrder.HOTBAR_FIRST, data.interactionItemMatcher().interactionItemInventoryConsumeOrder(), sample + " inventory order default");
            requireFalse(data.itemSubmitEnabled(), sample + " itemSubmit default disabled");
            requireTrue(data.itemSubmitRequirements().isEmpty(), sample + " itemSubmit requirements default empty");
            requireTrue(data.itemConditions().isEmpty(), sample + " itemConditions default empty");
            if (!sample.equals("signal_device_legacy_5_8.json")
                    && !sample.equals("signal_device_legacy_5_10.json")
                    && !sample.equals("signal_device_legacy_5_12.json")
                    && !sample.equals("signal_device_legacy_5_14.json")) {
                requireFalse(data.containerEnabled(), sample + " container default disabled");
            }
            if (!sample.equals("signal_device_legacy_5_10.json")
                    && !sample.equals("signal_device_legacy_5_12.json")
                    && !sample.equals("signal_device_legacy_5_14.json")) {
                requireFalse(data.interactionEnabled(), sample + " interaction default disabled");
            }
        }
    }

    private static void testDisplayNames() {
        requireContains(InteractionItemSource.displayName(InteractionItemSource.MAIN_HAND), "主手", "main_hand display name");
        requireContains(InteractionItemSource.displayName(InteractionItemSource.OFF_HAND), "副手", "off_hand display name");
        requireContains(InteractionItemSource.displayName(InteractionItemSource.INVENTORY_CONTAINS), "背包/热键栏", "inventory_contains display name");
        requireContains(InteractionItemSource.displayName(InteractionItemSource.ARMOR_HEAD), "头盔槽", "armor_head display name");
        requireContains(InteractionItemSource.displayName(InteractionItemSource.ARMOR_ANY), "任意盔甲槽", "armor_any display name");
        requireContains(InteractionItemConsumeSource.displayName(InteractionItemConsumeSource.MATCHED_SOURCE), "匹配来源", "matched_source display name");
        requireContains(InventoryConsumeOrder.displayName(InventoryConsumeOrder.HOTBAR_FIRST), "优先热键栏", "hotbar_first display name");
        requireContains(InteractionItemVanillaPolicy.displayName(InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH), "需要物品匹配", "require_item_match display name");
        requireContains(ContainerItemCountMode.displayName(ContainerItemCountMode.IGNORE.id()), "不检查数量", "ignore display name");
        requireContains(BlockStateConditionMode.displayName(BlockStateConditionMode.CONDITION_BOTH.id()), "进入和退出", "condition_both display name");
        requireContains(VirtualBlockDeviceMode.displayName(VirtualBlockDeviceMode.REDSTONE_BOTH.id()), "通电和断电", "redstone_both display name");
    }

    private static void testDiagnosticIssueModel() {
        DiagnosticIssue error = DiagnosticIssue.of(
                DiagnosticSeverity.ERROR,
                "test_error",
                "测试错误",
                "测试说明",
                "测试建议"
        );
        DiagnosticIssue warning = DiagnosticIssue.of(
                DiagnosticSeverity.WARNING,
                "test_warning",
                "测试警告",
                "测试说明",
                "测试建议"
        );
        DiagnosticIssue info = DiagnosticIssue.of(
                DiagnosticSeverity.INFO,
                "test_info",
                "测试信息",
                "测试说明",
                "测试建议"
        );
        List<DiagnosticIssue> issues = List.of(error, warning, info);
        requireEquals(1L, issues.stream().filter(issue -> issue.severity() == DiagnosticSeverity.ERROR).count(), "diagnostic error count");
        requireEquals(1L, issues.stream().filter(issue -> issue.severity() == DiagnosticSeverity.WARNING).count(), "diagnostic warning count");
        requireEquals(1L, issues.stream().filter(issue -> issue.severity() == DiagnosticSeverity.INFO).count(), "diagnostic info count");
        for (DiagnosticIssue issue : issues) {
            requireNotBlank(issue.code(), "diagnostic code present");
            requireNotBlank(issue.title(), "diagnostic title present");
            requireNotBlank(issue.message(), "diagnostic message present");
            requireNotBlank(issue.suggestion(), "diagnostic suggestion present");
        }
    }

    private static void testDiagnosticIssueRendering() {
        DiagnosticIssue issue = DiagnosticIssue.of(
                DiagnosticSeverity.WARNING,
                "item_submit_requirement_not_met",
                "提交条件未满足",
                "条件：need_emerald_2\n当前匹配数量：1\n要求数量：至少 2",
                "检查玩家背包/热键栏中的物品、matcher 模板或 countMode。"
        );
        List<String> detailLines = DiagnosticIssueText.detailLines(issue);
        String rendered = DiagnosticIssueText.headline(issue) + "\n" + String.join("\n", detailLines);
        requireFalse(rendered.startsWith("code="), "diagnostic rendering does not start with code=");
        requireFalse(rendered.contains("code="), "diagnostic rendering does not expose code= prefix");
        requireContains(rendered, "[警告]", "diagnostic rendering includes bracketed Chinese severity");
        requireContains(rendered, "提交条件未满足", "diagnostic rendering includes Chinese title");
        requireContains(rendered, "物品匹配模板", "diagnostic rendering localizes matcher");
        requireContains(rendered, "数量模式", "diagnostic rendering localizes countMode");
        requireContains(rendered, "代码：item_submit_requirement_not_met", "diagnostic rendering keeps code as auxiliary detail");
        requireTrue(detailLines.stream().anyMatch(line -> line.startsWith("条件：")), "diagnostic rendering keeps condition as its own line");
        requireTrue(detailLines.stream().anyMatch(line -> line.startsWith("当前：")), "diagnostic rendering keeps current value as its own line");
        requireTrue(detailLines.stream().anyMatch(line -> line.startsWith("要求：")), "diagnostic rendering keeps requirement as its own line");
        requireTrue(detailLines.stream().anyMatch(line -> line.startsWith("建议：")), "diagnostic rendering keeps suggestion as its own line");
    }

    private static void testVirtualDeviceDiagnostics() throws Exception {
        SignalDeviceData itemSubmitNoRequirement = invokeSignalDeviceCopy(
                "withItemSubmit",
                new Class<?>[]{
                        SignalDeviceData.class,
                        boolean.class,
                        boolean.class,
                        String.class,
                        List.class,
                        boolean.class,
                        String.class,
                        String.class,
                        String.class
                },
                fullDevice(),
                true,
                true,
                InventoryConsumeOrder.HOTBAR_FIRST,
                List.of(submitRequirement("disabled_need_diamond", "minecraft:diamond", ContainerItemCountMode.AT_LEAST, 1, 1, false)),
                false,
                "no enabled requirements",
                "",
                "result"
        );

        DeviceDiagnostic diagnostic = VirtualBlockDeviceDiagnosticService.diagnoseStatic(itemSubmitNoRequirement);
        requireTrue(hasDiagnosticCode(diagnostic, "item_submit_no_enabled_requirements"), "itemSubmit without enabled requirements produces error");

        InteractionItemDiagnostic interactionDiagnostic = VirtualBlockDeviceDiagnosticService.interactionItemDiagnostic(fullDevice());
        requireTrue(interactionDiagnostic.consumeEnabled(), "interaction diagnostic reads consume enabled");
        requireTrue(interactionDiagnostic.sourceSupportsConsume(), "interaction diagnostic returns source support boolean");

        ItemSubmitDiagnostic itemSubmitDiagnostic = VirtualBlockDeviceDiagnosticService.itemSubmitDiagnostic(itemSubmitNoRequirement);
        requireTrue(itemSubmitDiagnostic.enabled(), "itemSubmit diagnostic sees enabled mode");
        requireEquals(0, itemSubmitDiagnostic.enabledRequirementCount(), "itemSubmit diagnostic counts enabled requirements");

        SignalDeviceData unsupportedConsume = invokeSignalDeviceCopy(
                "withInteractionItemMatcher",
                new Class<?>[]{
                        SignalDeviceData.class,
                        boolean.class,
                        ItemStackMatcherData.class,
                        boolean.class,
                        String.class
                },
                fullDevice(),
                true,
                ItemStackMatcherSupport.withSource(fullMatcher(), InteractionItemSource.ARMOR_HEAD),
                false,
                "armor consume unsupported"
        );
        DeviceDiagnostic consumeDiagnostic = VirtualBlockDeviceDiagnosticService.diagnoseStatic(unsupportedConsume);
        requireTrue(hasDiagnosticCode(consumeDiagnostic, "consume_source_unsupported"), "armor source consume incompatibility is diagnosed");
    }

    private static void testWebAdminReadonlyFrontendAssets() throws Exception {
        String loginHtml = WebAdminFrontendAssets.loginHtml();
        String appHtml = WebAdminFrontendAssets.appHtml();
        String css = WebAdminFrontendAssets.appCss();
        String js = WebAdminFrontendAssets.appJs();
        String shellLoginHtml = WebAdminFrontendShell.loginHtml();
        String shellAppHtml = WebAdminFrontendShell.appHtml();
        String stylesCss = WebAdminFrontendStyles.appCss();
        String scriptsJs = WebAdminFrontendScripts.appJs();

        requireNotBlank(loginHtml, "WebAdmin login HTML asset");
        requireNotBlank(appHtml, "WebAdmin app HTML asset");
        requireNotBlank(css, "WebAdmin CSS asset");
        requireNotBlank(js, "WebAdmin JS asset");
        requireNotBlank(shellLoginHtml, "WebAdmin frontend shell login HTML asset");
        requireNotBlank(shellAppHtml, "WebAdmin frontend shell app HTML asset");
        requireNotBlank(stylesCss, "WebAdmin frontend styles CSS asset");
        requireNotBlank(scriptsJs, "WebAdmin frontend scripts JS asset");
        requireEquals(shellLoginHtml, loginHtml, "WebAdminFrontendAssets delegates login HTML to shell");
        requireEquals(shellAppHtml, appHtml, "WebAdminFrontendAssets delegates app HTML to shell");
        requireEquals(stylesCss, css, "WebAdminFrontendAssets delegates CSS to styles");
        requireEquals(scriptsJs, js, "WebAdminFrontendAssets delegates JS to scripts");
        requireContains(appHtml, "id=\"app-view\"", "WebAdmin app root container remains present");
        requireContains(appHtml, "class=\"sidebar\"", "WebAdmin sidebar shell remains present");
        requireContains(appHtml, "class=\"topbar\"", "WebAdmin topbar shell remains present");
        requireContains(loginHtml, "rel=\"icon\" href=\"data:,\"", "WebAdmin login shell suppresses favicon 404");
        requireContains(appHtml, "rel=\"icon\" href=\"data:,\"", "WebAdmin app shell suppresses favicon 404");

        for (String route : List.of(
                "#/dashboard",
                "#/devices",
                "#/virtual-block-devices",
                "#/block-devices",
                "#/signals",
                "#/signalbridge",
                "#/receivers",
                "#/listeners",
                "#/signal-listeners",
                "#/doctor",
                "#/diagnostics",
                "#/signal-doctor",
                "#/history",
                "#/events",
                "#/users",
                "#/permissions",
                "#/users-permissions",
                "#/settings",
                "#/system-settings",
                "#/config",
                "#/config-management",
                "#/settings/config",
                "#/regions",
                "#/region-list",
                "#/region-controllers",
                "#/regionctl",
                "#/actions",
                "#/action-templates",
                "#/templates"
        )) {
            requireContains(appHtml + js, route, "WebAdmin readonly route present: " + route);
            requireContains(js, route, "WebAdmin JS route present: " + route);
        }

        for (String cssClass : List.of(
                ".sidebar",
                ".topbar",
                ".panel-card",
                ".pill",
                ".toast",
                ".validation-list",
                ".readonly-note",
                ".channel-combo",
                ".select option"
        )) {
            requireContains(css, cssClass, "WebAdmin CSS class remains present: " + cssClass);
        }
        requireContains(css, "color-scheme:dark", "WebAdmin form controls remain in dark color scheme");
        requireContains(css, "select option{background:#081725;color:var(--text)}", "WebAdmin native select options keep dark styling");
        requireContains(css, ".channel-combo-menu", "custom channel combobox menu style remains present");

        for (String editEntry : List.of(
                "WebAdmin 显示信息",
                "设备基础配置",
                "设备扩展配置",
                "频道显示信息",
                "虚拟监听器基础配置"
        )) {
            requireContains(js, editEntry, "WebAdmin edit entry remains present: " + editEntry);
        }

        for (String helper : List.of(
                "formatDateTime",
                "formatRelativeTime",
                "withReturnContext",
                "goBackOrFallback",
                "backButton",
                "navigationButton",
                "connectRealtime",
                "closeRealtime",
                "handleRealtimeEvent",
                "shouldHandleRealtimeEvent",
                "markRealtimeDirty",
                "flushVisibleRealtimeRefresh",
                "runRealtimeRefresh",
                "captureViewState",
                "restoreViewState"
        )) {
            requireContains(js, helper, "WebAdmin frontend helper present: " + helper);
        }



        for (String routeSmoke : List.of(
                "renderActionTemplatesPage",
                "renderDoctorPage",
                "renderSignalDetail",
                "renderSignalListenerDetail",
                "renderDeviceDetail",
                "renderActionDetail",
                "renderRegionDetail",
                "loadSignalListenerDetail",
                "actionTemplates",
                "listenerDetail:",
                "listenerEventRef",
                "signalDetail:"
        )) {
            requireContains(js, routeSmoke, "WebAdmin 7.5 route/render smoke marker present: " + routeSmoke);
        }

        for (String disabledBoundary : List.of(
                "添加模板",
                "使用模板",
                "导入模板",
                "自动修复",
                "清空问题",
                "编辑监听器",
                "删除监听器"
        )) {
            requireContains(js, disabledBoundary, "WebAdmin 7.5 disabled operation marker present: " + disabledBoundary);
        }

        for (String realtimeMarker : List.of(
                "actionTemplates",
                "listenerDetail:",
                "doctor_issues_changed",
                "signal_listener_changed",
                "signal_history_appended",
                "sync_required"
        )) {
            requireContains(js, realtimeMarker, "WebAdmin 7.5 realtime mapping marker present: " + realtimeMarker);
        }

        for (String saveHelper : List.of(
                "function route",
                "handleRealtimeEvent",
                "saveDeviceMetadata",
                "saveDeviceBasicConfig",
                "saveDeviceExtendedConfig",
                "saveChannelMetadata",
                "saveSignalListenerBasicConfig"
        )) {
            requireContains(js, saveHelper, "WebAdmin JS remains wired for: " + saveHelper);
        }

        for (String modalMarker : List.of(
                "showDeviceMetadataEditModal",
                "showDeviceBasicConfigEditModal",
                "showDeviceExtendedConfigEditModal",
                "showDeviceConfigEditModal",
                "showChannelMetadataEditModal",
                "showSignalListenerBasicConfigEditModal",
                "wa-modal-backdrop",
                "wa-modal-viewport",
                "wa-modal-body",
                "wa-modal-foot",
                "modalDirtyChecker",
                "modalSyncBeforeClose",
                "modalDraftDirty",
                "markModalInitialSnapshot",
                "syncModalDraftBeforeClose",
                "data-discard-confirm-modal",
                "cancelDiscardModalClose",
                "confirmDiscardModalClose",
                "closeWebAdminModal(true,true)"
        )) {
            requireContains(js + css, modalMarker, "WebAdmin 7.5 modal marker present: " + modalMarker);
        }
        for (String selectionMarker : List.of(
                "/api/webadmin/selection/start",
                "/api/webadmin/selection/cancel",
                "openCreateVirtualBlockDeviceModal",
                "startCreateVirtualBlockDeviceSelection",
                "/api/webadmin/online-players",
                "data-selection-player-combo=\"true\"",
                "data-selection-channel-combo=\"true\"",
                "handleSelectionTargetPlayerKey",
                "handleSelectionChannelKey",
                "selectionDeviceDetailRoute",
                "selectionTerminalById",
                "data-selection-wizard=\"virtual_block_device\"",
                "等待玩家在游戏内右键方块",
                "新建虚拟方块设备",
                "selection_started",
                "selection_completed",
                "selection_cancelled",
                "selection_failed"
        )) {
            requireContains(js, selectionMarker, "WebAdmin 7.6 selection UI marker present: " + selectionMarker);
        }
        for (String lifecycleMarker : List.of(
                "/api/webadmin/virtual-block-devices/",
                "/api/webadmin/signal-listeners",
                "openVirtualBlockDeviceDeleteModal",
                "openSignalListenerCreateModal",
                "openSignalListenerDeleteModal",
                "data-vbd-delete-modal=\"true\"",
                "data-listener-create-modal=\"true\"",
                "data-listener-delete-modal=\"true\"",
                "data-listener-create-channel-combo=\"true\"",
                "data-danger-confirm-modal=\"true\"",
                "不会创建 matcher、itemSubmit 或 ConditionEngine",
                "不 setblock、不破坏世界方块"
        )) {
            requireContains(js, lifecycleMarker, "WebAdmin 7.6 lifecycle UI marker present: " + lifecycleMarker);
        }
        for (String selectionStyle : List.of(
                ".wa-selection-modal",
                ".wa-selection-grid",
                ".wa-selection-status",
                ".wa-selection-status.ok",
                ".wa-selection-status.error"
        )) {
            requireContains(css, selectionStyle, "WebAdmin 7.6 selection modal style present: " + selectionStyle);
        }
        for (String modalStyle : List.of(
                "waModalIn",
                "waModalOut",
                "waModalBackdropIn",
                "waModalBackdropOut",
                "backdrop-filter:blur",
                ".wa-modal-body .edit-form .form-actions{display:none}",
                "@media(max-width:900px)",
                "wa-tabs-scroll",
                "detail-fixed-layout",
                "detail-full-width-stack",
                "pointer-events:none",
                "overflow-x:auto",
                "calc(100vh - 20px)",
                ".wa-config-modal",
                ".wa-edit-section",
                ".wa-discard-confirm-layer",
                ".wa-discard-confirm-dialog",
                ".wa-flow-chain",
                ".readonly-note.danger",
                ".switch-row"
        )) {
            requireContains(css, modalStyle, "WebAdmin 7.5 modal interaction style present: " + modalStyle);
        }
        requireContains(js, "modalClosing", "modal close lifecycle marks closing DOM before removal");
        requireContains(js, "animationend", "modal close lifecycle waits for fade-out animation end");
        requireContains(js, "data-table-row-stretch", "detail tables carry row stretch guard marker");
        requireFalse(css.contains("grid-auto-flow:dense"), "detail layout must not use masonry/dense packing");
        requireFalse(js.contains("data-detail-adaptive-grid=\"true\""), "detail render must not use adaptive free layout marker");
        requireFalse(js.contains("wa-detail-adaptive-grid") || css.contains("wa-detail-adaptive-grid"), "detail render/style must not use adaptive grid class");

        requireContains(appHtml, "区域管理", "sidebar contains Region navigation");
        requireContains(appHtml, "动作系统", "sidebar contains Action navigation");
        requireContains(appHtml, "动作模板", "sidebar contains Action Template navigation");
        requireContains(appHtml + js, "/api/realtime/events", "WebAdmin realtime event stream route present");
        requireContains(js, "dirtyRoutes", "realtime hidden-tab dirty route tracking present");
        requireContains(js, "pendingRefresh", "realtime pending refresh guard present");
        requireContains(js, "route({silent:true,expectedHash:hash,expectedSeq:seq})", "realtime refresh uses silent route update");
        requireContains(js, "function routePollInterval(hash){", "route-level polling hook is present");
        requireContains(js, "return 0;", "route-level polling interval remains disabled");
        requireContains(js, "getFullYear()", "time formatter uses local Date fields");
        requireFalse(js.contains("text.slice(0,10)") || js.contains("text.slice(11,19)"),
                "time formatter does not display UTC ISO strings by substring slicing");
        requireFalse(js.contains("toISOString()"), "frontend does not expose ISO UTC strings as visible time");
        requireContains(js, "暂无", "Chinese empty state fallback present");
        requireContains(js, "只读", "readonly UI hint present");
        requireFalse(js.contains("'code=") || js.contains("\"code=") || js.contains(">code="),
                "diagnostic code is not rendered as a visible code= prefix");
        requireFalse(js.contains(">undefined<"), "frontend does not render raw undefined marker");
        requireFalse(js.contains(">null<"), "frontend does not render raw null marker");
        requireFalse(js.contains("location.hash='http"), "frontend does not route to external URL");
        requireFalse(js.contains("setInterval("), "frontend does not use global polling interval");
        requireFalse(js.contains("location.reload"), "frontend does not force full page reloads during realtime refresh");
        requireFalse(js.contains("history.go(0)"), "frontend does not use browser history reload as refresh fallback");
        requireFalse(js.contains("location.href=location.href") || js.contains("location.href = location.href"),
                "frontend does not self-assign location.href as a reload fallback");
        runWebAdminRenderRouteSmoke(js);
    }

    private static void runWebAdminRenderRouteSmoke(String js) throws Exception {
        String node = findNodeExecutable();
        Path smokeFile = Files.createTempFile("webadmin-route-smoke-", ".js");
        String encodedAppJs = Base64.getEncoder().encodeToString(js.getBytes(StandardCharsets.UTF_8));
        Files.writeString(smokeFile, webAdminRenderSmokeHarness(encodedAppJs), StandardCharsets.UTF_8);
        Process process = new ProcessBuilder(node, smokeFile.toString())
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("WebAdmin render/route smoke timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        requireEquals(0, process.exitValue(), "WebAdmin render/route smoke passed. Output:\n" + output);
    }

    private static String findNodeExecutable() {
        List<String> candidates = new ArrayList<>();
        addNodeCandidate(candidates, System.getProperty("node.path"));
        addNodeCandidate(candidates, System.getenv("CODEX_NODE"));
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        candidates.add(windows ? "node.exe" : "node");
        Path home = Path.of(System.getProperty("user.home", ""));
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/" + (windows ? "node.exe" : "node")).toString());
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node").toString());
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe").toString());
        for (String candidate : candidates) {
            if (isRunnableNode(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("Node.js is required for WebAdmin render/route smoke test but no runnable node executable was found");
    }

    private static void addNodeCandidate(List<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value);
        }
    }

    private static boolean isRunnableNode(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(candidate, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static String webAdminRenderSmokeHarness(String encodedAppJs) {
        String harness = """
                const vm = require('vm');
                const code = Buffer.from('__APP_JS_BASE64__', 'base64').toString('utf8');
                const elements = new Map();
                function el(id='') {
                  if (elements.has(id)) return elements.get(id);
                  const node = {
                    id,
                    dataset: {},
                    className: '',
                    classList: {
                      add(...names){ node.className = Array.from(new Set(`${node.className || ''} ${names.join(' ')}`.trim().split(/\\s+/).filter(Boolean))).join(' '); },
                      remove(...names){ const remove = new Set(names); node.className = `${node.className || ''}`.split(/\\s+/).filter(Boolean).filter(n => !remove.has(n)).join(' '); },
                      toggle(name){ if (` ${node.className || ''} `.includes(` ${name} `)) this.remove(name); else this.add(name); },
                    },
                    style: {},
                    innerHTML: '',
                    textContent: '',
                    hidden: true,
                    value: '',
                    checked: false,
                    type: 'text',
                    scrollTop: 0,
                    scrollLeft: 0,
                    querySelectorAll(){ return []; },
                    querySelector(){ return null; },
                    setAttribute(name,value){ node[String(name)] = String(value); },
                    getAttribute(name){ return node[String(name)] || null; },
                    addEventListener(){},
                    removeEventListener(){},
                    appendChild(child){ if (child && child.id) elements.set(child.id, child); return child; },
                    remove(){ if (node.id) elements.delete(node.id); },
                    focus(){},
                    setSelectionRange(){},
                  };
                  elements.set(id, node);
                  return node;
                }
                const body = el('body');
                body.dataset.page = 'none';
                const view = el('app-view');
                elements.set('view', view);
                elements.set('app-view', view);
                elements.set('toast', el('toast'));
                const errors = [];
                const requestedUrls = [];
                const requestedRequests = [];
                let failConditionDebuggerDetail = false;
                function apiData(path, options={}) {
                  const url = String(path);
                  const method = String(options.method || 'GET').toUpperCase();
                  if (url.startsWith('/api/webadmin/edit-locks/acquire')) return { success:true, data:{ lock:{ lockId:'lock-1', locked:true, heldByCurrentUser:true, holderUsername:'Owner', expiresAt:'2026-05-09T10:10:00Z' } } };
                  if (url.startsWith('/api/webadmin/edit-locks/release') || url.startsWith('/api/webadmin/edit-locks/heartbeat')) return { success:true, data:{ lock:{ lockId:'lock-1', locked:true, heldByCurrentUser:true, expiresAt:'2026-05-09T10:10:00Z' } } };
                  if (url === '/api/webadmin/condition-types') return { readOnly:true, count:7, types:[
                    { type:'always_true', displayName:'永远通过', description:'总是通过。', category:'调试条件', suite:'core', fields:[] },
                    { type:'context_equals', displayName:'上下文字段匹配', description:'检查上下文字段。', category:'上下文条件', suite:'core', fields:[{ key:'field', displayName:'上下文字段', kind:'string', required:true }, { key:'expected', displayName:'期望值', kind:'string', required:true }] },
                    { type:'state_variable_bool_equals', displayName:'布尔状态匹配', description:'检查布尔状态。', category:'状态变量条件', suite:'state-variable', fields:[{ key:'scope', displayName:'作用域', kind:'enum:GLOBAL,PLAYER', required:true, options:['GLOBAL','PLAYER'] }, { key:'key', displayName:'变量键', kind:'string', required:true }, { key:'targetMode', displayName:'目标模式', kind:'enum:global,context_player,explicit_target', required:true, options:['global','context_player','explicit_target'] }, { key:'expected', displayName:'期望值', kind:'boolean', required:true }] },
                    { type:'inventory_contains_item', displayName:'背包包含物品', description:'检查背包快照。', category:'物品条件', suite:'item-inventory-container', fields:[{ key:'inventoryKey', displayName:'背包快照键', kind:'string', required:true }, { key:'itemId', displayName:'物品 ID', kind:'item-id', required:true }, { key:'countOperator', displayName:'数量比较方式', kind:'enum:eq,ne,gt,gte,lt,lte', required:true }, { key:'count', displayName:'目标数量', kind:'integer', required:true }] },
                    { type:'container_slot_item_matches', displayName:'容器槽位物品匹配', description:'检查容器槽位。', category:'物品条件', suite:'item-inventory-container', fields:[{ key:'containerKey', displayName:'容器快照键', kind:'string', required:true }, { key:'slot', displayName:'槽位', kind:'integer', required:true }, { key:'itemId', displayName:'物品 ID', kind:'item-id', required:true }] },
                    { type:'region_enabled', displayName:'区域已启用', description:'检查区域启用。', category:'区域条件', suite:'region-signal-logic-chain', fields:[{ key:'regionKey', displayName:'区域快照键', kind:'string', required:true }] },
                    { type:'signal_event_count_compare', displayName:'信号事件数量比较', description:'检查信号历史事件数。', category:'信号条件', suite:'region-signal-logic-chain', fields:[{ key:'signalHistoryKey', displayName:'信号历史快照键', kind:'string', required:true }, { key:'operator', displayName:'比较方式', kind:'enum:eq,ne,gt,gte,lt,lte', required:true }, { key:'count', displayName:'目标数量', kind:'integer', required:true }] },
                    { type:'logic_chain_has_cycle', displayName:'逻辑链存在循环', description:'检查逻辑链循环标记。', category:'逻辑链条件', suite:'region-signal-logic-chain', fields:[{ key:'logicChainKey', displayName:'逻辑链快照键', kind:'string', required:true }, { key:'expected', displayName:'期望循环状态', kind:'boolean', required:false }] }
                  ] };
                  if (url === '/api/webadmin/condition-gates/history/smoke-record/replay') return { success:true, recordId:'smoke-record', readOnly:true, noSideEffects:true, noLiveWorldRead:true, originalResult:'BLOCKED', replayResult:'BLOCKED', resultConsistent:true, failureReason:'always_false 阻断', warnings:[], debugTree:{ matched:false, label:'Root 条件组', type:'group', nodeId:'root', reasonCode:'always_false', failureReason:'always_false 阻断', debugSummary:'blocked', childResults:[{ matched:false, label:'永远不通过', type:'always_false', nodeId:'false-node', reasonCode:'always_false', failureReason:'always_false 阻断', debugSummary:'blocked', childResults:[] }] } };
                  if (url === '/api/webadmin/condition-gates/history/smoke-record') { if (failConditionDebuggerDetail) throw new Error('transient condition debugger detail failure'); return { id:'smoke-record', occurredAt:'2026-05-09T10:00:00Z', targetType:'SIGNAL_LISTENER', targetId:'test-listener', channel:'test.channel', conditionGroupId:'smoke.roundtrip', conditionGroupDisplayName:'Smoke 条件组', result:'BLOCKED', allowed:false, code:'CONDITION_FALSE', failureReason:'always_false 阻断', debugSummary:'Root 条件组未通过', evaluatedCount:2, durationNanos:123000, conditionFingerprint:'condition-fp', definitionFingerprint:'definition-fp', replayable:true, replayReadOnly:true, noActionExecution:true, noSignalEmit:true, noRawJsonEditor:true, contextSummary:{ player:'Owner', sourceType:'SIGNAL_LISTENER', channel:'test.channel', world:'minecraft:overworld', listenerId:'test-listener', itemSnapshotCount:0, inventorySnapshotCount:1, containerSnapshotCount:0, stateVariableCount:2 }, debugTree:{ matched:false, label:'Root 条件组', type:'group', nodeId:'root', reasonCode:'always_false', failureReason:'always_false 阻断', debugSummary:'blocked', childResults:[{ matched:false, label:'永远不通过', type:'always_false', nodeId:'false-node', reasonCode:'always_false', failureReason:'always_false 阻断', debugSummary:'blocked', childResults:[] }] } }; }
                  if (url === '/api/webadmin/condition-gates/history/missing-record') return null;
                  if (url.startsWith('/api/webadmin/condition-gates/history')) return { maxRecords:200, records:[{ id:'smoke-record', occurredAt:'2026-05-09T10:00:00Z', targetType:'SIGNAL_LISTENER', targetId:'test-listener', channel:'test.channel', conditionGroupId:'smoke.roundtrip', conditionGroupDisplayName:'Smoke 条件组', result:'BLOCKED', failureReason:'always_false 阻断', debugSummary:'Root 条件组未通过', evaluatedCount:2, durationNanos:123000, replayable:true, debuggerRoute:'#/condition-debugger/smoke-record' }] };
                  if (url === '/api/webadmin/condition-groups' && method === 'POST') return { success:true, message:'条件组已保存。', data:{ group:{ id:'smoke.request' }, routeTarget:'#/condition-groups/smoke.request' } };
                  if (url === '/api/webadmin/condition-groups') return { count:1, groups:[{ id:'smoke.roundtrip', displayName:'Smoke 条件组', note:'', iconKey:'doctor-overview', enabled:true, nodeCount:2, fingerprint:'condition-fp', updatedAt:'2026-05-09T10:00:00Z' }] };
                  if (url === '/api/webadmin/condition-groups/smoke.roundtrip') return { id:'smoke.roundtrip', displayName:'Smoke 条件组', note:'', iconKey:'doctor-overview', enabled:true, fingerprint:'condition-fp', expectedFingerprint:'condition-fp', lockStatus:{ locked:false }, groupDefinition:{ id:'smoke.roundtrip', version:1, displayName:'Smoke 条件组', note:'', tags:[], root:{ id:'root', type:'group', name:'', note:'', enabled:true, groupMode:'AND', config:{ values:{} }, children:[{ id:'context', type:'context_equals', name:'', note:'', enabled:true, groupMode:'AND', config:{ values:{ field:'channel', expected:'mission.start' } }, children:[] }] } }, validation:{ valid:true, issues:[] } };
                  if (url === '/api/webadmin/condition-groups/smoke.roundtrip/validate') return { valid:true, issues:[], message:'条件组校验通过。' };
                  if (url === '/api/webadmin/condition-groups/smoke.roundtrip/preview') return { success:true, matched:true, previewOnly:true, failureReason:'', evaluatedCount:1, debugTree:{ matched:true, label:'上下文字段匹配', childResults:[] } };
                  if (url.startsWith('/api/webadmin/device-metadata/')) return { success:true, changed:false, message:'ok' };
                  if (url.startsWith('/api/webadmin/device-basic-config/')) return { supported:true, enabled:true, channel:'test.channel', expectedFingerprint:'basic-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/device-extended-config/')) return { supported:true, supportedFields:['pulseTicks','cooldownTicks'], fieldLabels:{ pulseTicks:'脉冲时长', cooldownTicks:'冷却时间' }, values:{ pulseTicks:20, cooldownTicks:0 }, expectedFingerprint:'extended-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/channel-metadata')) return { channel:'test.channel', displayName:'Test Channel', effectiveDisplayName:'Test Channel', note:'', iconKey:'auto', expectedFingerprint:'channel-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/selection/start')) return { success:true, targetType:'OBJECT_SELECTION', targetId:'sel-1', changed:true, message:'已通知目标玩家进入选择模式。', data:{ selection:{ selectionId:'sel-1', targetPlayerName:'Owner', purpose:'create_virtual_block_device', status:'started', channel:'test.channel' } } };
                  if (url.startsWith('/api/webadmin/selection/cancel')) return { success:true, targetType:'OBJECT_SELECTION', targetId:'sel-1', changed:true, message:'选择已取消。', data:{ selection:{ selectionId:'sel-1', targetPlayerName:'Owner', status:'cancelled', channel:'test.channel' } } };
                  if (url.startsWith('/api/webadmin/selection/status')) return { active:true, selectionId:'sel-1', status:'active', purpose:'create_virtual_block_device', targetPlayerName:'Owner', channel:'test.channel' };
                  if (url.startsWith('/api/webadmin/online-players')) return [{ name:'Owner', uuid:'00000000-0000-0000-0000-000000000001' }, { name:'Builder', uuid:'00000000-0000-0000-0000-000000000002' }];
                  if (url.startsWith('/api/webadmin/virtual-block-devices/') && url.endsWith('/native-triggers')) return { deviceId:'vdev-1', deviceType:'VIRTUAL_BLOCK_DEVICE', displayName:'Virtual', supported:true, readOnly:false, writeApiEnabled:true, nativeTriggerWriteApiEnabled:true, expectedFingerprint:'native-fp', lockStatus:{ locked:false }, availableTriggerTypes:['redstone_powered','blockstate','right_click','container_open','container_close','container_change'], allowedRedstoneModes:['redstone_rising','redstone_falling','redstone_both'], allowedConditionModes:['condition_enter','condition_exit','condition_both'], activeTriggerTypes:['redstone_powered','blockstate','right_click'], boundBlock:{dimension:'minecraft:overworld', pos:{x:3,y:64,z:4}, expectedBlockId:'minecraft:lever', actualBlockId:'minecraft:lever', status:'ready', worldAvailable:true, chunkLoaded:true, supportedPropertyCount:2}, triggers:{ redstone_powered:{type:'redstone_powered',label:'红石 / 受电状态',enabled:true,configured:true,mode:'redstone_both',modeDisplayName:'通电和断电都触发（redstone_both）',channel:'test.channel',offChannel:'off.channel',lastPowered:true,lastPowerLevel:15,currentPowered:true,currentPowerLevel:15,blockStatePowered:true,currentPoweredExpression:'currentPowered = blockStatePowered || receivedPowerLevel > 0',lastTriggerResult:'SUCCESS'}, blockstate:{type:'blockstate',label:'BlockState 条件',enabled:true,configured:true,conditionEnabled:true,conditionBlockId:'minecraft:lever',conditionProperties:{powered:'true'},conditionRaw:'powered=true',conditionMode:'condition_enter',conditionModeDisplayName:'进入条件时触发（condition_enter）',lastConditionMatched:true,lastConditionResult:'SUCCESS',runtimeState:'ready',supportedPropertyCount:2,propertiesFromBoundBlock:true,allowedValuesFromBoundBlock:true,currentMatched:true,serverValidatesBoundBlockProperties:true,supportedProperties:[{name:'powered',kind:'boolean',currentValue:'true',allowedValues:['true','false'],targetValue:'true',targetMatched:true,selectedInCondition:true},{name:'face',kind:'enum',currentValue:'wall',allowedValues:['floor','wall','ceiling'],targetValue:'',targetMatched:false,selectedInCondition:false}],validationIssues:[]}, right_click:{type:'right_click',label:'玩家右键交互',enabled:true,configured:true,interactionEnabled:true,interactChannel:'test.channel',interactionCooldownTicks:5,lastInteractionPlayerName:'Owner',lastInteractionResult:'SUCCESS',interactionItemMatcherLayer:{enabled:true,configured:true,templateItemId:'minecraft:stick',summary:'minecraft:stick x1'},conditionLayerNote:'interaction item matcher 是右键交互之后的条件/判定层，不是新的原生触发源。'}, container_open:{type:'container_open',label:'容器打开',enabled:false,configured:false,containerEnabled:false,containerOpenChannel:'',containerCooldownTicks:0,lastContainerResult:''}, container_close:{type:'container_close',label:'容器关闭',enabled:false,configured:false,containerEnabled:false,containerCloseChannel:'',containerCooldownTicks:0,lastContainerResult:''}, container_change:{type:'container_change',label:'容器内容变化',enabled:false,configured:false,containerEnabled:false,containerChangeChannel:'',containerCooldownTicks:0,containerChangeCheckIntervalTicks:10,itemConditionCount:0,itemConditions:[],itemConditionsReadOnly:true,templateEditorPhase:'7.9 P3',lastContainerResult:''} }, forbiddenInP2:['itemSubmit','consume','conditionEngine','successFailPathGraph','scratchLikeEditor','rawJsonTextarea','containerItemTemplateGui'] };
                  if (url.startsWith('/api/webadmin/virtual-block-devices/') && url.endsWith('/container-template-session/start')) return { success:true, targetType:'VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION', targetId:'ct-1', changed:false, message:'已通知目标玩家打开 GUI。', data:{ containerTemplateSession:{ sessionRef:'ct-1', id:'ct-1', status:'started', active:true } } };
                  if (url.startsWith('/api/webadmin/virtual-block-devices/') && url.includes('/container-template-session/status')) return { sessionRef:'ct-1', id:'ct-1', status:'started', active:true, message:'等待目标玩家打开 GUI。' };
                  if (url.startsWith('/api/webadmin/virtual-block-devices/') && url.endsWith('/container-template-session/cancel')) return { success:true, targetType:'VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION', targetId:'ct-1', changed:false, message:'容器模板会话已取消。', data:{ containerTemplateSession:{ sessionRef:'ct-1', id:'ct-1', status:'cancelled', active:false } } };
                  if (url.startsWith('/api/webadmin/virtual-block-devices/') && url.endsWith('/container-template')) { const id = decodeURIComponent(url.substring('/api/webadmin/virtual-block-devices/'.length).slice(0, -'/container-template'.length)); return { deviceId:id, displayName:'Complex VBD', supported:true, typeSupported:true, p3bGhostEditing:true, saveImplemented:true, dryRunGhostInteraction:false, expectedFingerprint:'container-template-fp', lockStatus:{ locked:false }, itemConditions:[{ id:'slot-0', name:'Slot 0 stone', type:'slot_item', slot:0, itemId:'minecraft:stone', templateItemId:'minecraft:stone', countMode:'at_least', count:1 }, { id:'total-stick', name:'Total sticks', type:'total_item', itemId:'minecraft:stick', templateItemId:'minecraft:stick', countMode:'exactly', count:8 }] }; }
                  if (url.startsWith('/api/webadmin/virtual-block-devices/')) return { success:true, targetType:'VIRTUAL_BLOCK_DEVICE', targetId:'vdev-1', changed:true, message:'虚拟方块设备已删除 / 解绑，世界方块未被破坏。', data:{ deviceId:'vdev-1', routeTarget:'#/virtual-block-devices' } };
                  if (url === '/api/webadmin/signal-listeners') return { success:true, targetType:'SIGNAL_LISTENER', targetId:'new-listener', changed:true, message:'Signal Listener 已创建。', data:{ listenerId:'new-listener', routeTarget:'#/listeners/new-listener?returnTo=%23%2Flisteners' } };
                  if (url.startsWith('/api/webadmin/signal-listeners/')) return { success:true, targetType:'SIGNAL_LISTENER', targetId:'test-listener', changed:true, message:'Signal Listener 已删除。', data:{ listenerId:'test-listener', routeTarget:'#/listeners' } };
                  if (url.startsWith('/api/devices/')) { const id = decodeURIComponent(url.substring('/api/devices/'.length).split('?')[0]); if (id === 'vdev-1') return { id, displayName:'Virtual', type:'VIRTUAL_BLOCK_DEVICE', enabled:true, channel:'test.channel', world:'world', pos:{x:3,y:64,z:4}, doctorStatus:'INFO', metadata:{ displayName:'Virtual', note:'', iconKey:'virtual_block_device', version:1, updatedAt:'2026-05-09T10:00:00Z', updatedBy:'Owner' }, configSummary:{triggerType:'interact', blockId:'minecraft:lever', expectedFingerprint:'vbd-fp'}, debugSummary:{status:'OK'} }; return { id, displayName:'Emitter', type:'SIGNAL_RECEIVER', enabled:true, channel:'test.channel', world:'world', pos:{x:1,y:64,z:2}, doctorStatus:'OK', metadata:{ displayName:'Emitter', note:'', iconKey:'auto', version:1, updatedAt:'2026-05-09T10:00:00Z', updatedBy:'Owner' }, configSummary:{pulseTicks:20, expectedFingerprint:'cfg-fp'}, debugSummary:{status:'OK'} }; }
                  if (url.startsWith('/api/devices')) return [
                    { id:'dev-1', displayName:'Emitter', type:'SIGNAL_EMITTER', enabled:true, channel:'test.channel', world:'world', pos:{x:1,y:64,z:2}, doctorStatus:'OK' },
                    { id:'recv-1', displayName:'Receiver', type:'SIGNAL_RECEIVER', enabled:true, channel:'test.channel', world:'world', pos:{x:2,y:64,z:3}, doctorStatus:'OK', configSummary:{pulseTicks:20} },
                    { id:'vdev-1', displayName:'Virtual', type:'VIRTUAL_BLOCK_DEVICE', enabled:true, channel:'test.channel', world:'world', pos:{x:3,y:64,z:4}, doctorStatus:'INFO', configSummary:{triggerType:'interact', blockId:'minecraft:lever'} }
                  ];
                  if (url.startsWith('/api/signals/channels/')) { const channel = decodeURIComponent(url.substring('/api/signals/channels/'.length).split('?')[0]); return { channel, type:'CUSTOM', metadata:{effectiveDisplayName:'Test Channel', note:'Test note', updatedAt:'2026-05-09T10:00:00Z', updatedBy:'Owner'}, stats:{listenerCount:1, receiverCount:1, actionRelayCount:0, sourceDeviceCount:1, triggerCountToday:3, totalTriggerCount:9, lastTriggeredAt:'2026-05-09T10:00:00Z'}, listeners:[{id:'test-listener', name:'Test Listener', enabled:true, cooldownTicks:0, actionCount:1, lastTriggeredAt:'2026-05-09T10:00:00Z', actions:[{id:'action-1', type:'COMMAND', summary:'say test', doctorStatus:'OK'}]}], receivers:[{id:'recv-1', name:'Receiver'}], actionRelays:[], actions:[{id:'action-1', type:'COMMAND', summary:'say test', doctorStatus:'OK'}], sources:[{id:'test-device', name:'Emitter'}], downstreamSignals:[], recentHistory:[{ time:'2026-05-09T10:00:00Z', channel, sourceType:'DEVICE', sourceName:'Emitter', result:'SUCCESS' }], doctorIssues:[], doctorStatus:'OK' }; }
                  if (url.startsWith('/api/signals/channels')) return [{ channel:'test.channel', displayName:'Test Channel', listenerCount:1, receiverCount:1, actionRelayCount:0, consumerCount:2, doctorStatus:'OK' }];
                  if (url.startsWith('/api/webadmin/signal-listener-basic-config/')) return { listenerRef:'test-listener', listenerId:'test-listener', displayName:'Test Listener', enabled:true, channel:'test.channel', cooldownTicks:0, actionCount:1, expectedFingerprint:'listener-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/actions/')) { const id = decodeURIComponent(url.substring('/api/actions/'.length).split('?')[0]); return { id, type:'COMMAND', owner:{ownerType:'LISTENER', ownerId:'test-listener', ownerName:'Test Listener', channel:'test.channel'}, configSummary:{name:'Open Door', executionCount:2, referencedByCount:1, doctorStatus:'OK'}, summary:'command: say test', recentExecutions:[{time:'2026-05-09T10:00:00Z', result:'SUCCESS', owner:'Test Listener', detail:'ok'}], doctorIssues:[] }; }
                  if (url.startsWith('/api/actions')) return [
                    { id:'action-1', name:'Open Door', type:'COMMAND', summary:'command: say test', doctorStatus:'OK', ownerType:'LISTENER', channel:'test.channel', executionCountToday:2, lastExecutedAt:'2026-05-09T10:00:00Z' },
                    { id:'action-2', name:'Send Signal', type:'SIGNAL', summary:'signal: next', doctorStatus:'WARNING', ownerType:'ACTION_RELAY', channel:'next.channel', executionCountToday:0 }
                  ];
                  if (url.startsWith('/api/doctor')) return { summary:{errorCount:2, warningCount:6, infoCount:4}, issues:Array.from({length:12}, (_, index) => ({id:`issue-${index + 1}`, title:`Doctor issue ${index + 1}`, severity:index < 2 ? 'ERROR' : (index < 8 ? 'WARNING' : 'INFO'), relatedObjectType:index % 2 === 0 ? 'DEVICE' : 'CHANNEL', relatedObjectId:index % 2 === 0 ? 'dev-1' : '', channel:index % 2 === 0 ? '' : 'test.channel', message:`message ${index + 1}`, suggestion:`suggestion ${index + 1}`, detectedAt:'2026-05-09T10:00:00Z'})) };
                  if (url.startsWith('/api/signals/history')) return [{ time:'2026-05-09T10:00:00Z', channel:'test.channel', sourceType:'DEVICE', sourceName:'Emitter', sourceId:'dev-1', result:'SUCCESS' }];
                  if (url.startsWith('/api/regions/')) { const id = decodeURIComponent(url.substring('/api/regions/'.length).split('?')[0]); return { id, name:'Spawn', world:'world', bounds:{min:{x:0,y:60,z:0}, max:{x:10,y:70,z:10}}, targetFilter:'ALL', actions:{enter:[{id:'action-1', type:'COMMAND', summary:'command: say test', enabled:true}], exit:[], stay:[]}, boundChannels:['test.channel'], playersInside:['Owner'], recentEvents:[{type:'enter', time:'2026-05-09T10:00:00Z', playerName:'Owner'}], doctorIssues:[] }; }
                  if (url.startsWith('/api/regions')) return [{ id:'region-1', name:'Spawn', world:'world', enabled:true, doctorStatus:'OK', bounds:{min:{x:0,y:60,z:0}, max:{x:10,y:70,z:10}}, controllerCount:1, enterActionCount:1, exitActionCount:0, stayActionCount:0, targetFilter:'ALL' }];
                  if (url.startsWith('/api/webadmin/settings')) return { storage:{baseDir:'test'}, security:{accessMode:'LOCAL_ONLY'}, audit:{enabled:true} };
                  if (url.startsWith('/api/status')) return { online:true, serverStatus:'RUNNING', version:'test' };
                  if (url.startsWith('/api/webadmin/write/capabilities')) return { canWrite:false };
                  if (url.startsWith('/api/webadmin/users')) return { users:[{username:'Owner', displayName:'Owner', role:'OWNER', enabled:true, online:true, sessionCount:1, lastLoginAt:'2026-05-09T10:00:00Z'}] };
                  if (url.startsWith('/api/auth/me')) return { username:'Owner', role:'OWNER' };
                  return {};
                }
                const context = {
                  console: { log(){}, warn(){}, error(...args){ errors.push(args.map(String).join(' ')); } },
                  setTimeout, clearTimeout, setInterval, clearInterval,
                  requestAnimationFrame(fn){ fn(); },
                  requestedUrls,
                  requestedRequests,
                  URLSearchParams,
                  encodeURIComponent,
                  decodeURIComponent,
                  location: { hash:'#/dashboard', href:'' },
                  navigator: { onLine:true },
                  sessionStorage: { getItem(){ return null; }, setItem(){}, removeItem(){} },
                  document: {
                    body,
                    documentElement: { classList: { add(){}, remove(){}, toggle(){} } },
                    activeElement: null,
                    getElementById(id){ return id === 'wa-modal-root' ? (elements.get(id) || null) : el(id); },
                    querySelector(){ return null; },
                    querySelectorAll(){ return []; },
                    addEventListener(){},
                    createElement(tag){ const node = el(`${tag}-${elements.size}`); node.tagName = String(tag || '').toUpperCase(); node.id = ''; return node; },
                  },
                  window: { addEventListener(){}, removeEventListener(){} },
                  fetch: async (path, options={}) => { requestedUrls.push(String(path)); requestedRequests.push({ url:String(path), method:String(options.method || 'GET').toUpperCase(), body:String(options.body || ''), headers:options.headers || {} }); return { ok:true, status:200, json: async () => ({ ok:true, data: apiData(path, options) }) }; },
                  EventSource: function(){ this.close = function(){}; },
                };
                context.window.location = context.location;
                context.window.navigator = context.navigator;
                context.globalThis = context;
                vm.createContext(context);
                vm.runInContext(code + "\\n;globalThis.__smokeRoute = async function(hash){ location.hash = hash; return await route(); };globalThis.__smokeModal = async function(){ openWebAdminModal('Smoke Modal','<form class=\\\"edit-form\\\"><div class=\\\"form-actions\\\">hidden</div></form>',editModalFooter(false)); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); const closePromise = closeWebAdminModal(false); const className = String(modal && modal.className || ''); const closingMarker = String(modal && modal.dataset && modal.dataset.modalClosing || ''); await closePromise; return {html, className, closingMarker, removed:!document.getElementById('wa-modal-root')}; };globalThis.__smokeDeviceConfigModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/devices/test-device'; await route(); await startDeviceConfigEdit('test-device'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); const className = String(modal && modal.className || ''); await cancelDeviceConfigEdit('test-device'); return {html,className}; };globalThis.__smokeChannelModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/signals/test.channel'; await route(); await startChannelMetadataEdit('test.channel'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); await cancelChannelMetadataEdit('test.channel'); return {html}; };globalThis.__smokeListenerModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/listeners/test-listener'; await route(); await startSignalListenerBasicConfigEdit('test-listener','test.channel'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); await cancelSignalListenerBasicConfigEdit('test-listener','test.channel'); return {html}; };", context, { filename:'webadmin-app.js' });
                vm.runInContext(`
                  globalThis.__smokeSilentRoute = async function(hash){ location.hash = hash; return await route({silent:true}); };
                  globalThis.__smokeVbdDeviceConfigModal = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    location.hash = '#/devices/vdev-1';
                    await route();
                    const detailHtml = String(document.getElementById('view').innerHTML || '');
                    await startDeviceConfigEdit('vdev-1');
                    const modal = document.getElementById('wa-modal-root');
                    const modalHtml = String(modal && modal.innerHTML || '');
                    const urls = requestedUrls.join('|');
                    await cancelDeviceConfigEdit('vdev-1');
                    return { detailHtml, modalHtml, urls };
                  };
                  globalThis.__smokeDoctorPagination = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    appState.doctorFilters = { search:'', severity:'ALL', objectType:'ALL', jump:'ALL' };
                    appState.uiPages = appState.uiPages || {};
                    appState.uiPages.doctor = 1;
                    location.hash = '#/doctor';
                    await route();
                    const page1 = String(document.getElementById('view').innerHTML || '');
                    setWaPage('doctor', 2);
                    const page2 = String(document.getElementById('view').innerHTML || '');
                    const pageAfterNext = appState.uiPages.doctor;
                    const filtersAfterPage = JSON.stringify(appState.doctorFilters);
                    appState.doctorFilters.severity = 'ERROR';
                    appState.uiPages.doctor = 2;
                    renderDoctorList('');
                    const filteredPage = appState.uiPages.doctor;
                    return { page1, page2, pageAfterNext, filtersAfterPage, filteredPage };
                  };
                  globalThis.__smokeConditionDebuggerDetail = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    appState.conditionDebuggerFilters = { search:'test-listener', targetType:'SIGNAL_LISTENER', result:'BLOCKED', conditionGroupId:'smoke.roundtrip', channel:'test.channel' };
                    appState.uiPages = appState.uiPages || {};
                    appState.uiPages.conditionDebugger = 1;
                    location.hash = '#/condition-debugger';
                    await route();
                    const listHtml = String(document.getElementById('view').innerHTML || '');
                    navigateTo('#/condition-debugger/smoke-record');
                    const detailHash = String(location.hash || '');
                    await route();
                    const detailHtml = String(document.getElementById('view').innerHTML || '');
                    await replayConditionGateHistory('smoke-record');
                    const replayHtml = String(document.getElementById('view').innerHTML || '');
                    const replayUrls = requestedRequests.filter(r => r.url.includes('/condition-gates/history')).map(r => r.method + ' ' + r.url);
                    const replayBackKeepsReturnTo = replayHtml.includes('targetType=SIGNAL_LISTENER') && replayHtml.includes('result=BLOCKED') && replayHtml.includes('conditionGroupId=smoke.roundtrip');
                    await route({silent:true});
                    const refreshedHash = String(location.hash || '');
                    const refreshedHtml = String(document.getElementById('view').innerHTML || '');
                    failConditionDebuggerDetail = true;
                    await route({silent:true});
                    const transientHash = String(location.hash || '');
                    const transientHtml = String(document.getElementById('view').innerHTML || '');
                    failConditionDebuggerDetail = false;
                    const returnTo = parseHashParams(location.hash).returnTo || '';
                    goBackOrFallback(returnTo, '#/condition-debugger');
                    await route();
                    const backHtml = String(document.getElementById('view').innerHTML || '');
                    return { listHtml, detailHash, detailHtml, replayHtml, replayUrls, replayBackKeepsReturnTo, refreshedHash, refreshedHtml, transientHash, transientHtml, backHtml, filters:JSON.stringify(appState.conditionDebuggerFilters) };
                  };
                  globalThis.__smokeToggleAdvanced = async function(kind,id){ await toggleAdvancedDetail(kind,id); return String(document.getElementById('view').innerHTML || ''); };
                  globalThis.__smokeModalSilent = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    location.hash = '#/devices/test-device';
                    await route();
                    await startDeviceConfigEdit('test-device');
                    const modalBefore = document.getElementById('wa-modal-root');
                    const before = String(modalBefore && modalBefore.innerHTML || '');
                    await route({silent:true});
                    const modalAfter = document.getElementById('wa-modal-root');
                    const after = String(modalAfter && modalAfter.innerHTML || '');
                    const stillOpen = !!modalAfter && after.includes('data-unified-device-config="true"');
                    await cancelDeviceConfigEdit('test-device');
                    return { before, after, stillOpen };
                  };
                  globalThis.__smokeChannelModalSilent = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    location.hash = '#/signals/test.channel';
                    await route();
                    await startChannelMetadataEdit('test.channel');
                    const before = String(document.getElementById('wa-modal-root').innerHTML || '');
                    await route({silent:true});
                    const after = String(document.getElementById('wa-modal-root').innerHTML || '');
                    const stillOpen = after.includes('channel-metadata-display-name');
                    await cancelChannelMetadataEdit('test.channel');
                    return { before, after, stillOpen };
                  };
                  globalThis.__smokeListenerModalSilent = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    location.hash = '#/listeners/test-listener';
                    await route();
                    await startSignalListenerBasicConfigEdit('test-listener','test.channel');
                    const before = String(document.getElementById('wa-modal-root').innerHTML || '');
                    await route({silent:true});
                    const after = String(document.getElementById('wa-modal-root').innerHTML || '');
                    const stillOpen = after.includes('listener-enabled-') && after.includes('listener-cooldown-');
                    await cancelSignalListenerBasicConfigEdit('test-listener','test.channel');
                    return { before, after, stillOpen };
                  };
                  globalThis.__smokeCreateVirtualBlockSelectionModal = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    requestedUrls.length = 0;
                    location.hash = '#/virtual-block-devices';
                    await route();
                    await openCreateVirtualBlockDeviceModal();
                    const config = String(document.getElementById('wa-modal-root').innerHTML || '');
                    document.getElementById('selection-target-player').value = 'Owner';
                    document.getElementById('selection-channel').value = 'test.channel';
                    document.getElementById('selection-display-name').value = 'Smoke VBD';
                    document.getElementById('selection-enabled').checked = true;
                    await startCreateVirtualBlockDeviceSelection();
                    const waiting = String(document.getElementById('wa-modal-root').innerHTML || '');
                    const startedId = appState.selectionCreateVirtualBlock && appState.selectionCreateVirtualBlock.selectionId;
                    handleSelectionRealtimeEvent({ type:'selection_completed', id:'evt-1', summary:'虚拟方块设备已创建。', deviceId:'vdev-1', payload:{ selectionId:startedId, deviceId:'vdev-1', routeTarget:'#/devices/vdev-1' } });
                    const completedHash = String(location.hash || '');
                    const completedModal = String(document.getElementById('wa-modal-root')?.innerHTML || '');
                    handleSelectionRealtimeEvent({ type:'selection_cancelled', id:'evt-2', summary:'选择已取消。', payload:{ selectionId:startedId } });
                    const afterDuplicateHash = String(location.hash || '');
                    await route();
                    const detailHtml = String(document.getElementById('app-view').innerHTML || '');
                    const keys = Array.from(realtimeRouteKeysForEvent({ type:'selection_completed', deviceId:'vdev-1', payload:{ selectionId:startedId, deviceId:'vdev-1' } })).join(',');
                    await closeCreateVirtualBlockDeviceModal(false);
                    return { config, waiting, completedHash, completedModal, afterDuplicateHash, detailHtml, urls:requestedUrls.slice(), keys };
                  };
                  globalThis.__smokeLifecycleModals = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    requestedUrls.length = 0;
                    location.hash = '#/listeners';
                    await route();
                    await openSignalListenerCreateModal();
                    const listenerCreateHtml = String(document.getElementById('wa-modal-root').innerHTML || '');
                    document.getElementById('listener-create-name').value = 'Smoke Listener';
                    document.getElementById('listener-create-channel').value = 'test.channel';
                    document.getElementById('listener-create-enabled').checked = true;
                    document.getElementById('listener-create-cooldown').value = '0';
                    await saveSignalListenerCreateModal();
                    const listenerCreateHash = String(location.hash || '');
                    const listenerCreateUrls = requestedUrls.slice();
                    requestedUrls.length = 0;
                    location.hash = '#/listeners/test-listener';
                    await route();
                    await openSignalListenerDeleteModal('test-listener','test.channel');
                    const listenerDeleteHtml = String(document.getElementById('wa-modal-root').innerHTML || '');
                    document.getElementById('listener-delete-confirmed').checked = true;
                    await deleteSignalListenerFromModal();
                    const listenerDeleteHash = String(location.hash || '');
                    const listenerDeleteUrls = requestedUrls.slice();
                    requestedUrls.length = 0;
                    location.hash = '#/virtual-block-devices';
                    await route();
                    await openVirtualBlockDeviceDeleteModal('vdev-1');
                    const vbdDeleteHtml = String(document.getElementById('wa-modal-root').innerHTML || '');
                    document.getElementById('vbd-delete-confirmed').checked = true;
                    document.getElementById('vbd-delete-confirmation').value = 'vdev-1';
                    await deleteVirtualBlockDeviceFromModal();
                    const vbdDeleteHash = String(location.hash || '');
                    const vbdDeleteUrls = requestedUrls.slice();
                    const keys = Array.from(realtimeRouteKeysForEvent({ type:'device_removed', deviceId:'vdev-1', sourceType:'virtual_block_device' })).join(',') + '|' + Array.from(realtimeRouteKeysForEvent({ type:'signal_listener_changed', channel:'test.channel', payload:{ listenerId:'test-listener' } })).join(',');
                    return { listenerCreateHtml, listenerCreateHash, listenerCreateUrls, listenerDeleteHtml, listenerDeleteHash, listenerDeleteUrls, vbdDeleteHtml, vbdDeleteHash, vbdDeleteUrls, keys };
                  };
                  globalThis.__smokeContainerTemplateModal = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    const complex = 'virtual_block_device:minecraft:overworld@-11,-60,-7';
                    requestedUrls.length = 0;
                    await openContainerTemplateSessionModal(complex);
                    const config = String(document.getElementById('wa-modal-root')?.innerHTML || '');
                    const player = document.getElementById('container-template-player');
                    player.value = 'Owner';
                    player.selectedIndex = 0;
                    player.options = [{ dataset:{ uuid:'00000000-0000-0000-0000-000000000001' } }];
                    await startContainerTemplateSession(complex);
                    const waiting = String(document.getElementById('wa-modal-root')?.innerHTML || '');
                    const closeAttempt = await closeContainerTemplateSessionModal(complex);
                    const closeConfirmOpen = !!document.getElementById('wa-container-template-cancel-confirm');
                    cancelContainerTemplateCancelConfirm();
                    const afterContinue = String(document.getElementById('wa-modal-root')?.innerHTML || '');
                    requestContainerTemplateSessionCancel(complex);
                    const cancelConfirmOpen = !!document.getElementById('wa-container-template-cancel-confirm');
                    await confirmContainerTemplateSessionCancel();
                    const afterClose = String(document.getElementById('wa-modal-root')?.innerHTML || '');
                    return { config, waiting, urls: requestedUrls.slice(), closeAttempt, closeConfirmOpen, afterContinue, cancelConfirmOpen, afterClose, session: appState.containerTemplateSession };
                  };
                  globalThis.__smokeRealtime = function(){
                    const listenerLock = { type:'edit_lock_changed', payload:{ targetType:'signal_listener_basic_config', targetId:'test-listener' } };
                    const signalEvent = { type:'channel_metadata_changed', channel:'test.channel' };
                    return {
                      listenerRef: listenerEventRef(listenerLock),
                      listenerKeys: Array.from(realtimeRouteKeysForEvent(listenerLock)).join(','),
                      listenerShould: shouldHandleRealtimeEvent('#/listeners/test-listener', listenerLock),
                      signalShould: shouldHandleRealtimeEvent('#/signals/test.channel', signalEvent),
                      signalKeys: Array.from(realtimeRouteKeysForEvent(signalEvent)).join(',')
                    };
                  };
                  globalThis.__smokeRealtimeRun = async function(){
                    requestedUrls.length = 0;
                    location.hash = '#/listeners/test-listener';
                    await route();
                    requestedUrls.length = 0;
                    const before = String(document.getElementById('view').innerHTML || '');
                    const eventData = { type:'edit_lock_changed', seq:101, payload:{ targetType:'signal_listener_basic_config', targetId:'test-listener' } };
                    handleRealtimeEvent('edit_lock_changed', { data: JSON.stringify(eventData), lastEventId:'101' });
                    await new Promise(resolve => setTimeout(resolve, 320));
                    const after = String(document.getElementById('view').innerHTML || '');
                    return { before, after, urls: requestedUrls.slice(), pending:Object.keys(appState.realtime.pendingRefresh).length, dirty:Object.keys(appState.realtime.dirtyRoutes).join(',') };
                  };
                  globalThis.__smokeConditionGroupSavePayload = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    await loadConditionCatalog(true);
                    const cases = [
                      { type:'context_equals', values:{ field:'channel', expected:'mission.start' } },
                      { type:'state_variable_bool_equals', values:{ scope:'GLOBAL', key:'game.active', targetMode:'global', expected:'true' } },
                      { type:'inventory_contains_item', values:{ inventoryKey:'player', itemId:'minecraft:diamond', countOperator:'gte', count:'1' } },
                      { type:'container_slot_item_matches', values:{ containerKey:'chest', slot:'0', itemId:'minecraft:stone' } },
                      { type:'region_enabled', values:{ regionKey:'spawn' } },
                      { type:'signal_event_count_compare', values:{ signalHistoryKey:'history', operator:'gte', count:'2' } },
                      { type:'logic_chain_has_cycle', values:{ logicChainKey:'chain', expected:'false' } }
                    ];
                    const results = [];
                    for (const testCase of cases) {
                      appState.conditionGroupEdit = makeConditionDraftFromDetail({ id:'smoke.' + testCase.type, displayName:'Smoke Create' }, 'create');
                      const target = conditionNodeByPath('0');
                      appState.conditionNodeEditor = { open:true, path:'0', draft:cloneConditionNode(target), errors:[], typeQuery:'', typeSuite:'all', initialSnapshot:'' };
                      changeConditionNodeType('', testCase.type);
                      appState.conditionNodeEditor.draft.config = { values:{ ...testCase.values } };
                      const selectorHtml = conditionTypePicker(appState.conditionNodeEditor, appState.conditionNodeEditor.draft.type);
                      Object.keys(target).forEach(k => delete target[k]);
                      Object.assign(target, cloneConditionNode(appState.conditionNodeEditor.draft));
                      appState.conditionNodeEditor = null;
                      syncConditionGroupDraft();
                      const payload = conditionGroupSavePayload(appState.conditionGroupEdit);
                      const child = payload.groupDefinition.root.children[0];
                      results.push({
                        expectedType:testCase.type,
                        selectedCount:(selectorHtml.match(/data-condition-type-single-selected/g) || []).length,
                        selectorMarker:selectorHtml.includes('data-condition-type-selector="list-search-custom-ui"'),
                        type:child.type,
                        values:child.config.values,
                        fellBack:child.type === 'always_true'
                      });
                    }
                    return { results };
                  };
                  globalThis.__smokeConditionGroupSaveRequest = async function(){
                    appState.me = { username:'Owner', role:'OWNER' };
                    appState.capabilities = { csrf:{ token:'csrf-smoke' } };
                    await loadConditionCatalog(true);
                    requestedRequests.length = 0;
                    appState.conditionGroupEdit = makeConditionDraftFromDetail({ id:'smoke.request', displayName:'Smoke Request' }, 'create');
                    appState.conditionNodeEditor = { path:'0' };
                    document.getElementById('condition-group-id').value = 'smoke.request';
                    document.getElementById('condition-group-name').value = 'Smoke Request';
                    document.getElementById('condition-group-icon').value = 'doctor-overview';
                    document.getElementById('condition-group-note').value = '';
                    document.getElementById('condition-group-tags').value = '';
                    document.getElementById('condition-group-enabled').checked = true;
                    const target = conditionNodeByPath('0');
                    appState.conditionNodeEditor = { open:true, path:'0', draft:cloneConditionNode(target), errors:[], typeQuery:'', typeSuite:'all', initialSnapshot:'' };
                    changeConditionNodeType('', 'context_equals');
                    appState.conditionNodeEditor.draft.config = { values:{ field:'channel', expected:'mission.start' } };
                    Object.keys(target).forEach(k => delete target[k]);
                    Object.assign(target, cloneConditionNode(appState.conditionNodeEditor.draft));
                    appState.conditionNodeEditor = null;
                    await saveConditionGroup();
                    const post = requestedRequests.find(r => r.url === '/api/webadmin/condition-groups' && r.method === 'POST') || {};
                    const body = post.body ? JSON.parse(post.body) : {};
                    const child = body.groupDefinition?.root?.children?.[0] || {};
                    return {
                      urls: requestedRequests.map(r => r.method + ' ' + r.url),
                      hasCsrf: !!(post.headers && post.headers['X-TZZ-WebAdmin-CSRF']),
                      type: child.type,
                      field: child.config?.values?.field,
                      expected: child.config?.values?.expected,
                      fellBack: child.type === 'always_true'
                    };
                  };
                  globalThis.__smokeClearRequestedUrls = function(){ requestedUrls.length = 0; };
                  globalThis.__smokeRequestedUrls = function(){ return requestedUrls.slice(); };
                `, context, { filename:"webadmin-app-extra-smoke.js" });
                """;
        harness += """
                const routes = [
                  '#/dashboard',
                  '#/signals',
                  '#/signalbridge',
                  '#/receivers',
                  '#/listeners',
                  '#/signal-listeners',
                  '#/actions',
                  '#/devices',
                  '#/virtual-block-devices',
                  '#/block-devices',
                  '#/history',
                  '#/history?channel=test.channel',
                  '#/events',
                  '#/config',
                  '#/users',
                  '#/settings',
                  '#/system-settings',
                  '#/regions',
                  '#/region-controllers',
                  '#/action-templates',
                  '#/templates',
                  '#/condition-groups',
                  '#/condition-groups/smoke.roundtrip',
                  '#/conditions',
                  '#/conditions/smoke.roundtrip',
                  '#/condition-debugger',
                  '#/condition-debugger/smoke-record',
                  '#/condition-debugger?id=smoke-record',
                  '#/condition-debugger/missing-record',
                  '#/doctor',
                  '#/diagnostics',
                  '#/signal-doctor',
                  '#/signals/test.channel',
                  '#/devices/test-device',
                  '#/devices/vdev-1',
                  '#/devices/minecraft%3Aoverworld%409%2C-60%2C13',
                  '#/devices/signal_receiver%3Aminecraft%3Aoverworld%40-13%2C-60%2C10',
                  '#/actions/test-action',
                  '#/regions/test-region',
                  '#/listeners/test-listener',
                  '#/signal-listeners/test-listener'
                ];
                const detailRoutes = new Set([
                  '#/signals/test.channel',
                  '#/devices/test-device',
                  '#/devices/vdev-1',
                  '#/devices/minecraft%3Aoverworld%409%2C-60%2C13',
                  '#/devices/signal_receiver%3Aminecraft%3Aoverworld%40-13%2C-60%2C10',
                  '#/actions/test-action',
                  '#/regions/test-region',
                  '#/listeners/test-listener',
                  '#/signal-listeners/test-listener'
                ]);
                const settingsRoutes = new Set([
                  '#/settings',
                  '#/system-settings'
                ]);
                const responsiveListRoutes = new Set([
                  '#/action-templates',
                  '#/doctor',
                  '#/history',
                  '#/history?channel=test.channel',
                  '#/virtual-block-devices'
                ]);
                const expectedDetailApi = {
                  '#/signals/test.channel':'/api/signals/channels/test.channel',
                  '#/devices/test-device':'/api/devices/test-device',
                  '#/devices/vdev-1':'/api/devices/vdev-1',
                  '#/devices/minecraft%3Aoverworld%409%2C-60%2C13':'/api/devices/minecraft%3Aoverworld%409%2C-60%2C13',
                  '#/devices/signal_receiver%3Aminecraft%3Aoverworld%40-13%2C-60%2C10':'/api/devices/minecraft%3Aoverworld%40-13%2C-60%2C10',
                  '#/actions/test-action':'/api/actions/test-action',
                  '#/regions/test-region':'/api/regions/test-region',
                  '#/listeners/test-listener':'/api/webadmin/signal-listener-basic-config/test-listener',
                  '#/signal-listeners/test-listener':'/api/webadmin/signal-listener-basic-config/test-listener'
                };
                (async () => {
                  const failures = [];
                  for (const route of routes) {
                    errors.length = 0;
                    view.innerHTML = '';
                    context.__smokeClearRequestedUrls();
                    try {
                      await context.__smokeRoute(route);
                    } catch (err) {
                      failures.push(`${route}: ${err.name}: ${err.message}`);
                      continue;
                    }
                    const html = String(view.innerHTML || '');
                    const errorText = errors.join('\\n');
                    if (/ReferenceError|countBy is not defined|parseHashParams is not defined|renderIcons is not defined|route render failed/.test(errorText)) {
                      failures.push(`${route}: console error ${errorText}`);
                    } else if (html.includes('error-state')) {
                      failures.push(`${route}: rendered error-state`);
                    } else if (html.includes('loading-state')) {
                      failures.push(`${route}: remained in loading-state`);
                    } else if (!html.trim()) {
                      failures.push(`${route}: rendered empty view`);
                    }
                    if (route === '#/condition-debugger') {
                      if (!html.includes('data-condition-gate-list-route="true"') || !html.includes('data-condition-gate-history-table="true"') || !html.includes('data-condition-gate-row-click-navigates-detail="true"') || !html.includes('data-condition-gate-list-full-width="true"') || !html.includes('data-condition-gate-list-no-full-debug-rail="true"')) {
                        failures.push(`${route}: condition debugger list is missing full-width list or row-click markers`);
                      }
                      if (!html.includes('#/condition-debugger/smoke-record') || html.includes('data-condition-gate-debug-detail="true"') || html.includes('请选择一条 gate 历史记录')) {
                        failures.push(`${route}: condition debugger list still behaves like narrow selected-detail rail`);
                      }
                    }
                    if (route === '#/condition-debugger/smoke-record' || route === '#/condition-debugger?id=smoke-record') {
                      if (!html.includes('data-condition-gate-detail-route="true"') || !html.includes('data-condition-gate-detail-full-width="true"') || !html.includes('data-condition-gate-detail-summary="true"') || !html.includes('data-condition-gate-context-summary="true"') || !html.includes('data-condition-gate-debug-tree-section="true"') || !html.includes('data-condition-gate-replay-section="true"') || !html.includes('data-condition-gate-technical-collapsed-readonly="true"')) {
                        failures.push(`${route}: condition debugger detail missing full-width section markers`);
                      }
                      if (!html.includes('wa-detail-shell') || !html.includes('data-detail-kind="condition-debugger"') || !html.includes('返回') || html.includes('请选择一条 gate 历史记录')) {
                        failures.push(`${route}: condition debugger detail did not render independent detail shell`);
                      }
                      const routeUrls = context.__smokeRequestedUrls();
                      if (!routeUrls.includes('/api/webadmin/condition-gates/history/smoke-record')) {
                        failures.push(`${route}: did not execute condition debugger detail API`);
                      }
                    }
                    if (route === '#/condition-debugger/missing-record' && !html.includes('data-condition-gate-not-found="true"')) {
                      failures.push(`${route}: missing record did not render Chinese not-found state marker`);
                    }
                    if (settingsRoutes.has(route)) {
                      if (!html.includes('data-settings-layout="true"') || !html.includes('data-responsive-layout="true"') || !html.includes('data-settings-tabs="true"') || !html.includes('wa-tabs-scroll') || !html.includes('data-settings-status-rail="true"') || !html.includes('data-settings-switch-grid="true"')) {
                        failures.push(`${route}: missing 7.5 settings layout shell`);
                      }
                      if (!html.includes('wa-settings-info-grid') || !html.includes('wa-settings-action-list') || !html.includes('wa-progress-bar')) {
                        failures.push(`${route}: missing settings info grid, progress rail, or quick actions`);
                      }
                      if (!html.includes('保存设置') || !html.includes('重置为默认') || !html.includes('disabled')) {
                        failures.push(`${route}: settings unsupported write actions are not visibly disabled`);
                      }
                      if (html.includes('panel-card') || html.includes('detail-grid') || html.includes('raw-config') || html.includes('legacy')) {
                        failures.push(`${route}: legacy settings markup leaked`);
                      }
                      const routeUrls = context.__smokeRequestedUrls();
                      for (const expected of ['/api/webadmin/settings','/api/status','/api/webadmin/write/capabilities']) {
                        if (!routeUrls.includes(expected)) failures.push(`${route}: did not execute expected settings API path ${expected}`);
                      }
                    }
                    if (detailRoutes.has(route)) {
                      if (!html.includes('wa-detail-shell') || !html.includes('data-detail-tabs="true"') || !html.includes('data-responsive-tabs="true"') || !html.includes('wa-tabs-scroll') || !html.includes('wa-detail-first-row') || !html.includes('data-detail-layout="fixed-two-column"') || !html.includes('data-detail-two-column="true"') || !html.includes('data-detail-column="left"') || !html.includes('data-detail-column="right"') || !html.includes('data-detail-bottom-full-width="true"')) {
                        failures.push(`${route}: missing fixed two-column detail shell or bottom full-width stack`);
                      }
                      if (!html.includes('data-collapsible-detail="true"') || !html.includes('data-advanced-open="false"')) {
                        failures.push(`${route}: missing collapsed advanced detail card`);
                      }
                      const mainIndex = html.indexOf('data-detail-row="main"');
                      const bottomIndex = html.indexOf('data-detail-bottom-full-width="true"');
                      const advancedIndex = html.indexOf('data-detail-bottom-card="advanced"');
                      if (!(mainIndex >= 0 && bottomIndex > mainIndex && advancedIndex > bottomIndex)) {
                        failures.push(`${route}: advanced detail is not in bottom full-width stack after normal columns`);
                      }
                      if (html.includes('wa-table') && !html.includes('data-table-row-stretch="false"')) {
                        failures.push(`${route}: detail table missing no-row-stretch marker`);
                      }
                      if (html.includes('data-detail-adaptive-grid="true"') || html.includes('wa-detail-adaptive-grid') || html.includes('data-responsive-card-grid="true') || html.includes('raw-config') || html.includes('detail-grid') || html.includes('config-section') || html.includes('panel-card') || html.includes('legacy') || html.includes('wa-card-grid wa-metrics-5') || html.includes('wa-table-card')) {
                        failures.push(`${route}: legacy detail markup leaked`);
                      }
                      const routeUrls = context.__smokeRequestedUrls();
                      if (!routeUrls.includes(expectedDetailApi[route])) {
                        failures.push(`${route}: did not execute expected detail API path ${expectedDetailApi[route]}`);
                      }
                    }
                    if (responsiveListRoutes.has(route)) {
                      if (!html.includes('wa-table-scroll') && !html.includes('table-wrap')) {
                        failures.push(`${route}: missing responsive table overflow wrapper`);
                      }
                      if (route.startsWith('#/history') && !html.includes('wa-tabs-scroll')) {
                        failures.push(`${route}: missing horizontally scrollable tabs`);
                      }
                    }
                  }
                  try {
                    const conditionPayload = await context.__smokeConditionGroupSavePayload();
                    for (const item of conditionPayload.results || []) {
                      if (item.type !== item.expectedType) failures.push(`condition editor save payload type mismatch for ${item.expectedType}: ${item.type}`);
                      if (item.selectedCount !== 1 || !item.selectorMarker) failures.push(`condition editor selector single-selected marker mismatch for ${item.expectedType}: ${JSON.stringify(item)}`);
                      if (item.fellBack) failures.push(`condition editor save payload fell back to always_true for ${item.expectedType}`);
                    }
                    if ((conditionPayload.results || []).length !== 7) failures.push(`condition editor representative payload case count mismatch: ${(conditionPayload.results || []).length}`);
                  } catch (err) {
                    failures.push(`condition editor save payload smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    const saveRequest = await context.__smokeConditionGroupSaveRequest();
                    if (!saveRequest.urls.includes('POST /api/webadmin/edit-locks/acquire')) failures.push(`condition editor save request did not acquire lock: ${JSON.stringify(saveRequest.urls)}`);
                    if (!saveRequest.urls.includes('POST /api/webadmin/condition-groups')) failures.push(`condition editor save request did not POST condition group: ${JSON.stringify(saveRequest.urls)}`);
                    if (!saveRequest.hasCsrf) failures.push('condition editor save request missing CSRF header');
                    if (saveRequest.type !== 'context_equals') failures.push(`condition editor save request type mismatch: ${saveRequest.type}`);
                    if (saveRequest.field !== 'channel' || saveRequest.expected !== 'mission.start') failures.push(`condition editor save request config mismatch: ${JSON.stringify(saveRequest)}`);
                    if (saveRequest.fellBack) failures.push('condition editor save request fell back to always_true');
                  } catch (err) {
                    failures.push(`condition editor save request smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    await context.__smokeRoute('#/signals/test.channel');
                    const collapsed = String(view.innerHTML || '');
                    if (!collapsed.includes('data-advanced-open="false"')) failures.push('advanced detail: default state is not collapsed');
                    const expanded = await context.__smokeToggleAdvanced('signals','test.channel');
                    if (!expanded.includes('data-advanced-open="true"') || !expanded.includes('wa-advanced-group')) failures.push('advanced detail: expand did not render additional grouped fields');
                    await context.__smokeSilentRoute('#/signals/test.channel');
                    const refreshed = String(view.innerHTML || '');
                    if (!refreshed.includes('data-advanced-open="true"')) failures.push('advanced detail: silent refresh reset expanded state');
                    const closed = await context.__smokeToggleAdvanced('signals','test.channel');
                    if (!closed.includes('data-advanced-open="false"')) failures.push('advanced detail: collapse did not restore compact state');
                  } catch (err) {
                    failures.push(`advanced detail: ${err.name}: ${err.message}`);
                  }
                  for (const advancedCase of [
                    ['#/devices/test-device','devices','test-device'],
                    ['#/actions/test-action','actions','test-action'],
                    ['#/regions/test-region','regions','test-region'],
                    ['#/listeners/test-listener','listeners','test-listener'],
                    ['#/signal-listeners/test-listener','listeners','test-listener']
                  ]) {
                    try {
                      const [hash, kind, id] = advancedCase;
                      await context.__smokeRoute(hash);
                      let html = String(view.innerHTML || '');
                      if (!html.includes('data-advanced-open="false"')) failures.push(`advanced detail ${hash}: default state is not collapsed`);
                      html = await context.__smokeToggleAdvanced(kind,id);
                      if (!html.includes('data-advanced-open="true"')) failures.push(`advanced detail ${hash}: expand failed`);
                      await context.__smokeSilentRoute(hash);
                      html = String(view.innerHTML || '');
                      if (!html.includes('data-advanced-open="true"')) failures.push(`advanced detail ${hash}: silent refresh reset expanded state`);
                      html = await context.__smokeToggleAdvanced(kind,id);
                      if (!html.includes('data-advanced-open="false"')) failures.push(`advanced detail ${hash}: collapse failed`);
                    } catch (err) {
                      failures.push(`advanced detail ${advancedCase[0]}: ${err.name}: ${err.message}`);
                    }
                  }
                  try {
                    const modal = await context.__smokeModal();
                    if (!modal || !modal.html.includes('wa-modal') || !modal.html.includes('wa-modal-viewport') || !modal.html.includes('wa-modal-body') || !modal.html.includes('wa-modal-foot')) {
                      failures.push('modal: missing 7.5 modal markup');
                    }
                    if (!String(modal.className || '').includes('closing') || modal.closingMarker !== 'true') {
                      failures.push('modal: close did not apply fade-out closing class and marker before removal');
                    }
                    if (!modal.removed) {
                      failures.push('modal: close lifecycle did not remove modal after fade-out');
                    }
                  } catch (err) {
                    failures.push(`modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const deviceModal = await context.__smokeDeviceConfigModal();
                    if (!deviceModal.html.includes('data-unified-device-config="true"') || !deviceModal.html.includes('data-edit-section="metadata"') || !deviceModal.html.includes('data-edit-section="basic"') || !deviceModal.html.includes('data-edit-section="extended"')) {
                      failures.push('device config modal: missing unified metadata/basic/extended sections');
                    }
                    if (!deviceModal.html.includes('wa-edit-section') || !deviceModal.html.includes('wa-modal-body') || deviceModal.html.includes('raw-config')) {
                      failures.push('device config modal: missing 7.5 modal section markup or leaked raw config');
                    }
                  } catch (err) {
                    failures.push(`device config modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const vbdModal = await context.__smokeVbdDeviceConfigModal();
                    if (vbdModal.detailHtml.includes('data-type-specific-config-card="true"') || vbdModal.detailHtml.includes('<h3>类型专属配置</h3>')) {
                      failures.push('VBD detail config summary should not render legacy type-specific config card');
                    }
                    if (vbdModal.modalHtml.includes('data-edit-section="extended"') || vbdModal.modalHtml.includes('<h3>类型专属配置</h3>')) {
                      failures.push('VBD unified config modal should not render legacy type-specific config editor');
                    }
                    if (!vbdModal.detailHtml.includes('data-vbd-native-trigger-config-summary="true"') || !vbdModal.modalHtml.includes('data-vbd-native-trigger-config-modal-section="true"')) {
                      failures.push('VBD native trigger section should remain after legacy type-specific cleanup');
                    }
                    if (!vbdModal.modalHtml.includes('data-vbd-type-specific-suppressed="true"')) {
                      failures.push('VBD unified config modal should document that legacy type-specific config is split into native trigger / matcher phases');
                    }
                    if (!vbdModal.modalHtml.includes('data-vbd-native-trigger-interaction-matcher-inline-edit="true"')) {
                      failures.push('VBD matcher entry should remain inside right-click/native trigger flow');
                    }
                    if (vbdModal.urls.includes('/api/webadmin/device-extended-config/vdev-1')) {
                      failures.push('VBD unified config modal should not fetch legacy device extended config');
                    }
                  } catch (err) {
                    failures.push(`VBD device config modal cleanup: ${err.name}: ${err.message}`);
                  }
                  try {
                    const doctorPager = await context.__smokeDoctorPagination();
                    if (!doctorPager.page1.includes('data-shared-pagination-helper="true"') || !doctorPager.page1.includes('data-action="wa-pagination-page"') || !doctorPager.page1.includes('data-page-key="doctor"')) {
                      failures.push('doctor pagination: controls must render with shared delegated pagination actions');
                    }
                    if (doctorPager.page1.includes('onclick="setWaPage')) {
                      failures.push('doctor pagination: should not depend on unsafe inline pagination onclick handlers');
                    }
                    if (doctorPager.pageAfterNext !== 2 || !doctorPager.page2.includes('Doctor issue 11')) {
                      failures.push('doctor pagination: clicking page 2 should update page state and rerender page 2 results');
                    }
                    if (!doctorPager.filtersAfterPage.includes('"severity":"ALL"') || !doctorPager.filtersAfterPage.includes('"search":""')) {
                      failures.push('doctor pagination: filters/search should be preserved across pagination');
                    }
                    if (doctorPager.filteredPage !== 1) {
                      failures.push('doctor pagination: filter changes should clamp/reset page to a valid page');
                    }
                  } catch (err) {
                    failures.push(`doctor pagination smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    const debuggerDetail = await context.__smokeConditionDebuggerDetail();
                    if (!debuggerDetail.listHtml.includes('data-condition-gate-list-no-full-debug-rail="true"') || !debuggerDetail.listHtml.includes('data-condition-gate-row-click-navigates-detail="true"')) {
                      failures.push('condition debugger: list did not render full-width row navigation markers');
                    }
                    if (!debuggerDetail.detailHash.includes('#/condition-debugger/smoke-record') || !debuggerDetail.detailHash.includes('returnTo=')) {
                      failures.push(`condition debugger: row/detail navigation did not preserve returnTo: ${debuggerDetail.detailHash}`);
                    }
                    if (!debuggerDetail.detailHtml.includes('data-condition-gate-detail-full-width="true"') || !debuggerDetail.detailHtml.includes('data-condition-gate-context-summary="true"') || !debuggerDetail.detailHtml.includes('data-condition-gate-debug-tree-section="true"') || !debuggerDetail.detailHtml.includes('data-condition-gate-technical-collapsed-readonly="true"')) {
                      failures.push('condition debugger: detail page missing full-width detail sections');
                    }
                    if (!debuggerDetail.replayUrls.includes('POST /api/webadmin/condition-gates/history/smoke-record/replay') || !debuggerDetail.replayHtml.includes('data-condition-gate-replay-result="true"') || !debuggerDetail.replayHtml.includes('data-condition-gate-replay-readonly-marker="true"')) {
                      failures.push(`condition debugger: replay did not stay in detail or missed readonly result markers: ${JSON.stringify(debuggerDetail.replayUrls)}`);
                    }
                    if (!debuggerDetail.replayBackKeepsReturnTo) {
                      failures.push('condition debugger: replay rerender dropped returnTo from rendered back button');
                    }
                    if (debuggerDetail.refreshedHash !== debuggerDetail.detailHash || !debuggerDetail.refreshedHtml.includes('data-condition-gate-detail-refresh-stays-detail="true"')) {
                      failures.push('condition debugger: silent refresh changed route or dropped detail preservation marker');
                    }
                    if (debuggerDetail.transientHash !== debuggerDetail.detailHash || !debuggerDetail.transientHtml.includes('data-condition-gate-detail-refresh-stays-detail="true"') || debuggerDetail.transientHtml.includes('data-condition-gate-not-found="true"')) {
                      failures.push('condition debugger: transient silent detail failure replaced the current detail page');
                    }
                    if (!debuggerDetail.backHtml.includes('data-condition-gate-list-route="true"') || !debuggerDetail.filters.includes('"targetType":"SIGNAL_LISTENER"') || !debuggerDetail.filters.includes('"result":"BLOCKED"')) {
                      failures.push('condition debugger: back-to-list did not preserve filters/list state');
                    }
                  } catch (err) {
                    failures.push(`condition debugger detail smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    const modalSilent = await context.__smokeModalSilent();
                    if (!modalSilent.stillOpen || !modalSilent.before.includes('data-unified-device-config="true"') || !modalSilent.after.includes('data-unified-device-config="true"')) {
                      failures.push('device config modal: silent refresh closed modal or dropped unified sections');
                    }
                  } catch (err) {
                    failures.push(`device config modal silent refresh: ${err.name}: ${err.message}`);
                  }
                  try {
                    const channelSilent = await context.__smokeChannelModalSilent();
                    if (!channelSilent.stillOpen || !channelSilent.before.includes('channel-metadata-display-name') || !channelSilent.after.includes('channel-metadata-display-name')) {
                      failures.push('channel metadata modal: silent refresh closed modal or dropped fields');
                    }
                  } catch (err) {
                    failures.push(`channel metadata modal silent refresh: ${err.name}: ${err.message}`);
                  }
                  try {
                    const listenerSilent = await context.__smokeListenerModalSilent();
                    if (!listenerSilent.stillOpen || !listenerSilent.before.includes('listener-enabled-') || !listenerSilent.after.includes('listener-cooldown-')) {
                      failures.push('listener config modal: silent refresh closed modal or dropped fields');
                    }
                  } catch (err) {
                    failures.push(`listener config modal silent refresh: ${err.name}: ${err.message}`);
                  }
                  try {
                    const channelModal = await context.__smokeChannelModal();
                    if (!channelModal.html.includes('channel-metadata-display-name') || !channelModal.html.includes('wa-modal-body')) {
                      failures.push('channel metadata modal: missing real edit fields or modal shell');
                    }
                  } catch (err) {
                    failures.push(`channel metadata modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const listenerModal = await context.__smokeListenerModal();
                    if (!listenerModal.html.includes('listener-enabled-') || !listenerModal.html.includes('listener-cooldown-') || !listenerModal.html.includes('wa-modal-body')) {
                      failures.push('listener config modal: missing real edit fields or modal shell');
                    }
                  } catch (err) {
                    failures.push(`listener config modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const selectionModal = await context.__smokeCreateVirtualBlockSelectionModal();
                    if (!selectionModal.config.includes('data-selection-player-combo="true"') || !selectionModal.config.includes('data-selection-channel-combo="true"') || !selectionModal.config.includes('role="combobox"') || !selectionModal.urls.includes('/api/webadmin/online-players')) {
                      failures.push('selection modal: missing online player or channel combobox setup');
                    }
                    if (!selectionModal.waiting.includes('data-selection-wizard="virtual_block_device"') || !selectionModal.waiting.includes('等待玩家在游戏内右键方块') || !selectionModal.urls.includes('/api/webadmin/selection/start')) {
                      failures.push('selection modal: start did not render waiting state or call selection API');
                    }
                    if (!selectionModal.completedHash.includes('#/devices/vdev-1') || !selectionModal.completedHash.includes('returnTo=%23%2Fvirtual-block-devices')) {
                      failures.push('selection modal: completed realtime did not navigate with VBD returnTo');
                    }
                    if (selectionModal.completedModal.includes('查看设备详情')) {
                      failures.push('selection modal: completed state kept repeatable detail button');
                    }
                    if (selectionModal.afterDuplicateHash !== selectionModal.completedHash) {
                      failures.push('selection modal: duplicate cancelled event changed completed route');
                    }
                    if (!selectionModal.detailHtml.includes('#/virtual-block-devices') || !selectionModal.detailHtml.includes('返回上一页')) {
                      failures.push('selection detail returnTo: device detail did not preserve virtual block return target');
                    }
                    if (!selectionModal.keys.includes('virtualBlockDevices') || !selectionModal.keys.includes('deviceDetail:vdev-1')) {
                      failures.push('selection realtime: route dirty mapping missing VBD list or device detail');
                    }
                  } catch (err) {
                    failures.push(`selection modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const lifecycle = await context.__smokeLifecycleModals();
                    if (!lifecycle.listenerCreateHtml.includes('data-listener-create-modal="true"') || !lifecycle.listenerCreateHtml.includes('data-listener-create-channel-combo="true"') || !lifecycle.listenerCreateHtml.includes('role="combobox"')) {
                      failures.push('lifecycle modal: listener create missing modal or channel combobox');
                    }
                    if (!lifecycle.listenerCreateHash.includes('#/listeners/new-listener') || !lifecycle.listenerCreateHash.includes('returnTo=%23%2Flisteners') || !lifecycle.listenerCreateUrls.includes('/api/webadmin/signal-listeners')) {
                      failures.push('lifecycle modal: listener create did not call API or navigate to detail returnTo');
                    }
                    if (!lifecycle.listenerDeleteHtml.includes('data-listener-delete-modal="true"') || !lifecycle.listenerDeleteHtml.includes('data-danger-confirm-modal="true"') || !lifecycle.listenerDeleteHtml.includes('动作数量')) {
                      failures.push('lifecycle modal: listener delete missing dangerous confirm summary');
                    }
                    if (!lifecycle.listenerDeleteHash.includes('#/listeners') || !lifecycle.listenerDeleteUrls.includes('/api/webadmin/signal-listeners/test-listener/delete')) {
                      failures.push('lifecycle modal: listener delete did not call delete API or return list');
                    }
                    if (!lifecycle.vbdDeleteHtml.includes('data-vbd-delete-modal="true"') || !lifecycle.vbdDeleteHtml.includes('不 setblock、不破坏世界方块') || !lifecycle.vbdDeleteHtml.includes('data-danger-confirm-modal="true"')) {
                      failures.push('lifecycle modal: VBD delete missing unbind semantics or dangerous confirm');
                    }
                    if (!lifecycle.vbdDeleteHash.includes('#/virtual-block-devices') || !lifecycle.vbdDeleteUrls.includes('/api/webadmin/virtual-block-devices/vdev-1/delete')) {
                      failures.push('lifecycle modal: VBD delete did not call delete API or remain on VBD list');
                    }
                    if (!lifecycle.keys.includes('virtualBlockDevices') || !lifecycle.keys.includes('listenerDetail:test-listener') || !lifecycle.keys.includes('listeners')) {
                      failures.push('lifecycle realtime: route dirty mapping missing VBD/listener refresh keys');
                    }
                  } catch (err) {
                    failures.push(`lifecycle modal: ${err.name}: ${err.message}`);
                  }
                  try {
                    const realtime = context.__smokeRealtime();
                    if (realtime.listenerRef !== 'test-listener' || !realtime.listenerShould || !String(realtime.listenerKeys || '').includes('listenerDetail:test-listener')) {
                      failures.push('realtime: signal listener basic config lock event does not mark listener detail dirty');
                    }
                    if (!realtime.signalShould || !String(realtime.signalKeys || '').includes('signalDetail:test.channel')) {
                      failures.push('realtime: channel metadata event does not mark signal detail dirty');
                    }
                  } catch (err) {
                    failures.push(`realtime route smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    const realtimeRun = await context.__smokeRealtimeRun();
                    if (!realtimeRun.after.includes('wa-detail-shell') || !realtimeRun.urls.includes('/api/webadmin/signal-listener-basic-config/test-listener') || realtimeRun.pending !== 0) {
                      failures.push('realtime: handleRealtimeEvent did not complete listener detail silent refresh path');
                    }
                  } catch (err) {
                    failures.push(`realtime run smoke: ${err.name}: ${err.message}`);
                  }
                  try {
                    const p3a = await context.__smokeContainerTemplateModal();
                    const expectedOverview = '/api/webadmin/virtual-block-devices/virtual_block_device%3Aminecraft%3Aoverworld%40-11%2C-60%2C-7/container-template';
                    const expectedStart = '/api/webadmin/virtual-block-devices/virtual_block_device%3Aminecraft%3Aoverworld%40-11%2C-60%2C-7/container-template-session/start';
                    const expectedCancel = '/api/webadmin/virtual-block-devices/virtual_block_device%3Aminecraft%3Aoverworld%40-11%2C-60%2C-7/container-template-session/cancel';
                    if (!p3a.config.includes('data-container-template-session="p3b"') || !p3a.config.includes('data-container-template-save-itemconditions="true"') || !p3a.config.includes('data-action="container-template-start"') || !p3a.config.includes('data-device-id="virtual_block_device:minecraft:overworld@-11,-60,-7"')) {
                      failures.push('container template modal: missing escaped data-action/data-device-id start button for complex device id');
                    }
                    if (p3a.config.includes('onclick="startContainerTemplateSession(') || p3a.config.includes('onclick="cancelContainerTemplateSession(') || p3a.config.includes('onsubmit="event.preventDefault();startContainerTemplateSession(')) {
                      failures.push('container template modal: unsafe inline start/cancel handler remains');
                    }
                    if (!p3a.waiting.includes('data-container-template-session-status="started"')) {
                      failures.push('container template modal: start did not update modal session status');
                    }
                    if (!p3a.urls.includes(expectedOverview) || !p3a.urls.includes(expectedStart) || !p3a.urls.includes(expectedCancel)) {
                      failures.push('container template modal: complex device id did not call overview/start/cancel API with encoded route');
                    }
                    if (p3a.closeAttempt !== false || !p3a.closeConfirmOpen || !p3a.afterContinue.includes('data-container-template-session-status="started"') || !p3a.cancelConfirmOpen) {
                      failures.push('container template modal: active close/cancel should open confirm and continue editing should keep session modal active');
                    }
                    if (!p3a.afterClose.includes('data-container-template-session-status="cancelled"') || !p3a.session || p3a.session.active) {
                      failures.push('container template modal: confirmed cancel should leave a terminal cancelled status without active orphan state');
                    }
                  } catch (err) {
                    failures.push(`container template modal smoke: ${err.name}: ${err.message}`);
                  }
                  if (failures.length) {
                    console.log(failures.join('\\n'));
                    process.exit(1);
                  }
                  console.log(`render smoke ok: ${routes.length} routes`);
                })();
                """;
        return harness.replace("__APP_JS_BASE64__", encodedAppJs);
    }

    private static void testWebAdminRealtimeFoundation() throws Exception {
        for (WebAdminRealtimeEventType type : WebAdminRealtimeEventType.values()) {
            requireNotBlank(type.id(), "realtime event type id present");
            requireNotBlank(type.displayName(), "realtime event type display name present");
        }

        WebAdminRealtimeEvent event = WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_EMITTED)
                .channel("guard.channel")
                .sourceType("test")
                .severity("INFO")
                .summary("guard signal")
                .routeTarget("#/signals/guard.channel")
                .payload("result", "SUCCESS")
                .payload("passwordHash", null)
                .build("guard-1");
        String json = WebAdminJsonResponse.GSON.toJson(event);
        requireContains(json, "signal_emitted", "realtime event serializes type");
        requireContains(json, "guard.channel", "realtime event serializes channel");
        requireFalse(json.contains("passwordHash"), "realtime event omits null sensitive payload");
        requireFalse(json.contains("passwordSalt"), "realtime event omits password salt");
        requireFalse(json.contains("sessionId"), "realtime event omits session id");
        requireFalse(json.contains("cookie"), "realtime event omits cookie value");

        WebAdminRealtimeEventBus.closeAll();
        WebAdminRealtimeClient client = WebAdminRealtimeEventBus.subscribe("guard", "VIEWER");
        requireEquals(1, WebAdminRealtimeEventBus.clientCount(), "realtime client subscribed");
        WebAdminRealtimeEvent connected = client.poll(Duration.ofMillis(200));
        requireTrue(connected != null, "realtime client receives connected event");
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.HEARTBEAT)
                .summary("guard heartbeat"));
        WebAdminRealtimeEvent received = client.poll(Duration.ofMillis(200));
        requireTrue(received != null, "realtime client receives published event");
        WebAdminRealtimeEventBus.unsubscribe(client);
        requireEquals(0, WebAdminRealtimeEventBus.clientCount(), "realtime client unsubscribed");
        WebAdminRealtimeEventBus.closeAll();
    }

    private static void testWebAdminSelectionFoundation() throws Exception {
        requireEquals(WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE, WebAdminSelectionPurpose.parse("create_virtual_block_device"),
                "selection purpose parses create_virtual_block_device");
        requireEquals(null, WebAdminSelectionPurpose.parse("unsupported"), "unsupported selection purpose is rejected");

        WebAdminSelectionStartRequest valid = new WebAdminSelectionStartRequest();
        valid.purpose = "create_virtual_block_device";
        valid.targetPlayerName = "Owner";
        valid.channel = "guard.channel";
        valid.displayName = "Guard VBD";
        valid.note = "";
        valid.iconKey = "auto";
        valid.enabled = Boolean.TRUE;
        requireTrue(WebAdminSelectionService.validateStartRequest(valid).isEmpty(), "valid VBD selection start request is accepted");

        WebAdminSelectionStartRequest missingPlayer = new WebAdminSelectionStartRequest();
        missingPlayer.purpose = "create_virtual_block_device";
        missingPlayer.channel = "guard.channel";
        requireFalse(WebAdminSelectionService.validateStartRequest(missingPlayer).isEmpty(), "selection start requires target player");

        WebAdminSelectionStartRequest invalidChannel = new WebAdminSelectionStartRequest();
        invalidChannel.purpose = "create_virtual_block_device";
        invalidChannel.targetPlayerName = "Owner";
        invalidChannel.channel = "Bad Channel";
        requireFalse(WebAdminSelectionService.validateStartRequest(invalidChannel).isEmpty(), "selection start rejects invalid channel");

        WebAdminSelectionStartRequest invalidEnabled = new WebAdminSelectionStartRequest();
        invalidEnabled.purpose = "create_virtual_block_device";
        invalidEnabled.targetPlayerName = "Owner";
        invalidEnabled.channel = "guard.channel";
        invalidEnabled.enabled = "true";
        requireFalse(WebAdminSelectionService.validateStartRequest(invalidEnabled).isEmpty(), "selection start rejects non-boolean enabled");

        WebAdminPermissionService permissions = new WebAdminPermissionService();
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.START_OBJECT_SELECTION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.START_OBJECT_SELECTION, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.START_OBJECT_SELECTION, true);
        requirePermission(permissions, WebAdminRole.OWNER, WebAdminOperationType.START_OBJECT_SELECTION, true);

        for (WebAdminRealtimeEventType type : List.of(
                WebAdminRealtimeEventType.SELECTION_STARTED,
                WebAdminRealtimeEventType.SELECTION_COMPLETED,
                WebAdminRealtimeEventType.SELECTION_CANCELLED,
                WebAdminRealtimeEventType.SELECTION_FAILED
        )) {
            requireNotBlank(type.id(), "selection realtime event type id present");
            requireContains(type.id(), "selection_", "selection realtime event type has selection prefix");
        }

        Path root = Path.of("").toAbsolutePath();
        String client = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSelectionClient.java"), StandardCharsets.UTF_8);
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java"), StandardCharsets.UTF_8);
        String service = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSelectionService.java"), StandardCharsets.UTF_8);
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String mouseMixin = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/mixin/CameraModeMouseMixin.java"), StandardCharsets.UTF_8);
        String network = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSelectionPayloads.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSelectionC2SPayload.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSelectionS2CPayload.java"), StandardCharsets.UTF_8);

        for (String marker : List.of(
                "ClientPlayNetworking.registerGlobalReceiver",
                "handleKey",
                "input.isEscape()",
                "cancelFromClient(\"esc\")",
                "completeFromCrosshair",
                "shouldConsumeMouseClick",
                "shouldConsumeMouseScroll",
                "inventoryKey",
                "setScreen(null)",
                "ensureGameInputCaptured",
                "isCursorLocked",
                "lockCursor",
                "clearPressedInputs",
                "getScaledWindowWidth",
                "trimToWidth",
                "CameraModeClient.deactivate",
                "右键方块确认",
                "ESC 取消"
        )) {
            requireContains(client, marker, "client selection mode marker present: " + marker);
        }
        requireTrue(countOccurrences(client, "ensureGameInputCaptured(client)") >= 2, "client selection mode captures mouse on activate and tick");
        requireFalse(client.contains("extends Screen"), "client selection mode does not open a Screen");
        requireFalse(client.contains("setTimeout") || client.contains("timeout"), "client selection mode has no auto timeout marker");

        for (String marker : List.of(
                "Map<String, WebAdminSelectionSession>",
                "ACTIVE_BY_PLAYER",
                "currentServer",
                "cancelForDisconnect",
                "clearAll",
                "findVirtualBlockDevice",
                "MAX_SELECTION_DISTANCE_SQUARED",
                "squaredDistanceTo",
                "playerRaycastMatches",
                "TERMINAL_STATUS",
                "isChunkLoaded",
                "state.isAir()",
                "isDedicatedSignalDevice",
                "upsertVirtualBlock",
                "Formatting.GREEN",
                "SELECTION_COMPLETED",
                "SELECTION_CANCELLED",
                "SELECTION_FAILED"
        )) {
            requireContains(server, marker, "server selection lifecycle marker present: " + marker);
        }

        for (String marker : List.of(
                "START_OBJECT_SELECTION",
                "requireValidCsrf",
                "sameOrigin",
                "WebAdminWriteResult",
                "WebAdminAuditLogger.writeEvent",
                "validateStartRequest"
        )) {
            requireContains(service, marker, "selection service security marker present: " + marker);
        }
        for (String route : List.of(
                "/api/webadmin/selection/start",
                "/api/webadmin/selection/cancel",
                "/api/webadmin/selection/status",
                "/api/webadmin/online-players"
        )) {
            requireContains(webServer, route, "selection API route present: " + route);
        }
        requireContains(mouseMixin, "WebAdminSelectionClient.shouldConsumeMouseClick(click)", "selection mouse click mixin marker present");
        requireContains(mouseMixin, "WebAdminSelectionClient.shouldConsumeMouseScroll()", "selection mouse scroll mixin marker present");
        requireContains(mouseMixin, "ci.cancel()", "selection mouse mixin cancels vanilla input");
        requireContains(network, "webadmin_selection_c2s", "selection C2S payload registered");
        requireContains(network, "webadmin_selection_s2c", "selection S2C payload registered");
        requireContains(network, "playC2S().register", "selection C2S registry present");
        requireContains(network, "playS2C().register", "selection S2C registry present");
    }

    private static void testWebAdminLifecycleFoundation() throws Exception {
        WebAdminSignalListenerCreateRequest valid = new WebAdminSignalListenerCreateRequest();
        valid.name = "Guard Listener";
        valid.channel = "guard.listener";
        valid.enabled = Boolean.TRUE;
        valid.cooldownTicks = 0;
        requireTrue(WebAdminSignalListenerLifecycleService.validateCreateRequest(valid).isEmpty(),
                "valid signal listener lifecycle create request is accepted");

        WebAdminSignalListenerCreateRequest missingName = new WebAdminSignalListenerCreateRequest();
        missingName.channel = "guard.listener";
        requireFalse(WebAdminSignalListenerLifecycleService.validateCreateRequest(missingName).isEmpty(),
                "listener lifecycle create requires name/displayName");

        WebAdminSignalListenerCreateRequest invalidChannel = new WebAdminSignalListenerCreateRequest();
        invalidChannel.name = "Bad Channel Listener";
        invalidChannel.channel = "Bad Channel";
        requireFalse(WebAdminSignalListenerLifecycleService.validateCreateRequest(invalidChannel).isEmpty(),
                "listener lifecycle create rejects invalid channel");

        WebAdminSignalListenerCreateRequest invalidCooldown = new WebAdminSignalListenerCreateRequest();
        invalidCooldown.name = "Bad Cooldown";
        invalidCooldown.channel = "guard.listener";
        invalidCooldown.cooldownTicks = -1;
        requireFalse(WebAdminSignalListenerLifecycleService.validateCreateRequest(invalidCooldown).isEmpty(),
                "listener lifecycle create rejects negative cooldown");

        WebAdminPermissionService permissions = new WebAdminPermissionService();
        for (WebAdminOperationType operation : List.of(
                WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE,
                WebAdminOperationType.CREATE_SIGNAL_LISTENER,
                WebAdminOperationType.DELETE_SIGNAL_LISTENER
        )) {
            requirePermission(permissions, WebAdminRole.VIEWER, operation, false);
            requirePermission(permissions, WebAdminRole.TESTER, operation, false);
            requirePermission(permissions, WebAdminRole.EDITOR, operation, true);
            requirePermission(permissions, WebAdminRole.OWNER, operation, true);
        }

        Path root = Path.of("").toAbsolutePath();
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String vbdService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceLifecycleService.java"), StandardCharsets.UTF_8);
        String listenerService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerLifecycleService.java"), StandardCharsets.UTF_8);
        String listenerStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalListenerStore.java"), StandardCharsets.UTF_8);
        String deviceStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();

        for (String route : List.of(
                "/api/webadmin/virtual-block-devices/",
                "/api/webadmin/signal-listeners"
        )) {
            requireContains(webServer, route, "7.6 lifecycle WebAdmin API route present: " + route);
        }
        for (String marker : List.of(
                "DELETE_VIRTUAL_BLOCK_DEVICE",
                "requireValidCsrf",
                "sameOrigin",
                "DANGEROUS_OPERATION_REQUIRES_CONFIRMATION",
                "SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE",
                "SignalDeviceStore.removeById",
                "WebAdminDeviceMetadataStore.removeDevice",
                "never the world block",
                "CONFIG_CHANGED",
                "WRITE_AUDIT_APPENDED"
        )) {
            requireContains(vbdService, marker, "VBD delete lifecycle marker present: " + marker);
        }
        requireContains(deviceStore, "publishDeviceRemoved", "VBD delete store publish path remains present");
        requireFalse(vbdService.contains("setBlockState") || vbdService.contains("breakBlock") || vbdService.contains("removeBlock("),
                "VBD delete lifecycle service must not mutate world blocks");

        for (String marker : List.of(
                "CREATE_SIGNAL_LISTENER",
                "DELETE_SIGNAL_LISTENER",
                "requireValidCsrf",
                "sameOrigin",
                "DANGEROUS_OPERATION_REQUIRES_CONFIRMATION",
                "SignalListenerStore.createListener",
                "SignalListenerStore.deleteListener",
                "SignalBridgeServer.clearListenerRuntime",
                "actionsCreated",
                "actions().size()",
                "CONFIG_CHANGED",
                "WRITE_AUDIT_APPENDED"
        )) {
            requireContains(listenerService, marker, "Signal Listener lifecycle marker present: " + marker);
        }
        requireContains(listenerStore, "List.of()", "listener create default actions remain empty");
        requireContains(listenerStore, "SIGNAL_LISTENER_CHANGED", "listener store publishes listener lifecycle realtime event");

        for (String marker : List.of(
                "openVirtualBlockDeviceDeleteModal",
                "openSignalListenerCreateModal",
                "openSignalListenerDeleteModal",
                "data-vbd-delete-modal=\"true\"",
                "data-listener-create-modal=\"true\"",
                "data-listener-delete-modal=\"true\"",
                "data-listener-create-channel-combo=\"true\"",
                "data-danger-confirm-modal=\"true\"",
                "/api/webadmin/virtual-block-devices/",
                "/api/webadmin/signal-listeners",
                "lifecycleRouteWithReturn",
                "不 setblock、不破坏世界方块"
        )) {
            requireContains(js, marker, "7.6 lifecycle frontend marker present: " + marker);
        }
        requireFalse(js.contains("method:'DELETE'"), "7.6 lifecycle frontend uses existing POST write style instead of native DELETE");
        requireFalse(js.contains("saveItemSubmit") || js.contains("saveMatcher") || js.contains("saveConditionEngine") || js.contains("conditionEngineEditor"),
                "7.6 lifecycle stage does not enter matcher/itemSubmit/ConditionEngine editors");
    }

    private static void testWebAdminPhysicalDeviceActionRelayFoundation() throws Exception {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry command = actionEntry("command", "say guard");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry signal = actionEntry("signal", "guard.channel");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry message = actionEntry("message", "hello guard");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry sound = actionEntry("sound", "minecraft:entity.experience_orb.pickup");
        requireTrue(WebAdminActionRelayActionsService.validateActionEntries(List.of(command, signal, message, sound)).isEmpty(),
                "7.7 action relay accepts command/signal/message/sound action entries");

        WebAdminActionRelayActionsUpdateRequest.ActionEntry badType = actionEntry("matcher", "minecraft:diamond");
        requireFalse(WebAdminActionRelayActionsService.validateActionEntries(List.of(badType)).isEmpty(),
                "7.7 action relay rejects unsupported matcher action type");
        WebAdminActionRelayActionsUpdateRequest.ActionEntry badSignal = actionEntry("signal", "Bad Channel");
        requireFalse(WebAdminActionRelayActionsService.validateActionEntries(List.of(badSignal)).isEmpty(),
                "7.7 action relay validates signal action channel");
        for (String allowedCommand : List.of(
                "setblock 1 2 3 minecraft:air",
                "execute positioned 1 2 3 run setblock 1 2 3 minecraft:air",
                "fill 0 64 0 2 64 2 minecraft:stone",
                "clone 0 64 0 1 64 1 5 64 5",
                "place feature minecraft:oak 0 64 0",
                "function guard:world_mutation",
                "schedule function guard:world_mutation 1t",
                "scoreboard objectives add guard dummy",
                "tag PlayerName add guard",
                "title PlayerName title {\"text\":\"Guard\"}",
                "playsound minecraft:block.note_block.pling master PlayerName",
                "particle minecraft:happy_villager 0 64 0",
                "effect give PlayerName minecraft:speed 1 1",
                "give PlayerName minecraft:stone",
                "tp PlayerName 0 80 0",
                "teleport PlayerName 0 80 0",
                "say guard",
                "tellraw PlayerName {\"text\":\"guard\"}",
                "summon minecraft:marker 0 64 0",
                "clear PlayerName minecraft:stone",
                "gamemode adventure PlayerName",
                "gamerule doDaylightCycle false",
                "time set day",
                "weather clear",
                "bossbar add guard:bar \"Guard\"",
                "team add guard",
                "advancement grant PlayerName only minecraft:story/root",
                "attribute PlayerName minecraft:generic.max_health get",
                "data get entity PlayerName",
                "say ban kick op stop whitelist",
                "tellraw PlayerName {\"text\":\"op stop whitelist\"}",
                "save-all"
        )) {
            requireTrue(WebAdminActionRelayActionsService.validateActionEntries(List.of(actionEntry("command", allowedCommand))).isEmpty(),
                    "7.7 action relay allows map-control command action: " + allowedCommand);
        }
        for (String blockedCommand : List.of(
                "ban PlayerName",
                "BAN PlayerName",
                "ban-ip 127.0.0.1",
                "kick PlayerName",
                "op PlayerName",
                "deop PlayerName",
                "stop",
                "whitelist add PlayerName",
                "pardon PlayerName",
                "pardon-ip 127.0.0.1",
                "/minecraft:op PlayerName",
                "execute as PlayerName run op Someone",
                "execute positioned 0 64 0 run stop",
                "minecraft:execute as PlayerName run minecraft:deop Someone",
                "save-off",
                "save-on",
                "reload"
        )) {
            requireFalse(WebAdminActionRelayActionsService.validateActionEntries(List.of(actionEntry("command", blockedCommand))).isEmpty(),
                    "7.7 action relay rejects server management command action: " + blockedCommand);
        }
        requireEquals("world_unavailable", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(false, false, "", "tzz_mod:signal_receiver", false, false),
                "physical runtime state distinguishes unavailable world");
        requireEquals("chunk_unloaded", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, false, "", "tzz_mod:signal_receiver", false, false),
                "physical runtime state distinguishes unloaded chunk");
        requireEquals("block_missing", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "minecraft:air", "tzz_mod:signal_receiver", false, false),
                "physical runtime state distinguishes missing/air block");
        requireEquals("physical_block_mismatch", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "minecraft:stone", "tzz_mod:signal_receiver", false, false),
                "physical runtime state checks expected physical block before block entity");
        requireEquals("block_entity_missing", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "tzz_mod:signal_receiver", "tzz_mod:signal_receiver", false, false),
                "signal_receiver block with missing block entity is classified precisely");
        requireEquals("block_entity_type_mismatch", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "tzz_mod:signal_receiver", "tzz_mod:signal_receiver", true, false),
                "signal_receiver block with wrong block entity is classified as type mismatch");
        requireEquals("ready", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "tzz_mod:signal_receiver", "tzz_mod:signal_receiver", true, true),
                "signal_receiver block with matching block entity is ready for pulseTicks edits");
        requireEquals("ready", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "tzz_mod:signal_emitter", "tzz_mod:signal_emitter", true, true),
                "signal_emitter block with matching block entity is ready for physical status reporting");
        requireEquals("ready", WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(true, true, "tzz_mod:action_relay", "tzz_mod:action_relay", true, true),
                "action_relay block with matching block entity is ready for action list edits");
        SignalDeviceData fingerprintDevice = fullDevice();
        String baseFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say guard", true, false, 0, false)
        ));
        String cooldownFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say guard", true, false, 20, false)
        ));
        String notifyFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say guard", true, false, 0, true)
        ));
        String valueFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say changed", true, false, 0, false)
        ));
        String typeFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.MESSAGE, "say guard", true, false, 0, false)
        ));
        String enabledFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say guard", false, false, 0, false)
        ));
        String requiresOpFingerprint = WebAdminActionRelayActionsService.fingerprintFor(fingerprintDevice, List.of(
                new ActionConfig(ActionType.COMMAND, "say guard", true, true, 0, false)
        ));
        requireFalse(baseFingerprint.equals(cooldownFingerprint), "action relay fingerprint includes action cooldownTicks");
        requireFalse(baseFingerprint.equals(notifyFingerprint), "action relay fingerprint includes notifyOps");
        requireFalse(baseFingerprint.equals(valueFingerprint), "action relay fingerprint includes action value");
        requireFalse(baseFingerprint.equals(typeFingerprint), "action relay fingerprint includes action type");
        requireFalse(baseFingerprint.equals(enabledFingerprint), "action relay fingerprint includes action enabled state");
        requireFalse(baseFingerprint.equals(requiresOpFingerprint), "action relay fingerprint includes requiresOp");

        WebAdminPermissionService permissions = new WebAdminPermissionService();
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, true);
        requirePermission(permissions, WebAdminRole.OWNER, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, true);

        Path root = Path.of("").toAbsolutePath();
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String actionService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String extendedConfigService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminDeviceExtendedConfigService.java"), StandardCharsets.UTF_8);
        String basicConfigServiceFull = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminDeviceBasicConfigService.java"), StandardCharsets.UTF_8);
        String actionRelayCommand = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/ActionRelayCommand.java"), StandardCharsets.UTF_8);
        String actionRelayBe = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/ModBlock/entity/ActionRelayBlockEntity.java"), StandardCharsets.UTF_8);
        String basicConfigService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminDeviceBasicConfigService.java"), StandardCharsets.UTF_8);
        String editLockService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String signalDeviceStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java"), StandardCharsets.UTF_8);
        String deviceMetadataStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminDeviceMetadataStore.java"), StandardCharsets.UTF_8);
        String deviceMetadataService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminDeviceMetadataService.java"), StandardCharsets.UTF_8);
        String readonlyRoutes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/route/WebAdminReadonlyRoutes.java"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();
        String styles = WebAdminFrontendStyles.appCss();

        requireContains(webServer, "/api/webadmin/action-relay-actions/", "7.7 action relay action API route exists");
        for (String marker : List.of(
                "EDIT_ACTION_RELAY_ACTIONS",
                "TARGET_ACTION_RELAY_ACTIONS",
                "requireValidCsrf",
                "sameOrigin",
                "fingerprintMatches",
                "actionFingerprintList",
                "server_management_command_forbidden",
                "isBlockedServerManagementCommand",
                "isServerManagementRoot",
                "actionsReadable",
                "actionsEditable",
                "snapshotActionCount",
                "blockId",
                "expectedBlockId",
                "DEVICE_CONFIG_CHANGED",
                "replaceActions",
                "ActionType.COMMAND",
                "ActionType.SIGNAL",
                "case MESSAGE",
                "case SOUND",
                "CONFIG_CHANGED",
                "ACTION_CONFIG_CHANGED",
                "WRITE_AUDIT_APPENDED"
        )) {
            requireContains(actionService, marker, "7.7 action relay service marker present: " + marker);
        }
        requireFalse(actionService.contains("\"invalid_command\"") || actionService.contains("命令解析器"),
                "7.7 action relay command validation must not hard-fail ordinary map commands through parser checks");
        for (String marker : List.of(
                "editableFields",
                "fieldDisabledReasons",
                "runtimeState",
                "block_missing",
                "physical_block_mismatch",
                "block_entity_missing",
                "当前方块是",
                "data-physical-tick-field",
                "pulseTicks",
                "cooldownTicks"
        )) {
            requireContains(extendedConfigService + js, marker, "7.7 Step 2 physical device extended config full coverage marker present: " + marker);
        }
        requireContains(actionRelayBe, "replaceActions", "action relay block entity supports safe action list replacement");
        requireFalse(actionRelayCommand.contains("ActionExecutionResult result = relay.executeRelayActions(source.getWorld(), player, true);\r\n        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);")
                        || actionRelayCommand.contains("ActionExecutionResult result = relay.executeRelayActions(source.getWorld(), player, true);\n        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);"),
                "manual action relay trigger must not publish action-list config changes");
        requireFalse(actionService.contains("setBlockState") || actionService.contains("breakBlock") || actionService.contains("removeBlock("),
                "7.7 action relay action service must not mutate world blocks");
        requireContains(basicConfigServiceFull, "previousChannel", "device basic config realtime includes previous channel for old signal detail refresh");
        for (String marker : List.of(
                "TYPE_SIGNAL_EMITTER,",
                "TYPE_SIGNAL_RECEIVER,",
                "TYPE_ACTION_RELAY -> new Support(true",
                "该设备类型暂不支持 WebAdmin 基础配置编辑"
        )) {
            requireContains(basicConfigService, marker, "7.7 physical device basic config remains editable without action-list loaded gating: " + marker);
        }
        requireContains(editLockService, "action_relay_actions", "7.7 edit lock target exists for action relay actions");
        for (String marker : List.of(
                "runOnServerThread(() -> handleDeviceBasicConfig",
                "runOnServerThread(() -> handleDeviceExtendedConfig",
                "runOnServerThread(() -> handleActionRelayActions",
                "runOnServerThread(() -> handleEditLocks",
                "runOnServerThread(() -> readonlyHandled[0] = readonlyRoutes.handle",
                "minecraftServer.execute"
        )) {
            requireContains(webServer, marker, "7.7 physical device WebAdmin world access runs on server thread: " + marker);
        }
        for (String marker : List.of(
                "canonicalizeEditLockTargetId",
                "SignalDeviceStore.resolveDevice(minecraftServer, safeTargetId)",
                "WebAdminEditLockService.TARGET_DEVICE_METADATA",
                "WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG",
                "WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG",
                "WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS"
        )) {
            requireContains(webServer, marker, "7.7 typed device refs canonicalize edit-lock targets before status/acquire/release: " + marker);
        }
        for (String marker : List.of(
                "parseSourcePositionRef",
                "findBySourcePosition",
                "hasDeviceTypePrefix",
                "stripDeviceTypePrefix",
                "lastIndexOf('@')",
                "split(\",\", -1)",
                "expectedType",
                "SignalDeviceData.TYPE_SIGNAL_RECEIVER,",
                "SignalDeviceData.TYPE_SIGNAL_EMITTER",
                "SignalDeviceData.TYPE_ACTION_RELAY",
                "replaceOrAdd(state, normalized)"
        )) {
            requireContains(signalDeviceStore, marker, "7.7 physical device id lookup supports dimension@x,y,z marker: " + marker);
        }
        requireContains(js, "function deviceRouteRef", "frontend builds typed physical device route refs");
        requireContains(js, "function deviceHash(idOrDevice,type='')", "frontend centralizes device detail hash encoding");
        requireContains(js, "function deviceApiRef(id){return stripDeviceTypeRef(id);}", "typed device routes use canonical raw id for detail/config API calls");
        requireContains(js, "const canonicalEncoded=encodeURIComponent(detail.id||lookupId)", "device detail follow-up APIs use canonical raw id after typed route lookup");
        requireContains(js, "deviceHash(d,'signal_receiver')", "receiver list routes use typed signal_receiver refs");
        requireContains(js, "function currentRouteHash(){return location.hash||'#/dashboard';}", "frontend keeps hash raw and decodes only route segments");
        requireFalse(js.contains("decodeURIComponent(location.hash"), "frontend must not decode the entire hash before route parsing");
        for (String marker : List.of(
                "isPhysicalSignalDeviceType",
                "withBasicConfigForWebAdmin(existing, enabled, normalizedChannel)",
                "publishDeviceChange(WebAdminRealtimeEventType.DEVICE_CHANGED, updated)"
        )) {
            requireContains(signalDeviceStore, marker, "7.7 physical device basic config has store fallback when block entity is unavailable: " + marker);
        }
        requireContains(readonlyRoutes, "getRawPath", "readonly device route decodes raw path for encoded physical device ids");
        requireContains(readonlyRoutes, "decodePath", "readonly device route keeps plus signs safe while decoding path segments");

        for (String marker : List.of(
                "/api/webadmin/action-relay-actions/",
                "data-action-relay-more-menu-entry=\"true\"",
                "data-device-config-more-menu-entry=\"true\"",
                "data-action-relay-detail-card=\"true\"",
                "data-action-relay-config-modal-section=\"true\"",
                "data-command-action-editor",
                "data-signal-action-editor",
                "data-message-action-editor",
                "data-sound-action-editor",
                "data-action-add",
                "data-action-delete",
                "data-action-delete-confirm",
                "data-action-reorder",
                "requestDeleteActionRelayAction",
                "confirmDeleteActionRelayAction",
                "action_relay_actions",
                "动作详情当前不可读取；store 快照记录",
                "snapshotActionCount",
                "actionsReadable",
                "actionsEditable",
                "expectedFingerprint",
                "handleActionRelayActionsRealtimeEvent",
                "actionRelayRuntimeStatusHtml",
                "data-action-relay-loaded-state",
                "data-device-runtime-state",
                "extendedReadonlyReason",
                "当前类型专属配置以只读快照显示",
                "data-physical-device-delete-disabled",
                "channel-combo action-relay-channel-combo",
                "closeDeviceMoreMenu",
                "不会创建或删除真实方块"
        )) {
            requireContains(js, marker, "7.7 frontend action relay marker present: " + marker);
        }
        requireContains(js, "if(target==='action_relay_actions'){add('dashboard','signals','devices','actions','actionTemplates','doctor')",
                "7.7 config/action realtime marks only related routes for action relay actions");
        requireContains(js, "if(target==='action_relay_actions'){add('dashboard','history','signals','devices','actions','actionTemplates')",
                "7.7 write audit realtime maps action relay action changes to related routes");
        for (String marker : List.of(
                "data-floating-popover=\"device-more\"",
                "data-table-popover-portal=\"true\"",
                "ensurePopoverRoot",
                "positionDeviceMorePopover",
                "closeDeviceMoreMenu(false)",
                "data-action-list-preserve-scroll=\"true\"",
                "captureActionRelayEditorUiState",
                "restoreActionRelayEditorUiState",
                "draft.conflict=result.code==='conflict_detected'",
                "actionRelayLockHeldByOther",
                "lockHeldByOther",
                "deviceEditLocks:{}",
                "rememberDeviceEditLockEvent(data)",
                "cachedDeviceConfigLocks",
                "actionRelayLockForDevice",
                "deviceConfigLockMessage",
                "data-device-config-lock-disabled=\"true\"",
                "data-device-config-lock-badge=\"true\"",
                "openActionRelayActionsReadonlyModal",
                "data-action-relay-lock-held=\"true\"",
                "if(type==='edit_lock_changed')return",
                "eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`))",
                "addDeviceDetailRouteKeys(add,event.deviceId",
                "sameDeviceRef(event.deviceId,routeDetailId"
        )) {
            requireContains(js, marker, "7.7 Step 2 repair frontend marker present: " + marker);
        }
        requireContains(styles, ".wa-device-more-popover{display:grid;gap:4px;position:fixed",
                "7.7 more menu uses fixed floating popover instead of table-row expansion");
        requireContains(styles, ".wa-action-value-field{display:grid;gap:6px;min-width:0}",
                "7.7 action cards use stable no-overlap value field layout");
        for (String marker : List.of(
                "removeDeviceAliases",
                "metadataKey(String deviceId, String deviceType)",
                "metadataKeys(String deviceId, String deviceType)",
                "stripKnownTypePrefix"
        )) {
            requireContains(deviceMetadataStore, marker, "7.7 physical device metadata cleanup/type-aware key marker present: " + marker);
        }
        requireContains(deviceMetadataService, "metadataEntry(file, device)",
                "7.7 device metadata lookup reads type-aware primary key plus legacy raw aliases");
        requireContains(deviceMetadataService, "file.devices.remove(alias)",
                "7.7 device metadata save migrates legacy raw aliases to the type-aware primary key");
        for (String marker : List.of(
                "cleanupIfTypeChanged",
                "existingOfType(rawExisting, SignalDeviceData.TYPE_SIGNAL_RECEIVER)",
                "existing = existingOfType(existing, SignalDeviceData.TYPE_ACTION_RELAY)",
                "cleanupWebAdminMetadata(server, device.id(), device.type())",
                "WebAdminRealtimeEventBus.publishDeviceRemoved(device.id(), device.type())"
        )) {
            requireContains(signalDeviceStore, marker, "7.7 physical device replacement cleanup marker present: " + marker);
        }
        requireContains(actionService, "Validation validation = validateRequest(server, request, gateBindingValidator);",
                "7.7 action relay validates request before stale fingerprint conflict classification");
        requireContains(actionService, "affectedSignalChannels(device, beforeActions, afterActions)",
                "7.7 action relay realtime includes affected signal action channels");
        requireContains(js, "event?.payload?.previousChannel", "device basic config route mapping refreshes previous channel detail");
        requireContains(deviceMetadataService, "request.deviceId = device.id();",
                "7.7 metadata save canonicalizes typed refs before lock release and audit target handling");
        requireContains(basicConfigService, "request.deviceId = device.id();",
                "7.7 basic config save canonicalizes typed refs before lock release and channel save");
        requireContains(extendedConfigService, "request.deviceId = device.id();",
                "7.7 extended config save canonicalizes typed refs before lock release and tick save");
        requireContains(js, "body:JSON.stringify({displayName:meta.displayName,note:meta.note,iconKey:meta.iconKey,expectedVersion:meta.expectedVersion,lockId:meta.lockId})",
                "7.7 unified device config save sends displayName/note/iconKey with edit lock");
        requireContains(js, "body:JSON.stringify({enabled:basic.enabled,channel:basic.channel,expectedFingerprint:basic.expectedFingerprint,lockId:basic.lockId})",
                "7.7 unified device config save sends enabled/channel with edit lock");
        requireContains(js, "applyDeviceConfigDraftsFromForm(deviceId);showDeviceConfigEditModal(deviceId)",
                "action relay action editor rerender preserves other unified config draft inputs");
        for (String dirtyCloseMarker : List.of(
                "data-discard-confirm-modal",
                "modalDirtyChecker",
                "modalSyncBeforeClose",
                "syncBeforeClose:()=>syncModalDraftBeforeClose('device_config'",
                "dirtyCheck:()=>isDeviceConfigModalDirty(deviceId)",
                "dirtyCheck:()=>!!appState.actionRelayActionsEdit?.lockId&&modalDraftDirty('action_relay_actions'",
                "dirtyCheck:()=>modalDraftDirty('signal_listener_create'",
                "closeWebAdminModal(true,true)",
                "event.key==='Escape'){if(appState.modalDiscardConfirmOpen)",
                "normalizeActionRelayEditableAction",
                "actionRelayActionsEditableJson",
                "actionRelayActionsDirty(draft)"
        )) {
            requireContains(js, dirtyCloseMarker, "7.7 dirty edit modal close guard marker present: " + dirtyCloseMarker);
        }
        requireFalse(js.contains("openPhysicalDeviceDeleteModal"), "frontend does not expose physical signal device delete modal");
        requireFalse(js.contains("session.errors.push({message:cfg.unsupportedReason"),
                "runtime readonly physical device state is shown once in the extended status card, not duplicated as a modal error");
        requireFalse(js.contains("data-signal-receiver-action-list") || js.contains("receiverOutputTarget"),
                "signal_receiver remains redstone pulse only and does not expose action list or arbitrary output target");
        requireFalse(js.contains("saveItemSubmit") || js.contains("saveMatcher") || js.contains("saveConditionEngine") || js.contains("conditionEngineEditor"),
                "7.7 Step 1 does not enter matcher/itemSubmit/ConditionEngine editors");
        requireFalse(js.contains("raw-json-textarea") || js.contains("action-json"),
                "7.7 action relay action list does not use raw JSON textarea markers");
        requireFalse(js.contains("confirm("),
                "7.7 action relay delete confirmation uses the fixed WebAdmin modal flow instead of native browser confirm");
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry actionEntry(String type, String value) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = type;
        entry.value = value;
        entry.enabled = Boolean.TRUE;
        entry.requiresOp = Boolean.FALSE;
        entry.cooldownTicks = 0;
        entry.notifyOps = Boolean.FALSE;
        return entry;
    }

    private static void testWebAdminInteractionItemMatcherEditing() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String context = Files.readString(root.resolve("docs/WEBADMIN_INTERACTION_ITEM_MATCHER_7_8_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String matcherService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminInteractionItemMatcherService.java"), StandardCharsets.UTF_8);
        String matcherRequest = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminInteractionItemMatcherUpdateRequest.java"), StandardCharsets.UTF_8);
        String deviceStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java"), StandardCharsets.UTF_8);
        String editLockService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String writeFoundation = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminWriteFoundationService.java"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();

        for (String marker : List.of(
                "7.8 WebAdmin Interaction Item Matcher Editing",
                "virtual_block_device",
                "7.8 Step 1 当前实现内容",
                "不做 itemSubmit",
                "不做 consume",
                "不做 ConditionEngine",
                "不使用 raw JSON textarea"
        )) {
            requireContains(context, marker, "7.8 current context marker present: " + marker);
        }
        requireFalse(context.contains("尚未实现 interaction item matcher 编辑闭环"),
                "7.8 context no longer claims the matcher editing loop is unimplemented after Step 1 implementation");

        WebAdminInteractionItemMatcherUpdateRequest good = new WebAdminInteractionItemMatcherUpdateRequest();
        good.enabled = Boolean.TRUE;
        good.templateItemId = "minecraft:diamond";
        good.countMode = ContainerItemCountMode.AT_LEAST.id();
        good.requiredCount = 2;
        good.matchDamage = Boolean.FALSE;
        good.matchCustomName = Boolean.TRUE;
        good.templateCustomName = "Access Key";
        good.matchLore = Boolean.TRUE;
        good.templateLore = List.of("Line A", "Line B");
        good.interactionItemSource = InteractionItemSource.MAIN_HAND;
        good.interactionItemVanillaPolicy = InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH;
        requireTrue(WebAdminInteractionItemMatcherService.validateRequest(good).isEmpty(),
                "7.8 interaction item matcher accepts ordinary item id/count/name/lore configuration");

        WebAdminInteractionItemMatcherUpdateRequest badItem = new WebAdminInteractionItemMatcherUpdateRequest();
        badItem.enabled = Boolean.TRUE;
        badItem.templateItemId = "diamond";
        badItem.countMode = ContainerItemCountMode.AT_LEAST.id();
        badItem.requiredCount = 1;
        badItem.interactionItemSource = InteractionItemSource.MAIN_HAND;
        badItem.interactionItemVanillaPolicy = InteractionItemVanillaPolicy.ALLOW;
        requireFalse(WebAdminInteractionItemMatcherService.validateRequest(badItem).isEmpty(),
                "7.8 interaction item matcher validates namespace:path item ids");

        WebAdminInteractionItemMatcherUpdateRequest forbiddenSource = new WebAdminInteractionItemMatcherUpdateRequest();
        forbiddenSource.enabled = Boolean.TRUE;
        forbiddenSource.templateItemId = "minecraft:diamond";
        forbiddenSource.countMode = ContainerItemCountMode.AT_LEAST.id();
        forbiddenSource.requiredCount = 1;
        forbiddenSource.interactionItemSource = InteractionItemSource.INVENTORY_CONTAINS;
        forbiddenSource.interactionItemVanillaPolicy = InteractionItemVanillaPolicy.ALLOW;
        requireFalse(WebAdminInteractionItemMatcherService.validateRequest(forbiddenSource).isEmpty(),
                "7.8 interaction item matcher rejects inventory/equipment sources for this phase");

        SignalDeviceData before = fullDevice();
        ItemStackMatcherData nextMatcher = new ItemStackMatcherData(
                true,
                "minecraft:emerald",
                4,
                ContainerItemCountMode.EXACTLY.id(),
                4,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                "minecraft:emerald x4",
                100L,
                200L
        ).normalized();
        SignalDeviceData updated = SignalDeviceStore.withInteractionItemMatcherForWebAdmin(before, nextMatcher, true).normalized();
        requireEquals(before.itemSubmitEnabled(), updated.itemSubmitEnabled(), "7.8 matcher update preserves itemSubmit enabled flag");
        requireEquals(before.itemSubmitRequirements().size(), updated.itemSubmitRequirements().size(), "7.8 matcher update preserves itemSubmit requirements");
        requireEquals(before.interactChannel(), updated.interactChannel(), "7.8 matcher update preserves interaction channel");
        requireEquals(before.interactionCooldownTicks(), updated.interactionCooldownTicks(), "7.8 matcher update preserves interaction cooldown");
        requireEquals("minecraft:emerald", updated.interactionItemMatcher().templateItemId(), "7.8 matcher update changes only matcher template item");
        requireFalse(WebAdminInteractionItemMatcherService.fingerprintFor(before).equals(WebAdminInteractionItemMatcherService.fingerprintFor(updated)),
                "7.8 matcher fingerprint changes when editable matcher fields change");

        for (String marker : List.of(
                "/api/webadmin/interaction-item-matcher/",
                "handleInteractionItemMatcher",
                "WebAdminInteractionItemMatcherUpdateRequest",
                "WebAdminInteractionItemMatcherService"
        )) {
            requireContains(webServer + matcherRequest, marker, "7.8 matcher API/server marker present: " + marker);
        }
        for (String marker : List.of(
                "SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE",
                "WebAdminOperationType.EDIT_ITEM_MATCHER",
                "TARGET_INTERACTION_ITEM_MATCHER",
                "request.deviceId = device.id();",
                "unsupported_existing_matcher",
                "expectedFingerprint",
                "fingerprintFor",
                "validateRequest",
                "SignalDeviceStore.withInteractionItemMatcherForWebAdmin",
                "String templateCustomData = previous.templateCustomData();",
                "String templateComponents = previous.templateComponents();",
                "CONFIG_CHANGED",
                "DEVICE_CONFIG_CHANGED",
                "WRITE_AUDIT_APPENDED",
                "successChannel",
                "failChannel",
                "consumeEnabled()",
                "templateComponents"
        )) {
            requireContains(matcherService, marker, "7.8 matcher service marker present: " + marker);
        }
        requireContains(deviceStore, "withInteractionItemMatcherForWebAdmin", "7.8 store exposes scoped matcher update helper");
        requireContains(editLockService, "TARGET_INTERACTION_ITEM_MATCHER", "7.8 matcher edit lock target exists");
        requireContains(writeFoundation, "interactionItemMatcherWriteEnabled", "7.8 write foundation exposes matcher capability");

        for (String marker : List.of(
                "data-vbd-config-summary=\"true\"",
                "openInteractionItemMatcherModal",
                "openInteractionItemMatcherReadonlyModal",
                "data-interaction-item-matcher-modal=\"true\"",
                "data-interaction-item-matcher-config-modal-section=\"true\"",
                "data-matcher-enabled=\"true\"",
                "data-matcher-disabled-fields-collapsed=\"true\"",
                "data-matcher-enabled-item-id-shown=\"true\"",
                "data-matcher-template-item-id=\"true\"",
                "data-matcher-count-section=\"true\"",
                "data-matcher-count-mode=\"true\"",
                "data-matcher-required-count=\"true\"",
                "data-matcher-count-ignore-hides-count=\"true\"",
                "data-matcher-match-damage=\"true\"",
                "data-matcher-damage-value-row=\"true\"",
                "data-matcher-damage-value-hidden=\"true\"",
                "data-matcher-template-damage=\"true\"",
                "data-matcher-match-custom-name=\"true\"",
                "data-matcher-custom-name-value-row=\"true\"",
                "data-matcher-custom-name-value-hidden=\"true\"",
                "data-matcher-template-custom-name=\"true\"",
                "data-matcher-match-lore=\"true\"",
                "data-matcher-lore-value-row=\"true\"",
                "data-matcher-lore-value-hidden=\"true\"",
                "data-matcher-template-lore=\"true\"",
                "data-matcher-source=\"true\"",
                "data-matcher-source-readonly=\"true\"",
                "data-matcher-vanilla-policy=\"true\"",
                "data-matcher-vanilla-policy-readonly=\"true\"",
                "data-matcher-source-policy-section=\"true\"",
                "data-matcher-advanced-readonly-section=\"true\"",
                "saveInteractionItemMatcher",
                "interactionItemMatcherDirty",
                "modalSnapshot(kind,draft)",
                "syncModalDraftBeforeClose('interaction_item_matcher'",
                "interaction_item_matcher",
                "handleInteractionItemMatcherRealtimeEvent",
                "maybeReleaseInteractionItemMatcherEditForRoute",
                "data-interaction-item-matcher-no-raw-json=\"true\""
        )) {
            requireContains(js + matcherService, marker, "7.8 matcher frontend/security marker present: " + marker);
        }
        requireContains(js, "data-vbd-native-trigger-interaction-matcher-summary=\"true\"", "7.8 matcher summary is nested inside the 7.9 right-click native trigger summary");
        requireContains(js, "data-vbd-native-trigger-interaction-matcher-inline-edit=\"true\"", "unified config modal opens matcher editing from the native trigger interaction context");
        requireContains(js, "data-vbd-native-trigger-matcher-disabled-warning=\"true\"", "matcher configured while right-click trigger disabled is warned inside interaction summary");
        requireContains(js, "data-vbd-native-trigger-interaction-matcher-entry=\"true\"", "7.8 matcher entry remains available from the 7.9 right-click trigger context");
        requireContains(js, "interactionItemMatcherForm(detail,matcher,true)", "unified device config modal reuses the same conditional matcher UI inside the right-click native trigger section");
        requireContains(js, "document.getElementById('matcher-template-item-id')?.value ?? v.templateItemId", "hidden matcher fields preserve draft values when collapsed");
        requireContains(js, "document.getElementById('matcher-template-lore'))v.templateLore", "hidden matcher lore preserves draft values when collapsed");
        requireContains(js, "interactionItemMatcherPatchBody(draft)", "matcher patch body stays scoped to ordinary editable fields");
        requireContains(js, "withPreservedModalScroll", "unified config modal preserves scroll when matcher sections rerender");
        requireContains(js, "restoreModalScrollState", "modal scroll state is restored after matcher expand/collapse");
        requireContains(js, "data-matcher-toggle-preserves-scroll=\"true\"", "matcher toggle scroll preservation marker exists");
        requireFalse(js.contains("data-vbd-matcher-side-card=\"true\"") || js.contains("data-detail-side-card=\"interaction-item-matcher\"")
                        || js.contains("wa-vbd-matcher-config-card") || js.contains("data-vbd-matcher-summary-card=\"true\""),
                "VBD detail no longer renders a standalone interaction item matcher card");
        requireFalse(js.contains("successChannel:v.") || js.contains("failChannel:v.") || js.contains("consumeEnabled:v.")
                        || js.contains("templateCustomData:v.") || js.contains("templateComponents:v.")
                        || js.contains("itemSubmitRequirements:v."),
                "matcher patch body does not send preserved advanced/itemSubmit/consume fields");
        requireFalse(js.contains("itemSubmitEditor") || js.contains("saveItemSubmit") || js.contains("consumeEditor")
                        || js.contains("inventoryMatcherEditor") || js.contains("equipmentMatcherEditor")
                        || js.contains("conditionEngineEditor") || js.contains("successFailPathGraph"),
                "7.8 matcher stage does not expose itemSubmit/consume/inventory/equipment/ConditionEngine/path graph editors");
        requireFalse(js.contains("raw-json-textarea") || js.contains("matcher-json") || js.contains("data-component-json"),
                "7.8 matcher UI does not expose raw JSON/data component editors");
    }

    private static void testWebAdminVbdNativeTriggerOverview() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String context = Files.readString(root.resolve("docs/WEBADMIN_VBD_NATIVE_TRIGGER_CONFIG_7_9_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String itemSubmitContext = Files.readString(root.resolve("docs/WEBADMIN_SINGLE_ITEM_SUBMIT_7_10_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String unifiedItemSubmitContext = Files.readString(root.resolve("docs/WEBADMIN_UNIFIED_ITEM_SUBMIT_EDITOR_7_11_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String manual = Files.readString(root.resolve("docs/test/测试_7.9_WebAdmin虚拟方块设备原生触发配置P1验收.md"), StandardCharsets.UTF_8);
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String nativeTriggerService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java"), StandardCharsets.UTF_8);
        String nativeTriggerRequest = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.java"), StandardCharsets.UTF_8);
        String containerTemplateService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceContainerTemplateSessionService.java"), StandardCharsets.UTF_8);
        String containerTemplateSessions = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/container/WebAdminContainerTemplateSessions.java"), StandardCharsets.UTF_8);
        String containerTemplateClient = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminContainerTemplateClient.java"), StandardCharsets.UTF_8);
        String containerTemplateScreen = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminContainerTemplatePreviewScreen.java"), StandardCharsets.UTF_8);
        String containerTemplateServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/container/WebAdminContainerTemplateServer.java"), StandardCharsets.UTF_8);
        String containerTemplatePayloads = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminContainerTemplatePayloads.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminContainerTemplateS2CPayload.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminContainerTemplateC2SPayload.java"), StandardCharsets.UTF_8);
        String singleItemSubmitService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.java"), StandardCharsets.UTF_8);
        String singleItemSubmitSessions = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/itemsubmit/WebAdminSingleItemSubmitTemplateSessions.java"), StandardCharsets.UTF_8);
        String singleItemSubmitClient = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateClient.java"), StandardCharsets.UTF_8);
        String singleItemSubmitScreen = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateScreen.java"), StandardCharsets.UTF_8);
        String singleItemSubmitServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/itemsubmit/WebAdminSingleItemSubmitTemplateServer.java"), StandardCharsets.UTF_8);
        String singleItemSubmitPayloads = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSingleItemSubmitTemplatePayloads.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSingleItemSubmitTemplateS2CPayload.java"), StandardCharsets.UTF_8)
                + Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/network/WebAdminSingleItemSubmitTemplateC2SPayload.java"), StandardCharsets.UTF_8);
        String signalDeviceStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java"), StandardCharsets.UTF_8);
        String signalService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalService.java"), StandardCharsets.UTF_8);
        String itemConditionSupport = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/ContainerItemConditionSupport.java"), StandardCharsets.UTF_8);
        String vbdContainerHandler = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceContainerHandler.java"), StandardCharsets.UTF_8);
        String diagnosticService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/debug/VirtualBlockDeviceDiagnosticService.java"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();
        String css = WebAdminFrontendStyles.appCss();

        for (String marker : List.of(
                "7.9 WebAdmin Virtual Block Device Native Trigger Config Coverage",
                "7.9 P1",
                "7.9 P2",
                "7.9 P3",
                "红石 / 受电状态",
                "BlockState 条件",
                "`condition_enter` 只在条件从不满足变为满足时触发",
                "`condition_exit` 只在条件从满足变为不满足时触发",
                "`condition_both` 在进入和退出条件时触发",
                "保存配置不会立即触发 BlockState signal",
                "BlockState 条件没有独立 `conditionChannel` 字段",
                "玩家右键交互",
                "容器打开",
                "容器关闭",
                "容器内容变化",
                "不做 itemSubmit",
                "不做 consume",
                "不做 ConditionEngine",
                "不做成功 / 失败路径图",
                "VBD 原生触发普通配置完整编辑",
                "服务端二次校验 BlockState 属性和值",
                "virtual_block_device_triggers",
                "redstone_disabled",
                "不会误关右键交互或容器触发",
                "Container Change Template GUI",
                "P3a：Container Change Template GUI Session + GUI Skeleton",
                "P3a 不真实保存 `itemConditions`",
                "virtual_block_device_container_template:<deviceId>",
                "P3b：Ghost item editing + save",
                "玩家可以从下方背包 / 快捷栏真实拿起鼠标 cursor 物品",
                "单个 slot 模板格显示和保存的数量必须 clamp 到该物品 `ItemStack#getMaxCount`",
                "P3b 当前仅完整编辑 slot_* 模板",
                "total_* 表示“匹配整个容器中某物品 / matcher 的总数量，不对应具体槽位”",
                "保存成功后 WebUI container template modal 必须刷新 `GET /container-template` 快照",
                "P3b 保存的模板条件默认继承父 VBD 的 `containerChangeChannel`",
                "`condition.channel` 非空时作为显式覆盖",
                "`condition.channel` 为空时使用父 VBD `containerChangeChannel` 作为 effective channel",
                "`condition.channel` 和父 VBD `containerChangeChannel` 都为空时才是真正的频道缺失错误",
                "P3b GUI 不要求每个模板格单独配置 channel",
                "同一个 save / cancel / close / expired terminal 事件只向目标玩家反馈一次",
                "不在 P1 / P2 普通 Web 表单里硬做复杂物品模板",
                "`virtual_block_device` 的旧“类型专属配置”不再作为 WebUI 中的独立显示或编辑区域",
                "VBD 原生触发字段统一归入“原生触发配置”",
                "interaction item matcher 归入“玩家右键交互”的条件 / 判定层",
                "receiver / relay 的类型专属配置仍保留"
        )) {
            requireContains(context, marker, "7.9 current context marker present: " + marker);
        }

        for (String marker : List.of(
                "无手动触发方式选择器",
                "只显示 active 触发摘要",
                "右侧 detail / secondary column",
                "channel catalog 与 combobox 来源一致",
                "红石摘要",
                "BlockState 属性读取",
                "容器 open / close / change 摘要",
                "统一设备配置 modal",
                "不出现 itemSubmit / consume / ConditionEngine / 路径图",
                "Console / Network"
        )) {
            requireContains(manual, marker, "7.9 P1 manual acceptance marker present: " + marker);
        }
        for (String marker : List.of(
                "native trigger compact cards",
                "readonly native trigger detail modal",
                "matcher toggle preserves modal scroll",
                "详情页直接展开长篇完整字段",
                "只读详情出现保存按钮 / edit lock",
                "滚动位置跳回顶部或底部"
        )) {
            requireContains(manual, marker, "7.9 P1 compact trigger / scroll acceptance marker present: " + marker);
        }
        for (String marker : List.of(
                "interaction item matcher 必须完全隐藏，除非右键交互触发已启用或已选中",
                "matcher 必须纳入“玩家右键交互”配置区域内",
                "P1 暂时允许保留现有 matcher 编辑入口"
        )) {
            requireContains(context, marker, "7.9 P2 matcher hidden-until-interaction rule recorded: " + marker);
        }

        for (String marker : List.of(
                "/api/webadmin/virtual-block-devices/",
                "/native-triggers",
                "handleVirtualBlockDeviceNativeTriggers",
                "virtualBlockDeviceNativeTriggerService.overview",
                "virtualBlockDeviceNativeTriggerService.update",
                "method.equalsIgnoreCase(\"GET\")",
                "method.equalsIgnoreCase(\"PATCH\")",
                "该接口只支持 GET / PATCH"
        )) {
            requireContains(webServer, marker, "7.9 P2 native trigger route marker present: " + marker);
        }
        requireContains(webServer, "WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest",
                "7.9 P2 native trigger write request DTO is routed through WebAdminServer");
        for (String marker : List.of(
                "/container-template",
                "/container-template-session/start",
                "/container-template-session/status",
                "/container-template-session/cancel",
                "handleVirtualBlockDeviceContainerTemplate",
                "containerTemplateSessionService.overview",
                "containerTemplateSessionService.start",
                "containerTemplateSessionService.cancel",
                "WebAdminContainerTemplateSessionStartRequest",
                "WebAdminContainerTemplateSessionCancelRequest"
        )) {
            requireContains(webServer, marker, "7.9 P3a container template route marker present: " + marker);
        }

        for (String marker : List.of(
                "knownChannels(server, devices, listeners, regions)",
                "addActionRelayActionChannels",
                "SignalDeviceStore.getLoadedActionRelay",
                "countSignalActionsTo(relay.actions(), channel)"
        )) {
            requireContains(signalService, marker, "signal channel catalog includes loaded action relay signal action target: " + marker);
        }

        for (String marker : List.of(
                "SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE",
                "writeApiEnabled\", true",
                "nativeTriggerWriteApiEnabled",
                "EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS",
                "WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS",
                "expectedFingerprint",
                "redstone_powered",
                "blockstate",
                "right_click",
                "container_open",
                "container_close",
                "container_change",
                "activeTriggerTypes",
                "VirtualBlockDeviceSupport.powerState",
                "currentPowered = blockStatePowered || receivedPowerLevel > 0",
                "redstone_disabled",
                "runtimeEnabled",
                "world.isChunkLoaded(pos)",
                "state.getProperties()",
                "rawProperty.getValues()",
                "BlockStateConditionParser.fromPropertiesAndValidate",
                "conditionModeUsesChannelAndOffChannelSemantics",
                "saveDoesNotEmitBlockStateSignal",
                "interaction item matcher 是右键交互之后的条件/判定层",
                "itemConditionsReadOnly",
                "templateEditorPhase",
                "7.9 P3"
        )) {
            requireContains(nativeTriggerService, marker, "7.9 P2 native trigger service marker present: " + marker);
        }
        requireTrue(nativeTriggerService.contains("fingerprintFor(device)") || nativeTriggerService.contains("fingerprintFor(device, gates)"),
                "7.9/8.6 native trigger fingerprint marker present");

        for (String marker : List.of(
                "START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION",
                "CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION",
                "WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE",
                "expectedFingerprint",
                "fingerprintFor(device)",
                "fingerprintConditions(device.itemConditions())",
                "itemConditionDtos(device.itemConditions(), device.containerChangeChannel())",
                "p3bItemConditionInheritsContainerChangeChannel",
                "perSlotChannelRequired",
                "effectiveChannel",
                "inheritsContainerChangeChannel",
                "p3bGhostEditing",
                "saveImplemented\", true",
                "dryRunGhostInteraction\", false",
                "p3bSavesItemConditions",
                "saveEnabledInP3b",
                "ghostTemplateEditingEnabled",
                "noRealInventoryTransfer",
                "targetPlayerName",
                "目标玩家不在线",
                "只支持 virtual_block_device"
        )) {
            requireContains(containerTemplateService, marker, "7.9 P3b container template service marker present: " + marker);
        }
        requireContains(Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java"), StandardCharsets.UTF_8),
                "updateVirtualItemConditionsForWebAdmin",
                "7.9 P3b saves itemConditions through a scoped WebAdmin store update");
        for (String marker : List.of(
                "effectiveChannel(ContainerItemConditionData rawCondition, String inheritedContainerChangeChannel, boolean exiting)",
                "effectiveChannelSource",
                "return inherited",
                "物品条件频道为空，且父 VBD 未配置容器内容变化频道。"
        )) {
            requireContains(itemConditionSupport, marker, "7.9 P3b itemCondition effective channel marker present: " + marker);
        }
        requireContains(vbdContainerHandler,
                "ContainerItemConditionSupport.effectiveChannel(condition, device.containerChangeChannel(), exiting)",
                "7.9 P3b runtime itemConditions inherit containerChangeChannel when condition channel is empty");
        for (String marker : List.of(
                "ContainerItemConditionSupport.effectiveChannel(condition, device.containerChangeChannel(), false)",
                "没有显式频道，且父 VBD 未配置容器内容变化频道",
                "ContainerItemConditionSupport.validate(inventory, condition, device.containerChangeChannel())"
        )) {
            requireContains(diagnosticService, marker, "7.9 P3b Doctor effective itemCondition channel marker present: " + marker);
        }

        for (String marker : List.of(
                "SESSION_TTL_MILLIS",
                "TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE",
                "CONTAINER_TEMPLATE_SESSION_STARTED",
                "CONTAINER_TEMPLATE_SESSION_OPENED",
                "CONTAINER_TEMPLATE_SESSION_SAVED",
                "CONTAINER_TEMPLATE_SESSION_CANCELLED",
                "CONTAINER_TEMPLATE_SESSION_FAILED",
                "CONTAINER_TEMPLATE_SESSION_EXPIRED",
                "sendOpen(targetPlayer, session)",
                "saveFromClient",
                "saveSession",
                "publishConfigChangedAfterSave",
                "cancelFromWebAdmin",
                "cancelFromClient",
                "cancelForDisconnect",
                "sendEnd(player, \"cancelled\"",
                "sendEnd(player, \"saved\"",
                "notifyPlayer",
                "data.put(\"sessionRef\", session.sessionId)",
                "alreadyTerminal",
                "idempotentNoOp",
                "releaseForSessionCleanup",
                "WRITE_AUDIT_APPENDED",
                "p3bSavesItemConditions"
        )) {
            requireContains(containerTemplateSessions, marker, "7.9 P3b container template session marker present: " + marker);
        }
        for (String marker : List.of(
                "WebAdminContainerTemplatePreviewScreen.fromJson",
                "sendOpened(screen.sessionId(), screen.nonce())",
                "static void sendCancel",
                "static void sendSave",
                "ClientPlayConnectionEvents.DISCONNECT",
                "saved\", \"cancelled\", \"failed\", \"expired"
        )) {
            requireContains(containerTemplateClient, marker, "7.9 P3b client session marker present: " + marker);
        }
        for (String marker : List.of(
                "extends Screen",
                "保存模板",
                "TemplateCondition.newSlotItem",
                "payloadConditions",
                "copySourceToTemplateSlot",
                "clearTemplateSlot",
                "adjustCount",
                "ctrlDown",
                "SlotActionType.PICKUP",
                "screenHandlerSlotForInventorySlot",
                "clickPlayerInventorySlot",
                "cursorStack()",
                "playerStack",
                "player.currentScreenHandler.getCursorStack",
                "stack.getMaxCount()",
                "总量模板后续支持",
                "total_* 条件按整个容器总数量匹配",
                "sendSave(sessionId, nonce, deviceId, expectedFingerprint",
                "drawItemTooltip",
                "左键复制来源物品为模板",
                "右键清空模板格",
                "Ctrl+滚轮一次调整 8",
                "data-container-template-compact-layout",
                "footerButtonsDoNotOverlapInventory",
                "compactInstructionLines",
                "footerButtonY",
                "slotCondition",
                "totalCondition",
                "WebAdminContainerTemplateClient.sendCancel",
                "button -> openCancelConfirm(\"button_cancel\")",
                "openCancelConfirm(\"esc\")",
                "cancelCancelConfirm()",
                "confirmCancelSession()",
                "requestCancel(pendingCancelReason.isBlank() ? \"confirm_cancel\"",
                "private void requestCancel",
                "sessionClosing",
                "cancelSent",
                "ItemStack.EMPTY"
        )) {
            requireContains(containerTemplateScreen, marker, "7.9 P3b GUI ghost editing marker present: " + marker);
        }
        requireContains(js, "refreshContainerTemplateSessionOverview", "7.9 P3b saved snapshot refresh marker present");
        requireContains(js, "await refreshContainerTemplateSessionOverview(draft.deviceId,true)", "7.9 P3b saved status refreshes container template snapshot");
        requireContains(js, "containerTemplateConditionChannelText", "7.9 P3b WebUI snapshot displays effective itemCondition channel");
        requireContains(js, "继承容器内容变化频道", "7.9 P3b WebUI snapshot shows inherited containerChangeChannel wording");
        requireFalse(containerTemplateClient.contains("sendMessage("),
                "7.9 P3b terminal player feedback is server-side only; client must not duplicate chat");
        int cancelStart = containerTemplateSessions.indexOf("private static WebAdminWriteResult cancelSession");
        int saveStart = containerTemplateSessions.indexOf("private static WebAdminWriteResult saveSession");
        requireTrue(cancelStart >= 0 && saveStart > cancelStart, "7.9 P3b cancel and save handlers are both present");
        requireFalse(containerTemplateSessions.substring(cancelStart, saveStart).contains("CONFIG_CHANGED")
                        || containerTemplateSessions.substring(cancelStart, saveStart).contains("config_changed"),
                "7.9 P3b container template cancel must not publish config_changed");
        requireFalse(containerTemplateScreen.contains("HandledScreen") || containerTemplateScreen.contains("extends ScreenHandler")
                        || containerTemplateScreen.contains("quickMove") || containerTemplateScreen.contains("insertItem")
                        || containerTemplateScreen.contains("dropSlot") || containerTemplateScreen.contains("onSlotClick"),
                "7.9 P3b preview screen must not expose real inventory transfer paths");
        for (String marker : List.of(
                "registerGlobalReceiver(WebAdminContainerTemplateC2SPayload.ID",
                "ServerPlayConnectionEvents.DISCONNECT",
                "openedFromClient",
                "saveFromClient",
                "cancelFromClient"
        )) {
            requireContains(containerTemplateServer, marker, "7.9 P3b server payload marker present: " + marker);
        }
        for (String marker : List.of(
                "webadmin_container_template_s2c",
                "webadmin_container_template_c2s",
                "PayloadTypeRegistry.playC2S()",
                "PayloadTypeRegistry.playS2C()"
        )) {
            requireContains(containerTemplatePayloads, marker, "7.9 P3a payload marker present: " + marker);
        }

        for (String marker : List.of(
                "7.10 WebAdmin Single ItemSubmit Template Editing",
                "itemSubmit 不是新的触发源",
                "只有启用右键交互后才显示 itemSubmit",
                "7.10 只支持单个 itemSubmit requirement",
                "当前已有多个 requirements：7.10 不覆盖",
                "原版交互策略",
                "`itemSubmitEnabled` 启用 / 禁用",
                "`at_least`、`exactly`、`at_most`、`ignore`",
                "`matchDamage`、`matchCustomName`、`matchLore`、`matchCustomData`、`matchComponents`",
                "`itemSubmitConsumeEnabled`",
                "`hotbar_first`、`main_inventory_first`",
                "`consumeCount`",
                "display template 使用内部 `ItemStack` 快照持久化",
                "`matchComponents=false` 时组件只用于回显，不参与匹配",
                "`matchComponents=true` 时继续按现有 `templateComponents` / `ItemStackMatcher` 语义参与匹配",
                "不做多 requirement",
                "不做复杂 consume 策略编辑",
                "不做 inventory / equipment / armor 来源编辑",
                "不做 ConditionEngine",
                "不做成功 / 失败路径可视化",
                "不使用 raw JSON"
        )) {
            requireContains(itemSubmitContext, marker, "7.10 single itemSubmit context marker present: " + marker);
        }
        for (String marker : List.of(
                "7.11 WebAdmin Unified ItemSubmit Requirement List Editor",
                "统一 itemSubmit requirement list 编辑器",
                "7.10 的单物品编辑器语义在 7.11 中成为“1 个 requirement”的自然特例",
                "7.10 对多 requirement 的只读拒绝需要取消",
                "itemSubmit 仍然不是新触发源",
                "旧多物品 itemSubmit 数据语义",
                "VirtualBlockItemSubmitCommand",
                "ItemSubmitEvaluator",
                "ItemSubmitEvaluationResult",
                "ItemSubmitRequirementData",
                "ConsumePlan",
                "ConsumePlanner",
                "ItemSubmitInventoryAdapter",
                "SignalDeviceData",
                "VirtualBlockDeviceInteractionHandler",
                "SignalDeviceStore.updateVirtualItemSubmit",
                "all-or-nothing / staged consume",
                "部分满足不消耗",
                "全部满足才消耗",
                "0 个 requirement",
                "1 个 requirement",
                "2 个及以上 requirement",
                "单项 / 多项 UI 自适应显示规则",
                "用户明确回复“UI 验收通过，可以 checkpoint”前不得 checkpoint",
                "`webadmin.responsive_matrix`",
                "`minecraft.client_screenshot_matrix`",
                "deviceScaleFactor",
                "明显问题预检",
                "needs_user_review",
                "不使用 Minecraft GUI 坐标点击",
                "不做 raw JSON 编辑",
                "不做 ConditionEngine"
        )) {
            requireContains(unifiedItemSubmitContext, marker, "7.11 unified itemSubmit context marker present: " + marker);
        }
        for (String marker : List.of(
                "/single-item-submit",
                "/single-item-submit-session/start",
                "/single-item-submit-session/status",
                "/single-item-submit-session/cancel",
                "WebAdminSingleItemSubmitTemplateSessionStartRequest",
                "WebAdminSingleItemSubmitTemplateSessionCancelRequest"
        )) {
            requireContains(webServer, marker, "7.10 single itemSubmit WebAdmin API marker present: " + marker);
        }
        for (String marker : List.of(
                "START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION",
                "WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT",
                "expectedFingerprint",
                "fingerprintFor(device)",
                "singleRequirementDto(device)",
                "rightClickConditionLayer",
                "multiRequirementEditable",
                "unifiedRequirementListOnly",
                "unifiedItemSubmitEditor",
                "requirementListEditable",
                "multipleRequirementsEditable",
                "oldMultiRequirementReadOnlyRefusalRemoved",
                "requirementsFingerprintDto",
                "requirementDtos",
                "consumeEditor",
                "noRawJson",
                "noConditionEngine",
                "requireValidCsrf",
                "sameOrigin",
                "InteractionItemVanillaPolicy.displayName",
                "advancedMatcherEditable",
                "countModeValues",
                "consumeOrderValues",
                "vanillaPolicyValues",
                "matchItemId",
                "matchDamage",
                "matchCustomName",
                "matchLore",
                "matchCustomData",
                "matchComponents",
                "templateDisplayStack",
                "displayTemplateComponentsPreserved",
                "targetPlayerName",
                "multiRequirementReadOnly\", false",
                "singleItemSubmitOnly\", false"
        )) {
            requireContains(singleItemSubmitService, marker, "7.11 unified itemSubmit service marker present: " + marker);
        }
        requireContains(signalDeviceStore, "updateVirtualItemSubmitForWebAdmin", "7.10 saves itemSubmit through scoped WebAdmin store update");
        for (String marker : List.of(
                "WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT",
                "SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_STARTED",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_OPENED",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_SAVED",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_CANCELLED",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_FAILED",
                "SINGLE_ITEM_SUBMIT_TEMPLATE_SESSION_EXPIRED",
                "sendOpen(targetPlayer, session)",
                "saveFromClient",
                "saveSession",
                "lockService.validateLock",
                "session.lockId",
                "ItemSubmitSaveDraft",
                "ItemSubmitRequirementDraft",
                "requirementsFromDraft",
                "previousRequirement",
                "template.has(\"requirements\")",
                "itemSubmitConsumeEnabled",
                "itemSubmitConsumeOrder",
                "consumeCount",
                "InteractionItemVanillaPolicy.normalize",
                "InventoryConsumeOrder.normalize",
                "draft.matchItemId()",
                "matchDamage",
                "matchCustomName",
                "matchLore",
                "matchCustomData",
                "matchComponents",
                "templateDisplayStack",
                "updateVirtualItemSubmitForWebAdmin",
                "publishConfigChangedAfterSave",
                "cancelFromWebAdmin",
                "cancelFromClient",
                "sendEnd(player, \"cancelled\"",
                "sendEnd(player, \"saved\"",
                "alreadyTerminal",
                "idempotentNoOp"
        )) {
            requireContains(singleItemSubmitSessions, marker, "7.10 single itemSubmit session marker present: " + marker);
        }
        requireContains(singleItemSubmitService, "itemSubmitConsumeEnabled", "7.10 single itemSubmit fingerprint includes consume enabled");
        requireContains(singleItemSubmitService, "itemSubmitConsumeOrder", "7.10 single itemSubmit fingerprint includes consume order");
        requireContains(signalDeviceStore, "consumeEnabled == null ? existing.itemSubmitConsumeEnabled() : consumeEnabled", "7.10 scoped itemSubmit save preserves consume enabled unless explicitly edited");
        requireContains(signalDeviceStore, "consumeOrder == null || consumeOrder.isBlank() ? existing.itemSubmitConsumeOrder() : consumeOrder", "7.10 scoped itemSubmit save preserves consume order unless explicitly edited");
        for (String marker : List.of(
                "extends Screen",
                "保存模板",
                "提交物品模板",
                "player.currentScreenHandler.getCursorStack",
                "SlotActionType.PICKUP",
                "clickPlayerInventorySlot",
                "copyCursorToTemplate",
                "clearTemplate",
                "adjustCount",
                "ctrlDown",
                "stack.getMaxCount()",
                "drawItemTooltip",
                "copyCursorToTemplate()",
                "clearTemplate()",
                "itemSubmitEnabled",
                "requirementEnabled",
                "countMode",
                "consumeCount",
                "consumeCountFollowsCount",
                "syncConsumeCountIfFollowing",
                "syncConsumeCountToCount",
                "data-consume-count-follow-count",
                "matchItemId",
                "matchDamage",
                "matchCustomName",
                "matchLore",
                "matchCustomData",
                "matchComponents",
                "templateDisplayStack",
                "ItemStackDisplaySnapshot.encode",
                "ItemStackDisplaySnapshot.decode",
                "stackFromDisplaySnapshot",
                "data-single-item-submit-gui-no-overlap",
                "data-single-item-submit-compact-layout",
                "footerButtonsDoNotOverlapInventory",
                "compactInstructionLines",
                "data-unified-item-submit-compact-layout",
                "headerEmptyAddNonOverlap",
                "requirementConfigGridNonOverlap",
                "footerInventoryNonOverlap",
                "compactLongTextHidden",
                "layout4kNonOverlap",
                "simpleModeConfigHeadingHidden",
                "simpleModeTemplateConfigGap",
                "controlButtonsBottom",
                "maxInstructionLines",
                "footerButtonY",
                "InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH",
                "InventoryConsumeOrder.MAIN_INVENTORY_FIRST",
                "ContainerItemCountMode.AT_LEAST",
                "ContainerItemCountMode.EXACTLY",
                "ContainerItemCountMode.AT_MOST",
                "ContainerItemCountMode.IGNORE",
                "sendSave(sessionId, nonce, deviceId, expectedFingerprint",
                "WebAdminSingleItemSubmitTemplateClient.sendCancel",
                "sessionClosing",
                "cancelSent",
                "ItemStack.EMPTY"
        )) {
            requireContains(singleItemSubmitScreen, marker, "7.10 single itemSubmit GUI marker present: " + marker);
        }
        for (String marker : List.of(
                "data-unified-item-submit-editor",
                "data-requirement-list-scroll",
                "data-delete-requirement-confirm",
                "data-multiple-requirements-editable",
                "data-item-submit-adaptive-zero-one-many",
                "data-single-requirement-simplified",
                "data-single-requirement-no-list-card",
                "data-multi-requirement-controls-only-when-many",
                "multiRequirementMode()",
                "simpleRequirementMode()",
                "testBridgeAddRequirement",
                "testBridgeDeleteRequirement",
                "testBridgeSetCountMode",
                "testBridgeSetRequirementEnabled",
                "testBridgeSetMatcherOptions",
                "testBridgeSetConsume",
                "testBridgeSetGlobal",
                "SINGLE_REQUIREMENT_DELETE_DENIED",
                "toPayload()",
                "requirements",
                "requirementListSaveOrder",
                "unifiedItemSubmitEditor",
                "noRawJson",
                "noConditionEngine",
                "noNewConsumeStrategy"
        )) {
            requireContains(singleItemSubmitScreen, marker, "7.11 unified itemSubmit GUI marker present: " + marker);
        }
        requireFalse(singleItemSubmitScreen.contains("HandledScreen") || singleItemSubmitScreen.contains("extends ScreenHandler")
                        || singleItemSubmitScreen.contains("quickMove") || singleItemSubmitScreen.contains("insertItem")
                        || singleItemSubmitScreen.contains("dropSlot") || singleItemSubmitScreen.contains("onSlotClick"),
                "7.10 single itemSubmit screen must not expose real template transfer paths");
        for (String marker : List.of(
                "registerGlobalReceiver(WebAdminSingleItemSubmitTemplateC2SPayload.ID",
                "ServerPlayConnectionEvents.DISCONNECT",
                "openedFromClient",
                "saveFromClient",
                "cancelFromClient"
        )) {
            requireContains(singleItemSubmitServer, marker, "7.10 single itemSubmit server payload marker present: " + marker);
        }
        for (String marker : List.of(
                "webadmin_single_item_submit_template_s2c",
                "webadmin_single_item_submit_template_c2s",
                "PayloadTypeRegistry.playC2S()",
                "PayloadTypeRegistry.playS2C()"
        )) {
            requireContains(singleItemSubmitPayloads, marker, "7.10 single itemSubmit payload marker present: " + marker);
        }
        requireContains(singleItemSubmitClient, "WebAdminSingleItemSubmitTemplateScreen.fromJson", "7.10 client opens single itemSubmit template screen");
        requireContains(singleItemSubmitClient, "sendSave", "7.10 client sends save payload for single itemSubmit");
        requireContains(singleItemSubmitClient, "sendCancel", "7.10 client sends cancel payload for single itemSubmit");
        requireFalse(singleItemSubmitClient.contains("sendMessage("), "7.10 terminal player feedback must not be duplicated by client chat");
        for (String marker : List.of(
                "itemSubmitLayer",
                "data-unified-requirement-list-only=\"true\"",
                "singleItemSubmitInlineSummary",
                "data-single-item-submit-under-right-click=\"true\"",
                "data-single-item-submit-hidden-when-interaction-disabled=\"true\"",
                "data-single-item-submit-disabled-warning=\"true\"",
                "data-unified-item-submit-editor=\"true\"",
                "data-item-submit-requirement-list=\"true\"",
                "data-item-submit-adaptive-zero-one-many=\"true\"",
                "data-zero-requirement-add-only=\"true\"",
                "data-single-requirement-simplified=\"true\"",
                "data-multi-requirement-summary=\"true\"",
                "data-old-multi-requirement-readonly-refusal-removed=\"true\"",
                "data-single-item-submit-advanced-editable=\"true\"",
                "data-single-item-submit-consume-editor=\"true\"",
                "data-single-item-submit-vanilla-policy-existing-field=\"true\"",
                "data-single-item-submit-display-template-preserved=\"true\"",
                "data-single-item-submit-lock-target=\"virtual_block_device_single_item_submit\"",
                "singleItemSubmitResultSession",
                "deviceDetailRouteKeys(draft.deviceId,'virtual_block_device')",
                "single-item-submit-open",
                "single-item-submit-open-unified",
                "single-item-submit-start",
                "single-item-submit-cancel",
                "openSingleItemSubmitSessionModal",
                "openSingleItemSubmitSessionModalFromUnified",
                "handleSingleItemSubmitAction",
                "refreshSingleItemSubmitSessionOverview",
                "single_item_submit_template_session_started",
                "single_item_submit_template_session_saved",
                "virtual_block_device_single_item_submit"
        )) {
            requireContains(js, marker, "7.11 unified itemSubmit frontend marker present: " + marker);
        }
        requireFalse(js.contains("multiItemSubmitEditor") || js.contains("consumeItemSubmitEditor")
                        || js.contains("inventoryItemSubmitEditor") || js.contains("equipmentItemSubmitEditor")
                        || js.contains("itemSubmitConditionEngine") || js.contains("itemSubmitPathGraph")
                        || js.contains("itemSubmitRawJson"),
                "7.10 frontend must not expose multi itemSubmit, consume, inventory/equipment, ConditionEngine, path graph or raw JSON editors");
        int singleSubmitCancelStart = singleItemSubmitSessions.indexOf("private static WebAdminWriteResult cancelSession");
        int singleSubmitSaveStart = singleItemSubmitSessions.indexOf("private static WebAdminWriteResult saveSession");
        requireTrue(singleSubmitCancelStart >= 0 && singleSubmitSaveStart > singleSubmitCancelStart, "7.10 single itemSubmit cancel/save sections are locatable");
        String singleSubmitCancelBlock = singleItemSubmitSessions.substring(singleSubmitCancelStart, singleSubmitSaveStart);
        requireFalse(singleSubmitCancelBlock.contains("publishConfigChangedAfterSave") || singleSubmitCancelBlock.contains("CONFIG_CHANGED") || singleSubmitCancelBlock.contains("config_changed"),
                "7.10 single itemSubmit cancel must not publish config_changed");
        requireContains(singleItemSubmitService, "requirementsFingerprintDto", "7.11 unified itemSubmit fingerprint uses ordered runtime-free requirement DTO list");
        requireFalse(singleItemSubmitSessions.contains("multiItemSubmit") || singleItemSubmitSessions.contains("consumeItemSubmitEditor")
                        || singleItemSubmitSessions.contains("inventoryItemSubmit") || singleItemSubmitSessions.contains("equipmentItemSubmit")
                        || singleItemSubmitSessions.contains("itemSubmitConditionEngine") || singleItemSubmitSessions.contains("itemSubmitPathGraph")
                        || singleItemSubmitSessions.contains("itemSubmitRawJson"),
                "7.10 single itemSubmit session must not expose multi/consume/inventory/equipment/ConditionEngine/path/raw JSON save paths");

        for (String marker : List.of(
                "/api/webadmin/virtual-block-devices/${canonicalEncoded}/native-triggers",
                "data-vbd-native-trigger-area=\"true\"",
                "data-vbd-native-trigger-side-card=\"true\"",
                "data-detail-side-card=\"vbd-native-triggers\"",
                "redstone_powered",
                "blockstate",
                "right_click",
                "container_open",
                "container_close",
                "container_change",
                "data-vbd-native-trigger-summary-selected=\"true\"",
                "data-vbd-native-trigger-summary-active=\"true\"",
                "data-vbd-native-trigger-compact-card=\"true\"",
                "data-vbd-native-trigger-card-summary=\"true\"",
                "data-vbd-native-trigger-card-type=\"",
                "data-vbd-native-trigger-card-click=\"readonly-detail\"",
                "openVbdNativeTriggerReadonlyModal",
                "vbdNativeTriggerCompactCard",
                "vbdNativeTriggerReadonlyDetail",
                "data-vbd-native-trigger-readonly-modal=\"true\"",
                "data-vbd-native-trigger-readonly-detail=\"true\"",
                "data-vbd-native-trigger-detail-modal-body=\"true\"",
                "data-vbd-native-trigger-readonly-no-save=\"true\"",
                "data-vbd-native-trigger-readonly-no-edit-lock=\"true\"",
                "data-vbd-native-trigger-detail-no-dirty-guard=\"true\"",
                "data-vbd-native-trigger-detail-no-write-request=\"true\"",
                "data-vbd-native-trigger-compact-empty-state=\"true\"",
                "data-vbd-native-trigger-summary-data-driven=\"true\"",
                "data-vbd-native-trigger-no-manual-selector=\"true\"",
                "data-vbd-native-trigger-empty-state=\"true\"",
                "data-vbd-native-blockstate-properties-from-bound-block=\"true\"",
                "data-vbd-native-blockstate-allowed-values=\"true\"",
                "data-vbd-native-trigger-config-modal-section=\"true\"",
                "data-vbd-native-trigger-edit-modal=\"true\"",
                "data-vbd-native-trigger-patch-api=\"true\"",
                "data-vbd-native-trigger-lock-disabled=\"true\"",
                "data-vbd-native-redstone-edit=\"true\"",
                "data-vbd-native-blockstate-edit=\"true\"",
                "data-vbd-native-blockstate-property-dropdown-from-bound-block=\"true\"",
                "data-vbd-native-blockstate-allowed-values-from-property=\"true\"",
                "data-vbd-native-blockstate-trigger-channel-combo=\"true\"",
                "data-vbd-native-blockstate-trigger-channel-shares-main-channel=\"true\"",
                "data-vbd-native-blockstate-exit-channel-uses-off-channel=\"true\"",
                "data-vbd-native-blockstate-channel-unified-catalog=\"true\"",
                "data-vbd-native-blockstate-no-condition-channel=\"true\"",
                "data-custom-combobox-arrow-toggle-close=\"true\"",
                "data-custom-combobox-outside-click-close=\"true\"",
                "data-custom-combobox-escape-close=\"true\"",
                "data-custom-combobox-select-option-close=\"true\"",
                "data-custom-combobox-single-open=\"true\"",
                "data-custom-combobox-toggle-no-dirty=\"true\"",
                "closeAllCustomComboboxes",
                "data-vbd-native-interaction-edit=\"true\"",
                "data-vbd-native-trigger-matcher-hidden-when-interaction-disabled=\"true\"",
                "data-vbd-native-trigger-matcher-visible-inside-interaction=\"true\"",
                "data-vbd-native-container-open-edit=\"true\"",
                "data-vbd-native-container-close-edit=\"true\"",
                "data-vbd-native-container-change-edit=\"true\"",
                "data-vbd-native-container-common-edit=\"true\"",
                "data-vbd-native-container-itemconditions-readonly=\"true\"",
                "data-vbd-native-trigger-matcher-lock-disabled=\"true\"",
                "data-vbd-native-trigger-no-raw-json=\"true\"",
                "data-vbd-native-trigger-interaction-matcher-summary=\"true\"",
                "data-vbd-native-trigger-interaction-matcher-inline-edit=\"true\"",
                "data-vbd-native-trigger-matcher-disabled-warning=\"true\"",
                "redstoneEnabled:!!red.configured&&red.mode!=='redstone_disabled'",
                "matcherDraftHiddenWhenInteractionDisabled",
                "saveVbdNativeTrigger",
                "vbdNativeTriggerPatchBody",
                "virtual_block_device_triggers",
                "activeVbdNativeTriggerTypes",
                "vbdNativeTriggerConfigModalSection",
                "vbdBlockStatePropertyList",
                "vbdInteractionTriggerSummary",
                "/container-template",
                "/container-template-session/start",
                "/container-template-session/status",
                "/container-template-session/cancel",
                "openContainerTemplateSessionModal",
                "openContainerTemplateSessionModalFromUnified",
                "containerTemplateActionAttrs",
                "handleContainerTemplateAction",
                "[data-action^=\"container-template-\"]",
                "container-template-start",
                "container-template-cancel",
                "container-template-close",
                "requestContainerTemplateSessionCancel",
                "containerTemplateSessionId",
                "sessionRef",
                "scheduleContainerTemplateSessionStatusPoll",
                "refreshContainerTemplateSessionStatus",
                "openContainerTemplateCancelConfirm",
                "data-container-template-cancel-confirm",
                "data-container-template-confirm-esc-continues",
                "data-container-template-confirm-backdrop-continues",
                "confirmContainerTemplateSessionCancel",
                "cancelContainerTemplateCancelConfirm",
                "closeContainerTemplateSessionModal",
                "closeAfter:true",
                "WebAdmin 关闭窗口时取消容器模板会话。",
                "maybeCancelContainerTemplateSessionForRoute",
                "WebAdmin 离开页面时取消容器模板会话。",
                "if(!draft.active){draft.lockId='';draft.lock=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();cancelContainerTemplateCancelConfirm();}",
                "container-template-open",
                "container-template-open-unified",
                "data-container-template-session-entry=\"detail\"",
                "data-container-template-session-entry=\"unified-config\"",
                "data-container-template-session=\"p3b\"",
                "data-container-template-save-itemconditions=\"true\"",
                "data-container-template-real-item-safe=\"true\"",
                "data-container-template-ghost-editing=\"true\"",
                "handlePaginationAction",
                "data-shared-pagination-helper=\"true\"",
                "data-action=\"wa-pagination-page\"",
                "if(key==='doctor')renderDoctorList('')",
                "appState.uiPages.doctor=1;renderDoctorList",
                "data-container-template-lock-target=\"virtual_block_device_container_template\"",
                "data-container-template-fingerprint=\"itemConditions-only\"",
                "data-container-template-session-requires-clean-native-draft=\"true\"",
                "P3b 会在目标玩家客户端打开箱子式模板 GUI",
                "点击游戏内“保存模板”才写入 itemConditions",
                "不会修改世界容器或玩家物品",
                "container_template_session_started",
                "container_template_session_opened",
                "container_template_session_saved",
                "container_template_session_cancelled",
                "container_template_session_failed",
                "container_template_session_expired"
        )) {
            requireContains(js, marker, "7.9 P2 native trigger frontend marker present: " + marker);
        }
        for (String marker : List.of(
                ".wa-native-trigger-grid",
                ".wa-native-trigger-compact-card",
                ".wa-native-trigger-summary",
                ".wa-native-property-list",
                ".wa-container-template-session-form",
                ".wa-template-condition-pills"
        )) {
            requireContains(css, marker, "7.9 P1 native trigger responsive style marker present: " + marker);
        }
        requireFalse(js.contains("data-vbd-native-trigger-selector") || js.contains("data-vbd-native-trigger-option")
                        || js.contains("vbdNativeTriggerSelector") || js.contains("vbdNativeTriggerFilters")
                        || css.contains(".wa-native-trigger-selector") || css.contains(".wa-native-trigger-chip"),
                "7.9 P1 does not render manual native trigger selector/filter controls");
        requireFalse(js.contains("data-vbd-native-trigger-summary-active=\"false\"") || js.contains("请选择要查看的触发方式"),
                "7.9 P1 hides unconfigured native trigger cards and uses empty state instead");
        requireFalse(js.contains("data-vbd-native-trigger-summary-active=\"true\"><header") && js.contains("vbdRedstoneSummary(trigger)"),
                "VBD detail should not inline fully expanded native trigger detail fields");
        requireFalse(css.contains(".wa-vbd-matcher-config-card"),
                "7.9 P1 no longer keeps standalone interaction matcher summary card styling");
        for (String marker : List.of(
                "data-vbd-type-specific-suppressed=\"true\"",
                "isVirtualBlockDevice(detail)?''",
                "!isVirtualBlockDevice(detail)&&appState.deviceExtendedConfigEdit",
                "vbdDetail?Promise.resolve({ok:true,data:null}):settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`)",
                "isVbdDetail?Promise.resolve({ok:true,data:null}):settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`)"
        )) {
            requireContains(js, marker, "7.9 VBD legacy type-specific UI cleanup marker present: " + marker);
        }
        requireFalse(js.contains("native-trigger-json") || js.contains("data-vbd-native-trigger-save")
                        || js.contains("containerTemplateGui") || js.contains("ghostTemplateItemGui"),
                "7.9 P2 native trigger edit does not expose raw JSON or container template GUI");
        requireFalse(js.contains("onclick=\"startContainerTemplateSession(")
                        || js.contains("onclick=\"cancelContainerTemplateSession(")
                        || js.contains("onsubmit=\"event.preventDefault();startContainerTemplateSession("),
                "7.9 P3a container template modal must use data-action event delegation instead of unsafe inline JS handlers");
        String nativeTriggerRequestLegacySurface = nativeTriggerRequest.replace("itemSubmitConditionGroupId", "");
        for (String forbidden : List.of("itemSubmit", "consume", "inventory", "equipment", "armor", "itemConditions", "rawJson", "conditionEngine", "conditionChannel")) {
            requireFalse(nativeTriggerRequestLegacySurface.contains(forbidden),
                    "7.9 P2 native trigger PATCH DTO must not expose forbidden field: " + forbidden);
        }
        requireFalse(js.contains("conditionChannel"),
                "7.9 P2 BlockState UI must not invent a conditionChannel field");
        requireFalse(js.contains("itemSubmitEditor") || js.contains("consumeEditor") || js.contains("conditionEngineEditor")
                        || js.contains("scratchLikeNativeTriggerEditor") || js.contains("successFailPathGraph")
                        || js.contains("pathVisualization") || js.contains("nativeTriggerGraph") || js.contains("mindMap"),
                "7.9 P1 does not expose itemSubmit/consume/ConditionEngine/Scratch-like native trigger editors");
    }

    private static void testWebAdminWriteFoundation() throws Exception {
        WebAdminPermissionService permissions = new WebAdminPermissionService();
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.TEST, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.ACQUIRE_EDIT_LOCK, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_METADATA, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_CHANNEL_METADATA, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.CREATE_SIGNAL_LISTENER, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.DELETE_SIGNAL_LISTENER, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_USER, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.TEST, true);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.ACQUIRE_EDIT_LOCK, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_METADATA, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_CHANNEL_METADATA, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.CREATE_SIGNAL_LISTENER, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.DELETE_SIGNAL_LISTENER, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_USER, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.TEST, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.ACQUIRE_EDIT_LOCK, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.RELEASE_EDIT_LOCK, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_METADATA, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_CHANNEL_METADATA, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.CREATE_SIGNAL_LISTENER, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.DELETE_SIGNAL_LISTENER, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_SIGNAL, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_REGION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_ACTION, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_ITEM_MATCHER, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_USER, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_SYSTEM_SETTINGS, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.DANGEROUS_OPERATION, false);
        for (WebAdminOperationType operation : WebAdminOperationType.values()) {
            requirePermission(permissions, WebAdminRole.OWNER, operation, true);
        }

        WebAdminDeviceMetadataUpdateRequest validMetadata = new WebAdminDeviceMetadataUpdateRequest();
        validMetadata.displayName = "";
        validMetadata.note = "";
        validMetadata.iconKey = "auto";
        requireTrue(WebAdminDeviceMetadataService.validateRequest(validMetadata).isEmpty(), "empty device metadata values are allowed");
        WebAdminDeviceMetadataUpdateRequest longNameMetadata = new WebAdminDeviceMetadataUpdateRequest();
        longNameMetadata.displayName = "x".repeat(WebAdminDeviceMetadataService.MAX_DISPLAY_NAME_LENGTH + 1);
        longNameMetadata.iconKey = "auto";
        requireFalse(WebAdminDeviceMetadataService.validateRequest(longNameMetadata).isEmpty(), "long display name is rejected");
        WebAdminDeviceMetadataUpdateRequest longNoteMetadata = new WebAdminDeviceMetadataUpdateRequest();
        longNoteMetadata.note = "x".repeat(WebAdminDeviceMetadataService.MAX_NOTE_LENGTH + 1);
        longNoteMetadata.iconKey = "auto";
        requireFalse(WebAdminDeviceMetadataService.validateRequest(longNoteMetadata).isEmpty(), "long note is rejected");
        WebAdminDeviceMetadataUpdateRequest invalidIconMetadata = new WebAdminDeviceMetadataUpdateRequest();
        invalidIconMetadata.iconKey = "https://example.invalid/icon.png";
        requireFalse(WebAdminDeviceMetadataService.validateRequest(invalidIconMetadata).isEmpty(), "external icon key is rejected");
        WebAdminDeviceMetadataUpdateRequest controlCharMetadata = new WebAdminDeviceMetadataUpdateRequest();
        controlCharMetadata.displayName = "bad\u0001name";
        controlCharMetadata.iconKey = "auto";
        requireFalse(WebAdminDeviceMetadataService.validateRequest(controlCharMetadata).isEmpty(), "control characters are rejected");
        requireTrue(WebAdminDeviceMetadataService.isAllowedIconKey("signal_emitter"), "known metadata icon key is allowed");
        requireFalse(WebAdminDeviceMetadataService.isAllowedIconKey("http_icon"), "unknown metadata icon key is rejected");

        WebAdminChannelMetadataUpdateRequest validChannelMetadata = new WebAdminChannelMetadataUpdateRequest();
        validChannelMetadata.displayName = "";
        validChannelMetadata.note = "";
        validChannelMetadata.iconKey = "auto";
        requireTrue(WebAdminChannelMetadataService.validateChannel("guard.channel", "guard.channel").isEmpty(), "valid channel metadata target is accepted");
        requireTrue(WebAdminChannelMetadataService.validateRequest(validChannelMetadata).isEmpty(), "empty channel metadata values are allowed");
        requireFalse(WebAdminChannelMetadataService.validateChannel("", "").isEmpty(), "empty channel metadata target is rejected");
        requireFalse(WebAdminChannelMetadataService.validateChannel("bad\u0001channel", "bad\u0001channel").isEmpty(), "control characters in metadata channel are rejected");
        WebAdminChannelMetadataUpdateRequest longChannelDisplayName = new WebAdminChannelMetadataUpdateRequest();
        longChannelDisplayName.displayName = "x".repeat(WebAdminChannelMetadataService.MAX_DISPLAY_NAME_LENGTH + 1);
        longChannelDisplayName.iconKey = "auto";
        requireFalse(WebAdminChannelMetadataService.validateRequest(longChannelDisplayName).isEmpty(), "long channel display name is rejected");
        WebAdminChannelMetadataUpdateRequest longChannelNote = new WebAdminChannelMetadataUpdateRequest();
        longChannelNote.note = "x".repeat(WebAdminChannelMetadataService.MAX_NOTE_LENGTH + 1);
        longChannelNote.iconKey = "auto";
        requireFalse(WebAdminChannelMetadataService.validateRequest(longChannelNote).isEmpty(), "long channel note is rejected");
        WebAdminChannelMetadataUpdateRequest invalidChannelIcon = new WebAdminChannelMetadataUpdateRequest();
        invalidChannelIcon.iconKey = "https://example.invalid/icon.png";
        requireFalse(WebAdminChannelMetadataService.validateRequest(invalidChannelIcon).isEmpty(), "external channel icon key is rejected");
        WebAdminChannelMetadataStore.MetadataEntry channelMetadata = new WebAdminChannelMetadataStore.MetadataEntry();
        channelMetadata.channel = "guard.channel";
        channelMetadata.displayName = "Guard Channel";
        channelMetadata.iconKey = "signal";
        channelMetadata.version = 1L;
        String channelMetadataFingerprint = WebAdminChannelMetadataService.fingerprintFor(channelMetadata);
        WebAdminChannelMetadataStore.MetadataEntry changedChannelMetadata = WebAdminChannelMetadataStore.MetadataEntry.normalized("guard.channel", channelMetadata);
        changedChannelMetadata.displayName = "Changed Guard Channel";
        requireFalse(channelMetadataFingerprint.equals(WebAdminChannelMetadataService.fingerprintFor(changedChannelMetadata)), "channel metadata fingerprint detects stale edits");

        WebAdminDeviceBasicConfigUpdateRequest validBasicConfig = new WebAdminDeviceBasicConfigUpdateRequest();
        validBasicConfig.enabled = Boolean.TRUE;
        validBasicConfig.channel = "guard.channel";
        requireTrue(WebAdminDeviceBasicConfigService.validateRequest(validBasicConfig).isEmpty(), "valid basic config is accepted");
        WebAdminDeviceBasicConfigUpdateRequest invalidEnabledBasicConfig = new WebAdminDeviceBasicConfigUpdateRequest();
        invalidEnabledBasicConfig.enabled = "true";
        invalidEnabledBasicConfig.channel = "guard.channel";
        requireFalse(WebAdminDeviceBasicConfigService.validateRequest(invalidEnabledBasicConfig).isEmpty(), "non-boolean enabled is rejected");
        WebAdminDeviceBasicConfigUpdateRequest emptyChannelBasicConfig = new WebAdminDeviceBasicConfigUpdateRequest();
        emptyChannelBasicConfig.enabled = Boolean.TRUE;
        emptyChannelBasicConfig.channel = "";
        requireFalse(WebAdminDeviceBasicConfigService.validateRequest(emptyChannelBasicConfig).isEmpty(), "empty primary channel is rejected in 7.2");
        WebAdminDeviceBasicConfigUpdateRequest longChannelBasicConfig = new WebAdminDeviceBasicConfigUpdateRequest();
        longChannelBasicConfig.enabled = Boolean.TRUE;
        longChannelBasicConfig.channel = "a".repeat(WebAdminDeviceBasicConfigService.MAX_CHANNEL_LENGTH + 1);
        requireFalse(WebAdminDeviceBasicConfigService.validateRequest(longChannelBasicConfig).isEmpty(), "long primary channel is rejected");
        WebAdminDeviceBasicConfigUpdateRequest controlChannelBasicConfig = new WebAdminDeviceBasicConfigUpdateRequest();
        controlChannelBasicConfig.enabled = Boolean.TRUE;
        controlChannelBasicConfig.channel = "bad\u0001channel";
        requireFalse(WebAdminDeviceBasicConfigService.validateRequest(controlChannelBasicConfig).isEmpty(), "control characters in primary channel are rejected");

        WebAdminSignalListenerBasicConfigUpdateRequest validListenerConfig = new WebAdminSignalListenerBasicConfigUpdateRequest();
        validListenerConfig.enabled = Boolean.TRUE;
        validListenerConfig.channel = "guard.listener";
        validListenerConfig.cooldownTicks = 20;
        requireTrue(WebAdminSignalListenerBasicConfigService.validateRequest(validListenerConfig).isEmpty(), "valid listener basic config is accepted");
        WebAdminSignalListenerBasicConfigUpdateRequest invalidListenerEnabled = new WebAdminSignalListenerBasicConfigUpdateRequest();
        invalidListenerEnabled.enabled = "true";
        invalidListenerEnabled.channel = "guard.listener";
        invalidListenerEnabled.cooldownTicks = 20;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(invalidListenerEnabled).isEmpty(), "non-boolean listener enabled is rejected");
        WebAdminSignalListenerBasicConfigUpdateRequest emptyListenerChannel = new WebAdminSignalListenerBasicConfigUpdateRequest();
        emptyListenerChannel.enabled = Boolean.TRUE;
        emptyListenerChannel.channel = "";
        emptyListenerChannel.cooldownTicks = 20;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(emptyListenerChannel).isEmpty(), "empty listener channel is rejected");
        WebAdminSignalListenerBasicConfigUpdateRequest longListenerChannel = new WebAdminSignalListenerBasicConfigUpdateRequest();
        longListenerChannel.enabled = Boolean.TRUE;
        longListenerChannel.channel = "a".repeat(WebAdminSignalListenerBasicConfigService.MAX_CHANNEL_LENGTH + 1);
        longListenerChannel.cooldownTicks = 20;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(longListenerChannel).isEmpty(), "long listener channel is rejected");
        WebAdminSignalListenerBasicConfigUpdateRequest negativeListenerCooldown = new WebAdminSignalListenerBasicConfigUpdateRequest();
        negativeListenerCooldown.enabled = Boolean.TRUE;
        negativeListenerCooldown.channel = "guard.listener";
        negativeListenerCooldown.cooldownTicks = -1;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(negativeListenerCooldown).isEmpty(), "negative listener cooldown is rejected");
        WebAdminSignalListenerBasicConfigUpdateRequest hugeListenerCooldown = new WebAdminSignalListenerBasicConfigUpdateRequest();
        hugeListenerCooldown.enabled = Boolean.TRUE;
        hugeListenerCooldown.channel = "guard.listener";
        hugeListenerCooldown.cooldownTicks = WebAdminSignalListenerBasicConfigService.MAX_COOLDOWN_TICKS + 1;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(hugeListenerCooldown).isEmpty(), "huge listener cooldown is rejected");
        WebAdminSignalListenerBasicConfigUpdateRequest fractionalListenerCooldown = new WebAdminSignalListenerBasicConfigUpdateRequest();
        fractionalListenerCooldown.enabled = Boolean.TRUE;
        fractionalListenerCooldown.channel = "guard.listener";
        fractionalListenerCooldown.cooldownTicks = 1.5d;
        requireFalse(WebAdminSignalListenerBasicConfigService.validateRequest(fractionalListenerCooldown).isEmpty(), "fractional listener cooldown is rejected");

        SignalListenerData listener = new SignalListenerData(
                "listener-1",
                "Guard Listener",
                "guard.listener",
                true,
                20,
                List.of(ActionConfig.command("say hello", false), ActionConfig.signal("guard.downstream", false))
        ).normalized();
        String listenerFingerprint = WebAdminSignalListenerBasicConfigService.fingerprintFor(listener);
        SignalListenerData changedListener = SignalListenerStore.withBasicConfigForWebAdmin(listener, false, "changed.listener", 40);
        requireFalse(listenerFingerprint.equals(WebAdminSignalListenerBasicConfigService.fingerprintFor(changedListener)), "listener fingerprint detects stale edits");
        requireEquals(false, changedListener.enabled(), "listener enabled updated");
        requireEquals("changed.listener", changedListener.channel(), "listener channel updated");
        requireEquals(40, changedListener.cooldownTicks(), "listener cooldown updated");
        requireEquals(listener.id(), changedListener.id(), "listener id preserved");
        requireEquals(listener.name(), changedListener.name(), "listener name preserved");
        requireEquals(listener.actions().size(), changedListener.actions().size(), "listener actions list preserved");
        requireEquals(listener.actions().get(0), changedListener.actions().get(0), "listener first action preserved");
        requireEquals(listener.actions().get(1), changedListener.actions().get(1), "listener second action preserved");

        SignalDeviceData baseConfigDevice = fullDevice();
        String baseFingerprint = WebAdminDeviceBasicConfigService.fingerprintFor(baseConfigDevice);
        requireTrue(WebAdminDeviceBasicConfigService.fingerprintMatches(baseConfigDevice, baseFingerprint), "basic config fingerprint matches current device");
        SignalDeviceData changedBasicConfig = SignalDeviceStore.withBasicConfigForWebAdmin(baseConfigDevice, false, "changed.channel");
        requireFalse(WebAdminDeviceBasicConfigService.fingerprintMatches(changedBasicConfig, baseFingerprint), "basic config fingerprint detects stale edits");
        assertInteractionMatcherPreserved(baseConfigDevice, changedBasicConfig);
        assertSubmitPreserved(baseConfigDevice, changedBasicConfig);
        assertContainerPreserved(baseConfigDevice, changedBasicConfig);
        assertItemConditionsPreserved(baseConfigDevice, changedBasicConfig);
        requireEquals("changed.channel", changedBasicConfig.channel(), "primary channel updated");
        requireEquals(false, changedBasicConfig.enabled(), "enabled updated");
        requireEquals(baseConfigDevice.offChannel(), changedBasicConfig.offChannel(), "offChannel preserved by basic config edit");
        requireEquals(baseConfigDevice.mode(), changedBasicConfig.mode(), "redstone mode preserved by basic config edit");

        WebAdminDeviceExtendedConfigUpdateRequest validExtendedConfig = new WebAdminDeviceExtendedConfigUpdateRequest();
        validExtendedConfig.interactChannel = "guard.ext.interact";
        validExtendedConfig.successChannel = "guard.ext.success";
        validExtendedConfig.failChannel = "guard.ext.fail";
        validExtendedConfig.interactionCooldownTicks = 20;
        requireTrue(WebAdminDeviceExtendedConfigService.validateRequest(validExtendedConfig, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_INTERACT_CHANNEL,
                WebAdminDeviceExtendedConfigService.FIELD_SUCCESS_CHANNEL,
                WebAdminDeviceExtendedConfigService.FIELD_FAIL_CHANNEL,
                WebAdminDeviceExtendedConfigService.FIELD_INTERACTION_COOLDOWN_TICKS
        )).isEmpty(), "valid virtual block extended config is accepted");
        WebAdminDeviceExtendedConfigUpdateRequest unsupportedPulse = new WebAdminDeviceExtendedConfigUpdateRequest();
        unsupportedPulse.pulseTicks = 4;
        requireFalse(WebAdminDeviceExtendedConfigService.validateRequest(unsupportedPulse, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_INTERACT_CHANNEL
        )).isEmpty(), "unsupported extended field is rejected");
        WebAdminDeviceExtendedConfigUpdateRequest longExtendedChannel = new WebAdminDeviceExtendedConfigUpdateRequest();
        longExtendedChannel.successChannel = "a".repeat(WebAdminDeviceExtendedConfigService.MAX_CHANNEL_LENGTH + 1);
        requireFalse(WebAdminDeviceExtendedConfigService.validateRequest(longExtendedChannel, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_SUCCESS_CHANNEL
        )).isEmpty(), "long extended channel is rejected");
        WebAdminDeviceExtendedConfigUpdateRequest controlExtendedChannel = new WebAdminDeviceExtendedConfigUpdateRequest();
        controlExtendedChannel.failChannel = "bad\u0001channel";
        requireFalse(WebAdminDeviceExtendedConfigService.validateRequest(controlExtendedChannel, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_FAIL_CHANNEL
        )).isEmpty(), "control characters in extended channel are rejected");
        WebAdminDeviceExtendedConfigUpdateRequest clearExtendedChannel = new WebAdminDeviceExtendedConfigUpdateRequest();
        clearExtendedChannel.clearSuccessChannel = Boolean.TRUE;
        requireTrue(WebAdminDeviceExtendedConfigService.validateRequest(clearExtendedChannel, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_SUCCESS_CHANNEL
        )).isEmpty(), "optional extended channel can be explicitly cleared");
        WebAdminDeviceExtendedConfigUpdateRequest validPulse = new WebAdminDeviceExtendedConfigUpdateRequest();
        validPulse.pulseTicks = 1;
        requireTrue(WebAdminDeviceExtendedConfigService.validateRequest(validPulse, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_PULSE_TICKS
        )).isEmpty(), "valid receiver pulse ticks are accepted");
        WebAdminDeviceExtendedConfigUpdateRequest invalidPulse = new WebAdminDeviceExtendedConfigUpdateRequest();
        invalidPulse.pulseTicks = 0;
        requireFalse(WebAdminDeviceExtendedConfigService.validateRequest(invalidPulse, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_PULSE_TICKS
        )).isEmpty(), "zero receiver pulse ticks are rejected");
        WebAdminDeviceExtendedConfigUpdateRequest negativeCooldown = new WebAdminDeviceExtendedConfigUpdateRequest();
        negativeCooldown.interactionCooldownTicks = -1;
        requireFalse(WebAdminDeviceExtendedConfigService.validateRequest(negativeCooldown, List.of(
                WebAdminDeviceExtendedConfigService.FIELD_INTERACTION_COOLDOWN_TICKS
        )).isEmpty(), "negative extended cooldown ticks are rejected");
        String extendedFingerprint = WebAdminDeviceExtendedConfigService.fingerprintFor(baseConfigDevice);
        requireTrue(WebAdminDeviceExtendedConfigService.fingerprintMatches(baseConfigDevice, extendedFingerprint), "extended config fingerprint matches current device");
        SignalDeviceStore.ExtendedConfigPatch extendedPatch = new SignalDeviceStore.ExtendedConfigPatch(
                "changed.interact",
                true,
                false,
                "changed.success",
                true,
                false,
                "changed.fail",
                true,
                false,
                0,
                null,
                null
        );
        SignalDeviceData changedExtendedConfig = SignalDeviceStore.withExtendedConfigForWebAdmin(baseConfigDevice, extendedPatch);
        requireFalse(WebAdminDeviceExtendedConfigService.fingerprintMatches(changedExtendedConfig, extendedFingerprint), "extended config fingerprint detects stale edits");
        requireEquals("changed.interact", changedExtendedConfig.interactChannel(), "interact channel updated by extended config edit");
        requireEquals(0, changedExtendedConfig.interactionCooldownTicks(), "interaction cooldown updated by extended config edit");
        requireEquals("changed.success", changedExtendedConfig.interactionItemMatcher().successChannel(), "success channel updated by extended config edit");
        requireEquals("changed.fail", changedExtendedConfig.interactionItemMatcher().failChannel(), "fail channel updated by extended config edit");
        requireEquals(baseConfigDevice.channel(), changedExtendedConfig.channel(), "primary channel preserved by extended config edit");
        requireEquals(baseConfigDevice.enabled(), changedExtendedConfig.enabled(), "enabled preserved by extended config edit");
        requireEquals(baseConfigDevice.interactionItemMatcher().templateItemId(), changedExtendedConfig.interactionItemMatcher().templateItemId(), "matcher template item preserved by extended config edit");
        requireEquals(baseConfigDevice.interactionItemMatcher().consumeCount(), changedExtendedConfig.interactionItemMatcher().consumeCount(), "matcher consume count preserved by extended config edit");
        assertSubmitPreserved(baseConfigDevice, changedExtendedConfig);
        assertContainerPreserved(baseConfigDevice, changedExtendedConfig);
        assertItemConditionsPreserved(baseConfigDevice, changedExtendedConfig);
        assertRedstoneAndConditionPreserved(baseConfigDevice, changedExtendedConfig);
        SignalDeviceData clearedExtendedConfig = SignalDeviceStore.withExtendedConfigForWebAdmin(baseConfigDevice, new SignalDeviceStore.ExtendedConfigPatch(
                "",
                false,
                false,
                "",
                true,
                true,
                "",
                true,
                true,
                null,
                null,
                null
        ));
        requireEquals("", clearedExtendedConfig.interactionItemMatcher().successChannel(), "success channel can be cleared explicitly");
        requireEquals("", clearedExtendedConfig.interactionItemMatcher().failChannel(), "fail channel can be cleared explicitly");
        assertSubmitPreserved(baseConfigDevice, clearedExtendedConfig);

        WebAdminWriteTarget target = new WebAdminWriteTarget("DEVICE", "device-1", "测试设备");
        WebAdminWriteResult denied = permissions.decide(WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_METADATA).asWriteResult(target);
        requireFalse(denied.success(), "permission denied write result fails");
        requireEquals(WebAdminWriteResultCode.PERMISSION_DENIED.id(), denied.code(), "permission denied code");
        requireNotBlank(denied.message(), "permission denied message is readable");

        WebAdminValidationError validationError = new WebAdminValidationError(
                "channel",
                "required",
                "频道不能为空。",
                "passwordHash=secret"
        );
        WebAdminWriteResult validation = WebAdminWriteResult.validationFailed(target, List.of(validationError));
        requireFalse(validation.success(), "validation failed result fails");
        requireEquals(1, validation.validationErrors().size(), "validation error list is present");
        requireEquals("已隐藏", validation.validationErrors().get(0).rejectedValueSummary(), "rejected sensitive value is hidden");
        WebAdminValidationError sensitiveFieldError = new WebAdminValidationError(
                "passwordHash",
                "invalid",
                "敏感字段不能提交。",
                "plain-secret-value"
        );
        requireEquals("已隐藏", sensitiveFieldError.rejectedValueSummary(), "sensitive validation field is hidden");

        WebAdminWriteResult ok = WebAdminWriteResult.ok(target, true, "配置预览通过。");
        requireTrue(ok.success(), "ok write result succeeds");
        requireTrue(ok.changed(), "ok write result carries changed flag");
        WebAdminWriteResult noChange = WebAdminWriteResult.noChange(target, "");
        requireTrue(noChange.success(), "no-change result succeeds");
        requireFalse(noChange.changed(), "no-change result carries unchanged flag");
        requireEquals(WebAdminWriteResultCode.NO_CHANGE.id(), noChange.code(), "no-change result code");
        for (WebAdminWriteResultCode code : List.of(
                WebAdminWriteResultCode.UNAUTHENTICATED,
                WebAdminWriteResultCode.CSRF_REQUIRED,
                WebAdminWriteResultCode.CSRF_INVALID,
                WebAdminWriteResultCode.TARGET_NOT_FOUND,
                WebAdminWriteResultCode.CONFLICT_DETECTED,
                WebAdminWriteResultCode.EDIT_LOCK_REQUIRED,
                WebAdminWriteResultCode.EDIT_LOCK_CONFLICT,
                WebAdminWriteResultCode.EDIT_LOCK_EXPIRED,
                WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION,
                WebAdminWriteResultCode.INTERNAL_ERROR
        )) {
            WebAdminWriteResult failed = WebAdminWriteResult.failed(code, target, "");
            requireFalse(failed.success(), "failed write result fails for " + code.id());
            requireNotBlank(failed.message(), "failed write result message present for " + code.id());
            requireFalse(failed.message().contains("Exception"), "failed write result omits stack trace");
        }
        requireTrue(WebAdminWriteResult.failed(
                WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION,
                target,
                ""
        ).requiresConfirmation(), "dangerous write result requires confirmation");
        String resultJson = WebAdminJsonResponse.GSON.toJson(Map.of(
                "denied", denied,
                "validation", validation,
                "ok", ok,
                "noChange", noChange
        ));
        requireFalse(resultJson.contains("passwordHash"), "write result omits sensitive rejected key");
        requireFalse(resultJson.contains("secret"), "write result omits sensitive rejected value");

        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminSession session = new WebAdminSession("session-hash-for-guard", "guard", WebAdminRole.OWNER.id(), 1L, 10_000L, "127.0.0.1", "guard");
        String csrfToken = security.csrfTokenFor(session);
        requireNotBlank(csrfToken, "csrf token generated");
        requireFalse(security.requireValidCsrf(session, "").success(), "missing csrf token fails");
        requireFalse(security.requireValidCsrf(session, "wrong").success(), "wrong csrf token fails");
        requireTrue(security.requireValidCsrf(session, csrfToken).success(), "correct csrf token passes");
        requireTrue(security.isSameOrigin("http://127.0.0.1:18080", "127.0.0.1", 18080), "same origin accepted");
        requireFalse(security.isSameOrigin("http://evil.example:18080", "127.0.0.1", 18080), "cross origin rejected");

        WebAdminEditLockService basicConfigLocks = new WebAdminEditLockService(permissions, security, 1_000L);
        WebAdminEditLockRequest basicLockRequest = new WebAdminEditLockRequest();
        basicLockRequest.targetType = WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG;
        basicLockRequest.targetId = baseConfigDevice.id();
        WebAdminWriteResult viewerBasicLock = basicConfigLocks.acquire(
                webAdminUser("viewer", WebAdminRole.VIEWER),
                session,
                "127.0.0.1",
                basicLockRequest,
                csrfToken,
                true
        );
        requireFalse(viewerBasicLock.success(), "viewer cannot acquire basic config edit lock");
        WebAdminUser basicEditor = webAdminUser("basic-editor", WebAdminRole.EDITOR);
        WebAdminSession basicEditorSession = new WebAdminSession("editor-session-for-basic-config", "basic-editor", WebAdminRole.EDITOR.id(), 1L, 10_000L, "127.0.0.1", "guard");
        String basicEditorCsrf = security.csrfTokenFor(basicEditorSession);
        WebAdminWriteResult editorBasicLock = basicConfigLocks.acquire(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                basicLockRequest,
                basicEditorCsrf,
                true
        );
        requireTrue(editorBasicLock.success(), "editor can acquire basic config edit lock");
        WebAdminEditLockStatusDto basicLockStatus = (WebAdminEditLockStatusDto) editorBasicLock.data().get("lock");
        String basicLockId = basicLockStatus.lockId();
        requireNotBlank(basicLockId, "basic config lock id returned");
        requireTrue(basicConfigLocks.validateLock(
                WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG,
                baseConfigDevice.id(),
                basicLockId,
                basicEditor,
                basicEditorSession
        ).success(), "basic config valid lock accepted");
        requireFalse(basicConfigLocks.validateLock(
                WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG,
                baseConfigDevice.id(),
                "wrong-lock",
                basicEditor,
                basicEditorSession
        ).success(), "wrong basic config lock rejected");
        basicLockRequest.lockId = basicLockId;
        WebAdminWriteResult basicLockRelease = basicConfigLocks.release(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                basicLockRequest,
                basicEditorCsrf,
                true
        );
        requireTrue(basicLockRelease.success(), "editor can release basic config edit lock");
        WebAdminEditLockService extendedConfigLocks = new WebAdminEditLockService(permissions, security, 1_000L);
        WebAdminEditLockRequest extendedLockRequest = new WebAdminEditLockRequest();
        extendedLockRequest.targetType = WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG;
        extendedLockRequest.targetId = baseConfigDevice.id();
        WebAdminWriteResult viewerExtendedLock = extendedConfigLocks.acquire(
                webAdminUser("viewer", WebAdminRole.VIEWER),
                session,
                "127.0.0.1",
                extendedLockRequest,
                csrfToken,
                true
        );
        requireFalse(viewerExtendedLock.success(), "viewer cannot acquire extended config edit lock");
        WebAdminWriteResult editorExtendedLock = extendedConfigLocks.acquire(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                extendedLockRequest,
                basicEditorCsrf,
                true
        );
        requireTrue(editorExtendedLock.success(), "editor can acquire extended config edit lock");
        WebAdminEditLockStatusDto extendedLockStatus = (WebAdminEditLockStatusDto) editorExtendedLock.data().get("lock");
        requireNotBlank(extendedLockStatus.lockId(), "extended config lock id returned");
        requireTrue(extendedConfigLocks.validateLock(
                WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG,
                baseConfigDevice.id(),
                extendedLockStatus.lockId(),
                basicEditor,
                basicEditorSession
        ).success(), "extended config valid lock accepted");
        requireFalse(extendedConfigLocks.validateLock(
                WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG,
                baseConfigDevice.id(),
                "wrong-lock",
                basicEditor,
                basicEditorSession
        ).success(), "wrong extended config lock rejected");
        extendedLockRequest.lockId = extendedLockStatus.lockId();
        requireTrue(extendedConfigLocks.release(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                extendedLockRequest,
                basicEditorCsrf,
                true
        ).success(), "editor can release extended config edit lock");
        WebAdminEditLockService signalEditLocks = new WebAdminEditLockService(permissions, security, 1_000L);
        WebAdminEditLockRequest channelMetadataLockRequest = new WebAdminEditLockRequest();
        channelMetadataLockRequest.targetType = WebAdminEditLockService.TARGET_CHANNEL_METADATA;
        channelMetadataLockRequest.targetId = "guard.channel";
        requireFalse(signalEditLocks.acquire(
                webAdminUser("viewer", WebAdminRole.VIEWER),
                session,
                "127.0.0.1",
                channelMetadataLockRequest,
                csrfToken,
                true
        ).success(), "viewer cannot acquire channel metadata edit lock");
        WebAdminWriteResult channelMetadataLock = signalEditLocks.acquire(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                channelMetadataLockRequest,
                basicEditorCsrf,
                true
        );
        requireTrue(channelMetadataLock.success(), "editor can acquire channel metadata edit lock");
        WebAdminEditLockStatusDto channelMetadataLockStatus = (WebAdminEditLockStatusDto) channelMetadataLock.data().get("lock");
        requireTrue(signalEditLocks.validateLock(
                WebAdminEditLockService.TARGET_CHANNEL_METADATA,
                "guard.channel",
                channelMetadataLockStatus.lockId(),
                basicEditor,
                basicEditorSession
        ).success(), "channel metadata valid lock accepted");
        channelMetadataLockRequest.lockId = channelMetadataLockStatus.lockId();
        requireTrue(signalEditLocks.release(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                channelMetadataLockRequest,
                basicEditorCsrf,
                true
        ).success(), "editor can release channel metadata edit lock");
        WebAdminEditLockRequest listenerLockRequest = new WebAdminEditLockRequest();
        listenerLockRequest.targetType = WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG;
        listenerLockRequest.targetId = "listener-1";
        WebAdminWriteResult listenerLock = signalEditLocks.acquire(
                basicEditor,
                basicEditorSession,
                "127.0.0.1",
                listenerLockRequest,
                basicEditorCsrf,
                true
        );
        requireTrue(listenerLock.success(), "editor can acquire signal listener basic config edit lock");
        WebAdminEditLockStatusDto listenerLockStatus = (WebAdminEditLockStatusDto) listenerLock.data().get("lock");
        requireTrue(signalEditLocks.validateLock(
                WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG,
                "listener-1",
                listenerLockStatus.lockId(),
                basicEditor,
                basicEditorSession
        ).success(), "signal listener valid lock accepted");
        requireTrue(security.isSameOriginOrReferer("", "http://127.0.0.1:18080/app#/settings", "127.0.0.1", 18080),
                "same referer accepted");
        requireFalse(security.isSameOriginOrReferer("", "http://evil.example/app", "127.0.0.1", 18080),
                "cross referer rejected");

        WebAdminUser editor = webAdminUser("editor", WebAdminRole.EDITOR);
        WebAdminUser viewer = webAdminUser("viewer", WebAdminRole.VIEWER);
        WebAdminUser otherEditor = webAdminUser("other", WebAdminRole.EDITOR);
        WebAdminSession editorSession = new WebAdminSession("editor-session-hash-for-guard", "editor", WebAdminRole.EDITOR.id(), 1L, 10_000L, "127.0.0.1", "guard");
        WebAdminSession otherSession = new WebAdminSession("other-session-hash-for-guard", "other", WebAdminRole.EDITOR.id(), 1L, 10_000L, "127.0.0.1", "guard");
        WebAdminSession viewerSession = new WebAdminSession("viewer-session-hash-for-guard", "viewer", WebAdminRole.VIEWER.id(), 1L, 10_000L, "127.0.0.1", "guard");
        WebAdminEditLockService locks = new WebAdminEditLockService(permissions, security, 1_000L);
        WebAdminEditLockRequest lockRequest = new WebAdminEditLockRequest();
        lockRequest.targetType = WebAdminEditLockService.TARGET_DEVICE_METADATA;
        lockRequest.targetId = "device-1";
        WebAdminWriteResult viewerAcquire = locks.acquire(viewer, viewerSession, "127.0.0.1", lockRequest, security.csrfTokenFor(viewerSession), true);
        requireFalse(viewerAcquire.success(), "viewer cannot acquire device metadata edit lock");
        WebAdminWriteResult acquired = locks.acquire(editor, editorSession, "127.0.0.1", lockRequest, security.csrfTokenFor(editorSession), true);
        requireTrue(acquired.success(), "editor can acquire device metadata edit lock");
        requireTrue(acquired.data().get("lock") instanceof WebAdminEditLockStatusDto, "lock acquire returns safe status dto");
        WebAdminEditLockStatusDto lockStatus = (WebAdminEditLockStatusDto) acquired.data().get("lock");
        requireNotBlank(lockStatus.lockId(), "holder receives lock id");
        WebAdminWriteResult otherAcquire = locks.acquire(otherEditor, otherSession, "127.0.0.1", lockRequest, security.csrfTokenFor(otherSession), true);
        requireFalse(otherAcquire.success(), "second editor cannot acquire same active lock");
        requireEquals(WebAdminWriteResultCode.EDIT_LOCK_CONFLICT.id(), otherAcquire.code(), "lock conflict result code");
        requireFalse(locks.validateLock(WebAdminEditLockService.TARGET_DEVICE_METADATA, "device-1", "", editor, editorSession).success(), "patch without lock fails");
        requireTrue(locks.validateLock(WebAdminEditLockService.TARGET_DEVICE_METADATA, "device-1", lockStatus.lockId(), editor, editorSession).success(), "holder lock validates");
        lockRequest.lockId = lockStatus.lockId();
        requireFalse(locks.release(otherEditor, otherSession, "127.0.0.1", lockRequest, security.csrfTokenFor(otherSession), true).success(), "non-holder cannot release active lock");
        requireTrue(locks.release(editor, editorSession, "127.0.0.1", lockRequest, security.csrfTokenFor(editorSession), true).success(), "holder can release active lock");
        lockRequest.lockId = "";
        WebAdminWriteResult reacquired = locks.acquire(editor, editorSession, "127.0.0.1", lockRequest, security.csrfTokenFor(editorSession), true);
        WebAdminEditLockStatusDto expiringLock = (WebAdminEditLockStatusDto) reacquired.data().get("lock");
        Thread.sleep(1_100L);
        requireFalse(locks.validateLock(WebAdminEditLockService.TARGET_DEVICE_METADATA, "device-1", expiringLock.lockId(), editor, editorSession).success(), "expired lock fails validation");
        requireTrue(WebAdminDeviceMetadataService.versionMatches(4L, 4L), "matching expectedVersion passes");
        requireFalse(WebAdminDeviceMetadataService.versionMatches(4L, 3L), "stale expectedVersion fails");

        WebAdminWriteContext writeContext = new WebAdminWriteContext(
                "owner",
                WebAdminRole.OWNER,
                "abcdefghijklmnopqrstuvwxyz",
                "127.0.0.1",
                WebAdminOperationType.EDIT_DEVICE,
                target
        );
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(writeContext),
                denied,
                Map.of("passwordHash", "secret", "passwordSalt", "salt-value", "plainPassword", "plain-value", "safeField", "before"),
                Map.of("sessionToken", "token", "cookieValue", "cookie-value", "safeField", "after")
        );
        requireNotBlank(auditEvent.auditId(), "audit id present");
        requireNotBlank(auditEvent.actorUsername(), "audit actor present");
        requireNotBlank(auditEvent.operationType(), "audit operation present");
        String auditJson = WebAdminJsonResponse.GSON.toJson(auditEvent);
        requireFalse(auditJson.contains("passwordHash"), "audit event omits password hash key");
        requireFalse(auditJson.contains("sessionToken"), "audit event omits session token key");
        requireFalse(auditJson.contains("passwordSalt"), "audit event omits password salt key");
        requireFalse(auditJson.contains("plainPassword"), "audit event omits plain password key");
        requireFalse(auditJson.contains("cookieValue"), "audit event omits cookie value key");
        requireFalse(auditJson.contains("secret"), "audit event omits sensitive value");
        requireFalse(auditJson.contains("token"), "audit event omits token value");
        requireFalse(auditJson.contains("salt-value"), "audit event omits salt value");
        requireFalse(auditJson.contains("plain-value"), "audit event omits plain password value");
        requireFalse(auditJson.contains("cookie-value"), "audit event omits cookie value");

        for (WebAdminRealtimeEventType type : List.of(
                WebAdminRealtimeEventType.CONFIG_CHANGED,
                WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED,
                WebAdminRealtimeEventType.PERMISSION_DENIED,
                WebAdminRealtimeEventType.VALIDATION_FAILED,
                WebAdminRealtimeEventType.USER_CHANGED,
                WebAdminRealtimeEventType.SYSTEM_SETTINGS_CHANGED,
                WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED,
                WebAdminRealtimeEventType.SIGNAL_CONFIG_CHANGED,
                WebAdminRealtimeEventType.CHANNEL_METADATA_CHANGED,
                WebAdminRealtimeEventType.SIGNAL_LISTENER_CONFIG_CHANGED,
                WebAdminRealtimeEventType.REGION_CONFIG_CHANGED,
                WebAdminRealtimeEventType.ACTION_CONFIG_CHANGED,
                WebAdminRealtimeEventType.EDIT_LOCK_CHANGED
        )) {
            requireNotBlank(type.id(), "write realtime event type id present");
            requireNotBlank(type.displayName(), "write realtime event display present");
        }

        WebAdminRealtimeEvent event = WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .summary("配置变更预留事件")
                .payload("passwordSalt", null)
                .build("write-guard");
        String eventJson = WebAdminJsonResponse.GSON.toJson(event);
        requireFalse(eventJson.contains("passwordSalt"), "write realtime event omits sensitive payload");

        WebAdminUser owner = new WebAdminUser();
        owner.username = "owner";
        owner.displayName = "owner";
        owner.role = WebAdminRole.OWNER.id();
        owner.passwordHash = "passwordHash-should-not-leak";
        owner.passwordSalt = "passwordSalt-should-not-leak";
        owner.normalized();
        Map<String, Object> capabilities = new WebAdminWriteFoundationService(security).capabilities(owner, session);
        String capabilitiesJson = WebAdminJsonResponse.GSON.toJson(capabilities);
        requireContains(capabilitiesJson, "metadataWriteEnabled", "capabilities describe metadata write stage");
        requireContains(capabilitiesJson, "deviceExtendedConfigWriteEnabled", "capabilities describe extended config write stage");
        requireContains(capabilitiesJson, "actionRelayActionListWriteEnabled", "capabilities describe action relay action list write stage");
        requireContains(capabilitiesJson, "channelMetadataWriteEnabled", "capabilities describe channel metadata write stage");
        requireContains(capabilitiesJson, "signalListenerBasicConfigWriteEnabled", "capabilities describe signal listener write stage");
        requireContains(capabilitiesJson, "objectSelectionEnabled", "capabilities describe object selection write stage");
        requireContains(capabilitiesJson, "virtualBlockDeviceLifecycleEnabled", "capabilities describe VBD lifecycle stage");
        requireContains(capabilitiesJson, "singleItemSubmitTemplateWriteEnabled", "capabilities describe 7.10 single itemSubmit template write stage");
        requireContains(capabilitiesJson, "signalListenerLifecycleWriteEnabled", "capabilities describe signal listener lifecycle stage");
        requireContains(capabilitiesJson, "DELETE_VIRTUAL_BLOCK_DEVICE", "capabilities expose VBD delete operation");
        requireContains(capabilitiesJson, "EDIT_ACTION_RELAY_ACTIONS", "capabilities expose action relay action list operation");
        requireContains(capabilitiesJson, "CREATE_SIGNAL_LISTENER", "capabilities expose listener create operation");
        requireContains(capabilitiesJson, "DELETE_SIGNAL_LISTENER", "capabilities expose listener delete operation");
        requireContains(capabilitiesJson, "START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION", "capabilities expose 7.10 single itemSubmit start operation");
        requireContains(capabilitiesJson, "SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT", "capabilities expose 7.10 single itemSubmit save operation");
        requireContains(capabilitiesJson, "CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION", "capabilities expose 7.10 single itemSubmit cancel operation");
        requireContains(capabilitiesJson, "FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION", "capabilities expose 7.10 single itemSubmit fail operation");
        requireContains(capabilitiesJson, "X-TZZ-WebAdmin-CSRF", "capabilities expose csrf header name");
        requireFalse(capabilitiesJson.contains(owner.passwordHash), "capabilities omit password hash value");
        requireFalse(capabilitiesJson.contains(owner.passwordSalt), "capabilities omit password salt value");
        requireFalse(capabilitiesJson.contains(session.sessionIdHash), "capabilities omit session hash");

        String js = WebAdminFrontendAssets.appJs();
        requireFalse(js.contains("fetch('/api/devices', {method:'POST'"), "frontend does not expose device write POST");
        requireFalse(js.contains("method:'DELETE'"), "frontend does not expose DELETE writes");
        requireFalse(js.contains("resetPassword("), "frontend does not expose reset password action");
        requireContains(js, "/api/webadmin/device-metadata/", "frontend exposes scoped device metadata write endpoint");
        requireContains(js, "/api/webadmin/device-basic-config/", "frontend exposes scoped device basic config write endpoint");
        requireContains(js, "/api/webadmin/device-extended-config/", "frontend exposes scoped device extended config write endpoint");
        requireContains(js, "/api/webadmin/action-relay-actions/", "frontend exposes scoped action relay action list endpoint");
        requireContains(js, "/api/webadmin/interaction-item-matcher/", "frontend exposes scoped VBD interaction item matcher endpoint");
        requireContains(js, "/api/webadmin/channel-metadata?channel=", "frontend exposes scoped channel metadata write endpoint");
        requireContains(js, "/api/webadmin/signal-listener-basic-config/", "frontend exposes scoped signal listener basic config endpoint");
        requireContains(js, "/api/webadmin/virtual-block-devices/", "frontend exposes scoped VBD lifecycle endpoint");
        requireContains(js, "/api/webadmin/signal-listeners", "frontend exposes scoped signal listener lifecycle endpoint");
        requireContains(js, "/api/signals/channels", "basic config channel picker reuses readonly signal channel API");
        requireContains(js, "channel-combo", "basic config channel field uses custom dark combobox");
        requireContains(js, "role=\"combobox\"", "basic config channel field keeps typed input semantics");
        requireContains(js, "handleDeviceBasicConfigChannelKey", "basic config channel combobox supports keyboard handling");
        requireContains(js, "handleDeviceExtendedConfigChannelKey", "extended config channel combobox supports keyboard handling");
        requireContains(js, "handleSignalListenerBasicConfigChannelKey", "signal listener channel combobox supports keyboard handling");
        requireContains(js, "handleSignalListenerCreateChannelKey", "signal listener create combobox supports keyboard handling");
        requireContains(js, "renderDeviceExtendedConfigChannelCombo", "extended config channel fields reuse dark combobox helper");
        requireContains(js, "renderSignalListenerConfigChannelCombo", "signal listener channel field reuses dark combobox helper");
        requireContains(js, "renderSignalListenerCreateChannelCombo", "signal listener create channel field uses dark combobox helper");
        requireContains(js, "channelOptionLabel", "basic config channel candidates include display helper");
        requireContains(js, "channelComboQuery", "channel combobox keeps typed value separate from search query so opening menu shows existing channels");
        requireContains(js, "setChannelComboQuery", "channel combobox only filters after the user types a search query");
        requireContains(js, "resetChannelComboQuery", "channel combobox resets search query when opening from a prefilled value");
        requireContains(js, "markChannelOptionsDirty(data)", "realtime channel/device changes invalidate the channel option cache");
        requireContains(js, "appState.channelOptionsDirty=true", "channel option cache marks dirty after related realtime events");
        requireContains(js, "storeSignalChannelOptions(await api('/api/signals/channels'))", "channel combobox and Signal page share the same channel catalog loader");
        requireContains(js, "storeSignalChannelOptions(channels)", "Signal page refresh updates combobox channel option cache");
        requireContains(js, "selection_completed", "VBD selection completion invalidates channel option cache");
        requireContains(js, "virtual_block_device", "virtual block device channel events invalidate channel option cache");
        requireContains(js, "device_extended_config", "extended config channel writes invalidate channel option cache");
        requireContains(js, "interaction_item_matcher", "interaction matcher success/fail channel writes invalidate channel option cache");
        requireContains(js, "if(!force&&!appState.channelOptionsDirty&&Array.isArray(appState.channelOptions))return appState.channelOptions;",
                "channel options reload after dirty realtime events but remain cached otherwise");
        requireContains(js, "filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft))",
                "basic/listener/selection channel comboboxes use independent query instead of current saved channel value");
        requireContains(js, "filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,field))",
                "extended channel combobox uses independent per-field query");
        requireContains(js, "filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,index))",
                "action relay signal action channel combobox uses independent per-action query");
        requireFalse(js.contains("<datalist"), "basic config channel picker does not use native datalist menu");
        requireContains(js, "该频道当前未在系统中发现", "basic config channel input warns about unseen channels");
        requireContains(js, "不会自动创建监听器", "basic config channel input explains manual channel behavior");
        requireContains(js, "/api/webadmin/edit-locks/acquire", "frontend acquires edit lock before metadata write");
        requireContains(js, "/api/webadmin/edit-locks/heartbeat", "frontend heartbeats edit lock during metadata edit");
        requireContains(js, "/api/webadmin/edit-locks/release", "frontend releases edit lock after edit");
        requireContains(js, "device_basic_config", "frontend uses distinct basic config edit lock target");
        requireContains(js, "device_extended_config", "frontend uses distinct extended config edit lock target");
        requireContains(js, "action_relay_actions", "frontend uses distinct action relay action list edit lock target");
        requireContains(js, "interaction_item_matcher", "frontend uses distinct interaction item matcher edit lock target");
        requireContains(js, "channel_metadata", "frontend uses distinct channel metadata edit lock target");
        requireContains(js, "signal_listener_basic_config", "frontend uses distinct signal listener edit lock target");
        requireContains(js, "expectedVersion", "frontend sends expectedVersion for metadata writes");
        requireContains(js, "expectedFingerprint", "frontend sends expectedFingerprint for basic config writes");
        requireContains(js, "saveDeviceExtendedConfig", "frontend contains scoped extended config save handler");
        requireContains(js, "clearInteractChannel", "frontend can explicitly clear optional interact channel");
        requireContains(js, "clearSuccessChannel", "frontend can explicitly clear optional success channel");
        requireContains(js, "clearFailChannel", "frontend can explicitly clear optional fail channel");
        requireContains(js, "interactionCooldownTicks", "frontend exposes interaction cooldown as an extended config field");
        requireContains(js, "pulseTicks", "frontend exposes receiver pulse ticks as an extended config field");
        requireContains(js, "cooldownTicks", "frontend exposes action relay cooldown as an extended config field");
        requireContains(js, "lockId", "frontend sends lock id for metadata writes");
        requireContains(js, "edit_lock_changed", "frontend listens for edit lock realtime events");
        requireContains(js, "saveDeviceBasicConfig", "frontend contains scoped basic config save handler");
        requireContains(js, "saveChannelMetadata", "frontend contains scoped channel metadata save handler");
        requireContains(js, "htmlEvent('onsubmit',`event.preventDefault();saveChannelMetadata(", "channel metadata save form uses escaped submit handler");
        requireFalse(js.contains("onsubmit='event.preventDefault();saveChannelMetadata("), "channel metadata save handler does not use unsafe single-quoted attribute");
        requireFalse(js.contains("lockedByOther?'disabled':''"), "channel metadata and listener locks hide edit buttons instead of showing disabled ambiguous buttons");
        requireContains(js, "canEdit&&!lockedByOther?`<button class=\"secondary\" ${htmlHandler(`startChannelMetadataEdit", "channel metadata hides edit action while another user holds lock");
        requireContains(js, "saveSignalListenerBasicConfig", "frontend contains scoped signal listener save handler");
        requireContains(js, "listener.basicConfig=result.data", "signal detail loads listener lock status for readonly lock hint");
        requireContains(js, "canEdit&&!lockedByOther?`<button class=\"secondary\" type=\"button\" ${htmlHandler(`startSignalListenerBasicConfigEdit", "signal listener basic config hides edit action while another user holds lock");
        requireContains(js, "/api/webadmin/region-controllers", "frontend exposes scoped RegionController WebUI editing API");
        requireContains(js, "region_controller_config", "frontend uses distinct RegionController edit lock target");
        requireContains(js, "saveRegionControllerEdit", "frontend contains scoped RegionController config save handler");
        requireContains(js, "saveRegionControllerCreate", "frontend contains RegionController create handler");
        requireContains(js, "saveRegionControllerDelete", "frontend contains RegionController delete handler");
        requireContains(js, "saveRegionControllerActionAdd", "frontend contains RegionController action add handler");
        requireContains(js, "saveRegionControllerActionClear", "frontend contains RegionController action clear handler");
        requireContains(js, "会阻断 stop/op/ban/kick/whitelist", "RegionController action add UI documents dangerous command blocking");
        requireContains(js, "channel_metadata_changed", "frontend listens for channel metadata realtime events");
        requireContains(js, "signal_listener_config_changed", "frontend listens for signal listener realtime events");
        requireContains(js, "编辑显示信息", "frontend exposes scoped metadata edit action");
        requireContains(js, "此信息仅用于 WebAdmin 展示", "metadata edit warning describes display-only scope");
        requireFalse(js.contains("fetch('/api/actions', {method:'PATCH'"), "frontend does not expose action write PATCH");
        requireFalse(js.contains("fetch('/api/regions', {method:'PATCH'"), "frontend does not expose region write PATCH");
        requireFalse(js.contains("fetch('/api/webadmin/users', {method:'PATCH'"), "frontend does not expose user write PATCH");
        requireFalse(js.contains("saveItemSubmit"), "frontend does not expose itemSubmit save flow");
        requireFalse(js.contains("saveRegion(") || js.contains("saveSettings"), "frontend does not expose generic region/settings save flow");
        requireFalse(js.contains("saveAction(") || js.contains("saveActionTemplate"), "frontend still avoids generic action editor save flow");
        requireContains(js, "data-danger-confirm-modal=\"true\"", "supported lifecycle deletes use dangerous confirm modal");
    }

    private static void testWebAdminRegionControllerEditing() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String context = Files.readString(root.resolve("docs/WEBADMIN_REGION_CONTROLLER_EDITING_7_12_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String service = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java"), StandardCharsets.UTF_8);
        String requests = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminRegionControllerRequests.java"), StandardCharsets.UTF_8);
        String store = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/region/RegionControllerStore.java"), StandardCharsets.UTF_8);
        String actionRelayActions = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String editLock = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String writeFoundation = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminWriteFoundationService.java"), StandardCharsets.UTF_8);

        for (String marker : List.of(
                "7.12 WebAdmin RegionController Full Editing",
                "RegionController WebUI editing only",
                "Existing RegionController Feature Inventory",
                "targetFilter",
                "`enterActions`",
                "`exitActions`",
                "`stayActions`",
                "Manual testing is primary",
                "4K 200% scaled visual profile",
                "No checkpoint before user approval",
                "ConditionEngine",
                "Virtual listener",
                "Path visualization",
                "raw JSON",
                "RegionController runtime semantics are not changed"
        )) {
            requireContains(context, marker, "7.12 context marker present: " + marker);
        }

        for (String marker : List.of(
                "/api/webadmin/region-controllers",
                "handleRegionControllers",
                "regionControllerService.listControllers",
                "regionControllerService.create",
                "regionControllerService.update",
                "regionControllerService.addAction",
                "regionControllerService.clearActions",
                "regionControllerService.deleteAction",
                "regionControllerService.delete",
                "\"clear\".equals(parts[3])",
                "\"delete\".equals(parts[4])",
                "parseRegionTriggerType",
                "triggerType 只支持 enter / exit / stay"
        )) {
            requireContains(webServer, marker, "7.12 RegionController route marker present: " + marker);
        }

        for (String marker : List.of(
                "WebAdminRegionControllerRequests",
                "CreateRequest",
                "UpdateRequest",
                "ActionAddRequest",
                "ActionClearRequest",
                "ActionDeleteRequest",
                "DeleteRequest",
                "expectedFingerprint",
                "lockId"
        )) {
            requireContains(requests, marker, "7.12 RegionController request DTO marker present: " + marker);
        }

        for (String marker : List.of(
                "listControllers",
                "controllerFor",
                "create(",
                "update(",
                "addAction(",
                "clearActions(",
                "deleteAction(",
                "delete(",
                "fingerprintFor",
                "CREATE_LOCK_TARGET_ID",
                "CREATE_EXPECTED_FINGERPRINT",
                "expectedFingerprint",
                "validateLock",
                "requireValidCsrf",
                "sameOrigin",
                "RegionTargetFilter.Type.TAG",
                "MIN_STAY_INTERVAL_TICKS",
                "WebAdminActionRelayActionsService.validateActionEntries",
                "REGION_CONTROLLER_CHANGED",
                "WRITE_AUDIT_APPENDED",
                "noRawJson",
                "noConditionEngine"
        )) {
            requireContains(service, marker, "7.12 RegionController service marker present: " + marker);
        }
        requireFalse(service.contains("rawJson") || service.contains("raw-json") || service.contains("pathGraph"),
                "7.12 RegionController service does not introduce raw JSON/path graph editing");
        for (String marker : List.of(
                "server_management_command_forbidden",
                "isBlockedServerManagementCommand",
                "isServerManagementRoot"
        )) {
            requireContains(actionRelayActions, marker, "7.12 RegionController action validation marker present: " + marker);
        }

        for (String marker : List.of(
                "updateController",
                "replaceActions",
                "RegionTargetFilter.all()",
                "RegionControllerData.DEFAULT_STAY_INTERVAL_TICKS",
                "controller.enterActions()",
                "controller.exitActions()",
                "controller.stayActions()",
                "triggerType == RegionTriggerType.ENTER ? safeActions : controller.enterActions()"
        )) {
            requireContains(store, marker, "7.12 RegionController store scoped update marker present: " + marker);
        }

        for (String marker : List.of(
                "TARGET_REGION_CONTROLLER_CONFIG",
                "RegionController 配置",
                "WebAdminOperationType.EDIT_REGION",
                "#/region-controllers/"
        )) {
            requireContains(editLock, marker, "7.12 RegionController edit lock marker present: " + marker);
        }
        requireContains(writeFoundation, "regionControllerWriteEnabled", "write capabilities expose RegionController editing");
        requireContains(writeFoundation, "RegionController 配置", "write capability message mentions RegionController config");

        for (String marker : List.of(
                "renderRegionControllersPage",
                "renderRegionControllerDetail",
                "regionControllerWriteEnabled",
                "regionControllerDetail:",
                "loadRegionCatalog",
                "/api/regions?limit=500",
                "renderRegionControllerRegionCombo",
                "data-region-catalog-selector=\"true\"",
                "data-region-catalog-read-only=\"true\"",
                "当前绑定区域未在区域列表中找到",
                "openRegionControllerCreateModal",
                "region_controller_create_v1",
                "saveRegionControllerCreate",
                "startRegionControllerEdit",
                "saveRegionControllerEdit",
                "openRegionControllerDeleteModal",
                "saveRegionControllerDelete",
                "openRegionControllerActionAddModal",
                "saveRegionControllerActionAdd",
                "data-region-action-dynamic-fields=\"true\"",
                "data-region-action-signal-only=\"true\"",
                "data-region-action-command-fields=\"true\"",
                "data-region-action-message-field=\"true\"",
                "data-region-action-sound-field=\"true\"",
                "data-region-action-preserve-scroll=\"true\"",
                "openRegionControllerActionClearModal",
                "saveRegionControllerActionClear",
                "openRegionControllerActionListModal",
                "data-region-controller-action-list-modal=\"true\"",
                "data-region-controller-actions-managed-in-modal=\"true\"",
                "data-region-controller-action-list-acquires-lock=\"true\"",
                "data-region-controller-action-preview-hidden=\"true\"",
                "confirmDeleteRegionControllerAction",
                "data-region-controller-action-delete-confirm=\"true\"",
                "data-region-controller-lock-disabled=\"true\"",
                "data-region-controller-lock-badge=\"true\"",
                "region_controller_config",
                "region_controller_action",
                "expectedFingerprint",
                "region-controller-name",
                "region-controller-region-id",
                "region-controller-enabled",
                "targetFilterType",
                "targetFilterValue",
                "stayIntervalTicks",
                "进入动作",
                "离开动作",
                "停留动作",
                "添加动作",
                "暂无进入动作",
                "暂无离开动作",
                "暂无停留动作",
                "dangerousModalFooter(d.saving,",
                "data-danger-confirm-modal=\"true\"",
                "region-controller-action-type",
                "region-controller-action-value",
                "region-controller-action-cooldown"
        )) {
            requireContains(js, marker, "7.12 RegionController frontend marker present: " + marker);
        }
        requireFalse(js.contains("conditionEngineEditor") || js.contains("regionPathGraph") || js.contains("raw-json-textarea"),
                "7.12 RegionController frontend does not expose ConditionEngine/path graph/raw JSON editors");
    }

    private static void testWebAdminSignalListenerEditing() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String context = Files.readString(root.resolve("docs/WEBADMIN_SIGNAL_LISTENER_EDITING_7_13_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendAssets.appJs();
        String webServer = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String service = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerActionsService.java"), StandardCharsets.UTF_8);
        String requests = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminSignalListenerActionRequests.java"), StandardCharsets.UTF_8);
        String store = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalListenerStore.java"), StandardCharsets.UTF_8);
        String signalBridge = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalBridgeServer.java"), StandardCharsets.UTF_8);
        String actionEngine = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/action/ActionEngine.java"), StandardCharsets.UTF_8);
        String lifecycle = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerLifecycleService.java"), StandardCharsets.UTF_8);
        String actionRelayActions = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String editLock = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String writeFoundation = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminWriteFoundationService.java"), StandardCharsets.UTF_8);

        for (String marker : List.of(
                "7.13 WebAdmin SignalListener",
                "虚拟监听器完整编辑",
                "This phase only completes WebAdmin editing for existing SignalListener capabilities",
                "Action list display",
                "Action add, single delete, and clear",
                "Dark channel combobox",
                "Edit lock UI must be visible",
                "Manual UI validation remains required before checkpoint",
                "854x480",
                "4K 200% scaled",
                "ConditionEngine",
                "Path visualization",
                "Raw JSON",
                "SignalBridge runtime rewrite",
                "RegionController editing",
                "Listener recent events",
                "latest 3",
                "basic config edit button"
        )) {
            requireContains(context, marker, "7.13 context marker present: " + marker);
        }

        for (String marker : List.of(
                "signalListenerActionsService",
                "/api/webadmin/signal-listeners/",
                "\"actions\".equals(parts[1])",
                "signalListenerActionsService.actionsFor",
                "signalListenerActionsService.addAction",
                "signalListenerActionsService.clearActions",
                "signalListenerActionsService.deleteAction",
                "\"clear\".equals(parts[2])",
                "\"delete\".equals(parts[3])"
        )) {
            requireContains(webServer, marker, "7.13 SignalListener action route marker present: " + marker);
        }

        for (String marker : List.of(
                "ActionAddRequest",
                "ActionClearRequest",
                "ActionDeleteRequest",
                "expectedFingerprint",
                "lockId",
                "confirmed"
        )) {
            requireContains(requests, marker, "7.13 SignalListener action request DTO marker present: " + marker);
        }

        for (String marker : List.of(
                "actionsFor",
                "addAction(",
                "clearActions(",
                "deleteAction(",
                "WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS",
                "WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS",
                "validateLock",
                "requireValidCsrf",
                "sameOrigin",
                "synchronized (SignalListenerStore.class)",
                "WebAdminSignalListenerBasicConfigService.fingerprintFor",
                "WebAdminActionRelayActionsService.validateActionEntries",
                "DANGEROUS_OPERATION_REQUIRES_CONFIRMATION",
                "SIGNAL_LISTENER_ACTION_CHANGED",
                "WRITE_AUDIT_APPENDED",
                "noRawJson",
                "noConditionEngine",
                "noPathVisualization"
        )) {
            requireContains(service, marker, "7.13 SignalListener actions service marker present: " + marker);
        }
        requireFalse(service.contains("conditionEngineEditor") || service.contains("pathGraph") || service.contains("rawJsonEditor"),
                "7.13 SignalListener action service does not introduce ConditionEngine/path graph/raw JSON editors");

        for (String marker : List.of(
                "replaceActionsForWebAdmin",
                "listener.name()",
                "listener.channel()",
                "listener.enabled()",
                "listener.cooldownTicks()",
                "safeActions",
                "SIGNAL_LISTENER_ACTION_CHANGED"
        )) {
            requireContains(store, marker, "7.13 SignalListener store scoped action update marker present: " + marker);
        }

        for (String marker : List.of(
                "ActionEngine.execute(context, action)",
                "监听器动作执行失败：第 ",
                "action.type().id()",
                "部分监听器执行失败",
                "recordHistory("
        )) {
            requireContains(signalBridge, marker, "7.13 SignalListener runtime diagnostic marker present: " + marker);
        }
        requireFalse(signalBridge.contains("conditionEngine") || signalBridge.contains("PathGraph") || signalBridge.contains("rawJson"),
                "SignalListener runtime fix does not introduce ConditionEngine/path graph/raw JSON");

        for (String marker : List.of(
                "case MESSAGE -> executeMessage",
                "context.world().getServer().getPlayerManager().getPlayerList()",
                "消息已广播",
                "case SIGNAL -> executeSignal",
                "case SOUND -> executeSound",
                "case COMMAND -> executeCommand"
        )) {
            requireContains(actionEngine, marker, "7.13 SignalListener supported action runtime marker present: " + marker);
        }

        requireContains(lifecycle, "请确认删除该虚拟监听器", "SignalListener delete confirm no longer requires typed id/name");
        requireFalse(lifecycle.contains("请输入 Listener ID 或名称以确认删除"),
                "SignalListener delete confirmation does not require typed listener id/name");

        for (String marker : List.of(
                "TARGET_SIGNAL_LISTENER_ACTIONS",
                "Signal Listener 动作列表",
                "WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS",
                "#/listeners/"
        )) {
            requireContains(editLock, marker, "7.13 SignalListener action edit lock marker present: " + marker);
        }
        requireContains(writeFoundation, "signalListenerActionListWriteEnabled", "write capabilities expose SignalListener action list editing");
        requireContains(writeFoundation, "Signal Listener 基础配置与动作列表", "write capability message mentions SignalListener action list");

        for (String marker : List.of(
                "renderSignalListenerDetail",
                "currentSignalListenerDetail",
                "routeListenerChannel",
                "signal_history_appended",
                "data-signal-listener-recent-events-max3=\"true\"",
                "data-signal-listener-recent-events-deduped=\"true\"",
                "listenerRecentEvents(items,channel)",
                "recent.length>=3",
                "signalListenerBasicConfigEditAction",
                "data-signal-listener-basic-lock-disabled=\"true\"",
                "data-signal-listener-basic-lock-badge=\"true\"",
                "data-signal-listener-basic-lock-current=\"true\"",
                "signalListenerActionSummaryCard",
                "data-signal-listener-action-summary-card=\"true\"",
                "data-signal-listener-action-preview-hidden=\"true\"",
                "openSignalListenerActionListModal",
                "openReadonlySignalListenerActionListFromLock",
                "data-signal-listener-action-list-modal=\"true\"",
                "data-signal-listener-actions-managed-in-modal=\"true\"",
                "data-signal-listener-action-list-acquires-lock=\"true\"",
                "openSignalListenerActionAddModal",
                "saveSignalListenerActionAdd",
                "data-signal-listener-action-dynamic-fields=\"true\"",
                "data-signal-listener-action-signal-only=\"true\"",
                "data-signal-listener-action-command-fields=\"true\"",
                "data-signal-listener-action-message-field=\"true\"",
                "data-signal-listener-action-sound-field=\"true\"",
                "data-signal-listener-action-preserve-scroll=\"true\"",
                "renderSignalListenerActionChannelCombo",
                "signal-listener-action-channel-combo",
                "confirmDeleteSignalListenerAction",
                "data-signal-listener-action-delete-confirm=\"true\"",
                "openSignalListenerActionClearModal",
                "saveSignalListenerActionClear",
                "data-signal-listener-action-clear-confirm=\"true\"",
                "data-signal-listener-action-lock-disabled=\"true\"",
                "data-signal-listener-action-lock-badge=\"true\"",
                "data-signal-listener-action-current-lock=\"true\"",
                "signal_listener_actions",
                "expectedFingerprint",
                "编辑锁",
                "虚拟监听器",
                "监听频道",
                "动作列表",
                "添加动作",
                "清空动作",
                "暂无动作",
                "信号频道",
                "消息内容",
                "音效 ID",
                "需要 OP 权限",
                "通知 OP",
                "不需要输入 ID 或名称",
                "不会提供 raw JSON",
                "不改变 SignalBridge 运行时语义"
        )) {
            requireContains(js, marker, "7.13 SignalListener frontend marker present: " + marker);
        }
        requireFalse(js.contains("listener-delete-confirmation"), "SignalListener delete modal no longer asks for typed id/name");
        requireFalse(js.contains("conditionEngineEditor") || js.contains("signalPathGraph") || js.contains("raw-json-textarea"),
                "7.13 SignalListener frontend does not expose ConditionEngine/path graph/raw JSON editors");
        for (String marker : List.of(
                "server_management_command_forbidden",
                "isBlockedServerManagementCommand",
                "isServerManagementRoot"
        )) {
            requireContains(actionRelayActions, marker, "7.13 SignalListener command action validation marker present: " + marker);
        }
    }

    private static WebAdminUser webAdminUser(String username, WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = username;
        user.displayName = username;
        user.role = role.id();
        return user.normalized();
    }

    private static SignalDeviceData fullDevice() {
        Map<String, String> conditionProperties = new LinkedHashMap<>();
        conditionProperties.put("powered", "true");

        ContainerItemConditionData itemCondition = new ContainerItemConditionData(
                "condition-1",
                "slot0_diamond",
                true,
                ContainerItemConditionType.SLOT_ITEM.id(),
                0,
                "minecraft:diamond",
                ContainerItemCountMode.AT_LEAST.id(),
                3,
                "item.enter",
                "item.exit",
                BlockStateConditionMode.CONDITION_BOTH.id(),
                true,
                101L,
                102L,
                103L,
                "item condition ok"
        ).normalized();

        return new SignalDeviceData(
                "virtual_block_device:minecraft:overworld@1,2,3",
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE,
                "full-device",
                "minecraft:overworld",
                1,
                2,
                3,
                "main.channel",
                true,
                5,
                0,
                0,
                2,
                10L,
                20L,
                30L,
                40L,
                "last result",
                "minecraft:chest",
                "off.channel",
                VirtualBlockDeviceMode.REDSTONE_BOTH.id(),
                true,
                15,
                true,
                "minecraft:chest",
                conditionProperties,
                "minecraft:chest[powered=true]",
                BlockStateConditionMode.CONDITION_BOTH.id(),
                true,
                50L,
                "condition result",
                true,
                "interact.channel",
                40,
                60L,
                70L,
                "Steve",
                "00000000-0000-0000-0000-000000000001",
                "interaction result",
                "MAIN_HAND",
                "north",
                true,
                "container.open",
                "container.close",
                "container.change",
                20,
                10,
                80L,
                "fingerprint",
                81L,
                82L,
                83L,
                84L,
                85L,
                86L,
                "Alex",
                "00000000-0000-0000-0000-000000000002",
                "container result",
                "change",
                List.of(itemCondition),
                true,
                fullMatcher(),
                true,
                "interaction item result",
                true,
                true,
                InventoryConsumeOrder.MAIN_INVENTORY_FIRST,
                List.of(fullRequirement("need_diamond_3", "minecraft:diamond", 3)),
                true,
                "",
                "submit:need_diamond_3 x3",
                "submit result"
        ).normalized();
    }

    private static ItemStackMatcherData fullMatcher() {
        return new ItemStackMatcherData(
                true,
                "minecraft:diamond",
                3,
                ContainerItemCountMode.AT_LEAST.id(),
                2,
                true,
                true,
                true,
                true,
                true,
                true,
                4,
                "Custom Diamond",
                List.of("Lore line"),
                "{custom:1b}",
                "{components}",
                "minecraft:diamond x3",
                "success.channel",
                "fail.channel",
                "success message",
                "fail message",
                "minecraft:block.note_block.pling",
                0.7F,
                1.1F,
                "minecraft:block.note_block.bass",
                0.5F,
                0.8F,
                true,
                2,
                InteractionItemConsumeSource.INVENTORY,
                InventoryConsumeOrder.MAIN_INVENTORY_FIRST,
                InteractionItemSource.INVENTORY_CONTAINS,
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH,
                InteractionItemSource.INVENTORY_CONTAINS,
                4,
                9,
                "source matched",
                InteractionItemConsumeSource.INVENTORY,
                "inv:0 x2",
                "consume ok",
                100L,
                200L
        ).normalized();
    }

    private static ItemSubmitRequirementData fullRequirement(String name, String itemId, int count) {
        ItemStackMatcherData matcher = new ItemStackMatcherData(
                true,
                itemId,
                count,
                ContainerItemCountMode.AT_LEAST.id(),
                count,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                itemId + " x" + count,
                100L,
                200L
        ).normalized();
        return new ItemSubmitRequirementData(
                "requirement-" + name,
                name,
                true,
                matcher,
                count,
                true,
                count,
                300L,
                "matched"
        ).normalized();
    }

    private static ItemSubmitRequirementData submitRequirement(
            String name,
            String itemId,
            ContainerItemCountMode mode,
            int requiredCount,
            int consumeCount,
            boolean enabled
    ) {
        ItemStackMatcherData matcher = new ItemStackMatcherData(
                true,
                itemId,
                requiredCount,
                mode.id(),
                requiredCount,
                true,
                false,
                false,
                false,
                false,
                false,
                0,
                "",
                List.of(),
                "",
                "",
                itemId,
                100L,
                200L
        ).normalized();
        return new ItemSubmitRequirementData(
                "requirement-" + name,
                name,
                enabled,
                matcher,
                consumeCount,
                false,
                0,
                0L,
                ""
        ).normalized();
    }

    private static ConsumePlanner.ConsumableStack consumable(String key, FakeStack stack) {
        return new ConsumePlanner.ConsumableStack(key, stack.count(), key, stack::consume);
    }

    private static List<ItemSubmitEvaluator.SourceStack> sourceStacks(FakeStack... stacks) {
        List<ItemSubmitEvaluator.SourceStack> sources = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            FakeStack stack = stacks[i];
            sources.add(new ItemSubmitEvaluator.SourceStack(
                    "inv:" + i,
                    stack.itemId(),
                    stack.count(),
                    "inv:" + i,
                    stack::consume
            ));
        }
        return sources;
    }

    private static List<ItemSubmitEvaluator.SourceStack> sourceStacksWithKeys(FakeStack stack) {
        return List.of(new ItemSubmitEvaluator.SourceStack(
                "inv:0",
                stack.itemId(),
                stack.count(),
                "inv:0",
                stack::consume
        ));
    }

    private static boolean sourceStackMatchesItemId(ItemSubmitEvaluator.SourceStack source, ItemStackMatcherData matcher) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return data.enabled() && (!data.matchItemId() || source.itemId().equals(data.templateItemId()));
    }

    private static SignalDeviceData readSample(Gson gson, String sampleName) {
        String path = "stabilization/" + sampleName;
        try (InputStream stream = StabilizationGuardTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("Missing test resource: " + path);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                SignalDeviceData data = gson.fromJson(reader, SignalDeviceData.class);
                if (data == null) {
                    throw new AssertionError("Failed to parse test resource: " + path);
                }
                return data;
            }
        } catch (Exception exception) {
            throw new AssertionError("Failed to read test resource " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static final class FakeStack {
        private final String itemId;
        private int count;

        private FakeStack(String itemId, int count) {
            this.itemId = itemId;
            this.count = Math.max(0, count);
        }

        private String itemId() {
            return itemId;
        }

        private int count() {
            return count;
        }

        private void consume(int amount) {
            count = Math.max(0, count - Math.max(0, amount));
        }
    }

    private static SignalDeviceData invokeSignalDeviceCopy(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return invokeSignalDeviceCopyRaw(methodName, parameterTypes, args).normalized();
    }

    private static SignalDeviceData invokeSignalDeviceCopyRaw(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = SignalDeviceStore.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        Object result = method.invoke(null, args);
        if (!(result instanceof SignalDeviceData data)) {
            throw new AssertionError(methodName + " did not return SignalDeviceData");
        }
        return data;
    }

    private static void assertSubmitPreserved(SignalDeviceData expected, SignalDeviceData actual) {
        requireEquals(expected.itemSubmitEnabled(), actual.itemSubmitEnabled(), "itemSubmitEnabled preserved");
        requireEquals(expected.itemSubmitConsumeEnabled(), actual.itemSubmitConsumeEnabled(), "itemSubmitConsumeEnabled preserved");
        requireEquals(expected.itemSubmitConsumeOrder(), actual.itemSubmitConsumeOrder(), "itemSubmitConsumeOrder preserved");
        requireEquals(expected.itemSubmitRequirements().size(), actual.itemSubmitRequirements().size(), "itemSubmitRequirements preserved");
        requireEquals(expected.itemSubmitRequirements().get(0).name(), actual.itemSubmitRequirements().get(0).name(), "itemSubmit requirement name preserved");
        requireEquals(expected.lastItemSubmitConsumedSummary(), actual.lastItemSubmitConsumedSummary(), "lastItemSubmitConsumedSummary preserved");
    }

    private static void assertContainerPreserved(SignalDeviceData expected, SignalDeviceData actual) {
        requireEquals(expected.containerEnabled(), actual.containerEnabled(), "containerEnabled preserved");
        requireEquals(expected.containerOpenChannel(), actual.containerOpenChannel(), "containerOpenChannel preserved");
        requireEquals(expected.containerCloseChannel(), actual.containerCloseChannel(), "containerCloseChannel preserved");
        requireEquals(expected.containerChangeChannel(), actual.containerChangeChannel(), "containerChangeChannel preserved");
        requireEquals(expected.containerChangeCheckIntervalTicks(), actual.containerChangeCheckIntervalTicks(), "container interval preserved");
    }

    private static void assertItemConditionsPreserved(SignalDeviceData expected, SignalDeviceData actual) {
        requireEquals(expected.itemConditions().size(), actual.itemConditions().size(), "itemConditions preserved");
        requireEquals(expected.itemConditions().get(0).name(), actual.itemConditions().get(0).name(), "item condition name preserved");
        requireEquals(expected.itemConditions().get(0).channel(), actual.itemConditions().get(0).channel(), "item condition channel preserved");
    }

    private static void assertRedstoneAndConditionPreserved(SignalDeviceData expected, SignalDeviceData actual) {
        requireEquals(expected.channel(), actual.channel(), "redstone channel preserved");
        requireEquals(expected.offChannel(), actual.offChannel(), "offChannel preserved");
        requireEquals(expected.mode(), actual.mode(), "redstone mode preserved");
        requireEquals(expected.conditionEnabled(), actual.conditionEnabled(), "conditionEnabled preserved");
        requireEquals(expected.conditionBlockId(), actual.conditionBlockId(), "conditionBlockId preserved");
        requireEquals(expected.conditionProperties(), actual.conditionProperties(), "conditionProperties preserved");
        requireEquals(expected.conditionMode(), actual.conditionMode(), "conditionMode preserved");
    }

    private static void assertInteractionMatcherPreserved(SignalDeviceData expected, SignalDeviceData actual) {
        ItemStackMatcherData expectedMatcher = expected.interactionItemMatcher();
        ItemStackMatcherData actualMatcher = actual.interactionItemMatcher();
        requireEquals(expected.interactionItemMatcherEnabled(), actual.interactionItemMatcherEnabled(), "interactionItemMatcherEnabled preserved");
        requireEquals(expectedMatcher.templateItemId(), actualMatcher.templateItemId(), "matcher template item preserved");
        requireEquals(expectedMatcher.successChannel(), actualMatcher.successChannel(), "matcher success channel preserved");
        requireEquals(expectedMatcher.failChannel(), actualMatcher.failChannel(), "matcher fail channel preserved");
        requireEquals(expectedMatcher.successMessage(), actualMatcher.successMessage(), "matcher success message preserved");
        requireEquals(expectedMatcher.failMessage(), actualMatcher.failMessage(), "matcher fail message preserved");
        requireEquals(expectedMatcher.consumeEnabled(), actualMatcher.consumeEnabled(), "matcher consume enabled preserved");
        requireEquals(expectedMatcher.consumeCount(), actualMatcher.consumeCount(), "matcher consume count preserved");
        requireEquals(expectedMatcher.interactionItemSource(), actualMatcher.interactionItemSource(), "matcher source preserved");
        requireEquals(expectedMatcher.interactionItemVanillaPolicy(), actualMatcher.interactionItemVanillaPolicy(), "matcher vanilla policy preserved");
    }

    private static void requirePermission(
            WebAdminPermissionService permissions,
            WebAdminRole role,
            WebAdminOperationType operation,
            boolean expected
    ) {
        boolean actual = permissions.decide(role, operation).allowed();
        requireEquals(expected, actual, "permission " + role.id() + " " + operation.id());
    }

    private static void testWebAdminEditingStabilization714() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String context = Files.readString(root.resolve("docs/WEBADMIN_EDITING_STABILIZATION_7_14_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/WEBADMIN_EDITING_CAPABILITY_MATRIX_7_14.md"), StandardCharsets.UTF_8);
        String localMcpContext = Files.readString(root.resolve("docs/LOCAL_TEST_MCP_FOUNDATION_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendScripts.appJs();

        for (String marker : List.of(
                "7.14 WebAdmin Editing Stabilization",
                "not a new feature expansion stage",
                "Manual testing is primary after 7.13",
                "Local Test MCP remains available as an auxiliary tool",
                "Generate screenshots",
                "Run screenshot matrices",
                "Run MCP scenarios",
                "7.15 Channel Logic Chain Viewer",
                "8.x ConditionEngine",
                "Raw JSON or arbitrary NBT path editors",
                "No arbitrary shell",
                "No git mutation tools in MCP"
        )) {
            requireContains(context, marker, "7.14 stabilization context marker present: " + marker);
        }

        for (String marker : List.of(
                "v1.46.0-condition-engine-core",
                "7.14 WebAdmin Editing Stabilization",
                "7.x 已把 WebAdmin 从只读观察层推进到受控编辑层",
                "docs/WEBADMIN_EDITING_STABILIZATION_7_14_CURRENT_CONTEXT.md",
                "docs/WEBADMIN_EDITING_CAPABILITY_MATRIX_7_14.md",
                "Local Test MCP Foundation",
                "手动测试仍为主",
                "MCP 不提供任意 shell",
                "不提供 git mutation",
                "edit lock 不是 toast-only",
                "dark combobox",
                "4K 200% scaled",
                "7.15 WebAdmin Logic Chain Viewer Current Context",
                "8.x：ConditionEngine",
                "不要误认为已完成",
                "不提交 `logs/`、`reports/mcp`、screenshots、`node_modules`",
                "不提供 raw JSON / NBT path 编辑",
                "WebAdmin 历史阶段记录"
        )) {
            requireContains(readme, marker, "README 7.14 current capability marker present: " + marker);
        }

        for (String marker : List.of(
                "WebAdmin Editing Capability Matrix 7.14",
                "Device metadata",
                "Physical device basic config",
                "VBD native trigger",
                "Interaction matcher",
                "itemSubmit unified editor",
                "Container template",
                "Signal channel metadata",
                "ActionRelay",
                "SignalReceiver",
                "RegionController",
                "SignalListener",
                "Users",
                "Settings",
                "Doctor",
                "History",
                "Local Test MCP",
                "Manual testing is primary",
                "Logic Chain Viewer",
                "8.x: ConditionEngine"
        )) {
            requireContains(matrix, marker, "7.14 editing capability matrix marker present: " + marker);
        }

        for (String marker : List.of(
                "channel-combo",
                "renderRegionControllerRegionCombo",
                "openActionRelayActionsReadonlyModal",
                "openRegionControllerActionListModal",
                "openSignalListenerActionListModal",
                "signalListenerBasicConfigEditAction",
                "data-signal-listener-basic-lock-disabled=\"true\"",
                "data-region-controller-action-summary-card=\"true\"",
                "data-signal-listener-action-summary-card=\"true\"",
                "data-region-action-dynamic-fields=\"true\"",
                "data-signal-listener-action-dynamic-fields=\"true\"",
                "dangerousModalFooter"
        )) {
            requireContains(js, marker, "7.14 WebAdmin UI style consistency marker present: " + marker);
        }

        requireContains(localMcpContext, "No arbitrary shell tool", "MCP context still forbids arbitrary shell");
        requireContains(localMcpContext, "No git mutation tools", "MCP context still forbids git mutation");
        requireContains(localMcpContext, "No Minecraft client GUI coordinate clicking", "MCP context still forbids Minecraft GUI coordinate clicking");
        requireContains(localMcpContext, "reports/mcp", "MCP context documents local report output");

        requireFalse(readme.contains("MCP 已完全代替手工验收") || readme.contains("MCP completely replaces manual testing"),
                "README must not claim MCP replaces manual testing");
        requireFalse(context.contains("本阶段实现 ConditionEngine") || context.contains("Channel Logic Chain Editor is complete"),
                "7.14 context must not claim future large systems are complete");
        requireFalse(matrix.contains("raw JSON editor complete") || matrix.contains("ConditionEngine complete"),
                "7.14 matrix must not mark raw JSON or ConditionEngine complete");
        requireFalse(js.contains("channelLogicChainEditor") || js.contains("saveChannelLogicChain") || js.contains("data-channel-logic-chain-editor"),
                "7.14 must not introduce channel logic chain editor write paths");
        requireFalse(js.contains("conditionEngineEditor") || js.contains("saveConditionEngine"),
                "7.14 must not introduce ConditionEngine editor write paths");
        requireFalse(js.contains("raw-json-textarea") || js.contains("rawJsonEditor"),
                "7.14 must not introduce raw JSON editor paths");
    }

    private static void testWebAdminLogicChainViewer715() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/WEBADMIN_LOGIC_CHAIN_VIEWER_7_15_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/WEBADMIN_EDITING_CAPABILITY_MATRIX_7_14.md"), StandardCharsets.UTF_8);
        String js = WebAdminFrontendScripts.appJs();
        String shell = WebAdminFrontendShell.appHtml();
        String styles = WebAdminFrontendStyles.appCss();
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String service = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainService.java"), StandardCharsets.UTF_8);
        String listenerActionsService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerActionsService.java"), StandardCharsets.UTF_8);
        String store = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainMetadataStore.java"), StandardCharsets.UTF_8);
        String dtos = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminDtos.java"), StandardCharsets.UTF_8);
        String operationType = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminOperationType.java"), StandardCharsets.UTF_8);
        String editLock = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String realtime = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/realtime/WebAdminRealtimeEventType.java"), StandardCharsets.UTF_8);

        for (String marker : List.of(
                "7.15 WebAdmin Cross-Channel Logic Chain Viewer MVP",
                "read-only visualization stage",
                "Producer -> Channel -> Consumer -> Action / Relay / Receiver -> Downstream Channel -> Next Channel Segment",
                "WebAdmin-only metadata",
                "does not modify SignalBridge runtime",
                "tree / mind-map model",
                "not a fixed-lane table layout",
                "Curved connectors",
                "mouse drag pan",
                "parent y",
                "main chain / sub-chain hierarchy",
                "Child-chain rows can be expanded and collapsed both ways",
                "Self-cycle channels",
                "A -> B -> A",
                "SignalListener action management supports single action edit",
                "Do not mix consumers from different channels",
                "Do not draw long cross-channel lines",
                "Cycle detection",
                "Maximum depth",
                "ConditionEngine",
                "AND / OR / NOT",
                "New MCP tool",
                "MCP scenario"
        )) {
            requireContains(context, marker, "7.15 context marker present: " + marker);
        }
        requireContains(context, "7.15 does not implement:", "7.15 context keeps explicit non-goal section");
        requireContains(context, "New MCP tool.", "7.15 context keeps new MCP tool in non-goals");
        requireContains(context, "MCP scenario.", "7.15 context keeps MCP scenario in non-goals");
        requireFalse(context.contains("ConditionEngine is complete") || context.contains("ConditionEngine 已完成")
                        || context.contains("channel logic chain editor is complete") || context.contains("频道逻辑链编辑器已完成")
                        || context.contains("MCP scenario is required") || context.contains("Run MCP scenarios"),
                "7.15 context must not claim ConditionEngine/editor/MCP scenario completion");

        for (String marker : List.of(
                "v1.46.0-condition-engine-core",
                "8.0 ConditionEngine Core",
                "Logic Chain Viewer MVP",
                "只读跨频道逻辑链查看器",
                "WebAdmin-only 视图 metadata",
                "7.15 逻辑链只读查看器不是编辑器",
                "docs/WEBADMIN_LOGIC_CHAIN_VIEWER_7_15_CURRENT_CONTEXT.md"
        )) {
            requireContains(readme, marker, "README 7.15 marker present: " + marker);
        }

        for (String marker : List.of(
                "Logic Chain Viewer",
                "Cross-channel logic chain list and read-only mind-map/tree viewer",
                "WebAdmin-only view metadata",
                "mind-map/tree viewer",
                "downstream channel child subtrees",
                "main/sub-chain hierarchy",
                "draggable viewport",
                "SVG curved connectors",
                "no table-like fixed lane layout",
                "no runtime editing",
                "no ConditionEngine",
                "no long cross-channel line mixing"
        )) {
            requireContains(matrix, marker, "capability matrix 7.15 marker present: " + marker);
        }

        for (String marker : List.of(
                "#/logic-chains",
                "renderLogicChainsPage",
                "renderLogicChainViewer",
                "renderLogicChainResolve",
                "logicChainResolveHash",
                "查看逻辑链",
                "data-channel-logic-chain-viewer=\"true\"",
                "data-logic-chain-mind-map-tree=\"true\"",
                "data-curved-connectors=\"true\"",
                "data-no-table-like-fixed-lane-layout=\"true\"",
                "data-root-channel-consumer-action-downstream-tree=\"true\"",
                "data-channel-separates-consumers=\"true\"",
                "data-same-channel-consumers-parallel=\"true\"",
                "data-action-order-local-only=\"true\"",
                "data-downstream-channel-child-subtree=\"true\"",
                "data-no-cross-channel-consumer-mixing=\"true\"",
                "data-no-cross-channel-long-line-mixing=\"true\"",
                "data-logic-chain-readonly-graph=\"true\"",
                "data-no-runtime-node-creation=\"true\"",
                "logicChainBuildTree",
                "logicChainLayoutTree",
                "logicChainMindMap",
                "logicChainHierarchyRows",
                "data-curved-continuous-connectors=\"true\"",
                "data-canvas-mouse-drag-pan=\"true\"",
                "data-node-selection-preserves-viewport=\"true\"",
                "data-local-badges-anchored=\"true\"",
                "data-parent-y-centered-by-child-subtree=\"true\"",
                "data-logic-chain-sub-chain-hierarchy=\"true\"",
                "data-no-flat-all-chains-list=\"true\"",
                "data-third-level-sub-chain",
                "data-multiple-upstream-reference",
                "data-cycle-hierarchy-guard",
                "data-logic-chain-child-toggle-collapses=\"true\"",
                "data-nested-child-chain-collapse=\"true\"",
                "data-expanded-state-keyed-by-chain-id=\"true\"",
                "data-self-cycle-not-child-chain",
                "data-signal-listener-single-action-edit=\"true\"",
                "data-signal-listener-action-edit-uses-dynamic-fields=\"true\"",
                "data-signal-listener-action-edit-preserves-order=\"true\"",
                "data-signal-listener-action-delete-preserves-scroll=\"true\"",
                "data-action-modal-validation-preserves-scroll=\"true\"",
                "data-action-update-keeps-listener-base-fields=\"true\"",
                "toggleLogicChainSubtree",
                "startLogicChainPan",
                "思维导图模式",
                "展开下游",
                "下游频道表示由上游动作触发的新频道",
                "7.15 仅提供只读跨频道逻辑链查看器",
                "8.x ConditionEngine"
        )) {
            requireContains(js, marker, "7.15 frontend marker present: " + marker);
        }

        requireContains(shell, "data-route=\"#/logic-chains\"", "7.15 shell navigation exposes logic chain list page");
        requireContains(styles, ".logic-chain-layout", "7.15 styles include logic chain layout");
        requireContains(styles, ".logic-chain-mind-map", "7.15 styles include mind-map tree layout");
        requireContains(styles, ".logic-chain-tree-node", "7.15 styles include tree nodes");
        requireContains(styles, ".logic-chain-edge-layer", "7.15 styles include SVG edge overlay");
        requireContains(styles, ".logic-chain-edge", "7.15 styles include continuous curved connectors");
        requireContains(styles, ".logic-chain-collapse", "7.15 styles include downstream expand/collapse control");
        requireContains(styles, ".logic-chain-node-badge", "7.15 styles anchor local badges to nodes");
        requireContains(styles, ".logic-chain-row-toggle", "7.15 styles include expandable sub-chain rows");
        requireContains(styles, ".logic-chain-minimap", "7.15 styles include minimap");
        requireFalse(styles.contains(".logic-chain-segment-grid") || js.contains("logicChainColumn(") || js.contains("logicChainSegment("),
                "7.15 frontend no longer uses table-like fixed lane segment layout");
        requireContains(server, "/api/webadmin/logic-chains", "7.15 server exposes logic chain API route");
        requireContains(server, "tail.equals(\"resolve\")", "7.15 server exposes temporary logic chain resolve route");
        requireContains(service, "buildSegment", "7.15 service builds channel segments");
        requireContains(service, "already_expanded", "7.15 service marks repeated channel references");
        requireContains(service, "cycle", "7.15 service handles cycles");
        requireContains(service, "DEFAULT_MAX_DEPTH", "7.15 service declares default max depth");
        requireContains(service, "HARD_MAX_DEPTH", "7.15 service clamps hard max depth");
        requireContains(service, "超出最大展开深度", "7.15 service reports max-depth truncation");
        requireContains(service, "includeNode", "7.15 service applies includeDisabled setting");
        requireContains(service, "未加载，无法展开其动作列表", "7.15 service warns when action relay actions are unavailable");
        requireContains(service, "deviceMatchesRootType", "7.15 service validates non-channel root type matches");
        requireContains(service, "resolveActionRootChannel", "7.15 service can resolve action detail entries without runtime editing");
        requireContains(service, "emits_downstream", "7.15 service models downstream channel cards");
        requireContains(service, "ChannelHierarchy", "7.15 service derives top-level/sub-chain hierarchy");
        requireContains(service, "multipleParents", "7.15 service marks multiple upstream references");
        requireContains(service, "selfCycles", "7.15 service marks self-cycle without generating child chain");
        requireContains(service, "cycleChannels", "7.15 service guards A->B->A and deeper cycle references");
        requireContains(dtos, "hierarchyLevel", "7.15 summary DTO exposes hierarchy level");
        requireContains(dtos, "parentChainId", "7.15 summary DTO exposes parent chain id");
        requireContains(dtos, "upstreamSourceLabel", "7.15 summary DTO exposes upstream source label");
        requireContains(dtos, "selfCycle", "7.15 summary DTO exposes self-cycle flag");
        requireContains(server, "method.equalsIgnoreCase(\"PATCH\")", "7.15 SignalListener single action edit API uses PATCH");
        requireContains(server, "signalListenerActionsService.updateAction", "7.15 server routes SignalListener single action update");
        requireContains(listenerActionsService, "updateAction", "7.15 SignalListener action service supports single action edit");
        requireContains(listenerActionsService, "actions.set(index", "7.15 SignalListener action edit preserves action order by replacing target index");
        requireContains(listenerActionsService, "changedFields\", List.of(\"actions\")", "7.15 SignalListener action edit only changes actions");
        requireContains(service, "GraphStats", "7.15 service summarizes graph stats");
        requireContains(dtos, "LogicChainGraphDto", "7.15 DTO exposes logic chain graph");
        requireContains(store, "WebAdmin-only", "7.15 metadata store documents WebAdmin-only metadata");
        requireContains(operationType, "EDIT_LOGIC_CHAIN_METADATA", "7.15 metadata write operation registered");
        requireContains(editLock, "TARGET_LOGIC_CHAIN_METADATA", "7.15 metadata edit lock target registered");
        requireContains(realtime, "LOGIC_CHAIN_METADATA_CHANGED", "7.15 realtime event registered");
        requireContains(service, "requireValidCsrf", "7.15 metadata write requires CSRF");
        requireContains(service, "sameOrigin", "7.15 metadata write checks same-origin");
        requireContains(service, "expectedFingerprint", "7.15 metadata write carries expected fingerprint");
        requireContains(service, "WebAdminWriteResult", "7.15 metadata write returns WebAdminWriteResult");
        requireContains(service, "WebAdminAuditLogger", "7.15 metadata write records audit");
        requireContains(js, "modalDraftDirty('logic_chain_metadata'", "7.15 metadata modal has dirty guard");
        requireContains(service, "autoRootChainId", "7.15 temporary metadata ids are scoped by root type/ref");

        requireFalse(js.contains("data-channel-logic-chain-editor") || js.contains("saveRuntimeLogicChain") || js.contains("createRuntimeLogicNode"),
                "7.15 must not expose runtime logic chain editor paths");
        requireFalse(js.contains("conditionBranchNode") || js.contains("andOrNotNode") || js.contains("logicConditionEditor"),
                "7.15 must not expose AND/OR/NOT or condition branch nodes");
        requireFalse(js.contains("conditionEngineEditor") || js.contains("saveConditionEngine") || service.contains("ConditionEngine"),
                "7.15 must not introduce ConditionEngine implementation");
        requireFalse(js.contains("raw-json-textarea") || js.contains("rawJsonEditor") || service.contains("raw JSON editor"),
                "7.15 must not introduce raw JSON editor paths");
        requireFalse(service.contains("SignalDeviceStore.update") || service.contains("SignalDeviceStore.create") || service.contains("SignalDeviceStore.delete")
                        || service.contains("SignalListenerStore.update") || service.contains("SignalListenerStore.create") || service.contains("SignalListenerStore.delete")
                        || service.contains("RegionControllerStore.createController") || service.contains("RegionControllerStore.deleteController")
                        || service.contains("ActionRelayBlockEntity.setActions"),
                "7.15 logic chain metadata must not mutate runtime devices/listeners/regions/actions");
        requireFalse(service.contains("SignalBridge.dispatch") || service.contains("SignalBridge.emit"),
                "7.15 service must not rewrite or dispatch SignalBridge runtime");
    }

    private static void testConditionEngineCore80() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_ENGINE_CORE_8_0_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_0.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String evaluator = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluator.java"), StandardCharsets.UTF_8);
        String registry = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java"), StandardCharsets.UTF_8);
        String node = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionNode.java"), StandardCharsets.UTF_8);
        String definition = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionGroupDefinition.java"), StandardCharsets.UTF_8);
        String contextModel = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java"), StandardCharsets.UTF_8);
        String result = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationResult.java"), StandardCharsets.UTF_8);
        String tests = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/ConditionEngineCoreTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "src/main/java/com/zcpu/tzzmod/condition/ConditionDefinition.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionGroupDefinition.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionNode.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionGroupMode.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationResult.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationTrace.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluator.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionTypeHandler.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionPredicate.java",
                "src/main/java/com/zcpu/tzzmod/condition/ConditionValidationResult.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.0 ConditionEngine core file exists: " + file);
        }

        for (String marker : List.of(
                "8.0 ConditionEngine Core",
                "旧数据包只作为条件复杂度参考",
                "Condition has no side effects",
                "EvaluationContext",
                "ConditionResult",
                "ConditionRegistry",
                "Validation",
                "max depth",
                "max node",
                "always_true",
                "always_false",
                "AND / OR / NOT",
                "failure reason",
                "debug tree",
                "不做 GameController",
                "不做 MissionSystem",
                "不做 PhaseController",
                "不接入 VBD",
                "不接入 SignalListener",
                "不接入 RegionController",
                "不接入 ActionRelay",
                "不改 SignalBridge runtime",
                "不做 raw JSON editor",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图"
        )) {
            requireContains(context, marker, "8.0 context marker present: " + marker);
        }

        for (String marker : List.of(
                "ConditionEngine Core",
                "无副作用",
                "ConditionDefinition",
                "ConditionGroupDefinition",
                "ConditionNode",
                "ConditionGroupMode",
                "AND / OR / NOT",
                "ConditionEvaluationContext",
                "ConditionEvaluationResult",
                "ConditionRegistry",
                "Validation",
                "maxDepth",
                "maxNodes",
                "always_true",
                "always_false",
                "context_field_exists",
                "context_equals",
                "不做具体逃走中任务",
                "不做 GameController",
                "不做 MissionSystem",
                "不接入现有运行时"
        )) {
            requireContains(matrix, marker, "8.0 capability matrix marker present: " + marker);
        }

        for (String marker : List.of(
                "v1.46.0-condition-engine-core",
                "8.0 ConditionEngine Core",
                "只做 ConditionEngine Core",
                "不做具体逃走中任务",
                "不做 GameController / MissionSystem / PhaseController"
        )) {
            requireContains(readme, marker, "README 8.0 marker present: " + marker);
        }

        for (String marker : List.of(
                "ConditionNodeType.GROUP",
                "ConditionGroupMode.AND",
                "ConditionGroupMode.OR",
                "ConditionGroupMode.NOT",
                "condition_max_depth_exceeded",
                "condition_max_nodes_exceeded",
                "condition_group_not_child_count_invalid",
                "condition_duplicate_node_id"
        )) {
            requireContains(evaluator, marker, "8.0 evaluator marker present: " + marker);
        }
        for (String marker : List.of(
                "ConditionNodeType.ALWAYS_TRUE",
                "ConditionNodeType.ALWAYS_FALSE",
                "ConditionNodeType.CONTEXT_FIELD_EXISTS",
                "ConditionNodeType.CONTEXT_EQUALS",
                "condition_type_unknown",
                "condition_evaluation_exception",
                "ConditionTypeMetadata",
                "ConditionFieldSchema"
        )) {
            requireContains(registry, marker, "8.0 registry marker present: " + marker);
        }
        for (String marker : List.of(
                "enabled",
                "groupMode",
                "children",
                "ConditionNodeConfig",
                "normalized"
        )) {
            requireContains(node, marker, "8.0 node marker present: " + marker);
        }
        for (String marker : List.of(
                "stableFingerprint",
                "SHA-256",
                "CURRENT_VERSION"
        )) {
            requireContains(definition, marker, "8.0 stable fingerprint marker present: " + marker);
        }
        for (String marker : List.of(
                "playerId",
                "worldId",
                "sourceType",
                "channel",
                "deviceId",
                "listenerId",
                "regionId",
                "actionId",
                "blockPos",
                "itemStackSummary",
                "gameTime",
                "variables",
                "eventMetadata"
        )) {
            requireContains(contextModel, marker, "8.0 EvaluationContext marker present: " + marker);
        }
        for (String marker : List.of(
                "matched",
                "failureReason",
                "reasonCode",
                "childResults",
                "skipped",
                "error",
                "evaluatedNodeCount",
                "durationNanos",
                "contextSummary"
        )) {
            requireContains(result, marker, "8.0 ConditionResult debug tree marker present: " + marker);
        }
        for (String marker : List.of(
                "testBooleanGroups",
                "testNestedGroup",
                "testDisabledNode",
                "testContextConditions",
                "testUnknownAndInvalidTypes",
                "testValidationIssues",
                "testDepthAndNodeLimits",
                "testResultDebugTree"
        )) {
            requireContains(tests, marker, "8.0 condition core test marker present: " + marker);
        }

        String conditionCore = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime"));
        requireFalse(conditionCore.contains("ActionEngine.execute") || conditionCore.contains("SignalBridgeServer.emit"),
                "8.0 ConditionEngine core must not execute actions or emit signals");
        requireFalse(conditionCore.contains("SignalDeviceStore") || conditionCore.contains("SignalListenerStore")
                        || conditionCore.contains("RegionControllerStore") || conditionCore.contains("ActionRelayBlockEntity"),
                "8.0 ConditionEngine core must not integrate VBD/listener/region/action runtime stores");
        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimeMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        if (!runtimeGates86Started) {
            requireFalse(runtimeMain.contains("import com.zcpu.tzzmod.condition") || runtimeMain.contains("new ConditionEvaluator")
                            || runtimeMain.contains("ConditionRegistry.defaultRegistry"),
                    "8.0 must not wire ConditionEngine into existing runtime packages");
        }
        String webadminMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        requireFalse(webadminMain.contains("conditionEngineEditor") || webadminMain.contains("saveConditionEngine")
                        || webadminMain.contains("/api/webadmin/conditions") || webadminMain.contains("condition-groups/raw"),
                "8.0 must not add a WebAdmin ConditionEngine editor or raw JSON condition API");
        requireFalse(readme.contains("旧数据包任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.0 docs must not claim concrete old datapack tasks or high-level game systems are implemented");
    }

    private static void testConditionBasicPlayerContext81() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_BASIC_PLAYER_CONTEXT_8_1_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_1.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String nodeTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionNodeType.java"), StandardCharsets.UTF_8);
        String registry = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java"), StandardCharsets.UTF_8);
        String contextModel = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java"), StandardCharsets.UTF_8);
        String result = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationResult.java"), StandardCharsets.UTF_8);
        String tests = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/ConditionBasicPlayerContextTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_BASIC_PLAYER_CONTEXT_8_1_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_1.md",
                "src/test/java/com/zcpu/tzzmod/condition/ConditionBasicPlayerContextTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.1 file exists: " + file);
        }

        for (String marker : List.of(
                "基础玩家 / 上下文条件",
                "中文显示名",
                "中文失败原因",
                "player_exists",
                "player_has_tag",
                "player_lacks_tag",
                "player_team_equals",
                "player_gamemode_equals",
                "player_is_op",
                "channel_equals",
                "source_type_equals",
                "source_id_equals",
                "device_id_equals",
                "listener_id_equals",
                "region_id_equals",
                "action_id_equals",
                "game_time_compare",
                "event_metadata_exists",
                "event_metadata_equals",
                "不接入 VBD",
                "不接入 SignalListener",
                "不接入 RegionController",
                "不做 WebAdmin 条件可视化编辑器",
                "不做 State Variable System",
                "不做物品 / 背包 / 容器条件",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(context, marker, "8.1 context marker present: " + marker);
        }

        for (String marker : List.of(
                "player_exists",
                "player_online",
                "player_is_op",
                "player_has_tag",
                "player_lacks_tag",
                "player_team_equals",
                "player_gamemode_equals",
                "player_alive",
                "player_dead",
                "source_type_equals",
                "source_id_equals",
                "channel_equals",
                "world_equals",
                "device_id_equals",
                "listener_id_equals",
                "region_id_equals",
                "action_id_equals",
                "game_time_compare",
                "event_metadata_exists",
                "event_metadata_equals",
                "中文失败原因",
                "无 runtime integration",
                "无 WebAdmin editor",
                "无 MCP scenario requirement",
                "无截图要求",
                "无 Minecraft 启动要求"
        )) {
            requireContains(matrix, marker, "8.1 capability matrix marker present: " + marker);
        }

        for (String marker : List.of(
                "基础玩家 / 上下文条件",
                "条件显示名支持中文",
                "仍未接入 runtime",
                "不做任务/关卡",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(readme, marker, "README 8.1 marker present: " + marker);
        }

        for (String marker : List.of(
                "PLAYER_EXISTS",
                "PLAYER_ONLINE",
                "PLAYER_IS_OP",
                "PLAYER_HAS_TAG",
                "PLAYER_LACKS_TAG",
                "PLAYER_TEAM_EQUALS",
                "PLAYER_GAMEMODE_EQUALS",
                "PLAYER_ALIVE",
                "PLAYER_DEAD",
                "SOURCE_TYPE_EQUALS",
                "SOURCE_ID_EQUALS",
                "CHANNEL_EQUALS",
                "WORLD_EQUALS",
                "DEVICE_ID_EQUALS",
                "LISTENER_ID_EQUALS",
                "REGION_ID_EQUALS",
                "ACTION_ID_EQUALS",
                "GAME_TIME_COMPARE",
                "EVENT_METADATA_EXISTS",
                "EVENT_METADATA_EQUALS"
        )) {
            requireContains(nodeTypes, marker, "8.1 node type marker present: " + marker);
        }

        for (String marker : List.of(
                "玩家条件",
                "上下文条件",
                "时间条件",
                "元数据条件",
                "玩家拥有标签",
                "玩家没有标签",
                "玩家队伍匹配",
                "玩家游戏模式匹配",
                "玩家是管理员",
                "信号频道匹配",
                "来源类型匹配",
                "来源 ID 匹配",
                "设备 ID 匹配",
                "监听器 ID 匹配",
                "区域 ID 匹配",
                "动作 ID 匹配",
                "游戏时间比较",
                "事件元数据存在",
                "事件元数据匹配",
                "玩家缺少标签",
                "信号频道不匹配",
                "游戏时间不满足",
                "事件元数据不匹配",
                "SignalChannel.normalize",
                "condition_config_invalid_channel",
                "condition_config_invalid_gamemode",
                "condition_config_invalid_operator"
        )) {
            requireContains(registry, marker, "8.1 registry marker present: " + marker);
        }

        for (String marker : List.of(
                "playerOnline",
                "playerOp",
                "playerTags",
                "playerTeam",
                "playerGameMode",
                "playerAlive",
                "hasPlayerIdentity",
                "hasPlayerTag",
                "eventMetadata",
                "variables"
        )) {
            requireContains(contextModel, marker, "8.1 context model marker present: " + marker);
        }

        requireContains(result, "leaf(", "8.1 result helper keeps leaf result path");
        requireContains(result, "String label", "8.1 result helper allows Chinese labels");

        for (String marker : List.of(
                "testPlayerExistsAndOnline",
                "testPlayerOpTagsTeamGamemodeAndAliveState",
                "testContextIdConditions",
                "testGameTimeCompare",
                "testEventMetadataConditions",
                "testMissingContextSafeFailures",
                "testInvalidConfigValidation",
                "testChineseMetadataAndFailureReasons",
                "testGroupsWithNewConditions",
                "player_exists true",
                "channel_equals normalizes true",
                "game_time eq",
                "metadata equals true"
        )) {
            requireContains(tests, marker, "8.1 condition test marker present: " + marker);
        }

        String conditionCore = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime"));
        requireFalse(conditionCore.contains("ActionEngine.execute") || conditionCore.contains("SignalBridgeServer.emit"),
                "8.1 ConditionEngine conditions must not execute actions or emit signals");
        requireFalse(conditionCore.contains("SignalDeviceStore") || conditionCore.contains("SignalListenerStore")
                        || conditionCore.contains("RegionControllerStore") || conditionCore.contains("ActionRelayBlockEntity"),
                "8.1 ConditionEngine conditions must not integrate VBD/listener/region/action runtime stores");
        requireContains(context, "不做物品 / 背包 / 容器条件", "8.1 context keeps item/inventory/container deferred marker");
        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimeMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        if (!runtimeGates86Started) {
            requireFalse(runtimeMain.contains("import com.zcpu.tzzmod.condition") || runtimeMain.contains("new ConditionEvaluator")
                            || runtimeMain.contains("ConditionRegistry.defaultRegistry"),
                    "8.1 must not wire ConditionEngine into existing runtime packages");
        }
        String webadminMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        requireFalse(webadminMain.contains("conditionEngineEditor") || webadminMain.contains("saveConditionEngine")
                        || webadminMain.contains("/api/webadmin/conditions") || webadminMain.contains("condition-groups/raw"),
                "8.1 must not add a WebAdmin condition editor or raw JSON condition API");
        requireFalse(readme.contains("旧数据包任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.1 docs must not claim concrete tasks or high-level game systems are implemented");
    }

    private static void testConditionStateVariables82() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_STATE_VARIABLES_8_2_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_2.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String nodeTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionNodeType.java"), StandardCharsets.UTF_8);
        String registry = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java"), StandardCharsets.UTF_8);
        String contextModel = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java"), StandardCharsets.UTF_8);
        String statePackage = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/condition/state"));
        String tests = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/ConditionStateVariableTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_STATE_VARIABLES_8_2_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_2.md",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableScope.java",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableType.java",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableRecord.java",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableStore.java",
                "src/main/java/com/zcpu/tzzmod/condition/state/StateVariableService.java",
                "src/test/java/com/zcpu/tzzmod/condition/ConditionStateVariableTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.2 file exists: " + file);
        }

        for (String marker : List.of(
                "8.2 State Variable System",
                "状态变量系统",
                "scoreboard",
                "player tag",
                "fake player",
                "Condition 只读取状态变量",
                "StateVariableService",
                "world-scoped",
                "tzz/webadmin/state_variables.json",
                "GLOBAL",
                "PLAYER",
                "BOOLEAN",
                "INTEGER",
                "STRING",
                "state_variable_exists",
                "state_variable_bool_equals",
                "state_variable_int_compare",
                "state_variable_string_equals",
                "state_variable_string_contains",
                "不做 GameController",
                "不做 MissionSystem",
                "不做具体逃走中任务",
                "不做任何游戏关卡",
                "不接入 runtime",
                "不做 WebAdmin condition editor",
                "不提供状态变量 WebAdmin 页面/API",
                "不做物品 / 背包 / 容器条件",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(context, marker, "8.2 context marker present: " + marker);
        }

        for (String marker : List.of(
                "State Variable System",
                "GLOBAL",
                "PLAYER",
                "BOOLEAN",
                "INTEGER",
                "STRING",
                "world-scoped store",
                "state_variable_exists",
                "state_variable_bool_equals",
                "state_variable_int_compare",
                "state_variable_string_equals",
                "state_variable_string_contains",
                "中文失败原因",
                "missing player safe failure",
                "wrong type safe failure",
                "无 runtime integration",
                "无 WebAdmin condition editor",
                "无 State Variable WebAdmin 页面/API",
                "无 GameController / MissionSystem",
                "无具体任务 / 关卡",
                "无 MCP scenario requirement",
                "无截图要求",
                "无 Minecraft 启动要求"
        )) {
            requireContains(matrix, marker, "8.2 capability matrix marker present: " + marker);
        }

        for (String marker : List.of(
                "8.2 State Variable System",
                "GLOBAL / PLAYER",
                "BOOLEAN / INTEGER / STRING",
                "仍不接入 runtime",
                "不做具体任务/关卡",
                "不提供 WebAdmin condition editor",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(readme, marker, "README 8.2 marker present: " + marker);
        }

        for (String marker : List.of(
                "STATE_VARIABLE_EXISTS",
                "STATE_VARIABLE_BOOL_EQUALS",
                "STATE_VARIABLE_INT_COMPARE",
                "STATE_VARIABLE_STRING_EQUALS",
                "STATE_VARIABLE_STRING_CONTAINS"
        )) {
            requireContains(nodeTypes, marker, "8.2 node type marker present: " + marker);
        }

        for (String marker : List.of(
                "状态变量条件",
                "状态变量存在",
                "布尔状态匹配",
                "整数状态比较",
                "文本状态匹配",
                "文本状态包含",
                "作用域",
                "目标模式",
                "显式目标 ID",
                "状态变量不存在",
                "状态变量类型不匹配",
                "上下文缺少触发玩家",
                "context.stateVariables()",
                "StateVariableCompareOperator"
        )) {
            requireContains(registry, marker, "8.2 registry marker present: " + marker);
        }

        requireContains(contextModel, "StateVariableSnapshot", "8.2 EvaluationContext carries state variable snapshot");
        requireContains(contextModel, "stateVariables", "8.2 EvaluationContext exposes stateVariables");

        for (String marker : List.of(
                "StateVariableScope",
                "GLOBAL",
                "PLAYER",
                "StateVariableType",
                "BOOLEAN",
                "INTEGER",
                "STRING",
                "StateVariableStore",
                "FILE_NAME = \"state_variables.json\"",
                "WebAdminStoragePaths.resolve(server).directory()",
                "StateVariableService",
                "expectedFingerprint",
                "StateVariableValidation",
                "状态变量键不能为空",
                "状态变量值过长"
        )) {
            requireContains(statePackage, marker, "8.2 state package marker present: " + marker);
        }

        for (String marker : List.of(
                "testCompleteScopeTypeLifecycleMatrix",
                "testStoreAndService",
                "testStoreCorruptionFallback",
                "testExistsAndBooleanConditions",
                "testIntegerCompareConditions",
                "testStringConditions",
                "testConditionFailureCoverageMatrix",
                "testMissingAndTypeMismatchSafeFailures",
                "testInvalidConfigValidation",
                "testGroupIntegrationAndNoSideEffects",
                "StateVariableScope.GLOBAL",
                "StateVariableScope.PLAYER",
                "stale delete fingerprint rejected",
                "corrupt persisted record falls back",
                "player int context target",
                "exists missing player",
                "string contains missing player",
                "disabled state condition is skipped",
                "condition evaluation does not change state variable fingerprint",
                "state exists true",
                "bool equals true",
                "int eq",
                "string contains true",
                "condition evaluation has no side effects"
        )) {
            requireContains(tests, marker, "8.2 state variable test marker present: " + marker);
        }

        requireFalse(registry.contains("StateVariableService") || registry.contains("StateVariableStore"),
                "8.2 condition evaluation must read only StateVariableSnapshot, not write through store/service");
        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimeMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        if (!runtimeGates86Started) {
            requireFalse(runtimeMain.contains("StateVariableService") || runtimeMain.contains("StateVariableStore")
                            || runtimeMain.contains("ConditionNodeType.STATE_VARIABLE"),
                    "8.2 must not wire state variables into existing runtime packages");
        }
        String webadminMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        requireFalse(webadminMain.contains("conditionEngineEditor") || webadminMain.contains("stateVariableEditor")
                        || webadminMain.contains("/api/webadmin/state-variables") || webadminMain.contains("condition-groups/raw"),
                "8.2 must not add WebAdmin condition editor, state variable UI/API, or raw JSON editor");
        requireContains(context, "不做物品 / 背包 / 容器条件", "8.2 context keeps item/inventory/container deferred marker");
        requireFalse(readme.contains("旧数据包任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.2 docs must not claim concrete tasks or high-level game systems are implemented");
    }

    private static void testConditionItemInventoryContainer83() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_ITEM_INVENTORY_CONTAINER_8_3_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_3.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String nodeTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionNodeType.java"), StandardCharsets.UTF_8);
        String registry = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java"), StandardCharsets.UTF_8);
        String contextModel = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java"), StandardCharsets.UTF_8);
        String itemPackage = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/condition/item"));
        String result = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationResult.java"), StandardCharsets.UTF_8);
        String tests = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/ConditionItemInventoryContainerTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_ITEM_INVENTORY_CONTAINER_8_3_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_3.md",
                "src/main/java/com/zcpu/tzzmod/condition/item/ConditionItemStackSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/item/ConditionInventorySnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/item/ConditionContainerSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/item/ConditionItemMatcher.java",
                "src/main/java/com/zcpu/tzzmod/condition/item/ConditionItemConditions.java",
                "src/test/java/com/zcpu/tzzmod/condition/ConditionItemInventoryContainerTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.3 file exists: " + file);
        }

        for (String marker : List.of(
                "8.3 Item / Inventory / Container Conditions",
                "ConditionItemStackSnapshot",
                "ConditionInventorySnapshot",
                "ConditionContainerSnapshot",
                "ConditionItemMatcher",
                "condition-safe",
                "item_stack_exists",
                "item_stack_matches",
                "inventory_contains_item",
                "inventory_item_count_compare",
                "container_slot_empty",
                "container_slot_item_matches",
                "container_item_count_compare",
                "minecraft:air",
                "count <= 0",
                "0-based",
                "负数",
                "越界",
                "跨多个 slot 聚合",
                "eq/ne/gt/gte/lt/lte",
                "中文显示名",
                "中文 validation error",
                "中文 failureReason",
                "不做任意 NBT path",
                "不做 BlockEntity NBT path",
                "不读取 live",
                "不消耗物品",
                "不移动物品",
                "不写 store",
                "不 emit signal",
                "不执行 action",
                "不接入 runtime",
                "不做 WebAdmin condition editor",
                "不做 WebAdmin API",
                "不做 WebAdmin UI",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(context + "\n" + matrix + "\n" + readme, marker, "8.3 docs/README marker: " + marker);
        }

        for (String marker : List.of(
                "ITEM_STACK_EXISTS",
                "ITEM_STACK_MATCHES",
                "INVENTORY_CONTAINS_ITEM",
                "INVENTORY_ITEM_COUNT_COMPARE",
                "CONTAINER_SLOT_EMPTY",
                "CONTAINER_SLOT_ITEM_MATCHES",
                "CONTAINER_ITEM_COUNT_COMPARE"
        )) {
            requireContains(nodeTypes, marker, "8.3 node type marker: " + marker);
        }
        requireContains(registry, "ConditionItemConditions.register", "8.3 registry registers item/inventory/container conditions");
        requireContains(result, "物品快照存在", "8.3 result label item Chinese marker");
        requireContains(result, "背包包含物品", "8.3 result label inventory Chinese marker");
        requireContains(result, "容器槽位为空", "8.3 result label container Chinese marker");
        requireContains(contextModel, "Map<String, ConditionItemStackSnapshot> itemSnapshots", "8.3 context item snapshot map marker");
        requireContains(contextModel, "Map<String, ConditionInventorySnapshot> inventorySnapshots", "8.3 context inventory snapshot map marker");
        requireContains(contextModel, "Map<String, ConditionContainerSnapshot> containerSnapshots", "8.3 context container snapshot map marker");
        requireContains(itemPackage, "List.copyOf", "8.3 snapshot equivalent immutable copy marker");
        requireContains(itemPackage, "minecraft:air", "8.3 air empty semantics source marker");
        requireContains(itemPackage, "count <= 0", "8.3 count empty semantics source marker");
        requireContains(itemPackage, "matchingCount", "8.3 aggregate count source marker");
        requireContains(itemPackage, "slot < 0 || slot >=", "8.3 invalid slot source marker");
        requireContains(itemPackage, "物品匹配器为空", "8.3 Chinese validation/failure marker");
        requireContains(itemPackage, "快照类型不匹配", "8.3 wrong snapshot type Chinese marker");

        for (String marker : List.of(
                "empty item snapshot",
                "minecraft:air is empty",
                "count <= 0 is empty",
                "count eq",
                "count ne",
                "count gt",
                "count gte",
                "count lt",
                "count lte",
                "item exists true",
                "item stack matches true",
                "inventory contains aggregate true",
                "inventory count eq",
                "container slot empty true",
                "container slot item matches",
                "container count eq",
                "group integration with item/inventory/container",
                "evaluation does not modify item snapshot",
                "evaluation does not modify inventory snapshot",
                "evaluation does not modify container snapshot",
                "Chinese display name"
        )) {
            requireContains(tests, marker, "8.3 test matrix marker: " + marker);
        }

        String forbiddenSource = itemPackage + "\n" + contextModel;
        for (String forbidden : List.of(
                "net.minecraft.item.ItemStack",
                "net.minecraft.inventory.Inventory",
                "net.minecraft.block.entity.BlockEntity",
                "net.minecraft.server.world.ServerWorld",
                "ItemSubmitEvaluator",
                "ConsumePlanner",
                "ContainerItemConditionSupport",
                "SignalBridgeServer.emit",
                "ActionEngine.execute",
                "StateVariableService",
                "StateVariableStore"
        )) {
            requireFalse(forbiddenSource.contains(forbidden), "8.3 condition item package must not use forbidden runtime/source: " + forbidden);
        }
        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimeMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        if (!runtimeGates86Started) {
            requireFalse(runtimeMain.contains("ConditionItemStackSnapshot") || runtimeMain.contains("ConditionInventorySnapshot")
                            || runtimeMain.contains("ConditionContainerSnapshot") || runtimeMain.contains("ConditionNodeType.ITEM_STACK")
                            || runtimeMain.contains("ConditionNodeType.INVENTORY_") || runtimeMain.contains("ConditionNodeType.CONTAINER_"),
                    "8.3 must not wire item/inventory/container conditions into existing runtime packages");
        }
        String webadminMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        requireFalse(webadminMain.contains("conditionEngineEditor") || webadminMain.contains("/api/webadmin/conditions")
                        || webadminMain.contains("itemConditionEditor") || webadminMain.contains("condition-groups/raw"),
                "8.3 must not add WebAdmin condition editor/API/UI or raw JSON editor");
        requireFalse(readme.contains("旧数据包任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.3 docs must not claim concrete tasks or high-level game systems are implemented");
    }

    private static void testConditionRegionSignalLogicChain84() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_REGION_SIGNAL_LOGIC_CHAIN_8_4_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_4.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String nodeTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionNodeType.java"), StandardCharsets.UTF_8);
        String registry = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java"), StandardCharsets.UTF_8);
        String contextModel = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationContext.java"), StandardCharsets.UTF_8);
        String regionLogicPackage = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/condition/regionlogic"));
        String result = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/ConditionEvaluationResult.java"), StandardCharsets.UTF_8);
        String tests = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/ConditionRegionSignalLogicChainTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_REGION_SIGNAL_LOGIC_CHAIN_8_4_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_4.md",
                "src/main/java/com/zcpu/tzzmod/condition/regionlogic/ConditionRegionSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/regionlogic/ConditionSignalChannelSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/regionlogic/ConditionSignalHistorySnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/regionlogic/ConditionLogicChainSnapshot.java",
                "src/main/java/com/zcpu/tzzmod/condition/regionlogic/ConditionRegionSignalLogicChainConditions.java",
                "src/test/java/com/zcpu/tzzmod/condition/ConditionRegionSignalLogicChainTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.4 file exists: " + file);
        }

        for (String marker : List.of(
                "8.4 Region / Signal / Logic Chain Conditions",
                "ConditionRegionSnapshot",
                "ConditionSignalChannelSnapshot",
                "ConditionSignalHistorySnapshot",
                "ConditionLogicChainSnapshot",
                "condition-safe snapshot",
                "region_exists",
                "region_enabled",
                "player_in_region",
                "region_player_count_compare",
                "signal_channel_exists",
                "signal_channel_consumer_count_compare",
                "signal_event_count_compare",
                "logic_chain_contains_node",
                "logic_chain_contains_channel",
                "logic_chain_has_cycle",
                "logic_chain_node_count_compare",
                "eq/ne/gt/gte/lt/lte",
                "中文显示名",
                "中文 validation error",
                "中文 failureReason",
                "不读取 live world",
                "不读取 live RegionController",
                "不读取 live SignalBridge",
                "不读取 live SignalEventHistory",
                "不调用 live Logic Chain Viewer service",
                "不自动构建全局逻辑链",
                "不接入 runtime",
                "不做 WebAdmin condition editor",
                "不做 WebAdmin API",
                "不做 WebAdmin UI",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(context + "\n" + matrix + "\n" + readme, marker, "8.4 docs/README marker: " + marker);
        }

        for (String marker : List.of(
                "REGION_EXISTS",
                "REGION_ENABLED",
                "PLAYER_IN_REGION",
                "REGION_PLAYER_COUNT_COMPARE",
                "SIGNAL_CHANNEL_EXISTS",
                "SIGNAL_CHANNEL_CONSUMER_COUNT_COMPARE",
                "SIGNAL_EVENT_COUNT_COMPARE",
                "LOGIC_CHAIN_CONTAINS_NODE",
                "LOGIC_CHAIN_CONTAINS_CHANNEL",
                "LOGIC_CHAIN_HAS_CYCLE",
                "LOGIC_CHAIN_NODE_COUNT_COMPARE"
        )) {
            requireContains(nodeTypes, marker, "8.4 node type marker: " + marker);
        }
        requireContains(registry, "ConditionRegionSignalLogicChainConditions.register", "8.4 registry registers region/signal/logic chain conditions");
        requireContains(result, "区域快照存在", "8.4 result label region Chinese marker");
        requireContains(result, "信号频道快照存在", "8.4 result label signal Chinese marker");
        requireContains(result, "逻辑链包含节点", "8.4 result label logic chain Chinese marker");
        requireContains(contextModel, "Map<String, ConditionRegionSnapshot> regionSnapshots", "8.4 context region snapshot map marker");
        requireContains(contextModel, "Map<String, ConditionSignalChannelSnapshot> signalChannelSnapshots", "8.4 context signal channel snapshot map marker");
        requireContains(contextModel, "Map<String, ConditionSignalHistorySnapshot> signalHistorySnapshots", "8.4 context signal history snapshot map marker");
        requireContains(contextModel, "Map<String, ConditionLogicChainSnapshot> logicChainSnapshots", "8.4 context logic chain snapshot map marker");
        requireContains(regionLogicPackage, "List.copyOf", "8.4 snapshot equivalent immutable copy marker");
        requireContains(regionLogicPackage, "Map.copyOf", "8.4 snapshot immutable metadata marker");
        requireContains(regionLogicPackage, "playerIdsInside", "8.4 region membership source marker");
        requireContains(regionLogicPackage, "consumerCount", "8.4 signal consumer count source marker");
        requireContains(regionLogicPackage, "ConditionSignalHistorySnapshot", "8.4 signal history snapshot marker");
        requireContains(regionLogicPackage, "detectsCycle", "8.4 logic chain in-memory cycle marker");
        requireContains(regionLogicPackage, "快照类型不匹配", "8.4 wrong snapshot type Chinese marker");
        requireContains(regionLogicPackage, "比较方式必须是 eq/ne/gt/gte/lt/lte", "8.4 Chinese validation operator marker");

        for (String marker : List.of(
                "region snapshot exists",
                "region enabled true",
                "explicit playerId in region",
                "context_player missing player",
                "region player count eq",
                "signal channel exists true",
                "signal consumer count eq",
                "signal event count eq",
                "signal event optional channel filter",
                "empty chain node count",
                "logic chain node exists",
                "logic chain downstream channel exists",
                "logic chain hasCycle true",
                "logic chain node count eq",
                "group integration with region/signal/logic chain",
                "evaluation does not modify region snapshot",
                "evaluation does not modify signal snapshot",
                "evaluation does not modify logic chain snapshot",
                "Chinese display name"
        )) {
            requireContains(tests, marker, "8.4 test matrix marker: " + marker);
        }

        String forbiddenSource = regionLogicPackage + "\n" + contextModel;
        for (String forbidden : List.of(
                "RegionControllerStore",
                "RegionControllerServer",
                "SignalBridgeServer",
                "SignalEventHistory",
                "SignalListenerStore",
                "SignalDeviceStore",
                "WebAdminLogicChainService",
                "WebAdminLogicChainMetadataStore",
                "WebAdminDtos.LogicChain",
                "MinecraftServer",
                "ServerWorld",
                "ServerPlayerEntity",
                "ActionEngine.execute",
                "SignalBridgeServer.emit",
                "StateVariableService",
                "StateVariableStore"
        )) {
            requireFalse(forbiddenSource.contains(forbidden), "8.4 regionlogic package must not use forbidden runtime/source: " + forbidden);
        }
        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimeMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"),
                root.resolve("src/main/java/com/zcpu/tzzmod/condition"),
                root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        if (!runtimeGates86Started) {
            requireFalse(runtimeMain.contains("ConditionRegionSnapshot") || runtimeMain.contains("ConditionSignalChannelSnapshot")
                            || runtimeMain.contains("ConditionSignalHistorySnapshot") || runtimeMain.contains("ConditionLogicChainSnapshot")
                            || runtimeMain.contains("ConditionNodeType.REGION_EXISTS") || runtimeMain.contains("ConditionNodeType.SIGNAL_CHANNEL")
                            || runtimeMain.contains("ConditionNodeType.LOGIC_CHAIN"),
                    "8.4 must not wire region/signal/logic chain conditions into existing runtime packages");
        }
        String webadminMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"));
        requireFalse(webadminMain.contains("conditionEngineEditor") || webadminMain.contains("/api/webadmin/conditions")
                        || webadminMain.contains("regionConditionEditor") || webadminMain.contains("signalConditionEditor")
                        || webadminMain.contains("logicChainConditionEditor") || webadminMain.contains("condition-groups/raw"),
                "8.4 must not add WebAdmin condition editor/API/UI or raw JSON editor");
        requireFalse(readme.contains("旧数据包任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.4 docs must not claim concrete tasks or high-level game systems are implemented");
    }

    private static void testWebAdminConditionEditor85() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/WEBADMIN_CONDITION_EDITOR_8_5_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_5.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String scripts = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java"), StandardCharsets.UTF_8);
        String styles = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java"), StandardCharsets.UTF_8);
        String shell = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java"), StandardCharsets.UTF_8);
        String store = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminConditionGroupStore.java"), StandardCharsets.UTF_8);
        String service = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupService.java"), StandardCharsets.UTF_8);
        String catalogService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionCatalogService.java"), StandardCharsets.UTF_8);
        String operationTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminOperationType.java"), StandardCharsets.UTF_8);
        String rolePolicy = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminRolePolicy.java"), StandardCharsets.UTF_8);
        String editLocks = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/write/WebAdminEditLockService.java"), StandardCharsets.UTF_8);
        String realtimeTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/realtime/WebAdminRealtimeEventType.java"), StandardCharsets.UTF_8);
        String catalogTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionCatalogTest.java"), StandardCharsets.UTF_8);
        String groupTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupServiceTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/WEBADMIN_CONDITION_EDITOR_8_5_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_5.md",
                "docs/WEBADMIN_CONDITION_EDITOR_8_5_TEST_PLAN.md",
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminConditionGroupStore.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionCatalogService.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupService.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminConditionGroupRequest.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminConditionGroupPreviewRequest.java",
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionCatalogTest.java",
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupServiceTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.5 file exists: " + file);
        }

        for (String marker : List.of(
                "8.5 WebAdmin Condition Editor",
                "Condition Type Catalog",
                "Condition Group world-scoped store",
                "condition_groups.json",
                "structured",
                "Preview / Simulation evaluate",
                "CSRF / same-origin",
                "edit lock",
                "expectedFingerprint",
                "audit",
                "realtime",
                "不接入 runtime",
                "不把 condition group 挂到",
                "不查询 live world",
                "不提供 raw JSON editor 作为主入口",
                "不新增 MCP tool",
                "不跑 MCP scenario",
                "不生成截图",
                "不启动 Minecraft"
        )) {
            requireContains(context + "\n" + matrix + "\n" + readme, marker, "8.5 docs/README marker: " + marker);
        }

        requireContains(catalogService, "ConditionRegistry.defaultRegistry()", "8.5 catalog reads ConditionRegistry");
        requireContains(catalogService, "displayName", "8.5 catalog exposes Chinese display name");
        requireContains(catalogService, "fields", "8.5 catalog exposes field schema");
        requireContains(catalogService, "readOnly", "8.5 catalog is read-only");

        requireContains(store, "WebAdminStoragePaths.resolve(server)", "8.5 store uses world-scoped WebAdminStoragePaths");
        requireContains(store, "condition_groups.json", "8.5 store file marker");
        requireContains(store, "fingerprintFor", "8.5 store fingerprint marker");
        requireContains(store, "ConditionGroupDefinition", "8.5 store persists condition group definition");

        for (String marker : List.of(
                "/api/webadmin/condition-types",
                "/api/webadmin/condition-groups",
                "/delete",
                "/validate",
                "/preview",
                "X-TZZ-WebAdmin-CSRF",
                "isWriteSameOrigin",
                "WebAdminWriteResult"
        )) {
            requireContains(server, marker, "8.5 WebAdmin API marker: " + marker);
        }
        requireContains(server, "该接口只支持 POST。", "8.5 condition group delete is POST-only");
        requireContains(server, "Cache-Control", "8.5 P0 asset no-store cache header marker");
        requireContains(server, "no-store, max-age=0", "8.5 P0 app shell/assets avoid stale JS marker");
        requireContains(shell, "8.6-runtime-gates", "8.6 runtime gates bumped asset version marker");
        requireContains(shell, "data-asset-version", "8.5 P0 loaded asset version visible in DOM marker");
        requireContains(shell, "tzz-webadmin-asset-version", "8.5 P0 asset version meta marker");

        for (String marker : List.of(
                "WebAdminEditLockService.TARGET_CONDITION_GROUP",
                "EDIT_CONDITION_GROUP",
                "CONDITION_GROUP_CHANGED",
                "previewOnly",
                "deferredSnapshots",
                "ConditionEvaluationContext.builder",
                "stateVariables"
        )) {
            requireContains(service, marker, "8.5 condition group service marker: " + marker);
        }
        requireContains(operationTypes, "EDIT_CONDITION_GROUP", "8.5 operation type marker");
        requireContains(rolePolicy, "EDIT_CONDITION_GROUP", "8.5 role policy marker");
        requireContains(editLocks, "TARGET_CONDITION_GROUP", "8.5 edit lock target marker");
        requireContains(realtimeTypes, "CONDITION_GROUP_CHANGED", "8.5 realtime event marker");

        for (String marker : List.of(
                "#/condition-groups",
                "条件组",
                "renderConditionGroupsPage",
                "renderConditionGroupDetail",
                "data-condition-group-structured-editor",
                "condition-preview-panel",
                "previewConditionGroup",
                "saveConditionGroup",
                "openConditionGroupDeleteModal",
                "data-condition-group-create-draft-lock-note",
                "data-condition-node-compact-list",
                "data-condition-main-compact-card-model",
                "data-condition-node-modal-model",
                "data-condition-no-secondary-editor",
                "data-condition-node-edit-modal",
                "data-condition-group-edit-modal",
                "data-condition-node-modal-uses-wa-animation",
                "data-condition-node-modal-animated-closing",
                "data-condition-node-modal-capture-delegation",
                "data-condition-node-card-click-opens-editor",
                "data-condition-node-quick-action",
                "conditionNodeQuickAttrs",
                "openConditionNodeEditor",
                "saveConditionNodeEditor",
                "deleteConditionNodeFromModal",
                "addConditionNodeModalChild",
                "data-condition-node-type-select",
                "data-condition-type-selector=\"list-search-custom-ui\"",
                "data-condition-category",
                "data-condition-type",
                "data-condition-type-option",
                "data-condition-type-suite",
                "data-condition-type-single-selected",
                "handleConditionNodeModalDelegatedClick",
                "changeConditionTypeSuiteFromElement",
                "changeConditionNodeTypeFromElement",
                "changeConditionNodeType('',type)",
                "node.type=next",
                "showConditionNodeEditorModal(captureConditionNodeModalUiState())",
                "data-condition-field-kind",
                "data-condition-field-required",
                "key.endsWith('Operator')?'operator':'enum'",
                "marker:'scope'",
                "marker:'targetMode'",
                "marker:'gamemode'",
                "marker:'sourceType'",
                "data-condition-field-editor=\"boolean\"",
                "data-condition-field-help",
                "conditionFieldPlaceholder",
                "data-condition-node-safe-actions",
                "data-no-native-condition-type-select",
                "data-no-condition-type-grid-buttons",
                "data-condition-type-event-delegation",
                "conditionDraftRoot(){const def=ensureConditionGroupDefinitionDraft",
                "conditionNodeByPath(path,rootOverride=null)",
                "toast('未找到要切换类型的条件节点。')",
                "syncConditionNodeModalDraftFromForm",
                "captureConditionNodeModalUiState",
                "restoreConditionNodeModalUiState",
                "nodeListScrollTop",
                "typeListScrollTop",
                "condition-node-discard-confirm",
                "appState.modalDirtyChecker=dirtyChecker",
                "definitionMissing",
                "data-condition-modal-scroll-restore",
                "data-condition-dirty-confirm-one-shot",
                "function captureViewState()",
                "windowScrollTop:window.scrollY",
                "appState.conditionNodeEditor?.open",
                "conditionGroupSavePayload",
                "TZZ_WEBADMIN_ASSET_VERSION",
                "scheduleConditionEditorRerender",
                "targetId:d.lockTargetId||d.id",
                "targetId,lockId:d.lockId",
                "location.hash===target",
                "condition_group",
                "条件类型目录",
                "测试评估"
        )) {
            requireContains(scripts + "\n" + shell, marker, "8.5 frontend marker: " + marker);
        }
        requireContains(styles, "condition-editor-layout", "8.5 condition editor layout CSS marker");
        requireContains(styles, "condition-node-compact-card", "8.5 compact condition node card CSS marker");
        requireContains(styles, "condition-node-modal-layer", "8.5 independent condition node modal CSS marker");
        requireContains(styles, "condition-node-edit-modal", "8.5 independent condition node edit modal CSS marker");
        requireContains(styles, "condition-type-selector-layout", "8.5 list/search condition type picker CSS marker");
        requireContains(styles, "condition-type-option", "8.5 readable condition type option CSS marker");
        requireContains(styles, "condition-specific-fields", "8.5 condition field editor CSS marker");
        requireContains(styles, "min-width:0;box-sizing:border-box", "8.5 preview input width guard marker");
        requireContains(styles, "condition-preview-card", "8.5 preview CSS marker");
        requireFalse(scripts.contains("condition-raw-json-editor") || scripts.contains("conditionGroupRawJson"),
                "8.5 frontend must not add raw JSON condition editor as primary UI");
        requireFalse(scripts.contains("data-condition-node-secondary-editor") || scripts.contains("condition-node-secondary-card"),
                "8.5 P0 condition node editing must not stay in the old right-side secondary editor markup");
        requireFalse(scripts.contains("condition-type-choice"),
                "8.5 P0 condition type picker must not render the unreadable legacy grid choice markup");
        requireFalse(scripts.contains("document.querySelectorAll('[data-condition-node-type-select][data-condition-node-path]')"),
                "8.5 P0 condition save must not depend on stale hidden select scans");
        requireFalse(scripts.contains("onclick=\"changeConditionNodeType('{") || scripts.contains("onclick=\"changeConditionNodeType([") || scripts.contains("datalist"),
                "8.5 P0 condition type picker must not inline JSON or use native datalist/page popup controls");
        requireFalse(scripts.contains("onchange=\"changeConditionNodeType("),
                "8.5 P0 condition type picker must not use broken native select onchange handler");
        requireFalse(scripts.contains("setValueAndClosePopup") || scripts.contains("PagePopupController"),
                "8.5 P0 frontend must not contain stale browser popup error handlers");

        for (String marker : List.of(
                "catalog includes 8.0 core condition types",
                "condition catalog is read-only",
                "condition type has Chinese display name",
                "operator field exposes compare options",
                "context_equals",
                "state_variable_bool_equals",
                "inventory_contains_item"
        )) {
            requireContains(catalogTest, marker, "8.5 catalog test marker: " + marker);
        }
        for (String marker : List.of(
                "create condition group",
                "list contains one group",
                "update changes fingerprint",
                "stale expectedFingerprint rejected",
                "delete condition group",
                "preview true result",
                "preview false result",
                "preview failure reason Chinese",
                "VIEWER cannot write condition groups",
                "CSRF required for condition group writes",
                "same-origin required for condition group writes"
        )) {
            requireContains(groupTest, marker, "8.5 group service test marker: " + marker);
        }
        for (String marker : List.of(
                "invalid preview does not evaluate",
                "preview does not create store file",
                "missing groupDefinition store blocks writes",
                "degraded store blocks writes",
                "condition group write requires edit lock",
                "condition group lock conflict rejected",
                "condition group write succeeds with held lock",
                "context_equals type roundtrips",
                "state_variable_bool_equals type roundtrips",
                "inventory condition type roundtrips",
                "JSON create keeps selected condition types",
                "JSON update from always_true to context_equals succeeds",
                "container_slot_item_matches",
                "region_enabled",
                "signal_event_count_compare",
                "logic_chain_has_cycle",
                "JSON missing groupDefinition rejected",
                "invalid JSON does not persist default always_true fallback",
                "unknown-type",
                "blank-type"
        )) {
            requireContains(groupTest, marker, "8.5 expanded group service test marker: " + marker);
        }

        boolean runtimeGates86Started = Files.isRegularFile(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"));
        String runtimePackages = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/signal"))
                + readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/action"))
                + readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/region"));
        if (!runtimeGates86Started) {
            requireFalse(runtimePackages.contains("WebAdminConditionGroupStore") || runtimePackages.contains("condition_groups.json")
                            || runtimePackages.contains("EDIT_CONDITION_GROUP") || runtimePackages.contains("TARGET_CONDITION_GROUP"),
                    "8.5 must not wire condition group store/editor into runtime packages");
        }
        if (!runtimeGates86Started) {
            requireFalse(service.contains("SignalBridgeServer.emit") || service.contains("ActionEngine.execute")
                            || service.contains("RegionControllerStore") || service.contains("SignalListenerStore")
                            || service.contains("SignalDeviceStore") || service.contains("WebAdminLogicChainService"),
                    "8.5 preview/service must not query live runtime services or execute side effects");
        } else {
            requireFalse(service.contains("SignalBridgeServer.emit") || service.contains("ActionEngine.execute")
                            || service.contains("RegionControllerStore") || service.contains("SignalListenerStore")
                            || service.contains("WebAdminLogicChainService"),
                    "8.6 available-list service may inspect VBD target capability but must not execute side effects or query out-of-scope runtimes");
        }
        requireFalse(readme.contains("具体任务已实现") || context.contains("旧数据包任务已实现")
                        || matrix.contains("GameController 已完成") || matrix.contains("MissionSystem 已完成"),
                "8.5 docs must not claim concrete tasks or high-level game systems are implemented");
    }

    private static void testConditionRuntimeGates86() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_5.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String gateService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateService.java"), StandardCharsets.UTF_8);
        String gateRequest = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateRequest.java"), StandardCharsets.UTF_8);
        String gateResult = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateResult.java"), StandardCharsets.UTF_8);
        String gateStore = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeGateStore.java"), StandardCharsets.UTF_8);
        String contextBuilder = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeContextBuilder.java"), StandardCharsets.UTF_8);
        String compatibility = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityService.java"), StandardCharsets.UTF_8);
        String profile = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityProfile.java"), StandardCharsets.UTF_8);
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String groupService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupService.java"), StandardCharsets.UTF_8);
        String nativeService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java"), StandardCharsets.UTF_8);
        String requestDto = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.java"), StandardCharsets.UTF_8);
        String scripts = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java"), StandardCharsets.UTF_8);
        String dispatcher = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceDispatcher.java"), StandardCharsets.UTF_8);
        String interaction = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceInteractionHandler.java"), StandardCharsets.UTF_8);
        String container = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceContainerHandler.java"), StandardCharsets.UTF_8);
        String compatibilityTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityServiceTest.java"), StandardCharsets.UTF_8);
        String gateTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateServiceTest.java"), StandardCharsets.UTF_8);
        String gateConfigTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateConfigTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateService.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateRequest.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateResult.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeContextBuilder.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityService.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeGateStore.java",
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateConfigTest.java",
                "src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateServiceTest.java",
                "src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityServiceTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.6 file exists: " + file);
        }

        for (String marker : List.of(
                "8.6 Runtime Integration I",
                "Optional Gate 原则",
                "conditionGroupId == null / empty / unset",
                "外层 ConditionGateService",
                "VBD_REDSTONE",
                "VBD_INTERACTION",
                "ITEM_SUBMIT",
                "CONTAINER_OPEN",
                "CONTAINER_CHANGE",
                "GET /api/webadmin/condition-groups/available",
                "未配置条件组 = 不拦截，保持旧逻辑",
                "backend reject incompatible bind",
                "SignalListener condition gate",
                "ActionRelay condition gate",
                "RegionController enter / exit / stay condition gate",
                "GameController / MissionSystem / PhaseController"
        )) {
            requireContains(context + "\n" + matrix + "\n" + readme, marker, "8.6 docs/README marker: " + marker);
        }

        for (String marker : List.of(
                "groupId.isBlank()",
                "ConditionGateResult.skippedResult()",
                "WebAdminConditionGroupStore.loadWithStatus",
                "condition_group_missing",
                "condition_group_validation_failed",
                "condition_group_incompatible",
                "condition_gate_exception",
                "ConditionEvaluationContext context = request",
                "evaluator.evaluateTrace"
        )) {
            requireContains(gateService, marker, "8.6 gate service marker: " + marker);
        }
        requireContains(gateRequest, "Supplier<ConditionEvaluationContext>", "8.6 gate request uses lazy context supplier");
        requireContains(gateResult, "boolean skipped", "8.6 gate result exposes skipped");
        requireContains(gateResult, "failureReason", "8.6 gate result exposes failure reason");
        requireContains(gateStore, "condition_runtime_gates.json", "8.6 runtime gate store is separate from legacy device JSON");
        requireContains(gateStore, "VirtualBlockDeviceGateConfig", "8.6 VBD gate config model");

        for (String marker : List.of(
                "ConditionItemStackSnapshot",
                "ConditionInventorySnapshot",
                "ConditionContainerSnapshot",
                "submitted_item",
                "player_inventory",
                "container",
                "StateVariableStore.getSnapshot"
        )) {
            requireContains(contextBuilder, marker, "8.6 context snapshot builder marker: " + marker);
        }
        requireFalse(contextBuilder.contains("ItemSubmitInventoryAdapter"), "8.6 context builder must not pass live itemSubmit adapter into ConditionEngine");

        for (String marker : List.of(
                "analyzeNode(definition.root()",
                "if (!node.enabled())",
                "PLAYER_TYPES",
                "STATE_VARIABLE_TYPES",
                "context_player",
                "itemKeys()",
                "inventoryKeys()",
                "containerKeys()",
                "regionKeys()",
                "signalHistoryKeys()",
                "logicChainKeys()"
        )) {
            requireContains(compatibility, marker, "8.6 compatibility marker: " + marker);
        }
        for (String marker : List.of(
                "VBD_REDSTONE",
                "VBD_BLOCKSTATE",
                "VBD_INTERACTION",
                "ITEM_SUBMIT",
                "CONTAINER_OPEN",
                "CONTAINER_CLOSE",
                "CONTAINER_CHANGE",
                "playerContext",
                "itemKeys",
                "inventoryKeys",
                "containerKeys"
        )) {
            requireContains(profile, marker, "8.6 compatibility profile marker: " + marker);
        }

        requireContains(server, "root + \"/available\"", "8.6 available list API route marker");
        requireContains(groupService, "public Map<String, Object> available", "8.6 available list service marker");
        requireContains(groupService, "ConditionRuntimeTargetType.parse", "8.6 available list target type parse marker");
        requireContains(groupService, "compatibilityService.analyze", "8.6 available list compatibility marker");
        requireContains(groupService, "incompatibleReasons", "8.6 available list returns diagnostic reasons");
        requireContains(groupService, "optionalGateMessage", "8.6 available list optional gate message marker");

        for (String field : List.of(
                "redstoneConditionGroupId",
                "blockStateConditionGroupId",
                "interactionConditionGroupId",
                "itemSubmitConditionGroupId",
                "containerOpenConditionGroupId",
                "containerCloseConditionGroupId",
                "containerChangeConditionGroupId"
        )) {
            requireContains(requestDto, field, "8.6 request DTO condition gate field: " + field);
            requireContains(nativeService, field, "8.6 native trigger service condition gate field: " + field);
            requireContains(scripts, field, "8.6 frontend condition gate field: " + field);
        }
        requireContains(nativeService, "validateGateBinding", "8.6 backend rejects incompatible binding marker");
        requireContains(nativeService, "compatibilityService.analyze", "8.6 backend compatibility validation marker");
        requireContains(nativeService, "ConditionRuntimeGateStore.updateVirtualBlockDevice", "8.6 saves gate config through runtime gate store");
        requireContains(nativeService, "fingerprintFor(device, gates)", "8.6 gate fields participate in expectedFingerprint");
        requireContains(nativeService, "conditionGroupId", "8.6 audit/fingerprint includes condition group changed fields");

        for (String marker : List.of(
                "loadConditionGateOptions",
                "conditionGateTargetTypes",
                "conditionGatePicker",
                "data-condition-runtime-gate-picker",
                "data-condition-runtime-available-list",
                "data-condition-runtime-incompatible-current",
                "data-condition-runtime-clear-incompatible",
                "clearVbdNativeConditionGate",
                "v[key]===incompatible",
                "未配置条件组 = 保持旧逻辑，不拦截",
                "触发条件组 gate",
                "itemSubmit gate",
                "data-condition-runtime-field",
                "暂无适用于此触发方式的条件组",
                "只列出兼容条件组",
                "vbdNativeTriggerPatchBody",
                "/condition-groups/available"
        )) {
            requireContains(scripts, marker, "8.6 frontend picker marker: " + marker);
        }
        requireFalse(scripts.contains("conditionGateRawJson") || scripts.contains("condition-runtime-raw-json-editor"),
                "8.6 WebAdmin picker must not introduce raw JSON editor");
        requireFalse(scripts.contains("<span>条件组 gate</span>"),
                "8.6 interaction UI must not render duplicate generic 条件组 gate picker labels");
        requireContains(scripts, "interactionConditionGroupId:['触发条件组 gate'",
                "8.6 interaction runtime gate label is distinct");
        requireContains(scripts, "itemSubmitConditionGroupId:['itemSubmit gate'",
                "8.6 itemSubmit gate label is distinct and compact");
        requireFalse(scripts.contains("itemSubmit 提交 gate"),
                "8.6 itemSubmit gate label stays compact");

        for (String marker : List.of(
                "ConditionRuntimeTargetType.VBD_REDSTONE",
                "ConditionRuntimeTargetType.VBD_BLOCKSTATE",
                "evaluateGate(",
                "ConditionRuntimeGateStore.conditionGroupId",
                "ConditionRuntimeContextBuilder.base",
                "if (!gate.allowed())",
                "SignalBridgeServer.emit"
        )) {
            requireContains(dispatcher, marker, "8.6 VBD redstone/blockstate gate marker: " + marker);
        }
        requireGateReturnBeforeSideEffects(
                dispatcher,
                "ConditionRuntimeTargetType.VBD_REDSTONE",
                "SignalDeviceStore.recordVirtualBlockTrigger",
                "8.6 redstone gate false returns before emit/record side effects"
        );
        requireGateReturnBeforeSideEffects(
                dispatcher,
                "ConditionRuntimeTargetType.VBD_BLOCKSTATE",
                "SignalDeviceStore.recordVirtualConditionTrigger",
                "8.6 blockstate gate false returns before emit/record side effects"
        );
        for (String marker : List.of(
                "ConditionRuntimeTargetType.VBD_INTERACTION",
                "ConditionRuntimeTargetType.ITEM_SUBMIT",
                "ConditionRuntimeContextBuilder.interaction",
                "ConditionRuntimeContextBuilder.itemSubmit",
                "evaluateItemSubmit",
                "if (!gate.allowed())"
        )) {
            requireContains(interaction, marker, "8.6 interaction/itemSubmit gate marker: " + marker);
        }
        requireTrue(interaction.indexOf("ConditionRuntimeTargetType.ITEM_SUBMIT") >= 0
                        && interaction.indexOf("ConditionRuntimeTargetType.ITEM_SUBMIT") < interaction.indexOf("evaluateItemSubmit"),
                "8.6 itemSubmit gate runs before itemSubmit evaluation/consume path");
        for (String marker : List.of(
                "ConditionRuntimeTargetType.CONTAINER_OPEN",
                "ConditionRuntimeTargetType.CONTAINER_CLOSE",
                "ConditionRuntimeTargetType.CONTAINER_CHANGE",
                "ConditionRuntimeContextBuilder.container",
                "ConditionRuntimeGateStore.conditionGroupId",
                "if (!gate.allowed())"
        )) {
            requireContains(container, marker, "8.6 container gate marker: " + marker);
        }

        for (String marker : List.of(
                "blank conditionGroupId does not read store",
                "blank conditionGroupId does not build EvaluationContext",
                "missing group fails closed",
                "invalid group fails closed",
                "incompatible group fails closed",
                "corrupt runtime gate store fails closed",
                "condition_runtime_gate_store_unavailable",
                "false condition gate blocks",
                "context/evaluation exception fails closed",
                "Inventory open runtime accepts and evaluates container condition",
                "non-Inventory open runtime rejects container condition"
        )) {
            requireContains(gateTest, marker, "8.6 gate test marker: " + marker);
        }
        for (String marker : List.of(
                "always_true compatible",
                "player not compatible with redstone",
                "context player state rejected",
                "variables.* context fields are not advertised",
                "event.trigger metadata is provided",
                "inventory not compatible with redstone",
                "container compatible with change",
                "without target Inventory capability",
                "Inventory container open supports container snapshot group",
                "Inventory container close supports container snapshot group",
                "disabled incompatible player node ignored",
                "incompatibility reason is Chinese"
        )) {
            requireContains(compatibilityTest, marker, "8.6 compatibility test marker: " + marker);
        }
        for (String marker : List.of(
                "backend rejects gate binding",
                "condition_group_missing",
                "condition_group_disabled",
                "condition_group_validation_failed",
                "condition_group_incompatible",
                "compatible player group accepted by interaction gate backend",
                "compatible player group accepted by itemSubmit gate backend",
                "Inventory container open accepts container_slot_item_matches group",
                "non-Inventory container open rejects container_slot_item_matches group",
                "non-Inventory container close rejects container_slot_item_matches group"
        )) {
            requireContains(gateConfigTest, marker, "8.6 backend gate config test marker: " + marker);
        }

        String nonVbdSignal = readJavaDirectory(
                root.resolve("src/main/java/com/zcpu/tzzmod/signal"),
                root.resolve("src/main/java/com/zcpu/tzzmod/signal/device")
        );
        String actionRuntime = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/action"));
        String regionRuntime = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod/region"));
        String actionRelay = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/ModBlock/entity/ActionRelayBlockEntity.java"), StandardCharsets.UTF_8);
        for (String runtime : List.of(nonVbdSignal, actionRuntime, regionRuntime, actionRelay)) {
            requireFalse(runtime.contains("ConditionRuntimeGateStore") || runtime.contains("condition_runtime_gates.json"),
                    "8.6/8.7 receiver-side gates must not reuse the VBD runtime gate store inside receiver runtimes");
        }
        String allMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"));
        requireFalse(allMain.contains("MissionSystem") || allMain.contains("GameController") || allMain.contains("PhaseController"),
                "8.6 must not add GameController/MissionSystem/PhaseController");
        requireFalse(allMain.contains("condition-runtime-raw-json-editor") || allMain.contains("scriptExpression"),
                "8.6 must not add raw JSON editor or generic script expression");
    }

    private static void testConditionRuntimeReceiverGates87() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_RUNTIME_RECEIVER_GATES_8_7_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String targetTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeTargetType.java"), StandardCharsets.UTF_8);
        String profile = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityProfile.java"), StandardCharsets.UTF_8);
        String builder = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeContextBuilder.java"), StandardCharsets.UTF_8);
        String signalBridge = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalBridgeServer.java"), StandardCharsets.UTF_8);
        String signalData = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalListenerData.java"), StandardCharsets.UTF_8);
        String signalService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerBasicConfigService.java"), StandardCharsets.UTF_8);
        String dtos = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminDtos.java"), StandardCharsets.UTF_8);
        String signalConfigSurface = signalService + "\n" + dtos;
        String relay = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/ModBlock/entity/ActionRelayBlockEntity.java"), StandardCharsets.UTF_8);
        String relayService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String regionData = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/region/RegionControllerData.java"), StandardCharsets.UTF_8);
        String regionTracker = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/region/RegionControllerTracker.java"), StandardCharsets.UTF_8);
        String regionCommand = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/region/RegionControllerCommand.java"), StandardCharsets.UTF_8);
        String regionService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java"), StandardCharsets.UTF_8);
        String scripts = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java"), StandardCharsets.UTF_8);
        String compatibilityTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityServiceTest.java"), StandardCharsets.UTF_8);
        String gateConfigTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateConfigTest.java"), StandardCharsets.UTF_8);
        String availableListTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupServiceTest.java"), StandardCharsets.UTF_8);

        for (String marker : List.of(
                "8.7 Receiver-side Runtime Gates",
                "SignalListener gate",
                "ActionRelay gate",
                "RegionController enter / exit / stay gate",
                "未配置 conditionGroupId",
                "单条 Action condition gate",
                "SignalReceiver redstone output gate",
                "GameController / MissionSystem / PhaseController"
        )) {
            requireContains(context, marker, "8.7 context marker: " + marker);
        }
        for (String marker : List.of("SIGNAL_LISTENER", "ACTION_RELAY", "REGION_ENTER", "REGION_EXIT", "REGION_STAY")) {
            requireContains(targetTypes, marker, "8.7 target type exists: " + marker);
            requireContains(profile, marker, "8.7 compatibility profile exists: " + marker);
        }
        for (String marker : List.of(
                "signalListener(SignalEvent event, SignalListenerData listener)",
                "actionRelay(",
                "regionController(",
                "regionSnapshot(\"region\"",
                "regionSnapshot(\"current_region\""
        )) {
            requireContains(builder, marker, "8.7 context builder marker: " + marker);
        }
        for (String marker : List.of(
                "String conditionGroupId",
                "ConditionGroupIds.normalize(conditionGroupId)"
        )) {
            requireContains(signalData, marker, "8.7 SignalListener data marker: " + marker);
        }
        for (String runtimeData : List.of(signalData, regionData, relay)) {
            requireFalse(runtimeData.contains("WebAdminConditionGroupStore"),
                    "8.7 receiver runtime data normalizes conditionGroupId without depending on WebAdminConditionGroupStore");
        }
        for (String marker : List.of(
                "ConditionRuntimeTargetType.SIGNAL_LISTENER",
                "ConditionRuntimeContextBuilder.signalListener",
                "if (!gate.allowed())",
                "skippedConditionCount",
                "conditionSkipSuffix"
        )) {
            requireContains(signalBridge, marker, "8.7 SignalListener runtime gate marker: " + marker);
        }
        requireTrue(signalBridge.indexOf("ConditionRuntimeTargetType.SIGNAL_LISTENER") < signalBridge.indexOf("executeListenerActions"),
                "8.7 SignalListener gate runs before listener actions");
        requireTrue(signalBridge.indexOf("if (!gate.allowed())") < signalBridge.indexOf("LAST_TRIGGER_TICKS.put"),
                "8.7 SignalListener blocked gate does not advance cooldown timestamp");

        for (String marker : List.of(
                "CONDITION_GROUP_ID_KEY",
                "conditionGroupId()",
                "setConditionGroupId",
                "ConditionRuntimeTargetType.ACTION_RELAY",
                "ConditionRuntimeContextBuilder.actionRelay",
                "if (!gate.allowed())",
                "if (!manual)"
        )) {
            requireContains(relay, marker, "8.7 ActionRelay runtime gate marker: " + marker);
        }
        requireTrue(relay.indexOf("ConditionRuntimeTargetType.ACTION_RELAY") < relay.indexOf("ActionContext context = new ActionContext"),
                "8.7 ActionRelay gate runs before action context/actions");
        requireTrue(relay.indexOf("if (!gate.allowed())") < relay.indexOf("updateLastRun"),
                "8.7 ActionRelay blocked gate returns before run history update");

        for (String marker : List.of(
                "enterConditionGroupId",
                "exitConditionGroupId",
                "stayConditionGroupId"
        )) {
            requireContains(regionData, marker, "8.7 RegionController data marker: " + marker);
            requireContains(regionService, marker, "8.7 RegionController WebAdmin marker: " + marker);
        }
        for (String marker : List.of(
                "ConditionRuntimeTargetType.REGION_ENTER",
                "ConditionRuntimeTargetType.REGION_EXIT",
                "ConditionRuntimeTargetType.REGION_STAY",
                "ConditionRuntimeContextBuilder.regionController",
                "executeActionsForTest",
                "conditionGroupIdFor"
        )) {
            requireContains(regionTracker, marker, "8.7 RegionController runtime marker: " + marker);
        }
        requireContains(regionCommand, "RegionControllerTracker.executeActionsForTest", "8.7 /tzz regionctl test uses gate-aware helper");
        requireTrue(regionTracker.indexOf("if (!gate.allowed())") < regionTracker.indexOf("executeActionListWithSingleActionGates"),
                "8.7 RegionController gate runs before action list");
        requireTrue(regionTracker.contains("executeActions(player, controller, RegionTriggerType.STAY);\n        state.lastStayTriggerTicks.put(controller.id(), currentTick);")
                        || regionTracker.contains("executeActions(player, controller, RegionTriggerType.STAY);\r\n        state.lastStayTriggerTicks.put(controller.id(), currentTick);"),
                "8.7 Region stay records interval tick even when gate blocks action list");

        for (String marker : List.of(
                "WebAdminConditionGateBindingValidator",
                "ConditionRuntimeTargetType.SIGNAL_LISTENER",
                "conditionGroupId",
                "conditionGateTargetType",
                "conditionGateTargetId"
        )) {
            requireContains(signalConfigSurface, marker, "8.7 SignalListener WebAdmin config marker: " + marker);
        }
        for (String marker : List.of(
                "ConditionRuntimeTargetType.ACTION_RELAY",
                "conditionGroupId",
                "setConditionGroupId",
                "validateRequest(server, request, gateBindingValidator)",
                "fingerprintFor(device, actions,",
                "未配置条件组 = 保持旧继电器逻辑"
        )) {
            requireContains(relayService, marker, "8.7 ActionRelay WebAdmin config marker: " + marker);
        }
        for (String marker : List.of(
                "ConditionRuntimeTargetType.REGION_ENTER",
                "ConditionRuntimeTargetType.REGION_EXIT",
                "ConditionRuntimeTargetType.REGION_STAY",
                "conditionRuntimeGates",
                "未配置条件组 = 保持旧区域控制器逻辑"
        )) {
            requireContains(regionService, marker, "8.7 RegionController WebAdmin config marker: " + marker);
        }
        for (String marker : List.of(
                "loadRuntimeConditionGateOptions",
                "runtimeConditionGatePicker",
                "draft.conditionGroupId=document.getElementById('runtime-condition-conditionGroupId')?.value??''",
                "<option value=\"\" ${current?'':'selected'}>未配置条件组</option>",
                "conditionGroupId:'外层条件组'",
                "enterConditionGroupId:'进入动作条件组'",
                "conditionGroupId:String(draft.conditionGroupId||'')",
                "enterConditionGroupId:String(draft.enterConditionGroupId||'')",
                "exitConditionGroupId:String(draft.exitConditionGroupId||'')",
                "stayConditionGroupId:String(draft.stayConditionGroupId||'')",
                "SIGNAL_LISTENER",
                "ACTION_RELAY",
                "REGION_ENTER",
                "REGION_EXIT",
                "REGION_STAY",
                "未配置条件组 = 保持旧逻辑，不拦截",
                "data-region-controller-condition-gates"
        )) {
            requireContains(scripts, marker, "8.7 frontend condition gate marker: " + marker);
        }
        for (String marker : List.of(
                "generic SignalListener profile rejects player-dependent condition",
                "generic ActionRelay profile rejects player-dependent condition",
                "Region enter supports region snapshot key",
                "Region stay supports current_region snapshot key"
        )) {
            requireContains(compatibilityTest, marker, "8.7 compatibility test marker: " + marker);
        }
        for (String marker : List.of(
                "8.7 backend rejects receiver gate binding",
                "always_true group accepted by SignalListener receiver gate backend",
                "always_true group accepted by ActionRelay receiver gate backend",
                "player group accepted by Region stay gate backend"
        )) {
            requireContains(gateConfigTest, marker, "8.7 backend gate config test marker: " + marker);
        }
        for (String marker : List.of(
                "SignalListener available excludes player context group",
                "ActionRelay available includes relayId context group",
                "REGION_ENTER",
                "REGION_EXIT",
                "REGION_STAY"
        )) {
            requireContains(availableListTest, marker, "8.7 available-list test marker: " + marker);
        }

        String allMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"));
        requireFalse(allMain.contains("MissionSystem") || allMain.contains("GameController") || allMain.contains("PhaseController"),
                "8.7 must not add GameController/MissionSystem/PhaseController");
        requireFalse(allMain.contains("singleActionConditionGroupId") || allMain.contains("SignalReceiver condition gate"),
                "8.7 must not add single Action gates or SignalReceiver output gate");
    }

    private static void testConditionRuntimeDebugger88() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_RUNTIME_DEBUGGER_8_8_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_8.md"), StandardCharsets.UTF_8);
        String manualTest = Files.readString(root.resolve("docs/test/测试_8.8_WebAdmin条件模拟诊断回放验收.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String history = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistory.java"), StandardCharsets.UTF_8);
        String historyRecord = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryRecord.java"), StandardCharsets.UTF_8);
        String debugNode = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateDebugNode.java"), StandardCharsets.UTF_8);
        String replayService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayService.java"), StandardCharsets.UTF_8);
        String replayResult = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayResult.java"), StandardCharsets.UTF_8);
        String gateService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateService.java"), StandardCharsets.UTF_8);
        String webHistoryService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateHistoryService.java"), StandardCharsets.UTF_8);
        String doctorService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorService.java"), StandardCharsets.UTF_8);
        String webAdminDoctorService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminDoctorService.java"), StandardCharsets.UTF_8);
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String scripts = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java"), StandardCharsets.UTF_8);
        String shell = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java"), StandardCharsets.UTF_8);
        String realtime = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/realtime/WebAdminRealtimeEventType.java"), StandardCharsets.UTF_8);
        String dtos = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/dto/WebAdminDtos.java"), StandardCharsets.UTF_8);
        String vbdService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java"), StandardCharsets.UTF_8);
        String listenerService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerBasicConfigService.java"), StandardCharsets.UTF_8);
        String relayService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String regionService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java"), StandardCharsets.UTF_8);
        String historyTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryServiceTest.java"), StandardCharsets.UTF_8);
        String replayTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayServiceTest.java"), StandardCharsets.UTF_8);
        String doctorTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorServiceTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_RUNTIME_DEBUGGER_8_8_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_8.md",
                "docs/test/测试_8.8_WebAdmin条件模拟诊断回放验收.md",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistory.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryRecord.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayService.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayResult.java",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateDebugNode.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateHistoryService.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorService.java",
                "src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryServiceTest.java",
                "src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayServiceTest.java",
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorServiceTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.8 file exists: " + file);
        }

        for (String marker : List.of(
                "8.8 Condition Runtime Debugger / Doctor / Simulation",
                "runtime history",
                "debug tree",
                "Replay 只读",
                "不读取 live world / player / inventory / region / SignalBridge",
                "不写 store、不 emit signal、不执行 action",
                "内存环形缓冲",
                "最大 200 条",
                "未配置 conditionGroupId",
                "SignalReceiver gate",
                "单条 Action gate",
                "GameController / MissionSystem / PhaseController",
                "#/condition-debugger",
                "/api/webadmin/condition-gates/history"
        )) {
            requireContains(context + "\n" + matrix + "\n" + manualTest + "\n" + readme, marker, "8.8 docs/README marker: " + marker);
        }

        for (String marker : List.of(
                "public static final int MAX_RECORDS = 200",
                "result == null || result.skipped() || result.conditionGroupId().isBlank()",
                "context.summary()",
                "ConditionGateDebugNode.from",
                "context,",
                "definition",
                "CONDITION_GATE_HISTORY_APPENDED",
                "routeTarget(\"#/condition-debugger\")"
        )) {
            requireContains(history, marker, "8.8 history marker: " + marker);
        }
        for (String marker : List.of(
                "contextSummary",
                "debugTree",
                "replayContext",
                "definitionSnapshot",
                "no live world/player/inventory/service references",
                "compactDto",
                "detailDto",
                "replayReadOnly",
                "noActionExecution",
                "noSignalEmit",
                "noRawJsonEditor"
        )) {
            requireContains(historyRecord, marker, "8.8 history record marker: " + marker);
        }
        requireContains(debugNode, "childResults", "8.8 debug node preserves child debug tree");
        requireContains(gateService, "ConditionGateHistory.record", "8.8 gate service records configured gate results");
        requireTrue(gateService.indexOf("groupId.isBlank()") < gateService.indexOf("ConditionGateHistory.record"),
                "8.8 blank conditionGroupId still returns before history recording");

        for (String marker : List.of(
                "record.replayContext()",
                "record.definitionSnapshot()",
                "condition_gate_replay_group_deleted",
                "boolean changed",
                "历史快照",
                "Replay 只使用历史 ConditionEvaluationContext 快照",
                "Replay 不写 store、不 emit signal、不执行 action",
                "ConditionGateDebugNode.from"
        )) {
            requireContains(replayService, marker, "8.8 replay service marker: " + marker);
        }
        for (String marker : List.of("boolean readOnly", "boolean noSideEffects", "boolean noLiveWorldRead", "originalResult", "replayResult", "resultConsistent")) {
            requireContains(replayResult, marker, "8.8 replay result marker: " + marker);
        }
        requireFalse(replayService.contains("SignalBridgeServer.emit") || replayService.contains("ActionEngine.execute")
                        || replayService.contains("SignalDeviceStore") || replayService.contains("RegionControllerStore")
                        || replayService.contains("ConditionRuntimeContextBuilder") || replayService.contains("ServerPlayerEntity")
                        || replayService.contains("ServerWorld") || replayService.contains("Inventory")
                        || replayService.contains("StateVariableStore"),
                "8.8 replay must not emit signals, execute actions, or read live runtime stores");

        for (String marker : List.of(
                "condition-runtime-missing-group",
                "condition-runtime-disabled-group",
                "condition-runtime-invalid-group",
                "condition-runtime-incompatible-group",
                "compatibility.message()",
                "condition-runtime-always-false-node",
                "continue;"
        )) {
            requireContains(doctorService, marker, "8.8 doctor service marker: " + marker);
        }
        requireContains(webAdminDoctorService, "conditionRuntimeDoctorService.inspect", "8.8 WebAdmin doctor includes condition runtime diagnostics");

        for (String marker : List.of(
                "/api/webadmin/condition-gates/history",
                "handleConditionGateHistory",
                "conditionGateHistoryService.list",
                "conditionGateHistoryService.detail",
                "conditionGateHistoryService.replay",
                "该接口只支持 GET",
                "该接口只支持 POST"
        )) {
            requireContains(server, marker, "8.8 WebAdmin API marker: " + marker);
        }
        for (String marker : List.of(
                "readOnly",
                "inMemory",
                "worldScoped",
                "maxRecords",
                "targetType",
                "conditionGroupId",
                "recentStatus",
                "#/condition-debugger/"
        )) {
            requireContains(webHistoryService, marker, "8.8 WebAdmin history service marker: " + marker);
        }

        for (String marker : List.of(
                "#/condition-debugger",
                "条件调试",
                "data-condition-debugger-page",
                "data-condition-gate-list-route",
                "data-condition-gate-detail-route",
                "data-condition-gate-detail-full-width",
                "data-condition-gate-row-click-navigates-detail",
                "data-condition-gate-not-found",
                "data-condition-gate-history-table",
                "data-condition-gate-detail-summary",
                "data-condition-gate-context-summary",
                "data-condition-gate-debug-tree-section",
                "data-condition-gate-replay-section",
                "data-condition-gate-replay-result",
                "data-condition-gate-technical-collapsed-readonly",
                "data-condition-gate-return-preserves-filters",
                "data-condition-gate-scroll-preservation",
                "data-condition-gate-detail-refresh-stays-detail",
                "data-condition-gate-realtime-preserves-detail",
                "data-condition-gate-replay-readonly",
                "data-condition-gate-no-action-execution",
                "data-condition-gate-no-signal-emit",
                "data-condition-gate-no-consume",
                "data-condition-gate-no-live-world-read",
                "data-condition-gate-no-raw-json-editor",
                "data-condition-gate-debug-tree",
                "condition_gate_history_appended",
                "conditionDebuggerFilters",
                "conditionDebuggerDetailHash",
                "replayConditionGateHistory",
                "模拟重放",
                "真实重放 Action"
        )) {
            requireContains(scripts + "\n" + shell, marker, "8.8 frontend marker: " + marker);
        }
        requireFalse(scripts.contains("data-condition-gate-debug-detail") || scripts.contains("conditionDebuggerDetailPanel("),
                "8.8 condition debugger full detail must not remain in the old narrow right panel");
        requireContains(realtime, "CONDITION_GATE_HISTORY_APPENDED", "8.8 realtime event type marker");
        requireContains(dtos, "recentConditionGate", "8.8 DTO recent gate status marker");
        for (String serviceSource : List.of(vbdService, listenerService, relayService, regionService)) {
            requireContains(serviceSource, "recentConditionGate", "8.8 existing page service exposes recent condition gate status");
        }

        for (String marker : List.of(
                "blank conditionGroupId records no runtime history",
                "history ring buffer max records",
                "history list API filters result and targetType",
                "history detail API exposes debug tree",
                "recent status links debugger"
        )) {
            requireContains(historyTest, marker, "8.8 history test marker: " + marker);
        }
        for (String marker : List.of(
                "replay allowed record succeeds",
                "replay changed condition group succeeds from historical snapshot",
                "replay deleted condition group fails safely",
                "replay missing record fails safely",
                "replay record without context snapshot fails safely",
                "replay record without definition snapshot fails safely"
        )) {
            requireContains(replayTest, marker, "8.8 replay test marker: " + marker);
        }
        for (String marker : List.of(
                "condition-runtime-missing-group",
                "condition-runtime-disabled-group",
                "condition-runtime-invalid-group",
                "condition-runtime-incompatible-group",
                "context_player",
                "容器快照",
                "物品快照",
                "背包快照",
                "信号历史快照",
                "condition-runtime-definition-missing",
                "dynamic container open/close compatibility profile",
                "doctor does not report deferred SignalReceiver or Signal Join / Barrier / Aggregator as missing errors"
        )) {
            requireContains(doctorTest, marker, "8.8 doctor test marker: " + marker);
        }

        String allMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"));
        requireFalse(allMain.contains("singleActionConditionGroupId") || allMain.contains("SignalReceiver condition gate"),
                "8.8 must not add single Action gates or SignalReceiver output gate");
        requireFalse(allMain.contains("MissionSystem") || allMain.contains("GameController") || allMain.contains("PhaseController"),
                "8.8 must not add GameController/MissionSystem/PhaseController");
        requireFalse(scripts.contains("conditionGateRawJson") || scripts.contains("condition-runtime-raw-json-editor") || scripts.contains("rawJsonEditor"),
                "8.8 condition debugger must not expose a raw JSON editor");
    }

    private static void testConditionRuntimeSingleActionGates89() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String context = Files.readString(root.resolve("docs/CONDITION_RUNTIME_SINGLE_ACTION_GATES_8_9_CURRENT_CONTEXT.md"), StandardCharsets.UTF_8);
        String matrix = Files.readString(root.resolve("docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_9.md"), StandardCharsets.UTF_8);
        String readme = Files.readString(root.resolve("README.md"), StandardCharsets.UTF_8);
        String actionConfig = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/action/ActionConfig.java"), StandardCharsets.UTF_8);
        String actionEngine = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/action/ActionEngine.java"), StandardCharsets.UTF_8);
        String actionGateService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionActionGateService.java"), StandardCharsets.UTF_8);
        String targetTypes = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeTargetType.java"), StandardCharsets.UTF_8);
        String profile = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityProfile.java"), StandardCharsets.UTF_8);
        String builder = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionRuntimeContextBuilder.java"), StandardCharsets.UTF_8);
        String request = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateRequest.java"), StandardCharsets.UTF_8);
        String historyRecord = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryRecord.java"), StandardCharsets.UTF_8);
        String signalBridge = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/signal/SignalBridgeServer.java"), StandardCharsets.UTF_8);
        String relay = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/ModBlock/entity/ActionRelayBlockEntity.java"), StandardCharsets.UTF_8);
        String regionTracker = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/region/RegionControllerTracker.java"), StandardCharsets.UTF_8);
        String server = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java"), StandardCharsets.UTF_8);
        String scripts = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java"), StandardCharsets.UTF_8);
        String conditionGroupService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupService.java"), StandardCharsets.UTF_8);
        String listenerService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerActionsService.java"), StandardCharsets.UTF_8);
        String relayService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java"), StandardCharsets.UTF_8);
        String regionService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java"), StandardCharsets.UTF_8);
        String doctorService = Files.readString(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorService.java"), StandardCharsets.UTF_8);
        String actionGateTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionActionGateServiceTest.java"), StandardCharsets.UTF_8);
        String compatibilityTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityServiceTest.java"), StandardCharsets.UTF_8);
        String availableListTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupServiceTest.java"), StandardCharsets.UTF_8);
        String gateConfigTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateConfigTest.java"), StandardCharsets.UTF_8);
        String doctorTest = Files.readString(root.resolve("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorServiceTest.java"), StandardCharsets.UTF_8);

        for (String file : List.of(
                "docs/CONDITION_RUNTIME_SINGLE_ACTION_GATES_8_9_CURRENT_CONTEXT.md",
                "docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_9.md",
                "src/main/java/com/zcpu/tzzmod/condition/runtime/ConditionActionGateService.java",
                "src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionActionGateServiceTest.java"
        )) {
            requireTrue(Files.isRegularFile(root.resolve(file)), "8.9 file exists: " + file);
        }

        for (String marker : List.of(
                "8.9 Single Action Runtime Gate",
                "单条 Action",
                "skip current action and continue",
                "先执行 parent/list-level gate",
                "ActionRelay 手动测试绕过",
                "SignalReceiver gate",
                "Signal Join / Barrier / Aggregator",
                "GameController / MissionSystem / PhaseController",
                "failure policy",
                "fallback action",
                "stop-list"
        )) {
            requireContains(context + "\n" + matrix + "\n" + readme, marker, "8.9 docs/README marker: " + marker);
        }

        for (String marker : List.of(
                "String conditionGroupId",
                "ConditionGroupIds.normalize(conditionGroupId)",
                "public ActionConfig(",
                "this(type, value, enabled, requiresOp, cooldownTicks, notifyOps, \"\")"
        )) {
            requireContains(actionConfig, marker, "8.9 ActionConfig marker: " + marker);
        }
        requireFalse(actionEngine.contains("ConditionActionGateService") || actionEngine.contains("ConditionGateService"),
                "8.9 action gate stays outside ActionEngine action type semantics");

        for (String marker : List.of(
                "SIGNAL_LISTENER_ACTION",
                "ACTION_RELAY_ACTION",
                "REGION_ENTER_ACTION",
                "REGION_EXIT_ACTION",
                "REGION_STAY_ACTION"
        )) {
            requireContains(targetTypes, marker, "8.9 action target type marker: " + marker);
            requireContains(profile, marker, "8.9 action compatibility profile marker: " + marker);
            requireContains(scripts, marker, "8.9 frontend action target type marker: " + marker);
        }
        requireFalse(targetTypes.contains("SIGNAL_RECEIVER_ACTION") || targetTypes.contains("SIGNAL_RECEIVER_GATE"),
                "8.9 must not add SignalReceiver action/gate target type");

        for (String marker : List.of(
                "ConditionActionGateService",
                "conditionGroupId.isBlank()",
                "ConditionRuntimeContextBuilder.withActionMetadata",
                "\"ACTION\"",
                "actionTargetId(",
                "regionActionTargetId(",
                "regionActionTargetType("
        )) {
            requireContains(actionGateService, marker, "8.9 action gate helper marker: " + marker);
        }
        for (String marker : List.of(
                "gateLevel",
                "parentTargetType",
                "parentTargetId",
                "actionIndex",
                "actionDisplayIndex",
                "actionType",
                "parentActionBucket"
        )) {
            requireContains(request, marker, "8.9 request metadata marker: " + marker);
            requireContains(historyRecord, marker, "8.9 history metadata marker: " + marker);
        }
        for (String marker : List.of(
                "withActionMetadata",
                ".actionId(actionId)",
                ".eventMetadata(\"actionIndex\"",
                ".variable(\"actionType\"",
                ".variable(\"parentTargetId\""
        )) {
            requireContains(builder, marker, "8.9 action context metadata marker: " + marker);
        }

        requireContains(signalBridge, "executeListenerActions(context, event, listener)", "8.9 SignalListener action gate has event context");
        requireContains(signalBridge, "ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION", "8.9 SignalListener action gate target");
        requireTrue(signalBridge.indexOf("ConditionRuntimeTargetType.SIGNAL_LISTENER") < signalBridge.indexOf("executeListenerActions(context, event, listener)"),
                "8.9 SignalListener list gate still runs before action list");
        requireTrue(signalBridge.indexOf("ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION") < signalBridge.indexOf("ActionEngine.execute(context, action)"),
                "8.9 SignalListener action gate runs before single action execution");
        requireContains(signalBridge, "continue;", "8.9 SignalListener action false continues later actions");

        requireContains(relay, "if (!manual)", "8.9 ActionRelay manual path remains separated");
        requireContains(relay, "ConditionRuntimeTargetType.ACTION_RELAY_ACTION", "8.9 ActionRelay action gate target");
        requireTrue(relay.indexOf("ConditionRuntimeTargetType.ACTION_RELAY") < relay.indexOf("ActionContext context = new ActionContext"),
                "8.9 ActionRelay list gate still runs before action context/actions");
        requireTrue(relay.indexOf("ConditionRuntimeTargetType.ACTION_RELAY_ACTION") < relay.indexOf("last = ActionEngine.execute(context, action)"),
                "8.9 ActionRelay action gate runs before single action execution");
        requireTrue(relay.indexOf("if (!manual)") < relay.indexOf("ConditionRuntimeTargetType.ACTION_RELAY_ACTION"),
                "8.9 ActionRelay action gates are runtime-only and manual test bypasses them");

        requireContains(regionTracker, "executeActionListWithSingleActionGates", "8.9 RegionController uses gate-aware action loop");
        requireContains(regionTracker, "ConditionRuntimeTargetType.REGION_ENTER_ACTION", "8.9 RegionController enter action gate target");
        requireContains(regionTracker, "ConditionRuntimeTargetType.REGION_EXIT_ACTION", "8.9 RegionController exit action gate target");
        requireContains(regionTracker, "ConditionRuntimeTargetType.REGION_STAY_ACTION", "8.9 RegionController stay action gate target");
        requireTrue(regionTracker.indexOf("targetTypeFor(triggerType),") < regionTracker.indexOf("executeActionListWithSingleActionGates"),
                "8.9 RegionController list gate still runs before action-level gates");
        requireContains(regionTracker, "state.lastStayTriggerTicks.put(controller.id(), currentTick);",
                "8.9 Region stay interval still advances after stay execution attempt");

        for (String marker : List.of(
                "condition-groups/available",
                "queryMap",
                "region-controllers",
                "actions",
                "PATCH"
        )) {
            requireContains(server, marker, "8.9 WebAdmin API marker: " + marker);
        }
        for (String marker : List.of(
                "available(",
                "parentTargetType",
                "parentTargetId",
                "actionType",
                "actionIndex",
                "actionBucket"
        )) {
            requireContains(conditionGroupService, marker, "8.9 available list action metadata marker: " + marker);
        }
        for (String serviceSource : List.of(listenerService, relayService, regionService)) {
            for (String marker : List.of(
                    "conditionGroupId",
                    "actionConditionGateTargetType",
                    "actionConditionGateTargetId",
                    "recentActionConditionGate",
                    "ConditionRuntimeTargetType."
            )) {
                requireContains(serviceSource, marker, "8.9 WebAdmin action service marker: " + marker);
            }
        }
        requireContains(doctorService, "SIGNAL_LISTENER_ACTION", "8.9 doctor scans SignalListener action gates");
        requireContains(doctorService, "ACTION_RELAY_ACTION", "8.9 doctor scans ActionRelay action gates");
        requireContains(doctorService, "REGION_CONTROLLER_ACTION", "8.9 doctor scans RegionController action gates");

        for (String marker : List.of(
                "data-action-condition-gate-picker",
                "data-action-condition-gate-target-type",
                "data-action-condition-gate-summary",
                "data-action-condition-gate-incompatible-current",
                "data-action-condition-gate-clear",
                "data-action-relay-action-condition-gate-picker",
                "data-signal-listener-action-condition-gate-picker",
                "data-region-controller-action-condition-gate-picker",
                "conditionGroupId:d.conditionGroupId||''",
                "conditionGroupId:document.getElementById",
                "单条条件"
        )) {
            requireContains(scripts, marker, "8.9 frontend single action gate marker: " + marker);
        }
        requireFalse(scripts.contains("condition-action-raw-json-editor") || scripts.contains("actionGateRawJson"),
                "8.9 action editor must not expose raw JSON editor");

        for (String marker : List.of(
                "legacy action JSON without conditionGroupId defaults blank",
                "blank action condition does not read group store",
                "action gate history marks gate level",
                "false single action gate returns blocked decision",
                "incompatible action gate rejects before live context builder runs"
        )) {
            requireContains(actionGateTest, marker, "8.9 action gate test marker: " + marker);
        }
        for (String marker : List.of(
                "SignalListener action available includes action metadata group",
                "ActionRelay action available includes action metadata group",
                "Region action available includes action metadata group"
        )) {
            requireContains(availableListTest, marker, "8.9 available list test marker: " + marker);
        }
        for (String marker : List.of(
                "SIGNAL_LISTENER_ACTION",
                "ACTION_RELAY_ACTION",
                "REGION_ENTER_ACTION",
                "REGION_EXIT_ACTION",
                "REGION_STAY_ACTION",
                "player group accepted by Region stay single action backend",
                "action metadata group accepted by 8.9 single action backend"
        )) {
            requireContains(gateConfigTest + "\n" + compatibilityTest + "\n" + doctorTest, marker, "8.9 backend/test marker: " + marker);
        }

        String allMain = readJavaDirectory(root.resolve("src/main/java/com/zcpu/tzzmod"));
        for (String forbidden : List.of(
                "SignalJoin",
                "SignalBarrier",
                "SignalAggregator",
                "GameController",
                "MissionSystem",
                "PhaseController",
                "ActionFailurePolicy",
                "FallbackAction",
                "StopListPolicy"
        )) {
            requireFalse(allMain.contains(forbidden), "8.9 must not add out-of-scope type: " + forbidden);
        }
    }

    private static String readJavaDirectory(Path directory, Path... excludedDirectories) throws IOException {
        StringBuilder content = new StringBuilder();
        List<Path> excluded = new ArrayList<>();
        if (excludedDirectories != null) {
            for (Path excludedDirectory : excludedDirectories) {
                if (excludedDirectory != null) {
                    excluded.add(excludedDirectory.toAbsolutePath().normalize());
                }
            }
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter((path) -> path.toString().endsWith(".java"))
                    .filter((path) -> excluded.stream()
                            .noneMatch((excludedDirectory) -> path.toAbsolutePath().normalize().startsWith(excludedDirectory)))
                    .sorted()
                    .forEach((path) -> {
                        try {
                            content.append(Files.readString(path, StandardCharsets.UTF_8)).append('\n');
                        } catch (IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    });
        } catch (java.io.UncheckedIOException exception) {
            throw exception.getCause();
        }
        return content.toString();
    }

    private static void requireTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static void requireBlank(String value, String message) {
        if (value != null && !value.isBlank()) {
            throw new AssertionError(message + ": expected blank but was <" + value + ">");
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AssertionError(message + ": expected non-blank value");
        }
    }

    private static int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static void requireContains(String value, String expectedPart, String message) {
        if (value == null || !value.contains(expectedPart)) {
            throw new AssertionError(message + ": expected <" + value + "> to contain <" + expectedPart + ">");
        }
    }

    private static void requireGateReturnBeforeSideEffects(
            String source,
            String targetMarker,
            String recordMarker,
            String message
    ) {
        int targetIndex = source.indexOf(targetMarker);
        int gateIndex = targetIndex < 0 ? -1 : source.indexOf("if (!gate.allowed())", targetIndex);
        int returnIndex = gateIndex < 0 ? -1 : source.indexOf("return;", gateIndex);
        int emitIndex = gateIndex < 0 ? -1 : source.indexOf("SignalBridgeServer.emit", gateIndex);
        int recordIndex = gateIndex < 0 ? -1 : source.indexOf(recordMarker, gateIndex);
        requireTrue(targetIndex >= 0, message + ": target marker missing");
        requireTrue(gateIndex > targetIndex, message + ": gate allowed check missing after target marker");
        requireTrue(returnIndex > gateIndex, message + ": gate false return missing");
        requireTrue(emitIndex > returnIndex, message + ": emit must stay after gate false return");
        requireTrue(recordIndex > emitIndex, message + ": trigger record must stay after guarded emit");
    }

    private static boolean hasDiagnosticCode(DeviceDiagnostic diagnostic, String code) {
        if (diagnostic == null || code == null) {
            return false;
        }
        for (DiagnosticIssue issue : diagnostic.issues()) {
            if (code.equals(issue.code())) {
                return true;
            }
        }
        return false;
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
