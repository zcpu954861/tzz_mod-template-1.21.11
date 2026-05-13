# WebAdmin Unified ItemSubmit Editor 7.11 Current Context

## 当前阶段

7.11 WebAdmin Unified ItemSubmit Requirement List Editor。

当前稳定基线：`v1.40.0-local-test-mcp-foundation`。

本阶段目标是把 7.10 的单物品 itemSubmit 编辑器升级为统一的 `itemSubmitRequirements[]` 列表编辑器。7.11 不再把“单物品提交”和“多物品提交”做成两个入口，而是始终围绕现有 requirement list 工作：

- 0 个 requirement：未配置 itemSubmit。
- 1 个 requirement：单物品提交。
- 2+ 个 requirement：多物品提交。

itemSubmit 仍然不是新触发源。它属于玩家右键交互触发之后的条件 / 提交层。

## 与 7.10 的兼容迁移关系

7.10 的单物品编辑器语义在 7.11 中成为“1 个 requirement”的自然特例。

7.10 对多 requirement 的只读拒绝需要取消：已有 2+ requirements 的设备必须能进入同一个统一编辑器，保存时不得覆盖成单 requirement，不得丢失顺序，不得降级 matcher、display template、components 或 runtime last 字段。

旧的 single-item-submit API / session / lock 可以被兼容复用或重命名为 unified itemSubmit 语义，但 WebAdmin 文案、context 和测试必须明确 7.11 后是统一 itemSubmit editor，不再是 single-only editor。

## 右键交互条件层归属

正确运行链路：

玩家右键 VBD -> 右键交互触发 -> itemSubmit requirement list 判定 -> 所有启用 requirement 按旧运行时规则判断 -> 成功 / 失败 -> consume 旧字段处理 -> 原版交互策略 -> signal / feedback / runtime result。

要求：

- itemSubmit 只在右键交互启用后显示完整编辑入口。
- 右键交互未启用时，不显示完整编辑入口。
- 如果右键交互未启用但已有 itemSubmit 数据，只显示 warning。
- WebUI 文案不能把 itemSubmit 误导为独立触发源。
- 运行时继续复用 `ItemSubmitEvaluator`、`InteractionItemVanillaPolicy`、`ConsumePlanner` 等旧逻辑。

## 统一 requirement list UI 方向

游戏内 GUI 使用一个统一界面：

- 左侧 requirement list，可滚动。
- 有“添加条件”按钮。
- 每项显示名称或序号、item summary、enabled 状态和 count summary。
- 点击列表项选中当前 requirement。
- 不使用固定一堆空格子代表未配置项，避免空格子污染 UI。
- 右侧编辑当前选中 requirement 的模板物品、enabled、countMode、count / requiredCount、matcher options、consumeCount。
- 删除 requirement 必须二次确认。
- 上移 / 下移为 P0 可控项；如果风险过大可降 P1。
- 底部玩家背包 / 快捷栏保持 7.10 类原版箱子操作感。

## 必须保留的旧多物品 itemSubmit 数据语义

7.11 不允许只按 7.10 单物品经验扩展 UI。实现前必须扫描并保留旧多物品 itemSubmit 能力：

- `VirtualBlockItemSubmitCommand`
- `ItemSubmitEvaluator`
- `ItemSubmitEvaluationResult`
- `ItemSubmitRequirementData`
- `ConsumePlan`
- `ConsumePlanner`
- `ItemSubmitInventoryAdapter`
- `SignalDeviceData` itemSubmit 字段
- `VirtualBlockDeviceInteractionHandler` 中 `evaluateItemSubmit` / `consumePlan` 相关逻辑
- `SignalDeviceStore.updateVirtualItemSubmit`

必须确认并保留：

- 多 requirement 同时判定语义：所有 enabled requirements 都满足才算最终成功；任一 enabled requirement 不满足则整体失败；disabled requirement 不参与成功条件，但配置和状态必须保留。
- 原子提交 / staged consume 语义：`itemSubmitConsumeEnabled=false` 时只判定不消耗；`itemSubmitConsumeEnabled=true` 时先 stage 所有 requirement 的消耗计划，只有全部 stage 成功才 apply consume。任一 requirement consume plan 失败不得消耗任何物品，不能出现半提交状态。
- consume 字段：全局 `itemSubmitConsumeEnabled`、全局 `itemSubmitConsumeOrder`、每个 requirement 的 `consumeCount`。`count` / `requiredCount` 是匹配数量，`consumeCount` 是消耗数量，二者必须在 UI 文案中明确区分。
- matcher 能力：itemId/template、`countMode`、`matchDamage`、`matchCustomName`、`matchLore`、`matchCustomData`、`matchComponents`、display template / `templateDisplayStack`。附魔书、components、自定义名、Lore、damage 回显不能丢。
- runtime last 字段：`lastMatched`、`lastMatchedCount`、`lastCheckGameTime`、`lastResult`、`lastItemSubmitMatched`、`lastItemSubmitFailureReason`、`lastItemSubmitConsumedSummary`、`lastItemSubmitResult`。这些字段不能被 WebUI 编辑乱写；保存配置时应保留或由运行逻辑更新。
- 多 requirement 顺序：保存时必须按用户列表顺序写入 `itemSubmitRequirements[]`。删除 requirement 必须二次确认；上移 / 下移如实现，必须保持数据顺序一致。

