# 6.2 WebAdmin Dashboard + 设备管理只读页面

6.2 的目标是把 6.1 已完成的只读 Service / DTO / API 接入 WebAdmin 前端，形成第一批正式只读管理页面。本阶段仍然不做配置编辑、不做 WebSocket、不做设备新增/删除/修改，也不改变 5.x 与 6.0 / 6.1 的运行语义。

## 页面路由

6.2 使用 hash route，避免 JDK HTTP server 处理复杂前端路由：

```text
/app#/dashboard
/app#/devices
/app#/devices/<deviceId>
```

登录成功后默认进入 `/app#/dashboard`。`/app` 没有 hash 时会自动跳到 `#/dashboard`。刷新 `/app#/devices` 或 `/app#/devices/<deviceId>` 时，前端会按 hash 恢复页面。未登录或 session 过期时，前端会跳回 `/login` 并显示中文提示。

## Dashboard 数据来源

Dashboard 只调用 6.1 只读 API：

```text
GET /api/status
GET /api/devices
GET /api/signals/channels
GET /api/signals/history?limit=10
GET /api/doctor
GET /api/regions
GET /api/actions
```

页面展示：

- 服务器状态。
- 设备总数。
- Signal Channel 数量。
- Region / Action 数量。
- Doctor 错误 / 警告数量。
- 最近 Signal 触发。
- 设备概览。
- Doctor 前 5 条问题摘要。

Dashboard 不做复杂图表、不做日环比、不做增长率，也不做配置编辑。某个模块 API 失败时，其他模块仍会尽量展示，失败模块显示中文错误。

## 设备列表数据来源

设备管理页调用：

```text
GET /api/devices
```

页面展示：

- 设备总数、启用设备、禁用设备、有 Doctor 警告/错误的设备数。
- 搜索：设备名称、id、channel、坐标。
- 类型筛选：`ALL`、`SIGNAL_EMITTER`、`SIGNAL_RECEIVER`、`ACTION_RELAY`、`VIRTUAL_BLOCK_DEVICE`、`UNKNOWN`。
- 状态筛选：`ALL`、`ENABLED`、`DISABLED`。
- Doctor 筛选：`ALL`、`OK`、`INFO`、`WARNING`、`ERROR`、`UNKNOWN`。
- 世界筛选：根据返回数据中的 world 动态生成。
- 表格字段：图标、设备名称、类型、世界、坐标、channel、enabled、最近触发、Doctor 状态、查看详情。

设备列表只读，不提供添加、导出、删除、编辑、修改 channel、enable / disable 操作按钮。

6.2 发布前补齐的展示规范：

- 筛选器必须显示明确 label：设备类型、启用状态、诊断状态、世界 / 维度。
- 设备名称副标题不直接显示截断后的 raw id 或 namespace；需要显示 ID 时使用 `ID：<shortId>`，否则显示维度等有意义信息。
- 设备类型、启用状态、Doctor 状态使用中文主显示。
- 左侧 sidebar 在桌面端固定在视口左侧，主内容区域独立滚动。

## 设备详情数据来源

设备详情页调用：

```text
GET /api/devices/{id}
GET /api/devices/{id}/debug
GET /api/signals/history?channel=<device.channel>&limit=10
GET /api/doctor
```

页面展示：

- 设备名称、设备 ID、短 ID、类型、世界、坐标、启用状态、主频道、最近触发时间、Doctor 状态。
- 关联 channel 卡片与简化链路预览。
- Debug checks 中文展示。
- 最近 Signal history。
- Doctor 问题摘要。
- 按分组收敛后的配置摘要。

如果设备不存在，页面显示“设备不存在或已被删除”，并提供返回设备列表按钮。频道详情 / 逻辑链入口在 6.2 只显示后续版本提示，不实现完整频道详情页。

设备详情页默认只展示关键配置摘要，并按基础配置、信号配置、交互配置、容器配置、物品提交等分组。低价值或空字段不在摘要中铺开；原始字段放入“高级 / 原始字段”折叠区，默认收起。Debug 检查和 Doctor 问题均以中文显示，且状态应与设备列表保持一致。

## 只读边界

6.2 明确不包含：

- 设备新增、编辑、删除。
- 修改 channel。
- enable / disable 操作。
- Signal 频道管理完整页。
- 频道详情 / 横向逻辑链页。
- Doctor 完整页。
- History 完整页。
- 用户管理 Web CRUD。
- 系统设置编辑页。
- Region 页面。
- Action 页面。
- WebSocket。
- 配置写入。
- 多人协作锁。
- ConditionEngine。
- 高层 GameController / MissionSystem / PhaseController。

## 权限与错误处理

- Dashboard、设备列表、设备详情都要求 WebAdmin session。
- `VIEWER` 可以访问所有 6.2 只读页面。
- API 返回 `401` 时跳回 `/login`，并提示“登录已失效，请重新登录”。
- API 返回 `403` 时显示中文权限不足提示。
- API 返回 `404` 时显示对象不存在。
- API 返回 `500` 时显示服务器错误，不展示 Java stacktrace。
- 网络错误时显示“无法连接 WebAdmin 服务”。
- 页面不读取、展示或保存 password hash、salt、sessionId、cookie。

## 性能边界

- 前端只调用 6.1 只读 API。
- 后端不因为页面访问新增世界扫描。
- 不强制加载区块。
- 不触发 signal / action / region 行为。
- 不增加 tick 扫描。
- 不做高频轮询；6.2 默认由用户手动刷新。
- WebAdmin 持久化路径仍为 `<world-save-root>/tzz/webadmin/`，不恢复全局 `config/tzz`。

## 测试数据准备命令

创建 WebAdmin 用户：

```text
/tzz webadmin user create admin OWNER
/tzz webadmin user create viewer VIEWER
```

检查 WebAdmin 状态：

```text
/tzz webadmin status
```

准备 Signal history 的最小数据时，优先使用项目当前可用的 Signal 测试/触发命令。可先查看：

```text
/tzz signal
/tzz signal device list
/tzz signal device debug <device>
```

如果当前环境提供 signal emit/test 命令，可触发测试 channel，例如：

```text
/tzz signal test webadmin.test
```

若实际命令名称与上例不同，请以 `/tzz signal` 帮助提示为准。

准备设备数据：

1. 在游戏内放置 `signal_emitter`、`signal_receiver` 或 `action_relay` 方块，或绑定一个 `virtual_block_device`。
2. 使用当前项目已有的设备命令确认设备：

```text
/tzz signal device list
/tzz signal device info <device>
/tzz signal device debug <device>
```

如果需要测试 virtual block device，请先在游戏内选择或记录目标方块坐标，再使用当前项目已有 `/tzz signal blockDevice ...` 命令完成绑定。

准备 Region / Action 数据：

```text
/tzz regionctl
/tzz signal
```

Region 和 Action 的具体创建命令请以当前项目命令帮助为准。6.2 Dashboard 只读取 `/api/regions` 和 `/api/actions`，没有测试数据时应显示空状态，不应报错。

## 后续计划

6.3 建议进入 Signal 频道管理 + 频道详情逻辑链页面，复用 6.1 的 channel DTO 和 6.2 的 app shell / API client。
