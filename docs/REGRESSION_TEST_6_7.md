# 6.7 WebAdmin 只读层回归测试清单

本清单用于 6.7 WebAdmin Readonly Stabilization / Frontend Foundation 收尾前回归。6.7 不新增业务页面、不新增写 API、不做 WebSocket；重点确认 6.2 到 6.6 的只读页面、导航、格式化、中文显示和安全边界没有回退。

## 1. 构建与自动化护栏

执行：

```powershell
git diff --check
git grep -n "6\\.0\\.1\\|6\\.0\\.2" -- README.md CHANGELOG.md docs src
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期：
- `clean build` 通过。
- `stabilizationGuardTest` 输出 `Stabilization guard checks passed.`
- WebAdmin frontend assets guard 覆盖 app shell、CSS、JS、核心路由、时间格式化、返回导航和只读页面入口。
- 不出现 6.0.1 / 6.0.2 阶段编号残留。

## 2. 登录 / Session

准备命令：

```mcfunction
/tzz webadmin status
/tzz webadmin user create admin OWNER
/tzz webadmin user create viewer VIEWER
```

测试项：
- 访问 WebAdmin 根地址后可进入登录页。
- OWNER 用户可登录并进入 `/app#/dashboard`。
- VIEWER 用户可登录并访问只读页面。
- 错误密码登录失败，提示中文错误。
- 登出后访问 `/api/auth/me` 返回 401，页面跳回登录页。
- 刷新页面后 session 仍有效，过期或失效后会重新登录。
- 页面不显示 cookie、sessionId、passwordHash、passwordSalt。

## 3. Dashboard

路由：

```text
/app#/dashboard
```

测试项：
- 总览状态卡片显示服务器、设备、Signal、Doctor、Region、Action 摘要。
- 最近事件为空时显示中文空状态。
- Doctor 摘要可跳转 `/app#/doctor`。
- 最近 Signal / 最近事件入口可跳转 `/app#/history`。
- Region / Action 统计卡可跳转对应只读页面。
- 不显示 ISO 原始时间、`undefined`、`null`、raw enum。

## 4. Devices / Device Detail

路由：

```text
/app#/devices
/app#/devices/<deviceId>
```

建议准备：

```mcfunction
/tzz signal device list
```

如需设备数据，先在游戏内放置或绑定 signal_emitter / signal_receiver / action_relay / virtual_block_device，再使用当前项目命令创建或绑定 channel。

测试项：
- 设备列表可搜索、按类型、启用状态、Doctor 状态、世界筛选。
- 筛选器有明确中文 label。
- 设备类型、状态、Doctor badge 中文化。
- 列表不显示截断的 `minecraf` / `minecraft` 副标题。
- 点击设备进入详情页。
- 设备详情显示基础信息、关联频道、Debug、Doctor、最近事件、配置摘要。
- 原始字段默认折叠。
- 从设备详情跳转 Signal 频道详情后，返回按钮回到设备详情。
- 直接打开设备详情时，返回按钮 fallback 到设备列表。

## 5. Signals / Signal Detail

路由：

```text
/app#/signals
/app#/signals/<encodedChannel>
```

建议准备：

```mcfunction
/tzz signal emit webadmin.test
```

如果需要 listener / action 测试数据，请使用当前项目 TAB 补全确认命令格式，或让 Codex 输出当前实现的准确命令后再执行。

测试项：
- Signal 管理页可搜索、按消费者状态、事件状态排序筛选。
- 频道名、消费者、最近触发、Doctor 状态显示中文。
- 频道详情显示概览、基础信息、消费者分组、最近 Signal 事件。
- 横向逻辑链显示：触发源 → 频道 → 消费者 → 动作 / 下游影响。
- 不把 signal_receiver / action_relay 消费者误显示为触发源。
- channel 中包含 `.`、`:`、`/` 等字符时，路由 encode/decode 正常。
- 时间显示为 `YYYY-MM-DD HH:mm:ss` 或相对时间。
- 从 Signal 详情跳转设备 / action 后，返回按钮回到 Signal 详情。

## 6. Doctor

路由：

```text
/app#/doctor
```

测试项：
- Doctor 页面统计卡片、搜索、严重级别筛选、对象类型筛选正常。
- 问题列表中文可读，诊断代码作为次要信息显示。
- 不出现 `code=xxx` 作为主文本。
- 有 deviceId 的问题可跳转设备详情。
- 有 channel 的问题可跳转频道详情。
- 无跳转目标时显示“暂无跳转目标”。
- 从 Doctor 跳转详情页后，返回按钮回到 Doctor。
- 无问题时显示“当前没有诊断问题”。

## 7. History

路由：

```text
/app#/history
```

测试项：
- History 页面统计卡片、搜索、channel / sourceType / result / 时间范围筛选正常。
- 排序“最新优先 / 最旧优先”正常。
- 事件时间不显示 ISO 原始字符串。
- 空玩家、空详情、空来源显示中文 fallback。
- 有 channel 的事件可跳转 Signal 详情。
- 有 deviceId / routeTarget 的事件可跳转对应对象详情。
- 从 History 跳转详情页后，返回按钮回到 History。
- 页面不提供删除、导出、重放、emit 按钮。

## 8. Users

路由：

```text
/app#/users
```

