package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.ConditionValidationResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityProfile;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityService;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeGateStore;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.ContainerDeviceSupport;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminConditionRuntimeDoctorService {
    private final ConditionEvaluator evaluator = new ConditionEvaluator();
    private final ConditionGroupCompatibilityService compatibilityService = new ConditionGroupCompatibilityService();

    public List<WebAdminDtos.DoctorIssueDto> inspect(MinecraftServer server) {
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        WebAdminConditionGroupStore.ConditionGroupLoadResult groups = WebAdminConditionGroupStore.loadWithStatus(server);
        if (groups.degraded()) {
            issues.add(issue(
                    "condition-runtime-store-degraded",
                    "ERROR",
                    "条件组配置读取失败",
                    groups.message().isBlank() ? "条件组配置当前不可读取，运行时 gate 会安全阻断相关触发。" : groups.message(),
                    "SYSTEM",
                    "",
                    "",
                    "检查 condition_groups.json 是否为有效 JSON，并在修复前不要继续绑定新的运行时 gate。",
                    ""
            ));
            return List.copyOf(issues);
        }
        issues.addAll(diagnoseBindings(groups.file().groups, runtimeBindings(server)));
        return List.copyOf(issues);
    }

    public List<WebAdminDtos.DoctorIssueDto> diagnoseBindings(
            Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups,
            List<Binding> bindings
    ) {
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> safeGroups = groups == null ? Map.of() : groups;
        List<Binding> safeBindings = bindings == null ? List.of() : bindings;
        for (Binding rawBinding : safeBindings) {
            Binding binding = rawBinding == null ? Binding.empty() : rawBinding.normalized();
            if (binding.conditionGroupId().isBlank()) {
                continue;
            }
            WebAdminConditionGroupStore.ConditionGroupEntry entry = safeGroups.get(binding.conditionGroupId());
            if (entry == null) {
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-missing-group",
                        "ERROR",
                        "运行时 gate 绑定的条件组不存在",
                        "目标 " + binding.targetId() + " 绑定了条件组 " + binding.conditionGroupId() + "，但该条件组不存在或已删除。",
                        "重新绑定一个存在且兼容的条件组，或清空该 gate 保持旧逻辑。"
                ));
                continue;
            }
            WebAdminConditionGroupStore.ConditionGroupEntry normalized = WebAdminConditionGroupStore.ConditionGroupEntry.normalized(entry.id, entry);
            if (!normalized.enabled) {
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-disabled-group",
                        "WARNING",
                        "运行时 gate 绑定了已停用条件组",
                        "目标 " + binding.targetId() + " 绑定的条件组 " + binding.conditionGroupId() + " 已停用，运行时会安全阻断。",
                        "启用条件组、改绑其它兼容条件组，或清空该 gate。"
                ));
                continue;
            }
            if (normalized.groupDefinition == null) {
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-definition-missing",
                        "ERROR",
                        "运行时 gate 绑定的条件组定义缺失",
                        "目标 " + binding.targetId() + " 的条件组 " + binding.conditionGroupId() + " 缺少 groupDefinition，运行时会安全阻断。",
                        "修复条件组定义后重新保存，或清空该 gate。"
                ));
                continue;
            }
            ConditionValidationResult validation = evaluator.validate(normalized.groupDefinition);
            if (!validation.valid()) {
                String first = validation.issues().stream()
                        .map(issue -> issue.message())
                        .filter(message -> message != null && !message.isBlank())
                        .findFirst()
                        .orElse("存在无效条件节点");
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-invalid-group",
                        "ERROR",
                        "运行时 gate 绑定的条件组无效",
                        "条件组 " + binding.conditionGroupId() + " 校验失败：" + first + "。",
                        "打开条件组详情修复无效节点；修复前该 gate 会安全阻断。"
                ));
                continue;
            }
            ConditionGroupCompatibilityProfile profile = binding.compatibilityProfile() == null
                    ? compatibilityService.profile(binding.runtimeTargetType())
                    : binding.compatibilityProfile();
            ConditionGroupCompatibilityResult compatibility = compatibilityService.analyze(normalized.groupDefinition, profile);
            if (!compatibility.compatible()) {
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-incompatible-group",
                        "ERROR",
                        "运行时 gate 与条件组能力不兼容",
                        "目标 " + binding.targetId() + " 的 " + binding.runtimeTargetType().displayName() + " 不提供该条件组需要的上下文：" + compatibility.message(),
                        "改绑适用于 " + binding.runtimeTargetType().displayName() + " 的条件组；不要通过 UI 强行保存不兼容绑定。"
                ));
                continue;
            }
            if (containsAlwaysFalse(normalized.groupDefinition.root())) {
                issues.add(bindingIssue(
                        binding,
                        "condition-runtime-always-false-node",
                        "WARNING",
                        "运行时 gate 条件链包含永远失败节点",
                        "条件组 " + binding.conditionGroupId() + " 包含 always_false 节点，可能导致目标长期被阻断。",
                        "确认这是预期的调试阻断；否则移除或禁用该节点。"
                ));
            }
        }
        return List.copyOf(issues);
    }

    private List<Binding> runtimeBindings(MinecraftServer server) {
        List<Binding> bindings = new ArrayList<>();
        Map<String, SignalDeviceData> devicesById = SignalDeviceStore.getSnapshot(server).stream()
                .map(SignalDeviceData::normalized)
                .collect(java.util.stream.Collectors.toMap(
                        SignalDeviceData::id,
                        device -> device,
                        (left, right) -> left
                ));
        ConditionRuntimeGateStore.ConditionRuntimeGateLoadResult vbdGates = ConditionRuntimeGateStore.loadWithStatus(server);
        if (!vbdGates.degraded()) {
            vbdGates.file().virtualBlockDevices.forEach((deviceId, config) -> {
                ConditionRuntimeGateStore.VirtualBlockDeviceGateConfig gates = config == null ? ConditionRuntimeGateStore.VirtualBlockDeviceGateConfig.empty() : config.normalized();
                bindings.add(new Binding("VIRTUAL_BLOCK_DEVICE", deviceId, gates.redstoneConditionGroupId(), ConditionRuntimeTargetType.VBD_REDSTONE, "device:" + deviceId));
                bindings.add(new Binding("VIRTUAL_BLOCK_DEVICE", deviceId, gates.blockStateConditionGroupId(), ConditionRuntimeTargetType.VBD_BLOCKSTATE, "device:" + deviceId));
                bindings.add(new Binding("VIRTUAL_BLOCK_DEVICE", deviceId, gates.interactionConditionGroupId(), ConditionRuntimeTargetType.VBD_INTERACTION, "device:" + deviceId));
                bindings.add(new Binding("VIRTUAL_BLOCK_DEVICE", deviceId, gates.itemSubmitConditionGroupId(), ConditionRuntimeTargetType.ITEM_SUBMIT, "device:" + deviceId));
                bindings.add(vbdContainerBinding(server, devicesById.get(deviceId), deviceId, gates.containerOpenConditionGroupId(), ConditionRuntimeTargetType.CONTAINER_OPEN));
                bindings.add(vbdContainerBinding(server, devicesById.get(deviceId), deviceId, gates.containerCloseConditionGroupId(), ConditionRuntimeTargetType.CONTAINER_CLOSE));
                bindings.add(new Binding("VIRTUAL_BLOCK_DEVICE", deviceId, gates.containerChangeConditionGroupId(), ConditionRuntimeTargetType.CONTAINER_CHANGE, "device:" + deviceId));
            });
        }

        for (SignalListenerData raw : SignalListenerStore.getSnapshot(server)) {
            SignalListenerData listener = raw.normalized();
            bindings.add(new Binding("SIGNAL_LISTENER", listener.id(), listener.conditionGroupId(), ConditionRuntimeTargetType.SIGNAL_LISTENER, "channel:" + listener.channel()));
            for (int index = 0; index < listener.actions().size(); index++) {
                com.zcpu.tzzmod.action.ActionConfig action = listener.actions().get(index);
                if (action == null) {
                    continue;
                }
                String targetId = ConditionActionGateService.actionTargetId("listener", listener.id(), index);
                bindings.add(new Binding(
                        "SIGNAL_LISTENER_ACTION",
                        targetId,
                        action.conditionGroupId(),
                        ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                        "channel:" + listener.channel()
                ));
            }
        }

        for (SignalDeviceData device : devicesById.values()) {
            if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                continue;
            }
            ActionRelayBlockEntity relay = SignalDeviceStore.getLoadedActionRelay(server, device);
            if (relay != null) {
                bindings.add(new Binding("ACTION_RELAY", device.id(), relay.conditionGroupId(), ConditionRuntimeTargetType.ACTION_RELAY, "device:" + device.id()));
                for (int index = 0; index < relay.actions().size(); index++) {
                    com.zcpu.tzzmod.action.ActionConfig action = relay.actions().get(index);
                    if (action == null) {
                        continue;
                    }
                    String relayTargetId = device.id();
                    String targetId = ConditionActionGateService.actionTargetId("relay", relayTargetId, index);
                    bindings.add(new Binding(
                            "ACTION_RELAY_ACTION",
                            targetId,
                            action.conditionGroupId(),
                            ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                            "device:" + device.id()
                    ));
                }
            }
        }

        for (RegionControllerData raw : RegionControllerStore.getSnapshot(server)) {
            RegionControllerData controller = raw.normalized();
            bindings.add(new Binding("REGION_CONTROLLER", controller.id(), controller.enterConditionGroupId(), ConditionRuntimeTargetType.REGION_ENTER, "region:" + controller.regionId()));
            bindings.add(new Binding("REGION_CONTROLLER", controller.id(), controller.exitConditionGroupId(), ConditionRuntimeTargetType.REGION_EXIT, "region:" + controller.regionId()));
            bindings.add(new Binding("REGION_CONTROLLER", controller.id(), controller.stayConditionGroupId(), ConditionRuntimeTargetType.REGION_STAY, "region:" + controller.regionId()));
            addRegionActionBindings(bindings, controller, "enter", controller.enterActions(), ConditionRuntimeTargetType.REGION_ENTER_ACTION);
            addRegionActionBindings(bindings, controller, "exit", controller.exitActions(), ConditionRuntimeTargetType.REGION_EXIT_ACTION);
            addRegionActionBindings(bindings, controller, "stay", controller.stayActions(), ConditionRuntimeTargetType.REGION_STAY_ACTION);
        }
        return List.copyOf(bindings);
    }

    private static void addRegionActionBindings(
            List<Binding> bindings,
            RegionControllerData controller,
            String bucket,
            List<com.zcpu.tzzmod.action.ActionConfig> actions,
            ConditionRuntimeTargetType targetType
    ) {
        for (int index = 0; index < (actions == null ? List.<com.zcpu.tzzmod.action.ActionConfig>of() : actions).size(); index++) {
            com.zcpu.tzzmod.action.ActionConfig action = actions.get(index);
            if (action == null) {
                continue;
            }
            bindings.add(new Binding(
                    "REGION_CONTROLLER_ACTION",
                    ConditionActionGateService.regionActionTargetId(controller.id(), bucket, index),
                    action.conditionGroupId(),
                    targetType,
                    "region:" + controller.regionId()
            ));
        }
    }

    private Binding vbdContainerBinding(
            MinecraftServer server,
            SignalDeviceData device,
            String deviceId,
            String conditionGroupId,
            ConditionRuntimeTargetType targetType
    ) {
        boolean hasContainerSnapshot = runtimeProvidesContainerSnapshot(server, device);
        return new Binding(
                "VIRTUAL_BLOCK_DEVICE",
                deviceId,
                conditionGroupId,
                targetType,
                "device:" + deviceId,
                compatibilityService.profile(targetType, hasContainerSnapshot)
        );
    }

    private static boolean runtimeProvidesContainerSnapshot(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return false;
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return false;
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        return world.isChunkLoaded(pos) && ContainerDeviceSupport.hasInventory(world, pos);
    }

    private WebAdminDtos.DoctorIssueDto bindingIssue(
            Binding binding,
            String code,
            String severity,
            String title,
            String message,
            String suggestion
    ) {
        return issue(
                code + ":" + binding.runtimeTargetType().id() + ":" + binding.targetId() + ":" + binding.conditionGroupId(),
                severity,
                title,
                message,
                binding.targetType(),
                binding.targetId(),
                binding.conditionGroupId(),
                suggestion,
                binding.navigationTarget()
        );
    }

    private static WebAdminDtos.DoctorIssueDto issue(
            String id,
            String severity,
            String title,
            String message,
            String relatedType,
            String relatedId,
            String conditionGroupId,
            String suggestion,
            String navigationTarget
    ) {
        return new WebAdminDtos.DoctorIssueDto(
                id,
                severity,
                title,
                message,
                relatedType,
                relatedId,
                relatedId,
                "",
                safe(conditionGroupId).isBlank() ? message : message + " 条件组：" + conditionGroupId,
                suggestion,
                Instant.now().toString(),
                navigationTarget
        );
    }

    private static boolean containsAlwaysFalse(ConditionNode node) {
        if (node == null || !node.enabled()) {
            return false;
        }
        if (ConditionNodeType.ALWAYS_FALSE.equals(node.type())) {
            return true;
        }
        for (ConditionNode child : node.children()) {
            if (containsAlwaysFalse(child)) {
                return true;
            }
        }
        return false;
    }

    public record Binding(
            String targetType,
            String targetId,
            String conditionGroupId,
            ConditionRuntimeTargetType runtimeTargetType,
            String navigationTarget,
            ConditionGroupCompatibilityProfile compatibilityProfile
    ) {
        public Binding(
                String targetType,
                String targetId,
                String conditionGroupId,
                ConditionRuntimeTargetType runtimeTargetType,
                String navigationTarget
        ) {
            this(targetType, targetId, conditionGroupId, runtimeTargetType, navigationTarget, null);
        }

        public Binding normalized() {
            return new Binding(
                    safe(targetType).isBlank() ? "SYSTEM" : safe(targetType),
                    safe(targetId),
                    WebAdminConditionGroupStore.normalizeId(conditionGroupId),
                    runtimeTargetType == null ? ConditionRuntimeTargetType.VBD_INTERACTION : runtimeTargetType,
                    safe(navigationTarget),
                    compatibilityProfile
            );
        }

        static Binding empty() {
            return new Binding("", "", "", ConditionRuntimeTargetType.VBD_INTERACTION, "", null);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
