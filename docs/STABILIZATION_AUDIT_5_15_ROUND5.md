# 5.15 稳定化审查 Round5：debug / doctor 结构化诊断增强

## 1. 本轮目标

Round5 的目标是增强 `device debug`、`doctor` 和 `info/debug` 相关显示的诊断基础，为未来 Web Admin UI 提供可复用的结构化数据模型。

本轮没有新增玩法功能，没有改变命令参数、JSON 字段或 consume / itemSubmit / cooldown / lock 的业务语义。

## 2. 新增诊断 DTO / service

新增结构化诊断模型：

- `DiagnosticSeverity`：稳定严重级别，包含 `ERROR`、`WARNING`、`INFO`。
- `DiagnosticIssue`：单条诊断问题，包含 `severity`、`code`、中文标题、中文说明、建议、相关命令、设备与频道上下文。
- `DeviceDiagnostic`：设备级诊断结果，可统计各严重级别数量。
- `InteractionItemDiagnostic`：interactionItem 子系统诊断摘要。
- `ItemSubmitDiagnostic`：itemSubmit 子系统诊断摘要。
- `VirtualBlockDeviceDiagnosticService`：virtual_block_device 诊断服务，供命令、doctor 和未来 Web UI 复用。

诊断项使用稳定 code，例如：

- `device_disabled`
- `channel_empty`
- `interaction_item_template_missing`
- `consume_source_unsupported`
- `item_submit_no_enabled_requirements`
- `item_submit_requirement_not_met`
- `block_id_mismatch`
- `chunk_unloaded`
- `channel_no_consumers`

## 3. device debug 增强项

`/tzz signal device debug <device>` 对 `virtual_block_device` 增加结构化诊断渲染：

- 基础状态：设备禁用、区块未加载、空气位置、blockId 不一致、门上下半格归一化提示。
- channel 消费者：主 channel、interactChannel、successChannel、failChannel、container channels 是否无 listener / receiver / action_relay。
- interactionItem：source、consume 支持性、consumeCount、vanillaInteractionPolicy、模板缺失、最近失败原因。
- itemSubmit：当前匹配模式、requirement 总数、启用数量、disabled requirement、最近未满足 requirement、consumeCount 异常。
- vanilla interaction / lock：`require_item_match` 是锁，cooldown 不解除锁，也不跳过 consume。
- container / itemCondition：container channel、itemConditions 数量、slot 越界、itemId 无效、channel 为空。
- cleanup 风险：空气位置可 cleanup，blockId 不一致不自动删除，未加载区块不强制加载。

命令输出仍是中文聊天文本，但内容来自 DTO，不再只依赖散落的字符串拼接。

后续烟测后又补了一轮玩家可见渲染优化：

- 每条 issue 使用固定格式：`[警告] 标题` / `[错误] 标题` / `[信息] 标题`。
- 详情按 `设备`、`位置`、`频道`、`条件`、`当前`、`要求`、`说明`、`建议`、`代码` 分行显示。
- 长说明和建议按中文标点主动拆行，减少 Minecraft 聊天栏自动换行造成的混乱。
- 每条 issue 后添加灰色分隔线，避免多条诊断粘连。
- `DiagnosticIssue.code` 继续保留为机器可读字段，但聊天中只作为最后的 `代码：xxx` 辅助信息显示。

## 4. Signal Doctor 增强项

`/tzz signal doctor` 增加设备层摘要检查：

- disabled device 数量。
- channel 为空的设备。
- virtual_block_device 的结构化诊断中 `ERROR` / `WARNING` 项。
- source 与 consume 不兼容。
- itemSubmit 启用但无启用 requirement。
- interactionItem matcher 缺失但启用。
- blockId 不一致、空气位置、区块未加载等运行时可判断项。

doctor 仍遵守性能边界：

- 不强制加载区块。
- 不扫描世界。
- 不扫描玩家背包。
- 只检查已登记设备和已有内存状态。
- 对需要玩家上下文的内容，只做静态诊断或输出当前无法静态判断。

## 5. enum / displayName 中文化

本轮统一或补充了玩家可见 displayName：

- `InteractionItemSource`
  - `main_hand` -> `主手（main_hand）`
  - `off_hand` -> `副手（off_hand）`
  - `inventory_contains` -> `背包/热键栏（inventory_contains）`
  - `armor_head` -> `头盔槽（armor_head）`
  - `armor_chest` -> `胸甲槽（armor_chest）`
  - `armor_legs` -> `护腿槽（armor_legs）`
  - `armor_feet` -> `靴子槽（armor_feet）`
  - `armor_any` -> `任意盔甲槽（armor_any）`
