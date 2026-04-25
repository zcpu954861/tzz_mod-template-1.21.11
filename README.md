# Tzz_mod

Tzz_mod（mod id: `tzz_mod`）是用于适配“全员逃走中”数据包和服务器玩法的 Fabric mod。模组提供手机、AR、地图区域、任务、封锁卡、动作执行和区域事件控制等服务端与客户端能力。

- 最新发布版本：`v1.5.0-signal-receiver`
- 当前开发版本：以 `gradle.properties` 的 `mod_version` 为准
- 作者：`zcpu`
- 目标 Minecraft：`1.21.11`
- 依赖：Fabric Loader `>=0.18.4`，Fabric API `0.141.3+1.21.11`
- 许可证：`CC0-1.0`

## 主要功能

- 手机系统：地图、聊天、任务、图库、呼叫管理员和设置等内置 App。
- AR 头显：提供空间化的应用入口和调试展示能力。
- 地图与区域工具：创建地图标点、规划区域，并同步到客户端地图。
- 任务配置器：配合数据包创建和编辑任务线。
- 封锁卡系统：保存触发条件和命令动作，并在命中实体或方块条件时执行。
- ActionEngine：统一执行命令、消息、音效等动作。
- RegionController：为已有规划区域绑定进入、离开、停留事件动作。

## 命令入口

当前主要命令入口已经统一到 `/tzz`：

```text
/tzz map ...
/tzz task ...
/tzz note ...
/tzz sendmsg ...
/tzz regionctl ...
/tzz signal ...
```

旧根命令已迁移到 `/tzz` 子命令下；当前代码不再注册旧的 `/map`、`/task`、`/note`、`/sendmsg` 根命令。

## SignalBridge

SignalBridge 是服务端事件桥 / 事件频道系统，用于把不同系统产生的事件通过 `signal channel` 串联起来。RegionController、封锁卡、密码机、感应板以及未来工具都可以通过 signal channel 联动，并最终由 listener 触发 ActionEngine 动作。

完整使用说明见 [docs/SIGNAL_BRIDGE.md](docs/SIGNAL_BRIDGE.md)。

### 基本示例

```text
/tzz signal listen create debug.test 测试监听器
/tzz signal listen addAction "测试监听器" command say 收到 debug.test
/tzz signal emit debug.test
```

### signal action 示例

```text
/tzz signal listen create area.a.enter A区进入监听器
/tzz signal listen addAction "A区进入监听器" command say 收到A区进入信号
/tzz regionctl addAction A区控制器 enter signal area.a.enter
```

### channel 规则

channel 是技术标识，会被规范化为小写，只允许小写字母、数字、`_`、`-`、`.`、`:`，长度为 1 到 128 个字符。

```text
area.a.enter
password.main.success
debug.test
```

SignalBridge 内置最大递归深度限制，防止 signal 无限触发自身。listener 也可以设置 `cooldownTicks`，用于限制高频触发。

### SignalEmitter 信号发射器

`signal_emitter` 是一个可放置的信号发射器方块。它可以绑定一个 SignalBridge channel，并在红石从未通电变为通电时发出 signal。

- 红石上升沿触发 signal。
- 持续通电不会重复触发。
- 断电后再次通电可再次触发。
- 右键方块可查看频道、启用状态、红石状态和位置。
- 可通过 `/tzz signal device` 命令配置。

设备命令：

```text
/tzz signal device bind <x> <y> <z> redstone.test
/tzz signal device info <x> <y> <z>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
```

5.2 阶段补充了设备管理命令：

```text
/tzz signal device list
/tzz signal device name <x> <y> <z> <name>
/tzz signal device clearName <device>
/tzz signal device info <device>
/tzz signal device history <device>
/tzz signal device debug <device>
```

`<device>` 可以是设备名称、完整 sourceId 或短 ID。设备名称包含空格时需要加引号，例如：

```text
/tzz signal device info "大厅拉杆发射器"
```

最小使用示例：

