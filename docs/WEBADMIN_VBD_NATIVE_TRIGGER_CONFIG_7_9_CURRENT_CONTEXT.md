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

P3 拆分为：

- P3a：Container Change Template GUI Session + GUI Skeleton。
  - WebAdmin 从 VBD 详情页和统一设备配置 modal 的容器内容变化 section 发起模板编辑 session。
  - 目标在线玩家打开游戏内“容器内容变化模板”GUI skeleton。
  - Session 使用 `virtual_block_device_container_template:<deviceId>` lock target。
  - expected fingerprint 只覆盖 normalized `itemConditions`，包含 condition 基础字段和 matcher 模板 / 匹配开关 / hash；排除 runtime `last*`、P2 native trigger 基础字段、itemSubmit / consume、raw / NBT / path 字段。
  - GUI 从已保存 `itemConditions` 加载只读模板快照；二次打开必须显示当前已保存配置。
  - slot 条件映射到模板槽，总量条件映射到总量模板区域，高级 / 无法映射条件显示为只读摘要。
  - ESC、关闭窗口、WebUI cancel、目标玩家离线或 session 超时都取消 session，不保存，并释放 lock。
  - P3a 不真实保存 `itemConditions`，保存按钮禁用或 not_implemented。
  - P3a 不实现持久化 ghost item editing，不修改世界容器，不消耗或转移玩家真实物品。
- P3b：Ghost item editing + save。
  - 左键复制模板、右键清空、滚轮 / Ctrl+滚轮调整数量。
  - 点击保存才写入 `itemConditions`。
  - 完整 field preservation、audit、realtime 和失败处理闭环。
  - GUI 采用接近原版箱子的布局：上方 ghost/template 槽，下方玩家背包与快捷栏展示；玩家背包只作为安全来源选择，不走真实物品转移路径。

## 允许范围

- `virtual_block_device` 原生触发只读摘要。
- 详情页和统一设备配置 modal 中的原生触发配置骨架。
- 数据驱动的原生触发只读摘要。
- BlockState 支持属性与目标值的只读读取基础。
- 7.9 P2 中 `virtual_block_device` 原生触发普通配置编辑闭环：
  - 红石 / 受电状态。
  - BlockState 条件结构化编辑。
  - 玩家右键交互基础配置。
  - 容器打开 / 关闭 / 内容变化基础配置。
  - `virtual_block_device_triggers:<deviceId>` 编辑锁、fingerprint、audit、realtime 和 `WebAdminWriteResult`。
- 7.9 P1 验收文档。
- P1 smoke / guard。

## 禁止范围

- P1 / P2 不做容器内容变化物品模板 GUI；P3a 只允许 session + 只读 GUI skeleton，不保存 `itemConditions`；P3b 才允许通过游戏内 ghost/template GUI 保存 `itemConditions`。
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
- 不把 interaction item matcher 做成独立触发源。
- 不清空 7.8 matcher 数据、itemSubmit 未来字段或 container itemConditions。

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

7.9 P2 当前实现状态：

