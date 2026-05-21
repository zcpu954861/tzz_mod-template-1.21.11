# 8.19 WebAdmin Visual System Design Spike - UI UX PRO MAX

本文件是 8.19 WebAdmin Visual System Design Spike 的 UI UX PRO MAX 方向设计探索成果。

本阶段只探索视觉系统方向，不做正式功能开发，不改 WebAdmin 页面逻辑、页面结构、路由、API、后端或 runtime 语义。

## 范围声明

本设计方向基于当前稳定基线：

```text
v1.64.0-snapshot-rollback-timeline
master: 2209a851
```

允许探索：

- 颜色 token。
- 字体层级、字重、字号。
- 图标语义、图标颜色、模块图标补齐方向。
- 按钮、卡片、表格、modal、drawer、toast、alert、输入框、select、tag、badge 的视觉样式。
- 阴影、边框、圆角、hover、active、focus 状态。
- Logic Chain graph、Snapshot timeline、状态图表的线条和节点视觉。
- 代表页面样张说明和 8.20 正式重构建议。

明确不做：

- 不改页面结构和排版模式。
- 不改页面组件顺序。
- 不改 DOM 事件逻辑。
- 不改 WebAdminFrontendScripts 里的业务状态机。
- 不改 API、权限、CSRF、edit lock、audit、realtime。
- 不改后端、runtime、ActionType、ConditionNodeType。
- 不新增业务功能，不删除现有功能入口。

## UI UX PRO MAX 方向总述

推荐方向名：

```text
Deep Control Console
```

目标气质：

- 专业后台，而不是游戏化皮肤。
- 高端控制台，而不是霓虹 HUD。
- 数据密度高，但层级明确。
- 暗色长期使用舒适，低眩光，低饱和。
- 科技感来自精确边界、稳定布局、语义色和高质量图标，不依赖大面积发光。

设计关键词：

```text
沉稳
专业
可读
有层级
低眩光
高密度
可长期工作
```

核心视觉策略：

- 背景使用更克制的深蓝黑层级，避免所有卡片都是同一块蓝色。
- 交互主色从高亮 cyan 收敛为更产品化的 signal blue，cyan 只用于焦点、实时、可交互强调。
- 状态色统一语义：成功、警告、危险、信息、草稿、保护点、禁用各自固定。
- 卡片边界减少强 glow，改用清晰 1px 边框、轻量内阴影和低透明投影。
- 表格和列表增加 row hover、selected、dirty、readonly、danger 的一致视觉规则。
- Graph / timeline 的线条使用中低透明灰蓝，关键线用语义色增强，不让所有线抢视觉。
- 图标保持当前 2D flat tech inline SVG 体系，但补齐模块语义色和缺失图标。

## 视觉 Token 草案

### 基础色

| Token | 建议值 | 用途 |
| --- | --- | --- |
| `--wa-bg-canvas` | `#07101A` | 页面最底层背景 |
| `--wa-bg-shell` | `#0A1422` | sidebar / topbar 背景 |
| `--wa-bg-surface` | `#0E1B2B` | 主卡片、panel |
| `--wa-bg-surface-2` | `#122235` | hover 或二级卡片 |
| `--wa-bg-surface-3` | `#091522` | 表格 body、输入框、代码块 |
| `--wa-bg-elevated` | `#14263A` | modal / popover |
| `--wa-border-subtle` | `#20364D` | 普通边框 |
| `--wa-border-strong` | `#2B4A66` | active card / modal 边框 |
| `--wa-border-focus` | `#67E8F9` | focus ring |
| `--wa-text-primary` | `#E7F2FA` | 主文本 |
| `--wa-text-secondary` | `#B7C8D6` | 次级正文 |
| `--wa-text-muted` | `#7F95A8` | 元信息 |
| `--wa-text-disabled` | `#5F7183` | disabled 文本 |

### 品牌与交互色

| Token | 建议值 | 用途 |
| --- | --- | --- |
| `--wa-accent-primary` | `#38BDF8` | 主操作、选中态 |
| `--wa-accent-primary-strong` | `#0EA5E9` | 主按钮 hover / active |
| `--wa-accent-cyan` | `#67E8F9` | focus、实时同步、可点击 hint |
| `--wa-accent-violet` | `#A78BFA` | 模板、逻辑链弱关联、帮助链接 |
| `--wa-accent-amber` | `#FBBF24` | 回滚保护点、warning |
| `--wa-accent-green` | `#34D399` | 正常、成功、manual snapshot |
| `--wa-accent-red` | `#FB7185` | 危险、删除、阻断 |

