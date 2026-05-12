# WebAdmin Single ItemSubmit 7.10 Current Context

## 当前阶段

7.10 WebAdmin Single ItemSubmit Template Editing。

本阶段目标是给 `virtual_block_device` 实现“单物品 itemSubmit”基础 WebAdmin + 游戏内模板编辑闭环。

itemSubmit 不是新的触发源。itemSubmit 属于“玩家右键交互触发”之后的条件 / 提交层：

玩家右键 VBD -> 右键交互触发 -> itemSubmit 单物品提交判定 -> 成功 / 失败后按当前已有逻辑处理。

因此，只有启用右键交互后才显示 itemSubmit 摘要和编辑入口。未启用右键交互时，不显示 itemSubmit 卡片，不显示 itemSubmit 编辑入口；如已有数据，只能在右键交互语义内给出 warning。

## 当前分支

`feature/web-admin-single-item-submit-editing`

## 允许范围

- VBD 单物品 itemSubmit 摘要显示。
- itemSubmit 只在“玩家右键交互”启用 / 选中后显示。
- VBD 详情页右键交互区域内显示 itemSubmit 摘要 / 编辑入口。
- 统一设备配置 modal 的右键交互 section 内显示 itemSubmit 摘要 / 编辑入口。
- 复用 7.9 P3b 的游戏内 ghost/template item GUI 能力和真实物品安全交互。
- 7.10 GUI 只有一个“提交物品模板槽”。
- 左键复制模板，不消耗真实物品。
- 右键清空模板。
- 滚轮调整数量，Ctrl + 滚轮一次调整 8。
- tooltip 正常显示。
- 保存后写入单个 itemSubmit requirement。
- 二次打开回显当前单物品 itemSubmit 配置。
- edit lock / fingerprint / WebAdminWriteResult / audit / realtime / dirty guard。
- 字段保留。
- 原版交互策略显示 / 编辑必须使用已有逻辑，不改变运行语义。

## 禁止范围

- 不做多 requirement。
- 不做多物品 itemSubmit。
- 不做多槽提交。
- 不做复杂 consume 策略编辑。
- 不做 inventory / equipment / armor 来源编辑。
- 不做 ConditionEngine。
- 不做成功 / 失败路径可视化。
- 不做 Scratch-like / graph / mind-map。
- 不使用 raw JSON。
- 不做任意 NBT path。
- 不做告示牌文本检测。
- 不做命令方块命令检测。
- 不做刷怪笼 NBT 检测。
- 不做玩家 / 实体 NBT 检测。
- 不重新实现或破坏 7.9 container template GUI。
- 不改变 P3b 容器模板语义。
- 不改变红石 / BlockState / 容器触发逻辑。
- 不新增“屏蔽交互”字段，不改原版交互运行时语义。

## 单物品与 7.11 多物品边界

7.10 只支持单个 itemSubmit requirement：

- 当前无 requirements：保存时创建一个 requirement。
- 当前正好一个 requirement：保存时更新这个 requirement。
- 当前已有多个 requirements：7.10 不覆盖、不降级为单 requirement；只读显示摘要并提示“当前为多物品提交配置，7.10 单物品编辑器不支持编辑；请等待 7.11 多物品编辑。”

7.11 才做多物品 itemSubmit / 多 requirement。

## 原版交互策略边界

原版交互策略属于右键交互条件层，不是独立触发源。本阶段必须先复用现有字段和逻辑：

- `InteractionItemVanillaPolicy`
- `InteractionDecisionEvaluator`
- `VirtualBlockDeviceInteractionHandler`

UI 文案统一为“原版交互策略”。可展示当前已有选项，例如：

- 允许原版交互
- 需要匹配 / 提交成功才允许原版交互

7.10 保存 itemSubmit 不得重置该策略，除非用户明确修改同一既有字段。若 7.8 matcher 已经编辑该字段，7.10 必须复用 / 链接 / 摘要，不做两个冲突入口。

## 当前已实现内容

- 已创建 7.10 独立 context。
- 新增 WebAdmin 单物品 itemSubmit 模板会话 API：
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/single-item-submit`
  - `POST /api/webadmin/virtual-block-devices/{deviceId}/single-item-submit-session/start`
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/single-item-submit-session/status`
  - `POST /api/webadmin/virtual-block-devices/{deviceId}/single-item-submit-session/cancel`
