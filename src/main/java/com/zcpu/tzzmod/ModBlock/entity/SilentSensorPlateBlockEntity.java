package com.zcpu.tzzmod.ModBlock.entity;

import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.custom.SilentSensorPlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SilentSensorPlateBlockEntity extends BlockEntity {
    private static final int REQUIRED_STILL_TICKS = 60;
    private static final double HORIZONTAL_POSITION_EPSILON_SQUARED = 1.0E-6D;
    private static final double VERTICAL_POSITION_EPSILON = 0.04D;
    private static final float ROTATION_EPSILON = 0.01F;

    private final Map<UUID, PlayerTrackingData> trackedPlayers = new HashMap<>();

    public SilentSensorPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILENT_SENSOR_PLATE, pos, state);
    }

    public static void tickServer(World world, BlockPos pos, BlockState state, SilentSensorPlateBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(state.getBlock() instanceof SilentSensorPlateBlock)) {
            blockEntity.trackedPlayers.clear();
            return;
        }

        Set<UUID> presentPlayers = new HashSet<>();
        boolean shouldPower = false;

        for (PlayerEntity player : serverWorld.getEntitiesByClass(PlayerEntity.class, SilentSensorPlateBlock.getDetectionBox(pos), candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            UUID playerId = player.getUuid();
            presentPlayers.add(playerId);

            PlayerTrackingData trackingData = blockEntity.trackedPlayers.get(playerId);
            if (trackingData == null) {
                trackingData = PlayerTrackingData.from(player);
                blockEntity.trackedPlayers.put(playerId, trackingData);
            } else if (trackingData.hasMoved(player)) {
                trackingData.updateFrom(player, 0);
            } else {
                trackingData.updateFrom(player, trackingData.stillTicks + 1);
            }

            if (trackingData.stillTicks >= REQUIRED_STILL_TICKS) {
                shouldPower = true;
            }
        }

        blockEntity.trackedPlayers.keySet().removeIf(uuid -> !presentPlayers.contains(uuid));
        SilentSensorPlateBlock.setPowered(serverWorld, pos, state, shouldPower);
    }

    private static final class PlayerTrackingData {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private int stillTicks;

        private PlayerTrackingData(double x, double y, double z, float yaw, float pitch, int stillTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.stillTicks = stillTicks;
        }

        private static PlayerTrackingData from(PlayerEntity player) {
            return new PlayerTrackingData(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), 1);
        }

        private boolean hasMoved(PlayerEntity player) {
            double deltaX = player.getX() - x;
            double deltaY = player.getY() - y;
            double deltaZ = player.getZ() - z;
            if ((deltaX * deltaX) + (deltaZ * deltaZ) > HORIZONTAL_POSITION_EPSILON_SQUARED) {
                return true;
            }

            if (Math.abs(deltaY) > VERTICAL_POSITION_EPSILON) {
                return true;
            }

            float yawDelta = Math.abs(MathHelper.wrapDegrees(player.getYaw() - yaw));
            float pitchDelta = Math.abs(player.getPitch() - pitch);
            return yawDelta > ROTATION_EPSILON || pitchDelta > ROTATION_EPSILON;
        }

        private void updateFrom(PlayerEntity player, int stillTicks) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.stillTicks = stillTicks;
        }
    }
}


