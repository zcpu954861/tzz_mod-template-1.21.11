package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.BlockStateConditionMode;
import com.zcpu.tzzmod.signal.device.BlockStateConditionParser;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceMode;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.signal.device.VirtualBlockPowerState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

public final class WebAdminVirtualBlockDeviceNativeTriggerService {
    public Map<String, Object> overview(MinecraftServer server, String deviceRef) {
        if (server == null || isBlank(deviceRef)) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceRef);
        if (!resolved.foundUnique()) {
            return null;
        }
        SignalDeviceData device = resolved.device().normalized();
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            Map<String, Object> unsupported = new LinkedHashMap<>();
            unsupported.put("deviceId", device.id());
            unsupported.put("deviceType", WebAdminReadonlySupport.deviceType(device));
            unsupported.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
            unsupported.put("supported", false);
            unsupported.put("typeSupported", false);
            unsupported.put("readOnly", true);
            unsupported.put("writeApiEnabled", false);
            unsupported.put("unsupportedReason", "7.9 P1 原生触发配置只支持 virtual_block_device。");
            return unsupported;
        }

        NativeTriggerRuntime runtime = runtime(server, device);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("supported", true);
        data.put("typeSupported", true);
        data.put("readOnly", true);
        data.put("writeApiEnabled", false);
        data.put("p1NoNativeTriggerWriteApi", true);
        data.put("availableTriggerTypes", List.of(
                "redstone_powered",
                "blockstate",
                "right_click",
                "container_open",
                "container_close",
                "container_change"
        ));
        data.put("defaultSelectedTriggerTypes", defaultSelectedTriggerTypes(device));
        data.put("boundBlock", boundBlock(device, runtime));
        data.put("triggers", triggers(device, runtime));
        data.put("forbiddenInP1", List.of(
                "nativeTriggerWriteApi",
                "itemSubmit",
                "consume",
                "inventory",
                "equipment",
                "armor",
                "conditionEngine",
                "successFailPathGraph",
                "scratchLikeEditor",
                "containerItemTemplateGui",
                "rawJsonTextarea"
        ));
        data.put("notes", List.of(
                "7.9 P1 只读展示 VBD 原生触发配置和当前方块 BlockState 属性。",
                "触发方式选择器只是详情页显示过滤；持久化启用/禁用在 7.9 P2 实现。",
                "容器内容变化的物品模板编辑器规划在 7.9 P3。"
        ));
        return data;
    }

    private static Map<String, Object> triggers(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> triggers = new LinkedHashMap<>();
        triggers.put("redstone_powered", redstone(device, runtime));
        triggers.put("blockstate", blockState(device, runtime));
        triggers.put("right_click", rightClick(device));
        triggers.put("container_open", containerOpen(device));
        triggers.put("container_close", containerClose(device));
        triggers.put("container_change", containerChange(device));
        return triggers;
    }

    private static Map<String, Object> redstone(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> data = baseTrigger("redstone_powered", "红石 / powered 状态", true, true);
        data.put("mode", device.mode());
        data.put("modeDisplayName", VirtualBlockDeviceMode.displayName(device.mode()));
        data.put("channel", device.channel());
        data.put("offChannel", device.offChannel());
        data.put("lastPowered", device.lastPowered());
        data.put("lastPowerLevel", device.lastPowerLevel());
        data.put("lastTriggerResult", device.lastResult());
        data.put("runtimeAvailable", runtime.powerState() != null);
        if (runtime.powerState() != null) {
            VirtualBlockPowerState power = runtime.powerState();
            data.put("currentPowered", power.currentPowered());
            data.put("currentPowerLevel", power.receivedPowerLevel());
            data.put("blockStatePowered", power.blockStatePowered());
            data.put("actualBlockId", power.blockId());
        }
        return data;
    }

    private static Map<String, Object> blockState(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> data = baseTrigger(
                "blockstate",
                "BlockState 条件",
                device.conditionEnabled(),
                device.conditionEnabled()
                        || !isBlank(device.conditionBlockId())
                        || !device.conditionProperties().isEmpty()
                        || !isBlank(device.conditionRaw())
        );
        data.put("conditionEnabled", device.conditionEnabled());
        data.put("conditionBlockId", device.conditionBlockId());
        data.put("conditionProperties", device.conditionProperties());
        data.put("conditionRaw", device.conditionRaw());
        data.put("conditionMode", device.conditionMode());
        data.put("conditionModeDisplayName", BlockStateConditionMode.displayName(device.conditionMode()));
        data.put("lastConditionMatched", device.lastConditionMatched());
        data.put("lastConditionCheckGameTime", device.lastConditionCheckGameTime());
        data.put("lastConditionResult", device.lastConditionResult());
        data.put("runtimeState", runtime.status());
        data.put("supportedPropertyCount", runtime.properties().size());
        data.put("supportedProperties", runtime.properties());
        boolean hasBoundBlockState = runtime.state() != null && !runtime.state().isAir();
        data.put("allowedValuesFromBoundBlock", hasBoundBlockState);
        data.put("propertiesFromBoundBlock", hasBoundBlockState);
        data.put("propertySourceStatus", runtime.status());
        data.put("currentMatched", hasBoundBlockState && device.conditionEnabled()
                ? BlockStateConditionParser.matches(runtime.state(), device)
                : null);
        data.put("validationIssues", runtime.state() == null
                ? List.of("当前绑定方块状态不可用，无法校验已保存的 BlockState 条件。")
                : BlockStateConditionParser.validateSavedCondition(device, runtime.state()));
        return data;
    }

    private static Map<String, Object> rightClick(SignalDeviceData device) {
        boolean configured = device.interactionEnabled()
                || !isBlank(device.interactChannel())
                || device.interactionCooldownTicks() > 0;
        Map<String, Object> data = baseTrigger(
                "right_click",
                "玩家右键交互",
                device.interactionEnabled(),
                configured
        );
        data.put("interactionEnabled", device.interactionEnabled());
        data.put("interactChannel", device.interactChannel());
        data.put("interactionCooldownTicks", device.interactionCooldownTicks());
        data.put("lastInteractionGameTime", device.lastInteractionGameTime());
        data.put("lastInteractionWallTimeMillis", device.lastInteractionWallTimeMillis());
        data.put("lastInteractionPlayerName", device.lastInteractionPlayerName());
        data.put("lastInteractionPlayerUuid", device.lastInteractionPlayerUuid());
        data.put("lastInteractionResult", device.lastInteractionResult());
        data.put("lastInteractionHand", device.lastInteractionHand());
        data.put("lastInteractionSide", device.lastInteractionSide());
        data.put("interactionItemMatcherLayer", Map.of(
                "enabled", device.interactionItemMatcherEnabled(),
                "configured", device.interactionItemMatcher().normalized().enabled(),
                "templateItemId", device.interactionItemMatcher().normalized().templateItemId(),
                "summary", device.interactionItemMatcher().normalized().templateSummary()
        ));
        data.put("conditionLayerNote", "interaction item matcher 是右键交互之后的条件/判定层，不是新的原生触发源。");
        return data;
    }

    private static Map<String, Object> containerOpen(SignalDeviceData device) {
        boolean configured = !isBlank(device.containerOpenChannel());
        Map<String, Object> data = baseTrigger(
                "container_open",
                "容器打开",
                device.containerEnabled() && configured,
                configured
        );
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerOpenChannel", device.containerOpenChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("lastContainerOpenGameTime", device.lastContainerOpenGameTime());
        data.put("lastContainerOpenWallTimeMillis", device.lastContainerOpenWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled。");
        return data;
    }

    private static Map<String, Object> containerClose(SignalDeviceData device) {
        boolean configured = !isBlank(device.containerCloseChannel());
        Map<String, Object> data = baseTrigger(
                "container_close",
                "容器关闭",
                device.containerEnabled() && configured,
                configured
        );
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerCloseChannel", device.containerCloseChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("lastContainerCloseGameTime", device.lastContainerCloseGameTime());
        data.put("lastContainerCloseWallTimeMillis", device.lastContainerCloseWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled。");
        return data;
    }

    private static Map<String, Object> containerChange(SignalDeviceData device) {
        boolean configured = !isBlank(device.containerChangeChannel()) || !device.itemConditions().isEmpty();
        Map<String, Object> data = baseTrigger(
                "container_change",
                "容器内容变化",
                device.containerEnabled() && configured,
                configured
        );
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerChangeChannel", device.containerChangeChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("containerChangeCheckIntervalTicks", device.containerChangeCheckIntervalTicks());
        data.put("lastContainerCheckGameTime", device.lastContainerCheckGameTime());
        data.put("lastContainerFingerprint", device.lastContainerFingerprint());
        data.put("lastContainerChangeGameTime", device.lastContainerChangeGameTime());
        data.put("lastContainerChangeWallTimeMillis", device.lastContainerChangeWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("itemConditionCount", device.itemConditions().size());
        data.put("itemConditions", containerItemConditionSummaries(device.itemConditions()));
        data.put("templateEditorPhase", "7.9 P3");
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled；物品模板 GUI 不在 P1/P2 普通 Web 表单中实现。");
        return data;
    }

    private static Map<String, Object> baseTrigger(String type, String label, boolean enabled, boolean configured) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("label", label);
        data.put("enabled", enabled);
        data.put("configured", configured);
        data.put("readOnly", true);
        return data;
    }

    private static List<Map<String, Object>> containerItemConditionSummaries(List<ContainerItemConditionData> rawConditions) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (ContainerItemConditionData raw : rawConditions == null ? List.<ContainerItemConditionData>of() : rawConditions) {
            ContainerItemConditionData condition = raw == null ? null : raw.normalized();
            if (condition == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", condition.id());
            summary.put("name", condition.name());
            summary.put("enabled", condition.enabled());
            summary.put("type", condition.type());
            summary.put("slot", condition.slot());
            summary.put("itemId", condition.itemId());
            summary.put("countMode", condition.countMode());
            summary.put("count", condition.count());
            summary.put("channel", condition.channel());
            summary.put("offChannel", condition.offChannel());
            summary.put("mode", condition.mode());
            summary.put("lastMatched", condition.lastMatched());
            summary.put("lastResult", condition.lastResult());
            summaries.add(summary);
        }
        return List.copyOf(summaries);
    }

    private static List<String> defaultSelectedTriggerTypes(SignalDeviceData device) {
        List<String> selected = new ArrayList<>();
        selected.add("redstone_powered");
        if (device.conditionEnabled() || !device.conditionProperties().isEmpty() || !isBlank(device.conditionRaw())) {
            selected.add("blockstate");
        }
        if (device.interactionEnabled() || !isBlank(device.interactChannel()) || device.interactionCooldownTicks() > 0) {
            selected.add("right_click");
        }
        if (!isBlank(device.containerOpenChannel())) {
            selected.add("container_open");
        }
        if (!isBlank(device.containerCloseChannel())) {
            selected.add("container_close");
        }
        if (!isBlank(device.containerChangeChannel()) || !device.itemConditions().isEmpty()) {
            selected.add("container_change");
        }
        return List.copyOf(selected);
    }

    private static Map<String, Object> boundBlock(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", device.dimension());
        data.put("pos", Map.of("x", device.x(), "y", device.y(), "z", device.z()));
        data.put("expectedBlockId", device.blockId());
        data.put("actualBlockId", runtime.actualBlockId());
        data.put("status", runtime.status());
        data.put("worldAvailable", runtime.worldAvailable());
        data.put("chunkLoaded", runtime.chunkLoaded());
        data.put("air", runtime.state() != null && runtime.state().isAir());
        data.put("blockMatches", !isBlank(runtime.actualBlockId()) && runtime.actualBlockId().equals(device.blockId()));
        data.put("supportedPropertyCount", runtime.properties().size());
        return data;
    }

    private static NativeTriggerRuntime runtime(MinecraftServer server, SignalDeviceData device) {
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return new NativeTriggerRuntime(false, false, "world_unavailable", "", null, null, List.of());
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return new NativeTriggerRuntime(true, false, "chunk_unloaded", "", null, null, List.of());
        }
        BlockState state = world.getBlockState(pos);
        VirtualBlockPowerState power = VirtualBlockDeviceSupport.powerState(world, pos);
        String blockId = VirtualBlockDeviceSupport.blockId(state);
        String status = state.isAir()
                ? "air"
                : (!isBlank(device.blockId()) && !blockId.equals(device.blockId()) ? "block_mismatch" : "ready");
        return new NativeTriggerRuntime(true, true, status, blockId, state, power, propertyDtos(state, device));
    }

    private static List<Map<String, Object>> propertyDtos(BlockState state, SignalDeviceData device) {
        if (state == null || state.isAir()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            String currentValue = valueName(state, property);
            String targetValue = device.conditionProperties().get(property.getName());
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("name", property.getName());
            dto.put("kind", propertyKind(property));
            dto.put("currentValue", currentValue);
            dto.put("allowedValues", valueNames(property));
            dto.put("targetValue", targetValue == null ? "" : targetValue);
            dto.put("targetMatched", targetValue != null && targetValue.equals(currentValue));
            dto.put("selectedInCondition", targetValue != null);
            result.add(dto);
        }
        return List.copyOf(result);
    }

    private static String propertyKind(Property<?> property) {
        if (property instanceof BooleanProperty) {
            return "boolean";
        }
        if (property instanceof IntProperty) {
            return "integer";
        }
        if (property instanceof EnumProperty<?>) {
            return "enum";
        }
        return "value";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> valueNames(Property<?> property) {
        if (property == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Property rawProperty = property;
        Collection values = rawProperty.getValues();
        for (Object value : values) {
            result.add(rawProperty.name((Comparable) value));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(BlockState state, Property<?> property) {
        if (state == null || property == null) {
            return "";
        }
        Property rawProperty = property;
        Comparable value = state.get(rawProperty);
        return rawProperty.name(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private record NativeTriggerRuntime(
            boolean worldAvailable,
            boolean chunkLoaded,
            String status,
            String actualBlockId,
            BlockState state,
            VirtualBlockPowerState powerState,
            List<Map<String, Object>> properties
    ) {
    }
}
