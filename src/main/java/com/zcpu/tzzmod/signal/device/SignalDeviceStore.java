package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

public final class SignalDeviceStore {
    private static final Map<MinecraftServer, State> CACHE = new WeakHashMap<>();

    private SignalDeviceStore() {
    }

    public record WebAdminNativeTriggerPatch(
            boolean redstoneEnabled,
            String channel,
            String offChannel,
            String mode,
            boolean conditionEnabled,
            String conditionBlockId,
            Map<String, String> conditionProperties,
            String conditionRaw,
            String conditionMode,
            boolean currentConditionMatched,
            String conditionResult,
            boolean interactionEnabled,
            String interactChannel,
            int interactionCooldownTicks,
            boolean containerOpenEnabled,
            String containerOpenChannel,
            boolean containerCloseEnabled,
            String containerCloseChannel,
            boolean containerChangeEnabled,
            String containerChangeChannel,
            int containerCooldownTicks,
            int containerChangeCheckIntervalTicks,
            String containerFingerprint
    ) {
        public WebAdminNativeTriggerPatch {
            conditionProperties = conditionProperties == null ? Map.of() : Map.copyOf(conditionProperties);
        }
    }

    public static synchronized List<SignalDeviceData> getSnapshot(MinecraftServer server) {
        State state = getState(server);
        refreshLoadedDevices(server, state);
        List<SignalDeviceData> result = new ArrayList<>(state.devices);
        result.sort(Comparator
                .comparing((SignalDeviceData device) -> displayName(device).toLowerCase())
                .thenComparing(SignalDeviceData::id));
        return List.copyOf(result);
    }

