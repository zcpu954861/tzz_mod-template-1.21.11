# WebAdmin Interaction Item Matcher 7.8 Current Context

## 当前阶段

7.8 WebAdmin Interaction Item Matcher Editing。

当前目标只覆盖 `virtual_block_device` 的 interaction item matcher 普通配置编辑：玩家右键虚拟方块设备时，WebUI 可以查看和编辑是否要求手持/使用指定物品，以及当前代码中已经存在且安全可表单化的物品匹配字段。

## 当前分支

`feature/web-admin-interaction-item-matcher-editing`

## 允许范围

- `virtual_block_device` 详情页展示 interaction item matcher 摘要。
- 提供独立的 interaction item matcher 编辑 modal。
- 在统一设备配置 modal 内提供清晰可见的“交互物品匹配”section / tab / collapsible。
- 编辑当前真实数据结构中已经存在且安全的 matcher 字段。
- 保持 7.5 fixed modal、dark input / combobox、dirty close guard、edit lock、fingerprint、audit、realtime、`WebAdminWriteResult`。
- 保存时保留 VBD 其它配置字段，不破坏 redstone、container、channels、cooldown 或后续 itemSubmit 等未进入本阶段的字段。

## 禁止范围

- 不做 itemSubmit 单物品提交。
- 不做 itemSubmit 多物品提交。
- 不做 consume 策略。
- 不做 inventory consume order。
- 不做 equipment / armor matcher。
- 不做多物品 requirement。
- 不做任意 NBT path 编辑。
- 不做任意 data component raw editor。
- 不做 ConditionEngine。
- 不做 phone / task / blocking / password 联动。
- 不做 Scratch-like editor。
- 不做成功 / 失败路径可视化。
- 不做 trigger -> matcher -> channel -> listener -> action 逻辑链图。
- 不做 graph / mind-map。
- 不使用 raw JSON textarea。
- 不新增与 matcher 无关的业务功能。

## 当前已实现内容

7.7 基线已完成并 tag：

- `v1.36.0-web-admin-signal-physical-device-config`
- 物理 Signal 设备全字段配置覆盖。
- `action_relay` Action list 查看 / 新增 / 编辑 / 删除 / 排序。
- command action validation 已放宽，仅阻断极高风险服务器管理命令。
- typed device ref / complex deviceId、edit lock 前置禁用、realtime 多用户同步、dirty modal close guard 已通过手动验收。

7.8 Step 1 当前实现内容：

- 新增 `virtual_block_device` interaction item matcher 只读摘要。
- 新增独立的“交互物品匹配”编辑 modal。
- 统一“设备配置”modal 内已显示 `virtual_block_device` 专属“交互物品匹配”section。
- 新增 matcher GET / PATCH WebAdmin API，限定 `virtual_block_device`。
- 保存链路接入 EDITOR / OWNER 权限、CSRF / same-origin、edit lock、expected fingerprint、validation、audit、realtime 和 `WebAdminWriteResult`。
- 当前可编辑字段只覆盖 7.8 安全表单字段：启用匹配、物品 ID、数量规则、damage、自定义名称、Lore、主手 / 副手来源、原版交互策略。
- 当前高级来源或策略、raw custom data / data component、consume、itemSubmit、inventory / equipment matcher 均保持只读或禁止。
- 7.9 P1 返修后，matcher 编辑 UI 采用“启用后展开”规则：总开关关闭时只显示开关和说明；启用后显示物品 ID、数量规则、source / vanilla policy；数量为 ignore 时收起数量输入；damage / 自定义名称 / Lore 的值输入只在对应匹配开关开启后显示。隐藏字段不等于清空字段，draft 和后端字段保留策略不回退。

## 待验收内容

- VBD 详情页有 interaction item matcher 摘要卡。
- VBD 详情页可单独打开 matcher 编辑 modal。
- 统一设备配置 modal 内有“交互物品匹配”section，用户不需要退出配置 modal 才能发现该能力。
- 非 VBD 不显示 matcher 编辑入口，或明确 disabled 并说明仅 VBD 支持。
- matcher 字段保存经过权限、CSRF / same-origin、edit lock、fingerprint、validation、audit、realtime。
- dirty close guard 在 matcher 编辑中有效：clean 直接关闭，dirty 才二次确认。
- 保存成功 silent refresh 相关页面，不整页 reload，不覆盖正在编辑的 modal 输入。

## 后续阶段边界

- 7.9 再做单物品 itemSubmit。
- 7.10 再做多物品 itemSubmit / 多 requirement。
- consume / inventory / equipment 后续独立阶段评估。
- 小逻辑链卡片 / Scratch-like 模块化编辑器后续统一处理成功 / 失败路径可视化。
- ConditionEngine 不属于 7.8。
