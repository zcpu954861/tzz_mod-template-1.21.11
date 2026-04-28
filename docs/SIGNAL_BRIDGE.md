# SignalBridge 使用说明

SignalBridge 是 TZZ Mod 的服务端事件桥 / 事件频道系统。它把“某个系统发生了事件”抽象成一个 `signal channel`，再由监听器根据 channel 执行动作。

典型用途：

```text
RegionController 触发 signal
-> SignalBridge 查找 listener
-> listener 执行 command action 或 signal action
-> ActionEngine 统一执行动作
```

SignalBridge 不新增方块、GUI 或网络 payload。它只负责服务端事件联动，适合让 RegionController、封锁卡、密码机、感应板以及未来工具共享同一套事件通道。

## 5.15 稳定化 / GUI 前置整理版

5.15 标记为 `v1.17.0-stabilization-foundation`。这是 5.x 底层工具链稳定化版本，不是新玩法功能版本。

### 5.x 底层能力封版含义

到 5.15 为止，SignalBridge、SignalDevice、`virtual_block_device`、ItemStackMatcher、interactionItem、container、itemCondition、consume 和 itemSubmit 等底层工具链进入稳定化状态。后续不应继续在 5.x 内堆叠大量新触发类型，而应优先推进：

- 服务层 / DTO / 权限与审计。
- debug / doctor 结构化输出。
- Web Admin UI 与实时同步。
- 更高层的 ConditionEngine / ConditionGroup。
- 游戏主线调度、任务和 GUI 管理。

### 稳定化护栏

`stabilizationGuardTest` 已挂到 Gradle `check` / `build`。执行：

```text
./gradlew.bat clean build
```

会自动运行稳定化护栏。当前覆盖：

- `SignalDeviceData` 字段保留。
- 旧 `signal_devices.json` 缺字段样本兼容。
- `ConsumePlan` / `ConsumePlanner` 两阶段消耗计划。
- `ItemSubmitEvaluator` 与生产 itemSubmit 路径。
- `InteractionDecisionEvaluator` 的 allow / require lock / cooldown 决策。
- displayName 和 diagnostic DTO 渲染。

### debug / doctor

- `/tzz signal device debug <device>` 用于单设备诊断。
- `/tzz signal doctor` 用于全局诊断。
- doctor 不扫描世界、不强制加载区块、不扫描玩家背包。
- doctor 只检查已登记设备、已加载方块和已有内存状态。
- 对需要玩家上下文或真实交互才能判断的内容，doctor 应输出诊断提示，而不是做昂贵扫描。

### Web Admin UI 方向

未来管理界面建议分工：

- Minecraft 游戏内工具：绑定、选择、定位、简单初始化。
- 本地 / 服务器 Web Admin UI：完整全局配置、缩放逻辑图、模块化卡片、实时状态、多人协作基础。
- Web UI 必须与服务器实时同步，不能直接改 JSON。
- 命令、游戏内工具、Web UI 必须共用服务层。
- 所有写操作应走服务端统一服务，并预留权限与审计。

## 7.1 WebAdmin 对象版本 / 编辑锁

7.1 只增强 7.0 已开放的 WebAdmin 设备显示元数据编辑链路，不扩大编辑范围。当前仍只允许编辑 `displayName`、`note`、`iconKey`，这些字段仅影响 WebAdmin 展示，不改变 Minecraft 游戏逻辑、SignalBridge 行为或 `signal_devices.json`。

### 对象版本

- WebAdmin 设备显示元数据继续存放在 `<world-save-root>/tzz/webadmin/web_admin_device_metadata.json`。
- 每条 metadata 带有 `version`、`updatedAt`、`updatedBy`。
- 前端保存时必须提交 `expectedVersion`。
- 服务端发现 `expectedVersion` 与当前版本不一致时返回 `conflict_detected`，不会覆盖已有数据。

### 编辑锁

- 编辑锁是 WebAdmin 运行态协作状态，当前不持久化。
- 锁目标为 `device_metadata:<deviceId>`。
- EDITOR / OWNER 可以获取锁；VIEWER / TESTER 不能获取锁。
- PATCH 保存必须持有有效锁，并继续通过 session、CSRF / same-origin、权限、校验、审计和 realtime 链路。
- 锁默认 TTL 为 5 分钟，编辑模式会定期 heartbeat 续锁。
- 保存成功或取消编辑会释放锁；页面关闭或断线由 TTL 兜底。

### realtime 与审计

- 锁获取、释放、过期会发布轻量 `edit_lock_changed` 事件，不包含 session token、cookie value、密码 hash 或 salt。
- metadata 保存成功仍发布 `config_changed`、`device_config_changed`、`write_audit_appended`。
- 冲突、权限拒绝、校验失败会通过统一 `WebAdminWriteResult` 返回，并写入安全摘要审计。

7.1 仍不开放 enabled、channel、itemSubmit、action、region bounds、用户或系统设置编辑。后续 7.2 可在对象版本和编辑锁基础上逐步评估低风险设备逻辑配置编辑。

## 7.2 WebAdmin 设备基础配置编辑

7.2 在 7.0 / 7.1 的写入安全链路上开放第一批低风险设备基础逻辑配置编辑。与 7.0 的 WebAdmin 显示元数据不同，本阶段的字段会影响当前世界中的设备触发与 Signal 分发，因此必须继续使用权限、CSRF、编辑锁、冲突检测、audit 和 realtime。

当前只允许编辑：

- `enabled`：设备启用 / 禁用状态。
- `channel`：设备主频道 / primary channel。

写入 API：

```text
GET /api/webadmin/device-basic-config/{deviceId}
PATCH /api/webadmin/device-basic-config/{deviceId}
```

`PATCH` 要求：

- 有效 WebAdmin session。
- `EDITOR` 或 `OWNER` 权限。
- CSRF / same-origin 校验。
- `device_basic_config` 编辑锁。
- `expectedFingerprint` 冲突检测。
- 输入校验通过。
- 通过 `SignalDeviceStore` / domain service 写入，不允许前端直接改 JSON。
- 写入后记录 audit，并发布轻量 `config_changed`、`device_config_changed`、`write_audit_appended` realtime 事件。

7.2 不改变 `SignalDeviceData` schema，不写入 WebAdmin metadata 文件之外的展示元数据，不修改 `signal_devices.json` 的复杂结构。修改 enabled / channel 时必须保留 interactionItem、itemSubmit、matcher、container、itemConditions、redstone mode、BlockState condition、success/fail/off channel、action、region、用户和系统设置等既有字段。

暂不开放：

- `interactChannel`、success/fail/off channel。
- cooldown、pulseTicks、redstone mode。
- BlockState condition。
- interactionItem、itemSubmit、matcher、consume。
- action、command action。
- region bounds。
- 用户和系统设置。

后续阶段可在 7.2 的锁和冲突检测基础上继续评估 success/fail channel、cooldown、pulseTicks 等较低风险字段；itemSubmit、action command、region bounds 等高风险配置应继续单独分阶段设计。

## 7.3 WebAdmin 设备扩展基础配置编辑

7.3 在 7.2 `enabled` / 主频道编辑稳定后，继续开放少量按设备类型支持的扩展基础配置字段。该阶段仍然只通过 WebAdmin 写入安全链路处理简单字段，不进入 itemSubmit、matcher、Action、Region 等复杂编辑。

当前按设备类型支持：

- `virtual_block_device`：`interactChannel`、`successChannel`、`failChannel`、`interactionCooldownTicks`。
- `signal_receiver`：在接收器所在区块已加载时支持 `pulseTicks`。
- `action_relay`：在继电器所在区块已加载时支持 `cooldownTicks`。
- `signal_emitter`：当前没有可编辑扩展基础配置，只做只读展示。

7.3 的 channel 字段继续复用深色 WebAdmin combobox，可选择 `/api/signals/channels` 返回的已有频道，也可手动输入新频道。手动输入不会自动创建 listener、receiver 或 action_relay。可选扩展频道支持显式“设为未设置”，但不会误把空输入当作清空操作。

所有写入仍必须通过：

- `EDITOR` / `OWNER` 权限。
- CSRF / same-origin 校验。
- `device_extended_config` 编辑锁。
- `expectedFingerprint` 冲突检测。
- `WebAdminWriteResult` / validationErrors。
- audit 记录。
- 轻量 `config_changed`、`device_config_changed`、`write_audit_appended` realtime 事件。

7.3 不改变 `SignalDeviceData` schema，不绕过 `SignalDeviceStore`，不直接让前端写 JSON。修改扩展字段时必须保留 enabled、主频道、offChannel、interactionItem matcher/source/consume、itemSubmit、container、itemConditions、BlockState condition、redstone mode、WebAdmin metadata、runtime/history summary 等既有字段。

暂不开放：

- itemSubmit requirements / consume。
- ItemStackMatcher / interactionItem source。
- action 列表、command action 或 action 执行。
- Region bounds / target filter。
- 用户和系统设置。
- Scratch-like 模块编辑器、ConditionEngine、GameController / MissionSystem。

后续阶段可基于 7.3 的 supported-field、fingerprint、lock、audit 和 realtime 模式，继续设计 Signal / Listener / Receiver / ActionRelay 更系统化的编辑能力。

## 7.4 WebAdmin Channel Metadata + Signal Listener 基础配置编辑

7.4 开始在 Signal 管理页面开放两类低风险编辑：WebAdmin-only channel 显示元数据，以及已存在 Signal Listener 的基础配置。该阶段继续使用 7.0 到 7.3 已建立的权限、CSRF、编辑锁、expectedFingerprint、WebAdminWriteResult、audit 和 realtime 安全链路。

Channel metadata 存放在 `<world-save-root>/tzz/webadmin/web_admin_channel_metadata.json`，只影响 WebAdmin 显示：

