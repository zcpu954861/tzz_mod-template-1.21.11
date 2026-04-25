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

最小联动流程：

```text
/tzz signal listen create redstone.test 红石测试监听器
/tzz signal listen addAction "红石测试监听器" command say 收到红石信号
/tzz signal device bind <x> <y> <z> redstone.test
```

绑定后，用拉杆或按钮给 `signal_emitter` 通电即可触发 `redstone.test`。持续通电不会重复触发，断电后再次通电会再次触发。

右键信号发射器可以查看当前频道、启用状态、红石状态和位置。`test` 命令会使用执行命令的玩家作为上下文；红石自动触发时没有玩家上下文，SignalBridge 会以设备位置作为动作执行位置。SignalEventHistory 中会记录 `sourceType = signal_device`。

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