运行时测试必须覆盖：

- requirement A + requirement B，玩家只满足 A 不满足 B，`use_block` 后整体失败。
- 如果 consume enabled=true，部分满足失败时 A 也不能被消耗。
- 玩家同时满足 A+B 时整体成功。
- consume enabled=true 时 A+B 按 staged plan 一次性消耗。
- 消耗失败场景不产生半消耗。

完成报告必须明确回答旧 itemSubmit 是否是 all-or-nothing / staged consume，7.11 是否保留该语义，以及自动测试是否覆盖“部分满足不消耗”和“全部满足才消耗”。

## 单项 / 多项 UI 自适应显示规则

统一编辑器不代表总是显示复杂多物品管理按钮。UI 必须按 requirement 数量自适应：

0 个 requirement：

- 显示“尚未配置提交条件”。
- 显示“添加提交条件”按钮。
- 不显示排序、删除、批量操作、多项列表管理按钮。
- 不显示空槽矩阵，不用一堆空格子代表未配置项。

1 个 requirement：

- 视觉上尽量保持 7.10 单物品提交体验。
- 显示当前 requirement 的模板槽和配置项。
- 不显示多物品管理按钮、上移 / 下移、批量启用 / 批量禁用、重复 / 复制 requirement。
- 不显示复杂列表滚动控件，除非布局必须复用容器，但不能让用户感觉这是多项列表。
- 删除唯一 requirement 的按钮默认不显示；如果后续支持清空配置，必须使用“清空提交条件 / 禁用 itemSubmit”这类明确入口并二次确认。
- “添加提交条件”可以显示，用于从单物品提交扩展为多物品提交。点击后新增第二个 requirement，并自动切换到多项列表 UI。

2 个及以上 requirement：

- 才显示多物品 requirement list、滚动列表、删除 requirement、上移 / 下移。
- 上移 / 下移只在 2 个及以上 requirement 时显示，首项不能上移，末项不能下移。
- 删除 requirement 必须二次确认。
- 删除后如果只剩 1 个 requirement，UI 自动回到单物品简化显示。
- 保存时按列表顺序写入 `itemSubmitRequirements[]`。

WebAdmin 摘要同样按 0 / 1 / N 自适应：

- 0：显示“未配置 itemSubmit”，只显示“添加提交条件 / 编辑 itemSubmit”入口，不显示多物品管理按钮。
- 1：显示“单物品提交”，展示 itemId、countMode、count、consumeCount、matcher options、consume 状态，不显示排序、删除、批量操作按钮。
- 2+：显示“多物品提交：N 个条件，M 个启用”，展示 requirement list 与多项管理能力。

验收必须覆盖 0、1、2、3+ requirement 状态，验证 1 -> 2 切换到多项列表，2 -> 1 回到单物品简化显示，1 个 requirement 时多项管理按钮不显示，2+ requirement 时多项管理按钮显示且可用，删除唯一 requirement 不会误删配置。

## 允许范围

- 统一 itemSubmit requirement list 编辑器。
- 0/1/N requirements 的统一回显、编辑、保存。
- `itemSubmitEnabled`、`itemSubmitConsumeEnabled`、`itemSubmitConsumeOrder`、`InteractionItemVanillaPolicy` 等全局 itemSubmit / 右键交互条件层字段编辑。
- per requirement 的模板物品、enabled、countMode、count / requiredCount、matcher options、consumeCount 编辑。
- 复用 7.10 display template / components 保存与回显逻辑。
- 保存使用 scoped itemSubmit update，并保持 session / lock / fingerprint / WebAdminWriteResult / audit / realtime。
- WebAdmin VBD 详情页与统一设备配置 modal 的右键交互 section 内显示统一 itemSubmit 摘要和唯一编辑入口。
- Local Test MCP 逻辑测试、WebAdmin 截图矩阵、Minecraft GUI 截图矩阵。

## 禁止范围

- 不做 ConditionEngine。
- 不做 inventory / equipment / armor 来源编辑。
- 不新增 consume strategy。
- 不做成功 / 失败路径可视化。
- 不做 Scratch-like / graph / mind-map。
- 不做 raw JSON 编辑。
- 不做任意 NBT path 编辑。
- 不做区域控制器编辑。
- 不做 WebAdmin 大改版。
- 不新增触发源。
- 不新增每版本专属 MCP scenario tool，除非现有原子能力无法测试。
- 不使用 OS 鼠标键盘操作。
- 不使用 Minecraft GUI 坐标点击。
- 不 commit / push / merge / tag，除非用户截图验收后明确允许 checkpoint。

