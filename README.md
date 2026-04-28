# Tzz_mod

Tzz_mod（mod id: `tzz_mod`）是用于适配“全员逃走中”数据包和服务器玩法的 Fabric mod。模组提供手机、AR、地图区域、任务、封锁卡、动作执行和区域事件控制等服务端与客户端能力。

- 最新发布版本：`v1.32.0-web-admin-device-extended-config`
- 当前开发版本：`v1.33.0-web-admin-signal-listener-basic-editing`（7.4 WebAdmin Signal channel metadata + listener basic config editing；以 `gradle.properties` 的 `mod_version` 为准）
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
- Signal 设备：支持发射器、接收器和动作继电器，把红石、signal 与 ActionEngine 串联起来。
- WebAdmin：提供默认关闭的轻量 Web 管理入口，支持登录、session、只读 API、Dashboard、设备管理、Signal 频道、Doctor 诊断、History 历史、用户管理、系统设置、Region 管理、Action 系统只读页面，以及 7.0 起低风险设备显示元数据编辑闭环。

## 命令入口

当前主要命令入口已经统一到 `/tzz`：

```text
/tzz map ...
/tzz task ...
/tzz note ...
/tzz sendmsg ...
/tzz regionctl ...
/tzz signal ...
/tzz webadmin ...
```

旧根命令已迁移到 `/tzz` 子命令下；当前代码不再注册旧的 `/map`、`/task`、`/note`、`/sendmsg` 根命令。

## WebAdmin Foundation

### 7.2 WebAdmin Device Basic Config Editing

7.2 在 7.0 / 7.1 的安全写入链路基础上，开放第一批低风险但会影响游戏逻辑的设备基础配置编辑。当前只允许编辑：

- `enabled`：设备启用 / 禁用状态。
- `channel`：设备主频道 / primary channel。

这些字段会影响当前世界中的设备触发和 Signal 分发，因此所有写入都必须经过 WebAdmin session、`EDITOR` / `OWNER` 权限、CSRF / 同源校验、`device_basic_config` 编辑锁、`expectedFingerprint` 冲突检测、输入校验、结构化 audit 和 realtime 事件发布。

新增 API：

```text
GET /api/webadmin/device-basic-config/{deviceId}
PATCH /api/webadmin/device-basic-config/{deviceId}
```

`GET` 对已登录用户只读开放，返回当前 enabled、主 channel、是否支持编辑、当前 fingerprint 和锁状态摘要。`PATCH` 只允许 `EDITOR` / `OWNER`，必须携带有效 lockId 与 expectedFingerprint；冲突时返回 `conflict_detected`，不会覆盖服务器上的新配置。

7.2 不开放 `interactChannel`、success/fail/off channel、cooldown、pulseTicks、redstone mode、BlockState condition、interactionItem、itemSubmit、matcher、consume、action、command action、region bounds、用户或系统设置编辑。写入通过 `SignalDeviceStore` / domain service 路径执行，不允许前端直接改 JSON，并且必须保留 itemSubmit、interactionItem、container、itemConditions、redstone/condition 等既有字段。

更多说明见 `docs/WEBADMIN_DEVICE_BASIC_CONFIG_7_2.md`，回归测试见 `docs/REGRESSION_TEST_7_2.md`。

### 7.0 WebAdmin Editing Foundation

7.0 是 WebAdmin 配置编辑基础 / 最小安全写入闭环。本阶段只开放低风险 WebAdmin 设备显示元数据编辑：`displayName`、`note`、`iconKey`。这些字段只影响 WebAdmin 展示，不改变 Minecraft 游戏逻辑，不改变 SignalBridge、SignalDevice、VirtualBlockDevice、itemSubmit、RegionController 或 ActionEngine 的运行语义。

新增世界级 WebAdmin 元数据文件：

```text
<world-save-root>/tzz/webadmin/web_admin_device_metadata.json
```

新增 API：

```text
GET /api/webadmin/device-metadata/{deviceId}
PATCH /api/webadmin/device-metadata/{deviceId}
```

