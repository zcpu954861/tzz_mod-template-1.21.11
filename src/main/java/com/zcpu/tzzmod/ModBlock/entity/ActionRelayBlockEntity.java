package com.zcpu.tzzmod.ModBlock.entity;

import com.google.gson.Gson;
import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.custom.ActionRelayBlock;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.ConditionGroupIds;
import com.zcpu.tzzmod.condition.runtime.ConditionGateRequest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeContextBuilder;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class ActionRelayBlockEntity extends BlockEntity {
    public static final int MIN_COOLDOWN_TICKS = 0;
    public static final int MAX_COOLDOWN_TICKS = 72000;
    public static final int ACTIVE_TICKS = 10;

    private static final Gson GSON = new Gson();
    private static final ConditionGateService CONDITION_GATE_SERVICE = new ConditionGateService();
    private static final String CHANNEL_KEY = "Channel";
    private static final String ENABLED_KEY = "Enabled";
    private static final String COOLDOWN_TICKS_KEY = "CooldownTicks";
    private static final String CONDITION_GROUP_ID_KEY = "ConditionGroupId";
    private static final String ACTIONS_KEY = "Actions";
    private static final String LAST_RUN_GAME_TIME_KEY = "LastRunGameTime";
    private static final String LAST_RUN_WALL_TIME_KEY = "LastRunWallTimeMillis";
    private static final String LAST_RESULT_KEY = "LastResult";
    private static final String LAST_SUCCESS_COUNT_KEY = "LastSuccessCount";
    private static final String LAST_FAILURE_COUNT_KEY = "LastFailureCount";
    private static final String LAST_SOURCE_CHANNEL_KEY = "LastSourceChannel";
    private static final String LAST_SOURCE_TYPE_KEY = "LastSourceType";
    private static final String ACTIVE_TICKS_KEY = "ActiveTicks";

    private String channel = "";
    private boolean enabled = true;
    private int cooldownTicks = 0;
    private String conditionGroupId = "";
    private List<ActionConfig> actions = List.of();
    private long lastRunGameTime = 0L;
    private long lastRunWallTimeMillis = 0L;
    private String lastResult = "";
    private int lastSuccessCount = 0;
    private int lastFailureCount = 0;
    private String lastSourceChannel = "";
    private String lastSourceType = "";
    private int activeTicks = 0;

    public ActionRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACTION_RELAY, pos, state);
    }

    public String channel() {
        return channel;
    }

    public boolean enabled() {
        return enabled;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String conditionGroupId() {
        return conditionGroupId;
    }

    public List<ActionConfig> actions() {
        return actions;
    }

    public long lastRunGameTime() {
        return lastRunGameTime;
    }

    public long lastRunWallTimeMillis() {
        return lastRunWallTimeMillis;
    }

    public String lastResult() {
        return lastResult;
    }

    public int lastSuccessCount() {
        return lastSuccessCount;
    }

    public int lastFailureCount() {
        return lastFailureCount;
    }

    public String lastSourceChannel() {
        return lastSourceChannel;
    }

    public String lastSourceType() {
        return lastSourceType;
    }

    public int activeTicks() {
        return activeTicks;
    }

    public void setChannel(String channel) {
        this.channel = SignalChannel.normalize(channel);
        markDirty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = clampCooldownTicks(cooldownTicks);
        markDirty();
    }

    public void setConditionGroupId(String conditionGroupId) {
        this.conditionGroupId = ConditionGroupIds.normalize(conditionGroupId);
        markDirty();
    }

    public void addAction(ActionConfig action) {
        if (action == null) {
            return;
        }
        List<ActionConfig> copy = new ArrayList<>(actions);
        copy.add(normalizeAction(action));
        actions = List.copyOf(copy);
        markDirty();
    }

    public boolean removeAction(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= actions.size()) {
            return false;
        }
        List<ActionConfig> copy = new ArrayList<>(actions);
        copy.remove(zeroBasedIndex);
        actions = List.copyOf(copy);
        markDirty();
        return true;
    }

    public void clearActions() {
        actions = List.of();
        markDirty();
    }

    public void replaceActions(List<ActionConfig> newActions) {
        if (newActions == null || newActions.isEmpty()) {
            actions = List.of();
            markDirty();
            return;
        }
        List<ActionConfig> copy = new ArrayList<>();
        for (ActionConfig action : newActions) {
            if (action != null) {
                copy.add(normalizeAction(action));
            }
        }
        actions = List.copyOf(copy);
        markDirty();
    }

    public long remainingCooldownTicks(long currentGameTime) {
        if (cooldownTicks <= 0 || lastRunGameTime <= 0L) {
            return 0L;
        }
        return Math.max(0L, cooldownTicks - (currentGameTime - lastRunGameTime));
    }

    public ActionExecutionResult executeRelayActions(ServerWorld world, @Nullable SignalEvent event, boolean manual) {
        if (!enabled) {
            return ActionExecutionResult.failure(Text.literal("动作继电器已禁用"));
        }
        if (actions.isEmpty()) {
            return ActionExecutionResult.failure(Text.literal("动作继电器没有配置动作"));
        }
        if (!manual && remainingCooldownTicks(world.getTime()) > 0L) {
            return ActionExecutionResult.failure(Text.literal("动作继电器正在冷却"));
        }
        if (!manual) {
            ConditionGateResult gate = CONDITION_GATE_SERVICE.evaluate(world.getServer(), new ConditionGateRequest(
                    conditionGroupId,
                    ConditionRuntimeTargetType.ACTION_RELAY,
                    sourceId(world, pos),
                    () -> ConditionRuntimeContextBuilder.actionRelay(world, pos, null, this, event)
            ));
            if (!gate.allowed()) {
                return ActionExecutionResult.failure(Text.literal("动作继电器条件阻断：" + gate.failureReason()));
            }
        }

        ActionContext context = new ActionContext(
                event == null ? null : event.player(),
                world,
                Vec3d.ofCenter(pos),
                ActionSourceType.ACTION_RELAY,
                sourceId(world, pos),
                ItemStack.EMPTY
        );

        int successCount = 0;
        int failureCount = 0;
        ActionExecutionResult last = ActionExecutionResult.failure(Text.literal("没有可执行动作"));
        for (ActionConfig action : actions) {
            if (action == null || !action.isUsable()) {
                continue;
            }
            last = ActionEngine.execute(context, action);
            if (last.success()) {
                successCount++;
            } else {
                failureCount++;
                break;
            }
        }

        if (successCount > 0 && failureCount == 0) {
            last = ActionExecutionResult.success(Text.literal("动作继电器已执行：" + successCount + " 个动作"));
        }
        updateLastRun(world, event, last, successCount, failureCount);
        return last;
    }

    public ActionExecutionResult executeRelayActions(ServerWorld world, @Nullable ServerPlayerEntity player, boolean manual) {
        SignalEvent event = new SignalEvent(
                channel,
                player,
                world,
                Vec3d.ofCenter(pos),
                ActionSourceType.COMMAND,
                "manual",
                0,
                world.getTime()
        );
        return executeRelayActions(world, event, manual);
    }

    public static void tickServer(World world, BlockPos pos, BlockState state, ActionRelayBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld) || blockEntity.activeTicks <= 0) {
            return;
        }

        blockEntity.activeTicks--;
        if (blockEntity.activeTicks <= 0) {
            blockEntity.activeTicks = 0;
            ActionRelayBlock.setActive(serverWorld, pos, state, false);
        }
        blockEntity.markDirty();
    }

    public static String sourceId(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static int clampCooldownTicks(int ticks) {
        return Math.max(MIN_COOLDOWN_TICKS, Math.min(MAX_COOLDOWN_TICKS, ticks));
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        channel = SignalChannel.normalize(view.getString(CHANNEL_KEY, ""));
        enabled = view.getBoolean(ENABLED_KEY, true);
        cooldownTicks = clampCooldownTicks(view.getInt(COOLDOWN_TICKS_KEY, 0));
        conditionGroupId = ConditionGroupIds.normalize(view.getString(CONDITION_GROUP_ID_KEY, ""));
        actions = parseActions(view.getString(ACTIONS_KEY, "[]"));
        lastRunGameTime = Math.max(0L, view.getLong(LAST_RUN_GAME_TIME_KEY, 0L));
        lastRunWallTimeMillis = Math.max(0L, view.getLong(LAST_RUN_WALL_TIME_KEY, 0L));
        lastResult = view.getString(LAST_RESULT_KEY, "");
        lastSuccessCount = Math.max(0, view.getInt(LAST_SUCCESS_COUNT_KEY, 0));
        lastFailureCount = Math.max(0, view.getInt(LAST_FAILURE_COUNT_KEY, 0));
        lastSourceChannel = SignalChannel.normalize(view.getString(LAST_SOURCE_CHANNEL_KEY, ""));
        lastSourceType = view.getString(LAST_SOURCE_TYPE_KEY, "");
        activeTicks = Math.max(0, view.getInt(ACTIVE_TICKS_KEY, 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString(CHANNEL_KEY, channel);
        view.putBoolean(ENABLED_KEY, enabled);
        view.putInt(COOLDOWN_TICKS_KEY, cooldownTicks);
        view.putString(CONDITION_GROUP_ID_KEY, conditionGroupId);
        view.putString(ACTIONS_KEY, GSON.toJson(actions));
        view.putLong(LAST_RUN_GAME_TIME_KEY, lastRunGameTime);
        view.putLong(LAST_RUN_WALL_TIME_KEY, lastRunWallTimeMillis);
        view.putString(LAST_RESULT_KEY, lastResult);
        view.putInt(LAST_SUCCESS_COUNT_KEY, lastSuccessCount);
        view.putInt(LAST_FAILURE_COUNT_KEY, lastFailureCount);
        view.putString(LAST_SOURCE_CHANNEL_KEY, lastSourceChannel);
        view.putString(LAST_SOURCE_TYPE_KEY, lastSourceType);
        view.putInt(ACTIVE_TICKS_KEY, activeTicks);
    }

    private void updateLastRun(
            ServerWorld world,
            @Nullable SignalEvent event,
            ActionExecutionResult result,
            int successCount,
            int failureCount
    ) {
        lastRunGameTime = world.getTime();
        lastRunWallTimeMillis = System.currentTimeMillis();
        lastResult = result == null || result.message() == null ? "" : result.message().getString();
        lastSuccessCount = successCount;
        lastFailureCount = failureCount;
        lastSourceChannel = event == null ? channel : SignalChannel.normalize(event.channel());
        lastSourceType = event == null || event.sourceType() == null ? "unknown" : event.sourceType().id();
        activeTicks = ACTIVE_TICKS;
        ActionRelayBlock.setActive(world, pos, world.getBlockState(pos), true);
        markDirty();
        SignalDeviceStore.recordActionRelayRun(world, pos, this, result);
    }

    private static List<ActionConfig> parseActions(String raw) {
        try {
            ActionConfig[] parsed = GSON.fromJson(raw == null || raw.isBlank() ? "[]" : raw, ActionConfig[].class);
            if (parsed == null || parsed.length == 0) {
                return List.of();
            }
            List<ActionConfig> result = new ArrayList<>();
            for (ActionConfig action : parsed) {
                if (action != null) {
                    result.add(normalizeAction(action));
                }
            }
            return List.copyOf(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static ActionConfig normalizeAction(ActionConfig action) {
        ActionType type = action.type() == null ? ActionType.COMMAND : action.type();
        String value = action.value() == null ? "" : action.value().trim();
        if (type == ActionType.COMMAND) {
            value = ActionConfig.normalizeCommand(value);
        } else if (type == ActionType.SIGNAL) {
            value = SignalChannel.normalize(value);
        }
        return new ActionConfig(
                type,
                value,
                action.enabled(),
                action.requiresOp(),
                Math.max(0, action.cooldownTicks()),
                action.notifyOps()
        );
    }
}
