package com.zcpu.tzzmod.ModBlock.entity;

import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public class SignalEmitterBlockEntity extends BlockEntity {
    private static final String CHANNEL_KEY = "Channel";
    private static final String ENABLED_KEY = "Enabled";
    private static final String LAST_POWERED_KEY = "LastPowered";

    private String channel = "";
    private boolean enabled = true;
    private boolean lastPowered = false;

    public SignalEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_EMITTER, pos, state);
    }

    public String channel() {
        return channel;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean lastPowered() {
        return lastPowered;
    }

    public void setChannel(String channel) {
        this.channel = SignalChannel.normalize(channel);
        markDirty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public void setLastPowered(boolean lastPowered) {
        this.lastPowered = lastPowered;
        markDirty();
    }

    public ActionExecutionResult emitSignal(ServerWorld world, @Nullable ServerPlayerEntity player) {
        if (!enabled) {
            return ActionExecutionResult.failure(Text.literal("信号发射器已禁用"));
        }

        String normalizedChannel = SignalChannel.normalize(channel);
        if (normalizedChannel.isBlank()) {
            return ActionExecutionResult.failure(Text.literal("信号发射器未绑定频道"));
        }
        if (!SignalChannel.isValid(normalizedChannel)) {
            return ActionExecutionResult.failure(SignalChannel.validationError(normalizedChannel));
        }

        SignalEvent event = new SignalEvent(
                normalizedChannel,
                player,
                world,
                Vec3d.ofCenter(pos),
                ActionSourceType.SIGNAL_DEVICE,
                sourceId(world, pos),
                0,
                world.getTime()
        );
        return SignalBridgeServer.emit(event);
    }

    public static String sourceId(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        channel = SignalChannel.normalize(view.getString(CHANNEL_KEY, ""));
        enabled = view.getBoolean(ENABLED_KEY, true);
        lastPowered = view.getBoolean(LAST_POWERED_KEY, false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString(CHANNEL_KEY, channel);
        view.putBoolean(ENABLED_KEY, enabled);
        view.putBoolean(LAST_POWERED_KEY, lastPowered);
    }
}