`GET` 要求登录，`VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可读取安全 DTO。`PATCH` 要求有效 WebAdmin session、`EDITOR` 或 `OWNER` 权限、CSRF / 同源写请求安全校验、JSON content、输入 validation、审计记录和 realtime 事件发布。写入结果统一使用 `WebAdminWriteResult`，校验失败返回 `validation_failed`，权限不足返回 `permission_denied`，无变化返回 `no_change`。

设备详情页新增“WebAdmin 显示信息”卡片。`EDITOR` / `OWNER` 可以编辑显示名称、备注和预设图标；`VIEWER` / `TESTER` 只能查看并看到权限说明。保存成功后发布轻量 `config_changed`、`device_config_changed` 和 `write_audit_appended` 事件，前端静默更新，不跳页、不丢滚动位置、不影响详情页返回上下文。

7.0 明确不开放 `enabled`、`channel`、`interactChannel`、success/fail channel、cooldown、redstone mode、interactionItem、itemSubmit、matcher、consume、action、command action、region bounds、用户或系统设置编辑。

更多说明见 `docs/WEBADMIN_EDITING_FOUNDATION_7_0.md`，回归测试见 `docs/REGRESSION_TEST_7_0.md`。

### 6.10 WebAdmin Write Foundation Stabilization

6.10 是 7.0 WebAdmin 配置编辑前的安全闸门，定位为写入前置稳定化 / 编辑阶段前总审查。本阶段不开放真实配置编辑，不新增公开可调用的配置写入 API，不写 JSON，不改变 5.x 已封版底层工具链语义，也不改变 6.2～6.8 已完成的只读观察层和 realtime 行为。

本阶段重点审查并补强 6.9 写入前置体系：权限矩阵、统一写结果模型、validation error 脱敏、CSRF / 同源写请求安全 helper、结构化审计模型、mutation service 规范、realtime 写入事件类型、前端只读边界和敏感信息保护。`stabilizationGuardTest` 增加 6.10 guard，用于确认 VIEWER / TESTER / EDITOR / OWNER 权限矩阵、CSRF token 校验、审计脱敏、写结果 code、realtime 写事件类型和前端无真实写入口。

6.10 结论用于判断是否可以进入 7.0。推荐 7.0 第一批只开放低风险编辑对象，例如设备名称 / 备注 / iconKey、基础 enabled 状态和基础 channel 字段；itemSubmit、action command、region bounds、用户 / 系统安全设置等高风险编辑应继续放到后续独立阶段。

更多说明见 `docs/WEBADMIN_WRITE_STABILIZATION_6_10.md`，回归测试见 `docs/REGRESSION_TEST_6_10.md`。

### 6.9 WebAdmin Write Permission / Audit / Service API Foundation

6.9 是 WebAdmin 配置编辑前的安全地基阶段，不开放真实配置编辑、不新增公开写 API、不写 JSON，也不改变 5.x 已封版底层工具链语义。

本阶段新增写操作统一结果模型、校验错误模型、权限矩阵、CSRF / 同源写请求安全 helper、结构化写操作审计模型、未来 mutation service 接口规范，以及写入相关 realtime 事件类型规范。新增的能力用于后续 7.0 WebAdmin 配置编辑复用，当前只作为基础设施和测试护栏存在。

新增只读能力接口：

```text
GET /api/webadmin/write/capabilities
```

该接口要求有效 WebAdmin session，只返回当前角色的未来写入能力摘要、CSRF 要求和 token，不执行任何写操作。`VIEWER` 仅只读，`TESTER` 预留测试 / dry-run，`EDITOR` 预留普通配置编辑，`OWNER` 预留用户、系统设置和危险操作。

更多说明见 `docs/WEBADMIN_WRITE_FOUNDATION_6_9.md`，回归清单见 `docs/REGRESSION_TEST_6_9.md`。

### 6.8 WebAdmin Realtime Sync Foundation

6.8 是 WebAdmin 实时同步基础阶段，不新增编辑能力、不新增写 API、不做配置写入。当前实现采用认证后的 Server-Sent Events / Event Stream：`GET /api/realtime/events`。

服务端新增轻量 realtime event bus。Signal history 追加时会发布 `signal_emitted` / `history_appended` 事件；WebAdmin 连接建立、断开和 heartbeat 也会发出轻量事件。事件只包含 channel、sourceType、summary、routeTarget 和少量 payload，不推送完整 devices/history/doctor DTO，不包含 password、hash、salt、session token 或 cookie。

前端登录后建立 realtime 连接，topbar 显示“实时同步”状态和最后事件时间。收到事件后按当前 hash route 过滤，并用节流后的当前页面静默局部 refetch 处理相关变化；浏览器标签页在后台时只记录 dirty route，回到前台后再刷新当前相关页面。不做全站轮询、不全页 reload，并保留滚动位置、筛选条件和折叠状态。

更多说明见 `docs/WEBADMIN_REALTIME_SYNC_6_8.md`，完整人工回归清单见 `docs/REGRESSION_TEST_6_8.md`。

### 6.7 WebAdmin Readonly Stabilization

6.7 是 WebAdmin 只读层稳定化 / 前端架构整理版，不新增业务页面、不新增写 API、不接入 WebSocket。

本阶段整理 WebAdmin 前端资源边界：`WebAdminServer` 继续负责 HTTP request dispatch、auth/session 和 API route dispatch，HTML / CSS / JS 静态资源集中到 `WebAdminFrontendAssets`。页面路径、登录、session、只读 API 和 world-save scoped WebAdmin 存储目录均保持兼容。

6.7 同时增加 WebAdmin readonly guard，纳入 `stabilizationGuardTest` / `clean build`，覆盖 app shell / CSS / JS assets 非空、Dashboard / Devices / Signals / Doctor / History / Users / Settings / Regions / Actions 路由存在、时间格式化 helper、详情页上下文返回 helper、中文空状态和只读提示等基础护栏。

更多说明见 `docs/WEBADMIN_READONLY_STABILIZATION_6_7.md`，完整人工回归清单见 `docs/REGRESSION_TEST_6_7.md`。

### 6.6 WebAdmin Region + Action

6.6 在 6.5 用户管理 / 系统设置只读页面基础上接入 Region 管理和 Action 系统只读页面：

```text
/app#/regions
/app#/regions/<regionId>
/app#/actions
/app#/actions/<actionId>
```

Region 管理页用于查看 RegionController 区域、世界、坐标边界、目标过滤、进入 / 离开 / 停留动作数量、绑定频道、当前玩家数量和 Doctor 状态。Region 详情页展示 bounds、目标过滤、事件动作摘要、绑定频道、当前玩家 / 最近事件和诊断摘要。

Action 系统页用于查看 ActionEngine 动作、动作类型、归属对象、关联 channel、引用次数、执行摘要和 Doctor 状态。Action 详情页展示动作基础信息、配置摘要、引用来源、最近执行记录和诊断摘要。

Region / Action 页面与 Signal、Doctor、History、设备详情之间支持只读跨页面跳转。详情页返回按钮会优先回到进入前的上下文页面；直接打开详情 URL 时会回退到对应模块列表页。

6.6 仍然只读：不提供新增、编辑、删除、执行 action、测试 action、修改 region bounds、修改 target filter、修改 enter / exit / stay actions、配置写入、WebSocket 或任何写 API。

### 6.5 WebAdmin Users + Settings

6.5 在 6.4 Doctor / History 只读观测页面基础上接入用户管理和系统设置只读页面：

```text
/app#/users
/app#/settings
```

用户管理页用于查看 WebAdmin 用户、角色、启用状态、在线 / session 摘要、创建时间和最近登录时间。该页面只对 `OWNER` 开放，不返回 password hash、salt、session token、cookie 或明文密码。

系统设置页用于查看 WebAdmin 服务运行状态、监听地址、端口、accessMode、世界级存储目录、安全配置摘要、审计日志状态和系统信息。非 `OWNER` 用户可以查看基础运行状态，但敏感存储路径会隐藏。

6.5 仍然只读：不提供创建用户、删除用户、禁用 / 启用用户、重置密码、修改角色、踢出 session、修改 host / port / accessMode、保存配置、WebSocket 或任何写 API。写操作仍通过 `/tzz webadmin` 命令和后续专门阶段谨慎开放。

### 6.4 WebAdmin Doctor + History

6.4 在 6.3 Signal 频道只读页面基础上接入全局 Doctor 诊断页和 History 历史时间线：

```text
/app#/doctor
/app#/history
```

Doctor 页面读取 6.1 的只读诊断 API，展示错误 / 警告 / 信息数量、受影响设备 / 频道、问题搜索、严重级别筛选、对象类型筛选和跳转目标筛选。问题列表以中文显示标题、影响、建议和诊断代码，并可跳转到相关设备、频道或历史视图。

History 页面读取已有 Signal history 只读 API，展示 Signal 事件时间线，支持按关键词、channel、sourceType、result、时间范围和排序筛选。时间显示统一为 `YYYY-MM-DD HH:mm:ss`，不显示 ISO 原始字符串。

6.4 仍然只读：不提供修复按钮、清除问题、删除历史、导出历史、signal emit、重放事件、配置写入、WebSocket 或任何设备 / channel / listener / action / region 编辑能力。

### 6.3 WebAdmin Signal Channels

6.3 在 6.2 Dashboard / 设备管理只读页面基础上接入 Signal 频道管理和频道详情逻辑链只读视图：

```text
/app#/signals
/app#/signals/<channel>
```

Signal 管理页展示频道总数、消费者数量、最近触发、Doctor 状态，并提供频道名搜索、消费者筛选、状态筛选和排序。频道详情页展示频道基础信息、最近事件、诊断摘要，以及“触发源 → 频道 → 消费者 → 动作 / 下游影响”的横向逻辑链雏形。

6.3 继续保持只读边界：不新增 channel，不编辑 / 删除 channel，不修改 listener、receiver、action_relay、device 或 action，不执行 signal emit，不提供配置写入，不接入 WebSocket。设备详情中的关联 channel 可以跳转到频道详情页；Dashboard 也提供进入 Signal 管理的入口。

### 6.2 WebAdmin Dashboard + Devices

6.2 将 6.1 的只读 Service / DTO / API 接入 WebAdmin 前端，提供第一批正式只读页面：

```text
/app#/dashboard
/app#/devices
/app#/devices/<deviceId>
```

登录后默认进入 Dashboard，总览服务器状态、设备数量、Signal channel 数量、最近 Signal 历史、Doctor 摘要、Region / Action 数量。设备管理页提供只读列表、搜索和筛选；设备详情页展示设备身份、关联 channel、debug checks、最近事件、Doctor 问题和配置摘要。

6.2 同时完成 WebAdmin 视觉与可读性整理：统一深色控制台风格、2D inline SVG 图标系统、固定 sidebar、中文筛选器标签、设备类型 / 状态 / Doctor badge 中文化、Debug 中文化、Doctor / Debug 状态一致性、配置摘要收敛和原始字段默认折叠。资源侧修复了 lang JSON、翻译 key 和关键模型 / 贴图加载问题。

本阶段仍然只读：不提供新增设备、编辑设备、删除设备、修改 channel、enable / disable 操作、配置写入或 WebSocket。前端只调用 6.1 只读 API，不扫描世界、不强制加载区块、不触发游戏逻辑。更多说明见 `docs/web_admin_dashboard_devices.md`。

### 6.1 WebAdmin Readonly Services

6.1 建立 WebAdmin 的只读 Service / DTO / API 数据层，面向后续 Dashboard、设备页、Signal 频道页、Doctor / History 页提供稳定后端结构。本阶段不做完整 Web 页面、不做配置编辑、不做 WebSocket，也不改变 5.x 已封版功能语义。

新增只读 API：

```text
GET /api/devices
GET /api/devices/{id}
GET /api/devices/{id}/debug
GET /api/signals/channels
GET /api/signals/channels/{channel}
GET /api/signals/history
GET /api/doctor
GET /api/regions
GET /api/regions/{id}
GET /api/actions
GET /api/actions/{id}
```

所有 6.1 API 都要求 WebAdmin 登录，`VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可访问这些只读接口。接口只通过 service / DTO 层读取现有系统状态，不直接读写业务 JSON，不扫描世界，不强制加载区块，不触发游戏逻辑，也不修改设备、频道、区域或动作配置。

