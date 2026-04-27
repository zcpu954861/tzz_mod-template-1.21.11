# 6.1 WebAdmin 只读 Service / DTO 数据层

> 6.2 已开始把本页定义的只读 API 接入 Dashboard 和设备管理只读页面。页面层说明见 `docs/web_admin_dashboard_devices.md`。

## WebAdmin 存储作用域

从 6.1 开始，WebAdmin 所有持久化文件都存放在当前世界 / 当前存档根目录下：

```text
<world-save-root>/tzz/webadmin/
```

当前使用的文件包括：

```text
<world-save-root>/tzz/webadmin/web_admin_config.json
<world-save-root>/tzz/webadmin/web_admin_users.json
<world-save-root>/tzz/webadmin/web_admin_audit.log
```

WebAdmin 不再使用全局 `config/tzz` 作为持久化目录。每个世界拥有独立的 WebAdmin 用户、密码、访问模式、端口、安全设置和审计日志。单人世界 A 与单人世界 B 不共享 WebAdmin 用户；Dedicated Server 的 WebAdmin 设置也跟随当前 server world。

如果检测到旧版 `config/tzz/web_admin_config.json`、`config/tzz/web_admin_users.json` 或 `config/tzz/web_admin_audit.log`，当前版本只会在日志或 `/tzz webadmin status` 中提示，不会自动加载、复制、迁移或删除。确实需要迁移时，管理员应手动复制到目标世界的 `tzz/webadmin/` 目录。

6.1 的目标是为后续 WebAdmin 页面建立稳定只读数据层：

```text
Web UI
→ HTTP API
→ DTO
→ Readonly Service
→ 现有 5.x / 6.0 系统
```

本阶段不做 Dashboard 页面、不做配置编辑、不做 WebSocket，也不修改 5.x 已封版的 SignalBridge、SignalDevice、VirtualBlockDevice、ItemStackMatcher、RegionController、ActionEngine、Doctor 或 History 运行语义。

## 新增 API

所有接口都沿用 6.0 JSON envelope：

```json
{
  "ok": true,
  "data": {}
}
```

失败时：

```json
{
  "ok": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "请先登录。"
  }
}
```

### 设备

```text
GET /api/devices
GET /api/devices/{id}
GET /api/devices/{id}/debug
```

设备列表返回稳定摘要字段：

- `id`
- `displayName`
- `type`
- `world`
- `pos`
- `enabled`
- `channel`
- `lastTriggeredAt`
- `doctorStatus`
- `debugAvailable`

详情接口返回配置摘要、最近历史、诊断问题、debug 摘要和可导航链接。debug 接口返回结构化检查项，供后续设备详情页直接展示。

### Signal 频道

```text
GET /api/signals/channels
GET /api/signals/channels/{channel}
GET /api/signals/history
GET /api/signals/history?channel=<channel>&limit=50
```

频道列表和详情会合并已有 SignalListener、SignalDevice、SignalReceiver、ActionRelay、RegionController action 和 SignalEventHistory 中可低成本获得的频道信息。

当前不做完整图形逻辑链。`sources`、`listeners`、`receivers`、`actionRelays`、`actions`、`downstreamSignals` 等 DTO 字段是为 6.3 频道详情 / 逻辑链页预留的稳定结构。

History API 只读取现有内存历史，不改变持久化策略，不新增历史记录。

### Doctor

```text
GET /api/doctor
```

Doctor API 复用已有 `/tzz signal doctor` 和 5.15 结构化诊断基础，返回：

- `summary.errorCount`
- `summary.warningCount`
- `summary.infoCount`
- `summary.affectedDeviceCount`
- `summary.affectedChannelCount`
- `issues[]`

本接口不扫描世界、不强制加载区块、不扫描玩家背包，只检查已登记设备和已有内存状态。需要玩家上下文或真实世界状态才能判断的项目会保留为空、`UNKNOWN` 或由后续页面提示人工检查。

### Region

```text
GET /api/regions
GET /api/regions/{id}
```

Region API 读取 RegionController 配置和 Map planner region 的低成本边界信息，返回 region 名称、世界、边界、target filter、enter / exit / stay action 数量、绑定 signal channel 摘要和诊断状态。

本阶段不做区域编辑、重新选择、重新计算，也不为了 `playersInside` 做额外世界扫描。

### Action

```text
GET /api/actions
GET /api/actions/{id}
```

Action API 从 SignalListener、RegionController actions 和 ActionRelay 摘要中收集只读 action 信息。

如果某些 action_relay 详细配置只能从方块实体运行态获得且当前 store 只保存 action 数量，则 6.1 返回 `UNKNOWN` 或聚合摘要，不为了统计重构 ActionEngine 或 BlockEntity 存储。

## DTO 原则

- Web UI 不直接读取业务 JSON。
- Web UI 不直接暴露内部 Java 对象。
- 所有返回都经过 `webadmin.service` 和 `webadmin.dto`。
- 字段名保持稳定，适合作为后续前端页面和 WebSocket 增量同步的基础。
- 暂不可得的数据返回 `UNKNOWN`、`null` 或空数组，不触发昂贵计算。

## 权限规则

- 所有 6.1 新 API 都要求 WebAdmin session。
- 未登录返回 `401`。
- 对象不存在返回 `404`。
- 内部异常返回 `500`，但不向前端暴露 Java stacktrace。
- `VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可访问这些只读 API。
- DTO 不返回 `passwordHash`、`passwordSalt`、session token、sessionIdHash 或服务器文件绝对路径。

## 性能边界

6.1 API 必须保持：

- 不扫描世界。
- 不扫描区块。
- 不强制加载未加载区块。
- 不每 tick 扫描设备、容器、玩家背包或 region。
- 不因为 Web API 访问触发游戏逻辑。
- 不修改 cooldown、consume、itemSubmit、interaction lock 等 5.x 行为。
- 列表接口限制最大返回数量。
- DTO 只从已有配置、store、history、doctor/cache 中读取。

## 本阶段不包含

- Dashboard 页面。
- 设备管理页面。
- 设备详情页面。
- Signal 频道管理页面。
- 频道详情 / 逻辑链页面。
- Doctor 完整页面。
- History 完整页面。
- 用户管理 Web CRUD 页面。
- 系统设置编辑页面。
- Region 页面。
- Action 页面。
- WebSocket。
- 配置写入。
- 新增、删除或修改设备。
- 修改 channel、action 或 region。
- 多人协作锁。
- ConditionEngine。
- 高层 GameController / MissionSystem / PhaseController。

## 后续方向

建议 6.2 进入 Dashboard + 设备管理只读页面，直接使用 6.1 的 `/api/devices`、`/api/signals/channels`、`/api/doctor` 和 `/api/signals/history`。后续 6.3 / 6.4 可在同一 DTO 基础上补频道逻辑链、Doctor 页面、History 页面和 WebSocket 实时同步。