### 状态色语义表

| 状态 | 主色 | 背景 | 边框 | 使用场景 |
| --- | --- | --- | --- | --- |
| 正常 / 成功 | `#34D399` | `rgba(52,211,153,.10)` | `rgba(52,211,153,.38)` | enabled、通过、manual snapshot |
| 信息 / 进行中 | `#38BDF8` | `rgba(56,189,248,.10)` | `rgba(56,189,248,.36)` | realtime、auto snapshot、普通更新 |
| 警告 | `#FBBF24` | `rgba(251,191,36,.10)` | `rgba(251,191,36,.42)` | degraded、pre_rollback、潜在风险 |
| 危险 / 阻断 | `#FB7185` | `rgba(251,113,133,.10)` | `rgba(251,113,133,.42)` | validation fail、delete、rollback apply |
| 草稿 / 未保存 | `#A78BFA` | `rgba(167,139,250,.10)` | `rgba(167,139,250,.38)` | Logic Chain draft、template preview |
| 禁用 / 只读 | `#94A3B8` | `rgba(148,163,184,.08)` | `rgba(148,163,184,.22)` | disabled、readonly、unavailable |

### 阴影与层级

| Token | 建议值 | 用途 |
| --- | --- | --- |
| `--wa-shadow-card` | `0 10px 24px rgba(0,0,0,.18)` | 普通 card |
| `--wa-shadow-hover` | `0 14px 34px rgba(0,0,0,.26)` | hover card |
| `--wa-shadow-modal` | `0 28px 90px rgba(0,0,0,.52)` | modal / drawer |
| `--wa-inner-line` | `inset 0 1px 0 rgba(255,255,255,.035)` | 高级卡片内高光 |

原则：

- 不使用大面积 neon glow。
- glow 只用于 focus、selected graph node、关键实时状态。
- 普通 hover 用边框色和背景层级变化，不依赖强阴影。

### 圆角

| Token | 建议值 | 用途 |
| --- | --- | --- |
| `--wa-radius-xs` | `6px` | tag、badge、inline code |
| `--wa-radius-sm` | `8px` | icon button、table row affordance |
| `--wa-radius-md` | `10px` | input、button、list card |
| `--wa-radius-lg` | `12px` | panel、metric card |
| `--wa-radius-xl` | `14px` | modal 内大卡片 |

建议不要继续扩大到 16px 以上，除非是 modal 容器。WebAdmin 是后台工具，圆角应克制。

### 间距

| Token | 建议值 | 用途 |
| --- | --- | --- |
| `--wa-space-1` | `4px` | icon/text micro gap |
| `--wa-space-2` | `8px` | badge gap、compact row |
| `--wa-space-3` | `12px` | card 内小间距 |
| `--wa-space-4` | `16px` | panel padding |
| `--wa-space-5` | `20px` | page block gap |
| `--wa-space-6` | `24px` | page padding |

## 字体层级表

推荐字体策略：

- 中文主字体优先使用系统栈，避免 WebAdmin 内置服务依赖外部 Google Fonts。
- 设计方向可参考 UI UX PRO MAX 的 `Noto Sans SC` / `Dashboard Data`，正式实现不强依赖外链。
- 技术 ID、fingerprint、JSON preview、resource id 使用 monospace。

建议 CSS 栈：

```css
--wa-font-sans: Inter, "Noto Sans SC", "Microsoft YaHei UI", "Segoe UI", Arial, sans-serif;
--wa-font-mono: "Fira Code", "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
```

| 层级 | 字号 | 行高 | 字重 | 用途 |
| --- | --- | --- | --- | --- |
| Page H1 | `26px` | `32px` | `700` | 页面标题 |
| Section H2 | `18px` | `24px` | `700` | panel 标题 |
| Card title | `15px` | `22px` | `700` | 卡片主标题 |
| Body | `14px` | `21px` | `400` | 正文 |
| Body strong | `14px` | `21px` | `600` | 重点字段 |
| Meta | `12px` | `18px` | `500` | 副文本、技术 ID |
| Table header | `12px` | `16px` | `700` | 表头 |
| Mono data | `12px` | `18px` | `500` | ID、hash、JSON 摘要 |

