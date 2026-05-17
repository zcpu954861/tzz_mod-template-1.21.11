package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.scheduler.TimerRuntimeService;
import com.zcpu.tzzmod.scheduler.TimerScopeMode;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.scheduler.TimerValidationIssue;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminTimerDoctorService {
    public List<WebAdminDtos.DoctorIssueDto> inspect(MinecraftServer server) {
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        TimerStore.TimerLoadResult loaded = TimerStore.loadWithStatus(server);
        if (loaded.degraded()) {
            issues.add(issue(
                    "timer-store-degraded",
                    "ERROR",
                    "Timer 配置读取失败",
                    loaded.message().isBlank() ? "timers.json 当前不可读取，Timer 写入与运行会安全失败。" : loaded.message(),
                    "SYSTEM",
                    "",
                    "修复 <world>/tzz/webadmin/timers.json 后再继续编辑 Timer。",
                    "#/timers"
            ));
            return List.copyOf(issues);
        }
        Map<String, TimerDefinition> timers = loaded.file().timers;
        for (TimerDefinition raw : timers.values()) {
            inspectDefinition(raw == null ? null : raw.normalized(), issues, server);
        }
        inspectTimerActionReferences(server, timers, issues);
        return List.copyOf(issues);
    }

    private void inspectDefinition(TimerDefinition timer, List<WebAdminDtos.DoctorIssueDto> issues, MinecraftServer server) {
        if (timer == null) {
            return;
        }
        for (TimerValidationIssue validationIssue : TimerValidator.validate(timer, false)) {
            issues.add(issue(
                    "timer-invalid:" + timer.id + ":" + validationIssue.code(),
                    "ERROR",
                    "Timer 配置无效",
                    "Timer " + timerLabel(timer) + " 配置无效：" + validationIssue.message(),
                    "TIMER",
                    timer.id,
                    "修正字段 " + validationIssue.field() + " 后重新保存。",
                    "#/timers/" + timer.id
            ));
        }
        if (!timer.hasTickOrCompleteOutput()) {
            issues.add(issue(
                    "timer-no-output:" + timer.id,
                    "WARNING",
                    "Timer 没有输出或动作",
                    "Timer " + timerLabel(timer) + " 没有 onCompleteActions / onTickActions / outputChannel；启动后只能作为纯 status timer。",
                    "TIMER",
                    timer.id,
                    "如不是刻意作为纯状态计时器，请至少配置完成动作或输出频道。",
                    "#/timers/" + timer.id
            ));
        }
        if (timer.mode == TimerMode.REPEAT && timer.intervalTicks > 0 && timer.intervalTicks < 20) {
            issues.add(issue(
                    "timer-repeat-small-interval:" + timer.id,
                    "WARNING",
                    "REPEAT 间隔过小",
                    "Timer " + timerLabel(timer) + " 的 intervalTicks 小于 20，onTickActions 频繁执行可能影响服务器 tick。",
                    "TIMER",
                    timer.id,
                    "建议每秒级重复使用 20 ticks 或更大间隔；高频 timer 只保留轻量动作。",
                    "#/timers/" + timer.id
            ));
        }
        if (timer.mode == TimerMode.REPEAT && timer.maxRuns == 0 && timer.onCancelActions.isEmpty()) {
            issues.add(issue(
                    "timer-infinite-repeat-no-cancel-note:" + timer.id,
                    "WARNING",
                    "无限重复 Timer 需要明确取消路径",
                    "Timer " + timerLabel(timer) + " maxRuns=0，会一直重复直到 timer_cancel 或 WebAdmin 手动取消。",
                    "TIMER",
                    timer.id,
                    "确认已有外部 timer_cancel 动作或人工运维流程；否则可能长期占用运行态。",
                    "#/timers/" + timer.id
            ));
        }
        int active = TimerRuntimeService.status(server, timer, currentGameTime(server)).activeInstanceCount();
        if (active > 512) {
            issues.add(issue(
                    "timer-active-high:" + timer.id,
                    "WARNING",
                    "Timer active instance 数量过高",
                    "Timer " + timerLabel(timer) + " 当前 active instance 数量为 " + active + "，接近运行态上限。",
                    "TIMER",
                    timer.id,
                    "检查 PLAYER scope 是否被大量重复启动；必要时取消或调高间隔。",
                    "#/timers/" + timer.id
            ));
        }
    }

    private void inspectTimerActionReferences(MinecraftServer server, Map<String, TimerDefinition> timers, List<WebAdminDtos.DoctorIssueDto> issues) {
        for (SignalListenerData raw : SignalListenerStore.getSnapshot(server)) {
            SignalListenerData listener = raw.normalized();
            inspectActions(timers, issues, "SIGNAL_LISTENER_ACTION", listener.id(), listener.actions(), ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION, "#/listeners/" + listener.id());
        }
        for (SignalDeviceData rawDevice : SignalDeviceStore.getSnapshot(server)) {
            SignalDeviceData device = rawDevice.normalized();
            if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
                continue;
            }
            ActionRelayBlockEntity relay = SignalDeviceStore.getLoadedActionRelay(server, device);
            if (relay != null) {
                inspectActions(timers, issues, "ACTION_RELAY_ACTION", device.id(), relay.actions(), ConditionRuntimeTargetType.ACTION_RELAY_ACTION, "#/devices/" + device.id());
            }
        }
        for (RegionControllerData raw : RegionControllerStore.getSnapshot(server)) {
            RegionControllerData controller = raw.normalized();
            inspectActions(timers, issues, "REGION_ENTER_ACTION", controller.id(), controller.enterActions(), ConditionRuntimeTargetType.REGION_ENTER_ACTION, "#/region-controllers/" + controller.id());
            inspectActions(timers, issues, "REGION_EXIT_ACTION", controller.id(), controller.exitActions(), ConditionRuntimeTargetType.REGION_EXIT_ACTION, "#/region-controllers/" + controller.id());
            inspectActions(timers, issues, "REGION_STAY_ACTION", controller.id(), controller.stayActions(), ConditionRuntimeTargetType.REGION_STAY_ACTION, "#/region-controllers/" + controller.id());
        }
        for (TimerDefinition raw : timers.values()) {
            TimerDefinition timer = raw == null ? null : raw.normalized();
            if (timer == null) {
                continue;
            }
            inspectActions(timers, issues, "TIMER_ON_START_ACTION", timer.id, timer.onStartActions, ConditionRuntimeTargetType.TIMER_ON_START_ACTION, "#/timers/" + timer.id);
            inspectActions(timers, issues, "TIMER_ON_TICK_ACTION", timer.id, timer.onTickActions, ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION, "#/timers/" + timer.id);
            inspectActions(timers, issues, "TIMER_ON_COMPLETE_ACTION", timer.id, timer.onCompleteActions, ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION, "#/timers/" + timer.id);
            inspectActions(timers, issues, "TIMER_ON_CANCEL_ACTION", timer.id, timer.onCancelActions, ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION, "#/timers/" + timer.id);
        }
    }

    private void inspectActions(
            Map<String, TimerDefinition> timers,
            List<WebAdminDtos.DoctorIssueDto> issues,
            String ownerType,
            String ownerId,
            List<ActionConfig> actions,
            ConditionRuntimeTargetType runtimeTargetType,
            String navigation
    ) {
        for (int index = 0; index < (actions == null ? List.<ActionConfig>of() : actions).size(); index++) {
            ActionConfig action = actions.get(index);
            if (action == null || (action.type() != ActionType.TIMER_START && action.type() != ActionType.TIMER_CANCEL)) {
                continue;
            }
            String timerId = TimerStore.normalizeId(action.timerId());
            if (timerId.isBlank()) {
                issues.add(issue(
                        "timer-action-missing-id:" + ownerType + ":" + ownerId + ":" + index,
                        "ERROR",
                        "Timer action 缺少 timerId",
                        ownerType + " " + ownerId + " 的第 " + (index + 1) + " 条 Timer action 没有配置 timerId。",
                        ownerType,
                        ownerId,
                        "打开对应 Action 编辑器选择一个 Timer。",
                        navigation
                ));
                continue;
            }
            TimerDefinition target = timers == null ? null : timers.get(timerId);
            if (target == null) {
                issues.add(issue(
                        "timer-action-missing-target:" + ownerType + ":" + ownerId + ":" + index + ":" + timerId,
                        "ERROR",
                        "Timer action 引用不存在的 Timer",
                        ownerType + " " + ownerId + " 引用了不存在或已删除的 Timer：" + timerId + "。",
                        ownerType,
                        ownerId,
                        "重新绑定存在的 Timer，或删除该 Timer action。",
                        navigation
                ));
                continue;
            }
            TimerDefinition normalized = target.normalized();
            if (!normalized.enabled && action.type() == ActionType.TIMER_START) {
                issues.add(issue(
                        "timer-action-disabled-target:" + ownerType + ":" + ownerId + ":" + timerId,
                        "WARNING",
                        "timer_start 引用了停用 Timer",
                        ownerType + " " + ownerId + " 的 timer_start 指向已停用 Timer：" + timerId + "，运行时会失败。",
                        ownerType,
                        ownerId,
                        "启用 Timer 或改绑其它 Timer。",
                        navigation
                ));
            }
            TimerTargetMode targetMode = TimerTargetMode.parse(action.timerTargetMode());
            if (normalized.scopeMode == TimerScopeMode.PLAYER
                    && (targetMode == null || targetMode == TimerTargetMode.CONTEXT_PLAYER)
                    && !providesPlayerContext(runtimeTargetType)) {
                issues.add(issue(
                        "timer-action-player-context-missing:" + ownerType + ":" + ownerId + ":" + timerId,
                        "ERROR",
                        "PLAYER Timer 缺少触发玩家上下文",
                        ownerType + " " + ownerId + " 使用 context_player 启动/取消 PLAYER Timer，但该运行来源通常不提供玩家上下文。",
                        ownerType,
                        ownerId,
                        "改用 explicit_target，或只在 RegionController / 玩家触发链路中使用 context_player。",
                        navigation
                ));
            }
        }
    }

    private static boolean providesPlayerContext(ConditionRuntimeTargetType targetType) {
        return targetType == ConditionRuntimeTargetType.REGION_ENTER_ACTION
                || targetType == ConditionRuntimeTargetType.REGION_EXIT_ACTION
                || targetType == ConditionRuntimeTargetType.REGION_STAY_ACTION
                || targetType == ConditionRuntimeTargetType.TIMER_ON_START_ACTION
                || targetType == ConditionRuntimeTargetType.TIMER_ON_TICK_ACTION
                || targetType == ConditionRuntimeTargetType.TIMER_ON_COMPLETE_ACTION
                || targetType == ConditionRuntimeTargetType.TIMER_ON_CANCEL_ACTION;
    }

    private static WebAdminDtos.DoctorIssueDto issue(
            String id,
            String severity,
            String title,
            String message,
            String relatedType,
            String relatedId,
            String suggestion,
            String navigationTarget
    ) {
        return new WebAdminDtos.DoctorIssueDto(
                id,
                severity,
                title,
                message,
                relatedType,
                relatedId,
                relatedId,
                "",
                message,
                suggestion,
                Instant.now().toString(),
                navigationTarget
        );
    }

    private static String timerLabel(TimerDefinition timer) {
        return timer.displayName.isBlank() ? timer.id : timer.displayName + " (" + timer.id + ")";
    }

    private static long currentGameTime(MinecraftServer server) {
        return server == null || server.getOverworld() == null ? 0L : server.getOverworld().getTime();
    }
}