- `displayName`
- `note`
- `iconKey`

metadata 不创建真实 channel，不创建 listener / receiver / action_relay，不改变 SignalBridge 的 channel 字符串，也不改变逻辑链运行语义。raw channel 仍是稳定技术 ID。

Signal Listener 基础配置只允许编辑已存在 listener 的：

- `enabled`
- `channel`
- `cooldownTicks`

Listener 使用当前 `SignalListenerStore` 的稳定 `id` 作为 WebAdmin listenerRef。PATCH 只更新上述三个字段，必须保留 listener 名称、id、actions 列表和其它运行统计信息。修改 channel 不会自动创建 channel metadata，也不会自动创建 listener、receiver 或 action_relay。

7.4 不开放：

- 创建 / 删除 / 重命名 listener。
- 新增 / 删除 / 编辑 / 排序 / 执行 action。
- command action、signal action、matcher、itemSubmit、interactionItem、consume、container condition、Region、用户、系统设置或 raw JSON 编辑。
- Scratch-like 编辑器、图谱拖拽编辑、ConditionEngine 或 GameController / MissionSystem。

保存成功会发布轻量 `channel_metadata_changed`、`signal_listener_config_changed`、`config_changed` 和 `write_audit_appended` realtime 事件。前端按当前 route 过滤并静默局部刷新，不整页刷新、不闪屏、不跳顶部、不清空筛选、排序或正在输入的其它表单。

## channel

channel 是事件频道名，也是技术标识。它会被自动规范化为小写，只允许：

- 小写字母 `a-z`
- 数字 `0-9`
- `_`
- `-`
- `.`
- `:`

示例：

```text
area.a.enter
password.main.success
debug.test
```

不要在 channel 中使用中文、空格或其他特殊字符。

## listener

listener 是 signal 的监听器。每个 listener 绑定一个 channel，并保存一组动作。

当有人执行：

```text
/tzz signal emit debug.test
```

所有启用状态、channel 为 `debug.test` 且不在冷却中的 listener 都会执行自己的动作。

listener 可以通过完整 ID、唯一短 ID 或精确名称引用。中文名称可直接使用；名称包含空格时需要加引号。

```text
/tzz signal listen info 测试监听器
/tzz signal listen info "测试 监听器"
```

## 创建 listener

```text
/tzz signal listen create debug.test 测试监听器
/tzz signal listen list
/tzz signal listen info 测试监听器
```

`list` 会优先显示名称和短 ID，`info` 会显示完整 ID、channel、状态、冷却时间和动作数量。

## command action

listener 可以绑定命令动作：

```text
/tzz signal listen create debug.test 测试监听器
/tzz signal listen addAction "测试监听器" command say 收到 debug.test
/tzz signal emit debug.test
```

命令文本会交给 ActionEngine 执行。

## signal action

listener 也可以继续发出另一个 signal，用于链式事件：

```text
/tzz signal listen create debug.test 测试监听器
/tzz signal listen create debug.chain 链式监听器
/tzz signal listen addAction "测试监听器" signal debug.chain
/tzz signal listen addAction "链式监听器" command say 收到链式信号
/tzz signal emit debug.test
```

这种方式可以把多个系统拆成独立 listener，再用 channel 串联。

## RegionController 联动

RegionController 的 `addAction` 已支持 `signal` 类型：

```text
/tzz signal listen create area.a.enter A区进入监听器
/tzz signal listen addAction "A区进入监听器" command say 收到A区进入信号
/tzz regionctl addAction A区控制器 enter signal area.a.enter
```

当玩家进入 `A区控制器` 绑定的区域时，RegionController 会发出 `area.a.enter`，SignalBridge 再触发对应 listener。

`exit` 和 `stay` 也可以使用 signal：

```text
/tzz regionctl addAction A区控制器 exit signal area.a.exit
/tzz regionctl addAction A区控制器 stay signal area.a.stay
```

## SignalEmitter 发射器方块

`signal_emitter` 是第一个可放置的 SignalBridge 设备方块。它绑定一个 channel，并在红石从未通电变为通电时发出 signal。

```text
/tzz signal device bind <x> <y> <z> redstone.test
/tzz signal device info <x> <y> <z>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
```

设备管理命令：

```text
/tzz signal device list
/tzz signal device name <x> <y> <z> <name>
/tzz signal device clearName <device>
/tzz signal device info <device>
/tzz signal device history <device>
/tzz signal device debug <device>
```

`<device>` 可以是设备名称、完整 sourceId 或短 ID。名称包含空格时需要加引号：

```text
/tzz signal device info "大厅拉杆发射器"
```

最小联动流程：

```text
/tzz signal listen create redstone.test 红石测试监听器
/tzz signal listen addAction "红石测试监听器" command say 收到红石信号
/tzz signal device bind <x> <y> <z> redstone.test
/tzz signal device name <x> <y> <z> 大厅拉杆发射器
```

绑定后，用拉杆或按钮给 `signal_emitter` 通电即可触发 `redstone.test`。持续通电不会重复触发，断电后再次通电会再次触发。

右键信号发射器可以查看当前频道、启用状态、红石状态和位置。`test` 命令会使用执行命令的玩家作为上下文；红石自动触发时没有玩家上下文，SignalBridge 会以设备位置作为动作执行位置。SignalEventHistory 中会记录 `sourceType = signal_device`。

设备管理索引保存到：

```text
world/tzz_mod/signal_devices.json
```

这个文件只用于管理显示名、位置、最近触发和调试信息。`SignalEmitterBlockEntity` 仍然保存实际 `channel`、`enabled` 和 `lastPowered`。设备 history 来自内存 SignalEventHistory，不写入 JSON。设备管理不会扫描未加载区块，因此 registry 中的离线设备只会按已有记录展示；如果方块未加载或已不存在，`debug` 会显示相应提示。

## SignalReceiver 接收器方块

`signal_receiver` 是世界实体红石输出接收端。它把 SignalBridge channel 转换为红石脉冲：

```text
signal -> signal_receiver -> 红石输出
```

职责边界：

- `SignalListener` 是虚拟接收端，负责执行 command / message / sound / signal 等 ActionEngine 动作。
- `signal_receiver` 只负责红石输出，不执行命令动作。
- `signal_receiver` 不要求 channel 上存在 SignalListener。
- `signal_receiver` 只处理已登记且已加载区块中的方块实体，不扫描世界，不强制加载区块。

基础命令：

```text
/tzz signal device bind <x> <y> <z> door.a.open
/tzz signal device info <x> <y> <z>
/tzz signal device test <x> <y> <z>
/tzz signal device debug <device>
```

`device bind` 会根据坐标上的方块类型自动绑定发射频道或接收频道。`device test` 对发射器表示测试 emit signal；对接收器表示手动输出一次红石脉冲，不会 emit 新 signal。

接收器专用命令：

```text
/tzz signal receiver pulse <x> <y> <z> <ticks>
/tzz signal receiver trigger <x> <y> <z>
/tzz signal receiver info <x> <y> <z>
```

`pulse` 设置输出脉冲时长，单位是 GT。默认值为 `5 GT`，常用范围建议 `2 GT` 到 `20 GT`。命令参数只输入整数，不输入 `GT` 后缀。

最小联动流程：

```text
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
/tzz signal receiver pulse <receiver-x> <receiver-y> <receiver-z> 5
/tzz signal emit door.a.open
```

由发射器驱动接收器：

```text
/tzz signal device bind <emitter-x> <emitter-y> <emitter-z> door.a.open
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
```

然后给 `signal_emitter` 通电，`signal_receiver` 会收到 `door.a.open` 并输出红石脉冲。

`signal_devices.json` 继续作为设备管理索引。`SignalReceiverBlockEntity` 保存实际 `channel`、`enabled`、`pulseTicks` 和当前脉冲状态；索引用于显示名称、位置、最近接收和 debug 信息。receiver 的历史展示来自内存 SignalEventHistory，不写入新的 JSON。

## ActionRelay 动作继电器方块

`action_relay` 是世界中可见的 ActionEngine 执行节点。它监听一个 SignalBridge channel，收到 signal 后执行自己保存的 `actions[]`：

```text
signal -> action_relay -> ActionEngine actions
```

职责边界：

- `SignalListener` 是后台虚拟逻辑接收端。
- `signal_receiver` 是实体红石输出端。
- `action_relay` 是实体可见 ActionEngine 执行节点。
- `action_relay` 不是单纯命令方块，而是执行 `actions[]`。
- `action_relay` 不输出红石。
- `action_relay` 没有 SignalListener 也能工作。
- `action_relay` 只处理已登记且已加载区块中的方块实体，不扫描世界，不强制加载区块。

继电器专用命令：

```text
/tzz signal relay bind <x> <y> <z> <channel>
/tzz signal relay addAction <x> <y> <z> command <command>
/tzz signal relay addAction <x> <y> <z> message <message>
/tzz signal relay addAction <x> <y> <z> sound <sound>
/tzz signal relay addAction <x> <y> <z> signal <channel>
/tzz signal relay listActions <x> <y> <z>
/tzz signal relay removeAction <x> <y> <z> <index>
/tzz signal relay clearActions <x> <y> <z>
/tzz signal relay cooldown <x> <y> <z> <ticks>
/tzz signal relay trigger <x> <y> <z>
/tzz signal relay info <x> <y> <z>
```

`cooldownTicks` 的单位是 GT，默认 `0 GT`，表示无冷却。命令参数只输入整数，不输入 `GT` 后缀。

`addAction` 复用现有 ActionEngine 支持的动作类型。当前可添加 command、message、sound 和 signal 动作；其中 signal 动作会继续走 SignalBridge 的递归保护。

最小使用流程：

