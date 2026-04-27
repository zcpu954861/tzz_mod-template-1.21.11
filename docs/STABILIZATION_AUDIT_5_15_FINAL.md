# 5.15 稳定化最终报告

## 1. 版本定位

5.15 / `v1.17.0-stabilization-foundation` 是底层工具链稳定化 / GUI 前置整理版，不是新功能版本。本阶段目标是为 5.1 到 5.14 已完成的 SignalBridge、SignalDevice、`virtual_block_device`、ItemStackMatcher、consume 和 itemSubmit 链路建立审查结论、自动化护栏、结构化诊断和未来 GUI / Web Admin UI 的服务层方向。

本阶段不新增玩法触发类型，不改命令语义，不改 JSON schema，不实现 GUI，不实现 ConditionEngine。

## 2. Round1 到 Round5 汇总

### Round1：总体审查

生成 `docs/STABILIZATION_AUDIT_5_15.md`。审查范围覆盖 SignalBridge、SignalDevice、专用设备、`virtual_block_device`、ItemStackMatcher、itemSubmit、ActionEngine、RegionController、生命周期和文档一致性。

主要结论：

- P0：0。
- P1：8。
- P2：7。
- 最大风险集中在 `SignalDeviceData` 字段数量、`SignalDeviceStore` 和命令类职责过重、`VirtualBlockDeviceInteractionHandler` 组合路径复杂，以及 consume / itemSubmit / cooldown / lock 回归风险。

### Round2：字段保留 / 旧数据兼容基础护栏

生成 `docs/STABILIZATION_AUDIT_5_15_ROUND2.md`。新增 `stabilizationGuardTest` 并挂到 Gradle `check` / `build`。

覆盖：

- `SignalDeviceData` 字段保留。
- `withInteractionItemMatcher` / `withItemSubmit` 不丢失无关配置。
- setFromHand / matcher capture 保留 success/fail/source/consume 等配置。
- 旧构造器和缺字段默认值。

### Round3：consume / itemSubmit / cooldown 纯逻辑护栏

生成 `docs/STABILIZATION_AUDIT_5_15_ROUND3.md`。

新增：

- `ConsumePlan`
- `ConsumePlanner`
- `ItemSubmitEvaluator`
- `ItemSubmitEvaluationResult`
- `InteractionDecision`
- `InteractionDecisionEvaluator`

覆盖 consumeCount、跨 stack 背包消耗、重复预扣、itemSubmit 原子消耗、ignore 语义、allow / require lock / cooldown 决策。

### Round4：itemSubmit 生产路径接入 evaluator

生成 `docs/STABILIZATION_AUDIT_5_15_ROUND4.md`。

新增 `ItemSubmitInventoryAdapter`，让生产 itemSubmit 评估路径接入 `ItemSubmitEvaluator`。生产逻辑继续使用真实 `ItemStackMatcher` 能力，包括 item id、count、damage、自定义名称、lore、custom_data 和 components 的已支持匹配项。

游戏内烟测确认：

- 3 钻石 + 1 绿宝石：失败，不开箱，不消耗。
- 3 钻石 + 2 绿宝石：成功，开箱，正确消耗。
- cooldown 中失败仍锁住，不消耗，不刷反馈。
- cooldown 中成功仍开箱并消耗，不刷成功反馈。

### Round5：debug / doctor 结构化诊断增强

生成 `docs/STABILIZATION_AUDIT_5_15_ROUND5.md`。

新增 / 增强：

- `DiagnosticSeverity`
- `DiagnosticIssue`
- `DeviceDiagnostic`
- `InteractionItemDiagnostic`
- `ItemSubmitDiagnostic`
- `VirtualBlockDeviceDiagnosticService`
- `DiagnosticIssueText`

`/tzz signal device debug <device>` 输出单设备结构化诊断。`/tzz signal doctor` 增加设备层诊断摘要。诊断输出已按中文标题、字段、建议和诊断代码分组，便于管理员阅读，也为未来 Web UI 保留机器可读 code。