- `InteractionItemVanillaPolicy`
  - `allow` -> `允许原版交互（allow）`
  - `require_item_match` -> `需要物品匹配才允许原版交互（require_item_match）`
- `InteractionItemConsumeSource`
  - 保持 `匹配来源 / 主手 / 副手 / 背包/热键栏` 中文显示。
- `InventoryConsumeOrder`
  - 保持 `优先热键栏 / 优先主背包` 中文显示。
- `ContainerItemCountMode`
  - `ignore` -> `不检查数量（ignore）`
- `BlockStateConditionMode`
  - `condition_both` -> `进入和退出都触发（condition_both）`
- `VirtualBlockDeviceMode`
  - `redstone_both` -> `通电和断电都触发（redstone_both）`

调试输出可以在中文主信息后保留内部 ID，方便高级排查，但不再把内部 enum 作为唯一主信息。

## 6. 自动化测试覆盖

`stabilizationGuardTest` 新增覆盖：

- displayName 中文化：
  - `main_hand`、`off_hand`、`inventory_contains`、`armor_head`、`armor_any`
  - `matched_source`、`hotbar_first`
  - `require_item_match`
  - `ignore`
  - `condition_both`
  - `redstone_both`
- `DiagnosticIssue` 模型：
  - `ERROR` / `WARNING` / `INFO` 可统计。
  - code、title、message、suggestion 非空。
- `VirtualBlockDeviceDiagnosticService` 纯数据诊断：
  - itemSubmit 启用但没有启用中的 requirement 会生成 `item_submit_no_enabled_requirements`。
  - interactionItem diagnostic 能读取 consumeEnabled 和 source 是否支持 consume。
  - source=armor_* 且 consumeEnabled=true 会生成 `consume_source_unsupported`。

Round2 到 Round4 的既有测试继续保留并通过：

- SignalDeviceData 字段保留。
- 旧 JSON 样本兼容。
- ConsumePlan 两阶段 plan/apply。
- itemSubmit evaluation。
- cooldown / vanilla policy decision。
- production itemSubmit path 接入 evaluator 的护栏。

## 7. 仍需游戏内回归的部分

以下路径依赖真实 server/world/player/channel 状态，仍建议保留游戏内回归：

- `/tzz signal device debug <device>` 对真实区块加载、空气、blockId、门上下半格的显示。
- `/tzz signal doctor` 在真实 listener / receiver / action_relay 组合下的 channel 消费者统计。
- channel 无消费者时的 WARNING 是否符合管理员预期。
- itemSubmit 最近匹配状态和真实背包内容之间的诊断说明是否足够明确。
- receiver / action_relay 联动 smoke test。

## 8. 是否修改生产代码

本轮修改了生产代码，但仅限诊断与显示层：

- 新增 DTO / diagnostic service。
- `device debug` 使用结构化诊断结果渲染中文文本。
- `doctor` 增加设备诊断摘要。
- enum / mode displayName 中文化。

未修改：

- JSON 字段。
- 命令参数。
- SignalBridge emit 路径。
- consume / itemSubmit / cooldown / require_item_match 行为。
- itemSubmit 与 interactionItem 的互斥模式。

## 9. 是否发现新问题

未发现新的生产逻辑 bug。

构建过程中发现并修正了测试构造问题：测试想覆盖“itemSubmit 启用但无可用 requirement”的异常配置，但 normalized 会把完全空 requirement 的 itemSubmitEnabled 折叠为 false。测试已调整为“存在 requirement 但全部 disabled”，该状态能在 normalized 后保留，并能覆盖 doctor 需要诊断的真实风险。

## 10. clean build 结果

`./gradlew.bat clean build` 已通过。

`stabilizationGuardTest` 已通过，输出：

```text
Stabilization guard checks passed.
```

## 11. 下一轮建议

建议 Round6 聚焦小范围修复和收口，不做大重构：

1. 游戏内回归 `device debug` 和 `doctor` 的实际聊天输出，确认不会过长、不会刷屏、中文可读。
2. 根据 Round1 P1 清单，补齐缺失但低风险的 debug/doctor 诊断项。
3. 检查 README / SIGNAL_BRIDGE 是否需要补充“doctor 结构化诊断”说明。
4. 视用户确认后，再进入 5.15 最终回归与收尾流程。