```text
/tzz signal relay bind <x> <y> <z> game.start
/tzz signal relay addAction <x> <y> <z> command say 游戏开始
/tzz signal emit game.start
```

由发射器驱动动作继电器：

```text
/tzz signal device bind <emitter-x> <emitter-y> <emitter-z> game.start
/tzz signal relay bind <relay-x> <relay-y> <relay-z> game.start
/tzz signal relay addAction <relay-x> <relay-y> <relay-z> command say 游戏开始
```

之后给 `signal_emitter` 通电，`action_relay` 会收到 `game.start` 并把动作交给 ActionEngine 执行。

设备管理命令也支持动作继电器：

```text
/tzz signal device bind <x> <y> <z> <channel>
/tzz signal device list
/tzz signal device info <device>
/tzz signal device debug <device>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
/tzz signal device cleanup
```

`signal_devices.json` 继续作为设备管理索引。`ActionRelayBlockEntity` 保存实际 `channel`、`enabled`、`cooldownTicks` 和 `actions[]`；索引用于显示名称、位置、最近执行和 debug 信息。历史展示仍使用内存 SignalEventHistory / 设备管理机制，不新增永久 history JSON。

Signal 设备被破坏后会自动从 `signal_devices.json` 中移除。`/tzz signal device cleanup` 用于维护已有索引：它只遍历已登记设备，只检查当前已加载区块，如果已加载位置不再是对应设备类型，就删除该记录；未加载区块会跳过，不扫描世界，也不强制加载区块。powered / pulse / active 等同方块状态变化不会删除索引。

`action_relay` 外观复用 `signal_emitter` / `signal_receiver` 的多元素科技风模型结构，但使用绿色主题。执行动作后会短暂进入 active 高亮状态；active 只表示最近执行过动作，不输出红石。

## Virtual Block Device 虚拟方块发射器

`virtual_block_device` 是虚拟方块发射器。它不会新增方块，也不会自动扫描世界；管理员需要手动把某个已有方块坐标登记为 signal 触发源。

```text
已有方块红石状态变化
-> virtual_block_device
-> emit SignalBridge channel
```

它适合把普通拉杆、按钮、压力板、红石灯、普通方块或其他模组方块接入 SignalBridge。TZZ 专用设备方块不建议绑定为虚拟方块发射器，因为它们已经有专用命令。

### 绑定与配置

```text
/tzz signal blockDevice bind <x> <y> <z> <channel>
/tzz signal blockDevice offChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearOffChannel <x> <y> <z>
/tzz signal blockDevice mode <x> <y> <z> redstone_rising
/tzz signal blockDevice mode <x> <y> <z> redstone_falling
/tzz signal blockDevice mode <x> <y> <z> redstone_both
/tzz signal blockDevice info <x> <y> <z>
/tzz signal blockDevice test <x> <y> <z>
/tzz signal blockDevice unbind <x> <y> <z>
/tzz signal blockDevice refresh <x> <y> <z>
/tzz signal blockDevice condition <x> <y> <z> <condition>
/tzz signal blockDevice clearCondition <x> <y> <z>
/tzz signal blockDevice conditionMode <x> <y> <z> condition_enter
/tzz signal blockDevice conditionMode <x> <y> <z> condition_exit
/tzz signal blockDevice conditionMode <x> <y> <z> condition_both
/tzz signal blockDevice conditionInfo <x> <y> <z>
/tzz signal blockDevice interactChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearInteractChannel <x> <y> <z>
/tzz signal blockDevice interaction <x> <y> <z> enable
/tzz signal blockDevice interaction <x> <y> <z> disable
/tzz signal blockDevice interactionCooldown <x> <y> <z> <ticks>
/tzz signal blockDevice interactionInfo <x> <y> <z>
```

`bind` 会记录当前位置的方块 ID、当前是否通电和当前红石强度，避免绑定瞬间误触发。`refresh` 用于管理员更换该坐标方块后重新读取当前方块 ID 和红石状态。

### 红石检测

每次检测只读取绑定坐标本身：

- `blockStatePowered`：如果方块状态包含 `powered` 属性且为 true，则为 true。
- `receivedPowerLevel`：读取该坐标接收到的红石信号强度，范围 0 到 15。
- `currentPowered = blockStatePowered || receivedPowerLevel > 0`。

触发规则：

- `redstone_rising`：`lastPowered=false` 且 `currentPowered=true` 时发出 `channel`。
- `redstone_falling`：`lastPowered=true` 且 `currentPowered=false` 时发出 `offChannel`；未设置 `offChannel` 时发出 `channel`。
- `redstone_both`：通电边沿发出 `channel`，断电边沿优先发出 `offChannel`；未设置 `offChannel` 时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式仍是 `redstone_both`，通电和断电都会发出主 `channel`，这是预期行为。

`test` 命令只手动 emit 主频道，不改变 `lastPowered`，也不模拟红石边沿。

### BlockState 条件触发

5.6 阶段新增方块状态条件触发。它检测当前绑定坐标方块公开的 BlockState 属性，例如 `powered`、`open`、`lit`、`waterlogged`、`facing`、`age`、`level`、`delay`、`mode` 等当前运行时方块实际拥有的属性。

它不检测：

- 箱子、木桶、潜影盒里有什么物品。
- 告示牌写了什么字。
- 命令方块里的命令。
- 方块实体 NBT。
- 容器是否被玩家打开。
- 周围方块状态或红石网络结构。

完整格式示例：

```text
/tzz signal blockDevice condition <x> <y> <z> minecraft:lever[powered=true]
/tzz signal blockDevice condition <x> <y> <z> minecraft:oak_door[open=true]
/tzz signal blockDevice condition <x> <y> <z> minecraft:oak_stairs[waterlogged=true,facing=north]
/tzz signal blockDevice condition <x> <y> <z> minecraft:redstone_lamp[lit=true]
/tzz signal blockDevice condition <x> <y> <z> minecraft:repeater[delay=4]
/tzz signal blockDevice condition <x> <y> <z> minecraft:comparator[mode=subtract]
/tzz signal blockDevice condition <x> <y> <z> minecraft:wheat[age=7]
```

条件设置时会做一次解析和验证：

- 方块 ID 必须存在，并且必须与当前坐标的当前方块一致。
- 当前方块必须拥有条件中写到的每个 property。
- 每个 value 必须是该 property 允许的值。
- 条件中重复 property 会被拒绝。
- 格式错误会给中文错误提示，不会写入 `signal_devices.json`。

例如：

- `minecraft:stone[waterlogged=true]` 会被拒绝，因为 stone 不支持 `waterlogged`。
- `minecraft:stone[open=true]` 会被拒绝，因为 stone 不支持 `open`。
- `minecraft:repeater[delay=9]` 会被拒绝，因为 `delay` 不支持 `9`。
- `minecraft:comparator[mode=abc]` 会被拒绝，因为 `mode` 不支持 `abc`。

代码不会硬编码 Wiki 的 BlockState 属性表。公开 Wiki 的 Java Edition data values 页面可以作为测试参考，但运行时当前方块的 `BlockState.getProperties()` 才是权威来源，因此也能兼容版本差异和其他模组方块。

条件触发模式：

- `condition_enter`：`lastConditionMatched=false` 且 `currentMatched=true` 时发出 `channel`。
- `condition_exit`：`lastConditionMatched=true` 且 `currentMatched=false` 时优先发出 `offChannel`；未设置 `offChannel` 时发出 `channel`。
- `condition_both`：进入条件发出 `channel`，退出条件优先发出 `offChannel`；未设置 `offChannel` 时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式是 `condition_both`，进入和退出条件都会发出主 `channel`，这是预期行为。

`clearCondition` 只清空 BlockState 条件，不影响 5.5 的红石状态检测配置。`conditionInfo` 会显示当前方块 ID、条件方块 ID、条件属性、当前方块支持的 property 列表、上次满足状态和当前满足状态。

### 右键交互触发

5.7 阶段新增虚拟方块交互触发。它只对 `signal_devices.json` 中已经登记为 `virtual_block_device` 的坐标生效，不监听未绑定方块。

```text
/tzz signal blockDevice interactChannel <x> <y> <z> lobby.terminal.click
/tzz signal blockDevice interactionCooldown <x> <y> <z> 20
/tzz signal blockDevice interactionInfo <x> <y> <z>
```

交互触发规则：

- `interactChannel` 设置后会自动启用 interaction。
- `clearInteractChannel` 会清空 `interactChannel` 并禁用 interaction。
- `interaction enable` 要求已经设置 `interactChannel`。
- `interactionCooldownTicks` 单位是 GT，默认 `0 GT`，表示无冷却；命令参数只输入整数，不输入 `GT` 后缀。
- 默认只处理 `MAIN_HAND`，避免主副手双触发。
- 默认不阻止原版交互：右键箱子仍打开箱子，右键门仍开关门，右键按钮仍按下按钮，同时可以 emit signal。
- 成功触发 interaction signal 时，触发玩家会播放一次主手挥手动画。
- 交互触发会带玩家上下文进入 SignalBridge / ActionEngine。
- 当前方块 ID 与绑定时 `blockId` 不一致时不触发，`interactionInfo` 和 `device debug` 会提示 refresh 或重新 bind。

一个虚拟方块发射器可以同时配置红石、BlockState condition 和 interaction 三种触发。如果这些触发都指向同一 channel，一次玩家右键可能因为原版状态变化和 interaction 同时产生多个 signal。这是可配置行为，管理员应按玩法需求使用不同 channel 或关闭不需要的触发。

### 容器事件触发

5.8 阶段新增容器事件触发。它只对已经登记为 `virtual_block_device` 的容器坐标生效，不是通用 NBT 检测，也不做槽位物品条件。

