# 5.15 稳定化审查报告 - 第三轮

本轮范围：最小 test seam / consume + itemSubmit 纯逻辑护栏。未新增玩法功能，未改命令语义，未重构 `SignalDeviceData` schema，未做 GUI / Web UI / ConditionEngine。

## 1. 本轮新增 helper / test seam

新增纯逻辑 helper：

- `ConsumePlan`
  - staged consume plan。
  - plan 阶段只记录预扣，不修改真实 inventory。
  - apply 阶段统一执行消费动作。
  - 支持 `copy` / `replaceWith` / `reserved` / `summary` / `primarySource` / `totalCount`。

- `ConsumePlanner`
  - 提供 `stageSingle` 和 `stageAcrossStacks`。
  - 支持 main hand / off hand / inventory slot 这类抽象 key。
  - 支持跨 stack 消耗。
  - 支持基于已有 `ConsumePlan` 的 reserved 计数，避免同一个 stack 被重复预扣。
  - 提供 `inventorySlotOrder`，统一 hotbar first / main inventory first 顺序。

- `ItemSubmitEvaluator`
  - 纯逻辑评估 itemSubmit requirements。
  - 输入 requirement、抽象 inventory source stack、是否启用 consume、已有 consume plan。
  - 输出 finalSuccess、failureReason、requirementResults、stagedConsumePlan、consumedSummary。
  - 支持 disabled requirement 跳过。
  - 支持 `ignore`：至少存在一个匹配 stack 才成功。
  - 支持和已有 consume plan 合并，防止 interactionItem consume 与 itemSubmit consume 重复占用同一 stack。

- `ItemSubmitEvaluationResult`
  - itemSubmit 评估结果 DTO。

- `InteractionDecisionEvaluator`
  - 将“是否允许原版交互”和“是否执行副作用”拆开。
  - cooldown 只影响 signal / message / sound / extra swing / history。
  - cooldown 不影响 consume。
  - cooldown 不解除 `require_item_match` 锁。

- `InteractionDecision`
  - interaction 决策结果 DTO。

## 2. 生产代码接入情况

已小范围接入 `VirtualBlockDeviceInteractionHandler`：

- 原 handler 内部 `ConsumePlan` / `ConsumeEntry` 已替换为新的 `ConsumePlan`。
- main hand / off hand consume 使用 `ConsumePlanner.stageSingle`。
- inventory consume 使用 `ConsumePlanner.stageAcrossStacks`。
- inventory consume slot 顺序委托到 `ConsumePlanner.inventorySlotOrder`。
- cooldown 后成功路径使用 `InteractionDecisionEvaluator` 判断是否执行 signal/message/sound/history/extra swing。
- cooldown 后失败路径使用 `InteractionDecisionEvaluator` 判断是否返回 `PASS` 或 `FAIL`。

保持不变：

- 右键事件仍只处理 `MAIN_HAND`。
- `allow` 模式仍不阻止原版交互。
- `require_item_match` 失败仍阻止原版交互。
- cooldown 不解除锁。
- cooldown 不跳过 consume。
- itemSubmit 与 interactionItem matcher 仍是互斥匹配模式。
- armor source 仍不支持 consume。
- 门上下半格归一化逻辑未改。
- 命令语义未改。
- JSON 字段名未改。

未接入：

- `ItemSubmitEvaluator` 尚未替换 handler 中的 itemSubmit 匹配统计逻辑。

原因：

- 生产 handler 的 itemSubmit 匹配支持真实 `ItemStackMatcher.matchesIgnoringCount`，包括 custom name / lore / custom_data / components。
- 本轮新增 `ItemSubmitEvaluator` 为纯逻辑 seam，测试使用抽象 `SourceStack` 和注入 matcher。
- 直接替换 handler 需要额外 adapter，属于更大的生产逻辑改动；本轮先用它锁住原子 consume / finalSuccess / reserved plan 的核心语义。

## 3. 自动化覆盖情况

自动化入口：

```text
./gradlew.bat stabilizationGuardTest
./gradlew.bat clean build
```

`stabilizationGuardTest` 已挂到 `check/build`。

### 3.1 consume planning

已自动化覆盖：

- main_hand：stack count=3，consumeCount=2，plan 成功，apply 后剩 1，summary 显示 x2。
- main_hand 不足：stack count=1，consumeCount=2，plan 失败，apply 不修改 stack。
- off_hand：stack count=3，consumeCount=2，apply 后剩 1。
- inventory 跨 stack：2 + 3，consumeCount=5，plan 成功，apply 后两个 stack 都扣完。
- inventory 不足：2 + 2，consumeCount=5，plan 失败，apply 不修改两个 stack。
- 防重复预扣：同一 stack 已预扣 2，再尝试预扣 3 且总量不足，第二次 plan 失败，未 apply 的 transaction 不修改真实 stack。

结论：consume planning 已可自动化测试。

### 3.2 itemSubmit evaluation

已自动化覆盖：

