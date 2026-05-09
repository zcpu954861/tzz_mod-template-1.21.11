# WebAdmin UI Refactor 7.5 当前上下文

本文档是 7.5 WebAdmin UI Refactor 当前阶段的 Codex 交接 / 当前状态文档。

后续 Codex 任务应优先阅读本文档，再决定是否需要读其它文件。历史文档仅作背景，不代表当前实现状态。Figma 18 页是视觉参考稿，不是逐像素最终稿。当前 7.5 已从 Figma 打磨阶段进入前端代码落地阶段。

如果旧文档、旧截图、旧提示词与本文档冲突，以本文档和当前代码为准。

## 当前阶段

当前阶段是：

**7.5 WebAdmin Frontend Refactor / Step 5.5 文档同步与 7.6 规划准备**

### 2026-05-09 7.6 当前分支说明

当前后续开发已进入独立分支上的 7.6 第一阶段 MVP，名称为：

**WebAdmin Object Lifecycle + Client Selection Foundation**

第一阶段已完成并通过用户手动验收，checkpoint commit 为 `eab82bb`。第一阶段允许新增且仅新增 WebAdmin 新建 `virtual_block_device` 所需的选择模式 API、client selection payload、server-side in-memory selection session 和 WebAdmin modal 入口。范围限定为：

- WebAdmin 发起 `create_virtual_block_device` 选择 session。
- 指定目标在线玩家。
- 目标玩家客户端显示选择模式 UI。
- 选择模式 UI 必须兼容 Minecraft 小窗口与不同 GUI scale，文字需裁剪或自适应，不能遮挡准星或核心视野。
- 选择模式右键任意方块完成选择，不要求空手。
- 选择模式阻断原方块交互、物品使用、背包和其它 GUI 打开，但不影响移动和视角。
- ESC 取消。
- 服务端创建 `virtual_block_device`，不 setblock，不覆盖已有 VBD。
- 创建成功只给目标玩家发送绿色聊天提示。
- WebAdmin 通过 realtime 刷新并进入或提示新设备结果。

第二阶段当前范围是在同一 feature 分支上补齐最小对象生命周期：

- WebAdmin 删除 / 解绑 `virtual_block_device`：只删除 `SignalDeviceStore` / WebAdmin registry 配置，不 setblock，不破坏世界方块，不删除其它 Signal 设备类型。
- WebAdmin 新建 `SignalListener`：最小字段为 name/displayName、channel、enabled、cooldownTicks；默认 actions 为空，不创建 matcher、itemSubmit 或 ConditionEngine。
- WebAdmin 删除 `SignalListener`：删除该 listener 内嵌 action 引用，但不删除 channel、receiver、device 或历史记录。
- 新增 API 必须接入 EDITOR / OWNER 权限、CSRF、same-origin、`WebAdminWriteResult`、audit 和 realtime。
- UI 必须使用 7.5 fixed modal、暗色 channel combobox、dangerous confirm modal、silent refresh 和安全 returnTo。

7.6 第二阶段仍不做 interaction matcher、itemSubmit、consume / inventory / equipment、Action list 完整编辑、ConditionEngine、Scratch-like editor、GameController、phone/task/blocking/password 联动、region 编辑或 Figma 修改。

Step 1、Step 2、Step 2.5、Step 3 和 Step 4 已完成代码层落地。当前 Step 5 已补强 route / render / realtime 稳定化守卫；Step 5.5 只同步当前状态与 7.6 规划建议，不进入 7.6 代码实现。

Step 1 已落地 3 个代表页面：

1. Dashboard / 总览页
2. SignalBridge
3. Receivers / 接收器

这 3 页用于验证并沉淀：

- 新登录页
- 新 sidebar / topbar shell
- design tokens
- common components
- responsive layout
- table
- right rail
- pagination
- filter bar
- modal foundation
- icon registry / 纯色几何 SVG 图标
- disabled 未实现功能边界
- non-disruptive silent refresh

