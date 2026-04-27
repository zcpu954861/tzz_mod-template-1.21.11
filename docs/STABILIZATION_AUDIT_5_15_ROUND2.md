# 5.15 稳定化审查报告 - 第二轮

本轮范围：只做稳定性护栏、回归验证和报告。未新增玩法功能，未改命令语义，未重构生产逻辑，未提交、合并、推送或打标签。

## 1. 本轮目标

本轮针对上一轮审查中的三类 P1 风险建立防回归基础：

- P1-1：`SignalDeviceData` 字段保留回归测试。
- P1-6：`interactionItem` / `itemSubmit` / `consume` / `cooldown` / `require_item_match` 核心路径回归测试。
- P1-7：旧 `signal_devices.json` / 旧构造数据兼容默认值测试。

由于当前项目没有现成 JUnit 或 Fabric GameTest 测试框架，本轮采用最小依赖方案：新增一个可由 Gradle 运行的 Java main 型护栏测试 `stabilizationGuardTest`，并挂到 `check` / `build` 流程中。这样后续每次 `./gradlew.bat clean build` 都会执行这些防回归检查。

## 2. 新增测试与验证项

### 2.1 自动化护栏任务

新增 Gradle 任务：

```text
stabilizationGuardTest
```

执行方式：

```text
./gradlew.bat stabilizationGuardTest
./gradlew.bat clean build
```

该任务编译并运行：

```text
src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java
```

项目没有测试框架时，默认 `test` 任务不会发现 JUnit 测试。为避免 `test` 因“存在测试源码但没有测试框架测试用例”误失败，本轮关闭了默认 `test` 的 `failOnNoDiscoveredTests`，实际护栏由 `stabilizationGuardTest` 执行。

### 2.2 P1-1 字段保留自动化覆盖

已自动化覆盖：

- 构造包含 5.14 主要字段的 `virtual_block_device`：
  - redstone / condition
  - interaction / container
  - `itemConditions`
  - `interactionItem` matcher
  - success / fail channel、message、sound
  - `interactionItemSource`
  - `vanillaInteractionPolicy`
  - `consumeSource`
  - `inventoryConsumeOrder`
  - `itemSubmitEnabled`
  - `itemSubmitConsumeEnabled`
  - `itemSubmitConsumeOrder`
  - `itemSubmitRequirements`
  - runtime summary / last result 字段
- 反射调用 `SignalDeviceStore.withInteractionItemMatcher(...)` 后确认：
  - `itemSubmitRequirements` 不丢失。
  - container 配置不丢失。
  - `itemConditions` 不丢失。
  - redstone / condition 配置不丢失。
  - success / fail / consume 配置不回退。
- 反射调用 `SignalDeviceStore.withItemSubmit(...)` 后确认：
  - `interactionItemMatcher` 不被清空。
  - success / fail 配置不丢失。
  - container / itemCondition / redstone / condition 配置不丢失。
- 调用 `ItemStackMatcherSupport.withInteractionSettingsFrom(...)` 后确认：
  - `successChannel` / `failChannel` 保留。
  - `successMessage` / `failMessage` 保留。
  - `interactionItemSource` 保留。
  - `vanillaInteractionPolicy` 保留。
  - `consumeEnabled` / `consumeCount` / `consumeSource` / `inventoryConsumeOrder` 保留。
  - `consumeCount` 不会回到默认 `1`。

覆盖结论：P1-1 的关键字段复制风险已建立自动化护栏。

### 2.3 P1-7 旧数据兼容自动化覆盖

已自动化覆盖：

- 使用旧构造器创建缺少新版字段的 `SignalDeviceData`，再执行 `normalized()`。
- 使用旧构造器创建缺少 5.11～5.14 字段的 `ItemStackMatcherData`，再执行 `normalized()`。

已验证默认值：

- `interactionItemSource = main_hand`
- `vanillaInteractionPolicy = allow`
- `interactionItemConsumeSource = matched_source`
- `interactionItemInventoryConsumeOrder = hotbar_first`
- `consumeEnabled = false`
- `consumeCount = 1`
- `itemSubmitEnabled = false`
- `itemSubmitConsumeEnabled = false`
- `itemSubmitRequirements = []`
- `itemConditions = []`
- container / interaction / condition 默认关闭

未完全自动化的部分：

- 没有直接加载多版本真实 `signal_devices.json` 样本文件。
- 没有通过 `SignalDeviceStore` 的服务器持久化路径做端到端读写测试。

原因：

- `SignalDeviceStore` 的真实读写路径依赖 Minecraft server 上下文和 `PersistentState`。
- 本轮要求避免为测试大改生产逻辑，因此暂不抽离存储适配层。

覆盖结论：P1-7 的构造器兼容和默认值已覆盖；真实 JSON 文件读写建议下一轮补充样本级测试或 Fabric 集成测试。