## MCP 自动化测试要求

7.11 必须使用现有 Local Test MCP / TestBridge 原子能力组合逻辑测试：

1. start_client autoEnterWorld。
2. wait_world / wait_webadmin / wait_testbridge。
3. prepare_test_world。
4. WebAdmin login。
5. 准备 VBD + receiver。
6. 启用右键交互。
7. 打开 unified itemSubmit GUI。
8. `gui_current` 确认 GUI 类型。
9. 添加多个 requirement。
10. 配置 diamond 与 emerald 或 enchanted_book requirement。
11. 保存并 inspect device。
12. 满足条件 use_block 成功。
13. 不满足条件 use_block 失败。
14. 检查 signal / receiver / device last result / Doctor / console errors。
15. `report.write` 输出报告。

不要为了 7.11 新增专属 scenario tool；优先复用原子工具。若缺少必要原子能力，只能新增安全的通用 GUI 原子能力，并更新 guard。

本阶段允许扩展的通用 GUI 原子能力：

- `minecraft.gui_select_requirement`
- `minecraft.gui_add_requirement`
- `minecraft.gui_delete_requirement`
- `minecraft.gui_set_count_mode`
- `minecraft.gui_set_requirement_enabled`
- `minecraft.gui_set_matcher_options`
- `minecraft.gui_set_consume`
- `minecraft.gui_set_global`

这些工具只作用于当前已打开的受支持 TestBridge GUI，仍走 loopback + token 的 `/api/testbridge/gui/*`，不使用 OS 鼠标键盘，不做坐标点击，不开放任意 GUI 操作。

## UI 截图矩阵要求

游戏内 GUI 必须截图 unified itemSubmit requirement list editor：

- 小分辨率：854x480。
- 4K 200% scaled：3840x2160 @ 2，按当前 MCP 工具定义的 profile。
- GUI scale 至少覆盖 2 / 3 / 4 中的关键组合。

WebAdmin 必须截图：

- VBD 详情页右键交互区域的 itemSubmit 摘要。
- 统一设备配置 modal 的 itemSubmit 区域。
- 854x480。
- 4K 200% scaled：1920x1080 CSS viewport + deviceScaleFactor 2。
- 可加 1920x1080 / 2560x1440。

截图矩阵报告必须列出截图路径，并明确需要用户人工确认。用户明确回复“UI 验收通过，可以 checkpoint”前不得 checkpoint。

推荐复用现有截图矩阵工具：

- WebAdmin WebUI：`webadmin.responsive_matrix`，并使用 CSS viewport、`deviceScaleFactor`、expected physical screenshot size 区分真实 4K scaled 视觉。
- Minecraft 游戏内 GUI：`minecraft.client_screenshot_matrix`，并覆盖 window size + GUI scale 组合。

Codex 生成截图后必须先做明显问题预检，但不能替代用户验收。预检项包括：明显遮挡、按钮被背包 / footer / sidebar / topbar 覆盖、横向溢出、内容裁切、关键按钮不可见、关键卡片 / 表格错位、小分辨率与 4K scaled 的明显布局差异。每张截图必须标记 `pass`、`warning`、`fail` 或 `needs_user_review`，并写明 reason。若 Codex 无法直接视觉判断某张图，必须标记 `needs_user_review`，不得假装通过。最终 UI 是否通过仍必须等待用户确认。

## Guard / Smoke Marker

本阶段 guard / smoke 需要覆盖：

- 7.11 context exists.
- unified itemSubmit editor marker.
- no separate single/multi editor marker.
- requirement list scroll marker.
- add requirement marker.
- delete requirement confirm marker.
- multiple requirements editable marker.
- old multi requirement read-only refusal removed marker.
- itemSubmit still under right-click interaction marker.
- itemSubmit hidden/warning when interaction disabled marker.
- global consume fields preserved marker.
- per requirement consumeCount marker.
- matcher options per requirement marker.
- display template components preserved marker.
- save uses scoped itemSubmit update marker.
- cancel does not save marker.
- no raw JSON / NBT path marker.
- no ConditionEngine marker.
- no new consume strategy marker.
- MCP automatic logic test marker.
- UI screenshot matrix required marker.
- screenshot obvious issue precheck marker.
- per-screenshot pass / warning / fail / needs_user_review marker.
- user approval required before checkpoint marker.
- no OS mouse/keyboard marker.
- no coordinate clicking marker.
- no arbitrary shell marker.
- no git mutation marker.
- no external host marker.