```text
/tzz signal blockDevice containerOpenChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerOpenChannel <x> <y> <z>
/tzz signal blockDevice containerCloseChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerCloseChannel <x> <y> <z>
/tzz signal blockDevice containerChangeChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerChangeChannel <x> <y> <z>
/tzz signal blockDevice container <x> <y> <z> enable
/tzz signal blockDevice container <x> <y> <z> disable
/tzz signal blockDevice containerCooldown <x> <y> <z> <ticks>
/tzz signal blockDevice containerCheckInterval <x> <y> <z> <ticks>
/tzz signal blockDevice containerInfo <x> <y> <z>
```

容器事件规则：

- `containerOpenChannel` 在玩家实际打开对应容器 screen 后 emit。
- `containerCloseChannel` 在对应 screen session 关闭时 emit。
- `containerChangeChannel` 在容器内容指纹发生变化时 emit。
- `container enable` 要求至少已经设置一个容器事件 channel。
- `containerCooldownTicks` 单位是 GT，默认 `0 GT`，表示无冷却。
- `containerChangeCheckIntervalTicks` 单位是 GT，默认 `10 GT`；建议使用 `10-20 GT`，避免过高频率。
- 命令参数只输入整数，不输入 `GT` 后缀。

内容变化的 MVP 指纹只包含每个 slot 的物品 registry id、数量和 damage。它不会匹配第几格是什么物品，不会匹配物品名称、lore、NBT 或新版数据组件，也不会读取告示牌文字、命令方块命令或任意 BlockEntity NBT path。

性能边界：

- open / close 使用右键候选和实际 screen session 确认，不把普通右键直接当作打开。
- content changed 只检查已登记且启用了 `containerChangeChannel` 的绑定容器。
- 每次检查只读取该设备自己的一个容器坐标。
- 不扫描世界、区块或周围方块。
- 不自动寻找箱子、木桶、潜影盒或其他容器。
- 不强制加载区块，未加载区块直接跳过。
- 内容不变不 emit，也不写 `signal_devices.json`。

一个虚拟方块发射器可以同时配置红石、BlockState condition、interaction 和 container 事件。如果多个触发指向同一 channel，可能出现多个 signal，这是配置结果，不是 bug。

### 容器槽位 / 物品条件触发

5.9 阶段新增容器槽位 / 物品条件触发。它只对已经登记为 `virtual_block_device`、当前方块是容器、并且配置了 itemCondition 的坐标生效。它不是通用 NBT 检测，也不匹配物品名称、lore、附魔或新版数据组件。

```text
/tzz signal blockDevice itemCondition addSlotEmpty <x> <y> <z> <name> <slot> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> at_least <count> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> exactly <count> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> at_most <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> at_least <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> exactly <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> at_most <count> <channel>
/tzz signal blockDevice itemCondition list <x> <y> <z>
/tzz signal blockDevice itemCondition info <x> <y> <z> <name>
/tzz signal blockDevice itemCondition remove <x> <y> <z> <name>
/tzz signal blockDevice itemCondition clear <x> <y> <z>
/tzz signal blockDevice itemCondition enable <x> <y> <z> <name>
/tzz signal blockDevice itemCondition disable <x> <y> <z> <name>
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_enter
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_exit
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_both
/tzz signal blockDevice itemCondition offChannel <x> <y> <z> <name> <channel>
/tzz signal blockDevice itemCondition clearOffChannel <x> <y> <z> <name>
/tzz signal blockDevice itemCondition refresh <x> <y> <z> <name>
/tzz signal blockDevice itemCondition test <x> <y> <z> <name>
```

条件类型：

- `slot_empty`：指定槽位为空。
- `slot_item`：指定槽位的 item registry id 等于配置值，并且 count 满足 `at_least`、`exactly` 或 `at_most`。
- `total_item`：统计整个容器内指定 item registry id 的总数量，并按 `at_least`、`exactly` 或 `at_most` 判断。

触发规则：

- `condition_enter`：条件 false -> true 时 emit `channel`。
- `condition_exit`：条件 true -> false 时优先 emit `offChannel`；未设置时回退 emit `channel`。
- `condition_both`：进入条件 emit `channel`，退出条件优先 emit `offChannel`；未设置时回退 emit `channel`。
- 新增或重新启用条件时会把 `lastMatched` 初始化为当前匹配结果，避免配置瞬间误触发。
- `refresh` 只重新同步当前匹配状态，不会 emit signal。
- `test` 只手动 emit 该条件的 `channel`，不会改变 `lastMatched`。

校验规则：

- `itemId` 必须存在于当前运行时物品注册表。
- `slot_empty` 和 `slot_item` 的 slot 必须在当前容器槽位范围内。
- `count` 必须大于等于 1。
- name 在同一设备内必须唯一。
- 条件配置异常不会被 cleanup 自动删除，`device debug` 会提示管理员修复或 remove condition。

性能边界：

- 只检查已登记、已启用、配置了 itemCondition 的绑定容器。
- slot 条件只读取指定 slot。
- total 条件只遍历该容器自身 slot；同一容器内多个 total 条件会在一次统计中复用结果。
- 不扫描世界、区块或周围方块，不自动寻找容器，不强制加载区块。
- 不读取未绑定容器，不序列化完整 ItemStack NBT。
- 内容不变不 emit；条件匹配状态不变不 emit；状态不变不写 `signal_devices.json`。

5.8 的 `containerChangeChannel` 和 5.9 的 itemCondition 可以同时存在：前者表示“任意内容变化”，后者表示“具体条件进入 / 退出”。如果两个配置指向同一 channel，可能出现多个 signal，这是配置结果，不是 bug。

### ItemStack Matcher 物品数据匹配

5.10 阶段新增 `ItemStackMatcher`。它是可复用的 ItemStack 模板匹配核心，容器 `slot_matcher` / `total_matcher` 和右键交互主手物品匹配都使用同一套数据结构和判断逻辑。

容器模板条件命令：

```text
/tzz signal blockDevice itemCondition addSlotMatchFromHand <x> <y> <z> <name> <slot> at_least <count> <channel>
/tzz signal blockDevice itemCondition addSlotMatchFromHand <x> <y> <z> <name> <slot> ignore <channel>
/tzz signal blockDevice itemCondition addSlotMatchFromSlot <x> <y> <z> <name> <targetSlot> <templateSlot> exactly <count> <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> at_most <count> <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> ignore <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromSlot <x> <y> <z> <name> <templateSlot> at_least <count> <channel>
/tzz signal blockDevice itemCondition matcherInfo <x> <y> <z> <name>
/tzz signal blockDevice itemCondition matcherFromHand <x> <y> <z> <name>
/tzz signal blockDevice itemCondition matcherFromSlot <x> <y> <z> <name> <slot>
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchDamage enable
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchCustomName disable
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchLore enable
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchCustomData enable
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchComponents enable
/tzz signal blockDevice itemCondition matcherCount <x> <y> <z> <name> at_least <count>
/tzz signal blockDevice itemCondition matcherCount <x> <y> <z> <name> ignore
```

右键交互主手物品匹配命令：

```text
/tzz signal blockDevice interactionItem setFromHand <x> <y> <z>
/tzz signal blockDevice interactionItem clear <x> <y> <z>
/tzz signal blockDevice interactionItem enable <x> <y> <z>
/tzz signal blockDevice interactionItem disable <x> <y> <z>
/tzz signal blockDevice interactionItem option <x> <y> <z> matchDamage enable
/tzz signal blockDevice interactionItem option <x> <y> <z> matchLore disable
/tzz signal blockDevice interactionItem count <x> <y> <z> exactly <count>
/tzz signal blockDevice interactionItem count <x> <y> <z> ignore
/tzz signal blockDevice interactionItem info <x> <y> <z>
```

匹配能力：

- 默认匹配 item registry id 和数量规则。
- `countMode` 支持 `at_least`、`exactly`、`at_most` 和 `ignore`。
- `ignore` 不接收数量参数，表示 matcher 不检查数量；info/debug 中显示“数量要求：不检查”。如果需要至少 2 个物品，应使用 `at_least 2`。
- 可选匹配 damage、自定义名称、lore、`custom_data` 和 data components 整体快照。
- 所有启用的匹配项都必须满足；未启用的匹配项不参与判断。
- 模板可以从玩家主手捕获，也可以从容器槽位捕获。

边界：

- 本阶段不是任意 NBT path 查询系统。
- 不检测告示牌文字、命令方块命令、刷怪笼 NBT、任意 BlockEntity NBT、玩家 NBT 或实体 NBT。
- 不扫描玩家背包，不检查副手，不检查装备栏或盔甲栏。
- 右键交互匹配失败时不 emit、不阻止原版交互、不显示失败提示、不消耗物品。
- 容器 slot matcher 只读取指定 slot；total matcher 只遍历该绑定容器自身 slot。
- 不扫描世界、区块或周围方块，不强制加载区块，状态不变不写 `signal_devices.json`。

### 右键物品匹配增强

5.11 阶段增强 `interactionItem` 主手物品匹配。所有成功 / 失败反馈都是可选配置：默认不显示消息、不播放音效、不触发失败频道、不消耗物品。

```text
/tzz signal blockDevice interactionItem successChannel <x> <y> <z> <channel>
/tzz signal blockDevice interactionItem clearSuccessChannel <x> <y> <z>
/tzz signal blockDevice interactionItem failChannel <x> <y> <z> <channel>
/tzz signal blockDevice interactionItem clearFailChannel <x> <y> <z>
/tzz signal blockDevice interactionItem successMessage <x> <y> <z> <message>
/tzz signal blockDevice interactionItem clearSuccessMessage <x> <y> <z>
/tzz signal blockDevice interactionItem failMessage <x> <y> <z> <message>
/tzz signal blockDevice interactionItem clearFailMessage <x> <y> <z>
/tzz signal blockDevice interactionItem successSound <x> <y> <z> <soundId> <volume> <pitch>
/tzz signal blockDevice interactionItem clearSuccessSound <x> <y> <z>
/tzz signal blockDevice interactionItem failSound <x> <y> <z> <soundId> <volume> <pitch>
/tzz signal blockDevice interactionItem clearFailSound <x> <y> <z>
/tzz signal blockDevice interactionItem consume <x> <y> <z> enable
/tzz signal blockDevice interactionItem consume <x> <y> <z> disable
/tzz signal blockDevice interactionItem consumeCount <x> <y> <z> <count>
```

