package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

public final class SignalDeviceStore {
    private static final Map<MinecraftServer, State> CACHE = new WeakHashMap<>();

    private SignalDeviceStore() {
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
        SignalDeviceData existing = findById(state, SignalEmitterBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = fromEmitter(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
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
        SignalDeviceData existing = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = fromReceiver(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
        return updated;
    }

    public static synchronized SignalDeviceData updateChannel(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        return upsertReceiver(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updateEnabled(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        return upsertReceiver(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData updatePulse(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity) {
        return upsertReceiver(world, pos, blockEntity);
    }

    public static synchronized SignalDeviceData upsertActionRelay(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, ActionRelayBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = fromActionRelay(world, pos, blockEntity, existing, false);
        replaceOrAdd(state, updated);
        state.markDirty();
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
        return upsertActionRelay(world, pos, blockEntity);
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
        return updated;
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
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, SignalEmitterBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalEmitterBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromEmitter(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, SignalReceiverBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, SignalReceiverBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromReceiver(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
        return updated;
    }

    public static synchronized SignalDeviceData setName(ServerWorld world, BlockPos pos, ActionRelayBlockEntity blockEntity, String name) {
        State state = getState(world.getServer());
        SignalDeviceData existing = findById(state, ActionRelayBlockEntity.sourceId(world, pos));
        SignalDeviceData updated = withName(fromActionRelay(world, pos, blockEntity, existing, false), cleanUserText(name));
        replaceOrAdd(state, updated);
        state.markDirty();
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
        boolean removed = state.devices.removeIf(device -> sourceId.equals(device.id()));
        if (removed) {
            state.markDirty();
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
                existing.lastConditionResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
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
                conditioned.lastConditionResult()
        ).normalized();
        replaceOrAdd(state, updated);
        state.markDirty();
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
        for (SignalDeviceData device : state.devices) {
            if (device.id().equals(query)) {
                return ResolveResult.unique(device);
            }
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

    private static SignalDeviceData fromEmitter(
            ServerWorld world,
            BlockPos pos,
            SignalEmitterBlockEntity blockEntity,
            SignalDeviceData existing,
            boolean preserveUpdatedTime
    ) {
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
                existing == null ? "" : existing.lastConditionResult()
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
                device.lastConditionResult()
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
                device.lastConditionResult()
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
                lastConditionResult
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
                device.lastConditionResult()
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

    private static SignalDeviceData findById(State state, String id) {
        for (SignalDeviceData device : state.devices) {
            if (device.id().equals(id)) {
                return device;
            }
        }
        return null;
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
                    state.devices.add(normalized);
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