    public static synchronized SignalDeviceData upsertEmitter(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity) {
        State state = getState(world.getServer());
        SignalDeviceData rawExisting = findById(state, SignalEmitterBlockEntity.sourceId(world, pos));
        cleanupIfTypeChanged(world.getServer(), rawExisting, SignalDeviceData.TYPE_SIGNAL_EMITTER);
        SignalDeviceData existing = existingOfType(rawExisting, SignalDeviceData.TYPE_SIGNAL_EMITTER);
        SignalDeviceData updated = fromEmitter(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(existing == null ? WebAdminRealtimeEventType.DEVICE_REGISTERED : WebAdminRealtimeEventType.DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateChannel(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity) {
        return upsertEmitter(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateEnabled(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity) {
        return upsertEmitter(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData upsertReceiver(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        State state = getState(world.getServer());
        SignalDeviceData rawExisting = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        cleanupIfTypeChanged(world.getServer(), rawExisting, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        SignalDeviceData existing = existingOfType(rawExisting, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        SignalDeviceData updated = fromReceiver(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(existing == null ? WebAdminRealtimeEventType.DEVICE_REGISTERED : WebAdminRealtimeEventType.RECEIVER_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateChannel(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        return upsertReceiver(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateEnabled(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        return upsertReceiver(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updatePulse(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        State state = getState(world.getServer());
        SignalDeviceData rawExisting = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        cleanupIfTypeChanged(world.getServer(), rawExisting, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        SignalDeviceData existing = existingOfType(rawExisting, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        SignalDeviceData updated = fromReceiver(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.RECEIVER_PULSE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData upsertActionRelay(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        State state = getState(world.getServer());
        SignalDeviceData rawExisting = findById(state, ActionRelayBlockEntity.sourceId(world, pos));
        cleanupIfTypeChanged(world.getServer(), rawExisting, SignalDeviceData.TYPE_ACTION_RELAY);
        SignalDeviceData existing = existingOfType(rawExisting, SignalDeviceData.TYPE_ACTION_RELAY);
        SignalDeviceData updated = fromActionRelay(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(existing == null ? WebAdminRealtimeEventType.DEVICE_REGISTERED : WebAdminRealtimeEventType.DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateChannel(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        return upsertActionRelay(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateEnabled(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        return upsertActionRelay(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateCooldown(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        return upsertActionRelay(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateActions(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        SignalDeviceData updated = upsertActionRelay(world, pos, blockEntity);
        publishDeviceChange(WebAdminRealtimeEventType.ACTION_CONFIG_CHANGED, updated);
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData upsertVirtualBlock(ServerWorld world, BlockPos pos, String channel) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        VirtualBlockPowerState powerState = VirtualBlockDeviceSupport.powerState(world, pos);
        SignalDeviceData updated = fromVirtualBlock(
                world,
                pos,
                existing,
                SignalChannel.normalize(channel),
                existing == null ? "" : existing.offChannel(),
                existing == null ? VirtualBlockDeviceMode.REDSTONE_RISING.id() : existing.mode(),
                existing == null || existing.enabled(),
                powerState,
                false,
                existing == null ? "" : existing.name()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(existing == null ? WebAdminRealtimeEventType.DEVICE_REGISTERED : WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualOffChannel(ServerWorld world, BlockPos pos, String offChannel) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withVirtualSettings(
                existing,
                existing.channel(),
                SignalChannel.normalize(offChannel),
                existing.mode(),
                existing.enabled()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualMode(ServerWorld world, BlockPos pos, String mode) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withVirtualSettings(
                existing,
                existing.channel(),
                existing.offChannel(),
                VirtualBlockDeviceMode.normalize(mode),
                existing.enabled()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualEnabled(ServerWorld world, BlockPos pos, boolean enabled) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withVirtualSettings(existing, existing.channel(), existing.offChannel(), existing.mode(), enabled);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateBasicConfig(MinecraftServer server, String deviceId, boolean enabled, String channel) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return null;
        }
        State state = getState(server);
        refreshLoadedDevices(server, state);
        SignalDeviceData existing = findById(state, cleanUserText(deviceId));
        if (existing == null) {
            return null;
        }
        String normalizedChannel = SignalChannel.normalize(channel);
        SignalDeviceData updated = null;
        if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(existing.type())) {
            updated = withVirtualSettings(existing, normalizedChannel, existing.offChannel(), existing.mode(), enabled);
            replaceOrAdd(state, updated);
            state.markDirty();
            publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        } else if (isPhysicalSignalDeviceType(existing.type())) {
            ServerWorld world = getDeviceWorld(server, existing);
            if (world != null) {
                BlockPos pos = new BlockPos(existing.x(), existing.y(), existing.z());
                if (SignalDeviceData.TYPE_SIGNAL_EMITTER.equals(existing.type())) {
                    SignalEmitterBlockEntity emitter = getLoadedEmitter(server, existing);
                    if (emitter != null) {
                        emitter.setEnabled(enabled);
                        emitter.setChannel(normalizedChannel);
                        updated = updateChannel(world, pos, emitter);
                    }
                } else if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(existing.type())) {
                    SignalReceiverBlockEntity receiver = getLoadedReceiver(server, existing);
                    if (receiver != null) {
                        receiver.setEnabled(enabled);
                        receiver.setChannel(normalizedChannel);
                        updated = updateChannel(world, pos, receiver);
                    }
                } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(existing.type())) {
                    ActionRelayBlockEntity relay = getLoadedActionRelay(server, existing);
                    if (relay != null) {
                        relay.setEnabled(enabled);
                        relay.setChannel(normalizedChannel);
                        updated = updateChannel(world, pos, relay);
                    }
                }
            }
            if (updated == null) {
                updated = withBasicConfigForWebAdmin(existing, enabled, normalizedChannel);
                replaceOrAdd(state, updated);
                state.markDirty();
                publishDeviceChange(WebAdminRealtimeEventType.DEVICE_CHANGED, updated);
            }
        }
        if (updated != null) {
            state.flushDirty(true, currentGameTime(server));
        }
        return updated == null ? null : updated.normalized();
    }

    private static boolean isPhysicalSignalDeviceType(String type) {
        return SignalDeviceData.TYPE_SIGNAL_EMITTER.equals(type)
                || SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(type)
                || SignalDeviceData.TYPE_ACTION_RELAY.equals(type);
    }

    public static SignalDeviceData withBasicConfigForWebAdmin(SignalDeviceData device, boolean enabled, String channel) {
        if (device == null) {
            return null;
        }
        return withVirtualSettings(device.normalized(), SignalChannel.normalize(channel), device.offChannel(), device.mode(), enabled);
    }

    public static synchronized SignalDeviceData updateExtendedConfig(MinecraftServer server, String deviceId, ExtendedConfigPatch patch) {
        if (server == null || deviceId == null || deviceId.isBlank() || patch == null) {
            return null;
        }
        State state = getState(server);
        refreshLoadedDevices(server, state);
        SignalDeviceData existing = findById(state, cleanUserText(deviceId));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = null;
        if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(existing.type())) {
            updated = applyVirtualExtendedPatch(existing, patch);
            replaceOrAdd(state, updated);
            state.markDirty();
            publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        } else {
            ServerWorld world = getDeviceWorld(server, existing);
            if (world == null) {
                return null;
            }
            BlockPos pos = new BlockPos(existing.x(), existing.y(), existing.z());
            if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(existing.type()) && patch.pulseTicks() != null) {
                SignalReceiverBlockEntity receiver = getLoadedReceiver(server, existing);
                if (receiver == null) {
                    return null;
                }
                receiver.setPulseTicks(patch.pulseTicks());
                updated = updatePulse(world, pos, receiver);
            } else if (SignalDeviceData.TYPE_ACTION_RELAY.equals(existing.type()) && patch.cooldownTicks() != null) {
                ActionRelayBlockEntity relay = getLoadedActionRelay(server, existing);
                if (relay == null) {
                    return null;
                }
                relay.setCooldownTicks(patch.cooldownTicks());
                updated = updateCooldown(world, pos, relay);
            }
        }
        if (updated != null) {
            state.flushDirty(true, currentGameTime(server));
        }
        return updated == null ? null : updated.normalized();
    }

    public static SignalDeviceData withExtendedConfigForWebAdmin(SignalDeviceData device, ExtendedConfigPatch patch) {
        if (device == null || patch == null) {
            return device;
        }
        SignalDeviceData normalized = device.normalized();
        if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(normalized.type())) {
            return applyVirtualExtendedPatch(normalized, patch);
        }
        if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(normalized.type()) && patch.pulseTicks() != null) {
            return withPulseCooldown(normalized, patch.pulseTicks(), normalized.cooldownTicks());
        }
        if (SignalDeviceData.TYPE_ACTION_RELAY.equals(normalized.type()) && patch.cooldownTicks() != null) {
            return withPulseCooldown(normalized, normalized.pulseTicks(), patch.cooldownTicks());
        }
        return normalized;
    }

    public static SignalDeviceData withInteractionItemMatcherForWebAdmin(
            SignalDeviceData device,
            ItemStackMatcherData matcher,
            boolean enabled
    ) {
        if (device == null) {
            return null;
        }
        SignalDeviceData normalized = device.normalized();
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(normalized.type())) {
            return normalized;
        }
        return withInteractionItemMatcher(
                normalized,
                enabled,
                matcher == null ? ItemStackMatcherData.empty() : matcher,
                normalized.lastInteractionItemMatched(),
                normalized.lastInteractionItemResult()
        );
    }

    private static SignalDeviceData applyVirtualExtendedPatch(SignalDeviceData existing, ExtendedConfigPatch patch) {
        SignalDeviceData updated = existing.normalized();
        if (patch.updateInteractChannel()) {
            String normalizedChannel = patch.clearInteractChannel() ? "" : SignalChannel.normalize(patch.interactChannel());
            updated = withInteraction(
                    updated,
                    !normalizedChannel.isBlank(),
                    normalizedChannel,
                    updated.interactionCooldownTicks(),
                    updated.lastInteractionGameTime(),
                    updated.lastInteractionWallTimeMillis(),
                    updated.lastInteractionPlayerName(),
                    updated.lastInteractionPlayerUuid(),
                    updated.lastInteractionResult(),
                    updated.lastInteractionHand(),
                    updated.lastInteractionSide()
            );
        }
        if (patch.interactionCooldownTicks() != null) {
            updated = withInteraction(
                    updated,
                    updated.interactionEnabled(),
                    updated.interactChannel(),
                    patch.interactionCooldownTicks(),
                    updated.lastInteractionGameTime(),
                    updated.lastInteractionWallTimeMillis(),
                    updated.lastInteractionPlayerName(),
                    updated.lastInteractionPlayerUuid(),
                    updated.lastInteractionResult(),
                    updated.lastInteractionHand(),
                    updated.lastInteractionSide()
            );
        }
        ItemStackMatcherData matcher = updated.interactionItemMatcher();
        boolean matcherChanged = false;
        if (patch.updateSuccessChannel()) {
            matcher = ItemStackMatcherSupport.withSuccessChannel(matcher, patch.clearSuccessChannel() ? "" : SignalChannel.normalize(patch.successChannel()));
            matcherChanged = true;
        }
        if (patch.updateFailChannel()) {
            matcher = ItemStackMatcherSupport.withFailChannel(matcher, patch.clearFailChannel() ? "" : SignalChannel.normalize(patch.failChannel()));
            matcherChanged = true;
        }
        if (matcherChanged) {
            updated = withInteractionItemMatcher(
                    updated,
                    updated.interactionItemMatcherEnabled(),
                    matcher,
                    updated.lastInteractionItemMatched(),
                    updated.lastInteractionItemResult()
            );
        }
        return updated.normalized();
    }

    public record ExtendedConfigPatch(
            String interactChannel,
            boolean updateInteractChannel,
            boolean clearInteractChannel,
            String successChannel,
            boolean updateSuccessChannel,
            boolean clearSuccessChannel,
            String failChannel,
            boolean updateFailChannel,
            boolean clearFailChannel,
            Integer interactionCooldownTicks,
            Integer pulseTicks,
            Integer cooldownTicks
    ) {
    }

    public static synchronized SignalDeviceData refreshVirtualBlock(ServerWorld world, BlockPos pos) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        VirtualBlockPowerState powerState = VirtualBlockDeviceSupport.powerState(world, pos);
        SignalDeviceData updated = fromVirtualBlock(
                world,
                pos,
                existing,
                existing.channel(),
                existing.offChannel(),
                existing.mode(),
                existing.enabled(),
                powerState,
                false,
                existing.name()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualCondition(
            ServerWorld world,
            BlockPos pos,
            BlockStateCondition condition,
            boolean currentMatched
    ) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null || condition == null) {
            return null;
        }

        SignalDeviceData updated = withCondition(
                existing,
                true,
                condition.blockId(),
                condition.properties(),
                condition.raw(),
                existing.conditionMode(),
                currentMatched,
                world.getTime(),
                currentMatched ? "当前满足条件" : "当前不满足条件"
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData clearVirtualCondition(ServerWorld world, BlockPos pos) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withCondition(
                existing,
                false,
                "",
                Map.of(),
                "",
                BlockStateConditionMode.CONDITION_ENTER.id(),
                false,
                world.getTime(),
                "已清空方块状态条件"
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualConditionMode(ServerWorld world, BlockPos pos, String mode) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withCondition(
                existing,
                existing.conditionEnabled(),
                existing.conditionBlockId(),
                existing.conditionProperties(),
                existing.conditionRaw(),
                BlockStateConditionMode.normalize(mode),
                existing.lastConditionMatched(),
                existing.lastConditionCheckGameTime(),
                existing.lastConditionResult()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualInteractChannel(ServerWorld world, BlockPos pos, String channel) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        String normalizedChannel = SignalChannel.normalize(channel);
        SignalDeviceData updated = withInteraction(
                existing,
                !normalizedChannel.isBlank(),
                normalizedChannel,
                existing.interactionCooldownTicks(),
                existing.lastInteractionGameTime(),
                existing.lastInteractionWallTimeMillis(),
                existing.lastInteractionPlayerName(),
                existing.lastInteractionPlayerUuid(),
                existing.lastInteractionResult(),
                existing.lastInteractionHand(),
                existing.lastInteractionSide()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData clearVirtualInteractChannel(ServerWorld world, BlockPos pos) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withInteraction(
                existing,
                false,
                "",
                existing.interactionCooldownTicks(),
                existing.lastInteractionGameTime(),
                existing.lastInteractionWallTimeMillis(),
                existing.lastInteractionPlayerName(),
                existing.lastInteractionPlayerUuid(),
                "已清空交互触发频道",
                existing.lastInteractionHand(),
                existing.lastInteractionSide()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualInteractionEnabled(ServerWorld world, BlockPos pos, boolean enabled) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withInteraction(
                existing,
                enabled,
                existing.interactChannel(),
                existing.interactionCooldownTicks(),
                existing.lastInteractionGameTime(),
                existing.lastInteractionWallTimeMillis(),
                existing.lastInteractionPlayerName(),
                existing.lastInteractionPlayerUuid(),
                existing.lastInteractionResult(),
                existing.lastInteractionHand(),
                existing.lastInteractionSide()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualInteractionCooldown(ServerWorld world, BlockPos pos, int cooldownTicks) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withInteraction(
                existing,
                existing.interactionEnabled(),
                existing.interactChannel(),
                cooldownTicks,
                existing.lastInteractionGameTime(),
                existing.lastInteractionWallTimeMillis(),
                existing.lastInteractionPlayerName(),
                existing.lastInteractionPlayerUuid(),
                existing.lastInteractionResult(),
                existing.lastInteractionHand(),
                existing.lastInteractionSide()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualInteractionItemMatcher(
            ServerWorld world,
            BlockPos pos,
            ItemStackMatcherData matcher,
            boolean enabled,
            String result
    ) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withInteractionItemMatcher(
                existing,
                enabled,
                matcher == null ? ItemStackMatcherData.empty() : matcher,
                false,
                result
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData clearVirtualInteractionItemMatcher(ServerWorld world, BlockPos pos) {
        return updateVirtualInteractionItemMatcher(world, pos, ItemStackMatcherData.empty(), false, "已清空交互主手物品匹配");
    }

    public static synchronized SignalDeviceData updateVirtualContainerOpenChannel(ServerWorld world, BlockPos pos, String channel) {
        return updateVirtualContainerChannels(world, pos, SignalChannel.normalize(channel), null, null, null);
    }

    public static synchronized SignalDeviceData clearVirtualContainerOpenChannel(ServerWorld world, BlockPos pos) {
        return updateVirtualContainerChannels(world, pos, "", null, null, null);
    }

    public static synchronized SignalDeviceData updateVirtualContainerCloseChannel(ServerWorld world, BlockPos pos, String channel) {
        return updateVirtualContainerChannels(world, pos, null, SignalChannel.normalize(channel), null, null);
    }

    public static synchronized SignalDeviceData clearVirtualContainerCloseChannel(ServerWorld world, BlockPos pos) {
        return updateVirtualContainerChannels(world, pos, null, "", null, null);
    }

    public static synchronized SignalDeviceData updateVirtualContainerChangeChannel(
            ServerWorld world,
            BlockPos pos,
            String channel,
            String fingerprint
    ) {
        return updateVirtualContainerChannels(world, pos, null, null, SignalChannel.normalize(channel), fingerprint);
    }

    public static synchronized SignalDeviceData clearVirtualContainerChangeChannel(ServerWorld world, BlockPos pos) {
        return updateVirtualContainerChannels(world, pos, null, null, "", "");
    }

    public static synchronized SignalDeviceData updateVirtualContainerEnabled(ServerWorld world, BlockPos pos, boolean enabled) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withContainer(
                existing,
                enabled,
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                existing.containerCooldownTicks(),
                existing.containerChangeCheckIntervalTicks(),
                existing.lastContainerCheckGameTime(),
                existing.lastContainerFingerprint(),
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                existing.lastContainerResult(),
                existing.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualContainerCooldown(ServerWorld world, BlockPos pos, int cooldownTicks) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withContainer(
                existing,
                existing.containerEnabled(),
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                cooldownTicks,
                existing.containerChangeCheckIntervalTicks(),
                existing.lastContainerCheckGameTime(),
                existing.lastContainerFingerprint(),
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                existing.lastContainerResult(),
                existing.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualContainerCheckInterval(ServerWorld world, BlockPos pos, int ticks) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withContainer(
                existing,
                existing.containerEnabled(),
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                existing.containerCooldownTicks(),
                ticks,
                existing.lastContainerCheckGameTime(),
                existing.lastContainerFingerprint(),
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                existing.lastContainerResult(),
                existing.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData addVirtualItemCondition(
            ServerWorld world,
            BlockPos pos,
            ContainerItemConditionData condition
    ) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null || condition == null) {
            return null;
        }

        ContainerItemConditionData normalized = condition.normalized();
        List<ContainerItemConditionData> conditions = new ArrayList<>(existing.itemConditions());
        conditions.removeIf(candidate -> candidate.name().equalsIgnoreCase(normalized.name()));
        conditions.add(normalized);
        SignalDeviceData updated = withItemConditions(existing, conditions);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData removeVirtualItemCondition(ServerWorld world, BlockPos pos, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        String cleanName = cleanUserText(name);
        List<ContainerItemConditionData> conditions = new ArrayList<>(existing.itemConditions());
        boolean removed = conditions.removeIf(condition -> condition.name().equalsIgnoreCase(cleanName));
        if (!removed) {
            return existing;
        }

        SignalDeviceData updated = withItemConditions(existing, conditions);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData clearVirtualItemConditions(ServerWorld world, BlockPos pos) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withItemConditions(existing, List.of());
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualItemConditionsForWebAdmin(
            MinecraftServer server,
            String deviceId,
            List<ContainerItemConditionData> itemConditions
    ) {
        if (server == null || deviceId == null || deviceId.isBlank() || itemConditions == null) {
            return null;
        }
        State state = getState(server);
        refreshLoadedDevices(server, state);
        SignalDeviceData existing = findById(state, cleanUserText(deviceId));
        if (existing == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(existing.type())) {
            return null;
        }
        List<ContainerItemConditionData> normalizedConditions = new ArrayList<>();
        for (ContainerItemConditionData condition : itemConditions) {
            if (condition != null) {
                normalizedConditions.add(condition.normalized());
            }
        }
        SignalDeviceData updated = withItemConditions(existing, normalizedConditions);
        replaceOrAdd(state, updated);
        state.markDirty();
        state.flushDirty(true, currentGameTime(server));
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated.normalized();
    }

    public static synchronized SignalDeviceData updateVirtualItemCondition(ServerWorld world, BlockPos pos, ContainerItemConditionData condition) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null || condition == null) {
            return null;
        }

        ContainerItemConditionData normalized = condition.normalized();
        List<ContainerItemConditionData> conditions = new ArrayList<>();
        boolean replaced = false;
        for (ContainerItemConditionData candidate : existing.itemConditions()) {
            if (candidate.name().equalsIgnoreCase(normalized.name())) {
                conditions.add(normalized);
                replaced = true;
            } else {
                conditions.add(candidate);
            }
        }
        if (!replaced) {
            return existing;
        }

        SignalDeviceData updated = withItemConditions(existing, conditions);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualItemSubmit(
            ServerWorld world,
            BlockPos pos,
            boolean enabled,
            boolean consumeEnabled,
            String consumeOrder,
            List<ItemSubmitRequirementData> requirements,
            String result
    ) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withItemSubmit(
                existing,
                enabled,
                consumeEnabled,
                consumeOrder,
                requirements == null ? existing.itemSubmitRequirements() : requirements,
                existing.lastItemSubmitMatched(),
                existing.lastItemSubmitFailureReason(),
                existing.lastItemSubmitConsumedSummary(),
                result == null ? existing.lastItemSubmitResult() : result
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualItemSubmitForWebAdmin(
            MinecraftServer server,
            String deviceId,
            boolean enabled,
            List<ItemSubmitRequirementData> requirements,
            boolean disableInteractionItemMatcher
    ) {
        return updateVirtualItemSubmitForWebAdmin(
                server,
                deviceId,
                enabled,
                null,
                null,
                requirements,
                disableInteractionItemMatcher,
                ""
        );
    }

    public static synchronized SignalDeviceData updateVirtualItemSubmitForWebAdmin(
            MinecraftServer server,
            String deviceId,
            boolean enabled,
            Boolean consumeEnabled,
            String consumeOrder,
            List<ItemSubmitRequirementData> requirements,
            boolean disableInteractionItemMatcher,
            String interactionItemVanillaPolicy
    ) {
        if (server == null || deviceId == null || deviceId.isBlank() || requirements == null) {
            return null;
        }
        State state = getState(server);
        refreshLoadedDevices(server, state);
        SignalDeviceData existing = findById(state, cleanUserText(deviceId));
        if (existing == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(existing.type())) {
            return null;
        }
        List<ItemSubmitRequirementData> normalizedRequirements = new ArrayList<>();
        for (ItemSubmitRequirementData requirement : requirements) {
            if (requirement != null) {
                normalizedRequirements.add(requirement.normalized());
            }
        }
        SignalDeviceData updated = withItemSubmit(
                existing,
                enabled,
                consumeEnabled == null ? existing.itemSubmitConsumeEnabled() : consumeEnabled,
                consumeOrder == null || consumeOrder.isBlank() ? existing.itemSubmitConsumeOrder() : consumeOrder,
                normalizedRequirements,
                existing.lastItemSubmitMatched(),
                existing.lastItemSubmitFailureReason(),
                existing.lastItemSubmitConsumedSummary(),
                existing.lastItemSubmitResult()
        );
        if (interactionItemVanillaPolicy != null && !interactionItemVanillaPolicy.isBlank()) {
            updated = withInteractionItemMatcher(
                    updated,
                    updated.interactionItemMatcherEnabled(),
                    ItemStackMatcherSupport.withVanillaPolicy(updated.interactionItemMatcher(), interactionItemVanillaPolicy),
                    updated.lastInteractionItemMatched(),
                    updated.lastInteractionItemResult()
            );
        }
        if (disableInteractionItemMatcher && updated.interactionItemMatcherEnabled()) {
            updated = withInteractionItemMatcher(
                    updated,
                    false,
                    updated.interactionItemMatcher(),
                    updated.lastInteractionItemMatched(),
                    updated.lastInteractionItemResult()
            );
        }
        replaceOrAdd(state, updated);
        state.markDirty();
        state.flushDirty(true, currentGameTime(server));
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated.normalized();
    }

    public static synchronized void recordVirtualItemSubmitResult(
            ServerWorld world,
            SignalDeviceData device,
            boolean matched,
            String failureReason,
            String consumedSummary,
            String result
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        SignalDeviceData updated = withItemSubmit(
                existing,
                existing.itemSubmitEnabled(),
                existing.itemSubmitConsumeEnabled(),
                existing.itemSubmitConsumeOrder(),
                existing.itemSubmitRequirements(),
                matched,
                failureReason,
                consumedSummary,
                result
        );
        replaceOrAdd(state, updated);
        state.markDirty();
    }

    public static synchronized void recordVirtualItemConditionState(
            ServerWorld world,
            SignalDeviceData device,
            ContainerItemConditionData condition,
            boolean currentMatched,
            String resultMessage
    ) {
        if (world == null || device == null || condition == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        List<ContainerItemConditionData> conditions = new ArrayList<>();
        boolean changed = false;
        for (ContainerItemConditionData candidate : existing.itemConditions()) {
            if (candidate.name().equalsIgnoreCase(condition.name())) {
                ContainerItemConditionData updatedCondition = candidate.withMatched(currentMatched, world.getTime(), resultMessage);
                conditions.add(updatedCondition);
                changed = true;
            } else {
                conditions.add(candidate);
            }
        }
        if (!changed) {
            return;
        }

        SignalDeviceData updated = withItemConditions(existing, conditions);
        replaceOrAdd(state, updated);
        state.markDirty();
    }

    public static synchronized void recordVirtualItemConditionTrigger(
            ServerWorld world,
            SignalDeviceData device,
            ContainerItemConditionData condition,
            boolean currentMatched,
            ActionExecutionResult result
    ) {
        if (world == null || device == null || condition == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        long gameTime = world.getTime();
        List<ContainerItemConditionData> conditions = new ArrayList<>();
        boolean changed = false;
        for (ContainerItemConditionData candidate : existing.itemConditions()) {
            if (candidate.name().equalsIgnoreCase(condition.name())) {
                conditions.add(candidate.withTriggered(currentMatched, gameTime, now, resultMessage));
                changed = true;
            } else {
                conditions.add(candidate);
            }
        }
        if (!changed) {
            return;
        }

        SignalDeviceData conditioned = withItemConditions(existing, conditions);
        SignalDeviceData triggered = new SignalDeviceData(
                conditioned.id(),
                conditioned.type(),
                conditioned.name(),
                conditioned.dimension(),
                conditioned.x(),
                conditioned.y(),
                conditioned.z(),
                conditioned.channel(),
                conditioned.enabled(),
                conditioned.pulseTicks(),
                conditioned.remainingPulseTicks(),
                conditioned.cooldownTicks(),
                conditioned.actionCount(),
                conditioned.createdWallTimeMillis(),
                now,
                gameTime,
                now,
                resultMessage,
                conditioned.blockId(),
                conditioned.offChannel(),
                conditioned.mode(),
                conditioned.lastPowered(),
                conditioned.lastPowerLevel(),
                conditioned.conditionEnabled(),
                conditioned.conditionBlockId(),
                conditioned.conditionProperties(),
                conditioned.conditionRaw(),
                conditioned.conditionMode(),
                conditioned.lastConditionMatched(),
                conditioned.lastConditionCheckGameTime(),
                conditioned.lastConditionResult(),
                conditioned.interactionEnabled(),
                conditioned.interactChannel(),
                conditioned.interactionCooldownTicks(),
                conditioned.lastInteractionGameTime(),
                conditioned.lastInteractionWallTimeMillis(),
                conditioned.lastInteractionPlayerName(),
                conditioned.lastInteractionPlayerUuid(),
                conditioned.lastInteractionResult(),
                conditioned.lastInteractionHand(),
                conditioned.lastInteractionSide(),
                conditioned.containerEnabled(),
                conditioned.containerOpenChannel(),
                conditioned.containerCloseChannel(),
                conditioned.containerChangeChannel(),
                conditioned.containerCooldownTicks(),
                conditioned.containerChangeCheckIntervalTicks(),
                conditioned.lastContainerCheckGameTime(),
                conditioned.lastContainerFingerprint(),
                conditioned.lastContainerOpenGameTime(),
                conditioned.lastContainerOpenWallTimeMillis(),
                conditioned.lastContainerCloseGameTime(),
                conditioned.lastContainerCloseWallTimeMillis(),
                conditioned.lastContainerChangeGameTime(),
                conditioned.lastContainerChangeWallTimeMillis(),
                conditioned.lastContainerPlayerName(),
                conditioned.lastContainerPlayerUuid(),
                resultMessage,
                "item_condition",
                conditioned.itemConditions(),
                conditioned.interactionItemMatcherEnabled(),
                conditioned.interactionItemMatcher(),
                conditioned.lastInteractionItemMatched(),
                conditioned.lastInteractionItemResult(),
                conditioned.itemSubmitEnabled(),
                conditioned.itemSubmitConsumeEnabled(),
                conditioned.itemSubmitConsumeOrder(),
                conditioned.itemSubmitRequirements(),
                conditioned.lastItemSubmitMatched(),
                conditioned.lastItemSubmitFailureReason(),
                conditioned.lastItemSubmitConsumedSummary(),
                conditioned.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, triggered);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, triggered);
    }

    private static SignalDeviceData updateVirtualContainerChannels(
            ServerWorld world,
            BlockPos pos,
            String openChannel,
            String closeChannel,
            String changeChannel,
            String fingerprint
    ) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        String nextOpen = openChannel == null ? existing.containerOpenChannel() : SignalChannel.normalize(openChannel);
        String nextClose = closeChannel == null ? existing.containerCloseChannel() : SignalChannel.normalize(closeChannel);
        String nextChange = changeChannel == null ? existing.containerChangeChannel() : SignalChannel.normalize(changeChannel);
        boolean nextEnabled = existing.containerEnabled() || !nextOpen.isBlank() || !nextClose.isBlank() || !nextChange.isBlank();
        SignalDeviceData updated = withContainer(
                existing,
                nextEnabled,
                nextOpen,
                nextClose,
                nextChange,
                existing.containerCooldownTicks(),
                existing.containerChangeCheckIntervalTicks(),
                fingerprint == null ? existing.lastContainerCheckGameTime() : world.getTime(),
                fingerprint == null ? existing.lastContainerFingerprint() : fingerprint,
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                existing.lastContainerResult(),
                existing.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData updateVirtualNativeTriggersForWebAdmin(
            ServerWorld world,
            BlockPos pos,
            WebAdminNativeTriggerPatch patch
    ) {
        if (world == null || pos == null || patch == null) {
            return null;
        }
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(existing.type())) {
            return null;
        }

        SignalDeviceData updated = withVirtualSettings(
                existing,
                SignalChannel.normalize(patch.channel()),
                SignalChannel.normalize(patch.offChannel()),
                patch.redstoneEnabled()
                        ? VirtualBlockDeviceMode.normalize(patch.mode())
                        : VirtualBlockDeviceMode.REDSTONE_DISABLED.id(),
                existing.enabled()
        );
        updated = withCondition(
                updated,
                patch.conditionEnabled(),
                safe(patch.conditionBlockId()),
                patch.conditionProperties(),
                safe(patch.conditionRaw()),
                BlockStateConditionMode.normalize(patch.conditionMode()),
                patch.currentConditionMatched(),
                world.getTime(),
                safe(patch.conditionResult())
        );
        updated = withInteraction(
                updated,
                patch.interactionEnabled(),
                SignalChannel.normalize(patch.interactChannel()),
                Math.max(0, patch.interactionCooldownTicks()),
                updated.lastInteractionGameTime(),
                updated.lastInteractionWallTimeMillis(),
                updated.lastInteractionPlayerName(),
                updated.lastInteractionPlayerUuid(),
                updated.lastInteractionResult(),
                updated.lastInteractionHand(),
                updated.lastInteractionSide()
        );
        boolean nextContainerEnabled = patch.containerOpenEnabled() || patch.containerCloseEnabled() || patch.containerChangeEnabled();
        String nextOpen = patch.containerOpenEnabled()
                ? SignalChannel.normalize(patch.containerOpenChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerOpenChannel()));
        String nextClose = patch.containerCloseEnabled()
                ? SignalChannel.normalize(patch.containerCloseChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerCloseChannel()));
        String nextChange = patch.containerChangeEnabled()
                ? SignalChannel.normalize(patch.containerChangeChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerChangeChannel()));
        long checkTime = patch.containerChangeEnabled() && !safe(patch.containerFingerprint()).isBlank()
                ? world.getTime()
                : updated.lastContainerCheckGameTime();
        String fingerprint = patch.containerChangeEnabled() && !safe(patch.containerFingerprint()).isBlank()
                ? safe(patch.containerFingerprint())
                : updated.lastContainerFingerprint();
        updated = withContainer(
                updated,
                nextContainerEnabled,
                nextOpen,
                nextClose,
                nextChange,
                Math.max(0, patch.containerCooldownTicks()),
                Math.max(1, patch.containerChangeCheckIntervalTicks()),
                checkTime,
                fingerprint,
                updated.lastContainerOpenGameTime(),
                updated.lastContainerOpenWallTimeMillis(),
                updated.lastContainerCloseGameTime(),
                updated.lastContainerCloseWallTimeMillis(),
                updated.lastContainerChangeGameTime(),
                updated.lastContainerChangeWallTimeMillis(),
                updated.lastContainerPlayerName(),
                updated.lastContainerPlayerUuid(),
                updated.lastContainerResult(),
                updated.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalEmitterBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromEmitter(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromReceiver(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, ActionRelayBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromActionRelay(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED, updated);
        return updated;
    }

    public static synchronized SignalDeviceData setVirtualName(ServerWorld world, BlockPos pos, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, VirtualBlockDeviceSupport.id(world, pos));
        if (existing == null) {
            return null;
        }

        SignalDeviceData updated = withName(existing, cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED, updated);
        return updated;
    }

    public static synchronized ResolveResult clearName(MinecraftServer server, String deviceRef) {
        ResolveResult resolved = resolveDevice(server, deviceRef);
        if (!resolved.foundUnique()) {
            return resolved;
        }

        State state = getState(server);
        SignalDeviceData updated = withName(resolved.device(), "");
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_METADATA_CHANGED, updated);
        return ResolveResult.unique(updated);
    }

    public static synchronized boolean remove(MinecraftServer server, ServerWorld world, BlockPos pos) {
        return removeById(server, sourceId(world, pos));
    }

    public static synchronized boolean removeVirtualBlock(MinecraftServer server, ServerWorld world, BlockPos pos) {
        return removeById(server, VirtualBlockDeviceSupport.id(world, pos));
    }

    public static synchronized boolean removeById(MinecraftServer server, String sourceId) {
        if (server == null || sourceId == null || sourceId.isBlank()) {
            return false;
        }

        State state = getState(server);
        SignalDeviceData removedDevice = findById(state, sourceId);
        boolean removed = state.devices.removeIf(device -> sourceId.equals(device.id()));
        if (removed) {
            state.markDirty();
            cleanupWebAdminMetadata(server, sourceId, removedDevice == null ? "" : removedDevice.type());
            WebAdminRealtimeEventBus.publishDeviceRemoved(
                    sourceId,
                    removedDevice == null ? "" : removedDevice.type()
            );
        }
        return removed;
    }

    public static synchronized int cleanupInvalidLoadedDevices(MinecraftServer server) {
        State state = getState(server);
        int removed = 0;
        for (SignalDeviceData device : List.copyOf(state.devices)) {
            ServerWorld world = findWorld(server, device.dimension());
            if (world == null) {
                continue;
            }

            BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isAir() && state.devices.removeIf(candidate -> candidate.id().equals(device.id()))) {
                    removed++;
                    cleanupWebAdminMetadata(server, device.id(), device.type());
                    WebAdminRealtimeEventBus.publishDeviceRemoved(device.id(), device.type());
                }
                continue;
            }

            if (matchesLoadedDevice(world, pos, device.type())) {
                SignalDeviceData refreshed = refreshLoadedDevice(server, state, device);
                if (refreshed != null) {
                    replaceOrAdd(state, refreshed);
                }
                continue;
            }

            if (state.devices.removeIf(candidate -> candidate.id().equals(device.id()))) {
                removed++;
                cleanupWebAdminMetadata(server, device.id(), device.type());
                WebAdminRealtimeEventBus.publishDeviceRemoved(device.id(), device.type());
            }
        }
        if (removed > 0) {
            state.markDirty();
        }
        return removed;
    }

    public static synchronized List<SignalDeviceData> getVirtualBlockDevicesSnapshot(MinecraftServer server) {
        State state = getState(server);
        List<SignalDeviceData> result = new ArrayList<>();
        for (SignalDeviceData device : state.devices) {
            if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
                result.add(device.normalized());
            }
        }
        return List.copyOf(result);
    }

    public static synchronized SignalDeviceData findVirtualBlockDevice(MinecraftServer server, ServerWorld world, BlockPos pos) {
        if (server == null || world == null || pos == null) {
            return null;
        }
        State state = getState(server);
        return findById(state, VirtualBlockDeviceSupport.id(world, pos));
    }

    public static synchronized void recordTrigger(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity, ActionExecutionResult result) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalEmitterBlockEntity.sourceId(world, pos));
        SignalDeviceData base = fromEmitter(world, pos, blockEntity, existing, false);
        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        SignalDeviceData updated = new SignalDeviceData(
                base.id(),
                base.type(),
                base.name(),
                base.dimension(),
                base.x(),
                base.y(),
                base.z(),
                base.channel(),
                base.enabled(),
                base.pulseTicks(),
                base.remainingPulseTicks(),
                base.cooldownTicks(),
                base.actionCount(),
                base.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                base.blockId(),
                base.offChannel(),
                base.mode(),
                base.lastPowered(),
                base.lastPowerLevel()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_CHANGED, updated);
    }

    public static synchronized void recordReceive(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity, ActionExecutionResult result) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        SignalDeviceData base = fromReceiver(world, pos, blockEntity, existing, false);
        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        SignalDeviceData updated = new SignalDeviceData(
                base.id(),
                base.type(),
                base.name(),
                base.dimension(),
                base.x(),
                base.y(),
                base.z(),
                base.channel(),
                base.enabled(),
                base.pulseTicks(),
                base.remainingPulseTicks(),
                base.cooldownTicks(),
                base.actionCount(),
                base.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                base.blockId(),
                base.offChannel(),
                base.mode(),
                base.lastPowered(),
                base.lastPowerLevel()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.RECEIVER_PULSE_CHANGED, updated);
    }

    public static synchronized void recordActionRelayRun(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity, ActionExecutionResult result) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, ActionRelayBlockEntity.sourceId(world, pos));
        SignalDeviceData base = fromActionRelay(world, pos, blockEntity, existing, false);
        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        SignalDeviceData updated = new SignalDeviceData(
                base.id(),
                base.type(),
                base.name(),
                base.dimension(),
                base.x(),
                base.y(),
                base.z(),
                base.channel(),
                base.enabled(),
                base.pulseTicks(),
                base.remainingPulseTicks(),
                base.cooldownTicks(),
                base.actionCount(),
                base.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                base.blockId(),
                base.offChannel(),
                base.mode(),
                base.lastPowered(),
                base.lastPowerLevel()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualPowerState(ServerWorld world, SignalDeviceData device, VirtualBlockPowerState powerState) {
        if (world == null || device == null || powerState == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        SignalDeviceData updated = withVirtualPower(world, existing, powerState, existing.lastResult(), false);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualBlockTrigger(
            ServerWorld world,
            SignalDeviceData device,
            VirtualBlockPowerState powerState,
            ActionExecutionResult result
    ) {
        if (world == null || device == null || powerState == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        SignalDeviceData updated = withVirtualPower(world, existing, powerState, resultMessage, true);
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualBlockManualTrigger(
            ServerWorld world,
            SignalDeviceData device,
            ActionExecutionResult result
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        SignalDeviceData updated = new SignalDeviceData(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.dimension(),
                existing.x(),
                existing.y(),
                existing.z(),
                existing.channel(),
                existing.enabled(),
                existing.pulseTicks(),
                existing.remainingPulseTicks(),
                existing.cooldownTicks(),
                existing.actionCount(),
                existing.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                existing.blockId(),
                existing.offChannel(),
                existing.mode(),
                existing.lastPowered(),
                existing.lastPowerLevel(),
                existing.conditionEnabled(),
                existing.conditionBlockId(),
                existing.conditionProperties(),
                existing.conditionRaw(),
                existing.conditionMode(),
                existing.lastConditionMatched(),
                existing.lastConditionCheckGameTime(),
                existing.lastConditionResult(),
                existing.interactionEnabled(),
                existing.interactChannel(),
                existing.interactionCooldownTicks(),
                existing.lastInteractionGameTime(),
                existing.lastInteractionWallTimeMillis(),
                existing.lastInteractionPlayerName(),
                existing.lastInteractionPlayerUuid(),
                existing.lastInteractionResult(),
                existing.lastInteractionHand(),
                existing.lastInteractionSide(),
                existing.containerEnabled(),
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                existing.containerCooldownTicks(),
                existing.containerChangeCheckIntervalTicks(),
                existing.lastContainerCheckGameTime(),
                existing.lastContainerFingerprint(),
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                existing.lastContainerResult(),
                existing.lastContainerEventType(),
                existing.itemConditions(),
                existing.interactionItemMatcherEnabled(),
                existing.interactionItemMatcher(),
                existing.lastInteractionItemMatched(),
                existing.lastInteractionItemResult(),
                existing.itemSubmitEnabled(),
                existing.itemSubmitConsumeEnabled(),
                existing.itemSubmitConsumeOrder(),
                existing.itemSubmitRequirements(),
                existing.lastItemSubmitMatched(),
                existing.lastItemSubmitFailureReason(),
                existing.lastItemSubmitConsumedSummary(),
                existing.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualConditionState(
            ServerWorld world,
            SignalDeviceData device,
            boolean currentMatched,
            String resultMessage
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        SignalDeviceData updated = withCondition(
                existing,
                existing.conditionEnabled(),
                existing.conditionBlockId(),
                existing.conditionProperties(),
                existing.conditionRaw(),
                existing.conditionMode(),
                currentMatched,
                world.getTime(),
                resultMessage
        );
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualConditionTrigger(
            ServerWorld world,
            SignalDeviceData device,
            boolean currentMatched,
            ActionExecutionResult result
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        SignalDeviceData conditioned = withCondition(
                existing,
                existing.conditionEnabled(),
                existing.conditionBlockId(),
                existing.conditionProperties(),
                existing.conditionRaw(),
                existing.conditionMode(),
                currentMatched,
                world.getTime(),
                resultMessage
        );
        long now = System.currentTimeMillis();
        SignalDeviceData updated = new SignalDeviceData(
                conditioned.id(),
                conditioned.type(),
                conditioned.name(),
                conditioned.dimension(),
                conditioned.x(),
                conditioned.y(),
                conditioned.z(),
                conditioned.channel(),
                conditioned.enabled(),
                conditioned.pulseTicks(),
                conditioned.remainingPulseTicks(),
                conditioned.cooldownTicks(),
                conditioned.actionCount(),
                conditioned.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                conditioned.blockId(),
                conditioned.offChannel(),
                conditioned.mode(),
                conditioned.lastPowered(),
                conditioned.lastPowerLevel(),
                conditioned.conditionEnabled(),
                conditioned.conditionBlockId(),
                conditioned.conditionProperties(),
                conditioned.conditionRaw(),
                conditioned.conditionMode(),
                conditioned.lastConditionMatched(),
                conditioned.lastConditionCheckGameTime(),
                conditioned.lastConditionResult(),
                conditioned.interactionEnabled(),
                conditioned.interactChannel(),
                conditioned.interactionCooldownTicks(),
                conditioned.lastInteractionGameTime(),
                conditioned.lastInteractionWallTimeMillis(),
                conditioned.lastInteractionPlayerName(),
                conditioned.lastInteractionPlayerUuid(),
                conditioned.lastInteractionResult(),
                conditioned.lastInteractionHand(),
                conditioned.lastInteractionSide(),
                conditioned.containerEnabled(),
                conditioned.containerOpenChannel(),
                conditioned.containerCloseChannel(),
                conditioned.containerChangeChannel(),
                conditioned.containerCooldownTicks(),
                conditioned.containerChangeCheckIntervalTicks(),
                conditioned.lastContainerCheckGameTime(),
                conditioned.lastContainerFingerprint(),
                conditioned.lastContainerOpenGameTime(),
                conditioned.lastContainerOpenWallTimeMillis(),
                conditioned.lastContainerCloseGameTime(),
                conditioned.lastContainerCloseWallTimeMillis(),
                conditioned.lastContainerChangeGameTime(),
                conditioned.lastContainerChangeWallTimeMillis(),
                conditioned.lastContainerPlayerName(),
                conditioned.lastContainerPlayerUuid(),
                conditioned.lastContainerResult(),
                conditioned.lastContainerEventType(),
                conditioned.itemConditions(),
                conditioned.interactionItemMatcherEnabled(),
                conditioned.interactionItemMatcher(),
                conditioned.lastInteractionItemMatched(),
                conditioned.lastInteractionItemResult(),
                conditioned.itemSubmitEnabled(),
                conditioned.itemSubmitConsumeEnabled(),
                conditioned.itemSubmitConsumeOrder(),
                conditioned.itemSubmitRequirements(),
                conditioned.lastItemSubmitMatched(),
                conditioned.lastItemSubmitFailureReason(),
                conditioned.lastItemSubmitConsumedSummary(),
                conditioned.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static synchronized void recordVirtualInteractionTrigger(
            ServerWorld world,
            SignalDeviceData device,
            ServerPlayerEntity player,
            String handName,
            String sideName,
            ActionExecutionResult result
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        SignalDeviceData interacted = withInteraction(
                existing,
                existing.interactionEnabled(),
                existing.interactChannel(),
                existing.interactionCooldownTicks(),
                world.getTime(),
                now,
                player == null ? "" : player.getName().getString(),
                player == null ? "" : player.getUuidAsString(),
                resultMessage,
                handName,
                sideName
        );
        SignalDeviceData updated = new SignalDeviceData(
                interacted.id(),
                interacted.type(),
                interacted.name(),
                interacted.dimension(),
                interacted.x(),
                interacted.y(),
                interacted.z(),
                interacted.channel(),
                interacted.enabled(),
                interacted.pulseTicks(),
                interacted.remainingPulseTicks(),
                interacted.cooldownTicks(),
                interacted.actionCount(),
                interacted.createdWallTimeMillis(),
                now,
                world.getTime(),
                now,
                resultMessage,
                interacted.blockId(),
                interacted.offChannel(),
                interacted.mode(),
                interacted.lastPowered(),
                interacted.lastPowerLevel(),
                interacted.conditionEnabled(),
                interacted.conditionBlockId(),
                interacted.conditionProperties(),
                interacted.conditionRaw(),
                interacted.conditionMode(),
                interacted.lastConditionMatched(),
                interacted.lastConditionCheckGameTime(),
                interacted.lastConditionResult(),
                interacted.interactionEnabled(),
                interacted.interactChannel(),
                interacted.interactionCooldownTicks(),
                interacted.lastInteractionGameTime(),
                interacted.lastInteractionWallTimeMillis(),
                interacted.lastInteractionPlayerName(),
                interacted.lastInteractionPlayerUuid(),
                interacted.lastInteractionResult(),
                interacted.lastInteractionHand(),
                interacted.lastInteractionSide(),
                interacted.containerEnabled(),
                interacted.containerOpenChannel(),
                interacted.containerCloseChannel(),
                interacted.containerChangeChannel(),
                interacted.containerCooldownTicks(),
                interacted.containerChangeCheckIntervalTicks(),
                interacted.lastContainerCheckGameTime(),
                interacted.lastContainerFingerprint(),
                interacted.lastContainerOpenGameTime(),
                interacted.lastContainerOpenWallTimeMillis(),
                interacted.lastContainerCloseGameTime(),
                interacted.lastContainerCloseWallTimeMillis(),
                interacted.lastContainerChangeGameTime(),
                interacted.lastContainerChangeWallTimeMillis(),
                interacted.lastContainerPlayerName(),
                interacted.lastContainerPlayerUuid(),
                interacted.lastContainerResult(),
                interacted.lastContainerEventType(),
                interacted.itemConditions(),
                interacted.interactionItemMatcherEnabled(),
                interacted.interactionItemMatcher(),
                interacted.interactionItemMatcherEnabled(),
                interacted.interactionItemMatcherEnabled() ? "主手物品匹配通过" : interacted.lastInteractionItemResult(),
                interacted.itemSubmitEnabled(),
                interacted.itemSubmitConsumeEnabled(),
                interacted.itemSubmitConsumeOrder(),
                interacted.itemSubmitRequirements(),
                interacted.lastItemSubmitMatched(),
                interacted.lastItemSubmitFailureReason(),
                interacted.lastItemSubmitConsumedSummary(),
                interacted.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
        publishDeviceChange(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED, updated);
    }

    public static long getRemainingInteractionCooldownTicks(SignalDeviceData device, long currentGameTime) {
        if (device == null || device.interactionCooldownTicks() <= 0 || device.lastInteractionGameTime() <= 0L) {
            return 0L;
        }
        long elapsed = Math.max(0L, currentGameTime - device.lastInteractionGameTime());
        return Math.max(0L, device.interactionCooldownTicks() - elapsed);
    }

    public static synchronized void recordVirtualInteractionItemResult(
            ServerWorld world,
            SignalDeviceData device,
            ServerPlayerEntity player,
            String handName,
            String sideName,
            boolean matched,
            String feedbackResult,
            int consumedCount,
            String itemSource,
            int matchedSlot,
            int matchedCount,
            String consumeSource,
            String consumedSlots,
            String consumeResult,
            ActionExecutionResult signalResult
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String signalMessage = signalResult == null || signalResult.message() == null ? "" : signalResult.message().getString();
        String resultMessage = feedbackResult == null ? "" : feedbackResult.trim();
        if (!signalMessage.isBlank()) {
            resultMessage = resultMessage.isBlank() ? signalMessage : resultMessage + "; " + signalMessage;
        }
        if (consumedCount > 0) {
            resultMessage = resultMessage.isBlank()
                    ? "consumed " + consumedCount + " item(s)"
                    : resultMessage + "; consumed " + consumedCount + " item(s)";
        }
        String sourceResult = resultMessage;
        String cleanItemSource = itemSource == null ? "" : itemSource.trim();
        if (!cleanItemSource.isBlank()) {
            String sourceDetail = "source=" + cleanItemSource + " slot=" + matchedSlot + " count=" + matchedCount;
            sourceResult = sourceResult.isBlank() ? sourceDetail : sourceResult + "; " + sourceDetail;
        }
        String cleanConsumeSource = consumeSource == null ? "" : consumeSource.trim();
        String cleanConsumedSlots = consumedSlots == null ? "" : consumedSlots.trim();
        String cleanConsumeResult = consumeResult == null ? "" : consumeResult.trim();
        long now = System.currentTimeMillis();
        SignalDeviceData interacted = withInteraction(
                existing,
                existing.interactionEnabled(),
                existing.interactChannel(),
                existing.interactionCooldownTicks(),
                world.getTime(),
                now,
                player == null ? "" : player.getName().getString(),
                player == null ? "" : player.getUuidAsString(),
                resultMessage,
                handName,
                sideName
        );
        SignalDeviceData updated = new SignalDeviceData(
                interacted.id(),
                interacted.type(),
                interacted.name(),
                interacted.dimension(),
                interacted.x(),
                interacted.y(),
                interacted.z(),
                interacted.channel(),
                interacted.enabled(),
                interacted.pulseTicks(),
                interacted.remainingPulseTicks(),
                interacted.cooldownTicks(),
                interacted.actionCount(),
                interacted.createdWallTimeMillis(),
                now,
                signalResult == null ? interacted.lastTriggerGameTime() : world.getTime(),
                signalResult == null ? interacted.lastTriggerWallTimeMillis() : now,
                signalResult == null ? interacted.lastResult() : resultMessage,
                interacted.blockId(),
                interacted.offChannel(),
                interacted.mode(),
                interacted.lastPowered(),
                interacted.lastPowerLevel(),
                interacted.conditionEnabled(),
                interacted.conditionBlockId(),
                interacted.conditionProperties(),
                interacted.conditionRaw(),
                interacted.conditionMode(),
                interacted.lastConditionMatched(),
                interacted.lastConditionCheckGameTime(),
                interacted.lastConditionResult(),
                interacted.interactionEnabled(),
                interacted.interactChannel(),
                interacted.interactionCooldownTicks(),
                interacted.lastInteractionGameTime(),
                interacted.lastInteractionWallTimeMillis(),
                interacted.lastInteractionPlayerName(),
                interacted.lastInteractionPlayerUuid(),
                interacted.lastInteractionResult(),
                interacted.lastInteractionHand(),
                interacted.lastInteractionSide(),
                interacted.containerEnabled(),
                interacted.containerOpenChannel(),
                interacted.containerCloseChannel(),
                interacted.containerChangeChannel(),
                interacted.containerCooldownTicks(),
                interacted.containerChangeCheckIntervalTicks(),
                interacted.lastContainerCheckGameTime(),
                interacted.lastContainerFingerprint(),
                interacted.lastContainerOpenGameTime(),
                interacted.lastContainerOpenWallTimeMillis(),
                interacted.lastContainerCloseGameTime(),
                interacted.lastContainerCloseWallTimeMillis(),
                interacted.lastContainerChangeGameTime(),
                interacted.lastContainerChangeWallTimeMillis(),
                interacted.lastContainerPlayerName(),
                interacted.lastContainerPlayerUuid(),
                interacted.lastContainerResult(),
                interacted.lastContainerEventType(),
                interacted.itemConditions(),
                interacted.interactionItemMatcherEnabled(),
                ItemStackMatcherSupport.withConsumeResult(
                        ItemStackMatcherSupport.withSourceResult(
                                interacted.interactionItemMatcher(),
                                cleanItemSource,
                                matchedSlot,
                                matchedCount,
                                sourceResult
                        ),
                        cleanConsumeSource,
                        cleanConsumedSlots,
                        cleanConsumeResult
                ),
                matched,
                resultMessage,
                interacted.itemSubmitEnabled(),
                interacted.itemSubmitConsumeEnabled(),
                interacted.itemSubmitConsumeOrder(),
                interacted.itemSubmitRequirements(),
                interacted.lastItemSubmitMatched(),
                interacted.lastItemSubmitFailureReason(),
                interacted.lastItemSubmitConsumedSummary(),
                interacted.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
    }

    public static synchronized void recordVirtualContainerEvent(
            ServerWorld world,
            SignalDeviceData device,
            String eventType,
            ServerPlayerEntity player,
            ActionExecutionResult result,
            String fingerprint
    ) {
        if (world == null || device == null || eventType == null || eventType.isBlank()) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        String resultMessage = result == null || result.message() == null ? "" : result.message().getString();
        long now = System.currentTimeMillis();
        long gameTime = world.getTime();
        boolean open = "open".equals(eventType);
        boolean close = "close".equals(eventType);
        boolean change = "change".equals(eventType);
        SignalDeviceData updated = withContainer(
                existing,
                existing.containerEnabled(),
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                existing.containerCooldownTicks(),
                existing.containerChangeCheckIntervalTicks(),
                change ? gameTime : existing.lastContainerCheckGameTime(),
                change && fingerprint != null ? fingerprint : existing.lastContainerFingerprint(),
                open ? gameTime : existing.lastContainerOpenGameTime(),
                open ? now : existing.lastContainerOpenWallTimeMillis(),
                close ? gameTime : existing.lastContainerCloseGameTime(),
                close ? now : existing.lastContainerCloseWallTimeMillis(),
                change ? gameTime : existing.lastContainerChangeGameTime(),
                change ? now : existing.lastContainerChangeWallTimeMillis(),
                player == null ? "" : player.getName().getString(),
                player == null ? "" : player.getUuidAsString(),
                resultMessage,
                eventType
        );
        SignalDeviceData triggered = new SignalDeviceData(
                updated.id(),
                updated.type(),
                updated.name(),
                updated.dimension(),
                updated.x(),
                updated.y(),
                updated.z(),
                updated.channel(),
                updated.enabled(),
                updated.pulseTicks(),
                updated.remainingPulseTicks(),
                updated.cooldownTicks(),
                updated.actionCount(),
                updated.createdWallTimeMillis(),
                now,
                gameTime,
                now,
                resultMessage,
                updated.blockId(),
                updated.offChannel(),
                updated.mode(),
                updated.lastPowered(),
                updated.lastPowerLevel(),
                updated.conditionEnabled(),
                updated.conditionBlockId(),
                updated.conditionProperties(),
                updated.conditionRaw(),
                updated.conditionMode(),
                updated.lastConditionMatched(),
                updated.lastConditionCheckGameTime(),
                updated.lastConditionResult(),
                updated.interactionEnabled(),
                updated.interactChannel(),
                updated.interactionCooldownTicks(),
                updated.lastInteractionGameTime(),
                updated.lastInteractionWallTimeMillis(),
                updated.lastInteractionPlayerName(),
                updated.lastInteractionPlayerUuid(),
                updated.lastInteractionResult(),
                updated.lastInteractionHand(),
                updated.lastInteractionSide(),
                updated.containerEnabled(),
                updated.containerOpenChannel(),
                updated.containerCloseChannel(),
                updated.containerChangeChannel(),
                updated.containerCooldownTicks(),
                updated.containerChangeCheckIntervalTicks(),
                updated.lastContainerCheckGameTime(),
                updated.lastContainerFingerprint(),
                updated.lastContainerOpenGameTime(),
                updated.lastContainerOpenWallTimeMillis(),
                updated.lastContainerCloseGameTime(),
                updated.lastContainerCloseWallTimeMillis(),
                updated.lastContainerChangeGameTime(),
                updated.lastContainerChangeWallTimeMillis(),
                updated.lastContainerPlayerName(),
                updated.lastContainerPlayerUuid(),
                updated.lastContainerResult(),
                updated.lastContainerEventType(),
                updated.itemConditions(),
                updated.interactionItemMatcherEnabled(),
                updated.interactionItemMatcher(),
                updated.lastInteractionItemMatched(),
                updated.lastInteractionItemResult(),
                updated.itemSubmitEnabled(),
                updated.itemSubmitConsumeEnabled(),
                updated.itemSubmitConsumeOrder(),
                updated.itemSubmitRequirements(),
                updated.lastItemSubmitMatched(),
                updated.lastItemSubmitFailureReason(),
                updated.lastItemSubmitConsumedSummary(),
                updated.lastItemSubmitResult()
        ).normalized();
        replaceOrAdd(state, triggered);
        state.markDirty();
    }

    public static synchronized void recordVirtualContainerFingerprintState(
            ServerWorld world,
            SignalDeviceData device,
            String fingerprint,
            String resultMessage
    ) {
        if (world == null || device == null) {
            return;
        }

        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, device.id());
        if (existing == null) {
            return;
        }

        SignalDeviceData updated = withContainer(
                existing,
                existing.containerEnabled(),
                existing.containerOpenChannel(),
                existing.containerCloseChannel(),
                existing.containerChangeChannel(),
                existing.containerCooldownTicks(),
                existing.containerChangeCheckIntervalTicks(),
                world.getTime(),
                fingerprint == null ? "" : fingerprint,
                existing.lastContainerOpenGameTime(),
                existing.lastContainerOpenWallTimeMillis(),
                existing.lastContainerCloseGameTime(),
                existing.lastContainerCloseWallTimeMillis(),
                existing.lastContainerChangeGameTime(),
                existing.lastContainerChangeWallTimeMillis(),
                existing.lastContainerPlayerName(),
                existing.lastContainerPlayerUuid(),
                resultMessage,
                existing.lastContainerEventType()
        );
        replaceOrAdd(state, updated);
        state.markDirty();
    }

    public static long getRemainingContainerCooldownTicks(SignalDeviceData device, long currentGameTime) {
        if (device == null || device.containerCooldownTicks() <= 0) {
            return 0L;
        }
        long lastEvent = Math.max(device.lastContainerOpenGameTime(),
                Math.max(device.lastContainerCloseGameTime(), device.lastContainerChangeGameTime()));
        if (lastEvent <= 0L) {
            return 0L;
        }
        long elapsed = Math.max(0L, currentGameTime - lastEvent);
        return Math.max(0L, device.containerCooldownTicks() - elapsed);
    }

    public static synchronized List<SignalDeviceData> getEnabledReceiversForChannel(MinecraftServer server, String channel) {
        String normalizedChannel = com.zcpu.tzzmod.signal.SignalChannel.normalize(channel);
        List<SignalDeviceData> result = new ArrayList<>();
        for (SignalDeviceData device : getSnapshot(server)) {
            if (device.type().equals(SignalDeviceData.TYPE_SIGNAL_RECEIVER)
                    && device.enabled()
                    && device.channel().equals(normalizedChannel)) {
                result.add(device);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized List<SignalDeviceData> getEnabledActionRelaysForChannel(MinecraftServer server, String channel) {
        String normalizedChannel = com.zcpu.tzzmod.signal.SignalChannel.normalize(channel);
        List<SignalDeviceData> result = new ArrayList<>();
        for (SignalDeviceData device : getSnapshot(server)) {
            if (device.type().equals(SignalDeviceData.TYPE_ACTION_RELAY)
                    && device.enabled()
                    && device.channel().equals(normalizedChannel)) {
                result.add(device);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized ResolveResult resolveDevice(MinecraftServer server, String deviceRef) {
        if (deviceRef == null || deviceRef.isBlank()) {
            return ResolveResult.none();
        }

        State state = getState(server);
        refreshLoadedDevices(server, state);
        String query = cleanUserText(deviceRef);
        if (hasDeviceTypePrefix(query)) {
            List<SignalDeviceData> typedPositionMatches = findBySourcePosition(state, query);
            if (!typedPositionMatches.isEmpty()) {
                return typedPositionMatches.size() == 1
                        ? ResolveResult.unique(typedPositionMatches.getFirst())
                        : ResolveResult.ambiguous(typedPositionMatches);
            }
            SourcePositionRef typedRef = parseSourcePositionRef(query);
            String untypedQuery = stripDeviceTypePrefix(query);
            if (typedRef != null && !untypedQuery.equals(query)) {
                SignalDeviceData exact = findById(state, untypedQuery);
                if (exact != null && exact.normalized().type().equals(typedRef.expectedType())) {
                    return ResolveResult.unique(exact.normalized());
                }
            }
        }
        for (SignalDeviceData device : state.devices) {
            if (device.id().equals(query)) {
                return ResolveResult.unique(device);
            }
        }

        List<SignalDeviceData> positionMatches = findBySourcePosition(state, query);
        if (!positionMatches.isEmpty()) {
            return positionMatches.size() == 1
                    ? ResolveResult.unique(positionMatches.getFirst())
                    : ResolveResult.ambiguous(positionMatches);
        }

        String shortQuery = query.endsWith("...") ? query.substring(0, query.length() - 3) : query;
        List<SignalDeviceData> matches = new ArrayList<>();
        for (SignalDeviceData device : state.devices) {
            if (cleanUserText(device.name()).equals(query)
                    || shortId(device.id()).equals(query)
                    || tailId(device.id()).equals(query)
                    || (shortQuery.length() >= 8 && device.id().startsWith(shortQuery))
                    || (shortQuery.length() >= 4 && tailId(device.id()).startsWith(shortQuery))) {
                matches.add(device);
            }
        }

        if (matches.isEmpty()) {
            return ResolveResult.none();
        }
        if (matches.size() == 1) {
            return ResolveResult.unique(matches.getFirst());
        }
        return ResolveResult.ambiguous(List.copyOf(matches));
    }

    public static synchronized SignalDeviceData refreshLoadedState(MinecraftServer server, SignalDeviceData device) {
        if (device == null) {
            return null;
        }

        State state = getState(server);
        SignalDeviceData refreshed = refreshLoadedDevice(server, state, device);
        return refreshed == null ? device : refreshed;
    }

    public static SignalEmitterBlockEntity getLoadedEmitter(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return null;
        }

        ServerWorld world = findWorld(server, device.dimension());
        if (world == null) {
            return null;
        }

        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return null;
        }

        return world.getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity ? blockEntity : null;
    }

    public static SignalReceiverBlockEntity getLoadedReceiver(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return null;
        }

        ServerWorld world = findWorld(server, device.dimension());
        if (world == null) {
            return null;
        }

        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return null;
        }

        return world.getBlockEntity(pos) instanceof SignalReceiverBlockEntity blockEntity ? blockEntity : null;
    }

    public static ActionRelayBlockEntity getLoadedActionRelay(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return null;
        }

        ServerWorld world = findWorld(server, device.dimension());
        if (world == null) {
            return null;
        }

        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return null;
        }

        return world.getBlockEntity(pos) instanceof ActionRelayBlockEntity blockEntity ? blockEntity : null;
    }

    public static ServerWorld getDeviceWorld(MinecraftServer server, SignalDeviceData device) {
        return device == null ? null : findWorld(server, device.dimension());
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        State state = CACHE.get(server);
        if (state != null) {
            state.flushDirty(false, currentGameTime(server));
        }
    }

    public static synchronized void forceFlushDirty(MinecraftServer server) {
        State state = CACHE.get(server);
        if (state != null) {
            state.flushDirty(true, currentGameTime(server));
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static String sourceId(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String shortId(String id) {
        String tail = tailId(id);
        if (!tail.isBlank() && tail.length() <= 24) {
            return tail;
        }
        String value = id == null ? "" : id.trim();
        if (value.isBlank()) {
            return "未知";
        }
        return value.length() <= 12 ? value : value.substring(0, 12) + "...";
    }

    public static String displayName(SignalDeviceData device) {
        if (device == null) {
            return "未知设备";
        }
        return device.name() == null || device.name().isBlank() ? "未命名信号设备" : device.name();
    }

    public static String positionText(SignalDeviceData device) {
        if (device == null) {
            return "未知位置";
        }
        return device.dimension() + " " + device.x() + " " + device.y() + " " + device.z();
    }

    public static String cleanUserText(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static void refreshLoadedDevices(MinecraftServer server, State state) {
        for (int index = 0; index < state.devices.size(); index++) {
            SignalDeviceData refreshed = refreshLoadedDevice(server, state, state.devices.get(index));
            if (refreshed != null) {
                state.devices.set(index, refreshed);
            }
        }
    }

    private static SignalDeviceData refreshLoadedDevice(MinecraftServer server, State state, SignalDeviceData device) {
        ServerWorld world = findWorld(server, device.dimension());
        if (world == null) {
            return device;
        }

        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        SignalDeviceData refreshed = null;
        if (SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            return device;
        }
        if (device.type().equals(SignalDeviceData.TYPE_ACTION_RELAY)) {
            ActionRelayBlockEntity relay = getLoadedActionRelay(server, device);
            if (relay != null) {
                refreshed = fromActionRelay(world, pos, relay, device, true);
            }
        } else if (device.type().equals(SignalDeviceData.TYPE_SIGNAL_RECEIVER)) {
            SignalReceiverBlockEntity receiver = getLoadedReceiver(server, device);
            if (receiver != null) {
                refreshed = fromReceiver(world, pos, receiver, device, true);
            }
        } else if (device.type().equals(SignalDeviceData.TYPE_SIGNAL_EMITTER)) {
            SignalEmitterBlockEntity emitter = getLoadedEmitter(server, device);
            if (emitter != null) {
                refreshed = fromEmitter(world, pos, emitter, device, true);
            }
        }
        if (refreshed == null) {
            return device;
        }
        if (!refreshed.equals(device)) {
            replaceOrAdd(state, refreshed);
            state.markDirty();
        }
        return refreshed;
    }

    private static boolean matchesLoadedDevice(ServerWorld world, BlockPos pos, String type) {
        if (SignalDeviceData.TYPE_ACTION_RELAY.equals(type)) {
            return world.getBlockEntity(pos) instanceof ActionRelayBlockEntity;
        }
        if (SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(type)) {
            return world.getBlockEntity(pos) instanceof SignalReceiverBlockEntity;
        }
        return SignalDeviceData.TYPE_SIGNAL_EMITTER.equals(type)
                && world.getBlockEntity(pos) instanceof SignalEmitterBlockEntity;
    }

    private static SignalDeviceData existingOfType(SignalDeviceData existing, String expectedType) {
        SignalDeviceData normalized = existing == null ? null : existing.normalized();
        return normalized != null && normalized.type().equals(expectedType) ? normalized : null;
    }

    private static void cleanupIfTypeChanged(MinecraftServer server, SignalDeviceData existing, String expectedType) {
        SignalDeviceData normalized = existing == null ? null : existing.normalized();
        if (normalized == null || normalized.type().equals(expectedType)) {
            return;
        }
        cleanupWebAdminMetadata(server, normalized.id(), normalized.type());
        WebAdminRealtimeEventBus.publishDeviceRemoved(normalized.id(), normalized.type());
    }

    private static void cleanupWebAdminMetadata(MinecraftServer server, String deviceId, String deviceType) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return;
        }
        WebAdminDeviceMetadataStore.removeDeviceAliases(server, deviceId, deviceType);
    }

    private static SignalDeviceData fromEmitter(
            ServerWorld world,
            BlockPos pos,
            SignalEmitterBlockEntity blockEntity,
            SignalDeviceData existing,
            boolean preserveUpdatedTime
    ) {
        existing = existingOfType(existing, SignalDeviceData.TYPE_SIGNAL_EMITTER);
        long now = System.currentTimeMillis();
        long created = existing == null || existing.createdWallTimeMillis() <= 0 ? now : existing.createdWallTimeMillis();
        long updated = preserveUpdatedTime && existing != null ? existing.updatedWallTimeMillis() : now;
        return new SignalDeviceData(
                SignalEmitterBlockEntity.sourceId(world, pos),
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                existing == null ? "" : existing.name(),
                world.getRegistryKey().getValue().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                blockEntity.channel(),
                blockEntity.enabled(),
                0,
                0,
                0,
                0,
                created,
                updated,
                existing == null ? 0L : existing.lastTriggerGameTime(),
                existing == null ? 0L : existing.lastTriggerWallTimeMillis(),
                existing == null ? "" : existing.lastResult(),
                existing == null ? "" : existing.blockId(),
                existing == null ? "" : existing.offChannel(),
                existing == null ? VirtualBlockDeviceMode.REDSTONE_RISING.id() : existing.mode(),
                existing != null && existing.lastPowered(),
                existing == null ? 0 : existing.lastPowerLevel()
        ).normalized();
    }

    private static SignalDeviceData fromReceiver(
            ServerWorld world,
            BlockPos pos,
            SignalReceiverBlockEntity blockEntity,
            SignalDeviceData existing,
            boolean preserveUpdatedTime
    ) {
        existing = existingOfType(existing, SignalDeviceData.TYPE_SIGNAL_RECEIVER);
        long now = System.currentTimeMillis();
        long created = existing == null || existing.createdWallTimeMillis() <= 0 ? now : existing.createdWallTimeMillis();
        long updated = preserveUpdatedTime && existing != null ? existing.updatedWallTimeMillis() : now;
        return new SignalDeviceData(
                SignalReceiverBlockEntity.sourceId(world, pos),
                SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                existing == null ? "" : existing.name(),
                world.getRegistryKey().getValue().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                blockEntity.channel(),
                blockEntity.enabled(),
                blockEntity.pulseTicks(),
                blockEntity.remainingPulseTicks(),
                0,
                0,
                created,
                updated,
                existing == null ? 0L : existing.lastTriggerGameTime(),
                existing == null ? 0L : existing.lastTriggerWallTimeMillis(),
                existing == null ? "" : existing.lastResult(),
                existing == null ? "" : existing.blockId(),
                existing == null ? "" : existing.offChannel(),
                existing == null ? VirtualBlockDeviceMode.REDSTONE_RISING.id() : existing.mode(),
                existing != null && existing.lastPowered(),
                existing == null ? 0 : existing.lastPowerLevel()
        ).normalized();
    }

    private static SignalDeviceData fromActionRelay(
            ServerWorld world,
            BlockPos pos,
            ActionRelayBlockEntity blockEntity,
            SignalDeviceData existing,
            boolean preserveUpdatedTime
    ) {
        existing = existingOfType(existing, SignalDeviceData.TYPE_ACTION_RELAY);
        long now = System.currentTimeMillis();
        long created = existing == null || existing.createdWallTimeMillis() <= 0 ? now : existing.createdWallTimeMillis();
        long updated = preserveUpdatedTime && existing != null ? existing.updatedWallTimeMillis() : now;
        return new SignalDeviceData(
                ActionRelayBlockEntity.sourceId(world, pos),
                SignalDeviceData.TYPE_ACTION_RELAY,
                existing == null ? "" : existing.name(),
                world.getRegistryKey().getValue().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                blockEntity.channel(),
                blockEntity.enabled(),
                0,
                0,
                blockEntity.cooldownTicks(),
                blockEntity.actions().size(),
                created,
                updated,
                existing == null ? 0L : existing.lastTriggerGameTime(),
                existing == null ? 0L : existing.lastTriggerWallTimeMillis(),
                existing == null ? "" : existing.lastResult(),
                existing == null ? "" : existing.blockId(),
                existing == null ? "" : existing.offChannel(),
                existing == null ? VirtualBlockDeviceMode.REDSTONE_RISING.id() : existing.mode(),
                existing != null && existing.lastPowered(),
                existing == null ? 0 : existing.lastPowerLevel()
        ).normalized();
    }

    private static SignalDeviceData fromVirtualBlock(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData existing,
            String channel,
            String offChannel,
            String mode,
            boolean enabled,
            VirtualBlockPowerState powerState,
            boolean preserveUpdatedTime,
            String name
    ) {
        long now = System.currentTimeMillis();
        long created = existing == null || existing.createdWallTimeMillis() <= 0 ? now : existing.createdWallTimeMillis();
        long updated = preserveUpdatedTime && existing != null ? existing.updatedWallTimeMillis() : now;
        return new SignalDeviceData(
                VirtualBlockDeviceSupport.id(world, pos),
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE,
                name == null ? "" : name,
                world.getRegistryKey().getValue().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                channel,
                enabled,
                0,
                0,
                0,
                0,
                created,
                updated,
                existing == null ? 0L : existing.lastTriggerGameTime(),
                existing == null ? 0L : existing.lastTriggerWallTimeMillis(),
                existing == null ? "" : existing.lastResult(),
                powerState.blockId(),
                offChannel,
                mode,
                powerState.currentPowered(),
                powerState.receivedPowerLevel(),
                existing != null && existing.conditionEnabled(),
                existing == null ? "" : existing.conditionBlockId(),
                existing == null ? Map.of() : existing.conditionProperties(),
                existing == null ? "" : existing.conditionRaw(),
                existing == null ? BlockStateConditionMode.CONDITION_ENTER.id() : existing.conditionMode(),
                existing != null && existing.lastConditionMatched(),
                existing == null ? 0L : existing.lastConditionCheckGameTime(),
                existing == null ? "" : existing.lastConditionResult(),
                existing != null && existing.interactionEnabled(),
                existing == null ? "" : existing.interactChannel(),
                existing == null ? 0 : existing.interactionCooldownTicks(),
                existing == null ? 0L : existing.lastInteractionGameTime(),
                existing == null ? 0L : existing.lastInteractionWallTimeMillis(),
                existing == null ? "" : existing.lastInteractionPlayerName(),
                existing == null ? "" : existing.lastInteractionPlayerUuid(),
                existing == null ? "" : existing.lastInteractionResult(),
                existing == null ? "" : existing.lastInteractionHand(),
                existing == null ? "" : existing.lastInteractionSide(),
                existing != null && existing.containerEnabled(),
                existing == null ? "" : existing.containerOpenChannel(),
                existing == null ? "" : existing.containerCloseChannel(),
                existing == null ? "" : existing.containerChangeChannel(),
                existing == null ? 0 : existing.containerCooldownTicks(),
                existing == null ? 10 : existing.containerChangeCheckIntervalTicks(),
                existing == null ? 0L : existing.lastContainerCheckGameTime(),
                existing == null ? "" : existing.lastContainerFingerprint(),
                existing == null ? 0L : existing.lastContainerOpenGameTime(),
                existing == null ? 0L : existing.lastContainerOpenWallTimeMillis(),
                existing == null ? 0L : existing.lastContainerCloseGameTime(),
                existing == null ? 0L : existing.lastContainerCloseWallTimeMillis(),
                existing == null ? 0L : existing.lastContainerChangeGameTime(),
                existing == null ? 0L : existing.lastContainerChangeWallTimeMillis(),
                existing == null ? "" : existing.lastContainerPlayerName(),
                existing == null ? "" : existing.lastContainerPlayerUuid(),
                existing == null ? "" : existing.lastContainerResult(),
                existing == null ? "" : existing.lastContainerEventType(),
                existing == null ? List.of() : existing.itemConditions(),
                existing != null && existing.interactionItemMatcherEnabled(),
                existing == null ? ItemStackMatcherData.empty() : existing.interactionItemMatcher(),
                existing != null && existing.lastInteractionItemMatched(),
                existing == null ? "" : existing.lastInteractionItemResult(),
                existing != null && existing.itemSubmitEnabled(),
                existing != null && existing.itemSubmitConsumeEnabled(),
                existing == null ? com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder.HOTBAR_FIRST : existing.itemSubmitConsumeOrder(),
                existing == null ? List.of() : existing.itemSubmitRequirements(),
                existing != null && existing.lastItemSubmitMatched(),
                existing == null ? "" : existing.lastItemSubmitFailureReason(),
                existing == null ? "" : existing.lastItemSubmitConsumedSummary(),
                existing == null ? "" : existing.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withVirtualSettings(
            SignalDeviceData device,
            String channel,
            String offChannel,
            String mode,
            boolean enabled
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                channel,
                enabled,
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                offChannel,
                mode,
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withVirtualPower(
            ServerWorld world,
            SignalDeviceData device,
            VirtualBlockPowerState powerState,
            String resultMessage,
            boolean triggered
    ) {
        long now = System.currentTimeMillis();
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                now,
                triggered ? world.getTime() : device.lastTriggerGameTime(),
                triggered ? now : device.lastTriggerWallTimeMillis(),
                resultMessage,
                powerState.blockId(),
                device.offChannel(),
                device.mode(),
                powerState.currentPowered(),
                powerState.receivedPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withCondition(
            SignalDeviceData device,
            boolean conditionEnabled,
            String conditionBlockId,
            Map<String, String> conditionProperties,
            String conditionRaw,
            String conditionMode,
            boolean lastConditionMatched,
            long lastConditionCheckGameTime,
            String lastConditionResult
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                conditionEnabled,
                conditionBlockId,
                conditionProperties == null ? Map.of() : conditionProperties,
                conditionRaw,
                conditionMode,
                lastConditionMatched,
                lastConditionCheckGameTime,
                lastConditionResult,
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withInteraction(
            SignalDeviceData device,
            boolean interactionEnabled,
            String interactChannel,
            int interactionCooldownTicks,
            long lastInteractionGameTime,
            long lastInteractionWallTimeMillis,
            String lastInteractionPlayerName,
            String lastInteractionPlayerUuid,
            String lastInteractionResult,
            String lastInteractionHand,
            String lastInteractionSide
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                interactionEnabled,
                interactChannel,
                interactionCooldownTicks,
                lastInteractionGameTime,
                lastInteractionWallTimeMillis,
                lastInteractionPlayerName,
                lastInteractionPlayerUuid,
                lastInteractionResult,
                lastInteractionHand,
                lastInteractionSide,
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withContainer(
            SignalDeviceData device,
            boolean containerEnabled,
            String containerOpenChannel,
            String containerCloseChannel,
            String containerChangeChannel,
            int containerCooldownTicks,
            int containerChangeCheckIntervalTicks,
            long lastContainerCheckGameTime,
            String lastContainerFingerprint,
            long lastContainerOpenGameTime,
            long lastContainerOpenWallTimeMillis,
            long lastContainerCloseGameTime,
            long lastContainerCloseWallTimeMillis,
            long lastContainerChangeGameTime,
            long lastContainerChangeWallTimeMillis,
            String lastContainerPlayerName,
            String lastContainerPlayerUuid,
            String lastContainerResult,
            String lastContainerEventType
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                containerEnabled,
                containerOpenChannel,
                containerCloseChannel,
                containerChangeChannel,
                containerCooldownTicks,
                containerChangeCheckIntervalTicks,
                lastContainerCheckGameTime,
                lastContainerFingerprint,
                lastContainerOpenGameTime,
                lastContainerOpenWallTimeMillis,
                lastContainerCloseGameTime,
                lastContainerCloseWallTimeMillis,
                lastContainerChangeGameTime,
                lastContainerChangeWallTimeMillis,
                lastContainerPlayerName,
                lastContainerPlayerUuid,
                lastContainerResult,
                lastContainerEventType,
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withItemConditions(
            SignalDeviceData device,
            List<ContainerItemConditionData> itemConditions
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                itemConditions == null ? List.of() : itemConditions,
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withInteractionItemMatcher(
            SignalDeviceData device,
            boolean interactionItemMatcherEnabled,
            ItemStackMatcherData interactionItemMatcher,
            boolean lastInteractionItemMatched,
            String lastInteractionItemResult
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                interactionItemMatcherEnabled,
                interactionItemMatcher == null ? ItemStackMatcherData.empty() : interactionItemMatcher,
                lastInteractionItemMatched,
                lastInteractionItemResult == null ? "" : lastInteractionItemResult,
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withItemSubmit(
            SignalDeviceData device,
            boolean itemSubmitEnabled,
            boolean itemSubmitConsumeEnabled,
            String itemSubmitConsumeOrder,
            List<ItemSubmitRequirementData> itemSubmitRequirements,
            boolean lastItemSubmitMatched,
            String lastItemSubmitFailureReason,
            String lastItemSubmitConsumedSummary,
            String lastItemSubmitResult
    ) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                itemSubmitEnabled,
                itemSubmitConsumeEnabled,
                itemSubmitConsumeOrder,
                itemSubmitRequirements == null ? List.of() : itemSubmitRequirements,
                lastItemSubmitMatched,
                lastItemSubmitFailureReason == null ? "" : lastItemSubmitFailureReason,
                lastItemSubmitConsumedSummary == null ? "" : lastItemSubmitConsumedSummary,
                lastItemSubmitResult == null ? "" : lastItemSubmitResult
        ).normalized();
    }

    private static SignalDeviceData withName(SignalDeviceData device, String name) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                name,
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static SignalDeviceData withPulseCooldown(SignalDeviceData device, int pulseTicks, int cooldownTicks) {
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.channel(),
                device.enabled(),
                pulseTicks,
                device.remainingPulseTicks(),
                cooldownTicks,
                device.actionCount(),
                device.createdWallTimeMillis(),
                System.currentTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                device.offChannel(),
                device.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                device.conditionEnabled(),
                device.conditionBlockId(),
                device.conditionProperties(),
                device.conditionRaw(),
                device.conditionMode(),
                device.lastConditionMatched(),
                device.lastConditionCheckGameTime(),
                device.lastConditionResult(),
                device.interactionEnabled(),
                device.interactChannel(),
                device.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                device.containerEnabled(),
                device.containerOpenChannel(),
                device.containerCloseChannel(),
                device.containerChangeChannel(),
                device.containerCooldownTicks(),
                device.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                device.lastContainerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static void replaceOrAdd(State state, SignalDeviceData device) {
        SignalDeviceData normalized = device.normalized();
        for (int index = 0; index < state.devices.size(); index++) {
            if (state.devices.get(index).id().equals(normalized.id())) {
                state.devices.set(index, normalized);
                return;
            }
        }
        state.devices.add(normalized);
    }

    private static void publishDeviceChange(WebAdminRealtimeEventType type, SignalDeviceData device) {
        if (device == null) {
            return;
        }
        SignalDeviceData normalized = device.normalized();
        String summary = switch (type == null ? WebAdminRealtimeEventType.DEVICE_CHANGED : type) {
            case DEVICE_REGISTERED -> "设备已注册：" + displayName(normalized);
            case DEVICE_METADATA_CHANGED -> "设备显示信息已变化：" + displayName(normalized);
            case RECEIVER_CHANGED -> "接收器已变化：" + displayName(normalized);
            case RECEIVER_PULSE_CHANGED -> "接收器脉冲已变化：" + displayName(normalized);
            case VIRTUAL_BLOCK_DEVICE_CHANGED -> "虚拟方块设备已变化：" + displayName(normalized);
            default -> "设备已变化：" + displayName(normalized);
        };
        WebAdminRealtimeEventBus.publishDeviceEvent(type, normalized, summary);
    }

    private static SignalDeviceData findById(State state, String id) {
        for (SignalDeviceData device : state.devices) {
            if (device.id().equals(id)) {
                return device;
            }
        }
        return null;
    }

    private static List<SignalDeviceData> findBySourcePosition(State state, String deviceRef) {
        SourcePositionRef ref = parseSourcePositionRef(deviceRef);
        if (ref == null) {
            return List.of();
        }
        List<SignalDeviceData> matches = new ArrayList<>();
        for (SignalDeviceData device : state.devices) {
            SignalDeviceData normalized = device.normalized();
            if (!ref.expectedType().isBlank() && !normalized.type().equals(ref.expectedType())) {
                continue;
            }
            if (normalized.dimension().equals(ref.dimension())
                    && normalized.x() == ref.x()
                    && normalized.y() == ref.y()
                    && normalized.z() == ref.z()) {
                matches.add(normalized);
            }
        }
        return List.copyOf(matches);
    }

    private static boolean hasDeviceTypePrefix(String value) {
        String clean = cleanUserText(value);
        for (String type : List.of(
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                SignalDeviceData.TYPE_ACTION_RELAY,
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE
        )) {
            if (clean.startsWith(type + ":")) {
                return true;
            }
        }
        return false;
    }

    private static String stripDeviceTypePrefix(String value) {
        String clean = cleanUserText(value);
        for (String type : List.of(
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                SignalDeviceData.TYPE_ACTION_RELAY,
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE
        )) {
            String prefix = type + ":";
            if (clean.startsWith(prefix)) {
                return clean.substring(prefix.length());
            }
        }
        return clean;
    }

    private static SourcePositionRef parseSourcePositionRef(String deviceRef) {
        String value = cleanUserText(deviceRef);
        String expectedType = "";
        for (String type : List.of(
                SignalDeviceData.TYPE_SIGNAL_EMITTER,
                SignalDeviceData.TYPE_SIGNAL_RECEIVER,
                SignalDeviceData.TYPE_ACTION_RELAY,
                SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE
        )) {
            String prefix = type + ":";
            if (value.startsWith(prefix)) {
                expectedType = type;
                value = value.substring(prefix.length());
                break;
            }
        }
        int at = value.lastIndexOf('@');
        if (at <= 0 || at + 1 >= value.length()) {
            return null;
        }
        String dimension = value.substring(0, at).trim();
        String[] parts = value.substring(at + 1).split(",", -1);
        if (dimension.isBlank() || parts.length != 3) {
            return null;
        }
        try {
            return new SourcePositionRef(
                    dimension,
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    expectedType
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static State getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, SignalDeviceStore::load);
    }

    private static State load(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("tzz_mod")
                .resolve("signal_devices.json");
        State state = new State(path);
        DataFile dataFile = JsonStoreSupport.readOrDefault(path, DataFile.class, DataFile::new, "signal devices");
        if (dataFile.devices != null) {
            for (SignalDeviceData device : dataFile.devices) {
                if (device == null) {
                    continue;
                }
                SignalDeviceData normalized = device.normalized();
                if (!normalized.id().isBlank() && !normalized.dimension().isBlank()) {
                    replaceOrAdd(state, normalized);
                }
            }
        }
        return state;
    }

    private static ServerWorld findWorld(MinecraftServer server, String dimension) {
        if (server == null || dimension == null || dimension.isBlank()) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimension)) {
                return world;
            }
        }
        return null;
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
    }

    private static String tailId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        int atIndex = id.indexOf('@');
        if (atIndex < 0 || atIndex + 1 >= id.length()) {
            return "";
        }
        return id.substring(atIndex + 1);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class DataFile {
        public int version = 1;
        public List<SignalDeviceData> devices = new ArrayList<>();
    }

    public record ResolveResult(
            SignalDeviceData device,
            List<SignalDeviceData> matches,
            boolean ambiguous
    ) {
        public static ResolveResult none() {
            return new ResolveResult(null, List.of(), false);
        }

        public static ResolveResult unique(SignalDeviceData device) {
            return new ResolveResult(device, List.of(device), false);
        }

        public static ResolveResult ambiguous(List<SignalDeviceData> matches) {
            return new ResolveResult(null, List.copyOf(matches), true);
        }

        public boolean foundUnique() {
            return device != null && !ambiguous;
        }
    }

    private record SourcePositionRef(String dimension, int x, int y, int z, String expectedType) {
    }

    private static final class State {
        private static final long FLUSH_INTERVAL_TICKS = 100L;

        private final Path path;
        private final List<SignalDeviceData> devices = new ArrayList<>();
        private boolean dirty;
        private long lastFlushGameTime = -FLUSH_INTERVAL_TICKS;

        private State(Path path) {
            this.path = path;
        }

        private void markDirty() {
            dirty = true;
        }

        private void flushDirty(boolean force, long currentGameTime) {
            if (!dirty) {
                return;
            }
            if (!force && currentGameTime - lastFlushGameTime < FLUSH_INTERVAL_TICKS) {
                return;
            }
            DataFile dataFile = new DataFile();
            dataFile.devices = new ArrayList<>(devices.size());
            for (SignalDeviceData device : devices) {
                dataFile.devices.add(device.normalized());
            }
            if (JsonStoreSupport.write(path, dataFile, "signal devices")) {
                dirty = false;
                lastFlushGameTime = currentGameTime;
            }
        }
    }
}
