# WebAdmin VBD Native Trigger Config 7.9 Current Context

## 当前阶段

7.9 WebAdmin Virtual Block Device Native Trigger Config Coverage。

本阶段目标是把 `virtual_block_device` 的原生触发方式 WebUI 化。VBD 原生触发源只有：

- 红石 / 受电状态
- BlockState 条件
- 玩家右键交互
- 容器打开
- 容器关闭
- 容器内容变化

`interactionItem` / `ItemStackMatcher` / `itemSubmit` / `consume` / `successChannel` / `failChannel` 不是新的触发源，它们属于右键交互触发之后的条件或判定层。VBD 详情页不应把 interaction item matcher 渲染成长期独立卡片；matcher 摘要和编辑入口应挂在“原生触发配置 -> 玩家右键交互”区域内。

## 当前分支

`feature/web-admin-vbd-native-trigger-config`

## P1 / P2 / P3 拆分

### 7.9 P1

- VBD 原生触发字段调查。
- VBD 详情页右侧 detail / secondary column 中的“原生触发配置”只读摘要。
- 根据当前 VBD 实际已启用 / 已配置数据自动显示触发方式摘要。
- 没有任何原生触发配置时显示空状态，不显示未配置卡片。
- BlockState 当前方块属性与可选值读取基础。
- 统一设备配置 modal 内“原生触发配置”section / tab / collapsible 骨架。
- 手工验收文档与 stabilization / smoke guard。

P1 不做持久化写入，不新增 native trigger 写 API。

### 7.9 P2

- VBD 原生触发普通配置完整编辑。
- 红石、BlockState、右键交互、容器 open / close / change 的普通配置保存。
- P2 支持真正选择 / 启用原生触发方式后，interaction item matcher 必须完全受“玩家右键交互”控制：
  - interaction item matcher 必须完全隐藏，除非右键交互触发已启用或已选中。
  - 只有右键交互触发被启用或在编辑 UI 中被选中时，才显示 matcher 摘要和 matcher 编辑入口。
  - matcher 是右键交互触发之后的条件 / 判定层，不是独立触发源。
  - matcher 不得与红石、BlockState、容器打开、容器关闭、容器内容变化平级展示。
  - matcher 不得作为独立 native trigger card、独立配置模块或长期独立详情卡片出现。
  - matcher 必须纳入“玩家右键交互”配置区域内。
  - P1 暂时允许保留现有 matcher 编辑入口；P2 必须把 matcher 纳入“玩家右键交互”配置区域内。
  - P2 仍不做成功 / 失败路径图、逻辑链图、graph、mind-map 或 Scratch-like 可视化。
- 服务端二次校验 BlockState 属性和值。
- `virtual_block_device_triggers:<deviceId>` 或项目风格等价 lock target。

### 7.9 P3

- Container Change Template GUI / 容器内容变化物品模板编辑器。
- WebUI 发起编辑 session。
- 游戏内打开自定义箱子 GUI。
- 二次打开加载当前已保存模板。
- 左键放入 ghost / template item，不消耗鼠标物品。
- 右键模板格直接清空。
- 鼠标悬浮模板物品显示正常物品 tooltip。
- 鼠标滚轮调整数量。
- 不按 Ctrl 每次调整 1。
- 按 Ctrl 每次调整 8。
- 顶部说明文字适配 GUI scale / 小屏，不遮挡槽位。
- ESC 关闭等于取消，不保存。
- 点击保存才写入配置。
- 匹配模式决定哪些字段参与匹配。
- 不在 P1 / P2 普通 Web 表单里硬做复杂物品模板。

## 允许范围

- `virtual_block_device` 原生触发只读摘要。
- 详情页和统一设备配置 modal 中的原生触发配置骨架。
- 数据驱动的原生触发只读摘要。
- BlockState 支持属性与目标值的只读读取基础。
- 7.9 P1 验收文档。
- P1 smoke / guard。

## 禁止范围