从 6.1 开始，WebAdmin 持久化文件按当前世界 / 当前存档隔离，目录为 `<world-save-root>/tzz/webadmin/`。该目录包含 `web_admin_config.json`、`web_admin_users.json` 和 `web_admin_audit.log`。WebAdmin 不再读取全局 `config/tzz` 下的旧文件；如需迁移，管理员需要手动复制到对应世界的 `tzz/webadmin/` 目录。

更多说明见 `docs/web_admin_readonly_services.md`。

### 6.0 WebAdmin Foundation

6.0 WebAdmin Foundation 是 WebAdmin 后端地基与登录闭环，不是完整 WebAdmin Dashboard。本阶段默认关闭，不会自动公网开放，不改变 5.x SignalBridge / SignalDevice / `virtual_block_device` / ItemStackMatcher / itemSubmit / Doctor / debug 等既有逻辑。

当前能力：

- WebAdmin 配置文件：`<world-save-root>/tzz/webadmin/web_admin_config.json`（6.1 起按世界 / 存档隔离）。
- 默认 `enabled=false`、`host=127.0.0.1`、`port=18080`、`accessMode=LOCAL_ONLY`。
- 支持访问模式：`LOCAL_ONLY`、`LAN_DEV`、`MULTIPLAYER_DEV`。
- `LAN_DEV` / `MULTIPLAYER_DEV` 必须显式配置，启动日志和 `/tzz webadmin status` 会显示安全提示。
- WebAdmin 用户文件：`<world-save-root>/tzz/webadmin/web_admin_users.json`（6.1 起每个世界独立）。
- 用户密码使用 JDK 原生 `PBKDF2WithHmacSHA256` 保存，不保存明文。
- 初始密码由服务端随机生成，只在 `/tzz webadmin user create` 或 `resetPassword` 时显示一次。
- 登录成功后写入短期 `TZZ_WEBADMIN_SESSION` HttpOnly cookie。
- 浏览器访问 `http://host:port` 会打开登录页，登录后进入基础状态页。
- 已实现 API：`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`GET /api/status`。
- `/api/status` 返回 WebAdmin、Minecraft server、当前用户和基础版本状态。
- 服务器停止时释放 WebAdmin HTTP 端口。

WebAdmin 命令：

```text
/tzz webadmin status
/tzz webadmin user list
/tzz webadmin user create <username> <role>
/tzz webadmin user disable <username>
/tzz webadmin user enable <username>
/tzz webadmin user resetPassword <username>
```

权限边界：

- 控制台允许执行 `/tzz webadmin`。
- OP / 创造级管理员允许执行。
- 普通玩家禁止管理 WebAdmin 用户。
- 普通玩家不能通过游戏内命令创建 WebAdmin 账号。

本阶段暂不包含：

- 设备列表、Signal 频道页、逻辑链视图。
- Doctor 完整页、History 完整页。
- WebSocket、实时同步、配置编辑。
- 用户管理 Web 页面完整 CRUD。
- 区域、动作系统、节点编辑或多人协作锁。

后续 Web UI 原则保持 5.15 稳定化结论：

- Web UI 不直接读写 JSON。
- 命令、游戏内工具、Web UI 应共用服务层。
- 未来需要 DTO、权限、审计和 WebSocket 实时同步。
- 游戏内工具负责轻量绑定、选择和定位。
- Web Admin UI 负责全局逻辑视图、模块化卡片、实时调试和配置编辑。

## SignalBridge

### 5.15 Stabilization Foundation / GUI 前置整理版

Version marker: `v1.17.0-stabilization-foundation`.

5.15 是底层工具链稳定化 / GUI 前置整理版，不是新玩法功能版本。本阶段围绕 5.1 到 5.14 已完成的 SignalBridge、SignalDevice、`virtual_block_device`、ItemStackMatcher、consume 和 itemSubmit 链路做审查、测试护栏、诊断输出和 Web Admin UI 前置设计整理。

稳定化审查报告：

- `docs/STABILIZATION_AUDIT_5_15.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND2.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND3.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND4.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND5.md`
- `docs/STABILIZATION_AUDIT_5_15_FINAL.md`

自动化护栏：

- 新增 `stabilizationGuardTest`，已挂到 Gradle `check` / `build`。
- 执行 `./gradlew.bat clean build` 会自动运行稳定化护栏测试。
- 覆盖 `SignalDeviceData` 字段保留、旧 JSON 样本兼容、`ConsumePlan` / `ConsumePlanner`、`ItemSubmitEvaluator`、`InteractionDecisionEvaluator`、displayName 和 diagnostic DTO。
- 防止 interactionItem / itemSubmit / consume / cooldown / `require_item_match` 组合路径再次出现字段丢失、部分消耗、冷却绕锁等回归。

逻辑稳定化：

- 新增并接入 `ConsumePlan` / `ConsumePlanner`，消耗采用两阶段 plan / apply。
- 新增并接入 `ItemSubmitEvaluator` / `ItemSubmitEvaluationResult` / `ItemSubmitInventoryAdapter`，生产 itemSubmit 路径使用统一 evaluator。
- 新增并接入 `InteractionDecisionEvaluator` / `InteractionDecision`，明确区分原版交互放行、消耗执行和 signal / message / sound / history 等副作用。
- `cooldown` 不解除 `require_item_match` 锁，不跳过成功消耗，只抑制 signal / message / sound / 额外动画 / 高频 history 等副作用。
- `itemSubmit` 原子消耗保持两阶段：先完整模拟 consume plan，再统一 apply；任一 requirement 不足时不消耗任何物品。
- `interactionItem` 与 `itemSubmit` 保持互斥匹配模式：多物品提交启用时不再执行单物品 matcher / consume。

debug / doctor 结构化诊断：

- 新增 `DiagnosticSeverity`、`DiagnosticIssue`、`DeviceDiagnostic`、`InteractionItemDiagnostic`、`ItemSubmitDiagnostic`、`VirtualBlockDeviceDiagnosticService`。
- `/tzz signal device debug <device>` 会输出结构化诊断。
- `/tzz signal doctor` 增加设备层诊断摘要。
- 诊断输出已中文化、分组化，并保留机器可读诊断代码用于未来 Web UI / 高级排查。

GUI / Web Admin UI 前置原则：

- 未来 Web UI 不应直接读写 JSON。
- 命令、游戏内工具和 Web UI 应共用服务层，所有写操作走服务端统一服务。
- 后续需要 service / DTO / internal event bus / WebSocket 实时同步。
- 游戏内工具负责轻量初始化、绑定、选择和定位。
- Web Admin UI 负责全局逻辑视图、模块化卡片、实时调试、配置编辑、history 和 doctor。
- Web UI 最终必须覆盖所有可配置功能，不是命令系统的缩水版。

### 5.14 Consume Strategies / Multi-Item Submission MVP

Version marker: `v1.16.0-consume-submit`.

5.14 extends `virtual_block_device` right-click item matching with optional consume strategies and optional multi-item submission.

- `interactionItem` consume can use `matched_source`, `main_hand`, `off_hand`, or `inventory`.
- `main_hand`, `off_hand`, and `inventory_contains` can consume matched items when explicitly enabled.
- `armor_head`, `armor_chest`, `armor_legs`, `armor_feet`, and `armor_any` still reject consume; equipment / armor consume is not implemented.
- Inventory consume only reads and consumes the triggering player's main inventory / hotbar.
- `inventoryConsumeOrder` supports `hotbar_first` and `main_inventory_first`.
- Consume is atomic: the mod checks every required item before decrementing any stack.
- `itemSubmit` is disabled by default and must be enabled by an admin.
- `interactionItem` matcher and `itemSubmit` are mutually exclusive matching modes.
- Enabling `itemSubmit` automatically disables the single-item `interactionItem` matcher while preserving success/fail feedback configuration.
- `itemSubmit` requirements are captured from the admin's main hand and checked against the triggering player's main inventory / hotbar.
- All enabled `itemSubmit` requirements must match for submit success.
- When `itemSubmit` is enabled, submit requirements decide success and the single-item `interactionItem` matcher / consume path is not evaluated.
- `itemSubmit consume` is optional and atomically consumes all requirement items when enabled.
- `ignore` count mode does not take a count parameter and means the matcher does not check count; inventory matching still requires at least one matching stack.
- `require_item_match` remains a lock. In `itemSubmit` mode it locks based on submit success/failure; cooldown only suppresses signal/message/sound/history/extra animation side effects and does not unlock failed matches or skip enabled consume.
- No GUI, armor consume, backpack tick scan, world scan, ConditionEngine, or generic NBT path query is implemented in this phase.

New commands:

```text
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> matched_source
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> inventory
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> main_hand
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> off_hand
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> hotbar_first
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> main_inventory_first

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

