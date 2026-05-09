# WebAdmin UI Refactor 7.6 Planning Notes

本文档记录 7.6 当前定义。若历史 7.6 规划与本文冲突，以本文和 `WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md` 的最新条目为准。

## 1. 7.6 名称

**WebAdmin Object Lifecycle + Client Selection Foundation**

## 2. 第一阶段 MVP

本阶段只做 WebAdmin 发起新建 `virtual_block_device` 的选择模式闭环：

- WebAdmin 在虚拟方块设备页打开统一 modal。
- 输入目标在线玩家、channel、显示名、备注、图标和 enabled。
- WebAdmin 调用安全写 API 开始选择 session。
- 目标玩家客户端进入选择模式 UI，不打开 Screen，不影响移动和视角。
- 选择模式 UI 需兼容 Minecraft 小窗口与不同 GUI scale，避免文字越界、重叠或遮挡核心视野。
- 右键任意方块完成选择，不要求空手。
- 选择模式阻断原方块交互、手持物品使用、背包和其它 GUI 打开。
- 服务端校验 session、维度、区块、非空气、非专用信号设备和 VBD 冲突。
- 服务端通过 `SignalDeviceStore` 创建 `virtual_block_device`，不修改世界方块。
- 只给目标玩家发送绿色“选择成功”聊天提示。
- 客户端退出选择模式。
- WebAdmin 通过 realtime 刷新并进入或提示新设备详情。

## 3. 第二阶段 MVP

第二阶段在同一 feature 分支上补齐最小 WebAdmin 对象生命周期入口：

- `virtual_block_device` 删除 / 解绑：只删除 `SignalDeviceStore` / registry 配置，不 setblock，不破坏世界方块，不删除其它 Signal 设备类型。
- `SignalListener` 新建：最小字段为 name/displayName、channel、enabled、cooldownTicks；默认 actions 为空。
- `SignalListener` 删除：删除该 listener 内嵌 actions，但不删除 channel、receiver、device 或历史记录。
- WebUI 使用 7.5 fixed modal、暗色 channel combobox、dangerous confirm modal、silent refresh 和安全 returnTo。
- 创建 listener 成功后进入 `#/listeners/<id>?returnTo=%23%2Flisteners`。
- 删除当前详情对象后返回对应列表页。

## 4. API

- `POST /api/webadmin/selection/start`
- `POST /api/webadmin/selection/cancel`
- `GET /api/webadmin/selection/status`
- `POST /api/webadmin/virtual-block-devices/{deviceId}/delete`
- `POST /api/webadmin/signal-listeners`
- `POST /api/webadmin/signal-listeners/{listenerId}/delete`

这些 API 必须接入现有 WebAdmin 写安全链路：EDITOR / OWNER 权限、CSRF、same-origin、`WebAdminWriteResult`、audit、validation 和 realtime。

## 5. 第一阶段网络与客户端

- S2C：开始选择、取消、失败、完成确认。
- C2S：完成选择、ESC 取消。
- 客户端模式参考 Camera APP 的非 Screen 模式：HUD overlay + keyboard/mouse mixin 阻断交互。
- 选择模式不设自动 timeout，只由 ESC、服务端取消、选择完成、断线或服务器停止结束。

## 6. 当前仍不做

- 不做 interaction item matcher。
- 不做 itemSubmit。
- 不做 consume / inventory / equipment。
- 不做 Action list 编辑。
- 不做 ConditionEngine。
- 不做 Scratch-like editor。
- 不做 GameController。
- 不做复杂客户端高亮 outline。
- 不修改 Figma。
- 不做 WebAdmin 全量对象生命周期。

## 7. 后续方向

7.6 后续仍需要单独规划：

- 更多对象生命周期入口。
- SignalListener action 新增 / 删除 / reorder。
- VBD 重新绑定、批量操作或其它 device 生命周期。
- 更通用的游戏内 / 客户端对象选择器。
- region、entity、container 等选择类型。
- interaction matcher、itemSubmit、ConditionEngine 与 Scratch-like editor。