Step 2 第一批范围已落地：

1. 信号监听器页面
2. 动作列表
3. 信号设备
4. 虚拟方块设备
5. 事件历史

Step 2.5 范围：

- 将当前已落地页面的数据刷新机制从页面级 5 / 8 / 10 / 15 秒高频 polling，调整为 SSE 服务器事件驱动 + route dirty + silent refresh。
- 现有 `/api/realtime/events` 不新增 API；事件增加 `seq`，SSE 支持 `Last-Event-ID` / `lastEventId` 补发 recent buffer。
- recent buffer 缺失时发送 `sync_required`，前端执行当前 route full silent sync，不整页 reload。
- 前端处理 seq gap、reconnect sync、document hidden -> visible sync、offline -> online sync。
- 当前覆盖 Dashboard、SignalBridge、Receivers、信号监听器、动作列表、信号设备、虚拟方块设备、事件历史，以及已存在的 doctor / users / settings / regions 路由映射。

Step 3 第二批范围已在当前工作树落地：

1. 配置管理
2. 用户与权限
3. 系统设置
4. 区域列表
5. 区域控制器

Step 4 剩余页面与详情范围已在当前工作树落地：

1. 动作模板（`#/action-templates` / `#/templates`）
2. 信号诊断 Doctor（`#/doctor` / `#/diagnostics` / `#/signal-doctor`）
3. 频道详情（`#/signals/<encoded channel>`）
4. 监听器详情（`#/listeners/<id>` / `#/signal-listeners/<id>`，找不到稳定详情数据时显示 unavailable fallback）

Step 5 已补强 `stabilizationGuardTest` 对当前 7.5 route、render、disabled 写操作边界和 realtime mapping 的代码层护栏。

当前仍不是 18 页全量前端落地阶段，也不是 7.6 代码实现阶段。

## 当前最新同步

截至本阶段修复，前端落地继续保持只改 Java 内嵌前端资源，不新增后端 API：