- 全部满足，不启用 consume：3 diamond + 2 emerald，finalSuccess=true，consume plan 为空。
- requirement 不满足：3 diamond + 1 emerald，finalSuccess=false，failureReason 指向 emerald requirement。
- 原子消耗成功：3 diamond + 2 emerald，apply 后扣 3 + 2。
- 原子消耗失败：3 diamond + 1 emerald，finalSuccess=false，apply 不扣任何物品。
- disabled requirement：不参与判断和消耗。
- ignore：有至少一个匹配 stack 成功；没有匹配 stack 失败。
- 与 interactionItem consume 预扣同时存在：
  - 先预扣 2 diamond，总量 4，itemSubmit 还需要 3 diamond -> 失败，不重复使用同一 stack。
  - 先预扣 2 diamond，总量 5，itemSubmit 还需要 3 diamond -> 成功，apply 后扣完 5。

结论：itemSubmit evaluation 的纯逻辑已经可自动化测试。

### 3.3 vanilla policy / cooldown decision

已自动化覆盖：

- `allow + failure`：允许原版交互。
- `require_item_match + failure`：阻止原版交互。
- `require_item_match + failure + cooldown`：仍阻止原版交互，并抑制 signal/message/sound。
- `require_item_match + success + consume enabled + cooldown`：允许原版交互，仍执行 consume，但抑制 signal/message/sound。
- `require_item_match + success + no cooldown`：允许原版交互，执行 consume 和副作用。
- `consume plan failed`：finalSuccess=false，`require_item_match` 下阻止原版交互。

结论：vanilla policy / cooldown decision 已可自动化测试。

### 3.4 旧 JSON 样本兼容

新增测试资源：

- `src/test/resources/stabilization/signal_device_legacy_5_4.json`
- `src/test/resources/stabilization/signal_device_legacy_5_5.json`
- `src/test/resources/stabilization/signal_device_legacy_5_8.json`
- `src/test/resources/stabilization/signal_device_legacy_5_10.json`
- `src/test/resources/stabilization/signal_device_legacy_5_12.json`
- `src/test/resources/stabilization/signal_device_legacy_5_14.json`

已自动化覆盖：

- Gson 反序列化到 `SignalDeviceData`。
- 调用 `normalized()`。
- 断言默认值：
  - `interactionItemSource = main_hand`
  - `vanillaInteractionPolicy = allow`
  - `consumeEnabled = false`
  - `consumeCount = 1`
  - `consumeSource = matched_source`
  - `inventoryConsumeOrder = hotbar_first`
  - `itemSubmitEnabled = false`
  - `itemSubmitRequirements = []`
  - `itemConditions = []`
  - 缺失 container / interaction 字段时默认安全关闭。

结论：旧 JSON 样本兼容已自动化。

## 4. 仍只能游戏内回归的路径

以下路径仍建议保留游戏内回归：

- Fabric `UseBlockCallback` 实际 `ActionResult.PASS/FAIL` 是否阻止箱子 / 门 / 按钮 / 拉杆。
- 门上下半格归一化与真实门 blockstate 行为。
- success/fail signal 是否真实进入 `SignalBridgeServer.emit` 并联动 receiver / action_relay。
- success/fail message / sound / swing animation 真实客户端体验。
- 真实 `ItemStackMatcher` 对 custom name / lore / custom_data / components 的匹配。
- creative 玩家 inventory 消耗表现。

原因：

- 这些路径依赖 `ServerPlayerEntity`、`ServerWorld`、Fabric callback、Minecraft `ItemStack` 数据组件和客户端可见行为。
- 本轮只抽纯逻辑 seam，不引入 Fabric GameTest 或服务端集成测试框架。

## 5. 是否修改生产代码

修改了生产代码，但仅限最小 test seam 接入：

- 新增纯逻辑 helper。
- `VirtualBlockDeviceInteractionHandler` 将 consume plan 和 cooldown decision 委托给 helper。
- 未改变命令语义。
- 未改变 JSON 字段。
- 未改变已验收过的业务规则。

## 6. 是否发现新问题

未发现新的生产 bug。

本轮发现并确认的测试限制：

- `ItemSubmitEvaluator` 目前是纯逻辑 seam，尚未完全替换生产 handler 的 itemSubmit 匹配路径。
- 若后续要让 itemSubmit evaluation 100% 覆盖生产路径，需要增加真实 `ItemStack` adapter，并小心保留 custom name / lore / custom_data / components 匹配能力。

## 7. clean build 结果

执行命令：

```text
./gradlew.bat clean build
```

结果：

```text
BUILD SUCCESSFUL
stabilizationGuardTest: Stabilization guard checks passed.
```

## 8. 下一轮建议

建议下一轮继续保持 5.15 稳定化范围：

1. 将 `ItemSubmitEvaluator` 通过真实 `ItemStack` adapter 小范围接入 handler。
   - 目标：让 production itemSubmit matching 和测试 seam 完全重合。
   - 风险点：必须保留 custom name / lore / custom_data / components。
2. 为 `VirtualBlockDeviceInteractionHandler` 增加结构化 debug result DTO。
   - 目标：debug/doctor/Web UI 未来不用解析聊天文本。
3. 补 Fabric GameTest 或轻量集成测试可行性调研。
   - 目标：覆盖 `ActionResult`、门半格、箱子打开阻止、真实 player inventory。

本轮不建议继续扩大重构面。
