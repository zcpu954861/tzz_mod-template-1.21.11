# 8.19 UI UX PRO MAX 静态样张 V2

本文件是 8.19 WebAdmin Visual System Design Spike 的 V2 返修交付。V2 目标不是继续做换色样张，而是预演 8.20 可落地的组件级视觉重构，并补充 Deep Control Console Dark / Light 两套主题 token。

本轮仍然只新增 docs 下的静态 HTML/CSS/JS 样张，不修改真实 WebAdmin 运行代码。

## 本轮返修目标

上一轮 V1 已接近“专业工具平台 / Deep Control Console”，但如果 8.20 只改颜色、字体和图标颜色，实际提升会很有限。因此 V2 按以下边界重做：

```text
页面级结构不改
业务结构不改
组件内部信息层级允许优化
状态样式允许统一
深浅色主题 token 同步设计
```

关键判断：

- 只换颜色会像换皮。
- 组件内部 title / id / status / action / hint 的层级不统一，才是“廉价感”的主要来源。
- 8.20 应先统一 token、card、row、badge、table、modal、graph node、timeline node 这些组件，再逐页替换视觉。

## 边界

8.20 仍应禁止：

- 改页面信息架构。
- 改页面主布局模式。
- 改路由、API、后端、runtime。
- 改权限、CSRF、edit lock、audit、realtime。
- 改按钮功能、字段含义、保存 / 删除 / 回滚 / apply / dry-run 流程。
- 改 Logic Chain graph membership / layout algorithm / edge endpoint 算法。
- 改 Snapshot timeline 的基本交互模型。
- 改 Help Center 四主视图。

8.20 可允许：

- 组件级视觉重构。
- 卡片内部信息层级重排。
- field label / value / hint 的视觉关系优化。
- 按钮、表格、modal、badge、chip、status pill、hover、selected、readonly、danger、draft 状态统一。
- graph/timeline 节点卡内部层级和线条视觉调整。
- diff/detail/JSON preview 的只读展示样式优化。

## 样张入口

入口：

```text
docs/visual-system-8-19/uiux-pro-max-v2/index.html
```

页面：

```text
docs/visual-system-8-19/uiux-pro-max-v2/dashboard.html
docs/visual-system-8-19/uiux-pro-max-v2/logic-chain.html
docs/visual-system-8-19/uiux-pro-max-v2/snapshot.html
docs/visual-system-8-19/uiux-pro-max-v2/timer-join.html
docs/visual-system-8-19/uiux-pro-max-v2/condition-groups.html
docs/visual-system-8-19/uiux-pro-max-v2/help-template.html
```

共享样式与 fake toggle：

```text
docs/visual-system-8-19/uiux-pro-max-v2/visual-system.css
docs/visual-system-8-19/uiux-pro-max-v2/theme-toggle.js
```

截图：

```text
未生成 PNG；HTML 可直接打开查看。
```

## Dark / Light 主题切换

每个 V2 样张页面右上角或侧栏底部有主题按钮。点击后会在当前静态页面内切换：

```text
Deep Control Console Dark
Deep Control Console Light
```

这个 toggle 只是静态样张用的 fake toggle：

- 不接入真实 WebAdmin 设置页。
- 不写真实用户配置。
- 不接入真实前端业务状态机。
- 不使用 sessionStorage / localStorage 做真实持久化。
- 不代表 8.20 已经实现主题保存。

也可以用 query 参数预览浅色：

```text
index.html?theme=light
```

## Dark Token

| Token | 值 |
| --- | --- |
| background | `#07101a` |
| shell | `#0a1422` |
| surface | `#0e1b2b` |
| surface elevated | `#14263a` |
| input | `#081522` |
| border | `#20364d` |
| border strong | `#2b4a66` |
| text primary | `#e7f2fa` |
| text secondary | `#b7c8d6` |
| text muted | `#7f95a8` |
| accent primary | `#38bdf8` |
| ok / manual | `#34d399` |
| info / auto | `#38bdf8` |
| warning / pre_rollback | `#fbbf24` |
| danger | `#fb7185` |
| focus ring | `#67e8f9` |
| shadow | `0 10px 24px rgba(0,0,0,.18)` |

