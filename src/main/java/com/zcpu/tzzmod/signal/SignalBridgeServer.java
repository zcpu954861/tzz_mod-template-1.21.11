package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class SignalBridgeServer {
    public static final int MAX_SIGNAL_DEPTH = 8;

    private static final ThreadLocal<Integer> CURRENT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<String, Long> LAST_TRIGGER_TICKS = new HashMap<>();

    private SignalBridgeServer() {
    }

    public static ActionExecutionResult emit(SignalEvent event) {
        if (event == null) {
            recordHistory(null, "", 0, 0, 0, 0, 1, 0, "信号事件为空");
            return ActionExecutionResult.failure(Text.literal("信号事件为空"));
        }

        String channel = SignalChannel.normalize(event.channel());
        if (!SignalChannel.isValid(channel)) {
            recordHistory(event, channel, 0, 0, 0, 0, 1, event.depth(), "频道名称无效");
            return ActionExecutionResult.failure(SignalChannel.validationError(event.channel()));
        }

        if (event.player() == null || event.world() == null || event.position() == null) {
            recordHistory(event, channel, 0, 0, 0, 0, 1, event.depth(), "信号缺少玩家、世界或位置上下文");
            return ActionExecutionResult.failure(Text.literal("信号缺少玩家、世界或位置上下文"));
        }

        int depth = Math.max(event.depth(), CURRENT_DEPTH.get());
        if (depth > MAX_SIGNAL_DEPTH) {
            recordHistory(event, channel, 0, 0, 0, 0, 1, depth, "信号递归深度超过限制");
            return ActionExecutionResult.failure(Text.literal("信号递归深度超过限制：" + channel));
        }

        List<SignalListenerData> listeners = SignalListenerStore.getEnabledListenersForChannel(event.world().getServer(), channel);
        Tzz_mod.LOGGER.info(
                "[SignalBridge] channel={} source={} listenerCount={}",
                channel,
                event.sourceType() == null ? "unknown" : event.sourceType().id(),
                listeners.size()
        );

        if (listeners.isEmpty()) {
            recordHistory(event, channel, 0, 0, 0, 0, 0, depth, "没有监听器处理该信号");
            return ActionExecutionResult.success(Text.literal("信号已发出，但没有监听器处理：" + channel));
        }

        ActionExecutionResult lastResult = ActionExecutionResult.success(
                Text.literal("信号已发出：" + channel + "，匹配监听器：" + listeners.size())
        );
        int executedCount = 0;
        int skippedCooldownCount = 0;
        int skippedEmptyCount = 0;
        int failedCount = 0;
        int previousDepth = CURRENT_DEPTH.get();
        CURRENT_DEPTH.set(depth);
        try {
            for (SignalListenerData listener : listeners) {
                if (listener.actions().isEmpty()) {
                    skippedEmptyCount++;
                    continue;
                }

                if (isCoolingDown(listener, event.gameTime())) {
                    skippedCooldownCount++;
                    lastResult = ActionExecutionResult.success(Text.literal("信号监听器正在冷却：" + safeName(listener)));
                    continue;
                }

                ActionContext context = new ActionContext(
                        event.player(),
                        event.world(),
                        event.position(),
                        ActionSourceType.SIGNAL_BRIDGE,
                        listener.id(),
                        ItemStack.EMPTY
                );
                executedCount++;
                lastResult = ActionEngine.executeAll(context, listener.actions());
                LAST_TRIGGER_TICKS.put(listener.id(), event.gameTime());

                if (!lastResult.success()) {
                    failedCount++;
                    recordHistory(
                            event,
                            channel,
                            listeners.size(),
                            executedCount,
                            skippedCooldownCount,
                            skippedEmptyCount,
                            failedCount,
                            depth,
                            "部分监听器执行失败"
                    );
                    return lastResult;
                }
            }
        } finally {
            CURRENT_DEPTH.set(previousDepth);
        }

        recordHistory(
                event,
                channel,
                listeners.size(),
                executedCount,
                skippedCooldownCount,
                skippedEmptyCount,
                failedCount,
                depth,
                resultMessage(listeners.size(), executedCount, skippedCooldownCount, skippedEmptyCount, failedCount)
        );
        return lastResult;
    }

    public static void clearServerState() {
        LAST_TRIGGER_TICKS.clear();
        CURRENT_DEPTH.remove();
        SignalEventHistory.clear();
    }

    public static int currentDepth() {
        return CURRENT_DEPTH.get();
    }

    private static boolean isCoolingDown(SignalListenerData listener, long gameTime) {
        int cooldownTicks = listener.cooldownTicks();
        if (cooldownTicks <= 0) {
            return false;
        }

        Long lastTick = LAST_TRIGGER_TICKS.get(listener.id());
        return lastTick != null && gameTime - lastTick < cooldownTicks;
    }

    private static String safeName(SignalListenerData listener) {
        return listener.name() == null || listener.name().isBlank() ? "未命名监听器" : listener.name();
    }

    private static void recordHistory(
            SignalEvent event,
            String channel,
            int listenerCount,
            int executedCount,
            int skippedCooldownCount,
            int skippedEmptyCount,
            int failedCount,
            int depth,
            String resultMessage
    ) {
        SignalEventHistory.record(new SignalEventRecord(
                event == null ? 0L : event.gameTime(),
                channel == null || channel.isBlank() ? "unknown" : channel,
                event == null || event.player() == null ? "unknown" : event.player().getName().getString(),
                event == null || event.sourceType() == null ? "unknown" : event.sourceType().id(),
                event == null || event.sourceId() == null || event.sourceId().isBlank() ? "unknown" : event.sourceId(),
                listenerCount,
                executedCount,
                skippedCooldownCount,
                skippedEmptyCount,
                failedCount,
                depth,
                resultMessage == null || resultMessage.isBlank() ? "已处理信号" : resultMessage
        ));
    }

    private static String resultMessage(
            int listenerCount,
            int executedCount,
            int skippedCooldownCount,
            int skippedEmptyCount,
            int failedCount
    ) {
        if (failedCount > 0) {
            return "部分监听器执行失败";
        }
        if (listenerCount == 0) {
            return "没有监听器处理该信号";
        }
        if (executedCount > 0 && skippedCooldownCount == 0 && skippedEmptyCount == 0) {
            return "已处理信号";
        }
        if (skippedCooldownCount == listenerCount) {
            return "所有监听器均因冷却跳过";
        }
        if (skippedEmptyCount == listenerCount) {
            return "所有监听器均没有配置动作";
        }
        if (skippedCooldownCount > 0 && skippedEmptyCount > 0) {
            return "部分监听器因冷却或空动作跳过";
        }
        if (skippedCooldownCount > 0) {
            return "部分监听器因冷却跳过";
        }
        if (skippedEmptyCount > 0) {
            return "部分监听器没有配置动作";
        }
        return "已处理信号";
    }
}
