package com.zcpu.tzzmod.signal.device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public final class BlockStateConditionParser {
    private BlockStateConditionParser() {
    }

    public static BlockStateConditionResult parseAndValidate(String raw, BlockState currentState) {
        if (raw == null || raw.trim().isBlank()) {
            return BlockStateConditionResult.failure("方块状态条件不能为空。");
        }
        if (currentState == null || currentState.isAir()) {
            return BlockStateConditionResult.failure("当前位置是空气，不能设置方块状态条件。");
        }

        BlockStateConditionResult parsed = parse(raw.trim(), VirtualBlockDeviceSupport.blockId(currentState));
        if (!parsed.success()) {
            return parsed;
        }
        return validate(parsed.condition(), currentState);
    }

    public static boolean matches(BlockState state, SignalDeviceData device) {
        if (state == null || device == null || !device.conditionEnabled()) {
            return false;
        }
        return matches(state, new BlockStateCondition(
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw()
        ));
    }

    public static boolean matches(BlockState state, BlockStateCondition condition) {
        if (state == null || condition == null) {
            return false;
        }
        if (!VirtualBlockDeviceSupport.blockId(state).equals(condition.blockId())) {
            return false;
        }
        for (Map.Entry<String, String> entry : condition.properties().entrySet()) {
            Property<?> property = findProperty(state, entry.getKey());
            if (property == null) {
                return false;
            }
            String value = valueName(state, property);
            if (!entry.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }

    public static String supportedProperties(BlockState state) {
        if (state == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Property<?> property : state.getProperties()) {
            joiner.add(property.getName());
        }
        return joiner.toString();
    }

    public static String allowedValues(Property<?> property) {
        if (property == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String value : valueNames(property)) {
            joiner.add(value);
        }
        return joiner.toString();
    }

    public static List<String> validateSavedCondition(SignalDeviceData device, BlockState currentState) {
        List<String> issues = new ArrayList<>();
        if (device == null || !device.conditionEnabled()) {
            return issues;
        }
        if (currentState == null) {
            issues.add("当前方块状态不可用。");
            return issues;
        }
        String currentBlockId = VirtualBlockDeviceSupport.blockId(currentState);
        if (!currentBlockId.equals(device.conditionBlockId())) {
            issues.add("当前方块 ID 与条件方块 ID 不一致。");
            return issues;
        }
        for (Map.Entry<String, String> entry : device.conditionProperties().entrySet()) {
            Property<?> property = findProperty(currentState, entry.getKey());
            if (property == null) {
                issues.add("当前方块不支持状态 " + entry.getKey() + "。");
                continue;
            }
            if (!canParse(property, entry.getValue())) {
                issues.add("状态 " + entry.getKey() + " 不支持值 " + entry.getValue() + "。");
            }
        }
        return issues;
    }

    public static Property<?> findProperty(BlockState state, String name) {
        if (state == null || name == null) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    private static BlockStateConditionResult parse(String raw, String currentBlockId) {
        String blockId;
        String propertiesText;
        int bracketStart = raw.indexOf('[');
        if (bracketStart >= 0) {
            if (!raw.endsWith("]")) {
                return BlockStateConditionResult.failure("条件格式错误，应使用 block_id[property=value]。");
            }
            blockId = raw.substring(0, bracketStart).trim();
            propertiesText = raw.substring(bracketStart + 1, raw.length() - 1).trim();
        } else if (raw.contains("=")) {
            blockId = currentBlockId;
            propertiesText = raw;
        } else {
            return BlockStateConditionResult.failure("条件格式错误，应使用 block_id[property=value]。");
        }

        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            return BlockStateConditionResult.failure("方块 ID 无效：" + blockId);
        }
        if (!Registries.BLOCK.containsId(identifier)) {
            return BlockStateConditionResult.failure("方块 ID 不存在：" + identifier);
        }
        if (propertiesText.isBlank()) {
            return BlockStateConditionResult.failure("条件至少需要一个 property=value。");
        }

        Map<String, String> properties = new LinkedHashMap<>();
        for (String token : propertiesText.split(",")) {
            String part = token.trim();
            if (part.isBlank()) {
                return BlockStateConditionResult.failure("条件中存在空的属性项。");
            }
            int equalsIndex = part.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == part.length() - 1) {
                return BlockStateConditionResult.failure("属性格式错误：" + part);
            }
            String propertyName = part.substring(0, equalsIndex).trim();
            String value = part.substring(equalsIndex + 1).trim();
            if (propertyName.isBlank() || value.isBlank()) {
                return BlockStateConditionResult.failure("属性名和值不能为空：" + part);
            }
            if (properties.containsKey(propertyName)) {
                return BlockStateConditionResult.failure("条件中重复设置了状态 " + propertyName + "。");
            }
            properties.put(propertyName, value);
        }

        return BlockStateConditionResult.success(new BlockStateCondition(
                identifier.toString(),
                Map.copyOf(properties),
                normalizedRaw(identifier.toString(), properties)
        ));
    }

    private static BlockStateConditionResult validate(BlockStateCondition condition, BlockState currentState) {
        String currentBlockId = VirtualBlockDeviceSupport.blockId(currentState);
        if (!currentBlockId.equals(condition.blockId())) {
            return BlockStateConditionResult.failure("当前方块与条件方块 ID 不一致。当前：" + currentBlockId + "，条件：" + condition.blockId());
        }

        Block block = Registries.BLOCK.get(Identifier.of(condition.blockId()));
        if (block == null) {
            return BlockStateConditionResult.failure("方块 ID 不存在：" + condition.blockId());
        }

        Map<String, String> normalizedProperties = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : condition.properties().entrySet()) {
            Property<?> property = findProperty(currentState, entry.getKey());
            if (property == null) {
                return BlockStateConditionResult.failure("该方块不支持状态 " + entry.getKey() + "，无法添加此条件。");
            }
            Optional<String> parsedValue = parseValueName(property, entry.getValue());
            if (parsedValue.isEmpty()) {
                return BlockStateConditionResult.failure("状态 " + entry.getKey() + " 不支持值 " + entry.getValue()
                        + "。允许值：" + allowedValues(property) + "。");
            }
            normalizedProperties.put(entry.getKey(), parsedValue.get());
        }

        return BlockStateConditionResult.success(new BlockStateCondition(
                condition.blockId(),
                Map.copyOf(normalizedProperties),
                normalizedRaw(condition.blockId(), normalizedProperties)
        ));
    }

    private static String normalizedRaw(String blockId, Map<String, String> properties) {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return blockId + "[" + joiner + "]";
    }

    private static boolean canParse(Property<?> property, String rawValue) {
        return parseValueName(property, rawValue).isPresent();
    }

    private static Optional<String> parseValueName(Property<?> property, String rawValue) {
        for (String value : valueNames(property)) {
            if (value.equals(rawValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Collection<String> valueNames(Property<?> property) {
        List<String> result = new ArrayList<>();
        Property rawProperty = property;
        for (Object value : rawProperty.getValues()) {
            result.add(rawProperty.name((Comparable) value));
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(BlockState state, Property<?> property) {
        Property rawProperty = property;
        Comparable value = state.get(rawProperty);
        return rawProperty.name(value);
    }
}