## 3. 自动化护栏现状

`stabilizationGuardTest` 当前覆盖：

- `SignalDeviceData` 字段保留。
- `withInteractionItemMatcher` 不清空 itemSubmit、container、itemCondition、success/fail、consume 配置。
- `withItemSubmit` 不清空 interactionItem、container、itemCondition、redstone、condition 配置。
- setFromHand / matcher refresh 保留反馈、source、vanilla policy、consume 配置。
- 旧 JSON 样本兼容。
- `ConsumePlan` / `ConsumePlanner` 两阶段 plan / apply。
- `ItemSubmitEvaluator` requirement 匹配、ignore、原子消耗、预扣合并。
- `ItemSubmitInventoryAdapter` 与生产路径接入检查。
- `InteractionDecisionEvaluator` 的 allow / require_item_match / cooldown / consume 决策。
- displayName 中文化。
- diagnostic DTO 和诊断文本渲染。

## 4. 生产代码影响范围

5.15 小范围修改过的生产代码：

- `VirtualBlockDeviceInteractionHandler`：接入 `ConsumePlan`、`ItemSubmitEvaluator`、`InteractionDecisionEvaluator`，保持既有业务语义。
- `ConsumePlan` / `ConsumePlanner`：新增 staged consume helper。
- `ItemSubmitEvaluator` / `ItemSubmitEvaluationResult` / `ItemSubmitInventoryAdapter`：新增 itemSubmit 统一评估路径。
- `InteractionDecisionEvaluator` / `InteractionDecision`：新增 lock / cooldown / side effect 决策 helper。
- debug / doctor 诊断类：新增结构化 DTO 和中文渲染。
- displayName helper：补充模式枚举的中文展示。

未改变：

- 命令语义。
- 命令参数。
- JSON 字段名。
- 旧 `signal_devices.json` 兼容逻辑。
- 5.5 到 5.14 已验收行为。

## 5. 当前仍需人工回归的少量路径

仍建议人工回归：

- 真实 custom name / lore / custom_data / components 的 ItemStackMatcher 匹配。
- UseBlockCallback / ActionResult 对箱子、门、按钮、拉杆的真实影响。
- 门上下半格锁定归一化。
- receiver / action_relay 联动。
- failChannel / successChannel 与 SignalEventHistory / device history 显示。
- 真实重启后的 JSON 保存 / 读取。

尚未实现：

- Web UI。
- WebSocket / REST API。
- ConditionEngine / ConditionGroup。
- GameController / MissionSystem。

## 6. GUI / Web Admin UI 前置结论

6.x 前置重点应是服务层、DTO 和 event bus，而不是继续把逻辑写进命令类。

原则：

- Web UI 不直接改 JSON。
- 命令、游戏内工具、Web UI 共用服务层。
- Web UI 必须功能完整，覆盖所有可配置能力。
- 游戏内工具只做轻量绑定、选择、定位和快速初始化。
- Web UI 负责全局逻辑图、节点卡片、配置编辑、实时状态、debug、doctor、history。
- 写操作应统一进入服务端 service，并预留权限和审计。
- debug / doctor 输出应同时支持聊天文本和结构化 DTO。

优先抽出的服务层：

- `SignalDeviceConfigService`
- `VirtualBlockDeviceConfigService`
- `InteractionItemConfigService`
- `ItemSubmitConfigService`
- `ItemStackMatcherConfigService`
- `ActionRelayConfigService`
- `RegionConfigService`
- `SignalDebugService`
- `SignalHistoryService`
- `DoctorService`

## 7. 5.15 后续建议

5.15 收尾后建议二选一进入：

1. 6.0 服务层 / API / DTO / WebSocket 前置。
2. 6.0 Web Admin UI 基础框架。

ConditionEngine、复杂 ConditionGroup、GameController 和 MissionSystem 适合后续独立阶段，不建议混入 5.15 稳定化收尾。
