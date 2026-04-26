# Changelog

## v1.9.0-block-interaction

- 为 `virtual_block_device` 新增右键交互触发能力。
- 新增 `/tzz signal blockDevice interactChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearInteractChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice interaction <x> <y> <z> enable|disable`。
- 新增 `/tzz signal blockDevice interactionCooldown <x> <y> <z> <ticks>`。
- 新增 `/tzz signal blockDevice interactionInfo <x> <y> <z>`。
- 交互触发只对已登记的 `virtual_block_device` 坐标生效，不监听未绑定方块。
- 右键事件只检查被右键的一个坐标，不扫描世界、区块或周围方块。
- 默认只处理 `MAIN_HAND`，避免主副手双触发。
- 默认不阻止原版交互，箱子、门、按钮等原有右键行为继续执行。
- 成功触发 interaction signal 时，触发玩家会播放一次主手挥手动画。
- interaction 会带玩家上下文进入 SignalBridge / ActionEngine。
- `interactionCooldownTicks` 单位是 GT，命令参数只输入整数。
- 当前方块 ID 与绑定时 `blockId` 不一致时不触发，并在 info/debug 中提示 refresh 或重新 bind。
- `/tzz signal blockDevice info`、`/tzz signal device info`、`/tzz signal device debug` 会显示 interaction 摘要和诊断信息。
- `/tzz signal device history` 可查看 `virtual_block_device` 的交互触发记录。
- 保持 5.5 红石虚拟方块发射器和 5.6 BlockState condition 功能兼容。

## v1.8.0-blockstate-condition

- 为 `virtual_block_device` 新增方块状态条件触发能力。
- 新增 `/tzz signal blockDevice condition <x> <y> <z> <condition>`。
- 新增 `/tzz signal blockDevice clearCondition <x> <y> <z>`。
- 新增 `/tzz signal blockDevice conditionMode <x> <y> <z> condition_enter|condition_exit|condition_both`。
- 新增 `/tzz signal blockDevice conditionInfo <x> <y> <z>`。
- 支持完整 BlockState 条件字符串，例如 `minecraft:lever[powered=true]`、`minecraft:redstone_lamp[lit=true]`、`minecraft:repeater[delay=4]`。
- 条件设置时按当前运行时方块实际 `BlockState` 验证 block id、property 名和值，不硬编码 Wiki 属性白名单。
- 方块不支持的状态会拒绝保存，例如 `minecraft:stone[waterlogged=true]`。
- 非法状态值会拒绝保存，例如 `minecraft:repeater[delay=9]`。
- `condition_enter` 支持条件从不满足变为满足时 emit `channel`。
- `condition_exit` 支持条件从满足变为不满足时优先 emit `offChannel`，未设置时回退 `channel`。
- `condition_both` 支持进入和退出条件都触发；`clearOffChannel` 后退出边沿回退使用 `channel` 属于设计行为。
- `/tzz signal blockDevice info`、`/tzz signal device info`、`/tzz signal device debug` 会显示 condition 摘要与诊断信息。
- tick 检测仍只遍历已登记的虚拟方块发射器，每个设备只读取自己的一个坐标，不扫描世界、区块或周围方块。
- tick 时不重新解析 condition 字符串，状态不变不 emit，也不写 `signal_devices.json`。
- 5.5 红石虚拟方块发射器功能保持兼容。
- 5.7 交互触发、5.8 容器事件、5.9 多条件触发仍只是后续计划，本阶段未实现。

## v1.7.0-virtual-block-device