## Light Token

| Token | 值 |
| --- | --- |
| background | `#eef3f8` |
| shell | `#f8fafc` |
| surface | `#ffffff` |
| surface elevated | `#ffffff` |
| input | `#f8fafc` |
| border | `#d8e2ec` |
| border strong | `#b8c8d8` |
| text primary | `#0f172a` |
| text secondary | `#334155` |
| text muted | `#64748b` |
| accent primary | `#0369a1` |
| ok / manual | `#047857` |
| info / auto | `#0369a1` |
| warning / pre_rollback | `#b45309` |
| danger | `#be123c` |
| focus ring | `#0284c7` |
| shadow | `0 10px 22px rgba(15,23,42,.06)` |

浅色主题不是深色反色。它使用 warm gray / blue gray canvas、white-ish surface、slate text、低饱和状态色，避免纯白刺眼和 Bootstrap admin 风。

## Module Colors

| Module | Dark / Light 语义 |
| --- | --- |
| Signal / Channel | blue / cyan-blue |
| Join | green |
| Timer | blue |
| Condition / Gate | amber |
| State | emerald |
| Action / Template / Draft | violet |
| Region | teal |
| Doctor / Danger | red |
| Snapshot | blue, with manual green and pre_rollback amber |

## 样张说明

### Dashboard

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/dashboard.html
```

保留结构：

- sidebar / topbar。
- 总览 metric card。
- 模块入口卡片。
- 模块状态和核心指标。

组件级优化：

- metric card 改为 icon + metric-copy 两列，label / value / delta / hint 层级固定。
- module card 固定 title、subtitle、status badges、body rows、CTA 的位置。
- active card 使用左侧语义条和边界增强，不改卡片数量或路由。
- topbar status chip 使用统一 status pill。

未改变逻辑：

- 不改变模块入口含义。
- 不改变按钮功能。
- 不新增或删除 Dashboard 功能。

8.20 落地：

- 先引入 metric-card、module-card、component-row、status-pill 基础组件。

### Logic Chain Viewer / Editor

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/logic-chain.html
```

保留结构：

- canvas + toolbar + legend + 右侧详情。
- 节点和边的基本关系。

组件级优化：

- node card 内部固定 node-head、technical id、badge、meta chip。
- channel / join / draft / reference 通过语义色条区分。
- edge 默认低权重，primary / draft 才增强。
- 右侧详情改为 field-card 统一 field-value。

未改变逻辑：

- 不改 graph membership。
- 不改 layout algorithm。
- 不改 edge endpoint / merge point 算法。
- 不改 pan/zoom/connect mode。

8.20 落地：

- 仅替换 CSS class 和 node card 内部 markup helper，不碰 graph model。

### Snapshot Timeline / Rollback

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/snapshot.html
```

保留结构：

- Git-graph-like timeline。
- 详情 rail。
- diff。
- rollback dry-run/apply。

组件级优化：

- manual / auto / pre_rollback 使用 node-color token。
- timeline card 内部 title、time、actor、resource badges 分层。
- diff-card 统一 change type / resource / action。
- 只读变更详情 modal 使用三列 field / old / new。
- danger 操作区克制但明确。

未改变逻辑：

- timeline 不改表格。
- rollback 仍 dry-run first。
- pre_rollback 逻辑不弱化。

8.20 落地：

- 为 snapshot kind、diff row、modal field diff 增加 CSS token 和 guard。

### Timer + Signal Join

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/timer-join.html
```

保留结构：

- Timer / Join 列表。
- 详情。
- 配置摘要。
- action bucket。

组件级优化：

- Timer mode badge、Join mode badge 统一。
- channel chip 统一。
- runtime status strip 统一。
- action bucket card 固定标题、数量、hint。
- table row hover/selected 风格统一。

未改变逻辑：

- 不改 Timer / Join 字段含义。
- 不改 mode 逻辑。
- 不改 save/edit 流程。

8.20 落地：

- 把 mode badge / channel chip / status strip 作为共享组件。