规则：

- `successChannel` 为空时，匹配成功回退使用 `interactChannel`。
- `failChannel` 为空时，匹配失败不 emit signal。
- success / fail message 完全由管理员配置，未配置时不发送。
- success / fail sound 只播放给触发玩家，未配置时不播放。
- `consume` 只消耗右键玩家 `MAIN_HAND`，不搜索背包、副手、装备栏或盔甲栏。
- `consumeCount` 必须大于 0；主手数量不足时进入失败流程，不发成功频道、不发送成功反馈、不消耗。
- `interactionCooldownTicks` 同时限制成功和失败反馈；冷却中不 emit、不反馈、不额外播放触发动效。5.14 起，已启用的成功消耗属于开锁成本，匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。
- 成功和失败交互尝试都会播放 `MAIN_HAND` 主手挥手动画；冷却中不额外播放触发动画。
- 成功 / 失败 signal 都保留玩家上下文并走现有 SignalBridge 递归保护。

边界：

- 本阶段只增强 interactionItem 主手匹配。
- 不做玩家背包检测、副手匹配、装备栏 / 盔甲栏匹配、复杂多物品条件或 GUI。
- 不做通用 NBT 查询，不检测告示牌文字、命令方块命令、刷怪笼 NBT、任意 BlockEntity NBT、玩家 NBT 或实体 NBT。
- 未来 GUI / Admin UI 应覆盖 success/fail channel、message、sound、consume 和 consumeCount；也可以拆分成交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器和 debug/doctor 工具。

### 玩家物品来源匹配

5.12 阶段把 `interactionItem` 的物品来源从固定主手扩展为可配置来源。旧配置缺少新字段时默认 `main_hand`，保持 5.10 / 5.11 行为；`off_hand` 和 `inventory_contains` 必须由管理员显式配置。

5.13 阶段继续新增装备 / 盔甲来源：`armor_head`、`armor_chest`、`armor_legs`、`armor_feet`、`armor_any`。这些来源同样必须显式配置；右键事件仍只处理 `MAIN_HAND`，armor 来源只读取触发玩家对应盔甲槽位的 ItemStack，不处理装备事件，也不支持装备 / 盔甲消耗。

```text
/tzz signal blockDevice interactionItem source <x> <y> <z> main_hand
/tzz signal blockDevice interactionItem source <x> <y> <z> off_hand
/tzz signal blockDevice interactionItem source <x> <y> <z> inventory_contains
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_head
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_chest
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_legs
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_feet
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_any
/tzz signal blockDevice interactionItem vanillaInteraction <x> <y> <z> allow
/tzz signal blockDevice interactionItem vanillaInteraction <x> <y> <z> require_item_match
```

来源规则：

- `main_hand`：读取触发玩家 `MAIN_HAND`，继续支持 5.11 的主手消耗。
- `off_hand`：只检查触发玩家副手物品；右键事件仍只处理 `MAIN_HAND`，不会处理副手右键事件。
- `inventory_contains`：只在玩家右键已绑定方块时检查该玩家自己的主背包 / 热键栏；不包含副手、装备栏或盔甲栏，也不在 tick 中扫描。
- `armor_head`：只检查触发玩家头盔槽。
- `armor_chest`：只检查触发玩家胸甲槽。
- `armor_legs`：只检查触发玩家护腿槽。
- `armor_feet`：只检查触发玩家靴子槽。
- `armor_any`：只检查头盔、胸甲、护腿、靴子四个盔甲槽，任意槽位匹配即成功，并记录第一个匹配槽位。
- `inventory_contains` 使用 `ItemStackMatcher` 的非数量条件筛选背包 stack，然后统计总数；`ignore` 表示至少存在一个匹配 stack，`at_least` / `exactly` / `at_most` 作用于总数，其中 `at_most` 要求总数大于 0。
- `consumeCount` 是成功后消耗数量，和 `countMode=ignore` 无关；启用 consume 时仍必须满足消耗数量。
- `consume` 只支持 `main_hand`。source 为 `off_hand`、`inventory_contains` 或任意 `armor_*` 时启用 consume 会被拒绝；旧数据中出现不兼容配置时运行时不会消耗，并会进入失败流程或在 debug 中提示。
- `vanillaInteraction` 默认 `allow`，不阻止原版右键行为。显式设置 `require_item_match` 后，它会作为锁定策略生效：只有 interactionItem 匹配成功才允许原版交互继续；匹配失败、空手不匹配或数量不足以 consume 时会阻止箱子打开、门开关、按钮/拉杆切换等原版 use。`interactionCooldownTicks` 不会让锁失效；冷却中匹配失败仍会阻止原版交互。cooldown 只抑制 signal、message、sound、history / lastResult 和额外挥手动画，不会跳过已启用的成功消耗。匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。设备禁用、interaction 禁用、matcher 未启用、blockId 不一致、空气或未绑定方块仍保持 `PASS`。
- 对门使用 `require_item_match` 时，绑定上半格或下半格都可以；玩家右键另一半门时会尝试归一化到已绑定设备，避免通过门的另一半绕过锁。该逻辑只检查当前点击坐标和门的另一半坐标，不扫描世界。

性能边界：

- 仍然只检查被右键的一个坐标。
- 不扫描世界、区块或周围方块，不强制加载区块。
- 不每 tick 检查玩家背包。
- 不读取其他玩家背包。
- `armor_head` / `armor_chest` / `armor_legs` / `armor_feet` 只读取对应盔甲槽；`armor_any` 只读取四个盔甲槽。
- 不做背包消耗、副手消耗、装备 / 盔甲消耗、多物品提交、复杂条件组或通用 NBT 查询。
- `interactionItem info` 会显示 source、最近匹配来源、最近匹配槽位和最近匹配数量；`device debug` 会显示 source 与 consume 的兼容性诊断。

### 消耗策略 / 多物品提交

5.14 阶段增强 `interactionItem` 的消耗策略，并新增可选的 `itemSubmit` 多物品提交。所有新增能力默认关闭，必须由管理员显式配置。

`interactionItem matcher` 和 `itemSubmit` 是互斥的匹配模式：前者用于钥匙、通行证、单个任务物品等单物品匹配；后者用于多个 requirement 全部满足后提交。启用 `itemSubmit` 时会自动关闭单物品 `interactionItem matcher`，但 success/fail channel、message、sound、`vanillaInteractionPolicy`、cooldown 等反馈配置仍会保留并复用。如果要恢复单物品匹配，需要先 disable `itemSubmit`，再手动 enable `interactionItem` matcher。

```text
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> matched_source
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> inventory
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> main_hand
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> off_hand
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> hotbar_first
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> main_inventory_first
```

消耗规则：

- `consume` 仍默认关闭。
- `interactionItemConsumeSource=matched_source` 是默认策略：`main_hand` 来源消耗主手，`off_hand` 来源消耗副手，`inventory_contains` 来源消耗主背包 / 热键栏。
- `interactionItemConsumeSource=inventory` 会从触发玩家主背包 / 热键栏中消耗匹配物品。
- `interactionItemConsumeSource=main_hand` / `off_hand` 会强制从对应手消耗，但对应手上的物品也必须匹配。
- `inventoryConsumeOrder` 支持 `hotbar_first` 和 `main_inventory_first`。
- `main_hand`、`off_hand`、`inventory_contains` 可以在显式启用 consume 后消耗匹配物品。
- `armor_head`、`armor_chest`、`armor_legs`、`armor_feet` 和 `armor_any` 仍不支持 consume。
- 物品数量不足时进入失败流程，不发成功频道、不发送成功反馈、不播放成功音效、不消耗。
- 消耗是原子操作：会先检查所有需要消耗的物品，确认全部满足后才实际扣减 stack。
- creative 玩家按同一套配置扣减匹配 stack，用于避免小游戏逻辑绕过。

`itemSubmit` 是可选的多物品提交系统。它只在玩家右键已绑定 `virtual_block_device` 时检查触发玩家自己的主背包 / 热键栏，不在 tick 中扫描玩家背包。

```text
/tzz signal blockDevice itemSubmit enable <x> <y> <z>
/tzz signal blockDevice itemSubmit disable <x> <y> <z>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> at_least <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> exactly <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> at_most <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> ignore
/tzz signal blockDevice itemSubmit list <x> <y> <z>
/tzz signal blockDevice itemSubmit info <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit infoAll <x> <y> <z>
/tzz signal blockDevice itemSubmit remove <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit clear <x> <y> <z>
/tzz signal blockDevice itemSubmit enableRequirement <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit disableRequirement <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit matcherFromHand <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit matcherOption <x> <y> <z> <name> <option> enable|disable
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> at_least <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> exactly <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> at_most <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> ignore
/tzz signal blockDevice itemSubmit consume <x> <y> <z> enable
/tzz signal blockDevice itemSubmit consume <x> <y> <z> disable
/tzz signal blockDevice itemSubmit consumeOrder <x> <y> <z> hotbar_first
/tzz signal blockDevice itemSubmit consumeOrder <x> <y> <z> main_inventory_first
/tzz signal blockDevice itemSubmit consumeCount <x> <y> <z> <name> <count>
```

提交规则：