- 已扩展原生触发 API：
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/native-triggers`
  - `PATCH /api/webadmin/virtual-block-devices/{deviceId}/native-triggers`
  - 仅支持 `virtual_block_device`；非 VBD 返回 validation error。
- PATCH 接入 EDITOR / OWNER 权限、CSRF / same-origin、`WebAdminWriteResult`、`virtual_block_device_triggers:<deviceId>` edit lock、expected fingerprint、audit 和 realtime。
- 已实现红石 / 受电状态编辑；文案保留 `currentPowered = blockStatePowered || receivedPowerLevel > 0` 语义，不把红石简化为 `powered` 属性。
- 红石触发禁用使用 `redstone_disabled` 模式停止红石边沿触发，不写入 VBD 全局 `enabled=false`，因此不会误关右键交互或容器触发。主频道 / 断电频道字段保留。
- 已实现 BlockState 条件结构化编辑：
  - 属性名下拉只来自当前绑定方块实际 `BlockState.getProperties()`。
  - 目标值只来自该属性实际 `Property.getValues()`。
  - BlockState 条件没有独立 `conditionChannel` 字段，WebUI 中的“BlockState 触发频道 / 主频道”写入 VBD 共享 `channel`。
  - `condition_enter` 只在条件从不满足变为满足时触发，使用 VBD 主 `channel`。
  - `condition_exit` 只在条件从满足变为不满足时触发，使用 `offChannel`，未设置时回退主 `channel`。
  - `condition_both` 在进入和退出条件时触发，进入使用主 `channel`，退出使用 `offChannel` 回退主 `channel`。
  - 保存配置不会立即触发 BlockState signal；保存时记录当前匹配状态，之后需要绑定方块状态变化才会按 `conditionMode` 触发。
  - 服务端保存时再次校验方块、属性和值。
  - 不提供 raw JSON / raw condition textarea。
- 已修复 WebAdmin 自定义 channel combobox 的 toggle 行为：点击箭头可打开，再次点击同一箭头可关闭；点击外部、ESC、选择选项会关闭；打开一个 combobox 会关闭其它 combobox；这些开关状态不进入 dirty 判断。
- 已实现右键交互基础配置：`interactionEnabled`、`interactChannel`、`interactionCooldownTicks`。
- 已实现容器打开 / 关闭 / 内容变化基础配置：channel、container cooldown、change check interval。`itemConditions` 只读保留，复杂物品模板 GUI 仍在 P3。
- 容器 open / close / change 共用 `containerEnabled`；全部关闭时 `containerEnabled=false`，保存的 `itemConditions` 仍保留但不会触发。
- 统一设备配置 modal 内可以直接编辑原生触发配置；独立“编辑原生触发配置”modal 也可用。
- 7.9 后，`virtual_block_device` 的旧“类型专属配置”不再作为 WebUI 中的独立显示或编辑区域：
  - VBD 原生触发字段统一归入“原生触发配置”。
  - interaction item matcher 归入“玩家右键交互”的条件 / 判定层。
  - itemSubmit 后续单独阶段处理。
  - receiver / relay 的类型专属配置仍保留，不属于本次清理范围。
- P2 的触发方式选择是真实配置；编辑 UI 只显示已启用 / 已选中的触发 section。
- interaction item matcher 在 P2 中完全受右键交互控制：
  - 右键交互未启用 / 未选中时隐藏 matcher 摘要与入口。
  - 右键交互启用 / 选中后，matcher 摘要和编辑入口显示在右键交互 section 内。
  - 隐藏 matcher 不清空 matcher 数据。
- 保存 native trigger 只写入原生触发字段，不重写整个 VBD JSON，不清空 redstone / BlockState / interaction / matcher / container / `itemConditions` / itemSubmit 保留字段。
- 保存成功发布 `virtual_block_device_changed`、`device_config_changed`、`config_changed`、`write_audit_appended` 等 realtime 事件，并让 channel catalog cache dirty。

7.9 P3a 当前实现目标：

- 新增容器内容变化模板 session API：
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/container-template`
  - `POST /api/webadmin/virtual-block-devices/{deviceId}/container-template-session/start`
  - `GET /api/webadmin/virtual-block-devices/{deviceId}/container-template-session/status`
  - `POST /api/webadmin/virtual-block-devices/{deviceId}/container-template-session/cancel`
- WebAdmin start / cancel 需要 EDITOR / OWNER、CSRF / same-origin、`WebAdminWriteResult` 风格响应和 edit lock；overview 可供 VIEWER 查看。
- P3a 只创建独立 session、获取 `virtual_block_device_container_template:<deviceId>` lock、计算 itemConditions fingerprint、向目标在线玩家打开 GUI skeleton、展示 session 状态。
- 客户端 GUI skeleton 是只读预览，不使用真实容器路径，不允许真实物品转移，不写入世界容器。
- GUI 读取已保存 `itemConditions` 并展示 slot / total / advanced 条件摘要；tooltip 只作展示，不代表所有字段都会参与匹配。
- GUI ESC、关闭窗口或 WebUI cancel 都取消 session，不保存 `itemConditions`。
- P3a 发送 `container_template_session_started/opened/cancelled/failed/expired` realtime 状态事件；不发布 `config_changed`，因为不保存配置。
- P3a audit 只记录 session start / cancel / fail / expire，不伪造配置写入。

7.9 P3b 当前实现目标：

