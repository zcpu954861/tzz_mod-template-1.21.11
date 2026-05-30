package com.zcpu.tzzmod.action.validation;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionOwnerType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ActionValidationServiceTest {
    private ActionValidationServiceTest() {
    }

    public static void run() {
        testValidCurrentActionsPassForEveryOwner();
        testUnknownAndBlankTypesFailClosed();
        testUnsupportedOwnerFailsClosed();
        testRequiredAndLegacyMalformedCodes();
        testConditionGroupValidationUsesOwnerActionTargetAndSkipsBlank();
        testStrictActionConstructionDoesNotFallbackToCommand();
    }

    private static void testValidCurrentActionsPassForEveryOwner() {
        for (ActionOwnerType ownerType : ActionOwnerType.values()) {
            for (ActionType actionType : EnumSet.allOf(ActionType.class)) {
                ActionValidationResult result = ActionValidationService.validate(ownerType, "actions[0]", validDraft(actionType));
                require(result.valid(), ownerType + " valid " + actionType + " errors=" + result.errors());
                require(result.action().isPresent(), ownerType + " valid action must produce normalized ActionConfig");
                require(result.action().get().type() == actionType, ownerType + " normalized action type mismatch");
            }
        }
    }

    private static void testUnknownAndBlankTypesFailClosed() {
        requireCode(
                ActionValidationService.validate(ActionOwnerType.ACTION_RELAY, "actions[0]", ActionDraft.simple("unknown_action", "say hi")),
                "invalid_type"
        );
        requireCode(
                ActionValidationService.validate(ActionOwnerType.ACTION_RELAY, "actions[0]", ActionDraft.simple("", "say hi")),
                "invalid_type"
        );
        require(ActionValidationService.parseActionTypeStrict("unknown_action").isEmpty(), "strict parse must not fallback");
    }

    private static void testUnsupportedOwnerFailsClosed() {
        requireCode(
                ActionValidationService.validate(null, "actions[0]", ActionDraft.simple("message", "hi")),
                "unsupported_owner"
        );
    }

    private static void testRequiredAndLegacyMalformedCodes() {
        requireCode(ActionValidationService.validate(ActionOwnerType.ACTION_RELAY, "actions[0]", ActionDraft.simple("message", "")), "empty");
        requireCode(ActionValidationService.validate(ActionOwnerType.ACTION_RELAY, "actions[0]", ActionDraft.simple("signal", "Bad Channel")), "invalid_channel");
        requireCode(ActionValidationService.validate(ActionOwnerType.ACTION_RELAY, "actions[0]", ActionDraft.simple("command", "/stop")), "server_management_command_forbidden");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.ACTION_RELAY,
                "actions[0]",
                ActionDraft.builder("message").value("hi").enabled("maybe").build()
        ), "invalid_boolean");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.ACTION_RELAY,
                "actions[0]",
                ActionDraft.builder("state_variable")
                        .stateOperation("set_variable")
                        .stateScope("GLOBAL")
                        .stateTargetMode("global")
                        .stateValueType("BOOLEAN")
                        .stateValue("true")
                        .build()
        ), "missing_key");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.ACTION_RELAY,
                "actions[0]",
                ActionDraft.builder("state_variable")
                        .stateOperation("set_variable")
                        .stateScope("GLOBAL")
                        .stateTargetMode("global")
                        .stateKey("flag")
                        .stateValueType("BOOLEAN")
                        .stateValue("not_bool")
                        .build()
        ), "invalid_boolean");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.ACTION_RELAY,
                "actions[0]",
                ActionDraft.builder("state_variable")
                        .stateOperation("increment_variable")
                        .stateScope("GLOBAL")
                        .stateTargetMode("global")
                        .stateKey("score")
                        .stateValueType("INTEGER")
                        .stateDelta(0)
                        .build()
        ), "invalid_delta");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.TIMER_COMPLETE,
                "onCompleteActions[0]",
                ActionDraft.builder("timer_start").timerTargetMode("nearby_player").timerId("timer.other").build()
        ), "timer_target_mode_invalid");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.TIMER_COMPLETE,
                "onCompleteActions[0]",
                ActionDraft.builder("timer_start").timerTargetMode("explicit_target").timerId("timer.other").build()
        ), "timer_target_id_required");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.TIMER_COMPLETE,
                "onCompleteActions[0]",
                ActionDraft.builder("timer_start").timerId("timer.other").timerStartPolicyOverride("restart_later").build()
        ), "timer_start_policy_invalid");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.TIMER_COMPLETE,
                "onCompleteActions[0]",
                ActionDraft.builder("timer_cancel").timerId("timer.other").timerMissingBehavior("delete_config").build()
        ), "timer_missing_behavior_invalid");
        requireCode(ActionValidationService.validate(
                ActionOwnerType.TIMER_COMPLETE,
                "onCompleteActions[0]",
                ActionDraft.builder("timer_start").build()
        ), "timer_id_required");
    }

    private static void testConditionGroupValidationUsesOwnerActionTargetAndSkipsBlank() {
        AtomicInteger calls = new AtomicInteger();
        ActionValidationService.ConditionGroupValidator validator = (field, groupId, targetType) -> {
            calls.incrementAndGet();
            return List.of(new ActionValidationError(field, "condition_group_incompatible", "条件组不兼容：" + targetType.id(), groupId));
        };
        ActionValidationResult blank = ActionValidationService.validate(
                ActionOwnerType.TIMER_START,
                "onStartActions[0]",
                ActionDraft.builder("message").value("hi").conditionGroupId("   ").build(),
                validator
        );
        require(blank.valid(), "blank condition group must preserve lazy skip");
        require(calls.get() == 0, "blank condition group must not call validator");

        for (ActionOwnerType ownerType : ActionOwnerType.values()) {
            AtomicReference<ConditionRuntimeTargetType> target = new AtomicReference<>();
            AtomicReference<String> field = new AtomicReference<>();
            AtomicReference<String> group = new AtomicReference<>();
            ActionValidationResult nonBlank = ActionValidationService.validate(
                    ownerType,
                    "actions[0]",
                    ActionDraft.builder("message").value("hi").conditionGroupId(" Gate.Id ").build(),
                    (errorField, groupId, targetType) -> {
                        field.set(errorField);
                        group.set(groupId);
                        target.set(targetType);
                        return List.of();
                    }
            );
            require(nonBlank.valid(), ownerType + " compatible condition callback should pass");
            require("actions[0].conditionGroupId".equals(field.get()), ownerType + " condition field must use action prefix");
            require("gate.id".equals(group.get()), ownerType + " condition id must be normalized before callback");
            require(target.get() == ActionCapabilityMatrix.require(ownerType).actionConditionTargetType(),
                    ownerType + " must use matrix action condition target");
        }

        ActionValidationResult incompatible = ActionValidationService.validate(
                ActionOwnerType.REGION_ENTER,
                "actions[0]",
                ActionDraft.builder("message").value("hi").conditionGroupId("gate").build(),
                validator
        );
        requireCode(incompatible, "condition_group_incompatible");
    }

    private static void testStrictActionConstructionDoesNotFallbackToCommand() {
        require(ActionValidationService.actionFromDraft(ActionDraft.simple("unknown_action", "stop")).isEmpty(),
                "strict action construction must reject unknown types");
        require(ActionValidationService.actionFromDraft(validDraft(ActionType.COMMAND)).orElseThrow().type() == ActionType.COMMAND,
                "strict action construction must keep known command type");
    }

    private static ActionDraft validDraft(ActionType actionType) {
        return switch (actionType) {
            case COMMAND -> ActionDraft.simple("command", "/say hello");
            case MESSAGE -> ActionDraft.simple("message", "hello");
            case SOUND -> ActionDraft.simple("sound", "minecraft:entity.experience_orb.pickup");
            case SIGNAL -> ActionDraft.simple("signal", "game.start");
            case STATE_VARIABLE -> ActionDraft.builder("state_variable")
                    .stateOperation("set_variable")
                    .stateScope("GLOBAL")
                    .stateTargetMode("global")
                    .stateKey("game.flag")
                    .stateValueType("BOOLEAN")
                    .stateValue("true")
                    .build();
            case TIMER_START -> ActionDraft.builder("timer_start")
                    .timerId("round.timer")
                    .timerTargetMode("context_player")
                    .timerStartPolicyOverride("RESTART")
                    .timerDurationOverrideTicks(0)
                    .build();
            case TIMER_CANCEL -> ActionDraft.builder("timer_cancel")
                    .timerId("round.timer")
                    .timerTargetMode("context_player")
                    .timerMissingBehavior("fail_if_missing")
                    .build();
        };
    }

    private static void requireCode(ActionValidationResult result, String code) {
        List<String> codes = new ArrayList<>();
        for (ActionValidationError error : result.errors()) {
            codes.add(error.code());
            if (code.equals(error.code())) {
                require(containsChinese(error.message()) || error.message().contains("TIMER_ON_"),
                        "validation code " + code + " should keep a readable message");
                return;
            }
        }
        throw new AssertionError("missing validation code " + code + " actual=" + codes);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