- `itemSubmit` 默认关闭；启用后会作为当前主要匹配条件，所有启用的 requirement 必须同时满足才算提交成功。
- `itemSubmit` 启用后不再额外要求单物品 `interactionItem matcher` 通过，也不会执行单物品 `interactionItem consume`，避免隐藏单物品条件和双重消耗。
- `addFromHand` 从管理员主手捕获 `ItemStackMatcher` 模板；提交时仍检查触发玩家的主背包 / 热键栏。
- `ignore` 不带 count，表示不检查 matcher 数量；背包匹配仍要求至少存在一个匹配 stack。
- `itemSubmit consume` 默认关闭；启用后会按 `consumeOrder` 原子消耗所有 requirement 对应的物品。
- 每个 requirement 可以设置自己的 `consumeCount`。
- 任一 requirement 不满足或消耗计划不足时，整次提交失败，不扣减任何物品。
- 成功 / 失败反馈复用 5.11 的 success/fail channel、message、sound、`vanillaInteractionPolicy` 和 cooldown 逻辑。
- `require_item_match` 仍然是锁定策略；`itemSubmit` 启用时按提交结果决定是否放行原版交互：提交满足则放行，提交失败则阻止。cooldown 不会让锁失效，也不会跳过已启用的原子消耗；它只限制 signal、message、sound、history / lastResult 和额外动画等副作用。

性能边界：

- 不扫描世界、区块或周围方块，不强制加载区块。
- 不每 tick 扫描玩家背包。
- 只有玩家右键已绑定方块时才检查触发玩家。
- 单个 `interactionItem` inventory consume 只扫描触发玩家主背包 / 热键栏。
- `itemSubmit` 只扫描触发玩家主背包 / 热键栏。
- 不读取其他玩家。
- 不读取副手，除非 source 明确为 `off_hand`。
- 不读取装备 / 盔甲用于消耗。
- 不做完整 NBT path 查询。
- 状态不变不写 `signal_devices.json`。

### 统一设备命令

统一设备命令也支持虚拟方块发射器：

```text
/tzz signal device list
/tzz signal device info <device>
/tzz signal device debug <device>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
/tzz signal device cleanup
```

`device info` 和 `device debug` 会显示当前方块 ID、绑定时方块 ID、两者是否一致、`blockStatePowered`、`receivedPowerLevel`、`currentPowered`、`lastPowered`、触发模式、主频道、断电频道、condition 摘要和常见问题提示。
5.7 后也会显示 interaction 摘要、`interactChannel`、交互冷却、最近交互玩家和最近交互结果。5.8 后会显示 container 摘要、open / close / change channel、容器冷却、内容检查间隔、最近容器事件和最近结果。5.9 后会显示 itemCondition 数量、启用数量、最近物品条件触发和条件诊断。5.14 后会显示当前匹配模式、`interactionItem` consumeSource、inventoryConsumeOrder、最近消耗摘要，以及 `itemSubmit` requirement 数量、提交结果和消耗摘要；`itemSubmit` 模式下会提示单物品 matcher 已被忽略/禁用。`device history` 可查看来源为 `virtual_block_device` 的红石、condition、interaction、container、itemCondition 和 itemSubmit 触发记录。

`cleanup` 对虚拟方块发射器采用保守策略：只遍历已登记设备，只检查已加载区块，不强制加载区块。已加载位置变成空气时会删除记录；当前方块 ID 与绑定时不一致但不是空气时不会自动删除，只在 debug 中提示。condition 不合法时也不会自动删除记录，只在 debug 中提示重新设置 condition 或 `clearCondition`。

### 性能边界

Virtual Block Device 的 tick 检测复杂度是 `O(已登记 virtual_block_device 数量)`。

- 不扫描世界。
- 不扫描区块。
- 不扫描周围方块。
- 不自动寻找拉杆、按钮、压力板或红石灯。
- 不递归追踪红石线路。
- 不强制加载未加载区块。
- 未加载区块直接跳过。
- 每个设备每次只检测自己的一个坐标。
- 有 condition 时 tick 不重新解析 condition 字符串，只比较保存后的 property/value。
- 交互触发是事件驱动的，不通过 tick 轮询。
- 右键事件只检查被右键的一个坐标，不扫描世界、区块或周围方块。
- 不自动寻找可交互方块。
- 不在每次右键时遍历世界内容。
- 状态不变不 emit。
- 状态不变不写 JSON。
- `signal_devices.json` 写入已节流，服务端停止时强制保存。

### 职责边界

- `signal_emitter`：专用方块，红石 / 交互 -> signal。
- `virtual_block_device`：已有方块，红石状态变化 / BlockState condition / 玩家右键交互 -> signal。
- `signal_receiver`：signal -> 红石输出。
- `action_relay`：signal -> ActionEngine actions。
- `SignalListener`：后台虚拟逻辑接收端。

## 后续计划

以下内容只作为后续阶段计划记录，不在 5.14 MVP 实现：

- 复杂 ConditionEngine / ConditionGroup 后续单独设计。
- 5.15 稳定化 / GUI 前置整理版。
- 6.0 / 7.0 GUI / Admin UI：所有 source、matcher、consume、itemSubmit、反馈、容器、区域、action 和 signal 设备配置未来都应进入 GUI；可拆分成交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器、debug/doctor 工具。

## cooldown

listener 可以设置全局冷却时间，避免高频 signal 重复执行动作。

```text
/tzz signal listen cooldown 测试监听器 100
```

`100` 表示 100 tick。冷却时间不能小于 `0 tick`。

## 递归保护

SignalBridge 有最大递归深度限制，用于防止 signal action 无限触发自身或形成循环。

示例风险：

```text
listener A -> signal debug.loop
debug.loop 又触发 listener A
```

达到最大递归深度后，SignalBridge 会停止继续递归执行，并返回中文失败结果，不会卡死服务器。

## 常见问题

### channel 被拒绝

检查 channel 是否只包含小写字母、数字、`_`、`-`、`.`、`:`。

错误示例：

```text
Debug.Test
区域.进入
debug test
```

正确示例：

```text
debug.test
area.a.enter
password.main.success
```

### 找不到 listener

可以先执行：

```text
/tzz signal listen list
```

然后使用完整名称、唯一短 ID 或完整 ID。名称包含空格时必须加引号。

### emit 没有执行动作

检查以下内容：

- listener 是否启用。
- listener 的 channel 是否和 emit 的 channel 完全一致。
- listener 是否还在 cooldown 中。
- listener 是否已经添加 action。

### RegionController 没有触发 signal

先用下面的命令确认 listener 本身可用：

```text
/tzz signal listen test A区进入监听器
/tzz signal emit area.a.enter
```

如果 listener 可用，再检查 RegionController 是否启用、绑定区域是否正确、目标过滤是否允许当前玩家触发。

## 配置文件

SignalBridge listener 配置保存到：

```text
world/tzz_mod/signal_listeners.json
```

该文件由模组自动维护，不建议手动编辑，除非熟悉当前 JSON 结构。

## 可观测性与诊断命令

4.5 阶段新增了一组只读观测命令，用于排查 SignalBridge 的运行状态：

```text
/tzz signal history
/tzz signal history <channel>
/tzz signal clearHistory
/tzz signal channels
/tzz signal channel info <channel>
/tzz signal listen debug <listener>
/tzz signal doctor
```

- `history`：查看最近 signal 事件。
- `history <channel>`：查看指定 channel 的最近 signal 事件。
- `clearHistory`：清空当前内存中的 signal 历史记录。
- `channels`：查看所有已知 signal channel，以及 listener 数量、动作数量、最近触发时间。
- `channel info <channel>`：查看指定 channel 的 listener 和最近事件详情。
- `listen debug <listener>`：查看单个 listener 的动作列表、冷却剩余、最近事件和直接 signal 自递归风险。
- `doctor`：全局诊断 SignalBridge 配置问题，包括空动作 listener、最近触发但没有 listener 的 channel、全部禁用的 channel、直接 signal 自递归、过长 cooldown、非法 channel 脏数据和异常 action 配置。

这些命令不会新增配置文件，也不会改变 SignalBridge `emit`、listener cooldown、ActionEngine 或 RegionController 的执行语义。

## WebAdmin 7.0 配置编辑基础 / 最小安全写入闭环

7.0 正式进入 WebAdmin 编辑阶段，但第一批只开放低风险 WebAdmin 设备显示元数据编辑。本阶段不是游戏逻辑配置编辑，不修改 `SignalDeviceData` 业务语义，不写 `signal_devices.json`，不改变 SignalBridge、SignalDevice、VirtualBlockDevice、itemSubmit、RegionController 或 ActionEngine 的执行行为。

### 允许编辑的字段

7.0 仅允许通过 WebAdmin 设备详情页编辑：

- `displayName`：WebAdmin 显示名称，可为空，为空时回退到原设备名称。
- `note`：WebAdmin 备注，仅纯文本。
- `iconKey`：WebAdmin 预设图标 key，不支持上传图片、外部 URL 或任意资源引用。

这些字段保存到世界级 WebAdmin 元数据文件：

```text
<world-save-root>/tzz/webadmin/web_admin_device_metadata.json
```

每个世界拥有独立的设备显示元数据；旧世界没有该文件时使用空 metadata。

### 写入安全链路

7.0 的写入必须经过 6.9 / 6.10 建立的安全链路：

```text
Web UI
→ PATCH /api/webadmin/device-metadata/{deviceId}
→ WebAdmin session
→ EDITOR / OWNER 权限检查
→ CSRF / 同源写请求校验
→ validation
→ WebAdminDeviceMetadataService
→ WebAdminDeviceMetadataStore
→ structured audit
→ realtime config_changed / device_config_changed
→ WebAdminWriteResult
```

