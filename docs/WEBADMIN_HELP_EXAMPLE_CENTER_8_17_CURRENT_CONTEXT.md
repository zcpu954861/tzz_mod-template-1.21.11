# 8.17 WebAdmin Help / Example Center Current Context

8.17 WebAdmin Help / Example Center adds a built-in read-only help, example, troubleshooting and glossary entry to WebAdmin.

The goal is to make the existing WebAdmin system easier to learn without changing runtime behavior. The page explains current modules with a 基础 / 专业 split:

- 基础模式: short Chinese explanations, minimal steps, common mistakes and safe starting points.
- 专业模式: runtime semantics, cross-module relationships, diagnostics, edge cases and deferred boundaries.

## Scope

Implemented:

- Help Center main route: `#/help`.
- Example Center route alias: `#/examples`, compatible with `#/help?view=examples`.
- Four main views: `#/help?view=docs`, `#/help?view=examples`, `#/help?view=troubleshooting`, `#/help?view=glossary`.
- Fixed Help Center app viewport with internal scrolling for topic list, document body, and right category panel.
- Right-side navigation adapts to the current main view: docs use document categories, examples use example modules, troubleshooting uses failure domains, and glossary uses term/module groups.
- Read-only catalog API: `GET /api/webadmin/help`.
- Sidebar entry under 模板与复用.
- The old top search/category/mode toolbar is intentionally not rendered.
- Category filtering is kept in the right-side category navigation.
- Basic/professional content fields remain in the catalog; the visible top mode toggle is removed.
- Topic detail with related pages, examples, troubleshooting and glossary.
- Topic list active item highlighting and scroll preservation during topic changes.
- Page-level help links through `data-page-help-link`.
- Inline term links through curated `termId -> route/help topic` mapping. Supported terms include SignalBridge, SignalListener, Timer, ConditionGroup, StateVariable, Logic Chain, Templates, Debugger, Doctor, Signal Join, Action, ActionRelay, VBD, Region and SignalReceiver.
- Inline term text clicks open the mapped help topic by default; they do not leave Help Center for the feature page.
- Inline term hover/focus popovers show a short Chinese definition, an explicit open-page action and a related-help action when available.
- Only the popover `打开页面` action stores return context in sessionStorage with a safe `helpReturn` id and restores view, topic, mode, filters, document scroll, topic list scroll and right panel scroll when returning.
- Popover state is single-active; close timers are tied to the active term id so fast term switching does not close the new popover.
- Target pages show `返回文档` only when opened from Help inline term navigation with `fromHelp=1`.
- Static builtin Help topic model in `WebAdminHelpCatalogService`.
- Help Center controls use delegated `data-*` actions instead of inline `onclick`; search input is IME-safe for Chinese composition.

The catalog is world independent and read-only. It does not create configs, does not apply templates, does not save user notes, and does not add a write API.

## Inline Terms And Return Context

Inline terms are intentionally curated, not global text replacement. Basic mode limits repeated terms per paragraph to reduce visual noise; professional mode can show a few more links.

Each inline term mapping contains:

- `termId`
- display aliases
- short Chinese definition
- target WebAdmin route
- related Help topic
- category

Clicking a term opens the real WebAdmin page with `fromHelp=1&helpReturn=<safe-id>`. The stored context is short-lived and session-local. A normal sidebar visit to the same page does not show the return button.

## Help Topic Model

Each topic supports:

- `basicTitle`
- `basicSummary`
- `basicSections`
- `professionalTitle`
- `professionalSummary`
- `professionalSections`
- `examples`
- `troubleshootingLinks`
- `relatedTopics`
- `glossaryTerms`
- `pageLinks`

Covered modules:

- Channel / SignalBridge
- SignalListener
- Action / ActionConfig
- ConditionGroup / ConditionEngine
- StateVariable
- Signal Join / Barrier / Aggregator
- Timer / Scheduler
- Logic Chain Viewer
- Logic Chain Editor controlled draft/editing scope
- Templates / Prefab / Import-Export
- Debugger / Doctor / Replay
- VBD / ActionRelay / Region / SignalReceiver as world entity references

## Examples

The Example Center is documentation-only. Examples describe what to configure and where to inspect results; they do not auto-create resources.
It is a separate main view rather than a lower section in the documentation page. Template relation appears in a fixed footer area on every example card; examples without a template show `无模板关联` in the same footer position.

Initial examples:

- 两个输入频道汇合后输出
- Timer 延迟后触发频道
- 监听频道后发送消息
- 监听频道后写入 StateVariable
- 用 ConditionGroup 控制 action
- 用 Template Center 预览并应用 Join/Timer/Listener 组合
- Signal 发出但没有后续动作
- Template import 与 apply 的区别
- Logic Chain 里新增 Join / Timer 草稿

Template links point to the existing Template Center. Real apply still uses the existing Template Center dry-run, lock, validation, fingerprint and audit flow.

## Troubleshooting

Initial troubleshooting entries cover:

- 为什么条件组不可选？
- 为什么 Join 没有输出？
- 为什么 Timer 没触发？
- 为什么 Listener 没执行 action？
- 为什么模板 apply 冲突？
- 为什么逻辑链列表只有一个入口但有很多频道？
- 为什么编辑器保存失败？
- 为什么看不到某个节点？
- 为什么某些节点在编辑器里只能查看不能创建？
- 导入 JSON 后为什么没有生效？
- 为什么空 gate 没有调试记录？
- 为什么状态变量动作失败？
- 为什么 Signal 有事件但无后续动作？

Troubleshooting reason/check/fix phrase lists are rendered without punctuation-before-slash formatting, for example `edit lock 丢失 / expectedFingerprint 冲突 / 草稿缺少连线`.

## Glossary

Initial glossary terms include, but are not limited to:

- 频道
- 逻辑链
- 焦点频道
- 关联组件
- SignalBridge
- SignalListener
- Action
- ActionConfig
- ConditionGroup
- StateVariable
- Join
- Barrier
- Timer
- Template
- Prefab
- Edit Lock
- Fingerprint
- Dry-run
- Placeholder
- Runtime Gate
- Action Gate
- Debugger
- Doctor
- Replay

## Accuracy

The help catalog must keep these statements accurate:

- ConditionEngine 只判断，不写状态，不发信号，不执行动作。
- SignalBridge 是事件总线，不是状态数据库。
- StateVariable 保存状态。
- Logic Chain Viewer 的顺序是可视化顺序，不是全局执行顺序。
- Logic Chain Editor 保存 typed config，不保存假图。

## Deferred

8.17 documents but does not implement:

- 用户自定义笔记 / 收藏 deferred
- GameController / MissionSystem / PhaseController deferred
- full Logic Chain Editor deferred
- Scratch editor deferred
- if / else runtime deferred
- 旧节点任意移动 / 删除 / 重排 deferred
- 旧 action 任意删除 / 重排 deferred
- world entity in-editor draft create documented as deferred
- placeholder binding apply deferred
- component export deferred
- ConditionGroup apply deferred
- StateVariable definition apply deferred
- external reference fail closed
- version rollback / Git-like branch merge deferred
- raw JSON editor deferred

## Future Direction

Future work can add:

- 用户自定义笔记 / 收藏
- 更完整的示例项目
- 和模板中心联动生成练习
- 交互式教程
- 新手引导流程
- IDE 内上下文帮助

These are intentionally not write-enabled in 8.17.
