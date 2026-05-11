package com.zcpu.tzzmod.signal.device.debug;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalChannelInspector;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.ContainerDeviceSupport;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionSupport;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.signal.device.VirtualBlockPowerState;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class VirtualBlockDeviceDiagnosticService {
    private VirtualBlockDeviceDiagnosticService() {
    }

    public static DeviceDiagnostic diagnose(MinecraftServer server, SignalDeviceData rawDevice, long currentGameTime) {
        boolean rawItemSubmitEnabled = rawDevice != null && rawDevice.itemSubmitEnabled();
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return new DeviceDiagnostic("", "", "", "", List.of(issue(
                    DiagnosticSeverity.ERROR,
                    "device_missing",
                    "设备不存在",
                    "未找到可诊断的 SignalDeviceData。",
                    "请确认设备 ID 或短 ID 是否正确。"
            )));
        }

        List<DiagnosticIssue> issues = new ArrayList<>();
        addStaticIssues(device, rawItemSubmitEnabled, issues);
        addChannelIssues(server, device, issues);
        addRuntimeIssues(server, device, currentGameTime, issues);
        addDeviceContext(device, issues);
        return new DeviceDiagnostic(
                device.id(),
                SignalDeviceStore.displayName(device),
                device.type(),
                SignalDeviceStore.positionText(device),
                issues
        );
    }

    public static DeviceDiagnostic diagnoseStatic(SignalDeviceData rawDevice) {
        return diagnose(null, rawDevice, 0L);
    }

    public static InteractionItemDiagnostic interactionItemDiagnostic(SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return new InteractionItemDiagnostic(false, "", false, false, 0, "", List.of());
        }
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        List<DiagnosticIssue> issues = new ArrayList<>();
        addInteractionItemIssues(device, matcher, issues);
        return new InteractionItemDiagnostic(
                device.interactionItemMatcherEnabled(),
                matcher.interactionItemSource(),
                InteractionItemSource.supportsConsume(matcher.interactionItemSource()),
                matcher.consumeEnabled(),
                matcher.consumeCount(),
                matcher.interactionItemVanillaPolicy(),
                issues
        );
    }

    public static ItemSubmitDiagnostic itemSubmitDiagnostic(SignalDeviceData rawDevice) {
        boolean rawItemSubmitEnabled = rawDevice != null && rawDevice.itemSubmitEnabled();
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return new ItemSubmitDiagnostic(false, 0, 0, false, InventoryConsumeOrder.HOTBAR_FIRST, List.of());
        }
        List<DiagnosticIssue> issues = new ArrayList<>();
        addItemSubmitIssues(device, rawItemSubmitEnabled, issues);
        return new ItemSubmitDiagnostic(
                rawItemSubmitEnabled,
                device.itemSubmitRequirements().size(),
                enabledRequirementCount(device),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                issues
        );
    }

    private static void addStaticIssues(SignalDeviceData device, boolean rawItemSubmitEnabled, List<DiagnosticIssue> issues) {
        if (!device.enabled()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "device_disabled",
                    "设备已禁用",
                    "该设备不会触发 signal。",
                    "需要恢复时执行 /tzz signal device enable <device>"
            ));
        }

        if (device.channel().isBlank()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "channel_empty",
                    "主频道未设置",
                    "红石或默认成功触发没有主 channel。",
                    "如需要红石触发，请重新 bind 或设置对应频道。"
            ));
        } else if (!SignalChannel.isValid(device.channel())) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "channel_invalid",
                    "主频道无效",
                    "频道名称不符合 SignalBridge channel 规则。",
                    "请使用有效 channel 名称重新配置。"
            ).withChannel(device.channel()));
        }

        if (device.interactionEnabled() && device.interactChannel().isBlank()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "interact_channel_empty",
                    "交互频道未设置",
                    "interaction 已启用，但没有 interactChannel。",
                    "执行 /tzz signal blockDevice interactChannel <pos> <channel>"
            ));
        }

        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        addInteractionItemIssues(device, matcher, issues);
        addItemSubmitIssues(device, rawItemSubmitEnabled, issues);
        addContainerIssues(device, issues);
        addItemConditionIssues(device, issues);
    }

    private static void addInteractionItemIssues(
            SignalDeviceData device,
            ItemStackMatcherData matcher,
            List<DiagnosticIssue> issues
    ) {
        if (device.itemSubmitEnabled() && device.interactionItemMatcherEnabled()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "interaction_item_ignored_by_submit",
                    "单物品 matcher 被 itemSubmit 模式忽略",
                    "itemSubmitEnabled=true 时，多物品提交是当前匹配模式，interactionItem matcher 不参与最终匹配。",
                    "如需恢复单物品匹配，先 disable itemSubmit，再 enable interactionItem。"
            ));
        }
        if (device.interactionItemMatcherEnabled() && !matcher.enabled()) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "interaction_item_template_missing",
                    "交互物品模板缺失",
                    "interactionItem matcher 已启用，但模板为空或未启用。",
                    "执行 /tzz signal blockDevice interactionItem setFromHand <pos>"
            ));
        }
        if (device.interactionItemMatcherEnabled()
                && device.interactChannel().isBlank()
                && matcher.successChannel().isBlank()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "interaction_success_channel_missing",
                    "成功频道未配置",
                    "匹配成功时没有 successChannel，也没有 interactChannel 可回退。",
                    "配置 interactChannel 或 interactionItem successChannel。"
            ));
        }
        if (!matcher.successChannel().isBlank() && !SignalChannel.isValid(matcher.successChannel())) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "interaction_success_channel_invalid",
                    "成功频道无效",
                    "interactionItem successChannel 不符合 channel 规则。",
                    "重新设置 successChannel。"
            ).withChannel(matcher.successChannel()));
        }
        if (!matcher.failChannel().isBlank() && !SignalChannel.isValid(matcher.failChannel())) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "interaction_fail_channel_invalid",
                    "失败频道无效",
                    "interactionItem failChannel 不符合 channel 规则。",
                    "重新设置 failChannel，或 clearFailChannel。"
            ).withChannel(matcher.failChannel()));
        }
        if (matcher.consumeEnabled() && matcher.consumeCount() <= 0) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "consume_count_invalid",
                    "消耗数量无效",
                    "consumeEnabled=true，但 consumeCount 小于 1。",
                    "执行 /tzz signal blockDevice interactionItem consumeCount <pos> <count>"
            ));
        }
        if (matcher.consumeEnabled() && !InteractionItemSource.supportsConsume(matcher.interactionItemSource())) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "consume_source_unsupported",
                    "当前物品来源不支持消耗",
                    "source=" + InteractionItemSource.displayName(matcher.interactionItemSource()) + "，但 consumeEnabled=true。",
                    "关闭 consume，或切回支持消耗的 main_hand / off_hand / inventory_contains。"
            ));
        }
        if (InteractionItemVanillaPolicy.blocksVanillaOnFailure(matcher.interactionItemVanillaPolicy())) {
            issues.add(issue(
                    DiagnosticSeverity.INFO,
                    "vanilla_policy_lock",
                    "原版交互锁已启用",
                    "require_item_match 是锁；cooldown 不会解除锁，也不会跳过成功消耗。",
                    "如果只需要反馈不需要锁，改为 vanillaInteraction allow。"
            ));
        }
    }

    private static void addItemSubmitIssues(SignalDeviceData device, boolean rawItemSubmitEnabled, List<DiagnosticIssue> issues) {
        if (!rawItemSubmitEnabled) {
            return;
        }

        int enabledCount = enabledRequirementCount(device);
        if (enabledCount <= 0) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "item_submit_no_enabled_requirements",
                    "多物品提交没有启用条件",
                    "itemSubmitEnabled=true，但没有任何 enabled requirement，提交不会成功。",
                    "添加并启用至少一个 itemSubmit requirement。"
            ));
        }
        if (device.itemSubmitConsumeEnabled() && device.itemSubmitRequirements().isEmpty()) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    "item_submit_consume_without_requirements",
                    "提交消耗缺少条件",
                    "itemSubmit consume 已启用，但没有 requirement 可用于构建原子消耗计划。",
                    "添加 requirement 或关闭 itemSubmit consume。"
            ));
        }

        for (ItemSubmitRequirementData rawRequirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData requirement = rawRequirement.normalized();
            if (!requirement.enabled()) {
                issues.add(issue(
                        DiagnosticSeverity.INFO,
                        "item_submit_requirement_disabled",
                        "提交条件已禁用",
                        "条件“" + requirement.name() + "”不会参与 itemSubmit 判断或消耗。",
                        "需要时执行 enableRequirement。"
                ));
                continue;
            }
            if (!requirement.lastMatched()) {
                issues.add(issue(
                        DiagnosticSeverity.WARNING,
                        "item_submit_requirement_not_met",
                        "提交条件未满足",
                        "条件：" + requirement.name()
                                + "\n当前匹配数量：" + requirement.lastMatchedCount()
                                + "\n要求数量：" + diagnosticCountRequirementText(requirement.matcher()),
                        "检查玩家背包/热键栏中的物品、物品匹配模板或数量模式。"
                ));
            }
            if (device.itemSubmitConsumeEnabled() && requirement.consumeCount() <= 0) {
                issues.add(issue(
                        DiagnosticSeverity.ERROR,
                        "item_submit_consume_count_invalid",
                        "提交条件消耗数量无效",
                        "条件“" + requirement.name() + "”的 consumeCount 小于 1。",
                        "执行 /tzz signal blockDevice itemSubmit consumeCount <pos> <name> <count>"
                ));
            }
        }
    }

    private static void addContainerIssues(SignalDeviceData device, List<DiagnosticIssue> issues) {
        boolean hasContainerChannel = !device.containerOpenChannel().isBlank()
                || !device.containerCloseChannel().isBlank()
                || !device.containerChangeChannel().isBlank();
        if (device.containerEnabled() && !hasContainerChannel) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "container_enabled_without_channel",
                    "容器事件启用但没有频道",
                    "open / close / change channel 均为空。",
                    "设置至少一个 container channel，或 disable container。"
            ));
        }
        addChannelValidityIssue("container_open_channel_invalid", "容器打开频道无效", device.containerOpenChannel(), issues);
        addChannelValidityIssue("container_close_channel_invalid", "容器关闭频道无效", device.containerCloseChannel(), issues);
        addChannelValidityIssue("container_change_channel_invalid", "容器变化频道无效", device.containerChangeChannel(), issues);
    }

    private static void addItemConditionIssues(SignalDeviceData device, List<DiagnosticIssue> issues) {
        for (ContainerItemConditionData rawCondition : device.itemConditions()) {
            ContainerItemConditionData condition = rawCondition.normalized();
            if (!condition.enabled()) {
                issues.add(issue(
                        DiagnosticSeverity.INFO,
                        "item_condition_disabled",
                        "物品条件已禁用",
                        "条件“" + condition.name() + "”当前不参与检测。",
                        "需要时执行 itemCondition enable。"
                ));
            }
            String effectiveChannel = ContainerItemConditionSupport.effectiveChannel(condition, device.containerChangeChannel(), false);
            if (effectiveChannel.isBlank()) {
                issues.add(issue(
                        DiagnosticSeverity.ERROR,
                        "item_condition_channel_empty",
                        "物品条件频道为空",
                        "条件“" + condition.name() + "”没有显式频道，且父 VBD 未配置容器内容变化频道。",
                        "设置容器内容变化频道，或为该条件设置显式频道。"
                ));
            } else if (!SignalChannel.isValid(effectiveChannel)) {
                issues.add(issue(
                        DiagnosticSeverity.ERROR,
                        "item_condition_channel_invalid",
                        "物品条件频道无效",
                        "条件“" + condition.name() + "”的有效 channel 不符合规则。",
                        "修正显式 itemCondition channel，或修正父 VBD 容器内容变化频道。"
                ).withChannel(effectiveChannel));
            }
            if (!condition.offChannel().isBlank() && !SignalChannel.isValid(condition.offChannel())) {
                issues.add(issue(
                        DiagnosticSeverity.ERROR,
                        "item_condition_off_channel_invalid",
                        "物品条件退出频道无效",
                        "条件“" + condition.name() + "”的 offChannel 不符合规则。",
                        "清除或重新设置 offChannel。"
                ).withChannel(condition.offChannel()));
            }
        }
    }

    private static void addChannelValidityIssue(
            String code,
            String title,
            String channel,
            List<DiagnosticIssue> issues
    ) {
        if (channel != null && !channel.isBlank() && !SignalChannel.isValid(channel)) {
            issues.add(issue(
                    DiagnosticSeverity.ERROR,
                    code,
                    title,
                    "频道“" + channel + "”不符合 SignalBridge channel 规则。",
                    "重新设置有效 channel。"
            ).withChannel(channel));
        }
    }

    private static void addChannelIssues(MinecraftServer server, SignalDeviceData device, List<DiagnosticIssue> issues) {
        if (server == null) {
            return;
        }

        addNoConsumerIssue(server, device.channel(), "channel_no_consumers", "主频道暂无消费者", issues);
        addNoConsumerIssue(server, device.interactChannel(), "interact_channel_no_consumers", "交互频道暂无消费者", issues);
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        addNoConsumerIssue(server, matcher.successChannel(), "success_channel_no_consumers", "成功频道暂无消费者", issues);
        addNoConsumerIssue(server, matcher.failChannel(), "fail_channel_no_consumers", "失败频道暂无消费者", issues);
        addNoConsumerIssue(server, device.containerOpenChannel(), "container_open_channel_no_consumers", "容器打开频道暂无消费者", issues);
        addNoConsumerIssue(server, device.containerCloseChannel(), "container_close_channel_no_consumers", "容器关闭频道暂无消费者", issues);
        addNoConsumerIssue(server, device.containerChangeChannel(), "container_change_channel_no_consumers", "容器变化频道暂无消费者", issues);
    }

    private static void addNoConsumerIssue(
            MinecraftServer server,
            String channel,
            String code,
            String title,
            List<DiagnosticIssue> issues
    ) {
        String normalized = SignalChannel.normalize(channel);
        if (normalized.isBlank() || !SignalChannel.isValid(normalized)) {
            return;
        }

        ChannelConsumers consumers = channelConsumers(server, normalized);
        if (consumers.total() <= 0) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    code,
                    title,
                    "频道：" + normalized
                            + "\n说明：信号会发出并写入历史，但当前不会触发任何监听器、接收器或动作继电器。",
                    "添加监听器 listener、接收器 signal_receiver 或动作继电器 action_relay。"
            ).withChannel(normalized));
        }
    }

    private static void addRuntimeIssues(
            MinecraftServer server,
            SignalDeviceData device,
            long currentGameTime,
            List<DiagnosticIssue> issues
    ) {
        if (server == null) {
            return;
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (world == null) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "dimension_unavailable",
                    "维度不可用",
                    "设备记录的维度当前无法解析。",
                    "确认维度 ID 是否仍存在。"
            ));
            return;
        }
        if (!world.isChunkLoaded(pos)) {
            issues.add(issue(
                    DiagnosticSeverity.INFO,
                    "chunk_unloaded",
                    "区块未加载",
                    "诊断不会强制加载区块；tick 检测会跳过该位置。",
                    "需要实时诊断时靠近该区块或加载区块。"
            ));
            return;
        }

        VirtualBlockPowerState powerState = VirtualBlockDeviceSupport.powerState(world, pos);
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "virtual_block_air",
                    "绑定位置为空气",
                    "当前方块为空气，cleanup 可以清理该 virtual_block_device 记录。",
                    "执行 /tzz signal device cleanup"
            ));
        } else if (!powerStateBlockId(powerState).isBlank() && !powerStateBlockId(powerState).equals(device.blockId())) {
            issues.add(issue(
                    DiagnosticSeverity.WARNING,
                    "virtual_block_id_mismatch",
                    "当前方块与绑定方块不一致",
                    "绑定时 blockId=" + device.blockId() + "，当前 blockId=" + powerStateBlockId(powerState) + "；触发会被跳过。",
                    "执行 refresh 或重新 bind。"
            ));
        }
        if (state.getBlock() instanceof DoorBlock) {
            issues.add(issue(
                    DiagnosticSeverity.INFO,
                    "door_half_normalization",
                    "门双格归一化已启用",
                    "右键门的另一半时，会额外检查另一半坐标上的 virtual_block_device，避免锁被绕过。",
                    "绑定上半格或下半格均可，但建议保持项目内一致。"
            ));
        }

        long remainingInteractionCooldown = SignalDeviceStore.getRemainingInteractionCooldownTicks(device, currentGameTime);
        if (remainingInteractionCooldown > 0L) {
            issues.add(issue(
                    DiagnosticSeverity.INFO,
                    "interaction_cooldown_active",
                    "交互冷却中",
                    "剩余 " + remainingInteractionCooldown + " GT；cooldown 只抑制反馈和 signal，不解除锁，也不跳过成功消耗。",
                    "等待 cooldown 结束，或调整 interactionCooldown。"
            ));
        }

        if (!device.itemConditions().isEmpty()) {
            Inventory inventory = ContainerDeviceSupport.isContainer(world, pos)
                    ? ContainerItemConditionSupport.inventory(world, pos)
                    : null;
            if (inventory == null) {
                issues.add(issue(
                        DiagnosticSeverity.WARNING,
                        "item_conditions_container_unavailable",
                        "物品条件缺少可用容器",
                        "已配置 itemCondition，但当前位置不是可读取容器。",
                        "确认 blockId，或 refresh / 重新 bind。"
                ));
            } else {
                for (ContainerItemConditionData condition : device.itemConditions()) {
                    for (String validationIssue : ContainerItemConditionSupport.validate(inventory, condition, device.containerChangeChannel())) {
                        issues.add(issue(
                                DiagnosticSeverity.WARNING,
                                "item_condition_validation",
                                "物品条件配置需要检查",
                                "条件“" + condition.normalized().name() + "”： " + validationIssue,
                                "按提示修正 slot、itemId 或 matcher 配置。"
                        ));
                    }
                }
            }
        }
    }

    private static void addDeviceContext(SignalDeviceData device, List<DiagnosticIssue> issues) {
        String id = device.id();
        String name = SignalDeviceStore.displayName(device);
        String pos = SignalDeviceStore.positionText(device);
        for (int index = 0; index < issues.size(); index++) {
            DiagnosticIssue issue = issues.get(index);
            issues.set(index, issue.withDevice(id, name, pos));
        }
    }

    private static ChannelConsumers channelConsumers(MinecraftServer server, String channel) {
        int listeners = 0;
        int receivers = 0;
        int relays = 0;
        if (server != null) {
            List<SignalListenerData> listenerData = SignalChannelInspector.getListenersForChannel(server, channel);
            listeners = listenerData.size();
            String normalized = SignalChannel.normalize(channel);
            for (SignalDeviceData device : SignalDeviceStore.getSnapshot(server)) {
                if (!device.enabled() || !SignalChannel.normalize(device.channel()).equals(normalized)) {
                    continue;
                }
                if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type())) {
                    receivers++;
                } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                    relays++;
                }
            }
        }
        return new ChannelConsumers(listeners, receivers, relays);
    }

    private static int enabledRequirementCount(SignalDeviceData device) {
        int count = 0;
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            if (requirement.normalized().enabled()) {
                count++;
            }
        }
        return count;
    }

    private static String diagnosticCountRequirementText(ItemStackMatcherData rawMatcher) {
        ItemStackMatcherData matcher = rawMatcher == null ? ItemStackMatcherData.empty() : rawMatcher.normalized();
        ContainerItemCountMode mode = ContainerItemCountMode.fromId(matcher.countMode());
        if (mode == ContainerItemCountMode.IGNORE) {
            return "不检查数量";
        }
        int requiredCount = Math.max(1, matcher.requiredCount());
        return switch (mode) {
            case EXACTLY -> "等于 " + requiredCount;
            case AT_MOST -> "至多 " + requiredCount;
            case IGNORE -> "不检查数量";
            default -> "至少 " + requiredCount;
        };
    }

    private static String powerStateBlockId(VirtualBlockPowerState powerState) {
        return powerState == null ? "" : powerState.blockId();
    }

    private static DiagnosticIssue issue(
            DiagnosticSeverity severity,
            String code,
            String title,
            String message,
            String suggestion
    ) {
        return DiagnosticIssue.of(severity, code, title, message, suggestion);
    }

    private record ChannelConsumers(int listeners, int receivers, int relays) {
        int total() {
            return listeners + receivers + relays;
        }
    }

}
