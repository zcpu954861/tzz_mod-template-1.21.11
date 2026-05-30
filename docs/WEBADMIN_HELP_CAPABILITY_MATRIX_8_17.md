# WebAdmin Help Capability Matrix 8.17

8.17 WebAdmin Help / Example Center is a read-only documentation surface inside the existing WebAdmin frontend.

| Capability | Status | Notes |
| --- | --- | --- |
| Help Center route | Implemented | `#/help` renders the built-in catalog. |
| Main Help views | Implemented | 文档区 / 示例中心 / 排错中心 / 术语表 are separate views. |
| Example Center route | Implemented | `#/examples` opens `#/help?view=examples` compatibility behavior. |
| Builtin catalog API | Implemented | `GET /api/webadmin/help`; no write method. |
| 基础 / 专业 content | Implemented | Content keeps basic/professional fields; the top mode toolbar is not rendered. |
| Help topic model | Implemented | Topic supports basic/pro sections, examples, troubleshooting, glossary, related topics and page links. |
| Top toolbar | Removed | Search/category/mode toolbar is not rendered to avoid the empty strip and covered bubble. |
| Fixed app viewport | Implemented | Help page shell does not become one long page; topic list, content and right panel scroll internally. |
| Topic navigation | Implemented | Topic list preserves scroll on topic changes and highlights the current topic. |
| Right category navigation | Implemented | Right navigation adapts to docs / examples / troubleshooting / glossary and filters the active view. |
| Page-level help | Implemented | Main pages render `data-page-help-link` links into topic routes. |
| Inline term mapping | Implemented | Curated `termId -> targetRoute / targetHelpTopic` mapping; no blind whole-text route guessing. |
| Inline term default click | Implemented | Clicking term text opens the help topic, not the feature page. |
| Inline term popover | Implemented | Hover/focus shows a compact Chinese definition with open-page and related-help actions; only one active popover is allowed. |
| Help return context | Implemented | Only the popover `打开页面` action stores return context and restores view, topic, mode, filters and internal scroll positions. |
| Return-to-help button | Implemented | Target pages show `返回文档` only when `fromHelp=1` and `helpReturn` resolves. |
| Examples | Implemented | Documentation-only, copy-only examples. |
| Example template footer | Implemented | Template CTA and `无模板关联` render in a fixed footer area. |
| Troubleshooting | Implemented | Common failure reasons, checks, recommended actions and professional explanation. |
| Snapshot / Rollback help | Implemented | 8.20 stabilization adds `snapshot.rollback`, Snapshot/Rollback glossary terms and troubleshooting for degraded packages, operation diff direction and retention. |
| Clean slash formatting | Implemented | Short phrase lists render as `A / B / C` without punctuation before slash. |
| Glossary | Implemented | Chinese-first terms with technical aliases. |
| Template link | Implemented as navigation | Links to Template Center; apply still uses existing template flow. |
| User notes / favorites | Deferred | 用户自定义笔记 / 收藏 deferred. |
| External docs sync | Deferred | No external documentation site or online sync. |
| AI-generated docs | Deferred | Catalog is curated static builtin content. |
| Runtime semantic change | Not allowed | No SignalBridge, ActionEngine, ConditionEngine, Join, Timer or Logic Chain runtime changes. |
| New ActionType | Not allowed | No ActionType added. |
| New ConditionNodeType | Not allowed | No ConditionNodeType added. |

## Required Content Coverage

Covered topics:

- Channel / SignalBridge
- SignalListener
- Action / ActionConfig
- ConditionGroup
- StateVariable
- Signal Join
- Timer
- Logic Chain Viewer
- Logic Chain Editor
- Templates / Prefab
- Snapshot / Rollback / 配置时间轴
- Debugger / Doctor
- Device / VBD / ActionRelay / Region / SignalReceiver reference boundaries

Covered examples:

- 两个输入频道汇合后输出
- Timer 延迟后触发频道
- 监听频道后发送消息
- 监听频道后写入 StateVariable
- 用 ConditionGroup 控制 action
- 用 Template Center 预览并应用 Join/Timer/Listener 组合
- Signal 发出但没有后续动作
- Template import 与 apply 的区别
- Logic Chain 里新增 Join / Timer 草稿
- 用配置时间轴 dry-run 回滚前先确认变化

Covered troubleshooting:

- 条件组不可选
- Join 没有输出
- Timer 没触发
- Listener 没执行 action
- 模板 apply 冲突
- 逻辑链列表只有一个入口但有很多频道
- 编辑器保存失败
- 看不到某个节点
- 某些节点只能查看不能创建
- 导入 JSON 后没有生效
- 空 gate 没有调试记录
- 状态变量动作失败
- Signal 有事件但无后续动作
- 配置时间轴 degraded / bad package
- 回滚前保护点 operation diff
- 自动快照 retention 200

## Safety Boundaries

8.17 explicitly documents these deferred boundaries:

- GameController / MissionSystem / PhaseController deferred
- full Logic Chain Editor deferred
- Scratch editor deferred
- if / else runtime deferred
- old node move/delete/reorder deferred
- old action delete/reorder deferred
- world entity in-editor draft create documented as deferred
- placeholder binding apply deferred
- component export deferred
- ConditionGroup apply deferred
- StateVariable definition apply deferred
- external reference fail closed
- Git-like branch / merge / rebase deferred；Snapshot 配置回滚已实现且仅限 allowlist 配置
- raw JSON editor deferred

