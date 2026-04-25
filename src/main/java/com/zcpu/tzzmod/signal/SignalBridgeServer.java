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
            return ActionExecutionResult.failure(Text.literal("信号事件为空"));
        }

        String channel = SignalChannel.normalize(event.channel());
        if (!SignalChannel.isValid(channel)) {
            return ActionExecutionResult.failure(SignalChannel.validationError(event.channel()));
        }

        if (event.player() == null || event.world() == null || event.position() == null) {
            return ActionExecutionResult.failure(Text.literal("信号缺少玩家、世界或位置上下文"));
        }

        int depth = Math.max(event.depth(), CURRENT_DEPTH.get());
        if (depth > MAX_SIGNAL_DEPTH) {
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
            return ActionExecutionResult.success(Text.literal("信号已发出，但没有监听器处理：" + channel));
        }

        ActionExecutionResult lastResult = ActionExecutionResult.success(
                Text.literal("信号已发出：" + channel + "，匹配监听器：" + listeners.size())
        );
        int previousDepth = CURRENT_DEPTH.get();
        CURRENT_DEPTH.set(depth);
        try {
            for (SignalListenerData listener : listeners) {
                if (listener.actions().isEmpty()) {
                    continue;
                }

                if (isCoolingDown(listener, event.gameTime())) {
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
                lastResult = ActionEngine.executeAll(context, listener.actions());
                LAST_TRIGGER_TICKS.put(listener.id(), event.gameTime());

                if (!lastResult.success()) {
                    return lastResult;
                }
            }
        } finally {
            CURRENT_DEPTH.set(previousDepth);
        }

        return lastResult;
    }

    public static void clearServerState() {
        LAST_TRIGGER_TICKS.clear();
        CURRENT_DEPTH.remove();
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
}