注意：

- 不使用负 letter-spacing。
- 中文正文不压到 12px 以下。
- 卡片内标题不使用 hero 级大字。
- 技术 ID 必须作为副文本，不抢主文案层级。

## 组件视觉规范草案

### Button

| 类型 | 视觉 |
| --- | --- |
| Primary | signal blue 背景，深色文字，hover 使用更深蓝，不扩大尺寸 |
| Secondary | surface-2 背景，strong border，浅色文字 |
| Ghost | 透明背景，subtle border，只用于低风险次级操作 |
| Danger | red text + red border + very dark red background，不使用满红大块，确认按钮可满红 |
| Icon button | 32x32 或 34x34 固定尺寸，图标 16x16，hover 不改变尺寸 |

Focus：

- 所有 button 必须有 `2px` cyan focus ring。
- 不允许 `outline: none` 后无替代。

### Card / Panel

普通 panel：

```text
surface background
1px subtle border
12px radius
16px padding
card shadow only when elevated or selected
```

Selected card：

```text
border: primary
background: surface-2
left accent bar or top accent line
```

危险 card：

```text
red border
red semantic icon
不要整块强红背景，避免长时间使用刺眼
```

### Table

建议：

- 表头使用 `surface-3`，字体 12px / 700 / muted。
- body row 高度 42-48px，保持数据密度。
- hover 只改变背景和左侧细线，不造成行高变化。
- selected row 使用 primary left border 或 inset highlight。
- danger/degraded 行用 status dot + badge，不把整行染红。

### Input / Select / Search

建议：

- 高度 36-38px。
- 背景 `surface-3`。
- focus ring 使用 `--wa-border-focus`。
- search 宽度可自适应，但普通 select 不应过宽。
- 中文 IME 输入期间不得触发打断式 rerender。

### Modal / Drawer

建议：

- backdrop 使用 `rgba(2,8,18,.68)` + blur，但 blur 不超过 10px。
- modal header/footer 固定，body 内滚动。
- modal 主体半径 14-16px，内部卡片 10-12px。
- 危险确认 modal 使用红色语义，但主要靠标题 icon、说明和确认按钮表达危险。

### Toast / Alert

建议：

- Toast 只做短反馈，不替代 lock/validation 的可见状态。
- Alert 分 info/warning/danger/readonly 四类。
- 不使用 browser alert/confirm/prompt。

## 图标体系建议

当前已有基础：

- inline SVG registry。
- 2D flat tech 风格。
- 图标本体不带圆底，外层泡泡和 hover 由 CSS 控制。
- 已有 `snapshot`、`help-center`、`example-center`、`template-package`、`logic-chain`、`signal-join`、`timer`、`condition-group`、`state-variable`、`doctor` 等 key。

保留方向：

- 继续使用单一 inline SVG 图标体系。
- 图标 stroke 统一 `1.8`，圆角端点，24x24 viewBox。
- 模块图标只用 `currentColor`，颜色由 CSS 语义类控制。
- Minecraft 原版 block/item 图标继续使用原版材质，不生成替代图。

### 图标审计表

