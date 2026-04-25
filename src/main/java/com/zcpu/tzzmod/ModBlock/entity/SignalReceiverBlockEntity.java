package com.zcpu.tzzmod.ModBlock.entity;

import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.custom.SignalReceiverBlock;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SignalReceiverBlockEntity extends BlockEntity {
    public static final int DEFAULT_PULSE_TICKS = 5;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 72000;

    private static final String CHANNEL_KEY = "Channel";
    private static final String ENABLED_KEY = "Enabled";
    private static final String PULSE_TICKS_KEY = "PulseTicks";
    private static final String REMAINING_PULSE_TICKS_KEY = "RemainingPulseTicks";
    private static final String LAST_RECEIVED_GAME_TIME_KEY = "LastReceivedGameTime";
    private static final String LAST_RECEIVED_WALL_TIME_KEY = "LastReceivedWallTimeMillis";
    private static final String LAST_RESULT_KEY = "LastResult";

    private String channel = "";
    private boolean enabled = true;
    private int pulseTicks = DEFAULT_PULSE_TICKS;
    private int remainingPulseTicks = 0;
    private long lastReceivedGameTime = 0L;
    private long lastReceivedWallTimeMillis = 0L;
    private String lastResult = "";

    public SignalReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_RECEIVER, pos, state);
    }

    public String channel() {
        return channel;
    }

    public boolean enabled() {
        return enabled;
    }

    public int pulseTicks() {
        return pulseTicks;
    }

    public int remainingPulseTicks() {
        return remainingPulseTicks;
    }

    public long lastReceivedGameTime() {
        return lastReceivedGameTime;
    }

    public long lastReceivedWallTimeMillis() {
        return lastReceivedWallTimeMillis;
    }

    public String lastResult() {
        return lastResult;
    }

    public void setChannel(String channel) {
        this.channel = SignalChannel.normalize(channel);
        markDirty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public void setPulseTicks(int pulseTicks) {
        this.pulseTicks = clampPulseTicks(pulseTicks);
        markDirty();
    }

    public ActionExecutionResult receiveSignal(ServerWorld world) {
        if (!enabled) {
            return ActionExecutionResult.failure(Text.literal("信号接收器已禁用"));
        }

        String normalizedChannel = SignalChannel.normalize(channel);
        if (normalizedChannel.isBlank()) {
            return ActionExecutionResult.failure(Text.literal("信号接收器未绑定频道"));
        }
        if (!SignalChannel.isValid(normalizedChannel)) {
            return ActionExecutionResult.failure(SignalChannel.validationError(normalizedChannel));
        }

        remainingPulseTicks = pulseTicks;
        lastReceivedGameTime = world.getTime();
        lastReceivedWallTimeMillis = System.currentTimeMillis();
        lastResult = "已输出红石脉冲：" + pulseTicks + " GT";
        SignalReceiverBlock.setPowered(world, pos, world.getBlockState(pos), true);
        markDirty();
        SignalDeviceStore.recordReceive(world, pos, this, ActionExecutionResult.success(Text.literal(lastResult)));
        return ActionExecutionResult.success(Text.literal(lastResult));
    }

    public static void tickServer(World world, BlockPos pos, BlockState state, SignalReceiverBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld) || blockEntity.remainingPulseTicks <= 0) {
            return;
        }

        blockEntity.remainingPulseTicks--;
        if (blockEntity.remainingPulseTicks <= 0) {
            blockEntity.remainingPulseTicks = 0;
            SignalReceiverBlock.setPowered(serverWorld, pos, state, false);
        }
        blockEntity.markDirty();
    }

    public static String sourceId(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static int clampPulseTicks(int ticks) {
        return Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, ticks));
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        channel = SignalChannel.normalize(view.getString(CHANNEL_KEY, ""));
        enabled = view.getBoolean(ENABLED_KEY, true);
        pulseTicks = clampPulseTicks(view.getInt(PULSE_TICKS_KEY, DEFAULT_PULSE_TICKS));
        remainingPulseTicks = Math.max(0, view.getInt(REMAINING_PULSE_TICKS_KEY, 0));
        lastReceivedGameTime = Math.max(0L, view.getLong(LAST_RECEIVED_GAME_TIME_KEY, 0L));
        lastReceivedWallTimeMillis = Math.max(0L, view.getLong(LAST_RECEIVED_WALL_TIME_KEY, 0L));
        lastResult = view.getString(LAST_RESULT_KEY, "");
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString(CHANNEL_KEY, channel);
        view.putBoolean(ENABLED_KEY, enabled);
        view.putInt(PULSE_TICKS_KEY, pulseTicks);
        view.putInt(REMAINING_PULSE_TICKS_KEY, remainingPulseTicks);
        view.putLong(LAST_RECEIVED_GAME_TIME_KEY, lastReceivedGameTime);
        view.putLong(LAST_RECEIVED_WALL_TIME_KEY, lastReceivedWallTimeMillis);
        view.putString(LAST_RESULT_KEY, lastResult);
    }
}
