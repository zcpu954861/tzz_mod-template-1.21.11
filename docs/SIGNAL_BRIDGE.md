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
/tzz signal blockDevice itemCondition addSlotMatchFromSlot <x> <y> <z> <name> <targetSlot> <templateSlot> exactly <count> <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> at_most <count> <channel>
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
- `interactionCooldownTicks` 同时限制成功和失败反馈；冷却中不 emit、不反馈、不消耗、不阻止原版交互。
- 成功 / 失败 signal 都保留玩家上下文并走现有 SignalBridge 递归保护。

边界：

- 本阶段只增强 interactionItem 主手匹配。
- 不做玩家背包检测、副手匹配、装备栏 / 盔甲栏匹配、复杂多物品条件或 GUI。
- 不做通用 NBT 查询，不检测告示牌文字、命令方块命令、刷怪笼 NBT、任意 BlockEntity NBT、玩家 NBT 或实体 NBT。
- 未来 GUI / Admin UI 应覆盖 success/fail channel、message、sound、consume 和 consumeCount；也可以拆分成交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器和 debug/doctor 工具。

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
5.7 后也会显示 interaction 摘要、`interactChannel`、交互冷却、最近交互玩家和最近交互结果。5.8 后会显示 container 摘要、open / close / change channel、容器冷却、内容检查间隔、最近容器事件和最近结果。5.9 后会显示 itemCondition 数量、启用数量、最近物品条件触发和条件诊断。`device history` 可查看来源为 `virtual_block_device` 的红石、condition、interaction、container 和 itemCondition 触发记录。

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

以下内容只作为后续阶段计划记录，不在 5.10 MVP 实现：

- 玩家背包内是否包含匹配物品。
- 玩家副手物品匹配。
- 更复杂的消耗规则和失败提示策略。
- 匹配装备栏和盔甲栏。
- 6.0 / 7.0 GUI / Admin UI：通过配置界面管理 SignalBridge、SignalDevice、VirtualBlockDevice、RegionController 和 ActionEngine；容器槽位和物品条件不应长期依赖超长命令，未来应允许打开配置页面、选择槽位，并把目标物品放入配置槽作为匹配模板。

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