测试项：
- OWNER 可查看用户统计、角色统计、在线/session 摘要、用户列表。
- VIEWER / 非 OWNER 显示权限不足，不泄漏用户列表和敏感字段。
- 搜索、角色筛选、启用状态筛选、在线状态筛选正常。
- 角色中文主显示：所有者、编辑者、测试者、查看者。
- 不返回或显示 passwordHash、passwordSalt、session token、cookie value、明文密码。
- 页面不提供创建、禁用、启用、重置密码、踢 session、修改角色按钮。

## 9. Settings

路由：

```text
/app#/settings
```

测试项：
- 显示 WebAdmin 服务状态、host、port、accessMode、访问 URL、当前用户/角色。
- OWNER 可查看 world-save scoped 存储目录、config/users/audit 文件状态。
- 非 OWNER 敏感路径信息隐藏。
- 安全配置显示认证方式、PBKDF2、session cookie 名称、session 有效期、审计状态。
- 不显示 hash、salt、session token、cookie value。
- 不提供修改 host、port、accessMode、session、安全设置按钮。

## 10. Regions / Region Detail

路由：

```text
/app#/regions
/app#/regions/<regionId>
```

如需 Region 测试数据，请使用项目当前 `regionctl` 命令创建最小区域；命令格式不确定时，请使用 TAB 补全或让 Codex 输出当前实现的准确命令。

测试项：
- Region 列表可搜索、按世界、启用状态、Doctor 状态、玩家状态筛选。
- Region 详情显示基础信息、bounds、target filter、enter / exit / stay 动作摘要、绑定 channel、最近事件、Doctor 摘要。
- channel 可跳转 Signal 详情。
- action 可跳转 Action 详情。
- 从 Region 详情进入 Action 详情后，返回按钮回到 Region 详情。
- 页面不提供新增、编辑、删除、修改 bounds、修改 target filter、修改 action 按钮。

## 11. Actions / Action Detail

路由：

```text
/app#/actions
/app#/actions/<actionId>
```

测试项：
- Action 列表可搜索、按 action 类型、owner 类型、执行结果、Doctor 状态筛选。
- Action 类型中文主显示：命令动作、消息动作、音效动作、信号动作、未知动作。
- Action 详情显示基础信息、配置摘要、引用来源、最近执行记录、Doctor 摘要。
- signal action 的下游 channel 可跳转频道详情。
- 从 Signal / Region / Doctor / History 进入 Action 详情时，返回按钮回到进入前页面。
- 直接打开 Action 详情时，返回按钮 fallback 到 Action 列表。
- 页面不提供执行、测试、编辑、删除 action 按钮。

## 12. Sidebar / Layout

测试项：
- sidebar 固定在左侧，不随主内容滚动。
- 主内容独立滚动，不遮挡 sidebar。
- 顶部栏布局不随滚动错位。
- sidebar 包含：总览、设备管理、Signal 管理、Doctor 诊断、历史记录、用户管理、系统设置、区域管理、动作系统。
- active 状态随 hash route 正确变化。
- 小屏幕下页面仍可操作，不出现关键文本重叠。

## 13. 详情页返回

测试项：
- `/app#/devices` → 设备详情 → 返回设备列表。
- `/app#/signals` → 频道详情 → 返回 Signal 管理。
- `/app#/regions` → Region 详情 → 返回 Region 管理。
- `/app#/actions` → Action 详情 → 返回动作系统。
- Signal 详情 → Action 详情 → 返回 Signal 详情。
- Region 详情 → Action 详情 → 返回 Region 详情。
- Doctor → 设备详情 → 返回 Doctor。
- History → 频道详情 → 返回 History。
- 设备详情 → 频道详情 → 返回设备详情。
- 直接打开任一详情页，返回 fallback 到对应列表页。
- 刷新详情页后点击返回，不跳到登录页、空白页或外部页面。

## 14. 时间格式与中文显示

测试项：
- 所有用户可见完整时间显示为 `YYYY-MM-DD HH:mm:ss`。
- 最近时间可显示“12 秒前 / 1 分钟前 / 3 小时前 / 暂无”。
- 不显示 `T`、毫秒、`Z`、`Invalid Date`。
- 页面不显示 raw `RUNNING`、`LOCAL_ONLY`、`enabled`、`channel`、`shortId`、`undefined`、`null` 作为主文本。
- 内部 ID 仅作为辅助信息，例如 `动作继电器（action_relay）`。

## 15. 只读边界

全站确认不存在：
- 新增 / 编辑 / 删除设备。
- 修改 channel。
- signal emit / 测试触发按钮。
- 新增 / 删除 / 修改 listener、receiver、action_relay。
- 新增 / 编辑 / 删除 region。
- 新增 / 编辑 / 删除 / 执行 action。
- 创建 / 删除 / 禁用 / 启用用户。
- 重置密码 / 修改角色 / 踢 session。
- 修改 host / port / accessMode / session / 安全设置。
- 删除历史、导出历史、重放事件。
- WebSocket / 自动实时同步。
- 配置写入。

## 16. WebAdmin API 只读安全

测试项：
- 未登录访问只读 API 返回 401。
- VIEWER 可访问 Dashboard、Devices、Signals、Doctor、History、Regions、Actions 等只读页面。
- 用户管理 API 对非 OWNER 不泄漏用户敏感信息。
- Settings API 对非 OWNER 隐藏敏感路径信息。
- API 不返回 passwordHash、passwordSalt、sessionId、cookie value、明文密码。
- WebAdmin 持久化目录仍为 `<world-save-root>/tzz/webadmin/`。
- 不恢复或写入全局 `config/tzz` WebAdmin 持久化文件。