Future plan only: 5.15 stabilization / GUI preparation, later ConditionEngine / ConditionGroup, and 6.0 / 7.0 GUI / Admin UI. These are not implemented in 5.14.

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
/tzz signal receiver info <x> <y> <z>
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

### ActionRelay 动作继电器

`action_relay` 是世界中可见的 ActionEngine 执行节点。它监听一个 SignalBridge channel，收到 signal 后执行自己保存的 `actions[]`：

```text
signal -> action_relay -> ActionEngine actions
```

职责边界：

- `SignalListener` 是后台虚拟逻辑接收端。
- `signal_receiver` 是世界实体红石输出端。
- `action_relay` 是世界中可见的 ActionEngine 执行节点。
- `action_relay` 不输出红石，也不是单纯命令方块；它执行的是 `actions[]`。
- `action_relay` 不需要同一 channel 上存在 SignalListener 才能工作。
- 动作继电器只处理已登记且已加载区块中的方块实体，不扫描世界，也不强制加载区块。

新增命令：

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

`cooldown` 的单位是 GT，默认 `0 GT`，表示无冷却。命令参数只输入整数，不输入 `GT` 后缀。

`/tzz signal device bind/info/list/debug/test/enable/disable` 现在也支持 `action_relay`。设备列表会显示动作数量、冷却时间和最近执行结果。

