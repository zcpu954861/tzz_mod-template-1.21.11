package com.zcpu.tzzmod.stabilization;

import com.google.gson.Gson;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
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
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminInteractionItemMatcherService;
import com.zcpu.tzzmod.webadmin.service.WebAdminChannelMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSelectionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerLifecycleService;
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
                "Signal Listener 基础配置"
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
                "不会创建 matcher、itemSubmit、ConditionEngine",
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
        return """
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
                function apiData(path) {
                  const url = String(path);
                  if (url.startsWith('/api/webadmin/edit-locks/acquire')) return { success:true, data:{ lock:{ lockId:'lock-1', locked:true, heldByCurrentUser:true, holderUsername:'Owner', expiresAt:'2026-05-09T10:10:00Z' } } };
                  if (url.startsWith('/api/webadmin/edit-locks/release') || url.startsWith('/api/webadmin/edit-locks/heartbeat')) return { success:true, data:{ lock:{ lockId:'lock-1', locked:true, heldByCurrentUser:true, expiresAt:'2026-05-09T10:10:00Z' } } };
                  if (url.startsWith('/api/webadmin/device-metadata/')) return { success:true, changed:false, message:'ok' };
                  if (url.startsWith('/api/webadmin/device-basic-config/')) return { supported:true, enabled:true, channel:'test.channel', expectedFingerprint:'basic-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/device-extended-config/')) return { supported:true, supportedFields:['pulseTicks','cooldownTicks'], fieldLabels:{ pulseTicks:'脉冲时长', cooldownTicks:'冷却时间' }, values:{ pulseTicks:20, cooldownTicks:0 }, expectedFingerprint:'extended-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/channel-metadata')) return { channel:'test.channel', displayName:'Test Channel', effectiveDisplayName:'Test Channel', note:'', iconKey:'auto', expectedFingerprint:'channel-fp', lockStatus:{ locked:false } };
                  if (url.startsWith('/api/webadmin/selection/start')) return { success:true, targetType:'OBJECT_SELECTION', targetId:'sel-1', changed:true, message:'已通知目标玩家进入选择模式。', data:{ selection:{ selectionId:'sel-1', targetPlayerName:'Owner', purpose:'create_virtual_block_device', status:'started', channel:'test.channel' } } };
                  if (url.startsWith('/api/webadmin/selection/cancel')) return { success:true, targetType:'OBJECT_SELECTION', targetId:'sel-1', changed:true, message:'选择已取消。', data:{ selection:{ selectionId:'sel-1', targetPlayerName:'Owner', status:'cancelled', channel:'test.channel' } } };
                  if (url.startsWith('/api/webadmin/selection/status')) return { active:true, selectionId:'sel-1', status:'active', purpose:'create_virtual_block_device', targetPlayerName:'Owner', channel:'test.channel' };
                  if (url.startsWith('/api/webadmin/online-players')) return [{ name:'Owner', uuid:'00000000-0000-0000-0000-000000000001' }, { name:'Builder', uuid:'00000000-0000-0000-0000-000000000002' }];
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
                  if (url.startsWith('/api/doctor')) return { summary:{errorCount:1, warningCount:1, infoCount:0}, issues:[{id:'issue-1', title:'Bad device', severity:'ERROR', relatedObjectType:'DEVICE', relatedObjectId:'dev-1', message:'broken', suggestion:'check', detectedAt:'2026-05-09T10:00:00Z'}] };
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
                    querySelectorAll(){ return []; },
                    addEventListener(){},
                    createElement(tag){ const node = el(`${tag}-${elements.size}`); node.tagName = String(tag || '').toUpperCase(); node.id = ''; return node; },
                  },
                  window: { addEventListener(){}, removeEventListener(){} },
                  fetch: async (path) => { requestedUrls.push(String(path)); return { ok:true, status:200, json: async () => ({ ok:true, data: apiData(path) }) }; },
                  EventSource: function(){ this.close = function(){}; },
                };
                context.window.location = context.location;
                context.window.navigator = context.navigator;
                context.globalThis = context;
                vm.createContext(context);
                vm.runInContext(code + "\\n;globalThis.__smokeRoute = async function(hash){ location.hash = hash; return await route(); };globalThis.__smokeModal = async function(){ openWebAdminModal('Smoke Modal','<form class=\\\"edit-form\\\"><div class=\\\"form-actions\\\">hidden</div></form>',editModalFooter(false)); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); const closePromise = closeWebAdminModal(false); const className = String(modal && modal.className || ''); const closingMarker = String(modal && modal.dataset && modal.dataset.modalClosing || ''); await closePromise; return {html, className, closingMarker, removed:!document.getElementById('wa-modal-root')}; };globalThis.__smokeDeviceConfigModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/devices/test-device'; await route(); await startDeviceConfigEdit('test-device'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); const className = String(modal && modal.className || ''); await cancelDeviceConfigEdit('test-device'); return {html,className}; };globalThis.__smokeChannelModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/signals/test.channel'; await route(); await startChannelMetadataEdit('test.channel'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); await cancelChannelMetadataEdit('test.channel'); return {html}; };globalThis.__smokeListenerModal = async function(){ appState.me = { username:'Owner', role:'OWNER' }; location.hash = '#/listeners/test-listener'; await route(); await startSignalListenerBasicConfigEdit('test-listener','test.channel'); const modal = document.getElementById('wa-modal-root'); const html = String(modal && modal.innerHTML || ''); await cancelSignalListenerBasicConfigEdit('test-listener','test.channel'); return {html}; };", context, { filename:'webadmin-app.js' });
                vm.runInContext(`
                  globalThis.__smokeSilentRoute = async function(hash){ location.hash = hash; return await route({silent:true}); };
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
                    document.getElementById('listener-delete-confirmation').value = 'test-listener';
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
                  globalThis.__smokeClearRequestedUrls = function(){ requestedUrls.length = 0; };
                  globalThis.__smokeRequestedUrls = function(){ return requestedUrls.slice(); };
                `, context, { filename:"webadmin-app-extra-smoke.js" });
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
                  '#/doctor',
                  '#/diagnostics',
                  '#/signal-doctor',
                  '#/signals/test.channel',
                  '#/devices/test-device',
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
                      if (html.includes('data-detail-adaptive-grid="true"') || html.includes('wa-detail-adaptive-grid') || html.includes('data-responsive-card-grid="true') || html.includes('raw-config') || html.includes('logic-chain') || html.includes('detail-grid') || html.includes('config-section') || html.includes('panel-card') || html.includes('legacy') || html.includes('wa-card-grid wa-metrics-5') || html.includes('wa-table-card')) {
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
                    if (!lifecycle.listenerDeleteHtml.includes('data-listener-delete-modal="true"') || !lifecycle.listenerDeleteHtml.includes('data-danger-confirm-modal="true"') || !lifecycle.listenerDeleteHtml.includes('Action 数量')) {
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
                  if (failures.length) {
                    console.log(failures.join('\\n'));
                    process.exit(1);
                  }
                  console.log(`render smoke ok: ${routes.length} routes`);
                })();
                """.replace("__APP_JS_BASE64__", encodedAppJs);
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
                "execute as PlayerName run op Someone",
                "save-off",
                "save-on",
                "save-all",
                "reload"
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
                "/minecraft:op PlayerName"
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
        requireContains(actionService, "Validation validation = validateRequest(server, request);",
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
                "data-vbd-matcher-summary-card=\"true\"",
                "data-vbd-matcher-side-card=\"true\"",
                "data-detail-side-card=\"interaction-item-matcher\"",
                "data-vbd-config-summary=\"true\"",
                "wa-vbd-matcher-config-card",
                "openInteractionItemMatcherModal",
                "openInteractionItemMatcherReadonlyModal",
                "data-interaction-item-matcher-modal=\"true\"",
                "data-interaction-item-matcher-config-modal-section=\"true\"",
                "data-matcher-enabled=\"true\"",
                "data-matcher-template-item-id=\"true\"",
                "data-matcher-count-mode=\"true\"",
                "data-matcher-required-count=\"true\"",
                "data-matcher-match-damage=\"true\"",
                "data-matcher-match-custom-name=\"true\"",
                "data-matcher-match-lore=\"true\"",
                "data-matcher-source=\"true\"",
                "data-matcher-source-readonly=\"true\"",
                "data-matcher-vanilla-policy=\"true\"",
                "data-matcher-vanilla-policy-readonly=\"true\"",
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
        requireFalse(js.contains("itemSubmitEditor") || js.contains("saveItemSubmit") || js.contains("consumeEditor")
                        || js.contains("inventoryMatcherEditor") || js.contains("equipmentMatcherEditor")
                        || js.contains("conditionEngineEditor") || js.contains("successFailPathGraph"),
                "7.8 matcher stage does not expose itemSubmit/consume/inventory/equipment/ConditionEngine/path graph editors");
        requireFalse(js.contains("raw-json-textarea") || js.contains("matcher-json") || js.contains("data-component-json"),
                "7.8 matcher UI does not expose raw JSON/data component editors");
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
        requireContains(capabilitiesJson, "signalListenerLifecycleWriteEnabled", "capabilities describe signal listener lifecycle stage");
        requireContains(capabilitiesJson, "DELETE_VIRTUAL_BLOCK_DEVICE", "capabilities expose VBD delete operation");
        requireContains(capabilitiesJson, "EDIT_ACTION_RELAY_ACTIONS", "capabilities expose action relay action list operation");
        requireContains(capabilitiesJson, "CREATE_SIGNAL_LISTENER", "capabilities expose listener create operation");
        requireContains(capabilitiesJson, "DELETE_SIGNAL_LISTENER", "capabilities expose listener delete operation");
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
        requireContains(js, "channel_metadata_changed", "frontend listens for channel metadata realtime events");
        requireContains(js, "signal_listener_config_changed", "frontend listens for signal listener realtime events");
        requireContains(js, "编辑显示信息", "frontend exposes scoped metadata edit action");
        requireContains(js, "此信息仅用于 WebAdmin 展示", "metadata edit warning describes display-only scope");
        requireFalse(js.contains("fetch('/api/actions', {method:'PATCH'"), "frontend does not expose action write PATCH");
        requireFalse(js.contains("fetch('/api/regions', {method:'PATCH'"), "frontend does not expose region write PATCH");
        requireFalse(js.contains("fetch('/api/webadmin/users', {method:'PATCH'"), "frontend does not expose user write PATCH");
        requireFalse(js.contains("saveItemSubmit"), "frontend does not expose itemSubmit save flow");
        requireFalse(js.contains("saveRegion") || js.contains("saveSettings"), "frontend does not expose region/settings save flow");
        requireFalse(js.contains("saveAction(") || js.contains("saveActionTemplate"), "frontend still avoids generic action editor save flow");
        requireContains(js, "data-danger-confirm-modal=\"true\"", "supported lifecycle deletes use dangerous confirm modal");
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