```text
/tzz signal listen create redstone.test 红石测试监听器
/tzz signal listen addAction "红石测试监听器" command say 收到红石信号
/tzz signal device bind <x> <y> <z> redstone.test
```

然后用拉杆或按钮给 `signal_emitter` 通电。SignalEventHistory 会记录来源为 `signal_device` 的事件。

### SignalReceiver 信号接收器

`signal_receiver` 是一个可放置的信号接收器方块。它负责把 SignalBridge channel 转换为红石输出：

```text
signal -> signal_receiver -> 红石输出
```

职责边界：

- `SignalListener` 是虚拟逻辑接收端，用于执行 command / message / sound / signal 等 ActionEngine 动作。
- `signal_receiver` 是世界实体红石接收端，只负责输出红石脉冲。
- `signal_receiver` 不负责执行命令，也不需要 channel 上存在 SignalListener 才能工作。
- 接收器只处理已登记且已加载区块中的方块实体，不扫描世界，也不强制加载区块。

新增命令：

```text
/tzz signal receiver pulse <x> <y> <z> <ticks>
/tzz signal receiver trigger <x> <y> <z>
```

`pulse` 用于设置红石输出脉冲时长，单位是 GT。默认 `5 GT`，常用范围建议 `2 GT` 到 `20 GT`。命令参数只输入整数，不输入 `GT` 后缀。

`/tzz signal device bind <x> <y> <z> <channel>` 现在同时支持 `signal_emitter` 和 `signal_receiver`。`device list/info/debug/test` 也会显示和操作接收器：

```text
/tzz signal device bind <x> <y> <z> door.a.open
/tzz signal receiver pulse <x> <y> <z> 5
/tzz signal receiver trigger <x> <y> <z>
/tzz signal device info <x> <y> <z>
/tzz signal device debug <device>
```

最小使用示例：

```text
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
/tzz signal receiver pulse <receiver-x> <receiver-y> <receiver-z> 5
/tzz signal emit door.a.open
```

也可以由 `signal_emitter` 发出同一 channel：

```text
/tzz signal device bind <emitter-x> <emitter-y> <emitter-z> door.a.open
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
```

之后给 `signal_emitter` 通电，`signal_receiver` 会收到 `door.a.open` 并输出红石脉冲。

Signal 设备管理索引保存到：

```text
world/tzz_mod/signal_devices.json
```

该文件用于管理显示名、位置、最近触发/接收和调试信息。`SignalEmitterBlockEntity` 仍然保存实际 `channel`、`enabled` 和 `lastPowered`；`SignalReceiverBlockEntity` 保存实际 `channel`、`enabled`、`pulseTicks` 和当前脉冲状态。设备历史来自内存中的 SignalEventHistory，不写入 JSON。设备管理不会扫描未加载区块。

### SignalBridge 可观测性命令

4.5 阶段补充了 SignalBridge 的只读观测与诊断命令，用于排查 signal 是否发出、channel 是否存在 listener、listener 是否处于冷却或存在递归风险。

```text
/tzz signal history
/tzz signal history <channel>
/tzz signal clearHistory
/tzz signal channels
/tzz signal channel info <channel>
/tzz signal listen debug <listener>
/tzz signal doctor
```

- `history`：查看最近 signal 事件，默认显示最近 10 条。
- `history <channel>`：只查看指定 channel 的最近 signal 事件。
- `clearHistory`：清空内存中的 signal 历史记录。
- `channels`：查看所有已知 signal channel，包括 listener 数量、动作数量和最近触发时间。
- `channel info <channel>`：查看某个 channel 的 listener 列表和最近事件。
- `listen debug <listener>`：查看单个 listener 的动作、冷却剩余、最近频道事件和直接递归风险。
- `doctor`：全局诊断 SignalBridge 配置问题，例如空动作 listener、无 listener channel、全部禁用 channel、直接 signal 自递归、异常 cooldown 和脏数据。