设备维护命令：

```text
/tzz signal device cleanup
```

`cleanup` 只检查 `signal_devices.json` 中已经登记的设备，并且只处理所在区块已加载的记录。如果已加载位置不再是对应类型的 Signal 设备，就会移除该索引记录；未加载区块会跳过，不扫描世界，也不强制加载区块。Signal 设备被破坏后也会自动从 `signal_devices.json` 中移除，powered / pulse / active 等同方块状态变化不会误删索引。

最小使用示例：

```text
/tzz signal relay bind <x> <y> <z> game.start
/tzz signal relay addAction <x> <y> <z> command say 游戏开始
/tzz signal emit game.start
```

`action_relay` 的 `actions[]` 直接使用 ActionEngine 的 `ActionConfig` 格式。后续 ActionEngine 增加新动作类型时，动作继电器可以继续复用同一套动作结构。`signal_devices.json` 继续作为设备管理索引，设备历史仍来自内存中的 SignalEventHistory，不新增永久 history JSON。

### Virtual Block Device 虚拟方块发射器

`virtual_block_device` 是虚拟方块发射器。它不是新方块，而是把管理员手动指定的已有方块坐标登记为 SignalBridge 触发源：

```text
已有方块的红石状态变化 -> virtual_block_device -> emit signal
```

