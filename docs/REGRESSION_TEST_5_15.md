# 5.15 最终回归测试清单

本清单用于 5.15 稳定化收尾后的人工回归。自动化护栏已覆盖字段保留、旧 JSON、consume plan、itemSubmit evaluator、cooldown / lock 决策和诊断 DTO；以下项目重点验证真实 Minecraft / Fabric 运行时行为。

## 1. SignalBridge 基础

- `/tzz signal emit <channel>` 能发出 signal。
- SignalEventHistory 能记录 channel、来源、玩家、结果。
- channel 没有 listener 时，`signal_receiver` / `action_relay` 仍能工作。
- listener 的 command / message / sound / signal action 正常。
- signal action 自递归会被递归保护拦截，不会无限触发。

## 2. 专用设备

- `signal_emitter` 红石上升沿触发 signal，持续通电不重复触发。
- `signal_receiver` 收到 signal 后按 pulseTicks 输出红石。
- `action_relay` 收到 signal 后执行 actions。
- 设备 `enable` / `disable` / `name` / `clearName` / `info` / `debug` / `history` 正常。

## 3. virtual_block_device

- redstone rising / falling / both 模式正常。
- BlockState condition 的 enter / exit / both 正常。
- 右键 interaction 只对已绑定坐标生效。
- `allow` 模式不阻止箱子、门、按钮、拉杆的原版行为。
- `require_item_match` 模式匹配失败会阻止原版交互。
- cooldown 不解除 `require_item_match` 锁。
- 绑定门上半格或下半格，右键另一半都不能绕过锁。
- blockId mismatch 时不触发，需要 refresh 或重新 bind。

## 4. container

- chest / barrel / shulker / furnace 等容器 open 只在实际打开后触发。
- close 只在对应 screen 关闭后触发。
- changed 在内容变化后触发，内容不变不重复触发。
- containerCheckInterval 生效。
- containerCooldown 生效。
- 未加载区块不强制加载。

## 5. itemCondition / ItemStackMatcher

- `slot_empty` 正常。
- `slot_item` 的 at_least / exactly / at_most 正常。
- `total_item` 正常。
- `slot_matcher` 从主手 / 槽位捕获模板后正常匹配。
- `total_matcher` 从主手 / 槽位捕获模板后正常统计。
- `ignore` 不接收 count，表示不检查 matcher 数量。
- 当前支持的 custom name / lore / custom_data / components 匹配按实际能力回归。
- itemCondition `enable` / `disable` / `mode` / `offChannel` / `refresh` / `test` 正常。

## 6. interactionItem

- `main_hand` source 正常匹配。
- `off_hand` source 只读取副手。
- `inventory_contains` 只读取主背包 / 热键栏。
- `armor_head` / `armor_chest` / `armor_legs` / `armor_feet` / `armor_any` 只读取对应盔甲槽。
- successChannel / failChannel 正常走 SignalBridge。
- successMessage / failMessage 正常显示。
- successSound / failSound 只播放给触发玩家。
- main_hand / off_hand / inventory_contains consume 按 consumeCount 消耗。
- cooldown 中成功仍按配置消耗。
- cooldown 中失败仍按 `require_item_match` 锁住原版交互。
- armor_* source 仍拒绝 consume。
- allow 模式仍不阻止原版交互。

## 7. itemSubmit

- 多个 requirement 全部满足时成功。
- 任一 requirement 不足时失败。
- itemSubmit 与 interactionItem matcher 互斥：itemSubmitEnabled=true 时不再要求单物品 matcher。
- itemSubmitEnabled=true 时不执行 interactionItem consume。
- itemSubmit consume disabled 时不消耗。
- itemSubmit consume enabled 时执行两阶段原子消耗。
- 原子失败时不消耗任何物品。
- cooldown 中成功仍执行原子消耗。
- cooldown 中失败仍锁住原版交互。
- failChannel / failMessage / failSound 在非 cooldown 失败时生效。
- itemSubmit requirement `ignore` 表示至少存在一个匹配 stack。

## 8. cleanup

- 已加载区块中绑定位置为空气时，cleanup 可清理 virtual_block_device。
- blockId mismatch 但非空气时不自动删除，只提示 refresh / rebind。
- 未加载区块跳过，不强制加载。
- cleanup 不扫描世界、不扫描区块、不扫描周围方块。

## 9. debug / doctor

- `/tzz signal device debug <device>` 显示结构化诊断。
- `/tzz signal doctor` 显示全局总览和问题列表。
- channel no consumers 能显示清晰中文提示。
- itemSubmit requirement not met 能显示条件、当前数量、要求数量和建议。
- source consume incompatible 能显示 source 与 consume 不兼容。
- debug / doctor 不直接暴露 `code=`，诊断代码只作为辅助字段。
- doctor 不扫描玩家背包，不强制加载区块。

## 10. 重启保存

- `signal_devices.json` 重启后可读取。
- itemSubmit requirements 保存正常。
- interactionItem matcher / source / vanilla policy 保存正常。
- success/fail channel、message、sound 保存正常。
- consumeSource、inventoryConsumeOrder、consumeCount 保存正常。
- name / debug runtime summary 不影响核心配置。

## 11. 旧 JSON 兼容

- 缺少新字段的旧 `signal_devices.json` 不崩溃。
- 默认 source 为 `main_hand`。
- 默认 vanillaInteractionPolicy 为 `allow`。
- 默认 consume disabled，consumeCount 为 1。
- 默认 itemSubmit disabled，requirements 为空。
- container / itemCondition / interaction 默认安全关闭。

## 12. 性能边界

- 不扫描世界。
- 不强制加载区块。
- 不 tick 扫玩家背包。
- 不 tick 扫全部容器。
- 只处理已登记设备。
- 只处理已配置条件。
- 状态不变不 emit。
- 状态不变不写 JSON。
- flushDirty 继续节流，停服时强制 flush。