这些命令只用于查看、清理内存历史或诊断配置，不改变 SignalBridge 的 `emit`、listener 或 ActionEngine 执行语义。

## RegionController

RegionController 是“区域事件控制器”，用于让已有规划区域拥有逻辑触发能力：

```text
已有规划区域
-> 创建区域控制器
-> 玩家进入区域触发 enterActions
-> 玩家离开区域触发 exitActions
-> 玩家停留区域触发 stayActions
-> 动作通过 ActionEngine 执行
```

RegionController 不改变区域本身数据。`PlannerRegionData` 仍然只负责区域形状、名称、维度等地图数据；`RegionControllerData` 单独保存触发逻辑。

完整使用说明见 [docs/region_controller.md](docs/region_controller.md)。

### 快速示例

```text
/tzz regionctl regions
/tzz regionctl create <区域名称或区域ID> A区控制器
/tzz regionctl addAction A区控制器 enter command say 玩家进入A区
/tzz regionctl addAction A区控制器 exit command say 玩家离开A区
/tzz regionctl addAction A区控制器 stay command say 玩家仍在A区
/tzz regionctl stayInterval A区控制器 100
/tzz regionctl target A区控制器 all
/tzz regionctl test A区控制器 enter
```

### 触发对象过滤

- `all`：所有玩家触发。
- `op`：只有 OP 玩家触发。
- `tag <tagName>`：只有拥有指定 scoreboard tag 的玩家触发。

示例：

```text
/tzz regionctl target A区控制器 tag runner
```

### STAY 语义

`stayActions` 是玩家持续停留在区域内时周期触发的动作。

- 默认间隔为 `100 tick`。
- 最小间隔为 `20 tick`。
- 进入区域后不会立刻触发 `stay`，而是在达到间隔后触发。

### 事件语义

- 玩家第一次被扫描时，不触发 `ENTER`。
- 玩家退出服务器时，不触发 `EXIT`。
- 玩家跨维度时，对原区域触发 `EXIT`。
- 玩家传送跨过边界，也会触发 `ENTER` / `EXIT`。
- 区域边界是否算区域内，沿用现有区域几何判断。

### 配置文件

RegionController 配置保存到：

```text
world/tzz_mod/region_controllers.json
```

该文件由模组自动维护，不建议手动编辑，除非你熟悉当前 JSON 结构。

## 最小验收流程

1. 创建一个规划区域。
2. 执行 `/tzz regionctl regions`。
3. 执行 `/tzz regionctl create <region> 测试控制器`。
4. 添加 `enter` 动作。
5. 添加 `exit` 动作。
6. 执行 `/tzz regionctl test <controller> enter`。
7. 实际走入区域。
8. 实际走出区域。
9. 添加 `stay` 动作并测试。
10. 重启世界后确认配置仍存在。

## 物品与使用

- `phone`：右键打开手机界面。
- `ar_headset`：可装备到头部，右键打开 AR 界面。
- `attention`：右键播放提示音并将玩家朝向对齐到最近的 90 度方向。
- `*_blocking_card`：保存实体或方块触发配置，并在满足条件时执行动作。
- `blocking_card_configurator`：批量装入、取出和配置封锁卡。
- `password_config_card`：打开密码配置界面。
- `map_marker`：添加地图标点。
- `region_planner`：创建和编辑规划区域。
- `task_configurator`：创建和编辑任务配置。
- `signal_emitter`：可绑定 SignalBridge channel，并在红石上升沿发出 signal。

## 开发与构建

要求：JDK 21、Fabric Loader、Fabric API。

运行客户端：

```bash
./gradlew.bat runClient
```

构建：

```bash
./gradlew.bat build
```

完整验证：

```bash
./gradlew.bat clean build
```

构建产物位于 `build/libs/`。

## 贡献与许可

欢迎提交 Issue 和 Pull Request。建议先使用 `runClient` 本地调试。

许可证：`CC0-1.0`，详见 [LICENSE](LICENSE)。
