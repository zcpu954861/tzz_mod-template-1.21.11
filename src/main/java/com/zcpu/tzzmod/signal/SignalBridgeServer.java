package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.condition.runtime.ConditionGateRequest;
import com.zcpu.tzzmod.condition.runtime.ConditionGateResult;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeContextBuilder;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.device.ActionRelayDispatcher;
import com.zcpu.tzzmod.signal.device.SignalReceiverDispatcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class SignalBridgeServer {
    public static final int MAX_SIGNAL_DEPTH = 8;

    private static final ThreadLocal<Integer> CURRENT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<String, Long> LAST_TRIGGER_TICKS = new HashMap<>();
    private static final ConditionGateService CONDITION_GATE_SERVICE = new ConditionGateService();
    private static final ConditionActionGateService CONDITION_ACTION_GATE_SERVICE = new ConditionActionGateService();

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

        if (event.world() == null || event.position() == null) {
            recordHistory(event, channel, 0, 0, 0, 0, 1, event.depth(), "信号缺少世界或位置上下文");
            return ActionExecutionResult.failure(Text.literal("信号缺少世界或位置上下文"));
        }

        int depth = Math.max(event.depth(), CURRENT_DEPTH.get());
        if (depth > MAX_SIGNAL_DEPTH) {
            recordHistory(event, channel, 0, 0, 0, 0, 1, depth, "信号递归深度超过限制");
            return ActionExecutionResult.failure(Text.literal("信号递归深度超过限制：" + channel));
        }

        int receiverCount = SignalReceiverDispatcher.dispatch(event, channel);
        ActionRelayDispatcher.DispatchResult relayResult = ActionRelayDispatcher.dispatch(event, channel);
        List<SignalListenerData> listeners = SignalListenerStore.getEnabledListenersForChannel(event.world().getServer(), channel);
        Tzz_mod.LOGGER.info(
                "[SignalBridge] channel={} source={} listenerCount={} receiverCount={} relayExecuted={} relayFailed={}",
                channel,
                event.sourceType() == null ? "unknown" : event.sourceType().id(),
                listeners.size(),
                receiverCount,
                relayResult.executedCount(),
                relayResult.failedCount()
        );

        if (listeners.isEmpty()) {
            String message = noListenerMessage(receiverCount, relayResult);
            recordHistory(event, channel, 0, 0, 0, 0, 0, depth, message);
            return ActionExecutionResult.success(Text.literal("信号已发出：" + channel + "；" + message));
        }

        ActionExecutionResult lastResult = ActionExecutionResult.success(
                Text.literal("信号已发出：" + channel + "，匹配监听器：" + listeners.size()
                        + "，触发接收器：" + receiverCount
                        + "，执行动作继电器：" + relayResult.executedCount())
        );
        int executedCount = 0;
        int skippedCooldownCount = 0;
        int skippedEmptyCount = 0;
        int skippedConditionCount = 0;
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

                ConditionGateResult gate = CONDITION_GATE_SERVICE.evaluate(event.world().getServer(), new ConditionGateRequest(
                        listener.conditionGroupId(),
                        ConditionRuntimeTargetType.SIGNAL_LISTENER,
                        listener.id(),
                        () -> ConditionRuntimeContextBuilder.signalListener(event, listener)
                ));
                if (!gate.allowed()) {
                    skippedConditionCount++;
                    lastResult = ActionExecutionResult.success(Text.literal("监听器条件阻断：" + gate.failureReason()));
                    Tzz_mod.LOGGER.info(
                            "[SignalBridge] listener condition gate blocked listener={} channel={} code={} reason={}",
                            listener.id(),
                            channel,
                            gate.code(),
                            gate.failureReason()
                    );
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
                lastResult = executeListenerActions(context, event, listener);
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
                            lastResult.message() == null ? "部分监听器执行失败" : lastResult.message().getString()
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
                resultMessage(listeners.size(), executedCount, skippedCooldownCount, skippedEmptyCount, failedCount, receiverCount, relayResult)
                        + conditionSkipSuffix(skippedConditionCount)
        );
        return lastResult;
    }

    private static ActionExecutionResult executeListenerActions(ActionContext context, SignalEvent event, SignalListenerData listener) {
        ActionExecutionResult lastResult = ActionExecutionResult.success(Text.literal("未执行动作"));
        List<ActionConfig> actions = listener == null ? List.of() : listener.actions();
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index);
            if (action == null || !action.isUsable()) {
                continue;
            }
            ConditionGateResult actionGate = CONDITION_ACTION_GATE_SERVICE.evaluate(
                    event == null || event.world() == null ? null : event.world().getServer(),
                    action,
                    ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION,
                    ConditionActionGateService.actionTargetId("listener", listener == null ? "" : listener.id(), index),
                    ConditionRuntimeTargetType.SIGNAL_LISTENER,
                    listener == null ? "" : listener.id(),
                    "",
                    index,
                    () -> ConditionRuntimeContextBuilder.signalListener(event, listener)
            );
            if (!actionGate.allowed()) {
                lastResult = ActionExecutionResult.success(Text.literal(
                        "监听器第 " + (index + 1) + " 条 action 被条件阻断，已跳过：" + actionGate.failureReason()
                ));
                continue;
            }
            lastResult = ActionEngine.execute(context, action);
            if (!lastResult.success()) {
                String reason = lastResult.message() == null ? "unknown" : lastResult.message().getString();
                String type = action.type() == null ? "unknown" : action.type().id();
                return ActionExecutionResult.failure(Text.literal(
                        "监听器动作执行失败：第 " + (index + 1) + " 条 " + type + " action，原因：" + reason
                ));
            }
        }
        return lastResult;
    }

    public static void clearServerState() {
        LAST_TRIGGER_TICKS.clear();
        CURRENT_DEPTH.remove();
        SignalEventHistory.clear();
    }

    public static void clearListenerRuntime(String listenerId) {
        if (listenerId == null || listenerId.isBlank()) {
            return;
        }
        LAST_TRIGGER_TICKS.remove(listenerId);
    }

    public static int currentDepth() {
        return CURRENT_DEPTH.get();
    }

    public static long getRemainingCooldownTicks(SignalListenerData listener, long currentGameTime) {
        if (listener == null || listener.cooldownTicks() <= 0) {
            return 0L;
        }

        Long lastTick = LAST_TRIGGER_TICKS.get(listener.id());
        if (lastTick == null) {
            return 0L;
        }

        long remaining = listener.cooldownTicks() - (currentGameTime - lastTick);
        return Math.max(0L, remaining);
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
        String detail = event == null || event.detail() == null || event.detail().isBlank()
                ? ""
                : "（" + event.detail() + "）";
        String historyMessage = resultMessage == null || resultMessage.isBlank() ? "已处理信号" : resultMessage;
        SignalEventHistory.record(new SignalEventRecord(
                event == null ? 0L : event.gameTime(),
                System.currentTimeMillis(),
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
                historyMessage + detail
        ));
    }

    private static String resultMessage(
            int listenerCount,
            int executedCount,
            int skippedCooldownCount,
            int skippedEmptyCount,
            int failedCount,
            int receiverCount,
            ActionRelayDispatcher.DispatchResult relayResult
    ) {
        if (failedCount > 0) {
            return "部分监听器执行失败";
        }
        if (listenerCount == 0) {
            return noListenerMessage(receiverCount, relayResult);
        }
        if (executedCount > 0 && skippedCooldownCount == 0 && skippedEmptyCount == 0) {
            return appendDeviceSummary("已处理信号", receiverCount, relayResult);
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
        return appendDeviceSummary("已处理信号", receiverCount, relayResult);
    }

    private static String noListenerMessage(int receiverCount, ActionRelayDispatcher.DispatchResult relayResult) {
        return appendDeviceSummary("没有监听器处理该信号", receiverCount, relayResult);
    }

    private static String appendDeviceSummary(String base, int receiverCount, ActionRelayDispatcher.DispatchResult relayResult) {
        StringBuilder builder = new StringBuilder(base);
        if (receiverCount > 0) {
            builder.append("，已触发接收器：").append(receiverCount);
        }
        if (relayResult.executedCount() > 0) {
            builder.append("，已执行动作继电器：").append(relayResult.executedCount());
        }
        if (relayResult.failedCount() > 0) {
            builder.append("，动作继电器跳过或失败：").append(relayResult.failedCount());
        }
        return builder.toString();
    }

    private static String conditionSkipSuffix(int skippedConditionCount) {
        return skippedConditionCount <= 0 ? "" : "，条件阻断监听器：" + skippedConditionCount;
    }
}
