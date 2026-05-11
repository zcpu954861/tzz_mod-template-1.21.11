package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.util.NullSafety;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirtualBlockDeviceContainerHandler {
    private static final long PENDING_OPEN_TICKS = 5L;
    private static final Map<UUID, PendingOpen> PENDING_OPENS = new HashMap<>();
    private static final Map<UUID, OpenSession> OPEN_SESSIONS = new HashMap<>();
    private static final Map<String, Long> LAST_CHANGE_CHECKS = new HashMap<>();
    private static boolean registered;

    private VirtualBlockDeviceContainerHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockPos pos = hitResult.getBlockPos();
            SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(serverWorld.getServer(), serverWorld, pos);
            if (!isOpenCloseCandidate(serverWorld, pos, device)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            PENDING_OPENS.put(serverPlayer.getUuid(), new PendingOpen(device.id(), serverWorld, pos, serverWorld.getTime()));
            return NullSafety.requireNonNull(ActionResult.PASS);
        });
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        tickOpenClose(server);
        tickContentChanges(server);
    }

    public static ServerPlayerEntity playerForOpenSession(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        for (OpenSession session : OPEN_SESSIONS.values()) {
            if (deviceId.equals(session.deviceId())) {
                return session.player();
            }
        }
        return null;
    }

    private static boolean isOpenCloseCandidate(ServerWorld world, BlockPos pos, SignalDeviceData device) {
        if (device == null
                || !SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())
                || !device.enabled()
                || !device.containerEnabled()
                || (device.containerOpenChannel().isBlank() && device.containerCloseChannel().isBlank())) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        return !state.isAir()
                && VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())
                && ContainerDeviceSupport.isContainer(world, pos);
    }

    private static void tickOpenClose(MinecraftServer server) {
        Iterator<Map.Entry<UUID, OpenSession>> sessionIterator = OPEN_SESSIONS.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            Map.Entry<UUID, OpenSession> entry = sessionIterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                sessionIterator.remove();
                continue;
            }

            OpenSession session = entry.getValue();
            if (player.currentScreenHandler != player.playerScreenHandler
                    && player.currentScreenHandler.syncId == session.syncId()) {
                continue;
            }

            emitContainerEvent(session.world(), session.pos(), session.deviceId(), "close", player);
            sessionIterator.remove();
        }

        Iterator<Map.Entry<UUID, PendingOpen>> pendingIterator = PENDING_OPENS.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<UUID, PendingOpen> entry = pendingIterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                pendingIterator.remove();
                continue;
            }

            PendingOpen pending = entry.getValue();
            if (pending.world().getTime() - pending.createdGameTime() > PENDING_OPEN_TICKS) {
                pendingIterator.remove();
                continue;
            }

            if (player.currentScreenHandler == player.playerScreenHandler) {
                continue;
            }

            OPEN_SESSIONS.put(
                    entry.getKey(),
                    new OpenSession(pending.deviceId(), pending.world(), pending.pos(), player.currentScreenHandler.syncId, player)
            );
            emitContainerEvent(pending.world(), pending.pos(), pending.deviceId(), "open", player);
            pendingIterator.remove();
        }
    }

    private static void tickContentChanges(MinecraftServer server) {
        for (SignalDeviceData device : SignalDeviceStore.getVirtualBlockDevicesSnapshot(server)) {
            boolean hasChangeChannel = device.containerEnabled()
                    && !device.containerChangeChannel().isBlank()
                    && SignalChannel.isValid(device.containerChangeChannel());
            boolean hasItemConditions = device.containerEnabled() && hasEnabledItemConditions(device.itemConditions());
            if (!device.enabled() || (!hasChangeChannel && !hasItemConditions)) {
                continue;
            }

            ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
            if (world == null) {
                continue;
            }
            BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            long gameTime = world.getTime();
            long lastCheck = LAST_CHANGE_CHECKS.getOrDefault(device.id(), 0L);
            int interval = Math.max(1, device.containerChangeCheckIntervalTicks());
            if (gameTime - lastCheck < interval) {
                continue;
            }
            LAST_CHANGE_CHECKS.put(device.id(), gameTime);

            BlockState state = world.getBlockState(pos);
            if (state.isAir() || !VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())) {
                continue;
            }
            if (!ContainerDeviceSupport.hasInventory(world, pos)) {
                continue;
            }

            String fingerprint = ContainerDeviceSupport.fingerprint(world, pos);
            if (device.lastContainerFingerprint().isBlank()) {
                SignalDeviceStore.recordVirtualContainerFingerprintState(world, device, fingerprint, "已初始化容器内容指纹");
                continue;
            }
            if (fingerprint.equals(device.lastContainerFingerprint())) {
                continue;
            }
            boolean changeCooldownReady = SignalDeviceStore.getRemainingContainerCooldownTicks(device, gameTime) <= 0L;

            ServerPlayerEntity player = playerForOpenSession(device.id());
            boolean recordedFingerprint = false;
            if (hasChangeChannel && changeCooldownReady) {
                ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                    device.containerChangeChannel(),
                    player,
                    world,
                    Vec3d.ofCenter(pos),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    gameTime,
                    "容器内容变化"
                ));
                SignalDeviceStore.recordVirtualContainerEvent(world, device, "change", player, result, fingerprint);
                recordedFingerprint = true;
            }

            Inventory inventory = ContainerItemConditionSupport.inventory(world, pos);
            boolean itemConditionChanged = evaluateItemConditions(world, pos, device, inventory, player, gameTime);
            if (!recordedFingerprint) {
                SignalDeviceStore.recordVirtualContainerFingerprintState(
                        world,
                        device,
                        fingerprint,
                        itemConditionChanged ? "已检查容器物品条件" : "容器内容已变化"
                );
            }
        }
    }

    private static boolean evaluateItemConditions(
            ServerWorld world,
            BlockPos pos,
            SignalDeviceData device,
            Inventory inventory,
            ServerPlayerEntity player,
            long gameTime
    ) {
        if (inventory == null || !hasEnabledItemConditions(device.itemConditions())) {
            return false;
        }

        boolean changed = false;
        Map<String, Integer> totalCounts = ContainerItemConditionSupport.totalCounts(inventory, device.itemConditions());
        for (ContainerItemConditionData rawCondition : device.itemConditions()) {
            if (rawCondition == null || !rawCondition.enabled()) {
                continue;
            }
            ContainerItemConditionData condition = rawCondition.normalized();
            boolean currentMatched = ContainerItemConditionSupport.matchesWithTotals(inventory, condition, totalCounts);
            if (currentMatched == condition.lastMatched()) {
                continue;
            }

            changed = true;
            BlockStateConditionMode mode = BlockStateConditionMode.fromId(condition.mode());
            boolean entering = !condition.lastMatched() && currentMatched;
            boolean exiting = condition.lastMatched() && !currentMatched;
            boolean shouldEmit = entering ? mode.triggersEnter() : exiting && mode.triggersExit();
            String channel = entering
                    ? condition.channel()
                    : condition.offChannel().isBlank() ? condition.channel() : condition.offChannel();
            if (!shouldEmit || channel.isBlank() || !SignalChannel.isValid(channel)) {
                SignalDeviceStore.recordVirtualItemConditionState(
                        world,
                        device,
                        condition,
                        currentMatched,
                        "容器物品条件状态已更新"
                );
                continue;
            }

            ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                    channel,
                    player,
                    world,
                    Vec3d.ofCenter(pos),
                    ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                    device.id(),
                    SignalBridgeServer.currentDepth(),
                    gameTime,
                    "物品条件 " + condition.name() + " " + condition.type()
            ));
            SignalDeviceStore.recordVirtualItemConditionTrigger(world, device, condition, currentMatched, result);
        }
        return changed;
    }

    private static boolean hasEnabledItemConditions(List<ContainerItemConditionData> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        for (ContainerItemConditionData condition : conditions) {
            if (condition != null && condition.normalized().enabled()) {
                return true;
            }
        }
        return false;
    }

    private static void emitContainerEvent(ServerWorld world, BlockPos pos, String deviceId, String eventType, ServerPlayerEntity player) {
        SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(world.getServer(), world, pos);
        if (device == null || !device.id().equals(deviceId)) {
            return;
        }
        if (!device.enabled() || !device.containerEnabled()) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()
                || !VirtualBlockDeviceSupport.blockId(state).equals(device.blockId())
                || !ContainerDeviceSupport.isContainer(world, pos)
                || SignalDeviceStore.getRemainingContainerCooldownTicks(device, world.getTime()) > 0L) {
            return;
        }

        String channel = switch (eventType) {
            case "open" -> device.containerOpenChannel();
            case "close" -> device.containerCloseChannel();
            default -> "";
        };
        if (channel.isBlank() || !SignalChannel.isValid(channel)) {
            return;
        }

        String detail = "open".equals(eventType) ? "容器打开" : "容器关闭";
        ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                channel,
                player,
                world,
                Vec3d.ofCenter(pos),
                ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                device.id(),
                SignalBridgeServer.currentDepth(),
                world.getTime(),
                detail
        ));
        SignalDeviceStore.recordVirtualContainerEvent(world, device, eventType, player, result, null);
    }

    private record PendingOpen(
            String deviceId,
            ServerWorld world,
            BlockPos pos,
            long createdGameTime
    ) {
    }

    private record OpenSession(
            String deviceId,
            ServerWorld world,
            BlockPos pos,
            int syncId,
            ServerPlayerEntity player
    ) {
    }
}