- 新增 `virtual_block_device` 虚拟方块发射器设备类型。
- 支持把任意已存在方块坐标手动绑定为 signal 触发源。
- 同时检测方块自身 `powered` 属性和该坐标接收到的红石强度。
- 支持 `redstone_rising`、`redstone_falling`、`redstone_both` 三种触发模式。
- 支持可选 `offChannel`，用于断电边沿发出不同 signal。
- 新增 `/tzz signal blockDevice bind <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice offChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearOffChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice mode <x> <y> <z> redstone_rising|redstone_falling|redstone_both`。
- 新增 `/tzz signal blockDevice info <x> <y> <z>`。
- 新增 `/tzz signal blockDevice test <x> <y> <z>`。
- 新增 `/tzz signal blockDevice unbind <x> <y> <z>`。
- 新增 `/tzz signal blockDevice refresh <x> <y> <z>`。
- `/tzz signal device list/info/debug/test/enable/disable` 现在支持 `virtual_block_device`。
- `/tzz signal device cleanup` 会清理已加载区块中已变为空气的虚拟方块发射器记录。
- 虚拟方块发射器只检测已登记坐标，不扫描世界、不扫描区块、不扫描周围方块、不强制加载区块。
- 状态不变时不 emit，不写 `signal_devices.json`。
- `signal_devices.json` 写入增加节流，服务端停止时强制保存。
- 文档记录 5.6 BlockState 条件、5.7 交互触发、5.8 容器事件、5.9 多条件触发的后续计划，本阶段未实现这些功能。

## v1.6.0-action-relay

- 新增 `action_relay` 动作继电器方块。
- 支持 `signal -> action_relay -> ActionEngine actions`。
- 新增 `ActionRelayBlockEntity`，保存 `channel`、`enabled`、`cooldownTicks`、`actions[]`、最近执行时间和最近结果。
- `actions[]` 使用现有 `ActionConfig` 格式，不新增 action 类型。
- 新增 `/tzz signal relay bind <x> <y> <z> <channel>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> command <command>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> message <message>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> sound <sound>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> signal <channel>`。
- 新增 `/tzz signal relay listActions <x> <y> <z>`。
- 新增 `/tzz signal relay removeAction <x> <y> <z> <index>`。
- 新增 `/tzz signal relay clearActions <x> <y> <z>`。
- 新增 `/tzz signal relay cooldown <x> <y> <z> <ticks>`。
- 新增 `/tzz signal relay trigger <x> <y> <z>`。
- 新增 `/tzz signal relay info <x> <y> <z>`。
- `/tzz signal device bind/list/info/debug/test/enable/disable` 现在支持 `action_relay`。
- 新增 `/tzz signal device cleanup`，用于清理已加载区块中的无效 Signal 设备索引。
- Signal 设备被破坏后会自动从 `signal_devices.json` 移除，避免 `/tzz signal device list` 残留旧设备。
- SignalBridge emit 现在会同时分发到 SignalListener、`signal_receiver` 和已加载的 `action_relay`。
- `action_relay` 不输出红石，保持 SignalReceiver 作为实体红石输出端。
- 保持 SignalListener 作为后台虚拟逻辑接收端，`action_relay` 是世界中可见的 ActionEngine 执行节点。
- `action_relay` 使用与前两个 Signal 设备一致的多元素科技风模型，并采用绿色主题与短暂 active 高亮。

## v1.5.0-signal-receiver

- 新增 `signal_receiver` 信号接收器方块。
- 支持 `signal -> signal_receiver -> 红石输出`。
- 新增 `SignalReceiverBlockEntity`，保存 `channel`、`enabled`、`pulseTicks`、`remainingPulseTicks`、最近接收时间和最近结果。
- `pulseTicks` 默认 `5 GT`，命令参数使用整数 tick，不解析 `GT` 后缀。
- 新增 `/tzz signal receiver pulse <x> <y> <z> <ticks>`。
- 新增 `/tzz signal receiver trigger <x> <y> <z>`。
- 新增 `/tzz signal receiver info <x> <y> <z>`。
- `/tzz signal device bind <pos> <channel>` 现在支持 `signal_receiver`。
- `/tzz signal device list/info/debug/test/enable/disable` 现在支持 `signal_receiver`。
- `signal_receiver` 不需要 SignalListener 也能工作，只处理已登记且已加载的接收器，不扫描世界或强制加载区块。
- SignalBridge emit 现在会同时分发到 SignalListener 和已加载的 `signal_receiver`。
- `signal_receiver` 使用与 `signal_emitter` 一致的多元素科技风模型，红色区分接收端，并支持 powered on/off 视觉状态。
- 保持 SignalListener 作为虚拟逻辑接收端，`signal_receiver` 只负责红石输出，不执行命令动作。