### Condition Groups

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/condition-groups.html
```

保留结构：

- 条件组列表。
- 条件树。
- 类型目录。
- 节点编辑 modal 样式。

组件级优化：

- AND / OR / NOT 和 leaf condition 都是 condition-node。
- connector 低透明，不靠大色块。
- validation error 用 danger status pill。
- type id 保持 monospace 副文本。

未改变逻辑：

- 不改 condition tree 逻辑。
- 不改 condition type。
- 不改 validation。

8.20 落地：

- 条件节点、catalog chip、validation alert 可先 CSS 化，再替换局部 helper。

### Help Center / Template Center

文件：

```text
docs/visual-system-8-19/uiux-pro-max-v2/help-template.html
```

保留结构：

- Help Center 四主视图。
- 左主题列表。
- 中间文档滚动。
- 右侧分类导航。
- inline term popover。
- Template list/detail/apply/dry-run/JSON preview 概念。

组件级优化：

- topic-card active 状态统一。
- doc-section 形成清晰阅读块。
- inline term 低噪音化。
- popover 使用 modal-card 视觉。
- Template source badge、dry-run、JSON preview 使用共享 card/chip。

未改变逻辑：

- 不改 Help Center 四主视图。
- 不改 return context。
- 不改 Template apply/dry-run 流程。

8.20 落地：

- 先替换 Help topic card、doc section、inline term、popover 视觉，再处理 Template detail rail。

## 图标与状态色

V2 样张继续使用 inline SVG / currentColor / 2D flat tech 风格，不使用 emoji、icon font、外部图片资源或 Minecraft 原版材质替代图。

已体现：

- snapshot manual / auto / pre_rollback。
- logic chain / draft / reference / lock 风格。
- timer / delay / countdown / repeat badge。
- signal join / all / any_n / count badge。
- condition group / condition gate。
- template / import / export / apply。
- help docs / examples / troubleshooting / glossary。
- doctor / debugger 语义色。

## 8.20 正式重构建议

建议分阶段：

1. 加入 dark/light token alias，但默认仍 dark。
2. 先做静态 CSS component token，不改 JS 状态机。
3. 更新 shared button / input / badge / chip / status pill / field-card / table row。
4. 更新 Dashboard metric-card / module-card。
5. 更新 Logic Chain node card 和 edge color，只动 CSS / render helper，不动 graph model。
6. 更新 Snapshot timeline card / diff card / modal field diff。
7. 更新 Timer / Join mode badge、runtime strip、action bucket。
8. 更新 Condition node / validation / catalog。
9. 更新 Help / Template topic/doc/popover/source badge。
10. 最后再接入真实主题设置；先只支持前端 CSS token，后续再决定是否保存到用户设置。

## 风险与验收点

风险：

- 浅色主题容易变成普通 Bootstrap admin，需要保持灰蓝 canvas 和明确边界。
- Graph 在浅色下 edge 对比必须重新检查。
- 状态色不能只靠颜色，应保留文字 badge。
- 组件内部层级优化不能演变成页面结构改造。
- Help 正文不能被过多 chip / icon 干扰。

验收点：

- 每个页面 dark/light 都可读。
- Dashboard 模块卡更像专业组件，而不只是换色。
- Logic Chain draft/reference/selected 一眼区分。
- Snapshot manual/auto/pre_rollback 一眼区分。
- Timer/Join mode/status/channel 层级清楚。
- Condition validation 不清空输入、不只靠红边。
- Help/Template 更像专业文档控制台。

## 范围确认

本轮没有修改真实 WebAdmin 源码：

```text
未修改 WebAdminFrontendScripts.java
未修改 WebAdminFrontendStyles.java
未修改 WebAdminFrontendShell.java
未修改 WebAdminServer.java
未修改后端 service / DTO / store
未修改 API / route / runtime
未修改权限 / CSRF / edit lock / audit / realtime
未修改 build / gradle / tests
```

本轮没有改变：

```text
业务逻辑
页面结构
页面主布局模式
按钮功能
表单字段含义
保存 / 删除 / 回滚 / apply / dry-run / edit lock 流程
ActionType
ConditionNodeType
runtime 语义
```

本轮没有 commit / push / merge / tag。

本轮没有处理 `.codex/` 或 `logs/`。