它会同时检测该坐标方块自身的 `powered` 状态和该坐标接收到的红石强度。只有已登记坐标从未通电变为通电，或从通电变为未通电时，才会根据触发模式发出 signal。

新增命令：

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
```

触发模式：

- `redstone_rising`：未通电 -> 通电时发出 `channel`。
- `redstone_falling`：通电 -> 未通电时发出 `offChannel`；未设置 `offChannel` 时发出 `channel`。
- `redstone_both`：通电和断电边沿都触发；通电发出 `channel`，断电优先使用 `offChannel`，未设置 `offChannel` 时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式仍是 `redstone_both`，通电和断电都会发出主 `channel`，这是预期行为。

5.6 阶段为虚拟方块发射器增加了方块状态条件触发。它检测的是当前方块公开的 BlockState 属性，不检测方块实体 NBT、容器内容、告示牌文字或命令方块命令。

```text
/tzz signal blockDevice condition <x> <y> <z> <condition>
/tzz signal blockDevice clearCondition <x> <y> <z>
/tzz signal blockDevice conditionMode <x> <y> <z> condition_enter
/tzz signal blockDevice conditionMode <x> <y> <z> condition_exit
/tzz signal blockDevice conditionMode <x> <y> <z> condition_both
/tzz signal blockDevice conditionInfo <x> <y> <z>
```

条件使用完整 BlockState 字符串，例如：

```text
minecraft:lever[powered=true]
minecraft:oak_door[open=true]
minecraft:oak_stairs[waterlogged=true,facing=north]
minecraft:redstone_lamp[lit=true]
minecraft:repeater[delay=4]
minecraft:comparator[mode=subtract]
minecraft:wheat[age=7]
```

代码不会硬编码 Wiki 属性白名单，运行时以当前方块实际拥有的 `BlockState.getProperties()` 为准。方块不支持某个属性时会拒绝添加条件，例如 `minecraft:stone[waterlogged=true]`。属性值不在允许值中也会拒绝，例如 `minecraft:repeater[delay=9]`。

条件触发模式：

- `condition_enter`：不满足 -> 满足时发出 `channel`。
- `condition_exit`：满足 -> 不满足时优先发出 `offChannel`，未设置时发出 `channel`。
- `condition_both`：进入条件发出 `channel`，退出条件优先发出 `offChannel`，未设置时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式是 `condition_both`，进入和退出条件都会发出主 `channel`，这是预期行为。

5.7 阶段为虚拟方块发射器增加了右键交互触发。它只对已经登记为 `virtual_block_device` 的坐标生效，玩家右键该坐标方块时可以 emit 独立的 `interactChannel`。

```text
/tzz signal blockDevice interactChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearInteractChannel <x> <y> <z>
/tzz signal blockDevice interaction <x> <y> <z> enable
/tzz signal blockDevice interaction <x> <y> <z> disable
/tzz signal blockDevice interactionCooldown <x> <y> <z> <ticks>
/tzz signal blockDevice interactionInfo <x> <y> <z>
```

交互触发是事件驱动的，不通过 tick 轮询；默认只处理 `MAIN_HAND`，避免主副手双触发。它不会阻止原版右键行为：右键箱子仍会打开箱子，右键门仍会开关门，右键按钮或拉杆仍会正常响应，同时可发出 signal。成功触发 interaction signal 时，触发玩家会播放一次主手挥手动画。`interactionCooldownTicks` 单位是 GT，命令参数只输入整数，不输入 `GT` 后缀。

右键交互会带玩家上下文进入 SignalBridge / ActionEngine。当前方块 ID 与绑定时 `blockId` 不一致时不会触发，`interactionInfo` / `device debug` 会提示 refresh 或重新 bind。一个虚拟方块发射器可以同时配置红石、condition 和 interaction；如果这些触发都指向同一 channel，一次右键可能因原版状态变化和 interaction 同时产生多个 signal，这是可配置行为。

性能边界：

- 不扫描世界。
- 不扫描区块。
- 不扫描周围方块。
- 不自动寻找拉杆、按钮、压力板或红石灯。
- 不强制加载区块。
- 只检测 `signal_devices.json` 中登记过的 `virtual_block_device`。
- 每个设备每次只检测自己的一个坐标。
- 交互触发只检查被右键的一个坐标，不扫描世界、区块或周围方块。
- 不自动寻找可交互方块。
- 不在每次右键时遍历世界内容。
- 有 condition 时 tick 不重新解析 condition 字符串，只比较保存后的 property/value。
- 状态不变不 emit，也不写 JSON。
- `signal_devices.json` 写入已节流，服务端停止时会强制保存。

统一设备命令现在也支持虚拟方块发射器：

```text
/tzz signal device list
/tzz signal device info <device>
/tzz signal device debug <device>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
/tzz signal device cleanup
```

`device info` 和 `device debug` 会显示 condition 摘要与诊断信息。`cleanup` 对虚拟方块发射器采用保守策略：如果已加载位置变成空气，会删除记录；如果当前方块 ID 与绑定时不一致但不是空气，只在 debug 中提示，不自动删除。condition 无效时也不会自动删除记录，只会提示重新设置 condition 或 `clearCondition`。
`device info` 和 `device debug` 也会显示 interaction 摘要、交互冷却、最近交互玩家和最近交互结果。`device history` 可查看来源为 `virtual_block_device` 的红石、condition 和 interaction 触发记录。

5.8 阶段为虚拟方块发射器增加了容器事件触发。它不是通用 NBT 检测系统，只处理已绑定容器方块的打开、关闭和内容变化事件：

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

`containerOpenChannel` 和 `containerCloseChannel` 会在玩家实际打开或关闭对应容器 screen 时触发；`containerChangeChannel` 使用轻量内容指纹检测内容变化。MVP 指纹只包含每个 slot 的物品 registry id、数量和 damage，不做槽位物品条件、物品名称、lore、NBT 或数据组件匹配。

容器事件只对已登记的 `virtual_block_device` 生效，当前方块必须是容器。open / close 使用右键候选加实际 screen 状态确认，不把普通右键直接当作打开；change 只按 `containerChangeCheckIntervalTicks` 轮询已经配置 change channel 的绑定容器。容器事件会带玩家上下文；如果内容变化无法确定玩家，则允许无玩家上下文。

性能边界：

- 不扫描世界、区块或周围方块。
- 不自动寻找箱子、木桶、潜影盒或其他容器。
- 不强制加载区块，未加载区块直接跳过。
- open / close 按玩家实际 screen session 处理。
- content changed 只检查已登记且配置了 change channel 的一个容器坐标。
- 内容不变不 emit，也不写 `signal_devices.json`。
- `containerCooldownTicks` 和 `containerChangeCheckIntervalTicks` 单位都是 GT，命令参数只输入整数，不输入 `GT` 后缀。

职责边界：

- `signal_emitter`：专用方块，红石 / 交互 -> signal。
- `virtual_block_device`：已有方块，红石状态变化 -> signal。
- `signal_receiver`：signal -> 红石输出。
- `action_relay`：signal -> ActionEngine actions。
- `SignalListener`：后台虚拟逻辑接收端。

后续计划仍只记录，不在 5.8 实现：

- 5.10 物品数据 / NBT / 数据组件条件：匹配物品名称、lore、自定义数据、NBT 或新版数据组件。
- 6.0 / 7.0 GUI / Admin UI：通过配置界面管理 SignalBridge、SignalDevice、VirtualBlockDevice、RegionController、ActionEngine、容器/物品条件和游戏主线调度系统。

5.9 阶段为已绑定容器的 `virtual_block_device` 增加了容器槽位 / 物品条件触发。它不是物品 NBT 检测、数据组件匹配或 GUI 配置系统，只比较基础 item registry id 和数量：

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

- `slot_empty`：指定槽位为空时匹配。
- `slot_item`：指定槽位是指定 `itemId`，并且数量满足 `at_least`、`exactly` 或 `at_most`。
- `total_item`：统计整个容器内指定 `itemId` 的总数量，并按 `at_least`、`exactly` 或 `at_most` 判断。

触发规则：

- `condition_enter`：条件从 false -> true 时 emit `channel`。
- `condition_exit`：条件从 true -> false 时优先 emit `offChannel`；未设置时回退 emit `channel`。
- `condition_both`：进入条件 emit `channel`，退出条件优先 emit `offChannel`；未设置时回退 emit `channel`。
- 新增条件时会初始化 `lastMatched` 为当前匹配结果，避免设置瞬间误触发；`refresh` 可手动重新同步当前匹配状态。

性能和边界：

- 只对已绑定、已配置 `itemCondition` 的 `virtual_block_device` 生效。
- 当前方块必须是容器。
- 不扫描世界、区块或周围方块，不强制加载区块，不读取未绑定容器。
- slot 条件只读取指定 slot；total 条件只遍历该容器自身 slot。
- 内容不变不 emit；条件匹配状态不变不 emit；状态不变不写 `signal_devices.json`。
- 本阶段不比较 NBT、数据组件、lore、自定义名称或附魔，也不是通用 NBT 检测系统。
- 如果 `containerChangeChannel` 和 itemCondition channel 指向同一 channel，内容变化和条件边沿可能各自发出 signal，这是配置结果，不是 bug。

5.10 阶段新增可复用 `ItemStackMatcher`。容器物品条件和右键交互主手物品匹配共用同一套模板匹配逻辑，不再各自维护一套判断：

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
/tzz signal blockDevice itemCondition matcherCount <x> <y> <z> <name> ignore
```

