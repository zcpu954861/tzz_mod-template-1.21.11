# 5.15 稳定化审查报告 Round4

## 1. 本轮目标

本轮目标是把 5.14 `itemSubmit` 的生产评估路径和 Round3 建立的 `ItemSubmitEvaluator` 测试 seam 对齐，避免 `VirtualBlockDeviceInteractionHandler` 内部再次维护一套独立的提交判断、统计和原子消耗逻辑。

本轮不新增玩法功能，不修改命令语义，不修改 JSON 字段名，不改变已验收的 `allow` / `require_item_match`、cooldown、consume、itemSubmit 互斥模式、SignalBridge emit 路径。

## 2. 生产路径接入结果

已接入。

`VirtualBlockDeviceInteractionHandler` 的 itemSubmit 分支现在负责：

- 构造触发玩家主背包 / 热键栏的提交视图。
- 调用 `ItemSubmitEvaluator.evaluate(...)`。
- 根据 `ItemSubmitEvaluationResult.finalSuccess()` 决定成功 / 失败路径。
- 在成功且启用 itemSubmit consume 时使用 evaluator 返回的 staged `ConsumePlan`。
- 在非 cooldown 时把 requirement 最近匹配结果写回设备状态。

生产 handler 不再在 itemSubmit 路径内自行维护独立的 requirement 统计和 consume plan 构建逻辑。

## 3. Adapter 设计

新增 `ItemSubmitInventoryAdapter`，用于把真实 Minecraft `ItemStack` 列表转换为 `ItemSubmitEvaluator` 可消费的抽象视图。

设计要点：

- 输入是玩家 `getInventory().getMainStacks()`。
- 使用 `ConsumePlanner.inventorySlotOrder(...)` 保留 `hotbar_first` / `main_inventory_first` 顺序。
- 每个 source stack 使用 `inv:<slot>` 作为稳定 consume key。
- apply 阶段通过 `ItemStack.decrement(amount)` 执行真实扣除。
- 空 stack 暴露为 count 0，不会被 matcher 误判为满足。
- adapter 只覆盖主背包 / 热键栏，不读取副手、装备栏、盔甲栏、其他玩家或世界容器。

## 4. 真实 ItemStackMatcher 能力保留

生产 adapter 的 matcher 委托到：

```java
ItemStackMatcher.matchesIgnoringCount(realItemStack, matcher)
```

因此生产 itemSubmit 仍保留 5.10 已实现的真实 ItemStack 匹配能力，包括当前代码支持的 item id、damage、自定义名称、lore、custom_data、components 等非数量匹配项。

`ItemSubmitEvaluator` 只负责：

- 汇总匹配 stack 的总数量。
- 应用 `ignore` / `at_least` / `exactly` / `at_most` 规则。
- 构建 staged consume plan。
- 维护原子性和失败原因。

它不重新实现 ItemStack 细节匹配，避免和 `ItemStackMatcher` 分叉。

## 5. ConsumePlan 两阶段语义

仍保持 Round3 的两阶段语义：

1. plan 阶段只记录预扣，不修改真实 inventory。
2. 任一 requirement 不满足或 consume plan 不完整时，返回失败且 staged plan 为空。
3. 只有 `finalSuccess=true` 后，handler 才 apply staged plan。
4. cooldown 不抑制 consume；cooldown 只抑制 signal、message、sound、额外挥手动画、高频 history / lastResult 写入。

## 6. 自动化覆盖

本轮在 `stabilizationGuardTest` 中新增两项护栏：

- `testItemSubmitInventoryAdapterSeam`
  - 验证空 adapter view 不暴露 source stack。
  - 验证 adapter matcher 不会在无真实 stack 时凭空产生匹配。

- `testProductionItemSubmitPathUsesEvaluationResult`
  - 通过反射确认生产 handler 的 `evaluateItemSubmit(...)` 返回 `ItemSubmitEvaluationResult`。
  - 用于防止以后把生产 itemSubmit 路径退回独立判断结果结构。

Round3 已有测试继续覆盖：

- `ConsumePlan` / `ConsumePlanner` staged plan、跨 stack 消耗、防重复预扣。
- `ItemSubmitEvaluator` 多 requirement 满足 / 不足 / 原子消耗 / ignore / disabled requirement / 预扣冲突。
- `InteractionDecisionEvaluator` 中 `allow` / `require_item_match` / cooldown / consume 决策。
- 旧 JSON 样本兼容和默认值。

## 7. 仍需游戏内回归的路径

普通 JVM 测试仍不启动 Minecraft server，因此以下路径仍需保留游戏内回归：

- 带真实 custom name / lore / custom_data / components 的 itemSubmit 匹配。
- 真实玩家背包 / 热键栏扣除后的客户端同步表现。
- `UseBlockCallback` 的 `ActionResult` 对箱子、门、按钮、拉杆的实际原版交互影响。
- SignalBridge success / fail emit、receiver、action_relay、history 的联动。
- cooldown 中 success consume 与 fail lock 的实际体验。

本轮已经把生产 itemSubmit 判断入口统一到 evaluator，游戏内回归主要验证真实 Minecraft runtime 行为，不再验证两套判断逻辑是否分叉。

## 8. 是否修改生产代码

修改了少量生产代码：

- 新增 `ItemSubmitInventoryAdapter`。
- 调整 `VirtualBlockDeviceInteractionHandler` 的 itemSubmit 评估路径，改为委托 `ItemSubmitEvaluator`。

修改目的仅是让生产路径与测试 seam 共享同一套 itemSubmit 评估和 consume planning 结果。未改变业务语义、命令语义或 JSON schema。

## 9. 是否发现新问题

未发现新的生产 bug。

构建和 stabilization guard 测试通过。

## 10. 下一轮建议

建议下一轮继续保持小步：

- 补充 debug / doctor 的结构化诊断字段，优先覆盖 itemSubmit finalSuccess、consume plan failure、source/consume 不兼容、door half normalization。
- 不急于重构 `VirtualBlockDeviceInteractionHandler`，先把现有诊断和测试护栏补齐。
- 如果要进一步提高自动化程度，可考虑 Fabric GameTest 或 dedicated integration test，用真实 server runtime 覆盖 custom name / lore / components 和 ActionResult。

## 11. clean build 结果

已执行：

```powershell
./gradlew.bat clean build
```

结果：通过。