| 模块 | 当前状态 | 问题 | 建议 |
| --- | --- | --- | --- |
| Snapshot Timeline | 已有 `snapshot` | 可表达文档快照，但 rollback / pre_rollback 语义不够独立 | 补 `snapshot-rollback`、`snapshot-protected` 变体，仅用于图例/按钮 |
| Help Center | 已有 `help-center` / `example-center` | docs/examples/troubleshooting/glossary 可继续细分 | 补 `help-docs`、`help-troubleshooting`、`help-glossary` 或用现有图标加语义色 |
| Template Center | 已有 `template-package` | import/export/apply 语义有时借用 action/channel 图标 | 补 `template-import`、`template-export`、`template-apply` |
| Logic Chain Editor | 已有 `logic-chain` / `logic-node` | draft、connect、lock、reference、existing edit 缺语义图标 | 补 `logic-draft`、`logic-connect`、`logic-reference`、`logic-lock` |
| Timer | 已有 `timer` / `timer-start` / `timer-cancel` / `delay` / `countdown` / `repeat` | mode 图标基本够用 | 正式重构时统一 mode badge 色 |
| Signal Join | 已有 `signal-join` / `signal-barrier` / `signal-aggregator` | ALL / ANY_N / COUNT mode 需要更清楚 | 用同图标 + mode badge，避免图标过多 |
| ConditionGroup | 已有 `condition-group` / `condition-debugger` / `runtime-gate` | action gate 和 list gate 视觉容易混 | 补 `action-gate` 或使用 gate icon + action accent |
| StateVariable | 已有 global/player 变体 | bool/int/string 类型未区分 | 用 type badge，不建议新增过多图标 |
| Debugger | 已有 `condition-debugger` / `replay` | timeline/replay/doctor 色彩需统一 | Debugger 用 violet，Doctor 用 amber/red/green 状态 |
| Doctor | 已有 ok/warning/error | 语义清楚 | 保持 |
| ActionRelay / VBD / Receiver | 已有 | 与 Minecraft 物理设备的区分可更强 | 物理设备仍避免伪材质；用几何设备轮廓 |

图标语义色建议：

| 类别 | 颜色 |
| --- | --- |
| Signal / Channel | cyan-blue |
| Logic Chain | violet |
| Timer / Scheduler | blue |
| Join / Aggregator | green-cyan |
| Condition / Gate | amber-violet |
| State | emerald |
| Template | violet |
| Snapshot / Rollback | blue / amber / green by kind |
| Help / Docs | slate-cyan |
| Doctor / Debug | status semantic |

## Graph / Timeline 视觉规则

### Logic Chain Viewer / Editor

节点：

- channel：cyan-blue。
- join：green-cyan。
- timer：blue。
- condition gate：amber。
- action gate：amber-violet。
- state variable/action：emerald。
- reference：muted gray-blue。
- draft：violet。

连线：

- 默认 edge：`#6B8296`，60% opacity。
- primary edge：对应模块主色，85% opacity。
- reference edge：dash + muted，45% opacity。
- draft edge：violet/green by operation，80% opacity。
- selected related edge：主色 + 2px，不增加线条端点数量。

原则：

- 不恢复可见 shared trunk / merge point。
- 统一端点、单箭头的 8.13 修复必须保持。
- 视觉变化只能调整颜色、透明度、hover/selected，不改 graph membership、layout、交互或端点算法。

### Snapshot Timeline / Rollback

节点：

- manual：emerald。
- auto：blue。
- pre_rollback：amber。
- selected：blue border + inner highlight。

时间轴线：

- 默认 rail 使用 muted blue-gray。
- hover/selected 时仅增强当前节点卡片，不让整条 rail 过亮。

Diff：

- created：emerald。
- updated：blue。
- deleted：amber/red 之间需区分；建议 deleted 用 amber 表示回滚删除预览，真正危险删除按钮才用 red。
- read-only diff modal 使用左右栏旧/新明确标签，避免只靠颜色判断。

## 代表页面样张说明

本轮不生成可运行正式 UI，仅描述可落地样张方向。8.20 若进入正式重构，应按这些页面各出 1 张实际截图或 Story-style 静态样张。

### Dashboard

样张方向：

- 保持现有 sidebar / topbar / dashboard card 结构。
- 顶部 H1 降到 26px，副标题使用 muted。
- metric card 使用 12px radius、subtle border、低阴影。
- 概览卡仍是卡片网格，不改卡片数量和跳转逻辑。
- 每张卡片的图标泡泡使用模块语义色，指标行 mini icon 使用 muted。

重点：

- 卡片 hover 有清楚边框和背景变化。
- disabled/unavailable card 不应和可点击 card 一样亮。

### Logic Chain Viewer / Editor

样张方向：

- 保持 canvas、toolbar、legend、detail panel、draft 编辑模式结构。
- Canvas 背景由深蓝黑改为轻微网格纹理或极低透明 radial，不使用装饰光球。
- node card 边界更清楚，type accent 在左侧 3px bar 或 top stripe。
- draft 节点使用 violet + dashed edge；合法连接点使用 green，但不能过亮。
- reference card 使用 muted gray-blue，降低视觉权重。