- 不做完整保存红石配置。
- 不做完整保存 BlockState 条件。
- 不做完整保存容器 open / close / change 配置。
- 不做容器内容变化物品模板 GUI。
- 不做 itemSubmit。
- 不做 consume。
- 不做 inventory / equipment / armor。
- 不做 ConditionEngine。
- 不做成功 / 失败路径图。
- 不做 Scratch-like / graph / mind-map。
- 不做任意 NBT path。
- 不做玩家 NBT / 实体 NBT。
- 不做告示牌文本检测。
- 不做命令方块命令检测。
- 不做刷怪笼 NBT 检测。
- 不使用 raw JSON textarea。
- 不新增 native trigger 持久化写 API。

## 当前已实现内容

当前稳定基线：

- `v1.37.0-web-admin-interaction-item-matcher-editing`
- 7.8 已完成 VBD interaction item matcher 摘要、独立编辑 modal、统一设备配置 modal 内 matcher 编辑能力、edit lock、fingerprint、audit、realtime、dirty close guard。7.9 语义中 matcher 作为玩家右键交互后的条件层展示，不作为独立原生触发源。

7.9 P1 实现状态：

- 已实现 VBD 原生触发只读 overview API：
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/native-triggers`
  - 仅支持 `virtual_block_device`
  - P1 只读，`writeApiEnabled=false`
- 已实现 VBD 详情页“原生触发配置”摘要区域，并放在右侧 detail / secondary column，避免左侧主内容流过长导致左右高度不平衡。
- 已实现 6 类触发源数据驱动只读摘要：
  - 红石 / 受电状态
  - BlockState 条件
  - 玩家右键交互
  - 容器打开
  - 容器关闭
  - 容器内容变化
- 已实现只显示实际 active / configured 的触发摘要；P1 不提供手动 selector，也不持久化触发启用状态。
- 已修复 channel catalog / combobox 来源一致性：Signal 页面和所有 channel combobox 统一使用 `/api/signals/channels`，VBD 创建、设备配置、action relay actions、listener 配置等事件会让 channel options cache dirty。
- 已实现 BlockState 当前绑定方块属性读取基础：
  - 属性来自当前绑定方块实际 `BlockState.getProperties()`
  - 可选值来自该属性实际 `Property.getValues()`
  - world / chunk / air / block mismatch / ready 状态分开展示
- 已在统一设备配置 modal 内加入“原生触发配置”只读 section。
- 已将 7.8 interaction item matcher 摘要和入口合并到“玩家右键交互”摘要中；独立 matcher 编辑 modal / API / 保存链路保留，但 VBD 详情页不再显示 standalone matcher card。
- 已创建 P1 手工验收文档和 stabilization guard。

## 待验收内容

- VBD 详情页右侧 detail / secondary column 出现“原生触发配置”区域。
- 原生触发区域不显示手动触发方式 selector。
- 当前 VBD 只显示实际已启用 / 已配置的原生触发摘要。
- 无原生触发配置时显示空状态。
- 红石配置 active 时显示红石摘要。
- BlockState 条件 active 时显示 BlockState 摘要。
- 右键交互 active 时显示右键交互摘要，并在该摘要内显示 7.8 matcher 条件状态和入口。
- 如果 matcher 已配置但右键交互未启用，右键交互摘要显示“已配置交互物品匹配，但右键交互触发尚未启用”。
- 容器 open / close / change active 时只显示对应容器摘要。
- Signal 页面可见的 channel 在 WebUI channel combobox 中也可见。
- BlockState 属性来自当前绑定方块实际 `BlockState.getProperties()`。
- BlockState 目标值来自该属性实际支持值。
- 统一设备配置 modal 内能看到“原生触发配置”section。
- P1 不新增 native trigger 写 API。
- 不出现 itemSubmit / consume / ConditionEngine / 路径图 / raw JSON。

## 后续阶段边界

- 7.9 P2 做 native trigger 普通配置写入闭环。
- interaction item matcher 在 7.9 P2 起必须纳入“玩家右键交互”配置区域；未启用 / 未选中右键交互时隐藏 matcher，不作为独立模块展示。
- 7.9 P3 做容器内容变化物品模板 GUI。
- 7.10 做单物品 itemSubmit。
- 7.11 做多物品 itemSubmit / 多 requirement。
- 成功 / 失败路径可视化、Scratch-like、graph、mind-map 后续统一处理。
- ConditionEngine 属于后续大阶段，不属于 7.9 P1。
