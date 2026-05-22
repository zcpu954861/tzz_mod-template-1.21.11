package com.zcpu.tzzmod.webadmin.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminHelpCatalogService {
    public Map<String, Object> catalog() {
        List<Map<String, Object>> topics = topics();
        List<Map<String, Object>> examples = examples();
        List<Map<String, Object>> troubleshooting = troubleshooting();
        List<Map<String, Object>> glossary = glossary();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", "8.20-pre9-stabilization");
        data.put("title", "WebAdmin Help / Example Center");
        data.put("readOnly", true);
        data.put("noWriteApi", true);
        data.put("copyOnly", true);
        data.put("worldScoped", false);
        data.put("message", "帮助中心是只读内置目录；不会写入用户笔记、收藏或配置。");
        data.put("categories", categories());
        data.put("topics", topics);
        data.put("examples", examples);
        data.put("troubleshooting", troubleshooting);
        data.put("glossary", glossary);
        data.put("featuredTopicIds", List.of(
                "getting-started.overview",
                "signalbridge.channel-basics",
                "logic-chain.viewer",
                "templates.prefab",
                "snapshot.rollback",
                "debugger.doctor-replay"
        ));
        data.put("deferredCapabilities", List.of(
                "GameController / MissionSystem / PhaseController deferred",
                "full Logic Chain Editor deferred",
                "Scratch editor deferred",
                "if / else runtime deferred",
                "old node move / delete / reorder deferred",
                "old action delete / reorder deferred",
                "world entity in-editor draft create and binding deferred",
                "placeholder binding apply deferred",
                "component export deferred",
                "ConditionGroup apply deferred",
                "StateVariable definition apply deferred",
                "external reference fail closed",
                "Git-like branch / merge / rebase deferred；Snapshot 配置回滚已实现且仅限 allowlist 配置。",
                "raw JSON editor deferred",
                "new runtime integration deferred",
                "new write API for notes / favorites deferred"
        ));
        data.put("accuracyNotes", List.of(
                "ConditionEngine 只判断，不写状态，不发信号，不执行动作。",
                "SignalBridge 是事件总线，不是状态数据库。",
                "StateVariable 保存状态。",
                "Logic Chain Viewer 的顺序是可视化顺序，不是全局执行顺序。",
                "Logic Chain Editor 保存 typed config，不保存假图。",
                "Snapshot / Rollback 是 WebAdmin 配置恢复能力，不是 Git 分支系统或世界备份。"
        ));
        data.put("counts", map(
                "topics", topics.size(),
                "examples", examples.size(),
                "troubleshooting", troubleshooting.size(),
                "glossary", glossary.size()
        ));
        return data;
    }

    private static List<Map<String, Object>> categories() {
        return List.of(
                category("getting-started", "入门", "先理解 WebAdmin 怎么组织事件、动作和诊断。"),
                category("signal", "Signal / 频道", "SignalBridge、频道、监听器、接收器和发射源。"),
                category("device", "设备与触发", "VBD、ActionRelay、Region、SignalReceiver 等世界实体引用。"),
                category("action", "Action / 动作", "ActionConfig 和受控动作执行。"),
                category("condition", "Condition / 条件", "ConditionGroup、runtime gate、调试和 replay。"),
                category("state", "StateVariable / 状态变量", "GLOBAL / PLAYER 状态读取和受控写入。"),
                category("join-timer", "Join / Timer", "多输入汇合、延迟、倒计时和重复计时。"),
                category("logic-chain", "Logic Chain / 逻辑链", "逻辑链查看器和受控编辑入口。"),
                category("template", "Templates / 模板", "模板中心、prefab、导入导出和安全 apply。"),
                category("snapshot", "Snapshot / 配置时间轴", "保存点、自动快照、diff、dry-run 和配置回滚。"),
                category("diagnostics", "Debugger / Doctor", "排错、诊断、历史和只读 replay。")
        );
    }

    private static List<Map<String, Object>> topics() {
        List<Map<String, Object>> topics = new ArrayList<>();
        topics.add(topic(
                "getting-started.overview",
                "从帮助中心开始",
                "把 WebAdmin 看成小游戏 IDE 的配置面板：先理解频道，再连接动作、条件、状态和模板。",
                "getting-started",
                List.of("入门", "新手", "IDE"),
                sections(
                        section("这是什么", "帮助中心把模块说明、示例、排错和术语放在同一个入口。", "默认是基础模式，只显示最短路径。"),
                        section("最小路线", "先看频道 / SignalBridge，再看监听器和动作。", "需要组合多个信号时再看 Join / Timer。", "配置失败时看 Doctor 和条件调试。"),
                        section("常见误区", "帮助中心不会自动创建配置。", "示例是文档示例；只有模板中心的 apply 会走现有写入流程。")
                ),
                sections(
                        section("边界", "帮助中心只新增只读文档目录和页面级帮助入口。", "不新增 runtime 语义，不新增 ActionType / ConditionNodeType。"),
                        section("未来方向", "用户笔记、收藏、交互式教程和上下文帮助会作为后续能力推进。")
                ),
                List.of("example.template-join-timer-listener"),
                List.of("trouble.logic-chain-one-entry-many-channels"),
                List.of("channel", "logic-chain", "doctor"),
                routes(link("帮助中心", "#/help"), link("模板中心", "#/templates")),
                List.of("signalbridge.channel-basics", "templates.prefab")
        ));
        topics.add(topic(
                "signalbridge.channel-basics",
                "频道 / SignalBridge",
                "频道是 SignalBridge 中被命名的事件通道，生产者 emit，消费者监听并执行后续配置。",
                "signal",
                List.of("SignalBridge", "Channel", "频道", "事件总线"),
                sections(
                        section("这是什么", "频道像一条命名事件线。设备、监听器、Join、Timer 或 Action 都可以围绕频道工作。"),
                        section("最简单怎么用", "创建或绑定一个会发 signal 的对象。", "在虚拟监听器里监听同一个频道。", "给监听器添加 message / signal / state_variable / timer action。"),
                        section("常见误区", "SignalBridge 是事件总线，不是状态数据库。", "频道 metadata 只改变 WebAdmin 展示，不会自动创建消费者。")
                ),
                sections(
                        section("运行语义", "SignalBridge 接受事件后会进入 history、receiver、ActionRelay、SignalListener、Join 等现有消费者路径。", "同一频道下多个消费者并行展示；不要把表格顺序理解为全局执行顺序。"),
                        section("诊断路径", "无消费者频道先看频道详情和 Doctor。", "有事件但动作没执行时看 Listener、Action gate 和条件调试。")
                ),
                List.of("example.signal-no-consumer", "example.listener-message"),
                List.of("trouble.signal-no-consumer", "trouble.listener-action-not-executed"),
                List.of("channel", "signalbridge", "logic-chain"),
                routes(link("SignalBridge", "#/signals"), link("History", "#/history"), link("Doctor", "#/doctor")),
                List.of("signalbridge.listener-flow", "logic-chain.viewer")
        ));
        topics.add(topic(
                "signalbridge.listener-flow",
                "SignalListener / 信号监听器",
                "SignalListener 监听一个频道，并按配置执行 ActionConfig 列表。",
                "signal",
                List.of("SignalListener", "listener", "虚拟监听器", "ActionConfig"),
                sections(
                        section("这是什么", "虚拟监听器不依赖世界方块；它监听指定频道。"),
                        section("最简单怎么用", "在信号监听器页面创建监听器。", "填写 channel、enabled 和 cooldown。", "添加一条 message 或 signal 动作。"),
                        section("常见误区", "enabled=false 或 cooldown 仍可能让你以为没有触发。", "空 action list 不会产生可见效果。")
                ),
                sections(
                        section("Gate", "监听器级 conditionGroupId 是列表级 runtime gate。", "单条 Action 也可以有 action gate；gate false 只跳过对应层级，不改其它 runtime。"),
                        section("受控编辑", "已有 SignalListener 的基础配置可受控编辑；同 index Action 替换仅限 SignalListener / Timer action bucket。", "ActionRelay / Region 旧 Action 仍只读或只支持追加；旧 action 删除 / 重排仍 deferred。")
                ),
                List.of("example.listener-message", "example.listener-state-variable"),
                List.of("trouble.listener-action-not-executed", "trouble.condition-not-selectable"),
                List.of("signal-listener", "action-config", "runtime-gate"),
                routes(link("监听器", "#/listeners"), link("条件调试", "#/condition-debugger")),
                List.of("action.config-basics", "condition.group-basics")
        ));
        topics.add(topic(
                "action.config-basics",
                "Action / ActionConfig",
                "ActionConfig 是动作系统的 typed 配置，当前用于命令、消息、音效、发信号、状态变量写入和 Timer 控制。",
                "action",
                List.of("Action", "ActionConfig", "state_variable", "timer_start"),
                sections(
                        section("这是什么", "动作是触发后真正产生效果的配置。", "常见动作包括 message、signal、state_variable、timer_start 和 timer_cancel。"),
                        section("最小例子", "监听频道 mission.start。", "添加 message 动作。", "触发频道后玩家看到消息。"),
                        section("常见误区", "Action 列表为空时，信号已经触发但不会有后续效果。")
                ),
                sections(
                        section("边界", "帮助中心不新增 ActionType。", "旧 action 任意删除、移动、重排仍 deferred。"),
                        section("Gate", "单条 action gate false 时只跳过当前 action，不会改写其它 action 或父列表。")
                ),
                List.of("example.listener-message", "example.listener-state-variable", "example.condition-controls-action"),
                List.of("trouble.listener-action-not-executed", "trouble.state-variable-action-failed"),
                List.of("action", "action-config", "action-gate"),
                routes(link("动作列表", "#/actions"), link("监听器", "#/listeners")),
                List.of("condition.group-basics", "state-variable.basics")
        ));
        topics.add(topic(
                "condition.group-basics",
                "ConditionGroup / 条件组",
                "ConditionGroup 是只读判断树，可绑定到 runtime gate 控制某个触发或 action 是否继续。",
                "condition",
                List.of("ConditionEngine", "ConditionGroup", "runtime gate", "conditionGroupId"),
                sections(
                        section("这是什么", "条件组只回答通过或不通过。", "它不会自己写状态、发信号或执行动作。"),
                        section("最简单怎么用", "创建条件组。", "在支持的模块里选择该 conditionGroupId。", "触发后到条件调试查看通过或阻断原因。"),
                        section("常见误区", "没有配置 conditionGroupId 时不会评估，也不会产生调试历史。")
                ),
                sections(
                        section("运行语义", "ConditionEngine 只判断，不写状态，不发信号，不执行动作。", "未配置 gate 时旧逻辑完全不变，不读取 condition group store。"),
                        section("兼容性", "选择器只显示与目标 profile 兼容的条件组；后端也会拒绝 incompatible binding。")
                ),
                List.of("example.condition-controls-action"),
                List.of("trouble.condition-not-selectable", "trouble.blank-gate-no-history"),
                List.of("condition-group", "runtime-gate", "debugger"),
                routes(link("条件组", "#/condition-groups"), link("条件调试", "#/condition-debugger")),
                List.of("debugger.doctor-replay", "state-variable.basics")
        ));
        topics.add(topic(
                "state-variable.basics",
                "StateVariable / 状态变量",
                "状态变量保存 GLOBAL / PLAYER 范围的布尔、整数和文本值，供条件读取，也可由受控状态动作写入。",
                "state",
                List.of("StateVariable", "GLOBAL", "PLAYER", "state action"),
                sections(
                        section("这是什么", "StateVariable 保存状态。", "条件可以读取它，state_variable action 可以受控写入它。"),
                        section("最简单怎么用", "给监听器添加 state_variable 动作。", "触发后到状态变量页面确认当前值。", "再用条件组读取该变量。"),
                        section("常见误区", "状态变量页面是只读结果页，不是变量定义管理器。")
                ),
                sections(
                        section("边界", "当前支持 GLOBAL / PLAYER scope，不支持 TEAM / REGION / DEVICE / GAME scope。", "变量变化不会自动 emit signal。"),
                        section("诊断路径", "PLAYER scope 需要明确玩家上下文或目标 ID；无玩家上下文会失败并给中文原因。")
                ),
                List.of("example.listener-state-variable"),
                List.of("trouble.state-variable-action-failed"),
                List.of("state-variable", "state-action"),
                routes(link("状态变量", "#/state-variables"), link("条件组", "#/condition-groups")),
                List.of("condition.group-basics", "action.config-basics")
        ));
        topics.add(topic(
                "signal-join.basics",
                "Signal Join / 汇合",
                "Signal Join 观察多个输入频道，满足 ALL / ANY_N / COUNT 后发出一个输出频道。",
                "join-timer",
                List.of("Signal Join", "Barrier", "Aggregator", "Join"),
                sections(
                        section("这是什么", "当多个输入都到达后，再统一发出输出 signal。"),
                        section("最简单怎么用", "配置两个 input channel。", "选择 output channel。", "模式 ALL 表示所有输入都到达后输出。"),
                        section("常见误区", "Join 不会阻断原始输入 signal。", "没有 output channel 或下游消费者时，看起来会像没有效果。")
                ),
                sections(
                        section("运行语义", "Join 是 SignalBridge 的 passive observer。", "pending / latched state 保存在内存中，重启后清空。"),
                        section("编辑边界", "当前可受控编辑输入 / 输出和基础字段。", "旧节点任意移动、删除、重排仍 deferred。")
                ),
                List.of("example.join-two-inputs", "example.template-join-timer-listener"),
                List.of("trouble.join-no-output"),
                List.of("join", "barrier", "aggregator"),
                routes(link("信号汇合", "#/signal-joins"), link("逻辑链", "#/logic-chains")),
                List.of("logic-chain.viewer", "templates.prefab")
        ));
        topics.add(topic(
                "timer.delay",
                "Timer / 调度器",
                "Timer 提供 DELAY / COUNTDOWN / REPEAT 计时，可由 action 启动或手动操作，完成后可发频道或执行动作。",
                "join-timer",
                List.of("Timer", "Scheduler", "delay", "timer_start"),
                sections(
                        section("这是什么", "Timer 用来延迟、倒计时或重复执行。"),
                        section("最简单怎么用", "创建 DELAY Timer。", "配置 duration 和 outputChannel。", "用 timer_start action 启动。"),
                        section("常见误区", "应用模板不会自动启动 Timer。", "outputChannel 可选；没有 outputChannel 但有 onCompleteActions 也可以工作。")
                ),
                sections(
                        section("运行语义", "Timer runtime 实例保存在内存中。", "Timer action list 继续走 ActionConfig 和单条 action gate。"),
                        section("诊断路径", "检查 disabled、timerId 缺失、PLAYER context、无输出/动作、REPEAT 高频等 Doctor 提示。")
                ),
                List.of("example.timer-delay-channel", "example.template-join-timer-listener"),
                List.of("trouble.timer-not-triggered"),
                List.of("timer", "scheduler"),
                routes(link("计时器", "#/timers"), link("History", "#/history")),
                List.of("action.config-basics", "logic-chain.viewer")
        ));
        topics.add(topic(
                "logic-chain.viewer",
                "Logic Chain Viewer / 逻辑链查看器",
                "逻辑链查看器按当前真实配置推导 component graph，帮助你理解频道、Join、Timer、监听器和动作的关系。",
                "logic-chain",
                List.of("Logic Chain", "Viewer", "component graph"),
                sections(
                        section("这是什么", "逻辑链是一组强关联组件，不等于单个频道。", "列表中一个入口可能包含多个频道。"),
                        section("最简单怎么用", "从频道详情或逻辑链列表进入。", "切换焦点频道查看同一 component 的不同入口。"),
                        section("常见误区", "画布顺序是可视化顺序，不是全局执行顺序。")
                ),
                sections(
                        section("图谱来源", "Viewer 从 SignalBridge、Join、Timer、Action 和 WebAdmin metadata 推导，不保存 runtime 图结构。"),
                        section("边界", "不保证全局唯一拓扑排序。", "不改变 SignalBridge / ActionEngine / Timer / Join runtime 语义。")
                ),
                List.of("example.editor-draft-join-timer", "example.signal-no-consumer"),
                List.of("trouble.logic-chain-one-entry-many-channels", "trouble.node-hidden-missing"),
                List.of("logic-chain", "focus-channel", "associated-component"),
                routes(link("逻辑链", "#/logic-chains"), link("Doctor", "#/doctor")),
                List.of("logic-chain.editor-draft", "signalbridge.channel-basics")
        ));
        topics.add(topic(
                "logic-chain.editor-draft",
                "Logic Chain Editor / 受控编辑",
                "编辑器在 Viewer 画布内做受控新增和已有节点局部维护，保存真实 typed config，不保存假图。",
                "logic-chain",
                List.of("Logic Chain Editor", "draft", "edit lock"),
                sections(
                        section("这是什么", "它是受控编辑入口，不是完整图编程器。"),
                        section("当前能做", "新增 Join / Timer / channel endpoint。", "受控编辑已有 Channel metadata、Join、Timer、SignalListener 基础配置；同 index Action 仅限 SignalListener / Timer action bucket。"),
                        section("常见误区", "旧节点不能任意移动、删除、重排。", "旧 action 不能任意删除、重排。")
                ),
                sections(
                        section("保存语义", "保存写入真实配置服务：SignalJoin、Timer、SignalListener、Channel metadata、ActionConfig。", "后端 validation、edit lock、expectedFingerprint 都必须通过。"),
                        section("Deferred", "full Logic Chain Editor、Scratch editor、if / else runtime、世界实体草稿创建和回滚都 deferred。")
                ),
                List.of("example.editor-draft-join-timer"),
                List.of("trouble.editor-save-failed", "trouble.readonly-nodes"),
                List.of("edit-lock", "fingerprint", "placeholder"),
                routes(link("逻辑链", "#/logic-chains")),
                List.of("templates.prefab", "debugger.doctor-replay")
        ));
        topics.add(topic(
                "templates.prefab",
                "Templates / Prefab 模板中心",
                "模板中心提供内置和用户模板的导入、导出、dry-run 和安全 apply，用来在模板中心 apply 创建低层配置组合。",
                "template",
                List.of("Template", "Prefab", "dry-run", "apply"),
                sections(
                        section("这是什么", "模板是可复用配置包。", "导入只保存用户模板；apply 才会尝试写真实配置。"),
                        section("最简单怎么用", "打开模板中心。", "选择内置模板。", "先预览，再确认应用。"),
                        section("常见误区", "导入 JSON 不等于应用。", "模板不会自动复制世界方块、区域或设备。")
                ),
                sections(
                        section("安全边界", "placeholder binding apply deferred。", "ConditionGroup apply deferred。", "StateVariable definition apply deferred。", "component export deferred。", "external reference fail closed。"),
                        section("写入顺序", "apply 走现有权限、CSRF、edit lock、expectedFingerprint、audit 和 realtime。")
                ),
                List.of("example.template-join-timer-listener", "example.template-import-vs-apply"),
                List.of("trouble.template-apply-conflict", "trouble.import-json-no-effect"),
                List.of("template", "prefab", "dry-run", "placeholder"),
                routes(link("模板中心", "#/templates"), link("逻辑链", "#/logic-chains")),
                List.of("logic-chain.viewer", "signal-join.basics", "timer.delay", "snapshot.rollback")
        ));
        topics.add(topic(
                "snapshot.rollback",
                "Snapshot / Rollback 配置时间轴",
                "配置时间轴保存 WebAdmin allowlist 配置快照，支持手动保存点、写入前自动快照、只读 diff、dry-run 和确认后配置回滚。",
                "snapshot",
                List.of("Snapshot", "Rollback", "配置时间轴", "保存点", "pre_rollback", "operationDiff"),
                sections(
                        section("这是什么", "Snapshot 保存的是 WebAdmin 配置文件，不是世界备份。", "手动保存点用于明确标记；自动快照在关键写操作前创建。"),
                        section("最简单怎么用", "打开配置时间轴。", "先查看“本次操作变化”和“与上一保存点变化”。", "需要回滚时先执行 dry-run，再确认写入。"),
                        section("常见误区", "自动快照是写入前保护点；真正的本次写入变化通过 operationDiff 回填显示。", "筛选隐藏当前选中快照时，页面会提示你清空筛选。")
                ),
                sections(
                        section("恢复语义", "Rollback 只恢复 allowlist 配置文件，并在 apply 前创建 pre_rollback 保护点。", "pre_rollback 的本次操作变化显示回滚本身创建、更新或删除了哪些资源。"),
                        section("完整性与安全", "bad manifest / bad package 进入 degraded 状态并 fail closed；原始解析异常只写服务端日志。", "rollback 需要权限、CSRF / same-origin、edit lock、manifest fingerprint 和 dry-run fingerprint。"),
                        section("边界", "自动快照保留最近 200 个，manual 和 pre_rollback 受保护。", "不备份 runtime history、Timer active state、Join pending state、玩家背包、世界实体，也不实现 Git branch / merge / rebase。")
                ),
                List.of("example.snapshot-dry-run-rollback"),
                List.of("trouble.snapshot-degraded", "trouble.rollback-operation-diff", "trouble.snapshot-retention"),
                List.of("snapshot", "rollback", "pre-rollback", "operation-diff"),
                routes(link("配置时间轴", "#/snapshots"), link("Doctor", "#/doctor")),
                List.of("templates.prefab", "debugger.doctor-replay")
        ));
        topics.add(topic(
                "debugger.doctor-replay",
                "Debugger / Doctor / Replay",
                "Doctor 给出配置健康问题；条件调试器记录 runtime gate 判断；Replay 只读复算历史 snapshot。",
                "diagnostics",
                List.of("Doctor", "Debugger", "Replay", "History"),
                sections(
                        section("这是什么", "Doctor 帮你找到缺失、禁用、无输出、冲突和不兼容问题。", "条件调试看 gate 为什么通过或阻断。"),
                        section("最简单怎么用", "先看 Doctor 严重问题。", "再打开条件调试查看具体 gate。", "必要时用 Replay 复查历史 snapshot。"),
                        section("常见误区", "Replay 不执行 action、不发 signal、不读取 live world。")
                ),
                sections(
                        section("边界", "Doctor 不自动修复。", "Gate history 是内存 recent history。", "未配置 gate 不会产生 debugger 记录。"),
                        section("排错路径", "Signal 无下游看频道和 Doctor。", "Timer 不触发看 Timer Doctor。", "模板失败看 dry-run conflicts。")
                ),
                List.of("example.condition-controls-action", "example.signal-no-consumer"),
                List.of("trouble.blank-gate-no-history", "trouble.timer-not-triggered", "trouble.template-apply-conflict", "trouble.snapshot-degraded"),
                List.of("doctor", "debugger", "replay", "snapshot"),
                routes(link("Doctor", "#/doctor"), link("条件调试", "#/condition-debugger"), link("History", "#/history"), link("配置时间轴", "#/snapshots")),
                List.of("condition.group-basics", "logic-chain.viewer", "snapshot.rollback")
        ));
        topics.add(topic(
                "device-trigger.references",
                "设备、VBD、ActionRelay、Region、SignalReceiver",
                "这些模块连接世界触发和信号系统；帮助中心只做说明，不新增世界实体创建能力。",
                "device",
                List.of("VBD", "ActionRelay", "Region", "SignalReceiver", "world entity"),
                sections(
                        section("这是什么", "VBD 可以把虚拟方块交互、红石或容器事件转换成 signal。", "ActionRelay block 可保存动作列表。", "SignalReceiver 可输出红石脉冲。"),
                        section("最简单怎么用", "先从设备页确认绑定和频道。", "再从频道详情看是否有消费者。"),
                        section("常见误区", "世界实体在 Logic Chain Editor 内创建 / 绑定仍是后续能力。")
                ),
                sections(
                        section("边界", "VBD / Region / ActionRelay block / SignalReceiver 等世界实体节点在编辑器内仍为只读引用。", "帮助中心不启动 Minecraft、不跑 scenario。")
                ),
                List.of("example.signal-no-consumer"),
                List.of("trouble.readonly-nodes"),
                List.of("vbd", "action-relay", "signal-receiver"),
                routes(link("设备", "#/devices"), link("虚拟方块设备", "#/virtual-block-devices")),
                List.of("signalbridge.channel-basics", "logic-chain.viewer")
        ));
        topics.add(topic(
                "region.controller",
                "Region / 区域控制器",
                "区域控制器把进入、离开、停留事件接入 action 和 signal 工作流。",
                "device",
                List.of("Region", "RegionController", "enter", "exit", "stay"),
                sections(
                        section("这是什么", "区域控制器把玩家 enter / exit / stay 转换为可执行动作列表。"),
                        section("最简单怎么用", "先确认区域存在。", "查看区域控制器绑定和动作数量。", "用 Doctor 看缺失区域、禁用或 action 问题。")
                ),
                sections(
                        section("边界", "帮助中心不新增区域运行时能力。", "RegionController 配置写入沿用现有 WebAdmin 写入基础。", "Region 实体新增、编辑、删除、定位、导入和导出仍不可用 / deferred。")
                ),
                List.of("example.condition-controls-action"),
                List.of("trouble.condition-not-selectable"),
                List.of("region", "runtime-gate"),
                routes(link("区域", "#/regions"), link("区域控制器", "#/region-controllers")),
                List.of("condition.group-basics", "action.config-basics")
        ));
        for (Map<String, Object> topic : topics) {
            topic.put("searchText", searchText(topic));
        }
        return topics;
    }

    private static List<Map<String, Object>> examples() {
        List<Map<String, Object>> examples = new ArrayList<>();
        examples.add(example("example.join-two-inputs", "两个输入频道汇合后输出", "两个独立按钮或事件都完成后再发出 mission.ready。", List.of("Signal Join", "SignalBridge", "Logic Chain"), List.of("创建 input.a 和 input.b。", "创建 ALL 模式 Join，outputChannel 设为 mission.ready。", "给 mission.ready 配置监听器或下游逻辑。"), List.of("Join 是 passive observer，不阻断 input.a / input.b 原始消费者。"), List.of("Join 没有 outputChannel。", "outputChannel 没有消费者。"), routes(link("信号汇合", "#/signal-joins"), link("内置模板", "#/templates/built_in%3Ajoin_all_two_inputs")), "join_all_two_inputs", List.of("signal-join.basics")));
        examples.add(example("example.timer-delay-channel", "Timer 延迟后触发频道", "收到 mission.start 后等待几秒再发 mission.timeout。", List.of("Timer", "SignalListener", "timer_start action"), List.of("创建 DELAY Timer。", "设置 durationTicks 和 outputChannel。", "监听 mission.start，添加 timer_start action。"), List.of("模板 apply 只创建配置，不会自动启动 Timer。"), List.of("Timer disabled。", "timer_start 引用 ID 错误。"), routes(link("计时器", "#/timers"), link("内置模板", "#/templates/built_in%3Atimer_delay_with_start_listener")), "timer_delay_with_start_listener", List.of("timer.delay")));
        examples.add(example("example.listener-message", "监听频道后发送消息", "收到 signal 后向触发玩家发送提示；无玩家上下文时广播给在线玩家。", List.of("SignalListener", "message action"), List.of("创建监听器并填写 channel。", "添加 message action。", "触发频道后查看 History 和消息效果。"), List.of("message action 仍走 ActionEngine 和 gate；operator 通知使用独立 notifyOps。"), List.of("监听器 disabled。", "action list 为空。"), routes(link("监听器", "#/listeners"), link("内置模板", "#/templates/built_in%3Alistener_message_action")), "listener_message_action", List.of("signalbridge.listener-flow")));
        examples.add(example("example.listener-state-variable", "监听器写入 StateVariable", "把某个事件记录成 GLOBAL 或 PLAYER 状态。", List.of("SignalListener", "state_variable action", "StateVariable"), List.of("在监听器动作里选择 state_variable。", "设置 scope、key、operation 和目标模式。", "触发后到状态变量页面确认值。"), List.of("PLAYER scope 需要玩家上下文或明确 targetId。"), List.of("key 为空。", "类型不匹配。", "无玩家上下文。"), routes(link("监听器", "#/listeners"), link("状态变量", "#/state-variables")), "", List.of("state-variable.basics")));
        examples.add(example("example.condition-controls-action", "用 ConditionGroup 控制 action", "只有条件通过时才执行某条 action。", List.of("ConditionGroup", "Action gate", "Debugger"), List.of("创建条件组。", "在 action 的 conditionGroupId 选择该组。", "触发后到条件调试查看 ALLOWED / BLOCKED。"), List.of("未配置 conditionGroupId 时不会读取 store，也不会产生 gate history。"), List.of("条件组不兼容。", "条件组 disabled 或 invalid。"), routes(link("条件组", "#/condition-groups"), link("条件调试", "#/condition-debugger")), "", List.of("condition.group-basics")));
        examples.add(example("example.template-join-timer-listener", "用 Template apply 创建 Join/Timer/Listener 组合", "从内置 prefab 开始，减少重复配置。", List.of("Template Center", "Signal Join", "Timer", "SignalListener"), List.of("打开模板中心。", "选择内置模板并 dry-run。", "确认前缀和冲突后 apply。", "到逻辑链查看生成 component。"), List.of("apply 写真实配置；import 只保存模板 JSON。"), List.of("前缀冲突。", "placeholder 缺失。", "deferred resource 阻断。"), routes(link("模板中心", "#/templates"), link("逻辑链", "#/logic-chains")), "", List.of("templates.prefab")));
        examples.add(example("example.signal-no-consumer", "Signal 发出但没有后续动作", "确认 signal 已进 history，但没有消费者或 action。", List.of("SignalBridge", "Doctor", "History"), List.of("打开 History 确认频道触发。", "打开频道详情看消费者数量。", "打开 Doctor 看无消费者或 disabled 提示。"), List.of("频道事件存在不代表后续一定有 action。"), List.of("频道名拼写不同。", "消费者 disabled。"), routes(link("SignalBridge", "#/signals"), link("Doctor", "#/doctor"), link("History", "#/history")), "", List.of("signalbridge.channel-basics")));
        examples.add(example("example.template-import-vs-apply", "Template import 与 apply 的区别", "导入 JSON 后为什么世界配置没有变化。", List.of("Template Center", "dry-run", "apply"), List.of("导入 JSON 只进入用户模板库。", "选择模板，执行预览。", "确认 apply 后才写真实配置。"), List.of("导入和 apply 使用不同权限/锁流程。"), List.of("只导入未应用。", "apply dry-run 有冲突。"), routes(link("模板中心", "#/templates")), "", List.of("templates.prefab")));
        examples.add(example("example.editor-draft-join-timer", "Logic Chain 里新增 Join / Timer 草稿", "在当前图谱中放置新 Join 或 Timer 并保存到 typed config。", List.of("Logic Chain Editor", "Signal Join", "Timer"), List.of("进入逻辑链详情。", "进入编辑模式并获取锁。", "新增 Join 或 Timer，连接合法频道。", "保存并查看真实配置。"), List.of("草稿连线保存为 typed config，不保存假图结构。"), List.of("编辑锁丢失。", "fingerprint 冲突。", "旧节点移动/删除被拒绝。"), routes(link("逻辑链", "#/logic-chains")), "", List.of("logic-chain.editor-draft")));
        examples.add(example("example.snapshot-dry-run-rollback", "用配置时间轴 dry-run 回滚前先确认变化", "需要撤回一次配置变更时，先看保护点、operation diff 和 rollback dry-run 计划。", List.of("Snapshot", "Rollback", "Doctor"), List.of("打开配置时间轴。", "选择目标保存点并查看本次操作变化。", "点击回滚 dry-run，确认将新增、覆盖或删除的配置文件。", "确认前先检查是否已有 pre_rollback 保护点和 Doctor degraded 警告。"), List.of("rollback apply 会先创建 pre_rollback 保护点；它不是 Git 分支或世界备份。"), List.of("manifest/package degraded。", "选择了被筛选隐藏的保存点。", "dry-run fingerprint 过期。"), routes(link("配置时间轴", "#/snapshots"), link("Doctor", "#/doctor")), "", List.of("snapshot.rollback")));
        for (Map<String, Object> example : examples) {
            example.put("kind", "example");
            example.put("readOnlyExample", true);
            example.put("searchText", searchText(example));
        }
        return examples;
    }

    private static List<Map<String, Object>> troubleshooting() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(trouble("trouble.condition-not-selectable", "为什么条件组不可选？", List.of("目标 profile 不兼容。", "条件组 disabled / invalid。"), List.of("打开条件组详情看 validation。", "确认目标是 listener、action、timer 还是 region。"), List.of("换用兼容条件组。", "修复 invalid node。"), "后端也会拒绝 incompatible binding，不能只靠前端隐藏。", routes(link("条件组", "#/condition-groups"), link("条件调试", "#/condition-debugger"))));
        items.add(trouble("trouble.join-no-output", "为什么 Join 没有输出？", List.of("输入未全部满足。", "outputChannel 为空或无消费者。", "Join disabled。"), List.of("看 Join runtime status。", "看 outputChannel 的频道详情。"), List.of("补齐输入。", "配置输出频道和下游消费者。"), "Join 不阻断原始 input signal；它只在满足条件后 emit output。", routes(link("信号汇合", "#/signal-joins"), link("Doctor", "#/doctor"))));
        items.add(trouble("trouble.timer-not-triggered", "为什么 Timer 没触发？", List.of("Timer disabled。", "timer_start 引用不存在。", "PLAYER scope 缺上下文。", "没有 outputChannel 或动作。"), List.of("看 Timer Doctor。", "看启动 action 的执行记录。"), List.of("修正 timerId。", "补输出或 onCompleteActions。"), "Timer runtime 实例是内存态；模板 apply 不会自动启动。", routes(link("计时器", "#/timers"), link("Doctor", "#/doctor"), link("History", "#/history"))));
        items.add(trouble("trouble.listener-action-not-executed", "为什么 Listener 没执行 action？", List.of("监听器 disabled。", "channel 不一致。", "cooldown 中。", "列表 gate 或 action gate 阻断。"), List.of("看 listener 详情。", "看 condition debugger。", "看 History。"), List.of("修正 channel。", "启用 listener/action。", "检查 gate。"), "action gate false 只跳过当前 action，不会改写其它 action。", routes(link("监听器", "#/listeners"), link("条件调试", "#/condition-debugger"))));
        items.add(trouble("trouble.template-apply-conflict", "为什么模板 apply 冲突？", List.of("前缀已存在。", "fingerprint 过期。", "edit lock 丢失。", "deferred resource 或 placeholder 缺失。"), List.of("看 dry-run conflicts。", "刷新模板中心。"), List.of("换前缀。", "重新预览。", "处理缺失 placeholder。"), "Template apply fail closed，不覆盖既有资源。", routes(link("模板中心", "#/templates"))));
        items.add(trouble("trouble.logic-chain-one-entry-many-channels", "为什么逻辑链列表只有一个入口但有很多频道？", List.of("列表按 connected component 聚合。", "一个 component 可以包含多个 channel。"), List.of("打开详情页。", "切换焦点频道。"), List.of("用详情页 focus channel 查看不同入口。"), "Logic Chain 不等于单个 channel；Viewer 只做可视化组织。", routes(link("逻辑链", "#/logic-chains"))));
        items.add(trouble("trouble.editor-save-failed", "为什么编辑器保存失败？", List.of("edit lock 丢失。", "expectedFingerprint 冲突。", "草稿缺必要连线。", "旧节点/旧 action 越界操作。"), List.of("看保存错误列表。", "确认锁状态和草稿连线。"), List.of("刷新后重试。", "只保存支持的 typed config。"), "保存先统一 validation，再写真实服务；不会保存假图。", routes(link("逻辑链", "#/logic-chains"))));
        items.add(trouble("trouble.node-hidden-missing", "为什么看不到某个节点？", List.of("当前 focusChannel 只影响高亮和入口。", "节点被筛选或折叠。", "脱钩结构在退出连接模式后裁剪。"), List.of("清空节点类型筛选。", "切换 focus channel。", "看 Doctor。"), List.of("放宽筛选。", "从相关频道重新进入。"), "Viewer 按 component-aware traversal 展示，弱引用不会把不相关 component 合并。", routes(link("逻辑链", "#/logic-chains"), link("Doctor", "#/doctor"))));
        items.add(trouble("trouble.readonly-nodes", "为什么某些节点在编辑器里只能查看不能创建？", List.of("VBD、Region、ActionRelay block、SignalReceiver 是世界实体引用。", "当前编辑器没有安全的世界实体草稿创建/绑定。"), List.of("看节点详情的 readonly 标记。"), List.of("通过已有页面或游戏内流程管理世界实体。"), "world entity in-editor draft create documented as deferred。", routes(link("设备", "#/devices"), link("逻辑链", "#/logic-chains"))));
        items.add(trouble("trouble.import-json-no-effect", "导入 JSON 后为什么没有生效？", List.of("import 只保存为用户模板。", "没有执行 apply。"), List.of("打开模板详情。", "检查是否有 apply 预览记录。"), List.of("执行 dry-run，然后确认 apply。"), "导入不发 signal、不执行 action、不写低层配置。", routes(link("模板中心", "#/templates"))));
        items.add(trouble("trouble.blank-gate-no-history", "为什么空 gate 没有调试记录？", List.of("未配置 conditionGroupId。", "旧逻辑按原流程运行。"), List.of("确认目标字段是否为空。"), List.of("需要调试时明确绑定条件组。"), "未配置 conditionGroupId 时不读取 store、不 evaluate、不写 history。", routes(link("条件调试", "#/condition-debugger"))));
        items.add(trouble("trouble.state-variable-action-failed", "为什么状态变量动作失败？", List.of("变量 key 为空。", "类型转换失败。", "PLAYER scope 无目标。"), List.of("看 action 执行记录。", "看状态变量页面是否出现目标值。"), List.of("修正 key/type/targetMode。"), "StateVariable 写入通过受控 state_variable action，不是任意 JSON 写入。", routes(link("状态变量", "#/state-variables"), link("动作列表", "#/actions"))));
        items.add(trouble("trouble.signal-no-consumer", "为什么 Signal 有事件但无后续动作？", List.of("频道没有消费者。", "消费者 disabled。", "channel 拼写不一致。"), List.of("看频道详情消费者摘要。", "看 Doctor 无消费者提示。"), List.of("新增或修正监听器 / receiver / action relay。"), "SignalBridge 只负责派发事件；没有消费者时不会凭空执行动作。", routes(link("SignalBridge", "#/signals"), link("Doctor", "#/doctor"))));
        items.add(trouble("trouble.snapshot-degraded", "为什么配置时间轴显示 degraded？", List.of("manifest 或 snapshot package 解析失败。", "快照包指纹与 manifest 不匹配。", "存储文件被外部手动改坏。"), List.of("看配置时间轴顶部和详情警告。", "manifest degraded 可打开 Doctor 查看 Snapshot 诊断。", "单个 package 警告以时间轴详情为准。", "检查服务端日志里的具体 JSON/IO 错误。"), List.of("先停止继续创建保存点或回滚。", "修复或移走损坏的 manifest/package 后再刷新。"), "Snapshot degraded 会 fail closed，避免把半可信数据写回配置。", routes(link("配置时间轴", "#/snapshots"), link("Doctor", "#/doctor"))));
        items.add(trouble("trouble.rollback-operation-diff", "为什么回滚前保护点显示本次操作变化？", List.of("pre_rollback 是回滚 apply 前自动创建的保护点。", "它的 operationDiff 记录回滚本身会改什么。"), List.of("区分“本次操作变化”和“与上一保存点变化”。", "dry-run 中确认 create/update/delete 文件计划。"), List.of("需要理解回滚影响时优先看本次操作变化。", "需要理解保护点来源时再看与上一保存点变化。"), "回滚不是把 diff 方向倒置展示；pre_rollback operation diff 以当前配置到目标快照为方向。", routes(link("配置时间轴", "#/snapshots"))));
        items.add(trouble("trouble.snapshot-retention", "超过 200 个自动快照会怎样？", List.of("自动快照默认只保留最新 200 个。", "manual 和 pre_rollback 不被自动 retention 删除。"), List.of("在配置时间轴里筛选自动快照。", "确认重要节点是否需要手动保存点。"), List.of("关键里程碑使用手动保存点。", "不要把自动快照当长期归档。"), "Retention 只清理旧 auto snapshot manifest 记录和对应 package，不清理 manual / pre_rollback。", routes(link("配置时间轴", "#/snapshots"))));
        for (Map<String, Object> item : items) {
            item.put("kind", "troubleshooting");
            item.put("searchText", searchText(item));
        }
        return items;
    }

    private static List<Map<String, Object>> glossary() {
        List<Map<String, Object>> terms = new ArrayList<>();
        addTerm(terms, "channel", "频道", List.of("Channel"), "SignalBridge 中命名事件通道。", "频道不是状态存储。");
        addTerm(terms, "logic-chain", "逻辑链", List.of("Logic Chain"), "按强关联组件展示的可视化链路。", "一个逻辑链可以包含多个频道。");
        addTerm(terms, "focus-channel", "焦点频道", List.of("Focus Channel"), "逻辑链详情中的当前观察入口。", "只影响视角和高亮，不代表裁剪 runtime。");
        addTerm(terms, "associated-component", "关联组件", List.of("Component"), "由 Signal / Join / Timer / Action 强关联组成的一组节点。", "弱引用不会合并无关 component。");
        addTerm(terms, "signalbridge", "SignalBridge", List.of("事件总线"), "负责接收和分发 signal 的事件总线。", "SignalBridge 是事件总线，不是状态数据库。");
        addTerm(terms, "signal-listener", "SignalListener", List.of("监听器"), "监听一个频道并执行动作列表的虚拟配置。", "可有列表 gate 和 action gate。");
        addTerm(terms, "action", "Action", List.of("动作"), "被触发后执行的具体效果。", "由 ActionEngine 执行。");
        addTerm(terms, "action-config", "ActionConfig", List.of("动作配置"), "Action 的 typed 配置结构。", "帮助中心不新增 ActionType。");
        addTerm(terms, "action-engine", "ActionEngine", List.of("动作引擎"), "统一执行 ActionConfig 的 runtime 入口。", "帮助中心不改变执行顺序。");
        addTerm(terms, "condition-group", "ConditionGroup", List.of("条件组"), "可复用的条件树。", "ConditionEngine 只判断。");
        addTerm(terms, "condition-engine", "ConditionEngine", List.of("条件引擎"), "只读评估条件树的引擎。", "不写状态、不发 signal、不执行 action。");
        addTerm(terms, "state-variable", "StateVariable", List.of("状态变量"), "保存 GLOBAL / PLAYER 状态的存储。", "由 state_variable action 受控写入。");
        addTerm(terms, "state-action", "State Action", List.of("state_variable action"), "通过受控 Action 写入 StateVariable。", "不是任意 JSON 写入。");
        addTerm(terms, "join", "Join", List.of("Signal Join"), "多个输入满足后输出 signal。", "passive observer。");
        addTerm(terms, "barrier", "Barrier", List.of("ALL Join"), "所有输入都到达才输出的 Join 模式。", "常用于 A+B 都完成。");
        addTerm(terms, "aggregator", "Aggregator", List.of("ANY_N / COUNT Join"), "按阈值或计数聚合输入的 Join 用法。", "仍是 passive observer。");
        addTerm(terms, "timer", "Timer", List.of("Scheduler"), "延迟、倒计时或重复计时配置。", "runtime 实例为内存态。");
        addTerm(terms, "scheduler", "Scheduler", List.of("调度器"), "Timer 背后的时间调度语义。", "配置保存和运行态实例不同。");
        addTerm(terms, "template", "Template", List.of("模板"), "可导入、导出和 apply 的配置包。", "import 不等于 apply。");
        addTerm(terms, "prefab", "Prefab", List.of("预制组合"), "面向常见组合的模板化配置。", "不会自动复制世界实体。");
        addTerm(terms, "snapshot", "Snapshot", List.of("配置时间轴", "保存点"), "WebAdmin allowlist 配置的保存点。", "不包含 runtime history、玩家背包或世界实体。");
        addTerm(terms, "rollback", "Rollback", List.of("配置回滚", "回滚"), "把 allowlist 配置恢复到选定保存点的受控写入。", "必须先 dry-run，apply 前创建 pre_rollback。");
        addTerm(terms, "pre-rollback", "pre_rollback", List.of("回滚前保护点"), "回滚 apply 前自动创建的保护点。", "其 operation diff 显示本次回滚实际变化。");
        addTerm(terms, "operation-diff", "operationDiff", List.of("本次操作变化"), "写入前自动快照回填的本次写入资源变化。", "不同于与上一保存点的普通 diff。");
        addTerm(terms, "edit-lock", "Edit Lock", List.of("编辑锁"), "写入前保护同一目标的锁。", "丢失后不能继续保存。");
        addTerm(terms, "fingerprint", "Fingerprint", List.of("expectedFingerprint"), "配置快照指纹，用于冲突检测。", "过期会要求刷新。");
        addTerm(terms, "dry-run", "Dry-run", List.of("预览"), "只读计划和校验，不实际写入。", "模板 apply 前必须看预览。");
        addTerm(terms, "placeholder", "Placeholder", List.of("占位引用"), "模板里无法自动绑定的外部世界引用。", "placeholder binding apply deferred。");
        addTerm(terms, "runtime-gate", "Runtime Gate", List.of("运行时条件门"), "绑定 conditionGroupId 后在运行路径外层判断。", "空 gate 不评估。");
        addTerm(terms, "action-gate", "Action Gate", List.of("单条动作条件"), "单条 Action 上的条件组。", "false 只跳过当前 action。");
        addTerm(terms, "vbd", "VBD", List.of("Virtual Block Device", "虚拟方块设备"), "把虚拟方块或容器等世界交互转换成 signal 的设备。", "编辑器内世界实体草稿创建 deferred。");
        addTerm(terms, "action-relay", "ActionRelay", List.of("动作中继方块"), "世界方块上的动作列表引用。", "Logic Chain Editor 内旧 Action 通常只读或追加。");
        addTerm(terms, "signal-receiver", "SignalReceiver", List.of("信号接收器"), "监听 signal 并输出世界效果的接收器引用。", "世界绑定仍走现有页面或游戏内流程。");
        addTerm(terms, "region", "Region", List.of("RegionController", "区域"), "区域 enter / exit / stay 触发和控制对象。", "区域运行时语义不在帮助中心改变。");
        addTerm(terms, "doctor", "Doctor", List.of("诊断"), "只读健康检查和建议入口。", "不自动修复。");
        addTerm(terms, "debugger", "Debugger", List.of("条件调试"), "查看 runtime gate 历史判断。", "历史是最近内存记录。");
        addTerm(terms, "replay", "Replay", List.of("模拟重放"), "用历史 snapshot 只读复算条件。", "不执行 action、不 emit signal。");
        for (Map<String, Object> term : terms) {
            term.put("kind", "glossary");
            term.put("searchText", searchText(term));
        }
        return terms;
    }

    private static Map<String, Object> category(String id, String title, String summary) {
        return map("id", id, "title", title, "summary", summary);
    }

    private static Map<String, Object> topic(
            String id,
            String title,
            String summary,
            String category,
            List<String> tags,
            List<Map<String, Object>> basicSections,
            List<Map<String, Object>> professionalSections,
            List<String> exampleIds,
            List<String> troubleshootingIds,
            List<String> glossaryIds,
            List<Map<String, Object>> pageLinks,
            List<String> relatedTopicIds
    ) {
        return map(
                "id", id,
                "kind", "topic",
                "title", title,
                "summary", summary,
                "category", category,
                "tags", tags,
                "basicTitle", title + "：基础",
                "basicSummary", summary,
                "basicSections", basicSections,
                "professionalTitle", title + "：专业",
                "professionalSummary", "运行语义、边界、诊断路径和 deferred 能力。",
                "professionalSections", professionalSections,
                "examples", exampleIds,
                "troubleshootingLinks", troubleshootingIds,
                "glossaryTerms", glossaryIds,
                "pageLinks", pageLinks,
                "relatedTopics", relatedTopicIds
        );
    }

    private static Map<String, Object> example(
            String id,
            String title,
            String goal,
            List<String> modules,
            List<String> steps,
            List<String> professionalNotes,
            List<String> commonErrors,
            List<Map<String, Object>> relatedRoutes,
            String relatedTemplateId,
            List<String> relatedTopicIds
    ) {
        return map(
                "id", id,
                "title", title,
                "goal", goal,
                "whenToUse", goal,
                "modules", modules,
                "steps", steps,
                "professionalNotes", professionalNotes,
                "commonErrors", commonErrors,
                "relatedRoutes", relatedRoutes,
                "relatedTemplateId", relatedTemplateId,
                "relatedTopicIds", relatedTopicIds
        );
    }

    private static Map<String, Object> trouble(
            String id,
            String symptom,
            List<String> likelyCauses,
            List<String> checks,
            List<String> fixHints,
            String professionalExplanation,
            List<Map<String, Object>> relatedRoutes
    ) {
        return map(
                "id", id,
                "symptom", symptom,
                "title", symptom,
                "likelyCauses", likelyCauses,
                "checks", checks,
                "fixHints", fixHints,
                "professionalExplanation", professionalExplanation,
                "relatedRoutes", relatedRoutes
        );
    }

    private static void addTerm(List<Map<String, Object>> terms, String id, String term, List<String> aliases, String definition, String technicalNotes) {
        terms.add(map(
                "id", id,
                "term", term,
                "title", term,
                "aliases", aliases,
                "definition", definition,
                "technicalNotes", technicalNotes
        ));
    }

    private static List<Map<String, Object>> sections(Map<String, Object>... sections) {
        return List.of(sections);
    }

    private static Map<String, Object> section(String title, String... bullets) {
        return map("title", title, "bullets", List.of(bullets));
    }

    private static List<Map<String, Object>> routes(Map<String, Object>... links) {
        return List.of(links);
    }

    private static Map<String, Object> link(String label, String route) {
        return map("label", label, "route", route);
    }

    private static String searchText(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder();
        appendSearch(builder, map);
        return builder.toString().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private static void appendSearch(StringBuilder builder, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> valueMap) {
            for (Object child : valueMap.values()) {
                appendSearch(builder, child);
            }
            return;
        }
        if (value instanceof Iterable<?> values) {
            for (Object child : values) {
                appendSearch(builder, child);
            }
            return;
        }
        builder.append(' ').append(value);
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