- 新增独立 lock target：`virtual_block_device_single_item_submit:<deviceId>`。
- expected fingerprint 覆盖右键交互开关、`itemSubmitEnabled`、单 requirement 的可配置字段、旧有 consume 字段和既有原版交互策略；排除 requirement runtime `last*`、顶层 runtime `last*`、7.9 native trigger、7.8 matcher 复杂数据、container itemConditions 和 metadata。
- VBD 原生触发右键交互摘要内显示单物品 itemSubmit 摘要 / 编辑入口。
- 统一设备配置 modal 的右键交互 section 内显示单物品 itemSubmit 摘要 / 编辑入口。
- 右键交互未启用时隐藏 itemSubmit 编辑入口；如已有数据，只显示“已配置但右键交互未启用”的 warning。
- 游戏内单槽“单物品提交模板”GUI 已接入：
  - 类箱子式布局。
  - 下方玩家背包 / 快捷栏作为真实 cursor stack 来源。
  - 左键复制模板，不消耗真实物品。
  - 右键清空模板。
  - 滚轮 / Ctrl + 滚轮调整匹配数量；模板 ItemStack 显示数量仍按物品最大堆叠数 clamp，匹配 `count` 可使用旧命令层允许的更大数值。
  - tooltip 正常显示。
  - ESC / 取消不保存，保存按钮才写入配置。
- 返修后，7.10 单 requirement WebUI / 游戏内 GUI 补齐旧命令层已有的安全字段：
  - `itemSubmitEnabled` 启用 / 禁用。
  - 单 requirement `enabled` 启用 / 禁用。
  - `countMode`：`at_least`、`exactly`、`at_most`、`ignore`。
  - 匹配数量 `requiredCount` / `count`。
  - matcher option：`matchDamage`、`matchCustomName`、`matchLore`、`matchCustomData`、`matchComponents`。
  - `itemSubmitConsumeEnabled`。
  - `itemSubmitConsumeOrder`：`hotbar_first`、`main_inventory_first`。
  - 单 requirement `consumeCount`。
  - 原版交互策略仍复用既有 `InteractionItemVanillaPolicy` 字段：`allow` / `require_item_match`。
- 保存路径只写入单个 itemSubmit requirement：
  - 当前无 requirements：创建一个 requirement。
  - 当前正好一个 requirement：更新该 requirement，并保留 id / name / runtime last。
  - 当前已有多个 requirements：只读提示，不覆盖、不降级。
- 保存时 scoped update `itemSubmitEnabled` / `itemSubmitConsumeEnabled` / `itemSubmitConsumeOrder` / 单 requirement / 既有 `InteractionItemVanillaPolicy` 字段，保留 7.9 native trigger、7.8 matcher 其它字段、container itemConditions、metadata 和 runtime `last*`。
- 保存成功发布 config / device / audit realtime，并刷新 WebUI 已保存快照；新的 session 必须从当前 `SignalDeviceData` 初始化，不能复用旧 draft。
- 原版交互策略继续使用既有 `InteractionItemVanillaPolicy` 字段，不新增字段，不改变运行时 PASS / FAIL / `blocksVanillaOnFailure` 语义。
- 返修后，单物品 itemSubmit 模板区分 display template 和 matching semantics：
  - display template 使用内部 `ItemStack` 快照持久化，供游戏内 GUI 回显和 tooltip 使用。
  - 附魔书 stored enchantments、普通附魔、customName、Lore、damage、customData、components 等可见信息必须在保存 / 再打开 / 再保存过程中保留。
  - matcher option 仍决定匹配语义；`matchComponents=false` 时组件只用于回显，不参与匹配；`matchComponents=true` 时继续按现有 `templateComponents` / `ItemStackMatcher` 语义参与匹配。
  - 不暴露 raw JSON / NBT path 编辑。

## 待验收内容

- VBD 详情页右键交互区域内显示单物品 itemSubmit 摘要 / 编辑入口。
- 统一设备配置 modal 的右键交互 section 内显示单物品 itemSubmit 摘要 / 编辑入口。
- 右键交互未启用时不显示 itemSubmit 卡片和编辑入口。
- 已有 itemSubmit 但右键交互未启用时，在右键交互语义内显示 warning。
- 单槽游戏内模板 GUI 可打开。
- GUI 类原版箱子布局，下方玩家背包 / 快捷栏可作为真实 cursor 物品来源。
- 左键复制模板不消耗真实物品。
- 右键清空模板。
- 滚轮 / Ctrl + 滚轮调整数量。
- tooltip 正常。
- 保存写入单个 itemSubmit requirement。
- 二次打开回显。
- 多 requirement 不被覆盖。
- consume 只开放旧有字段：提交后消耗开关、消耗顺序、consumeCount；不做新的 consume 策略。
- 原版交互策略使用已有字段，不改变运行语义。
- edit lock / fingerprint / WebAdminWriteResult / audit / realtime / dirty guard 有效。
- 不出现 itemSubmit 多物品编辑、新 consume 策略、inventory/equipment、ConditionEngine、路径图、raw JSON、NBT path。

## 后续阶段边界

- 7.11 做多物品 itemSubmit / 多 requirement。
- consume 策略、inventory / equipment / armor 来源、ConditionEngine 后续独立阶段评估。
- 成功 / 失败路径可视化、Scratch-like、graph、mind-map 后续统一处理。
