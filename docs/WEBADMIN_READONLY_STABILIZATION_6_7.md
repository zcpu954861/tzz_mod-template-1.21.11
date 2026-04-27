# 6.7 WebAdmin 只读层稳定化报告

## 1. 版本定位

6.7 是 WebAdmin 只读层稳定化 / 前端架构整理版，目标版本为 `v1.25.0-web-admin-readonly-stabilization`。本阶段不新增业务页面、不新增写 API、不接入 WebSocket，也不改变 5.x 底层工具链语义。

本阶段定位类似 5.15：先稳定已经完成的 WebAdmin 只读观察层，为后续实时同步、配置编辑、权限细化、多人协作、草稿 / 发布 / 回滚打基础。

## 2. 当前只读页面覆盖范围

当前 WebAdmin 只读页面覆盖：

- Dashboard：`/app#/dashboard`
- 设备列表：`/app#/devices`
- 设备详情：`/app#/devices/<deviceId>`
- Signal 频道列表：`/app#/signals`
- Signal 频道详情：`/app#/signals/<channel>`
- Doctor 诊断：`/app#/doctor`
- History 历史：`/app#/history`
- 用户管理：`/app#/users`
- 系统设置：`/app#/settings`
- Region 列表：`/app#/regions`
- Region 详情：`/app#/regions/<regionId>`
- Action 列表：`/app#/actions`
- Action 详情：`/app#/actions/<actionId>`

这些页面均为只读观察页面，不提供创建、编辑、删除、执行、导出、配置写入或 WebSocket 能力。

## 3. WebAdminServer 整理结果

6.7 采用低风险的 Java 静态资源类拆分方案。

- `WebAdminServer` 继续负责 HTTP request dispatch、auth/session、静态资源返回和 API route dispatch。
- HTML / CSS / JS 静态资源集中到 `WebAdminFrontendAssets`。
- `/login`、`/app`、`/assets/app.css`、`/assets/app.js` 的访问路径不变。
- 登录页、session、`/api/auth/me`、`/api/status` 和 6.1～6.6 只读 API 语义不变。

本阶段没有引入 npm、前端构建链、外部 CDN 或大型前端框架。

## 4. 前端 helper / assets 整理结果

前端 helper 仍作为浏览器端 JS 复用，集中在静态脚本资源中：

- route / navigation：`navigateTo`、`withReturnContext`、`detailRoute`、`goBackOrFallback`、`backButton`。
- 时间格式化：`formatDateTime`、`formatRelativeTime`、`fmtTime`。
- display formatter：设备类型、Action 类型、owner 类型、source 类型、result、accessMode、role、Doctor 状态等中文显示。
- icon system：统一 2D inline SVG，不使用 PNG、3D 方块图、Minecraft 贴图或外部图标库。
- UI helper：统计卡、badge、empty / loading / error state、表格、筛选器、key/value、折叠 raw data。

详情页返回逻辑保持 6.6 规则：优先返回进入前页面；直接打开详情 URL 或来源无效时 fallback 到对应列表页。

## 5. 只读 API / DTO 一致性审查

本轮未改变 API 语义，继续使用已有只读接口：

- `/api/status`
- `/api/auth/me`
- `/api/devices`
- `/api/devices/{id}`
- `/api/devices/{id}/debug`
- `/api/signals/channels`
- `/api/signals/channels/{channel}`
- `/api/signals/history`
- `/api/doctor`
- `/api/regions`
- `/api/regions/{id}`
- `/api/actions`
- `/api/actions/{id}`
- `/api/webadmin/users`
- `/api/webadmin/settings`

审查结论：

- 未新增写 API。
- 未返回 passwordHash、passwordSalt、session token 或 cookie value。
- 前端继续将 null / empty 数据显示为“暂无”等中文空状态。
- 时间显示继续使用 `YYYY-MM-DD HH:mm:ss` 或相对时间，不直接展示 ISO 原始字符串。
- Doctor 状态、channel 跳转、详情页返回和只读边界保持一致。

## 6. 新增稳定化护栏

`stabilizationGuardTest` 增加 WebAdmin readonly frontend guard，覆盖：

- login HTML、app HTML、CSS、JS assets 非空。
- sidebar / app 中存在 Dashboard、Devices、Signals、Doctor、History、Users、Settings、Regions、Actions 路由。
- JS 中存在 `formatDateTime`、`formatRelativeTime`、`withReturnContext`、`goBackOrFallback`、`backButton`、`navigationButton`。
- Region / Action 导航中文文本存在。
- 中文空状态和只读提示存在。
- 不出现 `code=` 作为诊断主显示前缀。
- 不出现明显的 `>undefined<`、`>null<` 原始占位。
- 不出现跳向外部 URL 的 `location.hash='http...` 逻辑。

## 7. 仍存在的技术债

- 前端 JS 仍是单文件脚本，未来随着编辑能力、实时同步和权限细化增加，建议进一步拆成资源文件或更细的 Java asset provider。
- WebAdmin 仍未引入真实前端构建链；这是当前阶段的稳定性选择，但长期会限制模块化能力。
- 浏览器端 helper 目前主要通过字符串 guard 和人工回归保护，尚未执行真实 JS 单元测试。
- 实时同步、草稿 / 发布 / 回滚和配置编辑服务层尚未实现。
- 权限模型仍以现有角色边界为主，细粒度 Web UI 权限需要后续阶段设计。

## 8. 后续建议

建议后续阶段二选一推进：

- WebAdmin 实时同步阶段：设计 internal event bus、WebSocket / Event Stream、前端状态更新策略和权限审计。
- 配置编辑服务层阶段：先设计写操作 service / DTO / audit / validation / draft，再开放有限 Web UI 写能力。

ConditionEngine、高层 GameController / MissionSystem / PhaseController 仍适合单独阶段设计，不建议混入 WebAdmin 前端稳定化。