```text
/tzz signal blockDevice interactionItem setFromHand <x> <y> <z>
/tzz signal blockDevice interactionItem clear <x> <y> <z>
/tzz signal blockDevice interactionItem enable <x> <y> <z>
/tzz signal blockDevice interactionItem disable <x> <y> <z>
/tzz signal blockDevice interactionItem option <x> <y> <z> matchLore enable
/tzz signal blockDevice interactionItem count <x> <y> <z> at_least <count>
/tzz signal blockDevice interactionItem count <x> <y> <z> ignore
/tzz signal blockDevice interactionItem info <x> <y> <z>
```

`slot_matcher` 会用模板匹配指定槽位；`total_matcher` 会统计容器内所有匹配模板的 ItemStack 数量。模板可以从执行者主手捕获，也可以从同一容器的某个槽位捕获。交互物品匹配只检查右键玩家的 `MAIN_HAND`，匹配成功才 emit `interactChannel`；不匹配时不阻止原版交互、不显示失败提示、不消耗物品。

当前 `ItemStackMatcher` 支持 item registry id、count、damage、自定义名称、lore、`custom_data` 和 data components 的整体快照匹配。默认只启用 item id 与数量规则；更严格的 damage / 名称 / lore / custom_data / components 需要管理员显式开启。本阶段不是任意 NBT path 查询系统，也不检测告示牌文字、命令方块命令、刷怪笼 NBT、BlockEntity NBT、玩家 NBT 或实体 NBT。

