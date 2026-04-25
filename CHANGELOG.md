# Changelog

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
