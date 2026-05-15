# ConditionEngine Capability Matrix 8.5

8.5 在 8.0 Core、8.1 玩家 / 上下文条件、8.2 状态变量、8.3 物品 / 背包 / 容器条件、8.4 Region / Signal / Logic Chain 条件之上，新增 WebAdmin Condition Editor MVP。它只管理和测试 Condition Group 配置，不接入运行时。

## WebAdmin 能力

| 能力 | 状态 | 说明 |
|---|---|---|
| Condition Type Catalog | 已实现 | `/api/webadmin/condition-types` 从 `ConditionRegistry` 输出只读目录。 |
| 中文 metadata | 已实现 | catalog 输出中文显示名、描述、字段名、分类；type id 作为副文本。 |
| Condition Group Store | 已实现 | world-scoped `condition_groups.json`。 |
| Condition Group List / Detail | 已实现 | WebAdmin `#/condition-groups` 和 `#/condition-groups/{id}`。 |
| Structured Node Editor | 已实现 | 紧凑节点卡片 + 二级字段编辑面板，不在主页面堆叠所有节点完整表单，不做 Scratch 拖拽。 |
| AND / OR / NOT 编辑 | 已实现 | root 和 nested group 均支持。 |
| validation | 已实现 | 保存前和 API validate 都走 `ConditionEvaluator.validate`。 |
| preview evaluate | 已实现 MVP | 支持基础 context、player snapshot、手动输入的 GLOBAL / PLAYER state variable snapshot；不读取 live state store。 |
| item / inventory / container preview form | Deferred | 8.5 不做完整结构化模拟输入；后续补。 |
| region / signal / logic chain preview form | Deferred | 8.5 不查询 live service，后续补结构化 snapshot 输入。 |
| WebAdmin write foundation | 已实现 | permission、CSRF、same-origin、edit lock、fingerprint、audit、realtime。 |
| 新建草稿锁状态 | 已实现 | 新建条件组本地草稿显示保存时自动获取 edit lock；已有条件组编辑必须持锁。 |
| condition type 回显 | 已实现 | 保存后按真实 type 和 config 回显；unknown / 缺字段中文校验失败，不静默变 always_true。8.5 P0 返修后覆盖 JSON request -> service -> store -> detail 的真实 round-trip。 |
| condition type 选择器 | 已实现 | 使用自绘按钮列表 + hidden draft value，不使用原生 condition type select，避免 PagePopupController 截断 inline JS。 |
| 前端保存 payload | 已实现 | `conditionGroupSavePayload` 是保存请求唯一构造入口，保存前同步当前节点 type/config。 |
| 资源版本 / 缓存 | 已实现 | asset version `8.5-condition-editor-p0-3`，HTML/CSS/JS 使用 no-store，避免旧 app.js 继续生效。 |
| raw JSON editor | 不做 | 只允许结构化编辑；不提供 raw JSON 主入口。 |

## API

| API | 权限 | 状态 | 说明 |
|---|---|---|---|
| `GET /api/webadmin/condition-types` | VIEWER | 已实现 | 只读 condition type catalog。 |
| `GET /api/webadmin/condition-groups` | VIEWER | 已实现 | 条件组列表。 |
| `GET /api/webadmin/condition-groups/{id}` | VIEWER | 已实现 | 条件组详情。 |
| `POST /api/webadmin/condition-groups` | EDITOR / OWNER | 已实现 | 创建条件组。 |
| `PATCH /api/webadmin/condition-groups/{id}` | EDITOR / OWNER | 已实现 | 更新条件组。 |
| `POST /api/webadmin/condition-groups/{id}/delete` | EDITOR / OWNER | 已实现 | 删除条件组。 |
| `POST /api/webadmin/condition-groups/{id}/validate` | TESTER+ / policy | 已实现 | 只读校验。 |
| `POST /api/webadmin/condition-groups/{id}/preview` | TESTER+ / policy | 已实现 | simulation evaluate，不写 store。 |

## Store

| 字段 | 状态 | 说明 |
|---|---|---|
| `id` | 已实现 | stable condition group id。 |
| `displayName` | 已实现 | 中文显示名。 |
| `note` | 已实现 | 备注。 |
| `iconKey` | 已实现 | WebAdmin 图标 key。 |
| `enabled` | 已实现 | 仅配置状态，不影响 runtime。 |
| `tags` | 已实现 | WebAdmin metadata。 |
| `groupDefinition` | 已实现 | 复用 ConditionEngine definition。 |
| `version` / `fingerprint` | 已实现 | expectedFingerprint 冲突保护。 |
| `createdAt` / `updatedAt` / `updatedBy` | 已实现 | audit / list 展示辅助。 |

## 边界

- 8.5 不把 condition group 挂到 VBD、SignalListener、RegionController、ActionRelay、Action、itemSubmit。
- 8.5 不读取 live world、live player list、live inventory / container、live RegionController、live SignalBridge、live Logic Chain Viewer service。
- 8.5 不做具体任务 / 关卡，不做 GameController / MissionSystem / PhaseController。
- 8.5 不新增 MCP tool，不跑 MCP scenario，不生成截图，不启动 Minecraft。

## 后续规划

| 阶段 | 建议范围 |
|---|---|
| 8.6 | Condition Group runtime attach metadata 设计，先从只读挂载预览开始。 |
| 8.7 | 小范围 runtime gate 接入，保持可回滚和诊断。 |
| 8.8 | 更完整 preview snapshot 表单、Doctor debug tree 与引用分析。 |
| 8.9 | 条件模板、可视化调试器、复杂多人聚合条件准备。 |