- CSS / JS 资源版本参数已推进到 `7.5-step3-pages-batch2`，用于避免浏览器继续拿旧资源。
- Dashboard 概览卡已按统一 `data-nav-route` 导航模式支持整卡点击、hover、键盘 Enter / Space。
- SignalBridge 表格行已按统一 `data-nav-route` 导航模式支持整行点击到频道详情，详情按钮仍保留。
- 接收器表格行在已有设备详情路由可用时也使用同一行导航模式，不新增接收器详情页。
- Dashboard、SignalBridge、Receivers 和 shell 当前可见 WebAdmin 自定义图标已切换为 7.5 纯色几何 inline SVG registry，参考 Figma / 截图里的 2D 平面线性图标风格，不再使用上一轮复杂 image2 PNG、旧 SVG glyph、旧图标染色或 atlas。
- 当前 WebAdmin HTTP 服务仍只直接服务 `/assets/app.css` 和 `/assets/app.js`；自定义 WebAdmin 图标由 JS 内嵌 SVG registry 渲染，不新增后端图片 API，也不再读取独立图标图片资源。
- Receivers 页面不新增后端 API，已从现有设备详情 API 的 `configSummary.pulseTicks` 接入接收器脉冲时长；详情缓存用于补齐 `/api/devices` 列表缺失字段。
- Dashboard / SignalBridge / Receivers / Step 2 第一批页面不再依赖 5 / 8 / 10 / 15 秒高频 polling 追实时；统一通过 SSE 事件映射 route dirty，当前受影响 route 执行 non-disruptive silent refresh。接收器 silent refresh 仍会刷新当前可见行的详情缓存和 pulseTicks。
- Realtime 后端当前已接入 `seq`、recent buffer replay、`sync_required`，并在 Signal history append、device / receiver / virtual block device store 变化、Signal Listener store 变化、Action execution audit、Region controller store / runtime event 变化、WebAdmin 写服务已有审计与配置事件处发布事件。
- 已定义但不一定已有实际 publish 点的预留事件包括部分 `doctor_issues_changed`、`webadmin_settings_changed`、`webadmin_user_changed`、`action_changed`、`region_changed` 等；不要因为预留事件类型而新增业务功能。
- Step 2 第一批新增 / 更新 `docs/test/测试_7.5_WebAdmin前端重构第二阶段第一批页面验收.md`，记录 5 个页面、现有命令、disabled 写操作、silent refresh、row click 和响应式验收。
- Step 3 第二批只做前端主页面扩展，不生成新的验收 Markdown；配置管理、用户与权限、系统设置、区域列表、区域控制器均复用既有 readonly API 和 Step 2.5 realtime route dirty mapping。
- Step 4 动作模板页面复用 `/api/actions` 只读数据派生模板候选，不新增模板 API；添加模板、使用模板、导入模板、导出配置和批量操作均保持 disabled / unavailable。
- Step 4 Doctor 页面复用 `/api/doctor`，已接入 `#/diagnostics` / `#/signal-doctor` alias，自动修复、清空问题和导出报告均保持 disabled / unavailable。
- Step 4 频道详情继续复用已有 `/api/signals/channels/{channel}`，SignalBridge 行点击与详情按钮仍进入安全 encode 的 `#/signals/<encoded channel>`；特殊字符 channel 只按文本显示，不执行。
- Step 4 监听器详情不新增后端 API，复用频道详情中的 listener 数据和可选 listener basic config 只读查询；若找不到稳定 listener id / 详情数据，显示 unavailable fallback，不硬跳坏路由。
- 当前已有详情子页面已重新统一到 7.5 detail shell：设备详情、频道详情、动作详情、区域详情和监听器详情均使用 `wa-detail-shell`、详情 Header、Tabs、first row（基本信息 + 状态统计）、second row（列表 / 最近事件 / 分布 / 右侧信息栈）和默认折叠的“完整详情”卡片。频道详情和监听器详情只参考 Figma 的信息架构与视觉密度，不做像素照搬；详情页不再使用顶部一整排 metrics 或旧式纵向堆叠作为主结构。
- “完整详情”卡用于承载低频字段、runtime/debug、fingerprint/version/timestamp、configSummary 和 DTO 中不适合放进主信息卡的字段；默认折叠，展开后内部滚动，silent refresh 不应重置展开状态，也不能替代主布局。
- 已有安全编辑能力继续保留并迁移到 7.5 modal：device metadata、device basic config、device extended config、channel metadata、signal listener basic config 仍使用原有 CSRF / edit lock / WebAdminWriteResult 链路，不新增写 API。Modal 使用固定窗口、backdrop blur、淡入淡出、body 内部滚动和固定 header/footer；无后端支持的新增、删除、复杂编辑、动作链和导入导出仍保持 disabled / unavailable。
- 7.6 第二阶段新增的 VBD 删除 / 解绑、SignalListener 新建和 SignalListener 删除也必须走 WebAdmin 写安全链路：EDITOR / OWNER、CSRF、same-origin、`WebAdminWriteResult`、audit、realtime 和明确确认。VBD 删除只删配置不破坏方块；SignalListener 新建默认 actions 为空；SignalListener 删除不删除 channel、receiver、device 或历史记录。
- Step 5 稳定化已将动作模板、Doctor alias、频道详情、设备详情、动作详情、区域详情、监听器详情纳入 route / render / realtime smoke guard；detail smoke 会实际执行 `#/devices/test-device`、`#/signals/test.channel`、`#/actions/test-action`、`#/regions/test-region`、`#/listeners/test-listener`、`#/signal-listeners/test-listener`，并检查 detail shell、tabs、左右结构、折叠完整详情、modal silent refresh 和 realtime listener dirty mapping。继续禁止 `setInterval` 高频 polling 和整页 reload。
- 配置管理复用 `/api/webadmin/settings`、`/api/status`、`/api/webadmin/write/capabilities`，只读展示配置文件、服务、存储、审计和安全边界；发布、回滚、Diff、导入覆盖等真实配置版本系统仍未实现。
- 用户与权限复用 `/api/webadmin/users`，只读展示用户、角色、在线状态和权限概览；不实现真实用户权限写入、重置密码、踢出会话或删除用户。
- 系统设置复用 `/api/webadmin/settings`、`/api/status`、`/api/webadmin/write/capabilities`，只读展示平台信息、服务信息、功能开关和运行环境；不实现真实系统设置写入。
- 区域列表复用 `/api/regions?limit=500` 和已有 `#/regions/<id>` 详情路由；只读展示区域、坐标、范围、状态和统计。
- 区域控制器复用 `/api/regions?limit=500` 以 RegionController 视角展示 enter / exit / stay、目标过滤和动作数量；不新增 `/api/region-controllers`，不做 ConditionEngine 或复杂规则编辑。
- `/api/regions` 已修正为以 `MapDataStore` 中游戏内实际创建的 planner region 为基础数据源，再叠加 `RegionControllerStore` 中同 regionId 的控制器动作 / 状态信息；没有 RegionController 的游戏内区域也会出现在区域列表。
- planner region 新建、重命名、删除、颜色变化成功后会发布 `region_changed`，前端继续通过既有 Step 2.5 realtime route dirty 机制刷新区域列表，不新增后端 API 或区域写能力。

