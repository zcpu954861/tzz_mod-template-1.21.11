package com.zcpu.tzzmod.stabilization;

import com.google.gson.Gson;
import com.zcpu.tzzmod.resources.ResourceIntegrityTest;
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
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeClient;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
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
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static void testWebAdminReadonlyFrontendAssets() {
        String loginHtml = WebAdminFrontendAssets.loginHtml();
        String appHtml = WebAdminFrontendAssets.appHtml();
        String css = WebAdminFrontendAssets.appCss();
        String js = WebAdminFrontendAssets.appJs();

        requireNotBlank(loginHtml, "WebAdmin login HTML asset");
        requireNotBlank(appHtml, "WebAdmin app HTML asset");
        requireNotBlank(css, "WebAdmin CSS asset");
        requireNotBlank(js, "WebAdmin JS asset");

        for (String route : List.of(
                "#/dashboard",
                "#/devices",
                "#/signals",
                "#/doctor",
                "#/history",
                "#/users",
                "#/settings",
                "#/regions",
                "#/actions"
        )) {
            requireContains(appHtml + js, route, "WebAdmin readonly route present: " + route);
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

        requireContains(appHtml, "区域管理", "sidebar contains Region navigation");
        requireContains(appHtml, "动作系统", "sidebar contains Action navigation");
        requireContains(appHtml + js, "/api/realtime/events", "WebAdmin realtime event stream route present");
        requireContains(js, "dirtyRoutes", "realtime hidden-tab dirty route tracking present");
        requireContains(js, "pendingRefresh", "realtime pending refresh guard present");
        requireContains(js, "route({silent:true,expectedHash:hash,expectedSeq:seq})", "realtime refresh uses silent route update");
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

    private static void testWebAdminWriteFoundation() throws Exception {
        WebAdminPermissionService permissions = new WebAdminPermissionService();
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.TEST, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.ACQUIRE_EDIT_LOCK, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_METADATA, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_DEVICE, false);
        requirePermission(permissions, WebAdminRole.VIEWER, WebAdminOperationType.EDIT_USER, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.TEST, true);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.ACQUIRE_EDIT_LOCK, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_METADATA, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_DEVICE, false);
        requirePermission(permissions, WebAdminRole.TESTER, WebAdminOperationType.EDIT_USER, false);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.READ, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.TEST, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.ACQUIRE_EDIT_LOCK, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.RELEASE_EDIT_LOCK, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_METADATA, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, true);
        requirePermission(permissions, WebAdminRole.EDITOR, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, true);
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
        requireContains(js, "/api/signals/channels", "basic config channel picker reuses readonly signal channel API");
        requireContains(js, "channel-combo", "basic config channel field uses custom dark combobox");
        requireContains(js, "role=\"combobox\"", "basic config channel field keeps typed input semantics");
        requireContains(js, "handleDeviceBasicConfigChannelKey", "basic config channel combobox supports keyboard handling");
        requireContains(js, "handleDeviceExtendedConfigChannelKey", "extended config channel combobox supports keyboard handling");
        requireContains(js, "renderDeviceExtendedConfigChannelCombo", "extended config channel fields reuse dark combobox helper");
        requireContains(js, "channelOptionLabel", "basic config channel candidates include display helper");
        requireFalse(js.contains("<datalist"), "basic config channel picker does not use native datalist menu");
        requireContains(js, "该频道当前未在系统中发现", "basic config channel input warns about unseen channels");
        requireContains(js, "不会自动创建监听器", "basic config channel input explains manual channel behavior");
        requireContains(js, "/api/webadmin/edit-locks/acquire", "frontend acquires edit lock before metadata write");
        requireContains(js, "/api/webadmin/edit-locks/heartbeat", "frontend heartbeats edit lock during metadata edit");
        requireContains(js, "/api/webadmin/edit-locks/release", "frontend releases edit lock after edit");
        requireContains(js, "device_basic_config", "frontend uses distinct basic config edit lock target");
        requireContains(js, "device_extended_config", "frontend uses distinct extended config edit lock target");
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
        requireContains(js, "编辑显示信息", "frontend exposes scoped metadata edit action");
        requireContains(js, "此信息仅用于 WebAdmin 展示", "metadata edit warning describes display-only scope");
        requireFalse(js.contains("fetch('/api/actions', {method:'PATCH'"), "frontend does not expose action write PATCH");
        requireFalse(js.contains("fetch('/api/regions', {method:'PATCH'"), "frontend does not expose region write PATCH");
        requireFalse(js.contains("fetch('/api/webadmin/users', {method:'PATCH'"), "frontend does not expose user write PATCH");
        requireFalse(js.contains("saveItemSubmit") || js.contains("saveMatcher"), "frontend does not expose itemSubmit or matcher save flow");
        requireFalse(js.contains("saveRegion") || js.contains("saveAction") || js.contains("saveSettings"), "frontend does not expose region/action/settings save flow");
        requireFalse(js.contains(">删除<"), "frontend does not expose delete button");
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