## Guard Markers

Frontend and docs include these stability markers:

- `data-help-example-center-nav`
- `data-help-example-center-route`
- `data-help-example-center-view-tabs`
- `data-help-example-center-view-tab`
- `data-help-example-center-docs-view`
- `data-help-example-center-examples-view`
- `data-help-example-center-troubleshooting-view`
- `data-help-example-center-glossary-view`
- `data-help-example-center-no-toolbar`
- `data-help-example-center-no-topic-category-pill`
- `data-help-example-center-fixed-viewport`
- `data-help-example-center-no-whole-page-long-scroll`
- `data-help-example-center-topic-list`
- `data-help-example-center-topic-list-internal-scroll`
- `data-help-example-center-topic-list-preserve-scroll`
- `data-help-example-center-topic-active`
- `data-help-example-center-topic-card`
- `data-help-example-center-topic-detail`
- `data-help-example-center-right-category-nav`
- `data-help-example-center-right-nav-per-view`
- `data-help-example-center-right-nav-view`
- `data-help-example-center-category-clickable`
- `data-help-example-center-category-active`
- `data-help-example-center-example-list`
- `data-help-example-center-example-card`
- `data-help-example-center-template-relation-footer`
- `data-help-example-center-no-template-aligned`
- `data-help-example-center-template-cta-aligned`
- `data-help-example-center-troubleshooting-list`
- `data-help-example-center-clean-reason-list`
- `data-help-example-center-glossary`
- `data-help-example-center-readonly`
- `data-help-example-center-no-write-api`
- `data-help-example-center-copy-only`
- `data-help-example-center-template-link`
- `data-help-example-center-doctor-link`
- `data-help-example-center-route-link`
- `data-help-example-center-no-browser-dialogs`
- `data-help-example-center-no-unsafe-inline-onclick`
- `data-help-example-center-event-delegation`
- `data-help-example-center-button-type-button`
- `data-help-example-center-no-unexpected-end-of-input`
- `data-help-example-center-no-punctuation-before-slash`
- `data-help-example-center-responsive-stack`
- `data-help-example-center-inline-term`
- `data-help-example-center-inline-term-data-id`
- `data-help-example-center-inline-term-click-opens-topic`
- `data-help-example-center-inline-term-click-not-feature-page`
- `data-help-example-center-inline-term-popover`
- `data-help-example-center-inline-term-definition`
- `data-help-example-center-inline-term-open-page-action`
- `data-help-example-center-inline-term-related-help-action`
- `data-help-example-center-single-active-popover`
- `data-help-example-center-popover-close-timer-term-id`
- `data-help-example-center-popover-fast-switch-stable`
- `data-help-example-center-popover-scroll-close`
- `data-help-example-center-popover-bottom-safe`
- `data-help-example-center-return-context-session`
- `data-help-example-center-return-context-safe-id`
- `data-help-example-center-return-restore-view-topic-mode`
- `data-help-example-center-return-restore-scroll`
- `data-help-example-center-return-to-help-only-from-inline`
- `data-page-help-link`
- `data-page-help-topic`
- `data-page-help-return-to`
- `data-page-help-return-action`
- `snapshot.rollback`
- `trouble.snapshot-degraded`
- `trouble.rollback-operation-diff`
- `trouble.snapshot-retention`

## Non-Goals

8.17 does not implement:

- user-written notes
- favorites
- external documentation publishing
- online documentation sync
- AI-generated documentation
- full editor workflows
- if/else runtime
- new gameplay controller systems
- Git-like branch / merge / rebase

## 9.2 Typed Actions Additive Coverage

This section is implemented in 9.2 Phase 6 and does not change the original 8.17 Help capability boundary.

| Capability | Status | Notes |
| --- | --- | --- |
| 9.2 Phase 6 typed action help coverage | Implemented in 9.2 Phase 6 | Adds read-only Help topic, example, troubleshooting and glossary coverage for typed actions. |
| docs derive from ActionSchemaRegistry and ActionCapabilityMatrix | Guarded | `WebAdminTypedActionHelpGuardTest` reads the Java registry/matrix dynamically. |
| Help Center remains read-only and world-independent | Guarded | No POST/PATCH/DELETE help API, no notes/favorites writes. |
| typed action help covers every current ActionType | Guarded | command, message, sound, signal, state_variable, timer_start, timer_cancel. |
| typed action help covers every current ActionConfig owner | Guarded | SignalListener, ActionRelay, Region enter/exit/stay and Timer start/tick/complete/cancel buckets. |
| explicit typed action non-owners: vbd_trigger, item_submit, container_change, branch | Guarded | These remain outside the `ActionConfig` owner matrix. |
| docs must not diverge from registry / matrix | Guarded | Drift is a Phase 6 stop condition. |

Phase 6 does not add ActionType, owner, runtime behavior, WebAdmin API, save payload or snapshot storage. It also does not implement Program Model, Rich Text Builder, GameController, MissionSystem, PhaseController, if / else runtime, typed action sequence runtime, raw JSON editor, user notes, favorites or external docs sync.
