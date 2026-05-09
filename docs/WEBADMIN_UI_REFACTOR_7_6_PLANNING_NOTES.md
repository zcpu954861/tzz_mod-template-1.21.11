# WebAdmin UI Refactor 7.6 Planning Notes

本文档只记录 7.6 规划建议，不代表已经进入 7.6 代码实现。当前 7.5 仍以只读页面、route 稳定化、disabled 写操作边界和 realtime silent refresh 为主。

## 1. 7.6 名称

**interactionItem / itemSubmit / ItemStack Matcher 可视化编辑**

## 2. 7.6 目标

- 右键交互物品条件的可视化配置。
- itemSubmit 条件的可视化配置。
- 容器 / 物品匹配条件的表单化展示与编辑。
- ItemStack Matcher 的可视化配置，避免让用户直接编辑裸 JSON。

## 3. 7.6 UI 原则

- 不能要求用户直接填写裸 JSON。
- 优先使用卡片化、表单化、分组化和可视化控件。
- Minecraft 物品 / 方块材质必须使用原版资源，不使用 image2 重画原版材质。
- 编辑 / 配置入口统一使用 animated modal + 毛玻璃遮罩。
- modal 受 viewport 约束，内容过多时仅 modal body 内部滚动。
- modal header / footer 固定，危险操作使用红色语义。
- 复杂编辑不在主页面展开，也不在表格行内直接展开。

## 4. 7.6 后端边界

- 开始实现前必须先确认当前 matcher / interactionItem / itemSubmit 数据模型。
- 必须保留现有 itemSubmit / interactionItem 语义和兼容字段。
- 不破坏已有设备配置、频道配置、listener 配置和 Action / Region 只读展示。
- 任何写操作都必须具备权限检查、CSRF / same-origin、安全校验、audit、edit lock、`WebAdminWriteResult`、冲突处理和错误边界。
- 不因为 UI 有按钮就新增未审查的业务 API 或启用写操作。

## 5. 7.6 不做

- 不做 Scratch-like editor。
- 不做 ConditionEngine。
- 不做全局图编辑。
- 不做高层 GameController。
- 不做复杂配置版本发布 / 回滚系统。
- 不做真实用户权限写入或系统设置写入。

## 6. 7.7 提醒

小逻辑链 card / Scratch-like editor v1 不能丢，应在 7.6 后紧接推进，但不能提前混入 7.6 第一批实现。

## 7. 7.8 提醒

模板 / 向导 / 预设应单独规划。7.5 的动作模板页面当前只是只读候选与 disabled 边界，不代表已有真实模板 CRUD。

## 8. 7.9 提醒

简化配置版本管理 + 编辑层稳定化可作为 7.9 方向，但必须等待写入基础、权限、审计、edit lock 和回滚边界完整后推进。

## 9. 虚拟监听器页面

虚拟监听器页面以后需要规划，但不是 7.5 内容，也不是 7.6 第一优先级。后续应单独整理范围、数据源、写入边界和验收方式。