Step 2 第一批实现边界：

- 信号监听器页面不新增 listener 详情页；列表数据从现有 `/api/signals/channels` 与 `/api/signals/channels/<channel>` 聚合。
- 动作列表复用现有 `/api/actions` 与已有 `#/actions/<id>` 详情 route。
- 信号设备复用现有 `/api/devices` 与已有 `#/devices/<id>` 详情 route。
- 虚拟方块设备复用 `/api/devices` 过滤 `virtual_block_device`，可见行用已有 `/api/devices/<id>` 详情缓存补齐配置摘要；不使用 WebAdmin 图标伪造 Minecraft 原版材质。
- 事件历史复用 `/api/signals/history?limit=500`；无 history 详情页时不启用整行跳坏路由。
- Step 2 / Step 3 页面继续使用 route-level silent refresh，但不再靠页面级 5 / 8 / 10 / 15 秒高频 polling 追实时；刷新由 SSE event、route enter、reconnect、visible 和 online 补同步驱动。

## 当前主要修改文件

7.5 Step 1 主要改动集中在：

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java`

当前没有引入 React / Vite，仍使用 Java 内嵌前端资源结构。不要为了 7.5 Step 1 直接引入大型前端构建系统。不要为了并行随意拆大量页面文件。当前先轻量整理，不做大规模文件拆分。

职责边界：

- `WebAdminFrontendShell.java`：生成登录页 HTML 和登录后的 app shell HTML。
- `WebAdminFrontendStyles.java`：生成 WebAdmin CSS，包括 7.5 shell、tokens、页面组件、响应式规则。
- `WebAdminFrontendScripts.java`：生成 WebAdmin JS，包括 icon registry、hash route、API 调用、页面渲染、组件 helper、modal、silent refresh 相关逻辑。
- `WebAdminFrontendAssets.java` 当前主要是资源门面，委托以上三个类输出 HTML/CSS/JS。
- `WebAdminServer.java` 负责将 `/`、`/login`、`/app`、`/assets/app.css`、`/assets/app.js` 等路径映射到内嵌资源。

## 当前已完成方向

当前 7.5 Step 3 第二批已开始落地：

- 登录页正在迁移为 7.5 暗色品牌登录页。
- 登录后 app shell 正在迁移为 7.5 sidebar / topbar。
- Dashboard 已有 2 x 3 概览卡方向，并作为 Step 1 基线保持不回退。
- SignalBridge 已有全宽表格方向，并作为 Step 1 基线保持不回退。
- Receivers 已有 table + right rail 方向，并作为 Step 1 基线保持不回退。
- Step 2 第一批页面方向：信号监听器全宽列表，动作列表 / 信号设备 / 虚拟方块设备 / 事件历史使用 table + right rail 或全宽表格。
- Step 3 第二批页面方向：配置管理 / 用户与权限 / 系统设置 / 区域列表 / 区域控制器复用 table + right rail、tabs、只读设置卡和 disabled 操作边界。
- 当前可见 WebAdmin 自定义 UI 图标已转向 2D 平面、纯色、简单几何线条的 inline SVG registry；Minecraft 原版方块 / 物品仍使用原版材质资源。
- 无后端支持的写入类按钮应保持 disabled / unavailable。
- CSS/JS 资源需要使用版本参数或其它 cache-busting，避免浏览器继续加载旧资源。

注意：如果真实浏览器仍显示旧登录页或旧 shell，优先检查实际运行的 WebAdmin 进程是否已重启、浏览器是否缓存旧资源、`WebAdminFrontendShell.java` 的入口 HTML 是否真正生效，而不是只继续改页面主体组件。

## 当前已知问题 / 正在修复点

### 1. Dashboard 图标问题

之前 Dashboard 图标像旧图标或无色图标；后续 image2 atlas / PNG 版本又暴露出背景残留、图标偏移、纹理复杂和小尺寸辨识不稳定的问题。当前最新决策是：Step 1 可见 WebAdmin 自定义图标改用参考图风格的 2D 平面、纯色、简单几何线条 inline SVG。

要求：

- 图标必须有颜色、有语义，统一为 2D 平面纯色几何线条风格。
- Dashboard / SignalBridge / Receivers / shell 当前可见 WebAdmin 自定义图标应从统一 inline SVG registry 获取。
- 图标本体不带圆底、方底、背景图、阴影底板、纹理或卡片底；UI 圆形底、hover、glow 由 CSS 负责。
- 不使用 image2 位图、atlas、PNG data URI 或位图描摹 SVG 来承载 Step 1 当前可见自定义图标。
- 图标通过 `currentColor` 继承 CSS 语义色，CSS 负责尺寸、居中、外层气泡背景和 hover；图标显示尺寸保持适中，避免过大、过小或偏移裁切。
- Dashboard 的 6 张概览卡主图标和指标行图标都应为新的纯色几何 SVG 图标，不是旧图标染色。
- 不使用 emoji、字母、纯文本或奇怪符号冒充图标。

### 2. Dashboard 整卡点击

用户要求点击整个概览卡片都能跳转，不能只有“查看详情”文字可点击。

要求：

- 卡片需要 hover 状态。
- 卡片需要 `cursor: pointer`。
- 需要 keyboard access。
- “查看详情”仍可保留为明确 CTA。
- 没有已落地路由的卡片应 disabled / unavailable，不要假装可用。

### 3. SignalBridge 行点击

用户要求点击频道表格整行任意位置进入频道详情，不能只点详情按钮。

要求：

- 整行点击进入已有频道详情路由。
- “详情”按钮仍应可用。
- channel 必须安全 encode / decode。
- 特殊字符 channel 不能破坏路由、HTML 或 inline handler。
- Raw Channel 必须安全转义显示，不能执行用户输入字符串。

### 4. SignalBridge 图标

统计卡和消费者摘要里的图标必须使用新的纯色几何 SVG 图标。

消费者摘要顺序固定：

1. listener
2. receiver
3. relay
4. region

图标不能是无色旧图标，也不能挤压表格列。

### 5. Receivers 自动刷新

用户放置接收器后，接收器页应自动更新。

要求：

- 必须使用 non-disruptive silent refresh。
- 禁止全屏闪烁。
- 禁止整页 reload。
- 禁止重置滚动位置。
- 禁止重置筛选 / 分页 / 输入状态。
- 禁止关闭 modal。
- 优先由 SSE realtime event 驱动 route dirty 和 silent refresh。
- 不再用页面级 5 秒 polling 作为主刷新机制；设备变化应通过后端 publish realtime event 覆盖。
- 如果 `/api/devices` 列表缺少接收器扩展字段，允许复用已有设备详情 API，只刷新当前页面可见接收器行并缓存结果。
- pulseTicks 应参与静默刷新，用户把 signal_receiver 从默认 5 tick 改为 20 tick 后，列表应在 silent refresh 后显示真实值。
- 不新增后端 API。
- 全局 `setInterval` 可能被稳定化守卫禁止；当前使用 SSE / dirty flag / reconnect-visible-online 补同步。

### 6. 测试文档缺数据准备命令

新世界测试时，测试文档必须说明如何创建接收器 / 绑定 channel / 设置 pulseTicks / 触发测试。

要求：

- 不能写裸父命令。
- 命令必须来自当前 Brigadier 注册代码。
- 不要凭历史记忆猜命令。
- 写测试文档前先读当前命令注册代码。

## 硬性设计原则

### 分辨率 / 响应式

- Figma 1536 x 864 是视觉参考，不是前端固定尺寸。
- 禁止写死主页面 `width: 1536px` / `height: 864px`。
- 4K / 2K 大屏不能缩成一小块。
- 小屏不能重叠。
- 大屏内容自然扩展。
- 小屏 table container 可横向滚动。
- right rail 小屏可下移。
- filter bar 小屏可换行。
- pagination 始终在表格下方，不能压最后一行。

需要重点检查：

- 3840 x 2160
- 2560 x 1440
- 1920 x 1080
- 1536 x 864
- 1366 x 768
- 1280 x 720
- 1024 x 768

### Modal 规则

凡是涉及修改参数 / 修改配置 / 写入数据 / 危险确认，一律使用统一 animated modal。

Modal 要求：

- 外层使用 backdrop blur / 毛玻璃。
- overlay 半透明暗色。
- modal 固定设计尺寸，但受 viewport 限制。
- body 内容过多时内部滚动。
- header / footer 固定。
- danger action 使用红色风格。
- esc / close / cancel 行为一致。
- 不在主页面直接展开复杂编辑表单。
- 不在表格行内做复杂编辑。

### 后端边界

UI 里有些按钮只是产品方向，后端没有实现。

要求：

- 不允许因为 Figma 有按钮就新增 API。
- 不允许假装功能可用。
- 没有后端支持的按钮必须 disabled / unavailable。
- 高风险写操作必须等权限、CSRF、审计、edit lock、`WebAdminWriteResult` 完整后再启用。
- 危险操作不能只靠前端按钮样式保护。

当前尤其不要启用：

- 配置管理发布 / 回滚 / Diff 写操作
- 动作模板 CRUD
- 动作编辑
- 用户权限高级管理
- 系统设置写入
- Doctor 自动修复
- 批量导入 / 批量导出 / 批量操作
- 虚拟方块设备复杂条件编辑
- matcher / itemSubmit / interactionItem 新能力

### 图标规则

- Step 1 当前可见 WebAdmin 自定义图标使用 2D 平面、纯色、简单几何线条 inline SVG。
- Dashboard / SignalBridge / Receivers / shell 当前可见图标必须使用统一 SVG icon registry。
- 当前可见 WebAdmin 自定义图标不能带圆底、方底、背景图、抠图残留、复杂纹理或位图噪声。
- UI 圆形底、卡片底、hover 和 glow 由 CSS 实现，不画进图标本体。
- 不再使用上一轮 image2 PNG、atlas、PNG data URI 或从位图自动描摹出来的复杂单色路径。
- 图标应统一从 icon registry 取，不要散落字符图标、裸图片路径或旧 glyph。
- 不允许 emoji。
- 不允许字母、纯文本、奇怪符号作为最终图标。
- Minecraft 原版方块 / 物品图标必须使用原版材质，不允许 image2 重画。

### Realtime / silent refresh

- 不整页刷新。
- 不闪屏。
- 不重置滚动。
- 不重置筛选。
- 不重置分页。
- 不关闭 modal。
- 不重置用户当前选择。
- `document.hidden` 时可暂停或标记 dirty，重新 visible 后 silent refresh。
- 页面刷新数据时应尽量保留输入焦点和筛选状态。

## 当前 Step 1 禁止事项

当前 7.5 Step 1 禁止：

- 不修改 Figma。
- 不新增后端 API。
- 不新增业务功能。
- 不扩展剩余 15 页。
- 不做子详情页。
- 不做 matcher / itemSubmit。
- 不做 Scratch-like editor。
- 不做 ConditionEngine。
- 不做大规模文件拆分。
- 不 commit / push / merge / tag，除非用户明确要求。
- 不启用没有后端支持的写操作。
- 不恢复底部版权行。
- 不用截图或背景图伪装 UI。
- 不用固定 1536 x 864 画布硬搬 Figma。

## 当前测试入口

主要浏览器入口：

- `http://127.0.0.1:18080/`
- `http://127.0.0.1:18080/app#/dashboard`
- `http://127.0.0.1:18080/app#/signals`
- `http://127.0.0.1:18080/app#/signalbridge`
- `http://127.0.0.1:18080/app#/receivers`