- 在 P3a session / lock / status / cancel lifecycle 基础上，实现游戏内 ghost/template item 编辑与保存。
- 客户端 GUI 使用接近原版箱子的布局：模板槽区域、玩家背包区域和快捷栏区域；模板槽是 ghost/template，不绑定世界真实容器。
- 玩家背包和快捷栏应尽量遵循原版箱子操作逻辑：玩家可以从下方背包 / 快捷栏真实拿起鼠标 cursor 物品，也可以放回玩家背包；模板槽永远不参与真实物品转移。
- 左键模板格复制当前鼠标 cursor 物品为 ghost/template，不消耗 cursor stack；鼠标为空点击模板槽不会从 ghost 模板取出真实物品；右键清空模板格；滚轮调整数量，Ctrl + 滚轮一次调整 8。
- 单个 slot 模板格显示和保存的数量必须 clamp 到该物品 `ItemStack#getMaxCount` 或当前版本等价 API；不可堆叠物品不得超过 1。
- P3b 当前仅完整编辑 slot_* 模板。total_* 表示“匹配整个容器中某物品 / matcher 的总数量，不对应具体槽位”，本轮作为只读保留显示；总量模板完整编辑后续再支持，避免显示看似可用但保存 / 回显语义不清的区域。
- 保存成功后 WebUI container template modal 必须刷新 `GET /container-template` 快照，立即显示刚保存的 `itemConditions` 摘要；二次打开游戏内 GUI 也必须回显保存后的模板。
- P3b 保存的模板条件默认继承父 VBD 的 `containerChangeChannel`：
  - `condition.channel` 非空时作为显式覆盖。
  - `condition.channel` 为空时使用父 VBD `containerChangeChannel` 作为 effective channel。
  - `condition.channel` 和父 VBD `containerChangeChannel` 都为空时才是真正的频道缺失错误。
  - Doctor、runtime 和 WebUI snapshot 必须统一使用 effective channel 语义。
  - P3b GUI 不要求每个模板格单独配置 channel，也不把 per-slot channel 作为主要入口。
  - 父 `containerChangeChannel` 为空时允许保存模板，但 WebUI / Doctor 需要清楚提示模板不会发出 signal，直到配置容器内容变化频道或显式 condition channel。
- 同一个 save / cancel / close / expired terminal 事件只向目标玩家反馈一次；玩家可见反馈由服务端 terminal handler 统一发送，客户端只关闭 UI，不重复发送聊天文本。
- Tooltip 使用正常 `ItemStack` tooltip 展示；tooltip 只是展示，不代表所有字段都会参与匹配，实际匹配仍由 `slot_*` / `total_*` 和 `countMode` / matcher 字段决定。
- 点击游戏内“保存模板”才通过 session 保存 `itemConditions`；保存前校验 sessionId / nonce / target player / expected fingerprint / VBD 类型和 itemConditions 数据。
- 保存只做 scoped update `SignalDeviceData.itemConditions`，不重写 P2 原生触发、7.8 matcher、metadata、itemSubmit / consume 未来保留字段，也不修改世界容器或玩家物品。
- 保存成功发布 `container_template_session_saved`、`config_changed`、`device_config_changed`、`write_audit_appended` 等 realtime / audit；取消仍不保存且不发布 `config_changed`。

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
- 统一设备配置 modal 内可以直接编辑原生触发配置，切换 section 不重置滚动。
- 独立 native trigger 编辑 modal 可保存红石 / BlockState / 右键交互 / 容器 open / close / change 基础配置。
- 他人持有 `virtual_block_device_triggers` lock 时，编辑按钮前置禁用。
- BlockState 保存拒绝当前方块不支持的属性和值。
- 不出现 itemSubmit / consume / ConditionEngine / 路径图 / raw JSON。

## 后续阶段边界

- 7.9 P2 做 native trigger 普通配置写入闭环。
- interaction item matcher 在 7.9 P2 起必须纳入“玩家右键交互”配置区域；未启用 / 未选中右键交互时隐藏 matcher，不作为独立模块展示。
- 7.9 P3 做容器内容变化物品模板 GUI。
- 7.10 做单物品 itemSubmit。
- 7.11 做多物品 itemSubmit / 多 requirement。
- 成功 / 失败路径可视化、Scratch-like、graph、mind-map 后续统一处理。
- ConditionEngine 属于后续大阶段，不属于 7.9 P1。