重点：

- 不改 pan/zoom、connect mode、slot、draft overlay、layout。
- 只改节点/edge/token 视觉。

### Snapshot Timeline / Rollback

样张方向：

- 保持 Git-graph-like timeline，不改成表格。
- 筛选条保持一行优先，搜索框最大，其余 select 收窄。
- timeline card 内部滚动，最多同时露出约 10 条节点。
- detail rail 使用固定分组卡：元信息、本次操作变化、与上一保存点变化、rollback 操作。
- diff modal 使用三列：字段 / 旧 / 新；created/deleted 用单侧摘要。

重点：

- manual / auto / pre_rollback 三种节点一眼可分。
- rollback apply 危险色只出现在确认动作，不污染所有 rollback 信息。

### Timer

样张方向：

- 保持列表 + detail / modal 编辑结构。
- mode badge 使用 DELAY blue、COUNTDOWN cyan、REPEAT amber。
- active runtime 状态使用 compact status strip。
- action bucket summary 使用四个小卡片，onComplete / onTick / onCancel 有一致图标色。

重点：

- Timer 是时间轴能力，不要视觉上伪装成普通 signal-only 节点。

### Signal Join

样张方向：

- 保持 Join 列表、详情和编辑 modal。
- mode 视觉：ALL 使用 chain-check，ANY_N 使用 split-count，COUNT 使用 counter badge。
- input channels 使用 compact chips；output channel 使用 primary chip。
- pending / cooldown / timeout 使用状态行，不堆大卡片。

重点：

- Join 是 passive observer，不应用强危险或阻断视觉误导为“阻止原 signal”。

### Condition Groups

样张方向：

- 保持条件组列表、catalog、条件树 compact card、node modal。
- condition type 分类 tab 使用 restrained segmented control。
- AND / OR / NOT 使用小 badge + tree connector，而不是大面积彩色块。
- validation error 使用 danger alert + 定位到字段，不清空用户输入。

重点：

- 中文名称优先，type id 为 monospace muted。

### Template Center

样张方向：

- 保持 Template Center 列表/详情/右侧 apply/export rail。
- built-in / user / imported 用 source badge。
- apply dry-run 使用 preview card，不改变现有 dry-run -> apply 流程。
- JSON preview 使用 elevated code panel，滚动高度受限。

重点：

- 模板应更像“可复用组件库”，但不能做 marketplace 或新增功能。

### Help / Example Center

样张方向：

- 保持四主视图：docs、examples、troubleshooting、glossary。
- 中间正文区继续内部滚动，圆角裁切必须稳定。
- inline term 使用 subtle underline + popover，不使用强色背景污染阅读。
- 右侧 category nav 使用 compact active state，不显示无意义空卡片。

重点：

- 文档阅读体验优先，视觉不能压过正文。

## 8.20 正式重构建议

建议把 8.20 拆成低风险步骤：

1. 只落地 token rename / alias，不改变任何页面结构。
2. 更新全局 button / input / badge / table / modal 视觉。
3. 更新 sidebar / topbar / dashboard / common detail shell。
4. 更新 graph / timeline 视觉，不改 layout / edge endpoint / interaction。
5. 更新 Help / Template / Snapshot 这些复杂页面的局部 polish。
6. 补齐图标 registry 缺口和语义色类。
7. 增加 guard：禁止 8.20 中改 route、API、runtime、DOM event business handlers。

建议 8.20 验收：

- JS export + `node --check`。
- `.\gradlew.bat clean build`。
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`。
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`。
- `git diff --check`。
- 用户人工浏览 WebAdmin 核验重点页面：Dashboard、Logic Chain、Snapshot、Timer、Signal Join、Condition Groups、Template Center、Help Center。

## 本设计 Spike 的文件改动

本方向只新增本设计文档：

```text
docs/WEBADMIN_VISUAL_SYSTEM_UIUX_PRO_MAX_8_19.md
```

没有修改：

- `WebAdminFrontendStyles.java`
- `WebAdminFrontendScripts.java`
- `WebAdminFrontendShell.java`
- 后端 API / service / model
- runtime 代码
- tests / guards

没有 commit、push、merge、tag。