`VIEWER` 和 `TESTER` 不能写入；`EDITOR` 和 `OWNER` 可以写入显示元数据。前端权限提示只用于用户体验，后端仍强制执行 session、权限、CSRF 和 validation。

### API

```text
GET /api/webadmin/device-metadata/{deviceId}
PATCH /api/webadmin/device-metadata/{deviceId}
```

`GET` 是安全只读接口。`PATCH` 是 7.0 唯一新增的真实写 API，但它只写 WebAdmin 元数据，不写游戏逻辑配置。所有写入结果使用 `WebAdminWriteResult`，包括 `ok`、`no_change`、`validation_failed`、`permission_denied`、`target_not_found` 和 CSRF 相关错误。

### 设备详情 UI 布局

7.0 的设备详情页采用平衡式双栏布局。左侧主列保留设备基础信息、Debug 检查、最近事件和紧凑折叠的配置摘要；右侧侧栏保留 WebAdmin 显示信息、Doctor 状态摘要、关联频道摘要、设备标识 / 快捷信息、快捷跳转和最近状态。后续编辑页面应继续遵循“核心内容在主列，摘要 / 跳转 / WebAdmin 元数据在侧栏，避免重复展示并减少滚动”的原则。

### 明确不包含

7.0 不开放以下编辑：

- `enabled`
- 主 channel / success channel / fail channel / interactChannel
- cooldown / redstone mode
- interactionItem / itemSubmit / matcher / consume
- action / command action
- region bounds / target filter / enter-exit-stay actions
- WebAdmin 用户、角色、密码、session、系统设置

这些高风险编辑需要在后续阶段继续设计 preview、conflict detection、草稿 / 发布 / 回滚和更细的 validation。

更多说明见 `docs/WEBADMIN_EDITING_FOUNDATION_7_0.md`，回归测试见 `docs/REGRESSION_TEST_7_0.md`。

## WebAdmin 6.10 写入前置稳定化 / 编辑阶段前总审查

6.10 是 7.0 WebAdmin 配置编辑前的安全闸门。本阶段不开放真实编辑，不新增公开写 API，不写 JSON，也不改变 SignalBridge、SignalDevice、VirtualBlockDevice、RegionController、ActionEngine 或 WebAdmin 只读页面的既有运行语义。

### 审查结论

6.10 对 6.9 写入前置体系做了总审查：

- 权限矩阵：VIEWER 只读，TESTER 预留测试 / dry-run，EDITOR 可进入普通配置编辑范围但不能管理用户、系统设置或危险操作，OWNER 拥有完整权限。
- 写结果模型：未来写 API 必须统一返回 `WebAdminWriteResult`，包括 `ok`、`permission_denied`、`unauthenticated`、`csrf_required`、`csrf_invalid`、`validation_failed`、`target_not_found`、`conflict_detected`、`dangerous_operation_requires_confirmation`、`no_change` 和 `internal_error`。
- CSRF / 安全：未来写 API 必须经过有效 session、后端权限判断、CSRF / 同源校验、validation、audit 和 realtime 事件发布链路。
- 审计模型：写成功、写失败、权限拒绝、校验失败和内部错误都应可审计，且不记录明文密码、password hash、salt、session token 或 cookie value。
- Mutation service：Web UI 不能直接改 JSON，必须通过 service / DTO / domain store；未来 service 应支持 preview、validate、apply、no-change、conflict detection 和审计。
- Realtime 写事件：`config_changed`、`write_audit_appended`、`permission_denied`、`validation_failed` 以及对象级 config_changed 事件当前作为协议预留，不伪造、不推送敏感 payload。

### 7.0 推荐入口

6.10 建议 7.0 第一批只接入低风险编辑：设备名称 / 备注 / iconKey、基础 enabled 状态和基础 channel 字段。itemSubmit / matcher、action command、region bounds、用户管理和系统安全设置属于高风险编辑，应在后续阶段单独设计 validation、preview、冲突检测和回滚策略。

更多说明见 `docs/WEBADMIN_WRITE_STABILIZATION_6_10.md`，回归测试见 `docs/REGRESSION_TEST_6_10.md`。

## WebAdmin 6.9 写入前置 / 权限审计 / Service API 基础

6.9 是 WebAdmin 进入配置编辑前的安全前置阶段。该阶段不开放真实配置编辑，不新增公开可调用的写 API，不写 JSON，也不改变 SignalBridge、SignalDevice、VirtualBlockDevice、RegionController 或 ActionEngine 的既有运行语义。

### 权限矩阵

WebAdmin 写入前置定义统一角色权限：

| 角色 | 当前能力 | 未来预留能力 |
| --- | --- | --- |
| VIEWER | 只读查看 | 无配置写入 |
| TESTER | 只读查看 | 测试触发 / dry-run / validate |
| EDITOR | 只读查看 | 普通配置编辑：device、Signal、Region、Action、item matcher |
| OWNER | 只读查看 | 用户管理、系统设置、危险操作和完整写入权限 |

权限判断由服务端 `WebAdminPermissionService` / `WebAdminRolePolicy` 负责，前端不能作为核心权限来源。

### 写操作结果格式

未来写 API 必须返回统一 `WebAdminWriteResult`：

- `success`
- `code`
- `message`
- `targetType`
- `targetId`
- `changed`
- `validationErrors`
- `auditId`
- `realtimeEventId`
- `requiresConfirmation`
- `conflict`
- `data`

常见 code 包括：`ok`、`permission_denied`、`unauthenticated`、`csrf_required`、`csrf_invalid`、`validation_failed`、`target_not_found`、`conflict_detected`、`dangerous_operation_requires_confirmation`、`no_change`、`internal_error`。所有 message 必须中文可读，不返回 Java stack trace、password hash、salt、session token、cookie 或大型内部对象。

### CSRF / 写请求安全

6.9 新增写请求安全 helper。未来所有写操作必须同时满足：

- 有效 WebAdmin session。
- 服务端权限检查通过。
- CSRF token 校验通过，或使用等价同源保护。
- JSON 请求类型符合未来写 API 约束。
- 危险操作具备二次确认机制。

当前新增 `GET /api/webadmin/write/capabilities` 只读接口，用于返回当前角色的未来写入能力摘要、CSRF 策略和 token。该接口不执行写操作。

### 审计日志模型

6.9 定义结构化写操作审计事件：

- `auditId`
- `occurredAt`
- `actorUsername`
- `actorRole`
- `sessionIdHashSummary`
- `remoteAddress`
- `operationType`
- `targetType`
- `targetId`
- `beforeSummary`
- `afterSummary`
- `result`
- `errorCode`
- `message`

审计 summary 会脱敏敏感字段，不记录明文密码、password hash、salt、session token 或 cookie。未来写失败、权限拒绝和校验失败都应产生审计事件。

### Service 层原则

未来写 service 应使用统一模式：

```text
request
→ WebAdminMutationContext
→ permission check
→ CSRF / write security
→ validate
→ preview
→ apply through existing domain service / store
→ audit
→ realtime config_changed
→ WebAdminWriteResult
```

Web UI 不能直接改 JSON，不能绕过现有 store / domain service。写入成功后必须发布轻量 realtime 事件，例如 `config_changed` 和对象专用事件。

### Realtime 变更事件

6.9 补充未来写操作相关事件类型：

- `config_changed`
- `write_audit_appended`
- `permission_denied`
- `validation_failed`
- `user_changed`
- `system_settings_changed`
- `device_config_changed`
- `signal_config_changed`
- `region_config_changed`
- `action_config_changed`

这些事件当前主要作为协议和测试护栏存在；没有真实写操作时不会伪造事件。事件 payload 必须轻量且不包含敏感内容。

6.9 后仍不开放编辑。6.10 建议作为 7.0 配置编辑前总审查阶段，确认权限、审计、CSRF、DTO、realtime 和回滚策略后再进入真实编辑能力。

更多说明见 `docs/WEBADMIN_WRITE_FOUNDATION_6_9.md`，回归测试见 `docs/REGRESSION_TEST_6_9.md`。

## WebAdmin 6.8 实时同步基础

6.8 建立 WebAdmin 只读实时同步基础。当前阶段采用认证后的 Server-Sent Events / Event Stream：

```text
GET /api/realtime/events
```

该连接必须携带有效 WebAdmin session cookie；未登录或 session 无效时不能建立连接。事件流只用于只读通知，不接受客户端写操作。

### 事件驱动边界

- 服务端只在状态变化时推送轻量事件。
- 当前真实接入：`realtime_connected`、`heartbeat`、`webadmin_user_connected`、`webadmin_user_disconnected`、`signal_emitted`、`history_appended`。
- 预留类型：`device_updated`、`doctor_changed`、`action_executed`、`receiver_pulse`、`region_event`、`config_changed`。
- 事件不包含完整 devices / history / doctor DTO。
- 事件不包含 password hash、salt、session token、cookie value 或内部大对象。
- 慢客户端使用有界队列保护，断开后释放资源。

### 前端处理方式

WebAdmin 登录后建立 realtime event stream，topbar 显示实时同步状态和最后事件时间。前端收到事件后按当前 hash route 过滤：

- Dashboard 处理 Signal/history/doctor/device/session 相关提示。
- History 处理 `history_appended` / `signal_emitted`。
- Signal 列表处理频道相关事件。
- Signal 详情仅处理当前 channel 匹配事件。
- Device 详情仅处理当前 deviceId 匹配事件。
- Region / Action 详情仅处理对应 regionId / actionId 匹配事件。

当前阶段采用“事件通知 + 当前页面静默局部 refetch”的稳定策略，并带 700ms 合并窗口和 pending guard，避免高频事件造成 API 风暴。浏览器标签页在后台时只记录 dirty route，回到前台后再刷新当前相关页面；刷新会保留滚动位置、筛选条件和折叠状态。6.8 不做全站轮询、不做 WebSocket 配置项、不做自动修复、不做配置写入。

