package com.zcpu.tzzmod.webadmin.template;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.scheduler.TimerScopeMode;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.join.SignalJoinDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.signal.join.SignalJoinMode;
import com.zcpu.tzzmod.signal.join.SignalJoinResetPolicy;
import com.zcpu.tzzmod.signal.join.SignalJoinScopeMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WebAdminBuiltInTemplates {
    private WebAdminBuiltInTemplates() {
    }

    public static List<WebAdminTemplatePackage> list() {
        List<WebAdminTemplatePackage> templates = new ArrayList<>();
        templates.add(joinAllTemplate());
        templates.add(timerDelayTemplate());
        templates.add(listenerMessageTemplate());
        return templates.stream()
                .map(WebAdminTemplatePackage::normalized)
                .sorted(Comparator.comparing(template -> template.displayName))
                .toList();
    }

    public static WebAdminTemplatePackage find(String templateId) {
        String id = WebAdminTemplatePackage.normalizeId(templateId);
        return list().stream()
                .filter(template -> template.templateId.equals(id))
                .findFirst()
                .orElse(null);
    }

    private static WebAdminTemplatePackage joinAllTemplate() {
        WebAdminTemplatePackage template = base(
                "join_all_two_inputs",
                "两输入 Join 后输出频道",
                "创建 input_a + input_b -> Signal Join ALL -> output_c 的基础汇合结构。",
                "Join / Barrier"
        );
        template.resources.channels = List.of(
                channel("input_a", "输入 A", "Join 输入频道 A", "active-channel"),
                channel("input_b", "输入 B", "Join 输入频道 B", "active-channel"),
                channel("output_c", "输出 C", "Join 完成后的输出频道", "signal-join")
        );
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = "join.main";
        join.displayName = "两输入 Join";
        join.note = "内置模板：两个输入频道全部到达后输出。";
        join.enabled = true;
        join.inputChannels = List.of(
                new SignalJoinInputDefinition("input_a", "输入 A", "", 1),
                new SignalJoinInputDefinition("input_b", "输入 B", "", 1)
        );
        join.outputChannel = "output_c";
        join.mode = SignalJoinMode.ALL;
        join.threshold = 2;
        join.scopeMode = SignalJoinScopeMode.GLOBAL;
        join.resetPolicy = SignalJoinResetPolicy.RESET_AFTER_EMIT;
        WebAdminTemplatePackage.SignalJoinResource resource = new WebAdminTemplatePackage.SignalJoinResource();
        resource.id = "join.main";
        resource.definition = join;
        template.resources.signalJoins = List.of(resource);
        template.metadata.notes = List.of("落地为 SignalJoinDefinition 和频道 metadata，不创建世界实体。");
        return template.normalized();
    }

    private static WebAdminTemplatePackage timerDelayTemplate() {
        WebAdminTemplatePackage template = base(
                "timer_delay_with_start_listener",
                "频道启动 Delay Timer",
                "创建 start channel -> SignalListener(timer_start) -> Timer DELAY -> output channel 的可复用延迟结构。",
                "Timer / Countdown"
        );
        template.resources.channels = List.of(
                channel("start", "启动频道", "触发该频道后启动 Timer", "timer-start"),
                channel("timer_done", "Timer 完成频道", "Timer 完成后发出的下游频道", "timer")
        );
        TimerDefinition timer = new TimerDefinition();
        timer.id = "timer.delay";
        timer.displayName = "Delay Timer";
        timer.note = "内置模板：延迟 40 tick 后输出频道。";
        timer.enabled = true;
        timer.mode = TimerMode.DELAY;
        timer.scopeMode = TimerScopeMode.GLOBAL;
        timer.durationTicks = 40L;
        timer.intervalTicks = 0L;
        timer.maxRuns = 1;
        timer.startPolicy = TimerStartPolicy.RESTART;
        timer.outputChannel = "timer_done";
        WebAdminTemplatePackage.TimerResource timerResource = new WebAdminTemplatePackage.TimerResource();
        timerResource.id = "timer.delay";
        timerResource.definition = timer;
        SignalListenerData listener = new SignalListenerData(
                "listener.start_timer",
                "启动 Delay Timer",
                "start",
                true,
                0,
                "",
                List.of(ActionConfig.timerStart("timer.delay", TimerTargetMode.GLOBAL, "", TimerStartPolicy.RESTART, ""))
        ).normalized();
        WebAdminTemplatePackage.SignalListenerResource listenerResource = new WebAdminTemplatePackage.SignalListenerResource();
        listenerResource.id = "listener.start_timer";
        listenerResource.listener = listener;
        template.resources.timers = List.of(timerResource);
        template.resources.signalListeners = List.of(listenerResource);
        template.metadata.notes = List.of("Timer 不会在应用时启动；只有运行时收到启动频道后才会执行。");
        return template.normalized();
    }

    private static WebAdminTemplatePackage listenerMessageTemplate() {
        WebAdminTemplatePackage template = base(
                "listener_message_action",
                "频道触发消息动作",
                "创建 input channel -> SignalListener -> message action 的最小监听器动作结构。",
                "Listener / Action"
        );
        template.resources.channels = List.of(channel("input", "输入频道", "触发监听器的频道", "consumer-listener"));
        SignalListenerData listener = new SignalListenerData(
                "listener.message",
                "频道消息监听器",
                "input",
                true,
                0,
                "",
                List.of(new ActionConfig(com.zcpu.tzzmod.action.ActionType.MESSAGE, "模板消息：频道已触发", true, false, 0, false, ""))
        ).normalized();
        WebAdminTemplatePackage.SignalListenerResource resource = new WebAdminTemplatePackage.SignalListenerResource();
        resource.id = "listener.message";
        resource.listener = listener;
        template.resources.signalListeners = List.of(resource);
        template.metadata.notes = List.of("消息 Action 使用现有 ActionType.MESSAGE，不新增 ActionType。");
        return template.normalized();
    }

    private static WebAdminTemplatePackage base(String id, String name, String description, String category) {
        WebAdminTemplatePackage template = new WebAdminTemplatePackage();
        template.schema = WebAdminTemplatePackage.SCHEMA;
        template.templateId = id;
        template.displayName = name;
        template.description = description;
        template.category = category;
        template.version = 1;
        template.author = "TZZ WebAdmin";
        template.iconKey = "template-package";
        template.parameters = defaultParameters();
        template.metadata.source = "built_in";
        template.metadata.compatibility = List.of("8.15", "真实配置落地", "dry-run/apply");
        return template;
    }

    private static List<WebAdminTemplatePackage.Parameter> defaultParameters() {
        WebAdminTemplatePackage.Parameter prefix = new WebAdminTemplatePackage.Parameter();
        prefix.key = "prefix";
        prefix.displayName = "命名空间前缀";
        prefix.type = "text";
        prefix.required = true;
        prefix.description = "应用模板时加到 channel、Join、Timer、Listener ID 前，避免冲突。";
        WebAdminTemplatePackage.Parameter display = new WebAdminTemplatePackage.Parameter();
        display.key = "display_name_prefix";
        display.displayName = "显示名前缀";
        display.type = "text";
        display.required = false;
        display.description = "应用到中文显示名之前的前缀。";
        WebAdminTemplatePackage.Parameter root = new WebAdminTemplatePackage.Parameter();
        root.key = "root_channel";
        root.displayName = "Root 频道";
        root.type = "channel";
        root.required = false;
        root.description = "可选；用于把 start/input/root 类频道映射到已有或指定 root channel。";
        return List.of(prefix, display, root);
    }

    private static WebAdminTemplatePackage.ChannelResource channel(String id, String displayName, String note, String iconKey) {
        WebAdminTemplatePackage.ChannelResource channel = new WebAdminTemplatePackage.ChannelResource();
        channel.id = id;
        channel.displayName = displayName;
        channel.note = note;
        channel.iconKey = iconKey;
        return channel;
    }
}
