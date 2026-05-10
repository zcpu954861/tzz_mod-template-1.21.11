# WebAdmin Signal Physical Device Config 7.7 Current Context

## 当前分支

`feature/web-admin-signal-physical-device-config`

## 7.7 Step 1 目标

WebAdmin 补齐已放置物理 Signal 设备的配置能力，并落地 `action_relay` Action 列表查看与编辑。

物理设备包括：

- `signal_emitter`
- `signal_receiver`
- `action_relay`

这些是真实世界方块设备。WebUI 只能编辑安全配置，不能创建或删除这些方块。

## 本轮返修问题

用户浏览器验收发现：

1. 物理设备详情页请求 `/api/devices/<deviceId>` 返回 404。
2. `signal_emitter` / `signal_receiver` / `action_relay` 详情页只看到显示信息编辑，未看到有效配置能力。
3. `enabled` / `channel` / receiver `pulseTicks` / relay `cooldownTicks` 等既有配置项未正常暴露或不可用。
4. `action_relay` Action 列表可见，但新增 / 编辑 / 删除 / 排序按钮不可点击，并提示“区块未加载”。
5. command action 校验过度限制了地图控制类命令；`setblock` / `fill` / `clone` / `place` / `function` / `schedule` 应允许。

## 7.7 Step 1 checkpoint 状态

Step 1 已先落地并准备 checkpoint：

- `action_relay` Action list 基础查看 / 编辑框架。
- command / signal / message / sound action 基础支持。
- 物理设备不提供删除真实方块按钮。
- WebUI 不 `setblock`，不 `destroy block`。
- `signal_receiver` 仍保持 signal -> redstone pulse 语义，不做 action executor。
- WebAdmin permission / CSRF / same-origin / edit lock / `WebAdminWriteResult` / audit / realtime / smoke 基础链路。

Step 1 known limitations，必须在 Step 2 彻底解决：

- `pulseTicks` / `cooldownTicks` 等 tick 字段的 WebUI 编辑入口仍不够明确，必须补到清晰可见、可保存、可校验、可 audit、可 realtime 同步。
- `signal_emitter` / `signal_receiver` / `action_relay` 当前已有安全配置字段尚未全部形成 WebUI 完整闭环。
- `action_relay` block entity / chunk 状态提示仍需精修，尤其要解释“chunk 已加载但当前位置未找到 action_relay 方块实体”的真实原因。
- command action validation 需要继续放宽：除 `ban` / `kick` / `op` / `stop` 等极高风险服务器管理命令外，地图制作常用命令应允许。
- Step 2 硬标准：凡是报告支持编辑的字段，WebUI 必须有明确入口；没有入口就不能报告“已支持”。

## 必须落地的编辑能力

`signal_emitter`：

- displayName / note / iconKey
- enabled
- channel

`signal_receiver`：

- displayName / note / iconKey
- enabled
- channel
- pulseTicks

`action_relay`：

- displayName / note / iconKey
- enabled
- channel
- cooldownTicks
- Action list

如果 block entity 未加载，只有确实依赖 loaded block entity 的能力可以 disabled；metadata / basic config 不应因此被禁用。

返修落地要求：

- `enabled` / `channel` 作为 physical signal device basic config，不能因为 `action_relay` action list 需要 loaded block entity 而从配置 modal 中消失。
- 保存 basic config 时应优先同步 loaded block entity；如果 block entity 暂不可用，应至少更新 WebAdmin registry/store 快照并发布 realtime，UI 不应把基础配置整体禁用。
- `action_relay` action list 需要准确显示 world / chunk / block entity / type 状态，不能统一误报“区块未加载”。

## command action 校验

允许地图控制 / 玩法命令，包括但不限于：

- setblock
- fill
- clone
- place
- function
- schedule
- execute
- scoreboard
- tag
- title
- playsound
- particle
- effect
- give
- tp
- say
- tellraw
- summon

默认阻断极高风险服务器管理命令：

- ban
- ban-ip
- kick
- op
- deop
- stop
- whitelist
- pardon
- pardon-ip
- save-off
- save-on

命令校验只检查第一个命令词，不应误伤命令文本中的普通内容。

## 禁止事项

- 不给物理设备添加删除方块按钮。
- 不通过 WebUI setblock / destroy block。
- 不进入 matcher / itemSubmit / ConditionEngine。
- 不做 phone / task / blocking / password 联动。
- 不修改 Figma。
- 不处理或提交 `logs/`。
- 不 commit / push / merge / tag，除非用户后续明确要求。

## 验证

完成返修后必须运行：

```powershell
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

如果涉及 WebAdmin JS，`stabilizationGuardTest` 内的 route/render smoke 或等价 Node smoke 必须覆盖相关入口。
