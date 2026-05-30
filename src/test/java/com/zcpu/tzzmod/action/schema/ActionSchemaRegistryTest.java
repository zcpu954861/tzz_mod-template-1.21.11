package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ActionSchemaRegistryTest {
    private static final Set<ActionOwnerType> EXPECTED_OWNERS = EnumSet.of(
            ActionOwnerType.SIGNAL_LISTENER,
            ActionOwnerType.ACTION_RELAY,
            ActionOwnerType.REGION_ENTER,
            ActionOwnerType.REGION_EXIT,
            ActionOwnerType.REGION_STAY,
            ActionOwnerType.TIMER_START,
            ActionOwnerType.TIMER_TICK,
            ActionOwnerType.TIMER_COMPLETE,
            ActionOwnerType.TIMER_CANCEL
    );

    private ActionSchemaRegistryTest() {
    }

    public static void run() throws IOException {
        requireAllActionTypesHaveOneSchema();
        requireSchemaShape();
        requireCriticalFieldMetadata();
        requireOwnerCapabilityShape();
        requireRegistryLookupIsStrict();
        requireRegistryIsImmutable();
        requireSchemaPackageDoesNotImportRuntimeServices();
    }

    private static void requireAllActionTypesHaveOneSchema() {
        Set<ActionType> expected = EnumSet.allOf(ActionType.class);
        Set<ActionType> actual = EnumSet.noneOf(ActionType.class);
        Set<String> ids = new HashSet<>();
        for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
            require(schema.actionType() != null, "schema action type must not be null");
            require(actual.add(schema.actionType()), "duplicate schema for " + schema.actionType());
            require(ids.add(schema.id()), "duplicate schema id " + schema.id());
            require(schema.id().equals(schema.actionType().id()), "schema id must match ActionType id for " + schema.actionType());
        }
        require(actual.equals(expected), "schema action types mismatch expected=" + expected + " actual=" + actual);
        require(ActionSchemaRegistry.schemasByType().keySet().equals(expected), "schemasByType must cover every ActionType exactly once");
    }

    private static void requireSchemaShape() {
        for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
            requireChinese(schema.displayName(), "schema display name must be Chinese for " + schema.id());
            requireChinese(schema.description(), "schema description must be Chinese for " + schema.id());
            requireChinese(schema.helpText(), "schema help text must be Chinese for " + schema.id());
            require(schema.supportsConditionGroup(), "existing ActionConfig schemas must expose optional conditionGroupId");
            require(!schema.fields().isEmpty(), "schema fields must not be empty for " + schema.id());
            require(!schema.applicableOwners().isEmpty(), "schema applicable owners must not be empty for " + schema.id());
            require(schema.applicableOwners().equals(EXPECTED_OWNERS), "schema owners must remain current Resource Graph owners for " + schema.id());

            Set<String> fieldIds = new HashSet<>();
            for (ActionFieldSchema field : schema.fields()) {
                require(!field.id().isBlank(), "field id must not be blank for " + schema.id());
                require(fieldIds.add(field.id()), "duplicate field id " + schema.id() + "." + field.id());
                requireChinese(field.label(), "field label must be Chinese for " + schema.id() + "." + field.id());
                requireChinese(field.description(), "field description must be Chinese for " + schema.id() + "." + field.id());
                requireValidDefault(field, schema.id());
                requireValidOptions(field, schema.id());
            }
        }
    }

    private static void requireValidDefault(ActionFieldSchema field, String schemaId) {
        String prefix = schemaId + "." + field.id();
        if (field.type() == ActionFieldType.BOOLEAN) {
            require("true".equals(field.defaultValue()) || "false".equals(field.defaultValue()),
                    "boolean default must be true/false for " + prefix);
        }
        if (field.type() == ActionFieldType.NUMBER) {
            long parsed;
            try {
                parsed = Long.parseLong(field.defaultValue());
            } catch (NumberFormatException ex) {
                throw new AssertionError("number default must parse for " + prefix + ": " + field.defaultValue(), ex);
            }
            if (field.minNumber() != null) {
                require(parsed >= field.minNumber(), "number default below min for " + prefix);
            }
            if (field.maxNumber() != null) {
                require(parsed <= field.maxNumber(), "number default above max for " + prefix);
            }
            if (field.minNumber() != null && field.maxNumber() != null) {
                require(field.minNumber() <= field.maxNumber(), "number min/max inverted for " + prefix);
            }
        }
        if (field.maxLength() != null) {
            require(field.maxLength() > 0, "maxLength must be positive for " + prefix);
        }
    }

    private static void requireValidOptions(ActionFieldSchema field, String schemaId) {
        if (field.options().isEmpty()) {
            return;
        }
        Set<String> values = new HashSet<>();
        for (ActionFieldOption option : field.options()) {
            require(values.add(option.value()), "duplicate option value for " + schemaId + "." + field.id() + ": " + option.value());
            requireChinese(option.label(), "option label must be Chinese for " + schemaId + "." + field.id() + "." + option.value());
        }
        require(field.defaultValue().isBlank() || values.contains(field.defaultValue()),
                "select default must be empty or one of the options for " + schemaId + "." + field.id());
    }

    private static void requireOwnerCapabilityShape() {
        require(ActionSchemaRegistry.ownerTypes().equals(EXPECTED_OWNERS), "owner enum set must stay limited to current ActionConfig owners");
        Set<ActionOwnerType> capabilityOwners = EnumSet.noneOf(ActionOwnerType.class);
        for (ActionCapability capability : ActionSchemaRegistry.capabilities()) {
            require(capability.ownerType() != null, "capability owner must not be null");
            require(capabilityOwners.add(capability.ownerType()), "duplicate owner capability " + capability.ownerType());
            require(!capability.actionTypes().isEmpty(), "owner capability must not be empty for " + capability.ownerType());
            require(capability.actionTypes().equals(EnumSet.allOf(ActionType.class)),
                    "Phase 1 owner metadata must cover existing ActionType set for " + capability.ownerType());
            requireChinese(capability.boundaryNote(), "capability boundary note must be Chinese for " + capability.ownerType());
        }
        require(capabilityOwners.equals(EXPECTED_OWNERS), "capability owner set mismatch");
        for (ActionOwnerType ownerType : EXPECTED_OWNERS) {
            require(!ActionSchemaRegistry.actionTypesForOwner(ownerType).isEmpty(), "owner lookup must not be empty for " + ownerType);
        }
        for (ActionOwnerType ownerType : ActionOwnerType.values()) {
            String name = ownerType.name();
            require(!name.contains("VBD") && !name.contains("ITEM") && !name.contains("CONTAINER")
                            && !name.contains("PROGRAM") && !name.contains("BRANCH") && !name.contains("SEQUENCE"),
                    "Phase 1 owner enum must not include non-ActionConfig owner " + name);
        }
    }

    private static void requireRegistryLookupIsStrict() {
        require(ActionSchemaRegistry.find(null).isEmpty(), "null ActionType lookup must not fall back to command");
        require(ActionSchemaRegistry.findById(null).isEmpty(), "null id lookup must not fall back to command");
        require(ActionSchemaRegistry.findById("unknown_action_type").isEmpty(), "unknown id lookup must not fall back to command");
        Optional<ActionSchema> command = ActionSchemaRegistry.findById("command");
        require(command.isPresent() && command.get().actionType() == ActionType.COMMAND, "known id lookup must return exact schema");
    }

    private static void requireRegistryIsImmutable() {
        ActionSchema firstSchema = ActionSchemaRegistry.schemas().get(0);
        ActionFieldSchema firstField = firstSchema.fields().get(0);
        ActionCapability firstCapability = ActionSchemaRegistry.capabilities().get(0);
        expectUnsupported(() -> ActionSchemaRegistry.schemas().add(firstSchema), "schemas list must be immutable");
        expectUnsupported(() -> ActionSchemaRegistry.schemasByType().put(ActionType.COMMAND, firstSchema), "schemasByType map must be immutable");
        expectUnsupported(() -> firstSchema.fields().add(firstField), "schema fields must be immutable");
        expectUnsupported(() -> firstSchema.applicableOwners().add(ActionOwnerType.ACTION_RELAY), "schema owners must be immutable");
        expectUnsupported(() -> firstField.options().add(new ActionFieldOption("x", "测试")), "field options must be immutable");
        expectUnsupported(() -> ActionSchemaRegistry.capabilities().add(firstCapability), "capabilities list must be immutable");
        expectUnsupported(() -> firstCapability.actionTypes().add(ActionType.COMMAND), "capability action type set must be immutable");
        expectUnsupported(() -> ActionSchemaRegistry.ownerTypes().add(ActionOwnerType.ACTION_RELAY), "ownerTypes set must be immutable");
        expectUnsupported(() -> ActionSchemaRegistry.actionTypesForOwner(ActionOwnerType.ACTION_RELAY).add(ActionType.COMMAND),
                "owner action type lookup must be immutable");
    }

    private static void requireCriticalFieldMetadata() {
        requireFieldMaxLength(ActionType.COMMAND, "value", 512);
        requireFieldMaxLength(ActionType.MESSAGE, "value", 500);
        requireFieldMaxLength(ActionType.SOUND, "value", 128);
        requireFieldMaxLength(ActionType.SIGNAL, "value", 128);
        requireFieldMaxLength(ActionType.STATE_VARIABLE, "stateKey", 96);
        requireFieldMaxLength(ActionType.STATE_VARIABLE, "stateValue", 512);
        requireFieldMaxLength(ActionType.STATE_VARIABLE, "stateInitialValue", 512);
        requireFieldDefault(ActionType.STATE_VARIABLE, "stateValueType", "BOOLEAN");
        requireFieldRequired(ActionType.TIMER_START, "timerTargetMode", false);
        requireFieldRequired(ActionType.TIMER_CANCEL, "timerTargetMode", false);
        requireNumberMax(ActionType.TIMER_START, "timerDurationOverrideTicks", 1_728_000L);
        requireFieldOption(ActionType.TIMER_CANCEL, "timerMissingBehavior", "fail_if_missing");
        require(!ActionOwnerType.TIMER_START.id().equals(ActionType.TIMER_START.id()), "timer owner id must not collide with timer_start action id");
        require(!ActionOwnerType.TIMER_CANCEL.id().equals(ActionType.TIMER_CANCEL.id()), "timer owner id must not collide with timer_cancel action id");
    }

    private static void requireFieldMaxLength(ActionType actionType, String fieldId, int expected) {
        ActionFieldSchema field = field(actionType, fieldId);
        require(field.maxLength() != null && field.maxLength() == expected,
                actionType.id() + "." + fieldId + " maxLength expected " + expected + " actual " + field.maxLength());
    }

    private static void requireFieldDefault(ActionType actionType, String fieldId, String expected) {
        ActionFieldSchema field = field(actionType, fieldId);
        require(expected.equals(field.defaultValue()),
                actionType.id() + "." + fieldId + " default expected " + expected + " actual " + field.defaultValue());
    }

    private static void requireFieldRequired(ActionType actionType, String fieldId, boolean expected) {
        ActionFieldSchema field = field(actionType, fieldId);
        require(field.required() == expected,
                actionType.id() + "." + fieldId + " required expected " + expected + " actual " + field.required());
    }

    private static void requireNumberMax(ActionType actionType, String fieldId, long expected) {
        ActionFieldSchema field = field(actionType, fieldId);
        require(field.maxNumber() != null && field.maxNumber() == expected,
                actionType.id() + "." + fieldId + " maxNumber expected " + expected + " actual " + field.maxNumber());
    }

    private static void requireFieldOption(ActionType actionType, String fieldId, String expectedValue) {
        ActionFieldSchema field = field(actionType, fieldId);
        require(field.options().stream().anyMatch(option -> expectedValue.equals(option.value())),
                actionType.id() + "." + fieldId + " missing option " + expectedValue);
    }

    private static ActionFieldSchema field(ActionType actionType, String fieldId) {
        return ActionSchemaRegistry.require(actionType).fields().stream()
                .filter(field -> field.id().equals(fieldId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(actionType.id() + " missing field " + fieldId));
    }

    private static void requireSchemaPackageDoesNotImportRuntimeServices() throws IOException {
        Path sourceDir = projectRoot().resolve("src/main/java/com/zcpu/tzzmod/action/schema");
        List<String> forbiddenImports = List.of(
                "ActionEngine",
                "ActionValidator",
                "TimerRuntimeService",
                "SignalBridge",
                "StateVariableStore",
                "WebAdminWriteResult",
                "net.minecraft"
        );
        try (var stream = Files.list(sourceDir)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("import ")) {
                        continue;
                    }
                    for (String forbidden : forbiddenImports) {
                        require(!trimmed.contains(forbidden), "schema package must not import runtime/service dependency " + forbidden + " in " + file);
                    }
                }
            }
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("build.gradle")) && Files.exists(current.resolve("src"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate project root");
    }

    private static void expectUnsupported(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void requireChinese(String value, String message) {
        require(value != null && value.codePoints().anyMatch(ActionSchemaRegistryTest::isCjk), message);
    }

    private static boolean isCjk(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