更多实现说明见 `docs/WEBADMIN_REALTIME_SYNC_6_8.md`，回归测试见 `docs/REGRESSION_TEST_6_8.md`。

## WebAdmin 6.7 只读层稳定化 / 前端架构整理

6.7 是 WebAdmin 只读观察层稳定化阶段，不新增业务页面、不新增写 API、不接入 WebSocket。当前 WebAdmin 只读覆盖范围包括：

```text
/app#/dashboard
/app#/devices
/app#/devices/<deviceId>
/app#/signals
/app#/signals/<channel>
/app#/doctor
/app#/history
/app#/users
/app#/settings
/app#/regions
/app#/regions/<regionId>
/app#/actions
/app#/actions/<actionId>
```

本阶段把前端 HTML / CSS / JS 静态资源从 `WebAdminServer` 拆到 `WebAdminFrontendAssets`。`WebAdminServer` 继续只负责 HTTP request dispatch、auth/session、静态资源返回和 API route dispatch；页面脚本中的 formatter、icon、route、navigation 和 UI helper 仍作为浏览器端 helper 复用，不引入 npm、前端构建链、外部 CDN 或大型前端框架。

6.7 增加 WebAdmin readonly guard 到 `stabilizationGuardTest`：检查 app shell / CSS / JS assets 非空、所有只读页面 route 存在、时间格式化 helper、详情页上下文返回 helper、中文空状态、只读提示和基础危险字符串。该护栏用于避免后续 WebSocket、配置编辑、权限细化和多人协作阶段破坏 6.2～6.6 的只读观察层。

6.7 不包含：

- 实时同步 / WebSocket / Event Stream。
- 设备、Signal、Region、Action、用户或系统设置编辑。
- 配置写入、草稿 / 发布 / 回滚。
- ConditionEngine 或高层 GameController / MissionSystem。

后续阶段可以在当前只读 service / DTO / frontend helper 基础上继续推进 WebAdmin 实时同步，或先进入配置编辑服务层、权限审计和草稿发布模型。

## WebAdmin 6.6 Region / Action 只读页面

6.6 在 WebAdmin 中新增 Region 管理和 Action 系统只读页面：

```text
/app#/regions
/app#/regions/<regionId>
/app#/actions
/app#/actions/<actionId>
```

Region 管理页用于查看 RegionController 区域、世界、坐标边界、目标过滤、进入 / 离开 / 停留动作数量、绑定频道、当前玩家数量和 Doctor 状态。Region 详情页展示 bounds、中心点、尺寸 / 体积、目标过滤、事件动作摘要、绑定频道、当前玩家 / 最近事件和诊断摘要。

Action 系统页用于查看 ActionEngine 动作、动作类型、归属对象、关联 channel、引用次数、执行次数、最近结果和 Doctor 状态。Action 详情页展示动作基础信息、按类型整理的配置摘要、引用来源、最近执行记录和诊断摘要。

Region / Action 页面会与设备详情、Signal 频道详情、Doctor 问题和 History 历史记录形成只读跳转。详情页返回按钮使用上下文返回规则：从列表进入则返回对应列表，从 Signal / Region / Doctor / History 等交叉页面进入则返回进入前页面；直接打开详情 URL 或来源无效时，回退到对应模块列表页。

6.6 仍是只读阶段，不包含：

- 新增、编辑或删除 region。
- 修改 region bounds、target filter、enter / exit / stay actions。
- 新增、编辑、删除、执行或测试 action。
- 修改 action_relay、listener、channel 或其他设备配置。
- 保存配置、WebSocket 或新增写 API。

Region 与 Action 页面用于补齐 WebAdmin 对 RegionController 和 ActionEngine 的观测能力，并把区域、动作、Signal channel、设备和历史记录之间的只读跳转逐步连通。后续阶段将继续规划 WebAdmin 实时同步、编辑能力、ConditionEngine 和高层游戏调度系统。

## WebAdmin 6.5 用户管理 + 系统设置只读页面

6.5 在 WebAdmin 中新增用户管理和系统设置只读页面：

```text
/app#/users
/app#/settings
```

用户管理页用于查看当前世界 WebAdmin 用户、角色、启用状态、在线 / session 摘要、创建时间、创建者和最近登录时间。该页面只对 `OWNER` 开放；API 不返回 password hash、salt、session token、cookie value、明文密码或敏感密钥。

系统设置页用于查看 WebAdmin 服务运行状态、监听地址、端口、accessMode、当前访问 URL、当前登录用户、世界级存储目录摘要、安全配置摘要、审计日志状态和系统信息。非 `OWNER` 用户可以查看基础运行状态，但敏感存储路径会显示为受限信息。

6.5 仍是只读阶段，不包含：

- 创建、删除、禁用 / 启用 WebAdmin 用户。
- 重置密码、修改角色或踢出 session。
- 修改 host、port、accessMode、session 有效期或安全设置。
- 保存配置、WebSocket 或新增写 API。

写操作继续由 `/tzz webadmin` 命令和未来专门阶段管理。后续阶段会继续推进 WebAdmin 实时同步、编辑能力、ConditionEngine 和高层游戏调度系统。

## WebAdmin 6.4 Doctor + History 观测页面

6.4 在 WebAdmin 中新增全局 Doctor 诊断页和 History 历史页：

```text
/app#/doctor
/app#/history
```

Doctor 页面用于查看当前 SignalBridge、SignalDevice、WebAdmin 只读诊断问题。页面展示错误 / 警告 / 信息数量、受影响设备 / 频道、问题标题、关联对象、影响说明、建议操作和诊断代码，并支持关键词、严重级别、对象类型和跳转目标筛选。问题可跳转到相关设备、频道或历史视图；无法静态定位的项目会显示“暂无跳转目标”。

History 页面用于查看已有 Signal history 时间线。页面展示时间、事件类型、channel、来源对象、来源类型、玩家上下文、结果和详情，并支持关键词、channel、sourceType、result、时间范围和排序筛选。时间统一格式化为 `YYYY-MM-DD HH:mm:ss`，不会直接显示 ISO 原始字符串。

6.4 仍是只读观测阶段，不包含：

- 修复按钮或清除问题。
- 删除、导出或重放历史。
- 手动 signal emit 或测试触发按钮。
- 编辑设备、channel、listener、receiver、action_relay、action 或 region。
- 配置写入、WebSocket 或新增写 API。

后续 WebAdmin 实时同步 / WebSocket / Event Stream 会在未来专门阶段单独规划，配置编辑能力也会继续独立分阶段接入。

## WebAdmin 6.3 Signal 频道只读页面

6.3 在 WebAdmin 中新增 Signal 频道管理和频道详情逻辑链只读页面：

```text
/app#/signals
/app#/signals/<channel>
```

Signal 管理页读取 6.1 的只读 Signal API，展示频道列表、消费者统计、最近触发、最近来源和 Doctor 状态。页面支持按频道名搜索，并按消费者状态、最近事件 / 警告状态排序和筛选。所有筛选仅在前端只读数据上执行，不写入配置。

频道详情页展示频道基础信息、最近 Signal 事件、频道诊断摘要，以及“触发源 → 频道 → 消费者 → 动作 / 下游影响”的横向逻辑链雏形。消费者会区分 listener、signal_receiver 和 action_relay；如果消费者关联设备，页面可以跳转到设备详情。设备详情中的关联 channel 也可以跳转回频道详情。

WebAdmin 页面会把后端返回的 ISO 时间格式化为 `YYYY-MM-DD HH:mm:ss`，避免在表格、详情页和历史列表中直接显示原始 `T`、毫秒或 `Z` 字段。频道类型无法静态推断时，页面使用普通“频道”作为类型说明，避免在真实频道名下方显示误导性的“未知频道”。

6.3 仍然只读，不包含：

- 新增、编辑或删除 channel。
- 修改 listener、receiver、action_relay、device 或 action。
- enable / disable 操作。
- 手动 signal emit 或测试触发按钮。
- 图标编辑或上传。
- 配置写入、导出、WebSocket。
- 完整 Doctor 页面或完整 History 页面。

后续阶段可以在同一 DTO / service 基础上接入更完整的 Signal 频道逻辑链、Doctor 页面、History 页面和实时同步。

## WebAdmin 6.2 只读页面

6.2 将 WebAdmin 6.1 的只读 Service / DTO / API 接入浏览器端，提供第一批正式只读页面：

```text
/app#/dashboard
/app#/devices
/app#/devices/<deviceId>
```

Dashboard 展示服务器状态、设备数量、Signal channel 数量、最近 Signal history、Doctor 摘要、Region / Action 数量。设备管理列表展示设备名称、类型、世界、坐标、主频道、启用状态、最近触发和 Doctor 状态，并提供只读搜索与筛选。设备详情基础页展示设备状态、关联 channel、debug checks、Doctor 问题、最近事件和收敛后的配置摘要。

6.2 仍然是只读页面阶段，不包含：

- 新增、编辑、删除设备。
- 修改 channel。
- enable / disable 操作。
- 导出列表。
- Signal 频道详情 / 横向逻辑链页面。
- Doctor 完整页。
- History 完整页。
- WebSocket。
- 配置编辑或写 API。

设备详情中的“关联频道 / 逻辑链”入口仅显示后续阶段提示。Signal 频道管理和频道详情逻辑链计划放到 6.3；Doctor 完整页、History 完整页和实时同步会在后续阶段接入。

6.2 页面只调用 6.1 只读 API，不直接读写业务 JSON，不扫描世界，不强制加载区块，不触发 signal / action / region 行为，也不改变 5.x 已封版的 SignalBridge、SignalDevice、VirtualBlockDevice、ItemStackMatcher、itemSubmit、Doctor 或 debug 运行语义。