### 2.4 P1-6 核心逻辑自动化覆盖

已自动化覆盖：

- `countMode=ignore` 会将 `requiredCount` 归一化为 `0`。
- `countMode=ignore` 的显示文本不为空，且不会直接显示误导性的 `0`。
- `ContainerItemCountMode.IGNORE.matches(...)` 不检查数量。
- 与 `itemSubmit` / `interactionItem` 互斥相关的字段复制路径不会互相清空配置。

未自动化覆盖：

- `main_hand` / `off_hand` / `inventory_contains` 的真实 `ItemStack` 消耗。
- inventory 跨 stack consume plan。
- itemSubmit 两阶段 plan / apply 原子消耗。
- `require_item_match` 下 cooldown 不解除锁。
- cooldown 中成功仍执行 consume。
- 门上下半格、`ActionResult.PASS/FAIL`、原版交互阻止行为。

原因：

- 这些路径目前集中在 `VirtualBlockDeviceInteractionHandler` 内部，依赖 `ServerPlayerEntity`、`ServerWorld`、Fabric `UseBlockCallback`、真实 `ItemStack` 和玩家 inventory mutation。
- 直接单测需要引入 Fabric/Minecraft 运行时测试环境，或抽出 consume planning / submit evaluation 纯逻辑服务。
- 本轮明确要求不重构 `VirtualBlockDeviceInteractionHandler`，因此未为测试拆生产代码。

覆盖结论：P1-6 已建立字段与基础枚举语义护栏；核心运行时行为仍需要游戏内回归或下一轮小范围测试 seam。

## 3. 只能作为游戏内回归清单的项目

以下项目本轮没有自动化，仍建议保留为游戏内回归清单：

- `main_hand consumeCount=2`：3 个钻石成功后剩 1 个，1 个钻石失败且不消耗。
- `off_hand consumeCount=2`：副手 3 个成功后剩 1 个。
- `inventory_contains consumeCount=5`：2+3 跨 stack 成功；2+2 失败且不消耗。
- itemSubmit：3 钻石 + 1 绿宝石失败且不消耗任何物品。
- itemSubmit：3 钻石 + 2 绿宝石成功并扣 3+2。
- cooldown 中失败：`require_item_match` 仍阻止原版交互。
- cooldown 中成功：允许原版交互，且 consume 仍执行。
- `itemSubmitEnabled=true`：不执行单物品 `interactionItem` matcher / consume。
- `itemSubmit disable` 后不会自动恢复 `interactionItem` matcher。
- 绑定门上半格/下半格，右键另一半时 `require_item_match` 不能被绕过。

## 4. 是否发现新的 bug

本轮新增自动化护栏没有发现新的生产 bug。

保留风险：

- consume plan / itemSubmit evaluation 仍缺少可直接单测的纯逻辑入口。
- `SignalDeviceStore` 真实 JSON 读写兼容仍缺少样本级自动化。
- `VirtualBlockDeviceInteractionHandler` 仍然职责较重，后续重构时回归风险高。

这些风险与第一轮报告一致，建议进入下一轮 5.15 修复/测试 seam 设计，不建议在本轮直接大改。

## 5. 是否修改生产代码

未修改 `src/main/java` 生产代码。

本轮修改：

- `build.gradle`：新增 `stabilizationGuardTest` 验证任务，并挂到 `check`。
- `src/test/java/.../StabilizationGuardTest.java`：新增无外部依赖的 Java main 护栏测试。
- `docs/STABILIZATION_AUDIT_5_15_ROUND2.md`：新增本报告。

## 6. 是否需要下一轮修复

建议需要，但仍应保持 5.15 稳定化范围，不做玩法扩展。

下一轮优先级建议：

1. 抽出最小 consume planning / itemSubmit evaluation 纯逻辑 helper。
   - 目标：不改变业务语义，只把现有内部逻辑变成可单测。
   - 覆盖：跨 stack 消耗、原子 plan/apply、重复 stack 预扣、防部分消耗。
2. 增加旧 JSON 样本兼容测试。
   - 目标：用多版本 JSON 样本验证 `SignalDeviceData.normalized()` 和 store 读取默认值。
   - 避免依赖真实 server 时，可先测试 Gson 反序列化到 record + normalized。
3. 为 `require_item_match` + cooldown + vanilla policy 建立可测试决策对象。
   - 目标：将“是否阻止原版交互”和“是否执行副作用”拆成结构化结果，减少 handler 回归。

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

## 8. 本轮结论

- P1-1：已建立核心自动化护栏。
- P1-6：已建立部分自动化护栏，真实运行时路径仍需下一轮小范围 test seam 或游戏内回归。
- P1-7：旧构造器默认值已自动化；真实 JSON 样本兼容仍建议下一轮补齐。
- 未发现新的 P0/P1 生产 bug。
- 未修改生产代码。