## v1.4.1-signal-device-management

- 新增 `signal_devices.json` 信号设备管理索引。
- 新增 `/tzz signal device list`。
- 新增 `/tzz signal device name <x> <y> <z> <name>`。
- 新增 `/tzz signal device clearName <device>`。
- 新增 `/tzz signal device info <device>`，同时保留坐标版 `info`。
- 新增 `/tzz signal device history <device>`。
- 新增 `/tzz signal device debug <device>`。
- 支持使用设备名称、完整 sourceId 或短 ID 引用信号设备。
- 设备列表与详情优先显示名称，并显示短 ID、位置、频道、启用状态和最近触发时间。
- 设备调试会提示未绑定频道、设备禁用、频道无 listener、方块未加载或注册表与 BlockEntity 状态不一致。
- Signal 设备历史仍来自内存 SignalEventHistory，不新增历史 JSON，不扫描未加载区块。

## v1.4.0-signal-emitter

- 新增 `signal_emitter` 信号发射器方块。
- 新增 `SignalEmitterBlockEntity`，支持保存 `channel`、`enabled`、`lastPowered`。
- 支持红石上升沿 emit signal。
- 持续通电不会重复 emit。
- 支持 `/tzz signal device bind/info/test/enable/disable`。
- 支持右键信号发射器查看状态。
- 支持无玩家上下文的 SignalBridge 触发。
- SignalEventHistory 可记录 `signal_device` 来源。
- 新增 `signal_emitter` 科技风模型与贴图。
- `signal_emitter` 支持 powered on/off 视觉状态。

## v1.3.1-signal-observability

- 新增 Signal 事件历史记录。
- 新增 `/tzz signal history` 和 `/tzz signal clearHistory`。
- 新增 Signal channel 总览和详情命令。
- 新增 `/tzz signal channels`。
- 新增 `/tzz signal channel info <channel>`。
- 新增 `/tzz signal listen debug <listener>`。
- 新增 `/tzz signal doctor`。
- 增强 `/tzz signal` 命令反馈可读性，统一添加青色分割线。
- 修复带空格监听器名称解析。
- SignalBridge 可观测性增强，不改变原有 emit / listener / ActionEngine 执行语义。

## 下一开发版本

- 统一命令入口到 `/tzz`。
- 新增 `/tzz regionctl` 区域事件控制器命令。
- 支持将已有规划区域绑定为区域控制器。
- 支持玩家进入区域触发 `enterActions`。
- 支持玩家离开区域触发 `exitActions`。
- 支持玩家停留区域周期触发 `stayActions`。
- 区域动作接入 `ActionEngine` 统一执行。
- 支持区域控制器启用、禁用、删除、动作测试、目标过滤。
- 支持 `all`、`op`、`tag` 三种触发对象过滤。
- 新增 `region_controllers.json` 持久化存储。
- `/tzz regionctl` 命令反馈已中文化、颜色化，并优先显示名称和短 ID。
- 保留封锁卡原有使用方式，封锁卡命令执行已接入 `ActionEngine`。
- 补充 RegionController 使用说明和最小验收流程文档。

- 新增 SignalBridge 事件桥系统。
- 新增 `/tzz signal` 命令。
- 支持 signal listener 创建、删除、启用、禁用、查看、测试。
- 支持 listener 绑定 command action。
- 支持 listener 绑定 signal action，实现链式事件。
- 支持 listener cooldown。
- 支持 signal 递归保护。
- RegionController `addAction` 已支持 signal 类型。
- 支持 RegionController -> SignalBridge -> Listener -> ActionEngine 联动。
- `/tzz signal` 命令反馈已中文化、颜色化、名称优先。
- 补充 SignalBridge 使用说明和常见问题文档。

## 1.1.5

- 同步当前 GitHub Release 版本。
- 保留现有手机、AR、封锁卡、地图、任务、密码、图库、笔记等功能。