常用构建和守卫测试：

```powershell
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

建议额外检查：

- 生成出的 `/assets/app.css` 是否包含 7.5 shell / page styles。
- 生成出的 `/assets/app.js` 是否包含 icon registry、Dashboard、SignalBridge、Receivers 渲染逻辑。
- `/login` 和 `/app` HTML 是否来自 `WebAdminFrontendShell.java` 的当前内容。
- 浏览器是否实际加载带版本参数的 CSS/JS。
- 修改内嵌资源后需要重启实际 WebAdmin / Minecraft 进程。

## 历史文档使用说明

旧 5.x / 6.x / 7.0-7.4 文档仍可作为历史背景，但 WebAdmin 7.5 当前实现状态必须以本文档和当前代码为准。

如果旧文档与本文档冲突，以本文档为准。

不要根据旧文档认为：

- 所有 Figma 按钮都有后端。
- 18 页都已经前端落地。
- WebAdmin 自定义图标与 Minecraft 原版材质图标可以混用同一规则。
- 可以写死 1536 x 864。
- 当前可以启用高风险写操作。
- 当前可以新增 matcher / itemSubmit / Scratch-like editor / ConditionEngine。

可能容易误导的历史文档包括但不限于：

- `docs/WEBADMIN_READONLY_STABILIZATION_6_7.md`
- `docs/WEBADMIN_REALTIME_SYNC_6_8.md`
- `docs/WEBADMIN_WRITE_FOUNDATION_6_9.md`
- `docs/WEBADMIN_WRITE_STABILIZATION_6_10.md`
- `docs/WEBADMIN_EDITING_FOUNDATION_7_0.md`
- `docs/WEBADMIN_EDIT_LOCKS_7_1.md`
- `docs/WEBADMIN_DEVICE_BASIC_CONFIG_7_2.md`
- `docs/WEBADMIN_DEVICE_EXTENDED_CONFIG_7_3.md`
- `docs/WEBADMIN_SIGNAL_LISTENER_BASIC_CONFIG_7_4.md`
- `docs/REGRESSION_TEST_7_0.md` 到 `docs/REGRESSION_TEST_7_4.md`

这些文档描述的是历史阶段能力、测试或边界，不等于 7.5 当前 UI refactor 的实现范围。

## 后续 Codex 起手建议

后续新任务建议先执行：

```text
先阅读 docs/WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md，再执行本任务。历史文档只作背景；如有冲突，以该 current context 文档为准。
```

如果任务涉及具体实现，再按需阅读：

- `WebAdminFrontendShell.java`
- `WebAdminFrontendStyles.java`
- `WebAdminFrontendScripts.java`
- `WebAdminServer.java`
- 当前相关 API / service / Brigadier 注册代码

不要一上来全仓库扫描，也不要根据旧文档直接假设当前能力。