`ignore` 数量模式不接收数量参数，表示 matcher 不检查数量；info/debug 中显示“数量要求：不检查”。如果需要至少 2 个物品，应使用 `at_least 2`。`consumeCount` 是成功后消耗数量，和 `countMode=ignore` 无关，启用 consume 时仍会检查主手数量是否足够。

5.11 阶段增强了 interactionItem 主手匹配反馈。成功 / 失败频道、消息、音效和成功后消耗物品都可选配置，默认不显示消息、不播放音效、不触发失败频道、不消耗物品。`successChannel` 为空时成功回退使用 `interactChannel`；失败时 `failChannel` 为空则不 emit。成功和失败交互尝试都会播放 `MAIN_HAND` 主手挥手动画；冷却中不会 emit、不会反馈，也不会额外播放触发动画。当前 5.14 语义下，已启用的成功消耗属于开锁成本：匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。

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

5.11 的消耗只处理右键玩家 `MAIN_HAND`，不搜索背包、副手、装备栏或盔甲栏；物品数量不足以消耗时会进入失败流程。成功 / 失败 signal 都继续通过 SignalBridge emit，保留玩家上下文，并记录到 history。

5.12 阶段把 `interactionItem` 的检测来源扩展为可配置的玩家物品来源。默认仍是 `main_hand`，旧配置没有新字段时保持 5.10 / 5.11 行为；`off_hand` 和 `inventory_contains` 只有管理员显式配置后才启用。右键事件本身仍只处理 `MAIN_HAND`，`off_hand` 只是检查玩家副手物品，`inventory_contains` 只在玩家右键该绑定方块时检查该玩家自己的主背包 / 热键栏，不包含副手、装备栏或盔甲栏，也不会在 tick 中扫描。

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

5.13 阶段继续扩展 `interactionItem` 的玩家物品来源，新增 `armor_head`、`armor_chest`、`armor_legs`、`armor_feet`、`armor_any`。这些来源必须由管理员显式配置；右键事件仍只处理 `MAIN_HAND`，armor 来源只是读取触发玩家对应盔甲槽位的 ItemStack，不处理装备事件，也不做装备 / 盔甲消耗。`armor_any` 只检查头盔、胸甲、护腿、靴子四个盔甲槽，并记录第一个匹配槽位。

`inventory_contains` 会用同一套 `ItemStackMatcher` 先匹配非数量条件，再统计主背包 / 热键栏内匹配 ItemStack 的总数：`ignore` 表示至少存在一个匹配 stack，`at_least` / `exactly` / `at_most` 作用于总数量，其中 `at_most` 要求总数大于 0，避免没有物品也满足条件。消耗仍只支持 `main_hand`；source 为 `off_hand` 或 `inventory_contains` 时启用 consume 会被拒绝，旧数据中出现不兼容配置时运行时不会消耗，并会在 debug 中提示。

`consume` 仍只支持 `main_hand`；source 为 `off_hand`、`inventory_contains` 或任意 `armor_*` 时启用 consume 会被拒绝。旧数据中出现 `armor_*` source 同时 `consumeEnabled=true` 时，运行时不会消耗，并会按失败流程处理或在 debug 中提示。

`vanillaInteraction` 默认是 `allow`，保持旧行为：即使 interactionItem 匹配失败，也不阻止箱子、门、按钮、拉杆等原版右键行为。管理员显式设置为 `require_item_match` 后，它会作为锁定策略生效：只有 interactionItem 匹配成功才允许原版交互继续；匹配失败、空手不匹配或数量不足以 consume 时会返回阻止原版 use 的结果，不触发成功频道、不消耗物品。`interactionCooldownTicks` 不会让这个锁失效；冷却中匹配失败仍会阻止箱子打开、门开关、按钮/拉杆切换等原版交互。cooldown 只抑制 signal、message、sound、history / lastResult 和额外挥手动画；不会跳过已启用的成功消耗。匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。设备禁用、interaction 禁用、matcher 未启用、blockId 不一致、空气或未绑定方块仍保持 `PASS`。

门会按上下半格做最小归一化：如果管理员绑定门下半格，玩家右键上半格时会尝试匹配下半格设备；如果绑定上半格，右键下半格也会尝试匹配上半格设备。该逻辑只检查当前点击坐标和门的另一半坐标，不扫描世界，用于避免 `require_item_match` 被右键另一半门绕过。

性能边界保持不变：只检查被右键的一个坐标，不扫描世界、区块或周围方块，不强制加载区块；`main_hand` 只读主手，`off_hand` 只读副手，`inventory_contains` 只读触发玩家的主背包 / 热键栏，`armor_head` / `armor_chest` / `armor_legs` / `armor_feet` 只读对应盔甲槽，`armor_any` 只读四个盔甲槽，不读取其他玩家，也不在 tick 中检查装备。

后续计划仍只记录，不在 5.13 实现：

- 5.14 消耗策略 / 多物品提交，包括背包消耗、副手消耗和更复杂的提交规则。
- 复杂 ConditionEngine / ConditionGroup 后续单独设计。
- 5.15 稳定化 / GUI 前置整理版。
- 更完整的 GUI / Admin UI：所有 source、matcher、consume 和反馈配置未来都应进入 GUI；可拆分成交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器、debug/doctor 工具。

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
- `signal_receiver`：接收 SignalBridge channel 并输出红石脉冲。
- `action_relay`：接收 SignalBridge channel 并执行 ActionEngine actions。

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
